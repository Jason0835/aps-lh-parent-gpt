<template>
  <el-dialog
    :title="dialogTitle"
    :visible="visible"
    width="720px"
    @close="hide"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :append-to-body="true"
  >
    <info-form
      class="form-item-height"
      ref="form"
      :form="form"
      :rules="rules"
      :columns="columns"
      label-position="right"
      label-width="160px"
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
import { saveCxStructureTreadConfig } from "@/api/cx/cxStructureTreadConfig";
import structureSelectWithDesc from "@/views/components/structureSelectWithDesc.vue";
import infoForm from "@/views/components/infoForm.vue";

export default {
  components: { infoForm, structureSelectWithDesc },
  inject: ["parentDict"],
  data() {
    const validateInteger = (rule, value, callback) => {
      if (value === undefined || value === null || value === "") {
        callback();
        return;
      }
      if (!Number.isInteger(Number(value))) {
        callback(new Error("请输入整数"));
        return;
      }
      callback();
    };
    return {
      loading: false,
      visible: false,
      isEdit: false,
      form: {},
      rules: {
        stockDate: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "blur",
          },
        ],
        structureCode: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        treadCount: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
          {
            validator: validateInteger,
            trigger: ["blur", "change"],
          },
        ],
        factoryCode: [
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
    dialogTitle() {
      return this.isEdit ? this.$t("common.button.edit") : this.$t("common.button.add");
    },
    columns() {
      return [
        {
          label: this.$t("ui.data.column.factoryCode"),
          prop: "factoryCode",
          span: 24,
          type: "select",
          dictData: this.parentDict.type.biz_factory_name,
          filterable: true,
          required: true,
        },
        {
          label: this.$t("ui.data.column.cxStructureTreadConfig.structureCode"),
          prop: "structureCode",
          span: 24,
          required: true,
          maxlength: 50,
          render: (form) => {
            return (
              <structureSelectWithDesc
                key={`${form.factoryCode || ""}-${form.structureCode || ""}`}
                factoryCode={form.factoryCode}
                multiple={false}
                v-model={form.structureCode}
                onChange={(value, row) => this.handleStructureChange(form, value, row)}
                onClear={() => this.handleStructureClear(form)}
              />
            );
          },
        },
        {
          label: this.$t("ui.data.column.cxStructureTreadConfig.embryoCode"),
          prop: "embryoCode",
          span: 24,
          disabled: true,
          maxlength: 20,
        },
        {
          label: this.$t("ui.data.column.cxStructureTreadConfig.mainMaterialDesc"),
          prop: "mainMaterialDesc",
          span: 24,
          disabled: true,
          maxlength: 64,
        },
        {
          label: this.$t("ui.data.column.cxStructureTreadConfig.treadCount"),
          prop: "treadCount",
          span: 24,
          type: "number",
          min: 1,
          max: 999,
          precision: 0,
          required: true,
        },
        {
          label: this.$t("ui.common.column.remark"),
          prop: "remark",
          span: 24,
          type: "textarea",
          maxlength: 300,
        },
      ];
    },
  },
  methods: {
    show(data) {
      this.visible = true;
      if (data) {
        this.isEdit = true;
        this.form = { ...data };
      } else {
        this.form = {
          factoryCode: "116",
        };
      }
    },
    hide() {
      this.form = {};
      this.$refs.form && this.$refs.form.triggerResetForm();
      this.isEdit = false;
      this.visible = false;
    },
    handleConfirm() {
      this.$refs.form.triggerConfirm(this.save);
    },
    handleStructureChange(form, value, row) {
      this.$set(form, "structureCode", value);
      this.$set(form, "embryoCode", row ? row.embryoCode : undefined);
      this.$set(form, "mainMaterialDesc", row ? row.mainMaterialDesc : undefined);
    },
    handleStructureClear(form) {
      this.$set(form, "structureCode", undefined);
      this.$set(form, "embryoCode", undefined);
      this.$set(form, "mainMaterialDesc", undefined);
    },
    async save(payload) {
      try {
        this.loading = true;
        const res = await saveCxStructureTreadConfig(payload);
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
</style>
