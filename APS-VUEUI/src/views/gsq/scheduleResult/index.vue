<template>
  <basic-container>
    <page-table
      tableRef="gsqScheduleResultMainTable"
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
      :showSummary="true"
      :selectArea="false"
      :row-style="rowStyle"
      :cell-style="cellStyle"
      :summary-method="getSummaries"
    >
      <template slot="header">
        <el-button
          type="warning"
          v-hasPermi="['gsq:scheduleResult:autoPlan']"
          @click="handleAutoPlan"
        >{{ $t("ui.data.column.scheduleResult.autoPlan") }}</el-button>
        <el-button
          type="success"
          v-hasPermi="['gsq:scheduleResult:insertOrder']"
          @click="handleInsertOrder"
        >{{ $t("ui.data.column.scheduleResult.insertOrder") }}</el-button>
        <el-button
          type="primary"
          class="single disabled"
          @click="handleEdit(selection[0])"
          v-hasPermi="['gsq:scheduleResult:edit']"
        >{{ $t("ui.frame.btn.modify") }}</el-button>
        <el-button
          type="danger"
          class="multiple disabled"
          :disabled="selection.length === 0"
          v-hasPermi="['gsq:scheduleResult:remove']"
          @click="handleDelete"
        >{{ $t("ui.frame.btn.delete") }}</el-button>
        <el-button
          v-hasPermi="['gsq:scheduleResult:changeMachine']"
          type="primary"
          class="single disabled"
          :disabled="selection.length !== 1"
          @click="handleChangeMachine(selection[0])"
        >{{ $t("ui.data.column.scheduleResult.changeMachine") }}</el-button>
        <el-button
          v-hasPermi="['gsq:scheduleResult:changeQty']"
          type="primary"
          class="single disabled"
          @click="handleChangeQty(selection[0])"
        >{{ $t("ui.data.column.scheduleResult.changePlan") }}</el-button>
        <el-button
          v-hasPermi="['gsq:scheduleResult:changeMachine']"
          type="primary"
          plain
          :disabled="selection.length < 2"
          @click="handleBatchChangeMachine"
        >{{ $t("ui.data.btn.gsqScheduleResult.batchChangeMachine") }}</el-button>
        <el-button
          v-hasPermi="['gsq:scheduleResult:changeQty']"
          type="primary"
          plain
          :disabled="selection.length < 2"
          @click="handleBatchChangeQty"
        >{{ $t("ui.data.btn.gsqScheduleResult.batchChangeQty") }}</el-button>
        <el-button
          v-hasPermi="['gsq:scheduleResult:publish']"
          type="primary"
          :disabled="selection.length === 0"
          @click="handlePublish"
        >{{ $t("ui.data.btn.gsqScheduleResult.publish") }}</el-button>
        <el-dropdown>
          <el-button type="primary" style="margin-left: 10px">
            {{ $t("ui.frame.btn.more") }}<i class="el-icon-arrow-down el-icon--right"></i>
          </el-button>
          <el-dropdown-menu slot="dropdown">
            <el-dropdown-item>
              <el-button type="primary" class="more-btn" @click="handleExport">
                {{ $t("ui.frame.btn.export") }}
              </el-button>
            </el-dropdown-item>
            <el-dropdown-item v-hasPermi="['gsq:scheduleResult:import']">
              <el-button
                type="primary"
                class="more-btn"
                @click="$refs.tltUploadForm.handleImport(importDefaultValue)"
              >
                {{ $t("ui.frame.btn.import") }}
              </el-button>
            </el-dropdown-item>
          </el-dropdown-menu>
        </el-dropdown>
      </template>
    </page-table>

    <!-- 自动排程弹窗 -->
    <auto-plan-dialog
      v-if="autoPlanVisible"
      :visible.sync="autoPlanVisible"
      :default-factory-code="search.factoryCode"
      :default-schedule-date="search.scheduleDate"
      @refresh="getList"
    />

    <!-- 插单弹窗 -->
    <insert-order-dialog
      v-if="insertOrderVisible"
      :visible.sync="insertOrderVisible"
      @refresh="getList"
    />

    <!-- 修改弹窗 -->
    <edit-dialog
      v-if="editVisible"
      :visible.sync="editVisible"
      :row="currentRow"
      @refresh="getList"
    />

    <!-- 转机台弹窗 -->
    <change-machine-dialog
      ref="changeMachineDialog"
      @success="getList"
    />

    <!-- 调量弹窗 -->
    <change-qty-dialog
      v-if="changeQtyVisible"
      :visible.sync="changeQtyVisible"
      :row="currentRow"
      @refresh="getList"
    />

    <!-- 导入弹窗（按专用模板导入） -->
    <tlt-upload-form
      ref="tltUploadForm"
      :importDialogVisible.sync="importDialogVisible"
      :updateSupport="true"
      :download-url-formatter="(form) => handleTemplateDownload('/gsq/scheduleResult/importTemplateCust', form)"
      downloadUrl="/gsq/scheduleResult/importTemplateCust"
      uploadUrl="/gsq/scheduleResult/importDataCust"
      @uploadSuccess="getList"
      :columns="importColumns"
      :rules="importRules"
    />
  </basic-container>
