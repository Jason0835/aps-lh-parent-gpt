
<template>
  <basic-container>
    <page-table
      tableRef="curingUnScheduleResultMainTable"
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
        <!-- <el-button
          v-hasPermi="['lh:lhUnscheduledResult:add']"
          type="success"
          plain
          @click="handleAdd"
          >{{ $t("ui.frame.btn.add") }}</el-button
        > -->
        <!-- <el-button
          v-hasPermi="['lh:lhLhMoldChangePlan:edit']"
          @click="handleEdit(selection[0])"
          :disabled="selection.length != 1"
          >{{ $t("ui.frame.btn.modify") }}</el-button
        > -->
        <!-- <el-button
          v-hasPermi="['lh:lhUnscheduledResult:remove']"
          type="danger"
          plain
          @click="handleDeleteMulti"
          :disabled="selection.length == 0"
          >{{ $t("ui.frame.btn.delete") }}</el-button
        > -->

        <!-- <el-button
          v-hasPermi="['lh:lhUnscheduledResult:import']"
          @click="() => $refs.tltUploadForm.handleImport(importDefaultValue)"
          >{{ $t("ui.frame.btn.import") }}</el-button
        > -->
        <el-button
          v-hasPermi="['lh:lhUnscheduledResult:export']"
          @click="handleExport"
          >{{ $t("ui.frame.btn.export") }}</el-button
        >
      </template>
    </page-table>
    <!-- <tlt-upload-form
      ref="tltUploadForm"
      title="导入"
      downloadUrl="/lh/lhLhMoldChangePlan/importTemplate"
      uploadUrl="/lh/lhLhMoldChangePlan/importData"
      @uploadSuccess="getList"
      :columns="importColumns"
      :rules="importRules"
    /> -->
    <InfoDialog ref="infoDialogRef" @success="handelSuccess" />
  </basic-container>
</template>
<script>
import moment from "moment";
import { mapState } from "vuex";

import { listLhUnscheduledResult } from "@/api/lh/lhUnscheduledResult.js";

// import TltUploadForm from "@/views/components/tltUploadForm.vue";
import InfoDialog from "./components/infoDialog.vue";

