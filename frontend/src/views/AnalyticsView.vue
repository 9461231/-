<template>
  <div>
    <!-- KPI 指标卡 -->
    <el-row :gutter="16">
      <el-col :span="6" v-for="card in kpiCards" :key="card.label">
        <div class="kpi-card page-card">
          <div class="kpi-label">{{ card.label }}</div>
          <div class="kpi-value">{{ card.value }}</div>
          <div class="kpi-sub" v-if="card.sub">{{ card.sub }}</div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="14">
        <div class="page-card">
          <h4>销售趋势（近14天）</h4>
          <div ref="trendChartRef" style="height: 320px" />
        </div>
      </el-col>
      <el-col :span="10">
        <div class="page-card">
          <h4>分类销售占比（近30天）</h4>
          <div ref="categoryChartRef" style="height: 320px" />
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="12">
        <div class="page-card">
          <h4>商品销量排行 TOP10（近30天）</h4>
          <div ref="topChartRef" style="height: 320px" />
        </div>
      </el-col>
      <el-col :span="12">
        <div class="page-card">
          <h4>滞销商品（近30天销量最低）</h4>
          <el-table :data="slowList" border size="small" max-height="320">
            <el-table-column prop="productName" label="商品" min-width="130" />
            <el-table-column prop="quantity" label="销量" width="70" align="center" />
            <el-table-column prop="stock" label="库存" width="70" align="center" />
            <el-table-column label="库存覆盖天数" width="110" align="center">
              <template #default="{ row }">{{ row.coverDays ?? '∞' }}</template>
            </el-table-column>
          </el-table>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="12">
        <div class="page-card">
          <h4>采购趋势（近6月）</h4>
          <div ref="purchaseChartRef" style="height: 280px" />
        </div>
      </el-col>
      <el-col :span="12">
        <div class="page-card">
          <h4>损耗统计（近6月）</h4>
          <div ref="lossChartRef" style="height: 280px" />
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="12">
        <div class="page-card">
          <h4>损耗原因排行（近30天）</h4>
          <div ref="reasonChartRef" style="height: 280px" />
        </div>
      </el-col>
      <el-col :span="12">
        <div class="page-card">
          <h4>促销效果分析（近30天，按优惠成本排序）</h4>
          <el-table :data="promoRows" border size="small" max-height="280">
            <el-table-column prop="promotion" label="活动" min-width="130" />
            <el-table-column prop="orderCount" label="订单数" width="80" align="center" />
            <el-table-column label="活动销售额" width="110" align="right">
              <template #default="{ row }">¥{{ row.amount.toFixed(2) }}</template>
            </el-table-column>
            <el-table-column label="优惠成本" width="100" align="right">
              <template #default="{ row }">¥{{ row.discountCost.toFixed(2) }}</template>
            </el-table-column>
          </el-table>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, type Ref } from 'vue'
import * as echarts from 'echarts'
import {
  dashboard, salesTrend, topProducts, slowProducts, categorySales,
  lossStats, purchaseTrend, type Dashboard
} from '@/api/analytics'
import { promotionAnalysis, lossReasons, memberAnalysis } from '@/api/extensions'

const kpi = ref<Dashboard | null>(null)
const slowList = ref<any[]>([])
const promoRows = ref<any[]>([])
const memberInfo = ref<any>(null)
const trendChartRef = ref<HTMLElement>()
const categoryChartRef = ref<HTMLElement>()
const topChartRef = ref<HTMLElement>()
const purchaseChartRef = ref<HTMLElement>()
const lossChartRef = ref<HTMLElement>()
const reasonChartRef = ref<HTMLElement>()

const money = (v: number | undefined | null) =>
  '¥' + (v ?? 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })

