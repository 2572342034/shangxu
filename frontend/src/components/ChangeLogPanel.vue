<template>
  <div v-loading="loading" class="change-log-panel">
    <div v-if="showTitle" class="log-head">
      <span class="log-title">修改记录</span>
      <span class="log-count">共 {{ list.length }} 条</span>
      <el-button link type="primary" size="small" @click="load">刷新</el-button>
    </div>
    <el-empty v-if="!loading && !list.length" description="暂无修改记录" :image-size="70" />
    <el-table v-else :data="list" size="small" border stripe>
      <el-table-column prop="createTime" label="时间" width="155" />
      <el-table-column label="操作人" width="100">
        <template #default="{ row }">{{ row.operatorName || '系统' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="100" align="center">
        <template #default="{ row }">
          <el-tag size="small" effect="plain" :type="actionTag(row.action)">{{ actionLabel(row.action) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="字段" width="130">
        <template #default="{ row }">{{ row.fieldLabel || row.fieldKey || '-' }}</template>
      </el-table-column>
      <el-table-column label="变更前" min-width="140" show-overflow-tooltip>
        <template #default="{ row }"><span class="old-val">{{ row.oldValue || '空' }}</span></template>
      </el-table-column>
      <el-table-column label="变更后" min-width="140" show-overflow-tooltip>
        <template #default="{ row }"><span class="new-val">{{ row.newValue || '空' }}</span></template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { actionLabel } from '@/api/changeLog'
import { getChangeLogs } from '@/api/changeLog'

const props = defineProps({
  bizType: { type: String, required: true },
  bizId: { type: [Number, String], default: null },
  showTitle: { type: Boolean, default: false }
})

const loading = ref(false)
const list = ref([])

function actionTag(action) {
  if (action === 'DELETE' || action === 'RECYCLE') return 'danger'
  if (action === 'CREATE' || action === 'CLAIM' || action === 'TRANSFER') return 'success'
  if (action === 'TO_PUBLIC') return 'warning'
  return 'info'
}

async function load() {
  if (!props.bizId) {
    list.value = []
    return
  }
  loading.value = true
  try {
    const { data } = await getChangeLogs({ bizType: props.bizType, bizId: props.bizId, limit: 100 })
    list.value = data || []
  } catch (e) {
    list.value = []
  } finally {
    loading.value = false
  }
}

watch(() => [props.bizType, props.bizId], load, { immediate: true })

defineExpose({ load })
</script>

<style scoped>
.change-log-panel {
  padding: 2px 0;
}
.log-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}
.log-title {
  font-size: 13.5px;
  font-weight: 500;
  color: #2c3648;
}
.log-count {
  font-size: 12px;
  color: #9aa5b6;
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
