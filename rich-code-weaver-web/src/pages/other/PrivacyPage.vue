<template>
  <div class="privacy-page">
    <div class="page-header">
      <h1 class="page-title">隐私政策</h1>
      <div class="page-dates">
        <span>更新日期：<strong>2026/3/25</strong></span>
        <span>生效日期：<strong>2026/3/25</strong></span>
      </div>
    </div>

    <div class="content-wrap">
      <section class="section">
        <h2 class="section-title">导言</h2>
        <p class="section-text">
          <em>RichCodeWeaver</em> 是一款由 <em>MoJie 团队</em> （以下简称"我们"）提供的产品。
          您在使用我们的服务时，我们可能会收集和使用您的相关信息。我们希望通过本《隐私政策》向您说明，在使用我们的服务时，我们如何收集、使用、储存和分享这些信息，以及我们为您提供的访问、更新、控制和保护这些信息的方式。
          本《隐私政策》与您所使用的 <em>RichCodeWeaver</em>
          服务息息相关，希望您仔细阅读，在需要时，按照本《隐私政策》的指引，作出您认为适当的选择。本《隐私政策》中涉及的相关技术词汇，我们尽量以简明扼要的表述，并提供进一步说明的链接，以便您的理解。
        </p>
        <p class="section-text highlight">
          您使用或继续使用我们的服务，即意味着同意我们按照本《隐私政策》收集、使用、储存和分享您的相关信息。
        </p>
        <p class="section-text">
          如对本《隐私政策》或相关事宜有任何问题，请通过
          <strong>NotProvidedTemporarilyRUBY 智能助手.com</strong> 与我们联系。
        </p>
      </section>

      <section class="section">
        <h2 class="section-title">1. 我们收集的信息</h2>
        <p class="section-text">
          我们或我们的第三方合作伙伴提供服务时，可能会收集、储存和使用下列与您有关的信息。如果您不提供相关信息，可能无法注册成为我们的用户或无法享受我们提供的某些服务，或者无法达到相关服务拟达到的效果。
        </p>
        <ul class="info-list">
          <li><strong>个人信息</strong>，您在注册账户或使用我们的服务时，向我们提供的相关个人信息，例如电话号码、电子邮件等。
          </li>
          <li><strong>日志信息</strong>，指您使用我们的服务时，系统可能通过cookies、标识符及相关技术收集的信息，包括您的<strong>设备信息</strong>、<strong>浏览信息</strong>、<strong>点击信息</strong>，并将该等信息储存为日志信息，为您提供个性化的用户体验、保障服务安全。您可以通过浏览器设置拒绝或管理cookie、标识符或相关技术的使用。
          </li>
        </ul>
        <p class="section-text sdk-title">我们接入的第三方SDK：</p>
        <div class="sdk-list">
          <div class="sdk-item">
            <h3 class="sdk-name">OpenAI API (通过LangChain4j集成)</h3>
            <dl class="sdk-details">
              <div class="sdk-detail-item">
                <dt>第三方名称：</dt>
                <dd>OpenAI, L.L.C</dd>
              </div>
              <div class="sdk-detail-item">
                <dt>使用目的：</dt>
                <dd>为应用提供大语言模型(LLM)的智能对话与内容生成能力。</dd>
              </div>
              <div class="sdk-detail-item">
                <dt>收集个人信息类型：</dt>
                <dd>用户在使用AI功能时主动提交的所有输入内容、提问或指令。</dd>
              </div>
              <div class="sdk-detail-item">
                <dt>处理方式：</dt>
                <dd>应用将用户输入内容传输至OpenAI的服务器进行处理，以获取AI生成的回复。</dd>
              </div>
              <div class="sdk-detail-item">
                <dt>隐私政策链接：</dt>
                <dd><a class="sdk-link" href="https://openai.com/policies/privacy-policy"
                       target="_blank">https://openai.com/policies/privacy-policy</a></dd>
              </div>
            </dl>
          </div>
          <div class="sdk-item">
            <h3 class="sdk-name">阿里云百炼 (dashscope-sdk-java)</h3>
            <dl class="sdk-details">
              <div class="sdk-detail-item">
                <dt>第三方名称：</dt>
                <dd>阿里巴巴集团</dd>
              </div>
              <div class="sdk-detail-item">
                <dt>使用目的：</dt>
                <dd>为应用提供大语言模型(LLM)等AI能力，作为OpenAI的替代或补充选项。</dd>
              </div>
              <div class="sdk-detail-item">
                <dt>收集个人信息类型：</dt>
                <dd>用户在使用相关AI功能时主动提交的所有输入内容、提问或指令。</dd>
              </div>
              <div class="sdk-detail-item">
                <dt>处理方式：</dt>
                <dd>应用将用户输入内容传输至阿里云百炼的服务器进行处理，以获取AI生成的回复。</dd>
              </div>
              <div class="sdk-detail-item">
                <dt>隐私政策链接：</dt>
                <dd><a class="sdk-link" href="https://help.aliyun.com/document_detail/189971.html"
                       target="_blank">https://help.aliyun.com/document_detail/189971.html</a></dd>
              </div>
            </dl>
          </div>
          <div class="sdk-item">
            <h3 class="sdk-name">阿里云对象存储 OSS (aliyun-sdk-oss)</h3>
            <dl class="sdk-details">
              <div class="sdk-detail-item">
                <dt>第三方名称：</dt>
                <dd>阿里巴巴集团</dd>
              </div>
              <div class="sdk-detail-item">
                <dt>使用目的：</dt>
                <dd>为用户提供文件上传、存储和下载服务。</dd>
              </div>
              <div class="sdk-detail-item">
                <dt>收集个人信息类型：</dt>
                <dd>用户主动上传的文件，可能包含图片、文档等任何格式的个人文件。</dd>
              </div>
              <div class="sdk-detail-item">
                <dt>处理方式：</dt>
                <dd>应用将用户上传的文件传输并存储于阿里云OSS的服务器。</dd>
              </div>
              <div class="sdk-detail-item">
                <dt>隐私政策链接：</dt>
                <dd><a class="sdk-link" href="https://help.aliyun.com/document_detail/189731.html"
                       target="_blank">https://help.aliyun.com/document_detail/189731.html</a></dd>
              </div>
            </dl>
          </div>
        </div>
      </section>

      <section class="section">
        <h2 class="section-title">2. 信息的存储</h2>
        <h3 class="subsection-title">2.1 信息存储的方式和期限</h3>
        <ul class="info-list">
          <li>
            我们会通过安全的方式存储您的信息，包括本地存储（例如利用APP进行数据缓存）、数据库和服务器日志。
          </li>
          <li>
            一般情况下，我们只会在为实现服务目的所必需的时间内或法律法规规定的条件下存储您的个人信息。
          </li>
        </ul>
        <h3 class="subsection-title">2.2 信息存储的地域</h3>
        <ul class="info-list">
          <li>我们会按照法律法规规定，将境内收集的用户个人信息存储于中国境内。</li>
          <li>
            目前我们不会跨境传输或存储您的个人信息。将来如需跨境传输或存储的，我们会向您告知信息出境的目的、接收方、安全保证措施和安全风险，并征得您的同意。
          </li>
        </ul>
        <h3 class="subsection-title">2.3 产品或服务停止运营时的通知</h3>
        <ul class="info-list">
          <li>
            当我们的产品或服务发生停止运营的情况时，我们将以推送通知、公告等形式通知您，并在合理期限内删除您的个人信息或进行匿名化处理，法律法规另有规定的除外。
          </li>
        </ul>
      </section>

      <section class="section">
        <h2 class="section-title">3. 信息安全</h2>
        <p class="section-text">
          我们使用各种安全技术和程序，以防信息的丢失、不当使用、未经授权阅览或披露。例如，在某些服务中，我们将利用加密技术（例如SSL）来保护您提供的个人信息。但请您理解，由于技术的限制以及可能存在的各种恶意手段，在互联网行业，即便竭尽所能加强安全措施，也不可能始终保证信息百分之百的安全。您需要了解，您接入我们的服务所用的系统和通讯网络，有可能因我们可控范围外的因素而出现问题。
        </p>
      </section>

      <section class="section">
        <h2 class="section-title">4. 我们如何使用信息</h2>
        <p class="section-text">我们可能将在向您提供服务的过程之中所收集的信息用作下列用途：</p>
        <ul class="info-list">
          <li>向您提供服务；</li>
          <li>
            在我们提供服务时，用于身份验证、客户服务、安全防范、诈骗监测、存档和备份用途，确保我们向您提供的产品和服务的安全性；
          </li>
          <li>帮助我们设计新服务，改善我们现有服务；</li>
          <li>
            使我们更加了解您如何接入和使用我们的服务，从而针对性地回应您的个性化需求，例如语言设定、位置设定、个性化的帮助服务和指示，或对您和其他用户作出其他方面的回应；
          </li>
          <li>向您提供与您更加相关的广告以替代普遍投放的广告；</li>
          <li>评估我们服务中的广告和其他促销及推广活动的效果，并加以改善；</li>
          <li>软件认证或管理软件升级；</li>
          <li>让您参与有关我们产品和服务的调查。</li>
        </ul>
      </section>

      <section class="section">
        <h2 class="section-title">5. 信息共享</h2>
        <p class="section-text">
          目前，我们不会主动共享或转让您的个人信息至第三方，如存在其他共享或转让您的个人信息或您需要我们将您的个人信息共享或转让至第三方情形时，我们会直接或确认第三方征得您对上述行为的明示同意。
        </p>
        <p class="section-text">
          为了投放广告，评估、优化广告投放效果等目的，我们需要向广告主及其代理商等第三方合作伙伴共享您的部分数据，要求其严格遵守我们关于数据隐私保护的措施与要求，包括但不限于根据数据保护协议、承诺书及相关数据处理政策进行处理，避免识别出个人身份，保障隐私安全。
        </p>
        <p class="section-text">
          我们不会向合作伙伴分享可用于识别您个人身份的信息（例如您的姓名或电子邮件地址），除非您明确授权。
        </p>
        <p class="section-text">
          我们不会对外公开披露所收集的个人信息，如必须公开披露时，我们会向您告知此次公开披露的目的、披露信息的类型及可能涉及的敏感信息，并征得您的明示同意。
        </p>
        <p class="section-text">
          随着我们业务的持续发展，我们有可能进行合并、收购、资产转让等交易，我们将告知您相关情形，按照法律法规及不低于本《隐私政策》所要求的标准继续保护或要求新的控制者继续保护您的个人信息。
        </p>
        <p class="section-text">
          另外，根据相关法律法规及国家标准，以下情形中，我们可能会共享、转让、公开披露个人信息无需事先征得您的授权同意：
        </p>
        <ul class="info-list">
          <li>与国家安全、国防安全直接相关的；</li>
          <li>与公共安全、公共卫生、重大公共利益直接相关的；</li>
          <li>犯罪侦查、起诉、审判和判决执行等直接相关的；</li>
          <li>出于维护个人信息主体或其他个人的生命、财产等重大合法权益但又很难得到本人同意的；</li>
          <li>个人信息主体自行向社会公众公开个人信息的；</li>
          <li>从合法公开披露的信息中收集个人信息的，如合法的新闻报道、政府信息公开等渠道。</li>
        </ul>
      </section>

      <section class="section">
        <h2 class="section-title">6. 您的权利</h2>
        <p class="section-text">
          在您使用我们的服务期间，我们可能会视产品具体情况为您提供相应的操作设置，以便您可以查询、删除、更正或撤回您的相关个人信息，您可参考相应的具体指引进行操作。此外，我们还设置了投诉举报渠道，您的意见将会得到及时的处理。如果您无法通过上述途径和方式行使您的个人信息主体权利，您可以通过本《隐私政策》中提供的联系方式提出您的请求，我们会按照法律法规的规定予以反馈。
        </p>
        <p class="section-text">
          当您决定不再使用我们的产品或服务时，可以申请注销账户。注销账户后，除法律法规另有规定外，我们将删除或匿名化处理您的个人信息。
        </p>
      </section>

      <section class="section">
        <h2 class="section-title">7. 变更</h2>
        <p class="section-text">
          我们可能适时修订本《隐私政策》的条款。当变更发生时，我们会在版本更新时向您提示新的《隐私政策》，并向您说明生效日期。请您仔细阅读变更后的《隐私政策》内容，<strong>若您继续使用我们的服务，即表示您同意我们按照更新后的《隐私政策》处理您的个人信息。</strong>
        </p>
      </section>

      <section class="section">
        <h2 class="section-title">8. 未成年人保护</h2>
        <p class="section-text">
          我们鼓励父母或监护人指导未满十八岁的未成年人使用我们的服务。我们建议未成年人鼓励他们的父母或监护人阅读本《隐私政策》，并建议未成年人在提交的个人信息之前寻求父母或监护人的同意和指导。
        </p>
      </section>

    </div>
  </div>
