<template>
  <div class="registration-card" :class="shouldShowPassword ? 'w-full max-w-md' : 'w-full max-w-sm'">
    <!-- Animated gradient border -->
    <div class="absolute inset-0 rounded-2xl bg-gradient-to-r from-blue-500/20 via-purple-500/20 to-pink-500/20 blur-xl opacity-60 animate-gradient-shift"></div>
    
    <!-- Glass card container -->
    <div class="relative bg-white/[0.08] backdrop-blur-2xl border border-white/[0.15] rounded-2xl p-8 shadow-2xl overflow-hidden">
      <!-- Inner glow effects -->
      <div class="absolute inset-0 bg-gradient-to-br from-white/10 via-transparent to-transparent pointer-events-none"></div>
      <div class="absolute top-0 left-0 right-0 h-px bg-gradient-to-r from-transparent via-white/30 to-transparent"></div>
      
      <!-- Header with icon -->
      <div class="relative z-10 text-center mb-6">
        <div class="inline-flex items-center justify-center w-16 h-16 rounded-2xl bg-gradient-to-br from-blue-500/20 to-purple-600/20 border border-white/10 mb-4 shadow-lg">
          <svg class="w-8 h-8 text-blue-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M18 9v3m0 0v3m0-3h3m-3 0h-3m-2-5a4 4 0 11-8 0 4 4 0 018 0zM3 20a6 6 0 0112 0v1H3v-1z"></path>
          </svg>
        </div>
        <h2 class="text-2xl font-bold text-white mb-2 tracking-tight">{{ $t('register.title') }}</h2>
        <p class="text-white/60 text-sm">{{ $t('register.subtitle') }}</p>
      </div>
    
    <form @submit.prevent="handleSubmit" class="space-y-5 relative z-10">
      <div class="space-y-3">
        <!-- Username Field -->
        <div>
          <label for="username" class="block text-sm font-medium text-white mb-1">
            {{ $t('register.form.username') }}
          </label>
          <input
            id="username"
            v-model="form.username"
            type="text"
            :placeholder="$t('register.form.username_placeholder')"
            class="glass-input"
            :class="{ 'glass-input-error': errors.username }"
            @blur="validateUsername"
          />
          <p v-if="errors.username" class="mt-1 text-sm text-red-400">{{ errors.username }}</p>
        </div>
        
        <!-- Email Field -->
        <div>
          <label for="email" class="block text-sm font-medium text-white mb-1">
            {{ $t('register.form.email') }}
          </label>
          <input
            id="email"
            v-model="form.email"
            type="email"
            :placeholder="$t('register.form.email_placeholder')"
            class="glass-input"
            :class="{ 'glass-input-error': errors.email }"
            @blur="validateEmail"
          />
          <p v-if="errors.email" class="mt-1 text-sm text-red-400">{{ errors.email }}</p>
        </div>
        
        <!-- Password Field -->
        <div v-if="shouldShowPassword">
          <label for="password" class="block text-sm font-medium text-white mb-1">
            {{ $t('register.form.password') }}
          </label>
          <input
            id="password"
            v-model="form.password"
            type="password"
            :placeholder="$t('register.form.password_placeholder')"
            class="glass-input"
            :class="{ 'glass-input-error': errors.password }"
            @blur="validatePassword"
          />
          <p v-if="errors.password" class="mt-1 text-sm text-red-400">{{ errors.password }}</p>
          <p v-if="authmeConfig.password_regex" class="mt-1 text-xs text-gray-300">{{ $t('register.form.password_hint', { regex: authmeConfig.password_regex }) }}</p>
        </div>
        
        <!-- 邮箱验证码 Field (if email auth is enabled) -->
        <div v-if="emailEnabled">
          <label for="code" class="block text-sm font-medium text-white mb-1">
            {{ $t('register.form.code') }}
          </label>
          <div class="flex flex-col sm:flex-row gap-2">
            <input
              id="code"
              v-model="form.code"
              type="text"
              :placeholder="$t('register.form.code_placeholder')"
              class="glass-input"
              :class="{ 'glass-input-error': errors.code }"
              @blur="validateCode"
            />
            <button
              type="button"
              @click="sendCode"
              :disabled="sending || !form.email || cooldownSeconds > 0"
              class="glass-button-secondary"
            >
              {{ 
                sending ? $t('register.sending') : 
                cooldownSeconds > 0 ? `${cooldownSeconds}s` : 
                $t('register.sendCode') 
              }}
            </button>
          </div>
          <p v-if="errors.code" class="mt-1 text-sm text-red-400">{{ errors.code }}</p>
        </div>
        
        <!-- 图形验证码 Field (if captcha auth is enabled) -->
        <div v-if="captchaEnabled">
          <label for="captcha" class="block text-sm font-medium text-white mb-1">
            {{ $t('register.form.captcha') }}
          </label>
          <div class="flex flex-col sm:flex-row gap-2 items-center">
            <input
              id="captcha"
              v-model="form.captchaAnswer"
              type="text"
              :placeholder="$t('register.form.captcha_placeholder')"
              class="glass-input"
              :class="{ 'glass-input-error': errors.captcha }"
              @blur="validateCaptcha"
            />
            <div 
              class="captcha-image-container cursor-pointer border border-white/20 rounded-lg overflow-hidden bg-white/10 backdrop-blur-sm hover:bg-white/20 hover:border-white/30 transition-all duration-300 flex-shrink-0 shadow-lg"
              @click="refreshCaptcha"
              :title="$t('register.form.captcha_refresh')"
            >
              <img 
                v-if="captchaImage" 
                :src="captchaImage" 
                alt="captcha" 
                class="h-11 w-auto"
              />
              <div v-else class="h-11 w-28 flex items-center justify-center text-white/60 text-sm">
                {{ $t('common.loading') }}
              </div>
            </div>
          </div>
          <p v-if="errors.captcha" class="mt-1 text-sm text-red-400">{{ errors.captcha }}</p>
          <p class="mt-1 text-xs text-gray-300">{{ $t('register.form.captcha_hint') }}</p>
        </div>
        
        <!-- Discord Link (if Discord integration is enabled) -->
        <div v-if="discordEnabled" class="pt-2">
          <label class="block text-sm font-medium text-white mb-2">
            Discord {{ discordRequired ? '*' : '' }}
          </label>
          <DiscordLink 
            :username="form.username"
            :required="discordRequired"
            @linked="onDiscordLinked"
            @unlinked="onDiscordUnlinked"
          />
          <p v-if="errors.discord" class="mt-1 text-sm text-red-400">{{ errors.discord }}</p>
        </div>
      </div>
      
      <!-- Submit Button -->
      <button
        type="submit"
        :disabled="loading || !isFormValid"
        class="submit-button"
      >
        <div v-if="loading" class="spinner"></div>
        <span>{{ $t('register.form.submit') }}</span>
        <div class="button-shine"></div>
      </button>

    </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { apiService } from '@/services/api'
