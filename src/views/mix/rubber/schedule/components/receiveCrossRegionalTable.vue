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
            $t("schedule.glueDecomposePlan.receiveCrossRegional.scheduleDate")
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
          {
            label: this.$t(
              "schedule.glueDecomposePlan.sendCrossRegional.scheduleDate"
            ),
            prop: "scheduleDate",
            render() {
              return <el-date-picker />;
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
            label: this.$t("schedule.sendCrossRegional.sendQty"),
            prop: "sendQty",
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
      if (this.isEdit) {
        return undefined;
      }
      return [
        {
          label: this.$t(
            "schedule.glueDecomposePlan.sendCrossRegional.scheduleDate"
          ),
          prop: "scheduleDate",
        },
        {
          label: this.$t("schedule.sendCrossRegional.entrustedMixArea"),
          prop: "entrustedMixArea",
          // type: "select",
        },
        {
          label: this.$t("schedule.sendCrossRegional.glue"),
          prop: "glue",
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