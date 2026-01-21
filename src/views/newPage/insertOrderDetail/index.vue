
<template>
  <basic-container>
    <page-table
      tableRef="cxFixedMachineMainTable"
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
      @selection-change="handleSelectionChange"
      :showSummary="false"
      :selectArea="false"
    >
      <template slot="header">
        <el-tabs v-model="activeName" @tab-click="handleClick" type="card">
          <el-tab-pane label="净需求计划" name="first"> </el-tab-pane>
          <el-tab-pane label="供应链订单" name="second"> </el-tab-pane>
          <el-tab-pane label="排产计划" name="three"> </el-tab-pane>
          <el-tab-pane label="未排产计划" name="four"> </el-tab-pane>
        </el-tabs>
      </template>
    </page-table>
    <tlt-upload
      ref="tltUpload"
      downloadUrl="/mdm/productMoldingLimit/importTemplate"
      uploadUrl="/mdm/productMoldingLimit/importData"
      @uploadSuccess="getList"
    />
    <!-- <infoDialog ref="infoRef" @success="getList" /> -->
  </basic-container>
</template>
<script>
//lib
import { mapState, mapGetters } from "vuex";
//utils
import { downloadLink } from "@/utils/request";

import { listDemandPlan } from "@/api/monthplan/demandPlan";
import { listSupplyOrderPool } from "@/api/monthplan/supplyOrderPool";
import { listProductionPlan } from "@/api/monthplan/monthlyProductionPlan";
import { listMonthPlanNoProductionPlan } from "@/api/monthplan/monthPlanNoProductionPlan.js";
//components
import tltUpload from "@/components/tltUpload/tltUpload.vue";

// import infoDialog from "./components/infoDialog.vue";

