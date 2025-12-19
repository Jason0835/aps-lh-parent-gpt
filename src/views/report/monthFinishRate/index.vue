
<template>
  <basic-container>
    <page-table
      tableRef="MonthFinishRateMainTable"
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
          v-hasPermi="['report:monthFinishRate:export']"
          >{{ $t("ui.frame.btn.export") }}</el-button
        >
      </template>
      <template slot="headerRight">
        <div class="stat-info">
          <span>
            <span
              >{{ $t("ui.data.column.report.monthFinishRate.producePlanQty") }}:
            </span>
            <span>
              {{ stat.producePlanQty }}
            </span>
          </span>
          <span
            ><span
              >{{
                $t(
                  "ui.data.column.report.monthFinishRate.produceFinishPlanQty"
                )
              }}:</span
            >
            <span>{{ stat.produceFinishPlanQty }}</span>
          </span>
          <span
            ><span
              >{{
                $t("ui.data.column.report.monthFinishRate.produceFinishRate")
              }}:</span
            >
            <span>{{ stat.produceFinishRate }}</span>
          </span>
          <span>
            <span
              >{{
                $t("ui.data.column.report.monthFinishRate.salePlanQty")
              }}:</span
            ><span>{{ stat.salePlanQty }}</span>
          </span>
          <span>
            <span
              >{{
                $t("ui.data.column.report.monthFinishRate.saleFinishPlanQty")
              }}:
            </span>
            <span>{{ stat.saleFinishPlanQty }}</span></span
          >
          <span
            ><span
              >{{
                $t("ui.data.column.report.monthFinishRate.saleFinishRate")
              }}:</span
            ><span>{{ stat.saleFinishRate }}</span></span
          >
        </div>
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
  listMonthFinishRate,
  exportMonthFinishRate,
} from "@/api/monthplan/report";
//components

export default {
  name: "MonthFinishRate",
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
          prop: "proRangeLabel",
          label: this.$t("ui.data.column.report.monthFinishRate.proRangeLabel"),
          align: "center",
        },
        {
          prop: "proSkuCount",
          label: this.$t("ui.data.column.report.monthFinishRate.proSkuCount"),
          align: "center",
        },
        {
          prop: "proUnit",
          label: this.$t("ui.data.column.report.monthFinishRate.proUnit"),
          align: "center",
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.unit, value);
          },
        },
        {
          prop: "salRangeLabel",
          label: this.$t("ui.data.column.report.monthFinishRate.salRangeLabel"),
          align: "center",
        },
        {
          prop: "salSkuCount",
          label: this.$t("ui.data.column.report.monthFinishRate.salSkuCount"),
          align: "center",
        },
        {
          prop: "salUnit",
          label: this.$t("ui.data.column.report.monthFinishRate.salUnit"),
          align: "center",
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.unit, value);
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
      exportMonthFinishRate(this.formatParams(false));
    },

    // utils
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
        const data = await listMonthFinishRate(this.formatParams());
        // console.log(data);
        const list = [];
        this.stat = {
          produceFinishPlanQty: data.produceFinishPlanQty,
          produceFinishRate: data.produceFinishRate ? Big(data.produceFinishRate).times(100).toString() + "%": "",
          producePlanQty: data.producePlanQty,
          saleFinishPlanQty: data.saleFinishPlanQty,
          saleFinishRate: data.saleFinishRate ? Big(data.saleFinishRate).times(100).toString() + "%": "",
          salePlanQty: data.salePlanQty,
        };

        data.produceResultList.forEach((item, index) => {
          let salItem = data.saleResultList[index];
          list.push({
            proRangeLabel: item.rangeLabel,
            proSkuCount: item.skuCount,
            proUnit: item.unit,
            salRangeLabel: salItem ? salItem.rangeLabel : "",
            salSkuCount: salItem ? salItem.skuCount : "",
            salUnit: salItem ? salItem.unit : "",
          });
        });

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