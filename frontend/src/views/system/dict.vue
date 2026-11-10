<template>
  <div class="page-container">
    <div class="filter-bar">
      <el-input v-model="query.keyword" placeholder="搜索字典标签" clearable style="width: 200px" @keyup.enter="search" @clear="search" />
      <el-select v-model="query.dictType" placeholder="字典类型" clearable filterable allow-create style="width: 200px" @change="search">
        <el-option v-for="type in dictTypes" :key="type" :label="type" :value="type" />
      </el-select>
      <el-button type="primary" plain @click="search">查询</el-button>
      <el-button @click="resetQuery">重置</el-button>
      <div class="spacer"></div>
      <el-button v-perm="'system:dict:add'" type="primary" @click="openForm()">
        <el-icon><Plus /></el-icon>新增字典
      </el-button>
    </div>

    <div class="table-card">
      <div class="table-toolbar">
        <div class="title">数据字典 <span class="text-muted" style="font-size: 12px; font-weight: 400">共 {{ total }} 项</span></div>
      </div>

      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column prop="dictType" label="字典类型" width="200">
          <template #default="{ row }"><code class="code">{{ row.dictType }}</code></template>
        </el-table-column>
        <el-table-column prop="dictLabel" label="字典标签" width="180" />
        <el-table-column prop="dictValue" label="字典值" width="160" />
        <el-table-column prop="sort" label="排序" width="80" align="center" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small" effect="light">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
        <el-table-column label="操作" width="140" align="center" fixed="right">
          <template #default="{ row }">
            <el-button v-perm="'system:dict:edit'" link type="primary" size="small" @click="openForm(row)">编辑</el-button>
            <el-button v-perm="'system:dict:delete'" link type="danger" size="small" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty description="暂无字典数据" :image-size="90" /></template>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next"
          background
          @size-change="load"
          @current-change="load"
        />
      </div>
    </div>

    <el-dialog v-model="formVisible" :title="form.id ? '编辑字典' : '新增字典'" width="520px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="字典类型" prop="dictType">
          <el-select v-model="form.dictType" filterable allow-create default-first-option style="width: 100%">
            <el-option v-for="type in dictTypes" :key="type" :label="type" :value="type" />
          </el-select>
        </el-form-item>
        <el-form-item label="字典标签" prop="dictLabel">
          <el-input v-model="form.dictLabel" />
        </el-form-item>
        <el-form-item label="字典值" prop="dictValue">
          <el-input v-model="form.dictValue" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" style="width: 100%" controls-position="right" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="停用" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { allDicts, createDict, deleteDict, pageDicts, updateDict } from '@/api/system'
import { useDictStore } from '@/store/dict'

const dictStore = useDictStore()

const loading = ref(false)
const saving = ref(false)
const list = ref([])
const total = ref(0)
const dictTypes = ref([])
const formRef = ref()
const formVisible = ref(false)

const query = reactive({ pageNum: 1, pageSize: 20, keyword: '', dictType: '' })

const form = reactive({
  id: null,
  dictType: '',
  dictLabel: '',
  dictValue: '',
  sort: 0,
  status: 1,
  remark: ''
})

const rules = {
  dictType: [{ required: true, message: '请选择或输入字典类型', trigger: 'change' }],
  dictLabel: [{ required: true, message: '请输入字典标签', trigger: 'blur' }],
  dictValue: [{ required: true, message: '请输入字典值', trigger: 'blur' }]
}

async function load() {
  loading.value = true
  try {
    const { data } = await pageDicts(query)
    list.value = data.records || []
    total.value = data.total || 0
    const { data: all } = await allDicts()
    dictTypes.value = Object.keys(all || {})
  } finally {
    loading.value = false
  }
}

function search() {
  query.pageNum = 1
  load()
}

function resetQuery() {
  Object.assign(query, { keyword: '', dictType: '', pageNum: 1 })
  load()
}

function openForm(row) {
  Object.assign(form, { id: null, dictType: query.dictType || '', dictLabel: '', dictValue: '', sort: 0, status: 1, remark: '' })
  if (row) Object.assign(form, row)
  formVisible.value = true
}

async function submit() {
  await formRef.value.validate()
  saving.value = true
  try {
    if (form.id) {
      await updateDict(form.id, form)
    } else {
      await createDict(form)
    }
    ElMessage.success('保存成功')
    formVisible.value = false
    await load()
    await dictStore.load(true)
  } finally {
    saving.value = false
  }
}

async function remove(row) {
  await ElMessageBox.confirm(`确定删除字典「${row.dictLabel}」吗？`, '提示', { type: 'warning' })
  await deleteDict(row.id)
  ElMessage.success('已删除')
  await load()
  await dictStore.load(true)
}

onMounted(load)
</script>

<style scoped>
.code {
  font-family: Consolas, monospace;
  font-size: 11.5px;
  background: #f4f6fa;
  padding: 2px 6px;
  border-radius: 4px;
  color: #5b6779;
}
</style>
