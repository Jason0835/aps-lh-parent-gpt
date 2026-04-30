
<template>
  <basic-container>
    <page-table
      tableRef="MouldusestatusMainTable"
      :calcHeight="true"
      v-loading="loading"
      :columns="columns"
      :searchColumns="searchColumns"
      :data="data"
      :page="page"
      :search="search"
      @refresh="getList"
      @search="handleSearch"
      @pageChange="handlePageChange"
      @sort-change="handleSortChange"
      @selection-change="handleSelectionChange"
      :showSummary="false"
      :selectArea="false"
    >
      <template slot="header">
        <el-button
          type="primary"
          plain
          v-hasPermi="['lean:mouldusestatus:add']"
          @click="handleAdd"
          >{{ $t("ui.frame.btn.add") }}</el-button
        >
        <el-button
          type="primary"
          plain
          :disabled="selection.length !== 1"
          v-hasPermi="['lean:mouldusestatus:edit']"
          @click="handleEdit(selection[0])"
          >{{ $t("ui.frame.btn.modify") }}</el-button
        >
        <el-button
          type="primary"
          plain
          v-hasPermi="['lean:mouldusestatus:copy']"
          @click="handleCopy(selection[0])"
          >{{ $t("ui.frame.btn.copy") }}</el-button
        >
        <el-button
          type="danger"
          plain
          :disabled="selection.length === 0"
          v-hasPermi="['lean:mouldusestatus:remove']"
          @click="handleDelete(selection)"
          >{{ $t("ui.frame.btn.delete") }}</el-button
        >
        <el-button
          v-hasPermi="['lean:mouldusestatus:import']"
          @click="$refs.tltUpload.handleImport()"
          >{{ $t("ui.frame.btn.import") }}</el-button
        >
        <el-button
          @click="handleExport"
          v-hasPermi="['lean:mouldusestatus:export']"
          >{{ $t("ui.frame.btn.export") }}</el-button
        >
      </template>
    </page-table>
    <!-- <el-button style="display: none" ref="hidePopoverBtnRef"></el-button> -->
    <tlt-upload
      ref="tltUpload"
      downloadUrl="/lean/mouldusestatus/importTemplate"
      uploadUrl="/lean/mouldusestatus/importData"
      @uploadSuccess="getList"
    />
    <infoDialog ref="infoRef" @success="getList" />
    <copyDialog ref="copyRef" @success="getList" />
  </basic-container>
</template>
<script>
//lib
import moment from "moment";
//utils
import { downloadLink } from "@/utils/request";
//interface
import {
  listMouldusestatus,
  editMouldusestatus,
  removeMouldusestatus,
} from "@/api/lean/mouldusestatus";
//components
import tltUpload from "@/components/tltUpload/tltUpload.vue";

import infoDialog from "./components/infoDialog.vue";
import copyDialog from "./components/copyDialog.vue";

