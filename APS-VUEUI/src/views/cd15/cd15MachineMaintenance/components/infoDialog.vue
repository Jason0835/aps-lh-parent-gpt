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
        downtimeStartTime: [requiredInput],
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
        { prop: "downtimeStartTime", label: this.$t("ui.data.column.cd15MachineMaintenancePlan.downtimeStartTime"), type: "date", dateType: "datetime", valueFormat: "yyyy-MM-dd HH:mm:ss", listeners: { change: () => this.onDateTimeChange() } },
        { prop: "downtimeEndTime", label: this.$t("ui.data.column.cd15MachineMaintenancePlan.downtimeEndTime"), type: "date", dateType: "datetime", valueFormat: "yyyy-MM-dd HH:mm:ss", listeners: { change: () => this.onDateTimeChange() } },
        { prop: "downtimeHours", label: this.$t("ui.data.column.cd15MachineMaintenancePlan.downtimeHours"), type: "number", disabled: true, precision: 2 },
        { prop: "remark", label: this.$t("ui.common.column.remark"), type: "textarea", rows: 3, maxlength: 900 },
      ];
    },
  },
  methods: {
    onDateTimeChange() {
      if (this.form.downtimeStartTime && this.form.downtimeEndTime) {
        const startDateTime = moment(this.form.downtimeStartTime, "YYYY-MM-DD HH:mm:ss");
        const endDateTime = moment(this.form.downtimeEndTime, "YYYY-MM-DD HH:mm:ss");
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
    async save(params) {
      this.loading = true;
      try {
        const payload = { ...params };
        delete payload.downtimeDate;
        delete payload.downtimeEndDate;
        const res = this.isEdit ? await updateMachineMaintenancePlan(payload) : await addMachineMaintenancePlan(payload);
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
          this.form.downtimeStartTime = moment(this.form.downtimeStartTime).format("YYYY-MM-DD HH:mm:ss");
        }
        if (this.form.downtimeEndTime) {
          this.form.downtimeEndTime = moment(this.form.downtimeEndTime).format("YYYY-MM-DD HH:mm:ss");
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
