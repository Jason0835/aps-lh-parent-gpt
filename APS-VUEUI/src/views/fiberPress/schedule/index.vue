
<template>
  <basic-container>
    <page-table
      tableRef="fiberPressScheduleMainTable"
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
      :row-style="rowStyle"
      :cell-style="cellStyle"
    >
      <template slot="header">
        <el-button
          type="warning"
          v-hasPermi="['xwyy:scheduleResult:autoPlan']"
          @click="handleAutoPlan"
          >{{ $t("ui.data.column.scheduleResult.autoPlan") }}</el-button
        >
        <el-button
          type="warning"
          v-hasPermi="['xwyy:scheduleResult:add']"
          @click="handleAdd"
          >{{ $t("ui.data.column.scheduleResult.insertOrder") }}</el-button
        >
        <el-button
          type="warning"

          @click="() => handleEdit(selection[0])"
          v-hasPermi="['xwyy:scheduleResult:edit']"
          >{{ $t("ui.frame.btn.modify") }}</el-button
        >
        <el-button
          type="danger"
          :disabled="selection.length === 0"
          v-hasPermi="['xwyy:scheduleResult:remove']"
          @click="handleDelete"
          >{{ $t("ui.frame.btn.delete") }}</el-button
        >
        <el-button
          v-hasPermi="['xwyy:scheduleResult:changeMachine']"
          type="primary"
          :disabled="selection.length === 0"
          @click="handleChangeMachine"
          >{{ $t("ui.data.column.scheduleResult.changeMachine") }}</el-button
        >
        <el-button
          v-hasPermi="['xwyy:scheduleResult:changePlan']"
          type="primary"
          @click="handleChangePlan"
          >{{ $t("ui.data.column.scheduleResult.changePlan") }}</el-button
        >
        <!-- <el-button
          v-hasPermi="['xwyy:scheduleResult:balance']"
          type="primary"
          @click="handleBalance"
          >{{ $t("ui.data.column.scheduleResult.balance") }}</el-button
        > -->
        <!-- <el-button
          v-hasPermi="['xwyy:scheduleResult:mergeProduct']"
          type="primary"
          >{{ $t("ui.data.column.scheduleResult.mergeProduct") }}</el-button
        > -->
        <el-button
          v-hasPermi="['xwyy:scheduleResult:combinationMiddleAndNight']"
          type="primary"
          :disabled="selection.length == 0"
          @click="handleCombinationMiddleAndNight"
          >{{ $t("ui.data.column.combinationMiddleAndNight") }}</el-button
        >

        <el-button
          v-hasPermi="['xwyy:scheduleResult:publish']"
          type="primary"
          :disabled="selection.length === 0"
          @click="handlePublish"
          >{{ $t("ui.data.column.scheduleResult.publish") }}</el-button
        >
        <el-dropdown>
          <el-button type="primary" style="margin-left: 10px">
            更多按钮<i class="el-icon-arrow-down el-icon--right"></i>
          </el-button>
          <el-dropdown-menu slot="dropdown">
            <el-dropdown-item>
              <el-button
                type="primary"
                class="more-btn"
                @click="handleExportUiExcel"
              >
                {{ $t("ui.frame.btn.export") }}
              </el-button>
            </el-dropdown-item>
            <el-dropdown-item>
              <el-button
                type="primary"
                class="more-btn"
                @click="$refs.tltUploadForm.handleImport(importDefaultValue)"
              >
                {{ $t("ui.frame.btn.import") }}
              </el-button>
            </el-dropdown-item>
            <el-dropdown-item>
              <el-button
                type="primary"
                class="more-btn"
                @click="$refs.tltUploadForm2.handleImport(importDefaultValue)"
              >
                {{ $t("导入2") }}
              </el-button>
            </el-dropdown-item>
            <el-dropdown-item v-hasPermi="['xwyy:finishQty:import']">
              <el-button
                type="primary"
                class="more-btn"
                @click="$refs.tltUploadForm3.handleImport(importDefaultValue)"
              >
                {{ $t("完成量导入") }}
              </el-button>
            </el-dropdown-item>
            <el-dropdown-item v-hasRole="['admin']">
              <el-button
                type="primary"
                class="more-btn"
                @click="handleChangeReleaseStatus"
              >
                {{ $t("ui.data.column.scheduleResult.changeReleaseStatus") }}
              </el-button>
            </el-dropdown-item>
          </el-dropdown-menu>
        </el-dropdown>
      </template>
      <template slot="headerRight">
        <span class="stat-info">
          <span>
            昨日早班合计：<span class="stat-value">{{
              stat.lastDayPlanQty === null ? "--" : stat.lastDayPlanQty
            }}</span
            >，
          </span>
          <span
            >夜班合计：<span class="stat-value">{{
              stat.dayPlanQty === null ? "--" : stat.dayPlanQty
            }}</span
            >，</span
          >
          <span
            >早班合计：<span class="stat-value">{{
              stat.nightPlanQty === null ? "--" : stat.nightPlanQty
            }}</span
            >，</span
          >
          <span
            >库存合计：<span class="stat-value">{{
              stat.stockQty === null ? "--" : stat.stockQty
            }}</span
            >，</span
          >
          <span
            >理论交班库存合计：<span class="stat-value">{{
              stat.theoreticClassStockQty === null
                ? "--"
                : stat.theoreticClassStockQty
            }}</span></span
          >
        </span>
      </template>
    </page-table>
    <!-- <el-button style="display: none" ref="hidePopoverBtnRef"></el-button> -->
    <autoPlanDialog ref="autoPlanRef" @success="getList" />
    <addDialog ref="addRef" @success="getList" />
    <editDialog ref="editRef" @success="getList" />
    <changeMachineDialog ref="changeMachineRef" @success="getList" />
    <changePlanDialog ref="changePlanRef" @success="getList" />
    <releaseStatusDialog
      ref="releaseStatusRef"
      :scheduleDate="this.query.scheduleDate"
      @success="getList"
    />
    <balanceDialog ref="balanceRef" @success="getList" />
    <allocateDialog
      ref="allocRef"
      :scheduleDate="this.query.scheduleDate"
      @success="getList"
    />
    <tlt-upload-form
      ref="tltUploadForm"
      title="导入纤维压延程结果数据"
      downloadUrl="/xwyy/scheduleResult/importTemplate"
      uploadUrl="/xwyy/scheduleResult/importScheduleData"
      @uploadSuccess="getList"
      :columns="[
        {
          label: '排程日期',
          prop: 'scheduleDate',
        },
      ]"
      :rules="importRules"
    />
    <tlt-upload-form
      ref="tltUploadForm2"
      title="导入纤维压延程结果数据"
      uploadUrl="/xwyy/scheduleResult/importScheduleData2"
      @uploadSuccess="getList"
      :columns="[
        {
          label: '排程日期',
          prop: 'scheduleDate',
        },
      ]"
      :rules="importRules"
    />
    <tlt-upload-form
      ref="tltUploadForm3"
      title="导入纤维压延程结果数据"
      uploadUrl="/xwyy/scheduleResult/importFinishQty"
      downloadUrl="/xwyy/scheduleResult/importFinishQtyTemplate"
      @uploadSuccess="getList"
      :columns="[
        {
          label: '排程日期',
          prop: 'scheduleDate',
        },
      ]"
      :rules="importRules"
    />
  </basic-container>
