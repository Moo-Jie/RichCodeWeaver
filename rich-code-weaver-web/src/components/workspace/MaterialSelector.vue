<template>
  <a-modal
    v-model:open="visible"
    title=""
    width="980px"
    :footer="null"
    @cancel="handleClose"
    :destroy-on-close="false"
    class="material-selector-modal"
    :body-style="{ padding: 0 }"
  >
    <!-- 自定义头部 -->
    <div class="selector-header">
      <div class="header-title">
        <PaperClipOutlined class="title-icon" />
        <span>选择素材</span>
      </div>
      <div class="header-filters">
        <a-input-search
          v-model:value="searchText"
          placeholder="搜索素材..."
          style="width: 180px"
          @search="handleSearch"
          allow-clear
          size="small"
        />
        <div class="type-filter-group">
          <span
            v-for="t in typeOptions"
            :key="t.value"
            :class="['type-tag', { active: filterType === t.value }]"
            @click="toggleTypeFilter(t.value)"
          >
            {{ t.label }}
          </span>
        </div>
      </div>
    </div>

    <!-- Tab 切换栏 -->
    <div class="selector-tabs">
      <div
        :class="['tab-item', { active: activeTab === 'personal' }]"
        @click="switchTab('personal')"
      >
        <UserOutlined class="tab-icon" />
        <span>我的素材</span>
        <span v-if="personalTotal > 0" class="tab-count">{{ personalTotal }}</span>
      </div>
      <div
        :class="['tab-item', { active: activeTab === 'public' }]"
        @click="switchTab('public')"
      >
        <GlobalOutlined class="tab-icon" />
        <span>公共素材</span>
      </div>
    </div>

    <!-- 素材列表区域 -->
    <div class="selector-content">
      <!-- 个人素材 -->
      <div v-show="activeTab === 'personal'" class="material-list-area">
        <div v-if="personalLoading && personalMaterials.length === 0" class="loading-state">
          <a-spin size="large" />
          <span>加载中...</span>
        </div>
        <a-empty v-else-if="!personalLoading && personalMaterials.length === 0"
                 description="暂无个人素材" class="empty-state">
          <template #image><InboxOutlined class="empty-icon" /></template>
        </a-empty>
        <div v-else class="material-grid">
          <MaterialItem
            v-for="item in personalMaterials"
            :key="item.id"
            :item="item"
            :selected="isSelected(item.id)"
            @click="toggleSelect(item)"
          />
        </div>
        <div class="load-more" v-if="personalHasMore && personalMaterials.length > 0">
          <a-button :loading="personalLoading" @click="loadMorePersonal" type="link">
            <template #icon><DownOutlined /></template>
            加载更多
          </a-button>
        </div>
      </div>

      <!-- 公共素材 -->
      <div v-show="activeTab === 'public'" class="material-list-area">
        <div v-if="publicLoading && publicMaterials.length === 0" class="loading-state">
          <a-spin size="large" />
          <span>加载中...</span>
        </div>
        <a-empty v-else-if="!publicLoading && publicMaterials.length === 0"
                 description="暂无公共素材" class="empty-state">
          <template #image><GlobalOutlined class="empty-icon" /></template>
        </a-empty>
        <div v-else class="material-grid">
          <MaterialItem
            v-for="item in publicMaterials"
            :key="item.id"
            :item="item"
            :selected="isSelected(item.id)"
            @click="toggleSelect(item)"
          />
        </div>
        <div class="load-more" v-if="publicHasMore && publicMaterials.length > 0">
          <a-button :loading="publicLoading" @click="loadMorePublic" type="link">
            <template #icon><DownOutlined /></template>
            加载更多
          </a-button>
        </div>
      </div>
    </div>

    <!-- 底部操作栏 -->
    <div class="selector-footer">
      <div class="footer-left">
        <template v-if="selectedIds.length > 0">
          <div class="selected-preview">
            <div
              v-for="(item, idx) in selectedMaterials.slice(0, 5)"
              :key="item.id"
              class="preview-thumb"
              :style="{ zIndex: 5 - idx }"
            >
              <img v-if="item.materialType === 'image'" :src="item.content || item.thumbnailUrl" />
              <component :is="getTypeIcon(item.materialType)" v-else class="thumb-icon" />
            </div>
            <span v-if="selectedMaterials.length > 5" class="more-count">+{{ selectedMaterials.length - 5 }}</span>
          </div>
          <span class="selected-text">已选 <b>{{ selectedIds.length }}</b> 个</span>
          <a-button type="link" size="small" @click="clearSelection" class="clear-btn">清除</a-button>
        </template>
        <span v-else class="no-select-hint">请选择素材（最多20个）</span>
      </div>
      <div class="footer-actions">
        <a-button @click="handleClose">取消</a-button>
        <a-button type="primary" @click="handleConfirm" :disabled="selectedIds.length === 0">
          <template #icon><CheckOutlined /></template>
          确认选择
        </a-button>
      </div>
    </div>
  </a-modal>
