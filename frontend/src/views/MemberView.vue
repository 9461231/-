<template>
  <div class="page-card">
    <div class="toolbar">
      <el-input v-model="query.keyword" placeholder="搜索：姓名 / 手机号 / 会员号" clearable style="width: 240px"
        @keyup.enter="loadData" @clear="loadData" />
      <el-select v-model="query.levelId" placeholder="全部等级" clearable style="width: 140px" @change="loadData">
        <el-option v-for="l in levels" :key="l.id" :label="l.name" :value="l.id" />
      </el-select>
      <el-button type="primary" @click="loadData">查询</el-button>
      <div style="flex: 1" />
      <el-button type="primary" @click="openCreate">新增会员</el-button>
    </div>

    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="memberNo" label="会员号" width="200" />
      <el-table-column prop="name" label="姓名" width="110" />
      <el-table-column prop="phone" label="手机号" width="130" />
      <el-table-column label="性别" width="70" align="center">
        <template #default="{ row }">{{ row.gender === 1 ? '男' : row.gender === 2 ? '女' : '—' }}</template>
      </el-table-column>
      <el-table-column prop="levelName" label="等级" width="100" align="center">
        <template #default="{ row }">
          <el-tag type="warning">{{ row.levelName }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="points" label="积分" width="90" align="center" />
      <el-table-column label="储值余额" width="110" align="right">
        <template #default="{ row }">¥{{ (row.balance ?? 0).toFixed(2) }}</template>
      </el-table-column>
      <el-table-column label="累计消费" width="120" align="right">
        <template #default="{ row }">¥{{ row.totalSpent?.toFixed(2) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '正常' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="注册时间" width="170" />
      <el-table-column label="操作" width="240" align="center" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="warning" @click="openPoints(row)">积分</el-button>
          <el-button link type="success" @click="openRecharge(row)">充值</el-button>
          <el-button link type="primary" @click="openRecords(row)">记录</el-button>
          <el-button link :type="row.status === 1 ? 'danger' : 'success'" @click="toggleStatus(row)">
            {{ row.status === 1 ? '停用' : '启用' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination">
      <el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :total="total"
        :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" @size-change="loadData"
        @current-change="loadData" />
    </div>

    <!-- 新增/编辑 -->
    <el-dialog v-model="dialogVisible" :title="editing ? '编辑会员' : '新增会员'" width="460px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="姓名" prop="name">
          <el-input v-model="form.name" maxlength="50" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" maxlength="11" />
        </el-form-item>
        <el-form-item label="性别" prop="gender">
          <el-radio-group v-model="form.gender">
            <el-radio :value="1">男</el-radio>
            <el-radio :value="2">女</el-radio>
            <el-radio :value="0">未知</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="生日" prop="birthday">
          <el-date-picker v-model="form.birthday" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 积分调整 -->
    <el-dialog v-model="pointsVisible" title="积分调整" width="420px" :close-on-click-modal="false">
      <div v-if="pointsMember" style="margin-bottom: 10px; color: #909399">
        {{ pointsMember.name }}（{{ pointsMember.phone }}）当前积分：{{ pointsMember.points }}
      </div>
      <el-form label-width="80px">
        <el-form-item label="变动">
          <el-input-number v-model="pointsForm.change" :step="10" style="width: 100%" />
          <div style="color: #909399; font-size: 12px">正数累计，负数扣减</div>
        </el-form-item>
        <el-form-item label="原因">
          <el-input v-model="pointsForm.reason" maxlength="255" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pointsVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handlePoints">确认</el-button>
      </template>
    </el-dialog>

    <!-- 储值充值 -->
    <el-dialog v-model="rechargeVisible" title="会员储值充值" width="400px" :close-on-click-modal="false">
      <div v-if="rechargeMember" style="margin-bottom: 10px; color: #909399">
        {{ rechargeMember.name }} 当前余额：¥{{ (rechargeMember.balance ?? 0).toFixed(2) }}
      </div>
      <el-form label-width="80px">
        <el-form-item label="充值金额">
          <el-input-number v-model="rechargeAmount" :min="1" :precision="2" :step="50" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rechargeVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleRecharge">确认充值</el-button>
      </template>
    </el-dialog>

    <!-- 积分/消费记录 -->
    <el-drawer v-model="recordsVisible" :title="`记录 - ${recordsMember?.name ?? ''}`" size="560px">
      <template v-if="recordsMember">
        <h4 style="margin-bottom: 8px">积分明细</h4>
        <el-table :data="pointsRecords" border size="small" max-height="260">
          <el-table-column prop="createdAt" label="时间" width="160" />
          <el-table-column label="变动" width="80" align="center">
            <template #default="{ row }">
              <span :style="{ color: row.change > 0 ? '#67C23A' : '#F56C6C' }">
                {{ row.change > 0 ? '+' : '' }}{{ row.change }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="remark" label="说明" min-width="140" />
        </el-table>
        <h4 style="margin: 14px 0 8px">消费记录</h4>
        <el-table :data="consumptions" border size="small" max-height="260">
          <el-table-column prop="createdAt" label="时间" width="160" />
          <el-table-column prop="orderNo" label="订单号" width="190" />
          <el-table-column label="金额" width="90" align="right">
            <template #default="{ row }">¥{{ row.amount.toFixed(2) }}</template>
          </el-table-column>
          <el-table-column prop="pointsEarned" label="积分" width="70" align="center" />
        </el-table>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { PageVO } from '@/types/product'
import {
  pageMembers, createMember, updateMember, updateMemberStatus, adjustMemberPoints,
  memberPointsRecords, memberConsumptions, memberLevels,
  type Member, type MemberLevel, type PointsRecord, type ConsumptionRecord
} from '@/api/member'
import { rechargeBalance } from '@/api/extensions'

const list = ref<Member[]>([])
const levels = ref<MemberLevel[]>([])
const total = ref(0)
const loading = ref(false)
const submitting = ref(false)
const query = reactive({ page: 1, size: 10, keyword: '', levelId: undefined as number | undefined })

const dialogVisible = ref(false)
const editing = ref<Member | null>(null)
const formRef = ref<FormInstance>()
const form = reactive<{ name: string; phone: string; gender: number; birthday: string | null }>({
  name: '', phone: '', gender: 0, birthday: null
})
const rules: FormRules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1\d{10}$/, message: '手机号格式不正确', trigger: 'blur' }
  ]
}

const pointsVisible = ref(false)
const pointsMember = ref<Member | null>(null)
const pointsForm = reactive({ change: 0, reason: '' })

const recordsVisible = ref(false)
const recordsMember = ref<Member | null>(null)
const pointsRecords = ref<PointsRecord[]>([])
const consumptions = ref<ConsumptionRecord[]>([])

async function loadData() {
  loading.value = true
  try {
    const data: PageVO<Member> = await pageMembers({ ...query })
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editing.value = null
  Object.assign(form, { name: '', phone: '', gender: 0, birthday: null })
  dialogVisible.value = true
}

function openEdit(row: Member) {
  editing.value = row
  Object.assign(form, { name: row.name, phone: row.phone, gender: row.gender, birthday: row.birthday })
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (editing.value) {
      await updateMember(editing.value.id, form)
      ElMessage.success('更新成功')
    } else {
      await createMember(form)
      ElMessage.success('会员创建成功')
    }
    dialogVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

async function toggleStatus(row: Member) {
  await updateMemberStatus(row.id, row.status === 1 ? 0 : 1)
  ElMessage.success(row.status === 1 ? '已停用' : '已启用')
  loadData()
}

function openPoints(row: Member) {
  pointsMember.value = row
  pointsForm.change = 0
  pointsForm.reason = ''
  pointsVisible.value = true
}

async function handlePoints() {
  if (!pointsMember.value) return
  if (!pointsForm.change) return ElMessage.warning('变动不能为0')
  if (!pointsForm.reason.trim()) return ElMessage.warning('请填写原因')
  submitting.value = true
  try {
    await adjustMemberPoints(pointsMember.value.id, pointsForm)
    ElMessage.success('积分调整成功')
    pointsVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

const rechargeVisible = ref(false)
const rechargeMember = ref<Member | null>(null)
const rechargeAmount = ref(100)

function openRecharge(row: Member) {
  rechargeMember.value = row
  rechargeAmount.value = 100
  rechargeVisible.value = true
}

async function handleRecharge() {
  if (!rechargeMember.value) return
  submitting.value = true
  try {
    await rechargeBalance(rechargeMember.value.id, rechargeAmount.value)
    ElMessage.success(`充值成功：¥${rechargeAmount.value.toFixed(2)}`)
    rechargeVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

async function openRecords(row: Member) {
  recordsMember.value = row
  recordsVisible.value = true
  pointsRecords.value = await memberPointsRecords(row.id)
  consumptions.value = await memberConsumptions(row.id)
}

onMounted(async () => {
  loadData()
  levels.value = await memberLevels()
})
</script>
