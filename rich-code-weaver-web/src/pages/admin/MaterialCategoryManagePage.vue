<template>
  <div class="material-category-manage-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-left">
        <h2>
          <span class="header-icon">📁</span>
          素材分类管理
        </h2>
        <p class="header-desc">管理素材的分类体系，支持自定义图标和排序</p>
      </div>
      <a-button type="primary" size="large" @click="showAddModal" class="add-btn">
        <template #icon><PlusOutlined /></template>
        添加分类
      </a-button>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-icon total">📊</div>
        <div class="stat-info">
          <div class="stat-value">{{ categories.length }}</div>
          <div class="stat-label">总分类数</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon enabled">✅</div>
        <div class="stat-info">
          <div class="stat-value">{{ enabledCount }}</div>
          <div class="stat-label">已启用</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon disabled">⏸️</div>
        <div class="stat-info">
          <div class="stat-value">{{ disabledCount }}</div>
          <div class="stat-label">已禁用</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon materials">📦</div>
        <div class="stat-info">
          <div class="stat-value">{{ totalMaterialCount }}</div>
          <div class="stat-label">素材总数</div>
        </div>
      </div>
    </div>

    <!-- 视图切换 -->
    <div class="view-toolbar">
      <div class="view-switch">
        <span
          :class="['view-btn', { active: viewMode === 'card' }]"
          @click="viewMode = 'card'"
        >
          <AppstoreOutlined /> 卡片
        </span>
        <span
          :class="['view-btn', { active: viewMode === 'table' }]"
          @click="viewMode = 'table'"
        >
          <UnorderedListOutlined /> 列表
        </span>
      </div>
    </div>

    <!-- 卡片视图 -->
    <div v-if="viewMode === 'card'" class="category-cards">
      <div v-if="loading" class="loading-state">
        <a-spin size="large" />
      </div>
      <a-empty v-else-if="categories.length === 0" description="暂无分类" />
      <div v-else class="cards-grid">
        <div
          v-for="item in categories"
          :key="item.id"
          :class="['category-card', { disabled: item.isEnabled !== 1 }]"
        >
          <div class="card-header">
            <div class="card-icon">
              <component :is="getIconComponent(item.categoryIcon)" v-if="item.categoryIcon" />
              <FolderOutlined v-else />
            </div>
            <div class="card-status">
              <a-switch
                :checked="item.isEnabled === 1"
                size="small"
                @change="handleToggleEnabled(item, $event)"
              />
            </div>
          </div>
          <div class="card-body">
            <div class="card-name">{{ item.categoryName }}</div>
            <div class="card-code">{{ item.categoryCode }}</div>
            <div class="card-desc" v-if="item.description">{{ item.description }}</div>
          </div>
          <div class="card-footer">
            <div class="card-stats">
              <span class="stat-item">
                <FileOutlined /> {{ item.materialCount || 0 }} 个素材
              </span>
              <span class="stat-item">
                排序: {{ item.sortOrder || 0 }}
              </span>
            </div>
            <div class="card-actions">
              <a-button type="text" size="small" @click="showEditModal(item)">
                <EditOutlined />
              </a-button>
              <a-popconfirm
                title="确定删除此分类吗？"
                @confirm="confirmDelete(item.id)"
                placement="topRight"
              >
                <a-button type="text" size="small" danger>
                  <DeleteOutlined />
                </a-button>
              </a-popconfirm>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 表格视图 -->
    <div v-else class="category-table">
      <a-table
        :columns="columns"
        :data-source="categories"
        :loading="loading"
        :pagination="pagination"
        @change="handleTableChange"
        row-key="id"
        size="middle"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'categoryIcon'">
            <div class="table-icon">
              <component :is="getIconComponent(record.categoryIcon)" v-if="record.categoryIcon" />
              <FolderOutlined v-else />
            </div>
          </template>
          <template v-if="column.key === 'categoryName'">
            <div class="table-name">
              <span class="name-text">{{ record.categoryName }}</span>
              <span class="name-code">{{ record.categoryCode }}</span>
            </div>
          </template>
          <template v-if="column.key === 'materialCount'">
            <a-tag color="blue">{{ record.materialCount || 0 }} 个</a-tag>
          </template>
          <template v-if="column.key === 'isEnabled'">
            <a-switch
              :checked="record.isEnabled === 1"
              size="small"
              @change="handleToggleEnabled(record, $event)"
            />
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="showEditModal(record)">
                <EditOutlined /> 编辑
              </a-button>
              <a-popconfirm title="确定删除此分类吗？" @confirm="confirmDelete(record.id)">
                <a-button type="link" size="small" danger>
                  <DeleteOutlined /> 删除
                </a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </div>

    <!-- 添加/编辑弹窗 -->
    <a-modal
      v-model:open="modalVisible"
      :title="isEdit ? '编辑分类' : '添加分类'"
      :confirm-loading="submitLoading"
      @ok="handleSubmit"
      width="520px"
      class="category-modal"
    >
      <a-form :model="formData" :label-col="{ span: 5 }" :wrapper-col="{ span: 18 }" class="category-form">
        <a-form-item label="分类名称" required>
          <a-input v-model:value="formData.categoryName" placeholder="请输入分类名称" size="large" />
        </a-form-item>
        <a-form-item label="分类编码" required>
          <a-select
            v-model:value="formData.categoryCode"
            placeholder="请选择素材类型编码"
            :disabled="isEdit"
            size="large"
          >
            <a-select-option value="image">
              <div class="code-option">
                <PaperClipOutlined /> image — 图片类
              </div>
            </a-select-option>
            <a-select-option value="text">
              <div class="code-option">
                <FileTextOutlined /> text — 文本类
              </div>
            </a-select-option>
            <a-select-option value="video">
              <div class="code-option">
                <VideoCameraOutlined /> video — 视频类
              </div>
            </a-select-option>
            <a-select-option value="audio">
              <div class="code-option">
                <SoundOutlined /> audio — 音频类
              </div>
            </a-select-option>
            <a-select-option value="link">
              <div class="code-option">
                <LinkOutlined /> link — 链接类
              </div>
            </a-select-option>
          </a-select>
          <div class="form-tip" v-if="!isEdit">
            <InfoCircleOutlined /> 编码决定该分类下素材的录入方式，创建后不可修改
          </div>
        </a-form-item>
        <a-form-item label="图标">
          <a-select v-model:value="formData.categoryIcon" placeholder="选择图标" allow-clear size="large">
            <a-select-option v-for="icon in iconOptions" :key="icon.value" :value="icon.value">
              <div class="icon-option">
                <component :is="getIconComponent(icon.value)" />
                <span>{{ icon.label }}</span>
              </div>
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="描述">
          <a-textarea
            v-model:value="formData.description"
            placeholder="分类描述（可选）"
            :rows="3"
            show-count
            :maxlength="200"
          />
        </a-form-item>
        <a-form-item label="排序">
          <a-input-number
            v-model:value="formData.sortOrder"
            :min="0"
            :max="9999"
            placeholder="越小越靠前"
            style="width: 100%"
            size="large"
          />
        </a-form-item>
        <a-form-item label="状态">
          <a-radio-group v-model:value="formData.isEnabled" button-style="solid">
            <a-radio-button :value="1">
              <CheckCircleOutlined /> 启用
            </a-radio-button>
            <a-radio-button :value="0">
              <StopOutlined /> 禁用
            </a-radio-button>
          </a-radio-group>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import {
  AppstoreOutlined,
  CheckCircleOutlined,
  DeleteOutlined,
  EditOutlined,
  FileOutlined,
  FileTextOutlined,
  FolderOutlined,
  InfoCircleOutlined,
  LinkOutlined,
  PaperClipOutlined,
  PlusOutlined,
  SoundOutlined,
  StopOutlined,
  UnorderedListOutlined,
  VideoCameraOutlined
} from '@ant-design/icons-vue'
import {
  addMaterialCategory,
  deleteMaterialCategory,
  listMaterialCategoryByPage,
  updateMaterialCategory
} from '@/api/materialController'

