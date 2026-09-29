<template>
  <div class="page-card">
    <el-tabs v-model="activeTab">
      <!-- 收银 -->
      <el-tab-pane label="收银结算" name="pos">
        <el-row :gutter="16">
          <el-col :span="15">
            <div class="toolbar">
              <el-input v-model="posKeyword" placeholder="搜索商品：名称 / SKU / 条码" clearable style="width: 280px"
                @keyup.enter="loadPosProducts" @clear="loadPosProducts" />
              <el-button type="primary" @click="loadPosProducts">搜索</el-button>
            </div>
            <el-table :data="posProducts" v-loading="posLoading" border size="small" max-height="420">
              <el-table-column prop="sku" label="SKU" width="110" />
              <el-table-column prop="name" label="商品" min-width="150" />
              <el-table-column prop="spec" label="规格" width="110" show-overflow-tooltip />
              <el-table-column label="售价" width="90" align="right">
                <template #default="{ row }">¥{{ row.salePrice?.toFixed(2) }}</template>
              </el-table-column>
              <el-table-column label="操作" width="90" align="center">
                <template #default="{ row }">
                  <el-button link type="primary" @click="addToCart(row)">加入</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-col>
          <el-col :span="9">
            <div style="border: 1px solid #ebeef5; border-radius: 6px; padding: 12px">
              <div class="toolbar" style="margin-bottom: 8px">
                <span style="font-weight: 600">购物车</span>
                <div style="flex: 1" />
                <el-button link type="danger" @click="cart = []">清空</el-button>
              </div>
              <el-select v-model="memberId" filterable clearable placeholder="识别会员（可选）" style="width: 100%; margin-bottom: 8px">
                <el-option v-for="m in members" :key="m.id" :label="`${m.phone} ${m.name}`" :value="m.id" />
              </el-select>
              <el-input v-model="couponCode" clearable placeholder="优惠券码（可选，会员结算时可用）"
                style="margin-bottom: 8px" maxlength="32" />
              <el-table :data="cart" border size="small" max-height="240">
                <el-table-column prop="name" label="商品" min-width="100" />
                <el-table-column label="数量" width="130" align="center">
                  <template #default="{ row }">
                    <el-input-number v-model="row.quantity" :min="1" size="small" style="width: 110px" />
                  </template>
                </el-table-column>
                <el-table-column label="小计" width="80" align="right">
                  <template #default="{ row }">¥{{ (row.salePrice * row.quantity).toFixed(2) }}</template>
                </el-table-column>
                <el-table-column label="" width="60" align="center">
                  <template #default="{ $index }">
                    <el-button link type="danger" @click="cart.splice($index, 1)">删除</el-button>
                  </template>
                </el-table-column>
              </el-table>
              <div style="margin-top: 10px">
                <el-radio-group v-model="payMethod">
                  <el-radio-button v-for="(label, key) in POS_PAY" :key="key" :value="key"
                    :disabled="key === 'BALANCE' && !memberId">{{ label }}</el-radio-button>
                </el-radio-group>
              </div>
              <div style="margin: 10px 0; display: flex; justify-content: space-between; font-size: 15px">
                <span>合计：<b style="color: #f56c6c; font-size: 20px">¥{{ cartTotal.toFixed(2) }}</b></span>
                <span style="color: #909399; font-size: 13px">共 {{ cart.reduce((s, i) => s + i.quantity, 0) }} 件</span>
              </div>
              <el-button type="primary" size="large" style="width: 100%" :loading="checkoutLoading"
                :disabled="!cart.length" @click="handleCheckout">结算</el-button>
            </div>
          </el-col>
        </el-row>
      </el-tab-pane>

      <!-- 销售订单 -->
      <el-tab-pane label="销售订单" name="orders">
        <div class="toolbar">
          <el-input v-model="query.keyword" placeholder="搜索订单号" clearable style="width: 200px" @keyup.enter="loadOrders" />
          <el-button type="primary" @click="loadOrders">查询</el-button>
        </div>
        <el-table :data="orders" v-loading="orderLoading" border stripe>
          <el-table-column prop="orderNo" label="订单号" width="210" />
          <el-table-column prop="memberName" label="会员" width="100">
            <template #default="{ row }">{{ row.memberName || '散客' }}</template>
          </el-table-column>
          <el-table-column label="原价" width="100" align="right">
            <template #default="{ row }">¥{{ row.totalAmount.toFixed(2) }}</template>
          </el-table-column>
          <el-table-column label="优惠" width="90" align="right">
            <template #default="{ row }">-¥{{ row.discountAmount.toFixed(2) }}</template>
          </el-table-column>
          <el-table-column label="实收" width="110" align="right">
            <template #default="{ row }"><b>¥{{ row.payableAmount.toFixed(2) }}</b></template>
          </el-table-column>
          <el-table-column label="支付" width="90" align="center">
            <template #default="{ row }">{{ PAY_METHODS[row.payMethod] ?? row.payMethod }}</template>
          </el-table-column>
          <el-table-column prop="pointsEarned" label="积分" width="80" align="center" />
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'warning'">{{ row.statusLabel }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="时间" width="170" />
          <el-table-column label="操作" width="140" align="center" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openDetail(row)">详情</el-button>
              <el-button v-if="row.status !== 3" link type="danger" @click="openReturn(row)">退货</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="pagination">
          <el-pagination v-model:current-page="query.page" v-model:page-size="query.size"
            :total="total" layout="total, prev, pager, next" @current-change="loadOrders" />
        </div>
      </el-tab-pane>

      <!-- 交班与日结 -->
      <el-tab-pane label="交班与日结" name="shift">
        <el-row :gutter="16">
          <el-col :span="12">
            <div class="page-card" style="margin-bottom: 16px">
              <h4>当前班次</h4>
              <template v-if="currentShiftData">
                <el-descriptions :column="2" border size="small">
                  <el-descriptions-item label="收银员">{{ currentShiftData.cashierName }}</el-descriptions-item>
                  <el-descriptions-item label="开班时间">{{ currentShiftData.startTime }}</el-descriptions-item>
                </el-descriptions>
                <el-button type="warning" style="margin-top: 10px" @click="handleCloseShift">交班结算</el-button>
              </template>
              <template v-else>
                <el-empty description="尚未开班" :image-size="60" />
                <el-button type="primary" @click="handleOpenShift">开班</el-button>
              </template>
            </div>

            <div class="page-card">
              <h4>营业日结单</h4>
              <el-date-picker v-model="settleDate" type="date" value-format="YYYY-MM-DD" @change="loadSettlement" style="margin-bottom: 10px" />
              <template v-if="settlement">
                <el-descriptions :column="2" border size="small">
                  <el-descriptions-item label="订单数">{{ settlement.orderCount }}</el-descriptions-item>
                  <el-descriptions-item label="销售额">¥{{ settlement.totalAmount.toFixed(2) }}</el-descriptions-item>
                  <el-descriptions-item label="优惠合计">¥{{ settlement.discountAmount.toFixed(2) }}</el-descriptions-item>
                  <el-descriptions-item v-for="(v, k) in settlement.byMethod" :key="k" :label="PAY_METHODS[String(k)] ?? k">
                    ¥{{ (v as number).toFixed(2) }}
                  </el-descriptions-item>
                </el-descriptions>
              </template>
            </div>
          </el-col>

          <el-col :span="12">
            <div class="page-card">
              <h4>交班记录</h4>
              <el-table :data="shifts" border size="small" max-height="420">
                <el-table-column prop="cashierName" label="收银员" width="90" />
                <el-table-column prop="startTime" label="开班" width="150" />
                <el-table-column prop="endTime" label="交班" width="150" />
                <el-table-column prop="orderCount" label="单数" width="60" align="center" />
                <el-table-column label="销售额" width="100" align="right">
                  <template #default="{ row }">¥{{ row.totalAmount.toFixed(2) }}</template>
                </el-table-column>
                <el-table-column label="优惠" width="90" align="right">
                  <template #default="{ row }">¥{{ row.discountAmount.toFixed(2) }}</template>
                </el-table-column>
                <el-table-column prop="remark" label="备注" min-width="90" show-overflow-tooltip />
              </el-table>
            </div>
          </el-col>
        </el-row>
      </el-tab-pane>
    </el-tabs>

    <!-- 订单详情 -->
    <el-drawer v-model="detailVisible" title="订单详情" size="560px">
      <template v-if="detail">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="订单号">{{ detail.order.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="会员">{{ detail.order.memberName || '散客' }}</el-descriptions-item>
          <el-descriptions-item label="原价">¥{{ detail.order.totalAmount.toFixed(2) }}</el-descriptions-item>
          <el-descriptions-item label="优惠">-¥{{ detail.order.discountAmount.toFixed(2) }}</el-descriptions-item>
          <el-descriptions-item label="实收">¥{{ detail.order.payableAmount.toFixed(2) }}</el-descriptions-item>
          <el-descriptions-item label="积分">+{{ detail.order.pointsEarned }}</el-descriptions-item>
        </el-descriptions>
        <el-table :data="detail.items" border size="small" style="margin-top: 12px">
          <el-table-column prop="productName" label="商品" min-width="130" />
          <el-table-column prop="quantity" label="数量" width="60" align="center" />
          <el-table-column prop="unitPrice" label="单价" width="80" align="right" />
          <el-table-column prop="discountAmount" label="优惠" width="80" align="right" />
          <el-table-column prop="amount" label="实收" width="80" align="right" />
          <el-table-column prop="returnedQuantity" label="已退" width="60" align="center" />
        </el-table>
      </template>
    </el-drawer>

    <!-- 退货对话框 -->
    <el-dialog v-model="returnVisible" title="销售退货（库存将回增）" width="560px" :close-on-click-modal="false">
      <el-table :data="returnItems" border size="small">
        <el-table-column prop="productName" label="商品" min-width="140" />
        <el-table-column prop="quantity" label="购买" width="70" align="center" />
        <el-table-column prop="returnedQuantity" label="已退" width="70" align="center" />
        <el-table-column label="本次退货" width="150" align="center">
          <template #default="{ row }">
            <el-input-number v-model="row.returnQty" :min="0" :max="row.quantity - row.returnedQuantity"
              size="small" style="width: 120px" />
          </template>
        </el-table-column>
      </el-table>
      <el-input v-model="returnReason" placeholder="退货原因" style="margin-top: 10px" maxlength="255" />
      <template #footer>
        <el-button @click="returnVisible = false">取消</el-button>
        <el-button type="danger" :loading="checkoutLoading" @click="handleReturn">确认退货</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { PageVO, Product } from '@/types/product'
import {
  checkout, pageSalesOrders, getSalesOrder, createSalesReturn,
  PAY_METHODS, type SalesOrder, type SalesOrderItem
} from '@/api/sales'
import {
  openShift, currentShift, closeShift, shiftHistory, dailySettlement,
  type CashShift
} from '@/api/extensions'
import { pageProducts } from '@/api/product'
import { pageMembers, type Member } from '@/api/member'

const activeTab = ref('pos')
const posKeyword = ref('')
const posProducts = ref<Product[]>([])
const posLoading = ref(false)
const members = ref<Member[]>([])
const memberId = ref<number | undefined>()
const payMethod = ref('CASH')
const checkoutLoading = ref(false)

const cart = ref<{ productId: number; name: string; salePrice: number; quantity: number }[]>([])
const cartTotal = computed(() => cart.value.reduce((s, i) => s + i.salePrice * i.quantity, 0))
const couponCode = ref('')
const POS_PAY = { ...PAY_METHODS, BALANCE: '储值' }

// 交班/日结
const currentShiftData = ref<CashShift | null>(null)
const shifts = ref<CashShift[]>([])
const settleDate = ref(new Date().toISOString().slice(0, 10))
const settlement = ref<any>(null)

async function loadShiftPanel() {
  currentShiftData.value = await currentShift()
  shifts.value = await shiftHistory()
  await loadSettlement()
}

async function loadSettlement() {
  settlement.value = await dailySettlement(settleDate.value || undefined)
}

async function handleOpenShift() {
  currentShiftData.value = await openShift()
  ElMessage.success('已开班，开始收银吧')
  loadShiftPanel()
}

async function handleCloseShift() {
  const ok = await ElMessageBox.confirm('交班将汇总本班次销售数据并结束班次，确认？', '交班', { type: 'warning' }).catch(() => false)
  if (!ok) return
  await closeShift(currentShiftData.value!.id)
  ElMessage.success('交班完成')
  loadShiftPanel()
}

const orders = ref<SalesOrder[]>([])
const total = ref(0)
const orderLoading = ref(false)
const query = reactive({ page: 1, size: 10, keyword: '' })

const detailVisible = ref(false)
const detail = ref<{ order: SalesOrder; items: SalesOrderItem[] } | null>(null)

const returnVisible = ref(false)
const returnOrderId = ref(0)
const returnItems = ref<any[]>([])
const returnReason = ref('')

async function loadPosProducts() {
  posLoading.value = true
  try {
    const data: PageVO<Product> = await pageProducts({
      page: 1, size: 30, keyword: posKeyword.value || undefined, status: 1
    })
    posProducts.value = data.list
  } finally {
    posLoading.value = false
  }
}

function addToCart(row: Product) {
  const exist = cart.value.find((i) => i.productId === row.id)
  if (exist) {
    exist.quantity += 1
  } else {
    cart.value.push({ productId: row.id, name: row.name, salePrice: row.salePrice, quantity: 1 })
  }
}

async function handleCheckout() {
  checkoutLoading.value = true
  try {
    const order = await checkout({
      memberId: memberId.value,
      payMethod: payMethod.value,
      couponCode: couponCode.value || undefined,
      items: cart.value.map((i) => ({ productId: i.productId, quantity: i.quantity }))
    })
    ElMessage.success(`结算成功：${order.orderNo}，实收 ¥${order.payableAmount.toFixed(2)}，积分 +${order.pointsEarned}`)
    cart.value = []
    memberId.value = undefined
    couponCode.value = ''
    loadPosProducts()
  } finally {
    checkoutLoading.value = false
  }
}

async function loadOrders() {
  orderLoading.value = true
  try {
    const data: PageVO<SalesOrder> = await pageSalesOrders({ ...query })
    orders.value = data.list
    total.value = data.total
  } finally {
    orderLoading.value = false
  }
}

async function openDetail(row: SalesOrder) {
  detail.value = await getSalesOrder(row.id)
  detailVisible.value = true
}

async function openReturn(row: SalesOrder) {
  const data = await getSalesOrder(row.id)
  returnOrderId.value = row.id
  returnReason.value = ''
  returnItems.value = data.items
    .filter((i) => i.returnedQuantity < i.quantity)
    .map((i) => ({ ...i, returnQty: i.quantity - i.returnedQuantity }))
  returnVisible.value = true
}

async function handleReturn() {
  const items = returnItems.value.filter((i) => i.returnQty > 0)
  if (!items.length) return ElMessage.warning('请填写退货数量')
  checkoutLoading.value = true
  try {
    const result = await createSalesReturn({
      orderId: returnOrderId.value,
      reason: returnReason.value || undefined,
      items: items.map((i) => ({ orderItemId: i.id, quantity: i.returnQty }))
    })
    ElMessage.success(`退货成功：${result.returnNo}，退款 ¥${result.refundAmount.toFixed(2)}`)
    returnVisible.value = false
    loadOrders()
  } finally {
    checkoutLoading.value = false
  }
}

onMounted(async () => {
  loadPosProducts()
  loadOrders()
  loadShiftPanel()
  const data = await pageMembers({ page: 1, size: 100, status: 1 })
  members.value = data.list
})
</script>
