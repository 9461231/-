"""Spring Boot 业务后端 HTTP 客户端：所有工具最终通过业务 API 访问数据（不直接操作数据库）"""
import os
import httpx
from typing import Any

BASE_URL = os.getenv("BACKEND_BASE_URL", "http://localhost:8080/api")


class BackendError(Exception):
    pass


async def _request(method: str, path: str, **kwargs) -> Any:
    async with httpx.AsyncClient(timeout=15) as client:
        resp = await client.request(method, f"{BASE_URL}{path}", **kwargs)
    if resp.status_code >= 400:
        detail = None
        try:
            detail = resp.json().get("message")
        except Exception:
            detail = resp.text
        raise BackendError(f"后端返回 {resp.status_code}: {detail}")
    body = resp.json()
    if isinstance(body, dict) and "code" in body:
        if body["code"] != 0:
            raise BackendError(body.get("message", "业务错误"))
        return body.get("data")
    return body


# ==================== 只读工具的数据接口 ====================

async def search_products(keyword: str | None = None, page: int = 1, size: int = 10) -> Any:
    params = {"page": page, "size": size}
    if keyword:
        params["keyword"] = keyword
    return await _request("GET", "/products", params=params)


async def get_product_detail(product_id: int) -> Any:
    return await _request("GET", f"/products/{product_id}")


async def get_product_categories() -> Any:
    return await _request("GET", "/product-categories")


async def get_inventory_status(keyword: str | None = None, size: int = 20) -> Any:
    params = {"page": 1, "size": size}
    if keyword:
        params["keyword"] = keyword
    return await _request("GET", "/inventory", params=params)


async def get_inventory_alerts() -> Any:
    return await _request("GET", "/inventory/alerts")


async def get_sales_summary(days: int = 14) -> Any:
    return await _request("GET", "/analytics/sales-trend", params={"days": days})


async def get_top_products(days: int = 30, limit: int = 10) -> Any:
    return await _request("GET", "/analytics/top-products", params={"days": days, "limit": limit})


async def get_slow_products(days: int = 30, limit: int = 10) -> Any:
    return await _request("GET", "/analytics/slow-products", params={"days": days, "limit": limit})


async def get_sales_trend(days: int = 14) -> Any:
    return await _request("GET", "/analytics/sales-trend", params={"days": days})


async def get_supplier_info(keyword: str | None = None) -> Any:
    params = {"page": 1, "size": 20}
    if keyword:
        params["keyword"] = keyword
    return await _request("GET", "/suppliers", params=params)


async def get_supplier_products(supplier_id: int) -> Any:
    return await _request("GET", f"/suppliers/{supplier_id}/products")


async def get_business_summary() -> Any:
    return await _request("GET", "/analytics/dashboard")


async def get_profit_analysis() -> Any:
    return await _request("GET", "/analytics/dashboard")


async def get_monthly_purchase() -> Any:
    return await _request("GET", "/purchase-returns/stats/month")


async def get_loss_statistics() -> Any:
    return await _request("GET", "/analytics/loss-stats", params={"months": 6})


# ==================== 创新点扩展数据接口 ====================

async def get_expiring_products() -> Any:
    """临期/过期批次（expired/urgent/near 三组）"""
    return await _request("GET", "/batches/expiry-alerts")


async def get_product_price(product_id: int) -> Any:
    detail = await _request("GET", f"/products/{product_id}")
    return {
        "productId": detail.get("id"),
        "name": detail.get("name"),
        "purchasePrice": detail.get("purchasePrice"),
        "salePrice": detail.get("salePrice"),
        "memberPrice": detail.get("memberPrice"),
        "minSalePrice": detail.get("minSalePrice"),
    }


async def get_supplier_stats(supplier_id: int) -> Any:
    return await _request("GET", f"/suppliers/{supplier_id}/stats")


async def compare_suppliers(product_id: int) -> Any:
    return await _request("GET", "/suppliers/compare", params={"productId": product_id})


async def get_purchase_history() -> Any:
    return await _request("GET", "/purchase-orders", params={"page": 1, "size": 50})


async def get_inventory_turnover() -> Any:
    return await _request("GET", "/analytics/inventory-analysis")


async def get_member_summary() -> Any:
    return await _request("GET", "/analytics/member-analysis")


async def get_active_promotions() -> Any:
    return await _request("GET", "/promotions", params={"page": 1, "size": 50})


async def get_business_daily_report() -> Any:
    return await _request("GET", "/analytics/cockpit")


async def get_loss_reasons(days: int = 30) -> Any:
    return await _request("GET", "/analytics/loss-reasons", params={"days": days})


async def get_sales_returns(days: int = 7) -> Any:
    """近期退货（异常分析用）"""
    data = await _request("GET", "/purchase-returns", params={"page": 1, "size": 20})
    return data


async def get_stocktake_history() -> Any:
    return await _request("GET", "/stocktakes", params={"page": 1, "size": 10})


# ==================== 写操作（草稿 + 人工确认） ====================

async def create_purchase_draft(supplier_id: int, items: list[dict], remark: str | None = None) -> Any:
    """创建采购订单草稿（status=0），人工确认后再走提交/审核流程"""
    payload = {"supplierId": supplier_id, "items": items}
    if remark:
        payload["remark"] = remark
    return await _request("POST", "/purchase-orders", json=payload)


async def create_promotion_draft(payload: dict) -> Any:
    """创建促销活动草稿（默认停用，人工确认后启用）"""
    return await _request("POST", "/promotions", json=payload)


async def adjust_inventory(product_id: int, change_qty: int, reason: str) -> Any:
    return await _request(
        "POST", "/inventory/adjust",
        json={"productId": product_id, "changeQty": change_qty, "reason": reason},
    )
