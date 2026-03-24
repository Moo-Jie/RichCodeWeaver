<template>
  <div id="systemPromptManagePage">
    <div class="page-header">
      <h1>系统提示词管理</h1>
      <p>管理 AI 系统提示词文件，支持 Markdown 格式编辑与实时预览</p>
    </div>

    <!-- 搜索面板 -->
    <a-card class="search-panel">
      <h2>筛选提示词</h2>
      <a-form :model="searchParams" layout="inline" @finish="doSearch">
        <a-form-item class="search-item" label="提示词名称">
          <a-input
            v-model:value="searchParams.promptName"
            allow-clear
            placeholder="输入名称关键词"
          />
        </a-form-item>
        <a-form-item class="search-item" label="文件路径">
          <a-input
            v-model:value="searchParams.filePath"
            allow-clear
            placeholder="输入文件路径"
          />
        </a-form-item>
        <a-form-item class="search-actions">
          <a-button html-type="submit" type="primary">
            <template #icon><SearchOutlined /></template>
            搜索
          </a-button>
          <a-button @click="resetSearch">重置</a-button>
        </a-form-item>
      </a-form>
    </a-card>

    <!-- 操作栏 -->
    <div class="action-bar">
      <a-button type="primary" @click="showAddModal">
        <template #icon><PlusOutlined /></template>
        新增提示词
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
        <template v-if="column.dataIndex === 'filePath'">
          <a-tooltip :title="record.filePath">
            <code class="file-path-cell">{{ record.filePath }}</code>
          </a-tooltip>
        </template>
        <template v-if="column.dataIndex === 'description'">
          <span class="desc-cell">{{ record.description || '-' }}</span>
        </template>
        <template v-if="column.dataIndex === 'action'">
          <a-space>
            <a @click="openEditor(record)">编辑内容</a>
            <a @click="showEditModal(record)">修改信息</a>
            <a-popconfirm title="确定删除该提示词记录？（不会删除文件）" @confirm="handleDelete(record.id)">
              <a class="danger-link">删除</a>
            </a-popconfirm>
          </a-space>
        </template>
      </template>
    </a-table>

    <!-- 新增/编辑元数据模态框 -->
    <a-modal
      v-model:open="metaModalVisible"
      :title="isEdit ? '修改提示词信息' : '新增提示词'"
      width="560px"
      :ok-text="'提交'"
      :cancel-text="'取消'"
      @ok="handleMetaSubmit"
    >
      <a-form :model="metaForm" :label-col="{ span: 5 }" :wrapper-col="{ span: 19 }" style="margin-top: 16px;">
        <a-form-item label="提示词名称" required>
          <a-input v-model:value="metaForm.promptName" placeholder="如：HTML代码生成" />
        </a-form-item>
        <a-form-item label="文件路径" required>
          <a-input v-model:value="metaForm.filePath" placeholder="如：/aiPrompt/html-system-prompt.txt" :disabled="isEdit" />
          <div v-if="!isEdit" class="form-tip">请先手动创建文件，再填入相对于 resources 的路径</div>
        </a-form-item>
        <a-form-item label="描述">
          <a-textarea v-model:value="metaForm.description" :rows="3" placeholder="提示词的简短描述" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 文件内容编辑模态框（全屏 Markdown 编辑器） -->
    <a-modal
      v-model:open="editorModalVisible"
      :title="'编辑提示词：' + currentEditPrompt?.promptName"
      width="95vw"
      :footer="null"
      :body-style="{ padding: '0', height: 'calc(90vh - 55px)', overflow: 'hidden' }"
      centered
      destroy-on-close
    >
      <div class="editor-wrapper">
        <div class="editor-toolbar">
          <div class="editor-info">
            <code>{{ currentEditPrompt?.filePath }}</code>
          </div>
          <div class="editor-actions">
            <a-button :loading="saving" type="primary" @click="handleSaveContent">
              <template #icon><SaveOutlined /></template>
              保存
            </a-button>
          </div>
        </div>
        <div class="editor-container">
          <MdEditor
            v-model="editorContent"
            :theme="'light'"
            :language="'zh-CN'"
            :show-code-row-number="true"
            :preview="true"
            style="height: 100%"
          />
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script lang="ts" setup>
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, SearchOutlined, SaveOutlined } from '@ant-design/icons-vue'
import { MdEditor } from 'md-editor-v3'
import 'md-editor-v3/lib/style.css'
import {
  addSystemPrompt,
  deleteSystemPrompt,
  listSystemPromptByPage,
  readSystemPromptFileContent,
  updateSystemPrompt,
  writeSystemPromptFileContent
} from '@/api/systemPromptController'

const loading = ref(false)
const saving = ref(false)
const dataList = ref<API.SystemPromptVO[]>([])
const metaModalVisible = ref(false)
const editorModalVisible = ref(false)
const isEdit = ref(false)
const currentEditPrompt = ref<API.SystemPromptVO | null>(null)
const editorContent = ref('')

