<template>
  <div class="user-management">
    <Tabs :tabs="tabs" :default-tab="activeTab" @tab-change="onTabChange">
      <template #default="{ activeTab }">
        <!-- Pending Users Tab -->
        <div v-if="activeTab === 'review'" class="tab-content">
          <div class="tab-header">
            <h3 class="tab-title">{{ $t('admin.review.title') }}</h3>
            <button
              @click="loadPendingUsers"
              :disabled="loading"
              class="refresh-btn"
              :title="$t('common.refresh')"
            >
              <RefreshCw class="w-5 h-5" :class="{ 'animate-spin': loading }" />
            </button>
          </div>

          <div class="table-container">
            <Table class="min-w-[600px]">
              <TableHeader>
                <TableRow>
                  <TableHead>{{ $t('admin.review.table.username') }}</TableHead>
                  <TableHead>{{ $t('admin.review.table.email') }}</TableHead>
                  <TableHead>{{ $t('admin.review.table.register_time') }}</TableHead>
                  <TableHead v-if="showQuestionnaireScoreColumn">{{ $t('admin.review.table.questionnaire_score') }}</TableHead>
                  <TableHead v-if="showQuestionnaireReasonColumn">{{ $t('admin.review.table.questionnaire_reason') }}</TableHead>
                  <TableHead class="w-[150px]">{{ $t('admin.review.table.actions') }}</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                <TableRow v-for="user in pendingUsers" :key="user.username">
                  <TableCell class="font-medium text-white">{{ user.username }}</TableCell>
                  <TableCell>{{ user.email }}</TableCell>
                  <TableCell>{{ formatDate(user.regTime) }}</TableCell>
                  <TableCell v-if="showQuestionnaireScoreColumn">{{ user.questionnaireScore ?? '—' }}</TableCell>
                  <TableCell v-if="showQuestionnaireReasonColumn" class="max-w-[360px] break-words">{{ user.questionnaireReviewSummary || '—' }}</TableCell>
                  <TableCell>
                    <div class="flex space-x-2">
                      <Button
                        variant="outline"
                        size="sm"
                        @click="approveUser(user)"
                        :disabled="loading || processingUsers.has(user.username)"
                      >
                        {{ $t('admin.review.actions.approve') }}
                      </Button>
                      <Button
                        variant="outline"
                        size="sm"
                        @click="openRejectDialog(user)"
                        :disabled="loading || processingUsers.has(user.username)"
                      >
                        {{ $t('admin.review.actions.reject') }}
                      </Button>
                    </div>
                  </TableCell>
                </TableRow>
                <TableRow v-if="pendingUsers.length === 0">
                  <TableCell :colspan="reviewTableColspan" class="text-center py-8 text-white/60">
                    {{ $t('admin.review.no_pending') }}
                  </TableCell>
                </TableRow>
              </TableBody>
            </Table>
          </div>
        </div>

        <!-- All Users Tab -->
        <div v-if="activeTab === 'users'" class="tab-content">
          <div class="tab-header">
            <h3 class="tab-title">{{ $t('admin.users.title') }}</h3>
            <button
              @click="loadAllUsers"
              :disabled="loading"
              class="refresh-btn"
              :title="$t('common.refresh')"
            >
              <RefreshCw class="w-5 h-5" :class="{ 'animate-spin': loading }" />
            </button>
          </div>

          <div class="search-container">
            <SearchBar
              v-model="searchQuery"
              :placeholder="$t('admin.users.search_placeholder')"
            />
          </div>

          <div class="table-container">
            <Table class="min-w-[600px] text-xs sm:text-sm">
              <TableHeader>
                <TableRow>
                  <TableHead>{{ $t('admin.users.table.username') }}</TableHead>
                  <TableHead>{{ $t('admin.users.table.email') }}</TableHead>
                  <TableHead>{{ $t('admin.users.table.status') }}</TableHead>
                  <TableHead>{{ $t('admin.users.table.register_time') }}</TableHead>
                  <TableHead class="w-[200px]">{{ $t('admin.users.table.actions') }}</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                <TableRow v-for="user in allUsers" :key="user.username">
                  <TableCell class="font-medium text-white break-all">{{ user.username }}</TableCell>
                  <TableCell class="break-all">{{ user.email }}</TableCell>
                  <TableCell>
                    <span :class="getStatusClass(user.status) + ' inline-block px-2 py-1 text-xs font-medium rounded-full whitespace-nowrap'">
                      {{ $t(`admin.users.status.${(user.status || '').toLowerCase()}`) }}
                    </span>
                  </TableCell>
                  <TableCell>{{ formatDate(user.regTime) }}</TableCell>
                  <TableCell>
                    <div class="flex items-center gap-2">
                      <button
                        @click="showPasswordDialog = true; selectedUser = user; newPassword = ''"
                        :disabled="loading"
                        class="action-btn blue"
                        :title="$t('admin.users.actions.change_password')"
                      >
                        <Key class="w-4 h-4" />
                      </button>
                      <button
                        @click="showDeleteConfirm(user)"
                        :disabled="loading"
                        class="action-btn red"
                        :title="$t('admin.users.actions.delete')"
                      >
                        <Trash2 class="w-4 h-4" />
                      </button>
                      <button
                        v-if="user.status !== 'banned'"
                        @click="showBanConfirm(user)"
                        :disabled="loading"
                        class="action-btn red"
                        :title="$t('admin.users.actions.ban')"
                      >
                        <Ban class="w-4 h-4" />
                      </button>
                      <button
                        v-if="user.status === 'banned'"
                        @click="showUnbanConfirm(user)"
                        :disabled="loading"
                        class="action-btn green"
                        :title="$t('admin.users.actions.unban')"
                      >
                        <CheckCircle class="w-4 h-4" />
                      </button>
                    </div>
                  </TableCell>
                </TableRow>
                <TableRow v-if="allUsers.length === 0">
                  <TableCell colspan="5" class="text-center py-8 text-white/60">
                    {{ searchQuery ? $t('admin.users.no_search_results') : $t('admin.users.no_users') }}
                  </TableCell>
                </TableRow>
              </TableBody>
            </Table>
          </div>

          <Pagination
            :current-page="currentPage"
            :total-pages="totalPages"
            :total-count="totalCount"
            :page-size="pageSize"
            :has-next="hasNext"
            :has-prev="hasPrev"
            @page-change="handlePageChange"
            @page-size-change="handlePageSizeChange"
          />
        </div>
      </template>
    </Tabs>

    <!-- Confirm Dialogs -->
    <ConfirmDialog
      :show="showDeleteDialog"
      :title="$t('admin.users.delete_modal.title')"
      :message="$t('admin.users.delete_modal.message').replace('{username}', selectedUser?.username || '')"
      :confirm-text="$t('admin.users.delete_modal.confirm')"
      :cancel-text="$t('admin.users.delete_modal.cancel')"
      type="danger"
      @confirm="confirmDelete"
      @cancel="showDeleteDialog = false"
    />

    <ConfirmDialog
      :show="showBanDialog"
      :title="$t('admin.users.ban_modal.title')"
      :message="$t('admin.users.ban_modal.message').replace('{username}', selectedUser?.username || '')"
      :confirm-text="$t('admin.users.ban_modal.confirm')"
      :cancel-text="$t('admin.users.ban_modal.cancel')"
      type="danger"
      @confirm="confirmBan"
      @cancel="showBanDialog = false"
    />

    <ConfirmDialog
      :show="showUnbanDialog"
      :title="$t('admin.users.unban_modal.title')"
      :message="$t('admin.users.unban_modal.message').replace('{username}', selectedUser?.username || '')"
      :confirm-text="$t('admin.users.unban_modal.confirm')"
      :cancel-text="$t('admin.users.unban_modal.cancel')"
      type="info"
      @confirm="confirmUnban"
      @cancel="showUnbanDialog = false"
    />

    <!-- Reject Dialog -->
    <div v-if="rejectDialog.show" class="modal-overlay">
      <div class="modal-backdrop" @click="closeRejectDialog"></div>
      <div class="modal-dialog">
        <h3 class="modal-title">{{ $t('admin.review.reject_modal.title') }}</h3>
        <div class="modal-content">
          <Label for="rejectReason" class="modal-label">{{ $t('admin.review.reject_modal.reason_label') }}</Label>
          <textarea
            id="rejectReason"
            v-model="rejectDialog.reason"
            class="modal-textarea"
            :placeholder="$t('admin.review.reject_modal.reason_placeholder')"
          />
        </div>
        <div class="modal-actions">
          <Button variant="outline" @click="closeRejectDialog" class="btn-cancel">
            {{ $t('admin.review.reject_modal.cancel') }}
          </Button>
          <Button @click="confirmReject" :disabled="rejectDialog.processing" class="btn-danger">
            {{ $t('admin.review.reject_modal.confirm') }}
          </Button>
        </div>
      </div>
    </div>

    <!-- Password Dialog -->
    <div v-if="showPasswordDialog" class="modal-overlay">
      <div class="modal-backdrop" @click="showPasswordDialog = false"></div>
      <div class="modal-dialog">
        <h3 class="modal-title">{{ $t('admin.users.change_password_modal.title') }}</h3>
        <div class="modal-content">
          <Label forId="newPassword" class="modal-label">{{ $t('admin.users.change_password_modal.new_password') }}</Label>
          <input
            id="newPassword"
            v-model="newPassword"
            type="password"
            class="modal-input"
            :placeholder="$t('admin.users.change_password_modal.password_placeholder')"
          />
        </div>
        <div class="modal-actions">
          <Button variant="outline" @click="showPasswordDialog = false" class="btn-cancel">
            {{ $t('common.cancel') }}
          </Button>
          <Button @click="confirmChangePassword" :disabled="!newPassword" class="btn-primary">
            {{ $t('common.save') }}
          </Button>
        </div>
      </div>
    </div>

    <!-- Version Update Notification -->
    <VersionUpdateNotification />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, inject, onMounted, onUnmounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { RefreshCw, Key, Trash2, Ban, CheckCircle } from 'lucide-vue-next'
