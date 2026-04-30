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

import { editScheduleResult } from "@/api/tq/scheduleResult.js";

export default {
  components: { infoForm },
  inject: ["parentDict"],
  data() {
    return {
      loading: false,
      visible: false,
      isEdit: false,
      form: {},
      midDisabled: false,
      nightDisabled: false,
      dayDisabled: false,
      nextMidDisabled: false,
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
      machines: (state) => state.bead.machines,
    }),
    title: function () {
      return this.$t("ui.data.column.tq.scheduleResult.modelName");
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
          label: this.$t("ui.data.column.tq.scheduleResult.beadCode"),
          prop: "beadCode",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.gsq.scheduleResult.steelRingCode"),
          prop: "steelRingCode",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.tq.scheduleResult.triangleGlueCode"),
          prop: "triangleGlueCode",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.glueCode"),
          prop: "glueCode",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.mouthPlateCode"),
          prop: "mouthPlateCode",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.produceLine"),
          prop: "machineId",
          span: 12,
          disabled: true,
          type: "select",
          dictData: this.machines,
          valueKey: "id",
          labelKey: "machineName",
          valueType: "string",
        },
        {
          label: this.$t("ui.data.column.scheduleResult.stockQty"),
          prop: "stockQty",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.specSize"),
          prop: "specSize",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.supplyTime"),
          prop: "supplyTime",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.isRelease"),
          prop: "isRelease",
          span: 12,
          type: "select",
          dictData: this.parentDict.type.IS_RELEASE,
          disabled: true,
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
          span: 12,
          type: "textarea",
          maxlength: "300",
        },
        {
          label: this.$t("ui.data.column.scheduleResult.nightPlanEightHour"),
          type: "title",
        },

        {
          label: this.$t("ui.data.column.scheduleResult.nightPlanQty"),
          prop: "midPlanQty",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.nightFinishQty"),
          prop: "midFinishQty",
          span: 12,
          disabled: true,
        },

        {
          label: this.$t("ui.data.column.scheduleResult.nightFinishRate"),
          prop: "midFinishRate",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.nightProduceOrder"),
          prop: "midProduceOrder",
          span: 12,
          disabled: this.midDisabled,
          type: "number",
          min: 0,
          max: 999999,
          precision: 0,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.nightSysAnalysis"),
          prop: "midSysAnalysis",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.nightHandAnalysis"),
          prop: "midHandAnalysis",
          span: 12,
          maxlength: "100",
        },
        {
          label: this.$t("ui.data.column.scheduleResult.midPlanEightHour"),
          type: "title",
        },
        {
          label: this.$t("ui.data.column.scheduleResult.dayPlanQty"),
          prop: "nightPlanQty",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.dayFinishQty"),
          prop: "nightFinishQty",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.dayFinishRate"),
          prop: "nightFinishRate",
          span: 12,
          disabled: true,
          type: "number",
          min: 0,
          max: 999999,
          precision: 0,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.dayProduceOrder"),
          prop: "nightProduceOrder",
          span: 12,
          disabled: this.nightDisabled,
          type: "number",
          min: 0,
          max: 999999,
          precision: 0,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.daySysAnalysis"),
          prop: "nightSysAnalysis",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.dayHandAnalysis"),
          prop: "nightHandAnalysis",
          span: 12,
          maxlength: "100",
        },
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
          label: this.$t("中班完成率"),
          prop: "nightFinishRate",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("中班顺序"),
          prop: "nightProduceOrder",
          span: 12,
          disabled: this.nightDisabled,
          type: "number",
          min: 0,
          max: 999999,
          precision: 0,
        },
        {
          label: this.$t("中班系统原因分析"),
          prop: "nightSysAnalysis",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("中班手动输入原因分析"),
          prop: "nightHandAnalysis",
          span: 12,
          maxlength: "100",
        },

        // {
        //   label: this.$t("ui.data.column.scheduleResult.dayPlanEightHour"),
        //   type: "title",
        // },
        // {
        //   label: this.$t("ui.data.column.scheduleResult.midPlanQty"),
        //   prop: "dayPlanQty",
        //   span: 12,
        //   disabled: true,
        // },
        // {
        //   label: this.$t("ui.data.column.scheduleResult.midFinishQty"),
        //   prop: "dayFinishQty",
        //   span: 12,
        //   disabled: true,
        // },

        // {
        //   label: this.$t("ui.data.column.scheduleResult.midFinishRate"),
        //   prop: "dayFinishRate",
        //   span: 12,
        //   disabled: true,
        // },
        // {
        //   label: this.$t("ui.data.column.scheduleResult.midProduceOrder"),
        //   prop: "dayProduceOrder",
        //   span: 12,
        //   disabled: this.dayDisabled,
        //   type: "number",
        //   min: 0,
        //   max: 999999,
        //   precision: 0,
        // },
        // {
        //   label: this.$t("ui.data.column.scheduleResult.midSysAnalysis"),
        //   prop: "daySysAnalysis",
        //   span: 12,
        //   disabled: true,
        // },
        // {
        //   label: this.$t("ui.data.column.scheduleResult.midHandAnalysis"),
        //   prop: "dayHandAnalysis",
        //   span: 12,
        //   maxlength: "100",
        // },

        // {
        //   label: this.$t("ui.data.column.scheduleResult.midPlanEightHourTwo"),
        //   type: "title",
        // },
        // {
        //   label: this.$t("ui.data.column.scheduleResult.nextMidPlanQty"),
        //   prop: "nextMidPlanQty",
        //   span: 12,
        //   disabled: true,
        // },
        // {
        //   label: this.$t("ui.data.column.scheduleResult.nextMidFinishQty"),
        //   prop: "nextMidFinishQty",
        //   span: 12,
        //   disabled: true,
        // },
        // {
        //   label: this.$t("ui.data.column.scheduleResult.nextMidFinishRate"),
        //   prop: "nextMidFinishRate",
        //   span: 12,
        //   disabled: true,
        // },
        // {
        //   label: this.$t("ui.data.column.scheduleResult.nextMidProduceOrder"),
        //   prop: "nextMidProduceOrder",
        //   span: 12,
        //   disabled: this.nextMidDisabled,
        //   type: "number",
        //   min: 0,
        //   max: 999999,
        //   precision: 0,
        // },
        // {
        //   label: this.$t("ui.data.column.scheduleResult.nextMidSysAnalysis"),
        //   prop: "nextMidSysAnalysis",
        //   span: 12,
        //   disabled: true,
        // },
        // {
        //   label: this.$t("ui.data.column.scheduleResult.nextMidHandAnalysis"),
        //   prop: "nextMidHandAnalysis",
        //   span: 12,
        //   maxlength: "100",
        // },

        {
          label: this.$t("ui.data.column.scheduleResult.tqPlan2"),
          type: "title",
        },
        {
          label: this.$t("ui.data.column.scheduleResult.cxClass1Plan"),
          prop: "cxClass2Plan",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class3Plan2"),
          prop: "cxClass4Plan",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class2Plan2"),
          prop: "cxClass3Plan",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.cxClass4Plan"),
          prop: "cxClass5Plan",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("中班计划量"),
          prop: "cxClass3Plan",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("次日中班计划量"),
          prop: "cxClass5Plan",
          span: 12,
          disabled: true,
        },
        // {
        //   label: this.$t("ui.data.column.scheduleResult.cxClass5Plan"),
        //   prop: "cxClass5Plan",
        //   span: 12,
        //   disabled: true,
        // },
      ];
    },
  },
  methods: {
    // api
    async save(params) {
      try {
        this.loading = true;
        let res = await editScheduleResult(params);
        this.$modal.msgSuccess(res.msg);
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
          midPlanQty: numberEmpty(data.midPlanQty),
          midProduceOrder: numberEmpty(data.midProduceOrder),
          nightPlanQty: numberEmpty(data.nightPlanQty),
          nightProduceOrder: numberEmpty(data.nightProduceOrder),
          // midFinishRate: Number(data.midFinishRate * 100).toFixed(2),
          // nightFinishRate: Number(data.nightFinishRate * 100).toFixed(2),
          // dayFinishRate: Number(data.dayFinishRate * 100).toFixed(2),
          // nextMidFinishRate: Number(data.nextMidFinishRate * 100).toFixed(2),
        };
      }
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
        if (moment().isAfter(moment(data.scheduleDate).add(1, "days"))) {
          this.nextMidDisabled = true;
        }
      } else {
        this.midDisabled = true;
        this.nightDisabled = true;
        this.dayDisabled = true;
        this.nextMidDisabled = true;
      }
    },
    hide() {
      this.form = {};
      this.$refs.form.triggerResetForm();
      // this.resetForm("infoForm");
      this.isEdit = false;
      this.visible = false;
      this.midDisabled = false;
      this.nightDisabled = false;
      this.dayDisabled = false;
      this.nextMidDisabled = false;
    },

    handleConfirm() {
      this.$refs.form.triggerConfirm((params) => {
        this.save({
          ...params,
          midFinishRate: undefined,
          nightFinishRate: undefined,
          dayFinishRate: undefined,
          nextMidFinishRate: undefined,
        });
      });
    },
  },
};
</script>
