-- =====================================================
-- V4: RAG 知识库文档表
-- 替代原来从文件系统 docs/ragDocs/ 读取 .md 文件的方式
-- 将知识库文档内容存入数据库，通过管理界面维护
-- docContent 使用 TEXT 类型（MySQL 上限 ~64KB，PostgreSQL TEXT 无限制）
-- 当前三份文档约 8-10KB，TEXT 类型完全足够
-- =====================================================

-- ===== MySQL 版本 =====
CREATE TABLE IF NOT EXISTS `rag_document` (
    `id`          BIGINT       NOT NULL COMMENT 'id（雪花）',
    `docTitle`    VARCHAR(255) NOT NULL COMMENT '文档标题',
    `docContent`  MEDIUMTEXT   NOT NULL COMMENT '文档正文内容（Markdown 格式），MEDIUMTEXT 上限 ~16MB',
    `codeGenType` VARCHAR(50)  NOT NULL DEFAULT 'GENERAL' COMMENT '适用代码生成类型：HTML/MULTI_FILE/VUE_PROJECT/GENERAL',
    `description` VARCHAR(500)          DEFAULT NULL COMMENT '文档简短描述',
    `isEnabled`   TINYINT      NOT NULL DEFAULT 1 COMMENT '是否启用（1-启用，0-禁用）',
    `sortOrder`   INT          NOT NULL DEFAULT 0 COMMENT '排序权重（越小越靠前）',
    `userId`      BIGINT                DEFAULT NULL COMMENT '创建人 id',
    `createTime`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updateTime`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `isDelete`    TINYINT      NOT NULL DEFAULT 0 COMMENT '是否删除（逻辑删除）',
    PRIMARY KEY (`id`),
    INDEX `idx_codeGenType` (`codeGenType`),
    INDEX `idx_isEnabled` (`isEnabled`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT = 'RAG 知识库文档表';

-- =====================================================
-- ===== PostgreSQL 版本 =====
-- CREATE TABLE IF NOT EXISTS rag_document (
--     id          BIGINT       NOT NULL,
--     "docTitle"  VARCHAR(255) NOT NULL,
--     "docContent" TEXT        NOT NULL,
--     "codeGenType" VARCHAR(50) NOT NULL DEFAULT 'GENERAL',
--     description VARCHAR(500),
--     "isEnabled" SMALLINT     NOT NULL DEFAULT 1,
--     "sortOrder" INT          NOT NULL DEFAULT 0,
--     "userId"    BIGINT,
--     "createTime" TIMESTAMP   NOT NULL DEFAULT NOW(),
--     "updateTime" TIMESTAMP   NOT NULL DEFAULT NOW(),
--     "isDelete"  SMALLINT     NOT NULL DEFAULT 0,
--     PRIMARY KEY (id)
-- );
-- =====================================================

-- =====================================================
-- 初始数据：将原 docs/ragDocs/ 下三份 .md 文件内容导入数据库
-- 注意：单引号已转义为 ''（标准 SQL 转义）
-- =====================================================

INSERT INTO `rag_document` (`id`, `docTitle`, `docContent`, `codeGenType`, `description`, `isEnabled`, `sortOrder`, `userId`, `createTime`, `updateTime`, `isDelete`)
VALUES (
    1,
    '网页开发规范与建议-单HTML版本',
    '# 网页开发规范与建议-单HTML版本\n\n本文档是单 HTML 页面模式下的网页开发知识库，供 AI 代码生成时参考。本模式将所有 HTML、CSS、JavaScript 写在一个 HTML 文件中，AI\n没有任何外部工具可用。\n\n## 一、运行环境与部署限制\n\n生成的 HTML 文件将直接作为静态页面部署和预览，没有任何后端服务支撑。这意味着：\n\n- 没有 Node.js 运行环境\n- 没有数据库（MySQL、MongoDB、Redis 等均不可用）\n- 没有服务端 API 可供调用\n- 没有服务端会话管理\n- 没有服务端文件读写能力\n- 没有构建步骤（无 npm、无 Webpack、无 Vite）\n\n所有功能必须在浏览器端独立完成，仅依赖原生 HTML5、CSS3 和 JavaScript ES6+。\n\n## 二、单文件模式的能力边界\n\n### 本模式的特点\n\n- 生成单个完整的 HTML 文件，CSS 内联在 `<style>` 标签中，JavaScript 内联在 `<script>` 标签中\n- 不可调用任何外部工具（无法搜索图片、无法联网搜索、无法生成图片、无法读写文件）\n- 代码通过正则表达式从 ` ```html ` 代码块中提取，输出格式必须严格合规\n- 修改时必须输出完整的 HTML 文件代码，不可省略任何部分\n\n### 不可使用的技术\n\n- 任何需要 npm install 的框架（Vue、React、Angular、Svelte）\n- TypeScript、SCSS、LESS 等需要编译的语言\n- 任何需要构建步骤的工具链\n- ES Module 的 import/export 语法（无模块打包器）\n\n## 三、数据存储方案\n\n由于没有后端和数据库，所有需要持久化的数据必须使用浏览器本地存储。\n\n### 推荐使用 localStorage\n\n适用于用户偏好设置、表单草稿、待办事项列表、购物车数据、主题切换状态等场景。\n\n使用要点：\n\n- 存储前用 `JSON.stringify()` 序列化，读取后用 `JSON.parse()` 反序列化\n- 页面加载时主动从 localStorage 读取并还原界面状态\n- 数据变更时立即同步写入 localStorage\n- 对 `JSON.parse()` 进行 try-catch 保护，避免数据损坏导致页面白屏\n- 存储键名加上项目前缀，避免与其他网站数据冲突\n\n### sessionStorage 的适用场景\n\n适用于仅在当前标签页生命周期内有效的临时数据。\n\n### 不可使用的存储方式\n\n- 禁止调用后端接口保存数据\n- 禁止使用 IndexedDB（复杂度过高，单文件场景不适用）\n- 禁止依赖 Cookie 做数据持久化（容量限制 4KB）\n\n## 四、功能边界——做得到与做不到\n\n### 可以实现的功能\n\n- 单页面内的多区域布局（导航栏、内容区、页脚）\n- 响应式适配（桌面、平板、手机三端）\n- 页内锚点导航和平滑滚动\n- 表单收集与本地展示（数据保存在 localStorage）\n- 前端数据筛选、排序、搜索（基于页面内的 JavaScript 数组）\n- CSS 动画、过渡效果\n- 倒计时、计时器、实时时钟（JavaScript 动态获取时间）\n- 模拟数据的增删改查（数据存在 localStorage 中）\n- 暗色模式/主题切换（CSS 变量 + localStorage 记忆）\n- 选项卡、手风琴、模态框等交互组件\n- 图片轮播（纯 JavaScript 实现）\n- 打印功能（window.print）\n- 导出为纯文本（生成文本内容并触发下载）\n\n### 不可实现的功能（禁止尝试）\n\n- 用户注册/登录的数据持久化到服务端\n- 发送邮件或短信\n- 文件上传到服务器\n- 调用需要 API Key 的第三方服务（Google Maps API、OpenAI API、支付接口等），除非用户明确提供了 Key\n- 多页面路由（本模式只有一个 HTML 文件）\n- WebSocket 实时通信\n- 多用户数据共享或同步\n- 引入 npm 包或 CDN 以外的第三方库\n\n### 处理原则\n\n当用户需求涉及无法实现的功能时，**绝对不要留下空函数、TODO 注释或假的接口调用**。正确做法：\n\n- 用 localStorage 模拟数据持久化，提供完整的本地体验\n- 登录功能做成前端模拟版本：信息存入 localStorage，刷新后自动读取\n- 表单提交改为本地保存并弹出成功提示\n- 多页面需求在单页面内用选项卡或区域切换实现\n- 搜索功能用前端模糊匹配过滤实现\n\n## 五、图片与媒体资源\n\n### 本模式无法调用工具获取图片\n\nAI 在本模式下没有图片搜索、图片生成等工具，图片来源仅限于：\n\n1. 系统消息中提供的素材图片 URL（必须全部使用，不可遗漏）\n2. picsum.photos 在线占位图（仅在系统未提供足够图片时作为补充）\n3. 内联 SVG 代码（适合图标和简单图形）\n4. Font Awesome CDN 图标库\n\n### 图片使用优先级\n\n1. 优先使用系统提供的素材图片 URL（强制要求，禁止用占位图替代）\n2. 使用 picsum.photos 在线占位图补充不足部分\n3. 使用内联 SVG 或 Font Awesome 图标\n\n### 禁止事项\n\n- 禁止引用本地文件路径（如 `./images/logo.png`），文件不存在会显示裂图\n- 禁止调用素材搜集工具（本模式无此能力）\n\n## 六、第三方库引入\n\n### 可通过 CDN 引入的库\n\n在 `<head>` 中通过 `<link>` 或 `<script>` 标签引入：\n\n- Font Awesome（图标库）\n- animate.css（CSS 动画库）\n- marked.js（Markdown 渲染）\n- Chart.js（图表，无需 API Key）\n- highlight.js（代码高亮）\n\n### 禁止引入的内容\n\n- 任何需要 npm install 的包\n- 任何 Vue、React、Angular 等框架\n- 任何需要 API Key 才能使用的服务 SDK\n\n## 七、样式与布局\n\n### 响应式设计\n\n所有页面必须适配三种屏幕尺寸：\n\n- 手机：宽度 < 768px\n- 平板：768px - 1024px\n- 桌面：> 1024px\n\n采用移动优先设计原则，基础样式面向手机，通过媒体查询向上适配更大屏幕。\n\n### 样式编写原则\n\n- 使用 CSS 变量定义主题色和关键尺寸，便于全局调整和主题切换\n- 优先使用 Flexbox 和 Grid 布局\n- CSS 选择器采用 BEM 命名规范，层次清晰\n- 避免使用 `!important` 声明\n- 图片使用 `max-width: 100%` 确保自适应\n\n### 页面结构规范\n\n- 使用语义化 HTML5 标签：`<header>`、`<nav>`、`<main>`、`<section>`、`<article>`、`<footer>`\n- 添加章节注释 `<!-- Section: 区域名称 -->` 方便定位\n- 添加必要的 ARIA 可访问性属性\n\n## 八、JavaScript 编写规范\n\n### 基本要求\n\n- 使用 ES6+ 语法\n- 所有 JavaScript 写在 `<script>` 标签内，放在 `</body>` 前\n- 使用 `DOMContentLoaded` 事件确保 DOM 加载完成后再执行\n- 表单提交使用 `preventDefault()` 阻止默认行为\n\n### 代码组织\n\n- 按功能模块用注释分隔代码区域\n- 关键函数添加简明注释\n- 避免全局变量污染，使用 IIFE 或模块化组织\n- 事件监听统一在 `DOMContentLoaded` 回调中绑定\n\n## 九、代码质量红线\n\n### 禁止留下半成品\n\n- 禁止出现 `// TODO` 注释\n- 禁止出现空的函数体\n- 禁止出现 `console.log` 调试代码\n- 禁止出现注释掉的代码块\n- 禁止出现无实际功能的装饰性按钮（点击无反应的按钮）\n\n### JavaScript 运行时错误防范\n\n这是代码审查中最常见的失败原因，必须严格遵守：\n\n- `document.getElementById()` / `document.querySelector()` 所引用的 id 或选择器**必须在 HTML 中真实存在**\n- 事件监听（`addEventListener`）绑定的元素必须确保**DOM 中存在**，否则会抛出 `Cannot read properties of null`\n- 所有 DOM 操作必须在 `DOMContentLoaded` 回调内执行，确保元素已加载\n- 函数调用前确认函数已定义，变量使用前确认已声明和赋值\n- 对可能为 null 的 DOM 查询结果进行判空保护（`if (element) { ... }`）\n\n### 确保可运行\n\n- HTML 结构完整（DOCTYPE、html、head、body 标签齐全）\n- 所有引用的 CSS 类名在 `<style>` 中有定义\n- 所有引用的 JavaScript 函数有实现\n- 所有 JS 中引用的 DOM 元素 id/class 在 HTML 中必须存在且一致\n- 所有事件处理有实际逻辑和用户反馈\n- 页面在浏览器中打开即可正常使用\n\n### 内容真实性\n\n- 所有文本使用真实、有意义的中文内容\n- 使用接近真实场景的模拟数据（产品名称、价格、描述等）\n- 禁止使用 "Lorem Ipsum"、"示例文字"、"这里是描述"、"功能A" 等占位内容\n\n## 十、输出格式要求\n\n代码必须包裹在 ` ```html ` 代码块中，系统通过正则提取。格式错误将导致代码无法保存。修改时必须输出完整文件，不可用省略号或注释代替未修改的代码段。\n',
    'HTML',
    '单 HTML 文件模式知识库：运行环境限制、能力边界、localStorage 数据存储、图片处理、CDN 库引入、响应式样式、JS 规范、代码质量红线',
    1,
    10,
    NULL,
    NOW(),
    NOW(),
    0
);

