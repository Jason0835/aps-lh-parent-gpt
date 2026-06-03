
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
      :cell-style="cellStyle"
    >
      <template slot="header">
        <el-button
          type="primary"
          plain
          v-hasPermi="['schedule:glueScheduleResult:autoSchedule']"
          @click="handleAutoPlan"
        >
          {{ $t("schedule.glueScheduleResult.btn.autoSchedule") }}
        </el-button>
        <el-button
          type="primary"
          plain
          v-hasPermi="['schedule:glueScheduleResult:insert']"
          @click="handleAdd"
        >
          {{ $t("schedule.glueScheduleResult.btn.insert") }}
        </el-button>
        <el-button
          type="primary"
          plain
          @click="() => handleEdit(selection[0])"
          v-hasPermi="['schedule:glueScheduleResult:edit']"
        >
          {{ $t("ui.frame.btn.modify") }}
        </el-button>
        <el-button
          type="danger"
          plain
          :disabled="selection.length === 0"
          v-hasPermi="['schedule:glueScheduleResult:remove']"
          @click="handleDelete"
        >
          {{ $t("ui.frame.btn.delete") }}
        </el-button>
        <el-button
          v-hasPermi="['schedule:glueScheduleResult:changeMachine']"
          type="primary"
          plain
          :disabled="selection.length != 1"
          @click="handleChangeMachine"
        >
          {{ $t("schedule.glueScheduleResult.btn.changeMachine") }}
        </el-button>

        <el-button
          v-hasPermi="['schedule:glueScheduleResult:publish']"
          type="primary"
          plain
          :disabled="selection.length === 0"
          @click="handlePublish"
        >
          {{ $t("schedule.glueScheduleResult.btn.publish") }}
        </el-button>

        <!-- <el-button
          v-hasPermi="['schedule:glueScheduleResult:statistics']"
          type="primary"
          plain
          @click="handleStatistics"
        >
          {{ $t("schedule.glueScheduleResult.btn.statistics") }}
        </el-button> -->
        <!-- <el-button
          v-hasPermi="['schedule:glueScheduleResult:glueSpanSend']"
          type="primary"
          @click="handleSendCrossRegional"
        >
          {{ $t("schedule.glueScheduleResult.btn.sendCrossRegional") }}
        </el-button>
        <el-button
          v-hasPermi="['schedule:glueScheduleResult:glueSpanReceive']"
          type="primary"
          @click="handleReceiveCrossRegional"
        >
          {{ $t("schedule.glueScheduleResult.btn.receiveCrossRegional") }}
        </el-button> -->
        <!-- <el-button
          v-hasPermi="['schedule:glueScheduleResult:supplement']"
          type="primary"
          plain
          @click="handleSupplement"
        >
          {{ $t("schedule.glueScheduleResult.supplement") }}
        </el-button> -->
        <el-button
          v-hasPermi="['schedule:glueScheduleResult:import']"
          @click="$refs.tltUploadForm.handleImport(importDefaultValue)"
        >
          {{ $t("ui.frame.btn.import") }}
        </el-button>
        <el-button
          v-hasPermi="['schedule:glueScheduleResult:export']"
          @click="handleExportUiExcel"
        >
          {{ $t("ui.frame.btn.export") }}
        </el-button>
      </template>
    </page-table>
    <!-- <el-button style="display: none" ref="hidePopoverBtnRef"></el-button> -->
    <autoPlanDialog
      ref="autoPlanRef"
      :scheduleMixAreaPermission="scheduleMixAreaPermission"
      @success="getList"
    />
    <addDialog
      ref="addRef"
      :scheduleMixAreaPermission="scheduleMixAreaPermission"
      @success="getList"
    />
    <editDialog ref="editRef" @success="getList" />
    <changeMachineDialog ref="changeMachineRef" @success="getList" />
    <statisticsDialog ref="statRef" />

    <tlt-upload-form
      ref="tltUploadForm"
      title="导入排程结果数据"
      downloadUrl="/schedule/glueScheduleResult/importTemplate"
      uploadUrl="/schedule/glueScheduleResult/importData"
      @uploadSuccess="getList"
      :columns="importColumn"
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
  listGlueScheduleResult,
  removeGlueScheduleResult,
  publishGlueScheduleResult,
  publishValidateGlueScheduleResult,
} from "@/api/schedule/glueScheduleResult";
import { scheduleMixAreaPermission } from "@/api/setting/service";

