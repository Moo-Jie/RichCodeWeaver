// 导入图标
import {
  BankOutlined,
  EnvironmentOutlined,
  ExperimentOutlined,
  HomeOutlined,
  MedicineBoxOutlined,
  PictureOutlined,
  ReadOutlined,
  RocketOutlined,
  SecurityScanOutlined,
  ShopOutlined
} from '@ant-design/icons-vue'
import {ref} from 'vue'

/**
 * 主页推荐提示词 - 纯前端实现方案
 */
export const promptOptions = ref([
  {
    value: '设计一个极简风格艺术日历页面，展示月份和日期网格，支持节日高亮显示，包含月份切换功能和优雅的动画过渡效果。',
    label: '艺术日历',
    desc: '节日展示页面',
    icon: PictureOutlined,
    color: '#ff9f7f'
  },
  {
    value: '创建一个现代风格计算器页面，支持基础数学运算和科学计算功能，包含清晰的按钮布局和实时结果显示区域，支持明暗主题切换。',
    label: '计算器',
    desc: '数学计算工具',
    icon: ReadOutlined,
    color: '#7eb8ff'
  },
  {
    value: '制作前端知识学习单页应用，采用卡片式布局展示HTML、CSS、JavaScript的示例和说明，包含代码高亮和实时预览功能。',
    label: '前端语言知识',
    desc: '前端学习资料',
    icon: EnvironmentOutlined,
    color: '#91d2a3'
  },
  {
    value: '设计蔬菜信息展示画廊页面，采用网格布局展示常见蔬菜的高清图片和详细介绍，包含分类筛选和搜索功能。',
    label: '蔬菜知识',
    desc: '蔬菜图鉴页面',
    icon: SecurityScanOutlined,
    color: '#a98bf9'
  },
  {
    value: '创建一个智能家居控制面板界面，采用深色科技风格，支持设备状态可视化展示和模式切换，包含平滑的开关动画和状态指示器。',
    label: '智能家居面板',
    desc: '设备控制界面',
    icon: HomeOutlined,
    color: '#ffcc5c'
  },
  {
    value: '设计商务会议预约表单页面，采用专业蓝色调设计，支持时间选择器和基本信息填写，包含表单验证和预约确认弹窗。',
    label: '商务洽谈平台',
    desc: '会议预约系统',
    icon: ShopOutlined,
    color: '#ff7c98'
  },
  {
    value: '制作健康数据看板页面，采用医疗风格设计，支持数据图表可视化展示和记录查看，包含步数、心率和睡眠等健康指标卡片。',
    label: '健康管理系统',
    desc: '健康数据看板',
    icon: MedicineBoxOutlined,
    color: '#6cd9e6'
  },
  {
    value: '创建航天知识展示单页应用，采用深空背景和星空特效，包含航天器3D卡片介绍和太空知识科普，支持滚动视差效果。',
    label: '航天科普平台',
    desc: '航天知识展示',
    icon: RocketOutlined,
    color: '#c17ce0'
  },
  {
    value: '设计学术资源库页面，采用文献馆风格设计，支持文献分类浏览和基本信息查看，包含搜索过滤和详情弹窗功能。',
    label: '科研协作系统',
    desc: '学术资源库',
    icon: ExperimentOutlined,
    color: '#8dd1b2'
  },
  {
    value: '制作旅游目的地介绍页面，采用明信片风格设计，包含景点高清图集和基本信息展示，支持轮播图和地图嵌入展示。',
    label: '文化旅游平台',
    desc: '旅游信息展示',
    icon: BankOutlined,
    color: '#f9a166'
  }
])