</template>

 <!-- 素材卡片子组件（内联定义） -->
 <script lang="ts">
 import { defineComponent, h, reactive } from 'vue'
 import {
  CheckOutlined as CheckOutlinedIcon,
  PictureOutlined as PictureOutlinedIcon,
  VideoCameraOutlined as VideoCameraOutlinedIcon,
  SoundOutlined as SoundOutlinedIcon,
  FileTextOutlined as FileTextOutlinedIcon,
  LinkOutlined as LinkOutlinedIcon
 } from '@ant-design/icons-vue'

 const stripMd = (s: string) =>
   s.replace(/\*\*(.*?)\*\*/g, '$1').replace(/\*(.*?)\*/g, '$1')
   .replace(/#{1,6}\s*/g, '').replace(/`([^`]+)`/g, '$1').trim()

const TYPE_COLOR: Record<string, string> = {
  image: '#1677ff', text: '#fa8c16', video: '#52c41a', audio: '#eb2f96', link: '#722ed1'
}
const TYPE_LABEL: Record<string, string> = {
  image: '图片', text: '文本', video: '视频', audio: '音频', link: '链接'
 }
 const TYPE_ICON = {
  image: PictureOutlinedIcon,
  text: FileTextOutlinedIcon,
  video: VideoCameraOutlinedIcon,
  audio: SoundOutlinedIcon,
  link: LinkOutlinedIcon
 }

 const IMAGE_STATUS = reactive<Record<number, 'loading' | 'loaded' | 'error'>>({})

 const setImageStatus = (id: number | undefined, status: 'loading' | 'loaded' | 'error') => {
  if (!id) return
  IMAGE_STATUS[id] = status
 }

 const getImageStatus = (id: number | undefined) => {
  if (!id) return 'loading'
  return IMAGE_STATUS[id] || 'loading'
 }

 const getPreviewSrc = (item: API.MaterialVO) => {
  if (item.materialType === 'image') {
    return item.content || item.thumbnailUrl || ''
  }
  return item.thumbnailUrl || item.content || ''
 }

 const getUrlSnippet = (item: API.MaterialVO) => {
  const value = item.materialType === 'image' ? (item.content || item.thumbnailUrl || '') : getPreviewSrc(item)
  if (!value) return ''
  return value.length > 48 ? `${value.substring(0, 48)}...` : value
 }

 const getSafePreviewSrc = (item: API.MaterialVO) => getPreviewSrc(item) || undefined

 export const MaterialItem = defineComponent({
  name: 'MaterialItem',
  props: { item: Object, selected: Boolean },
  emits: ['click'],
  setup(props, { emit }) {
    return () => {
      const item = props.item as API.MaterialVO
      return h('div', {
        class: ['material-item', { selected: props.selected }],
        onClick: () => emit('click')
      }, [
        props.selected ? h('div', { class: 'check-mark' }, [h(CheckOutlinedIcon)]) : null,
        h('div', { class: ['item-preview', `item-preview--${item.materialType}`] }, [
          item.materialType === 'image'
            ? h('div', { class: 'image-preview-wrap' }, [
                getImageStatus(item.id) !== 'loaded'
                  ? h('div', { class: 'image-placeholder' }, [h(PictureOutlinedIcon, { class: 'image-placeholder-icon' })])
                  : null,
                h('img', {
                  src: getSafePreviewSrc(item),
                  alt: item.materialName,
                  loading: 'lazy',
                  class: ['preview-image', { 'is-loaded': getImageStatus(item.id) === 'loaded' }],
                  onLoad: () => setImageStatus(item.id, 'loaded'),
                  onError: () => setImageStatus(item.id, 'error')
                })
              ])
          : item.materialType === 'text'
              ? h('div', { class: 'preview-text-snippet' }, stripMd(item.content?.substring(0, 120) || ''))
              : h(TYPE_ICON[item.materialType as keyof typeof TYPE_ICON] || FileTextOutlinedIcon, {
                  class: ['type-icon', item.materialType]
                })
        ]),
        h('div', { class: 'item-info' }, [
          h('div', { class: 'item-name' }, item.materialName),
          ['image', 'link'].includes(item.materialType || '') && getUrlSnippet(item)
            ? h('div', { class: 'item-url' }, getUrlSnippet(item))
            : null,
          h('div', { class: 'item-meta' }, [
            h('span', { class: 'item-category' }, item.categoryName || ''),
            h('span', {
              class: 'item-type-badge',
              style: { background: TYPE_COLOR[item.materialType || ''] || '#aaa' }
            }, TYPE_LABEL[item.materialType || ''] || item.materialType || '')
          ])
        ])
      ])
    }
  }
})
</script>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { message } from 'ant-design-vue'
import {
  CheckOutlined,
  DownOutlined,
  FileTextOutlined,
  GlobalOutlined,
  InboxOutlined,
  LinkOutlined,
  PaperClipOutlined,
  PictureOutlined,
  SoundOutlined,
  UserOutlined,
  VideoCameraOutlined
} from '@ant-design/icons-vue'
import {
  listMyMaterialByPage,
  listPublicMaterialByPage,
  listEnabledCategories
} from '@/api/materialController'
import { useLoginUserStore } from '@/stores/loginUser'

const loginUserStore = useLoginUserStore()

const props = defineProps<{
  open: boolean
  selected?: API.MaterialVO[]
}>()

const emit = defineEmits<{
  (e: 'update:open', value: boolean): void
  (e: 'confirm', materials: API.MaterialVO[]): void
}>()

const visible = computed({
  get: () => props.open,
  set: (val) => emit('update:open', val)
})

// 分类
const categories = ref<API.MaterialCategoryVO[]>([])

// 筛选
const searchText = ref('')
const filterCategoryId = ref<number>()
const filterType = ref<string>()

// Tab
const activeTab = ref('personal')

// ── 个人素材 ──
const personalMaterials = ref<API.MaterialVO[]>([])
const personalLoading = ref(false)
const personalPage = ref(1)
const personalTotal = ref(0)
const personalHasMore = computed(() => personalMaterials.value.length < personalTotal.value)

// ── 公共素材 ──
const publicMaterials = ref<API.MaterialVO[]>([])
const publicLoading = ref(false)
const publicPage = ref(1)
const publicTotal = ref(0)
const publicHasMore = computed(() => publicMaterials.value.length < publicTotal.value)

// 选中状态
const selectedIds = ref<number[]>([])
const selectedMaterials = ref<API.MaterialVO[]>([])

const isSelected = (id?: number) => !!id && selectedIds.value.includes(id)

const loadCategories = async () => {
  try {
    const res = await listEnabledCategories()
    if (res.data.code === 0) categories.value = res.data.data || []
  } catch (e) { /* ignore */ }
}

const loadPersonal = async (reset = false) => {
  if (reset) { personalPage.value = 1; personalMaterials.value = [] }
  personalLoading.value = true
  try {
    const res = await listMyMaterialByPage({
      pageNum: personalPage.value,
      pageSize: 20,
      searchText: searchText.value || undefined,
      categoryId: filterCategoryId.value,
      materialType: filterType.value
    })
    if (res.data.code === 0) {
      const d = res.data.data
      if (reset) personalMaterials.value = d?.records || []
      else personalMaterials.value.push(...(d?.records || []))
      personalTotal.value = d?.totalRow || 0
    }
  } catch (e) { message.error('加载个人素材失败') }
  finally { personalLoading.value = false }
}

const loadPublic = async (reset = false) => {
  if (reset) { publicPage.value = 1; publicMaterials.value = [] }
  publicLoading.value = true
  try {
    const res = await listPublicMaterialByPage({
      pageNum: publicPage.value,
      pageSize: 20,
      searchText: searchText.value || undefined,
      categoryId: filterCategoryId.value,
      materialType: filterType.value
    })
    if (res.data.code === 0) {
      const d = res.data.data
      const myId = loginUserStore.loginUser?.id
      // 公约：个人素材优先——公共素材列表中排除自己的素材
      const records = (d?.records || []).filter((m: API.MaterialVO) => m.userId !== myId)
      if (reset) publicMaterials.value = records
      else publicMaterials.value.push(...records)
      publicTotal.value = d?.totalRow || 0
    }
  } catch (e) { message.error('加载公共素材失败') }
  finally { publicLoading.value = false }
}

const handleSearch = () => {
  if (activeTab.value === 'personal') loadPersonal(true)
  else loadPublic(true)
}

const switchTab = (key: string) => {
  activeTab.value = key
  if (key === 'public' && publicMaterials.value.length === 0) loadPublic(true)
}

const typeOptions = [
  { value: '', label: '全部' },
  { value: 'image', label: '图片' },
  { value: 'text', label: '文本' },
  { value: 'video', label: '视频' },
  { value: 'audio', label: '音频' },
  { value: 'link', label: '链接' }
]

const toggleTypeFilter = (val: string) => {
  filterType.value = val || undefined
  handleSearch()
}

const getTypeIcon = (type?: string) => {
  const map = {
    image: PictureOutlined,
    text: FileTextOutlined,
    video: VideoCameraOutlined,
    audio: SoundOutlined,
    link: LinkOutlined
  }
  return map[type as keyof typeof map] || FileTextOutlined
}

const loadMorePersonal = () => { personalPage.value++; loadPersonal() }
const loadMorePublic = () => { publicPage.value++; loadPublic() }

const toggleSelect = (item: API.MaterialVO) => {
  const id = item.id!
  const idx = selectedIds.value.indexOf(id)
  if (idx > -1) {
    selectedIds.value.splice(idx, 1)
    selectedMaterials.value = selectedMaterials.value.filter(m => m.id !== id)
  } else {
    if (selectedIds.value.length >= 20) { message.warning('最多选择20个素材'); return }
    selectedIds.value.push(id)
    selectedMaterials.value.push(item)
  }
}

const clearSelection = () => {
  selectedIds.value = []
  selectedMaterials.value = []
}

const handleClose = () => { visible.value = false }

const handleConfirm = () => {
  emit('confirm', selectedMaterials.value)
  visible.value = false
}

watch(() => props.open, (val) => {
  if (val) {
    if (props.selected?.length) {
      selectedMaterials.value = [...props.selected]
      selectedIds.value = props.selected.map(item => item.id!).filter(Boolean)
    } else {
      selectedIds.value = []
      selectedMaterials.value = []
    }
    if (categories.value.length === 0) loadCategories()
    activeTab.value = 'personal'
    loadPersonal(true)
  }
})
</script>

<style scoped>
/* ── 弹窗整体样式 ── */
:deep(.material-selector-modal .ant-modal-header) {
  display: none;
}
:deep(.material-selector-modal .ant-modal-content) {
  border-radius: 20px;
  overflow: hidden;
  background: #ffffff;
  box-shadow: 0 18px 48px rgba(0, 0, 0, 0.12);
}

/* ── 自定义头部 ── */
.selector-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 18px 20px 16px;
  background: #ffffff;
  color: #1a1a1a;
  border-bottom: 1px solid #f0f0f0;
}

