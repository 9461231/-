"""
Supermarket Operations Agent 编排层

优先使用 OpenAI Agents SDK（openai-agents）+ MCP streamable HTTP 工具层；
未配置 OPENAI_API_KEY 时自动降级为"规则意图解析 + 直接工具调用"，
保证毕业设计在没有外网/密钥环境下依然可以演示完整业务闭环。
"""
import json
import os
from dataclasses import dataclass, field
from typing import Any

from app.services import backend_client as backend

USE_LLM = bool(os.getenv("OPENAI_API_KEY"))
MODEL = os.getenv("OPENAI_MODEL", "gpt-4.1-mini")
MCP_URL = os.getenv("MCP_SERVER_URL", "http://localhost:8200/mcp")

SYSTEM_PROMPT = """你是中小型超市的智能运营 Agent。你可以调用工具查询真实业务数据（商品/库存/销售/供应商/经营概况），
并基于数据生成分析与建议。写操作（创建采购草稿/促销草稿/库存调整）必须先征得用户确认。
分析补货时：建议采购数量 = 安全库存(最低库存×2) - 当前库存；报告金额时保留两位小数。"""


@dataclass
class PendingAction:
    """待人工确认的写操作"""
    action_id: str
    kind: str  # purchase_draft / promotion_draft / adjust_inventory
    summary: str
    payload: dict


@dataclass
class AgentSession:
    history: list = field(default_factory=list)
    pending: dict = field(default_factory=dict)  # action_id -> PendingAction


SESSIONS: dict = {}


def get_session(session_id: str) -> AgentSession:
    return SESSIONS.setdefault(session_id, AgentSession())


# ==================== LLM 模式（OpenAI Agents SDK） ====================

async def _run_llm(session: AgentSession, message: str) -> str:
    from agents import Agent, Runner
    from agents.mcp import MCPServerStreamableHttp

    server = MCPServerStreamableHttp({"url": MCP_URL, "client_session_timeout_seconds": 30})
    async with server:
        agent = Agent(
            name="SupermarketOperationsAgent",
            instructions=SYSTEM_PROMPT,
            model=MODEL,
            mcp_servers=[server],
        )
        input_items = session.history + [{"role": "user", "content": message}]
        result = await Runner.run(agent, input_items)
        session.history = result.to_input_list()
        return str(result.final_output)


# ==================== 降级模式：规则意图解析 ====================

def _fmt_alerts(alerts: list) -> str:
    if not alerts:
        return "当前没有库存预警商品，库存状况良好。"
    lines = ["当前库存预警商品如下：", "", "| 商品 | 当前库存 | 最低库存 | 预警 | 建议补货 |", "|---|---|---|---|---|"]
    for a in alerts:
        label = {"OUT_OF_STOCK": "缺货", "LOW": "偏低", "HIGH": "偏高"}.get(a["alertType"], a["alertType"])
        lines.append(f"| {a['productName']} | {a['quantity']} | {a['minStock']} | {label} | {a.get('suggestQty') or '—'} |")
    lines.append("")
    lines.append("补货建议：建议采购数量 ≈ 安全库存（最低库存×2）- 当前库存，可结合近 7 天销量调整。")
    return "\n".join(lines)


def _fmt_top(rows: list, title: str) -> str:
    if not rows:
        return f"{title}：暂无销售数据。"
    lines = [f"{title}（近30天）：", ""]
    for i, r in enumerate(rows, 1):
        lines.append(f"{i}. {r['productName']}（{r['sku']}） 销量 {r['quantity']}，销售额 ¥{r['amount']:.2f}")
    return "\n".join(lines)


def _fmt_slow(rows: list) -> str:
    if not rows:
        return "暂无滞销商品数据。"
    lines = ["滞销商品分析（近30天）：", ""]
    for r in rows:
        cover = f"约 {r['coverDays']} 天" if r.get("coverDays") else "销量为0，无法估算"
        lines.append(
            f"- {r['productName']}：近30天销量 {r['quantity']}，当前库存 {r['stock']}，库存覆盖 {cover}。"
        )
    lines.append("")
    lines.append("建议：对覆盖天数过长的商品可考虑降价促销或加入促销活动。")
    return "\n".join(lines)


