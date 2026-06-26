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
import { addShiftConfig, updateShiftConfig } from "@/api/xwyy/xwyyShiftConfig";
import infoForm from "@/views/components/infoForm.vue";

export default {
  components: { infoForm },
  inject: ["parentDict"],
  data() {
    const requiredSelect = { required: true, message: this.$t("common.rule.select"), trigger: "change" };
    const requiredInput = { required: true, message: this.$t("common.rule.input"), trigger: "blur" };
    return {
      loading: false, visible: false, isEdit: false, form: {},
      rules: {
        factoryCode: [requiredSelect],
        shiftCode: [requiredInput],
        startTime: [requiredInput],
        endTime: [requiredInput],
        shiftHours: [requiredInput],
        isActive: [requiredSelect],
      },
    };
  },
  computed: {
    title() { return this.isEdit ? this.$t("common.button.edit") : this.$t("common.button.add"); },
    columns() {
      return [
        { prop: "factoryCode", label: this.$t("ui.data.column.xwyyShiftConfig.factoryCode"), type: "select", dictData: this.parentDict.type.biz_factory_name, filterable: true },
        { prop: "shiftCode", label: this.$t("ui.data.column.xwyyShiftConfig.shiftCode"), type: "input" },
        { prop: "shiftName", label: this.$t("ui.data.column.xwyyShiftConfig.shiftName"), type: "input" },
        { prop: "shiftOrder", label: this.$t("ui.data.column.xwyyShiftConfig.shiftOrder"), type: "number" },
        { prop: "startTime", label: this.$t("ui.data.column.xwyyShiftConfig.startTime"), type: "input" },
        { prop: "endTime", label: this.$t("ui.data.column.xwyyShiftConfig.endTime"), type: "input" },
        { prop: "shiftHours", label: this.$t("ui.data.column.xwyyShiftConfig.shiftHours"), type: "number" },
        { prop: "isCrossDay", label: this.$t("ui.data.column.xwyyShiftConfig.isCrossDay"), type: "select", dictData: this.parentDict.type.biz_yes_no },
        { prop: "scheduleDay", label: this.$t("ui.data.column.xwyyShiftConfig.scheduleDay"), type: "number" },
        { prop: "dayShiftOrder", label: this.$t("ui.data.column.xwyyShiftConfig.dayShiftOrder"), type: "number" },
        { prop: "classField", label: this.$t("ui.data.column.xwyyShiftConfig.classField"), type: "input" },
        { prop: "isActive", label: this.$t("ui.data.column.xwyyShiftConfig.isActive"), type: "select", dictData: this.parentDict.type.sys_enable_disable },
        { prop: "remark", label: this.$t("ui.common.column.remark"), type: "textarea", rows: 3, maxlength: 500 },
      ];
    },
  },
  methods: {
    async save(params) { this.loading = true; try { const res = this.isEdit ? await updateShiftConfig(params) : await addShiftConfig(params); this.$modal.msgSuccess(res.msg); this.$emit("success"); this.hide(); } finally { this.loading = false; } },
    show(data) { this.visible = true; if (data) { this.isEdit = true; this.form = { ...data }; } else { this.form = { factoryCode: "116", isActive: 1 }; } },
    hide() { this.form = {}; this.$refs.form && this.$refs.form.triggerResetForm(); this.isEdit = false; this.visible = false; },
    handleConfirm() { this.$refs.form.triggerConfirm(this.save); },
  },
};
</script>
