<template>
  <div class="markdown-content" @click="handleContentClick" v-html="renderedMarkdown"></div>
  <a-modal
    v-if="collapseLongCode"
    v-model:open="codeViewerOpen"
    :footer="null"
    :title="codeViewerTitle"
    centered
    destroy-on-close
    width="min(1120px, 92vw)"
    wrap-class-name="cw-code-viewer-modal"
  >
    <div v-if="codeViewer" class="cw-code-viewer">
      <div class="cw-code-viewer-meta">
        <span>{{ codeViewer.languageLabel }}</span>
        <span>{{ codeViewer.lineCount }} 行</span>
        <span>{{ codeViewer.charCount.toLocaleString('zh-CN') }} 字符</span>
      </div>
      <pre class="cw-code-viewer-pre"><code v-html="highlightedViewerCode"></code></pre>
    </div>
  </a-modal>
</template>

<script lang="ts" setup>
import { computed, ref } from 'vue'
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js'
import 'highlight.js/styles/github.css'
import { processToolCallMarkers } from '@/utils/toolCallParser'
import { fixCodeBlockFormat, processWorkflowMarkers } from '@/utils/workflowMarkerParser'

interface Props {
  content: string
  streaming?: boolean
  collapseLongCode?: boolean
  collapseLongCodeThreshold?: number
}

interface CodeBlockInfo {
  key: string
  code: string
  language: string
  languageLabel: string
  lineCount: number
  charCount: number
}

interface MarkdownRenderEnv {
  codeBlocks: Map<string, CodeBlockInfo>
}

const props = withDefaults(defineProps<Props>(), {
  streaming: false,
  collapseLongCode: false,
  collapseLongCodeThreshold: 1200
})

const codeViewerOpen = ref(false)
const codeViewer = ref<CodeBlockInfo | null>(null)
let renderedCodeBlocks = new Map<string, CodeBlockInfo>()
let nextCodeBlockIndex = 0

const LANGUAGE_LABELS: Record<string, string> = {
  html: 'HTML',
  xml: 'HTML / XML',
  css: 'CSS',
  javascript: 'JavaScript',
  js: 'JavaScript',
  typescript: 'TypeScript',
  ts: 'TypeScript',
  vue: 'Vue',
  json: 'JSON',
  bash: 'Shell',
  shell: 'Shell',
  sh: 'Shell',
  python: 'Python',
  java: 'Java',
  sql: 'SQL'
}

const normalizeLanguage = (lang: string) => {
  const normalized = (lang || '').trim().toLowerCase()
  if (normalized === 'js') return 'javascript'
  if (normalized === 'ts') return 'typescript'
  if (normalized === 'html' || normalized === 'vue') return 'xml'
  return normalized
}

const getLanguageLabel = (lang: string) => {
  const normalized = (lang || '').trim().toLowerCase()
  return LANGUAGE_LABELS[normalized] || (normalized ? normalized.toUpperCase() : '代码')
}

const md: MarkdownIt = new MarkdownIt({
  html: true,
  linkify: true,
  typographer: true,
  highlight: function(str: string, lang: string): string {
    const normalizedLang = normalizeLanguage(lang)
    if (!props.streaming && normalizedLang && hljs.getLanguage(normalizedLang)) {
      try {
        return (
          '<pre class="hljs"><code>' +
          hljs.highlight(str, { language: normalizedLang, ignoreIllegals: true }).value +
          '</code></pre>'
        )
      } catch {
        // 忽略错误，使用默认处理
      }
    }

    return '<pre class="hljs"><code>' + md.utils.escapeHtml(str) + '</code></pre>'
  }
})

const defaultFenceRule = md.renderer.rules.fence

