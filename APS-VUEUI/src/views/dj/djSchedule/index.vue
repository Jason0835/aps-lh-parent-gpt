
<template>
  <basic-container>
    <page-table
      tableRef="djScheduleMainTable"
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
      :row-style="rowStyles"
      :cell-style="cellStyle"
    >
      <template slot="header">
        <el-button
          type="warning"
          v-hasPermi="['dj:djScheduleResult:autoPlan']"
          @click="handleAutoPlan"
          >{{ $t("ui.data.column.scheduleResult.autoPlan") }}</el-button
        >
        <el-button
          type="warning"
          v-hasPermi="['dj:djScheduleResult:add']"
          @click="handleAdd"
          >{{ $t("ui.data.column.scheduleResult.insertOrder") }}</el-button
        >
        <el-button
          type="warning"
          @click="handleEdit()"
          :disabled="selection.length !== 1"
          v-hasPermi="['dj:djScheduleResult:edit']"
          >{{ $t("ui.frame.btn.modify") }}</el-button
        >
        <el-button
          type="danger"
          :disabled="selection.length === 0"
          v-hasPermi="['dj:djScheduleResult:remove']"
          @click="handleDelete"
          >{{ $t("ui.frame.btn.delete") }}</el-button
        >
        <el-button
          v-hasPermi="['dj:djScheduleResult:changeMachine']"
          type="primary"
          :disabled="selection.length === 0"
          @click="handleChangeMachine"
          >{{ $t("ui.data.column.scheduleResult.changeMachine") }}</el-button
        >
        <el-button
          v-hasPermi="['dj:djScheduleResult:balance']"
          type="primary"
          @click="handleBalance"
          >{{ $t("ui.data.column.scheduleResult.balance") }}</el-button
        >
        <el-button
          v-hasPermi="['dj:djScheduleResult:mergeProduct']"
          type="primary"
          @click="handleMergeProduct"
          >{{ $t("ui.data.column.scheduleResult.mergeProduct") }}</el-button
        >
        <el-button
          v-hasPermi="['dj:djScheduleResult:combinationMiddleAndNight']"
          type="primary"
          :disabled="selection.length == 0"
          @click="handleCombinationMiddleAndNight"
          >{{ $t("ui.data.column.combinationMiddleAndNight") }}</el-button
        >

        <el-button
          v-hasPermi="['dj:djScheduleResult:publish']"
          type="primary"
          :disabled="selection.length === 0"
          @click="handlePublish"
          >{{ $t("ui.data.column.scheduleResult.publish") }}</el-button
        >
        <el-button
          type="primary"
          @click="handleExportUiExcel"
        >
          {{ $t("ui.frame.btn.export") }}
        </el-button>
        <el-button
          type="primary"
          @click="$refs.tltUploadForm.handleImport(importDefaultValue)"
        >
          {{ $t("ui.frame.btn.import") }}
        </el-button>
        <el-button
          type="primary"
          v-hasPermi="['dj:finishQty:import']"
          @click="$refs.tltUploadForm3.handleImport(importDefaultValue)"
        >
          {{ $t("完成量导入") }}
        </el-button>
        <el-button
          type="primary"
          v-hasRole="['admin']"
          @click="handleChangeReleaseStatus"
        >
          {{ $t("ui.data.column.scheduleResult.changeReleaseStatus") }}
        </el-button>
      </template>
      <template slot="headerRight">
        <span class="stat-info">
          <span>{{ $t("ui.data.column.scheduleResult.class." + shiftSuffixes[0]) }}合计：<span class="stat-value">{{
              stat.class1PlanQty === null ? "--" : stat.class1PlanQty
            }}</span
            >，</span
          >
          <span>{{ $t("ui.data.column.scheduleResult.class." + shiftSuffixes[1]) }}合计：<span class="stat-value">{{
              stat.class2PlanQty === null ? "--" : stat.class2PlanQty
            }}</span
            >，</span
          >
          <span>{{ $t("ui.data.column.scheduleResult.class." + shiftSuffixes[2]) }}合计：<span class="stat-value">{{
              stat.class3PlanQty === null ? "--" : stat.class3PlanQty
            }}</span
            >，</span
          >
          <span>{{ $t("ui.data.column.scheduleResult.stockQty") }}合计：<span class="stat-value">{{
              stat.stockQty === null ? "--" : stat.stockQty
            }}</span
            ></span
          >
        </span>
      </template>
    </page-table>
    <!-- <el-button style="display: none" ref="hidePopoverBtnRef"></el-button> -->
    <autoPlanDialog ref="autoPlanRef" @success="handleAutoPlanSuccess" />
    <addDialog ref="addRef" @success="getList" />
    <editDialog ref="editRef" @success="getList" />
    <changeMachineDialog ref="changeMachineRef" @success="getList" />
    <changePlanDialog ref="changePlanRef" @success="getList" />
    <mergeDialog ref="mergeRef" @success="getList" />
    <releaseStatusDialog
      ref="releaseStatusRef"
      :scheduleDate="this.query.scheduleDate"
      @success="getList"
    />
    <balanceDialog ref="balanceRef" @success="getList" />
    <allocateDialog
      ref="allocRef"
      :scheduleDate="this.query.scheduleDate"
      @success="getList"
    />
    <tlt-upload-form
      ref="tltUploadForm"
      :title="$t('ui.data.column.djScheduleResult.importTitle')"
      downloadUrl="/dj/djScheduleResult/importTemplate"
      uploadUrl="/dj/djScheduleResult/importScheduleData"
      @uploadSuccess="getList"
      :columns="[
        {
          label: this.$t('ui.data.column.scheduleResult.scheduleDate'),
          prop: 'scheduleDate',
          align: 'center',
          minWidth: 120,
        },
      ]"
      :rules="importRules"
    />
    <tlt-upload-form
      ref="tltUploadForm3"
      :title="$t('ui.data.column.djScheduleResult.importTitle')"
      uploadUrl="/dj/djScheduleResult/importFinishQty"
      downloadUrl="/dj/djScheduleResult/importFinishQtyTemplate"
      @uploadSuccess="getList"
      :columns="[
        {
          label: this.$t('ui.data.column.scheduleResult.scheduleDate'),
          prop: 'scheduleDate',
          align: 'center',
          minWidth: 120,
        },
      ]"
      :rules="importRules"
    />
  </basic-container>
