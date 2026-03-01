<template>
  <div class="space-y-4">
    <div class="flex items-center justify-end">
      <Button
        @click="loadPendingUsers"
        :disabled="loading"
        variant="outline"
        class="gap-2"
        :title="$t('common.refresh')"
      >
        <RefreshCw class="w-5 h-5" :class="{ 'animate-spin': loading }" />
        <span>{{ $t('common.refresh') }}</span>
      </Button>
    </div>

    <Card class="overflow-hidden">
      <div class="overflow-x-auto">
        <Table class="min-w-full">
          <TableHeader>
            <TableRow>
              <TableHead>{{ $t('admin.review.table.username') }}</TableHead>
              <TableHead>{{ $t('admin.review.table.email') }}</TableHead>
              <TableHead>{{ $t('admin.review.table.register_time') }}</TableHead>
              <TableHead v-if="showQuestionnaireScoreColumn">{{ $t('admin.review.table.questionnaire_score') }}</TableHead>
              <TableHead v-if="showQuestionnaireReasonColumn">{{ $t('admin.review.table.questionnaire_reason') }}</TableHead>
              <TableHead class="w-40">{{ $t('admin.review.table.actions') }}</TableHead>
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
                    class="h-8 text-green-400 border-green-500/30 hover:bg-green-500/10 hover:text-green-300"
                    @click="approveUser(user)"
                    :disabled="loading || processingUsers.has(user.username)"
                  >
                    {{ $t('admin.review.actions.approve') }}
                  </Button>
                  <Button
                    variant="outline"
                    size="sm"
                    class="h-8 text-red-400 border-red-500/30 hover:bg-red-500/10 hover:text-red-300"
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
    </Card>

    <!-- Reject Dialog -->
    <Dialog
      :show="rejectDialog.show"
      :title="$t('admin.review.reject_modal.title')"
      @close="closeRejectDialog"
    >
      <div class="space-y-4">
        <div>
          <Label for="rejectReason" class="block text-sm font-medium text-white mb-2">{{ $t('admin.review.reject_modal.reason_label') }}</Label>
          <textarea
            id="rejectReason"
            v-model="rejectDialog.reason"
            class="w-full px-3 py-2 bg-white/5 border border-white/10 rounded-lg text-white text-sm focus:outline-none focus:border-purple-500/50 min-h-24 resize-y placeholder-white/30"
            :placeholder="$t('admin.review.reject_modal.reason_placeholder')"
          />
        </div>
      </div>
      <template #footer>
        <Button variant="outline" @click="closeRejectDialog">
          {{ $t('admin.review.reject_modal.cancel') }}
        </Button>
        <Button @click="confirmReject" :disabled="rejectDialog.processing" variant="destructive">
          {{ $t('admin.review.reject_modal.confirm') }}
        </Button>
      </template>
    </Dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { RefreshCw } from 'lucide-vue-next'
import { useNotification } from '@/composables/useNotification'
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
import Dialog from '@/components/ui/Dialog.vue'

const { t, locale } = useI18n()
const notification = useNotification()

const actionLoading = ref(false)
const pendingUsers = ref<PendingUser[]>([])
const questionnaireEnabled = ref(false)
const questionnaireHasTextQuestions = ref(false)
const processingUsers = ref(new Set<string>())

const loading = computed(() => actionLoading.value)

const showQuestionnaireScoreColumn = computed(() => questionnaireEnabled.value)
const showQuestionnaireReasonColumn = computed(() => questionnaireEnabled.value && questionnaireHasTextQuestions.value)
const reviewTableColspan = computed(() => 4 + (showQuestionnaireScoreColumn.value ? 1 : 0) + (showQuestionnaireReasonColumn.value ? 1 : 0))

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

onMounted(async () => {
  await loadQuestionnaireConfig()
  loadPendingUsers()
})

defineExpose({
  loadPendingUsers
})
</script>
