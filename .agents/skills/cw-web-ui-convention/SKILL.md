---
name: cw-web-ui-convention
description: Apply the CW Web UI visual convention when building, restyling, or reviewing RichCodeWeaver frontend pages. Use for color, typography, spacing, borders, buttons, states, and layout consistency in rich-code-weaver-web; do not use for backend-only work.
metadata:
  short-description: CW Web UI design and color convention
---

# CW Web UI Convention

## 目标

在 `rich-code-weaver-web` 中新增、修改或评审页面前，先沿用本公约，不要重新发明一套视觉语言。目标是保持现有黑白极简主界面、克制的状态色、Ant Design Vue 组件规范和少量产品化渐变的边界。

英文 skill 名称固定为 `cw-web-ui-convention`。技能正文使用中文。

## 先判断视觉区域

不同区域的用色规则不同，不能把首页或工作区的装饰色直接搬进管理后台。

| 区域 | 代表页面 | 规则 |
| --- | --- | --- |
| 管理后台、配置、列表、表单 | `pages/admin/**`、系统管理 | 白底、浅灰面板、`#f0f0f0` 边框、`#1a1a1a` 主色；禁止新增黑紫渐变、深色 hero、装饰性光晕 |
| 创建与工作区 | `WorkspacePage.vue`、`AppChatPage.vue`、`CustomerServicePanel.vue` | 允许低饱和蓝/青、浅色渐变和深色卡片，但必须复用现有色值；不要扩散紫色 |
| 首页与产品展示 | `HomePage.vue`、`DocsPage.vue`、`GlobalFooter.vue` | 允许既有品牌渐变；新渐变必须先能对应到已有色系 |
| Markdown 与代码内容 | `MarkdownRenderer.vue` | 使用 GitHub 风格的灰阶、代码块底色和状态底色 |

## 核心色板

### 画布、表面与边框

| Token | 色值 | 用途 |
| --- | --- | --- |
| `cw-bg-canvas` | `#ffffff` / `#fff` | 全局背景、卡片、弹窗、输入区域 |
| `cw-bg-subtle` | `#fafafa` | 卡片内浅灰面板、表格工具栏、次级区域 |
| `cw-bg-muted` | `#f5f5f5` | hover、禁用区域、进度轨道、浅灰标签 |
| `cw-border` | `#f0f0f0` | 卡片、表格、分割线、弹窗分区 |
| `cw-border-strong` | `#e5e5e5` | 输入框、次级按钮、较明显边界 |
| `cw-border-disabled` | `#d9d9d9` | 禁用、弱化轮廓、进度底色 |
| `cw-border-soft` | `#e8e8e8` / `#ececec` | 页面局部细线；优先合并到 `#f0f0f0` 或 `#e5e5e5` |

### 文字

| Token | 色值 | 用途 |
| --- | --- | --- |
| `cw-text-primary` | `#1a1a1a` | 标题、主按钮文字、正文主色 |
| `cw-text-strong` | `#333` | 次级标题、主按钮 hover、强调正文 |
| `cw-text-secondary` | `#666` | 常规辅助文字、说明、标签文字 |
| `cw-text-tertiary` | `#888` | 分组说明、弱提示 |
| `cw-text-muted` | `#999` | 页面副标题、时间、占位说明 |
| `cw-text-disabled` | `#bbb` | 禁用文字、极弱提示 |
| `cw-text-inverse` | `#fff` | 深色区域文字 |

### 交互与状态

| Token | 色值 | 用途 |
| --- | --- | --- |
| `cw-primary` | `#1a1a1a` | 主按钮、主操作、当前选中态、进度条填充 |
| `cw-primary-hover` | `#333` | 主按钮 hover |
| `cw-info` | `#1890ff` | 链接、信息提示、任务进行中 |
| `cw-focus` | `#4096ff` | 键盘焦点、输入框 focus outline |
| `cw-info-bg` | `#e6f7ff` | 信息提示、进行中背景 |
| `cw-success` | `#52c41a` | 成功、启用、已完成 |
| `cw-success-bg` | `#f6ffed` | 成功提示、完成步骤背景 |
| `cw-warning` | `#faad14` | 警告、待确认、注意事项图标 |
| `cw-warning-bg` | `#fffbe6` / `#fffbeb` | 警告提示背景 |
| `cw-danger` | `#ff4d4f` | 删除、失败、危险操作 |
| `cw-danger-bg` | `#fff2f0` | 错误提示、失败步骤背景 |

