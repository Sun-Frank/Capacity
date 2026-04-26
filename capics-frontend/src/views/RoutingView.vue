<template>
  <div class="page">
    <div class="page-header">
      <h1 class="page-title">工艺路线</h1>
      <p class="page-subtitle">成品BOM定义</p>
    </div>
    <div style="margin-bottom: 2rem; display: flex; gap: 1rem; align-items: center;">
      <input
        type="text"
        v-model="searchKeyword"
        placeholder="搜索成品物料号..."
        class="form-input"
        style="width: 200px;"
      >
      <button v-if="canManageMasterData" class="btn btn-primary" @click="showImportModal">导入工艺路线</button>
      <button class="btn" @click="handleDownloadRoutingTemplate">模板下载</button>
      <button class="btn" @click="handleExportRoutings">数据导出</button>
    </div>
    <div class="table-wrapper">
      <table>
        <thead>
          <tr>
            <th style="width: 40px;"></th>
            <th>成品物料号</th>
            <th>描述</th>
            <th>组件数</th>
          </tr>
        </thead>
        <tbody>
          <template v-for="group in groupedRoutings" :key="group.productNumber">
            <!-- 成品行（可展开） -->
            <tr class="group-header" @click="toggleGroup(group.productNumber)">
              <td class="expand-cell">
                <span class="expand-icon" :class="{ expanded: expandedGroups.has(group.productNumber) }">
                  ▶
                </span>
              </td>
              <td>{{ group.productNumber }}</td>
              <td>{{ group.description }}</td>
              <td>{{ group.items.length }}</td>
            </tr>
            <!-- 组件行（可展开） -->
            <template v-if="expandedGroups.has(group.productNumber)">
              <!-- 子表头 -->
              <tr class="child-header">
                <td></td>
                <td class="child-cell">组件物料号</td>
                <td>生产线</td>
                <td>BOM层级</td>
              </tr>
              <!-- 子数据 -->
              <tr v-for="(item, idx) in group.items" :key="idx" class="child-row">
                <td></td>
                <td class="child-cell">{{ item.componentNumber }}</td>
                <td>{{ item.lineCode }}</td>
                <td>{{ item.bomLevel }}</td>
              </tr>
            </template>
          </template>
          <tr v-if="groupedRoutings.length === 0">
            <td colspan="4" style="text-align: center; color: var(--muted-foreground);">暂无数据</td>
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
import { ref, computed, onMounted } from 'vue'
import { useAuth } from '@/composables/useAuth'
import { useToast } from '@/composables/useToast'
import { getRoutingsFull, importRoutings, checkRoutingImportDuplicates, downloadRoutingTemplate } from '@/api/routing'
import { downloadCsv } from '@/utils/export'
import ImportModal from '@/components/common/ImportModal.vue'
import ConfirmModal from '@/components/common/ConfirmModal.vue'

const { token, currentUser, hasAnyRole } = useAuth()
const { showToast } = useToast()
const canManageMasterData = computed(() => hasAnyRole(['MASTERDATA', 'ADMIN']))

const routings = ref([])
const expandedGroups = ref(new Set())
const searchKeyword = ref('')
const showImport = ref(false)
const isImporting = ref(false)
const showConfirmModal = ref(false)
const confirmTitle = ref('')
const confirmItems = ref([])
const pendingFile = ref(null)

// 按成品物料号分组
const groupedRoutings = computed(() => {
  const keyword = searchKeyword.value.toLowerCase().trim()
  const filtered = keyword
    ? routings.value.filter(r => r.productNumber.toLowerCase().includes(keyword))
    : routings.value
  const groups = {}
  filtered.forEach(item => {
    if (!groups[item.productNumber]) {
      groups[item.productNumber] = {
        productNumber: item.productNumber,
        description: item.routingDescription,
        items: []
      }
    }
    groups[item.productNumber].items.push(item)
  })
  // 按BOM层级降序排序
  Object.values(groups).forEach(g => {
    g.items.sort((a, b) => b.bomLevel - a.bomLevel)
  })
  return Object.values(groups)
})

// 展开/收起
const toggleGroup = (productNumber) => {
  if (expandedGroups.value.has(productNumber)) {
    expandedGroups.value.delete(productNumber)
  } else {
    expandedGroups.value.add(productNumber)
  }
}

const loadRoutings = async () => {
  try {
    const data = await getRoutingsFull(token.value)
    routings.value = data.data || []
  } catch (err) {
    console.error('Load routings error:', err)
  }
}

const showImportModal = () => {
  if (!canManageMasterData.value) {
    showToast('当前账号无主数据维护权限', 'warning')
    return
  }
  showImport.value = true
}
const handleDownloadRoutingTemplate = async () => {
  try {
    const blob = await downloadRoutingTemplate(token.value)
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = '工艺路线导入模板.xlsx'
    document.body.appendChild(a)
    a.click()
    a.remove()
    window.URL.revokeObjectURL(url)
    showToast('模板下载成功', 'success')
  } catch (err) {
    showToast('模板下载失败: ' + (err?.message || '未知错误'), 'error')
  }
}

const handleExportRoutings = () => {
  const headers = [
    { key: 'productNumber', label: '成品物料号' },
    { key: 'routingDescription', label: '描述' },
    { key: 'componentNumber', label: '组件物料号' },
    { key: 'lineCode', label: '生产线' },
    { key: 'bomLevel', label: 'BOM层级' }
  ]
  const rows = (routings.value || []).map(r => ({
    productNumber: r.productNumber || '',
    routingDescription: r.routingDescription || '',
    componentNumber: r.componentNumber || '',
    lineCode: r.lineCode || '',
    bomLevel: r.bomLevel ?? ''
  }))
  downloadCsv('工艺路线.csv', headers, rows)
  showToast('导出成功', 'success')
}

const handleImport = async ({ file }) => {
  if (!canManageMasterData.value) {
    showToast('当前账号无主数据维护权限', 'warning')
    return
  }
  if (!file) {
    showToast('请选择文件', 'warning')
    return
  }

  // 检查重复
  try {
    const checkResult = await checkRoutingImportDuplicates(token.value, file)
    console.log('Check result:', checkResult)
    if (checkResult.success === false && checkResult.data && checkResult.data.length > 0) {
      confirmTitle.value = `发现 ${checkResult.data.length} 条已存在的工艺路线，点击确定将覆盖这些数据`
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
    await doImport(pendingFile.value, true) // overwrite=true
    pendingFile.value = null
  }
}

const handleCancelImport = () => {
  showConfirmModal.value = false
  pendingFile.value = null
  showToast('已取消导入', 'warning')
}

const doImport = async (file, overwrite = false) => {
  isImporting.value = true
  try {
    const result = await importRoutings(token.value, file, currentUser.value, overwrite)
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

<style scoped>
.group-header {
  cursor: pointer;
  background: var(--muted) !important;
}

.group-header:hover {
  background: var(--muted-background) !important;
}

.expand-cell {
  width: 40px;
  text-align: center;
}

.expand-icon {
  display: inline-block;
  transition: transform 0.2s;
  font-size: 0.7rem;
  color: var(--muted-foreground);
}

.expand-icon.expanded {
  transform: rotate(90deg);
}

.child-row {
  background: white;
}

.child-row:hover {
  background: var(--muted-background);
}

.child-header {
  background: var(--muted) !important;
  font-size: 0.7rem;
  font-weight: 600;
}

.child-header:hover {
  background: var(--muted) !important;
}

.child-cell {
  padding-left: 2rem !important;
  color: var(--muted-foreground);
}
</style>