//components
import TltUploadForm from "@/views/components/tltUploadForm.vue";

import autoPlanDialog from "./components/autoPlanDialog.vue";
import addDialog from "./components/addDialog.vue";
import editDialog from "./components/editDialog.vue";
import changeMachineDialog from "./components/changeMachineDialog.vue";

import statisticsDialog from "./components/statisticsDialog.vue";

export default {
  name: "MixRubberSchedule",
  components: {
    autoPlanDialog,
    addDialog,
    editDialog,
    changeMachineDialog,
    statisticsDialog,
    TltUploadForm,
  },
  dicts: ["MIX_RELEASE_STATUS", "PRODUCT_STAGE", "MIX_AREA"],
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
      search: {},
      query: {},
      importDefaultValue: {
        scheduleDate: moment().add(1, "days").format("YYYY-MM-DD"),
      },
      importRules: {},
      isGetScheduleMixArea: false,
      scheduleMixAreaPermission: [],
    };
  },
  computed: {
    ...mapState({
      machines: (state) => state.mix.machines,
    }),
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
              label: this.$t("schedule.glueScheduleResult.machineName"),
              // formatter: (row, column, value, index) => {
              //   if ($.common.isEmpty(value)) {
              //     return this.$t(
              //       "schedule.glueScheduleResult.defaultMachineName"
              //     );
              //   }
              //   return value;
              // },
            },
            {
              prop: "releaseStatus",
              //  sortable: "custom",
              halign: "center",
              valign: "middle",
              align: "center",
              label: this.$t("schedule.glueScheduleResult.releaseStatus"),
              render: ({ row }) => {
                var val = this.selectDictLabel(
                  this.dict.type.MIX_RELEASE_STATUS,
                  row.releaseStatus
                );
                if (this.isEmpty(row.releaseStatusTip)) {
                  return val;
                }
                return (
                  <el-tooltip
                    effect="dark"
                    content={row.releaseStatusTip}
                    placement="top-start"
                  >
                    {val}{" "}
                  </el-tooltip>
                );
              },
            },
            {
              prop: "glue",
              //  sortable: "custom",
              halign: "center",
              valign: "middle",
              align: "left",
              label: this.$t("schedule.glueScheduleResult.glue"),
              cellStyle: function (value, row, index) {
                if (row.dataSource == "4") {
                  return { css: { background: "#f4cf01" } };
                }
                return {};
              },
            },
            {
              prop: "recipeTypeName",
              //  sortable: "custom",
              halign: "center",
              valign: "middle",
              align: "center",
              label: this.$t("schedule.glueScheduleResult.recipeTypeName"),
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
              label: this.$t("schedule.glueScheduleResult.recipeVersionId"),
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
              label: this.$t("schedule.glueScheduleResult.recipeStage"),
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
              label: this.$t("schedule.glueScheduleResult.stockQty.br"),
            },
            {
              prop: "safeStockQty",
              //  sortable: "custom",
              halign: "center",
              valign: "middle",
              align: "right",
              label: this.$t("schedule.glueScheduleResult.safeStockQty"),
            },
            {
              prop: "formulaWeight",
              //  sortable: "custom",
              halign: "center",
              valign: "middle",
              align: "right",
              label: this.$t("schedule.glueScheduleResult.formulaWeight"),
            },
            {
              prop: "formulaTime",
              //  sortable: "custom",
              halign: "center",
              valign: "middle",
              align: "right",
              label: this.$t("schedule.glueScheduleResult.formulaTime"),
            },
            {
              prop: "totalPlanQty",
              //  sortable: "custom",
              halign: "center",
              valign: "middle",
              align: "right",
              label: this.$t("schedule.glueScheduleResult.totalPlanQty"),
            },
            {
              prop: "totalSurplus",
              //  sortable: "custom",
              halign: "center",
              valign: "middle",
              align: "right",
              label: this.$t("schedule.glueScheduleResult.totalSurplus"),
            },
            {
              prop: "totalFinish",
              //  sortable: "custom",
              halign: "center",
              valign: "middle",
              align: "right",
              label: this.$t("schedule.glueScheduleResult.totalFinish"),
            },
            {
              prop: "remark",
              //  sortable: "custom",
              halign: "center",
              valign: "middle",
              align: "center",
              label: this.$t("ui.common.column.remark"),
              minWidth: 100,
              // editable: {
              //   type: "text",
              //   label: this.$t("ui.common.column.remark"),
              //   emptytext: "-",
              //   type: "textarea",
              //   display: remarkDisplay,
              //   validate: remarkValidate,
              // },
            },
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
                this.$t("schedule.glueScheduleResult.mid") +
                this.$t("schedule.glueScheduleResult.produceOrder"),
              // editable: {
              //   type: "text",
              //   label:
              //     this.$t("schedule.glueScheduleResult.mid") +
              //     this.$t("schedule.glueScheduleResult.produceOrder"),
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
              prop: "预计开始时间",
              valign: "middle",
              halign: "center",
              align: "center",
              label: this.$t("预计开始时间"),
              width: 150,
            },
            {
              prop: "预计完成时间",
              valign: "middle",
              halign: "center",
              align: "center",
              label: this.$t("预计完成时间"),
              width: 150,
            },
            {
              prop: "midPlanQty",
              //  sortable: "custom",
              halign: "center",
              valign: "middle",
              align: "right",
              // cellStyle: backColor2(),
              label:
                this.$t("schedule.glueScheduleResult.mid") +
                this.$t("schedule.glueScheduleResult.plan"),
              formatter: (row, column, value, index) => {
                // 3878 【分厂胶料需求计划】页面修改中、夜、白三班计划量为0时直接放空处理
                if (value === 0) {
                  return null;
                }
                return value;
              },
              // editable: {
              //   type: "text",
              //   label:
              //     this.$t("schedule.glueScheduleResult.mid") +
              //     this.$t("schedule.glueScheduleResult.plan"),
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
                this.$t("schedule.glueScheduleResult.mid") +
                this.$t("schedule.glueScheduleResult.finish"),
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
                this.$t("schedule.glueScheduleResult.mid") +
                this.$t("schedule.glueScheduleResult.rate"),
            },
            // {
            //   prop: "midExpectFinishTime",
            //   sortable: "custom",
            //   halign: "center",
            //   valign: "middle",
            //   align: "center",
            //   label:
            //     this.$t("schedule.glueScheduleResult.mid") +
            //     this.$t("schedule.glueScheduleResult.finishTime"),
            //   visible: false,
            // },
            {
              prop: "midRemark",
              //  sortable: "custom",
              halign: "center",
              valign: "middle",
              align: "center",
              label: this.$t("schedule.glueScheduleResult.midRemark"),
              // editable: {
              //   type: "text",
              //   label: this.$t("schedule.glueScheduleResult.midRemark"),
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
                this.$t("schedule.glueScheduleResult.night") +
                this.$t("schedule.glueScheduleResult.produceOrder"),
              // editable: {
              //   type: "text",
              //   label:
              //     this.$t("schedule.glueScheduleResult.night") +
              //     this.$t("schedule.glueScheduleResult.produceOrder"),
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
              prop: "预计开始时间",
              valign: "middle",
              halign: "center",
              align: "center",
              label: this.$t("预计开始时间"),
              width: 150,
            },
            {
              prop: "预计完成时间",
              valign: "middle",
              halign: "center",
              align: "center",
              label: this.$t("预计完成时间"),
              width: 150,
            },
            {
              prop: "nightPlanQty",
              //  sortable: "custom",
              halign: "center",
              valign: "middle",
              align: "right",
              // cellStyle: backColor2(),
              label:
                this.$t("schedule.glueScheduleResult.night") +
                this.$t("schedule.glueScheduleResult.plan"),
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
              //     this.$t("schedule.glueScheduleResult.night") +
              //     this.$t("schedule.glueScheduleResult.plan"),
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
                this.$t("schedule.glueScheduleResult.night") +
                this.$t("schedule.glueScheduleResult.finish"),
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
                this.$t("schedule.glueScheduleResult.night") +
                this.$t("schedule.glueScheduleResult.rate"),
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
            //     this.$t("schedule.glueScheduleResult.night") +
            //     this.$t("schedule.glueScheduleResult.finishTime"),
            //   visible: false,
            // },
            {
              prop: "nightRemark",
              //  sortable: "custom",
              halign: "center",
              valign: "middle",
              align: "center",
              label: this.$t("schedule.glueScheduleResult.nightRemark"),
            },
          ],
        },

        {
          label: this.$t("中班（14:00-22:00)"),
          children: [
            {
              prop: "midProduceOrder",
              //  sortable: "custom",
              halign: "center",
              valign: "middle",
              align: "right",
              // cellStyle: backColor1(),
              label:
                this.$t("中班") +
                this.$t("schedule.glueScheduleResult.produceOrder"),
              // editable: {
              //   type: "text",
              //   label:
              //     this.$t("schedule.glueScheduleResult.mid") +
              //     this.$t("schedule.glueScheduleResult.produceOrder"),
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
              prop: "预计开始时间",
              valign: "middle",
              halign: "center",
              align: "center",
              label: this.$t("预计开始时间"),
              width: 150,
            },
            {
              prop: "预计完成时间",
              valign: "middle",
              halign: "center",
              align: "center",
              label: this.$t("预计完成时间"),
              width: 150,
            },
            {
              prop: "midPlanQty",
              //  sortable: "custom",
              halign: "center",
              valign: "middle",
              align: "right",
              // cellStyle: backColor2(),
              label:
                this.$t("中班") +
                this.$t("schedule.glueScheduleResult.plan"),
              formatter: (row, column, value, index) => {
                // 3878 【分厂胶料需求计划】页面修改中、夜、白三班计划量为0时直接放空处理
                if (value === 0) {
                  return null;
                }
                return value;
              },
              // editable: {
              //   type: "text",
              //   label:
              //     this.$t("schedule.glueScheduleResult.mid") +
              //     this.$t("schedule.glueScheduleResult.plan"),
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
                this.$t("中班") +
                this.$t("schedule.glueScheduleResult.finish"),
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
                this.$t("中班") +
                this.$t("schedule.glueScheduleResult.rate"),
            },
            // {
            //   prop: "midExpectFinishTime",
            //   sortable: "custom",
            //   halign: "center",
            //   valign: "middle",
            //   align: "center",
            //   label:
            //     this.$t("schedule.glueScheduleResult.mid") +
            //     this.$t("schedule.glueScheduleResult.finishTime"),
            //   visible: false,
            // },
            {
              prop: "midRemark",
              //  sortable: "custom",
              halign: "center",
              valign: "middle",
              align: "center",
              label: this.$t("中班备注"),
              // editable: {
              //   type: "text",
              //   label: this.$t("schedule.glueScheduleResult.midRemark"),
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
    searchColumns() {
      return [
        {
          label: this.$t("schedule.glueScheduleResult.scheduleDate"),
          prop: "scheduleDate",
          type: "date",
          valueFormat: "yyyy-MM-dd",
        },
        {
          label: this.$t("schedule.glueScheduleResult.mixArea"),
          prop: "mixArea",
          type: "select",
          dictData: this.scheduleMixAreaPermission,
          labelKey: "dictLabel",
          valueKey: "dictValue",
        },
        {
          label: this.$t("schedule.glueScheduleResult.glue"),
          prop: "glue",
        },
        {
          label: this.$t("schedule.glueScheduleResult.machineName"),
          prop: "machineCode",
          dictData: this.machines,
          labelKey: "machineName",
          valueKey: "machineCode",
        },
        {
          label: this.$t("schedule.glueScheduleResult.releaseStatus"),
          prop: "releaseStatus",
          type: "select",
          dictData: this.dict.type.MIX_RELEASE_STATUS, // "MIX_RELEASE_STATUS",
        },
        {
          label: "",
          prop: "isFilterNoMachine",
          type: "checkbox",
          content: this.$t("schedule.glueScheduleResult.filterNoMachine"),
        },
      ];
    },

    importColumn() {
      return [
        {
          label: "排程日期",
          prop: "scheduleDate",
          type: "date",
        },
        {
          label: "密炼区",
          prop: "mixArea",
          type: "select",
          type: "select",
          dictData: this.scheduleMixAreaPermission,
          labelKey: "dictLabel",
          valueKey: "dictValue",
        },
        {},
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
    handleDelete() {
      this.$confirm(this.$t("common.confirm.delete"), {
        type: "warning",
      }).then(() => {
        const ids = this.selection.map((row) => row.id).join(",");
        // console.log(ids);
        removeGlueScheduleResult({ ids }).then((data) => {
          this.$modal.msgSuccess(data.msg);
          // this.$set(this.page, "current", 1);
          this.getList();
        });
      });
    },
    // 转机台弹窗
    handleChangeMachine() {
      if (this.$refs.changeMachineRef) {
        let row = this.selection[0];
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
    handleSupplement() {},

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
      this.publishSchedule();
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
        this.$refs.autoPlanRef.show(
          {
            scheduleDate: moment().add(1, "days").format("yyyy-MM-DD"),
            mixArea: this.query.mixArea,
          },
          "1"
        );
      }
    },

    handleExportUiExcel() {
      downloadLink(
        "/schedule/glueScheduleResult/export",
        this.formatParams(false)
      );
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
      return {};
    },
    cellStyle({ row, column, rowIndex, columnIndex }) {
      if (column.property === "midProduceOrder") {
        return { "background-color": "#10d3be", color: "#035149" };
      }
      if (column.property === "nightProduceOrder") {
        return { "background-color": "#10d3be", color: "#035149" };
      }
      if (column.property === "dayProduceOrder") {
        return { "background-color": "#10d3be", color: "#035149" };
      }
      if (column.property === "dayPlanQty") {
        return { "background-color": "#f4cf01", color: "#735c00" };
      }
      if (column.property === "midPlanQty") {
        return { "background-color": "#f4cf01", color: "#735c00" };
      }
      if (column.property === "nightPlanQty") {
        return { "background-color": "#f4cf01", color: "#735c00" };
      }
      if (column.property === "glue") {
        if (row.dataSource == "4") {
          return { "background-color": "#f4cf01" };
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
        const data = await listGlueScheduleResult(this.formatParams());
        console.log(data);
        this.data = data.rows;
        this.$nextTick(() => {
          this.loading = false;
        });
        // this.page.total = data.total;
      } catch (error) {
        console.error(error);
        this.loading = false;
      } finally {
      }
    },

    publishSchedule() {
      this.$confirm(this.$t("ui.biz.alter.makeSurePublish")).then(async () => {
        try {
          this.loading = true;
          let ids = this.selection.map((row) => row.id).join(",");
          const valid = publishValidateGlueScheduleResult({
            scheduleDate: this.query.scheduleDate,
            ids: ids,
          });

          const res = await publishGlueScheduleResult({
            scheduleDate: this.query.scheduleDate,
            ids: ids,
          });
          this.$modal.msgSuccess(res.msg);
          this.loading = false;
        } catch (error) {
          console.error(error);
          this.loading = false;
        }
      });
    },

    async getScheduleMixAreaPermission() {
      try {
        if (this.isGetScheduleMixArea) {
          return;
        }
        this.loading = true;
        const res = await scheduleMixAreaPermission();
        this.scheduleMixAreaPermission = res.map(({ dictLabel, dictValue }) => {
          return {
            dictValue,
            dictLabel,
          };
        });
        if (this.scheduleMixAreaPermission.length) {
          const data = this.scheduleMixAreaPermission[0];
          this.$set(this.search, "mixArea", data.dictValue);
          this.$set(this.importDefaultValue, "mixArea", data.dictValue);
          // this.search.mixArea = data.dictValue;
          this.query.mixArea = data.dictValue;
        }
        this.isGetScheduleMixArea = true;
        this.loading = false;
      } catch (error) {
        console.error(error);
        this.loading = false;
      }
    },

    async init() {
      await this.getScheduleMixAreaPermission();
      await this.getList();
    },
  },
  async created() {
    //设置默认排程时间
    let date = moment().add(1, "days").format("YYYY-MM-DD");
    // date = "2023-06-02"; //test
    this.query.scheduleDate = date;
    this.search.scheduleDate = date;

    // await this.getScheduleMixAreaPermission();

    this.$store.dispatch("mix/getMachineList");
  },
  activated() {
    this.init();
  },
};
</script>
<style lang="scss" scoped>
.more-btn {
  margin: 2px 0;
  width: 100%;
}
</style>