md.renderer.rules.fence = (tokens, idx, options, env, self) => {
  const token = tokens[idx]
  const code = token.content || ''
  const language = (token.info || '').trim().split(/\s+/)[0] || 'text'
  const lineCount = code ? code.replace(/\r?\n$/, '').split(/\r?\n/).length : 0
  const isLongCode = code.length >= props.collapseLongCodeThreshold || lineCount >= 40

  if (props.collapseLongCode && isLongCode && code.length > 0) {
    const renderEnv = env as MarkdownRenderEnv
    const key = `cw-code-${nextCodeBlockIndex++}`
    const languageLabel = getLanguageLabel(language)
    const codeBlock: CodeBlockInfo = {
      key,
      code,
      language,
      languageLabel,
      lineCount,
      charCount: code.length
    }
    renderEnv.codeBlocks.set(key, codeBlock)

    return `
      <div class="cw-code-collapsed">
        <div class="cw-code-collapsed__bar">
          <span class="cw-code-collapsed__icon">&lt;/&gt;</span>
          <div class="cw-code-collapsed__copy">
            <strong class="cw-code-collapsed__title">${md.utils.escapeHtml(languageLabel)} 代码块</strong>
            <span class="cw-code-collapsed__meta">${lineCount} 行 · ${code.length.toLocaleString('zh-CN')} 字符 · 已自动折叠</span>
          </div>
          <button type="button" class="cw-code-open-btn" data-cw-code-key="${key}" aria-label="弹窗查看完整代码">
            弹窗查看
          </button>
        </div>
      </div>
    `
  }

  if (defaultFenceRule) {
    return defaultFenceRule(tokens, idx, options, env, self)
  }
  return self.renderToken(tokens, idx, options)
}

// 预处理Markdown内容，识别并包裹工具调用信息和工作流标记
const preprocessMarkdown = (content: string): string => {
  if (!content) return ''

  // 1. 处理工作流标记（工作流模式）
  content = processWorkflowMarkers(content)

  // 2. 处理工具调用标记（Agent 模式）
  content = processToolCallMarkers(content)

  // 3. 修复代码块格式
  content = fixCodeBlockFormat(content)

  return content
}

const renderedMarkdown = computed(() => {
  const processedContent = preprocessMarkdown(props.content)
  const codeBlocks = new Map<string, CodeBlockInfo>()
  renderedCodeBlocks = codeBlocks
  nextCodeBlockIndex = 0
  return md.render(processedContent, { codeBlocks })
})

const codeViewerTitle = computed(() => {
  return codeViewer.value ? `${codeViewer.value.languageLabel} 完整代码` : '完整代码'
})

const highlightedViewerCode = computed(() => {
  const code = codeViewer.value?.code || ''
  const language = normalizeLanguage(codeViewer.value?.language || '')
  if (language && hljs.getLanguage(language)) {
    try {
      return hljs.highlight(code, { language, ignoreIllegals: true }).value
    } catch {
      return md.utils.escapeHtml(code)
    }
  }
  return md.utils.escapeHtml(code)
})

const handleContentClick = (event: MouseEvent) => {
  const target = event.target as HTMLElement | null
  const trigger = target?.closest<HTMLElement>('[data-cw-code-key]')
  if (!trigger) return

  const key = trigger.dataset.cwCodeKey
  const codeBlock = key ? renderedCodeBlocks.get(key) : undefined
  if (!codeBlock) return

  event.preventDefault()
  codeViewer.value = { ...codeBlock }
  codeViewerOpen.value = true
}
</script>

<style scoped>
.markdown-content {
  line-height: 1.6;
  color: #333;
  word-wrap: break-word;
}

/* 全局样式 */
.markdown-content :deep(h1),
.markdown-content :deep(h2),
.markdown-content :deep(h3),
.markdown-content :deep(h4),
.markdown-content :deep(h5),
.markdown-content :deep(h6) {
  margin: 1.5em 0 0.5em 0;
  font-weight: 600;
  line-height: 1.25;
}

.markdown-content :deep(h1) {
  font-size: 1.5em;
  border-bottom: 1px solid #eee;
  padding-bottom: 0.3em;
}

.markdown-content :deep(h2) {
  font-size: 1.3em;
  border-bottom: 1px solid #eee;
  padding-bottom: 0.3em;
}

.markdown-content :deep(h3) {
  font-size: 1.1em;
}

