<template>
  <div class="login-page">
    <!-- Engineering blueprint grid -->
    <div class="tech-grid" aria-hidden="true"></div>

    <!-- Diagonal geometric accent -->
    <div class="geo-accent" aria-hidden="true"></div>

    <!-- Soft cyan glow top-right -->
    <div class="ambient-glow" aria-hidden="true"></div>

    <!-- Scan line — signature "precision instrument" moment -->
    <div class="scan-line" aria-hidden="true"></div>

    <div class="login-container">
      <el-card class="login-card" shadow="never">
        <!-- Scan line inside card as well -->
        <div class="card-scan-line" aria-hidden="true"></div>

        <div class="login-header">
          <div class="login-icon-wrap" style="animation-delay: 0.1s">
            <el-icon :size="32"><Monitor /></el-icon>
          </div>
          <h1 class="login-title" style="animation-delay: 0.2s">在途车辆监控系统</h1>
          <p class="login-subtitle" style="animation-delay: 0.3s">安吉远海 · 内部管理系统</p>
        </div>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          class="login-form"
          @keyup.enter="handleLogin"
        >
          <el-form-item prop="username" style="animation-delay: 0.4s">
            <el-input
              v-model="form.username"
              placeholder="请输入用户名"
              :prefix-icon="User"
              size="large"
            />
          </el-form-item>

          <el-form-item prop="password" style="animation-delay: 0.5s">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="请输入密码"
              :prefix-icon="Lock"
              size="large"
            />
          </el-form-item>

          <el-form-item style="animation-delay: 0.6s">
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
/* ===== Google Fonts ===== */
@import url('https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;500;600;700&family=JetBrains+Mono:wght@400;500&display=swap');

/* ===== Keyframes ===== */
@keyframes fade-slide-up {
  0% {
    opacity: 0;
    transform: translateY(16px);
    filter: blur(6px);
  }
  100% {
    opacity: 1;
    transform: translateY(0);
    filter: blur(0);
  }
}

@keyframes card-entrance {
  0% {
    opacity: 0;
    transform: translateY(20px) scale(0.97);
    filter: blur(8px);
  }
  100% {
    opacity: 1;
    transform: translateY(0) scale(1);
    filter: blur(0);
  }
}

@keyframes scan-line-sweep {
  0% {
    top: 0;
    opacity: 0;
  }
  5% {
    opacity: 1;
  }
  95% {
    opacity: 1;
  }
  100% {
    top: 100%;
    opacity: 0;
  }
}

@keyframes badge-pulse {
  0%, 100% { opacity: 0.7; }
  50% { opacity: 1; }
}

/* ===== Page Container ===== */
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: url('/login.jpg') center/cover no-repeat;
  position: relative;
  overflow: hidden;
}

/* ===== Background Overlay ===== */
.login-page::before {
  content: '';
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.35);
  z-index: 0;
  pointer-events: none;
}

/* ===== Engineering Blueprint Grid ===== */
.tech-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(0, 102, 255, 0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(0, 102, 255, 0.03) 1px, transparent 1px);
  background-size: 80px 80px;
  pointer-events: none;
  z-index: 0;
}

/* ===== Diagonal Geometric Accent ===== */
.geo-accent {
  position: absolute;
  inset: 0;
  pointer-events: none;
  z-index: 0;
  background:
    linear-gradient(
      45deg,
      transparent 48%,
      rgba(0, 180, 216, 0.06) 48%,
      rgba(0, 180, 216, 0.06) 50%,
      transparent 50%
    ),
    linear-gradient(
      -45deg,
      transparent 48%,
      rgba(0, 180, 216, 0.04) 48%,
      rgba(0, 180, 216, 0.04) 50%,
      transparent 50%
    );
}

/* ===== Ambient Cyan Glow ===== */
.ambient-glow {
  position: absolute;
  width: 600px;
  height: 600px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(0, 180, 216, 0.08) 0%, transparent 65%);
  top: -200px;
  right: -150px;
  pointer-events: none;
  z-index: 0;
}

/* ===== Scan Line (page-level) ===== */
.scan-line {
  position: absolute;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(0, 102, 255, 0.15), transparent);
  pointer-events: none;
  z-index: 3;
  animation: scan-line-sweep 1.8s cubic-bezier(0.4, 0, 0.2, 1) forwards;
}

/* ===== Card Container ===== */
.login-container {
  position: relative;
  z-index: 2;
  width: 100%;
  max-width: 420px;
  padding: 24px;
  animation: card-entrance 0.8s cubic-bezier(0.22, 1, 0.36, 1) both;
}

/* ===== Frosted Glass Card ===== */
.login-card {
  padding: 44px 36px 36px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.75);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(0, 102, 255, 0.08);
  box-shadow: 0 8px 40px rgba(0, 60, 130, 0.08);
  position: relative;
  overflow: hidden;
}

/* Card scan line */
.card-scan-line {
  position: absolute;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(0, 102, 255, 0.12), transparent);
  pointer-events: none;
  z-index: 1;
  animation: scan-line-sweep 1.8s cubic-bezier(0.4, 0, 0.2, 1) forwards;
  animation-delay: 0.1s;
}

/* Disable global hover transform */
.login-card:hover {
  transform: none !important;
  box-shadow: 0 8px 40px rgba(0, 60, 130, 0.08) !important;
}

/* Override card body padding */
.login-card :deep(.el-card__body) {
  padding: 0;
}

