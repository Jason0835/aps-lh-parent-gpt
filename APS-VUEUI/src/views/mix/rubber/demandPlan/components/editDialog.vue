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

import { numberEmpty } from "@/utils/index";

import { editGlueDemandPlan } from "@/api/schedule/glueDemandPlan";

import infoForm from "@/views/components/infoForm.vue";

export default {
  components: { infoForm },
  inject: ["parentDict"],
  data() {
    return {
      loading: false,
      visible: false,
      isEdit: false,
      editType: null,
      form: {
        scheduleDate: moment().add(1, "days").format("yyyy-MM-DD"),
      },
      rules: {
        scheduleDate: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        factory: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        glue: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        mixArea: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
      },
    };
  },
  computed: {
    title: function () {
      return this.$t("schedule.glueDemandPlan.modelName");
    },
    columns() {
      return [
        {
          label: this.$t("ui.baseInfo"),
          type: "title",
        },
        {
          label: this.$t("schedule.glueDemandPlan.planDate"),
          prop: "planDate",
          span: 12,
          type: "date",
          valueFormat: "yyyy-MM-dd",
        },
        {
          label: this.$t("schedule.glueDemandPlan.factory"),
          prop: "factory",
          span: 12,
          type: "select", //FACTORY
          dictData: this.parentDict.type.FACTORY,
        },
        {
          label: this.$t("schedule.glueDemandPlan.glue"),
          prop: "glue",
          span: 12,
          maxlength: "30",
        },
        {
          label: this.$t("schedule.glueDemandPlan.mixArea"),
          prop: "mixArea",
          span: 12,
          type: "select", //  MIX_AREA
          dictData: this.parentDict.type.MIX_AREA,
        },
        {
          label: this.$t("schedule.glueDemandPlan.totalPlanQty"),
          prop: "totalPlanQty",
          span: 12,
          type: "number",
          max: 9999999,
          precision: 0,
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
          label: this.$t("schedule.glueDemandPlan.midPlanQty"),
          prop: "midPlanQty",
          span: 12,
          disabled: true,
          // type: "number",
          // max: 9999999,
          // precision: 0,
        },
        {
          label: this.$t("schedule.glueDemandPlan.midRemark"),
          prop: "midRemark",
          span: 12,
          // type: "textarea",
          maxlength: "300",
        },
        {
          label: this.$t("schedule.common.nightClass"),
          type: "title",
        },
        {
          label: this.$t("schedule.glueDemandPlan.nightPlanQty"),
          prop: "nightPlanQty",
          span: 12,
          disabled: true,
          // type: "number",
          // max: 9999999,
          // precision: 0,
        },
        {
          label: this.$t("schedule.glueDemandPlan.nightRemark"),
          prop: "nightRemark",
          span: 12,
          // type: "textarea",
          maxlength: "300",
        },

        {
          label: this.$t("schedule.common.dayClass"),
          type: "title",
        },
        {
          label: this.$t("schedule.glueDemandPlan.dayPlanQty"),
          prop: "dayPlanQty",
          span: 12,
          disabled: true,
          // type: "number",
          // max: 9999999,
          // precision: 0,
        },
        {
          label: this.$t("schedule.glueDemandPlan.dayRemark"),
          prop: "dayRemark",
          span: 12,
          // type: "textarea",
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

        let result = await editGlueDemandPlan(params);
        this.loading = false;
       if (result.code == 200) {
          this.$modal.msgSuccess("操作成功");
          this.$emit("success");
          this.hide();
        }
        this.loading = false;
      } catch (error) {
        console.error(error);
        this.loading = false;
      }
    },

    //utils
    show(data, editType) {
      this.visible = true;
      if (data) {
        this.isEdit = true;
        this.form = {
          ...data,
          dayPlanQty: numberEmpty(data.dayPlanQty),
          nightPlanQty: numberEmpty(data.nightPlanQty),
          midPlanQty: numberEmpty(data.midPlanQty),
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
    toUpperCase(e) {
      let value = e.target.value;
      if (value.length) {
        this.form.treadCode = value.toUpperCase();
      }
    },

    handleConfirm() {
      this.$refs.form.triggerConfirm((params) => {
        Object.keys(params).forEach((key) => {
          if (this.isEmpty(params[key])) {
            params[key] = "";
          }
        });
        this.save(params);
      });
    },
  },
};
</script>
