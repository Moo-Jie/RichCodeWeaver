<template>
  <div class="material-manage-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <h1>素材管理</h1>
      <p>管理构建数字产物的素材</p>
    </div>

    <AdminBackToDashboardButton />

    <a-tabs v-model:activeKey="activeTab" class="manage-tabs">
      <a-tab-pane key="material" tab="素材管理">
        <div class="tab-content">
          <!-- 工具栏 -->
          <div class="toolbar">
            <div class="toolbar-left">
              <a-input-search
                v-model:value="mSearchText"
                placeholder="搜索素材名称"
                style="width: 220px"
                @search="loadMaterials(true)"
                allow-clear
              />
              <a-select
                v-model:value="mFilterType"
                placeholder="素材类型"
                style="width: 120px"
                allow-clear
                @change="loadMaterials(true)"
              >
                <a-select-option value="image">图片</a-select-option>
                <a-select-option value="text">文本</a-select-option>
                <a-select-option value="video">视频</a-select-option>
                <a-select-option value="audio">音频</a-select-option>
                <a-select-option value="link">链接</a-select-option>
              </a-select>
              <a-select
                v-model:value="mFilterCategory"
                placeholder="素材分类"
                style="width: 140px"
                allow-clear
                @change="loadMaterials(true)"
              >
                <a-select-option v-for="cat in categories" :key="cat.id" :value="cat.id">
                  {{ cat.categoryName }}
                </a-select-option>
              </a-select>
              <a-select
                v-model:value="mFilterPublic"
                placeholder="是否公开"
                style="width: 110px"
                allow-clear
                @change="loadMaterials(true)"
              >
                <a-select-option :value="1">公开</a-select-option>
                <a-select-option :value="0">私有</a-select-option>
              </a-select>
            </div>
            <a-button type="primary" class="solid-btn" @click="showMAddModal">
              <template #icon><PlusOutlined /></template>
              添加素材
            </a-button>
          </div>

          <!-- 素材表格 -->
          <a-table
            :columns="mColumns"
            :data-source="materials"
            :loading="mLoading"
            :pagination="mPagination"
            @change="handleMTableChange"
            row-key="id"
            size="middle"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'materialType'">
                <a-tag class="mono-tag">
                  {{ typeLabel(record.materialType) }}
                </a-tag>
              </template>
              <template v-if="column.key === 'isPublic'">
                <a-tag :class="['mono-tag', record.isPublic === 1 ? 'status-tag-enabled' : 'status-tag-disabled']">
                  {{ record.isPublic === 1 ? '公开' : '私有' }}
                </a-tag>
              </template>
              <template v-if="column.key === 'content'">
                <template v-if="record.materialType === 'image'">
                  <a-image :src="record.content" width="48" height="48" style="object-fit:cover;border-radius:4px" />
                </template>
                <template v-else>
                  <a-tooltip :title="record.content">
                    <span class="content-preview">{{ record.content?.substring(0, 30) }}...</span>
                  </a-tooltip>
                </template>
              </template>
              <template v-if="column.key === 'action'">
                <a-space>
                  <a-button type="link" size="small" @click="showMEditModal(record)">编辑</a-button>
                  <a-button
                    type="link"
                    size="small"
                    @click="togglePublic(record)"
                  >{{ record.isPublic === 1 ? '设为私有' : '设为公开' }}</a-button>
                  <a-popconfirm title="确定删除此素材吗？" @confirm="handleMDelete(record.id)">
                    <a-button type="link" size="small" danger>删除</a-button>
                  </a-popconfirm>
                </a-space>
              </template>
            </template>
          </a-table>
        </div>
      </a-tab-pane>

      <!-- ==================== 分类管理 TAB ==================== -->
      <a-tab-pane key="category" tab="分类管理">
        <div class="tab-content">
          <div class="toolbar">
            <div class="toolbar-left"></div>
            <a-button type="primary" class="solid-btn" @click="showCAddModal">
              <template #icon><PlusOutlined /></template>
              添加分类
            </a-button>
          </div>

          <a-table
            :columns="cColumns"
            :data-source="categories"
            :loading="cLoading"
            :pagination="cPagination"
            @change="handleCTableChange"
            row-key="id"
            size="middle"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'categoryIcon'">
                <component :is="getIconComponent(record.categoryIcon)" v-if="record.categoryIcon" />
                <span v-else>-</span>
              </template>
              <template v-if="column.key === 'isEnabled'">
                <a-tag :class="['mono-tag', record.isEnabled === 1 ? 'status-tag-enabled' : 'status-tag-disabled']">
                  {{ record.isEnabled === 1 ? '启用' : '禁用' }}
                </a-tag>
              </template>
              <template v-if="column.key === 'action'">
                <a-space>
                  <a-button type="link" size="small" @click="showCEditModal(record)">编辑</a-button>
                  <a-popconfirm title="确定删除此分类吗？" @confirm="handleCDelete(record.id)">
                    <a-button type="link" size="small" danger>删除</a-button>
                  </a-popconfirm>
                </a-space>
              </template>
            </template>
          </a-table>
        </div>
      </a-tab-pane>
    </a-tabs>

    <!-- ==================== 素材 添加/编辑 弹窗 ==================== -->
    <a-modal
      v-model:open="mModalVisible"
      :title="mIsEdit ? '编辑素材' : '添加素材'"
      :confirm-loading="mSubmitLoading"
      @ok="handleMSubmit"
      width="620px"
      :destroy-on-close="true"
    >
      <a-form :model="mForm" :label-col="{ span: 5 }" :wrapper-col="{ span: 18 }" style="margin-top:12px">
        <a-form-item label="素材名称" required>
          <a-input v-model:value="mForm.materialName" placeholder="请输入素材名称" />
        </a-form-item>
        <a-form-item label="分类" required>
          <a-select v-model:value="mForm.categoryId" placeholder="请选择分类" @change="handleMCategoryChange">
            <a-select-option v-for="cat in categories" :key="cat.id" :value="cat.id">
              {{ cat.categoryName }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="素材类型" required>
          <a-input :value="typeLabel(mForm.materialType) || '选择分类后自动带出'" disabled />
        </a-form-item>
        <a-form-item v-if="mForm.materialType !== 'text'" label="素材内容" required>
          <template v-if="['image', 'video', 'audio'].includes(mForm.materialType || '')">
            <div class="upload-area">
              <a-upload
                :before-upload="handleMUpload"
                :show-upload-list="false"
                :accept="uploadAccept"
              >
                <a-button :loading="mUploading">
                  <template #icon><UploadOutlined /></template>
                  {{ mUploading ? '上传中...' : '上传文件' }}
                </a-button>
              </a-upload>
              <a-input
                v-model:value="mForm.content"
                :placeholder="getContentPlaceholder(mForm.materialType)"
                style="margin-top:8px"
              />
              <div v-if="mForm.materialType === 'image' && mForm.content" class="img-preview">
                <a-image :src="mForm.content" width="120" height="80" style="object-fit:cover;border-radius:4px;margin-top:8px" />
              </div>
            </div>
          </template>
          <a-input
            v-else
            v-model:value="mForm.content"
            :placeholder="getContentPlaceholder(mForm.materialType)"
          />
        </a-form-item>
        <a-form-item v-if="mForm.materialType === 'text'" label="素材内容" required>
          <a-button type="primary" class="solid-btn" @click="openTextEditor">
            <template #icon><EditOutlined /></template>
            打开编辑器
          </a-button>
          <span v-if="mForm.content" class="text-hint">已有 {{ mForm.content.length }} 字符</span>
        </a-form-item>
        <a-form-item label="描述">
          <a-textarea v-model:value="mForm.description" placeholder="素材描述/用途说明" :rows="2" />
        </a-form-item>
        <a-form-item label="标签">
          <a-select
            v-model:value="mTagsArray"
            mode="tags"
            placeholder="输入标签后按 Enter 确认"
            :token-separators="[',']"
            style="width:100%"
          />
        </a-form-item>
        <a-form-item label="是否公开">
          <a-switch v-model:checked="mIsPublicChecked" />
          <span style="margin-left:8px;color:#999;font-size:12px">公开后其他用户也可使用此素材</span>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- ==================== 文本素材编辑器（全屏，左编辑右预览） ==================== -->
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
            <a-button type="primary" class="solid-btn" @click="confirmTextEditor">
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

    <!-- ==================== 分类 添加/编辑 弹窗 ==================== -->
    <a-modal
      v-model:open="cModalVisible"
      :title="cIsEdit ? '编辑分类' : '添加分类'"
      :confirm-loading="cSubmitLoading"
      @ok="handleCSubmit"
      width="500px"
    >
      <a-form :model="cForm" :label-col="{ span: 5 }" :wrapper-col="{ span: 18 }" style="margin-top:12px">
        <a-form-item label="分类名称" required>
          <a-input v-model:value="cForm.categoryName" placeholder="请输入分类名称" />
        </a-form-item>
        <a-form-item label="分类编码" required>
          <a-select
            v-model:value="cForm.categoryCode"
            placeholder="请选择素材类型编码"
            :disabled="cIsEdit"
            style="width:100%"
          >
            <a-select-option value="image">image &nbsp;—&nbsp; 图片类（需上传文件）</a-select-option>
            <a-select-option value="text">text &nbsp;—&nbsp; 文本类（纯文本输入）</a-select-option>
            <a-select-option value="video">video &nbsp;—&nbsp; 视频类（需上传文件）</a-select-option>
            <a-select-option value="audio">audio &nbsp;—&nbsp; 音频类（需上传文件）</a-select-option>
            <a-select-option value="link">link &nbsp;—&nbsp; 链接类（输入 URL）</a-select-option>
          </a-select>
          <div class="form-tip" v-if="!cIsEdit">编码决定该分类下素材的录入方式，创建后不可修改</div>
        </a-form-item>
        <a-form-item label="图标">
          <a-select v-model:value="cForm.categoryIcon" placeholder="选择图标" allow-clear>
            <a-select-option value="PaperClipOutlined">PaperClipOutlined (图片)</a-select-option>
            <a-select-option value="FileTextOutlined">FileTextOutlined (文本)</a-select-option>
            <a-select-option value="VideoCameraOutlined">VideoCameraOutlined (视频)</a-select-option>
            <a-select-option value="SoundOutlined">SoundOutlined (音频)</a-select-option>
            <a-select-option value="LinkOutlined">LinkOutlined (链接)</a-select-option>
            <a-select-option value="FolderOutlined">FolderOutlined (文件夹)</a-select-option>
            <a-select-option value="FileOutlined">FileOutlined (文件)</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="描述">
          <a-textarea v-model:value="cForm.description" placeholder="分类描述" :rows="2" />
        </a-form-item>
        <a-form-item label="排序">
          <a-input-number v-model:value="cForm.sortOrder" :min="0" placeholder="越小越靠前" style="width:100%" />
        </a-form-item>
        <a-form-item label="是否启用">
          <a-switch v-model:checked="cIsEnabledChecked" />
        </a-form-item>
      </a-form>
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
  EditOutlined,
  FileOutlined,
  FileTextOutlined,
  FolderOutlined,
  LinkOutlined,
  PaperClipOutlined,
  PlusOutlined,
  SoundOutlined,
  UploadOutlined,
  VideoCameraOutlined
} from '@ant-design/icons-vue'
import {
  addMaterial,
  addMaterialCategory,
  deleteMaterial,
  deleteMaterialCategory,
  listAllMaterialByPage,
  listEnabledCategories,
  listMaterialCategoryByPage,
  updateMaterial,
  updateMaterialCategory
 } from '@/api/materialController'
 import { uploadFile } from '@/api/fileController'
