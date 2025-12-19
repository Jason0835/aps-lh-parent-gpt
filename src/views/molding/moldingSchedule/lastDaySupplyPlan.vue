
<template>
  <basic-container>
    <page-table
      tableRef="LastDaySupplyPlanMainTable"
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
        <el-button type="warning" @click="handleGenerateSupplyPlan">{{
          $t("ui.data.column.cx.lastDaySupplyPlan.generate")
        }}</el-button>
        <el-button type="warning" @click="handleRegenerateSupplyPlan">{{
          $t("ui.data.column.cx.lastDaySupplyPlan.regenerate")
        }}</el-button>
        <el-button
          type="warning"
          :disabled="selection.length != 1"
          @click="handleAdd"
          >{{ $t("ui.frame.btn.add") }}</el-button
        >
        <el-button
          type="warning"
          :disabled="selection.length != 1"
          @click="handleChangeMachine"
          >{{ $t("ui.data.column.scheduleResult.changeMachine") }}</el-button
        >
        <el-button
          type="primary"
          :disabled="selection.length === 0"
          @click="handleBatchDelete"
          >{{ $t("ui.frame.btn.delete") }}</el-button
        >
      </template>
    </page-table>

    <addDialog ref="addRef" @success="getList" />
    <editDialog ref="editRef" @success="getList" />
    <changeMachineDialog ref="changeMachineRef" @success="getList" />
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
  listLastDaySupplyPlan,
  generateSupplyPlan,
  regenerateSupplyPlan,
} from "@/api/cx/lastDaySupplyPlan";
//components
import TltUploadForm from "@/views/components/tltUploadForm.vue";

import autoPlanDialog from "./components/autoPlanDialog";
import modifyLhMachineQtyDialog from "./components/modifyLhMachineQtyDialog.vue";
import addDialog from "./components/addDialog.vue";
import editDialog from "./components/editDialog.vue";
import changeMachineDialog from "./components/changeMachineDialog.vue";
import changePlanDialog from "./components/changePlanDialog.vue";

