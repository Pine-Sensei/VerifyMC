<template>
  <div class="home-page">
    <!-- Hero Section -->
    <HeroGeometric 
      v-if="serverName !== undefined"
      :title1="$t('home.welcome')"
      :title2="serverName"
    />
  </div>
</template>

<script setup lang="ts">
import { inject, computed, ref, type Ref } from 'vue'
import HeroGeometric from '@/components/HeroGeometric.vue'

interface AppConfig {
  webServerPrefix?: string
}

const config = inject<Ref<AppConfig>>('config', ref({}))

const serverName = computed(() => config.value?.webServerPrefix || 'VerifyMC')
</script>

<style scoped>
.home-page {
  position: relative;
  min-height: 100vh;
  width: 100%;
  overflow: hidden;
  /* 移除任何可能覆盖背景的样式 */
  background: transparent;
}

/* 确保在移动端也能正确显示 */
@supports (-webkit-touch-callout: none) {
  .home-page {
    min-height: -webkit-fill-available;
  }
}
</style>
