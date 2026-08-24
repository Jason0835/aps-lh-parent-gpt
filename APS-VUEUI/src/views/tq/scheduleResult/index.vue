<template>
  <basic-container>
    <page-table
      tableRef="tqScheduleResultMainTable"
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
          v-hasPermi="['tq:scheduleResult:autoPlan']"
          @click="handleAutoPlan"
        >{{ $t("ui.data.btn.tqScheduleResult.autoPlan") }}</el-button>
        <el-button
          type="warning"
          plain
          v-hasPermi="['tq:scheduleResult:insertOrder']"
          @click="handleInsertOrder"
        >{{ $t("ui.data.btn.tqScheduleResult.insertOrder") }}</el-button>
        <el-button
          type="danger"
          v-hasPermi="['tq:scheduleResult:remove']"
          @click="handleBatchDelete"
          :disabled="selection.length == 0"
        >{{ $t("ui.frame.btn.delete") }}</el-button>
        <el-button
          type="primary"
          plain
          v-hasPermi="['tq:scheduleResult:changeMachine']"
          @click="handleChangeMachine"
          :disabled="selection.length == 0"
        >{{ $t("ui.data.btn.tqScheduleResult.changeMachine") }}</el-button>
        <el-button
          type="primary"
          plain
          v-hasPermi="['tq:scheduleResult:adjustQty']"
          @click="handleAdjustQty"
          :disabled="selection.length == 0"
        >{{ $t("ui.data.btn.tqScheduleResult.adjustQty") }}</el-button>
        <el-button
          type="primary"
          plain
          v-hasPermi="['tq:scheduleResult:changeMachine']"
          @click="handleBatchChangeMachine"
          :disabled="selection.length < 2"
        >{{ $t("ui.data.btn.tqScheduleResult.batchChangeMachine") }}</el-button>
        <el-button
          type="primary"
          plain
          v-hasPermi="['tq:scheduleResult:adjustQty']"
          @click="handleBatchChangeQty"
          :disabled="selection.length < 2"
        >{{ $t("ui.data.btn.tqScheduleResult.batchChangeQty") }}</el-button>
        <el-button
          type="success"
          plain
          v-hasPermi="['tq:scheduleResult:publish']"
          @click="handleRelease"
          :disabled="selection.length == 0"
        >{{ $t("ui.data.btn.tqScheduleResult.publish") }}</el-button>
        <el-button
          v-hasPermi="['tq:scheduleResult:import']"
          @click="handleImport"
        >{{ $t("ui.frame.btn.import") }}</el-button>
        <el-button
          @click="handleExport"
          v-hasPermi="['tq:scheduleResult:export']"
        >{{ $t("ui.frame.btn.export") }}</el-button>
      </template>
    </page-table>
    <tlt-upload-form
      ref="tltUpload"
      :updateSupport="true"
      :download-url-formatter="(form) => handleTemplateDownload('/tq/scheduleResult/importTemplateCust', form)"
      downloadUrl="/tq/scheduleResult/importTemplateCust"
      uploadUrl="/tq/scheduleResult/importDataCust"
      @uploadSuccess="getList"
      labelWidth="0"
      :columns="importColumns"
    ></tlt-upload-form>
    <!-- 自动排程弹窗 -->
    <auto-plan-dialog ref="autoPlanDialog" @success="getList" />
    <!-- 插单弹窗 -->
    <insert-order-dialog ref="insertOrderDialog" @success="getList" />
    <!-- 转机台弹窗 -->
    <change-machine-dialog ref="changeMachineDialog" @success="getList" />
    <!-- 调量弹窗 -->
    <adjust-qty-dialog ref="adjustQtyDialog" @success="getList" />
  </basic-container>
