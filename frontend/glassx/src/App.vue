<template>
  <div id="app">
    <!-- Top Navigation -->
    <TopNavigation />

    <!-- Main Content with top padding for navigation -->
    <div class="pt-16 pb-safe relative z-10">
      <router-view />
    </div>

    <!-- Notification System -->
    <NotificationSystem ref="notificationSystemRef" />

    <!-- Enhanced Footer -->
    <footer class="app-footer">
      <!-- Gradient top border -->
      <div class="footer-gradient-line"></div>
      
      <div class="footer-content">
        <div class="footer-row">
          <span class="footer-brand">VerifyMC by KiteMC Team © 2025-{{ currentYear }}</span>
          <span class="footer-divider">•</span>
          <span class="footer-love">
            <span>Made with</span>
            <span class="heart-icon">💖</span>
          </span>
        </div>
      </div>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { inject, watch, ref, onMounted, computed } from 'vue'
import { useNotification } from '@/composables/useNotification'
import NotificationSystem from '@/components/NotificationSystem.vue'
import TopNavigation from '@/components/TopNavigation.vue'

const config = inject('config', { value: {} as any })
const reloadConfig = inject('reloadConfig', () => { })
const { setNotificationSystem } = useNotification()

const notificationSystemRef = ref()

// Dynamic year for footer
const currentYear = computed(() => new Date().getFullYear())

onMounted(() => {
  if (notificationSystemRef.value) {
    setNotificationSystem(notificationSystemRef.value)
  }
})

// 监听配置变化，动态设置页面标题
watch(() => config.value?.frontend?.web_server_prefix, (newPrefix: string | undefined) => {
  if (newPrefix) {
    document.title = newPrefix
    console.log('Page title updated:', newPrefix)
  } else {
    document.title = 'VerifyMC - GlassX Theme'
  }
}, { immediate: true })

// 暴露重载配置方法给全局
if (typeof window !== 'undefined') {
  (window as any).reloadVerifyMCConfig = reloadConfig
}
</script>

<style>
/* ===========================================
   Global CSS Custom Properties
   =========================================== */
:root {
  --glass-bg: rgba(255, 255, 255, 0.08);
  --glass-bg-hover: rgba(255, 255, 255, 0.12);
  --glass-border: rgba(255, 255, 255, 0.15);
  --glass-border-hover: rgba(255, 255, 255, 0.25);
  --glass-blur: 20px;
  --glass-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
  --glass-inset: inset 0 1px 0 rgba(255, 255, 255, 0.1);
  
  --gradient-primary: linear-gradient(135deg, #3b82f6 0%, #8b5cf6 100%);
  --gradient-accent: linear-gradient(90deg, #3b82f6, #8b5cf6, #ec4899);
  
  --transition-smooth: cubic-bezier(0.4, 0, 0.2, 1);
  --transition-bounce: cubic-bezier(0.68, -0.55, 0.265, 1.55);
}

/* ===========================================
   Base Styles
   =========================================== */
#app {
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  min-height: 100vh;
  background: #030303;
  transform: translateZ(0);
}

body {
  background: #030303;
  margin: 0;
  padding: 0;
}

* {
  box-sizing: border-box;
}

/* ===========================================
   Enhanced Footer Styles
   =========================================== */
.app-footer {
  position: relative;
  width: 100%;
  margin-top: 2rem;
  background: rgba(255, 255, 255, 0.03);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  z-index: 10;
}

.footer-gradient-line {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, 
    transparent 0%, 
    rgba(59, 130, 246, 0.3) 20%, 
    rgba(139, 92, 246, 0.3) 50%, 
    rgba(236, 72, 153, 0.3) 80%, 
    transparent 100%
  );
}

.footer-content {
  padding: 1.5rem 1rem;
  text-align: center;
}

.footer-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.footer-brand {
  color: rgba(255, 255, 255, 0.7);
  font-size: 0.875rem;
  font-weight: 500;
}

