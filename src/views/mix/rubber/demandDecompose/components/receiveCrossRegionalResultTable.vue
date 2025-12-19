<template>
  <page-table
    :columns="columns"
    :data="tableData"
    :searchColumns="searchColumns"
    :search="search"
    :toolbar="false"
    @search="handleSearch"
  >
  </page-table>
</template>

<script>
export default {
  props: {
    isEdit: {
      type: Boolean,
      default: false,
    },
    params: Object,
    scheduleMixAreaPermission: Array,
  },
  inject: ["parentDict"],
  data() {
    return {
      tableData: [],
      mixArea: null,
      search: {
        scheduleDate: moment().add(1, "days").format("yyyy-MM-DD"),
      },
      query: {
        scheduleDate: moment().add(1, "days").format("yyyy-MM-DD"),
      },
    };
  },
  computed: {
    columns() {
      return [
        {
          label: this.$t("schedule.sendCrossRegional.sendPerson"),
          prop: "sendPerson",
        },
        {
          label: this.$t("schedule.sendCrossRegional.entrustMixArea"),
          prop: "entrustMixArea",
        },
        {
          label: this.$t("schedule.sendCrossRegional.entrustedMixArea"),
          prop: "entrustedMixArea",
        },
        {
          label: this.$t("schedule.sendCrossRegional.glue"),
          prop: "glue",
        },
        {
          label: this.$t("schedule.sendCrossRegional.sendQty"),
          prop: "sendQty",
        },
        {
          label: this.$t("schedule.sendCrossRegional.receiveQty"),
          prop: "receiveQty",
        },
        {
          label: this.$t("schedule.sendCrossRegional.expectDemandTime"),
          prop: "expectDemandTime",
        },
        {
          label: this.$t("schedule.sendCrossRegional.machineCode"),
          prop: "machineCode",
        },
        {
          label: this.$t("schedule.receiveCrossRegional.recipeTypeName"),
          prop: "recipeTypeName",
        },
        {
          label: this.$t("schedule.receiveCrossRegional.recipeVersionId"),
          prop: "recipeVersionId",
        },
        {
          label: this.$t("schedule.receiveCrossRegional.recipeStage"),
          prop: "recipeStage",
        },
        {
          label: this.$t("schedule.sendCrossRegional.finishQty"),
          prop: "finishQty",
        },
        {
          label: this.$t("ui.data.column.remark"),
          prop: "remark",
        },
      ];
    },
    searchColumns() {
      return [
        {
          label: this.$t(
            "schedule.glueDecomposePlan.sendCrossRegional.scheduleDate"
          ),
          prop: "scheduleDate",
          type: "date",
          valueFormat: "yyyy-MM-dd",
        },
        {
          label: this.$t("schedule.sendCrossRegional.entrustedMixArea"),
          prop: "entrustedMixArea",
          type: "select",
          dictData: this.parentDict.type.MIX_AREA,
        },
        {
          label: this.$t("schedule.sendCrossRegional.glue"),
          prop: "glue",
        },
      ];
    },
    permissionMixAreaList() {
      return this.scheduleMixAreaPermission.map((row) => row.dictValue);
    },
  },
  methods: {
    async getList() {
      try {
        this.loading = true;
        const res = await listGlueSpanReceive(this.formatParams());
        console.log(res);
        this.loading = false;
      } catch (error) {
        console.error(error);
        this.loading = false;
      }
    },

    formatParams(hasPage = true) {
      const params = {
        ...this.query,
        ...this.sort,
        scheduleDate: this.scheduleDate,
        entrustedMixArea: this.params.mixArea,
        source: "0",
      };

      if (hasPage) {
        // params.pageSize = this.page.pageSize;
        // params.pageNum = this.page.current;
      }

      if (params.createTime && params.createTime[0]) {
        params.createTimeStart = params.createTime[0];
        params.createTimeEnd = params.createTime[1];
        params.createTime = undefined;
      }

      return params;
    },

    handleSearch(query) {
      this.query = query;
      this.getList();
    },
  },
};
</script>

<style lang="scss" scoped>
.table-header {
  display: flex;
  .scheduleDate {
    margin-right: 10px;
  }
}
</style>