import { useNotification } from '@/composables/useNotification'
import DiscordLink from '@/components/DiscordLink.vue'
import type { RegisterRequest, ConfigResponse } from '@/services/api'

const { t, locale } = useI18n()
const { success, error } = useNotification()

const loading = ref(false)
const sending = ref(false)
const message = ref('')
const messageType = ref<'success' | 'error'>('success')
const config = ref<ConfigResponse>({
  login: { enable_email: false, email_smtp: '' },
  admin: {},
  frontend: { theme: '', logo_url: '', announcement: '', web_server_prefix: '', username_regex: '' },
  authme: { enabled: false, require_password: false, auto_register: false, auto_unregister: false, password_regex: '' },
  captcha: { enabled: false, email_enabled: true, type: 'math' }
})

// Captcha state
const captchaImage = ref('')
const captchaToken = ref('')
const captchaEnabled = computed(() => config.value.captcha?.enabled || false)
const emailEnabled = computed(() => config.value.captcha?.email_enabled !== false)

// Discord state
const discordLinked = ref(false)
const discordEnabled = computed(() => config.value.discord?.enabled || false)
const discordRequired = computed(() => config.value.discord?.required || false)

// 使用computed来确保类型正确
const authmeConfig = computed(() => config.value.authme)
const shouldShowPassword = computed(() => authmeConfig.value?.enabled && authmeConfig.value?.require_password)

