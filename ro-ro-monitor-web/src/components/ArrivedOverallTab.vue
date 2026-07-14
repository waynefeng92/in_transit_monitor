<template>
  <div class="arrived-overall-tab">
    <template v-if="loading">
      <div class="chart-layout">
        <div class="chart-card chart-card-bar">
          <div class="chart-card__header">
            <div class="chart-card__title">整段效率分布</div>
            <div class="chart-card__subtitle">按品牌查看高效、正常、延迟数量</div>
          </div>
          <el-skeleton animated>
            <template #template>
              <el-skeleton-item variant="rect" style="width: 100%; height: 420px; border-radius: 12px;" />
            </template>
          </el-skeleton>
        </div>
        <div class="chart-card chart-card-pie">
          <div class="chart-card__header">
            <div class="chart-card__title">整段效率占比</div>
            <div class="chart-card__subtitle">各品牌高效、正常、延迟数量比例</div>
          </div>
          <el-skeleton animated>
            <template #template>
              <el-skeleton-item variant="rect" style="width: 100%; height: 300px; border-radius: 12px;" />
            </template>
          </el-skeleton>
        </div>
      </div>
    </template>

    <template v-else>
      <el-empty v-if="isEmpty" description="暂无数据" />
      <div v-else class="chart-layout">
        <div class="chart-card chart-card-bar">
          <div class="chart-card__header">
            <div class="chart-card__title">整段效率分布</div>
            <div class="chart-card__subtitle">按品牌查看高效、正常、延迟数量</div>
          </div>
          <div ref="barChartRef" class="chart-container"></div>
        </div>
        <div class="chart-card chart-card-pie">
          <div class="chart-card__header">
            <div class="chart-card__title">整段效率占比</div>
            <div class="chart-card__subtitle">各品牌高效、正常、延迟数量比例</div>
          </div>
          <div ref="pieChartRef" class="pie-section__chart"></div>
        </div>
      </div>

      <section v-if="!isEmpty" class="dashboard-panel detail-section" style="margin-top: 20px;">
        <div class="panel-header">
          <div>
            <div class="panel-title">详细数据</div>
          </div>
        </div>
        <el-table :data="tableData" border stripe size="small" max-height="420">
          <el-table-column prop="brand" label="品牌" min-width="120" fixed />
          <el-table-column label="高效" width="100" align="center">
            <template #default="{ row }">
              <el-tag type="success" effect="dark" class="clickable-tag"
                @click.stop="handleCellClick(row, 'EFFICIENT')">{{ row.efficient }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="正常" width="100" align="center">
            <template #default="{ row }">
              <el-tag type="primary" effect="dark" class="clickable-tag"
                @click.stop="handleCellClick(row, 'NORMAL')">{{ row.normal }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="延迟" width="100" align="center">
            <template #default="{ row }">
              <el-tag type="danger" effect="dark" class="clickable-tag"
                @click.stop="handleCellClick(row, 'DELAYED')">{{ row.delayed }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="合计" width="100" align="center">
            <template #default="{ row }">{{ (row.efficient||0) + (row.normal||0) + (row.delayed||0) }}</template>
          </el-table-column>
        </el-table>
        <div v-show="activeDrillCell" class="drilldown-area" style="margin-top:12px">
          <el-skeleton v-if="drilldownLoading" animated :rows="3" />
          <template v-else>
            <VehicleDrilldownTable
              v-if="drilldownVehicleList.length"
              :vehicleList="drilldownVehicleList"
              :total="drilldownTotal"
              showType="arrived"
              :currentPage="drilldownPage"
              @close="handleCloseDrilldown"
              @page-change="handleDrilldownPageChange"
            />
            <el-empty v-else description="暂无匹配车辆" />
          </template>
        </div>
      </section>
    </template>
  </div>
</template>

<script setup>
import { computed, ref, onMounted, onUnmounted, watch, nextTick } from 'vue'
import * as echarts from 'echarts'
import VehicleDrilldownTable from '@/components/dashboard/VehicleDrilldownTable.vue'
import { getArrivedVehicleDetails } from '@/api/arrived'

const props = defineProps({
  chartData: {
    type: [Object, Array],
    default: () => ({})
  },
  loading: {
    type: Boolean,
    default: false
  },
  brand: {
    type: String,
    default: ''
  }
})

const categoryConfig = {
  EFFICIENT: { label: '高效', color: '#67c23a' },
  NORMAL: { label: '正常', color: '#409eff' },
  DELAYED: { label: '延迟', color: '#f56c6c' }
}

const barChartRef = ref(null)
const pieChartRef = ref(null)
let barChartInstance = null
let pieChartInstance = null

const isEmpty = computed(() => {
  const data = props.chartData
  if (!data) return true
  if (Array.isArray(data) && data.length === 0) return true
  if (typeof data === 'object') {
    const brands = data.brands || []
    const seriesData = data.data || []
    if (brands.length === 0) return true
    const hasAnyValue = seriesData.some(arr => (arr || []).some(v => Number(v) > 0))
    return !hasAnyValue
  }
  return true
})

const processedData = computed(() => {
  const data = props.chartData
  if (!data || typeof data !== 'object' || Array.isArray(data)) {
    return { brands: [], categories: [], data: [] }
  }

  const rawBrands = data.brands || []
  const rawCategories = data.categories || []
  const rawData = data.data || []

  let filteredIndices = rawBrands.map((_, i) => i)

  const brands = filteredIndices.map(i => rawBrands[i])
  const categories = rawCategories
  const seriesData = categories.map((_, catIndex) =>
    filteredIndices.map(brandIndex => Number(rawData[brandIndex]?.[catIndex] || 0))
  )

  return { brands, categories, data: seriesData }
})

const initCharts = () => {
  window.addEventListener('resize', handleResize)
}

const updateCharts = () => {
  updateBarChart()
  updatePieChart()
}

const updateBarChart = () => {
  if (!barChartInstance || !document.body.contains(barChartInstance.getDom())) {
    if (barChartRef.value) {
      barChartInstance?.dispose?.()
      barChartInstance = echarts.init(barChartRef.value, 'roro')
    } else {
      return
    }
  }

  const { brands, categories, data } = processedData.value

  if (brands.length === 0 || categories.length === 0) {
    barChartInstance.setOption({
      title: {
        text: '暂无数据',
        left: 'center',
        top: 'center',
        textStyle: { color: '#999', fontSize: 14 }
      }
    }, { notMerge: true })
    return
  }

  const series = categories.map((cat, index) => {
    const config = categoryConfig[cat] || { label: cat, color: '#999' }
    return {
      name: config.label,
      type: 'bar',
      stack: 'total',
      color: config.color,
      data: data[index] || [],
      barWidth: 54,
      barCategoryGap: '34%',
      itemStyle: {
        borderRadius: [2, 2, 2, 2]
      },
      label: {
        show: true,
        position: 'inside',
        color: '#fff',
        fontWeight: 'bold',
        padding: [2, 0, 0, 0],
        formatter: ({ value }) => (value ? value : '')
      }
    }
  })

  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      backgroundColor: 'rgba(255, 255, 255, 0.96)',
      borderColor: '#dbe7f5',
      borderWidth: 1,
      borderRadius: 7,
      padding: [12, 14],
      textStyle: { color: '#22324d' },
      extraCssText: 'box-shadow: 0 12px 32px rgba(26, 65, 122, 0.12);',
      formatter: (params) => {
        if (!params.length) return ''
        let result = params[0].name + '<br/>'
        let total = 0
        params.forEach(item => {
          result += `${item.marker} ${item.seriesName}: ${item.value}<br/>`
          total += item.value
        })
        result += `<b>总计: ${total}</b>`
        return result
      }
    },
    legend: {
      data: categories.map(cat => (categoryConfig[cat]?.label || cat)),
      orient: 'horizontal',
      left: 'center',
      top: 10,
      itemWidth: 12,
      itemHeight: 12,
      itemGap: 18,
      icon: 'roundRect',
      textStyle: {
        color: '#5e708d',
        fontSize: 13,
        padding: [0, 0, 0, 6]
      }
    },
    grid: {
      left: '12%',
      right: '5%',
      bottom: '15%',
      top: '20%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: brands,
      axisLabel: {
        rotate: 0,
        interval: 0,
        fontSize: 11,
        margin: 18,
        lineHeight: 16
      },
      axisLine: {
        lineStyle: { color: '#999' }
      }
    },
    yAxis: {
      type: 'value',
      name: '车辆数量',
      nameTextStyle: { fontSize: 12 },
      axisLine: { show: false },
      axisTick: { show: false },
      splitLine: {
        lineStyle: { color: '#e8eef6', type: 'dashed' }
      }
    },
    series,
    dataZoom: [
      {
        type: 'slider',
        start: 0, end: 100,
        height: 8, bottom: 8,
        showDetail: false,
        borderColor: 'transparent',
        backgroundColor: 'rgba(0,0,0,0.04)',
        fillerColor: 'rgba(64,158,255,0.25)'
      },
      {
        type: 'inside',
        start: 0,
        end: 100
      }
    ]
  }

  barChartInstance.setOption(option, { notMerge: true })
}

const updatePieChart = () => {
  if (!pieChartInstance || !document.body.contains(pieChartInstance.getDom())) {
    if (pieChartRef.value) {
      pieChartInstance?.dispose?.()
      pieChartInstance = echarts.init(pieChartRef.value, 'roro')
    } else {
      return
    }
  }

  const { categories, data } = processedData.value

  if (categories.length === 0) {
    pieChartInstance.setOption({
      title: {
        text: '暂无比例数据',
        left: 'center',
        top: 'center',
        textStyle: { color: '#999', fontSize: 14 }
      }
    }, { notMerge: true })
    return
  }

  const pieData = categories.map((cat, index) => {
    const config = categoryConfig[cat] || { label: cat, color: '#999' }
    const total = (data[index] || []).reduce((sum, val) => sum + val, 0)
    return {
      name: config.label,
      value: total,
      itemStyle: { color: config.color }
    }
  }).filter(item => item.value > 0)

  if (pieData.length === 0) {
    pieChartInstance.setOption({
      title: {
        text: '暂无比例数据',
        left: 'center',
        top: 'center',
        textStyle: { color: '#999', fontSize: 14 }
      }
    }, { notMerge: true })
    return
  }

  const option = {
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(255, 255, 255, 0.96)',
      borderColor: '#dbe7f5',
      borderWidth: 1,
      borderRadius: 7,
      padding: [12, 14],
      textStyle: { color: '#22324d' },
      extraCssText: 'box-shadow: 0 12px 32px rgba(26, 65, 122, 0.12);',
      formatter: ({ name, value, percent }) => `${name}<br/>数量: ${value}<br/>占比: ${percent}%`
    },
    legend: {
      bottom: 0,
      left: 'center',
      itemWidth: 12,
      itemHeight: 12,
      itemGap: 18,
      icon: 'roundRect',
      textStyle: {
        color: '#5e708d',
        fontSize: 13
      }
    },
    series: [
      {
        type: 'pie',
        radius: ['46%', '70%'],
        center: ['50%', '44%'],
        avoidLabelOverlap: true,
        itemStyle: {
          borderRadius: 6,
          borderColor: '#fff',
          borderWidth: 3
        },
        label: {
          show: true,
          color: '#22324d',
          formatter: '{b}\n{d}%',
          lineHeight: 18,
          fontSize: 12
        },
        labelLine: {
          length: 14,
          length2: 12,
          lineStyle: { color: '#b8c7db' }
        },
        data: pieData
      }
    ]
  }

  pieChartInstance.setOption(option, { notMerge: true })
}

