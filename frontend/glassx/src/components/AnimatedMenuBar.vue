<template>
  <div class="menu-bar">
    <RouterLink
      v-for="item in visibleItems"
      :key="item.to"
      :to="item.to"
      class="menu-item"
      :class="{ 'menu-item-active': isActive(item.to) }"
    >
      <component :is="item.icon" :size="16" />
      <span>{{ item.label }}</span>
    </RouterLink>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, RouterLink } from 'vue-router'
import { Home, LogIn, Settings, UserPlus } from 'lucide-vue-next'
import { sessionService } from '@/services/session'

const { t } = useI18n()
const route = useRoute()

const isLoggedIn = computed(() => sessionService.isAuthenticated())

const visibleItems = computed(() => {
  const base = [{ to: '/', label: t('nav.home'), icon: Home }]
  if (isLoggedIn.value) {
    base.push({ to: '/admin', label: t('nav.admin'), icon: Settings })
  } else {
    base.push({ to: '/register', label: t('nav.register'), icon: UserPlus })
    base.push({ to: '/login', label: t('nav.login'), icon: LogIn })
  }
  return base
})

const isActive = (to: string) => (to === '/' ? route.path === '/' : route.path.startsWith(to))
</script>

<style scoped>
.menu-bar {
  display: flex;
  align-items: center;
  gap: 0.35rem;
}

.menu-item {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.5rem 0.7rem;
  border-radius: 0.65rem;
  color: var(--color-text-muted);
}

.menu-item-active,
.menu-item:hover {
  color: var(--color-text);
  background: rgba(59, 130, 246, 0.2);
}
</style>
