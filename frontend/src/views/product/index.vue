<template>
  <div class="page-container">
    <div class="filter-bar">
      <el-input v-model="query.keyword" placeholder="搜索产品名称 / 编码 / 规格" clearable @keyup.enter="search" @clear="search">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-select v-model="query.categoryId" placeholder="所属目录" clearable style="width: 170px" @change="search">
        <el-option-group v-for="p in categories" :key="p.id" :label="p.name">
          <el-option :label="`${p.name}（全部）`" :value="p.id" />
          <el-option v-for="c in p.children || []" :key="c.id" :label="`　└ ${c.name}`" :value="c.id" />
        </el-option-group>
      </el-select>
      <el-select v-model="query.status" placeholder="销售状态" clearable style="width: 130px" @change="search">
        <el-option label="在售" :value="1" />
        <el-option label="停售" :value="0" />
      </el-select>
      <el-button type="primary" plain @click="search">查询</el-button>
      <el-button @click="resetQuery">重置</el-button>
      <div class="spacer"></div>
      <el-button v-perm="'crm:product:add'" plain @click="importVisible = true">
        <el-icon><Upload /></el-icon>导入
      </el-button>
      <el-button v-perm="'crm:product:list'" plain @click="doExport">
        <el-icon><Download /></el-icon>导出
      </el-button>
      <el-button link type="primary" @click="$router.push({ path: '/system/data-export', query: { module: 'PRODUCT' } })">批量导出</el-button>
      <el-button v-perm="'crm:product:add'" type="primary" @click="openForm()">
        <el-icon><Plus /></el-icon>新增产品
      </el-button>
    </div>

    <div class="table-card">
      <div class="table-toolbar">
        <div class="title">产品列表 <span class="text-muted" style="font-size: 12px; font-weight: 400">共 {{ total }} 个</span></div>
      </div>

      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column label="产品名称" min-width="190" fixed="left">
          <template #default="{ row }">
            <div class="product-cell" @click="openDetail(row)">
              <div class="p-icon"><el-icon><Goods /></el-icon></div>
              <div class="p-text">
                <div class="p-name">{{ row.name }}</div>
                <div class="p-desc">{{ row.code }} · {{ row.spec || row.unit || '-' }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="code" label="产品编码" width="108" />
        <el-table-column label="所属目录" width="116">
          <template #default="{ row }"><span class="cat-label">{{ row.category || '未分类' }}</span></template>
        </el-table-column>
        <el-table-column label="单位 / 规格" width="136">
          <template #default="{ row }">
            <div>{{ row.unit || '-' }}</div>
            <div class="cell-sub">{{ row.spec || '-' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="销售价" width="120" align="right">
          <template #default="{ row }"><span class="price">{{ formatMoney(row.price) }}</span></template>
        </el-table-column>
        <el-table-column label="毛利率" width="88" align="center">
          <template #default="{ row }">
            <span :class="grossRate(row) >= 50 ? 'rate-high' : 'rate-normal'">{{ grossRate(row) }}%</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="78" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small" effect="light">
              {{ row.status === 1 ? '在售' : '停售' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="190" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="info" size="small" title="查看修改记录" @click="$showChangeLog('PRODUCT', row.id, row.code)">记录</el-button>
            <el-button link type="primary" size="small" @click="openDetail(row)">详情</el-button>
            <el-button v-perm="'crm:product:edit'" link type="primary" size="small" @click="openForm(row)">编辑</el-button>
            <el-dropdown trigger="click" @command="(cmd) => rowCommand(cmd, row)">
              <el-button link type="primary" size="small" class="more-btn">更多<el-icon class="el-icon--right"><ArrowDown /></el-icon></el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="toggle">{{ row.status === 1 ? '设为停售' : '上架销售' }}</el-dropdown-item>
                  <el-dropdown-item v-perm="'crm:product:delete'" command="delete" divided>删除</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>
        <template #empty><el-empty description="暂无产品" :image-size="90" /></template>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="load"
          @current-change="load"
        />
      </div>
    </div>

    <!-- 导入 -->
    <el-dialog v-model="importVisible" title="批量导入产品" width="560px" destroy-on-close>
      <el-alert type="info" :closable="false" show-icon style="margin-bottom: 14px">
        <template #title>请使用 CSV 文件（UTF-8），列顺序见模板；产品编码留空将自动生成</template>
      </el-alert>
      <div class="import-actions">
        <el-button size="small" @click="downloadTemplate">
          <el-icon><Download /></el-icon>下载导入模板
        </el-button>
        <span class="text-muted" style="font-size: 12px">也可先「导出」现有产品，改完再导入</span>
      </div>
      <el-upload class="import-upload" drag accept=".csv" :show-file-list="false" :http-request="doImport">
        <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
        <div class="el-upload__text">拖拽 CSV 到此处，或<em>点击选择文件</em></div>
      </el-upload>
      <div v-if="importResult" class="import-result">
        <div>共 {{ importResult.total }} 行，成功 <b class="ok">{{ importResult.success }}</b> 条，跳过 <b class="bad">{{ importResult.failed }}</b> 条</div>
        <ul v-if="importResult.errors?.length">
          <li v-for="(e, i) in importResult.errors" :key="i">{{ e }}</li>
        </ul>
      </div>
      <template #footer>
        <el-button @click="importVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <ProductForm v-model="formVisible" :product="editingProduct" :groups="categories" @saved="onSaved" />
    <ProductDrawer v-model="detailVisible" :product-id="detailId" @edit="editFromDetail" @changed="onSaved" />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import ProductForm from './ProductForm.vue'
import ProductDrawer from './ProductDrawer.vue'
import { categoryTree, changeProductStatus, deleteProduct, pageProducts } from '@/api/product'
import { downloadProductTemplate, exportProducts, importProducts } from '@/api/data'
import { formatMoney } from '@/utils/format'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const categories = ref([])
const formVisible = ref(false)
const detailVisible = ref(false)
const detailId = ref(null)
const editingProduct = ref(null)
const importVisible = ref(false)
const importResult = ref(null)

const query = reactive({ pageNum: 1, pageSize: 10, keyword: '', status: '', categoryId: null })

function grossRate(row) {
  const price = Number(row.price || 0)
  if (!price) return 0
  return Math.round(((price - Number(row.cost || 0)) / price) * 100)
}

async function loadCategories() {
  const { data } = await categoryTree()
  categories.value = data || []
}

async function load() {
  loading.value = true
  try {
    const params = { ...query }
    if (params.status === '') delete params.status
    const { data } = await pageProducts(params)
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
  Object.assign(query, { keyword: '', status: '', categoryId: null, pageNum: 1 })
  load()
}

function openForm(row) {
  editingProduct.value = row ? { ...row } : null
  formVisible.value = true
}

function openDetail(row) {
  detailId.value = row.id
  detailVisible.value = true
}

function editFromDetail(product) {
  detailVisible.value = false
  openForm(product)
}

async function onSaved() {
  await Promise.all([load(), loadCategories()])
}

function rowCommand(command, row) {
  if (command === 'toggle') return toggleProduct(row)
  if (command === 'delete') return removeProduct(row)
}

async function toggleProduct(row) {
  const next = row.status === 1 ? 0 : 1
  await changeProductStatus(row.id, next)
  ElMessage.success(next === 1 ? '已上架' : '已停售')
  load()
}

async function removeProduct(row) {
  await ElMessageBox.confirm(`确定删除产品「${row.name}」吗？`, '提示', { type: 'warning' })
  await deleteProduct(row.id)
  ElMessage.success('已删除')
  onSaved()
}

function saveBlob(response, filename) {
  const url = window.URL.createObjectURL(new Blob([response.data]))
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  window.URL.revokeObjectURL(url)
}

async function doExport() {
  const response = await exportProducts()
  saveBlob(response, `产品列表-${new Date().toISOString().slice(0, 10)}.csv`)
  ElMessage.success('导出成功，可用 Excel 编辑后再导回')
}

async function downloadTemplate() {
  const response = await downloadProductTemplate()
  saveBlob(response, '产品导入模板.csv')
}

async function doImport(options) {
  importResult.value = null
  const { data } = await importProducts(options.file)
  importResult.value = data
  ElMessage.success(`导入完成：成功 ${data.success} 条，跳过 ${data.failed} 条`)
  await onSaved()
}

onMounted(async () => {
  await loadCategories()
  await load()
})
</script>

<style scoped>
.product-cell {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
}
.p-icon {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: #eef3ff;
  color: #2f6bff;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.p-text {
  min-width: 0;
}
.p-name {
  font-size: 13px;
  font-weight: 500;
  color: #2c3648;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.product-cell:hover .p-name {
  color: #2f6bff;
}
.p-desc,
.cell-sub {
  font-size: 11px;
  color: #a3adbd;
  margin-top: 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.cat-label {
  font-size: 12.5px;
  color: #5b6779;
}
.price {
  font-weight: 600;
  color: #1f2733;
}
.rate-high {
  color: #22b07d;
  font-weight: 600;
}
.rate-normal {
  color: #f5a623;
  font-weight: 600;
}
.more-btn {
  margin-left: 4px;
}
.import-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}
.import-upload :deep(.el-upload-dragger) {
  padding: 26px 0;
}
.import-result {
  margin-top: 14px;
  background: #f7f9fc;
  border-radius: 8px;
  padding: 12px 14px;
  font-size: 12.5px;
  color: #3c4759;
}
.import-result b.ok {
  color: #22b07d;
}
.import-result b.bad {
  color: #e2725b;
}
.import-result ul {
  margin: 8px 0 0;
  padding-left: 18px;
  max-height: 150px;
  overflow-y: auto;
  color: #8b97a8;
  line-height: 1.8;
}
:deep(.el-table .cell) {
  padding: 0 8px;
}
</style>
