
<template>
  <basic-container>
    <page-table
      tableRef="ProSizeSummaryMainTable"
      :calcHeight="true"
      v-loading="loading"
      :columns="columns"
       :row-class-name="tableRowClassName"
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
        <el-button
          @click="goProductionMonthPlanInit"
          v-hasPermi="['monthplan:mpStructureAllocation:list']"
          >{{ $t("查看结构排产") }}</el-button
        >
        <el-button
          :loading="syncLoading"
          @click="handleSyncAdjustedMonthPlan"
          v-hasPermi="['monthplan:factoryMonthPlanFinalResult:sync']"
          >{{ $t("推送SCM/MES") }}</el-button
        >
      </template>
      <template slot="headerRight"> </template>
    </page-table>
    <el-dialog
      title="推送SCM/MES"
      :visible.sync="syncDialog.visible"
      width="520px"
      append-to-body
      @close="resetSyncDialog"
    >
      <el-form
        ref="syncForm"
        :model="syncDialog.form"
        :rules="syncDialog.rules"
        label-width="120px"
      >
        <el-form-item label="年月" prop="yearMonth">
          <el-date-picker
            v-model="syncDialog.form.yearMonth"
            type="month"
            value-format="yyyy-MM"
            format="yyyy-MM"
            placeholder="请选择年月"
            style="width: 100%"
            @change="handleSyncBaseChange"
          />
        </el-form-item>
        <el-form-item label="分厂" prop="factoryCode">
          <el-select
            v-model="syncDialog.form.factoryCode"
            placeholder="请选择分厂"
            filterable
            clearable
            style="width: 100%"
            @change="handleSyncBaseChange"
          >
            <el-option
              v-for="dict in dict.type.biz_factory_name"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="月计划版本" prop="productionVersion">
          <el-select
            v-model="syncDialog.form.productionVersion"
            placeholder="请选择月计划版本"
            filterable
            clearable
            style="width: 100%"
            :loading="syncDialog.versionLoading"
            @change="handleSyncProductionVersionChange"
          >
            <el-option
              v-for="item in syncProductionVersionOptions"
              :key="item"
              :label="item"
              :value="item"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="需求计划版本" prop="lastMonthPlanVersion">
          <el-select
            v-model="syncDialog.form.lastMonthPlanVersion"
            placeholder="请选择需求计划版本"
            filterable
            clearable
            style="width: 100%"
            :loading="syncDialog.versionLoading"
            @change="handleSyncDemandVersionChange"
          >
            <el-option
              v-for="item in syncDemandVersionOptions"
              :key="item.optionKey"
              :label="item.lastMonthPlanVersion"
              :value="item.optionKey"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button @click="syncDialog.visible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="syncLoading"
          @click="submitSyncAdjustedMonthPlan"
        >确定</el-button>
      </span>
    </el-dialog>
  </basic-container>