export default {
 name: "LastDaySupplyPlan",
  components: {
    autoPlanDialog,
    modifyLhMachineQtyDialog,
    addDialog,
    editDialog,
    changeMachineDialog,
    changePlanDialog,
    TltUploadForm,
  },
  dicts: [
    "TASK_TYPE",
    "IS_RELEASE",
    "PRODUCTION_STATUS",
    "STORAGE_LOCATION",
    "CLASS_SHIFT",
    "ISORNOT",
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
    ...mapState({
      moldingMachines: (state) => state.molding.machines,
      curingMachines: (state) => state.curing.machines,
    }),
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
                return this.selectDictLabel(
                  this.dict.type.TASK_TYPE,
                  row.taskType
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
                  this.dict.type.PRODUCTION_STATUS,
                  row.productionStatus
                );
                return <text-button>{dictLabel}</text-button>;
              },
            },
            {
              prop: "isRelease",
              valign: "middle",
              align: "center",
              halign: "center",
              label: "是否发布",
              formatter: (row, column, value, index) => {
                return this.selectDictLabel(this.dict.type.IS_RELEASE, value);
              },
            },
            {
              prop: "lhMachineCode",
              valign: "middle",
              align: "left",
              halign: "center",
              label: this.$t(
                "ui.data.column.cxScheduleResult.lhMachineCode.br"
              ),
              // formatter: (row, column, value, index) => {
              //   if ($.common.isEmpty(value)) {
              //     return "";
              //   }
              //   return selectMachineName(lhMachineNameList, value);
              // },
            },
            {
              prop: "remark",
              valign: "middle",
              halign: "center",
              label: this.$t("ui.data.column.remark"),
              // formatter: (row, column, value, index) => {
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
              width: 200,
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
              // formatter: (row, column, value, index) => {
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
              // formatter: (row, column, value, index) => {
              //   if ($.common.isEmpty(value)) {
              //     return "";
              //   }
              //   return selectMachineName(cxMachineNameList, value);
              // },
            },
            {
              prop: "workShifts",
              valign: "middle",
              align: "center",
              halign: "center",
              label: this.$t("ui.data.column.cxScheduleResult.workShifts"),
              formatter: (row, column, value, index) => {
                return this.selectDictLabel(this.dict.type.CLASS_SHIFT, value);
              },
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
              // formatter: (row, column, value, index) => {
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
            },
            {
              prop: "storageLocation",
              valign: "middle",
              align: "center",
              halign: "center",
              label: this.$t("ui.data.column.cxScheduleResult.storageLocation"),
              formatter: (row, column, value, index) => {
                return this.selectDictLabel(
                  this.dict.type.STORAGE_LOCATION,
                  value
                );
              },
            },
            {
              prop: "specDesc",
              valign: "middle",
              align: "left",
              halign: "center",
              label: this.$t("ui.data.column.cxScheduleResult.specDesc"),
              // formatter: (row, column, value, index) => {
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
              //     return action.join("");
              //   }
              // },
              width: 50,
            },
            {
              prop: "embryoCode",
              valign: "middle",
              align: "center",
              halign: "center",
              label: this.$t("ui.data.column.cxScheduleResult.embryoCode"),
            },
            {
              prop: "bomDataVersion",
              valign: "middle",
              align: "center",
              halign: "center",
              label: this.$t("ui.data.column.productStatus.bomDataVersion.br"),
              // formatter: (row, column, value, index) => {
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
              // formatter: (row, column, value, index) => {
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
              // formatter: (row, column, value, index) => {
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
              // }
            },
            {
              prop: "monthStock",
              valign: "middle",
              align: "right",
              halign: "center",
              label: this.$t("ui.data.column.cxScheduleResult.br.monthStock"),
              // formatter: (row, column, value, index) => {
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
        // {
        //   label: this.$t("ui.data.column.scheduleResult.class1"),
        //   children: [
        //     {
        //       prop: "class1AvailableLhShift",
        //       valign: "middle",
        //       align: "right",
        //       halign: "center",
        //       label: this.$t(
        //         "ui.data.column.cxScheduleResult.br.classAvailableLhShift"
        //       ),
        //     },
        //     {
        //       prop: "class1PlanQty",
        //       valign: "middle",
        //       align: "right",
        //       halign: "center",
        //       label:
        //         this.$t("ui.data.column.scheduleResult.class11") +
        //
        //         this.$t("ui.data.column.scheduleResult.plan"),
        //       // editable: {
        //       //   type: "text",
        //       //   label: this.$t("ui.data.column.scheduleResult.plan"),
        //       //   validate: function (value) {
        //       //     var regu = /^[0-9]+?$/;
        //       //     if (!regu.test(value)) {
        //       //       layer.msg(
        //       //         this.$t(
        //       //           "ui.data.column.scheduleResult.msg.nonNegativeInteger"
        //       //         )
        //       //       );
        //       //       return this.$t(
        //       //         "ui.data.column.scheduleResult.msg.nonNegativeInteger"
        //       //       );
        //       //     }
        //       //     if (value > 9999999) {
        //       //       layer.msg(
        //       //         this.$t("ui.data.column.mdmMonthProdPlan.greatThan")
        //       //       );
        //       //       return this.$t("ui.data.column.mdmMonthProdPlan.greatThan");
        //       //     }
        //       //   },
        //       // },
        //     },
        //     {
        //       prop: "class1FinishQty",
        //       valign: "middle",
        //       align: "right",
        //       halign: "center",
        //       label:
        //         this.$t("ui.data.column.scheduleResult.class11") +
        //
        //         this.$t("ui.data.column.scheduleResult.finish"),
        //     },
        //     {
        //       prop: "class1AnalysisInput",
        //       valign: "middle",
        //       halign: "center",
        //       label:
        //         this.$t("ui.data.column.scheduleResult.class11") +
        //
        //         this.$t("ui.data.column.scheduleResult.analysis"),
        //       // editable: {
        //       //   type: "text",
        //       //   defaultValue: "",
        //       //   onblur: "cancel",
        //       //   emptytext: "-",
        //       //   label: this.$t("ui.data.column.scheduleResult.analysis"),
        //       //   validate: function (value) {
        //       //     if (value.length > 66) {
        //       //       layer.msg(
        //       //         this.$t("ui.data.column.scheduleResult.analysis.error")
        //       //       );
        //       //       return this.$t(
        //       //         "ui.data.column.scheduleResult.analysis.error"
        //       //       );
        //       //     }
        //       //   },
        //       // },
        //       // formatter: (row, column, value, index) => {
        //       //   var reasion = "";
        //       //   var SysAnalysis = row.class1Analysis;
        //       //   if (value != null) {
        //       //     reasion = reasion + value;
        //       //   }
        //       //   if (SysAnalysis != null) {
        //       //     if (reasion != "") {
        //       //       reasion = reasion + "," + SysAnalysis;
        //       //     } else {
        //       //       reasion = SysAnalysis;
        //       //     }
        //       //   }
        //       //   var _length = 20;
        //       //   var _text = "";
        //       //   var _value = $.common.nullToStr(reasion);
        //       //   if (_value.length > _length) {
        //       //     _text = _value.substr(0, _length) + "...";
        //       //     _value = _value.replace(/\'/g, "&apos;");
        //       //     _value = _value.replace(/\"/g, "&quot;");
        //       //     var actions = [];
        //       //     var content = _text;
        //       //     actions.push(content);
        //       //     return actions.join("");
        //       //   } else {
        //       //     _text = _value;
        //       //     return _text;
        //       //   }
        //       // },
        //       width: 50,
        //     },
        //   ],
        // },
        // {
        //   label: this.$t("ui.data.column.scheduleResult.class2"),
        //   children: [
        //     {
        //       prop: "class2AvailableLhShift",
        //       valign: "middle",
        //       align: "right",
        //       halign: "center",
        //       label: this.$t(
        //         "ui.data.column.cxScheduleResult.br.classAvailableLhShift"
        //       ),
        //     },
        //     {
        //       prop: "class2PlanQty",
        //       valign: "middle",
        //       align: "right",
        //       halign: "center",
        //       label:
        //         this.$t("ui.data.column.scheduleResult.class22") +
        //
        //         this.$t("ui.data.column.scheduleResult.plan"),
        //       // editable: {
        //       //   type: "text",
        //       //   label: this.$t("ui.data.column.scheduleResult.plan"),
        //       //   validate: function (value) {
        //       //     var regu = /^[0-9]+?$/;
        //       //     if (!regu.test(value)) {
        //       //       layer.msg(
        //       //         this.$t(
        //       //           "ui.data.column.scheduleResult.msg.nonNegativeInteger"
        //       //         )
        //       //       );
        //       //       return this.$t(
        //       //         "ui.data.column.scheduleResult.msg.nonNegativeInteger"
        //       //       );
        //       //     }
        //       //     if (value > 9999999) {
        //       //       layer.msg(
        //       //         this.$t("ui.data.column.mdmMonthProdPlan.greatThan")
        //       //       );
        //       //       return this.$t("ui.data.column.mdmMonthProdPlan.greatThan");
        //       //     }
        //       //   },
        //       // },
        //     },
        //     {
        //       prop: "class2FinishQty",
        //       valign: "middle",
        //       align: "right",
        //       halign: "center",
        //       label:
        //         this.$t("ui.data.column.scheduleResult.class22") +
        //
        //         this.$t("ui.data.column.scheduleResult.finish"),
        //     },
        //     {
        //       prop: "class2AnalysisInput",
        //       valign: "middle",
        //       halign: "center",
        //       label:
        //         this.$t("ui.data.column.scheduleResult.class22") +
        //
        //         this.$t("ui.data.column.scheduleResult.analysis"),
        //       // editable: {
        //       //   type: "text",
        //       //   defaultValue: "",
        //       //   onblur: "cancel",
        //       //   emptytext: "-",
        //       //   label: this.$t("ui.data.column.scheduleResult.analysis"),
        //       //   validate: function (value) {
        //       //     if (value.length > 66) {
        //       //       layer.msg(
        //       //         this.$t("ui.data.column.scheduleResult.analysis.error")
        //       //       );
        //       //       return this.$t(
        //       //         "ui.data.column.scheduleResult.analysis.error"
        //       //       );
        //       //     }
        //       //   },
        //       // },
        //       // formatter: (row, column, value, index) => {
        //       //   var reasion = "";
        //       //   var SysAnalysis = row.class2Analysis;
        //       //   if (value != null) {
        //       //     reasion = reasion + value;
        //       //   }
        //       //   if (SysAnalysis != null) {
        //       //     if (reasion != "") {
        //       //       reasion = reasion + "," + SysAnalysis;
        //       //     } else {
        //       //       reasion = SysAnalysis;
        //       //     }
        //       //   }
        //       //   var _length = 20;
        //       //   var _text = "";
        //       //   var _value = $.common.nullToStr(reasion);
        //       //   if (_value.length > _length) {
        //       //     _text = _value.substr(0, _length) + "...";
        //       //     _value = _value.replace(/\'/g, "&apos;");
        //       //     _value = _value.replace(/\"/g, "&quot;");
        //       //     var actions = [];
        //       //     var content = _text;
        //       //     actions.push(content);
        //       //     return actions.join("");
        //       //   } else {
        //       //     _text = _value;
        //       //     return _text;
        //       //   }
        //       // },
        //       width: 50,
        //     },
        //   ],
        // },
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
            },
            {
              prop: "class3FinishQty",
              valign: "middle",
              align: "right",
              halign: "center",
              label:
                this.$t("ui.data.column.scheduleResult.class33") +
                this.$t("ui.data.column.scheduleResult.finish"),
            },
            {
              prop: "class3AnalysisInput",
              valign: "middle",
              halign: "center",
              label:
                this.$t("ui.data.column.scheduleResult.class33") +
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
              // formatter: (row, column, value, index) => {
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
            },
            {
              prop: "class4FinishQty",
              valign: "middle",
              align: "right",
              halign: "center",
              label:
                this.$t("ui.data.column.scheduleResult.class44") +

                this.$t("ui.data.column.scheduleResult.finish"),
            },
            {
              prop: "class4AnalysisInput",
              valign: "middle",
              halign: "center",
              label:
                this.$t("ui.data.column.scheduleResult.class44") +

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
              // formatter: (row, column, value, index) => {
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
            },
            {
              prop: "class5FinishQty",
              valign: "middle",
              align: "right",
              halign: "center",
              label:
                this.$t("ui.data.column.scheduleResult.class55") +

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
              // formatter: (row, column, value, index) => {
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
    searchColumns() {
      return [
        {
          label: this.$t("ui.data.column.scheduleResult.scheduleDate"),
          prop: "scheduleDate",
          type: "date",
          valueFormat: "yyyy-MM-dd",
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
          dictData: this.dict.type.ISORNOT,
        },
        {
          label: this.$t("ui.data.column.cxScheduleResult.cxMachineCode"),
          prop: "cxMachineCode",
          type: "select",
          dictData: this.moldingMachines,
          labelKey: "moldingMachineCode",
          valueKey: "moldingMachineCode",
          filterable: true,
        },
        {
          label: this.$t("ui.data.column.cxScheduleResult.specDimension"),
          prop: "specDimension",
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
    handleGenerateSupplyPlan() {
      this.$confirm(this.$t("ui.biz.alter.createSupplePlanTask")).then(
        async () => {
          try {
            this.loading = true;
            const res = await generateSupplyPlan({
              scheduleDate: this.query.scheduleDate,
            });
            this.$modal.msgSuccess(res.msg);
            this.getList();
          } catch (error) {
            console.error(error);
            this.loading = false;
          }
        }
      );
    },
    handleRegenerateSupplyPlan() {
      this.$confirm(this.$t("ui.biz.alter.regenerateSupplyPlan")).then(
        async () => {
          try {
            this.loading = true;
            const res = await regenerateSupplyPlan({
              scheduleDate: this.query.scheduleDate,
            });
            this.$modal.msgSuccess(res.msg);
            this.getList();
          } catch (error) {
            console.error(error);
            this.loading = false;
          }
        }
      );
    },
    handleBatchDelete() {},

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
    handleSelectionChange(rows) {
      this.selection = rows;
    },

    // utils
    updateTableHeaderTitle() {
      //  TODO 更新表头标题
    },
    rowStyle({ row }) {
      // console.log(row.markCloseOutTip);
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
      if (column.property === "lhMachineCode") {
        if (row.colorForLhMachine > 1) {
          return { background: "#40EE67" };
        }
      }
      if (column.property === "lhMachineQty") {
        if (row.minimumLhMachineComRatioColor) {
          return { background: row.minimumLhMachineComRatioColor };
        }
        // let usedModels = Number(row.lhMachineQty);
        // let minimumLhMachine = 0;
        // let minLhMachines =
        //   Number(row.minimumLhMachineComQty) * 2 * minimumLhMachine;
        // if (usedModels > minLhMachines) {
        //   return { background: "#40EE67" };
        // } else if (usedModels < minLhMachines) {
        //   return { background: "#FF7B7B" };
        // }
      }
      if (column.property === "cxMachineCode") {
        if (row.changeCxMachine == 1) {
          return { background: "#ef6776" };
        }
      }
      if (column.property === "sapCode") {
        if (row.colorForSapCode > 1) {
          return { background: "#40EE67" };
        }
      }
      if (column.property === "storageLocation") {
        let value = row.storageLocation;
        if ((value == "T6" || value == "T2") && row.dataSource != "1") {
          return { "font-weight": "bold" };
        } else if ((value == "T6" || value == "T2") && row.dataSource == "1") {
          return {
            "font-weight": "bold",
            "background-color": "#BFE0F7",
          };
        }
      }
      if (column.property === "specDesc") {
        if (row.colorType == "1") {
          return { background: row.colorCode };
        }
        if (row.markCloseOutTip == "0") {
          return { "background-color": "#FFFFBF" };
        }
      }
      if (column.property === "embryoCode") {
        if (row.colorForEmbryoCode > 1) {
          return { background: "#40EE67" };
        }
      }
      if (column.property === "monthPlanOs") {
        // let monthPlanSurplusTip = 0; //
        // const monthPlanOs = Number(row.monthPlanOs);
        // if (monthPlanOs <= monthPlanSurplusTip) {
        //   return { background: "#FF7B7B" };
        // }
        if (row.monthPlanSurplusTipColor) {
          return { background: row.monthPlanSurplusTipColor };
        }
      }
      if (column.property === "class1PlanQty") {
        if (row.changeClass1Plan == 1) {
          return { background: "#ef6776" };
        }
        if (row.stopClassShift <= 0 && row.scheduleStop === 1) {
          return { background: "#ff0000" };
        }
      }
      if (column.property === "class2PlanQty") {
        if (row.changeClass2Plan == 1) {
          return { background: "#ef6776" };
        }
        if (row.stopClassShift <= 1 && row.scheduleStop === 1) {
          return { background: "#ff0000" };
        }
      }
      if (column.property === "class3PlanQty") {
        if (row.changeClass3Plan == 1) {
          return { background: "#ef6776" };
        }
        if (row.stopClassShift <= 2 && row.scheduleStop === 1) {
          return { background: "#ff0000" };
        }
      }
      if (column.property === "class4PlanQty") {
        if (row.changeClass4Plan == 1) {
          return { background: "#ef6776" };
        }
        if (row.stopClassShift <= 3 && row.scheduleStop === 1) {
          return { background: "#ff0000" };
        }
      }
      if (column.property === "class5PlanQty") {
        if (row.changeClass5Plan == 1) {
          return { background: "#ef6776" };
        }
        if (row.stopClassShift <= 4 && row.scheduleStop === 1) {
          return { background: "#ff0000" };
        }
      }
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
        const data = await listLastDaySupplyPlan(this.formatParams());
        this.data = data.rows;
        // this.page.total = data.total;
      } catch (error) {
        console.error(error);
      } finally {
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
    if (this.moldingMachines.length === 0) {
      this.$store.dispatch("molding/getMachineList");
    }
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