// 视图模式
const viewMode = ref<'card' | 'table'>('card')

// 图标映射
const iconMap: Record<string, any> = {
  PaperClipOutlined,
  FileTextOutlined,
  VideoCameraOutlined,
  SoundOutlined,
  LinkOutlined,
  FolderOutlined,
  FileOutlined
}

const iconOptions = [
  { value: 'PaperClipOutlined', label: '图片' },
  { value: 'FileTextOutlined', label: '文本' },
  { value: 'VideoCameraOutlined', label: '视频' },
  { value: 'SoundOutlined', label: '音频' },
  { value: 'LinkOutlined', label: '链接' },
  { value: 'FolderOutlined', label: '文件夹' },
  { value: 'FileOutlined', label: '文件' }
]

const getIconComponent = (iconName: string) => {
  return iconMap[iconName] || null
}

// 表格列定义
const columns = [
  { title: '图标', dataIndex: 'categoryIcon', key: 'categoryIcon', width: 60 },
  { title: '分类名称', dataIndex: 'categoryName', key: 'categoryName' },
  { title: '描述', dataIndex: 'description', key: 'description', ellipsis: true },
  { title: '素材数量', dataIndex: 'materialCount', key: 'materialCount', width: 100 },
  { title: '排序', dataIndex: 'sortOrder', key: 'sortOrder', width: 80 },
  { title: '状态', dataIndex: 'isEnabled', key: 'isEnabled', width: 100 },
  { title: '操作', key: 'action', width: 160 }
]

