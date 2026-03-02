<template>
  <div class="relative">
    <div class="relative">
      <Input
        :modelValue="modelValue"
        @update:modelValue="$emit('update:modelValue', $event)"
        :placeholder="placeholder || $t('common.search')"
        :aria-label="$t('common.search')"
        class="pl-10 pr-10"
      />
      <!-- 搜索图标 -->
      <div class="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none" aria-hidden="true">
        <svg class="h-5 w-5 text-white/60" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path>
        </svg>
      </div>
      <!-- 清除按钮 -->
      <Button
        v-if="modelValue"
        variant="ghost"
        size="icon"
        @click="$emit('update:modelValue', '')"
        :aria-label="$t('common.search') + ' ' + $t('common.clear') || 'Clear search'"
        :title="$t('common.clear') || 'Clear search'"
        class="absolute inset-y-0 right-0 h-full w-10 transition-colors rounded-none rounded-r-md"
      >
        <svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
        </svg>
      </Button>
    </div>
  </div>
</template>

<script setup lang="ts">
import Input from './Input.vue'
import Button from './Button.vue'

interface Props {
  modelValue: string
  placeholder?: string
}

withDefaults(defineProps<Props>(), {
  placeholder: ''
})

defineEmits<{
  'update:modelValue': [value: string]
}>()
</script> 
