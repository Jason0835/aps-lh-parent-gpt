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
  props: {
    clothOptions: {
      type: Array,
      default: () => [],
    },
  },
  data() {
    const requiredSelect = { required: true, message: this.$t("common.rule.select"), trigger: "change" };
    const requiredInput = { required: true, message: this.$t("common.rule.input"), trigger: "blur" };
    const maxCarNumRequired = { required: true, message: this.$t("common.rule.input"), trigger: "blur" };
    const maxCarNumPositive = { validator: (rule, value, callback) => {
      if (value === null || value === undefined || value === "") { return callback(new Error(this.$t("common.rule.input"))); }
      if (!/^\d+$/.test(value) || Number(value) <= 0) { return callback(new Error("最大车数必须为大于0的整数")); }
      callback();
    }, trigger: "blur" };
    const carNumNotExceedMax = { validator: (rule, value, callback) => {
      if (value === null || value === undefined || value === "") { return callback(); }
      const max = this.form.maxCarNum;
      if (max !== null && max !== undefined && max !== "" && Number(value) > Number(max)) {
        return callback(new Error("当前车数不能大于最大车数"));
      }
      callback();
    }, trigger: "blur" };
    const emptyLaneCarNumZero = { validator: (rule, value, callback) => {
      if (value === null || value === undefined || value === "") { return callback(); }
      const materialCode = this.form.materialCode;
      if ((!materialCode || materialCode === "") && Number(value) !== 0) {
        return callback(new Error("胎体代码为空(空库排)时当前车数必须为0"));
      }
      callback();
    }, trigger: "blur" };
    return {
      loading: false, visible: false, isEdit: false, form: {},
      rules: {
        factoryCode: [requiredSelect],
        laneDate: [requiredInput],
        shiftCode: [requiredInput],
        storageLaneCode: [requiredInput],
        carNum: [requiredInput, carNumNotExceedMax, emptyLaneCarNumZero],
        maxCarNum: [maxCarNumRequired, maxCarNumPositive],
      },
    };
  },
  computed: {
    title() { return this.isEdit ? this.$t("common.button.edit") : this.$t("common.button.add"); },
    columns() {
      return [
        { prop: "factoryCode", label: this.$t("ui.data.column.cd90StorageLaneLimit.factoryCode"), type: "select", dictData: this.parentDict.type.biz_factory_name, filterable: true },
        { prop: "materialCode", label: this.$t("ui.data.column.cd90StorageLaneLimit.materialCode"), type: "select", dictData: this.clothOptions, filterable: true },
        { prop: "laneDate", label: this.$t("ui.data.column.cd90StorageLaneLimit.laneDate"), type: "date" },
        { prop: "shiftCode", label: this.$t("ui.data.column.cd90StorageLaneLimit.shiftCode"), type: "select", dictData: this.parentDict.type.class_num_three_plan, filterable: true },
        { prop: "storageLaneCode", label: this.$t("ui.data.column.cd90StorageLaneLimit.storageLaneCode"), maxlength: 50 },
        { prop: "carNum", label: this.$t("ui.data.column.cd90StorageLaneLimit.carNum"), type: "number", required: true },
        { prop: "maxCarNum", label: this.$t("ui.data.column.cd90StorageLaneLimit.maxCarNum"), type: "number", required: true },
        { prop: "remark", label: this.$t("ui.common.column.remark"), type: "textarea", rows: 3, maxlength: 900 },
      ];
    },
  },
  methods: {
    async save(params) { this.loading = true; try { const res = this.isEdit ? await updateStorageLaneLimit(params) : await addStorageLaneLimit(params); this.$modal.msgSuccess(res.msg); this.$emit("success"); this.hide(); } finally { this.loading = false; } },
    show(data) { this.visible = true; if (data) { this.isEdit = true; this.form = { ...data }; } else { this.form = { factoryCode: "116" }; } },
    hide() { this.form = {}; this.$refs.form && this.$refs.form.triggerResetForm(); this.isEdit = false; this.visible = false; },
    handleConfirm() { this.$refs.form.triggerConfirm(this.save); },
  },
};
</script>