import { useNotification } from '@/composables/useNotification'
import { useAdminUsers } from '@/composables/useAdminUsers'
import { apiService, type PendingUser } from '@/services/api'
import { sessionService } from '@/services/session'
import Tabs from '@/components/ui/Tabs.vue'
import Table from '@/components/ui/Table.vue'
import TableHeader from '@/components/ui/TableHeader.vue'
import TableBody from '@/components/ui/TableBody.vue'
import TableRow from '@/components/ui/TableRow.vue'
import TableHead from '@/components/ui/TableHead.vue'
import TableCell from '@/components/ui/TableCell.vue'
import Button from '@/components/ui/Button.vue'
import Label from '@/components/ui/Label.vue'
import ConfirmDialog from '@/components/ui/ConfirmDialog.vue'
import SearchBar from '@/components/ui/SearchBar.vue'
import Pagination from '@/components/ui/Pagination.vue'
import VersionUpdateNotification from '@/components/ui/VersionUpdateNotification.vue'

const { t, locale } = useI18n()
const notification = useNotification()

const getWsPort = inject<() => number>('getWsPort', () => window.location.port ? (parseInt(window.location.port, 10) + 1) : 8081)

const activeTab = ref('review')
const actionLoading = ref(false)
const pendingUsers = ref<PendingUser[]>([])
const questionnaireEnabled = ref(false)
const questionnaireHasTextQuestions = ref(false)
let ws: WebSocket | null = null

