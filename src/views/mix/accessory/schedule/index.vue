
<template>
  <basic-container>
    <page-table
      tableRef="treadScheduleMainTable"
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
        <el-button
          type="warning"
          v-hasPermi="['schedule:materialScheduleResult:autoPlan']"
          @click="handleAutoPlan"
        >
          {{ $t("schedule.materialScheduleResult.btn.autoSchedule") }}
        </el-button>
        <el-button
          type="warning"
          v-hasPermi="['schedule:materialScheduleResult:add']"
          @click="handleAdd"
        >
          {{ $t("schedule.materialScheduleResult.btn.insert") }}
        </el-button>
        <el-button
          type="warning"
          :disabled="selection.length != 1"
          @click="handleEdit(selection[0])"
          v-hasPermi="['schedule:materialScheduleResult:edit']"
        >
          {{ $t("ui.frame.btn.modify") }}
        </el-button>
        <el-button
          type="primary"
          :disabled="selection.length === 0"
          v-hasPermi="['schedule:materialScheduleResult:remove']"
          @click="handleDelete"
        >
          {{ $t("ui.frame.btn.delete") }}
        </el-button>
        <el-button
          v-hasPermi="['schedule:materialScheduleResult:changeMachine']"
          type="primary"
          :disabled="selection.length === 0"
          @click="handleChangeMachine"
        >
          {{ $t("schedule.materialScheduleResult.btn.changeMachine") }}
        </el-button>
        <el-button
          type="primary"
          v-hasPermi="['schedule:materialScheduleResult:import']"
          @click="$refs.tltUploadForm.handleImport(importDefaultValue)"
        >
          {{ $t("ui.frame.btn.import") }}
        </el-button>
        <el-button
          type="primary"
          v-hasPermi="['schedule:materialScheduleResult:export']"
          @click="handleExportUiExcel"
        >
          {{ $t("ui.frame.btn.export") }}
        </el-button>

        <el-button
          v-hasPermi="['schedule:materialScheduleResult:publish']"
          type="primary"
          @click="handlePublish"
        >
          {{ $t("schedule.materialScheduleResult.btn.publish") }}
        </el-button>

        <el-button
          v-hasPermi="['schedule:materialScheduleResult:statistics']"
          type="primary"
          @click="handleStatistics"
        >
          {{ $t("schedule.materialScheduleResult.btn.statistics") }}
        </el-button>
        <el-button
          v-hasPermi="['schedule:materialScheduleResult:expireWarning']"
          type="primary"
          @click="handleExpireWarning"
        >
          {{ $t("schedule.materialScheduleResult.btn.expireWarning") }}
        </el-button>
        <el-button
          v-hasPermi="['schedule:materialScheduleResult:glueSpanSend']"
          type="primary"
          @click="handleSendCrossRegional"
        >
          {{ $t("schedule.glueScheduleResult.btn.sendCrossRegional") }}
        </el-button>
        <el-button
          v-hasPermi="['schedule:materialScheduleResult:glueSpanReceive']"
          type="primary"
          @click="handleReceiveCrossRegional"
        >
          {{ $t("schedule.glueScheduleResult.btn.receiveCrossRegional") }}
        </el-button>
      </template>
    </page-table>
    <!-- <el-button style="display: none" ref="hidePopoverBtnRef"></el-button> -->
    <autoPlanDialog ref="autoPlanRef" @success="getList" />
    <addDialog ref="addRef" @success="getList" />
    <editDialog ref="editRef" @success="getList" />
    <changeMachineDialog ref="changeMachineRef" @success="getList" />
    <statisticsDialog ref="statRef" />
    <expireWarnDialog ref="expRef" />

    <tlt-upload-form
      ref="tltUploadForm"
      title="导入排程结果数据"
      downloadUrl="/schedule/scheduleResult/importTemplate"
      uploadUrl="/schedule/scheduleResult/importData"
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
import { listScheduleResult } from "@/api/schedule/materialScheduleResult";
//components
import TltUploadForm from "@/views/components/tltUploadForm.vue";

import autoPlanDialog from "./components/autoPlanDialog.vue";
import addDialog from "./components/addDialog.vue";
import editDialog from "./components/editDialog.vue";
import changeMachineDialog from "./components/changeMachineDialog.vue";

