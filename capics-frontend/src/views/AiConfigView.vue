<template>
  <div class="page ai-config-page">
    <div class="page-header">
      <h1 class="page-title">AI后端环境配置</h1>
      <p class="page-subtitle">配置 MRP 对比页面 AI Agent 的后端连接参数</p>
    </div>

    <section class="table-wrapper config-panel">
      <div class="form-grid">
        <div class="form-group">
          <label class="form-label">API Key</label>
          <input
            v-model.trim="form.apiKey"
            class="form-input"
            :type="showApiKey ? 'text' : 'password'"
            placeholder="留空表示不修改；输入空字符串并保存可清空"
          />
          <div class="field-hint">
            当前状态：{{ configStatusText }}
            <button class="btn btn-link" @click="showApiKey = !showApiKey">
              {{ showApiKey ? '隐藏' : '显示' }}
            </button>
          </div>
        </div>

        <div class="form-group">
          <label class="form-label">Base URL</label>
          <input v-model.trim="form.baseUrl" class="form-input" placeholder="例如：https://api.openai.com/v1" />
        </div>

        <div class="form-group">
          <label class="form-label">Model</label>
          <input v-model.trim="form.model" class="form-input" placeholder="例如：gpt-4o-mini" />
        </div>

        <div class="form-group">
          <label class="form-label">Timeout (ms)</label>
          <input v-model.number="form.timeoutMs" type="number" min="1000" step="1000" class="form-input" />
        </div>
      </div>

      <div class="config-meta" v-if="meta.updatedAt || meta.source">
        <span v-if="meta.source">来源：{{ meta.source }}</span>
        <span v-if="meta.updatedBy">最后修改人：{{ meta.updatedBy }}</span>
        <span v-if="meta.updatedAt">最后修改时间：{{ meta.updatedAt }}</span>
      </div>

      <div class="actions-row">
        <button class="btn" :disabled="loading || saving || testing" @click="loadConfig">
          {{ loading ? '加载中...' : '重新加载' }}
        </button>
        <button class="btn" :disabled="loading || saving || testing" @click="testConfig">
          {{ testing ? '测试中...' : 'API端口测试' }}
        </button>
        <button class="btn btn-primary" :disabled="saving || testing" @click="saveConfig">
          {{ saving ? '保存中...' : '保存配置' }}
        </button>
      </div>

      <div v-if="testResult" class="test-result" :class="testResult.ok ? 'ok' : 'fail'">
        <div class="test-title">{{ testResult.ok ? '连接测试成功' : '连接测试失败' }}</div>
        <div class="test-grid">
          <span>Endpoint：</span>
          <span>{{ testResult.endpoint || '--' }}</span>
          <span>HTTP状态：</span>
          <span>{{ testResult.status ?? '--' }}</span>
          <span>耗时：</span>
          <span>{{ testResult.latencyMs ? `${testResult.latencyMs} ms` : '--' }}</span>
          <template v-if="testResult.errorType">
            <span>错误类型：</span>
            <span>{{ testResult.errorType }}</span>
          </template>
          <template v-if="testResult.errorCode">
            <span>错误码：</span>
            <span>{{ testResult.errorCode }}</span>
          </template>
          <template v-if="testResult.providerMessage">
            <span>上游消息：</span>
            <span>{{ testResult.providerMessage }}</span>
          </template>
          <span>说明：</span>
          <span>{{ testResult.message || '--' }}</span>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { useAuth } from '@/composables/useAuth'
import { useToast } from '@/composables/useToast'
import { getAiBackendConfig, saveAiBackendConfig, testAiBackendConfig } from '@/api/aiConfig'

const { token } = useAuth()
const { showToast } = useToast()

const loading = ref(false)
const saving = ref(false)
const testing = ref(false)
const showApiKey = ref(false)
const testResult = ref(null)

const form = reactive({
  apiKey: '',
  baseUrl: '',
  model: '',
  timeoutMs: 45000
})

