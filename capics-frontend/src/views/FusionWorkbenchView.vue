<template>
  <div class="page">
    <div class="page-header">
      <h1 class="page-title">融合工作台</h1>
      <p class="page-subtitle">整合V2能力：线体画像、人力因子、会议纪要</p>
    </div>

    <div class="tabs">
      <button class="btn" :class="{ 'btn-primary': tab === 'manpower' }" @click="tab = 'manpower'">人力因子</button>
      <button class="btn" :class="{ 'btn-primary': tab === 'minutes' }" @click="tab = 'minutes'">会议纪要</button>
    </div>

    <div v-if="tab === 'manpower'" class="card">
      <div class="filters-row">
        <input v-model="manpowerForm.lineClass" placeholder="线体分类(如 SMT/FAL)" class="input" />
        <input v-model="manpowerForm.belongTo" placeholder="归属(如 PCBA/FA)" class="input" />
        <input v-model="manpowerForm.planDate" type="date" class="input" />
        <input v-model.number="manpowerForm.manpowerFactor" type="number" step="0.01" min="0.1" class="input" />
        <button class="btn btn-primary" @click="saveManpower">保存</button>
      </div>
      <table class="main-table">
        <thead><tr><th>ID</th><th>线体分类</th><th>归属</th><th>日期</th><th>因子</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="item in manpowerPlans" :key="item.id">
            <td>{{ item.id }}</td>
            <td>{{ item.lineClass }}</td>
            <td>{{ item.belongTo || '-' }}</td>
            <td>{{ item.planDate }}</td>
            <td>{{ item.manpowerFactor }}</td>
            <td><button class="btn" @click="removeManpower(item.id)">删除</button></td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-else class="card">
      <div class="filters-row">
        <input v-model="minutesForm.mpsVersion" placeholder="MRP版本" class="input" />
        <input v-model.number="minutesForm.itemNo" type="number" min="1" class="input" />
        <input v-model="minutesForm.minutes" placeholder="会议内容" class="input wide" />
        <button class="btn btn-primary" @click="saveMinutes">保存</button>
      </div>
      <table class="main-table">
        <thead><tr><th>ID</th><th>版本</th><th>项次</th><th>内容</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="item in meetingMinutes" :key="item.id">
            <td>{{ item.id }}</td>
            <td>{{ item.mpsVersion }}</td>
            <td>{{ item.itemNo }}</td>
            <td>{{ item.minutes }}</td>
            <td><button class="btn" @click="removeMinutes(item.id)">删除</button></td>
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
import { createManpowerPlan, createMeetingMinutes, deleteManpowerPlan, deleteMeetingMinutes, getManpowerPlans, getMeetingMinutes } from '@/api/fusion'

const { token, currentUsername } = useAuth()
const { showToast } = useToast()

const tab = ref('manpower')
const manpowerPlans = ref([])
const meetingMinutes = ref([])

const manpowerForm = ref({
  lineClass: '',
  belongTo: '',
  planDate: '',
  manpowerFactor: 1
})

const minutesForm = ref({
  mpsVersion: '',
  itemNo: 1,
  minutes: ''
})

const loadManpower = async () => {
  const data = await getManpowerPlans(token.value)
  manpowerPlans.value = data.data || []
}

const loadMinutes = async () => {
  const data = await getMeetingMinutes(token.value)
  meetingMinutes.value = data.data || []
}

const saveManpower = async () => {
  if (!manpowerForm.value.lineClass || !manpowerForm.value.planDate) {
    showToast('请填写线体分类和日期', 'warning')
    return
  }
  await createManpowerPlan(token.value, {
    ...manpowerForm.value,
    updatedBy: currentUsername.value || 'system'
  })
  manpowerForm.value.manpowerFactor = 1
  await loadManpower()
  showToast('人力因子已保存', 'success')
}

const removeManpower = async (id) => {
  await deleteManpowerPlan(token.value, id)
  await loadManpower()
}

const saveMinutes = async () => {
  if (!minutesForm.value.mpsVersion || !minutesForm.value.minutes) {
    showToast('请填写版本和内容', 'warning')
    return
  }
  await createMeetingMinutes(token.value, {
    ...minutesForm.value,
    updatedBy: currentUsername.value || 'system'
  })
  minutesForm.value.minutes = ''
  await loadMinutes()
  showToast('会议纪要已保存', 'success')
}

const removeMinutes = async (id) => {
  await deleteMeetingMinutes(token.value, id)
  await loadMinutes()
}

onMounted(async () => {
  await loadManpower()
  await loadMinutes()
})
</script>

<style scoped>
.input { min-width: 160px; padding: 8px; border: 1px solid #d1d5db; border-radius: 4px; }
.wide { min-width: 320px; }
.card { margin-top: 16px; background: #fff; border: 1px solid #e5e7eb; padding: 16px; border-radius: 8px; }
.tabs { display: flex; gap: 8px; }
</style>
