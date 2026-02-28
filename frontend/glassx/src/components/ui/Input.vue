<template>
  <input
    :id="id"
    :type="type"
    :value="modelValue"
    @input="$emit('update:modelValue', ($event.target as HTMLInputElement).value)"
    :class="[
      'glass-input w-full',
      className
    ]"
    v-bind="$attrs"
  />
</template>

<script setup lang="ts">
type InputType = 'text' | 'password' | 'email' | 'number' | 'tel' | 'url' | 'search' | 'date' | 'time' | 'datetime-local'

interface Props {
  id?: string
  type?: InputType
  className?: string
  modelValue?: string
}

interface Emits {
  'update:modelValue': [value: string]
}

withDefaults(defineProps<Props>(), {
  id: undefined,
  type: 'text',
  modelValue: ''
})

defineEmits<Emits>()
</script> 
