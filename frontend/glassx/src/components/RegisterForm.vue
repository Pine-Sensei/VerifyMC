<template>
  <div class="flex flex-col gap-6 relative z-20">
    <Card>
      <CardHeader>
        <CardTitle class="text-2xl">{{ $t('register.title') }}</CardTitle>
        <CardDescription>{{ $t('register.subtitle') }}</CardDescription>
      </CardHeader>
      <CardContent>
        <form @submit.prevent="handleSubmit">
          <div class="flex flex-col gap-6">
            <div class="grid gap-2">
              <Label for="username">{{ $t('register.form.username') }}</Label>
              <Input 
                id="username" 
                type="text" 
                :placeholder="$t('register.form.username_placeholder')" 
                v-model="form.username"
              />
            </div>
            <div class="grid gap-2">
              <Label for="email">{{ $t('register.form.email') }}</Label>
              <Input 
                id="email" 
                type="email" 
                :placeholder="$t('register.form.email_placeholder')" 
                v-model="form.email"
              />
            </div>
            
            <!-- Email verification code (if email auth is enabled) -->
            <div class="grid gap-2" v-if="config.emailEnabled">
              <Label for="code">{{ $t('register.form.code') }}</Label>
              <div class="flex flex-col sm:flex-row gap-2">
                <Input 
                  id="code" 
                  type="text" 
                  :placeholder="$t('register.form.code_placeholder')"
                  v-model="form.code"
                  class="flex-1"
                />
                <button 
                  type="button" 
                  @click="sendCode"
                  :disabled="codeSending || !form.email.trim()"
                  class="px-4 py-2 border border-white/20 bg-white/10 backdrop-blur-sm hover:bg-white/20 text-white rounded-md whitespace-nowrap cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  <span v-if="codeSending">{{ $t('register.sending') }}</span>
                  <span v-else>{{ $t('register.send_code') }}</span>
                </button>
              </div>
            </div>

            <!-- Captcha verification (if captcha auth is enabled) -->
            <div class="grid gap-2" v-if="config.captchaEnabled">
              <Label for="captcha">{{ $t('register.form.captcha') }}</Label>
              <div class="flex flex-col sm:flex-row gap-2 items-center">
                <Input 
                  id="captcha" 
                  type="text" 
                  :placeholder="$t('register.form.captcha_placeholder')"
                  v-model="form.captchaAnswer"
                  class="flex-1"
                />
                <div 
                  class="captcha-image-container cursor-pointer border border-white/20 rounded-md overflow-hidden bg-white/10 backdrop-blur-sm hover:bg-white/20 transition-colors"
                  @click="refreshCaptcha"
                  :title="$t('register.form.captcha_refresh')"
                >
                  <img 
                    v-if="captchaImage" 
                    :src="captchaImage" 
                    alt="captcha" 
                    class="h-10 w-auto"
                  />
                  <div v-else class="h-10 w-24 flex items-center justify-center text-white/60 text-sm">
                    {{ $t('common.loading') }}
                  </div>
                </div>
              </div>
              <p class="text-xs text-white/60">{{ $t('register.form.captcha_hint') }}</p>
            </div>

            <!-- Password field (if authme is enabled and requires password) -->
            <div class="grid gap-2" v-if="config.requirePassword">
              <Label for="password">{{ $t('register.form.password') }}</Label>
              <Input 
                id="password" 
                type="password" 
                :placeholder="$t('register.form.password_placeholder')" 
                v-model="form.password"
              />
              <p class="text-xs text-white/60">{{ $t('register.form.password_hint', { regex: config.passwordRegex }) }}</p>
            </div>

            <Button type="submit" class="w-full" :disabled="loading">
              <span v-if="loading">{{ $t('common.loading') }}</span>
              <span v-else>{{ $t('register.form.submit') }}</span>
            </Button>
          </div>

        </form>
      </CardContent>
    </Card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useNotification } from '@/composables/useNotification'
import { apiService } from '@/services/api'

const { t, locale } = useI18n()
import Card from './ui/Card.vue'
import CardHeader from './ui/CardHeader.vue'
import CardTitle from './ui/CardTitle.vue'
import CardDescription from './ui/CardDescription.vue'
import CardContent from './ui/CardContent.vue'
import Button from './ui/Button.vue'
import Input from './ui/Input.vue'
import Label from './ui/Label.vue'

const router = useRouter()
const notification = useNotification()

const loading = ref(false)
const codeSending = ref(false)
const captchaImage = ref('')
const captchaToken = ref('')

// Configuration state
const config = reactive({
  emailEnabled: true,
  captchaEnabled: false,
  requirePassword: false,
  passwordRegex: '^[a-zA-Z0-9_]{3,16}$'
})

const form = reactive({
  username: '',
  email: '',
  code: '',
  captchaAnswer: '',
  password: ''
})

const errors = reactive({
  username: '',
  email: '',
  code: '',
  captcha: '',
  password: ''
})

onMounted(async () => {
  // Load configuration
  try {
    const configResponse = await apiService.getConfig()
    if (configResponse.captcha) {
      config.captchaEnabled = configResponse.captcha.enabled
      config.emailEnabled = configResponse.captcha.email_enabled
    }
    if (configResponse.authme) {
      config.requirePassword = configResponse.authme.enabled && configResponse.authme.require_password
      config.passwordRegex = configResponse.authme.password_regex || '^[a-zA-Z0-9_]{3,16}$'
    }
    
    // Load captcha if enabled
    if (config.captchaEnabled) {
      await refreshCaptcha()
    }
  } catch (error) {
    console.error('Failed to load config:', error)
  }
})

