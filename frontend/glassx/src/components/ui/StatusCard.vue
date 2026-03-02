<template>
  <div class="glass-card p-6 border-l-4" :class="statusCardClass">
    <div class="flex items-center space-x-4">
      <div class="flex-shrink-0">
        <div class="w-12 h-12 rounded-xl flex items-center justify-center" :class="statusIconBg">
          <Clock v-if="status.status === 'pending'" class="w-6 h-6 text-white" />
          <CheckCircle v-else-if="status.status === 'approved'" class="w-6 h-6 text-white" />
          <XCircle v-else class="w-6 h-6 text-white" />
        </div>
      </div>
      <div class="flex-1">
        <h3 class="text-xl font-bold text-white mb-1">
          {{ $t(`user_status.status.${status.status}`) }}
        </h3>
        <p class="text-white/70">
          {{ $t(`user_status.messages.${status.status}`) }}
        </p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Clock, CheckCircle, XCircle } from 'lucide-vue-next'

interface Status {
  status: string
  reason?: string
}

const props = defineProps<{
  status: Status
}>()

const statusCardClass = computed(() => {
  switch (props.status.status) {
    case 'pending':
      return 'border-yellow-400 bg-yellow-900/20'
    case 'approved':
      return 'border-green-400 bg-green-900/20'
    case 'rejected':
      return 'border-red-400 bg-red-900/20'
    default:
      return 'border-white/10 bg-white/10'
  }
})

const statusIconBg = computed(() => {
  switch (props.status.status) {
    case 'pending':
      return 'bg-yellow-500'
    case 'approved':
      return 'bg-green-500'
    case 'rejected':
      return 'bg-red-500'
    default:
      return 'bg-white/20'
  }
})
</script>
