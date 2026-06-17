<template>
  <div class="excel-mapping">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>Excel 字段映射配置</span>
          <div class="header-actions">
            <el-button type="primary" @click="handleCopy">
              <el-icon><CopyDocument /></el-icon>
              从其他品牌复制
            </el-button>
            <el-button type="success" @click="handleSave" :loading="saving">
              <el-icon><Check /></el-icon>
              保存
            </el-button>
          </div>
        </div>
      </template>

      <!-- 品牌筛选 -->
      <div class="filter-bar">
        <el-select
          v-model="selectedBrandId"
          placeholder="选择品牌"
          @change="handleBrandChange"
          style="width: 240px"
        >
          <el-option
            v-for="brand in brandList"
            :key="brand.id"
            :label="brand.brandName"
            :value="brand.id"
          />
          <el-option label="默认规则" :value="0" />
        </el-select>
        <span class="filter-hint">选择品牌后配置对应的字段映射规则</span>
      </div>

      <el-table :data="mappingList" border stripe v-loading="loading">
        <el-table-column prop="standardFieldName" label="标准字段" width="160" />
        <el-table-column prop="standardField" label="字段编码" width="140" />
        <el-table-column label="字段类型" width="110" align="center">
          <template #default="{ row }">
            <el-tag
              v-if="isDateField(row.standardField)"
              type="warning"
              effect="plain"
              size="small"
            >
              <el-icon style="vertical-align: -2px; margin-right: 2px"><Clock /></el-icon>
              日期类型
            </el-tag>
            <el-tag v-else type="info" effect="plain" size="small">
              文本类型
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Excel表头名称（多个用逗号分隔）" min-width="340">
          <template #default="{ row }">
            <el-input
              v-model="row.excelColumnNames"
              placeholder="如：车架号,VIN,vin"
              :class="{ 'field-input--date': isDateField(row.standardField) }"
            />
          </template>
        </el-table-column>
        <el-table-column label="时间格式" width="200">
          <template #default="{ row }">
            <el-select
              v-model="row.dateFormat"
              placeholder="选择格式"
              clearable
              filterable
              allow-create
              v-if="isDateField(row.standardField)"
            >
              <el-option label="yyyy-MM-dd HH:mm:ss" value="yyyy-MM-dd HH:mm:ss" />
              <el-option label="yyyy-MM-dd" value="yyyy-MM-dd" />
              <el-option label="yyyy/MM/dd HH:mm:ss" value="yyyy/MM/dd HH:mm:ss" />
              <el-option label="yyyy/MM/dd" value="yyyy/MM/dd" />
              <el-option label="yyyy年MM月dd日 HH:mm:ss" value="yyyy年MM月dd日 HH:mm:ss" />
              <el-option label="yyyy年MM月dd日" value="yyyy年MM月dd日" />
              <el-option label="MM/dd/yyyy HH:mm" value="MM/dd/yyyy HH:mm" />
              <el-option label="MM/dd/yyyy" value="MM/dd/yyyy" />
            </el-select>
            <span v-else class="date-placeholder">—</span>
          </template>
        </el-table-column>
        <el-table-column label="必填" width="70" align="center">
          <template #default="{ row }">
            <el-checkbox v-model="row.isRequired" :true-value="1" :false-value="0" />
          </template>
        </el-table-column>
        <el-table-column label="默认值" width="150">
          <template #default="{ row }">
            <el-input v-model="row.defaultValue" placeholder="可选" />
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 复制配置对话框 -->
    <el-dialog v-model="copyDialogVisible" title="从其他品牌复制配置" width="420px">
      <el-form label-width="80px">
        <el-form-item label="源品牌">
          <el-select v-model="copySourceBrandId" placeholder="选择源品牌" style="width: 100%">
            <el-option
              v-for="brand in brandList"
              :key="brand.id"
              :label="brand.brandName"
              :value="brand.id"
            />
            <el-option label="默认规则" :value="0" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="copyDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmCopy" :loading="copying">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { CopyDocument, Check, Clock } from '@element-plus/icons-vue'
import { getBrandList } from '@/api/brand'
import { getStandardFields, getMappingByBrand, batchSaveMapping, copyMapping } from '@/api/excelMapping'

