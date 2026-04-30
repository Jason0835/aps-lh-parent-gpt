
<template>
  <basic-container>
    <page-table
      tableRef="reportStatisticsTable"
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
import moment from "moment";
import {
  listReportStatistics,
  exportReportStatistics,
} from "@/api/cx/reportStatistics";
export default {
  name: "reportStatistics",
  components: {},
  dicts: [
    // "sys_yes_no"
    "PROCEDURE_CODE",
  ],
  data() {
    let nowDate = moment().format("YYYY-MM-DD");
    return {
      columns: [
        // {
        //   label: this.$t("ui.data.column.reportStatistics.title"),
        //   children: [
        {
          label: this.$t("ui.data.column.scheduleResult.scheduleDate"),
          prop: "scheduleDate",
          minWidth: 100,
          // sortable: "custom",
          formatter: (row) => {
            return row.scheduleDate || "-";
          },
        },
        {
          label: this.$t("ui.data.column.reportStatistics.procedureCode"),
          prop: "procedureCode",
          minWidth: 100,
          // sortable: "custom",
          formatter: (row, column, value) => {
            if (row.procedureCode === "-1") {
              return this.$t("ui.data.column.reportStatistics.halfParts");
            }
            return this.selectDictLabel(this.dict.type.PROCEDURE_CODE, value);
          },
        },
        {
          label: this.$t("ui.data.column.reportStatistics.planProductionQty"),
          prop: "planProductionQty",
          minWidth: 100,
          // sortable: "custom",
        },
        {
          label: this.$t("ui.data.column.reportStatistics.actualProductionQty"),
          prop: "actualProductionQty",
          minWidth: 100,
          // sortable: "custom",
        },
        {
          label: this.$t("ui.data.column.reportStatistics.produceFinishRate"),
          prop: "produceFinishRate",
          minWidth: 100,
          // sortable: "custom",
        },
        {
          label: this.$t(
            "ui.data.column.reportStatistics.actualProFinishRateLow"
          ),
          prop: "actualProFinishRateLow",
          minWidth: 100,
          // sortable: "custom",
        },
        {
          label: this.$t(
            "ui.data.column.reportStatistics.actualProFinishRateMid"
          ),
          prop: "actualProFinishRateMid",
          minWidth: 100,
          // sortable: "custom",
        },
        {
          label: this.$t(
            "ui.data.column.reportStatistics.actualProFinishRateHigh"
          ),
          prop: "actualProFinishRateHigh",
          minWidth: 100,
          // sortable: "custom",
        },
        {
          label: this.$t("ui.data.column.reportStatistics.shiftPlanAccuracy"),
          prop: "shiftPlanAccuracy",
          minWidth: 100,
          // sortable: "custom",
          formatter(row) {
            return row.shiftPlanAccuracy || "-";
          },
        },
        {
          label: this.$t("ui.data.column.reportStatistics.totalSpecifications"),
          prop: "totalSpecifications",
          minWidth: 100,
          // sortable: "custom",
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
          exportReportStatistics(params);
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
        const data = await listReportStatistics(this.formatParams());
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
  mounted() {},
  activated() {
    this.getList();
  },
};
</script>
