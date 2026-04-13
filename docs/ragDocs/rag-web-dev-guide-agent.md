# Agent 代码生成规范

本文档定义 Agent 自主代码生成模式的工作流程、工具使用规范和关键约束。

## 代码生成类型选择规范

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

## 运行环境硬约束规范

生成的代码部署为纯静态文件，**没有任何后端服务**：

- 无 Node.js 运行时
- 无数据库（MySQL/MongoDB/Redis 均不可用）
- 无服务端 API
- 无服务端会话

### 数据持久化唯一方案：localStorage

```javascript
// 存储
localStorage.setItem("key", JSON.stringify(data));
// 读取（必须 try-catch 保护）
try {
  const data = JSON.parse(localStorage.getItem("key")) || defaultValue;
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

## 图片资源规范

### 来源优先级
1. 系统消息提供的素材 URL（必须全部使用）
2. searchImages 工具搜索的图片
3. picsum.photos 占位图（仅补充不足）

### 禁止事项
- 禁止引用本地路径（./images/xxx.png 不存在）
- 禁止 import 未创建的 assets 文件（Vue 构建会失败）

## 致命错误预防规范

### JavaScript 运行时错误（最常见失败原因）

```javascript
// 错误：元素不存在导致 null 错误
document.getElementById("不存在的id").addEventListener("click", fn);

// 正确：判空保护
const el = document.getElementById("myId");
if (el) {
  el.addEventListener("click", fn);
}

// 正确：确保 DOM 加载完成
document.addEventListener("DOMContentLoaded", () => {
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

## 文件检查清单创建规范

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
