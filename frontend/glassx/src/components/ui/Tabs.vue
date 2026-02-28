<template>
  <div class="w-full">
    <div
      class="inline-flex h-10 items-center justify-center rounded-md bg-white/5 backdrop-blur-sm p-1 text-white/70 border border-white/10"
      role="tablist"
      :aria-label="ariaLabel"
    >
      <button
        v-for="(tab, index) in tabs"
        :key="tab.value"
        type="button"
        :id="`tab-${tab.value}`"
        :role="'tab'"
        :aria-selected="activeTab === tab.value"
        :aria-controls="`tabpanel-${tab.value}`"
        :tabindex="activeTab === tab.value ? 0 : -1"
        @click="selectTab(tab.value)"
        @keydown="handleKeyDown($event, index)"
        class="inline-flex items-center justify-center whitespace-nowrap rounded-sm px-3 py-1.5 text-sm font-medium ring-offset-background transition-all focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-50"
        :class="activeTab === tab.value 
          ? 'bg-white/20 text-white shadow-sm' 
          : 'text-white/70 hover:bg-white/5 hover:text-white'"
      >
        {{ tab.label }}
      </button>
    </div>
    
    <div class="mt-2" role="tabpanel" :id="`tabpanel-${activeTab}`" :aria-labelledby="`tab-${activeTab}`">
      <slot :active-tab="activeTab" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, provide, nextTick, watch } from 'vue'

interface Tab {
  value: string
  label: string
}

interface Props {
  tabs: Tab[]
  defaultTab?: string
  ariaLabel?: string
  modelValue?: string
}

const props = withDefaults(defineProps<Props>(), {
  defaultTab: '',
  ariaLabel: 'Tabs'
})

const emit = defineEmits(['update:modelValue', 'change'])

const activeTab = ref(props.modelValue || props.defaultTab || props.tabs[0]?.value || '')

// Watch for external changes
watch(() => props.modelValue, (newValue) => {
  if (newValue) {
    activeTab.value = newValue
  }
})

// Provide active tab to child components
provide('activeTab', activeTab)

// Select a tab
const selectTab = (value: string) => {
  activeTab.value = value
  emit('update:modelValue', value)
  emit('change', value)
}

// Handle keyboard navigation
const handleKeyDown = (event: KeyboardEvent, currentIndex: number) => {
  const tabs = props.tabs
  let newIndex = currentIndex

  switch (event.key) {
    case 'ArrowLeft':
      event.preventDefault()
      newIndex = currentIndex === 0 ? tabs.length - 1 : currentIndex - 1
      break
    case 'ArrowRight':
      event.preventDefault()
      newIndex = currentIndex === tabs.length - 1 ? 0 : currentIndex + 1
      break
    case 'Home':
      event.preventDefault()
      newIndex = 0
      break
    case 'End':
      event.preventDefault()
      newIndex = tabs.length - 1
      break
    default:
      return
  }

  // Update active tab and focus the new tab
  selectTab(tabs[newIndex].value)
  
  // Focus the new tab button
  nextTick(() => {
    const tabButton = document.getElementById(`tab-${tabs[newIndex].value}`)
    tabButton?.focus()
  })
}
</script> 
