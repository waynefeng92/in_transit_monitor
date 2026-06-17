<template>
  <div class="otd-config">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>OTD时效配置</span>
          <div class="header-actions">
            <el-button @click="handleExport" :disabled="!filterBrandId">
              <el-icon><Download /></el-icon>
              导出配置
            </el-button>
            <el-button type="success" @click="handleOpenImport" :disabled="!filterBrandId">
              <el-icon><Upload /></el-icon>
              批量导入
            </el-button>
          </div>
        </div>
      </template>

      <!-- 提示信息 -->
      <el-alert
        title="配置说明"
        type="info"
        :closable="false"
        show-icon
        class="config-alert"
      >
        <template #default>
          将标准OTD设置为 <strong>0</strong> 可跳过该阶段的监控（适用于无法获取该阶段时间的场景）。
        </template>
      </el-alert>

      <!-- 选择线路 -->
      <div class="filter-bar">
        <el-select
          v-model="filterBrandId"
          placeholder="按品牌筛选"
          clearable
          @change="handleBrandChange"
          style="width: 200px"
        >
          <el-option
            v-for="brand in brandList"
            :key="brand.id"
            :label="brand.brandName"
            :value="brand.id"
          />
        </el-select>
        <el-select
          v-model="selectedRouteId"
          placeholder="请选择线路"
          @change="handleRouteChange"
          style="width: 350px"
        >
          <el-option
            v-for="route in routeList"
            :key="route.id"
            :label="`${getBrandName(route.brandId)}: ${route.originCity || ''} → ${route.destCity || ''}`"
            :value="route.id"
          />
        </el-select>
      </div>

      <!-- 配置表单 — 7段卡片分组 -->
      <el-form
        v-if="selectedRouteId"
        ref="formRef"
        :model="formData"
        label-width="0"
        class="config-form"
      >
        <div class="segment-grid">
          <!-- 1. 未出库 → 集港在途 -->
          <div class="segment-card">
            <div class="segment-card__header">
              <span class="segment-index">01</span>
              <span class="segment-title">未出库 → 集港在途</span>
              <div class="segment-badges">
                <el-tag size="small" class="badge-otd">标准OTD</el-tag>
                <el-tag size="small" class="badge-warn">预警</el-tag>
              </div>
            </div>
            <div class="segment-card__body">
              <div class="segment-field segment-field--otd">
                <label class="field-label">标准OTD（小时）</label>
                <el-input-number v-model="formData.notDepartedOtd" :min="0" :step="0.5" :precision="1" controls-position="right" />
              </div>
              <div class="segment-field segment-field--warn">
                <label class="field-label">预警时效（小时）</label>
                <el-input-number v-model="formData.notDepartedWarn" :min="0" :step="0.5" :precision="1" controls-position="right" />
              </div>
            </div>
          </div>

          <!-- 2. 集港在途 → 已集港待装船 -->
          <div class="segment-card">
            <div class="segment-card__header">
              <span class="segment-index">02</span>
              <span class="segment-title">集港在途 → 已集港待装船</span>
              <div class="segment-badges">
                <el-tag size="small" class="badge-otd">标准OTD</el-tag>
                <el-tag size="small" class="badge-warn">预警</el-tag>
              </div>
            </div>
            <div class="segment-card__body">
              <div class="segment-field segment-field--otd">
                <label class="field-label">标准OTD（小时）</label>
                <el-input-number v-model="formData.toPortOtd" :min="0" :step="0.5" :precision="1" controls-position="right" />
              </div>
              <div class="segment-field segment-field--warn">
                <label class="field-label">预警时效（小时）</label>
                <el-input-number v-model="formData.toPortWarn" :min="0" :step="0.5" :precision="1" controls-position="right" />
              </div>
            </div>
          </div>

          <!-- 3. 已集港待装船 → 水运在途 -->
          <div class="segment-card">
            <div class="segment-card__header">
              <span class="segment-index">03</span>
              <span class="segment-title">已集港待装船 → 水运在途</span>
              <div class="segment-badges">
                <el-tag size="small" class="badge-otd">标准OTD</el-tag>
                <el-tag size="small" class="badge-warn">预警</el-tag>
              </div>
            </div>
            <div class="segment-card__body">
              <div class="segment-field segment-field--otd">
                <label class="field-label">标准OTD（小时）</label>
                <el-input-number v-model="formData.atPortWaitOtd" :min="0" :step="0.5" :precision="1" controls-position="right" />
              </div>
              <div class="segment-field segment-field--warn">
                <label class="field-label">预警时效（小时）</label>
                <el-input-number v-model="formData.atPortWaitWarn" :min="0" :step="0.5" :precision="1" controls-position="right" />
              </div>
            </div>
          </div>

          <!-- 4. 水运在途 → 已到港待卸船 -->
          <div class="segment-card">
            <div class="segment-card__header">
              <span class="segment-index">04</span>
              <span class="segment-title">水运在途 → 已到港待卸船</span>
              <div class="segment-badges">
                <el-tag size="small" class="badge-otd">标准OTD</el-tag>
                <el-tag size="small" class="badge-warn">预警</el-tag>
              </div>
            </div>
            <div class="segment-card__body">
              <div class="segment-field segment-field--otd">
                <label class="field-label">标准OTD（小时）</label>
                <el-input-number v-model="formData.onSeaOtd" :min="0" :step="0.5" :precision="1" controls-position="right" />
              </div>
              <div class="segment-field segment-field--warn">
                <label class="field-label">预警时效（小时）</label>
                <el-input-number v-model="formData.onSeaWarn" :min="0" :step="0.5" :precision="1" controls-position="right" />
              </div>
            </div>
          </div>

          <!-- 5. 已到港待卸船 → 已卸船待分拨 -->
          <div class="segment-card">
            <div class="segment-card__header">
              <span class="segment-index">05</span>
              <span class="segment-title">已到港待卸船 → 已卸船待分拨</span>
              <div class="segment-badges">
                <el-tag size="small" class="badge-otd">标准OTD</el-tag>
                <el-tag size="small" class="badge-warn">预警</el-tag>
              </div>
            </div>
            <div class="segment-card__body">
              <div class="segment-field segment-field--otd">
                <label class="field-label">标准OTD（小时）</label>
                <el-input-number v-model="formData.atDestWaitOtd" :min="0" :step="0.5" :precision="1" controls-position="right" />
              </div>
              <div class="segment-field segment-field--warn">
                <label class="field-label">预警时效（小时）</label>
                <el-input-number v-model="formData.atDestWaitWarn" :min="0" :step="0.5" :precision="1" controls-position="right" />
              </div>
            </div>
          </div>

          <!-- 6. 已卸船待分拨 → 分拨在途 -->
          <div class="segment-card">
            <div class="segment-card__header">
              <span class="segment-index">06</span>
              <span class="segment-title">已卸船待分拨 → 分拨在途</span>
              <div class="segment-badges">
                <el-tag size="small" class="badge-otd">标准OTD</el-tag>
                <el-tag size="small" class="badge-warn">预警</el-tag>
              </div>
            </div>
            <div class="segment-card__body">
              <div class="segment-field segment-field--otd">
                <label class="field-label">标准OTD（小时）</label>
                <el-input-number v-model="formData.unloadWaitDispatchOtd" :min="0" :step="0.5" :precision="1" controls-position="right" />
              </div>
              <div class="segment-field segment-field--warn">
                <label class="field-label">预警时效（小时）</label>
                <el-input-number v-model="formData.unloadWaitDispatchWarn" :min="0" :step="0.5" :precision="1" controls-position="right" />
              </div>
            </div>
          </div>

          <!-- 7. 分拨在途 → 已到达 -->
          <div class="segment-card">
            <div class="segment-card__header">
              <span class="segment-index">07</span>
              <span class="segment-title">分拨在途 → 已到达</span>
              <div class="segment-badges">
                <el-tag size="small" class="badge-otd">标准OTD</el-tag>
                <el-tag size="small" class="badge-warn">预警</el-tag>
              </div>
            </div>
            <div class="segment-card__body">
              <div class="segment-field segment-field--otd">
                <label class="field-label">标准OTD（小时）</label>
                <el-input-number v-model="formData.dispatchingOtd" :min="0" :step="0.5" :precision="1" controls-position="right" />
              </div>
              <div class="segment-field segment-field--warn">
                <label class="field-label">预警时效（小时）</label>
                <el-input-number v-model="formData.dispatchingWarn" :min="0" :step="0.5" :precision="1" controls-position="right" />
              </div>
            </div>
          </div>
        </div>

        <div class="form-actions">
          <el-button type="primary" size="large" @click="handleSubmit" :loading="submitting">
            保存配置
          </el-button>
        </div>
      </el-form>

      <el-empty v-else description="请选择一条线路" />
    </el-card>

    <!-- 批量导入对话框（保持不变） -->
    <el-dialog v-model="importDialogVisible" title="批量导入OTD配置" width="900px" @close="resetImport">
      <div v-if="importStep === 1">
        <el-alert type="info" :closable="false" style="margin-bottom: 16px">
          <template #title>
            请先导出配置模板，填写OTD时效和预警时效后上传
          </template>
        </el-alert>
        <el-upload
          ref="uploadRef"
          :auto-upload="false"
          :on-change="handleFileChange"
          :on-remove="handleFileRemove"
          :limit="1"
          accept=".xlsx,.xls"
          drag
        >
          <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
          <div class="el-upload__text">
            将文件拖到此处，或<em>点击上传</em>
          </div>
          <template #tip>
            <div class="el-upload__tip">
              支持 .xlsx / .xls 格式
            </div>
          </template>
        </el-upload>
      </div>

      <div v-else-if="importStep === 2">
        <el-alert type="info" :closable="false" style="margin-bottom: 16px">
          共 {{ importPreviewData.length }} 条配置，确认无误后点击"开始导入"
        </el-alert>
        <el-table :data="importPreviewData" border stripe max-height="400">
          <el-table-column prop="routeId" label="线路ID" width="80" />
          <el-table-column prop="brandName" label="品牌" width="100" />
          <el-table-column prop="originCity" label="出发地" width="100" />
          <el-table-column prop="destCity" label="目的地" width="100" />
          <el-table-column label="未出库" width="100" align="center">
            <template #default="{ row }">
              {{ row.notDepartedOtd || '-' }} / {{ row.notDepartedWarn || '-' }}
            </template>
          </el-table-column>
          <el-table-column label="集港在途" width="100" align="center">
            <template #default="{ row }">
              {{ row.toPortOtd || '-' }} / {{ row.toPortWarn || '-' }}
            </template>
          </el-table-column>
          <el-table-column label="待装船" width="100" align="center">
            <template #default="{ row }">
              {{ row.atPortWaitOtd || '-' }} / {{ row.atPortWaitWarn || '-' }}
            </template>
          </el-table-column>
          <el-table-column label="水运" width="100" align="center">
            <template #default="{ row }">
              {{ row.onSeaOtd || '-' }} / {{ row.onSeaWarn || '-' }}
            </template>
          </el-table-column>
          <el-table-column label="待卸船" width="100" align="center">
            <template #default="{ row }">
              {{ row.atDestWaitOtd || '-' }} / {{ row.atDestWaitWarn || '-' }}
            </template>
          </el-table-column>
          <el-table-column label="待分拨" width="100" align="center">
            <template #default="{ row }">
              {{ row.unloadWaitDispatchOtd || '-' }} / {{ row.unloadWaitDispatchWarn || '-' }}
            </template>
          </el-table-column>
          <el-table-column label="分拨在途" width="100" align="center">
            <template #default="{ row }">
              {{ row.dispatchingOtd || '-' }} / {{ row.dispatchingWarn || '-' }}
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div v-else-if="importStep === 3">
        <el-result
          :icon="importResult.failCount === 0 ? 'success' : 'warning'"
          title="导入完成"
        >
          <template #subtitle>
            <div class="import-result-detail">
              <p><el-tag type="success">成功 {{ importResult.successCount }} 条</el-tag></p>
              <p v-if="importResult.failCount > 0">
                <el-tag type="danger">失败 {{ importResult.failCount }} 条</el-tag>
              </p>
            </div>
          </template>
          <template #extra v-if="importResult.failCount > 0">
            <el-collapse style="max-width: 600px; margin: 0 auto">
              <el-collapse-item title="查看失败详情" name="1">
                <el-table :data="importResult.failDetails" border stripe size="small">
                  <el-table-column prop="value" label="失败原因" />
                </el-table>
              </el-collapse-item>
            </el-collapse>
          </template>
        </el-result>
      </div>

      <template #footer>
        <el-button @click="importDialogVisible = false">取消</el-button>
        <el-button v-if="importStep === 1" type="primary" @click="handlePreview" :loading="previewing">
          下一步
        </el-button>
        <el-button 
          v-if="importStep === 2" 
          type="primary" 
          @click="handleImport" 
          :loading="importing"
          :disabled="importPreviewData.length === 0"
        >
          开始导入（{{ importPreviewData.length }}条）
        </el-button>
        <el-button v-if="importStep === 3" type="primary" @click="handleImportFinish">
          完成
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Download, Upload, UploadFilled } from '@element-plus/icons-vue'
import { getBrandList } from '@/api/brand'
import { getRouteList, getRouteByBrand } from '@/api/route'
import { 
  getOtdConfigByRoute, saveOtdConfig, updateOtdConfig,
  exportOtdTemplate, previewOtdImport, batchImportOtd
} from '@/api/otdConfig'

