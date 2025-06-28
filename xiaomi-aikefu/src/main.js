

import { createApp } from 'vue'
import App from './App.vue'
import router from './router' // 1. 确保正确导入了 router 实例

const app = createApp(App)

app.use(router) // 2. 确保在挂载(mount)之前调用了 app.use(router)

app.mount('#app') // 3. 将应用挂载到HTML中的 #app 元素上
