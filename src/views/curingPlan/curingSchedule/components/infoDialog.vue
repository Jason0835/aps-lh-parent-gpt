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

import { editScheduleResult, changeQty } from "@/api/lh/scheduleResult";

import infoForm from "@/views/components/infoForm.vue";
export default {
  components: { infoForm },
  inject: ["parentDict"],
  data() {
    return {
      loading: false,
      visible: false,
      isEdit: false,
      isChangeQty: false,
      form: {},
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
      plan1Disabled: false,
      plan2Disabled: false,
      plan4Disabled: false,
      plan5Disabled: false,
      plan1Disabled: false,
      nightDisabled: false,
      dayDisabled: false,
      // columns:
    };
  },
  computed: {
    ...mapState({
      curingMachines: (state) => state.curing.machines,
    }),
    title: function () {
      return (
        (this.isEdit
          ? this.$t("common.button.edit")
          : this.$t("common.button.add")) +
        this.$t("ui.data.column.lh.scheduleResult.modelName")
      );
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
                options={this.parentDict.type.IS_RELEASE}
              />
            );
          },
        },
        // {
        //   label: this.$t("ui.data.column.scheduleResult.sapCode"),
        //   prop: "productCode",
        //   disabled: true,
        //   span: 12,
        // },
        // {
        //   label: this.$t("ui.data.column.scheduleResult.specCode"),
        //   prop: "specCode",
        //   disabled: true,
        //   span: 12,
        // },
        {
          label: this.$t("ui.data.column.scheduleResult.dailyPlanQty"),
          prop: "dailyPlanQty",
          disabled: true,
          span: 12,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.lhMachineCode"),
          prop: "lhMachineName",
          span: 12,
          disabled: true,
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
          // disabled: this.isChangeQty ? false : true,
          disabled: true,
          span: 12,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.adjustType"),
          prop: "operType",
          span: 12,
          disabled: this.isChangeQty ? false : true,
          type: "select",
          dictData: this.parentDict.type.adjust_type,
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
                <span>
                  {this.$t("一班")}
                </span>
              </div>
            );
          },
        },
        {
          label: this.$t("一班计划"),
          prop: "class1PlanQty",
          disabled: this.isChangeQty ? this.plan1Disabled : true,
          span: 12,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class1FinishQty.lh"),
          prop: "class1FinishQty",
          disabled: true,
          span: 12,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class1Analysis.lh"),
          prop: "class1Analysis",
          // disabled: true,
          span: 12,
        },
        // {
        //   label: this.$t("ui.data.column.scheduleResult.class1Analysis.lh"),
        //   prop: "class1AnalysisInput",
        //   span: 12,
        // },
        {
          render: () => {
            return (
              <div class="line-header">
                <span>
                  {this.$t("二班")}
                </span>
              </div>
            );
          },
        },
        {
          label: this.$t("二班计划量"),
          prop: "class2PlanQty",
          disabled: this.isChangeQty ? this.plan2Disabled : true,
          span: 12,
        },
        {
          label: this.$t("二班完成量"),
          prop: "class2FinishQty",
          disabled: true,
          span: 12,
        },
        {
          label: this.$t("二班计备注"),
          prop: "class2Analysis",
          // disabled: true,
          span: 12,
        },
        {
          render: () => {
            return (
              <div class="line-header">
                <span>
                  {this.$t("三班")}
                </span>
              </div>
            );
          },
        },
        {
          label: this.$t("三班计划量"),
          prop: "class3PlanQty",
          disabled: this.isChangeQty ? this.plan2Disabled : true,
          span: 12,
        },
        {
          label: this.$t("三班完成量"),
          prop: "class3FinishQty",
          disabled: true,
          span: 12,
        },
        {
          label: this.$t("三班计备注"),
          prop: "class3Analysis",
          // disabled: true,
          span: 12,
        },
        {
          render: () => {
            return (
              <div class="line-header">
                <span>
                  {this.$t("四班")}
                </span>
              </div>
            );
          },
        },
        {
          label: this.$t("四班计划量"),
          prop: "class4PlanQty",
          disabled: this.isChangeQty ? this.plan4Disabled : true,
          span: 12,
        },
        {
          label: this.$t("四班完成量"),
          prop: "class4FinishQty",
          disabled: true,
          span: 12,
        },
        {
          label: this.$t("四班备注"),
          prop: "class4Analysis",
          // disabled: true,
          span: 12,
        },
        // {
        //   label: this.$t("ui.data.column.scheduleResult.class4Analysis.lh"),
        //   prop: "class4AnalysisInput",
        //   span: 12,
        // },
        {
          render: () => {
            return (
              <div class="line-header">
                <span>
                  {this.$t("五班")}
                </span>
              </div>
            );
          },
        },
        {
          label: this.$t("五班计划量"),
          prop: "class5PlanQty",
          disabled: this.isChangeQty ? this.plan5Disabled : true,
          span: 12,
        },
        {
          label: this.$t("五班完成量"),
          prop: "class5FinishQty",
          disabled: true,
          span: 12,
        },
        {
          label: this.$t("五班备注"),
          prop: "class5Analysis",
          // disabled: true,
          span: 12,
        },
        {
          render: () => {
            return (
              <div class="line-header">
                <span>
                  {this.$t("六班")}
                </span>
              </div>
            );
          },
        },
        {
          label: this.$t("六班计划量"),
          prop: "class6PlanQty",
          disabled: this.isChangeQty ? this.plan5Disabled : true,
          span: 12,
        },
        {
          label: this.$t("六班完成量"),
          prop: "class6FinishQty",
          disabled: true,
          span: 12,
        },
        {
          label: this.$t("六班备注"),
          prop: "class6Analysis",
          // disabled: true,
          span: 12,
        },
        {
          render: () => {
            return (
              <div class="line-header">
                <span>
                  {this.$t("七班")}
                </span>
              </div>
            );
          },
        },
        {
          label: this.$t("七班计划量"),
          prop: "class7PlanQty",
          disabled: this.isChangeQty ? this.plan5Disabled : true,
          span: 12,
        },
        {
          label: this.$t("七班完成量"),
          prop: "class7FinishQty",
          disabled: true,
          span: 12,
        },
        {
          label: this.$t("七班备注"),
          prop: "class7Analysis",
          // disabled: true,
          span: 12,
        },
        {
          render: () => {
            return (
              <div class="line-header">
                <span>
                  {this.$t("八班")}
                </span>
              </div>
            );
          },
        },
        {
          label: this.$t("八班计划量"),
          prop: "class8PlanQty",
          disabled: this.isChangeQty ? this.plan5Disabled : true,
          span: 12,
        },
        {
          label: this.$t("八班完成量"),
          prop: "class8FinishQty",
          disabled: true,
          span: 12,
        },
        {
          label: this.$t("八班备注"),
          prop: "class8Analysis",
          // disabled: true,
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
        this.form = {
          ...data,
        };

        if (data.scheduleDate) {
          if (moment().isAfter(data.scheduleDate + " 19:00:00")) {
            this.plan1Disabled = true;
          }
          if (
            moment().isAfter(
              moment(data.scheduleDate).add(1, 'days"').format("yyyy-MM-dd") +
                " 07:00:00"
            )
          ) {
            this.plan2Disabled = true;
          }
          if (
            moment().isAfter(
              moment(data.scheduleDate).add(1, 'days"').format("yyyy-MM-dd") +
                " 19:00:00"
            )
          ) {
            this.plan4Disabled = true;
          }
          if (
            moment().isAfter(
              moment(data.scheduleDate).add(2, 'days"').format("yyyy-MM-dd") +
                " 07:00:00"
            )
          ) {
            this.plan5Disabled = true;
          }
        } else {
          this.plan1Disabled = true;
          this.plan2Disabled = true;
          this.plan4Disabled = true;
          this.plan5Disabled = true;
        }
      }
    },
    hide() {
      this.form = {};
      this.$refs.form.triggerResetForm();
      // this.resetForm("infoForm");
      this.isEdit = false;
      this.isChangeQty = false;
      this.visible = false;
      this.plan1Disabled = false;
      this.plan2Disabled = false;
      this.plan4Disabled = false;
      this.plan5Disabled = false;
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
