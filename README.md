# RichCodeWeaver - 织码睿奇

<div align="center">

![Version](https://img.shields.io/badge/version-2.0.0-blue)
![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.3-brightgreen)
![Vue](https://img.shields.io/badge/Vue-3.5.17-green)
![License](https://img.shields.io/badge/license-MIT-blue)

**面向数字产物创作全过程的智能开发平台 | 从创意到落地的完整解决方案**

[在线体验](https://TemporarilyNotDisclosed.com) | [快速开始](#快速开始) | [文档](#文档) | [贡献指南](#贡献指南)

</div>

---

## 项目简介

RichCodeWeaver（织码睿奇）是一款面向全社会的 AI 驱动智能开发平台，通过调用 AI 大模型能力，实现用户创意、工作业务与日常需求向可执行数字产物的快速转化。平台提供**需求分析、素材选配、代码生成、可视化编辑、项目部署及分享传播**的一体化解决方案。

### 设计理念

本项目解决的核心问题：
- **个人用户**：有明确的页面或应用想法，但缺少完整的开发能力
- **商户用户**：需要快速搭建宣传页面、业务展示等数字产物
- **企业团队**：借助通用 AI 工具生成的代码分散、难预览、难修改，更难形成可部署、可传播、可协作的完整产物

### 核心特性

#### 双模式 AI 生成
- **工作流模式**：基于 LangGraph4j 的节点式工作流，流程可控、阶段清晰、结果稳定
- **Agent 模式**：自主规划、工具协同，适合复杂任务和工程化应用生成

#### 多种产物形态
- **单页代码**：快速生成单文件 HTML 页面
- **多文件代码**：支持多文件结构的复杂项目
- **Vue 工程**：完整的 Vue3 + Vite 工程框架，支持依赖安装和构建

#### 智能增强系统
- **提示词优化**：AI 自动优化用户输入，提升生成质量
- **行业模板**：16+ 预置行业模板，智能匹配用户身份和行业
- **RAG 知识库**：基于 pgvector 的向量检索，持续积累生成经验
- **历史上下文**：Redis 存储对话记忆，支持连续迭代优化

#### 🛠工程化落地
- **实时预览**：生成过程中即可预览效果
- **可视化编辑**：点击元素进行可视化修改
- **构建验证**：自动执行依赖安装和项目构建
- **一键部署**：支持云端部署和访问

#### 社交传播
- **互动功能**：点赞、收藏、评论、分享
- **热门排行**：综合评分算法，推荐优质产物
- **协作邀请**：多人协同创作和编辑
- **社区交流**：经验分享、问题讨论

#### AI 客服
- **RAG 增强**：深入学习系统功能和能力边界
- **流式对话**：支持断线续接和历史恢复
- **首页常驻**：随时解答使用问题

### 与同类产品对比

| 比较维度 | 通用 AI 代码助手 | 织码睿奇 |
|---------|----------------|---------|
| 主要输入方式 | 文本指令、代码补全 | 自然语言 + 优化输入 + 提示词模板 + 多模式选择 |
| 主要输出形态 | 代码片段或局部工程 | 单页代码、多文件代码、Vue 工程框架 |
| 修改方式 | 继续提问或手工改代码 | 对话修改 + 可视化编辑 + 构建校验 |
| 落地能力 | 往往需要自行收尾 | 已形成预览、构建、部署闭环 |
| 传播与互动 | 一般较弱 | 支持点赞、收藏、评论、分享、社区 |
| 面向人群 | 开发者 | 同时面向普通用户与企业团队用户 |

---

## 🏗技术架构

### 架构设计理念

本项目采用**平台化多模块架构**，将用户、生成、社交、提示词、文件等核心业务拆解为**边界清晰的独立模块**，同时通过统一模型、公共常量与内部服务接口**保持整体一致性**。这种设计使系统既能支撑**生成数字产物**这种高复杂度的主要业务，又能不断扩展**协作、社区、客服、知识库**等外围能力。

### 后端技术栈

| 技术                   | 版本         | 说明                    |
|----------------------|------------|------------------------|
| Java                 | 21         | 核心开发语言，支持虚拟线程        |
| Spring Boot          | 3.5.3      | 应用框架                  |
| Spring Cloud         | 2023.0.1   | 微服务框架                 |
| Spring Cloud Alibaba | 2023.0.1.0 | 阿里云微服务组件              |
| Dubbo                | 3.3.0      | RPC 框架                |
| Nacos                | 2.x        | 服务注册与配置中心             |
| Sentinel             | 1.8+       | 流量控制与熔断降级             |
| MyBatis Flex         | 1.11.1     | ORM 框架                |
| MySQL                | 8.0.33     | 关系型数据库                |
| PostgreSQL + pgvector| -          | 向量数据库，支持 RAG 知识库     |
| Redis                | 6.0+       | 缓存、分布式锁、对话记忆          |
| Redisson             | 3.50.0     | Redis 客户端，分布式限流      |
| Caffeine             | -          | 本地缓存，AI 服务实例复用       |
| LangChain4j          | 1.1.0      | AI 应用开发框架             |
| LangGraph4j          | 1.6.0-rc2  | AI 工作流编排              |
| Alibaba DashScope    | 2.21.1     | 阿里云百炼 AI SDK          |
| Prometheus           | 3.11.2     | 监控指标采集                |
| Grafana              | 12.1.0     | 可视化监控看板               |

### 前端技术栈

| 技术             | 版本     | 说明              |
|----------------|--------|-----------------|
| Vue            | 3.5.17 | 前端框架            |
| TypeScript     | 5.8.0  | 类型安全            |
| Vite           | 7.0.0  | 构建工具            |
| Vue Router     | 4.5.1  | 路由管理            |
| Pinia          | 3.0.3  | 状态管理            |
| Ant Design Vue | 4.2.6  | UI 组件库          |
| Vant           | 4.9.21 | 移动端组件库          |
| Axios          | 1.11.0 | HTTP 客户端        |
| Markdown-it    | 14.1.0 | Markdown 渲染     |
| md-editor-v3   | 6.4.1  | Markdown 编辑器    |
| EventSource    | -      | SSE 流式响应，支持断线续接 |

### AI 模型使用

| 类型        | 模型名称 / 版本                    | 主要用途                      |
|-----------|------------------------------|---------------------------|
| 代码与通用对话 | qwen-plus-2025-12-01         | 提示词优化、生成策略判断、AI 客服       |
| 代码生成    | qwen3-coder-plus-2025-09-23  | 流式代码生成、Agent 自主规划生成      |
| 向量模型    | text-embedding-v4            | RAG 知识库向量化               |
| 文生图     | wan2.2-t2i-flash             | 在 AI 产物构建时生成 Logo 作为构建素材 |

---

## 模块说明

### 后端模块架构

```
rich-code-weaver-parent/
├── rich-code-weaver-common          # 通用模块 - 公共工具类、常量、异常处理
├── rich-code-weaver-model           # 数据模型模块 - 实体类、VO、DTO 定义
├── rich-code-weaver-client          # 内部接口模块 - Dubbo 服务接口定义
├── rich-code-weaver-user            # 用户模块 - 用户管理、认证授权（端口：8102）
├── rich-code-weaver-generator       # 产物生成模块 - 核心代码生成引擎（端口：8101）
├── rich-code-weaver-ai              # AI 模块 - AI 服务封装、提示词管理
├── rich-code-weaver-file            # 文件模块 - 文件上传、存储管理（端口：8103）
├── rich-code-weaver-prompt          # 提示词模板模块 - 模板管理与匹配（端口：8105）
├── rich-code-weaver-social          # 社交模块 - 点赞、收藏、评论、分享（端口：8104）
└── rich-code-weaver-server-admin    # 管理后台 - Spring Boot Admin（端口：9001）
```

#### 核心模块详解

**1. rich-code-weaver-generator（产物生成模块）** - 端口：8101

核心 AI 生成引擎，系统最复杂的模块：
- **双模式生成**：工作流模式（LangGraph4j）+ Agent 模式
- **工作流编排**：资源收集 → 提示优化 → 类型决策 → 代码生成 → 质量审查 → 项目构建
- **Agent 自主规划**：自主拆解任务、工具协同、连续推进
- **流式交互**：SSE 流式响应，支持断线续接和事件缓存
- **产物构建**：支持单页、多文件、Vue 工程三种形态
- **历史管理**：Redis 存储对话记忆，支持连续迭代
- **监控采集**：Prometheus 指标暴露，监控 Token 消耗和响应时长

**2. rich-code-weaver-user（用户模块）** - 端口：8102

用户身份与权限管理：
- 用户注册、登录、登出
- 用户信息管理（支持身份、行业属性）
- Session 管理（基于 Redis）
- 权限控制（用户/管理员）
- 好友关系管理

**3. rich-code-weaver-ai（AI 模块）**

AI 服务封装与增强：
- LangChain4j 集成
- 多 AI 模型支持（阿里云百炼）
- 提示词优化服务
- RAG 知识库检索（pgvector）
- 对话历史管理（Redis 存储）
- AI 服务实例缓存（Caffeine）

**4. rich-code-weaver-prompt（提示词模板模块）** - 端口：8105

智能模板匹配系统：
- 提示词模板 CRUD
- 智能匹配算法（精确匹配 → 行业匹配 → 通用模板）
- 模板字段动态渲染（select、text、switch 类型）
- 16+ 预置行业模板
- 模板评分排序

**5. rich-code-weaver-social（社交模块）** - 端口：8104

社交传播与社区运营：
- 点赞/取消点赞（Redis 分布式锁）
- 收藏/取消收藏
- 评论管理（支持评论点赞）
- 分享统计
- 热门排行榜（综合评分算法）
- 协作邀请与多人编辑

**6. rich-code-weaver-file（文件模块）** - 端口：8103

文件存储与管理：
- 文件上传（支持图片、文档等）
- OSS 云存储集成
- 文件访问控制
- 素材管理（图片、文本、视频、音频、链接）

**7. rich-code-weaver-server-admin（管理后台）** - 端口：9001

Spring Boot Admin 监控：
- 微服务健康检查
- 应用状态监控
- 日志查看

### 前端模块结构

```
rich-code-weaver-web/
├── src/
│   ├── api/                         # API 接口定义（自动生成）
│   ├── assets/                      # 静态资源
│   ├── components/                  # 公共组件
│   │   ├── sidebar/                 # 侧边栏组件
│   │   │   ├── SidebarAppList.vue   # 产物列表
│   │   │   └── TheSidebar.vue       # 主侧边栏
│   │   ├── workspace/               # 工作区组件
│   │   │   ├── ChatInput.vue        # 对话输入
│   │   │   ├── ChatMessages.vue     # 消息展示
│   │   │   ├── AppPreview.vue       # 产物预览
│   │   │   ├── CustomerServicePanel.vue # AI 客服面板
│   │   │   ├── MaterialSelector.vue # 素材选择器
│   │   │   ├── ModeSwitch.vue       # 模式切换
│   │   │   └── TaskPlanPanel.vue    # 任务计划面板
│   │   ├── admin/                   # 管理员组件
│   │   ├── chat/                    # 聊天组件
│   │   ├── community/               # 社区组件
│   │   ├── IdentitySetupModal.vue   # 身份设置弹窗
│   │   ├── PromptTemplateDialog.vue # 模板选择对话框
│   │   └── DeploySuccessModal.vue   # 部署成功弹窗
│   ├── pages/                       # 页面组件
│   │   ├── WorkspacePage.vue        # 主工作区页面
│   │   ├── HomePage.vue             # 首页
│   │   ├── app/                     # 应用相关页面
│   │   │   ├── AppChatPage.vue      # 产物对话页
│   │   │   ├── MyAppsPage.vue       # 我的产物
│   │   │   ├── AllAppsPage.vue      # 热门产物
│   │   │   └── MyFavoritesPage.vue  # 我的收藏
│   │   ├── admin/                   # 管理员页面
│   │   │   ├── SystemManageDashboard.vue # 系统管理仪表盘
│   │   │   ├── AppManagePage.vue    # 产物管理
│   │   │   ├── UserManagePage.vue   # 用户管理
│   │   │   ├── PromptTemplateManagePage.vue # 模板管理
│   │   │   ├── SystemPromptManagePage.vue # 系统提示词管理
│   │   │   ├── RagManagePage.vue    # RAG 知识库管理
│   │   │   ├── MaterialManagePage.vue # 素材管理
│   │   │   ├── GrafanaMonitorPage.vue # Grafana 监控
│   │   │   └── HigressManagePage.vue # 网关管理
│   │   ├── user/                    # 用户页面
│   │   ├── community/               # 社区页面
│   │   ├── material/                # 素材页面
│   │   └── other/                   # 其他页面
│   ├── stores/                      # Pinia 状态管理
│   │   ├── appStore.ts              # 产物状态
│   │   ├── chatStore.ts             # 对话状态
│   │   └── loginUser.ts             # 登录用户状态
│   ├── router/                      # 路由配置
│   ├── constants/                   # 常量定义
│   │   ├── identityOptions.ts       # 身份选项
│   │   ├── prompts.ts               # 提示词常量
│   │   └── adminNavItems.ts         # 管理员导航
│   ├── utils/                       # 工具函数
│   │   ├── streamChunkParser.ts     # 流式数据解析
│   │   ├── toolCallParser.ts        # 工具调用解析
│   │   └── workflowMarkerParser.ts  # 工作流标记解析
│   └── access.ts                    # 权限控制
└── public/                          # 公共资源
```

---

## 快速开始

### 环境要求

| 环境项              | 版本要求   | 说明                  |
|------------------|--------|---------------------|
| JDK              | 21+    | 后端运行环境，支持虚拟线程       |
| Maven            | 3.8+   | 后端构建工具              |
| Node.js          | 18+    | 前端运行环境              |
| MySQL            | 8.0+   | 业务数据持久化             |
| PostgreSQL + pgvector | -      | 向量数据库，支持 RAG 知识库（可选）   |
| Redis            | 6.0+   | 缓存、分布式锁、对话记忆        |
| Nacos            | 2.x    | 服务注册与配置中心           |
| Sentinel         | 1.8+   | 流量控制与熔断降级（可选）       |
| Nginx            | -      | 前端静态资源与反向代理（生产环境）   |
| Prometheus       | 3.11+  | 监控指标采集（可选）          |
| Grafana          | 12.1+  | 可视化监控看板（可选）         |

### 后端启动

1. **克隆项目**

```bash
git clone https://github.com/Moo-Jie/RichCodeWeaver.git
cd RichCodeWeaver
```

2. **初始化数据库**

```bash
# 创建数据库
mysql -u root -p
CREATE DATABASE rich_code_weaver CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

# 导入 SQL 脚本
mysql -u root -p rich_code_weaver < sql/RichCodeWeaverDB_MySQL.sql

# （可选）如果使用 PostgreSQL + pgvector 作为向量数据库
# 解压并导入 PostgreSQL 脚本
gunzip sql/RichCodeWeaverDB_PgSQL.gz
psql -U postgres -d rich_code_weaver_vector < sql/RichCodeWeaverDB_PgSQL
```

3. **启动基础设施**

```bash
# 启动 Redis
# Windows
cd bat
start-redis.bat

# Linux
redis-server

# 启动 Nacos（注册中心）
cd bat
run_nacos.bat  # Windows
# 或
cd middleware/nacos/bin
sh startup.sh -m standalone  # Linux

# （可选）启动 Sentinel（流量控制）
cd bat
run_sentinel_dashboard.bat

# （可选）启动 Prometheus（监控）
cd bat
start-prometheus.bat  # Windows
# 或
cd middleware/prometheus-3.11.2.linux-amd64
nohup ./prometheus --config.file=prometheus.yml > logs/prometheus.log 2>&1 &  # Linux

# （可选）启动 Grafana（可视化监控）
cd bat
start-grafana.bat  # Windows
# 或
cd middleware/grafana-12.1.0
nohup ./bin/grafana-server web > logs/grafana.log 2>&1 &  # Linux
```

4. **配置应用**

修改各模块的 `application-local.yml` 配置文件，重点配置以下内容：

- **数据库连接信息**（MySQL）
- **Redis 连接信息**
- **Nacos 地址**
- **AI 模型 API Key**（阿里云百炼 DashScope）
- **OSS 云存储配置**（阿里云 OSS）
- **邮箱配置**（用于注册验证）
- **RAG 配置**（PostgreSQL + pgvector，可选）

5. **启动微服务**

推荐按以下顺序启动各模块：

```bash
# 方式一：使用批处理脚本一键启动（Windows）
cd bat
run_all.bat

# 方式二：手动启动各模块
# 1. 产物生成模块（核心）
cd rich-code-weaver-generator
mvn spring-boot:run

# 2. 用户模块
cd rich-code-weaver-user
mvn spring-boot:run

# 3. 文件模块
cd rich-code-weaver-file
mvn spring-boot:run

# 4. 社交模块
cd rich-code-weaver-social
mvn spring-boot:run

# 5. 提示词模块
cd rich-code-weaver-prompt
mvn spring-boot:run

# 6. 管理后台（可选）
cd rich-code-weaver-server-admin
mvn spring-boot:run
```

**服务端口说明：**
- 8101：产物生成模块（generator）
- 8102：用户模块（user）
- 8103：文件模块（file）
- 8104：社交模块（social）
- 8105：提示词模块（prompt）
- 9001：管理后台（admin）

### 前端启动

1. **安装依赖**

```bash
cd rich-code-weaver-web
npm install
```

2. **启动开发服务器**

```bash
npm run dev
```

3. **访问应用**

打开浏览器访问：`http://localhost:5174`

**测试账号：**
- 账号：`rcwadmin@outlook.com`
- 密码：`richCodeWeaverAdmin`

### 生产环境部署

#### 后端打包

```bash
# 打包所有模块
mvn clean package -DskipTests

# 打包后的 JAR 文件位于各模块的 target 目录
# rich-code-weaver-generator/target/rich-code-weaver-generator-1.0.jar
# rich-code-weaver-user/target/rich-code-weaver-user-1.0.jar
# ...
```

#### 前端打包

```bash
cd rich-code-weaver-web
npm run build

# 打包后的静态文件位于 dist 目录
```

#### Linux 服务器部署

详细部署步骤请参考 `docs/设计和开发文档.md` 第五章。

**目录规划：**
```bash
mkdir -p /opt/RichCodeWeaver/{server,web,middleware,logs}
```

- `server/`：存放 6 个后端 JAR
- `web/`：存放前端解压后的静态文件
- `middleware/`：存放 Nacos、Prometheus、Grafana 等中间件
- `logs/`：存放运行日志

**Nginx 配置：**

参考 `bat/nginx.conf` 配置模板，配置静态资源与反向代理。

---

## 核心功能详解

### 1. 双模式 AI 生成

#### 工作流模式（LangGraph4j）
- **流程可控**：节点式工作流，每个环节清晰可见
- **阶段清晰**：资源收集 → 提示优化 → 类型决策 → 代码生成 → 质量审查 → 项目构建
- **结果稳定**：适合结构相对明确的生成任务
- **护轨机制**：AI 生成失败或非法输出时自动重新生成

#### Agent 模式
- **自主规划**：AI 自主拆解任务、制定计划
- **工具协同**：结合多种工具完成复杂任务
- **连续推进**：支持多步骤、多轮次的复杂创作
- **适用场景**：工程化程度更高的应用生成

### 2. 智能增强系统

#### 提示词优化
- AI 自动分析用户输入
- 补充缺失的关键信息
- 优化表达方式，提升生成质量

#### 行业模板匹配
- 16+ 预置行业模板（教育、医疗、金融、电商、旅游等）
- 智能匹配算法：精确匹配 → 行业匹配 → 通用模板
- 动态字段渲染：select、text、switch 类型
- 模板预览与编辑

#### RAG 知识库
- **文档切割**：LangChain4j 文档处理，保留语义完整性
- **向量存储**：text-embedding-v4 向量模型 + pgvector 数据库
- **文档过滤**：通过业务类型、生成类型等元数据区分知识片段
- **查询增强**：内容检索器 + 内容注入器，自动补充上下文
- **持续优化**：管理员可一键向量化新知识到数据库

#### 历史上下文
- Redis 存储对话记忆
- 支持连续迭代优化
- 断线续接，恢复对话状态

### 3. 产物工程化落地

#### 单页代码
- 快速生成单文件 HTML 页面
- 实时预览
- 一键下载

#### 多文件代码
- 支持多文件结构的复杂项目
- 文件树展示
- 批量下载

#### Vue 工程
- 完整的 Vue3 + Vite 工程框架
- 自动执行依赖安装（npm install）
- 项目构建验证（npm run build）
- 构建产物部署
- 访问已部署站点

### 4. 可视化编辑

- **实时预览**：生成过程中即可预览效果
- **点击选中**：点击页面元素进行可视化修改
- **对话修改**：选中元素后，通过对话方式修改内容
- **所见即所得**：修改立即生效，无需刷新

### 5. 流式交互与断线续接

#### 流式响应（SSE）
- 实时返回生成过程
- 用户可持续观察进度
- 提升交互体验

#### 断线续接
- 服务端持续缓存流式事件（最多 10000 条）
- 客户端基于事件位置继续跟随
- 刷新页面或网络波动不影响任务执行
- 30 分钟会话保留窗口

### 6. 社交传播与社区

#### 互动功能
- **点赞**：Redis 分布式锁，保证数据一致性
- **收藏**：个人收藏夹，快速回看
- **评论**：支持评论点赞和回复
- **分享**：生成分享文案，统计分享次数

#### 热门排行
- 综合评分算法：点赞数 × 2 + 收藏数 × 3 + 评论数 × 1.5 + 分享数 × 4
- 热门产物推荐
- 社区置顶机制

#### 协作邀请
- 主创发起协作邀请
- 团队成员权限管理
- 多人协同编辑

#### 社区交流
- 发布讨论、问答、反馈、分享帖子
- 按分类与排序筛选内容
- 经验沉淀与问题讨论

### 7. AI 客服

- **RAG 增强**：深入学习系统功能和能力边界
- **流式对话**：支持断线续接和历史恢复
- **首页常驻**：随时解答使用问题
- **会话管理**：新建或切换历史会话
- **知识库支持**：基于 pgvector 的向量检索

### 8. 素材管理

- **多类型支持**：图片、文本、视频、音频、链接
- **分类管理**：按类别组织素材
- **快速选择**：生成时将素材选入工作台
- **提升效率**：复用素材，减少重复输入

---

## 🗄️ 数据库设计

### 核心表结构

按照职责划分，主要可以归纳为以下几类：

| **业务分类**   | **业务相关的核心表**                                                  | **主要作用**                      |
|------------|---------------------------------------------------------------|-------------------------------|
| 用户与身份数据  | user、user_friendship                                          | 支撑登录、权限、好友、个性化推荐等基础能力       |
| 产物与协作数据  | app、app_collaborator                                          | 支撑产物归属、协作编辑、团队化创作           |
| 生成过程数据   | chat_history                                                  | 支撑连续追问、生成记忆与过程回溯            |
| 社交传播数据   | app_hot_stat、app_like、app_favorite、app_share、app_comment      | 支撑热度排行、行为反馈与传播分析            |
| 社区交流数据   | community_post、community_reply                                | 支撑经验沉淀、问题讨论与内容运营            |
| AI 配置与知识数据 | prompt_template、rag_document、rag_param、system_prompt         | 支撑模板匹配、知识增强与能力持续优化          |
| 客服会话数据   | customer_service_conversation、customer_service_message         | 支撑首页 AI 客服的持续答疑与历史沉淀        |
| 素材管理数据   | material、material_category                                    | 支撑用户素材管理，提升创作效率             |

### 数据库初始化脚本

- `sql/RichCodeWeaverDB_MySQL.sql` - MySQL 完整建表脚本
- `sql/RichCodeWeaverDB_PgSQL.gz` - PostgreSQL 建表脚本（含 pgvector 扩展）

---

## 🛠️ 开发工具

### API 文档

访问 Knife4j API 文档：

- 产物生成模块：`http://localhost:8101/doc.html`
- 用户模块：`http://localhost:8102/doc.html`
- 文件模块：`http://localhost:8103/doc.html`
- 社交模块：`http://localhost:8104/doc.html`
- 提示词模块：`http://localhost:8105/doc.html`

### 前端 API 自动生成

```bash
cd rich-code-weaver-web
npm run openapi2ts
```

### 监控与运维

#### Prometheus 监控

访问：`http://localhost:9090`

监控指标包括：
- AI 模型请求次数
- Token 消耗统计
- 响应时长
- 错误次数

#### Grafana 可视化

访问：`http://localhost:3000`

导入监控看板：
- `docs/grafana/ai_model_monitoring_dashboard.json` - AI 模型监控看板
- `docs/grafana/higress-dashboard.json` - 网关监控看板

#### Spring Boot Admin

访问：`http://localhost:9001`

功能：
- 微服务健康检查
- 应用状态监控
- 日志查看

---

## 贡献指南

欢迎贡献代码、提出问题和建议！

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 提交 Pull Request

### 代码规范

- 后端遵循阿里巴巴Java开发手册
- 前端遵循Vue官方风格指南
- 提交信息遵循Conventional Commits规范

---

## 许可证

本项目采用 MIT 许可证 - 详见 [LICENSE](LICENSE) 文件

---

## 作者

**MO-JIE-RUBY TEAM**

- GitHub: [@Moo-Jie](https://github.com/Moo-Jie)
- Email: NotProvidedTemporarily@xxx.com

---

## 致谢

感谢以下开源项目：

- [Spring Boot](https://spring.io/projects/spring-boot)
- [Vue.js](https://vuejs.org/)
- [LangChain4j](https://github.com/langchain4j/langchain4j)
- [LangGraph4j](https://github.com/bsorrentino/langgraph4j)
- [MyBatis Flex](https://mybatis-flex.com/)
- [Ant Design Vue](https://antdv.com/)

---

## 联系我们

如有任何问题或建议，欢迎通过以下方式联系：

- 提交 [Issue](https://github.com/Moo-Jie/RichCodeWeaver/issues)
- 发送邮件至：NotProvidedTemporarily@xxx.com

---

<div align="center">

**如果这个项目对你有帮助，请给个Star支持一下！⭐**

Made with by MO-JIE-RUBY TEAM

</div>