// 品牌列表
const brandList = ref([])
// 当前选中的品牌
const selectedBrandId = ref(null)
// 标准字段列表
const standardFields = ref([])
// 映射配置列表
const mappingList = ref([])
// 加载状态
const loading = ref(false)
const saving = ref(false)

// 复制对话框
const copyDialogVisible = ref(false)
const copySourceBrandId = ref(null)
const copying = ref(false)

// 判断是否是时间字段
const isDateField = (fieldName) => {
  const field = standardFields.value.find(f => f.fieldName === fieldName)
  return field?.isDateType || false
}

// 初始化映射列表（基于标准字段）
const initMappingList = (existingMappings = []) => {
  const mappingMap = {}
  existingMappings.forEach(m => {
    mappingMap[m.standardField] = m
  })

  mappingList.value = standardFields.value.map(field => {
    const existing = mappingMap[field.fieldName]
    
    // 判断是否是时间字段，设置默认时间格式
    let defaultDateFormat = null
    if (field.isDateType) {
      defaultDateFormat = 'yyyy-MM-dd HH:mm:ss'
    }
    
    return {
      id: existing?.id || null,
      brandId: selectedBrandId.value === 0 ? null : selectedBrandId.value,
      standardField: field.fieldName,
      standardFieldName: field.fieldLabel,
      excelColumnNames: existing?.excelColumnNames || '',
      dateFormat: existing?.dateFormat || defaultDateFormat,
      isRequired: existing?.isRequired ?? 1,
      defaultValue: existing?.defaultValue || '',
      sortOrder: existing?.sortOrder || 0,
      isActive: existing?.isActive ?? 1
    }
  })
}

// 加载品牌列表
const loadBrands = async () => {
  try {
    brandList.value = await getBrandList()
  } catch (error) {
    console.error('加载品牌失败:', error)
  }
}

// 加载标准字段
const loadStandardFields = async () => {
  try {
    standardFields.value = await getStandardFields()
  } catch (error) {
    console.error('加载标准字段失败:', error)
  }
}

// 加载映射配置
const loadMapping = async () => {
  if (selectedBrandId.value === null) return
  
  loading.value = true
  try {
    const mappings = await getMappingByBrand(selectedBrandId.value)
    initMappingList(mappings)
  } catch (error) {
    console.error('加载配置失败:', error)
    initMappingList([])
  } finally {
    loading.value = false
  }
}

// 品牌切换
const handleBrandChange = () => {
  loadMapping()
}

// 保存配置
const handleSave = async () => {
  if (selectedBrandId.value === null) {
    ElMessage.warning('请先选择品牌')
    return
  }

  saving.value = true
  try {
    await batchSaveMapping({
      brandId: selectedBrandId.value === 0 ? null : selectedBrandId.value,
      mappings: mappingList.value
    })
    ElMessage.success('保存成功')
    loadMapping()
  } catch (error) {
    console.error('保存失败:', error)
  } finally {
    saving.value = false
  }
}

// 打开复制对话框
const handleCopy = () => {
  if (selectedBrandId.value === null) {
    ElMessage.warning('请先选择目标品牌')
    return
  }
  copySourceBrandId.value = null
  copyDialogVisible.value = true
}

// 确认复制
const confirmCopy = async () => {
  if (!copySourceBrandId.value && copySourceBrandId.value !== 0) {
    ElMessage.warning('请选择源品牌')
    return
  }

  copying.value = true
  try {
    const targetBrandId = selectedBrandId.value === 0 ? null : selectedBrandId.value
    const result = await copyMapping({
      sourceBrandId: copySourceBrandId.value,
      targetBrandId: targetBrandId
    })
    if (result.success) {
      ElMessage.success(result.message)
      copyDialogVisible.value = false
      loadMapping()
    } else {
      ElMessage.error(result.message)
    }
  } catch (error) {
    console.error('复制失败:', error)
  } finally {
    copying.value = false
  }
}

onMounted(() => {
  loadBrands()
  loadStandardFields()
})
</script>

<style scoped>
.excel-mapping {
  height: 100%;
}

/* ── filter hint text ── */
.filter-hint {
  font-size: var(--font-size-sm);
  color: var(--text-muted);
}

/* ── field type badges ── */
.field-input--date :deep(.el-input__wrapper) {
  background-color: var(--color-warning-bg);
}
</style>
