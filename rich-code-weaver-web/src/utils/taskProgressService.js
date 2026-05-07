/**
 * 任务进度服务
 * 用于订阅WebSocket任务进度推送
 * 
 * @author DuRuiChi
 * @create 2026-05-06
 */
import SockJS from 'sockjs-client';
import Stomp from 'stompjs';

class TaskProgressService {
  constructor() {
    this.stompClient = null;
    this.subscriptions = new Map();
    this.reconnectAttempts = 0;
    this.maxReconnectAttempts = 5;
    this.reconnectDelay = 3000;
  }

  /**
   * 连接WebSocket
   */
  connect() {
    return new Promise((resolve, reject) => {
      try {
        const socket = new SockJS('/api/generator/ws/task');
        this.stompClient = Stomp.over(socket);

        // 禁用调试日志
        this.stompClient.debug = null;

        this.stompClient.connect(
          {},
          (frame) => {
            console.log('WebSocket连接成功:', frame);
            this.reconnectAttempts = 0;
            resolve(frame);
          },
          (error) => {
            console.error('WebSocket连接失败:', error);
            this.handleConnectionError();
            reject(error);
          }
        );
      } catch (error) {
        console.error('WebSocket初始化失败:', error);
        reject(error);
      }
    });
  }

  /**
   * 处理连接错误
   */
  handleConnectionError() {
    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++;
      console.log(`尝试重新连接WebSocket (${this.reconnectAttempts}/${this.maxReconnectAttempts})...`);
      
      setTimeout(() => {
        this.connect().catch(() => {
          console.warn('WebSocket重连失败');
        });
      }, this.reconnectDelay);
    } else {
      console.error('WebSocket连接失败，已达最大重试次数，降级到轮询模式');
    }
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
      await this.connect();
    }

    // 如果已订阅，先取消订阅
    if (this.subscriptions.has(appId)) {
      this.unsubscribeTaskProgress(appId);
    }

    const destination = `/topic/task/progress/${appId}`;
    
    try {
      const subscription = this.stompClient.subscribe(destination, (message) => {
        try {
          const progress = JSON.parse(message.body);
          console.log('收到任务进度:', progress);
          callback(progress);
        } catch (error) {
          console.error('解析任务进度消息失败:', error);
        }
      });

      this.subscriptions.set(appId, subscription);
      console.log(`订阅任务进度成功: appId=${appId}`);
    } catch (error) {
      console.error('订阅任务进度失败:', error);
      throw error;
    }
  }

  /**
   * 取消订阅任务进度
   * 
   * @param {Number} appId 产物ID
   */
  unsubscribeTaskProgress(appId) {
    const subscription = this.subscriptions.get(appId);
    if (subscription) {
      subscription.unsubscribe();
      this.subscriptions.delete(appId);
      console.log(`取消订阅任务进度: appId=${appId}`);
    }
  }

  /**
   * 断开连接
   */
  disconnect() {
    if (this.stompClient && this.stompClient.connected) {
      // 取消所有订阅
      this.subscriptions.forEach((subscription, appId) => {
        subscription.unsubscribe();
      });
      this.subscriptions.clear();

      // 断开连接
      this.stompClient.disconnect(() => {
        console.log('WebSocket已断开');
      });
    }
  }

  /**
   * 检查连接状态
   */
  isConnected() {
    return this.stompClient && this.stompClient.connected;
  }
}

// 导出单例
export default new TaskProgressService();
