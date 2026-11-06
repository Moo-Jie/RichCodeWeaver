<template>
  <div class="main-layout">
    <TheSidebar />
    <div class="main-area">
      <router-view />
    </div>
  </div>
</template>

<script lang="ts" setup>
import TheSidebar from '@/components/sidebar/TheSidebar.vue'
</script>

<style scoped>
.main-layout {
  display: flex;
  height: 100vh;
  width: 100vw;
  overflow: hidden;
  background: #fff;
}

.main-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: auto;
  min-width: 0;
}
</style>
