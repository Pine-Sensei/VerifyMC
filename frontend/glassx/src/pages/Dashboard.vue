<template>
  <div class="min-h-screen w-full relative">
    <!-- Mobile Header -->
    <header class="lg:hidden fixed top-0 w-full z-40 bg-white/5 backdrop-blur-xl border-b border-white/10 flex items-center justify-between px-4 h-16">
      <Button @click="toggleMobileMenu" variant="outline" size="icon" class="lg:hidden border-white/10 bg-white/5 text-white hover:bg-white/10">
        <Menu class="w-6 h-6" />
      </Button>
      <h1 class="text-lg font-bold text-white">{{ serverName }}</h1>
      <LanguageSwitcher />
    </header>

    <!-- Mobile Overlay -->
    <div v-if="mobileMenuOpen" class="fixed inset-0 bg-black/50 z-40 lg:hidden backdrop-blur-sm" @click="closeMobileMenu"></div>

    <!-- Sidebar -->
    <aside 
      class="fixed left-0 top-0 h-full bg-white/5 backdrop-blur-xl border-r border-white/10 z-50 transition-all duration-300 flex flex-col"
      :class="[
        sidebarCollapsed ? 'w-20' : 'w-64',
        mobileMenuOpen ? 'translate-x-0' : '-translate-x-full lg:translate-x-0'
      ]"
    >
      <!-- Sidebar Header -->
      <div class="h-16 flex items-center justify-between px-4 border-b border-white/10 shrink-0">
        <div class="flex items-center gap-3 overflow-hidden">
          <div class="w-10 h-10 rounded-xl bg-gradient-to-br from-blue-500 to-purple-600 flex items-center justify-center text-white shrink-0">
            <Server class="w-6 h-6" />
          </div>
          <span v-if="!sidebarCollapsed" class="text-lg font-bold text-transparent bg-clip-text bg-gradient-to-br from-blue-400 to-purple-500 whitespace-nowrap">{{ serverName }}</span>
        </div>
        <Button v-if="!mobileMenuOpen" @click="toggleSidebar" variant="outline" size="icon" class="hidden lg:flex w-7 h-7 border-white/10 bg-white/5 text-white/70 hover:text-white hover:bg-white/10">
          <ChevronLeft v-if="!sidebarCollapsed" class="w-4 h-4" />
          <ChevronRight v-else class="w-4 h-4" />
        </Button>
      </div>

      <!-- Navigation -->
      <nav class="flex-1 overflow-y-auto py-4 px-3 space-y-6">
        <!-- Player Section -->
        <div class="space-y-1">
          <div v-if="!sidebarCollapsed" class="px-3 text-xs font-semibold uppercase tracking-wider text-white/40 mb-2">{{ $t('dashboard.sections.player') }}</div>
          <button
            v-for="item in playerMenuItems"
            :key="item.id"
            @click="setActiveSection(item.id)"
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
            @click="setActiveSection(item.id)"
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
        <Button @click="handleLogout" variant="outline" class="w-full gap-2 bg-red-500/10 hover:bg-red-500/20 border-red-500/20 text-red-400 hover:text-red-300 hover:border-red-500/30">
          <LogOut class="w-4 h-4" />
          <span v-if="!sidebarCollapsed">{{ $t('nav.logout') }}</span>
        </Button>
      </div>
    </aside>

    <!-- Main Content -->
    <main 
      class="min-h-screen transition-all duration-300 flex flex-col"
      :class="[
        sidebarCollapsed ? 'lg:pl-20' : 'lg:pl-64'
      ]"
    >
      <div class="pt-20 lg:pt-8 px-4 lg:px-8 pb-8 flex-1">
        <div class="mb-6 flex items-center justify-between">
          <h1 class="text-2xl font-bold text-white">{{ currentSectionTitle }}</h1>
          <div class="hidden">
            <LanguageSwitcher />
          </div>
        </div>

        <div class="relative">
          <!-- Player Sections -->
          <ProfileSection v-if="activeSection === 'profile'" />
          <DownloadCenter v-if="activeSection === 'downloads'" />

          <!-- Admin Sections -->
          <ServerStatus v-if="activeSection === 'server-status' && isAdmin" />
          <UserManagement v-if="activeSection === 'user-management' && isAdmin" />
          <AuditLog v-if="activeSection === 'audit-log' && isAdmin" />
        </div>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, inject, markRaw, defineAsyncComponent, type Ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import {
  Server,
  User,
  LogOut,
  ChevronLeft,
  ChevronRight,
  Download,
  Activity,
  Users,
  FileText,
  Menu,
} from 'lucide-vue-next'
import { sessionService, type UserInfo } from '@/services/session'
import LanguageSwitcher from '@/components/LanguageSwitcher.vue'
import Button from '@/components/ui/Button.vue'

// Section components - lazy loaded for better performance
const ProfileSection = defineAsyncComponent(() => import('@/components/dashboard/ProfileSection.vue'))
const DownloadCenter = defineAsyncComponent(() => import('@/components/dashboard/DownloadCenter.vue'))
const ServerStatus = defineAsyncComponent(() => import('@/components/dashboard/ServerStatus.vue'))
const UserManagement = defineAsyncComponent(() => import('@/components/dashboard/UserManagement.vue'))
const AuditLog = defineAsyncComponent(() => import('@/components/dashboard/AuditLog.vue'))

interface AppConfig {
  webServerPrefix?: string
}

const { t } = useI18n()
const router = useRouter()
const config = inject<Ref<AppConfig>>('config', ref({}))

const sidebarCollapsed = ref(false)
const mobileMenuOpen = ref(false)
const activeSection = ref('profile')
const userInfo = ref<UserInfo | null>(null)

const serverName = computed(() => config.value?.webServerPrefix || 'VerifyMC')
const isAdmin = computed(() => sessionService.isAdmin())

const playerMenuItems = computed(() => [
  {
    id: 'profile',
    label: t('dashboard.menu.profile'),
    icon: markRaw(User),
  },
  {
    id: 'downloads',
    label: t('dashboard.menu.downloads'),
    icon: markRaw(Download),
  },
])

const adminMenuItems = computed(() => [
  {
    id: 'server-status',
    label: t('dashboard.menu.server_status'),
    icon: markRaw(Activity),
  },
  {
    id: 'user-management',
    label: t('dashboard.menu.user_management'),
    icon: markRaw(Users),
  },
  {
    id: 'audit-log',
    label: t('dashboard.menu.audit_log'),
    icon: markRaw(FileText),
  },
])

const currentSectionTitle = computed(() => {
  const allItems = [...playerMenuItems.value, ...adminMenuItems.value]
  const item = allItems.find(i => i.id === activeSection.value)
  return item?.label || ''
})

const toggleSidebar = () => {
  sidebarCollapsed.value = !sidebarCollapsed.value
}

const toggleMobileMenu = () => {
  mobileMenuOpen.value = !mobileMenuOpen.value
}

const closeMobileMenu = () => {
  mobileMenuOpen.value = false
}

const setActiveSection = (section: string) => {
  activeSection.value = section
  closeMobileMenu()
}

const handleLogout = () => {
  sessionService.clearToken()
  router.push('/')
}

onMounted(() => {
  // Check authentication
  if (!sessionService.isAuthenticated()) {
    sessionService.redirectToLogin()
    return
  }

  // Load user info
  userInfo.value = sessionService.getUserInfo()

  // Set default section based on user role
  if (isAdmin.value) {
    activeSection.value = 'user-management'
  } else {
    activeSection.value = 'profile'
  }
})
</script>


