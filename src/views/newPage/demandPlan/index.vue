<template>
  <basic-container>
    <page-table
      :calcHeight="true"
      v-loading="loading"
      :columns="columns"
      :searchColumns="searchColumns"
      :row-class-name="tableRowClassName"
      :data="data"
      :page="page"
      :search="search"
      @reset="refreshSearch"
      @search="handleSearch"
      @pageChange="handlePageChange"
      @sort-change="handleSortChange"
      @selection-change="handleSelectionChange"
      :showSummary="false"
      :isReset="true"
      :selectArea="false"
    >
      <template slot="header">
        <el-button
          type="primary"
          :loading="createLoading"
          plain
          @click="generPlan"
          v-hasPermi="['monthplan:demandPlan:createMonthRequire']"
          >{{ $t("ui.data.DemandPlan.createMonthRequire") }}
        </el-button>
        <el-button
          type="primary"
         :loading="btnLoading"
          plain
           v-hasPermi="['monthplan:demandPlan:confirmSubmit']"
          @click="handleSubmit"
          >{{ $t("提交确认") }}
        </el-button>
        <el-button
          type="primary"
          plain
          :loading="btnLoading"
           v-hasPermi="['monthplan:demandPlan:cancelSubmit']"
          @click="handleRevoke"
          >{{ $t("撤销提交") }}
        </el-button>
        <!-- <el-button
          type="success"
          plain
          v-hasPermi="['monthplan:productionMouldConfiguration:add']"
          @click="handleChanged"
          >{{ $t("优先级调整") }}
        </el-button> -->
        <!-- <el-button
          type="primary"
          plain
          v-hasPermi="['monthplan:productionMouldConfiguration:add']"
          @click="handleAdd"
          >{{ $t("ui.frame.btn.add") }}
        </el-button> -->
        <!-- <el-button
          type="danger"
          plain
          :disabled="selection.length == 0"
          v-hasPermi="['monthplan:productionMouldConfiguration:remove']"
          @click="handleDelete(selection)"
          >{{ $t("ui.frame.btn.delete") }}
        </el-button> -->

        <!-- <el-button
          v-hasPermi="['monthplan:productionMouldConfiguration:import']"
          @click="$refs.tltUpload.handleImport()"
          >{{ $t("ui.frame.btn.import") }}
        </el-button> -->
        <el-button
          @click="handleExport"
          v-hasPermi="['monthplan:demandPlan:export']"
          >{{ $t("ui.frame.btn.export") }}
        </el-button>
      </template>
      <template slot="headerRight">
        <span class="stat-info">
          <span
            >{{ $t("库存总量") }}:
            <span class="stat-value"> {{ stat.stockQty }} </span></span
          >
          <span
            >{{ $t("订单总量") }}:
            <span class="stat-value"> {{ stat.orderQty }} </span></span
          >
          <span
            >{{ $t("排产净需求总量") }}:
            <span class="stat-value"> {{ stat.netQty }} </span></span
          >
        </span>
      </template>
    </page-table>
    <!-- <el-button style="display: none" ref="hidePopoverBtnRef"></el-button> -->
    <tlt-upload
      ref="tltUpload"
      downloadUrl="/monthplan/productionMouldConfiguration/importTemplate"
      uploadUrl="/monthplan/productionMouldConfiguration/importData"
      @uploadSuccess="getList"
    />
    <infoDialog ref="infoRef" @success="getListResize" />
    <el-dialog
      :title="title"
      :visible="visible"
      width="400px"
      @close="hide"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      :append-to-body="true"
    >
      <info-form
        class="form-item-height"
        ref="form"
        :form="form"
        :rules="rules"
        :columns="formColumns"
        label-position="right"
        label-width="160px"
        v-loading="loading"
      >
      </info-form>
      <template slot="footer">
        <el-button @click="hide">{{
          this.$t("common.button.cancel")
        }}</el-button>
        <el-button type="primary" :loading="loading" @click="handleConfirm">{{
          this.$t("common.button.confirm")
        }}</el-button>
      </template>
    </el-dialog>
  </basic-container>
