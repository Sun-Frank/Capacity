<template>
  <div class="page">
    <div class="page-header">
      <h1 class="page-title">产品主数据</h1>
      <p class="page-subtitle">管理编码族和产品主数据</p>
    </div>

    <BaseTabs
      v-model="productTab"
      :tabs="[
        { label: '编码族', value: 'families' },
        { label: '编码族定线', value: 'family-lines' },
        { label: '产品列表', value: 'products' }
      ]"
    />

    <div v-if="productTab === 'families'">
      <div style="margin-bottom: 2rem; display: flex; gap: 1rem; align-items: center;">
        <input
          type="text"
          v-model="familySearch"
          placeholder="搜索编码族..."
          class="form-input"
          style="width: 200px;"
          @input="handleFamilySearch"
        >
        <button class="btn btn-primary" @click="showImportModal('family')">导入编码族</button>
        <button class="btn" @click="handleDownloadFamilyTemplate">模板下载</button>
        <button class="btn" @click="handleExportFamilies">数据导出</button>
      </div>
      <FamiliesTable :families="families" @edit="handleEditFamily" />
    </div>

    <div v-if="productTab === 'family-lines'">
      <div style="margin-bottom: 2rem; display: flex; gap: 1rem; align-items: center;">
        <input
          type="text"
          v-model="familyLineSearch"
          placeholder="搜索编码族..."
          class="form-input"
          style="width: 200px;"
          @input="handleFamilyLineSearch"
        >
        <button class="btn btn-primary" @click="showImportModal('family-line')">导入编码族定线</button>
        <button class="btn" @click="handleDownloadFamilyLineTemplate">模板下载</button>
        <button class="btn" @click="handleExportFamilyLines">数据导出</button>
      </div>
      <FamilyLinesTable :family-lines="familyLines" @edit="handleEditFamilyLine" />
    </div>

    <div v-if="productTab === 'products'">
      <div style="margin-bottom: 2rem; display: flex; gap: 1rem; align-items: center;">
        <input
          type="text"
          v-model="productSearch"
          placeholder="搜索物料号..."
          class="form-input"
          style="width: 200px;"
          @input="handleProductSearch"
        >
        <button class="btn btn-primary" @click="showImportModal('product')">导入产品</button>
        <button class="btn" @click="handleDownloadProductTemplate">模板下载</button>
        <button class="btn" @click="handleExportProducts">数据导出</button>
      </div>
      <ProductsTable :products="products" @save="handleUpdateProduct" />
    </div>

    <ImportModal
      :show="showImport"
      :type="importType"
      :isImporting="isImporting"
      @close="showImport = false"
      @confirm="handleImport"
    />

    <EditFamilyModal
      :show="showEditFamily"
      :family="editingFamily"
      @close="showEditFamily = false"
      @confirm="handleUpdateFamily"
    />

    <EditFamilyLineModal
      :show="showEditFamilyLine"
      :family-line="editingFamilyLine"
      @close="showEditFamilyLine = false"
      @confirm="handleUpdateFamilyLine"
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
import { ref, onMounted, watch } from 'vue'
import { useAuth } from '@/composables/useAuth'
import { useToast } from '@/composables/useToast'
import { getFamilies, searchFamilies, getProducts, searchProducts, importFamilies, importProducts, updateFamily, updateProduct, checkFamilyImportDuplicates, checkProductImportDuplicates, getFamilyLines, searchFamilyLines, importFamilyLines, checkFamilyLineImportDuplicates, updateFamilyLine, downloadFamilyTemplate, downloadFamilyLineTemplate, downloadProductTemplate } from '@/api/product'
import BaseTabs from '@/components/common/BaseTabs.vue'
import FamiliesTable from '@/components/products/FamiliesTable.vue'
import ProductsTable from '@/components/products/ProductsTable.vue'
import FamilyLinesTable from '@/components/products/FamilyLinesTable.vue'
import ImportModal from '@/components/common/ImportModal.vue'
import EditFamilyModal from '@/components/products/EditFamilyModal.vue'
import EditFamilyLineModal from '@/components/products/EditFamilyLineModal.vue'
import ConfirmModal from '@/components/common/ConfirmModal.vue'
import { downloadCsv } from '@/utils/export'

