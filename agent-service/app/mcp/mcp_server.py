"""
MCP Tool Layer：通过 MCP（Model Context Protocol）向 Agent 暴露超市业务工具。
- 只读工具：直接执行
- 写操作工具：create_purchase_draft / create_promotion_draft / adjust_inventory
  只生成草稿或标记待确认的操作，人工确认后才真正生效（Human-in-the-Loop）

运行方式（streamable HTTP）：
    uvicorn app.mcp.mcp_server:mcp_app --port 8200
"""
import json
from mcp.server.fastmcp import FastMCP

from app.services import backend_client as backend

mcp = FastMCP("supermarket-operations")

mcp_app = mcp.streamable_http_app()


def _dump(data) -> str:
    return json.dumps(data, ensure_ascii=False, default=str)


# ==================== 只读工具 ====================

@mcp.tool()
async def search_product(keyword: str = "", page: int = 1, size: int = 10) -> str:
    """按关键字（名称/SKU/条码/品牌）分页查询商品"""
    return _dump(await backend.search_products(keyword or None, page, size))


@mcp.tool()
async def get_product_detail(product_id: int) -> str:
    """查询商品详情（含分类、价格、库存阈值）"""
    return _dump(await backend.get_product_detail(product_id))


@mcp.tool()
async def get_sales_summary(days: int = 14) -> str:
    """查询最近 N 天销售趋势汇总（每日销售额与订单数）"""
    return _dump(await backend.get_sales_summary(days))


@mcp.tool()
async def get_sales_trend(days: int = 14) -> str:
    """查询销售趋势（与销售汇总相同口径，供趋势分析）"""
    return _dump(await backend.get_sales_trend(days))


@mcp.tool()
async def get_top_products(days: int = 30, limit: int = 10) -> str:
    """查询热销商品排行（近 N 天销量 TOP）"""
    return _dump(await backend.get_top_products(days, limit))


@mcp.tool()
async def get_slow_products(days: int = 30, limit: int = 10) -> str:
    """查询滞销商品（近 N 天销量低且有库存，含库存覆盖天数）"""
    return _dump(await backend.get_slow_products(days, limit))


@mcp.tool()
async def get_inventory_status(keyword: str = "", size: int = 20) -> str:
    """查询当前库存（支持关键字过滤，含预警状态与库存金额）"""
    return _dump(await backend.get_inventory_status(keyword or None, size))


@mcp.tool()
async def get_inventory_alerts() -> str:
    """查询库存预警：缺货 / 低于最低库存 / 高于最高库存，含建议补货数量"""
    return _dump(await backend.get_inventory_alerts())


@mcp.tool()
async def get_supplier_info(keyword: str = "") -> str:
    """查询供应商列表（名称/联系人/电话/状态/合作商品数）"""
    return _dump(await backend.get_supplier_info(keyword or None))


@mcp.tool()
async def get_supplier_price(supplier_id: int) -> str:
    """查询某供应商的合作商品与供货价"""
    return _dump(await backend.get_supplier_products(supplier_id))


@mcp.tool()
async def get_business_summary() -> str:
    """查询经营概况 KPI（今日/本月销售、采购额、毛利、库存金额、会员占比、损耗）"""
    return _dump(await backend.get_business_summary())


@mcp.tool()
async def get_profit_analysis() -> str:
    """查询利润分析（毛利、销售额、采购额等经营口径）"""
    return _dump(await backend.get_profit_analysis())


@mcp.tool()
async def get_product_price(product_id: int) -> str:
    """查询商品价格档案（采购价/销售价/会员价/最低销售价）"""
    return _dump(await backend.get_product_price(product_id))


@mcp.tool()
async def get_expiring_products() -> str:
    """查询临期/过期批次（expired=已过期, urgent=7天内, near=30天内）"""
    return _dump(await backend.get_expiring_products())


@mcp.tool()
async def get_inventory_turnover() -> str:
    """查询库存周转分析（库存金额/30天销售成本/周转天数）"""
    return _dump(await backend.get_inventory_turnover())


@mcp.tool()
async def get_purchase_history() -> str:
    """查询最近采购订单历史"""
    return _dump(await backend.get_purchase_history())


@mcp.tool()
async def get_member_summary() -> str:
    """查询会员分析（数量/占比/客单价/复购率）"""
    return _dump(await backend.get_member_summary())


@mcp.tool()
async def get_active_promotions() -> str:
    """查询促销活动列表（含状态与规则）"""
    return _dump(await backend.get_active_promotions())


@mcp.tool()
async def get_business_daily_report() -> str:
    """查询经营驾驶舱数据（今日销售/毛利/库存预警/临期/待收货/新增会员）"""
    return _dump(await backend.get_business_daily_report())


@mcp.tool()
async def get_loss_analysis(days: int = 30) -> str:
    """查询损耗原因排行与金额"""
    return _dump(await backend.get_loss_reasons(days))


@mcp.tool()
async def get_supplier_comparison(product_id: int) -> str:
    """供应商智能比较：同一商品不同供应商的价格/准时率/退货率/评分对比"""
    return _dump(await backend.compare_suppliers(product_id))


# ==================== 写操作工具（草稿，人工确认后生效） ====================

@mcp.tool()
async def create_purchase_draft(supplier_id: int, items_json: str, remark: str = "") -> str:
    """创建采购订单草稿。items_json 为 JSON 数组字符串，元素形如 {"productId":1,"quantity":100,"purchasePrice":3.5}。
    草稿不会立即进入采购流程，需人工在前端"采购管理"中提交审核。"""
    items = json.loads(items_json)
    result = await backend.create_purchase_draft(supplier_id, items, remark or None)
    return _dump({"message": "采购订单草稿已创建，等待人工提交审核", "order": result})


@mcp.tool()
async def create_promotion_draft(payload_json: str) -> str:
    """创建促销活动草稿（默认停用）。payload_json 为 JSON 对象字符串，
    形如 {"name":"薯片促销","type":1,"startTime":"...","endTime":"...","discountRate":0.8,"productIds":[1,2]}。
    需人工在前端"促销管理"中确认并启用。"""
    payload = json.loads(payload_json)
    result = await backend.create_promotion_draft(payload)
    return _dump({"message": "促销活动草稿已创建（默认停用），等待人工确认启用", "promotion": result})


@mcp.tool()
async def adjust_inventory(product_id: int, change_qty: int, reason: str) -> str:
    """库存调整（正数盘盈/增加，负数盘亏/减少）。写操作会直接生效，请谨慎调用并填写明确原因。"""
    result = await backend.adjust_inventory(product_id, change_qty, reason)
    return _dump({"message": "库存调整完成", "result": result})
