import { computed, shallowRef, type ComputedRef } from 'vue'

interface MenuItem {
  id: string
  label: string
  icon: unknown
}

interface UseDashboardLayoutOptions {
  defaultSection: string
  playerMenuItems: ComputedRef<MenuItem[]>
  adminMenuItems: ComputedRef<MenuItem[]>
}

export function useDashboardLayout(options: UseDashboardLayoutOptions) {
  const sidebarCollapsed = shallowRef(false)
  const mobileMenuOpen = shallowRef(false)
  const activeSection = shallowRef(options.defaultSection)

  const currentSectionTitle = computed(() => {
    const allItems = [...options.playerMenuItems.value, ...options.adminMenuItems.value]
    return allItems.find((item) => item.id === activeSection.value)?.label ?? ''
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

  return {
    sidebarCollapsed,
    mobileMenuOpen,
    activeSection,
    currentSectionTitle,
    toggleSidebar,
    toggleMobileMenu,
    closeMobileMenu,
    setActiveSection,
  }
}
