<template>
  <aside class="sidebar" :class="{ collapsed }">
    <div class="logo">
      <div class="logo-mark">序</div>
      <transition name="fade">
        <div v-if="!collapsed" class="logo-text">
          <span class="name">商序云</span>
          <span class="sub">智能销售云</span>
        </div>
      </transition>
    </div>

    <el-scrollbar class="menu-scroll">
      <el-menu
        :default-active="activeMenu"
        :collapse="collapsed"
        :collapse-transition="false"
        router
        unique-opened
        background-color="transparent"
        text-color="#9fb0c9"
        active-text-color="#ffffff"
      >
        <template v-for="item in menus" :key="item.path">
          <el-sub-menu v-if="item.children" :index="item.path">
            <template #title>
              <el-icon><component :is="item.meta.icon" /></el-icon>
              <span>{{ item.meta.title }}</span>
            </template>
            <el-menu-item v-for="child in item.children" :key="child.path" :index="child.path">
              <el-icon><component :is="child.meta.icon" /></el-icon>
              <template #title>{{ child.meta.title }}</template>
            </el-menu-item>
          </el-sub-menu>
          <el-menu-item v-else :index="item.path">
            <el-icon><component :is="item.meta.icon" /></el-icon>
            <template #title>{{ item.meta.title }}</template>
          </el-menu-item>
        </template>
      </el-menu>
    </el-scrollbar>

    <div class="sidebar-footer">
      <div v-if="!collapsed" class="footer-card">
        <div class="footer-title">本月目标</div>
        <div class="footer-desc">保持跟进，冲刺业绩</div>
        <el-progress :percentage="68" :stroke-width="6" color="#4d8cff" :show-text="false" />
      </div>
    </div>
  </aside>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { menuRoutes } from '@/router'
import { useUserStore } from '@/store/user'
import { useAppStore } from '@/store/app'

const route = useRoute()
const userStore = useUserStore()
const appStore = useAppStore()

const collapsed = computed(() => appStore.sidebarCollapsed)
const activeMenu = computed(() => route.path)

const menus = computed(() =>
  menuRoutes
    .map((item) => {
      if (item.children) {
        const children = item.children.filter(
          (child) => !child.meta?.permission || userStore.hasPerm(child.meta.permission)
        )
        return children.length ? { ...item, children } : null
      }
      return !item.meta?.permission || userStore.hasPerm(item.meta.permission) ? item : null
    })
    .filter(Boolean)
)
</script>

<style scoped>
.sidebar {
  width: var(--crm-sidebar-width);
  height: 100%;
  background: linear-gradient(180deg, var(--crm-sidebar-bg) 0%, var(--crm-sidebar-bg-2) 100%);
  display: flex;
  flex-direction: column;
  transition: width 0.2s ease;
  flex-shrink: 0;
  overflow: hidden;
}

.sidebar.collapsed {
  width: var(--crm-sidebar-collapsed);
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 16px;
  flex-shrink: 0;
}

.logo-mark {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: linear-gradient(135deg, #4d8cff, #2f6bff);
  color: #fff;
  font-weight: 800;
  font-size: 17px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(47, 107, 255, 0.4);
  flex-shrink: 0;
}

.logo-text {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
  white-space: nowrap;
}

.logo-text .name {
  color: #fff;
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.logo-text .sub {
  color: #7d8ea8;
  font-size: 11px;
  margin-top: 2px;
}

.menu-scroll {
  flex: 1;
  padding: 6px 8px;
}

:deep(.el-menu) {
  border-right: none;
}

:deep(.el-menu-item),
:deep(.el-sub-menu__title) {
  height: 42px;
  line-height: 42px;
  border-radius: 8px;
  margin-bottom: 4px;
  font-size: 13.5px;
}

:deep(.el-menu-item:hover),
:deep(.el-sub-menu__title:hover) {
  background: rgba(255, 255, 255, 0.06) !important;
}

:deep(.el-menu-item.is-active) {
  background: linear-gradient(90deg, rgba(47, 107, 255, 0.95), rgba(77, 140, 255, 0.75)) !important;
  box-shadow: 0 4px 12px rgba(47, 107, 255, 0.35);
  font-weight: 500;
}

:deep(.el-sub-menu .el-menu-item) {
  height: 38px;
  line-height: 38px;
  padding-left: 46px !important;
  font-size: 13px;
}

.sidebar-footer {
  padding: 12px;
  flex-shrink: 0;
}

.footer-card {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.07);
  border-radius: 10px;
  padding: 12px;
}

.footer-title {
  color: #e7edf7;
  font-size: 12.5px;
  font-weight: 600;
}

.footer-desc {
  color: #7d8ea8;
  font-size: 11px;
  margin: 4px 0 10px;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.15s;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
