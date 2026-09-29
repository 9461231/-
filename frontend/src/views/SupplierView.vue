<template>
  <div class="page-card">
    <div class="toolbar">
      <el-input
        v-model="query.keyword"
        placeholder="搜索：名称 / 联系人 / 电话"
        clearable
        style="width: 240px"
        @keyup.enter="handleSearch"
        @clear="handleSearch"
      />
      <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 140px" @change="handleSearch">
        <el-option label="合作中" :value="1" />
        <el-option label="已停止合作" :value="0" />
      </el-select>
      <el-button type="primary" @click="handleSearch">查询</el-button>
      <div style="flex: 1" />
      <el-button type="primary" @click="openCreate">新增供应商</el-button>
    </div>

    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="id" label="ID" width="70" align="center" />
      <el-table-column prop="name" label="供应商名称" min-width="160" />
      <el-table-column prop="contactPerson" label="联系人" width="110" />
      <el-table-column prop="phone" label="联系方式" width="140" />
      <el-table-column prop="address" label="地址" min-width="140" show-overflow-tooltip />
      <el-table-column prop="productCount" label="合作商品" width="90" align="center" />
      <el-table-column label="状态" width="110" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">
            {{ row.status === 1 ? '合作中' : '已停止' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="300" align="center" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openStats(row)">评分统计</el-button>
          <el-button link type="primary" @click="openProducts(row)">合作商品</el-button>
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

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

    <!-- 新增/编辑 -->
    <el-dialog v-model="dialogVisible" :title="editing ? '编辑供应商' : '新增供应商'" width="520px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" maxlength="100" />
        </el-form-item>
        <el-form-item label="联系人" prop="contactPerson">
          <el-input v-model="form.contactPerson" maxlength="50" />
        </el-form-item>
        <el-form-item label="联系方式" prop="phone">
          <el-input v-model="form.phone" maxlength="32" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" maxlength="100" />
        </el-form-item>
        <el-form-item label="地址" prop="address">
          <el-input v-model="form.address" maxlength="200" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">合作中</el-radio>
            <el-radio :value="0">已停止合作</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="255" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 供应商评分统计 -->
    <el-dialog v-model="statsVisible" :title="`履约统计 - ${statsSupplier?.name ?? ''}`" width="520px">
      <el-descriptions v-if="stats" :column="2" border>
        <el-descriptions-item label="综合评分">
          <el-tag :type="(stats.score ?? 0) >= 80 ? 'success' : (stats.score ?? 0) >= 60 ? 'warning' : 'danger'">
            {{ stats.score }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="订单数">{{ stats.orderCount }}（完成 {{ stats.completedCount }}）</el-descriptions-item>
        <el-descriptions-item label="采购总额">¥{{ (stats.totalAmount ?? 0).toFixed(2) }}</el-descriptions-item>
        <el-descriptions-item label="已付 / 未付">
          ¥{{ (stats.paidAmount ?? 0).toFixed(2) }} / ¥{{ (stats.unpaidAmount ?? 0).toFixed(2) }}
        </el-descriptions-item>
        <el-descriptions-item label="准时完成率">{{ stats.deliveryOnTimeRate }}%</el-descriptions-item>
        <el-descriptions-item label="退货率">{{ stats.returnRate }}%（{{ stats.returnCount }} 次）</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <!-- 合作商品抽屉 -->
    <el-drawer v-model="productsVisible" :title="`合作商品 - ${current?.name ?? ''}`" size="720px">
      <div class="toolbar">
        <el-select
          v-model="bindForm.productId"
          filterable
          remote
          :remote-method="searchProducts"
          :loading="productSearching"
          placeholder="搜索并选择商品"
          style="width: 260px"
        >
          <el-option v-for="p in productOptions" :key="p.id" :label="`${p.sku} ${p.name}`" :value="p.id" />
        </el-select>
        <el-input-number v-model="bindForm.supplyPrice" :min="0" :precision="2" placeholder="供货价" style="width: 150px" />
        <el-button type="primary" :disabled="!bindForm.productId" @click="handleBind">绑定</el-button>
      </div>
      <el-table :data="supplierProducts" border v-loading="productsLoading">
        <el-table-column prop="sku" label="SKU" width="120" />
        <el-table-column prop="productName" label="商品名称" min-width="140" />
        <el-table-column label="供货价" width="120" align="right">
          <template #default="{ row }">
            <span @click="startEditPrice(row)" style="cursor: pointer">
              ¥{{ row.supplyPrice.toFixed(2) }}
              <el-icon style="font-size: 12px"><Edit /></el-icon>
            </span>
          </template>
        </el-table-column>
        <el-table-column label="档案采购价" width="110" align="right">
          <template #default="{ row }">¥{{ row.purchasePrice?.toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="主供货" width="80" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.isPrimary" type="warning" size="small">主供</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90" align="center">
          <template #default="{ row }">
            <el-button link type="danger" @click="handleUnbind(row)">解除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Edit } from '@element-plus/icons-vue'
import type { Supplier, SupplierProduct } from '@/types/supplier'
import type { PageVO, Product } from '@/types/product'
import {
  pageSuppliers, createSupplier, updateSupplier, deleteSupplier,
  listSupplierProducts, bindSupplierProduct, updateSupplyPrice, unbindSupplierProduct
} from '@/api/supplier'
import { supplierStats } from '@/api/extensions'
import { pageProducts } from '@/api/product'

const list = ref<Supplier[]>([])
const total = ref(0)
const loading = ref(false)
const submitting = ref(false)

const query = reactive({ page: 1, size: 10, keyword: '', status: undefined as number | undefined })

const dialogVisible = ref(false)
const editing = ref<Supplier | null>(null)
const formRef = ref<FormInstance>()
const form = reactive<Partial<Supplier>>({})
const rules: FormRules = { name: [{ required: true, message: '请输入供应商名称', trigger: 'blur' }] }

const productsVisible = ref(false)
const productsLoading = ref(false)
const current = ref<Supplier | null>(null)
const supplierProducts = ref<SupplierProduct[]>([])
const bindForm = reactive({ productId: undefined as number | undefined, supplyPrice: 0 })
const productOptions = ref<Product[]>([])
const productSearching = ref(false)

async function loadData() {
  loading.value = true
  try {
    const data: PageVO<Supplier> = await pageSuppliers({ ...query })
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.page = 1
  loadData()
}

function openCreate() {
  editing.value = null
  Object.assign(form, { name: '', contactPerson: '', phone: '', email: '', address: '', status: 1, remark: '' })
  dialogVisible.value = true
}

function openEdit(row: Supplier) {
  editing.value = row
  Object.assign(form, row)
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (editing.value) {
      await updateSupplier(editing.value.id, form)
      ElMessage.success('更新成功')
    } else {
      await createSupplier(form)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row: Supplier) {
  const ok = await ElMessageBox.confirm(`确定删除供应商「${row.name}」吗？`, '删除确认', { type: 'warning' })
    .catch(() => false)
  if (!ok) return
  await deleteSupplier(row.id)
  ElMessage.success('删除成功')
  loadData()
}

async function openProducts(row: Supplier) {
  current.value = row
  productsVisible.value = true
  productsLoading.value = true
  bindForm.productId = undefined
  bindForm.supplyPrice = 0
  try {
    supplierProducts.value = await listSupplierProducts(row.id)
  } finally {
    productsLoading.value = false
  }
  searchProducts('')
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

async function handleBind() {
  if (!current.value || !bindForm.productId) return
  await bindSupplierProduct(current.value.id, {
    productId: bindForm.productId,
    supplyPrice: bindForm.supplyPrice
  })
  ElMessage.success('绑定成功')
  supplierProducts.value = await listSupplierProducts(current.value.id)
}

async function startEditPrice(row: SupplierProduct) {
  const { value } = await ElMessageBox.prompt('请输入新的供货价', '修改供货价', {
    inputValue: String(row.supplyPrice),
    inputPattern: /^\d+(\.\d{1,2})?$/,
    inputErrorMessage: '请输入正确的价格'
  }).catch(() => ({ value: '' }))
  if (!value || !current.value) return
  await updateSupplyPrice(current.value.id, row.productId, Number(value))
  ElMessage.success('修改成功')
  supplierProducts.value = await listSupplierProducts(current.value.id)
}

async function handleUnbind(row: SupplierProduct) {
  if (!current.value) return
  const ok = await ElMessageBox.confirm(`解除商品「${row.productName}」的绑定？`, '提示', { type: 'warning' })
    .catch(() => false)
  if (!ok) return
  await unbindSupplierProduct(current.value.id, row.productId)
  ElMessage.success('已解除绑定')
  supplierProducts.value = await listSupplierProducts(current.value.id)
}

// 评分统计
const statsVisible = ref(false)
const statsSupplier = ref<Supplier | null>(null)
const stats = ref<any>(null)

async function openStats(row: Supplier) {
  statsSupplier.value = row
  stats.value = await supplierStats(row.id)
  statsVisible.value = true
}

onMounted(loadData)
</script>
