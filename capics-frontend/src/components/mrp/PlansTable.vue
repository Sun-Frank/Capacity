<template>
  <div class="table-wrapper">
    <table>
      <thead>
        <tr>
          <th>物料号</th>
          <th>描述</th>
          <th>工厂</th>
          <th>生产线</th>
          <th>发布日期</th>
          <th>数量</th>
          <th>版本</th>
          <th>文件</th>
          <th>导入人</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="plan in plans" :key="plan.id">
          <td>{{ plan.itemNumber }}</td>
          <td>{{ plan.description }}</td>
          <td>{{ plan.site }}</td>
          <td>{{ plan.productionLine }}</td>
          <td>{{ plan.releaseDate }}</td>
          <td>{{ plan.quantityScheduled }}</td>
          <td><span class="version-badge">{{ plan.version }}</span></td>
          <td>{{ plan.fileName || '-' }}</td>
          <td>{{ plan.createdBy || '-' }}</td>
        </tr>
        <tr v-if="plans.length === 0">
          <td colspan="9" style="text-align: center; color: var(--muted-foreground);">暂无数据</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup>
defineProps({
  plans: {
    type: Array,
    default: () => []
  }
})
</script>
