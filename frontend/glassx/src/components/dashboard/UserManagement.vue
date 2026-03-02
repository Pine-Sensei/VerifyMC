<template>
  <div class="w-full space-y-4">
    <!-- Tag Navigation -->
    <div class="flex items-center space-x-2 overflow-x-auto pb-2 pt-1 scrollbar-hide">
      <Button
        v-for="tag in tags"
        :key="tag.id"
        @click="activeTag = tag.id"
        :variant="activeTag === tag.id ? 'default' : 'ghost'"
        class="rounded-full"
      >
        {{ tag.label }}
      </Button>
    </div>

    <!-- Content Area -->
    <div class="min-h-[400px]">
      <transition
        mode="out-in"
        enter-active-class="transition duration-200 ease-out"
        enter-from-class="opacity-0 translate-y-2"
        enter-to-class="opacity-100 translate-y-0"
        leave-active-class="transition duration-150 ease-in"
        leave-from-class="opacity-100 translate-y-0"
        leave-to-class="opacity-0 translate-y-2"
      >
        <component :is="activeComponent" ref="activeComponentRef" />
      </transition>
    </div>

    <!-- Version Update Notification -->
    <VersionUpdateNotification />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, inject, shallowRef } from 'vue'
import { useI18n } from 'vue-i18n'
import { useWebSocket } from '@/composables/useWebSocket'
import Button from '@/components/ui/Button.vue'
import VersionUpdateNotification from '@/components/ui/VersionUpdateNotification.vue'
import UserList from './users/UserList.vue'
import PendingReviews from './users/PendingReviews.vue'

const { t } = useI18n()

const activeTag = ref('all')
const activeComponentRef = ref()

const tags = computed(() => [
  { id: 'all', label: t('admin.tabs.users'), component: UserList },
  { id: 'pending', label: t('admin.tabs.review'), component: PendingReviews }
])

const activeComponent = computed(() => {
  const tag = tags.value.find(t => t.id === activeTag.value)
  return tag ? tag.component : UserList
})

const getWsPort = inject<() => number>('getWsPort', () => window.location.port ? (parseInt(window.location.port, 10) + 1) : 8081)

const getWsUrl = () => {
  const wsProtocol = window.location.protocol === 'https:' ? 'wss' : 'ws'
  const wsHost = window.location.hostname
  const wsPort = getWsPort()
  return `${wsProtocol}://${wsHost}:${wsPort}`
}

const handleWsMessage = () => {
  // Refresh the current active component if it has the method
  if (activeComponentRef.value) {
    if (activeTag.value === 'all' && activeComponentRef.value.loadAllUsers) {
      activeComponentRef.value.loadAllUsers()
    } else if (activeTag.value === 'pending' && activeComponentRef.value.loadPendingUsers) {
      activeComponentRef.value.loadPendingUsers()
    }
  }
}

useWebSocket(getWsUrl, {
  onMessage: handleWsMessage
})
</script>
