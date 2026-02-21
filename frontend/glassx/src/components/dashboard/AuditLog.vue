<template>
  <div class="audit-log">
    <div class="section-header">
      <h2 class="section-title">{{ $t('dashboard.audit_log.title') }}</h2>
      <div class="header-actions">
        <select v-model="actionFilter" class="filter-select">
          <option value="">{{ $t('dashboard.audit_log.all_actions') }}</option>
          <option value="approve">{{ $t('dashboard.audit_log.actions.approve') }}</option>
          <option value="reject">{{ $t('dashboard.audit_log.actions.reject') }}</option>
          <option value="ban">{{ $t('dashboard.audit_log.actions.ban') }}</option>
          <option value="unban">{{ $t('dashboard.audit_log.actions.unban') }}</option>
          <option value="delete">{{ $t('dashboard.audit_log.actions.delete') }}</option>
          <option value="register">{{ $t('dashboard.audit_log.actions.register') }}</option>
          <option value="login">{{ $t('dashboard.audit_log.actions.login') }}</option>
          <option value="logout">{{ $t('dashboard.audit_log.actions.logout') }}</option>
          <option value="password_change">{{ $t('dashboard.audit_log.actions.password_change') }}</option>
          <option value="admin_access_denied">{{ $t('dashboard.audit_log.actions.admin_access_denied') }}</option>
        </select>
        <button @click="loadAuditLogs" :disabled="loading" class="refresh-btn">
          <RefreshCw class="w-4 h-4" :class="{ 'animate-spin': loading }" />
          {{ $t('common.refresh') }}
        </button>
      </div>
    </div>

    <!-- Loading State -->
    <div v-if="loading && !auditLogs.length" class="loading-state">
      <div class="spinner"></div>
      <p>{{ $t('common.loading') }}</p>
    </div>

    <!-- Audit Logs Table -->
    <div v-else class="table-container">
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
            <TableCell class="time-cell">
              {{ formatTime(log.timestamp) }}
            </TableCell>
            <TableCell>
              <span :class="getActionClass(log.action)" class="action-badge">
                {{ $t(`dashboard.audit_log.actions.${log.action}`) || log.action }}
              </span>
            </TableCell>
            <TableCell class="operator-cell">{{ log.operator || 'System' }}</TableCell>
            <TableCell class="target-cell">{{ log.target || '—' }}</TableCell>
            <TableCell class="detail-cell">{{ log.detail || '—' }}</TableCell>
          </TableRow>
          <TableRow v-if="filteredLogs.length === 0">
            <TableCell colspan="5" class="empty-cell">
              <div class="empty-state">
                <FileText class="w-12 h-12" />
                <p>{{ $t('dashboard.audit_log.no_logs') }}</p>
              </div>
            </TableCell>
          </TableRow>
        </TableBody>
      </Table>
    </div>

    <!-- Pagination -->
    <div v-if="totalPages > 1" class="pagination-container">
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
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { RefreshCw, FileText } from 'lucide-vue-next'
import { apiService, type AuditRecord } from '@/services/api'
import { useNotification } from '@/composables/useNotification'
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

const filteredLogs = computed(() => {
  if (!actionFilter.value) return auditLogs.value
  return auditLogs.value.filter(log =>
    log.action.toLowerCase().includes(actionFilter.value.toLowerCase())
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
      return `${baseClasses} bg-gray-500/20 text-gray-300`
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
</script>

<style scoped>
.audit-log {
  max-width: 1200px;
  margin: 0 auto;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1.5rem;
  flex-wrap: wrap;
  gap: 1rem;
}

.section-title {
  font-size: 1.5rem;
  font-weight: 700;
  color: white;
  margin: 0;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.filter-select {
  padding: 0.5rem 1rem;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 10px;
  color: white;
  font-size: 0.875rem;
  cursor: pointer;
}

.filter-select:focus {
  outline: none;
  border-color: rgba(139, 92, 246, 0.5);
}

.filter-select option {
  background: #1a1a1a;
  color: white;
}

.refresh-btn {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.8);
  font-size: 0.875rem;
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

/* Loading State */
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 4rem 2rem;
  color: rgba(255, 255, 255, 0.6);
}

.spinner {
  width: 40px;
  height: 40px;
  border: 3px solid rgba(255, 255, 255, 0.1);
  border-top-color: #8b5cf6;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin-bottom: 1rem;
}

/* Table */
.table-container {
  overflow-x: auto;
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  background: rgba(255, 255, 255, 0.02);
}

.time-cell {
  white-space: nowrap;
  font-size: 0.875rem;
  color: rgba(255, 255, 255, 0.7);
}

.action-badge {
  display: inline-block;
}

.operator-cell,
.target-cell {
  font-weight: 500;
  color: white;
}

.detail-cell {
  max-width: 300px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: rgba(255, 255, 255, 0.7);
  font-size: 0.875rem;
}

.empty-cell {
  padding: 3rem !important;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: rgba(255, 255, 255, 0.4);
  text-align: center;
}

.empty-state p {
  margin: 0.75rem 0 0 0;
}

/* Pagination */
.pagination-container {
  margin-top: 1.5rem;
}

/* Responsive */
@media (max-width: 768px) {
  .section-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .header-actions {
    width: 100%;
    flex-wrap: wrap;
  }

  .filter-select {
    flex: 1;
    min-width: 150px;
  }

  .detail-cell {
    max-width: 150px;
  }
}
</style>
