import { mount } from '@vue/test-utils'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import StatusPieChart from '@/components/StatusPieChart.vue'

// Create mocks accessible from both the mocked module and test assertions
const { setOptionMock, initMock } = vi.hoisted(() => {
  const setOptionMock = vi.fn()
  const initMock = vi.fn(() => ({
    setOption: setOptionMock,
    resize: vi.fn(),
    dispose: vi.fn()
  }))
  return { setOptionMock, initMock }
})

vi.mock('echarts', () => ({
  init: initMock
}))

describe('StatusPieChart', () => {
  beforeEach(() => {
    setOptionMock.mockClear()
    initMock.mockClear()
  })

  it('renders a chart container with correct height prop', () => {
    const wrapper = mount(StatusPieChart, {
      props: { chartData: [], height: '400px' }
    })
    expect(wrapper.exists()).toBe(true)
    const div = wrapper.find('div')
    expect(div.attributes('style')).toContain('height: 400px')
  })

  it('uses default height when no height prop provided', () => {
    const wrapper = mount(StatusPieChart, {
      props: { chartData: [] }
    })
    const div = wrapper.find('div')
    expect(div.attributes('style')).toContain('height: 320px')
  })

  it('initializes echarts on mount', () => {
    mount(StatusPieChart, {
      props: { chartData: [] }
    })
    expect(initMock).toHaveBeenCalledTimes(1)
  })

  it('aggregates data and applies correct status colors when no alert filter', () => {
    const chartData = [
      { transportStatus: '在途', normal: 10, warn: 2, overdue: 1 },
      { transportStatus: '待发', normal: 5, warn: 0, overdue: 3 }
    ]
    mount(StatusPieChart, {
      props: { chartData }
    })

    const option = setOptionMock.mock.calls[0][0]
    expect(option.series).toBeDefined()
    const seriesData = option.series[0].data

    // Should aggregate by status type: 正常=15, 预警=2, 超期=4
    expect(seriesData).toHaveLength(3)

    const normal = seriesData.find(d => d.name === '正常')
    expect(normal.value).toBe(15)
    expect(normal.itemStyle.color).toBe('#67c23a') // --color-success

    const warn = seriesData.find(d => d.name === '预警')
    expect(warn.value).toBe(2)
    expect(warn.itemStyle.color).toBe('#e6a23c') // --color-warning

    const overdue = seriesData.find(d => d.name === '超期')
    expect(overdue.value).toBe(4)
    expect(overdue.itemStyle.color).toBe('#f56c6c') // --color-danger
  })

  it('uses transportStatusPalette when filtered by selectedAlertStatus', () => {
    const chartData = [
      { transportStatus: '上海港', normal: 5, warn: 3, overdue: 0 },
      { transportStatus: '宁波港', normal: 2, warn: 1, overdue: 0 }
    ]
    mount(StatusPieChart, {
      props: {
        chartData,
        selectedAlertStatus: '正常'
      }
    })

    const option = setOptionMock.mock.calls[0][0]
    const seriesData = option.series[0].data

    // Should aggregate by transportStatus, summing 'normal' values
    expect(seriesData).toHaveLength(2)
    expect(seriesData[0].name).toBe('上海港')
    expect(seriesData[0].value).toBe(5)
    // Uses transportStatusPalette (not statusColorMap)
    expect(seriesData[0].itemStyle.color).toBe('#409eff')

    expect(seriesData[1].name).toBe('宁波港')
    expect(seriesData[1].value).toBe(2)
  })

  it('shows empty state when all values are zero', () => {
    const chartData = [
      { transportStatus: '在途', normal: 0, warn: 0, overdue: 0 }
    ]
    mount(StatusPieChart, {
      props: { chartData }
    })

    const option = setOptionMock.mock.calls[0][0]
    // Empty state should display a title instead of series
    expect(option.title).toBeDefined()
    expect(option.title.text).toBe('暂无比例数据')
    expect(option.title.left).toBe('center')
    expect(option.title.top).toBe('center')
  })

  it('reacts to chartData changes', async () => {
    const wrapper = mount(StatusPieChart, {
      props: { chartData: [] }
    })

    // Initial mount calls setOption with empty state
    expect(setOptionMock).toHaveBeenCalledTimes(1)

    // Update chartData with real data
    await wrapper.setProps({
      chartData: [
        { transportStatus: '在途', normal: 3, warn: 0, overdue: 0 }
      ]
    })

    // setOption should be called again after props change
    expect(setOptionMock).toHaveBeenCalledTimes(2)
    const option = setOptionMock.mock.calls[1][0]
    expect(option.series[0].data[0].value).toBe(3)
  })
})
