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
import moment from "moment";

import regexp from "@/utils/regexp";

import infoForm from "@/views/components/infoForm.vue";

import { editProductMinConfiguration } from "@/api/monthplan/productMinConfiguration";

import productSelect from "@/views/components/productSelect.vue";

export default {
  components: { infoForm },
  inject: ["parentDict"],
  data() {
    return {
      loading: false,
      visible: false,
      isEdit: false,
      editType: null,
      form: {},
      rules: {
        productCode: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        minQty: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "blur",
          },
        ],
        upQty: [
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
          label: this.$t("ui.data.column.confMinProd.factoryCode"),
          disabled: false,
          type: "select",
          dictData: this.parentDict.type.biz_factory_name,
        },
        {
          prop: "productCode",
          label: this.$t("ui.data.column.confMinProd.productCode"),
          render: (form) => {
            return (
              <productSelect
                v-model={form.productCode}
                label={form.productCode}
                onChange={this.handleProductChange}
              />
            );
          },
        },
        {
          prop: "productDesc",
          label: this.$t("ui.data.column.confMinProd.productDescription"),
          disabled: true,
        },
        {
          prop: "productType",
          label: this.$t("ui.data.column.confMinProd.productType"),
          disabled: true,
          type: "select",
          dictData: this.parentDict.type.biz_product_name,
        },
        {
          prop: "minQty",
          label: this.$t("ui.data.column.confMinProd.minQty"),
        },
        {
          prop: "upQty",
          label: this.$t("ui.data.column.confMinProd.upQty"),
        },

        // {
        //   label: this.$t("ui.common.column.remark"),
        //   prop: "remark",
        //   span: 24,
        //   type: "textarea",
        // },
      ];
    },
  },
  methods: {
    // api
    async save(params) {
      try {
        this.loading = true;

        const res = await editProductMinConfiguration(params);
        this.$modal.msgSuccess(res.msg);
        this.$emit("success");
        this.hide();
        this.loading = false;
      } catch (error) {
        console.log(error);
        this.loading = false;
      }
    },

    //utils
    show(data) {
      this.visible = true;
      if (data) {
        this.isEdit = true;
        this.form = {
          ...data,
          minQty: this.numberEmpty(data.minQty),
          upQty: this.numberEmpty(data.upQty),
        };
      } 
    },
    hide() {
      this.form = {};
      this.$refs.form.triggerResetForm();
      // this.resetForm("infoForm");
      this.isEdit = false;
      this.visible = false;
    },
    numberEmpty(val) {
      return this.isEmpty(val) ? undefined : val;
    },

    checkUnique(rule, value, callback) {
      return new Promise((resolve, reject) => {
        checkUnique({
          ...this.form,
        })
          .then((res) => {
            if (res === 0) {
              resolve();
            } else {
              reject(new Error(this.$t("ui.data.column.cx.machine.message")));
            }
          })
          .catch((error) => {
            console.error(error);
            reject(new Error("验证失败，请稍后再试"));
          });
      });
    },

    handleConfirm() {
      this.$refs.form.triggerConfirm(async (params) => {
        Object.keys(params).forEach((key) => {
          if (this.isEmpty(params[key])) {
            params[key] = "";
          }
        });

        if (
          Number(params.minQty) &&
          Number(params.upQty) &&
          Number(params.minQty) < Number(params.upQty)
        ) {
          this.$modal.msgError("上调控制水位应该小于等于最小批量值");
          return;
        }

        try {
          this.loading = true;
          // await this.checkUnique();
          this.save(params);
        } catch (error) {
          console.error(error);
          this.$modal.msgError(error.message);
          this.loading = false;
        }
      });
    },

    handleProductChange(value, row) {
      console.log(row);
      this.form.productDesc = row.productDesc;
    },
  },
};
</script>
