<template>
  <div class="server-status">
    <div class="section-header">
      <h2 class="section-title">{{ $t('dashboard.server_status.title') }}</h2>
      <button @click="refreshStatus" :disabled="loading" class="refresh-btn">
        <RefreshCw class="w-4 h-4" :class="{ 'animate-spin': loading }" />
        {{ $t('common.refresh') }}
      </button>
    </div>

    <!-- Loading State -->
    <div v-if="loading && !status" class="loading-state">
      <div class="spinner"></div>
      <p>{{ $t('common.loading') }}</p>
    </div>

    <!-- Status Content -->
    <div v-else class="status-content">
      <!-- Server Status Card -->
      <div class="status-card main-status" :class="status?.online ? 'online' : 'offline'">
        <div class="status-indicator">
          <div class="indicator-dot" :class="status?.online ? 'online' : 'offline'"></div>
          <span class="indicator-text">{{ status?.online ? $t('dashboard.server_status.online') : $t('dashboard.server_status.offline') }}</span>
        </div>
        <div class="server-motd" v-if="status?.motd">
          {{ status.motd }}
        </div>
      </div>

      <!-- Stats Grid -->
      <div class="stats-grid">
        <!-- Players -->
        <div class="stat-card">
          <div class="stat-icon players">
            <Users class="w-6 h-6" />
          </div>
          <div class="stat-info">
            <span class="stat-value">{{ status?.players?.online ?? 0 }} / {{ status?.players?.max ?? 0 }}</span>
            <span class="stat-label">{{ $t('dashboard.server_status.players') }}</span>
          </div>
        </div>

        <!-- TPS -->
        <div class="stat-card">
          <div class="stat-icon tps" :class="tpsClass">
            <Activity class="w-6 h-6" />
          </div>
          <div class="stat-info">
            <span class="stat-value">{{ status?.tps?.toFixed(1) ?? '—' }}</span>
            <span class="stat-label">{{ $t('dashboard.server_status.tps') }}</span>
          </div>
        </div>

        <!-- Memory -->
        <div class="stat-card">
          <div class="stat-icon memory">
            <Database class="w-6 h-6" />
          </div>
          <div class="stat-info">
            <span class="stat-value">{{ formatMemory(status?.memory?.used) }} / {{ formatMemory(status?.memory?.max) }}</span>
            <span class="stat-label">{{ $t('dashboard.server_status.memory') }}</span>
          </div>
          <div class="memory-bar" v-if="status?.memory">
            <div class="memory-usage" :style="{ width: memoryPercent + '%' }"></div>
          </div>
        </div>

        <!-- Version -->
        <div class="stat-card">
          <div class="stat-icon version">
            <Tag class="w-6 h-6" />
          </div>
          <div class="stat-info">
            <span class="stat-value">{{ status?.version ?? '—' }}</span>
            <span class="stat-label">{{ $t('dashboard.server_status.version') }}</span>
          </div>
        </div>
      </div>

      <!-- Online Players -->
      <div class="players-section glass-card" v-if="status?.players?.list?.length">
        <h3 class="section-subtitle">
          <Users class="w-5 h-5" />
          {{ $t('dashboard.server_status.online_players') }}
        </h3>
        <div class="players-list">
          <div
            v-for="player in status.players.list"
            :key="player.uuid || player.name"
            class="player-item"
          >
            <img
              :src="`https://mc-heads.net/avatar/${player.uuid || player.name}/32`"
              :alt="player.name"
              class="player-avatar"
            />
            <span class="player-name">{{ player.name }}</span>
          </div>
        </div>
      </div>

      <!-- No Players Online -->
      <div v-else-if="status?.online" class="no-players glass-card">
        <UserX class="w-8 h-8" />
        <p>{{ $t('dashboard.server_status.no_players') }}</p>
      </div>

      <!-- API Not Available Notice -->
      <div v-if="apiNotAvailable" class="api-notice">
        <AlertCircle class="w-4 h-4" />
        <span>{{ $t('dashboard.server_status.api_not_available') }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  Users,
  Activity,
  Database,
  Tag,
  RefreshCw,
  UserX,
  AlertCircle,
} from 'lucide-vue-next'
import { apiService } from '@/services/api'

const { t } = useI18n()

const loading = ref(false)
const apiNotAvailable = ref(false)

interface ServerStatus {
  online: boolean
  players?: {
    online: number
    max: number
    list?: Array<{ name: string; uuid?: string }>
  }
  version?: string
  tps?: number
  memory?: {
    used: number
    max: number
  }
  motd?: string
}

const status = ref<ServerStatus | null>(null)

// Mock data for when API is not available
const mockStatus: ServerStatus = {
  online: true,
  players: {
    online: 12,
    max: 50,
    list: [
      { name: 'Steve', uuid: '12345678-1234-1234-1234-123456789012' },
      { name: 'Alex', uuid: '87654321-4321-4321-4321-210987654321' },
      { name: 'Notch', uuid: '11111111-2222-3333-4444-555555555555' },
    ],
  },
  version: '1.20.4',
  tps: 19.8,
  memory: {
    used: 4096,
    max: 8192,
  },
  motd: 'Welcome to our Minecraft Server!',
}

