
<template>
  <basic-container>
    <page-table
      tableRef="lhMoldChangePlanMainTable"
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
          v-hasPermi="['lh:lhMoldChangePlan:add']"
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
        <el-button
          v-hasPermi="['lh:lhMoldChangePlan:remove']"
          type="danger"
          plain
          @click="handleDeleteMulti"
          :disabled="selection.length == 0"
          >{{ $t("ui.frame.btn.delete") }}</el-button
        >
        <!-- <el-button
          v-hasPermi="['lh:lhLhMoldChangePlan:publish']"
          type="primary"
          @click="handlePublish"
          :disabled="selection.length == 0"
          >{{ $t("ui.data.column.scheduleResult.publish") }}</el-button
        > -->
        <!-- <el-button
          v-hasPermi="['lh:lhMoldChangePlan:import']"
          @click="() => $refs.tltUploadForm.handleImport(importDefaultValue)"
          >{{ $t("ui.frame.btn.import") }}</el-button
        > -->
        <!-- <el-button
            v-hasPermi="['lh:lhMoldChangePlan:generateMoldReplacementPlan']"
            @click="handleGenerateMoldReplacementPlan"
            >{{ $t("生成换模计划") }}</el-button
          > -->
        <el-button
          v-hasPermi="['lh:lhMoldChangePlan:export']"
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
    <!-- <InfoDialog ref="infoDialogRef" @success="handelSuccess" /> -->
    <generateDialog ref="genDialogRef" @success="handelSuccess" />

    <el-button style="display: none" ref="hidePopoverBtnRef"></el-button>
  </basic-container>
</template>
<script>
import moment from "moment";
import { mapState } from "vuex";

import {
  listLhMoldChangePlan,
  exportLhMoldChangePlan,
  publishLhMoldChangePlan,
  removeLhMoldChangePlan,
  generateMoldReplacementPlan,
} from "@/api/lh/lhMoldChangePlan.js";

import generateDialog from "./components/generateDialog.vue";
// import TltUploadForm from "@/views/components/tltUploadForm.vue";

