/**
 * roro — ECharts 自定义主题
 * 匹配项目蓝色主色调设计系统
 *
 * 导入方式（在 main.js 中全局导入一次即可）:
 *   import './utils/echarts-theme'
 */
import * as echarts from 'echarts'

// ── 项目调色板 ──────────────────────────────────────
// 取自 style.css CSS 变量:
//   --color-primary: #1d72f3 (蓝)
//   --color-success: #67c23a (绿)
//   --color-warning: #e6a23c (橙)
//   --color-danger:  #f56c6c (红)
//   --color-info:    #909399 (灰)
//   --sidebar-active, --color-primary-light
// ─────────────────────────────────────────────────────
const palette = ['#1d72f3', '#67c23a', '#e6a23c', '#f56c6c', '#409eff', '#3884f7', '#909399']

const roroTheme = {
  color: palette,

  backgroundColor: 'transparent',

  textStyle: {
    fontFamily: "'PingFang SC', 'Microsoft YaHei', sans-serif",
    fontSize: 13,
    color: '#5e708d'
  },

  // ── 标题 ──
  title: {
    textStyle: {
      fontSize: 16,
      color: '#13233c'
    },
    subtextStyle: {
      fontSize: 12,
      color: '#7a8aa2'
    }
  },

  // ── 图例 ──
  legend: {
    textStyle: {
      color: '#5e708d',
      fontSize: 12
    },
    pageTextStyle: {
      color: '#5e708d'
    }
  },

  // ── 提示框 ──
  tooltip: {
    backgroundColor: 'rgba(255,255,255,0.95)',
    borderColor: '#dbe7f5',
    borderWidth: 1,
    textStyle: {
      color: '#13233c',
      fontSize: 13
    },
    extraCssText: 'border-radius: 8px; box-shadow: 0 4px 12px rgba(26,65,122,0.12);'
  },

  // ── 网格 ──
  grid: {
    containLabel: true,
    borderColor: '#dbe7f5'
  },

  // ── 类目轴 ──
  categoryAxis: {
    axisLine: {
      lineStyle: { color: '#dbe7f5' }
    },
    axisTick: {
      lineStyle: { color: '#dbe7f5' }
    },
    axisLabel: {
      color: '#5e708d',
      fontSize: 12
    },
    splitLine: {
      show: false
    }
  },

  // ── 数值轴 ──
  valueAxis: {
    axisLine: { show: false },
    axisTick: { show: false },
    axisLabel: {
      color: '#7a8aa2',
      fontSize: 11
    },
    splitLine: {
      lineStyle: {
        color: '#e9f0f8',
        type: 'dashed'
      }
    }
  }
}

// ── 全局注册 ──
echarts.registerTheme('roro', roroTheme)

export default roroTheme
