<template>
  <basic-container>
    <el-tabs type="card">
      <el-tab-pane :label="$t('发送跨区请求')">
        <sendCrossRegionalTable :isEdit="true" />
      </el-tab-pane>
      <el-tab-pane :label="$t('跨区请求查询')">
        <sendCrossRegionalTable :isEdit="false" />
      </el-tab-pane>
    </el-tabs>
  </basic-container>
</template>
<script>
import sendCrossRegionalTable from "./components/sendCrossRegionalTable.vue";
export default {
 name: "MixRubberDemandPlanDecomposeSendCrossRegional",
  components: { sendCrossRegionalTable },
  data() {
    return {};
  },
};
</script>