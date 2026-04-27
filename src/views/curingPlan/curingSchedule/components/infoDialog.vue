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
                options={this.parentDict.type.IS_RELEASE_LH}
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
          label: this.$t("ui.data.column.scheduleResult.totalSurplusQty"),
          prop: "mouldSurplusQty",
          disabled: true,
          span: 12,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.lhMachineCode"),
          prop: "lhMachineCode",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.lhTime"),
          prop: "lhTime",
          disabled: true,
          span: 12,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.materialCode"),
          prop: "materialCode",
          disabled: true,
          span: 12,
        },
        {
          label: this.$t("ui.common.column.remark"),
          prop: "remark",
          type: "textarea",
          maxlength: 300,
          "show-word-limit": true,
        },
        {
          render: () => {
            return (
              <div class="line-header">
                <span>
                  {this.$t("ui.data.column.scheduleResult.class11.lh")}
                </span>
              </div>
            );
          },
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class1PlanQty"),
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
          label: this.$t("ui.data.column.scheduleResult.leftRightMould"),
          prop: "leftRightMould",
          disabled: true,
          span: 12,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.constructionStage"),
          prop: "constructionStage",
          disabled: true,
          span: 12,
          render: (form) => {
            return (
              <dict-select
                v-model={form.constructionStage}
                disabled
                options={this.parentDict.type.biz_construction_stage}
              />
            );
          },
        },
        {
          label: this.$t("ui.data.column.scheduleResult.type"),
          prop: "class1IsEnd",
          disabled: true,
          span: 12,
          render: (form) => {
            return (
              <dict-select
                v-model={form.class1IsEnd}
                disabled
                options={this.parentDict.type.biz_end_type}
              />
            );
          },
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class1Analysis.lh"),
          prop: "class1Analysis",
          type: "textarea",
          maxlength: 200,
          "show-word-limit": true,
        },
        {
          render: () => {
            return (
              <div class="line-header">
                <span>
                  {this.$t("ui.data.column.scheduleResult.class22.lh")}
                </span>
              </div>
            );
          },
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class2PlanQty"),
          prop: "class2PlanQty",
          disabled: this.isChangeQty ? this.plan2Disabled : true,
          span: 12,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class2FinishQty"),
          prop: "class2FinishQty",
          disabled: true,
          span: 12,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.leftRightMould"),
          prop: "leftRightMould",
          disabled: true,
          span: 12,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.constructionStage"),
          prop: "constructionStage",
          disabled: true,
          span: 12,
          render: (form) => {
            return (
              <dict-select
                v-model={form.constructionStage}
                disabled
                options={this.parentDict.type.biz_construction_stage}
              />
            );
          },
        },
        {
          label: this.$t("ui.data.column.scheduleResult.type"),
          prop: "class2IsEnd",
          disabled: true,
          span: 12,
          render: (form) => {
            return (
              <dict-select
                v-model={form.class2IsEnd}
                disabled
                options={this.parentDict.type.biz_end_type}
              />
            );
          },
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class2Analysis"),
          prop: "class2Analysis",
          type: "textarea",
          maxlength: 200,
          "show-word-limit": true,
        },
        {
          render: () => {
            return (
              <div class="line-header">
                <span>
                  {this.$t("ui.data.column.scheduleResult.class33.lh")}
                </span>
              </div>
            );
          },
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class3PlanQty"),
          prop: "class3PlanQty",
          disabled: this.isChangeQty ? this.plan2Disabled : true,
          span: 12,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class3FinishQty"),
          prop: "class3FinishQty",
          disabled: true,
          span: 12,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.leftRightMould"),
          prop: "leftRightMould",
          disabled: true,
          span: 12,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.constructionStage"),
          prop: "constructionStage",
          disabled: true,
          span: 12,
          render: (form) => {
            return (
              <dict-select
                v-model={form.constructionStage}
                disabled
                options={this.parentDict.type.biz_construction_stage}
              />
            );
          },
        },
        {
          label: this.$t("ui.data.column.scheduleResult.type"),
          prop: "class3IsEnd",
          disabled: true,
          span: 12,
          render: (form) => {
            return (
              <dict-select
                v-model={form.class3IsEnd}
                disabled
                options={this.parentDict.type.biz_end_type}
              />
            );
          },
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class3Analysis"),
          prop: "class3Analysis",
          type: "textarea",
          maxlength: 200,
          "show-word-limit": true,
        },
        {
          render: () => {
            return (
              <div class="line-header">
                <span>
                  {this.$t("ui.data.column.scheduleResult.class44.lh")}
                </span>
              </div>
            );
          },
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class4PlanQty"),
          prop: "class4PlanQty",
          disabled: this.isChangeQty ? this.plan4Disabled : true,
          span: 12,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class4FinishQty"),
          prop: "class4FinishQty",
          disabled: true,
          span: 12,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.leftRightMould"),
          prop: "leftRightMould",
          disabled: true,
          span: 12,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.constructionStage"),
          prop: "constructionStage",
          disabled: true,
          span: 12,
          render: (form) => {
            return (
              <dict-select
                v-model={form.constructionStage}
                disabled
                options={this.parentDict.type.biz_construction_stage}
              />
            );
          },
        },
        {
          label: this.$t("ui.data.column.scheduleResult.type"),
          prop: "class4IsEnd",
          disabled: true,
          span: 12,
          render: (form) => {
            return (
              <dict-select
                v-model={form.class4IsEnd}
                disabled
                options={this.parentDict.type.biz_end_type}
              />
            );
          },
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class4Analysis"),
          prop: "class4Analysis",
          type: "textarea",
          maxlength: 200,
          "show-word-limit": true,
        },
        {
          render: () => {
            return (
              <div class="line-header">
                <span>
                  {this.$t("ui.data.column.scheduleResult.class55.lh")}
                </span>
              </div>
            );
          },
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class5PlanQty"),
          prop: "class5PlanQty",
          disabled: this.isChangeQty ? this.plan5Disabled : true,
          span: 12,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class5FinishQty"),
          prop: "class5FinishQty",
          disabled: true,
          span: 12,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.leftRightMould"),
          prop: "leftRightMould",
          disabled: true,
          span: 12,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.constructionStage"),
          prop: "constructionStage",
          disabled: true,
          span: 12,
          render: (form) => {
            return (
              <dict-select
                v-model={form.constructionStage}
                disabled
                options={this.parentDict.type.biz_construction_stage}
              />
            );
          },
        },
        {
          label: this.$t("ui.data.column.scheduleResult.type"),
          prop: "class5IsEnd",
          disabled: true,
          span: 12,
          render: (form) => {
            return (
              <dict-select
                v-model={form.class5IsEnd}
                disabled
                options={this.parentDict.type.biz_end_type}
              />
            );
          },
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class5Analysis"),
          prop: "class5Analysis",
          type: "textarea",
          maxlength: 200,
          "show-word-limit": true,
        },
        {
          render: () => {
            return (
              <div class="line-header">
                <span>
                  {this.$t("ui.data.column.scheduleResult.class66.lh")}
                </span>
              </div>
            );
          },
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class6PlanQty"),
          prop: "class6PlanQty",
          disabled: this.isChangeQty ? this.plan5Disabled : true,
          span: 12,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class6FinishQty"),
          prop: "class6FinishQty",
          disabled: true,
          span: 12,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.leftRightMould"),
          prop: "leftRightMould",
          disabled: true,
          span: 12,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.constructionStage"),
          prop: "constructionStage",
          disabled: true,
          span: 12,
          render: (form) => {
            return (
              <dict-select
                v-model={form.constructionStage}
                disabled
                options={this.parentDict.type.biz_construction_stage}
              />
            );
          },
        },
        {
          label: this.$t("ui.data.column.scheduleResult.type"),
          prop: "class6IsEnd",
          disabled: true,
          span: 12,
          render: (form) => {
            return (
              <dict-select
                v-model={form.class6IsEnd}
                disabled
                options={this.parentDict.type.biz_end_type}
              />
            );
          },
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class6Analysis"),
          prop: "class6Analysis",
          type: "textarea",
          maxlength: 200,
          "show-word-limit": true,
        },
        {
          render: () => {
            return (
              <div class="line-header">
                <span>
                  {this.$t("ui.data.column.scheduleResult.class77.lh")}
                </span>
              </div>
            );
          },
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class7PlanQty"),
          prop: "class7PlanQty",
          disabled: this.isChangeQty ? this.plan5Disabled : true,
          span: 12,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class7FinishQty"),
          prop: "class7FinishQty",
          disabled: true,
          span: 12,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.leftRightMould"),
          prop: "leftRightMould",
          disabled: true,
          span: 12,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.constructionStage"),
          prop: "constructionStage",
          disabled: true,
          span: 12,
          render: (form) => {
            return (
              <dict-select
                v-model={form.constructionStage}
                disabled
                options={this.parentDict.type.biz_construction_stage}
              />
            );
          },
        },
        {
          label: this.$t("ui.data.column.scheduleResult.type"),
          prop: "class7IsEnd",
          disabled: true,
          span: 12,
          render: (form) => {
            return (
              <dict-select
                v-model={form.class7IsEnd}
                disabled
                options={this.parentDict.type.biz_end_type}
              />
            );
          },
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class7Analysis"),
          prop: "class7Analysis",
          type: "textarea",
          maxlength: 200,
          "show-word-limit": true,
        },
        {
          render: () => {
            return (
              <div class="line-header">
                <span>
                  {this.$t("ui.data.column.scheduleResult.class88.lh")}
                </span>
              </div>
            );
          },
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class8PlanQty"),
          prop: "class8PlanQty",
          disabled: this.isChangeQty ? this.plan5Disabled : true,
          span: 12,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class8FinishQty"),
          prop: "class8FinishQty",
          disabled: true,
          span: 12,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.leftRightMould"),
          prop: "leftRightMould",
          disabled: true,
          span: 12,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.constructionStage"),
          prop: "constructionStage",
          disabled: true,
          span: 12,
          render: (form) => {
            return (
              <dict-select
                v-model={form.constructionStage}
                disabled
                options={this.parentDict.type.biz_construction_stage}
              />
            );
          },
        },
        {
          label: this.$t("ui.data.column.scheduleResult.type"),
          prop: "class8IsEnd",
          disabled: true,
          span: 12,
          render: (form) => {
            return (
              <dict-select
                v-model={form.class8IsEnd}
                disabled
                options={this.parentDict.type.biz_end_type}
              />
            );
          },
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class8Analysis"),
          prop: "class8Analysis",
          type: "textarea",
          maxlength: 200,
          "show-word-limit": true,
        },

      ];
      return tempColumns;
    },
  },
  methods: {
    calcShiftIsEndFields(data) {
      const referenceQty = Math.max(data.mouldSurplusQty || 0, data.embryoStock || 0);
      for (let i = 1; i <= 8; i++) {
        const planQty = data['class' + i + 'PlanQty'];
        if (planQty == null || planQty <= 0 || referenceQty <= 0) {
          this.$set(this.form, 'class' + i + 'IsEnd', '0');
          continue;
        }
        let totalPlanQty = 0;
        for (let j = 1; j <= 8; j++) {
          totalPlanQty += (data['class' + j + 'PlanQty'] || 0);
        }
        if (totalPlanQty < referenceQty) {
          this.$set(this.form, 'class' + i + 'IsEnd', '0');
          continue;
        }
        let remaining = referenceQty;
        let isEndShift = false;
        for (let j = 1; j <= 8; j++) {
          remaining -= (data['class' + j + 'PlanQty'] || 0);
          if (remaining <= 0) {
            if (j === i) {
              isEndShift = true;
            }
            break;
          }
        }
        this.$set(this.form, 'class' + i + 'IsEnd', isEndShift ? '1' : '0');
      }
    },
    encodeRemark(remark) {
      if (!remark) return remark;
      return remark
        .replace(/%/g, '__PERCENT__')
        .replace(/&/g, '__AMP__')
        .replace(/</g, '__LT__')
        .replace(/>/g, '__GT__')
        .replace(/"/g, '__QUOT__')
        .replace(/'/g, '__APOS__');
    },
    decodeRemark(remark) {
      if (!remark) return remark;
      return remark
        .replace(/__PERCENT__/g, '%')
        .replace(/__AMP__/g, '&')
        .replace(/__LT__/g, '<')
        .replace(/__GT__/g, '>')
        .replace(/__QUOT__/g, '"')
        .replace(/__APOS__/g, "'");
    },
    async save(params) {
      try {
        this.loading = true;
        // 处理完成量：值不大于0时不带入保存
        const finishQtyFields = ['class1FinishQty', 'class2FinishQty', 'class3FinishQty', 'class4FinishQty',
          'class5FinishQty', 'class6FinishQty', 'class7FinishQty', 'class8FinishQty'];
        const saveParams = {
          ...params,
          remark: this.encodeRemark(params.remark),
          class1Analysis: this.encodeRemark(params.class1Analysis),
          class2Analysis: this.encodeRemark(params.class2Analysis),
          class3Analysis: this.encodeRemark(params.class3Analysis),
          class4Analysis: this.encodeRemark(params.class4Analysis),
          class5Analysis: this.encodeRemark(params.class5Analysis),
          class6Analysis: this.encodeRemark(params.class6Analysis),
          class7Analysis: this.encodeRemark(params.class7Analysis),
          class8Analysis: this.encodeRemark(params.class8Analysis),
        };
        // 移除不大于0的完成量字段
        finishQtyFields.forEach(field => {
          if (saveParams[field] == null || saveParams[field] <= 0) {
            delete saveParams[field];
          }
        });
        let data;
        if (this.isChangeQty) {
          data = await changeQty(saveParams);
        } else {
          data = await editScheduleResult(saveParams);
        }

        this.$emit("success");
        this.hide();
        this.$modal.msgSuccess(data.msg || "保存成功");
      } catch (error) {
        console.error("保存失败:", error);
        this.$modal.msgError(error.message || "保存失败");
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
          remark: this.decodeRemark(data.remark),
          class1Analysis: this.decodeRemark(data.class1Analysis),
          class2Analysis: this.decodeRemark(data.class2Analysis),
          class3Analysis: this.decodeRemark(data.class3Analysis),
          class4Analysis: this.decodeRemark(data.class4Analysis),
          class5Analysis: this.decodeRemark(data.class5Analysis),
          class6Analysis: this.decodeRemark(data.class6Analysis),
          class7Analysis: this.decodeRemark(data.class7Analysis),
          class8Analysis: this.decodeRemark(data.class8Analysis),
          class1FinishQty: data.class1FinishQty == null ? 0 : data.class1FinishQty,
          class2FinishQty: data.class2FinishQty == null ? 0 : data.class2FinishQty,
          class3FinishQty: data.class3FinishQty == null ? 0 : data.class3FinishQty,
          class4FinishQty: data.class4FinishQty == null ? 0 : data.class4FinishQty,
          class5FinishQty: data.class5FinishQty == null ? 0 : data.class5FinishQty,
          class6FinishQty: data.class6FinishQty == null ? 0 : data.class6FinishQty,
          class7FinishQty: data.class7FinishQty == null ? 0 : data.class7FinishQty,
          class8FinishQty: data.class8FinishQty == null ? 0 : data.class8FinishQty,
        };
        this.calcShiftIsEndFields(data);

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
