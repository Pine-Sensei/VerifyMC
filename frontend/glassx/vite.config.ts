import { defineConfig } from "vite"
import vue from "@vitejs/plugin-vue"
import { resolve } from "path"
import { readFileSync } from "fs"

const packageJson = JSON.parse(
  readFileSync(resolve(__dirname, "package.json"), "utf-8")
) as { version?: string }

export default defineConfig({
  define: {
    __APP_VERSION__: JSON.stringify(packageJson.version ?? "0.0.0"),
  },
  plugins: [
    vue({
      template: {
        compilerOptions: {
          // Enable hoisting for better performance
          hoistStatic: true,
          // Cache inline event handlers
          cacheHandlers: true,
        }
      }
    })
  ],
  resolve: {
    alias: {
      "@": resolve(__dirname, "src"),
    },
  },
  build: {
    outDir: "dist",
    assetsDir: "assets",
    sourcemap: false,
    minify: "terser",
    // Optimize chunk size
    chunkSizeWarningLimit: 1000,
    rollupOptions: {
      output: {
        manualChunks: (id) => {
          if (id.includes("node_modules/vue") || id.includes("node_modules/vue-router") || id.includes("node_modules/vue-i18n")) {
            return "vendor"
          }
          if (id.includes("node_modules/axios") || id.includes("node_modules/lucide-vue-next")) {
            return "utils"
          }
          if (id.includes("src/components/HeroGeometric.vue")) {
            return "components"
          }
        },
        // Optimize asset naming
        assetFileNames: (assetInfo) => {
          const name = assetInfo.name ?? ""
          const info = name.split('.')
          const ext = info[info.length - 1] ?? ""
          if (/png|jpe?g|svg|gif|tiff|bmp|ico/i.test(ext)) {
            return `images/[name]-[hash][extname]`
          }
          if (/css/i.test(ext)) {
            return `css/[name]-[hash][extname]`
          }
          return `assets/[name]-[hash][extname]`
        },
        chunkFileNames: 'js/[name]-[hash].js',
        entryFileNames: 'js/[name]-[hash].js',
      },
    },
    // Enable terser optimizations
    terserOptions: {
      compress: {
        drop_console: true,
        drop_debugger: true,
        pure_funcs: ['console.log'],
      },
    },
  },
  server: {
    port: 3000,
    proxy: {
      "/api": {
        target: "http://localhost:8080",
        changeOrigin: true,
        secure: false,
      },
    },
  },
  // Performance optimizations
  optimizeDeps: {
    include: ['vue', 'vue-router', 'vue-i18n'],
    exclude: ['@vueuse/core'],
  },
  // CSS optimizations
  css: {
    devSourcemap: false,
    preprocessorOptions: {
      scss: {
        charset: false,
      },
    },
  },
})
