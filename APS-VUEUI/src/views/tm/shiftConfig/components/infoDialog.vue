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
import infoForm from "@/views/components/infoForm.vue";
import {saveTmShiftConfig} from "@/api/tm/shiftConfig";

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

        shiftCode: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        shiftName: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        shiftOrder: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        planStartTime: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
          {
            validator: (rule, value, callback) => {
              if (value && this.form.planEndTime) {
                if (value > this.form.planEndTime) {
                  callback(new Error("开始时间不能大于结束时间"));
                } else {
                  callback();
                }
              } else {
                callback();
              }
            },
            trigger: "change",
          },
        ],
        planEndTime: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
          {
            validator: (rule, value, callback) => {
              if (value && this.form.planStartTime) {
                if (value < this.form.planStartTime) {
                  callback(new Error("结束时间不能小于开始时间"));
                } else {
                  callback();
                }
              } else {
                callback();
              }
            },
            trigger: "change",
          },
        ],
        crossDayFlag: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
        openFlag: [
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
          ? this.$t("common.button.edit")
          : this.$t("common.button.add")) +
        this.$t("ui.data.column.tm.shiftConfig.modelName")
      );
    },
    columns() {
      return [
        {
          prop: "factoryCode",
          label: this.$t("ui.data.column.tm.shiftConfig.factoryCode"),
          type: "select",
          dictData: this.parentDict.type.biz_factory_name,
          span: 12,
          required: true,
        },

        {
          prop: "shiftCode",
          label: this.$t("ui.data.column.tm.shiftConfig.shiftCode"),
          span: 12,
          maxlength: 20,
          required: true,
          disabled: this.isEdit,
        },
        {
          prop: "shiftName",
          label: this.$t("ui.data.column.tm.shiftConfig.shiftName"),
          span: 12,
          maxlength: 50,
          required: true,
        },
        {
          prop: "shiftOrder",
          label: this.$t("ui.data.column.tm.shiftConfig.shiftOrder"),
          span: 12,
          type: "number",
          required: true,
        },
        {
          prop: "planStartTime",
          label: this.$t("ui.data.column.tm.shiftConfig.planStartTime"),
          type: "time",
          valueFormat: "HH:mm:ss",
          span: 12,
          required: true,
        },
        {
          prop: "planEndTime",
          label: this.$t("ui.data.column.tm.shiftConfig.planEndTime"),
          type: "time",
          valueFormat: "HH:mm:ss",
          span: 12,
          required: true,
        },
        {
          prop: "crossDayFlag",
          label: this.$t("ui.data.column.tm.shiftConfig.crossDayFlag"),
          type: "select",
          span: 12,
          required: true,
          dictData: this.parentDict.type.biz_yes_no,
        },
        {
          prop: "openFlag",
          label: this.$t("ui.data.column.tm.shiftConfig.openFlag"),
          type: "select",
          span: 12,
          required: true,
          dictData: this.parentDict.type.biz_yes_no,
        },
        {
          prop: "remark",
          label: this.$t("ui.common.column.remark"),
          span: 24,
          type: "textarea",
          maxlength: 900,
        },
      ];
    },
  },
  methods: {
    async save(params) {
      try {
        this.loading = true;
        const res = await saveTmShiftConfig(params);
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
