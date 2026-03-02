import { ref } from 'vue';

// Shared global sidebar state across components.
const isOpen = ref(false);
const isCollapsed = ref(false);
const showTrigger = ref(false);

export function useSidebar() {
  const toggleOpen = () => {
    isOpen.value = !isOpen.value;
  };

  const setOpen = (value: boolean) => {
    isOpen.value = value;
  };

  const toggleCollapse = () => {
    isCollapsed.value = !isCollapsed.value;
  };

  const setCollapse = (value: boolean) => {
    isCollapsed.value = value;
  };

  const toggleTrigger = () => {
    showTrigger.value = !showTrigger.value;
  };

  const setTrigger = (value: boolean) => {
    showTrigger.value = value;
  };

  return {
    isOpen,
    isCollapsed,
    showTrigger,
    toggleOpen,
    setOpen,
    toggleCollapse,
    setCollapse,
    toggleTrigger,
    setTrigger,
  };
}
