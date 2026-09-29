<template>
  <el-container class="layout">
    <el-aside width="220px" class="aside">
      <div class="logo">🛒 超市运营管理系统</div>
      <el-menu
        :default-active="route.path"
        router
        background-color="#001529"
        text-color="rgba(255,255,255,0.65)"
        active-text-color="#ffffff"
      >
        <template v-for="item in menuItems" :key="item.path">
          <el-menu-item v-if="!item.roles || hasRole(...item.roles)" :index="item.path">
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.label }}</span>
          </el-menu-item>
        </template>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <span class="header-title">{{ route.meta.title }}</span>
        <div style="flex: 1" />

        <!-- 消息中心 -->
        <el-badge :value="msgCount" :hidden="msgCount === 0" style="margin-right: 18px">
          <el-icon style="font-size: 18px; cursor: pointer" @click="msgVisible = true">
            <Bell />
          </el-icon>
        </el-badge>

        <el-dropdown>
          <span class="user-info">
            {{ auth.user?.realName || auth.user?.username }}（{{ auth.user?.roleLabel }}）
            <el-icon><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item disabled>{{ auth.user?.storeName }}</el-dropdown-item>
              <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>

    <!-- 消息中心抽屉 -->
    <el-drawer v-model="msgVisible" title="🔔 消息与预警中心" size="480px">
      <template v-if="msgSummary">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="库存预警">{{ msgSummary.inventoryAlerts }}</el-descriptions-item>
          <el-descriptions-item label="缺货商品">{{ msgSummary.outOfStock }}</el-descriptions-item>
          <el-descriptions-item label="临期(≤7天)">{{ msgSummary.expiringUrgent }}</el-descriptions-item>
          <el-descriptions-item label="已过期">{{ msgSummary.expired }}</el-descriptions-item>
        </el-descriptions>
        <el-button size="small" style="margin: 12px 0" @click="router.push('/inventory'); msgVisible = false">
          前往库存管理处理
        </el-button>
        <h4>临期 / 过期批次</h4>
        <el-table :data="expiringRows" border size="small" max-height="300">
          <el-table-column prop="productName" label="商品" min-width="110" />
          <el-table-column prop="quantity" label="库存" width="60" align="center" />
          <el-table-column label="距过期" width="80" align="center">
            <template #default="{ row }">{{ row.daysToExpire < 0 ? '已过期' : row.daysToExpire + '天' }}</template>
          </el-table-column>
          <el-table-column label="级别" width="80" align="center">
            <template #default="{ row }">
              <el-tag :type="row.daysToExpire < 0 ? 'danger' : row.daysToExpire <= 7 ? 'warning' : 'info'" size="small">
                {{ row.daysToExpire < 0 ? '过期' : row.daysToExpire <= 7 ? '紧急' : '临期' }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </el-drawer>
  </el-container>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Goods, Menu, OfficeBuilding, ShoppingCartFull, Box, Sell,
  User, Present, List, DataAnalysis, MagicStick, Bell, ArrowDown,
  Monitor, Odometer
} from '@element-plus/icons-vue'
import { auth, hasRole, logout } from '@/stores/auth'
import { messageSummary, messageExpiring } from '@/api/system'

const route = useRoute()
const router = useRouter()

const menuItems = [
  { path: '/dashboard', label: '经营驾驶舱', icon: Odometer, roles: null },
  { path: '/products', label: '商品与价格', icon: Goods, roles: null },
  { path: '/categories', label: '商品分类', icon: Menu, roles: null },
  { path: '/suppliers', label: '供应商管理', icon: OfficeBuilding, roles: null },
  { path: '/purchase', label: '采购管理', icon: ShoppingCartFull, roles: null },
  { path: '/inventory', label: '库存管理', icon: Box, roles: null },
  { path: '/sales', label: '销售与收银', icon: Sell, roles: null },
  { path: '/members', label: '会员管理', icon: User, roles: null },
  { path: '/promotions', label: '促销管理', icon: Present, roles: null },
  { path: '/stocktake', label: '盘点与损耗', icon: List, roles: null },
  { path: '/analytics', label: '经营分析', icon: DataAnalysis, roles: null },
  { path: '/agent', label: 'AI 智能中心', icon: MagicStick, roles: null },
  { path: '/system', label: '系统管理', icon: Monitor, roles: ['STORE_OWNER', 'STORE_MANAGER'] }
]

const msgVisible = ref(false)
const msgSummary = ref<Record<string, number> | null>(null)
const expiringRows = ref<any[]>([])
const msgCount = computed(() => {
  const s = msgSummary.value
  if (!s) return 0
  return (s.inventoryAlerts || 0) + (s.expiringUrgent || 0) + (s.expired || 0)
})

async function loadMessages() {
  try {
    msgSummary.value = await messageSummary()
    const exp = await messageExpiring()
    expiringRows.value = [...exp.expired, ...exp.urgent, ...exp.near]
  } catch {
    /* 忽略 */
  }
}

async function handleLogout() {
  logout()
  router.push('/login')
}

onMounted(() => {
  if (auth.user) loadMessages()
})
</script>

<style scoped>
.layout {
  height: 100%;
}

.aside {
  background-color: #001529;
}

.logo {
  height: 56px;
  line-height: 56px;
  text-align: center;
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.aside :deep(.el-menu) {
  border-right: none;
}

.aside :deep(.el-menu-item.is-active) {
  background-color: #1677ff;
}

.header {
  background: #fff;
  display: flex;
  align-items: center;
  border-bottom: 1px solid #e8e8e8;
}

.header-title {
  font-size: 16px;
  font-weight: 600;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  color: #303133;
  font-size: 14px;
}

.main {
  padding: 16px;
}
</style>
