<template>
  <div class="history-replay">
    <!-- Header with title -->
    <section class="replay-header">
      <h2 class="replay-title">历史回放</h2>
      <span class="replay-subtitle">回放在途车辆监控历史快照</span>
    </section>

    <!-- Tab switching -->
    <el-tabs v-model="activeTab" @tab-change="loadSnapshots">
      <el-tab-pane label="整段监控" name="overall" />
      <el-tab-pane label="三段监控" name="three-section" />
      <el-tab-pane label="分段监控" name="segment" />
    </el-tabs>

    <!-- Time slider and controls -->
    <section class="replay-controls">
      <div class="replay-timeline">
        <el-slider
          v-if="snapshotList.length > 0"
          v-model="currentIndex"
          :min="0"
          :max="snapshotList.length - 1"
          :format-tooltip="formatTimestamp"
          show-stops
        />
        <div v-else class="timeline-placeholder">暂无快照数据</div>
      </div>
      <div class="replay-buttons">
        <el-button-group>
          <el-button @click="stepBackward" :disabled="currentIndex <= 0">
            <el-icon><DArrowLeft /></el-icon>
          </el-button>
          <el-button @click="stepForward" :disabled="currentIndex >= snapshotList.length - 1">
            <el-icon><DArrowRight /></el-icon>
          </el-button>
        </el-button-group>
        <el-button type="primary" @click="togglePlay" :disabled="snapshotList.length === 0">
          <el-icon><component :is="playing ? 'VideoPause' : 'VideoPlay'" /></el-icon>
          {{ playing ? '暂停' : '播放' }}
        </el-button>
        <span class="replay-speed">
          <el-select v-model="playSpeed" size="small" style="width: 80px">
            <el-option :value="1" label="1x" />
            <el-option :value="2" label="2x" />
            <el-option :value="4" label="4x" />
          </el-select>
        </span>
      </div>
      <div class="replay-info" v-if="currentSnapshot">
        <el-tag>{{ formatTimestamp(currentIndex) }}</el-tag>
        <span>共 {{ snapshotList.length }} 个快照</span>
      </div>
    </section>

    <!-- Empty state -->
    <el-empty v-if="snapshotList.length === 0 && !loading" description="暂无快照数据" />

    <!-- Current snapshot display -->
    <template v-if="currentSnapshot">
      <!-- Summary cards -->
      <section class="dashboard-summary">
        <div class="summary-card summary-card-normal">
          <div class="summary-card__label">正常</div>
          <div class="summary-card__value summary-card__value-normal">{{ currentSummary.normal }}</div>
          <div class="summary-card__meta">处于标准时效内</div>
        </div>
        <div class="summary-card summary-card-warn">
          <div class="summary-card__label">预警</div>
          <div class="summary-card__value summary-card__value-warn">{{ currentSummary.warn }}</div>
          <div class="summary-card__meta">接近时效阈值</div>
        </div>
        <div class="summary-card summary-card-overdue">
          <div class="summary-card__label">超期</div>
          <div class="summary-card__value summary-card__value-overdue">{{ currentSummary.overdue }}</div>
          <div class="summary-card__meta">已超过监控时效</div>
        </div>
        <div class="summary-card summary-card-total">
          <div class="summary-card__label">在途总数</div>
          <div class="summary-card__value summary-card__value-total">{{ currentSummary.total }}</div>
          <div class="summary-card__meta">当前在途车辆总量</div>
        </div>
      </section>

      <!-- Charts -->
      <section class="dashboard-panel">
        <div class="panel-header">
          <div class="panel-title">品牌 × 在途状态 监控图表</div>
        </div>
        <div class="chart-layout">
          <div class="chart-card chart-card-bar">
            <StackedBarChart :chartData="currentChartData" height="340px" />
          </div>
          <div class="chart-card chart-card-pie">
            <StatusPieChart :chartData="currentChartData" height="260px" />
          </div>
        </div>
      </section>
    </template>

    <!-- Loading -->
    <div v-if="loading" style="text-align: center; padding: 60px;">
      <el-icon class="is-loading" :size="32"><Loading /></el-icon>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onUnmounted } from 'vue'
import { DArrowLeft, DArrowRight, VideoPlay, VideoPause, Loading } from '@element-plus/icons-vue'
import StackedBarChart from '@/components/StackedBarChart.vue'
import StatusPieChart from '@/components/StatusPieChart.vue'
import { getSnapshots } from '@/api/snapshot'

const activeTab = ref('overall')
const snapshotList = ref([])
const currentIndex = ref(0)
const loading = ref(false)
const playing = ref(false)
const playSpeed = ref(1)
let playTimer = null

const currentSnapshot = computed(() => snapshotList.value[currentIndex.value])

