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
import { saveLoss } from "@/api/tq/loss";
import { listEnabledMachines } from "@/api/tq/machine";

export default {
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
        beadCode: [
          {
            required: true,
            message: this.$t("common.rule.input"),
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
        this.$t("ui.data.column.tq.loss.modelName")
      );
    },
    columns() {
      return [
        {
          label: this.$t("ui.data.column.loss.beadCode"),
          prop: "beadCode",
          span: 24,
          required: true,
          maxlength: "20",
        },
        {
          label: this.$t("ui.data.column.loss.machineCode"),
          prop: "machineCode",
          span: 24,
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
          label: this.$t("ui.data.column.loss.lossRate"),
          prop: "lossRate",
          span: 24,
          type: "number",
          min: 0,
          max: 100,
          precision: 2,
          append: "%",
        },
        {
          label: this.$t("ui.common.column.remark"),
          prop: "remark",
          span: 24,
          type: "textarea",
          maxlength: "300",
        },
      ];
    },
  },
  methods: {
    async save(params) {
      if (!params.beadCode && !params.machineCode) {
        this.$modal.msgWarning(
          this.$t("ui.error.message.loss.isAllNull") || "代码和机台不能全部为空"
        );
        return;
      }
      try {
        this.loading = true;
        // 将百分比转换为小数存储（如 2 → 0.02）
        if (params.lossRate != null) {
          params.lossRate = parseFloat((params.lossRate / 100).toFixed(4));
        }
        const res = await saveLoss(params);
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
          lossRate: data.lossRate != null ? parseFloat((data.lossRate * 100).toFixed(2)) : null,
        };
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
