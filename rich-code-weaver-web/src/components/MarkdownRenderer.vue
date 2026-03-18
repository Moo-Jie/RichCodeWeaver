<template>
  <div class="markdown-content" v-html="renderedMarkdown"></div>
</template>

<script lang="ts" setup>
import {computed, nextTick, onMounted, watch} from 'vue'
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js'
import 'highlight.js/styles/github.css'

interface Props {
  content: string
}

const props = defineProps<Props>()

const md: MarkdownIt = new MarkdownIt({
  html: true,
  linkify: true,
  typographer: true,
  highlight: function (str: string, lang: string): string {
    if (lang && hljs.getLanguage(lang)) {
      try {
        return (
          '<pre class="hljs"><code>' +
          hljs.highlight(str, {language: lang, ignoreIllegals: true}).value +
          '</code></pre>'
        )
      } catch {
        // 忽略错误，使用默认处理
      }
    }

    return '<pre class="hljs"><code>' + md.utils.escapeHtml(str) + '</code></pre>'
  }
})

// 预处理Markdown内容，识别并包裹工具调用信息和工作流标记
const preprocessMarkdown = (content: string): string => {
  if (!content) return ''

  // 识别并包裹工作流开始标记
  content = content.replace(
    /<!-- WORKFLOW_START -->/g,
    '<div class="workflow-start-marker"></div>'
  )

  // 识别并包裹工作流执行开始标记
  content = content.replace(
    /<!-- WORKFLOW_EXECUTION_START -->/g,
    '<div class="workflow-execution-marker"></div>'
  )

  // 识别并包裹工作流完成标记
  content = content.replace(
    /<!-- WORKFLOW_COMPLETE -->/g,
    '<div class="workflow-complete-marker"></div>'
  )

  // 识别并包裹工作流错误标记
  content = content.replace(
    /<!-- WORKFLOW_ERROR -->/g,
    '<div class="workflow-error-marker"></div>'
  )

  // 识别并包裹节点开始/结束标记
  content = content.replace(
    /<!-- NODE_START:(\w+) -->/g,
    '<div class="node-start-marker" data-node="$1"></div>'
  )
  content = content.replace(
    /<!-- NODE_END:(\w+) -->/g,
    '<div class="node-end-marker" data-node="$1"></div>'
  )

  // 识别并包裹工具调用开始标记
  content = content.replace(
    /\[开始调用系统工具\]/g,
    '<div class="tool-call-start">[开始调用系统工具]</div>'
  )

  // 识别并包裹工具调用结束标记
  content = content.replace(
    /\[工具调用结束\]/g,
    '<div class="tool-call-end">[工具调用结束]</div>'
  )

  // 包裹工具调用之间的内容
  content = content.replace(
    /<div class="tool-call-start">[^<]*<\/div>([\s\S]*?)<div class="tool-call-end">[^<]*<\/div>/g,
    (_, toolContent) =>
      `<div class="tool-call">${toolContent}</div>`
  )

  // 修复代码块格式
  content = content.replace(/( {4}```[\s\S]*? {4}```)/g, (match) => {
    return match.replace(/^ {4}/gm, '')
  })

  // 确保代码块有正确的换行
  content = content.replace(/```(\w+)\s*\n([\s\S]*?)```/g, '```$1\n$2```')

  return content
}

const renderedMarkdown = computed(() => {
  const processedContent = preprocessMarkdown(props.content)
  return md.render(processedContent)
})

onMounted(() => {
  highlightCode()
})

watch(() => props.content, () => {
  nextTick(() => {
    highlightCode()
  })
})

const highlightCode = () => {
  requestAnimationFrame(() => {
    document.querySelectorAll('.markdown-content pre code').forEach((block) => {
      if (!block.classList.contains('hljs')) {
        hljs.highlightElement(block as HTMLElement)
      }
    })
  })
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
  overflow-x: auto;
  margin: 1em 0;
}

.markdown-content :deep(pre code) {
  background-color: transparent;
  padding: 0;
  border-radius: 0;
  font-size: 0.9em;
  line-height: 1.4;
  white-space: pre-wrap;
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

/* 工具调用样式 */
.markdown-content :deep(.tool-call-start) {
  display: inline-block;
  background-color: #e6f7ff;
  color: #1890ff;
  padding: 0.2em 0.5em;
  border-radius: 4px;
  margin: 0.5em 0;
  font-weight: bold;
  font-family: monospace;
}

.markdown-content :deep(.tool-call-end) {
  display: inline-block;
  background-color: #f6ffed;
  color: #52c41a;
  padding: 0.2em 0.5em;
  border-radius: 4px;
  margin: 0.5em 0;
  font-weight: bold;
  font-family: monospace;
}

.markdown-content :deep(.tool-call) {
  background-color: #f0f7ff;
  border-left: 4px solid #1890ff;
  padding: 0.8em 1.2em;
  margin: 1.2em 0;
  border-radius: 0 6px 6px 0;
  font-family: monospace;
  position: relative;
}

.markdown-content :deep(.tool-call:before) {
  content: "< 工具调用 >";
  position: absolute;
  top: -0.8em;
  left: 0;
  background: #1890ff;
  color: white;
  font-size: 0.8em;
  padding: 0.2em 0.8em;
  border-radius: 4px 4px 0 0;
}

.markdown-content :deep(.tool-call pre) {
  background-color: #e6f7ff;
  border: 1px solid #91d5ff;
}

.markdown-content :deep(.tool-call code) {
  background-color: transparent;
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
  overflow-x: auto;
  margin: 12px 0;
  box-shadow: inset 0 1px 3px rgba(0, 0, 0, 0.05);
}
</style>