.header-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 600;
}

.title-icon {
  font-size: 18px;
  color: #1a1a1a;
}

.header-filters {
  display: flex;
  align-items: center;
  gap: 10px;
}

.header-filters :deep(.ant-input-search) {
  border-radius: 10px;
}
.header-filters :deep(.ant-input) {
  color: #1a1a1a;
}
.header-filters :deep(.ant-input::placeholder) {
  color: #999;
}
.header-filters :deep(.ant-input-search-button) {
  color: #666;
}
.header-filters :deep(.ant-select-selector) {
  border-radius: 10px !important;
  border-color: #f0f0f0 !important;
  box-shadow: none !important;
}
.header-filters :deep(.ant-select-arrow) {
  color: #999;
}

.type-filter-group {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.type-tag {
  padding: 5px 12px;
  font-size: 12px;
  border-radius: 999px;
  cursor: pointer;
  background: #f7f7f7;
  color: #666;
  border: 1px solid #f0f0f0;
  transition: all 0.2s;
}

.type-tag:hover {
  color: #1a1a1a;
  border-color: #d9d9d9;
}

.type-tag.active {
  background: #1a1a1a;
  color: #fff;
  border-color: #1a1a1a;
  font-weight: 500;
}

/* ── Tab 切换栏 ── */
.selector-tabs {
  display: flex;
  gap: 0;
  padding: 0 20px;
  background: #ffffff;
  border-bottom: 1px solid #f0f0f0;
}

.tab-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 12px 20px;
  font-size: 14px;
  color: #666;
  cursor: pointer;
  border-bottom: 2px solid transparent;
  transition: all 0.2s;
}

