<template>
  <a-layout-footer class="footer">
    <div class="footer-container">
      <div class="social-links">
        <a href="https://github.com/Moo-Jie"
           target="_blank">
          <GithubOutlined style="font-size: 24px;" />
        </a>
      </div>

      <!-- 导航链接 -->
      <nav class="footer-nav">
        <a href="/other/about" class="nav-link">关于我们</a>
        <a href="/other/docs" class="nav-link">文档中心</a>
        <a href="/other/privacy" class="nav-link">隐私政策</a>
        <a href="/other/terms" class="nav-link">服务条款</a>
      </nav>

      <!-- 版权声明 -->
      <div class="copyright-container">
        <p class="copyright">
          © {{ currentYear }} RichCodeWeaver - 织码睿奇 |
          <a href="https://github.com/Moo-Jie"
             target="_blank"
             rel="noopener noreferrer"
             class="author-link">
            莫桀
          </a>
          版权所有
        </p>
      </div>
    </div>
  </a-layout-footer>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { GithubOutlined } from '@ant-design/icons-vue';

const currentYear = computed(() => new Date().getFullYear());
</script>

<style scoped>
.footer {
  padding: 28px 20px;
  margin-top: auto;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(12px);
  border-top: 1px solid rgba(102, 126, 234, 0.1);
  box-shadow: 0 -2px 10px rgba(0, 0, 0, 0.03);
}

.footer-container {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 18px;
}

.social-links {
  display: flex;
  gap: 22px;
  margin-bottom: 10px;
}

.social-links a {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: rgba(102, 126, 234, 0.1);
  transition: all 0.3s ease;
}

.social-links a:hover {
  background: rgba(102, 126, 234, 0.2);
  transform: translateY(-3px);
}

.footer-nav {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 24px;
  margin-bottom: 10px;
}

.nav-link {
  color: #666;
  font-size: 0.875rem;
  text-decoration: none;
  transition: color 0.3s;
  position: relative;
  padding: 4px 0;
}

.nav-link:hover {
  color: #1890ff;
}

.nav-link::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  width: 0;
  height: 1px;
  background: #1890ff;
  transition: width 0.3s ease;
}

.nav-link:hover::after {
  width: 100%;
}

.copyright-container {
  margin-top: 8px;
  text-align: center;
}

.copyright {
  margin: 0;
  color: #777;
  font-size: 0.875rem;
}

.author-link {
  color: #1890ff;
  text-decoration: none;
  transition: color 0.3s;
  margin: 0 4px;
}

.author-link:hover {
  color: #0d76e0;
  text-decoration: underline;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .footer-nav {
    gap: 16px;
  }

  .social-links {
    gap: 20px;
  }
}

@media (max-width: 480px) {
  .footer-nav {
    gap: 12px;
    font-size: 0.8125rem;
  }

  .social-links a {
    width: 32px;
    height: 32px;
  }

  .copyright {
    font-size: 0.8125rem;
  }
}
</style>
