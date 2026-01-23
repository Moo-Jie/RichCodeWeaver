import { API_BASE_URL } from '@/config/env'

export type CodeGenWsServerMessage =
  | { type: 'started'; taskId: string; appId?: string }
  | { type: 'chunk'; taskId: string; appId?: string; seq?: number; data?: string }
  | { type: 'end'; taskId: string; appId?: string; seq?: number; message?: string }
  | { type: 'error'; taskId?: string; appId?: string; message?: string }
  | { type: 'pong' }

export type CodeGenWsClientMessage =
  | { type: 'start'; appId: string; message: string; isAgent: boolean }
  | { type: 'resume'; appId: string; taskId: string; fromSeq: number }
  | { type: 'cancel'; taskId: string }
  | { type: 'ping' }

function apiBaseToWsUrl(apiBaseUrl: string): string {
  // 允许 apiBaseUrl 是绝对（http(s)://.../api）或相对（/api）
  const u = apiBaseUrl.startsWith('http://') || apiBaseUrl.startsWith('https://')
    ? new URL(apiBaseUrl)
    : new URL(apiBaseUrl, window.location.origin)
  u.protocol = u.protocol === 'https:' ? 'wss:' : 'ws:'
  // API_BASE_URL 默认是 .../api
  u.pathname = u.pathname.replace(/\/$/, '') + '/ws/codegen'
  u.search = ''
  u.hash = ''
  return u.toString()
}

export class CodeGenWsClient {
  private ws: WebSocket | null = null
  private manuallyClosed = false
  private reconnectDelayMs = 500
  private readonly maxReconnectDelayMs = 8000
  private readonly url: string

  onMessage?: (msg: CodeGenWsServerMessage) => void
  onOpen?: () => void
  onClose?: () => void

  constructor(apiBaseUrl: string = API_BASE_URL) {
    this.url = apiBaseToWsUrl(apiBaseUrl)
  }

  isConnected() {
    return this.ws?.readyState === WebSocket.OPEN
  }

  async ensureConnected(): Promise<void> {
    if (this.isConnected()) return
    await this.connect()
  }

  connect(): Promise<void> {
    this.manuallyClosed = false
    return new Promise((resolve, reject) => {
      try {
        this.ws = new WebSocket(this.url)
        const ws = this.ws
        let settled = false
        const timeout = window.setTimeout(() => {
          if (settled) return
          settled = true
          try {
            ws.close()
          } catch {}
          reject(new Error('WebSocket 连接超时'))
        }, 5000)

        ws.onopen = () => {
          if (settled) return
          settled = true
          window.clearTimeout(timeout)
          this.reconnectDelayMs = 500
          this.onOpen?.()
          resolve()
        }

        ws.onmessage = (event) => {
          try {
            const msg = JSON.parse(event.data) as CodeGenWsServerMessage
            this.onMessage?.(msg)
          } catch (e) {
            // ignore
          }
        }

        ws.onclose = () => {
          window.clearTimeout(timeout)
          this.onClose?.()
          if (!settled) {
            settled = true
            reject(new Error('WebSocket 连接关闭'))
            return
          }
          if (!this.manuallyClosed) {
            this.scheduleReconnect()
          }
        }

        ws.onerror = () => {
          // 触发 close 后会走重连；这里确保首连失败能抛出错误
          if (!settled) {
            window.clearTimeout(timeout)
            settled = true
            reject(new Error('WebSocket 连接错误'))
          }
        }
      } catch (e) {
        reject(e)
      }
    })
  }

  close() {
    this.manuallyClosed = true
    this.ws?.close()
    this.ws = null
  }

  send(msg: CodeGenWsClientMessage) {
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN) return
    this.ws.send(JSON.stringify(msg))
  }

  private scheduleReconnect() {
    const delay = this.reconnectDelayMs
    this.reconnectDelayMs = Math.min(this.reconnectDelayMs * 2, this.maxReconnectDelayMs)
    setTimeout(() => {
      if (this.manuallyClosed) return
      this.connect().catch(() => {
        this.scheduleReconnect()
      })
    }, delay)
  }
}


