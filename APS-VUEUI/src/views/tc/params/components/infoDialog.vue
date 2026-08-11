<template>
  <el-dialog
    :append-to-body="true"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :title="title"
    :visible="visible"
    width="800px"
    @close="hide"
  >
    <info-form
      ref="form"
      v-loading="loading"
      :columns="columns"
      :form="form"
      :rules="rules"
      class="form-item-height"
      label-position="right"
      label-width="160px"
    >
    </info-form>
    <template slot="footer">
      <el-button @click="hide">{{ this.$t("common.button.cancel") }}</el-button>
      <el-button :loading="loading" type="primary" @click="handleConfirm">{{
        this.$t("common.button.confirm")
      }}</el-button>
    </template>
  </el-dialog>
</template>

<script>
import infoForm from "@/views/components/infoForm.vue";
import {saveParams} from "@/api/tc/params";

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
      },
    };
  },
  computed: {
    title: function () {
      return (
        (this.isEdit
          ? this.$t("common.button.edit")
          : this.$t("common.button.add")) +
        this.$t("ui.data.column.tc.Params.modelName")
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
          label: this.$t("ui.data.column.tc.params.defaultValue"),
          span: 12,
          maxlength: 200,
        },
        {
          prop: "paramGroup",
          label: this.$t("ui.data.column.tc.params.paramGroup"),
          type: "select",
          span: 12,
          required: true,
          options: [
            { label: "全局参数", value: "GLOBAL" },
            { label: "班次参数", value: "SHIFT" },
            { label: "机台参数", value: "MACHINE" },
            { label: "胎侧参数", value: "SIDEWALL" },
          ],
        },
        {
          prop: "valueType",
          label: this.$t("ui.data.column.tc.params.valueType"),
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
          label: this.$t("ui.data.column.tc.params.enableStatus"),
          type: "switch",
          span: 12,
          activeValue: "1",
          inactiveValue: "0",
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
          enableStatus: data.enableStatus || "0",
        };
      } else {
        this.form = {
          factoryCode: "116",
          enableStatus: "1",
          paramGroup: "SIDEWALL",
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