</template>

<script lang="ts" setup>
</script>

<style scoped>
.privacy-page {
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
  margin: 0 0 12px;
}

.page-dates {
  display: flex;
  justify-content: center;
  gap: 24px;
  font-size: 13px;
  color: #666;
}

.page-dates strong {
  color: #1a1a1a;
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

.section-text.highlight {
  font-weight: 600;
  color: #1a1a1a;
}

.section-text em {
  font-style: normal;
  color: #1a1a1a;
  font-weight: 500;
}

.subsection-title {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 20px 0 12px;
}

.sdk-title {
  font-weight: 600;
  margin-top: 20px;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 16px;
}

.info-card {
  background: #fafafa;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  padding: 20px;
}

.info-card-title {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0 0 12px;
}

.info-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.info-list li {
  font-size: 13px;
  color: #666;
  line-height: 1.6;
  margin-bottom: 8px;
  padding-left: 16px;
  position: relative;
}

.info-list li::before {
  content: '•';
  position: absolute;
  left: 0;
  color: #999;
}

.info-list li:last-child {
  margin-bottom: 0;
}

.sdk-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-top: 16px;
}

.sdk-item {
  background: #fafafa;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  padding: 20px;
}

.sdk-name {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0 0 16px;
}

.sdk-details {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin: 0;
}

