import router from './router'
import store from './store'
import i18n from './lang'
// import { Message } from 'element-ui'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
// import { getToken } from '@/utils/auth'
import { isRelogin } from '@/utils/request'
import { loadLanguageAsync } from '@/lang/i18n-setup'

NProgress.configure({ showSpinner: false })

const whiteList = [
  '/login',
  '/register',
  '/largescreen/home',
  '/largescreen/monthPlan',
  '/largescreen/vulcanization',
  '/largescreen/semiComponent',
  '/components/table/basic',
  '/components/table/state',
  '/components/form/basic'
]

router.beforeEach(async (to, from, next) => {
  NProgress.start()
  // 监听路由变化
  const langParam = to.query.lang
  // if (langParam && langParam !== store.getters.language) {
  //   // 根据 URL 参数设置语言
  //   i18n.locale = langParam
  //   store.dispatch('app/setLanguage', langParam)
  //   router.go(0)
  // }
  // 如果切换了语言，需要刷新页面
  const changeLang = await loadLanguageAsync(langParam)
  if (changeLang) {
    // router.go(0)
    next()
  }

  if (whiteList.indexOf(to.path) !== -1) {
    // 在免登录白名单，直接进入
    next()
    return
  }
  if (!store.state.app?.menus?.length) {
    store
      .dispatch('GetInfo')
      .then(() => {
        isRelogin.show = false
        store.dispatch('GenerateRoutes').then((accessRoutes) => {
          // 根据roles权限生成可访问的路由表
          router.addRoutes(accessRoutes) // 动态添加可访问路由表
          next({ ...to, replace: true }) // hack方法 确保addRoutes已完成
        })
        store.dispatch('globalList/fetchGlobalList')
      })
      .catch((err) => {
        console.error(err)
        next({ path: '/login' })
      })
  } else {
    next()
  }
  // if (getToken()) {
  //   to.meta.title && store.dispatch('settings/setTitle', to.meta.title)
  //   /* has token*/
  //   if (to.path === '/login') {
  //     next({ path: '/' })
  //     NProgress.done()
  //   } else {
  //     if (store.getters.roles.length === 0) {
  //       isRelogin.show = true
  //       // 判断当前用户是否已拉取完user_info信息
  //       store.dispatch('GetInfo').then(() => {
  //         isRelogin.show = false
  //         store.dispatch('GenerateRoutes').then(accessRoutes => {
  //           // 根据roles权限生成可访问的路由表
  //           router.addRoutes(accessRoutes) // 动态添加可访问路由表
  //           next({ ...to, replace: true }) // hack方法 确保addRoutes已完成
  //         })
  //       }).catch(err => {
  //           store.dispatch('LogOut').then(() => {
  //             Message.error(err)
  //             next({ path: '/' })
  //           })
  //         })
  //     } else {
  //       next()
  //     }
  //   }
  // } else {
  //   // 没有token
  //   if (whiteList.indexOf(to.path) !== -1) {
  //     // 在免登录白名单，直接进入
  //     next()
  //   } else {
  //     next(`/login?redirect=${encodeURIComponent(to.fullPath)}`) // 否则全部重定向到登录页
  //     NProgress.done()
  //   }
  // }
})

router.afterEach(() => {
  NProgress.done()
})