const handleResize = () => {
  if (barChartInstance) barChartInstance.resize()
  if (pieChartInstance) pieChartInstance.resize()
}

watch(() => props.chartData, () => {
  nextTick(updateCharts)
}, { deep: true, immediate: true })

watch(() => props.brand, () => {
  nextTick(updateCharts)
})

watch(() => props.loading, (val) => {
  if (!val) {
    nextTick(() => {
      if (!barChartInstance && barChartRef.value) {
        barChartInstance = echarts.init(barChartRef.value, 'roro')
      }
      if (!pieChartInstance && pieChartRef.value) {
        pieChartInstance = echarts.init(pieChartRef.value, 'roro')
      }
      updateCharts()
    })
  }
})

onMounted(() => {
  if (!props.loading) {
    initCharts()
  }
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  if (barChartInstance) {
    barChartInstance.dispose()
    barChartInstance = null
  }
  if (pieChartInstance) {
    pieChartInstance.dispose()
    pieChartInstance = null
  }
})

// ── Drilldown table state ──
const drilldownVehicleList = ref([])
const drilldownLoading = ref(false)
const activeDrillCell = ref(null)
const drilldownTotal = ref(0)
const drilldownPage = ref(1)

const tableData = computed(() => {
  const data = props.chartData
  if (!data || !data.brands) return []
  return data.brands.map((brand, idx) => ({
    brand: brand,
    efficient: data.data?.[idx]?.[0] || 0,
    normal: data.data?.[idx]?.[1] || 0,
    delayed: data.data?.[idx]?.[2] || 0
  }))
})