/* ===== Header ===== */
.login-header {
  text-align: center;
  margin-bottom: 36px;
}

/* Icon container */
.login-icon-wrap {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 64px;
  height: 64px;
  border-radius: 16px;
  background: linear-gradient(135deg, rgba(0, 102, 255, 0.12), rgba(0, 180, 216, 0.08));
  border: 1px solid rgba(0, 102, 255, 0.15);
  margin-bottom: 20px;
  box-shadow: 0 0 12px rgba(0, 102, 255, 0.20);
  transition: box-shadow 0.4s ease, transform 0.3s ease;
  animation: fade-slide-up 0.6s both;
  animation-delay: 0.1s;
}

.login-icon-wrap:hover {
  box-shadow: 0 0 20px rgba(0, 102, 255, 0.30);
  transform: scale(1.04);
}

.login-icon-wrap .el-icon {
  color: #0066ff;
  filter: drop-shadow(0 0 6px rgba(0, 102, 255, 0.20));
}

/* Title */
.login-title {
  margin: 0;
  font-family: 'Outfit', 'Noto Sans SC', sans-serif;
  font-size: 24px;
  font-weight: 700;
  color: #0a1929;
  letter-spacing: 0.5px;
  animation: fade-slide-up 0.6s both;
  animation-delay: 0.2s;
}

/* Subtitle */
.login-subtitle {
  margin: 10px 0 0;
  font-family: 'JetBrains Mono', 'SF Mono', 'Fira Code', monospace;
  font-size: 11px;
  font-weight: 400;
  color: #6b7d99;
  letter-spacing: 3px;
  text-transform: uppercase;
  animation: fade-slide-up 0.6s both;
  animation-delay: 0.3s;
}

/* ===== Form ===== */
.login-form {
  margin-top: 4px;
}

.login-form :deep(.el-form-item) {
  margin-bottom: 22px;
  animation: fade-slide-up 0.6s both;
}

.login-form :deep(.el-form-item):nth-child(1) { animation-delay: 0.4s; }
.login-form :deep(.el-form-item):nth-child(2) { animation-delay: 0.5s; }
.login-form :deep(.el-form-item):nth-child(3) { animation-delay: 0.6s; }

/* Input wrapper — light glass */
.login-form :deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.6);
  box-shadow: none;
  border: 1px solid rgba(0, 60, 130, 0.10);
  border-radius: 10px;
  transition: border-color 0.3s ease, background 0.3s ease, box-shadow 0.3s ease;
  padding: 4px 12px;
}

.login-form :deep(.el-input__wrapper:hover) {
  border-color: rgba(0, 102, 255, 0.20);
  background: rgba(255, 255, 255, 0.75);
}

.login-form :deep(.el-input__wrapper.is-focus) {
  border-color: #0066ff;
  background: rgba(255, 255, 255, 0.85);
  box-shadow: 0 0 0 3px rgba(0, 102, 255, 0.12);
}

.login-form :deep(.el-input__inner) {
  height: 46px;
  color: #0a1929;
  font-family: 'Outfit', 'Noto Sans SC', sans-serif;
  font-size: 14px;
  caret-color: #0066ff;
}

.login-form :deep(.el-input__inner::placeholder) {
  color: rgba(107, 125, 153, 0.55);
  font-family: 'Outfit', 'Noto Sans SC', sans-serif;
  font-size: 13px;
}

/* Prefix icon */
.login-form :deep(.el-input__prefix) {
  margin-right: 8px;
}

.login-form :deep(.el-input__prefix-inner) {
  color: #6b7d99;
  transition: color 0.3s ease;
}

.login-form :deep(.el-input__wrapper.is-focus .el-input__prefix-inner) {
  color: #0066ff;
}

/* Error state */
.login-form :deep(.el-form-item.is-error .el-input__wrapper) {
  border-color: rgba(245, 108, 108, 0.50);
  box-shadow: 0 0 0 3px rgba(245, 108, 108, 0.08);
}

.login-form :deep(.el-form-item__error) {
  color: #f56c6c;
  font-size: 12px;
  font-family: 'JetBrains Mono', 'SF Mono', 'Fira Code', monospace;
  margin-top: 6px;
}

/* ===== Login Button ===== */
.login-button {
  width: 100%;
  height: 48px;
  font-size: 15px;
  font-weight: 600;
  font-family: 'Outfit', 'Noto Sans SC', sans-serif;
  letter-spacing: 4px;
  border-radius: 10px;
  margin-top: 4px;
  border: none;
  background: linear-gradient(135deg, #0066ff 0%, #00b4d8 100%);
  color: #ffffff;
  transition: all 0.3s cubic-bezier(0.22, 1, 0.36, 1);
  position: relative;
  overflow: hidden;
}

.login-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 102, 255, 0.25);
}

.login-button:active {
  transform: translateY(0);
  box-shadow: 0 4px 12px rgba(0, 102, 255, 0.15);
}

/* Loading state */
.login-button.is-loading {
  background: linear-gradient(135deg, #0052cc 0%, #0098b8 100%);
  color: #ffffff;
}

.login-button :deep(.el-loading-spinner .path) {
  stroke: #ffffff;
  stroke-width: 3;
}

/* ===== Responsive ===== */
@media (max-width: 480px) {
  .login-card {
    padding: 32px 24px 28px;
    border-radius: 12px;
  }

  .login-title {
    font-size: 20px;
  }

  .login-icon-wrap {
    width: 56px;
    height: 56px;
  }
}
</style>
