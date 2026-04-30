<template>
  <basic-container v-loading="loading">
    <el-tabs type="card">
      <el-tab-pane :label="$t('ui.data.tabTitle.receiveCrossRegional')">
        <receiveCrossRegionalTable :params="params" />
      </el-tab-pane>
      <el-tab-pane :label="$t('ui.data.tabTitle.queryReceiveCrossRegional')">
        <receiveCrossRegionalResultTable :params="params" :scheduleMixAreaPermission="scheduleMixAreaPermission"/>
      </el-tab-pane>
    </el-tabs>
  </basic-container>
</template>
<script>
import { scheduleMixAreaPermission } from "@/api/setting/service";

import receiveCrossRegionalTable from "./components/receiveCrossRegionalTable.vue";
import receiveCrossRegionalResultTable from "./components/receiveCrossRegionalResultTable.vue";
export default {
 name: "MixRubberDemandPlanDecomposeReceiveCrossRegional",
  components: { receiveCrossRegionalTable, receiveCrossRegionalResultTable },
  dicts: ["MIX_AREA"],
  provide() {
    return {
      parentDict: this.dict,
    };
  },
  data() {
    return {
      loading: false,
      params: {},
      scheduleMixAreaPermission: [],
    };
  },
  methods: {
    async getScheduleMixAreaPermission() {
      try {
        this.loading = true;
        const res = await scheduleMixAreaPermission();
        this.scheduleMixAreaPermission = res.map(({dictLabel, dictValue}) => {
          return {
            dictValue,
            dictLabel,
          };
        });
        this.loading = false;
      } catch (error) {
        console.error(error);
        this.loading = false;
      }
    },
  },

  created() {
    if (this.$route.query) {
      this.params = {
        ...this.$route.query,
      };
    }
    this.getScheduleMixAreaPermission();
  },
};
</script>
