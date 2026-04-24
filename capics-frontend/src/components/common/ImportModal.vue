<template>
  <div v-if="show" class="modal-overlay" @click.self="$emit('close')">
    <div class="modal-content">
      <div class="modal-header">
        <h3 class="modal-title">导入{{ typeLabel }}</h3>
      </div>

      <div v-if="isImporting" class="import-loading">
        <div class="import-loading-text">{{ importLoadingText }}</div>
        <div class="import-loading-hint">大文件可能需要较长时间</div>
        <div v-if="showProgressBar" class="progress-wrap">
          <div class="progress-bar">
            <div class="progress-bar-inner" :style="{ width: `${safeProgress}%` }"></div>
          </div>
          <div class="progress-text">{{ safeProgress }}%</div>
        </div>
      </div>

      <div v-else class="modal-body">
        <div class="form-group">
          <input type="file" accept=".xlsx,.xls" @change="handleFileUpload" class="form-input">
        </div>

        <div v-if="type === 'mrp'" class="form-group">
          <input
            type="text"
            v-model="fileName"
            placeholder="输入文件名称（自定义命名）"
            class="form-input"
          >
        </div>
      </div>

      <div v-if="!isImporting" class="modal-footer">
        <button class="btn" @click="$emit('close')">取消</button>
        <button class="btn btn-primary" @click="$emit('confirm', { file, fileName })">确认导入</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  show: {
    type: Boolean,
    default: false
  },
  type: {
    type: String,
    default: 'family'
  },
  isImporting: {
    type: Boolean,
    default: false
  },
  importProgress: {
    type: Number,
    default: null
  },
  importLoadingText: {
    type: String,
    default: '导入中，请稍候...'
  }
})

const file = ref(null)
const fileName = ref('')

const safeProgress = computed(() => {
  if (typeof props.importProgress !== 'number' || Number.isNaN(props.importProgress)) {
    return 0
  }
  return Math.max(0, Math.min(100, Math.floor(props.importProgress)))
})

const showProgressBar = computed(() => typeof props.importProgress === 'number')

const typeLabel = computed(() => {
  const labels = {
    family: '编码族',
    product: '产品',
    mrp: 'MRP',
    routing: '工艺路线',
    line: '生产线配置',
    'ct-line': '产线-产品'
  }
  return labels[props.type] || props.type
})

const handleFileUpload = (e) => {
  file.value = e.target.files[0]
}

defineExpose({ fileName })
</script>

<style scoped>
.progress-wrap {
  margin-top: 12px;
}

.progress-bar {
  width: 100%;
  height: 8px;
  border-radius: 999px;
  background: #e5edf9;
  overflow: hidden;
}

.progress-bar-inner {
  height: 100%;
  background: linear-gradient(90deg, #1890ff, #4aa8ff);
  transition: width 0.2s ease;
}

.progress-text {
  margin-top: 6px;
  color: #4b5563;
  font-size: 12px;
}
</style>