INSERT INTO `rag_document` (`id`, `docTitle`, `docContent`, `codeGenType`, `description`, `isEnabled`, `sortOrder`, `userId`, `createTime`, `updateTime`, `isDelete`)
VALUES (
    2,
    '网页开发规范与建议-多文件版本',
    '# 网页开发规范与建议-多文件版本\n\n本文档是多文件结构模式下的网页开发知识库，供 AI 代码生成时参考。本模式生成固定的三个文件（index.html、style.css、script.js），AI\n没有任何外部工具可用。\n\n## 一、运行环境与部署限制\n\n生成的三个文件将直接作为静态页面部署和预览，没有任何后端服务支撑。这意味着：\n\n- 没有 Node.js 运行环境\n- 没有数据库（MySQL、MongoDB、Redis 等均不可用）\n- 没有服务端 API 可供调用\n- 没有服务端会话管理\n- 没有服务端文件读写能力\n- 没有构建步骤（无 npm、无打包工具）\n\n所有功能必须在浏览器端独立完成，仅依赖原生 HTML5、CSS3 和 JavaScript ES6+。\n\n## 二、多文件模式的能力边界\n\n### 本模式的特点\n\n- 固定生成三个文件：index.html（文档结构）、style.css（全局样式）、script.js（交互逻辑）\n- 文件数量不可增减，所有样式必须写入 style.css，所有脚本必须写入 script.js\n- 禁止在 index.html 中使用内联 `<style>` 或 `<script>`（引用外部文件即可）\n- 不可调用任何外部工具（无法搜索图片、无法联网搜索、无法生成图片、无法读写其他文件）\n- 代码通过正则表达式分别从 ` ```html `、` ```css `、` ```javascript ` 三个代码块中提取\n- 修改时必须输出全部三个文件的完整代码，不可省略任何一个文件\n\n### 不可使用的技术\n\n- 任何需要 npm install 的框架（Vue、React、Angular、Svelte）\n- TypeScript、SCSS、LESS、PostCSS 嵌套等需要编译的语言和语法\n- 任何需要构建步骤的工具链\n- ES Module 的 import/export 语法（无模块打包器）\n\n### 与单 HTML 模式的关键区别\n\n- 样式和脚本分离到独立文件，代码结构更清晰\n- index.html 必须通过 `<link rel="stylesheet" href="style.css">` 引用样式\n- index.html 必须在 `</body>` 前通过 `<script src="script.js"></script>` 引用脚本\n- 修改可能涉及多个文件的协调更新\n\n## 三、数据存储方案\n\n由于没有后端和数据库，所有需要持久化的数据必须使用浏览器本地存储。\n\n### 推荐使用 localStorage\n\n适用于用户偏好设置、表单草稿、待办事项列表、购物车数据、主题切换状态等场景。\n\n使用要点：\n\n- 存储前用 `JSON.stringify()` 序列化，读取后用 `JSON.parse()` 反序列化\n- 页面加载时在 script.js 的 `DOMContentLoaded` 回调中从 localStorage 读取并还原界面状态\n- 数据变更时立即同步写入 localStorage\n- 对 `JSON.parse()` 进行 try-catch 保护，避免数据损坏导致页面白屏\n- 存储键名加上项目前缀，避免与其他网站数据冲突\n\n### sessionStorage 的适用场景\n\n适用于仅在当前标签页生命周期内有效的临时数据。\n\n### 不可使用的存储方式\n\n- 禁止调用后端接口保存数据\n- 禁止使用 IndexedDB（复杂度过高，三文件场景不适用）\n- 禁止依赖 Cookie 做数据持久化（容量限制 4KB）\n\n## 四、功能边界——做得到与做不到\n\n### 可以实现的功能\n\n- 单页面内的多区域布局（导航栏、内容区、页脚）\n- 响应式适配（桌面、平板、手机三端）\n- 页内锚点导航和平滑滚动\n- 表单收集与本地展示（数据保存在 localStorage）\n- 前端数据筛选、排序、搜索（基于 script.js 中的数据数组）\n- CSS 动画、过渡效果（在 style.css 中定义）\n- 倒计时、计时器、实时时钟（JavaScript 动态获取时间）\n- 模拟数据的增删改查（数据存在 localStorage 中）\n- 暗色模式/主题切换（CSS 变量 + localStorage 记忆）\n- 选项卡、手风琴、模态框等交互组件\n- 图片轮播（纯 JavaScript 实现）\n- 打印功能（window.print）\n- 导出为纯文本（生成文本内容并触发下载）\n\n### 不可实现的功能（禁止尝试）\n\n- 用户注册/登录的数据持久化到服务端\n- 发送邮件或短信\n- 文件上传到服务器\n- 调用需要 API Key 的第三方服务（Google Maps API、OpenAI API、支付接口等），除非用户明确提供了 Key\n- 多页面路由（本模式只有一个 HTML 入口文件）\n- WebSocket 实时通信\n- 多用户数据共享或同步\n- 引入 npm 包或 CDN 以外的第三方库\n- 创建第四个文件或子目录\n\n### 处理原则\n\n当用户需求涉及无法实现的功能时，**绝对不要留下空函数、TODO 注释或假的接口调用**。正确做法：\n\n- 用 localStorage 模拟数据持久化，提供完整的本地体验\n- 登录功能做成前端模拟版本：信息存入 localStorage，刷新后自动读取\n- 表单提交改为本地保存并弹出成功提示\n- 多页面需求在单页面内用选项卡或区域切换实现\n- 搜索功能用前端模糊匹配过滤实现\n\n## 五、图片与媒体资源\n\n### 本模式无法调用工具获取图片\n\nAI 在本模式下没有图片搜索、图片生成等工具，图片来源仅限于：\n\n1. 系统消息中提供的素材图片 URL（必须全部使用，不可遗漏）\n2. picsum.photos 在线占位图（仅在系统未提供足够图片时作为补充）\n3. Font Awesome CDN 图标库\n\n### 图片使用优先级\n\n1. 优先使用系统提供的素材图片 URL（强制要求，禁止用占位图替代）\n2. 使用 picsum.photos 在线占位图补充不足部分\n3. 使用 Font Awesome 图标\n\n### 禁止事项\n\n- 禁止引用本地文件路径（如 `./images/logo.png`），只有三个文件，不可能存在图片文件\n- 禁止调用素材搜集工具（本模式无此能力）\n\n## 六、第三方库引入\n\n### 可通过 CDN 引入的库\n\n在 index.html 的 `<head>` 中通过 `<link>` 或 `<script>` 标签引入：\n\n- Font Awesome（图标库）\n- animate.css（CSS 动画库）\n- marked.js（Markdown 渲染）\n- Chart.js（图表，无需 API Key）\n- highlight.js（代码高亮）\n\n### 禁止引入的内容\n\n- 任何需要 npm install 的包\n- 任何 Vue、React、Angular 等框架\n- 任何需要 API Key 才能使用的服务 SDK\n\n## 七、三文件协作规范\n\n### index.html 职责\n\n- 定义文档结构和内容\n- 引用 style.css 和 script.js\n- 使用语义化 HTML5 标签：`<header>`、`<nav>`、`<main>`、`<section>`、`<article>`、`<footer>`\n- 添加章节注释 `<!-- Section: 区域名称 -->` 方便定位\n- 添加必要的 ARIA 可访问性属性\n- 不包含任何内联样式或内联脚本\n\n### style.css 职责\n\n- 定义所有视觉样式\n- 使用 CSS 变量定义主题色和关键尺寸\n- 使用标准 CSS3 语法，不使用预处理器语法\n- 优先使用 Flexbox 和 Grid 布局\n- 包含响应式媒体查询断点：< 768px、768-1024px、> 1024px\n- CSS 选择器采用 BEM 命名规范\n- 避免使用 `!important` 声明\n- 图片使用 `max-width: 100%` 确保自适应\n\n### script.js 职责\n\n- 实现所有交互逻辑\n- 使用 `''use strict''` 严格模式\n- 使用 ES6+ 语法\n- 使用 `DOMContentLoaded` 确保 DOM 加载完成后再执行\n- 按功能模块用注释分隔代码区域\n- 表单提交使用 `preventDefault()` 阻止默认行为\n- 所有 localStorage 操作集中管理\n\n### 跨文件修改协调\n\n修改时需注意三个文件的关联性：\n\n- 新增 HTML 元素时，对应的样式要同步写入 style.css，交互逻辑要同步写入 script.js\n- 修改 CSS 类名时，index.html 和 script.js 中的引用要同步更新\n- 修改 DOM 结构时，script.js 中的选择器要同步调整\n\n## 八、代码质量红线\n\n### 禁止留下半成品\n\n- 禁止出现 `// TODO` 注释\n- 禁止出现空的函数体\n- 禁止出现 `console.log` 调试代码\n- 禁止出现注释掉的代码块\n- 禁止出现无实际功能的装饰性按钮（点击无反应的按钮）\n\n### JavaScript 运行时错误防范（多文件模式重点）\n\n多文件模式下 script.js 与 index.html 分离，DOM 引用不一致是最常见的运行时错误：\n\n- script.js 中 `document.getElementById()` / `document.querySelector()` 所引用的 id 或选择器**必须在 index.html 中真实存在**\n- 修改 index.html 的元素 id/class 时，**必须同步修改 script.js 中的对应选择器**\n- 事件监听（`addEventListener`）绑定的元素必须确保 index.html 中**DOM 存在**，否则会抛出 `Cannot read properties of null`\n- 所有 DOM 操作必须在 `DOMContentLoaded` 回调内执行\n- 对可能为 null 的 DOM 查询结果进行判空保护（`if (element) { ... }`）\n- 函数调用前确认函数已定义，变量使用前确认已声明和赋值\n\n### 确保可运行\n\n- HTML 结构完整（DOCTYPE、html、head、body 标签齐全）\n- index.html 正确引用了 style.css 和 script.js\n- style.css 中定义了 index.html 中使用的所有 CSS 类\n- script.js 中的 DOM 选择器能匹配到 index.html 中的元素（**逐一核对 id 和 class**）\n- 所有事件处理有实际逻辑和用户反馈\n- 三个文件放在同一目录下，浏览器打开 index.html 即可正常使用\n\n### 内容真实性\n\n- 所有文本使用真实、有意义的中文内容\n- 使用接近真实场景的模拟数据（产品名称、价格、描述等）\n- 禁止使用 "Lorem Ipsum"、"示例文字"、"这里是描述"、"功能A" 等占位内容\n\n## 九、输出格式要求\n\n必须按顺序输出三个代码块（html → css → javascript），系统通过正则分别提取保存为对应文件。格式错误将导致文件缺失或内容错乱。修改时必须输出全部三个文件的完整代码，即使某个文件未被修改也不可省略。\n',
    'MULTI_FILE',
    '多文件结构模式知识库（index.html+style.css+script.js）：三文件职责分工、跨文件DOM一致性、''use strict''规范、JS运行时错误防范',
    1,
    20,
    NULL,
    NOW(),
    NOW(),
    0
);

