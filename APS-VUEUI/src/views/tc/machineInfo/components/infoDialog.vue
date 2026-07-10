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
import {saveTcMachineInfo} from "@/api/tc/machineInfo";

export default {
  components: { infoForm },
  inject: ["parentDict"],
  data() {
    return {
      loading: false,
      visible: false,
      isEdit: false,
      editType: null,
      form: {
        openShiftCode: [],
      },
      rules: {
        factoryCode: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
        machineCode: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        machineName: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        openShiftCode: [
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
    title: function () {
      return (
        (this.isEdit
          ? this.$t("common.button.edit")
          : this.$t("common.button.add")) +
        this.$t("ui.data.column.tc.MachineInfo.modelName")
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
          prop: "machineCode",
          label: this.$t("ui.data.column.tc.machineInfo.machineCode"),
          span: 12,
          maxlength: 50,
          required: true,
          disabled: this.isEdit,
        },
        {
          prop: "machineName",
          label: this.$t("ui.data.column.tc.machineInfo.machineName"),
          span: 12,
          maxlength: 100,
          required: true,
        },
        {
          prop: "maxCapacity",
          label: this.$t("ui.data.column.tc.machineInfo.maxCapacity"),
          span: 12,
        },
        {
          prop: "openShiftCode",
          label: this.$t("ui.data.column.tc.machineInfo.openShiftCode"),
          span: 24,
          render: (form) => {
            return (
              <el-checkbox-group v-model={form.openShiftCode}>
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
          prop: "machineStatus",
          label: this.$t("ui.data.column.tc.machineInfo.machineStatus"),
          span: 12,
          type: "switch",
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
        if (params.openShiftCode && Array.isArray(params.openShiftCode)) {
          params.openShiftCode = params.openShiftCode.join(",");
        }
        const res = await saveTcMachineInfo(params);
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
          machineStatus: data.machineStatus || "0",
          openShiftCode: data.openShiftCode
            ? data.openShiftCode.split(",")
            : [],
        };
      } else {
        this.form = {
          factoryCode: "116",
          machineStatus: "1",
          openShiftCode: [],
        };
      }
    },
    hide() {
      this.form = { openShiftCode: [] };
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
