<template>
  <div class="max-w-4xl mx-auto flex flex-col gap-4">
    <Card class="flex flex-col sm:flex-row items-center gap-6 p-6 text-center sm:text-left">
      <div class="relative">
        <div class="w-20 h-20 rounded-2xl bg-gradient-to-br from-blue-500 to-purple-600 flex items-center justify-center text-white">
          <User class="w-12 h-12" />
        </div>
        <div class="absolute -bottom-1 -right-1 px-2 py-1 rounded-lg text-[0.7rem] font-semibold whitespace-nowrap" :class="statusClass">
          {{ statusText }}
        </div>
      </div>
      <div class="flex-1">
        <h2 class="text-2xl font-bold text-white mb-1">{{ userInfo?.username || 'User' }}</h2>
        <p class="text-white/60 m-0">{{ userInfo?.email || '' }}</p>
      </div>
    </Card>

    <Card class="border-l-4 p-5 flex items-start gap-4" :class="statusCardClass">
      <div class="w-12 h-12 rounded-xl flex items-center justify-center shrink-0" :class="statusIconWrapperClass">
        <Clock v-if="userStatus === 'pending'" class="w-6 h-6" />
        <CheckCircle v-else-if="userStatus === 'approved'" class="w-6 h-6" />
        <XCircle v-else class="w-6 h-6" />
      </div>
      <div class="flex-1">
        <h3 class="text-lg font-semibold text-white mb-1">{{ $t(`user_status.status.${userStatus}`) }}</h3>
        <p class="text-white/70 text-sm m-0">{{ $t(`user_status.messages.${userStatus}`) }}</p>
        <p v-if="userStatus === 'rejected' && rejectReason" class="text-white/60 mt-2 text-sm">
          {{ $t('user_status.reason') }}: {{ rejectReason }}
        </p>
      </div>
    </Card>

    <Card class="p-6">
      <h3 class="text-lg font-semibold text-white mb-5">{{ $t('dashboard.profile.edit_profile') }}</h3>

      <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div class="flex flex-col gap-2">
          <Label>{{ $t('register.form.username') }}</Label>
          <Input :value="userInfo?.username" disabled class="opacity-60 cursor-not-allowed" />
        </div>

        <div class="flex flex-col gap-2">
          <Label>{{ $t('register.form.email') }}</Label>
          <Input v-model="form.email" :placeholder="$t('register.form.email_placeholder')" :disabled="saving" />
        </div>

        <div v-if="requiresVerificationCode" class="flex flex-col gap-2 md:col-span-2">
          <Label>{{ $t('register.form.code') }}</Label>
          <div class="flex flex-col sm:flex-row gap-2">
            <Input
              v-model="verificationCode"
              :placeholder="$t('register.form.code_placeholder')"
              :disabled="saving"
            />
            <Button
              @click="sendVerificationCode"
              :disabled="saving || sendingCode || !normalizedFormEmail || cooldownSeconds > 0"
              variant="secondary"
              class="whitespace-nowrap"
            >
              {{ sendingCode ? $t('register.sending') : cooldownSeconds > 0 ? `${cooldownSeconds}s` : $t('register.sendCode') }}
            </Button>
          </div>
        </div>
      </div>

      <div class="mt-5 flex justify-end">
        <Button @click="saveProfile" :disabled="saving" variant="default">
          <div v-if="saving" class="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin mr-2"></div>
          {{ saving ? $t('common.loading') : $t('common.save') }}
        </Button>
      </div>
    </Card>

    <Card class="p-6">
      <h3 class="text-lg font-semibold text-white mb-5">{{ $t('dashboard.profile.change_password') }}</h3>

      <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div class="flex flex-col gap-2">
          <Label>{{ $t('dashboard.profile.current_password') }}</Label>
          <Input
            v-model="passwordForm.currentPassword"
            type="password"
            :placeholder="$t('dashboard.profile.current_password_placeholder')"
            :disabled="changingPassword"
          />
        </div>

        <div class="flex flex-col gap-2">
          <Label>{{ $t('dashboard.profile.new_password') }}</Label>
          <Input
            v-model="passwordForm.newPassword"
            type="password"
            :placeholder="$t('dashboard.profile.new_password_placeholder')"
            :disabled="changingPassword"
          />
        </div>
      </div>

      <div class="mt-5 flex justify-end">
        <Button @click="changePassword" :disabled="changingPassword" variant="outline">
          <div v-if="changingPassword" class="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin mr-2"></div>
          {{ changingPassword ? $t('common.loading') : $t('dashboard.profile.change_password_btn') }}
        </Button>
      </div>
    </Card>

    <Card v-if="discordEnabled" class="p-6">
      <h3 class="text-lg font-semibold text-white mb-5">{{ $t('discord.linked') }}</h3>
      <div class="flex items-center">
        <div v-if="discordStatus?.linked" class="flex items-center gap-4">
          <div class="w-12 h-12 rounded-xl bg-[#5865F2]/20 flex items-center justify-center text-[#5865F2] overflow-hidden">
            <img
              v-if="discordStatus.user?.avatar"
              :src="`https://cdn.discordapp.com/avatars/${discordStatus.user.id}/${discordStatus.user.avatar}.png`"
              alt="Discord Avatar"
              class="w-full h-full object-cover"
            />
            <User v-else class="w-8 h-8" />
          </div>
          <div class="flex flex-col">
            <span class="font-semibold text-white">{{ discordStatus.user?.globalName || discordStatus.user?.username }}</span>
            <span class="text-sm text-white/50">@{{ discordStatus.user?.username }}</span>
          </div>
        </div>
        <div v-else class="flex items-center justify-between w-full">
          <p class="text-white/60 m-0">{{ $t('discord.required_hint') }}</p>
          <Button @click="linkDiscord" variant="outline">
            {{ $t('discord.link_button') }}
          </Button>
        </div>
      </div>
    </Card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, inject, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { User, Clock, CheckCircle, XCircle } from 'lucide-vue-next'
