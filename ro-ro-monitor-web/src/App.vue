<template>
  <div id="app">
    <!-- Mobile drawer backdrop -->
    <transition name="backdrop-fade">
      <div
        v-if="isMobile && !isCollapse"
        class="sidebar-backdrop"
        @click="closeDrawer"
      />
    </transition>

    <el-container class="layout-container">
      <!-- Sidebar -->
      <el-aside
        :width="asideWidth"
        class="aside"
        :class="asideClasses"
      >
        <div class="logo">
          <span class="logo-text" :class="{ 'hide-text': !isMobile && isCollapse }">安吉远海在途监控系统</span>
        </div>
        <el-menu
          :default-active="$route.path"
          router
          :collapse="!isMobile && isCollapse"
          text-color="#bfcbd9"
          active-text-color="#409eff"
        >
          <el-sub-menu index="monitor">
            <template #title>
              <el-icon><DataAnalysis /></el-icon>
              <span>车辆监控</span>
            </template>
            <el-menu-item index="/dashboard/in-transit">
              <el-icon><TrendCharts /></el-icon>
              <span>在途车辆</span>
            </el-menu-item>
            <el-menu-item index="/dashboard/arrived">
              <el-icon><CircleCheck /></el-icon>
              <span>到达车辆</span>
            </el-menu-item>
            <el-menu-item index="/dashboard/history-replay">
              <el-icon><VideoPlay /></el-icon>
              <span>历史回放</span>
            </el-menu-item>
          </el-sub-menu>
          <!-- 数据管理子菜单 -->
          <el-sub-menu index="data-mgmt">
            <template #title>
              <el-icon><FolderOpened /></el-icon>
              <span>数据管理</span>
            </template>
            <el-menu-item index="/upload">
              <el-icon><Upload /></el-icon>
              <span>文件上传</span>
            </el-menu-item>
            <el-menu-item index="/order-manage">
              <el-icon><List /></el-icon>
              <span>订单管理</span>
            </el-menu-item>
            <el-menu-item index="/excel-mapping">
              <el-icon><Tools /></el-icon>
              <span>Excel映射配置</span>
            </el-menu-item>
          </el-sub-menu>

          <!-- 基础信息维护子菜单 -->
          <el-sub-menu index="base">
            <template #title>
              <el-icon><Setting /></el-icon>
              <span>基础信息维护</span>
            </template>
            <el-menu-item index="/brand">
              <el-icon><Coin /></el-icon>
              <span>品牌管理</span>
            </el-menu-item>
            <el-menu-item index="/port">
              <el-icon><Location /></el-icon>
              <span>港口管理</span>
            </el-menu-item>
            <el-menu-item index="/route">
              <el-icon><Connection /></el-icon>
              <span>线路管理</span>
            </el-menu-item>
            <el-menu-item index="/otd-config">
              <el-icon><Timer /></el-icon>
              <span>OTD时效配置</span>
            </el-menu-item>
            <el-menu-item index="/location-alias">
              <el-icon><Connection /></el-icon>
              <span>地点别名</span>
            </el-menu-item>
          </el-sub-menu>
        </el-menu>
      </el-aside>

      <!-- Main area -->
      <el-container>
        <el-header class="header">
          <div class="header-left">
            <el-icon class="hamburger" :size="20" @click="toggleCollapse">
              <Fold v-if="!isCollapse" />
              <Expand v-else />
            </el-icon>
            <el-breadcrumb separator="/">
              <el-breadcrumb-item :to="{ path: '/dashboard/in-transit' }">首页</el-breadcrumb-item>
              <el-breadcrumb-item v-if="$route.meta.title">{{ $route.meta.title }}</el-breadcrumb-item>
            </el-breadcrumb>
          </div>
          <div class="header-right">
            <!-- reserved for right-aligned content -->
          </div>
        </el-header>
        <el-main class="main">
          <router-view v-slot="{ Component }">
            <transition name="fade" mode="out-in">
              <component :is="Component" />
            </transition>
          </router-view>
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const isCollapse = ref(false)
const isMobile = ref(false)

const checkMobile = () => {
  isMobile.value = window.innerWidth <= 768
  if (isMobile.value) {
    isCollapse.value = true
  }
}

const asideWidth = computed(() => {
  if (isMobile.value) return '200px'
  return isCollapse.value ? '64px' : '200px'
})

const asideClasses = computed(() => ({
  'is-collapsed': !isMobile.value && isCollapse.value,
  'is-drawer': isMobile.value,
  'drawer-open': isMobile.value && !isCollapse.value
}))

const toggleCollapse = () => {
  isCollapse.value = !isCollapse.value
}

const closeDrawer = () => {
  isCollapse.value = true
}

// Close drawer on navigation (mobile)
watch(() => route.path, () => {
  if (isMobile.value) {
    isCollapse.value = true
  }
})

onMounted(() => {
  checkMobile()
  window.addEventListener('resize', checkMobile)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', checkMobile)
})
</script>

<style scoped>
.layout-container {
  height: 100vh;
}

/* ========== Sidebar ========== */
.aside {
  background-color: var(--sidebar-bg);
  overflow: hidden;
}

.aside:not(.is-drawer) {
  transition: width var(--transition-normal);
}

/* Desktop collapsed 64px */
.aside.is-collapsed {
  width: 64px !important;
}

/* Mobile drawer */
.aside.is-drawer {
  position: fixed;
  top: 0;
  left: 0;
  bottom: 0;
  z-index: 2000;
  transform: translateX(-100%);
  transition: transform var(--transition-normal);
}

.aside.is-drawer.drawer-open {
  transform: translateX(0);
}

/* ========== Logo ========== */
.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  background-color: #2b3a4a;
  overflow: hidden;
  white-space: nowrap;
}

.logo-text {
  color: #fff;
  font-size: 16px;
  font-weight: bold;
  transition: opacity var(--transition-fast);
}

.logo-text.hide-text {
  opacity: 0;
  width: 0;
}

/* ========== el-menu background via CSS variable ========== */
:deep(.aside .el-menu) {
  background-color: var(--sidebar-bg) !important;
  border-right: none;
}

:deep(.aside .el-menu--popup) {
  background-color: var(--sidebar-bg) !important;
}

/* ========== Header ========== */
.header {
  background-color: var(--header-bg);
  border-bottom: 1px solid var(--header-border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  height: 60px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.hamburger {
  cursor: pointer;
  color: var(--text-secondary);
  transition: color var(--transition-fast);
  flex-shrink: 0;
}

.hamburger:hover {
  color: var(--color-primary);
}

/* ========== Main ========== */
.main {
  background-color: var(--page-bg);
  padding: 20px;
}

/* ========== Backdrop ========== */
.sidebar-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  z-index: 1999;
}

/* ========== Transitions ========== */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.backdrop-fade-enter-active,
.backdrop-fade-leave-active {
  transition: opacity 0.3s ease;
}
.backdrop-fade-enter-from,
.backdrop-fade-leave-to {
  opacity: 0;
}

/* ========== Breadcrumb ========== */
:deep(.el-breadcrumb__inner) {
  color: var(--text-secondary) !important;
  font-weight: 400 !important;
}

:deep(.el-breadcrumb__inner.is-link) {
  color: var(--color-primary) !important;
  font-weight: 500 !important;
}

:deep(.el-breadcrumb__separator) {
  color: var(--text-muted) !important;
}
</style>
