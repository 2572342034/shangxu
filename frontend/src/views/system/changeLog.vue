<template>
  <div class="page-container">
    <div class="page-head">
      <div>
        <h2>修改记录</h2>
        <p class="page-sub">全系统单据的字段级变更轨迹，按模块、单号、操作人检索</p>
      </div>
    </div>

    <div class="filter-bar">
      <el-select v-model="query.bizType" placeholder="全部模块" clearable style="width: 170px" @change="search">
        <el-option v-for="t in BIZ_TYPES" :key="t.value" :label="t.label" :value="t.value" />
      </el-select>
      <el-input
        v-model="query.keyword"
        placeholder="单号 / 字段 / 操作人 / 变更值"
        clearable
        style="width: 260px"
        @keyup.enter="search"
      />
      <el-button type="primary" @click="search">查询</el-button>
      <el-button @click="reset">重置</el-button>
    </div>

    <div class="table-card">
      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column prop="createTime" label="时间" width="160" />
        <el-table-column label="模块" width="110" align="center">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ bizTypeLabel(row.bizType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="bizNo" label="单号 / 名称" width="160" show-overflow-tooltip />
        <el-table-column label="操作" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" effect="plain" :type="row.action === 'DELETE' ? 'danger' : 'info'">
              {{ actionLabel(row.action) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="字段" width="130">
          <template #default="{ row }">{{ row.fieldLabel || row.fieldKey || '-' }}</template>
        </el-table-column>
        <el-table-column label="变更前" min-width="150" show-overflow-tooltip>
          <template #default="{ row }"><span class="old-val">{{ row.oldValue || '空' }}</span></template>
        </el-table-column>
        <el-table-column label="变更后" min-width="150" show-overflow-tooltip>
          <template #default="{ row }"><span class="new-val">{{ row.newValue || '空' }}</span></template>
        </el-table-column>
        <el-table-column label="操作人" width="110">
          <template #default="{ row }">{{ row.operatorName || '系统' }}</template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无修改记录" :image-size="90" />
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
import { onMounted, reactive, ref } from 'vue'
import { BIZ_TYPES, actionLabel, bizTypeLabel, pageChangeLogs } from '@/api/changeLog'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 20, bizType: '', keyword: '' })

async function load() {
  loading.value = true
  try {
    const { data } = await pageChangeLogs({ ...query })
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

function reset() {
  query.bizType = ''
  query.keyword = ''
  search()
}

onMounted(load)
</script>

<style scoped>
.page-sub {
  font-size: 12.5px;
  color: #8b97a8;
  margin: 4px 0 0;
}
.old-val {
  color: #98a3b3;
  text-decoration: line-through;
}
.new-val {
  color: #2f6bff;
  font-weight: 500;
}
</style>
