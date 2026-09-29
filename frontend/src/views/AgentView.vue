<template>
  <div class="page-card agent-page">
    <div class="toolbar">
      <span style="font-weight: 600">AI 智能运营中心</span>
      <el-tag :type="health.mode === 'llm' ? 'success' : 'info'" size="small">
        {{ health.mode === 'llm' ? 'LLM 智能模式' : '离线规则模式' }}
      </el-tag>
      <span style="color: #909399; font-size: 12px">
        只读分析可直接执行；写操作（采购草稿/促销草稿/库存调整）需人工确认
      </span>
      <div style="flex: 1" />
      <el-button @click="clearChat">清空对话</el-button>
    </div>

    <div class="chat-box" ref="chatBoxRef">
      <div v-for="(msg, idx) in messages" :key="idx" :class="['msg', msg.role]">
        <div class="bubble">
          <div v-if="msg.role === 'agent'" v-html="renderMarkdown(msg.content)" />
          <template v-else>{{ msg.content }}</template>
        </div>
        <div v-if="msg.pending?.length" class="pending">
          <div v-for="p in msg.pending" :key="p.action_id" class="pending-card">
            <div style="font-weight: 600">待确认操作：{{ pendingLabel(p.kind) }}</div>
            <div style="margin: 6px 0; color: #606266; font-size: 13px">{{ p.summary }}</div>
            <el-button type="primary" size="small" @click="handleApprove(p)">确认执行</el-button>
            <el-button size="small" @click="handleCancel(p)">取消</el-button>
          </div>
        </div>
      </div>
      <div v-if="thinking" class="msg agent">
        <div class="bubble">思考中…</div>
      </div>
    </div>

    <div class="suggestions">
      <el-button v-for="s in suggestions" :key="s" size="small" round @click="send(s)">{{ s }}</el-button>
    </div>

    <div style="display: flex; gap: 8px; margin-top: 10px">
      <el-input v-model="input" placeholder="例如：最近有哪些商品需要补货？" @keyup.enter="send()" />
      <el-button type="primary" :loading="thinking" @click="send()">发送</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, ref } from 'vue'
import { agentChat, agentHealth, approveAction, cancelAction, type PendingAction } from '@/api/agent'

interface Msg {
  role: 'user' | 'agent'
  content: string
  pending?: PendingAction[]
}

const sessionId = ref('web-' + Math.random().toString(36).slice(2, 8))
const messages = ref<Msg[]>([
  {
    role: 'agent',
    content:
      '您好，我是超市智能运营 Agent。我可以帮您：\n- 分析库存风险与补货建议\n- 滞销商品分析\n- 热销排行\n- 经营报告\n- 生成采购/促销草稿（需人工确认）'
  }
])
const input = ref('')
const thinking = ref(false)
const health = ref<{ status: string; mode: string; backend: string }>({ status: '-', mode: '-', backend: '-' })
const chatBoxRef = ref<HTMLElement>()

const suggestions = [
  '最近有哪些商品需要补货？',
  '最近哪些商品卖得不好？',
  '热销商品有哪些？',
  '帮我分析这个月超市的经营情况。',
  '哪些商品适合做促销？'
]

function pendingLabel(kind: string) {
  return kind === 'purchase_draft' ? '创建采购订单草稿'
    : kind === 'promotion_draft' ? '创建促销活动草稿'
    : kind === 'adjust_inventory' ? '库存调整' : kind
}

function renderMarkdown(text: string) {
  return text
    .replace(/&/g, '&amp;').replace(/</g, '&lt;')
    .replace(/\n/g, '<br/>')
    .replace(/\*\*(.+?)\*\*/g, '<b>$1</b>')
}

function scrollToBottom() {
  nextTick(() => {
    chatBoxRef.value?.scrollTo({ top: chatBoxRef.value.scrollHeight, behavior: 'smooth' })
  })
}

async function send(text?: string) {
  const message = (text ?? input.value).trim()
  if (!message || thinking.value) return
  input.value = ''
  messages.value.push({ role: 'user', content: message })
  thinking.value = true
  scrollToBottom()
  try {
    const resp = await agentChat(sessionId.value, message)
    messages.value.push({ role: 'agent', content: resp.reply, pending: resp.pending })
  } finally {
    thinking.value = false
    scrollToBottom()
  }
}

async function handleApprove(p: PendingAction) {
  const resp = await approveAction(sessionId.value, p.action_id)
  messages.value.push({ role: 'agent', content: resp.reply })
  scrollToBottom()
}

async function handleCancel(p: PendingAction) {
  const resp = await cancelAction(sessionId.value, p.action_id)
  messages.value.push({ role: 'agent', content: resp.reply })
  scrollToBottom()
}

function clearChat() {
  sessionId.value = 'web-' + Math.random().toString(36).slice(2, 8)
  messages.value = messages.value.slice(0, 1)
}

onMounted(async () => {
  try {
    health.value = await agentHealth()
  } catch {
    health.value = { status: 'unreachable', mode: '-', backend: '-' }
  }
})
</script>

<style scoped>
.agent-page {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 120px);
}

.chat-box {
  flex: 1;
  overflow-y: auto;
  background: #f5f7fa;
  border-radius: 6px;
  padding: 12px;
}

.msg {
  display: flex;
  margin-bottom: 10px;
}

.msg.user {
  justify-content: flex-end;
}

.msg.agent {
  justify-content: flex-start;
}

.bubble {
  max-width: 72%;
  padding: 8px 12px;
  border-radius: 8px;
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-wrap;
}

.msg.user .bubble {
  background: #1677ff;
  color: #fff;
}

.msg.agent .bubble {
  background: #fff;
  border: 1px solid #e8e8e8;
}

.pending {
  width: 100%;
  margin-top: 4px;
}

.pending-card {
  background: #fdf6ec;
  border: 1px solid #faecd8;
  border-radius: 6px;
  padding: 10px;
  margin-top: 6px;
  max-width: 72%;
}

.suggestions {
  margin-top: 10px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
</style>
