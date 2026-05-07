import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import 'nprogress/nprogress.css'
import './styles/index.css'
import App from './App.vue'
import router from './router'
import pinia from './stores'
import permissionDirective from './directives/permission'

const app = createApp(App)
app.use(ElementPlus)
app.use(pinia)
app.use(router)
app.use(permissionDirective)
app.mount('#app')