.markdown-content :deep(p) {
  margin: 0.8em 0;
}

.markdown-content :deep(ul),
.markdown-content :deep(ol) {
  margin: 0.8em 0;
  padding-left: 1.5em;
}

.markdown-content :deep(li) {
  margin: 0.3em 0;
}

.markdown-content :deep(blockquote) {
  margin: 1em 0;
  padding: 0.5em 1em;
  border-left: 4px solid #ddd;
  background-color: #f9f9f9;
  color: #666;
}

.markdown-content :deep(code) {
  background-color: #f1f1f1;
  padding: 0.2em 0.4em;
  border-radius: 3px;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 0.9em;
}

.markdown-content :deep(pre) {
  background-color: #f8f8f8;
  border: 1px solid #e1e1e1;
  border-radius: 6px;
  padding: 1em;
  max-height: 520px;
  overflow: auto;
  margin: 1em 0;
  contain: layout;
}

.markdown-content :deep(pre code) {
  background-color: transparent;
  padding: 0;
  border-radius: 0;
  font-size: 0.9em;
  line-height: 1.4;
  white-space: pre;
  overflow-wrap: normal;
  word-break: normal;
}

.markdown-content :deep(table) {
  border-collapse: collapse;
  margin: 1em 0;
  width: 100%;
}

.markdown-content :deep(table th),
.markdown-content :deep(table td) {
  border: 1px solid #ddd;
  padding: 0.5em 0.8em;
  text-align: left;
}

.markdown-content :deep(table th) {
  background-color: #f5f5f5;
  font-weight: 600;
}

.markdown-content :deep(table tr:nth-child(even)) {
  background-color: #f9f9f9;
}

.markdown-content :deep(a) {
  color: #1890ff;
  text-decoration: none;
}

.markdown-content :deep(a:hover) {
  text-decoration: underline;
}

.markdown-content :deep(img) {
  max-width: 100%;
  height: auto;
  border-radius: 4px;
  margin: 0.5em 0;
}

.markdown-content :deep(hr) {
  border: none;
  border-top: 1px solid #eee;
  margin: 1.5em 0;
}

