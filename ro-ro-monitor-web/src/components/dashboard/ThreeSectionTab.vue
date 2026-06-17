<template>
  <div class="three-section-tab">
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
          <div class="panel-title">{{ drillDownSection ? `${drillDownSection} - 品牌监控图表` : '三段监控状态图表' }}</div>
          <div class="panel-subtitle">{{ drillDownSection ? '按品牌查看监控状态分布' : '按段查看正常、预警、超期车辆分布' }}</div>
        </div>
        <div class="panel-actions">
          <el-tag effect="plain" round class="panel-tag">三段监控</el-tag>
          <el-button v-if="drillDownSection" text @click="$emit('backToSection')">
            <el-icon><ArrowLeft /></el-icon>
            返回段级视图
          </el-button>
          <el-button type="primary" @click="$emit('refresh')" :loading="loading">
            <el-icon><Refresh /></el-icon>
            刷新数据
          </el-button>
        </div>
      </div>

      <div class="chart-filters">
        <el-select
          v-if="drillDownSection"
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
              <div class="chart-card__subtitle">按段查看正常、预警、超期数量</div>
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
              <div class="chart-card__subtitle">{{ drillDownSection ? '按品牌与监控状态查看数量' : '按段查看正常、预警、超期数量' }}</div>
            </div>
            <div class="chart-container">
              <StackedBarChart
                ref="sectionBarChartRef"
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
                :chartData="pieChartData"
                :selected-alert-status="selectedAlertStatus"
                height="320px"
              />
            </div>
          </div>
        </div>
      </template>

      <div style="margin-top: 20px;">
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px;">
          <div>
            <div class="panel-title">详细数据</div>
            <div class="panel-subtitle" style="margin-top: 4px;">{{ drillDownSection ? '展示各品牌监控明细' : '展示各段监控明细' }}</div>
          </div>
          <el-button text class="detail-toggle" @click="showDetail = !showDetail">
            {{ showDetail ? '收起明细' : '展开明细' }}
          </el-button>
        </div>
        <el-collapse-transition>
          <div v-show="showDetail" class="detail-table-wrap">
            <el-table :data="tableData" border stripe size="small" max-height="420">
              <el-table-column prop="brand" :label="drillDownSection ? '品牌' : '段名称'" min-width="120" fixed />
              <el-table-column prop="transportStatus" label="监控类型" min-width="150" />
              <el-table-column prop="normal" label="正常" width="100" align="center">
                <template #default="{ row }">
                  <el-tag type="success" effect="dark">{{ row.normal }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="warn" label="预警" width="100" align="center">
                <template #default="{ row }">
                  <el-tag type="warning" effect="dark">{{ row.warn }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="overdue" label="超期" width="100" align="center">
                <template #default="{ row }">
                  <el-tag type="danger" effect="dark">{{ row.overdue }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="合计" width="100" align="center">
                <template #default="{ row }">
                  {{ row.normal + row.warn + row.overdue }}
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-collapse-transition>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, ref, watch, nextTick } from 'vue'
import { Refresh, ArrowLeft } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import StackedBarChart from '@/components/StackedBarChart.vue'
import StatusPieChart from '@/components/StatusPieChart.vue'

const props = defineProps({
  sectionChartData: { type: Array, default: () => [] },
  sectionBrandChartData: { type: Array, default: () => [] },
  displaySummary: { type: Object, default: () => ({ normal: 0, warn: 0, overdue: 0 }) },
  loading: { type: Boolean, default: false },
  initialLoading: { type: Boolean, default: true },
  selectedBrand: { type: String, default: '' },
  selectedAlertStatus: { type: String, default: '' },
  drillDownSection: { type: String, default: null }
})

const emit = defineEmits([
  'update:selectedBrand',
  'update:selectedAlertStatus',
  'resetFilters',
  'refresh',
  'drilldown',
  'backToSection'
])

const showDetail = ref(false)
const sectionBarChartRef = ref(null)
let chartClickHandler = null

const monitorStatusOptions = ['正常', '预警', '超期']
const statusFieldMap = { 正常: 'normal', 预警: 'warn', 超期: 'overdue' }

const chartDisplayData = computed(() => {
  if (props.drillDownSection) {
    return props.sectionBrandChartData.map(d => ({
      brand: d.brand,
      transportStatus: `${props.drillDownSection}监控`,
      normal: Number(d.normal || 0),
      warn: Number(d.warn || 0),
      overdue: Number(d.overdue || 0)
    })).filter(item => {
      const matchBrand = !props.selectedBrand || item.brand === props.selectedBrand
      const metricField = props.selectedAlertStatus ? statusFieldMap[props.selectedAlertStatus] : ''
      const matchAlertStatus = !metricField || Number(item[metricField] || 0) > 0
      return matchBrand && matchAlertStatus
    })
  }
  return props.sectionChartData
    .filter(item => {
      const matchBrand = !props.selectedBrand || item.brand === props.selectedBrand
      const metricField = props.selectedAlertStatus ? statusFieldMap[props.selectedAlertStatus] : ''
      const matchAlertStatus = !metricField || Number(item[metricField] || 0) > 0
      return matchBrand && matchAlertStatus
    })
    .map(d => ({
    brand: d.sectionName,
    transportStatus: '三段监控',
    normal: Number(d.normal || 0),
    warn: Number(d.warn || 0),
    overdue: Number(d.overdue || 0)
  }))
})

const pieChartData = computed(() => chartDisplayData.value)

const brandOptions = computed(() => {
  if (!props.drillDownSection) return []
  return Array.from(new Set(props.sectionBrandChartData.map(item => item.brand).filter(Boolean))).sort((a, b) =>
    a.localeCompare(b, 'zh-Hans-CN')
  )
})

const activeChartStatuses = computed(() => {
  return props.selectedAlertStatus ? [props.selectedAlertStatus] : monitorStatusOptions
})

const summaryCards = computed(() => [
      { key: 'normal', label: '正常', value: props.displaySummary.total > 0 ? props.displaySummary.normal : 0, meta: '处于标准时效内' },
      { key: 'warn', label: '预警', value: props.displaySummary.total > 0 ? props.displaySummary.warn : 0, meta: '接近时效阈值' },
      { key: 'overdue', label: '超期', value: props.displaySummary.total > 0 ? props.displaySummary.overdue : 0, meta: '已超过监控时效' },
      { key: 'total', label: '在途总数', value: props.displaySummary.total || 0, meta: '当前在途车辆总量' }
])

const barSectionTitle = computed(() => {
  if (props.drillDownSection) {
    const brandLabel = props.selectedBrand || '品牌'
    return `${props.drillDownSection} - ${brandLabel} × 监控分布`
  }
  return '三段监控状态分布'
})

const pieSectionTitle = computed(() => {
  if (props.drillDownSection) {
    return props.selectedAlertStatus
      ? `${props.drillDownSection} - ${props.selectedAlertStatus}占比`
      : `${props.drillDownSection} - 监控状态占比`
  }
  return '三段监控状态占比'
})

const pieSectionSubtitle = computed(() => {
  if (props.drillDownSection) {
    return props.selectedAlertStatus
      ? `基于当前筛选统计${props.selectedAlertStatus}下各状态数量比例`
      : '基于当前筛选统计正常、预警、超期数量比例'
  }
  return '各段正常、预警、超期数量比例'
})

const tableData = computed(() => {
  if (props.drillDownSection) {
    return props.sectionBrandChartData.filter(item => {
      if (props.selectedBrand && item.brand !== props.selectedBrand) return false
      if (props.selectedAlertStatus) {
        const field = statusFieldMap[props.selectedAlertStatus]
        if (!Number(item[field] || 0)) return false
      }
      return true
    }).map(d => ({
      brand: d.brand,
      transportStatus: `${props.drillDownSection}监控`,
      normal: Number(d.normal || 0),
      warn: Number(d.warn || 0),
      overdue: Number(d.overdue || 0)
    }))
  }
  return props.sectionChartData.map(d => ({
    brand: d.sectionName,
    transportStatus: '三段监控',
    normal: Number(d.normal || 0),
    warn: Number(d.warn || 0),
    overdue: Number(d.overdue || 0)
  }))
})

const attachChartClick = () => {
  if (!sectionBarChartRef.value) return
  const chartDiv = sectionBarChartRef.value.$el
  if (!chartDiv) return
  const chartInstance = echarts.getInstanceByDom(chartDiv)
  if (!chartInstance) return
  if (chartClickHandler) {
    chartInstance.off('click', chartClickHandler)
  }
  chartClickHandler = (params) => {
    if (props.drillDownSection) return
    if (params.componentType !== 'series') return
    const name = params.name
    const sectionName = name.split('\n')[0]
    if (['前段', '中段', '后段'].includes(sectionName)) {
      emit('drilldown', sectionName)
    }
  }
  chartInstance.on('click', chartClickHandler)
}

watch(() => chartDisplayData.value, () => {
  nextTick(() => {
    setTimeout(attachChartClick, 300)
  })
})
</script>

<style scoped>
.three-section-tab {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
</style>
