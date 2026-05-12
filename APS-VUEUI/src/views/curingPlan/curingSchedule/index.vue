
<template>
  <basic-container>
    <page-table
      tableRef="curingScheduleTable"
      :calcHeight="true"
      v-loading="loading"
      :columns="columns"
      :searchColumns="searchColumns"
      :page="page"
      :data="data"
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
          v-hasPermi="['lh:lhScheduleResult:autoLhScheduleResult']"
          type="warning"
          @click="handleAutoPlan"
          >{{ $t("ui.data.column.scheduleResult.autoSchedule") }}</el-button
        >
        <!-- <el-button
          v-hasPermi="['lh:lhScheduleResult:autoLhScheduleResult']"
          type="warning"
          @click="handleAutoPlan"
          >{{ $t("生成模具交替计划") }}</el-button
        > -->
        <el-button
          v-hasPermi="['lh:lhScheduleResult:insertOrder']"
          type="warning"
          :disabled="selection.length != 1"
          @click="handleAdd"
          >{{ $t("ui.data.column.scheduleResult.insertOrder") }}</el-button
        >
        <el-button
          v-hasPermi="['lh:lhScheduleResult:save']"
          type="warning"
          @click="
            () => {
              handleEdit(this.selection[0]);
            }
          "
          :disabled="selection.length != 1"
          >{{ $t("ui.frame.btn.modify") }}</el-button
        >
        <el-button
          v-hasPermi="['lh:lhScheduleResult:remove']"
          type="danger"
          @click="handleDeleteMulti"
          :disabled="selection.length == 0"
          >{{ $t("ui.frame.btn.delete") }}</el-button
        >
        <el-button
          v-hasPermi="['lh:lhScheduleResult:changeMachine']"
          type="primary"
          @click="handleChangeMachine"
          :disabled="selection.length !== 1"
          >{{ $t("ui.data.column.scheduleResult.changeMachine") }}</el-button
        >
        <el-button
          v-hasPermi="['lh:lhScheduleResult:changeMachine']"
          type="primary"
          @click="handleChangePlan"
          :disabled="selection.length !== 1"
          >{{ $t("ui.data.column.scheduleResult.changePlan") }}</el-button
        >

        <el-button
          type="primary"
          :disabled="selection.length === 0"
          @click="handlePublish"
          >{{ $t("ui.data.column.scheduleResult.schedulePublish") }}</el-button
        >
        <!-- <el-button
          v-hasPermi="['lh:lhScheduleResult:changeMachine']"
          type="primary"
          :disabled="selection.length == 0"
          @click="getAdjustTextNo"
          >{{ $t("ui.data.column.scheduleResult.textAdjust") }}</el-button
        > -->
        <el-button
          v-hasPermi="['lh:lhScheduleResult:generateTextPlan']"
          type="primary"
          :disabled="selection.length != 1"
          @click="handleGenerateTextMouldChangePlan"
        >{{ $t("ui.data.btn.lhMouldChangePlan.generateTextPlan") }}</el-button
        >
        <el-button
          v-hasPermi="['lh:lhScheduleResult:increaseMouldStartPlan']"
          type="primary"
          :disabled="selection.length != 1"
          @click="handleIncreaseMouldStartPlan"
        >{{ $t("ui.data.btn.lhScheduleResult.increaseMouldStartPlan") }}</el-button
        >
        <el-button
          v-hasPermi="['lh:lhScheduleResult:unscheduledResult']"
          type="primary"
          @click="handleUnschedule"
          >{{
          $t("ui.data.btn.scheduleResult.unscheduledResult")
        }}</el-button
        >
        <el-button
          v-hasPermi="['monthplan:mouldingDayResult:import']"
          @click="$refs.tltUpload.handleImport()"
          >{{ $t("ui.frame.btn.import") }}</el-button
        >
        <el-button
          @click="handleExport"
          v-hasPermi="['monthplan:mouldingDayResult:export']"
          >{{ $t("ui.frame.btn.export") }}</el-button
        >
        <el-button
          @click="handleExportSummaryReport"
          v-hasPermi="['monthplan:mouldingDayResult:export']"
          >{{ $t("ui.data.column.scheduleResult.exportScheduleSummaryReport") }}</el-button
        >
        <!-- <el-dropdown>
          <el-button type="primary" style="margin-left: 10px">
            更多按钮<i class="el-icon-arrow-down el-icon--right"></i>
          </el-button>
          <el-dropdown-menu slot="dropdown">
            <el-dropdown-item>
              <el-button
                v-hasPermi="['lh:scheduleResult:export']"
                type="primary"
                class="more-btn"
                @click="handleExport"
                >{{ $t("ui.frame.btn.export") }}</el-button
              >
            </el-dropdown-item>
            <el-dropdown-item>
              <el-button
                v-hasPermi="['lh:scheduleResult:export']"
                type="primary"
                class="more-btn"
                @click="handleExportCombine"
                >{{ $t("ui.data.column.productStatus.exportSpec") }}</el-button
              >
            </el-dropdown-item>
            <el-dropdown-item>
              <el-button
                class="more-btn"
                v-hasPermi="['lh:scheduleResult:import']"
                type="primary"
                @click="
                  () => $refs.tltUploadForm2.handleImport(importDefaultValue)
                "
                >{{ $t("ui.frame.btn.import") }}</el-button
              >
            </el-dropdown-item>

            <el-dropdown-item>
              <el-button
                v-hasRole="['admin']"
                type="primary"
                class="more-btn"
                @click="handleChangeReleaseStatus"
              >
                {{ $t("ui.data.column.scheduleResult.changeReleaseStatus") }}
              </el-button>
            </el-dropdown-item>
          </el-dropdown-menu>
        </el-dropdown> -->
      </template>
    </page-table>
    <tlt-upload-form
      ref="tltUploadForm"
      :title="$t('ui.data.column.scheduleResult.importLhScheduleResultData')"
      downloadUrl="/lh/lhScheduleResult/importTemplateDown"
      uploadUrl="/lh/lhScheduleResult/exportCombine"
      @uploadSuccess="getList"
      :columns="importColumns"
      :rules="importRules"
    />
    <tlt-upload-form
      ref="tltUploadForm2"
      :title="$t('ui.data.column.scheduleResult.importLhScheduleResultData')"
      downloadUrl="/lh/lhScheduleResult/importTemplateDown"
      uploadUrl="/lh/lhScheduleResult/importData2"
      @uploadSuccess="getList"
      :columns="importColumns"
      :rules="importRules"
    />
    <tlt-upload
      ref="tltUpload"
      :title="$t('ui.data.column.scheduleResult.importFinishQty')"
      downloadUrl="/lh/lhScheduleResult/importTemplateDown"
      :download-params="importTemplateDownloadParams"
      :upload-params="importByCustUploadParams"
      uploadUrl="/lh/lhScheduleResult/importDataByCust"
      @uploadSuccess="getList"
    />
    <AddDialog ref="addDialogRef" @success="handelSuccess" />
    <InfoDialog ref="infoDialogRef" @success="handelSuccess" />
    <AutoPlanDialog ref="autoPlanDialogRef" @success="handleAutoPlanSuccess" @validationError="handleValidationError" />
    <ChangeMachineDialog
      ref="changeMachineDialogRef"
      @success="handelSuccess"
    />
    <ChangeReleaseStatusDialog ref="changeReleaseStatusDialogRef" />
    <el-button style="display: none" ref="hidePopoverBtnRef"></el-button>
    <changePlanDialog ref="changePlanRef" @success="getList" />
    <ValidationErrorDialog ref="validationErrorDialogRef" />
  </basic-container>
