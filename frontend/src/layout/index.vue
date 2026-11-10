<template>
  <div class="layout">
    <Sidebar />
    <div class="main">
      <Navbar />
      <main class="app-main">
        <router-view v-slot="{ Component, route: current }">
          <transition name="fade-slide" mode="out-in">
            <component :is="Component" :key="current.path" />
          </transition>
        </router-view>
      </main>
    </div>
  </div>
</template>

<script setup>
import Sidebar from './components/Sidebar.vue'
import Navbar from './components/Navbar.vue'
</script>

<style scoped>
.layout {
  display: flex;
  height: 100%;
  overflow: hidden;
}

.main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  background: var(--crm-bg);
}

.app-main {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
}

.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: all 0.18s ease;
}

.fade-slide-enter-from {
  opacity: 0;
  transform: translateY(6px);
}

.fade-slide-leave-to {
  opacity: 0;
}
</style>
