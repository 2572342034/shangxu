<template>
  <div class="page-container">
    <div class="filter-bar">
      <el-input v-model="query.operName" placeholder="搜索操作人" clearable style="width: 180px" @keyup.enter="search" @clear="search" />
      <el-select v-model="query.status" placeholder="执行结果" clearable style="width: 140px" @change="search">
        <el-option label="成功" :value="1" />
        <el-option label="失败" :value="0" />
      </el-select>
      <el-button type="primary" plain @click="search">查询</el-button>
      <el-button @click="resetQuery">重置</el-button>
      <div class="spacer"></div>
      <el-button type="danger" plain @click="clearAll">
        <el-icon><Delete /></el-icon>清空日志
      </el-button>
    </div>

    <div class="table-card">
      <div class="table-toolbar">
        <div class="title">操作日志 <span class="text-muted" style="font-size: 12px; font-weight: 400">共 {{ total }} 条</span></div>
      </div>

      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column type="expand">
          <template #default="{ row }">
            <div class="expand-box">
              <div><b>请求方法：</b>{{ row.method || '-' }}</div>
              <div><b>请求地址：</b>{{ row.requestMethod }} {{ row.operUrl }}</div>
              <div><b>请求参数：</b>{{ row.operParam || '-' }}</div>
              <div v-if="row.errorMsg" class="err"><b>错误信息：</b>{{ row.errorMsg }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="模块" width="130" />
        <el-table-column label="操作类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" effect="light" :type="typeTag(row.businessType)">{{ row.businessType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="operName" label="操作人" width="110" />
        <el-table-column prop="operUrl" label="请求地址" min-width="200" show-overflow-tooltip />
        <el-table-column prop="operIp" label="IP" width="130" />
        <el-table-column label="耗时" width="90" align="right">
          <template #default="{ row }">{{ row.costTime }} ms</template>
        </el-table-column>
        <el-table-column label="结果" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small" effect="dark">
              {{ row.status === 1 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作时间" width="160">
          <template #default="{ row }">{{ formatDateTime(row.operTime) }}</template>
        </el-table-column>
        <template #empty><el-empty description="暂无日志" :image-size="90" /></template>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          background
          @size-change="load"
          @current-change="load"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { clearLogs, pageLogs } from '@/api/system'
import { formatDateTime } from '@/utils/format'

const loading = ref(false)
const list = ref([])
const total = ref(0)

const query = reactive({ pageNum: 1, pageSize: 20, operName: '', status: null })

function typeTag(type) {
  return { INSERT: 'success', UPDATE: 'primary', DELETE: 'danger', LOGIN: 'info', EXPORT: 'warning' }[type] || 'info'
}

async function load() {
  loading.value = true
  try {
    const { data } = await pageLogs(query)
    list.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function search() {
  query.pageNum = 1
  load()
}

function resetQuery() {
  Object.assign(query, { operName: '', status: null, pageNum: 1 })
  load()
}

async function clearAll() {
  await ElMessageBox.confirm('确定清空所有操作日志吗？该操作不可恢复。', '警告', { type: 'warning' })
  await clearLogs()
  ElMessage.success('日志已清空')
  load()
}

onMounted(load)
</script>

<style scoped>
.expand-box {
  padding: 8px 20px 12px 50px;
  font-size: 12px;
  color: #5b6779;
  line-height: 1.9;
  word-break: break-all;
}

.expand-box .err {
  color: #f56c6c;
}
</style>
