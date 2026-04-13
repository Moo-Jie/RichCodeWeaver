import type { Component } from 'vue'
import {
  AlignLeftOutlined,
  ApiOutlined,
  AppstoreOutlined,
  CloudServerOutlined,
  CommentOutlined,
  CopyOutlined,
  PaperClipOutlined,
  ReadOutlined,
  UserOutlined
} from '@ant-design/icons-vue'

export const SYSTEM_MANAGE_PATH = '/admin'
export const MAIN_MENU_PATH = '/'

export type AdminMetricKey =
  | 'userTotal'
  | 'appTotal'
  | 'chatHistoryTotal'
  | 'promptTemplateTotal'
  | 'systemPromptTotal'
  | 'ragDocTotal'
  | 'materialTotal'
  | 'categoryTotal'
  | 'enabledCategoryTotal'

export interface AdminNavItem {
  key: string
  label: string
  description: string
  actionText: string
  icon: Component
  path?: string
  url?: string
  metricKey?: AdminMetricKey
  secondaryMetricKey?: AdminMetricKey
  secondaryLabel?: string
}

export const adminNavItems: AdminNavItem[] = [
  {
    key: 'userManage',
    label: '用户管理',
    description: '管理用户账户、角色信息与基础资料。',
    actionText: '进入用户管理',
    icon: UserOutlined,
    path: '/admin/userManage',
    metricKey: 'userTotal'
  },
  {
    key: 'appManage',
    label: '产物管理',
    description: '管理 Generator 生成产物、星选权重与基础信息。',
    actionText: '进入产物管理',
    icon: AppstoreOutlined,
    path: '/admin/appManage',
    metricKey: 'appTotal'
  },
  {
    key: 'chatHistory',
    label: '会话管理',
    description: '查看和维护产物对话历史，支撑生成链路排查。',
    actionText: '进入会话管理',
    icon: CommentOutlined,
    path: '/admin/chatHistory',
    metricKey: 'chatHistoryTotal'
  },
  {
    key: 'promptTemplate',
    label: '模板管理',
    description: '维护多身份、多行业的提示词模板匹配能力。',
    actionText: '进入模板管理',
    icon: CopyOutlined,
    path: '/admin/promptTemplate',
    metricKey: 'promptTemplateTotal'
  },
  {
    key: 'systemPrompt',
    label: '提示词管理',
    description: '统一管理系统级 Prompt 配置与内容编辑。',
    actionText: '进入提示词管理',
    icon: AlignLeftOutlined,
    path: '/admin/systemPrompt',
    metricKey: 'systemPromptTotal'
  },
  {
    key: 'ragManage',
    label: '知识库管理',
    description: '维护 RAG 文档、业务类型与向量化知识内容。',
    actionText: '进入知识库管理',
    icon: ReadOutlined,
    path: '/admin/ragManage',
    metricKey: 'ragDocTotal'
  },
  {
    key: 'materialManage',
    label: '素材管理',
    description: '管理素材内容、公开状态与素材分类结构。',
    actionText: '进入素材管理',
    icon: PaperClipOutlined,
    path: '/admin/materialManage',
    metricKey: 'materialTotal',
    secondaryMetricKey: 'enabledCategoryTotal',
    secondaryLabel: '已启用分类'
  },
  {
    key: 'higressManage',
    label: '网关管理',
    description: '查看 Higress 网关入口，处理网关侧接入与巡检。',
    actionText: '进入网关管理',
    icon: ApiOutlined,
    path: '/admin/higressManage'
  },
  {
    key: 'nacosManage',
    label: 'Nacos 配置',
    description: '直达配置中心，处理环境配置与服务参数维护。',
    actionText: '打开 Nacos',
    icon: CloudServerOutlined,
    url: import.meta.env.VITE_NACOS_URL || 'http://192.168.43.4:8848/nacos/index.html'
  }
]
