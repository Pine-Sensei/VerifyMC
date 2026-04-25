<template>
  <div class="flex flex-col gap-6">
    <Card>
      <CardHeader>
        <CardTitle class="text-2xl">{{ $t('login.title') }}</CardTitle>
        <CardDescription>{{ $t('login.subtitle') }}</CardDescription>
      </CardHeader>
      <CardContent>
        <form @submit.prevent="handleSubmit" class="flex flex-col gap-5">
          <div v-if="identifierOptions.length > 1" class="grid grid-cols-3 gap-2 rounded-lg bg-white/5 p-1">
            <Button
              v-for="option in identifierOptions"
              :key="option"
              type="button"
              variant="outline"
              class="border-transparent"
              :class="identifierType === option ? 'bg-white/20 text-white' : 'text-white/60 hover:bg-white/10'"
              @click="selectIdentifier(option)"
            >
              {{ $t(`login.methods.${option}`) }}
            </Button>
          </div>

          <div v-if="authOptions.length > 1" class="grid grid-cols-2 gap-2 rounded-lg bg-white/5 p-1">
            <Button
              v-for="option in authOptions"
              :key="option"
              type="button"
              variant="outline"
              class="border-transparent"
              :class="authMethod === option ? 'bg-white/20 text-white' : 'text-white/60 hover:bg-white/10'"
              @click="authMethod = option"
            >
              {{ $t(`login.auth.${option}`) }}
            </Button>
          </div>

          <div v-if="identifierType === 'phone'" class="grid grid-cols-[7rem_1fr] gap-2">
            <div class="grid gap-2">
              <Label for="countryCode">{{ $t('login.form.country_code') }}</Label>
              <Input id="countryCode" v-model="form.countryCode" placeholder="+86" />
            </div>
            <div class="grid gap-2">
              <Label for="identifier">{{ $t('login.form.phone') }}</Label>
              <Input id="identifier" v-model="form.identifier" type="tel" :placeholder="$t('login.form.phone_placeholder')" />
            </div>
          </div>

          <div v-else class="grid gap-2">
            <Label for="identifier">{{ identifierLabel }}</Label>
            <Input id="identifier" v-model="form.identifier" type="text" :placeholder="identifierPlaceholder" />
          </div>

          <div v-if="authMethod === 'password'" class="grid gap-2">
            <Label for="password">{{ $t('login.form.password') }}</Label>
            <Input id="password" v-model="form.password" type="password" :placeholder="$t('login.form.password_placeholder')" />
          </div>

          <div v-else class="grid gap-2">
            <Label for="code">{{ $t('login.form.code') }}</Label>
            <div class="flex flex-col sm:flex-row gap-2">
              <Input id="code" v-model="form.code" inputmode="numeric" :placeholder="$t('login.form.code_placeholder')" />
              <Button type="button" variant="secondary" class="whitespace-nowrap" :disabled="sendingCode" @click="sendLoginCode">
                {{ sendingCode ? $t('register.sending') : $t('register.sendCode') }}
              </Button>
            </div>
          </div>

          <div v-if="accounts.length" class="rounded-lg border border-white/10 bg-white/5 p-3">
            <p class="mb-3 text-sm text-white/70">{{ $t('login.account_select') }}</p>
            <div class="grid gap-2">
              <button
                v-for="account in accounts"
                :key="account.username"
                type="button"
                class="rounded-lg border px-3 py-2 text-left transition"
                :class="form.selectedUsername === account.username ? 'border-blue-300 bg-blue-400/15 text-white' : 'border-white/10 bg-white/5 text-white/70 hover:bg-white/10'"
                @click="form.selectedUsername = account.username"
              >
                <span class="block font-medium">{{ account.username }}</span>
                <span class="text-xs text-white/50">{{ account.email || account.phone || account.status }}</span>
              </button>
            </div>
          </div>

          <Button type="submit" :disabled="loading" class="w-full">
            <div v-if="loading" class="mr-2 h-4 w-4 animate-spin rounded-full border-2 border-white border-t-transparent"></div>
            <span>{{ loading ? $t('common.loading') : $t('login.form.submit') }}</span>
          </Button>

          <router-link
            v-if="config?.forgotPassword?.enabled"
            to="/forgot-password"
            class="text-center text-sm text-white/60 transition hover:text-white"
          >
            {{ $t('login.forgot_password') }}
          </router-link>
        </form>
      </CardContent>
    </Card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useNotification } from '@/composables/useNotification'
import { apiService, type AccountSummary, type ConfigResponse } from '@/services/api'
import { sessionService } from '@/services/session'
import Card from './ui/Card.vue'
import CardHeader from './ui/CardHeader.vue'
import CardTitle from './ui/CardTitle.vue'
import CardDescription from './ui/CardDescription.vue'
import CardContent from './ui/CardContent.vue'
import Button from './ui/Button.vue'
import Input from './ui/Input.vue'
import Label from './ui/Label.vue'

type IdentifierType = 'username' | 'email' | 'phone'
type AuthMethod = 'password' | 'code'

const { t, locale } = useI18n()
const router = useRouter()
const route = useRoute()
const notification = useNotification()

