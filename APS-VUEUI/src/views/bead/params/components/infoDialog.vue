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

import { editParams } from "@/api/tq/params";

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
          label: this.$t("ui.data.column.paramsCode"),
          prop: "paramCode",
          span: 24,
          maxlength: "100",
          required: true,
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.paramsName"),
          prop: "paramName",
          span: 24,
          maxlength: "100",
          required: true,
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.paramsValue"),
          prop: "paramValue",
          span: 24,
          required: false,
        },
        // {
        //   label: this.$t("ui.data.column.errorTips"),
        //   prop: "errorTips",
        //   span: 24,
        //   required: false,
        // },
        // {
        //   label: this.$t("ui.data.column.regular"),
        //   prop: "regular",
        //   span: 24,
        //   required: true,
        // },
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
        this.$t("ui.data.column.tq.params.modelName")
      );
    },
  },
  methods: {
    // api
    async save(params) {
      try {
        this.loading = true;

        const res = await editParams(params);
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
