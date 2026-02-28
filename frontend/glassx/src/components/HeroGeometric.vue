<template>
  <section class="hero-wrap">
    <div class="hero-card glass-panel-strong">
      <a href="https://github.com/KiteMC/VerifyMC" target="_blank" rel="noopener noreferrer" class="hero-badge">
        GitHub
      </a>

      <h1 class="hero-title">
        <span class="hero-title-muted">{{ displayTitle1 }}</span>
        <span class="hero-title-brand">{{ displayTitle2 }}</span>
      </h1>

      <p class="hero-description">{{ announcement || $t('home.description') }}</p>

      <div class="hero-actions">
        <router-link v-if="!isAdminLoggedIn" to="/register" class="btn-primary hero-btn">
          {{ $t('home.cta.register') }}
        </router-link>
        <router-link :to="secondaryAction.href" class="btn-secondary hero-btn">
          {{ secondaryAction.label }}
        </router-link>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, inject, ref, type Ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { sessionService } from '@/services/session'

interface AppConfig {
  webServerPrefix?: string
  announcement?: string
}

interface Props {
  title1?: string
  title2?: string
}

const props = withDefaults(defineProps<Props>(), {
  title1: 'Welcome to join',
  title2: 'VerifyMC',
})

const config = inject<Ref<AppConfig>>('config', ref({}))
const { t } = useI18n()

const displayTitle1 = computed(() => props.title1 || t('home.welcome'))
const displayTitle2 = computed(() => config.value?.webServerPrefix || props.title2)
const announcement = computed(() => config.value?.announcement || '')
const isAdminLoggedIn = computed(() => sessionService.isAuthenticated())

const secondaryAction = computed(() => {
  if (isAdminLoggedIn.value) {
    return { href: '/admin', label: t('nav.admin') }
  }
  return { href: '/login', label: t('nav.login') }
})
</script>

<style scoped>
.hero-wrap {
  width: 100%;
  padding: 1rem;
}

.hero-card {
  max-width: 880px;
  margin: 0 auto;
  padding: clamp(1.4rem, 4vw, 3rem);
  text-align: center;
}

.hero-badge {
  display: inline-flex;
  border: 1px solid rgba(148, 163, 184, 0.35);
  border-radius: 999px;
  padding: 0.35rem 0.8rem;
  font-size: 0.8rem;
  color: var(--color-text-muted);
}

.hero-title {
  margin: 1rem 0;
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  line-height: 1.05;
}

.hero-title-muted {
  font-size: clamp(1.8rem, 6vw, 3.2rem);
  color: #dbeafe;
}

.hero-title-brand {
  font-size: clamp(2.2rem, 8vw, 4.2rem);
  background: linear-gradient(135deg, #93c5fd, #22d3ee 45%, #bfdbfe);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.hero-description {
  max-width: 44rem;
  margin: 0 auto;
  color: var(--color-text-muted);
  line-height: 1.6;
}

.hero-actions {
  margin-top: 1.5rem;
  display: flex;
  justify-content: center;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.hero-btn {
  min-width: 9.5rem;
  min-height: 2.5rem;
  padding: 0.55rem 1rem;
}

@media (max-width: 640px) {
  .hero-card {
    text-align: left;
  }

  .hero-actions {
    justify-content: stretch;
  }

  .hero-btn {
    width: 100%;
  }
}
</style>
