<template>
  <div class="about-page">
    <div class="page-header">
      <h1 class="page-title">关于 RichCodeWeaver</h1>
      <p class="page-desc">将创意灵感转化为数字现实的创新平台</p>
    </div>

    <div class="content-wrap">
      <section class="section">
        <h2 class="section-title">我的愿想</h2>
        <p class="section-text">通过AI技术降低软件开发门槛，让每个人都能轻松将创意转化为功能完备的数字产品，赋能开发者、设计师和创业者实现自己的IDEA。</p>
      </section>

      <section class="section">
        <h2 class="section-title">核心特点</h2>
        <div class="features-grid">
          <div class="feature-card">
            <div class="feature-icon">🚀</div>
            <h3 class="feature-title">创意加速</h3>
            <p class="feature-desc">将创意实现时间从数周缩短到几分钟，让灵感不再等待</p>
          </div>
          <div class="feature-card">
            <div class="feature-icon">💬</div>
            <h3 class="feature-title">智能生成</h3>
            <p class="feature-desc">基于最先进的AI模型，理解自然语言需求生成高质量代码</p>
          </div>
          <div class="feature-card">
            <div class="feature-icon">🔄</div>
            <h3 class="feature-title">无缝迭代</h3>
            <p class="feature-desc">支持实时编辑和持续优化，让创作过程流畅自然</p>
          </div>
        </div>
      </section>

      <section class="section">
        <h2 class="section-title">架构设计</h2>
        <div class="tech-flow">
          <div class="tech-step">
            <div class="tech-icon">🧠</div>
            <h3 class="tech-name">自然语言理解</h3>
            <p class="tech-desc">深度解析用户需求，提取关键创作要素</p>
          </div>
          <div class="tech-step">
            <div class="tech-icon">🧩</div>
            <h3 class="tech-name">组件化设计</h3>
            <p class="tech-desc">智能匹配最佳UI组件与交互模式</p>
          </div>
          <div class="tech-step">
            <div class="tech-icon">⚙️</div>
            <h3 class="tech-name">代码生成引擎</h3>
            <p class="tech-desc">生成高质量、可维护的项目工程化代码</p>
          </div>
          <div class="tech-step">
            <div class="tech-icon">🚀</div>
            <h3 class="tech-name">一键部署</h3>
            <p class="tech-desc">自动化测试与云端部署流程</p>
          </div>
        </div>
      </section>

      <section class="section">
        <h2 class="section-title">关于作者</h2>
        <div class="author-card">
          <div class="author-avatar">M</div>
          <div class="author-info">
            <h3 class="author-name">MO-JIE</h3>
            <p class="author-role">Java 后端开发</p>
            <a href="https://github.com/Moo-Jie" target="_blank" class="author-link">GitHub: https://github.com/Moo-Jie</a>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script lang="ts" setup>
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
  gap: 32px;
}

.section {
  background: #fafafa;
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
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
}

.feature-card {
  background: #fff;
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

.feature-icon {
  font-size: 32px;
  margin-bottom: 12px;
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

.tech-flow {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.tech-step {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  padding: 16px 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  transition: all 0.2s ease;
}

.tech-step:hover {
  border-color: #e5e5e5;
  background: #fafafa;
}

.tech-icon {
  font-size: 28px;
  flex-shrink: 0;
}

.tech-name {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0 0 4px;
}

.tech-desc {
  font-size: 13px;
  color: #999;
  margin: 0;
}

.author-card {
  display: flex;
  align-items: center;
  gap: 20px;
  background: #fff;
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

.author-link {
  font-size: 13px;
  color: #666;
  text-decoration: none;
  word-break: break-all;
  transition: color 0.15s;
}

.author-link:hover {
  color: #1a1a1a;
}

@media (max-width: 768px) {
  .features-grid {
    grid-template-columns: 1fr;
  }
  
  .tech-step {
    flex-direction: column;
    text-align: center;
  }
  
  .author-card {
    flex-direction: column;
    text-align: center;
  }
}
</style>
