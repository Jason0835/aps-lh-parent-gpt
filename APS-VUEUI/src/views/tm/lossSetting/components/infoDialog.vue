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
import {saveTmLossSetting} from "@/api/tm/lossSetting";

export default {
  components: { infoForm },
  inject: ["parentDict"],
  computed: {
    machines() {
      return this.$store.state.tm.machines;
    },
    title: function () {
      return (
        (this.isEdit
          ? this.$t("common.button.edit")
          : this.$t("common.button.add")) +
        this.$t("ui.data.column.tm.lossSetting.modelName")
      );
    },
    columns() {
      return [
        {
          prop: "factoryCode",
          label: this.$t("ui.data.column.tm.lossSetting.factoryCode"),
          type: "select",
          span: 12,
          required: true,
          dictData: this.parentDict.type.biz_factory_name,
        },
        {
          prop: "treadCode",
          label: this.$t("ui.data.column.tm.lossSetting.treadCode"),
          span: 12,
          maxlength: 60,
          required: true,
          disabled: this.isEdit,
        },
        {
          prop: "machineCode",
          label: this.$t("ui.data.column.tm.lossSetting.machineCode"),
          span: 12,
          required: true,
          disabled: this.isEdit,
          type: "select",
          dictData: this.machines,
          props: { label: "machineCode", value: "machineCode" },
          filterable: true,
        },
        {
          prop: "lossRate",
          label: this.$t("ui.data.column.tm.lossSetting.lossRate"),
          span: 12,
          type: "number",
          required: true,
        },
        {
          prop: "settingLevel",
          label: this.$t("ui.data.column.tm.lossSetting.settingLevel"),
          span: 12,
          maxlength: 30,
          disabled: this.isEdit,
        },
        {
          prop: "priority",
          label: this.$t("ui.data.column.tm.lossSetting.priority"),
          span: 12,
          type: "number",
        },
        {
          prop: "enableStatus",
          label: this.$t("ui.data.column.tm.lossSetting.enableStatus"),
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
        treadCode: [
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
        lossRate: [
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
  methods: {
    async save(params) {
      try {
        this.loading = true;
        const res = await saveTmLossSetting(params);
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
