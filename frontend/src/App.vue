<template>
  <router-view />
  <ChangeLogDialog
    v-model="logDialog.visible"
    :biz-type="logDialog.bizType"
    :biz-id="logDialog.bizId"
    :title="logDialog.title"
  />
</template>

<script setup>
import ChangeLogDialog from '@/components/ChangeLogDialog.vue'
import { changeLogDialog } from '@/store/changeLog'

const logDialog = changeLogDialog
</script>
