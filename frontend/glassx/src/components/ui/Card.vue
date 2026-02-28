<template>
  <div :class="['card-base', variantClasses[variant], className]" v-bind="$attrs">
    <slot />
  </div>
</template>

<script setup lang="ts">
interface Props {
  className?: string
  variant?: 'default' | 'elevated' | 'interactive'
}

withDefaults(defineProps<Props>(), {
  variant: 'default',
})

const variantClasses = {
  default: 'glass-panel',
  elevated: 'glass-panel-strong',
  interactive: 'glass-panel hover:border-blue-200/50 hover:shadow-[0_14px_35px_rgba(30,64,175,0.35)] transition-all duration-200',
}
</script>

<style scoped>
.card-base {
  border-radius: var(--radius-lg);
}
</style>
