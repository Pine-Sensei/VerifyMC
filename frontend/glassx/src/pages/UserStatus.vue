<template>
  <div class="min-h-screen p-6 relative overflow-hidden">
    <!-- Navigation -->
    <nav class="relative z-10 mb-8">
      <div class="max-w-4xl mx-auto flex justify-between items-center">
        <div class="flex items-center space-x-3">
          <div class="w-10 h-10 bg-white/10 border border-white/10 rounded-xl flex items-center justify-center">
            <Server class="w-6 h-6 text-white" />
          </div>
          <h1 class="text-2xl font-bold gradient-text" v-if="config?.webServerPrefix !== undefined">{{ config.webServerPrefix }}</h1>
        </div>

        <div class="flex items-center space-x-4">
          <Button
            variant="ghost"
            @click="logout"
            class="text-white hover:text-red-300 transition-colors duration-300 flex items-center space-x-2"
          >
            <LogOut class="w-4 h-4" />
            <span>{{ $t('nav.logout') }}</span>
          </Button>
          <LanguageSwitcher />
        </div>
      </div>
    </nav>

    <!-- Main Content -->
    <main class="relative z-10 max-w-4xl mx-auto">
      <div class="glass-card p-8 animate-scale-in">
        <!-- Header -->
        <div class="text-center mb-8">
          <div class="w-16 h-16 bg-white/5 border border-white/10 rounded-2xl flex items-center justify-center mx-auto mb-4">
            <User class="w-8 h-8 text-white" />
          </div>
          <h1 class="text-3xl font-bold gradient-text mb-2">
            {{ $t('user_status.title') }}
          </h1>
          <p v-if="user" class="text-white/70">
            {{ user.username }}
          </p>
        </div>

        <!-- Loading State -->
        <div v-if="loading" class="space-y-6">
          <Skeleton class="h-24 w-full rounded-xl" />
          <div class="glass-card p-6 border-l-4 border-white/10">
             <div class="h-6 w-32 mb-4 bg-white/10 rounded animate-pulse" />
             <div class="space-y-2">
               <Skeleton class="h-4 w-full" />
               <Skeleton class="h-4 w-3/4" />
             </div>
          </div>
           <div class="glass-card p-6">
            <div class="h-6 w-32 mb-4 bg-white/10 rounded animate-pulse" />
            <div class="grid md:grid-cols-2 gap-4">
              <div>
                <Skeleton class="h-4 w-24 mb-2" />
                <Skeleton class="h-6 w-48" />
              </div>
              <div>
                <Skeleton class="h-4 w-24 mb-2" />
                <Skeleton class="h-6 w-48" />
              </div>
            </div>
          </div>
        </div>

        <!-- Status Display -->
        <div v-else-if="status" class="space-y-6">
          <!-- Status Card -->
          <StatusCard :status="status" />

          <!-- Rejection Reason -->
          <div v-if="status.status === 'rejected' && status.reason" class="glass-card p-6 border-l-4 border-red-400 bg-red-900/20">
            <h4 class="text-lg font-semibold text-white mb-2">
              {{ $t('user_status.reason') }}
            </h4>
            <p class="text-white/70">
              {{ status.reason }}
            </p>
          </div>

          <!-- User Info -->
          <div v-if="user" class="glass-card p-6">
            <h4 class="text-lg font-semibold text-white mb-4">
              {{ $t('common.info') }}
            </h4>
            <div class="grid md:grid-cols-2 gap-4">
              <div>
                <label class="block text-sm font-medium text-white/60 mb-1">
                  {{ $t('register.form.username') }}
                </label>
                <p class="text-white">{{ user.username }}</p>
              </div>
              <div>
                <label class="block text-sm font-medium text-white/60 mb-1">
                  {{ $t('register.form.email') }}
                </label>
                <p class="text-white">{{ user.email }}</p>
              </div>
            </div>
          </div>

          <!-- Actions -->
          <div class="flex flex-col sm:flex-row gap-4 justify-center">
            <Button
              variant="outline"
              @click="refreshStatus"
              :disabled="refreshing"
              class="text-white hover:text-blue-300 transition-colors duration-300 flex items-center justify-center space-x-2"
            >
              <RefreshCw class="w-4 h-4" :class="{ 'animate-spin': refreshing }" />
              <span>{{ $t('common.refresh') }}</span>
            </Button>
            <Button
              variant="outline"
              @click="router.push('/')"
              class="text-white hover:text-blue-300 transition-colors duration-300 flex items-center justify-center space-x-2"
            >
              <Home class="w-4 h-4" />
              <span>{{ $t('common.back') }}</span>
            </Button>
          </div>
        </div>

        <!-- Error State -->
        <div v-else class="text-center py-12">
          <div class="w-16 h-16 bg-red-500 rounded-2xl flex items-center justify-center mx-auto mb-4">
            <AlertCircle class="w-8 h-8 text-white" />
          </div>
          <h3 class="text-xl font-bold text-white mb-2">
            {{ $t('common.error') }}
          </h3>
          <p class="text-white/70 mb-6">
            {{ $t('errors.unknown') }}
          </p>
          <Button
            variant="default"
            @click="loadStatus"
            class="bg-gradient-to-r from-blue-500 to-purple-600 hover:from-blue-600 hover:to-purple-700 text-white font-semibold py-3 px-6 rounded-2xl transition-all duration-300"
          >
            {{ $t('common.refresh') }}
          </Button>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, inject, type Ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import {
  Server,
  LogOut,
  User,
  RefreshCw,
  Home,
  AlertCircle
} from 'lucide-vue-next'
import Button from '@/components/ui/Button.vue'
import LanguageSwitcher from '@/components/LanguageSwitcher.vue'
import StatusCard from '@/components/ui/StatusCard.vue'
import Skeleton from '@/components/ui/Skeleton.vue'
import { apiService } from '@/services/api'
import { sessionService } from '@/services/session'
import type { UserInfo, AppConfig } from '@/types'
import { useNotification } from '@/composables/useNotification'

const config = inject<Ref<AppConfig>>('config', ref({}))

const router = useRouter()
const { t } = useI18n()
const { error: notifyError } = useNotification()

const loading = ref(true)
const refreshing = ref(false)
const status = ref<{ status: string; reason?: string } | null>(null)
const user = ref<UserInfo | null>(null)

const loadStatus = async () => {
  try {
    const response = await apiService.getUserStatus()
    if (response.success) {
      status.value = response.data
    } else {
      notifyError(t('common.error'), response.message && response.message !== t('common.error') ? response.message : '')
    }
  } catch (error: unknown) {
    notifyError(t('common.error'), t('errors.network'))
  } finally {
    loading.value = false
    refreshing.value = false
  }
}

const refreshStatus = async () => {
  refreshing.value = true
  await loadStatus()
}

const logout = () => {
  sessionService.clearToken()
  router.push('/')
}

onMounted(() => {
  const userInfo = sessionService.getUserInfo()
  if (userInfo) {
    user.value = userInfo
  }

  loadStatus()
})
</script>
