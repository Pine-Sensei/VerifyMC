<template>
  <!-- Fixed positioning modal overlay - ensures proper centering relative to viewport -->
  <div
    v-if="show"
    class="modal-overlay"
    role="dialog"
    aria-modal="true"
    :aria-labelledby="titleId"
    :aria-describedby="messageId"
  >
    <!-- Background mask -->
    <div class="absolute inset-0 bg-black/50 backdrop-blur-sm" @click="handleCancel"></div>
    
    <!-- Dialog container -->
    <div class="relative bg-white/5 backdrop-blur-xl border border-white/10 rounded-lg p-6 w-full max-w-md shadow-2xl mx-auto" ref="dialogRef">
      <!-- 标题 -->
      <div class="flex items-center justify-between mb-4">
        <h3 :id="titleId" class="text-lg font-semibold text-white">{{ title }}</h3>
        <Button 
          variant="ghost"
          size="icon"
          @click="handleCancel"
          class="text-white/60 hover:text-white"
          :aria-label="$t('common.close')"
        >
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
          </svg>
        </Button>
      </div>
      
      <!-- 内容 -->
      <div class="mb-6">
        <p :id="messageId" class="text-white/80">{{ message }}</p>
      </div>
      
      <!-- 操作按钮 -->
      <div class="flex gap-3 justify-end">
        <Button 
          ref="cancelBtnRef"
          variant="outline"
          @click="handleCancel"
          class="text-white/80 hover:text-white"
        >
          {{ cancelText || $t('common.cancel') }}
        </Button>
        <Button 
          ref="confirmBtnRef"
          @click="handleConfirm"
          :variant="type === 'danger' ? 'destructive' : 'default'"
        >
          {{ confirmText || $t('common.confirm') }}
        </Button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, nextTick, onUnmounted, useId, type ComponentPublicInstance } from 'vue'
import Button from '@/components/ui/Button.vue'

// Generate unique IDs for ARIA attributes
const titleId = useId()
const messageId = useId()

interface Props {
  show: boolean
  title: string
  message: string
  confirmText?: string
  cancelText?: string
  type?: 'danger' | 'warning' | 'info'
}

const props = withDefaults(defineProps<Props>(), {
  confirmText: '',
  cancelText: '',
  type: 'danger'
})

const emit = defineEmits<{
  confirm: []
  cancel: []
}>()

// Refs for focus management
const dialogRef = ref<HTMLElement | null>(null)
const cancelBtnRef = ref<ComponentPublicInstance | null>(null)
const confirmBtnRef = ref<ComponentPublicInstance | null>(null)

// Store the previously focused element
let previouslyFocusedElement: HTMLElement | null = null

const handleConfirm = () => {
  emit('confirm')
}

const handleCancel = () => {
  emit('cancel')
}

// Handle Escape key press
const handleKeyDown = (event: KeyboardEvent) => {
  if (event.key === 'Escape') {
    handleCancel()
  }
}

// Focus management
watch(() => props.show, async (newShow) => {
  if (newShow) {
    // Store the currently focused element
    previouslyFocusedElement = document.activeElement as HTMLElement
    
    // Add keyboard event listener
    document.addEventListener('keydown', handleKeyDown)
    
    // Focus the cancel button when dialog opens
    await nextTick()
    ;(cancelBtnRef.value?.$el as HTMLElement)?.focus()
  } else {
    // Remove keyboard event listener
    document.removeEventListener('keydown', handleKeyDown)
    
    // Restore focus to the previously focused element
    if (previouslyFocusedElement) {
      previouslyFocusedElement.focus()
      previouslyFocusedElement = null
    }
  }
})

// Cleanup on unmount
onUnmounted(() => {
  document.removeEventListener('keydown', handleKeyDown)
})
</script> 
