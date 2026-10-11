# Grain Oil Vue Frontend Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a Vue 2 + Element UI admin frontend under `frontend/` for the existing grain-oil inventory Spring Boot API.

**Architecture:** The frontend is an independent Vue CLI app inside the backend repository. It uses a RuoYi-style layout, fixed first-phase menu routes, Vuex-backed auth state, Axios interceptors for the backend `code === 0` response contract, and per-module API wrappers aligned to the current Controllers.

**Tech Stack:** Vue 2.6, Vue CLI 4, Vue Router 3, Vuex 3, Element UI 2, Axios, ECharts, Sass, Jest/Vue Test Utils where practical.

---

## Confirmed Decisions

- Create the app in `frontend/`.
- Use fixed menus for phase one.
- Use `/api/v1/auth/me` roles and permissions for button-level authorization.
- Use backend permission codes, not the draft codes in the design document:
  - `system:user:create`, `system:user:update`, `system:user:delete`, `system:user:list`, `system:user:view`
  - `system:user:grant-role`, `system:user:grant-permission`, `system:user:view-permission`
  - `system:role:create`, `system:role:update`, `system:role:delete`, `system:role:list`, `system:role:grant`
  - `system:menu:view`
  - `purchase:order:create`, `purchase:order:update`, `purchase:order:delete`, `purchase:order:list`, `purchase:order:view`
  - `sale:order:create`, `sale:order:update`, `sale:order:delete`, `sale:order:list`, `sale:order:view`
  - `inventory:stock:list`, `inventory:stock:update`, `inventory:stock:delete`, `inventory:batch:list`, `inventory:flow:list`
  - `statistics:daily:view`, `statistics:monthly:view`, `statistics:customer:view`, `statistics:supplier:view`, `statistics:category:view`
- Because product, warehouse, supplier, customer, unit, and category Controllers do not exist yet, phase one uses numeric ID inputs and clear placeholders instead of fake lookup data.
- Role management uses a non-paginated table because `GET /api/v1/roles` returns `List<RoleResponse>`.
- Dashboard uses existing statistics and inventory APIs only; stock warning remains a visual placeholder.

## File Structure

Create:

