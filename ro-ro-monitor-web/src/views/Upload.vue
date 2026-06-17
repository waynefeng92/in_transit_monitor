<template>
  <div class="upload-page">
    <el-row :gutter="20" class="upload-row">
      <!-- 上传区域 -->
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <div class="card-header__left">
                <span>上传在途表</span>
                <span class="card-header__subtitle">导入 Excel 数据到系统</span>
              </div>
            </div>
          </template>
          
          <el-form label-width="90px" class="upload-form">
            <el-form-item label="选择品牌" required>
              <el-select
                v-model="uploadForm.brandId"
                placeholder="请选择品牌"
                style="width: 100%"
                @change="handleBrandChange"
              >
                <el-option
                  v-for="brand in brandList"
                  :key="brand.id"
                  :label="brand.brandName"
                  :value="brand.id"
                />
              </el-select>
            </el-form-item>

            <el-form-item label="上传人">
              <el-input
                v-model="uploadForm.user"
                placeholder="请输入上传人（默认system）"
              />
            </el-form-item>

            <el-form-item label="选择文件" required>
              <div class="upload-zone">
                <!-- 渐变边框拖拽区 -->
                <el-upload
                  ref="uploadRef"
                  :auto-upload="false"
                  :on-change="handleFileChange"
                  :on-remove="handleFileRemove"
                  :limit="1"
                  accept=".xlsx,.xls"
                  drag
                  class="gradient-upload"
                >
                  <div class="upload-zone__icon">
                    <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
                  </div>
                  <div class="el-upload__text">
                    将文件拖到此处，或<em>点击上传</em>
                  </div>
                  <template #tip>
                    <div class="el-upload__tip">
                      支持 .xlsx / .xls 格式，文件大小不超过 10MB
                    </div>
                  </template>
                </el-upload>
                <!-- 文件选中状态 -->
                <div v-if="uploadForm.file" class="file-indicator">
                  <el-icon size="18" color="var(--color-success)"><CircleCheckFilled /></el-icon>
                  <span class="file-indicator__name">{{ uploadForm.file.name }}</span>
                  <span class="file-indicator__size">({{ formatFileSize(uploadForm.file.size) }})</span>
                </div>
              </div>
              <!-- 上传进度 -->
              <div v-if="uploading" class="upload-progress-bar">
                <el-progress
                  :percentage="uploadProgress"
                  :stroke-width="8"
                  :color="progressColor"
                  :format="progressFormat"
                />
              </div>
            </el-form-item>

            <el-form-item class="upload-actions">
              <el-button
                type="primary"
                @click="handlePreview"
                :disabled="previewing || !uploadForm.file || !uploadForm.brandId"
              >
                预览
              </el-button>
              <el-button
                type="success"
                @click="handleUpload"
                :disabled="!uploadForm.file || !uploadForm.brandId || !hasPreviewed"
              >
                上传
              </el-button>
              <el-button @click="handleReset">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <!-- 预览区域 -->
      <el-col :span="12" v-if="previewData">
        <el-card class="preview-card">
          <template #header>
            <div class="card-header">
              <div class="card-header__left">
                <span>预览数据</span>
                <span class="card-header__subtitle">{{ previewData.fileName }}</span>
              </div>
            </div>
          </template>

          <!-- Sheet 选择 -->
          <el-form-item label="选择Sheet" v-if="previewData.sheets?.length > 0">
            <el-select
              v-model="selectedSheetIndex"
              placeholder="请选择Sheet"
              @change="handleSheetChange"
              style="width: 100%"
            >
              <el-option
                v-for="sheet in previewData.sheets"
                :key="sheet.index"
                :label="`${sheet.name} (${sheet.rowCount}行)`"
                :value="sheet.index"
              />
            </el-select>
          </el-form-item>

          <!-- 表头 -->
          <div class="preview-section" v-if="previewData.headerRow?.length">
            <h4 class="preview-section__title">表头字段</h4>
            <div class="header-tags">
              <el-tag
                v-for="(header, index) in previewData.headerRow"
                :key="index"
                class="header-tag"
                :type="header ? 'primary' : 'info'"
                effect="plain"
                size="small"
              >
                {{ header || '(空)' }}
              </el-tag>
            </div>
          </div>

          <!-- 数据预览 -->
          <div class="preview-section" v-if="previewData.previewData?.length">
            <h4 class="preview-section__title">数据预览（前5行）</h4>
            <el-table 
              :data="previewData.previewData" 
              border 
              stripe 
              size="small" 
              max-height="300"
            >
              <el-table-column
                v-for="(_, index) in previewData.headerRow"
                :key="index"
                :prop="String(index)"
                :label="previewData.headerRow[index] || `列${index + 1}`"
                min-width="120"
              >
                <template #default="{ row }">
                  {{ row[index] || '-' }}
                </template>
              </el-table-column>
            </el-table>
          </div>

          <el-empty v-if="!previewData.previewData?.length" description="暂无预览数据" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 上传历史 -->
    <el-row class="history-row">
      <el-col :span="24">
        <el-card>
          <template #header>
            <div class="card-header">
              <div class="card-header__left">
                <span>上传历史</span>
                <span class="card-header__subtitle">最近 30 条上传记录</span>
              </div>
              <div class="header-actions">
                <el-button size="small" @click="loadHistory">
                  <el-icon><Refresh /></el-icon>
                  刷新
                </el-button>
              </div>
            </div>
          </template>
          <el-table :data="historyList" border stripe v-loading="historyLoading">
            <el-table-column prop="batchId" label="批次号" width="280">
              <template #default="{ row }">
                <code class="batch-id">{{ row.batchId }}</code>
              </template>
            </el-table-column>
            <el-table-column prop="fileName" label="文件名" min-width="200" />
            <el-table-column prop="uploadUser" label="上传人" width="120" />
            <el-table-column label="记录数" width="120" align="center">
              <template #default="{ row }">
                <!-- 处理中 -->
                <template v-if="row.status === 'PROCESSING'">
                  <el-icon class="is-loading" color="var(--color-primary)"><Loading /></el-icon>
                  <span style="margin-left: 4px; color: var(--text-muted)">处理中</span>
                </template>
                <!-- 成功（有状态字段） -->
                <template v-else-if="row.status === 'SUCCESS'">
                  <el-tag type="success" effect="plain">{{ row.recordCount }} 条</el-tag>
                </template>
                <!-- 失败（有状态字段） -->
                <template v-else-if="row.status === 'FAILED'">
                  <el-tag type="danger" effect="plain">失败</el-tag>
                </template>
                <!-- 兼容历史数据 -->
                <template v-else-if="!row.status && row.recordCount > 0">
                  <el-tag type="success" effect="plain">{{ row.recordCount }} 条</el-tag>
                </template>
                <template v-else-if="!row.status && row.recordCount === 0">
                  <el-tag type="danger" effect="plain">失败</el-tag>
                </template>
                <template v-else>
                  <span v-if="row.recordCount > 0" class="text-muted">{{ row.recordCount }} 条</span>
                  <span v-else class="text-muted">-</span>
                </template>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100" align="center">
              <template #default="{ row }">
                <el-tag
                  v-if="row.status === 'PROCESSING'"
                  type="info"
                  effect="light"
                >
                  <el-icon class="is-loading" style="margin-right: 2px"><Loading /></el-icon>
                  处理中
                </el-tag>
                <el-tag v-else-if="row.status === 'SUCCESS'" type="success" effect="light">成功</el-tag>
                <el-tag v-else-if="row.status === 'FAILED'" type="danger" effect="light">失败</el-tag>
                <el-tag v-else-if="!row.status && row.recordCount > 0" type="success" effect="light">成功</el-tag>
                <el-tag v-else-if="!row.status && row.recordCount === 0" type="danger" effect="light">失败</el-tag>
                <span v-else class="text-muted">-</span>
              </template>
            </el-table-column>
            <el-table-column prop="uploadTime" label="上传时间" width="180">
              <template #default="{ row }">
                <span class="upload-time">{{ formatDateTime(row.uploadTime) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="结果详情" min-width="160">
              <template #default="{ row }">
                <span v-if="row.status === 'PROCESSING'" class="text-muted">处理中...</span>
                <span v-else-if="row.status === 'SUCCESS' && !row.errorMessage" style="color: var(--color-success)">
                  ✓ 全部成功 {{ row.recordCount }} 条
                </span>
                <span v-else-if="row.status === 'SUCCESS' && row.errorMessage" style="color: var(--color-warning)">
                  ⚠ {{ row.recordCount }} 条成功，部分线路未匹配 
                </span>
                <span v-else-if="row.status === 'FAILED'" style="color: var(--color-danger)">✗ 处理失败</span>
                <span v-else class="text-muted">-</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100" align="center">
              <template #default="{ row }">
                <el-popover
                  v-if="row.errorMessage"
                  placement="left"
                  width="400"
                  trigger="click"
                  :title="'处理详情 - ' + row.batchId"
                >
                  <template #reference>
                    <el-button size="small" text type="primary">详情</el-button>
                  </template>
                  <div v-if="parseResult(row.errorMessage)" style="font-size:13px;line-height:2">
                    <p><b>成功数：</b>{{ parseResult(row.errorMessage).successCount || 0 }}</p>
                    <p v-if="parseResult(row.errorMessage).failCount > 0"><b>失败数：</b>{{ parseResult(row.errorMessage).failCount }}</p>
                    <p v-if="parseResult(row.errorMessage).routeUnmatchedCount > 0"><b>未匹配线路数：</b>{{ parseResult(row.errorMessage).routeUnmatchedCount }}</p>
                    <div v-if="parseResult(row.errorMessage).failDetails && parseResult(row.errorMessage).failDetails.length > 0" style="margin-top:8px">
                      <p><b>失败明细：</b></p>
                      <p v-for="(item, i) in parseResult(row.errorMessage).failDetails" :key="i" style="font-size:12px;color:var(--text-muted);margin:2px 0">{{ item }}</p>
                    </div>
                  </div>
                  <div v-else style="max-height:300px;overflow-y:auto;font-size:12px;line-height:1.8;white-space:pre-wrap;word-break:break-all">{{ row.errorMessage }}</div>
                </el-popover>
                <span v-else class="text-muted">-</span>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { UploadFilled, Refresh, Loading, Warning, CircleCheckFilled } from '@element-plus/icons-vue'
import request from '@/api/request'
import { getBrandList } from '@/api/brand'

// 品牌列表
const brandList = ref([])

// 上传表单
const uploadForm = ref({
  brandId: null,
  user: 'system',
  file: null
})

// 状态
const previewing = ref(false)
const uploading = ref(false)
const hasPreviewed = ref(false)
const uploadProgress = ref(0)
const previewData = ref(null)
const selectedSheetIndex = ref(0)

// 格式化文件大小
const formatFileSize = (bytes) => {
  if (!bytes) return ''
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

// 进度条颜色 — 随进度变化
const progressColor = computed(() => {
  if (uploadProgress.value < 30) return '#e6a23c'   // warning
  if (uploadProgress.value < 80) return '#1d72f3'   // primary
  return '#67c23a'                                   // success
})

// 进度文本
const progressFormat = (percent) => {
  return percent < 100 ? `上传中 ${percent}%` : '上传完成'
}

// 上传历史
const historyList = ref([])
const historyLoading = ref(false)

// 轮询定时器
let pollingTimer = null

// 加载品牌列表
const loadBrands = async () => {
  try {
    brandList.value = await getBrandList()
  } catch (error) {
    console.error('加载品牌失败:', error)
  }
}

// 品牌切换时清空预览
const handleBrandChange = () => {
  previewData.value = null
  hasPreviewed.value = false
  selectedSheetIndex.value = 0
}

// 文件变化
const handleFileChange = (file) => {
  uploadForm.value.file = file.raw
  previewData.value = null
  hasPreviewed.value = false
  selectedSheetIndex.value = 0
}

// 文件移除
const handleFileRemove = () => {
  uploadForm.value.file = null
  previewData.value = null
  hasPreviewed.value = false
  selectedSheetIndex.value = 0
}

// 预览
const handlePreview = async () => {
  if (!uploadForm.value.file || !uploadForm.value.brandId) {
    ElMessage.warning('请选择品牌和文件')
    return
  }

  previewing.value = true
  const formData = new FormData()
  formData.append('file', uploadForm.value.file)
  formData.append('brandId', uploadForm.value.brandId)

  try {
    const res = await request.post('/upload/preview', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    previewData.value = res
    hasPreviewed.value = true
    selectedSheetIndex.value = res.defaultSheetIndex || 0
    ElMessage.success('预览成功')
  } catch (error) {
    console.error('预览失败:', error)
    ElMessage.error('预览失败，请检查文件格式')
  } finally {
    previewing.value = false
  }
}

// Sheet 切换
const handleSheetChange = async () => {
  if (!uploadForm.value.file || !uploadForm.value.brandId) {
    return
  }

  previewing.value = true
  const formData = new FormData()
  formData.append('file', uploadForm.value.file)
  formData.append('brandId', uploadForm.value.brandId)
  formData.append('sheetIndex', selectedSheetIndex.value)

  try {
    const res = await request.post('/upload/preview', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    previewData.value = res
    hasPreviewed.value = true
  } catch (error) {
    console.error('切换Sheet失败:', error)
  } finally {
    previewing.value = false
  }
}

// 上传
const handleUpload = async () => {
  if (!uploadForm.value.file || !uploadForm.value.brandId) {
    ElMessage.warning('请选择品牌和文件')
    return
  }

  // 确认信息
  const brand = brandList.value.find(b => b.id === uploadForm.value.brandId)
  const brandName = brand?.brandName || uploadForm.value.brandId
  const fileName = uploadForm.value.file.name
  const currentSheet = previewData.value?.sheets?.find(s => s.index === selectedSheetIndex.value)
  const sheetName = currentSheet ? `${currentSheet.name}（${currentSheet.rowCount}行）` : '（自动定位）'

  // 前两行预览
  const headers = previewData.value?.headerRow || []
  const rows = (previewData.value?.previewData || []).slice(0, 2)
  let previewHtml = ''
  if (headers.length > 0) {
    const headerCells = headers.map(h => `<th style="padding:4px 8px;border:1px solid #dbe7f5;font-size:12px;background:#f3f8ff;white-space:nowrap">${h}</th>`).join('')
    const rowHtml = rows.map(r =>
      `<tr>${headers.map((_, i) => `<td style="padding:4px 8px;border:1px solid #dbe7f5;font-size:12px">${r[i] ?? '-'}</td>`).join('')}</tr>`
    ).join('')
    previewHtml = `<table style="border-collapse:collapse;width:100%;margin-top:8px">${headerCells ? `<thead><tr>${headerCells}</tr></thead>` : ''}<tbody>${rowHtml}</tbody></table>`
  }

  try {
    await ElMessageBox.confirm(
      `<div style="font-size:14px;line-height:2">
        <p><b>品牌：</b>${brandName}</p>
        <p><b>文件：</b>${fileName}</p>
        <p><b>Sheet：</b>${sheetName}</p>
        ${previewHtml ? `<div style="margin-top:12px;font-weight:600;font-size:13px">数据预览（前2行）：</div>${previewHtml}` : ''}
      </div>`,
      '确认上传',
      {
        confirmButtonText: '确认上传',
        cancelButtonText: '取消',
        type: 'info',
        dangerouslyUseHTMLString: true
      }
    )
  } catch {
    return // 用户取消
  }

  uploadProgress.value = 0
  uploading.value = true
  const formData = new FormData()
  formData.append('file', uploadForm.value.file)
  formData.append('brandId', uploadForm.value.brandId)
  formData.append('user', uploadForm.value.user || 'system')
  if (selectedSheetIndex.value !== undefined && selectedSheetIndex.value !== null) {
    formData.append('sheetIndex', selectedSheetIndex.value)
  }

  try {
    const res = await request.post('/upload/excel', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 120000,
      onUploadProgress: (progressEvent) => {
        uploadProgress.value = Math.round((progressEvent.loaded / progressEvent.total) * 100)
      }
    })
    if (res.success) {
      ElMessage.success(`上传成功！批次号：${res.batchId}`)
      handleReset()
      loadHistory()
    } else {
      ElMessage.error(res.message || '上传失败')
    }
  } catch (error) {
    console.error('上传失败:', error)
    ElMessage.error('上传失败，请重试')
  } finally {
    uploading.value = false
  }
}

// 重置
const handleReset = () => {
  uploadForm.value = {
    brandId: null,
    user: 'system',
    file: null
  }
  previewData.value = null
  hasPreviewed.value = false
  selectedSheetIndex.value = 0
}

// 加载上传历史
const loadHistory = async () => {
  historyLoading.value = true
  try {
    historyList.value = await request.get('/upload/history')
    
    // 检查是否有处理中的批次，有则继续轮询
    const hasProcessing = historyList.value.some(item => item.status === 'PROCESSING')
    if (hasProcessing) {
      startPolling()
    } else {
      stopPolling()
    }
  } catch (error) {
    console.error('加载历史失败:', error)
  } finally {
    historyLoading.value = false
  }
}

// 开始轮询
const startPolling = () => {
  if (pollingTimer) return
  pollingTimer = setInterval(() => {
    request.get('/upload/history').then(res => {
      historyList.value = res
      const hasProcessing = res.some(item => item.status === 'PROCESSING')
      if (!hasProcessing) {
        stopPolling()
      }
    }).catch(err => {
      console.error('轮询失败:', err)
    })
  }, 3000)
}

// 停止轮询
const stopPolling = () => {
  if (pollingTimer) {
    clearInterval(pollingTimer)
    pollingTimer = null
  }
}

// 格式化时间
const formatDateTime = (time) => {
  if (!time) return '-'
  const date = new Date(time)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

// 解析错误消息 JSON
const parseResult = (msg) => {
  if (!msg) return null
  try {
    const parsed = JSON.parse(msg)
    if (typeof parsed === 'object' && parsed.successCount !== undefined) return parsed
    return null
  } catch { return null }
}

onMounted(() => {
  loadBrands()
  loadHistory()
})

onUnmounted(() => {
  stopPolling()
})
</script>

<style scoped>
.upload-page {
  height: 100%;
}

/* ── Layout ── */
.upload-row {
  margin-bottom: 0;
}

.history-row {
  margin-top: 20px;
}

/* ── Card header with subtitle ── */
.card-header__left {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.card-header__subtitle {
  font-size: var(--font-size-sm);
  font-weight: 400;
  color: var(--text-muted);
}

/* ── Upload form ── */
.upload-form {
  margin-top: 4px;
}

.upload-actions {
  margin-top: 8px;
}

/* ── Gradient border drag zone ── */
.upload-zone :deep(.gradient-upload .el-upload-dragger) {
  border: 2px solid transparent;
  background:
    linear-gradient(180deg, rgba(255,255,255,0.98), rgba(248,251,255,0.98)) padding-box,
    var(--card-gradient) border-box;
  border-radius: var(--radius-lg);
  padding: 34px 20px;
  transition: all var(--transition-normal);
}

.upload-zone :deep(.gradient-upload .el-upload-dragger:hover) {
  border-color: transparent;
  background:
    linear-gradient(180deg, rgba(255,255,255,0.98), rgba(243,248,255,0.98)) padding-box,
    linear-gradient(135deg, #1d72f3, #6ba3f9) border-box;
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(29, 114, 243, 0.10);
}

.upload-zone :deep(.gradient-upload .el-upload-dragger.is-dragover) {
  border-color: transparent;
  background:
    linear-gradient(180deg, rgba(243,248,255,0.98), rgba(233,240,248,0.98)) padding-box,
    linear-gradient(135deg, #1d72f3, #6ba3f9) border-box;
  transform: scale(1.02);
  box-shadow: 0 10px 28px rgba(29, 114, 243, 0.14);
}

.upload-zone__icon {
  margin-bottom: 8px;
}

.upload-zone__icon .el-icon--upload {
  font-size: 48px;
  color: var(--color-primary);
}

/* ── File selected indicator ── */
.file-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  margin-top: 12px;
  background: linear-gradient(180deg, var(--color-success-bg), rgba(243, 251, 247, 0.98));
  border: 1px solid #b7e3c4;
  border-radius: var(--radius-sm);
}

.file-indicator__name {
  font-weight: 600;
  font-size: var(--font-size-base);
  color: var(--text-primary);
}

.file-indicator__size {
  font-size: var(--font-size-sm);
  color: var(--text-muted);
}

/* ── Upload progress bar ── */
.upload-progress-bar {
  margin-top: 16px;
  padding: 0 2px;
}

.upload-progress-bar :deep(.el-progress-bar__outer) {
  background-color: var(--color-border-light);
  border-radius: 10px;
}

/* ── Preview section ── */
.preview-card {
  height: 100%;
}

.preview-section {
  margin-bottom: 20px;
}

.preview-section__title {
  margin: 0 0 10px 0;
  font-size: var(--font-size-base);
  color: var(--text-secondary);
  font-weight: 600;
}

.header-tags {
  padding: 12px;
  background: linear-gradient(180deg, rgba(255,255,255,0.98), rgba(246,250,255,0.98));
  border: 1px solid var(--color-primary-lightest);
  border-radius: var(--radius-md);
  min-height: 40px;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.header-tag {
  margin: 0;
}

/* ── History table ── */
.batch-id {
  font-family: 'SF Mono', 'Fira Code', monospace;
  font-size: var(--font-size-xs);
  color: var(--text-primary);
  background: var(--bg-light);
  padding: 2px 6px;
  border-radius: 4px;
}

.upload-time {
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
}

/* ── Utils ── */
.text-muted {
  color: var(--text-muted);
}

.is-loading {
  animation: rotating 2s linear infinite;
}

@keyframes rotating {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}
</style>
