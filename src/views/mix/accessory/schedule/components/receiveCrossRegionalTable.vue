<template>
  <page-table
    :columns="columns"
    :data="tableData"
    :searchColumns="isEdit ? undefined : searchColumns"
    :toolbar="false"
  >
    <template slot="header" v-if="isEdit">
      <div class="table-header">
        <div class="scheduleDate">
          {{
            $t("schedule.glueScheduleResult.receiveCrossRegional.scheduleDate")
          }}: <el-date-picker />
        </div>

        <el-button>{{ $t("ui.data.btn.receiveGlueSpan") }}</el-button>
      </div>
    </template>
  </page-table>
</template>

<script>
export default {
  props: {
    isEdit: {
      type: Boolean,
      default: false,
    },
  },
  data() {
    return {
      tableData: [],
      minArea: null,
    };
  },
  computed: {
    columns() {
      if (this.isEdit) {
        return [
          {
            type: "selection",
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
          // {
          //   label: this.$t(
          //     "schedule.glueDecomposePlan.sendCrossRegional.scheduleDate"
          //   ),
          //   prop: "scheduleDate",
          //   render() {
          //     return <el-date-picker />;
          //   },
          // },
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
            label: this.$t("schedule.materialSpanReceive.materialName"),
            prop: "materialName",
            render() {
              return <el-input />;
            },
          },
          {
            label: this.$t("schedule.materialSpanReceive.totalSendQty"),
            prop: "totalSendQty",
            render() {
              return <el-input />;
            },
          },
          {
            label: this.$t("schedule.materialSpanReceive.totalReceiveQty"),
            prop: "totalReceiveQty",
            render() {
              return <el-input />;
            },
          },
          {
            label: this.$t("schedule.sendCrossRegional.machineCode"),
            prop: "machineName",
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
            label: this.$t("schedule.receiveCrossRegional.receivePerson"),
            prop: "receivePerson",
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
      }

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
          label: this.$t("schedule.materialSpanReceive.materialName"),
          prop: "materialName",
        },
        {
          label: this.$t("schedule.materialSpanReceive.totalSendQty"),
          prop: "totalSendQty",
        },
        {
          label: this.$t("schedule.materialSpanReceive.totalReceiveQty"),
          prop: "totalReceiveQty",
        },
        {
          label: this.$t("schedule.sendCrossRegional.machineCode"),
          prop: "machineName",
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
          label: this.$t("schedule.receiveCrossRegional.receivePerson"),
          prop: "receivePerson",
        },
        {
          label: this.$t("schedule.sendCrossRegional.finishQty"),
          prop: "finishQty",
        },
        {
          label: this.$t("ui.data.column.remark"),
          minWidth: 100, 
          prop: "remark",
        },
      ];
    },
    searchColumns() {
      if (this.isEdit) {
        return undefined;
      }
      return [
        {
          label: this.$t(
            "schedule.glueScheduleResult.receiveCrossRegional.scheduleDate"
          ),
          prop: "scheduleDate",
        },
        {
          label: this.$t("schedule.sendCrossRegional.entrustedMixArea"),
          prop: "entrustedMixArea",
          // type: "select",
        },
        {
          label: this.$t("schedule.materialSpanReceive.materialName"),
          prop: "materialName",
        },
      ];
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
