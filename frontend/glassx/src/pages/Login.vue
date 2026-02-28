<template>
  <section class="login-page">
    <div class="login-shell glass-panel-strong">
      <LoginForm />
    </div>
  </section>
</template>

<script setup lang="ts">
import LoginForm from '@/components/LoginForm.vue'
</script>

<style scoped>
.login-page {
  min-height: calc(100vh - 5rem);
  display: grid;
  place-items: center;
  padding: 1rem;
}

.login-shell {
  width: min(420px, 100%);
  padding: 1.2rem;
}
</style>
