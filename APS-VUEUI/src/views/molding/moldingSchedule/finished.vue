
<template>
  <basic-container>
    <page-table
      tableRef="productStatusMainTable"
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
          v-hasPermi="['cx:productStatus:production']"
          type="primary"
          @click="handleModifyQty"
          >{{ $t("ui.data.column.scheduleResult.production") }}</el-button
        >
        <el-button
          v-hasPermi="['cx:cxScheduleResult:export']"
          @click="handleExport"
          >{{ $t("ui.frame.btn.export") }}</el-button
        >
      </template>
    </page-table>
    <editDialog ref="editRef" @success="handelSuccess" />
  </basic-container>
</template>
<script>
//lib
import moment from "moment";

import { downloadLink } from "@/utils/request";

//interface
import { listFinished } from "@/api/cx/cxScheduleResult";
//components
import editDialog from "@/views/molding/productStatus/components/finishedProductionDialog.vue";

export default {
  name: "ProductStatus",
  components: {
    editDialog,
  },
  dicts: [
    "TASK_TYPE",
    "PRODUCTION_STATUS",
    "IS_RELEASE",
    "CLASS_SHIFT",
    "STORAGE_LOCATION",
  ],
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
        scheduleDate: "",
      },
      query: {
        scheduleDate: "",
      },
    };
  },
  computed: {
    columns() {
      let columns = [
        { type: "selection", fixed: "left" },
        {
          label: "基本信息",
          children: [
            {
              prop: "taskType",
              align: "center",
              halign: "center",
              label: this.$t("ui.data.column.cxScheduleResult.taskType"),
              formatter: (row, column, value, index) => {
                return this.selectDictLabel(this.dict.type.TASK_TYPE, value);
              },
            },
            {
              prop: "scheduleDate",
              align: "center",
              halign: "center",
              label: this.$t("ui.data.column.scheduleResult.scheduleDate"),
            },
            {
              prop: "productionStatus",
              align: "center",
              halign: "center",
              label: this.$t(
                "ui.data.column.cxScheduleResult.productionStatus"
              ),
              formatter: (row, column, value, index) => {
                return this.selectDictLabel(
                  this.dict.type.PRODUCTION_STATUS,
                  value
                );
              },
            },
            {
              prop: "isRelease",
              align: "center",
              halign: "center",
              label: this.$t("ui.data.column.scheduleResult.isRelease"),
              formatter: (row, column, value, index) => {
                return this.selectDictLabel(this.dict.type.IS_RELEASE, value);
              },
            },
            {
              prop: "lhMachineCode",
              align: "center",
              halign: "center",
              label: this.$t("ui.data.column.cxScheduleResult.lhMachineCode"),
              formatter: (row, column, value, index) => {
                return this.lhMachineName;
              },
            },
            {
              prop: "remark",
              align: "center",
              halign: "center",
              label: this.$t("ui.data.column.remark"),
              width: 200,
            },
            {
              prop: "lhMachineQty",
              align: "right",
              halign: "center",
              label: this.$t("ui.data.column.cxScheduleResult.lhMachineQty"),
              // formatter: (row, column, value, index) => {
              //     if ($.common.isEmpty(value)) {
              //         value = 0;
              //     }
              //     var actions = [];
              //     if (row.productionStatus == '2') {
              //         actions.push(value);
              //     } else {
              //         actions.push('<a href="javascript:void(0)" onclick="modifyLhMachineQty(\'' + row.id + '\')">' + value + '</a> ');
              //     }
              //     return actions.join('');
              // },
            },
            {
              prop: "minimumLhMachineComQty",
              align: "right",
              halign: "center",
              label: this.$t(
                "ui.data.column.cxScheduleResult.minimumLhMachineReqQty"
              ),
            },
            {
              prop: "availableMoldQty",
              align: "right",
              halign: "center",
              label: this.$t(
                "ui.data.column.cxScheduleResult.availableMoldQty"
              ),
            },
            {
              prop: "specDimension",
              valign: "middle",
              align: "right",
              halign: "center",
              label: this.$t("ui.data.column.cxScheduleResult.specDimension"),
            },
            {
              prop: "cxMachineCode",
              align: "center",
              halign: "center",
              label: this.$t("ui.data.column.cxScheduleResult.cxMachineCode"),
              formatter: (row, column, value, index) => {
                return row.cxMachineName;
              },
            },
            {
              prop: "workShifts",
              align: "center",
              halign: "center",
              label: this.$t("ui.data.column.cxScheduleResult.workShifts"),
              formatter: (row, column, value, index) => {
                return this.selectDictLabel(this.dict.type.CLASS_SHIFT, value);
              },
            },
            {
              prop: "maximumClassQty",
              align: "right",
              halign: "center",
              label: this.$t("ui.data.column.cxScheduleResult.maximumClassQty"),
            },
            {
              prop: "sapCode",
              align: "center",
              halign: "center",
              label: this.$t("ui.data.column.cxScheduleResult.sapCode"),
            },
            {
              prop: "storageLocation",
              align: "center",
              halign: "center",
              label: this.$t("ui.data.column.cxScheduleResult.storageLocation"),
              formatter: (row, column, value, index) => {
                return this.selectDictLabel(
                  this.dict.type.STORAGE_LOCATION,
                  value
                );
              },
            },
            {
              prop: "specDesc",
              align: "left",
              halign: "center",
              label: this.$t("ui.data.column.cxScheduleResult.specDesc"),
            },
            {
              prop: "embryoCode",
              align: "center",
              halign: "center",
              label: this.$t("ui.data.column.cxScheduleResult.embryoCode"),
            },
            {
              prop: "bomDataVersion",
              align: "center",
              halign: "center",
              label: this.$t("ui.data.column.productStatus.bomDataVersion"),
            },
            {
              prop: "totalStock",
              align: "right",
              halign: "center",
              label: this.$t("ui.data.column.cxScheduleResult.totalStock"),
            },
            {
              prop: "lhMiddleNightFinishQty",
              align: "right",
              halign: "center",
              label: this.$t(
                "ui.data.column.cxScheduleResult.lhMiddleNightFinishQty"
              ),
            },
            {
              prop: "class3PlannedQty",
              align: "right",
              halign: "center",
              label: this.$t(
                "ui.data.column.cxScheduleResult.class3PlannedQty"
              ),
            },
            {
              prop: "singleShiftLhQty",
              align: "right",
              halign: "center",
              label: this.$t(
                "ui.data.column.cxScheduleResult.singleShiftLhQty"
              ),
            },
            {
              prop: "cxMonthFinishQty",
              align: "right",
              halign: "center",
              label: this.$t(
                "ui.data.column.cxScheduleResult.cxMonthFinishQty"
              ),
            },
            {
              prop: "monthPlan",
              align: "right",
              halign: "center",
              label: this.$t("ui.data.column.scheduleResult.monthPlan"),
            },
            {
              prop: "planModifyQty",
              align: "right",
              halign: "center",
              label: this.$t("ui.data.column.cxScheduleResult.planModifyQty"),
            },
            {
              prop: "monthPlanOs",
              align: "right",
              halign: "center",
              label: this.$t("ui.data.column.scheduleResult.monthPlanOs"),
            },
            {
              prop: "monthStock",
              align: "right",
              halign: "center",
              label: this.$t("ui.data.column.cxScheduleResult.monthStock"),
            },
            {
              prop: "rejectQty",
              align: "right",
              halign: "center",
              label: this.$t("ui.data.column.cxScheduleResult.rejectQty"),
            },
            {
              prop: "newestPlanQty",
              align: "right",
              halign: "center",
              label: this.$t("ui.data.column.cxScheduleResult.newestPlanQty"),
            },
            {
              prop: "actualOverProduction",
              align: "right",
              halign: "center",
              label: this.$t(
                "ui.data.column.cxScheduleResult.actualOverProduction"
              ),
            },
            {
              prop: "expectedOverProduction",
              align: "right",
              halign: "center",
              label: this.$t(
                "ui.data.column.cxScheduleResult.expectedOverProduction"
              ),
            },
            {
              prop: "differenceOverProduction",
              align: "right",
              halign: "center",
              label: this.$t(
                "ui.data.column.cxScheduleResult.differenceOverProduction"
              ),
            },
          ],
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class1"),
          children: [
            {
              prop: "class1AvailableLhShift",
              align: "right",
              halign: "center",
              label: this.$t(
                "ui.data.column.cxScheduleResult.class1AvailableLhShift"
              ),
            },
            {
              prop: "class1PlanQty",
              align: "right",
              halign: "center",
              label: this.$t("ui.data.column.scheduleResult.plan"),
            },
            {
              prop: "class1FinishQty",
              align: "right",
              halign: "center",
              label: this.$t("ui.data.column.scheduleResult.finish"),
            },
            {
              prop: "class1AnalysisInput",
              halign: "center",
              label: this.$t("ui.data.column.scheduleResult.analysis"),
              formatter: (row, column, value, index) => {
                var reasion = "";
                var SysAnalysis = row.class1Analysis;
                if (value != null) {
                  reasion = reasion + value;
                }
                if (SysAnalysis != null) {
                  if (reasion != "") {
                    reasion = reasion + "," + SysAnalysis;
                  } else {
                    reasion = SysAnalysis;
                  }
                }
                return reasion;
              },
            },
          ],
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class2"),
          children: [
            {
              prop: "class2AvailableLhShift",
              align: "right",
              halign: "center",
              label: this.$t(
                "ui.data.column.cxScheduleResult.class2AvailableLhShift"
              ),
            },
            {
              prop: "class2PlanQty",
              align: "right",
              halign: "center",
              label: this.$t("ui.data.column.scheduleResult.plan"),
            },
            {
              prop: "class2FinishQty",
              align: "right",
              halign: "center",
              label: this.$t("ui.data.column.scheduleResult.finish"),
            },
            {
              prop: "class2AnalysisInput",
              halign: "center",
              label: this.$t("ui.data.column.scheduleResult.analysis"),
              formatter: (row, column, value, index) => {
                var reasion = "";
                var SysAnalysis = row.class2Analysis;
                if (value != null) {
                  reasion = reasion + value;
                }
                if (SysAnalysis != null) {
                  if (reasion != "") {
                    reasion = reasion + "," + SysAnalysis;
                  } else {
                    reasion = SysAnalysis;
                  }
                }
                return reasion;
              },
            },
          ],
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class3"),
          children: [
            {
              prop: "class3AvailableLhShift",
              align: "right",
              halign: "center",
              label: this.$t(
                "ui.data.column.cxScheduleResult.class3AvailableLhShift"
              ),
            },
            {
              prop: "class3PlanQty",
              align: "right",
              halign: "center",
              label: this.$t("ui.data.column.scheduleResult.plan"),
            },
            {
              prop: "class3FinishQty",
              align: "right",
              halign: "center",
              label: this.$t("ui.data.column.scheduleResult.finish"),
            },
            {
              prop: "class3AnalysisInput",
              halign: "center",
              label: this.$t("ui.data.column.scheduleResult.analysis"),
              formatter: (row, column, value, index) => {
                var reasion = "";
                var SysAnalysis = row.class3Analysis;
                if (value != null) {
                  reasion = reasion + value;
                }
                if (SysAnalysis != null) {
                  if (reasion != "") {
                    reasion = reasion + "," + SysAnalysis;
                  } else {
                    reasion = SysAnalysis;
                  }
                }
                return reasion;
              },
            },
          ],
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class4"),
          children: [
            {
              prop: "class4AvailableLhShift",
              align: "right",
              halign: "center",
              label: this.$t(
                "ui.data.column.cxScheduleResult.class4AvailableLhShift"
              ),
            },
            {
              prop: "class4PlanQty",
              align: "right",
              halign: "center",
              label: this.$t("ui.data.column.scheduleResult.plan"),
            },
            {
              prop: "class4FinishQty",
              align: "right",
              halign: "center",
              label: this.$t("ui.data.column.scheduleResult.finish"),
            },
            {
              prop: "class4AnalysisInput",
              halign: "center",
              label: this.$t("ui.data.column.scheduleResult.analysis"),
              formatter: (row, column, value, index) => {
                var reasion = "";
                var SysAnalysis = row.class4Analysis;
                if (value != null) {
                  reasion = reasion + value;
                }
                if (SysAnalysis != null) {
                  if (reasion != "") {
                    reasion = reasion + "," + SysAnalysis;
                  } else {
                    reasion = SysAnalysis;
                  }
                }
                return reasion;
              },
            },
          ],
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class5"),
          children: [
            {
              prop: "class5AvailableLhShift",
              align: "right",
              halign: "center",
              label: this.$t(
                "ui.data.column.cxScheduleResult.class5AvailableLhShift"
              ),
            },
            {
              prop: "class5PlanQty",
              align: "right",
              halign: "center",
              label: this.$t("ui.data.column.scheduleResult.plan"),
            },
            {
              prop: "class5FinishQty",
              align: "right",
              halign: "center",
              label: this.$t("ui.data.column.scheduleResult.finish"),
            },
            {
              prop: "class5AnalysisInput",
              halign: "center",
              label: this.$t("ui.data.column.scheduleResult.analysis"),
              formatter: (row, column, value, index) => {
                var reasion = "";
                var SysAnalysis = row.class5Analysis;
                if (value != null) {
                  reasion = reasion + value;
                }
                if (SysAnalysis != null) {
                  if (reasion != "") {
                    reasion = reasion + "," + SysAnalysis;
                  } else {
                    reasion = SysAnalysis;
                  }
                }
                return reasion;
              },
            },
          ],
        },
      ];

      return columns;
    },
    searchColumns() {
      return [
        {
          label: this.$t("ui.data.column.scheduleResult.scheduleDate"),
          prop: "scheduleDate",
          type: "date",
          dateType: "daterange",
          valueFormat: "yyyy-MM-dd",
        },
        {
          label: this.$t("ui.data.column.cxScheduleResult.taskType"),
          prop: "taskType",
          type: "select",
          dictData: this.dict.type.TASK_TYPE, // "TASK_TYPE",
        },
        {
          label: this.$t("ui.data.column.productStatus.sapCode"),
          prop: "sapCode",
        },
        {
          label: this.$t("ui.data.column.productStatus.embryoCode"),
          prop: "embryoCode",
        },
        {
          label: this.$t("ui.data.column.productStatus.bomDataVersion"),
          prop: "bomDataVersion",
        },
      ];
    },
  },
  methods: {
    handleAdd() {
      if (this.$refs.addRef) {
        this.$refs.addRef.show();
      }
    },
    handleEdit(row) {
      if (this.$refs.editRef) {
        this.$refs.editRef.show(row);
      }
    },
    handleDelete(row) {
      this.$confirm(this.$t("common.confirm.delete"), {
        type: "warning",
      }).then(() => {
        const ids = row.id;
        console.log(ids);
        // removeArea({ ids }).then((data) => {
        //   this.$modal.msgSuccess(data.msg);
        //   this.$set(this.page, "current", 1);
        //   this.getList();
        // });
      });
    },
    handleModifyQty() {
      if (this.$refs.editRef) {
        const row = this.selection[0];
        this.$refs.editRef.show(row, "2");
      }
    },
    handleExport() {
      downloadLink("/cx/productStatus/export", this.formatParams(false));
    },
    handleProduction() {
      if (this.$refs.proRef) {
        let row = this.selection[0];
        this.$refs.proRef.show(row);
      }
    },
    handleMarkUnProduct() {
      this.$confirm(
        this.$t("ui.data.column.productStatus.markUnProduct") + "?"
      ).then(async () => {
        const ids = this.selection.map((row) => row.id).join(",");
        const data = await markUnProduct({ ids });
        this.$modal.msgSuccess(data.msg);
        this.handelSuccess();
      });
    },
    handleClose() {
      this.$tab.closePage().then(() => {
        this.$router.push({
          path: "/moldingPlanManagement/moldingSchedule",
        });
      });
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

    // utils
    updateTableHeaderTitle() {
      //  TODO 更新表头标题
    },

    formatParams(page = true) {
      const params = {
        ...this.query,
        ...this.sort,
      };
      if (page) {
        params.pageSize = this.page.pageSize;
        params.pageNum = this.page.current;
      }

      if (params.scheduleDate && params.scheduleDate[0]) {
        params.startTime = params.scheduleDate[0];
        params.endTime = params.scheduleDate[1];
        params.scheduleDate = undefined;
      }

      return params;
    },
    // api
    async getList() {
      try {
        this.loading = true;
        const data = await listFinished(this.formatParams());
        console.log(data);
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
    //设置默认排程时间
    let date = moment().add(1, "days").format("YYYY-MM-DD");
    //// date = "2023-06-01"; //test

    date = ["2023-07-01", "2023-07-31"];
    this.query.scheduleDate = date;
    this.search.scheduleDate = date;
  },
  mounted() {
    this.getList();
  },
  activated() {
    // this.getList();
  },
};
</script>
<style lang="scss" scoped>
.more-btn {
  margin: 2px 0;
  width: 100%;
}
</style>
