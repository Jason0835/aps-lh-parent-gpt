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

import { editScheduleResult, getWorkClass } from "@/api/dj/djScheduleResult.js";

export default {
  components: { infoForm },
  inject: ["parentDict"],
  data() {
    return {
      loading: false,
      visible: false,
      isEdit: false,
      classHeaders: [],
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
    };
  },
  computed: {
    ...mapState({
      machines: (state) => state.insideLiner.machines,
    }),
    title: function () {
      return this.$t("ui.data.column.djScheduleResult.modalName");
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
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.dj.scheduleResult.paddingName"),
          prop: "paddingName",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.dj.scheduleResult.paddingCode"),
          prop: "paddingCode",
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
          label: this.$t("ui.data.column.scheduleResult.isRelease"),
          prop: "isRelease",
          span: 12,
          type: "select",
          disabled: true,
          dictData: this.parentDict.type.IS_RELEASE,
        },
        {
          label: this.$t("ui.data.column.dj.scheduleResult.machineCode"),
          prop: "machineCode",
          span: 12,
          type: "select",
          disabled: true,
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

        // ============ 中班（class1） ============
        {
          label: this.classHeaders[1],
          type: "title",
        },
        {
          label: this.$t("ui.data.column.dj.scheduleResult.planQty"),
          prop: "class1PlanQty",
          span: 12,
          type: "number",
          min: 0,
          max: 999999,
          precision: 0,
        },
        {
          label: this.$t("ui.data.column.dj.scheduleResult.finishQty"),
          prop: "class1FinishQty",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.finish"),
          prop: "class1FinishRate",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.dj.scheduleResult.sequence"),
          prop: "class1Sequence",
          span: 12,
          type: "number",
          min: 0,
          max: 999999,
          precision: 0,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class1Analysis"),
          prop: "class1Analysis",
          span: 12,
          maxlength: "100",
        },

        // ============ 夜班（class2） ============
        {
          label: this.classHeaders[2],
          type: "title",
        },
        {
          label: this.$t("ui.data.column.dj.scheduleResult.planQty"),
          prop: "class2PlanQty",
          span: 12,
          type: "number",
          min: 0,
          max: 999999,
          precision: 0,
        },
        {
          label: this.$t("ui.data.column.dj.scheduleResult.finishQty"),
          prop: "class2FinishQty",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.finish"),
          prop: "class2FinishRate",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.dj.scheduleResult.sequence"),
          prop: "class2Sequence",
          span: 12,
          type: "number",
          min: 0,
          max: 999999,
          precision: 0,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class2Analysis"),
          prop: "class2Analysis",
          span: 12,
          maxlength: "100",
        },

        // ============ 早班（class3） ============
        {
          label: this.classHeaders[3],
          type: "title",
        },
        {
          label: this.$t("ui.data.column.dj.scheduleResult.planQty"),
          prop: "class3PlanQty",
          span: 12,
          type: "number",
          min: 0,
          max: 999999,
          precision: 0,
        },
        {
          label: this.$t("ui.data.column.dj.scheduleResult.finishQty"),
          prop: "class3FinishQty",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.finish"),
          prop: "class3FinishRate",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.dj.scheduleResult.sequence"),
          prop: "class3Sequence",
          span: 12,
          type: "number",
          min: 0,
          max: 999999,
          precision: 0,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class3Analysis"),
          prop: "class3Analysis",
          span: 12,
          maxlength: "100",
        },

        // ============ 4班 ============
        {
          label: this.classHeaders[4],
          type: "title",
        },
        {
          label: this.$t("ui.data.column.dj.scheduleResult.planQty"),
          prop: "class4PlanQty",
          span: 12,
          type: "number",
          min: 0,
          max: 999999,
          precision: 0,
        },
        {
          label: this.$t("ui.data.column.dj.scheduleResult.finishQty"),
          prop: "class4FinishQty",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.finish"),
          prop: "class4FinishRate",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.dj.scheduleResult.sequence"),
          prop: "class4Sequence",
          span: 12,
          type: "number",
          min: 0,
          max: 999999,
          precision: 0,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class4Analysis"),
          prop: "class4Analysis",
          span: 12,
          maxlength: "100",
        },

        // ============ 5班 ============
        {
          label: this.classHeaders[5],
          type: "title",
        },
        {
          label: this.$t("ui.data.column.dj.scheduleResult.planQty"),
          prop: "class5PlanQty",
          span: 12,
          type: "number",
          min: 0,
          max: 999999,
          precision: 0,
        },
        {
          label: this.$t("ui.data.column.dj.scheduleResult.finishQty"),
          prop: "class5FinishQty",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.finish"),
          prop: "class5FinishRate",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.dj.scheduleResult.sequence"),
          prop: "class5Sequence",
          span: 12,
          type: "number",
          min: 0,
          max: 999999,
          precision: 0,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class5Analysis"),
          prop: "class5Analysis",
          span: 12,
          maxlength: "100",
        },

        // ============ 6班 ============
        {
          label: this.classHeaders[6],
          type: "title",
        },
        {
          label: this.$t("ui.data.column.dj.scheduleResult.planQty"),
          prop: "class6PlanQty",
          span: 12,
          type: "number",
          min: 0,
          max: 999999,
          precision: 0,
        },
        {
          label: this.$t("ui.data.column.dj.scheduleResult.finishQty"),
          prop: "class6FinishQty",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.finish"),
          prop: "class6FinishRate",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.dj.scheduleResult.sequence"),
          prop: "class6Sequence",
          span: 12,
          type: "number",
          min: 0,
          max: 999999,
          precision: 0,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class6Analysis"),
          prop: "class6Analysis",
          span: 12,
          maxlength: "100",
        },
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
          class1PlanQty: numberEmpty(data.class1PlanQty),
          class1Sequence: numberEmpty(data.class1Sequence),
          class2PlanQty: numberEmpty(data.class2PlanQty),
          class2Sequence: numberEmpty(data.class2Sequence),
          class3PlanQty: numberEmpty(data.class3PlanQty),
          class3Sequence: numberEmpty(data.class3Sequence),
          class4PlanQty: numberEmpty(data.class4PlanQty),
          class4Sequence: numberEmpty(data.class4Sequence),
          class5PlanQty: numberEmpty(data.class5PlanQty),
          class5Sequence: numberEmpty(data.class5Sequence),
          class6PlanQty: numberEmpty(data.class6PlanQty),
          class6Sequence: numberEmpty(data.class6Sequence),
        };
      }
      // 获取班次标题
      const scheduleDate = this.form.scheduleDate;
      getWorkClass({ scheduleDate }).then((res) => {
        this.classHeaders = res;
      });
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
