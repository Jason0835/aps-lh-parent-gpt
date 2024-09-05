
<template>
  <basic-container>
    <page-table
      tableRef="reportOrderStatisticsTable"
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
  listReportOrderStatistics,
  exportReportOrderStatistics,
} from "@/api/cx/reportOrderStatistics";
export default {
  name: "reportOrderStatistics",
  components: {},
  dicts: [
    // "sys_yes_no"
    "PROCEDURE_CODE",
  ],
  data() {
    let nowDate = moment().format("YYYY-MM-DD");
    return {
      dailyPlanVisiable: false,
      columns: [
        // {
        //   label: this.$t("ui.data.column.reportOrderStatistics.modelName"),
        //   children: [
        {
          label: this.$t("ui.data.column.reportOrderStatistics.scheduleDate"),
          prop: "scheduleDate",
          minWidth: 150,
          sortable: "custom",
          formatter: (row) => {
            return row.scheduleDate || "-";
          },
        },
        {
          label: this.$t("ui.data.column.reportOrderStatistics.procedureCode"),
          prop: "procedureCode",
          minWidth: 80,
          sortable: "custom",
          render: ({ row }) => {
            return (
              <dict-tag
                value={row.procedureCode}
                options={this.dict.type.PROCEDURE_CODE}
              />
            );
          },
        },
        {
          label: this.$t("ui.data.column.reportOrderStatistics.orderNo"),
          prop: "orderNo",
          minWidth: 80,
          sortable: "custom",
        },
        {
          label: this.$t("ui.data.column.reportOrderStatistics.specCode"),
          prop: "specCode",
          minWidth: 80,
          sortable: "custom",
        },
        {
          label: this.$t(
            "ui.data.column.reportOrderStatistics.planProductionQty"
          ),
          prop: "planProductionQty",
          minWidth: 100,
          sortable: "custom",
        },
        {
          label: this.$t(
            "ui.data.column.reportOrderStatistics.actualFinishQty"
          ),
          prop: "actualFinishQty",
          minWidth: 100,
          sortable: "custom",
        },
        {
          label: this.$t("ui.data.column.reportOrderStatistics.finishRate"),
          prop: "finishRate",
          minWidth: 80,
          sortable: "custom",
        },
        //   ],
        // },
      ],
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
          label: this.$t("ui.data.column.scheduleResult.scheduleDate"),
          prop: "scheduleDate",
          type: "date",
          dateType: "daterange",
          valueFormat: "yyyy-MM-dd",
          clearable: false,
        },
        {
          label: this.$t("ui.data.column.reportStatistics.statisticalMethod"),
          prop: "statisticalMethod",
          render: (form) => {
            return (
              <dict-select
                v-model={form.statisticalMethod}
                options={[
                  {
                    label: this.$t("ui.data.column.reportStatistics.everyDay"),
                    value: 1,
                  },
                  {
                    label: this.$t("ui.data.column.reportStatistics.summary"),
                    value: 2,
                  },
                ]}
              />
            );
          },
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
        scheduleDate: [nowDate, nowDate],
        statisticalMethod: 1,
      },
      query: {
        scheduleDate: [nowDate, nowDate],
        statisticalMethod: 1,
      },
    };
  },
  computed: {},
  methods: {
      tableRowClassName({ row, rowIndex }) {
      if (row && row.dataType === "2") {
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
      this.$confirm(this.$t(`确定导出所有 每日报表统计？`), {
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
          exportReportOrderStatistics(params);
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

      if (params.scheduleDate && params.scheduleDate[0]) {
        params.startTime = params.scheduleDate[0];
        params.endTime = params.scheduleDate[1];
        params.scheduleDate = undefined;
      }

      return params;
    },
    //
    async getList() {
      try {
        this.loading = true;
        const data = await listReportOrderStatistics(this.formatParams());
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
