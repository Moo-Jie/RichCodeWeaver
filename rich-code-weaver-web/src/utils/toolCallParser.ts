/**
 * 工具调用解析器
 * 负责解析 Agent 模式下的工具调用标记，生成格式化的 HTML
 *
 * 支持的工具类型：
 * - 通用工具调用：[开始调用系统工具] / [工具调用结束]
 * - 任务计划：[任务计划] {...}
 * - 思考工具：[思考完成] 类型: xxx | 结论: yyy
 * - 消息工具：[消息已发送] 类型: xxx | 标题
 * - 退出工具：[任务执行结束，已无需继续调用工具]
 */

import {
  TOOL_STATUS_ICONS,
  TASK_STATUS_ICONS,
  TOOL_TYPE_ICONS,
  THINK_TYPE_ICONS,
  MESSAGE_TYPE_ICONS,
  generateIconHtml
} from './iconMapper'

// ==================== 类型定义 ====================

export interface TaskItem {
  step: string
  status: 'completed' | 'in_progress' | 'pending'
  id?: number
  notes?: string
}

export interface TaskPlanData {
  type: 'taskPlan'
  action: 'create' | 'update' | 'list'
  tasks: TaskItem[]
  summary: {
    total: number
    completed: number
    inProgress: number
    pending: number
  }
}

interface Marker {
  type: 'start' | 'end'
  text: string
  detail?: string
  isTaskPlan?: boolean
  taskData?: TaskPlanData
  index: number
  endIndex: number
}

interface Replacement {
  index: number
  endIndex: number
  html: string
}

// ==================== 常量定义 ====================

const THINK_TYPE_NAMES: Record<string, string> = {
  'analyze': '需求分析',
  'plan': '方案规划',
  'reflect': '反思检查',
  'decide': '决策判断',
  'summarize': '总结归纳',
  'general': '通用思考'
}

const MSG_TYPE_NAMES: Record<string, string> = {
  'info': '信息通知',
  'progress': '进度更新',
  'warning': '警告提示',
  'error': '错误提示',
  'success': '成功提示'
}

const STATUS_LABELS: Record<string, string> = {
  'completed': '已完成',
  'in_progress': '进行中',
  'pending': '待处理'
}

// ==================== 工具函数 ====================

/**
 * 查找嵌套 JSON 对象的结束位置
 */
function findJsonObjectEndIndex(content: string, start: number): number {
  let depth = 0
  let inString = false
  let escapeNext = false

  for (let i = start; i < content.length; i++) {
    const char = content[i]

    if (escapeNext) {
      escapeNext = false
      continue
    }

    if (char === '\\' && inString) {
      escapeNext = true
      continue
    }

    if (char === '"') {
      inString = !inString
      continue
    }

    if (inString) continue

    if (char === '{') {
      depth++
    } else if (char === '}') {
      depth--
      if (depth === 0) {
        return i
      }
    }
  }

  return -1
}

/**
 * 生成任务列表 HTML（可折叠，优先显示待处理任务）
 */
function generateTaskListHtml(tasks: TaskItem[], isLatest: boolean): string {
  if (!tasks || tasks.length === 0) return ''

  // 按状态排序：completed > in_progress > pending（已完成在顶部，待处理在底部）
  const sortedTasks = [...tasks].sort((a, b) => {
    const order: Record<string, number> = { 'completed': 0, 'in_progress': 1, 'pending': 2 }
    return (order[a.status] ?? 2) - (order[b.status] ?? 2)
  })

  // 找到第一个进行中的任务索引（用于自动滚动）
  const firstInProgressIndex = sortedTasks.findIndex(t => t.status === 'in_progress')

  // 生成唯一ID用于折叠控制
  const listId = `task-list-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`
  const collapsedClass = isLatest ? '' : 'collapsed'
  const toggleIcon = isLatest ? '▼' : '▶'

  let html = `<div class="task-list-wrapper ${collapsedClass}" data-list-id="${listId}">`
  html += `<div class="task-list-toggle" onclick="this.parentElement.classList.toggle('collapsed');this.querySelector('.toggle-icon').textContent=this.parentElement.classList.contains('collapsed')?'▶':'▼'">`
  html += `<span class="toggle-icon">${toggleIcon}</span>`
  html += `<span class="toggle-text">任务详情 (${tasks.length}项)</span>`
  html += `</div>`
  html += `<div class="task-list-content" data-scroll-to-index="${firstInProgressIndex}">`

  sortedTasks.forEach((task, index) => {
    const originalIndex = tasks.findIndex(t => t === task)
    const iconSvg = TASK_STATUS_ICONS[task.status] || TASK_STATUS_ICONS.pending
    const label = STATUS_LABELS[task.status] || '待处理'
    const statusClass = `task-${task.status.replace('_', '-')}`
    const notes = task.notes ? `<span class="task-notes">${task.notes}</span>` : ''
    const scrollTargetClass = index === firstInProgressIndex ? ' task-scroll-target' : ''

    html += `<div class="task-item ${statusClass}${scrollTargetClass}" data-task-index="${index}">`
    html += `<span class="task-icon">${iconSvg}</span>`
    html += `<span class="task-index">#${originalIndex + 1}</span>`
    html += `<span class="task-step">${task.step}</span>`
    html += `<span class="task-label">${label}</span>`
    html += notes
    html += '</div>'
  })

  html += '</div></div>'
  return html
}