- `frontend/package.json`: frontend scripts and dependencies.
- `frontend/vue.config.js`: dev server proxy to Spring Boot and build settings.
- `frontend/.env.development`: development API base configuration.
- `frontend/.env.production`: production API base configuration.
- `frontend/babel.config.js`: Vue CLI Babel config.
- `frontend/src/main.js`: Vue bootstrap.
- `frontend/src/App.vue`: root router outlet.
- `frontend/src/permission.js`: route guard and current-user recovery.
- `frontend/src/router/index.js`: constant routes and fixed business menu routes.
- `frontend/src/store/index.js`: Vuex root store.
- `frontend/src/store/modules/user.js`: token, user info, roles, permissions.
- `frontend/src/store/modules/settings.js`: sidebar and UI settings.
- `frontend/src/utils/request.js`: Axios instance and response normalization.
- `frontend/src/utils/auth.js`: token persistence.
- `frontend/src/utils/permission.js`: permission helper functions.
- `frontend/src/directive/permission/index.js`: `v-permission` directive.
- `frontend/src/api/auth.js`: auth API wrapper.
- `frontend/src/api/system/user.js`: user API wrapper.
- `frontend/src/api/system/role.js`: role API wrapper.
- `frontend/src/api/system/menu.js`: menu API wrapper.
- `frontend/src/api/purchase/purchaseIn.js`: purchase API wrapper.
- `frontend/src/api/sale/saleOut.js`: sale API wrapper.
- `frontend/src/api/inventory/inventory.js`: inventory API wrapper.
- `frontend/src/api/statistics/statistics.js`: statistics API wrapper.
- `frontend/src/layout/index.vue`: admin shell.
- `frontend/src/layout/components/Sidebar.vue`: menu rendering.
- `frontend/src/layout/components/Navbar.vue`: top bar and logout.
- `frontend/src/layout/components/AppMain.vue`: content router outlet.
- `frontend/src/components/Pagination/index.vue`: backend `pageNum`/`pageSize` pagination.
- `frontend/src/components/RightToolbar/index.vue`: table toolbar.
- `frontend/src/components/DictTag/index.vue`: simple status tag component.
- `frontend/src/views/login.vue`: login page.
- `frontend/src/views/dashboard/index.vue`: dashboard.
- `frontend/src/views/system/user/index.vue`: user management.
- `frontend/src/views/system/role/index.vue`: role management.
- `frontend/src/views/system/menu/index.vue`: menu tree view.
- `frontend/src/views/purchase/in/index.vue`: purchase list and editor.
- `frontend/src/views/purchase/in/detail.vue`: purchase detail.
- `frontend/src/views/sale/out/index.vue`: sale list and editor.
- `frontend/src/views/sale/out/detail.vue`: sale detail.
- `frontend/src/views/inventory/stock/index.vue`: stock list and adjustment dialog.
- `frontend/src/views/inventory/batch/index.vue`: batch inventory view.
- `frontend/src/views/inventory/flow/index.vue`: inventory flow view.
- `frontend/src/views/statistics/daily.vue`: daily statistics.
- `frontend/src/views/statistics/monthly.vue`: monthly statistics.
- `frontend/src/views/statistics/customer.vue`: customer amount statistics.
- `frontend/src/views/statistics/supplier.vue`: supplier amount statistics.
- `frontend/src/views/statistics/category.vue`: category amount statistics.
- `frontend/src/views/basic/placeholder.vue`: shared placeholder for unimplemented basic-data modules.
- `frontend/src/views/error/401.vue`: unauthorized page.
- `frontend/src/views/error/404.vue`: not-found page.
- `frontend/tests/unit/request.spec.js`: Axios response behavior tests.
- `frontend/tests/unit/permission.spec.js`: permission helper tests.

Modify:

- `.gitignore`: add frontend dependency/build/cache ignores.

## Task 1: Frontend Scaffold

**Files:**
- Create: `frontend/package.json`
- Create: `frontend/vue.config.js`
- Create: `frontend/.env.development`
- Create: `frontend/.env.production`
- Create: `frontend/babel.config.js`
- Create: `frontend/src/main.js`
- Create: `frontend/src/App.vue`
- Modify: `.gitignore`

- [x] **Step 1: Write package and runtime skeleton**

Create `frontend/package.json` with:

```json
{
  "name": "liangyou-frontend",
  "version": "0.1.0",
  "private": true,
  "scripts": {
    "serve": "vue-cli-service serve",
    "build": "vue-cli-service build",
    "lint": "vue-cli-service lint",
    "test:unit": "vue-cli-service test:unit"
  },
  "dependencies": {
    "@vue/composition-api": "1.7.2",
    "axios": "1.12.2",
    "core-js": "3.38.1",
    "echarts": "5.5.1",
    "element-ui": "2.15.14",
    "js-cookie": "3.0.5",
    "normalize.css": "8.0.1",
    "nprogress": "0.2.0",
    "vue": "2.6.14",
    "vue-router": "3.6.5",
    "vuex": "3.6.2"
  },
  "devDependencies": {
    "@vue/cli-plugin-babel": "4.5.19",
    "@vue/cli-plugin-eslint": "4.5.19",
    "@vue/cli-plugin-unit-jest": "4.5.19",
    "@vue/cli-service": "4.5.19",
    "@vue/test-utils": "1.3.6",
    "babel-eslint": "10.1.0",
    "eslint": "6.8.0",
    "eslint-plugin-vue": "6.2.2",
    "sass": "1.69.7",
    "sass-loader": "10.4.1",
    "vue-template-compiler": "2.6.14"
  }
}
```

- [x] **Step 2: Add Vue CLI config**

