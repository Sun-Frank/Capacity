<template>
  <div class="page">
    <div class="tabs compact-tabs">
      <button class="tab" :class="{ active: activeTab === 'products' }" @click="activeTab = 'products'">产品主数据</button>
      <button class="tab" :class="{ active: activeTab === 'projects' }" @click="activeTab = 'projects'">项目主数据</button>
    </div>

    <div v-if="activeTab === 'products'" class="table-wrapper">
      <div class="toolbar sticky-toolbar">
        <input v-model.trim="productKeyword" class="form-input search-input" placeholder="搜索成品料号 / 产品描述" @keyup.enter="loadProducts" />
        <button class="btn" @click="loadProducts">查询</button>
        <button v-if="canManageMasterData" class="btn btn-primary" @click="startProductAdd">新增</button>
        <button v-if="canManageMasterData" class="btn btn-primary" @click="showProductImport = true">Excel导入</button>
        <button class="btn" @click="downloadProductTpl">模板下载</button>
        <button class="btn" @click="exportProducts">数据导出</button>
      </div>

      <table>
        <thead>
          <tr>
            <th>成品料号</th>
            <th>产品描述</th>
            <th>项目主数据校验</th>
            <th v-if="canManageMasterData">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="editingProductKey === '__new__'">
            <td><input v-model.trim="productForm.itemNumber" class="form-input" /></td>
            <td><input v-model.trim="productForm.description" class="form-input" /></td>
            <td>-</td>
            <td><button class="btn btn-small btn-primary" @click="saveProduct">保存</button><button class="btn btn-small" @click="cancelProductEdit">取消</button></td>
          </tr>
          <tr v-for="row in products" :key="`${row.itemNumber}-${row.lineCode}`">
            <template v-if="editingProductKey === `${row.itemNumber}-${row.lineCode}`">
              <td>{{ row.itemNumber }}</td>
              <td><input v-model.trim="productForm.description" class="form-input" /></td>
              <td>-</td>
              <td><button class="btn btn-small btn-primary" @click="saveProduct">保存</button><button class="btn btn-small" @click="cancelProductEdit">取消</button></td>
            </template>
            <template v-else>
              <td>{{ row.itemNumber }}</td>
              <td :class="{ 'missing-description': row.description && row.descriptionExistsInProjectMaster === false }">
                {{ row.description || '-' }}
                <span v-if="row.description && row.descriptionExistsInProjectMaster === false" class="warn-text">未在项目主数据中维护</span>
              </td>
              <td>{{ row.descriptionExistsInProjectMaster ? '已匹配' : '未匹配' }}</td>
              <td v-if="canManageMasterData"><button class="btn btn-small" @click="startProductEdit(row)">编辑</button></td>
            </template>
          </tr>
          <tr v-if="products.length === 0"><td :colspan="canManageMasterData ? 4 : 3" class="empty-cell">暂无数据</td></tr>
        </tbody>
      </table>
    </div>

    <div v-else class="table-wrapper">
      <div class="toolbar sticky-toolbar">
        <input v-model.trim="projectKeyword" class="form-input search-input" placeholder="搜索客户 / 平台 / 描述 / BWS" @keyup.enter="loadProjects" />
        <button class="btn" @click="loadProjects">查询</button>
        <button v-if="canManageMasterData" class="btn btn-primary" @click="startProjectAdd">新增</button>
        <button v-if="canManageMasterData" class="btn btn-primary" @click="showProjectImport = true">Excel导入</button>
        <button class="btn" @click="downloadProjectTpl">模板下载</button>
        <button class="btn" @click="exportProjects">数据导出</button>
      </div>

      <table>
        <thead>
          <tr>
            <th>客户</th><th>产品平台</th><th>车型配置</th><th>产品描述</th><th>BWS</th><th>版本</th><th v-if="canManageMasterData">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="editingProjectId === '__new__'">
            <td><input v-model.trim="projectForm.customer" class="form-input" /></td>
            <td><input v-model.trim="projectForm.productPlatform" class="form-input" /></td>
            <td><input v-model.trim="projectForm.vehicleConfig" class="form-input" /></td>
            <td><input v-model.trim="projectForm.productDescription" class="form-input" /></td>
            <td><input v-model.trim="projectForm.bws" class="form-input" /></td>
            <td><input v-model.trim="projectForm.version" class="form-input" /></td>
            <td><button class="btn btn-small btn-primary" @click="saveProject">保存</button><button class="btn btn-small" @click="cancelProjectEdit">取消</button></td>
          </tr>
          <tr v-for="row in projects" :key="row.id">
            <template v-if="editingProjectId === row.id">
              <td><input v-model.trim="projectForm.customer" class="form-input" /></td>
              <td><input v-model.trim="projectForm.productPlatform" class="form-input" /></td>
              <td><input v-model.trim="projectForm.vehicleConfig" class="form-input" /></td>
              <td><input v-model.trim="projectForm.productDescription" class="form-input" /></td>
              <td><input v-model.trim="projectForm.bws" class="form-input" /></td>
              <td><input v-model.trim="projectForm.version" class="form-input" /></td>
              <td><button class="btn btn-small btn-primary" @click="saveProject">保存</button><button class="btn btn-small" @click="cancelProjectEdit">取消</button></td>
            </template>
            <template v-else>
              <td>{{ row.customer || '-' }}</td><td>{{ row.productPlatform || '-' }}</td><td>{{ row.vehicleConfig || '-' }}</td><td>{{ row.productDescription }}</td><td>{{ row.bws || '-' }}</td><td>{{ row.version || '-' }}</td>
              <td v-if="canManageMasterData"><button class="btn btn-small" @click="startProjectEdit(row)">编辑</button></td>
            </template>
          </tr>
          <tr v-if="projects.length === 0"><td :colspan="canManageMasterData ? 7 : 6" class="empty-cell">暂无数据</td></tr>
        </tbody>
      </table>
    </div>

    <ImportModal :show="showProductImport" type="product" :isImporting="isImporting" @close="showProductImport = false" @confirm="handleProductImport" />
    <ImportModal :show="showProjectImport" type="project-master" :isImporting="isImporting" @close="showProjectImport = false" @confirm="handleProjectImport" />
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useAuth } from '@/composables/useAuth'
import { useToast } from '@/composables/useToast'
import ImportModal from '@/components/common/ImportModal.vue'
import { createProduct, downloadProductTemplate, getProducts, importProducts, searchProducts, updateProduct } from '@/api/product'
import { createProjectMaster, downloadProjectMasterTemplate, getProjectMasters, importProjectMasters, updateProjectMaster } from '@/api/projectMaster'
import { downloadCsv } from '@/utils/export'

