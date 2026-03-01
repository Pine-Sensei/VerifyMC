<template>
  <div class="space-y-4">
    <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
      <div class="w-full sm:max-w-md order-2 sm:order-1">
        <SearchBar
          v-model="searchQuery"
          :placeholder="$t('admin.users.search_placeholder')"
        />
      </div>
      <div class="flex items-center justify-end order-1 sm:order-2">
        <Button
          @click="loadAllUsers"
          :disabled="loading"
          variant="outline"
          class="gap-2"
          :title="$t('common.refresh')"
        >
          <RefreshCw class="w-5 h-5" :class="{ 'animate-spin': loading }" />
          <span>{{ $t('common.refresh') }}</span>
        </Button>
      </div>
    </div>

    <Card class="overflow-hidden">
      <div class="overflow-x-auto">
        <Table class="min-w-full text-xs sm:text-sm">
          <TableHeader>
            <TableRow>
              <TableHead>{{ $t('admin.users.table.username') }}</TableHead>
              <TableHead>{{ $t('admin.users.table.email') }}</TableHead>
              <TableHead>{{ $t('admin.users.table.status') }}</TableHead>
              <TableHead>{{ $t('admin.users.table.register_time') }}</TableHead>
              <TableHead class="w-48">{{ $t('admin.users.table.actions') }}</TableHead>
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
                  <Button
                    @click="showPasswordDialog = true; selectedUser = user; newPassword = ''"
                    :disabled="loading"
                    variant="ghost"
                    class="h-8 text-blue-400 hover:text-blue-300 hover:bg-blue-500/10 px-2"
                    :title="$t('admin.users.actions.change_password')"
                  >
                    <Key class="w-4 h-4 xl:mr-1" />
                    <span class="hidden xl:inline">{{ $t('admin.users.actions.change_password') }}</span>
                  </Button>
                  <Button
                    @click="showDeleteConfirm(user)"
                    :disabled="loading"
                    variant="ghost"
                    class="h-8 text-red-400 hover:text-red-300 hover:bg-red-500/10 px-2"
                    :title="$t('admin.users.actions.delete')"
                  >
                    <Trash2 class="w-4 h-4 xl:mr-1" />
                    <span class="hidden xl:inline">{{ $t('admin.users.actions.delete') }}</span>
                  </Button>
                  <Button
                    v-if="user.status !== 'banned'"
                    @click="showBanConfirm(user)"
                    :disabled="loading"
                    variant="ghost"
                    class="h-8 text-red-400 hover:text-red-300 hover:bg-red-500/10 px-2"
                    :title="$t('admin.users.actions.ban')"
                  >
                    <Ban class="w-4 h-4 xl:mr-1" />
                    <span class="hidden xl:inline">{{ $t('admin.users.actions.ban') }}</span>
                  </Button>
                  <Button
                    v-if="user.status === 'banned'"
                    @click="showUnbanConfirm(user)"
                    :disabled="loading"
                    variant="ghost"
                    class="h-8 text-green-400 hover:text-green-300 hover:bg-green-500/10 px-2"
                    :title="$t('admin.users.actions.unban')"
                  >
                    <CheckCircle class="w-4 h-4 xl:mr-1" />
                    <span class="hidden xl:inline">{{ $t('admin.users.actions.unban') }}</span>
                  </Button>
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
    </Card>

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

    <!-- Password Dialog -->
    <Dialog
      :show="showPasswordDialog"
      :title="$t('admin.users.change_password_modal.title')"
      @close="showPasswordDialog = false"
    >
      <div class="space-y-4">
        <div>
          <Label forId="newPassword" class="block text-sm font-medium text-white mb-2">{{ $t('admin.users.change_password_modal.new_password') }}</Label>
          <Input
            id="newPassword"
            v-model="newPassword"
            type="password"
            :placeholder="$t('admin.users.change_password_modal.password_placeholder')"
          />
        </div>
      </div>
      <template #footer>
        <Button variant="outline" @click="showPasswordDialog = false">
          {{ $t('common.cancel') }}
        </Button>
        <Button @click="confirmChangePassword" :disabled="!newPassword" variant="default">
          {{ $t('common.save') }}
        </Button>
      </template>
    </Dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  RefreshCw,
  Key,
  Trash2,
  Ban,
  CheckCircle,
  Search,
  Filter,
  X,
} from 'lucide-vue-next'
import { useNotification } from '@/composables/useNotification'
import { useAdminUsers } from '@/composables/useAdminUsers'
import { apiService, type PendingUser } from '@/services/api'
import Card from '@/components/ui/Card.vue'
import Table from '@/components/ui/Table.vue'
import TableHeader from '@/components/ui/TableHeader.vue'
import TableBody from '@/components/ui/TableBody.vue'
import TableRow from '@/components/ui/TableRow.vue'
import TableHead from '@/components/ui/TableHead.vue'
import TableCell from '@/components/ui/TableCell.vue'
import Button from '@/components/ui/Button.vue'
import Label from '@/components/ui/Label.vue'
import Input from '@/components/ui/Input.vue'
import ConfirmDialog from '@/components/ui/ConfirmDialog.vue'
import Dialog from '@/components/ui/Dialog.vue'
import SearchBar from '@/components/ui/SearchBar.vue'
import Pagination from '@/components/ui/Pagination.vue'

const { t, locale } = useI18n()
const notification = useNotification()

const actionLoading = ref(false)
const showDeleteDialog = ref(false)
const showBanDialog = ref(false)
const showUnbanDialog = ref(false)
const showPasswordDialog = ref(false)
const selectedUser = ref<PendingUser | null>(null)
const newPassword = ref('')

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
      return `${baseClasses} bg-white/5 text-white/70`
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

onMounted(() => {
  loadAllUsers()
})

defineExpose({
  loadAllUsers,
  resetUsersPagination
})
</script>
