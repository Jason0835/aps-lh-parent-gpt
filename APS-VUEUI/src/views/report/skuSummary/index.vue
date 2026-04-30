
<template>
  <basic-container>
    <page-table
      tableRef="SkuSummaryMainTable"
      :calcHeight="true"
      v-loading="loading"
      :columns="columns"
      :searchColumns="searchColumns"
      :data="data"
      :page="undefined"
      :search="search"
      @refresh="getList"
      @search="handleSearch"
      @pageChange="handlePageChange"
      @sort-change="handleSortChange"
      :showSummary="false"
      :selectArea="false"
      :span-method="objectSpanMethod"
    >
      <template slot="header">
        <el-button
          @click="handleExport"
          v-hasPermi="['report:skuSummary:export']"
          >{{ $t("ui.frame.btn.export") }}</el-button
        >
      </template>
      <template slot="headerRight"> </template>
    </page-table>
  </basic-container>
</template>
<script>
//lib
import moment from "moment";
import Big from "big.js";
//utils
// import { downloadLink } from "@/utils/request";
//interface
import { listSkuSummary, exportSkuSummary } from "@/api/monthplan/report";
//components

export default {
  name: "SkuSummary",
  components: {
    // tltUpload,
  },
  dicts: ["biz_factory_name", "unit"],
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
      search: {},
      query: {},
      stat: {
        produceFinishPlanQty: null,
        produceFinishRate: null,
        producePlanQty: null,
        saleFinishPlanQty: null,
        saleFinishRate: null,
        salePlanQty: null,
      },
    };
  },
  computed: {
    columns() {
      let columns = [
        {
          prop: "product",
          label: this.$t("ui.data.column.report.skuSummary.product"),
          align: "center",
        },
        {
          prop: "name",
          label: this.$t("ui.data.column.report.skuSummary.month"),
          align: "center",
          width: 200,
        },
        {
          prop: "month1",
          label: "1" + this.$t("common.month"),
          align: "right",
          width: 120,
        },
        {
          prop: "month2",
          label: "2" + this.$t("common.month"),
          align: "right",
          width: 120,
        },
        {
          prop: "month3",
          label: "3" + this.$t("common.month"),
          align: "right",
          width: 120,
        },
        {
          prop: "month4",
          label: "4" + this.$t("common.month"),
          align: "right",
          width: 120,
        },
        {
          prop: "month5",
          label: "5" + this.$t("common.month"),
          align: "right",
          width: 120,
        },
        {
          prop: "month6",
          label: "6" + this.$t("common.month"),
          align: "right",
          width: 120,
        },
        {
          prop: "month7",
          label: "7" + this.$t("common.month"),
          align: "right",
          width: 120,
        },
        {
          prop: "month8",
          label: "8" + this.$t("common.month"),
          align: "right",
          width: 120,
        },
        {
          prop: "month9",
          label: "9" + this.$t("common.month"),
          align: "right",
          width: 120,
        },
        {
          prop: "month10",
          label: "10" + this.$t("common.month"),
          align: "right",
          width: 120,
        },
        {
          prop: "month11",
          label: "11" + this.$t("common.month"),
          align: "right",
          width: 120,
        },
        {
          prop: "month12",
          label: "12" + this.$t("common.month"),
          align: "right",
          width: 120,
        },
        {
          prop: "yearSum",
          label: this.$t("ui.data.column.report.skuSummary.yearSum"),
          align: "right",
          width: 120,
        },
        {
          prop: "monthAvg",
          label: this.$t("ui.data.column.report.skuSummary.monthAvg"),
          align: "right",
          width: 120,
        },
        {
          prop: "currentMonthAvgDiff",
          label: this.$t(
            "ui.data.column.report.skuSummary.currentMonthAvgDiff"
          ),
          align: "right",
          width: 120,
          formatter: (row, column, value) => {
            return value
              ? Big(value).times(100).toString() + "%"
              : value === 0
              ? "0%"
              : "";
          },
        },
      ];

      return columns;
    },
    searchColumns() {
      return [
        {
          label: this.$t("ui.data.colume.yearMonth"),
          prop: "yearMonth",
          type: "date",
          dateType: "month",
          valueFormat: "yyyy-MM",
          clearable: false,
        },
        {
          label: this.$t("ui.data.column.scheduleAdjust.factoryCode"),
          prop: "factoryCode",
          type: "select",
          dictData: this.dict.type.biz_factory_name,
        },
      ];
    },
  },
  methods: {
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
      exportSkuSummary(this.formatParams(false));
    },

    // utils
    objectSpanMethod({ row, column, rowIndex, columnIndex }) {
      if (column.property === "product") {
        if (rowIndex % 5 === 0) {
          return {
            rowspan: 5,
            colspan: 1,
          };
        } else {
          return {
            rowspan: 0,
            colspan: 0,
          };
        }
      }
    },
    formatParams(hasPage = true) {
      const params = {
        ...this.query,
        ...this.sort,
      };

      if (hasPage) {
        // params.pageSize = this.page.pageSize;
        // params.pageNum = this.page.current;
      }
      if (params.yearMonth) {
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
        const res = await listSkuSummary(this.formatParams());
        console.log(res);
        // console.log()

        const keys = Object.keys(res);
        let keySort = [""];
        let map = {};

        keys.forEach((key) => {
          let item = res[key];
          const itemKeys = Object.keys(item);
          itemKeys.forEach((itemKey) => {
            if (map[itemKey]) {
              map[itemKey][key] = item[itemKey];
            } else {
              map[itemKey] = {};
              map[itemKey][key] = item[itemKey];
            }
          });
        });
        // console.log(map);
        this.data = [
          {
            product: "计划",
            name: this.$t("ui.data.column.report.skuSummary.produceTotal"),
            ...map["produceTotal"],
          },
          {
            product: "计划",
            name: this.$t("ui.data.column.report.skuSummary.planDay"),
            ...map["planDay"],
          },
          {
            product: "计划",
            name: this.$t("ui.data.column.report.skuSummary.avgDailyProduce"),
            ...map["avgDailyProduce"],
          },
          {
            product: "计划",
            name: this.$t("ui.data.column.report.skuSummary.produceSkuCount"),
            ...map["produceSkuCount"],
          },
          {
            product: "计划",
            name: this.$t(
              "ui.data.column.report.skuSummary.produceAvgSkuCount"
            ),
            ...map["produceAvgSkuCount"],
          },
          {
            product: "完成",
            name: this.$t("ui.data.column.report.skuSummary.finishTotal"),
            ...map["finishTotal"],
          },
          {
            product: "完成",
            name: this.$t("ui.data.column.report.skuSummary.actualDay"),
            ...map["actualDay"],
          },
          {
            product: "完成",
            name: this.$t("ui.data.column.report.skuSummary.avgDailyFinish"),
            ...map["avgDailyFinish"],
          },
          {
            product: "完成",
            name: this.$t("ui.data.column.report.skuSummary.finishSkuCount"),
            ...map["finishSkuCount"],
          },
          {
            product: "完成",
            name: this.$t("ui.data.column.report.skuSummary.finishAvgSkuCount"),
            ...map["finishAvgSkuCount"],
          },
        ];

        // this.data = list;
        // this.page.total = data.total;
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
    },
  },
  created() {
    const date = moment();
    this.search = {
      yearMonth: date.format("yyyy-MM"),
      factoryCode: "",
    };
    this.query = {
      yearMonth: date.format("yyyy-MM"),
      factoryCode: "",
    };
  },
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
<style lang="scss" scoped>
.stat-info {
  font-size: 12px;
  color: #5f5858;
  span {
    margin-left: 5px;
  }
}
</style>