<template>
  <div class="page">
    <section class="config-card wms-config-card">
      <div class="section-heading">
        <h2 class="section-title">WMS BOM配置</h2>
        <p class="section-subtitle">维护从公司 WMS 系统抓取 BOM 结构所需的登录参数。</p>
      </div>

      <div class="form-grid">
        <label class="form-field">
          <span class="form-label">WMS系统网址</span>
          <input v-model.trim="form.loginUrl" class="form-input" placeholder="http://10.119.144.90:2023/Index.aspx" />
        </label>
        <label class="form-field">
          <span class="form-label">用户名</span>
          <input v-model.trim="form.username" class="form-input" />
        </label>
        <label class="form-field full">
          <span class="form-label">密码</span>
          <input v-model="form.password" type="password" class="form-input" placeholder="留空则保留服务器已有密码" />
          <span class="field-hint">当前状态：{{ passwordStatus }}</span>
        </label>
      </div>

      <div class="config-meta" v-if="meta.updatedAt || meta.updatedBy">
        <span v-if="meta.updatedBy">最后修改人：{{ meta.updatedBy }}</span>
        <span v-if="meta.updatedAt">最后修改时间：{{ meta.updatedAt }}</span>
      </div>

      <div class="toolbar form-actions">
        <button class="btn" :disabled="loading || saving || testing" @click="load">重新加载</button>
        <button class="btn" :disabled="loading || saving || testing" @click="test">端口测试</button>
        <button class="btn btn-primary" :disabled="saving || testing" @click="save">保存配置</button>
      </div>

      <div v-if="testResult" class="test-result" :class="testResult.ok ? 'ok' : 'fail'">
        <strong>{{ testResult.ok ? '连接测试成功' : '连接测试失败' }}</strong>
        <span>HTTP：{{ testResult.status ?? '--' }}</span>
        <span>耗时：{{ testResult.latencyMs ?? '--' }} ms</span>
        <span>{{ testResult.message || '--' }}</span>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useAuth } from '@/composables/useAuth'
import { useToast } from '@/composables/useToast'
import { getWmsBomConfig, saveWmsBomConfig, testWmsBomConfig } from '@/api/wmsBomConfig'

const { token } = useAuth()
const { showToast } = useToast()

const loading = ref(false)
const saving = ref(false)
const testing = ref(false)
const testResult = ref(null)
const form = reactive({ loginUrl: '', username: '', password: '' })
const meta = reactive({ passwordConfigured: false, updatedBy: '', updatedAt: '' })

const passwordStatus = computed(() => (meta.passwordConfigured ? '已配置' : '未配置'))

async function load() {
  loading.value = true
  try {
    const res = await getWmsBomConfig(token.value)
    const data = res.data || {}
    form.loginUrl = data.loginUrl || ''
    form.username = data.username || ''
    form.password = ''
    meta.passwordConfigured = !!data.passwordConfigured
    meta.updatedBy = data.updatedBy || ''
    meta.updatedAt = data.updatedAt || ''
  } catch (error) {
    showToast(error.message || '加载WMS配置失败', 'error')
  } finally {
    loading.value = false
  }
}

async function save() {
  if (!form.loginUrl || !form.username) {
    showToast('WMS系统网址和用户名不能为空', 'warning')
    return
  }
  saving.value = true
  try {
    const res = await saveWmsBomConfig(token.value, form)
    const data = res.data || {}
    form.password = ''
    meta.passwordConfigured = !!data.passwordConfigured
    meta.updatedBy = data.updatedBy || ''
    meta.updatedAt = data.updatedAt || ''
    showToast('WMS配置已保存', 'success')
  } catch (error) {
    showToast(error.message || '保存WMS配置失败', 'error')
  } finally {
    saving.value = false
  }
}

async function test() {
  testing.value = true
  testResult.value = null
  try {
    const res = await testWmsBomConfig(token.value, form)
    testResult.value = res.data || null
    showToast(testResult.value?.message || '测试完成', testResult.value?.ok ? 'success' : 'error')
  } catch (error) {
    testResult.value = { ok: false, message: error.message }
    showToast(error.message || 'WMS端口测试失败', 'error')
  } finally {
    testing.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.wms-config-card {
  max-width: 820px;
}

.section-heading {
  margin-bottom: var(--space-4);
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

.form-field.full {
  grid-column: 1 / -1;
}

.field-hint,
.config-meta {
  margin-top: var(--space-2);
  color: var(--muted-foreground);
  font-size: 0.85rem;
}

.config-meta {
  display: flex;
  gap: var(--space-4);
}

.test-result {
  margin-top: var(--space-4);
  display: grid;
  gap: var(--space-2);
  border: 1px solid #c9d8f2;
  border-radius: var(--radius-md);
  padding: var(--space-3);
  background: #f8fbff;
}

.test-result.fail {
  border-color: #f3b6b6;
  background: #fff8f8;
}

.test-result.ok strong {
  color: #247a35;
}

.test-result.fail strong {
  color: #b42318;
}

@media (max-width: 768px) {
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
