# RichCodeWeaver - 织码睿奇

<div align="center">

![Version](https://img.shields.io/badge/version-1.0--SNAPSHOT-blue)
![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.3-brightgreen)
![Vue](https://img.shields.io/badge/Vue-3.5.17-green)
![License](https://img.shields.io/badge/license-MIT-blue)

**AI驱动的智能代码生成平台 | 将创意灵感转化为数字现实**

[在线体验](#) | [快速开始](#快速开始) | [文档](#文档) | [贡献指南](#贡献指南)

</div>

---

## 项目简介

RichCodeWeaver（织码睿奇）是一个基于AI技术的智能代码生成平台，旨在通过自然语言理解和大语言模型技术，帮助用户快速将创意想法转化为可运行的数字产物。平台采用微服务架构，支持多种AI模型集成，提供从需求描述到代码生成、可视化编辑、部署分享的全流程解决方案。

###  核心特性

- ** 智能代码生成**：基于LangChain4j和LangGraph4j的AI工作流，支持多轮对话式代码生成
- ** 可视化编辑**：实时预览和可视化编辑功能，所见即所得
- ** 智能提示模板**：根据用户身份和行业智能匹配提示词模板
- ** Agent模式**：支持传统生成和Agent智能生成两种模式
- ** 社交互动**：点赞、收藏、评论、分享等社交功能
- ** 多身份支持**：个人、商家、企业三种身份，覆盖15+行业
- ** 云端部署**：生成的数字产物支持一键部署和分享

---

##  技术架构

### 后端技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Java | 21 | 核心开发语言 |
| Spring Boot | 3.5.3 | 应用框架 |
| Spring Cloud | 2023.0.1 | 微服务框架 |
| Spring Cloud Alibaba | 2023.0.1.0 | 阿里云微服务组件 |
| Dubbo | 3.3.0 | RPC框架 |
| Nacos | - | 服务注册与配置中心 |
| Sentinel | - | 流量控制与熔断降级 |
| MyBatis Flex | 1.11.1 | ORM框架 |
| MySQL | 8.0.33 | 关系型数据库 |
| Redis | - | 缓存与分布式锁 |
| Redisson | 3.50.0 | Redis客户端 |
| LangChain4j | 1.1.0 | AI应用开发框架 |
| LangGraph4j | 1.6.0-rc2 | AI工作流编排 |
| Alibaba DashScope | 2.21.1 | 阿里云百炼AI SDK |

### 前端技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Vue | 3.5.17 | 前端框架 |
| TypeScript | 5.8.0 | 类型安全 |
| Vite | 7.0.0 | 构建工具 |
| Vue Router | 4.5.1 | 路由管理 |
| Pinia | 3.0.3 | 状态管理 |
| Ant Design Vue | 4.2.6 | UI组件库 |
| Vant | 4.9.21 | 移动端组件库 |
| Axios | 1.11.0 | HTTP客户端 |
| Markdown-it | 14.1.0 | Markdown渲染 |
| md-editor-v3 | 6.4.1 | Markdown编辑器 |

---

##  模块说明

### 后端模块

```
rich-code-weaver-parent/
├── rich-code-weaver-common          # 通用模块 - 公共工具类、常量、异常处理
├── rich-code-weaver-model           # 数据模型模块 - 实体类、VO、DTO定义
├── rich-code-weaver-client          # 内部接口模块 - Dubbo服务接口定义
├── rich-code-weaver-user            # 用户模块 - 用户管理、认证授权（端口：8101）
├── rich-code-weaver-generator       # 产物生成模块 - 核心代码生成引擎（端口：8102）
├── rich-code-weaver-ai              # AI模块 - AI服务封装、提示词管理（端口：8103）
├── rich-code-weaver-file            # 文件模块 - 文件上传、存储管理（端口：8104）
├── rich-code-weaver-prompt          # 提示词模板模块 - 模板管理与匹配（端口：8105）
├── rich-code-weaver-social          # 社交模块 - 点赞、收藏、评论、分享（端口：8106）
└── rich-code-weaver-tmp             # 临时模块 - 临时文件处理（端口：8107）
```

#### 模块详细说明

**1. rich-code-weaver-user（用户模块）**
- 用户注册、登录、登出
- 用户信息管理（支持身份、行业属性）
- Session管理（基于Redis）
- 权限控制（用户/管理员）

**2. rich-code-weaver-generator（产物生成模块）**
- AI代码生成核心引擎
- 支持传统模式和Agent模式
- LangGraph4j工作流编排
- 生成历史管理
- 产物评分与推荐

**3. rich-code-weaver-ai（AI模块）**
- LangChain4j集成
- 多AI模型支持（OpenAI、阿里云百炼）
- 提示词优化
- 对话历史管理（Redis存储）
- SSE流式响应

**4. rich-code-weaver-prompt（提示词模板模块）**
- 提示词模板CRUD
- 智能匹配算法（身份+行业）
- 模板字段动态渲染
- 模板评分排序

**5. rich-code-weaver-social（社交模块）**
- 点赞/取消点赞（Redis分布式锁）
- 收藏/取消收藏
- 评论管理（支持评论点赞）
- 分享统计
- 热门排行榜（综合评分算法）

**6. rich-code-weaver-file（文件模块）**
- 文件上传（支持图片、文档等）
- 文件存储管理
- 文件访问控制

**7. rich-code-weaver-tmp（临时模块）**
- 临时文件生成与清理
- 缓存管理

### 前端模块

```
rich-code-weaver-web/
├── src/
│   ├── api/                         # API接口定义（自动生成）
│   ├── assets/                      # 静态资源
│   ├── components/                  # 公共组件
│   │   ├── sidebar/                 # 侧边栏组件
│   │   ├── workspace/               # 工作区组件
│   │   ├── IdentitySetupModal.vue   # 身份设置弹窗
│   │   └── PromptTemplateDialog.vue # 模板选择对话框
│   ├── pages/                       # 页面组件
│   │   ├── WorkspacePage.vue        # 主工作区页面
│   │   ├── app/                     # 应用相关页面
│   │   ├── admin/                   # 管理员页面
│   │   ├── user/                    # 用户页面
│   │   └── other/                   # 其他页面
│   ├── stores/                      # Pinia状态管理
│   ├── router/                      # 路由配置
│   ├── constants/                   # 常量定义
│   └── access.ts                    # 权限控制
└── public/                          # 公共资源
```

---

## 快速开始

### 环境要求

- **JDK**: 21+
- **Maven**: 3.8+
- **Node.js**: 18+
- **MySQL**: 8.0+
- **Redis**: 6.0+
- **Nacos**: 2.x
- **Sentinel**: 1.8+

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

# 导入SQL脚本
mysql -u root -p rich_code_weaver < sql/RichCodeWeaverDB_MySQL.sql
mysql -u root -p rich_code_weaver < sql/V2_prompt_template_and_user_identity.sql
mysql -u root -p rich_code_weaver < sql/V2_prompt_template_data.sql
mysql -u root -p rich_code_weaver < sql/V3_social_module.sql
```

3. **启动基础设施**
```bash
# 启动Nacos（注册中心）
cd bat
./run_nacos.bat

# 启动Sentinel（流量控制）
./run_sentinel_dashboard.bat
```

4. **配置应用**

修改各模块的 `application-local.yml` 配置文件：
- 数据库连接信息
- Redis连接信息
- Nacos地址
- AI模型API Key

5. **启动微服务**

按以下顺序启动各模块：
```bash
# 1. 用户模块
cd rich-code-weaver-user
mvn spring-boot:run

# 2. AI模块
cd rich-code-weaver-ai
mvn spring-boot:run

# 3. 文件模块
cd rich-code-weaver-file
mvn spring-boot:run

# 4. 提示词模块
cd rich-code-weaver-prompt
mvn spring-boot:run

# 5. 产物生成模块
cd rich-code-weaver-generator
mvn spring-boot:run

# 6. 社交模块
cd rich-code-weaver-social
mvn spring-boot:run

# 7. 临时模块
cd rich-code-weaver-tmp
mvn spring-boot:run
```

或使用批处理脚本一键启动：
```bash
cd bat
./run_all.bat
```

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

### 构建部署

**后端打包**
```bash
mvn clean package -DskipTests
```

**前端打包**
```bash
cd rich-code-weaver-web
npm run build
```

---

## 核心功能

### 1. AI代码生成

- **传统模式**：单次生成，快速响应
- **Agent模式**：多步骤工作流，智能分析需求、生成代码、优化迭代

### 2. 可视化编辑

- 实时预览生成的数字产物
- 点击元素进行可视化编辑
- 支持对话式修改

### 3. 提示词模板系统

- 16+预置行业模板
- 智能匹配算法（精确匹配 → 行业匹配 → 通用模板）
- 动态字段渲染（select、text、switch类型）
- 模板预览与编辑

### 4. 社交互动

- 点赞/收藏数字产物
- 评论与回复
- 分享统计
- 热门排行榜

### 5. 用户管理

- 三种身份：个人、商家、企业
- 15+行业分类
- 个人中心
- 历史记录

---

## 数据库设计

### 核心表结构

- **user** - 用户表（含身份、行业字段）
- **generator** - 产物表
- **prompt_template** - 提示词模板表
- **app_hot_stat** - 产物热度统计表
- **app_like** - 点赞表
- **app_favorite** - 收藏表
- **app_comment** - 评论表
- **app_comment_like** - 评论点赞表
- **app_share** - 分享表

---

## 开发工具

### API文档

访问 Knife4j API文档：
- 用户模块：`http://localhost:8101/doc.html`
- 产物模块：`http://localhost:8102/doc.html`
- AI模块：`http://localhost:8103/doc.html`
- 其他模块类推...

### 代码生成

前端API自动生成：
```bash
cd rich-code-weaver-web
npm run openapi2ts
```

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

**MO-JIE TEAM**
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

**⭐ 如果这个项目对你有帮助，请给个Star支持一下！⭐**

Made with ❤️ by MO-JIE TEAM

</div>
