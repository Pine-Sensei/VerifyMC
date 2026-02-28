<template>
  <section class="home-page">
    <HeroGeometric v-if="serverName" :title1="$t('home.welcome')" :title2="serverName" />
  </section>
</template>

<script setup lang="ts">
import { computed, inject, ref, type Ref } from 'vue'
import HeroGeometric from '@/components/HeroGeometric.vue'

interface AppConfig {
  webServerPrefix?: string
}

const config = inject<Ref<AppConfig>>('config', ref({}))
const serverName = computed(() => config.value?.webServerPrefix || 'VerifyMC')
</script>

<style scoped>
.home-page {
  min-height: calc(100vh - 5rem);
  display: flex;
  align-items: center;
}
</style>