Create `frontend/vue.config.js` with:

```js
module.exports = {
  publicPath: '/',
  outputDir: 'dist',
  assetsDir: 'static',
  productionSourceMap: false,
  devServer: {
    port: 8081,
    proxy: {
      [process.env.VUE_APP_BASE_API]: {
        target: process.env.VUE_APP_PROXY_TARGET || 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
}
```

Create `frontend/.env.development` with:

```env
NODE_ENV=development
VUE_APP_BASE_API=/api
VUE_APP_PROXY_TARGET=http://localhost:8080
```

Create `frontend/.env.production` with:

```env
NODE_ENV=production
VUE_APP_BASE_API=/api
```

Create `frontend/babel.config.js` with:

```js
module.exports = {
  presets: ['@vue/cli-plugin-babel/preset']
}
```

- [x] **Step 3: Add app bootstrap**

Create `frontend/src/main.js` with:

```js
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
```

Create `frontend/src/App.vue` with:

```vue
<template>
  <router-view />
</template>
```

- [x] **Step 4: Update ignores**

Append to `.gitignore`:

```gitignore

### Frontend ###
frontend/node_modules/
frontend/dist/
frontend/coverage/
frontend/.env.local
frontend/.env.*.local
```

- [x] **Step 5: Install dependencies**

Run:

```bash
cd frontend && npm install
```

Expected: `package-lock.json` is created and install exits with code 0.

**Development Record - 2026-10-11 22:00 Asia/Shanghai**

- Created Vue CLI 4 frontend scaffold files under `frontend/`.
- Added frontend ignore rules to `.gitignore`.
- Ran `npm install --registry=https://registry.npmjs.org/ --loglevel=warn` in `frontend/`.
- Result: install exited with code 0 and created `package-lock.json`.
- Note: npm reported Vue 2 / Vue CLI era deprecation and audit warnings; no dependency upgrade was applied because the approved design intentionally follows Vue 2 + Element UI / RuoYi-Vue architecture.

## Task 2: Auth, Request, Store, and Permissions

**Files:**
- Create: `frontend/tests/unit/request.spec.js`
- Create: `frontend/tests/unit/permission.spec.js`
- Create: `frontend/src/utils/request.js`
- Create: `frontend/src/utils/auth.js`
- Create: `frontend/src/utils/permission.js`
- Create: `frontend/src/store/index.js`
- Create: `frontend/src/store/modules/user.js`
- Create: `frontend/src/store/modules/settings.js`
- Create: `frontend/src/api/auth.js`
- Create: `frontend/src/directive/permission/index.js`

- [x] **Step 1: Write failing tests for permission helpers**

Create `frontend/tests/unit/permission.spec.js` with:

```js
import { hasPermission } from '@/utils/permission'

describe('permission helpers', () => {
  test('allows access when user has required permission', () => {
    expect(hasPermission(['system:user:create'], 'system:user:create')).toBe(true)
  })

  test('denies access when required permission is missing', () => {
    expect(hasPermission(['system:user:list'], 'system:user:create')).toBe(false)
  })

  test('allows super admin wildcard permission', () => {
    expect(hasPermission(['*:*:*'], 'inventory:stock:update')).toBe(true)
  })
})
```

- [x] **Step 2: Run test to verify it fails**

Run:

```bash
cd frontend && npm run test:unit -- tests/unit/permission.spec.js
```

Expected: FAIL because `@/utils/permission` does not exist.

- [x] **Step 3: Implement permission helpers**

Create `frontend/src/utils/permission.js` with:

```js
export function hasPermission(permissions, required) {
  if (!required) return true
  if (!Array.isArray(permissions)) return false
  return permissions.includes('*:*:*') || permissions.includes(required)
}

export function hasAnyPermission(permissions, requiredList) {
  if (!Array.isArray(requiredList) || requiredList.length === 0) return true
  return requiredList.some(required => hasPermission(permissions, required))
}
```

