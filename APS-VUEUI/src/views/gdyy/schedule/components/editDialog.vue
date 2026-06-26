<template>
  <el-dialog
    :title="title"
    :visible="visible"
    width="900px"
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
    />
    <template slot="footer">
      <el-button @click="hide">{{ $t("common.button.cancel") }}</el-button>
      <el-button type="primary" :loading="loading" @click="handleConfirm">{{ $t("common.button.confirm") }}</el-button>
    </template>
  </el-dialog>
</template>

<script>
import { addGdyyScheduleResult, updateGdyyScheduleResult } from "@/api/gdyy/gdyyScheduleResult";
import infoForm from "@/views/components/infoForm.vue";

export default {
  components: { infoForm },
  inject: ["parentDict"],
  data() {
    const requiredSelect = {
      required: true,
      message: this.$t("common.rule.select"),
      trigger: "change",
    };
    const requiredInput = {
      required: true,
      message: this.$t("common.rule.input"),
      trigger: "blur",
    };
    return {
      loading: false,
      visible: false,
      isEdit: false,
      form: {},
      rules: {
        factoryCode: [requiredSelect],
        scheduleDate: [requiredSelect],
        bigRollCode: [requiredInput],
        machineCode: [requiredInput],
      },
    };
  },
  computed: {
    title() {
      return this.isEdit ? this.$t("common.button.edit") : this.$t("ui.data.column.scheduleResult.insertOrder");
    },
    columns() {
      return [
        {
          prop: "factoryCode",
          label: this.$t("ui.data.column.gdyyScheduleResult.factoryCode"),
          type: "select",
          dictData: this.parentDict.type.biz_factory_name,
          filterable: true,
          span: 12,
        },
        {
          prop: "scheduleDate",
          label: this.$t("ui.data.column.gdyyScheduleResult.scheduleDate"),
          type: "date",
          valueFormat: "yyyy-MM-dd",
          span: 12,
        },
        {
          prop: "bigRollCode",
          label: this.$t("ui.data.column.gdyyScheduleResult.bigRollCode"),
          maxlength: 30,
          span: 12,
        },
        {
          prop: "machineCode",
          label: this.$t("ui.data.column.gdyyScheduleResult.machineCode"),
          maxlength: 100,
          span: 12,
        },
        {
          prop: "dayUsed",
          label: this.$t("ui.data.column.gdyyScheduleResult.dayUsed"),
          type: "number",
          span: 12,
        },
        {
          prop: "stockQty",
          label: this.$t("ui.data.column.gdyyScheduleResult.stockQty"),
          type: "number",
          span: 12,
        },
        {
          prop: "class1PlanQty",
          label: this.$t("ui.data.column.gdyyScheduleResult.class1PlanQty"),
          type: "number",
          span: 8,
        },
        {
          prop: "class2PlanQty",
          label: this.$t("ui.data.column.gdyyScheduleResult.class2PlanQty"),
          type: "number",
          span: 8,
        },
        {
          prop: "class3PlanQty",
          label: this.$t("ui.data.column.gdyyScheduleResult.class3PlanQty"),
          type: "number",
          span: 8,
        },
        {
          prop: "class4PlanQty",
          label: this.$t("ui.data.column.gdyyScheduleResult.class4PlanQty"),
          type: "number",
          span: 8,
        },
        {
          prop: "class5PlanQty",
          label: this.$t("ui.data.column.gdyyScheduleResult.class5PlanQty"),
          type: "number",
          span: 8,
        },
        {
          prop: "class6PlanQty",
          label: this.$t("ui.data.column.gdyyScheduleResult.class6PlanQty"),
          type: "number",
          span: 8,
        },
        {
          prop: "class7PlanQty",
          label: this.$t("ui.data.column.gdyyScheduleResult.class7PlanQty"),
          type: "number",
          span: 8,
        },
        {
          prop: "class8PlanQty",
          label: this.$t("ui.data.column.gdyyScheduleResult.class8PlanQty"),
          type: "number",
          span: 8,
        },
        {
          prop: "remark",
          label: this.$t("ui.common.column.remark"),
          type: "textarea",
          rows: 2,
          maxlength: 900,
          span: 24,
        },
      ];
    },
  },
  methods: {
    async save(params) {
      this.loading = true;
      try {
        const res = this.isEdit
          ? await updateGdyyScheduleResult(params)
          : await addGdyyScheduleResult(params);
        this.$modal.msgSuccess(res.msg);
        this.$emit("success");
        this.hide();
      } finally {
        this.loading = false;
      }
    },
    show(data) {
      this.visible = true;
      this.form = data || {};
      if (data) {
        this.isEdit = true;
      } else {
        this.isEdit = false;
        this.form = {
          factoryCode: "116",
        };
      }
    },
    hide() {
      this.visible = false;
      this.form = {};
      this.$refs.form.resetFields();
    },
    handleConfirm() {
      this.$refs.form.validate((valid) => {
        if (valid) {
          this.save(this.form);
        }
      });
    },
    openDialog(type, data) {
      if (type === "add") {
        this.show(null);
      } else {
        this.show(data);
      }
    },
  },
};
</script>
