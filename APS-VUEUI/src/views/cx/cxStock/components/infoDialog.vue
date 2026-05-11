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
import materialCodeSelect from "./materialCodeSelect.vue";

export default {
  name: "InfoDialog",
  components: { infoForm, materialCodeSelect },
  inject: ["parentDict"],
  data() {
    return {
      visible: false,
      loading: false,
      isEdit: false,
      dict: this.parentDict,
      form: {},
      rules: {
        factoryCode: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
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
          {
            validator: (rule, value, callback) => {
              if (value === undefined || value === null || value === "") {
                callback(new Error(this.$t("common.rule.input")));
              } else if (!/^[1-9]\d*$/.test(String(value))) {
                callback(new Error("库存量必须为正整数"));
              } else {
                callback();
              }
            },
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
          render: (form) => {
            return (
              <materialCodeSelect
                key={form.embryoCode}
                v-model={form.embryoCode}
                onChange={this.handleEmbryoCodeChange}
              />
            );
          },
        },
        {
          prop: "embryoDesc",
          label: this.$t("ui.data.column.cxStock.embryoDesc"),
          disabled: true,
        },
        {
          prop: "stockNum",
          label: this.$t("ui.data.column.cxStock.stockNum"),
          type: "number",
          min: 1,
          precision: 0,
        },
        {
          prop: "dataSource",
          label: this.$t("ui.data.column.cxStock.dataSource"),
          type: "select",
          dictData: this.dict.type.lh_precision_data_source,
          filterable: true,
          disabled: true,
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
    handleEmbryoCodeChange(value, row) {
      this.$set(this.form, "embryoCode", value);
      this.$set(this.form, "embryoDesc", (row && row.embryoDesc) || "");
    },
    show(row) {
      this.visible = true;
      if (row) {
        this.isEdit = true;
        this.form = { ...row };
      } else {
        this.isEdit = false;
        this.form = {
          factoryCode: "116",
          embryoDesc: "",
          dataSource: "1",
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