import AdminBackToDashboardButton from '@/components/admin/AdminBackToDashboardButton.vue'

 const activeTab = ref('material')

// ===== 分类数据（两个 tab 共用） =====
const categories = ref<API.MaterialCategoryVO[]>([])
const loadCategories = async () => {
  try {
    const res = await listEnabledCategories()
    if (res.data.code === 0) categories.value = res.data.data || []
  } catch (e) { /* ignore */ }
}

const iconMap: Record<string, any> = {
  PaperClipOutlined, FileTextOutlined, VideoCameraOutlined,
  SoundOutlined, LinkOutlined, FolderOutlined, FileOutlined
}
const getIconComponent = (name: string) => iconMap[name] || null

// ===== 类型辅助 =====
const typeLabel = (type?: string) => {
  const m: Record<string, string> = { image: '图片', text: '文本', video: '视频', audio: '音频', link: '链接' }
  return m[type || ''] || type
}
const getContentPlaceholder = (type?: string) => {
  if (type === 'image') return '图片 URL（上传后自动填充）'
  if (type === 'video') return '视频 URL（上传后自动填充）'
  if (type === 'audio') return '音频 URL（上传后自动填充）'
  if (type === 'link') return '请输入链接 URL'
  return '请输入内容'
}
const getCategoryTypeById = (categoryId?: number) => {
  if (!categoryId) return undefined
  return categories.value.find(cat => cat.id === categoryId)?.categoryCode
}

