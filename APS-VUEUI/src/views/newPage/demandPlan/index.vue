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
          :disabled="selection.length != 1"
          type="primary"
             v-hasPermi="['monthplan:demandPlan:edit']"
          @click="handleEdit(selection[0])"
          >{{ $t("ui.data.demandPlan.structurePriority") }}
        </el-button>
        <el-button
          type="primary"
          :loading="createLoading"
          plain
          @click="generPlan"
          v-hasPermi="['monthplan:demandPlan:createMonthRequire']"
          >{{ $t("ui.data.demandPlan.createMonthRequire") }}
        </el-button>
        <el-button
          type="primary"
         :loading="btnLoading"
          plain
           v-hasPermi="['monthplan:demandPlan:confirmSubmit']"
          @click="handleSubmit"
          >{{ $t("ui.data.demandPlan.submitConfirm") }}
        </el-button>
        <el-button
          type="primary"
          plain
          :loading="btnLoading"
           v-hasPermi="['monthplan:demandPlan:cancelSubmit']"
          @click="handleRevoke"
          >{{ $t("ui.data.demandPlan.revokeSubmit") }}
        </el-button>
        <el-button
          type="primary"
          plain
          :loading="btnLoading"
           v-hasPermi="['monthplan:demandPlan:extendsConfiguration']"
          @click="handleExtends"
          >{{ $t("ui.data.demandPlan.inheritConfig") }}
        </el-button>
        <!-- <el-button
          type="success"
          plain
          v-hasPermi="['monthplan:productionMouldConfiguration:add']"
          @click="handleChanged"
          >{{ $t("ui.data.demandPlan.priorityAdjust") }}
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
            >{{ $t("ui.data.total.stockQty") }}:
            <span class="stat-value"> {{ stat.stockQty }} </span></span
          >
          <span
            >{{ $t("ui.data.total.orderQty") }}:
            <span class="stat-value"> {{ stat.orderQty }} </span></span
          >
          <span
            >{{ $t("ui.data.total.netQty") }}:
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
    <editDialog ref="editRef" @success="getListResize" />
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
  totalDemandPlan,
  extendsConfiguration
} from "@/api/monthplan/demandPlan";
import tltUpload from "@/components/tltUpload/tltUpload.vue";
import infoForm from "@/views/components/infoForm.vue";
import infoDialog from "./components/infoDialog.vue";
import editDialog from "./components/editDialog.vue";

