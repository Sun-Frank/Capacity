<template>
  <div class="table-wrapper">
    <table>
      <thead>
        <tr>
          <th>编码族</th>
          <th>生产线</th>
          <th>描述</th>
          <th>编码规则</th>
          <th>周期时间(秒)</th>
          <th>OEE(%)</th>
          <th>人数</th>
          <th>创建人/修改人</th>
          <th>创建/修改时间</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="f in familyLines" :key="f.familyCode + f.lineCode">
          <td>{{ f.familyCode }}</td>
          <td>{{ f.lineCode }}</td>
          <td>{{ f.description || '-' }}</td>
          <td>{{ f.codingRule }}</td>
          <td>{{ f.cycleTime }}</td>
          <td>{{ formatOee(f.oee) }}</td>
          <td>{{ f.workerCount }}</td>
          <td>{{ f.updatedBy || f.createdBy || '-' }}</td>
          <td>{{ formatDate(f.updatedAt || f.createdAt) }}</td>
          <td>
            <button class="btn btn-small" @click="$emit('edit', f)">编辑</button>
          </td>
        </tr>
        <tr v-if="familyLines.length === 0">
          <td colspan="10" style="text-align: center; color: var(--muted-foreground);">暂无数据</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup>
defineProps({
  familyLines: {
    type: Array,
    default: () => []
  }
})

defineEmits(['edit'])

const formatOee = (value) => {
  if (value === null || value === undefined) return '-'
  const num = parseFloat(value)
  if (isNaN(num)) return '-'
  return num.toFixed(2) + '%'
}

const formatDate = (value) => {
  if (!value) return '-'
  const [datePart, timePart] = value.split('T')
  const [year, month, day] = datePart.split('-')
  const time = timePart ? timePart.substring(0, 5) : ''
  return `${year}/${month}/${day} ${time}`
}
</script>