const loading = ref(false)
const sendingCode = ref(false)
const config = ref<ConfigResponse | null>(null)
const accounts = ref<AccountSummary[]>([])
const identifierType = ref<IdentifierType>('username')
const authMethod = ref<AuthMethod>('password')
const redirectTimeout = ref<ReturnType<typeof setTimeout> | null>(null)

const form = reactive({
  identifier: '',
  countryCode: '+86',
  password: '',
  code: '',
  selectedUsername: ''
})

const allowedMethods = computed(() => config.value?.login?.allowedMethods?.length ? config.value.login.allowedMethods : ['username', 'email_password'])
const identifierOptions = computed<IdentifierType[]>(() => {
  const options: IdentifierType[] = []
  if (allowedMethods.value.includes('username')) options.push('username')
  if (allowedMethods.value.some(method => method.startsWith('email_'))) options.push('email')
  if (allowedMethods.value.some(method => method.startsWith('phone_'))) options.push('phone')
  return options.length ? options : ['username']
})
const authOptions = computed<AuthMethod[]>(() => {
  if (identifierType.value === 'username') return ['password']
  const prefix = identifierType.value
  const options: AuthMethod[] = []
  if (allowedMethods.value.includes(`${prefix}_password`)) options.push('password')
  if (allowedMethods.value.includes(`${prefix}_code`)) options.push('code')
  return options.length ? options : ['password']
})
const identifierLabel = computed(() => t(`login.form.${identifierType.value}`))
const identifierPlaceholder = computed(() => t(`login.form.${identifierType.value}_placeholder`))

watch(identifierType, () => {
  authMethod.value = authOptions.value[0] || 'password'
  accounts.value = []
  form.selectedUsername = ''
})

watch(authOptions, options => {
  if (!options.includes(authMethod.value)) {
    authMethod.value = options[0] || 'password'
  }
})

const selectIdentifier = (type: IdentifierType) => {
  identifierType.value = type
}

const loadConfig = async () => {
  try {
    config.value = await apiService.getConfig()
    identifierType.value = identifierOptions.value[0] || 'username'
  } catch {
    config.value = null
  }
}

const validateForm = () => {
  if (!form.identifier.trim()) {
    notification.error(t(`login.validation.${identifierType.value}_required`))
    return false
  }
  if (identifierType.value === 'phone' && !form.countryCode.trim()) {
    notification.error(t('login.validation.country_code_required'))
    return false
  }
  if (authMethod.value === 'password' && !form.password) {
    notification.error(t('login.validation.password_required'))
    return false
  }
  if (authMethod.value === 'code' && !form.code.trim()) {
    notification.error(t('login.validation.code_required'))
    return false
  }
  if (accounts.value.length > 0 && !form.selectedUsername) {
    notification.error(t('login.validation.account_required'))
    return false
  }
  return true
}

const sendLoginCode = async () => {
  if (!form.identifier.trim()) {
    notification.error(t(`login.validation.${identifierType.value}_required`))
    return
  }
  if (identifierType.value === 'phone' && !form.countryCode.trim()) {
    notification.error(t('login.validation.country_code_required'))
    return
  }

  sendingCode.value = true
  try {
    const response = await apiService.sendCode({
      channel: identifierType.value === 'phone' ? 'sms' : 'email',
      purpose: 'login',
      email: identifierType.value === 'email' ? form.identifier.trim() : undefined,
      phone: identifierType.value === 'phone' ? form.identifier.trim() : undefined,
      countryCode: identifierType.value === 'phone' ? form.countryCode.trim() : undefined,
      language: locale.value
    })
    response.success ? notification.success(response.message) : notification.error(response.message)
  } catch (error) {
    notification.error(error instanceof Error ? error.message : t('register.send_failed'))
  } finally {
    sendingCode.value = false
  }
}

const handleSubmit = async () => {
  if (!validateForm()) return
  loading.value = true

  try {
    const response = await apiService.login({
      identifier: form.identifier.trim(),
      identifierType: identifierType.value,
      authMethod: authMethod.value,
      password: form.password,
      code: form.code.trim(),
      countryCode: identifierType.value === 'phone' ? form.countryCode.trim() : undefined,
      selectedUsername: form.selectedUsername || undefined,
      language: locale.value
    })

    if (response.code === 'ACCOUNT_SELECTION_REQUIRED' && response.accounts?.length) {
      accounts.value = response.accounts
      notification.info(response.message || t('login.account_select'))
      return
    }

    if (response.success && response.token) {
      sessionService.setToken(response.token)
      sessionService.setUserInfo({
        username: response.username || form.selectedUsername || form.identifier.trim(),
        isAdmin: response.isAdmin ?? false
      })
      notification.success(response.message || t('login.messages.success'))
      const redirect = typeof route.query.redirect === 'string' && route.query.redirect.startsWith('/')
        ? route.query.redirect
        : '/dashboard'
      redirectTimeout.value = setTimeout(() => router.push(redirect), 700)
    } else {
      notification.error(response.message || t('login.messages.error'))
    }
  } catch (error) {
    notification.error(error instanceof Error ? error.message : t('login.messages.invalid_credentials'))
  } finally {
    loading.value = false
  }
}

onMounted(loadConfig)

onUnmounted(() => {
  if (redirectTimeout.value) {
    clearTimeout(redirectTimeout.value)
    redirectTimeout.value = null
  }
})
</script>
