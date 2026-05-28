<template>
  <div class="dashboard">
    <section class="dashboard-monitor-tabs">
      <el-tabs v-model="activeMonitorTab" @tab-change="handleTabChange">
        <el-tab-pane label="分段监控" name="segment" />
        <el-tab-pane label="整段监控" name="overall" />
        <el-tab-pane label="三段监控" name="three-section" />
      </el-tabs>
    </section>
    <div v-if="activeMonitorTab === 'segment'">
    <section class="dashboard-time-filter">
      <div class="time-filter__label">订单释放时间</div>
      <el-date-picker
        v-model="dateRange"
        type="daterange"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        value-format="YYYY-MM-DDTHH:mm:ss"
        class="time-filter__picker"
      />
      <el-button type="primary" @click="loadData">查询</el-button>
      <el-button text class="filter-reset" @click="resetFilters">重置</el-button>
    </section>

    <section class="dashboard-summary">
      <template v-if="initialLoading">
        <div v-for="i in 4" :key="i" class="summary-card">
          <el-skeleton animated>
            <template #template>
              <div style="padding: 22px 24px;">
                <el-skeleton-item variant="text" style="width: 40%; height: 18px;" />
                <el-skeleton-item variant="text" style="width: 60%; height: 44px; margin: 14px 0 18px;" />
                <el-skeleton-item variant="text" style="width: 50%; height: 14px;" />
              </div>
            </template>
          </el-skeleton>
        </div>
      </template>
      <template v-else>
        <div
          v-for="item in summaryCards"
          :key="item.key"
          class="summary-card"
          :class="`summary-card-${item.key}`"
        >
          <div class="summary-card__label">{{ item.label }}</div>
          <div class="summary-card__value" :class="`summary-card__value-${item.key}`">{{ item.value }}</div>
          <div class="summary-card__meta">{{ item.meta }}</div>
        </div>
      </template>
    </section>

    <section class="dashboard-panel dashboard-panel-chart">
      <div class="panel-header">
        <div>
          <div class="panel-title">品牌 × 在途状态 监控图表</div>
        <div class="panel-subtitle">按品牌和运输状态统计正常、预警、超期车辆分布</div>
        </div>
        <div class="panel-actions">
          <el-tag effect="plain" round class="panel-tag">在途监控</el-tag>
          <el-button type="primary" @click="loadData" :loading="loading">
            <el-icon><Refresh /></el-icon>
            刷新数据
          </el-button>
        </div>
      </div>

      <div class="chart-filters">
        <el-select
          v-model="selectedBrand"
          clearable
          filterable
          placeholder="筛选品牌"
          class="chart-filter"
        >
          <el-option
            v-for="brand in brandOptions"
            :key="brand"
            :label="brand"
            :value="brand"
          />
        </el-select>
        <el-select
          v-model="selectedAlertStatus"
          clearable
          filterable
          placeholder="筛选监控状态"
          class="chart-filter"
        >
          <el-option
            v-for="status in monitorStatusOptions"
            :key="status"
            :label="status"
            :value="status"
          />
        </el-select>
        <el-button text class="filter-reset" @click="resetFilters">
          重置
        </el-button>
      </div>

      <template v-if="initialLoading">
        <div class="chart-layout">
          <div class="chart-card chart-card-bar">
            <div class="chart-card__header">
              <div class="chart-card__title">{{ barSectionTitle }}</div>
              <div class="chart-card__subtitle">按品牌与在途状态查看监控数量</div>
            </div>
            <el-skeleton animated>
              <el-skeleton-item variant="rect" style="width: 100%; height: 420px; border-radius: 12px;" />
            </el-skeleton>
          </div>
          <div class="chart-card chart-card-pie">
            <div class="chart-card__header">
              <div class="chart-card__title">{{ pieSectionTitle }}</div>
              <div class="chart-card__subtitle">{{ pieSectionSubtitle }}</div>
            </div>
            <el-skeleton animated>
              <el-skeleton-item variant="rect" style="width: 100%; height: 300px; border-radius: 12px;" />
            </el-skeleton>
          </div>
        </div>
      </template>
      <template v-else>
        <div class="chart-layout">
          <div class="chart-card chart-card-bar">
            <div class="chart-card__header">
              <div class="chart-card__title">{{ barSectionTitle }}</div>
              <div class="chart-card__subtitle">按品牌与在途状态查看监控数量</div>
            </div>
            <div class="chart-container">
              <StackedBarChart
                :chartData="chartDisplayData"
                :active-statuses="activeChartStatuses"
                height="460px"
              />
            </div>
          </div>

          <div class="chart-card chart-card-pie">
            <div class="chart-card__header">
              <div class="chart-card__title">{{ pieSectionTitle }}</div>
              <div class="chart-card__subtitle">{{ pieSectionSubtitle }}</div>
            </div>
            <div class="pie-section__chart">
              <StatusPieChart
                :chartData="filteredChartData"
                :selected-alert-status="selectedAlertStatus"
                height="320px"
              />
            </div>
          </div>
        </div>
      </template>
    </section>

    <section class="dashboard-panel dashboard-panel-detail">
      <div class="panel-header">
        <div>
          <div class="panel-title">详细数据</div>
          <div class="panel-subtitle">展示各品牌在不同在途状态下的监控明细</div>
        </div>
        <el-button text class="detail-toggle" @click="showDetail = !showDetail">
          {{ showDetail ? '收起明细' : '展开明细' }}
        </el-button>
      </div>

      <el-collapse-transition>
        <div v-show="showDetail" class="detail-table-wrap">
          <el-table :data="filteredChartData" border stripe size="small" max-height="420">
            <el-table-column prop="brand" label="品牌" min-width="120" fixed />
            <el-table-column prop="transportStatus" label="在途状态" min-width="150" />
            <el-table-column prop="normal" label="正常" width="100" align="center">
              <template #default="{ row }">
                <el-tag type="success" effect="dark">{{ row.normal }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="warn" label="预警" width="100" align="center">
              <template #default="{ row }">
                <el-tag type="warning" effect="dark">{{ row.warn }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="overdue" label="超期" width="100" align="center">
              <template #default="{ row }">
                <el-tag type="danger" effect="dark">{{ row.overdue }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="合计" width="100" align="center">
              <template #default="{ row }">
                {{ row.normal + row.warn + row.overdue }}
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-collapse-transition>
    </section>
    </div>

    <div v-if="activeMonitorTab === 'overall'" class="overall-view">
      <!-- Overall summary cards -->
      <section class="dashboard-summary">
        <template v-if="overallInitialLoading">
          <div v-for="i in 4" :key="i" class="summary-card">
            <el-skeleton animated>
              <template #template>
                <div style="padding: 22px 24px;">
                  <el-skeleton-item variant="text" style="width: 40%; height: 18px;" />
                  <el-skeleton-item variant="text" style="width: 60%; height: 44px; margin: 14px 0 18px;" />
                  <el-skeleton-item variant="text" style="width: 50%; height: 14px;" />
                </div>
              </template>
            </el-skeleton>
          </div>
        </template>
        <template v-else>
          <div v-for="item in overallSummaryCards" :key="item.key"
            class="summary-card" :class="`summary-card-${item.key}`">
            <div class="summary-card__label">{{ item.label }}</div>
            <div class="summary-card__value" :class="`summary-card__value-${item.key}`">{{ item.value }}</div>
            <div class="summary-card__meta">{{ item.meta }}</div>
          </div>
        </template>
      </section>

      <!-- Overall brand status chart panel -->
      <section class="dashboard-panel dashboard-panel-chart">
        <div class="panel-header">
          <div>
            <div class="panel-title">品牌 × 整段监控状态</div>
            <div class="panel-subtitle">按品牌统计整段正常、预警、超期车辆分布</div>
          </div>
          <div class="panel-actions">
            <el-tag effect="plain" round class="panel-tag">整段监控</el-tag>
            <el-button type="primary" @click="loadData" :loading="loading">
              <el-icon><Refresh /></el-icon>
              刷新数据
            </el-button>
          </div>
        </div>

        <div class="chart-filters">
          <el-select v-model="overallSelectedBrand" clearable filterable placeholder="筛选品牌" class="chart-filter">
            <el-option v-for="brand in overallBrandOptions" :key="brand" :label="brand" :value="brand" />
          </el-select>
          <el-select v-model="overallSelectedAlertStatus" clearable filterable placeholder="筛选监控状态" class="chart-filter">
            <el-option v-for="status in monitorStatusOptions" :key="status" :label="status" :value="status" />
          </el-select>
          <el-button text class="filter-reset" @click="overallResetFilters">重置</el-button>
        </div>

        <template v-if="overallInitialLoading">
          <div class="chart-layout">
            <div class="chart-card chart-card-bar">
              <div class="chart-card__header">
                <div class="chart-card__title">{{ overallBarSectionTitle }}</div>
                <div class="chart-card__subtitle">按品牌查看整段监控数量</div>
              </div>
              <el-skeleton animated>
                <el-skeleton-item variant="rect" style="width: 100%; height: 420px; border-radius: 12px;" />
              </el-skeleton>
            </div>
            <div class="chart-card chart-card-pie">
              <div class="chart-card__header">
                <div class="chart-card__title">{{ overallPieSectionTitle }}</div>
                <div class="chart-card__subtitle">{{ overallPieSectionSubtitle }}</div>
              </div>
              <el-skeleton animated>
                <el-skeleton-item variant="rect" style="width: 100%; height: 300px; border-radius: 12px;" />
              </el-skeleton>
            </div>
          </div>
        </template>
        <template v-else>
          <div class="chart-layout">
            <div class="chart-card chart-card-bar">
              <div class="chart-card__header">
                <div class="chart-card__title">{{ overallBarSectionTitle }}</div>
                <div class="chart-card__subtitle">按品牌查看整段监控数量</div>
              </div>
              <div class="chart-container">
                <StackedBarChart
                  :chartData="overallChartDisplayData"
                  :active-statuses="overallActiveChartStatuses"
                  height="460px"
                />
              </div>
            </div>
            <div class="chart-card chart-card-pie">
              <div class="chart-card__header">
                <div class="chart-card__title">{{ overallPieSectionTitle }}</div>
                <div class="chart-card__subtitle">{{ overallPieSectionSubtitle }}</div>
              </div>
              <div class="pie-section__chart">
                <StatusPieChart
                  :chartData="overallChartDisplayData"
                  :selected-alert-status="overallSelectedAlertStatus"
                  height="320px"
                />
              </div>
            </div>
          </div>
        </template>

        <!-- Detail table -->
        <div style="margin-top: 20px;">
          <el-table :data="overallFilteredChartTableData" border stripe size="small" max-height="420">
            <el-table-column prop="brand" label="品牌" min-width="120" />
            <el-table-column prop="normal" label="正常" width="120" align="center">
              <template #default="{ row }">
                <el-tag type="success" effect="dark">{{ row.normal }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="warn" label="预警" width="120" align="center">
              <template #default="{ row }">
                <el-tag type="warning" effect="dark">{{ row.warn }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="overdue" label="超期" width="120" align="center">
              <template #default="{ row }">
                <el-tag type="danger" effect="dark">{{ row.overdue }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="合计" width="120" align="center">
              <template #default="{ row }">
                {{ (row.normal || 0) + (row.warn || 0) + (row.overdue || 0) }}
              </template>
            </el-table-column>
          </el-table>
        </div>
      </section>
    </div>

    <div v-if="activeMonitorTab === 'three-section'" class="three-section-view">
      <section class="dashboard-summary">
        <template v-if="sectionInitialLoading">
          <div v-for="i in 3" :key="i" class="summary-card">
            <el-skeleton animated>
              <template #template>
                <div style="padding: 22px 24px;">
                  <el-skeleton-item variant="text" style="width: 40%; height: 18px;" />
                  <el-skeleton-item variant="text" style="width: 60%; height: 44px; margin: 14px 0 18px;" />
                  <el-skeleton-item variant="text" style="width: 50%; height: 14px;" />
                </div>
              </template>
            </el-skeleton>
          </div>
        </template>
        <template v-else>
          <div
            v-for="item in sectionSummaryCards"
            :key="item.key"
            class="summary-card"
            :class="`summary-card-${item.key}`"
          >
            <div class="summary-card__label">{{ item.label }}</div>
            <div class="summary-card__value" :class="`summary-card__value-${item.key}`">{{ item.value }}</div>
            <div class="summary-card__meta">{{ item.meta }}</div>
          </div>
        </template>
      </section>

      <section class="dashboard-panel dashboard-panel-chart">
        <div class="panel-header">
          <div>
            <div class="panel-title">{{ sectionDrillDown ? `${sectionDrillDown} - 品牌监控图表` : '三段监控状态图表' }}</div>
            <div class="panel-subtitle">{{ sectionDrillDown ? '按品牌查看监控状态分布' : '按段查看正常、预警、超期车辆分布' }}</div>
          </div>
          <div class="panel-actions">
            <el-tag effect="plain" round class="panel-tag">三段监控</el-tag>
            <el-button v-if="sectionDrillDown" text @click="goBackToSectionView">
              <el-icon><ArrowLeft /></el-icon>
              返回段级视图
            </el-button>
            <el-button type="primary" @click="loadData" :loading="loading">
              <el-icon><Refresh /></el-icon>
              刷新数据
            </el-button>
          </div>
        </div>

        <div class="chart-filters">
          <el-select
            v-if="sectionDrillDown"
            v-model="sectionSelectedBrand"
            clearable
            filterable
            placeholder="筛选品牌"
            class="chart-filter"
          >
            <el-option
              v-for="brand in sectionBrandOptions"
              :key="brand"
              :label="brand"
              :value="brand"
            />
          </el-select>
          <el-select
            v-model="sectionSelectedAlertStatus"
            clearable
            filterable
            placeholder="筛选监控状态"
            class="chart-filter"
          >
            <el-option
              v-for="status in monitorStatusOptions"
              :key="status"
              :label="status"
              :value="status"
            />
          </el-select>
          <el-button text class="filter-reset" @click="sectionResetFilters">
            重置
          </el-button>
        </div>

        <template v-if="sectionInitialLoading">
          <div class="chart-layout">
            <div class="chart-card chart-card-bar">
              <div class="chart-card__header">
                <div class="chart-card__title">{{ sectionBarTitle }}</div>
                <div class="chart-card__subtitle">按段查看正常、预警、超期数量</div>
              </div>
              <el-skeleton animated>
                <el-skeleton-item variant="rect" style="width: 100%; height: 420px; border-radius: 12px;" />
              </el-skeleton>
            </div>
            <div class="chart-card chart-card-pie">
              <div class="chart-card__header">
                <div class="chart-card__title">{{ sectionPieTitle }}</div>
                <div class="chart-card__subtitle">{{ sectionPieSubtitle }}</div>
              </div>
              <el-skeleton animated>
                <el-skeleton-item variant="rect" style="width: 100%; height: 300px; border-radius: 12px;" />
              </el-skeleton>
            </div>
          </div>
        </template>
        <template v-else>
          <div class="chart-layout">
            <div class="chart-card chart-card-bar">
              <div class="chart-card__header">
                <div class="chart-card__title">{{ sectionBarTitle }}</div>
                <div class="chart-card__subtitle">{{ sectionDrillDown ? '按品牌与监控状态查看数量' : '按段查看正常、预警、超期数量' }}</div>
              </div>
              <div class="chart-container">
                <StackedBarChart
                  ref="sectionBarChartRef"
                  :chartData="sectionChartDisplayData"
                  :active-statuses="sectionActiveChartStatuses"
                  height="460px"
                />
              </div>
            </div>

            <div class="chart-card chart-card-pie">
              <div class="chart-card__header">
                <div class="chart-card__title">{{ sectionPieTitle }}</div>
                <div class="chart-card__subtitle">{{ sectionPieSubtitle }}</div>
              </div>
              <div class="pie-section__chart">
                <StatusPieChart
                  :chartData="sectionPieChartData"
                  :selected-alert-status="sectionSelectedAlertStatus"
                  height="320px"
                />
              </div>
            </div>
          </div>
        </template>

        <!-- Detail table -->
        <div style="margin-top: 20px;">
          <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px;">
            <div>
              <div class="panel-title" style="font-size: 16px;">详细数据</div>
              <div class="panel-subtitle" style="margin-top: 4px;">{{ sectionDrillDown ? '展示各品牌监控明细' : '展示各段监控明细' }}</div>
            </div>
            <el-button text class="detail-toggle" @click="sectionDetailShow = !sectionDetailShow">
              {{ sectionDetailShow ? '收起明细' : '展开明细' }}
            </el-button>
          </div>
          <el-collapse-transition>
            <div v-show="sectionDetailShow" class="detail-table-wrap">
              <el-table :data="sectionTableData" border stripe size="small" max-height="420">
                <el-table-column prop="brand" :label="sectionDrillDown ? '品牌' : '段名称'" min-width="120" fixed />
                <el-table-column prop="transportStatus" label="监控类型" min-width="150" />
                <el-table-column prop="normal" label="正常" width="100" align="center">
                  <template #default="{ row }">
                    <el-tag type="success" effect="dark">{{ row.normal }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="warn" label="预警" width="100" align="center">
                  <template #default="{ row }">
                    <el-tag type="warning" effect="dark">{{ row.warn }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="overdue" label="超期" width="100" align="center">
                  <template #default="{ row }">
                    <el-tag type="danger" effect="dark">{{ row.overdue }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="合计" width="100" align="center">
                  <template #default="{ row }">
                    {{ row.normal + row.warn + row.overdue }}
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </el-collapse-transition>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, reactive, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { Refresh, ArrowLeft } from '@element-plus/icons-vue'
import request from '@/api/request'
import StackedBarChart from '@/components/StackedBarChart.vue'
import StatusPieChart from '@/components/StatusPieChart.vue'
import * as echarts from 'echarts'

// 汇总数据
const summary = ref({
  normal: 0,
  warn: 0,
  overdue: 0,
  total: 0
})

// 动画展示值（用于数字计数效果）
const displaySummary = reactive({
  normal: 0,
  warn: 0,
  overdue: 0,
  total: 0
})

const displayOverallSummary = reactive({
  overallNormal: 0,
  overallWarn: 0,
  overallOverdue: 0,
  total: 0
})

// 汇总数据（整段）
const overallSummary = ref({ overallNormal: 0, overallWarn: 0, overallOverdue: 0, total: 0 })
const overallChartData = ref([])

// 汇总数据（三段）
const sectionSummary = ref({ normal: 0, warn: 0, overdue: 0 })
const sectionChartData = ref([])
const sectionLoading = ref(false)
const sectionInitialLoading = ref(true)

// 动画展示值（三段）
const displaySectionSummary = reactive({
  normal: 0,
  warn: 0,
  overdue: 0
})

// 三段图表钻取状态
const sectionDrillDown = ref(null) // null = 段级视图, '前段'/'中段'/'后段' = 品牌钻取视图
const sectionBrandChartData = ref([])
const sectionSelectedBrand = ref('')
const sectionSelectedAlertStatus = ref('')
const sectionDetailShow = ref(false)
const sectionBarChartRef = ref(null)
let sectionChartClickHandler = null

let animationFrame = null

const animateNumbers = (from, to, duration = 800) => {
  cancelAnimationFrame(animationFrame)
  const startTime = performance.now()
  
  const step = (currentTime) => {
    const elapsed = currentTime - startTime
    const progress = Math.min(elapsed / duration, 1)
    const eased = 1 - Math.pow(1 - progress, 3) // ease-out cubic
    
    for (const key of Object.keys(from)) {
      from[key] = Math.round(from[key] + (to[key] - from[key]) * eased)
    }
    
    if (progress < 1) {
      animationFrame = requestAnimationFrame(step)
    } else {
      Object.assign(from, to)
    }
  }
  
  animationFrame = requestAnimationFrame(step)
}

// 初始加载骨架屏状态
const initialLoading = ref(true)

// 数据变化时触发数字动画
watch(() => summary.value, (val) => {
  if (val.total > 0) {
    if (initialLoading.value) initialLoading.value = false
    animateNumbers(displaySummary, {
      normal: val.normal || 0,
      warn: val.warn || 0,
      overdue: val.overdue || 0,
      total: val.total || 0
    })
  }
}, { deep: true })

watch(() => overallSummary.value, (val) => {
  if (val.total > 0) {
    animateNumbers(displayOverallSummary, {
      overallNormal: val.overallNormal || 0,
      overallWarn: val.overallWarn || 0,
      overallOverdue: val.overallOverdue || 0,
      total: val.total || 0
    })
  }
}, { deep: true })

watch(() => sectionSummary.value, (val) => {
  if (Object.keys(val).length > 0) {
    if (sectionInitialLoading.value) sectionInitialLoading.value = false
    animateNumbers(displaySectionSummary, {
      normal: val.normal || 0,
      warn: val.warn || 0,
      overdue: val.overdue || 0
    })
  }
}, { deep: true })

// 图表数据
const chartData = ref([])
const loading = ref(false)
const showDetail = ref(false)
const dateRange = ref(null)
const selectedBrand = ref('')
const selectedAlertStatus = ref('')
const overallSelectedBrand = ref('')
const overallSelectedAlertStatus = ref('')
const monitorStatusOptions = ['正常', '预警', '超期']

const activeMonitorTab = ref('segment')
const overallInitialLoading = ref(false)

const summaryCards = computed(() => [
  {
    key: 'normal',
    label: '正常',
    value: displaySummary.normal,
    meta: '处于标准时效内'
  },
  {
    key: 'warn',
    label: '预警',
    value: displaySummary.warn,
    meta: '接近时效阈值'
  },
  {
    key: 'overdue',
    label: '超期',
    value: displaySummary.overdue,
    meta: '已超过监控时效'
  },
  {
    key: 'total',
    label: '在途总数',
    value: displaySummary.total,
    meta: '当前在途车辆总量'
  }
])

const overallSummaryCards = computed(() => [
  { key: 'normal', label: '正常', value: displayOverallSummary.overallNormal, meta: '整段监控状态正常' },
  { key: 'warn', label: '预警', value: displayOverallSummary.overallWarn, meta: '接近整段时效阈值' },
  { key: 'overdue', label: '超期', value: displayOverallSummary.overallOverdue, meta: '已超过整段监控时效' },
  { key: 'total', label: '在途总数', value: displayOverallSummary.total, meta: '当前在途车辆总量' }
])

const sectionSummaryCards = computed(() => [
  { key: 'normal', label: '正常', value: displaySectionSummary.normal, meta: '三段监控状态正常' },
  { key: 'warn', label: '预警', value: displaySectionSummary.warn, meta: '接近三段时效阈值' },
  { key: 'overdue', label: '超期', value: displaySectionSummary.overdue, meta: '已超过三段监控时效' }
])

// 三段图表数据转换
const sectionChartDisplayData = computed(() => {
  if (sectionDrillDown.value) {
    // 品牌钻取视图
    return sectionBrandChartData.value.map(d => ({
      brand: d.brand,
      transportStatus: `${sectionDrillDown.value}监控`,
      normal: Number(d.normal || 0),
      warn: Number(d.warn || 0),
      overdue: Number(d.overdue || 0)
    })).filter(item => {
      const matchBrand = !sectionSelectedBrand.value || item.brand === sectionSelectedBrand.value
      const metricField = sectionSelectedAlertStatus.value ? statusFieldMap[sectionSelectedAlertStatus.value] : ''
      const matchAlertStatus = !metricField || Number(item[metricField] || 0) > 0
      return matchBrand && matchAlertStatus
    })
  } else {
    // 段级视图
    return sectionChartData.value.map(d => ({
      brand: d.sectionName,
      transportStatus: '三段监控',
      normal: Number(d.normal || 0),
      warn: Number(d.warn || 0),
      overdue: Number(d.overdue || 0)
    }))
  }
})

const sectionPieChartData = computed(() => {
  return sectionChartDisplayData.value
})

// 监听三段图表数据变化，绑定点击事件
watch(() => sectionChartDisplayData.value, () => {
  nextTick(() => {
    setTimeout(attachSectionChartClick, 300)
  })
})

const sectionBrandOptions = computed(() => {
  if (!sectionDrillDown.value) return []
  return Array.from(new Set(sectionBrandChartData.value.map(item => item.brand).filter(Boolean))).sort((a, b) =>
    a.localeCompare(b, 'zh-Hans-CN')
  )
})

const sectionActiveChartStatuses = computed(() => {
  return sectionSelectedAlertStatus.value ? [sectionSelectedAlertStatus.value] : monitorStatusOptions
})

const sectionBarTitle = computed(() => {
  if (sectionDrillDown.value) {
    const brandLabel = sectionSelectedBrand.value || '品牌'
    return `${sectionDrillDown.value} - ${brandLabel} × 监控分布`
  }
  return '三段监控状态分布'
})

const sectionPieTitle = computed(() => {
  if (sectionDrillDown.value) {
    return sectionSelectedAlertStatus.value
      ? `${sectionDrillDown.value} - ${sectionSelectedAlertStatus.value}占比`
      : `${sectionDrillDown.value} - 监控状态占比`
  }
  return '三段监控状态占比'
})

const sectionPieSubtitle = computed(() => {
  if (sectionDrillDown.value) {
    return sectionSelectedAlertStatus.value
      ? `基于当前筛选统计${sectionSelectedAlertStatus.value}下各状态数量比例`
      : `基于当前筛选统计正常、预警、超期数量比例`
  }
  return '各段正常、预警、超期数量比例'
})

const sectionTableData = computed(() => {
  if (sectionDrillDown.value) {
    return sectionBrandChartData.value.filter(item => {
      if (sectionSelectedBrand.value && item.brand !== sectionSelectedBrand.value) return false
      if (sectionSelectedAlertStatus.value) {
        const field = statusFieldMap[sectionSelectedAlertStatus.value]
        if (!Number(item[field] || 0)) return false
      }
      return true
    }).map(d => ({
      brand: d.brand,
      transportStatus: `${sectionDrillDown.value}监控`,
      normal: Number(d.normal || 0),
      warn: Number(d.warn || 0),
      overdue: Number(d.overdue || 0)
    }))
  } else {
    return sectionChartData.value.map(d => ({
      brand: d.sectionName,
      transportStatus: '三段监控',
      normal: Number(d.normal || 0),
      warn: Number(d.warn || 0),
      overdue: Number(d.overdue || 0)
    }))
  }
})

const brandOptions = computed(() => {
  return Array.from(new Set(chartData.value.map(item => item.brand).filter(Boolean))).sort((a, b) =>
    a.localeCompare(b, 'zh-Hans-CN')
  )
})

const statusFieldMap = {
  正常: 'normal',
  预警: 'warn',
  超期: 'overdue'
}

const activeChartStatuses = computed(() => {
  return selectedAlertStatus.value ? [selectedAlertStatus.value] : monitorStatusOptions
})

const filteredChartData = computed(() => {
  return chartData.value.filter(item => {
    const matchBrand = !selectedBrand.value || item.brand === selectedBrand.value
    const metricField = selectedAlertStatus.value ? statusFieldMap[selectedAlertStatus.value] : ''
    const matchAlertStatus = !metricField || Number(item[metricField] || 0) > 0
    return matchBrand && matchAlertStatus
  })
})

const chartDisplayData = computed(() => {
  if (!selectedAlertStatus.value) {
    return filteredChartData.value
  }

  const activeField = statusFieldMap[selectedAlertStatus.value]
  return filteredChartData.value.map(item => ({
    ...item,
    normal: activeField === 'normal' ? Number(item.normal || 0) : 0,
    warn: activeField === 'warn' ? Number(item.warn || 0) : 0,
    overdue: activeField === 'overdue' ? Number(item.overdue || 0) : 0
  }))
})

const pieSectionTitle = computed(() => {
  const brandLabel = selectedBrand.value || '品牌'
  return selectedAlertStatus.value
    ? `${brandLabel} ${selectedAlertStatus.value}在途状态占比`
    : `${brandLabel} 监控状态占比`
})

const barSectionTitle = computed(() => {
  const brandLabel = selectedBrand.value || '品牌'
  const statusLabel = selectedAlertStatus.value || '在途状态'
  return `${brandLabel} × ${statusLabel}分布`
})

const pieSectionSubtitle = computed(() => {
  return selectedAlertStatus.value
    ? `基于当前筛选结果统计${selectedBrand.value || '该品牌'}${selectedAlertStatus.value}下各在途状态数量比例`
    : `基于当前筛选结果统计${selectedBrand.value || '该品牌'}正常、预警、超期数量比例`
})

const overallChartDisplayData = computed(() => {
  return overallChartData.value.map(d => ({
    brand: d.brand,
    transportStatus: '整段监控',
    normal: d.normal || 0,
    warn: d.warn || 0,
    overdue: d.overdue || 0
  }))
})

const overallBrandOptions = computed(() => {
  return Array.from(new Set(overallChartData.value.map(item => item.brand).filter(Boolean))).sort((a, b) =>
    a.localeCompare(b, 'zh-Hans-CN')
  )
})

const overallFilteredChartTableData = computed(() => {
  return overallChartData.value.filter(item => {
    if (overallSelectedBrand.value && item.brand !== overallSelectedBrand.value) return false
    if (overallSelectedAlertStatus.value) {
      const field = statusFieldMap[overallSelectedAlertStatus.value]
      if (!Number(item[field] || 0)) return false
    }
    return true
  })
})

const overallActiveChartStatuses = computed(() => {
  return overallSelectedAlertStatus.value ? [overallSelectedAlertStatus.value] : monitorStatusOptions
})

const overallBarSectionTitle = computed(() => {
  const brandLabel = overallSelectedBrand.value || '品牌'
  return `${brandLabel} × 整段监控分布`
})

const overallPieSectionTitle = computed(() => {
  const brandLabel = overallSelectedBrand.value || '品牌'
  return overallSelectedAlertStatus.value
    ? `${brandLabel} ${overallSelectedAlertStatus.value}占比`
    : `${brandLabel} 监控状态占比`
})

const overallPieSectionSubtitle = computed(() => {
  return overallSelectedAlertStatus.value
    ? `基于当前筛选结果统计${overallSelectedBrand.value || '该品牌'}${overallSelectedAlertStatus.value}下各状态数量比例`
    : `基于当前筛选结果统计${overallSelectedBrand.value || '该品牌'}正常、预警、超期数量比例`
})

const overallResetFilters = () => {
  overallSelectedBrand.value = ''
  overallSelectedAlertStatus.value = ''
}

// 三段图表点击钻取
const handleSectionBarClick = (params) => {
  if (sectionDrillDown.value) return
  if (params.componentType !== 'series') return
  const name = params.name
  const sectionName = name.split('\n')[0]
  if (['前段', '中段', '后段'].includes(sectionName)) {
    sectionDrillDown.value = sectionName
    loadSectionBrandChartData(sectionName)
  }
}

const loadSectionBrandChartData = async (sectionName) => {
  try {
    const res = await request.get('/chart/brand-status', {
      params: { ...getTimeParams(), type: 'three-section', sectionName }
    })
    sectionBrandChartData.value = res || []
  } catch (error) {
    console.error('加载品牌钻取数据失败:', error)
    sectionBrandChartData.value = []
  }
}

const attachSectionChartClick = () => {
  if (!sectionBarChartRef.value) return
  const chartDiv = sectionBarChartRef.value.$el
  if (!chartDiv) return
  const chartInstance = echarts.getInstanceByDom(chartDiv)
  if (!chartInstance) return
  if (sectionChartClickHandler) {
    chartInstance.off('click', sectionChartClickHandler)
  }
  sectionChartClickHandler = handleSectionBarClick
  chartInstance.on('click', sectionChartClickHandler)
}

const goBackToSectionView = () => {
  sectionDrillDown.value = null
  sectionBrandChartData.value = []
  sectionSelectedBrand.value = ''
}

const sectionResetFilters = () => {
  sectionSelectedBrand.value = ''
  sectionSelectedAlertStatus.value = ''
}

// 加载汇总数据
const getTimeParams = () => {
  if (!dateRange.value) return {}
  const [start, end] = dateRange.value
  if (!start || !end) return {}
  return {
    startTime: start,
    endTime: end.substring(0, 10) + 'T23:59:59'
  }
}

const loadSummary = async () => {
  try {
    const res = await request.get('/transit/summary', { params: getTimeParams() })
    summary.value = res
  } catch (error) {
    console.error('加载汇总失败:', error)
  }
}

// 加载图表数据
const loadChartData = async () => {
  try {
    const res = await request.get('/chart/brand-status', { params: getTimeParams() })
    chartData.value = res || []
  } catch (error) {
    console.error('加载图表失败:', error)
    chartData.value = []
  }
}

const loadOverallSummary = async () => {
  try {
    const res = await request.get('/transit/summary', { params: getTimeParams() })
    overallSummary.value = res
  } catch (error) {
    console.error('加载整段汇总失败:', error)
  }
}

const loadOverallChartData = async () => {
  try {
    const res = await request.get('/chart/brand-status', { params: { ...getTimeParams(), type: 'overall' } })
    overallChartData.value = res || []
  } catch (error) {
    console.error('加载整段图表失败:', error)
    overallChartData.value = []
  }
}

const loadSectionSummary = async () => {
  try {
    const res = await request.get('/transit/summary', { params: getTimeParams() })
    sectionSummary.value = {
      normal: res.sectionNormal || 0,
      warn: res.sectionWarn || 0,
      overdue: res.sectionOverdue || 0
    }
  } catch (error) {
    console.error('加载三段汇总失败:', error)
  }
}

const loadSectionChartData = async () => {
  try {
    const res = await request.get('/chart/brand-status', { params: { ...getTimeParams(), type: 'three-section' } })
    sectionChartData.value = res || []
  } catch (error) {
    console.error('加载三段图表失败:', error)
    sectionChartData.value = []
  }
}

const loadSectionData = async () => {
  sectionLoading.value = true
  sectionInitialLoading.value = true
  try {
    await Promise.all([loadSectionSummary(), loadSectionChartData()])
    setTimeout(() => { sectionInitialLoading.value = false }, 150)
  } finally {
    sectionLoading.value = false
  }
}

const handleTabChange = (tabName) => {
  if (tabName === 'overall') {
    loadOverallData()
  } else if (tabName === 'three-section') {
    loadSectionData()
  }
  // 切换标签时重置三段钻取状态
  if (tabName !== 'three-section') {
    sectionDrillDown.value = null
    sectionBrandChartData.value = []
    sectionSelectedBrand.value = ''
    sectionSelectedAlertStatus.value = ''
  }
}

const loadOverallData = async () => {
  loading.value = true
  overallInitialLoading.value = true
  try {
    await Promise.all([loadOverallSummary(), loadOverallChartData()])
    setTimeout(() => { overallInitialLoading.value = false }, 150)
  } finally {
    loading.value = false
  }
}

// 刷新所有数据
const loadData = async () => {
  loading.value = true
  try {
    if (activeMonitorTab.value === 'segment') {
      await Promise.all([loadSummary(), loadChartData()])
    } else if (activeMonitorTab.value === 'overall') {
      await Promise.all([loadOverallSummary(), loadOverallChartData()])
    } else if (activeMonitorTab.value === 'three-section') {
      await Promise.all([loadSectionSummary(), loadSectionChartData()])
    }
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadData()
})

onUnmounted(() => {
  cancelAnimationFrame(animationFrame)
})
</script>

<style scoped>
.dashboard {
  min-height: 100%;
  display: flex;
  flex-direction: column;
  gap: 20px;
  color: #22324d;
}

.dashboard-time-filter {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 20px;
  border-radius: 20px;
  border: 1px solid #dbe7f5;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(246, 250, 255, 0.98));
  box-shadow: 0 12px 30px rgba(26, 65, 122, 0.08);
}

.time-filter__label {
  font-size: 14px;
  font-weight: 600;
  color: #13233c;
  white-space: nowrap;
}

.time-filter__picker {
  width: 380px;
}

.dashboard-summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.summary-card {
  position: relative;
  overflow: hidden;
  min-height: 144px;
  padding: 22px 24px;
  border-radius: 20px;
  border: 1px solid #dbe7f5;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(246, 250, 255, 0.98)),
    radial-gradient(circle at top right, rgba(64, 158, 255, 0.12), transparent 42%);
  box-shadow: 0 12px 30px rgba(26, 65, 122, 0.08);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.summary-card::after {
  content: '';
  position: absolute;
  inset: auto -36px -36px auto;
  width: 116px;
  height: 116px;
  border-radius: 50%;
  background: rgba(64, 158, 255, 0.08);
}

.summary-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 16px 36px rgba(26, 65, 122, 0.14);
}

.summary-card__label {
  position: relative;
  z-index: 1;
  font-size: 15px;
  color: #5e708d;
  letter-spacing: 1px;
}

.summary-card__value {
  position: relative;
  z-index: 1;
  margin-top: 14px;
  font-size: 44px;
  line-height: 1;
  font-weight: 700;
  color: #13233c;
}

.summary-card__value-normal {
  color: #67c23a;
}

.summary-card__value-warn {
  color: #e6a23c;
}

.summary-card__value-overdue {
  color: #f56c6c;
}

.summary-card__value-total {
  color: #2d78d6;
}

.summary-card__meta {
  position: relative;
  z-index: 1;
  margin-top: 18px;
  font-size: 13px;
  color: #7a8aa2;
}

.summary-card-normal {
  background:
    linear-gradient(180deg, #ffffff, #f3fbf7),
    radial-gradient(circle at top right, rgba(103, 194, 58, 0.16), transparent 42%);
}

.summary-card-warn {
  background:
    linear-gradient(180deg, #ffffff, #fff8ef),
    radial-gradient(circle at top right, rgba(230, 162, 60, 0.18), transparent 42%);
}

.summary-card-overdue {
  background:
    linear-gradient(180deg, #ffffff, #fff4f4),
    radial-gradient(circle at top right, rgba(245, 108, 108, 0.18), transparent 42%);
}

.summary-card-total {
  background:
    linear-gradient(180deg, #ffffff, #f3f8ff),
    radial-gradient(circle at top right, rgba(64, 158, 255, 0.18), transparent 42%);
}

.dashboard-panel {
  padding: 22px 24px 24px;
  border-radius: 22px;
  border: 1px solid #dbe7f5;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(248, 251, 255, 0.98)),
    radial-gradient(circle at top, rgba(64, 158, 255, 0.08), transparent 36%);
  box-shadow: 0 14px 34px rgba(26, 65, 122, 0.08);
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 18px;
}

.panel-title {
  font-size: 20px;
  font-weight: 700;
  color: #13233c;
}

.panel-subtitle {
  margin-top: 8px;
  font-size: 13px;
  color: #71839c;
}

.panel-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.chart-filters {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 8px;
}

.chart-filter {
  width: 220px;
}

.filter-reset {
  height: 32px;
  padding: 0 6px;
  color: #2d78d6;
}

.panel-tag {
  border-color: rgba(64, 158, 255, 0.2);
  background: rgba(64, 158, 255, 0.08);
  color: #2d78d6;
}

.chart-container {
  min-height: 460px;
  padding: 8px 0 0;
}

.chart-layout {
  display: grid;
  grid-template-columns: minmax(0, 2fr) minmax(320px, 1fr);
  gap: 20px;
  margin-top: 20px;
}

.chart-card {
  border-radius: 18px;
  border: 1px solid #e9f0f8;
  background: rgba(255, 255, 255, 0.72);
  padding: 18px 18px 16px;
}

.chart-card__header {
  margin-bottom: 8px;
}

.chart-card__title {
  font-size: 17px;
  font-weight: 600;
  color: #13233c;
}

.chart-card__subtitle {
  margin-top: 6px;
  font-size: 13px;
  color: #71839c;
}

.pie-section__chart {
  min-height: 320px;
}

.detail-toggle {
  color: #2d78d6;
}

.detail-table-wrap {
  border-radius: 16px;
  overflow: hidden;
}

:deep(.el-table) {
  --el-table-border-color: #dbe7f5;
  --el-table-header-bg-color: #eef5ff;
  --el-table-tr-bg-color: #ffffff;
  --el-table-row-hover-bg-color: #f3f8ff;
  --el-table-text-color: #22324d;
  --el-table-header-text-color: #13233c;
  --el-fill-color-blank: #ffffff;
}

:deep(.el-table th.el-table__cell) {
  font-weight: 600;
}

:deep(.el-button--primary) {
  --el-button-bg-color: #1d72f3;
  --el-button-border-color: #1d72f3;
  --el-button-hover-bg-color: #3884f7;
  --el-button-hover-border-color: #3884f7;
}

:deep(.chart-filter .el-select__wrapper) {
  background: #fff;
  box-shadow: 0 0 0 1px #d7e4f4 inset;
}

@media (max-width: 1200px) {
  .dashboard-summary {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .chart-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .dashboard-summary {
    grid-template-columns: 1fr;
  }

  .dashboard-panel,
  .summary-card {
    border-radius: 18px;
  }

  .panel-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .panel-actions {
    width: 100%;
    justify-content: space-between;
  }

  .chart-filter {
    width: 100%;
  }

  .summary-card__value {
    font-size: 38px;
  }
}

.dashboard-monitor-tabs {
  padding: 0;
  margin-bottom: -12px;
}

.dashboard-monitor-tabs :deep(.el-tabs__header) {
  margin: 0;
}

.dashboard-monitor-tabs :deep(.el-tabs__item) {
  font-size: 15px;
  font-weight: 600;
  padding: 0 24px;
  height: 44px;
  line-height: 44px;
  color: #5e708d;
}

.dashboard-monitor-tabs :deep(.el-tabs__item.is-active) {
  color: #1d72f3;
}

.dashboard-monitor-tabs :deep(.el-tabs__active-bar) {
  background-color: #1d72f3;
  height: 3px;
}

.overall-view {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.three-section-view {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

</style>
