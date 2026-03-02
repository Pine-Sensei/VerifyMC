<template>
  <Teleport to="body">
    <div class="fixed right-4 top-4 z-50 space-y-3 max-w-sm w-full" aria-live="polite" aria-atomic="true">
      <TransitionGroup
        enter-active-class="transition-all duration-300 ease-out"
        enter-from-class="opacity-0 translate-x-full scale-95"
        enter-to-class="opacity-100 translate-x-0 scale-100"
        leave-active-class="transition-all duration-200 ease-in"
        leave-from-class="opacity-100 translate-x-0 scale-100"
        leave-to-class="opacity-0 translate-x-full scale-95"
        move-class="transition-all duration-300 ease-out"
      >
        <div 
          v-for="notification in notifications" 
          :key="notification.id" 
          class="glass-card relative overflow-hidden p-4 shadow-lg pr-10"
          :class="getNotificationClasses(notification.type)"
          role="alert"
          @mouseenter="pauseTimer(notification.id)"
          @mouseleave="resumeTimer(notification.id)"
        >
          <div class="flex items-start gap-3">
            <div class="flex-shrink-0 mt-0.5">
              <CheckCircle v-if="notification.type === 'success'" class="w-5 h-5 text-green-400" />
              <XCircle v-else-if="notification.type === 'error'" class="w-5 h-5 text-red-400" />
              <AlertCircle v-else-if="notification.type === 'warning'" class="w-5 h-5 text-yellow-400" />
              <Info v-else class="w-5 h-5 text-blue-400" />
            </div>
            <div class="flex-1 min-w-0">
              <p class="text-sm font-semibold text-white">{{ notification.title }}</p>
              <p v-if="notification.message" class="text-sm text-gray-200 mt-1 leading-relaxed">{{ notification.message }}</p>
            </div>
          </div>
          
          <button
            @click="removeNotification(notification.id)"
            class="glass-button-ghost absolute top-2 right-2 p-1.5 rounded-full text-white/70 hover:text-white transition-colors"
            :aria-label="$t('common.close')"
            :title="$t('common.close')"
          >
            <X class="w-4 h-4" />
          </button>

          <!-- Progress Bar -->
          <div class="absolute bottom-0 left-0 h-1 bg-white/20 w-full">
            <div 
              class="h-full transition-all duration-100 ease-linear"
              :class="getProgressBarColor(notification.type)"
              :style="{ width: `${(notification.remaining / notification.duration) * 100}%` }"
            ></div>
          </div>
        </div>
      </TransitionGroup>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, onUnmounted } from 'vue'
import { CheckCircle, XCircle, AlertCircle, Info, X } from 'lucide-vue-next'
import type { Notification, NotificationInput } from '@/types'

const notifications = ref<Notification[]>([])
let animationFrameId: number | null = null
let lastTime = 0

const getNotificationClasses = (type: string) => {
  switch (type) {
    case 'success':
      return 'border-l-4 border-l-green-400'
    case 'error':
      return 'border-l-4 border-l-red-400'
    case 'warning':
      return 'border-l-4 border-l-yellow-400'
    default:
      return 'border-l-4 border-l-blue-400'
  }
}

const getProgressBarColor = (type: string) => {
  switch (type) {
    case 'success': return 'bg-green-400'
    case 'error': return 'bg-red-400'
    case 'warning': return 'bg-yellow-400'
    default: return 'bg-blue-400'
  }
}

const addNotification = (input: NotificationInput) => {
  const id = Date.now().toString() + Math.random().toString(36).substr(2, 9)
  const duration = input.duration ?? 5000
  
  const notification: Notification = {
    ...input,
    id,
    duration,
    remaining: duration,
    paused: false
  }
  
  notifications.value.push(notification)
  
  if (!animationFrameId) {
    lastTime = performance.now()
    animationFrameId = requestAnimationFrame(updateTimers)
  }
}

const updateTimers = (timestamp: number) => {
  const deltaTime = timestamp - lastTime
  lastTime = timestamp
  
  let hasActiveNotifications = false
  
  // Create a copy to avoid issues when removing items during iteration
  const currentNotifications = [...notifications.value]
  
  for (const notification of currentNotifications) {
    if (!notification.paused) {
      notification.remaining -= deltaTime
      
      if (notification.remaining <= 0) {
        removeNotification(notification.id)
      } else {
        hasActiveNotifications = true
      }
    } else {
      hasActiveNotifications = true
    }
  }
  
  if (hasActiveNotifications && notifications.value.length > 0) {
    animationFrameId = requestAnimationFrame(updateTimers)
  } else {
    animationFrameId = null
  }
}

const removeNotification = (id: string) => {
  const index = notifications.value.findIndex(n => n.id === id)
  if (index > -1) {
    notifications.value.splice(index, 1)
  }
  
  if (notifications.value.length === 0 && animationFrameId) {
    cancelAnimationFrame(animationFrameId)
    animationFrameId = null
  }
}

const pauseTimer = (id: string) => {
  const notification = notifications.value.find(n => n.id === id)
  if (notification) {
    notification.paused = true
  }
}

const resumeTimer = (id: string) => {
  const notification = notifications.value.find(n => n.id === id)
  if (notification) {
    notification.paused = false
    // If the loop stopped, restart it
    if (!animationFrameId && notifications.value.length > 0) {
      lastTime = performance.now()
      animationFrameId = requestAnimationFrame(updateTimers)
    }
  }
}

// Clean up on unmount
onUnmounted(() => {
  if (animationFrameId) {
    cancelAnimationFrame(animationFrameId)
  }
})

// Expose methods for external use
defineExpose({
  addNotification,
  removeNotification
})
</script>

<style scoped>
.glass-button-ghost {
  background: rgba(255, 255, 255, 0);
}
.glass-button-ghost:hover {
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(4px);
}
</style>