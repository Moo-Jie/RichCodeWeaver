<template>
  <router-view />
</template>

<style>
/* 全局样式优化 */
* {
  box-sizing: border-box;
  margin: 0;
  padding: 0;
}

body {
  margin: 0;
  padding: 0;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Hiragino Sans GB',
  'Microsoft YaHei', 'Helvetica Neue', Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  overflow: hidden;
  color: #1a1a1a;
  background: #fff;
}

html {
  overflow: hidden;
  height: 100%;
}

#app {
  height: 100%;
}

/* Ant Design overrides for minimalist style */
:root {
  --ant-primary-color: #1a1a1a;
}
</style>
