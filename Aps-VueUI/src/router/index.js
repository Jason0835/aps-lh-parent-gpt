import Vue from "vue";
import Router from "vue-router";

Vue.use(Router);

/* Layout */
import Layout from "@/layout";

/**
 * Note: 路由配置项
 *
 * hidden: true                     // 当设置 true 的时候该路由不会再侧边栏出现 如401，login等页面，或者如一些编辑页面/edit/1
 * alwaysShow: true                 // 当你一个路由下面的 children 声明的路由大于1个时，自动会变成嵌套的模式--如组件页面
 *                                  // 只有一个时，会将那个子路由当做根路由显示在侧边栏--如引导页面
 *                                  // 若你想不管路由下面的 children 声明的个数都显示你的根路由
 *                                  // 你可以设置 alwaysShow: true，这样它就会忽略之前定义的规则，一直显示根路由
 * redirect: noRedirect             // 当设置 noRedirect 的时候该路由在面包屑导航中不可被点击
 * name:'router-name'               // 设定路由的名字，一定要填写不然使用<keep-alive>时会出现各种问题
 * query: '{"id": 1, "name": "ry"}' // 访问路由的默认传递参数
 * roles: ['admin', 'common']       // 访问路由的角色权限
 * permissions: ['a:a:a', 'b:b:b']  // 访问路由的菜单权限
 * meta : {
    noCache: true                   // 如果设置为true，则不会被 <keep-alive> 缓存(默认 false)
    title: 'title'                  // 设置该路由在侧边栏和面包屑中展示的名字
    icon: 'svg-name'                // 设置该路由的图标，对应路径src/assets/icons/svg
    breadcrumb: false               // 如果设置为false，则不会在breadcrumb面包屑中显示
    activeMenu: '/system/user'      // 当路由设置了该属性，则会高亮相对应的侧边栏。
  }
 */

// 公共路由
export const constantRoutes = [
  {
    path: "/redirect",
    component: Layout,
    hidden: true,
    children: [
      {
        path: "/redirect/:path(.*)",
        component: () => import("@/views/redirect"),
      },
    ],
  },
  {
    path: "/login",
    component: () => import("@/views/login"),
    hidden: true,
  },
  {
    path: "/register",
    component: () => import("@/views/register"),
    hidden: true,
  },
  {
    path: "/404",
    component: () => import("@/views/error/404"),
    hidden: true,
  },
  {
    path: "/401",
    component: () => import("@/views/error/401"),
    hidden: true,
  },

  {
    path: "",
    component: Layout,
    redirect: "index",
    children: [
      {
        path: "index",
        component: () => import("@/views/index"),
        name: "Index",
        meta: { icon: "dashboard", affix: true, i18n: "tabTitle.mainPage" },
      },
    ],
  },

  {
    path: "/user",
    component: Layout,
    hidden: true,
    redirect: "noredirect",
    children: [
      {
        path: "profile",
        component: () => import("@/views/system/user/profile/index"),
        name: "Profile",
        meta: { icon: "user", i18n: "tabTitle.personalCenter" },
      },
    ],
  },
];
if (process.env.NODE_ENV == "development") {

}