const { token, currentUser, hasAnyRole } = useAuth()
const { showToast } = useToast()
const canManageMasterData = computed(() => hasAnyRole(['MASTERDATA', 'ADMIN']))

const activeTab = ref('products')
const products = ref([])
const projects = ref([])
const productKeyword = ref('')
const projectKeyword = ref('')
const showProductImport = ref(false)
const showProjectImport = ref(false)
const isImporting = ref(false)
const editingProductKey = ref(null)
const editingProjectId = ref(null)
const productForm = ref({ itemNumber: '', lineCode: 'MASTER', description: '' })
const projectForm = ref({ customer: '', productPlatform: '', vehicleConfig: '', productDescription: '', bws: '', version: '' })

const loadProducts = async () => {
  const res = productKeyword.value ? await searchProducts(token.value, productKeyword.value) : await getProducts(token.value)
  products.value = res?.data || []
}
const loadProjects = async () => {
  const res = await getProjectMasters(token.value, projectKeyword.value)
  projects.value = res?.data || []
}

const startProductAdd = () => { editingProductKey.value = '__new__'; productForm.value = { itemNumber: '', lineCode: 'MASTER', description: '' } }
const startProductEdit = (row) => { editingProductKey.value = `${row.itemNumber}-${row.lineCode}`; productForm.value = { ...row } }
const cancelProductEdit = () => { editingProductKey.value = null }
const saveProduct = async () => {
  if (!productForm.value.itemNumber || !productForm.value.description) return showToast('成品料号和产品描述不能为空', 'warning')
  const operator = currentUser.value || 'system'
  const data = { ...productForm.value, lineCode: productForm.value.lineCode || 'MASTER' }
  const res = editingProductKey.value === '__new__'
    ? await createProduct(token.value, data, operator)
    : await updateProduct(token.value, data.itemNumber, data.lineCode || 'MASTER', data, operator)
  showToast(res?.message || '保存成功', res?.success === false ? 'error' : 'success')
  editingProductKey.value = null
  await loadProducts()
}

