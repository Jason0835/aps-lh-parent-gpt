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
import { addMachineMaintenancePlan, updateMachineMaintenancePlan } from "@/api/cd90/machineMaintenancePlan";
import { getCd90MachineEnableOptions } from "@/api/cd90/cd90MachineInfo";
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
        machineCode: [requiredInput],
        downtimeStartTime: [requiredInput],
        downtimeEndTime: [requiredInput],
      },
    };
  },
  computed: {
    title() { return this.isEdit ? this.$t("common.button.edit") : this.$t("common.button.add"); },
    columns() {
      return [
        { prop: "factoryCode", label: this.$t("ui.data.column.cd90MachineMaintenancePlan.factoryCode"), type: "select", dictData: this.parentDict.type.biz_factory_name, filterable: true, change: () => this.loadMachineOptions() },
        { prop: "machineCode", label: this.$t("ui.data.column.cd90MachineMaintenancePlan.machineCode"), type: "select", dictData: this.machineOptions, filterable: true },
        { prop: "downtimeDate", label: this.$t("ui.data.column.cd90MachineMaintenancePlan.downtimeDate"), type: "date", disabled: true },
        { prop: "downtimeStartTime", label: this.$t("ui.data.column.cd90MachineMaintenancePlan.downtimeStartTime"), type: "time", valueFormat: "HH:mm", change: () => this.onTimeChange() },
        { prop: "downtimeEndTime", label: this.$t("ui.data.column.cd90MachineMaintenancePlan.downtimeEndTime"), type: "time", valueFormat: "HH:mm", change: () => this.onTimeChange() },
        { prop: "downtimeHours", label: this.$t("ui.data.column.cd90MachineMaintenancePlan.downtimeHours"), type: "number", disabled: true, precision: 2 },
        { prop: "remark", label: this.$t("ui.common.column.remark"), type: "textarea", rows: 3, maxlength: 900 },
      ];
    },
  },
  methods: {
    onTimeChange() {
      if (this.form.downtimeStartTime) {
        const today = moment().format("YYYY-MM-DD");
        this.$set(this.form, "downtimeDate", today);
      }
      if (this.form.downtimeStartTime && this.form.downtimeEndTime) {
        const startParts = this.form.downtimeStartTime.split(":");
        const endParts = this.form.downtimeEndTime.split(":");
        const startMinutes = parseInt(startParts[0]) * 60 + parseInt(startParts[1]);
        const endMinutes = parseInt(endParts[0]) * 60 + parseInt(endParts[1]);
        const diffMinutes = endMinutes - startMinutes;
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
      this.$set(this.form, "downtimeStartTime", null);
      this.$set(this.form, "downtimeEndTime", null);
      this.$set(this.form, "downtimeHours", null);
    },
    async save(params) {
      this.loading = true;
      try {
        if (params.downtimeDate && params.downtimeStartTime) {
          params.downtimeStartTime = moment(params.downtimeDate).format("YYYY-MM-DD") + " " + params.downtimeStartTime + ":00";
        }
        if (params.downtimeDate && params.downtimeEndTime) {
          params.downtimeEndTime = moment(params.downtimeDate).format("YYYY-MM-DD") + " " + params.downtimeEndTime + ":00";
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
      const res = await getCd90MachineEnableOptions({ factoryCode: this.form.factoryCode });
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