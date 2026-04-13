<template>
  <a-modal
    :closable="false"
    :footer="null"
    :keyboard="false"
    :maskClosable="false"
    :open="visible"
    centered
    class="identity-modal"
    width="480px"
  >
    <div class="identity-modal-body">
      <div class="identity-header">
        <div class="identity-icon">🎯</div>
        <h2 class="identity-title">只差最后一步</h2>
        <p class="identity-sub">完善身份信息后即可开始对话，系统将为您推荐专属模板</p>
      </div>

      <a-form class="identity-form" layout="vertical">
        <a-form-item label="您的身份" required>
          <a-select
            v-model:value="form.userIdentity"
            :options="identitySelectOptions"
            placeholder="请选择您的身份"
            size="large"
          />
        </a-form-item>

        <a-form-item label="所属行业领域" required>
          <a-auto-complete
            v-model:value="form.userIndustry"
            :filter-option="filterIndustry"
            :options="industryAutoOptions"
            placeholder="请选择或输入您的行业领域"
            size="large"
          />
        </a-form-item>
      </a-form>

      <a-button
        :disabled="!form.userIdentity || !form.userIndustry"
        :loading="submitting"
        block
        class="identity-submit"
        size="large"
        type="primary"
        @click="handleSubmit"
      >
        开始使用
      </a-button>
    </div>
  </a-modal>
</template>

<script lang="ts" setup>
import { computed, reactive, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/loginUser'
import { updateUser } from '@/api/userController'
import { identityOptions, industryOptions } from '@/constants/identityOptions'

const loginUserStore = useLoginUserStore()
const submitting = ref(false)

const form = reactive({
  userIdentity: '',
  userIndustry: ''
})

const visible = computed(() => {
  const user = loginUserStore.loginUser
  if (!user?.id) return false
  return !user.userIdentity || !user.userIndustry
})

const identitySelectOptions = identityOptions.map(item => ({
  value: item.value,
  label: item.label
}))

const industryAutoOptions = industryOptions.map(item => ({
  value: item
}))

const filterIndustry = (input: string, option: { value: string }) => {
  return option.value.toLowerCase().includes(input.toLowerCase())
}

watch(() => loginUserStore.loginUser, (user) => {
  if (user?.userIdentity) form.userIdentity = user.userIdentity
  if (user?.userIndustry) form.userIndustry = user.userIndustry
}, { immediate: true, deep: true })

const handleSubmit = async () => {
  if (!form.userIdentity || !form.userIndustry) {
    message.warning('请完善身份和行业信息')
    return
  }

  submitting.value = true
  try {
    const res = await updateUser({
      id: loginUserStore.loginUser.id,
      userIdentity: form.userIdentity,
      userIndustry: form.userIndustry
    })
    if (res.data.code === 0) {
      message.success('身份信息设置成功')
      loginUserStore.setLoginUser({
        ...loginUserStore.loginUser,
        userIdentity: form.userIdentity,
        userIndustry: form.userIndustry
      })
    } else {
      message.error('设置失败：' + (res.data.message || '请重试'))
    }
  } catch (e: any) {
    message.error('设置失败：' + (e.message || '请重试'))
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.identity-modal-body {
  padding: 8px 0;
}

.identity-header {
  text-align: center;
  margin-bottom: 28px;
}

.identity-icon {
  font-size: 40px;
  margin-bottom: 12px;
}

.identity-title {
  font-size: 22px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0 0 8px;
}

.identity-sub {
  font-size: 14px;
  color: #999;
  margin: 0;
  line-height: 1.5;
}

.identity-form {
  margin-bottom: 8px;
}

.identity-form :deep(.ant-form-item-label > label) {
  font-weight: 600;
  color: #1a1a1a;
  font-size: 14px;
}

.identity-form :deep(.ant-select-selector),
.identity-form :deep(.ant-input) {
  border-radius: 10px !important;
  border-color: #e5e5e5 !important;
}

.identity-form :deep(.ant-select-selector:hover),
.identity-form :deep(.ant-input:hover) {
  border-color: #1a1a1a !important;
}

.identity-form :deep(.ant-select-focused .ant-select-selector) {
  border-color: #1a1a1a !important;
  box-shadow: 0 0 0 2px rgba(26, 26, 26, 0.1) !important;
}

.identity-submit {
  height: 48px;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  background: #1a1a1a;
  border: none;
  margin-top: 8px;
}

.identity-submit:hover:not(:disabled) {
  background: #333 !important;
}

.identity-submit:disabled {
  background: #e5e5e5 !important;
  color: #bbb !important;
}
</style>
