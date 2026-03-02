import { ref, onMounted, onUnmounted, readonly } from 'vue';

/**
 * Reactive breakpoint composable based on Tailwind CSS breakpoints.
 * 
 * Breakpoints:
 * - sm: 640px
 * - md: 768px
 * - lg: 1024px
 * 
 * Logic:
 * - isMobile: < 768px (md)
 * - isTablet: >= 768px (md) and < 1024px (lg)
 * - isDesktop: >= 1024px (lg)
 */

// Simple debounce utility
function debounce<T extends unknown[]>(fn: (...args: T) => void, delay: number) {
  let timeoutId: ReturnType<typeof setTimeout>;
  return (...args: T) => {
    clearTimeout(timeoutId);
    timeoutId = setTimeout(() => fn(...args), delay);
  };
}

export function useBreakpoint() {
  const width = ref(typeof window !== 'undefined' ? window.innerWidth : 0);
  
  const isMobile = ref(false);
  const isTablet = ref(false);
  const isDesktop = ref(false);

  const updateState = () => {
    const w = width.value;
    
    isMobile.value = w < 768;
    isTablet.value = w >= 768 && w < 1024;
    isDesktop.value = w >= 1024;
  };

  const handleResize = () => {
    width.value = window.innerWidth;
    updateState();
  };

  // Debounced resize handler (100ms)
  const debouncedResize = debounce(handleResize, 100);

  onMounted(() => {
    if (typeof window !== 'undefined') {
      handleResize(); // Initial check
      window.addEventListener('resize', debouncedResize);
    }
  });

  onUnmounted(() => {
    if (typeof window !== 'undefined') {
      window.removeEventListener('resize', debouncedResize);
    }
  });

  // Initial update
  updateState();

  return {
    isMobile: readonly(isMobile),
    isTablet: readonly(isTablet),
    isDesktop: readonly(isDesktop),
    width: readonly(width),
  };
}
