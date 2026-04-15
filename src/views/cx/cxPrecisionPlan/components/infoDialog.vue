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
        planDate: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
        actualDate: [
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
    title() {
      return this.isEdit
        ? this.$t("common.button.edit")
        : this.$t("common.button.add");
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
          prop: "planDate",
          label: this.$t("ui.data.column.cxPrecisionPlan.planDate"),
          type: "date",
          valueFormat: "yyyy-MM-dd",
          listeners: {
            change: this.handlePlanDateChange,
          },
        },
        {
          prop: "actualDate",
          label: this.$t("ui.data.column.cxPrecisionPlan.actualDate"),
          type: "date",
          valueFormat: "yyyy-MM-dd",
        },
        {
          prop: "precisionTypeLabel",
          label: this.$t("ui.data.column.cxPrecisionPlan.precisionType"),
          disabled: true,
        },
        {
          prop: "cycleDays",
          label: this.$t("ui.data.column.cxPrecisionPlan.cycleDays"),
          disabled: true,
        },
        {
          prop: "daysToDue",
          label: this.$t("ui.data.column.cxPrecisionPlan.daysToDue"),
          disabled: true,
        },
        {
          prop: "dataSourceLabel",
          label: this.$t("ui.data.column.cxPrecisionPlan.dataSource"),
          disabled: true,
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
        this.refreshDerivedFields();
      } else {
        this.isEdit = false;
        this.form = {
          factoryCode: "116",
        };
        this.getMachineList();
        this.refreshDerivedFields();
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
      }
    },
    handlePlanDateChange() {
      this.refreshDerivedFields();
    },
    refreshDerivedFields() {
      // precisionType is read-only, provided by backend
      const pt = this.form.precisionType;
      this.$set(this.form, "precisionTypeLabel", this.getPrecisionTypeLabel(pt));
      this.$set(this.form, "cycleDays", this.getCycleDaysByPrecisionType(pt));
      this.$set(this.form, "dataSourceLabel", this.getDataSourceLabel(this.form.dataSource));
      this.$set(this.form, "daysToDue", this.calcDaysToDue(this.form.planDate));
    },
    getPrecisionTypeLabel(precisionType) {
      const list = (this.dict && this.dict.type && this.dict.type.MACHINE_ACCURACY_TYPE) || [];
      const item =
        list.find((d) => d.value === precisionType) ||
        list.find((d) => d.dictValue === precisionType) ||
        list.find((d) => String(d.value) === String(precisionType)) ||
        list.find((d) => String(d.dictValue) === String(precisionType));
      return (item && (item.label || item.dictLabel)) || "";
    },
    getCycleDaysByPrecisionType(precisionType) {
      const label = this.getPrecisionTypeLabel(precisionType);
      if (label.includes("15")) return "15";
      if (label.includes("60")) return "60";
      return "";
    },
    getDataSourceLabel(dataSource) {
      if (dataSource === "0" || dataSource === 0) return "MES";
      if (dataSource === "1" || dataSource === 1) return "系统";
      return "";
    },
    calcDaysToDue(planDate) {
      if (!planDate) return "";
      try {
        // planDate is yyyy-MM-dd
        const today = new Date();
        const t0 = new Date(today.getFullYear(), today.getMonth(), today.getDate()).getTime();
        const parts = String(planDate).split("-");
        if (parts.length < 3) return "";
        const pd = new Date(Number(parts[0]), Number(parts[1]) - 1, Number(parts[2])).getTime();
        return Math.floor((pd - t0) / (24 * 60 * 60 * 1000));
      } catch (e) {
        return "";
      }
    },
    handleConfirm() {
      this.$refs.form.triggerConfirm(this.save);
    },
    async save(payload) {
      // Derived display-only fields must not be sent to backend (backend entity doesn't have them)
      delete payload.precisionTypeLabel;
      delete payload.dataSourceLabel;
      delete payload.cycleDays;

      // Ensure daysToDue is up-to-date (computed from planDate)
      payload.daysToDue = this.calcDaysToDue(payload.planDate);

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
