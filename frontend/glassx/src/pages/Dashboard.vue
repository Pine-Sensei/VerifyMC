<template>
  <div class="dashboard-container">
    <header class="mobile-header glass-panel" v-if="mobileMenuOpen || isMobile">
      <button @click="toggleMobileMenu" class="icon-btn" type="button" aria-label="Toggle menu">
        <Menu class="w-5 h-5" />
      </button>
      <h1 class="mobile-title">{{ serverName }}</h1>
      <LanguageSwitcher />
    </header>

    <div v-if="mobileMenuOpen" class="mobile-overlay" @click="closeMobileMenu"></div>

    <aside class="sidebar glass-panel-strong" :class="{ 'sidebar-collapsed': sidebarCollapsed, 'sidebar-mobile-open': mobileMenuOpen }">
      <div class="sidebar-header">
        <div class="sidebar-logo">
          <div class="logo-icon">
            <Server class="w-5 h-5" />
          </div>
          <span v-if="!sidebarCollapsed" class="logo-text">{{ serverName }}</span>
        </div>
        <button @click="toggleSidebar" class="icon-btn" type="button" aria-label="Toggle sidebar">
          <ChevronLeft v-if="!sidebarCollapsed" class="w-4 h-4" />
          <ChevronRight v-else class="w-4 h-4" />
        </button>
      </div>

      <nav class="sidebar-nav">
        <section class="nav-section">
          <p v-if="!sidebarCollapsed" class="nav-section-title">{{ $t('dashboard.sections.player') }}</p>
          <button
            v-for="item in playerMenuItems"
            :key="item.id"
            @click="setActiveSection(item.id)"
            :class="['nav-item', { 'nav-item-active': activeSection === item.id }]"
            :title="sidebarCollapsed ? item.label : ''"
            type="button"
          >
            <component :is="item.icon" class="nav-icon" />
            <span v-if="!sidebarCollapsed">{{ item.label }}</span>
          </button>
        </section>

        <section v-if="isAdmin" class="nav-section">
          <p v-if="!sidebarCollapsed" class="nav-section-title">{{ $t('dashboard.sections.admin') }}</p>
          <button
            v-for="item in adminMenuItems"
            :key="item.id"
            @click="setActiveSection(item.id)"
            :class="['nav-item', { 'nav-item-active': activeSection === item.id }]"
            :title="sidebarCollapsed ? item.label : ''"
            type="button"
          >
            <component :is="item.icon" class="nav-icon" />
            <span v-if="!sidebarCollapsed">{{ item.label }}</span>
          </button>
        </section>
      </nav>

      <div class="sidebar-footer">
        <div class="user-info">
          <div class="user-avatar"><User class="w-4 h-4" /></div>
          <div v-if="!sidebarCollapsed">
            <p class="user-name">{{ userInfo?.username || 'User' }}</p>
            <p class="user-role">{{ isAdmin ? $t('dashboard.roles.admin') : $t('dashboard.roles.player') }}</p>
          </div>
        </div>
        <button class="btn-danger logout-btn" @click="handleLogout" type="button">
          <LogOut class="w-4 h-4" />
          <span v-if="!sidebarCollapsed">{{ $t('nav.logout') }}</span>
        </button>
      </div>
    </aside>

    <main class="main-content" :class="{ 'main-content-collapsed': sidebarCollapsed }">
      <div class="content-header glass-panel">
        <h2>{{ currentSectionTitle }}</h2>
        <LanguageSwitcher class="desktop-language" />
      </div>

      <div class="content-body">
        <ProfileSection v-if="activeSection === 'profile'" />
        <DownloadCenter v-if="activeSection === 'downloads'" />
        <ServerStatus v-if="activeSection === 'server-status' && isAdmin" />
        <UserManagement v-if="activeSection === 'user-management' && isAdmin" />
        <AuditLog v-if="activeSection === 'audit-log' && isAdmin" />
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, defineAsyncComponent, inject, markRaw, onMounted, onUnmounted, ref, type Ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import {
  Activity,
  ChevronLeft,
  ChevronRight,
  Download,
  FileText,
  LogOut,
  Menu,
  Server,
  User,
  Users,
} from 'lucide-vue-next'
import LanguageSwitcher from '@/components/LanguageSwitcher.vue'
import { sessionService, type UserInfo } from '@/services/session'
import { useDashboardLayout } from '@/composables/useDashboardLayout'

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
const userInfo = ref<UserInfo | null>(null)
const isMobile = ref(false)
let removeResizeListener: (() => void) | null = null

const serverName = computed(() => config.value?.webServerPrefix || 'VerifyMC')
const isAdmin = computed(() => sessionService.isAdmin())

const playerMenuItems = computed(() => [
  { id: 'profile', label: t('dashboard.menu.profile'), icon: markRaw(User) },
  { id: 'downloads', label: t('dashboard.menu.downloads'), icon: markRaw(Download) },
])

const adminMenuItems = computed(() => [
  { id: 'server-status', label: t('dashboard.menu.server_status'), icon: markRaw(Activity) },
  { id: 'user-management', label: t('dashboard.menu.user_management'), icon: markRaw(Users) },
  { id: 'audit-log', label: t('dashboard.menu.audit_log'), icon: markRaw(FileText) },
])