constantRoutes.push(
  {
    path: "/monthPlanManagement",
    component: Layout,
    hidden: false,
    redirect: "noredirect",
    name: "",
    meta: { title: "月计划管理", icon: "table" },
    children: [
      {
        path: "monthlyProductionPlan",
        component: () => import("@/views/monthPlanManagement/monthlyProductionPlan/index"),
        name: "monthlyProductionPlan",
        meta: { title: "月度生产计划" },
      },
      {
        path: "monthlyPlanGantChart",
        component: () => import("@/views/monthPlanManagement/monthlyProductionPlan/monthlyPlanGantChart"),
        name: "monthlyPlanGantChart",
        hidden: true,
        meta: { title: "月计划甘特图" },
      },
      {
        path: "dailyCurveChart",
        component: () => import("@/views/monthPlanManagement/monthlyProductionPlan/dailyCurveChart"),
        name: "dailyCurveChart",
        hidden: true,
        meta: { title: "日产量曲线图" },
      },
      {
        path: "checkConstruction",
        component: () => import("@/views/monthPlanManagement/checkConstruction/index"),
        name: "checkConstruction",
        meta: { title: "施工信息检测" },
      },
      {
        path: "reportStatistics",
        component: () => import("@/views/monthPlanManagement/reportStatistics/index"),
        name: "reportStatistics",
        meta: { title: "每日报表统计" },
      },
      {
        path: "reportClassAccuracy",
        component: () => import("@/views/monthPlanManagement/reportClassAccuracy/index"),
        name: "reportClassAccuracy",
        meta: { title: "班次完成统计报表" },
      },
      {
        path: "reportOrderStatistics",
        component: () => import("@/views/monthPlanManagement/reportOrderStatistics/index"),
        name: "reportOrderStatistics",
        meta: { title: "每日各工序工单完成情况统计" },
      },
    ],
  },
  {
    path: "/moldingPlanManagement",
    component: Layout,
    hidden: false,
    redirect: "noredirect",
    name: "",
    meta: { title: "成型计划管理", icon: "table" },
    children: [
      {
        path: "moldingSchedule",
        component: () => import("@/views/molding/moldingSchedule/index"),
        name: "moldingSchedule",
        meta: { title: "成型排程管理" },
      },
      {
        path: "productStatus",
        component: () => import("@/views/molding/productStatus/index"),
        name: "moldingSchedule",
        meta: { title: "投产列表" },
        hidden: true,
      },
      {
        path: "finished",
        component: () => import("@/views/molding/moldingSchedule/finished"),
        name: "finished",
        meta: { title: "收尾列表" },
        hidden: true,
      },
      {
        path: "lastDaySupplyPlan",
        component: () => import("@/views/molding/moldingSchedule/lastDaySupplyPlan"),
        name: "lastDaySupplyPlan",
        meta: { title: "前日增补计划" },
        hidden: true,
      },
      {
        path: "machine",
        component: () => import("@/views/molding/machine/index"),
        name: "machine",
        meta: { title: "成型机台管理" },
      },
      {
        path: "pointingMachine",
        component: () => import("@/views/molding/pointingMachine/index"),
        name: "pointingMachine",
        meta: { title: "成型定点机台管理" },
      },
      {
        path: "quotaMachine",
        component: () => import("@/views/molding/quotaMachine/index"),
        name: "quotaMachine",
        meta: { title: "成型定额设备管理" },
      },
      {
        path: "badNumber",
        component: () => import("@/views/molding/badNumber/index"),
        name: "badNumber",
        meta: { title: "SAP导入不良数" },
      },
      {
        path: "dispatcherLog",
        component: () => import("@/views/molding/dispatcherLog/index"),
        name: "dispatcherLog",
        meta: { title: "成型排程操作日志" },
      },
    ],
  },
  {
    path: "/curingPlan",
    component: Layout,
    hidden: false,
    redirect: "noredirect",
    name: "",
    meta: { title: "硫化计划管理", icon: "table" },
    children: [
      {
        path: "curingApsMoldAdjust",
        component: () => import("@/views/curingPlan/curingApsMoldAdjust/index"),
        name: "curingApsMoldAdjust",
        meta: { title: "APS模具变动单" },
      },
      {
        path: "curingSchedule",
        component: () => import("@/views/curingPlan/curingSchedule/index"),
        name: "curingSchedule",
        meta: { title: "硫化排程管理" },
      },
      {
        path: "machineGantChart",
        component: () => import("@/views/curingPlan/curingSchedule/machineGantChart"),
        name: "machineGantChart",
        hidden: true,
        meta: { title: "机台甘特图" },
      },
      {
        path: "specDescGantChart",
        component: () => import("@/views/curingPlan/curingSchedule/specDescGantChart"),
        name: "specDescGantChart",
        hidden: true,
        meta: { title: "规格甘特图" },
      },
      {
        path: "curingMachine",
        component: () => import("@/views/curingPlan/curingMachine/index"),
        name: "curingMachine",
        meta: { title: "硫化机台管理" },
      },
    ],
  },


  {
    path: "/system/user-auth",
    component: Layout,
    hidden: true,
    permissions: ["system:user:edit"],
    children: [
      {
        path: "role/:userId(\\d+)",
        component: () => import("@/views/system/user/authRole"),
        name: "AuthRole",
        meta: { title: "分配角色", activeMenu: "/system/user", i18n: "common.api.user.columnname.assignRole" },
      },
    ],
  },
  {
    path: "/system/role-auth",
    component: Layout,
    hidden: true,
    permissions: ["system:role:edit"],
    children: [
      {
        path: "user/:roleId(\\d+)",
        component: () => import("@/views/system/role/authUser"),
        name: "AuthUser",
        meta: { title: "分配用户", activeMenu: "/system/role", i18n: "common.api.role.columnname.assignUsers" },
      },
    ],
  },
  {
    path: "/system/dict-data",
    component: Layout,
    hidden: true,
    permissions: ["system:dict:list"],
    children: [
      {
        path: "index/:dictId(\\d+)",
        component: () => import("@/views/system/dict/data"),
        name: "Data",
        meta: { title: "字典数据", activeMenu: "/system/dict", i18n: "common.system.title.dictdata" },
      },
    ],
  },
  {
    path: "/monitor/job-log",
    component: Layout,
    hidden: true,
    permissions: ["monitor:job:list"],
    children: [
      {
        path: "index/:jobId(\\d+)",
        component: () => import("@/views/monitor/job/log"),
        name: "JobLog",
        meta: { title: "调度日志", activeMenu: "/monitor/job", i18n: "common.job.button.jobLog" },
      },
    ],
  },
  {
    path: "/tool/gen-edit",
    component: Layout,
    hidden: true,
    permissions: ["tool:gen:edit"],
    children: [
      {
        path: "index/:tableId(\\d+)",
        component: () => import("@/views/tool/gen/editTable"),
        name: "GenEdit",
        meta: { title: "修改生成配置", activeMenu: "/tool/gen" },
      },
    ],
  },
)

