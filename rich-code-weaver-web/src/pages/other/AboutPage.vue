<template>
  <div class="about-page">
    <div class="page-header">
      <h1 class="page-title">关于 RichCodeWeaver</h1>
      <p class="page-desc">AI驱动的智能代码生成平台 · 将创意灵感转化为数字现实</p>
    </div>

    <div class="content-wrap">
      <section class="section">
        <h2 class="section-title">项目简介</h2>
        <p class="section-text">
          RichCodeWeaver（织码睿奇）是一个基于AI技术的智能代码生成平台，旨在通过自然语言理解和大语言模型技术，帮助用户快速将创意想法转化为可运行的数字产物。
          平台采用微服务架构，支持多种AI模型集成，提供从需求描述到代码生成、可视化编辑、部署分享的全流程解决方案。
        </p>
      </section>

      <section class="section">
        <h2 class="section-title">核心特性</h2>
        <div class="features-grid">
          <div class="feature-card">
            <div class="feature-number">01</div>
            <h3 class="feature-title">智能代码生成</h3>
            <p class="feature-desc">基于LangChain4j和LangGraph4j的AI工作流，支持多轮对话式代码生成，提供高质量的代码输出</p>
          </div>
          <div class="feature-card">
            <div class="feature-number">02</div>
            <h3 class="feature-title">可视化编辑</h3>
            <p class="feature-desc">实时预览和可视化编辑功能，所见即所得的创作体验，提升开发效率</p>
          </div>
          <div class="feature-card">
            <div class="feature-number">03</div>
            <h3 class="feature-title">智能提示模板</h3>
            <p class="feature-desc">根据用户身份和行业智能匹配提示词模板，快速开始创作，降低使用门槛</p>
          </div>
          <div class="feature-card">
            <div class="feature-number">04</div>
            <h3 class="feature-title">Agent模式</h3>
            <p class="feature-desc">支持传统生成和Agent智能生成两种模式，满足不同场景需求，灵活应对复杂任务</p>
          </div>
          <div class="feature-card">
            <div class="feature-number">05</div>
            <h3 class="feature-title">社交互动</h3>
            <p class="feature-desc">点赞、收藏、评论、分享等社交功能，构建创作者社区，促进知识共享</p>
          </div>
          <div class="feature-card">
            <div class="feature-number">06</div>
            <h3 class="feature-title">云端部署</h3>
            <p class="feature-desc">生成的数字产物支持一键部署和分享，快速上线，缩短产品交付周期</p>
          </div>
        </div>
      </section>

      <section class="section">
        <h2 class="section-title">技术架构</h2>
        <div class="tech-stack">
          <div class="tech-category">
            <h3 class="tech-category-title">后端技术栈</h3>
            <div class="tech-tags">
              <span class="tech-tag">Java 21</span>
              <span class="tech-tag">Spring Boot 3.5.3</span>
              <span class="tech-tag">Spring Cloud 2023.0.1</span>
              <span class="tech-tag">Dubbo 3.3.0</span>
              <span class="tech-tag">Nacos</span>
              <span class="tech-tag">Sentinel</span>
              <span class="tech-tag">MyBatis Flex</span>
              <span class="tech-tag">MySQL 8.0</span>
              <span class="tech-tag">Redis</span>
              <span class="tech-tag">LangChain4j</span>
              <span class="tech-tag">LangGraph4j</span>
            </div>
          </div>
          <div class="tech-category">
            <h3 class="tech-category-title">前端技术栈</h3>
            <div class="tech-tags">
              <span class="tech-tag">Vue 3.5.17</span>
              <span class="tech-tag">TypeScript 5.8</span>
              <span class="tech-tag">Vite 7.0</span>
              <span class="tech-tag">Pinia</span>
              <span class="tech-tag">Vue Router</span>
              <span class="tech-tag">Ant Design Vue</span>
              <span class="tech-tag">Vant</span>
              <span class="tech-tag">Axios</span>
            </div>
          </div>
        </div>
      </section>

      <section class="section">
        <h2 class="section-title">微服务模块</h2>
        <div class="modules-grid">
          <div class="module-card">
            <div class="module-header">
              <span class="module-name">用户模块</span>
              <span :class="['module-status', moduleHealth.user ? 'status-running' : 'status-stopped']">
                {{ moduleHealth.user ? '运行中' : '未运行' }}
              </span>
            </div>
            <p class="module-desc">用户管理、认证授权、Session管理</p>
          </div>
          <div class="module-card">
            <div class="module-header">
              <span class="module-name">产物生成模块</span>
              <span :class="['module-status', moduleHealth.generator ? 'status-running' : 'status-stopped']">
                {{ moduleHealth.generator ? '运行中' : '未运行' }}
              </span>
            </div>
            <p class="module-desc">AI代码生成核心引擎、工作流编排</p>
          </div>
          <div class="module-card">
            <div class="module-header">
              <span class="module-name">AI模块</span>
              <span :class="['module-status', moduleHealth.ai ? 'status-running' : 'status-stopped']">
                {{ moduleHealth.ai ? '运行中' : '未运行' }}
              </span>
            </div>
            <p class="module-desc">AI服务封装、多模型支持、对话管理</p>
          </div>
          <div class="module-card">
            <div class="module-header">
              <span class="module-name">文件模块</span>
              <span :class="['module-status', moduleHealth.file ? 'status-running' : 'status-stopped']">
                {{ moduleHealth.file ? '运行中' : '未运行' }}
              </span>
            </div>
            <p class="module-desc">文件上传、存储管理、访问控制</p>
          </div>
          <div class="module-card">
            <div class="module-header">
              <span class="module-name">提示词模板模块</span>
              <span :class="['module-status', moduleHealth.prompt ? 'status-running' : 'status-stopped']">
                {{ moduleHealth.prompt ? '运行中' : '未运行' }}
              </span>
            </div>
            <p class="module-desc">模板管理、智能匹配、动态渲染</p>
          </div>
          <div class="module-card">
            <div class="module-header">
              <span class="module-name">社交模块</span>
              <span :class="['module-status', moduleHealth.social ? 'status-running' : 'status-stopped']">
                {{ moduleHealth.social ? '运行中' : '未运行' }}
              </span>
            </div>
            <p class="module-desc">点赞、收藏、评论、分享、热门排行</p>
          </div>
        </div>
      </section>

      <section class="section">
        <h2 class="section-title">开源信息</h2>
        <div class="opensource-info">
          <div class="info-item">
            <span class="info-label">项目地址</span>
            <a href="https://github.com/Moo-Jie/RichCodeWeaver" target="_blank" class="info-link">
              https://github.com/Moo-Jie/RichCodeWeaver
            </a>
          </div>
          <div class="info-item">
            <span class="info-label">开源协议</span>
            <span class="info-value">MIT License</span>
          </div>
          <div class="info-item">
            <span class="info-label">当前版本</span>
            <span class="info-value">V1.0.0</span>
          </div>
        </div>
      </section>

      <section class="section">
        <h2 class="section-title">关于作者</h2>
        <div class="author-card">
          <div class="author-avatar">M</div>
          <div class="author-info">
            <h3 class="author-name">MO-JIE TEAM</h3>
            <div class="author-links">
              <a href="https://github.com/Moo-Jie" target="_blank" class="author-link">
                GitHub: @Moo-Jie
              </a>
              <a href="mailto:NotProvidedTemporarily@xxx.com" class="author-link">
                Email: NotProvidedTemporarily@xxx.com
              </a>
            </div>
          </div>
        </div>
      </section>

      <section class="section">
        <h2 class="section-title">致谢</h2>
        <p class="section-text">
          感谢以下开源项目为本项目提供的支持：Spring Boot、Vue.js、LangChain4j、LangGraph4j、MyBatis Flex、Ant Design Vue 等。
          特别感谢所有为开源社区做出贡献的开发者们。
        </p>
      </section>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { onMounted, reactive } from 'vue'
