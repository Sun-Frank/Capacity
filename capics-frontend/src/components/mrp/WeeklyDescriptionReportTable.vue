<template>
  <div class="weekly-report">
    <h3 v-if="columns.length > 0" class="report-title">
      按描述分类周报表 - {{ createdBy }} / {{ fileName }}
    </h3>

    <div v-if="columns.length > 0" class="table-wrapper">
      <div class="table-scroll">
        <table class="main-table">
          <thead>
            <tr class="header-row-1">
              <th class="sticky-col sticky-col-1">描述分类</th>
              <th class="sticky-col sticky-col-2">物料数</th>
              <th
                v-for="(group, idx) in columnGroups"
                :key="'week-' + idx"
                :colspan="group.versions.length"
                class="week-header"
              >
                {{ group.weekLabel }}
              </th>
            </tr>

            <tr class="header-row-2">
              <th class="sticky-col sticky-col-1">描述分类</th>
              <th class="sticky-col sticky-col-2">物料数</th>
              <th
                v-for="(col, idx) in columns"
                :key="'ver-' + idx"
                :class="['version-header', { 'alt-bg': idx % 2 !== 0 }]"
              >
                {{ col.version }}
              </th>
            </tr>
          </thead>

          <tbody>
            <tr v-for="row in report" :key="row.descriptionGroup">
              <td class="sticky-col sticky-col-1" :title="row.descriptionGroup">{{ row.descriptionGroup }}</td>
              <td class="sticky-col sticky-col-2">{{ row.itemCount }}</td>
              <td
                v-for="(col, idx) in columns"
                :key="'data-' + idx"
                :class="['data-cell', { 'alt-bg': idx % 2 !== 0 }]"
              >
                {{ row[col.key] || '-' }}
              </td>
            </tr>

            <tr v-if="report.length === 0">
              <td :colspan="columns.length + 2" class="empty-cell">暂无数据</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  columns: {
    type: Array,
    default: () => []
  },
  report: {
    type: Array,
    default: () => []
  },
  createdBy: {
    type: String,
    default: ''
  },
  fileName: {
    type: String,
    default: ''
  }
})

const columnGroups = computed(() => {
  const groups = []
  let currentWeek = null
  let currentGroup = null

  for (const col of props.columns) {
    if (col.week !== currentWeek) {
      if (currentGroup) groups.push(currentGroup)
      currentGroup = { week: col.week, weekLabel: col.weekLabel, versions: [] }
      currentWeek = col.week
    }
    currentGroup.versions.push(col)
  }
  if (currentGroup) groups.push(currentGroup)
  return groups
})
</script>

<style scoped>
.weekly-report {
  --col-1-width: 260px;
  --col-2-width: 120px;
  --header-row-height: 40px;

  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.report-title {
  margin-bottom: 1rem;
  flex-shrink: 0;
  font-weight: 600;
  color: var(--foreground);
}

.table-wrapper {
  flex: 1;
  overflow: hidden;
  background: white;
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-light);
}

.table-scroll {
  width: 100%;
  height: 100%;
  overflow: auto;
  border-radius: var(--radius-lg);
}

.main-table {
  border-collapse: separate;
  border-spacing: 0;
  width: max-content;
  min-width: 100%;
}

.sticky-col {
  position: sticky;
  background: #fff;
}

.sticky-col-1 {
  left: 0;
  width: var(--col-1-width);
  min-width: var(--col-1-width);
  max-width: var(--col-1-width);
  z-index: 6;
}

.sticky-col-2 {
  left: var(--col-1-width);
  width: var(--col-2-width);
  min-width: var(--col-2-width);
  max-width: var(--col-2-width);
  z-index: 5;
  overflow: hidden;
  text-overflow: ellipsis;
}

thead th {
  position: sticky;
  border: 1px solid var(--border-light);
  font-weight: 600;
  text-align: center;
  padding: 0.5rem 0.75rem;
  white-space: nowrap;
  box-sizing: border-box;
  height: var(--header-row-height);
  background: var(--muted);
  color: var(--foreground);
}

.header-row-1 th {
  top: 0;
  z-index: 8;
  background: white;
  color: var(--foreground);
}

.header-row-2 th {
  top: var(--header-row-height);
  z-index: 7;
  background: var(--foreground);
  color: white;
}

.header-row-1 th.sticky-col-1,
.header-row-2 th.sticky-col-1 {
  z-index: 10;
}

.header-row-1 th.sticky-col-2,
.header-row-2 th.sticky-col-2 {
  z-index: 9;
}

.week-header {
  background: white !important;
  color: var(--foreground) !important;
  border-left: none;
  border-right: none;
}

.version-header {
  min-width: 80px;
  width: 80px;
  background: var(--muted) !important;
  color: var(--foreground) !important;
}

.alt-bg {
  background: #E8F4FD !important;
  color: var(--foreground) !important;
}

tbody td {
  border: 1px solid var(--border-light);
  padding: 0.5rem 0.75rem;
  text-align: center;
  white-space: nowrap;
  box-sizing: border-box;
  height: var(--header-row-height);
  background: white;
  color: var(--foreground);
}

.data-cell.alt-bg {
  background: #E8F4FD !important;
}

tbody td.sticky-col-1,
tbody td.sticky-col-2 {
  background: #fff;
  font-weight: 500;
}

tbody td.sticky-col-1 {
  z-index: 4;
  text-align: left;
}

tbody td.sticky-col-2 {
  z-index: 3;
}

.header-row-1 th.sticky-col-1,
.header-row-2 th.sticky-col-1,
tbody td.sticky-col-1 {
  border-right: 1px solid var(--border);
}

.empty-cell {
  text-align: center;
  color: var(--text-secondary);
  padding: 2rem 0;
}
</style>
