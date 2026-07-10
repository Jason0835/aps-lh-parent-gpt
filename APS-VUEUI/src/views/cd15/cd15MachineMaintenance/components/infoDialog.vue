<template>
  <el-dialog :title="title" :visible="visible" width="720px" @close="hide" :close-on-click-modal="false" :close-on-press-escape="false" :append-to-body="true">
    <info-form class="form-item-height" ref="form" :form="form" :rules="rules" :columns="columns" label-position="right" label-width="130px" v-loading="loading" />
    <template slot="footer">
      <el-button @click="hide">{{ $t("common.button.cancel") }}</el-button>
      <el-button type="primary" :loading="loading" @click="handleConfirm">{{ $t("common.button.confirm") }}</el-button>
    </template>
  </el-dialog>
</template>

<script>
import { addMachineMaintenancePlan, updateMachineMaintenancePlan } from "@/api/cd15/machineMaintenancePlan";
import { getCd15MachineEnableOptions } from "@/api/cd15/cd15MachineInfo";
import infoForm from "@/views/components/infoForm.vue";
import moment from "moment";

export default {
  components: { infoForm },
  inject: ["parentDict"],
  data() {
    const requiredSelect = { required: true, message: this.$t("common.rule.select"), trigger: "change" };
    const requiredInput = { required: true, message: this.$t("common.rule.input"), trigger: "blur" };
    return {
      loading: false, visible: false, isEdit: false, form: {}, machineOptions: [],
      rules: {
        factoryCode: [requiredSelect],
        machineCode: [requiredSelect],
        downtimeDate: [requiredInput],
        downtimeStartTime: [requiredInput],
        downtimeEndDate: [requiredInput],
        downtimeEndTime: [requiredInput],
      },
    };
  },
  computed: {
    title() { return this.isEdit ? this.$t("common.button.edit") : this.$t("common.button.add"); },
    columns() {
      return [
        { prop: "factoryCode", label: this.$t("ui.data.column.cd15MachineMaintenancePlan.factoryCode"), type: "select", dictData: this.parentDict.type.biz_factory_name, filterable: true, change: () => this.loadMachineOptions() },
        { prop: "machineCode", label: this.$t("ui.data.column.cd15MachineMaintenancePlan.machineCode"), type: "select", dictData: this.machineOptions, filterable: true },
        { prop: "downtimeDate", label: this.$t("ui.data.column.cd15MachineMaintenancePlan.downtimeDate"), type: "date", valueFormat: "yyyy-MM-dd", change: () => this.onDateChange() },
        { prop: "downtimeStartTime", label: this.$t("ui.data.column.cd15MachineMaintenancePlan.downtimeStartTime"), type: "time", valueFormat: "HH:mm", change: () => this.onTimeChange() },
        { prop: "downtimeEndDate", label: this.$t("ui.data.column.cd15MachineMaintenancePlan.downtimeEndDate"), type: "date", valueFormat: "yyyy-MM-dd", change: () => this.onDateChange() },
        { prop: "downtimeEndTime", label: this.$t("ui.data.column.cd15MachineMaintenancePlan.downtimeEndTime"), type: "time", valueFormat: "HH:mm", change: () => this.onTimeChange() },
        { prop: "downtimeHours", label: this.$t("ui.data.column.cd15MachineMaintenancePlan.downtimeHours"), type: "number", disabled: true, precision: 2 },
        { prop: "remark", label: this.$t("ui.common.column.remark"), type: "textarea", rows: 3, maxlength: 900 },
      ];
    },
  },
  methods: {
    onTimeChange() {
      if (this.form.downtimeStartTime && !this.form.downtimeDate) {
        const today = moment().format("YYYY-MM-DD");
        this.$set(this.form, "downtimeDate", today);
      }
      if (this.form.downtimeEndTime && !this.form.downtimeEndDate) {
        this.$set(this.form, "downtimeEndDate", this.form.downtimeDate || moment().format("YYYY-MM-DD"));
      }
      if (this.form.downtimeDate && this.form.downtimeStartTime && this.form.downtimeEndDate && this.form.downtimeEndTime) {
        const startDateTime = moment(this.form.downtimeDate + " " + this.form.downtimeStartTime, "YYYY-MM-DD HH:mm");
        const endDateTime = moment(this.form.downtimeEndDate + " " + this.form.downtimeEndTime, "YYYY-MM-DD HH:mm");
        const diffMinutes = endDateTime.diff(startDateTime, "minutes");
        if (diffMinutes > 0) {
          this.$set(this.form, "downtimeHours", Number((diffMinutes / 60).toFixed(2)));
        } else {
          this.$set(this.form, "downtimeHours", null);
        }
      } else {
        this.$set(this.form, "downtimeHours", null);
      }
    },
    onDateChange() {
      if (this.form.downtimeStartTime && !this.form.downtimeDate) {
        const today = moment().format("YYYY-MM-DD");
        this.$set(this.form, "downtimeDate", today);
      }
      if (this.form.downtimeEndTime && !this.form.downtimeEndDate) {
        this.$set(this.form, "downtimeEndDate", this.form.downtimeDate || moment().format("YYYY-MM-DD"));
      }
      if (!this.form.downtimeEndDate && this.form.downtimeDate) {
        this.$set(this.form, "downtimeEndDate", this.form.downtimeDate);
      }
      this.onTimeChange();
    },
    async save(params) {
      this.loading = true;
      try {
        const downtimeDate = params.downtimeDate || moment().format("YYYY-MM-DD");
        const downtimeEndDate = params.downtimeEndDate || downtimeDate;
        if (params.downtimeStartTime) {
          params.downtimeStartTime = moment(downtimeDate).format("YYYY-MM-DD") + " " + params.downtimeStartTime + ":00";
        }
        if (params.downtimeEndTime) {
          params.downtimeEndTime = moment(downtimeEndDate).format("YYYY-MM-DD") + " " + params.downtimeEndTime + ":00";
        }
        const res = this.isEdit ? await updateMachineMaintenancePlan(params) : await addMachineMaintenancePlan(params);
        this.$modal.msgSuccess(res.msg);
        this.$emit("success");
        this.hide();
      } finally {
        this.loading = false;
      }
    },
    async loadMachineOptions() {
      const res = await getCd15MachineEnableOptions({ factoryCode: this.form.factoryCode });
      const rows = Array.isArray(res) ? res : (res.rows || res.data || []);
      this.machineOptions = rows.map((item) => ({ label: item.machineCode, value: item.machineCode }));
    },
    show(data) {
      this.visible = true;
      if (data) {
        this.isEdit = true;
        this.form = { ...data };
        if (this.form.downtimeStartTime) {
          const dt = moment(this.form.downtimeStartTime);
          this.form.downtimeStartTime = dt.format("HH:mm");
          this.form.downtimeDate = dt.format("YYYY-MM-DD");
        }
        if (this.form.downtimeEndTime) {
          const dt = moment(this.form.downtimeEndTime);
          this.form.downtimeEndTime = dt.format("HH:mm");
          this.form.downtimeEndDate = dt.format("YYYY-MM-DD");
        }
      } else {
        this.form = { factoryCode: "116" };
      }
      this.loadMachineOptions();
    },
    hide() { this.form = {}; this.$refs.form && this.$refs.form.triggerResetForm(); this.isEdit = false; this.visible = false; },
    handleConfirm() { this.$refs.form.triggerConfirm(this.save); },
  },
};
</script>