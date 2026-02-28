<template>
  <div class="min-h-screen flex justify-center items-center p-8 relative">
    <div class="relative z-10 bg-white/10 backdrop-blur-xl rounded-2xl p-12 text-center shadow-2xl border border-white/10 max-w-lg">
      <h1 class="text-8xl sm:text-9xl font-bold text-white m-0">404</h1>
      <h2 class="text-3xl sm:text-4xl font-semibold text-white my-4">{{ $t('errors.404.title') }}</h2>
      <p class="text-white/80 text-lg my-6">{{ $t('errors.404.message') }}</p>
      <router-link to="/" class="inline-block bg-white/20 text-white px-6 py-3 rounded-xl font-medium border border-white/10 hover:bg-white/30 hover:-translate-y-0.5 transition-all duration-300">
        {{ $t('common.back_home') }}
      </router-link>
    </div>
  </div>
</template>

<script setup lang="ts">
// 404 Not Found page
</script>