import { checkAllModulesHealth } from '@/api/healthController'

const moduleHealth = reactive({
  user: false,
  generator: false,
  ai: false,
  file: false,
  prompt: false,
  social: false
})

const loadModuleHealth = async () => {
  try {
    const healthStatus = await checkAllModulesHealth()
    Object.assign(moduleHealth, healthStatus)
  } catch (error) {
    console.error('Failed to load module health status:', error)
  }
}

onMounted(() => {
  loadModuleHealth()
})
</script>

<style scoped>
.about-page {
  padding: 32px;
  max-width: 960px;
  margin: 0 auto;
  width: 100%;
}

.page-header {
  text-align: center;
  margin-bottom: 40px;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0 0 8px;
}

.page-desc {
  font-size: 14px;
  color: #999;
  margin: 0;
}

.content-wrap {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.section {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 14px;
  padding: 24px;
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0 0 16px;
}

.section-text {
  font-size: 14px;
  color: #666;
  line-height: 1.7;
  margin: 0;
}

.features-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 16px;
}

.feature-card {
  background: #fafafa;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  padding: 20px;
  text-align: center;
  transition: all 0.2s ease;
}

.feature-card:hover {
  border-color: #e5e5e5;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.05);
}

.feature-number {
  font-size: 14px;
  font-weight: 700;
  color: #999;
  margin-bottom: 12px;
  font-family: 'Courier New', monospace;
}

