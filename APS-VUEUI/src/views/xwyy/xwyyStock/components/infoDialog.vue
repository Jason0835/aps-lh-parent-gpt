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
import { addStock, updateStock } from "@/api/xwyy/xwyyStock";
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
        stockDate: [requiredInput],
        bigRollCode: [requiredInput],
        stockNum: [requiredInput, { validator: (rule, value, callback) => { if (value === undefined || value === null || value === "") { callback(new Error(this.$t("common.rule.input"))); } else { callback(); } }, trigger: "blur" }],
      },
    };
  },
  computed: {
    title() { return this.isEdit ? this.$t("common.button.edit") : this.$t("common.button.add"); },
    columns() {
      return [
        { prop: "factoryCode", label: this.$t("ui.data.column.xwyyStock.factoryCode"), type: "select", dictData: this.parentDict.type.biz_factory_name, filterable: true },
        { prop: "stockDate", label: this.$t("ui.data.column.xwyyStock.stockDate"), type: "date" },
        { prop: "bigRollCode", label: this.$t("ui.data.column.xwyyStock.bigRollCode"), type: "input" },
        { prop: "bigRollBarcode", label: this.$t("ui.data.column.xwyyStock.bigRollBarcode"), type: "input" },
        { prop: "stockNum", label: this.$t("ui.data.column.xwyyStock.stockNum"), type: "number" },
        { prop: "stockRollNum", label: this.$t("ui.data.column.xwyyStock.stockRollNum"), type: "number" },
        { prop: "modifyNum", label: this.$t("ui.data.column.xwyyStock.modifyNum"), type: "number" },
        { prop: "rollModifyNum", label: this.$t("ui.data.column.xwyyStock.rollModifyNum"), type: "number" },
        { prop: "badNum", label: this.$t("ui.data.column.xwyyStock.badNum"), type: "number" },
        { prop: "rollBadNum", label: this.$t("ui.data.column.xwyyStock.rollBadNum"), type: "number" },
        { prop: "remark", label: this.$t("ui.common.column.remark"), type: "textarea", rows: 3, maxlength: 900 },
      ];
    },
  },
  methods: {
    async save(params) { this.loading = true; try { const res = this.isEdit ? await updateStock(params) : await addStock(params); this.$modal.msgSuccess(res.msg); this.$emit("success"); this.hide(); } finally { this.loading = false; } },
    show(data) { this.visible = true; if (data) { this.isEdit = true; this.form = { ...data }; } else { this.form = { factoryCode: "116" }; } },
    hide() { this.form = {}; this.$refs.form && this.$refs.form.triggerResetForm(); this.isEdit = false; this.visible = false; },
    handleConfirm() { this.$refs.form.triggerConfirm(this.save); },
  },
};
</script>