- [x] **Step 4: Verify permission tests pass**

Run:

```bash
cd frontend && npm run test:unit -- tests/unit/permission.spec.js
```

Expected: PASS.

- [x] **Step 5: Write failing tests for response normalization**

Create `frontend/tests/unit/request.spec.js` with:

```js
import { normalizeApiResponse } from '@/utils/request'

describe('request response normalization', () => {
  test('returns data when backend code is zero', () => {
    expect(normalizeApiResponse({ code: 0, message: 'success', data: { id: 1 } })).toEqual({ id: 1 })
  })

  test('throws backend message when code is non-zero', () => {
    expect(() => normalizeApiResponse({ code: 400, message: 'validation-error', data: null }))
      .toThrow('validation-error')
  })
})
```

- [x] **Step 6: Run request test to verify it fails**

Run:

```bash
cd frontend && npm run test:unit -- tests/unit/request.spec.js
```

Expected: FAIL because `normalizeApiResponse` is not implemented.

- [x] **Step 7: Implement auth storage and request utility**

Create `frontend/src/utils/auth.js` with:

```js
import Cookies from 'js-cookie'

const TokenKey = 'Liangyou-Token'

export function getToken() {
  return Cookies.get(TokenKey)
}

export function setToken(token) {
  return Cookies.set(TokenKey, token)
}

export function removeToken() {
  return Cookies.remove(TokenKey)
}
```

Create `frontend/src/utils/request.js` with:

```js
import axios from 'axios'
import { Message } from 'element-ui'
import { getToken, removeToken } from './auth'

export function normalizeApiResponse(response) {
  const res = response && response.code !== undefined ? response : response.data
  if (res.code === 0) {
    return res.data
  }
  throw new Error(res.message || 'request-error')
}

const service = axios.create({
  baseURL: process.env.VUE_APP_BASE_API,
  timeout: 15000
})

service.interceptors.request.use(config => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

service.interceptors.response.use(
  response => normalizeApiResponse(response.data),
  error => {
    const status = error.response && error.response.status
    const message = error.response && error.response.data && error.response.data.message

    if (status === 401) {
      removeToken()
      window.location.href = '/login'
      return Promise.reject(error)
    }

    Message.error(message || error.message || '请求失败')
    return Promise.reject(error)
  }
)

export default service
```

- [x] **Step 8: Verify request tests pass**

Run:

```bash
cd frontend && npm run test:unit -- tests/unit/request.spec.js
```

Expected: PASS.

- [x] **Step 9: Add auth API and Vuex user module**

Create `frontend/src/api/auth.js` with:

```js
import request from '@/utils/request'

export function login(data) {
  return request({
    url: '/v1/auth/login',
    method: 'post',
    data
  })
}

export function getCurrentUser() {
  return request({
    url: '/v1/auth/me',
    method: 'get'
  })
}

export function logout() {
  return request({
    url: '/v1/auth/logout',
    method: 'post'
  })
}
```

Create `frontend/src/store/modules/user.js` with:

```js
import { login, logout, getCurrentUser } from '@/api/auth'
import { getToken, setToken, removeToken } from '@/utils/auth'

const state = {
  token: getToken(),
  id: null,
  username: '',
  realName: '',
  roles: [],
  permissions: []
}

const mutations = {
  SET_TOKEN(state, token) {
    state.token = token
  },
  SET_USER(state, user) {
    state.id = user.id
    state.username = user.username
    state.realName = user.realName
    state.roles = user.roles || []
    state.permissions = user.permissions || []
  },
  RESET_USER(state) {
    state.token = ''
    state.id = null
    state.username = ''
    state.realName = ''
    state.roles = []
    state.permissions = []
  }
}

const actions = {
  async login({ commit }, form) {
    const data = await login(form)
    setToken(data.token)
    commit('SET_TOKEN', data.token)
    commit('SET_USER', data.userInfo)
  },
  async getInfo({ commit }) {
    const data = await getCurrentUser()
    commit('SET_USER', data)
    return data
  },
  async logout({ commit }) {
    try {
      await logout()
    } finally {
      removeToken()
      commit('RESET_USER')
    }
  }
}

export default {
  namespaced: true,
  state,
  mutations,
  actions
}
```

