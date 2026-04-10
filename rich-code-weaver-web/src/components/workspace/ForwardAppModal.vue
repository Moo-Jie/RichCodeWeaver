<template>
  <a-modal
    v-model:open="visible"
    :width="760"
    :footer="null"
    :bodyStyle="{ padding: '0' }"
    @cancel="handleClose"
  >
    <div class="forward-modal-shell">
      <div class="forward-mode-switch">
        <button
          :class="['mode-switch-btn', { 'mode-switch-btn--active': activeMode === 'friends' }]"
          @click="activeMode = 'friends'"
        >
          <TeamOutlined />
          <span>好友转发</span>
        </button>
        <button
          :class="['mode-switch-btn', { 'mode-switch-btn--active': activeMode === 'poster' }]"
          @click="activeMode = 'poster'"
        >
          <QrcodeOutlined />
          <span>分享物料</span>
        </button>
      </div>

      <div v-if="activeMode === 'friends'" class="forward-panel">
        <div class="forward-panel-header">
          <div>
            <div class="panel-title">选择要转发的好友</div>
            <div class="panel-desc">支持多选，一次将产物卡片发送给多位好友。</div>
          </div>
          <div class="selected-pill">已选 {{ selectedFriendIds.length }} 位</div>
        </div>

        <div v-if="chatStore.friends.length === 0" class="empty-hint">
          <TeamOutlined class="empty-icon" />
          <p>暂无可转发的好友</p>
        </div>
        <div v-else class="friend-select-list">
          <div
            v-for="friend in chatStore.friends"
            :key="friend.friendId"
            :class="['friend-select-item', { 'friend-select-item--active': isSelected(friend.friendId) }]"
            @click="toggleFriend(friend.friendId)"
          >
            <a-avatar :size="42" :src="friend.friendAvatar">
              {{ friend.friendName?.charAt(0) || 'U' }}
            </a-avatar>
            <div class="friend-select-info">
              <span class="friend-select-name">{{ friend.friendName }}</span>
              <span class="friend-select-meta">点击即可快速加入本次转发列表</span>
            </div>
            <div class="friend-select-check">
              <CheckCircleFilled v-if="isSelected(friend.friendId)" />
              <PlusOutlined v-else />
            </div>
          </div>
        </div>
      </div>

      <div v-else class="forward-panel forward-panel--poster">
        <div class="poster-workbench-card">
          <div class="poster-preview-card">
            <div class="poster-preview-head">
              <span class="poster-preview-title">海报预览</span>
            </div>
            <div class="poster-preview-stage">
              <div class="poster-preview-placeholder">
                <div class="poster-preview-placeholder-card">
                  <div class="poster-preview-placeholder-cover-wrap">
                    <img
                      v-if="displayCoverUrl"
                      ref="posterCoverPreviewRef"
                      :src="displayCoverUrl"
                      :alt="displayAppName"
                      class="poster-preview-placeholder-cover"
                    />
                    <div v-else class="poster-preview-placeholder-cover poster-preview-placeholder-cover--empty">
                      <img alt="数字产物封面" class="poster-preview-placeholder-logo" src="@/assets/logo.png" />
                    </div>
                  </div>
                  <div class="poster-preview-placeholder-body">
                    <div class="poster-preview-placeholder-name">{{ displayAppName }}</div>
                    <div class="poster-preview-placeholder-type">{{ displayTypeLabel }}</div>
                  </div>
                  <div class="poster-preview-placeholder-divider" />
                  <div class="poster-preview-placeholder-footer">
                    <div class="poster-preview-placeholder-copy">
                      <div class="poster-preview-placeholder-copy-main">扫码查看产物</div>
                      <div class="poster-preview-placeholder-copy-sub">RichCodeWeaver · 织码睿奇</div>
                    </div>
                    <div class="poster-preview-placeholder-qr">
                      <a-qrcode
                        :size="80"
                        :value="shareLink || 'https://rich-code-weaver.local/share'"
                        bg-color="#ffffff"
                        color="#1a1a1a"
                      />
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div class="poster-action-card">
            <div class="poster-action-buttons">
              <a-button class="share-link-ghost-btn" block :loading="copyLoading" @click="handleCopyShare">
                <CopyOutlined />
                复制分享连接
              </a-button>
              <a-button class="share-link-copy-btn" type="primary" block :loading="posterGenerating" @click="handleGeneratePoster">
                <DownloadOutlined />
                下载海报图片
              </a-button>
            </div>
          </div>
        </div>

        <div v-if="shareLink" class="poster-qrcode-source">
          <a-qrcode
            ref="posterQrCodeRef"
            :size="240"
            :value="shareLink"
            bg-color="#ffffff"
            color="#1a1a1a"
            type="canvas"
          />
        </div>
      </div>

      <div class="modal-actions">
        <a-button class="modal-cancel-btn" @click="handleClose">取消</a-button>
        <a-button class="modal-confirm-btn" v-if="activeMode === 'friends'" type="primary" :loading="submitting" @click="handleConfirm">
          转发给好友
        </a-button>
      </div>
    </div>
  </a-modal>