</template>
<script>
import { mapState, mapGetters } from "vuex";
import { downloadLink } from "@/utils/request";
import {  versionConfirm,deleteMonthPlanRequire } from "@/api/factory/console";
import {
  listDemandPlan,
  saveDemandPlan,
  genenrDemandPlan,
  getVersionSelect,
  totalDemandPlan
} from "@/api/monthplan/demandPlan";
import tltUpload from "@/components/tltUpload/tltUpload.vue";
import infoForm from "@/views/components/infoForm.vue";
import infoDialog from "./components/infoDialog.vue";

export default {
  name: "DemandPlan",
  components: {
    tltUpload,
    infoForm,
    infoDialog,
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
  ],
  provide() {
    return {
      parentDict: this.dict,
    };
  },
  data() {
    return {
      btnLoading:false,
      title: "优先级调整",
      versionList: [],
      createLoading: false,
      loading: false,
      visible: false,
      data: [],
      selection: [],
      page: {
        current: 1,
        pageSize: 20,
        total: 0,
      },
      sort: {},
      search: {
        mainPlanMonth: "",
      },
      query: {
        mainPlanMonth: "",
      },
      importDefaultValue: {},
      importRules: {},
      form: {},
      rules: {
        scam: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
        isIOne: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
      },
      formColumns: [
        {
          prop: "sam",
          label: this.$t("ui.data.column.monthplan.scmPriority"),
          type: "select",
        },
      ],
      stat:{

      }
    };
  },
  computed: {
    ...mapGetters("globalList", ["structureList"]),
    columns() {
      let columns = [
        // { type: "selection", fixed: "left" },
        {
          prop: "factoryCode",
          label: this.$t("common.factory"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_factory_name, value);
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
            return this.selectDictLabel(this.dict.type.biz_product_type, value);
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
          // width:120,
          // formatter: (row, column, value) => {
          //   return this.selectDictLabel(this.dict.type.biz_order_type, value);
          // },
          render: ({ row }) => {
            return (
              <div>
                {this.hasPermission("monthplan:demandPlan:edit") && (
                  <el-select
                    v-model={row.scmPriority}
                    onChange={(val) =>
                      this.handlePriorityChange(row, "scmPriority")
                    }
                  >
                    {this.dict.type.biz_yes_no.map((item) => (
                      <el-option
                        key={item.value}
                        label={item.label}
                        value={item.value}
                      ></el-option>
                    ))}
                  </el-select>
                )}
                {!this.hasPermission("monthplan:demandPlan:edit") && (
                  <span>
                    {this.selectDictLabel(
                      this.dict.type.biz_yes_no,
                      row.scmPriority
                    )}
                  </span>
                )}
              </div>
            );
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
        // {
        //   prop: "yearWeek",
        //   label: this.$t("ui.data.column.monthplan.weekYear"),
        // },
        // {
        //   prop: "isUniformity",
        //   label: this.$t("ui.data.column.monthplan.dynamicBalance"),
        //   formatter: (row, column, value) => {
        //     return this.selectDictLabel(this.dict.type.biz_yes_no, value);
        //   },
        // },
        // {
        //   prop: "isDynamicBalance",
        //   label: this.$t("ui.data.column.monthplan.uniformity"),
        //   formatter: (row, column, value) => {
        //     return this.selectDictLabel(this.dict.type.biz_yes_no, value);
        //   },
        // },
        {
          prop: "orderQty",
          label: this.$t("ui.data.DemandPlan.orderQty"),
        },
        // {
        //   prop: "stockQty",
        //   label: this.$t("ui.data.DemandPlan.stockQty"),
        // },
        {
          prop: "plannedSurplus",
          label: this.$t("ui.data.DemandPlan.plannedSurplus"),
        },
        {
          prop: "netQty",
          label: this.$t("ui.data.DemandPlan.netQty"),
          renderColumnHeader: () => {
            return (
              <span>
                {this.$t("ui.data.DemandPlan.netQty")}
                <el-tooltip
                  content={this.$t("排产净需求=实单高优先级+实单中优先级+周期排产储备")}
                  placement="top"
                >
                  <i class="el-icon-info"></i>
                </el-tooltip>
              </span>
            );
          },
        },
        {
          prop: "isProduction",
          label: this.$t("ui.data.DemandPlan.isProduction"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_yes_no, value);
          },
          width: 120,
          render: ({ row }) => {
            return (
              <div>
                {this.hasPermission("monthplan:demandPlan:edit") &&
                  row.isReachMinProductionQty == 0 && (
                    <el-select
                      v-model={row.isProduction}
                      onChange={(val) =>
                        this.handlePriorityChange(row, "isProduction")
                      }
                    >
                      {this.dict.type.biz_yes_no.map((item) => (
                        <el-option
                          key={item.value}
                          label={item.label}
                          value={item.value}
                        ></el-option>
                      ))}
                    </el-select>
                  )}
                {this.hasPermission("monthplan:demandPlan:edit") &&
                  row.isReachMinProductionQty == 1 && (
                    <el-select
                      disabled
                      v-model={row.isProduction}
                      onChange={(val) =>
                        this.handlePriorityChange(row, "isProduction")
                      }
                    >
                      {this.dict.type.biz_yes_no.map((item) => (
                        <el-option
                          key={item.value}
                          label={item.label}
                          value={item.value}
                        ></el-option>
                      ))}
                    </el-select>
                  )}
                {!this.hasPermission("monthplan:demandPlan:edit") && (
                  <span>
                    {this.selectDictLabel(
                      this.dict.type.biz_yes_no,
                      row.isProduction
                    )}
                  </span>
                )}
              </div>
            );
          },
        },
        {
          prop: "postponeNetQty",
          label: this.$t("ui.data.DemandPlan.postponeNetQty"),
          renderColumnHeader: () => {
            return (
              <span>
                {this.$t("ui.data.DemandPlan.postponeNetQty")}
                <el-tooltip
                  content={this.$t("净需求（含暂缓）=（实单高优先级+实单中优先级）净需求 +暂缓")}
                  placement="top"
                >
                  <i class="el-icon-info"></i>
                </el-tooltip>
              </span>
            );
          },
        },
        {
          prop: "unPostponeNetQty",
          label: this.$t("ui.data.DemandPlan.unPostponeNetQty"),
          renderColumnHeader: () => {
            return (
              <span>
                {this.$t("ui.data.DemandPlan.unPostponeNetQty")}
                <el-tooltip
                  content={this.$t("净需求（不含暂缓）=（实单高优先级+实单中优先级）净需求")}
                  placement="top"
                >
                  <i class="el-icon-info"></i>
                </el-tooltip>
              </span>
            );
          },
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
          listeners: {
            change: this.handleFactoryChange,
          },
        },
        {
          prop: "yearMonth",
          label: this.$t("ui.data.colume.yearMonth"),
          type: "date",
          dateType: "month",
          valueFormat: "yyyy-MM",
          clearable: false,
          listeners: {
            change: this.handleYearMonthChange,
          },
        },

        {
          prop: "monthPlanVersion",
          label: this.$t("ui.data.demandPlan.monthPlanVersion"),
          type: "select",
          clearable: false,
          filterable: true,
          dictData: this.versionList,
        },
        {
          prop: "productTypeCode",
          label: this.$t("ui.data.column.monthplan.productType"),
          type: "select",
          dictData: this.dict.type.biz_product_type,
        },
        {
          prop: "structureName",
          label: this.$t("ui.data.column.finishStock.structureName"),
          type: "select",
          dictData: this.structureList,
          filterable: true,
        },
        // {
        //   prop: "orderPriority",
        //   label: this.$t("ui.data.DemandPlan.order"),
        //   type: "select",
        //   dictData: this.dict.type.biz_order_type,
        // },
        {
          prop: "isAlternateMaterial",
          label: this.$t("ui.data.demandPlan.isAlternateMaterial"),
          type: "select",
          dictData: this.dict.type.biz_yes_no,
        },
        {
          prop: "isReachMinProductionQty",
          label: this.$t("ui.data.DemandPlan.isReachMinProductionQty"),
          type: "select",
          dictData: this.dict.type.biz_yes_no,
        },
        {
          prop: "materialCode",
          label: this.$t("ui.data.column.monthplan.oriMaterialCode"),
        },
        {
          prop: "materialDesc",
          label: this.$t("ui.data.column.scheduleAdjust.productCodeDesc"),
        },
        {
          prop: "scmPriority",
          label: this.$t("ui.data.column.monthplan.scmPriority"),
          type: "select",
          dictData: this.dict.type.biz_yes_no,
        },
        {
          prop: "isProduction",
          label: this.$t("ui.data.DemandPlan.isProduction"),
          type: "select",
          dictData: this.dict.type.biz_yes_no,
        },
        {
          prop: "viewFlag",
          label: this.$t("显示所有净需求"),
          type: "select",
          dictData: this.dict.type.biz_yes_no,
        },
        {
          prop: "productionType",
          label: this.$t("ui.data.DemandPlan.productionType"),
          type: "select",
          dictData: this.dict.type.biz_schedule_type,
        },
      ];
    },
  },
  methods: {
    async handleSubmit(){
      try {
        this.btnLoading=true
        const params = {
          ...this.search,
          ...this.query,
        };

        let obj={
          factoryCode:params.factoryCode,
          monthPlanVersion:params.monthPlanVersion
        }
        if (params.yearMonth) {
          let arr = params.yearMonth.split("-");
          obj.year = arr[0];
          obj.month = arr[1];
        }

        const res = await versionConfirm(obj);
        this.$modal.msgSuccess(res.msg);
        this.$set(this.page, "current", 1);
        this.getList();
      } catch (error) {
        console.log(error);
        this.loading = false;
      }finally{
        this.btnLoading=false
      }
    },
    async handleRevoke(){
      this.$confirm(this.$t("该需求版本已做月计划，确认撤销？"), {
        type: "warning",
      }).then(() => {
        this.btnLoading=true
        const params = {
          ...this.search,
          ...this.query,
        };
        let obj={
          factoryCode:params.factoryCode,
          monthPlanVersion:params.monthPlanVersion
        }
        if (params.yearMonth) {
          let arr = params.yearMonth.split("-");
          obj.year = arr[0];
          obj.month = arr[1];
        }
        deleteMonthPlanRequire(obj).then((data) => {
          this.$modal.msgSuccess(data.msg);
          this.$set(this.page, "current", 1);
          this.getList();
        }).finally(()=>{
          this.btnLoading=false
        })
      });
    },
    refreshSearch(){
      this.search={
        factoryCode:this.search.factoryCode,
        yearMonth:this.search.yearMonth,
        monthPlanVersion:this.search.monthPlanVersion
      }
      this.query={
        factoryCode:this.search.factoryCode,
        yearMonth:this.search.yearMonth,
        monthPlanVersion:this.search.monthPlanVersion
      }
      this.getList()
    },
    getListResize() {
      this.search = {
        ...this.query,
      };
      this.query = {
        ...this.search,
      };
      // this.getList();
      this.getVersionList(true);
    },
    tableRowClassName({ row, rowIndex }) {
      if (row.isReachMinProductionQty == 0) {
        return "warning-row";
      }
      return "";
    },
    handleYearMonthChange(val) {
      this.search = {
        ...this.search,
        yearMonth: val,
      };
      this.query = {
        ...this.search,
        yearMonth: val,
      };
      this.getVersionList();
    },
    handleFactoryChange(val) {
      this.search = {
        ...this.search,
        factoryCode: val,
      };
      this.query = {
        ...this.search,
        factoryCode: val,
      };
      this.getVersionList();
    },
    hasPermission(permission) {
      const permissions = this.$store.state.user.permissions || [];
      if (Array.isArray(permission)) {
        return permission.some((perm) => permissions.includes(perm));
      }
      return permissions.includes(permission);
    },
    async generPlan() {
      this.handleAdd();
      return;
      // try {
      //   this.createLoading=true
      //   let res = await genenrDemandPlan(this.formatParams());
      //   this.$modal.msgSuccess(res.msg);
      //   this.getList();
      //   this.createLoading=false
      // } catch (err) {
      //   this.createLoading=false
      // }
    },
    save() {},
    hide() {
      this.$refs.form.triggerResetForm();
      this.visible = false;
    },
    handleConfirm() {
      this.$refs.form.triggerConfirm(this.save);
    },
    handleChanged() {
      (this.formColumns = [
        {
          prop: "scm",
          label: this.$t("ui.data.column.monthplan.scmPriority"),
          type: "select",
        },
      ]),
        (this.title = "优先级调整");
      this.visible = true;
    },
    handleRow() {
      (this.formColumns = [
        {
          prop: "ui.data.DemandPlan.isProduction",
          label: this.$t("ui.data.DemandPlan.isProduction"),
          type: "select",
        },
      ]),
        (this.title = "ui.data.DemandPlan.isProduction");
      this.visible = true;
    },
    handleAdd() {
      if (this.$refs.infoRef) {
        this.$refs.infoRef.show(this.query);
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
      }).then(() => {});
    },
    handlePriorityChange(row, type) {
      let params = {};
      if (type == "scmPriority") {
        params = {
          id: row.id,
          scmPriority: row.scmPriority,
        };
      }else{
        params = {
          id: row.id,
          isProduction: row.isProduction,
        };
      }
      // let params = {
      //   id: row.id,
      //   orderPriority: val,
      // };
      saveDemandPlan(params)
        .then((res) => {
          // this.$modal.msgSuccess(res.msg);
          this.getList();
        })
        .catch((err) => {
          console.log(err);
        });
    },
    handleSearch(data) {
      this.query = data;
      this.$set(this.page, "current", 1);
      // this.getList();
      this.getVersionList(true,false);
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
      // downloadLink("/monthplan/demandPlan/export", this.formatParams(false));
      downloadLink("/monthplan/demandPlanSum/export", this.formatParams(false));
    },

    handleSelectionChange(rows) {
      this.selection = rows;
    },

    handleBuild() {
      if (this.$refs.buildRef) {
        this.$refs.buildRef.show();
      }
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
        const [year, month] = params.yearMonth.split("-");
        params.year = year;
        params.month = month;
      }

      return params;
    },
    // api
    async getList() {
      try {
        this.loading = true;

        const data = await listDemandPlan(this.formatParams());
        this.data = data.rows;
        this.page.total = data.total;
        this.getTotal()
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
    },
    async getTotal(){
      try {

        const data = await totalDemandPlan(this.formatParams());
        this.stat=data

      } catch (error) {
        console.error(error);
      } finally {this.loading = false;
      }
    },
    async getVersionList(isGet,isSet=true) {
      if (isGet) {
        this.loading = true;
      }
      try {
        const data = await getVersionSelect(this.formatParams());
        let list = [];
        for (let i = 0; i < data.length; i++) {
          let obj = {
            label: data[i],
            value: data[i],
          };
          list.push(obj);
        }
        this.versionList = list;
        if(!isSet)return
        if (list.length > 0) {
          this.$set(this.search, "monthPlanVersion", list[0].value);
          this.$set(this.query, "monthPlanVersion", list[0].value);
        } else {
          this.$set(this.search, "monthPlanVersion", "");
          this.$set(this.query, "monthPlanVersion", "");
        }
      } catch (error) {
        console.error(error);
        this.loading = false;
      } finally {
        if (isGet) {
          this.$set(this.page, "current", 1);
          this.getList();
        }
      }
    },
  },
  created() {
    console.log(this.$route.query);
    if (this.$route.query.yearMonth) {
      let defaultParams = {
        ...this.$route.query,
      };
      this.search = {
        ...defaultParams,
      };
      this.query = {
        ...defaultParams,
      };
    } else {
      const now = new Date();
      // now.setMonth(now.getMonth() + 1);
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
    }

    this.getVersionList(true);
    // this.getList();
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
