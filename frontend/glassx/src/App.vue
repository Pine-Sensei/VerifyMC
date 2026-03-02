<template>
  <div id="app">
    <!-- Top Navigation -->
    <TopNavigation />

    <!-- Main Content with top padding for navigation -->
    <div class="pt-16 pb-safe relative z-10">
      <router-view />
    </div>

    <!-- Notification System -->
    <NotificationSystem ref="notificationSystemRef" />

    <!-- Enhanced Footer -->
    <AppFooter />
  </div>
</template>

<script setup lang="ts">
import { inject, watch, ref, onMounted, type Ref } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useNotification } from '@/composables/useNotification'
import NotificationSystem from '@/components/NotificationSystem.vue'
import TopNavigation from '@/components/TopNavigation.vue'
import AppFooter from '@/components/AppFooter.vue'
import type { AppConfig } from '@/types'

const config = inject<Ref<AppConfig>>('config', ref({}))
const reloadConfig = inject<() => Promise<boolean>>('reloadConfig', async () => false)
const { setNotificationSystem } = useNotification()

const notificationSystemRef = ref()

onMounted(() => {
  if (notificationSystemRef.value) {
    setNotificationSystem(notificationSystemRef.value)
  }
})

const route = useRoute()
const { t, locale } = useI18n()

// Update document title
watch(
  [() => route.meta.title, () => config.value?.webServerPrefix, () => locale.value],
  ([newTitle]) => {
    const title = newTitle ? t(newTitle as string) : ''
    const prefix = config.value?.webServerPrefix || 'VerifyMC'
    document.title = title ? `${title} - ${prefix}` : prefix
  },
  { immediate: true }
)

// Update title when language changes


// 暴露重载配置方法给全局
if (typeof window !== 'undefined') {
  (window as any).reloadVerifyMCConfig = reloadConfig
}
</script>

