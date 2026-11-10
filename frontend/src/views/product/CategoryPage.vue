<template>
  <div class="page-container category-layout">
    <!-- 左：目录树 -->
    <div class="tree-card">
      <div class="card-head">
        <span class="card-title">产品目录</span>
        <el-button v-perm="'crm:product:add'" link type="primary" size="small" @click="openCategory(null, false)">
          <el-icon><Plus /></el-icon>根目录
        </el-button>
      </div>
      <el-input v-model="keyword" placeholder="筛选目录" clearable size="small" class="tree-search">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <div class="all-node" :class="{ active: !currentId }" @click="selectAll">
        <el-icon><Files /></el-icon><span>全部产品</span><span class="node-count">{{ allCount }}</span>
      </div>
      <el-tree
        ref="treeRef"
        :data="tree"
        :props="{ label: 'name', children: 'children' }"
        node-key="id"
        default-expand-all
        :expand-on-click-node="false"
        :filter-node-method="filterNode"
        highlight-current
        @node-click="selectNode"
      >
        <template #default="{ data }">
          <div class="tree-node">
            <span class="node-name" :class="{ off: data.status === 0 }">{{ data.name }}</span>
            <span class="node-count">{{ data.productCount || 0 }}</span>
            <el-dropdown trigger="click" @command="(cmd) => nodeCommand(cmd, data)">
              <el-icon class="node-more" @click.stop><MoreFilled /></el-icon>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="addChild">新增子分类</el-dropdown-item>
                  <el-dropdown-item command="edit">编辑</el-dropdown-item>
                  <el-dropdown-item command="toggle">{{ data.status === 0 ? '启用' : '停用' }}</el-dropdown-item>
                  <el-dropdown-item command="delete" divided>删除</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </template>
      </el-tree>
    </div>

    <!-- 右：分类信息 + 目录内产品 -->
    <div class="detail-area">
      <div class="info-card">
        <template v-if="current">
          <div class="info-head">
            <div>
              <div class="info-title">
                {{ current.name }}
                <el-tag :type="current.status === 0 ? 'info' : 'success'" size="small" effect="light">
                  {{ current.status === 0 ? '已停用' : '启用中' }}
                </el-tag>
              </div>
              <div class="info-sub">{{ parentName || '根目录' }} · 编码 {{ current.code || '未设置' }}</div>
            </div>
            <div class="info-actions">
              <el-button v-perm="'crm:product:add'" size="small" type="primary" @click="openCategory(current, true)">新增子分类</el-button>
              <el-button v-perm="'crm:product:edit'" size="small" plain @click="openCategory(current, false)">编辑</el-button>
              <el-button v-perm="'crm:product:edit'" size="small" plain @click="toggleCategory(current)">
                {{ current.status === 0 ? '启用' : '停用' }}
              </el-button>
            </div>
          </div>
          <div class="info-stats">
            <div><span>本级产品</span><b>{{ currentOwnCount }}</b></div>
            <div><span>含子目录</span><b>{{ current.productCount || 0 }}</b></div>
            <div><span>子分类</span><b>{{ (current.children || []).length }}</b></div>
            <div><span>排序</span><b>{{ current.sort ?? 0 }}</b></div>
          </div>
          <div class="info-remark">{{ current.remark || '暂无备注' }}</div>
        </template>
        <template v-else>
          <div class="info-head">
            <div>
              <div class="info-title">全部产品</div>
              <div class="info-sub">从左侧选择一个目录，可查看和维护该目录下的产品</div>
            </div>
            <el-button v-perm="'crm:product:add'" size="small" type="primary" @click="openProduct()">新增产品</el-button>
          </div>
          <div class="info-stats">
            <div><span>产品总数</span><b>{{ allCount }}</b></div>
            <div><span>一级目录</span><b>{{ tree.length }}</b></div>
            <div><span>目录节点</span><b>{{ nodeCount }}</b></div>
            <div><span>停售产品</span><b>{{ stoppedCount }}</b></div>
          </div>
        </template>
      </div>

      <div class="table-card">
        <div class="table-toolbar">
          <div class="title">
            {{ current ? current.name + ' · 产品' : '全部产品' }}
            <span class="text-muted" style="font-size: 12px; font-weight: 400">共 {{ products.length }} 个</span>
          </div>
          <el-button v-perm="'crm:product:add'" size="small" plain @click="openProduct()">新增产品</el-button>
        </div>
        <el-table v-loading="loading" :data="products" stripe>
          <el-table-column label="产品名称" min-width="200">
            <template #default="{ row }">
              <div class="p-cell" @click="openDetail(row)">
                <div class="p-icon"><el-icon><Goods /></el-icon></div>
                <div>
                  <div class="p-name">{{ row.name }}</div>
                  <div class="p-sub">{{ row.code }} · {{ row.spec || row.unit || '-' }}</div>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="category" label="所属目录" width="120" />
          <el-table-column label="销售价" width="130" align="right">
            <template #default="{ row }"><span class="price">{{ formatMoney(row.price) }}</span></template>
          </el-table-column>
          <el-table-column label="状态" width="86" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small" effect="light">
                {{ row.status === 1 ? '在售' : '停售' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="90" align="center">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="openDetail(row)">详情</el-button>
            </template>
          </el-table-column>
          <template #empty><el-empty description="该目录下暂无产品" :image-size="80" /></template>
        </el-table>
      </div>
    </div>

    <!-- 分类表单 -->
    <el-dialog v-model="categoryVisible" :title="categoryForm.id ? '编辑分类' : '新增分类'" width="440px" destroy-on-close>
      <el-form ref="categoryFormRef" :model="categoryForm" :rules="categoryRules" label-width="86px">
        <el-form-item label="上级目录">
          <el-select v-model="categoryForm.parentId" style="width: 100%" clearable placeholder="根目录">
            <el-option v-for="p in tree" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="categoryForm.name" />
        </el-form-item>
        <el-form-item label="分类编码">
          <el-input v-model="categoryForm.code" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="categoryForm.sort" :min="0" style="width: 100%" controls-position="right" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="categoryForm.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="停用" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="categoryForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="categoryVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitCategory">保存</el-button>
      </template>
    </el-dialog>

    <ProductForm
      v-model="formVisible"
      :product="editingProduct"
      :groups="tree"
      :default-category-id="currentId"
      @saved="refresh"
    />
    <ProductDrawer v-model="detailVisible" :product-id="detailId" @changed="refresh" />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import ProductForm from './ProductForm.vue'
import ProductDrawer from './ProductDrawer.vue'
import {
  categoryTree,
  changeCategoryStatus,
  createCategory,
  deleteCategory,
  pageProducts,
  updateCategory
} from '@/api/product'
import { formatMoney } from '@/utils/format'

const tree = ref([])
const treeRef = ref()
const keyword = ref('')
const currentId = ref(null)
const current = ref(null)
const products = ref([])
const allCount = ref(0)
const loading = ref(false)
const saving = ref(false)
const categoryVisible = ref(false)
const categoryFormRef = ref()
const formVisible = ref(false)
const detailVisible = ref(false)
const detailId = ref(null)
const editingProduct = ref(null)

const categoryForm = reactive({ id: null, parentId: 0, name: '', code: '', sort: 0, status: 1, remark: '' })
const categoryRules = { name: [{ required: true, message: '请填写分类名称', trigger: 'blur' }] }

const nodeCount = computed(() => tree.value.reduce((sum, r) => sum + 1 + (r.children || []).length, 0))
const stoppedCount = computed(() => products.value.filter((p) => p.status !== 1).length)
const parentName = computed(() => {
  if (!current.value || !current.value.parentId) return ''
  const p = tree.value.find((r) => r.id === current.value.parentId)
  return p ? p.name : ''
})
const currentOwnCount = computed(() => {
  if (!current.value) return 0
  const total = current.value.productCount || 0
  const childSum = (current.value.children || []).reduce((s, c) => s + (c.productCount || 0), 0)
  return Math.max(0, total - childSum)
})

function filterNode(value, data) {
  return !value || String(data.name || '').includes(value)
}

watch(keyword, (v) => treeRef.value?.filter(v))

async function loadTree() {
  const { data } = await categoryTree()
  tree.value = data || []
  if (currentId.value) {
    current.value = findNode(currentId.value)
  }
}

function findNode(id) {
  for (const p of tree.value) {
    if (p.id === id) return p
    const hit = (p.children || []).find((c) => c.id === id)
    if (hit) return { ...hit, parentId: p.id }
  }
  return null
}

async function loadProducts() {
  loading.value = true
  try {
    const params = { pageNum: 1, pageSize: 100 }
    if (currentId.value) params.categoryId = currentId.value
    const { data } = await pageProducts(params)
    products.value = data.records || []
    if (!currentId.value) allCount.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function refresh() {
  loadTree()
  loadProducts()
}

function selectAll() {
  currentId.value = null
  current.value = null
  treeRef.value?.setCurrentKey(null)
  loadProducts()
}

function selectNode(data) {
  currentId.value = data.id
  current.value = findNode(data.id)
  loadProducts()
}

function openCategory(node, isChild) {
  Object.assign(categoryForm, {
    id: null,
    parentId: isChild && node ? node.id : node?.parentId || 0,
    name: '',
    code: '',
    sort: 0,
    status: 1,
    remark: ''
  })
  if (node && !isChild) {
    Object.assign(categoryForm, {
      id: node.id, parentId: node.parentId || 0, name: node.name,
      code: node.code || '', sort: node.sort || 0, status: node.status, remark: node.remark || ''
    })
  }
  categoryVisible.value = true
}

function nodeCommand(cmd, node) {
  if (cmd === 'addChild') return openCategory(node, true)
  if (cmd === 'edit') return openCategory(node, false)
  if (cmd === 'toggle') return toggleCategory(node)
  if (cmd === 'delete') return removeCategory(node)
}

async function submitCategory() {
  await categoryFormRef.value.validate()
  saving.value = true
  try {
    if (categoryForm.id) await updateCategory(categoryForm.id, { ...categoryForm })
    else await createCategory({ ...categoryForm })
    ElMessage.success('分类已保存')
    categoryVisible.value = false
    refresh()
  } finally {
    saving.value = false
  }
}

async function toggleCategory(node) {
  await changeCategoryStatus(node.id, node.status === 0 ? 1 : 0)
  ElMessage.success('状态已更新')
  refresh()
}

async function removeCategory(node) {
  await ElMessageBox.confirm(`确定删除分类「${node.name}」吗？`, '提示', { type: 'warning' })
  await deleteCategory(node.id)
  ElMessage.success('已删除')
  if (currentId.value === node.id) selectAll()
  else refresh()
}

function openProduct() {
  editingProduct.value = null
  formVisible.value = true
}

function openDetail(row) {
  detailId.value = row.id
  detailVisible.value = true
}

onMounted(async () => {
  await loadTree()
  await loadProducts()
})
</script>

<style scoped>
.category-layout {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}
.tree-card {
  width: 258px;
  flex-shrink: 0;
  background: #fff;
  border: 1px solid #edf0f5;
  border-radius: var(--crm-card-radius);
  padding: 14px 12px;
  min-height: 600px;
}
.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}
.card-title {
  font-size: 14px;
  font-weight: 500;
  color: #2c3648;
}
.tree-search {
  margin-bottom: 10px;
}
.all-node {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  border-radius: 8px;
  font-size: 13px;
  color: #3c4759;
  cursor: pointer;
  margin-bottom: 4px;
}
.all-node:hover,
.all-node.active {
  background: #eef3ff;
  color: #2f6bff;
}
.all-node .node-count {
  margin-left: auto;
}
.node-count {
  font-size: 11.5px;
  color: #9aa5b6;
  background: #f4f6fa;
  border-radius: 10px;
  padding: 0 6px;
  line-height: 18px;
  min-width: 20px;
  text-align: center;
}
.tree-node {
  display: flex;
  align-items: center;
  width: 100%;
  gap: 6px;
}
.node-name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.node-name.off {
  color: #b0bac9;
  text-decoration: line-through;
}
.node-more {
  color: #9aa5b6;
  cursor: pointer;
  visibility: hidden;
}
.tree-node:hover .node-more {
  visibility: visible;
}
.detail-area {
  flex: 1;
  min-width: 0;
}
.info-card {
  background: #fff;
  border: 1px solid #edf0f5;
  border-radius: var(--crm-card-radius);
  padding: 16px 18px;
  margin-bottom: 12px;
}
.info-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}
.info-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 17px;
  font-weight: 500;
  color: #1f2a3d;
}
.info-sub {
  font-size: 12px;
  color: #9aa5b6;
  margin-top: 5px;
}
.info-actions {
  display: flex;
  gap: 6px;
  flex-shrink: 0;
}
.info-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
  margin-top: 16px;
}
.info-stats div {
  background: #f7f9fc;
  border-radius: 8px;
  padding: 10px 12px;
}
.info-stats span {
  display: block;
  font-size: 11.5px;
  color: #8b97a8;
  margin-bottom: 4px;
}
.info-stats b {
  font-size: 18px;
  font-weight: 500;
  color: #2c3648;
}
.info-remark {
  margin-top: 12px;
  font-size: 12.5px;
  color: #5b6779;
}
.p-cell {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
}
.p-icon {
  width: 30px;
  height: 30px;
  border-radius: 8px;
  background: #eef3ff;
  color: #2f6bff;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.p-name {
  font-size: 13px;
  font-weight: 500;
  color: #2c3648;
}
.p-cell:hover .p-name {
  color: #2f6bff;
}
.p-sub {
  font-size: 11px;
  color: #a3adbd;
  margin-top: 2px;
}
.price {
  font-weight: 600;
  color: #1f2733;
}
:deep(.el-tree-node__content) {
  height: 34px;
  border-radius: 6px;
}
</style>