import { useNotification } from '@/composables/useNotification'
import { sessionService } from '@/services/session'
import { apiService, type ConfigResponse } from '@/services/api'
import { getStatusColors } from '@/lib/utils'
import Label from '@/components/ui/Label.vue'
import Input from '@/components/ui/Input.vue'
import Button from '@/components/ui/Button.vue'
import Card from '@/components/ui/Card.vue'
import type { UserInfo, DiscordStatus, UserStatusType } from '@/types'

const { t, locale } = useI18n()
const notification = useNotification()
const config = inject<{ value: ConfigResponse }>('config', { value: {} as ConfigResponse })

const userInfo = ref<UserInfo | null>(null)
const userStatus = ref<UserStatusType>('pending')
const rejectReason = ref<string>('')
const saving = ref(false)
const changingPassword = ref(false)
const discordStatus = ref<DiscordStatus | null>(null)
const sendingCode = ref(false)
const verificationCode = ref('')
const cooldownSeconds = ref(0)
const cooldownTimer = ref<ReturnType<typeof setInterval> | null>(null)

const form = ref({
  email: '',
})

const passwordForm = ref({
  currentPassword: '',
  newPassword: '',
})

const discordEnabled = computed(() => config.value?.discord?.enabled)
const emailVerificationEnabled = computed(() => config.value?.authMethods?.includes('email') ?? false)
const normalizedCurrentEmail = computed(() => (userInfo.value?.email || '').trim().toLowerCase())
const normalizedFormEmail = computed(() => form.value.email.trim().toLowerCase())
const requiresVerificationCode = computed(() => {
  return emailVerificationEnabled.value && normalizedFormEmail.value !== normalizedCurrentEmail.value
})

const statusClass = computed(() => {
  const colors = getStatusColors(userStatus.value)
  return `${colors.bg} ${colors.text} border ${colors.border}`
})

const statusText = computed(() => t(`user_status.status.${userStatus.value}`))

const statusCardClass = computed(() => {
  const colors = getStatusColors(userStatus.value)
  return `${colors.cardBg} ${colors.cardBorder}`
})

const statusIconWrapperClass = computed(() => {
  const colors = getStatusColors(userStatus.value)
  return `${colors.bg} ${colors.text}`
})

const loadUserInfo = async () => {
  userInfo.value = sessionService.getUserInfo()
  if (userInfo.value) {
    form.value.email = userInfo.value.email || ''
  }
}

const startCooldown = (seconds: number) => {
  cooldownSeconds.value = seconds
  if (cooldownTimer.value) {
    clearInterval(cooldownTimer.value)
  }
  cooldownTimer.value = setInterval(() => {
    cooldownSeconds.value -= 1
    if (cooldownSeconds.value <= 0 && cooldownTimer.value) {
      clearInterval(cooldownTimer.value)
      cooldownTimer.value = null
    }
  }, 1000)
}