</template>
<script>
import { mapState } from "vuex";
import moment from "moment";

import {
  listScheduleResult,
  changeQty,
  removeScheduleResult,
  exportScheduleResult,
  publishScheduleResult,
  issueToMes,
  exportCombine,
  getScheduleDate,
  adjustTextNo,
  generateTextMouldChangePlan,
  increaseMouldStartPlan,
  exportScheduleSummaryReport,
} from "@/api/lh/scheduleResult";
import { checkPermi } from "@/utils/permission";

import TltUploadForm from "@/views/components/tltUploadForm.vue";
import TltUpload from "@/components/tltUpload/tltUpload.vue";
import InfoDialog from "./components/infoDialog.vue";
import AddDialog from "./components/addDialog.vue";
import TPopover from "@/views/components/tPopover.vue";
import AutoPlanDialog from "./components/autoPlanDialog.vue";
import ChangeMachineDialog from "./components/changeMachineDialog.vue";
import ChangeReleaseStatusDialog from "./components/changeReleaseStatusDialog.vue";
import changePlanDialog from "./components/changePlanDialog.vue";
import ValidationErrorDialog from "./components/validationErrorDialog.vue";

export default {
  name: "CuringSchedule",
  components: {
    AutoPlanDialog,
    InfoDialog,
    TltUploadForm,
    TltUpload,
    AddDialog,
    TPopover,
    ChangeMachineDialog,
    ChangeReleaseStatusDialog,
    changePlanDialog,
    ValidationErrorDialog,
  },
  dicts: [
    "adjust_type",
    "IS_RELEASE_LH",
    "biz_factory_name",
    "biz_end_type",
    "biz_construction_stage",
    "lh_schedule_type",
    "biz_mould_Type",
  ],
  provide() {
    return {
      parentDict: this.dict,
    };
  },
  data() {
    let defaultDate = moment().add(1, "days").format("YYYY-MM-DD"); //明天
    // let defaultDate = "2024-06-01";
    return {
      importDefaultValue: {
        scheduleDate: defaultDate,
      },
      importColumns: [
        {
          label: this.$t("ui.data.column.scheduleResult.scheduleDate"),
          prop: "scheduleDate",
          type: "date",
          dateType: "date",
          valueFormat: "yyyy-MM-dd",
          clearable: false,
        },
      ],
      importRules: {
        scheduleDate: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "blur",
          },
        ],
      },

      loading: false,
      data: [],
      page: {
        current: 1,
        pageSize: 20,
        total: 0,
      },
      sort: {},
      search: {
        factoryCode: "116",
        scheduleDate: defaultDate,
      },
      query: {
        factoryCode: "116",
        scheduleDate: defaultDate,
      },
      selection: [],
      dateList: [
        {
          shift: 1,
          shiftDate: "",
        },
        {
          shift: 2,
          shiftDate: "",
        },
        {
          shift: 3,
          shiftDate: "",
        },
        {
          shift: 4,
          shiftDate: "",
        },
        {
          shift: 5,
          shiftDate: "",
        },
        {
          shift: 6,
          shiftDate: "",
        },
        {
          shift: 7,
          shiftDate: "",
        },
        {
          shift: 8,
          shiftDate: "",
        },
      ],
    };
  },
  computed: {
    ...mapState({
      curingMachines: (state) => state.curing.machines,
    }),
    importTemplateDownloadParams() {
      return {
        scheduleDate: this.query.scheduleDate || this.search.scheduleDate,
      };
    },
    importByCustUploadParams() {
      return {
        factoryCode: this.query.factoryCode || this.search.factoryCode,
        scheduleDate: this.query.scheduleDate || this.search.scheduleDate,
      };
    },
    columns() {
      let columns = [
        { type: "selection", fixed: "left" }, // 选择框列
        {
          label: this.$t("ui.data.column.factoryCode"),
          prop: "factoryCode",
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_factory_name, value);
          },
        }, // 工厂代码
        {
          label: this.$t("ui.data.column.scheduleResult.lhMachineCode"),
          prop: "lhMachineCode",
        }, // 硫化机台代码
        {
          label: this.$t("ui.data.column.scheduleResult.materialCode"),
          width:120,
          prop: "materialCode",
        }, // 物料编码
        {
          label: this.$t("ui.data.column.scheduleResult.materialDesc"),
          align: "left",
          prop: "materialDesc",
          minWidth: 350,
          showOverflowTooltip: true,
        }, // 物料描述
        {
          label: this.$t("ui.data.column.scheduleResult.embryoDesc"),
          align: "left",
          prop: "mainMaterialDesc",
          minWidth: 350,
          showOverflowTooltip: true,
        }, // 胚料描述
        {
          prop: "scheduleType",
          label: this.$t("ui.data.column.scheduleResult.scheduleType"),
          minWidth: 100,
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.lh_schedule_type, value);
          },
        }, // 排程类型

        {
          label: this.$t("ui.data.column.scheduleResult.finishQty"),
          prop: "todayNightFinishQty",
          minWidth: 100,
          align: "right",
        }, // 今日夜班完成数量
        {
          label: this.$t("ui.data.column.scheduleResult.totalSurplusQty"),
          prop: "mouldSurplusQty",
          minWidth: 100,
          align: "right",
        }, // 模具剩余数量
        {
          label: this.$t("ui.data.column.scheduleResult.embryoStock"),
          prop: "embryoStock",
        }, // 胚料库存
        {
          label: this.$t("ui.data.column.scheduleResult.lhShiftQty"),
          prop: "singleMouldShiftQty",
        }, // 单模班产数量
        {
          label: this.$t("ui.data.column.scheduleResult.isRelease"),
          prop: "isRelease",
          minWidth: 100,
          formatter: (row, column, value, index) => {
            return this.selectDictLabel(this.dict.type.IS_RELEASE_LH, value);
          },
        }, // 是否发布

        {
          label: this.$t("ui.data.column.scheduleResult.morningShift") + " " + (this.dateList[0]?.shiftDate ?? ""),
          children: [
            {
              prop: "leftRightMould",
              label: this.$t("ui.data.column.scheduleResult.leftRightMould"),
              formatter: (row, column, value) => this.shiftLeftRightMouldFormatter(row, column, value, 1),
            }, // 第1班-左右模
            {
              prop: "constructionStage",
              label: this.$t("ui.data.column.scheduleResult.constructionStage"),
              formatter: (row, column, value) => this.shiftConstructionStageFormatter(row, column, value, 1),
            }, // 第1班-施工阶段
            {
              prop: "class1IsEnd",
              label: this.$t("ui.data.column.scheduleResult.type"),
              formatter: (row, column, value) => this.calcShiftIsEnd(row, 1),
            }, // 第1班-类型
            {
              prop: "class1PlanQty",
              label: this.$t("ui.data.column.scheduleResult.plan"),
              formatter: (row, column, value) => this.shiftPlanQtyFormatter(row, column, value, 1),
            }, // 第1班-计划数量
            {
              prop: "class1FinishQty",
              label: this.$t("ui.data.column.scheduleResult.actual"),
              formatter: (row, column, value) => this.shiftFinishQtyFormatter(row, column, value, 1),
            }, // 第1班-实际数量
            {
              prop: "class1Analysis",
              label: this.$t("ui.data.column.scheduleResult.analysis"),
              formatter: (row, column, value) => this.shiftAnalysisFormatter(row, column, value, 1),
            }, // 第1班-分析备注
          ],
        }, // 第1班-早班
        {
          label: this.$t("ui.data.column.scheduleResult.middleShift") + " " + (this.dateList[1]?.shiftDate ?? ""),
          children: [
            {
              prop: "leftRightMould",
              label: this.$t("ui.data.column.scheduleResult.leftRightMould"),
              formatter: (row, column, value) => this.shiftLeftRightMouldFormatter(row, column, value, 2),
            }, // 第2班-左右模
            {
              prop: "constructionStage",
              label: this.$t("ui.data.column.scheduleResult.constructionStage"),
              formatter: (row, column, value) => this.shiftConstructionStageFormatter(row, column, value, 2),
            }, // 第2班-施工阶段
           {
              prop: "class2IsEnd",
              label: this.$t("ui.data.column.scheduleResult.type"),
              formatter: (row, column, value) => this.calcShiftIsEnd(row, 2),
            }, // 第2班-类型
            {
              prop: "class2PlanQty",
              label: this.$t("ui.data.column.scheduleResult.plan"),
              formatter: (row, column, value) => this.shiftPlanQtyFormatter(row, column, value, 2),
            }, // 第2班-计划数量
            {
              prop: "class2FinishQty",
              label: this.$t("ui.data.column.scheduleResult.actual"),
              formatter: (row, column, value) => this.shiftFinishQtyFormatter(row, column, value, 2),
            }, // 第2班-实际数量
            {
              prop: "class2Analysis",
              label: this.$t("ui.data.column.scheduleResult.analysis"),
              formatter: (row, column, value) => this.shiftAnalysisFormatter(row, column, value, 2),
            }, // 第2班-分析备注

          ],
        }, // 第2班-中班
        {
          label: this.$t("ui.data.column.scheduleResult.nightShift") + " " + (this.dateList[2]?.shiftDate ?? ""),
          children: [
            {
              prop: "leftRightMould",
              label: this.$t("ui.data.column.scheduleResult.leftRightMould"),
              formatter: (row, column, value) => this.shiftLeftRightMouldFormatter(row, column, value, 3),
            }, // 第3班-左右模
            {
              prop: "constructionStage",
              label: this.$t("ui.data.column.scheduleResult.constructionStage"),
              formatter: (row, column, value) => this.shiftConstructionStageFormatter(row, column, value, 3),
            }, // 第3班-施工阶段
           {
              prop: "class3IsEnd",
              label: this.$t("ui.data.column.scheduleResult.type"),
              formatter: (row, column, value) => this.calcShiftIsEnd(row, 3),
            }, // 第3班-类型
            {
              prop: "class3PlanQty",
              label: this.$t("ui.data.column.scheduleResult.plan"),
              formatter: (row, column, value) => this.shiftPlanQtyFormatter(row, column, value, 3),
            }, // 第3班-计划数量
            {
              prop: "class3FinishQty",
              label: this.$t("ui.data.column.scheduleResult.actual"),
              formatter: (row, column, value) => this.shiftFinishQtyFormatter(row, column, value, 3),
            }, // 第3班-实际数量
            {
              prop: "class3Analysis",
              label: this.$t("ui.data.column.scheduleResult.analysis"),
              formatter: (row, column, value) => this.shiftAnalysisFormatter(row, column, value, 3),
            }, // 第3班-分析备注

          ],
        }, // 第3班-晚班
        {
          label: this.$t("ui.data.column.scheduleResult.morningShift") + " " + (this.dateList[3]?.shiftDate ?? ""),
          children: [
            {
              prop: "leftRightMould",
              label: this.$t("ui.data.column.scheduleResult.leftRightMould"),
              formatter: (row, column, value) => this.shiftLeftRightMouldFormatter(row, column, value, 4),
            }, // 第4班-左右模
            {
              prop: "constructionStage",
              label: this.$t("ui.data.column.scheduleResult.constructionStage"),
              formatter: (row, column, value) => this.shiftConstructionStageFormatter(row, column, value, 4),
            }, // 第4班-施工阶段
           {
              prop: "class4IsEnd",
              label: this.$t("ui.data.column.scheduleResult.type"),
              formatter: (row, column, value) => this.calcShiftIsEnd(row, 4),
            }, // 第4班-类型
            {
              prop: "class4PlanQty",
              label: this.$t("ui.data.column.scheduleResult.plan"),
              formatter: (row, column, value) => this.shiftPlanQtyFormatter(row, column, value, 4),
            }, // 第4班-计划数量
            {
              prop: "class4FinishQty",
              label: this.$t("ui.data.column.scheduleResult.actual"),
              formatter: (row, column, value) => this.shiftFinishQtyFormatter(row, column, value, 4),
            }, // 第4班-实际数量
            {
              prop: "class4Analysis",
              label: this.$t("ui.data.column.scheduleResult.analysis"),
              formatter: (row, column, value) => this.shiftAnalysisFormatter(row, column, value, 4),
            }, // 第4班-分析备注

          ],
        }, // 第4班-早班
        {
          label: this.$t("ui.data.column.scheduleResult.middleShift") + " " + (this.dateList[4]?.shiftDate ?? ""),
          children: [
            {
              prop: "leftRightMould",
              label: this.$t("ui.data.column.scheduleResult.leftRightMould"),
              formatter: (row, column, value) => this.shiftLeftRightMouldFormatter(row, column, value, 5),
            }, // 第5班-左右模
            {
              prop: "constructionStage",
              label: this.$t("ui.data.column.scheduleResult.constructionStage"),
              formatter: (row, column, value) => this.shiftConstructionStageFormatter(row, column, value, 5),
            }, // 第5班-施工阶段
            {
              prop: "class5IsEnd",
              label: this.$t("ui.data.column.scheduleResult.type"),
              formatter: (row, column, value) => this.calcShiftIsEnd(row, 5),
            }, // 第5班-类型
            {
              prop: "class5PlanQty",
              label: this.$t("ui.data.column.scheduleResult.plan"),
              formatter: (row, column, value) => this.shiftPlanQtyFormatter(row, column, value, 5),
            }, // 第5班-计划数量
            {
              prop: "class5FinishQty",
              label: this.$t("ui.data.column.scheduleResult.actual"),
              formatter: (row, column, value) => this.shiftFinishQtyFormatter(row, column, value, 5),
            }, // 第5班-实际数量
            {
              prop: "class5Analysis",
              label: this.$t("ui.data.column.scheduleResult.analysis"),
              formatter: (row, column, value) => this.shiftAnalysisFormatter(row, column, value, 5),
            }, // 第5班-分析备注

          ],
        }, // 第5班-中班
        {
          label: this.$t("ui.data.column.scheduleResult.nightShift") + " " + (this.dateList[5]?.shiftDate ?? ""),
          children: [
            {
              prop: "leftRightMould",
              label: this.$t("ui.data.column.scheduleResult.leftRightMould"),
              formatter: (row, column, value) => this.shiftLeftRightMouldFormatter(row, column, value, 6),
            }, // 第6班-左右模
            {
              prop: "constructionStage",
              label: this.$t("ui.data.column.scheduleResult.constructionStage"),
              formatter: (row, column, value) => this.shiftConstructionStageFormatter(row, column, value, 6),
            }, // 第6班-施工阶段
            {
              prop: "class6IsEnd",
              label: this.$t("ui.data.column.scheduleResult.type"),
              formatter: (row, column, value) => this.calcShiftIsEnd(row, 6),
            }, // 第6班-类型
            {
              prop: "class6PlanQty",
              label: this.$t("ui.data.column.scheduleResult.plan"),
              formatter: (row, column, value) => this.shiftPlanQtyFormatter(row, column, value, 6),
            }, // 第6班-计划数量
            {
              prop: "class6FinishQty",
              label: this.$t("ui.data.column.scheduleResult.actual"),
              formatter: (row, column, value) => this.shiftFinishQtyFormatter(row, column, value, 6),
            }, // 第6班-实际数量
            {
              prop: "class6Analysis",
              label: this.$t("ui.data.column.scheduleResult.analysis"),
              formatter: (row, column, value) => this.shiftAnalysisFormatter(row, column, value, 6),
            }, // 第6班-分析备注

          ],
        }, // 第6班-晚班
        {
          label: this.$t("ui.data.column.scheduleResult.morningShift") + " " + (this.dateList[6]?.shiftDate ?? ""),
          children: [
            {
              prop: "leftRightMould",
              label: this.$t("ui.data.column.scheduleResult.leftRightMould"),
              formatter: (row, column, value) => this.shiftLeftRightMouldFormatter(row, column, value, 7),
            }, // 第7班-左右模
            {
              prop: "constructionStage",
              label: this.$t("ui.data.column.scheduleResult.constructionStage"),
              formatter: (row, column, value) => this.shiftConstructionStageFormatter(row, column, value, 7),
            }, // 第7班-施工阶段
            {
              prop: "class7IsEnd",
              label: this.$t("ui.data.column.scheduleResult.type"),
              formatter: (row, column, value) => this.calcShiftIsEnd(row, 7),
            }, // 第7班-类型
            {
              prop: "class7PlanQty",
              label: this.$t("ui.data.column.scheduleResult.plan"),
              formatter: (row, column, value) => this.shiftPlanQtyFormatter(row, column, value, 7),
            }, // 第7班-计划数量
            {
              prop: "class7FinishQty",
              label: this.$t("ui.data.column.scheduleResult.actual"),
              formatter: (row, column, value) => this.shiftFinishQtyFormatter(row, column, value, 7),
            }, // 第7班-实际数量
            {
              prop: "class7Analysis",
              label: this.$t("ui.data.column.scheduleResult.analysis"),
              formatter: (row, column, value) => this.shiftAnalysisFormatter(row, column, value, 7),
            }, // 第7班-分析备注

          ],
        }, // 第7班-早班
        {
          label: this.$t("ui.data.column.scheduleResult.middleShift") + " " + (this.dateList[7]?.shiftDate ?? ""),
          children: [
            {
              prop: "leftRightMould",
              label: this.$t("ui.data.column.scheduleResult.leftRightMould"),
              formatter: (row, column, value) => this.shiftLeftRightMouldFormatter(row, column, value, 8),
            }, // 第8班-左右模
            {
              prop: "constructionStage",
              label: this.$t("ui.data.column.scheduleResult.constructionStage"),
              formatter: (row, column, value) => this.shiftConstructionStageFormatter(row, column, value, 8),
            }, // 第8班-施工阶段
            {
              prop: "class8IsEnd",
              label: this.$t("ui.data.column.scheduleResult.type"),
              formatter: (row, column, value) => this.calcShiftIsEnd(row, 8),
            }, // 第8班-类型
            {
              prop: "class8PlanQty",
              label: this.$t("ui.data.column.scheduleResult.plan"),
              formatter: (row, column, value) => this.shiftPlanQtyFormatter(row, column, value, 8),
            }, // 第8班-计划数量
            {
              prop: "class8FinishQty",
              label: this.$t("ui.data.column.scheduleResult.actual"),
              formatter: (row, column, value) => this.shiftFinishQtyFormatter(row, column, value, 8),
            }, // 第8班-实际数量
            {
              prop: "class8Analysis",
              label: this.$t("ui.data.column.scheduleResult.analysis"),
              formatter: (row, column, value) => this.shiftAnalysisFormatter(row, column, value, 8),
            }, // 第8班-分析备注

          ],
        }, // 第8班-中班
        {
          prop: "remark",
          label: this.$t("ui.data.column.remark"),
        }, // 备注
        {
          label: this.$t("ui.data.column.scheduleResult.batchNo"),
          prop: "batchNo",
          align: "left",
          minWidth: 160,
        }, // 批号
        {
          label: this.$t("ui.data.column.scheduleResult.orderNo"),
          prop: "orderNo",
          align: "left",
          minWidth: 160,
        }, // 订单号
        {
          prop: "updateTime",
          label: this.$t("ui.data.column.scheduleResult.updateTime"),
          minWidth: 180,
        }, // 更新时间
        // {
        //   prop: "todayNightFinishQty",
        //   label: this.$t("ui.data.column.scheduleResult.todayNightFinishQty"),
        //   minWidth: 120,
        //   align: "right",
        // },
        // {
        //   prop: "todayNightFinishQty",
        //   label: this.$t("ui.data.column.scheduleResult.todayNightFinishQty"),
        //   minWidth: 120,
        //   align: "right",
        // },
        {
          prop: "rowOperator",
          label: this.$t("common.option"),
          width: 150,
          fixed: "right",
          render: ({ row }) => {
            return (
              <div>
                {checkPermi(["lh:lhScheduleResult:save"]) ? (
                  <el-button
                    type="text"
                    size="mini"
                    icon="el-icon-edit"
                    onClick={() => this.handleEdit(row)}
                  >
                    {this.$t("ui.frame.btn.modify")}
                  </el-button>
                ) : null}
                {checkPermi(["lh:lhScheduleResult:remove"]) ? (
                  <el-button
                    type="text"
                    size="mini"
                    icon="el-icon-delete"
                    onClick={() => this.handleDelete(row)}
                  >
                    {this.$t("ui.frame.btn.delete")}
                  </el-button>
                ) : null}
              </div>
            );
          },
        }, // 操作列
      ];
      return columns;
    },
    searchColumns() {
      return [
        {
          label: this.$t("ui.data.column.factoryCode"),
          prop: "factoryCode",
          type: "select",
          dictData: this.dict.type.biz_factory_name,
          filterable: true,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.scheduleDate"),
          prop: "scheduleDate",
          type: "date",
          valueFormat: "yyyy-MM-dd",
          listeners: {
            change: this.handleYearMonthChange,
          },
        },
        {
          label: this.$t("ui.data.column.scheduleResult.orderNo"),
          prop: "orderNo",
          align: "left",
          minWidth: 160,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.batchNo"),
          prop: "batchNo",
          align: "left",
          minWidth: 160,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.isRelease"),
          prop: "isRelease",
          render: (form) => {
            return (
              <dict-select
                v-model={form.isRelease}
                options={this.dict.type.IS_RELEASE_LH}
              />
            );
          },
        },
        {
          label: this.$t("ui.data.column.scheduleResult.materialCode"),
          prop: "materialCode",
        },
        {
          label: this.$t("ui.data.column.scheduleResult.materialDesc"),
          minWidth: 350,
          align: "left",
          prop: "materialDesc",
        },
        {
          label: this.$t("ui.data.column.scheduleResult.embryoDesc"),
          minWidth: 350,
          align: "left",
          prop: "mainMaterialDesc",
        },
        {
          label: this.$t("ui.data.column.cxScheduleResult.lhMachineCode"),
          prop: "lhMachineCode",
          type: "select",
          dictData: this.curingMachines,
          labelKey: "machineCode",
          valueKey: "machineCode",
          filterable: true,
        },
      ];
    },
  },
  methods: {
    handleYearMonthChange(val) {
      this.search = {
        ...this.search,
        scheduleDate: val,
      };
      this.query = {
        ...this.search,
        scheduleDate: val,
      };
      this.getList();
    },
    async handleIncreaseMouldStartPlan() {
      try {
        const row = this.selection[0];
        await this.$confirm(
          this.$t("ui.data.alert.lhScheduleResult.increaseMouldStartPlanConfirm"),
          {
            type: "warning",
          }
        );
        this.loading = true;
        const result = await increaseMouldStartPlan(row);
        this.$modal.msgSuccess(
          result.msg ||
          this.$t("ui.data.alert.lhScheduleResult.increaseMouldStartPlanSuccess")
        );
        this.getList();
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
    },
    calcShiftIsEnd(row, shiftIndex) {
      if (this.isShiftAfterEnding(row, shiftIndex)) {
        return '';
      }
      const planQty = row['class' + shiftIndex + 'PlanQty'];
      if (planQty == null || planQty <= 0) {
        return '';
      }
      const referenceQty = Math.max(row.mouldSurplusQty || 0, row.embryoStock || 0);
      if (referenceQty <= 0) {
        return this.selectDictLabel(this.dict.type.biz_end_type, "0");
      }
      let totalPlanQty = 0;
      for (let i = 1; i <= 8; i++) {
        totalPlanQty += (row['class' + i + 'PlanQty'] || 0);
      }
      if (totalPlanQty < referenceQty) {
        return this.selectDictLabel(this.dict.type.biz_end_type, "0");
      }
      let remaining = referenceQty;
      for (let i = 1; i <= 8; i++) {
        remaining -= (row['class' + i + 'PlanQty'] || 0);
        if (remaining <= 0) {
          if (i === shiftIndex) {
            return this.selectDictLabel(this.dict.type.biz_end_type, "1");
          }
          break;
        }
      }
      return this.selectDictLabel(this.dict.type.biz_end_type, "0");
    },
    async handleGenerateTextMouldChangePlan() {
      try {
        this.loading = true;
        const row = this.selection[0];
        const result = await generateTextMouldChangePlan({
          id: row.id,
          factoryCode: row.factoryCode,
        });
        if (result && result.needConfirm) {
          await this.$confirm(result.msg || this.$t("ui.data.alert.lhMouldChangePlan.generateTextPlan.replaceConfirm"), {
            type: "warning",
          });
          const confirmResult = await generateTextMouldChangePlan({
            id: row.id,
            factoryCode: row.factoryCode,
            confirmReplace: true,
          });
          this.$modal.msgSuccess(confirmResult.msg || this.$t("ui.data.alert.lhMouldChangePlan.generateTextPlan.success"));
          this.getList();
          return;
        }
        this.$modal.msgSuccess(result.msg || this.$t("ui.data.alert.lhMouldChangePlan.generateTextPlan.success"));
        this.getList();
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
    },
    isShiftAfterEnding(row, shiftIndex) {
      const referenceQty = Math.max(row.mouldSurplusQty || 0, row.embryoStock || 0);
      if (referenceQty <= 0) {
        return false;
      }
      let totalPlanQty = 0;
      for (let i = 1; i <= 8; i++) {
        totalPlanQty += (row['class' + i + 'PlanQty'] || 0);
      }
      if (totalPlanQty < referenceQty) {
        return false;
      }
      let remaining = referenceQty;
      for (let i = 1; i <= 8; i++) {
        remaining -= (row['class' + i + 'PlanQty'] || 0);
        if (remaining <= 0) {
          return shiftIndex > i;
        }
      }
      return false;
    },
    shiftLeftRightMouldFormatter(row, column, value, shiftIndex) {
      if (this.isShiftAfterEnding(row, shiftIndex)) return '';
      const planQty = row['class' + shiftIndex + 'PlanQty'];
      if (planQty == null || planQty <= 0) return '';
      return value;
    },
    shiftConstructionStageFormatter(row, column, value, shiftIndex) {
      if (this.isShiftAfterEnding(row, shiftIndex)) return '';
      const planQty = row['class' + shiftIndex + 'PlanQty'];
      if (planQty == null || planQty <= 0) return '';
      const dictValue = value || "0";
      return this.selectDictLabel(this.dict.type.biz_construction_stage, dictValue);
    },
    shiftPlanQtyFormatter(row, column, value, shiftIndex) {
      if (this.isShiftAfterEnding(row, shiftIndex)) return '';
      if (value == null || value === 0) return '';
      return value;
    },
    shiftFinishQtyFormatter(row, column, value, shiftIndex) {
      if (this.isShiftAfterEnding(row, shiftIndex)) return '';
      const planQty = row['class' + shiftIndex + 'PlanQty'];
      if (planQty == null || planQty <= 0) return '';
      return value;
    },
    shiftAnalysisFormatter(row, column, value, shiftIndex) {
      if (this.isShiftAfterEnding(row, shiftIndex)) return '';
      const planQty = row['class' + shiftIndex + 'PlanQty'];
      if (planQty == null || planQty <= 0) return '';
      return value;
    },
    decodeRemark(remark) {
      if (!remark) return remark;
      return remark
        .replace(/__PERCENT__/g, '%')
        .replace(/__AMP__/g, '&')
        .replace(/__LT__/g, '<')
        .replace(/__GT__/g, '>')
        .replace(/__QUOT__/g, '"')
        .replace(/__APOS__/g, "'");
    },
    decodeRemarkFields(row) {
      if (!row) return row;
      row.remark = this.decodeRemark(row.remark);
      for (let i = 1; i <= 8; i++) {
        const field = 'class' + i + 'Analysis';
        row[field] = this.decodeRemark(row[field]);
      }
      return row;
    },
    async getAdjustTextNo() {
      try {
        this.loading = true;
        let row = this.selection[0];
        const data = await adjustTextNo({
          id: row.id,
          factoryCode: row.factoryCode,
          lhMachineCode: row.lhMachineCode,
        });
        this.$modal.msgSuccess(data.msg);
        this.getList();
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
    },
    // 调量
    handleChangePlan() {
      if (this.$refs.changePlanRef) {
        let row = this.selection[0];
        this.$refs.changePlanRef.show(row);
      }
    },
    async getDate() {
      try {
        let res = await getScheduleDate({
          scheduleDate: this.query.scheduleDate,
        });
        console.log(res);
        this.dateList = res;
      } catch (error) {}
    },
    handleAutoPlan() {
      if (this.$refs.autoPlanDialogRef) {
        this.$refs.autoPlanDialogRef.show(this.query);
      }
    },
    handleAutoPlanSuccess(params) {
      this.getList();
    },
    handleValidationError(data) {
      if (this.$refs.validationErrorDialogRef) {
        this.$refs.validationErrorDialogRef.show(data);
      }
    },
    handleAdd() {
      if (this.$refs.addDialogRef) {
        const row =
          this.selection.length === 1 ? this.selection[0] : null;
        this.$refs.addDialogRef.show(row);
      }
    },
    handleEdit(row) {
      if (this.$refs.infoDialogRef) {
        // 深拷贝 row 数据，避免编辑时影响列表原始数据
        const rowCopy = JSON.parse(JSON.stringify(row));
        this.$refs.infoDialogRef.show(rowCopy);
      }
    },
    handleChangeMachine() {
      // 转机台仅支持单选，未选或多选时直接提示并阻断弹窗打开。
      if (this.selection.length !== 1) {
        this.$modal.msgWarning(this.$t("请选择一条需要转机台的数据"));
        return;
      }
      if (this.$refs.changeMachineDialogRef) {
        let row = this.selection[0];
        this.$refs.changeMachineDialogRef.show(row);
      }
    },
    handleGotoMachineGant() {
      this.$router.push("/curingPlan/machineGantChart");
    },
    handleGotoSpecDescGant() {
      this.$router.push("/curingPlan/specDescGantChart");
    },
    handleShowChangeQty(row) {
      if (this.$refs.infoDialogRef) {
        // 深拷贝 row 数据，避免编辑时影响列表原始数据
        const rowCopy = JSON.parse(JSON.stringify(row));
        this.$refs.infoDialogRef.show(rowCopy, true);
      }
    },
    async handleChangeQty(row) {
      try {
        this.loading = true;
        const data = await changeQty(row);
        this.$modal.msgSuccess(data.msg);
        // this.$set(this.page, "current", 1);
        this.getList();
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
    },

    handleDelete(row) {
      this.$confirm(this.$t("common.confirm.delete"), {
        type: "warning",
      }).then(() => {
        const ids = row.id;
        removeScheduleResult({ ids }).then((data) => {
          this.$modal.msgSuccess(data.msg);
          if (this.data.length <= 1 && this.page.current > 1) {
            this.$set(this.page, "current", this.page.current - 1);
          }
          this.getList();
        });
      });
    },
    handleDeleteMulti() {
      // if (this.selection.length == 0) {
      //   this.$modal.msgWarning(this.$t("请至少选择一条记录"));
      //   return;
      // }
      this.$confirm(this.$t("common.confirm.delete"), { type: "warning" })
        .then(async () => {
          //确认提交
          try {
            this.loading = true;
            let ids = [];
            this.selection.forEach((element) => {
              ids.push(element.id);
            });
            const params = {
              ids: ids.join(),
            };
            const data = await removeScheduleResult(params);
            this.$modal.msgSuccess(data.msg);
            if (ids.length >= this.data.length && this.page.current > 1) {
              this.$set(this.page, "current", this.page.current - 1);
            }
            this.getList();
          } catch (error) {
          } finally {
            this.loading = false;
          }
        })
        .catch(() => {});
    },
    handleExport() {
      this.$confirm(this.$t(`ui.data.column.scheduleResult.confirmExportLhScheduleResult`), {
        type: "warning",
      }).then(() => {
        try {
          this.loading = true;
          let params = this.formatParams();
          params = {
            ...params,
            // pageSize: undefined,
            // pageNum: undefined,
          };
          exportScheduleResult(params);
        } catch (error) {
          console.error(error);
        } finally {
          this.loading = false;
        }
      });
    },
    handleExportCombine() {
      let params = this.formatParams();
      params = {
        ...params,
        // pageSize: undefined,
        // pageNum: undefined,
      };
      exportCombine(params);
    },
    handleExportSummaryReport() {
      this.$confirm(this.$t(`ui.data.column.scheduleResult.confirmExportScheduleSummaryReport`), {
        type: "warning",
      }).then(() => {
        try {
          this.loading = true;
          let params = this.formatParams();
          exportScheduleSummaryReport(params);
        } catch (error) {
          console.error(error);
        } finally {
          this.loading = false;
        }
      });
    },
    handlePublish() {
      this.$confirm(this.$t(`ui.biz.alter.makeSurePublish`), {
        type: "warning",
      }).then(async () => {
        try {
          this.loading = true;
          let ids = [];
          this.selection.forEach((element) => {
            ids.push(element.id);
          });
          const params = {
            scheduleDate: this.query.scheduleDate,
            ids: ids.join(),
          };
          const valid = await issueToMes({ scheduleDate: this.query.scheduleDate });
          if (valid.msg == "0") {
            this.$confirm(
              this.$t("ui.data.column.scheduleResult.hasNullLhMachineCode")
            )
              .then(async () => {
                const data = await publishScheduleResult(params);
                this.$modal.msgSuccess(data.msg);
                this.getList();
              })
              .catch(() => {
                this.loading = false;
              });
          } else {
            const data = await publishScheduleResult(params);
            this.$modal.msgSuccess(data.msg);
            this.getList();
          }
        } catch (error) {
          console.error(error);
        } finally {
          this.loading = false;
        }
      });
    },
    handleChangeReleaseStatus() {
      if (this.$refs.changeReleaseStatusDialogRef) {
        this.$refs.changeReleaseStatusDialogRef.show(this.query.scheduleDate);
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
      this.$set(this.page, "current", current);
      this.$set(this.page, "pageSize", pageSize);
      this.getList();
    },
    async handelSuccess() {
      await this.getList();
    },
    handleSelectionChange(rows) {
      this.selection = rows;
    },
    handleUnschedule() {
      let query = {
        factoryCode: this.query.factoryCode,
        scheduleDate: this.query.scheduleDate,
      };

      this.$router.push({
        path: "/curingPlan/curingUnscheduleResult",
        query: query,
      });
    },

    //util
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
      if (row.multipleEmbryosOfSameSapFlag > 1) {
        return { "background-color": "#BFE0F7" };
      }
      return {};
    },
    cellStyle({ row, column, rowIndex, columnIndex }) {
      if (column.property === "lhMachineCode") {
        if (row.changeMachine == 1) {
          return { background: "#ef6776" };
        }
      }
      if (column.property === "class1PlanQty") {
        if (row.changeClass1Plan == 1) {
          return { background: "#ef6776" };
        }
      }
      if (column.property === "class2PlanQty") {
        if (row.changeClass2Plan == 1) {
          return { background: "#ef6776" };
        }
      }
      if (column.property === "class3PlanQty") {
        if (row.changeClass3Plan == 1) {
          return { background: "#ef6776" };
        }
      }
      if (column.property === "class4PlanQty") {
        if (row.changeClass4Plan == 1) {
          return { background: "#ef6776" };
        }
      }
      if (column.property === "class5PlanQty") {
        if (row.changeClass5Plan == 1) {
          return { background: "#ef6776" };
        }
      }
      if (column.property === "class6PlanQty") {
        if (row.changeClass6Plan == 1) {
          return { background: "#ef6776" };
        }
      }
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

    formatParams() {
      const params = {
        pageSize: this.page.pageSize,
        pageNum: this.page.current,
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
    //
    async getList() {
      try {
        this.loading = true;
        await this.getDate();
        const data = await listScheduleResult(this.formatParams());
        const rows = data.rows || [];
        rows.forEach(row => this.decodeRemarkFields(row));
        this.data = rows;
        this.page.total = data.total;
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
        this.$nextTick(() => {
          const tableRef = this.$refs.curingScheduleTable;
          if (tableRef && tableRef.getTableRef) {
            const table = tableRef.getTableRef();
            if (table && table.doLayout) {
              table.doLayout();
            }
          }
        });
      }
    },
  },
  created() {
    //设置默认排程时间
    let date = moment().add(2, "days").format("YYYY-MM-DD");

    // date = "2023-06-01"; //test
    this.query.scheduleDate = date;
    this.search.scheduleDate = date;
    this.$store.dispatch("curing/getMachineList");
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