// 品牌和线路列表
const brandList = ref([])
const routeList = ref([])
const filterBrandId = ref(null)
const selectedRouteId = ref(null)
const submitting = ref(false)

// 表单数据
const formData = reactive({
  id: null,
  routeId: null,
  notDepartedOtd: null,
  notDepartedWarn: null,
  toPortOtd: null,
  toPortWarn: null,
  atPortWaitOtd: null,
  atPortWaitWarn: null,
  onSeaOtd: null,
  onSeaWarn: null,
  atDestWaitOtd: null,
  atDestWaitWarn: null,
  unloadWaitDispatchOtd: null,
  unloadWaitDispatchWarn: null,
  dispatchingOtd: null,
  dispatchingWarn: null,
  isActive: 1
})

// 批量导入相关
const importDialogVisible = ref(false)
const importStep = ref(1)
const importFile = ref(null)
const importPreviewData = ref([])
const importResult = ref({ successCount: 0, failCount: 0, failDetails: [] })
const previewing = ref(false)
const importing = ref(false)

// 获取品牌名称
const getBrandName = (brandId) => {
  const brand = brandList.value.find(b => b.id === brandId)
  return brand?.brandName || '-'
}

// 加载品牌列表
const loadBrands = async () => {
  try {
    brandList.value = await getBrandList(true)
  } catch (error) {
    console.error('加载品牌失败:', error)
  }
}

