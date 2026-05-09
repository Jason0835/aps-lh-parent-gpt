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
          width="220"
          align="center"
          fixed="right"
        >
          <template slot-scope="scope">
            <template v-if="!scope.row._isNew">
              <el-button type="text" @click="handleSelect(scope.row)">{{
                $t("ui.data.column.monthPlanFinalAdjustQuery.btnSelect")
              }}</el-button>
            </template>
            <template v-else>
              <el-button type="text" @click="handlePlanDowntime(scope.row)">{{
                $t("ui.data.column.monthPlanFinalAdjustQuery.planDowntime")
              }}</el-button>
              <el-button type="text" @click="handleSaveRow(scope.row)">{{
                $t("common.button.save")
              }}</el-button>
            </template>
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
  addAdjust,
  setAdjustsCxMachineFromRedis,
} from "@/api/monthplan/adjustStructure";

const MONTH_STANDARD_MAX = 31;

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

function intervalsOverlap(a, b) {
  const b1 = Math.min(Number(a.begin), Number(a.end));
  const e1 = Math.max(Number(a.begin), Number(a.end));
  const b2 = Math.min(Number(b.begin), Number(b.end));
  const e2 = Math.max(Number(b.begin), Number(b.end));
  return Math.max(b1, b2) <= Math.min(e1, e2);
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
      this.adjustMachineLocked = !!fixed;
      this.serverRows = [];
      this.newRows = [];
      this.dialogVisible = true;
      /** 先打开弹窗再设机台；继续调整时带入固定机台并拉列表 */
      this.$nextTick(() => {
        if (fixed) {
          this.cxMachineCode = fixed;
          this.loadMachineStructureList(fixed);
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
      if (pv) {
        params.productionVersion = pv;
      }
      if (ver) {
        params.version = ver;
      }
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
    /** 计划停机：按当前表格内全部结构（含未保存新增）计算第一段空隙 */
    handlePlanDowntime(row) {
      const others = this.tableRows.filter((r) => r !== row);
      const gap = getFirstGap(
        others.map((r) => ({ begin: r.beginDay, end: r.endDay }))
      );
      if (!gap) {
        this.$modal.msgWarning(
          this.$t("ui.data.column.monthPlanFinalAdjustQuery.noGapForDowntime")
        );
        return;
      }
      this.$set(row, "beginDay", gap.begin);
      this.$set(row, "endDay", gap.end);
    },
    /** 同一结构名称下日期区间不得重叠 */
    validateNoOverlapForStructure(row) {
      const name = (row.structureName || "").trim();
      if (!name) {
        this.$modal.msgWarning(this.$t("common.rule.input"));
        return false;
      }
      if (row.beginDay == null || row.endDay == null) {
        this.$modal.msgWarning(this.$t("common.rule.select"));
        return false;
      }
      const cur = { begin: row.beginDay, end: row.endDay };
      const sameNameRows = this.tableRows.filter(
        (r) => (r.structureName || "").trim() === name && r !== row
      );
      for (const o of sameNameRows) {
        if (o.beginDay == null || o.endDay == null) {
          continue;
        }
        if (intervalsOverlap(cur, { begin: o.beginDay, end: o.endDay })) {
          this.$modal.msgWarning(
            this.$t("ui.data.column.monthPlanFinalAdjustQuery.structureDateOverlap")
          );
          return false;
        }
      }
      return true;
    },
    async handleSaveRow(row) {
      if (!this.validateNoOverlapForStructure(row)) {
        return;
      }
      try {
        this.loading = true;
        const params = {
          factoryCode: this.factoryCode,
          cxMachineCode: this.cxMachineCode,
          structureName: row.structureName,
          beginDay: row.beginDay,
          endDay: row.endDay,
          yearMonth: this.yearMonth,
        };
        if (params.yearMonth) {
          const arr = params.yearMonth.split("-");
          params.year = arr[0];
          params.month = arr[1];
          delete params.yearMonth;
        }
        const res = await addAdjust(params);
        /** 新增结构保存成功后，将当前调整机台写入 Redis（与主页面只读展示一致） */
        await setAdjustsCxMachineFromRedis(this.cxMachineCode);
        this.$modal.msgSuccess(
          res.msg || this.$t("common.msg.success.operate")
        );
        this.newRows = this.newRows.filter((r) => r._tmpId !== row._tmpId);
        await this.loadMachineStructureList();
        this.$emit("structure-adjust-saved");
        this.dialogVisible = false;
      } catch (e) {
        console.error(e);
      } finally {
        this.loading = false;
      }
    },
    handleSelect(row) {
      if (!this.yearMonth) {
        this.$modal.msgWarning(this.$t("common.rule.select"));
        return;
      }
      /** 跳转月计划结构调整页（与路由 path 一致） */
      this.$router.push({
        path: "/newPage/monthPlanStructureAdjust",
        query: {
          pageType: "structure",
          fromSelect: "1",
          factoryCode: this.factoryCode,
          yearMonth: this.yearMonth,
          cxMachineCode: row.cxMachineCode || this.cxMachineCode,
          scheduledMachines:
            row.scheduledMachines || row.cxMachineCode || this.cxMachineCode,
          structureName: row.structureName || "",
          beginDay: row.beginDay != null ? String(row.beginDay) : "",
          endDay: row.endDay != null ? String(row.endDay) : "",
          productionVersion: row.productionVersion || "",
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
