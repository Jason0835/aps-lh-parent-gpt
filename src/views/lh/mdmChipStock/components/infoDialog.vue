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
import {
  checkMdmChipStockUnique,
  editMdmChipStock,
  mergeMdmChipStock,
} from "@/api/lh/mdmChipStock";

import infoForm from "@/views/components/infoForm.vue";

export default {
  components: { infoForm },
  inject: ["parentDict"],
  data() {
    return {
      loading: false,
      visible: false,
      isEdit: false,
      form: {},
      rules: {
        factoryCode: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
        chipCode: [
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
      return this.isEdit
        ? this.$t("common.button.edit")
        : this.$t("common.button.add");
    },
    columns() {
      return [
        {
          prop: "factoryCode",
          label: this.$t("ui.data.column.mdmChipStock.factoryCode"),
          type: "select",
          dictData: this.parentDict.type.biz_factory_name,
        },
        {
          prop: "chipCode",
          label: this.$t("ui.data.column.mdmChipStock.chipCode"),
          maxlength: 32,
        },
        {
          prop: "stockNum",
          label: this.$t("ui.data.column.mdmChipStock.stockNum"),
          type: "number",
        },
        {
          prop: "finishQty",
          label: this.$t("ui.data.column.mdmChipStock.finishQty"),
          type: "number",
        },
        {
          prop: "remark",
          label: this.$t("ui.data.column.mdmChipStock.remark"),
          type: "textarea",
          rows: 3,
          maxlength: 500,
        },
      ];
    },
  },
  methods: {
    // api
    async save(params) {
      try {
        this.loading = true;

        const res = await editMdmChipStock(params);
        this.$modal.msgSuccess(res.msg);
        this.$emit("success");
        this.hide();
      } catch (error) {
        console.log(error);
      } finally {
        this.loading = false;
      }
    },
    async merge(params) {
      try {
        this.loading = true;

        const res = await mergeMdmChipStock(params);
        this.$modal.msgSuccess(res.msg);
        this.$emit("success");
        this.hide();
      } catch (error) {
        console.log(error);
      } finally {
        this.loading = false;
      }
    },
    async checkUniqueAndSave(params) {
      try {
        const res = await checkMdmChipStockUnique(params);
        if (res === 0) {
          await this.save(params);
        } else {
          await this.$confirm(this.$t("mdmChipStock.chipCodeExistsConfirmMerge"), {
            type: "warning",
            confirmButtonText: this.$t("common.button.confirm"),
            cancelButtonText: this.$t("common.button.cancel"),
          });
          await this.merge(params);
        }
      } catch (error) {
        // ignore confirm cancel/close
        if (error !== "cancel" && error !== "close") {
          console.log(error);
        }
      }
    },

    // utils
    show(data) {
      this.visible = true;
      if (data) {
        this.isEdit = true;
        this.form = { ...data };
      } else {
        this.isEdit = false;
        this.form = {
          factoryCode: "116",
          id: undefined,
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
      const callback = this.isEdit ? this.save : this.checkUniqueAndSave;
      this.$refs.form.triggerConfirm(callback);
    },
  },
};
</script>
