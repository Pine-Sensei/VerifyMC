<template>
  <div class="flex flex-col gap-6">
    <Card>
      <CardHeader>
        <CardTitle class="text-2xl">{{ $t('login.title') }}</CardTitle>
        <CardDescription>{{ $t('login.subtitle') }}</CardDescription>
      </CardHeader>
      <CardContent>
        <form @submit.prevent="handleSubmit">
          <div class="flex flex-col gap-6">
            <div class="grid gap-2">
              <Label for="username">{{ $t('login.form.username') }}</Label>
              <Input
                id="username"
                type="text"
                :placeholder="$t('login.form.username_placeholder')"
                v-model="form.username"
                :state="errors.username ? 'error' : 'default'"
              />
            </div>

            <div class="grid gap-2">
              <Label for="password">{{ $t('login.form.password') }}</Label>
              <Input
                id="password"
                type="password"
                :placeholder="$t('login.form.password_placeholder')"
                v-model="form.password"
                :state="errors.password ? 'error' : 'default'"
              />
            </div>

            <button
              type="submit"
              :disabled="loading"
              class="btn-primary w-full min-h-11 px-4 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              <div v-if="loading" class="spinner" />
              <span>{{ loading ? $t('common.loading') : $t('login.form.submit') }}</span>
            </button>
          </div>
        </form>
      </CardContent>
    </Card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useNotification } from '@/composables/useNotification'
import { apiService } from '@/services/api'
import { sessionService } from '@/services/session'
import Card from './ui/Card.vue'
import CardHeader from './ui/CardHeader.vue'
import CardTitle from './ui/CardTitle.vue'
import CardDescription from './ui/CardDescription.vue'
import CardContent from './ui/CardContent.vue'

import Input from './ui/Input.vue'
import Label from './ui/Label.vue'

const { t, locale } = useI18n()
const router = useRouter()
const route = useRoute()
const notification = useNotification()

const loading = ref(false)

// 使用 ref 保存定时器 ID，确保组件卸载时能正确清理
const redirectTimeout = ref<ReturnType<typeof setTimeout> | null>(null)

const form = reactive({
  username: '',
  password: ''
})

const errors = reactive({
  username: '',
  password: ''
})

const validateForm = () => {
  errors.username = ''
  errors.password = ''
  
  const username = form.username.trim()
  const password = form.password.trim()
  
  let isValid = true
  
  if (!username) {
    errors.username = t('login.validation.username_required')
    isValid = false
  }
  
  if (!password) {
    errors.password = t('login.validation.password_required')
    isValid = false
  }
  
  return isValid
}

const handleSubmit = async () => {
  if (!validateForm()) {
    const firstError = errors.username || errors.password
    if (firstError) {
      notification.error(firstError)
    }
    return
  }

  loading.value = true

  try {
    const response = await apiService.adminLogin({
      username: form.username.trim(),
      password: form.password,
      language: locale.value
    })
    
    if (response.success) {
      sessionService.setToken(response.token)
      
      // Store user info including admin status
      sessionService.setUserInfo({
        username: response.username || form.username.trim(),
        isAdmin: response.isAdmin ?? false
      })
      
      notification.success(response.message || t('login.messages.success'))
      const redirect = typeof route.query.redirect === 'string' && route.query.redirect.startsWith('/')
        ? route.query.redirect
        : '/dashboard'

      redirectTimeout.value = setTimeout(() => {
        router.push(redirect)
      }, 1000)
    } else {
      notification.error(response.message || t('login.messages.error'))
    }
    
  } catch (error) {
    notification.error(t('login.messages.invalid_credentials'))
  } finally {
    loading.value = false
  }
}

onUnmounted(() => {
  // 清理定时器
  if (redirectTimeout.value) {
    clearTimeout(redirectTimeout.value)
    redirectTimeout.value = null
  }
})
</script> 

<style scoped>
.spinner {
  width: 14px;
  height: 14px;
  border: 2px solid rgba(255, 255, 255, 0.35);
  border-top-color: #fff;
  border-radius: 999px;
  animation: spin 0.9s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