</template>
<script>
import { downloadLink } from "@/utils/request";
import { listScheduleResult, logicDeleteScheduleResult, batchDelete, batchChangeMachine, batchChangeQty, listScheduleShiftDates, autoPlan, insertOrder, changeMachine, changeQty, publishSchedule } from "@/api/tq/scheduleResult";
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
  // 组件 name 必须与动态路由 name（菜单 PATH 首字母大写）一致，否则 keep-alive 缓存失效，切 tab 后查询条件与数据被重置
  name: "TqScheduleResult",
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
        factoryCode: "116",
      },
      query: {
        scheduleDateQuery: getOffsetDate(1),
        factoryCode: "116",
      },
      dateList: [
        { shift: 1, shiftType: "afternoon", shiftDate: "" },
        { shift: 2, shiftType: "night", shiftDate: "" },
        { shift: 3, shiftType: "morning", shiftDate: "" },
        { shift: 4, shiftType: "afternoon", shiftDate: "" },
        { shift: 5, shiftType: "night", shiftDate: "" },
        { shift: 6, shiftType: "morning", shiftDate: "" },
      ],
      // keep-alive 首次激活标志，避免 created 与 activated 同时触发重复请求
      pageActivatedOnce: false,
    };
  },
  computed: {
    ...mapState({
      tqMachines: (state) => state.tqBead.machines,
    }),
    // 导入弹窗列配置放在 computed 中，确保 this.dict 已初始化（data() 执行时字典 mixin 尚未注入 dict）
    // 工厂与排程日期不在弹窗中选择，由列表页查询条件带入（见 handleImport 方法）
    importColumns() {
      return [
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
      ];
    },
    columns() {
      return [
        { type: "selection", fixed: "left" },
        {
          prop: "factoryCode",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.tqScheduleResult.factoryCode"),
          minWidth: 100,
          formatter: (row, column, value, index) => {
            return this.selectDictLabel(this.dict.type.biz_factory_name, value);
          },
        },
        {
          prop: "scheduleDate",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.tqScheduleResult.scheduleDate"),
          minWidth: 110,
        },
        {
          prop: "cxBatchNo",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.tqScheduleResult.cxBatchNo"),
          minWidth: 140,
        },
        {
          prop: "batchNo",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.tqScheduleResult.batchNo"),
          minWidth: 140,
        },
        {
          prop: "orderNo",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.tqScheduleResult.orderNo"),
          minWidth: 140,
        },
        {
          prop: "beadCode",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.tqScheduleResult.beadCode"),
          minWidth: 120,
        },
        {
          prop: "steelRingCode",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.tqScheduleResult.steelRingCode"),
          minWidth: 130,
        },
        {
          prop: "triangleGlueCode",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.tqScheduleResult.triangleGlueCode"),
          minWidth: 140,
        },
        {
          prop: "proSize",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.tqScheduleResult.proSize"),
          minWidth: 80,
        },
        {
          prop: "machineCode",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.tqScheduleResult.machineCode"),
          minWidth: 120,
        },
        {
          prop: "monthSurplusQty",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.tqScheduleResult.monthSurplusQty"),
          minWidth: 110,
        },
        {
          prop: "stockQty",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.tqScheduleResult.stockQty"),
          minWidth: 80,
        },
        {
          label: this.getShiftLabel(1),
          children: [
            {
              prop: "class1Sequence",
              align: "center",
              label: this.$t("ui.data.column.tqScheduleResult.sequence"),
              minWidth: 70,
            },
            {
              prop: "class1PlanQty",
              align: "center",
              label: this.$t("ui.data.column.tqScheduleResult.planQty"),
              minWidth: 70,
            },
            {
              prop: "class1FinishQty",
              align: "center",
              label: this.$t("ui.data.column.tqScheduleResult.finishQty"),
              minWidth: 70,
            },
            {
              prop: "class1Analysis",
              align: "center",
              label: this.$t("ui.data.column.tqScheduleResult.analysis"),
              minWidth: 100,
            },
          ],
        },
        {
          label: this.getShiftLabel(2),
          children: [
            {
              prop: "class2Sequence",
              align: "center",
              label: this.$t("ui.data.column.tqScheduleResult.sequence"),
              minWidth: 70,
            },
            {
              prop: "class2PlanQty",
              align: "center",
              label: this.$t("ui.data.column.tqScheduleResult.planQty"),
              minWidth: 70,
            },
            {
              prop: "class2FinishQty",
              align: "center",
              label: this.$t("ui.data.column.tqScheduleResult.finishQty"),
              minWidth: 70,
            },
            {
              prop: "class2Analysis",
              align: "center",
              label: this.$t("ui.data.column.tqScheduleResult.analysis"),
              minWidth: 100,
            },
          ],
        },
        {
          label: this.getShiftLabel(3),
          children: [
            {
              prop: "class3Sequence",
              align: "center",
              label: this.$t("ui.data.column.tqScheduleResult.sequence"),
              minWidth: 70,
            },
            {
              prop: "class3PlanQty",
              align: "center",
              label: this.$t("ui.data.column.tqScheduleResult.planQty"),
              minWidth: 70,
            },
            {
              prop: "class3FinishQty",
              align: "center",
              label: this.$t("ui.data.column.tqScheduleResult.finishQty"),
              minWidth: 70,
            },
            {
              prop: "class3Analysis",
              align: "center",
              label: this.$t("ui.data.column.tqScheduleResult.analysis"),
              minWidth: 100,
            },
          ],
        },
        {
          label: this.getShiftLabel(4),
          children: [
            {
              prop: "class4Sequence",
              align: "center",
              label: this.$t("ui.data.column.tqScheduleResult.sequence"),
              minWidth: 70,
            },
            {
              prop: "class4PlanQty",
              align: "center",
              label: this.$t("ui.data.column.tqScheduleResult.planQty"),
              minWidth: 70,
            },
            {
              prop: "class4FinishQty",
              align: "center",
              label: this.$t("ui.data.column.tqScheduleResult.finishQty"),
              minWidth: 70,
            },
            {
              prop: "class4Analysis",
              align: "center",
              label: this.$t("ui.data.column.tqScheduleResult.analysis"),
              minWidth: 100,
            },
          ],
        },
        {
          label: this.getShiftLabel(5),
          children: [
            {
              prop: "class5Sequence",
              align: "center",
              label: this.$t("ui.data.column.tqScheduleResult.sequence"),
              minWidth: 70,
            },
            {
              prop: "class5PlanQty",
              align: "center",
              label: this.$t("ui.data.column.tqScheduleResult.planQty"),
              minWidth: 70,
            },
            {
              prop: "class5FinishQty",
              align: "center",
              label: this.$t("ui.data.column.tqScheduleResult.finishQty"),
              minWidth: 70,
            },
            {
              prop: "class5Analysis",
              align: "center",
              label: this.$t("ui.data.column.tqScheduleResult.analysis"),
              minWidth: 100,
            },
          ],
        },
        {
          label: this.getShiftLabel(6),
          children: [
            {
              prop: "class6Sequence",
              align: "center",
              label: this.$t("ui.data.column.tqScheduleResult.sequence"),
              minWidth: 70,
            },
            {
              prop: "class6PlanQty",
              align: "center",
              label: this.$t("ui.data.column.tqScheduleResult.planQty"),
              minWidth: 70,
            },
            {
              prop: "class6FinishQty",
              align: "center",
              label: this.$t("ui.data.column.tqScheduleResult.finishQty"),
              minWidth: 70,
            },
            {
              prop: "class6Analysis",
              align: "center",
              label: this.$t("ui.data.column.tqScheduleResult.analysis"),
              minWidth: 100,
            },
          ],
        },
        {
          prop: "releaseStatus",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.tqScheduleResult.releaseStatus"),
          minWidth: 100,
          formatter: (row, column, value, index) => {
            return this.selectDictLabel(this.dict.type.IS_RELEASE, value);
          },
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
          label: this.$t("ui.data.column.tqScheduleResult.factoryCode"),
          prop: "factoryCode",
          type: "select",
          dictData: this.dict.type.biz_factory_name,
          filterable: true,
        },
        {
          label: this.$t("ui.data.column.tqScheduleResult.scheduleDate"),
          prop: "scheduleDateQuery",
          type: "date",
          valueFormat: "yyyy-MM-dd",
          listeners: {
            change: this.handleScheduleDateChange,
          },
        },
        {
          label: this.$t("ui.data.column.tqScheduleResult.beadCode"),
          prop: "beadCode",
        },
        {
          label: this.$t("ui.data.column.tqScheduleResult.proSize"),
          prop: "proSize",
        },
        {
          label: this.$t("ui.data.column.tqScheduleResult.triangleGlueCode"),
          prop: "triangleGlueCode",
        },
        {
          label: this.$t("ui.data.column.tqScheduleResult.releaseStatus"),
          prop: "releaseStatus",
          type: "select",
          dictData: this.dict.type.IS_RELEASE,
          filterable: true,
        },
        {
          label: this.$t("ui.data.column.tqScheduleResult.machineCode"),
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
      this.$refs.autoPlanDialog.show({
        scheduleDateQuery: this.query.scheduleDateQuery || this.search.scheduleDateQuery,
      });
    },
    handleInsertOrder() {
      if (this.selection.length !== 1) {
        this.$modal.msgWarning(this.$t("ui.placeholder.selectTableRow"));
        return;
      }
      this.$refs.insertOrderDialog.show(this.selection[0]);
    },
    handleChangeMachine() {
      if (this.selection.length !== 1) {
        this.$modal.msgWarning(this.$t("ui.placeholder.selectTableRow"));
        return;
      }
      this.$refs.changeMachineDialog.show(this.selection[0]);
    },
    handleAdjustQty() {
      if (this.selection.length !== 1) {
        this.$modal.msgWarning(this.$t("ui.placeholder.selectTableRow"));
        return;
      }
      this.$refs.adjustQtyDialog.show(this.selection[0]);
    },
    handleRelease() {
      if (!this.selection || this.selection.length === 0) {
        this.$modal.msgWarning(this.$t("ui.placeholder.selectTableRow"));
        return;
      }
      this.$confirm(this.$t("ui.biz.alter.makeSurePublish"), {
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
        this.$modal.msgWarning(this.$t("ui.placeholder.selectTableRow"));
        return;
      }
      const ids = this.selection.map(item => item.id);
      this.$confirm(this.$t("common.confirm.delete"), {
        type: "warning",
      }).then(() => {
        batchDelete(ids).then((data) => {
          this.$modal.msgSuccess(data.msg);
          this.$set(this.page, "current", 1);
          this.getList();
        });
      });
    },
    /** 批量转机台（走任务链路径，支持锚点、目标班次） */
    handleBatchChangeMachine() {
      if (!this.selection || this.selection.length === 0) {
        this.$modal.msgWarning(this.$t("ui.placeholder.selectTableRow"));
        return;
      }
      // 复用现有转机台对话框，传入选中列表的第一条作为模板
      // 批量提交时在对话框确认后调用 batchChangeMachine
      this.$refs.changeMachineDialog.show(this.selection[0], this.selection);
    },
    /** 批量调量（走任务链路径） */
    handleBatchChangeQty() {
      if (!this.selection || this.selection.length === 0) {
        this.$modal.msgWarning(this.$t("ui.placeholder.selectTableRow"));
        return;
      }
      this.$refs.adjustQtyDialog.show(this.selection[0], this.selection);
    },
    /** 打开导入弹窗：工厂与排程日期由列表页查询条件带入，无需在弹窗中重复选择 */
    handleImport() {
      this.$refs.tltUpload.handleImport({
        factoryCode: this.query.factoryCode,
        scheduleDate: this.query.scheduleDateQuery,
        updateSupport: false,
      });
    },
    handleExport() {
      if (!this.query.factoryCode || !this.query.scheduleDateQuery) {
        this.$modal.msgWarning(this.$t("ui.data.alert.tq.schedule.excel.factoryDateRequired"));
        return;
      }
      downloadLink("/tq/scheduleResult/export", {
        factoryCode: this.query.factoryCode,
        scheduleDate: this.query.scheduleDateQuery,
        machineCode: this.query.machineCode,
        beadCode: this.query.beadCode,
      });
    },
    handleTemplateDownload(url, formValues) {
      const params = {
        ...formValues,
        exportTemplate: true,
      };
      const paramsStr = Object.keys(params)
        .filter(key => params[key] !== undefined && params[key] !== null && params[key] !== "")
        .map(key => `${encodeURIComponent(key)}=${encodeURIComponent(params[key])}`)
        .join("&");
      return `${url}${paramsStr ? "?" + paramsStr : ""}`;
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
      // 默认排序由后端 wrapper 处理，前端仅在用户主动点列头排序时传递单列参数
      // 避免多列逗号分隔参数与 RuoYi PageDomain.getOrderBy() 不兼容导致 SQL 语法错误
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
        const data = await listScheduleResult(this.formatParams());
        this.data = data.rows;
        this.page.total = data.total;
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
    },
  },
  created() {
    // 首次进入页面时初始化班次日期并加载数据
    this.$store.dispatch("tqBead/getMachineList");
    this.getList();
  },
  activated() {
    // keep-alive 首次激活不重复请求（created 已加载），后续重新进入页面时刷新机台列表与数据
    if (this.pageActivatedOnce) {
      this.$store.dispatch("tqBead/getMachineList");
      this.getList();
      return;
    }
    this.pageActivatedOnce = true;
  },
};
</script>
<style lang="scss" scoped>
</style>
