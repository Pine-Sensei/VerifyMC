<template>
  <div class="profile-section">
    <div class="profile-header">
      <div class="avatar-container">
        <div class="avatar">
          <User class="w-12 h-12" />
        </div>
        <div class="status-badge" :class="statusClass">
          {{ statusText }}
        </div>
      </div>
      <div class="profile-info">
        <h2 class="username">{{ userInfo?.username || 'User' }}</h2>
        <p class="email">{{ userInfo?.email || '' }}</p>
      </div>
    </div>

    <!-- Status Card -->
    <div class="status-card" :class="statusCardClass">
      <div class="status-icon-wrapper">
        <Clock v-if="userStatus === 'pending'" class="status-icon" />
        <CheckCircle v-else-if="userStatus === 'approved'" class="status-icon" />
        <XCircle v-else class="status-icon" />
      </div>
      <div class="status-content">
        <h3 class="status-title">{{ $t(`user_status.status.${userStatus}`) }}</h3>
        <p class="status-message">{{ $t(`user_status.messages.${userStatus}`) }}</p>
        <p v-if="userStatus === 'rejected' && rejectReason" class="reject-reason">
          {{ $t('user_status.reason') }}: {{ rejectReason }}
        </p>
      </div>
    </div>

    <!-- Profile Form -->
    <div class="profile-form glass-card">
      <h3 class="form-title">{{ $t('dashboard.profile.edit_profile') }}</h3>

      <div class="form-grid">
        <div class="form-group">
          <Label>{{ $t('register.form.username') }}</Label>
          <Input :value="userInfo?.username" disabled class="input-disabled" />
        </div>

        <div class="form-group">
          <Label>{{ $t('register.form.email') }}</Label>
          <Input v-model="form.email" :placeholder="$t('register.form.email_placeholder')" :disabled="saving" />
        </div>
      </div>

      <div class="form-actions">
        <Button @click="saveProfile" :disabled="saving" variant="default">
          <div v-if="saving" class="spinner-small"></div>
          {{ saving ? $t('common.loading') : $t('common.save') }}
        </Button>
      </div>
    </div>

    <!-- Change Password -->
    <div class="password-section glass-card">
      <h3 class="form-title">{{ $t('dashboard.profile.change_password') }}</h3>

      <div class="form-grid">
        <div class="form-group">
          <Label>{{ $t('dashboard.profile.current_password') }}</Label>
          <Input
            v-model="passwordForm.currentPassword"
            type="password"
            :placeholder="$t('dashboard.profile.current_password_placeholder')"
            :disabled="changingPassword"
          />
        </div>

        <div class="form-group">
          <Label>{{ $t('dashboard.profile.new_password') }}</Label>
          <Input
            v-model="passwordForm.newPassword"
            type="password"
            :placeholder="$t('dashboard.profile.new_password_placeholder')"
            :disabled="changingPassword"
          />
        </div>
      </div>

      <div class="form-actions">
        <Button @click="changePassword" :disabled="changingPassword" variant="outline">
          <div v-if="changingPassword" class="spinner-small"></div>
          {{ changingPassword ? $t('common.loading') : $t('dashboard.profile.change_password_btn') }}
        </Button>
      </div>
    </div>

    <!-- Discord Status -->
    <div v-if="discordEnabled" class="discord-section glass-card">
      <h3 class="form-title">{{ $t('discord.linked') }}</h3>
      <div class="discord-content">
        <div v-if="discordStatus?.linked" class="discord-linked">
          <div class="discord-avatar">
            <img
              v-if="discordStatus.user?.avatar"
              :src="`https://cdn.discordapp.com/avatars/${discordStatus.user.id}/${discordStatus.user.avatar}.png`"
              alt="Discord Avatar"
            />
            <User v-else class="w-8 h-8" />
          </div>
          <div class="discord-info">
            <span class="discord-name">{{ discordStatus.user?.globalName || discordStatus.user?.username }}</span>
            <span class="discord-tag">@{{ discordStatus.user?.username }}</span>
          </div>
        </div>
        <div v-else class="discord-unlinked">
          <p>{{ $t('discord.required_hint') }}</p>
          <Button @click="linkDiscord" variant="outline">
            {{ $t('discord.link_button') }}
          </Button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, inject } from 'vue'
import { useI18n } from 'vue-i18n'
import { User, Clock, CheckCircle, XCircle } from 'lucide-vue-next'
import { useNotification } from '@/composables/useNotification'
import { sessionService, type UserInfo } from '@/services/session'
import { apiService, type ConfigResponse } from '@/services/api'
import Label from '@/components/ui/Label.vue'
import Input from '@/components/ui/Input.vue'
import Button from '@/components/ui/Button.vue'

// Type definitions
type UserStatus = 'pending' | 'approved' | 'rejected'

interface DiscordUser {
  id: string
  username: string
  discriminator: string
  avatar?: string
  globalName?: string
}

interface DiscordStatus {
  success: boolean
  linked: boolean
  user?: DiscordUser
  message?: string
}

const { t, locale } = useI18n()
const notification = useNotification()
const config = inject<{ value: ConfigResponse }>('config', { value: {} as ConfigResponse })

const userInfo = ref<UserInfo | null>(null)
const userStatus = ref<UserStatus>('pending')
const rejectReason = ref<string>('')
const saving = ref(false)
const changingPassword = ref(false)
const discordStatus = ref<DiscordStatus | null>(null)

