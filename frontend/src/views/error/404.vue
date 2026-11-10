<template>
  <div class="error-page">
    <div class="code">404</div>
    <div class="title">页面走丢了</div>
    <div class="desc">您访问的页面不存在，或已被移动到其他位置</div>
    <el-button type="primary" @click="$router.push('/dashboard')">返回工作台</el-button>
  </div>
</template>

<script setup>
</script>

<style scoped>
.error-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: #f7f9fc;
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
</style>
