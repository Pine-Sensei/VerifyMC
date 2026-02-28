<template>
  <div class="w-full space-y-6">
    <div class="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
      <h2 class="text-2xl font-bold text-white">{{ $t('dashboard.audit_log.title') }}</h2>
      <div class="flex items-center gap-3 w-full sm:w-auto">
        <select
          v-model="actionFilter"
          class="flex-1 sm:flex-none px-4 py-2 bg-white/5 border border-white/10 rounded-lg text-white text-sm focus:outline-none focus:border-purple-500/50 appearance-none cursor-pointer hover:bg-white/10 transition-colors"
        >
          <option value="" class="bg-neutral-900">{{ $t('dashboard.audit_log.all_actions') }}</option>
          <option value="approve" class="bg-neutral-900">{{ $t('dashboard.audit_log.actions.approve') }}</option>
          <option value="reject" class="bg-neutral-900">{{ $t('dashboard.audit_log.actions.reject') }}</option>
          <option value="ban" class="bg-neutral-900">{{ $t('dashboard.audit_log.actions.ban') }}</option>
          <option value="unban" class="bg-neutral-900">{{ $t('dashboard.audit_log.actions.unban') }}</option>
          <option value="delete" class="bg-neutral-900">{{ $t('dashboard.audit_log.actions.delete') }}</option>
          <option value="register" class="bg-neutral-900">{{ $t('dashboard.audit_log.actions.register') }}</option>
          <option value="login" class="bg-neutral-900">{{ $t('dashboard.audit_log.actions.login') }}</option>
          <option value="logout" class="bg-neutral-900">{{ $t('dashboard.audit_log.actions.logout') }}</option>
          <option value="password_change" class="bg-neutral-900">{{ $t('dashboard.audit_log.actions.password_change') }}</option>
          <option value="admin_access_denied" class="bg-neutral-900">{{ $t('dashboard.audit_log.actions.admin_access_denied') }}</option>
        </select>
        <Button
          @click="loadAuditLogs"
          :disabled="loading"
          variant="outline"
          class="gap-2"
        >
          <RefreshCw class="w-4 h-4" :class="{ 'animate-spin': loading }" />
          {{ $t('common.refresh') }}
        </Button>
      </div>
    </div>

    <!-- Loading State -->
    <div v-if="loading && !auditLogs.length" class="flex flex-col items-center justify-center py-16 text-white/60">
      <RefreshCw class="w-10 h-10 animate-spin text-purple-500 mb-4" />
      <p>{{ $t('common.loading') }}</p>
    </div>

    <!-- Audit Logs Table -->
    <Card v-else class="overflow-hidden">
      <div class="overflow-x-auto">
        <Table>
          <TableHeader>
          <TableRow>
            <TableHead>{{ $t('dashboard.audit_log.table.time') }}</TableHead>
            <TableHead>{{ $t('dashboard.audit_log.table.action') }}</TableHead>
            <TableHead>{{ $t('dashboard.audit_log.table.operator') }}</TableHead>
            <TableHead>{{ $t('dashboard.audit_log.table.target') }}</TableHead>
            <TableHead>{{ $t('dashboard.audit_log.table.detail') }}</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          <TableRow v-for="log in filteredLogs" :key="log.id || log.timestamp">
            <TableCell class="whitespace-nowrap text-white/70">
              {{ formatTime(log.timestamp) }}
            </TableCell>
            <TableCell>
              <span :class="getActionClass(log.action)" class="inline-block">
                {{ $t(`dashboard.audit_log.actions.${log.action}`) || log.action }}
              </span>
            </TableCell>
            <TableCell class="font-medium text-white">{{ log.operator || 'System' }}</TableCell>
            <TableCell class="font-medium text-white">{{ log.target || '—' }}</TableCell>
            <TableCell class="max-w-xs truncate text-white/70" :title="log.detail || ''">{{ log.detail || '—' }}</TableCell>
          </TableRow>
          <TableRow v-if="filteredLogs.length === 0">
            <TableCell colspan="5" class="h-32 text-center">
              <div class="flex flex-col items-center justify-center text-white/40">
                <FileText class="w-12 h-12 mb-3" />
                <p>{{ $t('dashboard.audit_log.no_logs') }}</p>
              </div>
            </TableCell>
          </TableRow>
          </TableBody>
        </Table>
      </div>
    </Card>

    <!-- Pagination -->
    <div v-if="totalPages > 1" class="mt-6">
      <Pagination
        :current-page="currentPage"
        :total-pages="totalPages"
        :total-count="totalCount"
        :page-size="pageSize"
        :has-next="currentPage < totalPages"
        :has-prev="currentPage > 1"
        @page-change="handlePageChange"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { RefreshCw, FileText } from 'lucide-vue-next'
