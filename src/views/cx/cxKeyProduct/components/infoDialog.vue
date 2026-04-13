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
import { saveCxKeyProduct, getCxKeyProduct, checkUniqueCxKeyProduct } from "@/api/cx/keyProduct";
import infoForm from "@/views/components/infoForm.vue";

export default {
  components: { infoForm },
  inject: ["parentDict"],
  data() {
    const validateEmbryoCode = async (rule, value, callback) => {
      if (!value) {
        callback(new Error(this.$t("common.rule.input")));
        return;
      }
      try {
        const res = await checkUniqueCxKeyProduct(this.form);
        if (res === "1") {
          callback(new Error(this.$t("ui.data.alert.cxKeyProduct.embryoCodeNotUnique")));
        } else {
          callback();
        }
      } catch (e) {
        callback();
      }
    };
    
    return {
      loading: false,
      visible: false,
      isEdit: false,
      form: {},
      rules: {
        embryoCode: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
          { validator: validateEmbryoCode, trigger: "blur" }
        ],
        isActive: [
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
      return (
        (this.isEdit ? this.$t("common.button.edit") : this.$t("common.button.add")) +
        this.$t("ui.data.column.cxKeyProduct.modelName")
      );
    },
    columns() {
      return [
        {
          label: this.$t("ui.data.column.cxKeyProduct.embryoCode"),
          prop: "embryoCode",
          span: 24,
          required: true,
          maxlength: 100,
          disabled: this.isEdit,
        },
        {
          label: this.$t("ui.data.column.cxKeyProduct.embryoDesc"),
          prop: "embryoDesc",
          span: 24,
          maxlength: 200,
        },
        {
          label: this.$t("ui.data.column.cxKeyProduct.structureName"),
          prop: "structureName",
          span: 24,
          maxlength: 100,
        },
        {
          label: this.$t("ui.data.column.cxKeyProduct.isActive"),
          prop: "isActive",
          span: 24,
          type: "select",
          dictData: this.parentDict.type.biz_yes_no,
          required: true,
        },
        {
          label: this.$t("ui.common.column.remark"),
          prop: "remark",
          span: 24,
          type: "textarea",
          maxlength: 500,
        },
      ];
    },
  },
  methods: {
    async show(data) {
      this.visible = true;
      if (data) {
        this.isEdit = true;
        try {
          this.loading = true;
          const res = await getCxKeyProduct(data.id);
          this.form = { ...res };
        } finally {
          this.loading = false;
        }
      } else {
        this.form = { isActive: 1 };
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
    async save(payload) {
      try {
        this.loading = true;
        const res = await saveCxKeyProduct(payload);
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
