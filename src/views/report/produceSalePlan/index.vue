
<template>
  <basic-container>
    <page-table
      tableRef="ProduceSalePlanMainTable"
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
      :summary-method="getSummaryMethod"
    >
      <template slot="header">
        <el-button
          @click="handleExport"
          v-hasPermi="['report:produceSalePlan:export']"
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
  listProduceSalePlan,
  exportProduceSalePlan,
} from "@/api/monthplan/report";
//components

export default {
  name: "ProduceSalePlan",
  components: {
    // tltUpload,
  },
  dicts: ["biz_factory_name"],
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
      stat: {},
    };
  },
  computed: {
    columns() {
      let columns = [
        {
          prop: "channelName",
          label: this.$t("ui.data.column.report.produceSalePlan.channelName"),
          align: "center",
        },
        {
          prop: "proSize",
          label: this.$t("ui.data.column.report.produceSalePlan.proSize"),
          align: "center",
          width: 120,
          formatter: (row, column, value) => {
            return value ? value : "合计";
          },
        },
        {
          prop: "singleTireWeight",
          label: this.$t(
            "ui.data.column.report.produceSalePlan.singleTireWeight"
          ),
          type: "number",
        },
        {
          prop: "monthProduceQty",
          label: this.$t(
            "ui.data.column.report.produceSalePlan.monthProduceQty"
          ),
          type: "number",
        },
        {
          prop: "monthProduceWeight",
          label: this.$t(
            "ui.data.column.report.produceSalePlan.monthProduceWeight"
          ),
          type: "number",
        },
        {
          prop: "monthProduceQtyProportion",
          label: this.$t(
            "ui.data.column.report.produceSalePlan.monthProduceQtyProportion"
          ),
          type: "number",
          formatter: (row, column, value) => {
            return value ? Big(value).times(100).round(2).toString() + "%" : "";
          },
        },
        {
          prop: "monthProduceWeightProportion",
          label: this.$t(
            "ui.data.column.report.produceSalePlan.monthProduceWeightProportion"
          ),
          type: "number",
          formatter: (row, column, value) => {
            return value ? Big(value).times(100).round(2).toString() + "%" : "";
          },
        },
        {
          prop: "monthSaleQty",
          label: this.$t("ui.data.column.report.produceSalePlan.monthSaleQty"),
          type: "number",
        },
        {
          prop: "monthSaleWeight",
          label: this.$t(
            "ui.data.column.report.produceSalePlan.monthSaleWeight"
          ),
          type: "number",
        },
        {
          prop: "monthSaleQtyProportion",
          label: this.$t(
            "ui.data.column.report.produceSalePlan.monthSaleQtyProportion"
          ),
          type: "number",
          formatter: (row, column, value) => {
            return value ? Big(value).times(100).round(2).toString() + "%" : "";
          },
        },
        {
          prop: "monthSaleWeightProportion",
          label: this.$t(
            "ui.data.column.report.produceSalePlan.monthSaleWeightProportion"
          ),
          type: "number",
          formatter: (row, column, value) => {
            return value ? Big(value).times(100).round(2).toString() + "%" : "";
          },
        },
      ];

      return columns;
    },
    searchColumns() {
      return [
        {
          prop: "yearMonth",
          label: this.$t("ui.data.column.report.produceSalePlan.yearMonth"),
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
      exportProduceSalePlan(this.formatParams(false));
    },

    // utils
    setSum(data) {
      if (data.length === 0) {
        this.stat = {};
        return;
      }
      const map = {};
      const keys = Object.keys(data[0]);
      data.forEach((item) => {
        keys.forEach((key) => {
          if (item[key] && !isNaN(item[key])) {
            if (map[key]) {
              map[key] = Big(map[key]).plus(item[key]).toString();
            } else {
              map[key] = item[key];
            }
          }
        });
      });

      if (map.proFinishQty && map.proPlanQty) {
        map.proFinishRate = Big(map.proFinishQty)
          .div(map.proPlanQty)
          .toString();
      }
      if (map.saleFinishQty && map.salePlanQty) {
        map.saleFinishRate = Big(map.saleFinishQty)
          .div(map.salePlanQty)
          .toString();
      }

      this.stat = map;
      console.log(map);
    },
    getSummaryMethod(param) {
      const { columns, data } = param;
      const sums = [];
      columns.forEach((column, index) => {
        if (column.property === "proSize") {
          sums[index] = "合计";
          return;
        } else {
          sums[index] = this.stat[column.property]
            ? this.stat[column.property]
            : "";
        }
      });

      return sums;
    },
    objectSpanMethod({ row, column, rowIndex, columnIndex }) {
      // if (column.property === "product") {
      //   if (rowIndex % 5 === 0) {
      //     return {
      //       rowspan: 5,
      //       colspan: 1,
      //     };
      //   } else {
      //     return {
      //       rowspan: 0,
      //       colspan: 0,
      //     };
      //   }
      // }
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
        const res = await listProduceSalePlan(this.formatParams());
        // console.log()

        this.data = res.rows;

        this.setSum(res.rows);

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