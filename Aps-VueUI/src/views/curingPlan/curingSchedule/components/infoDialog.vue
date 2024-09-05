<template>
  <el-dialog
    :title="title"
    :visible="visible"
    width="1000px"
    @close="hide"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
  >
    <info-form
      class="form-item-height"
      ref="form"
      :defaultValue="defaultValue"
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
import infoForm from "@/views/components/infoForm.vue";
import {  editScheduleResult,changeQty } from "@/api/lh/scheduleResult";
import CuringMachineSelect from "@/views/components/CuringMachineSelect.vue";
export default {
  components: { infoForm, CuringMachineSelect },
  data() {
    return {
      loading: false,
      visible: false,
      isEdit: false,
      isChangeQty: false,
      defaultValue: {},
      rules: {
        scheduleDate: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "blur",
          },
        ],
        lhMachineCode: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        sapCode: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
      },
      // columns:
    };
  },
  computed: {
    title: function () {
      return this.$t("ui.data.column.lh.scheduleResult.modelName");
    },
    columns() {
      let tempColumns = [
        {
          render: () => {
            return (
              <div class="line-header">
                <span>{this.$t("ui.data.column.scheduleResult.baseInfo")}</span>
              </div>
            );
          },
        },
        {
          label: this.$t("ui.data.column.scheduleResult.scheduleDate"),
          prop: "scheduleDate",
          type: "date",
          dateType: "date",
          valueFormat: "yyyy-MM-dd",
          disabled: true,
          span: 12,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.isRelease"),
          prop: "isRelease",
          disabled: true,
          span: 12,
          render: (form) => {
            return (
              <dict-select
                v-model={form.isRelease}
                disabled
                // options={this.dict.type.IS_RELEASE}
              />
            );
          },
        },
        {
          label: this.$t("ui.data.column.scheduleResult.sapCode"),
          prop: "sapCode",
          disabled: true,
          span: 12,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.model"),
          prop: "specDesc",
          disabled: true,
          span: 12,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.dailyPlanQty"),
          prop: "dailyPlanQty",
          disabled: true,
          span: 12,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.lhMachineCode"),
          prop: "lhMachineCode",
          span: 12,
          render: (form) => {
            return (
              <CuringMachineSelect
                v-model={form.lhMachineCode}
                label={form.lhMachineName}
                disabled
              />
            );
          },
        },
        {
          label: this.$t("ui.data.column.scheduleResult.lhTime"),
          prop: "lhTime",
          disabled: true,
          span: 12,
        },
        this.isChangeQty
          ? {}
          : {
              label: this.$t("ui.data.column.scheduleResult.leftRightMold"),
              prop: "leftRightMold",
              span: 12,
            },
        {
          label: this.$t("ui.data.column.scheduleResult.embryoCode"),
          prop: "embryoCode",
          disabled: this.isChangeQty ? true : false,
          span: 12,
        },
        {
          label: this.$t("ui.common.column.remark"),
          prop: "remark",
          type: "textarea",
        },
        {
          render: () => {
            return (
              <div class="line-header">
                <span>{this.$t("ui.data.column.scheduleResult.class1")}</span>
              </div>
            );
          },
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class1PlanQty"),
          prop: "class1PlanQty",
          disabled: this.isChangeQty ? false : true,
          span: 12,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class1FinishQty"),
          prop: "class1FinishQty",
          disabled: true,
          span: 12,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.analySystem"),
          prop: "class1Analysis",
          disabled: true,
          span: 12,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class1Analysis"),
          prop: "class1AnalysisInput",
          span: 12,
        },
        {
          render: () => {
            return (
              <div class="line-header">
                <span>{this.$t("ui.data.column.scheduleResult.class2")}</span>
              </div>
            );
          },
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class2PlanQty"),
          prop: "class2PlanQty",
          disabled: this.isChangeQty ? false : true,
          span: 12,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class2FinishQty"),
          prop: "class2FinishQty",
          disabled: true,
          span: 12,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.analySystem"),
          prop: "class2Analysis",
          disabled: true,
          span: 12,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class2Analysis"),
          prop: "class2AnalysisInput",
          span: 12,
        },
        {
          render: () => {
            return (
              <div class="line-header">
                <span>{this.$t("ui.data.column.scheduleResult.class3")}</span>
              </div>
            );
          },
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class3PlanQty"),
          prop: "class3PlanQty",
          disabled: this.isChangeQty ? false : true,
          span: 12,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class3FinishQty"),
          prop: "class3FinishQty",
          disabled: true,
          span: 12,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.analySystem"),
          prop: "class3Analysis",
          disabled: true,
          span: 12,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class3Analysis"),
          prop: "class3AnalysisInput",
          span: 12,
        },
      ];
      return tempColumns;
    },
  },
  methods: {
    // api
    async save(params) {
      // console.log(params);
      try {
        this.loading = true;
        if (this.isChangeQty) {
          const data = await changeQty({
            ...params,
          });
          this.$modal.msgSuccess(data.msg);
        } else {
          const data = await editScheduleResult({
            ...params,
          });
          this.$modal.msgSuccess(data.msg);
        }

        this.$emit("success");
        this.hide();
      } catch (error) {
        console.log(error);
      } finally {
        this.loading = false;
      }
    },

    //utils
    show(data, isChangeQty) {
      this.visible = true;
      this.isChangeQty = isChangeQty;
      if (data) {
        this.isEdit = true;
        this.defaultValue = {
          ...data,
        };
      }
    },
    hide() {
      this.defaultValue = {};
      this.$refs.form.triggerResetForm();
      // this.resetForm("infoForm");
      this.isEdit = false;
      (this.isChangeQty = false), (this.visible = false);
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
<style scoped>
.line-header {
  border-bottom: 1px solid #dcdfe6;
  padding-left: 15px;
  padding-bottom: 15px;
  margin-bottom: 15px;
  font-size: 15px;
}
</style>
