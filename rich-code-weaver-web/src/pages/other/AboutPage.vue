<template>
  <div class="about-page">
    <!-- 公司介绍部分 -->
    <section class="intro-section">
      <h1 class="hero-title">关于 RichCodeWeaver</h1>
      <p class="hero-subtitle">将创意灵感转化为数字现实的创新平台</p>

      <div class="mission-card fade-in-up" v-intersect>
        <div class="mission-content">
          <h2>我们的使命</h2>
          <p>通过AI技术降低软件开发门槛，让每个人都能轻松将创意转化为功能完备的数字产品，赋能开发者、设计师和创业者实现技术梦想。</p>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue';

// 苹果风格的交互动效指令
const vIntersect = {
  mounted(el: HTMLElement) {
    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          el.classList.add('is-visible');
          observer.disconnect();
        }
      },
      { threshold: 0.15 }
    );
    observer.observe(el);
  }
};
</script>

<style scoped>
/* 基础样式 */
.about-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 40px 20px;
  font-family: 'Source Sans Pro', sans-serif;
  min-height: 100vh;
}

/* 公司介绍部分 */
.intro-section {
  text-align: center;
  margin-bottom: 60px;
  padding-top: 40px;
}

.hero-title {
  font-size: 3.5rem;
  color: #5c4a48;
  margin-bottom: 40px;
  font-weight: 400;
}

.hero-subtitle {
  font-size: 1.5rem;
  color: #5c4a48;
  margin-bottom: 40px;
  font-weight: 400;
}

.mission-card {
  display: flex;
  align-items: center;
  background: linear-gradient(135deg, #fff 60%, #f5f6fa 100%);
  border: 1.5px solid #e5eaf0;
  border-radius: 32px;
  padding: 40px;
  box-shadow: 0 12px 48px 0 rgba(0, 113, 227, 0.10);
  max-width: 800px;
  margin: 0 auto;
  transition: all 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.mission-card:hover {
  box-shadow: 0 24px 56px 0 rgba(0, 113, 227, 0.18);
  background: linear-gradient(90deg, #fafdff 60%, #e3f0ff 100%);
  transform: translateY(-5px);
}

.mission-content h2 {
  font-size: 2rem;
  margin-bottom: 15px;
  font-family: 'Playfair Display', serif;
}

.mission-content p {
  font-size: 1.1rem;
  line-height: 1.8;
  color: #5c4a48;
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
    flex-direction: column;
    text-align: center;
    padding: 30px 20px;
  }
}
</style>
