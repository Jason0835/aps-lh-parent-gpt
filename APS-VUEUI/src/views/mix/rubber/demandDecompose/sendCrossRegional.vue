<template>
  <basic-container>
    <el-tabs type="card">
      <el-tab-pane :label="$t('发送跨区请求')">
        <sendCrossRegionalTable
          :params="params"
          :scheduleMixAreaPermission="scheduleMixAreaPermission"
        />
      </el-tab-pane>
      <el-tab-pane :label="$t('跨区请求查询')">
        <sendCrossRegionalResultTable
          :params="params"
          :scheduleMixAreaPermission="scheduleMixAreaPermission"
        />
      </el-tab-pane>
    </el-tabs>
  </basic-container>
</template>
<script>
import { scheduleMixAreaPermission } from "@/api/setting/service";

import sendCrossRegionalTable from "./components/sendCrossRegionalTable.vue";
import sendCrossRegionalResultTable from "./components/sendCrossRegionalResultTable.vue";
export default {
 name: "MixRubberDemandPlanDecomposeSendCrossRegional",
  components: { sendCrossRegionalTable, sendCrossRegionalResultTable },
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
        console.log(res);
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