const { token, currentUser } = useAuth()
const { showToast } = useToast()

const productTab = ref('families')
const families = ref([])
const familySearch = ref('')
const products = ref([])
const productSearch = ref('')
const familyLines = ref([])
const familyLineSearch = ref('')
const showImport = ref(false)
const importType = ref('family')
const isImporting = ref(false)
const showEditFamily = ref(false)
const editingFamily = ref(null)
const showEditFamilyLine = ref(false)
const editingFamilyLine = ref(null)
const showConfirmModal = ref(false)
const confirmTitle = ref('')
const confirmItems = ref([])
const pendingFile = ref(null)

const formatDateTime = (value) => {
  if (!value) return ''
  return String(value).replace('T', ' ').slice(0, 19)
}

const handleExportFamilies = () => {
  const headers = [
    { key: 'familyCode', label: '编码族' },
    { key: 'lineCode', label: '生产线' },
    { key: 'description', label: '描述' },
    { key: 'pf', label: 'PF' },
    { key: 'codingRule', label: '编码规则' },
    { key: 'cycleTime', label: '周期时间(秒)' },
    { key: 'oee', label: 'OEE(%)' },
    { key: 'workerCount', label: '人数' },
    { key: 'updatedBy', label: '创建/修改人' },
    { key: 'updatedAt', label: '创建/修改时间' }
  ]
  const rows = (families.value || []).map(f => ({
    familyCode: f.familyCode || '',
    lineCode: f.lineCode || '',
    description: f.description || '',
    pf: f.pf || '',
    codingRule: f.codingRule || '',
    cycleTime: f.cycleTime ?? '',
    oee: f.oee ?? '',
    workerCount: f.workerCount ?? '',
    updatedBy: f.updatedBy || f.createdBy || '',
    updatedAt: formatDateTime(f.updatedAt || f.createdAt)
  }))
  downloadCsv('产品主数据-编码族.csv', headers, rows)
  showToast('导出成功', 'success')
}

const handleExportFamilyLines = () => {
  const headers = [
    { key: 'familyCode', label: '编码族' },
    { key: 'lineCode', label: '生产线' },
    { key: 'description', label: '描述' },
    { key: 'updatedBy', label: '创建/修改人' },
    { key: 'updatedAt', label: '创建/修改时间' }
  ]
  const rows = (familyLines.value || []).map(f => ({
    familyCode: f.familyCode || '',
    lineCode: f.lineCode || '',
    description: f.description || '',
    updatedBy: f.updatedBy || f.createdBy || '',
    updatedAt: formatDateTime(f.updatedAt || f.createdAt)
  }))
  downloadCsv('产品主数据-编码族定线.csv', headers, rows)
  showToast('导出成功', 'success')
}

const handleExportProducts = () => {
  const headers = [
    { key: 'itemNumber', label: '物料号' },
    { key: 'lineCode', label: '生产线' },
    { key: 'familyCode', label: '编码族' },
    { key: 'pf', label: 'PF' },
    { key: 'description', label: '描述' },
    { key: 'cycleTime', label: 'CT(秒)' },
    { key: 'oee', label: 'OEE(%)' },
    { key: 'workerCount', label: '人数' }
  ]
  const rows = (products.value || []).map(p => ({
    itemNumber: p.itemNumber || '',
    lineCode: p.lineCode || '',
    familyCode: p.familyCode || '',
    pf: p.pf || '',
    description: p.description || '',
    cycleTime: p.cycleTime ?? '',
    oee: p.oee ?? '',
    workerCount: p.workerCount ?? ''
  }))
  downloadCsv('产品主数据-产品列表.csv', headers, rows)
  showToast('导出成功', 'success')
}

