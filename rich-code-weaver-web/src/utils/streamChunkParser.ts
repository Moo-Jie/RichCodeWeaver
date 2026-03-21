/**
 * AI 流式响应 JSON 消息块解析器
 * 用于前端解析后端 AI 流式输出中的 JSON 格式消息（ai_response / tool_request / tool_executed）
 * 与后端 JsonStreamHandler.parseJsonChunk 保持一致的解析逻辑
 */

/** 工具名称 → 中文显示名映射 */
const TOOL_DISPLAY_NAMES: Record<string, string> = {
  creatAndWrite: '文件创建并写入工具',
  readFile: '读取文件工具',
  modifyFile: '文件修改工具',
  deleteFile: '文件删除工具',
  readDir: '文件目录读取工具',
  searchWeb: '联网搜索工具',
  scrapeWebPage: '网页抓取工具',
  aiGeneratorImage: 'AI图片生成工具',
  searchImages: '图片搜索工具',
  exit: '退出工具调用'
}

/** 工具名称 → 图标映射 */
const TOOL_ICONS: Record<string, string> = {
  creatAndWrite: '📝',
  readFile: '📖',
  modifyFile: '✏️',
  deleteFile: '🗑️',
  readDir: '📂',
  searchWeb: '🌐',
  scrapeWebPage: '🕸️',
  aiGeneratorImage: '🎨',
  searchImages: '🖼️',
  exit: '🚪'
}

/** 获取文件扩展名对应的语言标识 */
function getFileLang(filePath: string): string {
  const ext = filePath.split('.').pop()?.toLowerCase() || ''
  const langMap: Record<string, string> = {
    js: 'javascript', ts: 'typescript', vue: 'vue', jsx: 'jsx', tsx: 'tsx',
    css: 'css', scss: 'scss', less: 'less', html: 'html', json: 'json',
    md: 'markdown', py: 'python', java: 'java', xml: 'xml', yaml: 'yaml',
    yml: 'yaml', sh: 'bash', bat: 'batch', sql: 'sql', txt: 'text'
  }
  return langMap[ext] || ext
}

/**
 * 流式 JSON 消息块解析上下文
 * 用于跟踪已出现的工具调用 ID，避免重复展示
 */
export class StreamChunkParserContext {
  private seenToolIds = new Set<string>()
  /** 当前正在进行的工具调用数量统计 */
  private toolCallCount = 0

  /**
   * 解析单个 SSE 数据块内容
   * @param content 原始内容（可能是纯文本或 JSON 字符串）
   * @returns 解析后的可展示文本内容
   */
  parseChunk(content: string): string {
    if (!content || !content.trim()) return content

    // 尝试检测是否为 JSON 格式的消息块
    const trimmed = content.trim()
    if (!trimmed.startsWith('{')) return content

    try {
      const obj = JSON.parse(trimmed)
      if (!obj || !obj.type) return content

      switch (obj.type) {
        case 'ai_response':
          return obj.data ?? ''

        case 'tool_request':
          return this.handleToolRequest(obj)

        case 'tool_executed':
          return this.handleToolExecuted(obj)

        default:
          return content
      }
    } catch {
      // 不是有效 JSON，原样返回
      return content
    }
  }

  /** 处理工具请求消息 */
  private handleToolRequest(obj: { id?: string; name?: string }): string {
    const toolId = obj.id
    if (!toolId || this.seenToolIds.has(toolId)) {
      // 非首次出现的工具调用碎片，忽略
      return ''
    }

    // 首次出现：记录 ID 并输出工具开始调用的消息
    this.seenToolIds.add(toolId)
    this.toolCallCount++
    const toolName = obj.name || 'unknown'
    const displayName = TOOL_DISPLAY_NAMES[toolName] || toolName
    const icon = TOOL_ICONS[toolName] || '🔧'

    return `\n\n[开始调用系统工具] ${icon} ${displayName}\n\n`
  }

  /** 处理工具执行完成消息 */
  private handleToolExecuted(obj: { name?: string; arguments?: string }): string {
    const toolName = obj.name || 'unknown'
    const displayName = TOOL_DISPLAY_NAMES[toolName] || toolName

    // 尝试解析 arguments 获取文件路径摘要（不含完整文件内容，避免存入数据库的冗余信息）
    if (obj.arguments) {
      try {
        const args = JSON.parse(obj.arguments)
        const filePath = args.relativeFilePath || args.relativeDirPath || ''
        if (filePath) {
          return `\n\n[工具调用结束] ${displayName} → ${filePath}\n\n`
        }
      } catch {
        // arguments 解析失败，使用简单格式
      }
    }

    return `\n\n[工具调用结束] ${displayName} 执行完成\n\n`
  }

  /** 重置解析上下文 */
  reset(): void {
    this.seenToolIds.clear()
    this.toolCallCount = 0
  }
}
