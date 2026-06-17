<template>
  <div class="port-manage">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>
            港口管理
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
            <el-button type="primary" @click="handleAdd">
              <el-icon><Plus /></el-icon>
              新增港口
            </el-button>
          </div>
        </div>
      </template>

      <!-- 搜索筛选 -->
      <div class="filter-bar">
        <el-input
          v-model="portSearch"
          placeholder="搜索港口名称..."
          clearable
          style="width: 260px"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
      </div>

      <el-table :data="filteredTableData" border stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="portName" label="港口名称" width="150" />
        <el-table-column prop="portCode" label="港口代码" width="120">
          <template #default="{ row }">
            {{ row.portCode || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="isActive" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.isActive === 1 ? 'success' : 'danger'">
              {{ row.isActive === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" align="center">
          <template #default="{ row }">
            <el-button type="primary" size="small" text @click="handleEdit(row)">
              编辑
            </el-button>
            <el-button
              v-if="row.isActive === 1"
              type="warning"
              size="small"
              text
              @click="handleToggleStatus(row)"
            >
              禁用
            </el-button>
            <el-button
              v-else
              type="success"
              size="small"
              text
              @click="handleToggleStatus(row)"
            >
              启用
            </el-button>
            <el-button type="danger" size="small" text @click="handleDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && filteredTableData.length === 0" description="暂无港口数据" />
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="500px"
      @close="resetForm"
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="100px"
      >
        <el-form-item label="港口名称" prop="portName">
          <el-input v-model="formData.portName" placeholder="如：上海港" />
        </el-form-item>
        <el-form-item label="港口代码" prop="portCode">
          <el-input v-model="formData.portCode" placeholder="如：CNSHA（可选）" />
        </el-form-item>
        <el-form-item label="状态" prop="isActive">
          <el-radio-group v-model="formData.isActive">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import { getPortList, savePort, updatePort, deletePort } from '@/api/port'

const tableData = ref([])
const loading = ref(false)
const showDisabled = ref(false)

// 港口名称搜索
const portSearch = ref('')
const filteredTableData = computed(() => {
  const kw = portSearch.value?.toLowerCase().trim()
  if (!kw) return tableData.value
  return tableData.value.filter(item =>
    item.portName?.toLowerCase().includes(kw)
  )
})

const dialogVisible = ref(false)
const dialogTitle = computed(() => formData.id ? '编辑港口' : '新增港口')
const submitting = ref(false)

const formRef = ref(null)
const formData = reactive({
  id: null,
  portName: '',
  portCode: '',
  isActive: 1
})

const formRules = {
  portName: [
    { required: true, message: '请输入港口名称', trigger: 'blur' }
  ]
}

const loadData = async () => {
  loading.value = true
  try {
    tableData.value = await getPortList(showDisabled.value)
  } catch (error) {
    console.error('加载失败:', error)
  } finally {
    loading.value = false
  }
}

const handleAdd = () => {
  resetForm()
  dialogVisible.value = true
}

const handleEdit = (row) => {
  Object.assign(formData, row)
  dialogVisible.value = true
}

const handleToggleStatus = async (row) => {
  const action = row.isActive === 1 ? '禁用' : '启用'
  try {
    await ElMessageBox.confirm(`确定要${action}港口「${row.portName}」吗？`, '提示', {
      type: 'warning'
    })
    const updatedRow = { ...row, isActive: row.isActive === 1 ? 0 : 1 }
    await updatePort(updatedRow)
    ElMessage.success(`${action}成功`)
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('操作失败:', error)
    }
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要删除港口「${row.portName}」吗？删除后可在"显示已禁用"中找回。`, '提示', {
      type: 'warning'
    })
    await deletePort(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
    }
  }
}

const handleSubmit = async () => {
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  submitting.value = true
  try {
    if (formData.id) {
      await updatePort(formData)
      ElMessage.success('更新成功')
    } else {
      await savePort(formData)
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

const resetForm = () => {
  formData.id = null
  formData.portName = ''
  formData.portCode = ''
  formData.isActive = 1
  formRef.value?.clearValidate()
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.port-manage {
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
</style>