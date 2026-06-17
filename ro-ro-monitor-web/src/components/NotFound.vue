<template>
  <div class="not-found">
    <el-result
      icon="warning"
      title="404"
      sub-title="页面不存在"
    >
      <template #extra>
        <el-button type="primary" @click="goHome">返回首页</el-button>
      </template>
    </el-result>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

function goHome() {
  if (authStore.isAuthenticated) {
    router.push('/dashboard/in-transit')
  } else {
    router.push('/login')
  }
}
</script>

<style scoped>
.not-found {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: #f0f2f5;
}
</style>
