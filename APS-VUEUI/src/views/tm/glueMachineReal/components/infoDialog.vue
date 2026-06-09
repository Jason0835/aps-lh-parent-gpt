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
import {saveGlueMachineReal} from "@/api/tm/glueMachineReal";

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
        glueCode: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        machineCode: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
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
    machines() {
      return this.$store.state.tm.machines;
    },
    title: function () {
      return (
        (this.isEdit
          ? this.$t("common.button.edit")
          : this.$t("common.button.add")) +
        this.$t("ui.data.column.tmGlueMachineReal.modelName")
      );
    },
    columns() {
      return [
        {
          prop: "factoryCode",
          label: this.$t("ui.data.column.tmGlueMachineReal.factoryCode"),
          type: "select",
          span: 12,
          required: true,
          dictData: this.parentDict.type.biz_factory_name,
        },
        {
          prop: "glueCode",
          label: this.$t("ui.data.column.tmGlueMachineReal.glueCode"),
          span: 12,
          maxlength: 20,
          required: true,
          disabled: this.isEdit,
        },
        {
          prop: "baseGlueCode",
          label: this.$t("ui.data.column.tmGlueMachineReal.baseGlueCode"),
          span: 12,
          maxlength: 60,
        },
        {
          prop: "machineCode",
          label: this.$t("ui.data.column.tmGlueMachineReal.machineCode"),
          span: 12,
          required: true,
          disabled: this.isEdit,
          type: "select",
          dictData: this.machines,
          props: { label: "machineCode", value: "machineCode" },
          filterable: true,
        },
        {
          prop: "shiftCode",
          label: this.$t("ui.data.column.tmGlueMachineReal.shiftCode"),
          span: 12,
          maxlength: 10,
        },
        {
          prop: "priority",
          label: this.$t("ui.data.column.tmGlueMachineReal.priority"),
          span: 12,
          type: "number",
        },
        {
          prop: "allowFlag",
          label: this.$t("ui.data.column.tmGlueMachineReal.allowFlag"),
          type: "select",
          span: 12,
          dictData: this.parentDict.type.biz_yes_no,
        },
        {
          prop: "enableStatus",
          label: this.$t("ui.data.column.tmGlueMachineReal.enableStatus"),
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
        const res = await saveGlueMachineReal(params);
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
          enableStatus: "1",
          allowFlag: "1",
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