// ===== 素材管理 =====
const materials = ref<API.MaterialVO[]>([])
const mLoading = ref(false)
const mSearchText = ref('')
const mFilterType = ref<string>()
const mFilterCategory = ref<number>()
const mFilterPublic = ref<number>()
const mPagination = ref({ current: 1, pageSize: 10, total: 0, showSizeChanger: true, showTotal: (t: number) => `共 ${t} 条` })

const mColumns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 70 },
  { title: '名称', dataIndex: 'materialName', key: 'materialName' },
  { title: '类型', dataIndex: 'materialType', key: 'materialType', width: 80 },
  { title: '分类', dataIndex: 'categoryName', key: 'categoryName', width: 100 },
  { title: '内容预览', dataIndex: 'content', key: 'content', width: 160 },
  { title: '公开', dataIndex: 'isPublic', key: 'isPublic', width: 80 },
  { title: '使用次数', dataIndex: 'useCount', key: 'useCount', width: 90 },
  { title: '操作', key: 'action', width: 200 }
]

const loadMaterials = async (reset = false) => {
  if (reset) mPagination.value.current = 1
  mLoading.value = true
  try {
    const res = await listAllMaterialByPage({
      pageNum: mPagination.value.current,
      pageSize: mPagination.value.pageSize,
      searchText: mSearchText.value || undefined,
      materialType: mFilterType.value,
      categoryId: mFilterCategory.value,
      isPublic: mFilterPublic.value
    })
    if (res.data.code === 0) {
      const d = res.data.data
      materials.value = d?.records || []
      mPagination.value.total = d?.totalRow || 0
    } else {
      message.error(res.data.message || '加载素材失败')
    }
  } catch (e) {
    message.error('加载素材失败')
  } finally {
    mLoading.value = false
  }
}

