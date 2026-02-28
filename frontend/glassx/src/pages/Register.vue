<template>
  <div class="relative min-h-screen w-full">
    <div class="relative z-10 min-h-screen flex flex-col lg:flex-row">
      <!-- Left: Branding -->
      <div class="hidden lg:flex lg:w-[420px] xl:w-[480px] flex-shrink-0 flex-col justify-center px-12 xl:px-16">
        <div class="space-y-6">
          <div class="inline-flex items-center justify-center w-14 h-14 rounded-2xl bg-white/[0.06] border border-white/10">
            <svg class="w-7 h-7 text-white/70" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M18 9v3m0 0v3m0-3h3m-3 0h-3m-2-5a4 4 0 11-8 0 4 4 0 018 0zM3 20a6 6 0 0112 0v1H3v-1z"></path>
            </svg>
          </div>
          <h1 class="text-4xl xl:text-5xl font-bold text-white tracking-tight leading-tight">
            {{ $t('register.title') }}
          </h1>
          <p class="text-white/50 text-base leading-relaxed max-w-sm">
            {{ $t('register.subtitle') }}
          </p>
          <div class="pt-4 space-y-3">
            <div class="flex items-center gap-3 text-white/40 text-sm">
              <svg class="w-4 h-4 text-white/30 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
              <span>{{ $t('register.features.secure') }}</span>
            </div>
            <div class="flex items-center gap-3 text-white/40 text-sm">
              <svg class="w-4 h-4 text-white/30 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z"></path></svg>
              <span>{{ $t('register.features.fast') }}</span>
            </div>
            <div class="flex items-center gap-3 text-white/40 text-sm">
              <svg class="w-4 h-4 text-white/30 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"></path></svg>
              <span>{{ $t('register.features.privacy') }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Right: Form -->
      <div class="flex-1 flex items-center justify-center p-4 md:p-6 lg:p-10">
        <RegistrationForm />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import RegistrationForm from '@/components/RegistrationForm.vue'
</script>