</template>
<script>
//lib
import moment from "moment";
import { mapState } from "vuex";
//utils
import { downloadLink } from "@/utils/request";
//interface
import {
  listScheduleResult,
  publishValidate,
  publishScheduleResult,
  validateConstruction,
  removeScheduleResult,
  getSummaryVo,
} from "@/api/xwyy/scheduleResult";
//components
import TltUploadForm from "@/views/components/tltUploadForm.vue";

import autoPlanDialog from "./components/autoPlanDialog.vue";
import addDialog from "./components/addDialog.vue";
import editDialog from "./components/editDialog.vue";
import changeMachineDialog from "./components/changeMachineDialog.vue";
import changePlanDialog from "./components/changePlanDialog.vue";
import releaseStatusDialog from "./components/releaseStatusDialog.vue";
import balanceDialog from "./components/balanceDialog.vue";
import allocateDialog from "./components/allocateDialog.vue";

export default {
  name: "FiberPressSchedule",
  components: {
    allocateDialog,
    autoPlanDialog,
    addDialog,
    editDialog,
    changeMachineDialog,
    changePlanDialog,
    releaseStatusDialog,
    balanceDialog,
    TltUploadForm,
  },
  dicts: ["IS_RELEASE"],
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
      // page: {
      //   current: 1,
      //   pageSize: 20,
      //   total: 0,
      // },
      page: undefined,
      sort: {},
      search: {
      },
      query: {
      },
      importDefaultValue: {
        scheduleDate: moment().add(1, "days").format("YYYY-MM-DD"),
      },
      importRules: {
        scheduleDate: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "blur",
          },
        ],
      },
      stat: {}
    };
  },
  computed: {
    ...mapState({
      machines: (state) => state.fiberPress.machines,
    }),
    columns() {
      let columns = [
        { type: "selection", fixed: "left" },
        {
          label: this.$t("ui.data.column.scheduleResult.baseInfo"),
          children: [
            {
              prop: "isRelease",
              valign: "middle",
              align: "center",
              halign: "center",
              label: this.$t("ui.data.column.scheduleResult.isRelease"),
              formatter: (row, column, value, index) => {
                return this.selectDictLabel(this.dict.type.IS_RELEASE, value);
              },
            },
            {
              prop: "bigRollCode",
              valign: "middle",
              halign: "center",
              align: "center",
              //  sortable: "custom",
              label: this.$t("ui.data.column.xwyy.scheduleResult.bigRollCode"),
              render: ({ row }) => {
                if (row.colorType == "0") {
                  return <font color={row.colorCode}>{row.bigRollCode}</font>;
                }
                return row.bigRollCode;
              },
            },
            {
              prop: "originalLineName",
              valign: "middle",
              align: "center",
              //  sortable: "custom",
              label: this.$t("ui.data.column.xwyy.spec.originalLineName"),
              width: 370,
              render: ({ row }) => {
                let value = row.originalLineName;
                if (row.originalRemindFlag == 1) {
                  return '<font color="#e674ff">' + value + "</font>";
                }
                return value;
              },
            },
            // {
            //   prop: "originalLineQtyNum",
            //   valign: "middle",
            //   halign: "center",
            //   align: "right",
            //   sortable: "custom",
            //   label: this.$t(
            //     "ui.data.column.xwyy.scheduleResult.originalLineQtyNum"
            //   ),
            // },
            // {
            //   prop: "originalBrand",
            //   valign: "middle",
            //   align: "center",
            //   sortable: "custom",
            //   label: this.$t("ui.data.column.bigRollOriginalBrand.brand"),
            // },
            // {
            //   prop: "originalBrandNum",
            //   valign: "middle",
            //   halign: "center",
            //   align: "right",
            //   sortable: "custom",
            //   label: this.$t("ui.data.column.bigRollOriginalBrand.br.brandNum"),
            // },
            {
              prop: "machineId",
              valign: "middle",
              align: "center",
              width: 160,
              //  sortable: "custom",
              label: this.$t("ui.data.column.scheduleResult.produceLine"),
              formatter: (row, column, value, index) => {
                // var actions = [];
                // if ($.common.isEmpty(value)) {
                //   actions.push(
                //     '<a href="javascript:void(0)" onclick="chooseMachine(\'' +
                //       row.id +
                //       "','" +
                //       index +
                //       "')\">" +
                //       this.$t("ui.data.column.selectMachineName") +
                //       "</a> "
                //   );
                //   return actions.join("");
                // }
                // let machineName = selectMachineName(machineNameList, value);
                // if (value.indexOf(",") > 0) {
                //   actions.push(
                //     '<a href="javascript:void(0)" onclick="chooseMachine(\'' +
                //       row.id +
                //       "','" +
                //       index +
                //       "')\">" +
                //       machineName +
                //       "</a> "
                //   );
                //   return actions.join("");
                // }
                return row.machineName;
              },
            },

            {
              prop: "monthPlanOs",
              valign: "middle",
              halign: "center",
              align: "right",
              //  sortable: "custom",
              label: this.$t(
                "ui.data.column.scheduleResult.br.monthPlanOs.meter"
              ),
            },
            {
              prop: "supplyTime",
              valign: "middle",
              halign: "center",
              align: "right",
              //  sortable: "custom",
              label: this.$t(
                "ui.data.column.scheduleResult.br.supplyTime.hour"
              ),
            },
            {
              prop: "yesStock",
              valign: "middle",
              halign: "center",
              align: "right",
              //  sortable: "custom",
              label: this.$t("ui.data.column.scheduleResult.br.yesStock"),
            },
            {
              prop: "todayStock",
              valign: "middle",
              halign: "center",
              align: "right",
              //  sortable: "custom",
              label: this.$t("ui.data.column.scheduleResult.br.todayStock"),
            },
            {
              label: this.$t("ui.data.column.scheduleResult.br.dayUsed"),
              prop: "dayUsed",
            },
            {
              prop: "dailyTotalQty",
              valign: "middle",
              halign: "center",
              align: "right",
              //  sortable: "custom",
              label: this.$t(
                "ui.data.column.scheduleResult.dailyTotalQty.meter"
              ),
            },
            {
              prop: "dailyTotalQtyNum",
              valign: "middle",
              halign: "center",
              align: "right",
              //  sortable: "custom",
              label: this.$t("ui.data.column.scheduleResult.dailyTotalQtyNum"),
            },
          ],
        },
        {
          label: this.$t("ui.data.column.scheduleResult.nightPlan"),
          children: [
            {
              prop: "dayPlanQty",
              valign: "middle",
              halign: "center",
              align: "right",
              //  sortable: "custom",
              label: this.$t("ui.data.column.scheduleResult.plan.meter"),
              // editable: {
              //   type: "text",
              //   label:
              //     this.$t("ui.data.column.scheduleResult.plan") +
              //     "(" +
              //     this.$t("ui.data.column.scheduleResult.unit.meter") +
              //     ")",
              //   validate: function (value) {
              //     var regu = /^[0-9]+?$/;
              //     if (!regu.test(value)) {
              //       layer.msg(
              //         this.$t(
              //           "ui.data.column.scheduleResult.msg.nonNegativeInteger"
              //         )
              //       );
              //       return this.$t(
              //         "ui.data.column.scheduleResult.msg.nonNegativeInteger"
              //       );
              //     }
              //     if (value > 9999999) {
              //       layer.msg(
              //         this.$t("ui.data.column.mdmMonthProdPlan.greatThan")
              //       );
              //       return this.$t("ui.data.column.mdmMonthProdPlan.greatThan");
              //     }
              //   },
              // },
            },
            {
              prop: "dayPlanQtyNum",
              valign: "middle",
              halign: "center",
              align: "right",
              //  sortable: "custom",
              label: this.$t("ui.data.column.scheduleResult.plan.num"),
              // editable: {
              //   type: "text",
              //   label: this.$t("ui.data.column.scheduleResult.plan.num"),
              //   validate: function (value) {
              //     // 正则匹配小数点前可七位，小数点后可一位
              //     var regu = /^([0-9]{1,7})(\.[0-9]{1})?$/;
              //     if (!regu.test(value)) {
              //       layer.msg(this.$t("ui.data.message.planQtyNum"));
              //       return this.$t("ui.data.message.planQtyNum");
              //     }
              //     if (value > 9999999) {
              //       layer.msg(
              //         this.$t("ui.data.column.mdmMonthProdPlan.greatThan")
              //       );
              //       return this.$t("ui.data.column.mdmMonthProdPlan.greatThan");
              //     }
              //   },
              // },
            },
            {
              prop: "dayFinishQty",
              valign: "middle",
              halign: "center",
              align: "right",
              //  sortable: "custom",
              label: this.$t("ui.data.column.scheduleResult.finish.meter"),
            },
            {
              prop: "dayHandAnalysis",
              valign: "middle",
              //  sortable: "custom",
              label: this.$t("ui.data.column.scheduleResult.analysis"),
              formatter: function (row, column, value, index) {
                var reasion = "";
                var HandAnaly = row.daySysAnalysis;
                if (value != null) {
                  reasion = reasion + value;
                }
                if (HandAnaly != null) {
                  if (reasion != "") {
                    reasion = reasion + "," + HandAnaly;
                  } else {
                    reasion = HandAnaly;
                  }
                }
                return reasion;
              },
              // editable: {
              //   type: "text",
              //   label: this.$t("ui.data.column.scheduleResult.analysis"),
              //   validate: function (value) {
              //     if (value.length > 100) {
              //       layer.msg(
              //         this.$t(
              //           "ui.data.column.scheduleResult.analysis.greatThan"
              //         )
              //       );
              //       return this.$t(
              //         "ui.data.column.scheduleResult.analysis.greatThan"
              //       );
              //     }
              //   },
              // },
            },
          ],
        },

        {
          label: this.$t("ui.data.column.scheduleResult.dayPlan"),
          children: [
            {
              prop: "nightPlanQty",
              valign: "middle",
              halign: "center",
              align: "right",
              //  sortable: "custom",
              label: this.$t("ui.data.column.scheduleResult.plan.meter"),
              // editable: {
              //   type: "text",
              //   label:
              //     this.$t("ui.data.column.scheduleResult.plan") +
              //     "(" +
              //     this.$t("ui.data.column.scheduleResult.unit.meter") +
              //     ")",
              //   validate: function (value) {
              //     var regu = /^[0-9]+?$/;
              //     if (!regu.test(value)) {
              //       layer.msg(
              //         this.$t(
              //           "ui.data.column.scheduleResult.msg.nonNegativeInteger"
              //         )
              //       );
              //       return this.$t(
              //         "ui.data.column.scheduleResult.msg.nonNegativeInteger"
              //       );
              //     }
              //     if (value > 9999999) {
              //       layer.msg(
              //         this.$t("ui.data.column.mdmMonthProdPlan.greatThan")
              //       );
              //       return this.$t("ui.data.column.mdmMonthProdPlan.greatThan");
              //     }
              //   },
              // },
              cellStyle: function (value, row, index) {
                if (row.changeNightPlan == 1) {
                  return { css: { background: "#ef6776" } };
                } else if (row.extraPlanFlag == 2) {
                  return { css: { background: "#66ff66" } };
                }
                return {};
              },
            },
            {
              prop: "nightPlanQtyNum",
              valign: "middle",
              halign: "center",
              align: "right",
              //  sortable: "custom",
              label: this.$t("ui.data.column.scheduleResult.plan.num"),
              // editable: {
              //   type: "text",
              //   label: this.$t("ui.data.column.scheduleResult.plan.num"),
              //   validate: function (value) {
              //     // 正则匹配小数点前可七位，小数点后可一位
              //     var regu = /^([0-9]{1,7})(\.[0-9]{1})?$/;
              //     if (!regu.test(value)) {
              //       layer.msg(this.$t("ui.data.message.planQtyNum"));
              //       return this.$t("ui.data.message.planQtyNum");
              //     }
              //     if (value > 9999999) {
              //       layer.msg(
              //         this.$t("ui.data.column.mdmMonthProdPlan.greatThan")
              //       );
              //       return this.$t("ui.data.column.mdmMonthProdPlan.greatThan");
              //     }
              //   },
              // },
            },
            {
              prop: "nightFinishQty",
              valign: "middle",
              halign: "center",
              align: "right",
              //  sortable: "custom",
              label: this.$t("ui.data.column.scheduleResult.finish.meter"),
            },
            {
              prop: "nightHandAnalysis",
              valign: "middle",
              //  sortable: "custom",
              label: this.$t("ui.data.column.scheduleResult.analysis"),
              formatter: function (row, column, value, index) {
                var reasion = "";
                var HandAnaly = row.nightSysAnalysis;
                if (value != null) {
                  reasion = reasion + value;
                }
                if (HandAnaly != null) {
                  if (reasion != "") {
                    reasion = reasion + "," + HandAnaly;
                  } else {
                    reasion = HandAnaly;
                  }
                }
                return reasion;
              },
              // editable: {
              //   type: "text",
              //   label: this.$t("ui.data.column.scheduleResult.analysis"),
              //   validate: function (value) {
              //     if (value.length > 100) {
              //       layer.msg(
              //         this.$t(
              //           "ui.data.column.scheduleResult.analysis.greatThan"
              //         )
              //       );
              //       return this.$t(
              //         "ui.data.column.scheduleResult.analysis.greatThan"
              //       );
              //     }
              //   },
              // },
            },
          ],
        },

        {
          label: this.$t("中班计划（14:00-22:00)"),
          children: [
            {
              prop: "nightPlanQty",
              valign: "middle",
              halign: "center",
              align: "right",
              //  sortable: "custom",
              label: this.$t("ui.data.column.scheduleResult.plan.meter"),
              // editable: {
              //   type: "text",
              //   label:
              //     this.$t("ui.data.column.scheduleResult.plan") +
              //     "(" +
              //     this.$t("ui.data.column.scheduleResult.unit.meter") +
              //     ")",
              //   validate: function (value) {
              //     var regu = /^[0-9]+?$/;
              //     if (!regu.test(value)) {
              //       layer.msg(
              //         this.$t(
              //           "ui.data.column.scheduleResult.msg.nonNegativeInteger"
              //         )
              //       );
              //       return this.$t(
              //         "ui.data.column.scheduleResult.msg.nonNegativeInteger"
              //       );
              //     }
              //     if (value > 9999999) {
              //       layer.msg(
              //         this.$t("ui.data.column.mdmMonthProdPlan.greatThan")
              //       );
              //       return this.$t("ui.data.column.mdmMonthProdPlan.greatThan");
              //     }
              //   },
              // },
            },
            {
              prop: "nightFinishQty",
              valign: "middle",
              halign: "center",
              align: "right",
              //  sortable: "custom",
              label: this.$t("ui.data.column.scheduleResult.finish.meter"),
            },
            {
              prop: "nightProduceOrder",
              valign: "middle",
              halign: "center",
              align: "right",
              //  sortable: "custom",
              label: this.$t("ui.data.column.scheduleResult.produceOrder"),
              // editable: {
              //   type: "text",
              //   label: this.$t("ui.data.column.scheduleResult.produceOrder"),
              //   validate: function (value) {
              //     var regu = /^[0-9]+?$/;
              //     if (!regu.test(value)) {
              //       layer.msg(
              //         this.$t(
              //           "ui.data.column.scheduleResult.msg.nonNegativeInteger"
              //         )
              //       );
              //       return this.$t(
              //         "ui.data.column.scheduleResult.msg.nonNegativeInteger"
              //       );
              //     }
              //     if (value > 999999) {
              //       var str = this.$t(
              //         "ui.data.column.mdmMonthProdPlan.greatThan"
              //       );
              //       layer.msg(String(str).substring(0, str.length - 1));
              //       return String(str).substring(0, str.length - 1);
              //     }
              //   },
              // },
            },
            {
              prop: "nightFinishRate",
              valign: "middle",
              halign: "center",
              align: "right",
              //  sortable: "custom",
              label: this.$t("ui.data.column.scheduleResult.finishRate"),
              formatter: function (row, column, value, index) {
                if (value == 0 || value == null) {
                  return "0%";
                }
                var str = Number(value * 100).toFixed(2);
                return (str += "%");
              },
            },
            {
              prop: "nightSysAnalysis",
              valign: "middle",
              halign: "center",
              align: "left",
              //  sortable: "custom",
              label: this.$t("ui.data.column.scheduleResult.analysis"),
              formatter: (row, column, value, index) => {
                var reasion = "";
                var HandAnaly = row.nightHandAnalysis;
                if (value != null) {
                  reasion = reasion + value;
                }
                if (HandAnaly != null) {
                  if (reasion != "") {
                    reasion = reasion + "," + HandAnaly;
                  } else {
                    reasion = HandAnaly;
                  }
                }
                return reasion;
              },
            },
          ],
        },

        {
          label: this.$t("ui.data.column.scheduleResult.xwyyPlan2"),
          children: [
            {
              prop: "cxClass2Plan",
              valign: "middle",
              halign: "center",
              align: "right",
              //  sortable: "custom",
              label: this.$t(
                "ui.data.column.scheduleResult.br.class1Plan.meter"
              ),
            },
            {
              prop: "cxClass3Plan",
              valign: "middle",
              halign: "center",
              align: "right",
              //  sortable: "custom",
              label: this.$t(
                "ui.data.column.scheduleResult.br.class2Plan.meter"
              ),
            },
            {
              prop: "cxClass5Plan",
              valign: "middle",
              halign: "center",
              align: "right",
              //  sortable: "custom",
              label: this.$t(
                "次日中班计划量(米)"
              ),
            },
            {
              prop: "cxClass4Plan",
              valign: "middle",
              halign: "center",
              align: "right",
              //  sortable: "custom",
              label: this.$t(
                "ui.data.column.scheduleResult.br.class3Plan.meter"
              ),
            },
            {
              prop: "cxClass5Plan",
              valign: "middle",
              halign: "center",
              align: "right",
              //  sortable: "custom",
              label: this.$t(
                "ui.data.column.scheduleResult.br.cxClass4Plan.meter"
              ),
            },
            {
              prop: "cxClass5Plan",
              valign: "middle",
              halign: "center",
              align: "right",
              //  sortable: "custom",
              label: this.$t(
                "次日中班计划量(米)"
              ),
            },
            // {
            //   prop: "cxClass5Plan",
            //   valign: "middle",
            //   halign: "center",
            //   align: "right",
            //   sortable: "custom",
            //   label: this.$t(
            //     "ui.data.column.scheduleResult.br.cxClass5Plan.meter"
            //   ),
            // },
          ],
        },
        {
          // label: this.$t("ui.biz.user.other.info"),
          label: this.$t("其他信息"),
          children: [
            {
              prop: "remark",
              valign: "middle",
              halign: "center",
              align: "center",
              minWidth: 100,
              //  sortable: "custom",
              label: this.$t("ui.common.column.remark"),
              // formatter: (row, column, value, index) => {
              //   return $.table.tooltip(value);
              // },
            },
          ],
        },
      ];

      return columns;
    },
    searchColumns() {
      return [
        {
          label: this.$t("ui.data.column.scheduleResult.scheduleDate"),
          prop: "scheduleDate",
          type: "date",
          valueFormat: "yyyy-MM-dd",
        },
        {
          label: this.$t("ui.data.column.xwyy.scheduleResult.bigRollCode"),
          prop: "bigRollCode",
        },
        {
          label: this.$t("ui.data.column.xwyy.spec.originalLineName"),
          prop: "originalLineName",
        },
        {
          label: this.$t("ui.data.column.scheduleResult.isRelease"),
          prop: "isRelease",
          type: "select",
          dictData: this.dict.type.IS_RELEASE, // "IS_RELEASE",
        },
        {
          label: this.$t("ui.data.column.scheduleResult.produceLine"),
          prop: "machineId",
          type: "select",
          dictData: this.machines,
          valueKey: "id",
          labelKey: "machineName",
        },
      ];
    },
  },
  methods: {
    handleAdd() {
      if (this.$refs.addRef) {
        this.$refs.addRef.show();
      }
    },
    handleEdit(row) {
      if (this.$refs.editRef) {
        this.$refs.editRef.show(row);
      }
    },
    handleDelete(row) {
      this.$confirm(this.$t("common.confirm.delete"), {
        type: "warning",
      }).then(() => {
        // let ids = this.selection.map((row) => row.id).join(",");
        // ids = ids + "|" + this.query.scheduleDate;

        removeScheduleResult({
          removeList: JSON.stringify(this.selection),
        }).then((data) => {
          this.$modal.msgSuccess(data.msg);
          // this.$set(this.page, "current", 1);
          this.getList();
        });
      });
    },
    // 转机台弹窗
    handleChangeMachine() {
      if (this.$refs.changeMachineRef) {
        let row = this.selection;
        this.$refs.changeMachineRef.show(row);
      }
    },

    // 调量
    handleChangePlan() {
      if (this.$refs.editRef) {
        let row = this.selection[0];
        this.$refs.changePlanRef.show(row);
      }
    },
    handleBalance() {
      if (this.$refs.balanceRef) {
        let row = this.selection[0];
        this.$refs.balanceRef.show(row);
      }
    },
    handleCombinationMiddleAndNight() {
      if (this.$refs.allocRef) {
        let ids = this.selection.map((row) => row.id).join(",");
        this.$refs.allocRef.show(ids);
      }
    },

    async handlePublish() {
      this.$confirm(this.$t("ui.biz.alter.makeSurePublish")).then(() => {
        this.publishSchedule();
      });
    },

    handleSearch(data) {
      this.query = data;
      // this.$set(this.page, "current", 1);
      this.getList();
    },
    handlePageChange(current, pageSize) {
      // this.$set(this.page, "current", current);
      // this.$set(this.page, "pageSize", pageSize);
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
    handleAutoPlan() {
      console.log("handleAutoPlan");
      if (this.$refs.autoPlanRef) {
        this.$refs.autoPlanRef.show("", "1");
      }
    },

    handleExportUiExcel() {
      downloadLink("/xwyy/scheduleResult/export", this.formatParams(false));
    },

    handleChangeReleaseStatus() {
      this.$refs.releaseStatusRef.show();
    },
    handleValidateConstruction() {
      this.$confirm(
        this.$t("ui.data.column.cxScheduleResult.validateConstruction")
      ).then(async () => {
        try {
          this.loading = true;
          const ids = this.selection.map((row) => row.id).join(",");
          const res = await validateConstruction({ ids });
          this.$modal.msgSuccess(res.msg);
          this.getList();
        } catch (error) {
          console.error(error);
          this.loading = false;
        }
      });
    },
    handleSelectionChange(rows) {
      this.selection = rows;
    },

    // utils
    updateTableHeaderTitle() {
      //  TODO 更新表头标题
    },
    rowStyle({ row }) {
      //标记收尾背景色（对应成型的收尾）
      if (row.closeOutSpecFlag == "0") {
        return { "background-color": "#C1CFE9" };
      }
      //标记收尾背景色
      if (row.markCloseOutTip == "0") {
        return { "background-color": "#FFFFBF" };
      }
      //插单背景色
      if (row.dataSource == "1") {
        return { "background-color": "#BFE0F7" };
      }
      return {};
    },
    cellStyle({ row, column, rowIndex, columnIndex }) {
      if (column.property === "machineId") {
        if (row.changeMachine == "1") {
          return { "background-color": "#ef6776" };
        }
      }
      if (column.property === "bigRollCode") {
        if (row.colorType == "1") {
          return { background: row.colorCode };
        }
        if (row.markCloseOutTip == "0") {
          return { "background-color": "#FFFFBF" };
        }
      }
      if (column.property === "dayPlanQty") {
        if (row.changeDayPlan == 1) {
          return { background: "#ef6776" };
        }
        if (row.extraPlanFlag == 1) {
          return { background: "#66ff66" };
        }
      }
      if (column.property === "nightPlanQty") {
        if (row.changeNightPlan == 1) {
          return { background: "#ef6776" };
        }
        if (row.extraPlanFlag == 1) {
          return { background: "#66ff66" };
        }
      }

      return {};
    },
    formatParams() {
      const params = {
        // pageSize: this.page.pageSize,
        // pageNum: this.page.current,
        ...this.query,
        ...this.sort,
      };

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
        this.getStat();
        const data = await listScheduleResult(this.formatParams());
        console.log(data);
        this.data = data.rows;
        // this.page.total = data.total;
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
    },
    async getStat() {
      try {
        const data = await getSummaryVo(this.formatParams());
        console.log(data);
        this.stat = data;
      } catch (error) {
        console.error(error);
      }
    },
    async publishSchedule() {
      try {
        this.loading = true;
        let ids = this.selection.map((row) => row.id).join(",");
        const res = await publishScheduleResult({
          ids: ids,
          scheduleDate: this.query.scheduleDate,
        });
        this.$modal.msgSuccess(res.msg);
        this.loading = false;
      } catch (error) {
        console.error(error);
        this.loading = false;
      }
    },
  },
  created() {
    //设置默认排程时间
    let date = moment().add(1, "days").format("YYYY-MM-DD");
    // date = "2023-06-01"; //test
    this.query.scheduleDate = date;
    this.search.scheduleDate = date;

    this.$store.dispatch("fiberPress/getMachineList");
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