</template>
<script>
//lib
import moment from "moment";
import Big from "big.js";
import {mapGetters} from "vuex";
//utils
import {downloadLink} from "@/utils/request";
//interface
import {
  getFinalResultVersionList,
  getProductionMonthType,
  listProduction,
  syncAdjustedMonthPlanToScmAndMes,
} from "@/api/monthplan/monthlyProductionPlan";
import {statisticsResult,} from "@/api/monthplan/adjustStructure";
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
      syncLoading: false,
      syncDialog: {
        visible: false,
        versionLoading: false,
        versionList: [],
        form: {
          yearMonth: "",
          factoryCode: "",
          productionVersion: "",
          lastMonthPlanVersion: "",
          monthPlanVersion: "",
        },
        rules: {
          yearMonth: [{ required: true, message: "请选择年月", trigger: "change" }],
          factoryCode: [{ required: true, message: "请选择分厂", trigger: "change" }],
          productionVersion: [{ required: true, message: "请选择月计划版本", trigger: "change" }],
          lastMonthPlanVersion: [{ required: true, message: "请选择需求计划版本", trigger: "change" }],
        },
      },
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
          width: 160,
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
          width: 180,
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
          label: this.$t("排产类型"),
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
          width: 180,
        },
        {
          prop: "textNo",
          label: this.$t("ui.data.column.trialPlan.textNo"),
          width: 180,
        },
        {
          prop: "lhNo",
          label: this.$t("ui.data.column.trialPlan.lhNo"),
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
        // {
        //   prop: "averageSaleQty",
        //   label: this.$t("ui.data.defectiveStock.averageSaleQty"),
        //   width: 120,
        // },
        // {
        //   prop: "inventorySalesRatio",
        //   label: this.$t("ui.data.monthlyProductionPlan.inventorySalesRatio"),
        //   width: 120,
        // },
        {
          prop: "dayLhQty",
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
    syncProductionVersionOptions() {
      const versionSet = new Set();
      this.syncDialog.versionList.forEach((item) => {
        if (item.productionVersion) {
          versionSet.add(item.productionVersion);
        }
      });
      return Array.from(versionSet);
    },
    syncDemandVersionOptions() {
      return this.syncDialog.versionList.filter((item) => {
        return item.productionVersion === this.syncDialog.form.productionVersion;
      });
    },
  },
  methods: {
    goProductionMonthPlanInit(){
      if(this.data.length==0){
        return this.$modal.msgWarning('暂无数据');
      }
      let date =this.data[0]
      this.$router.push({
        // path: `/monthPlanManagement/console/console/productionMonthPlanInit/${date.productionNo}`,
        name: 'ProductionMonthPlanInit',
        params: { id: date.productionNo },
        query: {
          year:date.year ,
          month:date.month,
          factoryCode:date.factoryCode,
          monthPlanVersion: date.monthPlanVersion,
          productionVersion: date.productionVersion,
        },
      });
    },
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
    // 打开手动推送弹框，由用户选择年月、分厂和版本后再推送
    handleSyncAdjustedMonthPlan() {
      this.syncDialog.visible = true;
      this.syncDialog.form.yearMonth = this.normalizeYearMonth(this.query.yearMonth);
      this.syncDialog.form.factoryCode = this.query.factoryCode || "";
      this.syncDialog.form.productionVersion = "";
      this.syncDialog.form.lastMonthPlanVersion = "";
      this.syncDialog.form.monthPlanVersion = "";
      this.syncDialog.versionList = [];
      this.$nextTick(() => {
        if (this.$refs.syncForm) {
          this.$refs.syncForm.clearValidate();
        }
      });
      this.loadSyncVersionList(true);
    },
    // 年月或分厂变化后，需要重新加载可推送版本，避免沿用旧条件下的版本
    handleSyncBaseChange() {
      this.syncDialog.form.productionVersion = "";
      this.syncDialog.form.lastMonthPlanVersion = "";
      this.syncDialog.form.monthPlanVersion = "";
      this.syncDialog.versionList = [];
      this.loadSyncVersionList(false);
    },
    // 月计划版本变化后，清空需求计划版本，确保提交的原需求版本与所选需求版本匹配
    handleSyncProductionVersionChange() {
      this.syncDialog.form.lastMonthPlanVersion = "";
      this.syncDialog.form.monthPlanVersion = "";
    },
    // 选择需求计划版本时，同时记录该版本对应的原始需求计划版本
    handleSyncDemandVersionChange(optionKey) {
      const selectedVersion = this.syncDialog.versionList.find((item) => item.optionKey === optionKey);
      this.syncDialog.form.monthPlanVersion = selectedVersion ? selectedVersion.monthPlanVersion : "";
    },
    // 关闭弹框时清空表单和版本列表，避免下一次打开残留旧选择
    resetSyncDialog() {
      this.syncDialog.form = {
        yearMonth: "",
        factoryCode: "",
        productionVersion: "",
        lastMonthPlanVersion: "",
        monthPlanVersion: "",
      };
      this.syncDialog.versionList = [];
    },
    // 加载当前年月和分厂下可推送的调整后版本
    async loadSyncVersionList(showWarning) {
      const { yearMonth, factoryCode } = this.syncDialog.form;
      if (!yearMonth || !factoryCode) {
        return;
      }
      const yearMonthInfo = this.parseYearMonth(yearMonth);
      if (!yearMonthInfo) {
        return;
      }
      try {
        this.syncDialog.versionLoading = true;
        const res = await getFinalResultVersionList({
          factoryCode,
          year: yearMonthInfo.year,
          month: yearMonthInfo.month,
        });
        const rows = res.rows || [];
        this.syncDialog.versionList = rows
          .filter((item) => {
            return item.productionVersion && item.monthPlanVersion && item.lastMonthPlanVersion;
          })
          .map((item) => {
            return {
              ...item,
              optionKey: `${item.productionVersion}__${item.monthPlanVersion}__${item.lastMonthPlanVersion}`,
            };
          });
        if (showWarning && this.syncDialog.versionList.length === 0) {
          this.$modal.msgWarning("暂无可推送数据");
        }
      } catch (error) {
        console.error(error);
      } finally {
        this.syncDialog.versionLoading = false;
      }
    },
    // 校验弹框选择并同步推送调整后的月计划到SCM和MES
    submitSyncAdjustedMonthPlan() {
      this.$refs.syncForm.validate((valid) => {
        if (!valid) {
          return;
        }
        const selectedVersion = this.syncDialog.versionList.find((item) => {
          return item.optionKey === this.syncDialog.form.lastMonthPlanVersion;
        });
        if (!selectedVersion) {
          this.$modal.msgWarning("暂无可推送数据");
          return;
        }
        const yearMonthInfo = this.parseYearMonth(this.syncDialog.form.yearMonth);
        if (!yearMonthInfo) {
          this.$modal.msgWarning("请选择正确的年月");
          return;
        }
        const params = {
          factoryCode: this.syncDialog.form.factoryCode,
          year: yearMonthInfo.year,
          month: yearMonthInfo.month,
          monthPlanVersion: selectedVersion.monthPlanVersion,
          lastMonthPlanVersion: selectedVersion.lastMonthPlanVersion,
          productionVersion: this.syncDialog.form.productionVersion,
        };
        this.$confirm(this.$t("确定推送调整后的月计划到SCM/MES？"), {
          type: "warning",
        }).then(async () => {
          try {
            this.syncLoading = true;
            const res = await syncAdjustedMonthPlanToScmAndMes(params);
            this.$modal.msgSuccess(res.msg);
            this.syncDialog.visible = false;
            await this.getList();
          } catch (error) {
            console.error(error);
          } finally {
            this.syncLoading = false;
          }
        });
      });
    },
    // 将页面查询或弹框年月统一转换成yyyy-MM，便于日期组件回显
    normalizeYearMonth(yearMonth) {
      if (!yearMonth) {
        return "";
      }
      const date = moment(yearMonth, ["YYYY-MM", "YYYY-M"], true);
      return date.isValid() ? date.format("YYYY-MM") : "";
    },
    // 将yyyy-MM拆分为后端所需的year、month
    parseYearMonth(yearMonth) {
      const normalizedYearMonth = this.normalizeYearMonth(yearMonth);
      if (!normalizedYearMonth) {
        return null;
      }
      const [year, month] = normalizedYearMonth.split("-");
      return {
        year: Number(year),
        month: Number(month),
      };
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
        // this.data = res.rows;
        this.page.total = res.total;
        if (res.rows.length != 0) {
          this.getStatisticsResult(res.rows[0],res.rows);
        }else{
          this.data = [];
        }
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
    },
      //渲染统计颜色
      tableRowClassName({ row, rowIndex }) {
      if (row.showBackground) {
        return row.showBackground;
      }
      if (row.adjustFlag == 1) {
        return "warning-row";
      }
      return "";
    },

    //调整结果统计
    async getStatisticsResult(data,resultList) {
      try {
        let params = {
          factoryCode: data.factoryCode,
          year: data.year,
          month: data.month,
          productionVersion: data.productionVersion,
          tempFlag:0
        };
        let res = await statisticsResult(params);



        let list = this.insertDataAfterEachName(resultList, res.rows);
        this.data = list;
      } catch (err) {
        console.log(err);
      } finally {
      }
    },
    //调整结果插入数据
    insertDataAfterEachName(arr, statistList) {
      if (!arr.length) return [];

      const result = [];
      for (let i = 0; i < arr.length; i++) {
        const current = arr[i];
        const next = arr[i + 1];
        // 添加当前数据
        result.push(current);
        console.log(current.structureName);
        // 如果下一个元素不存在或structureName不同，说明这是当前分组的最后一项
        if (!next || next.structureName !== current.structureName) {
          console.log(i);
          // 在当前分组后插入两条数据
          for (let i = 0; i < statistList.length; i++) {
            if (statistList[i].structureName == current.structureName) {
              let changeMould={
                structureName: current.structureName,
                showBackground: "light-yellow",
              }
              let embryoCount = {
                structureName: current.structureName,
                showBackground: "light-green",
              };
              let lhMachines = {
                structureName: current.structureName,
                showBackground: "light-blue",
              };
              embryoCount.productionNo = "胎胚种类数";
              lhMachines.productionNo = "硫化机台数";
              changeMould.productionNo = "换模次数";
              for (let j = 1; j <= 31; j++) {
                const key = `day${j}`;

                if (statistList[i][key]) {
                  let dayData = JSON.parse(statistList[i][key]);
                  // embryoCount.push{
                  //   `day${j}`:dayData.EmbryoCount
                  // }
                  embryoCount[key] = dayData.embryoCount;
                  lhMachines[key] = dayData.lhMachines;
                  changeMould[key] = dayData.changeMould;
                }
              }
              result.push(embryoCount);
              result.push(lhMachines);
              // result.push(changeMould);
            }
          }
        }
      }

      return result;
    },
    async updateList() {
      this.loading = true;
      await this.getProductionMonthType();
      await this.getList();
    },
  },
  created() {
    const now = new Date();
    // const year = now.getFullYear();
    // const month = String(now.getMonth() + 1).padStart(2, "0"); // 月份从0开始，需要+1
    const nextMonth = new Date(now.getFullYear(), now.getMonth() + 1, 1);
      const year = nextMonth.getFullYear();
      const month = nextMonth.getMonth() + 1; // 月份从0开始，需要+1
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