</template>

<script lang="ts" setup>
import { computed, nextTick, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { API_BASE_URL } from '@/config/env'
import {
  CheckCircleFilled,
  CopyOutlined,
  DownloadOutlined,
  PlusOutlined,
  QrcodeOutlined,
  TeamOutlined
} from '@ant-design/icons-vue'
import { useChatStore } from '@/stores/chatStore'

interface Props {
  open: boolean
  app?: API.AppVO | null
  submitting?: boolean
  copyLoading?: boolean
  shareLink?: string
}

const props = withDefaults(defineProps<Props>(), {
  app: null,
  submitting: false,
  copyLoading: false,
  shareLink: ''
})

const emit = defineEmits<{
  (e: 'update:open', value: boolean): void
  (e: 'confirm', friendIds: number[]): void
  (e: 'copy-share'): void
  (e: 'poster-generated'): void
}>()

const chatStore = useChatStore()
const selectedFriendIds = ref<number[]>([])
const activeMode = ref<'friends' | 'copy' | 'poster'>('friends')
const posterGenerating = ref(false)
const posterPreviewUrl = ref('')
const posterCoverPreviewRef = ref<HTMLImageElement | null>(null)
const posterQrCodeRef = ref<{ toDataURL?: (type?: string, quality?: number) => string | undefined } | null>(null)

const visible = computed({
  get: () => props.open,
  set: (val: boolean) => emit('update:open', val)
})

const displayAppName = computed(() => props.app?.appName?.trim() || '未命名数字产物')

const displayAuthorName = computed(() => props.app?.user?.userName || '平台创作者')

const displayTypeLabel = computed(() => {
  const t = props.app?.codeGenType
  if (t === 'single_html') return '单文件结构'
  if (t === 'multi_file') return '多文件结构'
  if (t === 'vue_project') return 'Vue 项目工程'
  return '数字产物'
})

const displayCoverUrl = computed(() => normalizeAssetUrl(props.app?.cover))

watch(
  () => props.open,
  async (val) => {
    if (val) {
      activeMode.value = 'friends'
      selectedFriendIds.value = []
      posterPreviewUrl.value = ''
      await chatStore.fetchFriends()
      return
    }
    posterPreviewUrl.value = ''
  }
)

function isSelected(friendId?: number) {
  return !!friendId && selectedFriendIds.value.includes(friendId)
}

function toggleFriend(friendId?: number) {
  if (!friendId) return
  if (selectedFriendIds.value.includes(friendId)) {
    selectedFriendIds.value = selectedFriendIds.value.filter((id) => id !== friendId)
    return
  }
  selectedFriendIds.value = [...selectedFriendIds.value, friendId]
}

function handleConfirm() {
  if (!props.app?.id) {
    message.warning('当前产物信息不存在')
    return
  }
  if (selectedFriendIds.value.length === 0) {
    message.warning('请至少选择一位好友')
    return
  }
  emit('confirm', selectedFriendIds.value)
}

function handleCopyShare() {
  if (!props.app?.id) {
    message.warning('当前产物信息不存在')
    return
  }
  emit('copy-share')
}

function normalizeAssetUrl(url?: string) {
  if (!url) return ''
  const trimmedUrl = url.trim()
  if (!trimmedUrl) return ''
  if (/^(data:|blob:|https?:)/i.test(trimmedUrl)) {
    return trimmedUrl
  }
  if (typeof window === 'undefined') {
    return trimmedUrl
  }
  if (trimmedUrl.startsWith('//')) {
    return `${window.location.protocol}${trimmedUrl}`
  }
  if (trimmedUrl.startsWith('/')) {
    return `${window.location.origin}${trimmedUrl}`
  }
  return `${window.location.origin}/${trimmedUrl.replace(/^\.\//, '')}`
}

function getPosterSafeCoverUrl(url?: string) {
  const normalizedUrl = normalizeAssetUrl(url)
  if (!normalizedUrl) {
    return ''
  }
  if (!/^https?:/i.test(normalizedUrl)) {
    return normalizedUrl
  }
  if (typeof window === 'undefined') {
    return normalizedUrl
  }
  try {
    const parsedUrl = new URL(normalizedUrl, window.location.origin)
    if (parsedUrl.origin === window.location.origin) {
      return parsedUrl.toString()
    }
  } catch (error) {
    return normalizedUrl
  }
  const baseUrl = API_BASE_URL || window.location.origin
  return `${baseUrl}/file/proxy/image?url=${encodeURIComponent(normalizedUrl)}`
}

function wait(duration = 80) {
  return new Promise((resolve) => window.setTimeout(resolve, duration))
}

async function getPosterQrDataUrl() {
  for (let attempt = 0; attempt < 3; attempt++) {
    await nextTick()
    const result = posterQrCodeRef.value?.toDataURL?.('image/png')
    if (result) {
      return result
    }
    await wait(80)
  }
  return ''
}

function drawRoundedRectPath(
  ctx: CanvasRenderingContext2D,
  x: number,
  y: number,
  width: number,
  height: number,
  radius: number
) {
  const safeRadius = Math.min(radius, width / 2, height / 2)
  ctx.beginPath()
  ctx.moveTo(x + safeRadius, y)
  ctx.arcTo(x + width, y, x + width, y + height, safeRadius)
  ctx.arcTo(x + width, y + height, x, y + height, safeRadius)
  ctx.arcTo(x, y + height, x, y, safeRadius)
  ctx.arcTo(x, y, x + width, y, safeRadius)
  ctx.closePath()
}

function fillRoundedRect(
  ctx: CanvasRenderingContext2D,
  x: number,
  y: number,
  width: number,
  height: number,
  radius: number,
  fillStyle: string | CanvasGradient
) {
  ctx.save()
  drawRoundedRectPath(ctx, x, y, width, height, radius)
  ctx.fillStyle = fillStyle
  ctx.fill()
  ctx.restore()
}

function clipRoundedRect(
  ctx: CanvasRenderingContext2D,
  x: number,
  y: number,
  width: number,
  height: number,
  radius: number
) {
  ctx.save()
  drawRoundedRectPath(ctx, x, y, width, height, radius)
  ctx.clip()
}

function trimLineToWidth(ctx: CanvasRenderingContext2D, line: string, maxWidth: number) {
  let currentLine = line
  while (currentLine && ctx.measureText(`${currentLine}...`).width > maxWidth) {
    currentLine = currentLine.slice(0, -1)
  }
  return `${currentLine}...`
}

function buildPosterTextLines(
  ctx: CanvasRenderingContext2D,
  text: string,
  maxWidth: number,
  maxLines: number
) {
  const lines: string[] = []
  let currentLine = ''
  const chars = Array.from(text)
  for (let index = 0; index < chars.length; index++) {
    const char = chars[index]
    const nextLine = `${currentLine}${char}`
    if (ctx.measureText(nextLine).width <= maxWidth) {
      currentLine = nextLine
      continue
    }
    if (!currentLine) {
      currentLine = char
      continue
    }
    lines.push(currentLine)
    currentLine = char
    if (lines.length === maxLines - 1) {
      const remain = chars.slice(index + 1).join('')
      lines.push(trimLineToWidth(ctx, `${currentLine}${remain}`, maxWidth))
      return lines.slice(0, maxLines)
    }
  }
  if (currentLine) {
    lines.push(currentLine)
  }
  return lines.slice(0, maxLines)
}

function drawCoverImage(
  ctx: CanvasRenderingContext2D,
  image: HTMLImageElement,
  x: number,
  y: number,
  width: number,
  height: number
) {
  const imageRatio = image.width / image.height
  const boxRatio = width / height
  let drawWidth = width
  let drawHeight = height
  let drawX = x
  let drawY = y
  if (imageRatio > boxRatio) {
    drawWidth = height * imageRatio
    drawX = x - (drawWidth - width) / 2
  } else {
    drawHeight = width / imageRatio
    drawY = y - (drawHeight - height) / 2
  }
  ctx.drawImage(image, drawX, drawY, drawWidth, drawHeight)
}

function drawPosterPlaceholder(
  ctx: CanvasRenderingContext2D,
  x: number,
  y: number,
  width: number,
  height: number,
  _title: string
) {
  fillRoundedRect(ctx, x, y, width, height, 16, '#f5f5f5')
  ctx.fillStyle = 'rgba(0, 0, 0, 0.03)'
  ctx.beginPath()
  ctx.arc(x + width / 2, y + height / 2, Math.min(width, height) * 0.28, 0, Math.PI * 2)
  ctx.fill()
  ctx.fillStyle = '#d9d9d9'
  ctx.font = '600 28px "Microsoft YaHei", "PingFang SC", sans-serif'
  ctx.textAlign = 'center'
  ctx.textBaseline = 'middle'
  ctx.fillText('RichCodeWeaver', x + width / 2, y + height / 2)
  ctx.textAlign = 'left'
  ctx.textBaseline = 'alphabetic'
}

async function fetchAsDataUrl(src: string) {
  const requestUrl = typeof window === 'undefined' ? null : new URL(src, window.location.origin)
  const credentials = requestUrl && requestUrl.origin === window.location.origin ? 'include' : 'omit'
  const response = await fetch(src, { mode: 'cors', credentials })
  if (!response.ok) {
    throw new Error(`fetch image failed: ${response.status}`)
  }
  const blob = await response.blob()
  return await new Promise<string>((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(String(reader.result || ''))
    reader.onerror = () => reject(new Error('read blob failed'))
    reader.readAsDataURL(blob)
  })
}

function isSameOriginAsset(src: string) {
  if (typeof window === 'undefined') {
    return true
  }
  if (!/^https?:/i.test(src)) {
    return true
  }
  try {
    return new URL(src, window.location.origin).origin === window.location.origin
  } catch (error) {
    return false
  }
}

function getReadyPreviewCoverImage() {
  const image = posterCoverPreviewRef.value
  if (!image) {
    return null
  }
  if (!image.complete || image.naturalWidth <= 0 || image.naturalHeight <= 0) {
    return null
  }
  const currentSrc = image.currentSrc || image.src
  if (!currentSrc) {
    return null
  }
  if (!isSameOriginAsset(currentSrc) && !currentSrc.startsWith('data:') && !currentSrc.startsWith('blob:')) {
    return null
  }
  return image
}

async function loadImage(src: string) {
  const normalizedSrc = getPosterSafeCoverUrl(src)
  const candidates = [normalizedSrc]
  if (/^https?:/i.test(normalizedSrc)) {
    try {
      const dataUrl = await fetchAsDataUrl(normalizedSrc)
      candidates.unshift(dataUrl)
    } catch (error) {
      console.warn('封面转 dataURL 失败，回退原地址加载', error)
    }
  }
  for (const candidate of candidates) {
    if (!candidate) continue
    try {
      const image = await new Promise<HTMLImageElement>((resolve, reject) => {
        const imageElement = new Image()
        if (!candidate.startsWith('data:') && !isSameOriginAsset(candidate)) {
          imageElement.crossOrigin = 'anonymous'
        }
        imageElement.onload = () => resolve(imageElement)
        imageElement.onerror = () => reject(new Error('image load failed'))
        imageElement.src = candidate
      })
      return image
    } catch (error) {
      console.warn('封面加载候选失败', candidate, error)
    }
  }
  throw new Error('image load failed')
}

async function buildPosterPreviewDataUrl(includeCover: boolean) {
  const qrDataUrl = await getPosterQrDataUrl()
  if (!qrDataUrl) {
    throw new Error('qrcode unavailable')
  }
  const qrImage = await loadImage(qrDataUrl)
  let coverImage: HTMLImageElement | null = null
  if (includeCover && displayCoverUrl.value) {
    coverImage = getReadyPreviewCoverImage()
    if (!coverImage) {
      try {
        coverImage = await loadImage(displayCoverUrl.value)
      } catch (error) {
        coverImage = null
      }
    }
  }

  const W = 750
  const PAD = 40
  const INNER = W - PAD * 2
  const coverH = 380
  const qrSize = 130
  const qrPad = 14

  const titleFontSize = displayAppName.value.length > 28 ? 26 : displayAppName.value.length > 16 ? 32 : 40
  const titleLineH = Math.round(titleFontSize * 1.35)
  const typeFontSize = 22

  const canvas = document.createElement('canvas')
  canvas.width = W
  canvas.height = 10000
  const ctx = canvas.getContext('2d')
  if (!ctx) throw new Error('canvas unavailable')

  ctx.font = `700 ${titleFontSize}px "Microsoft YaHei", "PingFang SC", sans-serif`
  const titleLines = buildPosterTextLines(ctx, displayAppName.value, INNER - 40, 2)

  let y = PAD
  const coverY = y
  y += coverH + 36

  const titleY = y
  y += titleLines.length * titleLineH + 16
  const typeY = y
  y += typeFontSize + 40

  const dividerY = y
  y += 1 + 32

  const footerY = y
  const footerH = Math.max(qrSize + qrPad * 2, 100)
  y += footerH + PAD

  const H = y
  canvas.height = H

  ctx.fillStyle = '#fafafa'
  ctx.fillRect(0, 0, W, H)

  const cardX = 24
  const cardY = 20
  const cardW = W - 48
  const cardH = H - 40
  ctx.save()
  ctx.shadowColor = 'rgba(0, 0, 0, 0.08)'
  ctx.shadowBlur = 24
  ctx.shadowOffsetY = 8
  fillRoundedRect(ctx, cardX, cardY, cardW, cardH, 24, '#ffffff')
  ctx.restore()

  clipRoundedRect(ctx, PAD, coverY, INNER, coverH, 16)
  if (coverImage) {
    drawCoverImage(ctx, coverImage, PAD, coverY, INNER, coverH)
  } else {
    drawPosterPlaceholder(ctx, PAD, coverY, INNER, coverH, displayAppName.value)
  }
  ctx.restore()

  ctx.fillStyle = '#1a1a1a'
  ctx.font = `700 ${titleFontSize}px "Microsoft YaHei", "PingFang SC", sans-serif`
  ctx.textAlign = 'center'
  ctx.textBaseline = 'top'
  titleLines.forEach((line, index) => {
    ctx.fillText(line, W / 2, titleY + index * titleLineH)
  })

  ctx.fillStyle = '#999999'
  ctx.font = `500 ${typeFontSize}px "Microsoft YaHei", "PingFang SC", sans-serif`
  ctx.fillText(displayTypeLabel.value, W / 2, typeY)

  ctx.strokeStyle = '#f0f0f0'
  ctx.lineWidth = 1
  ctx.beginPath()
  ctx.moveTo(PAD + 20, dividerY)
  ctx.lineTo(W - PAD - 20, dividerY)
  ctx.stroke()

  ctx.textAlign = 'left'
  ctx.textBaseline = 'top'
  ctx.fillStyle = '#1a1a1a'
  ctx.font = '600 24px "Microsoft YaHei", "PingFang SC", sans-serif'
  ctx.fillText('扫码查看产物', PAD + 20, footerY + qrPad + 10)
  ctx.fillStyle = '#999999'
  ctx.font = '400 18px "Microsoft YaHei", "PingFang SC", sans-serif'
  ctx.fillText('RichCodeWeaver · 织码睿奇', PAD + 20, footerY + qrPad + 46)

  const qrX = W - PAD - qrSize - qrPad - 20
  const qrY = footerY + qrPad
  fillRoundedRect(ctx, qrX - 6, qrY - 6, qrSize + 12, qrSize + 12, 12, '#fafafa')
  ctx.drawImage(qrImage, qrX, qrY, qrSize, qrSize)

  ctx.textAlign = 'left'
  ctx.textBaseline = 'alphabetic'

  return canvas.toDataURL('image/png')
}

async function handleGeneratePoster() {
  if (!props.app?.id) {
    message.warning('当前产物信息不存在')
    return
  }
  if (!props.shareLink) {
    message.warning('当前分享连接暂不可用')
    return
  }
  posterGenerating.value = true
  try {
    let posterUrl = ''
    try {
      posterUrl = await buildPosterPreviewDataUrl(true)
    } catch (error) {
      posterUrl = await buildPosterPreviewDataUrl(false)
    }
    const link = document.createElement('a')
    link.download = `${displayAppName.value}-海报.png`
    link.href = posterUrl
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    emit('poster-generated')
    message.success('海报已下载')
  } catch (error) {
    console.error('生成分享海报失败', error)
    message.error('生成海报失败，请稍后重试')
  } finally {
    posterGenerating.value = false
  }
}

function handleClose() {
  visible.value = false
}
</script>

<style scoped>
.forward-modal-shell {
  padding: 20px;
  background: #ffffff;
}

.forward-mode-switch {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.mode-switch-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 46px;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  background: #fafafa;
  color: #666666;
  font-size: 14px;
  font-weight: 600;
  transition: all 0.18s ease;
  cursor: pointer;
}

.mode-switch-btn:hover {
  border-color: #d9d9d9;
  background: #f5f5f5;
}

.mode-switch-btn--active {
  color: #1a1a1a;
  border-color: #1a1a1a;
  background: #ffffff;
}

.forward-panel {
  margin-top: 18px;
  padding: 18px;
  border-radius: 16px;
  background: #ffffff;
  border: 1px solid #f0f0f0;
}

.forward-panel--share {
  padding: 20px;
}

.forward-panel--poster {
  padding: 20px;
}

.forward-panel-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.panel-title {
  font-size: 17px;
  font-weight: 700;
  color: #1a1a1a;
}

.panel-desc {
  margin-top: 6px;
  font-size: 13px;
  line-height: 1.6;
  color: #999999;
}

.selected-pill {
  flex-shrink: 0;
  padding: 6px 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  color: #1a1a1a;
  background: #f0f0f0;
}

.empty-hint {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 28px 12px;
  border-radius: 14px;
  background: #fafafa;
  color: #999999;
}

.empty-icon {
  font-size: 28px;
  margin-bottom: 10px;
  color: #d9d9d9;
}

.empty-hint p {
  margin: 0;
  font-size: 13px;
}

.friend-select-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 320px;
  overflow: auto;
  padding-right: 2px;
}

