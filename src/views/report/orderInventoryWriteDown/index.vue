
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
import { areaList } from "@/api/monthplan/mdmAreaCapaAllocation";
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
      areaDist:[],
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
          label: this.$t("ui.data.colume.year"),
          width: 120,
        },
        {
          prop: "month",
          label: this.$t("ui.data.colume.month"),
          width: 120,
        },

        {
          prop: "monthPlanVersion",
          label: this.$t("ui.data.column.finishStock.requireVersion"),
          width: 180,
        },
        {
          prop: "productTypeCode",
          label: this.$t("ui.data.column.monthplan.productType"),
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
          label: this.$t("common.area"),
          width: 120,
        },
        {
          prop: "customName",
          label: this.$t("ui.data.column.monthplan.salCode"),
          width: 120,
        },
        {
          prop: "customNationCode",
          label: this.$t("ui.data.column.monthplan.salNCode"),
          width: 120,
        },
        {
          prop: "destinationNationCode",
          label: this.$t("ui.data.column.monthplan.natCode"),
          width: 120,
        },
        {
          prop: "poNumber",
          label: this.$t("ui.data.column.monthplan.salCodePo"),
          width: 240,
        },

        {
          prop: "brand",
          label: this.$t("common.brand"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_brand_type, value);
          },
          width: 120,
        },
        {
          prop: "materialCode",
          label: this.$t("ui.data.defectiveStock.materialCode"),
          width: 120,
        },
        {
          prop: "materialDesc",
          label: this.$t("ui.data.column.scheduleAdjust.productCodeDesc"),
          width: 320,
        },
        {
          prop: "orderQty",
          label: this.$t("book.deliveryOrder.orderNum"),
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
          label: this.$t("ui.data.column.monthplan.weekYear"),
        },
        {
          prop: "isEudr",
          label: this.$t("eudr"),
          type: "select",
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_yes_no, value);
          },
        },
        // {
        //   prop: "isUniformity",
        //   width: 120,
        //   label: this.$t("均匀性"),
        //   formatter: (row, column, value) => {
        //     return this.selectDictLabel(
        //       this.dict.type.biz_deliver_goods_type,
        //       value
        //     );
        //   },
        // },
        // {
        //   prop: "isDynamicBalance",
        //   width: 120,
        //   label: this.$t("动平衡"),
        //   formatter: (row, column, value) => {
        //     return this.selectDictLabel(
        //       this.dict.type.biz_deliver_goods_type,
        //       value
        //     );
        //   },
        // },
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
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_yes_no, value);
          },
        },
        {
          prop: "orderPriority",
          label: this.$t("ui.data.column.monthplan.orderPriority"),
          type: "select",
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_order_type, value);
          },

        },
        {
          width: 180,
          prop: "updateTime",
          label: this.$t("ui.data.column.scheduleAdjust.updata"),
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
          label: this.$t("ui.data.column.finishStock.requireVersion"),
          prop: "monthPlanVersion",
        },
        {
          prop: "productTypeCode",
          label: this.$t("ui.data.column.monthplan.productType"),
          type: "select",
          dictData: this.dict.type.biz_product_type,
        },
        {
          label: this.$t("common.area"),
          prop: "areaCode",
          type: "select",
          filterable: true,
          dictData:this.areaDist,
          special:true
        },
        {
          label: this.$t("ui.data.column.monthplan.salCode"),
          prop: "customName",
        },
        {
          label: this.$t("ui.data.column.monthplan.salCodePo"),
          prop: "poNumber",
        },
        {
          label: this.$t("ui.data.defectiveStock.materialCode"),
          prop: "materialCode",
        },
        {
          label: this.$t("ui.data.column.scheduleAdjust.productCodeDesc"),
          prop: "materialDesc",
        },
      ];
    },
  },
  methods: {
    async getAreaList() {
      try {
        let res = await areaList({
          pageSize: 1000,
          pageNum: 1,
          // factoryCode: '116',
          status: 0,
        });
        let list=[]
        for (let i = 0; i < res.rows.length; i++) {
          let obj={
            label:res.rows[i].areaNameI18n,
            value:res.rows[i].areaCode
          }
          list.push(obj)

        }
        this.areaDist=list
      } catch (error) {
        console.error(error);
      } finally {
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
    this.getAreaList();
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