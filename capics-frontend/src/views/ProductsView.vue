<template>
  <div class="page">
    <div class="page-header">
      <h1 class="page-title">产品主数据</h1>
      <p class="page-subtitle">管理编码族、编码族定线、产品列表</p>
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
      <div class="toolbar">
        <input
          v-model="familySearch"
          class="form-input toolbar-search"
          type="text"
          placeholder="搜索编码族..."
          @input="handleFamilySearch"
        />
        <button v-if="canManageMasterData" class="btn btn-primary" @click="showImportModal('family')">导入编码族</button>
        <button v-if="canManageMasterData" class="btn btn-primary" @click="showCreateFamily = true">新增编码族</button>
        <button class="btn" @click="handleDownloadFamilyTemplate">模板下载</button>
        <button class="btn" @click="handleExportFamilies">数据导出</button>
      </div>
      <FamiliesTable :families="families" :can-edit="canManageMasterData" @edit="handleEditFamily" />
    </div>

    <div v-if="productTab === 'family-lines'">
      <div class="toolbar">
        <input
          v-model="familyLineSearch"
          class="form-input toolbar-search"
          type="text"
          placeholder="搜索编码族..."
          @input="handleFamilyLineSearch"
        />
        <button v-if="canManageMasterData" class="btn btn-primary" @click="showImportModal('family-line')">导入编码族定线</button>
        <button v-if="canManageMasterData" class="btn btn-primary" @click="showCreateFamilyLine = true">新增编码族定线</button>
        <button class="btn" @click="handleDownloadFamilyLineTemplate">模板下载</button>
        <button class="btn" @click="handleExportFamilyLines">数据导出</button>
      </div>
      <FamilyLinesTable :family-lines="familyLines" :can-edit="canManageMasterData" @edit="handleEditFamilyLine" />
    </div>

    <div v-if="productTab === 'products'">
      <div class="toolbar">
        <input
          v-model="productSearch"
          class="form-input toolbar-search"
          type="text"
          placeholder="搜索物料号..."
          @input="handleProductSearch"
        />
        <button v-if="canManageMasterData" class="btn btn-primary" @click="showImportModal('product')">导入产品</button>
        <button v-if="canManageMasterData" class="btn btn-primary" @click="showCreateProduct = true">新增产品</button>
        <button class="btn" @click="handleDownloadProductTemplate">模板下载</button>
        <button class="btn" @click="handleExportProducts">数据导出</button>
      </div>
      <ProductsTable :products="products" :can-edit="canManageMasterData" @save="handleUpdateProduct" />
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

    <div v-if="showCreateFamily" class="modal-overlay" @click.self="showCreateFamily = false">
      <div class="modal">
        <div class="modal-header">
          <h3>新增编码族</h3>
          <button class="modal-close" @click="showCreateFamily = false">&times;</button>
        </div>
        <form @submit.prevent="handleCreateFamily">
          <div class="form-group">
            <label>编码族</label>
            <input v-model.trim="createFamilyForm.familyCode" class="form-input" required />
          </div>
          <div class="form-group">
            <label>生产线</label>
            <input v-model.trim="createFamilyForm.lineCode" class="form-input" required />
          </div>
          <div class="form-group">
            <label>描述</label>
            <input v-model.trim="createFamilyForm.description" class="form-input" />
          </div>
          <div class="form-group">
            <label>PF</label>
            <input v-model.trim="createFamilyForm.pf" class="form-input" />
          </div>
          <div class="form-group">
            <label>编码规则</label>
            <input v-model.trim="createFamilyForm.codingRule" class="form-input" />
          </div>
          <div class="form-group">
            <label>周期时间(秒)</label>
            <input v-model.number="createFamilyForm.cycleTime" class="form-input" type="number" step="0.01" />
          </div>
          <div class="form-group">
            <label>OEE(%)</label>
            <input v-model.number="createFamilyForm.oee" class="form-input" type="number" step="0.01" />
          </div>
          <div class="form-group">
            <label>人数</label>
            <input v-model.number="createFamilyForm.workerCount" class="form-input" type="number" />
          </div>
          <div class="modal-footer">
            <button type="button" class="btn" @click="showCreateFamily = false">取消</button>
            <button type="submit" class="btn btn-primary" :disabled="isCreatingFamily">
              {{ isCreatingFamily ? '保存中...' : '保存' }}
            </button>
          </div>
        </form>
      </div>
    </div>

    <div v-if="showCreateFamilyLine" class="modal-overlay" @click.self="showCreateFamilyLine = false">
      <div class="modal">
        <div class="modal-header">
          <h3>新增编码族定线</h3>
          <button class="modal-close" @click="showCreateFamilyLine = false">&times;</button>
        </div>
        <form @submit.prevent="handleCreateFamilyLine">
          <div class="form-group">
            <label>编码族</label>
            <input v-model.trim="createFamilyLineForm.familyCode" class="form-input" required />
          </div>
          <div class="form-group">
            <label>生产线</label>
            <input v-model.trim="createFamilyLineForm.lineCode" class="form-input" required />
          </div>
          <div class="modal-footer">
            <button type="button" class="btn" @click="showCreateFamilyLine = false">取消</button>
            <button type="submit" class="btn btn-primary" :disabled="isCreatingFamilyLine">
              {{ isCreatingFamilyLine ? '保存中...' : '保存' }}
            </button>
          </div>
        </form>
      </div>
    </div>

    <div v-if="showCreateProduct" class="modal-overlay" @click.self="showCreateProduct = false">
      <div class="modal">
        <div class="modal-header">
          <h3>新增产品</h3>
          <button class="modal-close" @click="showCreateProduct = false">&times;</button>
        </div>
        <form @submit.prevent="handleCreateProduct">
          <div class="form-group">
            <label>物料号</label>
            <input v-model.trim="createProductForm.itemNumber" class="form-input" required />
          </div>
          <div class="form-group">
            <label>生产线</label>
            <input v-model.trim="createProductForm.lineCode" class="form-input" required />
          </div>
          <div class="form-group">
            <label>编码族</label>
            <input v-model.trim="createProductForm.familyCode" class="form-input" required />
          </div>
          <div class="form-group">
            <label>描述</label>
            <input v-model.trim="createProductForm.description" class="form-input" />
          </div>
          <div class="form-group">
            <label>CT(秒)</label>
            <input v-model.number="createProductForm.cycleTime" class="form-input" type="number" step="0.01" />
          </div>
          <div class="form-group">
            <label>OEE(%)</label>
            <input v-model.number="createProductForm.oee" class="form-input" type="number" step="0.01" />
          </div>
          <div class="form-group">
            <label>人数</label>
            <input v-model.number="createProductForm.workerCount" class="form-input" type="number" />
          </div>
          <div class="modal-footer">
            <button type="button" class="btn" @click="showCreateProduct = false">取消</button>
            <button type="submit" class="btn btn-primary" :disabled="isCreatingProduct">
              {{ isCreatingProduct ? '保存中...' : '保存' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useAuth } from '@/composables/useAuth'
import { useToast } from '@/composables/useToast'
import {
  checkFamilyImportDuplicates,
  checkFamilyLineImportDuplicates,
  checkProductImportDuplicates,
  createFamily,
  createFamilyLine,
  createProduct,
  downloadFamilyLineTemplate,
  downloadFamilyTemplate,
  downloadProductTemplate,
  getFamilies,
  getFamilyLines,
  getProducts,
  importFamilies,
  importFamilyLines,
  importProducts,
  searchFamilies,
  searchFamilyLines,
  searchProducts,
  updateFamily,
  updateFamilyLine,
  updateProduct
} from '@/api/product'
import BaseTabs from '@/components/common/BaseTabs.vue'
import FamiliesTable from '@/components/products/FamiliesTable.vue'
import FamilyLinesTable from '@/components/products/FamilyLinesTable.vue'
import ProductsTable from '@/components/products/ProductsTable.vue'
import ImportModal from '@/components/common/ImportModal.vue'
import EditFamilyModal from '@/components/products/EditFamilyModal.vue'
import EditFamilyLineModal from '@/components/products/EditFamilyLineModal.vue'
import ConfirmModal from '@/components/common/ConfirmModal.vue'
import { downloadCsv } from '@/utils/export'

const { token, currentUser, hasAnyRole } = useAuth()
const { showToast } = useToast()
const canManageMasterData = computed(() => hasAnyRole(['MASTERDATA', 'ADMIN']))

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
const showConfirmModal = ref(false)
const confirmTitle = ref('')
const confirmItems = ref([])
const pendingFile = ref(null)

const showEditFamily = ref(false)
const editingFamily = ref(null)
const showEditFamilyLine = ref(false)
const editingFamilyLine = ref(null)

const showCreateFamily = ref(false)
const showCreateFamilyLine = ref(false)
const showCreateProduct = ref(false)
const isCreatingFamily = ref(false)
const isCreatingFamilyLine = ref(false)
const isCreatingProduct = ref(false)

const createFamilyForm = ref({
  familyCode: '',
  lineCode: '',
  description: '',
  pf: '',
  codingRule: '',
  cycleTime: null,
  oee: null,
  workerCount: null
})

const createFamilyLineForm = ref({
  familyCode: '',
  lineCode: ''
})

const createProductForm = ref({
  itemNumber: '',
  lineCode: '',
  familyCode: '',
  description: '',
  cycleTime: null,
  oee: null,
  workerCount: null
})

const formatDateTime = (value) => {
  if (!value) return ''
  return String(value).replace('T', ' ').slice(0, 19)
}

const loadFamilies = async () => {
  const data = await getFamilies(token.value)
  families.value = data?.data || []
}

const loadProducts = async () => {
  const data = await getProducts(token.value)
  products.value = data?.data || []
}

const loadFamilyLines = async () => {
  const data = await getFamilyLines(token.value)
  familyLines.value = data?.data || []
}

const handleFamilySearch = async () => {
  const data = await searchFamilies(token.value, familySearch.value)
  families.value = data?.data || []
}

const handleProductSearch = async () => {
  const data = await searchProducts(token.value, productSearch.value)
  products.value = data?.data || []
}

const handleFamilyLineSearch = async () => {
  const data = await searchFamilyLines(token.value, familyLineSearch.value)
  familyLines.value = data?.data || []
}

const showImportModal = (type) => {
  if (!canManageMasterData.value) {
    showToast('当前账号无主数据维护权限', 'warning')
    return
  }
  importType.value = type
  showImport.value = true
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

  try {
    let checkResult = null
    if (importType.value === 'family') {
      checkResult = await checkFamilyImportDuplicates(token.value, file)
    } else if (importType.value === 'product') {
      checkResult = await checkProductImportDuplicates(token.value, file)
    } else if (importType.value === 'family-line') {
      checkResult = await checkFamilyLineImportDuplicates(token.value, file)
    }

    if (checkResult?.success === false && Array.isArray(checkResult.data) && checkResult.data.length > 0) {
      confirmTitle.value = `发现 ${checkResult.data.length} 条已存在记录，点击确定继续导入覆盖`
      confirmItems.value = checkResult.data.map((d) => `${d.familyCode || d.itemNumber} + ${d.lineCode || ''}`)
      pendingFile.value = file
      showImport.value = false
      showConfirmModal.value = true
      return
    }
  } catch (err) {
    console.error('Check duplicates failed:', err)
  }

  await doImport(file)
}

const handleConfirmImport = async () => {
  showConfirmModal.value = false
  if (!pendingFile.value) return
  await doImport(pendingFile.value)
  pendingFile.value = null
}

const handleCancelImport = () => {
  showConfirmModal.value = false
  pendingFile.value = null
  showToast('已取消导入', 'warning')
}

const doImport = async (file) => {
  isImporting.value = true
  try {
    let result = null
    if (importType.value === 'family') {
      result = await importFamilies(token.value, file, currentUser.value)
    } else if (importType.value === 'product') {
      result = await importProducts(token.value, file, currentUser.value)
    } else if (importType.value === 'family-line') {
      result = await importFamilyLines(token.value, file, currentUser.value)
    }

    if (!result?.success) {
      showToast(`导入失败: ${result?.message || '未知错误'}`, 'error')
      return
    }

    showToast(`导入成功: ${result.message || ''}`, 'success')
    showImport.value = false

    if (importType.value === 'family') await loadFamilies()
    if (importType.value === 'product') await loadProducts()
    if (importType.value === 'family-line') await loadFamilyLines()
  } catch (err) {
    showToast(`导入失败: ${err?.message || '未知错误'}`, 'error')
  } finally {
    isImporting.value = false
  }
}

const handleEditFamily = (family) => {
  editingFamily.value = family
  showEditFamily.value = true
}

const handleUpdateFamily = async (formData) => {
  if (!canManageMasterData.value) {
    showToast('当前账号无主数据维护权限', 'warning')
    return
  }
  const result = await updateFamily(
    token.value,
    editingFamily.value.familyCode,
    editingFamily.value.lineCode,
    formData,
    currentUser.value
  )
  if (!result?.success) {
    showToast(`更新失败: ${result?.message || '未知错误'}`, 'error')
    return
  }
  showToast('更新成功', 'success')
  showEditFamily.value = false
  await loadFamilies()
}

const handleEditFamilyLine = (familyLine) => {
  editingFamilyLine.value = familyLine
  showEditFamilyLine.value = true
}

const handleUpdateFamilyLine = async (formData) => {
  if (!canManageMasterData.value) {
    showToast('当前账号无主数据维护权限', 'warning')
    return
  }
  const result = await updateFamilyLine(
    token.value,
    editingFamilyLine.value.familyCode,
    editingFamilyLine.value.lineCode,
    formData,
    currentUser.value
  )
  if (!result?.success) {
    showToast(`更新失败: ${result?.message || '未知错误'}`, 'error')
    return
  }
  showToast('更新成功', 'success')
  showEditFamilyLine.value = false
  await loadFamilyLines()
}

const handleUpdateProduct = async ({ itemNumber, lineCode, data, done }) => {
  if (!canManageMasterData.value) {
    showToast('当前账号无主数据维护权限', 'warning')
    return
  }
  const result = await updateProduct(token.value, itemNumber, lineCode, data, currentUser.value)
  if (!result?.success) {
    showToast(`更新失败: ${result?.message || '未知错误'}`, 'error')
    return
  }
  showToast('更新成功', 'success')
  if (typeof done === 'function') done()
  await loadProducts()
}

const resetCreateFamily = () => {
  createFamilyForm.value = {
    familyCode: '',
    lineCode: '',
    description: '',
    pf: '',
    codingRule: '',
    cycleTime: null,
    oee: null,
    workerCount: null
  }
}

const resetCreateFamilyLine = () => {
  createFamilyLineForm.value = { familyCode: '', lineCode: '' }
}

const resetCreateProduct = () => {
  createProductForm.value = {
    itemNumber: '',
    lineCode: '',
    familyCode: '',
    description: '',
    cycleTime: null,
    oee: null,
    workerCount: null
  }
}

const handleCreateFamily = async () => {
  if (!canManageMasterData.value) {
    showToast('当前账号无主数据维护权限', 'warning')
    return
  }
  if (!createFamilyForm.value.familyCode || !createFamilyForm.value.lineCode) {
    showToast('编码族和生产线不能为空', 'warning')
    return
  }
  isCreatingFamily.value = true
  try {
    const result = await createFamily(token.value, createFamilyForm.value, currentUser.value)
    if (!result?.success) {
      showToast(`新增失败: ${result?.message || '未知错误'}`, 'error')
      return
    }
    showToast('新增成功', 'success')
    showCreateFamily.value = false
    resetCreateFamily()
    await loadFamilies()
  } catch (err) {
    showToast(`新增失败: ${err?.message || '未知错误'}`, 'error')
  } finally {
    isCreatingFamily.value = false
  }
}

const handleCreateFamilyLine = async () => {
  if (!canManageMasterData.value) {
    showToast('当前账号无主数据维护权限', 'warning')
    return
  }
  if (!createFamilyLineForm.value.familyCode || !createFamilyLineForm.value.lineCode) {
    showToast('编码族和生产线不能为空', 'warning')
    return
  }
  isCreatingFamilyLine.value = true
  try {
    const result = await createFamilyLine(token.value, createFamilyLineForm.value, currentUser.value)
    if (!result?.success) {
      showToast(`新增失败: ${result?.message || '未知错误'}`, 'error')
      return
    }
    showToast('新增成功', 'success')
    showCreateFamilyLine.value = false
    resetCreateFamilyLine()
    await loadFamilyLines()
  } catch (err) {
    showToast(`新增失败: ${err?.message || '未知错误'}`, 'error')
  } finally {
    isCreatingFamilyLine.value = false
  }
}

const handleCreateProduct = async () => {
  if (!canManageMasterData.value) {
    showToast('当前账号无主数据维护权限', 'warning')
    return
  }
  if (!createProductForm.value.itemNumber || !createProductForm.value.lineCode || !createProductForm.value.familyCode) {
    showToast('物料号、生产线、编码族不能为空', 'warning')
    return
  }
  isCreatingProduct.value = true
  try {
    const result = await createProduct(token.value, createProductForm.value, currentUser.value)
    if (!result?.success) {
      showToast(`新增失败: ${result?.message || '未知错误'}`, 'error')
      return
    }
    showToast('新增成功', 'success')
    showCreateProduct.value = false
    resetCreateProduct()
    await loadProducts()
  } catch (err) {
    showToast(`新增失败: ${err?.message || '未知错误'}`, 'error')
  } finally {
    isCreatingProduct.value = false
  }
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
  const rows = (families.value || []).map((f) => ({
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
  downloadCsv('产品主数据_编码族.csv', headers, rows)
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
  const rows = (familyLines.value || []).map((f) => ({
    familyCode: f.familyCode || '',
    lineCode: f.lineCode || '',
    description: f.description || '',
    updatedBy: f.updatedBy || f.createdBy || '',
    updatedAt: formatDateTime(f.updatedAt || f.createdAt)
  }))
  downloadCsv('产品主数据_编码族定线.csv', headers, rows)
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
  const rows = (products.value || []).map((p) => ({
    itemNumber: p.itemNumber || '',
    lineCode: p.lineCode || '',
    familyCode: p.familyCode || '',
    pf: p.pf || '',
    description: p.description || '',
    cycleTime: p.cycleTime ?? '',
    oee: p.oee ?? '',
    workerCount: p.workerCount ?? ''
  }))
  downloadCsv('产品主数据_产品列表.csv', headers, rows)
  showToast('导出成功', 'success')
}

const downloadTemplate = async (downloader, fileName) => {
  const blob = await downloader(token.value)
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = fileName
  document.body.appendChild(a)
  a.click()
  a.remove()
  window.URL.revokeObjectURL(url)
}

const handleDownloadFamilyTemplate = async () => {
  try {
    await downloadTemplate(downloadFamilyTemplate, '编码族导入模板.xlsx')
    showToast('模板下载成功', 'success')
  } catch (err) {
    showToast(`模板下载失败: ${err?.message || '未知错误'}`, 'error')
  }
}

const handleDownloadFamilyLineTemplate = async () => {
  try {
    await downloadTemplate(downloadFamilyLineTemplate, '编码族定线导入模板.xlsx')
    showToast('模板下载成功', 'success')
  } catch (err) {
    showToast(`模板下载失败: ${err?.message || '未知错误'}`, 'error')
  }
}

const handleDownloadProductTemplate = async () => {
  try {
    await downloadTemplate(downloadProductTemplate, '产品导入模板.xlsx')
    showToast('模板下载成功', 'success')
  } catch (err) {
    showToast(`模板下载失败: ${err?.message || '未知错误'}`, 'error')
  }
}

onMounted(async () => {
  await loadFamilies()
  await loadProducts()
})

watch(productTab, async (newTab) => {
  if (newTab === 'products') await loadProducts()
  if (newTab === 'family-lines') await loadFamilyLines()
  if (newTab === 'families') await loadFamilies()
})
</script>

<style scoped>
.toolbar {
  margin-bottom: 2rem;
  display: flex;
  gap: 1rem;
  align-items: center;
  flex-wrap: wrap;
}

.toolbar-search {
  width: 220px;
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal {
  background: #fff;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  width: 90%;
  max-width: 520px;
  max-height: 90vh;
  overflow-y: auto;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem 1.5rem;
  border-bottom: 1px solid var(--border-color);
}

.modal-close {
  background: none;
  border: none;
  font-size: 1.5rem;
  cursor: pointer;
  color: var(--muted-foreground);
}

form {
  padding: 1.5rem;
}

.form-group {
  margin-bottom: 1rem;
}

.form-group label {
  display: block;
  margin-bottom: 0.5rem;
  font-weight: 500;
}

.form-group input {
  width: 100%;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 1rem;
  margin-top: 1.5rem;
  padding-top: 1rem;
  border-top: 1px solid var(--border-color);
}
</style>
