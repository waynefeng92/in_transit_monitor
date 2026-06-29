<template>
  <div class="order-manage">
    <!-- Header -->
    <section class="page-header">
      <h2 class="page-title">订单管理</h2>
      <span class="page-subtitle">按条件查询、下载或删除订单数据</span>
    </section>

    <!-- Filter bar -->
    <el-card>
      <div class="filter-bar">
        <el-select v-model="filters.brandId" placeholder="选择品牌" clearable style="width:150px">
          <el-option v-for="b in brandList" :key="b.id" :label="b.brandName" :value="b.id" />
        </el-select>
        <el-input v-model="filters.vin" placeholder="VIN（支持模糊搜索）" clearable style="width:180px" />
        <el-date-picker v-model="filters.dateRange" type="daterange" range-separator="至"
          start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DDTHH:mm:ss"
          style="width:260px" />
        <el-select v-model="filters.transportStatus" placeholder="在途状态" clearable style="width:140px">
          <el-option v-for="s in transportStatusOptions" :key="s.code" :label="s.name" :value="s.code" />
        </el-select>
        <el-select v-model="filters.monitorStatus" placeholder="预警状态" clearable style="width:120px">
          <el-option v-for="s in monitorStatusOptions" :key="s.code" :label="s.name" :value="s.code" />
        </el-select>
        <el-button type="primary" @click="search" :loading="loading">查询</el-button>
        <el-button @click="resetFilters">重置</el-button>
      </div>
    </el-card>

    <!-- Actions -->
    <div class="action-bar">
      <el-button type="danger" :disabled="selectedRows.length === 0" @click="handleBatchCancel">
        <el-icon><Delete /></el-icon> 批量删除（{{ selectedRows.length }}）
      </el-button>
      <el-button @click="handleExport" :loading="exporting">
        <el-icon><Download /></el-icon> 导出全部
      </el-button>
      <span class="record-count">共 {{ total }} 条记录</span>
    </div>

    <!-- Table -->
    <el-card>
      <el-table :data="records" border stripe v-loading="loading" @selection-change="handleSelectChange"
        max-height="600" style="width:100%">
        <el-table-column type="selection" width="45" />
        <el-table-column prop="vin" label="VIN" min-width="170" />
        <el-table-column prop="brandName" label="品牌" width="100" />
        <el-table-column prop="orderReleaseTime" label="订单释放时间" width="160">
          <template #default="{ row }">{{ formatDate(row.orderReleaseTime) }}</template>
        </el-table-column>
        <el-table-column prop="originCity" label="出发地" width="100" />
        <el-table-column prop="destCity" label="目的地" width="120" />
        <el-table-column prop="transportStatusName" label="在途状态" width="130">
          <template #default="{ row }">
            <el-tag v-if="row.transportStatusName" :type="statusTagType(row.transportStatus)" effect="plain">
              {{ row.transportStatusName }}
            </el-tag>
            <span v-else style="color:var(--text-muted)">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="monitorStatus" label="预警状态" width="90">
          <template #default="{ row }">
            <el-tag v-if="row.monitorStatus" :type="monitorTagType(row.monitorStatus)" effect="plain" size="small">
              {{ row.monitorStatus === 'NORMAL' ? '正常' : row.monitorStatus === 'WARN' ? '预警' : row.monitorStatus === 'OVERDUE' ? '超期' : row.monitorStatus }}
            </el-tag>
            <span v-else style="color:var(--text-muted)">-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleViewDetail(row)">详情</el-button>
            <el-button text type="danger" size="small" @click="handleCancelOne(row.orderId)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div style="margin-top:16px;text-align:center">
        <el-pagination v-model:current-page="pagination.page" v-model:page-size="pagination.size"
          :total="total" :page-sizes="[20,50,100]" layout="total, sizes, prev, pager, next"
          @size-change="search" @current-change="search" />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Download } from '@element-plus/icons-vue'
import { getOrderList, batchCancelOrders, exportOrders } from '@/api/order'
import { getBrandList } from '@/api/brand'

const router = useRouter()
const brandList = ref([])
const records = ref([])
const total = ref(0)
const loading = ref(false)
const exporting = ref(false)
const selectedRows = ref([])

const filters = reactive({
  brandId: null,
  vin: '',
  dateRange: null,
  transportStatus: '',
  monitorStatus: ''
})

const pagination = reactive({
  page: 1,
  size: 20
})

