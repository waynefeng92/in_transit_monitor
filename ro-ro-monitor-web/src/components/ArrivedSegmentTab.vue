<template>
  <div class="arrived-segment-tab">
    <template v-if="loading">
      <el-skeleton animated>
        <template #template>
          <el-skeleton-item variant="rect" style="width: 100%; height: 520px; border-radius: 12px;" />
        </template>
      </el-skeleton>
    </template>
    <template v-else>
      <el-empty v-if="!hasData" description="暂无分段监控数据" />
      <div v-else ref="chartRef" class="segment-chart"></div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  chartData: {
    type: Object,
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

// 7个运输分段（排除 ARRIVED）
const SEGMENT_ORDER = [
  'NOT_DEPARTED',
  'TO_PORT',
  'AT_PORT_WAIT_SHIP',
  'ON_SEA',
  'AT_DEST_WAIT_UNLOAD',
  'UNLOADED_WAIT_DISPATCH',
  'DISPATCHING'
]

const SEGMENT_LABEL_MAP = {
  NOT_DEPARTED: '未出库',
  TO_PORT: '集港在途',
  AT_PORT_WAIT_SHIP: '已集港待装船',
  ON_SEA: '水运在途',
  AT_DEST_WAIT_UNLOAD: '已到港待卸船',
  UNLOADED_WAIT_DISPATCH: '已卸船待分拨',
  DISPATCHING: '分拨在途'
}

const BUCKET_CONFIG = [
      { key: 'efficient', label: '高效', color: '#67c23a' },
      { key: 'normal', label: '正常', color: '#409eff' },
      { key: 'delayed', label: '延迟', color: '#f56c6c' }
]

const chartRef = ref(null)
let chartInstance = null

const filteredData = computed(() => {
  // Backend returns matrix DTO; brand filtering is done server-side via query param
  return props.chartData || {}
})

const hasData = computed(() => {
  const dto = filteredData.value
  const data = dto.data || []
  if (!data || data.length === 0) return false
  return data.some(row =>
    (row || []).some(val => (val || 0) > 0)
  )
})

const processChartData = () => {
  const dto = filteredData.value
  const brands = dto.brands || []
  const matrix = dto.data || []

  // Build map: segment code -> row index in matrix
  const segmentIndexMap = new Map()
  brands.forEach((seg, idx) => {
    segmentIndexMap.set(seg, idx)
  })

  const xAxisData = SEGMENT_ORDER.map(seg => SEGMENT_LABEL_MAP[seg] || seg)
  const seriesData = {
    efficient: [],
    normal: [],
    delayed: []
  }

  SEGMENT_ORDER.forEach(seg => {
    const rowIdx = segmentIndexMap.get(seg)
    if (rowIdx !== undefined && matrix[rowIdx]) {
      const row = matrix[rowIdx]
      seriesData.efficient.push(row[0] || 0)
      seriesData.normal.push(row[1] || 0)
      seriesData.delayed.push(row[2] || 0)
    } else {
      seriesData.efficient.push(0)
      seriesData.normal.push(0)
      seriesData.delayed.push(0)
    }
  })

  return { xAxisData, seriesData }
}

const buildOption = () => {
  const { xAxisData, seriesData } = processChartData()

  const series = BUCKET_CONFIG.map(bucket => ({
    name: bucket.label,
    type: 'bar',
    stack: 'total',
    color: bucket.color,
    data: seriesData[bucket.key],
    barWidth: 44,
    barCategoryGap: '30%',
    itemStyle: {
        borderRadius: [2, 2, 2, 2]
    },
    label: {
      show: true,
      position: 'inside',
      color: '#fff',
      fontWeight: 'bold',
      fontSize: 12,
      formatter: ({ value }) => (value > 0 ? value : '')
    }
  }))

  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      backgroundColor: 'rgba(255, 255, 255, 0.96)',
      borderColor: '#dbe7f5',
      borderWidth: 1,
      borderRadius: 8,
      padding: [12, 14],
      textStyle: { color: '#22324d' },
      extraCssText: 'box-shadow: 0 12px 32px rgba(26, 65, 122, 0.12);',
      formatter: (params) => {
        if (!params.length) return ''
        let result = `<div style="font-weight:600;margin-bottom:6px;">${params[0].name}</div>`
        let total = 0
        params.forEach(item => {
          result += `${item.marker} ${item.seriesName}: ${item.value}<br/>`
          total += item.value
        })
        result += `<div style="margin-top:6px;padding-top:6px;border-top:1px solid #e8eef6;font-weight:600;">总计: ${total}</div>`
        return result
      }
    },
    legend: {
      data: BUCKET_CONFIG.map(b => b.label),
      orient: 'horizontal',
      left: 'center',
      top: 0,
      itemWidth: 12,
      itemHeight: 12,
      itemGap: 20,
      icon: 'roundRect',
      textStyle: {
        color: '#5e708d',
        fontSize: 13,
        padding: [0, 0, 0, 6]
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '14%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: xAxisData,
      axisLabel: {
        interval: 0,
        fontSize: 12,
        color: '#5e708d',
        margin: 14
      },
      axisLine: {
        lineStyle: { color: '#dbe7f5' }
      },
      axisTick: {
        show: false
      }
    },
    yAxis: {
      type: 'value',
      name: '车辆数量',
      nameTextStyle: {
        color: '#71839c',
        fontSize: 12,
        padding: [0, 0, 8, 0]
      },
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: {
        color: '#71839c',
        fontSize: 12
      },
      splitLine: {
        lineStyle: { color: '#e8eef6', type: 'dashed' }
      }
    },
    series
  }
}

const initChart = () => {
  if (!chartRef.value) return
  chartInstance = echarts.init(chartRef.value, 'roro')
  updateChart()
  window.addEventListener('resize', handleResize)
}

const updateChart = () => {
  if (!chartInstance) return
  if (!hasData.value) {
    chartInstance.setOption({
      title: {
        text: '暂无数据',
        left: 'center',
        top: 'center',
        textStyle: { color: '#999', fontSize: 14 }
      },
      xAxis: { type: 'category', data: [] },
      yAxis: { type: 'value' },
      series: []
    }, { notMerge: true })
    return
  }
  chartInstance.setOption(buildOption(), { notMerge: true })
}

const handleResize = () => {
  if (chartInstance) {
    chartInstance.resize()
  }
}

watch(() => props.chartData, () => {
  updateChart()
}, { deep: true })

watch(() => props.brand, () => {
  updateChart()
})

watch(() => props.loading, (val) => {
  if (!val) {
    setTimeout(() => {
      updateChart()
    }, 0)
  }
})

onMounted(() => {
  if (!props.loading) {
    initChart()
  }
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }
})
</script>

<style scoped>
.arrived-segment-tab {
  min-height: 200px;
}

.segment-chart {
  width: 100%;
  height: 520px;
}
</style>
