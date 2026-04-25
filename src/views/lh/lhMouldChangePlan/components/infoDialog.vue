<template>
  <el-dialog
    :title="title"
    :visible="visible"
    width="1200px"
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
      label-width="120px"
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
import { editLhMouldChangePlan } from "@/api/lh/lhMouldChangePlan";
import { listMachine } from "@/api/lh/machine";

import infoForm from "@/views/components/infoForm.vue";
import materialCodeSelect from "@/views/components/materialCodeSelect.vue";

export default {
  components: { infoForm, materialCodeSelect },
  inject: ["parentDict"],
  data() {
    return {
      loading: false,
      visible: false,
      isEdit: false,
      form: {},
      originalIsRelease: "",
      machineOptions: [],
      rules: {
        factoryCode: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
        lhResultBatchNo: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        orderNo: [
          {
            required: false,
            trigger: "change",
          },
        ],
        planDate: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
        planOrder: [
          {
            validator: (rule, value, callback) => {
              if (value === undefined || value === null || value === "") {
                callback();
                return;
              }
              if (String(value).length > 5) {
                callback(new Error(this.$t("ui.data.alert.lhMouldChangePlan.planOrderMax")));
                return;
              }
              callback();
            },
            trigger: ["change", "blur"],
          },
        ],
        scheduleDate: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
        lhMachineCode: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
        mouldCode: [
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
      return this.isEdit
        ? this.$t("common.button.edit")
        : this.$t("common.button.add");
    },
    columns() {
      const columns = [
        {
          prop: "factoryCode",
          label: this.$t("ui.data.column.factoryCode"),
          type: "select",
          dictData: this.parentDict.type.biz_factory_name,
          filterable: true,
        },
        {
          prop: "lhResultBatchNo",
          label: this.$t("ui.data.column.lhMouldChangePlan.lhResultBatchNo"),
          disabled: this.isEdit,
          maxlength: 64,
        },
        {
          prop: "orderNo",
          label: this.$t("ui.data.column.lhMouldChangePlan.orderNo"),
          disabled: true,
          placeholder: "系统默认生成",
          maxlength: 64,
        },
        {
          prop: "planDate",
          label: this.$t("ui.data.column.lhMouldChangePlan.planDate"),
          type: "date",
          valueFormat: "yyyy-MM-dd",
        },
        {
          prop: "scheduleDate",
          label: this.$t("ui.data.column.lhMouldChangePlan.scheduleDate"),
          type: "date",
          valueFormat: "yyyy-MM-dd",
        },
        {
          prop: "planOrder",
          label: this.$t("ui.data.column.lhMouldChangePlan.planOrder"),
          render: (form) => {
            return (
              <el-input
                value={form.planOrder}
                clearable
                maxlength={5}
                placeholder={this.$t("common.rule.input")}
                onInput={(value) => {
                  this.handlePlanOrderInput(value);
                }}
              />
            );
          },
        },
        {
          prop: "classIndex",
          label: this.$t("ui.data.column.lhMouldChangePlan.classIndex"),
          type: "select",
          dictData: this.parentDict.type.class_num_two_mm,
          filterable: true,
        },
        {
          prop: "leftRightMould",
          label: this.$t("ui.data.column.lhMouldChangePlan.leftRightMould"),
          type: "select",
          dictData: this.parentDict.type.lr_molds,
          filterable: true,
        },
        {
          prop: "lhMachineCode",
          label: this.$t("ui.data.column.lhMouldChangePlan.lhMachineCode"),
          type: "select",
          dictData: this.machineOptions,
          filterable: true,
          listeners: {
            change: this.handleMachineChange,
          },
        },
        {
          prop: "beforeMaterialCode",
          label: this.$t("ui.data.column.lhMouldChangePlan.beforeMaterialCode"),
          render: (form) => {
            return (
              <materialCodeSelect
                key={form.beforeMaterialCode}
                v-model={form.beforeMaterialCode}
                onChange={this.handleBeforeMaterialChange}
              />
            );
          },
        },
        {
          prop: "beforeMaterialDesc",
          label: this.$t("ui.data.column.lhMouldChangePlan.beforeMaterialDesc"),
          disabled: true,
          maxlength: 128,
        },
        {
          prop: "changeMouldType",
          label: this.$t("ui.data.column.lhMouldChangePlan.changeMouldType"),
          type: "select",
          dictData: this.parentDict.type.CHANGE_MOULD_TYPE,
          filterable: true,
        },
        {
          prop: "afterMaterialCode",
          label: this.$t("ui.data.column.lhMouldChangePlan.afterMaterialCode"),
          render: (form) => {
            return (
              <materialCodeSelect
                key={form.afterMaterialCode}
                v-model={form.afterMaterialCode}
                onChange={this.handleAfterMaterialChange}
              />
            );
          },
        },
        {
          prop: "afterMaterialDesc",
          label: this.$t("ui.data.column.lhMouldChangePlan.afterMaterialDesc"),
          disabled: true,
          maxlength: 128,
        },
        {
          prop: "changeTime",
          label: this.$t("ui.data.column.lhMouldChangePlan.changeTime"),
          type: "date",
          valueFormat: "yyyy-MM-dd",
        },
        {
          prop: "mouldCode",
          label: this.$t("ui.data.column.lhMouldChangePlan.mouldCode"),
          maxlength: 30,
        },
        {
          prop: "isRelease",
          label: this.$t("ui.data.column.lhMouldChangePlan.isRelease"),
          type: "select",
          dictData: this.parentDict.type.IS_RELEASE,
          filterable: true,
          disabled: true,
        },
        {
          prop: "mouldStatus",
          label: this.$t("ui.data.column.lhMouldChangePlan.mouldStatus"),
          type: "select",
          dictData: this.parentDict.type.finish_completion,
          filterable: true,
          labelWidth: "150px",
          disabled: true,
        },
        {
          prop: "remark",
          label: this.$t("common.remark"),
          type: "textarea",
          span: 24,
          rows: 3,
          maxlength: 500,
        },
      ];
      return columns.map((item) => ({
        span: 12,
        ...item,
      }));
    },
  },
  methods: {
    async getMachineList() {
      try {
        const res = await listMachine(this.form.factoryCode ? { factoryCode: this.form.factoryCode } : {});
        const list = res.rows || [];
        const map = new Map();
        if (this.isEdit && this.form.lhMachineCode) {
          map.set(this.form.lhMachineCode, {
            label: this.form.lhMachineCode,
            value: this.form.lhMachineCode,
            machineCode: this.form.lhMachineCode,
            machineName: this.form.lhMachineName || this.form.lhMachineCode,
          });
        }
        list.forEach((item) => {
          if (item && item.machineCode) {
            map.set(item.machineCode, {
              label: item.machineCode,
              value: item.machineCode,
              machineCode: item.machineCode,
              machineName: item.machineName,
            });
          }
        });
        this.machineOptions = Array.from(map.values());
      } catch (error) {
        this.machineOptions = [];
        console.log(error);
      }
    },
    handleMachineChange(val) {
      if (val) {
        const item = this.machineOptions.find((i) => i.value === val);
        if (item) {
          this.$set(this.form, "lhMachineName", item.machineName || val);
        }
      } else {
        this.$set(this.form, "lhMachineName", "");
      }
    },
    handleBeforeMaterialChange(val, row) {
      if (val) {
        this.$set(this.form, "beforeMaterialDesc", (row && row.materialDesc) || "");
      } else {
        this.$set(this.form, "beforeMaterialDesc", "");
      }
    },
    handleAfterMaterialChange(val, row) {
      if (val) {
        this.$set(this.form, "afterMaterialDesc", (row && row.materialDesc) || "");
      } else {
        this.$set(this.form, "afterMaterialDesc", "");
      }
    },
    handlePlanOrderInput(val) {
      const value = (val || "").replace(/\D/g, "").slice(0, 5);
      this.$set(this.form, "planOrder", value);
    },

    // api
    async save(params) {
      try {
        this.loading = true;
        if (params.planOrder !== undefined && params.planOrder !== null && params.planOrder !== "") {
          params.planOrder = Number(params.planOrder);
        }
        if (this.isEdit && this.originalIsRelease === "1") {
          params.isRelease = "5";
          params.mouldStatus = "0";
        }

        const res = await editLhMouldChangePlan(params);
        this.$modal.msgSuccess(res.msg);
        this.$emit("success");
        this.hide();
      } catch (error) {
        console.log(error);
      } finally {
        this.loading = false;
      }
    },

    // utils
    show(data) {
      this.visible = true;
      if (data) {
        this.isEdit = true;
        this.originalIsRelease = data.isRelease || "";
        this.form = { ...data };
        if (data.lhMachineCode) {
          this.machineOptions = [
            {
              label: data.lhMachineCode,
              value: data.lhMachineCode,
              machineCode: data.lhMachineCode,
              machineName: data.lhMachineName || data.lhMachineCode,
            },
          ];
        }
      } else {
        this.isEdit = false;
        this.originalIsRelease = "";
        this.form = {
          factoryCode: "116",
          isRelease: "0",
          mouldStatus: "0",
        };
        this.machineOptions = [];
      }
      this.getMachineList();
    },
    hide() {
      this.form = {};
      this.machineOptions = [];
      this.$refs.form && this.$refs.form.triggerResetForm();
      this.isEdit = false;
      this.originalIsRelease = "";
      this.visible = false;
    },
    handleConfirm() {
      this.$refs.form.triggerConfirm(this.save);
    },
  },
};
</script>