const refreshCaptcha = async () => {
  try {
    const response = await apiService.getCaptcha()
    if (response.success && response.token && response.image) {
      captchaToken.value = response.token
      captchaImage.value = response.image
    }
  } catch (error) {
    console.error('Failed to load captcha:', error)
  }
}

const validateUsername = () => {
  errors.username = ''
  const username = form.username.trim()
  if (!username) {
    errors.username = t('register.validation.username_required')
    return false
  }
  
  if (username.length < 3) {
    errors.username = t('register.validation.username_length')
    return false
  }
  
  return true
}

const validateEmail = () => {
  errors.email = ''
  const email = form.email.trim()
  if (!email) {
    errors.email = t('register.validation.email_required')
    return false
  }
  
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  if (!emailRegex.test(email)) {
    errors.email = t('register.validation.email_format')
    return false
  }
  
  return true
}

const validateCode = () => {
  if (!config.emailEnabled) return true
  
  errors.code = ''
  const code = form.code.trim()
  if (!code) {
    errors.code = t('register.validation.code_required')
    return false
  }
  
  return true
}

const validateCaptcha = () => {
  if (!config.captchaEnabled) return true
  
  errors.captcha = ''
  const captcha = form.captchaAnswer.trim()
  if (!captcha) {
    errors.captcha = t('register.validation.captcha_required')
    return false
  }
  
  return true
}

const validatePassword = () => {
  if (!config.requirePassword) return true
  
  errors.password = ''
  const password = form.password.trim()
  if (!password) {
    errors.password = t('register.validation.password_required')
    return false
  }
  
  const regex = new RegExp(config.passwordRegex)
  if (!regex.test(password)) {
    errors.password = t('register.validation.password_format', { regex: config.passwordRegex })
    return false
  }
  
  return true
}

const sendCode = async () => {
  if (!form.email) {
    notification.error(t('common.error'), t('register.validation.email_required'))
    return
  }
  
  if (!validateEmail()) {
    return
  }
  
  codeSending.value = true
  
  try {
    const email = form.email.trim().toLowerCase()
    
    const response = await apiService.sendCode({
      email: email,
      language: locale.value
    })
    
    if (response.success) {
      notification.success(t('register.code_sent'), response.msg)
    } else {
      notification.error(t('register.send_failed'), response.msg)
    }
  } catch (error: any) {
    console.error('Send code error:', error)
    notification.error(t('register.send_failed'), t('register.send_failed'))
  } finally {
    codeSending.value = false
  }
}

const handleSubmit = async () => {
  // Validate form
  const usernameValid = validateUsername()
  const emailValid = validateEmail()
  const codeValid = validateCode()
  const captchaValid = validateCaptcha()
  const passwordValid = validatePassword()
  
  if (!usernameValid || !emailValid || !codeValid || !captchaValid || !passwordValid) {
    // Show first error notification
    if (!usernameValid) {
      notification.error(t('common.error'), errors.username)
    } else if (!emailValid) {
      notification.error(t('common.error'), errors.email)
    } else if (!codeValid) {
      notification.error(t('common.error'), errors.code)
    } else if (!captchaValid) {
      notification.error(t('common.error'), errors.captcha)
    } else if (!passwordValid) {
      notification.error(t('common.error'), errors.password)
    }
    return
  }

  loading.value = true

  try {
    // Generate standard UUID format
    const uuid = (() => {
      if (typeof crypto !== 'undefined' && crypto.randomUUID) {
        return crypto.randomUUID()
      }
      // Fallback: generate standard UUID format
      const generateUUID = () => {
        const hex = '0123456789abcdef'
        const uuid = []
        for (let i = 0; i < 36; i++) {
          if (i === 8 || i === 13 || i === 18 || i === 23) {
            uuid.push('-')
          } else {
            uuid.push(hex[Math.floor(Math.random() * 16)])
          }
        }
        return uuid.join('')
      }
      return generateUUID()
    })()
    
    const registerData: any = {
      email: form.email.trim().toLowerCase(),
      uuid: uuid,
      username: form.username,
      language: locale.value
    }
    
    // Add email code if enabled
    if (config.emailEnabled) {
      registerData.code = form.code
    }
    
    // Add captcha data if enabled
    if (config.captchaEnabled) {
      registerData.captchaToken = captchaToken.value
      registerData.captchaAnswer = form.captchaAnswer
    }
    
    // Add password if required
    if (config.requirePassword) {
      registerData.password = form.password
    }
    
    const response = await apiService.register(registerData)
    
    if (response.success) {
      notification.success(t('register.success'), response.msg && response.msg !== t('register.success') ? response.msg : '')
      
      // Clear form
      Object.assign(form, {
        username: '',
        email: '',
        code: '',
        captchaAnswer: '',
        password: ''
      })
      
      // Refresh captcha if enabled
      if (config.captchaEnabled) {
        await refreshCaptcha()
      }
    } else {
      notification.error(t('register.failed'), response.msg && response.msg !== t('register.failed') ? response.msg : '')
      
      // Refresh captcha on failure
      if (config.captchaEnabled) {
        await refreshCaptcha()
      }
    }
    
  } catch (error) {
    console.error('Registration error:', error)
    notification.error(t('register.failed'), t('register.failed'))
    
    // Refresh captcha on error
    if (config.captchaEnabled) {
      await refreshCaptcha()
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.captcha-image-container {
  min-width: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
