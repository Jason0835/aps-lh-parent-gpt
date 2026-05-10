<template>
  <basic-container>
    <page-table
      tableRef="MonthPlanFinalAdjustQueryTable"
      :calcHeight="88"
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
      :showSummary="false"
      :selectArea="false"
      :row-class-name="tableRowClassName"
    >
      <template slot="header">
        <div class="toolbar-row">
          <el-button
            type="primary"
            plain
            :disabled="!canUsePrimaryAdjustActions"
            @click="handleStructureInnerAdjust"
            >{{
              $t("ui.data.column.monthPlanFinalAdjustQuery.structureInnerAdjust")
            }}</el-button
          >
          <el-button
            type="primary"
            plain
            :disabled="!canUsePrimaryAdjustActions"
            @click="handleStructureAdjust"
            >{{
              $t("ui.data.column.monthPlanFinalAdjustQuery.structureAdjust")
            }}</el-button
          >
          <el-button @click="handleViewAdjustVersion">{{
            $t("ui.data.column.monthPlanFinalAdjustQuery.viewAdjustVersion")
          }}</el-button>
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
        </div>
      </template>
      <template slot="footer">
        <div class="footer-actions">
          <el-button
            type="primary"
            :loading="confirmAdjustLoading"
            :disabled="
              !canUsePrimaryAdjustActions ||
              confirmAdjustLoading ||
              recalculateLoading
            "
            @click="handleConfirmAdjust"
            >{{
              $t("ui.data.column.monthPlanFinalAdjustQuery.confirmAdjust")
            }}</el-button
          >
          <el-button
            :disabled="
              !canUseContinueAdjust ||
              confirmAdjustLoading ||
              recalculateLoading
            "
            @click="handleContinueAdjust"
            >{{
              $t("ui.data.column.monthPlanFinalAdjustQuery.continueAdjust")
            }}</el-button
          >
          <el-button
            :loading="recalculateLoading"
            :disabled="
              confirmAdjustLoading ||
              recalculateLoading
            "
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
        label-width="120px"
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
        <el-form-item :label="$t('common.factory')" prop="factoryCode">
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
          @click="submitSyncAdjustedMonthPlan"
          >{{
            $t("ui.data.column.monthPlanFinalAdjustQuery.issueScmMesOk")
          }}</el-button
        >
      </span>
    </el-dialog>
  </basic-container>
</template>

<script>
import moment from "moment";
import { mapGetters } from "vuex";
import { downloadLink } from "@/utils/request";
import {
  listMonthPlanFinal4Adjust,
  getFinalResultVersionList,
  syncAdjustedMonthPlanToScmAndMes,
} from "@/api/monthplan/monthlyProductionPlan";
import {
  getAdjustsCxMachineFromRedis,
  confirmAdjust,
  recalculateWeekRollAdjust,
  statisticsResult,
  saveAdjustResult,
  resultVersion
} from "@/api/monthplan/adjustStructure";
import structureAdjustDialog from "./components/structureAdjustDialog.vue";
import adjustVersionDialog from "./components/adjustVersionDialog.vue";