const handleCellClick = async (row, bucket) => {
  const cellKey = `${row.brand}_${bucket}`
  if (activeDrillCell.value === cellKey) {
    drilldownVehicleList.value = []
    activeDrillCell.value = null
    return
  }
  activeDrillCell.value = cellKey
  drilldownLoading.value = true
  drilldownVehicleList.value = []
  drilldownPage.value = 1
  try {
    const params = {
      brandName: row.brand,
      efficiencyBucket: bucket,
      page: 1, size: 20
    }
    const data = await getArrivedVehicleDetails(params)
    drilldownVehicleList.value = Array.isArray(data?.records) ? data.records : []
    drilldownTotal.value = data?.total || 0
  } catch (e) {
    console.error('获取车辆明细失败', e)
  } finally {
    drilldownLoading.value = false
  }
}

const handleCloseDrilldown = () => {
  drilldownVehicleList.value = []
  activeDrillCell.value = null
  drilldownTotal.value = 0
}

const handleDrilldownPageChange = async (newPage, newSize) => {
  drilldownPage.value = newPage
  drilldownLoading.value = true
  const cellKey = activeDrillCell.value
  const [brand, bucket] = cellKey.split('_')
  try {
    const params = {
      brandName: brand,
      efficiencyBucket: bucket,
      page: newPage,
      size: newSize || 20
    }
    const data = await getArrivedVehicleDetails(params)
    if (activeDrillCell.value === cellKey) {
      drilldownVehicleList.value = Array.isArray(data?.records) ? data.records : []
      drilldownTotal.value = data?.total || 0
    }
  } catch (e) {
    console.error('获取车辆明细失败', e)
  } finally {
    drilldownLoading.value = false
  }
}
</script>