const showQuestionnaireScoreColumn = computed(() => questionnaireEnabled.value)
const showQuestionnaireReasonColumn = computed(() => questionnaireEnabled.value && questionnaireHasTextQuestions.value)
const reviewTableColspan = computed(() => 4 + (showQuestionnaireScoreColumn.value ? 1 : 0) + (showQuestionnaireReasonColumn.value ? 1 : 0))

const {
  loading: usersLoading,
  allUsers,
  searchQuery,
  currentPage,
  pageSize,
  totalCount,
  totalPages,
  hasNext,
  hasPrev,
  loadAllUsers,
  handlePageChange,
  handlePageSizeChange,
  resetUsersPagination,
} = useAdminUsers({ locale, t, notification })

const loading = computed(() => usersLoading.value || actionLoading.value)

const tabs = computed(() => [
  { value: 'review', label: t('admin.tabs.review') },
  { value: 'users', label: t('admin.tabs.users') }
])

const showDeleteDialog = ref(false)
const showBanDialog = ref(false)
const showUnbanDialog = ref(false)
const showPasswordDialog = ref(false)
const selectedUser = ref<PendingUser | null>(null)
const newPassword = ref('')
const processingUsers = ref(new Set<string>())

const rejectDialog = ref({
  show: false,
  user: null as PendingUser | null,
  reason: '',
  processing: false
})

