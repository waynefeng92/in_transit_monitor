<template>
  <div class="dashboard">
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
      <div class="time-filter__label">订单释放时间</div>
      <el-date-picker
        v-model="dateRange"
        type="daterange"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        value-format="YYYY-MM-DDTHH:mm:ss"
        class="time-filter__picker"
      />
      <el-select v-model="selectedBrand" clearable filterable placeholder="筛选品牌" class="chart-filter" style="width:160px">
        <el-option v-for="brand in brandOptions" :key="brand" :label="brand" :value="brand" />
      </el-select>
      <el-select v-if="activeMonitorTab !== 'overall'" v-model="selectedSection" clearable filterable
        :placeholder="activeMonitorTab === 'three-section' ? '段选择' : '分段选择'" class="chart-filter" style="width:140px">
        <el-option v-for="opt in sectionOptions" :key="opt" :label="opt" :value="opt" />
      </el-select>
      <el-button type="primary" @click="loadData">查询</el-button>
      <el-button text class="filter-reset" @click="resetAllFilters">重置</el-button>
    </section>

    <SegmentTab
      v-if="activeMonitorTab === 'segment'"
      :chart-data="chartData"
      :display-summary="displaySummary"
      :loading="loading"
      :initial-loading="initialLoading"
      @refresh="loadData"
    />

    <OverallTab
      v-if="activeMonitorTab === 'overall'"
      :chart-data="overallChartData"
      :display-summary="displayOverallSummary"
      :loading="loading"
      :initial-loading="overallInitialLoading"
      @refresh="loadData"
    />

    <ThreeSectionTab
      v-if="activeMonitorTab === 'three-section'"
      :section-chart-data="sectionChartData"
      :display-summary="displaySectionSummary"
      :loading="loading"
      :initial-loading="sectionInitialLoading"
      @refresh="loadData"
    />
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted, onUnmounted } from 'vue'
import request from '@/api/request'
import SegmentTab from '@/components/dashboard/SegmentTab.vue'
import OverallTab from '@/components/dashboard/OverallTab.vue'
import ThreeSectionTab from '@/components/dashboard/ThreeSectionTab.vue'

// ── Shared state ──────────────────────────────────────────────
const activeMonitorTab = ref('overall')
const dateRange = ref(null)
const loading = ref(false)

// ── Segment tab state ─────────────────────────────────────────
const summary = ref({ normal: 0, warn: 0, overdue: 0, total: 0 })
const displaySummary = reactive({ normal: 0, warn: 0, overdue: 0, total: 0 })
const chartData = ref([])
const initialLoading = ref(true)
const selectedBrand = ref('')
const brandOptions = ref([])
const selectedSection = ref('')
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
  if (activeMonitorTab.value === 'overall') return '按整段运输链监控状态（正常/预警/超期）'
  if (activeMonitorTab.value === 'three-section') return '按前段（客户开单-装船）/中段（水路起运-到港卸船）/后段（卸船完成-到店）监控'
  if (activeMonitorTab.value === 'segment') return '按7个在途分段分别监控：未出库、集港在途、已集港待装船、水运在途、已到港待卸船、已卸船待分拨、分拨在途'
  return ''
})

// ── Overall tab state ─────────────────────────────────────────
const overallSummary = ref({ overallNormal: 0, overallWarn: 0, overallOverdue: 0, total: 0 })
const displayOverallSummary = reactive({ overallNormal: 0, overallWarn: 0, overallOverdue: 0, total: 0 })
const overallChartData = ref([])
const overallInitialLoading = ref(true)

// ── Three-section tab state ───────────────────────────────────
const sectionSummary = ref({ normal: 0, warn: 0, overdue: 0 })
const displaySectionSummary = reactive({ normal: 0, warn: 0, overdue: 0, total: 0 })
const sectionChartData = ref([])
const sectionInitialLoading = ref(true)

// ── Number animation ──────────────────────────────────────────
let animationFrame = null