const currentSummary = computed(() => {
  if (!currentSnapshot.value?.summaryJson) return { normal: 0, warn: 0, overdue: 0, total: 0 }
  try {
    const parsed = typeof currentSnapshot.value.summaryJson === 'string'
      ? JSON.parse(currentSnapshot.value.summaryJson)
      : currentSnapshot.value.summaryJson
    return {
      normal: parsed.normal || 0,
      warn: parsed.warn || 0,
      overdue: parsed.overdue || 0,
      total: parsed.total || 0
    }
  } catch {
    return { normal: 0, warn: 0, overdue: 0, total: 0 }
  }
})

const currentChartData = computed(() => {
  if (!currentSnapshot.value?.chartJson) return []
  try {
    const parsed = typeof currentSnapshot.value.chartJson === 'string'
      ? JSON.parse(currentSnapshot.value.chartJson)
      : currentSnapshot.value.chartJson
    if (!Array.isArray(parsed)) return []
    // 标准化：快照数据格式可能不同（segment有transportStatus，overall没有，three-section用sectionName）
    return parsed.map(item => ({
      brand: item.brand || item.sectionName || item.section || '未知',
      transportStatus: item.transportStatus || (activeTab.value === 'overall' ? '整段监控' : activeTab.value === 'three-section' ? '三段监控' : '监控'),
      normal: item.normal || 0,
      warn: item.warn || 0,
      overdue: item.overdue || 0
    }))
  } catch {
    return []
  }
})

const formatTimestamp = (index) => {
  const item = snapshotList.value[index]
  if (!item?.snapshotAt) return ''
  return item.snapshotAt.replace('T', ' ').substring(0, 16)
}

const loadSnapshots = async () => {
  loading.value = true
  try {
    const res = await getSnapshots({ tabType: activeTab.value })
    snapshotList.value = Array.isArray(res) ? res : []
    currentIndex.value = 0
  } catch (e) {
    console.error('加载快照失败:', e)
  } finally {
    loading.value = false
  }
}

const stepForward = () => {
  if (currentIndex.value < snapshotList.value.length - 1) currentIndex.value++
}

const stepBackward = () => {
  if (currentIndex.value > 0) currentIndex.value--
}

const togglePlay = () => {
  if (playing.value) {
    stopPlay()
  } else {
    startPlay()
  }
}

const startPlay = () => {
  playing.value = true
  playTimer = setInterval(() => {
    if (currentIndex.value >= snapshotList.value.length - 1) {
      stopPlay()
      return
    }
    currentIndex.value++
  }, 2000 / playSpeed.value)
}

const stopPlay = () => {
  playing.value = false
  if (playTimer) {
    clearInterval(playTimer)
    playTimer = null
  }
}

onUnmounted(() => stopPlay())

loadSnapshots()
</script>

<style scoped>
.history-replay {
  min-height: 100%;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.replay-header {
  margin-bottom: 0;
}

.replay-title {
  font-size: 20px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0;
}

.replay-subtitle {
  font-size: 14px;
  color: var(--text-muted);
  margin-top: 4px;
}

.replay-controls {
  padding: 10px 16px;
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-primary-lightest);
  background: var(--card-gradient);
}

.replay-timeline {
  margin-bottom: 6px;
}

.timeline-placeholder {
  height: 38px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-muted);
  font-size: 14px;
  background: var(--bg-secondary, #f5f7fa);
  border-radius: 6px;
}

.replay-buttons {
  display: flex;
  align-items: center;
  gap: 8px;
}

.replay-info {
  margin-top: 6px;
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 13px;
  color: var(--text-muted);
}

.dashboard-summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.summary-card {
  position: relative;
  padding: 12px 16px;
  border-radius: var(--radius-lg);
  background: linear-gradient(180deg, #fff, var(--color-primary-bg));
  box-shadow: 0 4px 12px rgba(26, 65, 122, 0.06);
  border: 1px solid var(--color-primary-lightest);
  overflow: hidden;
}

.summary-card__label {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-muted);
}

.summary-card__value {
  margin-top: 8px;
  font-size: var(--font-size-xxl);
  font-weight: 800;
}

.summary-card__value-normal {
  color: var(--color-success);
}

.summary-card__value-warn {
  color: var(--color-warning);
}

.summary-card__value-overdue {
  color: var(--color-danger);
}

.summary-card__value-total {
  color: var(--color-accent);
}

.summary-card__meta {
  margin-top: 6px;
  font-size: 13px;
  color: var(--text-muted);
}

.dashboard-panel {
  padding: 14px 18px 18px;
  border-radius: var(--radius-xl);
  border: var(--card-border);
  background: var(--card-gradient);
  box-shadow: var(--shadow-card);
}

.panel-header {
  margin-bottom: 10px;
}

.panel-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--text-primary);
}

.chart-layout {
  display: grid;
  grid-template-columns: minmax(0, 2fr) minmax(320px, 1fr);
  gap: 12px;
}

.chart-card {
  border-radius: var(--radius-xl);
  border: var(--card-border);
  background: var(--card-gradient);
  box-shadow: var(--shadow-card);
  padding: 18px 18px 16px;
  overflow: hidden;
}

.is-loading {
  animation: rotating 2s linear infinite;
}

@keyframes rotating {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}
</style>