async def _run_fallback(session: AgentSession, message: str) -> str:
    text = message.strip()

    # 创新点2：可解释智能补货
    if any(k in text for k in ["补货", "库存风险", "库存预警", "缺货", "要进货"]):
        return await _explainable_restock()

    # 创新点3：预算约束下的补货方案
    if "预算" in text or ("补货方案" in text and "元" in text):
        return await _budget_restock_plan()

    # 创新点5：临期商品处置
    if any(k in text for k in ["过期", "临期", "保质期"]):
        return await _expiry_disposal()

    # 创新点4：供应商智能比较
    if any(k in text for k in ["哪个供应商", "供应商比较", "从哪家", "比价"]):
        return await _supplier_compare_hint(text)

    # 创新点6：异常经营分析
    if any(k in text for k in ["异常", "退款率", "折扣异常", "损耗异常"]):
        return await _anomaly_analysis()

    # 创新点7：AI 经营日报
    if any(k in text for k in ["日报", "今天经营", "今日经营"]):
        return await _daily_report()

    if any(k in text for k in ["卖得不好", "滞销", "卖不动"]):
        rows = await backend.get_slow_products(30, 10)
        return _fmt_slow(rows)

    if any(k in text for k in ["热销", "卖得好", "销量排行", "畅销"]):
        rows = await backend.get_top_products(30, 10)
        return _fmt_top(rows, "热销商品排行")

    if any(k in text for k in ["经营情况", "经营分析", "经营概况", "经营报告"]):
        d = await backend.get_business_summary()
        loss = await backend.get_loss_statistics()
        month_loss = next((m["amount"] for m in loss if m["month"] == loss[-1]["month"]), 0) if loss else 0
        return (
            "经营报告（概览）\n\n"
            f"- 今日销售额：¥{d['todaySales']:.2f}（{d['todayOrders']} 单）\n"
            f"- 本月销售额：¥{d['monthSales']:.2f}（{d['monthOrders']} 单，客单价 ¥{d['monthAvgTicket']}）\n"
            f"- 本月毛利润：¥{d['monthProfit']:.2f}（毛利率 {d['monthMarginRate']}%）\n"
            f"- 本月采购额：¥{d['monthPurchase']:.2f}\n"
            f"- 库存金额：¥{d['inventoryValue']:.2f}，预警商品 {d['alertCount']} 个\n"
            f"- 会员消费占比：{d['memberSalesRatio']}%（会员 {d['memberCount']} 人）\n"
            f"- 本月损耗金额：¥{month_loss:.2f}\n\n"
            "建议：重点关注库存预警商品及时补货，关注毛利与损耗变化。"
        )

    if any(k in text for k in ["促销", "适合做活动"]):
        rows = await backend.get_slow_products(30, 5)
        return _fmt_slow(rows) + "\n\n如需我为某个商品生成促销活动草稿，请回复：为「商品名」创建折扣促销草稿。"

    if "库存" in text:
        rows = await backend.get_inventory_status(None, 20)
        items = rows.get("list", [])
        lines = ["当前库存概览：", ""]
        for i in items[:15]:
            lines.append(f"- {i['productName']}：{i['quantity']} {i['unit']}（预警：{i.get('alertType') or '正常'}）")
        return "\n".join(lines)

    return (
        "我是超市智能运营 Agent（当前为离线规则模式，配置 OPENAI_API_KEY 后可启用完整 LLM 规划能力）。"
        "你可以问我：\n"
        "- 最近有哪些商品需要补货？（输出结论+计算依据）\n"
        "- 我预算5000元，帮我安排补货。\n"
        "- 帮我处理未来7天要过期的商品。\n"
        "- 可口可乐从哪个供应商采购更合适？\n"
        "- 最近有没有异常经营数据？\n"
        "- 生成今日经营日报。\n"
        "- 最近哪些商品卖得不好？\n"
        "- 帮我分析这个月超市的经营情况。"
    )


# ==================== 创新点 2：可解释智能补货 ====================

