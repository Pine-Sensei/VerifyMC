<template>
  <label
    :for="forId"
    :class="[
      'text-sm font-medium leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70 text-white/80',
      className
    ]"
    v-bind="$attrs"
  >
    <slot />
  </label>
</template>

<script setup lang="ts">
interface Props {
  forId?: string
  className?: string
}

defineProps<Props>()
</script> 