export default {
  name: "Mouldusestatus",
  components: {
    tltUpload,
    infoDialog,
    copyDialog,
  },
  dicts: ["biz_factory_name", "biz_mould_Type", "biz_available_status"],
  provide() {
    return {
      parentDict: this.dict,
    };
  },
  data() {
    return {
      loading: false,
      data: [],
      selection: [],
      page: {
        current: 1,
        pageSize: 20,
        total: 0,
      },
      sort: {},
      search: {
        mainPlanMonth: "",
      },
      query: {
        mainPlanMonth: "",
      },
      importDefaultValue: {},
      importRules: {},
    };
  },
  computed: {
    columns() {
      let columns = [
        { type: "selection", fixed: "left" },
        {
          prop: "specificationsPattern",
          label: this.$t("ui.data.column.modelinfo.spattern"),

          width: 250,
        },
        {
          prop: "mouldCode",
          label: this.$t("ui.data.column.mouldusestatus.mouldCode"),
        },
        {
          prop: "year",
          label: this.$t("ui.data.column.mouldusestatus.year"),
        },
        {
          prop: "month",
          label: this.$t("ui.data.column.mouldusestatus.month"),
        },

        {
          prop: "factoryCode",
          label: this.$t("ui.data.column.mouldusestatus.factoryCode"),
          formatter: (row, column, value, index) => {
            return this.selectDictLabel(
              this.dict.type.biz_factory_name,
              row.factoryCode
            );
          },
        },
        {
          prop: "mouldStatus",
          label: this.$t("ui.data.column.mouldusestatus.mouldStatus"),
          // formatter: function (row, column, value, index) {
          //     return statusTools(row);
          // },
          formatter: (row, column, value, index) => {
            return this.selectDictLabel(
              this.dict.type.biz_available_status,
              row.mouldStatus
            );
          },
        },

        {
          prop: "specifications",
          label: this.$t("ui.data.column.mouldusestatus.specifications"),
          width: 120,
        },
        {
          prop: "pattern",
          label: this.$t("ui.data.column.modelinfo.patternt"),
          width: 140,
        },
        {
          prop: "mouldType",
          label: this.$t("ui.data.column.mouldusestatus.mouldType"),
          formatter: (row, column, value, index) => {
            return this.selectDictLabel(
              this.dict.type.biz_mould_Type,
              row.mouldType
            );
          },
        },
        {
          prop: "remark",
          label: this.$t("ui.data.column.mouldusestatus.remark"),
          minWidth: 100, 
        },
      ];

      return columns;
    },
    searchColumns() {
      return [
        {
          label: this.$t("ui.data.column.mouldusestatus.yearMonth"),
          prop: "yearMonth",
          type: "date",
          dateType: "month",
          valueFormat: "yyyy-MM",
        },
        // {
        //   label: this.$t("ui.data.column.mouldusestatus.year"),
        //   prop: "month",
        //   type: "date",
        //   dateType: "month",
        //   valueFormat: "MM",
        // },
        {
          label: this.$t("setting.factoryGlueAreaRelation.factory"),
          prop: "factoryCode",
          render: (form) => {
            return (
              <dict-select
                clearable
                v-model={form.factoryCode}
                options={this.dict.type.biz_factory_name}
              />
            );
          },
        },
        {
          prop: "mouldStatus",
          label: this.$t("ui.data.column.mouldusestatus.mouldStatus"),
          render: (form) => {
            return (
              <dict-select
                clearable
                v-model={form.mouldStatus}
                options={this.dict.type.biz_available_status}
              />
            );
          },
        },
        {
          prop: "mouldCode",
          label: this.$t("ui.data.column.mouldusestatus.mouldCode"),
        },
      ];
    },
  },
  methods: {
    handleAdd() {
      if (this.$refs.infoRef) {
        this.$refs.infoRef.show();
      }
    },
    handleEdit(row) {
      if (this.$refs.infoRef) {
        this.$refs.infoRef.show(row);
      }
    },
    handleCopy(row) {
      if (this.$refs.copyRef) {
        this.$refs.copyRef.show(row);
      }
    },
    handleDelete(rows) {
      this.$confirm(this.$t("common.confirm.delete"), {
        type: "warning",
      }).then(() => {
        const ids = rows.map((row) => row.id).join(",");
        console.log(ids);
        removeMouldusestatus({ ids }).then((data) => {
          this.$modal.msgSuccess(data.msg);
          this.$set(this.page, "current", 1);
          this.getList();
        });
      });
    },
    handleChangeStatus(status, row) {
      console.log(status);
      let label =
        status === "0"
          ? this.$t("ui.biz.alter.isOpen")
          : this.$t("ui.biz.alter.isStop");

      this.$confirm(label, {
        type: "warning",
      }).then(async () => {
        try {
          this.loading = true;
          const res = await editMachine({
            ...row,
            status,
          });
          this.$modal.msgSuccess(res.msg);
          this.getList();
        } catch (error) {
          this.loading = false;
        }
      });
    },

    handleSearch(data) {
      this.query = data;
      this.$set(this.page, "current", 1);
      this.getList();
    },
    handlePageChange(current, pageSize) {
      this.$set(this.page, "current", current);
      this.$set(this.page, "pageSize", pageSize);
      this.getList();
    },
    handelSuccess() {
      this.getList();
    },
    handleSortChange({ column, prop, order }) {
      if (order) {
        this.sort = {
          orderByColumn: prop,
          isAsc: order == "ascending" ? "asc" : "desc",
        };
      } else {
        //默认排序
        this.sort = {};
      }
      this.getList();
    },

    handleExport() {
      downloadLink("/lean/mouldusestatus/export", this.formatParams(false));
    },

    handleSelectionChange(rows) {
      this.selection = rows;
    },

    // utils
    formatParams(hasPage = true) {
      const params = {
        ...this.query,
        ...this.sort,
      };

      if (hasPage) {
        params.pageSize = this.page.pageSize;
        params.pageNum = this.page.current;
      }
      if(params.yearMonth) {
          let arr = params.yearMonth.split("-");
        params.year = arr[0];
        params.month = arr[1];
        params.yearMonth = undefined;
      }


      if (params.createTime && params.createTime[0]) {
        params.createTimeStart = params.createTime[0];
        params.createTimeEnd = params.createTime[1];
        params.createTime = undefined;
      }

      return params;
    },
    // api
    async getList() {
      try {
        this.loading = true;
        const data = await listMouldusestatus(this.formatParams());
        console.log(data);
        this.data = data.rows;
        this.page.total = data.total;
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
    },

    async publishSchedule() {
      try {
        this.loading = true;
        const valid = await publishValidate();
        if (valid.msg == "0") {
          this.$confirm(
            this.$t("ui.data.column.scheduleResult.hasNullLhMachineCode")
          ).then(async () => {
            const result = await publishScheduleResult();
            this.$emit("success");
            this.hide();
          });
        } else {
          const result = await publishScheduleResult();
          this.$emit("success");
          this.hide();
        }
      } catch (error) {
        console.error(error);
      }
    },
  },
  created() {},
  activated() {
    this.getList();
  },
};
</script>
<style lang="scss" scoped>
.more-btn {
  margin: 2px 0;
  width: 100%;
}
</style>
