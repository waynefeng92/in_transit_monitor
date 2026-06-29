<template>
  <div class="vehicle-detail">
    <!-- VIN Search Bar -->
    <section class="search-section">
      <div class="search-row">
        <el-input
          v-model="vinSearch"
          placeholder="输入VIN搜索"
          class="search-input"
          clearable
          @keyup.enter="handleSearch"
        />
        <el-button type="primary" @click="handleSearch">
          <el-icon><Search /></el-icon>
          搜索
        </el-button>
      </div>
    </section>

    <!-- Loading Skeleton -->
    <el-skeleton v-if="loading" :loading="true" animated :rows="10" />

    <!-- Empty State (no search performed yet) -->
    <div v-else-if="!detail && !notFound" class="empty-state">
      <el-icon :size="48" color="#909399"><Search /></el-icon>
      <p>请输入VIN搜索车辆详情</p>
    </div>

    <!-- Not Found State -->
    <div v-else-if="notFound" class="not-found-section">
      <el-result icon="warning" title="未找到车辆" :sub-title="'VIN: ' + searchedVin">
        <template #extra>
          <el-button type="primary" @click="handleSearch">重新查询</el-button>
        </template>
      </el-result>
    </div>

    <!-- Vehicle Detail Data -->
    <template v-else-if="detail">
      <!-- No-config Warning -->
      <el-alert
        v-if="detail.totalStandardOtdHours == null"
        type="warning"
        title="未配置标准OTD时效"
        show-icon
        class="config-warning"
      />

      <!-- Summary Cards: 4 cards in a row -->
      <el-row :gutter="20" class="summary-row">
        <el-col :xs="24" :sm="12" :md="6" class="summary-col">
          <el-card class="summary-card" shadow="never">
            <div class="summary-card__label">VIN</div>
            <div class="summary-card__value summary-card__value--vin">{{ detail.vin || '—' }}</div>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="12" :md="6" class="summary-col">
          <el-card class="summary-card" shadow="never">
            <div class="summary-card__label">品牌</div>
            <div class="summary-card__value">{{ detail.brandName || '—' }}</div>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="12" :md="6" class="summary-col">
          <el-card class="summary-card" shadow="never">
            <div class="summary-card__label">线路</div>
            <div class="summary-card__value summary-card__value--route">{{ routeDisplay }}</div>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="12" :md="6" class="summary-col">
          <el-card class="summary-card" shadow="never">
            <div class="summary-card__label">状态</div>
            <div class="summary-card__value summary-card__value--status">
              <el-tag :type="statusTagType(detail.transportStatus)">
                {{ detail.transportStatusName || '—' }}
              </el-tag>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- Gantt Chart -->
      <section class="dashboard-panel chart-section">
        <div class="panel-header">
          <div class="panel-title">运输甘特图</div>
        </div>
        <VehicleGanttChart :segments="detail.segments || []" height="400px" />
      </section>

      <!-- Segment Detail Tabs -->
      <section class="dashboard-panel table-section">
        <el-tabs v-model="activeDetailTab" type="border-card">
          <!-- ── Tab 1: 分段 ── -->
          <el-tab-pane label="分段详情 (7段)" name="segment">
            <el-table
              :data="detail.segments || []"
              stripe
              style="width: 100%"
              :row-class-name="tableRowClassName"
            >
              <el-table-column prop="segmentIndex" label="段序号" width="80" align="center" />
              <el-table-column prop="segmentName" label="段名称" width="140" />
              <el-table-column label="开始时间" width="170">
                <template #default="{ row }">
                  {{ formatTime(row.startTime) }}
                </template>
              </el-table-column>
              <el-table-column label="结束时间" width="170">
                <template #default="{ row }">
                  {{ row.endTime ? formatTime(row.endTime) : (row.actualDurationHours != null ? '进行中' : '—') }}
                </template>
              </el-table-column>
              <el-table-column label="标准OTD(h)" width="120" align="center">
                <template #default="{ row }">
                  {{ row.standardOtdHours != null && row.standardOtdHours > 0 ? row.standardOtdHours.toFixed(1) : '—' }}
                </template>
              </el-table-column>
              <el-table-column label="实际耗时(h)" width="120" align="center">
                <template #default="{ row }">
                  {{ row.actualDurationHours != null ? row.actualDurationHours.toFixed(1) : '—' }}
                </template>
              </el-table-column>
              <el-table-column label="状态" width="100" align="center">
                <template #default="{ row }">
                  <el-tag :type="segmentStatusTagType(row.status)" size="small">
                    {{ segmentStatusLabel(row.status) }}
                  </el-tag>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <!-- ── Tab 2: 三段 ── -->
          <el-tab-pane label="三段详情" name="section">
            <el-table
              :data="threeSectionData"
              stripe
              style="width: 100%"
            >
              <el-table-column prop="name" label="段名称" width="100" />
              <el-table-column prop="includedSegments" label="包含分段" width="280" />
              <el-table-column label="标准OTD(h)" width="140" align="center">
                <template #default="{ row }">
                  {{ row.standardOtd > 0 ? row.standardOtd.toFixed(1) : '—' }}
                </template>
              </el-table-column>
              <el-table-column label="实际耗时(h)" width="140" align="center">
                <template #default="{ row }">
                  {{ row.actualDuration != null ? row.actualDuration.toFixed(1) : '—' }}
                </template>
              </el-table-column>
              <el-table-column label="状态" width="100" align="center">
                <template #default="{ row }">
                  <el-tag :type="segmentStatusTagType(row.status)" size="small">
                    {{ segmentStatusLabel(row.status) }}
                  </el-tag>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <!-- ── Tab 3: 整段 ── -->
          <el-tab-pane label="整段详情" name="overall">
            <el-table
              :data="[overallData]"
              stripe
              style="width: 100%"
              :show-header="false"
            >
              <el-table-column label="指标" width="140">
                <template #default>
                  <span style="font-weight: 600">总标准OTD(h)</span>
                </template>
              </el-table-column>
              <el-table-column label="值" width="140" align="center">
                <template #default>
                  {{ overallData.standardOtd != null && overallData.standardOtd > 0 ? overallData.standardOtd.toFixed(1) : '—' }}
                </template>
              </el-table-column>
              <el-table-column>
                <template #default>
                  <span style="font-weight: 600; margin-left: 24px">总实际耗时(h)</span>
                </template>
              </el-table-column>
              <el-table-column align="center">
                <template #default>
                  {{ overallData.actualDuration != null ? overallData.actualDuration.toFixed(1) : '—' }}
                </template>
              </el-table-column>
              <el-table-column>
                <template #default>
                  <span style="font-weight: 600; margin-left: 24px">状态</span>
                </template>
              </el-table-column>
              <el-table-column align="center">
                <template #default>
                  <el-tag :type="segmentStatusTagType(overallData.status)" size="small">
                    {{ segmentStatusLabel(overallData.status) }}
                  </el-tag>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </section>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getVehicleDetail } from '@/api/vehicleDetail'
