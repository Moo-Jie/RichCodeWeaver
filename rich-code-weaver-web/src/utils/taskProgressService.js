/**
 * 任务进度服务
 * 用于订阅WebSocket任务进度推送
 *
 * @author DuRuiChi
 * @create 2026-05-06
 */
import SockJS from 'sockjs-client'
import Stomp from 'stompjs'

class TaskProgressService {
  constructor() {
    this.stompClient = null
    this.subscriptions = new Map()
    this.reconnectAttempts = 0
    this.maxReconnectAttempts = 5
    this.reconnectDelay = 3000
    this.reconnectTimer = null
    this.connectPromise = null
    this.manuallyDisconnected = false
    this.socket = null
  }

  /**
   * 连接WebSocket
   */
  connect() {
    if (this.isConnected()) {
      return Promise.resolve()
    }
    if (this.connectPromise) {
      return this.connectPromise
    }

    this.manuallyDisconnected = false
    this.clearReconnectTimer()

    const connectionPromise = new Promise((resolve, reject) => {
      try {
        const socket = new SockJS('/api/generator/ws/task')
        const client = Stomp.over(socket)
        this.socket = socket
        this.stompClient = client

        // 禁用调试日志
        client.debug = null

        client.connect(
          {},
          (frame) => {
            const isCurrentClient = this.stompClient === client
            if (this.manuallyDisconnected || !isCurrentClient) {
              try {
                client.disconnect()
              } catch {
                // 连接已被主动关闭，无需继续处理
              }
              reject(new Error('WebSocket连接已取消'))
              return
            }

            console.log('WebSocket连接成功:', frame)
            this.reconnectAttempts = 0
            this.clearReconnectTimer()
            resolve(frame)
          },
          (error) => {
            const isCurrentClient = this.stompClient === client
            if (isCurrentClient) {
              this.stompClient = null
              this.socket = null
            }
            if (isCurrentClient && !this.manuallyDisconnected) {
              console.error('WebSocket连接失败:', error)
              this.handleConnectionError()
            }
            reject(error instanceof Error ? error : new Error('WebSocket连接失败'))
          },
        )
      } catch (error) {
        this.stompClient = null
        this.socket = null
        console.error('WebSocket初始化失败:', error)
        if (!this.manuallyDisconnected) {
          this.handleConnectionError()
        }
        reject(error instanceof Error ? error : new Error('WebSocket初始化失败'))
      }
    })

    this.connectPromise = connectionPromise
    void connectionPromise.then(
      () => {
        if (this.connectPromise === connectionPromise) this.connectPromise = null
      },
      () => {
        if (this.connectPromise === connectionPromise) this.connectPromise = null
      },
    )
    return connectionPromise
  }

  /**
   * 处理连接错误
   */
  handleConnectionError() {
    if (this.manuallyDisconnected || this.reconnectTimer) return

    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      console.error('WebSocket连接失败，已达最大重试次数，降级到轮询模式')
      return
    }

    this.reconnectAttempts++
    console.log(
      '尝试重新连接WebSocket (' + this.reconnectAttempts + '/' + this.maxReconnectAttempts + ')...',
    )

    this.reconnectTimer = setTimeout(() => {
      this.reconnectTimer = null
      if (this.manuallyDisconnected) return
      this.connect().catch(() => {
        console.warn('WebSocket重连失败')
      })
    }, this.reconnectDelay)
  }

  /**
   * 订阅任务进度
   *
   * @param {Number} appId 产物ID
   * @param {Function} callback 进度回调函数
   * @returns {Promise} 订阅Promise
   */
  async subscribeTaskProgress(appId, callback) {
    // 确保已连接
    if (!this.stompClient || !this.stompClient.connected) {
      await this.connect()
    }

    if (this.manuallyDisconnected || !this.stompClient?.connected) {
      throw new Error('WebSocket未连接')
    }

    // 如果已订阅，先取消订阅
    if (this.subscriptions.has(appId)) {
      this.unsubscribeTaskProgress(appId)
    }

    const destination = '/topic/task/progress/' + appId

    try {
      const subscription = this.stompClient.subscribe(destination, (message) => {
        try {
          const progress = JSON.parse(message.body)
          console.log('收到任务进度:', progress)
          callback(progress)
        } catch (error) {
          console.error('解析任务进度消息失败:', error)
        }
      })

      this.subscriptions.set(appId, subscription)
      console.log('订阅任务进度成功: appId=' + appId)
    } catch (error) {
      console.error('订阅任务进度失败:', error)
      throw error
    }
  }

  /**
   * 取消订阅任务进度
   *
   * @param {Number} appId 产物ID
   */
  unsubscribeTaskProgress(appId) {
    const subscription = this.subscriptions.get(appId)
    if (!subscription) return

    this.subscriptions.delete(appId)
    try {
      subscription.unsubscribe()
      console.log('取消订阅任务进度: appId=' + appId)
    } catch (error) {
      console.warn('取消订阅任务进度失败: appId=' + appId, error)
    }
  }

  /**
   * 断开连接
   */
  disconnect() {
    this.manuallyDisconnected = true
    this.clearReconnectTimer()

    const client = this.stompClient
    const socket = this.socket
    this.stompClient = null
    this.socket = null
    this.connectPromise = null

    this.subscriptions.forEach((subscription, appId) => {
      try {
        subscription.unsubscribe()
      } catch (error) {
        console.warn('取消订阅任务进度失败: appId=' + appId, error)
      }
    })
    this.subscriptions.clear()

    if (client?.connected) {
      try {
        client.disconnect(() => {
          console.log('WebSocket已断开')
        })
      } catch (error) {
        console.warn('关闭WebSocket连接失败', error)
      }
    }

    if (socket) {
      try {
        socket.close()
      } catch (error) {
        console.warn('关闭WebSocket底层连接失败', error)
      }
    }

    this.reconnectAttempts = 0
  }

  /**
   * 检查连接状态
   */
  isConnected() {
    return Boolean(this.stompClient?.connected)
  }

  clearReconnectTimer() {
    if (!this.reconnectTimer) return
    clearTimeout(this.reconnectTimer)
    this.reconnectTimer = null
  }
}

// 导出单例
export default new TaskProgressService()
