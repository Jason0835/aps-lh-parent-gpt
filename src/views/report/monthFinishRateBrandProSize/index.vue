
<template>
  <basic-container>
    <page-table
      tableRef="MonthFinishRateBrandProSizeMainTable"
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
          v-hasPermi="['report:monthFinishRateBrandProSize:export']"
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
  listMonthFinishRateBrandProSize,
  exportMonthFinishRateBrandProSize,
} from "@/api/monthplan/report";
//components

export default {
  name: "MonthFinishRateBrandProSize",
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
      rowSpanMap: new Map(),
    };
  },
  computed: {
    columns() {
      let columns = [
        {
          prop: "brandName",
          label: this.$t(
            "ui.data.column.report.monthFinishRateBrandProSize.brandName"
          ),
          align: "center",
          width: 160,
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_brand_type, value);
          },
        },
        {
          prop: "proSize",
          label: this.$t(
            "ui.data.column.report.monthFinishRateBrandProSize.proSize"
          ),
        },
        {
          prop: "producePlanQty",
          label: this.$t(
            "ui.data.column.report.monthFinishRateBrandProSize.producePlanQty"
          ),
          type: "number",
        },
        {
          prop: "produceFinishPlanQty",
          label: this.$t(
            "ui.data.column.report.monthFinishRateBrandProSize.produceFinishPlanQty"
          ),
          type: "number",
        },

        {
          prop: "produceFinishRate",
          label: this.$t(
            "ui.data.column.report.monthFinishRateBrandProSize.produceFinishRate"
          ),
          type: "number",
          formatter: (row, column, value) => {
            return value ? `${Big(value).times(100).toString()}%` : "";
          },
        },

        {
          prop: "salePlanQty",
          label: this.$t(
            "ui.data.column.report.monthFinishRateBrandProSize.salePlanQty"
          ),
          type: "number",
        },
        {
          prop: "saleFinishPlanQty",
          label: this.$t(
            "ui.data.column.report.monthFinishRateBrandProSize.saleFinishPlanQty"
          ),
          type: "number",
        },
        {
          prop: "saleFinishRate",
          label: this.$t(
            "ui.data.column.report.monthFinishRateBrandProSize.saleFinishRate"
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
          label: this.$t(
            "ui.data.column.report.monthFinishRateBrandProSize.yearMonth"
          ),
          prop: "yearMonth",
          type: "date",
          dateType: "month",
          valueFormat: "yyyy-MM",
        },
        {
          prop: "factoryCode",
          label: this.$t(
            "ui.data.column.report.monthFinishRateBrandProSize.factoryCode"
          ),
          type: "select",
          dictData: this.dict.type.biz_factory_name,
        },
        // {
        //   prop: "brandName",
        //   label: this.$t(
        //     "ui.data.column.report.monthFinishRateBrandProSize.brandName"
        //   ),
        //   type: "select",
        //   dictData: this.dict.type.biz_brand_type,
        // },
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
      exportMonthFinishRateBrandProSize(this.formatParams(false));
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
    calculateRowSpans() {
      const columns = ["brandName"];

      columns.forEach((prop) => {
        const spanMap = [];
        let count = 1;
        let startPos = 0;

        for (let i = 1; i <= this.data.length; i++) {
          if (
            i < this.data.length &&
            this.data[i][prop] === this.data[i - 1][prop]
          ) {
            count++;
          } else {
            spanMap[startPos] = count;
            for (let j = startPos + 1; j < startPos + count; j++) {
              spanMap[j] = 0;
            }
            startPos = i;
            count = 1;
          }
        }

        this.rowSpanMap.set(prop, spanMap);
      });
    },
    objectSpanMethod({ row, column, rowIndex, columnIndex }) {
      const prop = column.property;
      const spanMap = this.rowSpanMap.get(prop);

      if (spanMap && spanMap[rowIndex] !== undefined) {
        return {
          rowspan: spanMap[rowIndex],
          colspan: 1, // 固定列宽为1
        };
      }

      return { rowspan: 1, colspan: 1 };
    },

    // api
    async getList() {
      try {
        this.loading = true;
        const data = await listMonthFinishRateBrandProSize(this.formatParams());

        this.data = data.rows;
        this.calculateRowSpans();

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