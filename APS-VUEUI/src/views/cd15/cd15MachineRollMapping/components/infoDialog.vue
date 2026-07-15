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
import { addMachineRollMapping, updateMachineRollMapping } from "@/api/cd15/machineRollMapping";
import { getCd15MachineEnableOptions } from "@/api/cd15/cd15MachineInfo";
import infoForm from "@/views/components/infoForm.vue";

export default {
  components: { infoForm },
  inject: ["parentDict"],
  props: {
    machineOptions: {
      type: Array,
      default: () => [],
    },
  },
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
        shiftCode: [],
      },
      localMachineOptions: [],
      rules: {
        factoryCode: [requiredSelect],
        bigRollCode: [requiredInput],
        machineCode: [requiredSelect],
        shiftCode: [
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
          label: this.$t("ui.data.column.cd15MachineRollMapping.factoryCode"),
          type: "select",
          dictData: this.parentDict.type.biz_factory_name,
          filterable: true,
          change: () => this.loadMachineOptions(),
        },
        {
          prop: "bigRollCode",
          label: this.$t("ui.data.column.cd15MachineRollMapping.bigRollCode"),
          maxlength: 30,
        },
        {
          prop: "machineCode",
          label: this.$t("ui.data.column.cd15MachineRollMapping.machineCode"),
          type: "select",
          dictData: this.localMachineOptions,
          filterable: true,
        },
        {
          prop: "shiftCode",
          label: this.$t("ui.data.column.cd15MachineRollMapping.shiftCode"),
          render: (form) => {
            return (
              <el-checkbox-group v-model={form.shiftCode}>
                {this.parentDict.type.class_num_three_plan.map((row) => {
                  const value = this.getDictValue(row);
                  return (
                    <el-checkbox key={`SHIFT_${value}`} label={value}>
                      {this.getDictLabel(row)}
                    </el-checkbox>
                  );
                })}
              </el-checkbox-group>
            );
          },
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
    getDictValue(row) {
      const value = row.value !== undefined && row.value !== null && row.value !== "" ? row.value : row.dictValue;
      return value === undefined || value === null ? "" : `${value}`;
    },
    getDictLabel(row) {
      return row.label !== undefined ? row.label : row.dictLabel;
    },
    normalizeShiftCode(value) {
      if (Array.isArray(value)) {
        return value.map((item) => `${item}`).filter((item) => item);
      }
      if (!value) {
        return [];
      }
      return `${value}`.split(",").map((item) => item.trim()).filter((item) => item);
    },
    async save(params, confirmOutOfOpenShift = false) {
      this.loading = true;
      try {
        const saveParams = this.normalizeParams({
          ...params,
          confirmOutOfOpenShift,
        });
        const res = this.isEdit
          ? await updateMachineRollMapping(saveParams)
          : await addMachineRollMapping(saveParams);
        if (res.needConfirm) {
          this.loading = false;
          this.$confirm(res.msg, this.$t("newPage.common.tips"), { type: "warning" }).then(() => this.save(params, true));
          return;
        }
        this.$modal.msgSuccess(res.msg);
        this.$emit("success");
        this.hide();
      } finally {
        this.loading = false;
      }
    },
    normalizeParams(params) {
      return {
        ...params,
        shiftCode: Array.isArray(params.shiftCode) ? params.shiftCode.join(",") : params.shiftCode,
      };
    },
    async loadMachineOptions() {
      const res = await getCd15MachineEnableOptions({ factoryCode: this.form.factoryCode || "116" });
      const rows = Array.isArray(res) ? res : (res.rows || res.data || []);
      this.localMachineOptions = rows.map((item) => ({ label: item.machineCode, value: item.machineCode }));
    },
    show(data) {
      if (data) {
        this.isEdit = true;
        this.form = {
          ...data,
          shiftCode: this.normalizeShiftCode(data.shiftCode),
        };
      } else {
        this.form = {
          factoryCode: "116",
          shiftCode: [],
        };
      }
      this.localMachineOptions = this.machineOptions;
      this.visible = true;
      this.loadMachineOptions();
    },
    hide() {
      this.form = { shiftCode: [] };
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
