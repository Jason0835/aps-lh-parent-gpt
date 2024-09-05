
<template>
  <basic-container>
    <page-table
      tableRef="monthProductionPlanTable"
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
        <el-button type="warning" @click="handleAutoPlan">{{
          $t("ui.data.column.cxScheduleResult.cxAutoPlan")
        }}</el-button>
        <el-button type="warning" @click="handleAdd">{{
          $t("ui.data.column.scheduleResult.insertOrder")
        }}</el-button>
        <el-button
          type="warning"
          :disabled="selection.length != 1"
          @click="handleEdit"
          >{{ $t("ui.frame.btn.modify") }}</el-button
        >
        <el-button
          type="primary"
          :disabled="selection.length != 1"
          @click="handleDelete"
          >{{ $t("ui.frame.btn.delete") }}</el-button
        >
        <el-button
          type="primary"
          :disabled="selection.length != 1"
          @click="handleChangeMachine"
          >{{ $t("ui.data.column.scheduleResult.changeMachine") }}</el-button
        >
        <el-button type="primary" plain @click="handleGotoMachineGant">{{
          $t("ui.data.column.scheduleResult.machine.gantt")
        }}</el-button>
        <el-button type="primary" plain @click="handleGotoSpecDescGant">{{
          $t("ui.data.column.scheduleResult.specDesc.gantt")
        }}</el-button>
        <el-button type="primary" @click="handleChangePlan">{{
          $t("ui.data.column.scheduleResult.changePlan")
        }}</el-button>
        <el-button type="primary" @click="handlePublish">{{
          $t("ui.data.column.scheduleResult.publish")
        }}</el-button>
        <el-dropdown>
          <el-button type="primary" style="margin-left: 10px">
            更多按钮<i class="el-icon-arrow-down el-icon--right"></i>
          </el-button>
          <el-dropdown-menu slot="dropdown">
            <el-dropdown-item>
              <el-button
                type="primary"
                class="more-btn"
                @click="handleModelChange"
                >{{ $t("ui.data.column.cxScheduleResult.modelChange") }}
              </el-button>
            </el-dropdown-item>
            <el-dropdown-item>
              <el-button
                type="primary"
                class="more-btn"
                :disabled="selection.length != 1"
                @click="handleModifyMonthQty"
                >{{ $t("ui.data.column.productStatus.modifyQty") }}
              </el-button>
            </el-dropdown-item>
            <el-dropdown-item>
              <el-button
                type="primary"
                class="more-btn"
                @click="handleProductStatus"
              >
                {{ $t("ui.data.column.scheduleResult.productStatus") }}
              </el-button>
            </el-dropdown-item>
            <el-dropdown-item>
              <el-button
                type="primary"
                class="more-btn"
                @click="handleManualClose"
              >
                {{ $t("ui.data.column.scheduleResult.manualClose") }}
              </el-button>
            </el-dropdown-item>
            <el-dropdown-item>
              <el-button
                type="primary"
                class="more-btn"
                @click="handleToFinishList"
              >
                {{ $t("ui.data.column.scheduleResult.finishedList") }}
              </el-button>
            </el-dropdown-item>
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
            <el-dropdown-item>
              <el-button
                type="primary"
                class="more-btn"
                @click="handleProducingIssue"
              >
                {{ $t("ui.data.column.scheduleResult.producingIssue") }}
              </el-button>
            </el-dropdown-item>
            <el-dropdown-item>
              <el-button
                type="primary"
                class="more-btn"
                @click="handleLastDaySupplyPlan"
              >
                {{ $t("ui.data.column.scheduleResult.lastDaySupplyPlan") }}
              </el-button>
            </el-dropdown-item>
            <el-dropdown-item>
              <el-button
                type="primary"
                class="more-btn"
                @click="handleChangeReleaseStatus"
              >
                {{ $t("ui.data.column.scheduleResult.changeReleaseStatus") }}
              </el-button>
            </el-dropdown-item>
            <el-dropdown-item>
              <el-button
                type="primary"
                class="more-btn"
                :disabled="selection.length == 0"
                @click="handleValidateConstruction"
              >
                {{ $t("ui.data.column.scheduleResult.validateConstruction") }}
              </el-button>
            </el-dropdown-item>
          </el-dropdown-menu>
        </el-dropdown>
      </template>
    </page-table>
    <!-- <el-button style="display: none" ref="hidePopoverBtnRef"></el-button> -->
    <autoPlanDialog ref="autoPlanRef" @success="getList" />
    <modifyLhMachineQtyDialog ref="qtyRef" @success="getList" />
    <addDialog ref="addRef" @success="getList" />
    <editDialog ref="editRef" @success="getList" />
    <changeMachineDialog ref="changeMachineRef" @success="getList" />
    <changePlanDialog ref="changePlanRef" @success="getList" />
    <releaseStatusDialog ref="releaseStatusRef" @success="getList" />
    <tlt-upload-form
      ref="tltUploadForm"
      title="导入成型排查结果数据"
      downloadUrl="/cx/cxScheduleResult/importTemplate"
      uploadUrl="/cx/cxScheduleResult/importData"
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
      title="导入成型排查结果数据"
      uploadUrl="/cx/cxScheduleResult/importData2"
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
//utils
import { downloadLink } from "@/utils/request";
//interface
import {
  listCxScheduleResult,
  publishValidate,
  publishCxScheduleResult,
  modifyQty,
  manualClose,
  producingIssue,
  validateConstruction,
} from "@/api/cx/cxScheduleResult";
//components
import TltUploadForm from "@/views/components/tltUploadForm.vue";

