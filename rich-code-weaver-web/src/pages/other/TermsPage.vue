<template>
  <div class="terms-container">
    <!-- 顶部导航 -->
    <div class="header-nav">
      <router-link class="nav-link" to="/">
        <arrow-left-outlined />
        返回首页
      </router-link>
    </div>

    <!-- 内容区域 -->
    <div class="content-wrapper">
      <h1 class="section-title">服务条款</h1>
      <p class="update-date">最后更新日期：2025年8月18日</p>

      <div class="terms-card">
        <section class="terms-section">
          <h2>接受条款</h2>
          <p>通过访问或使用 RichCodeWeaver（以下简称"本平台"）提供的服务，即表示您同意遵守本服务条款（以下简称"条款"）。这些条款构成您与
            RichCodeWeaver 之间的法律协议。</p>
        </section>

        <section class="terms-section">
          <h2>服务描述</h2>
          <div class="service-cards">
            <div class="service-card">
              <div class="service-icon">
                <code-outlined />
              </div>
              <p>基于用户描述生成应用原型</p>
            </div>
            <div class="service-card">
              <div class="service-icon">
                <edit-outlined />
              </div>
              <p>提供应用编辑和管理功能</p>
            </div>
            <div class="service-card">
              <div class="service-icon">
                <cloud-server-outlined />
              </div>
              <p>存储用户生成的内容</p>
            </div>
            <div class="service-card">
              <div class="service-icon">
                <share-alt-outlined />
              </div>
              <p>提供应用部署和分享功能</p>
            </div>
          </div>
        </section>

        <section class="terms-section">
          <h2>用户义务</h2>
          <div class="obligations-grid">
            <div class="obligation-card">
              <div class="obligation-badge">!</div>
              <p>不得违反任何适用法律法规</p>
            </div>
            <div class="obligation-card">
              <div class="obligation-badge">!</div>
              <p>不得侵犯他人知识产权</p>
            </div>
            <div class="obligation-card">
              <div class="obligation-badge">!</div>
              <p>不得传播恶意软件或干扰服务运行</p>
            </div>
            <div class="obligation-card">
              <div class="obligation-badge">!</div>
              <p>不得从事欺诈活动或虚假陈述</p>
            </div>
          </div>
        </section>

        <section class="terms-section">
          <h2>知识产权</h2>
          <div class="ip-grid">
            <div class="ip-card">
              <h3>平台知识产权</h3>
              <p>RichCodeWeaver
                保留所有平台内容、功能、源代码、设计元素和技术的所有权。未经明确书面许可，不得复制、修改或创建衍生作品。</p>
            </div>
            <div class="ip-card">
              <h3>用户生成内容</h3>
              <p>您保留在平台上创建内容的所有权。通过上传内容，您授予 RichCodeWeaver
                全球性、非独占、免版税的许可，用于运营、改进和推广服务。</p>
            </div>
          </div>
        </section>

        <section class="terms-section">
          <h2>免责声明</h2>
          <div class="disclaimer-card">
            <div class="disclaimer-item">
              <div class="disclaimer-icon">
                <warning-outlined />
              </div>
              <p>本服务按"原样"提供，不作任何明示或暗示的保证</p>
            </div>
            <div class="disclaimer-item">
              <div class="disclaimer-icon">
                <warning-outlined />
              </div>
              <p>生成内容可能存在不准确或缺陷，使用风险由您自行承担</p>
            </div>
            <div class="disclaimer-item">
              <div class="disclaimer-icon">
                <warning-outlined />
              </div>
              <p>服务可能中断或无法使用，不保证满足所有用户需求</p>
            </div>
          </div>
        </section>

        <section class="terms-section">
          <h2>联系我们</h2>
          <div class="contact-card">
            <div class="contact-info">
              <div class="contact-icon">
                <customer-service-outlined />
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
  CloudServerOutlined,
  CodeOutlined,
  CustomerServiceOutlined,
  EditOutlined,
  ShareAltOutlined,
  WarningOutlined
} from '@ant-design/icons-vue'
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Playfair+Display:wght@400;700&family=Source+Sans+Pro:wght@300;400;600&display=swap');

.terms-container {
  background: white;
  min-height: 100vh;
  font-family: 'Source Sans Pro', sans-serif;
  color: #3a3a44;
  padding-bottom: 60px;
}

.header-nav {
  max-width: 1200px;
  margin: 0 auto;
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
  max-width: 800px;
  margin: 0 auto;
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

.terms-card {
  background: white;
  border-radius: 16px;
  padding: 40px;
  box-shadow: 0 15px 35px rgba(0, 0, 0, 0.08);
  line-height: 1.8;
}

.terms-section {
  margin-bottom: 40px;
}

.terms-section h2 {
  font-family: 'Playfair Display', serif;
  color: #1d3660;
  border-bottom: 2px solid rgba(198, 160, 138, 0.3);
  padding-bottom: 10px;
  margin-bottom: 25px;
  font-size: 1.8rem;
}

.terms-section p {
  margin-bottom: 15px;
  font-size: 1.1rem;
  color: #555;
  line-height: 1.8;
}

.service-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 20px;
  margin-top: 20px;
}

.service-card {
  background: #f9fafb;
  border-radius: 12px;
  padding: 20px;
  text-align: center;
  border: 1px solid #f0f0f0;
  transition: all 0.3s;
}

.service-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 10px 20px rgba(0, 0, 0, 0.05);
}

.service-icon {
  width: 50px;
  height: 50px;
  background: #e6f7ff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 15px;
  font-size: 24px;
  color: #1890ff;
}

.obligations-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 15px;
  margin-top: 20px;
}

.obligation-card {
  display: flex;
  align-items: center;
  gap: 15px;
  background: #fffbe6;
  border-radius: 8px;
  padding: 15px;
  border: 1px solid #ffe58f;
}

.obligation-badge {
  width: 30px;
  height: 30px;
  background: #faad14;
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  flex-shrink: 0;
  font-size: 18px;
}

.ip-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 20px;
  margin-top: 20px;
}

@media (min-width: 768px) {
  .ip-grid {
    grid-template-columns: 1fr 1fr;
  }
}

.ip-card {
  background: #f9fafb;
  border-radius: 12px;
  padding: 20px;
  border: 1px solid #f0f0f0;
}

.ip-card h3 {
  font-size: 1.3rem;
  color: #1d3660;
  margin-bottom: 15px;
  font-family: 'Playfair Display', serif;
}

.disclaimer-card {
  background: #fff2f0;
  border-radius: 12px;
  padding: 20px;
  margin-top: 20px;
  border: 1px solid #ffccc7;
}

.disclaimer-item {
  display: flex;
  gap: 15px;
  margin-bottom: 15px;
  align-items: flex-start;
}

.disclaimer-item:last-child {
  margin-bottom: 0;
}

.disclaimer-icon {
  width: 30px;
  height: 30px;
  background: #ff4d4f;
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: 16px;
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

  .terms-card {
    padding: 25px;
  }

  .terms-section h2 {
    font-size: 1.6rem;
  }
}
</style>