const tpsClass = computed(() => {
  const tps = status.value?.tps
  if (!tps) return ''
  if (tps >= 18) return 'good'
  if (tps >= 15) return 'warning'
  return 'bad'
})

const memoryPercent = computed(() => {
  if (!status.value?.memory) return 0
  return Math.round((status.value.memory.used / status.value.memory.max) * 100)
})

const formatMemory = (mb?: number): string => {
  if (!mb) return '—'
  if (mb >= 1024) {
    return `${(mb / 1024).toFixed(1)} GB`
  }
  return `${mb} MB`
}

const loadStatus = async () => {
  loading.value = true
  try {
    const response = await apiService.getServerStatus()
    if (response.success && response.data) {
      status.value = response.data
      apiNotAvailable.value = false
    } else {
      // Use mock data when API is not available
      status.value = mockStatus
      apiNotAvailable.value = true
    }
  } catch (error) {
    console.error('Failed to load server status:', error)
    // Use mock data on error
    status.value = mockStatus
    apiNotAvailable.value = true
  } finally {
    loading.value = false
  }
}

const refreshStatus = () => {
  loadStatus()
}

onMounted(() => {
  loadStatus()
})
</script>

<style scoped>
.server-status {
  max-width: 900px;
  margin: 0 auto;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1.5rem;
}

.section-title {
  font-size: 1.5rem;
  font-weight: 700;
  color: white;
  margin: 0;
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

/* Status Card */
.status-card {
  padding: 1.5rem;
  border-radius: 16px;
  margin-bottom: 1.5rem;
}

.main-status.online {
  background: linear-gradient(135deg, rgba(34, 197, 94, 0.1) 0%, rgba(34, 197, 94, 0.05) 100%);
  border: 1px solid rgba(34, 197, 94, 0.2);
}

.main-status.offline {
  background: linear-gradient(135deg, rgba(239, 68, 68, 0.1) 0%, rgba(239, 68, 68, 0.05) 100%);
  border: 1px solid rgba(239, 68, 68, 0.2);
}

.status-indicator {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 0.75rem;
}

.indicator-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  animation: pulse 2s ease-in-out infinite;
}

.indicator-dot.online {
  background: #22c55e;
  box-shadow: 0 0 12px rgba(34, 197, 94, 0.5);
}

.indicator-dot.offline {
  background: #ef4444;
  box-shadow: 0 0 12px rgba(239, 68, 68, 0.5);
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.indicator-text {
  font-size: 1.125rem;
  font-weight: 600;
  color: white;
}

.server-motd {
  color: rgba(255, 255, 255, 0.7);
  font-size: 0.875rem;
}

/* Stats Grid */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.stat-card {
  background: rgba(255, 255, 255, 0.03);
  border-radius: 14px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  padding: 1.25rem;
  display: flex;
  align-items: flex-start;
  gap: 1rem;
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-icon.players {
  background: rgba(59, 130, 246, 0.2);
  color: #3b82f6;
}

.stat-icon.tps.good {
  background: rgba(34, 197, 94, 0.2);
  color: #22c55e;
}

.stat-icon.tps.warning {
  background: rgba(234, 179, 8, 0.2);
  color: #eab308;
}

.stat-icon.tps.bad {
  background: rgba(239, 68, 68, 0.2);
  color: #ef4444;
}

.stat-icon.memory {
  background: rgba(139, 92, 246, 0.2);
  color: #8b5cf6;
}

.stat-icon.version {
  background: rgba(236, 72, 153, 0.2);
  color: #ec4899;
}

.stat-info {
  display: flex;
  flex-direction: column;
}

.stat-value {
  font-size: 1.25rem;
  font-weight: 700;
  color: white;
}

.stat-label {
  font-size: 0.75rem;
  color: rgba(255, 255, 255, 0.5);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.memory-bar {
  width: 100%;
  height: 4px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 2px;
  margin-top: 0.75rem;
  overflow: hidden;
}

.memory-usage {
  height: 100%;
  background: linear-gradient(90deg, #3b82f6, #8b5cf6);
  border-radius: 2px;
  transition: width 0.3s ease;
}

/* Players Section */
.glass-card {
  background: rgba(255, 255, 255, 0.03);
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  padding: 1.5rem;
}

.section-subtitle {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 1rem;
  font-weight: 600;
  color: white;
  margin: 0 0 1rem 0;
}

.players-list {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.player-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 0.75rem;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 10px;
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.player-avatar {
  width: 24px;
  height: 24px;
  border-radius: 4px;
  image-rendering: pixelated;
}

.player-name {
  font-size: 0.875rem;
  color: white;
  font-weight: 500;
}

/* No Players */
.no-players {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 2rem;
  color: rgba(255, 255, 255, 0.4);
  text-align: center;
}

.no-players p {
  margin: 0.5rem 0 0 0;
}

/* API Notice */
.api-notice {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1rem;
  background: rgba(234, 179, 8, 0.1);
  border: 1px solid rgba(234, 179, 8, 0.2);
  border-radius: 10px;
  color: #fbbf24;
  font-size: 0.875rem;
  margin-top: 1.5rem;
}
</style>
