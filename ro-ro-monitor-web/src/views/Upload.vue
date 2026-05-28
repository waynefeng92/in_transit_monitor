<template>
  <div class="upload-page">
    <el-row :gutter="20">
      <!-- 上传区域 -->
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>上传在途表</span>
          </template>
          
          <el-form label-width="100px">
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
                <el-upload
                  ref="uploadRef"
                  :auto-upload="false"
                  :on-change="handleFileChange"
                  :on-remove="handleFileRemove"
                  :limit="1"
                  accept=".xlsx,.xls"
                  drag
                >
                  <el-icon class="el-icon--upload" style="font-size: 48px; color: var(--color-primary);"><UploadFilled /></el-icon>
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
                  <el-icon size="18" color="#67c23a"><CircleCheckFilled /></el-icon>
                  <span class="file-indicator__name">{{ uploadForm.file.name }}</span>
                  <span class="file-indicator__size">({{ formatFileSize(uploadForm.file.size) }})</span>
                </div>
              </div>
              <!-- 上传进度 -->
              <div v-if="uploading" class="upload-progress-bar">
                <el-progress :percentage="uploadProgress" :stroke-width="6" status="success" />
              </div>
            </el-form-item>

            <el-form-item>
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
                :disabled="!uploadForm.file || !uploadForm.brandId"
                :loading="uploading"
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
        <el-card>
          <template #header>
            <div class="card-header">
              <span>预览数据</span>
              <el-tag type="info">{{ previewData.fileName }}</el-tag>
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
            <h4>表头字段</h4>
            <div class="header-tags">
              <el-tag
                v-for="(header, index) in previewData.headerRow"
                :key="index"
                style="margin: 0 8px 8px 0"
              >
                {{ header || '(空)' }}
              </el-tag>
            </div>
          </div>

          <!-- 数据预览 -->
          <div class="preview-section" v-if="previewData.previewData?.length">
            <h4>数据预览（前5行）</h4>
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
    <el-row style="margin-top: 20px">
      <el-col :span="24">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>上传历史</span>
              <el-button size="small" @click="loadHistory">
                <el-icon><Refresh /></el-icon>
                刷新
              </el-button>
            </div>
          </template>
          <el-table :data="historyList" border stripe v-loading="historyLoading">
            <el-table-column prop="batchId" label="批次号" width="280" />
            <el-table-column prop="fileName" label="文件名" min-width="200" />
            <el-table-column prop="uploadUser" label="上传人" width="120" />
            <el-table-column label="记录数" width="120" align="center">
              <template #default="{ row }">
                <!-- 处理中 -->
                <template v-if="row.status === 'PROCESSING'">
                  <el-icon class="is-loading"><Loading /></el-icon>
                  <span style="margin-left: 4px">处理中</span>
                </template>
                <!-- 成功（有状态字段） -->
                <template v-else-if="row.status === 'SUCCESS'">
                  <el-tag type="success">{{ row.recordCount }} 条</el-tag>
                </template>
                <!-- 失败（有状态字段） -->
                <template v-else-if="row.status === 'FAILED'">
                  <el-tag type="danger">失败</el-tag>
                </template>
                <!-- 兼容历史数据：状态为空但记录数大于0 → 成功 -->
                <template v-else-if="!row.status && row.recordCount > 0">
                  <el-tag type="success">{{ row.recordCount }} 条</el-tag>
                </template>
                <!-- 兼容历史数据：状态为空且记录数为0 → 失败 -->
                <template v-else-if="!row.status && row.recordCount === 0">
                  <el-tag type="danger">失败</el-tag>
                </template>
                <!-- 其他 -->
                <template v-else>
                  <span v-if="row.recordCount > 0">{{ row.recordCount }} 条</span>
                  <span v-else>-</span>
                </template>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100" align="center">
              <template #default="{ row }">
                <el-tag v-if="row.status === 'PROCESSING'" type="info">处理中</el-tag>
                <el-tag v-else-if="row.status === 'SUCCESS'" type="success">成功</el-tag>
                <el-tag v-else-if="row.status === 'FAILED'" type="danger">失败</el-tag>
                <!-- 兼容历史数据 -->
                <el-tag v-else-if="!row.status && row.recordCount > 0" type="success">成功</el-tag>
                <el-tag v-else-if="!row.status && row.recordCount === 0" type="danger">失败</el-tag>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column prop="uploadTime" label="上传时间" width="180">
              <template #default="{ row }">
                {{ formatDateTime(row.uploadTime) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="80" align="center">
              <template #default="{ row }">
                <el-tooltip 
                  v-if="(row.status === 'FAILED' || (!row.status && row.recordCount === 0)) && row.errorMessage" 
                  :content="row.errorMessage" 
                  placement="left"
                >
                  <el-button type="danger" size="small" text>
                    <el-icon><Warning /></el-icon>
                  </el-button>
                </el-tooltip>
                <span v-else>-</span>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
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
  selectedSheetIndex.value = 0
}

// 文件变化
const handleFileChange = (file) => {
  uploadForm.value.file = file.raw
  previewData.value = null
  selectedSheetIndex.value = 0
}

// 文件移除
const handleFileRemove = () => {
  uploadForm.value.file = null
  previewData.value = null
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

/* ---- Upload drag zone ---- */
.upload-zone :deep(.el-upload-dragger) {
  border: 2px dashed #dbe7f5;
  border-radius: 16px;
  padding: 36px 20px;
  transition: all 0.3s ease;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(246, 250, 255, 0.98));
}

.upload-zone :deep(.el-upload-dragger:hover) {
  border-color: var(--color-primary);
  background: rgba(29, 114, 243, 0.02);
}

.upload-zone :deep(.el-upload-dragger.is-dragover) {
  border-color: var(--color-primary);
  background: rgba(29, 114, 243, 0.04);
  transform: scale(1.01);
}

.upload-zone :deep(.el-icon--upload) {
  margin-bottom: 8px;
}

/* ---- File selected indicator ---- */
.file-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  margin-top: 12px;
  background: linear-gradient(180deg, #f0f9ff, #e8f4fd);
  border: 1px solid #b7d9f5;
  border-radius: 10px;
}

.file-indicator__name {
  font-weight: 600;
  font-size: 14px;
  color: var(--text-primary);
}

.file-indicator__size {
  font-size: 13px;
  color: var(--text-muted);
}

/* ---- Upload progress ---- */
.upload-progress-bar {
  margin-top: 16px;
  padding: 0 2px;
}

/* ---- Preview section ---- */
.preview-section {
  margin-bottom: 20px;
}

.preview-section h4 {
  margin: 0 0 10px 0;
  font-size: 14px;
  color: var(--text-secondary);
}

.header-tags {
  padding: 12px;
  background: linear-gradient(180deg, rgba(255,255,255,0.98), rgba(246,250,255,0.98));
  border: 1px solid #dbe7f5;
  border-radius: 12px;
  min-height: 40px;
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
