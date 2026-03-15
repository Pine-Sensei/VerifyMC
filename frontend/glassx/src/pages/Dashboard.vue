<template>
  <div class="min-h-screen w-full relative">
    <div v-if="isOpen" class="fixed inset-0 bg-black/50 z-40 lg:hidden backdrop-blur-sm" @click="setOpen(false)"></div>

    <DashboardSidebar
      :active-section="activeSection"
      :user-info="userInfo"
      :is-admin="isAdmin"
      :admin-actions="adminActions"
      @set-active-section="setActiveSection"
      @logout="handleLogout"
    />

    <main
      class="min-h-screen transition-all duration-300 flex flex-col"
      :class="[
        isCollapsed ? 'lg:pl-20' : 'lg:pl-64'
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
          <ProfileSection v-if="activeSection === 'profile'" />
          <DownloadCenter v-if="activeSection === 'downloads'" />

          <ServerStatus v-if="activeSection === 'server-status' && canAccessSection('server-status')" />
          <UserManagement v-if="activeSection === 'user-management' && canAccessSection('user-management')" />
          <AuditLog v-if="activeSection === 'audit-log' && canAccessSection('audit-log')" />
        </div>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, defineAsyncComponent } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { sessionService } from '@/services/session'
import LanguageSwitcher from '@/components/LanguageSwitcher.vue'
import DashboardSidebar from '@/components/dashboard/DashboardSidebar.vue'
import type { AdminActionKey, UserInfo } from '@/types'
import { getPlayerMenuItems, getAdminMenuItems } from '@/config/menu'
import { canAccessAdminSection, getAccessibleAdminSections } from '@/lib/adminAccess'
import { useSidebar } from '@/composables/useSidebar'

const ProfileSection = defineAsyncComponent(() => import('@/components/dashboard/ProfileSection.vue'))
const DownloadCenter = defineAsyncComponent(() => import('@/components/dashboard/DownloadCenter.vue'))
const ServerStatus = defineAsyncComponent(() => import('@/components/dashboard/ServerStatus.vue'))
const UserManagement = defineAsyncComponent(() => import('@/components/dashboard/UserManagement.vue'))
const AuditLog = defineAsyncComponent(() => import('@/components/dashboard/AuditLog.vue'))

const { t } = useI18n()
const router = useRouter()
const { isCollapsed, isOpen, setTrigger, setOpen, setCollapse } = useSidebar()

const activeSection = ref('profile')
const userInfo = ref<UserInfo | null>(null)

const adminActions = computed<AdminActionKey[]>(() => sessionService.getAdminActions())
const isAdmin = computed(() => adminActions.value.length > 0)
const accessibleAdminSections = computed(() => getAccessibleAdminSections(adminActions.value))

const currentSectionTitle = computed(() => {
  const allItems = [...getPlayerMenuItems(t), ...getAdminMenuItems(t, adminActions.value)]
  const item = allItems.find(i => i.id === activeSection.value)
  return item?.label || ''
})

const canAccessSection = (section: 'server-status' | 'user-management' | 'audit-log') => {
  return canAccessAdminSection(section, adminActions.value)
}

const setActiveSection = (section: string) => {
  activeSection.value = section
  setOpen(false)
}

const handleLogout = () => {
  sessionService.clearToken()
  router.push('/')
}

onMounted(() => {
  setTrigger(true)
  if (!sessionService.isAuthenticated()) {
    sessionService.redirectToLogin()
    return
  }

  userInfo.value = sessionService.getUserInfo()

  if (accessibleAdminSections.value.length > 0) {
    activeSection.value = accessibleAdminSections.value[0]
  } else {
    activeSection.value = 'profile'
  }
})

onUnmounted(() => {
  setTrigger(false)
  setOpen(false)
  setCollapse(false)
})
</script>