/**
 * 生成思考内容展示 HTML（可折叠，默认展开）
 */
function generateThinkContentHtml(typeName: string, thinkContent: string): string {
  if (!thinkContent) return ''

  const listId = `think-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`

  // 将思考内容中的换行转换为 HTML 换行，并转义 HTML 特殊字符
  const thinkIcon = generateIconHtml(THINK_TYPE_ICONS.general, 'think-icon')
  const conclusionIcon = generateIconHtml(TOOL_STATUS_ICONS.completed, 'conclusion-icon')

  const escapedContent = thinkContent
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/【思考过程】/g, `<strong class="think-section-title">${thinkIcon} 思考过程</strong>`)
    .replace(/【结论】/g, `<strong class="think-section-title">${conclusionIcon} 结论</strong>`)

  let html = `<div class="think-content-wrapper" data-list-id="${listId}">`
  html += `<div class="think-content-toggle" onclick="this.parentElement.classList.toggle('collapsed');this.querySelector('.toggle-icon').textContent=this.parentElement.classList.contains('collapsed')?'▶':'▼'">`
  html += `<span class="toggle-icon">▼</span>`
  html += `<span class="toggle-text">查看完整思考过程</span>`
  html += `</div>`
  html += `<div class="think-content-body">`
  html += `<div class="think-conclusion">${escapedContent}</div>`
  html += `</div></div>`

  return html
}

// ==================== 标记收集函数 ====================

/**
 * 收集所有工具调用开始标记
 */
function collectStartMarkers(content: string, markers: Marker[]): void {
  const startRegex = /\[开始调用系统工具\]\s*(.+)/g
  let m
  while ((m = startRegex.exec(content)) !== null) {
    markers.push({
      type: 'start',
      text: m[1].trim(),
      index: m.index,
      endIndex: m.index + m[0].length
    })
  }
}

/**
 * 收集所有工具调用结束标记
 */
