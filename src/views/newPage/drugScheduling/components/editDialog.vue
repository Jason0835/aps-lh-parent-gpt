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

import { editGlueScheduleResult } from "@/api/schedule/glueScheduleResult.js";

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
      rules: {
        // specName: [
        //   {
        //     required: true,
        //     message: this.$t("common.rule.input"),
        //     trigger: "change",
        //   },
        // ],
      },
      columns: [
        {
          label: this.$t("ui.baseInfo"),
          type: "title",
        },
        {
          label: this.$t("schedule.glueScheduleResult.releaseStatus"),
          prop: "releaseStatus",
          span: 12,
          render: (form) => {
            return <dict-select v-model={form.releaseStatus} options={this.parentDict.type.MIX_RELEASE_STATUS} disabled />
          }
        },
        {
          label: this.$t("schedule.glueScheduleResult.scheduleDate"),
          prop: "scheduleDate",
          span: 12,
          // type: "date",
          // valueFormat: "yyyy-MM-dd",
          disabled: true,
        },
        {
          label: this.$t("schedule.glueScheduleResult.mixArea"),
          prop: "mixArea",
          span: 12,
          disabled: true,
          render: (form) => {
            return <dict-select v-model={form.mixArea} options={this.parentDict.type.MIX_AREA}  disabled />
          }
        },
        {
          label: this.$t("药品名称"),
          prop: "glue",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("称重工位"),
          prop: "machineName",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("schedule.glueScheduleResult.recipeTypeName"),
          prop: "recipeTypeName",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("schedule.glueScheduleResult.recipeVersionId"),
          prop: "recipeVersionId",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("schedule.glueScheduleResult.recipeStage"),
          prop: "recipeStage",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("schedule.glueScheduleResult.stockQty"),
          prop: "stockQty",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("schedule.glueScheduleResult.safeStockQty"),
          prop: "safeStockQty",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("schedule.glueScheduleResult.formulaWeight"),
          prop: "formulaWeight",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("schedule.glueScheduleResult.formulaTime"),
          prop: "formulaTime",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("schedule.glueScheduleResult.totalPlanQty"),
          prop: "totalPlanQty",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("schedule.glueScheduleResult.totalSurplus"),
          prop: "totalSurplus",
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
          label: this.$t("schedule.common.midClass"),
          type: "title",
        },

        {
          label: this.$t("schedule.glueScheduleResult.midPlanQty"),
          prop: "midPlanQty",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("schedule.glueScheduleResult.midFinishQty"),
          prop: "midFinishQty",
          span: 12,
          disabled: true,
        },

        {
          label: this.$t("schedule.glueScheduleResult.dayFinishRate"),
          prop: "dayFinishRate",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("schedule.glueScheduleResult.midProduceOrder"),
          prop: "midProduceOrder",
          span: 12,
          type: "number",
          min: 0,
          max: 999999,
          precision: 0,
        },
        {
          label: this.$t("schedule.glueScheduleResult.midExpectFinishTime"),
          prop: "midExpectFinishTime",
          span: 12,
          maxlength: "100",
        },
        {
          label: this.$t("schedule.glueScheduleResult.midRemark"),
          prop: "midRemark",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("schedule.common.nightClass"),
          type: "title",
        },
        {
          label: this.$t("schedule.glueScheduleResult.nightPlanQty"),
          prop: "nightPlanQty",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("schedule.glueScheduleResult.nightFinishQty"),
          prop: "nightFinishQty",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("schedule.glueScheduleResult.nightFinishRate"),
          prop: "nightFinishRate",
          span: 12,
          disabled: true,
          type: "number",
          min: 0,
          max: 999999,
          precision: 0,
        },
        {
          label: this.$t("schedule.glueScheduleResult.nightProduceOrder"),
          prop: "nightProduceOrder",
          span: 12,
        },
        {
          label: this.$t("schedule.glueScheduleResult.nightExpectFinishTime"),
          prop: "nightExpectFinishTime",
          span: 12,
          maxlength: "100",
        },
        {
          label: this.$t("schedule.glueScheduleResult.nightRemark"),
          prop: "nightRemark",
          span: 12,
          disabled: true,
        },
        // {
        //   label: this.$t("schedule.common.dayClass"),
        //   type: "title",
        // },
        // {
        //   label: this.$t("schedule.glueScheduleResult.dayPlanQty"),
        //   prop: "nightPlanQty",
        //   span: 12,
        //   disabled: true,
        // },
        // {
        //   label: this.$t("schedule.glueScheduleResult.dayFinishQty"),
        //   prop: "nightFinishQty",
        //   span: 12,
        //   disabled: true,
        // },
        // {
        //   label: this.$t("schedule.glueScheduleResult.dayFinishRate"),
        //   prop: "nightFinishRate",
        //   span: 12,
        //   disabled: true,
        //   type: "number",
        //   min: 0,
        //   max: 999999,
        //   precision: 0,
        // },
        // {
        //   label: this.$t("schedule.glueScheduleResult.dayProduceOrder"),
        //   prop: "dayProduceOrder",
        //   span: 12,
        // },
        // {
        //   label: this.$t("schedule.glueScheduleResult.dayExpectFinishTime"),
        //   prop: "dayExpectFinishTime",
        //   span: 12,
        //   maxlength: "100",
        // },
        // {
        //   label: this.$t("schedule.glueScheduleResult.dayRemark"),
        //   prop: "dayRemark",
        //   span: 12,
        //   disabled: true,
        // },
        {
          label: this.$t("中班（14:00-22:00)"),
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
          label: this.$t("中班生产顺序"),
          prop: "nightProduceOrder",
          span: 12,
          disabled: this.nightDisabled,
          type: "number",
          min: 0,
          max: 999999,
          precision: 0,
        },
        {
          label: this.$t("中班预计完成时间"),
          prop: "nightSysAnalysis",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("中班备注"),
          prop: "nightHandAnalysis",
          span: 12,
          maxlength: "100",
        },

      ],
    };
  },
  computed: {
    title: function () {
      return this.$t("schedule.glueScheduleResult.modelName");
    },
  },
  methods: {
    // api
    async save(params) {
      try {
        this.loading = true;
        let res = await editGlueScheduleResult(params);
        this.$modal.msgSuccess("操作成功")
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
