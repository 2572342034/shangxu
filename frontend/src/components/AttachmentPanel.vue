<template>
  <div class="attachment-panel">
    <div class="panel-head">
      <div class="panel-title">
        附件
        <span class="text-muted" style="font-size: 12px; font-weight: 400">共 {{ list.length }} 个</span>
      </div>
      <el-upload
        v-if="!readonly"
        :show-file-list="false"
        :http-request="handleUpload"
        :before-upload="beforeUpload"
        :disabled="uploading"
      >
        <el-button size="small" :loading="uploading">
          <el-icon><Upload /></el-icon>{{ uploading ? `上传中 ${percent}%` : '上传附件' }}
        </el-button>
      </el-upload>
    </div>

    <el-empty v-if="!list.length" description="暂无附件，可上传合同扫描件、发票、营业执照等" :image-size="36" />

    <div v-else class="file-list">
      <div v-for="file in list" :key="file.id" class="file-item">
        <div class="file-icon" :class="extClass(file.originalName)">
          {{ ext(file.originalName) }}
        </div>
        <div class="file-main">
          <div class="file-name" :title="file.originalName" @click="download(file)">{{ file.originalName }}</div>
          <div class="file-meta">
            {{ sizeText(file.fileSize) }} · {{ file.uploaderName || '-' }} · {{ formatDateTime(file.createTime) }}
          </div>
        </div>
        <div class="file-actions">
          <el-button link type="primary" size="small" @click="download(file)">下载</el-button>
          <el-button v-if="!readonly" v-perm="permCode" link type="danger" size="small" @click="remove(file)">删除</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteAttachment, downloadAttachment, listAttachments, uploadAttachment } from '@/api/attachment'
import { formatDateTime } from '@/utils/format'

const props = defineProps({
  readonly: { type: Boolean, default: false },
  bizType: { type: String, required: true },
  bizId: { type: [Number, String], required: true },
  permCode: { type: String, default: 'crm:customer:edit' }
})

const list = ref([])
const uploading = ref(false)
const percent = ref(0)

async function load() {
  if (!props.bizId) {
    list.value = []
    return
  }
  const { data } = await listAttachments(props.bizType, props.bizId)
  list.value = data || []
}

function beforeUpload(file) {
  if (file.size > 20 * 1024 * 1024) {
    ElMessage.error('单个文件不能超过 20MB')
    return false
  }
  return true
}

async function handleUpload(options) {
  uploading.value = true
  percent.value = 0
  try {
    await uploadAttachment(props.bizType, props.bizId, options.file, (p) => {
      percent.value = p
    })
    ElMessage.success('上传成功')
    await load()
  } finally {
    uploading.value = false
    percent.value = 0
  }
}

async function download(file) {
  const response = await downloadAttachment(file.id)
  const blob = new Blob([response.data])
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = file.originalName
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  window.URL.revokeObjectURL(url)
}

async function remove(file) {
  await ElMessageBox.confirm(`确定删除附件「${file.originalName}」？`, '提示', { type: 'warning' })
  await deleteAttachment(file.id)
  ElMessage.success('已删除')
  await load()
}

function ext(name) {
  const i = String(name || '').lastIndexOf('.')
  return i < 0 ? 'FILE' : String(name).slice(i + 1).toUpperCase().slice(0, 4)
}

function extClass(name) {
  const e = ext(name).toLowerCase()
  if (e === 'pdf') return 'pdf'
  if (['xls', 'xlsx', 'csv'].includes(e)) return 'excel'
  if (['doc', 'docx'].includes(e)) return 'word'
  if (['png', 'jpg', 'jpeg', 'gif', 'webp'].includes(e)) return 'image'
  return 'other'
}

function sizeText(size) {
  if (!size) return '0 KB'
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / 1024 / 1024).toFixed(2)} MB`
}

watch(() => [props.bizType, props.bizId], load)
onMounted(load)
</script>

<style scoped>
.attachment-panel {
  padding: 4px 0;
}
.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
.panel-title {
  font-size: 13.5px;
  font-weight: 500;
  color: #2c3648;
}
.file-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.file-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 12px;
  border: 1px solid #eef2f8;
  border-radius: 8px;
  background: #fbfcfe;
}
.file-icon {
  width: 34px;
  height: 34px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
  font-weight: 600;
  color: #fff;
  background: #8fa6c8;
  flex-shrink: 0;
}
.file-icon.pdf { background: #e2725b; }
.file-icon.excel { background: #22a06b; }
.file-icon.word { background: #3b73d1; }
.file-icon.image { background: #7b61c9; }
.file-main {
  flex: 1;
  min-width: 0;
}
.file-name {
  font-size: 13px;
  color: #2c3648;
  cursor: pointer;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.file-name:hover {
  color: #2f6bff;
}
.file-meta {
  font-size: 11.5px;
  color: #9aa5b6;
  margin-top: 2px;
}
.file-actions {
  display: flex;
  gap: 2px;
  flex-shrink: 0;
}
.attachment-panel :deep(.el-empty) {
  padding: 6px 0;
}
.attachment-panel :deep(.el-empty__description p) {
  font-size: 12px;
}
</style>
