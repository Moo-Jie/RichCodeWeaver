-- =====================================================
-- V7: Agent 模式专用 RAG 知识库文档
-- 
-- 背景：Agent 模式在代码生成开始时 codeGenType 为 ai_strategy（未确定），
--       无法像工作流模式那样在一开始就注入对应类型的 RAG 知识。
--       因此需要创建一个 codeGenType=AGENT 的专用知识库。
--
-- RAG 隔离机制：
--   - 工作流模式：检索 HTML/MULTI_FILE/VUE_PROJECT + GENERAL 类型的文档
--   - Agent 模式：只检索 AGENT 类型的文档（通过 createAgentRetrievalAugmentor）
--   - 两种模式的 RAG 知识库完全隔离，互不干扰
--
-- 代码修改：
--   - RagContentRetrieverAugmentorFactory.java: 新增 createAgentRetrievalAugmentor() 方法
--   - AiCodeGenAgentServiceFactory.java: 集成 Agent 专用 RAG 检索增强器
--   - RagManagePage.vue: 新增 AGENT 类型选项
--
-- 使用方式：
--   1. 执行本 SQL 脚本插入 RAG 文档
--   2. 在管理后台点击"刷新向量库"按钮，触发重新索引
--   3. Agent 模式下 RAG 会自动注入到每次 AI 调用中
--
-- 设计原则：
--   - 内容精炼，避免冗余信息干扰 AI 决策
--   - 明确约束边界，防止 AI 产生幻觉
--   - 提供可操作的具体指引，而非抽象描述
-- =====================================================

-- 插入 Agent 模式专用 RAG 文档
INSERT INTO `rag_document` (`id`, `docTitle`, `docContent`, `codeGenType`, `description`, `isEnabled`, `sortOrder`, `userId`, `createTime`, `updateTime`, `isDelete`)
VALUES (
    4,
    'Agent 代码生成工作流规范',
    '# Agent 代码生成工作流规范

本文档定义 Agent 自主代码生成模式的工作流程、工具使用规范和关键约束。

## 一、强制工作流程

Agent 必须严格按以下顺序执行，不可跳过或乱序：

1. **分析需求** → 理解用户意图，评估项目复杂度
2. **调用 setCodeGenType** → 确定技术方案（single_html / multi_file / vue_project）
3. **创建文件** → 使用 creatAndWrite 按类型要求创建所有必需文件
4. **自检验证** → 使用 readDir 检查文件结构完整性
5. **构建项目** → 仅 vue_project 类型需调用 buildProject
6. **调用 exit** → 结束任务，输出完成总结

**关键约束**：必须先调用 setCodeGenType 再创建任何文件，否则文件会写入错误目录。

## 二、代码生成类型选择

### single_html
- 适用：简单展示页、活动页、落地页
- 文件：仅 index.html（CSS/JS 内联）
- 禁止：Vue/React 框架、npm 包、TypeScript
- 无需构建

### multi_file
- 适用：中等复杂度单页应用
- 文件：固定三个 index.html + style.css + script.js
- 禁止：创建其他文件、Vue/React 框架、npm 包
- 无需构建

### vue_project
- 适用：多页面应用、需要路由、组件化开发
- 必须文件：package.json、vite.config.js、index.html、src/main.js、src/App.vue
- 路由：必须使用 hash 模式
- 禁止：TypeScript、Pinia/Vuex
- 必须调用 buildProject 构建

## 三、运行环境硬约束

生成的代码部署为纯静态文件，**没有任何后端服务**：

- 无 Node.js 运行时
- 无数据库（MySQL/MongoDB/Redis 均不可用）
- 无服务端 API
- 无服务端会话

### 数据持久化唯一方案：localStorage

```javascript
// 存储
localStorage.setItem(\"key\", JSON.stringify(data));
// 读取（必须 try-catch 保护）
try {
  const data = JSON.parse(localStorage.getItem(\"key\")) || defaultValue;
} catch (e) {
  const data = defaultValue;
}
```

### 绝对不可实现的功能

- 服务端用户注册/登录持久化
- 发送邮件/短信
- 文件上传到服务器
- 需要 API Key 的第三方服务（除非用户提供）
- WebSocket 实时通信
- 多用户数据同步

**处理原则**：用 localStorage 模拟本地体验，禁止留空函数或 TODO 注释。

## 四、图片资源规范

### 来源优先级
1. 系统消息提供的素材 URL（必须全部使用）
2. searchImages 工具搜索的图片
3. picsum.photos 占位图（仅补充不足）

### 禁止事项
- 禁止引用本地路径（./images/xxx.png 不存在）
- 禁止 import 未创建的 assets 文件（Vue 构建会失败）

## 五、致命错误防范

### JavaScript 运行时错误（最常见失败原因）

```javascript
// 错误：元素不存在导致 null 错误
document.getElementById(\"不存在的id\").addEventListener(\"click\", fn);

// 正确：判空保护
const el = document.getElementById(\"myId\");
if (el) {
  el.addEventListener(\"click\", fn);
}

// 正确：确保 DOM 加载完成
document.addEventListener(\"DOMContentLoaded\", () => {
  // 所有 DOM 操作放这里
});
```

**核心原则**：JS 中引用的每个 id/class 必须在 HTML 中真实存在。

### Vue 项目构建失败

- import 路径必须与实际文件路径完全一致（大小写敏感）
- ref/reactive 变量名必须与模板引用一致
- v-for 必须配合 :key
- 组件必须在使用前注册

### 代码质量红线

- 禁止 // TODO 注释
- 禁止空函数体
- 禁止 console.log
- 禁止无功能按钮
- 禁止占位文本（Lorem Ipsum、示例文字）

## 六、文件创建检查清单

### single_html
- index.html 存在且结构完整
- 所有 CSS 类有定义
- 所有 JS 函数有实现
- 所有 DOM 引用的 id 存在

### multi_file
- 三个文件都存在
- index.html 正确引用 style.css 和 script.js
- script.js 选择器与 HTML 元素匹配

### vue_project
- package.json 依赖正确
- 路由配置的组件都已创建
- buildProject 构建成功
',
    'AGENT',
    'Agent 自主代码生成模式的核心工作流规范，包含强制流程、类型选择、环境约束、错误防范',
    1,
    0,
    NULL,
    NOW(),
    NOW(),
    0
);

-- 验证插入结果
SELECT id, docTitle, codeGenType, description, isEnabled FROM rag_document WHERE id = 4;
