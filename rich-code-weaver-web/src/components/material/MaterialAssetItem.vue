<template>
  <article
    class="material-asset"
    :class="[`is-${materialType}`, { 'is-featured': featured }]"
    @click="handleOpen"
  >
    <template v-if="materialType === 'image'">
      <div class="material-asset-media">
        <img
          :src="item.thumbnailUrl || item.content"
          :alt="item.materialName || '图片素材'"
          loading="lazy"
        />
        <MaterialActions :item="item" @edit="emit('edit', item)" @delete="emit('delete', item)" />
      </div>
      <div class="material-asset-caption">
        <strong :title="item.materialName">{{ item.materialName || '未命名图片' }}</strong>
        <span>{{ assetMeta }}</span>
      </div>
    </template>

    <template v-else-if="materialType === 'video'">
      <div class="material-asset-media is-video">
        <video
          :src="item.content"
          :poster="item.thumbnailUrl || undefined"
          controls
          muted
          playsinline
          preload="metadata"
          @click.stop
        ></video>
        <MaterialActions :item="item" @edit="emit('edit', item)" @delete="emit('delete', item)" />
      </div>
      <div class="material-asset-caption">
        <strong :title="item.materialName">{{ item.materialName || '未命名视频' }}</strong>
        <span>{{ assetMeta }}</span>
      </div>
    </template>

    <template v-else-if="materialType === 'audio'">
      <div class="material-audio-head">
        <span class="material-type-mark"><SoundOutlined /></span>
        <div class="material-audio-copy">
          <strong :title="item.materialName">{{ item.materialName || '未命名音频' }}</strong>
          <span>{{ assetMeta }}</span>
        </div>
        <MaterialActions :item="item" @edit="emit('edit', item)" @delete="emit('delete', item)" />
      </div>
      <audio :src="item.content" controls preload="none" @click.stop></audio>
    </template>

    <template v-else-if="materialType === 'link'">
      <a
        class="material-link-main"
        :href="item.content"
        target="_blank"
        rel="noopener noreferrer"
        @click.stop
      >
        <span class="material-type-mark"><LinkOutlined /></span>
        <span class="material-link-copy">
          <strong :title="item.materialName">{{ item.materialName || linkHost }}</strong>
          <span>{{ linkHost }}</span>
          <small>{{ item.description || item.content }}</small>
        </span>
        <ExportOutlined class="material-link-arrow" />
      </a>
      <MaterialActions :item="item" @edit="emit('edit', item)" @delete="emit('delete', item)" />
    </template>

    <template v-else>
      <div class="material-text-head">
        <span class="material-type-mark"><FileTextOutlined /></span>
        <div class="material-audio-copy">
          <strong :title="item.materialName">{{ item.materialName || '未命名文本' }}</strong>
          <span>{{ assetMeta }} · {{ contentLength }} 字符</span>
        </div>
        <MaterialActions :item="item" @edit="emit('edit', item)" @delete="emit('delete', item)" />
      </div>
      <p class="material-text-preview">{{ item.content?.slice(0, 180) || '暂无内容' }}</p>
    </template>
  </article>
</template>

<script lang="ts" setup>
import { computed } from 'vue'
import {
  ExportOutlined,
  FileTextOutlined,
  LinkOutlined,
  SoundOutlined
} from '@ant-design/icons-vue'
import MaterialActions from './MaterialActions.vue'

interface Props {
  item: API.MaterialVO
  featured?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  featured: false
})

const emit = defineEmits<{
  open: [item: API.MaterialVO]
  edit: [item: API.MaterialVO]
  delete: [item: API.MaterialVO]
}>()