</template>

<script>
import moment from "moment";
import { mapGetters } from "vuex";
import { downloadLink } from "@/utils/request";
import {
  listScheduleResult,
  removeScheduleResult,
  logicDeleteScheduleResult,
  batchDelete,
  batchChangeMachine,
  batchChangeQty,
  publishSchedule,
  listScheduleShiftDates,
} from "@/api/gsq/scheduleResult";
import TltUploadForm from "@/views/components/tltUploadForm.vue";
import autoPlanDialog from "./components/autoPlanDialog.vue";
import insertOrderDialog from "./components/insertOrderDialog.vue";
import editDialog from "./components/editDialog.vue";
import changeMachineDialog from "./components/changeMachineDialog.vue";
import changeQtyDialog from "./components/changeQtyDialog.vue";

export default {
  name: "GsqScheduleResult",
  components: {
    TltUploadForm,
    autoPlanDialog,
    insertOrderDialog,
    editDialog,
    changeMachineDialog,
    changeQtyDialog,
  },
  dicts: ["biz_factory_name", "IS_RELEASE"],
  data() {
    return {
      loading: false,
      data: [],
      // keep-alive 首次激活守卫：避免 created 与 activated 重复请求
      pageActivatedOnce: false,
      page: {
        pageNum: 1,
        pageSize: 50,
        total: 0,
      },
      search: {
        scheduleDate: moment().add(1, "days").format("YYYY-MM-DD"),
        factoryCode: "116",
        steelRingCode: "",
        isRelease: "",
        machineCode: "",
      },
      selection: [],
      currentRow: null,
      // 弹窗显示控制
      autoPlanVisible: false,
      insertOrderVisible: false,
      editVisible: false,
      changeQtyVisible: false,
      importDialogVisible: false,
      // 6班次日期展示（D日中班/D+1日夜早中/D+2日夜早）
      // 默认值与 tq 保持一致，未拿到数据时也能显示班次类型
      shiftDateList: [
        { shift: 1, shiftType: "afternoon", shiftDate: "" },
        { shift: 2, shiftType: "night", shiftDate: "" },
        { shift: 3, shiftType: "morning", shiftDate: "" },
        { shift: 4, shiftType: "afternoon", shiftDate: "" },
        { shift: 5, shiftType: "night", shiftDate: "" },
        { shift: 6, shiftType: "morning", shiftDate: "" },
      ],
      // 导入默认值
      importDefaultValue: {
        factoryCode: "",
        scheduleDate: moment().add(1, "days").format("YYYY-MM-DD"),
      },
      };
  },
  computed: {
    ...mapGetters(["permissions"]),
    /** 导入弹窗列配置（放在 computed 中，确保 this.dict 已初始化；data() 执行时字典 mixin 尚未注入 dict） */
    importColumns() {
      return [
        {
          label: this.$t("ui.data.column.gsqScheduleResult.factoryCode"),
          prop: "factoryCode",
          type: "select",
          dictData: this.dict.type.biz_factory_name,
          filterable: true,
          clearable: false,
        },
        {
          label: this.$t("ui.data.column.gsqScheduleResult.scheduleDate"),
          prop: "scheduleDate",
          type: "date",
          dateType: "date",
          valueFormat: "yyyy-MM-dd",
          clearable: false,
        },
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
    /** 导入校验规则 */
    importRules() {
      return {
        factoryCode: [
          {
            required: true,
            message: this.$t("ui.data.alert.gsq.schedule.excel.factoryRequired"),
            trigger: "blur",
          },
        ],
        scheduleDate: [
          {
            required: true,
            message: this.$t("ui.data.alert.gsq.schedule.excel.dateRequired"),
            trigger: "blur",
          },
        ],
      };
    },
    searchColumns() {
      return [
        {
          label: this.$t("ui.data.column.gsqScheduleResult.factoryCode"),
          prop: "factoryCode",
          type: "select",
          dictData: this.dict.type.biz_factory_name,
          filterable: true,
        },
        {
          label: this.$t("ui.data.column.gsqScheduleResult.scheduleDate"),
          prop: "scheduleDate",
          component: "el-date-picker",
          type: "date",
          valueFormat: "yyyy-MM-dd",
          // 与 tq 一致：日期变更时即时刷新6班次表头
          listeners: { change: this.handleScheduleDateChange },
        },
        {
          label: this.$t("ui.data.column.gsqScheduleResult.steelRingCode"),
          prop: "steelRingCode",
          component: "el-input",
        },
        {
          label: this.$t("ui.data.column.gsqScheduleResult.isRelease"),
          prop: "isRelease",
          component: "el-select",
          filterable: true,
          options: this.dict.type.IS_RELEASE,
        },
        {
          label: this.$t("ui.data.column.gsqScheduleResult.machineCode"),
          prop: "machineCode",
          component: "el-input",
        },
      ];
    },
    columns() {
      return [
        { type: "selection", width: 50, fixed: "left" },
        // factoryCode 必须放在第一列
        {
          label: this.$t("ui.data.column.gsqScheduleResult.factoryCode"),
          prop: "factoryCode",
          width: 100,
          fixed: "left",
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_factory_name, value);
          },
        },
        {
          label: this.$t("ui.data.column.gsqScheduleResult.scheduleDate"),
          prop: "scheduleDate",
          width: 110,
          fixed: "left",
          formatter: (row, column, value) => {
            return value ? moment(value).format("YYYY-MM-DD") : "";
          },
        },
        {
          label: this.$t("ui.data.column.gsqScheduleResult.tqBatchNo"),
          prop: "tqBatchNo",
          width: 140,
        },
        {
          label: this.$t("ui.data.column.gsqScheduleResult.batchNo"),
          prop: "batchNo",
          width: 140,
        },
        {
          label: this.$t("ui.data.column.gsqScheduleResult.steelRingCode"),
          prop: "steelRingCode",
          width: 140,
        },
        {
          label: this.$t("ui.data.column.gsqScheduleResult.twiningDiscCode"),
          prop: "twiningDiscCode",
          width: 120,
        },
        {
          label: this.$t("ui.data.column.gsqScheduleResult.proSize"),
          prop: "proSize",
          width: 80,
        },
        {
          label: this.$t("ui.data.column.gsqScheduleResult.machineCode"),
          prop: "machineCode",
          width: 110,
        },
        {
          label: this.$t("ui.data.column.gsqScheduleResult.monthSurplusQty"),
          prop: "monthSurplusQty",
          width: 100,
        },
        {
          label: this.$t("ui.data.column.gsqScheduleResult.stockQty"),
          prop: "stockQty",
          width: 90,
        },
        // ===== 6班次动态列（含日期头，与 tq 写法对齐） =====
        {
          label: this.getShiftLabel(1),
          children: [
            { label: this.$t("ui.data.column.gsqScheduleResult.sequence"), prop: "class1Sequence", width: 80 },
            { label: this.$t("ui.data.column.gsqScheduleResult.planQty"), prop: "class1PlanQty", width: 90 },
            { label: this.$t("ui.data.column.gsqScheduleResult.finishQty"), prop: "class1FinishQty", width: 90 },
            { label: this.$t("ui.data.column.gsqScheduleResult.handAnalysis"), prop: "class1HandAnalysis", width: 130 },
          ],
        },
        {
          label: this.getShiftLabel(2),
          children: [
            { label: this.$t("ui.data.column.gsqScheduleResult.sequence"), prop: "class2Sequence", width: 80 },
            { label: this.$t("ui.data.column.gsqScheduleResult.planQty"), prop: "class2PlanQty", width: 90 },
            { label: this.$t("ui.data.column.gsqScheduleResult.finishQty"), prop: "class2FinishQty", width: 90 },
            { label: this.$t("ui.data.column.gsqScheduleResult.handAnalysis"), prop: "class2HandAnalysis", width: 130 },
          ],
        },
        {
          label: this.getShiftLabel(3),
          children: [
            { label: this.$t("ui.data.column.gsqScheduleResult.sequence"), prop: "class3Sequence", width: 80 },
            { label: this.$t("ui.data.column.gsqScheduleResult.planQty"), prop: "class3PlanQty", width: 90 },
            { label: this.$t("ui.data.column.gsqScheduleResult.finishQty"), prop: "class3FinishQty", width: 90 },
            { label: this.$t("ui.data.column.gsqScheduleResult.handAnalysis"), prop: "class3HandAnalysis", width: 130 },
          ],
        },
        {
          label: this.getShiftLabel(4),
          children: [
            { label: this.$t("ui.data.column.gsqScheduleResult.sequence"), prop: "class4Sequence", width: 80 },
            { label: this.$t("ui.data.column.gsqScheduleResult.planQty"), prop: "class4PlanQty", width: 90 },
            { label: this.$t("ui.data.column.gsqScheduleResult.finishQty"), prop: "class4FinishQty", width: 90 },
            { label: this.$t("ui.data.column.gsqScheduleResult.handAnalysis"), prop: "class4HandAnalysis", width: 130 },
          ],
        },
        {
          label: this.getShiftLabel(5),
          children: [
            { label: this.$t("ui.data.column.gsqScheduleResult.sequence"), prop: "class5Sequence", width: 80 },
            { label: this.$t("ui.data.column.gsqScheduleResult.planQty"), prop: "class5PlanQty", width: 90 },
            { label: this.$t("ui.data.column.gsqScheduleResult.finishQty"), prop: "class5FinishQty", width: 90 },
            { label: this.$t("ui.data.column.gsqScheduleResult.handAnalysis"), prop: "class5HandAnalysis", width: 130 },
          ],
        },
        {
          label: this.getShiftLabel(6),
          children: [
            { label: this.$t("ui.data.column.gsqScheduleResult.sequence"), prop: "class6Sequence", width: 80 },
            { label: this.$t("ui.data.column.gsqScheduleResult.planQty"), prop: "class6PlanQty", width: 90 },
            { label: this.$t("ui.data.column.gsqScheduleResult.finishQty"), prop: "class6FinishQty", width: 90 },
            { label: this.$t("ui.data.column.gsqScheduleResult.handAnalysis"), prop: "class6HandAnalysis", width: 130 },
          ],
        },
        // ===== 状态/操作列 =====
        {
          label: this.$t("ui.data.column.gsqScheduleResult.isRelease"),
          prop: "isRelease",
          width: 90,
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.IS_RELEASE, value);
          },
        },
        {
          label: this.$t("ui.data.column.gsqScheduleResult.remark"),
          prop: "remark",
          width: 150,
        }
      ];
    },
  },
  created() {
    // 首次进入页面时加载数据（getList 内部会先 await getDate() 拉取班次日期）
    this.getList();
  },
  activated() {
    // keep-alive 首次激活不重复请求（created 已加载），后续重新进入页面时刷新数据
    if (this.pageActivatedOnce) {
      this.getList();
      return;
    }
    this.pageActivatedOnce = true;
  },
  methods: {
    /**
     * 获取班次列表头标签（班次名 + 日期）
     * 与 tq 写法保持一致：未拿到数据时显示默认班次类型，拿到数据后显示"班次名 日期"
     * @param shiftIndex 班次序号 1~6
     */
    getShiftLabel(shiftIndex) {
      const item = this.shiftDateList[shiftIndex - 1];
      if (!item) return "";
      // 多语言 key 与 tq 共用通用 key
      const shiftNameMap = {
        night: this.$t("ui.data.column.scheduleResult.nightShift"),
        morning: this.$t("ui.data.column.scheduleResult.morningShift"),
        afternoon: this.$t("ui.data.column.scheduleResult.middleShift"),
      };
      const shiftName = shiftNameMap[item.shiftType] || "";
      return shiftName + " " + (item.shiftDate || "");
    },
    /** 加载6班次日期（拦截器已返回数组本身，无需再 .data） */
    async getDate() {
      try {
        const res = await listScheduleShiftDates({
          scheduleDate: this.search.scheduleDate,
        });
        if (Array.isArray(res) && res.length > 0) {
          this.shiftDateList = res;
        }
      } catch (error) {
        console.error(error);
      }
    },
    /** 查询列表（先 await getDate() 保证表头先就绪，与 tq 一致） */
    async getList() {
      this.loading = true;
      try {
        await this.getDate();
        const params = {
          ...this.search,
          scheduleDateQuery: this.search.scheduleDate,
          pageNum: this.page.pageNum,
          pageSize: this.page.pageSize,
        };
        const res = await listScheduleResult(params);
        this.data = res.rows || [];
        this.page.total = res.total || 0;
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
    },
    handleSearch() {
      this.page.pageNum = 1;
      this.getList();
    },
    /** 排程日期变更：同步到 search.scheduleDate 并即时刷新6班次日期 */
    handleScheduleDateChange(val) {
      this.search.scheduleDate = val;
      this.getDate();
    },
    handlePageChange(val) {
      this.page.pageNum = val.pageNum;
      this.page.pageSize = val.pageSize;
      this.getList();
    },
    handleSortChange() {
      this.getList();
    },
    handleSelectionChange(val) {
      this.selection = val;
    },
    /** 自动排程 */
    handleAutoPlan() {
      this.autoPlanVisible = true;
    },
    /** 插单 */
    handleInsertOrder() {
      this.insertOrderVisible = true;
    },
    /** 修改 */
    handleEdit(row) {
      if (!row) return;
      this.currentRow = row;
      this.editVisible = true;
    },
    /** 删除（走任务链路径，删除后resequence重排） */
    handleDelete() {
      if (this.selection.length === 0) {
        this.$modal.msgWarning(this.$t("ui.placeholder.selectTableRow"));
        return;
      }
      const ids = this.selection.map((item) => item.id);
      this.$modal
        .confirm(this.$t("ui.data.column.gsqScheduleResult.confirmDelete"))
        .then(() => {
          return batchDelete(ids);
        })
        .then(() => {
          this.$modal.msgSuccess(this.$t("ui.common.message.deleteSuccess"));
          this.getList();
        })
        .catch(() => {});
    },
    /** 批量转机台（走任务链路径，支持锚点、目标班次） */
    handleBatchChangeMachine() {
      if (this.selection.length < 2) return;
      this.$refs.changeMachineDialog.show(this.selection[0], this.selection);
    },
    /** 批量调量（走任务链路径） */
    handleBatchChangeQty() {
      if (this.selection.length < 2) return;
      this.$refs.changeQtyDialog.show(this.selection[0], this.selection);
    },
    /** 转机台（单选模式） */
    handleChangeMachine(row) {
      if (!row) return;
      this.$refs.changeMachineDialog.show(row);
    },
    /** 调量 */
    handleChangeQty(row) {
      if (!row) return;
      this.currentRow = row;
      this.changeQtyVisible = true;
    },
    /** 发布排程：将选中记录下发MES，后端按发布状态过滤可发布记录 */
    handlePublish() {
      if (this.selection.length === 0) return;
      this.$confirm(this.$t("ui.biz.alter.makeSurePublish"), {
        type: "warning",
      }).then(async () => {
        try {
          this.loading = true;
          // 收集选中记录ID列表，与排程日期、分厂一并传给后端
          const ids = this.selection.map((item) => item.id);
          const data = await publishSchedule({
            scheduleDateQuery: this.selection[0].scheduleDate,
            factoryCode: this.selection[0].factoryCode,
            ids: ids.join(","),
          });
          this.$modal.msgSuccess(data.msg);
          this.getList();
        } catch (error) {
          console.error(error);
        } finally {
          this.loading = false;
        }
      });
    },
    /** 导出（按专用模板导出钢丝圈排程结果） */
    handleExport() {
      if (!this.search.factoryCode || !this.search.scheduleDate) {
        this.$modal.msgWarning(this.$t("ui.data.alert.gsq.schedule.excel.factoryDateRequired"));
        return;
      }
      downloadLink("/gsq/scheduleResult/export", {
        factoryCode: this.search.factoryCode,
        scheduleDate: this.search.scheduleDate,
        steelRingCode: this.search.steelRingCode,
        machineCode: this.search.machineCode,
      });
    },
    /** 按专用模板下载导入模板（拼接查询参数） */
    handleTemplateDownload(url, formValues) {
      const params = {
        ...formValues,
      };
      const paramsStr = Object.keys(params)
        .filter(key => params[key] !== undefined && params[key] !== null && params[key] !== "")
        .map(key => `${encodeURIComponent(key)}=${encodeURIComponent(params[key])}`)
        .join("&");
      return `${url}${paramsStr ? "?" + paramsStr : ""}`;
    },
    /** 行样式：发布中标记 */
    rowStyle({ row }) {
      if (row.isRelease === "1") {
        return { backgroundColor: "#fff3cd" };
      }
      if (row.isRelease === "2") {
        return { backgroundColor: "#d4edda" };
      }
      return {};
    },
    /** 单元格样式：完成率/历史班次标灰 */
    cellStyle({ row, column, cellValue }) {
      return {};
    },
    /** 合计行：6班次计划量汇总 */
    getSummaries(param) {
      const { columns, data } = param;
      const sums = [];
      columns.forEach((column, index) => {
        if (index === 0) {
          sums[index] = this.$t("ui.data.column.gsqScheduleResult.total");
          return;
        }
        const prop = column.property;
        if (
          prop &&
          (prop.endsWith("PlanQty") || prop === "monthSurplusQty")
        ) {
          const values = data.map((item) => Number(item[prop]));
          if (!values.every((value) => isNaN(value))) {
            sums[index] = values.reduce((prev, curr) => {
              const value = Number(curr);
              if (!isNaN(value)) {
                return prev + value;
              } else {
                return prev;
              }
            }, 0);
            sums[index] = Number(sums[index].toFixed(0));
          } else {
            sums[index] = "";
          }
        } else {
          sums[index] = "";
        }
      });
      return sums;
    },
  },
};
</script>

<style scoped lang="scss">
.more-btn {
  width: 100%;
}
</style>
