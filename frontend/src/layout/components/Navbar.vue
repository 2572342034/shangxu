<template>
  <header class="navbar">
    <div class="nav-left">
      <el-icon class="collapse-btn" @click="appStore.toggleSidebar()">
        <Fold v-if="!appStore.sidebarCollapsed" />
        <Expand v-else />
      </el-icon>

      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item v-if="parentTitle && parentTitle !== currentTitle">{{ parentTitle }}</el-breadcrumb-item>
        <el-breadcrumb-item>{{ currentTitle }}</el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <div class="nav-right">
      <el-dropdown trigger="click" @command="quickCreate">
        <el-button type="primary" class="create-btn">
          <el-icon><Plus /></el-icon>
          <span>快速新建</span>
          <el-icon class="arrow"><ArrowDown /></el-icon>
        </el-button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="customer" icon="OfficeBuilding">新建客户</el-dropdown-item>
            <el-dropdown-item command="lead" icon="Aim">新建线索</el-dropdown-item>
            <el-dropdown-item command="opportunity" icon="TrendCharts">新建商机</el-dropdown-item>
            <el-dropdown-item command="contract" icon="Document">新建合同</el-dropdown-item>
            <el-dropdown-item command="order" icon="List">新建订单</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>

      <el-popover v-model:visible="noticeVisible" placement="bottom-end" :width="360" trigger="click">
        <template #reference>
          <div class="icon-btn">
            <el-badge :value="unread" :hidden="unread === 0" :max="99">
              <el-icon :size="18"><Bell /></el-icon>
            </el-badge>
          </div>
        </template>
        <div class="notice-panel">
          <div class="notice-head">
            <span class="notice-title">消息通知</span>
            <el-button v-if="unread > 0" link type="primary" size="small" @click="markAll">全部已读</el-button>
          </div>
          <div v-if="!notices.length" class="notice-empty">暂无未读消息</div>
          <div v-else class="notice-list">
            <div v-for="item in notices" :key="item.id" class="notice-item" @click="openNotice(item)">
              <div class="notice-dot" :class="item.type.toLowerCase()"></div>
              <div class="notice-main">
                <div class="notice-item-title" :class="{ unread: !item.readFlag }">{{ item.title }}</div>
                <div class="notice-item-content">{{ item.content }}</div>
                <div class="notice-item-time">{{ fromNow(item.createTime) }}</div>
              </div>
            </div>
          </div>
        </div>
      </el-popover>

      <el-dropdown trigger="click" @command="handleCommand">
        <div class="user-box">
          <el-avatar :size="30" class="avatar">{{ avatarChar(userStore.nickname) }}</el-avatar>
          <div class="user-meta">
            <span class="nick">{{ userStore.nickname }}</span>
            <span class="dept">{{ userStore.deptName || userStore.roleNames.join(' / ') }}</span>
          </div>
          <el-icon class="arrow"><ArrowDown /></el-icon>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="profile">个人资料</el-dropdown-item>
            <el-dropdown-item command="password">修改密码</el-dropdown-item>
            <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>

    <el-dialog v-model="profileVisible" title="个人资料" width="440px">
      <el-form :model="profileForm" label-width="80px">
        <el-form-item label="姓名">
          <el-input v-model="profileForm.nickname" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="profileForm.mobile" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="profileForm.email" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="profileVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveProfile">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="passwordVisible" title="修改密码" width="420px">
      <el-form ref="pwdRef" :model="pwdForm" :rules="pwdRules" label-width="90px">
        <el-form-item label="原密码" prop="oldPassword">
          <el-input v-model="pwdForm.oldPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="pwdForm.newPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="pwdForm.confirmPassword" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="savePassword">确认修改</el-button>
      </template>
    </el-dialog>
  </header>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { menuRoutes } from '@/router'
import { useAppStore } from '@/store/app'
import { useUserStore } from '@/store/user'
import { updateProfile, changePassword } from '@/api/auth'
import { getTodo } from '@/api/dashboard'
import { getUnreadCount, listNotices, readAllNotices, readNotice } from '@/api/notice'
import { avatarChar, fromNow } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()
const userStore = useUserStore()

const currentTitle = computed(() => route.meta?.title || '工作台')
const parentTitle = computed(() => {
  const group = menuRoutes.find((item) => item.children?.some((child) => child.path === route.path))
  return group?.meta?.title || ''
})

const todoCount = ref(0)
const unread = ref(0)
const notices = ref([])
const noticeVisible = ref(false)
const profileVisible = ref(false)
const passwordVisible = ref(false)
const saving = ref(false)
const pwdRef = ref()

const profileForm = reactive({ nickname: '', mobile: '', email: '' })
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于 6 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== pwdForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

async function loadTodo() {
  try {
    const { data } = await getTodo()
    todoCount.value =
      (data.followCustomers?.length || 0) +
      (data.pendingDeliveries?.length || 0) +
      (data.pendingReceivables?.length || 0) +
      (data.pendingContracts?.length || 0)
  } catch (e) {
    todoCount.value = 0
  }
}

