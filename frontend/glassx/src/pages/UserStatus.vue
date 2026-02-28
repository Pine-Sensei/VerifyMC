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
        <div v-if="loading" class="text-center py-12">
          <div class="spinner mx-auto mb-4"></div>
          <p class="text-white/70">{{ $t('common.loading') }}</p>
        </div>

        <!-- Status Display -->
        <div v-else-if="status" class="space-y-6">
          <!-- Status Card -->
          <div class="glass-card p-6 border-l-4" :class="statusCardClass">
            <div class="flex items-center space-x-4">
              <div class="flex-shrink-0">
                <div class="w-12 h-12 rounded-xl flex items-center justify-center" :class="statusIconBg">
                  <Clock v-if="status.status === 'pending'" class="w-6 h-6 text-white" />
                  <CheckCircle v-else-if="status.status === 'approved'" class="w-6 h-6 text-white" />
                  <XCircle v-else class="w-6 h-6 text-white" />
                </div>
              </div>
              <div class="flex-1">
                <h3 class="text-xl font-bold text-white mb-1">
                  {{ $t(`user_status.status.${status.status}`) }}
                </h3>
                <p class="text-white/70">
                  {{ $t(`user_status.messages.${status.status}`) }}
                </p>
              </div>
            </div>
          </div>

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

    <!-- Toast Container -->
    <div class="fixed top-0 right-0 z-50 p-4 space-y-2">
      <Toast
        v-for="toast in toasts"
        :key="toast.id"
        :type="toast.type"
        :title="toast.title"
        :message="toast.message"
        @close="removeToast(toast.id)"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, inject, type Ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import {
  Server,
  LogOut,
  User,
  Clock,
  CheckCircle,
  XCircle,
  RefreshCw,
  Home,
  AlertCircle
} from 'lucide-vue-next'
import Button from '@/components/ui/Button.vue'
import LanguageSwitcher from '@/components/LanguageSwitcher.vue'
import Toast from '@/components/Toast.vue'
import { apiService } from '@/services/api'
import { sessionService, type UserInfo } from '@/services/session'

interface AppConfig {
  webServerPrefix?: string
}

const config = inject<Ref<AppConfig>>('config', ref({}))

const router = useRouter()
const { t } = useI18n()

const loading = ref(true)
const refreshing = ref(false)
const status = ref<{ status: string; reason?: string } | null>(null)
const user = ref<UserInfo | null>(null)

type ToastType = 'success' | 'error' | 'warning' | 'info'

const toasts = ref<Array<{ id: number; type: ToastType; title: string; message?: string }>>([])
let toastId = 0

const addToast = (type: ToastType, title: string, message?: string) => {
  const id = ++toastId
  toasts.value.push({ id, type, title, message })
}

const removeToast = (id: number) => {
  const index = toasts.value.findIndex(toast => toast.id === id)
  if (index > -1) {
    toasts.value.splice(index, 1)
  }
}

const statusCardClass = computed(() => {
  if (!status.value) return ''

  switch (status.value.status) {
    case 'pending':
      return 'border-yellow-400 bg-yellow-900/20'
    case 'approved':
      return 'border-green-400 bg-green-900/20'
    case 'rejected':
      return 'border-red-400 bg-red-900/20'
    default:
      return 'border-white/10 bg-white/10'
  }
})

const statusIconBg = computed(() => {
  if (!status.value) return 'bg-white/20'

  switch (status.value.status) {
    case 'pending':
      return 'bg-yellow-500'
    case 'approved':
      return 'bg-green-500'
    case 'rejected':
      return 'bg-red-500'
    default:
      return 'bg-white/20'
  }
})

const loadStatus = async () => {
  try {
    const response = await apiService.getUserStatus()
    if (response.success) {
      status.value = response.data
    } else {
      addToast('error', t('common.error'), response.message && response.message !== t('common.error') ? response.message : '')
    }
  } catch (error: unknown) {
    addToast('error', t('common.error'), t('errors.network'))
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