.feature-title {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0 0 8px;
}

.feature-desc {
  font-size: 13px;
  color: #999;
  line-height: 1.5;
  margin: 0;
}

.tech-stack {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.tech-category {
  background: #fafafa;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  padding: 16px;
}

.tech-category-title {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0 0 12px;
}

.tech-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tech-tag {
  display: inline-block;
  padding: 4px 12px;
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 16px;
  font-size: 12px;
  color: #666;
  transition: all 0.2s ease;
}

.tech-tag:hover {
  border-color: #1a1a1a;
  color: #1a1a1a;
}

.modules-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 16px;
}

.module-card {
  background: #fafafa;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  padding: 16px;
  transition: all 0.2s ease;
}

.module-card:hover {
  border-color: #e5e5e5;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
}

.module-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.module-name {
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
}

.module-status {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 4px;
  font-weight: 500;
  transition: all 0.2s ease;
}

.status-running {
  color: #52c41a;
  background: #f6ffed;
  border: 1px solid #b7eb8f;
}

.status-stopped {
  color: #ff4d4f;
  background: #fff1f0;
  border: 1px solid #ffccc7;
}

.module-desc {
  font-size: 13px;
  color: #999;
  line-height: 1.5;
  margin: 0;
}

.opensource-info {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: #fafafa;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
}

.info-label {
  font-size: 13px;
  font-weight: 600;
  color: #1a1a1a;
  min-width: 80px;
}

.info-link {
  font-size: 13px;
  color: #666;
  text-decoration: none;
  word-break: break-all;
  transition: color 0.15s;
}

.info-link:hover {
  color: #1a1a1a;
}

.info-value {
  font-size: 13px;
  color: #666;
}

.author-card {
  display: flex;
  align-items: center;
  gap: 20px;
  background: #fafafa;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  padding: 20px;
}

.author-avatar {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: #1a1a1a;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  font-weight: 700;
  flex-shrink: 0;
}

.author-info {
  flex: 1;
  min-width: 0;
}

.author-name {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0 0 4px;
}

.author-role {
  font-size: 13px;
  color: #999;
  margin: 0 0 8px;
}

.author-links {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.author-link {
  font-size: 13px;
  color: #666;
  text-decoration: none;
  transition: color 0.15s;
}

.author-link:hover {
  color: #1a1a1a;
}

@media (max-width: 768px) {
  .about-page {
    padding: 20px;
  }

  .features-grid {
    grid-template-columns: 1fr;
  }

  .modules-grid {
    grid-template-columns: 1fr;
  }

  .author-card {
    flex-direction: column;
    text-align: center;
  }

  .author-links {
    align-items: center;
  }
}
</style>