// 数据状态
const categories = ref<API.MaterialCategoryVO[]>([])
const loading = ref(false)
const pagination = ref({
  current: 1,
  pageSize: 20,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`
})

// 统计数据
const enabledCount = computed(() => categories.value.filter(c => c.isEnabled === 1).length)
const disabledCount = computed(() => categories.value.filter(c => c.isEnabled !== 1).length)
const totalMaterialCount = computed(() => categories.value.reduce((sum, c) => sum + (c.materialCount || 0), 0))

// 弹窗状态
const modalVisible = ref(false)
const isEdit = ref(false)
const submitLoading = ref(false)
const formData = ref<API.MaterialCategoryAddRequest & { id?: number }>({})

// 加载分类列表
const loadCategories = async () => {
  loading.value = true
  try {
    const res = await listMaterialCategoryByPage({
      pageNum: pagination.value.current,
      pageSize: pagination.value.pageSize
    })
    if (res.data.code === 0) {
      const data = res.data.data
      categories.value = data?.records || []
      pagination.value.total = data?.totalRow || 0
    }
  } catch (e) {
    message.error('加载分类列表失败')
  } finally {
    loading.value = false
  }
}

const handleToggleEnabled = (record: API.MaterialCategoryVO, checked: boolean) => {
  toggleEnabled(record, checked)
}

// 表格分页变化
const handleTableChange = (pag: any) => {
  pagination.value.current = pag.current
  pagination.value.pageSize = pag.pageSize
  loadCategories()
}

// 显示添加弹窗
const showAddModal = () => {
  isEdit.value = false
  formData.value = { sortOrder: 0, isEnabled: 1 }
  modalVisible.value = true
}

// 显示编辑弹窗
const showEditModal = (record: API.MaterialCategoryVO) => {
  isEdit.value = true
  formData.value = { ...record }
  modalVisible.value = true
}

// 切换启用状态
const toggleEnabled = async (record: API.MaterialCategoryVO, checked: boolean) => {
  try {
    const res = await updateMaterialCategory({
      id: record.id,
      isEnabled: checked ? 1 : 0
    } as API.MaterialCategoryUpdateRequest)
    if (res.data.code === 0) {
      message.success(checked ? '已启用' : '已禁用')
      loadCategories()
    } else {
      message.error(res.data.message || '操作失败')
    }
  } catch (e) {
    message.error('操作失败')
  }
}

// 提交表单
const handleSubmit = async () => {
  if (!formData.value.categoryName) {
    message.warning('请输入分类名称')
    return
  }
  if (!formData.value.categoryCode) {
    message.warning('请输入分类编码')
    return
  }

  submitLoading.value = true
  try {
    if (isEdit.value) {
      const res = await updateMaterialCategory(formData.value as API.MaterialCategoryUpdateRequest)
      if (res.data.code === 0) {
        message.success('更新成功')
        modalVisible.value = false
        loadCategories()
      } else {
        message.error(res.data.message || '更新失败')
      }
    } else {
      const res = await addMaterialCategory(formData.value)
      if (res.data.code === 0) {
        message.success('添加成功')
        modalVisible.value = false
        loadCategories()
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

// 删除分类
const handleDelete = async (id: number) => {
  try {
    const res = await deleteMaterialCategory({ id })
    if (res.data.code === 0) {
      message.success('删除成功')
      loadCategories()
    } else {
      message.error(res.data.message || '删除失败')
    }
  } catch (e) {
    message.error('删除失败')
  }
}

const confirmDelete = (id?: number) => {
  if (id == null) {
    return
  }
  handleDelete(id)
}

onMounted(() => {
  loadCategories()
})
</script>

<style scoped>
.material-category-manage-page {
  padding: 24px;
  max-width: 1400px;
  margin: 0 auto;
}

/* 页面头部 */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
}

.header-left h2 {
  margin: 0 0 6px;
  font-size: 22px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-icon {
  font-size: 24px;
}

.header-desc {
  margin: 0;
  font-size: 13px;
  color: #999;
}

.add-btn {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  border-radius: 8px;
}

.add-btn:hover {
  background: linear-gradient(135deg, #5a6fd6 0%, #6a4190 100%);
}

/* 统计卡片 */
.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  padding: 16px 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  transition: all 0.2s;
}

.stat-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  transform: translateY(-2px);
}

.stat-icon {
  font-size: 28px;
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-icon.total { background: linear-gradient(135deg, #e0e7ff 0%, #c7d2fe 100%); }
.stat-icon.enabled { background: linear-gradient(135deg, #d1fae5 0%, #a7f3d0 100%); }
.stat-icon.disabled { background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%); }
.stat-icon.materials { background: linear-gradient(135deg, #dbeafe 0%, #bfdbfe 100%); }

.stat-value {
  font-size: 24px;
  font-weight: 600;
  color: #1a1a1a;
}

.stat-label {
  font-size: 13px;
  color: #999;
}

/* 视图切换 */
.view-toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 16px;
}

.view-switch {
  display: flex;
  background: #f5f5f5;
  border-radius: 8px;
  padding: 4px;
}

.view-btn {
  padding: 6px 14px;
  font-size: 13px;
  color: #666;
  cursor: pointer;
  border-radius: 6px;
  display: flex;
  align-items: center;
  gap: 6px;
  transition: all 0.2s;
}

.view-btn:hover {
  color: #1677ff;
}

.view-btn.active {
  background: #fff;
  color: #1677ff;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

/* 卡片视图 */
.category-cards {
  min-height: 200px;
}

.loading-state {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 60px 0;
}

.cards-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

.category-card {
  background: #fff;
  border: 1px solid #eee;
  border-radius: 12px;
  overflow: hidden;
  transition: all 0.2s;
}

.category-card:hover {
  border-color: #667eea;
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.12);
}

.category-card.disabled {
  opacity: 0.6;
  background: #fafafa;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 16px 12px;
  background: linear-gradient(135deg, #f8f9ff 0%, #f0f4ff 100%);
}

.card-icon {
  width: 44px;
  height: 44px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 20px;
}

.card-body {
  padding: 12px 16px;
}

.card-name {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 4px;
}

.card-code {
  font-size: 12px;
  color: #999;
  font-family: monospace;
  background: #f5f5f5;
  padding: 2px 8px;
  border-radius: 4px;
  display: inline-block;
  margin-bottom: 8px;
}

.card-desc {
  font-size: 13px;
  color: #666;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-top: 1px solid #f0f0f0;
  background: #fafafa;
}

.card-stats {
  display: flex;
  gap: 12px;
}

.stat-item {
  font-size: 12px;
  color: #999;
  display: flex;
  align-items: center;
  gap: 4px;
}

.card-actions {
  display: flex;
  gap: 4px;
}

/* 表格视图 */
.category-table {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
}

.table-icon {
  width: 32px;
  height: 32px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 14px;
}

.table-name {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.name-text {
  font-weight: 500;
  color: #1a1a1a;
}

.name-code {
  font-size: 11px;
  color: #999;
  font-family: monospace;
}

/* 弹窗表单 */
.category-form {
  margin-top: 16px;
}

.form-tip {
  font-size: 12px;
  color: #faad14;
  margin-top: 6px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.code-option,
.icon-option {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 响应式 */
@media (max-width: 768px) {
  .stats-row {
    grid-template-columns: repeat(2, 1fr);
  }
  .cards-grid {
    grid-template-columns: 1fr;
  }
  .page-header {
    flex-direction: column;
    gap: 16px;
  }
}
</style>