const formatDate = (dateValue: string | number) => {
  if (!dateValue) return '—'
  const date = typeof dateValue === 'number' ? new Date(dateValue) : new Date(dateValue)
  return date.toLocaleDateString()
}

const getStatusClass = (status: string) => {
  const s = (status || '').toLowerCase()
  const baseClasses = 'px-2 py-1 rounded-full text-xs font-medium'
  switch (s) {
    case 'pending':
      return `${baseClasses} bg-yellow-500/20 text-yellow-300`
    case 'approved':
      return `${baseClasses} bg-green-500/20 text-green-300`
    case 'rejected':
      return `${baseClasses} bg-red-500/20 text-red-300`
    case 'banned':
      return `${baseClasses} bg-red-500/20 text-red-300`
    default:
      return `${baseClasses} bg-gray-500/20 text-gray-300`
  }
}

const loadQuestionnaireConfig = async () => {
  try {
    const config = await apiService.getConfig()
    questionnaireEnabled.value = Boolean(config.questionnaire?.enabled)
    questionnaireHasTextQuestions.value = Boolean(config.questionnaire?.hasTextQuestions)
  } catch (error) {
    console.error('Failed to load questionnaire config:', error)
    questionnaireEnabled.value = false
    questionnaireHasTextQuestions.value = false
  }
}

const loadPendingUsers = async () => {
  actionLoading.value = true
  try {
    const response = await apiService.getPendingList(locale.value)
    if (response.success) {
      pendingUsers.value = response.users || []
    } else {
      notification.error(response.message || t('admin.review.messages.error'))
    }
  } catch (error) {
    notification.error(t('admin.review.messages.error'))
  } finally {
    actionLoading.value = false
  }
}

const notifyResult = (success: boolean, key: string, backendMessage?: string) => {
  const displayMessage = backendMessage || t(key)
  if (success) {
    notification.success(displayMessage)
  } else {
    notification.error(displayMessage)
  }
}

const openRejectDialog = (user: PendingUser) => {
  rejectDialog.value = {
    show: true,
    user,
    reason: '',
    processing: false
  }
}

const closeRejectDialog = () => {
  rejectDialog.value = {
    show: false,
    user: null,
    reason: '',
    processing: false
  }
}

const approveUser = async (user: PendingUser) => {
  processingUsers.value.add(user.username)
  actionLoading.value = true

  try {
    const response = await apiService.reviewUser({
      username: user.username,
      action: 'approve',
      language: locale.value
    })

    if (response.success) {
      notifyResult(true, 'admin.review.messages.approve_success', response.message)
      await apiService.syncAuthme(locale.value)
      await loadPendingUsers()
      await loadAllUsers()
    } else {
      notifyResult(false, 'admin.review.messages.error', response.message)
    }
  } catch (error) {
    notification.error(t('admin.review.messages.error'))
  } finally {
    processingUsers.value.delete(user.username)
    actionLoading.value = false
  }
}

const confirmReject = async () => {
  if (!rejectDialog.value.user) return

  rejectDialog.value.processing = true
  processingUsers.value.add(rejectDialog.value.user.username)
  actionLoading.value = true

  try {
    const response = await apiService.reviewUser({
      username: rejectDialog.value.user.username,
      action: 'reject',
      reason: rejectDialog.value.reason || undefined,
      language: locale.value
    })

    if (response.success) {
      notifyResult(true, 'admin.review.messages.reject_success', response.message)
      await loadPendingUsers()
      await loadAllUsers()
      closeRejectDialog()
    } else {
      notifyResult(false, 'admin.review.messages.error', response.message)
    }
  } catch (error) {
    notification.error(t('admin.review.messages.error'))
  } finally {
    rejectDialog.value.processing = false
    if (rejectDialog.value.user?.username) {
      processingUsers.value.delete(rejectDialog.value.user.username)
    }
    actionLoading.value = false
  }
}

