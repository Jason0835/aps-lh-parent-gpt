<template>
  <el-dialog
    :title="title"
    :visible="visible"
    width="1100px"
    @close="hide"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :append-to-body="true"
  >
    <div class="curing-schedule-info-content" v-loading="loading">
      <info-form
        class="form-item-height"
        ref="form"
        :form="form"
        :rules="rules"
        :columns="columns"
        label-position="right"
        label-width="120px"
      >
      </info-form>
    </div>
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

import { editScheduleResult, changeQty, getScheduleDate } from "@/api/lh/scheduleResult";

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
      dateList: [
        { shift: 1, shiftDate: "" },
        { shift: 2, shiftDate: "" },
        { shift: 3, shiftDate: "" },
        { shift: 4, shiftDate: "" },
        { shift: 5, shiftDate: "" },
        { shift: 6, shiftDate: "" },
        { shift: 7, shiftDate: "" },
        { shift: 8, shiftDate: "" },
      ],
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
          type: "title",
          label: this.$t("ui.data.column.scheduleResult.baseInfo"),
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
          label: this.$t("ui.data.column.scheduleResult.materialDesc"),
          prop: "materialDesc",
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
          type: "title",
          label: this.shiftBannerTitle(1),
        },
        {
          label: this.lhShiftInlineFieldLabel(1, "planQty"),
          prop: "class1PlanQty",
          disabled: this.isChangeQty ? this.plan1Disabled : true,
          span: 12,
        },
        {
          label: this.lhShiftInlineFieldLabel(1, "finishQty"),
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
          label: this.lhShiftInlineFieldLabel(1, "analysis"),
          prop: "class1Analysis",
          type: "textarea",
          maxlength: 200,
          "show-word-limit": true,
        },
        {
          type: "title",
          label: this.shiftBannerTitle(2),
        },
        {
          label: this.lhShiftInlineFieldLabel(2, "planQty"),
          prop: "class2PlanQty",
          disabled: this.isChangeQty ? this.plan2Disabled : true,
          span: 12,
        },
        {
          label: this.lhShiftInlineFieldLabel(2, "finishQty"),
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
          label: this.lhShiftInlineFieldLabel(2, "analysis"),
          prop: "class2Analysis",
          type: "textarea",
          maxlength: 200,
          "show-word-limit": true,
        },
        {
          type: "title",
          label: this.shiftBannerTitle(3),
        },
        {
          label: this.lhShiftInlineFieldLabel(3, "planQty"),
          prop: "class3PlanQty",
          disabled: this.isChangeQty ? this.plan2Disabled : true,
          span: 12,
        },
        {
          label: this.lhShiftInlineFieldLabel(3, "finishQty"),
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
          label: this.lhShiftInlineFieldLabel(3, "analysis"),
          prop: "class3Analysis",
          type: "textarea",
          maxlength: 200,
          "show-word-limit": true,
        },
        {
          type: "title",
          label: this.shiftBannerTitle(4),
        },
        {
          label: this.lhShiftInlineFieldLabel(4, "planQty"),
          prop: "class4PlanQty",
          disabled: this.isChangeQty ? this.plan4Disabled : true,
          span: 12,
        },
        {
          label: this.lhShiftInlineFieldLabel(4, "finishQty"),
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
          label: this.lhShiftInlineFieldLabel(4, "analysis"),
          prop: "class4Analysis",
          type: "textarea",
          maxlength: 200,
          "show-word-limit": true,
        },
        {
          type: "title",
          label: this.shiftBannerTitle(5),
        },
        {
          label: this.lhShiftInlineFieldLabel(5, "planQty"),
          prop: "class5PlanQty",
          disabled: this.isChangeQty ? this.plan5Disabled : true,
          span: 12,
        },
        {
          label: this.lhShiftInlineFieldLabel(5, "finishQty"),
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
          label: this.lhShiftInlineFieldLabel(5, "analysis"),
          prop: "class5Analysis",
          type: "textarea",
          maxlength: 200,
          "show-word-limit": true,
        },
        {
          type: "title",
          label: this.shiftBannerTitle(6),
        },
        {
          label: this.lhShiftInlineFieldLabel(6, "planQty"),
          prop: "class6PlanQty",
          disabled: this.isChangeQty ? this.plan5Disabled : true,
          span: 12,
        },
        {
          label: this.lhShiftInlineFieldLabel(6, "finishQty"),
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
          label: this.lhShiftInlineFieldLabel(6, "analysis"),
          prop: "class6Analysis",
          type: "textarea",
          maxlength: 200,
          "show-word-limit": true,
        },
        {
          type: "title",
          label: this.shiftBannerTitle(7),
        },
        {
          label: this.lhShiftInlineFieldLabel(7, "planQty"),
          prop: "class7PlanQty",
          disabled: this.isChangeQty ? this.plan5Disabled : true,
          span: 12,
        },
        {
          label: this.lhShiftInlineFieldLabel(7, "finishQty"),
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
          label: this.lhShiftInlineFieldLabel(7, "analysis"),
          prop: "class7Analysis",
          type: "textarea",
          maxlength: 200,
          "show-word-limit": true,
        },
        {
          type: "title",
          label: this.shiftBannerTitle(8),
        },
        {
          label: this.lhShiftInlineFieldLabel(8, "planQty"),
          prop: "class8PlanQty",
          disabled: this.isChangeQty ? this.plan5Disabled : true,
          span: 12,
        },
        {
          label: this.lhShiftInlineFieldLabel(8, "finishQty"),
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
          label: this.lhShiftInlineFieldLabel(8, "analysis"),
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
    /** class1～8 与列表同级：对应早/中/夜（无日期后缀） */
    shiftPeriodShortKey(classIndex) {
      const KEYS = [, "morningShift", "middleShift", "nightShift", "morningShift", "middleShift", "nightShift", "morningShift", "middleShift"];
      return KEYS[classIndex];
    },
    shiftPeriodNameOnly(classIndex) {
      return this.$t(`ui.data.column.scheduleResult.${this.shiftPeriodShortKey(classIndex)}`);
    },
    lhShiftInlineFieldLabel(classIndex, suffixKey) {
      const shiftName = this.shiftPeriodNameOnly(classIndex);
      const map = {
        planQty: "ui.data.column.scheduleResult.lhDialogShiftPlanQty",
        finishQty: "ui.data.column.scheduleResult.lhDialogShiftFinishQty",
        analysis: "ui.data.column.scheduleResult.lhDialogShiftAnalysis",
      };
      return this.$t(map[suffixKey], { shift: shiftName });
    },
    /** 与列表页 curingSchedule/index 班次分组一致：早/中/夜 + listScheduleShiftDates 返回日期 */
    shiftBannerTitle(classIndex) {
      const i = classIndex - 1;
      const dateStr = this.dateList[i]?.shiftDate ?? "";
      const label = this.shiftPeriodNameOnly(classIndex);
      return dateStr ? `${label} ${dateStr}` : label;
    },
    async fetchScheduleShiftDates(scheduleDate) {
      const empty = [
        { shift: 1, shiftDate: "" },
        { shift: 2, shiftDate: "" },
        { shift: 3, shiftDate: "" },
        { shift: 4, shiftDate: "" },
        { shift: 5, shiftDate: "" },
        { shift: 6, shiftDate: "" },
        { shift: 7, shiftDate: "" },
        { shift: 8, shiftDate: "" },
      ];
      if (!scheduleDate) {
        this.dateList = empty;
        return;
      }
      try {
        const res = await getScheduleDate({ scheduleDate });
        if (Array.isArray(res) && res.length) {
          this.dateList = res;
        } else {
          this.dateList = empty;
        }
      } catch (error) {
        console.error(error);
        this.dateList = empty;
      }
    },
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
        this.fetchScheduleShiftDates(data.scheduleDate);

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
      this.dateList = [
        { shift: 1, shiftDate: "" },
        { shift: 2, shiftDate: "" },
        { shift: 3, shiftDate: "" },
        { shift: 4, shiftDate: "" },
        { shift: 5, shiftDate: "" },
        { shift: 6, shiftDate: "" },
        { shift: 7, shiftDate: "" },
        { shift: 8, shiftDate: "" },
      ];
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
<style scoped lang="scss">
/** 对齐 moldingSchedule/changePlanDialog：可滚动内容区 */
.curing-schedule-info-content {
  width: 100%;
  max-height: 70vh;
  overflow: auto;
}
</style>
