<template>
  <div class="login-page">
    <div class="brand-panel">
      <div class="brand-inner">
        <div class="brand-logo">
          <div class="logo-mark">序</div>
          <div>
            <div class="logo-name">商序云</div>
            <div class="logo-sub">销售运营一体化 · 从线索到回款</div>
          </div>
        </div>

        <h1 class="headline">
          让销售团队<br />
          <span>把时间花在成交上</span>
        </h1>
        <p class="desc">
          线索、客户、商机、合同、订单、出库、应收，一条链路打通销售全流程。客户 360、销售阶段、业绩看板，随时掌握团队经营情况。
        </p>

        <div class="feature-grid">
          <div v-for="item in features" :key="item.title" class="feature">
            <div class="feature-icon">
              <el-icon :size="18"><component :is="item.icon" /></el-icon>
            </div>
            <div>
              <div class="feature-title">{{ item.title }}</div>
              <div class="feature-desc">{{ item.desc }}</div>
            </div>
          </div>
        </div>

        <div class="stats">
          <div class="stat">
            <div class="stat-num">360°</div>
            <div class="stat-label">客户视图</div>
          </div>
          <div class="stat">
            <div class="stat-num">7</div>
            <div class="stat-label">业务环节</div>
          </div>
          <div class="stat">
            <div class="stat-num">RBAC</div>
            <div class="stat-label">数据权限</div>
          </div>
        </div>
      </div>
      <div class="glow glow-1"></div>
      <div class="glow glow-2"></div>
    </div>

    <div class="form-panel">
      <div class="form-box">
        <div class="form-header">
          <h2>欢迎回来</h2>
          <p>登录 商序云，开启今天的销售工作</p>
        </div>

        <el-form ref="formRef" :model="form" :rules="rules" size="large" @keyup.enter="submit">
          <el-form-item prop="username">
            <el-input v-model="form.username" placeholder="请输入登录账号" clearable>
              <template #prefix>
                <el-icon><User /></el-icon>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item prop="password">
            <el-input v-model="form.password" type="password" placeholder="请输入登录密码" show-password>
              <template #prefix>
                <el-icon><Lock /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <div class="form-options">
            <el-checkbox v-model="remember">记住账号</el-checkbox>
            <span class="forget" @click="tip">忘记密码？</span>
          </div>

          <el-button type="primary" class="submit-btn" :loading="loading" @click="submit">
            登 录
          </el-button>
        </el-form>

        <div class="demo-block">
          <div class="demo-title">演示账号（密码均为 123456）</div>
          <div class="demo-list">
            <div v-for="acc in demoAccounts" :key="acc.username" class="demo-item" @click="fill(acc)">
              <span class="role">{{ acc.role }}</span>
              <span class="account">{{ acc.username }}</span>
            </div>
          </div>
        </div>

        <div class="copyright">© 2026 商序云 · 演示系统，数据仅供体验</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const formRef = ref()
const loading = ref(false)
const remember = ref(true)

const form = reactive({
  username: localStorage.getItem('shangxu_crm_last_user') || 'admin',
  password: '123456'
})

const rules = {
  username: [{ required: true, message: '请输入登录账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入登录密码', trigger: 'blur' }]
}

const features = [
  { icon: 'OfficeBuilding', title: '客户 360', desc: '客户、联系人、商机一屏掌握' },
  { icon: 'TrendCharts', title: '销售阶段', desc: '阶段推进与赢单率预测' },
  { icon: 'Document', title: '合同订单', desc: '合同审批到订单下达' },
  { icon: 'Van', title: '出库协同', desc: '出库单与物流跟踪' },
  { icon: 'Money', title: '应收回款', desc: '账龄分析与回款登记' },
  { icon: 'Lock', title: '数据权限', desc: '按角色隔离全部/部门/本人数据' }
]

const demoAccounts = [
  { role: '超级管理员', username: 'admin' },
  { role: '销售总监', username: 'manager01' },
  { role: '销售经理', username: 'manager02' },
  { role: '销售专员', username: 'sales01' }
]

function fill(acc) {
  form.username = acc.username
  form.password = '123456'
}

function tip() {
  ElMessage.info('演示环境请使用下方账号登录，密码统一为 123456')
}

async function submit() {
  await formRef.value.validate()
  loading.value = true
  try {
    await userStore.login({ ...form })
    if (remember.value) {
      localStorage.setItem('shangxu_crm_last_user', form.username)
    } else {
      localStorage.removeItem('shangxu_crm_last_user')
    }
    ElMessage.success(`欢迎回来，${userStore.nickname}`)
    const redirect = route.query.redirect ? decodeURIComponent(route.query.redirect) : '/dashboard'
    router.push(redirect)
  } catch (e) {
    // 错误提示已由拦截器处理
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  if (userStore.token) {
    router.replace('/dashboard')
  }
})
</script>

