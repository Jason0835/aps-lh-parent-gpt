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

import infoForm from "@/views/components/infoForm.vue";

import { cxScheduleResultEdit } from "@/api/cx/cxScheduleResult";

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
    title: function () {
      return this.$t("ui.data.column.cxScheduleResult.cxAutoPlan");
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
          type: "date",
          valueFormat: "yyyy-MM-dd",
          disabled: true,
        },
        // {
        //   label: this.$t("ui.data.column.cxScheduleResult.taskType"),
        //   prop: "taskType",
        //   span: 12,
        //   disabled: true,
        // },
        {
          label: this.$t("ui.data.column.cxScheduleResult.cxMachineCode"),
          prop: "cxMachineCode",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.cxScheduleResult.lhMachineCode"),
          prop: "lhMachineCode",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.cxScheduleResult.lhMachineQty"),
          prop: "lhMachineQty",
          span: 12,
          disabled: true,
        },
        // {
        //   label: this.$t("ui.data.column.cxScheduleResult.maximumClassQty"),
        //   prop: "maximumClassQty",
        //   span: 12,
        //   type: "number",
        //   min: 0,
        //   max: 9999,
        // },
        // {
        //   label: this.$t(
        //     "ui.data.column.cxScheduleResult.minimumLhMachineReqQty"
        //   ),
        //   prop: "minimumLhMachineReqQty",
        //   span: 12,
        //   disabled: true,
        // },
        // {
        //   label: this.$t("ui.data.column.cxScheduleResult.workShifts"),
        //   prop: "workShifts",
        //   span: 12,
        //   disabled: true,
        //   type: "select",
        //   dictData: this.parentDict.type.CLASS_SHIFT,
        // },
        // {
        //   label: this.$t("ui.data.column.cxScheduleResult.availableMoldQty"),
        //   prop: "availableMoldQty",
        //   span: 12,
        //   disabled: true,
        //   type: "number",
        // },
        // {
        //   label: this.$t("ui.data.column.cxScheduleResult.storageLocation"),
        //   prop: "storageLocation",
        //   span: 12,
        //   disabled: true,
        //   type: "select",
        //   dictData: this.parentDict.type.STORAGE_LOCATION,
        // },
        {
          label: this.$t("ui.data.column.productConstruction.embryoVersion"),
          prop: "embryoVersion",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.cxScheduleResult.embryoCode"),
          prop: "embryoCode",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.cxScheduleResult.specDesc"),
          prop: "specDesc",
          span: 12,
          disabled: true,
        },

        {
          label: this.$t("ui.data.column.cxScheduleResult.sapCode"),
          prop: "sapCode",
          span: 12,
          disabled: true,
        },
        // {
        //   label: this.$t(
        //     "ui.data.column.cxScheduleResult.lhMiddleNightFinishQty"
        //   ),
        //   prop: "lhMiddleNightFinishQty",
        //   span: 12,
        // },
        {
          label: this.$t("ui.data.column.cxScheduleResult.br.dayPlanQty.br"),
          prop: "class3PlannedQty",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.cxScheduleResult.singleShiftLhQty"),
          prop: "singleShiftLhQty",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.cxScheduleResult.cxMonthFinishQty"),
          prop: "productNum",
          span: 12,
          disabled: true,
        },
        // {
        //   label: this.$t("ui.data.column.scheduleResult.monthPlan"),
        //   prop: "monthPlan",
        //   span: 12,
        //   disabled: true,
        // },
        // {
        //   label: this.$t("ui.data.column.cxScheduleResult.planModifyQty"),
        //   prop: "planModifyQty",
        //   span: 12,
        //   disabled: true,
        // },
        {
          label: this.$t("ui.data.column.cxScheduleResult.totalStock"),
          prop: "totalStock",
          span: 12,
          disabled: true,
        },
        // {
        //   label: this.$t("ui.data.column.cxScheduleResult.monthStock"),
        //   prop: "monthStock",
        //   span: 12,
        //   disabled: true,
        // },
        // {
        //   label: this.$t("ui.data.column.cxScheduleResult.rejectQty"),
        //   prop: "rejectQty",
        //   span: 12,
        //   disabled: true,
        // },
        // {
        //   label: this.$t("ui.data.column.cxScheduleResult.newestPlanQty"),
        //   prop: "newestPlanQty",
        //   span: 12,
        //   disabled: true,
        // },
        // {
        //   label: this.$t(
        //     "ui.data.column.cxScheduleResult.actualOverProduction"
        //   ),
        //   prop: "actualOverProduction",
        //   span: 12,
        //   disabled: true,
        // },
        // {
        //   label: this.$t(
        //     "ui.data.column.cxScheduleResult.expectedOverProduction"
        //   ),
        //   prop: "expectedOverProduction",
        //   span: 12,
        //   type: "number",
        //   min: -9999999,
        //   max: 9999999,
        //   precision: 0,
        // },
        // {
        //   label: this.$t(
        //     "ui.data.column.cxScheduleResult.differenceOverProduction"
        //   ),
        //   prop: "differenceOverProduction",
        //   span: 12,
        //   disabled: true,
        // },
        // {
        //   label: this.$t("ui.data.column.scheduleResult.monthPlanOs"),
        //   prop: "monthPlanOs",
        //   span: 12,
        //   disabled: true,
        // },
        {
          label: this.$t("ui.data.column.scheduleResult.isRelease"),
          prop: "isRelease",
          span: 12,
          disabled: true,
          type: "select",
          dictData: this.parentDict.type.IS_RELEASE,
        },
        {
          label: this.$t("ui.data.column.cxScheduleResult.specDimension"),
          prop: "specDimension",
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

        // ui.data.column.scheduleResult.class1
        {
          label: this.$t("早班"),
          type: "title",
        },
        // {
        //   label: this.$t(
        //     "ui.data.column.cxScheduleResult.class1AvailableLhShift"
        //   ),
        //   prop: "class1AvailableLhShift",
        //   span: 12,
        //   disabled: true,
        // },
        {
          label: this.$t("ui.data.column.scheduleResult.plan"),
          prop: "class1PlanQty",
          span: 12,
          disabled: true,
          type: "number",
        },
        {
          label: this.$t("ui.data.column.scheduleResult.finish"),
          prop: "class1FinishQty",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.analysis"),
          prop: "class1AnalysisInput",
          span: 12,
          maxlength: "66",
        },
        {
          label: this.$t("ui.data.column.scheduleResult.analySystem"),
          prop: "class1Analysis",
          span: 12,
          disabled: true,
        },

        //  ui.data.column.scheduleResult.class2
        {
          label: this.$t("中班"),
          type: "title",
        },
        // {
        //   label: this.$t(
        //     "ui.data.column.cxScheduleResult.class2AvailableLhShift"
        //   ),
        //   prop: "class2AvailableLhShift",
        //   span: 12,
        //   disabled: true,
        // },
        {
          label: this.$t("ui.data.column.scheduleResult.plan"),
          prop: "class2PlanQty",
          span: 12,
          disabled: true,
          type: "number",
          min: 0,
        },
        // {
        //   label: this.$t("ui.data.column.scheduleResult.class2ModifySort"),
        //   prop: "class2ModifySort",
        //   span: 12,
        //   // disabled: true,
        // },
        // {
        //   label: this.$t("ui.data.column.scheduleResult.class2ModifyQty"),
        //   prop: "class2ModifyQty",
        //   span: 12,
        // },
        {
          label: this.$t("ui.data.column.scheduleResult.finish"),
          prop: "class2FinishQty",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.analysis"),
          prop: "class2AnalysisInput",
          span: 12,
          maxlength: "66",
        },
        {
          label: this.$t("ui.data.column.scheduleResult.analySystem"),
          prop: "class2Analysis",
          span: 12,
          disabled: true,
        },

        {
          label: this.$t("晚班"),
          type: "title",
        },

        {
          label: this.$t("ui.data.column.scheduleResult.plan"),
          prop: "class3PlanQty",
          span: 12,
          disabled: true,
          type: "number",
          min: 0,
        },

        {
          label: this.$t("ui.data.column.scheduleResult.finish"),
          prop: "class3FinishQty",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.analysis"),
          prop: "class3AnalysisInput",
          span: 12,
          maxlength: "66",
        },
        {
          label: this.$t("ui.data.column.scheduleResult.analySystem"),
          prop: "class3Analysis",
          span: 12,
          disabled: true,
        },

        // ui.data.column.scheduleResult.class4
        {
          label: this.$t("早班"),
          type: "title",
        },

        {
          label: this.$t("ui.data.column.scheduleResult.plan"),
          prop: "class4PlanQty",
          span: 12,
          disabled: true,
          type: "number",
          min: 0,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.finish"),
          prop: "class4FinishQty",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.analysis"),
          prop: "class4AnalysisInput",
          span: 12,
          maxlength: "66",
        },
        {
          label: this.$t("ui.data.column.scheduleResult.analySystem"),
          prop: "class4Analysis",
          span: 12,
          disabled: true,
        },

        // ui.data.column.scheduleResult.class5
        {
          label: this.$t("中班"),
          type: "title",
        },


        {
          label: this.$t("ui.data.column.scheduleResult.plan"),
          prop: "class5PlanQty",
          span: 12,
          disabled: true,
          type: "number",
          min: 0,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.finish"),
          prop: "class5FinishQty",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.analysis"),
          prop: "class5AnalysisInput",
          span: 12,
          maxlength: "66",
        },
        {
          label: this.$t("ui.data.column.scheduleResult.analySystem"),
          prop: "class5Analysis",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("晚班"),
          type: "title",
        },

        {
          label: this.$t("ui.data.column.scheduleResult.plan"),
          prop: "class6PlanQty",
          span: 12,
          disabled: true,
          type: "number",
          min: 0,
        },

        {
          label: this.$t("ui.data.column.scheduleResult.finish"),
          prop: "class6FinishQty",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.analysis"),
          prop: "class6AnalysisInput",
          span: 12,
          maxlength: "66",
        },
        {
          label: this.$t("ui.data.column.scheduleResult.analySystem"),
          prop: "class6Analysis",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("早班"),
          type: "title",
        },

        {
          label: this.$t("ui.data.column.scheduleResult.plan"),
          prop: "class7PlanQty",
          span: 12,
          disabled: true,
          type: "number",
          min: 0,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.finish"),
          prop: "class7FinishQty",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.analysis"),
          prop: "class7AnalysisInput",
          span: 12,
          maxlength: "66",
        },
        {
          label: this.$t("ui.data.column.scheduleResult.analySystem"),
          prop: "class7Analysis",
          span: 12,
          disabled: true,
        },

        // ui.data.column.scheduleResult.class5
        {
          label: this.$t("中班"),
          type: "title",
        },


        {
          label: this.$t("ui.data.column.scheduleResult.plan"),
          prop: "class8PlanQty",
          span: 12,
          disabled: true,
          type: "number",
          min: 0,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.finish"),
          prop: "class8FinishQty",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.analysis"),
          prop: "class8AnalysisInput",
          span: 12,
          maxlength: "66",
        },
        {
          label: this.$t("ui.data.column.scheduleResult.analySystem"),
          prop: "class8Analysis",
          span: 12,
          disabled: true,
        },
      ];
    },
  },
  methods: {
    // api
    async save(params) {
      try {
        this.loading = true;

        await cxScheduleResultEdit(params);
        this.loading = false;
        this.$modal.msgSuccess(
          this.$t("common.msg.ajax.operation.success")
        );
        this.$emit("success");
        this.hide();
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
      this.$refs.form.triggerConfirm((params) => {
        this.save({
          ...params,
          local: "xg",
        });
      });
      // this.$refs.form.validate((valid) => {
      //   if (valid) {
      //     this.save({
      //       ...this.form,
      //     });
      //   }
      // });
    },
  },
};
</script>
