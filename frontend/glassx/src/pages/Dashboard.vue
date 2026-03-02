<template>
  <div class="min-h-screen w-full relative">
    <!-- Mobile Header -->
    <DashboardHeader
      :sidebar-collapsed="sidebarCollapsed"
      :mobile-menu-open="mobileMenuOpen"
      :server-name="serverName"
      :current-section-title="currentSectionTitle"
      @toggle-mobile-menu="toggleMobileMenu"
    />

    <!-- Mobile Overlay -->
    <div v-if="mobileMenuOpen" class="fixed inset-0 bg-black/50 z-40 lg:hidden backdrop-blur-sm" @click="closeMobileMenu"></div>

    <!-- Sidebar -->
    <DashboardSidebar
      :sidebar-collapsed="sidebarCollapsed"
      :mobile-menu-open="mobileMenuOpen"
      :active-section="activeSection"
      :user-info="userInfo"
      :is-admin="isAdmin"
      @toggle-sidebar="toggleSidebar"
      @close-mobile-menu="closeMobileMenu"
      @set-active-section="setActiveSection"
      @logout="handleLogout"
    />

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
import { ref, computed, onMounted, inject, defineAsyncComponent, type Ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { sessionService } from '@/services/session'
import LanguageSwitcher from '@/components/LanguageSwitcher.vue'
import DashboardSidebar from '@/components/dashboard/DashboardSidebar.vue'
import DashboardHeader from '@/components/dashboard/DashboardHeader.vue'
import type { AppConfig, UserInfo } from '@/types'
import { getPlayerMenuItems, getAdminMenuItems } from '@/config/menu'

// Section components - lazy loaded for better performance
const ProfileSection = defineAsyncComponent(() => import('@/components/dashboard/ProfileSection.vue'))
const DownloadCenter = defineAsyncComponent(() => import('@/components/dashboard/DownloadCenter.vue'))
const ServerStatus = defineAsyncComponent(() => import('@/components/dashboard/ServerStatus.vue'))
const UserManagement = defineAsyncComponent(() => import('@/components/dashboard/UserManagement.vue'))
const AuditLog = defineAsyncComponent(() => import('@/components/dashboard/AuditLog.vue'))

const { t } = useI18n()
const router = useRouter()
const config = inject<Ref<AppConfig>>('config', ref({}))

const sidebarCollapsed = ref(false)
const mobileMenuOpen = ref(false)
const activeSection = ref('profile')
const userInfo = ref<UserInfo | null>(null)

const serverName = computed(() => config.value?.webServerPrefix || 'VerifyMC')
const isAdmin = computed(() => sessionService.isAdmin())

const currentSectionTitle = computed(() => {
  const allItems = [...getPlayerMenuItems(t), ...getAdminMenuItems(t)]
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
