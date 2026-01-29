<template>
  <nav class="nav-container">
    <!-- Gradient accent line -->
    <div class="nav-gradient-line"></div>
    
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
      <div class="flex justify-between items-center h-16">
        <!-- Logo -->
        <div class="flex items-center">
          <router-link to="/" class="logo-link group">
            <span v-if="serverName !== undefined" class="logo-text">{{ serverName }}</span>
          </router-link>
        </div>

        <!-- Desktop Navigation Links -->
        <div class="hidden md:flex items-center">
          <AnimatedMenuBar />
        </div>

        <!-- Language Switcher -->
        <div class="flex items-center gap-2">
          <LanguageSwitcher class="text-white" />
          
          <!-- Mobile Menu Button -->
          <button
            @click="toggleMobileMenu"
            class="mobile-menu-btn"
            aria-label="Toggle mobile menu"
          >
            <svg
              class="w-6 h-6 transition-transform duration-300"
              :class="{ 'rotate-90': mobileMenuOpen }"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                v-if="!mobileMenuOpen"
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="2"
                d="M4 6h16M4 12h16M4 18h16"
              />
              <path
                v-else
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="2"
                d="M6 18L18 6M6 6l12 12"
              />
            </svg>
          </button>
        </div>
      </div>

      <!-- Mobile Menu with animation -->
      <transition name="slide-fade">
        <div
          v-show="mobileMenuOpen"
          class="mobile-menu"
        >
          <div class="px-3 pt-3 pb-4 space-y-2">
            <AnimatedMenuBar class="flex-col space-y-2" />
          </div>
        </div>
      </transition>
    </div>
  </nav>
</template>

<script setup lang="ts">
import { inject, computed, ref } from 'vue'
import LanguageSwitcher from './LanguageSwitcher.vue'
import AnimatedMenuBar from './AnimatedMenuBar.vue'

const config = inject('config', { value: {} as any })
const mobileMenuOpen = ref(false)

const serverName = computed(() => config.value?.frontend?.web_server_prefix)

const toggleMobileMenu = () => {
  mobileMenuOpen.value = !mobileMenuOpen.value
}
</script>

<style scoped>
/* Navigation container */
.nav-container {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 40;
  background: rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

/* Gradient accent line at top */
.nav-gradient-line {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(90deg, 
    transparent 0%, 
    rgba(59, 130, 246, 0.5) 20%, 
    rgba(139, 92, 246, 0.5) 50%, 
    rgba(236, 72, 153, 0.5) 80%, 
    transparent 100%
  );
  opacity: 0.8;
}

/* Logo styling */
.logo-link {
  display: flex;
  align-items: center;
  text-decoration: none;
  transition: all 0.3s ease;
}

.logo-text {
  font-size: 1.25rem;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, #fff 0%, rgba(255, 255, 255, 0.8) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  transition: all 0.3s ease;
}

.logo-link:hover .logo-text {
  background: linear-gradient(135deg, #93c5fd 0%, #c4b5fd 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

/* Mobile menu button */
.mobile-menu-btn {
  display: none;
  padding: 0.5rem;
  border-radius: 8px;
  color: #fff;
  background: transparent;
  border: 1px solid transparent;
  cursor: pointer;
  transition: all 0.3s ease;
}

.mobile-menu-btn:hover {
  background: rgba(255, 255, 255, 0.1);
  border-color: rgba(255, 255, 255, 0.1);
}

@media (max-width: 768px) {
  .mobile-menu-btn {
    display: flex;
    align-items: center;
    justify-content: center;
  }
}

/* Mobile menu */
.mobile-menu {
  display: none;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  background: rgba(255, 255, 255, 0.05);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
}

@media (max-width: 768px) {
  .mobile-menu {
    display: block;
  }
}

/* Slide animation for mobile menu */
.slide-fade-enter-active {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.slide-fade-leave-active {
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

.slide-fade-enter-from,
.slide-fade-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

/* Reduced motion */
@media (prefers-reduced-motion: reduce) {
  .slide-fade-enter-active,
  .slide-fade-leave-active {
    transition: none;
  }
  
  .mobile-menu-btn svg {
    transition: none;
  }
}
</style>