<template>
  <div class="arrived-overall-tab">
    <template v-if="loading">
      <div class="chart-layout">
        <div class="chart-card chart-card-bar">
          <div class="chart-card__header">
            <div class="chart-card__title">品牌 × 效率分布</div>
            <div class="chart-card__subtitle">按品牌统计高效、正常、延迟车辆分布</div>
          </div>
          <el-skeleton animated>
            <template #template>
              <el-skeleton-item variant="rect" style="width: 100%; height: 420px; border-radius: 12px;" />
            </template>
          </el-skeleton>
        </div>
        <div class="chart-card chart-card-pie">
          <div class="chart-card__header">
            <div class="chart-card__title">整体效率占比</div>
            <div class="chart-card__subtitle">高效、正常、延迟车辆数量比例</div>
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
            <div class="chart-card__title">品牌 × 效率分布</div>
            <div class="chart-card__subtitle">按品牌统计高效、正常、延迟车辆分布</div>
          </div>
          <div ref="barChartRef" class="chart-container"></div>
        </div>
        <div class="chart-card chart-card-pie">
          <div class="chart-card__header">
            <div class="chart-card__title">整体效率占比</div>
            <div class="chart-card__subtitle">高效、正常、延迟车辆数量比例</div>
          </div>
          <div ref="pieChartRef" class="pie-section__chart"></div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed, ref, onMounted, onUnmounted, watch, nextTick } from 'vue'
import * as echarts from 'echarts'

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
  if (props.brand) {
    filteredIndices = rawBrands
      .map((b, i) => (b === props.brand ? i : -1))
      .filter(i => i !== -1)
  }

  const brands = filteredIndices.map(i => rawBrands[i])
  const categories = rawCategories
  const seriesData = categories.map((_, catIndex) =>
    filteredIndices.map(brandIndex => Number(rawData[brandIndex]?.[catIndex] || 0))
  )

  return { brands, categories, data: seriesData }
})

const initCharts = () => {
  if (barChartRef.value) {
    barChartInstance = echarts.init(barChartRef.value, 'roro')
  }
  if (pieChartRef.value) {
    pieChartInstance = echarts.init(pieChartRef.value, 'roro')
  }
  updateCharts()
  window.addEventListener('resize', handleResize)
}

const updateCharts = () => {
  updateBarChart()
  updatePieChart()
}

const updateBarChart = () => {
  if (!barChartInstance) return

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
        start: 0,
        end: 100,
        bottom: 20
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
  if (!pieChartInstance) return

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
}, { deep: true })

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
</style>
