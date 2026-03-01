<template>
  <button
    class="inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-md text-sm font-medium ring-offset-background transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-50"
    :class="[
      variants[variant],
      sizes[size],
      className
    ]"
    v-bind="$attrs"
  >
    <slot />
  </button>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  variant?: 'default' | 'destructive' | 'outline' | 'secondary' | 'ghost' | 'link'
  size?: 'default' | 'sm' | 'lg' | 'icon' | 'icon-sm' | 'xs'
  className?: string
}

const props = withDefaults(defineProps<Props>(), {
  variant: 'default',
  size: 'default'
})

const variants = {
  default: 'glass-button-primary shadow-lg',
  destructive: 'glass-button-danger',
  outline: 'glass-button bg-transparent hover:bg-white/10 border-white/10',
  secondary: 'glass-button',
  ghost: 'glass-button border-transparent bg-transparent hover:bg-white/5 shadow-none',
  link: 'text-blue-400 underline-offset-4 hover:underline'
}

const sizes = {
  default: 'h-10 px-4 py-2',
  sm: 'h-9 rounded-md px-3',
  xs: 'h-8 rounded-md px-2',
  lg: 'h-11 rounded-md px-8',
  icon: 'h-10 w-10',
  'icon-sm': 'h-8 w-8'
}
</script> 