Create `frontend/src/store/modules/settings.js` with:

```js
const state = {
  sidebarOpened: true
}

const mutations = {
  TOGGLE_SIDEBAR(state) {
    state.sidebarOpened = !state.sidebarOpened
  }
}

export default {
  namespaced: true,
  state,
  mutations
}
```

Create `frontend/src/store/index.js` with:

```js
import Vue from 'vue'
import Vuex from 'vuex'
import user from './modules/user'
import settings from './modules/settings'

Vue.use(Vuex)

export default new Vuex.Store({
  modules: {
    user,
    settings
  }
})
```

- [x] **Step 10: Add permission directive**

Create `frontend/src/directive/permission/index.js` with:

```js
import store from '@/store'
import { hasAnyPermission } from '@/utils/permission'

function inserted(el, binding) {
  const value = binding.value
  const required = Array.isArray(value) ? value : [value]
  const permissions = store.state.user.permissions

  if (!hasAnyPermission(permissions, required)) {
    el.parentNode && el.parentNode.removeChild(el)
  }
}

export default {
  install(Vue) {
    Vue.directive('permission', { inserted })
  }
}
```

**Development Record - 2026-10-11 22:08 Asia/Shanghai**

- Added unit tests for permission matching and backend response normalization.
- Verified RED states:
  - `npm run test:unit -- tests/unit/permission.spec.js --runInBand` failed because `@/utils/permission` did not exist.
  - `npm run test:unit -- tests/unit/request.spec.js --runInBand` failed because `@/utils/request` did not exist.
- Implemented `src/utils/permission.js`, `src/utils/auth.js`, `src/utils/request.js`, `src/api/auth.js`, Vuex user/settings modules, and `v-permission`.
- Added Jest alias config for `@/` and Axios CJS mapping because Vue CLI 4 Jest cannot parse Axios 1.x ESM entry.
- Verification command: `npm run test:unit -- tests/unit/permission.spec.js tests/unit/request.spec.js --runInBand`.
- Result: 2 test suites passed, 5 tests passed.

## Task 3: Routing, Layout, and Login

**Files:**
- Create: `frontend/src/router/index.js`
- Create: `frontend/src/permission.js`
- Create: `frontend/src/layout/index.vue`
- Create: `frontend/src/layout/components/Sidebar.vue`
- Create: `frontend/src/layout/components/Navbar.vue`
- Create: `frontend/src/layout/components/AppMain.vue`
- Create: `frontend/src/views/login.vue`
- Create: `frontend/src/views/error/401.vue`
- Create: `frontend/src/views/error/404.vue`

- [x] **Step 1: Implement fixed routes**

Create `frontend/src/router/index.js` with route groups for login, dashboard, system, purchase, sale, inventory, statistics, basic placeholders, and errors. Use `meta.title`, `meta.icon`, and `meta.permission` fields. Use backend permission codes listed in this plan.

- [x] **Step 2: Implement route guard**

Create `frontend/src/permission.js` so unauthenticated users can only access `/login`, authenticated users recover `/auth/me` on refresh, and unauthorized route entries redirect to `/401`.

- [x] **Step 3: Implement layout**

Create the layout files with a left sidebar, top navbar, logout button, and content area. Sidebar reads business routes from `router.options.routes` and hides routes with `hidden: true`.

- [x] **Step 4: Implement login page**

Create `frontend/src/views/login.vue` with Element UI form fields `username` and `password`, validation for both fields, and submit action `store.dispatch('user/login', form)`.

- [x] **Step 5: Run the app**

Run:

```bash
cd frontend && npm run serve
```

