<template>
  <div class="terms-page">
    <div class="page-header">
      <h1 class="page-title">用户协议</h1>
    </div>

    <div class="content-wrap">
      <section class="section">
        <p class="section-text">
          <em>MoJie
            团队</em>（以下简称"我们"）依据本协议为用户（以下简称"你"）提供<em>RichCodeWeaver</em>服务。本协议对你和我们均具有法律约束力。
        </p>
      </section>

      <section class="section">
        <h2 class="section-title">一、本服务的功能</h2>
        <p class="section-text">
          你可以使用本服务RichCodeWeaver（织码睿奇）是一个基于AI技术的智能代码生成平台，旨在通过自然语言理解和大语言模型技术，帮助用户快速将创意想法转化为可运行的数字产物。
        </p>
      </section>

      <section class="section">
        <h2 class="section-title">二、责任范围及限制</h2>
        <p class="section-text">
          你使用本服务得到的结果仅供参考，实际情况以官方为准。
        </p>
      </section>

      <section class="section">
        <h2 class="section-title">三、隐私保护</h2>
        <p class="section-text">
          我们重视对你隐私的保护，你的个人隐私信息将根据《隐私政策》受到保护与规范，详情请参阅《隐私政策》。
        </p>
      </section>

      <section class="section">
        <h2 class="section-title">四、其他条款</h2>
        <p class="section-text">
          4.1 本协议所有条款的标题仅为阅读方便，本身并无实际涵义，不能作为本协议涵义解释的依据。
        </p>
        <p class="section-text">
          4.2 本协议条款无论因何种原因部分无效或不可执行，其余条款仍有效，对双方具有约束力。
        </p>
      </section>
    </div>
  </div>
</template>

<script lang="ts" setup>
</script>

<style scoped>
.terms-page {
  padding: 32px;
  max-width: 960px;
  margin: 0 auto;
  width: 100%;
}

.page-header {
  text-align: center;
  margin-bottom: 40px;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0;
}

.content-wrap {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.section {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 14px;
  padding: 24px;
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0 0 16px;
}

.section-text {
  font-size: 14px;
  color: #666;
  line-height: 1.7;
  margin: 0 0 16px;
}

.section-text:last-child {
  margin-bottom: 0;
}

.section-text em {
  font-style: normal;
  color: #1a1a1a;
  font-weight: 500;
}

@media (max-width: 768px) {
  .terms-page {
    padding: 20px;
  }
}
</style>
