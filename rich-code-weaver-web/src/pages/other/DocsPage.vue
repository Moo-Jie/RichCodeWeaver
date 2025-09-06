<template>
  <div class="docs-page">
    <!-- 顶部导航 -->
    <div class="header-nav">
      <router-link to="/" class="nav-link">
        <arrow-left-outlined /> 返回首页
      </router-link>
    </div>

    <!-- 文档内容区域 -->
    <div class="docs-container">
      <h1 class="section-title">文档中心</h1>

      <!-- 使用 Collapse 折叠面板重构文档内容 -->
      <a-collapse
        v-model:activeKey="activeKeys"
        accordion
        ghost
        class="custom-collapse"
        :expand-icon-position="'right'"
      >
        <!-- 快速入门区域 -->
        <a-collapse-panel key="1" :show-arrow="false">
          <template #header>
            <div class="panel-header">
              <rocket-outlined class="section-icon" />
              <span class="panel-title">快速入门</span>
            </div>
          </template>

          <a-list item-layout="horizontal" :data-source="quickStartItems">
            <template #renderItem="{ item }">
              <a-list-item class="doc-list-item" @click="handleItemClick(item.id)">
                <a-list-item-meta>
                  <template #title>
                    <div class="list-title">{{ item.title }}</div>
                  </template>
                </a-list-item-meta>
              </a-list-item>
            </template>
          </a-list>
        </a-collapse-panel>

        <!-- 应用优化区域 -->
        <a-collapse-panel key="2" :show-arrow="false">
          <template #header>
            <div class="panel-header">
              <code-outlined class="section-icon" />
              <span class="panel-title">应用优化</span>
            </div>
          </template>

          <a-list item-layout="horizontal" :data-source="optimizationItems">
            <template #renderItem="{ item }">
              <a-list-item class="doc-list-item" @click="handleItemClick(item.id)">
                <a-list-item-meta>
                  <template #title>
                    <div class="list-title">{{ item.title }}</div>
                  </template>
                </a-list-item-meta>
              </a-list-item>
            </template>
          </a-list>
        </a-collapse-panel>

        <!-- 最佳实践区域 -->
        <a-collapse-panel key="3" :show-arrow="false">
          <template #header>
            <div class="panel-header">
              <experiment-outlined class="section-icon" />
              <span class="panel-title">最佳实践</span>
            </div>
          </template>

          <a-list item-layout="horizontal" :data-source="bestPracticeItems">
            <template #renderItem="{ item }">
              <a-list-item class="doc-list-item" @click="handleItemClick(item.id)">
                <a-list-item-meta>
                  <template #title>
                    <div class="list-title">{{ item.title }}</div>
                  </template>
                </a-list-item-meta>
              </a-list-item>
            </template>
          </a-list>
        </a-collapse-panel>
      </a-collapse>

      <!-- 文档详情区域 -->
      <div class="docs-content">
        <div v-if="activeSection" class="doc-section">
          <h2 class="section-heading">{{ activeSection.title }}</h2>

          <!-- 动态内容渲染 -->
          <div v-if="activeSection.id === 'getting-started'">
            <p>1. 在首页输入应用描述，或者选择"热门提示词"。</p>
            <p>2. 选择应用生成模式，点击"创建作品"按钮生成应用。</p>
            <p>3. 系统自动进入应用生成页面，生成应用代码。</p>
            <p>4. 可在应用生成页面对应用进行实时预览、AI 局部优化、AI 全局重构、部署上线、编辑应用信息等操作。</p>
          </div>

          <div v-if="activeSection.id === 'prompt-guide'">
            <p>1. 应用基础信息：应用名称、应用类型与核心功能、目标用户等。</p>
            <p>2. 技术栈与项目规范： 项目结构（Vue项目/ React项目/ 纯静态项目）、UI框架/组件库等。</p>
            <p>3. 详细功能需求：功能列表、交互流程、数据持久化等。</p>
            <p>4. UI/UX与布局设计：布局描述、风格描述、附加元素等。</p>
            <p>5. 数据与API：应用需要动态数据，必须详细说明。</p>
          </div>

          <div v-if="activeSection.id === 'deploy-app'">
            <p>1. 登录应用平台，进入应用详情页。</p>
            <p>2. 点击"部署为可访问网站"按钮，即可部署应用。</p>
            <p>3. 后续点击"访问已部署网站"即可访问应用。</p>
          </div>
        </div>

        <div v-else class="placeholder-section">
          <rocket-outlined class="placeholder-icon" />
          <p>请从左侧选择一个文档主题</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue';
