<template>
  <div id="ragManagePage">
    <div class="page-header">
      <h1>RAG 知识库管理</h1>
      <p>管理 AI 代码生成时参考的知识库文档，支持 Markdown 格式编辑，修改后需刷新混合检索索引</p>
    </div>

    <AdminBackToDashboardButton />

    <!-- 搜索面板 -->
    <a-card class="search-panel">
      <h2>筛选文档</h2>
      <a-form :model="searchParams" layout="inline" @finish="doSearch">
        <a-form-item class="search-item" label="文档标题">
          <a-input v-model:value="searchParams.docTitle" allow-clear placeholder="输入标题关键词" />
        </a-form-item>
        <a-form-item class="search-item" label="业务类型">
          <a-select
            v-model:value="searchParams.bizType"
            allow-clear
            placeholder="全部业务"
            style="width: 180px"
          >
            <a-select-option v-for="item in bizTypeOptions" :key="item.value" :value="item.value">
              {{ item.label }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item class="search-item" label="适用类型">
          <a-select
            v-model:value="searchParams.codeGenType"
            allow-clear
            placeholder="全部类型"
            style="width: 280px"
          >
            <a-select-option
              v-for="item in codeGenTypeOptions"
              :key="item.value"
              :value="item.value"
            >
              {{ item.label }}
            </a-select-option>
          </a-select>
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
        新增文档
      </a-button>
      <a-button class="reindex-btn" @click="openReindexModal">
        <template #icon>
          <SyncOutlined />
        </template>
        刷新混合索引
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
        <template v-if="column.dataIndex === 'bizType'">
          <div class="type-stack">
            <a-tag :color="bizTypeColorMap[getBizTypeValue(record)] || 'blue'">
              {{ bizTypeLabelMap[getBizTypeValue(record)] || '代码生成' }}
            </a-tag>
            <span class="type-hint">{{ getBizTypeHint(record) }}</span>
          </div>
        </template>
        <template v-else-if="column.dataIndex === 'codeGenType'">
          <a-tag :color="codeGenTypeColorMap[record.codeGenType] || 'default'">
            {{ getCodeGenTypeLabel(record) }}
          </a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'docContent'">
          <span class="text-muted">{{ (record.docContent || '').length }} 字符</span>
        </template>
        <template v-else-if="column.dataIndex === 'isEnabled'">
          <a-tag :color="record.isEnabled === 1 ? 'green' : 'red'">
            {{ record.isEnabled === 1 ? '启用' : '禁用' }}
          </a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'action'">
          <a-space>
            <a @click="showEditModal(record)">编辑信息</a>
            <a @click="openEditor(record)">编辑内容</a>
            <a-popconfirm
              title="确定删除该文档？删除后需重新刷新混合索引才能生效。"
              @confirm="handleDelete(record.id)"
            >
              <a class="danger-link">删除</a>
            </a-popconfirm>
          </a-space>
        </template>
      </template>
    </a-table>

    <!-- RAG 参数配置 -->
    <a-card :loading="paramsLoading" class="params-panel">
      <div class="params-header">
        <h2>RAG 参数配置</h2>
        <p class="params-sub">
          动态调整向量化、检索和注入各阶段参数，修改后立即生效（无需重新索引）
        </p>
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
              <template v-if="param.paramKey === 'max_results'">
                <div class="slider-wrap">
                  <a-slider
                    v-model:value="paramDrafts[param.paramKey || '']"
                    :marks="{ 1: '1', 5: '5', 10: '10', 20: '20' }"
                    :max="20"
                    :min="1"
                    :step="1"
                    style="flex: 1; min-width: 180px"
                  />
                  <span class="slider-value">{{ paramDrafts[param.paramKey || ''] }}</span>
                </div>
              </template>
              <template v-else-if="param.paramKey === 'min_score'">
                <div class="slider-wrap">
                  <a-slider
                    v-model:value="paramDrafts[param.paramKey || '']"
                    :marks="{ 0: '0', 0.5: '0.5', 1: '1' }"
                    :max="1"
                    :min="0"
                    :step="0.05"
                    style="flex: 1; min-width: 180px"
                  />
                  <span class="slider-value">{{ paramDrafts[param.paramKey || ''] }}</span>
                </div>
              </template>
              <a-input-number
                v-else-if="param.paramType === 'int'"
                v-model:value="paramDrafts[param.paramKey || '']"
                :min="1"
                :precision="0"
                style="width: 120px"
              />
              <a-input-number
                v-else-if="param.paramType === 'double'"
                v-model:value="paramDrafts[param.paramKey || '']"
                :max="1"
                :min="0"
                :precision="2"
                :step="0.05"
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
                :loading="paramSaving[param.paramKey || '']"
                class="param-save-btn"
                size="small"
                type="primary"
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
      cancel-text="取消"
      :confirm-loading="metaSubmitting"
      ok-text="提交"
      width="560px"
      @ok="handleMetaSubmit"
    >
      <a-form
        :label-col="{ span: 5 }"
        :model="metaForm"
        :wrapper-col="{ span: 19 }"
        style="margin-top: 16px"
      >
        <a-form-item label="文档标题" required>
          <a-input v-model:value="metaForm.docTitle" placeholder="请输入文档标题" />
        </a-form-item>
        <a-form-item label="业务类型" required>
          <a-select
            v-model:value="metaForm.bizType"
            placeholder="请选择文档业务类型"
            style="width: 220px"
          >
            <a-select-option v-for="item in bizTypeOptions" :key="item.value" :value="item.value">
              {{ item.label }}
            </a-select-option>
          </a-select>
          <div class="form-hint">AI 客服知识库只会被首页客服助手检索</div>
        </a-form-item>
        <a-form-item label="适用类型" required>
          <a-select
            v-model:value="metaForm.codeGenType"
            placeholder="请选择适用的代码生成类型"
            style="width: 280px"
          >
            <a-select-option
              v-for="item in codeGenTypeOptions"
              :key="item.value"
              :value="item.value"
            >
              {{ item.label }}
            </a-select-option>
          </a-select>
          <div class="form-hint">GENERAL 表示所有代码生成类型均使用该文档</div>
        </a-form-item>
        <a-form-item label="文档描述">
          <a-textarea
            v-model:value="metaForm.description"
            :rows="2"
            placeholder="简短描述文档用途"
          />
        </a-form-item>
        <a-form-item label="排序权重">
          <a-input-number v-model:value="metaForm.sortOrder" :max="9999" :min="0" />
          <span class="form-hint" style="margin-left: 8px">数字越小越靠前</span>
        </a-form-item>
        <a-form-item label="是否启用">
          <a-switch v-model:checked="metaFormEnabled" />
          <span class="form-hint" style="margin-left: 8px">禁用的文档不会被摄入向量库</span>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 文档内容编辑器（全屏 Markdown 编辑器） -->
    <a-modal
      v-model:open="editorModalVisible"
      :body-style="{ padding: '0', height: 'calc(90vh - 55px)', overflow: 'hidden' }"
      :footer="null"
      :title="'编辑内容：' + (currentEditDoc?.docTitle || '')"
      centered
      destroy-on-close
      width="95vw"
    >
      <div class="editor-wrapper">
        <div class="editor-toolbar">
          <div class="editor-info">
            <a-tag
              :color="bizTypeColorMap[getBizTypeValue(currentEditDoc || {})] || 'blue'"
              size="small"
            >
              {{ bizTypeLabelMap[getBizTypeValue(currentEditDoc || {})] || '代码生成' }}
            </a-tag>
            <a-tag
              :color="codeGenTypeColorMap[currentEditDoc?.codeGenType || ''] || 'default'"
              size="small"
            >
              {{ getCodeGenTypeLabel(currentEditDoc || {}) }}
            </a-tag>
            <span class="editor-char-count">{{ editorContent.length }} 字符</span>
          </div>
          <div class="editor-actions">
            <a-button :loading="saving" type="primary" @click="handleSaveContent">
              <template #icon>
                <SaveOutlined />
              </template>
              保存
            </a-button>
          </div>
        </div>
        <div class="editor-container">
          <MdEditor
            v-model="editorContent"
            :language="'zh-CN'"
            :preview="true"
            :show-code-row-number="true"
            :theme="'light'"
            style="height: 100%"
          />
        </div>
      </div>
    </a-modal>

    <!-- 刷新混合检索索引进度模态框 -->
    <a-modal
      v-model:open="reindexModalVisible"
      :closable="reindexState !== 'progress'"
      :footer="null"
      :mask-closable="reindexState !== 'progress'"
      :title="reindexModalTitle"
      centered
      width="660px"
      @cancel="closeReindexModal"
    >
      <!-- 确认阶段 -->
      <div v-if="reindexState === 'confirm'" class="reindex-confirm">
        <div class="hybrid-hero">
          <div class="hybrid-hero-icon">
            <ApartmentOutlined />
          </div>
          <div class="hybrid-hero-copy">
            <h3>Milvus + Elasticsearch 混合检索</h3>
            <p>向量召回与倒排召回并行，最终由 RRF 统一融合排序</p>
          </div>
          <span class="hybrid-badge">双路融合</span>
        </div>

        <div class="hybrid-flow">
          <div class="hybrid-sources">
            <div class="engine-node engine-milvus">
              <span class="engine-icon"><DatabaseOutlined /></span>
              <div class="engine-copy">
                <strong>Milvus</strong>
                <span>向量召回</span>
              </div>
            </div>
            <div class="engine-node engine-es">
              <span class="engine-icon"><FileSearchOutlined /></span>
              <div class="engine-copy">
                <strong>Elasticsearch</strong>
                <span>倒排召回</span>
              </div>
            </div>
          </div>
          <svg class="flow-connector" viewBox="0 0 66 138" aria-hidden="true">
            <path d="M2 32H24C34 32 34 69 46 69" />
            <path d="M2 106H24C34 106 34 69 46 69" />
            <path d="M46 69H62" />
            <path d="M57 64L63 69L57 74" />
          </svg>
          <div class="engine-node engine-rrf">
            <span class="engine-icon"><MergeCellsOutlined /></span>
            <div class="engine-copy">
              <strong>RRF</strong>
              <span>倒排融合排序</span>
            </div>
          </div>
        </div>

        <div class="reindex-confirm-note">
          <SyncOutlined />
          <span
            >将触发索引重建任务；Milvus 与 Elasticsearch
            的耗时当前为前端模拟，后端能力接入后以真实执行结果为准。</span
          >
        </div>
        <p class="reindex-confirm-sub">重建过程预计 3-10 秒，期间请勿关闭页面。</p>
        <div class="reindex-confirm-btns">
          <a-button @click="closeReindexModal">取消</a-button>
          <a-button class="confirm-primary-btn" type="primary" @click="startReindex"
            >确认重建
          </a-button>
        </div>
      </div>

      <!-- 进度 / 完成 / 失败阶段 -->
      <div v-else class="reindex-progress">
        <div class="progress-overview">
          <div>
            <span class="progress-eyebrow">索引重建进度</span>
            <h3>{{ reindexProgressCaption }}</h3>
          </div>
          <strong class="progress-value">{{ reindexProgress }}%</strong>
        </div>
        <div
          class="progress-track"
          role="progressbar"
          aria-valuemin="0"
          aria-valuemax="100"
          :aria-valuenow="reindexProgress"
        >
          <span class="progress-fill" :style="{ width: `${reindexProgress}%` }"></span>
        </div>

        <div class="hybrid-flow hybrid-flow-progress">
          <div class="hybrid-sources">
            <div class="engine-node engine-milvus" :class="getStepStatus('milvus')">
              <span class="engine-icon"><DatabaseOutlined /></span>
              <div class="engine-copy">
                <strong>Milvus</strong>
                <span class="engine-subtitle">
                  <template v-if="getStepStatus('milvus') === 'loading'">
                    向量召回 · {{ getStepProgress('milvus') }}%
                  </template>
                  <template v-else>向量召回</template>
                </span>
                <span v-if="getStepStatus('milvus') === 'loading'" class="engine-progress">
                  <span :style="{ width: `${getStepProgress('milvus')}%` }"></span>
                </span>
              </div>
              <span v-if="getStepStatus('milvus') === 'loading'" class="engine-parallel">并行</span>
            </div>
            <div class="engine-node engine-es" :class="getStepStatus('elasticsearch')">
              <span class="engine-icon"><FileSearchOutlined /></span>
              <div class="engine-copy">
                <strong>Elasticsearch</strong>
                <span class="engine-subtitle">
                  <template v-if="getStepStatus('elasticsearch') === 'loading'">
                    倒排召回 · {{ getStepProgress('elasticsearch') }}%
                  </template>
                  <template v-else>倒排召回</template>
                </span>
                <span v-if="getStepStatus('elasticsearch') === 'loading'" class="engine-progress">
                  <span :style="{ width: `${getStepProgress('elasticsearch')}%` }"></span>
                </span>
              </div>
              <span v-if="getStepStatus('elasticsearch') === 'loading'" class="engine-parallel"
                >并行</span
              >
            </div>
          </div>
          <svg class="flow-connector" viewBox="0 0 66 138" aria-hidden="true">
            <path d="M2 32H24C34 32 34 69 46 69" />
            <path d="M2 106H24C34 106 34 69 46 69" />
            <path d="M46 69H62" />
            <path d="M57 64L63 69L57 74" />
          </svg>
          <div class="engine-node engine-rrf" :class="getStepStatus('rrf')">
            <span class="engine-icon"><MergeCellsOutlined /></span>
            <div class="engine-copy">
              <strong>RRF</strong>
              <span>倒排融合排序</span>
            </div>
          </div>
        </div>

        <div class="progress-steps">
          <div
            v-for="step in reindexSteps"
            :key="step.id"
            :class="step.status"
            class="progress-step"
          >
            <div class="step-icon">
              <LoadingOutlined v-if="step.status === 'loading'" class="icon-loading" />
              <CheckCircleFilled v-else-if="step.status === 'done'" class="icon-done" />
              <CloseCircleFilled v-else-if="step.status === 'error'" class="icon-error" />
              <span v-else class="icon-pending" />
            </div>
            <div class="step-body">
              <div class="step-heading">
                <span class="step-label">{{ step.label }}</span>
                <span class="step-engine">{{ step.engine }}</span>
              </div>
              <span class="step-description">{{ step.description }}</span>
            </div>
            <span class="step-status">
              {{ getStepStatusLabel(step.status) }}
              <template v-if="step.status === 'loading' && step.eta">
                · 预计 {{ step.eta }}</template
              >
              <template v-else-if="step.status === 'done' && step.duration">
                · {{ step.duration }}</template
              >
            </span>
          </div>
        </div>

        <div v-if="reindexState === 'error'" class="reindex-result error">
          <CloseCircleFilled class="result-icon" />
          <span>{{ reindexErrorMsg }}</span>
        </div>

        <div v-if="reindexState === 'done' || reindexState === 'error'" class="reindex-done-btn">
          <a-button class="confirm-primary-btn" type="primary" @click="closeReindexModal">
            关闭
          </a-button>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script lang="ts" setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import {
  ApartmentOutlined,
  CheckCircleFilled,
  CloseCircleFilled,
  DatabaseOutlined,
  FileSearchOutlined,
  LoadingOutlined,
  MergeCellsOutlined,
  PlusOutlined,
  SaveOutlined,
  SearchOutlined,
  SyncOutlined,
} from '@ant-design/icons-vue'
import { MdEditor } from 'md-editor-v3'
import 'md-editor-v3/lib/style.css'
import {
  addRagDocument,
  deleteRagDocument,
  listRagDocumentByPage,
  reindexRagDocuments,
  updateRagDocument,
} from '@/api/ragDocumentController'
import AdminBackToDashboardButton from '@/components/admin/AdminBackToDashboardButton.vue'
import { listRagParams, updateRagParam } from '@/api/ragParamController'

// ── 通用状态 ──────────────────────────────────────────────
const loading = ref(false)
const dataList = ref<API.RagDocumentVO[]>([])
const isEdit = ref(false)
const metaSubmitting = ref(false)
const deletingRagDocumentIds = reactive(new Set<number>())
let dataRequestId = 0
let paramsRequestId = 0

const bizTypeOptions = [
  { value: 'CODE_GEN', label: '代码生成' },
  { value: 'CUSTOMER_SERVICE', label: 'AI 客服' },
]

const bizTypeLabelMap: Record<string, string> = {
  CODE_GEN: '代码生成',
  CUSTOMER_SERVICE: 'AI 客服',
}

const bizTypeColorMap: Record<string, string> = {
  CODE_GEN: 'blue',
  CUSTOMER_SERVICE: 'geekblue',
}

const codeGenTypeOptions = [
  { value: 'HTML', label: 'HTML — 单文件模式' },
  { value: 'MULTI_FILE', label: 'MULTI_FILE — 多文件模式' },
  { value: 'VUE_PROJECT', label: 'VUE_PROJECT — Vue 项目模式' },
  { value: 'AGENT', label: 'AGENT — Agent 自主模式专用' },
  { value: 'GENERAL', label: 'GENERAL — 通用（所有类型）' },
]

const codeGenTypeLabelMap: Record<string, string> = {
  HTML: '单文件',
  MULTI_FILE: '多文件',
  VUE_PROJECT: 'Vue项目',
  AGENT: 'Agent专用',
  GENERAL: '通用',
}

const codeGenTypeColorMap: Record<string, string> = {
  HTML: 'blue',
  MULTI_FILE: 'purple',
  VUE_PROJECT: 'cyan',
  AGENT: 'green',
  GENERAL: 'orange',
}

const getBizTypeValue = (record: Partial<API.RagDocumentVO>) => record.bizType || 'CODE_GEN'

const getBizTypeHint = (record: Partial<API.RagDocumentVO>) => {
  return getBizTypeValue(record) === 'CUSTOMER_SERVICE' ? '首页客服专用' : '代码生成链路'
}

const getCodeGenTypeLabel = (record: Partial<API.RagDocumentVO>) => {
  if (getBizTypeValue(record) === 'CUSTOMER_SERVICE') {
    if (!record.codeGenType || record.codeGenType === 'GENERAL') {
      return '客服通用'
    }
  }
  if (!record.codeGenType) {
    return '通用'
  }
  return codeGenTypeLabelMap[record.codeGenType] || record.codeGenType
}

const getErrorMessage = (error: unknown, fallback = '请重试') => {
  if (error instanceof Error && error.message) {
    return error.message
  }
  return fallback
}

// ── 搜索 & 分页 ───────────────────────────────────────────
const searchParams = reactive<API.RagDocumentQueryRequest>({
  pageNum: 1,
  pageSize: 10,
  bizType: undefined,
  docTitle: '',
  codeGenType: undefined,
  isEnabled: undefined,
})

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`,
})

// ── 表格列定义 ────────────────────────────────────────────
const columns = [
  { title: 'ID', dataIndex: 'id', width: 80, ellipsis: true },
  { title: '文档标题', dataIndex: 'docTitle', width: 200, ellipsis: true },
  { title: '业务类型', dataIndex: 'bizType', width: 110 },
  { title: '适用类型', dataIndex: 'codeGenType', width: 120 },
  { title: '内容长度', dataIndex: 'docContent', width: 100 },
  { title: '描述', dataIndex: 'description', width: 180, ellipsis: true },
  { title: '排序', dataIndex: 'sortOrder', width: 70 },
  { title: '状态', dataIndex: 'isEnabled', width: 80 },
  { title: '操作', dataIndex: 'action', width: 180, fixed: 'right' },
]

// ── 元数据模态框 ──────────────────────────────────────────
const metaModalVisible = ref(false)

const metaForm = reactive({
  id: undefined as number | undefined,
  docTitle: '',
  bizType: 'CODE_GEN' as string,
  codeGenType: 'GENERAL' as string,
  description: '',
  sortOrder: 0,
  isEnabled: 1,
})

const metaFormEnabled = computed({
  get: () => metaForm.isEnabled === 1,
  set: (val: boolean) => {
    metaForm.isEnabled = val ? 1 : 0
  },
})

const resetMetaForm = () => {
  metaForm.id = undefined
  metaForm.docTitle = ''
  metaForm.bizType = 'CODE_GEN'
  metaForm.codeGenType = 'GENERAL'
  metaForm.description = ''
  metaForm.sortOrder = 0
  metaForm.isEnabled = 1
}

const showAddModal = () => {
  if (metaSubmitting.value) {
    message.warning('文档信息正在提交，请稍候')
    return
  }
  isEdit.value = false
  resetMetaForm()
  metaModalVisible.value = true
}

const showEditModal = (record: API.RagDocumentVO) => {
  if (metaSubmitting.value) {
    message.warning('文档信息正在提交，请稍候')
    return
  }
  isEdit.value = true
  metaForm.id = record.id
  metaForm.docTitle = record.docTitle || ''
  metaForm.bizType = record.bizType || 'CODE_GEN'
  metaForm.codeGenType = record.codeGenType || 'GENERAL'
  metaForm.description = record.description || ''
  metaForm.sortOrder = record.sortOrder ?? 0
  metaForm.isEnabled = record.isEnabled ?? 1
  metaModalVisible.value = true
}

const handleMetaSubmit = async () => {
  if (metaSubmitting.value) return
  if (!metaForm.docTitle.trim()) {
    message.warning('文档标题不能为空')
    return
  }
  if (!metaForm.bizType) {
    message.warning('请选择业务类型')
    return
  }
  if (!metaForm.codeGenType) {
    message.warning('请选择适用类型')
    return
  }

  metaSubmitting.value = true
  try {
    if (isEdit.value && metaForm.id) {
      const res = await updateRagDocument({
        id: metaForm.id,
        docTitle: metaForm.docTitle,
        bizType: metaForm.bizType,
        codeGenType: metaForm.codeGenType,
        description: metaForm.description,
        sortOrder: metaForm.sortOrder,
        isEnabled: metaForm.isEnabled,
      })
      if (res.data.code === 0) {
        message.success('信息更新成功，请点击"刷新混合索引"使改动生效')
        metaModalVisible.value = false
        fetchData()
      } else {
        message.error('更新失败：' + res.data.message)
      }
    } else {
      const res = await addRagDocument({
        docTitle: metaForm.docTitle,
        docContent: '',
        bizType: metaForm.bizType,
        codeGenType: metaForm.codeGenType,
        description: metaForm.description,
        sortOrder: metaForm.sortOrder,
        isEnabled: metaForm.isEnabled,
      })
      if (res.data.code === 0) {
        message.success('新增成功，请在"编辑内容"中补充文档内容后刷新混合索引')
        metaModalVisible.value = false
        fetchData()
      } else {
        message.error('新增失败：' + res.data.message)
      }
    }
  } catch (e: unknown) {
    message.error('操作失败：' + getErrorMessage(e))
  } finally {
    metaSubmitting.value = false
  }
}

// ── 内容编辑器模态框 ──────────────────────────────────────
const editorModalVisible = ref(false)
const currentEditDoc = ref<API.RagDocumentVO | null>(null)
const editorContent = ref('')
const saving = ref(false)

const openEditor = (record: API.RagDocumentVO) => {
  if (saving.value || metaSubmitting.value) {
    message.warning('文档正在保存，请稍候')
    return
  }
  currentEditDoc.value = record
  editorContent.value = record.docContent || ''
  editorModalVisible.value = true
}

const handleSaveContent = async () => {
  if (saving.value || !currentEditDoc.value?.id) return
  saving.value = true
  try {
    const res = await updateRagDocument({
      id: currentEditDoc.value.id,
      docContent: editorContent.value,
    })
    if (res.data.code === 0) {
      message.success('内容保存成功，请点击"刷新混合索引"使改动生效')
      editorModalVisible.value = false
      fetchData()
    } else {
      message.error('保存失败：' + res.data.message)
    }
  } catch (e: unknown) {
    message.error('保存失败：' + getErrorMessage(e))
  } finally {
    saving.value = false
  }
}

// ── 刷新混合检索索引进度模态框 ────────────────────────────
const reindexModalVisible = ref(false)
const reindexState = ref<'confirm' | 'progress' | 'done' | 'error'>('confirm')
const reindexErrorMsg = ref('')

interface ReindexStep {
  id: string
  label: string
  engine: string
  description: string
  status: 'pending' | 'loading' | 'done' | 'error'
  progress: number
  duration?: string
  eta?: string
}

const reindexSteps = ref<ReindexStep[]>([
  {
    id: 'prepare',
    label: '文档清洗与向量化',
    engine: 'EMBEDDING',
    description: '读取启用文档并生成 text-embedding-v4 语义向量',
    status: 'pending',
    progress: 0,
  },
  {
    id: 'milvus',
    label: '写入 Milvus 向量索引',
    engine: 'MILVUS',
    description: '建立 1536 维向量索引，支撑语义召回',
    status: 'pending',
    progress: 0,
  },
  {
    id: 'elasticsearch',
    label: '构建 Elasticsearch 倒排索引',
    engine: 'ES',
    description: '写入 BM25 倒排索引，支撑关键词召回',
    status: 'pending',
    progress: 0,
  },
  {
    id: 'rrf',
    label: '执行 RRF 融合排序',
    engine: 'RRF',
    description: '合并向量与倒排两路排名，生成最终知识片段顺序',
    status: 'pending',
    progress: 0,
  },
])

const reindexProgress = computed(() => {
  const totalProgress = reindexSteps.value.reduce((total, step) => total + step.progress, 0)
  return Math.min(100, Math.round(totalProgress / reindexSteps.value.length))
})

const reindexActiveSteps = computed(() =>
  reindexSteps.value.filter((step) => step.status === 'loading'),
)

const reindexProgressCaption = computed(() => {
  if (reindexState.value === 'done') return '混合索引模拟完成'
  if (reindexState.value === 'error') return '索引重建已中断'
  const activeSteps = reindexActiveSteps.value
  const parallelIndexing = activeSteps.filter(
    (step) => step.id === 'milvus' || step.id === 'elasticsearch',
  )
  if (parallelIndexing.length === 2) return 'Milvus 与 Elasticsearch 正在并行构建'
  if (parallelIndexing.length === 1) {
    const otherStep = reindexSteps.value.find(
      (step) =>
        (step.id === 'milvus' || step.id === 'elasticsearch') && step.id !== parallelIndexing[0].id,
    )
    return otherStep?.status === 'done'
      ? `${parallelIndexing[0].label}正在收尾，另一路已完成`
      : parallelIndexing[0].label
  }
  return activeSteps[0]?.label || '等待后端确认重建结果'
})

const reindexStatusLabels: Record<ReindexStep['status'], string> = {
  pending: '等待中',
  loading: '处理中',
  done: '已完成',
  error: '失败',
}

const getStepStatus = (id: string): ReindexStep['status'] => {
  return reindexSteps.value.find((step) => step.id === id)?.status || 'pending'
}

const getStepProgress = (id: string) => {
  return Math.round(reindexSteps.value.find((step) => step.id === id)?.progress || 0)
}

const getStepStatusLabel = (status: ReindexStep['status']) => reindexStatusLabels[status]

const reindexModalTitle = computed(() => {
  if (reindexState.value === 'confirm') return '刷新混合检索索引'
  if (reindexState.value === 'progress') return '正在重建混合索引…'
  if (reindexState.value === 'done') return '混合索引模拟完成'
  return '混合索引重建失败'
})

const reindexTimers = new Set<number>()
const reindexStopHandlers = new Set<() => void>()
let reindexAbortController: AbortController | null = null
let reindexRunSequence = 0

const cancelReindexRun = () => {
  reindexRunSequence += 1
  reindexAbortController?.abort()
  reindexAbortController = null
  reindexStopHandlers.forEach((stop) => stop())
  reindexStopHandlers.clear()
  reindexTimers.forEach((timer) => window.clearInterval(timer))
  reindexTimers.clear()
}

const closeReindexModal = () => {
  cancelReindexRun()
  reindexModalVisible.value = false
}

const openReindexModal = () => {
  cancelReindexRun()
  reindexState.value = 'confirm'
  reindexSteps.value.forEach((s) => {
    s.status = 'pending'
    s.progress = 0
    s.duration = undefined
    s.eta = undefined
  })
  reindexModalVisible.value = true
}

const startReindex = async () => {
  if (reindexState.value === 'progress') return

  cancelReindexRun()
  const runId = reindexRunSequence
  const controller = new AbortController()
  reindexAbortController = controller
  reindexState.value = 'progress'
  reindexErrorMsg.value = ''

  const steps = reindexSteps.value
  const getStep = (id: string) => {
    const step = steps.find((item) => item.id === id)
    if (!step) throw new Error(`未找到索引阶段：${id}`)
    return step
  }

  const prepareStep = getStep('prepare')
  const milvusStep = getStep('milvus')
  const elasticsearchStep = getStep('elasticsearch')
  const rrfStep = getStep('rrf')
  const randomMs = (min: number, max: number) => Math.floor(Math.random() * (max - min + 1)) + min

  // 后端真实混合索引能力接入前，先在前端模拟各阶段耗时；
  // Milvus 与 Elasticsearch 独立计时并并发执行，RRF 等待两路完成后启动。
  const simulateStep = (step: ReindexStep, durationMs: number, maxProgress = 100) => {
    return new Promise<void>((resolve) => {
      let timer = 0
      let settled = false
      const stop = () => {
        if (settled) return
        settled = true
        window.clearInterval(timer)
        reindexTimers.delete(timer)
        reindexStopHandlers.delete(stop)
        resolve()
      }

      if (runId !== reindexRunSequence) {
        resolve()
        return
      }

      reindexStopHandlers.add(stop)
      step.status = 'loading'
      step.progress = 0
      step.duration = undefined
      step.eta = `${(durationMs / 1000).toFixed(1)}s`
      const startedAt = performance.now()

      const tick = () => {
        if (settled || runId !== reindexRunSequence) return
        const elapsed = performance.now() - startedAt
        step.progress = Math.min(maxProgress, Math.round((elapsed / durationMs) * maxProgress))
        if (elapsed < durationMs) return

        settled = true
        window.clearInterval(timer)
        reindexTimers.delete(timer)
        reindexStopHandlers.delete(stop)
        step.eta = undefined
        step.duration = `${(elapsed / 1000).toFixed(1)}s`
        if (maxProgress >= 100) {
          step.progress = 100
          step.status = 'done'
        }
        resolve()
      }

      timer = window.setInterval(tick, 100)
      reindexTimers.add(timer)
      tick()
    })
  }

  const runSimulation = async () => {
    await simulateStep(prepareStep, randomMs(900, 1400))
    if (runId !== reindexRunSequence) return
    await Promise.all([
      simulateStep(milvusStep, randomMs(4200, 7000)),
      simulateStep(elasticsearchStep, randomMs(3600, 6200)),
    ])
    if (runId !== reindexRunSequence) return
    await simulateStep(rrfStep, randomMs(1100, 1800), 95)
  }

  const apiRequest = reindexRagDocuments({ signal: controller.signal })
    .then((res) => ({
      status: 'response' as const,
      res,
    }))
    .catch((error) => ({
      status: 'rejected' as const,
      error,
    }))

  try {
    const [apiOutcome] = await Promise.all([apiRequest, runSimulation()])
    if (runId !== reindexRunSequence) return

    if (apiOutcome.status === 'rejected') {
      rrfStep.status = 'error'
      reindexErrorMsg.value = '混合索引重建失败：' + getErrorMessage(apiOutcome.error)
      reindexState.value = 'error'
    } else if (apiOutcome.res.data.code === 0) {
      rrfStep.status = 'done'
      rrfStep.progress = 100
      reindexState.value = 'done'
    } else {
      rrfStep.status = 'error'
      reindexErrorMsg.value = '混合索引重建失败：' + (apiOutcome.res.data.message || '未知错误')
      reindexState.value = 'error'
    }
  } catch (e: unknown) {
    if (runId !== reindexRunSequence) return
    reindexStopHandlers.forEach((stop) => stop())
    reindexStopHandlers.clear()
    steps
      .filter((step) => step.status === 'loading')
      .forEach((step) => {
        step.status = 'error'
        step.eta = undefined
      })
    reindexErrorMsg.value = '混合索引重建失败：' + getErrorMessage(e)
    reindexState.value = 'error'
  } finally {
    if (runId === reindexRunSequence) {
      reindexAbortController = null
    }
  }
}

// ── 数据操作 ──────────────────────────────────────────────
const fetchData = async () => {
  const requestId = ++dataRequestId
  loading.value = true
  try {
    const res = await listRagDocumentByPage({
      ...searchParams,
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
    })
    if (requestId !== dataRequestId) return
    if (res.data.code === 0 && res.data.data) {
      dataList.value = (res.data.data.records || []).map((item) => ({
        ...item,
        bizType: item.bizType || 'CODE_GEN',
      }))
      pagination.total = Number(res.data.data.totalRow) || 0
    } else {
      message.error('加载文档列表失败：' + (res.data.message || '请稍后重试'))
    }
  } catch (e) {
    if (requestId !== dataRequestId) return
    console.error('加载文档列表失败', e)
    message.error('加载文档列表失败，请稍后重试')
  } finally {
    if (requestId === dataRequestId) loading.value = false
  }
}

const doSearch = () => {
  pagination.current = 1
  fetchData()
}

const resetSearch = () => {
  searchParams.docTitle = ''
  searchParams.bizType = undefined
  searchParams.codeGenType = undefined
  searchParams.isEnabled = undefined
  doSearch()
}

const handleTableChange = (pag: { current?: number; pageSize?: number }) => {
  pagination.current = pag.current || 1
  pagination.pageSize = pag.pageSize || 10
  fetchData()
}

const handleDelete = async (id: number) => {
  if (deletingRagDocumentIds.has(id)) return
  deletingRagDocumentIds.add(id)
  try {
    const res = await deleteRagDocument({ id })
    if (res.data.code === 0) {
      message.success('删除成功，请点击"刷新混合索引"使改动生效')
      fetchData()
    } else {
      message.error('删除失败：' + res.data.message)
    }
  } catch (e: unknown) {
    message.error('删除失败：' + getErrorMessage(e))
  } finally {
    deletingRagDocumentIds.delete(id)
  }
}

onMounted(() => {
  fetchData()
  fetchParams()
})

onBeforeUnmount(() => {
  dataRequestId += 1
  paramsRequestId += 1
  cancelReindexRun()
})

// ── RAG 参数配置 ──────────────────────────────────────────
const paramsLoading = ref(false)
const rawParams = ref<API.RagParamVO[]>([])
const paramDrafts = reactive<Record<string, string | number | undefined>>({})
const paramSaving = reactive<Record<string, boolean>>({})

const groupMeta: Record<string, { label: string; color: string; desc: string }> = {
  etl: {
    label: 'ETL 切分',
    color: 'blue',
    desc: '文档收集与切片阶段参数（修改后需重新刷新混合索引才能生效）',
  },
  retrieval: {
    label: '向量检索',
    color: 'purple',
    desc: '向量搜索阶段参数（实时生效，无需重新索引）',
  },
  injection: {
    label: '提示词注入',
    color: 'cyan',
    desc: 'RAG 内容注入到提示词的模板（实时生效，无需重新索引）',
  },
}

const paramGroups = computed(() => {
  const groups: {
    key: string
    label: string
    color: string
    desc: string
    params: API.RagParamVO[]
  }[] = []
  const order = ['etl', 'retrieval', 'injection']
  for (const key of order) {
    const params = rawParams.value.filter((p) => p.paramGroup === key)
    if (params.length > 0) {
      groups.push({ key, ...groupMeta[key], params })
    }
  }
  return groups
})

const fetchParams = async () => {
  const requestId = ++paramsRequestId
  paramsLoading.value = true
  try {
    const res = await listRagParams()
    if (requestId !== paramsRequestId) return
    if (res.data.code === 0 && res.data.data) {
      rawParams.value = res.data.data
      res.data.data.forEach((p) => {
        if (p.paramKey) {
          paramDrafts[p.paramKey] =
            p.paramType === 'int'
              ? Number(p.paramValue)
              : p.paramType === 'double'
                ? Number(p.paramValue)
                : p.paramValue
        }
      })
    }
  } catch (e) {
    if (requestId !== paramsRequestId) return
    console.error('加载 RAG 参数失败', e)
    message.error('加载 RAG 参数失败，请稍后重试')
  } finally {
    if (requestId === paramsRequestId) paramsLoading.value = false
  }
}

const handleSaveParam = async (param: API.RagParamVO) => {
  const key = param.paramKey
  if (!key || paramSaving[key]) return
  paramSaving[key] = true
  try {
    const newVal = String(paramDrafts[key])
    const res = await updateRagParam({ id: param.id, paramValue: newVal })
    if (res.data.code === 0) {
      message.success(`「${param.paramName}」已保存`)
      const target = rawParams.value.find((p) => p.paramKey === key)
      if (target) target.paramValue = newVal
    } else {
      message.error('保存失败：' + res.data.message)
    }
  } catch (e: unknown) {
    message.error('保存失败：' + getErrorMessage(e))
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

.type-stack {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.type-hint {
  font-size: 12px;
  color: #999;
  line-height: 1;
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

// ── 混合检索索引重建模态框 ────────────────────────────────
.reindex-confirm {
  padding: 2px 0 10px;
}

.hybrid-hero {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  margin-bottom: 18px;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  background: #fafafa;
}

.hybrid-hero-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 42px;
  height: 42px;
  border: 1px solid #f0f0f0;
  border-radius: 10px;
  font-size: 19px;
  color: #1a1a1a;
  background: #fff;
}

.hybrid-hero-copy {
  flex: 1;
  min-width: 0;

  h3 {
    margin: 0 0 4px;
    font-size: 15px;
    font-weight: 600;
    color: #1a1a1a;
  }

  p {
    margin: 0;
    font-size: 12px;
    line-height: 1.5;
    color: #999;
  }
}

.hybrid-badge {
  flex-shrink: 0;
  padding: 3px 8px;
  border: 1px solid #f0f0f0;
  border-radius: 6px;
  font-size: 12px;
  color: #666;
  background: #fff;
}

.hybrid-flow {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 66px 178px;
  align-items: center;
  margin: 2px 0 16px;
}

.hybrid-sources {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.engine-node {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  min-height: 64px;
  padding: 10px 12px;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  background: #fafafa;
  transition:
    border-color 0.25s ease,
    background 0.25s ease,
    box-shadow 0.25s ease;

  .engine-copy {
    display: flex;
    flex-direction: column;
    flex: 1;
    min-width: 0;
    gap: 2px;
  }

  strong {
    overflow: hidden;
    font-size: 13px;
    font-weight: 600;
    color: #1a1a1a;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .engine-subtitle {
    font-size: 11px;
    line-height: 1.35;
    color: #999;
  }

  &.loading {
    border-color: #d9d9d9;
    background: #fff;
    box-shadow: none;
  }

  &.done {
    border-color: #b7eb8f;
    background: #f6ffed;
  }

  &.error {
    border-color: #ffa39e;
    background: #fef2f2;
  }
}

.engine-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 34px;
  height: 34px;
  border-radius: 8px;
  font-size: 16px;
  color: #595959;
  background: #f5f5f5;
}

.engine-progress {
  display: block;
  width: 100%;
  height: 3px;
  margin-top: 3px;
  overflow: hidden;
  border-radius: 999px;
  background: #f0f0f0;

  span {
    display: block;
    height: 100%;
    border-radius: inherit;
    background: #595959;
    transition: width 0.12s linear;
  }
}

.engine-parallel {
  flex-shrink: 0;
  padding: 1px 5px;
  border: 1px solid #f0f0f0;
  border-radius: 4px;
  font-size: 10px;
  line-height: 16px;
  color: #999;
  background: #fff;
}

.engine-milvus .engine-icon {
  color: #595959;
  background: #f5f5f5;
}

.engine-es .engine-icon {
  color: #595959;
  background: #f5f5f5;
}

.engine-rrf {
  min-height: 72px;
  border-color: #f0f0f0;
  background: #fafafa;

  .engine-icon {
    color: #1a1a1a;
    background: #f0f0f0;
  }
}

.flow-connector {
  width: 66px;
  height: 138px;
  overflow: visible;

  path {
    fill: none;
    stroke: #d9d9d9;
    stroke-width: 1.5;
    stroke-linecap: round;
    stroke-linejoin: round;
  }
}

.reindex-confirm-note {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 12px 14px;
  border: 1px solid #fde68a;
  border-radius: 10px;
  font-size: 12px;
  line-height: 1.6;
  color: #92400e;
  background: #fffbeb;

  :deep(.anticon) {
    margin-top: 2px;
    color: #d97706;
  }
}

.reindex-confirm-sub {
  margin: 10px 0 20px;
  font-size: 12px;
  color: #999;
}

.reindex-confirm-btns {
  display: flex;
  justify-content: flex-end;
  gap: 12px;

  :deep(.ant-btn) {
    min-width: 104px;
    border-radius: 8px;
  }
}

.confirm-primary-btn {
  border-color: #1a1a1a !important;
  color: #fff !important;
  background: #1a1a1a !important;

  &:hover {
    border-color: #333 !important;
    background: #333 !important;
  }
}

.reindex-progress {
  padding: 2px 0 10px;
}

.progress-overview {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 10px;

  h3 {
    margin: 4px 0 0;
    overflow: hidden;
    font-size: 16px;
    font-weight: 600;
    color: #1a1a1a;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.progress-eyebrow {
  font-size: 12px;
  color: #999;
}

.progress-value {
  flex-shrink: 0;
  font-size: 26px;
  font-variant-numeric: tabular-nums;
  line-height: 1;
  color: #1a1a1a;
}

.progress-track {
  height: 8px;
  margin-bottom: 18px;
  overflow: hidden;
  border-radius: 999px;
  background: #f5f5f5;
}

.progress-fill {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: #1a1a1a;
  transition: width 0.35s ease;
}

.hybrid-flow-progress {
  margin-bottom: 14px;
}

.progress-steps {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 16px;
}

.progress-step {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 11px 14px;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  background: #fafafa;
  transition:
    border-color 0.25s ease,
    background 0.25s ease;

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
    background: #fef2f2;
  }
}

.step-icon {
  width: 18px;
  height: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  margin-top: 1px;
}

.icon-loading {
  color: #1a1a1a;
  font-size: 15px;
}

.icon-done {
  color: #52c41a;
  font-size: 15px;
}

.icon-error {
  color: #ff4d4f;
  font-size: 15px;
}

.icon-pending {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #d9d9d9;
  display: block;
}

.step-body {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  flex: 1;
  min-width: 0;
  gap: 3px;
}

.step-heading {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.step-label {
  font-size: 13px;
  font-weight: 600;
  color: #333;
}

.step-engine {
  padding: 1px 6px;
  border-radius: 4px;
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.08em;
  color: #999;
  background: #f5f5f5;
}

.step-description {
  overflow: hidden;
  max-width: 100%;
  font-size: 11px;
  line-height: 1.5;
  color: #999;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.step-status {
  flex-shrink: 0;
  padding-top: 1px;
  font-size: 12px;
  color: #999;
}

.progress-step.loading .step-status {
  color: #1a1a1a;
}

.progress-step.done .step-status {
  color: #52c41a;
}

.progress-step.error .step-status {
  color: #ff4d4f;
}

.reindex-result {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  border-radius: 10px;
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
  text-align: right;

  :deep(.ant-btn) {
    min-width: 104px;
  }
}

@media (max-width: 640px) {
  .hybrid-hero {
    align-items: flex-start;
  }

  .hybrid-badge {
    display: none;
  }

  .hybrid-flow {
    grid-template-columns: minmax(0, 1fr);
    gap: 10px;
  }

  .flow-connector {
    width: 36px;
    height: 64px;
    margin: -6px auto;
    transform: rotate(90deg);
  }

  .engine-rrf {
    min-height: 58px;
  }

  .step-description {
    white-space: normal;
  }

  .step-status {
    display: none;
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

.slider-wrap {
  display: flex;
  align-items: center;
  gap: 16px;
  flex: 1;
  padding-bottom: 8px;
}

.slider-value {
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
  min-width: 36px;
  text-align: center;
  background: #f5f5f5;
  border-radius: 6px;
  padding: 2px 8px;
  flex-shrink: 0;
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
