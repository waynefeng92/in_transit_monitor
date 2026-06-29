<template>
  <div ref="chartRef" :style="{ width: '100%', height: height }"></div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  // 段详情数据数组（VehicleDetailDTO.segments）
  segments: {
    type: Array,
    default: () => []
  },
  // 图表高度
  height: {
    type: String,
    default: '400px'
  }
})

// ── 状态颜色映射，与 StackedBarChart line 28-31 保持一致 ──
const STATUS_COLOR_MAP = {
  NORMAL: '#67c23a',
  WARN: '#e6a23c',
  OVERDUE: '#f56c6c',
  PENDING: '#909399',
  'N/A': '#909399'
}

const STATUS_LABEL_MAP = {
  NORMAL: '正常',
  WARN: '预警',
  OVERDUE: '超期',
  PENDING: '未开始',
  'N/A': '无数据'
}

const chartRef = ref(null)
let chartInstance = null

// ── 时间格式化 ──
const formatTime = (isoStr) => {
  if (!isoStr) return '-'
  const d = new Date(isoStr)
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

const formatDuration = (hours) => {
  if (hours == null) return '-'
  if (hours < 1) return `${Math.round(hours * 60)} 分钟`
  return `${hours.toFixed(1)} 小时`
}

// ── 计算时间轴范围 ──
const computeTimeRange = (segments) => {
  const now = Date.now()
  let minTime = now
      let maxTime = 0

  segments.forEach((seg) => {
    if (seg.startTime) {
      const t = new Date(seg.startTime).getTime()
      if (t < minTime) minTime = t
    }
    if (seg.endTime) {
      const t = new Date(seg.endTime).getTime()
      if (t > maxTime) maxTime = t
    }
  })

  // 有真正进行中的段（endTime=null 且 actualDurationHours 已设置）→ 扩展到当前时间
  const hasInProgress = segments.some((s) => s.startTime && !s.endTime && s.actualDurationHours != null)
  if (hasInProgress) maxTime = Math.max(maxTime, now)

  // 添加 5% 内边距，至少 30 分钟
  const range = maxTime - minTime || 3600000
  const padding = Math.max(range * 0.05, 1800000)
  return { min: minTime - padding, max: maxTime + padding }
}

// ── 初始化图表 ──
const initChart = () => {
  if (!chartRef.value) return

  chartInstance = echarts.init(chartRef.value, 'roro')
  updateChart()

  window.addEventListener('resize', handleResize)
}

// ── 更新图表 ──
const updateChart = () => {
  if (!chartInstance) return

  if (!props.segments || props.segments.length === 0) {
    chartInstance.setOption(
      {
        title: {
          text: '暂无数据',
          left: 'center',
          top: 'center',
          textStyle: { color: '#999', fontSize: 14 }
        }
      },
      { notMerge: true }
    )
    return
  }

  const segments = [...props.segments].sort((a, b) => (a.segmentIndex || 0) - (b.segmentIndex || 0))
  const timeRange = computeTimeRange(segments)

  // y 轴类目：所有段名（inverse 使段 1 在顶部）
  const yAxisData = segments.map((s) => s.segmentName)

  // 图例数据
  const legendData = ['正常', '预警', '超期', '进行中/无数据']

  const option = {
    // ── 图例 ──
    legend: {
      data: legendData,
      orient: 'horizontal',
      left: 'center',
      top: 5,
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

    // ── 提示框 ──
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
      formatter: (params) => {
        const seg = segments[params.dataIndex]
        if (!seg) return ''

        const statusLabel = STATUS_LABEL_MAP[seg.status] || seg.status
        const color = STATUS_COLOR_MAP[seg.status] || '#909399'

        let html = `<div style="font-weight:600;margin-bottom:6px;">🔹 ${seg.segmentName}</div>`
        html += `<div style="margin:3px 0;">开始时间：${formatTime(seg.startTime)}</div>`
        html += `<div style="margin:3px 0;">结束时间：${seg.endTime ? formatTime(seg.endTime) : (seg.actualDurationHours != null ? '<span style="color:#e6a23c">进行中</span>' : '<span style="color:#909399">—</span>')}</div>`
        html += `<div style="margin:3px 0;">实际耗时：${formatDuration(seg.actualDurationHours)}</div>`
        html += `<div style="margin:3px 0;">标准 OTD：${seg.standardOtdHours != null && seg.standardOtdHours > 0 ? seg.standardOtdHours.toFixed(1) + ' 小时' : '—'}</div>`
        html += `<div style="margin:3px 0;">预警阈值：${seg.warnThresholdHours != null && seg.warnThresholdHours > 0 ? seg.warnThresholdHours.toFixed(1) + ' 小时' : '—'}</div>`
        html += `<div style="margin-top:6px;font-weight:500;color:${color}">状态：${statusLabel}</div>`
        return html
      }
    },

    // ── 网格 ──
    grid: {
      left: '15%',
      right: '5%',
      bottom: '12%',
      top: '15%',
      containLabel: true
    },

    // ── X 轴（时间） ──
    xAxis: {
      type: 'time',
      min: timeRange.min,
      max: timeRange.max,
      axisLabel: {
        fontSize: 11,
        formatter: (value) => {
          const d = new Date(value)
          const pad = (n) => String(n).padStart(2, '0')
          return `${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
        }
      },
      axisLine: {
        lineStyle: { color: '#999' }
      },
      splitLine: {
        lineStyle: { color: '#e8eef6', type: 'dashed' }
      }
    },

    // ── Y 轴（段名称） ──
    yAxis: {
      type: 'category',
      data: yAxisData,
      inverse: true,
      axisLabel: {
        fontSize: 12
      },
      axisLine: {
        lineStyle: { color: '#999' }
      },
      axisTick: {
        show: false
      }
    },

    // ── 缩放 ──
    dataZoom: [
      {
        type: 'slider',
        start: 0,
        end: 100,
        bottom: 10,
        height: 20,
        handleSize: '80%',
        handleStyle: { color: '#1d72f3' }
      },
      {
        type: 'inside',
        start: 0,
        end: 100
      }
    ],

    // ── 自定义系列（Gantt 条形） ──
    series: [
      {
        type: 'custom',
        renderItem: (params, api) => {
          const seg = segments[params.dataIndex]

          // 未来段（无开始时间）→ 跳过渲染
          if (!seg.startTime) return
          // 隐式完成段（有开始时间但无 endTime 且无 actualDurationHours）→ 跳过渲染
          if (!seg.endTime && seg.actualDurationHours == null) return

          const startTs = new Date(seg.startTime).getTime()
          const endTs = seg.endTime ? new Date(seg.endTime).getTime() : Date.now()
          const categoryIndex = params.dataIndex

          const startCoord = api.coord([startTs, categoryIndex])
          const endCoord = api.coord([endTs, categoryIndex])

          const barHeight = api.size([0, 1])[1] * 0.55
          const y = startCoord[1] - barHeight / 2
          const width = Math.max(endCoord[0] - startCoord[0], 2)

          const color = STATUS_COLOR_MAP[seg.status] || '#909399'
          const isInProgress = !seg.endTime && seg.actualDurationHours != null

          return {
            type: 'rect',
            shape: {
              x: startCoord[0],
              y: y,
              width: width,
              height: barHeight
            },
            style: isInProgress
              ? { fill: 'transparent', stroke: color, lineWidth: 2, lineDash: [6, 4] }
              : { fill: color },
            // 非进行中段 → 加圆角
            shape: isInProgress
              ? { x: startCoord[0], y: y, width: width, height: barHeight }
              : { x: startCoord[0], y: y, width: width, height: barHeight, r: 4 }
          }
        },
        data: segments.map((seg, idx) => ({
          name: seg.segmentName,
          value: [
            seg.startTime ? new Date(seg.startTime).getTime() : 0,
            seg.endTime
              ? new Date(seg.endTime).getTime()
              : seg.actualDurationHours != null
                ? Date.now()
                : seg.startTime
                  ? new Date(seg.startTime).getTime()
                  : 0,
            idx
          ]
        })),
        encode: {
          x: [0, 1],
          y: 2
        }
      }
    ]
  }

  chartInstance.setOption(option, { notMerge: true })
}

// ── 窗口大小变化时重绘 ──
const handleResize = () => {
  if (chartInstance) {
    chartInstance.resize()
  }
}

// ── 监听数据变化 ──
watch(
  () => props.segments,
  () => {
    updateChart()
  },
  { deep: true }
)

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