export default {
  name: "MonthPlanFinalAdjustQuery",
  components: {
    structureAdjustDialog,
    adjustVersionDialog,
  },
  /** 含成型机台选择弹窗所需字典（与 formingCapacitySelect / 周程滚动页一致） */
  dicts: [
    "biz_factory_name",
    "biz_brand_type",
    "biz_yes_no",
    "biz_class_type",
    "biz_machine_brand",
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
      page: {
        current: 1,
        pageSize: 20,
        total: 0,
      },
      sort: {},
      search: {},
      query: {},
      versionOptions: [],
      /** 当前调整机台（只读，来自 Redis） */
      currentAdjustMachine: "",
      confirmAdjustLoading: false,
      recalculateLoading: false,
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
    searchColumns() {
      return [
        {
          prop: "factoryCode",
          label: this.$t("common.factory"),
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
          prop: "cxMachineCode",
          label: this.$t("ui.data.column.monthPlanFinalAdjustQuery.cxMachine"),
          filterable: true,
        },
        {
          prop: "structureName",
          label: this.$t("ui.data.column.finishStock.structureName"),
          type: "select",
          dictData: this.structureList,
          filterable: true,
        },
        {
          /** 与 rollingCycle/index.backup-legacy.vue 结构调整 Tab（activeName==second）一致：字段名为 productionVersion */
          prop: "productionVersion",
          label: this.$t(
            "ui.data.column.monthPlanFinalAdjustQuery.productionVersion"
          ),
          type: "select",
          dictData: this.versionOptions,
          filterable: true,
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
      const structureTypeLabel = (v) => {
        const m = { "01": "周期结构", "02": "常规结构" };
        return m[v] != null ? m[v] : v || "";
      };
      const cols = [
        // {
        //   prop: "factoryCode",
        //   label: this.$t("common.factory"),
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
          prop: "embryoCode",
          label: "胎胚号",
          width: 100,
          align: "left",
        },
        {
          prop: "mainMaterialDesc",
          label: "胎胚描述",
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
          label: "英寸",
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
          prop: "dayVulcanizationQty",
          label: this.$t("ui.data.monthlyProductionPlan.dayVulcanizationQty"),
          width: 100,
        },
        {
          prop: "adjustQty1",
          label: "调整1",
          width: 80,
        },
        {
          prop: "adjustQty2",
          label: "调整2",
          width: 80,
        },
        {
          prop: "adjustQty3",
          label: "调整3",
          width: 80,
        },
        {
          prop: "adjustQty4",
          label: "调整4",
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
          prop: "isLockSchedule",
          label: this.$t(
            "ui.data.column.monthPlanFinalAdjustQuery.isLockSchedule"
          ),
          width: 120,
          render: ({ row }) => {
            if (!row.id) {
              return (
                <span>
                  {row.isLockSchedule != null && row.isLockSchedule !== ""
                    ? this.selectDictLabel(
                        this.dict.type.biz_yes_no,
                        row.isLockSchedule
                      )
                    : ""}
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
          label: `${i}号`,
          prop,
          minWidth: "72px",
          render: ({ row }) => {
            const text =
              row[prop] === null || row[prop] === undefined
                ? ""
                : String(row[prop]);
            if (!row.id) {
              return <span>{text}</span>;
            }
            return (
              <el-input
                size="mini"
                value={text}
                onInput={(value) => {
                  const n = String(value).replace(/[^\d]/g, "");
                  row[prop] = n === "" ? null : Number(n);
                }}
                onFocus={() => this.onDayEditFocus(row, prop)}
                onBlur={() => this.handleResultDayEdit(row, prop)}
              />
            );
          },
        });
      }
      cols.push({
        prop: "hasSpecialMaterial",
        label: this.$t("ui.data.column.mpAdjustResult.hasSpecialMaterial"),
        width: 120,
        formatter: (row, column, value) => {
          return this.selectDictLabel(this.dict.type.biz_yes_no, value);
        },
      });
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
    if (rq.productionVersion) {
      defaults.productionVersion = String(rq.productionVersion);
    }
    this.search = { ...defaults };
    this.query = { ...defaults };
    await this.loadVersionOptions();
    await this.fetchCurrentAdjustMachineFromRedis();
    this.getList();
  },
  methods: {
    async handleBaseQueryChange() {
      await this.loadVersionOptions();
      this.fetchCurrentAdjustMachineFromRedis();
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
    /** 从 Redis 拉取当前调整机台并回显到选择器 */
    async fetchCurrentAdjustMachineFromRedis() {
      try {
        const res = await getAdjustsCxMachineFromRedis();
        this.currentAdjustMachine = this.extractRedisCxMachine(res);
      } catch (e) {
        console.error(e);
        this.currentAdjustMachine = "";
      }
    },
    /**
     * 修改优先上机（原锁定上机），与 rollingCycle/index.backup-legacy.vue 一致调用 mpAdjustResult/save
     */
    handleLockScheduleChange(row) {
      if (!row || !row.id) {
        return;
      }
      saveAdjustResult({
        id: row.id,
        isLockSchedule: row.isLockSchedule,
      })
        .then((res) => {
          this.$modal.msgSuccess(res.msg);
        })
        .catch((err) => {
          console.error(err);
        });
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
    /**
     * 修改 1–31 号日排产后实时保存，与 rollingCycle handleResultDayEdit 一致
     */
    async handleResultDayEdit(row, prop) {
      if (!row.id) {
        return;
      }
      const oldVal = this.normalizeDayValue(this.dayEditOriginalValue);
      const newVal = this.normalizeDayValue(row[prop]);
      if (newVal === oldVal) {
        return;
      }
      try {
        this.recalculateBeginEndDay(row);
        await saveAdjustResult(row);
      } catch (err) {
        console.error(err);
      }
    },
    /**
     * 版本号下拉：与 rollingCycle/index.backup-legacy.vue 结构调整 Tab 的 getVersionList 一致，
     * 调用 versionStructure（即 /monthplan/factoryMonthPlanFinalResult/getVersionList），
     * 选项为每行的 productionVersion（label/value 均为 productionVersion），默认取列表首项；
     * 若当前 query/search 中 productionVersion 仍在列表中则保留。
     */
    async loadVersionOptions() {
      const factoryCode = this.query.factoryCode || this.search.factoryCode;
      const yearMonth = this.query.yearMonth || this.search.yearMonth;
      if (!factoryCode || !yearMonth) {
        this.versionOptions = [];
        this.search = { ...this.search, productionVersion: "" };
        this.query = { ...this.query, productionVersion: "" };
        return;
      }
      try {
        const res = await resultVersion(this.formatParamsForStructureVersionList());
        console.log('res版本获取', res)
        const rows = res.rows || [];
        const list = [];
        for (let i = 0; i < rows.length; i++) {
          const pv = rows[i].version;
          if (pv == null || String(pv).trim() === "") {
            continue;
          }
          list.push({
            label: pv,
            value: pv,
          });
        }
        this.versionOptions = list;

        if (list.length > 0) {
          const currentPv = String(
            this.query.productionVersion ||
              this.search.productionVersion ||
              ""
          ).trim();
          
          if (currentPv) {
            const hasVersion = list.some(
              (item) => String(item.value) === currentPv
            );
            if (hasVersion) {
              this.search = { ...this.search, productionVersion: currentPv };
              this.query = { ...this.query, productionVersion: currentPv };
              return;
            }
          }
          const defaultPv = list[0].value;
          console.log('defaultPv', defaultPv)
          console.log('list', list)
          this.search = { ...this.search, productionVersion: defaultPv };
          this.query = { ...this.query, productionVersion: defaultPv };
        } else {
          this.search = { ...this.search, productionVersion: "" };
          this.query = { ...this.query, productionVersion: "" };
        }
      } catch (e) {
        console.error(e);
        this.versionOptions = [];
        this.search = { ...this.search, productionVersion: "" };
        this.query = { ...this.query, productionVersion: "" };
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
      /** 与 query 一并维护 search，保证 PageTable → HeaderSearch 的 defaultValue 含当前表单中的版本号 */
      this.search = { ...this.search, ...data };
      this.query = { ...data };
      this.$set(this.page, "current", 1);
      await this.loadVersionOptions();
      await this.fetchCurrentAdjustMachineFromRedis();
      this.getList();
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
          isAsc: order === "ascending" ? "asc" : "desc",
        };
      } else {
        this.sort = {};
      }
      this.getList();
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
        const arr = params.yearMonth.split("-");
        params.year = Number(arr[0]);
        params.month = Number(arr[1]);
        delete params.yearMonth;
      }
      if(params.productionVersion){
        params.lastMonthPlanVersion = params.productionVersion
      }
      const scheduled = (this.currentAdjustMachine || "").trim();
      if (scheduled) {
        params.cxMachineCode = scheduled;
      } else {
        delete params.cxMachineCode;
      }
      return params;
    },
    /**
     * 版本号下拉数据请求入参：与 rollingCycle/index.backup-legacy.vue 结构调整 Tab 下
     * getVersionList → versionStructure(this.formatParams()) 一致（含分页与 year/month 拆分方式）。
     * 仅用于拉取版本列表，不影响本页其它接口的 formatParams。
     */
    formatParamsForStructureVersionList() {
      const params = {
        ...this.query,
        ...this.sort,
      };
      if (this.page) {
        params.pageSize = this.page.pageSize;
        params.pageNum = this.page.current;
      }
      if (params.yearMonth) {
        const arr = String(params.yearMonth).split("-");
        params.year = arr[0];
        params.month = arr[1];
        delete params.yearMonth;
      }
      return params;
    },
    /**
     * 排产明细导出入参：与 monthPlanManagement/mouldingDayResult formatParams(false) 一致，
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
      return params;
    },
    async getList() {
      const ym = this.normalizeYearMonth(this.query.yearMonth);
      if (!ym || !this.query.factoryCode) {
        return;
      }
      try {
        this.loading = true;
        /** list4Adjust 不传 productionVersion；与 query 解耦，不影响 mpMonthPlanStatistics 使用的 resolveProductionVersionForStatistics */
        const listParams = { ...this.formatParams(true) };
        delete listParams.productionVersion;
        const res = await listMonthPlanFinal4Adjust(listParams);
        const rawRows = res.rows || [];
        this.page.total = res.total != null ? res.total : rawRows.length;
        await this.applyAdjustmentStatisticsRows(rawRows);
      } catch (e) {
        console.error(e);
        this.data = [];
      } finally {
        this.loading = false;
      }
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
      const q = String(
        this.query.productionVersion ||
          this.search.productionVersion ||
          ""
      ).trim();
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
                materialCode: "胎胚种类数",
              };
              const lhMachines = {
                structureName: current.structureName,
                showBackground: "light-blue",
                materialCode: "硫化机台数",
              };
              for (let j = 1; j <= 31; j++) {
                const key = `day${j}`;
                if (statistList[s][key]) {
                  const dayData = JSON.parse(statistList[s][key]);
                  embryoCount[key] = dayData.embryoCount;
                  lhMachines[key] = dayData.lhMachines;
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
    /** 统计行背景色（与 PageTable / 周程滚动一致） */
    tableRowClassName({ row }) {
      if (row.showBackground) {
        return row.showBackground;
      }
      if (row.adjustFlag === 1) {
        return "warning-row";
      }
      return "";
    },
    /** 跳转月计划结构内调整页 */
    handleStructureInnerAdjust() {
      this.$router.push({
        path: "/newPage/monthPlanStructureInnerAdjust",
        query: { pageType: "inner" },
      });
    },
    /** 结构调整弹窗保存新增结构并写入 Redis 后，同步主页面机台与列表 */
    onStructureAdjustSaved() {
      this.fetchCurrentAdjustMachineFromRedis();
      this.getList();
    },
    /**
     * 与周程滚动「结构调整」listAdjusts 入参对齐：productionVersion + version + adjVersion（列表首行 version 一般为调整版本 ADJ…）
     */
    buildStructureDialogListVersionParams() {
      const row = this.data && this.data.length ? this.data[0] : null;
      const qpv = (
        this.query.productionVersion ||
        this.search.productionVersion ||
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
      this.$refs.structureAdjustDialogRef.show({
        factoryCode: fc,
        yearMonth: ym,
        ...this.buildStructureDialogListVersionParams(),
      });
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
     * 查看调整版本弹窗中点击版本号：同步到查询条件 productionVersion 并拉列表
     */
    async onAdjustVersionDialogSelectProductionVersion(productionVersion) {
      const v =
        productionVersion != null ? String(productionVersion).trim() : "";
      if (!v) {
        return;
      }
      this.search = { ...this.search, productionVersion: v };
      this.query = { ...this.query, productionVersion: v };
      const opts = this.versionOptions || [];
      if (!opts.some((item) => String(item.value) === v)) {
        this.versionOptions = [...opts, { label: v, value: v }];
      }
      this.$set(this.page, "current", 1);
      await this.fetchCurrentAdjustMachineFromRedis();
      this.getList();
    },
    handleExport() {
      downloadLink(
        "/monthplan/factoryMonthPlanMouldDayResult/export",
        this.formatMouldingDayExportParams()
      );
    },
    handleExportAllMaterial() {
      downloadLink(
        "/monthplan/factoryMonthPlanMouldDayResult/exportAllMaterial",
        this.formatMouldingDayExportParams()
      );
    },
    /** 打开下发弹窗，预填当前查询的年月、分厂，并加载可推送版本列表 */
    handleIssueScmMes() {
      const now = new Date();
      const nextMonth = new Date(now.getFullYear(), now.getMonth() + 1, 1);
      const year = nextMonth.getFullYear();
      const month = nextMonth.getMonth() + 1; // 月份从0开始，需要+1
      this.syncDialog.visible = true;
      this.syncDialog.form.yearMonth =`${year}-${month < 10 ? "0" + month : month}`;
      // this.syncDialog.form.yearMonth = this.formatYearMonthForPicker(
      //   this.query.yearMonth || this.search.yearMonth
      // );
      this.syncDialog.form.factoryCode =
        this.query.factoryCode || this.search.factoryCode || "";
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
     * 组装与周程滚动「结构内调整 -> 调整结果 -> 确定」confirmResult 一致的入参（adjustType=01）。
     * 供「确认调整」与「重新计算」共用，与 POST /monthplan/mpWeekRollAdjust/confirmAdjust 请求体一致。
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
      params.adjustType = "01";
      params.version = row.version;
      params.productionVersion =
        row.productionVersion || this.query.productionVersion;
      params.startDay = row.beginDay;
      params.endDay = row.endDay;
      params.adjustStartDay =
        row.adjustStartDay != null && row.adjustStartDay !== ""
          ? row.adjustStartDay
          : row.beginDay;
      params.adjustEndDay =
        row.adjustEndDay != null && row.adjustEndDay !== ""
          ? row.adjustEndDay
          : row.endDay;
      params.structureName =
        row.structureName || this.query.structureName;
      const sm = (this.currentAdjustMachine || "").trim();
      params.scheduledMachines = sm;
      return params;
    },
    /**
     * 确认调整 / 重新计算 前置校验，通过则返回与 confirmAdjust 相同的请求体，否则提示并返回 null
     */
    prepareWeekRollSubmitPayloadOrWarn() {
      const machine = (this.currentAdjustMachine || "").trim();
      // if (!machine) {
      //   this.$modal.msgWarning(
      //     this.$t(
      //       "ui.data.column.monthPlanFinalAdjustQuery.confirmNeedMachine"
      //     )
      //   );
      //   return null;
      // }
      if (!this.data || !this.data.length) {
        this.$modal.msgWarning(
          this.$t(
            "ui.data.column.monthPlanFinalAdjustQuery.confirmNeedListData"
          )
        );
        return null;
      }
      const payload = this.buildWeekRollConfirmPayload();
      if (
        !payload ||
        payload.version == null ||
        payload.version === "" ||
        !payload.productionVersion
      ) {
        this.$modal.msgWarning(
          this.$t(
            "ui.data.column.monthPlanFinalAdjustQuery.confirmNeedVersion"
          )
        );
        return null;
      }
      return payload;
    },
    async handleConfirmAdjust() {
      if (!this.canUsePrimaryAdjustActions) {
        return;
      }
      const payload = this.prepareWeekRollSubmitPayloadOrWarn();
      if (!payload) {
        return;
      }
      this.confirmAdjustLoading = true;
      try {
        const res = await confirmAdjust(payload);
        this.$modal.msgSuccess(
          (res && res.msg) ||
            this.$t("common.msg.ajax.operation.success")
        );
        await this.getList();
        await this.fetchCurrentAdjustMachineFromRedis();
      } catch (e) {
        console.error(e);
      } finally {
        this.confirmAdjustLoading = false;
      }
    },
    /** 继续调整：打开结构调整弹窗并带入当前调整机台（弹窗内机台只读） */
    handleContinueAdjust() {
      if (!this.canUseContinueAdjust) {
        return;
      }
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
      const machine = (this.currentAdjustMachine || "").trim();
      if (!machine) {
        this.$modal.msgWarning(
          this.$t(
            "ui.data.column.monthPlanFinalAdjustQuery.confirmNeedMachine"
          )
        );
        return;
      }
      this.$refs.structureAdjustDialogRef.show({
        factoryCode: fc,
        yearMonth: ym,
        ...this.buildStructureDialogListVersionParams(),
        fixedCxMachineCode: machine,
      });
    },
    /** 重新计算：POST /monthplan/mpWeekRollAdjust，body 与 confirmAdjust 完全一致 */
    async handleRecalculate() {
      const payload = this.prepareWeekRollSubmitPayloadOrWarn();
      if (!payload) {
        return;
      }
      this.recalculateLoading = true;
      try {
        const res = await recalculateWeekRollAdjust(payload);
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
  width: 220px;
}
.footer-actions {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 12px;
  padding: 8px 0 4px;
}
</style>
