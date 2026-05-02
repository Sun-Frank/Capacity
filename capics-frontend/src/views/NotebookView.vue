<template>
  <div class="page two-column-page">
    <aside class="note-list">
      <div class="toolbar">
        <button class="btn btn-primary" @click="newNote">新建记事</button>
      </div>
      <div
        v-for="note in notes"
        :key="note.id"
        class="note-item"
        :class="{ active: selected?.id === note.id }"
        @click="selectNote(note)"
      >
        <strong>{{ note.title }}</strong>
        <span>{{ note.updatedAt || note.createdAt }}</span>
      </div>
      <div v-if="notes.length === 0" class="empty-state">暂无记事</div>
    </aside>

    <section class="editor-panel">
      <input v-model.trim="form.title" class="form-input title-input" placeholder="标题" />
      <textarea v-model="form.content" class="markdown-editor" placeholder="支持 Markdown，输入后会自动保存到服务器"></textarea>
      <div class="toolbar">
        <button class="btn btn-primary" @click="saveNote">立即保存</button>
        <button v-if="form.id" class="btn btn-danger" @click="removeNote">删除</button>
        <span class="save-state">{{ saveState }}</span>
      </div>
      <h3 class="section-title preview-title">预览</h3>
      <pre class="markdown-preview">{{ form.content }}</pre>
    </section>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useAuth } from '@/composables/useAuth'
import { useToast } from '@/composables/useToast'
import { deleteNotebookNote, getNotebookNotes, saveNotebookNote } from '@/api/notebook'

const { token } = useAuth()
const { showToast } = useToast()
const notes = ref([])
const selected = ref(null)
const form = ref({ id: null, title: '', content: '' })
const saveState = ref('')
let saveTimer = null
let suppressAutoSave = false

const load = async () => {
  const res = await getNotebookNotes(token.value)
  notes.value = res?.data || []
}

const newNote = () => {
  selected.value = null
  form.value = { id: null, title: '', content: '' }
}

const selectNote = (note) => {
  selected.value = note
  form.value = { ...note }
}

const saveNote = async (silent = false) => {
  if (!form.value.title) {
    if (!silent) showToast('标题不能为空', 'warning')
    return
  }
  saveState.value = '保存中...'
  const res = await saveNotebookNote(token.value, form.value)
  saveState.value = '已保存'
  if (!silent) showToast(res?.message || '保存成功', 'success')
  suppressAutoSave = true
  await load()
  if (res?.data) selectNote(res.data)
  suppressAutoSave = false
}

const removeNote = async () => {
  await deleteNotebookNote(token.value, form.value.id)
  showToast('删除成功', 'success')
  newNote()
  await load()
}

watch(form, () => {
  if (suppressAutoSave || !form.value.title) return
  saveState.value = '等待自动保存...'
  if (saveTimer) clearTimeout(saveTimer)
  saveTimer = setTimeout(() => saveNote(true).catch(() => { saveState.value = '自动保存失败' }), 1200)
}, { deep: true })

onMounted(load)
onBeforeUnmount(() => { if (saveTimer) clearTimeout(saveTimer) })
</script>

<style scoped>
.two-column-page {
  display: grid;
  grid-template-columns: 300px minmax(0, 1fr);
  gap: var(--space-4);
  height: calc(100vh - var(--topbar-height));
  overflow: hidden;
}

.note-list,
.editor-panel {
  min-height: 0;
  overflow: auto;
}

.note-list {
  padding: var(--space-3);
}

.note-item {
  padding: var(--space-3);
  border: 1px solid transparent;
  border-radius: var(--radius-sm);
  cursor: pointer;
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
  transition: all var(--transition-fast);
}

.note-item:hover,
.note-item.active {
  background: var(--info-bg);
  border-color: var(--info-border);
}

.note-item span,
.save-state {
  font-size: var(--text-xs);
  color: var(--muted-foreground);
}

.title-input {
  margin-bottom: var(--space-3);
}

.markdown-editor {
  width: 100%;
  min-height: 320px;
  font-family: var(--font-mono);
}

.preview-title {
  margin: var(--space-4) 0 var(--space-2);
}

.markdown-preview {
  white-space: pre-wrap;
  background: var(--surface-soft);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  padding: var(--space-4);
  min-height: 160px;
}

@media (max-width: 900px) {
  .two-column-page {
    grid-template-columns: 1fr;
    overflow: auto;
  }
}
</style>