Expected: dev server starts on `http://localhost:8081`, `/login` renders, and navigating to protected pages redirects to `/login` without token.

**Development Record - 2026-10-11 22:07 Asia/Shanghai**

- Implemented fixed route table, auth guard, RuoYi-style shell layout, sidebar, navbar, app main outlet, login page, and 401/404 pages.
- Business routes temporarily use an internal placeholder component until their pages are implemented in later tasks.
- Verification commands:
  - `npm run lint -- --no-fix`
  - `npm run build`
- Result: lint exited with code 0. Build exited with code 0 and generated `frontend/dist/`.
- Note: build reported vendor asset size warnings caused by Vue 2 + Element UI bundle size; no functional build error.

## Task 4: API Modules

**Files:**
- Create: `frontend/src/api/system/user.js`
- Create: `frontend/src/api/system/role.js`
- Create: `frontend/src/api/system/menu.js`
- Create: `frontend/src/api/purchase/purchaseIn.js`
- Create: `frontend/src/api/sale/saleOut.js`
- Create: `frontend/src/api/inventory/inventory.js`
- Create: `frontend/src/api/statistics/statistics.js`

- [x] **Step 1: Implement system API wrappers**

Use these exact endpoint mappings:

```js
// users
GET /v1/users
GET /v1/users/{id}
POST /v1/users
PUT /v1/users/{id}
DELETE /v1/users/{id}
POST /v1/users/{id}/roles
POST /v1/users/{id}/permissions
GET /v1/users/{id}/permissions

// roles
GET /v1/roles
POST /v1/roles
PUT /v1/roles/{id}
DELETE /v1/roles/{id}
POST /v1/roles/{id}/permissions

// menus
GET /v1/menus/tree
```

- [x] **Step 2: Implement business API wrappers**

Use these exact endpoint mappings:

```js
// purchase
GET /v1/purchase-ins
GET /v1/purchase-ins/{id}
POST /v1/purchase-ins
PUT /v1/purchase-ins/{id}
DELETE /v1/purchase-ins/{id}

// sale
GET /v1/sale-outs
GET /v1/sale-outs/{id}
POST /v1/sale-outs
PUT /v1/sale-outs/{id}
DELETE /v1/sale-outs/{id}

// inventory
GET /v1/inventories
GET /v1/inventories/{productId}/batches
GET /v1/inventories/flows
PUT /v1/inventories/{id}
DELETE /v1/inventories/{id}

// statistics
GET /v1/statistics/daily/inbound-outbound
GET /v1/statistics/monthly/inbound-outbound
GET /v1/statistics/amount-by-customer
GET /v1/statistics/amount-by-supplier
GET /v1/statistics/amount-by-category
```

- [x] **Step 3: Run lint**

Run:

```bash
cd frontend && npm run lint
```

Expected: no lint errors in API modules.

**Development Record - 2026-10-11 22:09 Asia/Shanghai**

- Added API wrapper modules for auth-adjacent system APIs, purchase, sale, inventory, and statistics.
- Endpoint paths use `/v1/**` because Axios base URL is `/api`, matching backend `/api/v1/**`.
- Verification command: `npm run lint -- --no-fix`.
- Result: lint exited with code 0.

## Task 5: Shared Admin Components

**Files:**
- Create: `frontend/src/components/Pagination/index.vue`
- Create: `frontend/src/components/RightToolbar/index.vue`
- Create: `frontend/src/components/DictTag/index.vue`

- [x] **Step 1: Implement Pagination**

Build a wrapper around `el-pagination` that accepts `total`, `page`, `limit`, emits `update:page`, `update:limit`, and `pagination`, and uses backend names `pageNum` and `pageSize` in parent pages.

- [x] **Step 2: Implement RightToolbar**

Build a compact toolbar with refresh and column-toggle affordances. In phase one, emit `queryTable` on refresh and keep column configuration local to pages.

- [x] **Step 3: Implement DictTag**

