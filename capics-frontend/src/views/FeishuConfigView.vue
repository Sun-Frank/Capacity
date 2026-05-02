<template>
  <div class="page">
    <section class="config-card feishu-config-card">
      <div class="section-heading">
        <h2 class="section-title">飞书应用配置</h2>
        <p class="section-subtitle">维护飞书 OpenAPI 参数，并验证消息发送能力。</p>
      </div>

      <div class="form-grid">
        <label class="form-field">
          <span class="form-label">API URL</span>
          <input v-model.trim="form.apiUrl" class="form-input" placeholder="https://open.feishu.cn" />
        </label>
        <label class="form-field">
          <span class="form-label">App ID</span>
          <input v-model.trim="form.appId" class="form-input" />
        </label>
        <label class="form-field">
          <span class="form-label">App Secret</span>
          <input v-model.trim="form.appSecret" type="password" class="form-input" placeholder="留空则保留服务器已有密钥" />
        </label>
      </div>

      <div class="toolbar form-actions">
        <button class="btn btn-primary" @click="save">保存配置</button>
        <button class="btn" @click="test">端口测试</button>
      </div>

      <div class="divider"></div>

      <div class="section-heading compact">
        <h3 class="section-title">发送测试</h3>
        <p class="section-subtitle">向指定邮箱对应的飞书联系人发送测试消息。</p>
      </div>
      <label class="form-field">
        <span class="form-label">收件人邮箱</span>
        <input v-model.trim="email" class="form-input" placeholder="name@example.com" />
      </label>
      <label class="form-field">
        <span class="form-label">消息内容</span>
        <textarea v-model="content" class="form-textarea"></textarea>
      </label>
      <button class="btn btn-primary" @click="send">发送飞书消息</button>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useAuth } from '@/composables/useAuth'
import { useToast } from '@/composables/useToast'
import { getFeishuConfig, saveFeishuConfig, sendFeishuMessage, testFeishuConfig } from '@/api/feishuConfig'

const { token } = useAuth()
const { showToast } = useToast()
const form = ref({ apiUrl: 'https://open.feishu.cn', appId: '', appSecret: '' })
const email = ref('')
const content = ref('CAPICS 会议通知测试')

const load = async () => {
  const res = await getFeishuConfig(token.value)
  form.value = {
    apiUrl: res?.data?.apiUrl || 'https://open.feishu.cn',
    appId: res?.data?.appId || '',
    appSecret: ''
  }
}

const save = async () => {
  const res = await saveFeishuConfig(token.value, form.value)
  showToast(res?.message || '保存成功', 'success')
  form.value.appSecret = ''
}

const test = async () => {
  const res = await testFeishuConfig(token.value, form.value)
  showToast(res?.data || res?.message || '测试完成', 'success')
}

const send = async () => {
  if (!email.value) return showToast('请输入收件人邮箱', 'warning')
  const res = await sendFeishuMessage(token.value, { emails: [email.value], content: content.value })
  showToast(res?.message || '发送成功', 'success')
}

onMounted(load)
</script>

<style scoped>
.feishu-config-card {
  max-width: 820px;
}

.section-heading {
  margin-bottom: var(--space-4);
}

.section-heading.compact {
  margin-top: var(--space-2);
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-4);
}

.form-field {
  display: flex;
  flex-direction: column;
}

.form-field:nth-child(3) {
  grid-column: 1 / -1;
}

.divider {
  height: 1px;
  margin: var(--space-5) 0;
  background: var(--border-light);
}

.form-textarea {
  width: 100%;
}

@media (max-width: 768px) {
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