import { apiService, type AuditRecord } from '@/services/api'
import { useNotification } from '@/composables/useNotification'
import Card from '@/components/ui/Card.vue'
import Button from '@/components/ui/Button.vue'
import Table from '@/components/ui/Table.vue'
import TableHeader from '@/components/ui/TableHeader.vue'
import TableBody from '@/components/ui/TableBody.vue'
import TableRow from '@/components/ui/TableRow.vue'
import TableHead from '@/components/ui/TableHead.vue'
import TableCell from '@/components/ui/TableCell.vue'
import Pagination from '@/components/ui/Pagination.vue'

const { t } = useI18n()
const notification = useNotification()

const loading = ref(false)
const auditLogs = ref<AuditRecord[]>([])
const actionFilter = ref('')
const currentPage = ref(1)
const pageSize = ref(20)
const totalCount = ref(0)
const totalPages = ref(1)

// Debounced filter result
const debouncedFilter = ref('')
let filterDebounceTimer: ReturnType<typeof setTimeout> | null = null

watch(actionFilter, (newValue) => {
  if (filterDebounceTimer) {
    clearTimeout(filterDebounceTimer)
  }
  filterDebounceTimer = setTimeout(() => {
    debouncedFilter.value = newValue
  }, 300)
})

const filteredLogs = computed(() => {
  if (!debouncedFilter.value) return auditLogs.value
  return auditLogs.value.filter(log =>
    log.action.toLowerCase().includes(debouncedFilter.value.toLowerCase())
  )
})

const formatTime = (timestamp: number): string => {
  const date = new Date(timestamp)
  return date.toLocaleString()
}

const getActionClass = (action: string): string => {
  const actionLower = action.toLowerCase()
  const baseClasses = 'px-2 py-1 rounded-full text-xs font-medium'

  switch (actionLower) {
    case 'approve':
      return `${baseClasses} bg-green-500/20 text-green-300`
    case 'reject':
      return `${baseClasses} bg-red-500/20 text-red-300`
    case 'ban':
      return `${baseClasses} bg-red-500/20 text-red-300`
    case 'unban':
      return `${baseClasses} bg-blue-500/20 text-blue-300`
    case 'delete':
      return `${baseClasses} bg-orange-500/20 text-orange-300`
    case 'register':
      return `${baseClasses} bg-purple-500/20 text-purple-300`
    case 'login':
      return `${baseClasses} bg-cyan-500/20 text-cyan-300`
    case 'logout':
      return `${baseClasses} bg-slate-500/20 text-slate-300`
    case 'password_change':
      return `${baseClasses} bg-amber-500/20 text-amber-300`
    case 'admin_access_denied':
      return `${baseClasses} bg-rose-500/20 text-rose-300`
    default:
      return `${baseClasses} bg-white/10 text-white/70`
  }
}

const loadAuditLogs = async () => {
  loading.value = true
  try {
    const response = await apiService.getAuditLogs()
    if (response.success) {
      auditLogs.value = response.audits || []
      totalCount.value = auditLogs.value.length
      totalPages.value = Math.ceil(totalCount.value / pageSize.value)
    } else {
      notification.error(response.message || t('common.error'))
    }
  } catch (error) {
    console.error('Failed to load audit logs:', error)
    notification.error(t('common.error'))
  } finally {
    loading.value = false
  }
}

const handlePageChange = (page: number) => {
  currentPage.value = page
}

onMounted(() => {
  loadAuditLogs()
})

onUnmounted(() => {
  if (filterDebounceTimer) {
    clearTimeout(filterDebounceTimer)
    filterDebounceTimer = null
  }
})
</script>
