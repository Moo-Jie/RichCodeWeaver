/**
 * 工作流标记解析器
 * 负责解析工作流模式下的特殊标记，生成格式化的 HTML
 * 
 * 支持的标记类型：
 * - WORKFLOW_START / WORKFLOW_EXECUTION_START / WORKFLOW_COMPLETE / WORKFLOW_ERROR
 * - NODE_START / NODE_END
 */

/**
 * 处理工作流标记
 * 将工作流相关的 HTML 注释标记转换为可样式化的 div 元素
 */
export function processWorkflowMarkers(content: string): string {
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

  return content
}

/**
 * 修复代码块格式
 * 处理缩进问题和换行问题
 */
export function fixCodeBlockFormat(content: string): string {
  // 修复代码块格式
  content = content.replace(/( {4}```[\s\S]*? {4}```)/g, (match) => {
    return match.replace(/^ {4}/gm, '')
  })

  // 确保代码块有正确的换行
  content = content.replace(/```(\w+)\s*\n([\s\S]*?)```/g, '```$1\n$2```')

  return content
}