// 加载配置
onMounted(async () => {
  try {
    const res = await apiService.getConfig()
    config.value = res
    console.log('Config loaded:', config.value)
    console.log('Captcha enabled:', config.value.captcha?.enabled)
    
    // Load captcha if enabled
    if (config.value.captcha?.enabled) {
      await refreshCaptcha()
    }
  } catch (e) {
    console.error('Failed to load config:', e)
  }
})

// Refresh captcha image
const refreshCaptcha = async () => {
  try {
    const response = await apiService.getCaptcha()
    if (response.success && response.token && response.image) {
      captchaToken.value = response.token
      captchaImage.value = response.image
    }
  } catch (e) {
    console.error('Failed to load captcha:', e)
  }
}

const form = reactive({
  username: '',
  email: '',
  code: '',
  password: '',
  captchaAnswer: ''
})

const errors = reactive({
  username: '',
  email: '',
  code: '',
  password: '',
  captcha: '',
  discord: ''
})

// Discord event handlers
const onDiscordLinked = (user: any) => {
  discordLinked.value = true
  errors.discord = ''
  console.log('Discord linked:', user)
}

const onDiscordUnlinked = () => {
  discordLinked.value = false
}

const validateDiscord = () => {
  errors.discord = ''
  if (discordRequired.value && !discordLinked.value) {
    errors.discord = t('discord.required')
  }
}

const validateUsername = () => {
  errors.username = ''
  if (!form.username) {
    errors.username = t('register.validation.username_required')
  } else if (config.value.frontend?.username_regex && !new RegExp(config.value.frontend.username_regex).test(form.username)) {
    errors.username = t('register.validation.username_format', { regex: config.value.frontend.username_regex })
  } else if (!config.value.frontend?.username_regex && !/^[a-zA-Z0-9_]+$/.test(form.username)) {
    // 如果没有配置正则表达式，使用默认规则
    errors.username = t('register.validation.username_format', { regex: '^[a-zA-Z0-9_]+$' })
  }
}

const validateEmail = () => {
  errors.email = ''
  if (!form.email) {
    errors.email = t('register.validation.email_required')
  } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) {
    errors.email = t('register.validation.email_format')
  }
}

const validatePassword = () => {
  errors.password = ''
  if (shouldShowPassword.value) {
    if (!form.password) {
      errors.password = t('register.validation.password_required')
    } else if (authmeConfig.value?.password_regex && !new RegExp(authmeConfig.value.password_regex).test(form.password)) {
      errors.password = t('register.validation.password_format', { regex: authmeConfig.value.password_regex })
    }
  }
}

const validateCode = () => {
  errors.code = ''
  if (emailEnabled.value && !form.code) {
    errors.code = t('register.validation.code_required')
  }
}

const validateCaptcha = () => {
  errors.captcha = ''
  if (captchaEnabled.value && !form.captchaAnswer) {
    errors.captcha = t('register.validation.captcha_required')
  }
}

const validateForm = () => {
  validateUsername()
  validateEmail()
  validatePassword()
  validateCode()
  validateCaptcha()
  validateDiscord()
}