const handleMTableChange = (pag: any) => {
  mPagination.value.current = pag.current
  mPagination.value.pageSize = pag.pageSize
  loadMaterials()
}

// 素材弹窗
const mModalVisible = ref(false)
const mIsEdit = ref(false)
const mSubmitLoading = ref(false)
const mUploading = ref(false)
const mForm = ref<API.MaterialAddRequest & { id?: number }>({})
const mTagsArray = ref<string[]>([])

const mIsPublicChecked = computed({
  get: () => mForm.value.isPublic === 1,
  set: (val) => { mForm.value.isPublic = val ? 1 : 0 }
})

// 文本编辑器弹窗
const textEditorVisible = ref(false)
const textEditorContent = ref('')

const openTextEditor = () => {
  textEditorContent.value = mForm.value.content || ''
  textEditorVisible.value = true
}

const confirmTextEditor = () => {
  mForm.value.content = textEditorContent.value
  textEditorVisible.value = false
}

const uploadAccept = computed(() => {
  if (mForm.value.materialType === 'image') return 'image/*'
  if (mForm.value.materialType === 'video') return 'video/*'
  if (mForm.value.materialType === 'audio') return 'audio/*'
  return '*'
})

const handleMCategoryChange = (categoryId?: number) => {
  const nextType = getCategoryTypeById(categoryId)
  if (!nextType) return
  if (mForm.value.materialType && mForm.value.materialType !== nextType) {
    mForm.value.content = ''
  }
  mTagsArray.value = []
  mForm.value.materialType = nextType
}