.friend-select-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 14px;
  border: 1px solid #f0f0f0;
  border-radius: 14px;
  background: #ffffff;
  cursor: pointer;
  transition: all 0.18s ease;
}

.friend-select-item:hover {
  border-color: #d9d9d9;
  background: #fafafa;
}

.friend-select-item--active {
  border-color: #1a1a1a;
  background: #fafafa;
}

.friend-select-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex: 1;
  min-width: 0;
}

.friend-select-name {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
}

.friend-select-meta {
  font-size: 12px;
  color: #999999;
}

.friend-select-check {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #1a1a1a;
  background: #f0f0f0;
  font-size: 15px;
}

.share-copy-card {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.share-copy-visual,
.share-copy-content,
.poster-preview-card,
.poster-action-card {
  background: #ffffff;
  overflow: hidden;
}

.share-copy-visual {
  position: relative;
  background: #fafafa;
  aspect-ratio: 16 / 9;
}

.share-copy-cover {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.share-copy-cover--placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fafafa;
}

.share-copy-placeholder-logo {
  width: 140px;
  max-width: 50%;
  opacity: 0.7;
}

.share-copy-visual-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, transparent 0%, rgba(0, 0, 0, 0.3) 100%);
  padding: 16px;
  display: flex;
  align-items: flex-start;
  justify-content: flex-end;
}

