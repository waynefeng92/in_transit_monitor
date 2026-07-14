<template>
  <div class="statistics">
    <section class="statistics-filter">
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
      <el-select v-model="selectedBrand" clearable filterable placeholder="品牌" style="width:150px">
        <el-option v-for="b in brandOptions" :key="b" :label="b" :value="b" />
      </el-select>
      <el-select v-model="selectedRouteId" clearable filterable placeholder="线路" style="width:280px">
        <el-option v-for="r in routeOptions" :key="r.id" :label="r.name" :value="r.id" />
      </el-select>
      <el-button type="primary" @click="loadAll">查询</el-button>
      <el-button text class="filter-reset" @click="resetFilters">重置</el-button>
    </section>

    <section class="summary-row" v-loading="loading">
      <div class="summary-card summary-card-total" v-for="card in summaryCards" :key="card.key">
        <div class="summary-card__label">{{ card.label }}</div>
        <div class="summary-card__value" :class="card.valueClass">{{ card.value }}</div>
        <div class="summary-card__meta">{{ card.meta }}</div>
      </div>
    </section>

    <section class="trend-section" v-loading="loading">
      <div class="panel-header">
        <div>
          <div class="panel-title">消耗比与按时到达率趋势</div>
          <div class="panel-subtitle">已到达车辆到港数量、OTD消耗比及按时到达率</div>
        </div>
        <div class="panel-actions">
          <el-radio-group v-model="trendPeriod" size="small" @change="loadTrend">
            <el-radio-button label="week">周</el-radio-button>
            <el-radio-button label="month">月</el-radio-button>
          </el-radio-group>
        </div>
      </div>
      <div ref="trendChartRef" class="trend-chart"></div>
    </section>

    <section class="chart-row">
      <div class="chart-card distribution-panel" v-loading="loading">
        <div class="panel-header">
          <div>
            <div class="panel-title">OTD消耗比分布</div>
            <div class="panel-subtitle">各周期分桶分布（消耗比≤80%高效，80%~100%正常，>100%延迟）</div>
          </div>
        </div>
        <div ref="distributionChartRef" class="distribution-chart"></div>
      </div>

      <div class="chart-card dimension-panel" v-loading="dimLoading">
        <div class="panel-header">
          <div>
            <div class="panel-title">线路分析</div>
            <div class="panel-subtitle">各线路到达数及按时到达率</div>
          </div>
        </div>
        <div ref="dimensionChartRef" class="dimension-chart"></div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getStatisticsSummary, getTrend, getByRoute } from '@/api/statistics'
import { getBrandList } from '@/api/brand'
import { getRouteList, getRouteByBrand } from '@/api/route'

const loading = ref(false)
const dimLoading = ref(false)

const dateRange = ref([
  new Date(Date.now() - 90 * 24 * 60 * 60 * 1000).toISOString().substring(0, 10) + 'T00:00:00',
  new Date().toISOString().substring(0, 10) + 'T00:00:00'
])

const selectedBrand = ref('')
const selectedRouteId = ref(null)
const trendPeriod = ref('week')

const brandList = ref([])
const brandOptions = computed(() =>
  brandList.value.filter(b => b.isActive !== 0).map(b => b.brandName).sort()
)
const routeOptions = ref([])
const trendData = ref([])
const distributionData = ref([])
const dimensionData = ref([])

const summary = ref({ totalArrivals: 0, avgEfficiency: 0, otdComplianceRate: 0 })

const summaryCards = computed(() => {
  const ratio = summary.value.avgEfficiency || 0
  const compliance = summary.value.otdComplianceRate || 0

  const ratioClass = ratio <= 80 ? 'value-green' : ratio <= 100 ? 'value-blue' : 'value-red'
  const complianceClass = compliance >= 90 ? 'value-green' : compliance >= 70 ? 'value-yellow' : 'value-red'

  return [
    { key: 'total', label: '总到达数', value: summary.value.totalArrivals || 0, meta: '车辆总到达数量', valueClass: '' },
    { key: 'efficiency', label: 'OTD消耗比', value: ratio.toFixed(1) + '%', meta: '实际耗时 / 标准OTD，越低越好', valueClass: ratioClass },
    { key: 'compliance', label: '按时到达率', value: compliance.toFixed(1) + '%', meta: 'OTD消耗比≤100%的占比', valueClass: complianceClass }
  ]
})

