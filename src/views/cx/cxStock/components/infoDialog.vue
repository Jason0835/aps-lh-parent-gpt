<template>
  <el-dialog
    :title="title"
    :visible="visible"
    width="600px"
    @close="hide"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :append-to-body="true"
  >
    <info-form
      class="form-item-height cx-stock-form"
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
import { saveCxStock } from "@/api/cx/cxStock";

import infoForm from "@/views/components/infoForm.vue";

export default {
  name: "InfoDialog",
  components: { infoForm },
  inject: ["parentDict"],
  data() {
    return {
      visible: false,
      loading: false,
      isEdit: false,
      dict: this.parentDict,
      form: {},
      rules: {
        stockDate: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
        embryoCode: [
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
    title() {
      return this.isEdit ? this.$t("common.button.edit") : this.$t("common.button.add");
    },
    columns() {
      return [
        {
          prop: "factoryCode",
          label: this.$t("ui.data.column.cxStock.factoryCode"),
          type: "select",
          dictData: this.dict.type.biz_factory_name,
          filterable: true,
        },
        {
          prop: "stockDate",
          label: this.$t("ui.data.column.cxStock.stockDate"),
          type: "date",
          valueFormat: "yyyy-MM-dd",
        },
        {
          prop: "embryoCode",
          label: this.$t("ui.data.column.cxStock.embryoCode"),
          maxlength: 64,
        },
        {
          prop: "stockNum",
          label: this.$t("ui.data.column.cxStock.stockNum"),
          type: "number",
          min: 0,
          precision: 2,
        },
        {
          prop: "overTimeStock",
          label: this.$t("ui.data.column.cxStock.overTimeStock"),
          type: "number",
          min: 0,
          precision: 2,
        },
        {
          prop: "modifyNum",
          label: this.$t("ui.data.column.cxStock.modifyNum"),
          type: "number",
          min: -999999,
          precision: 2,
        },
        {
          prop: "badNum",
          label: this.$t("ui.data.column.cxStock.badNum"),
          type: "number",
          min: 0,
          precision: 2,
        },
        {
          prop: "isEndingSku",
          label: this.$t("ui.data.column.cxStock.isEndingSku"),
          type: "select",
          dictData: this.dict.type.biz_yes_no,
          filterable: true,
        },
        {
          prop: "remark",
          label: this.$t("ui.common.column.remark"),
          type: "textarea",
          rows: 3,
          maxlength: 300,
        },
      ];
    },
  },
  methods: {
    show(row) {
      this.visible = true;
      if (row) {
        this.isEdit = true;
        this.form = { ...row };
      } else {
        this.isEdit = false;
        this.form = {
          isEndingSku: "0",
        };
      }
    },
    hide() {
      this.visible = false;
      this.form = {};
      this.$refs.form && this.$refs.form.triggerResetForm();
    },
    handleConfirm() {
      this.$refs.form.triggerConfirm(this.save);
    },
    async save(payload) {
      try {
        this.loading = true;
        const res = await saveCxStock(payload);
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
::v-deep .cx-stock-form .el-form-item {
  margin-bottom: 25px;
}
</style>
