<template>
  <div class="privacy-container">
    <!-- 顶部导航 -->
    <div class="header-nav">
      <router-link class="nav-link" to="/">
        <arrow-left-outlined/>
        返回首页
      </router-link>
    </div>

    <!-- 内容区域 -->
    <div class="content-wrapper">
      <h1 class="section-title">隐私政策</h1>
      <p class="update-date">最后更新日期：2025年8月18日</p>

      <div class="policy-card">
        <section class="policy-section">
          <h2>引言</h2>
          <p>欢迎使用
            RichCodeWeaver（以下简称"本平台"）。我们深知个人信息对您的重要性，并致力于保护您的隐私权。本隐私政策旨在说明我们如何收集、使用、存储和共享您的个人信息，以及您如何行使您的隐私权利。</p>
        </section>

        <section class="policy-section">
          <h2>我们收集的信息</h2>
          <div class="info-grid">
            <div class="info-card">
              <h3>您直接提供的信息</h3>
              <ul>
                <li><strong>账户信息</strong>：用户名、电子邮箱地址和密码</li>
                <li><strong>个人资料</strong>：个人头像、昵称和联系信息</li>
                <li><strong>应用内容</strong>：创建的应用描述和配置信息</li>
              </ul>
            </div>
            <div class="info-card">
              <h3>自动收集的信息</h3>
              <ul>
                <li><strong>设备信息</strong>：IP地址、浏览器类型和设备标识符</li>
                <li><strong>使用数据</strong>：访问时间、浏览页面和功能使用情况</li>
                <li><strong>技术数据</strong>：Cookie和类似技术信息</li>
              </ul>
            </div>
          </div>
        </section>

        <section class="policy-section">
          <h2>我们如何使用信息</h2>
          <div class="usage-cards">
            <div class="usage-card">
              <div class="usage-icon">
                <rocket-outlined/>
              </div>
              <p>提供、维护和改进我们的服务</p>
            </div>
            <div class="usage-card">
              <div class="usage-icon">
                <experiment-outlined/>
              </div>
              <p>开发新功能和服务</p>
            </div>
            <div class="usage-card">
              <div class="usage-icon">
                <security-scan-outlined/>
              </div>
              <p>保护平台和用户的安全</p>
            </div>
            <div class="usage-card">
              <div class="usage-icon">
                <message-outlined/>
              </div>
              <p>与您沟通服务更新和安全通知</p>
            </div>
          </div>
        </section>

        <section class="policy-section">
          <h2>您的权利</h2>
          <div class="rights-grid">
            <div class="right-card">
              <h3>访问权</h3>
              <p>您有权访问我们持有的关于您的个人信息</p>
            </div>
            <div class="right-card">
              <h3>更正权</h3>
              <p>您可以更正不准确或不完整的个人信息</p>
            </div>
            <div class="right-card">
              <h3>删除权</h3>
              <p>您可以在某些情况下要求删除您的个人信息</p>
            </div>
            <div class="right-card">
              <h3>限制处理权</h3>
              <p>您可以限制我们处理您的个人信息的方式</p>
            </div>
          </div>
        </section>

        <section class="policy-section">
          <h2>联系我们</h2>
          <div class="contact-card">
            <div class="contact-info">
              <div class="contact-icon">
                <mail-outlined/>
              </div>
              <div>
                <h3>邮箱</h3>
                <p>DRC9941@outlook.com</p>
              </div>
            </div>
          </div>
        </section>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import {
  ArrowLeftOutlined,
  ExperimentOutlined,
  MailOutlined,
  MessageOutlined,
  RocketOutlined,
  SecurityScanOutlined
} from '@ant-design/icons-vue'
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Playfair+Display:wght@400;700&family=Source+Sans+Pro:wght@300;400;600&display=swap');

.privacy-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 40px 20px;
  font-family: 'Nunito', 'Comic Neue', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  min-height: 100vh;
  position: relative;
  z-index: 2;
}