const materialType = computed(() => props.item.materialType || 'text')
const contentLength = computed(() => props.item.content?.length || 0)
const linkHost = computed(() => {
  const value = props.item.content || ''
  try {
    return new URL(value).hostname.replace(/^www\./, '')
  } catch {
    return value.replace(/^https?:\/\//, '').split('/')[0] || '外部链接'
  }
})
const assetMeta = computed(() => {
  const parts = [props.item.categoryName, formatFileSize(props.item.fileSize), `使用 ${props.item.useCount || 0} 次`]
  return parts.filter(Boolean).join(' · ')
})

const formatFileSize = (size?: number) => {
  if (!size || size <= 0) return ''
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / 1024 / 1024).toFixed(1)} MB`
}

const handleOpen = () => {
  if (materialType.value !== 'link') emit('open', props.item)
}
</script>

<style scoped>
.material-asset {
  min-width: 0;
  padding: 10px;
  border: 1px solid #ececec;
  border-radius: 10px;
  background: #fff;
  cursor: pointer;
  transition: border-color 160ms ease, background 160ms ease;
}

.material-asset:hover {
  border-color: #d6d6d6;
  background: #fcfcfc;
}

.material-asset.is-image,
.material-asset.is-video {
  padding: 0;
  overflow: hidden;
}

.material-asset.is-image.is-featured,
.material-asset.is-video.is-featured {
  grid-column: span 6;
}

.material-asset.is-audio,
.material-asset.is-link {
  grid-column: span 6;
}

.material-asset.is-link {
  cursor: default;
}

.material-asset.is-text {
  grid-column: span 4;
}

.material-asset-media {
  position: relative;
  aspect-ratio: 4 / 3;
  overflow: hidden;
  background: #f5f5f5;
}

.material-asset.is-image.is-featured .material-asset-media {
  aspect-ratio: 16 / 9;
}

.material-asset-media img,
.material-asset-media video {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.material-asset-media.is-video {
  aspect-ratio: 16 / 9;
  background: #111;
}

.material-asset-media.is-video video {
  object-fit: contain;
}

.material-asset-caption {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 11px 12px 12px;
}

.material-asset-caption strong,
.material-audio-copy strong,
.material-link-copy strong {
  min-width: 0;
  overflow: hidden;
  font-size: 13px;
  font-weight: 600;
  line-height: 1.4;
  color: #1a1a1a;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.material-asset-caption span,
.material-audio-copy span,
.material-link-copy span {
  flex-shrink: 0;
  font-size: 11px;
  color: #999;
}

.material-audio-head,
.material-text-head {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.material-type-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  border-radius: 8px;
  font-size: 15px;
  color: #555;
  background: #f5f5f5;
}

.material-audio-copy,
.material-link-copy {
  display: flex;
  min-width: 0;
  flex: 1;
  flex-direction: column;
  gap: 3px;
}

.material-asset audio {
  width: 100%;
  height: 38px;
  margin-top: 10px;
  filter: grayscale(1);
}

.material-link-main {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  padding-right: 34px;
  color: inherit;
  text-decoration: none;
}

.material-link-copy small {
  overflow: hidden;
  font-size: 11px;
  line-height: 1.5;
  color: #999;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.material-link-arrow {
  flex-shrink: 0;
  color: #999;
}

.material-text-head {
  padding-right: 34px;
}

.material-text-preview {
  display: -webkit-box;
  overflow: hidden;
  margin: 12px 0 2px;
  color: #666;
  font-size: 12px;
  line-height: 1.7;
  white-space: pre-wrap;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 5;
}

.material-asset :deep(.material-actions) {
  flex-shrink: 0;
}

.material-asset.is-image :deep(.material-actions),
.material-asset.is-video :deep(.material-actions) {
  position: absolute;
  top: 8px;
  right: 8px;
  opacity: 1;
  z-index: 2;
}

.material-asset.is-video :deep(.material-actions) {
  top: 8px;
  right: 8px;
}

.material-asset.is-video :deep(.material-actions button),
.material-asset.is-image :deep(.material-actions button) {
  background: rgba(255, 255, 255, 0.92);
}

.material-asset.is-audio,
.material-asset.is-link,
.material-asset.is-text {
  position: relative;
}

.material-asset.is-audio > :deep(.material-actions),
.material-asset.is-link > :deep(.material-actions),
.material-asset.is-text > :deep(.material-actions) {
  position: absolute;
  top: 10px;
  right: 10px;
}

.material-asset.is-link :deep(.material-actions) {
  top: 10px;
}

@media (max-width: 1080px) {
  .material-asset.is-image.is-featured,
  .material-asset.is-video.is-featured,
  .material-asset.is-audio,
  .material-asset.is-link {
    grid-column: span 12;
  }

  .material-asset.is-text {
    grid-column: span 6;
  }
}

@media (max-width: 640px) {
  .material-asset,
  .material-asset.is-image.is-featured,
  .material-asset.is-video.is-featured,
  .material-asset.is-audio,
  .material-asset.is-link,
  .material-asset.is-text {
    grid-column: span 12;
  }

  .material-asset-caption {
    flex-direction: column;
    gap: 4px;
  }
}
</style>