const meta = reactive({
  hasApiKey: false,
  apiKeyMasked: '',
  source: '',
  updatedBy: '',
  updatedAt: ''
})

const configStatusText = computed(() => {
  if (meta.hasApiKey) return `已配置（${meta.apiKeyMasked || '已隐藏'}）`
  return '未配置'
})

async function loadConfig() {
  loading.value = true
  try {
    const res = await getAiBackendConfig(token.value)
    const data = res.data || {}

    form.apiKey = ''
    form.baseUrl = data.baseUrl || ''
    form.model = data.model || ''
    form.timeoutMs = data.timeoutMs || 45000

    meta.hasApiKey = !!data.hasApiKey
    meta.apiKeyMasked = data.apiKeyMasked || ''
    meta.source = data.source || ''
    meta.updatedBy = data.updatedBy || ''
    meta.updatedAt = data.updatedAt || ''
  } catch (error) {
    console.error('Load AI config failed:', error)
    showToast(error.message || '加载AI配置失败', 'error')
  } finally {
    loading.value = false
  }
}

async function saveConfig() {
  saving.value = true
  try {
    const payload = {
      apiKey: form.apiKey,
      baseUrl: form.baseUrl,
      model: form.model,
      timeoutMs: form.timeoutMs
    }

    const res = await saveAiBackendConfig(token.value, payload)
    const data = res.data || {}

    form.apiKey = ''
    meta.hasApiKey = !!data.hasApiKey
    meta.apiKeyMasked = data.apiKeyMasked || ''
    meta.source = data.source || ''
    meta.updatedBy = data.updatedBy || ''
    meta.updatedAt = data.updatedAt || ''

    showToast('AI配置已保存', 'success')
  } catch (error) {
    console.error('Save AI config failed:', error)
    showToast(error.message || '保存AI配置失败', 'error')
  } finally {
    saving.value = false
  }
}

async function testConfig() {
  testing.value = true
  testResult.value = null
  try {
    const payload = {
      apiKey: form.apiKey,
      baseUrl: form.baseUrl,
      model: form.model,
      timeoutMs: form.timeoutMs
    }
    const res = await testAiBackendConfig(token.value, payload)
    testResult.value = res.data || null

    if (testResult.value?.ok) {
      showToast('API端口测试成功', 'success')
    } else {
      showToast(testResult.value?.message || 'API端口测试失败', 'error')
    }
  } catch (error) {
    console.error('Test AI config failed:', error)
    showToast(error.message || 'API端口测试失败', 'error')
    testResult.value = {
      ok: false,
      message: error.message || 'API端口测试失败'
    }
  } finally {
    testing.value = false
  }
}

loadConfig()
</script>

<style scoped>
.ai-config-page {
  gap: 1rem;
}

.config-panel {
  padding: 1rem 1.2rem;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.9rem 1rem;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
}

.field-hint {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: var(--muted-foreground);
  font-size: 0.82rem;
}

.btn-link {
  padding: 0;
  border: none;
  background: transparent;
  color: #2f63c5;
  font-size: 0.82rem;
  text-decoration: underline;
}

.config-meta {
  margin-top: 1rem;
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
  font-size: 0.85rem;
  color: var(--muted-foreground);
}

.actions-row {
  margin-top: 1rem;
  display: flex;
  gap: 0.75rem;
}

.test-result {
  margin-top: 0.9rem;
  border: 1px solid #d9e2f5;
  border-radius: 8px;
  padding: 0.8rem 0.9rem;
  background: #f8fbff;
}

.test-result.fail {
  border-color: #f6c8c8;
  background: #fff8f8;
}

.test-title {
  font-size: 0.9rem;
  font-weight: 600;
  margin-bottom: 0.5rem;
}

.test-result.ok .test-title {
  color: #1f7a1f;
}

.test-result.fail .test-title {
  color: #b42318;
}

.test-grid {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 0.35rem 0.7rem;
  font-size: 0.84rem;
}

@media (max-width: 900px) {
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
