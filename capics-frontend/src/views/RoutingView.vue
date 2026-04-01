<template>
  <div class="page">
    <div class="page-header">
      <h1 class="page-title">工艺路线</h1>
      <p class="page-subtitle">成品BOM定义</p>
    </div>
    <div style="margin-bottom: 2rem;">
      <button class="btn btn-primary" @click="showImportModal">导入工艺路线</button>
    </div>
    <div class="table-wrapper">
      <table>
        <thead>
          <tr>
            <th>成品物料号</th>
            <th>描述</th>
            <th>组件物料号</th>
            <th>生产线</th>
            <th>BOM层级</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(r, index) in routings" :key="index">
            <td>{{ r.productNumber }}</td>
            <td>{{ r.routingDescription }}</td>
            <td>{{ r.componentNumber }}</td>
            <td>{{ r.lineCode }}</td>
            <td>{{ r.bomLevel }}</td>
          </tr>
          <tr v-if="routings.length === 0">
            <td colspan="5" style="text-align: center; color: var(--muted-foreground);">暂无数据</td>
          </tr>
        </tbody>
      </table>
    </div>

    <ImportModal
      :show="showImport"
      type="routing"
      :isImporting="isImporting"
      @close="showImport = false"
      @confirm="handleImport"
    />

    <ConfirmModal
      :show="showConfirmModal"
      :title="confirmTitle"
      :items="confirmItems"
      @confirm="handleConfirmImport"
      @cancel="handleCancelImport"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useAuth } from '@/composables/useAuth'
import { useToast } from '@/composables/useToast'
import { getRoutingsFull, importRoutings, checkRoutingImportDuplicates } from '@/api/routing'
import ImportModal from '@/components/common/ImportModal.vue'
import ConfirmModal from '@/components/common/ConfirmModal.vue'

const { token, currentUser } = useAuth()
const { showToast } = useToast()

const routings = ref([])
const showImport = ref(false)
const isImporting = ref(false)
const showConfirmModal = ref(false)
const confirmTitle = ref('')
const confirmItems = ref([])
const pendingFile = ref(null)

const loadRoutings = async () => {
  try {
    const data = await getRoutingsFull(token.value)
    routings.value = data.data || []
  } catch (err) {
    console.error('Load routings error:', err)
  }
}

const showImportModal = () => {
  showImport.value = true
}

const handleImport = async ({ file }) => {
  if (!file) {
    showToast('请选择文件', 'warning')
    return
  }

  // 检查重复
  try {
    const checkResult = await checkRoutingImportDuplicates(token.value, file)
    console.log('Check result:', checkResult)
    if (checkResult.success === false && checkResult.data && checkResult.data.length > 0) {
      confirmTitle.value = `发现 ${checkResult.data.length} 条已存在的成品物料号`
      confirmItems.value = checkResult.data.map(d => d.productNumber)
      pendingFile.value = file
      showConfirmModal.value = true
      showImport.value = false
      return
    }
  } catch (err) {
    console.error('Check duplicates error:', err)
  }

  await doImport(file)
}

const handleConfirmImport = async () => {
  showConfirmModal.value = false
  if (pendingFile.value) {
    await doImport(pendingFile.value)
    pendingFile.value = null
  }
}

const handleCancelImport = () => {
  showConfirmModal.value = false
  pendingFile.value = null
  showToast('已取消导入', 'warning')
}

const doImport = async (file) => {
  isImporting.value = true
  try {
    const result = await importRoutings(token.value, file, currentUser.value)
    if (result.success) {
      showToast('导入成功: ' + result.message, 'success')
      showImport.value = false
      loadRoutings()
    } else {
      showToast('导入失败: ' + result.message, 'error')
    }
  } catch (err) {
    showToast('导入失败: ' + err.message, 'error')
  } finally {
    isImporting.value = false
  }
}

onMounted(() => {
  loadRoutings()
})
</script>
