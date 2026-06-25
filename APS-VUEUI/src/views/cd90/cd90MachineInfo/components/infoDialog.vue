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
      form: {
        openMachineClass: [],
      },
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
        openMachineClass: [
          {
            required: true,
            validator: (rule, value, callback) => {
              if (!value || value.length === 0) {
                callback(new Error(this.$t("common.rule.select")));
              } else {
                callback();
              }
            },
            trigger: "change",
          },
        ],
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
          render: (form) => {
            return (
              <el-checkbox-group v-model={form.openMachineClass}>
                {this.parentDict.type.class_num_three_plan.map((row) => {
                  return (
                    <el-checkbox key={`SHIFT_${row.value}`} label={row.value}>
                      {row.label}
                    </el-checkbox>
                  );
                })}
              </el-checkbox-group>
            );
          },
        },
        {
          prop: "status",
          label: this.$t("ui.data.column.cd90MachineInfo.status"),
          type: "switch",
          activeValue: "1",
          inactiveValue: "0",
        },
        {
          prop: "remark",
          label: this.$t("ui.common.column.remark"),
          type: "textarea",
          rows: 3,
          maxlength: 900,
        },
      ];
    },
  },
  methods: {
    async save(params) {
      this.loading = true;
      try {
        if (params.openMachineClass && Array.isArray(params.openMachineClass)) {
          params.openMachineClass = params.openMachineClass.join(",");
        }
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
        this.form = {
          ...data,
          openMachineClass: data.openMachineClass
            ? data.openMachineClass.split(",")
            : [],
        };
      } else {
        this.form = {
          factoryCode: "116",
          isStickFilm: "0",
          status: "1",
          openMachineClass: [],
        };
      }
    },
    hide() {
      this.form = { openMachineClass: [] };
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
