<template>
  <el-dialog
    :title="title"
    :visible="visible"
    width="800px"
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
import infoForm from "@/views/components/infoForm.vue";

import { addStock, editStock } from "@/api/tq/stock";

export default {
  components: { infoForm },
  data() {
    return {
      loading: false,
      visible: false,
      isEdit: false,
      editType: null,
      form: {},
      rules: {
        stockDate: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "blur",
          },
        ],
        materialCode: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        stockNum: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
      },
    };
  },
  computed: {
    title: function () {
      return (
        (this.isEdit
          ? this.$t("common.button.edit")
          : this.$t("common.button.add")) + this.$t("ui.frame.page.stock.title")
      );
    },
    columns() {
      return [
        {
          label: this.$t("ui.data.column.stock.stockDate"),
          prop: "stockDate",
          span: 24,
          required: true,
          type: "date",
          valueFormat: "yyyy-MM-dd",
          disabled: this.editType === "2",
        },
        {
          label: this.$t("ui.data.column.tq.scheduleResult.beadCode"),
          prop: "materialCode",
          span: 24,
          required: true,
          maxlength: "50",
          disabled: this.editType === "2",
        },
        {
          label: this.$t("ui.data.column.stock.stockNum"),
          prop: "stockNum",
          span: 24,
          required: true,
          disabled: this.editType === "1",
          type: "number",
          min: 0,
          max: 999999,
          precision: 2,
        },
        {
          label: this.$t("ui.data.column.stock.modifyNum"),
          prop: "modifyNum",
          span: 24,
          type: "number",
          min: -999999,
          max: 999999,
          precision: 2,
        },
        {
          label: this.$t("ui.data.column.stock.badNum"),
          prop: "badNum",
          span: 24,
          type: "number",
          min: 0,
          max: 999999,
          precision: 2,
        },
        {
          label: this.$t("ui.common.column.remark"),
          prop: "remark",
          span: 24,
          type: "textarea",
          maxlength: "300",
        },
      ];
    },
  },
  methods: {
    async save(params) {
      try {
        this.loading = true;

        let res;
        if (this.editType === "0") {
          res = await addStock(params);
        } else {
          res = await editStock(params);
        }
        this.$modal.msgSuccess(res.msg);
        this.$emit("success");
        this.hide();

        this.loading = false;
      } catch (error) {
        console.log(error);
        this.loading = false;
      }
    },

    show(data, editType) {
      this.visible = true;
      this.editType = editType;
      if (data) {
        this.isEdit = true;
        this.form = {
          ...data,
        };
      }
    },
    hide() {
      this.form = {};
      this.$refs.form.triggerResetForm();
      this.isEdit = false;
      this.visible = false;
      this.editType = null;
    },
    handleConfirm() {
      this.$refs.form.triggerConfirm((params) => {
        let num =
          Number(params.stockNum || 0) +
          Number(params.modifyNum || 0) -
          Number(params.badNum || 0);
        if (num < 0) {
          this.$modal.msgError(
            this.$t("ui.data.column.stock.stockNumValidate")
          );
          return;
        }

        this.save({
          ...params,
          editType: this.editType,
        });
      });
    },
  },
};
</script>
