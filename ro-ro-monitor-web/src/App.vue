<template>
  <div id="app">
    <el-container class="layout-container">
      <el-aside width="200px" class="aside">
        <div class="logo">安吉远海在途监控系统</div>
        <el-menu
          :default-active="$route.path"
          router
          background-color="#304156"
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
          </el-sub-menu>
          <el-menu-item index="/upload">
            <el-icon><Upload /></el-icon>
            <span>文件上传</span>
          </el-menu-item>
          <el-menu-item index="/excel-mapping">
            <el-icon><Tools /></el-icon>
            <span>Excel映射配置</span>
          </el-menu-item>
          
          <!-- 基础信息维护子菜单 -->
          <el-sub-menu index="base">
            <template #title>
              <el-icon><Setting /></el-icon>
              <span>基础信息维护</span>
            </template>
            <el-menu-item index="/brand">品牌管理</el-menu-item>
            <el-menu-item index="/port">港口管理</el-menu-item>
            <el-menu-item index="/route">线路管理</el-menu-item>
            <el-menu-item index="/otd-config">OTD时效配置</el-menu-item>
          </el-sub-menu>
        </el-menu>
      </el-aside>
      <el-container>
        <el-header class="header">
          <span>{{ $route.meta.title }}</span>
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
// 这里就是 script setup 区域
// 需要在这里引入所有用到的图标组件
import { DataAnalysis, Upload, Tools, Setting, TrendCharts, CircleCheck } from '@element-plus/icons-vue'
</script>

<style scoped>
.layout-container {
  height: 100vh;
}
.aside {
  background-color: #304156;
}
.logo {
  height: 60px;
  line-height: 60px;
  text-align: center;
  color: #fff;
  font-size: 18px;
  font-weight: bold;
  background-color: #2b3a4a;
}
.header {
  background-color: #fff;
  border-bottom: 1px solid #e6e6e6;
  display: flex;
  align-items: center;
  font-size: 18px;
  font-weight: 500;
}
.main {
  background-color: #f0f2f5;
  padding: 20px;
}
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>