const transportStatusOptions = [
  { code: 'NOT_DEPARTED', name: '未出库' },
  { code: 'TO_PORT', name: '集港在途' },
  { code: 'AT_PORT_WAIT_SHIP', name: '已集港待装船' },
  { code: 'ON_SEA', name: '水运在途' },
  { code: 'AT_DEST_WAIT_UNLOAD', name: '已到港待卸船' },
  { code: 'UNLOADED_WAIT_DISPATCH', name: '已卸船待分拨' },
  { code: 'DISPATCHING', name: '分拨在途' },
  { code: 'ARRIVED', name: '已到达' }
]

const monitorStatusOptions = [
  { code: 'NORMAL', name: '正常' },
  { code: 'WARN', name: '预警' },
  { code: 'OVERDUE', name: '超期' }
]

const formatDate = (d) => {
  if (!d) return '-'
  return d.replace('T', ' ').substring(0, 19)
}

const statusTagType = (s) => {
  if (s === 'ARRIVED') return 'success'
  if (s === 'NOT_DEPARTED') return 'info'
  return ''
}

const monitorTagType = (s) => {
  if (s === 'NORMAL') return 'success'
  if (s === 'WARN') return 'warning'
  if (s === 'OVERDUE') return 'danger'
  return 'info'
}

const search = async () => {
  loading.value = true
  try {
    const params = {
      page: pagination.page,
      size: pagination.size
    }
    if (filters.brandId) params.brandId = filters.brandId
    if (filters.vin) params.vin = filters.vin
    if (filters.transportStatus) params.transportStatus = filters.transportStatus
    if (filters.monitorStatus) params.monitorStatus = filters.monitorStatus
    if (filters.dateRange) {
      params.startTime = filters.dateRange[0]
      params.endTime = filters.dateRange[1]
    }
    const res = await getOrderList(params)
    records.value = res.records || []
    total.value = res.total || 0
  } catch (e) {
    console.error('查询失败', e)
  } finally {
    loading.value = false
  }
}

const resetFilters = () => {
  filters.brandId = null
  filters.vin = ''
  filters.dateRange = null
  filters.transportStatus = ''
  filters.monitorStatus = ''
  pagination.page = 1
  search()
}

const handleSelectChange = (rows) => {
  selectedRows.value = rows
}

const handleBatchCancel = async () => {
  try {
    await ElMessageBox.confirm(
      `确认删除选中的 ${selectedRows.value.length} 个订单？删除后订单和关联的在途记录将被删除。`,
      '批量删除确认',
      { type: 'warning', confirmButtonText: '确认删除' }
    )
    const ids = selectedRows.value.map(r => r.orderId)
    const res = await batchCancelOrders(ids)
    ElMessage.success(`成功删除 ${res.successCount} 个订单`)
    search()
  } catch {
    // user cancelled
  }
}

const handleCancelOne = async (orderId) => {
  try {
    await ElMessageBox.confirm(
      '确认删除该订单？删除后订单和关联的在途记录将被删除。',
      '确认删除',
      { type: 'warning' }
    )
    const res = await batchCancelOrders([orderId])
    ElMessage.success('已删除')
    search()
  } catch {
    // user cancelled
  }
}

const handleViewDetail = (row) => {
  router.push('/vehicle-detail?vin=' + row.vin)
}

const handleExport = async () => {
  exporting.value = true
  try {
    const params = {}
    if (filters.brandId) params.brandId = filters.brandId
    if (filters.vin) params.vin = filters.vin
    if (filters.transportStatus) params.transportStatus = filters.transportStatus
    if (filters.monitorStatus) params.monitorStatus = filters.monitorStatus
    if (filters.dateRange) {
      params.startTime = filters.dateRange[0]
      params.endTime = filters.dateRange[1]
    }
    const blob = await exportOrders(params)
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = 'orders.xlsx'
    link.click()
    URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch (e) {
    console.error('导出失败', e)
  } finally {
    exporting.value = false
  }
}

onMounted(() => {
  getBrandList().then(list => {
    brandList.value = list
  }).catch(() => {})
  // 不自动查询，等用户点击搜索
})
</script>

<style scoped>
.order-manage {
  min-height: 100%;
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.page-header {
  margin-bottom: 4px;
}
.page-title {
  font-size: 22px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0;
}
.page-subtitle {
  font-size: 13px;
  color: var(--text-muted);
  margin-top: 4px;
  display: block;
}
.filter-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
}
.action-bar {
  display: flex;
  align-items: center;
  gap: 12px;
}
.record-count {
  font-size: 13px;
  color: var(--text-muted);
  margin-left: auto;
}
</style>