async def _explainable_restock() -> str:
    alerts = await backend.get_inventory_alerts()
    if not alerts:
        return "当前没有库存预警商品，库存状况良好。"
    trend = await backend.get_sales_summary(7)
    daily_sales = {d["date"]: d["amount"] for d in trend} if isinstance(trend, list) else {}

    lines = ["可解释补货分析（结论 + 数据依据 + 计算过程）", ""]
    for a in alerts[:8]:
        suggest = a.get("suggestQty")
        if not suggest:
            lines.append(f"- {a['productName']}：库存偏高（{a['quantity']}），无需补货。")
            continue
        safety = a["minStock"] * 2
        lines.append(
            f"**{a['productName']}**\n"
            f"- 结论：建议采购 {suggest} 件\n"
            f"- 依据：当前库存 {a['quantity']}，最低库存 {a['minStock']}，安全库存（最低×2）= {safety}\n"
            f"- 计算：建议采购量 = {safety} - {a['quantity']} = {suggest}\n"
        )
    lines.append("以上均为程序化确定性计算；确认后可回复「确认创建采购草稿：供应商ID」生成采购草稿。")
    return "\n".join(lines)


# ==================== 创新点 3：预算约束补货方案 ====================

async def _budget_restock_plan() -> str:
    import re
    alerts = await backend.get_inventory_alerts()
    need = [a for a in alerts if a.get("suggestQty")]
    if not need:
        return "当前没有需要补货的商品。"

    lines = ["预算补货方案（按库存风险优先排序）：", ""]
    total = 0.0
    # 用商品搜索获取采购价（近似：以档案采购价估算）
    for a in need[:10]:
        suggest = a["suggestQty"]
        price = 0.0
        try:
            products = await backend.search_products(a["productName"], 1, 1)
            items = products.get("list", []) if isinstance(products, dict) else []
            if items:
                price = float(items[0].get("purchasePrice") or 0)
        except Exception:
            pass
        cost = round(price * suggest, 2)
        total += cost
        lines.append(f"- {a['productName']} × {suggest}（单价 ¥{price:.2f}）= ¥{cost:.2f}")
    lines.append("")
    lines.append(f"预计总金额：¥{total:.2f}")
    lines.append("如需创建采购草稿，请回复「确认创建采购草稿：供应商ID」。")
    return "\n".join(lines)


# ==================== 创新点 5：临期商品处置 ====================

async def _expiry_disposal() -> str:
    data = await backend.get_expiring_products()
    expired = data.get("expired", [])
    urgent = data.get("urgent", [])
    near = data.get("near", [])

    if not expired and not urgent and not near:
        return "未来 30 天内没有临期批次，状态良好。"

    lines = ["临期商品处置分析（临期 × 库存 × 销售速度 → 处置建议）：", ""]
    for group, label in [(expired, "已过期"), (urgent, "7天内到期"), (near, "30天内到期")]:
        for b in group[:5]:
            lines.append(
                f"- [{label}] {b['productName']}（批次 {b['batchNo']}）："
                f"剩余库存 {b['quantity']}，距过期 {b['daysToExpire']} 天"
            )
    lines.append("")
    lines.append("处置建议：")
    lines.append("1. 对剩余库存>0且销售慢的批次：创建限时折扣或第二件优惠促销（可由我生成促销草稿）")
    lines.append("2. 设置会员专享价加速消化")
    lines.append("3. 暂停对应商品的采购计划")
    lines.append("4. 已过期批次立即在「库存管理-批次」中执行损耗处置（扣减库存并记录损耗）")
    return "\n".join(lines)


# ==================== 创新点 4：供应商智能比较 ====================

async def _supplier_compare_hint(text: str) -> str:
    # 尝试用引号或书名号中的商品名搜索
    import re
    m = re.search(r"[「\"'](.+?)[」\"']", text)
    keyword = m.group(1) if m else None
    product_id = None
    product_name = keyword or ""
    if keyword:
        products = await backend.search_products(keyword, 1, 1)
        items = products.get("list", []) if isinstance(products, dict) else []
        if items:
            product_id = items[0].get("id")
            product_name = items[0].get("name")
    if not product_id:
        return "请告诉我具体商品名称，例如：「可口可乐 330ml 从哪个供应商采购更合适？」"

    rows = await backend.compare_suppliers(product_id)
    if not rows:
        return f"商品「{product_name}」暂未绑定任何供应商，请先在供应商管理中绑定。"

    lines = [f"「{product_name}」供应商比较：", ""]
    for r in rows:
        lines.append(
            f"- {r['supplierName']}：供货价 ¥{float(r['supplyPrice']):.2f}，"
            f"准时完成率 {r['deliveryOnTimeRate']}%，退货率 {r['returnRate']}%，综合评分 {r['score']}"
        )
    lines.append("")
    lines.append("说明：价格最低者排最前；若价差不大，建议优先考虑准时率高、退货率低的供应商。最终由店主决定。")
    return "\n".join(lines)


