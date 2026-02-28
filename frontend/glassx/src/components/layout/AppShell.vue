<template>
  <div class="app-shell">
    <PageBackdrop />
    <MainNav />

    <main class="app-main">
      <slot />
    </main>

    <footer class="app-footer">
      <div class="app-footer-inner">
        VerifyMC by KiteMC Team 2025-{{ currentYear }}
      </div>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import MainNav from '@/components/navigation/MainNav.vue'
import PageBackdrop from '@/components/layout/PageBackdrop.vue'

const currentYear = computed(() => new Date().getFullYear())
</script>
