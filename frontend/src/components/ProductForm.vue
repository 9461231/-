<template>
  <el-dialog
    v-model="visible"
    :title="isEdit ? '编辑商品' : '新增商品'"
    width="640px"
    :close-on-click-modal="false"
    destroy-on-close
    @closed="resetForm"
  >
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="100px"
      v-loading="loading"
    >
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="商品名称" prop="name">
            <el-input v-model="form.name" placeholder="请输入商品名称" maxlength="100" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="商品分类" prop="categoryId">
            <el-select v-model="form.categoryId" placeholder="请选择分类" style="width: 100%">
              <el-option
                v-for="c in categories"
                :key="c.id"
                :label="c.name"
                :value="c.id"
              />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="SKU 编码" prop="sku">
            <el-input v-model="form.sku" placeholder="商品唯一编码" maxlength="64" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="条码" prop="barcode">
            <el-input v-model="form.barcode" placeholder="可选，如 6901234500011" maxlength="64" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="品牌" prop="brand">
            <el-input v-model="form.brand" placeholder="可选" maxlength="64" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="规格" prop="spec">
            <el-input v-model="form.spec" placeholder="如 330ml*24罐" maxlength="64" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="单位" prop="unit">
            <el-input v-model="form.unit" placeholder="如 个 / 瓶 / 箱" maxlength="16" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="采购价" prop="purchasePrice">
            <el-input-number
              v-model="form.purchasePrice"
              :min="0"
              :precision="2"
              :step="0.5"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="销售价" prop="salePrice">
            <el-input-number
              v-model="form.salePrice"
              :min="0"
              :precision="2"
              :step="0.5"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="会员价">
            <el-input-number v-model="form.memberPrice" :min="0" :precision="2" :step="0.5"
              style="width: 100%" placeholder="可选" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="最低销售价">
            <el-input-number v-model="form.minSalePrice" :min="0" :precision="2" :step="0.5"
              style="width: 100%" placeholder="改价/促销下限，可选" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="状态" prop="status">
            <el-radio-group v-model="form.status">
              <el-radio :value="2">草稿</el-radio>
              <el-radio :value="1">在售</el-radio>
              <el-radio :value="0">停售</el-radio>
              <el-radio :value="3">归档</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="最低库存" prop="minStock">
            <el-input-number v-model="form.minStock" :min="0" :step="5" style="width: 100%" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="最高库存" prop="maxStock">
            <el-input-number
              v-model="form.maxStock"
              :min="form.minStock ?? 0"
              :step="10"
              style="width: 100%"
              placeholder="可选"
            />
          </el-form-item>
        </el-col>
      </el-row>

      <el-form-item label="备注" prop="remark">
        <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="255" />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import type { Product, ProductCategory, ProductRequest } from '@/types/product'
import { createProduct, updateProduct } from '@/api/product'

const props = defineProps<{
  categories: ProductCategory[]
  product: Product | null
}>()

const emit = defineEmits<{ saved: [] }>()

const visible = defineModel<boolean>({ required: true })

const isEdit = computed(() => !!props.product)
const loading = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()

const defaultForm = (): ProductRequest => ({
  categoryId: undefined as unknown as number,
  sku: '',
  barcode: '',
  name: '',
  brand: '',
  spec: '',
  unit: '',
  purchasePrice: 0,
  salePrice: 0,
  memberPrice: undefined,
  minSalePrice: undefined,
  minStock: 0,
  maxStock: undefined,
  status: 1,
  remark: ''
})

const form = reactive<ProductRequest>(defaultForm())

const rules: FormRules = {
  name: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择商品分类', trigger: 'change' }],
  sku: [{ required: true, message: '请输入 SKU 编码', trigger: 'blur' }],
  unit: [{ required: true, message: '请输入单位', trigger: 'blur' }],
  purchasePrice: [{ required: true, message: '请输入采购价', trigger: 'blur' }],
  salePrice: [
    { required: true, message: '请输入销售价', trigger: 'blur' },
    {
      validator: (_rule, value: number, callback) => {
        if (value < (form.purchasePrice ?? 0)) {
          callback(new Error('销售价不能低于采购价'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ],
  minStock: [{ required: true, message: '请输入最低库存', trigger: 'blur' }]
}

watch(visible, (open) => {
  if (open && props.product) {
    const p = props.product
    Object.assign(form, {
      categoryId: p.categoryId,
      sku: p.sku,
      barcode: p.barcode ?? '',
      name: p.name,
      brand: p.brand ?? '',
      spec: p.spec ?? '',
      unit: p.unit,
      purchasePrice: p.purchasePrice,
      salePrice: p.salePrice,
      memberPrice: p.memberPrice ?? undefined,
      minSalePrice: p.minSalePrice ?? undefined,
      minStock: p.minStock,
      maxStock: p.maxStock ?? undefined,
      status: p.status,
      remark: p.remark ?? ''
    })
  }
})

function resetForm() {
  Object.assign(form, defaultForm())
  formRef.value?.clearValidate()
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const payload: ProductRequest = {
      ...form,
      barcode: form.barcode?.trim() || undefined,
      brand: form.brand?.trim() || undefined,
      spec: form.spec?.trim() || undefined,
      maxStock: form.maxStock ?? undefined,
      remark: form.remark?.trim() || undefined
    }
    if (isEdit.value && props.product) {
      await updateProduct(props.product.id, payload)
      ElMessage.success('商品更新成功')
    } else {
      await createProduct(payload)
      ElMessage.success('商品创建成功')
    }
    visible.value = false
    emit('saved')
  } finally {
    submitting.value = false
  }
}
</script>
