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
import { mapState } from "vuex";

import infoForm from "@/views/components/infoForm.vue";

import { validateAdd, editScheduleResult } from "@/api/gdyy/scheduleResult";

export default {
  components: { infoForm },
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
          label: this.$t("ui.data.column.scheduleResult.scheduleDate"),
          prop: "scheduleDate",
          span: 24,
          type: "date",
          valueFormat: "yyyy-MM-dd",
        },
        {
          label: this.$t("ui.data.column.gdyy.scheduleResult.bigRollCode"),
          prop: "bigRollCode",
          span: 24,
          maxlength: "20",
          listeners: {
            blur: this.toUpperCase,
          },
        },
        {
          label: this.$t("ui.data.column.scheduleResult.produceLine"),
          prop: "machineCode",
          span: 12,
          type: "select",
          dictData: this.machines,
          labelKey: "machineName",
          valueKey: "id",
        },
        {
          label: this.$t("ui.data.column.gdyy.scheduleResult.class2Plan"),
          prop: "class1Plan",
          span: 24,
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
        //   span: 24,
        //   type: "number",
        //   min: 0,
        //   max: 999999,
        //   precision: 0,
        // },
        // {
        //   label: this.$t("ui.data.column.gdyy.scheduleResult.class2Finish"),
        //   prop: "class1Finish",
        //   span: 24,
        //   type: "number",
        //   min: 0,
        //   max: 999999,
        //   precision: 0,
        // },
        {
          label: this.$t("ui.data.column.gdyy.scheduleResult.class2Remark"),
          prop: "class1Remark",
          span: 24,
          maxlength: "100",
        },
        {
          label: this.$t("ui.data.column.gdyy.scheduleResult.class1Plan"),
          prop: "class2Plan",
          span: 24,
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
        //   span: 24,
        //   type: "number",
        //   min: 0,
        //   max: 999999,
        //   precision: 0,
        // },
        // {
        //   label: this.$t("ui.data.column.gdyy.scheduleResult.class1Finish"),
        //   prop: "class2Finish",
        //   span: 24,
        //   type: "number",
        //   min: 0,
        //   max: 999999,
        //   precision: 0,
        // },
        {
          label: this.$t("ui.data.column.gdyy.scheduleResult.class1Remark"),
          prop: "class2Remark",
          span: 24,
          maxlength: "100",
        },
        // {
        //   label: this.$t("ui.data.column.gdyy.scheduleResult.class3Plan"),
        //   prop: "class3Plan",
        //   span: 24,
        // },
        // {
        //   label: this.$t(
        //     "ui.data.column.gdyy.scheduleResult.class3Plan.noStock"
        //   ),
        //   prop: "class3PlanNoStock",
        //   span: 24,
        // },
        // {
        //   label: this.$t("ui.data.column.gdyy.scheduleResult.class3Finish"),
        //   prop: "class3Finish",
        //   span: 24,
        //   maxlength: "100",
        // },
        // {
        //   label: this.$t("ui.data.column.gdyy.scheduleResult.class3Remark"),
        //   prop: "class3Remark",
        //   span: 24,
        //   maxlength: "100",
        // },
        {
          label: this.$t("中班计划量"),
          prop: "nightPlanQty",
          span: 24,
        },
        {
          label: this.$t("中班备注"),
          prop: "nightHandAnalysis",
          span: 24,
          maxlength: "100",
        },
        {
          label: this.$t("ui.common.column.remark"),
          prop: "class1PlanQty",
          span: 24,
          type: "textarea",
        },
      ];
    },
  },
  methods: {
    // api
    // api
    validateAdd(params) {
      return new Promise(async (resolve, reject) => {
        try {
          let valid = await validateAdd(params);
          if (valid.msg == "0") {
            this.$confirm(
              this.$t("ui.data.column.scheduleResult.isContinueAdd")
            )
              .then(async () => {
                resolve();
              })
              .catch((error) => {
                reject(error);
              });
          } else {
            resolve();
          }
        } catch (error) {
          reject(error);
        }
      });
    },

    async save(params) {
      try {
        this.loading = true;
        await this.validateAdd(params);
        let result = await editScheduleResult(params);
        this.loading = false;
        if (result.code == 200) {
          this.$modal.msgSuccess(result.msg);
          this.$emit("success");
          this.hide();
        }
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
        };
      } else {
        this.form = {
          scheduleDate: moment().add(1, "days").format("yyyy-MM-DD"),
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
