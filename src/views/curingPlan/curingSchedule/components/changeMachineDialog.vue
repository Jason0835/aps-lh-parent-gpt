<template>
  <el-dialog
    :title="title"
    :visible="visible"
    width="1500px"
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
            <el-form-item :label="$t('原机台')">
              <el-input v-model="form.oldMachineCode" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="$t('新机台')" prop="lhMachineCode">
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
import { listMachine } from "@/api/lh/machine";
import { validateChangeMachine, changeMachine } from "@/api/lh/scheduleResult";

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
      return this.$t("转机台");
    },
    tableColumns() {
      return [
        {
          label: this.$t("硫化机台"),
          prop: "lhMachineCode",
        },
        {
          label: this.$t("物料编码"),
          prop: "materialCode",
        },
        {
          label: this.$t("物料描述"),
          prop: "materialDesc",
        },
        {
          label: this.$t("胎胚描述"),
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
          label: this.$t("合计余量"),
          prop: "mouldSurplusQty",
        },
        {
          label: this.$t("胎胚库存"),
          prop: "embryoStock",
        },
        {
          label: this.$t("硫化班产"),
          prop: "singleMouldShiftQty",
        },
        {
          label: this.$t("左右模"),
          prop: "leftRightMould",
        },
        {
          label: this.$t("示方类型"),
          prop: "constructionStage",
          formatter: (row, column, value) => {
            return this.selectDictLabel(
              this.parentDict.type.biz_construction_stage,
              value
            );
          },
        },
        {
          label: this.$t("类型"),
          prop: "isEnd",
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.parentDict.type.biz_end_type, value);
          },
        },
        {
          label: this.$t("早班") + " " + this.dateList[0].shiftDate,
          children: [
            {
              prop: "class1PlanQty",
              label: this.$t("计划"),
            },
            {
              prop: "class1FinishQty",
              label: this.$t("实际"),
            },
            {
              prop: "class1Analysis",
              label: this.$t("备注"),
            },
          ],
        },
        {
          label: this.$t("中班") + " " + this.dateList[1].shiftDate,
          children: [
            {
              prop: "class2PlanQty",
              label: this.$t("计划"),
            },
            {
              prop: "class2FinishQty",
              label: this.$t("实际"),
            },
            {
              prop: "class2Analysis",
              label: this.$t("备注"),
            },
          ],
        },
        {
          label: this.$t("晚班") + " " + this.dateList[2].shiftDate,
          children: [
            {
              prop: "class3PlanQty",
              label: this.$t("计划"),
            },
            {
              prop: "class3FinishQty",
              label: this.$t("实际"),
            },
            {
              prop: "class3Analysis",
              label: this.$t("备注"),
            },
          ],
        },
        {
          label: this.$t("早班") + " " + this.dateList[3].shiftDate,
          children: [
            {
              prop: "class4PlanQty",
              label: this.$t("计划"),
            },
            {
              prop: "class4FinishQty",
              label: this.$t("实际"),
            },
            {
              prop: "class4Analysis",
              label: this.$t("备注"),
            },
          ],
        },
        {
          label: this.$t("中班") + " " + this.dateList[4].shiftDate,
          children: [
            {
              prop: "class5PlanQty",
              label: this.$t("计划"),
            },
            {
              prop: "class5FinishQty",
              label: this.$t("实际"),
            },
            {
              prop: "class5Analysis",
              label: this.$t("备注"),
            },
          ],
        },
        {
          label: this.$t("晚班") + " " + this.dateList[5].shiftDate,
          children: [
            {
              prop: "class6PlanQty",
              label: this.$t("计划"),
            },
            {
              prop: "class6FinishQty",
              label: this.$t("实际"),
            },
            {
              prop: "class6Analysis",
              label: this.$t("备注"),
            },
          ],
        },
        {
          label: this.$t("早班") + " " + this.dateList[6].shiftDate,
          children: [
            {
              prop: "class7PlanQty",
              label: this.$t("计划"),
            },
            {
              prop: "class7FinishQty",
              label: this.$t("实际"),
            },
            {
              prop: "class7Analysis",
              label: this.$t("备注"),
            },
          ],
        },
        {
          label: this.$t("中班") + " " + this.dateList[7].shiftDate,
          children: [
            {
              prop: "class8PlanQty",
              label: this.$t("计划"),
            },
            {
              prop: "class8FinishQty",
              label: this.$t("实际"),
            },
            {
              prop: "class8Analysis",
              label: this.$t("备注"),
            },
          ],
        },
        {
          prop: "remark",
          label: this.$t("备注"),
        },
        {
          prop: "updateTime",
          label: this.$t("ui.data.column.scheduleResult.updateTime"),
          minWidth: 160,
        },
      ];
    },
  },
  methods: {
    /**
     * 查询可用机台列表（仅查询启用且未被排程结果占用的机台）。
     * @returns {Promise<void>}
     */
    async getMachineOptions() {
      try {
        const scheduleDate = this.form.scheduleDate;
        if (!scheduleDate) {
          this.machineOptions = [];
          this.$modal.msgWarning(this.$t("排程日期缺失，无法查询可用机台"));
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
            this.$t("新机台名称获取失败，请重新选择新机台")
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
}
</style>
