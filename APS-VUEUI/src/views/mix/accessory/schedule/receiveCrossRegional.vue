<template>
  <basic-container>
    <el-tabs type="card">
      <el-tab-pane :label="$t('ui.data.tabTitle.receiveCrossRegional')">
        <receiveCrossRegionalTable :isEdit="true" />
      </el-tab-pane>
      <el-tab-pane :label="$t('ui.data.tabTitle.queryReceiveCrossRegional')">
        <receiveCrossRegionalTable :isEdit="false" />
      </el-tab-pane>
    </el-tabs>
  </basic-container>
</template>
<script>
import receiveCrossRegionalTable from "./components/receiveCrossRegionalTable.vue";
export default {
 name: "MixRubberDemandPlanDecomposeReceiveCrossRegional",
  components: { receiveCrossRegionalTable },
  data() {
    return {};
  },
};
</script>