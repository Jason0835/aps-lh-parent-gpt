<template>
  <page-table
    :loading="loading"
    tableRef="demandDecomposeReceiveCrossRegionalResultTable"
    :columns="columns"
    :data="tableData"
    :toolbar="false"
    @refresh="getList"
    @selection-change="handleSelectionChange"
  >
    <template slot="header">
      <div class="table-header">
        <div class="scheduleDate">
          {{
            $t("schedule.glueDecomposePlan.receiveCrossRegional.scheduleDate")
          }}:
          <el-date-picker
            v-model="scheduleDate"
            type="date"
            value-format="yyyy-MM-dd"
            @change="handleScheduleDateChange"
          />
        </div>

        <el-button @click="handleReceive">{{
          $t("ui.data.btn.receiveGlueSpan")
        }}</el-button>
      </div>
    </template>
  </page-table>
</template>

<script>
import moment from "moment";

import {
  listGlueSpanReceive,
  receiveGlueSpanReceive,
} from "@/api/schedule/glueDecomposePlan";
export default {
  props: {
    isEdit: {
      type: Boolean,
      default: false,
    },
    params: Object,
  },
  data() {
    return {
      tableData: [],
      mixArea: null,
      scheduleDate: moment().add(1, "days").format("yyyy-MM-DD"),
      selection: [],
    };
  },
  computed: {
    columns() {
      return [
        {
          type: "selection",
          selectable: (row, index) => {
            return row.receiveStatus !== "1";
          },
        },
        {
          type: "index",
        },
        {
          label: this.$t("schedule.sendCrossRegional.sendPerson"),
          prop: "sendPerson",
          render() {
            return <el-input />;
          },
        },

        {
          label: this.$t("schedule.sendCrossRegional.entrustMixArea"),
          prop: "entrustMixArea",
          render() {
            return <el-select />;
          },
        },
        {
          label: this.$t("schedule.sendCrossRegional.entrustedMixArea"),
          prop: "entrustedMixArea",
          render() {
            return <el-select />;
          },
        },
        {
          label: this.$t("schedule.sendCrossRegional.glue"),
          prop: "glue",
          render() {
            return <el-input />;
          },
        },
        {
          label: this.$t("schedule.receiveCrossRegional.sendQty"),
          prop: "sendQty",
          render() {
            return <el-input />;
          },
        },
        {
          label: this.$t("schedule.receiveCrossRegional.receiveQty"),
          prop: "receiveQty",
          render() {
            return <el-input />;
          },
        },
        {
          label: this.$t("schedule.sendCrossRegional.expectDemandTime"),
          prop: "expectDemandTime",
          render() {
            return <el-date-picker />;
          },
        },
        {
          label: this.$t("schedule.sendCrossRegional.machineCode"),
          prop: "machineCode",
          render() {
            return <el-input />;
          },
        },
        {
          label: this.$t("schedule.receiveCrossRegional.recipeTypeName"),
          prop: "recipeTypeName",
          render() {
            return <el-input />;
          },
        },
        {
          label: this.$t("schedule.receiveCrossRegional.recipeVersionId"),
          prop: "recipeVersionId",
          render() {
            return <el-input />;
          },
        },
        {
          label: this.$t("schedule.receiveCrossRegional.recipeStage"),
          prop: "recipeStage",
          render() {
            return <el-input />;
          },
        },
        {
          label: this.$t("ui.data.column.remark"),
          prop: "remark",
          render() {
            return <el-input />;
          },
        },
      ];
    },
  },
  methods: {
    async receive(params) {
      try {
        this.loading = true;
        const res = await receiveGlueSpanReceive(params);

        const ids = this.selection.map((row) => row.id);
        this.tableData.forEach((row) => {
          if (ids.includes(row.id)) {
            row.receiveStatus = "1";
          }
        });

        this.$refs.tableRef.getTableRef().clearSelection();

        this.$modal.msgSuccess(res.msg);
        this.loading = false;
      } catch (error) {
        console.error(error);
        this.loading = false;
      }
    },
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

    handleSelectionChange(rows) {
      this.selection = rows;
    },

    handleScheduleDateChange() {
      this.getList();
    },

    async handleReceive() {
      this.receive({
        glueSpanReceiveList: this.selection.map((row) => {
          return {
            ...row,
          };
        }),
      });
    },
  },

  created() {
    this.getList();
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
