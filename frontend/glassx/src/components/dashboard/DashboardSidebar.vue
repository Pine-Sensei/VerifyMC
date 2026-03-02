<template>
  <aside 
    class="fixed left-0 top-0 h-full bg-white/5 backdrop-blur-xl border-r border-white/10 z-50 transition-all duration-300 flex flex-col pt-16"
    :class="[
      sidebarCollapsed ? 'w-20' : 'w-64',
      mobileMenuOpen ? 'translate-x-0' : '-translate-x-full lg:translate-x-0'
    ]"
  >
    <!-- Navigation -->
    <nav class="flex-1 overflow-y-auto py-4 px-3 space-y-6">
      <!-- Player Section -->
      <div class="space-y-1">
        <div v-if="!sidebarCollapsed" class="px-3 text-xs font-semibold uppercase tracking-wider text-white/40 mb-2">{{ $t('dashboard.sections.player') }}</div>
        <button
          v-for="item in playerMenuItems"
          :key="item.id"
          @click="$emit('setActiveSection', item.id)"
          class="w-full flex items-center gap-3 px-3 py-3 rounded-xl transition-all group relative"
          :class="[
            activeSection === item.id 
              ? 'bg-white/10 text-white border-l-2 border-blue-400' 
              : 'text-white/60 hover:bg-white/10 hover:text-white'
          ]"
          :title="sidebarCollapsed ? item.label : ''"
        >
          <component :is="item.icon" class="w-5 h-5 shrink-0" />
          <span v-if="!sidebarCollapsed" class="text-sm font-medium whitespace-nowrap">{{ item.label }}</span>
        </button>
      </div>

      <!-- Admin Section -->
      <div v-if="isAdmin" class="space-y-1">
        <div v-if="!sidebarCollapsed" class="px-3 text-xs font-semibold uppercase tracking-wider text-white/40 mb-2">{{ $t('dashboard.sections.admin') }}</div>
        <button
          v-for="item in adminMenuItems"
          :key="item.id"
          @click="$emit('setActiveSection', item.id)"
          class="w-full flex items-center gap-3 px-3 py-3 rounded-xl transition-all group relative"
          :class="[
            activeSection === item.id 
              ? 'bg-white/10 text-white border-l-2 border-blue-400' 
              : 'text-white/60 hover:bg-white/10 hover:text-white'
          ]"
          :title="sidebarCollapsed ? item.label : ''"
        >
          <component :is="item.icon" class="w-5 h-5 shrink-0" />
          <span v-if="!sidebarCollapsed" class="text-sm font-medium whitespace-nowrap">{{ item.label }}</span>
        </button>
      </div>
    </nav>

    <!-- User Info -->
    <div class="p-4 border-t border-white/10 bg-white/5 shrink-0">
      <div class="flex items-center gap-3 mb-3 overflow-hidden">
        <div class="w-9 h-9 rounded-lg bg-gradient-to-br from-blue-500 to-purple-600 flex items-center justify-center text-white shrink-0">
          <User class="w-5 h-5" />
        </div>
        <div v-if="!sidebarCollapsed" class="flex flex-col overflow-hidden">
          <span class="text-sm font-semibold text-white truncate">{{ userInfo?.username || 'User' }}</span>
          <span class="text-xs text-white/50 truncate">{{ isAdmin ? $t('dashboard.roles.admin') : $t('dashboard.roles.player') }}</span>
        </div>
      </div>
      <Button @click="$emit('logout')" variant="outline" class="w-full gap-2 bg-red-500/10 hover:bg-red-500/20 border-red-500/20 text-red-400 hover:text-red-300 hover:border-red-500/30">
        <LogOut class="w-4 h-4" />
        <span v-if="!sidebarCollapsed">{{ $t('nav.logout') }}</span>
      </Button>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { computed, markRaw } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  User,
  LogOut,
  Download,
  Activity,
  Users,
  FileText,
} from 'lucide-vue-next'
import Button from '@/components/ui/Button.vue'
import type { UserInfo } from '@/types'
import { getPlayerMenuItems, getAdminMenuItems } from '@/config/menu'

const props = defineProps<{
  sidebarCollapsed: boolean
  mobileMenuOpen: boolean
  activeSection: string
  userInfo: UserInfo | null
  isAdmin: boolean
}>()

const emit = defineEmits<{
  (e: 'toggleSidebar'): void
  (e: 'closeMobileMenu'): void
  (e: 'setActiveSection', section: string): void
  (e: 'logout'): void
}>()

const { t } = useI18n()

const playerMenuItems = computed(() => getPlayerMenuItems(t))
const adminMenuItems = computed(() => getAdminMenuItems(t))
</script>
