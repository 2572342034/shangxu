<template>
  <div class="error-page">
    <div class="code">403</div>
    <div class="title">抱歉，您没有访问该页面的权限</div>
    <div class="desc">如需访问，请联系系统管理员为您的角色分配相应权限</div>
    <div class="actions">
      <el-button type="primary" @click="$router.push('/dashboard')">返回工作台</el-button>
      <el-button @click="$router.back()">返回上一页</el-button>
    </div>
  </div>
</template>

<script setup>
</script>

<style scoped>
.error-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
}

.code {
  font-size: 90px;
  font-weight: 800;
  background: linear-gradient(135deg, #2f6bff, #7aa9ff);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  line-height: 1;
}

.title {
  font-size: 17px;
  font-weight: 600;
  color: #2c3648;
  margin: 18px 0 8px;
}

.desc {
  font-size: 13px;
  color: #8b97a8;
  margin-bottom: 24px;
}

.actions {
  display: flex;
  gap: 10px;
}
</style>