// 加载线路列表
const loadRoutes = async (brandId) => {
  try {
    if (brandId) {
      routeList.value = await getRouteByBrand(brandId)
    } else {
      routeList.value = await getRouteList()
    }
  } catch (error) {
    console.error('加载线路失败:', error)
  }
}

// 品牌切换
const handleBrandChange = (brandId) => {
  selectedRouteId.value = null
  loadRoutes(brandId)
}

// 线路切换
const handleRouteChange = async (routeId) => {
  if (!routeId) return
  
  try {
    const config = await getOtdConfigByRoute(routeId)
    if (config) {
      Object.assign(formData, config)
    } else {
      resetForm(routeId)
    }
  } catch (error) {
    console.error('加载配置失败:', error)
    resetForm(routeId)
  }
}

// 重置表单
const resetForm = (routeId) => {
  formData.id = null
  formData.routeId = routeId
  formData.notDepartedOtd = null
  formData.notDepartedWarn = null
  formData.toPortOtd = null
  formData.toPortWarn = null
  formData.atPortWaitOtd = null
  formData.atPortWaitWarn = null
  formData.onSeaOtd = null
  formData.onSeaWarn = null
  formData.atDestWaitOtd = null
  formData.atDestWaitWarn = null
  formData.unloadWaitDispatchOtd = null
  formData.unloadWaitDispatchWarn = null
  formData.dispatchingOtd = null
  formData.dispatchingWarn = null
  formData.isActive = 1
}

