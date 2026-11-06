<template>
  <div class="main-layout">
    <TheSidebar />
    <div ref="mainAreaRef" class="main-area">
      <router-view />
    </div>
  </div>
</template>

<script lang="ts" setup>
import { nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import TheSidebar from '@/components/sidebar/TheSidebar.vue'

const route = useRoute()
const mainAreaRef = ref<HTMLElement | null>(null)
const scrollPositions = new Map<string, number>()

watch(
  () => route.fullPath,
  async (_to, from) => {
    const container = mainAreaRef.value
    if (!container) return
    scrollPositions.set(from, container.scrollTop)
    await nextTick()
    container.scrollTop = scrollPositions.get(route.fullPath) ?? 0
  },
)

onBeforeUnmount(() => {
  scrollPositions.clear()
})
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
