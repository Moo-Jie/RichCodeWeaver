# 网页开发规范与建议-多文件版本

本文档是多文件结构模式下的网页开发知识库，供 AI 代码生成时参考。本模式生成固定的三个文件（index.html、style.css、script.js），AI 没有任何外部工具可用。

## 一、运行环境与部署限制

生成的三个文件将直接作为静态页面部署和预览，没有任何后端服务支撑。这意味着：

- 没有 Node.js 运行环境
- 没有数据库（MySQL、MongoDB、Redis 等均不可用）
- 没有服务端 API 可供调用
- 没有服务端会话管理
- 没有服务端文件读写能力
- 没有构建步骤（无 npm、无打包工具）

所有功能必须在浏览器端独立完成，仅依赖原生 HTML5、CSS3 和 JavaScript ES6+。

## 二、多文件模式的能力边界

### 本模式的特点

- 固定生成三个文件：index.html（文档结构）、style.css（全局样式）、script.js（交互逻辑）
- 文件数量不可增减，所有样式必须写入 style.css，所有脚本必须写入 script.js
- 禁止在 index.html 中使用内联 `<style>` 或 `<script>`（引用外部文件即可）
- 不可调用任何外部工具（无法搜索图片、无法联网搜索、无法生成图片、无法读写其他文件）
- 代码通过正则表达式分别从 ` ```html `、` ```css `、` ```javascript ` 三个代码块中提取
- 修改时必须输出全部三个文件的完整代码，不可省略任何一个文件

### 不可使用的技术

- 任何需要 npm install 的框架（Vue、React、Angular、Svelte）
- TypeScript、SCSS、LESS、PostCSS 嵌套等需要编译的语言和语法
- 任何需要构建步骤的工具链
- ES Module 的 import/export 语法（无模块打包器）

### 与单 HTML 模式的关键区别

- 样式和脚本分离到独立文件，代码结构更清晰
- index.html 必须通过 `<link rel="stylesheet" href="style.css">` 引用样式
- index.html 必须在 `</body>` 前通过 `<script src="script.js"></script>` 引用脚本
- 修改可能涉及多个文件的协调更新

## 三、数据存储方案

由于没有后端和数据库，所有需要持久化的数据必须使用浏览器本地存储。

### 推荐使用 localStorage

适用于用户偏好设置、表单草稿、待办事项列表、购物车数据、主题切换状态等场景。

使用要点：

- 存储前用 `JSON.stringify()` 序列化，读取后用 `JSON.parse()` 反序列化
- 页面加载时在 script.js 的 `DOMContentLoaded` 回调中从 localStorage 读取并还原界面状态
- 数据变更时立即同步写入 localStorage
- 对 `JSON.parse()` 进行 try-catch 保护，避免数据损坏导致页面白屏
- 存储键名加上项目前缀，避免与其他网站数据冲突

### sessionStorage 的适用场景

适用于仅在当前标签页生命周期内有效的临时数据。

### 不可使用的存储方式

- 禁止调用后端接口保存数据
- 禁止使用 IndexedDB（复杂度过高，三文件场景不适用）
- 禁止依赖 Cookie 做数据持久化（容量限制 4KB）

## 四、功能边界——做得到与做不到

### 可以实现的功能

- 单页面内的多区域布局（导航栏、内容区、页脚）
- 响应式适配（桌面、平板、手机三端）
- 页内锚点导航和平滑滚动
- 表单收集与本地展示（数据保存在 localStorage）
- 前端数据筛选、排序、搜索（基于 script.js 中的数据数组）
- CSS 动画、过渡效果（在 style.css 中定义）
- 倒计时、计时器、实时时钟（JavaScript 动态获取时间）
- 模拟数据的增删改查（数据存在 localStorage 中）
- 暗色模式/主题切换（CSS 变量 + localStorage 记忆）
- 选项卡、手风琴、模态框等交互组件
- 图片轮播（纯 JavaScript 实现）
- 打印功能（window.print）
- 导出为纯文本（生成文本内容并触发下载）

### 不可实现的功能（禁止尝试）

- 用户注册/登录的数据持久化到服务端
- 发送邮件或短信
- 文件上传到服务器
- 调用需要 API Key 的第三方服务（Google Maps API、OpenAI API、支付接口等），除非用户明确提供了 Key
- 多页面路由（本模式只有一个 HTML 入口文件）
- WebSocket 实时通信
- 多用户数据共享或同步
- 引入 npm 包或 CDN 以外的第三方库
- 创建第四个文件或子目录

### 处理原则

当用户需求涉及无法实现的功能时，**绝对不要留下空函数、TODO 注释或假的接口调用**。正确做法：

- 用 localStorage 模拟数据持久化，提供完整的本地体验
- 登录功能做成前端模拟版本：信息存入 localStorage，刷新后自动读取
- 表单提交改为本地保存并弹出成功提示
- 多页面需求在单页面内用选项卡或区域切换实现
- 搜索功能用前端模糊匹配过滤实现

## 五、图片与媒体资源

### 本模式无法调用工具获取图片

AI 在本模式下没有图片搜索、图片生成等工具，图片来源仅限于：

1. 系统消息中提供的素材图片 URL（必须全部使用，不可遗漏）
2. picsum.photos 在线占位图（仅在系统未提供足够图片时作为补充）
3. Font Awesome CDN 图标库

### 图片使用优先级

1. 优先使用系统提供的素材图片 URL（强制要求，禁止用占位图替代）
2. 使用 picsum.photos 在线占位图补充不足部分
3. 使用 Font Awesome 图标

### 禁止事项

- 禁止引用本地文件路径（如 `./images/logo.png`），只有三个文件，不可能存在图片文件
- 禁止调用素材搜集工具（本模式无此能力）

## 六、第三方库引入

### 可通过 CDN 引入的库

在 index.html 的 `<head>` 中通过 `<link>` 或 `<script>` 标签引入：

- Font Awesome（图标库）
- animate.css（CSS 动画库）
- marked.js（Markdown 渲染）
- Chart.js（图表，无需 API Key）
- highlight.js（代码高亮）

### 禁止引入的内容

- 任何需要 npm install 的包
- 任何 Vue、React、Angular 等框架
- 任何需要 API Key 才能使用的服务 SDK

## 七、三文件协作规范

### index.html 职责

- 定义文档结构和内容
- 引用 style.css 和 script.js
- 使用语义化 HTML5 标签：`<header>`、`<nav>`、`<main>`、`<section>`、`<article>`、`<footer>`
- 添加章节注释 `<!-- Section: 区域名称 -->` 方便定位
- 添加必要的 ARIA 可访问性属性
- 不包含任何内联样式或内联脚本

### style.css 职责

- 定义所有视觉样式
- 使用 CSS 变量定义主题色和关键尺寸
- 使用标准 CSS3 语法，不使用预处理器语法
- 优先使用 Flexbox 和 Grid 布局
- 包含响应式媒体查询断点：< 768px、768-1024px、> 1024px
- CSS 选择器采用 BEM 命名规范
- 避免使用 `!important` 声明
- 图片使用 `max-width: 100%` 确保自适应

### script.js 职责

- 实现所有交互逻辑
- 使用 `'use strict'` 严格模式
- 使用 ES6+ 语法
- 使用 `DOMContentLoaded` 确保 DOM 加载完成后再执行
- 按功能模块用注释分隔代码区域
- 表单提交使用 `preventDefault()` 阻止默认行为
- 所有 localStorage 操作集中管理

### 跨文件修改协调

修改时需注意三个文件的关联性：

- 新增 HTML 元素时，对应的样式要同步写入 style.css，交互逻辑要同步写入 script.js
- 修改 CSS 类名时，index.html 和 script.js 中的引用要同步更新
- 修改 DOM 结构时，script.js 中的选择器要同步调整

## 八、代码质量红线

### 禁止留下半成品

- 禁止出现 `// TODO` 注释
- 禁止出现空的函数体
- 禁止出现 `console.log` 调试代码
- 禁止出现注释掉的代码块
- 禁止出现无实际功能的装饰性按钮（点击无反应的按钮）

