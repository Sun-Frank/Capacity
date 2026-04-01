<template>
  <div class="page">
    <div class="page-header">
      <h1 class="page-title">生产线实时配置</h1>
      <p class="page-subtitle">班产量计算结果</p>
    </div>
    <div style="margin-bottom: 2rem; display: flex; gap: 1rem; align-items: center;">
      <BaseSelect
        v-model="selectedVersion"
        :options="versions.map(v => ({ value: v, label: v }))"
        placeholder="选择版本"
      />
      <button class="btn btn-primary" @click="calculateRealtime">计算</button>
    </div>
    <div class="table-wrapper">
      <table>
        <thead>
          <tr>
            <th>生产线</th>
            <th>成品</th>
            <th>组件</th>
            <th>班产量</th>
            <th>CT(秒)</th>
            <th>OEE</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="r in realtimeData" :key="r.id">
            <td>{{ r.lineCode }}</td>
            <td>{{ r.itemNumber }}</td>
            <td>{{ r.componentNumber }}</td>
            <td>{{ r.shiftOutput }}</td>
            <td>{{ r.ct }}</td>
            <td>{{ formatOee(r.oee) }}</td>
          </tr>
          <tr v-if="realtimeData.length === 0">
            <td colspan="6" style="text-align: center; color: var(--muted-foreground);">暂无数据，请先选择版本并点击计算</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useAuth } from '@/composables/useAuth'
import { useVersion } from '@/composables/useVersion'
import { useToast } from '@/composables/useToast'
import { calculateRealtime as calcRealtime, getRealtimeList } from '@/api/realtime'
import BaseSelect from '@/components/common/BaseSelect.vue'

const { token } = useAuth()
const { versions, selectedVersion, loadVersions } = useVersion(token)
const { showToast } = useToast()

const realtimeData = ref([])

const calculateRealtime = async () => {
  if (!selectedVersion.value) {
    showToast('请先选择版本', 'warning')
    return
  }
  try {
    await calcRealtime(token.value, selectedVersion.value)
    const data = await getRealtimeList(token.value, selectedVersion.value)
    realtimeData.value = data.data || []
  } catch (err) {
    showToast('计算失败: ' + err.message, 'error')
  }
}

onMounted(async () => {
  await loadVersions()
})

const formatOee = (value) => {
  if (value === null || value === undefined) return '-'
  const num = parseFloat(value)
  if (isNaN(num)) return '-'
  return num.toFixed(2) + '%'
}
</script>