Build a simple status tag component where `1` renders `启用`/success, `0` renders `停用`/info, and unknown values render the raw value.

**Development Record - 2026-10-11 22:10 Asia/Shanghai**

- Added shared `Pagination`, `RightToolbar`, and `DictTag` components.
- Verification command: `npm run lint -- --no-fix`.
- Result: lint exited with code 0.

## Task 6: System Pages

**Files:**
- Create: `frontend/src/views/system/user/index.vue`
- Create: `frontend/src/views/system/role/index.vue`
- Create: `frontend/src/views/system/menu/index.vue`

- [x] **Step 1: Implement user management**

Implement query form, paginated table, create/edit dialog, delete confirmation, role assignment dialog, and permission assignment dialog. Use numeric `warehouseId` input because warehouse lookup API does not exist.

- [x] **Step 2: Implement role management**

Implement query form, non-paginated table, create/edit dialog, delete confirmation, and permission assignment dialog using `permissionIds`.

- [x] **Step 3: Implement menu tree**

Implement read-only `el-tree` for `/v1/menus/tree`. Show an empty state when the backend returns no visible menus.

**Development Record - 2026-10-11 22:15 Asia/Shanghai**

- Implemented user management with pagination, create/edit/delete, role assignment, and user direct permission assignment.
- Implemented role management with non-paginated table, create/edit/delete, and permission assignment.
- Implemented read-only menu tree backed by `/v1/menus/tree`.

## Task 7: Purchase and Sale Pages

**Files:**
- Create: `frontend/src/views/purchase/in/index.vue`
- Create: `frontend/src/views/purchase/in/detail.vue`
- Create: `frontend/src/views/sale/out/index.vue`
- Create: `frontend/src/views/sale/out/detail.vue`

- [x] **Step 1: Implement purchase list and editor**

Implement query form, table, create/edit dialog with main fields and editable item rows. Required item fields are `productId`, `unitId`, `qty`, `unitPrice`, `amount`, `batchNo`, and `inboundTime`.

- [x] **Step 2: Implement purchase detail**

Implement route `/purchase/in/detail/:id`, fetch `/v1/purchase-ins/{id}`, and render main fields plus item table.

- [x] **Step 3: Implement sale list and editor**

Implement query form, table, create/edit dialog with main fields and editable item rows. Required item fields are `productId`, `unitId`, `qty`, `unitPrice`, and `amount`. Do not include batch selection; backend performs FIFO deduction.

- [x] **Step 4: Implement sale detail**

Implement route `/sale/out/detail/:id`, fetch `/v1/sale-outs/{id}`, and render main fields plus item table.

**Development Record - 2026-10-11 22:15 Asia/Shanghai**

- Implemented purchase list/editor/detail pages using main form plus editable item rows.
- Implemented sale list/editor/detail pages using main form plus editable item rows.
- Kept product, unit, warehouse, supplier, and customer as numeric ID fields because lookup APIs are not available.

## Task 8: Inventory Pages

**Files:**
- Create: `frontend/src/views/inventory/stock/index.vue`
- Create: `frontend/src/views/inventory/batch/index.vue`
- Create: `frontend/src/views/inventory/flow/index.vue`

- [x] **Step 1: Implement stock page**

Implement warehouse/product filters, paginated table, adjustment dialog for `totalQty`, `availableQty`, `remark`, and delete confirmation.

- [x] **Step 2: Implement batch page**

Implement `productId` as required query input and optional `warehouseId`, then call `/v1/inventories/{productId}/batches`.

- [x] **Step 3: Implement flow page**

Implement filters for `warehouseId`, `productId`, `bizTimeFrom`, and `bizTimeTo`, then render flow rows from `/v1/inventories/flows`.

**Development Record - 2026-10-11 22:15 Asia/Shanghai**

- Implemented stock list with adjustment dialog, batch jump, and delete action.
- Implemented batch inventory query with required `productId` and optional `warehouseId`.
- Implemented inventory flow query with warehouse/product/time filters.

