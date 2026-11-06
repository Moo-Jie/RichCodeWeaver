<template>
  <div
    class="markdown-content"
    :class="{ 'cw-stream-markdown': autoCollapseSections }"
    @click="handleContentClick"
  >
    <div
      v-for="section in renderedSections"
      :key="section.key"
      v-memo="[section.html]"
      class="cw-markdown-segment"
      v-html="section.html"
    ></div>
  </div>
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
import { computed, onUnmounted, ref, watch } from 'vue'
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js/lib/core'
import bash from 'highlight.js/lib/languages/bash'
import css from 'highlight.js/lib/languages/css'
import dockerfile from 'highlight.js/lib/languages/dockerfile'
import java from 'highlight.js/lib/languages/java'
import javascript from 'highlight.js/lib/languages/javascript'
import json from 'highlight.js/lib/languages/json'
import less from 'highlight.js/lib/languages/less'
import markdown from 'highlight.js/lib/languages/markdown'
import nginx from 'highlight.js/lib/languages/nginx'
import python from 'highlight.js/lib/languages/python'
import scss from 'highlight.js/lib/languages/scss'
import sql from 'highlight.js/lib/languages/sql'
import typescript from 'highlight.js/lib/languages/typescript'
import xml from 'highlight.js/lib/languages/xml'
import yaml from 'highlight.js/lib/languages/yaml'
import DOMPurify from 'dompurify'
import 'highlight.js/styles/github.css'
import { processToolCallMarkers } from '@/utils/toolCallParser'
import { fixCodeBlockFormat, processWorkflowMarkers } from '@/utils/workflowMarkerParser'

interface Props {
  content: string
  streaming?: boolean
  collapseLongCode?: boolean
  collapseLongCodeThreshold?: number
  collapseWorkflowSections?: boolean
  autoCollapseSections?: boolean
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
  sectionKey: string
  nextCodeBlockIndex: number
}

interface MarkdownSourceSection {
  key: string
  source: string
  level: number
  title: string
  order?: number
}

interface RenderedMarkdownSection {
  source: string
  renderKey: string
  html: string
  codeBlocks: Map<string, CodeBlockInfo>
}

const props = withDefaults(defineProps<Props>(), {
  streaming: false,
  collapseLongCode: false,
  collapseLongCodeThreshold: 1200,
  collapseWorkflowSections: true,
  autoCollapseSections: false,
})

hljs.registerLanguage('bash', bash)
hljs.registerLanguage('css', css)
hljs.registerLanguage('dockerfile', dockerfile)
hljs.registerLanguage('java', java)
hljs.registerLanguage('javascript', javascript)
hljs.registerLanguage('json', json)
hljs.registerLanguage('less', less)
hljs.registerLanguage('markdown', markdown)
hljs.registerLanguage('nginx', nginx)
hljs.registerLanguage('python', python)
hljs.registerLanguage('scss', scss)
hljs.registerLanguage('sql', sql)
hljs.registerLanguage('typescript', typescript)
hljs.registerLanguage('xml', xml)
hljs.registerLanguage('yaml', yaml)

const codeViewerOpen = ref(false)
const codeViewer = ref<CodeBlockInfo | null>(null)
const STREAM_RENDER_INTERVAL_MS = 32
const LARGE_STREAM_RENDER_INTERVAL_MS = 72
const LARGE_CONTENT_THRESHOLD = 24000
const MAX_HIGHLIGHT_CONTENT_LENGTH = 60000
const renderContent = ref(props.content)
let renderTimer: number | null = null

const flushRenderedContent = () => {
  if (renderTimer !== null) {
    clearTimeout(renderTimer)
    renderTimer = null
  }
  renderContent.value = props.content
}

watch(
  () => props.content,
  () => {
    if (!props.streaming) {
      flushRenderedContent()
      return
    }
    if (renderTimer !== null) return
    const delay =
      props.content.length >= LARGE_CONTENT_THRESHOLD
        ? LARGE_STREAM_RENDER_INTERVAL_MS
        : STREAM_RENDER_INTERVAL_MS
    renderTimer = window.setTimeout(() => {
      renderTimer = null
      if (renderContent.value !== props.content) renderContent.value = props.content
    }, delay)
  },
  { flush: 'sync' },
)

watch(
  () => props.streaming,
  (streaming) => {
    if (!streaming) flushRenderedContent()
  },
  { flush: 'sync' },
)

onUnmounted(() => {
  if (renderTimer !== null) {
    clearTimeout(renderTimer)
    renderTimer = null
  }
})

let renderedCodeBlocks = new Map<string, CodeBlockInfo>()
let sectionRenderCache = new Map<number, RenderedMarkdownSection>()

const sanitizeRenderedHtml = (html: string) => {
  return DOMPurify.sanitize(html, {
    USE_PROFILES: { html: true, svg: true, svgFilters: true },
    ADD_ATTR: ['target', 'rel', 'data-cw-code-key', 'open'],
  })
}

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
  sql: 'SQL',
}

