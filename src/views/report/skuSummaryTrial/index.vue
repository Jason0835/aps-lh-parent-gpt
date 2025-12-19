
<template>
  <basic-container>
    <page-table
      tableRef="SkuSummaryTrial"
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
//utils
// import { downloadLink } from "@/utils/request";
//interface
import {
  listSkuSummaryTrial,
  exportSkuSummaryTrial,
} from "@/api/monthplan/report";
//components

export default {
  name: "SkuSummaryTrial",
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
          label: this.$t("ui.data.column.report.skuSummaryTrial.product"),
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
          prop: "month15",
          label: this.$t("ui.data.column.report.skuSummaryTrial.chainRatio"),
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
      exportSkuSummaryTrial(this.formatParams(false));
    },

    // utils
    objectSpanMethod({ row, column, rowIndex, columnIndex }) {
      if (column.property === "product") {
        if (rowIndex === 0) {
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
        const res = await listSkuSummaryTrial(this.formatParams());
        // console.log(res);

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
        console.log(map);

        let list = [];

        for (let i = 1; i <= 31; i++) {
          list.push(
            {
              product: "试制SKU",
              name: `生产${i}天的SKU数（个）`,
              ...map["dayCount" + i],
            },
            {
              product: "试制SKU",
              name: `生产${i}天的SKU数（条）`,
              ...map["daySum" + i],
            }
          );
        }
        list.push(
          {
            product: "试制SKU",
            name: `合计（个）`,
            ...map["totalCount"],
          },
          {
            product: "试制SKU",
            name: `合计（条）`,
            ...map["totalSum"],
          }
        );
        this.data = list;
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
      // year: "2024",
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
