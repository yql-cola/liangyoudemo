import Vue from 'vue'
import ElementUI from 'element-ui'
import 'element-ui/lib/theme-chalk/index.css'
import 'normalize.css/normalize.css'
import App from './App.vue'
import router from './router'
import store from './store'
import './permission'
import permissionDirective from './directive/permission'

Vue.use(ElementUI, { size: 'small' })
Vue.use(permissionDirective)
Vue.config.productionTip = false

new Vue({
  router,
  store,
  render: h => h(App)
}).$mount('#app')
