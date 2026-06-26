<template>
  <el-dialog
    :title="title"
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
      label-width="130px"
      v-loading="loading"
    />
    <template slot="footer">
      <el-button @click="hide">{{ $t("common.button.cancel") }}</el-button>
      <el-button type="primary" :loading="loading" @click="handleConfirm">{{ $t("common.button.confirm") }}</el-button>
    </template>
  </el-dialog>
</template>

<script>
import { addGdyyStock, updateGdyyStock } from "@/api/gdyy/gdyyStock";
import infoForm from "@/views/components/infoForm.vue";

export default {
  components: { infoForm },
  inject: ["parentDict"],
  data() {
    const requiredSelect = {
      required: true,
      message: this.$t("common.rule.select"),
      trigger: "change",
    };
    const requiredInput = {
      required: true,
      message: this.$t("common.rule.input"),
      trigger: "blur",
    };
    return {
      loading: false,
      visible: false,
      isEdit: false,
      form: {},
      rules: {
        factoryCode: [requiredSelect],
        stockDate: [requiredSelect],
        bigRollCode: [requiredInput],
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
          label: this.$t("ui.data.column.gdyyStock.factoryCode"),
          type: "select",
          dictData: this.parentDict.type.biz_factory_name,
          filterable: true,
        },
        {
          prop: "stockDate",
          label: this.$t("ui.data.column.gdyyStock.stockDate"),
          type: "date",
          valueFormat: "yyyy-MM-dd",
        },
        {
          prop: "bigRollCode",
          label: this.$t("ui.data.column.gdyyStock.bigRollCode"),
          maxlength: 30,
        },
        {
          prop: "bigRollBarcode",
          label: this.$t("ui.data.column.gdyyStock.bigRollBarcode"),
          maxlength: 50,
        },
        {
          prop: "stockNum",
          label: this.$t("ui.data.column.gdyyStock.stockNum"),
          type: "number",
        },
        {
          prop: "stockRollNum",
          label: this.$t("ui.data.column.gdyyStock.stockRollNum"),
          type: "number",
        },
        {
          prop: "stockMeters",
          label: this.$t("ui.data.column.gdyyStock.stockMeters"),
          type: "number",
        },
        {
          prop: "modifyNum",
          label: this.$t("ui.data.column.gdyyStock.modifyNum"),
          type: "number",
        },
        {
          prop: "badNum",
          label: this.$t("ui.data.column.gdyyStock.badNum"),
          type: "number",
        },
        {
          prop: "estimateStockFlag",
          label: this.$t("ui.data.column.gdyyStock.estimateStockFlag"),
          type: "select",
          options: [
            { label: "预计库存", value: "0" },
            { label: "正式库存", value: "1" },
          ],
        },
        {
          prop: "remark",
          label: this.$t("ui.common.column.remark"),
          type: "textarea",
          rows: 3,
          maxlength: 900,
        },
      ];
    },
  },
  methods: {
    async save(params) {
      this.loading = true;
      try {
        const res = this.isEdit
          ? await updateGdyyStock(params)
          : await addGdyyStock(params);
        this.$modal.msgSuccess(res.msg);
        this.$emit("success");
        this.hide();
      } finally {
        this.loading = false;
      }
    },
    show(data) {
      this.visible = true;
      this.form = data || {};
      if (data) {
        this.isEdit = true;
      } else {
        this.isEdit = false;
        this.form = {
          factoryCode: "116",
        };
      }
    },
    hide() {
      this.visible = false;
      this.form = {};
      this.$refs.form.resetFields();
    },
    handleConfirm() {
      this.$refs.form.validate((valid) => {
        if (valid) {
          this.save(this.form);
        }
      });
    },
    openDialog(type, data) {
      if (type === "add") {
        this.show(null);
      } else {
        this.show(data);
      }
    },
  },
};
</script>
