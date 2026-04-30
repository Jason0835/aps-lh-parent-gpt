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

import { editProductALevel, checkUnique } from "@/api/mdm/productALevel";

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
        factoryCode: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "blur",
          },
        ],
        productTypeCode: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        aLevel: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
          {
            pattern: regexp.num1,
            message: this.$t("请输入数值，最多2位小数"),
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
          : this.$t("common.button.add")) + this.$t("OEE率")
      );
    },
    columns() {
      return [
        {
          prop: "productCode",
          label: this.$t("SAP代码"),
        },
        {
          prop: "factoryCode",
          label: this.$t("分厂"),
          type: "select",
          dictData: this.parentDict.type.biz_factory_name,
        },
        {
          prop: "productTypeCode",
          label: this.$t("胎别"),
          type: "select",
          dictData: this.parentDict.type.biz_product_name,
        },
        {
          prop: "aLevel",
          label: this.$t("OEE率"),
          type: "input",
          render: (form) => {
            return (
              <el-input v-model={form.aLevel}>
                <template slot="append">%</template>
              </el-input>
            );
          },
        },
        {
          label: this.$t("ui.common.column.remark"),
          prop: "remark",
          span: 24,
          type: "textarea",
        },
      ];
    },
  },
  methods: {
    // api
    async save(params) {
      try {
        this.loading = true;

        const res = await editProductALevel(params);
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
          aLevel: this.numberEmpty(data.alevel),
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
  },
};
</script>