状态色只表达状态，不要拿来装饰普通卡片。普通业务界面不要使用紫色或大面积蓝色作为主操作色。

## 产品化与内容色

### 产品品牌与工作区

| 色组 | 色值 | 使用边界 |
| --- | --- | --- |
| 品牌青蓝 | `#00c4ff`、`#9face6` | 首页、聊天气泡、头像或已有品牌渐变 |
| 内容主题蓝 | `#0288d1`、`#e1f5fe` | 内容方案、模板配色、可配置主题 |
| 自然绿主题 | `#388e3c`、`#e8f5e9` | 内容方案中的自然绿可配置主题 |
| 温暖橙主题 | `#f57c00`、`#fff3e0` | 内容方案中的温暖橙可配置主题 |
| 首页暖冷渐变 | `rgb(255, 248, 206)`、`rgb(147, 203, 255)` | 首页/产品展示的既有装饰渐变 |
| 品牌页脚蓝 | `#59a7ff`、`#c8e5ff`、`#2998ff`、`#bddeff` | 全局页脚的品牌装饰，不进入业务表单 |
| 工作区深色 | `#0f172a`、`#111827`、`#1f2937`、`#334155`、`#374151` | 客服、工作区深色卡片；不要用于管理后台普通面板 |
| 图表蓝 | `#5b8ff9`、`#84aefc`、`#6f8cff`、`#9fb7f6` | 仪表盘、统计条、图表 |

### 内容类型与标签

| 语义 | 色值 |
| --- | --- |
| 图片/主媒体 | `#1677ff` |
| 文本 | `#fa8c16` |
| 视频/成功 | `#52c41a` |
| 音频 | `#eb2f96` |
| 链接/扩展 | `#722ed1` |

这些颜色用于媒体类型、标签或少量图标。不要把 `#722ed1` 扩展为后台主色或黑色渐变。

### Markdown 与代码

| Token | 色值 | 用途 |
| --- | --- | --- |
| `cw-code-text` | `#24292f` | 代码正文、GitHub 风格正文 |
| `cw-code-muted` | `#57606a` | 代码注释、弱说明 |
| `cw-code-faint` | `#8c959f` | 行号、占位文字 |
| `cw-code-border` | `#d0d7de` | 代码块、分割线 |
| `cw-code-bg` | `#f6f8fa` | 代码块、引用块 |
| `cw-code-strong` | `#1f2328` | 深色代码文本 |

Markdown 的提示、成功、警告、错误块继续复用 `cw-info-bg`、`cw-success-bg`、`cw-warning-bg`、`cw-danger-bg`。

### 低频页面例外

- `MaterialCategoryManagePage.vue` 使用 `#667eea` 到 `#764ba2` 的紫色渐变，属于历史页面局部例外，不作为后台通用规范。
- 全局页脚使用蓝青渐变，属于品牌展示区域，不复制到管理页。
- `#000`、`rgba(0, 0, 0, 0.04-0.15)` 主要用于遮罩、阴影和极少数高对比文本，不作为常规背景。

### 灰阶漂移归并

系统里存在少量历史灰阶变体。新代码不要把变体继续扩散，按以下方向归并：

| 历史变体 | 优先归并到 |
| --- | --- |
| `#d0d0d0`、`#ddd`、`#e0e0e0` | `#d9d9d9` |
| `#e8e8e8`、`#ececec`、`#eee` | `#f0f0f0` 或 `#e5e5e5` |
| `#f8f9fa`、`#f9f9f9`、`#fcfcfc`、`#f7f8fa` | `#fafafa` |
| `#999999`、`#888888` | `#999` / `#888` |
| `#ffffff` | `#fff` |

聊天、Markdown 提示卡和历史品牌页渐变可以保留现有强调色，但不要再新增同类色号。

## 形状、阴影与排版

- 页面主标题：`22px`、`700`、`#1a1a1a`；副标题：`14px`、`#999`。
- 卡片标题：`16px`、`600`、`#1a1a1a`；正文：`13-14px`。
- 辅助说明：`12px`、`#999`；禁用：`#bbb`。
- 卡片和表格外层圆角：`12px`。
- 输入框、普通按钮、步骤卡：`8px`。
- 小标签：`4-6px`；胶囊标签才使用 `999px`。
- 卡片优先用 `1px solid #f0f0f0` 建立边界；阴影只作辅助，常用 `0 2px 6px rgba(0, 0, 0, 0.04)` 或 `0 4px 16px rgba(0, 0, 0, 0.04)`。
- 页面主体优先留白与边框，不使用大阴影、玻璃拟态或高饱和背景。
- 普通管理页面禁止新增黑紫渐变；进度条使用 `#1a1a1a` 填充、`#f5f5f5` 轨道。

