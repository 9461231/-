"""
AI 智能运营 Agent 服务入口

    uvicorn main:app --host 0.0.0.0 --port 8100

接口：
    POST /api/agent/chat                 自然语言对话
    POST /api/agent/actions/{id}/approve 人工确认执行写操作（Human Approval）
    POST /api/agent/actions/{id}/cancel  取消待确认操作
    GET  /api/agent/health               健康检查
"""
import os
import uuid
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from dotenv import load_dotenv

load_dotenv()

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel

from app.agents import operations_agent as agent
from app.services import backend_client as backend

app = FastAPI(title="Supermarket Operations Agent", version="1.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)


class ChatRequest(BaseModel):
    message: str
    sessionId: str = "default"


class ActionApproval(BaseModel):
    sessionId: str = "default"


@app.get("/api/agent/health")
async def health():
    return {
        "status": "ok",
        "mode": "llm" if agent.USE_LLM else "fallback",
        "backend": backend.BASE_URL,
    }


@app.post("/api/agent/chat")
async def chat(req: ChatRequest):
    return await agent.chat(req.sessionId, req.message)


@app.post("/api/agent/actions/{action_id}/approve")
async def approve(action_id: str, req: ActionApproval):
    action = agent.pop_pending(req.sessionId, action_id)
    if action is None:
        return {"reply": "未找到该待确认操作，可能已被处理或取消。"}
    try:
        if action.kind == "purchase_draft":
            result = await backend.create_purchase_draft(
                action.payload["supplierId"], action.payload["items"], action.payload.get("remark")
            )
            return {"reply": f"已确认：采购订单草稿 {result['orderNo']} 创建成功，请到“采购管理”提交审核。"}
        if action.kind == "promotion_draft":
            result = await backend.create_promotion_draft(action.payload)
            return {"reply": f"已确认：促销活动「{result['name']}」草稿创建成功（默认停用），请到“促销管理”确认启用。"}
        if action.kind == "adjust_inventory":
            await backend.adjust_inventory(
                action.payload["productId"], action.payload["changeQty"], action.payload["reason"]
            )
            return {"reply": "已确认：库存调整完成。"}
        return {"reply": "未知操作类型。"}
    except backend.BackendError as e:
        return {"reply": f"执行失败：{e}"}


@app.post("/api/agent/actions/{action_id}/cancel")
async def cancel(action_id: str, req: ActionApproval):
    action = agent.pop_pending(req.sessionId, action_id)
    if action is None:
        return {"reply": "未找到该待确认操作。"}
    return {"reply": "已取消该操作，未对业务系统做任何修改。"}


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(app, host="0.0.0.0", port=int(os.getenv("AGENT_PORT", "8100")))
