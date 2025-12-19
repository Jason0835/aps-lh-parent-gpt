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

import { changeQty } from "@/api/tm/tmScheduleResult";

export default {
  components: { infoForm },
  prop: {},
  inject: ["parentDict"],
  data() {
    return {
      loading: false,
      visible: false,
      isEdit: false,
      editType: null,
      form: {},
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
      machines: (state) => state.tread.machines,
    }),
    title: function () {
      return this.$t("ui.data.column.scheduleResult.autoPlan");
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
          label: this.$t("ui.data.column.tmScheduleResult.treadCode"),
          prop: "treadCode",
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
          label: this.$t("ui.data.column.scheduleResult.stockQty"),
          prop: "stockQty",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.glueSeq"),
          prop: "glueSeq",
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
          label: this.$t("ui.data.column.scheduleResult.mouthPlateCode"),
          prop: "mouthPlateCode",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.monthPlanOs"),
          prop: "monthPlanOs",
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
          label: this.$t("ui.common.column.remark"),
          prop: "remark",
          span: 12,
          type: "textarea",
          maxlength: "300",
        },
        {
          label: this.$t("ui.data.column.scheduleResult.nightPlan"),
          type: "title",
        },

        {
          label: this.$t("ui.data.column.scheduleResult.nightPlanQty"),
          prop: "dayPlanQty",
          span: 12,
          disabled: this.dayDisabled,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.nightFinishQty"),
          prop: "dayFinishQty",
          span: 12,
          disabled: true,
        },

        {
          label: this.$t("ui.data.column.scheduleResult.nightFinishRate"),
          prop: "dayFinishRate",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.nightProduceOrder"),
          prop: "dayProduceOrder",
          span: 12,
          disabled: this.dayDisabled,
          type: "number",
          min: 0,
          max: 999999,
          precision: 0,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.nightSysAnalysis"),
          prop: "daySysAnalysis",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.nightHandAnalysis"),
          prop: "dayHandAnalysis",
          span: 12,
          maxlength: "100",
        },

        {
          label: this.$t("ui.data.column.scheduleResult.dayPlan"),
          type: "title",
        },
        {
          label: this.$t("ui.data.column.scheduleResult.dayPlanQty"),
          prop: "nightPlanQty",
          span: 12,
          disabled: this.nightDisabled,
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

        {
          label: this.$t("ui.data.column.scheduleResult.prePlanQty"),
          type: "title",
        },
        {
          label: `${this.$t("ui.data.column.scheduleResult.plan")}(${this.$t(
            "ui.data.column.scheduleResult.unit.meter"
          )})`,
          prop: "prePlanQty",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.tmPlan2"),
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
        const res = await changeQty(params);
        this.$modal.msgSuccess(
          this.$t("common.msg.ajax.operation.success")
        );
        this.loading = false;
        this.$emit("success");
        this.hide();
      } catch (error) {
        console.error(error);
        this.loading = false;
      }
    },

    //utils
    show(data, editType) {
      this.visible = true;
      this.editType = editType;
      if (data) {
        this.isEdit = true;
        this.form = {
          ...data,
          nightPlanQty: numberEmpty(data.nightPlanQty),
          dayProduceOrder: numberEmpty(data.dayProduceOrder),
          nightPlanQty: numberEmpty(data.nightPlanQty),
          nightProduceOrder: numberEmpty(data.nightProduceOrder),
        };
        if (data.scheduleDate) {
          if (moment().isAfter(data.scheduleDate)) {
            this.dayDisabled = true;
          }
          if (moment().isAfter(data.scheduleDate + " 12:00:00")) {
            this.nightDisabled = true;
          }
        } else {
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
      this.nightDisabled = false;
      this.dayDisabled = false;
    },

    handleConfirm() {
      this.$refs.form.triggerConfirm((params) => {
        this.save({
          ...params,
          nightFinishRate: undefined,
          dayFinishRate: undefined,
        });
      });
    },
  },
};
</script>
