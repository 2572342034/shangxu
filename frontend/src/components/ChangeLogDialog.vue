<template>
  <el-dialog v-model="visible" :title="title" width="900px" append-to-body destroy-on-close>
    <ChangeLogPanel v-if="visible" :biz-type="bizType" :biz-id="bizId" show-title />
    <template #footer>
      <el-button @click="visible = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed } from 'vue'
import ChangeLogPanel from '@/components/ChangeLogPanel.vue'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  bizType: { type: String, required: true },
  bizId: { type: [Number, String], default: null },
  title: { type: String, default: '修改记录' }
})
const emit = defineEmits(['update:modelValue'])

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})
</script>
