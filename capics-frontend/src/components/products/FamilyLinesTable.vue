<template>
  <div class="table-wrapper">
    <table>
      <thead>
        <tr>
          <th>编码族</th>
          <th>生产线</th>
          <th>描述</th>
          <th>创建人/修改人</th>
          <th>创建/修改时间</th>
          <th v-if="canEdit">操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="f in familyLines" :key="f.familyCode + f.lineCode">
          <td>{{ f.familyCode }}</td>
          <td>{{ f.lineCode }}</td>
          <td>{{ f.description || '-' }}</td>
          <td>{{ f.updatedBy || f.createdBy || '-' }}</td>
          <td>{{ formatDate(f.updatedAt || f.createdAt) }}</td>
          <td v-if="canEdit">
            <button class="btn btn-small" @click="$emit('edit', f)">编辑</button>
          </td>
        </tr>
        <tr v-if="familyLines.length === 0">
          <td :colspan="canEdit ? 6 : 5" style="text-align: center; color: var(--muted-foreground);">暂无数据</td>
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
  },
  canEdit: {
    type: Boolean,
    default: true
  }
})

defineEmits(['edit'])

const formatDate = (value) => {
  if (!value) return '-'
  const [datePart, timePart] = String(value).split('T')
  if (!datePart) return '-'
  const [year, month, day] = datePart.split('-')
  const time = timePart ? timePart.substring(0, 5) : ''
  return `${year}/${month}/${day} ${time}`.trim()
}
</script>