const handleMUpload = async (file: File) => {
  mUploading.value = true
  try {
    const res = await uploadFile(file)
    if (res.data.code === 0) {
      mForm.value.content = res.data.data
      mForm.value.fileSize = file.size
      message.success('上传成功')
    } else {
      message.error(res.data.message || '上传失败')
    }
  } catch (e) {
    message.error('上传失败')
  } finally {
    mUploading.value = false
  }
  return false
}

const showMAddModal = () => {
  mIsEdit.value = false
  mForm.value = { isPublic: 0 }
  mTagsArray.value = []
  mModalVisible.value = true
}

const showMEditModal = (item: API.MaterialVO) => {
  mIsEdit.value = true
  mForm.value = { ...item }
  mForm.value.materialType = getCategoryTypeById(item.categoryId) || item.materialType
  mTagsArray.value = item.tags ? item.tags.split(',').filter(Boolean) : []
  mModalVisible.value = true
}

const handleMSubmit = async () => {
  if (!mForm.value.materialName) { message.warning('请输入素材名称'); return }
  if (!mForm.value.categoryId) { message.warning('请选择分类'); return }
  mForm.value.materialType = getCategoryTypeById(mForm.value.categoryId)
  if (!mForm.value.materialType) { message.warning('请选择素材类型'); return }
  if (!mForm.value.content) { message.warning('请输入素材内容'); return }

  mForm.value.tags = mTagsArray.value.join(',')
  mSubmitLoading.value = true
  try {
    const res = mIsEdit.value
      ? await updateMaterial(mForm.value as API.MaterialUpdateRequest)
      : await addMaterial(mForm.value)
    if (res.data.code === 0) {
      message.success(mIsEdit.value ? '更新成功' : '添加成功')
      mModalVisible.value = false
      loadMaterials(true)
    } else {
      message.error(res.data.message || '操作失败')
    }
  } catch (e) {
    message.error('操作失败')
  } finally {
    mSubmitLoading.value = false
  }
}

const togglePublic = async (item: API.MaterialVO) => {
  try {
    const res = await updateMaterial({ id: item.id, isPublic: item.isPublic === 1 ? 0 : 1 } as API.MaterialUpdateRequest)
    if (res.data.code === 0) {
      message.success(item.isPublic === 1 ? '已设为私有' : '已设为公开')
      loadMaterials()
    } else {
      message.error(res.data.message || '操作失败')
    }
  } catch (e) {
    message.error('操作失败')
  }
}

const handleMDelete = async (id?: number) => {
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

// ===== 分类管理 =====
const cLoading = ref(false)
const cPagination = ref({ current: 1, pageSize: 10, total: 0, showSizeChanger: true, showTotal: (t: number) => `共 ${t} 条` })

const cColumns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 80 },
  { title: '图标', dataIndex: 'categoryIcon', key: 'categoryIcon', width: 60 },
  { title: '分类名称', dataIndex: 'categoryName', key: 'categoryName' },
  { title: '分类编码', dataIndex: 'categoryCode', key: 'categoryCode' },
  { title: '描述', dataIndex: 'description', key: 'description', ellipsis: true },
  { title: '素材数量', dataIndex: 'materialCount', key: 'materialCount', width: 100 },
  { title: '排序', dataIndex: 'sortOrder', key: 'sortOrder', width: 80 },
  { title: '状态', dataIndex: 'isEnabled', key: 'isEnabled', width: 80 },
  { title: '操作', key: 'action', width: 160 }
]