const animateNumbers = (from, to, duration = 800) => {
  cancelAnimationFrame(animationFrame)
  const startTime = performance.now()

  const step = (currentTime) => {
    const elapsed = currentTime - startTime
    const progress = Math.min(elapsed / duration, 1)
    const eased = 1 - Math.pow(1 - progress, 3)

    for (const key of Object.keys(from)) {
      from[key] = Math.round(from[key] + (to[key] - from[key]) * eased)
    }

    if (progress < 1) {
      animationFrame = requestAnimationFrame(step)
    } else {
      Object.assign(from, to)
    }
  }

  animationFrame = requestAnimationFrame(step)
}

// ── Watchers for animated numbers ─────────────────────────────
watch(() => summary.value, (val) => {
  if (val.total > 0) {
    if (initialLoading.value) initialLoading.value = false
    animateNumbers(displaySummary, {
      normal: val.normal || 0,
      warn: val.warn || 0,
      overdue: val.overdue || 0,
      total: val.total || 0
    })
  }
}, { deep: true })

watch(() => overallSummary.value, (val) => {
  if (val.total > 0) {
    if (overallInitialLoading.value) overallInitialLoading.value = false
    animateNumbers(displayOverallSummary, {
      overallNormal: val.overallNormal || 0,
      overallWarn: val.overallWarn || 0,
      overallOverdue: val.overallOverdue || 0,
      total: val.total || 0
    })
  }
}, { deep: true })

watch(() => sectionSummary.value, (val) => {
  if (Object.keys(val).length > 0) {
    if (sectionInitialLoading.value) sectionInitialLoading.value = false
    animateNumbers(displaySectionSummary, {
      normal: val.normal || 0,
      warn: val.warn || 0,
      overdue: val.overdue || 0,
      total: val.total || 0
    })
  }
}, { deep: true })

// ── Data fetching ─────────────────────────────────────────────
const getTimeParams = () => {
  if (!dateRange.value) return {}
  const [start, end] = dateRange.value
  if (!start || !end) return {}
  return { startTime: start, endTime: end.substring(0, 10) + 'T23:59:59' }
}

const loadSummary = async () => {
  try {
    const params = { ...getTimeParams(), brandName: selectedBrand.value || undefined }
    if (selectedSection.value) params.sectionName = selectedSection.value
    const res = await request.get('/transit/summary', { params })
    summary.value = res
  } catch (error) {
    console.error('加载汇总失败:', error)
  }
}

const loadChartData = async () => {
  try {
    const params = { ...getTimeParams(), brandName: selectedBrand.value || undefined }
    if (selectedSection.value) params.sectionName = selectedSection.value
    const res = await request.get('/chart/brand-status', { params })
    chartData.value = res || []
  } catch (error) {
    console.error('加载图表失败:', error)
    chartData.value = []
  }
}

const loadOverallSummary = async () => {
  try {
    const params = { ...getTimeParams(), brandName: selectedBrand.value || undefined }
    const res = await request.get('/transit/summary', { params })
    overallSummary.value = res
  } catch (error) {
    console.error('加载整段汇总失败:', error)
  }
}

const loadOverallChartData = async () => {
  try {
    const params = { ...getTimeParams(), type: 'overall', brandName: selectedBrand.value || undefined }
    const res = await request.get('/chart/brand-status', { params })
    overallChartData.value = res || []
  } catch (error) {
    console.error('加载整段图表失败:', error)
    overallChartData.value = []
  }
}

const loadSectionSummary = async () => {
  try {
    const params = { ...getTimeParams(), brandName: selectedBrand.value || undefined }
    if (selectedSection.value) params.sectionName = selectedSection.value
    const res = await request.get('/transit/summary', { params })
    const vals = {
      normal: res.sectionNormal || 0,
      warn: res.sectionWarn || 0,
      overdue: res.sectionOverdue || 0,
      total: res.total || 0
    }
    sectionSummary.value = vals
    displaySectionSummary.normal = vals.normal
    displaySectionSummary.warn = vals.warn
    displaySectionSummary.overdue = vals.overdue
    displaySectionSummary.total = vals.total
  } catch (error) {
    console.error('加载三段汇总失败:', error)
  }
}

const loadSectionChartData = async () => {
  try {
    const params = { ...getTimeParams(), type: 'three-section', brandName: selectedBrand.value || undefined }
    if (selectedSection.value) params.sectionName = selectedSection.value
    const res = await request.get('/chart/brand-status', { params })
    sectionChartData.value = res || []
  } catch (error) {
    console.error('加载三段图表失败:', error)
    sectionChartData.value = []
  }
}

