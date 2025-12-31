
<template>
  <basic-container>
    <page-table
      tableRef="ProSizeSummaryMainTable"
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
      :showSummary="true"
      :selectArea="false"
      :span-method="objectSpanMethod"
      :summary-method="getSummaryMethod"
    >
      <template slot="header">
        <el-button
          type="primary"
            :loading="createLoading"
          plain
          @click="generPlan"
          v-hasPermi="['monthplan:productionPrediction:createMonthPrediction']"
          >{{ $t("生成") }}
        </el-button>
        <el-button
          @click="handleExport"
          v-hasPermi="['monthplan:productionPrediction:export']"
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
import { downloadLink } from "@/utils/request";
//interface
import {
  listOrderForecast,
  createOrderForecast,
} from "@/api/monthplan/orderForecast";
//components

export default {
  name: "monthlyProductionPlan",
  components: {
    // tltUpload,
  },
  dicts: ["biz_factory_name",'biz_product_type','biz_brand_type'],
  data() {
    return {
      createLoading:false,
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
        // {
        //   prop: "yearMonth",
        //   label: this.$t("ui.data.column.report.proSizeSummary.yearMonth"),
        // },
        {
          prop: "factoryCode",
          label: this.$t("common.factory"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_factory_name, value);
          },
          width:120
        },
        {
          prop: "year",
          label: this.$t("年份"),
          width:120
        },
        {
          prop: "month",
          label: this.$t("月份"),
          width:120
        },
        {
          prop: "productTypeCode",
          label: this.$t("产品品类"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_product_type, value);
          },
          width:120
        },
        // {
        //   prop: "类型",
        //   label: this.$t("类型"),
        // },
        {
          prop: "brand",
          label: this.$t("品牌"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_brand_type, value);
          },
          width:120
        },
        {
          prop: "materialCode",
          label: this.$t("物料编码"),
          width:180
        },
        {
          prop: "materialDesc",
          label: this.$t("物料描述"),
          width:300
        },
        {
          prop: "month1",
          label: this.$t("T月"),
          width:120
        },

        {
          prop: "month2",
          label: this.$t("T+1月"),
          width:120
          // align: "right",
          // formatter: (row, column, value) => {
          //   return value
          //     ? Big(value).times(100).toString() + "%"
          //     : value === 0
          //     ? "0%"
          //     : "";
          // },
        },
        {
          prop: "month3",
          label: this.$t("T+2月"),
          width:120
        },
        {
          prop: "remark",
          label: this.$t("备注"),
          width:120
          // align: "right",
          // formatter: (row, column, value) => {
          //   return value
          //     ? Big(value).times(100).toString() + "%"
          //     : value === 0
          //     ? "0%"
          //     : "";
          // },
        },
        {
          prop: "updateTime",
          label: this.$t("生成时间"),
          width:180
        },


      ];

      return columns;
    },
    searchColumns() {
      return [
      {
          prop: "factoryCode",
          label: this.$t("common.factory"),
          type: "select",
          dictData: this.dict.type.biz_factory_name,
          clearable: false,
        },
        {
          prop: "yearMonth",
          label: this.$t("ui.data.column.report.proSizeSummary.yearMonth"),
          type: "date",
          dateType: "month",
          valueFormat: "yyyy-MM",
          clearable: false,
        },


        {
          label: this.$t("产品品类"),
          prop: "productTypeCode",
          type: "select",
          dictData: this.dict.type.biz_product_type,
        },

        {
          label: this.$t("物料编码"),
          prop: "materialCode",
        },
        {
          label: this.$t("物料描述"),
          prop: "materialDesc",
        },
      ];
    },
  },
  methods: {
    async generPlan() {
      try {
        this.createLoading=true
        let res = await createOrderForecast(this.formatParams());
        this.$modal.msgSuccess(res.msg);
        this.getList();
        this.createLoading=false
      } catch (err) {
        this.createLoading=false
      }
    },
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
      downloadLink("/monthplan/productionPrediction/export", this.formatParams(false));
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
        map.proFinishRate =
          Big(map.proFinishQty)
            .div(map.proPlanQty)
            .times(100)
            .round(2)
            .toString() + "%";
      }
      if (map.saleFinishQty && map.salePlanQty) {
        map.saleFinishRate =
          Big(map.saleFinishQty)
            .div(map.salePlanQty)
            .times(100)
            .round(2)
            .toString() + "%";
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

        const res = await listOrderForecast(this.formatParams());
        // console.log()

        this.data = res.rows;


        this.page.total = data.total;
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
    },
  },
  created() {
    const now = new Date();
    const year = now.getFullYear();
    const month = String(now.getMonth() + 1).padStart(2, "0"); // 月份从0开始，需要+1
    let defaultParams = {
      factoryCode: "116",
      yearMonth: `${year}-${month}`,
    };
    this.search = {
      ...defaultParams,
    };
    this.query = {
      ...defaultParams,
    };
    this.getList();
  },
  activated() {
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