import { ref, onUnmounted } from 'vue'

export function useCooldown() {
  const cooldownSeconds = ref(0)
  const cooldownTimer = ref<ReturnType<typeof setInterval> | null>(null)

  const startCooldown = (seconds: number) => {
    cooldownSeconds.value = seconds
    if (cooldownTimer.value) clearInterval(cooldownTimer.value)
    cooldownTimer.value = setInterval(() => {
      cooldownSeconds.value--
      if (cooldownSeconds.value <= 0) {
        clearInterval(cooldownTimer.value!)
        cooldownTimer.value = null
      }
    }, 1000)
  }

  const stopCooldown = () => {
    if (cooldownTimer.value) {
      clearInterval(cooldownTimer.value)
      cooldownTimer.value = null
    }
    cooldownSeconds.value = 0
  }

  onUnmounted(() => {
    stopCooldown()
  })

  return {
    cooldownSeconds,
    startCooldown,
    stopCooldown
  }
}
