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
import {saveParams} from "@/api/tm/params";

export default {
  components: { infoForm },
  inject: ["parentDict"],
  data() {
    return {
      loading: false,
      visible: false,
      isEdit: false,
      editType: null,
      form: {},
      rules: {
        factoryCode: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
        paramCode: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        paramName: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        paramValue: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        paramGroup: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
        valueType: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
        enableStatus: [
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
        this.$t("ui.data.column.tm.params.modelName")
      );
    },
    columns() {
      return [
        {
          prop: "factoryCode",
          label: this.$t("ui.data.column.factoryCode"),
          type: "select",
          dictData: this.parentDict.type.biz_factory_name,
          span: 12,
          required: true,
        },
        {
          prop: "paramCode",
          label: this.$t("ui.data.column.paramsCode"),
          span: 12,
          maxlength: 50,
          required: true,
          disabled: this.isEdit,
        },
        {
          prop: "paramName",
          label: this.$t("ui.data.column.paramsName"),
          span: 12,
          maxlength: 50,
          required: true,
        },
        {
          prop: "paramValue",
          label: this.$t("ui.data.column.paramsValue"),
          span: 12,
          maxlength: 200,
          required: true,
        },
        {
          prop: "defaultValue",
          label: this.$t("ui.data.column.tmParams.defaultValue"),
          span: 12,
          maxlength: 200,
        },
        {
          prop: "paramGroup",
          label: this.$t("ui.data.column.tmParams.paramGroup"),
          type: "select",
          span: 12,
          required: true,
          options: [
            { label: "全局参数", value: "GLOBAL" },
            { label: "班次参数", value: "SHIFT" },
            { label: "机台参数", value: "MACHINE" },
            { label: "胎面参数", value: "TREAD" },
          ],
        },
        {
          prop: "valueType",
          label: this.$t("ui.data.column.tmParams.valueType"),
          type: "select",
          span: 12,
          required: true,
          options: [
            { label: "字符串", value: "STRING" },
            { label: "数值", value: "NUMBER" },
            { label: "布尔", value: "BOOLEAN" },
            { label: "结构化对象", value: "JSON" },
          ],
        },
        {
          prop: "enableStatus",
          label: this.$t("ui.data.column.tmParams.enableStatus"),
          type: "select",
          span: 12,
          required: true,
          dictData: this.parentDict.type.biz_yes_no,
        },
        {
          prop: "regularExpression",
          label: this.$t("ui.data.column.tmParams.regularExpression"),
          span: 12,
          maxlength: 200,
        },
        {
          prop: "errorTips",
          label: this.$t("ui.data.column.tmParams.errorTips"),
          span: 12,
          maxlength: 200,
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
    // api
    async save(params) {
      try {
        this.loading = true;
        const res = await saveParams(params);
        this.$modal.msgSuccess(res.msg);
        this.$emit("success");
        this.hide();
        this.loading = false;
      } catch (error) {
        console.log(error);
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
        };
      } else {
        this.form = {
          factoryCode: "116",
          enableStatus: "1",
          paramGroup: "TREAD",
          valueType: "STRING",
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