const getFilterParams = () => {
  const params = {}
  if (dateRange.value) {
    const [s, e] = dateRange.value
    if (s && e) {
      params.startTime = s
      params.endTime = e.substring(0, 10) + 'T23:59:59'
    }
  }
  if (selectedBrand.value) params.brandName = selectedBrand.value
  if (selectedRouteId.value) params.routeId = selectedRouteId.value
  return params
}

const loadSummary = async () => {
  const data = await getStatisticsSummary(getFilterParams())
  summary.value = data || { totalArrivals: 0, avgEfficiency: 0, otdComplianceRate: 0 }
}

const loadTrend = async () => {
  const data = await getTrend({ ...getFilterParams(), period: trendPeriod.value })
  trendData.value = data || []
  nextTick(renderTrendChart)
}

const loadDistribution = async () => {
  const data = await getTrend({ ...getFilterParams(), period: trendPeriod.value })
  distributionData.value = data || []
  nextTick(renderDistributionChart)
}

const loadRouteDimension = async () => {
  dimLoading.value = true
  try {
    const data = await getByRoute(getFilterParams())
    dimensionData.value = data || []
    nextTick(renderDimensionChart)
  } finally {
    dimLoading.value = false
  }
}

const loadAll = () => {
  loading.value = true
  Promise.all([loadSummary(), loadTrend(), loadDistribution(), loadRouteDimension()]).finally(() => {
    loading.value = false
  })
}

const resetFilters = () => {
  dateRange.value = null
  selectedBrand.value = ''
  selectedRouteId.value = null
  loadAll()
}

// ── Trend chart ──
const trendChartRef = ref(null)
let trendChart = null

const renderTrendChart = () => {
  if (!trendChartRef.value) return
  if (!trendChart || !document.body.contains(trendChart.getDom())) {
    trendChart = echarts.init(trendChartRef.value, 'roro')
  }
  const periods = trendData.value.map(d => d.period)
  const counts = trendData.value.map(d => d.arrivalCount)
  const efficiencies = trendData.value.map(d => d.avgEfficiency || 0)
  const complianceRates = trendData.value.map(d => {
    const total = d.arrivalCount || 0
    if (total === 0) return 0
    return ((d.efficientCount || 0) + (d.normalCount || 0)) / total * 100
  })

  trendChart.setOption({
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255,255,255,0.96)',
      borderColor: '#dbe7f5',
      formatter: params => {
        let r = `<div style="font-weight:600;margin-bottom:4px">${params[0].name}</div>`
        params.forEach(p => {
          const isPct = p.seriesName === 'OTD消耗比' || p.seriesName === '按时到达率'
          const val = isPct ? Number(p.value).toFixed(1) + '%' : p.value
          r += `${p.marker} ${p.seriesName}: ${val}<br/>`
        })
        return r
      }
    },
    legend: {
      data: ['到港数量', 'OTD消耗比', '按时到达率'],
      top: 0,
      textStyle: { color: '#5e708d', fontSize: 12 }
    },
    grid: { left: '4%', right: '4%', bottom: '8%', top: '14%', containLabel: true },
    xAxis: {
      type: 'category', data: periods,
      axisLabel: { color: '#5e708d', fontSize: 11, rotate: periods.length > 12 ? 45 : 0 },
      axisLine: { lineStyle: { color: '#dbe7f5' } }
    },
    yAxis: [
      {
        type: 'value', name: '到港数量',
        nameTextStyle: { color: '#5e708d', fontSize: 11 },
        axisLabel: { color: '#5e708d' },
        splitLine: { lineStyle: { color: '#dbe7f5', type: 'dashed' } }
      },
      {
        type: 'value', name: 'OTD消耗比(%)',
        nameTextStyle: { color: '#5e708d', fontSize: 11 },
        axisLabel: { color: '#5e708d', formatter: '{value}%' },
        splitLine: { show: false }
      }
    ],
    series: [
      {
        name: '到港数量', type: 'bar', data: counts, barWidth: '36%',
        itemStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: '#409eff' }, { offset: 1, color: '#1d72f3' }]) }
      },
      {
        name: 'OTD消耗比', type: 'line', yAxisIndex: 1, data: efficiencies,
        smooth: true, symbol: 'circle', symbolSize: 6,
        lineStyle: { color: '#67c23a', width: 2 },
        itemStyle: { color: '#67c23a' }
      },
      {
        name: '按时到达率', type: 'line', yAxisIndex: 1, data: complianceRates,
        smooth: true, symbol: 'diamond', symbolSize: 6,
        lineStyle: { color: '#e6a23c', width: 2 },
        itemStyle: { color: '#e6a23c' }
      }
    ]
  }, { notMerge: true })
}

