<template>
  <div class="arrived-dashboard">
    <section class="dashboard-monitor-tabs">
      <div class="tabs-with-desc">
        <el-tabs v-model="activeMonitorTab" @tab-change="handleTabChange">
          <el-tab-pane label="整段监控" name="overall" />
          <el-tab-pane label="三段监控" name="three-section" />
          <el-tab-pane label="分段监控" name="segment" />
        </el-tabs>
        <span class="tabs-description">{{ tabDescription }}</span>
      </div>
    </section>

    <section class="dashboard-time-filter">
      <div class="time-filter__label">到达时间</div>
      <el-date-picker
        v-model="dateRange"
        type="daterange"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        value-format="YYYY-MM-DDTHH:mm:ss"
        class="time-filter__picker"
      />
      <el-select
        v-model="selectedBrand"
        clearable
        filterable
        placeholder="筛选品牌"
        class="chart-filter"
      >
        <el-option
          v-for="brand in brandOptions"
          :key="brand"
          :label="brand"
          :value="brand"
        />
      </el-select>
      <el-select
        v-if="activeMonitorTab !== 'overall'"
        v-model="selectedSection"
        clearable
        filterable
        :placeholder="activeMonitorTab === 'three-section' ? '三段选择' : '分段选择'"
        class="chart-filter"
      >
        <el-option
          v-for="seg in sectionOptions"
          :key="seg"
          :label="seg"
          :value="seg"
        />
      </el-select>
      <el-button type="primary" @click="loadData">查询</el-button>
      <el-button text class="filter-reset" @click="resetFilters">重置</el-button>
    </section>

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

    <div v-if="!hasData && !initialLoading" class="empty-data">
      <el-empty description="暂无数据" />
    </div>

    <template v-else-if="hasData || initialLoading">
      <div v-if="activeMonitorTab === 'segment'" class="tab-view">
        <section class="dashboard-panel">
          <div class="panel-header">
            <div>
              <div class="panel-title">分段监控视图</div>
              <div class="panel-subtitle">按分段查看到达车辆监控状态分布</div>
            </div>
            <div class="panel-actions">
              <el-tag effect="plain" round class="panel-tag">分段监控</el-tag>
              <el-button type="primary" @click="loadData" :loading="loading">
                <el-icon><Refresh /></el-icon>
                刷新数据
              </el-button>
            </div>
          </div>
          <ArrivedSegmentTab
            :chart-data="chartData"
            :loading="loading"
            :brand="selectedBrand"
            :selectedSection="selectedSection"
          />
        </section>
      </div>

      <div v-if="activeMonitorTab === 'three-section'" class="tab-view">
        <section class="dashboard-panel">
          <div class="panel-header">
            <div>
              <div class="panel-title">三段监控视图</div>
              <div class="panel-subtitle">按前段、中段、后段查看到达车辆监控状态分布</div>
            </div>
            <div class="panel-actions">
              <el-tag effect="plain" round class="panel-tag">三段监控</el-tag>
              <el-button type="primary" @click="loadData" :loading="loading">
                <el-icon><Refresh /></el-icon>
                刷新数据
              </el-button>
            </div>
          </div>
          <ArrivedThreeSectionTab
            :chart-data="chartData"
            :loading="loading"
            :brand="selectedBrand"
            :selectedSection="sectionFilter"
            @drilldown="handleDrilldown"
            @back="handleBack"
          />
        </section>
      </div>

      <div v-if="activeMonitorTab === 'overall'" class="tab-view">
        <section class="dashboard-panel">
          <div class="panel-header">
            <div>
              <div class="panel-title">整段监控视图</div>
              <div class="panel-subtitle">按整段查看到达车辆监控状态分布</div>
            </div>
            <div class="panel-actions">
              <el-tag effect="plain" round class="panel-tag">整段监控</el-tag>
              <el-button type="primary" @click="loadData" :loading="loading">
                <el-icon><Refresh /></el-icon>
                刷新数据
              </el-button>
            </div>
          </div>
          <ArrivedOverallTab
            :chart-data="chartData"
            :loading="loading"
            :brand="selectedBrand"
          />
        </section>
      </div>
    </template>

    <section class="dashboard-panel dashboard-panel-statistics">
      <div class="card-header">
        <div>
          <div class="panel-title">到达统计</div>
          <div class="panel-subtitle">按周期统计到达车辆效率分布</div>
        </div>
        <div class="header-actions">
          <el-radio-group v-model="statisticsPeriod" size="small" @change="handleStatisticsPeriodChange">
            <el-radio-button label="week">周</el-radio-button>
            <el-radio-button label="month">月</el-radio-button>
          </el-radio-group>
          <el-button type="primary" @click="loadStatistics(statisticsPeriod)" :loading="statisticsLoading">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
        </div>
      </div>
      <ArrivedStatisticsPanel
        :statistics-data="statisticsData"
        :loading="statisticsLoading"
        :period="statisticsPeriod"
      />
    </section>
  </div>