// 保存配置
const handleSubmit = async () => {
  submitting.value = true
  try {
    if (formData.id) {
      await updateOtdConfig(formData)
      ElMessage.success('更新成功')
    } else {
      await saveOtdConfig(formData)
      ElMessage.success('保存成功')
    }
  } catch (error) {
    console.error('保存失败:', error)
    ElMessage.error('保存失败')
  } finally {
    submitting.value = false
  }
}

// 导出配置
const handleExport = async () => {
  if (!filterBrandId.value) {
    ElMessage.warning('请先选择品牌')
    return
  }
  try {
    const res = await exportOtdTemplate(filterBrandId.value)
    const blob = new Blob([res], { type: 'application/vnd.ms-excel' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    const brand = brandList.value.find(b => b.id === filterBrandId.value)
    link.download = `${brand?.brandName || '品牌'}_OTD配置模板.xlsx`
    link.click()
    window.URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch (error) {
    console.error('导出失败:', error)
    ElMessage.error('导出失败')
  }
}

// 打开导入对话框
const handleOpenImport = () => {
  if (!filterBrandId.value) {
    ElMessage.warning('请先选择品牌')
    return
  }
  importStep.value = 1
  importFile.value = null
  importPreviewData.value = []
  importDialogVisible.value = true
}

// 重置导入
const resetImport = () => {
  importStep.value = 1
  importFile.value = null
  importPreviewData.value = []
}

// 文件变化
const handleFileChange = (file) => {
  importFile.value = file.raw
}

// 文件移除
const handleFileRemove = () => {
  importFile.value = null
}

// 预览
const handlePreview = async () => {
  if (!importFile.value) {
    ElMessage.warning('请选择文件')
    return
  }

  previewing.value = true
  try {
    importPreviewData.value = await previewOtdImport(importFile.value)
    if (importPreviewData.value.length === 0) {
      ElMessage.warning('没有读取到有效数据')
      return
    }
    importStep.value = 2
  } catch (error) {
    console.error('预览失败:', error)
    ElMessage.error('预览失败，请检查文件格式')
  } finally {
    previewing.value = false
  }
}

// 导入
const handleImport = async () => {
  importing.value = true
  try {
    importResult.value = await batchImportOtd(importPreviewData.value)
    importStep.value = 3
  } catch (error) {
    console.error('导入失败:', error)
    ElMessage.error('导入失败')
  } finally {
    importing.value = false
  }
}

// 完成导入
const handleImportFinish = () => {
  importDialogVisible.value = false
  if (selectedRouteId.value) {
    handleRouteChange(selectedRouteId.value)
  }
  ElMessage.success('配置已更新')
}

onMounted(() => {
  loadBrands()
  loadRoutes()
})
</script>

<style scoped>
.otd-config {
  height: 100%;
}

/* ── Alert styling ── */
.config-alert {
  margin-bottom: 20px;
}

/* ── Segment Grid ── */
.segment-grid {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-top: 4px;
}

/* ── Segment Card ── */
.segment-card {
  border: var(--card-border);
  border-radius: var(--radius-md);
  background: var(--card-gradient);
  box-shadow: 0 2px 8px rgba(26, 65, 122, 0.06);
  overflow: hidden;
  transition: box-shadow var(--transition-fast);
}

.segment-card:hover {
  box-shadow: 0 4px 14px rgba(26, 65, 122, 0.10);
}

.segment-card__header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 18px;
  background: linear-gradient(180deg, rgba(255,255,255,0.98), rgba(248,251,255,0.98));
  border-bottom: 1px solid var(--color-divider);
}

.segment-index {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 6px;
  background: var(--el-color-primary-light-9);
  color: var(--color-primary);
  font-size: var(--font-size-xs);
  font-weight: 700;
  line-height: 1;
}

.segment-title {
  flex: 1;
  font-size: var(--font-size-base);
  font-weight: 600;
  color: var(--text-primary);
}

.segment-badges {
  display: flex;
  gap: 6px;
}

.badge-otd {
  --el-tag-bg-color: var(--color-primary-bg);
  --el-tag-border-color: var(--color-primary-lightest);
  --el-tag-text-color: var(--color-primary);
  --el-tag-hover-color: var(--color-primary);
}

.badge-warn {
  --el-tag-bg-color: var(--color-warning-bg);
  --el-tag-border-color: #f5e6c8;
  --el-tag-text-color: var(--color-warning);
  --el-tag-hover-color: var(--color-warning);
}

/* ── Field row ── */
.segment-card__body {
  display: flex;
  gap: 0;
  padding: 16px 18px;
}

.segment-field {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.segment-field + .segment-field {
  border-left: 1px solid var(--color-divider);
  padding-left: 18px;
  margin-left: 18px;
}

.field-label {
  font-size: var(--font-size-sm);
  font-weight: 500;
  color: var(--text-secondary);
}

/* OTD field — primary accent */
.segment-field--otd .field-label {
  color: var(--color-primary);
}

.segment-field--otd :deep(.el-input-number) {
  width: 100%;
}

.segment-field--otd :deep(.el-input-number .el-input__wrapper) {
  background-color: var(--color-info-bg);
}

/* Warn field — warning accent */
.segment-field--warn .field-label {
  color: var(--color-warning);
}

.segment-field--warn :deep(.el-input-number) {
  width: 100%;
}

.segment-field--warn :deep(.el-input-number .el-input__wrapper) {
  background-color: var(--color-warning-bg);
}

/* ── Submit button ── */
.form-actions {
  margin-top: 28px;
  padding-top: 20px;
  border-top: 1px solid var(--color-divider);
  text-align: center;
}

/* ── Import result ── */
.import-result-detail {
  display: flex;
  justify-content: center;
  gap: 20px;
  margin-top: 16px;
}

/* ── Responsive: stack fields on narrow viewports ── */
@media (max-width: 768px) {
  .segment-card__body {
    flex-direction: column;
    gap: 16px;
  }

  .segment-field + .segment-field {
    border-left: none;
    padding-left: 0;
    margin-left: 0;
    padding-top: 12px;
    border-top: 1px solid var(--color-divider);
  }
}
</style>
