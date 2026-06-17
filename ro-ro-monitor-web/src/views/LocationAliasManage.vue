<template>
  <div class="location-alias-manage">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>
            地点别名
            <el-tag size="small" type="info" effect="plain" style="margin-left: 10px; vertical-align: middle">
              {{ filteredData.length }}
            </el-tag>
          </span>
          <div class="header-actions">
            <el-button type="primary" @click="handleAdd">
              <el-icon><Plus /></el-icon>
              新增
            </el-button>
          </div>
        </div>
      </template>

      <!-- 搜索筛选 -->
      <div class="filter-bar">
        <el-input
          v-model="searchText"
          placeholder="搜索规范名或别名..."
          clearable
          style="width: 260px"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
      </div>

      <el-table :data="filteredData" border stripe v-loading="loading">
        <el-table-column type="index" label="#" width="60" />
        <el-table-column prop="standardName" label="规范名" min-width="150" />
        <el-table-column prop="alias" label="别名" min-width="150" />
        <el-table-column label="操作" width="160" align="center">
          <template #default="{ row }">
            <el-button size="small" text type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button size="small" text type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && filteredData.length === 0" description="暂无数据" />
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑别名' : '新增别名'"
      width="450px"
      @close="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="规范名" prop="standardName">
          <el-input v-model="form.standardName" placeholder="如：杭州市、南沙港" />
        </el-form-item>
        <el-form-item label="别名" prop="alias">
          <el-input v-model="form.alias" placeholder="如：杭州、南沙码头" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import { getLocationAliasList, saveLocationAlias, updateLocationAlias, deleteLocationAlias } from '@/api/locationAlias'

const loading = ref(false)
const list = ref([])
const searchText = ref('')
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref(null)
const form = reactive({ id: null, standardName: '', alias: '' })

const rules = {
  standardName: [{ required: true, message: '请输入规范名', trigger: 'blur' }],
  alias: [{ required: true, message: '请输入别名', trigger: 'blur' }]
}

const filteredData = computed(() => {
  if (!searchText.value) return list.value
  const q = searchText.value.toLowerCase()
  return list.value.filter(item =>
    (item.standardName && item.standardName.toLowerCase().includes(q)) ||
    (item.alias && item.alias.toLowerCase().includes(q))
  )
})

const loadData = async () => {
  loading.value = true
  try {
    const res = await getLocationAliasList()
    list.value = res || []
  } catch {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

const handleAdd = () => {
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

const handleEdit = (row) => {
  isEdit.value = true
  Object.assign(form, row)
  dialogVisible.value = true
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确认删除该别名？', '确认', { type: 'warning' })
    await deleteLocationAlias(row.id)
    ElMessage.success('已删除')
    loadData()
  } catch {
    // cancelled or error
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
    if (isEdit.value) {
      await updateLocationAlias({ id: form.id, standardName: form.standardName, alias: form.alias })
      ElMessage.success('已更新')
    } else {
      await saveLocationAlias({ standardName: form.standardName, alias: form.alias })
      ElMessage.success('已新增')
    }
    dialogVisible.value = false
    loadData()
  } catch {
    ElMessage.error('操作失败')
  } finally {
    submitting.value = false
  }
}

const resetForm = () => {
  form.id = null
  form.standardName = ''
  form.alias = ''
  formRef.value?.clearValidate()
}

onMounted(loadData)
</script>

<style scoped>
.location-alias-manage {
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
