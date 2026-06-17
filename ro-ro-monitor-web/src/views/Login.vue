<template>
  <div class="login-page">
    <div class="login-container">
      <el-card class="login-card" shadow="never">
        <div class="login-header">
          <div class="login-icon-wrap">
            <el-icon :size="28"><Monitor /></el-icon>
          </div>
          <h1 class="login-title">在途车辆监控系统</h1>
          <p class="login-subtitle">安吉远海 · 内部管理系统</p>
        </div>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          class="login-form"
          @keyup.enter="handleLogin"
        >
          <el-form-item prop="username">
            <el-input
              v-model="form.username"
              placeholder="请输入用户名"
              :prefix-icon="User"
              size="large"
            />
          </el-form-item>

          <el-form-item prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="请输入密码"
              :prefix-icon="Lock"
              size="large"
            />
          </el-form-item>

          <el-form-item>
            <el-button
              type="primary"
              size="large"
              class="login-button"
              :loading="loading"
              @click="handleLogin"
            >
              {{ loading ? '登录中...' : '登 录' }}
            </el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 4, message: '密码至少需要4位字符', trigger: 'blur' }
  ]
}

async function handleLogin() {
  if (!formRef.value) return

  try {
    await formRef.value.validate()
  } catch {
    return
  }

  loading.value = true
  try {
    await authStore.login(form.username, form.password)
    ElMessage.success('登录成功')
    router.push('/dashboard')
  } catch (error) {
    ElMessage.error(error.response?.data?.message || error.message || '登录失败，请检查用户名和密码')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #0f1929 0%, #1a2d4a 50%, #1d4e8f 100%);
  position: relative;
  overflow: hidden;
}

/* Decorative glow — top right */
.login-page::before {
  content: '';
  position: absolute;
  width: 500px;
  height: 500px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(29, 114, 243, 0.18), transparent 70%);
  top: -160px;
  right: -160px;
  pointer-events: none;
}

/* Decorative glow — bottom left */
.login-page::after {
  content: '';
  position: absolute;
  width: 360px;
  height: 360px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.04), transparent 70%);
  bottom: -100px;
  left: -100px;
  pointer-events: none;
}

.login-container {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 420px;
  padding: 24px;
}

.login-card {
  padding: 40px 32px 32px;
  border-radius: var(--radius-xl);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(246, 250, 255, 0.98));
  border: 1px solid rgba(255, 255, 255, 0.12);
  box-shadow: 0 24px 80px rgba(0, 0, 0, 0.2);
}

.login-header {
  text-align: center;
  margin-bottom: 36px;
}

.login-icon-wrap {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  border-radius: 16px;
  background: linear-gradient(135deg, var(--color-primary), var(--color-primary-dark));
  margin-bottom: 20px;
  box-shadow: 0 8px 24px rgba(29, 114, 243, 0.25);
}

.login-icon-wrap .el-icon {
  color: #fff;
}

.login-title {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: 1px;
}

.login-subtitle {
  margin: 8px 0 0;
  font-size: 13px;
  color: var(--text-muted);
  letter-spacing: 2px;
}

.login-form {
  margin-top: 4px;
}

.login-form :deep(.el-input__wrapper) {
  background: #f5f7fa;
  box-shadow: none;
  border: 1px solid transparent;
  transition: all 0.2s ease;
}

.login-form :deep(.el-input__wrapper:hover) {
  border-color: var(--color-primary-lightest);
}

.login-form :deep(.el-input__wrapper.is-focus) {
  border-color: var(--color-primary);
  background: #fff;
  box-shadow: 0 0 0 3px rgba(29, 114, 243, 0.1);
}

.login-form :deep(.el-input__inner) {
  height: 44px;
}

.login-button {
  width: 100%;
  height: 44px;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 3px;
  border-radius: var(--radius-sm);
  margin-top: 4px;
  transition: all 0.2s ease;
}

.login-button:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(29, 114, 243, 0.35);
}
</style>