const isFormValid = computed(() => {
  let valid = form.username && 
         form.email && 
         !errors.username && 
         !errors.email
  
  // Check email code if email verification is enabled
  if (emailEnabled.value) {
    valid = valid && form.code && !errors.code
  }
  
  // Check captcha if captcha is enabled
  if (captchaEnabled.value) {
    valid = valid && form.captchaAnswer && !errors.captcha
  }
  
  // Check password if Authme requires it
  if (shouldShowPassword.value) {
    valid = valid && form.password && !errors.password
  }
  
  // Check Discord if required
  if (discordRequired.value) {
    valid = valid && discordLinked.value && !errors.discord
  }
  
  return valid
})

// Rate limiting state for send code button
const cooldownSeconds = ref(0)
const cooldownTimer = ref(null)

// Start countdown timer for rate limiting
const startCooldown = (seconds) => {
  cooldownSeconds.value = seconds
  if (cooldownTimer.value) {
    clearInterval(cooldownTimer.value)
  }
  cooldownTimer.value = setInterval(() => {
    cooldownSeconds.value--
    if (cooldownSeconds.value <= 0) {
      clearInterval(cooldownTimer.value)
      cooldownTimer.value = null
    }
  }, 1000)
}

const sendCode = async () => {
  if (sending.value || cooldownSeconds.value > 0) return
  validateEmail()
  if (errors.email) return
  sending.value = true
  message.value = ''
  try {
    const email = form.email.trim().toLowerCase()
    console.log('Sending code to:', email) // Debug log
    const res = await apiService.sendCode({
      email: email,
      language: locale.value
    })
    console.log('Send code response:', res) // Debug log
    if (res.success) {
      success(t('register.codeSent'))
      // Start 60 second cooldown after successful send
      startCooldown(60)
    } else {
      // Handle rate limiting response
      if (res.remaining_seconds && res.remaining_seconds > 0) {
        startCooldown(res.remaining_seconds)
        // Don't show static message, let the button text handle the countdown
        const secondsText = t('common.seconds', 'seconds')
        const secondText = t('common.second', 'second')
        const chineseSecond = t('common.second')
        const errorMsg = res.msg && !res.msg.includes(chineseSecond) && !res.msg.includes(secondsText) && !res.msg.includes(secondText) ? res.msg : t('register.sendFailed')
        if (errorMsg) error(errorMsg)
      } else {
        const errorMsg = res.msg && res.msg !== t('register.sendFailed') ? res.msg : t('register.sendFailed')
        error(errorMsg)
      }
    }
  } catch (e) {
    console.error('Send code error:', e) // Debug log
    error(t('register.sendFailed'))
  } finally {
    sending.value = false
  }
}

const handleSubmit = async () => {
  if (loading.value) return
  validateForm()
  if (!isFormValid.value) return
  loading.value = true
  message.value = ''
  try {
    const registerData: any = {
      username: form.username,
      email: form.email.trim().toLowerCase(),
      uuid: generateUUID(),
      language: locale.value
    }
    
    // Add email code if email verification is enabled
    if (emailEnabled.value) {
      registerData.code = form.code
    }
    
    // Add captcha data if captcha is enabled
    if (captchaEnabled.value) {
      registerData.captchaToken = captchaToken.value
      registerData.captchaAnswer = form.captchaAnswer
    }
    
    // Add password if required
    if (shouldShowPassword.value) {
      registerData.password = form.password
    }
    
    console.log('Submitting registration:', registerData)
    const response = await apiService.register(registerData)
    console.log('Registration response:', response)
    if (response.success) {
      success(t('register.success'))
      Object.assign(form, {
        username: '',
        email: '',
        code: '',
        password: '',
        captchaAnswer: ''
      })
    } else {
      error(response.msg || t('register.failed'))
      // Refresh captcha on failure
      if (captchaEnabled.value) {
        await refreshCaptcha()
      }
    }
  } catch (err: any) {
    console.error('Registration error:', err)
    error(t('register.failed'))
    // Refresh captcha on error
    if (captchaEnabled.value) {
      await refreshCaptcha()
    }
  } finally {
    loading.value = false
  }
}

