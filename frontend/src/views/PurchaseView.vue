<template>
  <div class="page-card">
    <el-tabs v-model="activeTab">
      <!-- 采购订单 -->
      <el-tab-pane label="采购订单" name="orders">
        <div class="toolbar">
          <el-input v-model="orderQuery.keyword" placeholder="搜索订单号" clearable style="width: 200px" @keyup.enter="loadOrders" />
          <el-select v-model="orderQuery.supplierId" placeholder="全部供应商" clearable filterable style="width: 180px" @change="loadOrders">
            <el-option v-for="s in suppliers" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
          <el-select v-model="orderQuery.status" placeholder="全部状态" clearable style="width: 140px" @change="loadOrders">
            <el-option v-for="(label, code) in STATUS_MAP" :key="code" :label="label" :value="Number(code)" />
          </el-select>
          <el-button type="primary" @click="loadOrders">查询</el-button>
          <div style="flex: 1" />
          <el-button type="primary" @click="openCreateOrder">创建采购订单</el-button>
        </div>

        <el-table :data="orders" v-loading="orderLoading" border stripe>
          <el-table-column prop="orderNo" label="订单号" width="200" />
          <el-table-column prop="supplierName" label="供应商" min-width="140" />
          <el-table-column prop="itemCount" label="品项数" width="80" align="center" />
          <el-table-column label="总金额" width="110" align="right">
            <template #default="{ row }">¥{{ row.totalAmount.toFixed(2) }}</template>
          </el-table-column>
          <el-table-column label="已付/未付" width="130" align="right">
            <template #default="{ row }">
              <span style="color: #67c23a">¥{{ (row.paidAmount ?? 0).toFixed(2) }}</span>
              <span style="color: #f56c6c"> / ¥{{ (row.totalAmount - (row.paidAmount ?? 0)).toFixed(2) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="STATUS_TAG[row.status]">{{ row.statusLabel }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="创建时间" width="170" />
          <el-table-column label="操作" width="300" align="center" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openDetail(row)">详情</el-button>
              <el-button v-if="row.status === 0" link type="primary" @click="handleSubmit(row)">提交审核</el-button>
              <el-button v-if="row.status === 1" link type="warning" @click="handleAudit(row, true)">通过</el-button>
              <el-button v-if="row.status === 1" link type="danger" @click="handleAudit(row, false)">驳回</el-button>
              <el-button v-if="row.status === 2 || row.status === 3" link type="success" @click="openReceipt(row)">入库</el-button>
              <el-button v-if="row.status >= 2 && row.status <= 4 && row.totalAmount - (row.paidAmount ?? 0) > 0.001"
                link type="warning" @click="openPay(row)">付款</el-button>
              <el-button v-if="row.status <= 1" link type="danger" @click="handleCancel(row)">取消</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="pagination">
          <el-pagination v-model:current-page="orderQuery.page" v-model:page-size="orderQuery.size"
            :total="orderTotal" layout="total, prev, pager, next" @current-change="loadOrders" />
        </div>
      </el-tab-pane>

      <!-- 采购退货 -->
      <el-tab-pane label="采购退货" name="returns">
        <div class="toolbar">
          <el-input v-model="returnQuery.keyword" placeholder="搜索退货单号" clearable style="width: 200px" @keyup.enter="loadReturns" />
          <el-button type="primary" @click="loadReturns">查询</el-button>
          <div style="flex: 1" />
          <el-button type="primary" @click="openCreateReturn">创建退货单</el-button>
        </div>
        <el-table :data="returns" v-loading="returnLoading" border stripe>
          <el-table-column prop="returnNo" label="退货单号" width="210" />
          <el-table-column prop="supplierName" label="供应商" min-width="140" />
          <el-table-column label="退货金额" width="120" align="right">
            <template #default="{ row }">¥{{ row.totalAmount.toFixed(2) }}</template>
          </el-table-column>
          <el-table-column prop="reason" label="原因" min-width="140" show-overflow-tooltip />
          <el-table-column label="明细" min-width="200">
            <template #default="{ row }">
              {{ row.items.map((i: any) => `${i.productName}×${i.quantity}`).join('，') }}
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="创建时间" width="170" />
        </el-table>
        <div class="pagination">
          <el-pagination v-model:current-page="returnQuery.page" v-model:page-size="returnQuery.size"
            :total="returnTotal" layout="total, prev, pager, next" @current-change="loadReturns" />
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 创建订单对话框 -->
    <el-dialog v-model="createVisible" title="创建采购订单" width="860px" :close-on-click-modal="false">
      <el-form label-width="90px">
        <el-form-item label="供应商" required>
          <el-select v-model="createForm.supplierId" filterable style="width: 300px">
            <el-option v-for="s in suppliers" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="商品明细" required>
          <div style="width: 100%">
            <div v-for="(item, idx) in createForm.items" :key="idx" style="display: flex; gap: 8px; margin-bottom: 8px">
              <el-select v-model="item.productId" filterable remote :remote-method="searchProducts"
                :loading="productSearching" placeholder="搜索商品" style="width: 340px">
                <el-option v-for="p in productOptions" :key="p.id" :label="`${p.sku} ${p.name}`" :value="p.id"
                  :disabled="createForm.items.some((x: any) => x.productId === p.id)" />
              </el-select>
              <el-input-number v-model="item.quantity" :min="1" placeholder="数量" style="width: 130px" />
              <el-input-number v-model="item.purchasePrice" :min="0" :precision="2" placeholder="采购价" style="width: 150px" />
              <el-button type="danger" plain circle @click="createForm.items.splice(idx, 1)">
                <el-icon><Delete /></el-icon>
              </el-button>
            </div>
            <el-button @click="createForm.items.push({ productId: undefined as any, quantity: 1, purchasePrice: 0 })">
              + 添加商品
            </el-button>
            <div style="margin-top: 8px; color: #909399">
              合计：¥{{ createForm.items.reduce((s: number, i: any) => s + i.quantity * i.purchasePrice, 0).toFixed(2) }}
            </div>
          </div>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="createForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleCreateOrder">保存草稿</el-button>
      </template>
    </el-dialog>

    <!-- 详情抽屉 -->
    <el-drawer v-model="detailVisible" title="采购订单详情" size="640px">
      <template v-if="detail">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="订单号">{{ detail.order.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ detail.order.statusLabel }}</el-descriptions-item>
          <el-descriptions-item label="供应商">{{ detail.order.supplierName }}</el-descriptions-item>
          <el-descriptions-item label="总金额">¥{{ detail.order.totalAmount.toFixed(2) }}</el-descriptions-item>
          <el-descriptions-item label="审核备注" :span="2">{{ detail.order.auditRemark || '—' }}</el-descriptions-item>
        </el-descriptions>
        <el-table :data="detail.items" border size="small" style="margin-top: 12px">
          <el-table-column prop="sku" label="SKU" width="110" />
          <el-table-column prop="productName" label="商品" min-width="130" />
          <el-table-column prop="quantity" label="数量" width="70" align="center" />
          <el-table-column prop="purchasePrice" label="采购价" width="90" align="right" />
          <el-table-column prop="receivedQuantity" label="已入库" width="70" align="center" />
        </el-table>
      </template>
    </el-drawer>

    <!-- 入库对话框 -->
    <el-dialog v-model="receiptVisible" title="采购入库" width="560px" :close-on-click-modal="false">
      <el-table :data="receiptItems" border size="small">
        <el-table-column prop="productName" label="商品" min-width="150" />
        <el-table-column label="订单数量" width="90" align="center">
          <template #default="{ row }">{{ row.quantity }}</template>
        </el-table-column>
        <el-table-column label="已入库" width="90" align="center">
          <template #default="{ row }">{{ row.receivedQuantity }}</template>
        </el-table-column>
        <el-table-column label="本次入库" width="150" align="center">
          <template #default="{ row }">
            <el-input-number v-model="row.receiptQty" :min="0" :max="row.quantity - row.receivedQuantity" style="width: 120px" />
          </template>
        </el-table-column>
      </el-table>
      <el-form label-width="110px" style="margin-top: 10px">
        <el-form-item label="生产日期">
          <el-date-picker v-model="receiptForm.productionDate" type="date" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="保质期(天)">
          <el-input-number v-model="receiptForm.shelfLifeDays" :min="0" :step="30" />
          <span style="margin-left: 8px; color: #909399; font-size: 12px">>0 时自动生成批次，用于临期管理</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="receiptVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleReceipt">确认入库（库存将增加）</el-button>
      </template>
    </el-dialog>

    <!-- 付款对话框 -->
    <el-dialog v-model="payVisible" title="登记付款" width="420px" :close-on-click-modal="false">
      <el-form label-width="90px">
        <el-form-item label="付款金额">
          <el-input-number v-model="payForm.amount" :min="0.01" :precision="2" style="width: 100%" />
          <div style="color: #909399; font-size: 12px">未付金额：¥{{ payForm.unpaid.toFixed(2) }}</div>
        </el-form-item>
        <el-form-item label="支付方式">
          <el-select v-model="payForm.method" style="width: 100%">
            <el-option label="现金" value="CASH" />
            <el-option label="银行转账" value="BANK" />
            <el-option label="微信" value="WECHAT" />
            <el-option label="支付宝" value="ALIPAY" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="payForm.remark" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="payVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handlePay">确认付款</el-button>
      </template>
    </el-dialog>

    <!-- 退货对话框 -->
    <el-dialog v-model="returnVisible" title="创建采购退货单" width="700px" :close-on-click-modal="false">
      <el-form label-width="90px">
        <el-form-item label="供应商" required>
          <el-select v-model="returnForm.supplierId" filterable style="width: 280px">
            <el-option v-for="s in suppliers" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="退货明细" required>
          <div style="width: 100%">
            <div v-for="(item, idx) in returnForm.items" :key="idx" style="display: flex; gap: 8px; margin-bottom: 8px">
              <el-select v-model="item.productId" filterable remote :remote-method="searchProducts"
                :loading="productSearching" placeholder="搜索商品" style="width: 300px">
                <el-option v-for="p in productOptions" :key="p.id" :label="`${p.sku} ${p.name}`" :value="p.id" />
              </el-select>
              <el-input-number v-model="item.quantity" :min="1" style="width: 130px" />
              <el-input-number v-model="item.purchasePrice" :min="0" :precision="2" style="width: 150px" />
              <el-button type="danger" plain circle @click="returnForm.items.splice(idx, 1)">
                <el-icon><Delete /></el-icon>
              </el-button>
            </div>
            <el-button @click="returnForm.items.push({ productId: undefined as any, quantity: 1, purchasePrice: 0 })">
              + 添加商品
            </el-button>
          </div>
        </el-form-item>
        <el-form-item label="退货原因">
          <el-input v-model="returnForm.reason" maxlength="255" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="returnVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleCreateReturn">确认退货（库存将减少）</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete } from '@element-plus/icons-vue'
import type { PageVO, Product } from '@/types/product'
import type { Supplier } from '@/types/supplier'
import {
  pagePurchaseOrders, getPurchaseOrder, createPurchaseOrder, submitPurchaseOrder,
  auditPurchaseOrder, cancelPurchaseOrder, receiptPurchaseOrder,
  pagePurchaseReturns, createPurchaseReturn
} from '@/api/inventory'
import { payOrder } from '@/api/extensions'
import { supplierOptions } from '@/api/supplier'
import { pageProducts } from '@/api/product'

const STATUS_MAP: Record<number, string> = { 0: '草稿', 1: '待审核', 2: '已审核', 3: '部分入库', 4: '已完成', 5: '已取消' }
const STATUS_TAG: Record<number, string> = { 0: 'info', 1: 'warning', 2: 'primary', 3: 'warning', 4: 'success', 5: 'danger' }

const activeTab = ref('orders')
const suppliers = ref<Supplier[]>([])
const productOptions = ref<Product[]>([])
const productSearching = ref(false)
const submitting = ref(false)

// 订单列表
const orders = ref<any[]>([])
const orderTotal = ref(0)
const orderLoading = ref(false)
const orderQuery = reactive({ page: 1, size: 10, keyword: '', supplierId: undefined as number | undefined, status: undefined as number | undefined })

// 退货列表
const returns = ref<any[]>([])
const returnTotal = ref(0)
const returnLoading = ref(false)
const returnQuery = reactive({ page: 1, size: 10, keyword: '' })

// 创建订单
const createVisible = ref(false)
const createForm = reactive<{ supplierId?: number; remark: string; items: any[] }>({ remark: '', items: [] })

// 详情
const detailVisible = ref(false)
const detail = ref<{ order: any; items: any[] } | null>(null)

// 入库
const receiptVisible = ref(false)
const receiptOrderId = ref<number>(0)
const receiptItems = ref<any[]>([])
const receiptForm = reactive<{ productionDate: string | null; shelfLifeDays: number }>({
  productionDate: null, shelfLifeDays: 0
})

// 付款
const payVisible = ref(false)
const payOrderId = ref(0)
const payForm = reactive({ amount: 0, unpaid: 0, method: 'BANK', remark: '' })

function openPay(row: any) {
  payOrderId.value = row.id
  payForm.unpaid = row.totalAmount - (row.paidAmount ?? 0)
  payForm.amount = payForm.unpaid
  payForm.method = 'BANK'
  payForm.remark = ''
  payVisible.value = true
}

async function handlePay() {
  submitting.value = true
  try {
    await payOrder(payOrderId.value, {
      amount: payForm.amount,
      method: payForm.method,
      remark: payForm.remark || undefined
    })
    ElMessage.success('付款已登记')
    payVisible.value = false
    loadOrders()
  } finally {
    submitting.value = false
  }
}

// 退货
const returnVisible = ref(false)
const returnForm = reactive<{ supplierId?: number; reason: string; items: any[] }>({ reason: '', items: [] })

async function loadOrders() {
  orderLoading.value = true
  try {
    const data: PageVO<any> = await pagePurchaseOrders({ ...orderQuery })
    orders.value = data.list
    orderTotal.value = data.total
  } finally {
    orderLoading.value = false
  }
}

async function loadReturns() {
  returnLoading.value = true
  try {
    const data: PageVO<any> = await pagePurchaseReturns({ ...returnQuery })
    returns.value = data.list
    returnTotal.value = data.total
  } finally {
    returnLoading.value = false
  }
}

async function searchProducts(keyword: string) {
  productSearching.value = true
  try {
    const data = await pageProducts({ page: 1, size: 20, keyword })
    productOptions.value = data.list
  } finally {
    productSearching.value = false
  }
}

function openCreateOrder() {
  createForm.supplierId = undefined
  createForm.remark = ''
  createForm.items = [{ productId: undefined as any, quantity: 1, purchasePrice: 0 }]
  searchProducts('')
  createVisible.value = true
}

async function handleCreateOrder() {
  if (!createForm.supplierId) return ElMessage.warning('请选择供应商')
  const items = createForm.items.filter((i) => i.productId && i.quantity > 0)
  if (!items.length) return ElMessage.warning('请添加商品明细')
  submitting.value = true
  try {
    await createPurchaseOrder({
      supplierId: createForm.supplierId,
      remark: createForm.remark || undefined,
      items: items.map((i) => ({ productId: i.productId, quantity: i.quantity, purchasePrice: i.purchasePrice }))
    })
    ElMessage.success('采购订单草稿已保存')
    createVisible.value = false
    loadOrders()
  } finally {
    submitting.value = false
  }
}

async function openDetail(row: any) {
  detail.value = await getPurchaseOrder(row.id)
  detailVisible.value = true
}

async function handleSubmit(row: any) {
  await submitPurchaseOrder(row.id)
  ElMessage.success('已提交审核')
  loadOrders()
}

async function handleAudit(row: any, approved: boolean) {
  let remark: string | undefined
  if (!approved) {
    const r = await ElMessageBox.prompt('请输入驳回原因', '驳回审核').catch(() => null)
    if (!r) return
    remark = r.value
  }
  await auditPurchaseOrder(row.id, approved, remark)
  ElMessage.success(approved ? '审核通过' : '已驳回')
  loadOrders()
}

async function handleCancel(row: any) {
  const ok = await ElMessageBox.confirm(`确定取消订单「${row.orderNo}」吗？`, '提示', { type: 'warning' }).catch(() => false)
  if (!ok) return
  await cancelPurchaseOrder(row.id)
  ElMessage.success('已取消')
  loadOrders()
}

async function openReceipt(row: any) {
  const data = await getPurchaseOrder(row.id)
  receiptOrderId.value = row.id
  receiptForm.productionDate = null
  receiptForm.shelfLifeDays = 0
  receiptItems.value = data.items
    .filter((i: any) => i.receivedQuantity < i.quantity)
    .map((i: any) => ({ ...i, receiptQty: i.quantity - i.receivedQuantity }))
  receiptVisible.value = true
}

async function handleReceipt() {
  const items = receiptItems.value.filter((i) => i.receiptQty > 0)
  if (!items.length) return ElMessage.warning('请填写入库数量')
  submitting.value = true
  try {
    const result = await receiptPurchaseOrder(receiptOrderId.value, {
      items: items.map((i) => ({ orderItemId: i.id, quantity: i.receiptQty })),
      productionDate: receiptForm.productionDate || undefined,
      shelfLifeDays: receiptForm.shelfLifeDays > 0 ? receiptForm.shelfLifeDays : undefined
    })
    ElMessage.success(`入库成功：${result.receiptNo}，订单状态：${result.orderStatus}`)
    receiptVisible.value = false
    loadOrders()
  } finally {
    submitting.value = false
  }
}

function openCreateReturn() {
  returnForm.supplierId = undefined
  returnForm.reason = ''
  returnForm.items = [{ productId: undefined as any, quantity: 1, purchasePrice: 0 }]
  searchProducts('')
  returnVisible.value = true
}

async function handleCreateReturn() {
  if (!returnForm.supplierId) return ElMessage.warning('请选择供应商')
  const items = returnForm.items.filter((i) => i.productId && i.quantity > 0)
  if (!items.length) return ElMessage.warning('请添加退货明细')
  submitting.value = true
  try {
    await createPurchaseReturn({
      supplierId: returnForm.supplierId,
      reason: returnForm.reason || undefined,
      items: items.map((i) => ({ productId: i.productId, quantity: i.quantity, purchasePrice: i.purchasePrice }))
    })
    ElMessage.success('退货单已创建，库存已减少')
    returnVisible.value = false
    loadReturns()
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  suppliers.value = (await supplierOptions()).filter((s) => s.status === 1)
  loadOrders()
  loadReturns()
})
</script>
