<template>
  <div class="page-card">
    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <!-- 当前库存 -->
      <el-tab-pane label="当前库存" name="stock">
        <div class="toolbar">
          <el-input v-model="query.keyword" placeholder="搜索商品 / SKU / 条码" clearable style="width: 220px" @keyup.enter="loadStock" @clear="loadStock" />
          <el-select v-model="query.alertType" placeholder="全部库存" clearable style="width: 170px" @change="loadStock">
            <el-option label="缺货" value="OUT_OF_STOCK" />
            <el-option label="低于最低库存" value="LOW" />
            <el-option label="高于最高库存" value="HIGH" />
          </el-select>
          <el-button type="primary" @click="loadStock">查询</el-button>
          <div style="flex: 1" />
          <el-button type="warning" @click="openAdjust">手工调整</el-button>
        </div>
        <el-table :data="stockList" v-loading="loading" border stripe>
          <el-table-column prop="sku" label="SKU" width="120" />
          <el-table-column prop="productName" label="商品名称" min-width="150" />
          <el-table-column prop="quantity" label="当前库存" width="100" align="center">
            <template #default="{ row }">
              <span :style="{ color: alertColor(row.alertType), fontWeight: 600 }">{{ row.quantity }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="unit" label="单位" width="70" align="center" />
          <el-table-column prop="minStock" label="最低库存" width="90" align="center" />
          <el-table-column prop="maxStock" label="最高库存" width="90" align="center">
            <template #default="{ row }">{{ row.maxStock ?? '—' }}</template>
          </el-table-column>
          <el-table-column label="库存金额" width="110" align="right">
            <template #default="{ row }">¥{{ (row.inventoryValue ?? 0).toFixed(2) }}</template>
          </el-table-column>
          <el-table-column label="预警" width="130" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.alertType" :type="alertTag(row.alertType)">
                {{ ALERT_LABELS[row.alertType] }}
              </el-tag>
              <span v-else style="color: #909399">正常</span>
            </template>
          </el-table-column>
          <el-table-column prop="updatedAt" label="更新时间" width="170" />
        </el-table>
        <div class="pagination">
          <el-pagination v-model:current-page="query.page" v-model:page-size="query.size"
            :total="stockTotal" layout="total, sizes, prev, pager, next" :page-sizes="[10, 20, 50]"
            @size-change="loadStock" @current-change="loadStock" />
        </div>
      </el-tab-pane>

      <!-- 库存预警 -->
      <el-tab-pane label="库存预警" name="alerts">
        <el-table :data="alerts" v-loading="alertLoading" border stripe>
          <el-table-column prop="sku" label="SKU" width="120" />
          <el-table-column prop="productName" label="商品名称" min-width="150" />
          <el-table-column prop="quantity" label="当前库存" width="100" align="center" />
          <el-table-column prop="minStock" label="最低库存" width="100" align="center" />
          <el-table-column label="预警类型" width="130" align="center">
            <template #default="{ row }">
              <el-tag :type="alertTag(row.alertType)">{{ ALERT_LABELS[row.alertType] }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="建议补货" width="100" align="center">
            <template #default="{ row }">{{ row.suggestQty ?? '—' }}</template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- 批次与临期 -->
      <el-tab-pane label="批次与临期" name="batches">
        <div class="toolbar">
          <el-select v-model="batchStatus" placeholder="全部批次" clearable style="width: 160px" @change="loadBatches">
            <el-option label="已过期" value="EXPIRED" />
            <el-option label="紧急临期(≤7天)" value="URGENT" />
            <el-option label="临期(≤30天)" value="NEAR" />
            <el-option label="正常" value="OK" />
          </el-select>
          <el-button type="primary" @click="loadBatches">查询</el-button>
          <div style="flex: 1" />
          <el-button type="primary" plain @click="openBatchCreate">手工建批次</el-button>
        </div>
        <el-table :data="batches" v-loading="batchLoading" border stripe size="small">
          <el-table-column prop="batchNo" label="批次号" width="140" />
          <el-table-column prop="productName" label="商品" min-width="120" />
          <el-table-column prop="quantity" label="库存" width="70" align="center" />
          <el-table-column prop="expireDate" label="到期日" width="110" />
          <el-table-column label="剩余天数" width="90" align="center">
            <template #default="{ row }">{{ row.daysToExpire < 0 ? '已过期' : row.daysToExpire + '天' }}</template>
          </el-table-column>
          <el-table-column label="状态" width="80" align="center">
            <template #default="{ row }">
              <el-tag :type="row.expiryStatus === 'EXPIRED' ? 'danger' : row.expiryStatus === 'URGENT' ? 'warning' : row.expiryStatus === 'NEAR' ? 'warning' : 'success'" size="small">
                {{ { EXPIRED: '过期', URGENT: '紧急', NEAR: '临期', OK: '正常' }[row.expiryStatus as string] }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="supplierName" label="供应商" width="110" />
          <el-table-column label="操作" width="100" align="center" fixed="right">
            <template #default="{ row }">
              <el-button link type="danger" @click="openDispose(row)">处置</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- 库存流水 -->
      <el-tab-pane label="库存流水" name="transactions">
        <div class="toolbar">
          <el-select v-model="txQuery.type" placeholder="全部类型" clearable style="width: 170px" @change="loadTransactions">
            <el-option v-for="(label, key) in TX_TYPE_LABELS" :key="key" :label="label" :value="key" />
          </el-select>
          <el-button type="primary" @click="loadTransactions">查询</el-button>
        </div>
        <el-table :data="txList" v-loading="txLoading" border stripe>
          <el-table-column prop="createdAt" label="时间" width="170" />
          <el-table-column prop="productName" label="商品" min-width="140" />
          <el-table-column label="类型" width="110" align="center">
            <template #default="{ row }">{{ TX_TYPE_LABELS[row.type] ?? row.type }}</template>
          </el-table-column>
          <el-table-column label="变动" width="90" align="center">
            <template #default="{ row }">
              <span :style="{ color: row.changeQty > 0 ? '#67C23A' : '#F56C6C' }">
                {{ row.changeQty > 0 ? '+' : '' }}{{ row.changeQty }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="beforeQty" label="变动前" width="80" align="center" />
          <el-table-column prop="afterQty" label="变动后" width="80" align="center" />
          <el-table-column prop="refNo" label="关联单据" width="200" />
          <el-table-column prop="remark" label="备注" min-width="120" show-overflow-tooltip />
        </el-table>
        <div class="pagination">
          <el-pagination v-model:current-page="txQuery.page" v-model:page-size="txQuery.size"
            :total="txTotal" layout="total, prev, pager, next" @current-change="loadTransactions" />
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 建批次 -->
    <el-dialog v-model="batchCreateVisible" title="手工创建批次（同步入库）" width="480px" :close-on-click-modal="false">
      <el-form label-width="90px">
        <el-form-item label="商品" required>
          <el-select v-model="batchForm.productId" filterable remote :remote-method="searchProducts" placeholder="搜索商品" style="width: 100%">
            <el-option v-for="p in productOptions" :key="p.id" :label="`${p.sku} ${p.name}`" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="数量" required>
          <el-input-number v-model="batchForm.quantity" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="生产日期">
          <el-date-picker v-model="batchForm.productionDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="保质期(天)">
          <el-input-number v-model="batchForm.shelfLifeDays" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="batchForm.remark" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchCreateVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleBatchCreate">创建</el-button>
      </template>
    </el-dialog>

    <!-- 手工调整 -->
    <el-dialog v-model="adjustVisible" title="手工调整库存" width="440px" :close-on-click-modal="false">
      <el-form label-width="90px">
        <el-form-item label="商品" required>
          <el-select v-model="adjustForm.productId" filterable remote :remote-method="searchProducts" placeholder="搜索商品" style="width: 100%">
            <el-option v-for="p in productOptions" :key="p.id" :label="`${p.sku} ${p.name}`" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="调整数量" required>
          <el-input-number v-model="adjustForm.changeQty" :step="1" style="width: 100%" />
          <div style="color: #909399; font-size: 12px">正数增加库存，负数减少库存</div>
        </el-form-item>
        <el-form-item label="调整原因" required>
          <el-input v-model="adjustForm.reason" maxlength="255" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="adjustVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleAdjust">确认调整</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  pageInventory, inventoryAlerts, pageInventoryTransactions, adjustInventory,
  TX_TYPE_LABELS, ALERT_LABELS, type InventoryVO, type InventoryAlertVO, type InventoryTransactionVO
} from '@/api/inventory'
import { pageProducts } from '@/api/product'
import { listBatches, createBatch, disposeBatch, type BatchVO } from '@/api/extensions'
import type { Product } from '@/types/product'

const activeTab = ref('stock')
const loading = ref(false)
const alertLoading = ref(false)
const txLoading = ref(false)
const submitting = ref(false)

const stockList = ref<InventoryVO[]>([])
const stockTotal = ref(0)
const query = reactive({ page: 1, size: 10, keyword: '', alertType: '' })

const alerts = ref<InventoryAlertVO[]>([])

const txList = ref<InventoryTransactionVO[]>([])
const txTotal = ref(0)
const txQuery = reactive({ page: 1, size: 10, type: '' })

const adjustVisible = ref(false)
const adjustForm = reactive({ productId: undefined as number | undefined, changeQty: 0, reason: '' })
const productOptions = ref<Product[]>([])

function alertTag(t?: string | null) {
  return t === 'OUT_OF_STOCK' ? 'danger' : t === 'LOW' ? 'warning' : 'info'
}
function alertColor(t?: string | null) {
  return t === 'OUT_OF_STOCK' || t === 'LOW' ? '#F56C6C' : '#303133'
}

async function loadStock() {
  loading.value = true
  try {
    const data = await pageInventory({
      page: query.page, size: query.size, keyword: query.keyword || undefined,
      alertType: query.alertType || undefined
    })
    stockList.value = data.list
    stockTotal.value = data.total
  } finally {
    loading.value = false
  }
}

async function loadAlerts() {
  alertLoading.value = true
  try {
    alerts.value = await inventoryAlerts()
  } finally {
    alertLoading.value = false
  }
}

async function loadTransactions() {
  txLoading.value = true
  try {
    const data = await pageInventoryTransactions({
      page: txQuery.page, size: txQuery.size, type: txQuery.type || undefined
    })
    txList.value = data.list
    txTotal.value = data.total
  } finally {
    txLoading.value = false
  }
}

function handleTabChange(tab: string | number) {
  if (tab === 'alerts') loadAlerts()
  if (tab === 'transactions') loadTransactions()
  if (tab === 'stock') loadStock()
  if (tab === 'batches') loadBatches()
}

async function searchProducts(keyword: string) {
  const data = await pageProducts({ page: 1, size: 20, keyword })
  productOptions.value = data.list
}

function openAdjust() {
  adjustForm.productId = undefined
  adjustForm.changeQty = 0
  adjustForm.reason = ''
  searchProducts('')
  adjustVisible.value = true
}

async function handleAdjust() {
  if (!adjustForm.productId) return ElMessage.warning('请选择商品')
  if (!adjustForm.changeQty) return ElMessage.warning('调整数量不能为0')
  if (!adjustForm.reason.trim()) return ElMessage.warning('请填写调整原因')
  submitting.value = true
  try {
    await adjustInventory({
      productId: adjustForm.productId,
      changeQty: adjustForm.changeQty,
      reason: adjustForm.reason
    })
    ElMessage.success('调整成功')
    adjustVisible.value = false
    loadStock()
  } finally {
    submitting.value = false
  }
}

// 批次
const batches = ref<BatchVO[]>([])
const batchLoading = ref(false)
const batchStatus = ref('')
const batchCreateVisible = ref(false)
const batchForm = reactive<{ productId: number | undefined; quantity: number; productionDate: string | null; shelfLifeDays: number; remark: string }>({
  productId: undefined, quantity: 1, productionDate: null, shelfLifeDays: 180, remark: ''
})

async function loadBatches() {
  batchLoading.value = true
  try {
    batches.value = await listBatches({ status: batchStatus.value || undefined })
  } finally {
    batchLoading.value = false
  }
}

function openBatchCreate() {
  batchForm.productId = undefined
  batchForm.quantity = 1
  batchForm.productionDate = null
  batchForm.shelfLifeDays = 180
  batchForm.remark = ''
  searchProducts('')
  batchCreateVisible.value = true
}

async function handleBatchCreate() {
  if (!batchForm.productId) return ElMessage.warning('请选择商品')
  submitting.value = true
  try {
    await createBatch({
      productId: batchForm.productId,
      quantity: batchForm.quantity,
      productionDate: batchForm.productionDate || undefined,
      shelfLifeDays: batchForm.shelfLifeDays,
      remark: batchForm.remark || undefined
    })
    ElMessage.success('批次已创建，库存已增加')
    batchCreateVisible.value = false
    loadBatches()
    loadStock()
  } finally {
    submitting.value = false
  }
}

async function openDispose(row: BatchVO) {
  const r = await ElMessageBox.prompt(
    `处置批次「${row.batchNo}」（${row.productName}，剩余 ${row.quantity}），将扣减总库存并记损耗`,
    '临期/过期处置',
    { inputPattern: /^\d+$/, inputErrorMessage: '请输入数量', inputValue: '1' }
  ).catch(() => null)
  if (!r) return
  const r2 = await ElMessageBox.prompt('处置原因（如：过期销毁/破损）', '原因').catch(() => null)
  if (!r2) return
  await disposeBatch(row.id, { quantity: Number(r.value), reason: r2.value })
  ElMessage.success('处置完成')
  loadBatches()
}

onMounted(loadStock)
</script>
