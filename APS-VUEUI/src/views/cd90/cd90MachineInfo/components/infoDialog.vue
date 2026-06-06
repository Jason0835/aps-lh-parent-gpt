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
import { addCd90MachineInfo, updateCd90MachineInfo } from "@/api/cd90/cd90MachineInfo";
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
        machineCode: [requiredInput],
        quota: [
          requiredInput,
          {
            validator: (rule, value, callback) => {
              if (value === undefined || value === null || value === "" || Number(value) <= 0) {
                callback(new Error(this.$t("ui.data.alert.cd90MachineInfo.quotaPositive")));
              } else {
                callback();
              }
            },
            trigger: "blur",
          },
        ],
        openMachineClass: [requiredSelect],
        status: [requiredSelect],
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
          label: this.$t("ui.data.column.cd90MachineInfo.factoryCode"),
          type: "select",
          dictData: this.parentDict.type.biz_factory_name,
          filterable: true,
        },
        {
          prop: "machineCode",
          label: this.$t("ui.data.column.cd90MachineInfo.machineCode"),
          maxlength: 30,
        },
        {
          prop: "isStickFilm",
          label: this.$t("ui.data.column.cd90MachineInfo.isStickFilm"),
          type: "select",
          dictData: this.parentDict.type.biz_yes_no,
          filterable: true,
        },
        {
          prop: "clothWidthMax",
          label: this.$t("ui.data.column.cd90MachineInfo.clothWidthMax"),
          type: "number",
        },
        {
          prop: "clothWidthMin",
          label: this.$t("ui.data.column.cd90MachineInfo.clothWidthMin"),
          type: "number",
        },
        {
          prop: "quota",
          label: this.$t("ui.data.column.cd90MachineInfo.quota"),
          type: "number",
        },
        {
          prop: "openMachineClass",
          label: this.$t("ui.data.column.cd90MachineInfo.openMachineClass"),
          type: "select",
          dictData: this.parentDict.type.class_num_three_plan,
          filterable: true,
        },
        {
          prop: "status",
          label: this.$t("ui.data.column.cd90MachineInfo.status"),
          type: "select",
          dictData: this.parentDict.type.sys_enable_disable,
          filterable: true,
        },
        {
          prop: "remark",
          label: this.$t("ui.common.column.remark"),
          type: "textarea",
          rows: 3,
          maxlength: 300,
        },
      ];
    },
  },
  methods: {
    async save(params) {
      this.loading = true;
      try {
        const res = this.isEdit
          ? await updateCd90MachineInfo(params)
          : await addCd90MachineInfo(params);
        this.$modal.msgSuccess(res.msg);
        this.$emit("success");
        this.hide();
      } finally {
        this.loading = false;
      }
    },
    show(data) {
      this.visible = true;
      if (data) {
        this.isEdit = true;
        this.form = { ...data };
      } else {
        this.form = {
          factoryCode: "116",
          isStickFilm: "0",
          status: "1",
        };
      }
    },
    hide() {
      this.form = {};
      this.$refs.form && this.$refs.form.triggerResetForm();
      this.isEdit = false;
      this.visible = false;
    },
    handleConfirm() {
      this.$refs.form.triggerConfirm(this.save);
    },
  },
};
</script>
