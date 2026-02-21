import { createRouter, createWebHistory } from "vue-router"
import type { RouteRecordRaw } from "vue-router"
import { sessionService } from "@/services/session"

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
    path: "/dashboard",
    name: "Dashboard",
    component: () => import("./pages/Dashboard.vue"),
    meta: { title: "dashboard.title", requiresAuth: true },
  },
  {
    path: "/admin",
    redirect: "/dashboard",
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

// Navigation guards
router.beforeEach((to, from, next) => {
  const authenticated = sessionService.isAuthenticated()

  if (to.path === '/login' && authenticated) {
    // Redirect authenticated users to dashboard
    const redirect = typeof to.query.redirect === 'string' ? to.query.redirect : '/dashboard'
    next(redirect)
    return
  }

  if (to.meta.requiresAuth && !authenticated) {
    next({
      path: '/login',
      query: { redirect: to.fullPath },
    })
    return
  }

  next()
})

export default router