<style scoped>
.arrived-overall-tab {
  min-height: 200px;
}

.chart-layout {
  display: grid;
  grid-template-columns: minmax(0, 2fr) minmax(320px, 1fr);
  gap: 20px;
}

.chart-card {
  border-radius: var(--radius-xl);
  border: var(--card-border);
  background: var(--card-gradient);
  box-shadow: var(--shadow-card);
  padding: 18px 18px 16px;
  overflow: hidden;
}

.chart-card__header {
  margin-bottom: 8px;
}

.chart-card__title {
  font-size: 17px;
  font-weight: 600;
  color: var(--text-primary);
}

.chart-card__subtitle {
  margin-top: 6px;
  font-size: 13px;
  color: var(--text-subtle);
}

.chart-container {
  min-height: 460px;
  padding: 8px 0 0;
}

.pie-section__chart {
  min-height: 320px;
}

@media (max-width: 1200px) {
  .chart-layout {
    grid-template-columns: 1fr;
  }
}

.clickable-tag {
  cursor: pointer;
  transition: opacity 0.2s;
}
.clickable-tag:hover {
  opacity: 0.7;
}

.detail-section {
  border-radius: var(--radius-xl);
  border: var(--card-border);
  background: var(--card-gradient);
  box-shadow: var(--shadow-card);
  padding: 18px 18px 16px;
  overflow: hidden;
}

.detail-section .panel-header {
  margin-bottom: 14px;
}

.detail-section .panel-title {
  font-size: 17px;
  font-weight: 600;
  color: var(--text-primary);
}

.drilldown-area {
  animation: drilldownFadeIn 0.3s ease;
}

@keyframes drilldownFadeIn {
  from { opacity: 0; transform: translateY(-8px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
