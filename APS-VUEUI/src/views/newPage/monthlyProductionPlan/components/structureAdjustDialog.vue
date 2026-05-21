<template>
  <el-dialog
    :title="$t('ui.data.column.monthPlanFinalAdjustQuery.structureAdjust')"
    :visible.sync="dialogVisible"
    width="700px"
    append-to-body
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <div v-loading="loading">
      <el-form inline class="dialog-toolbar">
        <el-form-item
          :label="$t('ui.data.column.monthPlanFinalAdjustQuery.adjustMachine')"
        >
          <forming-capacity-select
            :value="cxMachineCode"
            :factory-code="factoryCode"
            :disabled="adjustMachineLocked"
            style="width: 220px"
            @change="onAdjustMachineChange"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            plain
            :disabled="!canAddStructureRow"
            @click="handleAppendRow"
          >
            {{ $t("ui.data.column.monthPlanFinalAdjustQuery.addStructure") }}
          </el-button>
        </el-form-item>
        <el-form-item>
          <el-button
            type="warning"
            plain
            @click="handlePlanDowntime"
          >
            {{ $t("ui.data.column.monthPlanFinalAdjustQuery.planDowntime") }}
          </el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableRows" border size="small" max-height="420">
        <el-table-column
          :label="$t('ui.data.column.finishStock.structureName')"
          min-width="120"
        >
          <template slot-scope="scope">
            <el-select
              v-if="scope.row._isNew"
              v-model="scope.row.structureName"
              filterable
              clearable
              :placeholder="$t('ui.frame.btn.choose')"
              style="width: 100%"
            >
              <el-option
                v-for="opt in structureList"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value"
              />
            </el-select>
            <span v-else>{{ scope.row.structureName }}</span>
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('common.startDate')"
          width="120"
          align="center"
        >
          <template slot-scope="scope">
            <el-select
              v-if="scope.row._isNew"
              v-model="scope.row.beginDay"
              filterable
              clearable
              :placeholder="$t('ui.frame.btn.choose')"
              style="width: 100%"
            >
              <el-option
                v-for="d in dayOptions"
                :key="'b' + d"
                :label="String(d)"
                :value="d"
              />
            </el-select>
            <span v-else>{{ scope.row.beginDay }}</span>
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('common.endDate')"
          width="120"
          align="center"
        >
          <template slot-scope="scope">
            <el-select
              v-if="scope.row._isNew"
              v-model="scope.row.endDay"
              filterable
              clearable
              :placeholder="$t('ui.frame.btn.choose')"
              style="width: 100%"
            >
              <el-option
                v-for="d in dayOptions"
                :key="'e' + d"
                :label="String(d)"
                :value="d"
              />
            </el-select>
            <span v-else>{{ scope.row.endDay }}</span>
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('ui.data.btn.option')"
          width="140"
          align="center"
          fixed="right"
        >
          <template slot-scope="scope">
            <el-button
              type="text"
              :disabled="scope.row._isNew && (!scope.row.structureName || scope.row.beginDay == null || scope.row.endDay == null)"
              @click="handleSelect(scope.row, scope.$index)"
            >{{
              $t("ui.data.column.monthPlanFinalAdjustQuery.btnSelect")
            }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </el-dialog>
</template>

<script>
import { mapGetters } from "vuex";
import formingCapacitySelect from "@/views/components/formingCapacitySelect.vue";
/** listOutsideStructure → POST /monthplan/mpStructureAllocation/listAdjusts */
import {
  listOutsideStructure,
  setAdjustsCxMachineFromRedis,
  getAdjustsCxMachineFromRedis,
} from "@/api/monthplan/adjustStructure";

const MONTH_STANDARD_MAX = 31;

/**
 * 与 rollingCycle/index.vue 中常量一致：跳转前写入 listAdjusts 整行，created 里读出后与 handleAdd 的 selection[0] 对齐。
 */
const MP_STRUCTURE_ADJUST_PREFILL_STORAGE_KEY =
  "mpMonthPlanStructureAdjust.prefillListAdjustRow";

/** 合并区间（闭区间），用于判断 1～31 是否已被占满 */
function mergeIntervals(intervals) {
  const list = intervals
    .filter((x) => x && x.begin != null && x.end != null)
    .map((x) => ({
      b: Math.min(Number(x.begin), Number(x.end)),
      e: Math.max(Number(x.begin), Number(x.end)),
    }))
    .filter((x) => !Number.isNaN(x.b) && !Number.isNaN(x.e))
    .sort((a, b) => a.b - b.b);
  const merged = [];
  for (const iv of list) {
    if (!merged.length || iv.b > merged[merged.length - 1].e + 1) {
      merged.push({ b: iv.b, e: iv.e });
    } else {
      merged[merged.length - 1].e = Math.max(merged[merged.length - 1].e, iv.e);
    }
  }
  return merged;
}

/** 若存在空隙则返回第一段空隙 [begin,end]，否则 null（表示 1～maxDay 已被占满） */
function getFirstGap(allIntervals, maxDay = MONTH_STANDARD_MAX) {
  const merged = mergeIntervals(allIntervals);
  let cur = 1;
  for (const m of merged) {
    if (m.b > cur) {
      return { begin: cur, end: m.b - 1 };
    }
    cur = Math.max(cur, m.e + 1);
  }
  if (cur <= maxDay) {
    return { begin: cur, end: maxDay };
  }
  return null;
}

let tmpId = 1;

export default {
  name: "StructureAdjustDialog",
  components: { formingCapacitySelect },
  inject: ["parentDict"],
  data() {
    return {
      dialogVisible: false,
      loading: false,
      factoryCode: "116",
      yearMonth: "",
      /** 与月计划查询条件一致时传入，便于后端按定稿版本过滤机台产品结构 */
      productionVersion: "",
      /** 与周程滚动「结构调整」Tab 的 listAdjusts 一致：调整版本、排产版本可能不同 */
      listAdjustsVersion: "",
      listAdjustsAdjVersion: "",
      cxMachineCode: "",
      /** 继续调整：机台由主页面带入且不可改 */
      adjustMachineLocked: false,
      serverRows: [],
      newRows: [],
    };
  },
  computed: {
    ...mapGetters("globalList", ["structureList"]),
    dayOptions() {
      return Array.from({ length: MONTH_STANDARD_MAX }, (_, i) => i + 1);
    },
    tableRows() {
      return [...this.serverRows, ...this.newRows];
    },
    /** 仅服务端结构：若已排满 1～31（无空隙）则不可再新增 */
    canAddStructureRow() {
      if (!this.cxMachineCode) {
        return false;
      }
      const gap = getFirstGap(this.serverIntervalsOnly);
      return gap != null;
    },
    serverIntervalsOnly() {
      return this.serverRows.map((r) => ({
        begin: r.beginDay,
        end: r.endDay,
      }));
    },
  },
  methods: {
    /** 机台字段可能为 H1503,H1201 形式，跳转结构调整页只带第一台 */
    extractFirstCxMachineCode(val) {
      if (val == null) {
        return "";
      }
      const s = String(val).trim();
      if (!s) {
        return "";
      }
      const parts = s
        .split(/[,，]/)
        .map((x) => String(x).trim())
        .filter(Boolean);
      return parts.length ? parts[0] : "";
    },
    /**
     * 跳转结构调整页时的定稿版本：优先列表行，其次弹窗打开时月计划页传入的 productionVersion（与 buildStructureDialogListVersionParams 一致）。
     * @param {object} row 当前表格行
     */
    resolveProductionVersionForJump(row) {
      const fromRow =
        row && row.productionVersion != null
          ? String(row.productionVersion).trim()
          : "";
      const fromDialog = (this.productionVersion || "").trim();
      return fromRow || fromDialog;
    },
    show(payload) {
      this.factoryCode = (payload && payload.factoryCode) || "116";
      this.yearMonth = (payload && payload.yearMonth) || "";
      this.productionVersion = (payload && payload.productionVersion) || "";
      this.listAdjustsVersion =
        payload && payload.listAdjustsVersion != null
          ? String(payload.listAdjustsVersion).trim()
          : "";
      this.listAdjustsAdjVersion =
        payload && payload.listAdjustsAdjVersion != null
          ? String(payload.listAdjustsAdjVersion).trim()
          : "";
      const fixed = (
        payload &&
        payload.fixedCxMachineCode != null &&
        String(payload.fixedCxMachineCode).trim() !== ""
      )
        ? String(payload.fixedCxMachineCode).trim()
        : "";
      /** 月计划页外部默认带入的首台机台（勾选行 / Redis 当前机台等）；与 fixed 互斥 */
      const prefill =
        !fixed &&
        payload &&
        payload.prefillCxMachineCode != null &&
        String(payload.prefillCxMachineCode).trim() !== ""
          ? String(payload.prefillCxMachineCode).trim()
          : "";
      /** 外部带入或继续调整固定机台时不可编辑 */
      this.adjustMachineLocked = !!fixed || !!prefill;
      this.serverRows = [];
      this.newRows = [];
      this.dialogVisible = true;
      /** 先打开弹窗再设机台；继续调整时带入固定机台并拉列表；普通入口可预填首台机台 */
      this.$nextTick(() => {
        if (fixed) {
          this.cxMachineCode = fixed;
          this.loadMachineStructureList(fixed);
        } else if (prefill) {
          this.cxMachineCode = prefill;
          this.loadMachineStructureList(prefill);
        } else {
          this.cxMachineCode = "";
        }
      });
    },
    handleClose() {
      this.cxMachineCode = "";
      this.productionVersion = "";
      this.listAdjustsVersion = "";
      this.listAdjustsAdjVersion = "";
      this.adjustMachineLocked = false;
      this.serverRows = [];
      this.newRows = [];
    },
    /**
     * 成型机台弹窗点「确定」后触发：此处同时写入机台号并请求 listAdjusts（不用 v-model，避免与 change 时序问题）。
     */
    onAdjustMachineChange(val) {
      if (this.adjustMachineLocked) {
        return;
      }
      const code =
        val != null && val !== undefined && String(val).trim() !== ""
          ? String(val).trim()
          : "";
      this.cxMachineCode = code;
      this.newRows = [];
      if (!code) {
        this.serverRows = [];
        return;
      }
      if (!this.yearMonth) {
        this.$modal.msgWarning(
          this.$t(
            "ui.data.column.monthPlanFinalAdjustQuery.pleaseSelectYearMonth"
          )
        );
        return;
      }
      this.loadMachineStructureList(code);
    },
    /**
     * 与周程滚动「结构调整」Tab 的 formatParams + listAdjusts 一致：
     * factoryCode、scheduledMachines、productionVersion、version、adjVersion、year/month（字符串）、yearMonth 置空、分页。
     */
    formatListAdjustsParams(machineCode) {
      const code =
        machineCode != null && machineCode !== ""
          ? machineCode
          : this.cxMachineCode;
      const params = {
        factoryCode: this.factoryCode,
        scheduledMachines: code,
        pageNum: 1,
        pageSize: 20,
      };
      const pv = (this.productionVersion || "").trim();
      const ver = (this.listAdjustsVersion || "").trim();
      const adj = (this.listAdjustsAdjVersion || "").trim();
      // if (pv) {
      //   params.productionVersion = pv;
      // }
      // if (ver) {
      //   params.version = ver;
      // }
      if (adj) {
        params.adjVersion = adj;
      }
      if (this.yearMonth) {
        const raw = String(this.yearMonth).trim();
        const arr = raw.split("-");
        if (arr.length >= 2) {
          params.year = arr[0];
          params.month = arr[1];
          params.yearMonth = "";
        }
      }
      return params;
    },
    /**
     * 调用 /monthplan/mpStructureAllocation/listAdjusts 查询机台产品结构。
     * @param {string} [machineCode] 优先使用（来自 watch 的新值，避免与 this.cxMachineCode 不同步）
     */
    async loadMachineStructureList(machineCode) {
      const code =
        machineCode != null && machineCode !== ""
          ? machineCode
          : this.cxMachineCode;
      if (!code || !this.yearMonth) {
        this.serverRows = [];
        return;
      }
      try {
        this.loading = true;
        const res = await listOutsideStructure(
          this.formatListAdjustsParams(code)
        );
        const rows = res.rows || res.data || [];
        this.serverRows = rows.map((r) => ({
          ...r,
          _isNew: false,
        }));
      } catch (e) {
        console.error(e);
        this.serverRows = [];
      } finally {
        this.loading = false;
      }
    },
    handleAppendRow() {
      this.newRows.push({
        _isNew: true,
        _tmpId: tmpId++,
        structureName: "",
        beginDay: null,
        endDay: null,
      });
    },
    async handlePlanDowntime() {
      try {
        this.loading = true;
        await setAdjustsCxMachineFromRedis({
          cxMachineCode: "",
          structureName: "",
          beginDay: null,
          endDay: null,
          version: "",
        });
        this.dialogVisible = false;
        await getAdjustsCxMachineFromRedis();
        this.$emit("plan-downtime-applied");
      } catch (e) {
        console.error(e);
      } finally {
        this.loading = false;
      }
    },
    getDaysInMonth(year, month) {
      return new Date(year, month, 0).getDate();
    },

    resolveYearMonthFromRow(row) {
      let year = row.year != null && row.year !== "" ? Number(row.year) : NaN;
      let month = row.month != null && row.month !== "" ? Number(row.month) : NaN;
      if ((Number.isNaN(year) || Number.isNaN(month)) && this.yearMonth) {
        const arr = String(this.yearMonth).trim().split("-");
        if (arr.length >= 2) {
          year = Number(arr[0]);
          month = Number(arr[1]);
        }
      }
      return { year, month };
    },

    /**
     * 前结构 = 弹窗表格当前行的上一行（必须用表格行号，scope.row 与 serverRows 引用可能不一致）。
     */
    findPreviousStructureInMachineList(tableIndex) {
      const list = this.tableRows;
      if (
        tableIndex == null ||
        tableIndex <= 0 ||
        tableIndex >= list.length
      ) {
        return null;
      }
      const prev = list[tableIndex - 1];
      if (
        !prev ||
        prev.beginDay == null ||
        prev.endDay == null
      ) {
        return null;
      }
      return prev;
    },

    isAdjustStartDayEmpty(value) {
      return value == null || value === "";
    },

    /**
     * 选择时根据弹窗表格计算调整开始/结束日（仅用于跳转页展示）：
     * 当前行 adjustStartDay 为空且有上一行 → 上一行 endDay+1；
     * 无前一行 → 当前行 beginDay；
     * 调整结束日 → 当前行 endDay。
     */
    resolveAdjustDaysOnSelect(row, tableIndex) {
      const beginDay = Number(row.beginDay);
      const endDay = Number(row.endDay);
      const adjustEndDay = endDay;
      let adjustStartDay = beginDay;
      let hasPreviousStructure = false;
      let prevBeginDay = null;
      let prevEndDay = null;

      const prev = this.findPreviousStructureInMachineList(tableIndex);
      const rowAdjustStartEmpty = this.isAdjustStartDayEmpty(row.adjustStartDay);

      if (prev && rowAdjustStartEmpty) {
        hasPreviousStructure = true;
        prevBeginDay = Number(prev.beginDay);
        prevEndDay = Number(prev.endDay);
        if (!Number.isNaN(prevEndDay)) {
          const { year, month } = this.resolveYearMonthFromRow(row);
          const monthMax =
            !Number.isNaN(year) && !Number.isNaN(month)
              ? this.getDaysInMonth(year, month)
              : MONTH_STANDARD_MAX;
          adjustStartDay = Math.min(prevEndDay + 1, monthMax);
        }
      } else if (!this.isAdjustStartDayEmpty(row.adjustStartDay)) {
        adjustStartDay = Number(row.adjustStartDay);
      }

      return {
        hasPreviousStructure,
        prevBeginDay,
        prevEndDay,
        adjustStartDay,
        adjustEndDay,
      };
    },

    handleSelect(row, tableIndex) {
      if (!this.yearMonth) {
        this.$modal.msgWarning(this.$t("common.rule.select"));
        return;
      }
      if (row.beginDay == null || row.endDay == null) {
        this.$modal.msgWarning(this.$t("common.rule.select"));
        return;
      }
      const rawSched =
        row.scheduledMachines || row.cxMachineCode || this.cxMachineCode;
      const rawCx = row.cxMachineCode || this.cxMachineCode || rawSched;
      const cxFirst =
        this.extractFirstCxMachineCode(rawCx) ||
        this.extractFirstCxMachineCode(rawSched);
      const schedFirst =
        this.extractFirstCxMachineCode(rawSched) || cxFirst;

      const adjustContext = this.resolveAdjustDaysOnSelect(row, tableIndex);
      const prefillRow = {
        ...row,
        cxMachineCode: cxFirst,
        scheduledMachines: schedFirst,
        productionVersion: this.resolveProductionVersionForJump(row),
      };
      try {
        sessionStorage.setItem(
          MP_STRUCTURE_ADJUST_PREFILL_STORAGE_KEY,
          JSON.stringify({
            row: prefillRow,
            adjustContext,
          })
        );
      } catch (e) {
        console.warn("结构行写入 sessionStorage 失败", e);
        this.$modal.msgError(this.$t("common.msg.fail"));
        return;
      }

      /** 跳转月计划结构调整页（与路由 path 一致），多机台时 query 只带第一台成型机 */
      this.$router.push({
        path: "/newPage/monthPlanStructureAdjust",
        query: {
          pageType: "structure",
          fromSelect: "1",
          prefillStore: "1",
          factoryCode: this.factoryCode,
          yearMonth: this.yearMonth,
          cxMachineCode: cxFirst,
          scheduledMachines: schedFirst,
          structureName: row.structureName || "",
          beginDay: row.beginDay != null ? String(row.beginDay) : "",
          endDay: row.endDay != null ? String(row.endDay) : "",
          adjustStartDay:
            adjustContext.adjustStartDay != null
              ? String(adjustContext.adjustStartDay)
              : "",
          adjustEndDay:
            adjustContext.adjustEndDay != null
              ? String(adjustContext.adjustEndDay)
              : "",
          productionVersion: this.resolveProductionVersionForJump(row),
        },
      });
      this.dialogVisible = false;
    },
  },
};
</script>

<style scoped>
.dialog-toolbar {
  margin-bottom: 12px;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
}
</style>