const loadOverallData = async () => {
  loading.value = true
  overallInitialLoading.value = true
  try {
    await Promise.all([loadOverallSummary(), loadOverallChartData()])
    setTimeout(() => { overallInitialLoading.value = false }, 150)
  } finally {
    loading.value = false
  }
}

const loadSectionData = async () => {
  sectionInitialLoading.value = true
  try {
    await Promise.all([loadSectionSummary(), loadSectionChartData()])
    setTimeout(() => { sectionInitialLoading.value = false }, 150)
  } finally {
    // sectionLoading kept for future use / external reference
  }
}

// ── Main load (called by query button & tab refresh) ──────────
const loadData = async () => {
  loading.value = true
  try {
    if (activeMonitorTab.value === 'segment') {
      await Promise.all([loadSummary(), loadChartData()])
    } else if (activeMonitorTab.value === 'overall') {
      await Promise.all([loadOverallSummary(), loadOverallChartData()])
    } else if (activeMonitorTab.value === 'three-section') {
      await Promise.all([loadSectionSummary(), loadSectionChartData()])
    }
  } finally {
    loading.value = false
  }
}

// ── Tab change & brand options ────────────────────────────────
const loadBrandOptions = async () => {
  try {
    const { getBrandList } = await import('@/api/brand')
    const list = await getBrandList()
    brandOptions.value = (list || [])
      .filter(b => b.isActive !== 0)
      .map(b => b.brandName)
      .sort((a, b) => a.localeCompare(b, 'zh-Hans-CN'))
  } catch (e) {
    console.error('加载品牌列表失败', e)
  }
}

const resetFilters = () => {
  selectedBrand.value = ''
  selectedSection.value = ''
  loadData()
}

const handleTabChange = (tabName) => {
  selectedBrand.value = ''
  selectedSection.value = ''
  if (tabName === 'segment') {
    initialLoading.value = true
    loadData()
  } else if (tabName === 'overall') {
    loadOverallData()
  } else if (tabName === 'three-section') {
    loadSectionData()
  }
}

// ── Filter resets ─────────────────────────────────────────────
const resetAllFilters = () => {
  dateRange.value = null
  selectedBrand.value = ''
  selectedSection.value = ''
  loadData()
}

// ── Lifecycle ─────────────────────────────────────────────────
onMounted(() => {
  loadData()
  loadBrandOptions()
})

onUnmounted(() => {
  cancelAnimationFrame(animationFrame)
})
</script>

<style scoped>
.dashboard {
  min-height: 100%;
  display: flex;
  flex-direction: column;
  gap: 20px;
  color: var(--color-body);
}

.dashboard-time-filter {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 20px;
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-primary-lightest);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(246, 250, 255, 0.98));
  box-shadow: 0 12px 30px rgba(26, 65, 122, 0.08);
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

.dashboard-filter-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 20px;
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-primary-lightest);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(246, 250, 255, 0.98));
  box-shadow: 0 12px 30px rgba(26, 65, 122, 0.08);
}

.chart-container {
  min-height: 460px;
  padding: 8px 0 0;
}

.pie-section__chart {
  min-height: 320px;
}

.detail-toggle {
  color: var(--color-accent);
}

.detail-table-wrap {
  border-radius: 16px;
  overflow: hidden;
}

:deep(.el-table) {
  --el-table-border-color: var(--color-primary-lightest);
  --el-table-header-bg-color: #eef5ff;
  --el-table-tr-bg-color: #ffffff;
  --el-table-row-hover-bg-color: var(--color-info-bg);
  --el-table-text-color: var(--color-body);
  --el-table-header-text-color: var(--text-primary);
  --el-fill-color-blank: #ffffff;
}

:deep(.el-table th.el-table__cell) {
  font-weight: 600;
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

@media (max-width: 768px) {
  .dashboard-summary {
    grid-template-columns: 1fr;
  }

  .dashboard-panel,
  .summary-card {
    border-radius: var(--radius-lg);
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

  .summary-card__value {
    font-size: 38px;
  }
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
</style>
