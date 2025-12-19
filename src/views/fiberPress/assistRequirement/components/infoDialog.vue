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

import { editAssistRequirement } from "@/api/xwyy/assistRequirement";

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
        scheduleDate: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "blur",
          },
        ],
        bigRollCode: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
      },
      columns: [
        {
          label: this.$t("ui.data.column.scheduleResult.scheduleDate"),
          prop: "scheduleDate",
          span: 24,
          type: "date",
          valueFormat: "yyyy-MM-dd",
          required: true,
        },
        {
          label: this.$t("ui.data.column.xwyy.scheduleResult.bigRollCode"),
          prop: "bigRollCode",
          span: 24,
          maxlength: "30",
          required: true,
          // disabled: true,
        },
        {
          label: this.$t("ui.data.column.assistRequirement.midPlan"),
          prop: "dayPlanQty",
          span: 24,
          type: "number",
          min: 0,
          max: 9999999,
          precision: 0,
        },
        {
          label: this.$t("ui.data.column.assistRequirement.nightPlan"),
          prop: "nightPlanQty",
          span: 24,
          type: "number",
          min: 0,
          max: 9999999,
          precision: 0,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.todayStock"),
          prop: "todayStock",
          span: 24,
          type: "number",
          min: 0,
          max: 9999999,
          precision: 0,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.dayOut"),
          prop: "dayOut",
          span: 24,
          type: "number",
          min: 0,
          max: 9999999,
          precision: 0,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.fac5Class1Plan"),
          prop: "fac5Class1Plan",
          span: 24,
          type: "number",
          min: 0,
          max: 9999999,
          precision: 0,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.fac5Class2Plan"),
          prop: "fac5Class2Plan",
          span: 24,
          type: "number",
          min: 0,
          max: 9999999,
          precision: 0,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.fac5Class3Plan"),
          prop: "fac5Class3Plan",
          span: 24,
          type: "number",
          min: 0,
          max: 9999999,
          precision: 0,
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
        this.$t("ui.data.column.assistRequirement.modelName")
      );
    },
  },
  methods: {
    // api
    async save(params) {
      try {
        this.loading = true;

        const res = await editAssistRequirement(params);
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