export default {
  name: "insertOrderDetail",
  components: {
    tltUpload,
    // infoDialog,
  },
  dicts: [
    "biz_factory_name",
    "biz_product_type",
    "biz_order_type",
    "biz_yes_no",
    "biz_stor_type",
    "biz_brand_type",
    "biz_product_characteristics",
    "biz_schedule_type",
    "product_category",
    "supply_order_type",
    "biz_construction_stage",
    "trial_status",
    "biz_plan_type",
  ],
  provide() {
    return {
      parentDict: this.dict,
    };
  },
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
      importDefaultValue: {},
      importRules: {},
      activeName: "first",
      show: false,
      dailyVisible: true,
    };
  },
  computed: {
    ...mapGetters("globalList", ["structureList"]),
    ...mapState({
      moldingMachines: (state) => state.molding.machines,
    }),
    columns() {
      if (!this.show) {
        return [];
      }
      if (this.activeName == "first") {
        return [
          {
            prop: "factoryCode",
            label: this.$t("common.factory"),
            formatter: (row, column, value) => {
              return this.selectDictLabel(
                this.dict.type.biz_factory_name,
                value
              );
            },
          },
          {
            prop: "year",
            label: this.$t("ui.data.column.productionMouldConfiguration.year"),
          },
          {
            prop: "month",
            label: this.$t("ui.data.column.productionMouldConfiguration.month"),
          },
          {
            prop: "productTypeCode",
            label: this.$t("ui.data.column.monthplan.productType"),
            formatter: (row, column, value) => {
              return this.selectDictLabel(
                this.dict.type.biz_product_type,
                value
              );
            },
          },

          {
            prop: "locationType",
            label: this.$t("common.type"),
            formatter: (row, column, value) => {
              return this.selectDictLabel(this.dict.type.biz_stor_type, value);
            },
          },
          {
            prop: "monthPlanVersion",
            label: this.$t("ui.data.demandPlan.monthPlanVersion"),
            width: 180,
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
            prop: "scmPriority",
            label: this.$t("ui.data.column.monthplan.scmPriority"),
            width: 120,
            formatter: (row, column, value) => {
              return this.selectDictLabel(this.dict.type.biz_order_type, value);
            },
          },
          {
            prop: "structureName",
            label: this.$t("ui.data.column.finishStock.structureName"),
            width: 180,
          },
          {
            prop: "mainPattern",
            label: this.$t("ui.data.column.moldLedger.mainPattern"),
            width: 120,
          },
          {
            prop: "materialCode",
            label: this.$t("ui.data.column.monthplan.oriMaterialCode"),
            width: 120,
          },
          {
            prop: "materialDesc",
            label: this.$t("ui.data.column.scheduleAdjust.productCodeDesc"),
            width: 300,
          },
          {
            prop: "productionType",
            label: this.$t("ui.data.DemandPlan.productionType"),
            width: 120,
            formatter: (row, column, value) => {
              return this.selectDictLabel(
                this.dict.type.biz_schedule_type,
                value
              );
            },
          },
          {
            prop: "stockQty",
            label: this.$t("ui.data.demandPlan.stockQtyTotal"),
            width: 120,
          },
          {
            prop: "sub2YearStockQty",
            label: this.$t("Y-2+(DOT)"),
            width: 120,
          },
          {
            prop: "sub1YearStockQty",
            label: this.$t("Y-1+(DOT)"),
            width: 120,
          },
          {
            prop: "currentYearStockQty",
            label: this.$t("Y-0+(DOT)"),
            width: 120,
          },

          {
            prop: "orderQty",
            label: this.$t("ui.data.DemandPlan.orderQty"),
          },

          {
            prop: "plannedSurplus",
            label: this.$t("ui.data.DemandPlan.plannedSurplus"),
          },
          {
            prop: "netQty",
            label: this.$t("ui.data.DemandPlan.netQty"),
          },
          {
            prop: "isProduction",
            label: this.$t("ui.data.DemandPlan.isProduction"),
            formatter: (row, column, value) => {
              return this.selectDictLabel(this.dict.type.biz_yes_no, value);
            },
          },
          {
            prop: "postponeNetQty",
            label: this.$t("ui.data.DemandPlan.postponeNetQty"),
          },
          {
            prop: "unPostponeNetQty",
            label: this.$t("ui.data.DemandPlan.unPostponeNetQty"),
          },
          {
            prop: "heightQty",
            label: this.$t("ui.data.DemandPlan.heightQty"),
          },
          {
            prop: "midQty",
            label: this.$t("ui.data.DemandPlan.midQty"),
          },
          {
            prop: "postponeQty",
            label: this.$t("ui.data.DemandPlan.postponeQty"),
          },
          {
            prop: "cycleReserveQty",
            label: this.$t("ui.data.DemandPlan.cycleReserveQty"),
          },
          {
            prop: "conventionReserveQty",
            label: this.$t("ui.data.DemandPlan.conventionReserveQty"),
          },
          {
            prop: "isReachMinProductionQty",
            label: this.$t("ui.data.DemandPlan.isReachMinProductionQty"),
            formatter: (row, column, value) => {
              return this.selectDictLabel(this.dict.type.biz_yes_no, value);
            },
          },
          {
            prop: "minProductionQty",
            label: this.$t("ui.data.DemandPlan.minProductionQty"),
          },
          {
            prop: "remark",
            label: this.$t("common.remark"),
          },
          {
            prop: "updateTime",
            label: this.$t("ui.data.column.scheduleAdjust.updata"),
            width: 180,
          },
        ];
      }
      if (this.activeName == "second") {
        return [
          {
            prop: "factoryCode",
            label: this.$t("common.factory"),
            formatter: (row, column, value) => {
              return this.selectDictLabel(
                this.dict.type.biz_factory_name,
                value
              );
            },
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
            prop: "productTypeCode",
            label: this.$t("ui.data.column.monthplan.productType"),
            formatter: (row, column, value) => {
              return this.selectDictLabel(
                this.dict.type.biz_product_type,
                value
              );
            },
          },
          {
            prop: "orderType",
            label: this.$t("ui.data.defectiveStock.orderType"),
            formatter: (row, column, value) => {
              return this.selectDictLabel(
                this.dict.type.supply_order_type,
                value
              );
            },
            width: 160,
          },
          {
            prop: "locationType",
            label: this.$t("ui.data.column.finishStock.wai"),
            formatter: (row, column, value) => {
              return this.selectDictLabel(this.dict.type.biz_stor_type, value);
            },
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
            label: this.$t("ui.data.column.monthplan.oriMaterialCode"),
            width: 120,
          },
          {
            prop: "materialDesc",
            label: this.$t("ui.data.column.scheduleAdjust.productCodeDesc"),
            width: 400,
          },
          {
            prop: "productCategory",
            label: this.$t("ui.data.column.capsuleChuck.productTypeCode"),
            formatter: (row, column, value) => {
              return this.selectDictLabel(
                this.dict.type.product_category,
                value
              );
            },
          },
          {
            prop: "qty",
            label: this.$t("ui.data.defectiveStock.qty"),
          },
          {
            prop: "saleAreaName",
            label: this.$t("ui.data.defectiveStock.saleArea"),
            width: 180,
          },
          {
            prop: "threeAverageQty",
            label: this.$t("ui.data.defectiveStock.threeAverageQty"),
          },
          {
            prop: "sixAverageQty",
            label: this.$t("ui.data.defectiveStock.sixAverageQty"),
          },
          {
            prop: "deliveryFrequency",
            label: this.$t("ui.data.defectiveStock.deliveryFrequency"),
          },
          {
            prop: "structureFrequency",
            label: this.$t("ui.data.defectiveStock.structureFrequency"),
          },
          {
            prop: "threeOverdueStockQty",
            label: this.$t("ui.data.defectiveStock.threeOverdueStockQty"),
          },
          {
            prop: "sixOverdueStockQty",
            label: this.$t("ui.data.defectiveStock.sixOverdueStockQty"),
          },
          {
            prop: "twelveOverdueStockQty",
            label: this.$t("ui.data.defectiveStock.twelveOverdueStockQty"),
          },
          {
            prop: "stockLimit",
            label: this.$t("ui.data.defectiveStock.stockLimit"),
          },

          {
            prop: "remark",
            label: this.$t("common.remark"),
            width: 120,
          },
          {
            prop: "updateTime",
            label: this.$t("ui.data.column.scheduleAdjust.updata"),
            width: 180,
          },
        ];
      }
      if (this.activeName == "three") {
        let columns = [
          {
            prop: "materialCode",
            label: this.$t("ui.data.column.monthplan.oriMaterialCode"),
            width: 120,
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
            prop: "structureName",
            label: this.$t("ui.data.column.finishStock.structureName"),
            width: 180,
          },
          {
            label: this.$t("ui.data.column.capsuleChuck.productTypeCode"),
            prop: "productCategory",
            formatter: (row, column, value) => {
              return this.selectDictLabel(
                this.dict.type.product_category,
                value
              );
            },
            width: 120,
          },
          {
            label: this.$t("ui.data.column.monthplan.productType"),
            prop: "productTypeCode",
            formatter: (row, column, value) => {
              return this.selectDictLabel(
                this.dict.type.biz_product_type,
                value
              );
            },
            width: 120,
          },
          {
            label: this.$t("ui.data.monthlyProductionPlan.planType"),
            prop: "planType",
            formatter: (row, column, value) => {
              return this.selectDictLabel(this.dict.type.biz_plan_type, value);
            },
            width: 120,
          },
          {
            label: this.$t("ui.data.monthlyProductionPlan.constructionStage"),
            prop: "constructionStage",
            formatter: (row, column, value) => {
              return this.selectDictLabel(
                this.dict.type.biz_construction_stage,
                value
              );
            },
            width: 120,
          },
          {
            label: this.$t("ui.data.column.trialPlan.specifications"),
            prop: "specifications",
            width: 120,
          },
          {
            prop: "mainMaterialDesc",
            label: this.$t("ui.data.rubberMaterial.embryoDesc"),
            width: 280,
          },
          {
            prop: "materialDesc",
            label: this.$t("ui.data.column.scheduleAdjust.productCodeDesc"),
            width: 300,
          },
          {
            prop: "mainPattern",
            label: this.$t("ui.data.column.confMinProd.pattern"),
            width: 120,
          },

          {
            prop: "mouldCavityQty",
            label: this.$t("ui.data.monthlyProductionPlan.mouldCavityQty"),
          },
          {
            prop: "typeBlockQty",
            label: this.$t("ui.data.monthlyProductionPlan.typeBlockQty"),
            width: 120,
          },
          {
            prop: "prodReqPlan",
            label: this.$t("ui.data.monthlyProductionPlan.prodReqPlan"),
            width: 120,
          },
          {
            prop: "totalQty",
            label: this.$t("ui.data.mouldingDayResult.totalQty"),
            width: 120,
          },
          {
            prop: "differenceQty",
            label: this.$t("ui.data.mouldingDayResult.differenceQty"),
            width: 120,
          },
          {
            prop: "heightQty",
            label: this.$t("ui.data.DemandPlan.heightQty"),
            width: 120,
          },
          {
            prop: "averageQty",
            label: this.$t("ui.data.column.mpMonthlySaleQty.averageSaleQty"),
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
            prop: "heightProductionQty",
            label: this.$t("ui.data.mouldingDayResult.heightProductionQty"),
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
            label: this.$t("common.remark"),
            prop: "remark",
            width: 120,
          },
        ];
        if (this.dailyVisible) {
          const query = this.$route.query;
          if (query.productionStartDate) {
            //
            let start = moment(query.productionStartDate);
            let end = moment(query.productionStartDate).add(1, "M");

            let list = [];

            while (start.isBefore(end)) {
              list.push(start.format("DD"));
              start.add(1, "d");
            }
            // console.log(list);
            for (let i = 0; i < list.length; i++) {
              let dayNumStr = list[i];
              columns.push({
                label: `${Number(dayNumStr)}号`,
                // label: this.$t("ui.data.column.mouldingDayResult.day", {
                //   day: Number(dayNumStr),
                // }),
                prop: `day${i + 1}`,
                minWidth: "80px",
                type: "number",
              });
            }
          } else {
            //显示每日数据
            // const date = moment(this.query.yearMonth);
            // const year = date.year();
            // const month = date.month() + 1;
            const days = 31;

            for (let i = 0; i < days; i++) {
              columns.push({
                label: `${i + 1}号`,
                // label: this.$t("ui.data.column.mouldingDayResult.day", {
                //   day: i + 1,
                // }),
                prop: `day${i + 1}`,
                minWidth: "80px",
                type: "number",
              });
            }
          }
        }
        return columns;
      }
      if (this.activeName == "four") {
        return [
          {
            label: this.$t("common.factory"),
            prop: "factoryCode",
            minWidth: 100,
            formatter: (row, column, value) => {
              return this.selectDictLabel(
                this.dict.type.biz_factory_name,
                value
              );
            },
          },
          {
            label: this.$t("ui.data.colume.year"),
            prop: "year",
            minWidth: 100,
            // sortable: "custom",
          },
          {
            label: this.$t("ui.data.colume.month"),
            prop: "month",
            minWidth: 100,
            // sortable: "custom",
          },
          {
            label: this.$t("ui.data.column.monthplan.oriMaterialCode"),
            prop: "materialCode",
            minWidth: 120,
          },
          {
            label: this.$t("ui.data.column.scheduleAdjust.productCodeDesc"),
            prop: "materialDesc",
            minWidth: 300,
          },
          {
            label: this.$t("ui.data.monthlyProductionPlan.constructionStage"),
            prop: "constructionStage",
            minWidth: 100,
            formatter: (row, column, value) => {
              return this.selectDictLabel(
                this.dict.type.biz_construction_stage,
                value
              );
            },
          },
          {
            label: this.$t("ui.data.column.monthplan.productType"),
            prop: "productTypeCode",
            minWidth: 120,
            formatter: (row, column, value) => {
              return this.selectDictLabel(
                this.dict.type.biz_product_type,
                value
              );
            },
          },
          {
            label: this.$t("ui.data.DemandPlan.productionType"),
            prop: "productionType",
            minWidth: 120,
            formatter: (row, column, value) => {
              return this.selectDictLabel(
                this.dict.type.biz_schedule_type,
                value
              );
            },
          },

          {
            label: this.$t("ui.data.column.scheduleAdjust.proSize"),
            prop: "proSize",
            minWidth: 100,
            // sortable: "custom",
          },
          {
            label: this.$t("ui.data.column.trialPlan.specifications"),
            prop: "specifications",
            minWidth: 120,
          },
          {
            label: this.$t("ui.data.column.confMinProd.pattern"),
            prop: "pattern",
            minWidth: 140,
            // sortable: "custom",
          },
          {
            label: this.$t("ui.data.mouldingDayResult.differenceQty"),
            prop: "unProductionQty",
            minWidth: 120,
          },
          {
            label: this.$t("ui.data.monthlyProductionPlan.reason"),
            prop: "reason",
            minWidth: 240,
          },
          {
            label: this.$t("ui.data.DemandPlan.orderQty"),
            prop: "orderQty",
            minWidth: 120,
          },
          {
            label: this.$t("ui.data.DemandPlan.netQty"),
            prop: "netQty",
            minWidth: 120,
          },
          {
            label: this.$t("ui.data.mouldingDayResult.totalQty"),
            prop: "totalQty",
            minWidth: 120,
          },
          {
            label: this.$t("ui.data.DemandPlan.unPostponeNetQty"),
            prop: "unPostponeNetQty",
            minWidth: 120,
          },
          {
            label: this.$t("ui.data.DemandPlan.postponeNetQty"),
            prop: "postponeNetQty",
            minWidth: 120,
          },
          {
            label: this.$t("ui.data.DemandPlan.isProduction"),
            prop: "isProduction",
            minWidth: 120,
            formatter: (row, column, value) => {
              return this.selectDictLabel(this.dict.type.biz_yes_no, value);
            },
          },
          {
            label: this.$t("ui.data.monthlyProductionPlan.heightQty"),
            prop: "heightQty",
            minWidth: 120,
          },
          // {
          //   label: this.$t("高优先级需求(含损耗)"),
          //   prop: "heightLossQty",
          //   minWidth: 120,
          // },
          {
            label: this.$t("ui.data.monthlyProductionPlan.factProdReqQty"),
            prop: "factProdReqQty",
            minWidth: 120,
          },
          {
            label: this.$t("ui.data.DemandPlan.conventionReserveQty"),
            prop: "conventionReserveQty",
            minWidth: 120,
          },
          {
            label: this.$t("ui.data.DemandPlan.cycleReserveQty"),
            prop: "cycleReserveQty",
            minWidth: 120,
          },

          ,
          {
            label: this.$t("common.remark"),
            prop: "remark",
            minWidth: 200,
            // sortable: "custom",
          },
        ];
      }

      return [];
    },
    searchColumns() {
      return [
      {
          label: this.$t("ui.data.colume.wms.unused.productCode"),
          prop: "materialCode",
        },
        {
          label: this.$t("ui.data.column.scheduleAdjust.productCodeDesc"),
          prop: "materialDesc",
        },
        // {
        //   prop: "factoryCode",
        //   label: this.$t("common.factory"),
        //   type: "select",
        //   dictData: this.dict.type.biz_factory_name,
        //   clearable: false,
        // },
        // {
        //   prop: "yearMonth",
        //   label: this.$t("ui.data.column.report.proSizeSummary.yearMonth"),
        //   type: "date",
        //   dateType: "month",
        //   valueFormat: "yyyy-MM",
        //   clearable: false,
        // },
        // {
        //   prop: "structureName",
        //   label: this.$t("ui.data.column.finishStock.structureName"),
        //   type: "select",
        //   dictData: this.structureList,
        //   filterable: true,
        // },
      ];
    },
  },
  methods: {
    handleClick() {
      this.page = {
        current: 1,
        pageSize: 20,
        total: 0,
      };
      this.$nextTick(() => {
        this.getList();
      });
    },
    handleAdd() {
      if (this.$refs.infoRef) {
        this.$refs.infoRef.show();
      }
    },
    handleEdit(row) {
      if (this.$refs.infoRef) {
        this.$refs.infoRef.show(row);
      }
    },
    handleDelete(row) {
      this.$confirm(this.$t("common.confirm.delete"), {
        type: "warning",
      }).then(() => {
        const ids = row.id;
        // removeProductMoldingLimit({ ids }).then((data) => {
        //   this.$modal.msgSuccess(data.msg);
        //   this.$set(this.page, "current", 1);
        //   this.getList();
        // });
      });
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
    handelSuccess() {
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
    handleSelectionChange(rows) {
      this.selection = rows;
    },
    handleExport() {
      downloadLink("/mdm/productMoldingLimit/export", this.formatParams(false));
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

      if (params.createTime && params.createTime[0]) {
        params.createTimeStart = params.createTime[0];
        params.createTimeEnd = params.createTime[1];
        params.createTime = undefined;
      }
      if (params.yearMonth) {
        let arr = params.yearMonth.split("-");
        params.year = arr[0];
        params.month = arr[1];
        params.yearMonth = "";
      }
      return params;
    },
    // api
    async getList() {
      this.loading = true;
      this.show = false;
      try {
        let data;
        if (this.activeName == "first") {
          data = await listDemandPlan(this.formatParams());
        }
        if (this.activeName == "second") {
          data = await listSupplyOrderPool(this.formatParams());
        }
        if (this.activeName == "three") {
          data = await listProductionPlan(this.formatParams());
        }
        if (this.activeName == "four") {
          data = await listMonthPlanNoProductionPlan(this.formatParams());
        }

        this.data = data.rows;
        this.page.total = data.total;
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
        this.show = true;
      }
    },
  },
  created() {
    // const now = new Date();
    // const year = now.getFullYear(); // 2024
    // const month = now.getMonth() + 1; // 注意：月份从0开始，需要+1
    // let defaultParams = {
    //   yearMonth: `${year}-${month < 10 ? "0" + month : month}`,
    //   factoryCode: "116",
    // };

    // console.log('created',this.$route.query)
    this.search = {
      ...this.$route.query,
    };
    this.query = {
      ...this.$route.query,
    };
    this.getList();
  },
  activated() {
    console.log("activated", this.$route.query);
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