const kpiCards = computed(() => [
  { label: '今日销售额', value: money(kpi.value?.todaySales), sub: `${kpi.value?.todayOrders ?? 0} 单` },
  { label: '本月销售额', value: money(kpi.value?.monthSales), sub: `${kpi.value?.monthOrders ?? 0} 单` },
  { label: '本月毛利润', value: money(kpi.value?.monthProfit), sub: `采购 ${money(kpi.value?.monthPurchase)}` },
  {
    label: '库存金额',
    value: money(kpi.value?.inventoryValue),
    sub: `预警商品 ${kpi.value?.alertCount ?? 0} 个`
  },
  {
    label: '会员消费占比',
    value: `${kpi.value?.memberSalesRatio ?? 0}%`,
    sub: `复购率 ${memberInfo.value?.repurchaseRate ?? 0}%`
  },
  { label: '本月损耗', value: money(kpi.value?.monthLoss), sub: '' },
  { label: '本月客单价', value: money((kpi.value as any)?.monthAvgTicket), sub: '月均口径' },
  { label: '本月毛利率', value: `${(kpi.value as any)?.monthMarginRate ?? 0}%`, sub: '' }
])

function renderTrend(data: { date: string; amount: number; orders: number }[]) {
  const chart = echarts.init(trendChartRef.value!)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['销售额', '订单数'] },
    grid: { left: 60, right: 50, bottom: 30, top: 40 },
    xAxis: { type: 'category', data: data.map((d) => d.date.slice(5)) },
    yAxis: [
      { type: 'value', name: '销售额' },
      { type: 'value', name: '订单数' }
    ],
    series: [
      { name: '销售额', type: 'line', smooth: true, areaStyle: { opacity: 0.15 }, data: data.map((d) => d.amount) },
      { name: '订单数', type: 'line', yAxisIndex: 1, smooth: true, data: data.map((d) => d.orders) }
    ]
  })
}

function renderCategory(data: { category: string; amount: number }[]) {
  const chart = echarts.init(categoryChartRef.value!)
  chart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: ¥{c} ({d}%)' },
    series: [
      {
        type: 'pie',
        radius: ['35%', '65%'],
        label: { formatter: '{b}\n{d}%' },
        data: data.map((d) => ({ name: d.category, value: d.amount }))
      }
    ]
  })
}

function renderTop(data: any[]) {
  const chart = echarts.init(topChartRef.value!)
  const sorted = [...data].reverse()
  chart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 110, right: 40, bottom: 30, top: 20 },
    xAxis: { type: 'value' },
    yAxis: { type: 'category', data: sorted.map((d) => d.productName) },
    series: [{ type: 'bar', data: sorted.map((d) => d.quantity), itemStyle: { color: '#409EFF' } }]
  })
}

function renderBar(refEl: Ref<HTMLElement | undefined>, data: { month: string; amount: number }[], color: string) {
  const chart = echarts.init(refEl.value!)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 60, right: 30, bottom: 30, top: 20 },
    xAxis: { type: 'category', data: data.map((d) => d.month) },
    yAxis: { type: 'value' },
    series: [{ type: 'bar', data: data.map((d) => d.amount), itemStyle: { color }, barWidth: '40%' }]
  })
}

onMounted(async () => {
  kpi.value = await dashboard()
  const [trend, cat, top, slow, loss, purchase, promo, reasons, member] = await Promise.all([
    salesTrend(14), categorySales(30), topProducts(30, 10),
    slowProducts(30, 10), lossStats(6), purchaseTrend(6),
    promotionAnalysis(30), lossReasons(30), memberAnalysis(30)
  ])
  slowList.value = slow
  promoRows.value = promo
  memberInfo.value = member
  renderTrend(trend)
  renderCategory(cat)
  renderTop(top)
  renderBar(purchaseChartRef, purchase, '#67C23A')
  renderBar(lossChartRef, loss, '#F56C6C')
  const rc = echarts.init(reasonChartRef.value!)
  rc.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 90, right: 40, bottom: 30, top: 20 },
    xAxis: { type: 'value' },
    yAxis: { type: 'category', data: reasons.map((r) => r.reason).reverse() },
    series: [{ type: 'bar', data: reasons.map((r) => r.amount).reverse(), itemStyle: { color: '#E6A23C' } }]
  })
})
</script>

<style scoped>
.kpi-card {
  text-align: center;
  padding: 18px 12px;
}

.kpi-label {
  color: #909399;
  font-size: 13px;
}

.kpi-value {
  font-size: 22px;
  font-weight: 700;
  color: #303133;
  margin-top: 6px;
}

.kpi-sub {
  color: #c0c4cc;
  font-size: 12px;
  margin-top: 4px;
}

.page-card h4 {
  margin-bottom: 8px;
  color: #303133;
}
</style>
