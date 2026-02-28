<template>
  <section class="register-page">
    <aside class="register-intro glass-panel">
      <h1>{{ $t('register.title') }}</h1>
      <p>{{ $t('register.subtitle') }}</p>
      <ul>
        <li>{{ $t('register.features.secure') }}</li>
        <li>{{ $t('register.features.fast') }}</li>
        <li>{{ $t('register.features.privacy') }}</li>
      </ul>
    </aside>

    <div class="register-form-shell glass-panel-strong">
      <RegistrationForm />
    </div>
  </section>
</template>

<script setup lang="ts">
import RegistrationForm from '@/components/RegistrationForm.vue'
</script>

<style scoped>
.register-page {
  min-height: calc(100vh - 5rem);
  padding: 1rem;
  display: grid;
  grid-template-columns: minmax(250px, 360px) minmax(320px, 1fr);
  gap: 1rem;
  align-items: start;
}

.register-intro {
  padding: 1.4rem;
}

.register-intro h1 {
  margin: 0;
  font-size: 1.5rem;
}

.register-intro p {
  margin: 0.8rem 0 1rem;
  color: var(--color-text-muted);
}

.register-intro ul {
  margin: 0;
  padding-left: 1rem;
  color: var(--color-text-muted);
  line-height: 1.8;
}

.register-form-shell {
  padding: 1rem;
}

@media (max-width: 960px) {
  .register-page {
    grid-template-columns: 1fr;
  }
}
</style>
