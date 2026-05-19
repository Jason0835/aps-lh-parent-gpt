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
              @click="handleSelect(scope.row)"
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
/** listOutsideStructure → POST /monthplan/mpStructureAllocation/listAdjusts；outGetStayDay/versionOutHistory 与周程单结构调整一致 */
import {
  listOutsideStructure,
  setAdjustsCxMachineFromRedis,
  getAdjustsCxMachineFromRedis,
  outGetStayDay,
  versionOutHistory,
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
    /**
     * 组装 getPreviousStructure（outGetStayDay）入参：与周程旧版一致，传 **listAdjusts 行全部字段**，
     * 再补弹窗上下文（factory、年月、首台机台、定稿版本）；去掉 UI 临时字段。
     * @param {object} row 列表行
     * @param {string} cxFirst 成型机编码（首台，与跳转 query 一致）
     * @returns {object|null} 无行则 null
     */
    buildGetPreviousStructurePayload(row, cxFirst) {
      if (!row || typeof row !== "object") {
        return null;
      }
      const payload = { ...row };
      delete payload._isNew;
      delete payload._tmpId;

      if (this.factoryCode && !payload.factoryCode) {
        payload.factoryCode = this.factoryCode;
      }
      const needYm =
        payload.year == null ||
        payload.year === "" ||
        payload.month == null ||
        payload.month === "";
      if (needYm && this.yearMonth) {
        const arr = String(this.yearMonth).trim().split("-");
        if (arr.length >= 2) {
          payload.year = Number(arr[0]);
          payload.month = Number(arr[1]);
        }
      } else {
        if (payload.year != null && payload.year !== "") {
          payload.year = Number(payload.year);
        }
        if (payload.month != null && payload.month !== "") {
          payload.month = Number(payload.month);
        }
      }
      if (cxFirst != null && String(cxFirst).trim() !== "") {
        payload.cxMachineCode = String(cxFirst).trim();
      }
      const pv = this.resolveProductionVersionForJump(row);
      if (
        pv &&
        (!payload.productionVersion ||
          String(payload.productionVersion).trim() === "")
      ) {
        payload.productionVersion = pv;
      }
      if (
        payload.beginDay !== undefined &&
        payload.beginDay !== "" &&
        payload.beginDay != null
      ) {
        payload.beginDay = Number(payload.beginDay);
      }
      if (
        payload.endDay !== undefined &&
        payload.endDay !== "" &&
        payload.endDay != null
      ) {
        payload.endDay = Number(payload.endDay);
      }
      return payload;
    },
    /**
     * 组装 mpAdjustStructureOut/getVersionList 入参，与 rollingCycle 的 getOutVersionList 中 params 处理一致。
     * @param {string} schedFirst 排产机台（首台）
     * @param {object} row 列表行（取 productionVersion）
     */
    buildOutVersionListParams(schedFirst, row) {
      const params = {
        factoryCode: this.factoryCode,
        yearMonth: this.yearMonth,
        scheduledMachines: schedFirst || "",
      };
      const pv = this.resolveProductionVersionForJump(row);
      if (pv) {
        params.productionVersion = pv;
      }
      if (params.yearMonth) {
        const arr = String(params.yearMonth).split("-");
        if (arr.length >= 2) {
          params.year = arr[0];
          params.month = arr[1];
        }
      }
      return params;
    },
    /**
     * 跳转月计划结构调整页前，与周程「结构调整 → 单结构调整」一致预拉 getPreviousStructure、单结构版本列表。
     * @param {object} row 当前行
     * @param {string} cxFirst 成型机首台
     * @param {string} schedFirst 排产机台首台
     */
    async prefetchSingleStructureApis(row, cxFirst, schedFirst) {
      const prevPayload = this.buildGetPreviousStructurePayload(row, cxFirst);
      const versionParams = this.buildOutVersionListParams(schedFirst, row);
      const tasks = [];
      if (prevPayload) {
        tasks.push(
          outGetStayDay(prevPayload).catch((err) => {
            console.warn("getPreviousStructure 预请求失败", err);
          })
        );
      }
      tasks.push(
        versionOutHistory(versionParams).catch((err) => {
          console.warn("mpAdjustStructureOut/getVersionList 预请求失败", err);
        })
      );
      await Promise.all(tasks);
    },
    async handleSelect(row) {
      if (!this.yearMonth) {
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
      try {
        this.loading = true;
        await this.prefetchSingleStructureApis(row, cxFirst, schedFirst);
      } catch (e) {
        console.error(e);
      } finally {
        this.loading = false;
      }
      /** 整行写入 sessionStorage，目标页与周程「勾选 → 单结构调整」同样使用完整 listAdjusts 行 */
      try {
        sessionStorage.setItem(
          MP_STRUCTURE_ADJUST_PREFILL_STORAGE_KEY,
          JSON.stringify(row)
        );
      } catch (e) {
        console.warn("结构行写入 sessionStorage 失败", e);
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
