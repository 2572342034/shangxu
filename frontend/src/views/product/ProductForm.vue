<template>
  <el-dialog
    :model-value="modelValue"
    :title="form.id ? '编辑产品' : '新增产品'"
    width="700px"
    destroy-on-close
    @update:model-value="(v) => emit('update:modelValue', v)"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
      <el-row :gutter="14">
        <el-col :span="12">
          <el-form-item label="产品编码">
            <el-input v-model="form.code" placeholder="留空自动生成 PRD + 序号" :disabled="!!form.id" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="产品名称" prop="name">
            <el-input v-model="form.name" placeholder="请输入产品名称" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="所属目录">
            <el-select v-model="form.categoryId" placeholder="选择产品目录" style="width: 100%" clearable>
              <el-option-group v-for="parent in groups" :key="parent.id" :label="parent.name">
                <el-option :label="`${parent.name}（本级）`" :value="parent.id" />
                <el-option
                  v-for="child in parent.children || []"
                  :key="child.id"
                  :label="`　└ ${child.name}`"
                  :value="child.id"
                />
              </el-option-group>
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="规格型号">
            <el-input v-model="form.spec" placeholder="如 50 账号 / 年、人天" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="单位">
            <el-input v-model="form.unit" placeholder="套/年、人天、次" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="销售价" prop="price">
            <el-input-number v-model="form.price" :min="0" :precision="2" :step="1000" style="width: 100%" controls-position="right" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="成本价">
            <el-input-number v-model="form.cost" :min="0" :precision="2" :step="1000" style="width: 100%" controls-position="right" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="最低成交价">
            <el-input-number v-model="form.minPrice" :min="0" :precision="2" :step="1000" style="width: 100%" controls-position="right" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="默认税率 (%)">
            <el-input-number v-model="form.taxRate" :min="0" :max="100" :precision="2" style="width: 100%" controls-position="right" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="排序">
            <el-input-number v-model="form.sort" :min="0" :step="1" style="width: 100%" controls-position="right" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="销售状态">
            <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="在售" inactive-text="停售" />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="产品描述">
            <el-input v-model="form.description" type="textarea" :rows="3" maxlength="1000" show-word-limit placeholder="面向客户的产品说明、适用范围与包含内容" />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="备注">
            <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="255" show-word-limit />
          </el-form-item>
        </el-col>
      </el-row>

      <div class="gross-hint">
        预估毛利率
        <b :class="grossRate >= 50 ? 'good' : 'warn'">{{ grossRate }}%</b>
        <span class="text-muted">（销售价 {{ formatMoney(form.price) }} − 成本价 {{ formatMoney(form.cost) }}）</span>
      </div>
    </el-form>

    <template #footer>
      <el-button @click="emit('update:modelValue', false)">取消</el-button>
      <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { createProduct, updateProduct } from '@/api/product'
import { formatMoney } from '@/utils/format'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  product: { type: Object, default: null },
  groups: { type: Array, default: () => [] },
  defaultCategoryId: { type: [Number, String], default: null }
})
const emit = defineEmits(['update:modelValue', 'saved'])

const formRef = ref()
const saving = ref(false)

const form = reactive({
  id: null,
  code: '',
  name: '',
  categoryId: null,
  spec: '',
  unit: '',
  price: 0,
  cost: 0,
  minPrice: null,
  taxRate: 6,
  sort: 0,
  status: 1,
  description: '',
  remark: ''
})

const rules = {
  name: [{ required: true, message: '请输入产品名称', trigger: 'blur' }],
  price: [{ required: true, message: '请输入销售价', trigger: 'blur' }]
}

const grossRate = computed(() => {
  const price = Number(form.price || 0)
  if (!price) return 0
  return Math.round(((price - Number(form.cost || 0)) / price) * 100)
})

watch(
  () => props.modelValue,
  (visible) => {
    if (!visible) return
    Object.assign(form, {
      id: null,
      code: '',
      name: '',
      categoryId: props.defaultCategoryId ? Number(props.defaultCategoryId) : null,
      spec: '',
      unit: '',
      price: 0,
      cost: 0,
      minPrice: null,
      taxRate: 6,
      sort: 0,
      status: 1,
      description: '',
      remark: ''
    })
    if (props.product) {
      Object.assign(form, props.product)
    }
    formRef.value?.clearValidate()
  }
)

async function submit() {
  await formRef.value.validate()
  if (form.minPrice && Number(form.minPrice) > Number(form.price || 0)) {
    ElMessage.warning('最低成交价不能高于销售价')
    return
  }
  saving.value = true
  try {
    const payload = { ...form }
    if (form.id) {
      await updateProduct(form.id, payload)
    } else {
      await createProduct(payload)
    }
    ElMessage.success(form.id ? '产品已更新' : '产品已创建')
    emit('update:modelValue', false)
    emit('saved')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.gross-hint {
  margin: 4px 0 0 96px;
  font-size: 12.5px;
  color: #5b6779;
  background: #f7f9fc;
  border-radius: 8px;
  padding: 8px 12px;
}
.gross-hint b {
  margin: 0 6px;
  font-size: 15px;
}
.gross-hint b.good {
  color: #22b07d;
}
.gross-hint b.warn {
  color: #f5a623;
}
</style>
