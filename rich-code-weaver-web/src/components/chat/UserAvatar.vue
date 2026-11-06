<template>
  <span
    class="user-avatar"
    :style="avatarStyle"
    :aria-label="name ? `${name}的头像` : '用户头像'"
    role="img"
  >
    <img
      v-if="normalizedSrc && !imageFailed"
      :src="normalizedSrc"
      :alt="name || '用户头像'"
      loading="lazy"
      @error="imageFailed = true"
    />
    <span v-else class="avatar-fallback">{{ initial }}</span>
  </span>
</template>

<script lang="ts" setup>
import { computed, ref, watch } from 'vue'

const props = withDefaults(
  defineProps<{
    src?: string
    name?: string
    size?: number
  }>(),
  {
    src: '',
    name: '',
    size: 36,
  },
)

const imageFailed = ref(false)
const normalizedSrc = computed(() => props.src?.trim() || '')
const initial = computed(() => props.name?.trim()?.charAt(0)?.toUpperCase() || 'U')
const avatarStyle = computed(() => ({
  width: `${props.size}px`,
  height: `${props.size}px`,
  minWidth: `${props.size}px`,
  fontSize: `${Math.max(12, Math.round(props.size * 0.34))}px`,
}))

watch(
  () => props.src,
  () => {
    imageFailed.value = false
  },
)
</script>

<style scoped>
.user-avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  overflow: hidden;
  border-radius: 50%;
  background: #f2f2f2;
  color: #666;
  font-weight: 600;
  line-height: 1;
  user-select: none;
}

.user-avatar img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-fallback {
  transform: translateY(0.5px);
}
</style>
