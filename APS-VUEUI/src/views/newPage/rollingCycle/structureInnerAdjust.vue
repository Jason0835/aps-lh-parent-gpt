<template>
  <rolling-cycle page-variant="structureInner" />
</template>

<script>
import RollingCycle from "./index.vue";

/** 月计划结构内调整（独立路由页，与结构调整页拆分） */
export default {
  name: "MonthPlanStructureInnerAdjust",
  components: { RollingCycle },
};
</script>