const normalizeLanguage = (lang: string) => {
  const normalized = (lang || '').trim().toLowerCase()
  if (normalized === 'js') return 'javascript'
  if (normalized === 'ts') return 'typescript'
  if (normalized === 'html' || normalized === 'vue') return 'xml'
  if (normalized === 'sh' || normalized === 'shell') return 'bash'
  if (normalized === 'yml') return 'yaml'
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
  highlight: function (str: string, lang: string): string {
    const normalizedLang = normalizeLanguage(lang)
    if (
      !props.streaming &&
      renderContent.value.length <= MAX_HIGHLIGHT_CONTENT_LENGTH &&
      normalizedLang &&
      hljs.getLanguage(normalizedLang)
    ) {
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
  },
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
    const key = `cw-code-${renderEnv.sectionKey}-${renderEnv.nextCodeBlockIndex++}`
    const languageLabel = getLanguageLabel(language)
    const codeBlock: CodeBlockInfo = {
      key,
      code,
      language,
      languageLabel,
      lineCount,
      charCount: code.length,
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

const WORKFLOW_FOLD_HINTS = [
  '工作流初始化',
  '提示词增强',
  'AI 代码生成',
  '代码审查',
  '代码生成任务完成',
  '任务执行完成',
  '项目已完成',
  '项目构建',
  '交付检查',
]

const AUTO_FOLD_HEADING_PATTERN = /^(#{2,3})[ \t]+(.+?)[ \t]*#*[ \t]*$/

const stripHeadingHtml = (value: string) => {
  return value
    .replace(/<[^>]+>/g, ' ')
    .replace(/[\u{1F300}-\u{1FAFF}\u{2600}-\u{27BF}\uFE0F]/gu, ' ')
    .replace(/\s+/g, ' ')
    .trim()
}

const shouldFoldWorkflowSection = (title: string) => {
  return WORKFLOW_FOLD_HINTS.some((hint) => title.includes(hint))
}

// 将工作流模块转换为原生 details，最终输出默认收起，流式时只展开最后一个活动模块。
const foldWorkflowSections = (html: string, streaming: boolean) => {
  if (!props.collapseWorkflowSections) return html

  const headings = [...html.matchAll(/<h([1-3])([^>]*)>([\s\S]*?)<\/h\1>/gi)]
  if (headings.length === 0) return html

  const foldableIndexes = headings
    .map((heading, index) => (shouldFoldWorkflowSection(stripHeadingHtml(heading[3])) ? index : -1))
    .filter((index) => index >= 0)
  if (foldableIndexes.length === 0) return html

  const lastFoldableIndex = foldableIndexes[foldableIndexes.length - 1]
  let cursor = 0
  let output = ''

  for (let index = 0; index < headings.length; index++) {
    if (!foldableIndexes.includes(index)) continue

    const heading = headings[index]
    const start = heading.index ?? 0
    if (start < cursor) continue

    const end = headings[index + 1]?.index ?? html.length
    const title = stripHeadingHtml(heading[3])
    const body = html.slice(start + heading[0].length, end)
    const isActiveSection = streaming && index === lastFoldableIndex
    output += html.slice(cursor, start)
    output += `<details class="cw-workflow-fold cw-workflow-fold--h${heading[1]}"${isActiveSection ? ' open' : ''}>`
    output += `<summary class="cw-workflow-fold-summary"><span class="cw-workflow-fold-title"><span class="cw-workflow-fold-level">H${heading[1]}</span><span>${md.utils.escapeHtml(title)}</span></span><span class="cw-workflow-fold-mark" aria-hidden="true"></span></summary>`
    output += `<div class="cw-workflow-fold-body">${body}</div></details>`
    cursor = end
  }

  output += html.slice(cursor)
  return output
}

// 将连续 Markdown 按 h2-h3 切成稳定片段。H1 保留为文档标题，代码围栏内的 # 不参与切分。
const splitMarkdownSections = (markdown: string): MarkdownSourceSection[] => {
  if (!markdown) return []

  const sections: MarkdownSourceSection[] = []
  const lines = markdown.match(/[^\n]*(?:\n|$)/g) ?? []
  let activeFence = ''
  let sectionIndex = 0
  let stageOrder = 0
  let current: MarkdownSourceSection = {
    key: `section-${sectionIndex++}`,
    source: '',
    level: 0,
    title: '',
  }

  const flush = () => {
    if (current.source.trim()) sections.push(current)
  }

  for (const line of lines) {
    if (!line) continue
    const lineContent = line.endsWith('\n') ? line.slice(0, -1) : line

    const fenceMatch = lineContent.match(/^\s*(`{3,}|~{3,})/)
    if (fenceMatch) {
      const marker = fenceMatch[1]
      if (!activeFence) {
        activeFence = marker
      } else if (marker[0] === activeFence[0] && marker.length >= activeFence.length) {
        activeFence = ''
      }
      current.source += line
      continue
    }

    if (!activeFence && lineContent.trim() === '<!--CW_FINAL_STATUS-->') {
      flush()
      current = {
        key: `section-${sectionIndex++}`,
        source: '',
        level: 0,
        title: '',
      }
      continue
    }

    const headingMatch = !activeFence ? lineContent.match(AUTO_FOLD_HEADING_PATTERN) : null
    if (headingMatch) {
      flush()
      current = {
        key: `section-${sectionIndex++}`,
        source: '',
        level: headingMatch[1].length,
        title: stripHeadingHtml(md.renderInline(headingMatch[2].trim())),
        order: ++stageOrder,
      }
    }

    current.source += line
  }

  flush()
  return sections
}

const decorateConversationSemantics = (html: string) => {
  return html
    .replace(/<p>状态：([^<]*)<\/p>/g, (_match, status: string) => {
      const normalized = status.trim()
      const isError = /失败|未通过|中断|阻止/.test(normalized)
      return `<p class="cw-status-line${isError ? ' is-error' : ''}"><span class="cw-status-dot" aria-hidden="true"></span><span class="cw-status-label">状态</span><span class="cw-status-value">${md.utils.escapeHtml(normalized)}</span></p>`
    })
    .replace(/<blockquote>\s*<p>(生成完成[^<]*)<\/p>\s*<\/blockquote>/g, (_match, text: string) => {
      return `<blockquote class="cw-completion-note"><p>${text}</p></blockquote>`
    })
    .replace(
      /<blockquote>\s*<p>(生成未完成[^<]*)<\/p>\s*<\/blockquote>/g,
      (_match, text: string) => {
        return `<blockquote class="cw-error-note"><p>${text}</p></blockquote>`
      },
    )
}

const getStageIconMarkup = (title: string) => {
  let path = '<path d="M6 3.5h8l4 4V20H6V3.5Z"/><path d="M14 3.5v4h4M9 12h6M9 15.5h6"/>'
  if (title.includes('执行概览')) {
    path = '<path d="M5 6h14M5 12h14M5 18h9"/><circle cx="18" cy="18" r="2"/>'
  } else if (title.includes('资源')) {
    path =
      '<path d="m4 7.5 8-4.5 8 4.5-8 4.5-8-4.5Z"/><path d="m4 12 8 4.5 8-4.5M4 16.5l8 4.5 8-4.5"/>'
  } else if (title.includes('需求增强')) {
    path =
      '<path d="m11 3 1.5 4.5L17 9l-4.5 1.5L11 15l-1.5-4.5L5 9l4.5-1.5L11 3Z"/><path d="m18 14 .8 2.2L21 17l-2.2.8L18 20l-.8-2.2L15 17l2.2-.8L18 14Z"/>'
  } else if (title.includes('生成策略')) {
    path =
      '<circle cx="12" cy="12" r="7"/><circle cx="12" cy="12" r="2.4"/><path d="M12 3v3M12 18v3M3 12h3M18 12h3"/>'
  } else if (title.includes('代码生成')) {
    path = '<path d="m8 7-5 5 5 5M16 7l5 5-5 5M14 4l-4 16"/>'
  } else if (title.includes('结构检查')) {
    path = '<path d="M12 3 5 6v5c0 4.8 3 8 7 10 4-2 7-5.2 7-10V6l-7-3Z"/><path d="m9 12 2 2 4-4"/>'
  } else if (title.includes('质量审查')) {
    path = '<circle cx="11" cy="11" r="6.5"/><path d="m16 16 4 4M8 11h6"/>'
  } else if (title.includes('项目构建')) {
    path = '<path d="m4 7 8-4 8 4-8 4-8-4Z"/><path d="M4 7v10l8 4 8-4V7M12 11v10"/>'
  } else if (title.includes('交付检查')) {
    path = '<path d="M5 20V5m0 0h10l-2 3 2 3H5"/>'
  }
  return `<span class="cw-stage-icon" aria-hidden="true"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">${path}</svg></span>`
}

const wrapMarkdownSection = (
  section: MarkdownSourceSection,
  renderedHtml: string,
  index: number,
  lastIndex: number,
  lastStageIndex: number,
) => {
  const isActiveSection = props.streaming && index === lastIndex
  const cursor = isActiveSection ? '<span class="cw-typing-cursor" aria-hidden="true"></span>' : ''

  if (!section.level) return renderedHtml + cursor

  const headingPattern = new RegExp(
    `^\\s*<h${section.level}[^>]*>([\\s\\S]*?)<\\/h${section.level}>\\s*`,
    'i',
  )
  const headingMatch = renderedHtml.match(headingPattern)
  const body = headingMatch ? renderedHtml.slice(headingMatch[0].length) : renderedHtml
  const status = section.source.match(/(?:^|\n)状态：([^\n]+)/)?.[1]?.trim() || ''
  const isError = /失败|未通过|阻断|中断/.test(status)
  const isWarning = /需修复|警告/.test(status)
  const state = isError ? 'error' : isWarning ? 'warning' : isActiveSection ? 'active' : 'completed'
  const stateLabel =
    state === 'error'
      ? '异常'
      : state === 'warning'
        ? '需修复'
        : state === 'active'
          ? '进行中'
          : '已完成'
  const railSymbol =
    state === 'completed'
      ? '<span class="cw-stage-check"></span>'
      : state === 'active'
        ? '<span class="cw-stage-dot"></span>'
        : '<span class="cw-stage-rail-label">!</span>'
  const placeholder =
    isActiveSection && !body.trim() ? '<p class="cw-workflow-fold-placeholder">正在生成...</p>' : ''
  const orderLabel = section.order ? String(section.order).padStart(2, '0') : ''
  const stageIcon = getStageIconMarkup(section.title)
  const isLastStage = index === lastStageIndex

  return (
    `<div class="cw-stage-shell cw-stage-shell--h${section.level}${isLastStage ? ' is-last-stage' : ''}">` +
    `<span class="cw-stage-rail-node is-${state}" aria-label="${stateLabel}">${railSymbol}</span>` +
    `<details class="cw-workflow-fold cw-workflow-fold--h${section.level}"${isActiveSection ? ' open' : ''}>` +
    `<summary class="cw-workflow-fold-summary"><span class="cw-workflow-fold-title">${stageIcon}${orderLabel ? `<span class="cw-stage-index">${orderLabel}</span>` : ''}<span class="cw-stage-title">${md.utils.escapeHtml(section.title)}</span></span><span class="cw-workflow-fold-tail"><span class="cw-workflow-fold-mark" aria-hidden="true"></span></span></summary>` +
    `<div class="cw-workflow-fold-body${isActiveSection ? ' is-streaming' : ''}">${body}${placeholder}</div></details></div>`
  )
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

const renderedSections = computed(() => {
  const processedContent = preprocessMarkdown(renderContent.value)
  if (!processedContent) return []

  const sourceSections: MarkdownSourceSection[] = props.autoCollapseSections
    ? splitMarkdownSections(processedContent)
    : [{ key: 'document', source: processedContent, level: 0, title: '' }]
  const nextCache = new Map<number, RenderedMarkdownSection>()
  const allCodeBlocks = new Map<string, CodeBlockInfo>()
  const highlightEnabled =
    !props.streaming && renderContent.value.length <= MAX_HIGHLIGHT_CONTENT_LENGTH
  const renderKey = `${highlightEnabled}|${props.collapseLongCode}|${props.collapseLongCodeThreshold}`
  const lastSectionIndex = sourceSections.length - 1
  const lastStageIndex = sourceSections.reduce(
    (last, section, index) => (section.level > 0 ? index : last),
    -1,
  )

  const sections = sourceSections.map((section, index) => {
    let renderedSection = sectionRenderCache.get(index)
    if (
      !renderedSection ||
      renderedSection.source !== section.source ||
      renderedSection.renderKey !== renderKey
    ) {
      const codeBlocks = new Map<string, CodeBlockInfo>()
      const renderEnv: MarkdownRenderEnv = {
        codeBlocks,
        sectionKey: section.key,
        nextCodeBlockIndex: 0,
      }
      renderedSection = {
        source: section.source,
        renderKey,
        html: sanitizeRenderedHtml(md.render(section.source, renderEnv)),
        codeBlocks,
      }
    }

    nextCache.set(index, renderedSection)
    renderedSection.codeBlocks.forEach((block, key) => allCodeBlocks.set(key, block))

    let html = renderedSection.html
    if (props.autoCollapseSections) {
      html = decorateConversationSemantics(
        wrapMarkdownSection(section, html, index, lastSectionIndex, lastStageIndex),
      )
    } else {
      html = foldWorkflowSections(html, props.streaming)
    }

    return {
      key: section.key,
      html,
    }
  })

  sectionRenderCache = nextCache
  renderedCodeBlocks = allCodeBlocks
  return sections
})

const codeViewerTitle = computed(() => {
  return codeViewer.value ? `${codeViewer.value.languageLabel} 完整代码` : '完整代码'
})

const highlightedViewerCode = computed(() => {
  const code = codeViewer.value?.code || ''
  const language = normalizeLanguage(codeViewer.value?.language || '')
  if (language && hljs.getLanguage(language)) {
    try {
      return sanitizeRenderedHtml(hljs.highlight(code, { language, ignoreIllegals: true }).value)
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
.cw-markdown-segment {
  display: contents;
}

.markdown-content {
  line-height: 1.6;
  color: #333;
  word-wrap: break-word;
  min-width: 0;
  max-width: 100%;
  overflow-wrap: anywhere;
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
  border-left: 4px solid #4caf50;
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
  background: #4caf50;
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
  color: #4caf50;
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
  border-left: 4px solid #4caf50;
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
  transition:
    background 0.2s ease,
    border-color 0.2s ease;
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
  transition:
    max-height 0.3s ease,
    padding 0.3s ease,
    opacity 0.3s ease;
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
  transition:
    max-height 0.3s ease,
    padding 0.3s ease,
    opacity 0.3s ease;
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

/* Workflow sections: native details, monochrome and compact */
.markdown-content :deep(.cw-workflow-fold) {
  margin: 10px 0;
  overflow: hidden;
  border: 1px solid #ececec;
  border-radius: 10px;
  background: #fafafa;
}

.markdown-content :deep(.cw-workflow-fold-summary) {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 9px 12px;
  list-style: none;
  font-size: 12.5px;
  font-weight: 600;
  line-height: 1.45;
  color: #444;
  cursor: pointer;
  user-select: none;
  transition:
    background 150ms ease,
    color 150ms ease;
}

.markdown-content :deep(.cw-workflow-fold-summary:hover) {
  color: #1a1a1a;
  background: #f5f5f5;
}

.markdown-content :deep(.cw-workflow-fold-title) {
  display: flex;
  align-items: center;
  min-width: 0;
  gap: 8px;
}

.markdown-content :deep(.cw-workflow-fold-level) {
  flex-shrink: 0;
  width: 17px;
  color: #aaa;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 9.5px;
  font-weight: 500;
  letter-spacing: 0;
}

.markdown-content :deep(.cw-workflow-fold--h2 .cw-workflow-fold-summary) {
  padding-left: 20px;
}

.markdown-content :deep(.cw-workflow-fold--h3 .cw-workflow-fold-summary) {
  padding-left: 28px;
}

.markdown-content :deep(.cw-workflow-fold-summary::-webkit-details-marker) {
  display: none;
}

.markdown-content :deep(.cw-workflow-fold-mark) {
  flex-shrink: 0;
  width: 7px;
  height: 7px;
  border-right: 1.5px solid #777;
  border-bottom: 1.5px solid #777;
  transform: rotate(45deg);
  transition: transform 150ms ease;
}

.markdown-content :deep(.cw-workflow-fold[open] .cw-workflow-fold-mark) {
  transform: rotate(225deg);
}

.markdown-content :deep(.cw-workflow-fold-body) {
  padding: 0 12px 12px;
  border-top: 1px solid #ececec;
  background: #fff;
}

.markdown-content :deep(.cw-workflow-fold-placeholder) {
  margin: 10px 0 0;
  color: #999;
  font-size: 12px;
  line-height: 1.5;
}

.markdown-content :deep(.cw-workflow-fold-body h1),
.markdown-content :deep(.cw-workflow-fold-body h2),
.markdown-content :deep(.cw-workflow-fold-body h3) {
  font-size: 13px;
  font-weight: 600;
  color: #333;
}

.markdown-content :deep(.workflow-meta),
.markdown-content :deep(.workflow-steps),
.markdown-content :deep(.node-result),
.markdown-content :deep(.workflow-summary),
.markdown-content :deep(.workflow-error),
.markdown-content :deep(.tool-call-item),
.markdown-content :deep(.task-list-wrapper),
.markdown-content :deep(.think-content-wrapper) {
  background: #fafafa;
  border-color: #ececec;
  box-shadow: none;
}

.markdown-content :deep(.workflow-meta),
.markdown-content :deep(.node-result),
.markdown-content :deep(.workflow-summary),
.markdown-content :deep(.workflow-error) {
  border-radius: 8px;
}

.markdown-content :deep(.node-result:hover) {
  border-color: #e5e5e5;
  box-shadow: none;
}

.markdown-content :deep(.workflow-steps) {
  border-left-color: #d9d9d9;
}

.markdown-content :deep(.workflow-steps li::before),
.markdown-content :deep(.workflow-summary) li::before {
  background: #999;
  color: #777;
}

.markdown-content :deep(.node-result) strong,
.markdown-content :deep(.workflow-summary) strong,
.markdown-content :deep(.workflow-meta) strong,
.markdown-content :deep(.workflow-error) strong {
  color: #1a1a1a;
  font-size: 13px;
}

.markdown-content :deep(.node-result) li,
.markdown-content :deep(.workflow-summary) li,
.markdown-content :deep(.workflow-error) li {
  color: #666;
  font-size: 12px;
}

.markdown-content :deep(.workflow-meta table) {
  table-layout: fixed;
  width: 100%;
}

.markdown-content :deep(.workflow-meta td:last-child),
.markdown-content :deep(.workflow-meta th:last-child) {
  overflow-wrap: anywhere;
}

.markdown-content :deep(.tool-call-done),
.markdown-content :deep(.task-completed) {
  background: #fafafa;
}

.markdown-content :deep(.tool-call-done .tci-label),
.markdown-content :deep(.task-completed .task-label) {
  color: #666;
  background: #f0f0f0;
}

.markdown-content :deep(.task-in-progress) {
  background: #f5f5f5;
}

.markdown-content :deep(.task-in-progress .task-step) {
  color: #333;
}

.markdown-content :deep(.task-in-progress .task-label) {
  color: #666;
  background: #e8e8e8;
}

.markdown-content :deep(.task-pending .task-label) {
  color: #999;
  background: #f5f5f5;
}

.markdown-content :deep(blockquote) {
  border-left-color: #d9d9d9;
  background: #fafafa;
  color: #666;
}

.markdown-content :deep(.tool-call-item) {
  flex-wrap: wrap;
}

.markdown-content :deep(.tci-text) {
  min-width: 0;
  overflow-wrap: anywhere;
  word-break: normal;
}

@media (max-width: 768px) {
  .markdown-content :deep(.task-list-wrapper),
  .markdown-content :deep(.think-content-wrapper) {
    margin-left: 0;
  }

  .markdown-content :deep(.workflow-meta),
  .markdown-content :deep(.node-result),
  .markdown-content :deep(.workflow-summary),
  .markdown-content :deep(.workflow-error) {
    padding: 12px;
  }

  .markdown-content :deep(.workflow-meta table),
  .markdown-content :deep(table) {
    font-size: 12px;
  }

  .markdown-content :deep(table) {
    table-layout: fixed;
  }

  .markdown-content :deep(td),
  .markdown-content :deep(th) {
    overflow-wrap: anywhere;
  }
}
/* Conversation output: unified neutral system for both Agent and workflow modes. */
.cw-stream-markdown {
  color: #3f3f3f;
  font-size: 13.5px;
  line-height: 1.72;
}

.cw-stream-markdown :deep(h1) {
  margin: 0 0 12px;
  padding: 0 0 12px;
  border: 0;
  border-bottom: 1px solid #e8e8e8;
  color: #1a1a1a;
  font-size: 20px;
  font-weight: 650;
  line-height: 1.35;
  letter-spacing: -0.02em;
}

.cw-stream-markdown :deep(h2),
.cw-stream-markdown :deep(h3),
.cw-stream-markdown :deep(h4) {
  margin: 18px 0 8px;
  border: 0;
  color: #1f1f1f;
  font-weight: 600;
  line-height: 1.45;
  letter-spacing: -0.01em;
}

.cw-stream-markdown :deep(h2) {
  font-size: 15px;
}

.cw-stream-markdown :deep(h3),
.cw-stream-markdown :deep(h4) {
  font-size: 13.5px;
}

.cw-stream-markdown :deep(p) {
  margin: 8px 0;
  color: #444;
}

.cw-stream-markdown :deep(strong) {
  color: #1f1f1f;
  font-weight: 600;
}

.cw-stream-markdown :deep(ul),
.cw-stream-markdown :deep(ol) {
  margin: 8px 0 10px;
  padding-left: 18px;
}

.cw-stream-markdown :deep(li) {
  margin: 4px 0;
  padding-left: 2px;
  color: #555;
}

.cw-stream-markdown :deep(li::marker) {
  color: #999;
}

.cw-stream-markdown :deep(a) {
  color: #1890ff;
  text-underline-offset: 2px;
}

.cw-stream-markdown :deep(blockquote) {
  margin: 8px 0;
  padding: 7px 10px;
  border-left: 2px solid #d9d9d9;
  border-radius: 0;
  background: #fafafa;
  color: #666;
  font-size: 12px;
  line-height: 1.6;
}

.cw-stream-markdown :deep(blockquote p) {
  margin: 0;
  color: inherit;
  font-size: inherit;
}

.cw-stream-markdown :deep(.cw-completion-note) {
  margin-left: 28px;
  border-left-color: #1a1a1a;
  background: #f7f7f7;
  color: #333;
}

.cw-stream-markdown :deep(.cw-error-note) {
  margin-left: 28px;
  border-left-color: #ff4d4f;
  background: #fff7f5;
  color: #8a2c12;
}

.cw-stream-markdown :deep(.cw-completion-note p),
.cw-stream-markdown :deep(.cw-error-note p) {
  display: flex;
  align-items: center;
  gap: 8px;
}

.cw-stream-markdown :deep(.cw-completion-note p::before),
.cw-stream-markdown :deep(.cw-error-note p::before) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 16px;
  height: 16px;
  border: 1px solid #d9d9d9;
  border-radius: 50%;
  background-color: #f5f5f5;
  background-repeat: no-repeat;
  background-position: center;
  background-size: 10px 10px;
  content: '';
}

.cw-stream-markdown :deep(.cw-completion-note p::before) {
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 12 12'%3E%3Cpath d='M2.2 6.2 4.8 8.8 9.8 3.6' fill='none' stroke='%23333333' stroke-width='1.4' stroke-linecap='round' stroke-linejoin='round'/%3E%3C/svg%3E");
}

.cw-stream-markdown :deep(.cw-error-note p::before) {
  border-color: #ffccc7;
  background-color: #fff;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 12 12'%3E%3Cpath d='M6 2.4v4.8M6 9.6h.01' fill='none' stroke='%23cf1322' stroke-width='1.4' stroke-linecap='round'/%3E%3C/svg%3E");
}

.cw-stream-markdown :deep(hr) {
  height: 1px;
  margin: 18px 0;
  border: 0;
  background: #ececec;
}

.cw-stream-markdown :deep(code) {
  padding: 1px 5px;
  border: 0;
  border-radius: 4px;
  background: #f2f2f2;
  color: #333;
  font-size: 0.88em;
}

.cw-stream-markdown :deep(pre) {
  max-height: 440px;
  margin: 10px 0;
  padding: 12px 14px;
  border: 1px solid #ececec;
  border-radius: 8px;
  background: #fafafa;
  box-shadow: none;
}

.cw-stream-markdown :deep(pre code) {
  padding: 0;
  background: transparent;
}

.cw-stream-markdown :deep(table) {
  margin: 10px 0;
  border: 1px solid #ececec;
  border-radius: 8px;
  overflow: hidden;
}

.cw-stream-markdown :deep(th),
.cw-stream-markdown :deep(td) {
  padding: 7px 10px;
  border-color: #ececec;
  color: #555;
  font-size: 12px;
}

.cw-stream-markdown :deep(th) {
  background: #fafafa;
  color: #333;
  font-weight: 600;
}

.cw-stream-markdown :deep(tr:nth-child(even)) {
  background: transparent;
}

.cw-stream-markdown :deep(.cw-status-line) {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  margin: 2px 0 10px;
  color: #555;
  font-size: 11.5px;
  line-height: 1.5;
}

.cw-stream-markdown :deep(.cw-status-dot) {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: #1a1a1a;
  opacity: 0.72;
}

.cw-stream-markdown :deep(.cw-status-line.is-error .cw-status-dot) {
  background: #ff4d4f;
  opacity: 1;
}

.cw-stream-markdown :deep(.cw-status-label) {
  color: #aaa;
  font-size: 10px;
  letter-spacing: 0.08em;
}

.cw-stream-markdown :deep(.cw-status-value) {
  color: #444;
  font-weight: 600;
}

.cw-stream-markdown :deep(.cw-stage-shell) {
  position: relative;
  margin: 8px 0;
  padding-left: 28px;
}

.cw-stream-markdown :deep(.cw-stage-shell::before) {
  content: '';
  position: absolute;
  top: 14px;
  bottom: -18px;
  left: 8px;
  width: 1px;
  background: #e9e9e9;
}

.cw-stream-markdown :deep(.cw-stage-shell.is-last-stage::before) {
  bottom: calc(100% - 22px);
}

.cw-stream-markdown :deep(.cw-stage-rail-node) {
  position: absolute;
  top: 14px;
  left: 0;
  z-index: 2;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  border: 1px solid #d9d9d9;
  border-radius: 50%;
  color: #666;
  background: #fff;
}

.cw-stream-markdown :deep(.cw-stage-rail-node.is-completed) {
  border-color: #cfcfcf;
  background: #f5f5f5;
}

.cw-stream-markdown :deep(.cw-stage-rail-node.is-active) {
  border-color: #1a1a1a;
  background: #fff;
}

.cw-stream-markdown :deep(.cw-stage-rail-node.is-error) {
  border-color: #ffccc7;
  color: #cf1322;
  background: #fff7f5;
}

.cw-stream-markdown :deep(.cw-stage-rail-node.is-warning) {
  border-color: #d9d9d9;
  color: #555;
  background: #fafafa;
}

.cw-stream-markdown :deep(.cw-workflow-fold) {
  margin: 0;
  overflow: hidden;
  border: 1px solid #ececec;
  border-radius: 9px;
  background: #fcfcfc;
  transition:
    border-color 150ms ease,
    background 150ms ease,
    box-shadow 150ms ease;
}

.cw-stream-markdown :deep(.cw-workflow-fold[open]) {
  border-color: #dedede;
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.025);
}

.cw-stream-markdown :deep(.cw-workflow-fold-summary) {
  padding: 10px 12px;
  color: #4d4d4d;
  font-size: 12.5px;
  font-weight: 600;
  background: transparent;
}

.cw-stream-markdown :deep(.cw-workflow-fold-summary:hover) {
  color: #1a1a1a;
  background: #f7f7f7;
}

.cw-stream-markdown :deep(.cw-workflow-fold-title) {
  gap: 7px;
}

.cw-stream-markdown :deep(.cw-workflow-fold-tail) {
  display: inline-flex;
  align-items: center;
  flex-shrink: 0;
  gap: 9px;
}

.cw-stream-markdown :deep(.cw-stage-icon) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 26px;
  height: 26px;
  border: 1px solid #ececec;
  border-radius: 7px;
  color: #666;
  background: #f5f5f5;
}

.cw-stream-markdown :deep(.cw-stage-icon svg) {
  display: block;
  width: 14px;
  height: 14px;
}

.cw-stream-markdown :deep(.cw-workflow-fold[open] .cw-stage-icon) {
  border-color: #d9d9d9;
  color: #1a1a1a;
  background: #f0f0f0;
}

.cw-stream-markdown :deep(.cw-stage-index) {
  flex-shrink: 0;
  width: 17px;
  color: #b0b0b0;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 9.5px;
  font-weight: 500;
  letter-spacing: 0;
}

.cw-stream-markdown :deep(.cw-stage-title) {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cw-stream-markdown :deep(.cw-stage-check) {
  width: 4px;
  height: 7px;
  margin-top: -2px;
  border-right: 1.3px solid #333;
  border-bottom: 1.3px solid #333;
  transform: rotate(45deg);
}

.cw-stream-markdown :deep(.cw-stage-dot) {
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: #1a1a1a;
  animation: cw-stage-pulse 1.25s ease-in-out infinite;
}

.cw-stream-markdown :deep(.cw-stage-rail-label) {
  font-size: 10px;
  font-weight: 700;
  line-height: 1;
}

.cw-stream-markdown :deep(.cw-workflow-fold--h2 .cw-workflow-fold-summary) {
  padding-left: 12px;
}

.cw-stream-markdown :deep(.cw-workflow-fold--h3) {
  border-radius: 8px;
}

.cw-stream-markdown :deep(.cw-workflow-fold--h3 .cw-workflow-fold-summary) {
  padding-left: 10px;
  color: #666;
  font-size: 12px;
}

.cw-stream-markdown :deep(.cw-workflow-fold-mark) {
  width: 6px;
  height: 6px;
  border-right-width: 1px;
  border-bottom-width: 1px;
  border-color: #999;
}

.cw-stream-markdown :deep(.cw-workflow-fold-body) {
  padding: 10px 14px 14px;
  border-top: 1px solid #f0f0f0;
  background: #fff;
}

.cw-stream-markdown :deep(.cw-workflow-fold--h3 .cw-workflow-fold-body) {
  padding-left: 14px;
}

.cw-stream-markdown.is-streaming :deep(.cw-workflow-fold-body.is-streaming) {
  padding-bottom: 16px;
}

.markdown-content :deep(.cw-typing-cursor),
.cw-stream-markdown :deep(.cw-workflow-fold-body.is-streaming > :last-child::after) {
  display: inline-block;
  width: 1px;
  height: 1em;
  margin-left: 2px;
  vertical-align: -0.12em;
  background: #1a1a1a;
  content: '';
  animation: cw-caret-blink 900ms steps(1, end) infinite;
}

@keyframes cw-caret-blink {
  0%,
  46% {
    opacity: 1;
  }
  47%,
  100% {
    opacity: 0;
  }
}

@keyframes cw-stage-pulse {
  0%,
  100% {
    opacity: 0.45;
    transform: scale(0.82);
  }
  50% {
    opacity: 1;
    transform: scale(1);
  }
}

.cw-stream-markdown :deep(.cw-workflow-fold-placeholder) {
  margin: 2px 0 0;
  color: #aaa;
  font-size: 11.5px;
}

.cw-stream-markdown :deep(.tool-call-item) {
  padding: 6px 0;
  margin: 2px 0;
  border: 0;
  border-bottom: 1px solid #f2f2f2;
  border-radius: 0;
  background: transparent;
  font-size: 12px;
}

.cw-stream-markdown :deep(.tool-call-item .tci-icon),
.cw-stream-markdown :deep(.tool-type-icon),
.cw-stream-markdown :deep(.think-icon),
.cw-stream-markdown :deep(.conclusion-icon),
.cw-stream-markdown :deep(.plan-icon),
.cw-stream-markdown :deep(.task-icon) {
  display: none;
}

.cw-stream-markdown :deep(.tool-call-item .tci-label),
.cw-stream-markdown :deep(.tool-call-item .tci-text) {
  color: #666;
  background: transparent;
  font-size: 12px;
}

.cw-stream-markdown :deep(.workflow-meta),
.cw-stream-markdown :deep(.workflow-steps),
.cw-stream-markdown :deep(.node-result),
.cw-stream-markdown :deep(.workflow-summary),
.cw-stream-markdown :deep(.workflow-error),
.cw-stream-markdown :deep(.task-list-wrapper),
.cw-stream-markdown :deep(.think-content-wrapper) {
  margin: 8px 0;
  padding: 0;
  border: 0;
  border-radius: 0;
  background: transparent;
  box-shadow: none;
}

.cw-stream-markdown :deep(.task-list-toggle),
.cw-stream-markdown :deep(.think-content-toggle) {
  padding: 6px 0;
  border: 0;
  background: transparent;
}

.cw-stream-markdown :deep(.task-list-content),
.cw-stream-markdown :deep(.think-content-body) {
  padding: 4px 0 8px;
}

@media (max-width: 720px) {
  .cw-stream-markdown {
    font-size: 13px;
  }

  .cw-stream-markdown :deep(h1) {
    font-size: 18px;
  }

  .cw-stream-markdown :deep(.cw-workflow-fold-summary) {
    padding-top: 10px;
    padding-bottom: 10px;
  }

  .cw-stream-markdown :deep(.cw-workflow-fold--h3 .cw-workflow-fold-summary) {
    padding-left: 10px;
  }

  .cw-stream-markdown :deep(.cw-workflow-fold--h3 .cw-workflow-fold-body) {
    padding-left: 10px;
  }
}
</style>
