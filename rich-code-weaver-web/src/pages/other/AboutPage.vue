<template>
  <div class="about-page">
    <!-- 公司介绍部分 -->
    <section class="intro-section">
      <h1 class="hero-title">关于 RichCodeWeaver</h1>
      <p class="hero-subtitle">将创意灵感转化为数字现实的创新平台</p>

      <div v-intersect class="mission-card fade-in-up">
        <div class="mission-content">
          <h2>我的愿想</h2>
          <p>通过AI技术降低软件开发门槛，让每个人都能轻松将创意转化为功能完备的数字产品，赋能开发者、设计师和创业者实现自己的
            IDEA。</p>
        </div>
      </div>
    </section>

    <!-- 核心价值部分 -->
    <section class="values-section">
      <div v-intersect class="section-header fade-in-up">
        <h2>一些特点</h2>
        <p>基于 AI，提供智能应用生成服务</p>
      </div>

      <div class="values-grid">
        <div v-intersect class="value-card fade-in-up">
          <div class="value-icon">🚀</div>
          <h3>创意加速</h3>
          <p>将创意实现时间从数周缩短到几分钟，让灵感不再等待</p>
        </div>

        <div v-intersect class="value-card fade-in-up">
          <div class="value-icon">💬</div>
          <h3>智能生成</h3>
          <p>基于最先进的AI模型，理解自然语言需求生成高质量代码</p>
        </div>

        <div v-intersect class="value-card fade-in-up">
          <div class="value-icon">🔄</div>
          <h3>无缝迭代</h3>
          <p>支持实时编辑和持续优化，让创作过程流畅自然</p>
        </div>
      </div>
    </section>

    <!-- 架构设计部分 -->
    <section class="tech-section">
      <div v-intersect class="section-header fade-in-up">
        <h2>架构设计</h2>
        <p>融合前沿AI技术与工程实践，打造强大创作引擎</p>
      </div>

      <div v-intersect class="tech-diagram fade-in-up">
        <div class="tech-layer">
          <div class="layer-icon">🧠</div>
          <h3>自然语言理解</h3>
          <p>深度解析用户需求，提取关键创作要素</p>
        </div>

        <div class="tech-arrow">↓</div>

        <div class="tech-layer">
          <div class="layer-icon">🧩</div>
          <h3>组件化设计</h3>
          <p>智能匹配最佳UI组件与交互模式</p>
        </div>

        <div class="tech-arrow">↓</div>

        <div class="tech-layer">
          <div class="layer-icon">⚙️</div>
          <h3>代码生成引擎</h3>
          <p>生成高质量、可维护的项目工程化代码</p>
        </div>

        <div class="tech-arrow">↓</div>

        <div class="tech-layer">
          <div class="layer-icon">🚀</div>
          <h3>一键部署</h3>
          <p>自动化测试与云端部署流程</p>
        </div>
      </div>
    </section>

    <!-- 团队介绍部分 -->
    <section class="team-section">
      <div v-intersect class="section-header fade-in-up">
        <h2>关于作者</h2>
      </div>

      <div class="team-grid">
        <div v-intersect class="team-card fade-in-up">
          <div class="avatar"
               style="background: linear-gradient(135deg, #a8e6cf 0%, #dcedc1 100%);">Z
          </div>
          <h3>MO-JIE</h3>
          <p>Java 后端开发</p>
          <p class="bio">
            <a href="https://github.com/Moo-Jie" target="_blank">GitHub :
              https://github.com/Moo-Jie</a>
          </p>
        </div>

        <!--        <div class="team-card fade-in-up" v-intersect>-->
        <!--          <div class="avatar" style="background: linear-gradient(135deg, #ff9a9e 0%, #fad0c4 100%);">L</div>-->
        <!--          <h3>李四</h3>-->
        <!--          <p class="role">产品设计总监</p>-->
        <!--          <p class="bio">前腾讯高级产品设计师，用户体验专家</p>-->
        <!--        </div>-->

        <!--        <div class="team-card fade-in-up" v-intersect>-->
        <!--          <div class="avatar" style="background: linear-gradient(135deg, #00c4ff 0%, #9face6 100%);">W</div>-->
        <!--          <h3>王五</h3>-->
        <!--          <p class="role">工程负责人</p>-->
        <!--          <p class="bio">前阿里云高级工程师，全栈开发专家</p>-->
        <!--        </div>-->
      </div>
    </section>
  </div>