const showDeleteConfirm = (user: PendingUser) => {
  selectedUser.value = user
  showDeleteDialog.value = true
}

const showBanConfirm = (user: PendingUser) => {
  selectedUser.value = user
  showBanDialog.value = true
}

const showUnbanConfirm = (user: PendingUser) => {
  selectedUser.value = user
  showUnbanDialog.value = true
}

const confirmDelete = async () => {
  if (!selectedUser.value) return

  actionLoading.value = true
  showDeleteDialog.value = false

  try {
    const response = await apiService.deleteUser(selectedUser.value.username, locale.value)

    if (response.success) {
      notifyResult(true, 'admin.users.messages.delete_success', response.message)
      await loadAllUsers()
    } else {
      notifyResult(false, 'admin.users.messages.error', response.message)
    }
  } catch (error) {
    notification.error(t('admin.users.messages.error'))
  } finally {
    actionLoading.value = false
    selectedUser.value = null
  }
}

const confirmBan = async () => {
  if (!selectedUser.value) return

  actionLoading.value = true
  showBanDialog.value = false

  try {
    const response = await apiService.banUser(selectedUser.value.username, locale.value)

    if (response.success) {
      notifyResult(true, 'admin.users.messages.ban_success', response.message)
      await loadAllUsers()
    } else {
      notifyResult(false, 'admin.users.messages.error', response.message)
    }
  } catch (error) {
    notification.error(t('admin.users.messages.error'))
  } finally {
    actionLoading.value = false
    selectedUser.value = null
  }
}

const confirmUnban = async () => {
  if (!selectedUser.value) return

  actionLoading.value = true
  showUnbanDialog.value = false

  try {
    const response = await apiService.unbanUser(selectedUser.value.username, locale.value)

    if (response.success) {
      notifyResult(true, 'admin.users.messages.unban_success', response.message)
      await loadAllUsers()
    } else {
      notifyResult(false, 'admin.users.messages.error', response.message)
    }
  } catch (error) {
    notification.error(t('admin.users.messages.error'))
  } finally {
    actionLoading.value = false
    selectedUser.value = null
  }
}

const confirmChangePassword = async () => {
  if (!selectedUser.value || !newPassword.value) return

  actionLoading.value = true

  try {
    const response = await apiService.changePassword({
      username: selectedUser.value.username,
      password: newPassword.value,
      language: locale.value
    })

    if (response.success) {
      notifyResult(true, 'admin.users.messages.password_change_success', response.message)
      showPasswordDialog.value = false
      newPassword.value = ''
      selectedUser.value = null
    } else {
      notifyResult(false, 'admin.users.messages.error', response.message)
    }
  } catch (error) {
    notification.error(t('admin.users.messages.error'))
  } finally {
    actionLoading.value = false
  }
}

const onTabChange = async (tab: string) => {
  activeTab.value = tab
  if (tab === 'review') {
    await loadQuestionnaireConfig()
    loadPendingUsers()
  } else if (tab === 'users') {
    resetUsersPagination()
    loadAllUsers()
  }
}

onMounted(async () => {
  await loadQuestionnaireConfig()
  loadPendingUsers()
  loadAllUsers()

  // WebSocket for real-time updates
  if (window.WebSocket) {
    const wsProtocol = window.location.protocol === 'https:' ? 'wss' : 'ws'
    const wsHost = window.location.hostname
    const wsPort = getWsPort()
    const token = sessionService.getToken()
    const wsUrl = token
      ? `${wsProtocol}://${wsHost}:${wsPort}/?token=${encodeURIComponent(token)}`
      : `${wsProtocol}://${wsHost}:${wsPort}`
    try {
      ws = new WebSocket(wsUrl)
      ws.onmessage = () => {
        loadPendingUsers()
        loadAllUsers()
      }
      ws.onerror = () => {
        console.warn('WebSocket connection error')
      }
      ws.onclose = () => {
        ws = null
      }
    } catch {
      console.warn('WebSocket connection failed')
    }
  }
})

