<template>
  <div class="dashboard-container">
    <!-- Mobile Header -->
    <header class="mobile-header">
      <button @click="toggleMobileMenu" class="hamburger-btn">
        <Menu class="w-6 h-6" />
      </button>
      <h1 class="mobile-title">{{ serverName }}</h1>
      <LanguageSwitcher />
    </header>

    <!-- Mobile Overlay -->
    <div v-if="mobileMenuOpen" class="mobile-overlay" @click="closeMobileMenu"></div>

    <!-- Sidebar -->
    <aside class="sidebar" :class="{ 'sidebar-collapsed': sidebarCollapsed, 'sidebar-mobile-open': mobileMenuOpen }">
      <div class="sidebar-header">
        <div class="sidebar-logo">
          <div class="logo-icon">
            <Server class="w-6 h-6" />
          </div>
          <span v-if="!sidebarCollapsed" class="logo-text">{{ serverName }}</span>
        </div>
        <button @click="toggleSidebar" class="sidebar-toggle">
          <ChevronLeft v-if="!sidebarCollapsed" class="w-4 h-4" />
          <ChevronRight v-else class="w-4 h-4" />
        </button>
      </div>

      <nav class="sidebar-nav">
        <!-- Player Section -->
        <div class="nav-section">
          <div v-if="!sidebarCollapsed" class="nav-section-title">{{ $t('dashboard.sections.player') }}</div>
          <div class="nav-items">
            <button
              v-for="item in playerMenuItems"
              :key="item.id"
              @click="setActiveSection(item.id)"
              :class="['nav-item', { 'nav-item-active': activeSection === item.id }]"
              :title="sidebarCollapsed ? item.label : ''"
            >
              <component :is="item.icon" class="nav-icon" />
              <span v-if="!sidebarCollapsed" class="nav-label">{{ item.label }}</span>
            </button>
          </div>
        </div>

        <!-- Admin Section -->
        <div v-if="isAdmin" class="nav-section">
          <div v-if="!sidebarCollapsed" class="nav-section-title">{{ $t('dashboard.sections.admin') }}</div>
          <div class="nav-items">
            <button
              v-for="item in adminMenuItems"
              :key="item.id"
              @click="setActiveSection(item.id)"
              :class="['nav-item', { 'nav-item-active': activeSection === item.id }]"
              :title="sidebarCollapsed ? item.label : ''"
            >
              <component :is="item.icon" class="nav-icon" />
              <span v-if="!sidebarCollapsed" class="nav-label">{{ item.label }}</span>
            </button>
          </div>
        </div>
      </nav>

      <!-- User Info -->
      <div class="sidebar-footer">
        <div class="user-info">
          <div class="user-avatar">
            <User class="w-5 h-5" />
          </div>
          <div v-if="!sidebarCollapsed" class="user-details">
            <span class="user-name">{{ userInfo?.username || 'User' }}</span>
            <span v-if="isAdmin" class="user-role">{{ $t('dashboard.roles.admin') }}</span>
            <span v-else class="user-role">{{ $t('dashboard.roles.player') }}</span>
          </div>
        </div>
        <button @click="handleLogout" class="logout-btn" :title="$t('nav.logout')">
          <LogOut class="w-4 h-4" />
          <span v-if="!sidebarCollapsed">{{ $t('nav.logout') }}</span>
        </button>
      </div>
    </aside>

    <!-- Main Content -->
    <main class="main-content">
      <div class="content-header">
        <h1 class="page-title">{{ currentSectionTitle }}</h1>
        <div class="header-actions">
          <LanguageSwitcher />
        </div>
      </div>

      <div class="content-body">
        <!-- Player Sections -->
        <ProfileSection v-if="activeSection === 'profile'" />
        <DownloadCenter v-if="activeSection === 'downloads'" />

        <!-- Admin Sections -->
        <ServerStatus v-if="activeSection === 'server-status' && isAdmin" />
        <UserManagement v-if="activeSection === 'user-management' && isAdmin" />
        <AuditLog v-if="activeSection === 'audit-log' && isAdmin" />
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, inject, markRaw } from 'vue'
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

// Section components
import ProfileSection from '@/components/dashboard/ProfileSection.vue'
import DownloadCenter from '@/components/dashboard/DownloadCenter.vue'
import ServerStatus from '@/components/dashboard/ServerStatus.vue'
import UserManagement from '@/components/dashboard/UserManagement.vue'
import AuditLog from '@/components/dashboard/AuditLog.vue'

const { t } = useI18n()
const router = useRouter()
const config = inject('config', { value: {} as any })

const sidebarCollapsed = ref(false)
const mobileMenuOpen = ref(false)
const activeSection = ref('profile')
const userInfo = ref<UserInfo | null>(null)

const serverName = computed(() => config.value?.frontend?.web_server_prefix || 'VerifyMC')
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

<style scoped>
.dashboard-container {
  display: flex;
  min-height: 100vh;
  background: #030303;
}

