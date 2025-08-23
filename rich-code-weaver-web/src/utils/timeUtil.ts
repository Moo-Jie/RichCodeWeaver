import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import 'dayjs/locale/zh-cn'

dayjs.extend(relativeTime)
dayjs.locale('zh-cn')

/**
 * 格式化时间
 */
export const formatTime = (time: string | undefined, format = 'YYYY-MM-DD HH:mm:ss'): string => {
  if (!time) return ''
  return dayjs(time).format(format)
}

/**
 * 格式化时间为相对时间
 */
export const formatRelativeTime = (time: string | undefined): string => {
  if (!time) return ''
  return dayjs(time).fromNow()
}

/**
 * 格式化时间为日期
 */
export const formatDate = (time: string | undefined): string => {
  if (!time) return ''
  return dayjs(time).format('YYYY-MM-DD')
}
