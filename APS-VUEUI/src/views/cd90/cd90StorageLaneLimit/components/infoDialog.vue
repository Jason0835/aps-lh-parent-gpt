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
import { addStorageLaneLimit, updateStorageLaneLimit } from "@/api/cd90/storageLaneLimit";
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
        laneDate: [requiredInput],
        shiftCode: [requiredInput],
        storageLaneCode: [requiredInput],
      },
    };
  },
  computed: {
    title() { return this.isEdit ? this.$t("common.button.edit") : this.$t("common.button.add"); },
    columns() {
      return [
        { prop: "factoryCode", label: this.$t("ui.data.column.storageLaneLimit.factoryCode"), type: "select", dictData: this.parentDict.type.biz_factory_name, filterable: true },
        { prop: "laneDate", label: this.$t("ui.data.column.storageLaneLimit.laneDate"), type: "date" },
        { prop: "shiftCode", label: this.$t("ui.data.column.storageLaneLimit.shiftCode"), maxlength: 20 },
        { prop: "storageLaneCode", label: this.$t("ui.data.column.storageLaneLimit.storageLaneCode"), maxlength: 50 },
        { prop: "materialCode", label: this.$t("ui.data.column.storageLaneLimit.materialCode"), maxlength: 60 },
        { prop: "carNum", label: this.$t("ui.data.column.storageLaneLimit.carNum"), type: "number" },
        { prop: "maxCarNum", label: this.$t("ui.data.column.storageLaneLimit.maxCarNum"), type: "number" },
        { prop: "availableCarNum", label: this.$t("ui.data.column.storageLaneLimit.availableCarNum"), type: "number" },
        { prop: "dataSource", label: this.$t("ui.data.column.storageLaneLimit.dataSource"), maxlength: 20 },
        { prop: "remark", label: this.$t("ui.common.column.remark"), type: "textarea", rows: 3, maxlength: 300 },
      ];
    },
  },
  methods: {
    async save(params) { this.loading = true; try { const res = this.isEdit ? await updateStorageLaneLimit(params) : await addStorageLaneLimit(params); this.$modal.msgSuccess(res.msg); this.$emit("success"); this.hide(); } finally { this.loading = false; } },
    show(data) { this.visible = true; if (data) { this.isEdit = true; this.form = { ...data }; } else { this.form = { factoryCode: "116", maxCarNum: 7 }; } },
    hide() { this.form = {}; this.$refs.form && this.$refs.form.triggerResetForm(); this.isEdit = false; this.visible = false; },
    handleConfirm() { this.$refs.form.triggerConfirm(this.save); },
  },
};
</script>