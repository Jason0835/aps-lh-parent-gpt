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
import { saveSpecifyMachine, checkSpecifyMachineUnique } from "@/api/gsq/specifyMachine";
import { listEnabledMachines } from "@/api/gsq/machine";

export default {
  dicts: ["LINE_TYPE", "JOB_TYPE"],
  components: { infoForm },
  data() {
    return {
      loading: false,
      machineLoading: false,
      visible: false,
      isEdit: false,
      form: {},
      machineList: [],
      rules: {
        steelRingCode: [
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
        this.$t("ui.data.column.gsq.specifyMachine.modalName")
      );
    },
    columns() {
      return [
        {
          label: this.$t("ui.data.column.gsq.specifyMachine.steelRingCode"),
          prop: "steelRingCode",
          span: 24,
          required: true,
          type: "input",
          maxlength: "50",
        },
        {
          label: this.$t("ui.data.column.gsq.specifyMachine.machineName"),
          prop: "machineCode",
          span: 24,
          required: true,
          type: "select",
          dictData: this.machineList,
          filterable: true,
          loading: this.machineLoading,
          props: {
            label: "machineName",
            value: "machineCode",
          },
          onFocus: this.handleMachineFocus,
        },
        {
          label: this.$t("ui.data.column.gsq.specifyMachine.lineType"),
          prop: "lineType",
          span: 24,
          type: "select",
          dictData: this.dict.type.LINE_TYPE,
          filterable: true,
        },
        {
          label: this.$t("ui.data.column.gsq.specifyMachine.jobType"),
          prop: "jobType",
          span: 24,
          type: "select",
          dictData: this.dict.type.JOB_TYPE,
          filterable: true,
        },
        {
          label: this.$t("ui.common.column.remark"),
          prop: "remark",
          span: 24,
          type: "textarea",
          maxlength: "900",
        },
      ];
    },
  },
  methods: {
    /**
     * 保存前的唯一性校验
     * 校验"钢丝圈代码+生产线"组合是否已存在
     * 返回 true 表示通过校验，false 表示不通过
     */
    async validateUnique() {
      try {
        const res = await checkSpecifyMachineUnique(this.form);
        // UserConstants.NOT_UNIQUE = "1" 不唯一，UserConstants.UNIQUE = "0" 唯一
        if (res === "1" || res === true) {
          this.$modal.msgWarning(
            this.$t("ui.data.column.gsq.specifyMachine.unique")
          );
          return false;
        }
        return true;
      } catch (error) {
        console.log(error);
        return false;
      }
    },
    async save(params) {
      try {
        // 先校验唯一性
        const isUnique = await this.validateUnique();
        if (!isUnique) {
          return;
        }
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
        // 编辑时回显当前机台名称
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
