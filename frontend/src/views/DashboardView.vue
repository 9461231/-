<template>
  <div>
    <el-row :gutter="16">
      <el-col :span="6">
        <div class="kpi page-card primary">
          <div class="label">今日销售额</div>
          <div class="value">¥{{ fmt(d.todaySales) }}</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="kpi page-card">
          <div class="label">今日订单</div>
          <div class="value">{{ d.todayOrders }}</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="kpi page-card success">
          <div class="label">今日毛利</div>
          <div class="value">¥{{ fmt(d.todayProfit) }}</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="kpi page-card">
          <div class="label">今日新增会员</div>
          <div class="value">{{ d.newMembersToday }}</div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="6" v-for="risk in riskCards" :key="risk.label">
        <div class="page-card risk" :class="risk.type" @click="risk.to && router.push(risk.to)"
          style="cursor: pointer">
          <div class="label">{{ risk.label }}</div>
          <div class="value">{{ risk.value }}</div>
          <div class="hint">{{ risk.hint }}</div>
        </div>
      </el-col>
    </el-row>

    <div class="page-card" style="margin-top: 16px">
      <h4>快捷入口</h4>
      <el-space wrap>
        <el-button @click="router.push('/sales')">🧾 去收银</el-button>
        <el-button @click="router.push('/purchase')">📦 采购管理</el-button>
        <el-button @click="router.push('/inventory')">🏪 库存与临期</el-button>
        <el-button @click="router.push('/agent')">🤖 问 AI 助手</el-button>
        <el-button @click="router.push('/analytics')">📊 经营分析</el-button>
      </el-space>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { cockpit } from '@/api/extensions'
import { hasRole } from '@/stores/auth'

const router = useRouter()
const d = reactive({
  todaySales: 0,
  todayOrders: 0,
  todayProfit: 0,
  inventoryAlerts: 0,
  expiringCount: 0,
  pendingReceiptOrders: 0,
  newMembersToday: 0
})

const fmt = (v?: number) =>
  (v ?? 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })

const riskCards = computed(() => [
  { label: '库存预警商品', value: d.inventoryAlerts, hint: '缺货/低于最低库存', to: '/inventory', type: hasRole() ? 'warning' : '' },
  { label: '临期批次', value: d.expiringCount, hint: '30天内到期', to: '/inventory', type: 'danger' },
  { label: '待收货采购单', value: d.pendingReceiptOrders, hint: '已审核待入库', to: '/purchase', type: 'primary' },
  { label: '今日损耗', value: '—', hint: '见盘点与损耗页', to: '/stocktake', type: 'info' }
])

onMounted(async () => {
  const data = await cockpit()
  Object.assign(d, data)
})
</script>

<style scoped>
.kpi .label,
.risk .label {
  color: #909399;
  font-size: 13px;
}

.kpi .value,
.risk .value {
  font-size: 24px;
  font-weight: 700;
  margin-top: 6px;
  color: #303133;
}

.primary .value {
  color: #1677ff;
}

.success .value {
  color: #67c23a;
}

.risk.danger .value {
  color: #f56c6c;
}

.risk.warning .value {
  color: #e6a23c;
}

.hint {
  color: #c0c4cc;
  font-size: 12px;
  margin-top: 4px;
}

.page-card h4 {
  margin-bottom: 10px;
}
</style>
