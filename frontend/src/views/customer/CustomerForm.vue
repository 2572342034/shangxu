<template>
  <el-dialog
    v-model="visible"
    :title="isEdit ? '编辑客户' : '新建客户'"
    width="720px"
    destroy-on-close
    @closed="reset"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="客户名称" prop="name">
            <el-input v-model="form.name" placeholder="请输入企业全称" maxlength="128" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="客户简称">
            <el-input v-model="form.shortName" placeholder="选填" maxlength="64" />
          </el-form-item>
        </el-col>

        <el-col :span="12">
          <el-form-item label="客户级别" prop="level">
            <el-radio-group v-model="form.level">
              <el-radio-button value="A">A 类</el-radio-button>
              <el-radio-button value="B">B 类</el-radio-button>
              <el-radio-button value="C">C 类</el-radio-button>
            </el-radio-group>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="所属行业">
            <el-select v-model="form.industry" placeholder="请选择行业" clearable style="width: 100%">
              <el-option v-for="item in dictStore.options('industry')" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
        </el-col>

        <el-col :span="12">
          <el-form-item label="客户来源">
            <el-select v-model="form.source" placeholder="请选择来源" clearable style="width: 100%">
              <el-option v-for="item in dictStore.options('customer_source')" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="人员规模">
            <el-select v-model="form.scale" placeholder="请选择规模" clearable style="width: 100%">
              <el-option v-for="item in dictStore.options('customer_scale')" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
        </el-col>

        <el-col :span="12">
          <el-form-item label="联系电话">
            <el-input v-model="form.phone" placeholder="座机或手机号" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="官网">
            <el-input v-model="form.website" placeholder="如 www.example.com" />
          </el-form-item>
        </el-col>

        <el-col :span="12">
          <el-form-item label="省份">
            <el-input v-model="form.province" placeholder="如 广东省" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="城市">
            <el-input v-model="form.city" placeholder="如 广州市" />
          </el-form-item>
        </el-col>

        <el-col :span="24">
          <el-form-item label="详细地址">
            <el-input v-model="form.address" placeholder="街道、门牌号" />
          </el-form-item>
        </el-col>

        <el-col :span="12">
          <el-form-item label="负责人">
            <el-select v-model="form.ownerUserId" placeholder="默认当前用户" clearable filterable style="width: 100%">
              <el-option v-for="user in users" :key="user.id" :label="`${user.nickname}（${user.deptName || '-'}）`" :value="user.id" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="客户状态">
            <el-select v-model="form.status" style="width: 100%">
              <el-option label="跟进中" value="FOLLOWING" />
              <el-option label="已成交" value="DEAL" />
              <el-option label="无效" value="INVALID" />
            </el-select>
          </el-form-item>
        </el-col>

        <el-col :span="24">
          <el-form-item label="备注">
            <el-input v-model="form.remark" type="textarea" :rows="3" maxlength="500" show-word-limit />
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { createCustomer, updateCustomer } from '@/api/customer'
import { userOptions } from '@/api/system'
import { useDictStore } from '@/store/dict'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  data: { type: Object, default: null }
})
const emit = defineEmits(['update:modelValue', 'success'])

const dictStore = useDictStore()
const formRef = ref()
const saving = ref(false)
const users = ref([])

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const isEdit = computed(() => Boolean(props.data?.id))

const defaultForm = () => ({
  id: null,
  name: '',
  shortName: '',
  level: 'C',
  industry: '',
  source: '',
  scale: '',
  phone: '',
  website: '',
  province: '',
  city: '',
  address: '',
  ownerUserId: null,
  status: 'FOLLOWING',
  remark: ''
})

const form = reactive(defaultForm())

const rules = {
  name: [{ required: true, message: '请输入客户名称', trigger: 'blur' }],
  level: [{ required: true, message: '请选择客户级别', trigger: 'change' }]
}

watch(
  () => props.modelValue,
  async (val) => {
    if (!val) return
    Object.assign(form, defaultForm())
    if (props.data) {
      Object.assign(form, props.data)
    }
    if (!users.value.length) {
      const { data } = await userOptions()
      users.value = data || []
    }
  }
)

function reset() {
  formRef.value?.clearValidate()
  Object.assign(form, defaultForm())
}

async function submit() {
  await formRef.value.validate()
  saving.value = true
  try {
    if (isEdit.value) {
      await updateCustomer(form.id, form)
      ElMessage.success('客户信息已更新')
    } else {
      await createCustomer(form)
      ElMessage.success('客户创建成功')
    }
    visible.value = false
    emit('success')
  } finally {
    saving.value = false
  }
}
</script>
