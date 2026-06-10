<template>
  <el-dialog
    :title="title"
    :visible="visible"
    width="1200px"
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
            <el-form-item :label="$t('ui.data.column.tqNewScheduleResult.scheduleDate')">
              <el-input v-model="form.scheduleDate" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="$t('ui.data.column.tqNewScheduleResult.oldMachine')">
              <el-input v-model="form.oldMachineCode" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="$t('ui.data.column.tqNewScheduleResult.newMachine')" prop="newMachineCode">
              <el-select
                v-model="form.newMachineCode"
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
        tableRef="tqChangeMachineResultTable"
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
import { changeMachine, validateChangeMachine, listScheduleShiftDates } from "@/api/tq/tqNewScheduleResult";
import { listEnabledMachines } from "@/api/tq/machine";

export default {
  components: { PageTable },
  inject: ["parentDict"],
  data() {
    return {
      loading: false,
      visible: false,
      form: {},
      tableData: [],
      machineOptions: [],
      dateList: [
        { shift: 1, shiftType: "afternoon", shiftDate: "" },
        { shift: 2, shiftType: "night", shiftDate: "" },
        { shift: 3, shiftType: "morning", shiftDate: "" },
        { shift: 4, shiftType: "afternoon", shiftDate: "" },
        { shift: 5, shiftType: "night", shiftDate: "" },
        { shift: 6, shiftType: "morning", shiftDate: "" },
      ],
      rules: {
        newMachineCode: [
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
      return this.$t("ui.data.btn.tqNewScheduleResult.changeMachine");
    },
    tableColumns() {
      return [
        {
          label: this.$t("ui.data.column.tqNewScheduleResult.machineCode"),
          prop: "machineCode",
        },
        {
          label: this.$t("ui.data.column.tqNewScheduleResult.beadCode"),
          prop: "beadCode",
        },
        {
          label: this.$t("ui.data.column.tqNewScheduleResult.steelRingCode"),
          prop: "steelRingCode",
        },
        {
          label: this.$t("ui.data.column.tqNewScheduleResult.proSize"),
          prop: "proSize",
        },
        {
          label: this.$t("ui.data.column.tqNewScheduleResult.isRelease"),
          prop: "isRelease",
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.parentDict.type.IS_RELEASE, value);
          },
        },
        // 动态6个班次列
        ...this.buildShiftColumns(1),
        ...this.buildShiftColumns(2),
        ...this.buildShiftColumns(3),
        ...this.buildShiftColumns(4),
        ...this.buildShiftColumns(5),
        ...this.buildShiftColumns(6),
        {
          prop: "remark",
          label: this.$t("ui.common.column.remark"),
        },
      ];
    },
  },
  methods: {
    /** 班次名称映射 */
    shiftPeriodName(shiftType) {
      const map = {
        night: this.$t("ui.data.column.scheduleResult.nightShift"),
        morning: this.$t("ui.data.column.scheduleResult.morningShift"),
        afternoon: this.$t("ui.data.column.scheduleResult.middleShift"),
      };
      return map[shiftType] || "";
    },
    /** 构建单个班次的表格列组 */
    buildShiftColumns(classIndex) {
      const item = this.dateList[classIndex - 1];
      const shiftName = this.shiftPeriodName(item?.shiftType || "");
      const dateStr = item?.shiftDate || "";
      const headerLabel = dateStr ? `${shiftName} ${dateStr}` : shiftName;

      return [
        {
          label: headerLabel,
          children: [
            {
              prop: `class${classIndex}Sequence`,
              label: this.$t("ui.data.column.tqNewScheduleResult.sequence"),
              formatter: (row, column, value) => {
                if (value == null || value === 0) return "";
                return value;
              },
            },
            {
              prop: `class${classIndex}PlanQty`,
              label: this.$t("ui.data.column.tqNewScheduleResult.planQty"),
              formatter: (row, column, value) => {
                if (value == null || value === 0) return "";
                return value;
              },
            },
            {
              prop: `class${classIndex}FinishQty`,
              label: this.$t("ui.data.column.tqNewScheduleResult.finishQty"),
              formatter: (row, column, value) => {
                if (value == null || value === 0) return "";
                return value;
              },
            },
            {
              prop: `class${classIndex}Analysis`,
              label: this.$t("ui.data.column.tqNewScheduleResult.analysis"),
              formatter: (row, column, value) => {
                if (value != null && value !== "") return value;
                return "";
              },
            },
          ],
        },
      ];
    },
    /** 获取班次日期列表 */
    async fetchScheduleShiftDates(scheduleDate) {
      if (!scheduleDate) {
        this.dateList = this.getDefaultDateList();
        return;
      }
      try {
        const res = await listScheduleShiftDates({ scheduleDateQuery: scheduleDate });
        if (Array.isArray(res) && res.length) {
          this.dateList = res;
        } else {
          this.dateList = this.getDefaultDateList();
        }
      } catch (error) {
        console.error(error);
        this.dateList = this.getDefaultDateList();
      }
    },
    getDefaultDateList() {
      return [
        { shift: 1, shiftType: "afternoon", shiftDate: "" },
        { shift: 2, shiftType: "night", shiftDate: "" },
        { shift: 3, shiftType: "morning", shiftDate: "" },
        { shift: 4, shiftType: "afternoon", shiftDate: "" },
        { shift: 5, shiftType: "night", shiftDate: "" },
        { shift: 6, shiftType: "morning", shiftDate: "" },
      ];
    },
    /** 加载可用机台列表 */
    async getMachineOptions() {
      try {
        const res = await listEnabledMachines();
        this.machineOptions = res || [];
      } catch (error) {
        console.error(error);
        this.machineOptions = [];
      }
    },
    /** 执行转机台 */
    async handleChangeMachine(params) {
      try {
        this.loading = true;
        const validateRes = await validateChangeMachine(params);
        if (validateRes.code === 200) {
          const data = await changeMachine(params);
          this.$modal.msgSuccess(data.msg);
          this.$emit("success");
          this.hide();
        } else {
          this.$modal.msgError(validateRes.msg || "校验失败");
        }
      } catch (error) {
        console.log(error);
      } finally {
        this.loading = false;
      }
    },
    /** 打开弹窗 */
    async show(data) {
      this.visible = true;
      if (data) {
        this.form = {
          id: data.id,
          scheduleDate: data.scheduleDate,
          oldMachineCode: data.machineCode,
          newMachineCode: "",
        };
        this.tableData = [{ ...data }];
        await this.fetchScheduleShiftDates(data.scheduleDate);
      }
      await this.getMachineOptions();
    },
    /** 关闭弹窗 */
    hide() {
      this.form = {};
      this.tableData = [];
      this.machineOptions = [];
      this.dateList = this.getDefaultDateList();
      if (this.$refs.formRef) {
        this.$refs.formRef.resetFields();
      }
      this.visible = false;
    },
    /** 确认按钮 */
    handleConfirm() {
      this.$refs.formRef.validate((valid) => {
        if (!valid) {
          return;
        }
        this.handleChangeMachine({
          id: this.form.id,
          oldMachineCode: this.form.oldMachineCode,
          newMachineCode: this.form.newMachineCode,
          scheduleDate: this.form.scheduleDate,
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
