<template>
  <div id="ragManagePage">
    <div class="page-header">
      <h1>RAG 知识库管理</h1>
      <p>管理 AI 代码生成时参考的知识库文档，支持 Markdown 格式编辑，修改后需刷新向量数据库</p>
    </div>

    <!-- 搜索面板 -->
    <a-card class="search-panel">
      <h2>筛选文档</h2>
      <a-form :model="searchParams" layout="inline" @finish="doSearch">
        <a-form-item class="search-item" label="文档标题">
          <a-input v-model:value="searchParams.docTitle" allow-clear placeholder="输入标题关键词" />
        </a-form-item>
        <a-form-item class="search-item" label="适用类型">
          <a-select v-model:value="searchParams.codeGenType" placeholder="全部类型" style="width: 280px" allow-clear>
            <a-select-option v-for="item in codeGenTypeOptions" :key="item.value" :value="item.value">
              {{ item.label }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item class="search-item" label="状态">
          <a-select v-model:value="searchParams.isEnabled" placeholder="全部" style="width: 100px" allow-clear>
            <a-select-option :value="1">启用</a-select-option>
            <a-select-option :value="0">禁用</a-select-option>
          </a-select>
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
        新增文档
      </a-button>
      <a-button class="reindex-btn" @click="openReindexModal">
        <template #icon><SyncOutlined /></template>
        刷新向量数据库
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
        <template v-if="column.dataIndex === 'codeGenType'">
          <a-tag :color="codeGenTypeColorMap[record.codeGenType] || 'default'">
            {{ codeGenTypeLabelMap[record.codeGenType] || record.codeGenType }}
          </a-tag>
        </template>
        <template v-if="column.dataIndex === 'docContent'">
          <span class="text-muted">{{ (record.docContent || '').length }} 字符</span>
        </template>
        <template v-if="column.dataIndex === 'isEnabled'">
          <a-tag :color="record.isEnabled === 1 ? 'green' : 'red'">
            {{ record.isEnabled === 1 ? '启用' : '禁用' }}
          </a-tag>
        </template>
        <template v-if="column.dataIndex === 'action'">
          <a-space>
            <a @click="showEditModal(record)">编辑信息</a>
            <a @click="openEditor(record)">编辑内容</a>
            <a-popconfirm title="确定删除该文档？删除后需重新刷新向量数据库才能生效。" @confirm="handleDelete(record.id)">
              <a class="danger-link">删除</a>
            </a-popconfirm>
          </a-space>
        </template>
      </template>
    </a-table>

    <!-- RAG 参数配置 -->
    <a-card class="params-panel" :loading="paramsLoading">
      <div class="params-header">
        <h2>RAG 参数配置</h2>
        <p class="params-sub">动态调整向量化、检索和注入各阶段参数，修改后立即生效（无需重新索引）</p>
      </div>

      <div v-if="paramGroups.length > 0">
        <div v-for="group in paramGroups" :key="group.key" class="param-group">
          <div class="param-group-title">
            <a-tag :color="group.color">{{ group.label }}</a-tag>
            <span class="param-group-desc">{{ group.desc }}</span>
          </div>
          <div v-for="param in group.params" :key="param.id" class="param-row">
            <div class="param-meta">
              <span class="param-name">{{ param.paramName }}</span>
              <span class="param-desc">{{ param.description }}</span>
            </div>
            <div class="param-input-area">
              <a-input-number
                v-if="param.paramType === 'int'"
                v-model:value="paramDrafts[param.paramKey || '']"
                :min="1"
                :precision="0"
                style="width: 120px"
              />
              <a-input-number
                v-else-if="param.paramType === 'double'"
                v-model:value="paramDrafts[param.paramKey || '']"
                :min="0"
                :max="1"
                :step="0.05"
                :precision="2"
                style="width: 120px"
              />
              <a-textarea
                v-else-if="param.paramType === 'textarea'"
                v-model:value="paramDrafts[param.paramKey || '']"
                :rows="5"
                :style="{ width: '100%', fontFamily: 'monospace', fontSize: '12px' }"
                placeholder="模板需包含 {{userMessage}} 和 {{contents}} 占位符"
              />
              <a-button
                type="primary"
                size="small"
                class="param-save-btn"
                :loading="paramSaving[param.paramKey || '']"
                @click="handleSaveParam(param)"
              >
                保存
              </a-button>
            </div>
          </div>
        </div>
      </div>
      <a-empty v-else description="暂无参数配置（请检查 rag_param 表是否已初始化）" />
    </a-card>

    <!-- 新增/编辑元数据模态框 -->
    <a-modal
      v-model:open="metaModalVisible"
      :title="isEdit ? '编辑文档信息' : '新增知识库文档'"
      width="560px"
      ok-text="提交"
      cancel-text="取消"
      @ok="handleMetaSubmit"
    >
      <a-form :model="metaForm" :label-col="{ span: 5 }" :wrapper-col="{ span: 19 }" style="margin-top: 16px;">
        <a-form-item label="文档标题" required>
          <a-input v-model:value="metaForm.docTitle" placeholder="请输入文档标题" />
        </a-form-item>
        <a-form-item label="适用类型" required>
          <a-select v-model:value="metaForm.codeGenType" placeholder="请选择适用的代码生成类型" style="width: 280px">
            <a-select-option v-for="item in codeGenTypeOptions" :key="item.value" :value="item.value">
              {{ item.label }}
            </a-select-option>
          </a-select>
          <div class="form-hint">GENERAL 表示所有代码生成类型均使用该文档</div>
        </a-form-item>
        <a-form-item label="文档描述">
          <a-textarea v-model:value="metaForm.description" :rows="2" placeholder="简短描述文档用途" />
        </a-form-item>
        <a-form-item label="排序权重">
          <a-input-number v-model:value="metaForm.sortOrder" :min="0" :max="9999" />
          <span class="form-hint" style="margin-left: 8px;">数字越小越靠前</span>
        </a-form-item>
        <a-form-item label="是否启用">
          <a-switch v-model:checked="metaFormEnabled" />
          <span class="form-hint" style="margin-left: 8px;">禁用的文档不会被摄入向量库</span>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 文档内容编辑器（全屏 Markdown 编辑器） -->
    <a-modal
      v-model:open="editorModalVisible"
      :title="'编辑内容：' + (currentEditDoc?.docTitle || '')"
      width="95vw"
      :footer="null"
      :body-style="{ padding: '0', height: 'calc(90vh - 55px)', overflow: 'hidden' }"
      centered
      destroy-on-close
    >
      <div class="editor-wrapper">
        <div class="editor-toolbar">
          <div class="editor-info">
            <a-tag :color="codeGenTypeColorMap[currentEditDoc?.codeGenType || ''] || 'default'" size="small">
              {{ codeGenTypeLabelMap[currentEditDoc?.codeGenType || ''] || currentEditDoc?.codeGenType }}
            </a-tag>
            <span class="editor-char-count">{{ editorContent.length }} 字符</span>
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

    <!-- 刷新向量数据库进度模态框 -->
    <a-modal
      v-model:open="reindexModalVisible"
      :title="reindexModalTitle"
      width="480px"
      :footer="null"
      :closable="reindexState !== 'progress'"
      :mask-closable="reindexState !== 'progress'"
      centered
    >
      <!-- 确认阶段 -->
      <div v-if="reindexState === 'confirm'" class="reindex-confirm">
        <div class="reindex-confirm-icon">
          <SyncOutlined />
        </div>
        <p class="reindex-confirm-text">
          此操作将<strong>清空现有向量数据</strong>并重新从数据库读取所有已启用的知识库文档，向量化后写入 PGVector。
        </p>
        <p class="reindex-confirm-sub">向量化过程耗时约 3-10 秒，期间请勿关闭页面。</p>
        <div class="reindex-confirm-btns">
          <a-button @click="reindexModalVisible = false">取消</a-button>
          <a-button type="primary" class="confirm-primary-btn" @click="startReindex">确认执行</a-button>
        </div>
      </div>

      <!-- 进度 / 完成 / 失败阶段 -->
      <div v-else class="reindex-progress">
        <div class="progress-steps">
          <div
            v-for="step in reindexSteps"
            :key="step.id"
            class="progress-step"
            :class="step.status"
          >
            <div class="step-icon">
              <LoadingOutlined v-if="step.status === 'loading'" class="icon-loading" />
              <CheckCircleFilled v-else-if="step.status === 'done'" class="icon-done" />
              <CloseCircleFilled v-else-if="step.status === 'error'" class="icon-error" />
              <span v-else class="icon-pending" />
            </div>
            <div class="step-body">
              <span class="step-label">{{ step.label }}</span>
              <span v-if="step.duration" class="step-duration">{{ step.duration }}</span>
            </div>
          </div>
        </div>

        <div v-if="reindexState === 'done'" class="reindex-result success">
          <CheckCircleFilled class="result-icon" />
          <span>重新索引完成，所有已启用文档已同步至向量库</span>
        </div>
        <div v-if="reindexState === 'error'" class="reindex-result error">
          <CloseCircleFilled class="result-icon" />
          <span>{{ reindexErrorMsg }}</span>
        </div>

        <div v-if="reindexState === 'done' || reindexState === 'error'" class="reindex-done-btn">
          <a-button type="primary" class="confirm-primary-btn" @click="reindexModalVisible = false">关闭</a-button>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script lang="ts" setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import {
  CheckCircleFilled,
  CloseCircleFilled,
  LoadingOutlined,
  PlusOutlined,
  SaveOutlined,
  SearchOutlined,
  SyncOutlined
} from '@ant-design/icons-vue'
import { MdEditor } from 'md-editor-v3'
import 'md-editor-v3/lib/style.css'
import {
  addRagDocument,
  deleteRagDocument,
  listRagDocumentByPage,
  reindexRagDocuments,
  updateRagDocument
} from '@/api/ragDocumentController'
import { listRagParams, updateRagParam } from '@/api/ragParamController'

// ── 通用状态 ──────────────────────────────────────────────
const loading = ref(false)
const dataList = ref<API.RagDocumentVO[]>([])
const isEdit = ref(false)

const codeGenTypeOptions = [
  { value: 'HTML', label: 'HTML — 单文件模式' },
  { value: 'MULTI_FILE', label: 'MULTI_FILE — 多文件模式' },
  { value: 'VUE_PROJECT', label: 'VUE_PROJECT — Vue 项目模式' },
  { value: 'GENERAL', label: 'GENERAL — 通用（所有类型）' }
]

const codeGenTypeLabelMap: Record<string, string> = {
  HTML: '单文件',
  MULTI_FILE: '多文件',
  VUE_PROJECT: 'Vue项目',
  GENERAL: '通用'
}

const codeGenTypeColorMap: Record<string, string> = {
  HTML: 'blue',
  MULTI_FILE: 'purple',
  VUE_PROJECT: 'cyan',
  GENERAL: 'orange'
}

// ── 搜索 & 分页 ───────────────────────────────────────────
const searchParams = reactive<API.RagDocumentQueryRequest>({
  pageNum: 1,
  pageSize: 10,
  docTitle: '',
  codeGenType: undefined,
  isEnabled: undefined
})

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`
})

// ── 表格列定义 ────────────────────────────────────────────
const columns = [
  { title: 'ID', dataIndex: 'id', width: 80, ellipsis: true },
  { title: '文档标题', dataIndex: 'docTitle', width: 200, ellipsis: true },
  { title: '适用类型', dataIndex: 'codeGenType', width: 120 },
  { title: '内容长度', dataIndex: 'docContent', width: 100 },
  { title: '描述', dataIndex: 'description', width: 180, ellipsis: true },
  { title: '排序', dataIndex: 'sortOrder', width: 70 },
  { title: '状态', dataIndex: 'isEnabled', width: 80 },
  { title: '操作', dataIndex: 'action', width: 180, fixed: 'right' }
]

// ── 元数据模态框 ──────────────────────────────────────────
const metaModalVisible = ref(false)

const metaForm = reactive({
  id: undefined as number | undefined,
  docTitle: '',
  codeGenType: 'GENERAL' as string,
  description: '',
  sortOrder: 0,
  isEnabled: 1
})

const metaFormEnabled = computed({
  get: () => metaForm.isEnabled === 1,
  set: (val: boolean) => { metaForm.isEnabled = val ? 1 : 0 }
})

const resetMetaForm = () => {
  metaForm.id = undefined
  metaForm.docTitle = ''
  metaForm.codeGenType = 'GENERAL'
  metaForm.description = ''
  metaForm.sortOrder = 0
  metaForm.isEnabled = 1
}

const showAddModal = () => {
  isEdit.value = false
  resetMetaForm()
  metaModalVisible.value = true
}

const showEditModal = (record: API.RagDocumentVO) => {
  isEdit.value = true
  metaForm.id = record.id
  metaForm.docTitle = record.docTitle || ''
  metaForm.codeGenType = record.codeGenType || 'GENERAL'
  metaForm.description = record.description || ''
  metaForm.sortOrder = record.sortOrder ?? 0
  metaForm.isEnabled = record.isEnabled ?? 1
  metaModalVisible.value = true
}

const handleMetaSubmit = async () => {
  if (!metaForm.docTitle.trim()) {
    message.warning('文档标题不能为空')
    return
  }
  if (!metaForm.codeGenType) {
    message.warning('请选择适用类型')
    return
  }

  try {
    if (isEdit.value && metaForm.id) {
      const res = await updateRagDocument({
        id: metaForm.id,
        docTitle: metaForm.docTitle,
        codeGenType: metaForm.codeGenType,
        description: metaForm.description,
        sortOrder: metaForm.sortOrder,
        isEnabled: metaForm.isEnabled
      })
      if (res.data.code === 0) {
        message.success('信息更新成功，请点击"刷新向量数据库"使改动生效')
        metaModalVisible.value = false
        fetchData()
      } else {
        message.error('更新失败：' + res.data.message)
      }
    } else {
      const res = await addRagDocument({
        docTitle: metaForm.docTitle,
        docContent: '',
        codeGenType: metaForm.codeGenType,
        description: metaForm.description,
        sortOrder: metaForm.sortOrder,
        isEnabled: metaForm.isEnabled
      })
      if (res.data.code === 0) {
        message.success('新增成功，请在"编辑内容"中补充文档内容后刷新向量数据库')
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

// ── 内容编辑器模态框 ──────────────────────────────────────
const editorModalVisible = ref(false)
const currentEditDoc = ref<API.RagDocumentVO | null>(null)
const editorContent = ref('')
const saving = ref(false)

const openEditor = (record: API.RagDocumentVO) => {
  currentEditDoc.value = record
  editorContent.value = record.docContent || ''
  editorModalVisible.value = true
}

const handleSaveContent = async () => {
  if (!currentEditDoc.value?.id) return
  saving.value = true
  try {
    const res = await updateRagDocument({
      id: currentEditDoc.value.id,
      docContent: editorContent.value
    })
    if (res.data.code === 0) {
      message.success('内容保存成功，请点击"刷新向量数据库"使改动生效')
      editorModalVisible.value = false
      fetchData()
    } else {
      message.error('保存失败：' + res.data.message)
    }
  } catch (e: any) {
    message.error('保存失败：' + (e.message || '请重试'))
  } finally {
    saving.value = false
  }
}

// ── 刷新向量数据库进度模态框 ──────────────────────────────
const reindexModalVisible = ref(false)
const reindexState = ref<'confirm' | 'progress' | 'done' | 'error'>('confirm')
const reindexErrorMsg = ref('')

interface ReindexStep {
  id: string
  label: string
  status: 'pending' | 'loading' | 'done' | 'error'
  duration?: string
}

const reindexSteps = ref<ReindexStep[]>([
  { id: 'clear', label: '清空现有向量数据', status: 'pending' },
  { id: 'load', label: '从数据库读取已启用文档', status: 'pending' },
  { id: 'vector', label: '向量化文档内容', status: 'pending' },
  { id: 'write', label: '写入 PGVector 向量库', status: 'pending' }
])

const reindexModalTitle = computed(() => {
  if (reindexState.value === 'confirm') return '刷新向量数据库'
  if (reindexState.value === 'progress') return '正在重建索引…'
  if (reindexState.value === 'done') return '重建索引完成'
  return '重建索引失败'
})

const openReindexModal = () => {
  reindexState.value = 'confirm'
  reindexSteps.value.forEach(s => {
    s.status = 'pending'
    s.duration = undefined
  })
  reindexModalVisible.value = true
}

const startReindex = async () => {
  reindexState.value = 'progress'
  reindexErrorMsg.value = ''

  const steps = reindexSteps.value
  const delay = (ms: number) => new Promise(resolve => setTimeout(resolve, ms))

  const setStep = (idx: number, status: ReindexStep['status'], duration?: string) => {
    steps[idx].status = status
    if (duration) steps[idx].duration = duration
  }

  const startTimes: number[] = []

  for (let i = 0; i < steps.length; i++) {
    startTimes[i] = Date.now()
    setStep(i, 'loading')
    if (i > 0) setStep(i - 1, 'done', `${((Date.now() - startTimes[i - 1]) / 1000).toFixed(1)}s`)
    await delay(400)
  }

  try {
    const apiStart = Date.now()
    const res = await reindexRagDocuments()
    const elapsed = ((Date.now() - apiStart) / 1000).toFixed(1)

    if (res.data.code === 0) {
      setStep(steps.length - 1, 'done', `${elapsed}s`)
      reindexState.value = 'done'
    } else {
      setStep(steps.length - 1, 'error')
      reindexErrorMsg.value = '索引失败：' + (res.data.message || '未知错误')
      reindexState.value = 'error'
    }
  } catch (e: any) {
    setStep(steps.length - 1, 'error')
    reindexErrorMsg.value = '索引失败：' + (e.message || '请重试')
    reindexState.value = 'error'
  }
}

// ── 数据操作 ──────────────────────────────────────────────
const fetchData = async () => {
  loading.value = true
  try {
    const res = await listRagDocumentByPage({
      ...searchParams,
      pageNum: pagination.current,
      pageSize: pagination.pageSize
    })
    if (res.data.code === 0 && res.data.data) {
      dataList.value = res.data.data.records || []
      pagination.total = res.data.data.totalRow || 0
    }
  } catch (e) {
    console.error('加载文档列表失败', e)
  } finally {
    loading.value = false
  }
}

const doSearch = () => {
  pagination.current = 1
  fetchData()
}

const resetSearch = () => {
  searchParams.docTitle = ''
  searchParams.codeGenType = undefined
  searchParams.isEnabled = undefined
  doSearch()
}

const handleTableChange = (pag: any) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  fetchData()
}

const handleDelete = async (id: number) => {
  try {
    const res = await deleteRagDocument({ id })
    if (res.data.code === 0) {
      message.success('删除成功，请点击"刷新向量数据库"使改动生效')
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
  fetchParams()
})

// ── RAG 参数配置 ──────────────────────────────────────────
const paramsLoading = ref(false)
const rawParams = ref<API.RagParamVO[]>([])
const paramDrafts = reactive<Record<string, any>>({})
const paramSaving = reactive<Record<string, boolean>>({})

const groupMeta: Record<string, { label: string; color: string; desc: string }> = {
  etl: { label: 'ETL 切分', color: 'blue', desc: '文档收集与切片阶段参数（修改后需重新刷新向量数据库才能生效）' },
  retrieval: { label: '向量检索', color: 'purple', desc: '向量搜索阶段参数（实时生效，无需重新索引）' },
  injection: { label: '提示词注入', color: 'cyan', desc: 'RAG 内容注入到提示词的模板（实时生效，无需重新索引）' }
}

const paramGroups = computed(() => {
  const groups: { key: string; label: string; color: string; desc: string; params: API.RagParamVO[] }[] = []
  const order = ['etl', 'retrieval', 'injection']
  for (const key of order) {
    const params = rawParams.value.filter(p => p.paramGroup === key)
    if (params.length > 0) {
      groups.push({ key, ...groupMeta[key], params })
    }
  }
  return groups
})

const fetchParams = async () => {
  paramsLoading.value = true
  try {
    const res = await listRagParams()
    if (res.data.code === 0 && res.data.data) {
      rawParams.value = res.data.data
      res.data.data.forEach(p => {
        if (p.paramKey) {
          paramDrafts[p.paramKey] = p.paramType === 'int'
            ? Number(p.paramValue)
            : p.paramType === 'double'
              ? Number(p.paramValue)
              : p.paramValue
        }
      })
    }
  } catch (e) {
    console.error('加载 RAG 参数失败', e)
  } finally {
    paramsLoading.value = false
  }
}

const handleSaveParam = async (param: API.RagParamVO) => {
  const key = param.paramKey
  if (!key) return
  paramSaving[key] = true
  try {
    const newVal = String(paramDrafts[key])
    const res = await updateRagParam({ id: param.id, paramValue: newVal })
    if (res.data.code === 0) {
      message.success(`「${param.paramName}」已保存`)
      const target = rawParams.value.find(p => p.paramKey === key)
      if (target) target.paramValue = newVal
    } else {
      message.error('保存失败：' + res.data.message)
    }
  } catch (e: any) {
    message.error('保存失败：' + (e.message || '请重试'))
  } finally {
    paramSaving[key] = false
  }
}
</script>

<style lang="less" scoped>
#ragManagePage {
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
  display: flex;
  gap: 12px;

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

.reindex-btn {
  border-radius: 8px;
  font-weight: 500;
  border-color: #1a1a1a;
  color: #1a1a1a;

  &:hover {
    border-color: #333;
    color: #333;
  }
}

.form-hint {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

.char-warn {
  color: #faad14;
  font-weight: 500;
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

// ── 内容编辑器模态框 ──────────────────────────────────────
.editor-wrapper {
  display: flex;
  flex-direction: column;
  height: calc(90vh - 55px);
}

.editor-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 16px;
  border-bottom: 1px solid #f0f0f0;
  flex-shrink: 0;
}

.editor-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.editor-char-count {
  font-size: 12px;
  color: #999;
}

.editor-actions {
  display: flex;
  gap: 8px;

  :deep(.ant-btn-primary) {
    background: #1a1a1a;
    border: none;
    border-radius: 8px;

    &:hover {
      background: #333;
    }
  }
}

.editor-container {
  flex: 1;
  overflow: hidden;
}

// ── 刷新向量数据库进度模态框 ──────────────────────────────
.reindex-confirm {
  padding: 8px 0 16px;
  text-align: center;
}

.reindex-confirm-icon {
  font-size: 40px;
  color: #1a1a1a;
  margin-bottom: 16px;
}

.reindex-confirm-text {
  font-size: 14px;
  color: #333;
  margin-bottom: 8px;

  strong {
    color: #ff4d4f;
  }
}

.reindex-confirm-sub {
  font-size: 12px;
  color: #999;
  margin-bottom: 24px;
}

.reindex-confirm-btns {
  display: flex;
  justify-content: center;
  gap: 12px;

  :deep(.ant-btn) {
    border-radius: 8px;
    min-width: 100px;
  }
}

.confirm-primary-btn {
  background: #1a1a1a !important;
  border-color: #1a1a1a !important;
  color: #fff !important;
  border-radius: 8px;

  &:hover {
    background: #333 !important;
    border-color: #333 !important;
  }
}

.reindex-progress {
  padding: 8px 0 16px;
}

.progress-steps {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 20px;
}

.progress-step {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  border-radius: 8px;
  border: 1px solid #f0f0f0;
  background: #fafafa;
  transition: all 0.3s;

  &.loading {
    border-color: #d9d9d9;
    background: #fff;
  }

  &.done {
    border-color: #b7eb8f;
    background: #f6ffed;
  }

  &.error {
    border-color: #ffa39e;
    background: #fff2f0;
  }
}

.step-icon {
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.icon-loading {
  color: #1a1a1a;
  font-size: 16px;
}

.icon-done {
  color: #52c41a;
  font-size: 16px;
}

.icon-error {
  color: #ff4d4f;
  font-size: 16px;
}

.icon-pending {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #d9d9d9;
  display: block;
}

.step-body {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex: 1;
}

.step-label {
  font-size: 13px;
  color: #333;
}

.step-duration {
  font-size: 12px;
  color: #999;
}

.reindex-result {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  border-radius: 8px;
  font-size: 13px;
  margin-bottom: 16px;

  &.success {
    background: #f6ffed;
    border: 1px solid #b7eb8f;
    color: #389e0d;
  }

  &.error {
    background: #fff2f0;
    border: 1px solid #ffa39e;
    color: #cf1322;
  }
}

.result-icon {
  font-size: 16px;
  flex-shrink: 0;
}

.reindex-done-btn {
  text-align: center;

  :deep(.ant-btn) {
    min-width: 100px;
  }
}

// ── RAG 参数配置面板 ──────────────────────────────────────
.params-panel {
  margin-top: 24px;
  border-radius: 12px;
  border: 1px solid #f0f0f0;
}

.params-header {
  margin-bottom: 20px;
  padding-bottom: 14px;
  border-bottom: 1px solid #f0f0f0;

  h2 {
    font-size: 16px;
    font-weight: 600;
    color: #1a1a1a;
    margin: 0 0 4px;
  }
}

.params-sub {
  font-size: 13px;
  color: #999;
  margin: 0;
}

.param-group {
  margin-bottom: 28px;

  &:last-child {
    margin-bottom: 0;
  }
}

.param-group-title {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.param-group-desc {
  font-size: 12px;
  color: #999;
}

.param-row {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 12px 16px;
  border-radius: 8px;
  border: 1px solid #f5f5f5;
  background: #fafafa;
  margin-bottom: 8px;

  &:last-child {
    margin-bottom: 0;
  }
}

.param-meta {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.param-name {
  font-size: 13px;
  font-weight: 600;
  color: #1a1a1a;
}

.param-desc {
  font-size: 12px;
  color: #999;
  line-height: 1.5;
}

.param-input-area {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  flex-shrink: 0;
  min-width: 200px;
  max-width: 520px;
  width: 100%;
  flex-direction: row;
}

.param-save-btn {
  flex-shrink: 0;
  background: #1a1a1a !important;
  border-color: #1a1a1a !important;
  color: #fff !important;
  border-radius: 6px !important;

  &:hover {
    background: #333 !important;
    border-color: #333 !important;
  }
}
</style>