function collectEndMarkers(content: string, markers: Marker[]): void {
  const endRegex = /\[工具调用结束\]\s*([^\n]+?)(?:\s*\[\s*\n([\s\S]*?)\n\s*\])?(?=\s*(?:\[|$|\n))/g
  let m
  while ((m = endRegex.exec(content)) !== null) {
    markers.push({
      type: 'end',
      text: m[1].trim(),
      detail: m[2] ? m[2].trim() : undefined,
      index: m.index,
      endIndex: m.index + m[0].length
    })
  }
}

/**
 * 收集任务计划标记
 */
function collectTaskPlanMarkers(content: string, markers: Marker[], taskPlanCount: { value: number }): void {
  const taskPlanMarkerRegex = /\[任务计划\]\s*/g
  let m
  while ((m = taskPlanMarkerRegex.exec(content)) !== null) {
    const jsonStart = m.index + m[0].length
    if (content[jsonStart] === '{') {
      const jsonEnd = findJsonObjectEndIndex(content, jsonStart)
      if (jsonEnd !== -1) {
        let taskInfo = '任务计划管理 执行完成'
        let taskData: TaskPlanData | undefined

        try {
          const jsonStr = content.substring(jsonStart, jsonEnd + 1)
          const data = JSON.parse(jsonStr) as TaskPlanData
          if (data.summary) {
            const planIcon = generateIconHtml(TOOL_TYPE_ICONS.plan, 'plan-icon')
            taskInfo = `${planIcon} 任务计划 (${data.summary.completed}/${data.summary.total} 完成)`
          }
          taskData = data
        } catch {
          // 解析失败，使用默认文本
        }

        taskPlanCount.value++

        markers.push({
          type: 'end',
          text: taskInfo,
          isTaskPlan: true,
          taskData,
          index: m.index,
          endIndex: jsonEnd + 1
        })
      }
    }
  }
}

/**
 * 收集思考完成标记（支持完整思考内容展示）
 */
function collectThinkMarkers(content: string, markers: Marker[]): void {
  // 匹配完整格式：[思考完成] 类型: xxx | 结论: yyy\n[思考内容]\n...\n[/思考内容]
  const thinkFullRegex = /\[思考完成\]\s*类型:\s*(\w+)(?:\s*\|\s*结论:\s*(.+?))?\n\[思考内容\]\n([\s\S]*?)\n\[\/思考内容\]/g
  let m

  while ((m = thinkFullRegex.exec(content)) !== null) {
    const thinkType = m[1] || 'general'
    const conclusion = m[2] ? m[2].trim() : ''
    const thinkContent = m[3] ? m[3].trim() : ''
    const typeName = THINK_TYPE_NAMES[thinkType] || thinkType

    const thinkIcon = generateIconHtml(TOOL_TYPE_ICONS.think, 'tool-type-icon')
    let displayText = `${thinkIcon} 思考与推理 → ${typeName}`
    if (conclusion) {
      displayText += `: ${conclusion}`
    }

    // 生成可折叠的思考内容HTML
    let detailHtml = ''
    if (thinkContent) {
      detailHtml = generateThinkContentHtml(typeName, thinkContent)
    }

    markers.push({
      type: 'end',
      text: displayText,
      detail: detailHtml,
      index: m.index,
      endIndex: m.index + m[0].length
    })
  }

  // 兼容旧格式：[思考完成] 类型: xxx | 结论: yyy（无完整内容块）
  const thinkSimpleRegex = /\[思考完成\]\s*类型:\s*(\w+)(?:\s*\|\s*结论:\s*(.+?))?(?=\n(?!\[思考内容\])|$)/g
  while ((m = thinkSimpleRegex.exec(content)) !== null) {
    // 检查是否已被完整格式匹配
    const alreadyMatched = markers.some(mk => mk.index === m!.index)
    if (alreadyMatched) continue

    const thinkType = m[1] || 'general'
    const conclusion = m[2] ? m[2].trim() : ''
    const typeName = THINK_TYPE_NAMES[thinkType] || thinkType

    const thinkIcon = generateIconHtml(TOOL_TYPE_ICONS.think, 'tool-type-icon')
    let displayText = `${thinkIcon} 思考与推理 → ${typeName}`
    if (conclusion) {
      displayText += `: ${conclusion}`
    }

    markers.push({
      type: 'end',
      text: displayText,
      index: m.index,
      endIndex: m.index + m[0].length
    })
  }
}

/**
 * 收集消息发送标记
 */
function collectMessageMarkers(content: string, markers: Marker[]): void {
  const messageEndRegex = /\[消息已发送\]\s*类型:\s*(\w+)(?:\s*\|\s*(.+?))?(?=\n|$)/g
  let m
  while ((m = messageEndRegex.exec(content)) !== null) {
    const msgType = m[1] || 'info'
    const title = m[2] ? m[2].trim() : ''
    const typeName = MSG_TYPE_NAMES[msgType] || msgType

    const messageIcon = generateIconHtml(TOOL_TYPE_ICONS.message, 'tool-type-icon')
    let displayText = `${messageIcon} 消息通知 → ${typeName}`
    if (title) {
      displayText += `: ${title}`
    }

    markers.push({
      type: 'end',
      text: displayText,
      index: m.index,
      endIndex: m.index + m[0].length
    })
  }
}

/**
 * 收集退出工具标记
 */
function collectExitMarkers(content: string, markers: Marker[]): void {
  const exitEndRegex = /\[任务执行结束，已无需继续调用工具\]/g
  let m
  while ((m = exitEndRegex.exec(content)) !== null) {
    const exitIcon = generateIconHtml(TOOL_TYPE_ICONS.exit, 'tool-type-icon')
    markers.push({
      type: 'end',
      text: `${exitIcon} 任务执行完成`,
      index: m.index,
      endIndex: m.index + m[0].length
    })
  }
}

// ==================== HTML 生成函数 ====================

/**
 * 生成完成状态的工具调用 HTML
 */
function generateEndHtml(end: Marker, isLatestTaskPlan: boolean): string {
  const completedIcon = TOOL_STATUS_ICONS.completed
  let html = `<div class="tool-call-item tool-call-done">`
  html += `<span class="tci-icon">${completedIcon}</span>`
  html += `<span class="tci-label">完成</span>`
  html += `<span class="tci-text">${end.text}</span>`
  html += `</div>`

  // 任务计划：生成可折叠的任务列表
  if (end.isTaskPlan && end.taskData?.tasks) {
    html += generateTaskListHtml(end.taskData.tasks, isLatestTaskPlan)
  } else if (end.detail) {
    // 其他工具的详情
    html += `<div class="tool-call-item tool-call-detail">`
    html += `<span class="tci-text">${end.detail}</span>`
    html += `</div>`
  }

  return html
}

/**
 * 生成已配对的开始标记 HTML（灰色调）
 */
function generatePairedStartHtml(start: Marker): string {
  const settingsIcon = TOOL_STATUS_ICONS.settings
  return `<div class="tool-call-item tool-call-paired"><span class="tci-icon">${settingsIcon}</span><span class="tci-label">调用</span><span class="tci-text">${start.text}</span></div>`
}

/**
 * 生成加载中的开始标记 HTML
 */
function generateLoadingStartHtml(start: Marker): string {
  const loadingIcon = TOOL_STATUS_ICONS.loading
  return `<div class="tool-call-item tool-call-loading"><span class="tci-icon">${loadingIcon}</span><span class="tci-label">调用中</span><span class="tci-text">${start.text}</span></div>`
}

// ==================== 主处理函数 ====================

/**
 * 处理工具调用标记
 * 将原始内容中的工具调用标记转换为格式化的 HTML
 */
export function processToolCallMarkers(content: string): string {
  const markers: Marker[] = []
  const taskPlanCount = { value: 0 }

  // 收集所有标记
  collectStartMarkers(content, markers)
  collectEndMarkers(content, markers)
  collectTaskPlanMarkers(content, markers, taskPlanCount)
  collectThinkMarkers(content, markers)
  collectMessageMarkers(content, markers)
  collectExitMarkers(content, markers)

  if (markers.length === 0) return content

  // 按位置排序
  markers.sort((a, b) => a.index - b.index)

  // 分离 starts 和 ends
  const starts: Marker[] = []
  const ends: Marker[] = []
  markers.forEach(mk => {
    if (mk.type === 'start') starts.push(mk)
    else ends.push(mk)
  })

  // 1:1 配对
  const pairedStartCount = Math.min(starts.length, ends.length)

  // 找出最后一个任务计划的索引（用于控制展开/折叠）
  let lastTaskPlanIndex = -1
  ends.forEach((end, index) => {
    if (end.isTaskPlan) {
      lastTaskPlanIndex = index
    }
  })

  // 构建替换列表
  const replacements: Replacement[] = []

  // 处理已完成的 ends
  ends.forEach((end, index) => {
    const isLatestTaskPlan = end.isTaskPlan === true && index === lastTaskPlanIndex
    const html = generateEndHtml(end, isLatestTaskPlan)
    replacements.push({ index: end.index, endIndex: end.endIndex, html })
  })

  // 处理已配对的 starts
  for (let i = 0; i < pairedStartCount; i++) {
    const start = starts[i]
    replacements.push({
      index: start.index,
      endIndex: start.endIndex,
      html: generatePairedStartHtml(start)
    })
  }

  // 处理未配对的 starts（加载中）
  for (let i = pairedStartCount; i < starts.length; i++) {
    const start = starts[i]
    replacements.push({
      index: start.index,
      endIndex: start.endIndex,
      html: generateLoadingStartHtml(start)
    })
  }

  // 按位置从后往前替换
  replacements.sort((a, b) => b.index - a.index)
  replacements.forEach(r => {
    content = content.substring(0, r.index) + r.html + content.substring(r.endIndex)
  })

  return content
}