</template>

<script lang="ts" setup>

// 苹果风格的交互动效指令
const vIntersect = {
  mounted(el: HTMLElement) {
    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          el.classList.add('is-visible')
          observer.disconnect()
        }
      },
      { threshold: 0.15 }
    )
    observer.observe(el)
  }
}
</script>

<style scoped>
/* 基础样式 */
.about-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 40px 20px;
  font-family: 'Nunito', 'Comic Neue', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  min-height: 100vh;
  position: relative;
  z-index: 2;
}

/* 背景效果 */
.about-page::before {
  content: '';
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, rgb(255, 248, 206) 0%, rgb(147, 203, 255) 100%);
  z-index: -1;
  pointer-events: none;
}

/* 公司介绍部分 */
.intro-section {
  text-align: center;
  margin-bottom: 60px;
  padding-top: 40px;
}

.hero-title {
  font-family: 'Comic Neue', cursive;
  font-size: 3.5rem;
  color: #2c3e50;
  margin-bottom: 20px;
  font-weight: 700;
  letter-spacing: -1px;
}

.hero-subtitle {
  font-size: 1.5rem;
  color: #7f8c8d;
  margin-bottom: 60px;
  font-weight: 400;
  font-family: 'Comic Neue', cursive;
}

.mission-card {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #ffffff;
  border-radius: 32px;
  padding: 50px;
  box-shadow: 0 12px 48px 0 rgba(0, 113, 227, 0.10);
  max-width: 800px;
  margin: 0 auto;
  transition: all 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
  border: 1px solid rgba(0, 0, 0, 0.05);
}

.mission-card:hover {
  box-shadow: 0 24px 56px 0 rgba(0, 113, 227, 0.18);
  transform: translateY(-5px);
}

.mission-content {
  text-align: center;
}

.mission-content h2 {
  font-size: 2.2rem;
  margin-bottom: 25px;
  font-family: 'Comic Neue', cursive;
  color: #2c3e50;
}

.mission-content p {
  font-size: 1.2rem;
  line-height: 1.8;
  color: #5c4a48;
  max-width: 700px;
}

/* 通用部分标题 */
.section-header {
  text-align: center;
  margin-bottom: 60px;
}

.section-header h2 {
  font-size: 2.5rem;
  color: #2c3e50;
  margin-bottom: 15px;
  font-family: 'Comic Neue', cursive;
}

.section-header p {
  font-size: 1.2rem;
  color: #7f8c8d;
  max-width: 600px;
  margin: 0 auto;
}

/* 核心价值部分 */
.values-section {
  margin-bottom: 80px;
}

.values-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 30px;
  max-width: 1200px;
  margin: 0 auto;
}

.value-card {
  background: #ffffff;
  border-radius: 24px;
  padding: 35px 30px;
  text-align: center;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;
  border: 1px solid rgba(0, 0, 0, 0.05);
}

.value-card:hover {
  transform: translateY(-10px);
  box-shadow: 0 16px 32px rgba(0, 0, 0, 0.12);
}

.value-icon {
  font-size: 3.5rem;
  margin-bottom: 20px;
}

.value-card h3 {
  font-size: 1.6rem;
  margin-bottom: 15px;
  color: #2c3e50;
  font-family: 'Comic Neue', cursive;
}

.value-card p {
  font-size: 1.1rem;
  color: #5c4a48;
  line-height: 1.6;
}

/* 技术架构部分 */
.tech-section {
  margin-bottom: 80px;
}

.tech-diagram {
  display: flex;
  flex-direction: column;
  align-items: center;
  max-width: 800px;
  margin: 0 auto;
}

.tech-layer {
  background: #ffffff;
  border-radius: 24px;
  padding: 30px;
  text-align: center;
  width: 100%;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
  margin-bottom: 20px;
  transition: all 0.3s ease;
  border: 1px solid rgba(0, 0, 0, 0.05);
}

.tech-layer:hover {
  transform: scale(1.03);
}

