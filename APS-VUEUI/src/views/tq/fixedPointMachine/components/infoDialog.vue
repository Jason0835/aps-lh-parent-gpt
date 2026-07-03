<template>
  <el-dialog
    :title="title"
    :visible="visible"
    width="600px"
    @close="hide"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
  >
    <info-form
      class="form-item-height"
      ref="form"
      :form="form"
      :rules="rules"
      :columns="columns"
      label-position="right"
      label-width="150px"
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
import { saveSpecifyMachine } from "@/api/tq/specifyMachine";
import { listEnabledMachines } from "@/api/tq/machine";

export default {
  components: { infoForm },
  inject: ["parentDict"],
  data() {
    return {
      loading: false,
      machineLoading: false,
      visible: false,
      isEdit: false,
      form: {},
      machineList: [],
      rules: {
        beadCode: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        machineCode: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "blur",
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
        this.$t("ui.tq.specifyMachine.column.modalName")
      );
    },
    columns() {
      return [
        {
          label: this.$t("ui.tq.specifyMachine.column.beadCode"),
          prop: "beadCode",
          span: 24,
          required: true,
        },
        {
          label: this.$t("ui.specifyMachine.column.machineCode"),
          prop: "machineCode",
          span: 24,
          required: true,
          type: "select",
          dictData: this.machineList,
          filterable: true,
          loading: this.machineLoading,
          props: {
            label: "machineCode",
            value: "machineCode",
          },
          onFocus: this.handleMachineFocus,
        },
        {
          label: this.$t("ui.specifyMachine.column.lineType"),
          prop: "lineType",
          span: 24,
          type: "select",
          dictData: this.parentDict.type.LINE_TYPE,
        },
        {
          label: this.$t("ui.specifyMachine.column.jobType"),
          prop: "jobType",
          span: 24,
          type: "select",
          dictData: this.parentDict.type.JOB_TYPE,
        },
        {
          label: this.$t("ui.common.column.remark"),
          prop: "remark",
          span: 24,
          type: "textarea",
        },
      ];
    },
  },
  methods: {
    async save(params) {
      try {
        this.loading = true;
        const res = await saveSpecifyMachine(params);
        this.$modal.msgSuccess(res.msg);
        this.$emit("success");
        this.hide();
      } catch (error) {
        console.log(error);
      } finally {
        this.loading = false;
      }
    },
    async loadMachineList() {
      this.machineLoading = true;
      try {
        const res = await listEnabledMachines();
        const list = Array.isArray(res) ? res : (res.data || res.rows || []);
        this.machineList = list;
      } catch (error) {
        console.log(error);
      } finally {
        this.machineLoading = false;
      }
    },
    handleMachineFocus() {
      if (this.machineList.length === 0) {
        this.loadMachineList();
      }
    },
    show(data) {
      this.visible = true;
      this.machineList = [];
      if (data) {
        this.isEdit = true;
        this.form = {
          ...data,
        };
        // 编辑模式下，将当前选中的机台加入下拉选项
        if (data.machineCode && data.machineName) {
          this.machineList = [
            {
              machineCode: data.machineCode,
              machineName: data.machineName,
            },
          ];
        }
      } else {
        this.isEdit = false;
        this.form = {};
      }
    },
    hide() {
      this.form = {};
      this.$refs.form.triggerResetForm();
      this.isEdit = false;
      this.visible = false;
      this.machineList = [];
    },
    handleConfirm() {
      this.$refs.form.triggerConfirm(this.save);
    },
  },
};
</script>
