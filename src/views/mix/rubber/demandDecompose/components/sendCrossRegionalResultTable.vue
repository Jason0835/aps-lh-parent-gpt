<template>
  <page-table
    :loading="loading"
    tableRef="demandDecomposeSendCrossRegionalResultTable"
    row-key="uuid"
    :columns="columns"
    :data="tableData"
    :page="page"
    :searchColumns="searchColumns"
    :search="search"
    :toolbar="false"
    @search="handleSearch"
    @selection-change="handleSelectionChange"
  >
    <template slot="header">
      <el-button :disabled="selection.length === 0" @click="handleDelete">{{
        $t("ui.frame.btn.delete")
      }}</el-button>
    </template>
  </page-table>
</template>

<script>
import moment from "moment";

import { getUuid } from "@/utils/uuid";

import {
  listGlueSpanSend,
  deleteGlueSpanSend,
} from "@/api/schedule/glueDecomposePlan";

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
      selection: [],
      search: {
        scheduleDate: moment().add(1, "days").format("yyyy-MM-DD"),
      },
      query: {},
      page: {
        current: 1,
        pageSize: 20,
        total: 0,
      },
    };
  },
  computed: {
    columns() {
      return [
        {
          type: "selection",
        },
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
      return [
        {
          label: this.$t(
            "schedule.glueDecomposePlan.sendCrossRegional.scheduleDate"
          ),
          prop: "scheduleDate",
          type: "date",
          dateType: "date",
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
    // api
    async getList() {
      try {
        this.loading = true;
        const res = await listGlueSpanSend(this.formatParams());
        this.tableData = res.rows;
        this.loading = false;
      } catch (error) {
        this.loading = false;
        console.error(error);
      }
    },
    formatParams(hasPage = true) {
      const params = {
        ...this.query,
        ...this.sort,
        source: "0",
        permissionMixAreaList: this.permissionMixAreaList.join(","),
      };

      if (hasPage) {
        params.pageSize = this.page.pageSize;
        params.pageNum = this.page.current;
      }

      if (params.createTime && params.createTime[0]) {
        params.createTimeStart = params.createTime[0];
        params.createTimeEnd = params.createTime[1];
        params.createTime = undefined;
      }

      return params;
    },

    handleAdd() {
      this.tableData.push({
        uuid: getUuid(),
        sendPerson: this.$store.state.user.name,
        ...this.params,
      });
    },
    handleDelete() {
      this.$confirm("确认删除?").then(() => {
        const ids = this.selection.map((row) => row.id).join(",");
        deleteGlueSpanSend({ ids }).then((res) => {
          this.$modal.msgSuccess(res.msg);
          this.getList();
        });
      });
    },
    handleSearch(data) {
      this.query = data;
      this.getList();
    },
    handleSelectionChange(rows) {
      this.selection = rows;
    },

    handleSend() {},
  },
};
</script>