.share-copy-chip {
  display: inline-flex;
  align-items: center;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.92);
  color: #1a1a1a;
  font-size: 12px;
  font-weight: 600;
}

.share-copy-content {
  padding: 20px;
  display: flex;
  flex-direction: column;
}

.share-copy-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.share-copy-meta-card {
  margin-top: 16px;
  padding: 16px;
  border-radius: 12px;
  background: #fafafa;
  border: 1px solid #f0f0f0;
}

.share-copy-name {
  font-size: 20px;
  font-weight: 700;
  line-height: 1.4;
  color: #1a1a1a;
  word-break: break-word;
  margin-bottom: 12px;
}

.share-copy-meta-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 0;
}

.share-copy-meta-row + .share-copy-meta-row {
  border-top: 1px solid #f0f0f0;
}

.share-copy-meta-label {
  font-size: 13px;
  color: #999999;
  flex-shrink: 0;
  min-width: 56px;
}

.share-copy-meta-value {
  font-size: 13px;
  color: #1a1a1a;
  font-weight: 500;
}

.share-copy-platform-hint {
  margin-top: 14px;
  padding: 12px 14px;
  border-radius: 12px;
  background: #fafafa;
  border: 1px solid #f0f0f0;
  display: flex;
  align-items: center;
  gap: 12px;
}

