<template>
  <rolling-cycle page-variant="structureAdjust" />
</template>

<script>
import RollingCycle from "./index.vue";

/** 月计划结构调整（独立路由页，与结构内调整页拆分） */
export default {
  name: "MonthPlanStructureAdjust",
  components: { RollingCycle },
};
</script>
