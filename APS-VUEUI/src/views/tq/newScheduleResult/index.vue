<template>
  <basic-container>
    <page-table
      tableRef="tqNewScheduleResultMainTable"
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
          type="primary"
          plain
          v-hasPermi="['tq:newScheduleResult:autoPlan']"
          @click="handleAutoPlan"
        >{{ $t("ui.data.btn.tqNewScheduleResult.autoPlan") }}</el-button>
        <el-button
          type="warning"
          plain
          v-hasPermi="['tq:newScheduleResult:insertOrder']"
          @click="handleInsertOrder"
        >{{ $t("ui.data.btn.tqNewScheduleResult.insertOrder") }}</el-button>
        <el-button
          type="danger"
          v-hasPermi="['tq:newScheduleResult:remove']"
          @click="handleBatchDelete"
          :disabled="selection.length == 0"
        >{{ $t("ui.frame.btn.delete") }}</el-button>
        <el-button
          type="primary"
          plain
          v-hasPermi="['tq:newScheduleResult:changeMachine']"
          @click="handleChangeMachine"
          :disabled="selection.length == 0"
        >{{ $t("ui.data.btn.tqNewScheduleResult.changeMachine") }}</el-button>
        <el-button
          type="primary"
          plain
          v-hasPermi="['tq:newScheduleResult:adjustQty']"
          @click="handleAdjustQty"
          :disabled="selection.length == 0"
        >{{ $t("ui.data.btn.tqNewScheduleResult.adjustQty") }}</el-button>
        <el-button
          v-hasPermi="['tq:newScheduleResult:import']"
          @click="$refs.tltUpload.handleImport()"
        >{{ $t("ui.frame.btn.import") }}</el-button>
        <el-button
          @click="handleExport"
          v-hasPermi="['tq:newScheduleResult:export']"
        >{{ $t("ui.frame.btn.export") }}</el-button>
        <el-button
          type="success"
          plain
          v-hasPermi="['tq:newScheduleResult:release']"
          @click="handleRelease"
          :disabled="selection.length == 0"
        >{{ $t("ui.data.btn.tqNewScheduleResult.release") }}</el-button>
      </template>
    </page-table>
    <tlt-upload-form
      ref="tltUpload"
      :updateSupport="true"
      downloadUrl="/tq/newScheduleResult/importTemplate"
      uploadUrl="/tq/newScheduleResult/importData"
      @uploadSuccess="getList"
      labelWidth="0"
      :columns="importColumns"
    ></tlt-upload-form>
    <!-- 插单弹窗 -->
    <insert-order-dialog ref="insertOrderDialog" @success="getList" />
    <!-- 转机台弹窗 -->
    <change-machine-dialog ref="changeMachineDialog" @success="getList" />
    <!-- 调量弹窗 -->
    <adjust-qty-dialog ref="adjustQtyDialog" @success="getList" />
    <!-- 自动排程弹窗 -->
    <auto-plan-dialog ref="autoPlanDialogRef" @success="getList" />
  </basic-container>
</template>
<script>
import { downloadLink } from "@/utils/request";
import { listNewScheduleResult, logicDeleteNewScheduleResult, listScheduleShiftDates, autoPlan, insertOrder, changeMachine, changeQty, publishSchedule } from "@/api/tq/tqNewScheduleResult";
import { mapState } from "vuex";
import TltUploadForm from "@/views/components/tltUploadForm.vue";
import InsertOrderDialog from "./components/insertOrderDialog.vue";
import ChangeMachineDialog from "./components/changeMachineDialog.vue";
import AdjustQtyDialog from "./components/adjustQtyDialog.vue";
import AutoPlanDialog from "./components/autoPlanDialog.vue";

const formatDate = (date) => {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
};

const getOffsetDate = (offsetDay) => {
  const date = new Date();
  date.setDate(date.getDate() + offsetDay);
  return formatDate(date);
};

