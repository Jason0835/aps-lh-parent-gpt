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

import { editProductinfo } from "@/api/lean/productinfo";

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
        commonType: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        // outGrossRate: [
        //   {
        //     required: true,
        //     message: this.$t("common.rule.select"),
        //     trigger: "blur",
        //   },
        // ],
        // inGrossRate: [
        //   {
        //     required: true,
        //     message: this.$t("common.rule.input"),
        //     trigger: "blur",
        //   },
        // ],
        // oeGrossRate: [
        //   {
        //     required: true,
        //     message: this.$t("common.rule.input"),
        //     trigger: "blur",
        //   },
        //   {
        //     pattern: regexp.num1,
        //     message: this.$t("请输入数值，最多2位小数"),
        //     trigger: "blur",
        //   },
        // ],
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
          prop: "commonType",
          label: this.$t("ui.data.column.lean.productinfo.commonType"),
          type: "select",
          dictData: this.parentDict.type.biz_common_type,
        },
        {
          prop: "outGrossRate",
          label: this.$t("ui.data.column.lean.productinfo.outGrossRate"),
          type: "number",
          min: 0,
          max: 9.9999,
          precision: 4,
        },
        {
          prop: "inGrossRate",
          label: this.$t("ui.data.column.lean.productinfo.inGrossRate"),
          type: "number",
          min: 0,
          max: 9.9999,
          precision: 4,
        },
        {
          prop: "oeGrossRate",
          label: this.$t("ui.data.column.lean.productinfo.oeGrossRate"),
          type: "number",
          min: 0,
          max: 9.9999,
          precision: 4,
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

        const res = await editProductinfo(params);
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
          outGrossRate: this.numberEmpty(data.outGrossRate),
          inGrossRate: this.numberEmpty(data.inGrossRate),
          oeGrossRate: this.numberEmpty(data.oeGrossRate),
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

    handleConfirm() {
      this.$refs.form.triggerConfirm(async (params) => {
        Object.keys(params).forEach((key) => {
          if (this.isEmpty(params[key])) {
            params[key] = "";
          }
        });
        this.save(params);
      });
    },
  },
};
</script>