const startProjectAdd = () => { editingProjectId.value = '__new__'; projectForm.value = { customer: '', productPlatform: '', vehicleConfig: '', productDescription: '', bws: '', version: '' } }
const startProjectEdit = (row) => { editingProjectId.value = row.id; projectForm.value = { ...row } }
const cancelProjectEdit = () => { editingProjectId.value = null }
const saveProject = async () => {
  if (!projectForm.value.productDescription) return showToast('产品描述不能为空', 'warning')
  const res = editingProjectId.value === '__new__'
    ? await createProjectMaster(token.value, projectForm.value)
    : await updateProjectMaster(token.value, editingProjectId.value, projectForm.value)
  showToast(res?.message || '保存成功', res?.success === false ? 'error' : 'success')
  editingProjectId.value = null
  await loadProjects()
  await loadProducts()
}

const handleProductImport = async ({ file }) => {
  if (!file) return showToast('请选择文件', 'warning')
  isImporting.value = true
  try { const res = await importProducts(token.value, file, currentUser.value || 'system'); showToast(res?.message || '导入成功', 'success'); showProductImport.value = false; await loadProducts() }
  catch (e) { showToast(e?.message || '导入失败', 'error') }
  finally { isImporting.value = false }
}
const handleProjectImport = async ({ file }) => {
  if (!file) return showToast('请选择文件', 'warning')
  isImporting.value = true
  try { const res = await importProjectMasters(token.value, file); showToast(res?.message || '导入成功', 'success'); showProjectImport.value = false; await loadProjects(); await loadProducts() }
  catch (e) { showToast(e?.message || '导入失败', 'error') }
  finally { isImporting.value = false }
}

async function saveBlob(blob, name) { const url = URL.createObjectURL(blob); const a = document.createElement('a'); a.href = url; a.download = name; document.body.appendChild(a); a.click(); a.remove(); URL.revokeObjectURL(url) }
const downloadProductTpl = async () => saveBlob(await downloadProductTemplate(token.value), '产品主数据导入模板.xlsx')
const downloadProjectTpl = async () => saveBlob(await downloadProjectMasterTemplate(token.value), '项目主数据导入模板.xlsx')
const exportProducts = () => downloadCsv('产品主数据.csv', [{ key: 'itemNumber', label: '成品料号' }, { key: 'description', label: '产品描述' }, { key: 'descriptionExistsInProjectMaster', label: '项目主数据校验' }], products.value)
const exportProjects = () => downloadCsv('项目主数据.csv', [{ key: 'customer', label: '客户' }, { key: 'productPlatform', label: '产品平台' }, { key: 'vehicleConfig', label: '车型配置' }, { key: 'productDescription', label: '产品描述' }, { key: 'bws', label: 'BWS' }, { key: 'version', label: '版本' }], projects.value)

watch(activeTab, (tab) => { if (tab === 'projects' && projects.value.length === 0) loadProjects() })
onMounted(async () => { await loadProducts(); await loadProjects() })
</script>

<style scoped>
.compact-tabs { margin-bottom: var(--space-4); }
.toolbar { margin-bottom: var(--space-3); }
.search-input { width: 280px; max-width: 45vw; }
.empty-cell { text-align: center; color: var(--muted-foreground); }
.missing-description { font-weight: 700; }
.warn-text { display: inline-block; margin-left: var(--space-2); font-size: var(--text-xs); color: var(--error-text); }
</style>