import {
  RocketOutlined,
  CodeOutlined,
  ExperimentOutlined,
  ArrowLeftOutlined
} from '@ant-design/icons-vue';

// 折叠面板状态管理
const activeKeys = ref<string[]>(['1']);

// 文档项数据
const quickStartItems = reactive([
  { id: 'getting-started', title: '创建第一个应用' },
  { id: 'prompt-guide', title: '提示词编写指南' },
  { id: 'deploy-app', title: '应用部署流程' }
]);

const optimizationItems = reactive([
  { id: 'api-overview', title: '代码优化' },
  { id: 'auth-api', title: '数据本地化' },
  { id: 'app-api', title: '部署优化' }
]);

const bestPracticeItems = reactive([
  { id: 'ecommerce-case', title: '电商应用案例' },
  { id: 'education-case', title: '教育应用案例' },
  { id: 'performance-opt', title: '性能优化指南' }
]);

// 当前活动文档区域
const activeSection = ref<{id: string; title: string} | null>(null);

// 处理文档项点击
const handleItemClick = (id: string) => {
  // 查找对应的文档项
  const allItems = [...quickStartItems, ...optimizationItems, ...bestPracticeItems];
  const item = allItems.find(i => i.id === id);

  if (item) {
    activeSection.value = { id: item.id, title: item.title };

    // 滚动到文档区域
    setTimeout(() => {
      const element = document.querySelector('.docs-content');
      if (element) {
        element.scrollIntoView({ behavior: 'smooth', block: 'center' });
      }
    }, 100);
  }
};
</script>

<style scoped>
.docs-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
  background: #f5f7fa;
  min-height: 100vh;
}

.header-nav {
  margin-bottom: 30px;
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

.docs-container {
  background: white;
  border-radius: 12px;
  padding: 40px;
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.08);
  display: grid;
  grid-template-columns: 1fr 2fr;
  gap: 30px;
}

.section-title {
  grid-column: 1 / -1;
  text-align: center;
  margin-bottom: 20px;
  color: #1d3660;
  font-size: 32px;
  font-weight: 600;
  position: relative;
  padding-bottom: 15px;
}

.section-title::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 80px;
  height: 4px;
  background: linear-gradient(90deg, #1890ff, #52c41a);
  border-radius: 2px;
}

/* 折叠面板样式 */
.custom-collapse {
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.custom-collapse :deep(.ant-collapse-item) {
  border-bottom: 1px solid #f0f0f0;
}

.custom-collapse :deep(.ant-collapse-header) {
  padding: 16px 20px !important;
  background: #f9fafb !important;
  transition: all 0.3s;
}

.custom-collapse :deep(.ant-collapse-item-active .ant-collapse-header) {
  background: #e6f7ff !important;
}

.custom-collapse :deep(.ant-collapse-content) {
  border-top: none;
  background: #fff;
}

.custom-collapse :deep(.ant-collapse-content-box) {
  padding: 0 !important;
}

.panel-header {
  display: flex;
  align-items: center;
  gap: 12px;
}

.panel-title {
  font-size: 18px;
  font-weight: 500;
  color: #000000;
}

.section-icon {
  font-size: 20px;
  color: #1890ff;
}

/* 文档列表项样式 */
.doc-list-item {
  padding: 12px 20px;
  transition: all 0.2s;
  cursor: pointer;
  border-bottom: 1px solid #f0f0f0;
}

.doc-list-item:last-child {
  border-bottom: none;
}

.doc-list-item:hover {
  background: #f6ffed;
}

.list-title {
  font-size: 15px;
  color: #595959;
}

/* 文档内容区域样式 */
.docs-content {
  background: #fff;
  border-radius: 8px;
  padding: 30px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  min-height: 400px;
}

.doc-section {
  animation: fadeIn 0.4s ease;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.section-heading {
  color: #1d3660;
  border-left: 4px solid #1890ff;
  padding-left: 15px;
  margin-bottom: 25px;
  font-size: 24px;
}

.placeholder-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #bfbfbf;
  text-align: center;
}

.placeholder-icon {
  font-size: 64px;
  margin-bottom: 20px;
  color: #d9d9d9;
}

.example-btn {
  margin-top: 20px;
}

.screenshot-placeholder {
  height: 200px;
  background: linear-gradient(45deg, #f5f7fa, #e8ecef);
  border-radius: 8px;
  margin-top: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #8c8c8c;
}

.deploy-options {
  margin-top: 20px;
  display: flex;
  gap: 10px;
}

.example-header {
  font-weight: 500;
  color: #1890ff;
}
</style>
