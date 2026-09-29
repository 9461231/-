<template>
  <div class="page-card">
    <!-- 搜索筛选工具栏 -->
    <div class="toolbar">
      <el-input
        v-model="query.keyword"
        placeholder="搜索：名称 / SKU / 条码 / 品牌"
        clearable
        style="width: 260px"
        @keyup.enter="handleSearch"
        @clear="handleSearch"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>

      <el-select
        v-model="query.categoryId"
        placeholder="全部分类"
        clearable
        style="width: 160px"
        @change="handleSearch"
      >
        <el-option
          v-for="c in categories"
          :key="c.id"
          :label="c.name"
          :value="c.id"
        />
      </el-select>

      <el-select
        v-model="query.status"
        placeholder="全部状态"
        clearable
        style="width: 130px"
        @change="handleSearch"
      >
        <el-option label="在售" :value="1" />
        <el-option label="停售" :value="0" />
      </el-select>

      <el-button type="primary" @click="handleSearch">查询</el-button>
      <el-button @click="handleReset">重置</el-button>

      <div style="flex: 1" />
      <el-button @click="handleTemplate">下载模板</el-button>
      <el-button @click="handleExport">导出</el-button>
      <el-button type="success" plain @click="importVisible = true">导入</el-button>
      <el-button type="primary" @click="openCreate">
        <el-icon style="margin-right: 4px"><Plus /></el-icon>新增商品
      </el-button>
    </div>

    <!-- 商品列表 -->
    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="sku" label="SKU" width="120" />
      <el-table-column prop="name" label="商品名称" min-width="160" show-overflow-tooltip />
      <el-table-column prop="categoryName" label="分类" width="100" />
      <el-table-column prop="brand" label="品牌" width="100" show-overflow-tooltip />
      <el-table-column prop="spec" label="规格" width="130" show-overflow-tooltip />
      <el-table-column prop="unit" label="单位" width="70" align="center" />
      <el-table-column label="采购价" width="100" align="right">
        <template #default="{ row }">¥{{ row.purchasePrice?.toFixed(2) }}</template>
      </el-table-column>
      <el-table-column label="销售价" width="100" align="right">
        <template #default="{ row }">¥{{ row.salePrice?.toFixed(2) }}</template>
      </el-table-column>
      <el-table-column label="库存阈值" width="110" align="center">
        <template #default="{ row }">
          {{ row.minStock }} / {{ row.maxStock ?? '—' }}
        </template>
      </el-table-column>
      <el-table-column label="状态" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="STATUS_TAG[row.status]">
            {{ STATUS_LABEL[row.status] }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" align="center" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">详情</el-button>
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="pagination">
      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadData"
        @current-change="loadData"
      />
    </div>

    <!-- 新增 / 编辑 -->
    <ProductForm
      v-model="formVisible"
      :categories="categories"
      :product="editingProduct"
      @saved="loadData"
    />

    <!-- 详情抽屉 -->
    <el-drawer v-model="detailVisible" title="商品详情" size="420px">
      <el-descriptions :column="1" border v-if="detailProduct">
        <el-descriptions-item label="ID">{{ detailProduct.id }}</el-descriptions-item>
        <el-descriptions-item label="商品名称">{{ detailProduct.name }}</el-descriptions-item>
        <el-descriptions-item label="SKU">{{ detailProduct.sku }}</el-descriptions-item>
        <el-descriptions-item label="条码">{{ detailProduct.barcode || '—' }}</el-descriptions-item>
        <el-descriptions-item label="分类">{{ detailProduct.categoryName }}</el-descriptions-item>
        <el-descriptions-item label="品牌">{{ detailProduct.brand || '—' }}</el-descriptions-item>
        <el-descriptions-item label="规格">{{ detailProduct.spec || '—' }}</el-descriptions-item>
        <el-descriptions-item label="单位">{{ detailProduct.unit }}</el-descriptions-item>
        <el-descriptions-item label="采购价">¥{{ detailProduct.purchasePrice?.toFixed(2) }}</el-descriptions-item>
        <el-descriptions-item label="销售价">¥{{ detailProduct.salePrice?.toFixed(2) }}</el-descriptions-item>
        <el-descriptions-item label="最低库存">{{ detailProduct.minStock }}</el-descriptions-item>
        <el-descriptions-item label="最高库存">{{ detailProduct.maxStock ?? '—' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="detailProduct.status === 1 ? 'success' : 'info'">
            {{ detailProduct.status === 1 ? '在售' : '停售' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="备注">{{ detailProduct.remark || '—' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ detailProduct.createdAt }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ detailProduct.updatedAt }}</el-descriptions-item>
      </el-descriptions>

      <h4 style="margin: 14px 0 8px">价格变更记录</h4>
      <el-table :data="priceHistory" border size="small">
        <el-table-column prop="createdAt" label="时间" width="160" />
        <el-table-column label="字段" width="90">
          <template #default="{ row }">
            {{ { salePrice: '销售价', purchasePrice: '采购价', memberPrice: '会员价' }[row.fieldName as string] ?? row.fieldName }}
          </template>
        </el-table-column>
        <el-table-column prop="oldValue" label="原价" width="90" align="right" />
        <el-table-column prop="newValue" label="新价" width="90" align="right" />
        <el-table-column prop="changedBy" label="操作人" width="100" />
        <el-table-column prop="source" label="来源" width="80" align="center" />
      </el-table>
    </el-drawer>

    <!-- 导入对话框 -->
    <el-dialog v-model="importVisible" title="Excel 批量导入商品" width="440px">
      <el-alert type="info" :closable="false" style="margin-bottom: 12px"
        title="请先下载模板，按模板填写后上传（SKU 重复的行将被跳过）" />
      <input type="file" accept=".xlsx" @change="handleImportFile" />
      <div v-if="importResult" style="margin-top: 10px; color: #67c23a">
        导入成功 {{ importResult.success }} 条，跳过 {{ importResult.skipped }} 条
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import type { Product, ProductCategory } from '@/types/product'
import { deleteProduct, listCategories, pageProducts } from '@/api/product'
import { priceChanges } from '@/api/extensions'
import ProductForm from '@/components/ProductForm.vue'

const list = ref<Product[]>([])
const categories = ref<ProductCategory[]>([])
const total = ref(0)
const loading = ref(false)

const query = reactive({
  page: 1,
  size: 10,
  keyword: '',
  categoryId: undefined as number | undefined,
  status: undefined as number | undefined
})

const formVisible = ref(false)
const editingProduct = ref<Product | null>(null)

const detailVisible = ref(false)
const detailProduct = ref<Product | null>(null)
const priceHistory = ref<any[]>([])
const importVisible = ref(false)
const importResult = ref<{ success: number; skipped: number } | null>(null)

const STATUS_LABEL: Record<number, string> = { 0: '停售', 1: '在售', 2: '草稿', 3: '归档' }
const STATUS_TAG: Record<number, string> = { 0: 'info', 1: 'success', 2: 'warning', 3: 'danger' }

async function download(url: string, fallbackName: string) {
  const resp = await fetch(url, {
    headers: { Authorization: `Bearer ${localStorage.getItem('sm_token')}` }
  })
  if (!resp.ok) {
    ElMessage.error('下载失败，请确认已登录')
    return
  }
  const blob = await resp.blob()
  const a = document.createElement('a')
  a.href = URL.createObjectURL(blob)
  a.download = fallbackName
  a.click()
  URL.revokeObjectURL(a.href)
}

const handleExport = () => download('/api/products/export', `products_${Date.now()}.xlsx`)
const handleTemplate = () => download('/api/products/template', 'product_template.xlsx')

async function handleImportFile(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  const form = new FormData()
  form.append('file', file)
  const resp = await fetch('/api/products/import', {
    method: 'POST',
    headers: { Authorization: `Bearer ${localStorage.getItem('sm_token')}` },
    body: form
  })
  const body = await resp.json()
  if (body.code === 0) {
    importResult.value = body.data
    ElMessage.success(`导入完成：成功 ${body.data.success}，跳过 ${body.data.skipped}`)
    loadData()
  } else {
    ElMessage.error(body.message || '导入失败')
  }
}

async function loadData() {
  loading.value = true
  try {
    const data = await pageProducts({ ...query })
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

async function loadCategories() {
  categories.value = await listCategories()
}

function handleSearch() {
  query.page = 1
  loadData()
}

function handleReset() {
  query.keyword = ''
  query.categoryId = undefined
  query.status = undefined
  query.page = 1
  loadData()
}

function openCreate() {
  editingProduct.value = null
  formVisible.value = true
}

function openEdit(row: Product) {
  editingProduct.value = row
  formVisible.value = true
}

function openDetail(row: Product) {
  detailProduct.value = row
  detailVisible.value = true
  priceChanges(row.id).then((list) => (priceHistory.value = list))
}

async function handleDelete(row: Product) {
  const confirmed = await ElMessageBox.confirm(
    `确定删除商品「${row.name}」吗？删除后不可恢复。`,
    '删除确认',
    { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
  ).catch(() => false)
  if (!confirmed) return

  await deleteProduct(row.id)
  ElMessage.success('删除成功')
  if (list.value.length === 1 && query.page > 1) {
    query.page -= 1
  }
  loadData()
}

onMounted(() => {
  loadData()
  loadCategories()
})
</script>