const loadUserStatus = async () => {
  try {
    const response = await apiService.getUserStatus()
    if (response.success && response.data) {
      const status = response.data.status
      userStatus.value = (status === 'pending' || status === 'approved' || status === 'rejected')
        ? status
        : 'pending'
      rejectReason.value = response.data.reason || ''
    }
  } catch (error) {
    console.error('Failed to load user status:', error)
  }
}

const loadDiscordStatus = async () => {
  if (!userInfo.value?.username) return
  try {
    const response = await apiService.getDiscordStatus(userInfo.value.username)
    if (response.success) {
      discordStatus.value = response
    }
  } catch (error) {
    console.error('Failed to load Discord status:', error)
  }
}

const sendVerificationCode = async () => {
  if (!normalizedFormEmail.value || sendingCode.value || cooldownSeconds.value > 0) {
    return
  }

  if (!isValidEmail(normalizedFormEmail.value)) {
    notification.error(t('register.validation.email_format'))
    return
  }

  sendingCode.value = true
  try {
    const response = await apiService.sendCode({
      email: normalizedFormEmail.value,
      language: locale.value,
    })
    if (response.success) {
      notification.success(t('register.codeSent'))
      startCooldown(response.remainingSeconds && response.remainingSeconds > 0 ? response.remainingSeconds : 60)
    } else if (response.remainingSeconds && response.remainingSeconds > 0) {
      startCooldown(response.remainingSeconds)
      notification.error(response.message || t('register.sendFailed'))
    } else {
      notification.error(response.message || t('register.sendFailed'))
    }
  } catch (error) {
    notification.error(t('register.sendFailed'))
  } finally {
    sendingCode.value = false
  }
}

const saveProfile = async () => {
  if (requiresVerificationCode.value && !verificationCode.value.trim()) {
    notification.error(t('register.validation.code_required'))
    return
  }

  saving.value = true
  try {
    const response = await apiService.updateUserInfo({
      email: normalizedFormEmail.value,
      code: requiresVerificationCode.value ? verificationCode.value.trim() : undefined,
      language: locale.value,
    })
    if (response.success) {
      notification.success(t('dashboard.profile.save_success'))
      if (userInfo.value) {
        userInfo.value.email = normalizedFormEmail.value
        sessionService.setUserInfo(userInfo.value)
      }
      verificationCode.value = ''
    } else {
      notification.error(response.message || t('common.error'))
    }
  } catch (error) {
    notification.error(t('common.error'))
  } finally {
    saving.value = false
  }
}

const changePassword = async () => {
  if (!passwordForm.value.currentPassword || !passwordForm.value.newPassword) {
    notification.error(t('dashboard.profile.password_required'))
    return
  }

  changingPassword.value = true
  try {
    const response = await apiService.userChangePassword({
      currentPassword: passwordForm.value.currentPassword,
      newPassword: passwordForm.value.newPassword,
      language: locale.value,
    })
    if (response.success) {
      notification.success(t('dashboard.profile.password_change_success'))
      passwordForm.value = { currentPassword: '', newPassword: '' }
    } else {
      notification.error(response.message || t('common.error'))
    }
  } catch (error) {
    notification.error(t('common.error'))
  } finally {
    changingPassword.value = false
  }
}

const linkDiscord = async () => {
  if (!userInfo.value?.username) return
  try {
    const response = await apiService.getDiscordAuthUrl(userInfo.value.username)
    if (response.success && response.authUrl) {
      window.open(response.authUrl, '_blank')
    }
  } catch (error) {
    notification.error(t('discord.link_failed'))
  }
}

onMounted(() => {
  loadUserInfo()
  loadUserStatus()
  if (discordEnabled.value) {
    loadDiscordStatus()
  }
})

watch(normalizedFormEmail, (nextEmail, previousEmail) => {
  if (nextEmail !== previousEmail) {
    verificationCode.value = ''
  }
})

onUnmounted(() => {
  if (cooldownTimer.value) {
    clearInterval(cooldownTimer.value)
    cooldownTimer.value = null
  }
})

const isValidEmail = (email: string) => {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)
}
</script>
