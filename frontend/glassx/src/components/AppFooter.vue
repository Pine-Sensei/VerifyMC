<template>
  <footer class="relative w-full mt-8 bg-white/3 backdrop-blur-xl z-10">
    <!-- Gradient top border -->
    <div class="absolute top-0 left-0 right-0 h-px bg-gradient-to-r from-transparent via-blue-500/30 via-purple-500/30 via-pink-500/30 to-transparent"></div>

    <div class="p-6 text-center">
      <div class="flex items-center justify-center gap-3 flex-wrap">
        <span class="text-white/70 text-sm font-medium">VerifyMC by KiteMC Team 2025-{{ currentYear }}</span>
        <span class="text-white/30">•</span>
        <span class="flex items-center gap-1 text-white/50 text-sm">
          <span>Made with</span>
          <span class="text-pink-300 animate-pulse">💖</span>
        </span>
      </div>
    </div>
  </footer>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const currentYear = computed(() => new Date().getFullYear())
</script>
