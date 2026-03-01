<template>
  <Dialog
    :show="show"
    :title="title"
    :aria-describedby="messageId"
    max-width="max-w-md"
    @close="handleCancel"
  >
    <div class="mb-2">
      <p :id="messageId" class="text-white/80">{{ message }}</p>
    </div>
    
    <template #footer>
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
    </template>
  </Dialog>
</template>

<script setup lang="ts">
import { ref, watch, nextTick, useId, type ComponentPublicInstance } from 'vue'
import Button from '@/components/ui/Button.vue'
import Dialog from '@/components/ui/Dialog.vue'

// Generate unique IDs for ARIA attributes
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

const cancelBtnRef = ref<ComponentPublicInstance | null>(null)

const handleConfirm = () => {
  emit('confirm')
}

const handleCancel = () => {
  emit('cancel')
}

// Special focus handling for ConfirmDialog - prefer cancel button
watch(() => props.show, async (newShow) => {
  if (newShow) {
    await nextTick()
    // Give Dialog time to finish its focus logic, then override if needed
    setTimeout(() => {
      if (cancelBtnRef.value?.$el) {
        (cancelBtnRef.value.$el as HTMLElement).focus()
      }
    }, 50)
  }
})
</script> 
