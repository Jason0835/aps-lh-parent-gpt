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

import { editScheduleResult } from "@/api/schedule/materialScheduleResult.js";

export default {
  components: { infoForm },
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
        //     trigger: "blur",
        //   },
        // ],
      },
      columns: [
        {
          label: this.$t("ui.baseInfo"),
          type: "title",
        },
        {
          label: this.$t("schedule.materialScheduleResult.releaseStatus"),
          prop: "releaseStatus",
          span: 12,
          // type: "date",
          // valueFormat: "yyyy-MM-dd",
          disabled: true,
        },
        {
          label: this.$t("schedule.materialScheduleResult.scheduleDate"),
          prop: "scheduleDate",
          span: 12,
          // type: "date",
          // valueFormat: "yyyy-MM-dd",
          disabled: true,
        },
        {
          label: this.$t("schedule.materialScheduleResult.mixArea"),
          prop: "treadCode",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("schedule.materialScheduleResult.materialName"),
          prop: "materialName",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("schedule.materialScheduleResult.machineName"),
          prop: "machineCode",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("schedule.materialScheduleResult.classShift"),
          prop: "classShift",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("schedule.materialScheduleResult.recipeTypeName"),
          prop: "recipeTypeName",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("schedule.materialScheduleResult.recipeVersionId"),
          prop: "recipeVersionId",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("schedule.materialScheduleResult.recipeStage"),
          prop: "recipeStage",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("schedule.materialScheduleResult.stockQty"),
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
          label: this.$t("schedule.materialScheduleResult.demandQty"),
          prop: "demandQty",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("schedule.materialScheduleResult.demandPlanning"),
          prop: "demandPlanning",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("schedule.materialScheduleResult.totalPlanQty"),
          prop: "totalPlanQty",
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
          label: this.$t("schedule.materialScheduleResult.midPlanQty"),
          prop: "midPlanQty",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("schedule.materialScheduleResult.midFinishQty"),
          prop: "midFinishQty",
          span: 12,
          disabled: true,
        },

        {
          label: this.$t("schedule.materialScheduleResult.dayFinishRate"),
          prop: "dayFinishRate",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("schedule.materialScheduleResult.midProduceOrder"),
          prop: "midProduceOrder",
          span: 12,
          type: "number",
          min: 0,
          max: 999999,
          precision: 0,
        },
        {
          label: this.$t("schedule.materialScheduleResult.midExpectFinishTime"),
          prop: "midExpectFinishTime",
          span: 12,
          maxlength: "100",
        },
        {
          label: this.$t("schedule.materialScheduleResult.midRemark"),
          prop: "midRemark",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("schedule.common.nightClass"),
          type: "title",
        },
        {
          label: this.$t("schedule.materialScheduleResult.nightPlanQty"),
          prop: "nightPlanQty",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("schedule.materialScheduleResult.nightFinishQty"),
          prop: "nightFinishQty",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("schedule.materialScheduleResult.nightFinishRate"),
          prop: "nightFinishRate",
          span: 12,
          disabled: true,
          type: "number",
          min: 0,
          max: 999999,
          precision: 0,
        },
        {
          label: this.$t("schedule.materialScheduleResult.nightProduceOrder"),
          prop: "nightProduceOrder",
          span: 12,
        },
        {
          label: this.$t("schedule.materialScheduleResult.nightExpectFinishTime"),
          prop: "nightExpectFinishTime",
          span: 12,
          maxlength: "100",
        },
        {
          label: this.$t("schedule.materialScheduleResult.nightRemark"),
          prop: "nightRemark",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("schedule.common.dayClass"),
          type: "title",
        },
        {
          label: this.$t("schedule.materialScheduleResult.dayPlanQty"),
          prop: "nightPlanQty",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("schedule.materialScheduleResult.dayFinishQty"),
          prop: "nightFinishQty",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("schedule.materialScheduleResult.dayFinishRate"),
          prop: "nightFinishRate",
          span: 12,
          disabled: true,
          type: "number",
          min: 0,
          max: 999999,
          precision: 0,
        },
        {
          label: this.$t("schedule.materialScheduleResult.dayProduceOrder"),
          prop: "dayProduceOrder",
          span: 12,
        },
        {
          label: this.$t("schedule.materialScheduleResult.dayExpectFinishTime"),
          prop: "dayExpectFinishTime",
          span: 12,
          maxlength: "100",
        },
        {
          label: this.$t("schedule.materialScheduleResult.dayRemark"),
          prop: "dayRemark",
          span: 12,
          disabled: true,
        },
      ],
    };
  },
  computed: {
    title: function () {
      return this.$t("ui.data.column.tmScheduleResult.modalName");
    },
  },
  methods: {
    // api
    async save(params) {
      try {
        this.loading = true;
        let res = await editScheduleResult(params);
        this.$modal.msgSuccess(res.msg)
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