export default {
  name: "CuringUnschedule",
  components: {
    InfoDialog,
    // TltUploadForm,
  },
  dicts: ["biz_factory_name"],
  provide() {
    return {
      parentDict: this.dict,
    };
  },
  data() {
    return {
      importDefaultValue: {},
      importColumns: [],
      importRules: {},

      loading: false,
      data: [],
      page: {
        current: 1,
        pageSize: 20,
        total: 0,
      },
      sort: {},
      search: {},
      query: {},
      selection: [],
    };
  },
  computed: {
    // ...mapState({
    //   curingMachines: (state) => state.curing.machines,
    // }),
    columns() {
      const columns = [
        // { type: "selection", fixed: "left" },
        // { type: "index", fixed: "left" },
        {
          label: this.$t("ui.data.column.unscheduleResult.factoryCode"),
          prop: "factoryCode",
          formatter: (row, column, value, index) => {
            return this.selectDictLabel(this.dict.type.biz_factory_name, value);
          },
        },
        {
          label: this.$t("ui.data.column.unscheduleResult.scheduleDate"),
          prop: "scheduleDate",
          width: 160,
        },
        {
          label: this.$t("ui.data.column.unscheduleResult.batchNo"),
          prop: "batchNo",
          width: 180,
        },
        {
          label: this.$t("ui.data.column.unscheduleResult.embryoCode"),
          prop: "embryoCode",
        },
        {
          label: this.$t("ui.data.column.unscheduleResult.embryoStock"),
          prop: "embryoStock",
          type: "number",
        },
        {
          label: this.$t("ui.data.column.unscheduleResult.monthPlanNo"),
          prop: "monthPlanNo",
          width: 200,
        },
        {
          label: this.$t("ui.data.column.unscheduleResult.mpMoldQty"),
          prop: "mpMoldQty",
        },
        {
          label: this.$t("ui.data.column.unscheduleResult.productCode"),
          prop: "productCode",
        },
        {
          label: this.$t("ui.data.column.unscheduleResult.specCode"),
          prop: "specCode",
        },
        {
          label: this.$t("ui.data.column.unscheduleResult.specDesc"),
          prop: "specDesc",
          width: 250,
        },
        {
          label: this.$t("ui.data.column.unscheduleResult.unscheduledQty"),
          prop: "unscheduledQty",
          type: "number",
        },
        {
          label: this.$t("ui.data.column.unscheduleResult.unscheduledReason"),
          prop: "unscheduledReason",
          width: 300,
        },
      ];
      if (this.$auth.hasPermi("lh:lhScheduleResult:insertOrder")) {
        columns.push({
          align: "center",
          width: 100,
          fixed: "right",
          label: this.$t("ui.data.btn.option"),
          render: ({ row }) => {
            return (
              <el-button
                class="minus"
                type="success"
                onClick={() => this.handleEdit(row)}
              >
                {this.$t("ui.data.column.scheduleResult.insertOrder")}
              </el-button>
            );
          },
        });
      }
      return columns;
    },
    searchColumns() {
      return [
        {
          label: this.$t("ui.data.column.unscheduleResult.scheduleDate"),
          prop: "scheduleDate",
          type: "date",
          dateType: "date",
          valueFormat: "yyyy-MM-dd",
          clearable: false,
        },
        {
          label: this.$t("ui.data.column.unscheduleResult.batchNo"),
          prop: "batchNo",
        },
        {
          label: this.$t("ui.data.column.unscheduleResult.factoryCode"),
          prop: "factoryCode",
          type: "select",
          dictData: this.dict.type.biz_factory_name,
        },
        {
          label: this.$t("ui.data.column.unscheduleResult.productCode"),
          prop: "productCode",
        },
        {
          label: this.$t("ui.data.column.unscheduleResult.specCode"),
          prop: "specCode",
        },
        {
          label: this.$t("ui.data.column.unscheduleResult.specDesc"),
          prop: "specDesc",
        },
        {
          label: this.$t("ui.data.column.unscheduleResult.unscheduledReason"),
          prop: "unscheduledReason",
        },
      ];
    },
  },
  methods: {
    handleAdd() {
      if (this.$refs.addDialogRef) {
        this.$refs.addDialogRef.show();
      }
    },
    handleEdit(row) {
      if (this.$refs.infoDialogRef) {
        this.$refs.infoDialogRef.show(row);
      }
    },

    handleDelete(row) {
      this.$confirm(this.$t("common.confirm.delete"), {
        type: "warning",
      }).then(() => {
        const ids = row.id;
        removeLhMoldChangePlan({ ids }).then((data) => {
          this.$modal.msgSuccess(data.msg);
          this.$set(this.page, "current", 1);
          this.getList();
        });
      });
    },
    handleDeleteMulti() {
      // if (this.selection.length == 0) {
      //   this.$modal.msgWarning(this.$t("请至少选择一条记录"));
      //   return;
      // }
      this.$confirm(this.$t("common.confirm.delete"), { type: "warning" })
        .then(async () => {
          //确认提交
          try {
            this.loading = true;
            let ids = [];
            this.selection.forEach((element) => {
              ids.push(element.id);
            });
            const params = {
              ids: ids.join(),
            };
            const data = await removeLhMoldChangePlan(params);
            this.$modal.msgSuccess(data.msg);
            this.$set(this.page, "current", 1);
            this.getList();
          } catch (error) {
          } finally {
            this.loading = false;
          }
        })
        .catch(() => {});
    },
    handleExport() {
      this.$confirm(this.$t(`确定导出？`), {
        type: "warning",
      }).then(() => {
        try {
          this.loading = true;
          let params = this.formatParams();
          params = {
            ...params,
            pageSize: undefined,
            pageNum: undefined,
          };
          exportLhMoldChangePlan(params);
        } catch (error) {
          console.error(error);
        } finally {
          this.loading = false;
        }
      });
    },
    handlePublish() {
      this.$confirm(this.$t(`确认要发布排程吗？`), {
        type: "warning",
      }).then(async () => {
        try {
          this.loading = true;
          let ids = [];
          this.selection.forEach((element) => {
            ids.push(element.id);
          });
          const params = {
            planDate: this.query.planDate,
            ids: ids.join(),
          };
          const data = await publishLhMoldChangePlan(params);
          this.$modal.msgSuccess(data.msg);
          this.$set(this.page, "current", 1);
          this.getList();
        } catch (error) {
          console.error(error);
        } finally {
          this.loading = false;
        }
      });
    },
    handleConfirm(row) {
      this.$confirm(this.$t(`是否确认？`), {
        type: "warning",
      }).then(async () => {
        try {
          this.loading = true;
          const data = await confirmAdjust({ id: row.id });
          this.$modal.msgSuccess(data.msg);
          // this.$set(this.page, "current", 1);
          this.getList();
        } catch (error) {
          console.error(error);
        } finally {
          this.loading = false;
        }
      });
    },

    handleChangeExecute() {
      if (this.$refs.changeExecuteDialogRef) {
        let ids = [];
        this.selection.forEach((element) => {
          ids.push(element.id);
        });
        this.$refs.changeExecuteDialogRef.show(ids.join());
      }
    },
    handleQuery() {},
    handleHistoryQuery() {},

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
    handleSelectionChange(rows) {
      this.selection = rows;
    },

    //util
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

    formatParams() {
      const params = {
        pageSize: this.page.pageSize,
        pageNum: this.page.current,
        ...this.query,
        ...this.sort,
      };

      if (params.createTime && params.createTime[0]) {
        params.createTimeStart = params.createTime[0];
        params.createTimeEnd = params.createTime[1];
        params.createTime = undefined;
      }

      return params;
    },
    //
    async getList() {
      try {
        this.loading = true;
        const data = await listLhUnscheduledResult(this.formatParams());

        this.data = data.rows;
        this.page.total = data.total;
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
    },
  },
  created() {
    // if (!this.curingMachines.length) {
    //   this.$store.dispatch("curing/getMachineList");
    // }
    if (this.$route.query.scheduleDate) {
      const date = this.$route.query.scheduleDate;
      this.search.scheduleDate = date;
      this.query.scheduleDate = date;
    }
    if (this.$route.query.batchNo) {
      const batchNo = this.$route.query.batchNo;
      this.search.batchNo = batchNo;
      this.query.batchNo = batchNo;
    }
  },
  activated() {
    this.getList();
  },
};
</script>