.footer-divider {
  color: rgba(255, 255, 255, 0.3);
}

.footer-love {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  color: rgba(255, 255, 255, 0.5);
  font-size: 0.875rem;
}

.heart-icon {
  color: #f9a8d4;
  animation: heartbeat 2s ease-in-out infinite;
}

@keyframes heartbeat {
  0%, 100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.1);
  }
}

/* ===========================================
   Global Glass Morphism Utilities
   =========================================== */
.glass-card {
  background: var(--glass-bg);
  backdrop-filter: blur(var(--glass-blur));
  -webkit-backdrop-filter: blur(var(--glass-blur));
  border: 1px solid var(--glass-border);
  border-radius: 16px;
  box-shadow: var(--glass-shadow), var(--glass-inset);
  transition: all 0.3s var(--transition-smooth);
}

.glass-card:hover {
  background: var(--glass-bg-hover);
  border-color: var(--glass-border-hover);
}

.glass-button {
  background: var(--glass-bg);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid var(--glass-border);
  border-radius: 12px;
  color: #fff;
  cursor: pointer;
  transition: all 0.3s var(--transition-smooth);
}

.glass-button:hover {
  background: var(--glass-bg-hover);
  border-color: var(--glass-border-hover);
  transform: translateY(-2px);
}

/* Gradient text utility */
.gradient-text {
  background: var(--gradient-primary);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

/* Glow effect utility */
.glow-effect {
  box-shadow: 0 0 20px rgba(59, 130, 246, 0.3);
}

/* ===========================================
   Mobile Safe Area Support
   =========================================== */
.pb-safe {
  padding-bottom: env(safe-area-inset-bottom);
}

@supports (-webkit-touch-callout: none) {
  .min-h-screen {
    min-height: -webkit-fill-available;
  }
}

/* ===========================================
   Animation Utilities
   =========================================== */
@keyframes pulse {
  0%, 100% {
    opacity: 0.3;
    transform: translateZ(0);
  }
  50% {
    opacity: 0.7;
    transform: translateZ(0);
  }
}

.animate-pulse {
  animation: pulse 8s cubic-bezier(0.4, 0, 0.6, 1) infinite;
  transform: translateZ(0);
  will-change: opacity;
}

/* ===========================================
   Low Performance Mode
   =========================================== */
.low-performance-mode {
  backdrop-filter: none !important;
  -webkit-backdrop-filter: none !important;
  filter: none !important;
  box-shadow: none !important;
  text-shadow: none !important;
}

.low-performance-mode * {
  animation: none !important;
  transition: none !important;
  transform: none !important;
  backdrop-filter: none !important;
  -webkit-backdrop-filter: none !important;
}

.low-performance-mode .glass-card {
  background: rgba(255, 255, 255, 0.1) !important;
  backdrop-filter: none !important;
  border: 1px solid rgba(255, 255, 255, 0.2) !important;
}

.low-performance-mode .glass-button {
  background: rgba(255, 255, 255, 0.1) !important;
  backdrop-filter: none !important;
}

/* ===========================================
   Reduced Motion Support
   =========================================== */
@media (prefers-reduced-motion: reduce) {
  .animate-pulse,
  .heart-icon {
    animation: none;
  }
  
  * {
    animation-duration: 0.01ms !important;
    transition-duration: 0.01ms !important;
  }
}

/* ===========================================
   Scrollbar Styling
   =========================================== */
::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.05);
}

::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.2);
  border-radius: 4px;
}

::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.3);
}

/* Firefox scrollbar */
* {
  scrollbar-width: thin;
  scrollbar-color: rgba(255, 255, 255, 0.2) rgba(255, 255, 255, 0.05);
}

/* ===========================================
   Selection Styling
   =========================================== */
::selection {
  background: rgba(59, 130, 246, 0.3);
  color: #fff;
}

::-moz-selection {
  background: rgba(59, 130, 246, 0.3);
  color: #fff;
}
</style>