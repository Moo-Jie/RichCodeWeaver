<template>
  <div class="admin-back-entry">
    <a-button class="admin-back-btn" @click="goToDashboard">
      <template #icon>
        <ArrowLeftOutlined />
      </template>
      返回系统管理控制台
    </a-button>
  </div>
</template>

<script lang="ts" setup>
import { useRouter } from 'vue-router'
import { ArrowLeftOutlined } from '@ant-design/icons-vue'
import { SYSTEM_MANAGE_PATH } from '@/constants/adminNavItems'

const router = useRouter()

const goToDashboard = () => {
  router.push(SYSTEM_MANAGE_PATH)
}
</script>

<style lang="less" scoped>
.admin-back-entry {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 16px;
}

.admin-back-btn {
  height: 38px;
  padding: 0 16px;
  border-radius: 12px;
  border-color: #d8e4ff;
  color: #000000;
  background: linear-gradient(180deg, #ffffff 0%, #f4f8ff 100%);
  box-shadow: 0 6px 18px rgba(47, 84, 235, 0.08);

  &:hover {
    color: #000000;
    border-color: #656565;
    background: linear-gradient(180deg, #ffffff 0%, #eef4ff 100%);
  }
}
</style>
