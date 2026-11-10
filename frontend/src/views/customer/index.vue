<template>
  <div class="page-container">
    <div class="filter-bar">
      <el-input v-model="query.keyword" placeholder="搜索客户名称 / 编号" clearable @keyup.enter="search" @clear="search">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-select v-model="query.level" placeholder="客户级别" clearable @change="search">
        <el-option label="A 类（重点）" value="A" />
        <el-option label="B 类（普通）" value="B" />
        <el-option label="C 类（潜在）" value="C" />
      </el-select>
      <el-select v-model="query.status" placeholder="客户状态" clearable @change="search">
        <el-option label="跟进中" value="FOLLOWING" />
        <el-option label="已成交" value="DEAL" />
        <el-option label="无效" value="INVALID" />
      </el-select>
      <el-select v-model="query.industry" placeholder="所属行业" clearable @change="search">
        <el-option v-for="item in dictStore.options('industry')" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <el-button type="primary" plain @click="search">查询</el-button>
      <el-button @click="resetQuery">重置</el-button>
      <div class="spacer"></div>
      <el-radio-group v-model="query.poolType" @change="search">
        <el-radio-button value="PRIVATE">我的客户</el-radio-button>
        <el-radio-button value="PUBLIC">公海客户</el-radio-button>
        <el-radio-button value="HISTORY">历史客户</el-radio-button>
      </el-radio-group>
    </div>

    <div class="table-card">
      <div class="table-toolbar">
        <div class="title">
          客户列表
          <span class="text-muted" style="font-weight: 400; font-size: 12px; margin-left: 6px">
            共 {{ total }} 家
          </span>
        </div>
        <div class="toolbar-actions">
          <el-upload v-if="query.poolType !== 'HISTORY'" :show-file-list="false" :http-request="doImport" accept=".csv">
            <el-button v-perm="'crm:customer:add'" plain :loading="importing">
              <el-icon><Upload /></el-icon>导入
            </el-button>
          </el-upload>
          <el-button v-if="query.poolType !== 'HISTORY'" v-perm="'crm:customer:list'" plain @click="doExport">
            <el-icon><Download /></el-icon>导出
          </el-button>
          <el-button link type="primary" @click="$router.push({ path: '/system/data-export', query: { module: 'CUSTOMER' } })">批量导出</el-button>
          <el-button v-if="query.poolType !== 'HISTORY'" v-perm="'crm:customer:add'" type="primary" @click="openForm()">
            <el-icon><Plus /></el-icon>新建客户
          </el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column label="客户名称" min-width="260" fixed="left">
          <template #default="{ row }">
            <div class="customer-cell">
              <div class="cell-avatar">{{ initials(row.name) }}</div>
              <div class="cell-main">
                <div class="cell-title">
                  <span class="link-text" @click="openDrawer(row)">{{ row.name }}</span>
                  <el-tag v-if="row.poolType === 'PUBLIC'" type="warning" size="small" effect="plain">公海</el-tag>
                  <el-tag v-if="row.readOnly" type="info" size="small" effect="plain">已移交</el-tag>
                </div>
                <div class="cell-sub">{{ row.customerNo }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="级别" width="80" align="center">
          <template #default="{ row }"><DictTag type="customer_level" :value="row.level" /></template>
        </el-table-column>
        <el-table-column label="行业" width="110">
          <template #default="{ row }">{{ dictStore.label('industry', row.industry) }}</template>
        </el-table-column>
        <el-table-column prop="ownerName" label="负责人" width="90">
          <template #default="{ row }">{{ row.ownerName || '-' }}</template>
        </el-table-column>
        <el-table-column label="联系方式" width="140">
          <template #default="{ row }">
            <div v-if="row.readOnly" class="text-muted">已隐藏</div>
            <template v-else>
              <div>{{ row.phone || '-' }}</div>
              <div class="cell-sub">{{ row.city || '-' }}</div>
            </template>
          </template>
        </el-table-column>
        <el-table-column label="商机 / 联系人" width="110" align="center">
          <template #default="{ row }">
            <span v-if="row.readOnly" class="text-muted">-</span>
            <template v-else>
              <span class="count-badge">{{ row.opportunityCount || 0 }}</span>
              <span class="count-divider">/</span>
              <span class="count-badge">{{ row.contactCount || 0 }}</span>
            </template>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }"><DictTag type="customer_status" :value="row.status" /></template>
        </el-table-column>
        <el-table-column label="最近跟进" width="120">
          <template #default="{ row }">
            <span :class="{ 'text-muted': !row.lastFollowTime }">{{ fromNow(row.lastFollowTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="info" size="small" title="查看修改记录" @click="$showChangeLog('CUSTOMER', row.id, row.customerNo)">记录</el-button>
            <el-button link type="primary" size="small" @click="openDrawer(row)">详情</el-button>
            <el-button v-if="row.poolType === 'PRIVATE' && !row.readOnly" v-perm="'crm:customer:edit'" link type="primary" size="small" @click="openForm(row)">
              编辑
            </el-button>
            <el-dropdown v-if="row.poolType === 'PRIVATE' && !row.readOnly" trigger="click" @command="(cmd) => handleCommand(cmd, row)">
              <el-button link type="primary" size="small">
                更多<el-icon class="el-icon--right"><ArrowDown /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item v-if="canTransfer" command="transfer">转移负责人</el-dropdown-item>
                  <el-dropdown-item v-if="canTransfer" command="public">放入公海</el-dropdown-item>
                  <el-dropdown-item v-if="canDelete" command="delete" divided>删除客户</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
            <span v-if="row.readOnly" class="readonly-tag">只读</span>
            <el-button v-if="row.poolType === 'PUBLIC' && !row.readOnly" v-perm="'crm:customer:edit'" link type="success" size="small" @click="claim(row)">
              领取
            </el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无客户数据" :image-size="90" />
        </template>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="load"
          @current-change="load"
        />
      </div>
    </div>

    <CustomerForm v-model="formVisible" :data="currentRow" @success="load" />
    <CustomerDrawer v-model="drawerVisible" :customer-id="drawerId" @edit="handleEditFromDrawer" @refresh="load" />

    <el-dialog v-model="transferVisible" title="转移客户负责人" width="420px">
      <el-form label-width="80px">
        <el-form-item label="新负责人">
          <el-select v-model="transferForm.ownerUserId" filterable placeholder="请选择负责人" style="width: 100%">
            <el-option v-for="user in users" :key="user.id" :label="`${user.nickname}（${user.deptName || '-'}）`" :value="user.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="transferVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitTransfer">确定转移</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import DictTag from '@/components/DictTag.vue'
import CustomerForm from './CustomerForm.vue'
import CustomerDrawer from './CustomerDrawer.vue'
import { claimCustomer, deleteCustomer, pageCustomers, toPublicCustomer, transferCustomer } from '@/api/customer'
import { userOptions } from '@/api/system'
import { useDictStore } from '@/store/dict'
import { useUserStore } from '@/store/user'
import { fromNow, initials } from '@/utils/format'
import { exportCustomers, importCustomers } from '@/api/data'

const route = useRoute()
const router = useRouter()
const dictStore = useDictStore()
const userStore = useUserStore()

const loading = ref(false)
const saving = ref(false)
const list = ref([])
const total = ref(0)
const users = ref([])

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  level: '',
  status: '',
  industry: '',
  poolType: 'PRIVATE'
})

const formVisible = ref(false)
const currentRow = ref(null)
const drawerVisible = ref(false)
const drawerId = ref(null)
const transferVisible = ref(false)
const transferForm = reactive({ id: null, ownerUserId: null })

const canTransfer = computed(() => userStore.hasPerm('crm:customer:transfer'))
const canDelete = computed(() => userStore.hasPerm('crm:customer:delete'))

async function load() {
  loading.value = true
  try {
    const { data } = await pageCustomers(query)
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
  Object.assign(query, { keyword: '', level: '', status: '', industry: '', pageNum: 1 })
  load()
}

function openForm(row) {
  currentRow.value = row ? { ...row } : null
  formVisible.value = true
}

function openDrawer(row) {
  drawerId.value = row.id
  drawerVisible.value = true
}

function handleEditFromDrawer(customer) {
  drawerVisible.value = false
  openForm(customer)
}

async function handleCommand(command, row) {
  if (command === 'transfer') {
    if (!users.value.length) {
      const { data } = await userOptions()
      users.value = data || []
    }
    transferForm.id = row.id
    transferForm.ownerUserId = row.ownerUserId
    transferVisible.value = true
    return
  }
  if (command === 'public') {
    await ElMessageBox.confirm(`确定将「${row.name}」放入公海吗？放入后其他销售可领取。`, '提示', { type: 'warning' })
    await toPublicCustomer(row.id)
    ElMessage.success('已放入公海')
    load()
    return
  }
  if (command === 'delete') {
    await ElMessageBox.confirm(`确定删除客户「${row.name}」吗？`, '删除确认', { type: 'warning' })
    await deleteCustomer(row.id)
    ElMessage.success('删除成功')
    load()
  }
}

async function submitTransfer() {
  if (!transferForm.ownerUserId) {
    ElMessage.warning('请选择新的负责人')
    return
  }
  saving.value = true
  try {
    const user = users.value.find((u) => u.id === transferForm.ownerUserId)
    await transferCustomer(transferForm.id, { ownerUserId: transferForm.ownerUserId, deptId: user?.deptId })
    ElMessage.success('转移成功')
    transferVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function claim(row) {
  await ElMessageBox.confirm(`确定领取客户「${row.name}」吗？`, '提示', { type: 'info' })
  await claimCustomer(row.id)
  ElMessage.success('领取成功，已加入我的客户')
  query.poolType = 'PRIVATE'
  load()
}

const importing = ref(false)

async function doExport() {
  const response = await exportCustomers()
  const url = window.URL.createObjectURL(new Blob([response.data]))
  const a = document.createElement('a')
  a.href = url
  a.download = `客户列表-${new Date().toISOString().slice(0, 10)}.csv`
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  window.URL.revokeObjectURL(url)
  ElMessage.success('导出成功，可用 Excel 直接打开')
}

async function doImport(options) {
  importing.value = true
  try {
    const { data } = await importCustomers(options.file)
    ElMessage.success(`导入完成：成功 ${data.success} 条，跳过 ${data.failed} 条`)
    if (data.errors?.length) {
      ElMessageBox.alert(data.errors.join('<br/>'), '部分数据未导入', { dangerouslyUseHTMLString: true })
    }
    await load()
  } finally {
    importing.value = false
  }
}

onMounted(async () => {
  await dictStore.load()
  await load()
  if (route.query.create) {
    openForm()
    router.replace({ query: {} })
  } else if (route.query.id) {
    drawerId.value = Number(route.query.id)
    drawerVisible.value = true
    router.replace({ query: {} })
  }
})
</script>

<style scoped>
.customer-cell {
  display: flex;
  align-items: center;
  gap: 9px;
}

.cell-avatar {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: #eef3ff;
  color: #2f6bff;
  font-size: 11px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.cell-main {
  min-width: 0;
}

.cell-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 500;
  color: #2c3648;
}

.cell-sub {
  font-size: 11px;
  color: #a3adbd;
  margin-top: 2px;
}

.count-badge {
  display: inline-block;
  min-width: 20px;
  color: #2f6bff;
  font-weight: 600;
}

.count-divider {
  color: #d5dbe5;
  margin: 0 2px;
}
.toolbar-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}
.readonly-tag {
  font-size: 12px;
  color: #9aa5b6;
}
</style>
