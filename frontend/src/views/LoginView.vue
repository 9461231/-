<template>
  <div class="login-page">
    <div class="login-card">
      <h2>🛒 中小型超市运营管理系统</h2>
      <p class="sub">多租户业务运营平台 · 可解释 AI Agent</p>
      <el-form ref="formRef" :model="form" :rules="rules" @keyup.enter="handleLogin">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" size="large">
            <template #prefix><el-icon><User /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="密码" size="large">
            <template #prefix><el-icon><Lock /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-button type="primary" size="large" style="width: 100%" :loading="loading" @click="handleLogin">
          登 录
        </el-button>
      </el-form>
      <el-divider />
      <div class="demo-accounts">
        <div class="hint">演示账号（点击填入）：</div>
        <el-tag v-for="acc in demoAccounts" :key="acc.u" style="cursor: pointer; margin: 4px"
          @click="form.username = acc.u; form.password = acc.p">
          {{ acc.label }}：{{ acc.u }}
        </el-tag>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { login } from '@/api/system'
import { setLogin } from '@/stores/auth'

const router = useRouter()
const formRef = ref<FormInstance>()
const loading = ref(false)
const form = reactive({ username: '', password: '' })
const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const demoAccounts = [
  { label: '超级管理员', u: 'admin', p: 'Admin@123456' },
  { label: '店主', u: 'store_demo', p: 'Store@123456' },
  { label: '收银员', u: 'cashier01', p: 'Cashier@123456' },
  { label: '采购员', u: 'purchaser01', p: 'Purchase@123456' },
  { label: '库管员', u: 'warehouse01', p: 'Warehouse@123456' }
]

async function handleLogin() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    const user = await login(form.username, form.password)
    setLogin(user)
    ElMessage.success(`欢迎，${user.realName || user.username}（${user.roleLabel}）`)
    router.push('/dashboard')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1677ff 0%, #001529 100%);
}

.login-card {
  width: 400px;
  background: #fff;
  border-radius: 12px;
  padding: 32px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.3);
}

.login-card h2 {
  text-align: center;
  color: #303133;
}

.sub {
  text-align: center;
  color: #909399;
  font-size: 13px;
  margin: 6px 0 20px;
}

.demo-accounts .hint {
  color: #909399;
  font-size: 12px;
  margin-bottom: 4px;
}
</style>
