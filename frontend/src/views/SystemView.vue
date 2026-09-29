<template>
  <div class="page-card">
    <el-tabs v-model="activeTab">
      <!-- 店内员工 -->
      <el-tab-pane v-if="canManageStaff" label="店内员工" name="users">
        <div class="toolbar">
          <el-input v-model="userQuery.keyword" placeholder="搜索用户名/姓名" clearable style="width: 200px" @keyup.enter="loadUsers" />
          <el-button type="primary" @click="loadUsers">查询</el-button>
          <div style="flex: 1" />
          <el-button v-if="auth.user?.role === 'SUPER_ADMIN' || auth.user?.role === 'STORE_OWNER'"
            type="primary" @click="openCreateUser">新增员工</el-button>
        </div>
        <el-table :data="users" v-loading="userLoading" border stripe>
          <el-table-column prop="username" label="用户名" width="130" />
          <el-table-column prop="realName" label="姓名" width="120" />
          <el-table-column prop="roleLabel" label="角色" width="110" align="center">
            <template #default="{ row }">
              <el-tag>{{ row.roleLabel }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="storeName" label="所属店家" min-width="130" />
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '正常' : '禁用' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="创建时间" width="170" />
          <el-table-column v-if="auth.user?.role === 'SUPER_ADMIN' || auth.user?.role === 'STORE_OWNER'"
            label="操作" width="230" align="center" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openEditUser(row)">编辑</el-button>
              <el-button link type="warning" @click="handleResetPwd(row)">重置密码</el-button>
              <el-button link :type="row.status === 1 ? 'danger' : 'success'" @click="handleUserStatus(row)">
                {{ row.status === 1 ? '禁用' : '启用' }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- 店家/租户（仅超管） -->
      <el-tab-pane v-if="auth.user?.role === 'SUPER_ADMIN'" label="店家/租户" name="stores">
        <div class="toolbar">
          <el-input v-model="storeQuery.keyword" placeholder="搜索店家名称/联系人" clearable style="width: 220px" @keyup.enter="loadStores" />
          <el-button type="primary" @click="loadStores">查询</el-button>
        </div>
        <el-table :data="stores" v-loading="storeLoading" border stripe>
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="name" label="店家名称" min-width="150" />
          <el-table-column prop="contactPerson" label="联系人" width="110" />
          <el-table-column prop="phone" label="电话" width="130" />
          <el-table-column prop="userCount" label="员工数" width="90" align="center" />
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : row.status === 0 ? 'warning' : 'danger'">
                {{ row.status === 1 ? '正常' : row.status === 0 ? '待审核' : '已禁用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180" align="center">
            <template #default="{ row }">
              <el-button v-if="row.status !== 1" link type="success" @click="handleStoreStatus(row, 1)">启用</el-button>
              <el-button v-if="row.status === 1" link type="danger" @click="handleStoreStatus(row, 2)">禁用</el-button>
              <el-button v-if="row.status === 0" link type="success" @click="handleStoreStatus(row, 1)">审核通过</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- 审计日志 -->
      <el-tab-pane v-if="canManageStaff" label="操作审计" name="audit">
        <div class="toolbar">
          <el-input v-model="auditQuery.username" placeholder="操作人" clearable style="width: 140px" @keyup.enter="loadAudit" />
          <el-select v-model="auditQuery.source" placeholder="来源" clearable style="width: 130px" @change="loadAudit">
            <el-option label="页面 WEB" value="WEB" />
            <el-option label="AI Agent" value="AGENT" />
            <el-option label="系统 SYSTEM" value="SYSTEM" />
            <el-option label="导入 IMPORT" value="IMPORT" />
          </el-select>
          <el-button type="primary" @click="loadAudit">查询</el-button>
        </div>
        <el-table :data="auditLogs" v-loading="auditLoading" border stripe size="small">
          <el-table-column prop="createdAt" label="时间" width="170" />
          <el-table-column prop="username" label="操作人" width="110" />
          <el-table-column prop="role" label="角色" width="130" />
          <el-table-column prop="operationType" label="操作" width="80" align="center" />
          <el-table-column prop="targetType" label="目标" width="160" />
          <el-table-column prop="targetId" label="目标ID" width="80" align="center" />
          <el-table-column prop="source" label="来源" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="row.source === 'AGENT' ? 'warning' : 'info'" size="small">{{ row.source }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="afterData" label="内容摘要" min-width="200" show-overflow-tooltip />
        </el-table>
        <div class="pagination">
          <el-pagination v-model:current-page="auditQuery.page" v-model:page-size="auditQuery.size"
            :total="auditTotal" layout="total, prev, pager, next" @current-change="loadAudit" />
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 员工表单 -->
    <el-dialog v-model="userDialogVisible" :title="editingUser ? '编辑员工' : '新增员工'" width="460px"
      :close-on-click-modal="false">
      <el-form ref="userFormRef" :model="userForm" :rules="userRules" label-width="90px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="userForm.username" :disabled="!!editingUser" maxlength="50" />
        </el-form-item>
        <el-form-item v-if="!editingUser" label="初始密码" prop="password">
          <el-input v-model="userForm.password" show-password maxlength="50" placeholder="至少8位" />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="userForm.realName" maxlength="50" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="userForm.role" style="width: 100%">
            <el-option label="店主" value="STORE_OWNER" :disabled="auth.user?.role !== 'SUPER_ADMIN'" />
            <el-option label="店长" value="STORE_MANAGER" />
            <el-option label="收银员" value="CASHIER" />
            <el-option label="采购员" value="PURCHASER" />
            <el-option label="库管员" value="WAREHOUSE" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="userDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleUserSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import { auth, hasRole } from '@/stores/auth'
import {
  pageUsers, createUser, updateUser, resetUserPassword, updateUserStatus,
  pageStores, updateStore, pageAuditLogs, type UserVO
} from '@/api/system'

const activeTab = ref('users')
const canManageStaff = computed(() => hasRole('STORE_OWNER', 'STORE_MANAGER'))
const submitting = ref(false)

// 员工
const users = ref<UserVO[]>([])
const userLoading = ref(false)
const userQuery = reactive({ page: 1, size: 15, keyword: '' })
const userDialogVisible = ref(false)
const editingUser = ref<UserVO | null>(null)
const userFormRef = ref<FormInstance>()
const userForm = reactive({ username: '', password: '', realName: '', role: 'CASHIER' })
const userRules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, min: 8, message: '密码至少8位', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

// 店家
const stores = ref<any[]>([])
const storeLoading = ref(false)
const storeQuery = reactive({ page: 1, size: 15, keyword: '' })

// 审计
const auditLogs = ref<any[]>([])
const auditLoading = ref(false)
const auditTotal = ref(0)
const auditQuery = reactive({ page: 1, size: 15, username: '', source: '' })

async function loadUsers() {
  userLoading.value = true
  try {
    const data = await pageUsers({ ...userQuery })
    users.value = data.list
  } finally {
    userLoading.value = false
  }
}

function openCreateUser() {
  editingUser.value = null
  Object.assign(userForm, { username: '', password: '', realName: '', role: 'CASHIER' })
  userDialogVisible.value = true
}

function openEditUser(row: UserVO) {
  editingUser.value = row
  Object.assign(userForm, { username: row.username, password: '', realName: row.realName, role: row.role })
  userDialogVisible.value = true
}

async function handleUserSubmit() {
  const valid = await userFormRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (editingUser.value) {
      await updateUser(editingUser.value.id, {
        realName: userForm.realName,
        role: userForm.role
      })
      ElMessage.success('更新成功')
    } else {
      await createUser({ ...userForm })
      ElMessage.success('员工创建成功')
    }
    userDialogVisible.value = false
    loadUsers()
  } finally {
    submitting.value = false
  }
}

async function handleResetPwd(row: UserVO) {
  const r = await ElMessageBox.prompt(`重置「${row.username}」的密码（至少8位）`, '重置密码')
    .catch(() => null)
  if (!r) return
  await resetUserPassword(row.id, r.value)
  ElMessage.success('密码已重置')
}

async function handleUserStatus(row: UserVO) {
  await updateUserStatus(row.id, row.status === 1 ? 0 : 1)
  ElMessage.success(row.status === 1 ? '已禁用' : '已启用')
  loadUsers()
}

async function loadStores() {
  storeLoading.value = true
  try {
    const data = await pageStores({ ...storeQuery })
    stores.value = data.list
  } finally {
    storeLoading.value = false
  }
}

async function handleStoreStatus(row: any, status: number) {
  await updateStore(row.id, { ...row, status })
  ElMessage.success('已更新')
  loadStores()
}

async function loadAudit() {
  auditLoading.value = true
  try {
    const data = await pageAuditLogs({
      page: auditQuery.page,
      size: auditQuery.size,
      username: auditQuery.username || undefined,
      source: auditQuery.source || undefined
    })
    auditLogs.value = data.list
    auditTotal.value = data.total
  } finally {
    auditLoading.value = false
  }
}

onMounted(async () => {
  if (canManageStaff.value) loadUsers()
  if (auth.user?.role === 'SUPER_ADMIN') loadStores()
  if (canManageStaff.value) loadAudit()
})
</script>
