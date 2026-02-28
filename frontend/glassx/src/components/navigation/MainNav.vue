<template>
  <header class="main-nav-wrap">
    <nav class="main-nav glass-panel" aria-label="Main navigation">
      <RouterLink to="/" class="brand">{{ serverName }}</RouterLink>

      <button class="mobile-toggle" type="button" @click="mobileOpen = !mobileOpen" aria-label="Toggle menu">
        <Menu v-if="!mobileOpen" :size="18" />
        <X v-else :size="18" />
      </button>

      <div class="desktop-links">
        <RouterLink v-for="item in visibleItems" :key="item.to" :to="item.to" class="nav-link" :class="{ 'nav-link-active': isActive(item.to) }">
          <component :is="item.icon" :size="16" />
          <span>{{ item.label }}</span>
        </RouterLink>
      </div>

      <LanguageSwitcher class="language-switcher" />
    </nav>

    <transition name="menu-slide">
      <div v-if="mobileOpen" class="mobile-menu glass-panel">
        <RouterLink
          v-for="item in visibleItems"
          :key="`mobile-${item.to}`"
          :to="item.to"
          class="mobile-link"
          :class="{ 'mobile-link-active': isActive(item.to) }"
          @click="mobileOpen = false"
        >
          <component :is="item.icon" :size="16" />
          <span>{{ item.label }}</span>
        </RouterLink>
        <div class="mobile-language-wrap">
          <LanguageSwitcher />
        </div>
      </div>
    </transition>
  </header>
</template>

<script setup lang="ts">
import { computed, inject, ref, type Ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, RouterLink } from 'vue-router'
import { Home, LogIn, Menu, Settings, UserPlus, X } from 'lucide-vue-next'
import { sessionService } from '@/services/session'
import LanguageSwitcher from '@/components/LanguageSwitcher.vue'

interface AppConfig {
  webServerPrefix?: string
}

const config = inject<Ref<AppConfig>>('config', ref({}))
const { t } = useI18n()
const route = useRoute()
const mobileOpen = ref(false)

const serverName = computed(() => config.value?.webServerPrefix || 'VerifyMC')
const isLoggedIn = computed(() => {
  // Keep auth-dependent menu items in sync with in-app route transitions.
  route.fullPath
  return sessionService.isAuthenticated()
})

const visibleItems = computed(() => {
  const items = [{ to: '/', label: t('nav.home'), icon: Home }]

  if (isLoggedIn.value) {
    items.push({ to: '/admin', label: t('nav.admin'), icon: Settings })
  } else {
    items.push({ to: '/register', label: t('nav.register'), icon: UserPlus })
    items.push({ to: '/login', label: t('nav.login'), icon: LogIn })
  }

  return items
})

const isActive = (to: string) => {
  if (to === '/') {
    return route.path === '/'
  }
  return route.path.startsWith(to)
}
</script>

<style scoped>
.main-nav-wrap {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: var(--z-nav);
  padding: 0.65rem 1rem;
}

.main-nav {
  max-width: 1200px;
  margin: 0 auto;
  height: 3.25rem;
  padding: 0.5rem;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.brand {
  font-weight: 700;
  font-size: 1rem;
  padding: 0.35rem 0.65rem;
  border-radius: 0.6rem;
}

.desktop-links {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  margin-left: auto;
}

.nav-link,
.mobile-link {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  border-radius: 0.65rem;
  padding: 0.5rem 0.7rem;
  color: var(--color-text-muted);
  transition: background-color var(--motion-fast) var(--ease-standard), color var(--motion-fast) var(--ease-standard);
}

.nav-link:hover,
.mobile-link:hover,
.nav-link-active,
.mobile-link-active {
  background: rgba(37, 99, 235, 0.18);
  color: var(--color-text);
}

.language-switcher {
  margin-left: 0.4rem;
}

.mobile-language-wrap {
  display: none;
  margin-top: 0.2rem;
  padding-top: 0.45rem;
  border-top: 1px solid rgba(148, 163, 184, 0.25);
}

.mobile-toggle {
  display: none;
  margin-left: auto;
  color: var(--color-text);
  border: 1px solid rgba(148, 163, 184, 0.32);
  border-radius: 0.6rem;
  background: rgba(15, 23, 42, 0.5);
  width: 2.2rem;
  height: 2.2rem;
  align-items: center;
  justify-content: center;
}

.mobile-menu {
  display: none;
  max-width: 1200px;
  margin: 0.5rem auto 0;
  padding: 0.5rem;
  gap: 0.3rem;
  flex-direction: column;
}

.menu-slide-enter-active,
.menu-slide-leave-active {
  transition: all var(--motion-base) var(--ease-standard);
}

.menu-slide-enter-from,
.menu-slide-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

@media (max-width: 920px) {
  .desktop-links {
    display: none;
  }

  .main-nav > .language-switcher {
    display: none;
  }

  .mobile-toggle {
    display: inline-flex;
  }

  .mobile-menu {
    display: flex;
  }

  .mobile-language-wrap {
    display: block;
  }

  .brand {
    font-size: 0.95rem;
  }
}
</style>
