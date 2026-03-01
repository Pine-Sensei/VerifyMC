<template>
  <!-- Version update notification component with glassx theme styling -->
  <div v-if="showNotification" class="version-notification">
    <div class="version-notification-backdrop" @click="dismissNotification"></div>
    <div class="version-notification-dialog">
      <!-- Header -->
      <div class="flex items-center justify-between mb-4">
        <div class="flex items-center space-x-3">
          <div class="version-icon">
            <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M9 19l3 3m0 0l3-3m-3 3V10"></path>
            </svg>
          </div>
          <h3 class="text-xl font-semibold text-white">{{ $t('version.update_available') }}</h3>
        </div>
        <Button 
          variant="ghost"
          @click="dismissNotification"
          class="gap-2 text-white/60 hover:text-white"
        >
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
          </svg>
          <span>{{ $t('common.close') }}</span>
        </Button>
      </div>
      
      <!-- Version info -->
      <div class="bg-white/5 border border-white/10 rounded-xl p-4 mb-6">
        <div class="flex justify-between items-center mb-2">
          <span class="text-white/80">{{ $t('version.current_version') }}:</span>
          <span class="text-white font-mono">{{ versionInfo.currentVersion }}</span>
        </div>
        <div class="flex justify-between items-center mb-4">
          <span class="text-white/80">{{ $t('version.latest_version') }}:</span>
          <span class="text-green-400 font-mono font-semibold">{{ versionInfo.latestVersion }}</span>
        </div>
        <div class="text-white/70 text-sm">
          {{ $t('version.update_description') }}
        </div>
      </div>
      
      <!-- Actions -->
      <div class="flex gap-3 justify-end">
        <Button 
          variant="outline"
          @click="remindLater"
          class="text-white/80 hover:text-white"
        >
          {{ $t('version.remind_later') }}
        </Button>
        <Button 
          variant="default"
          @click="openDownloadPage"
          class="flex items-center space-x-2"
        >
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14"></path>
          </svg>
          <span>{{ $t('version.download_now') }}</span>
        </Button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { apiService } from '@/services/api'
import Button from '@/components/ui/Button.vue'

const isSafeUrl = (url: string): boolean => {
  try {
    if (!url.startsWith('http://') && !url.startsWith('https://')) {
      return false
    }
    const parsedUrl = new URL(url)
    return ['http:', 'https:'].includes(parsedUrl.protocol)
  } catch {
    return false
  }
}

const getSafeUrl = (url: string | undefined): string => {
  if (!url) return '#'
  return isSafeUrl(url) ? url : '#'
}

interface VersionInfo {
  currentVersion: string
  latestVersion: string
  updateAvailable: boolean
  releasesUrl: string
}

const showNotification = ref(false)
const versionInfo = ref<VersionInfo>({
  currentVersion: '',
  latestVersion: '',
  updateAvailable: false,
  releasesUrl: ''
})

let checkInterval: ReturnType<typeof setInterval> | null = null

/**
 * Check for version updates
 */
const checkForUpdates = async () => {
  try {
    const response = await apiService.checkVersion()
    
    if (response.success && response.updateAvailable) {
      versionInfo.value = {
        currentVersion: response.currentVersion || '',
        latestVersion: response.latestVersion || '',
        updateAvailable: response.updateAvailable || false,
        releasesUrl: response.releasesUrl || ''
      }
      
      // Check if user has dismissed this version
      const dismissedVersion = localStorage.getItem('dismissed_version')
      if (dismissedVersion !== response.latestVersion) {
        showNotification.value = true
      }
    }
  } catch (error) {
    console.warn('Version check failed:', error)
  }
}

/**
 * Dismiss the notification
 */
const dismissNotification = () => {
  showNotification.value = false
  // Remember that user dismissed this version
  localStorage.setItem('dismissed_version', versionInfo.value.latestVersion)
}

/**
 * Remind later (dismiss for this session only)
 */
const remindLater = () => {
  showNotification.value = false
  // Don't save to localStorage, so it will show again on next session
}

/**
 * Open download page in new tab
 */
const openDownloadPage = () => {
  const safeUrl = getSafeUrl(versionInfo.value.releasesUrl)
  if (safeUrl !== '#') {
    window.open(safeUrl, '_blank')
  }
  dismissNotification()
}

onMounted(() => {
  // Check immediately
  checkForUpdates()
  
  // Check every 30 minutes
  checkInterval = setInterval(checkForUpdates, 30 * 60 * 1000)
})

onUnmounted(() => {
  if (checkInterval) {
    clearInterval(checkInterval)
  }
})
</script>

<style scoped>
/* Animation */
@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateY(-20px) scale(0.95);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}
</style>