<template>
  <el-tag v-if="value !== null && value !== undefined && value !== ''" :type="tagType" effect="light" size="small" round>
    {{ label }}
  </el-tag>
  <span v-else class="text-muted">-</span>
</template>

<script setup>
import { computed } from 'vue'
import { useDictStore } from '@/store/dict'

const props = defineProps({
  type: { type: String, required: true },
  value: { type: [String, Number], default: '' }
})

const dictStore = useDictStore()

const COLOR_MAP = {
  'customer_status:FOLLOWING': 'primary',
  'customer_status:DEAL': 'success',
  'customer_status:INVALID': 'info',
  'customer_level:A': 'danger',
  'customer_level:B': 'warning',
  'customer_level:C': 'info',
  'lead_status:NEW': 'info',
  'lead_status:FOLLOWING': 'primary',
  'lead_status:QUALIFIED': 'warning',
  'lead_status:CONVERTED': 'success',
  'lead_status:INVALID': 'info',
  'opp_stage:CONTACT': 'info',
  'opp_stage:DEMAND': 'primary',
  'opp_stage:QUOTE': 'warning',
  'opp_stage:NEGOTIATION': 'danger',
  'opp_stage:WON': 'success',
  'opp_stage:LOST': 'info',
  'contract_status:DRAFT': 'info',
  'contract_status:PENDING': 'warning',
  'contract_status:APPROVED': 'primary',
  'contract_status:REJECTED': 'danger',
  'contract_status:SIGNED': 'success',
  'contract_status:VOID': 'info',
  'payment_status:UNPAID': 'warning',
  'payment_status:PART': 'primary',
  'payment_status:PAID': 'success',
  'pool_type:PUBLIC': 'warning',
  'pool_type:PRIVATE': 'success',
  'order_status:DRAFT': 'info',
  'order_status:PENDING': 'warning',
  'order_status:APPROVED': 'primary',
  'order_status:REJECTED': 'danger',
  'order_status:CANCELLED': 'info',
  'delivery_status:DRAFT': 'warning',
  'delivery_status:SHIPPED': 'primary',
  'delivery_status:SIGNED': 'success',
  'delivery_status:CANCELLED': 'info',
  'delivery_progress:UNDELIVERED': 'info',
  'delivery_progress:PART': 'warning',
  'delivery_progress:DELIVERED': 'success',
  'receivable_status:UNPAID': 'warning',
  'receivable_status:PART': 'primary',
  'receivable_status:PAID': 'success',
  'plan_status:PENDING': 'warning',
  'plan_status:SHIPPED': 'success',
  'plan_status:CANCELLED': 'info',
  'invoice_status:PENDING': 'warning',
  'invoice_status:ISSUED': 'success',
  'invoice_status:VOID': 'info',
  'return_status:PENDING': 'warning',
  'return_status:APPROVED': 'primary',
  'return_status:REJECTED': 'danger',
  'return_status:REFUNDED': 'success'
}

const label = computed(() => dictStore.label(props.type, props.value))
const tagType = computed(() => COLOR_MAP[`${props.type}:${props.value}`] || 'info')
</script>