export default {
  name: "DemandPlan",
  components: {
    tltUpload,
    infoForm,
    infoDialog,
    editDialog
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
      title: this.$t("ui.data.demandPlan.priorityAdjust"),
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

      },
      lastRouteLoadKey: "",
    };
  },
  computed: {
    ...mapGetters("globalList", ["structureList"]),
    columns() {
      let columns = [
        { type: "selection", fixed: "left" },
        {
          prop: "factoryCode",
          width: 120,
          align: "center",
          label: this.$t("ui.data.column.demandPlan.factoryCode"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_factory_name, value);
          },
        },
        {
          prop: "year",
          width: 80,
          align: "center",
          label: this.$t("ui.data.column.demandPlan.year"),
        },
        {
          prop: "month",
          align: "center",
          label: this.$t("ui.data.column.demandPlan.month"),
        },
        {
          prop: "productTypeCode",
          align: "center",
          label: this.$t("ui.data.column.demandPlan.productTypeCode"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_product_type, value);
          },
        },

        {
          prop: "locationType",
          align: "center",
          label: this.$t("ui.data.column.demandPlan.locationType"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_stor_type, value);
          },
        },
        {
          prop: "monthPlanVersion",
          align: "center",
          label: this.$t("ui.data.demandPlan.monthPlanVersion"),
          width: 180,
        },
        {
          prop: "brand",
          align: "center",
          label: this.$t("ui.data.column.demandPlan.brand"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_brand_type, value);
          },
          width: 120,
        },
        {
          prop: "scmPriority",
          align: "center",
          label: this.$t("ui.data.column.demandPlan.scmPriority"),
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
          prop: "structurePriority",
          align: "center",
          label: this.$t("ui.data.column.demandPlan.structurePriority"),
          width: 100,
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_yes_no, value);
          },

        },

        {
          prop: "structureName",
          align: "center",
          label: this.$t("ui.data.column.demandPlan.structureName"),
          width: 180,
        },
        {
          prop: "mainPattern",
          align: "center",
          label: this.$t("ui.data.column.demandPlan.mainPattern"),
          width: 120,
        },
        {
          prop: "materialCode",
          label: this.$t("ui.data.column.demandPlan.materialCode"),
          width: 120,
        },
        {
          prop: "materialDesc",
          label: this.$t("ui.data.column.demandPlan.materialDesc"),
          align: "left",
          minWidth: 350,
        },
        {
          prop: "productionType",
          label: this.$t("ui.data.column.demandPlan.productionType"),
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
          label: this.$t("ui.data.column.demandPlan.stockQty"),
          width: 120,
        },
        {
          prop: "sub2YearStockQty",
          label: this.$t("ui.data.column.demandPlan.sub2YearStockQty"),
          width: 120,
        },
        {
          prop: "sub1YearStockQty",
          label: this.$t("ui.data.column.demandPlan.sub1YearStockQty"),
          width: 120,
        },
        {
          prop: "currentYearStockQty",
          label: this.$t("ui.data.column.demandPlan.currentYearStockQty"),
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
          label: this.$t("ui.data.column.demandPlan.orderQty"),
        },
        // {
        //   prop: "stockQty",
        //   label: this.$t("ui.data.DemandPlan.stockQty"),
        // },
        {
          prop: "plannedSurplus",
          label: this.$t("ui.data.column.demandPlan.plannedSurplus"),
        },
        {
          prop: "netQty",
          label: this.$t("ui.data.column.demandPlan.netQty"),
          renderColumnHeader: () => {
            return (
              <span>
                {this.$t("ui.data.column.demandPlan.netQty")}
                <el-tooltip
                  content={this.$t("ui.data.tooltip.netQty")}
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
          label: this.$t("ui.data.column.demandPlan.isProduction"),
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
          prop: "isSchedule",
          label: this.$t("ui.data.column.demandPlan.isSchedule"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_yes_no, value);
          },
          width: 120,
          render: ({ row }) => {
            return (
              <div>
                {this.hasPermission("monthplan:demandPlan:edit") &&
                  row.conventionReserveQty > 0 && (
                    <el-select
                      v-model={row.isSchedule}
                      onChange={(val) =>
                        this.handleIsScheduleChange(row, "isSchedule")
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
                {(!this.hasPermission("monthplan:demandPlan:edit") ||
				  row.conventionReserveQty == null ||
				  row.conventionReserveQty == 0) && (
                  <span>
                    {this.selectDictLabel(
                      this.dict.type.biz_yes_no,
                      row.isSchedule
                    )}
                  </span>
                )}
              </div>
            );
          },
        },
        {
          prop: "postponeNetQty",
          label: this.$t("ui.data.column.demandPlan.postponeNetQty"),
          renderColumnHeader: () => {
            return (
              <span>
                {this.$t("ui.data.column.demandPlan.postponeNetQty")}
                <el-tooltip
                  content={this.$t("ui.data.tooltip.postponeNetQty")}
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
          label: this.$t("ui.data.column.demandPlan.unPostponeNetQty"),
          renderColumnHeader: () => {
            return (
              <span>
                {this.$t("ui.data.column.demandPlan.unPostponeNetQty")}
                <el-tooltip
                  content={this.$t("ui.data.tooltip.unPostponeNetQty")}
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
          label: this.$t("ui.data.column.demandPlan.heightQty"),
        },
        {
          prop: "midQty",
          label: this.$t("ui.data.column.demandPlan.midQty"),
        },
        {
          prop: "postponeQty",
          label: this.$t("ui.data.column.demandPlan.postponeQty"),
        },
        {
          prop: "cycleReserveQty",
          label: this.$t("ui.data.column.demandPlan.cycleReserveQty"),
        },
        {
          prop: "conventionReserveQty",
          label: this.$t("ui.data.column.demandPlan.conventionReserveQty"),
        },
        {
          prop: "isReachMinProductionQty",
          label: this.$t("ui.data.column.demandPlan.isReachMinProductionQty"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_yes_no, value);
          },
        },
        {
          prop: "minProductionQty",
          label: this.$t("ui.data.column.demandPlan.minProductionQty"),
        },
        {
          prop: "remark",
          label: this.$t("ui.data.column.demandPlan.remark"),
        },
        {
          prop: "updateTime",
          label: this.$t("ui.data.column.demandPlan.updateTime"),
          width: 180,
        },
      ];

      return columns;
    },
    searchColumns() {
      return [
        {
          prop: "factoryCode",
          label: this.$t("ui.data.column.demandPlan.factoryCode"),
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
          label: this.$t("ui.data.column.demandPlan.productTypeCode"),
          type: "select",
          dictData: this.dict.type.biz_product_type,
        },
        {
          prop: "structureName",
          label: this.$t("ui.data.column.demandPlan.structureName"),
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
          label: this.$t("ui.data.column.demandPlan.isReachMinProductionQty"),
          type: "select",
          dictData: this.dict.type.biz_yes_no,
        },
        {
          prop: "materialCode",
          label: this.$t("ui.data.column.demandPlan.materialCode"),
        },
        {
          prop: "materialDesc",
          label: this.$t("ui.data.column.demandPlan.materialDesc"),
          minWidth: 350,
          align: "left",
        },
        {
          prop: "scmPriority",
          label: this.$t("ui.data.column.demandPlan.scmPriority"),
          type: "select",
          dictData: this.dict.type.biz_yes_no,
        },
        {
          prop: "structurePriority",
          label: this.$t("ui.data.column.demandPlan.structurePriority"),
          type: "select",
          dictData: this.dict.type.biz_yes_no,
        },
        {
          prop: "isProduction",
          label: this.$t("ui.data.column.demandPlan.isProduction"),
          type: "select",
          dictData: this.dict.type.biz_yes_no,
        },
        {
          prop: "viewFlag",
          label: this.$t("ui.data.demandPlan.showAllNetQty"),
          type: "select",
          dictData: this.dict.type.biz_yes_no,
        },
        {
          prop: "productionType",
          label: this.$t("ui.data.column.demandPlan.productionType"),
          type: "select",
          dictData: this.dict.type.biz_schedule_type,
        },
      ];
    },
  },
  watch: {
    $route: {
      immediate: false,
      handler() {
        this.initByRouteAndLoad();
      },
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
      this.$confirm(this.$t("ui.data.demandPlan.confirmRevoke"), {
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
    async handleExtends(){
      this.$confirm(this.$t("ui.data.demandPlan.confirmInherit"), {
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
        extendsConfiguration(obj).then((data) => {
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
        (this.title = this.$t("ui.data.demandPlan.priorityAdjust"));
      this.visible = true;
    },
    handleRow() {
      (this.formColumns = [
        {
          prop: "ui.data.column.demandPlan.isProduction",
          label: this.$t("ui.data.column.demandPlan.isProduction"),
          type: "select",
        },
      ]),
        (this.title = this.$t("ui.data.column.demandPlan.isProduction"));
      this.visible = true;
    },
    handleAdd() {
      if (this.$refs.infoRef) {
        this.$refs.infoRef.show(this.query);
      }
    },
    handleEdit(row) {
      if (this.$refs.editRef) {
        this.$refs.editRef.show(row);
      }
    },
    handleDelete(rows) {
      this.$confirm(this.$t("common.confirm.delete"), {
        type: "warning",
      }).then(() => {});
    },
    handleIsScheduleChange(row) {
      let params = {
          id: row.id,
          isSchedule: row.isSchedule,
        };
      saveDemandPlan(params)
        .then((res) => {
          // this.$modal.msgSuccess(res.msg);
          this.getList();
        })
        .catch((err) => {
          console.log(err);
        });
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
    initByRouteAndLoad() {
      const routeQuery = this.$route.query || {};
      const routePath = this.$route.path || "";
      const routeKey = `${routePath}?${JSON.stringify(routeQuery)}`;
      if (routeKey === this.lastRouteLoadKey) {
        return;
      }
      this.lastRouteLoadKey = routeKey;
      if (routeQuery.yearMonth) {
        this.search = {
          ...this.search,
          ...routeQuery,
        };
        this.query = {
          ...this.query,
          ...routeQuery,
        };
        this.getVersionList(true, false);
      } else {
        if (!this.search.factoryCode && !this.search.yearMonth) {
          const now = new Date();
          const nextMonth = new Date(now.getFullYear(), now.getMonth() + 1, 1);
          const year = nextMonth.getFullYear();
          const month = nextMonth.getMonth() + 1;
          const defaultParams = {
            factoryCode: "116",
            yearMonth: `${year}-${month}`,
          };
          this.search = {
            ...defaultParams,
          };
          this.query = {
            ...defaultParams,
          };
          this.getVersionList(true);
        } else {
          this.getVersionList(true, false);
        }
      }
    },
  },
  created() {
    this.initByRouteAndLoad();
  },
  activated() {
    this.initByRouteAndLoad();
  },
};
</script>
<style lang="scss" scoped>
.more-btn {
  margin: 2px 0;
  width: 100%;
}
</style>