<style scoped>
.login-page {
  height: 100%;
  display: flex;
  background: #f7f9fc;
}

.brand-panel {
  flex: 1.15;
  position: relative;
  overflow: hidden;
  background: linear-gradient(140deg, #0f1b33 0%, #16294a 45%, #1d3a6b 100%);
  display: flex;
  align-items: center;
  padding: 64px 72px;
  color: #fff;
}

.brand-inner {
  position: relative;
  z-index: 2;
  max-width: 560px;
}

.brand-logo {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 48px;
}

.logo-mark {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  background: linear-gradient(135deg, #4d8cff, #2f6bff);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  font-weight: 800;
  box-shadow: 0 8px 24px rgba(47, 107, 255, 0.45);
}

.logo-name {
  font-size: 19px;
  font-weight: 700;
  letter-spacing: 1px;
}

.logo-sub {
  font-size: 12px;
  color: #93a7c4;
  margin-top: 2px;
}

.headline {
  font-size: 38px;
  line-height: 1.35;
  font-weight: 700;
  margin: 0 0 20px;
  letter-spacing: 1px;
}

.headline span {
  background: linear-gradient(90deg, #7aa9ff, #4d8cff);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.desc {
  font-size: 14px;
  line-height: 1.9;
  color: #a9bcd6;
  margin: 0 0 40px;
  max-width: 480px;
}

.feature-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 18px 28px;
}

.feature {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.feature-icon {
  width: 34px;
  height: 34px;
  border-radius: 9px;
  background: rgba(77, 140, 255, 0.16);
  border: 1px solid rgba(77, 140, 255, 0.3);
  color: #7aa9ff;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.feature-title {
  font-size: 13.5px;
  font-weight: 600;
  margin-bottom: 3px;
}

.feature-desc {
  font-size: 12px;
  color: #8fa3bf;
  line-height: 1.5;
}

.stats {
  display: flex;
  gap: 56px;
  margin-top: 52px;
  padding-top: 28px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}

.stat-num {
  font-size: 22px;
  font-weight: 700;
  color: #7aa9ff;
}

.stat-label {
  font-size: 12px;
  color: #8fa3bf;
  margin-top: 4px;
}

.glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.5;
}

.glow-1 {
  width: 420px;
  height: 420px;
  background: rgba(47, 107, 255, 0.45);
  top: -120px;
  right: -80px;
}

.glow-2 {
  width: 320px;
  height: 320px;
  background: rgba(0, 200, 255, 0.18);
  bottom: -120px;
  left: -60px;
}

.form-panel {
  width: 520px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
  padding: 40px;
}

.form-box {
  width: 100%;
  max-width: 360px;
}

.form-header {
  margin-bottom: 32px;
}

.form-header h2 {
  font-size: 26px;
  margin: 0 0 8px;
  color: #16233b;
}

.form-header p {
  margin: 0;
  color: #8b97a8;
  font-size: 13px;
}

.form-options {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: -4px 0 20px;
}

.forget {
  font-size: 12.5px;
  color: var(--el-color-primary);
  cursor: pointer;
}

.submit-btn {
  width: 100%;
  height: 44px;
  font-size: 15px;
  letter-spacing: 4px;
  border-radius: 8px;
  background: linear-gradient(90deg, #2f6bff, #4d8cff);
  border: none;
}

.demo-block {
  margin-top: 30px;
  padding-top: 22px;
  border-top: 1px dashed #e6ebf3;
}

.demo-title {
  font-size: 12px;
  color: #98a3b3;
  margin-bottom: 12px;
}

.demo-list {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
}

.demo-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 10px;
  border: 1px solid #eef1f6;
  border-radius: 7px;
  cursor: pointer;
  transition: all 0.15s;
  background: #fafbfd;
}

.demo-item:hover {
  border-color: #b9cdff;
  background: #f3f7ff;
}

.demo-item .role {
  font-size: 12px;
  color: #5b6779;
}

.demo-item .account {
  font-size: 11.5px;
  color: #9aa5b5;
  font-family: Consolas, monospace;
}

.copyright {
  margin-top: 28px;
  text-align: center;
  font-size: 11.5px;
  color: #b3bcc9;
}

@media (max-width: 1024px) {
  .brand-panel {
    display: none;
  }
  .form-panel {
    width: 100%;
  }
}
</style>
