<template>
  <el-dialog
    :title="title"
    :visible="visible"
    width="1300px"
    @close="hide"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
  >
    <div v-loading="loading" class="change-machine-dialog">
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="right"
        label-width="120px"
      >
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item :label="$t('ui.data.column.scheduleResult.scheduleDate')">
              <el-input v-model="form.scheduleDate" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="$t('ui.data.column.scheduleResult.oldMachine')">
              <el-input v-model="form.oldMachineCode" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="$t('ui.data.column.scheduleResult.newMachine')" prop="lhMachineCode">
              <el-select
                v-model="form.lhMachineCode"
                class="w100"
                filterable
                clearable
              >
                <el-option
                  v-for="item in machineOptions"
                  :key="item.machineCode"
                  :label="item.machineCode"
                  :value="item.machineCode"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <page-table
        tableRef="curingScheduleChangeMachineResultTable"
        :columns="tableColumns"
        :data="tableData"
        :showSummary="false"
        :selectArea="false"
      />
    </div>
    <template slot="footer">
      <el-button @click="hide">{{ this.$t("common.button.cancel") }}</el-button>
      <el-button type="primary" :loading="loading" @click="handleConfirm">{{
        this.$t("common.button.confirm")
      }}</el-button>
    </template>
  </el-dialog>
</template>

<script>
import PageTable from "@/components/Table/PageTable.vue";
import {listMachine} from "@/api/lh/machine";
import {changeMachine, validateChangeMachine} from "@/api/lh/scheduleResult";

