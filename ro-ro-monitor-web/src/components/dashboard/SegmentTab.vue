<template>
  <div class="segment-tab">
    <section class="dashboard-summary">
      <template v-if="initialLoading">
        <div v-for="i in 4" :key="i" class="summary-card">
          <el-skeleton animated>
            <template #template>
              <div style="padding: 22px 24px;">
                <el-skeleton-item variant="text" style="width: 40%; height: 18px;" />
                <el-skeleton-item variant="text" style="width: 60%; height: 44px; margin: 14px 0 18px;" />
                <el-skeleton-item variant="text" style="width: 50%; height: 14px;" />
              </div>
            </template>
          </el-skeleton>
        </div>
      </template>
      <template v-else>
        <div
          v-for="item in summaryCards"
          :key="item.key"
          class="summary-card"
          :class="`summary-card-${item.key}`"
        >
          <div class="summary-card__label">{{ item.label }}</div>
          <div class="summary-card__value" :class="`summary-card__value-${item.key}`">{{ item.value }}</div>
          <div class="summary-card__meta">{{ item.meta }}</div>
        </div>
      </template>
    </section>

    <section class="dashboard-panel">
      <div class="panel-header">
        <div>
          <div class="panel-title">分段监控状态图表</div>
          <div class="panel-subtitle">按品牌和在途状态查看正常、预警、超期车辆分布</div>
        </div>
        <div class="panel-actions">
          <el-tag effect="plain" round class="panel-tag">在途监控</el-tag>
          <el-button type="primary" @click="$emit('refresh')" :loading="loading">
            <el-icon><Refresh /></el-icon>
            刷新数据
          </el-button>
        </div>
      </div>
      <template v-if="initialLoading">
        <div class="chart-layout">
          <div class="chart-card chart-card-bar">
            <div class="chart-card__header">
              <div class="chart-card__title">分段监控状态分布</div>
              <div class="chart-card__subtitle">按品牌与在途状态查看正常、预警、超期数量</div>
            </div>
            <el-skeleton animated>
              <el-skeleton-item variant="rect" style="width: 100%; height: 420px; border-radius: 12px;" />
            </el-skeleton>
          </div>
          <div class="chart-card chart-card-pie">
            <div class="chart-card__header">
              <div class="chart-card__title">分段监控状态占比</div>
              <div class="chart-card__subtitle">各品牌正常、预警、超期数量比例</div>
            </div>
            <el-skeleton animated>
              <el-skeleton-item variant="rect" style="width: 100%; height: 300px; border-radius: 12px;" />
            </el-skeleton>
          </div>
        </div>
      </template>
      <template v-else>
        <div class="chart-layout">
          <div class="chart-card chart-card-bar">
            <div class="chart-card__header">
              <div class="chart-card__title">分段监控状态分布</div>
              <div class="chart-card__subtitle">按品牌与在途状态查看正常、预警、超期数量</div>
            </div>
            <div class="chart-container">
              <StackedBarChart
                :chartData="chartDisplayData"
                :active-statuses="activeChartStatuses"
                height="460px"
              />
            </div>
          </div>
          <div class="chart-card chart-card-pie">
            <div class="chart-card__header">
              <div class="chart-card__title">分段监控状态占比</div>
              <div class="chart-card__subtitle">各品牌正常、预警、超期数量比例</div>
            </div>
            <div class="pie-section__chart">
              <StatusPieChart
                :chartData="chartData"
                height="320px"
              />
            </div>
          </div>
        </div>
      </template>
    </section>

    <section class="dashboard-panel dashboard-panel-detail">
      <div class="panel-header">
        <div>
          <div class="panel-title">详细数据</div>
          <div class="panel-subtitle">展示各品牌监控明细</div>
        </div>
        <el-button text class="detail-toggle" @click="showDetail = !showDetail">
          {{ showDetail ? '收起明细' : '展开明细' }}
        </el-button>
      </div>
      <el-collapse-transition>
        <div v-show="showDetail" class="detail-table-wrap">
          <el-table :data="chartData" border stripe size="small" max-height="420">
            <el-table-column prop="brand" label="品牌" min-width="120" fixed />
            <el-table-column prop="transportStatus" label="在途状态" min-width="150" />
            <el-table-column prop="normal" label="正常" width="100" align="center">
              <template #default="{ row }">
                <el-tag type="success" effect="dark"
                  :class="{ 'clickable-tag': true }"
                  @click.stop="handleCellClick(row, 'normal')">{{ row.normal }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="warn" label="预警" width="100" align="center">
              <template #default="{ row }">
                <el-tag type="warning" effect="dark"
                  :class="{ 'clickable-tag': true }"
                  @click.stop="handleCellClick(row, 'warn')">{{ row.warn }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="overdue" label="超期" width="100" align="center">
              <template #default="{ row }">
                <el-tag type="danger" effect="dark"
                  :class="{ 'clickable-tag': true }"
                  @click.stop="handleCellClick(row, 'overdue')">{{ row.overdue }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="合计" width="100" align="center">
              <template #default="{ row }">
                {{ row.normal + row.warn + row.overdue }}
              </template>
            </el-table-column>
          </el-table>
          <div v-show="activeDrillCell" class="drilldown-area">
            <el-skeleton v-if="drilldownLoading" animated :rows="3" />
            <template v-else>
              <VehicleDrilldownTable
                v-if="drilldownVehicleList.length"
                :vehicleList="drilldownVehicleList"
                :total="drilldownTotal"
                @close="handleCloseDrilldown"
                @page-change="handleDrilldownPageChange"
              />
              <el-empty v-else description="暂无匹配车辆" />
            </template>
          </div>
        </div>
      </el-collapse-transition>
    </section>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import StackedBarChart from '@/components/StackedBarChart.vue'
import StatusPieChart from '@/components/StatusPieChart.vue'
import { getVehicleDetails } from '@/api/chart'
import VehicleDrilldownTable from '@/components/dashboard/VehicleDrilldownTable.vue'

const props = defineProps({
  chartData: { type: Array, default: () => [] },
  displaySummary: { type: Object, default: () => ({ normal: 0, warn: 0, overdue: 0, total: 0 }) },
  loading: { type: Boolean, default: false },
  initialLoading: { type: Boolean, default: true }
})

defineEmits(['refresh'])

const showDetail = ref(true)
const drilldownVehicleList = ref([])
const drilldownLoading = ref(false)
const activeDrillCell = ref(null)
const currentDrillRequestKey = ref(null)
const drilldownTotal = ref(0)
const drilldownPageSize = ref(20)
const drilldownCurrentParams = ref(null)

const monitorStatusOptions = ['正常', '预警', '超期']
const statusFieldMap = { 正常: 'normal', 预警: 'warn', 超期: 'overdue' }

const statusFieldMap_en = { normal: 'NORMAL', warn: 'WARN', overdue: 'OVERDUE' }

const handleCellClick = async (row, columnKey) => {
  const cellKey = `${row.brand}_${row.transportStatus}_${columnKey}`

  // toggle off if same cell
  if (activeDrillCell.value === cellKey) {
    drilldownVehicleList.value = []
    activeDrillCell.value = null
    return
  }

  activeDrillCell.value = cellKey
  currentDrillRequestKey.value = cellKey
  drilldownLoading.value = true
  drilldownVehicleList.value = []
  try {
    const params = {
      type: 'segment',
      brandName: row.brand,
      transportStatusName: row.transportStatus,
      monitorStatus: statusFieldMap_en[columnKey]
    }
    drilldownCurrentParams.value = { ...params }
    const paginatedParams = { ...params, page: 1, size: drilldownPageSize.value }
    const data = await getVehicleDetails(paginatedParams)
    if (currentDrillRequestKey.value === cellKey) {
      drilldownVehicleList.value = Array.isArray(data?.records) ? data.records : []
      drilldownTotal.value = data?.total || 0
    }
  } catch (e) {
    console.error('获取车辆明细失败', e)
    if (currentDrillRequestKey.value === cellKey) {
      drilldownVehicleList.value = []
    }
  } finally {
    if (currentDrillRequestKey.value === cellKey) {
      drilldownLoading.value = false
    }
  }
}

const handleCloseDrilldown = () => {
  drilldownVehicleList.value = []
  activeDrillCell.value = null
  currentDrillRequestKey.value = null
  drilldownTotal.value = 0
}

const handleDrilldownPageChange = async (newPage, newSize) => {
  drilldownLoading.value = true
  if (newSize) drilldownPageSize.value = newSize
  const cellKey = activeDrillCell.value
  currentDrillRequestKey.value = cellKey
  try {
    const paginatedParams = { ...drilldownCurrentParams.value, page: newPage, size: newSize || drilldownPageSize.value }
    const data = await getVehicleDetails(paginatedParams)
    if (currentDrillRequestKey.value === cellKey) {
      drilldownVehicleList.value = Array.isArray(data?.records) ? data.records : []
      drilldownTotal.value = data?.total || 0
    }
  } catch (e) {
    console.error('获取车辆明细失败', e)
    if (currentDrillRequestKey.value === cellKey) {
      drilldownVehicleList.value = []
    }
  } finally {
    if (currentDrillRequestKey.value === cellKey) {
      drilldownLoading.value = false
    }
  }
}

const chartDisplayData = computed(() => {
  return (props.chartData || []).map(d => ({...d}))
})

const activeChartStatuses = computed(() => monitorStatusOptions)

const summaryCards = computed(() => [
  { key: 'normal', label: '正常', value: props.displaySummary.normal, meta: '处于标准时效内' },
  { key: 'warn', label: '预警', value: props.displaySummary.warn, meta: '接近时效阈值' },
  { key: 'overdue', label: '超期', value: props.displaySummary.overdue, meta: '已超过监控时效' },
  { key: 'total', label: '在途总数', value: props.displaySummary.total, meta: '当前在途车辆总量' }
])
</script>

<style scoped>
.segment-tab {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.clickable-tag {
  cursor: pointer;
  transition: opacity 0.2s;
}
.clickable-tag:hover {
  opacity: 0.7;
}
.drilldown-area {
  margin-top: 12px;
}
</style>
