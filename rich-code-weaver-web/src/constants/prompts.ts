// 导入图标
import {
  PictureOutlined,
  ReadOutlined,
  EnvironmentOutlined,
  HomeOutlined,
  SecurityScanOutlined,
  ShopOutlined,
  MedicineBoxOutlined,
  RocketOutlined,
  ExperimentOutlined,
  BankOutlined,
} from '@ant-design/icons-vue';
import { ref } from 'vue'

/**
 * 主页推荐提示词
 * TODO 测试数据待优化
 */
export  const promptOptions = ref([
  {
    value: '快速生成一个艺术日历网站，点击可以查看对应日期的主要节日。',
    label: '艺术日历',
    desc: '沉浸式艺术展示平台',
    icon: PictureOutlined,
    color: '#ff9f7f'
  },
  {
    value: '快速生成一个计算器网站，可以执行快速计算。',
    label: '计算器',
    desc: '快速计算工具',
    icon: ReadOutlined,
    color: '#7eb8ff'
  },
  {
    value: '快速生成一个前端语言知识网站，包含HTML、CSS、JavaScript 等知识。',
    label: '前端语言知识',
    desc: '前端开发知识平台',
    icon: EnvironmentOutlined,
    color: '#91d2a3'
  },
  {
    value: '快速生成一个蔬菜知识网站。',
    label: '蔬菜知识',
    desc: '蔬菜知识平台',
    icon: SecurityScanOutlined,
    color: '#a98bf9'
  },
  {
    value: '开发一个智能家居控制面板，包含房间3D可视化、设备状态监控、能耗统计、情景模式设置、语音控制集成、安全警报系统。采用现代极简风格，支持暗黑模式',
    label: '智能家居面板',
    desc: '全屋智能控制系统',
    icon: HomeOutlined,
    color: '#ffcc5c'
  },
  {
    value: '创建一个虚拟商务洽谈平台，支持3D会议室、数字名片交换、实时文档协作、多语言翻译、会议记录AI助手、合同模板库、交易安全防护。界面需专业商务风格',
    label: '商务洽谈平台',
    desc: '企业级远程协作工具',
    icon: ShopOutlined,
    color: '#ff7c98'
  },
  {
    value: '开发一个健康管理系统，包含健康数据仪表盘、用药提醒、预约管理、AI症状分析、电子病历管理、家庭健康共享功能。符合医疗行业UI规范',
    label: '健康管理系统',
    desc: '个人健康数据追踪器',
    icon: MedicineBoxOutlined,
    color: '#6cd9e6'
  },
  {
    value: '设计航天科普教育平台，含火箭模拟器、星座导航系统、太空任务体验、航天新闻聚合、专家讲座预约。采用深空主题设计',
    label: '航天科普平台',
    desc: '太空探索教育系统',
    icon: RocketOutlined,
    color: '#c17ce0'
  },
  {
    value: '创建一个科研协作系统，支持实验数据可视化、论文协作编辑、学术资源索引、引用管理、会议投稿追踪。符合学术出版规范',
    label: '科研协作系统',
    desc: '学术研究管理工具',
    icon: ExperimentOutlined,
    color: '#8dd1b2'
  },
  {
    value: '设计一个文化旅游预约平台，整合景点门票、特色住宿、当地导游、文化体验工作坊预约。支持多支付方式和多语言界面',
    label: '文化旅游平台',
    desc: '深度旅游体验服务',
    icon: BankOutlined,
    color: '#f9a166'
  }
])
