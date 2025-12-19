
<template>
  <basic-container>
    <page-table
      tableRef="SkuSummaryProductMainTable"
      :calcHeight="true"
      v-loading="loading"
      :columns="columns"
      :searchColumns="this.searchColumns"
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
          v-hasPermi="['report:skuSummaryProduce:export']"
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
import {
  listSkuSummaryProduce,
  exportSkuSummaryProduce,
} from "@/api/monthplan/report";
//components

export default {
  name: "SkuSummaryProduce",
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
          label: this.$t("ui.data.column.report.skuSummaryProduce.product"),
          align: "center",
          width: 140,
        },
        {
          prop: "name",
          label: this.$t("ui.data.column.report.skuSummary.month"),
          align: "center",
          width: 250,
        },
        {
          prop: "month1Num",
          label: "1" + this.$t("common.month"),
          align: "right",
          width: 120,
          type: "number",
        },
        {
          prop: "month2Num",
          label: "2" + this.$t("common.month"),
          align: "right",
          width: 120,
        },
        {
          prop: "month3Num",
          label: "3" + this.$t("common.month"),
          align: "right",
          width: 120,
        },
        {
          prop: "month4Num",
          label: "4" + this.$t("common.month"),
          align: "right",
          width: 120,
        },
        {
          prop: "month5Num",
          label: "5" + this.$t("common.month"),
          align: "right",
          width: 120,
        },
        {
          prop: "month6Num",
          label: "6" + this.$t("common.month"),
          align: "right",
          width: 120,
        },
        {
          prop: "month7Num",
          label: "7" + this.$t("common.month"),
          align: "right",
          width: 120,
        },
        {
          prop: "month8Num",
          label: "8" + this.$t("common.month"),
          align: "right",
          width: 120,
        },
        {
          prop: "month9Num",
          label: "9" + this.$t("common.month"),
          align: "right",
          width: 120,
        },
        {
          prop: "month10Num",
          label: "10" + this.$t("common.month"),
          align: "right",
          width: 120,
        },
        {
          prop: "month11Num",
          label: "11" + this.$t("common.month"),
          align: "right",
          width: 120,
        },
        {
          prop: "month12Num",
          label: "12" + this.$t("common.month"),
          align: "right",
          width: 120,
        },
        {
          prop: "month13Num",
          label: this.$t("ui.data.column.report.skuSummary.yearSum"),
          align: "right",
          width: 120,
        },
        {
          prop: "month14Num",
          label: this.$t("ui.data.column.report.skuSummary.monthAvg"),
          align: "right",
          width: 120,
        },
        {
          prop: "month15Num",
          label: this.$t("ui.data.column.report.skuSummaryProduce.chainRatio"),
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
          clearable: false,
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
      exportSkuSummaryProduce(this.formatParams(false));
    },

    // utils
    objectSpanMethod({ row, column, rowIndex, columnIndex }) {
      if (column.property === "product") {
        if (rowIndex % 5 === 0) {
          return {
            rowspan: this.data.length,
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
        const res = await listSkuSummaryProduce(this.formatParams());
        // console.log(res);

        // const keys = Object.keys(res);
        // let keySort = [""];
        let map = {};

        res.rows.forEach((item) => {
          let monthKey = `month${item.month}Num`;
          const itemKeys = Object.keys(item);
          itemKeys.forEach((itemKey) => {
            if (map[itemKey]) {
              map[itemKey][monthKey] = item[itemKey];
            } else {
              map[itemKey] = {};
              map[itemKey][monthKey] = item[itemKey];
            }
          });
        });
        // console.log(map);
        this.data = [
          {
            product: "投产SKU分析",
            name: this.$t(
              "ui.data.column.report.skuSummaryProduce.day1To7Count"
            ),
            ...map["day1To7Count"],
          },
          {
            product: "投产SKU分析",
            name: this.$t(
              "ui.data.column.report.skuSummaryProduce.day8To14Count"
            ),
            ...map["day8To14Count"],
          },
          {
            product: "投产SKU分析",
            name: this.$t(
              "ui.data.column.report.skuSummaryProduce.day15To21Count"
            ),
            ...map["day15To21Count"],
          },
          {
            product: "投产SKU分析",
            name: this.$t(
              "ui.data.column.report.skuSummaryProduce.day22To31Count"
            ),
            ...map["day22To31Count"],
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
