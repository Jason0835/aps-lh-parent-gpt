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

import { numberEmpty } from "@/utils/index"


import infoForm from "@/views/components/infoForm.vue";

import { editShiftLimit } from "@/api/cx/shiftLimit";

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
        limitName: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        type: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "blur",
          },
        ],
        limitType: [
          {
            required: true,
            message: this.$t("common.rule.select"),
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
          : this.$t("common.button.add")) +
        this.$t("ui.data.column.shiftLimit.modelName")
      );
    },
    columns() {
      return [
        {
          label: this.$t("ui.data.column.shiftLimit.limitName"),
          prop: "limitName",
          span: 24,
          maxlength: "100",
          required: true,
        },
        {
          label: this.$t("ui.data.column.shiftLimit.type"),
          prop: "type",
          span: 24,
          type: "select", //MACHINE_TYPE
          required: true,
          dictData: this.parentDict.type.MACHINE_TYPE,
        },
        {
          label: this.$t("ui.data.column.shiftLimit.limitType"),
          prop: "limitType",
          span: 24,
          type: "select", //limit_type
          required: true,
          dictData: this.parentDict.type.limit_type,
        },
        {
          label: this.$t("ui.data.column.shiftLimit.stockNum"),
          prop: "stockNum",
          span: 24,
          type: "number",
          min: 0,
          max: 99999999,
          precision: 0,
          required: true,
        },
        {
          label: this.$t("ui.data.column.shiftLimit.shiftParams"),
          prop: "shiftParams",
          span: 24,
          type: "select",
          dictData: this.parentDict.type.biz_stor_type,
          required: true,
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
    // api
    async save(params) {
      try {
        this.loading = true;

        const res = await editShiftLimit(params);
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
          stockNum: numberEmpty(data.stockNum),
          shiftParams: data.shiftParams + ""
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
    handleConfirm() {
      this.$refs.form.triggerConfirm(this.save);
    },
  },
};
</script>