const handleDownloadFamilyTemplate = async () => {
  try {
    const blob = await downloadFamilyTemplate(token.value)
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = '编码族导入模板.xlsx'
    document.body.appendChild(a)
    a.click()
    a.remove()
    window.URL.revokeObjectURL(url)
    showToast('模板下载成功', 'success')
  } catch (err) {
    showToast('模板下载失败: ' + (err.message || '未知错误'), 'error')
  }
}
const handleDownloadFamilyLineTemplate = async () => {
  try {
    const blob = await downloadFamilyLineTemplate(token.value)
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = '编码族定线导入模板.xlsx'
    document.body.appendChild(a)
    a.click()
    a.remove()
    window.URL.revokeObjectURL(url)
    showToast('模板下载成功', 'success')
  } catch (err) {
    showToast('模板下载失败: ' + (err.message || '未知错误'), 'error')
  }
}

const handleDownloadProductTemplate = async () => {
  try {
    const blob = await downloadProductTemplate(token.value)
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = '产品导入模板.xlsx'
    document.body.appendChild(a)
    a.click()
    a.remove()
    window.URL.revokeObjectURL(url)
    showToast('模板下载成功', 'success')
  } catch (err) {
    showToast('模板下载失败: ' + (err.message || '未知错误'), 'error')
  }
}

const loadFamilies = async () => {
  try {
    const data = await getFamilies(token.value)
    families.value = data.data || []
  } catch (err) {
    console.error('Load families error:', err)
  }
}

const handleFamilySearch = async () => {
  try {
    const data = await searchFamilies(token.value, familySearch.value)
    families.value = data.data || []
  } catch (err) {
    console.error('Search families error:', err)
  }
}

const handleProductSearch = async () => {
  try {
    const data = await searchProducts(token.value, productSearch.value)
    products.value = data.data || []
  } catch (err) {
    console.error('Search products error:', err)
  }
}

const loadProducts = async () => {
  try {
    const data = await getProducts(token.value)
    products.value = data.data || []
  } catch (err) {
    console.error('Load products error:', err)
  }
}

const loadFamilyLines = async () => {
  try {
    const data = await getFamilyLines(token.value)
    familyLines.value = data.data || []
  } catch (err) {
    console.error('Load family lines error:', err)
  }
}

const handleFamilyLineSearch = async () => {
  try {
    const data = await searchFamilyLines(token.value, familyLineSearch.value)
    familyLines.value = data.data || []
  } catch (err) {
    console.error('Search family lines error:', err)
  }
}

const showImportModal = (type) => {
  importType.value = type
  showImport.value = true
}

