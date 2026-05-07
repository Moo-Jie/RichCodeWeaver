<template>
  <div class="deploy-button-container">
    <!-- 部署按钮 -->
    <el-button
      type="primary"
      :loading="isDeploying"
      :disabled="isDeploying"
      @click="handleDeploy"
    >
      {{ isDeploying ? '部署中...' : '部署产物' }}
    </el-button>

    <!-- 构建进度对话框 -->
    <el-dialog
      v-model="showBuildProgress"
      title="构建进度"
      width="500px"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      :show-close="buildStatus === 'SUCCESS' || buildStatus === 'FAILED'"
    >
      <div class="build-progress-content">
        <!-- 进度条 -->
        <el-progress
          :percentage="buildProgress"
          :status="getProgressStatus()"
          :stroke-width="20"
        />

        <!-- 状态信息 -->
        <div class="status-info">
          <div class="status-label">
            <span class="status-icon" :class="getStatusClass()">
              {{ getStatusIcon() }}
            </span>
            <span class="status-text">{{ buildMessage }}</span>
          </div>
          <div v-if="buildStatus === 'SUCCESS'" class="success-info">
            <el-link :href="deployUrl" target="_blank" type="primary">
              访问已部署站点
            </el-link>
          </div>
          <div v-if="buildStatus === 'FAILED'" class="error-info">
            <el-alert
              :title="buildMessage"
              type="error"
              :closable="false"
              show-icon
            />
          </div>
        </div>
      </div>

      <template #footer>
        <el-button
          v-if="buildStatus === 'SUCCESS' || buildStatus === 'FAILED'"
          @click="showBuildProgress = false"
        >
          关闭
        </el-button>
        <el-button
          v-if="buildStatus === 'FAILED'"
          type="primary"
          @click="handleRetry"
        >
          重试
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onUnmounted } from 'vue';
import { ElMessage } from 'element-plus';
import { deployAppAsync, getTaskStatus } from '@/api/appController';
import taskProgressService from '@/utils/taskProgressService';

const props = defineProps({
  appId: {
    type: Number,
    required: true
  }
});

const isDeploying = ref(false);
const showBuildProgress = ref(false);
const buildStatus = ref('PENDING');
const buildProgress = ref(0);
const buildMessage = ref('准备部署...');
const deployUrl = ref('');

/**
 * 处理部署
 */
const handleDeploy = async () => {
  try {
    isDeploying.value = true;
    showBuildProgress.value = true;
    buildStatus.value = 'PENDING';
    buildProgress.value = 0;
    buildMessage.value = '正在提交部署任务...';

    // 1. 调用部署接口
    const response = await deployAppAsync({ appId: props.appId });
    deployUrl.value = response.data;

    // 2. 订阅构建进度
    await taskProgressService.subscribeTaskProgress(props.appId, (progress) => {
      buildStatus.value = progress.status;
      buildProgress.value = progress.progress;
      buildMessage.value = progress.message;

      if (progress.status === 'SUCCESS') {
        ElMessage.success('部署成功！');
        isDeploying.value = false;
        if (progress.result) {
          deployUrl.value = progress.result;
        }
      } else if (progress.status === 'FAILED') {
        ElMessage.error('部署失败: ' + progress.message);
        isDeploying.value = false;
      }
    });

    buildMessage.value = '任务已提交，正在构建中...';

  } catch (error) {
    console.error('部署失败:', error);
    ElMessage.error('部署失败: ' + (error.message || '未知错误'));
    isDeploying.value = false;
    showBuildProgress.value = false;
  }
};

/**
 * 重试部署
 */
const handleRetry = () => {
  showBuildProgress.value = false;
  handleDeploy();
};

/**
 * 获取进度条状态
 */
const getProgressStatus = () => {
  if (buildStatus.value === 'SUCCESS') return 'success';
  if (buildStatus.value === 'FAILED') return 'exception';
  return undefined;
};

/**
 * 获取状态图标
 */
const getStatusIcon = () => {
  const icons = {
    PENDING: '⏳',
    RUNNING: '🔄',
    SUCCESS: '✅',
    FAILED: '❌',
    RETRYING: '🔁'
  };
  return icons[buildStatus.value] || '⏳';
};

/**
 * 获取状态样式类
 */
const getStatusClass = () => {
  return `status-${buildStatus.value.toLowerCase()}`;
};

// 组件卸载时取消订阅
onUnmounted(() => {
  taskProgressService.unsubscribeTaskProgress(props.appId);
});
</script>

<style scoped>
.deploy-button-container {
  display: inline-block;
}

.build-progress-content {
  padding: 20px 0;
}

.status-info {
  margin-top: 20px;
}

.status-label {
  display: flex;
  align-items: center;
  font-size: 16px;
  margin-bottom: 10px;
}

.status-icon {
  font-size: 24px;
  margin-right: 10px;
}

.status-text {
  color: #606266;
}

.success-info,
.error-info {
  margin-top: 15px;
}

.status-running {
  animation: rotate 1s linear infinite;
}

@keyframes rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}
</style>