### JavaScript 运行时错误防范（多文件模式重点）

多文件模式下 script.js 与 index.html 分离，DOM 引用不一致是最常见的运行时错误：

- script.js 中 `document.getElementById()` / `document.querySelector()` 所引用的 id 或选择器**必须在 index.html 中真实存在**
- 修改 index.html 的元素 id/class 时，**必须同步修改 script.js 中的对应选择器**
- 事件监听（`addEventListener`）绑定的元素必须确保 index.html 中**DOM 存在**，否则会抛出 `Cannot read properties of null`
- 所有 DOM 操作必须在 `DOMContentLoaded` 回调内执行
- 对可能为 null 的 DOM 查询结果进行判空保护（`if (element) { ... }`）
- 函数调用前确认函数已定义，变量使用前确认已声明和赋值

### 确保可运行

- HTML 结构完整（DOCTYPE、html、head、body 标签齐全）
- index.html 正确引用了 style.css 和 script.js
- style.css 中定义了 index.html 中使用的所有 CSS 类
- script.js 中的 DOM 选择器能匹配到 index.html 中的元素（**逐一核对 id 和 class**）
- 所有事件处理有实际逻辑和用户反馈
- 三个文件放在同一目录下，浏览器打开 index.html 即可正常使用

### 内容真实性

- 所有文本使用真实、有意义的中文内容
- 使用接近真实场景的模拟数据（产品名称、价格、描述等）
- 禁止使用 "Lorem Ipsum"、"示例文字"、"这里是描述"、"功能A" 等占位内容

## 九、输出格式要求

必须按顺序输出三个代码块（html → css → javascript），系统通过正则分别提取保存为对应文件。格式错误将导致文件缺失或内容错乱。修改时必须输出全部三个文件的完整代码，即使某个文件未被修改也不可省略。