/* 代码高亮样式优化 */
.markdown-content :deep(.hljs) {
  background-color: #f8f8f8 !important;
  border-radius: 6px;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 0.9em;
  line-height: 1.4;
  padding: 1em;
  overflow-x: auto;
  font-size-adjust: none;
  text-rendering: optimizeLegibility;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

.markdown-content :deep(.hljs-keyword) {
  color: #d73a49;
  font-weight: 600;
}

.markdown-content :deep(.hljs-string) {
  color: #032f62;
}

.markdown-content :deep(.hljs-comment) {
  color: #6a737d;
  font-style: italic;
}

.markdown-content :deep(.hljs-number) {
  color: #005cc5;
}

.markdown-content :deep(.hljs-function) {
  color: #6f42c1;
}

.markdown-content :deep(.hljs-tag) {
  color: #22863a;
}

.markdown-content :deep(.hljs-attr) {
  color: #6f42c1;
}

.markdown-content :deep(.hljs-title) {
  color: #6f42c1;
  font-weight: 600;
}

/* ====== 工具调用项 - 扁平化独立行样式 ====== */
.markdown-content :deep(.tool-call-item) {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  margin: 4px 0;
  border-radius: 6px;
  background: #f5f5f5;
  font-size: 13px;
  transition: all 0.2s;
}

.markdown-content :deep(.tool-call-item .tci-icon) {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
}

.markdown-content :deep(.tool-call-item .tci-icon svg) {
  width: 100%;
  height: 100%;
  display: block;
}

.markdown-content :deep(.tool-call-item .tci-label) {
  background: #f0f0f0;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
  letter-spacing: 0.3px;
  color: #666;
  font-weight: 600;
  flex-shrink: 0;
}

.markdown-content :deep(.tool-call-item .tci-text) {
  color: #1a1a1a;
  font-weight: 500;
  letter-spacing: -0.2px;
  word-break: break-all;
}

/* 已配对的调用项（灰色调表示已完成调用阶段） */
.markdown-content :deep(.tool-call-paired) {
  background: #fafafa;
  opacity: 0.7;
}

.markdown-content :deep(.tool-call-paired .tci-label) {
  background: #e8e8e8;
  color: #999;
}

/* 完成项 */
.markdown-content :deep(.tool-call-done) {
  background: #f6ffed;
}

.markdown-content :deep(.tool-call-done .tci-label) {
  background: #d9f7be;
  color: #389e0d;
}

.markdown-content :deep(.tool-call-done .tci-text) {
  color: #1a1a1a;
}

/* 详情项（如搜索关键词） */
.markdown-content :deep(.tool-call-detail) {
  background: #fafafa;
  padding-left: 44px;
}

.markdown-content :deep(.tool-call-detail .tci-text) {
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 12.5px;
  color: #555;
}

/* 加载中项（流式传输未完成） */
.markdown-content :deep(.tool-call-loading) {
  background: #f5f5f5;
  position: relative;
  overflow: hidden;
}

.markdown-content :deep(.tool-call-loading .tci-icon) {
  animation: toolItemSpin 1s linear infinite;
  transform-origin: center;
}

.markdown-content :deep(.tool-call-loading .tci-label) {
  background: #e0e0e0;
  color: #888;
}

.markdown-content :deep(.tool-call-loading::after) {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  height: 2px;
  background: linear-gradient(90deg, transparent, #1a1a1a, transparent);
  background-size: 200% 100%;
  animation: toolItemProgress 1.5s ease-in-out infinite;
}

@keyframes toolItemProgress {
  0% {
    background-position: 100% 0;
  }
  100% {
    background-position: -100% 0;
  }
}

@keyframes toolItemSpin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

/* 工作流标记样式 */
.markdown-content :deep(.workflow-start-marker),
.markdown-content :deep(.workflow-execution-marker),
.markdown-content :deep(.workflow-complete-marker),
.markdown-content :deep(.workflow-error-marker),
.markdown-content :deep(.node-start-marker),
.markdown-content :deep(.node-end-marker) {
  display: none;
}

/* 工作流元信息样式 */
.markdown-content :deep(.workflow-meta) {
  background: linear-gradient(135deg, #f5f7fa 0%, #e8ecf1 100%);
  border-radius: 12px;
  padding: 16px 20px;
  margin: 16px 0;
  border: 1px solid #e1e8ed;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.markdown-content :deep(.workflow-meta table) {
  margin: 0;
  background: white;
  border-radius: 8px;
  overflow: hidden;
}

.markdown-content :deep(.workflow-meta table th) {
  background: #f8f9fa;
  color: #495057;
  font-weight: 600;
  font-size: 13px;
  padding: 10px 14px;
}

.markdown-content :deep(.workflow-meta table td) {
  padding: 10px 14px;
  font-size: 13px;
}

/* 工作流步骤列表样式 */
.markdown-content :deep(.workflow-steps) {
  background: #f8f9fa;
  border-radius: 10px;
  padding: 16px 20px;
  margin: 16px 0;
  border-left: 4px solid #4CAF50;
}

.markdown-content :deep(.workflow-steps ul),
.markdown-content :deep(.workflow-steps ol) {
  margin: 8px 0;
  padding-left: 0;
  list-style: none;
}

.markdown-content :deep(.workflow-steps li) {
  padding: 6px 0;
  font-size: 14px;
  color: #495057;
  position: relative;
  padding-left: 28px;
}

.markdown-content :deep(.workflow-steps li::before) {
  content: '';
  position: absolute;
  left: 8px;
  top: 50%;
  transform: translateY(-50%);
  width: 6px;
  height: 6px;
  background: #4CAF50;
  border-radius: 50%;
}

/* 节点结果样式 */
.markdown-content :deep(.node-result) {
  background: white;
  border-radius: 10px;
  padding: 16px 20px;
  margin: 12px 0;
  border: 1px solid #e9ecef;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;
}

.markdown-content :deep(.node-result:hover) {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  border-color: #dee2e6;
}

.markdown-content :deep(.node-result) strong {
  color: #212529;
  font-weight: 600;
}

.markdown-content :deep(.node-result) ul {
  margin: 8px 0;
  padding-left: 20px;
}

.markdown-content :deep(.node-result) li {
  margin: 6px 0;
  color: #495057;
  font-size: 14px;
  line-height: 1.6;
}

/* 工作流总结样式 */
.markdown-content :deep(.workflow-summary) {
  background: linear-gradient(135deg, #e8f5e9 0%, #c8e6c9 100%);
  border-radius: 12px;
  padding: 20px 24px;
  margin: 16px 0;
  border: 2px solid #81c784;
  box-shadow: 0 4px 12px rgba(76, 175, 80, 0.15);
}

.markdown-content :deep(.workflow-summary) strong {
  color: #2e7d32;
  font-size: 16px;
}

.markdown-content :deep(.workflow-summary) ul {
  margin: 12px 0;
  padding-left: 0;
  list-style: none;
}

.markdown-content :deep(.workflow-summary) li {
  padding: 6px 0;
  color: #388e3c;
  font-size: 14px;
  position: relative;
  padding-left: 28px;
}

.markdown-content :deep(.workflow-summary) li::before {
  content: '✓';
  position: absolute;
  left: 8px;
  color: #4CAF50;
  font-weight: bold;
  font-size: 16px;
}

/* 工作流错误样式 */
.markdown-content :deep(.workflow-error) {
  background: linear-gradient(135deg, #ffebee 0%, #ffcdd2 100%);
  border-radius: 12px;
  padding: 20px 24px;
  margin: 16px 0;
  border: 2px solid #ef5350;
  box-shadow: 0 4px 12px rgba(244, 67, 54, 0.15);
}

.markdown-content :deep(.workflow-error) strong {
  color: #c62828;
  font-size: 15px;
}

.markdown-content :deep(.workflow-error) pre {
  background: #fff;
  border: 1px solid #ef9a9a;
  margin: 12px 0;
}

.markdown-content :deep(.workflow-error) ul,
.markdown-content :deep(.workflow-error) ol {
  margin: 12px 0;
  color: #d32f2f;
}

/* 增强标题样式 - 工作流专用 */
.markdown-content :deep(h1) {
  color: #212529;
  font-size: 1.6em;
  margin-top: 1.2em;
  margin-bottom: 0.8em;
  font-weight: 700;
  letter-spacing: -0.02em;
}

.markdown-content :deep(h2) {
  color: #343a40;
  font-size: 1.35em;
  margin-top: 1.2em;
  margin-bottom: 0.6em;
  font-weight: 600;
}

.markdown-content :deep(h3) {
  color: #495057;
  font-size: 1.15em;
  margin-top: 1em;
  margin-bottom: 0.5em;
  font-weight: 600;
}

/* 引用块样式增强 */
.markdown-content :deep(blockquote) {
  border-left: 4px solid #4CAF50;
  background: linear-gradient(90deg, #f1f8f4 0%, #f8f9fa 100%);
  color: #495057;
  padding: 12px 16px;
  margin: 12px 0;
  border-radius: 0 6px 6px 0;
  font-size: 14px;
}

/* 分隔线样式增强 */
.markdown-content :deep(hr) {
  border: none;
  height: 2px;
  background: linear-gradient(90deg, transparent 0%, #dee2e6 50%, transparent 100%);
  margin: 24px 0;
}

/* 代码块样式优化 - 工作流输出 */
.markdown-content :deep(code) {
  background: #f1f3f5;
  color: #495057;
  padding: 2px 6px;
  border-radius: 4px;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 0.88em;
  border: 1px solid #e9ecef;
}

.markdown-content :deep(pre) {
  background: #f8f9fa;
  border: 1px solid #dee2e6;
  border-radius: 8px;
  padding: 14px 16px;
  max-height: 520px;
  overflow: auto;
  margin: 12px 0;
  box-shadow: inset 0 1px 3px rgba(0, 0, 0, 0.05);
  contain: layout;
}

/* 单文件模式长代码块折叠 */
.markdown-content :deep(.cw-code-collapsed) {
  margin: 12px 0;
  overflow: hidden;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  background: #fafafa;
  contain: layout style;
}

.markdown-content :deep(.cw-code-collapsed__bar) {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
}

.markdown-content :deep(.cw-code-collapsed__icon) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 34px;
  height: 34px;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 11px;
  font-weight: 700;
  color: #595959;
  background: #fff;
}

.markdown-content :deep(.cw-code-collapsed__copy) {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-width: 0;
  gap: 2px;
}

.markdown-content :deep(.cw-code-collapsed__title) {
  overflow: hidden;
  font-size: 13px;
  font-weight: 600;
  color: #1a1a1a;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.markdown-content :deep(.cw-code-collapsed__meta) {
  overflow: hidden;
  font-size: 11px;
  color: #999;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.markdown-content :deep(.cw-code-open-btn) {
  flex-shrink: 0;
  padding: 6px 12px;
  border: 1px solid #1a1a1a;
  border-radius: 7px;
  font-size: 12px;
  font-weight: 600;
  line-height: 18px;
  color: #fff;
  background: #1a1a1a;
  cursor: pointer;
  transition: background 0.2s ease, border-color 0.2s ease;
}

.markdown-content :deep(.cw-code-open-btn:hover) {
  border-color: #333;
  background: #333;
}

.markdown-content :deep(.cw-code-open-btn:focus-visible) {
  outline: 2px solid #4096ff;
  outline-offset: 2px;
}

.cw-code-viewer-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
  color: #999;
  font-size: 12px;

  span {
    padding: 2px 8px;
    border-radius: 4px;
    background: #f5f5f5;
  }
}

.cw-code-viewer-pre {
  max-height: 70vh;
  margin: 0;
  overflow: auto;
  padding: 16px;
  border: 1px solid #e5e5e5;
  border-radius: 8px;
  background: #f8f9fa;

  code {
    display: block;
    min-width: max-content;
    padding: 0;
    border: none;
    background: transparent;
    font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
    font-size: 12px;
    line-height: 1.55;
    white-space: pre;
  }
}

/* ====== 任务列表折叠容器样式 ====== */
.markdown-content :deep(.task-list-wrapper) {
  margin: 4px 0 8px 44px;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  overflow: hidden;
  background: #fafbfc;
}

.markdown-content :deep(.task-list-toggle) {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  cursor: pointer;
  background: #f5f5f5;
  border-bottom: 1px solid #e8e8e8;
  user-select: none;
  transition: background 0.2s;
}

.markdown-content :deep(.task-list-toggle:hover) {
  background: #ebebeb;
}

.markdown-content :deep(.task-list-toggle .toggle-icon) {
  font-size: 10px;
  color: #666;
  transition: transform 0.2s;
}

.markdown-content :deep(.task-list-toggle .toggle-text) {
  font-size: 12px;
  color: #666;
  font-weight: 500;
}

.markdown-content :deep(.task-list-content) {
  padding: 8px 12px;
  max-height: 200px;
  overflow-y: auto;
  transition: max-height 0.3s ease, padding 0.3s ease, opacity 0.3s ease;
}

/* 折叠状态 */
.markdown-content :deep(.task-list-wrapper.collapsed .task-list-content) {
  max-height: 0;
  padding: 0 12px;
  opacity: 0;
  overflow: hidden;
}

.markdown-content :deep(.task-list-wrapper.collapsed .task-list-toggle) {
  border-bottom: none;
}

/* ====== 任务项样式 ====== */
.markdown-content :deep(.task-item) {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 8px;
  border-radius: 6px;
  font-size: 12px;
  margin: 2px 0;
  transition: background 0.2s;
}

.markdown-content :deep(.task-item:hover) {
  background: rgba(0, 0, 0, 0.04);
}

.markdown-content :deep(.task-item .task-icon) {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 14px;
  height: 14px;
}

.markdown-content :deep(.task-item .task-icon svg) {
  width: 100%;
  height: 100%;
  display: block;
}

.markdown-content :deep(.task-item .task-index) {
  color: #999;
  font-size: 11px;
  min-width: 24px;
}

.markdown-content :deep(.task-item .task-step) {
  flex: 1;
  color: #333;
  font-weight: 500;
}

.markdown-content :deep(.task-item .task-label) {
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 4px;
  font-weight: 600;
}

.markdown-content :deep(.task-item .task-notes) {
  font-size: 11px;
  color: #666;
  font-style: italic;
}

/* 任务状态样式 */
.markdown-content :deep(.task-completed) {
  background: #f6ffed;
}

.markdown-content :deep(.task-completed .task-step) {
  color: #389e0d;
}

.markdown-content :deep(.task-completed .task-label) {
  background: #d9f7be;
  color: #389e0d;
}

.markdown-content :deep(.task-in-progress) {
  background: #e6f7ff;
}

.markdown-content :deep(.task-in-progress .task-step) {
  color: #1890ff;
}

.markdown-content :deep(.task-in-progress .task-label) {
  background: #bae7ff;
  color: #1890ff;
}

.markdown-content :deep(.task-pending) {
  background: #fff;
}

.markdown-content :deep(.task-pending .task-step) {
  color: #666;
}

.markdown-content :deep(.task-pending .task-label) {
  background: #f0f0f0;
  color: #999;
}

/* ====== 思考内容折叠样式 ====== */
.markdown-content :deep(.think-content-wrapper) {
  margin: 4px 0 8px 44px;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  overflow: hidden;
  background: #fefefe;
}

.markdown-content :deep(.think-content-toggle) {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  cursor: pointer;
  background: #f9f9f9;
  border-bottom: 1px solid #e8e8e8;
  user-select: none;
  transition: background 0.2s;
}

.markdown-content :deep(.think-content-toggle:hover) {
  background: #f0f0f0;
}

.markdown-content :deep(.think-content-toggle .toggle-icon) {
  font-size: 10px;
  color: #666;
}

.markdown-content :deep(.think-content-toggle .toggle-text) {
  font-size: 12px;
  color: #555;
  font-weight: 500;
}

.markdown-content :deep(.think-content-body) {
  padding: 12px;
  max-height: 300px;
  overflow-y: auto;
  transition: max-height 0.3s ease, padding 0.3s ease, opacity 0.3s ease;
}

.markdown-content :deep(.think-content-wrapper.collapsed .think-content-body) {
  max-height: 0;
  padding: 0 12px;
  opacity: 0;
  overflow: hidden;
}

.markdown-content :deep(.think-content-wrapper.collapsed .think-content-toggle) {
  border-bottom: none;
}

.markdown-content :deep(.think-conclusion) {
  font-size: 13px;
  color: #444;
  line-height: 1.6;
  white-space: pre-wrap;
}

.markdown-content :deep(.think-section-title) {
  display: block;
  color: #1a1a1a;
  font-size: 13px;
  font-weight: 600;
  margin: 8px 0 4px 0;
  padding-bottom: 4px;
  border-bottom: 1px solid #eee;
}

.markdown-content :deep(.think-section-title:first-child) {
  margin-top: 0;
}

/* Ant Design 图标通用样式 */
.markdown-content :deep(.antd-icon) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  vertical-align: -0.125em;
}

.markdown-content :deep(.antd-icon svg) {
  display: block;
}

.markdown-content :deep(.tool-type-icon) {
  margin-right: 4px;
}

.markdown-content :deep(.think-icon),
.markdown-content :deep(.conclusion-icon) {
  margin-right: 6px;
  vertical-align: middle;
}

.markdown-content :deep(.plan-icon) {
  margin-right: 4px;
}

/* 工具调用加载中动画 */
.markdown-content :deep(.tool-call-loading .tci-icon svg) {
  animation: rotate 1s linear infinite;
}

@keyframes rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

/* 任务列表滚动目标标记 */
.markdown-content :deep(.task-scroll-target) {
  scroll-margin-top: 8px;
}
</style>