export default {
  name: "LhMoldChangePlan",
  components: {
    // InfoDialog,
    // TltUploadForm,
    generateDialog,
  },
  dicts: ["MOLD_CHANGE_TYPE", "IS_RELEASE", "IS_HAVE", "biz_factory_name"],
  provide() {
    return {
      parentDict: this.dict,
    };
  },
  data() {
    let defaultDate = moment().add(1, "days").format("YYYY-MM-DD"); //明天
    // let defaultDate = "2024-06-01";
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
      search: {
        scheduleDate: defaultDate,
      },
      query: {
        scheduleDate: defaultDate,
      },
      selection: [],
    };
  },
  computed: {
    ...mapState({
      curingMachines: (state) => state.curing.machines,
    }),
    columns() {
      return [
        { type: "selection", fixed: "left" },
        // { type: "index", fixed: "left" },

        {
          label: this.$t("ui.data.column.lhApsMoldAdjustPlan.scheduleDate"),
          prop: "scheduleDate",
          minWidth: 180,
          sortable: "custom",
        },
        // {
        //   label: "换模计划批次号",
        //   prop: "moldBatchNo",
        // },
        {
          label: "批次号",
          prop: "lhResultBatchNo",
          width: 160,
        },

        {
          label: this.$t("ui.data.column.lhApsMoldAdjustPlan.lhMachineName"),
          prop: "lhMachineCode",
          minWidth: 100,
          // sortable: "custom",
        },
        {
          label: this.$t("ui.data.column.scheduleResult.isRelease"),
          prop: "isRelease",
          minWidth: 100,
          // sortable: "custom",
          render: ({ row }) => {
            return this.selectDictLabel(
              this.dict.type.IS_RELEASE,
              row.isRelease
            );
          },
        },
        {
          label: this.$t("ui.data.column.lhApsMoldAdjustPlan.beforeSpecCode"),
          prop: "beforeSpecCode",
          minWidth: 100,
          // sortable: "custom",
        },
        {
          label: this.$t("ui.data.column.lhApsMoldAdjustPlan.beforeSpecDesc"),
          prop: "beforeSpecDesc",
          minWidth: 100,
          width: 250,
          // sortable: "custom",
        },
        // {
        //   label: this.$t("ui.data.column.lhApsMoldAdjustPlan.beforeEmbryoCode"),
        //   prop: "beforeEmbryoCode",
        //   minWidth: 100,
        //   sortable: "custom",
        // },
        // {
        //   label: this.$t("ui.data.column.lhApsMoldAdjustPlan.tireRoughStock"),
        //   prop: "tireRoughStock",
        //   minWidth: 100,
        //   // sortable: "custom",
        //   formatter: (row) => {
        //     return row.tireRoughStock || "-";
        //   },
        // },
        // {
        //   label: this.$t("ui.data.column.lhApsMoldAdjustPlan.useMoldNumber"),
        //   prop: "useMoldNumber",
        //   minWidth: 100,
        //   sortable: "custom",
        // },
        // {
        //   label: this.$t("ui.data.column.lhApsMoldAdjustPlan.leftRightMold"),
        //   prop: "leftRightMold",
        //   minWidth: 100,
        //   sortable: "custom",
        //   formatter: (row) => {
        //     return row.leftRightMold || "-";
        //   },
        // },
        // {
        //   label: this.$t("ui.data.column.lhApsMoldAdjustPlan.changeType"),
        //   prop: "changeType",
        //   minWidth: 100,
        //   // sortable: "custom",
        //   render: ({ row }) => {
        //     return this.selectDictLabel(
        //       this.dict.type.MOLD_CHANGE_TYPE,
        //       row.changeType
        //     );
        //   },
        // },
        {
          label: this.$t("ui.data.column.lhApsMoldAdjustPlan.afterSpecCode"),
          prop: "afterSpecCode",
          minWidth: 100,
          // sortable: "custom",
        },
        {
          label: this.$t("ui.data.column.lhApsMoldAdjustPlan.afterSpecDesc"),
          prop: "afterSpecDesc",
          minWidth: 100,
          width: 250,
          // sortable: "custom",
        },
        // {
        //   label: this.$t("ui.data.column.lhApsMoldAdjustPlan.afterEmbryoCode"),
        //   prop: "afterEmbryoCode",
        //   minWidth: 100,
        //   sortable: "custom",
        // },
        {
          label: this.$t("ui.data.column.lhApsMoldAdjustPlan.changeMoldTime"),
          prop: "changeTime",
          minWidth: 100,
          width: 180,
          // sortable: "custom",
        },
        {
          label: this.$t("ui.common.column.remark"),
          prop: "remark",
          minWidth: 100,
          // sortable: "custom",
          formatter: (row) => {
            return row.remark || "-";
          },
        },
      ];
    },
    searchColumns() {
      return [
        // {
        //   label: this.$t("ui.data.column.lhApsMoldAdjustPlan.planDate"),
        //   prop: "planDate",
        //   type: "date",
        //   dateType: "date",
        //   valueFormat: "yyyy-MM-dd",
        //   clearable: false,
        // },
        {
          label: this.$t("ui.data.column.lhApsMoldAdjustPlan.scheduleDate"),
          prop: "scheduleDate",
          type: "date",
          dateType: "date",
          valueFormat: "yyyy-MM-dd",
        },
        {
          label: this.$t("ui.data.column.lhApsMoldAdjustPlan.lhMachineName"),
          prop: "lhMachineCode",
          type: "select",
          dictData: this.curingMachines,
          valueKey: "machineCode",
          labelKey: "machineCode",
          filterable: true,
        },
        //{
        //  label: this.$t("ui.data.column.lhApsMoldAdjustPlan.changeType"),
        //  prop: "changeType",
        //  render: (form) => {
        //    return (
        //      <dict-select
        //        v-model={form.changeType}
        //        options={this.dict.type.MOLD_CHANGE_TYPE}
        //      />
        //    );
        //  },
        //},
        {
          label: this.$t("ui.data.column.scheduleResult.isRelease"),
          prop: "isRelease",
          render: (form) => {
            return (
              <dict-select
                v-model={form.isRelease}
                options={this.dict.type.IS_RELEASE}
              />
            );
          },
        },
        // {
        //   label: this.$t("ui.data.column.lhApsMoldAdjustPlan.changeMoldTime"),
        //   prop: "changeTime",
        //   type: "date",
        //   dateType: "datetimerange",
        //   defaultTime: ['00:00:00', '23:59:59'],
        //   // valueFormat: "yyyy-MM-dd",
        // },
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
    handleGenerateMoldReplacementPlan() {
      if (this.$refs.genDialogRef) {
        this.$refs.genDialogRef.show();
      }
      // this.$confirm(this.$t(`确认生成换模计划？`), {
      //   type: "warning",
      // }).then(async () => {
      //   try {
      //     this.loading = true;
      //     const data = await generateMoldReplacementPlan(row);
      //     this.$modal.msgSuccess(data.msg);
      //     this.$set(this.page, "current", 1);
      //     this.getList();
      //   } catch (error) {
      //     console.error(error);
      //   } finally {
      //     this.loading = false;
      //   }
      // });
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

      if (params.changeTime && params.changeTime[0]) {
        params.changeTimeStart = params.changeTime[0];
        params.changeTimeEnd = params.changeTime[1];
        params.changeTime = undefined;
      }

      return params;
    },
    //
    async getList() {
      try {
        this.loading = true;
        const data = await listLhMoldChangePlan(this.formatParams());

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
    if (!this.curingMachines.length) {
      this.$store.dispatch("curing/getMachineList");
    }
  },
  activated() {
    this.getList();
  },
};
</script>
