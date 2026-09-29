<template>
  <div class="page-card">
    <div class="toolbar">
      <el-select v-model="query.type" placeholder="全部类型" clearable style="width: 150px" @change="loadData">
        <el-option v-for="(label, key) in PROMOTION_TYPES" :key="key" :label="label" :value="Number(key)" />
      </el-select>
      <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 130px" @change="loadData">
        <el-option label="启用" :value="1" />
        <el-option label="停用" :value="0" />
      </el-select>
      <el-button type="primary" @click="loadData">查询</el-button>
      <div style="flex: 1" />
      <el-button type="primary" @click="openCreate">创建促销活动</el-button>
    </div>

    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="name" label="活动名称" min-width="140" />
      <el-table-column prop="typeLabel" label="类型" width="110" align="center">
        <template #default="{ row }">
          <el-tag>{{ row.typeLabel }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="规则" min-width="180">
        <template #default="{ row }">
          <span v-if="row.type === 1">{{ (row.discountRate * 10).toFixed(1) }} 折</span>
          <span v-else-if="row.type === 2">满 ¥{{ row.minAmount }} 减 ¥{{ row.reduceAmount }}</span>
          <span v-else-if="row.type === 3">第二件 {{ (row.secondRate * 10).toFixed(1) }} 折</span>
          <span v-else>会员价 ¥{{ row.memberPrice }}</span>
        </template>
      </el-table-column>
      <el-table-column label="参与商品" width="100" align="center">
        <template #default="{ row }">{{ row.productIds.length }} 个</template>
      </el-table-column>
      <el-table-column label="活动时间" width="320">
        <template #default="{ row }">{{ row.startTime }} ~ {{ row.endTime }}</template>
      </el-table-column>
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" align="center" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link :type="row.status === 1 ? 'warning' : 'success'" @click="toggleStatus(row)">
            {{ row.status === 1 ? '停用' : '启用' }}
          </el-button>
          <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination">
      <el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :total="total"
        layout="total, prev, pager, next" @current-change="loadData" />
    </div>

    <!-- 优惠券 -->
    <h4 style="margin: 20px 0 10px">优惠券（满减券 · 收银时报券码核销）</h4>
    <div class="toolbar">
      <span style="color: #909399; font-size: 13px">
        促销优先级说明：同一商品多个促销同时生效时，系统自动取"优惠金额最大"的活动（程序确定性规则，非 AI 决定）
      </span>
      <div style="flex: 1" />
      <el-button type="primary" plain @click="openCouponCreate">创建优惠券</el-button>
      <el-button plain @click="openIssue">发放优惠券</el-button>
    </div>
    <el-table :data="coupons" border size="small" max-height="260">
      <el-table-column prop="name" label="名称" min-width="130" />
      <el-table-column label="规则" width="150">
        <template #default="{ row }">满 ¥{{ row.minAmount }} 减 ¥{{ row.reduceAmount }}</template>
      </el-table-column>
      <el-table-column prop="validUntil" label="有效期至" width="170" />
      <el-table-column label="发放" width="100" align="center">
        <template #default="{ row }">{{ row.issuedCount }} / {{ row.totalCount }}</template>
      </el-table-column>
      <el-table-column label="状态" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="90" align="center">
        <template #default="{ row }">
          <el-button link :type="row.status === 1 ? 'warning' : 'success'"
            @click="handleCouponStatus(row)">{{ row.status === 1 ? '停用' : '启用' }}</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 发放优惠券 -->
    <el-dialog v-model="issueVisible" title="发放优惠券" width="420px" :close-on-click-modal="false">
      <el-form label-width="90px">
        <el-form-item label="优惠券" required>
          <el-select v-model="issueForm.couponId" style="width: 100%">
            <el-option v-for="c in coupons.filter((x) => x.status === 1)" :key="c.id"
              :label="`${c.name}（剩 ${c.totalCount - c.issuedCount} 张）`" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="会员" required>
          <el-select v-model="issueForm.memberId" filterable style="width: 100%">
            <el-option v-for="m in memberOptions" :key="m.id" :label="`${m.phone} ${m.name}`" :value="m.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <el-alert v-if="issuedCode" type="success" :closable="false"
        :title="`券码：${issuedCode}（收银时输入该券码即可抵扣）`" />
      <template #footer>
        <el-button @click="issueVisible = false">关闭</el-button>
        <el-button type="primary" @click="handleIssue">确认发放</el-button>
      </template>
    </el-dialog>

    <!-- 创建优惠券 -->
    <el-dialog v-model="couponDialogVisible" title="创建优惠券" width="440px" :close-on-click-modal="false">
      <el-form label-width="90px">
        <el-form-item label="名称" required>
          <el-input v-model="couponForm.name" maxlength="100" />
        </el-form-item>
        <el-form-item label="门槛金额" required>
          <el-input-number v-model="couponForm.minAmount" :min="0.01" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="抵扣金额" required>
          <el-input-number v-model="couponForm.reduceAmount" :min="0.01" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="有效期至" required>
          <el-date-picker v-model="couponForm.validUntil" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="发放总量" required>
          <el-input-number v-model="couponForm.totalCount" :min="1" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="couponDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCouponCreate">创建</el-button>
      </template>
    </el-dialog>

    <!-- 创建/编辑 -->
    <el-dialog v-model="dialogVisible" :title="editing ? '编辑促销活动' : '创建促销活动'" width="680px"
      :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="活动名称" prop="name">
          <el-input v-model="form.name" maxlength="100" />
        </el-form-item>
        <el-form-item label="促销类型" prop="type">
          <el-radio-group v-model="form.type" :disabled="!!editing">
            <el-radio :value="1">直接折扣</el-radio>
            <el-radio :value="2">满减</el-radio>
            <el-radio :value="3">第二件优惠</el-radio>
            <el-radio :value="4">会员专享价</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="form.type === 1" label="折扣率" required>
          <el-input-number v-model="form.discountRate" :min="0.01" :max="1" :step="0.05" :precision="2" style="width: 200px" />
          <span style="margin-left: 8px; color: #909399">{{ form.discountRate ? (form.discountRate * 10).toFixed(1) + ' 折' : '' }}</span>
        </el-form-item>
        <template v-if="form.type === 2">
          <el-form-item label="门槛金额" required>
            <el-input-number v-model="form.minAmount" :min="0" :precision="2" style="width: 200px" />
          </el-form-item>
          <el-form-item label="减免金额" required>
            <el-input-number v-model="form.reduceAmount" :min="0.01" :precision="2" style="width: 200px" />
          </el-form-item>
        </template>
        <el-form-item v-if="form.type === 3" label="第二件折扣" required>
          <el-input-number v-model="form.secondRate" :min="0.01" :max="1" :step="0.05" :precision="2" style="width: 200px" />
        </el-form-item>
        <el-form-item v-if="form.type === 4" label="会员价" required>
          <el-input-number v-model="form.memberPrice" :min="0" :precision="2" style="width: 200px" />
        </el-form-item>
        <el-form-item label="活动时间" required>
          <el-date-picker v-model="timeRange" type="datetimerange" value-format="YYYY-MM-DDTHH:mm:ss"
            start-placeholder="开始时间" end-placeholder="结束时间" style="width: 100%" />
        </el-form-item>
        <el-form-item label="参与商品" required>
          <el-select v-model="form.productIds" multiple filterable remote :remote-method="searchProducts"
            :loading="productSearching" placeholder="搜索并选择商品" style="width: 100%">
            <el-option v-for="p in productOptions" :key="p.id" :label="`${p.sku} ${p.name}`" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="255" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">保存（默认停用，保存后手动启用）</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { PageVO, Product } from '@/types/product'
import {
  pagePromotions, createPromotion, updatePromotion, deletePromotion,
  updatePromotionStatus, PROMOTION_TYPES, type Promotion, type PromotionRequest
} from '@/api/promotion'
import { pageProducts } from '@/api/product'
import { pageMembers, type Member } from '@/api/member'
import { listCoupons, createCoupon, updateCouponStatus, issueCoupon, type Coupon } from '@/api/extensions'

const list = ref<Promotion[]>([])
const total = ref(0)
const loading = ref(false)
const submitting = ref(false)
const query = reactive({ page: 1, size: 10, type: undefined as number | undefined, status: undefined as number | undefined })

const dialogVisible = ref(false)
const editing = ref<Promotion | null>(null)
const formRef = ref<FormInstance>()
const timeRange = ref<string[]>([])
const productOptions = ref<Product[]>([])
const productSearching = ref(false)

const form = reactive<PromotionRequest>({
  name: '', type: 1, startTime: '', endTime: '',
  discountRate: 0.8, minAmount: 0, reduceAmount: 0, secondRate: 0.5,
  memberPrice: 0, remark: '', productIds: []
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入活动名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }]
}

