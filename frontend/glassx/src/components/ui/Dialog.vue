<template>
  <!-- Fixed positioning modal overlay - ensures proper centering relative to viewport -->
  <div
    v-if="show"
    class="fixed inset-0 z-[60] flex items-center justify-center p-4"
    role="dialog"
    aria-modal="true"
    :aria-labelledby="titleId"
    :aria-describedby="ariaDescribedby"
  >
    <!-- Background mask -->
    <div class="fixed inset-0 bg-black/50 backdrop-blur-xl" @click="handleClose"></div>
    
    <!-- Dialog container -->
    <div 
      class="relative w-full bg-white/5 border border-white/10 rounded-xl p-6 shadow-2xl backdrop-blur-xl flex flex-col max-h-[90vh] outline-none" 
      :class="maxWidth"
      ref="dialogRef"
      tabindex="-1"
    >
      <!-- Header -->
      <div class="flex items-center justify-between mb-4 flex-shrink-0">
        <h3 :id="titleId" class="text-lg font-semibold text-white">{{ title }}</h3>
        <Button 
          variant="ghost"
          size="icon"
          @click="handleClose"
          class="text-white/60 hover:text-white"
          data-dialog-close="true"
          :aria-label="$t('common.close')"
          :title="$t('common.close')"
        >
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
          </svg>
        </Button>
      </div>
      
      <!-- Content -->
      <div class="mb-6 overflow-y-auto flex-1 custom-scrollbar">
        <slot></slot>
      </div>
      
      <!-- Footer Actions -->
      <div v-if="$slots.footer" class="flex gap-3 justify-end flex-shrink-0 pt-2 border-t border-white/5 mt-auto">
        <slot name="footer"></slot>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, nextTick, onUnmounted, useId } from 'vue'
import Button from '@/components/ui/Button.vue'

// Generate unique ID for ARIA attributes
const titleId = useId()

interface Props {
  show: boolean
  title: string
  maxWidth?: string
  ariaDescribedby?: string
}

const props = withDefaults(defineProps<Props>(), {
  maxWidth: 'max-w-md',
  ariaDescribedby: undefined
})

const emit = defineEmits<{
  close: []
}>()

// Refs for focus management
const dialogRef = ref<HTMLElement | null>(null)

// Store the previously focused element
let previouslyFocusedElement: HTMLElement | null = null

const handleClose = () => {
  emit('close')
}

// Handle Escape key press
const handleKeyDown = (event: KeyboardEvent) => {
  if (event.key === 'Escape') {
    handleClose()
  }
}

// Focus management
watch(() => props.show, async (newShow) => {
  if (newShow) {
    // Store the currently focused element
    previouslyFocusedElement = document.activeElement as HTMLElement
    
    // Add keyboard event listener
    document.addEventListener('keydown', handleKeyDown)
    
    // Focus the dialog container or the first focusable element inside
    await nextTick()
    
    // Check for an element with autofocus attribute first
    const autoFocusElement = dialogRef.value?.querySelector('[autofocus]') as HTMLElement
    if (autoFocusElement) {
      autoFocusElement.focus()
      return
    }

    // Try to focus the first input or button in the dialog (excluding close button)
    const focusable = dialogRef.value?.querySelector('input, textarea, select, button:not([data-dialog-close="true"])') as HTMLElement
    if (focusable) {
      focusable.focus()
    } else {
      // Fallback: focus the dialog container itself (make sure it has tabindex="-1")
      dialogRef.value?.focus()
    }
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

<style scoped>
.custom-scrollbar::-webkit-scrollbar {
  width: 6px;
}
.custom-scrollbar::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.05);
  border-radius: 3px;
}
.custom-scrollbar::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.2);
  border-radius: 3px;
}
.custom-scrollbar::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.3);
}
</style>