import VehicleGanttChart from '@/components/VehicleGanttChart.vue'

const route = useRoute()

// ── Reactive state ──
const vinSearch = ref('')
const searchedVin = ref('')
const detail = ref(null)
const loading = ref(false)
const notFound = ref(false)

// ── Segment status label mapping ──
const SEGMENT_STATUS_LABEL_MAP = {
  NORMAL: '正常',
  WARN: '预警',
  OVERDUE: '超期',
  PENDING: '未开始',
  'N/A': '无数据'
}

// ── Tab state ──
const activeDetailTab = ref('segment')

const sumOrZero = (val) => {
  if (val == null) return 0
  return Number(val) || 0
}

// ── Computed ──
const routeDisplay = computed(() => {
  if (!detail.value) return '未知线路'
  if (detail.value.originCity && detail.value.destCity) {
    return `${detail.value.originCity} → ${detail.value.destCity}`
  }
  return '未知线路'
})

const threeSectionData = computed(() => {
  const segs = detail.value?.segments || []
  const sections = [
    { name: '前段', includedSegments: '未出库+集港在途+已集港待装船', range: [0, 3] },
    { name: '中段', includedSegments: '水运在途+已到港待卸船', range: [3, 5] },
    { name: '后段', includedSegments: '已卸船待分拨+分拨在途', range: [5, 7] }
  ]
  return sections.map((sec) => {
    const slice = segs.slice(sec.range[0], sec.range[1])
    const otdVals = slice.map(s => sumOrZero(s.standardOtdHours))
    const stdOtd = otdVals.reduce((a, b) => a + b, 0)
    const actualVals = slice.map(s => s.actualDurationHours)
    const hasAnyActual = actualVals.some(v => v != null)
    const actual = hasAnyActual ? actualVals.map(v => sumOrZero(v)).reduce((a, b) => a + b, 0) : null
    const status = (stdOtd == null || stdOtd === 0)
      ? 'N/A'
      : (actual == null)
        ? 'N/A'
        : (actual > stdOtd)
          ? 'OVERDUE'
          : 'NORMAL'
    return {
      name: sec.name,
      includedSegments: sec.includedSegments,
      standardOtd: stdOtd,
      actualDuration: actual,
      status
    }
  })
})