async function loadData() {
  loading.value = true
  try {
    const data: PageVO<Promotion> = await pagePromotions({ ...query })
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
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
  editing.value = null
  Object.assign(form, {
    name: '', type: 1, startTime: '', endTime: '', discountRate: 0.8,
    minAmount: 0, reduceAmount: 0, secondRate: 0.5, memberPrice: 0, remark: '', productIds: []
  })
  timeRange.value = []
  searchProducts('')
  dialogVisible.value = true
}

function openEdit(row: Promotion) {
  editing.value = row
  Object.assign(form, {
    name: row.name, type: row.type, startTime: row.startTime, endTime: row.endTime,
    discountRate: row.discountRate ?? 0.8, minAmount: row.minAmount ?? 0,
    reduceAmount: row.reduceAmount ?? 0, secondRate: row.secondRate ?? 0.5,
    memberPrice: row.memberPrice ?? 0, remark: row.remark ?? '', productIds: row.productIds
  })
  timeRange.value = [row.startTime, row.endTime]
  searchProducts('')
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  if (!timeRange.value || timeRange.value.length !== 2) return ElMessage.warning('请选择活动时间')
  if (!form.productIds.length) return ElMessage.warning('请选择参与商品')

  const payload: PromotionRequest = { ...form, startTime: timeRange.value[0], endTime: timeRange.value[1] }
  submitting.value = true
  try {
    if (editing.value) {
      await updatePromotion(editing.value.id, payload)
      ElMessage.success('更新成功')
    } else {
      await createPromotion(payload)
      ElMessage.success('活动已创建（默认停用）')
    }
    dialogVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

async function toggleStatus(row: Promotion) {
  await updatePromotionStatus(row.id, row.status === 1 ? 0 : 1)
  ElMessage.success(row.status === 1 ? '已停用' : '已启用')
  loadData()
}

async function handleDelete(row: Promotion) {
  const ok = await ElMessageBox.confirm(`确定删除活动「${row.name}」吗？`, '删除确认', { type: 'warning' }).catch(() => false)
  if (!ok) return
  await deletePromotion(row.id)
  ElMessage.success('删除成功')
  loadData()
}

// ==================== 优惠券 ====================
const coupons = ref<Coupon[]>([])
const couponDialogVisible = ref(false)
const couponForm = reactive({
  name: '',
  minAmount: 50,
  reduceAmount: 10,
  validUntil: '',
  totalCount: 100
})

async function loadCoupons() {
  coupons.value = await listCoupons()
}

function openCouponCreate() {
  couponForm.validUntil = ''
  couponDialogVisible.value = true
}

async function handleCouponCreate() {
  if (!couponForm.name.trim() || !couponForm.validUntil) return ElMessage.warning('请完善优惠券信息')
  await createCoupon({ ...couponForm })
  ElMessage.success('优惠券已创建')
  couponDialogVisible.value = false
  loadCoupons()
}

async function handleCouponStatus(row: Coupon) {
  await updateCouponStatus(row.id, row.status === 1 ? 0 : 1)
  ElMessage.success('已更新')
  loadCoupons()
}

// 发放
const issueVisible = ref(false)
const issueForm = reactive({ couponId: undefined as number | undefined, memberId: undefined as number | undefined })
const memberOptions = ref<Member[]>([])
const issuedCode = ref('')

async function openIssue() {
  if (!coupons.value.some((c) => c.status === 1)) {
    return ElMessage.warning('没有启用中的优惠券')
  }
  const data = await pageMembers({ page: 1, size: 100, status: 1 })
  memberOptions.value = data.list
  issueForm.couponId = undefined
  issueForm.memberId = undefined
  issuedCode.value = ''
  issueVisible.value = true
}

async function handleIssue() {
  if (!issueForm.couponId || !issueForm.memberId) return ElMessage.warning('请选择优惠券与会员')
  const mc = await issueCoupon(issueForm.couponId, issueForm.memberId)
  issuedCode.value = mc.code
  ElMessage.success('发放成功')
  loadCoupons()
}

onMounted(() => {
  loadData()
  loadCoupons()
})
</script>