.tab-item:hover {
  color: #1a1a1a;
}

.tab-item.active {
  color: #1a1a1a;
  border-bottom-color: #1a1a1a;
  font-weight: 500;
}

.tab-icon {
  font-size: 14px;
}

.tab-count {
  background: #1a1a1a;
  color: #fff;
  font-size: 11px;
  padding: 1px 6px;
  border-radius: 10px;
  min-width: 20px;
  text-align: center;
}

/* ── 素材列表区域 ── */
.selector-content {
  padding: 16px 20px;
  max-height: 420px;
  overflow-y: auto;
  background: #ffffff;
}

.material-list-area {
  min-height: 200px;
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 60px 0;
  color: #999;
}

.empty-state {
  padding: 40px 0;
}

.empty-icon {
  font-size: 48px;
  color: #bfbfbf;
}

/* ── 3列瀑布流布局 ── */
.material-grid {
  column-count: 3;
  column-gap: 14px;
}

/* ── 卡片样式 ── */
:deep(.material-item) {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 16px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.2s ease;
  position: relative;
  break-inside: avoid;
  margin-bottom: 14px;
  width: 100%;
  display: inline-block;
}

:deep(.material-item:hover) {
  border-color: #d9d9d9;
  box-shadow: 0 10px 28px rgba(0, 0, 0, 0.08);
  transform: translateY(-2px);
}