const searchParams = reactive<API.SystemPromptQueryRequest>({
  pageNum: 1,
  pageSize: 10,
  promptName: '',
  filePath: ''
})

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`
})

const metaForm = reactive({
  id: undefined as number | undefined,
  promptName: '',
  filePath: '',
  description: ''
})

const columns = [
  { title: 'ID', dataIndex: 'id', width: 80, ellipsis: true },
  { title: '提示词名称', dataIndex: 'promptName', width: 160 },
  { title: '文件路径', dataIndex: 'filePath', width: 280, ellipsis: true },
  { title: '描述', dataIndex: 'description', width: 200, ellipsis: true },
  { title: '更新时间', dataIndex: 'updateTime', width: 170 },
  { title: '操作', dataIndex: 'action', width: 200, fixed: 'right' as const }
]

const fetchData = async () => {
  loading.value = true
  try {
    const res = await listSystemPromptByPage({
      ...searchParams,
      pageNum: pagination.current,
      pageSize: pagination.pageSize
    })
    if (res.data.code === 0 && res.data.data) {
      dataList.value = res.data.data.records || []
      pagination.total = res.data.data.totalRow || 0
    }
  } catch (e) {
    console.error('加载系统提示词列表失败', e)
  } finally {
    loading.value = false
  }
}

const doSearch = () => {
  pagination.current = 1
  fetchData()
}

const resetSearch = () => {
  searchParams.promptName = ''
  searchParams.filePath = ''
  doSearch()
}

const handleTableChange = (pag: any) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  fetchData()
}

// ===== 元数据模态框 =====
const resetMetaForm = () => {
  metaForm.id = undefined
  metaForm.promptName = ''
  metaForm.filePath = ''
  metaForm.description = ''
}

const showAddModal = () => {
  isEdit.value = false
  resetMetaForm()
  metaModalVisible.value = true
}

const showEditModal = (record: API.SystemPromptVO) => {
  isEdit.value = true
  metaForm.id = record.id
  metaForm.promptName = record.promptName || ''
  metaForm.filePath = record.filePath || ''
  metaForm.description = record.description || ''
  metaModalVisible.value = true
}

const handleMetaSubmit = async () => {
  if (!metaForm.promptName) {
    message.warning('提示词名称不能为空')
    return
  }
  if (!metaForm.filePath) {
    message.warning('文件路径不能为空')
    return
  }

  try {
    if (isEdit.value && metaForm.id) {
      const res = await updateSystemPrompt({
        id: metaForm.id,
        promptName: metaForm.promptName,
        filePath: metaForm.filePath,
        description: metaForm.description
      })
      if (res.data.code === 0) {
        message.success('更新成功')
        metaModalVisible.value = false
        fetchData()
      } else {
        message.error('更新失败：' + res.data.message)
      }
    } else {
      const res = await addSystemPrompt({
        promptName: metaForm.promptName,
        filePath: metaForm.filePath,
        description: metaForm.description
      })
      if (res.data.code === 0) {
        message.success('新增成功')
        metaModalVisible.value = false
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
    const res = await deleteSystemPrompt({ id })
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

// ===== 文件内容编辑器 =====
const openEditor = async (record: API.SystemPromptVO) => {
  currentEditPrompt.value = record
  editorContent.value = ''
  editorModalVisible.value = true

  try {
    const res = await readSystemPromptFileContent({ id: record.id! })
    if (res.data.code === 0) {
      editorContent.value = res.data.data || ''
    } else {
      message.error('读取文件内容失败：' + res.data.message)
    }
  } catch (e: any) {
    message.error('读取文件内容失败：' + (e.message || '请重试'))
  }
}

const handleSaveContent = async () => {
  if (!currentEditPrompt.value?.id) return

  saving.value = true
  try {
    const res = await writeSystemPromptFileContent({
      id: currentEditPrompt.value.id,
      content: editorContent.value
    })
    if (res.data.code === 0) {
      message.success('保存成功')
    } else {
      message.error('保存失败：' + res.data.message)
    }
  } catch (e: any) {
    message.error('保存失败：' + (e.message || '请重试'))
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  fetchData()
})
</script>

<style lang="less" scoped>
#systemPromptManagePage {
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

.file-path-cell {
  font-size: 12px;
  background: #f5f5f5;
  padding: 2px 6px;
  border-radius: 4px;
  color: #666;
}

.desc-cell {
  color: #666;
  font-size: 13px;
}

.danger-link {
  color: #ff4d4f;
}

.form-tip {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

// 编辑器相关
.editor-wrapper {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.editor-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  border-bottom: 1px solid #f0f0f0;
  background: #fafafa;
  flex-shrink: 0;

  .editor-info code {
    font-size: 13px;
    color: #666;
    background: #f0f0f0;
    padding: 2px 8px;
    border-radius: 4px;
  }

  .editor-actions {
    :deep(.ant-btn-primary) {
      background: #1a1a1a;
      border: none;
      border-radius: 8px;

      &:hover {
        background: #333;
      }
    }
  }
}

.editor-container {
  flex: 1;
  overflow: hidden;

  :deep(.md-editor) {
    height: 100% !important;
    border: none;
    border-radius: 0;
  }
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
