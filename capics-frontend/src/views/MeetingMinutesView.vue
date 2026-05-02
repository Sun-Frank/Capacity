<template>
  <div class="page">
    <div class="table-wrapper meeting-table">
      <div class="toolbar sticky-toolbar">
        <button class="btn btn-primary" @click="startAdd">新增会议项</button>
        <button class="btn" @click="exportRows">数据导出</button>
        <input v-model.trim="feishuEmail" class="form-input feishu-email" placeholder="飞书接收人邮箱" />
        <button class="btn" @click="sendToFeishu">发送到飞书</button>
      </div>
      <table>
        <thead>
          <tr>
            <th>产品维度</th>
            <th>产品描述</th>
            <th>生产线</th>
            <th>调整项</th>
            <th>调整前</th>
            <th>调整后</th>
            <th>责任人</th>
            <th>状态</th>
            <th>会议内容</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="editing">
            <td><input v-model.trim="form.productNumber" class="form-input" /></td>
            <td><input v-model.trim="form.productDescription" class="form-input" /></td>
            <td><input v-model.trim="form.lineCode" class="form-input" /></td>
            <td><input v-model.trim="form.adjustmentField" class="form-input" /></td>
            <td><input v-model.trim="form.beforeValue" class="form-input" /></td>
            <td><input v-model.trim="form.afterValue" class="form-input" /></td>
            <td><input v-model.trim="form.ownerName" class="form-input" /></td>
            <td><input v-model.trim="form.status" class="form-input" /></td>
            <td><input v-model.trim="form.minutes" class="form-input" /></td>
            <td class="row-actions">
              <button class="btn btn-small btn-primary" @click="save">保存</button>
              <button class="btn btn-small" @click="cancel">取消</button>
            </td>
          </tr>
          <tr v-for="row in rows" :key="row.id">
            <td>{{ row.productNumber || '-' }}</td>
            <td>{{ row.productDescription || '-' }}</td>
            <td>{{ row.lineCode || '-' }}</td>
            <td>{{ row.adjustmentField || '-' }}</td>
            <td>{{ row.beforeValue || '-' }}</td>
            <td>{{ row.afterValue || '-' }}</td>
            <td>{{ row.ownerName || '-' }}</td>
            <td>{{ row.status || 'OPEN' }}</td>
            <td>{{ row.minutes || '-' }}</td>
            <td class="row-actions">
              <button class="btn btn-small" @click="edit(row)">编辑</button>
              <button class="btn btn-small btn-danger" @click="remove(row.id)">删除</button>
            </td>
          </tr>
          <tr v-if="rows.length === 0 && !editing">
            <td colspan="10" class="empty-cell">暂无数据</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useAuth } from '@/composables/useAuth'
import { useToast } from '@/composables/useToast'
import { createMeetingMinutes, deleteMeetingMinutes, getMeetingMinutes } from '@/api/fusion'
import { sendFeishuMessage } from '@/api/feishuConfig'
import { downloadCsv } from '@/utils/export'

const { token, currentUser } = useAuth()
const { showToast } = useToast()
const rows = ref([])
const editing = ref(false)
const form = ref({})
const feishuEmail = ref('')

const load = async () => {
  const res = await getMeetingMinutes(token.value)
  rows.value = res?.data || []
}

const startAdd = () => {
  editing.value = true
  form.value = { status: 'OPEN', updatedBy: currentUser.value || 'system' }
}

const edit = (row) => {
  editing.value = true
  form.value = { ...row }
}

const cancel = () => {
  editing.value = false
  form.value = {}
}

const save = async () => {
  const res = await createMeetingMinutes(token.value, { ...form.value, updatedBy: currentUser.value || 'system' })
  showToast(res?.message || '保存成功', 'success')
  cancel()
  await load()
}

const remove = async (id) => {
  await deleteMeetingMinutes(token.value, id)
  showToast('删除成功', 'success')
  await load()
}

const exportRows = () => downloadCsv('会议纪要.csv', [
  { key: 'productNumber', label: '产品维度' },
  { key: 'productDescription', label: '产品描述' },
  { key: 'lineCode', label: '生产线' },
  { key: 'adjustmentField', label: '调整项' },
  { key: 'beforeValue', label: '调整前' },
  { key: 'afterValue', label: '调整后' },
  { key: 'ownerName', label: '责任人' },
  { key: 'status', label: '状态' },
  { key: 'minutes', label: '会议内容' }
], rows.value)

const sendToFeishu = async () => {
  if (!feishuEmail.value) return showToast('请输入飞书接收人邮箱', 'warning')
  const text = [
    'CAPICS会议纪要',
    ...(rows.value || []).map((row, index) => `${index + 1}. ${row.productDescription || row.productNumber || '-'} ${row.lineCode || ''} ${row.adjustmentField || ''}: ${row.beforeValue || '-'} -> ${row.afterValue || '-'}，责任人：${row.ownerName || '-'}，状态：${row.status || 'OPEN'}，内容：${row.minutes || '-'}`)
  ].join('\n')
  await sendFeishuMessage(token.value, { email: feishuEmail.value, text })
  showToast('已发送到飞书', 'success')
}

onMounted(load)
</script>

<style scoped>
.meeting-table {
  min-height: 0;
}

.feishu-email {
  width: 240px;
}

.row-actions {
  display: flex;
  gap: var(--space-2);
  white-space: nowrap;
}

.empty-cell {
  text-align: center;
  color: var(--muted-foreground);
}
</style>