function quickCreate(type) {
  const map = {
    customer: '/customers',
    lead: '/leads',
    opportunity: '/opportunities',
    contract: '/contracts',
    order: '/orders'
  }
  router.push({ path: map[type], query: { create: Date.now() } })
}

async function loadNotices() {
  try {
    const [countRes, listRes] = await Promise.all([getUnreadCount(), listNotices({ onlyUnread: true, limit: 10 })])
    unread.value = countRes.data || 0
    notices.value = listRes.data || []
  } catch (e) {
    unread.value = 0
  }
}

async function markAll() {
  await readAllNotices()
  await loadNotices()
}

async function openNotice(item) {
  if (!item.readFlag) {
    await readNotice(item.id)
  }
  noticeVisible.value = false
  const routeMap = {
    RECEIVABLE: '/receivables',
    FOLLOWUP: '/customers',
    POOL: '/customers',
    APPROVAL: '/contracts'
  }
  const target = routeMap[item.type]
  if (target) {
    router.push(target)
  }
  await loadNotices()
}

function handleCommand(command) {
  if (command === 'logout') {
    ElMessageBox.confirm('确定要退出当前账号吗？', '提示', { type: 'warning' }).then(async () => {
      await userStore.logout()
      router.push('/login')
    })
    return
  }
  if (command === 'profile') {
    profileForm.nickname = userStore.user?.nickname || ''
    profileForm.mobile = userStore.user?.mobile || ''
    profileForm.email = userStore.user?.email || ''
    profileVisible.value = true
    return
  }
  if (command === 'password') {
    pwdForm.oldPassword = ''
    pwdForm.newPassword = ''
    pwdForm.confirmPassword = ''
    passwordVisible.value = true
  }
}

async function saveProfile() {
  saving.value = true
  try {
    await updateProfile({ ...profileForm })
    await userStore.fetchInfo()
    ElMessage.success('资料已更新')
    profileVisible.value = false
  } finally {
    saving.value = false
  }
}

async function savePassword() {
  await pwdRef.value.validate()
  saving.value = true
  try {
    await changePassword({ oldPassword: pwdForm.oldPassword, newPassword: pwdForm.newPassword })
    ElMessage.success('密码修改成功，请重新登录')
    passwordVisible.value = false
    userStore.reset()
    router.push('/login')
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  loadTodo()
  loadNotices()
})
</script>

<style scoped>
.navbar {
  height: 60px;
  background: #fff;
  border-bottom: 1px solid #edf0f5;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  flex-shrink: 0;
}

.nav-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.collapse-btn {
  font-size: 18px;
  color: #5b6779;
  cursor: pointer;
}

.collapse-btn:hover {
  color: var(--el-color-primary);
}

.nav-right {
  display: flex;
  align-items: center;
  gap: 14px;
}

.create-btn {
  border-radius: 8px;
  padding: 8px 14px;
}

.create-btn .arrow {
  margin-left: 2px;
  font-size: 12px;
}

.icon-btn {
  width: 34px;
  height: 34px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #5b6779;
  cursor: pointer;
  background: #f6f8fb;
}

.icon-btn:hover {
  background: #eef3ff;
  color: var(--el-color-primary);
}

.user-box {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 8px;
}

.user-box:hover {
  background: #f6f8fb;
}

.avatar {
  background: linear-gradient(135deg, #4d8cff, #2f6bff);
  color: #fff;
  font-size: 12px;
  font-weight: 600;
}

.user-meta {
  display: flex;
  flex-direction: column;
  line-height: 1.3;
}

.nick {
  font-size: 13px;
  font-weight: 600;
  color: #1f2733;
}

.dept {
  font-size: 11px;
  color: #8b97a8;
}

.arrow {
  color: #98a3b3;
  font-size: 12px;
}

.notice-panel {
  margin: -4px -6px;
}
.notice-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 2px 4px 8px;
  border-bottom: 1px solid #f0f3f8;
}
.notice-title {
  font-size: 13.5px;
  font-weight: 500;
  color: #2c3648;
}
.notice-empty {
  padding: 26px 0;
  text-align: center;
  font-size: 12.5px;
  color: #9aa5b6;
}
.notice-list {
  max-height: 360px;
  overflow-y: auto;
}
.notice-item {
  display: flex;
  gap: 8px;
  padding: 10px 6px;
  border-bottom: 1px solid #f5f7fa;
  cursor: pointer;
}
.notice-item:hover {
  background: #f8faff;
}
.notice-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  margin-top: 6px;
  flex-shrink: 0;
  background: #8fa6c8;
}
.notice-dot.receivable { background: #e2725b; }
.notice-dot.followup { background: #2f6bff; }
.notice-dot.pool { background: #e08a1e; }
.notice-dot.approval { background: #7b61c9; }
.notice-main {
  min-width: 0;
}
.notice-item-title {
  font-size: 12.8px;
  color: #5b6779;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.notice-item-title.unread {
  color: #1f2a3d;
  font-weight: 500;
}
.notice-item-content {
  font-size: 11.8px;
  color: #8b97a8;
  margin-top: 2px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.notice-item-time {
  font-size: 11px;
  color: #b0bac9;
  margin-top: 3px;
}
</style>
