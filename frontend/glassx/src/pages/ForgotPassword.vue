<template>
  <div class="relative min-h-svh w-full flex items-center justify-center p-6 md:p-10">
    <div class="relative z-10 w-full max-w-md">
      <Card>
        <CardHeader>
          <CardTitle class="text-2xl">{{ $t('forgot_password.title') }}</CardTitle>
          <CardDescription>{{ $t('forgot_password.subtitle') }}</CardDescription>
        </CardHeader>
        <CardContent>
          <form class="flex flex-col gap-5" @submit.prevent="resetPassword">
            <div v-if="identifierOptions.length > 1" class="grid grid-cols-2 gap-2 rounded-lg bg-white/5 p-1">
              <Button
                v-for="option in identifierOptions"
                :key="option"
                type="button"
                variant="outline"
                class="border-transparent"
                :class="identifierType === option ? 'bg-white/20 text-white' : 'text-white/60 hover:bg-white/10'"
                @click="identifierType = option"
              >
                {{ $t(`login.methods.${option}`) }}
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
              <Label for="identifier">{{ $t('login.form.email') }}</Label>
              <Input id="identifier" v-model="form.identifier" type="email" :placeholder="$t('login.form.email_placeholder')" />
            </div>

            <div class="grid gap-2">
              <Label for="code">{{ $t('login.form.code') }}</Label>
              <div class="flex flex-col sm:flex-row gap-2">
                <Input id="code" v-model="form.code" inputmode="numeric" :placeholder="$t('login.form.code_placeholder')" />
                <Button type="button" variant="secondary" class="whitespace-nowrap" :disabled="sending" @click="sendCode">
                  {{ sending ? $t('register.sending') : $t('register.sendCode') }}
                </Button>
              </div>
            </div>

            <div class="grid gap-2">
              <Label for="newPassword">{{ $t('dashboard.profile.new_password') }}</Label>
              <Input id="newPassword" v-model="form.newPassword" type="password" :placeholder="$t('dashboard.profile.new_password_placeholder')" />
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
              {{ loading ? $t('common.loading') : $t('forgot_password.reset') }}
            </Button>

            <router-link to="/login" class="text-center text-sm text-white/60 transition hover:text-white">
              {{ $t('forgot_password.back_login') }}
            </router-link>
          </form>
        </CardContent>
      </Card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useNotification } from '@/composables/useNotification'
import { apiService, type AccountSummary, type ConfigResponse } from '@/services/api'
import Card from '@/components/ui/Card.vue'
import CardHeader from '@/components/ui/CardHeader.vue'
import CardTitle from '@/components/ui/CardTitle.vue'
import CardDescription from '@/components/ui/CardDescription.vue'
import CardContent from '@/components/ui/CardContent.vue'
import Button from '@/components/ui/Button.vue'
import Input from '@/components/ui/Input.vue'
import Label from '@/components/ui/Label.vue'

type IdentifierType = 'email' | 'phone'

const { t, locale } = useI18n()
const router = useRouter()
const notification = useNotification()

const identifierType = ref<IdentifierType>('email')
const loading = ref(false)
const sending = ref(false)
const accounts = ref<AccountSummary[]>([])
const config = ref<ConfigResponse | null>(null)

const form = reactive({
  identifier: '',
  countryCode: '+86',
  code: '',
  newPassword: '',
  selectedUsername: ''
})

const identifierOptions = computed<IdentifierType[]>(() => {
  const allowedMethods = config.value?.forgotPassword?.allowedMethods ?? ['email_code', 'phone_code']
  const options: IdentifierType[] = []
  if (allowedMethods.includes('email_code')) options.push('email')
  if (allowedMethods.includes('phone_code')) options.push('phone')
  return options.length ? options : ['email']
})

watch(identifierOptions, options => {
  if (!options.includes(identifierType.value)) {
    identifierType.value = options[0] || 'email'
  }
}, { immediate: true })

watch(identifierType, () => {
  accounts.value = []
  form.selectedUsername = ''
})

const loadConfig = async () => {
  try {
    config.value = await apiService.getConfig()
  } catch {
    config.value = null
  }
}

const validateIdentifier = () => {
  if (!form.identifier.trim()) {
    notification.error(t(`login.validation.${identifierType.value}_required`))
    return false
  }
  if (identifierType.value === 'phone' && !form.countryCode.trim()) {
    notification.error(t('login.validation.country_code_required'))
    return false
  }
  return true
}

const sendCode = async () => {
  if (!validateIdentifier()) return
  sending.value = true
  try {
    const response = await apiService.sendForgotPasswordCode({
      identifier: form.identifier.trim(),
      identifierType: identifierType.value,
      countryCode: identifierType.value === 'phone' ? form.countryCode.trim() : undefined,
      language: locale.value
    })
    response.success ? notification.success(response.message) : notification.error(response.message)
  } catch (error) {
    notification.error(error instanceof Error ? error.message : t('register.send_failed'))
  } finally {
    sending.value = false
  }
}

const resetPassword = async () => {
  if (!validateIdentifier()) return
  if (!form.code.trim() || !form.newPassword) {
    notification.error(t('forgot_password.required'))
    return
  }
  if (accounts.value.length && !form.selectedUsername) {
    notification.error(t('login.validation.account_required'))
    return
  }

  loading.value = true
  try {
    const response = await apiService.resetForgotPassword({
      identifier: form.identifier.trim(),
      identifierType: identifierType.value,
      countryCode: identifierType.value === 'phone' ? form.countryCode.trim() : undefined,
      code: form.code.trim(),
      newPassword: form.newPassword,
      selectedUsername: form.selectedUsername || undefined,
      language: locale.value
    })

    if (response.code === 'ACCOUNT_SELECTION_REQUIRED' && response.accounts?.length) {
      accounts.value = response.accounts
      notification.info(response.message || t('login.account_select'))
      return
    }

    if (response.success) {
      notification.success(response.message || t('forgot_password.success'))
      router.push('/login')
    } else {
      notification.error(response.message || t('common.error'))
    }
  } catch (error) {
    notification.error(error instanceof Error ? error.message : t('common.error'))
  } finally {
    loading.value = false
  }
}

onMounted(loadConfig)
</script>
