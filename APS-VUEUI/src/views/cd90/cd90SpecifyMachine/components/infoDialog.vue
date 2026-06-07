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
import { addSpecifyMachine, editSpecifyMachine } from "@/api/cd90/specifyMachine";
import { getCd90MachineEnableOptions } from "@/api/cd90/cd90MachineInfo";
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
      machineOptions: [],
      rules: {
        factoryCode: [requiredSelect],
        clothCode: [requiredInput],
        machineCode: [requiredSelect],
        jobType: [requiredSelect],
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
          label: this.$t("ui.data.column.cd90SpecifyMachine.factoryCode"),
          type: "select",
          dictData: this.parentDict.type.biz_factory_name,
          filterable: true,
          change: () => this.loadMachineOptions(),
        },
        {
          prop: "clothCode",
          label: this.$t("ui.data.column.cd90SpecifyMachine.clothCode"),
          maxlength: 20,
        },
        {
          prop: "machineCode",
          label: this.$t("ui.data.column.cd90SpecifyMachine.machineCode"),
          type: "select",
          dictData: this.machineOptions,
          filterable: true,
        },
        {
          prop: "jobType",
          label: this.$t("ui.data.column.cd90SpecifyMachine.jobType"),
          type: "select",
          dictData: this.parentDict.type.JOB_TYPE,
          filterable: true,
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
    async loadMachineOptions() {
      const res = await getCd90MachineEnableOptions({ factoryCode: this.form.factoryCode });
      const rows = Array.isArray(res) ? res : (res.rows || res.data || []);
      this.machineOptions = rows.map((item) => ({
        label: item.machineCode,
        value: item.machineCode,
      }));
    },
    async save(params) {
      this.loading = true;
      try {
        const payload = { ...params };
        const res = this.isEdit
          ? await editSpecifyMachine(payload)
          : await addSpecifyMachine(payload);
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
          jobType: "",
        };
      }
      this.loadMachineOptions();
    },
    hide() {
      this.form = {};
      this.machineOptions = [];
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