.layer-icon {
  font-size: 3rem;
  margin-bottom: 15px;
}

.tech-layer h3 {
  font-size: 1.6rem;
  margin-bottom: 10px;
  color: #2c3e50;
  font-family: 'Comic Neue', cursive;
}

.tech-layer p {
  font-size: 1.1rem;
  color: #5c4a48;
}

.tech-arrow {
  font-size: 2.5rem;
  margin: 10px 0;
  color: #5c4a48;
  opacity: 0.7;
}

/* 团队介绍部分 */
.team-section {
  margin-bottom: 80px;
}

.team-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 30px;
  max-width: 1000px;
  margin: 0 auto;
}

.team-card {
  background: #ffffff;
  border-radius: 24px;
  padding: 35px 30px;
  text-align: center;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;
  border: 1px solid rgba(0, 0, 0, 0.05);
}

.team-card:hover {
  transform: translateY(-10px);
  box-shadow: 0 16px 32px rgba(0, 0, 0, 0.12);
}

.avatar {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  margin: 0 auto 25px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 3.5rem;
  font-weight: bold;
  color: white;
}

.team-card h3 {
  font-size: 1.8rem;
  margin-bottom: 10px;
  color: #2c3e50;
  font-family: 'Comic Neue', cursive;
}

.role {
  font-size: 1.2rem;
  color: #1890ff;
  margin-bottom: 15px;
  font-weight: 600;
}

.bio {
  font-size: 1.1rem;
  color: #5c4a48;
  line-height: 1.6;
}

/* 联系部分 */
.contact-section {
  margin-bottom: 60px;
}

.contact-card {
  background: #ffffff;
  border-radius: 32px;
  padding: 60px 40px;
  text-align: center;
  box-shadow: 0 12px 48px rgba(0, 113, 227, 0.15);
  max-width: 800px;
  margin: 0 auto;
  transition: all 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
  border: 1px solid rgba(0, 0, 0, 0.05);
}

.contact-card:hover {
  box-shadow: 0 24px 56px rgba(0, 113, 227, 0.2);
  transform: translateY(-5px);
}

.contact-card h2 {
  font-size: 2.5rem;
  margin-bottom: 20px;
  color: #2c3e50;
  font-family: 'Comic Neue', cursive;
}

.contact-card p {
  font-size: 1.2rem;
  color: #5c4a48;
  margin-bottom: 40px;
  max-width: 600px;
  margin-left: auto;
  margin-right: auto;
}

.contact-actions {
  display: flex;
  justify-content: center;
  gap: 20px;
  flex-wrap: wrap;
}

.contact-button {
  background: linear-gradient(135deg, #00c4ff 0%, #9face6 100%);
  border: none;
  border-radius: 16px;
  padding: 15px 30px;
  font-size: 1.1rem;
  font-weight: 600;
  color: white;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 6px 16px rgba(116, 235, 213, 0.3);
}

.contact-button:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 20px rgba(116, 235, 213, 0.4);
}

/* 动画效果 */
.fade-in-up {
  opacity: 0;
  transform: translateY(40px) scale(0.98);
  transition: opacity 0.7s cubic-bezier(0.4, 0, 0.2, 1),
  transform 0.7s cubic-bezier(0.4, 0, 0.2, 1);
}

.fade-in-up.is-visible {
  opacity: 1;
  transform: translateY(0) scale(1);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .hero-title {
    font-size: 2.5rem;
  }

  .hero-subtitle {
    font-size: 1.2rem;
  }

  .mission-card {
    padding: 30px 20px;
  }

  .mission-content h2 {
    font-size: 1.8rem;
  }

  .mission-content p {
    font-size: 1rem;
  }

  .section-header h2 {
    font-size: 2rem;
  }

  .section-header p {
    font-size: 1rem;
  }

  .value-card, .tech-layer, .team-card {
    padding: 25px 20px;
  }

  .contact-card {
    padding: 40px 20px;
  }

  .contact-button {
    width: 100%;
    max-width: 280px;
  }
}

@media (max-width: 480px) {
  .values-grid, .team-grid {
    grid-template-columns: 1fr;
  }

  .tech-arrow {
    transform: rotate(90deg);
    margin: 15px 0;
  }
}
</style>
