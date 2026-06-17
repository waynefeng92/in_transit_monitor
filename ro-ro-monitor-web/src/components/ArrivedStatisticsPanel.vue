<template>
  <div class="arrived-statistics-panel">
    <template v-if="loading">
      <el-skeleton animated>
        <el-skeleton-item variant="rect" style="width: 100%; height: 400px; border-radius: 12px;" />
      </el-skeleton>
    </template>
    <div v-show="!loading" ref="chartRef" class="chart-container"></div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch, computed, nextTick } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  statisticsData: {
    type: Array,
    default: () => []
  },
  loading: {
    type: Boolean,
    default: false
  },
  period: {
    type: String,
    default: 'week'
  }
})

const chartRef = ref(null)
let chartInstance = null

const periods = computed(() => props.statisticsData.map(item => item.period))
const arrivalCounts = computed(() => props.statisticsData.map(item => item.arrivalCount || 0))
const avgEfficiencies = computed(() => props.statisticsData.map(item => item.avgEfficiency || 0))

const yAxisMax = computed(() => {
  const maxVal = Math.max(...avgEfficiencies.value, 100)
  return Math.ceil((maxVal + 10) / 10) * 10
})

const initChart = () => {
  if (!chartRef.value || chartInstance) return
  chartInstance = echarts.init(chartRef.value, 'roro')
  window.addEventListener('resize', handleResize)
}

const updateChart = () => {
  if (!chartInstance) return

  if (!props.statisticsData || props.statisticsData.length === 0) {
    chartInstance.setOption({
      title: {
        text: '暂无数据',
        left: 'center',
        top: 'center',
        textStyle: { color: '#999', fontSize: 14 }
      },
      xAxis: { show: false },
      yAxis: { show: false },
      series: [],
      graphic: []
    }, { notMerge: true })
    return
  }

  const isWeek = props.period === 'week'
  const dataCount = props.statisticsData.length
  const shouldRotate = isWeek && dataCount > 12

  const option = {
    title: { show: false },
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.96)',
      borderColor: '#dbe7f5',
      borderWidth: 1,
      borderRadius: 7,
      padding: [12, 14],
      textStyle: {
        color: '#22324d'
      },
      extraCssText: 'box-shadow: 0 12px 32px rgba(26, 65, 122, 0.12);',
      formatter: (params) => {
        let result = `<div style="font-weight:600;margin-bottom:6px;">${params[0].name}</div>`
        params.forEach(item => {
          const suffix = item.seriesName === '平均效率' ? '%' : ''
          result += `<div style="display:flex;align-items:center;gap:6px;margin-top:4px;">
            <span style="display:inline-block;width:10px;height:10px;border-radius:2px;background:${item.color};"></span>
            <span>${item.seriesName}: <b>${item.value}${suffix}</b></span>
          </div>`
        })
        return result
      }
    },
    legend: {
      data: ['到港数量', '平均效率'],
      top: 10,
      left: 'center',
      itemWidth: 14,
      itemHeight: 10,
      itemGap: 24,
      icon: 'roundRect',
      textStyle: {
        color: '#5e708d',
        fontSize: 13,
        padding: [0, 0, 0, 4]
      }
    },
    grid: {
      left: '4%',
      right: '4%',
      bottom: '10%',
      top: '16%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: periods.value,
      axisLabel: {
        color: '#5e708d',
        fontSize: 12,
        interval: 0,
        rotate: shouldRotate ? 45 : 0
      },
      axisLine: {
        lineStyle: { color: '#dbe7f5' }
      },
      axisTick: {
        show: false
      }
    },
    yAxis: [
      {
        type: 'value',
        name: '到港数量',
        nameTextStyle: {
          color: '#5e708d',
          fontSize: 12,
          padding: [0, 0, 0, -10]
        },
        axisLabel: {
          color: '#5e708d',
          fontSize: 12
        },
        axisLine: { show: false },
        axisTick: { show: false },
        splitLine: {
          lineStyle: { color: '#e8eef6', type: 'dashed' }
        }
      },
      {
        type: 'value',
        name: '平均效率 (%)',
        min: 0,
        max: yAxisMax.value,
        nameTextStyle: {
          color: '#5e708d',
          fontSize: 12,
          padding: [0, -10, 0, 0]
        },
        axisLabel: {
          color: '#5e708d',
          fontSize: 12,
          formatter: '{value}%'
        },
        axisLine: { show: false },
        axisTick: { show: false },
        splitLine: { show: false }
      }
    ],
    series: [
      {
        name: '到港数量',
        type: 'bar',
        data: arrivalCounts.value,
        barWidth: '36%',
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#409eff' },
            { offset: 1, color: '#1d72f3' }
          ]),
          borderRadius: [2, 2, 0, 0]
        },
        label: {
          show: true,
          position: 'top',
          color: '#1d72f3',
          fontSize: 11,
          fontWeight: 'bold'
        }
      },
      {
        name: '平均效率',
        type: 'line',
        yAxisIndex: 1,
        data: avgEfficiencies.value,
        smooth: true,
        symbol: 'circle',
        symbolSize: 8,
        lineStyle: {
          color: '#67c23a',
          width: 3
        },
        itemStyle: {
          color: '#67c23a',
          borderWidth: 2,
          borderColor: '#fff'
        },
        label: {
          show: true,
          position: 'top',
          color: '#67c23a',
          fontSize: 11,
          fontWeight: 'bold',
          formatter: '{c}%'
        },
        markLine: {
          symbol: 'none',
          silent: true,
          label: {
            position: 'insideEndTop',
            formatter: '{b}',
            fontSize: 11,
            color: '#fff',
            padding: [2, 6],
            borderRadius: 4
          },
          lineStyle: {
            type: 'dashed',
            width: 2
          },
          data: [
            {
              yAxis: 80,
              name: '预警阈值 80%',
              lineStyle: { color: '#e6a23c' },
              label: { backgroundColor: '#e6a23c' }
            },
            {
              yAxis: 100,
              name: 'OTD阈值 100%',
              lineStyle: { color: '#67c23a' },
              label: { backgroundColor: '#67c23a' }
            }
          ]
        }
      }
    ]
  }

  chartInstance.setOption(option, { notMerge: true })
}

const handleResize = () => {
  chartInstance?.resize()
}

watch(() => props.statisticsData, () => {
  nextTick(updateChart)
}, { deep: true })

watch(() => props.period, () => {
  nextTick(updateChart)
})

watch(() => props.loading, (val) => {
  if (!val) {
    nextTick(() => {
      initChart()
      updateChart()
      chartInstance?.resize()
    })
  }
})

onMounted(() => {
  if (!props.loading) {
    initChart()
    updateChart()
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
.arrived-statistics-panel {
  min-height: 200px;
}
.chart-container {
  width: 100%;
  height: 400px;
}
</style>
