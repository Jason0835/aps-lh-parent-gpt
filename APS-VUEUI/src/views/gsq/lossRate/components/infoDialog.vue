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
import { saveLossRate } from "@/api/gsq/lossRate";
import { listEnabledMachines } from "@/api/gsq/machine";

export default {
  dicts: [],
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
        lossRate: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        // 钢丝圈代码与机台编码至少一个有值（联合校验）
        steelRingCode: [
          {
            validator: (rule, value, callback) => {
              if (
                !value &&
                !this.form.machineCode
              ) {
                callback(
                  new Error(
                    this.$t(
                      "ui.error.message.gsq.lossRate.codeMachineEmpty"
                    )
                  )
                );
              } else {
                callback();
              }
            },
            trigger: "change",
          },
        ],
        machineCode: [
          {
            validator: (rule, value, callback) => {
              if (
                !value &&
                !this.form.steelRingCode
              ) {
                callback(
                  new Error(
                    this.$t(
                      "ui.error.message.gsq.lossRate.codeMachineEmpty"
                    )
                  )
                );
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
        this.$t("ui.data.column.gsq.lossRate.modalName")
      );
    },
    columns() {
      return [
        {
          label: this.$t("ui.data.column.gsq.lossRate.steelRingCode"),
          prop: "steelRingCode",
          span: 24,
          type: "input",
          maxlength: 50,
        },
        {
          label: this.$t("ui.data.column.gsq.lossRate.machineName"),
          prop: "machineCode",
          span: 24,
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
          label: this.$t("ui.data.column.gsq.lossRate.lossRate"),
          prop: "lossRate",
          span: 24,
          required: true,
          type: "number",
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
    async save(params) {
      try {
        this.loading = true;
        const res = await saveLossRate(params);
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
