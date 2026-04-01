<template>
  <div class="monthly-report">
    <h3 v-if="columns.length > 0" class="report-title">
      月需求报表 - {{ createdBy }} / {{ fileName }}
    </h3>
    <div v-if="columns.length > 0" class="table-wrapper">
      <div class="table-scroll">
        <table class="main-table">
          <thead>
            <tr class="header-row-1">
              <th class="sticky-col sticky-col-1">物料号</th>
              <th class="sticky-col sticky-col-2">描述</th>
              <th
                v-for="(group, idx) in columnGroups"
                :key="'month-' + idx"
                :colspan="group.versions.length"
                class="month-header"
              >
                {{ group.monthLabel }}
              </th>
            </tr>
            <tr class="header-row-2">
              <th class="sticky-col sticky-col-1">物料号</th>
              <th class="sticky-col sticky-col-2">描述</th>
              <th
                v-for="(col, idx) in columns"
                :key="'ver-' + idx"
                :class="['version-header', { 'alt-bg': col.versionIndex % 2 !== 0 }]"
              >
                {{ col.version }}
              </th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in report" :key="row.itemNumber">
              <td class="sticky-col sticky-col-1">{{ row.itemNumber }}</td>
              <td class="sticky-col sticky-col-2">{{ row.description }}</td>
              <td
                v-for="(col, idx) in columns"
                :key="'data-' + idx"
                :class="['data-cell', { 'alt-bg': col.versionIndex % 2 !== 0 }]"
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

// Group columns by month for the first header row
const columnGroups = computed(() => {
  const groups = []
  let currentMonth = null
  let currentGroup = null

  for (const col of props.columns) {
    if (col.month !== currentMonth) {
      if (currentGroup) groups.push(currentGroup)
      currentGroup = { month: col.month, monthLabel: col.monthLabel, versions: [], versionIndices: [] }
      currentMonth = col.month
    }
    currentGroup.versions.push(col.version)
    currentGroup.versionIndices.push(props.columns.indexOf(col))
  }
  if (currentGroup) groups.push(currentGroup)
  return groups
})
</script>

<style scoped>
/* === Apple Style Variables === */
.monthly-report {
  --col-1-width: 180px;
  --col-2-width: 260px;
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

/* === Table Wrapper === */
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

/* === Sticky Columns === */
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

/* === Header Cells === */
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

/* === Month Header === */
.month-header {
  background: white !important;
  color: var(--foreground) !important;
  border-left: none;
  border-right: none;
}

/* === Version Headers === */
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

/* === Body Cells === */
tbody td {
  border: 1px solid var(--border-light);
  border-top: none;
  padding: 0.5rem 0.75rem;
  text-align: center;
  white-space: nowrap;
  box-sizing: border-box;
  height: var(--header-row-height);
  background: white;
  color: var(--foreground);
}

tbody td.alt-bg {
  background: #E8F4FD !important;
}

tbody td.sticky-col-1,
tbody td.sticky-col-2 {
  background: #fff;
  font-weight: 500;
  text-align: left;
}

tbody td.sticky-col-1 {
  z-index: 4;
}

tbody td.sticky-col-2 {
  z-index: 3;
}

/* === Sticky Column Borders === */
.header-row-1 th.sticky-col-1,
.header-row-2 th.sticky-col-1,
tbody td.sticky-col-1 {
  border-right: 1px solid var(--border);
}

.header-row-1 th.sticky-col-2,
.header-row-2 th.sticky-col-2,
tbody td.sticky-col-2 {
  border-right: 1px solid var(--border);
}

/* === Empty Cell === */
.empty-cell {
  text-align: center;
  color: var(--muted-foreground);
  padding: 2rem;
}

/* === Scrollbar - Apple Style === */
.table-scroll::-webkit-scrollbar {
  height: 8px;
  width: 8px;
}

.table-scroll::-webkit-scrollbar-track {
  background: var(--muted);
  border-radius: 4px;
}

.table-scroll::-webkit-scrollbar-thumb {
  background: var(--border);
  border-radius: 4px;
}

.table-scroll::-webkit-scrollbar-thumb:hover {
  background: var(--muted-foreground);
}

.table-scroll::-webkit-scrollbar-corner {
  background: var(--muted);
}
</style>