# ==================== 创新点 6：异常经营分析 ====================

async def _anomaly_analysis() -> str:
    lines = ["异常经营扫描（系统只呈现证据，不做人为违规判断）：", ""]

    # 损耗异常：原因排行
    reasons = await backend.get_loss_reasons(30)
    if reasons:
        top = reasons[0]
        lines.append(f"- 损耗异常：近30天损耗最高原因为「{top['reason']}」，"
                     f"发生 {top['count']} 次，金额 ¥{top['amount']:.2f}")
    # 采购退货频次
    returns = await backend.get_sales_returns(7)
    return_count = len(returns.get("list", [])) if isinstance(returns, dict) else 0
    if return_count > 0:
        lines.append(f"- 采购退货：近期待查退货单 {return_count} 张，请核对退货原因与库存流水")
    # 盘点差异
    try:
        stocktakes = await backend.get_stocktake_history()
        for t in (stocktakes.get("list", []) if isinstance(stocktakes, dict) else [])[:3]:
            diffs = [abs(i["diff"]) for i in (t.get("items") or []) if i.get("diff")]
            big = [d for d in diffs if d >= 10]
            if big:
                lines.append(f"- 盘点偏差：任务 {t['taskNo']} 存在 {len(big)} 个差异≥10 的商品，账实差异较大")
    except Exception:
        pass
    # 库存预警
    alerts = await backend.get_inventory_alerts()
    out = [a for a in alerts if a["alertType"] == "OUT_OF_STOCK"]
    if out:
        names = "、".join(a["productName"] for a in out[:5])
        lines.append(f"- 缺货风险：{len(out)} 个商品缺货（{names}），可能影响销售")
    if len(lines) == 2:
        lines.append("未发现明显异常数据。")
    return "\n".join(lines)


# ==================== 创新点 7：AI 经营日报 ====================

async def _daily_report() -> str:
    d = await backend.get_business_daily_report()
    reasons = await backend.get_loss_reasons(1)
    loss_today = sum(r["amount"] for r in reasons)
    lines = [
        "今日经营日报",
        "",
        f"销售：¥{d['todaySales']:.2f}",
        f"订单：{d['todayOrders']}",
        f"毛利：¥{d['todayProfit']:.2f}",
        "",
        f"库存风险：{d['inventoryAlerts']} 个商品",
        f"临期：{d['expiringCount']} 个批次",
        f"待收货采购单：{d['pendingReceiptOrders']} 张",
        f"今日新增会员：{d['newMembersToday']} 人",
        f"今日损耗：¥{loss_today:.2f}",
        "",
        "建议：",
    ]
    if d["inventoryAlerts"] > 0:
        lines.append(f"1. 处理 {d['inventoryAlerts']} 个库存预警商品（可回复「最近有哪些商品需要补货」查看明细）")
    if d["expiringCount"] > 0:
        lines.append(f"2. 关注 {d['expiringCount']} 个临期批次，必要时创建促销")
    if d["pendingReceiptOrders"] > 0:
        lines.append(f"3. 跟进 {d['pendingReceiptOrders']} 张待收货采购单")
    if len(lines) == 8:
        lines.append("1. 各项指标正常，保持现状即可。")
    return "\n".join(lines)


async def chat(session_id: str, message: str) -> dict:
    session = get_session(session_id)
    try:
        if USE_LLM:
            reply = await _run_llm(session, message)
        else:
            reply = await _run_fallback(session, message)
    except backend.BackendError as e:
        reply = f"调用业务系统失败：{e}"
    except Exception as e:  # noqa: BLE001
        reply = f"Agent 处理失败：{e}"
    return {"reply": reply, "pending": [vars(p) for p in session.pending.values()]}


def add_pending(session_id: str, action: PendingAction) -> None:
    get_session(session_id).pending[action.action_id] = action


def pop_pending(session_id: str, action_id: str) -> PendingAction | None:
    return get_session(session_id).pending.pop(action_id, None)
