<template>
  <basic-container>
    <page-table
      tableRef="tmScheduleResultMainTable"
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
          type="warning"
          v-hasPermi="['tm:tmScheduleResult:autoPlan']"
          :disabled="writeTaskRunning"
          @click="handleAutoPlan"
        >{{ $t("ui.data.column.scheduleResult.autoPlan") }}</el-button>
        <el-button
          type="warning"
          v-hasPermi="['tm:tmScheduleResult:add']"
          :disabled="writeTaskRunning"
          @click="handleAdd"
        >{{ $t("ui.data.column.scheduleResult.insertOrder") }}</el-button>
        <el-button
          v-hasPermi="['tm:tmScheduleResult:edit']"
          :disabled="writeTaskRunning || selection.length !== 1"
          type="warning"
          @click="handleChangeQty"
        >{{ $t("ui.tm.schedule.button.modify") }}</el-button>
        <el-button
          v-hasPermi="['tm:tmScheduleResult:changeMachine']"
          :disabled="writeTaskRunning || selection.length === 0"
          type="primary"
          @click="handleChangeMachine"
        >{{ $t("ui.tm.schedule.button.changeMachine") }}</el-button>
        <el-button
          type="danger"
          v-hasPermi="['tm:tmScheduleResult:remove']"
          :disabled="writeTaskRunning || selection.length == 0"
          @click="handleDeleteAll"
        >{{ $t("ui.frame.btn.delete") }}</el-button>
        <el-button
          v-hasPermi="['tm:tmScheduleResult:export']"
          type="primary"
          @click="handleExport"
        >{{ $t("ui.frame.btn.export") }}</el-button>
        <el-button
          type="primary"
          v-hasPermi="['tm:tmScheduleResult:import']"
          :disabled="writeTaskRunning"
          @click="handleImport"
        >{{ $t("ui.frame.btn.import") }}</el-button>
        <el-button
          type="primary"
          :disabled="writeTaskRunning || selection.length === 0"
          v-hasPermi="['tm:tmScheduleResult:publish']"
          @click="handlePublish"
        >{{ $t("ui.tm.schedule.button.publish") }}</el-button>
        <el-button
          v-hasRole="['admin']"
          :disabled="writeTaskRunning || selection.length === 0"
          type="primary"
          @click="handleChangeReleaseStatus"
        >{{ $t("ui.tm.schedule.button.changeReleaseStatus") }}</el-button>
        <el-button
          v-hasPermi="['tm:tmScheduleResult:query']"
          plain
          type="info"
          @click="handleUnplanned"
        >{{ $t('ui.tm.schedule.unplannedTasks') }}（{{ unplannedCount || 0 }}）</el-button>
      </template>
      <template slot="headerRight">
        <div class="summary-bar stat-info">
          <span
            v-for="(planQty, index) in shiftPlanQtyList"
            :key="index"
          >{{ getShiftLabel(index + 1) }}{{ $t('ui.tm.schedule.planQty') }}：<span class="stat-value">{{ planQty || 0 }}</span></span>
        </div>
      </template>
    </page-table>
    <div v-if="autoPlanRunning || autoPlanRecoveryVisible" class="auto-plan-task-banner">
      <span>{{ autoPlanRecoveryVisible ? $t('ui.schedule.autoPlan.recoveryHint') : $t('ui.schedule.autoPlan.backgroundHint') }}</span>
      <el-button type="text" @click="resumeAutoPlanTask">{{ $t('ui.schedule.autoPlan.viewTask') }}</el-button>
    </div>
    <tlt-upload-form
      ref="tltUpload"
      :updateSupport="true"
      :download-url-formatter="(form) => handleTemplateDownload('/tm/tmScheduleResult/importTemplateCust', form)"
      :rules="importRules"
      downloadUrl="/tm/tmScheduleResult/importTemplateCust"
      @uploadSuccess="getList"
      labelWidth="0"
      :columns="importColumns"
      uploadUrl="/tm/tmScheduleResult/importDataCust"
    ></tlt-upload-form>
    <autoPlanDialog ref="autoPlanRef" @success="handleAutoPlanSuccess" />
    <infoDialog ref="infoRef" :machine-options="machines" @success="handleOperationTask" />
    <changeMachineDialog ref="changeMachineRef" @success="handleOperationTask" />
    <releaseStatusDialog ref="releaseStatusRef" @success="getList" />
    <unplanned-dialog ref="unplannedRef" />
    <el-dialog
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      :show-close="false"
      :title="$t('ui.data.column.tm.scheduleResult.autoPlanProgress')"
      :visible.sync="autoPlanProgressVisible"
      append-to-body
      width="420px"
    >
      <div style="text-align:center;margin-bottom:12px;color:#606266;font-size:14px;">
        {{ autoPlanProgressStage }}
      </div>
      <el-progress
        class="auto-plan-progress"
        :percentage="autoPlanProgressValue"
        :status="autoPlanProgressStatus"
        :stroke-width="18"
        :text-inside="true"
        text-color="#fff"
      />
      <div style="margin-top:10px;color:#909399;font-size:12px;text-align:center;">
        {{ autoPlanProgressHint }}
      </div>
      <template slot="footer">
        <el-button v-if="autoPlanRunning" @click="hideAutoPlanProgressInBackground">{{ $t('ui.schedule.autoPlan.backgroundContinue') }}</el-button>
      </template>
    </el-dialog>
    <el-dialog :title="$t('ui.schedule.autoPlan.resultSummary')" :visible.sync="autoPlanResultVisible" append-to-body width="460px">
      <div class="auto-plan-result-message">{{ autoPlanResult.message || $t('ui.schedule.autoPlan.completed') }}</div>
      <div class="auto-plan-result-summary">
        <span>{{ $t('ui.schedule.autoPlan.scheduledCount') }}：{{ autoPlanResult.resultCount || 0 }}</span>
        <span>{{ $t('ui.schedule.autoPlan.unplannedCount') }}：{{ autoPlanResult.unplannedCount || 0 }}</span>
        <span>{{ $t('ui.schedule.autoPlan.issueCount') }}：{{ autoPlanIssues.length }}</span>
        <span>{{ $t('ui.schedule.autoPlan.batchNo') }}：{{ autoPlanResult.batchNo || '-' }}</span>
      </div>
      <template slot="footer">
        <el-button :disabled="autoPlanIssues.length === 0" @click="openAutoPlanIssues">{{ $t('ui.schedule.autoPlan.viewIssues') }}</el-button>
        <el-button :disabled="Number(autoPlanResult.unplannedCount || 0) === 0" @click="openAutoPlanUnplanned">{{ $t('ui.schedule.autoPlan.viewUnplanned') }}</el-button>
        <el-button type="primary" @click="refreshAutoPlanBoard">{{ $t('ui.schedule.autoPlan.refreshBoard') }}</el-button>
      </template>
    </el-dialog>
    <el-dialog
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      :show-close="false"
      :title="$t('ui.data.column.tm.scheduleResult.operationProgress')"
      :visible.sync="operationProgressVisible"
      append-to-body
      width="420px"
    >
      <div style="text-align:center;margin-bottom:12px;color:#606266;font-size:14px;">
        {{ operationProgressStage }}
      </div>
      <el-progress
        :percentage="operationProgressValue"
        :status="operationProgressStatus"
        :stroke-width="18"
        :text-inside="true"
        text-color="#fff"
      />
      <div style="margin-top:10px;color:#909399;font-size:12px;text-align:center;">
        {{ $t("ui.data.column.tm.scheduleResult.operationProgressHint") }}
      </div>
    </el-dialog>
    <el-dialog
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      :show-close="false"
      :title="$t('ui.tc.schedule.releaseProgress')"
      :visible.sync="releaseProgressVisible"
      append-to-body
      width="420px"
    >
      <div style="text-align:center;margin-bottom:12px;color:#606266;font-size:14px;">
        {{ releaseProgressStage }}
      </div>
      <el-progress
        :percentage="releaseProgressValue"
        :status="releaseProgressStatus"
        :stroke-width="18"
        :text-inside="true"
        text-color="#fff"
      />
      <div style="margin-top:10px;color:#909399;font-size:12px;text-align:center;">
        {{ $t("ui.tc.schedule.releaseProgressHint") }}
      </div>
    </el-dialog>
    <el-dialog
      :title="$t('ui.tc.schedule.releaseIssues')"
      :visible.sync="releaseIssueVisible"
      append-to-body
      width="82%"
    >
      <el-table :data="releaseIssues" border max-height="520">
        <el-table-column :label="$t('ui.tc.schedule.issueLevel')" prop="level" width="90" />
        <el-table-column :label="$t('ui.tc.schedule.issueStage')" prop="stageName" width="150" />
        <el-table-column :label="$t('ui.tc.schedule.issueCategory')" min-width="150" prop="category" />
        <el-table-column :label="$t('ui.tc.schedule.issueMessage')" min-width="260" prop="message" show-overflow-tooltip />
      </el-table>
    </el-dialog>
    <el-dialog
      :title="$t('ui.data.column.tm.scheduleResult.autoPlanIssues')"
      :visible.sync="autoPlanIssueVisible"
      append-to-body
      width="80%"
    >
      <el-table :data="autoPlanIssues" border style="width:100%">
        <el-table-column :label="$t('ui.data.column.tm.scheduleResult.issueLevel')" prop="level" width="90" />
        <el-table-column :label="$t('ui.data.column.tm.scheduleResult.issueStage')" prop="stageName" width="120" />
        <el-table-column :label="$t('ui.data.column.tm.scheduleResult.issueCategory')" prop="category" width="170" />
        <el-table-column :label="$t('ui.data.column.tm.scheduleResult.issueSourceOrderNo')" prop="sourceOrderNo" width="160" />
        <el-table-column :label="$t('ui.data.column.tm.scheduleResult.issueEmbryoCode')" prop="embryoCode" width="150" />
        <el-table-column :label="$t('ui.data.column.tm.scheduleResult.issueRecipeNo')" prop="recipeNo" width="150" />
        <el-table-column :label="$t('ui.data.column.tm.scheduleResult.issueShiftOrder')" prop="shiftOrder" width="90" />
        <el-table-column :label="$t('ui.data.column.tm.scheduleResult.issueFieldName')" prop="fieldName" width="150" />
        <el-table-column :label="$t('ui.data.column.tm.scheduleResult.issueMessage')" min-width="220" prop="message" show-overflow-tooltip />
      </el-table>
    </el-dialog>
  </basic-container>