const form = ref({
  email: '',
})

const passwordForm = ref({
  currentPassword: '',
  newPassword: '',
})

const discordEnabled = computed(() => config.value?.discord?.enabled)

const statusClass = computed(() => {
  switch (userStatus.value) {
    case 'pending': return 'status-pending'
    case 'approved': return 'status-approved'
    case 'rejected': return 'status-rejected'
    default: return ''
  }
})

const statusText = computed(() => t(`user_status.status.${userStatus.value}`))

const statusCardClass = computed(() => {
  switch (userStatus.value) {
    case 'pending': return 'card-pending'
    case 'approved': return 'card-approved'
    case 'rejected': return 'card-rejected'
    default: return ''
  }
})

const loadUserInfo = async () => {
  userInfo.value = sessionService.getUserInfo()
  if (userInfo.value) {
    form.value.email = userInfo.value.email || ''
  }
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

const saveProfile = async () => {
  saving.value = true
  try {
    const response = await apiService.updateUserInfo({
      email: form.value.email,
      language: locale.value,
    })
    if (response.success) {
      notification.success(t('dashboard.profile.save_success'))
      if (userInfo.value) {
        userInfo.value.email = form.value.email
        sessionService.setUserInfo(userInfo.value)
      }
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
</script>

<style scoped>
.profile-section {
  max-width: 800px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.profile-header {
  display: flex;
  align-items: center;
  gap: 1.5rem;
  padding: 1.5rem;
  background: rgba(255, 255, 255, 0.03);
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.avatar-container {
  position: relative;
}

.avatar {
  width: 80px;
  height: 80px;
  border-radius: 20px;
  background: linear-gradient(135deg, #3b82f6 0%, #8b5cf6 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.status-badge {
  position: absolute;
  bottom: -4px;
  right: -4px;
  padding: 0.25rem 0.5rem;
  border-radius: 8px;
  font-size: 0.7rem;
  font-weight: 600;
  white-space: nowrap;
}

.status-pending {
  background: rgba(234, 179, 8, 0.2);
  color: #fbbf24;
  border: 1px solid rgba(234, 179, 8, 0.3);
}

.status-approved {
  background: rgba(34, 197, 94, 0.2);
  color: #22c55e;
  border: 1px solid rgba(34, 197, 94, 0.3);
}

.status-rejected {
  background: rgba(239, 68, 68, 0.2);
  color: #ef4444;
  border: 1px solid rgba(239, 68, 68, 0.3);
}

.profile-info {
  flex: 1;
}

.username {
  font-size: 1.5rem;
  font-weight: 700;
  color: white;
  margin: 0 0 0.25rem 0;
}

.email {
  color: rgba(255, 255, 255, 0.6);
  margin: 0;
}

/* Status Card */
.status-card {
  display: flex;
  align-items: flex-start;
  gap: 1rem;
  padding: 1.25rem;
  border-radius: 12px;
  border-left: 4px solid;
}

.card-pending {
  background: rgba(234, 179, 8, 0.1);
  border-color: #eab308;
}

.card-approved {
  background: rgba(34, 197, 94, 0.1);
  border-color: #22c55e;
}

.card-rejected {
  background: rgba(239, 68, 68, 0.1);
  border-color: #ef4444;
}

.status-icon-wrapper {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.card-pending .status-icon-wrapper {
  background: rgba(234, 179, 8, 0.2);
  color: #fbbf24;
}

.card-approved .status-icon-wrapper {
  background: rgba(34, 197, 94, 0.2);
  color: #22c55e;
}

.card-rejected .status-icon-wrapper {
  background: rgba(239, 68, 68, 0.2);
  color: #ef4444;
}

.status-icon {
  width: 24px;
  height: 24px;
}

.status-content {
  flex: 1;
}

.status-title {
  font-size: 1.125rem;
  font-weight: 600;
  color: white;
  margin: 0 0 0.25rem 0;
}

.status-message {
  color: rgba(255, 255, 255, 0.7);
  margin: 0;
  font-size: 0.875rem;
}

.reject-reason {
  color: rgba(255, 255, 255, 0.6);
  margin: 0.5rem 0 0 0;
  font-size: 0.875rem;
}

/* Form Styles */
.glass-card {
  background: rgba(255, 255, 255, 0.03);
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  padding: 1.5rem;
}

.form-title {
  font-size: 1.125rem;
  font-weight: 600;
  color: white;
  margin: 0 0 1.25rem 0;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 1rem;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.input-disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.form-actions {
  margin-top: 1.25rem;
  display: flex;
  justify-content: flex-end;
}

.spinner-small {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin-right: 0.5rem;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* Discord Section */
.discord-content {
  display: flex;
  align-items: center;
}

.discord-linked {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.discord-avatar {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: rgba(88, 101, 242, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #5865f2;
  overflow: hidden;
}

.discord-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.discord-info {
  display: flex;
  flex-direction: column;
}

.discord-name {
  font-weight: 600;
  color: white;
}

.discord-tag {
  font-size: 0.875rem;
  color: rgba(255, 255, 255, 0.5);
}

.discord-unlinked {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

.discord-unlinked p {
  color: rgba(255, 255, 255, 0.6);
  margin: 0;
}
</style>
