<template>
  <basic-container>
    <page-table
      tableRef="MouldingDayResultMainTable"
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
        <!-- <el-button
          type="primary"
          plain
          v-hasPermi="['monthplan:mouldingDayResult:edit']"
          @click="handleEdit(selection[0])"
          >{{ $t("ui.frame.btn.modify") }}</el-button
        > -->
        <!-- <el-buttonW
          type="danger"
          plain
          :disabled="selection.length === 0"
          v-hasPermi="['monthplan:mouldingDayResult:remove']"
          @click="handleDelete(selection)"
          >{{ $t("ui.frame.btn.delete") }}</el-button
        > -->
        <!-- <el-button
          v-hasPermi="['monthplan:mouldingDayResult:import']"
          @click="$refs.tltUpload.handleImport()"
          >{{ $t("ui.frame.btn.import") }}</el-button
        > -->
        <el-button
          @click="handleExport"
          v-hasPermi="['monthplan:mouldingDayResult:export']"
          >{{ $t("ui.frame.btn.export") }}</el-button
        >
      </template>
      <!-- <template slot="headerRight">
        <span class="stat-info">
          <span
            >排产SAP个数:
            <span class="stat-value"> {{ stat.productionCount }} </span></span
          >
          <span
            >未排SAP总量:
            <span class="stat-value">{{ stat.noProductionCount }}</span></span
          >
          <span
            >已排SAP总量:
            <span class="stat-value">{{ stat.productionSum }}</span></span
          >
          <span
            >提报的SAP个数:
            <span class="stat-value">{{ stat.reportCount }}</span></span
          >
          <span
            >提报的SAP总量:
            <span class="stat-value">{{ stat.reportSum }}</span></span
          >
          <span
            >备货量: <span class="stat-value">{{ stat.stockNum }}</span></span
          >
        </span>
      </template> -->
    </page-table>
    <!-- <el-button style="display: none" ref="hidePopoverBtnRef"></el-button> -->
    <tlt-upload
      ref="tltUpload"
      downloadUrl="/monthplan/mouldingDayResult/importTemplate"
      uploadUrl="/monthplan/mouldingDayResult/importData"
      @uploadSuccess="getList"
    />
    <infoDialog ref="infoRef" @success="getList" />
    <specDialog ref="specRef" @success="getList" />
  </basic-container>
</template>
<script>
//lib
import moment from "moment";
//utils
import { mapGetters } from "vuex";
import { downloadLink } from "@/utils/request";
//interface
import {
  listMouldingDayResult,
  removeNouldingDayResult,
  editMouldingDayResult,
  getVersionList,
  statistics,
} from "@/api/monthplan/mouldingDayResult";
import { listProductionPlan } from "@/api/monthplan/monthlyProductionPlan";
//components
import tltUpload from "@/components/tltUpload/tltUpload.vue";

import infoDialog from "./components/infoDialog.vue";
import specDialog from "./components/specDialog.vue";

