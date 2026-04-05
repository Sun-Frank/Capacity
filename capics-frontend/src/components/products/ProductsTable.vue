<template>
  <div class="table-wrapper">
    <table>
      <thead>
        <tr>
          <th>物料号</th>
          <th>生产线</th>
          <th>编码族</th>
          <th>PF</th>
          <th>描述</th>
          <th>CT(秒)</th>
          <th>OEE(%)</th>
          <th>人数</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="p in products" :key="p.itemNumber + p.lineCode">
          <td>{{ p.itemNumber }}</td>
          <td>{{ p.lineCode }}</td>
          <td>{{ p.familyCode }}</td>
          <td>{{ p.pf || '-' }}</td>
          <td>{{ p.description }}</td>
          <td>{{ p.cycleTime }}</td>
          <td>{{ formatOee(p.oee) }}</td>
          <td>{{ p.workerCount }}</td>
        </tr>
        <tr v-if="products.length === 0">
          <td colspan="8" style="text-align: center; color: var(--muted-foreground);">暂无数据</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup>
defineProps({
  products: {
    type: Array,
    default: () => []
  }
})

const formatOee = (value) => {
  if (value === null || value === undefined) return '-'
  const num = parseFloat(value)
  if (isNaN(num)) return '-'
  return num.toFixed(2) + '%'
}
</script>
