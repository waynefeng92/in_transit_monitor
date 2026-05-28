<template>
  <div ref="chartRef" :style="{ width: '100%', height: height }"></div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  chartData: {
    type: Array,
    default: () => []
  },
  selectedAlertStatus: {
    type: String,
    default: ''
  },
  height: {
    type: String,
    default: '320px'
  }
})

const chartRef = ref(null)
let chartInstance = null

const statusColorMap = {
  正常: '#67c23a',
  预警: '#e6a23c',
  超期: '#f56c6c'
}

const transportStatusPalette = [
  '#409eff',
  '#36cfc9',
  '#73d13d',
  '#ffc53d',
  '#ff7a45',
  '#9254de',
  '#597ef7',
  '#5cdbd3',
  '#95de64',
  '#ffd666'
]

const initChart = () => {
  if (!chartRef.value) return

  chartInstance = echarts.init(chartRef.value)
  updateChart()
  window.addEventListener('resize', handleResize)
}

const updateChart = () => {
  if (!chartInstance) return

  const data = buildPieData(props.chartData, props.selectedAlertStatus)
  const hasData = data.some(item => item.value > 0)

  if (!hasData) {
    chartInstance.setOption({
      title: {
        text: '暂无比例数据',
        left: 'center',
        top: 'center',
        textStyle: { color: '#999', fontSize: 14 }
      }
    }, { notMerge: true })
    return
  }

  chartInstance.setOption({
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(255, 255, 255, 0.96)',
      borderColor: '#dbe7f5',
      borderWidth: 1,
      borderRadius: 7,
      padding: [12, 14],
      textStyle: {
        color: '#22324d'
      },
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
          lineStyle: {
            color: '#b8c7db'
          }
        },
        data
      }
    ]
  }, { notMerge: true })
}

const buildPieData = (rawData, selectedAlertStatus) => {
  if (selectedAlertStatus) {
    const statusFieldMap = {
      正常: 'normal',
      预警: 'warn',
      超期: 'overdue'
    }
    const metricField = statusFieldMap[selectedAlertStatus]
    const transportTotals = new Map()

    rawData.forEach(item => {
      const transportStatus = item.transportStatus || '未知状态'
      const value = Number(item[metricField] || 0)
      transportTotals.set(transportStatus, (transportTotals.get(transportStatus) || 0) + value)
    })

    return Array.from(transportTotals.entries()).map(([name, value], index) => ({
      name,
      value,
      itemStyle: {
        color: transportStatusPalette[index % transportStatusPalette.length]
      }
    }))
  }

  const totals = {
    正常: 0,
    预警: 0,
    超期: 0
  }

  rawData.forEach(item => {
    totals.正常 += Number(item.normal || 0)
    totals.预警 += Number(item.warn || 0)
    totals.超期 += Number(item.overdue || 0)
  })

  return Object.entries(totals).map(([name, value]) => ({
    name,
    value,
    itemStyle: {
      color: statusColorMap[name]
    }
  }))
}

const handleResize = () => {
  if (chartInstance) {
    chartInstance.resize()
  }
}

watch(() => props.chartData, () => {
  updateChart()
}, { deep: true })

watch(() => props.selectedAlertStatus, () => {
  updateChart()
})

onMounted(() => {
  initChart()
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }
})
</script>