.share-copy-platform-logo {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  flex-shrink: 0;
}

.share-copy-platform-name {
  font-size: 13px;
  font-weight: 600;
  color: #1a1a1a;
}

.share-copy-platform-slogan {
  margin-top: 2px;
  font-size: 12px;
  color: #999999;
}

.poster-workbench-card {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

:deep(.share-link-copy-btn.ant-btn-primary) {
  height: 36px;
  padding: 0 16px;
  border: none;
  border-radius: 999px;
  background: #1a1a1a;
  box-shadow: none;
  font-size: 13px;
  font-weight: 600;
}

:deep(.share-link-copy-btn.ant-btn-primary:hover) {
  background: #333333;
}

:deep(.share-link-ghost-btn.ant-btn) {
  height: 36px;
  border-radius: 999px;
  border: 1px solid #d9d9d9;
  background: #ffffff;
  color: #1a1a1a;
  padding: 0 16px;
  font-size: 13px;
  font-weight: 600;
}

:deep(.share-link-ghost-btn.ant-btn:hover) {
  border-color: #1a1a1a;
  color: #1a1a1a;
}

.poster-preview-card {
  padding: 16px;
}

.poster-preview-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.poster-preview-title {
  font-size: 15px;
  font-weight: 700;
  color: #1a1a1a;
}

.poster-preview-tip {
  font-size: 12px;
  color: #999999;
}

.poster-preview-stage {
  display: flex;
  justify-content: center;
  margin-top: 10px;
}

.poster-preview-image {
  width: min(100%, 340px);
  border-radius: 16px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
  border: 1px solid #f0f0f0;
}

.poster-preview-placeholder {
  width: 100%;
  display: flex;
  justify-content: center;
}

.poster-preview-placeholder-card {
  width: min(80%, 360px);
  border-radius: 16px;
  background: #fafafa;
  overflow: hidden;
  border: 1px solid #f0f0f0;
}

.poster-preview-placeholder-cover-wrap {
  aspect-ratio: 16 / 9;
  overflow: hidden;
  background: #ffffff;
}

.poster-preview-placeholder-cover {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.poster-preview-placeholder-cover--empty {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f5f5;
}

.poster-preview-placeholder-logo {
  width: 100px;
  opacity: 0.6;
}

.poster-preview-placeholder-body {
  padding: 20px 20px 16px;
  text-align: center;
}

.poster-preview-placeholder-name {
  font-size: 18px;
  line-height: 1.35;
  font-weight: 700;
  color: #1a1a1a;
  word-break: break-word;
}

.poster-preview-placeholder-type {
  margin-top: 8px;
  font-size: 13px;
  color: #999999;
}

.poster-preview-placeholder-divider {
  height: 1px;
  margin: 0 20px;
  background: #f0f0f0;
}

.poster-preview-placeholder-footer {
  margin: 0;
  padding: 16px 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.poster-preview-placeholder-copy-main {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
}

.poster-preview-placeholder-copy-sub {
  margin-top: 4px;
  font-size: 12px;
  color: #999999;
}

.poster-preview-placeholder-qr {
  flex-shrink: 0;
  width: 88px;
  height: 88px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  background: #ffffff;
  border: 1px solid #f0f0f0;
}

.poster-action-card {
  padding: 4px 0 0;
  border: none;
  border-radius: 0;
  background: transparent;
}

.poster-action-title {
  font-size: 15px;
  font-weight: 700;
  color: #1a1a1a;
}

.poster-action-desc {
  margin-top: 6px;
  font-size: 13px;
  line-height: 1.6;
  color: #999999;
}

.poster-action-buttons {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.poster-action-points {
  margin: 16px 0 20px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.poster-action-point {
  padding: 12px 14px;
  border-radius: 10px;
  background: #fafafa;
  color: #666666;
  font-size: 13px;
  line-height: 1.6;
  border: 1px solid #f0f0f0;
}

.poster-qrcode-source {
  position: fixed;
  left: -9999px;
  top: -9999px;
  opacity: 0;
  pointer-events: none;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 18px;
}

:deep(.modal-cancel-btn.ant-btn) {
  height: 38px;
  padding: 0 18px;
  border-radius: 999px;
  border: 1px solid #d9d9d9;
  background: #ffffff;
  color: #1a1a1a;
  font-weight: 600;
}

:deep(.modal-cancel-btn.ant-btn:hover) {
  border-color: #1a1a1a;
  color: #1a1a1a;
}

:deep(.modal-confirm-btn.ant-btn-primary) {
  height: 38px;
  padding: 0 18px;
  border: none;
  border-radius: 999px;
  background: #1a1a1a;
  color: #ffffff;
  box-shadow: none;
  font-weight: 600;
}

:deep(.modal-confirm-btn.ant-btn-primary:hover) {
  background: #333333;
}

@media (max-width: 768px) {
  .forward-modal-shell {
    padding: 14px;
  }

  .forward-mode-switch {
    grid-template-columns: 1fr;
  }

  .forward-panel-header {
    flex-direction: column;
  }

  .share-copy-card,
  .poster-workbench-card {
    flex-direction: column;
  }

  .poster-preview-head {
    align-items: flex-start;
    flex-direction: column;
  }

  .poster-preview-placeholder-card {
    width: 100%;
  }

  .poster-action-buttons {
    flex-direction: column;
    align-items: stretch;
  }

  .poster-preview-placeholder-footer,
  .share-copy-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .selected-pill {
    align-self: flex-start;
  }
}
</style>