</template>
<script>
import {mapState} from "vuex";
import {downloadLink} from "@/utils/request";
import {
  getAutoPlanTask,
  getLatestAutoPlanTask,
  getLatestOperationTask,
  getOperationTask,
  getReleaseTask,
  listScheduleShiftDates,
  listTmScheduleResult,
  listTmScheduleSummary,
  listTmScheduleUnplanned,
  releaseScheduleResult,
  removeTmScheduleResult,
  validateRelease,
} from "@/api/tm/scheduleResult";
import tltUpload from "@/components/tltUpload/tltUpload.vue";
import TltUploadForm from "@/views/components/tltUploadForm.vue";
import autoPlanDialog from "./components/autoPlanDialog.vue";
import infoDialog from "./components/infoDialog.vue";
import changeMachineDialog from "./components/changeMachineDialog.vue";
import releaseStatusDialog from "./components/releaseStatusDialog.vue";
import UnplannedDialog from "./components/UnplannedDialog.vue";

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
  name: "/tm/tmScheduleResult",
  components: {
    tltUpload,
    autoPlanDialog,
    infoDialog,
    TltUploadForm,
    changeMachineDialog,
    releaseStatusDialog,
    UnplannedDialog,
  },
  dicts: ["biz_factory_name", "biz_yes_no", "IS_RELEASE", "tm_data_source"],
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
      summary: {},
      unplannedCount: 0,
      page: {
        current: 1,
        pageSize: 20,
        total: 0,
      },
      sort: {},
      search: {},
      query: {},
      importDefaultValue: {},
      importRules: {
        factoryCode: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
        scheduleDate: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
      },
      autoPlanTimer: null,
      autoPlanPollTimes: 0,
      maxAutoPlanPollTimes: 120,
      autoPlanProgressVisible: false,
      autoPlanProgressValue: 0,
      autoPlanProgressStage: "",
      autoPlanProgressStatus: null,
      autoPlanProgressHint: "",
      autoPlanRunning: false,
      autoPlanTaskId: "",
      autoPlanRecoveryVisible: false,
      autoPlanResultVisible: false,
      autoPlanResult: {},
      autoPlanResultScope: {},
      autoPlanIssueVisible: false,
      autoPlanIssues: [],
      operationTimer: null,
      operationPollTimes: 0,
      maxOperationPollTimes: 120,
      operationRunning: false,
      operationProgressVisible: false,
      operationProgressValue: 0,
      operationProgressStage: "",
      operationProgressStatus: null,
      releaseTimer: null,
      releasePollTimes: 0,
      maxReleasePollTimes: 240,
      releaseProgressVisible: false,
      releaseProgressValue: 0,
      releaseProgressStage: "",
      releaseProgressStatus: null,
      releaseIssueVisible: false,
      releaseIssues: [],
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
      machines: (state) => state.tm.machines,
    }),
    writeTaskRunning() {
      return this.operationRunning || this.autoPlanRunning || this.releaseProgressVisible;
    },
    // 各班次计划量合计列表，后端返回下标 0=1班，长度 6；为空时回退为空数组避免渲染异常
    shiftPlanQtyList() {
      return (this.summary && this.summary.shiftPlanQtyList) || [];
    },
    // 导入弹窗列配置放在 computed 中，确保 this.dict 已初始化（data() 执行时字典 mixin 尚未注入 dict）
    importColumns() {
      return [
        {
          label: this.$t("ui.data.column.tm.scheduleResult.factoryCode"),
          prop: "factoryCode",
          type: "select",
          dictData: this.dict.type.biz_factory_name,
          filterable: true,
          clearable: false,
        },
        {
          label: this.$t("ui.data.column.tm.scheduleResult.scheduleDate"),
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
    columns() {
      return [
        { type: "selection", fixed: "left" },
        {
          prop: "factoryCode",
          label: this.$t("ui.data.column.tm.scheduleResult.factoryCode"),
          type: "select",
          filterable: true,
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_factory_name, value);
          },
        },
        {
          prop: "batchNo",
          align: "left",
          minWidth: 160,
          label: this.$t("ui.data.column.tm.scheduleResult.batchNo"),
        },
        {
          prop: "orderNo",
          align: "left",
          minWidth: 160,
          label: this.$t("ui.data.column.tm.scheduleResult.orderNo"),
        },
        {
          prop: "scheduleDate",
          align: "center",
          minWidth: 120,
          label: this.$t("ui.data.column.tm.scheduleResult.scheduleDate"),
        },
        {
          prop: "machineCode",
          halign: "center",
          label: this.$t("ui.data.column.tm.scheduleResult.machineCode"),
        },
        {
          prop: "treadCode",
          halign: "center",
          label: this.$t("ui.data.column.tm.scheduleResult.treadCode"),
        },
        {
          prop: "glueCode",
          halign: "center",
          label: this.$t("ui.data.column.tm.scheduleResult.glueCode"),
        },
        {
          prop: "releaseStatus",
          halign: "center",
          label: this.$t("ui.data.column.tm.scheduleResult.releaseStatus"),
          type: "select",
          filterable: true,
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.IS_RELEASE, value);
          },
        },
        {
          prop: "tailFlag",
          halign: "center",
          label: this.$t("ui.data.column.tm.scheduleResult.tailFlag"),
          type: "select",
          filterable: true,
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_yes_no, value);
          },
        },
        {
          label: this.getShiftLabel(1),
          children: [
            {
              prop: "class1Sequence",
              align: "center",
              label: this.$t("ui.data.column.tm.scheduleResult.class1Sequence"),
              minWidth: 70,
            },
            {
              prop: "class1PlanQty",
              align: "center",
              label: this.$t("ui.data.column.tm.scheduleResult.class1PlanQty"),
              minWidth: 70,
            },
            {
              prop: "class1FinishQty",
              align: "center",
              label: this.$t("ui.data.column.tm.scheduleResult.class1FinishQty"),
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
              label: this.$t("ui.data.column.tm.scheduleResult.class2Sequence"),
              minWidth: 70,
            },
            {
              prop: "class2PlanQty",
              align: "center",
              label: this.$t("ui.data.column.tm.scheduleResult.class2PlanQty"),
              minWidth: 70,
            },
            {
              prop: "class2FinishQty",
              align: "center",
              label: this.$t("ui.data.column.tm.scheduleResult.class2FinishQty"),
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
              label: this.$t("ui.data.column.tm.scheduleResult.class3Sequence"),
              minWidth: 70,
            },
            {
              prop: "class3PlanQty",
              align: "center",
              label: this.$t("ui.data.column.tm.scheduleResult.class3PlanQty"),
              minWidth: 70,
            },
            {
              prop: "class3FinishQty",
              align: "center",
              label: this.$t("ui.data.column.tm.scheduleResult.class3FinishQty"),
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
              label: this.$t("ui.data.column.tm.scheduleResult.class4Sequence"),
              minWidth: 70,
            },
            {
              prop: "class4PlanQty",
              align: "center",
              label: this.$t("ui.data.column.tm.scheduleResult.class4PlanQty"),
              minWidth: 70,
            },
            {
              prop: "class4FinishQty",
              align: "center",
              label: this.$t("ui.data.column.tm.scheduleResult.class4FinishQty"),
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
              label: this.$t("ui.data.column.tm.scheduleResult.class5Sequence"),
              minWidth: 70,
            },
            {
              prop: "class5PlanQty",
              align: "center",
              label: this.$t("ui.data.column.tm.scheduleResult.class5PlanQty"),
              minWidth: 70,
            },
            {
              prop: "class5FinishQty",
              align: "center",
              label: this.$t("ui.data.column.tm.scheduleResult.class5FinishQty"),
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
              label: this.$t("ui.data.column.tm.scheduleResult.class6Sequence"),
              minWidth: 70,
            },
            {
              prop: "class6PlanQty",
              align: "center",
              label: this.$t("ui.data.column.tm.scheduleResult.class6PlanQty"),
              minWidth: 70,
            },
            {
              prop: "class6FinishQty",
              align: "center",
              label: this.$t("ui.data.column.tm.scheduleResult.class6FinishQty"),
              minWidth: 70,
            },
          ],
        },
        {
          prop: "updateTime",
          label: this.$t("ui.data.column.updateTime"),
          width: 180,
        },
        {
          align: "center",
          halign: "center",
          label: this.$t("ui.data.btn.option"),
          minWidth: 180,
          width: 200,
          fixed: "right",
          render: ({ row }) => {
            return (
              <div>
                <el-button
                  v-hasPermi={["tm:tmScheduleResult:edit"]}
                  class="minus"
                  type="success"
                  onClick={() => this.handleEdit(row)}
                >
                  {this.$t("ui.data.column.scheduleResult.changePlan")}
                </el-button>
                <el-button
                  v-hasPermi={["tm:tmScheduleResult:remove"]}
                  class="minus"
                  type="danger"
                  onClick={() => this.handleDelete(row)}
                >
                  {this.$t("ui.frame.btn.delete")}
                </el-button>
              </div>
            );
          },
        },
      ];
    },
    searchColumns() {
      return [
        {
          prop: "factoryCode",
          label: this.$t("ui.data.column.tm.scheduleResult.factoryCode"),
          type: "select",
          dictData: this.dict.type.biz_factory_name,
          filterable: true,
        },
        {
          prop: "batchNo",
          label: this.$t("ui.data.column.tm.scheduleResult.batchNo"),
        },
        {
          prop: "orderNo",
          label: this.$t("ui.data.column.tm.scheduleResult.orderNo"),
        },
        {
          label: this.$t("ui.data.column.tm.scheduleResult.scheduleDate"),
          prop: "scheduleDate",
          type: "date",
          valueFormat: "yyyy-MM-dd",
          listeners: {
            change: this.handleScheduleDateChange,
          },
        },
        {
          prop: "treadCode",
          label: this.$t("ui.data.column.tm.scheduleResult.treadCode"),
        },
        {
          prop: "machineCode",
          label: this.$t("ui.data.column.tm.scheduleResult.machineCode"),
          type: "select",
          dictData: this.machines,
          labelKey: "machineCode",
          valueKey: "machineCode",
          filterable: true,
        },
        {
          prop: "releaseStatus",
          label: this.$t("ui.data.column.tm.scheduleResult.releaseStatus"),
          type: "select",
          dictData: this.dict.type.IS_RELEASE,
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
      this.query.scheduleDate = val;
      this.getDate();
    },
    async getDate() {
      try {
        let res = await listScheduleShiftDates({
          factoryCode: this.query.factoryCode || this.search.factoryCode,
          scheduleDate: this.query.scheduleDate || this.search.scheduleDate,
        });
        if (res && res.length > 0) {
          this.dateList = res;
        }
      } catch (error) {
        console.error(error);
      }
    },
    handleAdd() {
      if (this.writeTaskRunning) return;
      if (this.$refs.infoRef) {
        this.$refs.infoRef.show({
          factoryCode: this.query.factoryCode || this.search.factoryCode || "116",
          scheduleDate: this.query.scheduleDate || this.search.scheduleDate,
        });
      }
    },
    // 自动排程入口：打开弹窗选择工厂和排程日期，具体接口由弹窗调用胎面接口。
    handleAutoPlan() {
      if (this.$refs.autoPlanRef) {
        this.$refs.autoPlanRef.show(
          this.query.factoryCode || this.search.factoryCode,
          this.query.scheduleDate || this.search.scheduleDate
        );
      }
    },
    // 自动排程提交成功后启动后台任务轮询。
    handleAutoPlanSuccess(scheduleDate, payload) {
      if (scheduleDate) {
        this.$set(this.query, "scheduleDate", scheduleDate);
        this.search = {
          ...this.search,
          scheduleDate: scheduleDate
        };
      }
      const result = payload || {};
      if (result.taskId) {
        this.saveLatestAutoPlanScope(result.taskId, scheduleDate);
        this.pollAutoPlanTask(result.taskId);
        return;
      }
      this.getList();
    },
    pollAutoPlanTask(taskId) {
      this.clearAutoPlanTimer();
      this.autoPlanPollTimes = 0;
      this.autoPlanRunning = true;
      this.autoPlanTaskId = taskId;
      this.autoPlanRecoveryVisible = false;
      this.autoPlanProgressVisible = true;
      this.autoPlanProgressValue = 0;
      this.autoPlanProgressStage = "";
      this.autoPlanProgressStatus = null;
      this.autoPlanProgressHint = this.$t("ui.data.column.tm.scheduleResult.autoPlanProgressHint");
      const poll = () => {
        getAutoPlanTask(taskId).then(res => {
          this.autoPlanPollTimes += 1;
          const task = res && res.data ? res.data : (res || {});
          if (task.progress != null) {
            this.autoPlanProgressValue = Math.min(100, Math.max(0, task.progress));
          }
          if (task.currentStageName) {
            this.autoPlanProgressStage = task.currentStageName;
          }
          if (task.taskStatus === "SUCCESS") {
            this.clearAutoPlanTimer();
            this.autoPlanRunning = false;
            this.autoPlanProgressValue = 100;
            const noScheduleResult = Number(task.resultCount || 0) === 0 && task.message;
            this.autoPlanProgressStatus = noScheduleResult ? "warning" : "success";
            this.autoPlanProgressStage = noScheduleResult
              ? task.message
              : this.$t("ui.data.column.tm.scheduleResult.autoPlanSuccess");
            if (noScheduleResult) {
              this.$modal.msgWarning(task.message);
            }
            this.closeAutoPlanProgress();
            this.setAutoPlanIssues(task.issues);
            this.showAutoPlanResult(task);
            this.getList();
            return;
          }
          if (task.taskStatus === "FAILED") {
            this.clearAutoPlanTimer();
            this.autoPlanRunning = false;
            this.autoPlanProgressStatus = "exception";
            this.autoPlanProgressStage = this.$t("ui.data.column.tm.scheduleResult.autoPlanFailed");
            this.showAutoPlanIssues(task.issues);
            this.$modal.msgError(task.message || this.$t("ui.data.column.tm.scheduleResult.autoPlanFailed"));
            window.setTimeout(() => { this.closeAutoPlanProgress(); }, 3000);
            return;
          }
          if (this.autoPlanPollTimes >= this.maxAutoPlanPollTimes) {
            this.clearAutoPlanTimer();
            this.autoPlanRunning = false;
            this.autoPlanRecoveryVisible = true;
            this.autoPlanProgressStatus = "exception";
            this.autoPlanProgressStage = this.$t("ui.data.column.tm.scheduleResult.autoPlanTimeout");
            this.$modal.msgWarning(this.$t("ui.data.column.tm.scheduleResult.autoPlanTimeout"));
            window.setTimeout(() => { this.closeAutoPlanProgress(); }, 3000);
            return;
          }
          this.autoPlanTimer = window.setTimeout(poll, 3000);
        }).catch(() => {
          this.clearAutoPlanTimer();
          this.autoPlanRunning = false;
          this.autoPlanRecoveryVisible = true;
          this.autoPlanProgressStatus = "exception";
          this.autoPlanProgressStage = this.$t("ui.data.column.tm.scheduleResult.autoPlanTimeout");
          this.$modal.msgWarning(this.$t("ui.data.column.tm.scheduleResult.autoPlanTimeout"));
          window.setTimeout(() => { this.closeAutoPlanProgress(); }, 3000);
        });
      };
      poll();
    },
    clearAutoPlanTimer() {
      if (this.autoPlanTimer) {
        window.clearTimeout(this.autoPlanTimer);
        this.autoPlanTimer = null;
      }
    },
    closeAutoPlanProgress() {
      this.autoPlanProgressVisible = false;
      this.autoPlanProgressValue = 0;
      this.autoPlanProgressStage = "";
      this.autoPlanProgressStatus = null;
      this.autoPlanProgressHint = "";
    },
    hideAutoPlanProgressInBackground() {
      this.autoPlanProgressVisible = false;
    },
    saveLatestAutoPlanScope(taskId, scheduleDate) {
      window.sessionStorage.setItem("tmAutoPlanLatestScope", JSON.stringify({
        taskId,
        factoryCode: this.query.factoryCode || this.search.factoryCode,
        scheduleDate: scheduleDate || this.query.scheduleDate || this.search.scheduleDate,
      }));
    },
    resumeAutoPlanTask() {
      if (this.autoPlanTaskId) {
        this.pollAutoPlanTask(this.autoPlanTaskId);
      }
    },
    setAutoPlanIssues(issues) {
      this.autoPlanIssues = Array.isArray(issues) ? issues : [];
      this.autoPlanIssueVisible = false;
    },
    showAutoPlanResult(task) {
      this.autoPlanResult = task || {};
      this.autoPlanResultScope = {
        factoryCode: this.query.factoryCode || this.search.factoryCode,
        scheduleDate: this.query.scheduleDate || this.search.scheduleDate,
        batchNo: task && task.batchNo,
      };
      this.autoPlanResultVisible = true;
    },
    openAutoPlanIssues() {
      this.autoPlanIssueVisible = this.autoPlanIssues.length > 0;
    },
    openAutoPlanUnplanned() {
      this.handleUnplanned(this.autoPlanResultScope);
    },
    handleUnplanned(scope) {
      const query = scope || {
        factoryCode: this.query.factoryCode || this.search.factoryCode,
        scheduleDate: this.query.scheduleDate || this.search.scheduleDate,
      };
      this.$refs.unplannedRef.show(query);
    },
    refreshAutoPlanBoard() {
      this.autoPlanResultVisible = false;
      this.getList();
    },
    showAutoPlanIssues(issues) {
      this.autoPlanIssues = Array.isArray(issues) ? issues : [];
      this.autoPlanIssueVisible = this.autoPlanIssues.length > 0;
    },
    restoreLatestAutoPlanTask() {
      let factoryCode = this.query.factoryCode || this.search.factoryCode;
      let scheduleDate = this.query.scheduleDate || this.search.scheduleDate;
      try {
        const storedScope = JSON.parse(window.sessionStorage.getItem("tmAutoPlanLatestScope") || "{}");
        factoryCode = storedScope.factoryCode || factoryCode;
        scheduleDate = storedScope.scheduleDate || scheduleDate;
        this.autoPlanTaskId = storedScope.taskId || this.autoPlanTaskId;
      } catch (error) {
        window.sessionStorage.removeItem("tmAutoPlanLatestScope");
      }
      if (!factoryCode || !scheduleDate) {
        return;
      }
      getLatestAutoPlanTask({ factoryCode, scheduleDate }).then(res => {
        const task = res && res.data ? res.data : null;
        if (task && task.taskId && (task.taskStatus === "PENDING" || task.taskStatus === "RUNNING")) {
          this.pollAutoPlanTask(task.taskId);
        }
      }).catch(() => {});
    },
    // 转机台弹窗
    handleChangeMachine() {
      if (this.writeTaskRunning) return;
      if (this.$refs.changeMachineRef) {
        let row = this.selection;
        this.$refs.changeMachineRef.show(row);
      }
    },
    handleOperationTask(task) {
      if (!task || !task.taskId) {
        return;
      }
      this.pollOperationTask(task.taskId, task);
    },
    pollOperationTask(taskId, initialTask) {
      this.clearOperationTimer();
      this.operationPollTimes = 0;
      this.operationRunning = true;
      this.operationProgressVisible = true;
      this.operationProgressValue = Number((initialTask && initialTask.progress) || 0);
      this.operationProgressStage = (initialTask && (initialTask.currentStageName || initialTask.currentStage)) || "";
      this.operationProgressStatus = null;
      const poll = () => {
        getOperationTask(taskId).then(task => {
          this.operationPollTimes += 1;
          this.operationProgressValue = Math.min(100, Math.max(0, Number(task.progress || 0)));
          this.operationProgressStage = task.currentStageName || task.currentStage || "";
          if (task.taskStatus === "SUCCESS") {
            this.clearOperationTimer();
            this.operationRunning = false;
            this.operationProgressValue = 100;
            this.operationProgressStatus = "success";
            this.$modal.msgSuccess(this.$t("ui.data.column.tm.scheduleResult.operationSuccess"));
            this.getList();
            window.setTimeout(() => { this.closeOperationProgress(); }, 600);
            return;
          }
          if (task.taskStatus === "FAILED") {
            this.clearOperationTimer();
            this.operationRunning = false;
            this.operationProgressStatus = "exception";
            this.$modal.msgError(task.message || this.$t("ui.data.column.tm.scheduleResult.operationFailed"));
            window.setTimeout(() => { this.closeOperationProgress(); }, 3000);
            return;
          }
          if (this.operationPollTimes >= this.maxOperationPollTimes) {
            this.clearOperationTimer();
            this.operationRunning = false;
            this.operationProgressStatus = "exception";
            this.$modal.msgWarning(this.$t("ui.data.column.tm.scheduleResult.operationTimeout"));
            window.setTimeout(() => { this.closeOperationProgress(); }, 3000);
            return;
          }
          this.operationTimer = window.setTimeout(poll, 3000);
        }).catch(() => {
          this.clearOperationTimer();
          this.operationRunning = false;
          this.operationProgressStatus = "exception";
          this.$modal.msgWarning(this.$t("ui.data.column.tm.scheduleResult.operationTimeout"));
          window.setTimeout(() => { this.closeOperationProgress(); }, 3000);
        });
      };
      poll();
    },
    clearOperationTimer() {
      if (this.operationTimer) {
        window.clearTimeout(this.operationTimer);
        this.operationTimer = null;
      }
    },
    closeOperationProgress() {
      this.operationProgressVisible = false;
      this.operationProgressValue = 0;
      this.operationProgressStage = "";
      this.operationProgressStatus = null;
    },
    restoreLatestOperationTask() {
      const factoryCode = this.query.factoryCode || this.search.factoryCode;
      const scheduleDate = this.query.scheduleDate || this.search.scheduleDate;
      if (!factoryCode || !scheduleDate) {
        return;
      }
      getLatestOperationTask({ factoryCode, scheduleDate }).then(task => {
        if (task && task.taskId && (task.taskStatus === "PENDING" || task.taskStatus === "RUNNING")) {
          this.pollOperationTask(task.taskId, task);
        }
      }).catch(() => {});
    },
    // 更改发布状态弹窗
    handleChangeReleaseStatus() {
      if (this.$refs.releaseStatusRef) {
        this.$refs.releaseStatusRef.show(this.selection);
      }
    },
    // 调量入口：复用编辑弹窗，由弹窗根据编辑状态调用调量接口。
    handleChangeQty() {
      if (this.writeTaskRunning) return;
      if (this.$refs.infoRef && this.selection.length === 1) {
        this.$refs.infoRef.show(this.selection[0]);
      }
    },
    // 发布入口：校验同scope+发布状态，提交异步发布任务并轮询下发进度（对齐胎侧 handleRelease）。
    async handlePublish() {
      if (this.writeTaskRunning) return;
      const scopeKeySet = new Set(this.selection.map((item) => `${item.factoryCode}|${item.scheduleDate}|${item.batchNo}`));
      if (scopeKeySet.size !== 1) {
        this.$modal.msgWarning(this.$t("ui.tc.schedule.sameScopeRequired"));
        return;
      }
      const invalidRow = this.selection.find((item) => !["0", "2", "4", "5"].includes(String(item.releaseStatus || "0")));
      if (invalidRow) {
        this.$modal.msgWarning(this.$t("ui.tc.schedule.releaseStatusInvalid"));
        return;
      }
      const requestData = this.buildReleaseRequest();
      const validateResult = await validateRelease(requestData);
      if (!validateResult.allowed) {
        const issues = Array.isArray(validateResult.issues) ? validateResult.issues : [];
        this.showReleaseIssues(issues);
        this.$modal.msgWarning(issues.length > 0 ? issues[0].message : this.$t("ui.tc.schedule.releaseValidateFailed"));
        return;
      }
      await this.$confirm(this.$t("ui.biz.alter.makeSurePublish"), { type: "warning" });
      const task = await releaseScheduleResult(requestData);
      this.pollReleaseTask(task.taskId);
    },
    // 构造发布请求（工厂+日期+结果项）
    buildReleaseRequest() {
      const row = this.selection[0];
      return {
        factoryCode: row.factoryCode,
        scheduleDate: row.scheduleDate,
        items: this.selection.map((item) => ({ resultId: item.id, expectedTaskVersion: 0 }))
      };
    },
    // 轮询发布任务进度（对齐胎侧 pollReleaseTask）
    pollReleaseTask(taskId) {
      this.clearReleaseTimer();
      this.releasePollTimes = 0;
      this.releaseProgressVisible = true;
      this.releaseProgressValue = 0;
      this.releaseProgressStatus = null;
      const poll = () => {
        getReleaseTask(taskId).then((task) => {
          this.releasePollTimes += 1;
          this.releaseProgressValue = Math.min(100, Math.max(0, Number(task.progress || 0)));
          this.releaseProgressStage = task.currentStageName || task.currentStage || "";
          if (task.taskStatus === "SUCCESS") {
            this.clearReleaseTimer();
            this.releaseProgressValue = 100;
            this.releaseProgressStatus = "success";
            this.releaseProgressStage = this.$t("ui.tc.schedule.releaseSuccess");
            this.showReleaseIssues(task.issues);
            window.setTimeout(() => { this.releaseProgressVisible = false; }, 600);
            this.queryList();
            return;
          }
          if (task.taskStatus === "FAILED") {
            this.clearReleaseTimer();
            this.releaseProgressStatus = "exception";
            this.releaseProgressStage = this.$t("ui.tc.schedule.releaseFailed");
            this.showReleaseIssues(task.issues);
            this.$modal.msgError(task.message || this.$t("ui.tc.schedule.releaseFailed"));
            window.setTimeout(() => { this.releaseProgressVisible = false; }, 3000);
            this.queryList();
            return;
          }
          if (this.releasePollTimes >= this.maxReleasePollTimes) {
            this.clearReleaseTimer();
            this.releaseProgressStatus = "exception";
            this.$modal.msgWarning(this.$t("ui.tc.schedule.releasePollTimeout"));
            window.setTimeout(() => { this.releaseProgressVisible = false; }, 3000);
            return;
          }
          this.releaseTimer = window.setTimeout(poll, 3000);
        }).catch(() => {
          this.clearReleaseTimer();
          this.releaseProgressStatus = "exception";
          this.$modal.msgWarning(this.$t("ui.tc.schedule.releasePollTimeout"));
        });
      };
      poll();
    },
    showReleaseIssues(issues) {
      this.releaseIssues = Array.isArray(issues) ? issues : [];
      this.releaseIssueVisible = this.releaseIssues.length > 0;
    },
    clearReleaseTimer() {
      if (this.releaseTimer) {
        window.clearTimeout(this.releaseTimer);
        this.releaseTimer = null;
      }
    },
    handleEdit(row) {
      if (this.$refs.infoRef) {
        this.$refs.infoRef.show(row);
      }
    },
    handleDelete(row) {
      if (this.writeTaskRunning) return;
      this.$confirm(this.$t("common.confirm.delete"), {
        type: "warning",
      }).then(() => {
        const ids = row.id;
        removeTmScheduleResult({ ids }).then((task) => {
          this.$set(this.page, "current", 1);
          this.handleOperationTask(task);
        });
      });
    },
    handleDeleteAll() {
      if (this.writeTaskRunning) return;
      let ids = "";
      for (let i = 0; i < this.selection.length; i++) {
        if (i == this.selection.length - 1) {
          ids = ids + this.selection[i].id;
        } else {
          ids = ids + this.selection[i].id + ",";
        }
      }
      this.$confirm(this.$t("common.confirm.delete"), {
        type: "warning",
      }).then(() => {
        removeTmScheduleResult({ ids }).then((task) => {
          this.$set(this.page, "current", 1);
          this.handleOperationTask(task);
        });
      });
    },
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
    handelSuccess() {
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
    handleImport() {
      this.$refs.tltUpload.handleImport({
        factoryCode: this.query.factoryCode || this.search.factoryCode,
        scheduleDate: this.query.scheduleDate || this.search.scheduleDate,
        updateSupport: true,
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
    handleExport() {
      if (!this.query.factoryCode || !this.query.scheduleDate) {
        this.$message.warning(this.$t("ui.tm.schedule.excelFactoryDateRequired"));
        return;
      }
      downloadLink("/tm/tmScheduleResult/export", this.formatParams(false));
    },

    formatParams(hasPage = true) {
      const params = {
        ...this.query,
        ...this.sort,
      };
      if (!params.orderByColumn) {
        params.orderByColumn = "scheduleDate,machineCode,class1Sequence";
        params.isAsc = "asc,asc,asc";
      }
      if (hasPage) {
        params.pageSize = this.page.pageSize;
        params.pageNum = this.page.current;
      }

      if (params.createTime && params.createTime[0]) {
        params.createTimeStart = params.createTime[0];
        params.createTimeEnd = params.createTime[1];
        params.createTime = undefined;
      }

      return params;
    },
    async getList() {
      try {
        this.loading = true;
        await this.getDate();
        const data = await listTmScheduleResult(this.formatParams());
        this.data = data.rows;
        this.page.total = data.total;
        // 合计基于查询条件下全部匹配行，与列表同口径，单独调用合计接口
        const summary = await listTmScheduleSummary(this.formatParams(false));
        this.summary = summary || {};
        if (this.query.factoryCode && this.query.scheduleDate) {
          const unplannedPage = await listTmScheduleUnplanned({
            factoryCode: this.query.factoryCode,
            scheduleDate: this.query.scheduleDate,
            pageNum: 1,
            pageSize: 1,
          });
          this.unplannedCount = Number(unplannedPage.total || 0);
        } else {
          this.unplannedCount = 0;
        }
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
    },
  },
  created() {
    this.$store.dispatch("tm/getMachineList");
    let defaultParams = {
      factoryCode: "116",
    };
    this.search = {
      ...defaultParams,
      scheduleDate: getOffsetDate(2),
    };
    this.query = {
      ...defaultParams,
      scheduleDate: getOffsetDate(2),
    };
    this.getList();
    this.restoreLatestAutoPlanTask();
    this.restoreLatestOperationTask();
  },
  activated() {
    // this.getList();
  },
  beforeDestroy() {
    this.clearAutoPlanTimer();
    this.clearOperationTimer();
  },
};
</script>
<style lang="scss" scoped>
.more-btn {
  margin: 2px 0;
  width: 100%;
}
.auto-plan-task-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  margin: 0 12px 10px;
  color: #606266;
  background: #fdf6ec;
  border: 1px solid #faecd8;
  border-radius: 4px;
}
.auto-plan-result-message {
  margin-bottom: 14px;
  color: #303133;
  line-height: 22px;
}
.auto-plan-result-summary {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  color: #606266;
}
.summary-bar {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 12px;
  max-width: calc(100vw - 160px);
  margin-right: 12px;
  color: #676a6c;
  font-size: 12px;
  font-weight: bold;
  white-space: nowrap;

  .stat-value {
    margin-left: 5px;
    color: #0088cc;
  }
}
</style>