const handleImport = async ({ file, fileName }) => {
  if (!file) {
    showToast('请选择文件', 'warning')
    return
  }

  // 检查重复
  try {
    let checkResult
    if (importType.value === 'family') {
      checkResult = await checkFamilyImportDuplicates(token.value, file)
      if (checkResult.success === false && checkResult.data && checkResult.data.length > 0) {
        const duplicates = checkResult.data
        confirmTitle.value = `发现 ${duplicates.length} 条已存在的编码族记录，点击确定将覆盖这些数据`
        confirmItems.value = duplicates.map(d => `${d.familyCode} + ${d.lineCode}`)
        pendingFile.value = file
        showConfirmModal.value = true
        showImport.value = false
        return
      }
    } else if (importType.value === 'product') {
      checkResult = await checkProductImportDuplicates(token.value, file)
      if (checkResult.success === false && checkResult.data && checkResult.data.length > 0) {
        const duplicates = checkResult.data
        confirmTitle.value = `发现 ${duplicates.length} 条已存在的产品记录，点击确定将覆盖这些数据`
        confirmItems.value = duplicates.map(d => `${d.itemNumber} + ${d.lineCode}`)
        pendingFile.value = file
        showConfirmModal.value = true
        showImport.value = false
        return
      }
    } else if (importType.value === 'family-line') {
      checkResult = await checkFamilyLineImportDuplicates(token.value, file)
      if (checkResult.success === false && checkResult.data && checkResult.data.length > 0) {
        const duplicates = checkResult.data
        confirmTitle.value = `发现 ${duplicates.length} 条已存在的编码族定线记录，点击确定将覆盖这些数据`
        confirmItems.value = duplicates.map(d => `${d.familyCode} + ${d.lineCode}`)
        pendingFile.value = file
        showConfirmModal.value = true
        showImport.value = false
        return
      }
    }
  } catch (err) {
    console.error('Check duplicates error:', err)
    // 检查失败也继续尝试导入
  }

  // 直接导入
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
    let result
    if (importType.value === 'family') {
      result = await importFamilies(token.value, file, currentUser.value)
    } else if (importType.value === 'product') {
      result = await importProducts(token.value, file, currentUser.value)
    } else if (importType.value === 'family-line') {
      result = await importFamilyLines(token.value, file, currentUser.value)
    }
    if (result && result.success) {
      showToast('导入成功: ' + result.message, 'success')
      showImport.value = false
      if (importType.value === 'family') {
        loadFamilies()
      } else if (importType.value === 'product') {
        loadProducts()
      } else if (importType.value === 'family-line') {
        loadFamilyLines()
      }
    } else {
      const errorMsg = result?.message || '导入失败，请检查模板字段和数据格式'
      showToast('导入失败: ' + errorMsg, 'error')
    }
  } catch (err) {
    showToast('导入失败: ' + (err?.message || '未知错误'), 'error')
  } finally {
    isImporting.value = false
  }
}

const handleEditFamily = (family) => {
  editingFamily.value = family
  showEditFamily.value = true
}

const handleUpdateFamily = async (formData) => {
  try {
    const result = await updateFamily(
      token.value,
      editingFamily.value.familyCode,
      editingFamily.value.lineCode,
      formData,
      currentUser.value
    )
    if (result && result.success) {
      showToast('更新成功', 'success')
      showEditFamily.value = false
      loadFamilies()
    } else {
      showToast('更新失败: ' + result.message, 'error')
    }
  } catch (err) {
    showToast('更新失败: ' + err.message, 'error')
  }
}

const handleEditFamilyLine = (familyLine) => {
  editingFamilyLine.value = familyLine
  showEditFamilyLine.value = true
}

const handleUpdateFamilyLine = async (formData) => {
  try {
    const result = await updateFamilyLine(
      token.value,
      editingFamilyLine.value.familyCode,
      editingFamilyLine.value.lineCode,
      formData,
      currentUser.value
    )
    if (result && result.success) {
      showToast('更新成功', 'success')
      showEditFamilyLine.value = false
      loadFamilyLines()
    } else {
      showToast('更新失败: ' + result.message, 'error')
    }
  } catch (err) {
    showToast('更新失败: ' + err.message, 'error')
  }
}

const handleUpdateProduct = async ({ itemNumber, lineCode, data, done }) => {
  try {
    const result = await updateProduct(
      token.value,
      itemNumber,
      lineCode,
      data,
      currentUser.value
    )
    if (result && result.success) {
      showToast('更新成功', 'success')
      if (typeof done === 'function') {
        done()
      }
      loadProducts()
    } else {
      showToast('更新失败: ' + (result?.message || '未知错误'), 'error')
    }
  } catch (err) {
    showToast('更新失败: ' + (err?.message || '未知错误'), 'error')
  }
}

onMounted(() => {
  loadFamilies()
  loadProducts()
})

watch(productTab, (newTab) => {
  if (newTab === 'products') {
    loadProducts()
  } else if (newTab === 'family-lines') {
    loadFamilyLines()
  }
})
</script>