.sdk-detail-item {
  display: flex;
  gap: 8px;
  font-size: 13px;
  line-height: 1.6;
}

.sdk-detail-item dt {
  font-weight: 600;
  color: #1a1a1a;
  min-width: 120px;
  flex-shrink: 0;
}

.sdk-detail-item dd {
  color: #666;
  margin: 0;
  flex: 1;
}

.sdk-link {
  color: #666;
  text-decoration: none;
  word-break: break-all;
  transition: color 0.15s;
}

.sdk-link:hover {
  color: #1a1a1a;
  text-decoration: underline;
}

.usage-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 16px;
}

.usage-item {
  display: flex;
  gap: 12px;
  background: #fafafa;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  padding: 16px;
}

.usage-number {
  width: 32px;
  height: 32px;
  background: #1a1a1a;
  color: #fff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
  flex-shrink: 0;
}

.usage-content {
  flex: 1;
  min-width: 0;
}

.usage-title {
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0 0 4px;
}

.usage-desc {
  font-size: 13px;
  color: #999;
  line-height: 1.5;
  margin: 0;
}

.security-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 16px;
}

.security-card {
  background: #fafafa;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  padding: 16px;
}

.security-title {
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0 0 8px;
}

.security-desc {
  font-size: 13px;
  color: #999;
  line-height: 1.5;
  margin: 0;
}