</template>

<script setup>
import { computed, ref, watch, onMounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import request from '@/api/request'
import { getBrandList } from '@/api/brand'
import ArrivedOverallTab from '@/components/ArrivedOverallTab.vue'
import ArrivedSegmentTab from '@/components/ArrivedSegmentTab.vue'
import ArrivedThreeSectionTab from '@/components/ArrivedThreeSectionTab.vue'
import ArrivedStatisticsPanel from '@/components/ArrivedStatisticsPanel.vue'

// State
const activeMonitorTab = ref('overall')
const dateRange = ref([
  new Date(Date.now() - 30 * 24 * 60 * 60 * 1000).toISOString().substring(0, 10) + 'T00:00:00',
  new Date().toISOString().substring(0, 10) + 'T00:00:00'
])
const selectedBrand = ref('')
const selectedSection = ref('')
const loading = ref(false)
const initialLoading = ref(true)

const summary = ref({
  efficientCount: 0,
  normalCount: 0,
  delayedCount: 0,
  totalCount: 0,
  avgEfficiency: 0
})

const chartData = ref([])
const brandOptions = ref([])

const sectionOptions = computed(() => {
  if (activeMonitorTab.value === 'three-section') {
    return ['前段', '中段', '后段']
  }
  if (activeMonitorTab.value === 'segment') {
    return ['未出库', '集港在途', '已集港待装船', '水运在途', '已到港待卸船', '已卸船待分拨', '分拨在途']
  }
  return []
})

const tabDescription = computed(() => {
  if (activeMonitorTab.value === 'overall') return '按整段运输链效率（高效/正常/延迟）'
  if (activeMonitorTab.value === 'three-section') return '按前段（客户开单-装船）/中段（水路起运-到港卸船）/后段（卸船完成-到店）效率'
  if (activeMonitorTab.value === 'segment') return '按7个在途分段分别评估效率：未出库、集港在途、已集港待装船、水运在途、已到港待卸船、已卸船待分拨、分拨在途'
  return ''
})

const currentSectionName = ref('')
const statisticsPeriod = ref('week')
const statisticsData = ref([])
const statisticsLoading = ref(false)

// Computed
const summaryCards = computed(() => [
  {
    key: 'efficient',
    label: '高效',
    value: summary.value.efficientCount || 0,
    meta: '高效到达车辆数量'
  },
  {
    key: 'normal',
    label: '正常',
    value: summary.value.normalCount || 0,
    meta: '正常到达车辆数量'
  },
  {
    key: 'delayed',
    label: '延迟',
    value: summary.value.delayedCount || 0,
    meta: '延迟到达车辆数量'
  },
  {
    key: 'total',
    label: '总数',
    value: summary.value.totalCount || 0,
    meta: '总到达车辆数量'
  }
])

const sectionFilter = computed(() => selectedSection.value || currentSectionName.value)

const hasData = computed(() => {
  const s = summary.value
  return (s.efficientCount || 0) + (s.normalCount || 0) + (s.delayedCount || 0) + (s.totalCount || 0) > 0
})

// Methods
const getTimeParams = () => {
  if (!dateRange.value) return {}
  const [start, end] = dateRange.value
  if (!start || !end) return {}
  const fmtEnd = end.substring ? end : end.toISOString().substring(0, 19)
  return {
    startTime: start.substring ? start : start.toISOString().substring(0, 19),
    endTime: fmtEnd.substring(0, 10) + 'T23:59:59'
  }
}

const loadSummary = async () => {
  try {
    const params = {
      ...getTimeParams(),
      ...(selectedBrand.value ? { brandName: selectedBrand.value } : {}),
      ...(selectedSection.value ? { sectionName: selectedSection.value }
          : currentSectionName.value ? { sectionName: currentSectionName.value } : {}),
    }
    const res = await request.get('/arrived/summary', { params })
    summary.value = res || { efficientCount: 0, normalCount: 0, delayedCount: 0, totalCount: 0, avgEfficiency: 0 }
  } catch (error) {
    console.error('加载到达汇总失败:', error)
    summary.value = { efficientCount: 0, normalCount: 0, delayedCount: 0, totalCount: 0, avgEfficiency: 0 }
  }
}

const loadChartData = async (type, section) => {
  try {
    const params = {
      ...getTimeParams(),
      type: type || activeMonitorTab.value,
      ...(selectedBrand.value ? { brandName: selectedBrand.value } : {})
    }
    if (section || selectedSection.value || currentSectionName.value) {
      params.sectionName = section || selectedSection.value || currentSectionName.value
    }
    const res = await request.get('/arrived/chart', { params })
    chartData.value = res || null
  } catch (error) {
    console.error('加载到达图表数据失败:', error)
    chartData.value = null
  }
}

const loadStatistics = async (period) => {
  statisticsLoading.value = true
  try {
    const res = await request.get('/arrived/statistics', {
      params: {
        period: period || statisticsPeriod.value,
        ...getTimeParams(),
        ...(selectedBrand.value ? { brandName: selectedBrand.value } : {})
      }
    })
    statisticsData.value = res || []
  } catch (error) {
    console.error('加载到达统计数据失败:', error)
    statisticsData.value = []
  } finally {
    statisticsLoading.value = false
  }
}

const loadBrandOptions = async () => {
  try {
    const list = await getBrandList()
    brandOptions.value = (list || [])
      .filter(b => b.isActive !== 0)
      .map(b => b.brandName)
      .sort((a, b) => a.localeCompare(b, 'zh-Hans-CN'))
  } catch (e) {
    console.error('加载品牌列表失败', e)
  }
}

const loadData = async () => {
  loading.value = true
  initialLoading.value = true
  try {
    await Promise.all([
      loadSummary(),
      loadChartData(activeMonitorTab.value)
    ])
  } finally {
    loading.value = false
    initialLoading.value = false
  }
}

const handleTabChange = (tabName) => {
  activeMonitorTab.value = tabName
  selectedSection.value = ''
  loadChartData(tabName)
}

const handleStatisticsPeriodChange = (period) => {
  loadStatistics(period)
}

const handleDrilldown = (section) => {
  currentSectionName.value = section
  selectedSection.value = ''
  loadChartData(activeMonitorTab.value, section)
}

const handleBack = () => {
  currentSectionName.value = ''
  selectedSection.value = ''
  loadChartData(activeMonitorTab.value)
}

const resetFilters = () => {
  dateRange.value = null
  selectedBrand.value = ''
  selectedSection.value = ''
  currentSectionName.value = ''
  loadData()
}

onMounted(() => {
  loadBrandOptions()
  loadData()
  loadStatistics('week')
})
</script>

<style scoped>
.arrived-dashboard {
  min-height: 100%;
  display: flex;
  flex-direction: column;
  gap: 20px;
  color: var(--color-body);
}

.dashboard-monitor-tabs {
  padding: 0;
  margin-bottom: -12px;
}

.dashboard-monitor-tabs :deep(.el-tabs__header) {
  margin: 0;
}

.dashboard-monitor-tabs :deep(.el-tabs__item) {
  font-size: 15px;
  font-weight: 600;
  padding: 0 24px;
  height: 44px;
  line-height: 44px;
  color: var(--text-secondary);
}

.dashboard-monitor-tabs :deep(.el-tabs__item.is-active) {
  color: var(--color-primary);
}

.dashboard-monitor-tabs :deep(.el-tabs__active-bar) {
  background-color: var(--color-primary);
  height: 3px;
}

.dashboard-time-filter {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 20px;
  border-radius: var(--radius-lg);
  border: var(--card-border);
  background: var(--card-gradient);
  box-shadow: var(--shadow-card);
}

.time-filter__label {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  white-space: nowrap;
}

.time-filter__picker {
  width: 380px;
}

.chart-filter {
  width: 220px;
}

.filter-reset {
  height: 32px;
  padding: 0 6px;
  color: var(--color-accent);
}

.dashboard-summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.summary-card {
  position: relative;
  overflow: hidden;
  min-height: 144px;
  padding: 22px 24px;
  border-radius: 20px;
  border: var(--card-border);
  background:
    var(--card-gradient),
    radial-gradient(circle at top right, rgba(64, 158, 255, 0.12), transparent 42%);
  box-shadow: var(--shadow-card);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.summary-card::after {
  content: '';
  position: absolute;
  inset: auto -36px -36px auto;
  width: 116px;
  height: 116px;
  border-radius: 50%;
  background: rgba(64, 158, 255, 0.08);
}

.summary-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-card-hover);
}

