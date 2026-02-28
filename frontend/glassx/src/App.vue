<template>
  <AppShell>
    <router-view />
    <NotificationSystem ref="notificationSystemRef" />
  </AppShell>
</template>

<script setup lang="ts">
import { computed, inject, onMounted, ref, watch, type Ref } from 'vue'
import { useNotification } from '@/composables/useNotification'
import NotificationSystem from '@/components/NotificationSystem.vue'
import AppShell from '@/components/layout/AppShell.vue'

interface AppConfig {
  webServerPrefix?: string
}

const config = inject<Ref<AppConfig>>('config', ref({}))
const reloadConfig = inject<() => Promise<boolean>>('reloadConfig', async () => false)
const { setNotificationSystem } = useNotification()
const notificationSystemRef = ref<InstanceType<typeof NotificationSystem> | null>(null)

const currentTitle = computed(() => config.value?.webServerPrefix || 'VerifyMC - GlassX Theme')

watch(
  () => currentTitle.value,
  (newTitle) => {
    document.title = newTitle
  },
  { immediate: true },
)

onMounted(() => {
  if (notificationSystemRef.value) {
    setNotificationSystem(notificationSystemRef.value)
  }
})

if (typeof window !== 'undefined') {
  (window as Window & { reloadVerifyMCConfig?: () => Promise<boolean> }).reloadVerifyMCConfig = reloadConfig
}
</script>