import statisticsDialog from "./components/statisticsDialog.vue";
import expireWarnDialog from "./components/expireWarnDialog.vue";

export default {
 name: "MixMaterialSchedule",
  components: {
    autoPlanDialog,
    addDialog,
    editDialog,
    changeMachineDialog,
    statisticsDialog,
    expireWarnDialog,
    TltUploadForm,
  },
  dicts: ["TASK_TYPE"],
  provide() {
    return {
      parentDict: this.dict,
    };
  },
  data() {
    return {
      searchColumns: [
        {
          label: this.$t("schedule.materialScheduleResult.scheduleDate"),
          prop: "scheduleDate",
          type: "date",
          valueFormat: "yyyy-MM-dd",
        },
        {
          label: this.$t("schedule.materialScheduleResult.mixArea"),
          prop: "mixArea",
        },
        {
          label: this.$t("schedule.materialScheduleResult.materialName"),
          prop: "materialName",
        },
        {
          label: this.$t("schedule.materialScheduleResult.machineName"),
          prop: "machineCode",
        },
        {
          label: this.$t("schedule.materialScheduleResult.releaseStatus"),
          prop: "releaseStatus",
          type: "select",
          dictData: [], // "MIX_RELEASE_STATUS",
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
      importRules: {
        scheduleDate: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "blur",
          },
        ],
      },
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
              prop: "machineName",
              halign: "center",
              valign: "middle",
              align: "center",
              label: this.$t("schedule.materialScheduleResult.br.machineName"),
              // formatter: (row, column, value, index) => {
              //   if ($.common.isEmpty(value)) {
              //     return this.$t(
              //       "schedule.materialScheduleResult.defaultMachineName"
              //     );
              //   }
              //   return value;
              // },
            },
            // {
            //   prop: "id",
            //   label: "id",
            //   visible: false,
            // },
            // {
            //   prop: "mixArea",
            //   sortable: "custom",
            //   halign: "center",
            //   valign: "middle",
            //   align: "center",
            //   label: this.$t("schedule.materialScheduleResult.mixArea"),
            //   visible: false,
            // },
            {
              prop: "classShift",
             //  sortable: "custom",
              halign: "center",
              valign: "middle",
              align: "left",
              label: this.$t("schedule.materialScheduleResult.classShift"),
            },
            {
              prop: "releaseStatus",
             //  sortable: "custom",
              halign: "center",
              valign: "middle",
              align: "center",
              label: this.$t(
                "schedule.materialScheduleResult.br.releaseStatus"
              ),
              // formatter: (row, column, value, index) => {
              //   var val = this.selectDictLabel(releaseStatusData, value);
              //   if ($.common.isEmpty(row.releaseStatusTip)) {
              //     return val;
              //   }
              //   return $.table.hoverValue(
              //     row.releaseStatusTip,
              //     val,
              //     null,
              //     0,
              //     "1"
              //   );
              // },
            },
            {
              prop: "materialName",
             //  sortable: "custom",
              halign: "center",
              valign: "middle",
              align: "left",
              label: this.$t("schedule.materialScheduleResult.materialName"),
            },
            {
              prop: "recipeTypeName",
             //  sortable: "custom",
              halign: "center",
              valign: "middle",
              align: "center",
              label: this.$t(
                "schedule.materialScheduleResult.br.recipeTypeName"
              ),
              // formatter: (row, column, value, index) => {
              //   if ($.common.isEmpty(value)) {
              //     value = "-";
              //   }
              //   var actions = [];
              //   actions.push(
              //     '<a href="javascript:void(0)" onclick="changeRecipe(\'' +
              //       row.id +
              //       "')\">" +
              //       value +
              //       "</a> "
              //   );
              //   return actions.join("");
              // },
            },
            {
              prop: "recipeVersionId",
             //  sortable: "custom",
              halign: "center",
              valign: "middle",
              align: "center",
              label: this.$t(
                "schedule.materialScheduleResult.br.recipeVersionId"
              ),
              // formatter: (row, column, value, index) => {
              //   if ($.common.isEmpty(value)) {
              //     value = "-";
              //   }
              //   var actions = [];
              //   actions.push(
              //     '<a href="javascript:void(0)" onclick="changeRecipe(\'' +
              //       row.id +
              //       "')\">" +
              //       value +
              //       "</a> "
              //   );
              //   return actions.join("");
              // },
            },
            {
              prop: "recipeStage",
             //  sortable: "custom",
              halign: "center",
              valign: "middle",
              align: "center",
              label: this.$t("schedule.materialScheduleResult.br.recipeStage"),
              // formatter: (row, column, value, index) => {
              //   if ($.common.isEmpty(value)) {
              //     value = "-";
              //   }
              //   var selectDictLabel = this.selectDictLabel(
              //     recipeStageData,
              //     value
              //   );
              //   var dictLabel = $.common.isEmpty(selectDictLabel)
              //     ? value
              //     : selectDictLabel;
              //   var actions = [];
              //   actions.push(
              //     '<a href="javascript:void(0)" onclick="changeRecipe(\'' +
              //       row.id +
              //       "')\">" +
              //       dictLabel +
              //       "</a> "
              //   );
              //   return actions.join("");
              // },
            },
            {
              prop: "stockQty",
             //  sortable: "custom",
              halign: "center",
              valign: "middle",
              align: "right",
              label: this.$t("schedule.materialScheduleResult.stockQty.br"),
            },
            {
              prop: "safeStockQty",
             //  sortable: "custom",
              halign: "center",
              valign: "middle",
              align: "right",
              label: this.$t("schedule.glueScheduleResult.br.safeStockQty"),
            },
            {
              prop: "demandQty",
             //  sortable: "custom",
              halign: "center",
              valign: "middle",
              align: "right",
              label: this.$t("schedule.materialScheduleResult.demandQty"),
            },
            {
              prop: "demandPlanning",
             //  sortable: "custom",
              halign: "center",
              valign: "middle",
              align: "left",
              label: this.$t(
                "schedule.materialScheduleResult.br.demandPlanning"
              ),
            },
            {
              prop: "totalPlanQty",
             //  sortable: "custom",
              halign: "center",
              valign: "middle",
              align: "right",
              label: this.$t("schedule.materialScheduleResult.totalPlanQty"),
            },
            // {
            //   prop: "totalSurplus",
            //   sortable: "custom",
            //   halign: "center",
            //   valign: "middle",
            //   align: "right",
            //   label: this.$t("schedule.materialScheduleResult.totalSurplus"),
            // },
            // {
            //   prop: "totalFinish",
            //   sortable: "custom",
            //   halign: "center",
            //   valign: "middle",
            //   align: "right",
            //   label: this.$t("schedule.materialScheduleResult.totalFinish"),
            // },
            // {
            //   prop: "remark",
            //   sortable: "custom",
            //   halign: "center",
            //   valign: "middle",
            //   align: "center",
            //   label: this.$t("ui.common.column.remark"),
            //   // editable: {
            //   //   type: "text",
            //   //   label: this.$t("ui.common.column.remark"),
            //   //   emptytext: "-",
            //   //   type: "textarea",
            //   //   display: remarkDisplay,
            //   //   validate: remarkValidate,
            //   // },
            // },
          ],
        },
        {
          label: this.$t("schedule.common.midClass"),
          children: [
            {
              prop: "midProduceOrder",
             //  sortable: "custom",
              halign: "center",
              valign: "middle",
              align: "right",
              // cellStyle: backColor1(),
              label:
                this.$t("schedule.materialScheduleResult.mid") +
                this.$t("schedule.materialScheduleResult.produceOrder"),
              // editable: {
              //   type: "text",
              //   label:
              //     this.$t("schedule.materialScheduleResult.mid") +
              //     this.$t("schedule.materialScheduleResult.produceOrder"),
              //   emptytext: "-",
              //   validate: function (value) {
              //     // 输入非负整数正则校验
              //     var regu = /^\d+$/;
              //     if (!regu.test(value) && $.common.isNotEmpty(value)) {
              //       layer.msg(this.$t("ui.message.editable.digits"));
              //       return this.$t("ui.message.editable.digits");
              //     }
              //     if (value.length > 6) {
              //       let message = String(
              //         this.$t("ui.message.editable.maxSeven")
              //       ).replace("9999999", "999999");
              //       layer.msg(message);
              //       return message;
              //     }
              //   },
              // },
            },
            {
              prop: "midPlanQty",
             //  sortable: "custom",
              halign: "center",
              valign: "middle",
              align: "right",
              // cellStyle: backColor2(),
              label:
                this.$t("schedule.materialScheduleResult.mid") +
                this.$t("schedule.materialScheduleResult.plan"),
              // formatter: (row, column, value, index) => {
              //   // 3878 【分厂胶料需求计划】页面修改中、夜、白三班计划量为0时直接放空处理
              //   if (value === 0) {
              //     return null;
              //   }
              //   return value;
              // },
              // editable: {
              //   type: "text",
              //   label:
              //     this.$t("schedule.materialScheduleResult.mid") +
              //     this.$t("schedule.materialScheduleResult.plan"),
              //   emptytext: "-",
              //   display: function (value, sourceData) {
              //     if (value === 0) {
              //       $(this).html("-");
              //     } else {
              //       $(this).html(value);
              //     }
              //   },
              //   validate: function (value) {
              //     // 输入非负整数正则校验
              //     var regu = /^\d+$/;
              //     if (!regu.test(value) && $.common.isNotEmpty(value)) {
              //       layer.msg(this.$t("ui.message.editable.digits"));
              //       return this.$t("ui.message.editable.digits");
              //     }
              //     if (value.length > 7) {
              //       layer.msg(this.$t("ui.message.editable.maxSeven"));
              //       return this.$t("ui.message.editable.maxSeven");
              //     }
              //   },
              // },
            },
            {
              prop: "midFinishQty",
             //  sortable: "custom",
              halign: "center",
              valign: "middle",
              align: "right",
              label:
                this.$t("schedule.materialScheduleResult.mid") +
                this.$t("schedule.materialScheduleResult.finish"),
              // formatter: (row, column, value, index) => {
              //   // 页面修改中、夜、白三班完成量为0时直接放空处理
              //   if (value === 0) {
              //     return null;
              //   }
              //   return value;
              // },
            },
            {
              prop: "midFinishRate",
             //  sortable: "custom",
              halign: "center",
              valign: "middle",
              align: "right",
              label:
                this.$t("schedule.materialScheduleResult.mid") +
                this.$t("schedule.materialScheduleResult.rate"),
            },
            // {
            //   prop: "midExpectFinishTime",
            //   sortable: "custom",
            //   halign: "center",
            //   valign: "middle",
            //   align: "center",
            //   label:
            //     this.$t("schedule.materialScheduleResult.mid") +
            //     this.$t("schedule.materialScheduleResult.br.finishTime"),
            //   visible: false,
            // },
            {
              prop: "midRemark",
             //  sortable: "custom",
              halign: "center",
              valign: "middle",
              align: "center",
              label: this.$t("schedule.materialScheduleResult.midRemark"),
              // editable: {
              //   type: "text",
              //   label: this.$t("schedule.materialScheduleResult.midRemark"),
              //   emptytext: "-",
              //   type: "textarea",
              //   display: remarkDisplay,
              //   validate: remarkValidate,
              // },
            },
          ],
        },
        {
          label: this.$t("schedule.common.nightClass"),
          children: [
            {
              prop: "nightProduceOrder",
             //  sortable: "custom",
              halign: "center",
              valign: "middle",
              align: "right",
              // cellStyle: backColor1(),
              label:
                this.$t("schedule.materialScheduleResult.night") +
                this.$t("schedule.materialScheduleResult.produceOrder"),
              // editable: {
              //   type: "text",
              //   label:
              //     this.$t("schedule.materialScheduleResult.night") +
              //     this.$t("schedule.materialScheduleResult.produceOrder"),
              //   emptytext: "-",
              //   validate: function (value) {
              //     // 输入非负整数正则校验
              //     var regu = /^\d+$/;
              //     if (!regu.test(value) && $.common.isNotEmpty(value)) {
              //       layer.msg(this.$t("ui.message.editable.digits"));
              //       return this.$t("ui.message.editable.digits");
              //     }
              //     if (value.length > 6) {
              //       let message = String(
              //         this.$t("ui.message.editable.maxSeven")
              //       ).replace("9999999", "999999");
              //       layer.msg(message);
              //       return message;
              //     }
              //   },
              // },
            },
            {
              prop: "nightPlanQty",
             //  sortable: "custom",
              halign: "center",
              valign: "middle",
              align: "right",
              // cellStyle: backColor2(),
              label:
                this.$t("schedule.materialScheduleResult.night") +
                this.$t("schedule.materialScheduleResult.plan"),
              // formatter: (row, column, value, index) => {
              //   // 3878 【分厂胶料需求计划】页面修改中、夜、白三班计划量为0时直接放空处理
              //   if (value === 0) {
              //     return null;
              //   }
              //   return value;
              // },
              // editable: {
              //   type: "text",
              //   label:
              //     this.$t("schedule.materialScheduleResult.night") +
              //     this.$t("schedule.materialScheduleResult.plan"),
              //   emptytext: "-",
              //   display: function (value, sourceData) {
              //     if (value === 0) {
              //       $(this).html("-");
              //     } else {
              //       $(this).html(value);
              //     }
              //   },
              //   validate: function (value) {
              //     // 输入非负整数正则校验
              //     var regu = /^\d+$/;
              //     if (!regu.test(value) && $.common.isNotEmpty(value)) {
              //       layer.msg(this.$t("ui.message.editable.digits"));
              //       return this.$t("ui.message.editable.digits");
              //     }
              //     if (value.length > 7) {
              //       layer.msg(this.$t("ui.message.editable.maxSeven"));
              //       return this.$t("ui.message.editable.maxSeven");
              //     }
              //   },
              // },
            },
            {
              prop: "nightFinishQty",
             //  sortable: "custom",
              halign: "center",
              valign: "middle",
              align: "right",
              label:
                this.$t("schedule.materialScheduleResult.night") +
                this.$t("schedule.materialScheduleResult.finish"),
              // formatter: (row, column, value, index) => {
              //   // 页面修改中、夜、白三班完成量为0时直接放空处理
              //   if (value === 0) {
              //     return null;
              //   }
              //   return value;
              // },
            },
            {
              prop: "nightFinishRate",
             //  sortable: "custom",
              halign: "center",
              valign: "middle",
              align: "right",
              label:
                this.$t("schedule.materialScheduleResult.night") +
                this.$t("schedule.materialScheduleResult.rate"),
              // formatter: (row, column, value, index) => {
              //   if ($.common.isEmpty(value)) {
              //     return "-";
              //   }
              //   var str = Number(value * 100).toFixed(2);
              //   return (str += "%");
              // },
            },
            // {
            //   prop: "nightExpectFinishTime",
            //   sortable: "custom",
            //   halign: "center",
            //   valign: "middle",
            //   align: "center",
            //   label:
            //     this.$t("schedule.materialScheduleResult.night") +
            //     this.$t("schedule.materialScheduleResult.br.finishTime"),
            //   visible: false,
            // },
            {
              prop: "nightRemark",
             //  sortable: "custom",
              halign: "center",
              valign: "middle",
              align: "center",
              label: this.$t("schedule.materialScheduleResult.nightRemark"),
            },
          ],
        },
        {
          label: this.$t("schedule.common.dayClass"),
          children: [
            {
              prop: "dayProduceOrder",
             //  sortable: "custom",
              halign: "center",
              valign: "middle",
              align: "right",
              // cellStyle: backColor1(),
              label:
                this.$t("schedule.materialScheduleResult.day") +
                this.$t("schedule.materialScheduleResult.produceOrder"),
              // editable: {
              //   type: "text",
              //   label:
              //     this.$t("schedule.materialScheduleResult.day") +
              //     this.$t("schedule.materialScheduleResult.produceOrder"),
              //   emptytext: "-",
              //   validate: function (value) {
              //     // 输入非负整数正则校验
              //     var regu = /^\d+$/;
              //     if (!regu.test(value) && $.common.isNotEmpty(value)) {
              //       layer.msg(this.$t("ui.message.editable.digits"));
              //       return this.$t("ui.message.editable.digits");
              //     }
              //     if (value.length > 6) {
              //       let message = String(
              //         this.$t("ui.message.editable.maxSeven")
              //       ).replace("9999999", "999999");
              //       layer.msg(message);
              //       return message;
              //     }
              //   },
              // },
            },
            {
              prop: "dayPlanQty",
             //  sortable: "custom",
              halign: "center",
              valign: "middle",
              align: "right",
              // cellStyle: backColor2(),
              label:
                this.$t("schedule.materialScheduleResult.day") +
                this.$t("schedule.materialScheduleResult.plan"),
              // formatter: (row, column, value, index) => {
              //   // 3878 【分厂胶料需求计划】页面修改中、夜、白三班计划量为0时直接放空处理
              //   if (value === 0) {
              //     return null;
              //   }
              //   return value;
              // },
              // editable: {
              //   type: "text",
              //   label:
              //     this.$t("schedule.materialScheduleResult.day") +
              //     this.$t("schedule.materialScheduleResult.plan"),
              //   emptytext: "-",
              //   display: function (value, sourceData) {
              //     if (value === 0) {
              //       $(this).html("-");
              //     } else {
              //       $(this).html(value);
              //     }
              //   },
              //   validate: function (value) {
              //     // 输入非负整数正则校验
              //     var regu = /^\d+$/;
              //     if (!regu.test(value) && $.common.isNotEmpty(value)) {
              //       layer.msg(this.$t("ui.message.editable.digits"));
              //       return this.$t("ui.message.editable.digits");
              //     }
              //     if (value.length > 7) {
              //       layer.msg(this.$t("ui.message.editable.maxSeven"));
              //       return this.$t("ui.message.editable.maxSeven");
              //     }
              //   },
              // },
            },
            {
              prop: "dayFinishQty",
             //  sortable: "custom",
              halign: "center",
              valign: "middle",
              align: "right",
              label:
                this.$t("schedule.materialScheduleResult.day") +
                this.$t("schedule.materialScheduleResult.finish"),
              // formatter: (row, column, value, index) => {
              //   // 页面修改中、夜、白三班完成量为0时直接放空处理
              //   if (value === 0) {
              //     return null;
              //   }
              //   return value;
              // },
            },
            {
              prop: "dayFinishRate",
             //  sortable: "custom",
              halign: "center",
              valign: "middle",
              align: "right",
              label:
                this.$t("schedule.materialScheduleResult.day") +
                this.$t("schedule.materialScheduleResult.rate"),
              // formatter: (row, column, value, index) => {
              //   if ($.common.isEmpty(value)) {
              //     return "-";
              //   }
              //   var str = Number(value * 100).toFixed(2);
              //   return (str += "%");
              // },
            },

            {
              prop: "dayRemark",
             //  sortable: "custom",
              halign: "center",
              valign: "middle",
              align: "center",
              label: this.$t("schedule.materialScheduleResult.dayRemark"),
              // editable: {
              //   type: "text",
              //   label: this.$t("schedule.materialScheduleResult.dayRemark"),
              //   emptytext: "-",
              //   type: "textarea",
              //   display: remarkDisplay,
              //   validate: remarkValidate,
              // },
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
        // console.log(ids);
        // removeTmScheduleResult({ ids }).then((data) => {
        //   this.$modal.msgSuccess(data.msg);
        //   // this.$set(this.page, "current", 1);
        //   this.getList();
        // });
      });
    },
    // 转机台弹窗
    handleChangeMachine() {
      if (this.$refs.changeMachineRef) {
        let row = this.selection;
        this.$refs.changeMachineRef.show(row);
      }
    },
    handleStatistics() {
      if (this.$refs.statRef) {
        // let row = this.selection[0];
        this.$refs.statRef.show();
      }
    },
    handleSendCrossRegional() {},
    handleReceiveCrossRegional() {},
    handleExpireWarning() {
      if (this.$refs.expRef) {
        this.$refs.expRef.show();
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
      this.publishSchedule();;
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
      downloadLink("/schedule/scheduleResult/export", this.formatParams(false));
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

    publishSchedule() {
      this.$confirm(this.$t("ui.biz.alter.makeSurePublish")).then(async () => {
        try {
          this.loading = true;
          let ids = this.selection.map((row) => row.id).join(",");
          const res = await publishScheduleResult({ ids: ids });
          this.$modal.msgSuccess(res.msg);
          this.loading = false;
        } catch (error) {
          console.error(error);
          this.loading = false;
        }
      });
    },
  },
  created() {
    //设置默认排程时间
    let date = moment().add(1, "days").format("YYYY-MM-DD");
   // date = "2023-06-01"; //test
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
