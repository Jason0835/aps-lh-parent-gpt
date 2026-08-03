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
import { addDepthConfig, updateDepthConfig } from "@/api/cd90/depthConfig";
import infoForm from "@/views/components/infoForm.vue";

export default {
  components: { infoForm },
  inject: ["parentDict"],
  data() {
    const requiredSelect = { required: true, message: this.$t("common.rule.select"), trigger: "change" };
    const requiredInput = { required: true, message: this.$t("common.rule.input"), trigger: "blur" };
    const positiveInteger = (rule, value, callback) => {
      if (value === "" || value === null || value === undefined) {
        callback();
        return;
      }
      const numberValue = Number(value);
      if (!Number.isInteger(numberValue) || numberValue <= 0) {
        callback(new Error(this.$t("ui.data.column.cd90DepthConfig.invalidRange")));
        return;
      }
      callback();
    };
    const validMax = (rule, value, callback) => {
      if (value === "" || value === null || value === undefined) {
        callback();
        return;
      }
      const numberValue = Number(value);
      if (!Number.isInteger(numberValue) || numberValue <= 0
        || numberValue < Number(this.form.minMachineQty)) {
        callback(new Error(this.$t("ui.data.column.cd90DepthConfig.invalidRange")));
        return;
      }
      callback();
    };
    const positiveDepth = (rule, value, callback) => {
      const numberValue = Number(value);
      if (value === "" || value === null || value === undefined
        || !Number.isFinite(numberValue) || numberValue <= 0
        || Number(numberValue.toFixed(2)) !== numberValue) {
        callback(new Error(this.$t("ui.data.column.cd90DepthConfig.invalidRange")));
        return;
      }
      callback();
    };
    return {
      loading: false, visible: false, isEdit: false, form: {},
      rules: {
        factoryCode: [requiredSelect],
        minMachineQty: [requiredInput, { validator: positiveInteger, trigger: "blur" }],
        maxMachineQty: [{ validator: validMax, trigger: "blur" }],
        depthClassQty: [requiredInput, { validator: positiveDepth, trigger: "blur" }],
      },
    };
  },
  computed: {
    title() { return this.isEdit ? this.$t("common.button.edit") : this.$t("common.button.add"); },
    columns() {
      return [
        { prop: "factoryCode", label: this.$t("ui.data.column.cd90DepthConfig.factoryCode"), type: "select", dictData: this.parentDict.type.biz_factory_name, filterable: true },
        { prop: "minMachineQty", label: this.$t("ui.data.column.cd90DepthConfig.minMachineQty"), type: "number", min: 1, required: true },
        { prop: "maxMachineQty", label: this.$t("ui.data.column.cd90DepthConfig.maxMachineQty"), type: "input" },
        { prop: "depthClassQty", label: this.$t("ui.data.column.cd90DepthConfig.depthClassQty"), type: "number", required: true, precision: 2 },
        { prop: "remark", label: this.$t("ui.common.column.remark"), type: "textarea", rows: 3, maxlength: 900 },
      ];
    },
  },
  methods: {
    async save(params) {
      this.loading = true;
      try {
        const payload = { ...params, maxMachineQty: params.maxMachineQty === "" ? null : params.maxMachineQty };
        const res = this.isEdit ? await updateDepthConfig(payload) : await addDepthConfig(payload);
        this.$modal.msgSuccess(res.msg);
        this.$emit("success");
        this.hide();
      } finally {
        this.loading = false;
      }
    },
    show(data, defaultFactoryCode) {
      this.visible = true;
      if (data) {
        this.isEdit = true;
        this.form = { ...data, maxMachineQty: data.maxMachineQty == null ? "" : data.maxMachineQty };
      } else {
        this.form = { factoryCode: defaultFactoryCode || "116" };
      }
    },
    hide() { this.form = {}; this.$refs.form && this.$refs.form.triggerResetForm(); this.isEdit = false; this.visible = false; },
    handleConfirm() { this.$refs.form.triggerConfirm(this.save); },
  },
};
</script>