export default {
  components: { PageTable },
  inject: ["parentDict"],
  data() {
    return {
      loading: false,
      visible: false,
      isEdit: false,
      form: {},
      tableData: [],
      machineOptions: [],
      dateList: Array.from({ length: 8 }, () => ({ shiftDate: "" })),
      rules: {
        lhMachineCode: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
      },
    };
  },
  computed: {
    title: function () {
      return this.$t("ui.data.column.scheduleResult.changeMachine");
    },
    tableColumns() {
      return [
        {
          label: this.$t("ui.data.column.scheduleResult.lhMachineCode"),
          prop: "lhMachineCode",
        },
        {
          label: this.$t("ui.data.column.scheduleResult.materialCode"),
          prop: "materialCode",
          width: 320
        },
        {
          label: this.$t("ui.data.column.scheduleResult.materialDesc"),
          prop: "materialDesc",
          width: 320
        },
        {
          label: this.$t("ui.data.column.scheduleResult.embryoDesc"),
          prop: "mainMaterialDesc",
        },
        {
          label: this.$t("ui.data.column.scheduleResult.scheduleType"),
          prop: "scheduleType",
          minWidth: 100,
          formatter: (row, column, value) => {
            return this.selectDictLabel(
              this.parentDict.type.lh_schedule_type,
              value
            );
          },
        },
        {
          label: this.$t("ui.data.column.scheduleResult.totalSurplusQty"),
          prop: "mouldSurplusQty",
        },
        {
          label: this.$t("ui.data.column.scheduleResult.finishQty"),
          prop: "todayNightFinishQty",
        },
        {
          label: this.$t("ui.data.column.scheduleResult.embryoStock"),
          prop: "embryoStock",
        },
        {
          label: this.$t("ui.data.column.scheduleResult.lhShiftQty"),
          prop: "singleMouldShiftQty",
        },
        {
          label: this.$t("ui.data.column.scheduleResult.leftRightMould"),
          prop: "leftRightMould",
        },
        {
          label: this.$t("ui.data.column.scheduleResult.constructionStage"),
          prop: "trialStatus",
          formatter: (row, column, value) => {
            return this.selectDictLabel(
              this.parentDict.type.lh_trial_status,
              value
            );
          },
        },
        {
          label: this.$t("ui.data.column.scheduleResult.type"),
          prop: "isEnd",
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.parentDict.type.biz_end_type, value);
          },
        },
        {
          label: this.$t("ui.data.column.scheduleResult.morningShift") + " " + this.dateList[0].shiftDate,
          children: [
            {
              prop: "leftRightMould",
              label: this.$t("ui.data.column.scheduleResult.leftRightMould"),
              formatter: (row, column, value) => this.shiftLeftRightMouldFormatter(row, column, value, 1),
            },
            {
              prop: "trialStatus",
              label: this.$t("ui.data.column.scheduleResult.constructionStage"),
              formatter: (row, column, value) => this.shiftConstructionStageFormatter(row, column, value, 1),
            },
            {
              prop: "class1IsEnd",
              label: this.$t("ui.data.column.scheduleResult.type"),
              formatter: (row, column, value) => this.calcShiftIsEnd(row, 1),
            },
            {
              prop: "class1PlanQty",
              label: this.$t("ui.data.column.scheduleResult.plan"),
              formatter: (row, column, value) => this.shiftPlanQtyFormatter(row, column, value, 1),
            },
            {
              prop: "class1FinishQty",
              label: this.$t("ui.data.column.scheduleResult.actual"),
              formatter: (row, column, value) => this.shiftFinishQtyFormatter(row, column, value, 1),
            },
            {
              prop: "class1Analysis",
              label: this.$t("ui.data.column.scheduleResult.analysis"),
              formatter: (row, column, value) => this.shiftAnalysisFormatter(row, column, value, 1),
            },
          ],
        },
        {
          label: this.$t("ui.data.column.scheduleResult.middleShift") + " " + this.dateList[1].shiftDate,
          children: [
            {
              prop: "leftRightMould",
              label: this.$t("ui.data.column.scheduleResult.leftRightMould"),
              formatter: (row, column, value) => this.shiftLeftRightMouldFormatter(row, column, value, 2),
            },
            {
              prop: "trialStatus",
              label: this.$t("ui.data.column.scheduleResult.constructionStage"),
              formatter: (row, column, value) => this.shiftConstructionStageFormatter(row, column, value, 2),
            },
            {
              prop: "class2IsEnd",
              label: this.$t("ui.data.column.scheduleResult.type"),
              formatter: (row, column, value) => this.calcShiftIsEnd(row, 2),
            },
            {
              prop: "class2PlanQty",
              label: this.$t("ui.data.column.scheduleResult.plan"),
              formatter: (row, column, value) => this.shiftPlanQtyFormatter(row, column, value, 2),
            },
            {
              prop: "class2FinishQty",
              label: this.$t("ui.data.column.scheduleResult.actual"),
              formatter: (row, column, value) => this.shiftFinishQtyFormatter(row, column, value, 2),
            },
            {
              prop: "class2Analysis",
              label: this.$t("ui.data.column.scheduleResult.analysis"),
              formatter: (row, column, value) => this.shiftAnalysisFormatter(row, column, value, 2),
            },
          ],
        },
        {
          label: this.$t("ui.data.column.scheduleResult.nightShift") + " " + this.dateList[2].shiftDate,
          children: [
            {
              prop: "leftRightMould",
              label: this.$t("ui.data.column.scheduleResult.leftRightMould"),
              formatter: (row, column, value) => this.shiftLeftRightMouldFormatter(row, column, value, 3),
            },
            {
              prop: "trialStatus",
              label: this.$t("ui.data.column.scheduleResult.constructionStage"),
              formatter: (row, column, value) => this.shiftConstructionStageFormatter(row, column, value, 3),
            },
            {
              prop: "class3IsEnd",
              label: this.$t("ui.data.column.scheduleResult.type"),
              formatter: (row, column, value) => this.calcShiftIsEnd(row, 3),
            },
            {
              prop: "class3PlanQty",
              label: this.$t("ui.data.column.scheduleResult.plan"),
              formatter: (row, column, value) => this.shiftPlanQtyFormatter(row, column, value, 3),
            },
            {
              prop: "class3FinishQty",
              label: this.$t("ui.data.column.scheduleResult.actual"),
              formatter: (row, column, value) => this.shiftFinishQtyFormatter(row, column, value, 3),
            },
            {
              prop: "class3Analysis",
              label: this.$t("ui.data.column.scheduleResult.analysis"),
              formatter: (row, column, value) => this.shiftAnalysisFormatter(row, column, value, 3),
            },
          ],
        },
        {
          label: this.$t("ui.data.column.scheduleResult.morningShift") + " " + this.dateList[3].shiftDate,
          children: [
            {
              prop: "leftRightMould",
              label: this.$t("ui.data.column.scheduleResult.leftRightMould"),
              formatter: (row, column, value) => this.shiftLeftRightMouldFormatter(row, column, value, 4),
            },
            {
              prop: "trialStatus",
              label: this.$t("ui.data.column.scheduleResult.constructionStage"),
              formatter: (row, column, value) => this.shiftConstructionStageFormatter(row, column, value, 4),
            },
            {
              prop: "class4IsEnd",
              label: this.$t("ui.data.column.scheduleResult.type"),
              formatter: (row, column, value) => this.calcShiftIsEnd(row, 4),
            },
            {
              prop: "class4PlanQty",
              label: this.$t("ui.data.column.scheduleResult.plan"),
              formatter: (row, column, value) => this.shiftPlanQtyFormatter(row, column, value, 4),
            },
            {
              prop: "class4FinishQty",
              label: this.$t("ui.data.column.scheduleResult.actual"),
              formatter: (row, column, value) => this.shiftFinishQtyFormatter(row, column, value, 4),
            },
            {
              prop: "class4Analysis",
              label: this.$t("ui.data.column.scheduleResult.analysis"),
              formatter: (row, column, value) => this.shiftAnalysisFormatter(row, column, value, 4),
            },
          ],
        },
        {
          label: this.$t("ui.data.column.scheduleResult.middleShift") + " " + this.dateList[4].shiftDate,
          children: [
            {
              prop: "leftRightMould",
              label: this.$t("ui.data.column.scheduleResult.leftRightMould"),
              formatter: (row, column, value) => this.shiftLeftRightMouldFormatter(row, column, value, 5),
            },
            {
              prop: "changedTrialStatus",
              label: this.$t("ui.data.column.scheduleResult.constructionStage"),
              formatter: (row, column, value) => this.shiftConstructionStageFormatter(row, column, value, 5),
            },
            {
              prop: "class5IsEnd",
              label: this.$t("ui.data.column.scheduleResult.type"),
              formatter: (row, column, value) => this.calcShiftIsEnd(row, 5),
            },
            {
              prop: "class5PlanQty",
              label: this.$t("ui.data.column.scheduleResult.plan"),
              formatter: (row, column, value) => this.shiftPlanQtyFormatter(row, column, value, 5),
            },
            {
              prop: "class5FinishQty",
              label: this.$t("ui.data.column.scheduleResult.actual"),
              formatter: (row, column, value) => this.shiftFinishQtyFormatter(row, column, value, 5),
            },
            {
              prop: "class5Analysis",
              label: this.$t("ui.data.column.scheduleResult.analysis"),
              formatter: (row, column, value) => this.shiftAnalysisFormatter(row, column, value, 5),
            },
          ],
        },
        {
          label: this.$t("ui.data.column.scheduleResult.nightShift") + " " + this.dateList[5].shiftDate,
          children: [
            {
              prop: "leftRightMould",
              label: this.$t("ui.data.column.scheduleResult.leftRightMould"),
              formatter: (row, column, value) => this.shiftLeftRightMouldFormatter(row, column, value, 6),
            },
            {
              prop: "changedTrialStatus",
              label: this.$t("ui.data.column.scheduleResult.constructionStage"),
              formatter: (row, column, value) => this.shiftConstructionStageFormatter(row, column, value, 6),
            },
            {
              prop: "class6IsEnd",
              label: this.$t("ui.data.column.scheduleResult.type"),
              formatter: (row, column, value) => this.calcShiftIsEnd(row, 6),
            },
            {
              prop: "class6PlanQty",
              label: this.$t("ui.data.column.scheduleResult.plan"),
              formatter: (row, column, value) => this.shiftPlanQtyFormatter(row, column, value, 6),
            },
            {
              prop: "class6FinishQty",
              label: this.$t("ui.data.column.scheduleResult.actual"),
              formatter: (row, column, value) => this.shiftFinishQtyFormatter(row, column, value, 6),
            },
            {
              prop: "class6Analysis",
              label: this.$t("ui.data.column.scheduleResult.analysis"),
              formatter: (row, column, value) => this.shiftAnalysisFormatter(row, column, value, 6),
            },
          ],
        },
        {
          label: this.$t("ui.data.column.scheduleResult.morningShift") + " " + this.dateList[6].shiftDate,
          children: [
            {
              prop: "leftRightMould",
              label: this.$t("ui.data.column.scheduleResult.leftRightMould"),
              formatter: (row, column, value) => this.shiftLeftRightMouldFormatter(row, column, value, 7),
            },
            {
              prop: "changedTrialStatus",
              label: this.$t("ui.data.column.scheduleResult.constructionStage"),
              formatter: (row, column, value) => this.shiftConstructionStageFormatter(row, column, value, 7),
            },
            {
              prop: "class7IsEnd",
              label: this.$t("ui.data.column.scheduleResult.type"),
              formatter: (row, column, value) => this.calcShiftIsEnd(row, 7),
            },
            {
              prop: "class7PlanQty",
              label: this.$t("ui.data.column.scheduleResult.plan"),
              formatter: (row, column, value) => this.shiftPlanQtyFormatter(row, column, value, 7),
            },
            {
              prop: "class7FinishQty",
              label: this.$t("ui.data.column.scheduleResult.actual"),
              formatter: (row, column, value) => this.shiftFinishQtyFormatter(row, column, value, 7),
            },
            {
              prop: "class7Analysis",
              label: this.$t("ui.data.column.scheduleResult.analysis"),
              formatter: (row, column, value) => this.shiftAnalysisFormatter(row, column, value, 7),
            },
          ],
        },
        {
          label: this.$t("ui.data.column.scheduleResult.middleShift") + " " + this.dateList[7].shiftDate,
          children: [
            {
              prop: "leftRightMould",
              label: this.$t("ui.data.column.scheduleResult.leftRightMould"),
              formatter: (row, column, value) => this.shiftLeftRightMouldFormatter(row, column, value, 8),
            },
            {
              prop: "changedTrialStatus",
              label: this.$t("ui.data.column.scheduleResult.constructionStage"),
              formatter: (row, column, value) => this.shiftConstructionStageFormatter(row, column, value, 8),
            },
            {
              prop: "class8IsEnd",
              label: this.$t("ui.data.column.scheduleResult.type"),
              formatter: (row, column, value) => this.calcShiftIsEnd(row, 8),
            },
            {
              prop: "class8PlanQty",
              label: this.$t("ui.data.column.scheduleResult.plan"),
              formatter: (row, column, value) => this.shiftPlanQtyFormatter(row, column, value, 8),
            },
            {
              prop: "class8FinishQty",
              label: this.$t("ui.data.column.scheduleResult.actual"),
              formatter: (row, column, value) => this.shiftFinishQtyFormatter(row, column, value, 8),
            },
            {
              prop: "class8Analysis",
              label: this.$t("ui.data.column.scheduleResult.analysis"),
              formatter: (row, column, value) => this.shiftAnalysisFormatter(row, column, value, 8),
            },
          ],
        },
        {
          prop: "remark",
          label: this.$t("ui.data.column.remark"),
        },
        {
          prop: "updateTime",
          label: this.$t("ui.data.column.updateTime"),
          minWidth: 180,
        },
      ];
    },
  },
  methods: {
    calcShiftIsEnd(row, shiftIndex) {
      if (this.isShiftAfterEnding(row, shiftIndex)) {
        return "";
      }
      const planQty = row["class" + shiftIndex + "PlanQty"];
      if (planQty == null || planQty <= 0) {
        return "";
      }
      const referenceQty = Math.max(row.mouldSurplusQty || 0, row.embryoStock || 0);
      if (referenceQty <= 0) {
        return this.selectDictLabel(this.parentDict.type.biz_end_type, "0");
      }
      let totalPlanQty = 0;
      for (let i = 1; i <= 8; i++) {
        totalPlanQty += row["class" + i + "PlanQty"] || 0;
      }
      if (totalPlanQty < referenceQty) {
        return this.selectDictLabel(this.parentDict.type.biz_end_type, "0");
      }
      let remaining = referenceQty;
      for (let i = 1; i <= 8; i++) {
        remaining -= row["class" + i + "PlanQty"] || 0;
        if (remaining <= 0) {
          if (i === shiftIndex) {
            return this.selectDictLabel(this.parentDict.type.biz_end_type, "1");
          }
          break;
        }
      }
      return this.selectDictLabel(this.parentDict.type.biz_end_type, "0");
    },
    isShiftAfterEnding(row, shiftIndex) {
      const referenceQty = Math.max(row.mouldSurplusQty || 0, row.embryoStock || 0);
      if (referenceQty <= 0) {
        return false;
      }
      let totalPlanQty = 0;
      for (let i = 1; i <= 8; i++) {
        totalPlanQty += row["class" + i + "PlanQty"] || 0;
      }
      if (totalPlanQty < referenceQty) {
        return false;
      }
      let remaining = referenceQty;
      for (let i = 1; i <= 8; i++) {
        remaining -= row["class" + i + "PlanQty"] || 0;
        if (remaining <= 0) {
          return shiftIndex > i;
        }
      }
      return false;
    },
    shiftLeftRightMouldFormatter(row, column, value, shiftIndex) {
      if (this.isShiftAfterEnding(row, shiftIndex)) return "";
      const planQty = row["class" + shiftIndex + "PlanQty"];
      if (planQty == null || planQty <= 0) return "";
      return value;
    },
    shiftConstructionStageFormatter(row, column, value, shiftIndex) {
      if (this.isShiftAfterEnding(row, shiftIndex)) return "";
      const planQty = row["class" + shiftIndex + "PlanQty"];
      if (planQty == null || planQty <= 0) return "";
      const dictValue = value || "0";
      return this.selectDictLabel(this.parentDict.type.lh_trial_status, dictValue);
    },
    shiftPlanQtyFormatter(row, column, value, shiftIndex) {
      if (this.isShiftAfterEnding(row, shiftIndex)) return "";
      if (value == null || value === 0) return "";
      return value;
    },
    shiftFinishQtyFormatter(row, column, value, shiftIndex) {
      if (this.isShiftAfterEnding(row, shiftIndex)) return "";
      const planQty = row["class" + shiftIndex + "PlanQty"];
      if (planQty == null || planQty <= 0) return "";
      return value;
    },
    shiftAnalysisFormatter(row, column, value, shiftIndex) {
      if (this.isShiftAfterEnding(row, shiftIndex)) return "";
      const planQty = row["class" + shiftIndex + "PlanQty"];
      if (planQty == null || planQty <= 0) return "";
      return value;
    },
    /**
     * 查询可用机台列表（仅查询启用且未被排程结果占用的机台）。
     * @returns {Promise<void>}
     */
    async getMachineOptions() {
      try {
        const scheduleDate = this.form.scheduleDate;
        if (!scheduleDate) {
          this.machineOptions = [];
          this.$modal.msgWarning(this.$t("ui.data.column.scheduleResult.scheduleDateRequired"));
          return;
        }
        const res = await listMachine({
          status: 1,
          params: {
            filterNotExistsScheduleResult: 1,
            scheduleDate,
          },
        });
        this.machineOptions = res.rows || [];
      } catch (error) {
        console.error(error);
      }
    },
    /**
     * 调用转机台相关接口完成机台切换。
     * @param {Object} params 转机台参数（id、factoryCode、lhMachineCode）
     * @returns {Promise<void>}
     */
    async handleChangeMachine(params) {
      try {
        this.loading = true;
        await validateChangeMachine(params);
        const data = await changeMachine(params);
        this.$modal.msgSuccess(data.msg);
        this.$emit("success");
        this.hide();
      } catch (error) {
        console.log(error);
      } finally {
        this.loading = false;
      }
    },
    /**
     * 打开弹窗并初始化表单、排程表格与可选机台。
     * @param {Object} data 硫化排程结果选中行
     * @returns {Promise<void>}
     */
    async show(data) {
      this.visible = true;
      if (data) {
        this.isEdit = true;
        this.dateList = Array.from({ length: 8 }, (item, index) => ({
          shiftDate: data[`class${index + 1}Date`] || "",
        }));
        this.form = {
          ...data,
          oldMachineCode: data.lhMachineCode,
          lhMachineCode: "",
        };
        this.tableData = [{ ...data }];
      }
      await this.getMachineOptions();
    },
    /**
     * 关闭弹窗并清理本次编辑上下文。
     * @returns {void}
     */
    hide() {
      this.form = {};
      this.tableData = [];
      this.machineOptions = [];
      this.dateList = Array.from({ length: 8 }, () => ({ shiftDate: "" }));
      if (this.$refs.formRef) {
        this.$refs.formRef.resetFields();
      }
      this.isEdit = false;
      this.visible = false;
    },
    /**
     * 校验表单并触发转机台提交。
     * @returns {void}
     */
    handleConfirm() {
      this.$refs.formRef.validate((valid) => {
        if (!valid) {
          return;
        }
        const selectedMachine = this.machineOptions.find(
          (item) => item.machineCode === this.form.lhMachineCode
        );
        if (!selectedMachine || !selectedMachine.machineName) {
          this.$modal.msgWarning(
            this.$t("ui.data.column.scheduleResult.newMachineNameNotFound")
          );
          return;
        }
        this.handleChangeMachine({
          lhMachineCode: this.form.lhMachineCode,
          lhMachineName: selectedMachine.machineName,
          id: this.form.id,
          factoryCode: this.form.factoryCode,
        });
      });
    },
  },
};
</script>

<style lang="scss" scoped>
.change-machine-dialog {
  .w100 {
    width: 100%;
  }

  /* 转机台弹窗表格：略增行高与单元格内边距，避免过密 */
  ::v-deep .el-table {
    th.el-table__cell,
    td.el-table__cell {
      padding-top: 14px;
      padding-bottom: 14px;
    }
    .cell {
      line-height: 24px;
      min-height: 24px;
    }
  }
}
</style>
