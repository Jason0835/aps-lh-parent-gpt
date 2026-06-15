<template>
  <basic-container>
    <page-table
      ref="monthPlanPageTableRef"
      tableRef="MonthPlanFinalAdjustQueryTable"
      :calcHeight="88"
      v-loading="loading"
      :element-loading-text="loadText"
      :columns="columns"
      :searchColumns="searchColumns"
      :data="data"
      :page="page"
      :search="search"
      @refresh="getList"
      @search="handleSearch"
      @pageChange="handlePageChange"
      @sort-change="handleSortChange"
      @selection-change="handleStructureAdjustSelectionChange"
      :showSummary="false"
      :selectArea="true"
      :row-class-name="tableRowClassName"
    >
      <template slot="header">
        <div class="toolbar-row">
          <el-button
            type="primary"
            :loading="getAdjustOrderLoading"
            v-hasPermi="['monthplan:mpWeekRollAdjust:getAdjustDetailList']"
            @click="handleGetAdjustOrder"
            >{{ $t("ui.data.rollingCycle.adjustOrder") }}</el-button
          >
          <el-button
            type="primary"
            plain
            :disabled="adjustFlowInProgress"
            v-hasPermi="['monthplan:mpWeekRollAdjust:getAdjustDetailList']"
            @click="handleStructureInnerAdjust"
            >{{
              $t("ui.data.column.monthPlanFinalAdjustQuery.structureInnerAdjust")
            }}</el-button
          >
          <el-button
            type="primary"
            plain
            v-hasPermi="['monthplan:mpStructureAllocation:list']"
            @click="handleStructureAdjust"
            >{{
              $t("ui.data.column.monthPlanFinalAdjustQuery.structureAdjust")
            }}</el-button
          >
          <el-button
            v-hasPermi="['monthplan:mpAdjustResult:list']"
            @click="handleViewAdjustVersion"
            >{{
            $t("ui.data.column.monthPlanFinalAdjustQuery.viewAdjustVersion")
          }}</el-button>
          <el-button
          v-hasPermi="['monthplan:mpAdjustResult:importData']"
            @click="$refs.tltUpload.handleImport()"
            >{{ $t("ui.frame.btn.import") }}</el-button
          >
          <!-- 导出：与 console 排产明细 mouldingDayResult 页 factoryMonthPlanMouldDayResult/export 一致 -->
          <el-button
            @click="handleExport"
            v-hasPermi="['monthplan:mouldingDayResult:export']"
            >{{ $t("ui.frame.btn.export") }}</el-button
          >
          <!-- 全物料导出：与 mouldingDayResult 页 exportAllMaterial 一致 -->
          <el-button
            @click="handleExportAllMaterial"
            v-hasPermi="['monthplan:mouldingDayResult:exportAllMaterial']"
            >{{
              $t("ui.data.column.monthPlanFinalAdjustQuery.exportAllMaterial")
            }}</el-button
          >
          <el-button
            :loading="syncLoading"
            :disabled="adjustFlowInProgress"
            v-hasPermi="['monthplan:factoryMonthPlanFinalResult:sync']"
            @click="handleIssueScmMes"
            >{{
              $t("ui.data.column.monthPlanFinalAdjustQuery.issueScmMes")
            }}</el-button
          >
          <div class="current-machine-wrap">
            <span class="current-machine-label">{{
              $t("ui.data.column.monthPlanFinalAdjustQuery.currentAdjustMachine")
            }}</span>
            <el-input
              class="current-machine-input"
              :value="currentAdjustMachine"
              disabled
              :placeholder="
                $t('ui.data.column.monthPlanFinalAdjustQuery.cxMachine')
              "
            />
          </div>
          <div class="current-machine-wrap">
            <span class="current-machine-label">{{ $t('ui.data.column.monthlyProductionPlan.currentAdjustStructure') }}</span>
            <el-input
              class="current-machine-input"
              :value="currentAdjustStructure"
              disabled
              :placeholder="$t('ui.data.column.monthlyProductionPlan.currentAdjustStructure')"
            />
          </div>
          <div class="current-machine-wrap">
            <span class="current-machine-label">{{
              $t("ui.data.column.monthPlanFinalAdjustQuery.adjustStartDay")
            }}</span>
            <el-input
              class="current-machine-input"
              :value="currentAdjustBeginDay"
              disabled
              :placeholder="
                $t('ui.data.column.monthPlanFinalAdjustQuery.adjustStartDay')
              "
            />
          </div>
          <div class="current-machine-wrap">
            <span class="current-machine-label">{{
              $t("ui.data.column.monthPlanFinalAdjustQuery.adjustEndDay")
            }}</span>
            <el-input
              class="current-machine-input"
              :value="currentAdjustEndDay"
              disabled
              :placeholder="
                $t('ui.data.column.monthPlanFinalAdjustQuery.adjustEndDay')
              "
            />
          </div>
        </div>
      </template>
      <template slot="footer">
        <div class="footer-actions">
          <el-button
            type="primary"
            :loading="confirmAdjustLoading"
            :disabled="
              confirmAdjustLoading ||
              recalculateLoading
            "
            v-hasPermi="['monthplan:mpWeekRollAdjust:autoAdjust']"
            @click="handleConfirmAdjust"
            >{{
              $t("ui.data.column.monthPlanFinalAdjustQuery.confirmAdjust")
            }}</el-button
          >
          <el-button
            :loading="recalculateLoading"
            :disabled="
              confirmAdjustLoading ||
              recalculateLoading
            "
            v-hasPermi="['monthplan:mpWeekRollAdjust:recalculate']"
            @click="handleRecalculate"
            >{{
              $t("ui.data.column.monthPlanFinalAdjustQuery.recalculate")
            }}</el-button
          >
        </div>
      </template>
    </page-table>
    <structure-adjust-dialog
      ref="structureAdjustDialogRef"
      @structure-adjust-saved="onStructureAdjustSaved"
      @plan-downtime-applied="onPlanDowntimeApplied"
    />
    <adjust-version-dialog
      ref="adjustVersionDialogRef"
      @select-production-version="onAdjustVersionDialogSelectProductionVersion"
    />
    <el-dialog
      :title="$t('ui.data.column.monthPlanFinalAdjustQuery.issueScmMes')"
      :visible.sync="syncDialog.visible"
      width="520px"
      append-to-body
      @close="resetSyncDialog"
    >
      <el-form
        ref="syncForm"
        :model="syncDialog.form"
        :rules="syncDialogRules"
        label-width="140px"
      >
        <el-form-item
          :label="$t('ui.data.column.report.proSizeSummary.yearMonth')"
          prop="yearMonth"
        >
          <el-date-picker
            v-model="syncDialog.form.yearMonth"
            type="month"
            value-format="yyyy-MM"
            format="yyyy-MM"
            :placeholder="
              $t('ui.data.column.monthPlanFinalAdjustQuery.issueScmMesRuleYearMonth')
            "
            style="width: 100%"
            @change="handleSyncBaseChange"
          />
        </el-form-item>
        <el-form-item :label="$t('ui.data.column.factoryCode')" prop="factoryCode">
          <el-select
            v-model="syncDialog.form.factoryCode"
            :placeholder="$t('ui.frame.btn.choose')"
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
        <el-form-item
          :label="$t('ui.data.monthlyProductionPlan.productionVersion')"
          prop="productionVersion"
        >
          <el-select
            v-model="syncDialog.form.productionVersion"
            :placeholder="
              $t(
                'ui.data.column.monthPlanFinalAdjustQuery.issueScmMesPlaceholderProductionVersion'
              )
            "
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
        <el-form-item
          :label="$t('ui.data.monthlyProductionPlan.lastMonthPlanVersion')"
          prop="lastMonthPlanVersion"
        >
          <el-select
            v-model="syncDialog.form.lastMonthPlanVersion"
            :placeholder="
              $t(
                'ui.data.column.monthPlanFinalAdjustQuery.issueScmMesPlaceholderDemandVersion'
              )
            "
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
        <el-button @click="syncDialog.visible = false">{{
          $t("common.button.cancel")
        }}</el-button>
        <el-button
          type="primary"
          :loading="syncLoading"
          v-hasPermi="['monthplan:factoryMonthPlanFinalResult:sync']"
          @click="submitSyncAdjustedMonthPlan"
          >{{
            $t("ui.data.column.monthPlanFinalAdjustQuery.issueScmMesOk")
          }}</el-button
        >
      </span>
    </el-dialog>
    <tlt-upload
      ref="tltUpload"
      downloadUrl=""
      uploadUrl="/monthplan/mpAdjustResult/importData"
      @uploadSuccess="getList"
    />
  </basic-container>
</template>

<script>
import moment from "moment";
import {mapGetters} from "vuex";
import {downloadLink} from "@/utils/request";
import {getFinalResultVersionList, listMonthPlanFinal4Adjust, syncAdjustedMonthPlanToScmAndMes,} from "@/api/monthplan/monthlyProductionPlan";
import {
  confirmAdjust,
  getAdjustDetailList,
  getAdjustsCxMachineFromRedis,
  recalculateWeekRollAdjust,
  saveAdjustResult,
  setAdjustsCxMachineFromRedis,
  statisticsResult,
  versionAdjust,
} from "@/api/monthplan/adjustStructure";
import { getByParamCode } from "@/api/monthplan/factoryParam";
import structureAdjustDialog from "./components/structureAdjustDialog.vue";
import adjustVersionDialog from "./components/adjustVersionDialog.vue";
import tltUpload from "@/components/tltUpload/tltUpload.vue";

