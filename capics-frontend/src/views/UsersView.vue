<template>
  <div class="page">
    <div class="page-header">
      <h1 class="page-title">用户管理</h1>
      <p class="page-subtitle">用户账号与权限分组维护</p>
    </div>
    <div class="table-wrapper">
      <div style="margin-bottom: 1rem; display: flex; gap: 1rem; align-items: center;">
        <button class="btn btn-primary" @click="showAddModal">新增用户</button>
      </div>
      <table>
        <thead>
          <tr>
            <th>用户名</th>
            <th>姓名</th>
            <th>邮箱</th>
            <th>用户组</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="user in users" :key="user.id">
            <td>{{ user.username }}</td>
            <td>{{ user.realName }}</td>
            <td>{{ user.email }}</td>
            <td>{{ roleLabel(user.roleCode) }}</td>
            <td>{{ user.enabled ? '启用' : '禁用' }}</td>
            <td>
              <button class="btn btn-small" @click="editUser(user)">编辑</button>
              <button class="btn btn-small" style="margin-left: 0.5rem;" @click="changeUserPassword(user)">改密码</button>
              <button v-if="user.id !== currentUserId" class="btn btn-small btn-danger" style="margin-left: 0.5rem;" @click="deleteUser(user)">删除</button>
            </td>
          </tr>
          <tr v-if="users.length === 0">
            <td colspan="6" style="text-align: center; color: var(--muted-foreground);">暂无数据</td>
          </tr>
        </tbody>
      </table>
    </div>

    <UserModal
      :show="showModal"
      :user="selectedUser"
      @close="closeModal"
      @confirm="handleConfirm"
    />

    <ConfirmModal
      :show="showDeleteConfirm"
      :message="'确定要删除用户 ' + deleteTarget?.username + ' 吗？'"
      @close="showDeleteConfirm = false"
      @confirm="handleDelete"
    />

    <div v-if="showPasswordModal" class="modal-overlay" @click.self="showPasswordModal = false">
      <div class="modal">
        <div class="modal-header">
          <h3>修改密码 - {{ passwordTarget?.username }}</h3>
          <button class="modal-close" @click="showPasswordModal = false">&times;</button>
        </div>
        <form @submit.prevent="handlePasswordChange">
          <div class="form-group">
            <label>新密码</label>
            <input type="password" v-model="newPassword" class="form-input" required />
          </div>
          <div class="modal-footer">
            <button type="button" class="btn" @click="showPasswordModal = false">取消</button>
            <button type="submit" class="btn btn-primary" :disabled="isSubmitting">
              {{ isSubmitting ? '保存中...' : '保存' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useAuth } from '@/composables/useAuth'
import { getUsers, createUser, updateUser, deleteUser as deleteUserApi, changePassword } from '@/api/user'
import UserModal from '@/components/users/UserModal.vue'
import ConfirmModal from '@/components/common/ConfirmModal.vue'

const { token, currentUser, userId } = useAuth()
const users = ref([])
const showModal = ref(false)
const selectedUser = ref(null)
const showDeleteConfirm = ref(false)
const deleteTarget = ref(null)
const showPasswordModal = ref(false)
const passwordTarget = ref(null)
const newPassword = ref('')
const isSubmitting = ref(false)

const currentUserId = userId

const roleLabel = (roleCode) => {
  const code = String(roleCode || '').toUpperCase()
  if (code === 'ADMIN') return '管理员'
  if (code === 'MASTERDATA') return '主数据'
  if (code === 'PLAN') return '计划'
  return code || '-'
}

const loadUsers = async () => {
  try {
    const data = await getUsers(token.value)
    users.value = data.data || []
  } catch (err) {
    console.error('Load users error:', err)
  }
}

const showAddModal = () => {
  selectedUser.value = null
  showModal.value = true
}

const editUser = (user) => {
  selectedUser.value = user
  showModal.value = true
}

const closeModal = () => {
  showModal.value = false
  selectedUser.value = null
}

const handleConfirm = async (formData) => {
  try {
    const username = currentUser.value || 'system'
    const userData = {
      ...formData,
      updatedBy: username
    }

    if (selectedUser.value) {
      await updateUser(token.value, selectedUser.value.id, userData)
    } else {
      await createUser(token.value, userData)
    }
    closeModal()
    loadUsers()
  } catch (err) {
    console.error('Save user error:', err)
    alert('保存失败')
  }
}

const changeUserPassword = (user) => {
  passwordTarget.value = user
  newPassword.value = ''
  showPasswordModal.value = true
}

const handlePasswordChange = async () => {
  if (!newPassword.value) return
  isSubmitting.value = true
  try {
    await changePassword(token.value, passwordTarget.value.id, newPassword.value)
    showPasswordModal.value = false
    alert('密码修改成功')
  } catch (err) {
    console.error('Change password error:', err)
    alert('修改密码失败')
  } finally {
    isSubmitting.value = false
  }
}

const deleteUser = (user) => {
  deleteTarget.value = user
  showDeleteConfirm.value = true
}

const handleDelete = async () => {
  try {
    await deleteUserApi(token.value, deleteTarget.value.id)
    showDeleteConfirm.value = false
    deleteTarget.value = null
    loadUsers()
  } catch (err) {
    console.error('Delete user error:', err)
    alert('删除失败')
  }
}

onMounted(() => {
  loadUsers()
})
</script>

<style scoped>
.btn-danger {
  background-color: #dc3545;
  color: white;
}

.btn-danger:hover {
  background-color: #c82333;
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal {
  background: #ffffff;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  width: 90%;
  max-width: 500px;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem 1.5rem;
  border-bottom: 1px solid var(--border-color);
}

.modal-header h3 {
  margin: 0;
  font-size: 1.25rem;
}

.modal-close {
  background: none;
  border: none;
  font-size: 1.5rem;
  cursor: pointer;
  color: var(--muted-foreground);
}

.modal-close:hover {
  color: var(--foreground);
}

form {
  padding: 1.5rem;
}

.form-group {
  margin-bottom: 1rem;
}

.form-group label {
  display: block;
  margin-bottom: 0.5rem;
  font-weight: 500;
}

.form-group input,
.form-group select {
  width: 100%;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 1rem;
  margin-top: 1.5rem;
  padding-top: 1rem;
  border-top: 1px solid var(--border-color);
}
</style>
