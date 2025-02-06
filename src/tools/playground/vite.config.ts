import {defineConfig} from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
    plugins: [
        react()
    ],
    optimizeDeps: {
        include: ['wirespec'], // Add the package containing this namespace
    },
})
