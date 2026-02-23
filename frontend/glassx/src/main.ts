import { createApp, provide, ref, h } from 'vue'
import App from './App.vue'
import router from './router'
import i18n from './i18n'
import './index.css'

interface RuntimeConfig {
  wsPort?: number
}

// 全局错误处理
window.addEventListener('error', (event) => {
  console.error('Global error:', event.error)
})

window.addEventListener('unhandledrejection', (event) => {
  console.error('Unhandled promise rejection:', event.reason)
})

const app = createApp({
  setup() {
    const config = ref<RuntimeConfig>({});
    
    const loadConfig = async () => {
      try {
        const response = await fetch('/api/config');
        if (!response.ok) {
          throw new Error(`HTTP error! status: ${response.status}`);
        }
        const data = await response.json();
        config.value = data.config || data;
      } catch (error) {
        console.error('Failed to load config:', error);
      }
    };

    const getWsPort = (): number => {
      if (typeof config.value.wsPort === 'number' && Number.isFinite(config.value.wsPort)) {
        return config.value.wsPort
      }
      return window.location.port ? (parseInt(window.location.port, 10) + 1) : 8081
    }

    const reloadConfig = async (): Promise<boolean> => {
      try {
        const response = await fetch('/api/reload-config', { method: 'POST' });
        if (!response.ok) {
          throw new Error(`HTTP error! status: ${response.status}`);
        }
        await loadConfig();
        return true;
      } catch (error) {
        console.error('Failed to reload config:', error);
        return false;
      }
    };
    
    provide('config', config);
    provide('reloadConfig', reloadConfig);
    provide('getWsPort', getWsPort);
    
    // 初始加载配置
    loadConfig();
  },
  render: () => h(App)
});

app.use(router)
app.use(i18n)

app.mount('#app') 