const overallData = computed(() => {
  const d = detail.value
  const stdOtd = d?.totalStandardOtdHours
  const actual = d?.totalActualHours
  const status = (stdOtd == null || stdOtd === 0)
    ? 'N/A'
    : (actual == null)
      ? 'N/A'
      : (actual > stdOtd)
        ? 'OVERDUE'
        : 'NORMAL'
  return {
    standardOtd: stdOtd,
    actualDuration: actual,
    status
  }
})

// ── Methods ──
const handleSearch = async () => {
  const vin = vinSearch.value?.trim()
  if (!vin) {
    ElMessage.warning('请输入VIN')
    return
  }

  loading.value = true
  notFound.value = false
  detail.value = null
  searchedVin.value = vin

  try {
    const data = await getVehicleDetail(vin)
    detail.value = data
  } catch {
    detail.value = null
    notFound.value = true
  } finally {
    loading.value = false
  }
}

const formatTime = (isoString) => {
  if (!isoString) return '—'
  return isoString.replace('T', ' ').substring(0, 16)
}

const statusTagType = (status) => {
  if (status && status.includes('ARRIVED')) return 'success'
  return 'info'
}

const segmentStatusTagType = (status) => {
  const map = {
    NORMAL: 'success',
    WARN: 'warning',
    OVERDUE: 'danger',
    PENDING: 'info',
    'N/A': 'info'
  }
  return map[status] || 'info'
}

const segmentStatusLabel = (status) => {
  return SEGMENT_STATUS_LABEL_MAP[status] || status || '—'
}

const tableRowClassName = ({ row }) => {
  if (row && row.status === 'OVERDUE') return 'overdue-row'
  return ''
}

// ── Auto-load from query param ──
onMounted(() => {
  if (route.query.vin) {
    vinSearch.value = route.query.vin
    handleSearch()
  }
})
</script>

<style scoped>
.vehicle-detail {
  padding: 0;
}

/* ── Search Section ── */
.search-section {
  margin-bottom: 24px;
}

.search-row {
  display: flex;
  gap: 12px;
  align-items: center;
}

.search-input {
  max-width: 360px;
}

/* ── Empty State ── */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 0;
  color: var(--text-muted);
}

.empty-state p {
  margin-top: 16px;
  font-size: var(--font-size-base);
}

/* ── Not Found Section ── */
.not-found-section {
  padding: 40px 0;
}

/* ── Config Warning ── */
.config-warning {
  margin-bottom: 20px;
}

/* ── Summary Cards ── */
.summary-row {
  margin-bottom: 20px;
}

.summary-col {
  margin-bottom: 20px;
}

.summary-card {
  border-radius: var(--radius-xl);
  border: var(--card-border);
  box-shadow: var(--shadow-card);
}

.summary-card :deep(.el-card__body) {
  padding: 20px 24px;
}

.summary-card__label {
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
  margin-bottom: 8px;
}

.summary-card__value {
  font-size: var(--font-size-lg);
  font-weight: 600;
  color: var(--text-primary);
  word-break: break-all;
}

.summary-card__value--vin {
  font-size: var(--font-size-md);
  font-family: 'SF Mono', 'Cascadia Code', 'Fira Code', monospace;
}

.summary-card__value--route {
  font-size: var(--font-size-base);
}

/* ── Chart & Table Sections ── */
.chart-section,
.table-section {
  margin-bottom: 20px;
}

/* ── Dashboard Panel ── */
.dashboard-panel {
  padding: 22px 24px 24px;
  border-radius: var(--radius-xl);
  border: var(--card-border);
  background:
    var(--card-gradient),
    radial-gradient(circle at top, rgba(64, 158, 255, 0.08), transparent 36%);
  box-shadow: var(--shadow-card);
}

/* ── Panel Header ── */
.panel-header {
  margin-bottom: 16px;
}

.panel-title {
  font-size: var(--font-size-lg);
  font-weight: 700;
  color: var(--text-primary);
}

/* ── Overdue Row ── */
:deep(.overdue-row) {
  border-left: 3px solid #f56c6c;
}

/* ── Responsive ── */
@media (max-width: 768px) {
  .search-row {
    flex-direction: column;
  }

  .search-input {
    max-width: 100%;
    width: 100%;
  }

  .dashboard-panel {
    border-radius: var(--radius-lg);
    padding: 16px;
  }
}
</style>
