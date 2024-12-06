import { fileURLToPath, URL } from 'node:url'
import { resolve } from 'path'
import { defineConfig } from 'vite'
import vitePluginFaviconsInject from 'vite-plugin-favicons-inject'
import vue from '@vitejs/plugin-vue2'
import { esbuildCommonjs } from '@originjs/vite-plugin-commonjs'
import autoPreload from 'vite-plugin-auto-preload'
//import vueDevTools from 'vite-plugin-vue-devtools'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vitePluginFaviconsInject('src/main/javascript/assets/favicon.png', {
      appName: 'xtreme video',
      appShortName: 'xtreme',
      appDescription: 'Social video',
      display: 'minimal-ui',
      start_url: '/index.html'
    }),
    vue(),
    autoPreload()
    //vueDevTools(),
  ],
  build: {
    outDir: 'target/generated-resources/webroot',
    commonjsOptions:{
      requireReturnsDefault: 'auto'
    },
    optimizeDeps: {
      esbuildOptions: {
        plugins: [esbuildCommonjs(['react-moment'])]
      }
    },
    rollupOptions: {
      input: {
        main: resolve(__dirname, 'index.html'),
        dashboard: resolve(__dirname, 'dashboard.html'),
      },
      output: {
        manualChunks(id) {
          //if (id.includes('rm-emoji-picker')) return 'rm-emoji-picker'
          if (id.includes('bootstrap')) return 'bootstrap'
          if (id.includes('hls.js')) return 'video'
          if (id.includes('video.js')) return 'video'
          //if (id.includes('/dashboard/')) return 'dashboard'
          //if (id.includes('/store/')) return 'store'
          //if (id.includes('/chat/')) return 'zchat'
          //return 'app'
          //if (id.includes('node_modules')) {
          //  return id.toString().split('node_modules/')[1].split('/')[0].toString();
          //}
        }
      }
    }
  },
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src/main/javascript', import.meta.url))
    },
  },
})