/* Sidebar Styles */
.sidebar {
  width: 260px;
  background: rgba(255, 255, 255, 0.03);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-right: 1px solid rgba(255, 255, 255, 0.08);
  display: flex;
  flex-direction: column;
  transition: width 0.3s ease;
  position: fixed;
  left: 0;
  top: 0;
  bottom: 0;
  z-index: 100;
}

.sidebar-collapsed {
  width: 72px;
}

.sidebar-header {
  padding: 1.25rem;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.sidebar-logo {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.logo-icon {
  width: 40px;
  height: 40px;
  background: linear-gradient(135deg, #3b82f6 0%, #8b5cf6 100%);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  flex-shrink: 0;
}

.logo-text {
  font-size: 1.125rem;
  font-weight: 700;
  background: linear-gradient(135deg, #3b82f6 0%, #8b5cf6 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  white-space: nowrap;
  overflow: hidden;
}

.sidebar-toggle {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
}

.sidebar-toggle:hover {
  background: rgba(255, 255, 255, 0.1);
  color: white;
}

/* Navigation */
.sidebar-nav {
  flex: 1;
  padding: 1rem 0.75rem;
  overflow-y: auto;
}

.nav-section {
  margin-bottom: 1.5rem;
}

.nav-section-title {
  font-size: 0.75rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: rgba(255, 255, 255, 0.4);
  padding: 0 0.75rem;
  margin-bottom: 0.5rem;
}

.nav-items {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem;
  border-radius: 10px;
  background: transparent;
  border: none;
  color: rgba(255, 255, 255, 0.7);
  cursor: pointer;
  transition: all 0.2s;
  width: 100%;
  text-align: left;
}

.nav-item:hover {
  background: rgba(255, 255, 255, 0.05);
  color: white;
}

.nav-item-active {
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.2) 0%, rgba(139, 92, 246, 0.2) 100%);
  color: white;
  border: 1px solid rgba(139, 92, 246, 0.3);
}

.nav-icon {
  width: 20px;
  height: 20px;
  flex-shrink: 0;
}

.nav-label {
  font-size: 0.875rem;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
}

/* Sidebar Footer */
.sidebar-footer {
  padding: 1rem;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}

.user-info {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 0.75rem;
}

.user-avatar {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: linear-gradient(135deg, #3b82f6 0%, #8b5cf6 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  flex-shrink: 0;
}

.user-details {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.user-name {
  font-size: 0.875rem;
  font-weight: 600;
  color: white;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-role {
  font-size: 0.75rem;
  color: rgba(255, 255, 255, 0.5);
}

.logout-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  width: 100%;
  padding: 0.625rem;
  border-radius: 8px;
  background: rgba(239, 68, 68, 0.1);
  border: 1px solid rgba(239, 68, 68, 0.2);
  color: #ef4444;
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.logout-btn:hover {
  background: rgba(239, 68, 68, 0.2);
  border-color: rgba(239, 68, 68, 0.3);
}

/* Main Content */
.main-content {
  flex: 1;
  margin-left: 260px;
  transition: margin-left 0.3s ease;
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

.sidebar-collapsed + .main-content {
  margin-left: 72px;
}

.content-header {
  padding: 1.5rem 2rem;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: rgba(255, 255, 255, 0.02);
  position: sticky;
  top: 0;
  z-index: 50;
}

.page-title {
  font-size: 1.5rem;
  font-weight: 700;
  color: white;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.content-body {
  flex: 1;
  padding: 2rem;
  overflow-y: auto;
}

/* Responsive */
@media (max-width: 768px) {
  .mobile-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 1rem;
    background: rgba(15, 23, 42, 0.95);
    border-bottom: 1px solid rgba(255, 255, 255, 0.1);
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    z-index: 100;
  }

  .hamburger-btn {
    background: rgba(255, 255, 255, 0.1);
    border: 1px solid rgba(255, 255, 255, 0.1);
    border-radius: 8px;
    padding: 0.5rem;
    color: white;
    cursor: pointer;
    transition: all 0.2s;
  }

  .hamburger-btn:hover {
    background: rgba(255, 255, 255, 0.2);
  }

  .mobile-title {
    font-size: 1.25rem;
    font-weight: 600;
    color: white;
  }

  .mobile-overlay {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(0, 0, 0, 0.5);
    z-index: 199;
  }

  .sidebar {
    width: 280px;
    transform: translateX(-100%);
    transition: transform 0.3s ease;
    position: fixed;
    top: 0;
    left: 0;
    height: 100vh;
    z-index: 200;
  }

  .sidebar.sidebar-mobile-open {
    transform: translateX(0);
  }

  .sidebar .sidebar-toggle {
    display: none;
  }

  .main-content {
    margin-left: 0;
    padding-top: 60px;
  }

  .content-header {
    padding: 1rem;
  }

  .content-body {
    padding: 1rem;
  }
}

/* Desktop - hide mobile header */
@media (min-width: 769px) {
  .mobile-header {
    display: none;
  }

  .mobile-overlay {
    display: none;
  }
}
</style>
