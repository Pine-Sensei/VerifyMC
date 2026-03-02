<template>
  <nav 
    class="relative p-1.5 md:p-2 rounded-2xl bg-white/5 backdrop-blur-xl border border-white/10 shadow-2xl transition-all duration-300 hover:bg-white/10 hover:border-white/20 hover:shadow-white/5"
    @mouseenter="isNavHovered = true"
    @mouseleave="isNavHovered = false"
  >
    <!-- Subtle Nav Glow -->
    <div 
      class="absolute inset-0 rounded-2xl bg-gradient-to-r from-transparent via-white/5 to-transparent opacity-0 transition-opacity duration-700 pointer-events-none"
      :class="{ 'opacity-100': isNavHovered }"
    />

    <ul 
      class="flex m-0 p-0 list-none relative z-10"
      :class="[
        vertical ? 'flex-col items-stretch space-y-1' : 'flex-row items-center gap-1 md:gap-2'
      ]"
    >
      <li 
        v-for="(item, index) in menuItems" 
        :key="item.label" 
        class="relative group"
        @mouseenter="hoveredIndex = index"
        @mouseleave="hoveredIndex = null"
      >
        <router-link
          :to="item.href"
          class="relative flex items-center gap-2 px-3 py-2 md:px-4 md:py-2.5 rounded-xl transition-all duration-300 overflow-hidden"
          :class="[
            isActive(item.href)
              ? 'bg-white/10 text-white shadow-inner shadow-white/5'
              : 'text-white/70 hover:text-white hover:bg-white/5',
            vertical ? 'w-full' : ''
          ]"
        >
          <!-- Hover Glow Effect -->
          <div 
            class="absolute inset-0 opacity-0 group-hover:opacity-100 transition-opacity duration-500 ease-out pointer-events-none blur-xl"
            :style="{ background: item.gradient }"
          />

          <!-- Icon with Scale & Color -->
          <span 
            class="relative z-10 flex items-center justify-center transition-all duration-300 group-hover:scale-110 group-active:scale-95"
            :class="[
              isActive(item.href) ? item.iconColor : item.iconHoverColor
            ]"
          >
            <component :is="item.icon" class="w-5 h-5" />
          </span>

          <!-- Label -->
          <span class="relative z-10 text-sm font-medium tracking-wide">{{ item.label }}</span>
        </router-link>
      </li>
    </ul>
  </nav>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import { Home, UserPlus, LogIn, Settings } from 'lucide-vue-next'
import { sessionService } from '@/services/session'

withDefaults(defineProps<{
  vertical?: boolean
}>(), {
  vertical: false
})

const { t } = useI18n()
const route = useRoute()

const hoveredIndex = ref<number | null>(null)
const isNavHovered = ref(false)

const isAdminLoggedIn = computed(() => {
  // Bind route to ensure reactivity
  route.fullPath
  return sessionService.isAuthenticated()
})

const menuItems = computed(() => {
  const items = [
    {
      icon: Home,
      label: t('nav.home'),
      href: '/',
      gradient: 'radial-gradient(circle, rgba(59,130,246,0.2) 0%, rgba(37,99,235,0.05) 70%, transparent 100%)',
      iconColor: 'text-blue-400',
      iconHoverColor: 'group-hover:text-blue-400',
    },
  ]

  if (isAdminLoggedIn.value) {
    items.push({
      icon: Settings,
      label: t('nav.admin'),
      href: '/admin',
      gradient: 'radial-gradient(circle, rgba(239,68,68,0.2) 0%, rgba(220,38,38,0.05) 70%, transparent 100%)',
      iconColor: 'text-red-400',
      iconHoverColor: 'group-hover:text-red-400',
    })
  } else {
    items.push({
      icon: UserPlus,
      label: t('nav.register'),
      href: '/register',
      gradient: 'radial-gradient(circle, rgba(249,115,22,0.2) 0%, rgba(234,88,12,0.05) 70%, transparent 100%)',
      iconColor: 'text-orange-400',
      iconHoverColor: 'group-hover:text-orange-400',
    })

    items.push({
      icon: LogIn,
      label: t('nav.login'),
      href: '/login',
      gradient: 'radial-gradient(circle, rgba(34,197,94,0.2) 0%, rgba(22,163,74,0.05) 70%, transparent 100%)',
      iconColor: 'text-green-400',
      iconHoverColor: 'group-hover:text-green-400',
    })
  }

  return items
})

const isActive = (href: string) => {
  if (href === '/') {
    return route.path === '/'
  }
  return route.path.startsWith(href)
}
</script>

<style scoped>
/* No extra CSS needed, using Tailwind utility classes */
</style>
