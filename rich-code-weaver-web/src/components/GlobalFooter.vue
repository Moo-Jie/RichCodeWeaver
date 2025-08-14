<template>
  <a-layout-footer class="footer">
    <div class="footer-content">
      <p class="copyright">
        <a href="https://github.com/Moo-Jie"
           target="_blank"
           rel="noopener noreferrer"
           class="author-link">
          作者：莫桀
        </a>
      </p>
    </div>
  </a-layout-footer>
</template>

<script setup lang="ts"></script>

<style scoped>
.footer {
  padding: 20px;
  margin-top: auto;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(10px);
  border-top: 1px solid rgba(102, 126, 234, 0.1);
  text-align: center;
}

.copyright {
  margin: 0;
  color: #666;
  font-size: 0.875rem;
  transition: color 0.3s;
}

.author-link:hover {
  color: #1890ff;
}
</style>