const loadCategoryTable = async () => {
  cLoading.value = true
  try {
    const res = await listMaterialCategoryByPage({
      pageNum: cPagination.value.current,
      pageSize: cPagination.value.pageSize
    })
    if (res.data.code === 0) {
      const d = res.data.data
      categories.value = d?.records || []
      cPagination.value.total = d?.totalRow || 0
    }
  } catch (e) {
    message.error('加载分类列表失败')
  } finally {
    cLoading.value = false
  }
}

const handleCTableChange = (pag: any) => {
  cPagination.value.current = pag.current
  cPagination.value.pageSize = pag.pageSize
  loadCategoryTable()
}

const cModalVisible = ref(false)
const cIsEdit = ref(false)
const cSubmitLoading = ref(false)
const cForm = ref<API.MaterialCategoryAddRequest & { id?: number }>({})
const cIsEnabledChecked = computed({
  get: () => cForm.value.isEnabled === 1,
  set: (val) => { cForm.value.isEnabled = val ? 1 : 0 }
})

const showCAddModal = () => {
  cIsEdit.value = false
  cForm.value = { sortOrder: 0, isEnabled: 1 }
  cModalVisible.value = true
}

const showCEditModal = (record: API.MaterialCategoryVO) => {
  cIsEdit.value = true
  cForm.value = { ...record }
  cModalVisible.value = true
}

const handleCSubmit = async () => {
  if (!cForm.value.categoryName) { message.warning('请输入分类名称'); return }
  if (!cForm.value.categoryCode) { message.warning('请输入分类编码'); return }
  cSubmitLoading.value = true
  try {
    const res = cIsEdit.value
      ? await updateMaterialCategory(cForm.value as API.MaterialCategoryUpdateRequest)
      : await addMaterialCategory(cForm.value)
    if (res.data.code === 0) {
      message.success(cIsEdit.value ? '更新成功' : '添加成功')
      cModalVisible.value = false
      loadCategoryTable()
      loadCategories()
    } else {
      message.error(res.data.message || '操作失败')
    }
  } catch (e) {
    message.error('操作失败')
  } finally {
    cSubmitLoading.value = false
  }
}