.disclosure-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 16px;
}

.disclosure-item {
  display: flex;
  gap: 12px;
  padding: 12px;
  background: #fafafa;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
}

.disclosure-label {
  font-size: 13px;
  font-weight: 600;
  color: #1a1a1a;
  min-width: 80px;
  flex-shrink: 0;
}

.disclosure-value {
  font-size: 13px;
  color: #666;
  line-height: 1.5;
}

.rights-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 16px;
}

.right-card {
  background: #fafafa;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  padding: 16px;
}

.right-title {
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0 0 8px;
}

.right-desc {
  font-size: 13px;
  color: #999;
  line-height: 1.5;
  margin: 0;
}

.contact-info {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 16px;
}

.contact-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: #fafafa;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
}

.contact-label {
  font-size: 13px;
  font-weight: 600;
  color: #1a1a1a;
  min-width: 80px;
}

.contact-value {
  font-size: 13px;
  color: #666;
  text-decoration: none;
  word-break: break-all;
  transition: color 0.15s;
}

.contact-value:hover {
  color: #1a1a1a;
}

@media (max-width: 768px) {
  .privacy-page {
    padding: 20px;
  }

  .page-dates {
    flex-direction: column;
    gap: 8px;
  }

  .sdk-detail-item {
    flex-direction: column;
    gap: 4px;
  }

  .sdk-detail-item dt {
    min-width: auto;
  }
}
</style>
