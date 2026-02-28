<template>
  <button
    :class="[
      'inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-[10px] text-sm font-semibold transition-all duration-200 disabled:pointer-events-none disabled:opacity-50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-300/50',
      buttonVariants[variant],
      buttonSizes[size],
      className,
    ]"
    v-bind="$attrs"
  >
    <slot />
  </button>
</template>

<script setup lang="ts">
interface Props {
  variant?:
    | 'primary'
    | 'secondary'
    | 'ghost'
    | 'danger'
    | 'link'
    | 'default'
    | 'outline'
    | 'destructive'
  size?: 'sm' | 'default' | 'lg' | 'icon'
  className?: string
}

withDefaults(defineProps<Props>(), {
  variant: 'primary',
  size: 'default',
})

const buttonVariants = {
  primary: 'btn-primary text-white',
  default: 'btn-primary text-white',
  secondary: 'btn-secondary',
  outline: 'btn-secondary',
  ghost: 'btn-ghost',
  danger: 'btn-danger',
  destructive: 'btn-danger',
  link: 'text-blue-300 underline-offset-4 hover:underline px-0 h-auto',
}

const buttonSizes = {
  sm: 'h-9 px-3',
  default: 'h-10 px-4',
  lg: 'h-11 px-6 text-base',
  icon: 'h-10 w-10',
}
</script>
