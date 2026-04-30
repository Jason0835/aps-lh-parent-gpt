<template>
  <el-dialog
    :title="title"
    :visible="visible"
    width="1000px"
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
import moment from "moment";
import { mapState } from "vuex";

import { numberEmpty } from "@/utils/index";

import infoForm from "@/views/components/infoForm.vue";

import { changeQty } from "@/api/gdyy/scheduleResult.js";

export default {
  components: { infoForm },
  inject: ["parentDict"],
  data() {
    return {
      loading: false,
      visible: false,
      isEdit: false,
      form: {
        scheduleDate: moment().add(1, "days").format("yyyy-MM-DD"),
      },
      midDisabled: false,
      nightDisabled: false,
      dayDisabled: false,
      rules: {
        // specName: [
        //   {
        //     required: true,
        //     message: this.$t("common.rule.input"),
        //     trigger: "blur",
        //   },
        // ],
      },
    };
  },
  computed: {
    ...mapState({
      machines: (state) => state.steelPress.machines,
    }),
    title: function () {
      return this.$t("ui.data.column.gdyy.scheduleResult.modelName");
    },
    columns() {
      return [
        {
          label: this.$t("ui.data.column.scheduleResult.baseInfo"),
          type: "title",
        },
        {
          label: this.$t("ui.data.column.scheduleResult.scheduleDate"),
          prop: "scheduleDate",
          span: 12,
          // type: "date",
          // valueFormat: "yyyy-MM-dd",
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.gdyy.scheduleResult.bigRollCode"),
          prop: "bigRollCode",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.produceLine"),
          prop: "machineCode",
          span: 12,
          disabled: true,
          type: "select",
          dictData: this.machines,
          labelKey: "machineName",
          valueKey: "id",
          valueType: "string",
        },
        {
          label: this.$t("ui.data.column.gdyy.scheduleResult.dayUsed"),
          prop: "dayUsed",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.gdyy.scheduleResult.stockQty"),
          prop: "stockQty",
          span: 12,
          disabled: true,
        },

        {
          label: this.$t("ui.data.column.scheduleResult.isRelease"),
          prop: "isRelease",
          span: 12,
          type: "select",
          disabled: true,
          dictData: this.parentDict.type.IS_RELEASE,
        },

        {
          label: this.$t("ui.data.column.scheduleResult.monthPlanOs"),
          prop: "monthPlanOs",
          span: 12,
          disabled: true,
        },

        {
          label: this.$t("ui.common.column.remark"),
          prop: "remark",
          span: 24,
          type: "textarea",
          maxlength: "300",
        },
        {
          label: this.$t("ui.data.column.scheduleResult.nightPlanEightHour"),
          type: "title",
        },

        {
          label: this.$t("ui.data.column.gdyy.scheduleResult.class2Plan"),
          prop: "class1Plan",
          span: 12,
          disabled: this.midDisabled,
          type: "number",
          min: 0,
          max: 999999,
          precision: 0,
        },
        // {
        //   label: this.$t(
        //     "ui.data.column.gdyy.scheduleResult.class2Plan.noStock"
        //   ),
        //   prop: "class1PlanNoStock",
        //   span: 12,
        //   disabled: true,
        //   type: "number",
        //   min: 0,
        //   max: 999999,
        //   precision: 0,
        // },

        {
          label: this.$t("ui.data.column.gdyy.scheduleResult.class2Finish"),
          prop: "class1Finish",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.gdyy.scheduleResult.class2Remark"),
          prop: "class1Remark",
          span: 12,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.midPlanEightHour"),
          type: "title",
        },
        {
          label: this.$t("ui.data.column.gdyy.scheduleResult.class1Plan"),
          prop: "class2Plan",
          span: 12,
          disabled: this.nightDisabled,
          type: "number",
          min: 0,
          max: 999999,
          precision: 0,
        },
        // {
        //   label: this.$t(
        //     "ui.data.column.gdyy.scheduleResult.class1Plan.noStock"
        //   ),
        //   prop: "class2PlanNoStock",
        //   span: 12,
        //   disabled: true,
        //   type: "number",
        //   min: 0,
        //   max: 999999,
        //   precision: 0,
        // },

        {
          label: this.$t("ui.data.column.gdyy.scheduleResult.class1Finish"),
          prop: "class2Finish",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.gdyy.scheduleResult.class1Remark"),
          prop: "class2Remark",
          span: 12,
        },

        // {
        //   label: this.$t("ui.data.column.scheduleResult.dayPlanEightHour"),
        //   type: "title",
        // },

        // {
        //   label: this.$t("ui.data.column.gdyy.scheduleResult.class3Plan"),
        //   prop: "class3Plan",
        //   span: 12,
        //   disabled: this.dayDisabled,
        //   type: "number",
        //   min: 0,
        //   max: 999999,
        //   precision: 0,
        // },
        // {
        //   label: this.$t(
        //     "ui.data.column.gdyy.scheduleResult.class3Plan.noStock"
        //   ),
        //   prop: "class3PlanNoStock",
        //   span: 12,
        //   disabled: true,
        //   type: "number",
        //   min: 0,
        //   max: 999999,
        //   precision: 0,
        // },

        // {
        //   label: this.$t("ui.data.column.gdyy.scheduleResult.class3Finish"),
        //   prop: "class3Finish",
        //   span: 12,
        //   type: "number",
        //   min: 0,
        //   max: 999999,
        //   precision: 0,
        // },
        // {
        //   label: this.$t("ui.data.column.gdyy.scheduleResult.class3Remark"),
        //   prop: "class3Remark",
        //   span: 12,
        // },
        {
          label: this.$t("中班计划（14:00-22:00)"),
          type: "title",
        },
        {
          label: this.$t("中班计划量"),
          prop: "nightPlanQty",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("中班完成量"),
          prop: "nightFinishQty",
          span: 12,
          disabled: true,
        },

        {
          label: this.$t("中班备注"),
          prop: "nightFinishRate",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.gdyy.scheduleResult.notes"),
          type: "title",
        },
        {
          label: this.$t("ui.data.column.gdyy.scheduleResult.notes"),
          prop: "notes",
          span: 24,
          disabled: true,
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
        let res = await changeQty(params);
        this.$modal.msgSuccess(
          this.$t("common.msg.ajax.operation.success")
        );
        this.$emit("success");
        this.hide();
        this.loading = false;
      } catch (error) {
        console.error(error);
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
          class1Plan: numberEmpty(data.class1Plan),
          class2Plan: numberEmpty(data.class2Plan),
        };
        if (data.scheduleDate) {
          if (moment().isAfter(data.scheduleDate)) {
            this.midDisabled = true;
          }
          if (moment().isAfter(data.scheduleDate + " 08:00:00")) {
            this.nightDisabled = true;
          }
          if (moment().isAfter(data.scheduleDate + " 16:00:00")) {
            this.dayDisabled = true;
          }
        } else {
          this.midDisabled = true;
          this.nightDisabled = true;
          this.dayDisabled = true;
        }
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
