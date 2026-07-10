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
      form: {},
      localMachineOptions: [],
      rules: {
        factoryCode: [requiredSelect],
        bigRollCode: [requiredInput],
        machineCode: [requiredSelect],
        shiftCode: [requiredSelect],
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
          type: "select",
          dictData: this.parentDict.type.class_num,
          filterable: true,
          attrs: {
            multiple: true,
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
    async save(params) {
      this.loading = true;
      try {
        const saveParams = this.normalizeParams(params);
        const res = this.isEdit
          ? await updateMachineRollMapping(saveParams)
          : await addMachineRollMapping(saveParams);
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
      this.visible = true;
      if (data) {
        this.isEdit = true;
        this.form = { ...data };
        if (this.form.shiftCode) {
          this.form.shiftCode = this.form.shiftCode.split(",").filter((item) => item);
        }
      } else {
        this.form = {
          factoryCode: "116",
        };
      }
      this.localMachineOptions = this.machineOptions;
      this.loadMachineOptions();
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