// 动态路由，基于用户权限动态去加载
export const dynamicRoutes = [
  // {
  //   path: "/system/user-auth",
  //   component: Layout,
  //   hidden: true,
  //   permissions: ["system:user:edit"],
  //   children: [
  //     {
  //       path: "role/:userId(\\d+)",
  //       component: () => import("@/views/system/user/authRole"),
  //       name: "AuthRole",
  //       meta: { title: "分配角色", activeMenu: "/system/user", i18n: "common.api.user.columnname.assignRole" },
  //     },
  //   ],
  // },
  // {
  //   path: "/system/role-auth",
  //   component: Layout,
  //   hidden: true,
  //   permissions: ["system:role:edit"],
  //   children: [
  //     {
  //       path: "user/:roleId(\\d+)",
  //       component: () => import("@/views/system/role/authUser"),
  //       name: "AuthUser",
  //       meta: { title: "分配用户", activeMenu: "/system/role", i18n: "common.api.role.columnname.assignUsers" },
  //     },
  //   ],
  // },
  // {
  //   path: "/system/dict-data",
  //   component: Layout,
  //   hidden: true,
  //   permissions: ["system:dict:list"],
  //   children: [
  //     {
  //       path: "index/:dictId(\\d+)",
  //       component: () => import("@/views/system/dict/data"),
  //       name: "Data",
  //       meta: { title: "字典数据", activeMenu: "/system/dict", i18n: "common.system.title.dictdata" },
  //     },
  //   ],
  // },
  // {
  //   path: "/monitor/job-log",
  //   component: Layout,
  //   hidden: true,
  //   permissions: ["monitor:job:list"],
  //   children: [
  //     {
  //       path: "index/:jobId(\\d+)",
  //       component: () => import("@/views/monitor/job/log"),
  //       name: "JobLog",
  //       meta: { title: "调度日志", activeMenu: "/monitor/job", i18n: "common.job.button.jobLog" },
  //     },
  //   ],
  // },
  // {
  //   path: "/tool/gen-edit",
  //   component: Layout,
  //   hidden: true,
  //   permissions: ["tool:gen:edit"],
  //   children: [
  //     {
  //       path: "index/:tableId(\\d+)",
  //       component: () => import("@/views/tool/gen/editTable"),
  //       name: "GenEdit",
  //       meta: { title: "修改生成配置", activeMenu: "/tool/gen" },
  //     },
  //   ],
  // },

];

// 防止连续点击多次路由报错
let routerPush = Router.prototype.push;
let routerReplace = Router.prototype.replace;
// push
Router.prototype.push = function push(location) {
  return routerPush.call(this, location).catch((err) => err);
};
// replace
Router.prototype.replace = function push(location) {
  return routerReplace.call(this, location).catch((err) => err);
};

export default new Router({
  mode: "history", // 去掉url中的#
  base: process.env.BASE_URL,
  scrollBehavior: (to, from, savedPosition) => {
    if (savedPosition) {
      // 如果页面已经有了滚动位置，则保持它
      return savedPosition;
    } else {
      // 如果没有保存位置，则返回顶部
      return { x: 0, y: 0 };
    }
  },
  routes: constantRoutes,
});