## Task 9: Dashboard and Statistics

**Files:**
- Create: `frontend/src/views/dashboard/index.vue`
- Create: `frontend/src/views/statistics/daily.vue`
- Create: `frontend/src/views/statistics/monthly.vue`
- Create: `frontend/src/views/statistics/customer.vue`
- Create: `frontend/src/views/statistics/supplier.vue`
- Create: `frontend/src/views/statistics/category.vue`

- [x] **Step 1: Implement dashboard**

Use existing statistics endpoints for recent trend charts. Use inventory page total from `/v1/inventories?pageNum=1&pageSize=1` as current stock SKU count. Render stock warning as `待后端接口补齐`.

- [x] **Step 2: Implement statistics pages**

Each page has filters matching `StatisticsQueryRequest`, an Element UI table, and an ECharts chart. `daily` uses `statDate`; `monthly` and amount summary pages use `key`.

**Development Record - 2026-10-11 22:15 Asia/Shanghai**

- Implemented dashboard cards, trend chart, and shortcut buttons using existing statistics/inventory APIs.
- Implemented daily, monthly, customer, supplier, and category statistics pages with shared chart/table component.

## Task 10: Basic Placeholder and Final Verification

**Files:**
- Create: `frontend/src/views/basic/placeholder.vue`

- [x] **Step 1: Implement placeholder page**

Create one reusable page that displays the selected basic-data module name and explains that the backend Controller is not implemented yet.

- [x] **Step 2: Run unit tests**

Run:

```bash
cd frontend && npm run test:unit
```

Expected: all unit tests pass.

- [x] **Step 3: Run production build**

Run:

```bash
cd frontend && npm run build
```

Expected: build exits with code 0 and writes `frontend/dist/`.

- [ ] **Step 4: Manual smoke test**

Start backend and frontend:

```bash
mvn spring-boot:run
cd frontend && npm run serve
```

Check:

- `/login` renders.
- Login calls `/api/v1/auth/login`.
- Refresh calls `/api/v1/auth/me`.
- Sidebar renders fixed menus.
- User, role, menu, purchase, sale, inventory, and statistics pages load without frontend runtime errors.
- Non-implemented basic-data pages show placeholder text.

**Development Record - 2026-10-11 22:15 Asia/Shanghai**

- Implemented reusable basic-data placeholder page.
- Updated router to point to real pages instead of temporary route placeholders.
- Verification commands:
  - `npm run lint -- --no-fix`
  - `npm run test:unit -- --runInBand`
  - `npm run build`
- Results:
  - Lint exited with code 0.
  - Unit tests passed: 2 suites, 5 tests.
  - Production build exited with code 0 and generated `frontend/dist/`.
- Note: Manual backend-connected smoke test was not run because the backend server was not started in this session. Build-level route/component integration is verified.
- Note: build still reports bundle size warnings for ECharts/Element UI/vendor chunks; this does not block deployment.
- Dev server check:
  - First `npm run serve -- --host 0.0.0.0` compiled successfully but crashed after compile due to Vue CLI 4 / Chokidar / fsevents incompatibility on Node 20.
  - Added `CHOKIDAR_USEPOLLING=true` to `.env.development` and `devServer.watchOptions.poll = 1000` to `vue.config.js`.
  - Re-ran `npm run serve -- --host 0.0.0.0`.
  - Result: frontend dev server is running at `http://localhost:8081/` and `http://192.168.2.19:8081/`.

## Self-Review

- Spec coverage: The plan covers scaffold, auth, request adaptation, fixed menu routing, permission buttons, system pages, purchase, sale, inventory, statistics, dashboard, placeholders, and final verification.
- Known scope limit: It does not implement real product/warehouse/supplier/customer/unit selectors because the backend Controllers are missing.
- Backend alignment: Endpoint paths and permission codes were checked against the current Controller files.
- Placeholder scan: No implementation step depends on unconfirmed backend APIs.