// ── Distribution chart ──
const distributionChartRef = ref(null)
let distributionChart = null

const renderDistributionChart = () => {
  if (!distributionChartRef.value) return
  if (!distributionChart || !document.body.contains(distributionChart.getDom())) {
    distributionChart = echarts.init(distributionChartRef.value, 'roro')
  }
  const periods = distributionData.value.map(d => d.period)
  const efficient = distributionData.value.map(d => d.efficientCount || 0)
  const normal = distributionData.value.map(d => d.normalCount || 0)
  const delayed = distributionData.value.map(d => d.delayedCount || 0)

  distributionChart.setOption({
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255,255,255,0.96)',
      borderColor: '#dbe7f5'
    },
    legend: { data: ['高效', '正常', '延迟'], top: 0, textStyle: { color: '#5e708d', fontSize: 12 } },
    grid: { left: '4%', right: '4%', bottom: '8%', top: '14%', containLabel: true },
    xAxis: {
      type: 'category', data: periods,
      axisLabel: { color: '#5e708d', fontSize: 11, rotate: periods.length > 12 ? 45 : 0 },
      axisLine: { lineStyle: { color: '#dbe7f5' } }
    },
    yAxis: {
      type: 'value', name: '车辆数',
      nameTextStyle: { color: '#5e708d' },
      axisLabel: { color: '#5e708d' },
      splitLine: { lineStyle: { color: '#dbe7f5', type: 'dashed' } }
    },
    series: [
      { name: '高效', type: 'bar', stack: 'total', data: efficient, color: '#67c23a', barWidth: '40%' },
      { name: '正常', type: 'bar', stack: 'total', data: normal, color: '#409eff' },
      { name: '延迟', type: 'bar', stack: 'total', data: delayed, color: '#f56c6c' }
    ]
  }, { notMerge: true })
}

// ── Dimension chart ──
const dimensionChartRef = ref(null)
let dimensionChart = null

const renderDimensionChart = () => {
  if (!dimensionChartRef.value) return
  if (!dimensionChart || !document.body.contains(dimensionChart.getDom())) {
    dimensionChart = echarts.init(dimensionChartRef.value, 'roro')
  }
  const data = dimensionData.value || []
  const sorted = [...data].sort((a, b) => (b.totalCount || 0) - (a.totalCount || 0))
  const names = sorted.map(d => d.name)
  const counts = sorted.map(d => d.totalCount || 0)
  const rates = sorted.map(d => (d.otdComplianceRate || 0).toFixed(1))

  dimensionChart.setOption({
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255,255,255,0.96)',
      borderColor: '#dbe7f5',
      formatter: params => {
        const idx = params[0].dataIndex
        return `${names[idx]}<br/>到达数: ${counts[idx]}<br/>按时到达率: ${rates[idx]}%`
      }
    },
    grid: { left: '4%', right: '4%', bottom: '8%', top: '4%', containLabel: true },
    xAxis: {
      type: 'value', name: '到达数',
      nameTextStyle: { color: '#5e708d' },
      axisLabel: { color: '#5e708d' },
      splitLine: { lineStyle: { color: '#dbe7f5', type: 'dashed' } }
    },
    yAxis: {
      type: 'category', data: names,
      axisLabel: { color: '#5e708d', fontSize: 11 },
      axisLine: { lineStyle: { color: '#dbe7f5' } },
      inverse: true
    },
    dataZoom: [
      {
        type: 'slider',
        yAxisIndex: 0,
        start: 0, end: 100,
        width: 6, right: 2,
        showDetail: false,
        borderColor: 'transparent',
        backgroundColor: 'rgba(0,0,0,0.04)',
        fillerColor: 'rgba(64,158,255,0.25)'
      },
      {
        type: 'inside',
        yAxisIndex: 0,
        start: 0, end: 100
      }
    ],
    series: [
      {
        type: 'bar', data: counts,
        barWidth: '60%',
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [{ offset: 0, color: '#1d72f3' }, { offset: 1, color: '#409eff' }])
        },
        label: {
          show: true, position: 'right', color: '#5e708d', fontSize: 11,
          formatter: p => counts[p.dataIndex] + ' 按时到达率' + rates[p.dataIndex] + '%'
        }
      }
    ]
  }, { notMerge: true })
}

