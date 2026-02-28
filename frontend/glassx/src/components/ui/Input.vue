<template>
  <input
    :id="id"
    :type="type"
    :value="modelValue"
    @input="$emit('update:modelValue', ($event.target as HTMLInputElement).value)"
    :class="['glass-input', stateClasses[state], className]"
    v-bind="$attrs"
  />
</template>

<script setup lang="ts">
type InputType = 'text' | 'password' | 'email' | 'number' | 'tel' | 'url' | 'search' | 'date' | 'time' | 'datetime-local'
type InputState = 'default' | 'error' | 'disabled'

interface Props {
  id?: string
  type?: InputType
  className?: string
  modelValue?: string
  state?: InputState
}

interface Emits {
  'update:modelValue': [value: string]
}

withDefaults(defineProps<Props>(), {
  id: undefined,
  type: 'text',
  modelValue: '',
  state: 'default',
})

defineEmits<Emits>()

const stateClasses = {
  default: '',
  error: 'border-red-400/60 focus:border-red-400/70 focus:shadow-[0_0_0_3px_rgba(239,68,68,0.22)]',
  disabled: 'opacity-60 cursor-not-allowed',
}
</script>
