<template>
  <rolling-cycle layout-mode="monthPlanStructureInner" />
</template>

<script>
import RollingCycle from "@/views/newPage/rollingCycle/index.vue";

/**
 * 月计划结构内/结构调整独立页（本地路由）。
 * 复用周程滚动调整业务组件，通过 layoutMode 隐藏 el-tabs。
 * 页签标题由路由 query.pageType 驱动：默认结构内调整，structure 为结构调整。
 */
export default {
  name: "MonthPlanStructureInnerAdjust",
  components: {
    RollingCycle,
  },
};
</script>
