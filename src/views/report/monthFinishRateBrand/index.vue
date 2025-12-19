
<template>
  <basic-container>
    <page-table
      tableRef="MonthFinishRateBrandMainTable"
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
    >
      <template slot="header">
        <el-button
          @click="handleExport"
          v-hasPermi="['report:monthFinishRateBrand:export']"
          >{{ $t("ui.frame.btn.export") }}</el-button
        >
      </template>
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
  listMonthFinishRateBrand,
  exportMonthFinishRateBrand,
} from "@/api/monthplan/report";
//components

export default {
  name: "MonthFinishRateBrand",
  components: {
    // tltUpload,
  },
  dicts: ["biz_factory_name", "unit", "biz_brand_type"],
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
          prop: "brandName",
          label: this.$t(
            "ui.data.column.report.monthFinishRateBrand.brandName"
          ),
          align: "center",
          width: 140,
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_brand_type, value);
          },
        },
        {
          prop: "stockSkuCount",
          label: this.$t(
            "ui.data.column.report.monthFinishRateBrand.stockSkuCount"
          ),
          type: "number",
        },
        {
          prop: "stockQty",
          label: this.$t("ui.data.column.report.monthFinishRateBrand.stockQty"),
          type: "number",
        },
        {
          prop: "produceSkuCount",
          label: this.$t(
            "ui.data.column.report.monthFinishRateBrand.produceSkuCount"
          ),
          type: "number",
        },
        {
          prop: "producePlanQty",
          label: this.$t(
            "ui.data.column.report.monthFinishRateBrand.producePlanQty"
          ),
          type: "number",
        },
        {
          prop: "produceFinishPlanQty",
          label: this.$t(
            "ui.data.column.report.monthFinishRateBrand.produceFinishPlanQty"
          ),
          type: "number",
        },
        {
          prop: "produceFinishRate",
          label: this.$t(
            "ui.data.column.report.monthFinishRateBrand.finishSatisfyRate"
          ),
          type: "number",
          formatter: (row, column, value) => {
            return value ? `${Big(value).times(100).toString()}%` : "";
          },
        },
        {
          prop: "salePlanSkuCount",
          label: this.$t(
            "ui.data.column.report.monthFinishRateBrand.salePlanSkuCount"
          ),
          type: "number",
        },
        {
          prop: "salePlanQty",
          label: this.$t(
            "ui.data.column.report.monthFinishRateBrand.salePlanQty"
          ),
          type: "number",
        },
        {
          prop: "saleFinishSkuCount",
          label: this.$t(
            "ui.data.column.report.monthFinishRateBrand.saleFinishSkuCount"
          ),
          type: "number",
        },
        {
          prop: "saleFinishPlanQty",
          label: this.$t(
            "ui.data.column.report.monthFinishRateBrand.saleFinishPlanQty"
          ),
          type: "number",
        },
        {
          prop: "accuracyRate",
          label: this.$t(
            "ui.data.column.report.monthFinishRateBrand.accuracyRate"
          ),
          type: "number",
          formatter: (row, column, value) => {
            return value ? `${Big(value).times(100).toString()}%` : "";
          },
        },
        {
          prop: "produceSatisfyRate",
          label: this.$t(
            "ui.data.column.report.monthFinishRateBrand.produceSatisfyRate"
          ),
          type: "number",
          formatter: (row, column, value) => {
            return value ? `${Big(value).times(100).toString()}%` : "";
          },
        },
        {
          prop: "finishSatisfyRate",
          label: this.$t(
            "ui.data.column.report.monthFinishRateBrand.finishSatisfyRate"
          ),
          type: "number",
          formatter: (row, column, value) => {
            return value ? `${Big(value).times(100).toString()}%` : "";
          },
        },
      ];

      return columns;
    },
    searchColumns() {
      return [
        {
          label: this.$t("ui.data.column.report.monthFinishRate.yearMonth"),
          prop: "yearMonth",
          type: "date",
          dateType: "month",
          valueFormat: "yyyy-MM",
        },
        {
          prop: "factoryCode",
          label: this.$t("ui.data.column.report.monthFinishRate.factoryCode"),
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
      exportMonthFinishRateBrand(this.formatParams(false));
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
        const data = await listMonthFinishRateBrand(this.formatParams());

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