const handleCDelete = async (id?: number) => {
  if (!id) return
  try {
    const res = await deleteMaterialCategory({ id: id as number })
    if (res.data.code === 0) {
      message.success('删除成功')
      loadCategoryTable()
      loadCategories()
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
  loadCategoryTable()
})
</script>

<style scoped lang="less">
.material-manage-page {
  padding: 24px;
  min-height: 100%;
  background: #f5f5f5;
}

.manage-tabs :deep(.ant-tabs-nav) {
  margin-bottom: 16px;
}

.manage-tabs :deep(.ant-tabs-tab-btn) {
  color: #666;
}

.manage-tabs :deep(.ant-tabs-tab:hover .ant-tabs-tab-btn) {
  color: #333;
}

.manage-tabs :deep(.ant-tabs-tab.ant-tabs-tab-active .ant-tabs-tab-btn) {
  color: #1a1a1a;
}

.manage-tabs :deep(.ant-tabs-ink-bar) {
  background: #333;
}

.tab-content {
  padding: 20px;
  border: 1px solid #e8e8e8;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.04);
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.toolbar-left {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.page-header {
  margin-bottom: 28px;

  h1 {
    font-size: 22px;
    font-weight: 700;
    color: #1a1a1a;
    margin: 0 0 6px;
  }

  p {
    font-size: 14px;
    color: #999;
    margin: 0;
  }
}

.content-preview {
  font-size: 12px;
  color: #666;
  max-width: 150px;
  display: inline-block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.upload-area {
  display: flex;
  flex-direction: column;
}

.img-preview {
  margin-top: 4px;
}

.form-tip {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

.text-hint {
  margin-left: 12px;
  font-size: 12px;
  color: #666;
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
  color: #333;
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

.mono-tag {
  margin-inline-end: 0;
  border-radius: 999px;
  border: 1px solid #d9d9d9;
  background: #fafafa;
  color: #333;
}

.status-tag-enabled {
  background: #f5f5f5;
  color: #222;
}

.status-tag-disabled {
  background: #fafafa;
  color: #777;
}

.solid-btn {
  border-color: #1f1f1f;
  background: #1f1f1f;
  box-shadow: none;
}

.solid-btn:hover,
.solid-btn:focus {
  border-color: #000;
  background: #000;
}

.material-manage-page :deep(.ant-btn-primary) {
  border-color: #1f1f1f;
  background: #1f1f1f;
  box-shadow: none;
}

.material-manage-page :deep(.ant-btn-primary:hover),
.material-manage-page :deep(.ant-btn-primary:focus) {
  border-color: #000;
  background: #000;
}

.material-manage-page :deep(.ant-btn-default) {
  border-color: #d9d9d9;
  color: #333;
  background: #fff;
  box-shadow: none;
}

.material-manage-page :deep(.ant-btn-default:hover),
.material-manage-page :deep(.ant-btn-default:focus) {
  border-color: #999;
  color: #000;
}

.material-manage-page :deep(.ant-btn-link) {
  color: #333;
}

.material-manage-page :deep(.ant-btn-link:hover),
.material-manage-page :deep(.ant-btn-link:focus) {
  color: #000;
}

.material-manage-page :deep(.ant-btn-link.ant-btn-dangerous) {
  color: #666 !important;
}

.material-manage-page :deep(.ant-btn-link.ant-btn-dangerous:hover),
.material-manage-page :deep(.ant-btn-link.ant-btn-dangerous:focus) {
  color: #000 !important;
}

.material-manage-page :deep(.ant-input),
.material-manage-page :deep(.ant-input-affix-wrapper),
.material-manage-page :deep(.ant-select-selector),
.material-manage-page :deep(.ant-input-number),
.material-manage-page :deep(.ant-input-number-input),
.material-manage-page :deep(.ant-picker) {
  border-color: #d9d9d9 !important;
  box-shadow: none !important;
}

.material-manage-page :deep(.ant-input:hover),
.material-manage-page :deep(.ant-input-affix-wrapper:hover),
.material-manage-page :deep(.ant-select:hover .ant-select-selector),
.material-manage-page :deep(.ant-input-number:hover) {
  border-color: #999 !important;
}

.material-manage-page :deep(.ant-input:focus),
.material-manage-page :deep(.ant-input-affix-wrapper-focused),
.material-manage-page :deep(.ant-select-focused .ant-select-selector),
.material-manage-page :deep(.ant-input-number-focused) {
  border-color: #999 !important;
  box-shadow: 0 0 0 2px rgba(0, 0, 0, 0.05) !important;
}

.material-manage-page :deep(.ant-switch) {
  background: #bfbfbf;
}

.material-manage-page :deep(.ant-switch.ant-switch-checked) {
  background: #595959;
}

.material-manage-page :deep(.ant-table-wrapper .ant-table) {
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  overflow: hidden;
}

.material-manage-page :deep(.ant-table-thead > tr > th) {
  background: #fafafa;
  color: #333;
  border-bottom: 1px solid #f0f0f0;
}

.material-manage-page :deep(.ant-table-tbody > tr > td) {
  border-bottom: 1px solid #f5f5f5;
}

.material-manage-page :deep(.ant-table-tbody > tr.ant-table-row:hover > td) {
  background: #fafafa;
}

.material-manage-page :deep(.ant-pagination-item-active) {
  border-color: #333;
}

.material-manage-page :deep(.ant-pagination-item-active a) {
  color: #333;
}

.material-manage-page :deep(.ant-modal-content) {
  border-radius: 16px;
  overflow: hidden;
}

.material-manage-page :deep(.ant-modal-header) {
  border-bottom: 1px solid #f0f0f0;
  background: #fafafa;
}

.material-manage-page :deep(.ant-modal-title) {
  color: #1a1a1a;
}
</style>
