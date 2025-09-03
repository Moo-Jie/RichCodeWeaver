<template>
  <a-layout-footer class="footer">
    <div class="footer-container">
      <!-- 社交链接 -->
      <div class="social-links">
        <a href="https://github.com/Moo-Jie"
           target="_blank"
           class="social-link">
          <GithubOutlined />
        </a>
        <a href="#" class="social-link">
          <WeiboOutlined />
        </a>
        <a href="#" class="social-link">
          <WechatOutlined />
        </a>
      </div>

      <!-- 导航链接 -->
      <nav class="footer-nav">
        <a href="/other/about" class="nav-link">关于我们</a>
        <a href="/other/docs" class="nav-link">文档中心</a>
        <a href="/other/privacy" class="nav-link">隐私政策</a>
        <a href="/other/terms" class="nav-link">服务条款</a>
        <a href="/other/contact" class="nav-link">联系我们</a>
      </nav>

      <!-- 版权声明 -->
      <div class="copyright-container">
        <p class="copyright">
          © {{ currentYear }} RichCodeWeaver - 织码睿奇 | 只需一句话，让创意触手可及 |
          <a href="https://github.com/Moo-Jie"
             target="_blank"
             rel="noopener noreferrer"
             class="author-link">
            莫桀
          </a>
        </p>
      </div>
    </div>
  </a-layout-footer>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { GithubOutlined, WeiboOutlined, WechatOutlined } from '@ant-design/icons-vue';

const currentYear = computed(() => new Date().getFullYear());
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Comic+Neue:wght@300;400;700&family=Nunito:wght@300;400;600;700&display=swap');

.footer {
  padding: 40px 20px 30px;
  margin-top: auto;
  background: linear-gradient(90deg, #d1e4e0, #a5cff2, #92c8f9);
  backdrop-filter: blur(12px);
  border-top: 1px solid rgb(189, 222, 255);
  box-shadow: 0 -4px 20px rgb(189, 222, 255);
  position: relative;
  overflow: hidden;
  font-family: 'Nunito', 'Comic Neue', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

.footer::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 3px;
}

.footer-container {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 24px;
}

.social-links {
  display: flex;
  gap: 20px;
  margin-bottom: 10px;
}

.social-link {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  border-radius: 50%;
  background: linear-gradient(135deg, #c8e5ff 0%, #59a7ff 100%);
  color: white;
  font-size: 18px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 4px 10px rgb(204, 211, 255);
}

.social-link:hover {
  transform: translateY(-5px) scale(1.1);
  box-shadow: 0 8px 15px rgba(255, 255, 255, 0.4);
  background: linear-gradient(135deg, #59a7ff 0%, #c8e5ff 100%);
}

.footer-nav {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 32px;
  margin-bottom: 15px;
}

.nav-link {
  color: #666;
  font-size: 0.95rem;
  text-decoration: none;
  transition: all 0.3s ease;
  position: relative;
  padding: 6px 0;
  font-weight: 500;
  font-family: 'Comic Neue', cursive;
}

.nav-link:hover {
  color: #c8e5ff;
}

.nav-link::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  width: 0;
  height: 2px;
  background: linear-gradient(90deg, #2998ff, #bddeff);
  transition: width 0.3s ease;
  border-radius: 2px;
}

.nav-link:hover::after {
  width: 100%;
}

.copyright-container {
  margin-top: 10px;
  text-align: center;
}

.copyright {
  margin: 0;
  color: #888;
  font-size: 0.9rem;
  font-family: 'Comic Neue', cursive;
}

.author-link {
  color: #1890ff;
  text-decoration: none;
  transition: all 0.3s;
  margin: 0 4px;
  font-weight: 600;
  position: relative;
}

.author-link::after {
  content: '';
  position: absolute;
  bottom: -2px;
  left: 0;
  width: 0;
  height: 1px;
  background: #1890ff;
  transition: width 0.3s ease;
}

.author-link:hover {
  color: #1890ff;
}

.author-link:hover::after {
  width: 100%;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .footer {
    padding: 30px 15px 25px;
    border-radius: 25px 25px 0 0;
  }

  .footer-nav {
    gap: 20px;
  }

  .social-links {
    gap: 16px;
  }

  .social-link {
    width: 38px;
    height: 38px;
    font-size: 16px;
  }
}

@media (max-width: 480px) {
  .footer {
    padding: 25px 12px 20px;
    border-radius: 20px 20px 0 0;
  }

  .footer-nav {
    gap: 16px;
    font-size: 0.85rem;
  }

  .social-links {
    gap: 14px;
  }

  .social-link {
    width: 36px;
    height: 36px;
    font-size: 15px;
  }

  .copyright {
    font-size: 0.85rem;
  }
}
</style>