:deep(.check-mark) {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 22px;
  height: 22px;
  background: #1a1a1a;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 11px;
  z-index: 2;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.18);
}

/* ── 图片预览 ── */
:deep(.item-preview--image) {
  background: #f5f5f5;
  overflow: hidden;
  min-height: 120px;
}

:deep(.image-preview-wrap) {
  position: relative;
  min-height: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f7f7f7;
}

:deep(.image-placeholder) {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  min-height: 120px;
  padding: 24px 12px;
  color: #bfbfbf;
}

:deep(.image-placeholder-icon) {
  font-size: 32px;
}

:deep(.preview-image) {
  position: relative;
  z-index: 1;
  width: 100%;
  height: auto;
  min-height: 120px;
  max-height: 320px;
  object-fit: cover;
  display: block;
  opacity: 0;
  transition: transform 0.3s;
}

:deep(.preview-image.is-loaded) {
  opacity: 1;
}

:deep(.material-item:hover .preview-image) {
  transform: scale(1.05);
}

/* ── 文本预览 ── */
:deep(.item-preview--text) {
  padding: 12px;
  background: #fafafa;
  min-height: 60px;
}

:deep(.preview-text-snippet) {
  font-size: 12px;
  color: #666;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 4;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* ── 视频/音频/链接 ── */
:deep(.item-preview--video),
:deep(.item-preview--audio),
:deep(.item-preview--link) {
  height: 70px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fafafa;
}

:deep(.type-icon) {
  font-size: 28px;
  color: #999;
}

:deep(.type-icon.image),
:deep(.type-icon.video),
:deep(.type-icon.audio),
:deep(.type-icon.link),
:deep(.type-icon.text) {
  color: #666;
}

:deep(.item-info) {
  padding: 10px 12px;
}

:deep(.item-name) {
  font-size: 13px;
  font-weight: 500;
  color: #1a1a1a;
  line-height: 1.5;
  word-break: break-word;
  margin-bottom: 6px;
}

:deep(.item-url) {
  margin-bottom: 6px;
  font-size: 11px;
  color: #999;
  line-height: 1.5;
  word-break: break-all;
}

:deep(.item-meta) {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
}

:deep(.item-category) {
  font-size: 11px;
  color: #999;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

:deep(.item-type-badge) {
  flex-shrink: 0;
  font-size: 10px;
  color: #fff;
  padding: 2px 8px;
  border-radius: 10px;
  font-weight: 500;
}

/* ── 加载更多 ── */
.load-more {
  text-align: center;
  margin-top: 16px;
}

/* ── 底部操作栏 ── */
.selector-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 20px;
  background: #ffffff;
  border-top: 1px solid #f0f0f0;
}

.footer-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.selected-preview {
  display: flex;
  align-items: center;
}

.preview-thumb {
  width: 32px;
  height: 32px;
  border-radius: 6px;
  overflow: hidden;
  border: 2px solid #fff;
  box-shadow: 0 2px 6px rgba(0,0,0,0.1);
  margin-left: -8px;
  background: #f5f5f5;
  display: flex;
  align-items: center;
  justify-content: center;
}

.preview-thumb:first-child {
  margin-left: 0;
}

.preview-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.thumb-icon {
  font-size: 14px;
  color: #666;
}

.more-count {
  margin-left: 6px;
  font-size: 12px;
  color: #666;
  font-weight: 500;
}

.selected-text {
  font-size: 13px;
  color: #333;
}

.selected-text b {
  color: #1a1a1a;
  font-weight: 600;
}

.clear-btn {
  color: #999;
  padding: 0 4px;
}

.no-select-hint {
  font-size: 13px;
  color: #999;
}

.footer-actions {
  display: flex;
  gap: 10px;
}

.footer-actions :deep(.ant-btn-primary) {
  background: #b3b3b3;
  border-color: #1a1a1a;
  box-shadow: none;
}

.footer-actions :deep(.ant-btn-primary:hover) {
  background: #787878;
  border-color: #333333;
}

/* ── 响应式 ── */
@media (max-width: 768px) {
  .material-grid {
    column-count: 2;
  }
  .header-filters {
    flex-wrap: wrap;
  }
  .type-filter-group {
    display: none;
  }
}

@media (max-width: 560px) {
  .material-grid {
    column-count: 1;
  }
}
</style>