const {
  sidebarCollapsed,
  mobileMenuOpen,
  activeSection,
  currentSectionTitle,
  toggleSidebar,
  toggleMobileMenu,
  closeMobileMenu,
  setActiveSection,
} = useDashboardLayout({
  defaultSection: 'profile',
  playerMenuItems,
  adminMenuItems,
})

const handleLogout = () => {
  sessionService.clearToken()
  router.push('/')
}

onMounted(() => {
  if (!sessionService.isAuthenticated()) {
    sessionService.redirectToLogin()
    return
  }

  userInfo.value = sessionService.getUserInfo()
  activeSection.value = isAdmin.value ? 'user-management' : 'profile'

  if (typeof window !== 'undefined') {
    const checkMobile = () => {
      isMobile.value = window.innerWidth <= 900
      if (!isMobile.value) {
        mobileMenuOpen.value = false
      }
    }
    checkMobile()
    window.addEventListener('resize', checkMobile)
    removeResizeListener = () => window.removeEventListener('resize', checkMobile)
  }
})

onUnmounted(() => {
  if (removeResizeListener) {
    removeResizeListener()
  }
})
</script>

<style scoped>
.dashboard-container {
  display: flex;
  min-height: calc(100vh - 5rem);
  padding: 0 1rem 1rem;
}

.mobile-header {
  display: none;
}

.sidebar {
  width: 250px;
  min-height: calc(100vh - 6rem);
  position: fixed;
  left: 1rem;
  top: 4.8rem;
  display: flex;
  flex-direction: column;
  transition: width var(--motion-base) var(--ease-standard), transform var(--motion-base) var(--ease-standard);
}

.sidebar-collapsed {
  width: 74px;
}

.sidebar-header,
.sidebar-footer {
  padding: 0.9rem;
  border-bottom: 1px solid rgba(148, 163, 184, 0.2);
}

.sidebar-footer {
  margin-top: auto;
  border-top: 1px solid rgba(148, 163, 184, 0.2);
  border-bottom: none;
}

.sidebar-logo {
  display: flex;
  align-items: center;
  gap: 0.6rem;
}

.logo-icon {
  width: 2rem;
  height: 2rem;
  border-radius: 0.65rem;
  background: linear-gradient(135deg, #60a5fa, #2563eb);
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.logo-text {
  font-weight: 700;
}

.icon-btn {
  border: 1px solid rgba(148, 163, 184, 0.28);
  color: var(--color-text);
  border-radius: 0.6rem;
  background: rgba(15, 23, 42, 0.4);
  width: 2rem;
  height: 2rem;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.sidebar-nav {
  padding: 0.8rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
  overflow: auto;
}

.nav-section {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.nav-section-title {
  margin: 0;
  font-size: 0.72rem;
  color: var(--color-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.08em;
  padding: 0 0.6rem;
}

.nav-item {
  border: none;
  background: transparent;
  color: var(--color-text-muted);
  border-radius: 0.68rem;
  padding: 0.55rem 0.65rem;
  display: flex;
  align-items: center;
  gap: 0.55rem;
  text-align: left;
}

.nav-item-active,
.nav-item:hover {
  color: var(--color-text);
  background: rgba(59, 130, 246, 0.2);
}

.nav-icon {
  width: 1rem;
  height: 1rem;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  margin-bottom: 0.6rem;
}

.user-avatar {
  width: 1.8rem;
  height: 1.8rem;
  border-radius: 0.6rem;
  background: rgba(96, 165, 250, 0.2);
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.user-name {
  margin: 0;
  font-size: 0.84rem;
  font-weight: 600;
}

.user-role {
  margin: 0;
  font-size: 0.73rem;
  color: var(--color-text-muted);
}

.logout-btn {
  width: 100%;
  min-height: 2rem;
}

.main-content {
  flex: 1;
  margin-left: 266px;
  transition: margin-left var(--motion-base) var(--ease-standard);
}

.main-content-collapsed {
  margin-left: 90px;
}

.content-header {
  margin-bottom: 1rem;
  padding: 0.9rem 1rem;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-radius: var(--radius-lg);
}

.content-header h2 {
  margin: 0;
  font-size: 1.15rem;
}

.content-body {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.mobile-overlay {
  position: fixed;
  inset: 0;
  background: rgba(2, 6, 23, 0.58);
  z-index: calc(var(--z-overlay) - 1);
}

@media (max-width: 900px) {
  .dashboard-container {
    padding: 0 0.5rem 0.8rem;
  }

  .mobile-header {
    display: flex;
    position: fixed;
    top: 4.6rem;
    left: 0.5rem;
    right: 0.5rem;
    z-index: var(--z-overlay);
    height: 3.1rem;
    align-items: center;
    justify-content: space-between;
    padding: 0.5rem;
    border-radius: var(--radius-md);
  }

  .mobile-title {
    margin: 0;
    font-size: 0.95rem;
  }

  .sidebar {
    z-index: var(--z-overlay);
    left: 0.5rem;
    transform: translateX(-120%);
    top: 8.2rem;
    min-height: calc(100vh - 9rem);
  }

  .sidebar.sidebar-mobile-open {
    transform: translateX(0);
  }

  .main-content,
  .main-content-collapsed {
    margin-left: 0;
    margin-top: 3.6rem;
  }

  .desktop-language {
    display: none;
  }
}
</style>
