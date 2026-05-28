<template>
  <div class="brand-manage">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>品牌管理</span>
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
              新增品牌
            </el-button>
          </div>
        </div>
      </template>

      <el-table :data="tableData" border stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="brandName" label="品牌名称" width="150" />
        <el-table-column prop="wmiCode" label="WMI代码" width="120">
          <template #default="{ row }">
            {{ row.wmiCode || '-' }}
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
        <el-form-item label="品牌名称" prop="brandName">
          <el-input v-model="formData.brandName" placeholder="如：上汽大众" />
        </el-form-item>
        <el-form-item label="WMI代码" prop="wmiCode">
          <el-input v-model="formData.wmiCode" placeholder="如：SVW（可选）" maxlength="3" />
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
import { Plus } from '@element-plus/icons-vue'
import { getBrandList, saveBrand, updateBrand, deleteBrand } from '@/api/brand'

// 表格数据
const tableData = ref([])
const loading = ref(false)

// 是否显示已禁用的品牌
const showDisabled = ref(false)

// 对话框
const dialogVisible = ref(false)
const dialogTitle = computed(() => formData.id ? '编辑品牌' : '新增品牌')
const submitting = ref(false)

// 表单
const formRef = ref(null)
const formData = reactive({
  id: null,
  brandName: '',
  wmiCode: '',
  isActive: 1
})

const formRules = {
  brandName: [
    { required: true, message: '请输入品牌名称', trigger: 'blur' }
  ]
}

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    tableData.value = await getBrandList(showDisabled.value)
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

// 切换启用/禁用状态
const handleToggleStatus = async (row) => {
  const action = row.isActive === 1 ? '禁用' : '启用'
  try {
    await ElMessageBox.confirm(`确定要${action}品牌「${row.brandName}」吗？`, '提示', {
      type: 'warning'
    })
    
    // 切换状态
    const updatedRow = { ...row, isActive: row.isActive === 1 ? 0 : 1 }
    await updateBrand(updatedRow)
    ElMessage.success(`${action}成功`)
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('操作失败:', error)
    }
  }
}

// 删除（软删除）
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要删除品牌「${row.brandName}」吗？删除后可在"显示已禁用"中找回。`, '提示', {
      type: 'warning'
    })
    await deleteBrand(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
    }
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
      await updateBrand(formData)
      ElMessage.success('更新成功')
    } else {
      await saveBrand(formData)
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
  formData.brandName = ''
  formData.wmiCode = ''
  formData.isActive = 1
  formRef.value?.clearValidate()
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.brand-manage {
  height: 100%;
}
</style>