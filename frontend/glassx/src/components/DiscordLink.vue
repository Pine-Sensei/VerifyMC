<template>
  <div class="discord-link">
    <!-- Already linked state -->
    <div v-if="linked" class="flex flex-col items-center gap-2">
      <div class="inline-flex items-center gap-2 bg-[#5865F2]/20 border border-[#5865F2]/40 rounded-lg px-4 py-2">
        <svg class="w-5 h-5" viewBox="0 0 127.14 96.36" fill="#5865F2">
          <path d="M107.7,8.07A105.15,105.15,0,0,0,81.47,0a72.06,72.06,0,0,0-3.36,6.83A97.68,97.68,0,0,0,49,6.83,72.37,72.37,0,0,0,45.64,0,105.89,105.89,0,0,0,19.39,8.09C2.79,32.65-1.71,56.6.54,80.21h0A105.73,105.73,0,0,0,32.71,96.36,77.7,77.7,0,0,0,39.6,85.25a68.42,68.42,0,0,1-10.85-5.18c.91-.66,1.8-1.34,2.66-2a75.57,75.57,0,0,0,64.32,0c.87.71,1.76,1.39,2.66,2a68.68,68.68,0,0,1-10.87,5.19,77,77,0,0,0,6.89,11.1A105.25,105.25,0,0,0,126.6,80.22h0C129.24,52.84,122.09,29.11,107.7,8.07ZM42.45,65.69C36.18,65.69,31,60,31,53s5-12.74,11.43-12.74S54,46,53.89,53,48.84,65.69,42.45,65.69Zm42.24,0C78.41,65.69,73.25,60,73.25,53s5-12.74,11.44-12.74S96.23,46,96.12,53,91.08,65.69,84.69,65.69Z"/>
        </svg>
        <span class="text-white font-medium">{{ discordUser?.globalName || discordUser?.username || 'Discord' }}</span>
        <span class="text-green-400 font-bold">✓</span>
      </div>
      <p class="text-white/70 text-sm">{{ $t('discord.linked') }}</p>
    </div>

    <!-- Not linked state -->
    <div v-else class="flex flex-col gap-2">
      <Button
        type="button"
        variant="ghost"
        @click="startDiscordLink"
        :disabled="loading || !username"
        class="inline-flex items-center justify-center gap-2 bg-[#5865F2] text-white border-none rounded-lg px-6 py-3 text-base font-medium cursor-pointer transition-all hover:bg-[#4752c4] hover:-translate-y-px hover:shadow-lg disabled:opacity-50 disabled:cursor-not-allowed h-auto"
      >
        <svg v-if="!loading" class="w-5 h-5" viewBox="0 0 127.14 96.36" fill="currentColor">
          <path d="M107.7,8.07A105.15,105.15,0,0,0,81.47,0a72.06,72.06,0,0,0-3.36,6.83A97.68,97.68,0,0,0,49,6.83,72.37,72.37,0,0,0,45.64,0,105.89,105.89,0,0,0,19.39,8.09C2.79,32.65-1.71,56.6.54,80.21h0A105.73,105.73,0,0,0,32.71,96.36,77.7,77.7,0,0,0,39.6,85.25a68.42,68.42,0,0,1-10.85-5.18c.91-.66,1.8-1.34,2.66-2a75.57,75.57,0,0,0,64.32,0c.87.71,1.76,1.39,2.66,2a68.68,68.68,0,0,1-10.87,5.19,77,77,0,0,0,6.89,11.1A105.25,105.25,0,0,0,126.6,80.22h0C129.24,52.84,122.09,29.11,107.7,8.07ZM42.45,65.69C36.18,65.69,31,60,31,53s5-12.74,11.43-12.74S54,46,53.89,53,48.84,65.69,42.45,65.69Zm42.24,0C78.41,65.69,73.25,60,73.25,53s5-12.74,11.44-12.74S96.23,46,96.12,53,91.08,65.69,84.69,65.69Z"/>
        </svg>
        <div v-else class="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin"></div>
        <span>{{ $t('discord.link_button') }}</span>
      </Button>
      <p v-if="required" class="text-white/60 text-xs text-center">{{ $t('discord.required_hint') }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, onUnmounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { apiService } from '@/services/api'
import { useNotification } from '@/composables/useNotification'
import Button from './ui/Button.vue'

const props = defineProps<{
  username: string
  required?: boolean
}>()

const emit = defineEmits<{
  (e: 'linked', user: any): void
  (e: 'unlinked'): void
}>()

const { t } = useI18n()
const { success, error } = useNotification()

const loading = ref(false)
const linked = ref(false)
const discordUser = ref<any>(null)

// 使用 ref 保存定时器，确保组件卸载时能正确清理
const pollInterval = ref<number | null>(null)
const checkClosedInterval = ref<number | null>(null)
const authWindow = ref<Window | null>(null)
const finalCheckTimeout = ref<number | null>(null)

const startDiscordLink = async () => {
  if (!props.username || loading.value) return
  
  loading.value = true
  
  try {
    const response = await apiService.getDiscordAuthUrl(props.username)
    
    if (response.success && response.authUrl) {
      // Open Discord auth in a new window
      authWindow.value = window.open(response.authUrl, 'discord_auth', 'width=500,height=800')
      
      // Start polling for link status
      startPolling()
      
      // Check if window was closed
      checkClosedInterval.value = window.setInterval(() => {
        if (authWindow.value && authWindow.value.closed) {
          if (checkClosedInterval.value) {
            clearInterval(checkClosedInterval.value)
            checkClosedInterval.value = null
          }
          // Final check after window closes
          finalCheckTimeout.value = window.setTimeout(() => checkLinkStatus(), 1000)
        }
      }, 500)
    } else {
      error(response.message || t('discord.link_failed'))
    }
  } catch (e) {
    console.error('Discord auth error:', e)
    error(t('discord.link_failed'))
  } finally {
    loading.value = false
  }
}

const checkLinkStatus = async () => {
  if (!props.username) return
  
  try {
    const response = await apiService.getDiscordStatus(props.username)
    
    if (response.success && response.linked) {
      linked.value = true
      discordUser.value = response.user
      stopPolling()
      emit('linked', response.user)
      success(t('discord.link_success'))
    }
  } catch (e) {
    console.error('Check Discord status error:', e)
  }
}

const startPolling = () => {
  stopPolling()
  // Poll every 2 seconds for up to 5 minutes
  let attempts = 0
  const maxAttempts = 150
  
  pollInterval.value = window.setInterval(() => {
    attempts++
    if (attempts >= maxAttempts) {
      stopPolling()
      return
    }
    checkLinkStatus()
  }, 2000)
}

const stopPolling = () => {
  if (pollInterval.value) {
    clearInterval(pollInterval.value)
    pollInterval.value = null
  }
}

// Watch for username changes to reset link status
watch(() => props.username, async (newUsername) => {
  if (newUsername) {
    await checkLinkStatus()
  } else {
    linked.value = false
    discordUser.value = null
  }
})

onMounted(() => {
  if (props.username) {
    checkLinkStatus()
  }
})

onUnmounted(() => {
  stopPolling()
  if (checkClosedInterval.value) {
    clearInterval(checkClosedInterval.value)
    checkClosedInterval.value = null
  }
  if (finalCheckTimeout.value) {
    clearTimeout(finalCheckTimeout.value)
    finalCheckTimeout.value = null
  }
  // 关闭 Discord 授权窗口
  if (authWindow.value && !authWindow.value.closed) {
    authWindow.value.close()
    authWindow.value = null
  }
})
</script>

