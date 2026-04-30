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

import infoForm from "@/views/components/infoForm.vue";

import { editCloseOutRange } from "@/api/cx/closeOutRange";

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
        rangeName: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        closeOutRangeMinimum: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        closeOutRangeMaximum: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        rangeValue: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
      },
      columns: [
        {
          label: this.$t("ui.data.column.closeOutRange.rangeName"),
          prop: "rangeName",
          span: 24,
          maxlength: "100",
          required: true,
        },
        {
          label: this.$t("ui.data.column.closeOutRange.closeOutRangeMinimum"),
          prop: "closeOutRangeMinimum",
          span: 24,
          type: "number",
          required: true,
          min: 0,
          max: 99999999,
          precision: 0,
        },
        {
          label: this.$t("ui.data.column.closeOutRange.closeOutRangeMaximum"),
          prop: "closeOutRangeMaximum",
          span: 24,
          type: "number",
          required: true,
          min: 0,
          max: 99999999,
          precision: 0,
        },
        {
          label: this.$t("ui.data.column.closeOutRange.rangeValue"),
          prop: "rangeValue",
          span: 24,
          type: "number",
          required: true,
          min: 0,
          max: 99999.9,
          precision: 1,
        },
        {
          label: this.$t("ui.common.column.remark"),
          prop: "remark",
          span: 24,
          type: "textarea",
          maxlength: "300",
        },
      ],
    };
  },
  computed: {
    title: function () {
      return (
        (this.isEdit
          ? this.$t("common.button.edit")
          : this.$t("common.button.add")) +
        this.$t("ui.data.column.closeOutRange.modelName")
      );
    },
  },
  methods: {
    // api
    async save(params) {
      try {
        this.loading = true;

        const res = await editCloseOutRange(params);
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
