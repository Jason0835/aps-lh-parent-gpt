
<template>
  <basic-container>
    <page-table
      tableRef="reportClassAccuracyTable"
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
      :showSummary="false"
      :selectArea="false"
      :row-class-name="tableRowClassName"
    >
      <template slot="header">
        <el-button type="primary" @click="handleExport">{{
          $t("common.button.export")
        }}</el-button>
      </template>
    </page-table>
  </basic-container>
</template>
<script>
import moment, { months } from "moment";
import {
  listReportClassAccuracy,
  exportReportClassAccuracy,
} from "@/api/cx/reportClassAccuracy";
export default {
  name: "reportClassAccuracy",
  components: {},
  dicts: [
    // "sys_yes_no"
    // "PROCEDURE_CODE",
  ],
  data() {
    let nowDate = moment().format("YYYY-MM-DD");
    return {
      dailyPlanVisiable: false,

      // columns: [
      //   { type: "selection", fixed: "left" },
      //   // { type: "index", fixed: "left" },
      //   // {
      //   //   label: this.$t("common.option"),
      //   //   prop: "option",
      //   //   width: "100px",
      //   //   fixed: "left",
      //   //   render: ({ row }) => {
      //   //     return (
      //   //       <div>
      //   //         <text-button
      //   //           onClick={() => {
      //   //             this.handleEdit(row);
      //   //           }}
      //   //         >
      //   //           {this.$t("common.button.modify")}
      //   //         </text-button>
      //   //       </div>
      //   //     );
      //   //   },
      //   // },

      // ],
      searchColumns: [
        {
          label: this.$t("ui.data.column.reportClassAccuracy.scheduleDate"),
          prop: "scheduleDate",
          type: "date",
          valueFormat: "yyyy-MM-dd",
          clearable: false,
        },
      ],
      loading: false,
      data: [],
      page: {
        current: 1,
        pageSize: 20,
        total: 0,
      },
      sort: {},
      search: {
        scheduleDate: nowDate,
        statisticalMethod: 1,
      },
      query: {
        scheduleDate: nowDate,
        statisticalMethod: 1,
      },
    };
  },
  computed: {
    columns() {
      let firstTitle = this.$t("ui.data.column.reportClassAccuracy.firstTitle",[0,0,0,0])
      if (this.data.length > 0) {
            var first = this.data[0];
            firstTitle = this.$t("ui.data.column.reportClassAccuracy.firstTitle", [first.planSpecNum, first.planTotalNum, first.actualSpecNum, first.actualTotalNum]);
        }
      let columns =  [
        {
          label: firstTitle,
          children: [
            {
              label: this.$t(
                "ui.data.column.reportClassAccuracy.procedureCode"
              ),
              prop: "procedureCode",
              minWidth: 100,
              sortable: "custom",
              render: ({ row }) => {
                return (
                  <dict-tag
                    value={row.procedureCode}
                    // options={this.dict.type.PROCEDURE_CODE}
                    options={[
                      {
                        label: `硫化${row.isSummary ? "汇总" : ""}`,
                        value: "0",
                        raw: {},
                      },
                      {
                        label: `成型${row.isSummary ? "汇总" : ""}`,
                        value: "1",
                        raw: {},
                      },
                      {
                        label: `胎面${row.isSummary ? "汇总" : ""}`,
                        value: "2",
                        raw: {},
                      },
                      {
                        label: `半部件${row.isSummary ? "汇总" : ""}`,
                        value: "-1",
                        raw: {},
                      },
                    ]}
                  />
                );
              },
            },
            {
              label: this.$t("ui.data.column.reportClassAccuracy.class1Title"),
              children: [
                {
                  label: this.$t(
                    "ui.data.column.reportClassAccuracy.planMaterial"
                  ),
                  prop: "class3PlanMaterial",
                  minWidth: 100,
                  sortable: "custom",
                },
                {
                  label: this.$t("ui.data.column.reportClassAccuracy.plan"),
                  prop: "planClass3",
                  minWidth: 100,
                  sortable: "custom",
                },
                {
                  label: this.$t(
                    "ui.data.column.reportClassAccuracy.actualMaterial"
                  ),
                  prop: "class3ActualMaterial",
                  minWidth: 100,
                  sortable: "custom",
                },
                {
                  label: this.$t("ui.data.column.reportClassAccuracy.actual"),
                  prop: "actualClass3",
                  minWidth: 100,
                  sortable: "custom",
                },
              ],
            },
            {
              label: this.$t("ui.data.column.reportClassAccuracy.class2Title"),
              children: [
                {
                  label: this.$t(
                    "ui.data.column.reportClassAccuracy.planMaterial"
                  ),
                  prop: "class1PlanMaterial",
                  minWidth: 100,
                  sortable: "custom",
                },
                {
                  label: this.$t("ui.data.column.reportClassAccuracy.plan"),
                  prop: "planClass1",
                  minWidth: 100,
                  sortable: "custom",
                },
                {
                  label: this.$t(
                    "ui.data.column.reportClassAccuracy.actualMaterial"
                  ),
                  prop: "class1ActualMaterial",
                  minWidth: 100,
                  sortable: "custom",
                },
                {
                  label: this.$t("ui.data.column.reportClassAccuracy.actual"),
                  prop: "actualClass1",
                  minWidth: 100,
                  sortable: "custom",
                },
              ],
            },
            {
              label: this.$t("ui.data.column.reportClassAccuracy.class3Title"),
              children: [
                {
                  label: this.$t(
                    "ui.data.column.reportClassAccuracy.planMaterial"
                  ),
                  prop: "class2PlanMaterial",
                  minWidth: 100,
                  sortable: "custom",
                },
                {
                  label: this.$t("ui.data.column.reportClassAccuracy.plan"),
                  prop: "planClass2",
                  minWidth: 100,
                  sortable: "custom",
                },
                {
                  label: this.$t(
                    "ui.data.column.reportClassAccuracy.actualMaterial"
                  ),
                  prop: "class2ActualMaterial",
                  minWidth: 100,
                  sortable: "custom",
                },
                {
                  label: this.$t("ui.data.column.reportClassAccuracy.actual"),
                  prop: "actualClass2",
                  minWidth: 100,
                  sortable: "custom",
                },
              ],
            },
          ],
        },
      ];
      return columns;
    },
  },
  methods: {
    tableRowClassName({ row, rowIndex }) {
      if (row && row.isSummary === 1) {
        return "yellow-row";
      }
      return "";
    },
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
    handleExport() {
      this.$confirm(this.$t(`确定导出所有 班次完成统计报表？`), {
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
          exportReportClassAccuracy(params);
        } catch (error) {
          console.error(error);
        } finally {
          this.loading = false;
        }
      });
    },
    handleDelete(row) {
      this.$confirm(this.$t("common.confirm.delete"), {
        type: "warning",
      }).then(() => {
        const ids = row.id;
        removeArea({ ids }).then((data) => {
          this.$modal.msgSuccess(data.msg);
          this.$set(this.page, "current", 1);
          this.getList();
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

    //util
    handleSortChange({ column, prop, order }) {
      if (order) {
        this.sort = {
          orderBy: prop,
          isAsc: order == "ascending",
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
        params: {
          ...this.sort,
        },
      };

      return params;
    },
    //
    async getList() {
      try {
        this.loading = true;
        const data = await listReportClassAccuracy(this.formatParams());
        // const data = await this.$axios.get("monthPlan/monthProductionPlan/list");

        this.data = data.rows;
        this.page.total = data.total;
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
    },
  },
  mounted() {

  },
  activated() {
    this.getList();
  },
};
</script>