import autoPlanDialog from "./components/autoPlanDialog";
import modifyLhMachineQtyDialog from "./components/modifyLhMachineQtyDialog.vue";
import addDialog from "./components/addDialog.vue";
import editDialog from "./components/editDialog.vue";
import changeMachineDialog from "./components/changeMachineDialog.vue";
import changePlanDialog from "./components/changePlanDialog.vue";
import releaseStatusDialog from "./components/releaseStatusDialog.vue";

export default {
  name: "moldingSchedule",
  components: {
    autoPlanDialog,
    modifyLhMachineQtyDialog,
    addDialog,
    editDialog,
    changeMachineDialog,
    changePlanDialog,
    releaseStatusDialog,
    TltUploadForm,
  },
  dicts: ["TASK_TYPE", "IS_RELEASE"],
  provide() {
    return {
      parentDict: this.dict,
    };
  },
  data() {
    return {
      searchColumns: [
        {
          label: this.$t("ui.data.column.scheduleResult.scheduleDate"),
          prop: "scheduleDate",
          type: "date",
          valueFormat: "yyyy-MM-dd",
        },
        {
          label: this.$t("ui.data.column.cxScheduleResult.taskType"),
          prop: "taskType",
          type: "select",
          dictData: [], // "TASK_TYPE",
        },
        {
          label: this.$t("ui.data.column.cxScheduleResult.productionStatus"),
          prop: "productionStatus",
          type: "select",
          dictData: [], // "PRODUCTION_STATUS",
        },
        {
          label: this.$t("ui.data.column.scheduleResult.embryoCode"),
          prop: "embryoCode",
        },
        {
          label: this.$t("ui.data.column.scheduleResult.sapCode"),
          prop: "sapCode",
        },
        {
          label: this.$t("ui.data.column.mdmMonthProdPlan.hasVersion"),
          prop: "hasVersion",
          type: "select",
          dictData: [], // "ISORNOT",
        },
        {
          label: this.$t("ui.data.column.scheduleResult.isRelease"),
          prop: "isRelease",
          type: "select",
          dictData: [], // "IS_RELEASE",
        },
        {
          label: this.$t("ui.data.column.cxScheduleResult.lhMachineCode"),
          prop: "lhMachineCode",
          type: "select",
          dictUrl: "getMachineInfo",
        },
        {
          label: this.$t("ui.data.column.cxScheduleResult.cxMachineCode"),
          prop: "cxMachineCode",
          type: "select",
          dictUrl: "getMachineInfo",
        },
      ],
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
        mainPlanMonth: "",
      },
      query: {
        mainPlanMonth: "",
      },
      importDefaultValue: {},
      importRules: {},
    };
  },
  computed: {
    columns() {
      let columns = [
        { type: "selection", fixed: "left" },
        {
          label: this.$t("ui.data.column.scheduleResult.baseInfo"),
          children: [
            {
              prop: "taskType",
              align: "center",
              label: this.$t("ui.data.column.cxScheduleResult.taskType"),
              render: ({ row }) => {
                return (
                  <dict-tag
                    options={this.dict.type.TASK_TYPE}
                    value={row.taskType}
                  />
                );
              },
            },
            {
              prop: "productionStatus",
              valign: "middle",
              align: "center",
              label: this.$t(
                "ui.data.column.cxScheduleResult.productionStatus"
              ),
              render: ({ row }) => {
                var dictLabel = this.selectDictLabel(
                  // this.dict.type.PRODUCTION_STATUS,
                  [],
                  row.productionStatus
                );
                // console.log(dictLabel, row.productionStatus);
                return <text-button>{dictLabel}</text-button>;
              },
            },
            {
              prop: "isRelease",
              valign: "middle",
              align: "center",
              halign: "center",
              label: "是否发布",
              // formatter: function (value, row, index) {
              //   return $.table.selectDictLabel(isReleaseDatas, value);
              // },
            },
            {
              prop: "lhMachineCode",
              valign: "middle",
              align: "left",
              halign: "center",
              label: this.$t(
                "ui.data.column.cxScheduleResult.lhMachineCode.br"
              ),
              // formatter: function (value, row, index) {
              //   if ($.common.isEmpty(value)) {
              //     return "";
              //   }
              //   return selectMachineName(lhMachineNameList, value);
              // },
              cellStyle: function (value, row, index) {
                if (row.colorForLhMachine > 1) {
                  return { css: { background: "#40EE67" } };
                }
                return {};
              },
            },
            {
              prop: "remark",
              valign: "middle",
              halign: "center",
              label: this.$t("ui.data.column.remark"),
              // formatter: function (value, row, index) {
              //   var remark = "";
              //   var specialRequirements = row.specialRequirements;
              //   if (value != null) {
              //     remark = value;
              //   }
              //   if (specialRequirements != null) {
              //     if (remark != "") {
              //       remark = remark + "," + specialRequirements;
              //     } else {
              //       remark = specialRequirements;
              //     }
              //   }
              //   return $.table.tooltip(remark);
              // },
              width: 50,
            },
            {
              prop: "lhMachineQty",
              valign: "middle",
              align: "right",
              label: this.$t("ui.data.column.cxScheduleResult.lhMachineQty"),
              render: ({ row }) => {
                return (
                  <text-button
                    onClick={() => {
                      this.handleModifyLhMachineQty(row);
                    }}
                  >
                    {row.lhMachineQty}
                  </text-button>
                );
              },
              // formatter: function (value, row, index) {
              //   if ($.common.isEmpty(value)) {
              //     value = 0;
              //   }
              //   var actions = [];
              //   actions.push(
              //     '<a href="javascript:void(0)" onclick="modifyLhMachineQty(\'' +
              //       row.id +
              //       "')\">" +
              //       value +
              //       "</a> "
              //   );
              //   return actions.join("");
              // },
              cellStyle: function (value, row, index) {
                var usedModels = Number(value);
                var minLhMachines =
                  Number(row.minimumLhMachineComQty) * 2 * minimumLhMachine;
                if (usedModels > minLhMachines) {
                  return { css: { background: "#40EE67" } };
                } else if (usedModels < minLhMachines) {
                  return { css: { background: "#FF7B7B" } };
                }
                return {};
              },
            },
            {
              prop: "minimumLhMachineComQty",
              valign: "middle",
              align: "right",
              halign: "center",
              label: this.$t(
                "ui.data.column.cxScheduleResult.br.minimumLhMachineReqQty"
              ),
            },
            {
              prop: "availableMoldQty",
              valign: "middle",
              align: "right",
              halign: "center",
              label: this.$t(
                "ui.data.column.cxScheduleResult.br.availableMoldQty"
              ),
            },
            {
              prop: "specDimension",
              valign: "middle",
              align: "right",
              halign: "center",
              label: this.$t("ui.data.column.cxScheduleResult.specDimension"),
            },
            {
              prop: "cxMachineCode",
              valign: "middle",
              align: "center",
              halign: "center",
              label: this.$t(
                "ui.data.column.cxScheduleResult.cxMachineCode.br"
              ),
              // formatter: function (value, row, index) {
              //   if ($.common.isEmpty(value)) {
              //     return "";
              //   }
              //   return selectMachineName(cxMachineNameList, value);
              // },
              cellStyle: function (value, row, index) {
                if (row.changeCxMachine == 1) {
                  return { css: { background: "#ef6776" } };
                }
                // if (row.scheduleStop == 1) {
                //     return { css: { "background": "#ff0000"} };
                // }
                return {};
              },
            },
            {
              prop: "workShifts",
              valign: "middle",
              align: "center",
              halign: "center",
              label: this.$t("ui.data.column.cxScheduleResult.workShifts"),
              // formatter: function (value, row, index) {
              //   return $.table.selectDictLabel(CLASS_SHIFT, value);
              // },
            },
            {
              prop: "maximumClassQty",
              valign: "middle",
              align: "right",
              halign: "center",
              label: this.$t(
                "ui.data.column.cxScheduleResult.maximumClassQty.br"
              ),
            },
            {
              prop: "sapCode",
              valign: "middle",
              align: "center",
              halign: "center",
              label: this.$t("ui.data.column.cxScheduleResult.sapCode"),
              // formatter: function (value, row, index) {
              //   if ($.common.isEmpty(row.shareMoldInfoStr)) {
              //     return value;
              //   }
              //   return $.table.hoverValue(
              //     row.shareMoldInfoStr,
              //     value,
              //     "#ff0000",
              //     0,
              //     "1"
              //   );
              // },
              cellStyle: function (value, row, index) {
                if (row.colorForSapCode > 1) {
                  return { css: { background: "#40EE67" } };
                }
                return {};
              },
            },
            {
              prop: "storageLocation",
              valign: "middle",
              align: "center",
              halign: "center",
              label: this.$t("ui.data.column.cxScheduleResult.storageLocation"),
              // formatter: function (value, row, index) {
              //   return $.table.selectDictLabel(STORAGE_LOCATION, value);
              // },
              cellStyle: function (value, row, index) {
                if ((value == "T6" || value == "T2") && row.dataSource != "1") {
                  return { css: { "font-weight": "bold" } };
                } else if (
                  (value == "T6" || value == "T2") &&
                  row.dataSource == "1"
                ) {
                  return {
                    css: {
                      "font-weight": "bold",
                      "background-color": "#BFE0F7",
                    },
                  };
                }
                return {};
              },
            },
            {
              prop: "specDesc",
              valign: "middle",
              align: "left",
              halign: "center",
              label: this.$t("ui.data.column.cxScheduleResult.specDesc"),
              // formatter: function (value, row, index) {
              //   if ($.common.isEmpty(value)) {
              //     return value;
              //   }
              //   var colorCode = "#676a6c";
              //   if (row.colorType == "0") {
              //     colorCode = row.colorCode;
              //   }
              //   // return $.table.hoverValue(value,value,colorCode);
              //   var n = 20;
              //   var action = [];
              //   var l = value.length;
              //   if (l < 20) {
              //     return value;
              //   } else {
              //     for (var i = 0; i < l / n; i++) {
              //       var a = value.slice(n * i, n * (i + 1));
              //       action.push(
              //         "<span style='color:" + colorCode + "'>" + a + "</span>"
              //       );
              //     }
              //     return action.join("<br>");
              //   }
              // },
              cellStyle: function (value, row, index) {
                if (row.colorType == "1") {
                  return { css: { background: row.colorCode } };
                }
                if (row.markCloseOutTip == "0") {
                  return { css: { "background-color": "#FFFFBF" } };
                }
                return {};
              },
              width: 50,
            },
            {
              prop: "embryoCode",
              valign: "middle",
              align: "center",
              halign: "center",
              label: this.$t("ui.data.column.cxScheduleResult.embryoCode"),
              cellStyle: function (value, row, index) {
                if (row.colorForEmbryoCode > 1) {
                  return { css: { background: "#40EE67" } };
                }
                return {};
              },
            },
            {
              prop: "bomDataVersion",
              valign: "middle",
              align: "center",
              halign: "center",
              label: this.$t("ui.data.column.productStatus.bomDataVersion.br"),
              // formatter: function (value, row, index) {
              //   if ($.common.isEmpty(value)) {
              //     value = this.$t(
              //       "ui.data.column.productConstruction.noVersion"
              //     );
              //   }
              //   var actions = [];
              //   actions.push(
              //     '<a href="javascript:void(0)" onclick="changeBomDataVersion(\'' +
              //       row.id +
              //       "')\">" +
              //       value +
              //       "</a> "
              //   );
              //   return actions.join("");
              // },
            },
            {
              prop: "noseWidth",
              valign: "middle",
              align: "center",
              halign: "center",
              label: this.$t("ui.data.column.productStatus.noseWidth.br"),
            },
            {
              prop: "totalStock",
              valign: "middle",
              align: "right",
              halign: "center",
              label: this.$t("ui.data.column.cxScheduleResult.br.totalStock"),
              // formatter: function (value, row, index) {
              //   var overNum = Number(row.overTimeStock);
              //   if (overNum > 0) {
              //     return $.table.hoverValue(
              //       this.$t("ui.data.column.cxScheduleResult.extendedStock") +
              //         "：" +
              //         row.overTimeStock,
              //       value,
              //       "#ff0000",
              //       0,
              //       "1"
              //     );
              //   }
              //   return value;
              // },
              width: 50,
            },
            {
              prop: "lhMiddleNightFinishQty",
              valign: "middle",
              align: "right",
              halign: "center",
              label: this.$t(
                "ui.data.column.cxScheduleResult.br.lhMiddleNightFinishQty"
              ),
            },
            {
              prop: "midNightDifferenceQty",
              valign: "middle",
              align: "right",
              halign: "center",
              label: this.$t(
                "ui.data.column.cxScheduleResult.br.midNightDifferenceQty"
              ),
            },
            {
              prop: "class3PlannedQty",
              valign: "middle",
              align: "right",
              halign: "center",
              label: this.$t(
                "ui.data.column.cxScheduleResult.br.dayPlanQty.br"
              ),
            },
            {
              prop: "singleShiftLhQty",
              valign: "middle",
              align: "right",
              halign: "center",
              label: this.$t(
                "ui.data.column.cxScheduleResult.br.singleShiftLhQty"
              ),
            },
            {
              prop: "cxMonthFinishQty",
              valign: "middle",
              align: "right",
              halign: "center",
              label: this.$t(
                "ui.data.column.cxScheduleResult.cxMonthFinishQty"
              ),
            },
            {
              prop: "monthPlan",
              valign: "middle",
              align: "right",
              halign: "center",
              label: this.$t("ui.data.column.scheduleResult.br.monthPlan"),
            },
            {
              prop: "planModifyQty",
              valign: "middle",
              align: "right",
              halign: "center",
              label: this.$t(
                "ui.data.column.cxScheduleResult.br.planModifyQty"
              ),
            },
            {
              prop: "monthPlanOs",
              valign: "middle",
              align: "right",
              halign: "center",
              label: this.$t("ui.data.column.scheduleResult.br.monthPlanOs"),
              // formatter: function (value, row, index) {
              //   if ($.common.isEmpty(row.monthPlanOsHoverStr)) {
              //     return value;
              //   }
              //   return $.table.hoverValue(
              //     row.monthPlanOsHoverStr,
              //     value,
              //     "#ff0000",
              //     0,
              //     "1"
              //   );
              // },
              cellStyle: function (value, row, index) {
                var monthPlanOs = Number(value);
                if (monthPlanOs <= monthPlanSurplusTip) {
                  return { css: { background: "#FF7B7B" } };
                }
                return {};
              },
            },
            {
              prop: "monthStock",
              valign: "middle",
              align: "right",
              halign: "center",
              label: this.$t("ui.data.column.cxScheduleResult.br.monthStock"),
              // formatter: function (value, row, index) {
              //   var overNum = Number(row.overTimeMonthStock);
              //   if (overNum > 0) {
              //     return $.table.hoverValue(
              //       this.$t("ui.data.column.cxScheduleResult.extendedStock") +
              //         "：" +
              //         row.overTimeMonthStock,
              //       value,
              //       "#337ab7",
              //       0,
              //       "1"
              //     );
              //   }
              //   return value;
              // },
              width: 50,
            },
            {
              prop: "rejectQty",
              valign: "middle",
              align: "right",
              halign: "center",
              label: this.$t("ui.data.column.cxScheduleResult.br.rejectQty"),
              visible: false,
            },
            {
              prop: "newestPlanQty",
              valign: "middle",
              align: "right",
              halign: "center",
              label: this.$t(
                "ui.data.column.cxScheduleResult.br.newestPlanQty"
              ),
            },
            {
              prop: "actualOverProduction",
              valign: "middle",
              align: "right",
              halign: "center",
              label: this.$t(
                "ui.data.column.cxScheduleResult.br.actualOverProduction"
              ),
            },
            {
              prop: "expectedOverProduction",
              valign: "middle",
              align: "right",
              halign: "center",
              label: this.$t(
                "ui.data.column.cxScheduleResult.br.expectedOverProduction"
              ),
            },
            {
              prop: "differenceOverProduction",
              valign: "middle",
              align: "right",
              halign: "center",
              label: this.$t(
                "ui.data.column.cxScheduleResult.br.differenceOverProduction"
              ),
            },
          ],
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class1"),
          children: [
            {
              prop: "class1AvailableLhShift",
              valign: "middle",
              align: "right",
              halign: "center",
              label: this.$t(
                "ui.data.column.cxScheduleResult.br.classAvailableLhShift"
              ),
            },
            {
              prop: "class1PlanQty",
              valign: "middle",
              align: "right",
              halign: "center",
              label:
                this.$t("ui.data.column.scheduleResult.class11") +
                "<br>" +
                this.$t("ui.data.column.scheduleResult.plan"),
              // editable: {
              //   type: "text",
              //   label: this.$t("ui.data.column.scheduleResult.plan"),
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
                if (row.changeClass1Plan == 1) {
                  return { css: { background: "#ef6776" } };
                }
                if (row.stopClassShift <= 0 && row.scheduleStop === 1) {
                  return { css: { background: "#ff0000" } };
                }
                return {};
              },
            },
            {
              prop: "class1FinishQty",
              valign: "middle",
              align: "right",
              halign: "center",
              label:
                this.$t("ui.data.column.scheduleResult.class11") +
                "<br>" +
                this.$t("ui.data.column.scheduleResult.finish"),
            },
            {
              prop: "class1AnalysisInput",
              valign: "middle",
              halign: "center",
              label:
                this.$t("ui.data.column.scheduleResult.class11") +
                "<br>" +
                this.$t("ui.data.column.scheduleResult.analysis"),
              // editable: {
              //   type: "text",
              //   defaultValue: "",
              //   onblur: "cancel",
              //   emptytext: "-",
              //   label: this.$t("ui.data.column.scheduleResult.analysis"),
              //   validate: function (value) {
              //     if (value.length > 66) {
              //       layer.msg(
              //         this.$t("ui.data.column.scheduleResult.analysis.error")
              //       );
              //       return this.$t(
              //         "ui.data.column.scheduleResult.analysis.error"
              //       );
              //     }
              //   },
              // },
              // formatter: function (value, row, index) {
              //   var reasion = "";
              //   var SysAnalysis = row.class1Analysis;
              //   if (value != null) {
              //     reasion = reasion + value;
              //   }
              //   if (SysAnalysis != null) {
              //     if (reasion != "") {
              //       reasion = reasion + "," + SysAnalysis;
              //     } else {
              //       reasion = SysAnalysis;
              //     }
              //   }
              //   var _length = 20;
              //   var _text = "";
              //   var _value = $.common.nullToStr(reasion);
              //   if (_value.length > _length) {
              //     _text = _value.substr(0, _length) + "...";
              //     _value = _value.replace(/\'/g, "&apos;");
              //     _value = _value.replace(/\"/g, "&quot;");
              //     var actions = [];
              //     var content = _text;
              //     actions.push(content);
              //     return actions.join("");
              //   } else {
              //     _text = _value;
              //     return _text;
              //   }
              // },
              width: 50,
            },
          ],
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class2"),
          children: [
            {
              prop: "class2AvailableLhShift",
              valign: "middle",
              align: "right",
              halign: "center",
              label: this.$t(
                "ui.data.column.cxScheduleResult.br.classAvailableLhShift"
              ),
            },
            {
              prop: "class2PlanQty",
              valign: "middle",
              align: "right",
              halign: "center",
              label:
                this.$t("ui.data.column.scheduleResult.class22") +
                "<br>" +
                this.$t("ui.data.column.scheduleResult.plan"),
              // editable: {
              //   type: "text",
              //   label: this.$t("ui.data.column.scheduleResult.plan"),
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
                if (row.changeClass2Plan == 1) {
                  return { css: { background: "#ef6776" } };
                }
                if (row.stopClassShift <= 1 && row.scheduleStop === 1) {
                  return { css: { background: "#ff0000" } };
                }
                return {};
              },
            },
            {
              prop: "class2FinishQty",
              valign: "middle",
              align: "right",
              halign: "center",
              label:
                this.$t("ui.data.column.scheduleResult.class22") +
                "<br>" +
                this.$t("ui.data.column.scheduleResult.finish"),
            },
            {
              prop: "class2AnalysisInput",
              valign: "middle",
              halign: "center",
              label:
                this.$t("ui.data.column.scheduleResult.class22") +
                "<br>" +
                this.$t("ui.data.column.scheduleResult.analysis"),
              // editable: {
              //   type: "text",
              //   defaultValue: "",
              //   onblur: "cancel",
              //   emptytext: "-",
              //   label: this.$t("ui.data.column.scheduleResult.analysis"),
              //   validate: function (value) {
              //     if (value.length > 66) {
              //       layer.msg(
              //         this.$t("ui.data.column.scheduleResult.analysis.error")
              //       );
              //       return this.$t(
              //         "ui.data.column.scheduleResult.analysis.error"
              //       );
              //     }
              //   },
              // },
              // formatter: function (value, row, index) {
              //   var reasion = "";
              //   var SysAnalysis = row.class2Analysis;
              //   if (value != null) {
              //     reasion = reasion + value;
              //   }
              //   if (SysAnalysis != null) {
              //     if (reasion != "") {
              //       reasion = reasion + "," + SysAnalysis;
              //     } else {
              //       reasion = SysAnalysis;
              //     }
              //   }
              //   var _length = 20;
              //   var _text = "";
              //   var _value = $.common.nullToStr(reasion);
              //   if (_value.length > _length) {
              //     _text = _value.substr(0, _length) + "...";
              //     _value = _value.replace(/\'/g, "&apos;");
              //     _value = _value.replace(/\"/g, "&quot;");
              //     var actions = [];
              //     var content = _text;
              //     actions.push(content);
              //     return actions.join("");
              //   } else {
              //     _text = _value;
              //     return _text;
              //   }
              // },
              width: 50,
            },
          ],
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class3"),
          children: [
            {
              prop: "class3AvailableLhShift",
              valign: "middle",
              align: "right",
              halign: "center",
              label: this.$t(
                "ui.data.column.cxScheduleResult.br.classAvailableLhShift"
              ),
            },
            {
              prop: "class3PlanQty",
              valign: "middle",
              align: "right",
              halign: "center",
              label:
                this.$t("ui.data.column.scheduleResult.class33") +
                "<br>" +
                this.$t("ui.data.column.scheduleResult.plan"),
              // editable: {
              //   type: "text",
              //   label: this.$t("ui.data.column.scheduleResult.plan"),
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
                if (row.changeClass3Plan == 1) {
                  return { css: { background: "#ef6776" } };
                }
                if (row.stopClassShift <= 2 && row.scheduleStop === 1) {
                  return { css: { background: "#ff0000" } };
                }
                return {};
              },
            },
            {
              prop: "class3FinishQty",
              valign: "middle",
              align: "right",
              halign: "center",
              label:
                this.$t("ui.data.column.scheduleResult.class33") +
                "<br>" +
                this.$t("ui.data.column.scheduleResult.finish"),
            },
            {
              prop: "class3AnalysisInput",
              valign: "middle",
              halign: "center",
              label:
                this.$t("ui.data.column.scheduleResult.class33") +
                "<br>" +
                this.$t("ui.data.column.scheduleResult.analysis"),
              // editable: {
              //   type: "text",
              //   defaultValue: "",
              //   onblur: "cancel",
              //   emptytext: "-",
              //   label: this.$t("ui.data.column.scheduleResult.analysis"),
              //   validate: function (value) {
              //     if (value.length > 66) {
              //       layer.msg(
              //         this.$t("ui.data.column.scheduleResult.analysis.error")
              //       );
              //       return this.$t(
              //         "ui.data.column.scheduleResult.analysis.error"
              //       );
              //     }
              //   },
              // },
              // formatter: function (value, row, index) {
              //   var reasion = "";
              //   var SysAnalysis = row.class3Analysis;
              //   if (value != null) {
              //     reasion = reasion + value;
              //   }
              //   if (SysAnalysis != null) {
              //     if (reasion != "") {
              //       reasion = reasion + "," + SysAnalysis;
              //     } else {
              //       reasion = SysAnalysis;
              //     }
              //   }
              //   var _length = 20;
              //   var _text = "";
              //   var _value = $.common.nullToStr(reasion);
              //   if (_value.length > _length) {
              //     _text = _value.substr(0, _length) + "...";
              //     _value = _value.replace(/\'/g, "&apos;");
              //     _value = _value.replace(/\"/g, "&quot;");
              //     var actions = [];
              //     var content = _text;
              //     actions.push(content);
              //     return actions.join("");
              //   } else {
              //     _text = _value;
              //     return _text;
              //   }
              // },
              width: 50,
            },
          ],
        },

        {
          label: this.$t("ui.data.column.scheduleResult.class4"),
          children: [
            {
              prop: "class4AvailableLhShift",
              valign: "middle",
              align: "right",
              halign: "center",
              label: this.$t(
                "ui.data.column.cxScheduleResult.br.classAvailableLhShift"
              ),
            },
            {
              prop: "class4PlanQty",
              valign: "middle",
              align: "right",
              halign: "center",
              label:
                this.$t("ui.data.column.scheduleResult.class44") +
                "<br>" +
                this.$t("ui.data.column.scheduleResult.plan"),
              // editable: {
              //   type: "text",
              //   label: this.$t("ui.data.column.scheduleResult.plan"),
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
                if (row.changeClass4Plan == 1) {
                  return { css: { background: "#ef6776" } };
                }
                if (row.stopClassShift <= 3 && row.scheduleStop === 1) {
                  return { css: { background: "#ff0000" } };
                }
                return {};
              },
            },
            {
              prop: "class4FinishQty",
              valign: "middle",
              align: "right",
              halign: "center",
              label:
                this.$t("ui.data.column.scheduleResult.class44") +
                "<br>" +
                this.$t("ui.data.column.scheduleResult.finish"),
            },
            {
              prop: "class4AnalysisInput",
              valign: "middle",
              halign: "center",
              label:
                this.$t("ui.data.column.scheduleResult.class44") +
                "<br>" +
                this.$t("ui.data.column.scheduleResult.analysis"),
              // editable: {
              //   type: "text",
              //   defaultValue: "",
              //   onblur: "cancel",
              //   emptytext: "-",
              //   label: this.$t("ui.data.column.scheduleResult.analysis"),
              //   validate: function (value) {
              //     if (value.length > 66) {
              //       layer.msg(
              //         this.$t("ui.data.column.scheduleResult.analysis.error")
              //       );
              //       return this.$t(
              //         "ui.data.column.scheduleResult.analysis.error"
              //       );
              //     }
              //   },
              // },
              // formatter: function (value, row, index) {
              //   var reasion = "";
              //   var SysAnalysis = row.class4Analysis;
              //   if (value != null) {
              //     reasion = reasion + value;
              //   }
              //   if (SysAnalysis != null) {
              //     if (reasion != "") {
              //       reasion = reasion + "," + SysAnalysis;
              //     } else {
              //       reasion = SysAnalysis;
              //     }
              //   }
              //   var _length = 20;
              //   var _text = "";
              //   var _value = $.common.nullToStr(reasion);
              //   if (_value.length > _length) {
              //     _text = _value.substr(0, _length) + "...";
              //     _value = _value.replace(/\'/g, "&apos;");
              //     _value = _value.replace(/\"/g, "&quot;");
              //     var actions = [];
              //     var content = _text;
              //     actions.push(content);
              //     return actions.join("");
              //   } else {
              //     _text = _value;
              //     return _text;
              //   }
              // },
              width: 50,
            },
          ],
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class5"),
          children: [
            {
              prop: "class5AvailableLhShift",
              valign: "middle",
              align: "right",
              halign: "center",
              label: this.$t(
                "ui.data.column.cxScheduleResult.br.classAvailableLhShift"
              ),
            },
            {
              prop: "class5PlanQty",
              valign: "middle",
              align: "right",
              halign: "center",
              label:
                this.$t("ui.data.column.scheduleResult.class55") +
                "<br>" +
                this.$t("ui.data.column.scheduleResult.plan"),
              // editable: {
              //   type: "text",
              //   label: this.$t("ui.data.column.scheduleResult.plan"),
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
                if (row.changeClass5Plan == 1) {
                  return { css: { background: "#ef6776" } };
                }
                if (row.stopClassShift <= 4 && row.scheduleStop === 1) {
                  return { css: { background: "#ff0000" } };
                }
                return {};
              },
            },
            {
              prop: "class5FinishQty",
              valign: "middle",
              align: "right",
              halign: "center",
              label:
                this.$t("ui.data.column.scheduleResult.class55") +
                "<br>" +
                this.$t("ui.data.column.scheduleResult.finish"),
            },
            {
              prop: "class5AnalysisInput",
              valign: "middle",
              halign: "center",
              label:
                this.$t("ui.data.column.scheduleResult.class55") +
                "\n\t" +
                this.$t("ui.data.column.scheduleResult.analysis"),
              // editable: {
              //   type: "text",
              //   defaultValue: "",
              //   onblur: "cancel",
              //   emptytext: "-",
              //   label: this.$t("ui.data.column.scheduleResult.analysis"),
              //   validate: function (value) {
              //     if (value.length > 66) {
              //       layer.msg(
              //         this.$t("ui.data.column.scheduleResult.analysis.error")
              //       );
              //       return this.$t(
              //         "ui.data.column.scheduleResult.analysis.error"
              //       );
              //     }
              //   },
              // },
              // formatter: function (value, row, index) {
              //   var reasion = "";
              //   var SysAnalysis = row.class5Analysis;
              //   if (value != null) {
              //     reasion = reasion + value;
              //   }
              //   if (SysAnalysis != null) {
              //     if (reasion != "") {
              //       reasion = reasion + "," + SysAnalysis;
              //     } else {
              //       reasion = SysAnalysis;
              //     }
              //   }
              //   var _length = 20;
              //   var _text = "";
              //   var _value = $.common.nullToStr(reasion);
              //   if (_value.length > _length) {
              //     _text = _value.substr(0, _length) + "...";
              //     _value = _value.replace(/\'/g, "&apos;");
              //     _value = _value.replace(/\"/g, "&quot;");
              //     var actions = [];
              //     var content = _text;
              //     actions.push(content);
              //     return actions.join("");
              //   } else {
              //     _text = _value;
              //     return _text;
              //   }
              // },
              width: 50,
            },
          ],
        },
      ];

      return columns;
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
        const ids = row.id;
        console.log(ids);
        // removeArea({ ids }).then((data) => {
        //   this.$modal.msgSuccess(data.msg);
        //   this.$set(this.page, "current", 1);
        //   this.getList();
        // });
      });
    },
    // 转机台弹窗
    handleChangeMachine() {
      if (this.$refs.changeMachineRef) {
        let row = this.selection[0];
        this.$refs.changeMachineRef.show(row);
      }
    },

    handleGotoMachineGant() {
      this.$router.push("/curingPlan/machineGantChart");
    },
    handleGotoSpecDescGant() {
      this.$router.push("/curingPlan/specDescGantChart");
    },
    // 调量
    handleChangePlan() {
      if (this.$refs.changePlanRef) {
        let row = this.selection[0];
        this.$refs.changePlanRef.show(row);
      }
    },
    async handlePublish() {
      this.$confirm(this.$t("ui.biz.alter.makeSurePublish")).then(() => {
        this.publishSchedule();
      });
    },

    async handleModifyMonthQty() {
      try {
        let row = this.selection[0];
        const valid = await hasRecordValidate(row);
        if (valid.code == 200) {
          // let params = row.embryoCode+","+row.sapCode+","+row.cxBatchNo+","+row.bomDataVersion;
          //
          // modifyQty(params).then(() => {});
        }
      } catch (error) {
        console.error(error);
      }
    },

    handleQuery() {},
    handleHistoryQuery() {},

    handleSearch(data) {
      this.query = data;
      this.$set(this.page, "current", 1);
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
          orderBy: prop,
          isAsc: order == "ascending",
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
    handleModifyLhMachineQty() {
      if (this.$refs.qtyRef) {
        this.$refs.qtyRef.show();
      }
    },

    handleModelChange() {
      if (this.$refs.autoPlanRef) {
        this.$refs.autoPlanRef.show("", "3");
      }
    },
    handleProductStatus() {
      this.$router.push("/moldingPlanManagement/productStatus");
    },
    handleManualClose() {
      this.$confirm(
        this.$t("ui.data.column.cxScheduleResult.manualClose")
      ).then(async () => {
        const ids = this.selection.map((row) => row.id).join(",");
        const data = await manualClose({ ids });
        this.$modal.msgSuccess(data.msg);
        this.handelSuccess();
      });
    },
    handleToFinishList() {
      this.$router.push("/moldingPlanManagement/finished");
    },
    handleExportUiExcel() {
      downloadLink("/cx/cxScheduleResult/export", {});
    },
    handleProducingIssue() {
      this.$$confirm(this.$t("ui.biz.alter.producingIssue")).then(async () => {
        try {
          this.loading = false;
          const res = await producingIssue({
            cxMachineCode: cxMachineCode,
            embryoCode: embryoCode,
            taskType: taskType,
            scheduleDate: scheduleDate,
          });
          this.$modal.msgSuccess(res.msg);
          this.getList();
        } catch (error) {
          console.error(error);
          this.loading = false;
        }
      });
    },
    handleLastDaySupplyPlan() {
      this.$router.push("/moldingPlanManagement/lastDaySupplyPlan");
    },

    handleChangeReleaseStatus() {
      this.$refs.releaseStatusRef.show();
    },
    handleValidateConstruction() {
      this.$$confirm(
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

    formatParams() {
      const params = {
        // pageSize: this.page.pageSize,
        // pageNum: this.page.current,
        ...this.query,
        params: {
          ...this.sort,
        },
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
        const data = await listCxScheduleResult(this.formatParams());
        console.log(data);
        this.data = data.rows;
        // this.page.total = data.total;
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
    },

    async publishSchedule() {
      try {
        this.loading = true;
        const valid = await publishValidate();
        if (valid.msg == "0") {
          this.$confirm(
            this.$t("ui.data.column.scheduleResult.hasNullLhMachineCode")
          ).then(async () => {
            const result = await publishCxScheduleResult();
            this.$emit("success");
            this.hide();
          });
        } else {
          const result = await publishCxScheduleResult();
          this.$emit("success");
          this.hide();
        }
      } catch (error) {
        console.error(error);
      }
    },
  },
  created() {
    //设置默认排程时间
    let date = moment().add(1, "days").format("YYYY-MM-DD");
    date = "2023-06-01"; //test
    this.query.scheduleDate = date;
    this.search.scheduleDate = date;
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