.summary-card__label {
  position: relative;
  z-index: 1;
  font-size: 15px;
  color: var(--text-secondary);
  letter-spacing: 1px;
}

.summary-card__value {
  position: relative;
  z-index: 1;
  margin-top: 14px;
  font-size: 44px;
  line-height: 1;
  font-weight: 700;
  color: var(--text-primary);
}

.summary-card__value-efficient {
  color: var(--color-success);
}

.summary-card__value-normal {
  color: var(--color-primary);
}

.summary-card__value-delayed {
  color: var(--color-danger);
}

.summary-card__value-total {
  color: var(--color-accent);
}

.summary-card__meta {
  position: relative;
  z-index: 1;
  margin-top: 18px;
  font-size: 13px;
  color: var(--text-muted);
}

.summary-card-efficient {
  background:
    linear-gradient(180deg, #ffffff, var(--color-success-bg)),
    radial-gradient(circle at top right, rgba(103, 194, 58, 0.16), transparent 42%);
}

.summary-card-normal {
  background:
    linear-gradient(180deg, #ffffff, var(--color-primary-bg)),
    radial-gradient(circle at top right, rgba(29, 114, 243, 0.14), transparent 42%);
}

.summary-card-delayed {
  background:
    linear-gradient(180deg, #ffffff, var(--color-danger-bg)),
    radial-gradient(circle at top right, rgba(245, 108, 108, 0.18), transparent 42%);
}

.summary-card-total {
  background:
    linear-gradient(180deg, #ffffff, var(--color-info-bg)),
    radial-gradient(circle at top right, rgba(64, 158, 255, 0.18), transparent 42%);
}

.empty-data {
  padding: 40px 0;
}

.tab-view {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.dashboard-panel {
  padding: 22px 24px 24px;
  border-radius: var(--radius-xl);
  border: var(--card-border);
  background:
    var(--card-gradient),
    radial-gradient(circle at top, rgba(64, 158, 255, 0.08), transparent 36%);
  box-shadow: var(--shadow-card);
}

.dashboard-panel-statistics .card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 18px;
}

.panel-title {
  font-size: 20px;
  font-weight: 700;
  color: var(--text-primary);
}

.panel-subtitle {
  margin-top: 8px;
  font-size: 13px;
  color: var(--text-subtle);
}

.panel-tag {
  border-color: rgba(64, 158, 255, 0.2);
  background: rgba(64, 158, 255, 0.08);
  color: var(--color-accent);
}

:deep(.el-button--primary) {
  --el-button-bg-color: var(--color-primary);
  --el-button-border-color: var(--color-primary);
  --el-button-hover-bg-color: var(--color-primary-light);
  --el-button-hover-border-color: var(--color-primary-light);
}

:deep(.chart-filter .el-select__wrapper) {
  background: #fff;
  box-shadow: 0 0 0 1px var(--color-border-focus) inset;
}

@media (max-width: 1200px) {
  .dashboard-summary {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .dashboard-summary {
    grid-template-columns: 1fr;
  }

  .dashboard-panel,
  .summary-card {
    border-radius: 18px;
  }

  .panel-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .panel-actions {
    width: 100%;
    justify-content: space-between;
  }

  .chart-filter {
    width: 100%;
  }

  .time-filter__picker {
    width: 100%;
  }

  .summary-card__value {
    font-size: 38px;
  }
}
</style>
