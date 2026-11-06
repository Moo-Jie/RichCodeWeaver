<template>
  <div class="library-page material-page">
    <header class="library-header">
      <div class="library-heading">
        <h2>我的素材</h2>
        <p>图片、视频、音频、链接和文本分区管理，按内容形态使用不同的预览方式。</p>
      </div>
      <div class="library-toolbar">
        <a-input-search
          v-model:value="searchText"
          allow-clear
          class="library-search"
          placeholder="搜索名称、描述或标签..."
          @search="handleSearch"
        />
        <a-select
          v-model:value="filterCategoryId"
          allow-clear
          class="library-select"
          placeholder="全部分类"
          @change="handleSearch"
        >
          <a-select-option v-for="cat in categories" :key="cat.id" :value="cat.id">
            {{ cat.categoryName }}
          </a-select-option>
        </a-select>
        <button class="library-primary-btn" @click="showAddModal">
          <PlusOutlined />
          添加素材
        </button>
      </div>
    </header>

    <div class="library-filter-row">
      <div class="material-type-tabs" role="tablist" aria-label="素材类型">
        <button
          v-for="item in materialTypeFilters"
          :key="item.label"
          type="button"
          class="library-tab material-type-tab"
          :class="{ 'is-active': filterType === item.value }"
          @click="setFilterType(item.value)"
        >
          {{ item.label }}
        </button>
      </div>
      <span class="library-count">共 {{ total }} 个素材</span>
    </div>

    <div v-if="loading && materials.length === 0" class="material-skeleton-grid">
      <div v-for="i in 8" :key="'skeleton-' + i" class="library-skeleton"></div>
    </div>

    <div v-else-if="materials.length === 0" class="library-empty">
      <h3>{{ searchText || filterType || filterCategoryId ? '没有匹配的素材' : '素材库还是空的' }}</h3>
      <p>{{ searchText ? '尝试换一个关键词。' : '添加图片、视频、音频、链接或文本素材。' }}</p>
      <button class="library-primary-btn" @click="showAddModal">
        <PlusOutlined />
        添加素材
      </button>
    </div>

    <div v-else class="material-sections">
      <section v-for="group in groupedMaterials" :key="group.type" class="material-section">
        <div class="material-section-heading">
          <div>
            <h3>{{ group.label }}</h3>
            <p>{{ group.description }}</p>
          </div>
          <span>{{ group.items.length }}</span>
        </div>

        <div class="material-group-grid" :class="`group-${group.type}`">
          <MaterialAssetItem
            v-for="(item, index) in group.items"
            :key="item.id"
            :item="item"
            :featured="index === 0"
            @open="showDetailModal"
            @edit="showEditModal"
            @delete="handleDeleteMaterial"
          />
        </div>
      </section>
    </div>

    <div v-if="hasMore" class="library-load-more-wrap">
      <button :disabled="loading" class="library-load-more" @click="loadMore">
        {{ loading ? '加载中...' : '加载更多' }}
      </button>
    </div>    <!-- 添加/编辑弹窗（非文本类型） -->
    <a-modal
      v-model:open="modalVisible"
      :title="isEdit ? '编辑素材' : '添加素材'"
      :confirm-loading="submitLoading"
      @ok="handleSubmit"
      width="600px"
    >
      <a-form :model="formData" :label-col="{ span: 5 }" :wrapper-col="{ span: 18 }">
        <a-form-item label="素材名称" required>
          <a-input v-model:value="formData.materialName" placeholder="请输入素材名称" />
        </a-form-item>
        <a-form-item label="分类" required>
          <a-select v-model:value="formData.categoryId" placeholder="请选择分类" @change="handleCategoryChange">
            <a-select-option v-for="cat in categories" :key="cat.id" :value="cat.id">
              {{ cat.categoryName }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="素材类型" required>
          <a-input :value="getTypeLabel(formData.materialType) || '选择分类后自动带出'" disabled />
        </a-form-item>
        <a-form-item v-if="formData.materialType !== 'text'" label="素材内容" required>
          <template v-if="['image', 'video', 'audio'].includes(formData.materialType || '')">
            <div style="display:flex;flex-direction:column;gap:8px">
              <a-upload
                :before-upload="handleUpload"
                :show-upload-list="false"
                :accept="uploadAccept"
              >
                <a-button :loading="uploading">
                  <template #icon><UploadOutlined /></template>
                  {{ uploading ? '上传中...' : '上传文件' }}
                </a-button>
              </a-upload>
              <a-input
                v-model:value="formData.content"
                :placeholder="getContentPlaceholder()"
              />
              <img
                v-if="formData.materialType === 'image' && formData.content"
                :src="formData.content"
                class="form-preview-image"
              />
            </div>
          </template>
          <a-input
            v-else
            v-model:value="formData.content"
            :placeholder="getContentPlaceholder()"
          />
        </a-form-item>
        <a-form-item v-if="formData.materialType === 'text'" label="素材内容" required>
          <a-button type="primary" @click="openTextEditor">
            <template #icon><EditOutlined /></template>
            打开编辑器
          </a-button>
          <span v-if="formData.content" class="text-hint">
            已有 {{ formData.content.length }} 字符 / {{ TEXT_MATERIAL_MAX_LENGTH }} 上限
          </span>
        </a-form-item>
        <a-form-item label="描述">
          <a-textarea v-model:value="formData.description" placeholder="素材描述/用途说明" :rows="2" />
        </a-form-item>
        <a-form-item label="标签">
          <a-select
            v-model:value="tagsArray"
            mode="tags"
            placeholder="输入标签后按 Enter 确认"
            :token-separators="[',']"
            style="width:100%"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 文本素材编辑器（全屏，左编辑右预览） -->
    <a-modal
      v-model:open="textEditorVisible"
      title="编辑文本内容"
      :footer="null"
      :body-style="{ padding: 0, height: 'calc(85vh - 55px)', overflow: 'hidden' }"
      width="90vw"
      centered
      destroy-on-close
    >
      <div class="text-editor-wrapper">
        <div class="text-editor-toolbar">
          <div class="toolbar-info">
            <span class="char-count">{{ textEditorContent.length }} 字符 / {{ TEXT_MATERIAL_MAX_LENGTH }} 上限</span>
            <span class="toolbar-tip">支持 Markdown 语法</span>
          </div>
          <div class="toolbar-actions">
            <a-button @click="textEditorVisible = false">取消</a-button>
            <a-button type="primary" @click="confirmTextEditor">
              <template #icon><CheckOutlined /></template>
              确认
            </a-button>
          </div>
        </div>
        <div class="text-editor-body">
          <MdEditor
            v-model="textEditorContent"
            language="zh-CN"
            :preview="true"
            :show-code-row-number="true"
            theme="light"
            @on-change="handleTextEditorChange"
            style="height: 100%"
          />
        </div>
      </div>
    </a-modal>

    <!-- 详情弹窗 -->
    <a-modal
      v-model:open="detailVisible"
      title="素材详情"
      :footer="null"
      width="700px"
    >
      <div class="detail-content" v-if="detailItem">
        <div class="detail-preview">
          <template v-if="detailItem.materialType === 'image'">
            <img :src="detailItem.content || detailItem.thumbnailUrl" :alt="detailItem.materialName" style="max-width: 100%; max-height: 400px" />
          </template>
          <template v-else-if="detailItem.materialType === 'video'">
            <video :src="detailItem.content" controls style="max-width: 100%; max-height: 400px"></video>
          </template>
          <template v-else-if="detailItem.materialType === 'audio'">
            <audio :src="detailItem.content" controls style="width: 100%"></audio>
          </template>
          <template v-else-if="detailItem.materialType === 'text'">
            <div class="text-preview">{{ detailItem.content }}</div>
          </template>
          <template v-else>
            <a :href="detailItem.content" target="_blank" rel="noopener">{{ detailItem.content }}</a>
          </template>
        </div>
        <a-descriptions :column="2" style="margin-top: 16px">
          <a-descriptions-item label="名称">{{ detailItem.materialName }}</a-descriptions-item>
          <a-descriptions-item label="分类">{{ detailItem.categoryName }}</a-descriptions-item>
          <a-descriptions-item label="类型">{{ getTypeLabel(detailItem.materialType) }}</a-descriptions-item>
          <a-descriptions-item label="使用次数">{{ detailItem.useCount }}</a-descriptions-item>
          <a-descriptions-item label="是否公开">{{ detailItem.isPublic ? '是' : '否' }}</a-descriptions-item>
          <a-descriptions-item label="创建时间">{{ detailItem.createTime }}</a-descriptions-item>
          <a-descriptions-item label="描述" :span="2">{{ detailItem.description || '-' }}</a-descriptions-item>
          <a-descriptions-item label="标签" :span="2">{{ detailItem.tags || '-' }}</a-descriptions-item>
        </a-descriptions>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { MdEditor } from 'md-editor-v3'
import 'md-editor-v3/lib/style.css'
import { message } from 'ant-design-vue'
import {
  CheckOutlined,
  DeleteOutlined,
  EditOutlined,
  PlusOutlined,
  UploadOutlined
} from '@ant-design/icons-vue'
import {
  addMaterial,
  deleteMaterial,
  listEnabledCategories,
  listMyMaterialByPage,
  updateMaterial
} from '@/api/materialController'
import { uploadFile } from '@/api/fileController'
import MaterialAssetItem from '@/components/material/MaterialAssetItem.vue'

// 分类列表
const categories = ref<API.MaterialCategoryVO[]>([])

// 素材列表
const materials = ref<API.MaterialVO[]>([])
const loading = ref(false)
const pageNum = ref(1)
const pageSize = ref(20)
const total = ref(0)
const hasMore = computed(() => materials.value.length < total.value)

const materialTypeFilters: { label: string; value?: string }[] = [
  { label: '全部' },
  { label: '图片', value: 'image' },
  { label: '视频', value: 'video' },
  { label: '音频', value: 'audio' },
  { label: '链接', value: 'link' },
  { label: '文本', value: 'text' }
]

const materialGroups = [
  { type: 'image', label: '图片', description: '视觉素材与封面资源' },
  { type: 'video', label: '视频', description: '可直接预览与播放的视频' },
  { type: 'audio', label: '音频', description: '用于对话、氛围或产品的音频' },
  { type: 'link', label: '链接', description: '参考网页与外部资源' },
  { type: 'text', label: '文本与数据', description: '可复用的文字、表格与数据内容' }
]

const groupedMaterials = computed(() => {
  return materialGroups
    .map(group => ({
      ...group,
      items: materials.value.filter(item => (item.materialType || 'text') === group.type)
    }))
    .filter(group => group.items.length > 0)
})

// 筛选条件
const searchText = ref('')
const filterCategoryId = ref<number>()
const filterType = ref<string>()
const setFilterType = (value?: string) => {
  filterType.value = value
  handleSearch()
}

// 弹窗状态
const modalVisible = ref(false)
const isEdit = ref(false)
const submitLoading = ref(false)
const uploading = ref(false)
const formData = ref<API.MaterialAddRequest & { id?: number }>({})
const tagsArray = ref<string[]>([])

// 详情弹窗
const detailVisible = ref(false)
const detailItem = ref<API.MaterialVO | null>(null)

// 文本编辑器弹窗
const textEditorVisible = ref(false)
const textEditorContent = ref('')
const TEXT_MATERIAL_MAX_LENGTH = 1800

const openTextEditor = () => {
  textEditorContent.value = formData.value.content || ''
  textEditorVisible.value = true
}

const handleTextEditorChange = (value: string) => {
  if (value.length > TEXT_MATERIAL_MAX_LENGTH) {
    message.warning(`文本素材最多 ${TEXT_MATERIAL_MAX_LENGTH} 字符`)
    textEditorContent.value = value.slice(0, TEXT_MATERIAL_MAX_LENGTH)
    return
  }
  textEditorContent.value = value
}

const confirmTextEditor = () => {
  if (textEditorContent.value.length > TEXT_MATERIAL_MAX_LENGTH) {
    message.warning(`文本素材最多 ${TEXT_MATERIAL_MAX_LENGTH} 字符`)
    return
  }
  formData.value.content = textEditorContent.value
  textEditorVisible.value = false
}

const getCategoryTypeById = (categoryId?: number) => {
  if (!categoryId) return undefined
  return categories.value.find(cat => cat.id === categoryId)?.categoryCode
}

// 加载分类
const loadCategories = async () => {
  try {
    const res = await listEnabledCategories()
    if (res.data.code === 0) {
      categories.value = res.data.data || []
    }
  } catch (e) {
    console.error('加载分类失败', e)
  }
}

// 加载素材
const loadMaterials = async (reset = false) => {
  if (reset) {
    pageNum.value = 1
    materials.value = []
  }
  loading.value = true
  try {
    const res = await listMyMaterialByPage({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      searchText: searchText.value || undefined,
      categoryId: filterCategoryId.value,
      materialType: filterType.value
    })
    if (res.data.code === 0) {
      const data = res.data.data
      if (reset) {
        materials.value = data?.records || []
      } else {
        materials.value.push(...(data?.records || []))
      }
      total.value = data?.totalRow || 0
    }
  } catch (e) {
    message.error('加载素材失败')
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  pageNum.value = 1
  loadMaterials(true)
}

// 加载更多
const loadMore = () => {
  pageNum.value++
  loadMaterials()
}

// 上传文件
const handleUpload = async (file: File) => {
  uploading.value = true
  try {
    const res = await uploadFile(file)
    if (res.data.code === 0) {
      formData.value.content = res.data.data
      formData.value.fileSize = file.size
      message.success('上传成功')
    } else {
      message.error(res.data.message || '上传失败')
    }
  } catch (e) {
    message.error('上传失败')
  } finally {
    uploading.value = false
  }
  return false
}

const uploadAccept = computed(() => {
  if (formData.value.materialType === 'image') return 'image/*'
  if (formData.value.materialType === 'video') return 'video/*'
  if (formData.value.materialType === 'audio') return 'audio/*'
  return '*'
})

// 显示添加弹窗
const showAddModal = () => {
  isEdit.value = false
  formData.value = {}
  tagsArray.value = []
  modalVisible.value = true
}

// 显示编辑弹窗
const showEditModal = (item: API.MaterialVO) => {
  isEdit.value = true
  formData.value = { ...item }
  formData.value.materialType = getCategoryTypeById(item.categoryId) || item.materialType
  tagsArray.value = item.tags ? item.tags.split(',').filter(Boolean) : []
  modalVisible.value = true
}

// 显示详情弹窗
const showDetailModal = (item: API.MaterialVO) => {
  detailItem.value = item
  detailVisible.value = true
}

// 分类变化时同步素材类型
const handleCategoryChange = (categoryId?: number) => {
  const nextType = getCategoryTypeById(categoryId)
  if (!nextType) return
  if (formData.value.materialType && formData.value.materialType !== nextType) {
    formData.value.content = ''
  }
  formData.value.materialType = nextType
}

// 获取内容占位符
const getContentPlaceholder = () => {
  const type = formData.value.materialType
  if (type === 'image') return '图片 URL（上传后自动填充）'
  if (type === 'video') return '视频 URL（上传后自动填充）'
  if (type === 'audio') return '音频 URL（上传后自动填充）'
  if (type === 'link') return '请输入链接 URL'
  return '请输入内容'
}

// 获取类型标签
const getTypeLabel = (type?: string) => {
  const map: Record<string, string> = {
    image: '图片',
    text: '文本',
    video: '视频',
    audio: '音频',
    link: '链接'
  }
  return map[type || ''] || type
}

// 提交表单
const handleSubmit = async () => {
  if (!formData.value.materialName) {
    message.warning('请输入素材名称')
    return
  }
  if (!formData.value.categoryId) {
    message.warning('请选择分类')
    return
  }
  formData.value.materialType = getCategoryTypeById(formData.value.categoryId)
  if (!formData.value.materialType) {
    message.warning('请选择素材类型')
    return
  }
  if (!formData.value.content) {
    message.warning('请输入素材内容')
    return
  }
  if (formData.value.materialType === 'text' && formData.value.content.length > TEXT_MATERIAL_MAX_LENGTH) {
    message.warning(`文本素材最多 ${TEXT_MATERIAL_MAX_LENGTH} 字符`)
    return
  }

  formData.value.tags = tagsArray.value.join(',')
  submitLoading.value = true
  try {
    if (isEdit.value) {
      const res = await updateMaterial(formData.value as API.MaterialUpdateRequest)
      if (res.data.code === 0) {
        message.success('更新成功')
        modalVisible.value = false
        loadMaterials(true)
      } else {
        message.error(res.data.message || '更新失败')
      }
    } else {
      const res = await addMaterial(formData.value)
      if (res.data.code === 0) {
        message.success('添加成功')
        modalVisible.value = false
        loadMaterials(true)
      } else {
        message.error(res.data.message || '添加失败')
      }
    }
  } catch (e) {
    message.error('操作失败')
  } finally {
    submitLoading.value = false
  }
}

// 删除素材
const handleDelete = async (id?: number) => {
  if (!id) return
  try {
    const res = await deleteMaterial({ id })
    if (res.data.code === 0) {
      message.success('删除成功')
      loadMaterials(true)
    } else {
      message.error(res.data.message || '删除失败')
    }
  } catch (e) {
    message.error('删除失败')
  }
}

const handleDeleteMaterial = (item: API.MaterialVO) => {
  handleDelete(item.id)
}

onMounted(() => {
  loadCategories()
  loadMaterials()
})
</script>

<style scoped>
.my-material-page {
  padding: 32px;
  width: 100%;
  max-width: 1400px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.header-left {
  display: flex;
  flex-direction: column;
  flex: 1;
}

.page-title {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: #1a1a1a;
  margin-bottom: 6px;
}

.page-desc {
  margin: 0;
  color: #999;
  font-size: 14px;
}

.header-right {
  display: flex;
  align-items: center;
  flex-shrink: 0;
}

.create-btn {
  background: #1a1a1a;
  border-color: #1a1a1a;
  color: #fff;
}

.create-btn:hover,
.create-btn:focus {
  background: #333;
  border-color: #333;
  color: #fff;
}

.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
}

.material-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

@media (max-width: 900px) {
  .material-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }
}

.material-card {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 16px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  break-inside: avoid;
  margin-bottom: 20px;
}

.material-card:hover {
  border-color: #d0d0d0;
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
}

.material-preview {
  position: relative;
  width: 100%;
  aspect-ratio: 4 / 3;
  min-height: 180px;
  background: #f5f5f5;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.material-preview img {
  display: block;
}

.preview-image {
  max-width: 100%;
  max-height: 100%;
  width: auto;
  height: auto;
  object-fit: contain;
}

.preview-icon {
  font-size: 40px;
  color: #999;
}

.preview-icon.video { color: #1890ff; }
.preview-icon.audio { color: #52c41a; }
.preview-icon.link { color: #722ed1; }

.preview-text {
  padding: 12px;
  font-size: 12px;
  color: #666;
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 5;
  -webkit-box-orient: vertical;
}

.material-info {
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.material-name {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.material-meta {
  display: flex;
  justify-content: space-between;
  margin-top: 6px;
  font-size: 12px;
  color: #999;
}

.material-actions {
  position: absolute;
  top: 8px;
  right: 8px;
  display: flex;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.2s;
}

.material-card:hover .material-actions {
  opacity: 1;
}

.material-actions .ant-btn {
  background: rgba(255, 255, 255, 0.9);
  border-radius: 4px;
}

.material-card:hover .material-preview img {
  transform: scale(1.05);
  transition: transform 0.3s;
}

.material-card:hover .material-preview .preview-image {
  transform: scale(1.05);
}

.load-more {
  text-align: center;
  margin-top: 24px;
}

.detail-content .text-preview {
  background: #fafafa;
  padding: 16px;
  border-radius: 8px;
  white-space: pre-wrap;
  max-height: 300px;
  overflow-y: auto;
}

.form-preview-image {
  max-width: 100%;
  max-height: 220px;
  width: auto;
  height: auto;
  object-fit: contain;
  border-radius: 6px;
  border: 1px solid #f0f0f0;
}

/* 文本编辑器弹窗样式 */
.text-editor-wrapper {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.text-editor-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
  background: #fafafa;
}

.toolbar-info {
  display: flex;
  align-items: center;
  gap: 16px;
}

.char-count {
  font-size: 13px;
  color: #1677ff;
  font-weight: 500;
}

.toolbar-tip {
  font-size: 12px;
  color: #999;
}

.toolbar-actions {
  display: flex;
  gap: 8px;
}

.text-editor-body {
  flex: 1;
  overflow: hidden;
}

.text-hint {
  margin-left: 12px;
  font-size: 12px;
  color: #52c41a;
}

.text-hint.warning,
.char-count.warning {
  color: #faad14;
}
</style>
<style scoped>
.material-type-tabs {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px;
}

.material-type-tab {
  min-height: 30px;
  border: 1px solid transparent;
}

.material-type-tab.is-active {
  border-color: #e5e5e5;
}

.material-skeleton-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 20px;
}

.material-sections {
  display: flex;
  flex-direction: column;
  gap: 34px;
}

.material-section {
  min-width: 0;
}

.material-section-heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 12px;
  padding-bottom: 9px;
  border-bottom: 1px solid #f0f0f0;
}

.material-section-heading h3 {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
}

.material-section-heading p {
  margin: 4px 0 0;
  font-size: 12px;
  color: #999;
}

.material-section-heading > span {
  font-size: 12px;
  color: #999;
}

.material-group-grid {
  display: grid;
  grid-template-columns: repeat(12, minmax(0, 1fr));
  align-items: start;
  gap: 16px;
}

.material-group-grid.group-image > :deep(.material-asset) {
  grid-column: span 3;
}

.material-group-grid.group-image > :deep(.material-asset.is-featured) {
  grid-column: span 6;
}

.material-group-grid.group-video {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.material-group-grid.group-video > :deep(.material-asset) {
  grid-column: auto;
}

.material-group-grid.group-video > :deep(.material-asset.is-featured) {
  grid-column: 1 / -1;
}

.material-group-grid.group-audio,
.material-group-grid.group-link {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.material-group-grid.group-text {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.material-group-grid.group-text > :deep(.material-asset) {
  grid-column: span 1;
}

.material-group-grid.group-audio > :deep(.material-asset),
.material-group-grid.group-link > :deep(.material-asset) {
  grid-column: auto;
}

@media (max-width: 1080px) {
  .material-group-grid.group-image > :deep(.material-asset) {
    grid-column: span 4;
  }

  .material-group-grid.group-image > :deep(.material-asset.is-featured) {
    grid-column: span 8;
  }

  .material-group-grid.group-video {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .material-group-grid.group-video > :deep(.material-asset.is-featured) {
    grid-column: 1 / -1;
  }

  .material-group-grid.group-text {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .material-skeleton-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .material-group-grid,
  .material-group-grid.group-image,
  .material-group-grid.group-video,
  .material-group-grid.group-audio,
  .material-group-grid.group-link,
  .material-group-grid.group-text {
    display: flex;
    flex-direction: column;
  }

  .material-group-grid > :deep(.material-asset),
  .material-group-grid.group-image > :deep(.material-asset),
  .material-group-grid.group-video > :deep(.material-asset),
  .material-group-grid.group-image > :deep(.material-asset.is-featured),
  .material-group-grid.group-video > :deep(.material-asset.is-featured) {
    width: 100%;
    grid-column: auto;
  }
}

@media (max-width: 560px) {
  .material-skeleton-grid {
    grid-template-columns: 1fr;
  }
}
</style>
