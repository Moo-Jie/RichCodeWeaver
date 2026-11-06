<template>
  <article class="app-library-card" :class="[`is-${variant}`, { 'is-hero': variant === 'hero' }]">
    <button
      type="button"
      class="app-library-cover"
      :aria-label="`打开 ${app.appName || '未命名数字产物'}`"
      @click="emit('open', app)"
    >
      <img :src="app.cover || fallbackCover" :alt="app.appName || '数字产物封面'" loading="lazy" decoding="async" />
      <span v-if="rank" class="app-library-rank">#{{ String(rank).padStart(2, '0') }}</span>
      <span v-if="status" class="app-library-status">
        <span class="app-library-status-dot" :class="{ 'is-muted': status !== '已部署' }"></span>
        {{ status }}
      </span>
      <span class="app-library-open-hint">
        打开
        <ArrowRightOutlined />
      </span>
    </button>

    <div class="app-library-body">
      <div class="app-library-title-row">
        <div class="app-library-title-copy">
          <h3 :title="app.appName">{{ app.appName || '未命名数字产物' }}</h3>
          <div class="app-library-meta">
            <span>{{ typeLabel }}</span>
            <template v-for="item in meta" :key="item">
              <span class="app-library-separator">·</span>
              <span>{{ item }}</span>
            </template>
          </div>
        </div>
        <div v-if="$slots.actions" class="app-library-actions" @click.stop>
          <slot name="actions"></slot>
        </div>
      </div>

      <div v-if="stats?.length" class="app-library-stats">
        <span v-for="stat in stats" :key="stat.label">
          <span>{{ stat.label }}</span>
          <strong>{{ stat.value }}</strong>
        </span>
      </div>
    </div>
  </article>
</template>

<script lang="ts" setup>
import { computed } from 'vue'
import { ArrowRightOutlined } from '@ant-design/icons-vue'
import fallbackCover from '@/assets/logo.png'

export interface AppLibraryStat {
  label: string
  value: string | number
}

interface Props {
  app: API.AppVO
  variant?: 'hero' | 'grid'
  rank?: number
  status?: string
  meta?: string[]
  stats?: AppLibraryStat[]
}

const props = withDefaults(defineProps<Props>(), {
  variant: 'grid',
  rank: undefined,
  status: '',
  meta: () => [],
  stats: () => []
})

const emit = defineEmits<{
  open: [app: API.AppVO]
}>()

const typeLabel = computed(() => {
  const labels: Record<string, string> = {
    single_html: '单文件',
    html: '单文件',
    multi_file: '多文件',
    vue_project: 'Vue 项目',
    VUE_PROJECT: 'Vue 项目',
    HTML: '单文件',
    MULTI_FILE: '多文件'
  }
  return labels[props.app.codeGenType || ''] || '数字产物'
})
</script>

<style scoped>
.app-library-card {
  min-width: 0;
}

.app-library-card.is-hero {
  grid-column: span 2;
}

.app-library-cover {
  position: relative;
  display: block;
  width: 100%;
  aspect-ratio: 4 / 3;
  padding: 0;
  overflow: hidden;
  border: 1px solid #ececec;
  border-radius: 10px;
  background: #f5f5f5;
  cursor: pointer;
}

.is-hero .app-library-cover {
  aspect-ratio: 16 / 8.8;
}

.app-library-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 220ms ease, opacity 220ms ease;
}

.app-library-cover:hover img {
  transform: scale(1.018);
}

.app-library-cover:focus-visible {
  outline: 2px solid #1a1a1a;
  outline-offset: 3px;
}

.app-library-rank {
  position: absolute;
  top: 10px;
  left: 10px;
  padding: 4px 7px;
  border-radius: 5px;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.04em;
  color: #fff;
  background: rgba(18, 18, 18, 0.88);
  backdrop-filter: blur(8px);
}

.app-library-status {
  position: absolute;
  left: 10px;
  bottom: 10px;
  display: inline-flex;
  align-items: center;
  padding: 4px 8px;
  border: 1px solid rgba(255, 255, 255, 0.4);
  border-radius: 5px;
  font-size: 11px;
  color: #333;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(8px);
}

.app-library-status-dot {
  width: 5px;
  height: 5px;
  margin-right: 5px;
  border-radius: 50%;
  background: #1a1a1a;
}

.app-library-status-dot.is-muted {
  background: #aaa;
}

.app-library-open-hint {
  position: absolute;
  right: 10px;
  bottom: 10px;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 4px 7px;
  border-radius: 5px;
  font-size: 11px;
  color: #fff;
  background: rgba(18, 18, 18, 0.78);
  opacity: 0;
  transform: translateY(3px);
  transition: opacity 160ms ease, transform 160ms ease;
}

.app-library-cover:hover .app-library-open-hint,
.app-library-cover:focus-visible .app-library-open-hint {
  opacity: 1;
  transform: translateY(0);
}

.app-library-body {
  padding: 11px 2px 0;
}

.app-library-title-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  min-width: 0;
}

.app-library-title-copy {
  min-width: 0;
  flex: 1;
}

.app-library-title-copy h3 {
  margin: 0;
  overflow: hidden;
  font-size: 15px;
  font-weight: 600;
  line-height: 1.35;
  letter-spacing: -0.01em;
  color: #1a1a1a;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.is-hero .app-library-title-copy h3 {
  font-size: 18px;
}

.app-library-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 5px;
  margin-top: 5px;
  font-size: 11px;
  line-height: 1.45;
  color: #999;
}

.app-library-separator {
  color: #d0d0d0;
}

.app-library-actions {
  display: flex;
  align-items: center;
  flex-shrink: 0;
  gap: 2px;
}

.app-library-stats {
  display: flex;
  flex-wrap: wrap;
  gap: 13px;
  margin-top: 8px;
  font-size: 11px;
  color: #999;
}

.app-library-stats strong {
  margin-left: 3px;
  font-weight: 600;
  color: #555;
}

@media (max-width: 860px) {
  .app-library-card.is-hero {
    grid-column: span 2;
  }

  .is-hero .app-library-cover {
    aspect-ratio: 16 / 9;
  }
}

@media (max-width: 560px) {
  .app-library-card.is-hero {
    grid-column: span 1;
  }

  .app-library-open-hint {
    opacity: 1;
    transform: none;
  }
}
</style>