onUnmounted(() => {
  if (ws && ws.readyState === WebSocket.OPEN) {
    ws.close()
  }
})
</script>

<style scoped>
.user-management {
  width: 100%;
}

.tab-content {
  padding-top: 1rem;
}

.tab-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

.tab-title {
  font-size: 1.25rem;
  font-weight: 600;
  color: white;
  margin: 0;
}

.refresh-btn {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.8);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
}

.refresh-btn:hover {
  background: rgba(255, 255, 255, 0.1);
  color: white;
}

.refresh-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.animate-spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.search-container {
  max-width: 400px;
  margin-bottom: 1rem;
}

.table-container {
  overflow-x: auto;
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.action-btn {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid;
  cursor: pointer;
  transition: all 0.2s;
}

.action-btn.blue {
  background: rgba(59, 130, 246, 0.1);
  border-color: rgba(59, 130, 246, 0.3);
  color: #3b82f6;
}

.action-btn.blue:hover {
  background: rgba(59, 130, 246, 0.2);
}

.action-btn.red {
  background: rgba(239, 68, 68, 0.1);
  border-color: rgba(239, 68, 68, 0.3);
  color: #ef4444;
}

.action-btn.red:hover {
  background: rgba(239, 68, 68, 0.2);
}

.action-btn.green {
  background: rgba(34, 197, 94, 0.1);
  border-color: rgba(34, 197, 94, 0.3);
  color: #22c55e;
}

.action-btn.green:hover {
  background: rgba(34, 197, 94, 0.2);
}

.action-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* Modal Styles */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 99999;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1rem;
}

.modal-backdrop {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.7);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  z-index: 99998;
}

.modal-dialog {
  position: relative;
  background: rgba(15, 15, 15, 0.95);
  backdrop-filter: blur(24px) saturate(180%);
  -webkit-backdrop-filter: blur(24px) saturate(180%);
  border-radius: 16px;
  padding: 1.5rem;
  width: 100%;
  max-width: 28rem;
  border: 1px solid rgba(255, 255, 255, 0.15);
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.6);
  z-index: 99999;
}

.modal-title {
  font-size: 1.25rem;
  font-weight: 600;
  color: white;
  margin: 0 0 1.25rem 0;
  text-align: center;
}

.modal-content {
  margin-bottom: 1.25rem;
}

.modal-label {
  display: block;
  font-size: 0.875rem;
  font-weight: 500;
  color: white;
  margin-bottom: 0.5rem;
}

.modal-textarea {
  width: 100%;
  padding: 0.75rem;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 10px;
  color: white;
  font-size: 0.875rem;
  resize: vertical;
  min-height: 80px;
}

.modal-textarea::placeholder {
  color: rgba(255, 255, 255, 0.4);
}

.modal-textarea:focus {
  outline: none;
  border-color: rgba(139, 92, 246, 0.5);
}

.modal-input {
  width: 100%;
  padding: 0.75rem;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 10px;
  color: white;
  font-size: 0.875rem;
}

.modal-input::placeholder {
  color: rgba(255, 255, 255, 0.4);
}

.modal-input:focus {
  outline: none;
  border-color: rgba(139, 92, 246, 0.5);
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
}

.btn-cancel {
  background: rgba(255, 255, 255, 0.1);
  border-color: rgba(255, 255, 255, 0.2);
  color: white;
}

.btn-cancel:hover {
  background: rgba(255, 255, 255, 0.15);
}

.btn-danger {
  background: #ef4444;
  border-color: #ef4444;
  color: white;
}

.btn-danger:hover {
  background: #dc2626;
}

.btn-primary {
  background: #3b82f6;
  border-color: #3b82f6;
  color: white;
}

.btn-primary:hover {
  background: #2563eb;
}
</style>
