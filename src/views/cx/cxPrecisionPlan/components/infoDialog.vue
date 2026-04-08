<template>
  <el-dialog
    :title="title"
    :visible="visible"
    width="700px"
    @close="hide"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :append-to-body="true"
  >
    <info-form
      class="form-item-height cx-precision-form"
      ref="form"
      :form="form"
      :rules="rules"
      :columns="columns"
      label-position="right"
      label-width="120px"
      v-loading="loading"
    >
    </info-form>
    <template slot="footer">
      <el-button @click="hide">{{ this.$t("common.button.cancel") }}</el-button>
      <el-button type="primary" :loading="loading" @click="handleConfirm">{{
        this.$t("common.button.confirm")
      }}</el-button>
    </template>
  </el-dialog>
</template>

<script>
import {
  checkCxPrecisionPlanUnique,
  getMachineList,
  saveCxPrecisionPlan,
} from "@/api/cx/cxPrecisionPlan";

import infoForm from "@/views/components/infoForm.vue";

export default {
  name: "InfoDialog",
  components: { infoForm },
  inject: ["parentDict"],
  data() {
    return {
      visible: false,
      loading: false,
      isEdit: false,
      dict: this.parentDict,
      form: {},
      machineList: [],
      rules: {
        factoryCode: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
        machineCode: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
        machineName: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        planDate: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
        estimatedHours: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
      },
    };
  },
  computed: {
    title() {
      return this.$t("ui.data.column.cxPrecisionPlan.modelName");
    },
    columns() {
      return [
        {
          prop: "factoryCode",
          label: this.$t("ui.data.column.cxPrecisionPlan.factoryCode"),
          type: "select",
          dictData: this.dict.type.biz_factory_name,
          filterable: true,
          listeners: {
            change: this.handleFactoryChange,
          },
        },
        {
          prop: "machineCode",
          label: this.$t("ui.data.column.cxPrecisionPlan.machineCode"),
          type: "select",
          dictData: this.machineList,
          props: {
            label: "cxMachineCode",
            value: "cxMachineCode",
          },
          filterable: true,
          listeners: {
            change: this.handleMachineChange,
          },
        },
        {
          prop: "machineName",
          label: this.$t("ui.data.column.cxPrecisionPlan.machineName"),
          disabled: true,
          maxlength: 64,
        },
        {
          prop: "planDate",
          label: this.$t("ui.data.column.cxPrecisionPlan.planDate"),
          type: "date",
          valueFormat: "yyyy-MM-dd",
        },
        {
          prop: "planShift",
          label: this.$t("ui.data.column.cxPrecisionPlan.planShift"),
          type: "select",
          dictData: this.dict.type.class_num_three_plan,
          filterable: true,
        },
        {
          prop: "planStartTime",
          label: this.$t("ui.data.column.cxPrecisionPlan.planStartTime"),
          type: "date",
          dateType: "datetime",
          valueFormat: "yyyy-MM-dd HH:mm:ss",
          listeners: {
            change: this.handlePlanStartTimeChange,
          },
        },
        {
          prop: "planEndTime",
          label: this.$t("ui.data.column.cxPrecisionPlan.planEndTime"),
          type: "date",
          dateType: "datetime",
          valueFormat: "yyyy-MM-dd HH:mm:ss",
          listeners: {
            change: this.handlePlanEndTimeChange,
          },
        },
        {
          prop: "estimatedHours",
          label: this.$t("ui.data.column.cxPrecisionPlan.estimatedHours"),
          type: "number",
          min: 0,
          precision: 1,
        },
        {
          prop: "lastPrecisionDate",
          label: this.$t("ui.data.column.cxPrecisionPlan.lastPrecisionDate"),
          type: "date",
          valueFormat: "yyyy-MM-dd",
          listeners: {
            change: this.handleLastPrecisionDateChange,
          },
        },
        {
          prop: "dueDate",
          label: this.$t("ui.data.column.cxPrecisionPlan.dueDate"),
          type: "date",
          valueFormat: "yyyy-MM-dd",
        },
        {
          prop: "remark",
          label: this.$t("ui.common.column.remark"),
          type: "textarea",
          rows: 3,
          maxlength: 300,
        },
      ];
    },
  },
  methods: {
    show(row) {
      this.visible = true;
      this.machineList = [];
      if (row) {
        this.isEdit = true;
        this.form = { ...row };
        this.getMachineList();
      } else {
        this.isEdit = false;
        this.form = {
          estimatedHours: 0,
        };
      }
    },
    hide() {
      this.visible = false;
      this.form = {};
      this.machineList = [];
      this.$refs.form && this.$refs.form.triggerResetForm();
    },
    async getMachineList() {
      if (!this.form.factoryCode) {
        this.machineList = [];
        return;
      }
      try {
        const res = await getMachineList({ factoryCode: this.form.factoryCode });
        this.machineList = res || [];
      } catch (e) {
        console.error(e);
      }
    },
    handleFactoryChange() {
      this.$set(this.form, "machineCode", "");
      this.getMachineList();
    },
    handleMachineChange(val) {
      const machine = this.machineList.find((item) => item.cxMachineCode === val);
      if (machine) {
        this.$set(this.form, "machineCode", machine.cxMachineCode);
        this.$set(this.form, "machineName", machine.cxMachineCode);
      }
    },
    handlePlanStartTimeChange(val) {
      if (this.form.planDate && val) {
        const planDate = this.form.planDate.substring(0, 10);
        const startTime = val.substring(0, 10);
        if (startTime < planDate) {
          this.$modal.msgError(this.$t("ui.data.alert.cxPrecisionPlan.startTimeBeforePlanDate"));
          this.$set(this.form, "planStartTime", "");
          return;
        }
      }
      this.calculateEstimatedHours();
    },
    handlePlanEndTimeChange(val) {
      if (this.form.planStartTime && val) {
        if (val < this.form.planStartTime) {
          this.$modal.msgError(this.$t("ui.data.alert.cxPrecisionPlan.endTimeBeforeStartTime"));
          this.$set(this.form, "planEndTime", "");
          return;
        }
      }
      this.calculateEstimatedHours();
    },
    handleLastPrecisionDateChange(val) {
      if (this.form.planDate && val) {
        if (val >= this.form.planDate) {
          this.$modal.msgError(this.$t("ui.data.alert.cxPrecisionPlan.lastPrecisionDateAfterStartTime"));
          this.$set(this.form, "lastPrecisionDate", "");
          return;
        }
      }
    },
    calculateEstimatedHours() {
      if (this.form.planStartTime && this.form.planEndTime) {
        const start = new Date(this.form.planStartTime).getTime();
        const end = new Date(this.form.planEndTime).getTime();
        const hours = (end - start) / (1000 * 60 * 60);
        this.$set(this.form, "estimatedHours", Math.round(hours * 10) / 10);
      } else {
        this.$set(this.form, "estimatedHours", 0);
      }
    },
    handleConfirm() {
      this.$refs.form.triggerConfirm(this.save);
    },
    async save(payload) {
      if (payload.planDate && payload.planStartTime) {
        const planDate = payload.planDate.substring(0, 10);
        const startTime = payload.planStartTime.substring(0, 10);
        if (startTime < planDate) {
          this.$modal.msgError(this.$t("ui.data.alert.cxPrecisionPlan.startTimeBeforePlanDate"));
          return;
        }
      }

      if (payload.planStartTime && payload.planEndTime) {
        if (payload.planEndTime < payload.planStartTime) {
          this.$modal.msgError(this.$t("ui.data.alert.cxPrecisionPlan.endTimeBeforeStartTime"));
          return;
        }
      }

      if (payload.planDate && payload.lastPrecisionDate) {
        if (payload.lastPrecisionDate >= payload.planDate) {
          this.$modal.msgError(this.$t("ui.data.alert.cxPrecisionPlan.lastPrecisionDateAfterStartTime"));
          return;
        }
      }

      if (payload.planDate && payload.dueDate) {
        if (payload.dueDate <= payload.planDate) {
          this.$modal.msgError(this.$t("ui.data.alert.cxPrecisionPlan.dueDateBeforePlanDate"));
          return;
        }
      }

      const uniqueRes = await checkCxPrecisionPlanUnique(payload);
      if (uniqueRes === "1") {
        this.$modal.msgError(this.$t("ui.data.alert.cxPrecisionPlan.notUnique"));
        return;
      }

      try {
        this.loading = true;
        const res = await saveCxPrecisionPlan(payload);
        this.$modal.msgSuccess(res.msg);
        this.$emit("success");
        this.hide();
      } finally {
        this.loading = false;
      }
    },
  },
};
</script>

<style scoped>
::v-deep .cx-precision-form .el-form-item {
  margin-bottom: 25px;
}
</style>