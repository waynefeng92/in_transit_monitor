<template>
  <div class="vehicle-drilldown">
    <div class="drilldown-header">
      <span class="drilldown-title">车辆明细</span>
      <el-button text circle @click="$emit('close')">
        <el-icon><Close /></el-icon>
      </el-button>
    </div>
    <el-table
      :data="vehicleList"
      border
      stripe
      size="small"
      max-height="360"
      empty-text="暂无数据"
    >
      <el-table-column prop="vin" label="车架号" min-width="180" />
      <el-table-column prop="brandName" label="品牌" min-width="100" />
      <el-table-column prop="transportStatusName" label="在途状态" min-width="120" />
      <el-table-column prop="monitorStatus" label="监控状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="monitorTagType(row.monitorStatus)" effect="dark" size="small">
            {{ monitorStatusLabel(row.monitorStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="路线" min-width="160">
        <template #default="{ row }">
          {{ row.originCity || '?' }} - {{ row.destCity || '?' }}
        </template>
      </el-table-column>
      <el-table-column label="订单释放时间" min-width="170">
        <template #default="{ row }">
          {{ formatTime(row.orderReleaseTime) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100" align="center" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link size="small" @click="handleRowClick(row)">
            详情
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <div style="margin-top: 10px; display: flex; justify-content: flex-end;">
      <el-pagination
        v-if="total > 0"
        :current-page="props.currentPage"
        :page-size="pageSize"
        :total="total"
        :page-sizes="[20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { Close } from '@element-plus/icons-vue'

const props = defineProps({
  vehicleList: {
    type: Array,
    default: () => []
  },
  total: {
    type: Number,
    default: 0
  },
  currentPage: {
    type: Number,
    default: 1
  }
})

const emit = defineEmits(['close', 'page-change'])

const router = useRouter()

const monitorTagType = (status) => {
  if (status === 'NORMAL') return 'success'
  if (status === 'WARN') return 'warning'
  if (status === 'OVERDUE') return 'danger'
  return 'info'
}

const monitorStatusLabel = (status) => {
  if (status === 'NORMAL') return '正常'
  if (status === 'WARN') return '预警'
  if (status === 'OVERDUE') return '超期'
  return status || '未知'
}

const formatTime = (isoString) => {
  if (!isoString) return '—'
  return isoString.replace('T', ' ').substring(0, 19)
}

const pageSize = ref(20)

const handlePageChange = (newPage) => {
  emit('page-change', newPage, pageSize.value)
}

const handleSizeChange = (newSize) => {
  pageSize.value = newSize
  emit('page-change', 1, newSize)
}

const handleRowClick = (row) => {
  router.push('/vehicle-detail?vin=' + row.vin)
}
</script>

<style scoped>
.vehicle-drilldown {
  margin-top: 12px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  padding: 12px;
  background: var(--el-fill-color-lighter);
}
.drilldown-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.drilldown-title {
  font-weight: 600;
  font-size: 14px;
  color: var(--el-text-color-primary);
}
.vehicle-drilldown {
  animation: drilldownFadeIn 0.3s ease;
}
@keyframes drilldownFadeIn {
  from { opacity: 0; transform: translateY(-8px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
