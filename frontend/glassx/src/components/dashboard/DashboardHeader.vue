<template>
  <header class="lg:hidden fixed top-0 w-full z-40 bg-white/5 backdrop-blur-xl border-b border-white/10 flex items-center justify-between px-4 h-16">
    <Button @click="$emit('toggleMobileMenu')" variant="outline" size="icon" class="lg:hidden border-white/10 bg-white/5 text-white hover:bg-white/10">
      <Menu class="w-6 h-6" />
    </Button>
    <h1 class="text-lg font-bold text-white">{{ serverName }}</h1>
    <LanguageSwitcher />
  </header>
</template>

<script setup lang="ts">
import { Menu } from 'lucide-vue-next'
import Button from '@/components/ui/Button.vue'
import LanguageSwitcher from '@/components/LanguageSwitcher.vue'

defineProps<{
  sidebarCollapsed: boolean
  mobileMenuOpen: boolean
  serverName: string
  currentSectionTitle: string
}>()

defineEmits<{
  (e: 'toggleMobileMenu'): void
}>()
</script>
