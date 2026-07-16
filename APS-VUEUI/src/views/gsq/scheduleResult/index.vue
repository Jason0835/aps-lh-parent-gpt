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
          class="multiple disabled"
          :disabled="selection.length === 0"
          @click="handleChangeMachine"
        >{{ $t("ui.data.column.scheduleResult.changeMachine") }}</el-button>
        <el-button
          v-hasPermi="['gsq:scheduleResult:changeQty']"
          type="primary"
          class="single disabled"
          @click="handleChangeQty(selection[0])"
        >{{ $t("ui.data.column.scheduleResult.changePlan") }}</el-button>
        <el-button
          v-hasPermi="['gsq:scheduleResult:publish']"
          type="primary"
          :disabled="selection.length === 0"
          @click="handlePublish"
        >{{ $t("ui.data.column.scheduleResult.publish") }}</el-button>
        <el-dropdown>
          <el-button type="primary" style="margin-left: 10px">
            {{ $t("ui.frame.btn.more") }}<i class="el-icon-arrow-down el-icon--right"></i>
          </el-button>
          <el-dropdown-menu slot="dropdown">
            <el-dropdown-item>
              <el-button type="primary" class="more-btn" @click="handleExportUiExcel">
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
      v-if="changeMachineVisible"
      :visible.sync="changeMachineVisible"
      :rows="selection"
      @refresh="getList"
    />

    <!-- 调量弹窗 -->
    <change-qty-dialog
      v-if="changeQtyVisible"
      :visible.sync="changeQtyVisible"
      :row="currentRow"
      @refresh="getList"
    />

    <!-- 导入弹窗 -->
    <tlt-upload-form
      ref="tltUploadForm"
      :importDialogVisible.sync="importDialogVisible"
      uploadUrl="/gsq/scheduleResult/importData"
      downloadUrl="/gsq/scheduleResult/importTemplate"
      :columns="importColumns"
      :rules="importRules"
      @refresh="getList"
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
  exportScheduleResult,
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
      page: {
        pageNum: 1,
        pageSize: 50,
        total: 0,
      },
      search: {
        scheduleDate: moment().add(2, "days").format("YYYY-MM-DD"),
        factoryCode: "",
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
      changeMachineVisible: false,
      changeQtyVisible: false,
      importDialogVisible: false,
      // 6班次日期展示（D日中班/D+1日夜早中/D+2日夜早）
      shiftDateList: [],
      // 导入默认值
      importDefaultValue: {
        scheduleDate: moment().add(2, "days").format("YYYY-MM-DD"),
      },
      // 导入列配置
      importColumns: [
        {
          label: this.$t("ui.data.column.gsqScheduleResult.scheduleDate"),
          prop: "scheduleDate",
          type: "date",
        },
      ],
      // 导入校验规则
      importRules: {
        scheduleDate: [
          {
            required: true,
            message: this.$t("ui.data.column.scheduleResult.chooseScheduleDate"),
            trigger: "blur",
          },
        ],
      },
    };
  },
  computed: {
    ...mapGetters(["permissions"]),
    searchColumns() {
      return [
        {
          label: this.$t("ui.data.column.gsqScheduleResult.factoryCode"),
          prop: "factoryCode",
          component: "el-select",
          filterable: true,
          options: this.dict.type.biz_factory_name,
          valueKey: "value",
          labelKey: "label",
        },
        {
          label: this.$t("ui.data.column.gsqScheduleResult.scheduleDate"),
          prop: "scheduleDate",
          component: "el-date-picker",
          type: "date",
          valueFormat: "yyyy-MM-dd",
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
        // ===== 6班次动态列（含日期头） =====
        this.buildShiftColumnGroup(1),
        this.buildShiftColumnGroup(2),
        this.buildShiftColumnGroup(3),
        this.buildShiftColumnGroup(4),
        this.buildShiftColumnGroup(5),
        this.buildShiftColumnGroup(6),
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
        },
        {
          label: this.$t("ui.frame.btn.operation"),
          prop: "operation",
          width: 160,
          fixed: "right",
          slot: true,
        },
      ];
    },
  },
  created() {
    this.loadShiftDates();
    this.getList();
  },
  methods: {
    /**
     * 构建6班次列分组（计划量/完成量/顺序/原因分析）
     * @param shift 班次序号 1~6
     */
    buildShiftColumnGroup(shift) {
      const shiftDateVo = this.shiftDateList.find((s) => s.shift === shift);
      const dateLabel = shiftDateVo ? shiftDateVo.shiftDate : "";
      const shiftTypeLabel = shiftDateVo ? this.shiftTypeLabel(shiftDateVo.shiftType) : "";
      const prefix = `class${shift}`;
      return {
        label: `${dateLabel} ${shiftTypeLabel}（${shift}班）`,
        children: [
          {
            label: this.$t("ui.data.column.gsqScheduleResult.planQty"),
            prop: `${prefix}PlanQty`,
            width: 90,
          },
          {
            label: this.$t("ui.data.column.gsqScheduleResult.finishQty"),
            prop: `${prefix}FinishQty`,
            width: 90,
          },
          {
            label: this.$t("ui.data.column.gsqScheduleResult.sequence"),
            prop: `${prefix}Sequence`,
            width: 80,
          },
          {
            label: this.$t("ui.data.column.gsqScheduleResult.handAnalysis"),
            prop: `${prefix}HandAnalysis`,
            width: 130,
          },
        ],
      };
    },
    /** 班次类型中文标签 */
    shiftTypeLabel(type) {
      if (type === "night") return this.$t("ui.data.column.gsqScheduleResult.nightShift");
      if (type === "morning") return this.$t("ui.data.column.gsqScheduleResult.morningShift");
      if (type === "afternoon") return this.$t("ui.data.column.gsqScheduleResult.afternoonShift");
      return "";
    },
    /** 加载6班次日期 */
    loadShiftDates() {
      listScheduleShiftDates({ scheduleDate: this.search.scheduleDate }).then((res) => {
        this.shiftDateList = res.data || [];
      });
    },
    /** 查询列表 */
    getList() {
      this.loading = true;
      const params = {
        ...this.search,
        scheduleDateQuery: this.search.scheduleDate,
        pageNum: this.page.pageNum,
        pageSize: this.page.pageSize,
      };
      listScheduleResult(params)
        .then((res) => {
          this.data = res.rows || [];
          this.page.total = res.total || 0;
        })
        .finally(() => {
          this.loading = false;
        });
    },
    handleSearch() {
      this.page.pageNum = 1;
      this.loadShiftDates();
      this.getList();
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
    /** 删除（逻辑删除） */
    handleDelete() {
      if (this.selection.length === 0) return;
      const ids = this.selection.map((item) => item.id).join(",");
      this.$modal
        .confirm(this.$t("ui.data.column.gsqScheduleResult.confirmDelete"))
        .then(() => {
          return logicDeleteScheduleResult(ids);
        })
        .then(() => {
          this.$modal.msgSuccess(this.$t("ui.common.message.deleteSuccess"));
          this.getList();
        })
        .catch(() => {});
    },
    /** 转机台 */
    handleChangeMachine() {
      if (this.selection.length === 0) return;
      this.changeMachineVisible = true;
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
      this.$confirm(this.$t("ui.data.column.gsqScheduleResult.confirmPublish"), {
        type: "warning",
      }).then(async () => {
        try {
          this.loading = true;
          // 收集选中记录ID列表，与排程日期、分厂一并传给后端
          const ids = this.selection.map((item) => item.id);
          const params = {
            scheduleDate: this.selection[0].scheduleDate,
            factoryCode: this.selection[0].factoryCode,
            ids: ids.join(","),
          };
          const data = await publishSchedule(params);
          this.$modal.msgSuccess(data.msg);
          this.getList();
        } catch (error) {
          console.error(error);
        } finally {
          this.loading = false;
        }
      });
    },
    /** 导出 */
    handleExportUiExcel() {
      this.download(
        "/gsq/scheduleResult/exportData/钢丝圈排程结果",
        { ...this.search },
        `钢丝圈排程结果_${moment().format("YYYYMMDDHHmmss")}.xlsx`
      );
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
