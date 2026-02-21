<template>
  <div class="download-center">
    <div class="section-header">
      <h2 class="section-title">{{ $t('dashboard.downloads.title') }}</h2>
      <p class="section-description">{{ $t('dashboard.downloads.description') }}</p>
    </div>

    <!-- Loading State -->
    <div v-if="loading" class="loading-state">
      <div class="spinner"></div>
      <p>{{ $t('common.loading') }}</p>
    </div>

    <!-- Resources Grid -->
    <div v-else-if="resources.length > 0" class="resources-grid">
      <div
        v-for="resource in resources"
        :key="resource.id"
        class="resource-card"
      >
        <div class="resource-icon">
          <component :is="getResourceIcon(resource.icon)" class="w-8 h-8" />
        </div>
        <div class="resource-info">
          <h3 class="resource-name">{{ resource.name }}</h3>
          <p class="resource-description">{{ resource.description }}</p>
          <div class="resource-meta">
            <span v-if="resource.version" class="meta-item">
              <Tag class="w-3 h-3" />
              {{ resource.version }}
            </span>
            <span v-if="resource.size" class="meta-item">
              <HardDrive class="w-3 h-3" />
              {{ resource.size }}
            </span>
          </div>
        </div>
        <a
          :href="resource.url"
          target="_blank"
          rel="noopener noreferrer"
          class="download-btn"
        >
          <Download class="w-4 h-4" />
          {{ $t('dashboard.downloads.download') }}
        </a>
      </div>
    </div>

    <!-- Empty State -->
    <div v-else class="empty-state">
      <div class="empty-icon">
        <Package class="w-16 h-16" />
      </div>
      <h3>{{ $t('dashboard.downloads.no_resources') }}</h3>
      <p>{{ $t('dashboard.downloads.no_resources_hint') }}</p>
    </div>

    <!-- Instructions -->
    <div class="instructions-card glass-card">
      <h3 class="instructions-title">
        <HelpCircle class="w-5 h-5" />
        {{ $t('dashboard.downloads.instructions_title') }}
      </h3>
      <div class="instructions-list">
        <div class="instruction-item">
          <span class="instruction-number">1</span>
          <p>{{ $t('dashboard.downloads.instruction_1') }}</p>
        </div>
        <div class="instruction-item">
          <span class="instruction-number">2</span>
          <p>{{ $t('dashboard.downloads.instruction_2') }}</p>
        </div>
        <div class="instruction-item">
          <span class="instruction-number">3</span>
          <p>{{ $t('dashboard.downloads.instruction_3') }}</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, markRaw } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  Download,
  Package,
  Tag,
  HardDrive,
  HelpCircle,
  FileArchive,
  Gamepad2,
  Monitor,
} from 'lucide-vue-next'
import { apiService, type DownloadResource } from '@/services/api'

const { t } = useI18n()

const loading = ref(true)
const resources = ref<DownloadResource[]>([])

// Default mock resources when API is not available
const defaultResources: DownloadResource[] = [
  {
    id: 'client-modpack',
    name: 'Client Modpack',
    description: 'Required modpack for the server. Includes all necessary mods and configurations.',
    version: '1.0.0',
    size: '256 MB',
    url: '#',
    icon: 'gamepad',
  },
  {
    id: 'server-resource-pack',
    name: 'Server Resource Pack',
    description: 'Custom textures and assets for the server.',
    version: '2.1.0',
    size: '64 MB',
    url: '#',
    icon: 'package',
  },
]

const getResourceIcon = (icon?: string) => {
  switch (icon) {
    case 'gamepad':
      return markRaw(Gamepad2)
    case 'monitor':
      return markRaw(Monitor)
    case 'package':
    default:
      return markRaw(FileArchive)
  }
}

const loadResources = async () => {
  try {
    const response = await apiService.getDownloadResources()
    if (response.success && response.resources) {
      resources.value = response.resources
    } else {
      // Use default resources when API is not available
      resources.value = defaultResources
    }
  } catch (error) {
    console.error('Failed to load resources:', error)
    // Use default resources on error
    resources.value = defaultResources
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadResources()
})
</script>

<style scoped>
.download-center {
  max-width: 900px;
  margin: 0 auto;
}

.section-header {
  margin-bottom: 2rem;
}

.section-title {
  font-size: 1.5rem;
  font-weight: 700;
  color: white;
  margin: 0 0 0.5rem 0;
}

.section-description {
  color: rgba(255, 255, 255, 0.6);
  margin: 0;
}

/* Loading State */
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 4rem 2rem;
  color: rgba(255, 255, 255, 0.6);
}

.spinner {
  width: 40px;
  height: 40px;
  border: 3px solid rgba(255, 255, 255, 0.1);
  border-top-color: #8b5cf6;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin-bottom: 1rem;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* Resources Grid */
.resources-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 1.25rem;
  margin-bottom: 2rem;
}

.resource-card {
  background: rgba(255, 255, 255, 0.03);
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
  transition: all 0.2s;
}

.resource-card:hover {
  background: rgba(255, 255, 255, 0.05);
  border-color: rgba(139, 92, 246, 0.3);
}

.resource-icon {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.2) 0%, rgba(139, 92, 246, 0.2) 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #8b5cf6;
  margin-bottom: 1rem;
}

.resource-info {
  flex: 1;
  margin-bottom: 1rem;
}

.resource-name {
  font-size: 1.125rem;
  font-weight: 600;
  color: white;
  margin: 0 0 0.5rem 0;
}

.resource-description {
  font-size: 0.875rem;
  color: rgba(255, 255, 255, 0.6);
  margin: 0 0 0.75rem 0;
  line-height: 1.5;
}

.resource-meta {
  display: flex;
  gap: 1rem;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  font-size: 0.75rem;
  color: rgba(255, 255, 255, 0.5);
}

.download-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 0.75rem 1rem;
  border-radius: 10px;
  background: linear-gradient(135deg, #3b82f6 0%, #8b5cf6 100%);
  color: white;
  font-weight: 600;
  font-size: 0.875rem;
  text-decoration: none;
  transition: all 0.2s;
}

.download-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(139, 92, 246, 0.3);
}

/* Empty State */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 4rem 2rem;
  text-align: center;
}

.empty-icon {
  width: 80px;
  height: 80px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.05);
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(255, 255, 255, 0.3);
  margin-bottom: 1.5rem;
}

.empty-state h3 {
  font-size: 1.25rem;
  font-weight: 600;
  color: white;
  margin: 0 0 0.5rem 0;
}

.empty-state p {
  color: rgba(255, 255, 255, 0.5);
  margin: 0;
}

/* Instructions Card */
.glass-card {
  background: rgba(255, 255, 255, 0.03);
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  padding: 1.5rem;
}

.instructions-title {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 1.125rem;
  font-weight: 600;
  color: white;
  margin: 0 0 1.25rem 0;
}

.instructions-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.instruction-item {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
}

.instruction-number {
  width: 24px;
  height: 24px;
  border-radius: 8px;
  background: linear-gradient(135deg, #3b82f6 0%, #8b5cf6 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.75rem;
  font-weight: 700;
  color: white;
  flex-shrink: 0;
}

.instruction-item p {
  color: rgba(255, 255, 255, 0.7);
  margin: 0;
  font-size: 0.875rem;
  line-height: 1.5;
}

/* Responsive */
@media (max-width: 640px) {
  .resources-grid {
    grid-template-columns: 1fr;
  }
}
</style>
