
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
          v-hasPermi="['monthplan:factoryMonthPlanMouldDayResult:export']"
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
import { mapGetters } from "vuex";
//utils
import { downloadLink } from "@/utils/request";
//interface
import {
  listProduction,
  getProductionMonthType,
} from "@/api/monthplan/monthlyProductionPlan";
//components

export default {
  name: "MonthlyProductionPlan",
  components: {
    // tltUpload,
  },
  dicts: [
    "biz_factory_name",
    "biz_product_type",
    "biz_brand_type",
    "biz_plan_type",
    "biz_construction_stage",
    "product_category",
    "biz_schedule_type",
    "biz_yes_no",
    "trial_status"
  ],
  data() {
    return {
      createLoading: false,
      productionStartDate: "",
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
    ...mapGetters('globalList', ['structureList']),
    columns() {
      let columns = [
        {
          prop: "productionNo",
          label: this.$t("ui.data.monthlyProductionPlan.productionNo"),
          width: 120,
        },
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
          label: this.$t("ui.data.demandPlan.monthPlanVersion"),
          width: 120,
        },
        {
          prop: "lastMonthPlanVersion",
          label: this.$t("ui.data.monthlyProductionPlan.lastMonthPlanVersion"),
          width: 120,
        },
        {
          prop: "productionVersion",
          label: this.$t("ui.data.monthlyProductionPlan.productionVersion"),
          width: 120,
        },
        {
          label: this.$t("ui.data.column.monthplan.productType"),
          prop: "productTypeCode",
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_product_type, value);
          },
          width: 120,
        },
        {
          prop: "materialCode",
          label: this.$t("ui.data.colume.wms.unused.productCode"),
          width: 120,
        },
        {
          prop: "materialDesc",
          label: this.$t("ui.data.column.scheduleAdjust.productCodeDesc"),
          width: 280,
        },
        {
          prop: "mesMaterialCode",
          label: this.$t("ui.data.defectiveStock.mesMaterialCode"),
          width: 120,
        },
        {
          prop: "structureName",
          label: this.$t("ui.data.column.finishStock.structureName"),
          width: 120,
        },
        {
          prop: "proSize",
          label: this.$t("ui.data.column.scheduleAdjust.proSize"),
          width: 120,
        },
        {
          prop: "productCategory",
          label: this.$t("ui.data.column.capsuleChuck.productTypeCode"),
          width: 120,
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.product_category, value);
          },
        },
        {
          prop: "productStatus",
          label: this.$t("ui.data.column.trialPlan.trialStatus"),
          width: 120,
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.trial_status, value);
          },
        },
        {
          prop: "productionType",
          label: this.$t("ui.data.DemandPlan.productionType"),
          width: 120,
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_schedule_type, value);
          },
        },
        {
          prop: "mainMaterialDesc",
          label: this.$t("ui.data.rubberMaterial.embryoDesc"),
          width: 320,
        },
        {
          prop: "constructionStage",
          label: this.$t("ui.data.monthlyProductionPlan.constructionStage"),
          width: 120,
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_construction_stage, value);
          },
        },
        {
          prop: "isZeroRack",
          label: this.$t("ui.data.column.mpMonthlySaleQty.isZeroRack"),
          width: 120,
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_yes_no, value);
          },
        },
        {
          prop: "embryoNo",
          label: this.$t("ui.data.column.trialPlan.embryoNo"),
          width: 120,
        },
        {
          prop: "textNo",
          label: this.$t("ui.data.column.trialPlan.textNo"),
          width: 120,
        },
        {
          prop: "lhNo",
          label: this.$t("ui.data.column.trialPlan.lhNo"),
          width: 120,
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
          prop: "specifications",
          label: this.$t("ui.data.column.trialPlan.specifications"),
          width: 120,
        },
        {
          prop: "mainPattern",
          label: this.$t("ui.data.column.moldLedger.mainPattern"),
          width: 120,
        },
        {
          prop: "pattern",
          label: this.$t("saleOrder.figure"),
          width: 120,
        },
        {
          prop: "mouldCavityQty",
          label: this.$t("ui.data.monthlyProductionPlan.mouldCavityQtyNum"),
          width: 120,
        },
        {
          prop: "typeBlockQty",
          label: this.$t("ui.data.monthlyProductionPlan.typeBlockQtyNum"),
          width: 120,
        },
        {
          prop: "heightQty",
          label: this.$t("ui.data.monthlyProductionPlan.heightQty"),
          width: 120,
        },
        {
          prop: "averageSaleQty",
          label: this.$t("ui.data.defectiveStock.averageSaleQty"),
          width: 120,
        },
        {
          prop: "inventorySalesRatio",
          label: this.$t("ui.data.monthlyProductionPlan.inventorySalesRatio"),
          width: 120,
        },
        {
          prop: "dayVulcanizationQty",
          label: this.$t("ui.data.monthlyProductionPlan.dayVulcanizationQty"),
          width: 120,
        },
        {
          prop: "cxMachineCode",
          label: this.$t("ui.data.monthlyProductionPlan.cxMachineCode"),
          width: 120,
        },
        {
          prop: "mouldChangeInfo",
          label: this.$t("ui.data.monthlyProductionPlan.mouldChangeInfo"),
          width: 120,
        },
        // {
        //   prop: "dynamicBalanceQty",
        //   label: this.$t("ui.data.monthlyProductionPlan.dynamicBalanceQty"),
        //   width: 120,
        // },
        // {
        //   prop: "uniformityQty",
        //   label: this.$t("ui.data.monthlyProductionPlan.uniformityQty"),
        //   width: 120,
        // },
        {
          prop: "curingTime",
          label: this.$t("ui.data.monthlyProductionPlan.curingTime"),
          width: 120,
        },
        {
          prop: "prodReqPlan",
          label: this.$t("ui.data.monthlyProductionPlan.prodReqPlan"),
          width: 120,
        },
        {
          prop: "trialQty",
          label: this.$t("ui.data.monthlyProductionPlan.trialQty"),
          width: 120,
        },
        {
          prop: "heightProductionQty",
          label: this.$t("ui.data.mouldingDayResult.heightProductionQty"),
          width: 120,
        },
        {
          prop: "factProdReqQty",
          label: this.$t("ui.data.monthlyProductionPlan.factProdReqQty"),
          width: 120,
        },
        {
          prop: "totalQty",
          label: this.$t("ui.data.mouldingDayResult.totalQty"),
          width: 120,
        },
        {
          prop: "midProductionQty",
          label: this.$t("ui.data.mouldingDayResult.midProductionQty"),
          width: 120,
        },
        {
          prop: "cycleProductionQty",
          label: this.$t("ui.data.mouldingDayResult.cycleProductionQty"),
          width: 120,
        },
        {
          prop: "conventionProductionQty",
          label: this.$t("ui.data.mouldingDayResult.conventionProductionQty"),
          width: 120,
        },
        {
          prop: "postponeProductionQty",
          label: this.$t("ui.data.monthlyProductionPlan.postponeProductionQty"),
          width: 120,
        },
        {
          prop: "trialProductionQty",
          label: this.$t("ui.data.monthlyProductionPlan.trialProductionQty"),
          width: 120,
        },
        {
          prop: "differenceQty",
          label: this.$t("ui.data.mouldingDayResult.differenceQty"),
          width: 120,
        },
        {
          prop: "reason",
          label: this.$t("ui.data.monthlyProductionPlan.reason"),
          width: 120,
        },
        {
          prop: "beginDay",
          label: this.$t("common.startDate"),
          width: 120,
        },
        {
          prop: "endDay",
          label: this.$t("common.endDate"),
          width: 120,
        },

      ];

      if (this.productionStartDate) {
        let start = moment(this.productionStartDate);
        let end = moment(this.productionStartDate).add(1, "M");
        let list = [];

        while (start.isBefore(end)) {
          list.push(start.format("DD"));
          start.add(1, "d");
        }
        // console.log(list);
        for (let i = 0; i < list.length; i++) {
          let dayNumStr = list[i];
          columns.push({
            label: `${i + 1}号`,

            prop: `day${i + 1}`,
            minWidth: "80px",
            type: "number",
          });
        }
      } else {
        //显示每日数据
        const date = moment(this.query.yearMonth);
        const days = date.daysInMonth();

        for (let i = 0; i < days; i++) {
          columns.push({
            label: `${i + 1}号`,

            prop: `day${i + 1}`,
            minWidth: "80px",
            type: "number",
          });
        }
      }
      // const days = 31;

      // for (let i = 0; i < days; i++) {
      //   columns.push({
      //     label: `${i + 1}号`,
      //     // label: this.$t("ui.data.column.mouldingDayResult.day", {
      //     //   day: i + 1,
      //     // }),
      //     prop: `day${i + 1}`,
      //     minWidth: "80px",
      //     type: "number",
      //   });
      // }
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
          label: this.$t("ui.data.column.finishStock.structureName"),
          prop: "structureName",
          type: "select",
          dictData:this.structureList,
          filterable: true
        },
        // {
        //   label: this.$t("ui.data.rubberMaterial.embryoDesc"),
        //   prop: "mainMaterialDesc",
        // },
        {
          label: this.$t("ui.data.column.scheduleAdjust.productCodeDesc"),
          prop: "materialDesc",
        },
        {
          label: this.$t("ui.data.column.confMinProd.pattern"),
          prop: "mainPattern",
        },
        // {
        //   label: this.$t("产品状态"),
        //   prop: "productStatus",
        //   type: "select",
        //   dictData: this.dict.type.biz_product_type,
        // },
        // {
        //   label: this.$t("规格"),
        //   prop: "materialDesc",
        // },
        {
          label: this.$t("ui.data.column.monthplan.oriMaterialCode"),
          prop: "materialCode",
        },
        {
          label: this.$t("ui.data.column.monthplan.productType"),
          prop: "productTypeCode",
          type: "select",
          dictData: this.dict.type.biz_product_type,
        },
      ];
    },
  },
  methods: {
    async generPlan() {
      try {
        this.createLoading = true;
        let res = await createOrderForecast(this.formatParams());
        this.$modal.msgSuccess(res.msg);
        this.getList();
        this.createLoading = false;
      } catch (err) {
        this.createLoading = false;
      }
    },
    handleSearch(data) {
      this.query = data;
      this.$set(this.page, "current", 1);
      this.updateList();
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
        "/monthplan/factoryMonthPlanFinalResult/export",
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
    async getProductionMonthType() {
      try {
        const res = await getProductionMonthType(this.formatParams(false));
        if (res.productionStartDate) {
          this.productionStartDate = res.productionStartDate;
        } else {
          this.productionStartDate = null;
        }
      } catch (error) {
        console.log(error);
      }
    },
    // api
    async getList() {
      try {
        this.loading = true;
        const res = await listProduction(this.formatParams());
        this.data = res.rows;
        this.page.total = res.total;
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
    },
    async updateList() {
      this.loading = true;
      await this.getProductionMonthType();
      await this.getList();
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
    // this.getProductionMonthType()
    // this.getList();
    this.updateList();
  },
  activated() {},
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
