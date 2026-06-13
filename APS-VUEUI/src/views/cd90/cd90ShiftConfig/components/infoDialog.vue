<template>
  <el-dialog
    :title="title"
    :visible="visible"
    width="720px"
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
      label-width="130px"
      v-loading="loading"
    />
    <template slot="footer">
      <el-button @click="hide">{{ $t("common.button.cancel") }}</el-button>
      <el-button type="primary" :loading="loading" @click="handleConfirm">{{ $t("common.button.confirm") }}</el-button>
    </template>
  </el-dialog>
</template>

<script>
import { addCd90ShiftConfig, updateCd90ShiftConfig } from "@/api/cd90/cd90ShiftConfig";
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
        shiftCode: [requiredInput],
        startTime: [requiredInput],
        endTime: [requiredInput],
        shiftHours: [
          requiredInput,
          {
            validator: (rule, value, callback) => {
              if (value === undefined || value === null || value === "" || Number(value) <= 0) {
                callback(new Error(this.$t("ui.data.alert.cd90ShiftConfig.shiftHoursPositive")));
              } else {
                callback();
              }
            },
            trigger: "blur",
          },
        ],
        isActive: [requiredSelect],
      },
    };
  },
  computed: {
    title() {
      return this.isEdit ? this.$t("common.button.edit") : this.$t("common.button.add");
    },
    columns() {
      return [
        {
          prop: "factoryCode",
          label: this.$t("ui.data.column.cd90ShiftConfig.factoryCode"),
          type: "select",
          dictData: this.parentDict.type.biz_factory_name,
          filterable: true,
        },
        {
          prop: "shiftCode",
          label: this.$t("ui.data.column.cd90ShiftConfig.shiftCode"),
          maxlength: 64,
        },
        {
          prop: "shiftName",
          label: this.$t("ui.data.column.cd90ShiftConfig.shiftName"),
          maxlength: 128,
        },
        {
          prop: "shiftOrder",
          label: this.$t("ui.data.column.cd90ShiftConfig.shiftOrder"),
          type: "number",
        },
        {
          prop: "startTime",
          label: this.$t("ui.data.column.cd90ShiftConfig.startTime"),
          type: "time",
          format: "HH:mm:ss",
          valueFormat: "HH:mm:ss",
        },
        {
          prop: "endTime",
          label: this.$t("ui.data.column.cd90ShiftConfig.endTime"),
          type: "time",
          format: "HH:mm:ss",
          valueFormat: "HH:mm:ss",
        },
        {
          prop: "shiftHours",
          label: this.$t("ui.data.column.cd90ShiftConfig.shiftHours"),
          type: "number",
        },
        {
          prop: "isCrossDay",
          label: this.$t("ui.data.column.cd90ShiftConfig.isCrossDay"),
          type: "select",
          dictData: this.parentDict.type.biz_yes_no,
          filterable: true,
        },
        {
          prop: "scheduleDay",
          label: this.$t("ui.data.column.cd90ShiftConfig.scheduleDay"),
          type: "number",
        },
        {
          prop: "dayShiftOrder",
          label: this.$t("ui.data.column.cd90ShiftConfig.dayShiftOrder"),
          type: "number",
        },
        {
          prop: "classField",
          label: this.$t("ui.data.column.cd90ShiftConfig.classField"),
          type: "select",
          options: [
            { label: "CLASS1", value: "CLASS1" },
            { label: "CLASS2", value: "CLASS2" },
            { label: "CLASS3", value: "CLASS3" },
            { label: "CLASS4", value: "CLASS4" },
            { label: "CLASS5", value: "CLASS5" },
            { label: "CLASS6", value: "CLASS6" },
            { label: "CLASS7", value: "CLASS7" },
            { label: "CLASS8", value: "CLASS8" },
          ],
          filterable: true,
        },
        {
          prop: "isActive",
          label: this.$t("ui.data.column.cd90ShiftConfig.isActive"),
          type: "switch",
          activeValue: 1,
          inactiveValue: 0,
        },
        {
          prop: "remark",
          label: this.$t("ui.common.column.remark"),
          type: "textarea",
          rows: 3,
          maxlength: 512,
        },
      ];
    },
  },
  methods: {
    async save(params) {
      this.loading = true;
      try {
        const res = this.isEdit
          ? await updateCd90ShiftConfig(params)
          : await addCd90ShiftConfig(params);
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
          isActive: 1,
          isCrossDay: 0,
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