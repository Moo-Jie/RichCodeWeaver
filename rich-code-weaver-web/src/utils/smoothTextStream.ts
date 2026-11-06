interface SmoothTextStreamOptions {
  initialText?: string
  intervalMs?: number
  drainTicks?: number
  maxTailLength?: number
  maxChunkLength?: number
  onUpdate: (content: string, done: boolean) => void
}

export interface SmoothTextStream {
  push: (delta: string) => void
  finish: () => void
  stop: () => void
  getSource: () => string
  getDisplayedText: () => string
}

/**
 * 将服务端大块文本切成稳定行，并按时序逐行释放给 Markdown 渲染器。
 * 参考 Codex：真实 delta 进入缓冲区，已完成行排队，未完成行只保留有限尾预览。
 */
export function createSmoothTextStream(options: SmoothTextStreamOptions): SmoothTextStream {
  const {
    initialText = '',
    intervalMs = 20,
    drainTicks = 120,
    maxTailLength = 8192,
    maxChunkLength = 512,
    onUpdate
  } = options

  let source = initialText
  let stableText = initialText
  let pendingText = ''
  let upstreamDone = false
  let stopped = false
  let timer: ReturnType<typeof setTimeout> | null = null
  let lastSnapshot = ''
  let lastDone = false
  const queue: string[] = []

  const enqueueChunk = (text: string) => {
    if (!text) return
    for (let offset = 0; offset < text.length; offset += maxChunkLength) {
      queue.push(text.slice(offset, offset + maxChunkLength))
    }
  }

  const consumeCompleteLines = () => {
    let lineEnd = pendingText.indexOf('\n')
    while (lineEnd >= 0) {
      enqueueChunk(pendingText.slice(0, lineEnd + 1))
      pendingText = pendingText.slice(lineEnd + 1)
      lineEnd = pendingText.indexOf('\n')
    }

    if (pendingText.length > maxTailLength) {
      const overflowLength = pendingText.length - maxTailLength
      enqueueChunk(pendingText.slice(0, overflowLength))
      pendingText = pendingText.slice(overflowLength)
    }
  }

  const emit = (done: boolean) => {
    const content = stableText + pendingText
    if (content !== lastSnapshot || done !== lastDone) {
      lastSnapshot = content
      lastDone = done
      onUpdate(content, done)
    }
  }

  const schedule = () => {
    if (stopped || timer !== null || (upstreamDone && queue.length === 0 && pendingText.length === 0)) return
    timer = setTimeout(tick, intervalMs)
  }

  const tick = () => {
    timer = null
    if (stopped) return

    if (queue.length > 0) {
      const batchSize = Math.max(1, Math.ceil(queue.length / drainTicks))
      stableText += queue.splice(0, batchSize).join('')
    }

    const done = upstreamDone && queue.length === 0 && pendingText.length === 0
    emit(done)
    if (!done && queue.length > 0) schedule()
  }

  return {
    push(delta: string) {
      if (!delta || stopped || upstreamDone) return
      source += delta
      pendingText += delta
      consumeCompleteLines()
      schedule()
    },
    finish() {
      if (stopped || upstreamDone) return
      upstreamDone = true
      if (pendingText) {
        enqueueChunk(pendingText)
        pendingText = ''
      }
      if (queue.length === 0) {
        emit(true)
      } else {
        schedule()
      }
    },
    stop() {
      stopped = true
      if (timer !== null) {
        clearTimeout(timer)
        timer = null
      }
      queue.length = 0
    },
    getSource() {
      return source
    },
    getDisplayedText() {
      return stableText + pendingText
    }
  }
}