export default {
  name: "MouldingDayResult",
  components: {
    tltUpload,
    infoDialog,
    specDialog,
  },
  dicts: [
    "biz_factory_name",
    "biz_product_type",
    "biz_brand_type",
    "biz_plan_type",
    "biz_construction_stage",
    "trial_status",
    "product_category",
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
      verList: [],
      dailyVisible: true,
      productionVersion: null,
      stat: {},
    };
  },
  computed: {
    ...mapGetters("globalList", ["structureList"]),
    columns() {
      let columns = [
      {
          prop: "factoryCode",
          label: this.$t("common.factory"),
          align: "center",

          formatter: (row) => {
            return this.selectDictLabel(
              this.dict.type.biz_factory_name,
              row.factoryCode
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
          prop: "monthPlanVersion",
          label: this.$t("ui.data.demandPlan.monthPlanVersion"),
          width: 180,
        },
        {
          prop: "productionVersion",
          label: this.$t("ui.data.productionMonthPlanInit.productionVersion"),
          width: 180,
        },
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
            return this.selectDictLabel(this.dict.type.product_category, value);
          },
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
          label: this.$t("ui.data.vulcanizationTable.cxMachine"),
          prop: "cxMachineCode",
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
      // columns.push({
      //   label: this.$t("ui.data.column.facMonthPlan.isImport"),
      //   prop: "isImport",
      //   align: "center",
      //   formatter: (row) => {
      //     return this.selectDictLabel(this.dict.type.biz_yes_no, row.isImport);
      //   },
      // });
      return columns;
    },
    searchColumns() {
      return [
        {
          label: this.$t("ui.data.column.finishStock.structureName"),
          prop: "structureName",
          type: "select",
          dictData: this.structureList,
          filterable: true,
        },
        // {
        //   prop: "productStatus",
        //   label: this.$t("ui.data.column.trialPlan.trialStatus"),
        //   type: "select",
        //   dictData: this.dict.type.trial_status,

        // },
        {
          label: this.$t("ui.data.column.capsuleChuck.productTypeCode"),
          prop: "productCategory",
          type: "select",
          dictData: this.dict.type.product_category,
        },
        {
          label: this.$t("ui.data.column.monthplan.productType"),
          prop: "productTypeCode",
          type: "select",
          dictData: this.dict.type.biz_product_type,
        },
        {
          label: this.$t("ui.data.column.trialPlan.specifications"),
          prop: "specifications",
        },
        {
          label: this.$t("ui.data.rubberMaterial.embryoDesc"),
          prop: "mainMaterialDesc",
        },
        {
          label: this.$t("ui.data.column.scheduleAdjust.productCodeDesc"),
          prop: "materialDesc",
        },
        {
          label: this.$t("ui.data.column.confMinProd.pattern"),
          prop: "mainPattern",
        },
        {
          label: this.$t("ui.data.column.trialPlan.trialStatus"),
          prop: "productStatus",
          type: "select",
          dictData: this.dict.type.trial_status,
        },
        // {
        //   label: this.$t("规格"),
        //   prop: "materialDesc",
        // },
        {
          label: this.$t("ui.data.column.monthplan.oriMaterialCode"),
          prop: "materialCode",
        },
        {
          label: this.$t("common.remark"),
          prop: "remark",
        },
      ];
    },
  },
  methods: {
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
    handleDelete(rows) {
      this.$confirm(this.$t("common.confirm.delete"), {
        type: "warning",
      }).then(() => {
        const ids = rows.map((row) => row.id).join(",");
        removemouldingDayResult({ ids }).then((data) => {
          this.$modal.msgSuccess(data.msg);
          this.$set(this.page, "current", 1);
          this.getList();
        });
      });
    },
    handleChangeStatus(status, row) {
      console.log(status);
      let label =
        status === "0"
          ? this.$t("ui.biz.alter.isOpen")
          : this.$t("ui.biz.alter.isStop");

      this.$confirm(label, {
        type: "warning",
      }).then(async () => {
        try {
          this.loading = true;
          const res = await editMouldingDayResult({
            ...row,
            status,
          });
          this.$modal.msgSuccess(res.msg);
          this.getList();
        } catch (error) {
          this.loading = false;
        }
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

    handleExport() {
      downloadLink(
        "/monthplan/factoryMonthPlanMouldDayResult/export",
        this.formatParams(false)
      );
    },

    handleSelectionChange(rows) {
      this.selection = rows;
    },
    handleYearChange(params) {
      this.getVersionList(params);
      console.log(1);
    },
    handleMonthChange(params) {
      this.getVersionList(params);
    },
    handleFactoryChange(params) {
      this.getVersionList(params);
    },
    handleChangeSpecCode(row) {
      this.$refs.specRef.show(row);
    },

    // utils
    updateTableHeaderlabel() {
      //  TODO 更新表头标题
    },

    formatParams(hasPage = true) {
      const params = {
        ...this.query,
        ...this.sort,
        // productionVersion: this.productionVersion,
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
        const data = await listProductionPlan(this.formatParams());
        console.log(data);
        this.data = data.rows;
        this.page.total = data.total;
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
    },
    async statistics(params) {
      try {
        const res = await statistics(params);
        console.log(res);
        this.stat = res;
      } catch (error) {
        this.stat = {};
      }
    },

    async getVersionList(params) {
      this.search = {
        ...this.query,
        ...params,
        monthPlanVersion: undefined,
      };

      if (!params.year || !params.month || !params.factoryCode) {
        return;
      }

      try {
        this.verList = [];

        const res = await getVersionList({
          year: this.search.year,
          month: this.search.month,
          factoryCode: this.search.factoryCode,
        });
        this.verList = res;

        console.log(this.verList);
      } catch (error) {
        console.error(error);
        this.verList = [];
      }
    },
  },
  created() {
    if (this.$route.query) {
      console.log(this.$route.query);
      let defaultParams = {
        ...this.$route.query,
      };
      this.search = {
        ...defaultParams,
      };
      this.query = {
        ...defaultParams,
      };
    }
    this.getList();
  },
  activated() {},
};
</script>
<style lang="scss" scoped>
.more-btn {
  margin: 2px 0;
  width: 100%;
}
.table-link {
  font-size: 11pt;
}
.stat-info {
  font-size: 12px;
  color: #676a6c;
  font-weight: bold;
  .stat-value {
    color: #0088cc;
  }
  span {
    margin-left: 5px;
  }
}
</style>
