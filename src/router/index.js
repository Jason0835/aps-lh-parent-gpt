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
  // TODO 大屏
  {
    path: "/largescreen/home",
    component: () => import("@/views/largescreen/home"),
    name: 'largescreenIndex',
    hidden: true,
  },
  {
    path: "/largescreen/vulcanization",
    component: () => import("@/views/largescreen/vulcanization"),
    name: 'largescreenIndex',
    hidden: true,
  },
  {
    path: "/largescreen/monthPlan",
    component: () => import("@/views/largescreen/monthPlan"),
    name: 'largescreenIndex',
    hidden: true,
  },
  {
    path: "/largescreen/semiComponent",
    component: () => import("@/views/largescreen/semiComponent"),
    name: 'largescreenIndex',
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
  // {
  //   path: "/new",
  //   component: Layout,
  //   hidden: false,
  //   redirect: "noredirect",
  //   meta: { title: "管理" },
  //   children: [
  //      {
  //       path: "messageTemplate",
  //       component: () => import("@/views/newPage/messageTemplate/index"),
  //       name: "messageTemplate",
  //       meta: { title: "消息模板" },
  //     },
  //     // {
  //     //   path: "rollingCycle",
  //     //   component: () => import("@/views/newPage/rollingCycle/index"),
  //     //   name: "rollingCycle",
  //     //   meta: { title: "周程滚动调整" },
  //     // },
  //     {
  //       path: "monthlyProductionPlan",
  //       component: () => import("@/views/newPage/monthlyProductionPlan/index"),
  //       name: "monthlyProductionPlan",
  //       meta: { title: "月度生产计划" },
  //     },
  //     {
  //       path: "rollingCycleResult",
  //       component: () => import("@/views/newPage/rollingCycle/components/result.vue"),
  //       hidden: true,
  //       name: "rollingCycleResult",
  //       meta: { title: "调整结果" },
  //     },
  //     {
  //       path: "belowMinimumProduction",
  //       component: () => import("@/views/report/belowMinimumProduction/index"),
  //       name: "BelowMinimumProduction",
  //       meta: { title: "小于最小投产量报表" },
  //     },
  //     // {
  //     //   path: "skuInventoryWriteDown",
  //     //   component: () => import("@/views/report/skuInventoryWriteDown/index"),
  //     //   name: "SkuInventoryWriteDown",
  //     //   meta: { title: "SKU层面的库存冲减报表" },
  //     // },
  //     // {
  //     //   path: "orderInventoryWriteDown",
  //     //   component: () => import("@/views/report/orderInventoryWriteDown/index"),
  //     //   name: "OrderInventoryWriteDown",
  //     //   meta: { title: "订单库存冲减" }, //订单层面的库存冲减报表
  //     // },
  //     {
  //       path: "skuWriteDownReplaceGroup",
  //       component: () => import("@/views/report/skuWriteDownReplaceGroup/index"),
  //       name: "SkuWriteDownReplaceGroup",
  //       meta: { title: "SKU冲减替换组报表" },
  //     },
  //     {
  //       path: "unfulfilledOrder",
  //       component: () => import("@/views/report/unfulfilledOrder/index"),
  //       name: "UnfulfilledOrder",
  //       meta: { title: "整单未满足提示报表" },
  //     },
  //     {
  //       path: "weeklyProductionVariance",
  //       component: () => import("@/views/report/weeklyProductionVariance/index"),
  //       name: "WeeklyProductionVariance",
  //       meta: { title: "周度呈报超欠产报表" },
  //     },
  //     // {
  //     //   path: "regionalCapacityAllocation",
  //     //   component: () => import("@/views/monthlydata/regionalCapacityAllocation/index"),
  //     //   name: "RegionalCapacityAllocation",
  //     //   meta: { title: "区域产能配置" },
  //     // },
  //     // {
  //     //   path: "orderPool",
  //     //   component: () => import("@/views/newPage/orderPool/index"),
  //     //   name: "orderPool",
  //     //   meta: { title: "销售订单池" },  //5个订单池-高优先级、中优先级、暂缓
  //     // },
  //     // {
  //     //   path: "cyclicScheduling",
  //     //   component: () => import("@/views/newPage/cyclicScheduling/index"),
  //     //   name: "cyclicScheduling",
  //     //   meta: { title: "供应链订单池" }, //5个订单池-储备订单、周期性排产
  //     // },
  //     // {
  //     //   path: "moldArrivalPlan",
  //     //   component: () => import("@/views/newPage/moldArrivalPlan/index"),
  //     //   name: "moldArrivalPlan",
  //     //   meta: { title: "新模具到货计划" },
  //     // },
  //     {
  //       path: "moldAlternationPlan",
  //       component: () => import("@/views/newPage/moldAlternationPlan/index"),
  //       name: "moldAlternationPlan",
  //       meta: { title: "模具交替计划" },
  //     },

  //     // {
  //     //   path: "moldingFixedMachine",
  //     //   component: () => import("@/views/molding/fixedMachine/index"),
  //     //   name: "moldingFixedMachine",
  //     //   meta: { title: "成型固定机台界面" },
  //     // },
  //     // {
  //     //   path: "moldingStructVulcRate",
  //     //   component: () => import("@/views/molding/moldingStructVulcRate/index"),
  //     //   name: "MoldingStructVulcRate",
  //     //   meta: { title: "成型结构与硫化配比" },
  //     // },
  //     {
  //       path: "formingAccuracyPlan",
  //       component: () => import("@/views/newPage/formingAccuracyPlan/index"),
  //       name: "formingAccuracyPlan",
  //       meta: { title: "成型精度计划" },
  //     },
  //     {
  //       path: "formingStructureProductionStandard",
  //       component: () => import("@/views/newPage/formingStructureProductionStandard/index"),
  //       name: "formingStructureProductionStandard",
  //       meta: { title: "成型结构班产标准" },
  //     },
  //     // {
  //     //   path: "skuEmbryoRelation",
  //     //   component: () => import("@/views/newPage/skuEmbryoRelation/index"),
  //     //   name: "SkuEmbryoRelation",
  //     //   meta: { title: "SKU与胎胚结构关系" },
  //     // },
  //     {
  //       path: "moldingEmbryoRelation",
  //       component: () => import("@/views/newPage/moldingEmbryoRelation/index"),
  //       name: "MoldingEmbryoRelation",
  //       meta: { title: "结构与成型编号关系" },
  //     },
  //     {
  //       path: "moldingEmbryoRelation",
  //       component: () => import("@/views/newPage/moldingEmbryoRelation/index"),
  //       name: "MoldingEmbryoRelation",
  //       meta: { title: "结构与成型编号关系" },
  //     },
  //     {
  //       path: "curingPressCapsuleUsageRecords",
  //       component: () => import("@/views/newPage/curingPressCapsuleUsageRecords/index"),
  //       name: "curingPressCapsuleUsageRecords",
  //       meta: { title: "硫化机胶囊使用次数" },
  //     },
  //     {
  //       path: "curingMachineMaintenancePlan",
  //       component: () => import("@/views/newPage/curingMachineMaintenancePlan/index"),
  //       name: "CuringMachineMaintenancePlan",
  //       meta: { title: "硫化机保养计划" },
  //     },
  //     {
  //       path: "curingMachineRepairPlan",
  //       component: () => import("@/views/newPage/curingMachineRepairPlan/index"),
  //       name: "CuringMachineRepairPlan",
  //       meta: { title: "硫化机维修计划" },
  //     },
  //     {
  //       path: "structTreadConfig",
  //       component: () => import("@/views/newPage/structTreadConfig/index"),
  //       name: "StructTreadConfig",
  //       meta: { title: "结构整车胎面配置" },
  //     },
  //     {
  //       path: "moldingClosingStageProgress",
  //       component: () => import("@/views/newPage/moldingClosingStageProgress/index"),
  //       name: "MoldingClosingStageProgress",
  //       meta: { title: "成型收尾进度" },
  //     },
  //     // {
  //     //   path: "curingPlanShiftProductStandard",
  //     //   component: () => import("@/views/newPage/curingPlanShiftProductStandard/index"),
  //     //   name: "CuringPlanShiftProductStandard",
  //     //   meta: { title: "硫化计划班产标准" },
  //     // },
  //     // {
  //     //   path: "materialProductStructureMapping",
  //     //   component: () => import("@/views/newPage/materialProductStructureMapping/index"),
  //     //   name: "MaterialProductStructureMapping",
  //     //   meta: { title: "特殊材料批次比例" },
  //     // },
  //     // {
  //     //   path: "rawMaterialRequirement",
  //     //   component: () => import("@/views/newPage/rawMaterialRequirement/index"),
  //     //   name: "RawMaterialRequirement",
  //     //   meta: { title: "原材料需求计划" },
  //     // },
  //     // {
  //     //   path: "rawWarningConfig",
  //     //   component: () => import("@/views/newPage/rawWarningConfig/index"),
  //     //   name: "rawWarningConfig",
  //     //   meta: { title: "原材料预警配置" },
  //     // },
  //     // {
  //     //   path: "rawWarningRecord",
  //     //   component: () => import("@/views/newPage/rawWarningRecord/index"),
  //     //   name: "rawWarningRecord",
  //     //   meta: { title: "原材料预警记录" },
  //     // },
  //     // {
  //     //   path: "rawWeekUsage",
  //     //   component: () => import("@/views/newPage/rawWeekUsage/index"),
  //     //   name: "rawWeekUsage",
  //     //   hidden: true,
  //     //   meta: { title: "月计划周度原材料用料偏差" },
  //     // },
  //     // {
  //     //   path: "specialRawMaterialConfig",
  //     //   component: () => import("@/views/newPage/specialRawMaterialConfig/index"),
  //     //   name: "SpecialRawMaterialConfig",
  //     //   meta: { title: "特殊材料库存" },
  //     // },
  //     // {
  //     //   path: "rawMaterialBatchConfig",
  //     //   component: () => import("@/views/newPage/rawMaterialBatchConfig/index"),
  //     //   name: "RawMaterialBatchConfig",
  //     //   meta: { title: "原材料出库量" },
  //     // },
  //     // {
  //     //   path: "moldingDrumRegister",
  //     //   component: () => import("@/views/newPage/moldingDrumRegister/index"),
  //     //   name: "MoldingDrumRegister",
  //     //   meta: { title: "成型鼓台账" },
  //     // },
  //     // {
  //     //   path: "capsuleJigRegister",
  //     //   component: () => import("@/views/newPage/capsuleJigRegister/index"),
  //     //   name: "CapsuleJigRegister",
  //     //   meta: { title: "胶囊卡盘台账" },
  //     // },
  //     // {
  //     //   path: "moldShellRegister",
  //     //   component: () => import("@/views/newPage/moldShellRegister/index"),
  //     //   name: "MoldShellRegister",
  //     //   meta: { title: "模壳台账" },
  //     // },
  //     // {
  //     //   path: "monthlyAverageSales",
  //     //   component: () => import("@/views/newPage/monthlyAverageSales/index"),
  //     //   name: "MonthlyAverageSales",
  //     //   meta: { title: "月均销量" },
  //     // },
  //     // {
  //     //   path: "demandPlan",
  //     //   component: () => import("@/views/newPage/demandPlan/index"),
  //     //   name: "DemandPlan",
  //     //   meta: { title: "需求计划" },
  //     // },
  //     // {
  //     //   path: "fgInventory",
  //     //   component: () => import("@/views/newPage/fgInventory/index"),
  //     //   name: "FgInventory",
  //     //   meta: { title: "成品库存" },
  //     // },
  //     // {
  //     //   path: "periodicSchedSetup",
  //     //   component: () => import("@/views/newPage/periodicSchedSetup/index"),
  //     //   name: "periodicSchedSetup",
  //     //   meta: { title: "月周期排产结构配置" },
  //     // },
  //     // {
  //     //   path: "versionInventory",
  //     //   component: () => import("@/views/newPage/versionInventory/index"),
  //     //   name: "versionInventory",
  //     //   meta: { title: "版本库存" },
  //     // },
  //     // {
  //     //   path: "monthPlanned",
  //     //   component: () => import("@/views/newPage/monthPlanned/index"),
  //     //   name: "monthPlanned",
  //     //   meta: { title: "月底计划余量" },
  //     // },
  //     // {
  //     //   path: "orderForecast",
  //     //   component: () => import("@/views/newPage/orderForecast/index"),
  //     //   name: "orderForecast",
  //     //   meta: { title: "排产预测" },
  //     // },
  //     // {
  //     //   path: "moldLedger",
  //     //   component: () => import("@/views/newPage/moldLedger/index"),
  //     //   name: "moldLedger",
  //     //   meta: { title: "模具台账" },
  //     // },
  //     // {
  //     //   path: "formingCapacity",
  //     //   component: () => import("@/views/newPage/formingCapacity/index"),
  //     //   name: "formingCapacity",
  //     //   meta: { title: "成型机台管理" },
  //     // },
  //     {
  //       path: "moldingRestrictions",
  //       component: () => import("@/views/newPage/moldingRestrictions/index"),
  //       name: "moldingRestrictions",
  //       meta: { title: "成型限制" },
  //     },
  //     {
  //       path: "machineStructure",
  //       component: () => import("@/views/newPage/machineStructure/index"),
  //       name: "machineStructure",
  //       meta: { title: "在机结构" },
  //     },
  //     {
  //       path: "machineSKU",
  //       component: () => import("@/views/newPage/machineSKU/index"),
  //       name: "machineSKU",
  //       meta: { title: "在机SKU" },
  //     },
  //     // {
  //     //   path: "trialPlan",
  //     //   component: () => import("@/views/newPage/trialPlan/index"),
  //     //   name: "trialPlan",
  //     //   meta: { title: "试制量试计划" },
  //     // },
  //     {
  //       path: "unscheduledSkuList",
  //       component: () => import("@/views/newPage/unscheduledSkuList/index"),
  //       name: "unscheduledSkuList",
  //       meta: { title: "未排SKU列表" },
  //     },
  //     // {
  //     //   path: "workCalendar",
  //     //   component: () => import("@/views/newPage/workCalendar/index"),
  //     //   name: "workCalendar",
  //     //   meta: { title: "工作日历" },
  //     // },
  //     // {
  //     //   path: "moldAllocation",
  //     //   component: () => import("@/views/newPage/moldAllocation/index"),
  //     //   name: "moldAllocation",
  //     //   meta: { title: "模具分配比例" },
  //     // },
  //     // {
  //     //   path: "insertOrder",
  //     //   component: () => import("@/views/newPage/insertOrder/index"),
  //     //   name: "insertOrder",
  //     //   meta: { title: "实单模拟排产" },
  //     // },
  //     // {
  //     //   path: "periodicSched",
  //     //   component: () => import("@/views/newPage/periodicSched/index"),
  //     //   name: "periodicSched",
  //     //   meta: { title: "周期排产结构配置" },
  //     // },
  //     // {
  //     //   path: "specialMaterial",
  //     //   component: () => import("@/views/newPage/specialMaterial/index"),
  //     //   name: "specialMaterial",
  //     //   meta: { title: "特殊材料清单" },
  //     // },
  //     // {
  //     //   path: "vulcanizationTable",
  //     //   component: () => import("@/views/newPage/vulcanizationTable/index"),
  //     //   name: "vulcanizationTable",
  //     //   meta: { title: "月计划硫化监控表" },
  //     // },
  //     {
  //       path: "weeklySchedule",
  //       component: () => import("@/views/newPage/weeklySchedule/index"),
  //       name: "weeklySchedule",
  //       meta: { title: "周程变动通知单" },
  //     },
  //     {
  //       path: "precisionPlan",
  //       component: () => import("@/views/newPage/precisionPlan/index"),
  //       name: "precisionPlan",
  //       meta: { title: "精度计划" },
  //     },
  //     // {
  //     //   path: "scheduledShutdown",
  //     //   component: () => import("@/views/newPage/scheduledShutdown/index"),
  //     //   name: "scheduledShutdown",
  //     //   meta: { title: "设备计划停机" },
  //     // },
  //     {
  //       path: "cleaningPlan",
  //       component: () => import("@/views/newPage/cleaningPlan/index"),
  //       name: "cleaningPlan",
  //       meta: { title: "模具清洗计划" },
  //     },
  //     {
  //       path: "warehouseManage",
  //       component: () => import("@/views/newPage/warehouseManage/index"),
  //       name: "warehouseManage",
  //       meta: { title: "钢丝斜裁排程库排管理" },
  //     },
  //     {
  //       path: "repairSchedule",
  //       component: () => import("@/views/newPage/repairSchedule/index"),
  //       name: "repairSchedule",
  //       meta: { title: "钢丝斜裁排程保养检修计划" },
  //     },
  //     {
  //       path: "rtredRepairSchedule",
  //       component: () => import("@/views/newPage/rtredRepairSchedule/index"),
  //       name: "rtredRepairSchedule",
  //       meta: { title: "胎面排程保养检修计划" },
  //     },
  //     {
  //       path: "rawSchedule",
  //       component: () => import("@/views/newPage/rawSchedule/index"),
  //       name: "rawSchedule",
  //       meta: { title: "母炼胶排程" },
  //     },
  //     {
  //       path: "drugScheduling",
  //       component: () => import("@/views/newPage/drugScheduling/index"),
  //       name: "drugScheduling",
  //       meta: { title: "药品排程" },
  //     },
  //     {
  //       path: "rubberMaterials",
  //       component: () => import("@/views/newPage/rubberMaterials/index"),
  //       name: "rubberMaterials",
  //       meta: { title: "各胶料优先排产机台" },
  //     },
  //     {
  //       path: "carWashGlue",
  //       component: () => import("@/views/newPage/carWashGlue/index"),
  //       name: "carWashGlue",
  //       meta: { title: "洗车胶规则管理" },
  //     },
  //     {
  //       path: "medicationCart",
  //       component: () => import("@/views/newPage/medicationCart/index"),
  //       name: "medicationCart",
  //       meta: { title: "药品车各规格包数管理" },
  //     },
  //     {
  //       path: "weighingPackages",
  //       component: () => import("@/views/newPage/weighingPackages/index"),
  //       name: "weighingPackages",
  //       meta: { title: "药品称重包数管理" },
  //     },
  //     {
  //       path: "drugInventory",
  //       component: () => import("@/views/newPage/drugInventory/index"),
  //       name: "drugInventory",
  //       meta: { title: "药品库存管理" },
  //     },
  //   ],
  // },

  {
    path: "/moldingPlanManagement",
    component: Layout,
    hidden: true,
    redirect: "noredirect",
    permissions: ["monthplan:mdmMoldingMachineCls:edit"],
    children: [
      {
        path: "mdmMoldingMachineClsB/:id",
        component: () => import("@/views/masterdata/mdmMoldingMachineClsB/index"),
        name: "MdmMoldingMachineClsB",
        meta: { title: "单机班产额度配置" },
      }],
  },
  {
    path: "/monthlydata",
    component: Layout,
    hidden: true,
    redirect: "noredirect",
    children: [
      {
        path: "mdmStockUpPlanCreate",
        component: () => import("@/views/monthlydata/mdmStockUpPlan/create"),
        name: "mdmStockUpPlanCreate",
        meta: { title: "生成备货计划" },
      }],
  },
  {
    path: "/curingPlan",
    component: Layout,
    hidden: false,
    redirect: "noredirect",
    name: "",
    children: [
      {
        path: "curingUnschedule",
        component: () => import("@/views/curingPlan/curingUnschedule/index"),
        name: "CuringUnschedule",
        hidden: true,
        meta: { title: "硫化未排程结果" },
      },
      {
        path: "machineGantChart",
        component: () => import("@/views/curingPlan/curingSchedule/machineGantChart"),
        name: "MachineGantChart",
        hidden: true,
        meta: { title: "机台甘特图" },
      },
      {
        path: "specDescGantChart",
        component: () => import("@/views/curingPlan/curingSchedule/specDescGantChart"),
        name: "SpecDescGantChart",
        hidden: true,
        meta: { title: "规格甘特图" },
      },
    ],
  },
  {
    path: "/moldingPlanManagement",
    component: Layout,
    hidden: false,
    redirect: "noredirect",
    name: "",
    children: [
      {
        path: "moldingUnschedule",
        component: () => import("@/views/molding/moldingUnschedule/index"),
        name: "MoldingUnschedule",
        hidden: true,
        meta: { title: "成型未排程结果" },
      },
    ],
  },
  {
    path: "/monthPlanManagement",
    component: Layout,
    hidden: true,
    redirect: "noredirect",
    permissions: ["monthplan:productionMonthPlanInit:list"],
    children: [
      {
        path: "console/productionMonthPlanInit/:id",
        component: () => import("@/views/monthPlanManagement/console/productionMonthPlanInit"),
        name: "ProductionMonthPlanInit",
        meta: { title: "排结构明细" },
      }],
  },
  {
    path: "/monthPlanManagement",
    component: Layout,
    hidden: true,
    redirect: "noredirect",
    children: [
      {
        path: "insertOrderDetail/:id",
        component: () => import("@/views/newPage/insertOrderDetail"),
        name: "insertOrderDetail",
        meta: { title: "实单模拟明细" },
      }],
  },
  {
    path: "/monthPlanManagement",
    component: Layout,
    hidden: true,
    redirect: "noredirect",
    permissions: ["monthplan:productionMonthPlanInit:list"],
    children: [
      {
        path: "mouldingDayResult/:id",
        component: () => import("@/views/monthPlanManagement/mouldingDayResult"),
        name: "MouldingDayResult",
        meta: { title: "排程明细" },
      }],
  },
  {
    path: "/monthPlanManagement",
    component: Layout,
    hidden: true,
    redirect: "noredirect",
    permissions: ["monthplan:monthPlanNoProductionPlan:list"],
    children: [
      {
        path: "monthPlanNoProductionPlan/:id",
        component: () => import("@/views/monthPlanManagement/monthPlanNoProductionPlan"),
        name: "MonthPlanNoProductionPlan",
        meta: { title: "未排产排程明细" },
      }],
  },
  {
    path: "/monthPlanManagement",
    component: Layout,
    hidden: true,
    redirect: "noredirect",
    permissions: ["monthplan:productionMonthPlanInit:list"],
    children: [
      {
        path: "report/:id",
        component: () => import("@/views/monthPlanManagement/console/report"),
        name: "versionReport",
        meta: { title: "报表" },
      }],
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