/* 背景效果 */
.privacy-container::before {
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

.header-nav {
  padding: 20px 20px 0;
}

.nav-link {
  color: #1890ff;
  font-size: 16px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  border-radius: 4px;
  transition: all 0.3s;
}

.nav-link:hover {
  background: #e6f7ff;
}

.content-wrapper {
  padding: 20px;
}

.section-title {
  text-align: center;
  font-size: 2.5rem;
  color: #1d3660;
  margin-bottom: 15px;
  font-family: 'Playfair Display', serif;
  position: relative;
  padding-bottom: 15px;
}

.section-title::after {
  content: '';
  display: block;
  width: 80px;
  height: 4px;
  background: linear-gradient(to right, #ff9f7f, #7eb8ff);
  margin: 10px auto 0;
  border-radius: 2px;
}

.update-date {
  text-align: center;
  color: #6d6b80;
  margin-bottom: 40px;
  font-size: 1.1rem;
}

.policy-card {
  background: white;
  border-radius: 16px;
  padding: 40px;
  box-shadow: 0 15px 35px rgba(0, 0, 0, 0.08);
  line-height: 1.8;
}

.policy-section {
  margin-bottom: 40px;
}

.policy-section h2 {
  font-family: 'Playfair Display', serif;
  color: #1d3660;
  border-bottom: 2px solid rgba(198, 160, 138, 0.3);
  padding-bottom: 10px;
  margin-bottom: 25px;
  font-size: 1.8rem;
}

.policy-section p {
  margin-bottom: 15px;
  font-size: 1.1rem;
  color: #555;
  line-height: 1.8;
}

.info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin-top: 20px;
}

.info-card {
  background: #f9fafb;
  border-radius: 12px;
  padding: 20px;
  border: 1px solid #f0f0f0;
}

.info-card h3 {
  color: #7a5a48;
  margin-bottom: 15px;
  font-size: 1.3rem;
}

.info-card ul {
  padding-left: 20px;
}

.info-card li {
  margin-bottom: 10px;
  font-size: 1.1rem;
}

.usage-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 20px;
  margin-top: 20px;
}

.usage-card {
  background: #f9fafb;
  border-radius: 12px;
  padding: 20px;
  text-align: center;
  border: 1px solid #f0f0f0;
  transition: all 0.3s;
}

.usage-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 10px 20px rgba(0, 0, 0, 0.05);
}

.usage-icon {
  width: 60px;
  height: 60px;
  background: #e6f7ff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 15px;
  font-size: 28px;
  color: #1890ff;
}

.disclosure-card {
  background: #f9fafb;
  border-radius: 12px;
  padding: 20px;
  margin-top: 20px;
  border: 1px solid #f0f0f0;
}

.disclosure-item {
  display: flex;
  gap: 15px;
  margin-bottom: 20px;
  align-items: flex-start;
}

.disclosure-item:last-child {
  margin-bottom: 0;
}

.disclosure-badge {
  width: 32px;
  height: 32px;
  background: #1890ff;
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  flex-shrink: 0;
}

.disclosure-content h3 {
  font-size: 1.3rem;
  color: #1d3660;
  margin-bottom: 5px;
}

.rights-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 20px;
  margin-top: 20px;
}

.right-card {
  background: #f9fafb;
  border-radius: 12px;
  padding: 20px;
  border: 1px solid #f0f0f0;
}

.right-card h3 {
  font-size: 1.3rem;
  color: #1d3660;
  margin-bottom: 10px;
  font-family: 'Playfair Display', serif;
}

.contact-card {
  background: #f9fafb;
  border-radius: 12px;
  padding: 20px;
  margin-top: 20px;
  border: 1px solid #f0f0f0;
}

.contact-info {
  display: flex;
  align-items: center;
  gap: 15px;
}

.contact-icon {
  width: 50px;
  height: 50px;
  background: #e6f7ff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: #1890ff;
}

@media (max-width: 768px) {
  .content-wrapper {
    padding: 15px;
  }

  .section-title {
    font-size: 2rem;
  }

  .policy-card {
    padding: 25px;
  }

  .info-grid {
    grid-template-columns: 1fr;
  }

  .policy-section h2 {
    font-size: 1.6rem;
  }
}
</style>
