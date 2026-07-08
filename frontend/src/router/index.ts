import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      redirect: '/login',
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
      meta: { public: true },
    },
    {
      path: '/user',
      component: () => import('@/layouts/UserLayout.vue'),
      meta: { role: 'USER' },
      children: [
        { path: '', redirect: '/user/home' },
        { path: 'home', name: 'user-home', component: () => import('@/views/user/HomeView.vue') },
        {
          path: 'profile',
          name: 'user-profile',
          component: () => import('@/views/user/ProfileView.vue'),
        },
        {
          path: 'recommend',
          name: 'user-recommend',
          component: () => import('@/views/user/RecommendView.vue'),
        },
        {
          path: 'recommend/result',
          name: 'recommend-result',
          component: () => import('@/views/user/RecommendationHistoryView.vue'),
        },
        {
          path: 'recipes/:id',
          name: 'user-recipe-detail',
          component: () => import('@/views/user/RecipeDetailView.vue'),
        },
        {
          path: 'shopping-lists',
          name: 'shopping-lists',
          component: () => import('@/views/user/ShoppingListsView.vue'),
        },
        {
          path: 'favorites',
          name: 'favorites',
          component: () => import('@/views/user/FavoritesView.vue'),
        },
        {
          path: 'history',
          name: 'history',
          component: () => import('@/views/user/RecommendationHistoryView.vue'),
        },
      ],
    },
    {
      path: '/admin',
      component: () => import('@/layouts/AdminLayout.vue'),
      meta: { role: 'MAINTAINER' },
      children: [
        { path: '', redirect: '/admin/dashboard' },
        {
          path: 'dashboard',
          name: 'admin-dashboard',
          component: () => import('@/views/admin/DashboardView.vue'),
        },
        { path: 'recipes', name: 'admin-recipes', component: () => import('@/views/admin/RecipesView.vue') },
        {
          path: 'ingredients',
          name: 'admin-ingredients',
          component: () => import('@/views/admin/IngredientsView.vue'),
        },
      ],
    },
  ],
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  const requiredRole = to.meta.role
  await auth.initSession()

  if (to.meta.public) {
    if (auth.role === 'USER') return '/user/home'
    if (auth.role === 'MAINTAINER') return '/admin/dashboard'
    return true
  }

  if (!auth.isLoggedIn) return '/login'
  if (requiredRole && auth.role !== requiredRole) {
    return auth.role === 'MAINTAINER' ? '/admin/dashboard' : '/user/home'
  }

  return true
})

export default router
