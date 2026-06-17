<template>
  <div class="route-manage">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>
            线路管理
            <el-tag size="small" type="info" effect="plain" style="margin-left: 10px; vertical-align: middle">
              {{ filteredTableData.length }}
            </el-tag>
          </span>
          <div class="header-actions">
            <el-switch
              v-model="showDisabled"
              active-text="显示已禁用"
              inactive-text="仅显示启用"
              @change="loadData"
              style="margin-right: 16px"
            />
            <el-button @click="handleDownloadTemplate">
              <el-icon><Download /></el-icon>
              下载模板
            </el-button>
            <el-button type="success" @click="handleOpenImport">
              <el-icon><Upload /></el-icon>
              批量导入
            </el-button>
            <el-button type="primary" @click="handleAdd">
              <el-icon><Plus /></el-icon>
              新增线路
            </el-button>
          </div>
        </div>
      </template>

      <!-- 筛选 -->
      <div class="filter-bar">
        <el-select
          v-model="filterBrandId"
          placeholder="按品牌筛选"
          clearable
          @change="loadData"
          style="width: 200px"
        >
          <el-option
            v-for="brand in brandList"
            :key="brand.id"
            :label="brand.brandName"
            :value="brand.id"
          />
        </el-select>
        <el-input
          v-model="routeSearch"
          placeholder="搜索出发地 / 目的地..."
          clearable
          style="width: 260px"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-button type="primary" @click="showBatchEdit = true" :disabled="selectedRoutes.length === 0">
          <el-icon><Edit /></el-icon>
          批量编辑（{{ selectedRoutes.length }}）
        </el-button>
      </div>

      <el-table :data="filteredTableData" border stripe v-loading="loading" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="45" />
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="品牌" width="120">
          <template #default="{ row }">
            {{ getBrandName(row.brandId) }}
          </template>
        </el-table-column>
        <el-table-column prop="originCity" label="出发地" width="120" />
        <el-table-column label="出发港" width="120">
          <template #default="{ row }">
            {{ getPortName(row.originPortId) }}
          </template>
        </el-table-column>
        <el-table-column label="目的港" width="120">
          <template #default="{ row }">
            {{ getPortName(row.destPortId) }}
          </template>
        </el-table-column>
        <el-table-column prop="destCity" label="目的地" width="120" />
        <el-table-column prop="isActive" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.isActive === 1 ? 'success' : 'danger'">
              {{ row.isActive === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" text @click="handleEdit(row)">编辑</el-button>
            <el-button
              v-if="row.isActive === 1"
              type="warning"
              size="small"
              text
              @click="handleToggleStatus(row)"
            >禁用</el-button>
            <el-button
              v-else
              type="success"
              size="small"
              text
              @click="handleToggleStatus(row)"
            >启用</el-button>
            <el-button type="danger" size="small" text @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && filteredTableData.length === 0" description="暂无线路数据" />
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
      @close="resetForm"
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="品牌" prop="brandId">
              <el-select v-model="formData.brandId" placeholder="请选择品牌" style="width: 100%">
                <el-option
                  v-for="brand in activeBrandList"
                  :key="brand.id"
                  :label="brand.brandName"
                  :value="brand.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="isActive">
              <el-radio-group v-model="formData.isActive">
                <el-radio :label="1">启用</el-radio>
                <el-radio :label="0">禁用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="出发地" prop="originCity">
              <el-input v-model="formData.originCity" placeholder="如：上海" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="出发港" prop="originPortId">
              <el-select v-model="formData.originPortId" placeholder="请选择出发港" style="width: 100%">
                <el-option
                  v-for="port in activePortList"
                  :key="port.id"
                  :label="port.portName"
                  :value="port.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="目的港" prop="destPortId">
              <el-select v-model="formData.destPortId" placeholder="请选择目的港" style="width: 100%">
                <el-option
                  v-for="port in activePortList"
                  :key="port.id"
                  :label="port.portName"
                  :value="port.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="目的地" prop="destCity">
              <el-input v-model="formData.destCity" placeholder="如：大连" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>

    <!-- 批量导入对话框 -->
    <el-dialog v-model="importDialogVisible" title="批量导入线路" width="900px" @close="resetImport">
      <!-- 步骤指示器 -->
      <el-steps :active="importStep - 1" align-center finish-status="success" style="margin-bottom: 28px">
        <el-step title="上传文件" />
        <el-step title="预览数据" />
        <el-step title="导入结果" />
      </el-steps>

      <!-- 步骤1：上传文件 -->
      <div v-if="importStep === 1">
        <el-alert type="info" :closable="false" style="margin-bottom: 16px">
          <template #title>
            请先下载模板，按模板格式填写数据后上传
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
          <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
        </el-upload>
      </div>

      <!-- 步骤2：预览数据 -->
      <div v-else-if="importStep === 2">
        <!-- 缺失提醒 -->
        <el-alert
          v-if="previewResult.missingBrands?.length || previewResult.missingPorts?.length"
          type="warning"
          :closable="false"
          style="margin-bottom: 16px"
        >
          <template #title>
            <div>
              <span v-if="previewResult.missingBrands?.length">
                <strong>缺失品牌：</strong>{{ previewResult.missingBrands.join('、') }}
              </span>
              <span v-if="previewResult.missingPorts?.length" style="margin-left: 20px">
                <strong>缺失港口：</strong>{{ previewResult.missingPorts.join('、') }}
              </span>
            </div>
          </template>
          <div style="margin-top: 8px">
            <el-button type="primary" size="small" @click="goToBrandManage">
              <el-icon><Setting /></el-icon>
              去维护品牌
            </el-button>
            <el-button type="primary" size="small" @click="goToPortManage">
              <el-icon><Setting /></el-icon>
              去维护港口
            </el-button>
          </div>
        </el-alert>

        <!-- 统计 -->
        <div class="import-stats">
          <span>总计 {{ previewResult.rows?.length || 0 }} 条</span>
          <span class="success">可导入 {{ previewResult.canImportCount || 0 }} 条</span>
          <span class="error" v-if="previewResult.cannotImportCount > 0">
            需维护 {{ previewResult.cannotImportCount }} 条
          </span>
        </div>

        <!-- 数据表格 -->
        <el-table :data="previewResult.rows" border stripe max-height="400" style="margin-top: 12px">
          <el-table-column prop="rowNum" label="行号" width="60" />
          <el-table-column prop="brandName" label="品牌" width="120">
            <template #default="{ row }">
              <span :class="{ 'text-error': !row.brandExists && row.brandName }">
                {{ row.brandName || '-' }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="originCity" label="出发地" width="100" />
          <el-table-column prop="originPortName" label="出发港" width="120">
            <template #default="{ row }">
              <span :class="{ 'text-error': !row.originPortExists && row.originPortName }">
                {{ row.originPortName || '-' }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="destPortName" label="目的港" width="120">
            <template #default="{ row }">
              <span :class="{ 'text-error': !row.destPortExists && row.destPortName }">
                {{ row.destPortName || '-' }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="destCity" label="目的地" width="100" />
          <el-table-column label="状态" width="80" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.canImport" type="success" size="small">可导入</el-tag>
              <el-tag v-else type="danger" size="small">需维护</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 步骤3：导入结果 -->
      <div v-else-if="importStep === 3">
        <el-result
          :icon="importResult.failCount === 0 ? 'success' : 'warning'"
          :title="`导入完成`"
        >
          <template #subtitle>
            <div class="import-result-detail">
              <p><el-tag type="success">成功 {{ importResult.successCount }} 条</el-tag></p>
              <p v-if="importResult.skipCount > 0">
                <el-tag type="info">跳过 {{ importResult.skipCount }} 条（已存在）</el-tag>
              </p>
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
          :disabled="previewResult.canImportCount === 0"
        >
          开始导入（{{ previewResult.canImportCount }}条）
        </el-button>
        <el-button v-if="importStep === 3" type="primary" @click="handleImportFinish">
          完成
        </el-button>
      </template>
    </el-dialog>

    <!-- 批量编辑对话框 -->
    <el-dialog v-model="showBatchEdit" title="批量编辑线路" width="500px" @close="resetBatchForm">
      <el-form :model="batchEditForm" label-width="100px">
        <el-form-item label="出发地">
          <el-input v-model="batchEditForm.originCity" placeholder="留空则不修改" />
        </el-form-item>
        <el-form-item label="出发港">
          <el-select v-model="batchEditForm.originPortId" clearable placeholder="留空则不修改" style="width:100%">
            <el-option v-for="p in portList" :key="p.id" :label="p.portName" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="目的港">
          <el-select v-model="batchEditForm.destPortId" clearable placeholder="留空则不修改" style="width:100%">
            <el-option v-for="p in portList" :key="p.id" :label="p.portName" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="目的地">
          <el-input v-model="batchEditForm.destCity" placeholder="留空则不修改" />
        </el-form-item>
        <el-form-item>
          <span style="color:var(--text-muted);font-size:13px">
            已选中 {{ selectedRoutes.length }} 条线路，仅填写需要修改的字段，留空则不修改
          </span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showBatchEdit = false">取消</el-button>
        <el-button type="primary" :loading="batchSubmitting" @click="handleBatchEdit">确认修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Download, Upload, UploadFilled, Setting, Search, Edit } from '@element-plus/icons-vue'
import { getBrandList } from '@/api/brand'
import { getPortList } from '@/api/port'
import { 
  getRouteList, saveRoute, updateRoute, deleteRoute,
  downloadTemplate, previewImport, batchImport, batchUpdateRoutes 
} from '@/api/route'

const router = useRouter()

// 表格数据
const tableData = ref([])
const loading = ref(false)
const showDisabled = ref(false)
const filterBrandId = ref(null)

// 线路关键字搜索
const routeSearch = ref('')

// 品牌和港口列表
const brandList = ref([])
const activeBrandList = computed(() => brandList.value.filter(b => b.isActive === 1))
const portList = ref([])
const activePortList = computed(() => portList.value.filter(p => p.isActive === 1))

// 批量编辑
const selectedRoutes = ref([])
const showBatchEdit = ref(false)
const batchEditForm = reactive({
  originCity: '',
  originPortId: null,
  destPortId: null,
  destCity: ''
})
const batchSubmitting = ref(false)

// 筛选后的表格数据
const filteredTableData = computed(() => {
  let data = tableData.value
  if (filterBrandId.value) {
    data = data.filter(item => item.brandId === filterBrandId.value)
  }
  const kw = routeSearch.value?.toLowerCase().trim()
  if (kw) {
    data = data.filter(item =>
      item.originCity?.toLowerCase().includes(kw) ||
      item.destCity?.toLowerCase().includes(kw)
    )
  }
  return data
})

// 对话框
const dialogVisible = ref(false)
const dialogTitle = computed(() => formData.id ? '编辑线路' : '新增线路')
const submitting = ref(false)
const formRef = ref(null)
const formData = reactive({
  id: null,
  brandId: null,
  originCity: '',
  originPortId: null,
  destPortId: null,
  destCity: '',
  isActive: 1
})
const formRules = {
  brandId: [{ required: true, message: '请选择品牌', trigger: 'change' }],
  originCity: [{ required: true, message: '请输入出发地', trigger: 'blur' }],
  originPortId: [{ required: true, message: '请选择出发港', trigger: 'change' }],
  destPortId: [{ required: true, message: '请选择目的港', trigger: 'change' }],
  destCity: [{ required: true, message: '请输入目的地', trigger: 'blur' }]
}

// 批量导入
const importDialogVisible = ref(false)
const importStep = ref(1)
const importFile = ref(null)
const previewResult = ref({ rows: [], missingBrands: [], missingPorts: [], canImportCount: 0, cannotImportCount: 0 })
const importResult = ref({ successCount: 0, failCount: 0, skipCount: 0, failDetails: [] })
const previewing = ref(false)
const importing = ref(false)

// 获取品牌名称
const getBrandName = (brandId) => {
  const brand = brandList.value.find(b => b.id === brandId)
  return brand?.brandName || '-'
}

// 获取港口名称
const getPortName = (portId) => {
  const port = portList.value.find(p => p.id === portId)
  return port?.portName || '-'
}

// 加载品牌
const loadBrands = async () => {
  try {
    brandList.value = await getBrandList(true)
  } catch (error) {
    console.error('加载品牌失败:', error)
  }
}

// 加载港口
const loadPorts = async () => {
  try {
    portList.value = await getPortList(true)
  } catch (error) {
    console.error('加载港口失败:', error)
  }
}

// 加载线路
const loadData = async () => {
  loading.value = true
  try {
    tableData.value = await getRouteList(showDisabled.value)
  } catch (error) {
    console.error('加载失败:', error)
  } finally {
    loading.value = false
  }
}

// 新增
const handleAdd = () => {
  resetForm()
  dialogVisible.value = true
}

// 编辑
const handleEdit = (row) => {
  Object.assign(formData, row)
  dialogVisible.value = true
}

// 切换状态
const handleToggleStatus = async (row) => {
  const action = row.isActive === 1 ? '禁用' : '启用'
  try {
    await ElMessageBox.confirm(`确定要${action}该线路吗？`, '提示', { type: 'warning' })
    const updatedRow = { ...row, isActive: row.isActive === 1 ? 0 : 1 }
    await updateRoute(updatedRow)
    ElMessage.success(`${action}成功`)
    loadData()
  } catch (error) {
    if (error !== 'cancel') console.error('操作失败:', error)
  }
}

// 删除
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该线路吗？删除后可在"显示已禁用"中找回。', '提示', { type: 'warning' })
    await deleteRoute(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (error) {
    if (error !== 'cancel') console.error('删除失败:', error)
  }
}

// 提交
const handleSubmit = async () => {
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  submitting.value = true
  try {
    if (formData.id) {
      await updateRoute(formData)
      ElMessage.success('更新成功')
    } else {
      await saveRoute(formData)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadData()
  } catch (error) {
    console.error('保存失败:', error)
  } finally {
    submitting.value = false
  }
}

// 重置表单
const resetForm = () => {
  formData.id = null
  formData.brandId = null
  formData.originCity = ''
  formData.originPortId = null
  formData.destPortId = null
  formData.destCity = ''
  formData.isActive = 1
  formRef.value?.clearValidate()
}

// 批量编辑选择
const handleSelectionChange = (rows) => {
  selectedRoutes.value = rows
}

const resetBatchForm = () => {
  batchEditForm.originCity = ''
  batchEditForm.originPortId = null
  batchEditForm.destPortId = null
  batchEditForm.destCity = ''
}

const handleBatchEdit = async () => {
  const updates = selectedRoutes.value.map(route => {
    const update = { id: route.id }
    if (batchEditForm.originCity) update.originCity = batchEditForm.originCity
    if (batchEditForm.originPortId) update.originPortId = batchEditForm.originPortId
    if (batchEditForm.destPortId) update.destPortId = batchEditForm.destPortId
    if (batchEditForm.destCity) update.destCity = batchEditForm.destCity
    return update
  })

  batchSubmitting.value = true
  try {
    const res = await batchUpdateRoutes(updates)
    ElMessage.success(`成功更新 ${res.successCount} 条，失败 ${res.failCount} 条`)
    showBatchEdit.value = false
    loadData()
  } catch (error) {
    console.error('批量编辑失败:', error)
    ElMessage.error('批量编辑失败')
  } finally {
    batchSubmitting.value = false
  }
}

// 下载模板
const handleDownloadTemplate = async () => {
  try {
    const res = await downloadTemplate()
    const blob = new Blob([res], { type: 'application/vnd.ms-excel' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    
    // 从响应头获取文件名，如果获取不到就用默认的
    const contentDisposition = res.headers?.['content-disposition']
    let fileName = '线路批量导入模板.xlsx'
    if (contentDisposition) {
      const match = contentDisposition.match(/filename\*=UTF-8''(.+)/)
      if (match) {
        fileName = decodeURIComponent(match[1])
      }
    }
    link.download = fileName
    
    link.click()
    window.URL.revokeObjectURL(url)
    ElMessage.success('模板下载成功')
  } catch (error) {
    console.error('下载模板失败:', error)
    ElMessage.error('下载模板失败')
  }
}

// 打开导入对话框
const handleOpenImport = () => {
  importStep.value = 1
  importFile.value = null
  previewResult.value = { rows: [], missingBrands: [], missingPorts: [], canImportCount: 0, cannotImportCount: 0 }
  importDialogVisible.value = true
}

// 重置导入
const resetImport = () => {
  importStep.value = 1
  importFile.value = null
  previewResult.value = { rows: [], missingBrands: [], missingPorts: [], canImportCount: 0, cannotImportCount: 0 }
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
    previewResult.value = await previewImport(importFile.value)
    importStep.value = 2
    
    if (previewResult.value.cannotImportCount > 0) {
      ElMessage.warning(`有 ${previewResult.value.cannotImportCount} 条数据需要先维护品牌或港口`)
    }
  } catch (error) {
    console.error('预览失败:', error)
    ElMessage.error('预览失败，请检查文件格式')
  } finally {
    previewing.value = false
  }
}

// 跳转到品牌管理
const goToBrandManage = () => {
  importDialogVisible.value = false
  router.push('/brand')
}

// 跳转到港口管理
const goToPortManage = () => {
  importDialogVisible.value = false
  router.push('/port')
}

// 导入
const handleImport = async () => {
  const canImportRows = previewResult.value.rows.filter(row => row.canImport)
  if (canImportRows.length === 0) {
    ElMessage.warning('没有可导入的数据')
    return
  }

  importing.value = true
  try {
    const importData = canImportRows.map(row => ({
      brandName: row.brandName,
      originCity: row.originCity,
      originPortName: row.originPortName,
      destPortName: row.destPortName,
      destCity: row.destCity
    }))
    importResult.value = await batchImport(importData)
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
  loadData()
  ElMessage.success('线路数据已更新')
}

onMounted(() => {
  Promise.all([loadBrands(), loadPorts()]).then(() => loadData())
})
</script>

<style scoped>
.route-manage {
  height: 100%;
}

.filter-bar {
  margin-bottom: 18px;
}

.el-table {
  border-radius: var(--radius-md);
  overflow: hidden;
}

.el-table :deep(th.el-table__cell) {
  background-color: var(--el-table-header-bg-color);
  font-weight: 600;
  color: var(--text-primary);
}

:deep(.el-empty) {
  padding: 40px 0;
}

.import-stats {
  display: flex;
  gap: 20px;
  padding: 10px 14px;
  background-color: var(--bg-light);
  border-radius: var(--radius-sm);
}
.import-stats .success {
  color: var(--color-success);
  font-weight: 600;
}
.import-stats .error {
  color: var(--color-danger);
  font-weight: 600;
}
.import-result-detail {
  display: flex;
  justify-content: center;
  gap: 20px;
  margin-top: 16px;
}

/* Step indicator refinements */
:deep(.el-step__title) {
  font-size: var(--font-size-sm);
}
</style>