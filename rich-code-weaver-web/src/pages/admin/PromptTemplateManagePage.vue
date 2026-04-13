<template>
  <div id="promptTemplateManagePage">
    <div class="page-header">
      <h1>提示词模板管理</h1>
      <p>管理系统提示词模板，支持增删改查</p>
    </div>

    <AdminBackToDashboardButton />

    <!-- 搜索面板 -->
    <a-card class="search-panel">
      <h2>筛选模板</h2>
      <a-form :model="searchParams" layout="inline" @finish="doSearch">
        <a-form-item class="search-item" label="模板名称">
          <a-input
            v-model:value="searchParams.templateName"
            allow-clear
            placeholder="输入模板名称"
          />
        </a-form-item>
        <a-form-item class="search-item" label="匹配身份">
          <a-select
            v-model:value="searchParams.matchIdentity"
            allow-clear
            placeholder="全部身份"
            style="width: 120px"
          >
            <a-select-option value="">全部</a-select-option>
            <a-select-option v-for="item in identityOptions" :key="item.value" :value="item.value">
              {{ item.label }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item class="search-item" label="匹配行业">
          <a-auto-complete
            v-model:value="searchParams.matchIndustry"
            :filter-option="filterIndustry"
            :options="industryAutoOptions"
            allow-clear
            placeholder="输入行业"
            style="width: 160px"
          />
        </a-form-item>
        <a-form-item class="search-item" label="状态">
          <a-select
            v-model:value="searchParams.isEnabled"
            allow-clear
            placeholder="全部"
            style="width: 100px"
          >
            <a-select-option :value="1">启用</a-select-option>
            <a-select-option :value="0">禁用</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item class="search-actions">
          <a-button html-type="submit" type="primary">
            <template #icon>
              <SearchOutlined />
            </template>
            搜索
          </a-button>
          <a-button @click="resetSearch">重置</a-button>
        </a-form-item>
      </a-form>
    </a-card>

    <!-- 操作栏 -->
    <div class="action-bar">
      <a-button type="primary" @click="showAddModal">
        <template #icon>
          <PlusOutlined />
        </template>
        新增模板
      </a-button>
    </div>

    <!-- 数据表格 -->
    <a-table
      :columns="columns"
      :data-source="dataList"
      :loading="loading"
      :pagination="pagination"
      row-key="id"
      size="middle"
      @change="handleTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'matchIdentity'">
          <a-tag v-if="record.matchIdentity" color="blue">
            {{ identityLabelMap[record.matchIdentity] || record.matchIdentity }}
          </a-tag>
          <span v-else class="text-muted">不限</span>
        </template>
        <template v-if="column.dataIndex === 'matchIndustry'">
          <a-tag v-if="record.matchIndustry" color="green">{{ record.matchIndustry }}</a-tag>
          <span v-else class="text-muted">不限</span>
        </template>
        <template v-if="column.dataIndex === 'isEnabled'">
          <a-tag :color="record.isEnabled === 1 ? 'green' : 'red'">
            {{ record.isEnabled === 1 ? '启用' : '禁用' }}
          </a-tag>
        </template>
        <template v-if="column.dataIndex === 'action'">
          <a-space>
            <a @click="showEditModal(record)">编辑</a>
            <a-popconfirm title="确定删除该模板？" @confirm="handleDelete(record.id)">
              <a class="danger-link">删除</a>
            </a-popconfirm>
          </a-space>
        </template>
      </template>
    </a-table>

    <!-- 新增/编辑模态框 -->
    <a-modal
      v-model:open="modalVisible"
      :body-style="{ maxHeight: '70vh', overflowY: 'auto' }"
      :cancel-text="'取消'"
      :ok-text="'提交'"
      :title="isEdit ? '编辑模板' : '新增模板'"
      width="900px"
      @ok="handleSubmit"
    >
      <a-form :label-col="{ span: 4 }" :model="formData" :wrapper-col="{ span: 20 }"
              style="margin-top: 16px;">
        <a-form-item label="模板名称" required>
          <a-input v-model:value="formData.templateName" placeholder="请输入模板名称" />
        </a-form-item>
        <a-form-item label="匹配身份">
          <a-select v-model:value="formData.matchIdentity" allow-clear placeholder="不限（留空）">
            <a-select-option v-for="item in identityOptions" :key="item.value" :value="item.value">
              {{ item.label }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="匹配行业">
          <a-auto-complete
            v-model:value="formData.matchIndustry"
            :filter-option="filterIndustry"
            :options="industryAutoOptions"
            placeholder="不限（留空）"
          />
        </a-form-item>
        <a-form-item label="模板描述">
          <a-textarea v-model:value="formData.description" :rows="2" placeholder="模板的简短描述" />
        </a-form-item>
        <a-form-item label="字段定义">
          <FieldDefinitionEditor
            v-model="formData.templateFields"
            @fields-updated="handleFieldsUpdated"
          />
        </a-form-item>
        <a-form-item label="提示词内容" required>
          <PromptContentEditor
            v-model="formData.promptContent"
            :fields="currentFields"
          />
        </a-form-item>
        <a-form-item label="排序权重">
          <a-input-number v-model:value="formData.sortOrder" :max="9999" :min="0" />
        </a-form-item>
        <a-form-item label="是否启用">
          <a-switch v-model:checked="formEnabled" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script lang="ts" setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, SearchOutlined } from '@ant-design/icons-vue'
import {
  addPromptTemplate,
  deletePromptTemplate,
  listPromptTemplateByPage,
  updatePromptTemplate
} from '@/api/promptTemplateController'
import AdminBackToDashboardButton from '@/components/admin/AdminBackToDashboardButton.vue'
import { identityLabelMap, identityOptions, industryOptions } from '@/constants/identityOptions'
import PromptContentEditor from '@/components/admin/PromptContentEditor.vue'
import FieldDefinitionEditor from '@/components/admin/FieldDefinitionEditor.vue'

const loading = ref(false)
const dataList = ref<API.PromptTemplateVO[]>([])
const modalVisible = ref(false)
const isEdit = ref(false)

const searchParams = reactive<API.PromptTemplateQueryRequest>({
  pageNum: 1,
  pageSize: 10,
  templateName: '',
  matchIdentity: undefined,
  matchIndustry: '',
  isEnabled: undefined
})

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`
})

const formData = reactive({
  id: undefined as number | undefined,
  templateName: '',
  matchIdentity: undefined as string | undefined,
  matchIndustry: '',
  description: '',
  promptContent: '',
  templateFields: '',
  sortOrder: 0,
  isEnabled: 1
})

const currentFields = ref<API.TemplateField[]>([])

const handleFieldsUpdated = (fields: API.TemplateField[]) => {
  currentFields.value = fields
}

const formEnabled = computed({
  get: () => formData.isEnabled === 1,
  set: (val: boolean) => {
    formData.isEnabled = val ? 1 : 0
  }
})

const industryAutoOptions = industryOptions.map(item => ({ value: item }))

const filterIndustry = (input: string, option: { value: string }) => {
  return option.value.toLowerCase().includes(input.toLowerCase())
}

const columns = [
  { title: 'ID', dataIndex: 'id', width: 80, ellipsis: true },
  { title: '模板名称', dataIndex: 'templateName', width: 160, ellipsis: true },
  { title: '匹配身份', dataIndex: 'matchIdentity', width: 100 },
  { title: '匹配行业', dataIndex: 'matchIndustry', width: 120 },
  { title: '排序', dataIndex: 'sortOrder', width: 70 },
  { title: '状态', dataIndex: 'isEnabled', width: 80 },
  { title: '操作', dataIndex: 'action', width: 120, fixed: 'right' }
]

const fetchData = async () => {
  loading.value = true
  try {
    const res = await listPromptTemplateByPage({
      ...searchParams,
      pageNum: pagination.current,
      pageSize: pagination.pageSize
    })
    if (res.data.code === 0 && res.data.data) {
      dataList.value = res.data.data.records || []
      pagination.total = res.data.data.totalRow || 0
    }
  } catch (e) {
    console.error('加载模板列表失败', e)
  } finally {
    loading.value = false
  }
}

const doSearch = () => {
  pagination.current = 1
  fetchData()
}

const resetSearch = () => {
  searchParams.templateName = ''
  searchParams.matchIdentity = undefined
  searchParams.matchIndustry = ''
  searchParams.isEnabled = undefined
  doSearch()
}

const handleTableChange = (pag: any) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  fetchData()
}

const resetForm = () => {
  formData.id = undefined
  formData.templateName = ''
  formData.matchIdentity = undefined
  formData.matchIndustry = ''
  formData.description = ''
  formData.promptContent = ''
  formData.templateFields = ''
  formData.sortOrder = 0
  formData.isEnabled = 1
  currentFields.value = []
}

const showAddModal = () => {
  isEdit.value = false
  resetForm()
  modalVisible.value = true
}

const showEditModal = (record: API.PromptTemplateVO) => {
  isEdit.value = true
  formData.id = record.id
  formData.templateName = record.templateName || ''
  formData.matchIdentity = record.matchIdentity || undefined
  formData.matchIndustry = record.matchIndustry || ''
  formData.description = record.description || ''
  formData.promptContent = record.promptContent || ''
  formData.templateFields = record.templateFields || ''
  formData.sortOrder = record.sortOrder || 0
  formData.isEnabled = record.isEnabled ?? 1

  // Parse fields for editor
  try {
    if (record.templateFields) {
      currentFields.value = JSON.parse(record.templateFields)
    } else {
      currentFields.value = []
    }
  } catch {
    currentFields.value = []
  }

  modalVisible.value = true
}

const handleSubmit = async () => {
  if (!formData.templateName || !formData.promptContent) {
    message.warning('模板名称和提示词内容不能为空')
    return
  }

  // Validate templateFields JSON
  if (formData.templateFields) {
    try {
      JSON.parse(formData.templateFields)
    } catch {
      message.error('字段定义JSON格式不正确')
      return
    }
  }

  try {
    if (isEdit.value && formData.id) {
      const res = await updatePromptTemplate({
        id: formData.id,
        templateName: formData.templateName,
        matchIdentity: formData.matchIdentity || undefined,
        matchIndustry: formData.matchIndustry || undefined,
        description: formData.description,
        promptContent: formData.promptContent,
        templateFields: formData.templateFields,
        sortOrder: formData.sortOrder,
        isEnabled: formData.isEnabled
      })
      if (res.data.code === 0) {
        message.success('更新成功')
        modalVisible.value = false
        fetchData()
      } else {
        message.error('更新失败：' + res.data.message)
      }
    } else {
      const res = await addPromptTemplate({
        templateName: formData.templateName,
        matchIdentity: formData.matchIdentity || undefined,
        matchIndustry: formData.matchIndustry || undefined,
        description: formData.description,
        promptContent: formData.promptContent,
        templateFields: formData.templateFields,
        sortOrder: formData.sortOrder,
        isEnabled: formData.isEnabled
      })
      if (res.data.code === 0) {
        message.success('新增成功')
        modalVisible.value = false
        fetchData()
      } else {
        message.error('新增失败：' + res.data.message)
      }
    }
  } catch (e: any) {
    message.error('操作失败：' + (e.message || '请重试'))
  }
}

const handleDelete = async (id: number) => {
  try {
    const res = await deletePromptTemplate({ id })
    if (res.data.code === 0) {
      message.success('删除成功')
      fetchData()
    } else {
      message.error('删除失败：' + res.data.message)
    }
  } catch (e: any) {
    message.error('删除失败：' + (e.message || '请重试'))
  }
}

onMounted(() => {
  fetchData()
})
</script>

<style lang="less" scoped>
#promptTemplateManagePage {
  padding: 32px;
  width: 100%;
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

.search-panel {
  margin-bottom: 20px;
  border-radius: 12px;
  border: 1px solid #f0f0f0;

  h2 {
    font-size: 16px;
    font-weight: 600;
    color: #1a1a1a;
    margin-bottom: 16px;
    padding-bottom: 12px;
    border-bottom: 1px solid #f0f0f0;
  }
}

.search-item {
  margin-bottom: 10px;
}

.search-actions {
  margin-bottom: 10px;

  :deep(.ant-btn) {
    border-radius: 8px;

    &.ant-btn-primary {
      background: #1a1a1a;
      border: none;
    }
  }
}

.action-bar {
  margin-bottom: 16px;

  :deep(.ant-btn-primary) {
    background: #1a1a1a;
    border: none;
    border-radius: 8px;
    font-weight: 500;

    &:hover {
      background: #333;
    }
  }
}

.text-muted {
  color: #bbb;
  font-size: 13px;
}

.danger-link {
  color: #ff4d4f;
}

:deep(.ant-table) {
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid #f0f0f0;
}

:deep(.ant-modal-content) {
  border-radius: 12px;
}

:deep(.ant-modal-header) {
  border-radius: 12px 12px 0 0;

  .ant-modal-title {
    font-weight: 600;
  }
}

:deep(.ant-modal-footer .ant-btn-primary) {
  background: #1a1a1a;
  border: none;

  &:hover {
    background: #333;
  }
}
</style>
