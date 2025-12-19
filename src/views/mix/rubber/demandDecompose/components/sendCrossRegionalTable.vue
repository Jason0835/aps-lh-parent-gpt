<template>
  <page-table
    ref="tableRef"
    tableRef="demandDecomposeSendCrossRegionalTable"
    row-key="uuid"
    :columns="columns"
    :data="tableData"
    :searchColumns="undefined"
    :toolbar="false"
    @selection-change="handleSelectionChange"
  >
    <template slot="header">
      <el-button @click="handleAdd">{{ $t("ui.frame.btn.add") }}</el-button>
      <el-button @click="handleDelete" :disabled="selection.length == 0">{{
        $t("ui.frame.btn.delete")
      }}</el-button>
      <el-select style="margin: 0 10px" v-model="mixArea">
        <el-option
          v-for="dict in parentDict.type.MIX_AREA"
          :key="dict.value"
          :value="dict.value"
          :label="dict.label"
        ></el-option>
      </el-select>
      <el-button :disabled="!mixArea" @click="handleMixAreaChange">{{
        $t("ui.data.btn.setMixArea")
      }}</el-button>
      <el-button
        :disabled="!mixArea || selection.length === 0"
        @click="handleSend"
        >{{ $t("ui.data.btn.send") }}</el-button
      >
    </template>
  </page-table>
</template>

<script>
import moment from 'moment';

import { getUuid } from "@/utils/uuid";

import { sendGlueSpan } from "@/api/schedule/glueDecomposePlan";

export default {
  props: {
    isEdit: {
      type: Boolean,
      default: false,
    },
    params: Object,
  },
  inject: ["parentDict"],
  data() {
    return {
      loading: false,
      tableData: [],
      mixArea: null,
      selection: [],
      entrustMixArea: null,
      scheduleDate: moment().add(1, "days").format("yyyy-MM-DD"),
    };
  },
  computed: {
    columns() {
      return [
        {
          type: "selection",
          selectable: (row, index) => {
            return row.sendFlag === "0";
          },
        },
        {
          type: "index",
        },
        {
          label: this.$t("schedule.sendCrossRegional.sendPerson"),
          prop: "sendPerson",
          // render: ({ row }) => {
          //   return <el-input />;
          // },
        },
        {
          label: this.$t(
            "schedule.glueScheduleResult.sendCrossRegional.scheduleDate"
          ),
          prop: "scheduleDate",
          render: ({ row }) => {
            return (
              <el-date-picker
                class="w100"
                type="date"
                v-model={row.scheduleDate}
                value-format="yyyy-MM-dd"
                disabled
              />
            );
          },
        },
        {
          label: this.$t("schedule.sendCrossRegional.entrustMixArea"),
          prop: "entrustMixArea",
          render: ({ row }) => {
            return (
              <dict-select
                v-model={row.entrustMixArea}
                options={this.parentDict.type.MIX_AREA}
                disabled
              />
            );
          },
        },
        {
          label: this.$t("schedule.sendCrossRegional.entrustedMixArea"),
          prop: "entrustedMixArea",
          render: ({ row }) => {
            return (
              <dict-select
                v-model={row.entrustedMixArea}
                options={this.parentDict.type.MIX_AREA}
              />
            );
          },
        },
        {
          label: this.$t("schedule.sendCrossRegional.glue"),
          prop: "glue",
          render: ({ row }) => {
            return <el-input v-model={row.glue} />;
          },
        },
        {
          label: this.$t("schedule.sendCrossRegional.sendQty"),
          prop: "sendQty",
          render: ({ row }) => {
            return <el-input v-model={row.sendQty} />;
          },
        },
        {
          label: this.$t("schedule.sendCrossRegional.expectDemandTime"),
          prop: "expectDemandTime",
          render: ({ row }) => {
            return <el-input v-model={row.expectDemandTime} />;
          },
        },
        {
          label: this.$t("ui.data.column.remark"),
          prop: "remark",
          render: ({ row }) => {
            return <el-input v-model={row.remark} />;
          },
        },
      ];
    },
  },
  methods: {
    // api
    async send(params) {
      try {
        this.loading = true;
        const res = await sendGlueSpan(params);

        const ids = this.selection.map((row) => row.uuid);
        this.tableData.forEach((row) => {
          if (ids.includes(row.uuid)) {
            row.sendFlag = "1";
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

    handleAdd() {
      this.tableData.push({
        uuid: getUuid(),
        sendPerson: this.$store.state.user.name,
        scheduleDate: this.scheduleDate,
        ...this.params,
        sendFlag: "0",
      });
    },
    handleDelete() {
      const ids = this.selection.map((row) => row.uuid);

      const list = this.tableData.filter((row) => {
        return !ids.includes(row.uuid);
      });
      this.tableData = list;
    },

    handleSelectionChange(rows) {
      this.selection = rows;
    },
    handleMixAreaChange() {
      this.tableData.forEach((row, index) => {
        this.$set(this.tableData[index], "entrustedMixArea", this.mixArea);
        // row.entrustedMixArea = this.mixArea;
      });
    },
    handleSend() {
      this.send({
        glueSpanSendList: this.selection.map((row) => {
          return {
            ...row,
          };
        }),
      });
    },
  },

};
</script>
