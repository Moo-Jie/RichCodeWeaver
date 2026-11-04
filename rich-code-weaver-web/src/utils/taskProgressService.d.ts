export interface TaskProgressService {
  connect(): Promise<unknown>
  subscribeTaskProgress(appId: number | string, callback: (progress: any) => void): Promise<void>
  unsubscribeTaskProgress(appId: number | string): void
  disconnect(): void
  isConnected(): boolean
}

declare const taskProgressService: TaskProgressService
export default taskProgressService