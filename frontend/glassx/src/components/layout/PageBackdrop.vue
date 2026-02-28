<template>
  <div class="app-backdrop">
    <div class="noise-mask" aria-hidden="true" />
  </div>
</template>

<style scoped>
.noise-mask {
  position: absolute;
  inset: 0;
  opacity: 0.06;
  background-image: radial-gradient(circle at 1px 1px, rgba(255, 255, 255, 0.8) 1px, transparent 0);
  background-size: 3px 3px;
}
</style>
