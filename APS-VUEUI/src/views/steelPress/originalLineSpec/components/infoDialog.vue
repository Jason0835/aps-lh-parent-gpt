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

// import { editOriginalLineSpec } from "@/api/gdyy/gdyyOriginalLineSpec";

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
        originalLineSpec: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        originalLineName: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        originalLineLength: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
      },
      columns: [
        {
          label: this.$t("ui.data.column.xwyy.spec.originalLineSpec"),
          prop: "originalLineCode",
          span: 24,
          maxlength: "30",
          required: true,
          // disabled: true,
        },
        {
          label: this.$t("ui.data.column.xwyy.spec.originalLineName"),
          prop: "originalLineName",
          span: 24,
          maxlength: "100",
          required: true,
        },
        {
          label: this.$t("ui.data.column.xwyy.spec.originalLineLength"),
          prop: "originalLineLength",
          span: 24,
          type: "number",
          min: 0,
          max: 9999999,
          precision: 0,
        },
        // {
        //   label: this.$t("ui.data.column.xwyy.spec.breakRollNum"),
        //   prop: "breakRollNum",
        //   span: 24,
        //   type: "number",
        //   min: 0,
        //   max: 9999999,
        //   precision: 0,
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
      return this.$t("纤维原线规格管理");
    },
  },
  methods: {
    // api
    async save(params) {
      try {
        this.loading = true;

        // const res = await editOriginalLineSpec(params);
        // this.$modal.msgSuccess(res.msg);
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