const handleResize = () => {
  trendChart?.resize()
  distributionChart?.resize()
  dimensionChart?.resize()
}

// ── Load options ──
const loadOptions = async () => {
  try {
    if (selectedBrand.value) {
      const b = brandList.value.find(b => b.brandName === selectedBrand.value)
      const brandId = b ? b.id : null
      const routes = brandId ? await getRouteByBrand(brandId) : []
      routeOptions.value = (routes || []).map(r => ({
        id: r.id, name: `${r.originCity || '?'} → ${r.destCity || '?'}`
      }))
    } else {
      const routes = await getRouteList(true)
      routeOptions.value = (routes || []).map(r => ({
        id: r.id, name: `${r.originCity || '?'} → ${r.destCity || '?'}`
      }))
    }
  } catch (e) {
    console.error('加载选项失败', e)
  }
}

watch(selectedBrand, () => {
  selectedRouteId.value = null
  loadOptions()
})

onMounted(async () => {
  try {
    const brands = await getBrandList()
    brandList.value = brands || []
  } catch (e) {
    console.error('加载品牌失败', e)
  }
  loadOptions()
  loadAll()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
  distributionChart?.dispose()
  dimensionChart?.dispose()
})
</script>

<style scoped>
.statistics {
  display: flex;
  flex-direction: column;
  gap: 20px;
  color: var(--color-body);
}
.statistics-filter {
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
.time-filter__picker { width: 380px; }
.filter-reset { height: 32px; padding: 0 6px; color: var(--color-primary); }

.summary-row {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}
.summary-card {
  position: relative;
  overflow: hidden;
  min-height: 130px;
  padding: 22px 24px;
  border-radius: var(--radius-lg);
  border: var(--card-border);
  background: var(--card-gradient);
  box-shadow: var(--shadow-card);
  transition: transform 0.2s ease;
}
.summary-card:hover { transform: translateY(-4px); }
.summary-card__label { font-size: 15px; color: var(--text-secondary); letter-spacing: 1px; }
.summary-card__value { margin-top: 14px; font-size: 38px; line-height: 1; font-weight: 700; color: var(--text-primary); }
.value-green { color: var(--color-success); }
.value-yellow { color: var(--color-warning); }
.value-red { color: var(--color-danger); }
.value-blue { color: var(--color-primary); }
.summary-card__meta { margin-top: 12px; font-size: 13px; color: var(--text-muted); }

.trend-section, .chart-card {
  padding: 22px 24px 24px;
  border-radius: var(--radius-xl);
  border: var(--card-border);
  background: var(--card-gradient);
  box-shadow: var(--shadow-card);
}
.trend-chart { width: 100%; height: 400px; }
.chart-row {
  display: grid;
  grid-template-columns: minmax(0, 3fr) minmax(0, 2fr);
  gap: 20px;
}
.distribution-chart { width: 100%; height: 300px; }
.dimension-chart { width: 100%; height: 380px; }

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 12px;
}
.panel-title { font-size: 18px; font-weight: 700; color: var(--text-primary); }
.panel-subtitle { margin-top: 6px; font-size: 12px; color: var(--text-muted); }
.panel-actions { display: flex; align-items: center; gap: 12px; }

@media (max-width: 1200px) {
  .chart-row { grid-template-columns: 1fr; }
}
</style>
