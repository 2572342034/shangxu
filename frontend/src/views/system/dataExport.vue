<template>
  <div class="page-container">
    <div class="page-head">
      <div>
        <h2>数据导出</h2>
        <p class="page-sub">按条件查询后预览，再选择导出范围与字段；导出结果为带 BOM 的 CSV，Excel 可直接打开</p>
      </div>
    </div>

    <div class="export-panel">
      <div class="filter-row">
        <span class="filter-label">数据类型</span>
        <el-radio-group v-model="query.module" @change="changeModule">
          <el-radio-button value="CUSTOMER">客户</el-radio-button>
          <el-radio-button value="LEAD">线索</el-radio-button>
          <el-radio-button value="RECEIVABLE">应收单</el-radio-button>
          <el-radio-button value="PRODUCT">产品</el-radio-button>
        </el-radio-group>
      </div>

      <div class="filter-row">
        <span class="filter-label">查询条件</span>
        <el-input v-model="query.keyword" placeholder="关键词" clearable style="width: 210px" @keyup.enter="search" />
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 140px">
          <el-option v-for="o in statusOptions" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
        <el-select v-if="query.module === 'CUSTOMER'" v-model="query.level" placeholder="客户级别" clearable style="width: 140px">
          <el-option v-for="o in dictStore.options('customer_level')" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
        <el-select v-if="query.module === 'CUSTOMER' || query.module === 'LEAD'" v-model="query.industry" placeholder="行业" clearable style="width: 140px">
          <el-option v-for="o in dictStore.options('industry')" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
        <el-select v-if="query.module === 'CUSTOMER' || query.module === 'LEAD'" v-model="query.source" placeholder="来源" clearable style="width: 140px">
          <el-option v-for="o in dictStore.options('customer_source')" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
        <el-select v-if="query.module === 'CUSTOMER'" v-model="query.poolType" placeholder="客户池" clearable style="width: 120px">
          <el-option label="私海" value="PRIVATE" />
          <el-option label="公海" value="PUBLIC" />
        </el-select>
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          value-format="YYYY-MM-DD"
          start-placeholder="创建起"
          end-placeholder="创建止"
          style="width: 240px"
        />
        <div class="filter-actions">
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </div>
      </div>

      <div class="filter-row column-row">
        <span class="filter-label">导出列</span>
        <div class="column-box">
          <el-checkbox v-model="checkAll" :indeterminate="indeterminate" @change="toggleAll">全选</el-checkbox>
          <el-checkbox-group v-model="selectedFields" @change="syncCheckAll">
            <el-checkbox v-for="c in columns" :key="c.key" :value="c.key">{{ c.label }}</el-checkbox>
          </el-checkbox-group>
        </div>
      </div>
    </div>

    <div class="table-card">
      <div class="export-toolbar">
        <div class="export-summary">
          共 <b>{{ total }}</b> 条结果<span v-if="selected.length">，已勾选 <b>{{ selected.length }}</b> 条</span>
          <span class="text-muted"> · 将导出 {{ selectedFields.length }} 列</span>
        </div>
        <div>
          <el-button type="primary" :disabled="!total || !selectedFields.length" @click="download(false)">
            <el-icon><Download /></el-icon>导出查询结果
          </el-button>
          <el-button :disabled="!selected.length || !selectedFields.length" @click="download(true)">
            导出选中项
          </el-button>
        </div>
      </div>

      <el-table
        v-loading="loading"
        :data="list"
        stripe
        height="440"
        row-key="id"
        @selection-change="selected = $event"
      >
        <el-table-column type="selection" width="46" reserve-selection />
        <el-table-column
          v-for="c in columns"
          :key="c.key"
          :prop="c.key"
          :label="c.label"
          min-width="130"
          show-overflow-tooltip
        />
        <template #empty>
          <el-empty description="暂无符合条件的数据" :image-size="90" />
        </template>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="load"
          @size-change="search"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useDictStore } from '@/store/dict'
import { exportDownload, exportMeta, exportPreview } from '@/api/dataExport'

const route = useRoute()
const dictStore = useDictStore()
const loading = ref(false)
const list = ref([])
const total = ref(0)
const columns = ref([])
const meta = reactive({ label: '客户', statusDict: 'customer_status' })
const selected = ref([])
const selectedFields = ref([])
const checkAll = ref(true)
const dateRange = ref([])

