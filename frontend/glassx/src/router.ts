import { createRouter, createWebHistory } from "vue-router"
import type { RouteRecordRaw } from "vue-router"
import { sessionService } from "@/services/session"
import i18n from "@/i18n"

const routes: RouteRecordRaw[] = [
  {
    path: "/",
    name: "Home",
    component: () => import("./pages/Home.vue"),
    meta: { title: "home.title" },
  },
  {
    path: "/register",
    name: "Register",
    component: () => import("./pages/Register.vue"),
    meta: { title: "register.title" },
  },
  {
    path: "/login",
    name: "Login",
    component: () => import("./pages/Login.vue"),
    meta: { title: "login.title" },
  },
  {
    path: "/forgot-password",
    name: "ForgotPassword",
    component: () => import("./pages/ForgotPassword.vue"),
    meta: { title: "forgot_password.title" },
  },
  {
    path: "/dashboard",
    name: "Dashboard",
    component: () => import("./pages/Dashboard.vue"),
    meta: { title: "dashboard.title", requiresAuth: true },
  },
  {
    path: "/admin",
    redirect: "/dashboard",
    meta: { requiresAdmin: true },
  },
  {
    path: "/status",
    redirect: "/dashboard",
  },
  {
    path: "/404",
    name: "NotFound",
    component: () => import("./pages/NotFound.vue"),
    meta: { title: "errors.404.title" },
  },
  {
    path: "/500",
    name: "ServerError",
    component: () => import("./pages/ServerError.vue"),
    meta: { title: "errors.500.title" },
  },
  {
    path: "/:pathMatch(.*)*",
    redirect: "/404",
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) {
      return savedPosition
    } else {
      return { top: 0 }
    }
  },
})

// 设置页面标题
const setPageTitle = (titleKey?: string) => {
  const baseTitle = "VerifyMC"
  if (titleKey) {
    try {
      const translatedTitle = i18n.global.t(titleKey)
      document.title = `${translatedTitle} | ${baseTitle}`
    } catch {
      document.title = baseTitle
    }
  } else {
    document.title = baseTitle
  }
}

// Navigation guards
router.beforeEach((to, from, next) => {
  // 设置页面标题
  setPageTitle(to.meta.title as string | undefined)

  const authenticated = sessionService.isAuthenticated()
  const isAdmin = sessionService.isAdmin()

  // 已登录用户访问登录页，重定向到 dashboard
  if (to.path === '/login' && authenticated) {
    const redirect = typeof to.query.redirect === 'string' ? to.query.redirect : '/dashboard'
    next(redirect)
    return
  }

  // 需要认证但未登录
  if (to.meta.requiresAuth && !authenticated) {
    next({
      path: '/login',
      query: { redirect: to.fullPath },
    })
    return
  }

  // 需要管理员权限但不是管理员
  if (to.meta.requiresAdmin && !isAdmin) {
    next({ path: '/dashboard' })
    return
  }

  next()
})

export default router
