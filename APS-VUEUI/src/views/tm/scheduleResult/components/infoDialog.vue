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
import infoForm from "@/views/components/infoForm.vue";
import {saveTmScheduleResult} from "@/api/tm/scheduleResult";

export default {
  components: { infoForm },
  inject: ["parentDict"],
  data() {
    return {
      loading: false,
      visible: false,
      isEdit: false,
      form: {},
      rules: {
        factoryCode: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
      },
    };
  },
  computed: {
    title: function () {
      return (
        (this.isEdit
          ? this.$t("ui.data.column.scheduleResult.changePlan")
          : this.$t("ui.data.column.scheduleResult.insertOrder")) +
        this.$t("ui.data.column.tm.scheduleResult.modelName")
      );
    },
    columns() {
      return [
        {
          prop: "factoryCode",
          label: this.$t("ui.data.column.tm.scheduleResult.factoryCode"),
          type: "select",
          span: 12,
          required: true,
          dictData: this.parentDict.type.biz_factory_name,
          filterable: true,
        },
        {
          prop: "batchNo",
          label: this.$t("ui.data.column.tm.scheduleResult.batchNo"),
          span: 12,
          maxlength: 50,
          disabled: this.isEdit,
        },
        {
          prop: "scheduleDate",
          label: this.$t("ui.data.column.tm.scheduleResult.scheduleDate"),
          type: "date",
          span: 12,
          valueFormat: "yyyy-MM-dd",
        },
        {
          prop: "machineCode",
          label: this.$t("ui.data.column.tm.scheduleResult.machineCode"),
          span: 12,
          maxlength: 50,
        },
        {
          prop: "treadCode",
          label: this.$t("ui.data.column.tm.scheduleResult.treadCode"),
          span: 12,
          maxlength: 50,
        },
        {
          prop: "glueCode",
          label: this.$t("ui.data.column.tm.scheduleResult.glueCode"),
          span: 12,
          maxlength: 50,
        },
        {
          prop: "wholeGlueCode",
          label: this.$t("ui.data.column.tm.scheduleResult.wholeGlueCode"),
          span: 12,
          maxlength: 100,
        },
        {
          prop: "glueSeq",
          label: this.$t("ui.data.column.tm.scheduleResult.glueSeq"),
          span: 12,
          maxlength: 50,
        },
        {
          prop: "mouthPlateCode",
          label: this.$t("ui.data.column.tm.scheduleResult.mouthPlateCode"),
          span: 12,
          maxlength: 50,
        },
        {
          prop: "releaseStatus",
          label: this.$t("ui.data.column.tm.scheduleResult.releaseStatus"),
          span: 12,
          type: "select",
          dictData: this.parentDict.type.tm_release_status,
          filterable: true,
        },
        {
          prop: "dataSource",
          label: this.$t("ui.data.column.tm.scheduleResult.dataSource"),
          span: 12,
          type: "select",
          dictData: this.parentDict.type.tm_data_source,
          filterable: true,
        },
        {
          prop: "tailFlag",
          label: this.$t("ui.data.column.tm.scheduleResult.tailFlag"),
          span: 12,
          type: "select",
          dictData: this.parentDict.type.biz_yes_no,
          filterable: true,
        },
        // 1班：顺序、计划量、完成量、原因分析
        {
          label: this.$t("ui.data.column.tm.scheduleResult.class1Sequence"),
          span: 24,
          type: "group",
        },
        {
          prop: "class1Sequence",
          label: this.$t("ui.data.column.tm.scheduleResult.class1Sequence"),
          span: 6,
          type: "number",
        },
        {
          prop: "class1PlanQty",
          label: this.$t("ui.data.column.tm.scheduleResult.class1PlanQty"),
          span: 6,
          type: "number",
        },
        {
          prop: "class1FinishQty",
          label: this.$t("ui.data.column.tm.scheduleResult.class1FinishQty"),
          span: 6,
          type: "number",
        },
        {
          prop: "class1Analysis",
          label: this.$t("ui.data.column.tm.scheduleResult.class1Analysis"),
          span: 6,
          maxlength: 200,
        },
        // 2班
        {
          label: this.$t("ui.data.column.tm.scheduleResult.class2Sequence"),
          span: 24,
          type: "group",
        },
        {
          prop: "class2Sequence",
          label: this.$t("ui.data.column.tm.scheduleResult.class2Sequence"),
          span: 6,
          type: "number",
        },
        {
          prop: "class2PlanQty",
          label: this.$t("ui.data.column.tm.scheduleResult.class2PlanQty"),
          span: 6,
          type: "number",
        },
        {
          prop: "class2FinishQty",
          label: this.$t("ui.data.column.tm.scheduleResult.class2FinishQty"),
          span: 6,
          type: "number",
        },
        {
          prop: "class2Analysis",
          label: this.$t("ui.data.column.tm.scheduleResult.class2Analysis"),
          span: 6,
          maxlength: 200,
        },
        // 3班
        {
          label: this.$t("ui.data.column.tm.scheduleResult.class3Sequence"),
          span: 24,
          type: "group",
        },
        {
          prop: "class3Sequence",
          label: this.$t("ui.data.column.tm.scheduleResult.class3Sequence"),
          span: 6,
          type: "number",
        },
        {
          prop: "class3PlanQty",
          label: this.$t("ui.data.column.tm.scheduleResult.class3PlanQty"),
          span: 6,
          type: "number",
        },
        {
          prop: "class3FinishQty",
          label: this.$t("ui.data.column.tm.scheduleResult.class3FinishQty"),
          span: 6,
          type: "number",
        },
        {
          prop: "class3Analysis",
          label: this.$t("ui.data.column.tm.scheduleResult.class3Analysis"),
          span: 6,
          maxlength: 200,
        },
        // 4班
        {
          label: this.$t("ui.data.column.tm.scheduleResult.class4Sequence"),
          span: 24,
          type: "group",
        },
        {
          prop: "class4Sequence",
          label: this.$t("ui.data.column.tm.scheduleResult.class4Sequence"),
          span: 6,
          type: "number",
        },
        {
          prop: "class4PlanQty",
          label: this.$t("ui.data.column.tm.scheduleResult.class4PlanQty"),
          span: 6,
          type: "number",
        },
        {
          prop: "class4FinishQty",
          label: this.$t("ui.data.column.tm.scheduleResult.class4FinishQty"),
          span: 6,
          type: "number",
        },
        {
          prop: "class4Analysis",
          label: this.$t("ui.data.column.tm.scheduleResult.class4Analysis"),
          span: 6,
          maxlength: 200,
        },
        // 5班
        {
          label: this.$t("ui.data.column.tm.scheduleResult.class5Sequence"),
          span: 24,
          type: "group",
        },
        {
          prop: "class5Sequence",
          label: this.$t("ui.data.column.tm.scheduleResult.class5Sequence"),
          span: 6,
          type: "number",
        },
        {
          prop: "class5PlanQty",
          label: this.$t("ui.data.column.tm.scheduleResult.class5PlanQty"),
          span: 6,
          type: "number",
        },
        {
          prop: "class5FinishQty",
          label: this.$t("ui.data.column.tm.scheduleResult.class5FinishQty"),
          span: 6,
          type: "number",
        },
        {
          prop: "class5Analysis",
          label: this.$t("ui.data.column.tm.scheduleResult.class5Analysis"),
          span: 6,
          maxlength: 200,
        },
        // 6班
        {
          label: this.$t("ui.data.column.tm.scheduleResult.class6Sequence"),
          span: 24,
          type: "group",
        },
        {
          prop: "class6Sequence",
          label: this.$t("ui.data.column.tm.scheduleResult.class6Sequence"),
          span: 6,
          type: "number",
        },
        {
          prop: "class6PlanQty",
          label: this.$t("ui.data.column.tm.scheduleResult.class6PlanQty"),
          span: 6,
          type: "number",
        },
        {
          prop: "class6FinishQty",
          label: this.$t("ui.data.column.tm.scheduleResult.class6FinishQty"),
          span: 6,
          type: "number",
        },
        {
          prop: "class6Analysis",
          label: this.$t("ui.data.column.tm.scheduleResult.class6Analysis"),
          span: 6,
          maxlength: 200,
        },
      ];
    },
  },
  methods: {
    async save(params) {
      try {
        this.loading = true;
        const res = await saveTmScheduleResult(params);
        this.$modal.msgSuccess(res.msg);
        this.$emit("success");
        this.hide();
        this.loading = false;
      } catch (error) {
        console.log(error);
        this.loading = false;
      }
    },

    show(data) {
      this.visible = true;
      if (data) {
        this.isEdit = true;
        this.form = {
          ...data,
        };
      } else {
        this.form = {
          factoryCode: "116",
        };
      }
    },
    hide() {
      this.form = {};
      this.$refs.form.triggerResetForm();
      this.isEdit = false;
      this.visible = false;
    },
    handleConfirm() {
      this.$refs.form.triggerConfirm(this.save);
    },
  },
};
</script>
