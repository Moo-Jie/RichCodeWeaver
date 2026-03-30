<template>
  <div class="my-material-page">
    <div class="page-header">
      <h2>我的素材</h2>
      <a-button type="primary" @click="showAddModal">
        <template #icon><PlusOutlined /></template>
        添加素材
      </a-button>
    </div>

    <!-- 筛选区域 -->
    <div class="filter-bar">
      <a-input-search
        v-model:value="searchText"
        placeholder="搜索素材名称、描述、标签"
        style="width: 280px"
        @search="handleSearch"
        allow-clear
      />
      <a-select
        v-model:value="filterCategoryId"
        placeholder="选择分类"
        style="width: 150px"
        allow-clear
        @change="handleSearch"
      >
        <a-select-option v-for="cat in categories" :key="cat.id" :value="cat.id">
          {{ cat.categoryName }}
        </a-select-option>
      </a-select>
      <a-select
        v-model:value="filterType"
        placeholder="素材类型"
        style="width: 120px"
        allow-clear
        @change="handleSearch"
      >
        <a-select-option value="image">图片</a-select-option>
        <a-select-option value="text">文本</a-select-option>
        <a-select-option value="video">视频</a-select-option>
        <a-select-option value="audio">音频</a-select-option>
        <a-select-option value="link">链接</a-select-option>
      </a-select>
    </div>

    <!-- 素材列表 -->
    <div class="material-grid" v-if="materials.length > 0">
      <div
        v-for="item in materials"
        :key="item.id"
        class="material-card"
        @click="showDetailModal(item)"
      >
        <!-- 缩略图/预览 -->
        <div class="material-preview">
          <template v-if="item.materialType === 'image'">
            <img :src="item.content || item.thumbnailUrl" :alt="item.materialName" />
          </template>
          <template v-else-if="item.materialType === 'video'">
            <div class="preview-icon video">
              <VideoCameraOutlined />
            </div>
          </template>
          <template v-else-if="item.materialType === 'audio'">
            <div class="preview-icon audio">
              <SoundOutlined />
            </div>
          </template>
          <template v-else-if="item.materialType === 'text'">
            <div class="preview-text">{{ item.content?.substring(0, 100) }}...</div>
          </template>
          <template v-else>
            <div class="preview-icon link">
              <LinkOutlined />
            </div>
          </template>
        </div>
        <!-- 信息 -->
        <div class="material-info">
          <div class="material-name">{{ item.materialName }}</div>
          <div class="material-meta">
            <span class="category">{{ item.categoryName }}</span>
            <span class="use-count">使用 {{ item.useCount }} 次</span>
          </div>
        </div>
        <!-- 操作按钮 -->
        <div class="material-actions" @click.stop>
          <a-button size="small" type="text" @click="showEditModal(item)">
            <EditOutlined />
          </a-button>
          <a-popconfirm
            title="确定删除此素材吗？"
            @confirm="handleDelete(item.id)"
          >
            <a-button size="small" type="text" danger>
              <DeleteOutlined />
            </a-button>
          </a-popconfirm>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <a-empty v-else description="暂无素材" style="margin-top: 80px">
      <a-button type="primary" @click="showAddModal">添加素材</a-button>
    </a-empty>

    <!-- 加载更多 -->
    <div class="load-more" v-if="hasMore">
      <a-button :loading="loading" @click="loadMore">加载更多</a-button>
    </div>

    <!-- 添加/编辑弹窗（非文本类型） -->
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
                style="width:120px;height:80px;object-fit:cover;border-radius:4px;border:1px solid #f0f0f0"
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
          <span v-if="formData.content" class="text-hint">已有 {{ formData.content.length }} 字符</span>
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
            <span class="char-count">{{ textEditorContent.length }} 字符</span>
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
import { ref, onMounted, computed } from 'vue'
import { MdEditor } from 'md-editor-v3'
import 'md-editor-v3/lib/style.css'
import { message } from 'ant-design-vue'
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  VideoCameraOutlined,
  SoundOutlined,
  LinkOutlined,
  UploadOutlined,
  CheckOutlined
} from '@ant-design/icons-vue'
import {
  listMyMaterialByPage,
  addMaterial,
  updateMaterial,
  deleteMaterial,
  listEnabledCategories
} from '@/api/materialController'
import { uploadFile } from '@/api/fileController'

// 分类列表
const categories = ref<API.MaterialCategoryVO[]>([])

// 素材列表
const materials = ref<API.MaterialVO[]>([])
const loading = ref(false)
const pageNum = ref(1)
const pageSize = ref(20)
const total = ref(0)
const hasMore = computed(() => materials.value.length < total.value)

// 筛选条件
const searchText = ref('')
const filterCategoryId = ref<number>()
const filterType = ref<string>()

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

const openTextEditor = () => {
  textEditorContent.value = formData.value.content || ''
  textEditorVisible.value = true
}

const confirmTextEditor = () => {
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

onMounted(() => {
  loadCategories()
  loadMaterials()
})
</script>

<style scoped>
.my-material-page {
  padding: 24px;
  max-width: 1400px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 500;
}

.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
}

.material-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 16px;
}

.material-card {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.2s;
  position: relative;
}

.material-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.material-preview {
  height: 140px;
  background: #fafafa;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.material-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
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
  padding: 12px;
}

.material-name {
  font-size: 14px;
  font-weight: 500;
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
</style>