function generateUUID() {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
    const r = Math.random() * 16 | 0
    const v = c == 'x' ? r : (r & 0x3 | 0x8)
    return v.toString(16)
  })
}
</script>

<style scoped>
/* Registration card container */
.registration-card {
  position: relative;
  padding: 2px;
}

/* Animated gradient background */
@keyframes gradient-shift {
  0%, 100% {
    opacity: 0.4;
    transform: scale(1);
  }
  50% {
    opacity: 0.6;
    transform: scale(1.02);
  }
}

.animate-gradient-shift {
  animation: gradient-shift 4s ease-in-out infinite;
}

/* Glass input styling */
.glass-input {
  width: 100%;
  padding: 0.75rem 1rem;
  background: rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 12px;
  color: #fff;
  font-size: 0.95rem;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  outline: none;
}

.glass-input::placeholder {
  color: rgba(255, 255, 255, 0.4);
}

.glass-input:hover {
  background: rgba(255, 255, 255, 0.1);
  border-color: rgba(255, 255, 255, 0.25);
}

.glass-input:focus {
  background: rgba(255, 255, 255, 0.12);
  border-color: rgba(59, 130, 246, 0.5);
  box-shadow: 
    0 0 0 3px rgba(59, 130, 246, 0.15),
    0 0 20px rgba(59, 130, 246, 0.2);
}

.glass-input-error {
  border-color: rgba(239, 68, 68, 0.5) !important;
}

.glass-input-error:focus {
  border-color: rgba(239, 68, 68, 0.6) !important;
  box-shadow: 
    0 0 0 3px rgba(239, 68, 68, 0.15),
    0 0 20px rgba(239, 68, 68, 0.2) !important;
}

/* Secondary glass button */
.glass-button-secondary {
  padding: 0.75rem 1.25rem;
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 12px;
  color: #fff;
  font-weight: 500;
  white-space: nowrap;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.glass-button-secondary:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.18);
  border-color: rgba(255, 255, 255, 0.3);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
}

.glass-button-secondary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* Submit button with shine effect */
.submit-button {
  position: relative;
  width: 100%;
  padding: 0.875rem 1.5rem;
  background: linear-gradient(135deg, #3b82f6 0%, #8b5cf6 100%);
  border: none;
  border-radius: 12px;
  color: #fff;
  font-weight: 600;
  font-size: 1rem;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  overflow: hidden;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 
    0 4px 15px rgba(59, 130, 246, 0.3),
    inset 0 1px 0 rgba(255, 255, 255, 0.2);
}

.submit-button:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 
    0 8px 25px rgba(59, 130, 246, 0.4),
    inset 0 1px 0 rgba(255, 255, 255, 0.2);
}

.submit-button:active:not(:disabled) {
  transform: translateY(0);
}

.submit-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}

/* Button shine animation */
.button-shine {
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(
    90deg,
    transparent,
    rgba(255, 255, 255, 0.3),
    transparent
  );
  transition: left 0.6s ease;
}

.submit-button:hover:not(:disabled) .button-shine {
  left: 100%;
}

/* Spinner */
.spinner {
  width: 20px;
  height: 20px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

/* Label styling */
label {
  display: block;
  font-size: 0.875rem;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.9);
  margin-bottom: 0.5rem;
}

/* Error text */
.text-red-400 {
  color: #f87171;
}

/* Hint text */
.text-gray-300 {
  color: rgba(255, 255, 255, 0.6);
}

/* Reduce motion for users who prefer it */
@media (prefers-reduced-motion: reduce) {
  .animate-gradient-shift {
    animation: none;
  }
  
  .button-shine {
    display: none;
  }
  
  .glass-input,
  .glass-button-secondary,
  .submit-button {
    transition: none;
  }
}
</style>