export default {
  name: "TqNewScheduleResult",
  components: {
    TltUploadForm,
    InsertOrderDialog,
    ChangeMachineDialog,
    AdjustQtyDialog,
    AutoPlanDialog,
  },
  dicts: ["IS_RELEASE", "biz_factory_name"],
  provide() {
    return {
      parentDict: this.dict,
    };
  },
  data() {
    return {
      importColumns: [
        {
          label: "",
          prop: "updateSupport",
          render: (form) => {
            return (
              <el-checkbox
                label={this.$t("common.rule.updateSupport")}
                v-model={form.updateSupport}
              >
                {this.$t("common.rule.updateSupport")}
              </el-checkbox>
            );
          },
        },
      ],
      loading: false,
      data: [],
      selection: [],
      page: {
        current: 1,
        pageSize: 20,
        total: 0,
      },
      sort: {},
      search: {
        scheduleDateQuery: getOffsetDate(1),
      },
      query: {
        scheduleDateQuery: getOffsetDate(1),
      },
      dateList: [
        { shift: 1, shiftType: "night", shiftDate: "" },
        { shift: 2, shiftType: "morning", shiftDate: "" },
        { shift: 3, shiftType: "afternoon", shiftDate: "" },
        { shift: 4, shiftType: "night", shiftDate: "" },
        { shift: 5, shiftType: "morning", shiftDate: "" },
        { shift: 6, shiftType: "afternoon", shiftDate: "" },
      ],
    };
  },
  computed: {
    ...mapState({
      tqMachines: (state) => state.tqBead.machines,
    }),
    columns() {
      return [
        { type: "selection", fixed: "left" },
        {
          prop: "scheduleDate",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.tqNewScheduleResult.scheduleDate"),
          minWidth: 110,
        },
        {
          prop: "cxBatchNo",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.tqNewScheduleResult.cxBatchNo"),
          minWidth: 140,
        },
        {
          prop: "batchNo",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.tqNewScheduleResult.batchNo"),
          minWidth: 140,
        },
        {
          prop: "orderNo",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.tqNewScheduleResult.orderNo"),
          minWidth: 140,
        },
        {
          prop: "beadCode",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.tqNewScheduleResult.beadCode"),
          minWidth: 120,
        },
        {
          prop: "steelRingCode",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.tqNewScheduleResult.steelRingCode"),
          minWidth: 130,
        },
        {
          prop: "triangleGlueCode",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.tqNewScheduleResult.triangleGlueCode"),
          minWidth: 140,
        },
        {
          prop: "proSize",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.tqNewScheduleResult.proSize"),
          minWidth: 80,
        },
        {
          prop: "machineCode",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.tqNewScheduleResult.machineCode"),
          minWidth: 120,
        },
        {
          prop: "monthSurplusQty",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.tqNewScheduleResult.monthSurplusQty"),
          minWidth: 110,
        },
        {
          label: this.getShiftLabel(1),
          children: [
            {
              prop: "class1Sequence",
              align: "center",
              label: this.$t("ui.data.column.tqNewScheduleResult.sequence"),
              minWidth: 70,
            },
            {
              prop: "class1PlanQty",
              align: "center",
              label: this.$t("ui.data.column.tqNewScheduleResult.planQty"),
              minWidth: 70,
            },
            {
              prop: "class1FinishQty",
              align: "center",
              label: this.$t("ui.data.column.tqNewScheduleResult.finishQty"),
              minWidth: 70,
            },
          ],
        },
        {
          label: this.getShiftLabel(2),
          children: [
            {
              prop: "class2Sequence",
              align: "center",
              label: this.$t("ui.data.column.tqNewScheduleResult.sequence"),
              minWidth: 70,
            },
            {
              prop: "class2PlanQty",
              align: "center",
              label: this.$t("ui.data.column.tqNewScheduleResult.planQty"),
              minWidth: 70,
            },
            {
              prop: "class2FinishQty",
              align: "center",
              label: this.$t("ui.data.column.tqNewScheduleResult.finishQty"),
              minWidth: 70,
            },
          ],
        },
        {
          label: this.getShiftLabel(3),
          children: [
            {
              prop: "class3Sequence",
              align: "center",
              label: this.$t("ui.data.column.tqNewScheduleResult.sequence"),
              minWidth: 70,
            },
            {
              prop: "class3PlanQty",
              align: "center",
              label: this.$t("ui.data.column.tqNewScheduleResult.planQty"),
              minWidth: 70,
            },
            {
              prop: "class3FinishQty",
              align: "center",
              label: this.$t("ui.data.column.tqNewScheduleResult.finishQty"),
              minWidth: 70,
            },
          ],
        },
        {
          label: this.getShiftLabel(4),
          children: [
            {
              prop: "class4Sequence",
              align: "center",
              label: this.$t("ui.data.column.tqNewScheduleResult.sequence"),
              minWidth: 70,
            },
            {
              prop: "class4PlanQty",
              align: "center",
              label: this.$t("ui.data.column.tqNewScheduleResult.planQty"),
              minWidth: 70,
            },
            {
              prop: "class4FinishQty",
              align: "center",
              label: this.$t("ui.data.column.tqNewScheduleResult.finishQty"),
              minWidth: 70,
            },
          ],
        },
        {
          label: this.getShiftLabel(5),
          children: [
            {
              prop: "class5Sequence",
              align: "center",
              label: this.$t("ui.data.column.tqNewScheduleResult.sequence"),
              minWidth: 70,
            },
            {
              prop: "class5PlanQty",
              align: "center",
              label: this.$t("ui.data.column.tqNewScheduleResult.planQty"),
              minWidth: 70,
            },
            {
              prop: "class5FinishQty",
              align: "center",
              label: this.$t("ui.data.column.tqNewScheduleResult.finishQty"),
              minWidth: 70,
            },
          ],
        },
        {
          label: this.getShiftLabel(6),
          children: [
            {
              prop: "class6Sequence",
              align: "center",
              label: this.$t("ui.data.column.tqNewScheduleResult.sequence"),
              minWidth: 70,
            },
            {
              prop: "class6PlanQty",
              align: "center",
              label: this.$t("ui.data.column.tqNewScheduleResult.planQty"),
              minWidth: 70,
            },
            {
              prop: "class6FinishQty",
              align: "center",
              label: this.$t("ui.data.column.tqNewScheduleResult.finishQty"),
              minWidth: 70,
            },
          ],
        },
        {
          prop: "isRelease",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.tqNewScheduleResult.isRelease"),
          minWidth: 100,
          formatter: (row, column, value, index) => {
            return this.selectDictLabel(this.dict.type.IS_RELEASE, value);
          },
        },
        {
          prop: "stockQty",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.tqNewScheduleResult.stockQty"),
          minWidth: 80,
        },
        {
          prop: "remark",
          align: "left",
          halign: "center",
          label: this.$t("ui.common.column.remark"),
          minWidth: 140,
        },
      ];
    },
    searchColumns() {
      return [
        {
          label: this.$t("ui.data.column.tqNewScheduleResult.scheduleDate"),
          prop: "scheduleDateQuery",
          type: "date",
          valueFormat: "yyyy-MM-dd",
          listeners: {
            change: this.handleScheduleDateChange,
          },
        },
        {
          label: this.$t("ui.data.column.tqNewScheduleResult.beadCode"),
          prop: "beadCode",
        },
        {
          label: this.$t("ui.data.column.tqNewScheduleResult.proSize"),
          prop: "proSize",
        },
        {
          label: this.$t("ui.data.column.tqNewScheduleResult.triangleGlueCode"),
          prop: "triangleGlueCode",
        },
        {
          label: this.$t("ui.data.column.tqNewScheduleResult.isRelease"),
          prop: "isRelease",
          type: "select",
          dictData: this.dict.type.IS_RELEASE,
          filterable: true,
        },
        {
          label: this.$t("ui.data.column.tqNewScheduleResult.machineCode"),
          prop: "machineCode",
          type: "select",
          dictData: this.tqMachines,
          labelKey: "machineCode",
          valueKey: "machineCode",
          filterable: true,
        },
      ];
    },
  },
  methods: {
    getShiftLabel(shiftIndex) {
      const item = this.dateList[shiftIndex - 1];
      if (!item) return "";
      const shiftNameMap = {
        night: this.$t("ui.data.column.scheduleResult.nightShift"),
        morning: this.$t("ui.data.column.scheduleResult.morningShift"),
        afternoon: this.$t("ui.data.column.scheduleResult.middleShift"),
      };
      const shiftName = shiftNameMap[item.shiftType] || "";
      return shiftName + " " + (item.shiftDate || "");
    },
    handleScheduleDateChange(val) {
      this.query.scheduleDateQuery = val;
      this.getDate();
    },
    async getDate() {
      try {
        let res = await listScheduleShiftDates({
          scheduleDateQuery: this.query.scheduleDateQuery || this.search.scheduleDateQuery,
        });
        if (res && res.length > 0) {
          this.dateList = res;
        }
      } catch (error) {
        console.error(error);
      }
    },
    handleAutoPlan() {
      this.$refs.autoPlanDialogRef.show({
        scheduleDateQuery: this.query.scheduleDateQuery || this.search.scheduleDateQuery,
      });
    },
    handleInsertOrder() {
      this.$refs.insertOrderDialog.show();
    },
    handleChangeMachine() {
      if (this.selection.length !== 1) {
        this.$modal.msgWarning(this.$t("common.tip.selectOne"));
        return;
      }
      this.$refs.changeMachineDialog.show(this.selection[0]);
    },
    handleAdjustQty() {
      if (this.selection.length !== 1) {
        this.$modal.msgWarning(this.$t("common.tip.selectOne"));
        return;
      }
      this.$refs.adjustQtyDialog.show(this.selection[0]);
    },
    handleRelease() {
      if (!this.selection || this.selection.length === 0) {
        this.$modal.msgWarning(this.$t("common.tip.selectOne"));
        return;
      }
      this.$confirm(this.$t("ui.data.btn.tqNewScheduleResult.release") + "?", {
        type: "warning",
      }).then(() => {
        publishSchedule({ scheduleDateQuery: this.query.scheduleDateQuery || this.search.scheduleDateQuery }).then((res) => {
          this.$modal.msgSuccess(res.msg);
          this.getList();
        });
      }).catch(() => {});
    },
    handleBatchDelete() {
      if (!this.selection || this.selection.length === 0) {
        this.$modal.msgWarning(this.$t("common.tip.selectOne"));
        return;
      }
      const ids = this.selection.map(item => item.id);
      this.$confirm(this.$t("common.confirm.delete"), {
        type: "warning",
      }).then(() => {
        logicDeleteNewScheduleResult(ids).then((data) => {
          this.$modal.msgSuccess(data.msg);
          this.$set(this.page, "current", 1);
          this.getList();
        });
      });
    },
    handleExport() {
      downloadLink("/tq/newScheduleResult/export", this.formatParams(false));
    },
    handleSearch(data) {
      this.query = { ...data };
      this.$set(this.page, "current", 1);
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
          isAsc: order == "ascending" ? "asc" : "desc",
        };
      } else {
        this.sort = {};
      }
      this.getList();
    },
    handleSelectionChange(rows) {
      this.selection = rows;
    },
    formatParams(hasPage = true) {
      const params = {
        ...this.query,
        ...this.sort,
      };
      if (!params.orderByColumn) {
        params.orderByColumn = "scheduleDate,machineCode,class1Sequence";
        params.isAsc = "desc,asc,asc";
      }
      if (hasPage) {
        params.pageSize = this.page.pageSize;
        params.pageNum = this.page.current;
      }
      return params;
    },
    async getList() {
      try {
        this.loading = true;
        await this.getDate();
        const data = await listNewScheduleResult(this.formatParams());
        this.data = data.rows;
        this.page.total = data.total;
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
    },
  },
  activated() {
    this.$store.dispatch("tqBead/getMachineList");
    this.getList();
  },
};
</script>
<style lang="scss" scoped>
</style>
