<template>
  <div class="overall-tab">
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
        <div v-for="item in summaryCards" :key="item.key"
          class="summary-card" :class="`summary-card-${item.key}`">
          <div class="summary-card__label">{{ item.label }}</div>
          <div class="summary-card__value" :class="`summary-card__value-${item.key}`">{{ item.value }}</div>
          <div class="summary-card__meta">{{ item.meta }}</div>
        </div>
      </template>
    </section>

    <section class="dashboard-panel">
      <div class="panel-header">
        <div>
          <div class="panel-title">品牌 × 整段监控状态</div>
          <div class="panel-subtitle">按品牌统计整段正常、预警、超期车辆分布</div>
        </div>
        <div class="panel-actions">
          <el-tag effect="plain" round class="panel-tag">整段监控</el-tag>
          <el-button type="primary" @click="$emit('refresh')" :loading="loading">
            <el-icon><Refresh /></el-icon>
            刷新数据
          </el-button>
        </div>
      </div>

      <div class="chart-filters">
        <el-select
          :model-value="selectedBrand"
          @update:model-value="$emit('update:selectedBrand', $event)"
          clearable filterable placeholder="筛选品牌" class="chart-filter"
        >
          <el-option v-for="brand in brandOptions" :key="brand" :label="brand" :value="brand" />
        </el-select>
        <el-select
          :model-value="selectedAlertStatus"
          @update:model-value="$emit('update:selectedAlertStatus', $event)"
          clearable filterable placeholder="筛选监控状态" class="chart-filter"
        >
          <el-option v-for="status in monitorStatusOptions" :key="status" :label="status" :value="status" />
        </el-select>
        <el-button text class="filter-reset" @click="$emit('resetFilters')">重置</el-button>
      </div>

      <template v-if="initialLoading">
        <div class="chart-layout">
          <div class="chart-card chart-card-bar">
            <div class="chart-card__header">
              <div class="chart-card__title">{{ barSectionTitle }}</div>
              <div class="chart-card__subtitle">按品牌查看整段监控数量</div>
            </div>
            <el-skeleton animated>
              <el-skeleton-item variant="rect" style="width: 100%; height: 420px; border-radius: 12px;" />
            </el-skeleton>
          </div>
          <div class="chart-card chart-card-pie">
            <div class="chart-card__header">
              <div class="chart-card__title">{{ pieSectionTitle }}</div>
              <div class="chart-card__subtitle">{{ pieSectionSubtitle }}</div>
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
              <div class="chart-card__title">{{ barSectionTitle }}</div>
              <div class="chart-card__subtitle">按品牌查看整段监控数量</div>
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
              <div class="chart-card__title">{{ pieSectionTitle }}</div>
              <div class="chart-card__subtitle">{{ pieSectionSubtitle }}</div>
            </div>
            <div class="pie-section__chart">
              <StatusPieChart
                :chartData="chartDisplayData"
                :selected-alert-status="selectedAlertStatus"
                height="320px"
              />
            </div>
          </div>
        </div>
      </template>

      <div style="margin-top: 20px;">
        <el-table :data="tableData" border stripe size="small" max-height="420">
          <el-table-column prop="brand" label="品牌" min-width="120" />
          <el-table-column prop="normal" label="正常" width="120" align="center">
            <template #default="{ row }">
              <el-tag type="success" effect="dark">{{ row.normal }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="warn" label="预警" width="120" align="center">
            <template #default="{ row }">
              <el-tag type="warning" effect="dark">{{ row.warn }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="overdue" label="超期" width="120" align="center">
            <template #default="{ row }">
              <el-tag type="danger" effect="dark">{{ row.overdue }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="合计" width="120" align="center">
            <template #default="{ row }">
              {{ (row.normal || 0) + (row.warn || 0) + (row.overdue || 0) }}
            </template>
          </el-table-column>
        </el-table>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import StackedBarChart from '@/components/StackedBarChart.vue'
import StatusPieChart from '@/components/StatusPieChart.vue'

const props = defineProps({
  chartData: { type: Array, default: () => [] },
  displaySummary: { type: Object, default: () => ({ overallNormal: 0, overallWarn: 0, overallOverdue: 0, total: 0 }) },
  loading: { type: Boolean, default: false },
  initialLoading: { type: Boolean, default: true },
  selectedBrand: { type: String, default: '' },
  selectedAlertStatus: { type: String, default: '' }
})

defineEmits(['update:selectedBrand', 'update:selectedAlertStatus', 'resetFilters', 'refresh'])

const monitorStatusOptions = ['正常', '预警', '超期']
const statusFieldMap = { 正常: 'normal', 预警: 'warn', 超期: 'overdue' }

const brandOptions = computed(() => {
  return Array.from(new Set(props.chartData.map(item => item.brand).filter(Boolean))).sort((a, b) =>
    a.localeCompare(b, 'zh-Hans-CN')
  )
})

const filteredChartData = computed(() => {
  return props.chartData.filter(item => {
    const matchBrand = !props.selectedBrand || item.brand === props.selectedBrand
    const metricField = props.selectedAlertStatus ? statusFieldMap[props.selectedAlertStatus] : ''
    const matchAlertStatus = !metricField || Number(item[metricField] || 0) > 0
    return matchBrand && matchAlertStatus
  })
})

const chartDisplayData = computed(() => {
  return filteredChartData.value.map(d => ({
    brand: d.brand,
    transportStatus: '整段监控',
    normal: d.normal || 0,
    warn: d.warn || 0,
    overdue: d.overdue || 0
  }))
})

const tableData = computed(() => {
  return filteredChartData.value
})

const activeChartStatuses = computed(() => {
  return props.selectedAlertStatus ? [props.selectedAlertStatus] : monitorStatusOptions
})

const summaryCards = computed(() => [
      { key: 'normal', label: '正常', value: props.displaySummary.overallNormal, meta: '处于标准时效内' },
      { key: 'warn', label: '预警', value: props.displaySummary.overallWarn, meta: '接近时效阈值' },
      { key: 'overdue', label: '超期', value: props.displaySummary.overallOverdue, meta: '已超过监控时效' },
      { key: 'total', label: '在途总数', value: props.displaySummary.total, meta: '当前在途车辆总量' }
])

const barSectionTitle = computed(() => {
  const brandLabel = props.selectedBrand || '品牌'
  return `${brandLabel} × 整段监控分布`
})

const pieSectionTitle = computed(() => {
  const brandLabel = props.selectedBrand || '品牌'
  return props.selectedAlertStatus
    ? `${brandLabel} ${props.selectedAlertStatus}占比`
    : `${brandLabel} 监控状态占比`
})

const pieSectionSubtitle = computed(() => {
  return props.selectedAlertStatus
    ? `基于当前筛选结果统计${props.selectedBrand || '该品牌'}${props.selectedAlertStatus}下各状态数量比例`
    : `基于当前筛选结果统计${props.selectedBrand || '该品牌'}正常、预警、超期数量比例`
})
</script>

<style scoped>
.overall-tab {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
</style>