</template>
<script>
//lib
import moment from "moment";
import { mapState } from "vuex";
//utils
import { downloadLink } from "@/utils/request";
//interface
import {
  listScheduleResult,
  removeScheduleResult,
  publishValidate,
  publishScheduleResult,
  modifyQty,
  manualClose,
  producingIssue,
  validateConstruction,
  getSummaryVo,
  getWorkClass,
} from "@/api/dj/djScheduleResult";
import { getConfigKey } from "@/api/system/config";
//components
import TltUploadForm from "@/views/components/tltUploadForm.vue";

import autoPlanDialog from "./components/autoPlanDialog.vue";
import addDialog from "./components/addDialog.vue";
import editDialog from "./components/editDialog.vue";
import changeMachineDialog from "./components/changeMachineDialog.vue";
import changePlanDialog from "./components/changePlanDialog.vue";
import releaseStatusDialog from "./components/releaseStatusDialog.vue";
import balanceDialog from "./components/balanceDialog.vue";
import allocateDialog from "./components/allocateDialog.vue";
import mergeDialog from "./components/mergeDialog.vue";

export default {
  name: "djSchedule",
  components: {
    allocateDialog,
    autoPlanDialog,
    addDialog,
    editDialog,
    changeMachineDialog,
    changePlanDialog,
    releaseStatusDialog,
    balanceDialog,
    TltUploadForm,
    mergeDialog,
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
      classHeaders: [],
      // page: {
      //   current: 1,
      //   pageSize: 20,
      //   total: 0,
      // },
      page: {
        current: 1,
        pageSize: 20,
        total: 0,
        pageSizes: [10, 20, 50, 100, 200, 500],
      },
      sort: {},
      search: {},
      query: {},
      importDefaultValue: {
        scheduleDate: moment().add(1, "days").format("YYYY-MM-DD"),
      },
      importRules: {
        scheduleDate: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "blur",
          },
        ],
      },
      stat: {},
      showPrevDayClass1: false,
      scheduleShiftClass: '01',
    };
  },
  computed: {
    ...mapState({
      machines: (state) => state.dj.machines,
    }),
    // 排产起始班次对应的3个连续班次国际化后缀
    shiftSuffixes() {
      const map = {
        '01': ['night', 'morning', 'day'],
        '02': ['morning', 'day', 'night'],
        '03': ['day', 'night', 'morning'],
      };
      return map[this.scheduleShiftClass] || map['01'];
    },
    columns() {
      let finishRateFormatter = function (row, column, value, index) {
        if (value == 0 || value == null) {
          return "0%";
        }
        var str = Number(value * 100).toFixed(2);
        return (str += "%");
      };
      let columns = [
        { type: "selection", fixed: "left" },
        {
          prop: "scheduleDate",
          valign: "middle",
          align: 'center',
          halign: 'center',
          minWidth: 110,
          label: this.$t("ui.data.column.scheduleResult.scheduleDate"),
        },
        {
          prop: "factoryCode",
          valign: "middle",
          align: 'center',
          halign: 'center',
          label: this.$t("ui.data.column.factoryCode"),
          dictData: this.dict.type.biz_factory_name,
          formatter: (row, column, value, index) => {
            return this.selectDictLabel(this.dict.type.biz_factory_name, value);
          },
        },
        {
          prop: "releaseStatus",
          valign: "middle",
          align: 'center',
          halign: 'center',
          label: this.$t("ui.data.column.dj.scheduleResult.releaseStatus"),
          formatter: (row, column, value, index) => {
            return this.selectDictLabel(this.dict.type.IS_RELEASE, value);
          },
        },
        {
          prop: "paddingName",
          valign: "middle",
          halign: 'center',
          align: 'center',
          minWidth: 120,
          label: this.$t("ui.data.column.dj.scheduleResult.paddingCode"),
        },
        {
          prop: "glueCode",
          valign: "middle",
          halign: 'center',
          align: "left",
          minWidth: 120,
          label: this.$t("ui.data.column.dj.scheduleResult.glueCode"),
        },
        {
          prop: "machineCode",
          valign: "middle",
          halign: 'center',
          align: "left",
          label: this.$t("ui.data.column.dj.scheduleResult.machineCode"),
          formatter: (row, column, value, index) => {
            return row.machineName;
          },
        },
        {
          prop: "stockQty",
          valign: "middle",
          halign: 'center',
          align: "right",
          label: this.$t("ui.data.column.scheduleResult.stockQty"),
        },
        {
          label: this.classHeaders[0],
          children: [
            {
              prop: "prevDayClass3Sequence",
              valign: "middle",
              halign: 'center',
              align: 'center',
              label: this.$t("ui.data.column.dj.scheduleResult.sequence"),
            },
            {
              prop: "prevDayClass3PlanQty",
              valign: "middle",
              halign: 'center',
              align: "right",
              label: this.$t("ui.data.column.dj.scheduleResult.planQty"),
            },
            {
              prop: "prevDayClass3FinishQty",
              valign: "middle",
              halign: 'center',
              align: "right",
              label: this.$t("ui.data.column.dj.scheduleResult.finishQty"),
            },
            {
              prop: "prevDayClass3FinishRate",
              valign: "middle",
              halign: 'center',
              align: "right",
              label: this.$t("ui.data.column.scheduleResult.finish"),
              formatter: finishRateFormatter,
            },
            {
              prop: "prevDayClass3Analysis",
              valign: "middle",
              halign: 'center',
              align: "left",
              label: this.$t("ui.data.column.dj.scheduleResult.analysis"),
            },
          ],
        },
        // T-1 日中班栏位（仅首班班次为夜班时显示）
        ...(this.showPrevDayClass1 ? [{
          label: this.$t("ui.data.column.dj.scheduleResult.prevDayClass1"),
          children: [
            {
              prop: "prevDayClass1Sequence",
              valign: "middle",
              halign: 'center',
              align: 'center',
              label: this.$t("ui.data.column.dj.scheduleResult.sequence"),
            },
            {
              prop: "prevDayClass1PlanQty",
              valign: "middle",
              halign: 'center',
              align: "right",
              label: this.$t("ui.data.column.dj.scheduleResult.planQty"),
            },
            {
              prop: "prevDayClass1FinishQty",
              valign: "middle",
              halign: 'center',
              align: "right",
              label: this.$t("ui.data.column.dj.scheduleResult.finishQty"),
            },
            {
              prop: "prevDayClass1FinishRate",
              valign: "middle",
              halign: 'center',
              align: "right",
              label: this.$t("ui.data.column.scheduleResult.finish"),
              formatter: finishRateFormatter,
            },
            {
              prop: "prevDayClass1Analysis",
              valign: "middle",
              halign: 'center',
              align: "left",
              label: this.$t("ui.data.column.dj.scheduleResult.analysis"),
            },
          ],
        }] : []),
        {
          label: this.classHeaders[1],
          children: [
            {
              prop: "class1Sequence",
              valign: "middle",
              halign: 'center',
              align: 'center',
              label: this.$t("ui.data.column.dj.scheduleResult.sequence"),
            },
            {
              prop: "class1PlanQty",
              valign: "middle",
              halign: 'center',
              align: "right",
              label: this.$t("ui.data.column.dj.scheduleResult.planQty"),
            },
            {
              prop: "class1FinishQty",
              valign: "middle",
              halign: 'center',
              align: "right",
              label: this.$t("ui.data.column.dj.scheduleResult.finishQty"),
            },
            {
              prop: "class1FinishRate",
              valign: "middle",
              halign: 'center',
              align: "right",
              label: this.$t("ui.data.column.scheduleResult.finish"),
              formatter: finishRateFormatter,
            },
            {
              prop: "class1Analysis",
              valign: "middle",
              halign: 'center',
              align: "left",
              label: this.$t("ui.data.column.dj.scheduleResult.analysis"),
            },
          ],
        },
        {
          label: this.classHeaders[2],
          children: [
            {
              prop: "class2Sequence",
              valign: "middle",
              halign: 'center',
              align: 'center',
              label: this.$t("ui.data.column.dj.scheduleResult.sequence"),
            },
            {
              prop: "class2PlanQty",
              valign: "middle",
              halign: 'center',
              align: "right",
              label: this.$t("ui.data.column.dj.scheduleResult.planQty"),
            },
            {
              prop: "class2FinishQty",
              valign: "middle",
              halign: 'center',
              align: "right",
              label: this.$t("ui.data.column.dj.scheduleResult.finishQty"),
            },
            {
              prop: "class2FinishRate",
              valign: "middle",
              halign: 'center',
              align: "right",
              label: this.$t("ui.data.column.scheduleResult.finish"),
              formatter: finishRateFormatter,
            },
            {
              prop: "class2Analysis",
              valign: "middle",
              halign: 'center',
              align: "left",
              label: this.$t("ui.data.column.dj.scheduleResult.analysis"),
            },
          ],
        },
        {
          label: this.classHeaders[3],
          children: [
            {
              prop: "class3Sequence",
              valign: "middle",
              halign: 'center',
              align: 'center',
              label: this.$t("ui.data.column.dj.scheduleResult.sequence"),
            },
            {
              prop: "class3PlanQty",
              valign: "middle",
              halign: 'center',
              align: "right",
              label: this.$t("ui.data.column.dj.scheduleResult.planQty"),
            },
            {
              prop: "class3FinishQty",
              valign: "middle",
              halign: 'center',
              align: "right",
              label: this.$t("ui.data.column.dj.scheduleResult.finishQty"),
            },
            {
              prop: "class3FinishRate",
              valign: "middle",
              halign: 'center',
              align: "right",
              label: this.$t("ui.data.column.scheduleResult.finish"),
              formatter: finishRateFormatter,
            },
            {
              prop: "class3Analysis",
              valign: "middle",
              halign: 'center',
              align: "left",
              label: this.$t("ui.data.column.dj.scheduleResult.analysis"),
            },
          ],
        },
        {
          label: this.classHeaders[4],
          children: [
            {
              prop: "class4Sequence",
              valign: "middle",
              halign: 'center',
              align: 'center',
              label: this.$t("ui.data.column.dj.scheduleResult.sequence"),
            },
            {
              prop: "class4PlanQty",
              valign: "middle",
              halign: 'center',
              align: "right",
              label: this.$t("ui.data.column.dj.scheduleResult.planQty"),
            },
            {
              prop: "class4FinishQty",
              valign: "middle",
              halign: 'center',
              align: "right",
              label: this.$t("ui.data.column.dj.scheduleResult.finishQty"),
            },
            {
              prop: "class4FinishRate",
              valign: "middle",
              halign: 'center',
              align: "right",
              label: this.$t("ui.data.column.scheduleResult.finish"),
              formatter: finishRateFormatter,
            },
            {
              prop: "class4Analysis",
              valign: "middle",
              halign: 'center',
              align: "left",
              label: this.$t("ui.data.column.dj.scheduleResult.analysis"),
            },
          ],
        },
        {
          label: this.classHeaders[5],
          children: [
            {
              prop: "class5Sequence",
              valign: "middle",
              halign: 'center',
              align: 'center',
              label: this.$t("ui.data.column.dj.scheduleResult.sequence"),
            },
            {
              prop: "class5PlanQty",
              valign: "middle",
              halign: 'center',
              align: "right",
              label: this.$t("ui.data.column.dj.scheduleResult.planQty"),
            },
            {
              prop: "class5FinishQty",
              valign: "middle",
              halign: 'center',
              align: "right",
              label: this.$t("ui.data.column.dj.scheduleResult.finishQty"),
            },
            {
              prop: "class5FinishRate",
              valign: "middle",
              halign: 'center',
              align: "right",
              label: this.$t("ui.data.column.scheduleResult.finish"),
              formatter: finishRateFormatter,
            },
            {
              prop: "class5Analysis",
              valign: "middle",
              halign: 'center',
              align: "left",
              label: this.$t("ui.data.column.dj.scheduleResult.analysis"),
            },
          ],
        },
        {
          label: this.classHeaders[6],
          children: [
            {
              prop: "class6Sequence",
              valign: "middle",
              halign: 'center',
              align: 'center',
              label: this.$t("ui.data.column.dj.scheduleResult.sequence"),
            },
            {
              prop: "class6PlanQty",
              valign: "middle",
              halign: 'center',
              align: "right",
              label: this.$t("ui.data.column.dj.scheduleResult.planQty"),
            },
            {
              prop: "class6FinishQty",
              valign: "middle",
              halign: 'center',
              align: "right",
              label: this.$t("ui.data.column.dj.scheduleResult.finishQty"),
            },
            {
              prop: "class6FinishRate",
              valign: "middle",
              halign: 'center',
              align: "right",
              label: this.$t("ui.data.column.scheduleResult.finish"),
              formatter: finishRateFormatter,
            },
            {
              prop: "class6Analysis",
              valign: "middle",
              halign: 'center',
              align: "left",
              label: this.$t("ui.data.column.dj.scheduleResult.analysis"),
            },
          ],
        },
      ];

      return columns;
    },
    searchColumns() {
      return [
        {
          label: this.$t("ui.data.column.factoryCode"),
          prop: "factoryCode",
          type: "select",
          dictData: this.dict.type.biz_factory_name,
          filterable: true,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.scheduleDate"),
          prop: "scheduleDate",
          type: "date",
          valueFormat: "yyyy-MM-dd",
          listeners: {
            change: this.handleScheduleDateChange,
          },
        },
        {
          label: this.$t("ui.data.column.dj.scheduleResult.paddingCode"),
          prop: "paddingCode",
        },
        {
          label: this.$t("ui.data.column.dj.scheduleResult.glueCode"),
          prop: "glueCode",
        },
        {
          label: this.$t("ui.data.column.dj.scheduleResult.releaseStatus"),
          prop: "releaseStatus",
          type: "select",
          dictData: this.dict.type.IS_RELEASE, // "IS_RELEASE",
        },
        {
          label: this.$t("ui.data.column.dj.scheduleResult.machineCode"),
          prop: "machineCode",
          type: "select",
          dictData: this.machines,
          valueKey: "id",
          labelKey: "machineName",
        },
        // {
        //   label: this.$t("ui.data.column.scheduleResult.supplyTime"),
        //   prop: "hasVersion",
        //   type: "date",
        //   dateType: "daterange",
        //   valueFormat: "yyyy-MM-dd",
        // },
      ];
    },
  },
  methods: {
    handleAdd() {
      if (this.$refs.addRef) {
        this.$refs.addRef.show();
      }
    },
    handleEdit() {
      if (this.selection.length !== 1) {
        this.$modal.msgWarning("请选择一条记录");
        return;
      }
      if (this.$refs.editRef) {
        this.$refs.editRef.show(this.selection[0]);
      }
    },
    handleDelete(row) {
      this.$confirm(this.$t("common.confirm.delete"), {
        type: "warning",
      }).then(() => {
        let ids = this.selection.map((row) => row.id).join(",");
        ids = ids + "|" + this.query.scheduleDate;
        removeScheduleResult({ ids }).then((data) => {
          this.$modal.msgSuccess(data.msg);
          // this.$set(this.page, "current", 1);
          this.getList();
        });
      });
    },
    // 转机台弹窗
    handleChangeMachine() {
      if (this.$refs.changeMachineRef) {
        let row = this.selection;
        this.$refs.changeMachineRef.show(row);
      }
    },

    // 调量
    handleChangePlan() {
      if (this.$refs.editRef) {
        let row = this.selection[0];
        this.$refs.changePlanRef.show(row);
      }
    },
    handleBalance() {
      if (this.$refs.balanceRef) {
        let row = this.selection[0];
        this.$refs.balanceRef.show(row);
      }
    },
    handleMergeProduct() {
      this.$refs.mergeRef.show();
    },
    handleCombinationMiddleAndNight() {
      if (this.$refs.allocRef) {
        let ids = this.selection.map((row) => row.id).join(",");
        this.$refs.allocRef.show(ids);
      }
    },

    async handlePublish() {
      this.$confirm(this.$t("ui.biz.alter.makeSurePublish")).then(() => {
        this.publishSchedule();
      });
    },

    handleScheduleDateChange(val) {
      this.search = {
        ...this.search,
        scheduleDate: val,
      };
      this.query = {
        ...this.query,
        scheduleDate: val,
      };
      if (this.page) {
        this.$set(this.page, "current", 1);
      }
      this.getList();
      getWorkClass({ scheduleDate: this.getEffectiveScheduleDate() }).then((res) => {
        this.classHeaders = res;
      });
    },
    handleSearch(data) {
      this.query = data;
      this.$set(this.page, "current", 1);
      this.getList();

      // 更新班次表头（排程日期变更后需要重新获取班次）
      getWorkClass({ scheduleDate: this.getEffectiveScheduleDate() }).then((res) => {
        this.classHeaders = res;
      });
    },
    handlePageChange(current, pageSize) {
      // this.$set(this.page, "current", current);
      // this.$set(this.page, "pageSize", pageSize);
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
        //默认排序
        this.sort = {};
      }
      this.getList();
    },
    handleAutoPlan() {
      console.log("handleAutoPlan");
      if (this.$refs.autoPlanRef) {
        this.$refs.autoPlanRef.show("", "1", this.query.factoryCode);
      }
    },
    // 自动排程成功后，更新排程日期并刷新列表
    handleAutoPlanSuccess(scheduleDate) {
      if (scheduleDate) {
        this.$set(this.query, 'scheduleDate', scheduleDate);
        this.search = { ...this.search, scheduleDate };
      }
      this.getList();
      if (scheduleDate) {
        getWorkClass({ scheduleDate }).then((res) => {
          this.classHeaders = res;
        });
      }
    },

    handleExportUiExcel() {
      downloadLink("/dj/djScheduleResult/export", this.formatParams(false));
    },

    handleChangeReleaseStatus() {
      this.$refs.releaseStatusRef.show();
    },
    handleSelectionChange(rows) {
      this.selection = rows;
    },

    // utils
    /** 获取有效排程日期：查询条件为空时默认 T-1 */
    getEffectiveScheduleDate() {
      return this.query.scheduleDate || moment().add(1, "days").format("YYYY-MM-DD");
    },
    updateTableHeaderTitle() {
      //  TODO 更新表头标题
    },
    rowStyles({ row }) {
      if (row.markCloseOutTip == "0") {
        return { "background-color": "#FFFFBF" };
      }
      //插单背景色
      if (row.dataSource == "1") {
        return { "background-color": "#BFE0F7" };
      }
      return {};
    },
    cellStyle({ row, column, rowIndex, columnIndex }) {
      if (column.property === "machineId") {
        if (row.changeMachine == 1) {
          return { background: "#ef6776" };
        }
      }
      if (column.property === "dayPlanQty") {
        if (row.changeDayPlan == 1) {
          return { background: "#ef6776" };
        }
        if (row.dayPlanQty > 0 && row.nightPlanQty > 0) {
          return { background: "yellow" };
        }
      }
      if (column.property === "nightPlanQty") {
        if (row.changeNightPlan == 1) {
          return { background: "#ef6776" };
        }
        if (row.dayPlanQty > 0 && row.nightPlanQty > 0) {
          return { background: "yellow" };
        }
      }

      return {};
    },
    formatParams() {
      const params = {
        // pageSize: this.page.pageSize,
        // pageNum: this.page.current,
        ...this.query,
        ...this.sort,
      };

      // 排程日期为空时默认 T-1
      if (!params.scheduleDate) {
        params.scheduleDate = this.getEffectiveScheduleDate();
      }

      if (params.createTime && params.createTime[0]) {
        params.createTimeStart = params.createTime[0];
        params.createTimeEnd = params.createTime[1];
        params.createTime = undefined;
      }

      return params;
    },
    // api
    async getList() {
      try {
        this.loading = true;
        await this.getStat();
        const data = await listScheduleResult(this.formatParams());
        console.log(data);
        this.data = data.rows;
        // 记录排产起始班次用于动态显示汇总标签（从getSummaryVo接口获取）
        this.scheduleShiftClass = this.stat && this.stat.scheduleShiftClass
          ? this.stat.scheduleShiftClass : '01';
        // 根据首班班次决定是否展示 T-1 日中班栏位
        this.showPrevDayClass1 = this.stat && this.stat.scheduleShiftClass === '01';
        // this.page.total = data.total;
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
    },
    async getStat() {
      try {
        const data = await getSummaryVo(this.formatParams());
        console.log(data);
        this.stat = data;
      } catch (error) {
        console.error(error);
      }
    },
    async publishSchedule() {
      try {
        this.loading = true;
        let ids = this.selection.map((row) => row.id).join(",");
        const res = await publishScheduleResult({
          ids: ids,
          scheduleDate: this.query.scheduleDate,
        });
        this.$modal.msgSuccess(res.msg);
        this.loading = false;
      } catch (error) {
        console.error(error);
        this.loading = false;
      }
    },
  },
  created() {
    // 清除之前持久化的错误列顺序，保留拖拽功能但不再恢复错误状态
    // localStorage.removeItem("djScheduleMainTable");
    //设置默认排程时间
    let date = moment().add(1, "days").format("YYYY-MM-DD");
    // date = "2023-06-01"; //test
    this.query.scheduleDate = date;
    this.search.scheduleDate = date;

    getConfigKey("sys.factory.code").then(response => {
      this.search.factoryCode = response.msg;
      this.query.factoryCode = response.msg;
      this.$store.dispatch("dj/getMachineList");
    }).catch(() => {
      this.$store.dispatch("dj/getMachineList");
    });

    //获取班次表头
    getWorkClass({ scheduleDate: this.getEffectiveScheduleDate() }).then((res) => {
      this.classHeaders = res;
    });
    this.getList();
  },
  activated() {
    this.getList();
  },
};
</script>
<style lang="scss" scoped>
.more-btn {
  margin: 2px 0;
  width: 100%;
}
</style>