const query = reactive({
  module: 'CUSTOMER',
  keyword: '',
  status: '',
  level: '',
  industry: '',
  source: '',
  poolType: '',
  dateFrom: '',
  dateTo: '',
  pageNum: 1,
  pageSize: 10
})

const indeterminate = computed(() =>
  selectedFields.value.length > 0 && selectedFields.value.length < columns.value.length
)

const statusOptions = computed(() => {
  if (query.module === 'PRODUCT') {
    return [{ label: '在售', value: '1' }, { label: '停售', value: '0' }]
  }
  return dictStore.options(meta.statusDict)
})

function buildParams(extra = {}) {
  return {
    ...query,
    dateFrom: dateRange.value?.[0] || '',
    dateTo: dateRange.value?.[1] || '',
    ...extra
  }
}

async function loadMeta() {
  const { data } = await exportMeta(query.module)
  meta.label = data.label
  meta.statusDict = data.statusDict
  columns.value = data.columns || []
  selectedFields.value = columns.value.map(c => c.key)
  checkAll.value = true
}

async function load() {
  loading.value = true
  try {
    const { data } = await exportPreview(buildParams())
    list.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function search() {
  query.pageNum = 1
  selected.value = []
  load()
}

async function changeModule() {
  Object.assign(query, {
    keyword: '', status: '', level: '', industry: '', source: '',
    poolType: '', pageNum: 1
  })
  dateRange.value = []
  selected.value = []
  await loadMeta()
  await load()
}

function reset() {
  Object.assign(query, {
    keyword: '', status: '', level: '', industry: '', source: '',
    poolType: '', pageNum: 1
  })
  dateRange.value = []
  search()
}

function toggleAll(val) {
  selectedFields.value = val ? columns.value.map(c => c.key) : []
}

function syncCheckAll() {
  checkAll.value = selectedFields.value.length === columns.value.length
}

async function download(onlySelected) {
  if (!selectedFields.value.length) {
    ElMessage.warning('请至少选择一列')
    return
  }
  const params = buildParams({
    fields: selectedFields.value.join(','),
    pageNum: 1,
    pageSize: 10000
  })
  if (onlySelected) {
    params.ids = selected.value.map(r => r.id).join(',')
  }
  try {
    const response = await exportDownload(params)
    const url = window.URL.createObjectURL(new Blob([response.data]))
    const a = document.createElement('a')
    a.href = url
    a.download = `${meta.label}-${new Date().toISOString().slice(0, 10)}.csv`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    window.URL.revokeObjectURL(url)
    ElMessage.success(onlySelected ? `已导出选中的 ${selected.value.length} 条` : `已导出 ${total.value} 条`)
  } catch (e) {
    ElMessage.error('导出失败，请缩小范围后重试')
  }
}

onMounted(async () => {
  await dictStore.load()
  if (route.query.module) {
    query.module = String(route.query.module).toUpperCase()
  }
  await loadMeta()
  await load()
})
</script>

<style scoped>
.page-sub {
  font-size: 12.5px;
  color: #8b97a8;
  margin: 4px 0 0;
}
.export-panel {
  background: #fff;
  border: 1px solid #edf0f5;
  border-radius: 10px;
  padding: 14px 16px;
  margin-bottom: 12px;
}
.filter-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  padding: 6px 0;
}
.filter-row + .filter-row {
  border-top: 1px dashed #f0f3f8;
  padding-top: 12px;
  margin-top: 6px;
}
.filter-label {
  font-size: 13px;
  color: #5b6779;
  width: 62px;
  flex-shrink: 0;
}
.column-row {
  align-items: flex-start;
}
.filter-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
  margin-left: auto;
}
.column-box {
  flex: 1;
  min-width: 0;
}
.column-box :deep(.el-checkbox-group) {
  display: inline-flex;
  flex-wrap: wrap;
  gap: 2px 16px;
  margin-left: 18px;
}
.export-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 4px 0 12px;
}
.export-summary {
  font-size: 13px;
  color: #5b6779;
}
.export-summary b {
  color: #2f6bff;
}
</style>
