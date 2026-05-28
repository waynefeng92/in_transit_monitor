<template>
  <div ref="chartRef" :style="{ width: '100%', height: height }"></div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  // 图表数据
  chartData: {
    type: Array,
    default: () => []
  },
  // 需要展示的监控状态
  activeStatuses: {
    type: Array,
    default: () => ['正常', '预警', '超期']
  },
  // 图表高度
  height: {
    type: String,
    default: '500px'
  }
})

const statusSeriesMap = {
  正常: { field: 'normal', color: '#67c23a' },
  预警: { field: 'warn', color: '#e6a23c' },
  超期: { field: 'overdue', color: '#f56c6c' }
}

const chartRef = ref(null)
let chartInstance = null

// 初始化图表
const initChart = () => {
  if (!chartRef.value) return
  
  chartInstance = echarts.init(chartRef.value)
  updateChart()
  
  // 监听窗口大小变化
  window.addEventListener('resize', handleResize)
}

// 更新图表
const updateChart = () => {
  if (!chartInstance) return
  
  // 如果没有数据，显示空状态
  if (!props.chartData || props.chartData.length === 0) {
    chartInstance.setOption({
      title: {
        text: '暂无数据',
        left: 'center',
        top: 'center',
        textStyle: { color: '#999', fontSize: 14 }
      }
    }, { notMerge: true })
    return
  }

  // 处理数据
  const { xAxisData, seriesData } = processData(props.chartData)
  const visibleStatuses = props.activeStatuses.filter(status => statusSeriesMap[status])
  const series = visibleStatuses.map(status => {
    const config = statusSeriesMap[status]
    return {
      name: status,
      type: 'bar',
      stack: 'total',
      color: config.color,
      data: seriesData[config.field],
      barWidth: 54,
      barCategoryGap: '34%',
      itemStyle: {
        borderRadius: [6, 6, 6, 6]
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
    // 工具箱
    toolbox: {
      feature: {
        saveAsImage: { title: '保存为图片' },
        dataView: { title: '数据视图', readOnly: true },
        restore: { title: '还原' }
      },
      right: 20
    },
    
    // 提示框
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      },
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
    
    // 图例
    legend: {
      data: visibleStatuses,
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
    
    // 网格
    grid: {
      left: '12%',
      right: '5%',
      bottom: '15%',
      top: '20%',
      containLabel: true
    },
    
    // X轴（在途状态）
    xAxis: {
      type: 'category',
      data: xAxisData,
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
    
    // Y轴
    yAxis: {
      type: 'value',
      name: '车辆数量',
      nameTextStyle: { fontSize: 12 },
      axisLine: {
        show: false
      },
      axisTick: {
        show: false
      },
      splitLine: {
        lineStyle: { color: '#e8eef6', type: 'dashed' }
      }
    },
    
    // 系列数据
    series,
    
    // 缩放
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
  
  chartInstance.setOption(option, { notMerge: true })
}

// 处理原始数据
const processData = (rawData) => {
  // 按品牌分组
  const brandMap = new Map()
  
  rawData.forEach(item => {
    const brand = item.brand
    const status = item.transportStatus
    const key = `${brand}|${status}`
    
    if (!brandMap.has(key)) {
      brandMap.set(key, {
        brand,
        status,
        normal: 0,
        warn: 0,
        overdue: 0
      })
    }
    
    const data = brandMap.get(key)
    data.normal += item.normal || 0
    data.warn += item.warn || 0
    data.overdue += item.overdue || 0
  })
  
  // 转换为数组并排序
  const processedData = Array.from(brandMap.values())
  processedData.sort((a, b) => {
    if (a.brand !== b.brand) {
      return a.brand.localeCompare(b.brand)
    }
    return a.status.localeCompare(b.status)
  })
  
  // 构建X轴数据和系列数据
  const xAxisData = processedData.map(item => `${item.brand}\n${item.status}`)
  const seriesData = {
    normal: processedData.map(item => item.normal),
    warn: processedData.map(item => item.warn),
    overdue: processedData.map(item => item.overdue)
  }
  
  return { xAxisData, seriesData }
}

// 窗口大小变化时重绘
const handleResize = () => {
  if (chartInstance) {
    chartInstance.resize()
  }
}

// 监听数据变化
watch(() => props.chartData, () => {
  updateChart()
}, { deep: true })

watch(() => props.activeStatuses, () => {
  updateChart()
}, { deep: true })

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
