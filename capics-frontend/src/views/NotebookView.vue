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
      <div class="markdown-help">
        <div class="markdown-help-title">Markdown 速查</div>
        <div class="markdown-help-grid">
          <span><code># 标题</code> 一级标题</span>
          <span><code>## 标题</code> 二级标题</span>
          <span><code>- 内容</code> 无序列表</span>
          <span><code>1. 内容</code> 有序列表</span>
          <span><code>**重点**</code> 加粗</span>
          <span><code>`代码`</code> 行内代码</span>
          <span><code>[文字](链接)</code> 超链接</span>
          <span><code>&gt; 引用</code> 引用说明</span>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
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
let lastSavedSignature = ''

const buildSignature = (note) => JSON.stringify({
  id: note?.id || null,
  title: note?.title || '',
  content: note?.content || ''
})

const applyFormSilently = async (note, selectedNote = note) => {
  suppressAutoSave = true
  selected.value = selectedNote
  form.value = { id: null, title: '', content: '', ...(note || {}) }
  lastSavedSignature = buildSignature(form.value)
  await nextTick()
  suppressAutoSave = false
}

const load = async () => {
  const res = await getNotebookNotes(token.value)
  notes.value = res?.data || []
}

const newNote = () => {
  if (saveTimer) clearTimeout(saveTimer)
  saveState.value = ''
  applyFormSilently({ id: null, title: '', content: '' }, null)
}

const selectNote = (note) => {
  if (saveTimer) clearTimeout(saveTimer)
  saveState.value = ''
  applyFormSilently(note, note)
}

const saveNote = async (silent = false) => {
  if (!form.value.title) {
    if (!silent) showToast('标题不能为空', 'warning')
    return
  }
  saveState.value = '保存中...'
  const res = await saveNotebookNote(token.value, form.value)
  if (!silent) showToast(res?.message || '保存成功', 'success')
  await load()
  if (res?.data) {
    await applyFormSilently(res.data, res.data)
  } else {
    lastSavedSignature = buildSignature(form.value)
  }
  saveState.value = '已保存'
}

const removeNote = async () => {
  await deleteNotebookNote(token.value, form.value.id)
  showToast('删除成功', 'success')
  newNote()
  await load()
}

watch(form, () => {
  if (suppressAutoSave || !form.value.title) return
  const currentSignature = buildSignature(form.value)
  if (currentSignature === lastSavedSignature) return
  saveState.value = '等待自动保存...'
  if (saveTimer) clearTimeout(saveTimer)
  saveTimer = setTimeout(() => {
    saveNote(true).catch((error) => {
      saveState.value = `自动保存失败：${error?.message || '请检查网络或重新登录'}`
    })
  }, 1200)
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

.markdown-help {
  margin: var(--space-3) 0;
  padding: var(--space-3);
  background: var(--info-bg);
  border: 1px solid var(--info-border);
  border-radius: var(--radius-md);
}

.markdown-help-title {
  margin-bottom: var(--space-2);
  font-weight: 800;
  color: var(--info-text);
}

.markdown-help-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-2);
  color: var(--color-text-muted);
  font-size: var(--text-sm);
}

.markdown-help code {
  display: inline-block;
  margin-right: var(--space-2);
  padding: 2px 6px;
  background: var(--surface);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-sm);
  color: var(--color-text-strong);
  font-family: var(--font-mono);
  font-size: var(--text-xs);
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

  .markdown-help-grid {
    grid-template-columns: 1fr;
  }
}
</style>
