<template>
  <div class="page-card">
    <el-tabs v-model="activeTab">
      <!-- 盘点任务 -->
      <el-tab-pane label="盘点任务" name="stocktake">
        <div class="toolbar">
          <span style="color: #909399; font-size: 13px">
            流程：创建任务（快照系统库存）→ 录入实盘数量 → 完成盘点（差异自动调整库存）
          </span>
          <div style="flex: 1" />
          <el-button type="primary" @click="openCreate">创建盘点任务</el-button>
        </div>
        <el-table :data="list" v-loading="loading" border stripe>
          <el-table-column prop="taskNo" label="任务号" width="210" />
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 0 ? 'warning' : row.status === 1 ? 'success' : 'info'">
                {{ row.statusLabel }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="remark" label="备注" min-width="140" show-overflow-tooltip />
          <el-table-column prop="createdAt" label="创建时间" width="170" />
          <el-table-column prop="completedAt" label="完成时间" width="170" />
          <el-table-column label="操作" width="240" align="center" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openDetail(row)">详情/盘点</el-button>
              <el-button v-if="row.status === 0" link type="success" @click="handleComplete(row)">完成盘点</el-button>
              <el-button v-if="row.status === 0" link type="danger" @click="handleCancel(row)">取消</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="pagination">
          <el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :total="total"
            layout="total, prev, pager, next" @current-change="loadData" />
        </div>
      </el-tab-pane>

      <!-- 损耗记录 -->
      <el-tab-pane label="损耗登记" name="loss">
        <div class="toolbar">
          <span style="color: #909399; font-size: 13px">损耗登记将同步扣减库存，损耗金额按采购价计算</span>
          <div style="flex: 1" />
          <el-button type="danger" plain @click="openLoss">登记损耗</el-button>
        </div>
        <el-table :data="lossList" v-loading="lossLoading" border stripe>
          <el-table-column prop="createdAt" label="时间" width="170" />
          <el-table-column prop="productName" label="商品" min-width="140" />
          <el-table-column prop="sku" label="SKU" width="120" />
          <el-table-column prop="quantity" label="损耗数量" width="100" align="center" />
          <el-table-column label="损耗金额" width="110" align="right">
            <template #default="{ row }">¥{{ row.lossAmount.toFixed(2) }}</template>
          </el-table-column>
          <el-table-column prop="reason" label="原因" min-width="160" show-overflow-tooltip />
        </el-table>
        <div class="pagination">
          <el-pagination v-model:current-page="lossQuery.page" v-model:page-size="lossQuery.size"
            :total="lossTotal" layout="total, prev, pager, next" @current-change="loadLoss" />
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 创建盘点任务 -->
    <el-dialog v-model="createVisible" title="创建盘点任务" width="520px" :close-on-click-modal="false">
      <el-form label-width="90px">
        <el-form-item label="盘点商品" required>
          <el-select v-model="form.productIds" multiple filterable remote :remote-method="searchProducts"
            :loading="productSearching" placeholder="搜索并选择商品" style="width: 100%">
            <el-option v-for="p in productOptions" :key="p.id" :label="`${p.sku} ${p.name}`" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" maxlength="255" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleCreate">创建</el-button>
      </template>
    </el-dialog>

    <!-- 盘点详情/录入 -->
    <el-drawer v-model="detailVisible" :title="`盘点详情 - ${detail?.taskNo ?? ''}`" size="680px">
      <template v-if="detail">
        <el-table :data="detail.items" border size="small">
          <el-table-column prop="sku" label="SKU" width="110" />
          <el-table-column prop="productName" label="商品" min-width="130" />
          <el-table-column prop="systemQty" label="系统数量" width="90" align="center" />
          <el-table-column label="实盘数量" width="140" align="center">
            <template #default="{ row }">
              <el-input-number v-if="detail.status === 0" v-model="row.actualQty" :min="0" size="small"
                style="width: 120px" @change="handleRecord(row)" />
              <span v-else>{{ row.actualQty ?? '未盘' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="差异" width="80" align="center">
            <template #default="{ row }">
              <span v-if="row.diff !== null" :style="{ color: row.diff > 0 ? '#67C23A' : row.diff < 0 ? '#F56C6C' : '#909399' }">
                {{ row.diff > 0 ? `+${row.diff}` : row.diff }}
              </span>
              <span v-else>—</span>
            </template>
          </el-table-column>
        </el-table>
        <div style="margin-top: 10px; color: #909399; font-size: 13px" v-if="detail.status === 0">
          录入实盘数量后自动保存差异；点击"完成盘点"将自动生成库存调整（盘盈+ / 盘亏-）。
        </div>
      </template>
    </el-drawer>

    <!-- 损耗登记 -->
    <el-dialog v-model="lossVisible" title="登记损耗" width="460px" :close-on-click-modal="false">
      <el-form label-width="90px">
        <el-form-item label="商品" required>
          <el-select v-model="lossForm.productId" filterable remote :remote-method="searchProducts"
            placeholder="搜索商品" style="width: 100%">
            <el-option v-for="p in productOptions" :key="p.id" :label="`${p.sku} ${p.name}`" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="损耗数量" required>
          <el-input-number v-model="lossForm.quantity" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="损耗原因" required>
          <el-input v-model="lossForm.reason" maxlength="255" placeholder="如：过期/破损/丢失" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="lossVisible = false">取消</el-button>
        <el-button type="danger" :loading="submitting" @click="handleLoss">确认登记（扣减库存）</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { PageVO, Product } from '@/types/product'
import {
  pageStocktakes, createStocktake, recordStocktakeItem, completeStocktake,
  cancelStocktake, pageLossRecords, createLossRecord,
  type Stocktake, type LossRecord
} from '@/api/stocktake'
import { pageProducts } from '@/api/product'

const activeTab = ref('stocktake')
const list = ref<Stocktake[]>([])
const total = ref(0)
const loading = ref(false)
const submitting = ref(false)
const query = reactive({ page: 1, size: 10 })

const lossList = ref<LossRecord[]>([])
const lossTotal = ref(0)
const lossLoading = ref(false)
const lossQuery = reactive({ page: 1, size: 10 })

const createVisible = ref(false)
const form = reactive<{ productIds: number[]; remark: string }>({ productIds: [], remark: '' })

const detailVisible = ref(false)
const detail = ref<Stocktake | null>(null)

const lossVisible = ref(false)
const lossForm = reactive<{ productId: number | undefined; quantity: number; reason: string }>({
  productId: undefined, quantity: 1, reason: ''
})

const productOptions = ref<Product[]>([])
const productSearching = ref(false)

async function loadData() {
  loading.value = true
  try {
    const data: PageVO<Stocktake> = await pageStocktakes({ ...query })
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

async function loadLoss() {
  lossLoading.value = true
  try {
    const data: PageVO<LossRecord> = await pageLossRecords({ ...lossQuery })
    lossList.value = data.list
    lossTotal.value = data.total
  } finally {
    lossLoading.value = false
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

function openCreate() {
  form.productIds = []
  form.remark = ''
  searchProducts('')
  createVisible.value = true
}

async function handleCreate() {
  if (!form.productIds.length) return ElMessage.warning('请选择盘点商品')
  submitting.value = true
  try {
    await createStocktake({ productIds: form.productIds, remark: form.remark || undefined })
    ElMessage.success('盘点任务已创建')
    createVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

async function openDetail(row: Stocktake) {
  detail.value = row
  detailVisible.value = true
}

async function handleRecord(row: any) {
  if (!detail.value || row.actualQty === null || row.actualQty === undefined) return
  await recordStocktakeItem(detail.value.id, row.id, row.actualQty)
  row.diff = row.actualQty - row.systemQty
  ElMessage.success(`已记录：${row.productName}`)
}

async function handleComplete(row: Stocktake) {
  const ok = await ElMessageBox.confirm('完成盘点将自动生成库存调整，确认？', '提示', { type: 'warning' }).catch(() => false)
  if (!ok) return
  await completeStocktake(row.id)
  ElMessage.success('盘点完成，库存已调整')
  detailVisible.value = false
  loadData()
}

async function handleCancel(row: Stocktake) {
  const ok = await ElMessageBox.confirm(`确定取消任务「${row.taskNo}」吗？`, '提示', { type: 'warning' }).catch(() => false)
  if (!ok) return
  await cancelStocktake(row.id)
  ElMessage.success('已取消')
  loadData()
}

function openLoss() {
  lossForm.productId = undefined
  lossForm.quantity = 1
  lossForm.reason = ''
  searchProducts('')
  lossVisible.value = true
}

async function handleLoss() {
  if (!lossForm.productId) return ElMessage.warning('请选择商品')
  if (!lossForm.reason.trim()) return ElMessage.warning('请填写损耗原因')
  submitting.value = true
  try {
    await createLossRecord({
      productId: lossForm.productId,
      quantity: lossForm.quantity,
      reason: lossForm.reason
    })
    ElMessage.success('损耗已登记，库存已扣减')
    lossVisible.value = false
    loadLoss()
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadData()
  loadLoss()
})
</script>
