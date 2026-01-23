
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
      :showSummary="false"
      :selectArea="false"
      :span-method="objectSpanMethod"
      :summary-method="getSummaryMethod"
    >
      <template slot="header">
        <el-button
          @click="handleExport"
          v-hasPermi="['maindata:dpOrderOffsetDetail:export']"
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
import { listOrderOffsetDetail } from "@/api/monthplan/report";
//components

export default {
  name: "OrderInventoryWriteDown",
  components: {
    // tltUpload,
  },
  dicts: [
    "biz_factory_name",
    "biz_product_name",
    "biz_brand_type",
    "biz_stor_type",
    "biz_product_type",
    "biz_deliver_goods_type",
    "biz_order_type",
    "biz_yes_no"
  ],
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
          prop: "factoryCode",
          label: this.$t("common.factory"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_factory_name, value);
          },
          width: 120,
        },
        {
          prop: "year",
          label: this.$t("年份"),
          width: 120,
        },
        {
          prop: "month",
          label: this.$t("月份"),
          width: 120,
        },

        {
          prop: "monthPlanVersion",
          label: this.$t("需求版本号"),
          width: 180,
        },
        {
          prop: "productTypeCode",
          label: this.$t("产品品类"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_product_type, value);
          },
          width: 120,
        },
        {
          prop: "locationType",
          label: this.$t("ui.data.column.finishStock.wai"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_stor_type, value);
          },
          width: 120,
        },
        {
          prop: "areaCode",
          label: this.$t("区域"),
          width: 120,
        },
        {
          prop: "customName",
          label: this.$t("客户"),
          width: 120,
        },
        {
          prop: "customNationCode",
          label: this.$t("客户国别"),
          width: 120,
        },
        {
          prop: "destinationNationCode",
          label: this.$t("目的国"),
          width: 120,
        },
        {
          prop: "poNumber",
          label: this.$t("PO号"),
          width: 240,
        },

        {
          prop: "brand",
          label: this.$t("品牌"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_brand_type, value);
          },
          width: 120,
        },
        {
          prop: "materialCode",
          label: this.$t("物料编码"),
          width: 120,
        },
        {
          prop: "materialDesc",
          label: this.$t("物料描述"),
          width: 320,
        },
        {
          prop: "orderQty",
          label: this.$t("订单数量"),
          width: 120,
        },

        {
          prop: "stockQty",
          width: 120,
          label: this.$t("库存总数"),
        },
        {
          prop: "allocationQty",
          width: 120,
          label: this.$t("库存分配量"),
        },
        // {
        //   prop: "生产分配量",
        //   width: 120,
        //   label: this.$t("生产分配量"),
        // },
        {
          prop: "plannedSurplus",
          width: 120,
          label: this.$t("月底计划余量分配量"),
        },
        {
          prop: "weekYear",
          width: 120,
          label: this.$t("年周号"),
        },
        {
          prop: "isUniformity",
          width: 120,
          label: this.$t("均匀性"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(
              this.dict.type.biz_deliver_goods_type,
              value
            );
          },
        },
        {
          prop: "isDynamicBalance",
          width: 120,
          label: this.$t("动平衡"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(
              this.dict.type.biz_deliver_goods_type,
              value
            );
          },
        },
        {
          prop: "deliverGoodsType",
          label: this.$t("common.shipType"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(
              this.dict.type.biz_deliver_goods_type,
              value
            );
          },
        },
        {
          prop: "scmPriority",
          label: this.$t("ui.data.column.monthplan.scmPriority"),
          type: "select",
          dictData: this.dict.type.biz_yes_no,
        },
        {
          prop: "orderPriority",
          label: this.$t("订单优先级"),
          type: "select",
          dictData: this.dict.type.biz_order_type,
        },
        {
          width: 180,
          prop: "updateTime",
          label: this.$t("更新日期"),
        },
      ];

      return columns;
    },
    searchColumns() {
      return [
        {
          label: this.$t("common.factory"),
          prop: "factoryCode",
          type: "select",
          dictData: this.dict.type.biz_factory_name,
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
          label: this.$t("需求版本号"),
          prop: "monthPlanVersion",
        },
        {
          prop: "productTypeCode",
          label: this.$t("ui.data.column.monthplan.productType"),
          type: "select",
          dictData: this.dict.type.biz_product_type,
        },
        {
          label: this.$t("区域"),
          prop: "areaCode",
        },
        {
          label: this.$t("客户"),
          prop: "customName",
        },
        {
          label: this.$t("PO号"),
          prop: "poNumber",
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
      downloadLink(
        "/maindata/dpOrderOffsetDetail/export",
        this.formatParams(false)
      );
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

        const res = await listOrderOffsetDetail(this.formatParams());
        // console.log()

        this.data = res.rows;

        // this.setSum(res.rows);

        this.page.total = res.total;
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
      factoryCode: "116",
    };
    this.query = {
      yearMonth: date.format("yyyy-MM"),
      factoryCode: "116",
    };
    this.getList();
  },
  activated() {
    // this.getList();
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