export default {
  name: "MonthPlanFinalAdjustQuery",
  components: {
    structureAdjustDialog,
    adjustVersionDialog,
    tltUpload
  },
  /** 含成型机台选择弹窗所需字典（与 formingCapacitySelect / 周程滚动页一致） */
  dicts: [
    "biz_factory_name",
    "biz_brand_type",
    "biz_yes_no",
    "biz_class_type",
    "biz_machine_brand",
    "biz_construction_stage",
  ],
  provide() {
    return {
      parentDict: this.dict,
    };
  },
  data() {
    return {
      loading: false,
      loadText: this.$t("newPage.message.loadingShort"),
      data: [],
      page: {
        current: 1,
        pageSize: 20,
        total: 0,
      },
      sort: {},
      search: {},
      query: {},
      versionOptions: [],
      /** getVersionList 原始行：供弹窗选版本后补全 versionOptions 的 adjustType（仅本页） */
      monthPlanMpAdjustVersionRawRows: [],
      /** 当前调整机台（只读，来自 Redis） */
      currentAdjustMachine: "",
      /** 当前调整结构（来自 Redis） */
      currentAdjustStructure: "",
      /** 调整开始日期（来自 Redis） */
      currentAdjustBeginDay: "",
      /** 调整结束日期（来自 Redis） */
      currentAdjustEndDay: "",
      /** 调整版本号（来自 Redis） */
      currentAdjustMonthPlanVersion: "",
      confirmAdjustLoading: false,
      recalculateLoading: false,
      getAdjustOrderLoading: false,
      syncLoading: false,
      /** 下发 SCM/MES 弹窗（与月度生产计划页「推送SCM/MES」同源接口与交互） */
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
      },
      /** 1–31 号列编辑前原始值，用于失焦时与 rollingCycle 一致判断是否调用 save */
      dayEditOriginalValue: null,
      /** 锁定天数（SYS0206001）：>0 含今天起向后；0 表示仅锁定今天之前的日期 */
      lockedDays: 0,
      /** 周程滚动调整日（SYS0206006），与后端 setAdjustDate 一致 */
      weekRollAdjustDate: "",
      /** 结构调整：表格多选勾选行（统计行不可选）；超过 1 条时禁用「结构调整」按钮 */
      structureAdjustSelection: [],
      /** 日排产单元格单击选中（用于高亮与填充柄） */
      dayCellActive: null,
      /** 拖动填充预览范围（Vue 渲染，避免 DOM class 被表格重渲染冲掉） */
      dayFillDragPreview: null,
      /** 拖拽填充结束后短暂抑制双击，避免误触发 */
      dayFillDragSuppressDblclickUntil: 0,
      /** 拖动填充开始时抑制 blur save，待松手后统一 save */
      suppressDayEditBlurSave: false,
    };
  },
  computed: {
    ...mapGetters("globalList", ["structureList"]),
    /** 当前调整机台有值则视为「调整进行中」 */
    adjustFlowInProgress() {
      return (this.currentAdjustMachine || "").trim() !== "";
    },
    /** 无机台或已清空：结构内调整、结构调整、确认、重新计算可用 */
    canUsePrimaryAdjustActions() {
      return !this.adjustFlowInProgress;
    },
    /** 有机台：仅「继续调整」可用 */
    canUseContinueAdjust() {
      return this.adjustFlowInProgress;
    },
    /** 结构调整入口：调整进行中不可点；勾选多于 1 条时不可点 */
    isStructureAdjustEntryDisabled() {
      if (!this.canUsePrimaryAdjustActions) {
        return true;
      }
      const n = (this.structureAdjustSelection || []).length;
      return n > 1;
    },
    searchColumns() {
      return [
        {
          prop: "factoryCode",
          label: this.$t("ui.data.column.factoryCode"),
          type: "select",
          dictData: this.dict.type.biz_factory_name,
          clearable: false,
          filterable: true,
          listeners: {
            change: this.handleBaseQueryChange,
          },
        },
        {
          prop: "yearMonth",
          label: this.$t("ui.data.column.report.proSizeSummary.yearMonth"),
          type: "date",
          dateType: "month",
          valueFormat: "yyyy-MM",
          clearable: false,
          listeners: {
            change: this.handleBaseQueryChange,
          },
        },
        {
          prop: "structureName",
          label: this.$t("ui.data.column.finishStock.structureName"),
          type: "select",
          dictData: this.structureList,
          filterable: true,
        },
        {
          prop: "version",
          label: this.$t(
            "ui.data.column.monthPlanFinalAdjustQuery.productionVersion"
          ),
          type: "select",
          dictData: this.versionOptions,
          filterable: true,
          clearable: false,
          listeners: {
            change: this.handleProductionVersionChange,
          },
        },
        {
          prop: "materialCode",
          label: this.$t("ui.data.column.monthplan.oriMaterialCode"),
        },
        {
          prop: "materialDesc",
          label: this.$t("ui.data.column.scheduleAdjust.productCodeDesc"),
        },
      ];
    },
    columns() {
      /** 拖动预览变化时触发列配置更新，保证日列高亮重新渲染 */
      const dayFillDragPreview = this.dayFillDragPreview;
      void dayFillDragPreview;
      const structureTypeLabel = (v) => {
        const m = { "01": this.$t("ui.data.column.monthlyProductionPlan.structureType.cycle"), "02": this.$t("ui.data.column.monthlyProductionPlan.structureType.normal") };
        return m[v] != null ? m[v] : v || "";
      };
      const cols = [
        {
          type: "selection",
          fixed: "left",
          width: 48,
          selectable: (row) => !row.showBackground,
        },
        // {
        //   prop: "factoryCode",
        //   label: this.$t("ui.data.column.factoryCode"),
        //   width: 120,
        //   formatter: (row, column, value) => {
        //     return this.selectDictLabel(this.dict.type.biz_factory_name, value);
        //   },
        // },
        {
          prop: "structureName",
          label: this.$t("ui.data.column.finishStock.structureName"),
          width: 180,
        },
        {
          prop: "structureType",
          label: this.$t("ui.data.column.monthPlanFinalAdjustQuery.structureType"),
          width: 110,
          formatter: (row, column, value) => structureTypeLabel(value),
        },
        {
          prop: "cxMachineCode",
          label: this.$t("ui.data.column.monthPlanFinalAdjustQuery.cxMachine"),
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
          align: "left",
        },
        {
          prop: "constructionStage",
          label: this.$t("ui.data.column.monthplan.constructionStage"),
          minWidth: 100,
          align: "left",
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_construction_stage, value);
          },
        }, // 排产类型
        {
          prop: "embryoCode",
          label: this.$t("ui.data.column.monthlyProductionPlan.embryoCode"),
          width: 100,
          align: "left",
        },
        {
          prop: "mainMaterialDesc",
          label: this.$t("ui.data.column.monthlyProductionPlan.embryoDesc"),
          width: 310,
          align: "left",
        },
        {
          prop: "brand",
          label: this.$t("common.brand"),
          width: 100,
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_brand_type, value);
          },
        },
        {
          prop: "specifications",
          label: this.$t("ui.data.column.trialPlan.specifications"),
          width: 100,
        },
        {
          prop: "mainPattern",
          label: this.$t("ui.data.column.confMinProd.pattern"),
          width: 100,
        },
        {
          prop: "proSize",
          label: this.$t("ui.data.column.monthlyProductionPlan.inch"),
          width: 80,
        },
        {
          prop: "mouldCavityQty",
          label: this.$t("ui.data.monthlyProductionPlan.mouldCavityQtyNum"),
          width: 80,
        },
        {
          prop: "typeBlockQty",
          label: this.$t("ui.data.monthlyProductionPlan.typeBlockQtyNum"),
          width: 80,
        },
        {
          prop: "dayLhQty",
          label: this.$t("ui.data.monthlyProductionPlan.dayVulcanizationQty"),
          width: 100,
        },
        {
          prop: "originalTotalQty",
          label: this.$t(
            "ui.data.column.monthlyProductionPlan.originalTotalQty"
          ),
          width: 100,
        },
        {
          prop: "adjustQty1",
          label: this.$t("ui.data.column.monthlyProductionPlan.adjustQty1"),
          width: 80,
        },
        {
          prop: "adjustQty2",
          label: this.$t("ui.data.column.monthlyProductionPlan.adjustQty2"),
          width: 80,
        },
        {
          prop: "adjustQty3",
          label: this.$t("ui.data.column.monthlyProductionPlan.adjustQty3"),
          width: 80,
        },
        {
          prop: "adjustQty4",
          label: this.$t("ui.data.column.monthlyProductionPlan.adjustQty4"),
          width: 80,
        },
        {
          prop: "netQty",
          label: this.$t("ui.data.column.demandPlanSum.netQty"),
          width: 100,
          formatter: (row, column, value) =>
            value != null && value !== "" ? value : row.prodReqPlan,
        },
        {
          prop: "heightQty",
          label: this.$t("ui.data.column.demandPlanSum.heightQty"),
          width: 120,
        },
        {
          prop: "midQty",
          label: this.$t("ui.data.column.demandPlanSum.midQty"),
          width: 120,
        },
        {
          prop: "cycleReserveQty",
          label: this.$t("ui.data.column.demandPlanSum.cycleReserveQty"),
          width: 120,
        },
        {
          prop: "conventionReserveQty",
          label: this.$t("ui.data.column.demandPlanSum.conventionReserveQty"),
          width: 120,
        },
        {
          prop: "totalQty",
          label: this.$t("ui.data.mouldingDayResult.totalQty"),
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
          label: this.$t(
            "ui.data.column.monthPlanFinalAdjustQuery.cycleReserveProductionQty"
          ),
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
          prop: "pendingQty",
          label: this.$t("ui.data.rollingCycle.pendingQty"),
          width: 120,
        },
        {
          prop: "isLockSchedule",
          label: this.$t(
            "ui.data.column.monthPlanFinalAdjustQuery.isLockSchedule"
          ),
          width: 120,
          render: ({ row }) => {
            if (this.isStatisticsRow(row)) {
              return (
                <span>
                  {this.selectDictLabel(
                    this.dict.type.biz_yes_no,
                    row.isLockSchedule
                  )}
                </span>
              );
            }
            return (
              <el-select
                v-model={row.isLockSchedule}
                size="mini"
                filterable
                clearable
                placeholder={this.$t("ui.frame.btn.choose")}
                style="width: 100%"
                onChange={() => this.handleLockScheduleChange(row)}
              >
                {this.dict.type.biz_yes_no.map((item) => (
                  <el-option
                    key={item.value}
                    label={item.label}
                    value={item.value}
                  />
                ))}
              </el-select>
            );
          },
        },
        {
          prop: "beginDay",
          label: this.$t("common.startDate"),
          width: 90,
        },
        {
          prop: "endDay",
          label: this.$t("common.endDate"),
          width: 90,
        },
      ];
      for (let i = 1; i <= 31; i++) {
        const prop = `day${i}`;
        cols.push({
          label: `${i}${this.$t("ui.data.column.monthlyProductionPlan.day")}`,
          prop,
          minWidth: "72px",
          render: ({ row }) => {
            const text =
              row[prop] === null || row[prop] === undefined
                ? ""
                : String(row[prop]);
            if (!this.canEditDayCell(row, i)) {
              const isOver = row._overDays && row._overDays[prop];
              return <span style={isOver ? { color: "red" } : {}}>{text}</span>;
            }
            const fillVisual = this.getDayFillDragVisual(row, i);
            const isActive = this.isDayCellActive(row, i) && !fillVisual;
            const showFillHandle = this.canShowDayFillHandle(row, i);
            return (
              <div
                class={[
                  "day-cell-wrap",
                  isActive ? "day-cell-wrap--active" : "",
                  fillVisual ? "day-cell-wrap--fill-preview" : "",
                  fillVisual && fillVisual.isSource
                    ? "day-cell-wrap--fill-source"
                    : "",
                  fillVisual && fillVisual.isTarget
                    ? "day-cell-wrap--fill-target"
                    : "",
                ]}
                attrs={{
                  "data-day-cell-row-key": this.getDayCellRowKey(row),
                  "data-day-cell-day": String(i),
                }}
                onClick={(e) => this.handleDayCellClick(row, i, e)}
                onMousedown={(e) => this.handleDayCellMouseDown(e)}
              >
                <el-input
                  size="mini"
                  value={text}
                  nativeOnMousedown={(e) => e.stopPropagation()}
                  onInput={(value) => {
                    const n = String(value).replace(/[^\d]/g, "");
                    row[prop] = n === "" ? null : Number(n);
                  }}
                  onFocus={() => {
                    this.setDayCellActive(row, i);
                    this.onDayEditFocus(row, prop);
                  }}
                  onBlur={() => this.handleResultDayEdit(row, prop)}
                />
                {showFillHandle ? (
                  <span
                    class="day-cell-fill-handle"
                    title={this.getDayFillHandleTitle(row, i)}
                    on={{
                      mousedown: (e) => {
                        e.stopPropagation();
                        e.preventDefault();
                        if (e.detail >= 2) {
                          this.cleanupDayFillDrag(true);
                          this.suppressDayEditBlurSave = false;
                          this.setDayCellActive(row, i);
                          this.handleDayFillDown(row, i);
                          return;
                        }
                        this.suppressDayEditBlurSave = true;
                        this.armDayFillPointer(row, i, e);
                      },
                      click: (e) => e.stopPropagation(),
                    }}
                  />
                ) : null}
              </div>
            );
          },
        });
      }
      cols.push(
        {
          prop: "lastMonthOverdueQty",
          label: this.$t(
            "ui.data.column.mpAdjustResult.lastMonthOverdueQty"
          ),
          width: 140,
          render: ({ row }) => {
            if (this.isStatisticsRow(row)) {
              return (
                <span>
                  {this.selectDictLabel(
                    this.dict.type.biz_yes_no,
                    row.lastMonthOverdueQty
                  )}
                </span>
              );
            }
            return (
              <el-select
                v-model={row.lastMonthOverdueQty}
                size="mini"
                filterable
                clearable
                placeholder={this.$t("ui.frame.btn.choose")}
                style="width: 100%"
                onChange={() => this.handleLockScheduleChange(row)}
              >
                {this.dict.type.biz_yes_no.map((item) => (
                  <el-option
                    key={item.value}
                    label={item.label}
                    value={item.value}
                  />
                ))}
              </el-select>
            );
          },
        },
        {
          prop: "lastMonthValidFlag",
          label: this.$t("ui.data.column.mpAdjustResult.lastMonthValidFlag"),
          width: 120,
          formatter: (row, column, value) => {
            return value === null || value === undefined ? "" : String(value);
          },
        },
        {
          prop: "hasSpecialMaterial",
          label: this.$t("ui.data.column.mpAdjustResult.hasSpecialMaterial"),
          width: 120,
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_yes_no, value);
          },
        }
      );
      return cols;
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
    syncDialogRules() {
      const t = (k) => this.$t(k);
      return {
        yearMonth: [
          {
            required: true,
            message: t(
              "ui.data.column.monthPlanFinalAdjustQuery.issueScmMesRuleYearMonth"
            ),
            trigger: "change",
          },
        ],
        factoryCode: [
          {
            required: true,
            message: t(
              "ui.data.column.monthPlanFinalAdjustQuery.issueScmMesRuleFactory"
            ),
            trigger: "change",
          },
        ],
        productionVersion: [
          {
            required: true,
            message: t(
              "ui.data.column.monthPlanFinalAdjustQuery.issueScmMesRuleProductionVersion"
            ),
            trigger: "change",
          },
        ],
        lastMonthPlanVersion: [
          {
            required: true,
            message: t(
              "ui.data.column.monthPlanFinalAdjustQuery.issueScmMesRuleDemandVersion"
            ),
            trigger: "change",
          },
        ],
      };
    },
  },
  mounted() {
    document.addEventListener("click", this.handleDocumentClickClearDayCell);
    this._boundDayFillDragMove = this.handleDayFillDragMove.bind(this);
    this._boundDayFillDragEnd = this.handleDayFillDragEnd.bind(this);
    this._boundDayCellKeydown = this.handleDayCellDeleteKey.bind(this);
    document.addEventListener("keydown", this._boundDayCellKeydown, true);
  },
  beforeDestroy() {
    document.removeEventListener("click", this.handleDocumentClickClearDayCell);
    document.removeEventListener("keydown", this._boundDayCellKeydown, true);
    this.cleanupDayFillDrag(true);
  },
  async created() {
    /** 与月度生产计划旧页 index.backup-legacy.vue 一致：默认查询「下个月」 */
    const now = new Date();
    const nextMonth = new Date(now.getFullYear(), now.getMonth() + 1, 1);
    const year = nextMonth.getFullYear();
    const month = nextMonth.getMonth();
    const defaults = {
      factoryCode: "116",
      yearMonth: `${year}-${month}`,
    };
    const rq = this.$route.query || {};
    if (rq.factoryCode) {
      defaults.factoryCode = String(rq.factoryCode);
    }
    if (rq.yearMonth) {
      defaults.yearMonth = String(rq.yearMonth);
    }
    /** 兼容旧链接 productionVersion；与 version 同时存在时以 version 为准 */
    if (rq.productionVersion) {
      defaults.version = String(rq.productionVersion);
    }
    if (rq.structureName) {
      defaults.structureName = String(rq.structureName);
    }
    /** 调整版本号（ADJ…），随路由带入 */
    if (rq.version) {
      defaults.version = String(rq.version);
    }
    this.search = { ...defaults };
    this.query = { ...defaults };
    await this.loadVersionOptions();
    await this.fetchCurrentAdjustMachineFromRedis();
    await this.fetchLockedDays();
    this.getList();
  },
  methods: {
    async handleBaseQueryChange() {
      const pt = this.$refs.monthPlanPageTableRef;
      const searchRef = pt && pt.$refs && pt.$refs.searchRef;
      if (searchRef && typeof searchRef.getValues === "function") {
        const vals = searchRef.getValues();
        this.search = { ...this.search, ...vals };
        this.query = { ...this.query, ...vals };
      }
      await this.loadVersionOptions();
      this.fetchCurrentAdjustMachineFromRedis();
      await this.fetchLockedDays();
    },
    handleProductionVersionChange() {
      this.fetchCurrentAdjustMachineFromRedis();
    },
    /**
     * 解析 getAdjustsCxMachineFromRedis 返回值（兼容字符串、AjaxResult、对象字段）
     */
    extractRedisCxMachine(payload) {
      if (payload == null || payload === "") {
        return "";
      }
      if (typeof payload === "string" || typeof payload === "number") {
        return String(payload).trim();
      }
      const inner =
        payload.data !== undefined && payload.code === undefined
          ? payload.data
          : payload;
      if (typeof inner === "string" || typeof inner === "number") {
        return String(inner).trim();
      }
      if (inner && typeof inner === "object") {
        if (inner.data != null && typeof inner.data !== "object") {
          return String(inner.data).trim();
        }
        if (inner.data && typeof inner.data === "object") {
          const nested = inner.data;
          if (nested.cxMachineCode != null && nested.cxMachineCode !== "") {
            return String(nested.cxMachineCode).trim();
          }
        }
        if (inner.cxMachineCode != null && inner.cxMachineCode !== "") {
          return String(inner.cxMachineCode).trim();
        }
        if (inner.currentAdjustMachine != null) {
          return String(inner.currentAdjustMachine).trim();
        }
        if (inner.currentCxMachine != null) {
          return String(inner.currentCxMachine).trim();
        }
      }
      return "";
    },
    /**
     * 解析 getAdjustsCxMachineFromRedis 返回值，提取所有字段
     */
    extractRedisFields(payload) {
      const empty = {
        cxMachineCode: "",
        structureName: "",
        beginDay: "",
        endDay: "",
        monthPlanVersion: "",
      };
      if (payload == null || payload === "") {
        return empty;
      }
      if (typeof payload === "string" || typeof payload === "number") {
        return { ...empty, cxMachineCode: String(payload).trim() };
      }
      const inner =
        payload.data !== undefined && payload.code === undefined
          ? payload.data
          : payload;
      if (typeof inner === "string" || typeof inner === "number") {
        return { ...empty, cxMachineCode: String(inner).trim() };
      }
      if (inner && typeof inner === "object") {
        const resolve = (obj) => ({
          cxMachineCode:
            obj.cxMachineCode != null && obj.cxMachineCode !== ""
              ? String(obj.cxMachineCode).trim()
              : obj.currentAdjustMachine != null
                ? String(obj.currentAdjustMachine).trim()
                : obj.currentCxMachine != null
                  ? String(obj.currentCxMachine).trim()
                  : "",
          structureName:
            obj.structureName != null ? String(obj.structureName).trim() : "",
          beginDay:
            obj.beginDay != null ? String(obj.beginDay).trim() : "",
          endDay:
            obj.endDay != null ? String(obj.endDay).trim() : "",
          monthPlanVersion:
            obj.monthPlanVersion != null
              ? String(obj.monthPlanVersion).trim()
              : "",
        });
        if (inner.data != null && typeof inner.data === "object") {
          return resolve(inner.data);
        }
        return resolve(inner);
      }
      return empty;
    },
    /** 从 Redis 拉取当前调整机台并回显到选择器 */
    async fetchCurrentAdjustMachineFromRedis() {
      try {
        const res = await getAdjustsCxMachineFromRedis();
        const fields = this.extractRedisFields(res);
        this.currentAdjustMachine = fields.cxMachineCode;
        this.currentAdjustStructure = fields.structureName;
        this.currentAdjustBeginDay = fields.beginDay;
        this.currentAdjustEndDay = fields.endDay;
        this.currentAdjustMonthPlanVersion = fields.monthPlanVersion;
      } catch (e) {
        console.error(e);
        this.currentAdjustMachine = "";
        this.currentAdjustStructure = "";
        this.currentAdjustBeginDay = "";
        this.currentAdjustEndDay = "";
        this.currentAdjustMonthPlanVersion = "";
      }
    },
    async fetchLockedDays() {
      const factoryCode = this.query.factoryCode || this.search.factoryCode;
      if (!factoryCode) {
        this.lockedDays = 0;
        this.weekRollAdjustDate = "";
        return;
      }
      const baseParams = {
        factoryCode,
        productTypeCode: "TBR",
      };
      try {
        /** 同 URL 的 POST 需间隔 ≥300ms，避免 request 防重复提交拦截 */
        const lockRes = await getByParamCode({
          ...baseParams,
          paramCode: "SYS0206001",
        });
        await new Promise((resolve) => setTimeout(resolve, 300));
        const adjustDateRes = await getByParamCode({
          ...baseParams,
          paramCode: "SYS0206006",
        });
        this.lockedDays = Number(lockRes?.paramValue) || 0;
        this.weekRollAdjustDate =
          adjustDateRes?.paramValue != null
            ? String(adjustDateRes.paramValue).trim()
            : "";
      } catch (e) {
        console.error("获取锁定天数/调整日失败:", e);
        this.lockedDays = 0;
        this.weekRollAdjustDate = "";
      }
    },
    /**
     * 解析用于周次计算的调整日：与后端 setAdjustDate 一致。
     * 调整日取自 SYS0206006，为空则用当天；若调整月不等于查询月，则视为 1 号。
     */
    resolveAdjustDayForWeekCalc() {
      const ym = this.normalizeYearMonth(
        this.query.yearMonth || this.search.yearMonth
      );
      if (!ym) {
        return 1;
      }
      let adjustDate = moment();
      if (this.weekRollAdjustDate) {
        const parsed = moment(this.weekRollAdjustDate, [
          "YYYY-MM-DD",
          "YYYY-M-D",
          "YYYY/MM/DD",
        ], true);
        if (parsed.isValid()) {
          adjustDate = parsed;
        }
      }
      if (
        adjustDate.year() !== ym.year ||
        adjustDate.month() + 1 !== ym.month
      ) {
        return 1;
      }
      return adjustDate.date();
    },
    /**
     * 根据调整日计算当前周次（第1周1-7，第2周8-14，第3周15-21，第4周22-31）
     */
    getAdjustWeekNumber(adjustDay) {
      const day = Number(adjustDay);
      const safeDay = Number.isFinite(day) && day > 0 ? day : 1;
      const baseWeek = Math.floor((safeDay - 1) / 7) + 1;
      return Math.min(baseWeek, 4);
    },
    /**
     * 按当前周次计算 adjustQty1~4：
     * 当前周调整量 = totalQty -（originalTotalQty + 前若干周 adjustQty 累计）
     * 当前周之后置 0；当前周之前保留后端原值。
     */
    calcRowAdjustQtyFields(row) {
      if (!row || this.isStatisticsRow(row)) {
        return;
      }
      const currentWeek = this.getAdjustWeekNumber(
        this.resolveAdjustDayForWeekCalc()
      );
      const originalTotalQty = Number(row.originalTotalQty) || 0;
      const totalQty = Number(row.totalQty) || 0;
      let cumulativePrevious = originalTotalQty;
      for (let week = 1; week <= 4; week++) {
        const prop = `adjustQty${week}`;
        if (week < currentWeek) {
          cumulativePrevious += Number(row[prop]) || 0;
        } else if (week === currentWeek) {
          row[prop] = totalQty - cumulativePrevious;
        } else {
          row[prop] = 0;
        }
      }
    },
    /** 批量重算列表行的 adjustQty1~4 */
    applyAdjustQtyFieldsToRows(rows) {
      (rows || []).forEach((row) => this.calcRowAdjustQtyFields(row));
    },
    /**
     * 查询月份与当前月份比较：-1 过去月，0 当月，1 未来月；无法解析时返回 null。
     */
    getQueryMonthCompareToCurrent() {
      const ym = this.normalizeYearMonth(
        this.query.yearMonth || this.search.yearMonth
      );
      if (!ym) {
        return null;
      }
      const queryYm = ym.year * 12 + ym.month;
      const now = moment();
      const currentYm = now.year() * 12 + (now.month() + 1);
      if (queryYm < currentYm) {
        return -1;
      }
      if (queryYm > currentYm) {
        return 1;
      }
      return 0;
    },
    /**
     * 全表统一：根据 SYS0206001 计算锁定截止日（与行数据、机台号无关）。
     * 仅当查询月份为「当前月」时生效。
     * lockedDays > 0：锁定 1 号至（今天 + lockedDays - 1）号；
     * lockedDays === 0：锁定当月今天之前的日期（如今天 18 号则锁定 1–17 号）。
     */
    getLockEndDay() {
      const lockDays = Number(this.lockedDays);
      if (Number.isNaN(lockDays) || lockDays < 0) {
        return 0;
      }
      const today = new Date().getDate();
      return lockDays === 0 ? today - 1 : today + lockDays - 1;
    },
    /**
     * 某日是否在锁定期内（全表同一规则）：
     * 查询月 < 当前月 → 1–31 号全部锁定；
     * 查询月 = 当前月 → 按 SYS0206001 接口规则锁定；
     * 查询月 > 当前月 → 不锁定。
     */
    isDayLocked(day) {
      const monthCompare = this.getQueryMonthCompareToCurrent();
      if (monthCompare === 1) {
        return false;
      }
      if (monthCompare === -1) {
        return day >= 1 && day <= 31;
      }
      const lockEndDay = this.getLockEndDay();
      if (lockEndDay < 1) {
        return false;
      }
      return day <= lockEndDay;
    },
    /** 统计行（胎胚种类数、硫化机台数等）不可编辑日排产 */
    isStatisticsRow(row) {
      return !!(row && row.showBackground);
    },
    /** 日排产是否显示编辑框：按查询月份与锁定日期判断，与单元格有无值、机台号、row.id 无关 */
    canEditDayCell(row, day) {
      if (!row || this.isStatisticsRow(row)) {
        return false;
      }
      return !this.isDayLocked(day);
    },
    /** 日排产失焦保存：需有持久化主键 */
    canSaveDayCell(row) {
      return !this.isStatisticsRow(row);
    },
    /**
     * 修改优先上机（原锁定上机），与 rollingCycle/index.backup-legacy.vue 一致调用 mpAdjustResult/save
     */
    handleLockScheduleChange(row) {
      const versionFromSearch = this.resolveSearchColumnsVersion();
      saveAdjustResult({
        ...row,
        version:
          versionFromSearch ||
          (row.version != null ? String(row.version).trim() : ""),
        adjustType: this.pickAdjustTypeOnlyMonthPlanConfirmRecalculate(),
      })
        .then((res) => {
          this.$modal.msgSuccess(res.msg);
        })
        .catch((err) => {
          console.error(err);
        });
    },
    /**
     * 查询区「调整版本」当前值（与 searchColumns 中 prop: version 一致）；
     * 优先取 HeaderSearch 表单，避免用户改了下拉未点查询时与 this.search 不同步。
     */
    resolveSearchColumnsVersion() {
      const pt = this.$refs.monthPlanPageTableRef;
      const searchRef = pt && pt.$refs && pt.$refs.searchRef;
      if (searchRef && typeof searchRef.getValues === "function") {
        const form = searchRef.getValues();
        if (form && form.version != null && String(form.version).trim() !== "") {
          return String(form.version).trim();
        }
      }
      return String(
        this.query.version != null
          ? this.query.version
          : this.search.version != null
            ? this.search.version
            : ""
      ).trim();
    },
    /**
     * 月计划页 mpAdjustResult/save、确认调整、重新计算 共用的 adjustType：
     * 查询区版本下拉清空 → "03"；否则为 versionOptions 对应项的 adjustType；无则 ""。
     * 以 HeaderSearch 表单 version 为准（无 ref 时退回 query.version）。
     */
    pickAdjustTypeOnlyMonthPlanConfirmRecalculate() {
      let selectedVersion = "";
      const pt = this.$refs.monthPlanPageTableRef;
      const searchRef = pt && pt.$refs && pt.$refs.searchRef;
      if (searchRef && typeof searchRef.getValues === "function") {
        const form = searchRef.getValues();
        if (form) {
          const raw = form.version;
          selectedVersion =
            raw != null && String(raw).trim() !== ""
              ? String(raw).trim()
              : "";
        }
      } else {
        selectedVersion = String(this.query.version ?? "").trim();
      }
      if (!selectedVersion) {
        return "03";
      }
      const opts = this.versionOptions || [];
      const hit = opts.find(
        (o) => o && String(o.value).trim() === selectedVersion
      );
      const at =
        hit && hit.adjustType != null ? String(hit.adjustType).trim() : "";
      return at;
    },
    /** 记录 1–31 号列编辑前的原始值 */
    onDayEditFocus(row, prop) {
      this.dayEditOriginalValue = row[prop];
    },
    /** 将日期列的值归一化：null/undefined/''/'0'/0 都视为空（与 rollingCycle 一致） */
    normalizeDayValue(val) {
      if (val == null || val === "" || val === 0 || val === "0") {
        return "";
      }
      return String(val);
    },
    /** 是否为「可视为空」的单元格（未填）；0 不算空 */
    isDayCellEmptyish(val) {
      return val == null || val === "";
    },
    /** 日排产单元格是否有值（含 0） */
    hasDayCellValue(val) {
      return !this.isDayCellEmptyish(val);
    },
    /** 日排产行唯一标识（用于单元格选中态） */
    getDayCellRowKey(row) {
      if (!row) {
        return "";
      }
      if (row.id != null && row.id !== "") {
        return String(row.id);
      }
      return [
        row.structureName || "",
        row.materialCode || "",
        row.cxMachineCode || "",
        row.embryoCode || "",
      ].join("_");
    },
    /** 清除 t-table 框选区域，避免与日排产单元格选中态冲突 */
    clearTableSelectArea() {
      const table = this.getMonthPlanTableInnerRef();
      const body = table && table.$refs && table.$refs.tableBody;
      if (body && typeof body.removeSelectArea === "function") {
        body.removeSelectArea();
      }
    },
    setDayCellActive(row, day) {
      this.clearTableSelectArea();
      this.dayCellActive = {
        rowKey: this.getDayCellRowKey(row),
        day,
      };
    },
    /** 阻止 selectArea 在 td 上拦截日排产格的按下/选中 */
    handleDayCellMouseDown(event) {
      if (event) {
        event.stopPropagation();
      }
    },
    isDayCellActive(row, day) {
      if (!this.dayCellActive) {
        return false;
      }
      return (
        this.dayCellActive.rowKey === this.getDayCellRowKey(row) &&
        this.dayCellActive.day === day
      );
    },
    /** 根据日排产行 key 在列表中定位行 */
    findRowByDayCellKey(rowKey) {
      if (!rowKey) {
        return null;
      }
      return (this.data || []).find(
        (row) => this.getDayCellRowKey(row) === rowKey
      );
    },
    /** 是否应忽略 Delete（日排产格与其它表单控件分开处理） */
    shouldIgnoreDayCellDeleteKey(target) {
      if (!target || !target.closest) {
        return false;
      }
      if (target.closest(".day-cell-wrap")) {
        return false;
      }
      const tag = target.tagName;
      if (tag === "INPUT" || tag === "TEXTAREA" || tag === "SELECT") {
        return true;
      }
      return !!target.isContentEditable;
    },
    /** 清空指定日排产格（可编辑且有值时返回 true） */
    clearDayCellValue(row, day) {
      if (!row || !this.canEditDayCell(row, day)) {
        return false;
      }
      const prop = `day${day}`;
      if (!this.hasDayCellValue(row[prop])) {
        return false;
      }
      row[prop] = null;
      return true;
    },
    /** 清空拖动范围内所有可编辑日排产格（含源格与目标格） */
    clearDayCellsInDragRange(row, startDay, endDay) {
      let changed = false;
      const from = Math.min(startDay, endDay);
      const to = Math.max(startDay, endDay);
      for (let d = from; d <= to; d++) {
        if (this.clearDayCellValue(row, d)) {
          changed = true;
        }
      }
      return changed;
    },
    /** 失焦当前日排产输入框，避免 Delete 后仍停留在逐字编辑态 */
    blurActiveDayCellInput() {
      const activeEl = document.activeElement;
      if (
        activeEl &&
        activeEl.closest &&
        activeEl.closest(".day-cell-wrap") &&
        activeEl.tagName === "INPUT"
      ) {
        activeEl.blur();
      }
    },
    /** 重新聚焦指定日排产格输入框（填充柄单击后保持可双击） */
    refocusDayCellInput(row, day) {
      this.$nextTick(() => {
        const rowKey = this.getDayCellRowKey(row);
        const selector = `.day-cell-wrap[data-day-cell-row-key="${rowKey}"][data-day-cell-day="${day}"] input`;
        const input = document.querySelector(selector);
        if (input && typeof input.focus === "function") {
          input.focus();
        }
      });
    },
    /** 将编辑基准值同步为当前聚焦日排产格的实际值，避免 blur 误判变更 */
    syncDayEditOriginalFromFocusedCell() {
      const activeEl = document.activeElement;
      if (activeEl && activeEl.closest) {
        const wrap = activeEl.closest(".day-cell-wrap[data-day-cell-day]");
        if (wrap) {
          const day = Number(wrap.getAttribute("data-day-cell-day"));
          const rowKey = wrap.getAttribute("data-day-cell-row-key") || "";
          const row = this.findRowByDayCellKey(rowKey);
          if (row && day) {
            this.dayEditOriginalValue = row[`day${day}`];
            return;
          }
        }
      }
      if (this.dayCellActive) {
        const row = this.findRowByDayCellKey(this.dayCellActive.rowKey);
        const day = this.dayCellActive.day;
        if (row) {
          this.dayEditOriginalValue = row[`day${day}`];
          return;
        }
      }
      this.dayEditOriginalValue = null;
    },
    /** 拖动结束并已 save 后，清除编辑态，避免 blur 再次触发 save */
    finishDayCellDragEditState() {
      this.syncDayEditOriginalFromFocusedCell();
      this.blurActiveDayCellInput();
      this.dayCellActive = null;
    },
    /**
     * Delete：选中格整格清空并保存；
     * 拖动填充未松手时清空源格与当前拖动范围内目标格，松手后统一 save
     */
    async handleDayCellDeleteKey(event) {
      if (!event || event.key !== "Delete") {
        return;
      }
      const session = this._dayFillDragSession;
      if (session) {
        event.preventDefault();
        event.stopPropagation();
        const { row, startDay, endDay } = session;
        if (this.clearDayCellsInDragRange(row, startDay, endDay)) {
          session.deletedDuringDrag = true;
          if (
            this.dayCellActive &&
            this.dayCellActive.rowKey === session.rowKey
          ) {
            const activeDay = this.dayCellActive.day;
            const from = Math.min(startDay, endDay);
            const to = Math.max(startDay, endDay);
            if (activeDay >= from && activeDay <= to) {
              this.dayEditOriginalValue = row[`day${activeDay}`];
            }
          }
        }
        return;
      }
      if (!this.dayCellActive) {
        if (this.shouldIgnoreDayCellDeleteKey(event.target)) {
          return;
        }
        return;
      }
      if (this.shouldIgnoreDayCellDeleteKey(event.target)) {
        return;
      }
      event.preventDefault();
      event.stopPropagation();
      const row = this.findRowByDayCellKey(this.dayCellActive.rowKey);
      const day = this.dayCellActive.day;
      if (!row || !this.canEditDayCell(row, day)) {
        return;
      }
      const prop = `day${day}`;
      const hadValue = this.hasDayCellValue(row[prop]);
      row[prop] = null;
      this.dayEditOriginalValue = null;
      this.blurActiveDayCellInput();
      if (!hadValue) {
        return;
      }
      try {
        await this.saveDayRowAdjust(row);
      } catch (err) {
        console.error(err);
      }
    },
    /** 当前格之后是否存在可编辑日期（用于显示填充柄，含拖动覆盖场景） */
    hasEditableDayAfter(row, day) {
      for (let d = day + 1; d <= 31; d++) {
        if (this.canEditDayCell(row, d)) {
          return true;
        }
      }
      return false;
    },
    /** 当前格之后是否存在可编辑且为空的日期格（连续空段，遇锁定日或有值则停止） */
    hasEditableEmptyDayAfter(row, day) {
      for (let d = day + 1; d <= 31; d++) {
        if (!this.canEditDayCell(row, d)) {
          break;
        }
        if (this.isDayCellEmptyish(row[`day${d}`])) {
          return true;
        }
        break;
      }
      return false;
    },
    /** 是否显示右下角填充柄 */
    canShowDayFillHandle(row, day) {
      if (!this.isDayCellActive(row, day)) {
        return false;
      }
      if (!this.canEditDayCell(row, day)) {
        return false;
      }
      if (!this.hasDayCellValue(row[`day${day}`])) {
        return false;
      }
      return this.hasEditableDayAfter(row, day);
    },
    /** 填充柄 hover 提示：无连续空格时不展示双击说明 */
    getDayFillHandleTitle(row, day) {
      if (this.hasEditableEmptyDayAfter(row, day)) {
        return this.$t("ui.data.column.monthlyProductionPlan.fillHandleTitle.doubleClick");
      }
      return this.$t("ui.data.column.monthlyProductionPlan.fillHandleTitle.drag");
    },
    /** 拖动填充预览视觉（由 Vue 渲染，表格 hover 重绘时仍保留） */
    getDayFillDragVisual(row, day) {
      const preview = this.dayFillDragPreview;
      if (!preview || preview.endDay <= preview.startDay) {
        return null;
      }
      if (preview.rowKey !== this.getDayCellRowKey(row)) {
        return null;
      }
      if (day < preview.startDay || day > preview.endDay) {
        return null;
      }
      return {
        isSource: day === preview.startDay,
        isTarget: day > preview.startDay,
      };
    },
    /** 从拖拽事件解析日排产单元格 */
    resolveDayCellFromDragEvent(event) {
      if (!event) {
        return null;
      }
      const target = document.elementFromPoint(event.clientX, event.clientY);
      const wrap =
        target && target.closest
          ? target.closest(".day-cell-wrap[data-day-cell-day]")
          : null;
      if (!wrap) {
        return null;
      }
      const day = Number(wrap.getAttribute("data-day-cell-day"));
      const rowKey = wrap.getAttribute("data-day-cell-row-key") || "";
      if (!day || Number.isNaN(day)) {
        return null;
      }
      return { day, rowKey };
    },
    /** 填充柄按下：仅记录指针，移动超过阈值后才开始拖动（保证双击第一下不失焦） */
    armDayFillPointer(row, startDay, event) {
      if (
        !this.canEditDayCell(row, startDay) ||
        !this.hasDayCellValue(row[`day${startDay}`]) ||
        !this.hasEditableDayAfter(row, startDay)
      ) {
        return;
      }
      this.cleanupDayFillDrag(true);
      this._dayFillDragSession = {
        row,
        rowKey: this.getDayCellRowKey(row),
        startDay,
        endDay: startDay,
        fillValue: row[`day${startDay}`],
        armed: false,
        moved: false,
        deletedDuringDrag: false,
        pendingSaveOnDragEnd: false,
        startX: event.clientX,
        startY: event.clientY,
        rafId: null,
      };
      document.addEventListener("mousemove", this._boundDayFillDragMove, {
        passive: true,
      });
      document.addEventListener("mouseup", this._boundDayFillDragEnd);
      this.refocusDayCellInput(row, startDay);
    },
    /** 指针移动超过阈值后，正式进入拖动填充 */
    activateDayFillDrag(session) {
      if (!session || session.armed) {
        return;
      }
      session.armed = true;
      session.pendingSaveOnDragEnd = this.dayCellValueChangedForSave(
        this.dayEditOriginalValue,
        session.row[`day${session.startDay}`]
      );
      this.suppressDayEditBlurSave = true;
      document.body.classList.add("day-fill-dragging");
    },
    handleDayFillDragMove(event) {
      const session = this._dayFillDragSession;
      if (!session) {
        return;
      }
      if (!session.armed) {
        const dx = Math.abs(event.clientX - session.startX);
        const dy = Math.abs(event.clientY - session.startY);
        if (dx <= 2 && dy <= 2) {
          return;
        }
        this.activateDayFillDrag(session);
      }
      session.pendingEvent = event;
      if (session.rafId != null) {
        return;
      }
      session.rafId = window.requestAnimationFrame(() => {
        session.rafId = null;
        this.syncDayFillDragPreview(session.pendingEvent);
      });
    },
    /** rAF 内同步拖动预览（仅 endDay 变化时更新 Vue 状态） */
    syncDayFillDragPreview(event) {
      const session = this._dayFillDragSession;
      if (!session || !event || !session.armed) {
        return;
      }
      const dx = Math.abs(event.clientX - session.startX);
      const dy = Math.abs(event.clientY - session.startY);
      session.moved = session.moved || dx > 2 || dy > 2;
      let endDay = session.startDay;
      const hit = this.resolveDayCellFromDragEvent(event);
      if (hit && hit.rowKey === session.rowKey && hit.day >= session.startDay) {
        endDay = hit.day;
      }
      session.endDay = endDay;
      if (!session.moved || endDay <= session.startDay) {
        if (this.dayFillDragPreview) {
          this.dayFillDragPreview = null;
        }
        return;
      }
      const preview = this.dayFillDragPreview;
      if (
        preview &&
        preview.rowKey === session.rowKey &&
        preview.startDay === session.startDay &&
        preview.endDay === endDay
      ) {
        return;
      }
      this.dayFillDragPreview = {
        rowKey: session.rowKey,
        startDay: session.startDay,
        endDay,
      };
    },
    async handleDayFillDragEnd() {
      const session = this._dayFillDragSession;
      if (!session) {
        this.cleanupDayFillDrag(true);
        return;
      }
      if (session.rafId != null) {
        window.cancelAnimationFrame(session.rafId);
        session.rafId = null;
        if (session.pendingEvent) {
          this.syncDayFillDragPreview(session.pendingEvent);
        }
      }
      const { row, startDay, endDay, moved, deletedDuringDrag, pendingSaveOnDragEnd } =
        session;
      if (!session.armed) {
        try {
          if (deletedDuringDrag) {
            await this.saveDayRowAdjust(row);
            this.dayEditOriginalValue = row[`day${startDay}`];
          }
        } catch (err) {
          console.error(err);
        }
        this.cleanupDayFillDrag(true);
        this.suppressDayEditBlurSave = false;
        this.refocusDayCellInput(row, startDay);
        return;
      }
      const didFill = moved && endDay > startDay;
      this.cleanupDayFillDrag(true);
      try {
        if (deletedDuringDrag) {
          await this.saveDayRowAdjust(row);
          this.dayEditOriginalValue = row[`day${startDay}`];
          return;
        }
        let fillChanged = false;
        if (didFill) {
          fillChanged = this.applyDayFillRangeChanges(row, startDay, endDay);
        }
        if (fillChanged || pendingSaveOnDragEnd) {
          await this.saveDayRowAdjust(row);
          if (fillChanged) {
            this.showDayFillResultMessage(row, startDay, endDay);
          }
          this.dayEditOriginalValue = row[`day${startDay}`];
        }
      } catch (err) {
        console.error(err);
      }
      if (didFill) {
        this.dayFillDragSuppressDblclickUntil = Date.now() + 400;
      }
    },
    cleanupDayFillDrag(silent) {
      const session = this._dayFillDragSession;
      if (session && session.rafId != null) {
        window.cancelAnimationFrame(session.rafId);
      }
      if (this._boundDayFillDragMove) {
        document.removeEventListener("mousemove", this._boundDayFillDragMove);
      }
      if (this._boundDayFillDragEnd) {
        document.removeEventListener("mouseup", this._boundDayFillDragEnd);
      }
      document.body.classList.remove("day-fill-dragging");
      this.dayFillDragPreview = null;
      this._dayFillDragSession = null;
      this.suppressDayEditBlurSave = false;
      if (!silent) {
        this.$forceUpdate();
      }
    },
    /** 填充结果提示（双击 / 拖动共用） */
    showDayFillResultMessage(row, startDay, endDay) {
      const total = this.sumDayRange(row, startDay, endDay);
      const dayLabel = this.$t("ui.data.column.monthlyProductionPlan.day");
      const rangeLabel =
        startDay === endDay ? `${startDay}${dayLabel}` : `${startDay}${dayLabel}-${endDay}${dayLabel}`;
      const filledLabel =
        startDay === endDay
          ? `${startDay}${dayLabel}`
          : `${startDay + 1}${dayLabel}-${endDay}${dayLabel}`;
      const filled = this.$t("ui.data.column.monthlyProductionPlan.filled");
      const totalLabel = this.$t("ui.data.column.monthlyProductionPlan.total");
      this.$message.success(
        `${filled} ${filledLabel}，${rangeLabel}${totalLabel}：${total}`
      );
    },
    /**
     * 拖动填充：覆盖 startDay+1 至 endDay 内所有可编辑日（不论原有无值）
     * @returns {boolean} 是否有单元格被改动
     */
    applyDayFillRangeChanges(row, startDay, endDay) {
      const fillValue = row[`day${startDay}`];
      let changed = false;
      for (let d = startDay + 1; d <= endDay; d++) {
        if (!this.canEditDayCell(row, d)) {
          continue;
        }
        row[`day${d}`] = fillValue;
        changed = true;
      }
      return changed;
    },
    /**
     * 拖动填充并保存（双击向下填充等场景复用）
     */
    async applyDayFillRangeOverwrite(row, startDay, endDay) {
      if (!this.applyDayFillRangeChanges(row, startDay, endDay)) {
        return;
      }
      try {
        await this.saveDayRowAdjust(row);
        this.showDayFillResultMessage(row, startDay, endDay);
      } catch (err) {
        console.error(err);
      }
    },
    handleDayCellClick(row, day, event) {
      if (event) {
        event.stopPropagation();
      }
      this.setDayCellActive(row, day);
    },
    handleDocumentClickClearDayCell(event) {
      if (!this.dayCellActive) {
        return;
      }
      const target = event && event.target;
      if (target && target.closest && target.closest(".day-cell-wrap")) {
        return;
      }
      this.dayCellActive = null;
    },
    /** 计算日排产区间内数值之和 */
    sumDayRange(row, startDay, endDay) {
      let sum = 0;
      for (let d = startDay; d <= endDay; d++) {
        const val = row[`day${d}`];
        if (this.hasDayCellValue(val)) {
          sum += Number(val);
        }
      }
      return sum;
    },
    /** 日排产整行保存（单格失焦与批量填充共用） */
    async saveDayRowAdjust(row) {
      this.recalculateBeginEndDay(row);
      const versionFromSearch = this.resolveSearchColumnsVersion();
      await saveAdjustResult({
        ...row,
        version:
          versionFromSearch ||
          (row.version != null ? String(row.version).trim() : ""),
        adjustType: this.pickAdjustTypeOnlyMonthPlanConfirmRecalculate(),
      });
      this.allocateProductionByPriority(row);
    },
    /**
     * 双击填充柄：以当前格数值填充同行后续连续空段（遇有值或不可编辑日停止）
     */
    async handleDayFillDown(row, startDay) {
      if (
        this.dayFillDragSuppressDblclickUntil &&
        Date.now() < this.dayFillDragSuppressDblclickUntil
      ) {
        return;
      }
      if (
        !this.canEditDayCell(row, startDay) ||
        !this.hasDayCellValue(row[`day${startDay}`]) ||
        !this.hasEditableEmptyDayAfter(row, startDay)
      ) {
        return;
      }
      const fillValue = row[`day${startDay}`];
      let endDay = startDay;
      for (let d = startDay + 1; d <= 31; d++) {
        if (!this.canEditDayCell(row, d)) {
          break;
        }
        if (!this.isDayCellEmptyish(row[`day${d}`])) {
          break;
        }
        row[`day${d}`] = fillValue;
        endDay = d;
      }
      if (endDay === startDay) {
        return;
      }
      try {
        await this.saveDayRowAdjust(row);
        this.dayEditOriginalValue = row[`day${startDay}`];
        this.showDayFillResultMessage(row, startDay, endDay);
      } catch (err) {
        console.error(err);
      }
    },
    /** 是否为数值 0（展示为 0） */
    isDayCellZeroish(val) {
      return val === 0 || val === "0";
    },
    /**
     * 1–31 号列失焦时是否应认为「有改动」。
     * normalizeDayValue 会把 0 与空都变成 ""，仅靠归一化比较会漏掉「删掉 0」或「空格里输入 0」等需落库的场景。
     */
    dayCellValueChangedForSave(oldVal, newVal) {
      const oldNorm = this.normalizeDayValue(oldVal);
      const newNorm = this.normalizeDayValue(newVal);
      if (oldNorm !== newNorm) {
        return true;
      }
      const crossZeroEmptyBoundary =
        (this.isDayCellZeroish(oldVal) && this.isDayCellEmptyish(newVal)) ||
        (this.isDayCellEmptyish(oldVal) && this.isDayCellZeroish(newVal));
      return crossZeroEmptyBoundary;
    },
    /** 按后端规则本地重算开始/结束日期 */
    recalculateBeginEndDay(row) {
      if (!row) {
        return;
      }
      const monthStartDay = 1;
      const monthMaxDay = 31;
      let realBeginDay = monthMaxDay + 1;
      let realEndDay = 0;
      for (let i = monthStartDay; i <= monthMaxDay; i++) {
        const dayField = `day${i}`;
        const dayVal = Number(row[dayField] || 0);
        if (dayVal !== 0) {
          if (realBeginDay > i) {
            realBeginDay = i;
          }
          if (realEndDay < i) {
            realEndDay = i;
          }
        }
      }
      row.beginDay = realBeginDay === monthMaxDay + 1 ? 0 : realBeginDay;
      row.endDay = realEndDay;
    },
    allocateProductionByPriority(row) {
      if (!row) {
        return;
      }
      let totalQty = 0;
      for (let i = 1; i <= 31; i++) {
        const val = Number(row[`day${i}`] || 0);
        totalQty += val;
      }
      row.totalQty = totalQty;
      this.calcRowAdjustQtyFields(row);

      const stageLabel = this.selectDictLabel(
        this.dict.type.biz_construction_stage,
        row.constructionStage
      );
      if (
        stageLabel &&
        (stageLabel.indexOf("试制") !== -1 || stageLabel.indexOf("量试") !== -1)
      ) {
        row.trialProductionQty = totalQty;
        row.heightProductionQty = 0;
        row.midProductionQty = 0;
        row.cycleProductionQty = 0;
        row.conventionProductionQty = 0;
        row.postponeProductionQty = 0;
        return;
      }

      row.trialProductionQty = 0;
      row.heightProductionQty = 0;
      row.midProductionQty = 0;
      row.cycleProductionQty = 0;
      row.conventionProductionQty = 0;
      row.postponeProductionQty = 0;

      if (!totalQty || totalQty <= 0) {
        return;
      }

      let remainingQty = totalQty;
      const scmPriorities = [];

      const heightQty = Number(row.heightQty || 0);
      if (heightQty > 0) {
        const allocated = Math.min(remainingQty, heightQty);
        row.heightProductionQty = allocated;
        remainingQty -= allocated;
        scmPriorities.push("height");
      }

      if (remainingQty > 0) {
        const midQty = Number(row.midQty || 0);
        if (midQty > 0) {
          const allocated = Math.min(remainingQty, midQty);
          row.midProductionQty = allocated;
          remainingQty -= allocated;
          scmPriorities.push("mid");
        }
      }

      if (remainingQty > 0) {
        const cycleReserveQty = Number(row.cycleReserveQty || 0);
        if (cycleReserveQty > 0) {
          const allocated = Math.min(remainingQty, cycleReserveQty);
          row.cycleProductionQty = allocated;
          remainingQty -= allocated;
          scmPriorities.push("cycle");
        }
      }

      const adjustPriority = Number(row.adjustPriority || 0);
      if (adjustPriority > 0 && remainingQty > 0) {
        const postponeQty = Number(row.postponeQty || 0);
        if (postponeQty > 0) {
          const allocated = Math.min(remainingQty, postponeQty);
          row.postponeProductionQty = allocated;
          remainingQty -= allocated;
          scmPriorities.push("postpone");
        }
      }

      if (remainingQty > 0) {
        const conventionReserveQty = Number(row.conventionReserveQty || 0);
        if (conventionReserveQty > 0) {
          const allocated = Math.min(remainingQty, conventionReserveQty);
          row.conventionProductionQty = allocated;
          remainingQty -= allocated;
          scmPriorities.push("convention");
        }
      }

      if (remainingQty > 0 && scmPriorities.length > 0) {
        const lastPriority = scmPriorities[scmPriorities.length - 1];
        switch (lastPriority) {
          case "convention":
            row.conventionProductionQty =
              Number(row.conventionProductionQty || 0) + remainingQty;
            break;
          case "postpone":
            row.postponeProductionQty =
              Number(row.postponeProductionQty || 0) + remainingQty;
            break;
          case "mid":
            row.midProductionQty =
              Number(row.midProductionQty || 0) + remainingQty;
            break;
          case "cycle":
            row.cycleProductionQty =
              Number(row.cycleProductionQty || 0) + remainingQty;
            break;
          default:
            row.heightProductionQty =
              Number(row.heightProductionQty || 0) + remainingQty;
            break;
        }
      }
    },
    /**
     * 修改 1–31 号日排产后实时保存，与 rollingCycle handleResultDayEdit 一致
     */
    async handleResultDayEdit(row, prop) {
      if (this.suppressDayEditBlurSave || this._dayFillDragSession) {
        return;
      }
      const dayNum = Number(String(prop).replace(/^day/, ""));
      if (!this.canSaveDayCell(row) || this.isDayLocked(dayNum)) {
        return;
      }
      if (
        !this.dayCellValueChangedForSave(this.dayEditOriginalValue, row[prop])
      ) {
        return;
      }
      try {
        await this.saveDayRowAdjust(row);
      } catch (err) {
        console.error(err);
      }
    },
    /**
     * 结构内/结构调整版本号默认值：与 structureInnerAdjust（rollingCycle structureInner）一致；
     * 列表首项为默认；isNewVersion 为 true 时强制首项；query.version 仍在列表中则保留。
     */
    applyAdjustVersionDefault(list, isNewVersion = false) {
      if (!list || list.length === 0) {
        this.search = { ...this.search, version: "" };
        this.query = { ...this.query, version: "" };
        return;
      }
      if (isNewVersion) {
        const v = list[0].value;
        this.search = { ...this.search, version: v };
        this.query = { ...this.query, version: v };
        return;
      }
      const currentPv = String(
        this.query.version || this.search.version || ""
      ).trim();
      if (currentPv) {
        const hasVersion = list.some(
          (item) => String(item.value) === currentPv
        );
        if (hasVersion) {
          this.search = { ...this.search, version: currentPv };
          this.query = { ...this.query, version: currentPv };
          return;
        }
      }
      const defaultPv = list[0].value;
      this.search = { ...this.search, version: defaultPv };
      this.query = { ...this.query, version: defaultPv };
    },
    /**
     * 版本号下拉：与 structureInnerAdjust 一致，调用 mpAdjustStructureIn/getVersionList（versionAdjust）；
     * 选项来自接口行的 version 字段；默认逻辑见 applyAdjustVersionDefault。
     */
    async loadVersionOptions(isNewVersion = false) {
      const factoryCode = this.query.factoryCode || this.search.factoryCode;
      const yearMonth = this.query.yearMonth || this.search.yearMonth;
      if (!factoryCode || !yearMonth) {
        this.versionOptions = [];
        this.monthPlanMpAdjustVersionRawRows = [];
        this.search = { ...this.search, version: "" };
        this.query = { ...this.query, version: "" };
        return;
      }
      try {
        const res = await versionAdjust(
          this.formatParamsForStructureVersionList()
        );
        const rows = res.rows || [];
        this.monthPlanMpAdjustVersionRawRows = rows;
        const list = [];
        for (let i = 0; i < rows.length; i++) {
          const pv = rows[i].version;
          if (pv == null || String(pv).trim() === "") {
            continue;
          }
          const adj =
            rows[i].adjustType != null
              ? String(rows[i].adjustType).trim()
              : "";
          list.push({
            label: pv,
            value: pv,
            adjustType: adj,
          });
        }
        this.versionOptions = list;
        if (list.length > 0) {
          this.applyAdjustVersionDefault(list, isNewVersion);
        } else {
          this.search = { ...this.search, version: "" };
          this.query = { ...this.query, version: "" };
        }
      } catch (e) {
        console.error(e);
        this.versionOptions = [];
        this.monthPlanMpAdjustVersionRawRows = [];
        this.search = { ...this.search, version: "" };
        this.query = { ...this.query, version: "" };
      }
    },
    normalizeYearMonth(yearMonth) {
      if (!yearMonth) {
        return null;
      }
      const m = moment(yearMonth, ["YYYY-MM", "YYYY-M"], true);
      if (!m.isValid()) {
        return null;
      }
      return { year: m.year(), month: m.month() + 1 };
    },
    async handleSearch(data) {
      /** 与 query 一并维护 search，保证 HeaderSearch defaultValue 含表单中的 version */
      this.search = { ...this.search, ...data };
      this.query = { ...data };
      this.$set(this.page, "current", 1);
      await this.loadVersionOptions();
      await this.fetchCurrentAdjustMachineFromRedis();
      await this.fetchLockedDays();
      this.getList();
    },
    handleSortChange({ column, prop, order }) {
      if (order) {
        this.sort = {
          orderByColumn: prop,
          isAsc: order === "ascending" ? "asc" : "desc",
        };
      } else {
        this.sort = {};
      }
      this.getList();
    },
    handlePageChange(current, pageSize) {
      this.$set(this.page, "current", current);
      this.$set(this.page, "pageSize", pageSize);
      this.getList();
    },
    /**
     * 排产明细导出接口 FactoryMonthPlanMouldDayResult 查询条件字段名为 productionVersion；
     * 本页 query 统一使用 version，导出前将 version 写入 productionVersion 并移除 version。
     * list4Adjust / getVersionList 等直接使用 version（与后端 FactoryMonthPlanProductionFinalResult 一致）。
     * @param {Object} params 即将请求的参数（就地修改）
     */
    applyProductionVersionAlias(params) {
      if (!params || typeof params !== "object") {
        return params;
      }
      const v =
        params.version != null ? String(params.version).trim() : "";
      if (v) {
        params.productionVersion = v;
      }
      delete params.version;
      return params;
    },
    /** 列表 list4Adjust 入参（直接传 version，与后端 list4Adjust 优先使用调整版本号一致） */
    formatParams() {
      const params = {
        ...this.query,
        ...this.sort,
      };
      params.pageSize = this.page.pageSize;
      params.pageNum = this.page.current;
      if (params.yearMonth) {
        const arr = params.yearMonth.split("-");
        params.year = Number(arr[0]);
        params.month = Number(arr[1]);
        delete params.yearMonth;
      }
      return params;
    },
    /**
     * 版本号下拉数据请求入参：与 structureInnerAdjust（rollingCycle 结构内）getVersionList 一致，
     * 调用 /monthplan/mpAdjustStructureIn/getVersionList，year/month 拆分后清空 yearMonth。
     */
    formatParamsForStructureVersionList() {
      const params = {
        ...this.query,
        ...this.sort,
      };
      if (params.yearMonth) {
        const arr = String(params.yearMonth).split("-");
        params.year = arr[0];
        params.month = arr[1];
        params.yearMonth = "";
      }
      return params;
    },
    /** 获取调整订单：与 structureInnerAdjust 页 adjustOrder 一致 */
    async handleGetAdjustOrder() {
      this.loadText = this.$t("正在获取订单中");
      this.getAdjustOrderLoading = true;
      this.loading = true;
      try {
        const params = {
          ...this.query,
          ...this.sort,
        };
        if (params.yearMonth) {
          const arr = params.yearMonth.split("-");
          params.mpYear = arr[0];
          params.mpMonth = arr[1];
          params.yearMonth = "";
        }
        params.adjustType = "01";
        await getAdjustDetailList(params);
        await this.loadVersionOptions(true);
        await this.getList();
      } catch (e) {
        console.error(e);
      } finally {
        this.getAdjustOrderLoading = false;
        this.loading = false;
        this.loadText = this.$t("newPage.message.loadingShort");
      }
    },
    /**
     * 排产明细导出入参：与 monthPlanManagement/mouldingDayResult 一致（不含分页），
     * 请求 /monthplan/factoryMonthPlanMouldDayResult/export、exportAllMaterial。
     * 另附带本页「当前调整机台」cxMachineCode（与列表 list4Adjust 一致）。
     */
    formatMouldingDayExportParams() {
      const params = {
        ...this.query,
        ...this.sort,
      };
      if (params.yearMonth) {
        const arr = String(params.yearMonth).split("-");
        params.year = arr[0];
        params.month = arr[1];
        delete params.yearMonth;
      }
      if (params.createTime && params.createTime[0]) {
        params.createTimeStart = params.createTime[0];
        params.createTimeEnd = params.createTime[1];
        delete params.createTime;
      }
      const scheduled = (this.currentAdjustMachine || "").trim();
      if (scheduled) {
        params.cxMachineCode = scheduled;
      } else {
        delete params.cxMachineCode;
      }
      return this.applyProductionVersionAlias(params);
    },
    async getList() {
      const ym = this.normalizeYearMonth(this.query.yearMonth);
      if (!ym || !this.query.factoryCode) {
        return;
      }
      try {
        this.loading = true;
        /** list4Adjust：请求体携带 version（调整版本号），与后端 condition.version 一致 */
        const listParams = { ...this.formatParams() };
        const res = await listMonthPlanFinal4Adjust(listParams);
        const rawRows = res.rows || [];
        this.page.total = res.total || 0;
        await this.applyAdjustmentStatisticsRows(rawRows);
      } catch (e) {
        console.error(e);
        this.data = [];
      } finally {
        this.loading = false;
        this.dayCellActive = null;
        this.cleanupDayFillDrag(true);
        this.$nextTick(() => {
          this.clearStructureAdjustSelection();
        });
      }
    },
    /**
     * 成型机台字段可能为多个机台逗号分隔，取第一个用于结构调整跳转（如 H1503,H1201 → H1503）
     */
    extractFirstCxMachineCode(val) {
      if (val == null || val === "") {
        return "";
      }
      const s = String(val).trim();
      if (!s) {
        return "";
      }
      const parts = s
        .split(/[,，]/)
        .map((x) => String(x).trim())
        .filter((x) => x);
      return parts.length ? parts[0] : "";
    },
    /** 获取 PageTable 内层表格实例 */
    getMonthPlanTableInnerRef() {
      const pt = this.$refs.monthPlanPageTableRef;
      return pt && typeof pt.getTableRef === "function"
        ? pt.getTableRef()
        : null;
    },
    clearStructureAdjustSelection() {
      this.structureAdjustSelection = [];
      const inner = this.getMonthPlanTableInnerRef();
      if (inner && typeof inner.clearSelection === "function") {
        inner.clearSelection();
      }
    },
    /** 结构调整：支持多选；勾选多于 1 条时「结构调整」按钮禁用 */
    handleStructureAdjustSelectionChange(rows) {
      this.structureAdjustSelection = rows || [];
    },
    /**
     * 与 rollingCycle/index.backup-legacy.vue getStatisticsResult(data) 一致：statistics 的 productionVersion 来自「列表首行」的 data.productionVersion。
     * 月计划 list4Adjust 若行上无该字段，再退 query/search、再退定稿版本下拉第一项。
     */
    resolveProductionVersionForStatistics(firstRow) {
      if (
        firstRow &&
        firstRow.productionVersion != null &&
        String(firstRow.productionVersion).trim() !== ""
      ) {
        return String(firstRow.productionVersion).trim();
      }
      const q = String(this.query.version || this.search.version || "").trim();
      if (q) {
        return q;
      }
      const opts = this.versionOptions || [];
      if (
        opts.length &&
        opts[0].value != null &&
        String(opts[0].value).trim() !== ""
      ) {
        return String(opts[0].value).trim();
      }
      return "";
    },
    /**
     * 与周程滚动「调整结果」一致：按结构分组末尾插入胎胚种类数、硫化机台数两行（接口 statisticsResult）
     */
    async applyAdjustmentStatisticsRows(resultList) {
      if (!resultList.length) {
        this.data = [];
        return;
      }
      this.applyAdjustQtyFieldsToRows(resultList);
      const first = resultList[0];
      const ym = this.normalizeYearMonth(this.query.yearMonth);
      const productionVersion = this.resolveProductionVersionForStatistics(first);
      const params = {
        factoryCode: first.factoryCode || this.query.factoryCode,
        year:
          first.year != null && first.year !== ""
            ? Number(first.year)
            : ym.year,
        month:
          first.month != null && first.month !== ""
            ? Number(first.month)
            : ym.month,
        productionVersion,
        isFinalAdjust: 1,
      };
      if (!params.productionVersion) {
        this.data = [...resultList];
        return;
      }
      try {
        const res = await statisticsResult(params);
        this.data = this.insertStatisticsRowsAfterEachStructure(
          resultList,
          res.rows || []
        );
      } catch (e) {
        console.error(e);
        this.data = [...resultList];
      }
    },
    /**
     * 在每个 structureName 分组最后一行之后插入统计行（与 rollingCycle 调整结果 insertDataAfterEachName 逻辑一致）
     */
    insertStatisticsRowsAfterEachStructure(arr, statistList) {
      if (!arr.length) {
        return [];
      }
      const result = [];
      for (let i = 0; i < arr.length; i++) {
        const current = arr[i];
        const next = arr[i + 1];
        result.push(current);
        if (!next || next.structureName !== current.structureName) {
          const curName = String(current.structureName || "").trim();
          for (let s = 0; s < statistList.length; s++) {
            if (
              String(statistList[s].structureName || "").trim() === curName
            ) {
              const embryoCount = {
                structureName: current.structureName,
                showBackground: "light-green",
                materialCode: this.$t("ui.data.column.mouldingDayResult.embryoCount"),
              };
              const lhMachines = {
                structureName: current.structureName,
                showBackground: "light-blue",
                materialCode: this.$t("ui.data.column.mouldingDayResult.lhMachineCount"),
                _overDays: {},
              };
              for (let j = 1; j <= 31; j++) {
                const key = `day${j}`;
                if (statistList[s][key]) {
                  const dayData = JSON.parse(statistList[s][key]);
                  embryoCount[key] = dayData.embryoCount;
                  lhMachines[key] = dayData.lhMachines;
                  if (
                    dayData.lhMachines != null &&
                    dayData.maxLhMachines != null &&
                    Number(dayData.lhMachines) > Number(dayData.maxLhMachines)
                  ) {
                    lhMachines._overDays[key] = true;
                  }
                }
              }
              result.push(embryoCount);
              result.push(lhMachines);
            }
          }
        }
      }
      return result;
    },
    /** 统计行 / 调整标记行背景色（与 rollingCycle 结构内调整「调整结果」tableRowClassName 一致） */
    tableRowClassName({ row }) {
      if (row.showBackground) {
        return row.showBackground;
      }
      if (row.adjustFlag == 1) {
        return "warning-row";
      }
      return "";
    },
    /** 跳转月计划结构内调整页（仅同步年月，与当前查询条件一致） */
    handleStructureInnerAdjust() {
      const ymRaw = this.query.yearMonth || this.search.yearMonth;
      const ym = this.formatYearMonthForPicker(ymRaw);
      const query = { pageType: "inner" };
      if (ym) {
        query.yearMonth = ym;
      }
      this.$router.push({
        path: "/newPage/monthPlanStructureInnerAdjust",
        query,
      });
    },
    /** 结构调整弹窗保存新增结构并写入 Redis 后，同步主页面机台与列表 */
    onStructureAdjustSaved() {
      this.fetchCurrentAdjustMachineFromRedis();
      this.getList();
    },
    /** 计划停机：清空 Redis 调整上下文后刷新主页面展示（仅在此处调用 set，避免与弹窗重复） */
    async onPlanDowntimeApplied() {
      try {
        const res = await setAdjustsCxMachineFromRedis({
          cxMachineCode: "",
          structureName: "",
          beginDay: null,
          endDay: null,
          version: "",
        });
        if (res && res.msg) {
          this.$modal.msgSuccess(res.msg);
        }
        await this.fetchCurrentAdjustMachineFromRedis();
      } catch (e) {
        console.error(e);
      }
    },
    /**
     * 与周程滚动「结构调整」listAdjusts 入参对齐：productionVersion + version + adjVersion（列表首行 version 一般为调整版本 ADJ…）
     */
    buildStructureDialogListVersionParams() {
      const row = this.data && this.data.length ? this.data[0] : null;
      const qpv = (
        this.query.version ||
        this.search.version ||
        ""
      ).trim();
      const rowPv =
        row &&
        row.productionVersion != null &&
        String(row.productionVersion).trim() !== ""
          ? String(row.productionVersion).trim()
          : "";
      const productionVersion = qpv || rowPv;
      const adj =
        row && row.version != null && String(row.version).trim() !== ""
          ? String(row.version).trim()
          : "";
      const listAdjustsVersion = adj || productionVersion;
      const listAdjustsAdjVersion = adj || productionVersion;
      return {
        productionVersion,
        listAdjustsVersion,
        listAdjustsAdjVersion,
      };
    },
    handleStructureAdjust() {
      const ym = this.query.yearMonth || this.search.yearMonth;
      const fc = this.query.factoryCode || this.search.factoryCode || "116";
      if (!ym) {
        this.$modal.msgWarning(
          this.$t(
            "ui.data.column.monthPlanFinalAdjustQuery.pleaseSelectYearMonth"
          )
        );
        return;
      }
      const sel = this.structureAdjustSelection || [];
      if (sel.length > 1) {
        return;
      }
      const payload = {
        factoryCode: fc,
        yearMonth: ym,
        ...this.buildStructureDialogListVersionParams(),
      };
      /** 仅当勾选一行且该机台有值时预填；未勾选或无机台则打开弹窗自行选机台 */
      if (sel.length === 1) {
        const firstMachine = this.extractFirstCxMachineCode(
          sel[0].cxMachineCode
        );
        if (firstMachine) {
          payload.prefillCxMachineCode = firstMachine;
        }
      }
      if (!payload.prefillCxMachineCode) {
        const redisMachine = (this.currentAdjustMachine || "").trim();
        if (redisMachine) {
          payload.prefillCxMachineCode = this.extractFirstCxMachineCode(redisMachine);
        }
      }
      this.$refs.structureAdjustDialogRef.show(payload);
    },
    handleViewAdjustVersion() {
      const q = {};
      const fc = this.query.factoryCode || this.search.factoryCode;
      if (fc) {
        q.factoryCode = fc;
      }
      const ym = this.normalizeYearMonth(
        this.query.yearMonth || this.search.yearMonth
      );
      if (ym) {
        q.year = ym.year;
        q.month = ym.month;
      }
      this.$refs.adjustVersionDialogRef.show(q);
    },
    /**
     * 查看调整版本弹窗中点击版本号：同步到查询条件 version 并拉列表
     */
    async onAdjustVersionDialogSelectProductionVersion(productionVersion) {
      const v =
        productionVersion != null ? String(productionVersion).trim() : "";
      if (!v) {
        return;
      }
      this.search = { ...this.search, version: v };
      this.query = { ...this.query, version: v };
      const opts = this.versionOptions || [];
      if (!opts.some((item) => String(item.value) === v)) {
        const raw = (this.monthPlanMpAdjustVersionRawRows || []).find(
          (r) => r && String(r.version).trim() === v
        );
        const adj =
          raw && raw.adjustType != null
            ? String(raw.adjustType).trim()
            : "";
        this.versionOptions = [...opts, { label: v, value: v, adjustType: adj }];
      }
      this.$set(this.page, "current", 1);
      await this.fetchCurrentAdjustMachineFromRedis();
      this.getList();
    },
    handleExport() {
      downloadLink(
        "/monthplan/mpAdjustResult/exportFinal",
        this.formatMouldingDayExportParams()
      );
    },
    handleExportAllMaterial() {
      downloadLink(
        "/monthplan/mpAdjustResult/exportAllMaterial",
        this.formatMouldingDayExportParams()
      );
    },
    /** 打开下发弹窗，预填当前查询的年月、分厂，并加载可推送版本列表 */
    handleIssueScmMes() {
      this.syncDialog.visible = true;
      this.syncDialog.form.yearMonth = this.formatYearMonthForPicker(
        this.query.yearMonth || this.search.yearMonth
      );
      this.syncDialog.form.factoryCode =
        this.query.factoryCode || this.search.factoryCode || "";
      /** 月度计划版本由 loadSyncVersionList 加载后取 options 第一项，不用查询区调整版本号 */
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
    handleSyncBaseChange() {
      this.syncDialog.form.productionVersion = "";
      this.syncDialog.form.lastMonthPlanVersion = "";
      this.syncDialog.form.monthPlanVersion = "";
      this.syncDialog.versionList = [];
      this.loadSyncVersionList(false);
    },
    handleSyncProductionVersionChange() {
      this.syncDialog.form.lastMonthPlanVersion = "";
      this.syncDialog.form.monthPlanVersion = "";
    },
    handleSyncDemandVersionChange(optionKey) {
      const selectedVersion = this.syncDialog.versionList.find(
        (item) => item.optionKey === optionKey
      );
      this.syncDialog.form.monthPlanVersion = selectedVersion
        ? selectedVersion.monthPlanVersion
        : "";
    },
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
    /**
     * 可推送版本列表加载后：月度计划版本默认取 options 第一项，并联动第一项需求计划版本。
     */
    applySyncDialogDefaultVersions() {
      const productionOptions = this.syncProductionVersionOptions;
      if (!productionOptions.length) {
        this.syncDialog.form.productionVersion = "";
        this.syncDialog.form.lastMonthPlanVersion = "";
        this.syncDialog.form.monthPlanVersion = "";
        return;
      }
      this.syncDialog.form.productionVersion = productionOptions[0];
      const demandOptions = this.syncDemandVersionOptions;
      if (demandOptions.length) {
        this.syncDialog.form.lastMonthPlanVersion = demandOptions[0].optionKey;
        this.syncDialog.form.monthPlanVersion =
          demandOptions[0].monthPlanVersion || "";
      } else {
        this.syncDialog.form.lastMonthPlanVersion = "";
        this.syncDialog.form.monthPlanVersion = "";
      }
    },
    async loadSyncVersionList(showWarning) {
      const { yearMonth, factoryCode } = this.syncDialog.form;
      if (!yearMonth || !factoryCode) {
        return;
      }
      const yearMonthInfo = this.parseYearMonthFromStr(yearMonth);
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
            return (
              item.productionVersion &&
              item.monthPlanVersion &&
              item.lastMonthPlanVersion
            );
          })
          .map((item) => {
            return {
              ...item,
              optionKey: `${item.productionVersion}__${item.monthPlanVersion}__${item.lastMonthPlanVersion}`,
            };
          });
        if (this.syncDialog.versionList.length > 0) {
          this.applySyncDialogDefaultVersions();
        } else {
          this.syncDialog.form.productionVersion = "";
          this.syncDialog.form.lastMonthPlanVersion = "";
          this.syncDialog.form.monthPlanVersion = "";
        }
        if (showWarning && this.syncDialog.versionList.length === 0) {
          this.$modal.msgWarning(
            this.$t(
              "ui.data.column.monthPlanFinalAdjustQuery.issueScmMesNoData"
            )
          );
        }
      } catch (error) {
        console.error(error);
      } finally {
        this.syncDialog.versionLoading = false;
      }
    },
    submitSyncAdjustedMonthPlan() {
      this.$refs.syncForm.validate((valid) => {
        if (!valid) {
          return;
        }
        const selectedVersion = this.syncDialog.versionList.find((item) => {
          return item.optionKey === this.syncDialog.form.lastMonthPlanVersion;
        });
        if (!selectedVersion) {
          this.$modal.msgWarning(
            this.$t(
              "ui.data.column.monthPlanFinalAdjustQuery.issueScmMesNoData"
            )
          );
          return;
        }
        const yearMonthInfo = this.parseYearMonthFromStr(
          this.syncDialog.form.yearMonth
        );
        if (!yearMonthInfo) {
          this.$modal.msgWarning(
            this.$t(
              "ui.data.column.monthPlanFinalAdjustQuery.issueScmMesInvalidYearMonth"
            )
          );
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
        this.$confirm(
          this.$t(
            "ui.data.column.monthPlanFinalAdjustQuery.confirmPushAdjustedMonthPlanToScmMes"
          ),
          {
            type: "warning",
          }
        ).then(async () => {
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
    /** 将查询条件中的年月转为 yyyy-MM，供下发弹窗日期组件回显 */
    formatYearMonthForPicker(yearMonth) {
      if (!yearMonth) {
        return "";
      }
      const m = moment(yearMonth, ["YYYY-MM", "YYYY-M"], true);
      return m.isValid() ? m.format("YYYY-MM") : "";
    },
    /** 将弹窗内 yyyy-MM 解析为接口所需的 year、month */
    parseYearMonthFromStr(yearMonth) {
      if (!yearMonth) {
        return null;
      }
      const m = moment(yearMonth, ["YYYY-MM", "YYYY-M"], true);
      if (!m.isValid()) {
        return null;
      }
      return {
        year: m.year(),
        month: m.month() + 1,
      };
    },
    /**
     * 将 Redis 中的调整日字符串转为 Integer（与 MpWeekRollAdjustDTO.startDay 等一致）
     */
    parseAdjustDayToInt(val) {
      if (val == null || String(val).trim() === "") {
        return null;
      }
      const n = parseInt(String(val).trim(), 10);
      return Number.isNaN(n) ? null : n;
    },
    /**
     * 月计划调整查询页「确认调整」请求体：核心业务字段按查询区 query + Redis 上下文组装；
     * adjustType：与本页 mpAdjustResult/save、确认调整、重新计算一致，见 pickAdjustTypeOnlyMonthPlanConfirmRecalculate。
     */
    buildMonthPlanConfirmAdjustPayload() {
      const ym = this.normalizeYearMonth(this.query.yearMonth);
      if (!ym || !this.query.factoryCode) {
        return null;
      }
      const version =
        String(this.currentAdjustMonthPlanVersion || "").trim() ||
        String(this.query.version != null ? this.query.version : "").trim();
      const adjustStartDay = this.parseAdjustDayToInt(this.currentAdjustBeginDay);
      const adjustEndDay = this.parseAdjustDayToInt(this.currentAdjustEndDay);
      return {
        factoryCode: this.query.factoryCode,
        version,
        mpYear: ym.year,
        mpMonth: ym.month,
        adjustStartDay,
        adjustEndDay,
        structureName: this.currentAdjustStructure || "",
        scheduledMachines: (this.currentAdjustMachine || "").trim(),
        adjustType: this.pickAdjustTypeOnlyMonthPlanConfirmRecalculate(),
        startDay: adjustStartDay,
        endDay: adjustEndDay,
      };
    },
    /**
     * 组装「重新计算」POST /monthplan/mpWeekRollAdjust/recalculate 入参；
     * adjustType 与本页 mpAdjustResult/save、确认调整一致。
     */
    buildWeekRollConfirmPayload() {
      const params = {
        ...this.query,
        ...this.sort,
      };
      delete params.pageNum;
      delete params.pageSize;
      if (params.yearMonth) {
        const arr = params.yearMonth.split("-");
        params.mpYear = arr[0];
        params.mpMonth = arr[1];
        delete params.yearMonth;
      } else if (
        this.query.year != null &&
        this.query.month != null
      ) {
        params.mpYear = String(this.query.year);
        params.mpMonth = String(this.query.month);
      }
      const row = this.data && this.data.length ? this.data[0] : null;
      if (!row) {
        return null;
      }
      params.adjustType = this.pickAdjustTypeOnlyMonthPlanConfirmRecalculate();
      params.version = row.version || this.query.version;
      params.productionVersion =
        row.productionVersion || this.query.version;
      params.startDay = row.beginDay;
      params.endDay = row.endDay;
      params.adjustStartDay = this.parseAdjustDayToInt(
        this.currentAdjustBeginDay
      );
      params.adjustEndDay = this.parseAdjustDayToInt(
        this.currentAdjustEndDay
      );
      params.structureName =
        row.structureName || this.query.structureName;
      const sm = (this.currentAdjustMachine || "").trim();
      params.scheduledMachines = sm;
      return params;
    },
    /**
     * 「重新计算」前置校验：通过则返回 buildWeekRollConfirmPayload() 请求体，否则提示并返回 null
     * @param {"recalculate"} weekRollSubmitKind 当前仅用于区分无列表数据时的提示文案
     */
    prepareWeekRollSubmitPayloadOrWarn(weekRollSubmitKind = "recalculate") {
      if (!this.data || !this.data.length) {
        const listDataMsgKey =
          weekRollSubmitKind === "recalculate"
            ? "ui.data.column.monthPlanFinalAdjustQuery.recalculateNeedListData"
            : "ui.data.column.monthPlanFinalAdjustQuery.confirmNeedListData";
        this.$modal.msgWarning(this.$t(listDataMsgKey));
        return null;
      }
      const payload = this.buildWeekRollConfirmPayload();
      return payload;
    },
    /** 确认调整成功后的列表刷新与结构调整弹窗逻辑 */
    async afterConfirmAdjustSuccess(res) {
      this.$modal.msgSuccess(
        (res && res.msg) || this.$t("common.msg.ajax.operation.success")
      );
      /** 成功后不主动清空 Redis 中的当前调整机台，便于继续结构调整 */
      if (this.$refs.structureAdjustDialogRef) {
        this.$refs.structureAdjustDialogRef.dialogVisible = false;
      }
      await this.getList();
      await this.fetchCurrentAdjustMachineFromRedis();
      const machineTrim = (this.currentAdjustMachine || "").trim();
      if (machineTrim && this.$refs.structureAdjustDialogRef) {
        const ym = this.query.yearMonth || this.search.yearMonth;
        const fc = this.query.factoryCode || this.search.factoryCode || "116";
        if (ym) {
          this.$refs.structureAdjustDialogRef.show({
            factoryCode: fc,
            yearMonth: ym,
            ...this.buildStructureDialogListVersionParams(),
            prefillCxMachineCode: this.extractFirstCxMachineCode(
              this.currentAdjustMachine
            ),
          });
        }
      }
    },
    async handleConfirmAdjust() {
      if (!this.data || !this.data.length) {
        this.$modal.msgWarning(
          this.$t("ui.data.column.monthPlanFinalAdjustQuery.confirmNeedListData")
        );
        return;
      }
      const payload = this.buildMonthPlanConfirmAdjustPayload();
      if (!payload) {
        this.$modal.msgWarning(
          this.$t(
            "ui.data.column.monthPlanFinalAdjustQuery.pleaseSelectYearMonth"
          )
        );
        return;
      }
      this.confirmAdjustLoading = true;
      try {
        const res = await confirmAdjust(payload);
        await this.afterConfirmAdjustSuccess(res);
      } catch (e) {
        console.error(e);
      } finally {
        this.confirmAdjustLoading = false;
      }
    },
    /**
     * 重新计算：POST /monthplan/mpWeekRollAdjust。
     * 有当前调整机台且有时当前调整结构时 body 与列表组装的 confirm 入参一致；
     * 否则使用精简入参（version 优先 currentAdjustMonthPlanVersion，productionVersion/startDay/endDay 置空等）。
     */
    async handleRecalculate() {
      const payload = this.prepareWeekRollSubmitPayloadOrWarn("recalculate");
      if (!payload) {
        return;
      }
      const machineTrim = (this.currentAdjustMachine || "").trim();
      const structureTrim = (this.currentAdjustStructure || "").trim();
      /** 无当前调整机台或无当前调整结构时，重新计算接口使用精简入参 */
      let recalculatePayload = payload;
      if (!machineTrim || !structureTrim) {
        const version =
          this.currentAdjustMonthPlanVersion || this.query.version;
        /** 优先当前调整结构，否则用查询条件产品结构 structureName，再无则传空 */
        const structureNameForRecalculate =
          structureTrim ||
          String(this.query.structureName || "").trim() ||
          "";
        recalculatePayload = {
          factoryCode: this.query.factoryCode,
          version,
          mpYear: payload.mpYear,
          mpMonth: payload.mpMonth,
          adjustType: payload.adjustType,
          productionVersion: "",
          startDay: "",
          endDay: "",
          adjustStartDay: this.parseAdjustDayToInt(
            this.currentAdjustBeginDay
          ),
          adjustEndDay: this.parseAdjustDayToInt(
            this.currentAdjustEndDay
          ),
          structureName: structureNameForRecalculate,
          scheduledMachines: machineTrim,
        };
      }
      this.recalculateLoading = true;
      try {
        const res = await recalculateWeekRollAdjust(recalculatePayload);
        this.$modal.msgSuccess(
          (res && res.msg) ||
            this.$t("common.msg.ajax.operation.success")
        );
        await this.getList();
        await this.fetchCurrentAdjustMachineFromRedis();
      } catch (e) {
        console.error(e);
      } finally {
        this.recalculateLoading = false;
      }
    },
  },
};
</script>

<style lang="scss" scoped>
.toolbar-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}
.current-machine-wrap {
  display: inline-flex;
  align-items: center;
  margin-left: 12px;
  margin-bottom: 5px;
}
.current-machine-label {
  margin-right: 8px;
  font-size: 14px;
  color: #606266;
  white-space: nowrap;
}
.current-machine-input {
  // width: 220px;
}
.footer-actions {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 12px;
  padding: 8px 0 4px;
}
/* 与 rollingCycle/index.vue「调整结果」表格行颜色一致（含固定列） */
::v-deep .el-table__fixed,
::v-deep .el-table__fixed-right {
  background-color: #fff;
}
.el-table__fixed-body-wrapper .light-green > td,
.el-table__fixed-right-body-wrapper .light-green > td {
  background-color: #e2efda !important;
}
.el-table__fixed-body-wrapper .light-blue > td,
.el-table__fixed-right-body-wrapper .light-blue > td {
  background-color: #9bc2e6 !important;
}
.el-table__fixed-body-wrapper .warning-row > td,
.el-table__fixed-right-body-wrapper .warning-row > td {
  background-color: #ffcccc !important;
}
::v-deep .light-green {
  background-color: #e2efda !important;
}
::v-deep .light-blue {
  background-color: #9bc2e6 !important;
}
::v-deep .warning-row {
  background-color: #ffcccc !important;
}

/* 日排产单元格：单击选中与 Excel 式填充柄 */
::v-deep .day-cell-wrap {
  position: relative;
  width: 100%;
  min-height: 28px;
  box-sizing: border-box;
}
::v-deep .day-cell-wrap--active {
  outline: 2px solid #409eff;
  outline-offset: -2px;
  background-color: #ecf5ff;
}

/* 拖动填充：整体范围极淡底色（可选） */
::v-deep .day-cell-wrap--fill-preview {
  z-index: 4;
  background-color: #f7fbff !important;
  transition: background-color 0.08s ease;
}

/* 拖动源格：与单击选中一致，不加深色 */
::v-deep .day-cell-wrap--fill-source {
  z-index: 5;
  outline: 2px solid #409eff;
  outline-offset: -2px;
  background-color: #ecf5ff !important;
  transition: background-color 0.08s ease, outline-color 0.08s ease;
}
::v-deep .day-cell-wrap--fill-source .el-input__inner {
  background: transparent !important;
}

/* 拖动目标格：亮蓝虚线框，极淡底 */
::v-deep .day-cell-wrap--fill-target {
  z-index: 5;
  background-color: #f0f7ff !important;
  transition: background-color 0.08s ease;
}
::v-deep .day-cell-wrap--fill-target::after {
  content: "";
  position: absolute;
  inset: 2px;
  border: 2px dashed #409eff;
  border-radius: 1px;
  pointer-events: none;
  z-index: 1;
  opacity: 1;
  transition: opacity 0.08s ease;
}
::v-deep .day-cell-wrap--fill-target .el-input__inner {
  background: transparent !important;
}

::v-deep .day-cell-fill-handle {
  position: absolute;
  right: 0;
  bottom: 0;
  width: 10px;
  height: 10px;
  background-color: #409eff;
  border: 1px solid #fff;
  cursor: crosshair;
  z-index: 8;
  box-sizing: border-box;
}
::v-deep .day-cell-fill-handle::before {
  content: "";
  position: absolute;
  right: -4px;
  bottom: -4px;
  width: 16px;
  height: 16px;
}
::v-deep .day-cell-fill-handle:hover {
  background-color: #337ecc;
}
</style>

<style lang="scss">
/* 日排产拖动填充：全局光标，仅拖拽时生效 */
body.day-fill-dragging {
  cursor: crosshair !important;
  user-select: none;

  .el-table .cell {
    overflow: visible;
  }

  .day-cell-wrap {
    transition: none;
  }
}
</style>