INSERT INTO `rag_document` (`id`, `docTitle`, `docContent`, `codeGenType`, `description`, `isEnabled`, `sortOrder`, `userId`, `createTime`, `updateTime`, `isDelete`)
VALUES (
    3,
    '网页开发规范与建议-Vue项目版本',
    '# 网页开发规范与建议-Vue项目版本\n\n本文档是 Vue 项目工程模式下的网页开发知识库，供 AI 代码生成时参考。本模式使用 Vue 3 + Vite 构建完整的前端工程项目，AI\n具备文件操作工具和素材搜集工具。\n\n## 一、运行环境与部署限制\n\n本系统的最终产物是纯静态文件（构建后的 dist 目录），部署在静态文件服务器上，没有任何后端服务支撑。这意味着：\n\n- 没有 Node.js 服务端运行环境（构建阶段有，运行阶段没有）\n- 没有数据库（MySQL、MongoDB、Redis 等均不可用）\n- 没有服务端 API 可供调用\n- 没有服务端会话管理（Session）\n- 没有服务端文件读写能力\n\n所有功能必须在浏览器端独立完成。生成的代码必须通过 `npm run build` 成功构建，构建产物必须能在任意子路径下正常部署。\n\n## 二、数据存储方案\n\n由于没有后端和数据库，所有需要持久化的数据必须使用浏览器本地存储。\n\n### 推荐使用 localStorage\n\n适用于用户偏好设置、表单草稿、购物车数据、待办事项列表、主题切换状态、阅读进度等场景。\n\n使用要点：\n\n- 存储前用 `JSON.stringify()` 序列化，读取后用 `JSON.parse()` 反序列化\n- 页面加载时主动从 localStorage 读取数据并还原状态\n- 数据变更时及时同步写入 localStorage\n- 对 `JSON.parse()` 进行 try-catch 保护，防止数据损坏导致页面崩溃\n- 存储键名加上项目前缀，避免与其他网站冲突\n\n### sessionStorage 的适用场景\n\n适用于仅在当前浏览器标签页生命周期内有效的临时数据，如多步骤表单的中间状态、页面间临时传参。\n\n### 不可使用的存储方式\n\n- 禁止调用后端接口保存数据\n- 禁止使用 IndexedDB 存储大量结构化数据（复杂度过高，容易出错）\n- 禁止使用 Cookie 做数据持久化（容量限制 4KB，且用途不符）\n\n## 三、功能边界——做得到与做不到\n\n### 可以实现的功能\n\n- 页面路由与导航（Vue Router，hash 模式）\n- 响应式页面布局（桌面、平板、手机自适应）\n- 表单收集与本地展示（不含提交到服务器）\n- 前端数据筛选、排序、分页（基于本地数据）\n- 动画和过渡效果（CSS 动画、Vue Transition）\n- 图表展示（如使用 CDN 引入 Chart.js 等无需 npm 的库）\n- 倒计时、计时器、时钟等时间类功能\n- 模拟数据的增删改查（数据存在 localStorage 中）\n- 暗色模式/主题切换\n- 多语言切换（前端硬编码翻译文本）\n- 图片轮播、画廊、灯箱效果\n- Markdown 渲染（可用 CDN 引入 marked.js）\n- 拖拽排序（原生 Drag API 或轻量库）\n- 导出功能（导出为 CSV、TXT 等纯文本格式）\n\n### 不可实现的功能（禁止尝试）\n\n- 用户注册/登录的数据持久化（无法将账号密码存在服务端）\n- 发送邮件或短信通知\n- 文件上传到服务器\n- 调用需要 API Key 的第三方服务（如 Google Maps API、OpenAI API、支付接口），除非用户明确提供了 Key\n- 服务端渲染（SSR）\n- WebSocket 实时通信（无服务端）\n- 多用户数据共享或同步\n- 后端鉴权与权限管理\n- 数据库查询\n\n### 处理原则\n\n当用户需求中包含无法实现的功能时，**不要留下空的函数体、TODO 注释或假接口调用**。正确做法：\n\n- 用 localStorage 模拟数据持久化，提供完整的本地增删改查体验\n- 登录功能可以做成前端模拟版本：将用户信息存入 localStorage，页面刷新后自动读取登录状态\n- 表单提交改为本地保存并弹出成功提示，而非发送 HTTP 请求\n- 需要后端的搜索功能改为前端模糊匹配过滤\n\n## 四、Vue 项目模式的工具能力\n\n本模式下 AI 具备以下工具，可以创建完整的多文件项目结构：\n\n### 文件操作工具\n\n- creatAndWrite：创建新文件并写入内容\n- modifyFile：局部修改现有文件\n- deleteFile：删除文件\n- readDir：读取目录结构\n- readFile：读取文件内容\n\n### 素材搜集工具\n\n- searchImages：搜索网络图片\n- aiGeneratorImage：AI 生成图片\n- searchWeb：联网搜索信息\n- scrapeWebPage：抓取网页内容\n\n当系统已提供素材资源时，禁止再调用素材搜集工具。仅在系统未提供任何素材时使用这些工具获取素材。\n\n## 五、第三方依赖管理\n\n### 推荐方式\n\nVue 项目通过 package.json 管理依赖。可以使用的 npm 包需满足：\n\n- 是纯前端库，不依赖 Node.js 运行时\n- 不需要 API Key 或后端服务配合\n- 包体积合理，不会导致构建产物过大\n\n### 可安全使用的依赖示例\n\n- vue（核心框架，必装）\n- vue-router（路由，必装）\n- dayjs（日期处理）\n- animate.css（动画库）\n- swiper（轮播组件）\n\n### 禁止使用的依赖\n\n- 状态管理库（Pinia、Vuex）——增加复杂度，本项目规模不需要\n- TypeScript——增加配置复杂度，容易引发类型错误导致构建失败\n- 代码格式化工具（ESLint、Prettier）——与代码生成无关\n- 需要后端配合的库（axios 用于调用不存在的后端、socket.io 等）\n\n## 六、图片与静态资源\n\n### 禁止引用未创建的本地资源\n\n这是 Vue 项目构建失败的最常见原因。禁止在代码中 import 或引用 `src/assets/` 下未实际创建的文件。\n\n错误写法：\n\n- `import logo from ''@/assets/logo.png''`（文件不存在，构建报错）\n- `<img src="/src/assets/banner.jpg">`（文件不存在，构建报错）\n\n正确做法：\n\n- 使用系统提供的素材图片 URL\n- 使用在线占位图服务 picsum.photos\n- 使用工具搜索或生成图片后获取 URL\n- 图标使用内联 SVG 或 Font Awesome CDN\n\n### 图片使用优先级\n\n1. 优先使用系统提供的素材图片 URL（必须全部使用，不可遗漏）\n2. 使用工具搜索或生成的图片 URL\n3. 使用 picsum.photos 在线占位图补充\n\n## 七、路由配置\n\n必须使用 hash 模式路由，确保构建后的项目在任意子路径下正常工作。\n\n原因：静态文件服务器无法处理 HTML5 History 模式的路由回退。使用 hash 模式（URL 中带 `#`）可以避免页面刷新 404 问题。\n\n## 八、样式与布局\n\n### 响应式设计\n\n所有页面必须适配三种屏幕尺寸：\n\n- 手机：宽度 < 768px\n- 平板：768px - 1024px\n- 桌面：> 1024px\n\n使用原生 CSS 实现响应式，不引入 UI 框架（减少构建体积和依赖冲突风险）。\n\n### 样式编写原则\n\n- 使用 CSS 变量定义主题色和关键尺寸，便于全局调整\n- 优先使用 Flexbox 和 Grid 布局\n- 避免使用 `!important` 声明\n- `<style scoped>` 确保组件样式隔离\n\n## 九、代码质量红线\n\n### 禁止留下半成品\n\n- 禁止出现 `// TODO` 注释\n- 禁止出现空的函数体或方法\n- 禁止出现 `console.log` 调试代码\n- 禁止出现注释掉的代码块\n- 禁止出现 `placeholder` 样式的临时内容\n\n### 运行时错误防范（Vue 项目重点）\n\nVue 项目中以下问题会导致构建失败或运行时白屏：\n\n- 组件内 `ref()` / `reactive()` 声明的响应式变量必须在模板中正确引用，名称必须一致\n- `v-for` 指令必须配合 `:key` 使用，且 key 值唯一\n- 组件 `import` 路径必须与实际创建的文件路径完全匹配（大小写敏感）\n- 路由配置中的 `component` 引用必须指向已创建的 `.vue` 文件\n- 父组件的 `components` 选项中必须注册所有使用的子组件（Options API 模式）\n- 禁止在模板中引用未在 `<script setup>` 或 `setup()` 中声明的变量/函数\n- 事件处理方法 `@click="methodName"` 中的 methodName 必须已定义\n\n### 确保可运行\n\n- 所有组件的 import 路径必须正确\n- 路由配置中引用的页面组件必须已创建\n- 事件处理函数必须有实际实现\n- 表单交互必须有反馈（成功提示、数据变更等）\n\n### 内容真实性\n\n- 所有文本使用真实、有意义的中文内容\n- 产品数据使用接近真实的模拟数据（名称、价格、描述等）\n- 禁止使用 "示例文字"、"这里是描述"、"项目1" 等占位内容\n\n## 十、构建与部署验证\n\nVue 项目生成完成后需通过以下验证：\n\n- `npm install` 成功安装所有依赖（无报错）\n- `npm run build` 成功构建生产版本（无 Vite/Rollup 报错）\n- 构建产物 dist 目录包含完整的静态文件\n- 页面在浏览器中正常渲染，路由跳转正常\n\n常见构建失败原因及预防：\n\n- 引用了不存在的本地资源文件——使用在线 URL 替代\n- 组件 import 路径拼写错误——创建文件后立即在路由或父组件中正确引用\n- package.json 依赖版本不兼容——使用经过验证的稳定版本组合\n- vite.config.js 配置错误——直接使用参考配置，不做额外修改\n',
    'VUE_PROJECT',
    'Vue 3 + Vite 项目工程模式知识库：文件操作工具、素材搜集工具、hash路由、依赖管理、构建验证、组件运行时错误防范',
    1,
    30,
    NULL,
    NOW(),
    NOW(),
    0
);
