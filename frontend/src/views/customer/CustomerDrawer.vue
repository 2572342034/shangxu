<template>
  <el-drawer v-model="visible" size="900px" :with-header="false" destroy-on-close>
    <div v-loading="loading" class="drawer-body">
      <template v-if="detail.customer">
        <!-- 头部 -->
        <div class="profile-head">
          <div class="company-avatar">{{ initials(detail.customer.name) }}</div>
          <div class="profile-main">
            <div class="profile-title">
              <h3>{{ detail.customer.name }}</h3>
              <DictTag type="customer_level" :value="detail.customer.level" />
              <DictTag type="customer_status" :value="detail.customer.status" />
              <el-tag v-if="detail.customer.poolType === 'PUBLIC'" type="warning" size="small" effect="light">公海</el-tag>
            </div>
            <div class="profile-meta">
              <span><el-icon><Tickets /></el-icon>{{ detail.customer.customerNo }}</span>
              <span><el-icon><User /></el-icon>{{ detail.customer.ownerName || '未分配' }}</span>
              <span><el-icon><Phone /></el-icon>{{ detail.customer.phone || '-' }}</span>
              <span><el-icon><Location /></el-icon>{{ detail.customer.province }}{{ detail.customer.city }}</span>
            </div>
          </div>
          <div class="profile-actions">
            <el-button v-if="!readOnly" v-perm="'crm:activity:add'" type="primary" plain @click="activityVisible = true">
              <el-icon><ChatLineSquare /></el-icon>记跟进
            </el-button>
            <el-button v-if="!readOnly" v-perm="'crm:customer:edit'" @click="$emit('edit', detail.customer)">编辑</el-button>
            <el-button @click="visible = false">关闭</el-button>
          </div>
        </div>

        <el-alert v-if="readOnly" type="warning" :closable="false" show-icon class="readonly-alert"
          title="该客户已移交，仅可查看摘要" description="联系方式、地址、备注及关联商机/合同/跟进记录已隐藏，且不能编辑或操作。" />

        <!-- 数据概览 -->
        <div class="stat-row">
          <div class="stat-item">
            <div class="stat-num">{{ detail.contacts?.length || 0 }}</div>
            <div class="stat-label">联系人</div>
          </div>
          <div class="stat-item">
            <div class="stat-num">{{ detail.opportunities?.length || 0 }}</div>
            <div class="stat-label">商机数</div>
          </div>
          <div class="stat-item">
            <div class="stat-num">{{ formatWan(detail.opportunityAmount) }}</div>
            <div class="stat-label">商机金额</div>
          </div>
          <div class="stat-item">
            <div class="stat-num">{{ formatWan(detail.contractAmount) }}</div>
            <div class="stat-label">合同金额</div>
          </div>
          <div class="stat-item">
            <div class="stat-num">{{ detail.activities?.length || 0 }}</div>
            <div class="stat-label">跟进次数</div>
          </div>
        </div>

        <el-tabs v-model="tab" class="detail-tabs">
          <el-tab-pane label="基本信息" name="info">
            <el-descriptions :column="2" border size="small">
              <el-descriptions-item label="客户编号">{{ detail.customer.customerNo }}</el-descriptions-item>
              <el-descriptions-item label="客户简称">{{ detail.customer.shortName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="所属行业">
                {{ dictStore.label('industry', detail.customer.industry) }}
              </el-descriptions-item>
              <el-descriptions-item label="客户来源">
                {{ dictStore.label('customer_source', detail.customer.source) }}
              </el-descriptions-item>
              <el-descriptions-item label="人员规模">
                {{ dictStore.label('customer_scale', detail.customer.scale) }}
              </el-descriptions-item>
              <el-descriptions-item label="官网">{{ detail.customer.website || '-' }}</el-descriptions-item>
              <el-descriptions-item label="所属部门">{{ detail.customer.deptName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="负责人">{{ detail.customer.ownerName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="最近跟进">{{ formatDateTime(detail.customer.lastFollowTime) }}</el-descriptions-item>
              <el-descriptions-item label="下次跟进">{{ formatDateTime(detail.customer.nextFollowTime) }}</el-descriptions-item>
              <el-descriptions-item label="详细地址" :span="2">
                {{ `${detail.customer.province || ''}${detail.customer.city || ''}${detail.customer.address || ''}` || '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="备注" :span="2">{{ detail.customer.remark || '-' }}</el-descriptions-item>
              <el-descriptions-item label="创建时间" :span="2">{{ formatDateTime(detail.customer.createTime) }}</el-descriptions-item>
            </el-descriptions>
          </el-tab-pane>

          <el-tab-pane :label="`联系人 (${detail.contacts?.length || 0})`" name="contact">
            <div class="tab-toolbar">
              <el-button v-if="!readOnly" v-perm="'crm:contact:add'" type="primary" size="small" plain @click="openContactForm()">
                新增联系人
              </el-button>
            </div>
            <el-table :data="detail.contacts" size="small" border>
              <el-table-column prop="name" label="姓名" width="90">
                <template #default="{ row }">
                  <span class="contact-name">{{ row.name }}</span>
                  <el-tag v-if="row.isPrimary" type="danger" size="small" effect="plain" style="margin-left: 4px">关键</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="post" label="职务" width="110" />
              <el-table-column prop="mobile" label="手机" width="120" />
              <el-table-column prop="email" label="邮箱" min-width="150" show-overflow-tooltip />
              <el-table-column label="操作" width="90" align="center">
                <template #default="{ row }">
                  <el-button v-if="!readOnly" v-perm="'crm:contact:edit'" link type="primary" size="small" @click="openContactForm(row)">编辑</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <el-tab-pane :label="`商机 (${detail.opportunities?.length || 0})`" name="opp">
            <el-table :data="detail.opportunities" size="small" border>
              <el-table-column prop="name" label="商机名称" min-width="180" show-overflow-tooltip />
              <el-table-column label="金额" width="120" align="right">
                <template #default="{ row }">{{ formatMoney(row.amount) }}</template>
              </el-table-column>
              <el-table-column label="阶段" width="100">
                <template #default="{ row }"><DictTag type="opp_stage" :value="row.stage" /></template>
              </el-table-column>
              <el-table-column prop="winRate" label="赢单率" width="80" align="center">
                <template #default="{ row }">{{ row.winRate }}%</template>
              </el-table-column>
              <el-table-column label="预计成交" width="110">
                <template #default="{ row }">{{ formatDate(row.expectDealDate) }}</template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <el-tab-pane :label="`合同 (${detail.contracts?.length || 0})`" name="contract">
            <el-table :data="detail.contracts" size="small" border>
              <el-table-column prop="name" label="合同名称" min-width="180" show-overflow-tooltip />
              <el-table-column label="金额" width="120" align="right">
                <template #default="{ row }">{{ formatMoney(row.amount) }}</template>
              </el-table-column>
              <el-table-column label="状态" width="90">
                <template #default="{ row }"><DictTag type="contract_status" :value="row.status" /></template>
              </el-table-column>
              <el-table-column label="回款" width="90">
                <template #default="{ row }"><DictTag type="payment_status" :value="row.paymentStatus" /></template>
              </el-table-column>
              <el-table-column label="签约日期" width="110">
                <template #default="{ row }">{{ formatDate(row.signDate) }}</template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <el-tab-pane label="附件" name="attachment">
            <AttachmentPanel biz-type="CUSTOMER" :biz-id="detail.id" perm-code="crm:customer:edit" :readonly="readOnly" />
          </el-tab-pane>

          <el-tab-pane label="修改记录" name="changeLog">
            <ChangeLogPanel biz-type="CUSTOMER" :biz-id="customerId" show-title />
          </el-tab-pane>

          <el-tab-pane :label="`跟进记录 (${detail.activities?.length || 0})`" name="activity">
            <div class="timeline-wrap">
              <el-timeline v-if="detail.activities?.length">
                <el-timeline-item
                  v-for="item in detail.activities"
                  :key="item.id"
                  :timestamp="formatDateTime(item.createTime)"
                  placement="top"
                  :color="typeColor(item.type)"
                >
                  <div class="timeline-card">
                    <div class="timeline-head">
                      <span class="timeline-type">{{ dictStore.label('activity_type', item.type) }}</span>
                      <span class="timeline-user">{{ item.ownerName || item.createBy }}</span>
                      <el-button
                        v-if="!readOnly"
                        v-perm="'crm:activity:delete'"
                        link
                        type="danger"
                        size="small"
                        class="del-btn"
                        @click="removeActivity(item)"
                      >
                        删除
                      </el-button>
                    </div>
                    <div class="timeline-content">{{ item.content }}</div>
                    <div v-if="item.nextFollowTime" class="timeline-next">
                      下次跟进：{{ formatDateTime(item.nextFollowTime) }}
                    </div>
                  </div>
                </el-timeline-item>
              </el-timeline>
              <el-empty v-else description="暂无跟进记录" :image-size="80" />
            </div>
          </el-tab-pane>
        </el-tabs>
      </template>
    </div>

    <!-- 新增联系人 -->
    <el-dialog v-model="contactVisible" :title="contactForm.id ? '编辑联系人' : '新增联系人'" width="520px" append-to-body>
      <el-form ref="contactRef" :model="contactForm" label-width="80px">
        <el-form-item label="姓名" required>
          <el-input v-model="contactForm.name" />
        </el-form-item>
        <el-form-item label="职务">
          <el-input v-model="contactForm.post" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="contactForm.mobile" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="contactForm.email" />
        </el-form-item>
        <el-form-item label="微信">
          <el-input v-model="contactForm.wechat" />
        </el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="contactForm.gender">
            <el-radio value="MALE">男</el-radio>
            <el-radio value="FEMALE">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="关键联系人">
          <el-switch v-model="contactForm.isPrimary" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="contactVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveContact">保存</el-button>
      </template>
    </el-dialog>

    <!-- 新增跟进 -->
    <el-dialog v-model="activityVisible" title="记录跟进" width="520px" append-to-body>
      <el-form :model="activityForm" label-width="90px">
        <el-form-item label="跟进方式">
          <el-select v-model="activityForm.type" style="width: 100%">
            <el-option v-for="item in dictStore.options('activity_type')" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="跟进内容" required>
          <el-input v-model="activityForm.content" type="textarea" :rows="4" maxlength="1000" show-word-limit />
        </el-form-item>
        <el-form-item label="下次跟进">
          <el-date-picker
            v-model="activityForm.nextFollowTime"
            type="datetime"
            placeholder="选择时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="activityVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveActivity">提交</el-button>
      </template>
    </el-dialog>
  </el-drawer>
</template>

<script setup>
import ChangeLogPanel from '@/components/ChangeLogPanel.vue'
import AttachmentPanel from '@/components/AttachmentPanel.vue'
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import DictTag from '@/components/DictTag.vue'
import { getCustomerDetail } from '@/api/customer'
import { createContact, updateContact } from '@/api/customer'
import { createActivity, deleteActivity } from '@/api/activity'
import { useDictStore } from '@/store/dict'
import { formatDate, formatDateTime, formatMoney, formatWan, initials } from '@/utils/format'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  customerId: { type: [Number, String], default: null }
})
const emit = defineEmits(['update:modelValue', 'edit', 'refresh'])

const dictStore = useDictStore()
const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const readOnly = computed(() => !!detail.readOnly)
const loading = ref(false)
const saving = ref(false)
const tab = ref('info')
const detail = reactive({ customer: null, contacts: [], opportunities: [], contracts: [], activities: [] })

const contactVisible = ref(false)
const contactRef = ref()
const contactForm = reactive({ id: null, customerId: null, name: '', post: '', mobile: '', email: '', wechat: '', gender: 'MALE', isPrimary: 0 })

const activityVisible = ref(false)
const activityForm = reactive({ bizType: 'CUSTOMER', bizId: null, type: 'CALL', content: '', nextFollowTime: '' })

async function load(id) {
  if (!id) return
  loading.value = true
  try {
    const { data } = await getCustomerDetail(id)
    Object.assign(detail, data)
  } finally {
    loading.value = false
  }
}

watch(
  () => [props.modelValue, props.customerId],
  ([open, id]) => {
    if (open && id) {
      tab.value = 'info'
      load(id)
    }
  },
  { immediate: true }
)

function typeColor(type) {
  return { CALL: '#2f6bff', VISIT: '#22b07d', MEETING: '#7b61ff', EMAIL: '#f5a623', WECHAT: '#36c98f' }[type] || '#98a3b3'
}

function openContactForm(row) {
  Object.assign(contactForm, {
    id: null,
    customerId: detail.customer.id,
    name: '',
    post: '',
    mobile: '',
    email: '',
    wechat: '',
    gender: 'MALE',
    isPrimary: 0
  })
  if (row) Object.assign(contactForm, row)
  contactVisible.value = true
}

async function saveContact() {
  if (!contactForm.name) {
    ElMessage.warning('请输入联系人姓名')
    return
  }
  saving.value = true
  try {
    if (contactForm.id) {
      await updateContact(contactForm.id, contactForm)
    } else {
      await createContact(contactForm)
    }
    ElMessage.success('保存成功')
    contactVisible.value = false
    await load(detail.customer.id)
    emit('refresh')
  } finally {
    saving.value = false
  }
}

async function saveActivity() {
  if (!activityForm.content) {
    ElMessage.warning('请填写跟进内容')
    return
  }
  saving.value = true
  try {
    activityForm.bizId = detail.customer.id
    await createActivity(activityForm)
    ElMessage.success('跟进已记录')
    activityVisible.value = false
    activityForm.content = ''
    activityForm.nextFollowTime = ''
    await load(detail.customer.id)
    emit('refresh')
  } finally {
    saving.value = false
  }
}

async function removeActivity(item) {
  await ElMessageBox.confirm('确定删除这条跟进记录吗？', '提示', { type: 'warning' })
  await deleteActivity(item.id)
  ElMessage.success('已删除')
  await load(detail.customer.id)
  emit('refresh')
}
</script>

<style scoped>
.drawer-body {
  min-height: 300px;
}

.profile-head {
  display: flex;
  gap: 14px;
  align-items: flex-start;
  padding-bottom: 16px;
  border-bottom: 1px solid #eef1f6;
}

.company-avatar {
  width: 52px;
  height: 52px;
  border-radius: 12px;
  background: linear-gradient(135deg, #4d8cff, #2f6bff);
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.profile-main {
  flex: 1;
  min-width: 0;
}

.profile-title {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.profile-title h3 {
  margin: 0;
  font-size: 18px;
  color: #16233b;
}

.profile-meta {
  display: flex;
  gap: 18px;
  margin-top: 8px;
  color: #8b97a8;
  font-size: 12px;
  flex-wrap: wrap;
}

.profile-meta span {
  display: flex;
  align-items: center;
  gap: 4px;
}

.profile-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.stat-row {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 10px;
  padding: 16px 0;
}

.stat-item {
  background: #f7f9fd;
  border-radius: 8px;
  padding: 12px;
  text-align: center;
}

.stat-num {
  font-size: 18px;
  font-weight: 700;
  color: #16233b;
}

.stat-label {
  font-size: 11.5px;
  color: #8b97a8;
  margin-top: 3px;
}

.detail-tabs {
  margin-top: 4px;
}

.tab-toolbar {
  margin-bottom: 10px;
  text-align: right;
}

.contact-name {
  font-weight: 500;
}

.timeline-wrap {
  max-height: 460px;
  overflow-y: auto;
  padding: 4px 4px 0 2px;
}

.timeline-card {
  background: #f8fafd;
  border-radius: 8px;
  padding: 10px 12px;
}

.timeline-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 5px;
}

.timeline-type {
  font-size: 11.5px;
  color: #2f6bff;
  background: #eef3ff;
  padding: 1px 6px;
  border-radius: 3px;
}

.timeline-user {
  font-size: 12px;
  color: #6b7789;
}

.del-btn {
  margin-left: auto;
}

.timeline-content {
  font-size: 13px;
  color: #3c4759;
  line-height: 1.6;
}

.timeline-next {
  margin-top: 6px;
  font-size: 11.5px;
  color: #f5a623;
}
.readonly-alert {
  margin-bottom: 14px;
}
</style>