## 组件规则

1. 优先使用 Ant Design Vue 的 `a-card`、`a-button`、`a-table`、`a-modal`、`a-tag`、`a-progress`，只覆盖必要的品牌色和圆角。
2. 主按钮使用 `type="primary"` 并保持 `#1a1a1a`；同一操作区只保留一个主按钮。
3. 次级按钮使用白底、`#d9d9d9` 边框；hover/focus 才使用 `#4096ff`。
4. 删除和失败才使用 `#ff4d4f`，不要把红色用于普通强调。
5. 链接和信息态使用 `#1890ff`；焦点态使用 `#4096ff`。
6. 表面层级按“白底 -> 白色卡片 -> `#fafafa` 子面板 -> `#f5f5f5` 弱化区”组织。
7. 标签优先使用蓝、geekblue、青、绿、橙；新增颜色前先检查本公约是否已有语义色。
8. 进度、加载、模拟状态必须明确区分真实结果与模拟结果，不得把未执行能力显示为真实成功。
9. 管理页不要使用大面积深色 hero、紫色渐变、彩色光斑或与现有色系无关的品牌色。
10. 响应式调整优先保持现有断点、圆角和间距，不另起一套视觉系统。

## 推荐 Token 映射

如果后续需要抽取全局 CSS 变量，可按下表渐进迁移；当前实现允许保留硬编码色，但新代码应优先复用这些值。

```css
:root {
  --cw-bg-canvas: #ffffff;
  --cw-bg-subtle: #fafafa;
  --cw-bg-muted: #f5f5f5;
  --cw-border: #f0f0f0;
  --cw-border-strong: #e5e5e5;
  --cw-text-primary: #1a1a1a;
  --cw-text-strong: #333333;
  --cw-text-secondary: #666666;
  --cw-text-muted: #999999;
  --cw-text-disabled: #bbbbbb;
  --cw-primary: #1a1a1a;
  --cw-primary-hover: #333333;
  --cw-info: #1890ff;
  --cw-focus: #4096ff;
  --cw-info-bg: #e6f7ff;
  --cw-success: #52c41a;
  --cw-success-bg: #f6ffed;
  --cw-warning: #faad14;
  --cw-warning-bg: #fffbe6;
  --cw-danger: #ff4d4f;
  --cw-danger-bg: #fff2f0;
}
```

## 交付前检查

- 新增颜色能在本公约中找到语义，或明确标注为页面局部例外。
- 管理后台没有新增黑紫渐变、深色 hero 或彩色光斑。
- 主按钮、边框、圆角和卡片层级与相邻页面一致。
- 成功、警告、错误只用于对应状态。
- 表格、表单、弹窗在窄屏下不溢出。
- 通过 `npm run type-check` 和 `npm run build-only`。
- 用浏览器复核正常态、hover/focus、加载态、完成态和错误态。

## 证据来源

- 全局基线：`rich-code-weaver-web/src/App.vue:21-22`、`rich-code-weaver-web/src/App.vue:36`
- 管理页卡片与主按钮：`rich-code-weaver-web/src/pages/admin/RagManagePage.vue:1115`、`rich-code-weaver-web/src/pages/admin/RagManagePage.vue:1153`
- 管理页边框与浅灰面板：`rich-code-weaver-web/src/pages/admin/UserManagePage.vue:541`、`rich-code-weaver-web/src/pages/admin/SystemManageDashboard.vue:857`
- 首页蓝色交互与品牌渐变：`rich-code-weaver-web/src/pages/HomePage.vue:620`、`rich-code-weaver-web/src/pages/HomePage.vue:839`
- 可配置主题色：`rich-code-weaver-web/src/components/admin/FieldDefinitionEditor.vue:367`
- 品牌页脚渐变：`rich-code-weaver-web/src/components/GlobalFooter.vue:56`
- 工作区深色卡片：`rich-code-weaver-web/src/components/workspace/CustomerServicePanel.vue:836`
- Markdown 代码底色：`rich-code-weaver-web/src/components/MarkdownRenderer.vue:407`
- 仪表盘蓝色：`rich-code-weaver-web/src/pages/admin/SystemManageDashboard.vue:1158`
- 低频紫色例外：`rich-code-weaver-web/src/pages/admin/MaterialCategoryManagePage.vue:522`
