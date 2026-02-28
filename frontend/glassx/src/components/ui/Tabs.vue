<template>
  <div class="w-full">
    <div
      class="inline-flex h-10 items-center justify-center rounded-md bg-white/5 backdrop-blur-sm p-1 text-white/70 border border-white/10"
      role="tablist"
      :aria-label="ariaLabel"
    >
      <Button
        v-for="(tab, index) in tabs"
        :key="tab.value"
        variant="ghost"
        :id="`tab-${tab.value}`"
        :role="'tab'"
        :aria-selected="activeTab === tab.value"
        :aria-controls="`tabpanel-${tab.value}`"
        :tabindex="activeTab === tab.value ? 0 : -1"
        @click="selectTab(tab.value)"
        @keydown="handleKeyDown($event, index)"
        class="h-auto rounded-sm px-3 py-1.5 text-sm font-medium transition-all focus-visible:ring-white/50"
        :class="activeTab === tab.value 
          ? 'bg-white/20 text-white shadow-sm hover:bg-white/20' 
          : 'text-white/70 hover:text-white hover:bg-white/5'"
      >
        {{ tab.label }}
      </Button>
    </div>
    
    <div class="mt-2" role="tabpanel" :id="`tabpanel-${activeTab}`" :aria-labelledby="`tab-${activeTab}`">
      <slot :active-tab="activeTab" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, provide, nextTick } from 'vue'
import Button from './Button.vue'

interface Tab {
  value: string
  label: string
}

interface Props {
  tabs: Tab[]
  defaultTab?: string
  ariaLabel?: string
}

const props = withDefaults(defineProps<Props>(), {
  defaultTab: '',
  ariaLabel: 'Tabs'
})

const activeTab = ref(props.defaultTab || props.tabs[0]?.value || '')

// Provide active tab to child components
provide('activeTab', activeTab)

// Select a tab
const selectTab = (value: string) => {
  activeTab.value = value
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
  activeTab.value = tabs[newIndex].value
  
  // Focus the new tab button
  nextTick(() => {
    const tabButton = document.getElementById(`tab-${tabs[newIndex].value}`)
    tabButton?.focus()
  })
}
</script> 
