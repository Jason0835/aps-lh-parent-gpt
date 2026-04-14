<template>
  <el-dialog
    :title="title"
    :visible="visible"
    width="800px"
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
import {
  editLhMouldChangePlan,
  getMachineList,
} from "@/api/lh/lhMouldChangePlan";

import infoForm from "@/views/components/infoForm.vue";
import materialCodeSelect from "@/views/components/materialCodeSelect.vue";

export default {
  components: { infoForm, materialCodeSelect },
  inject: ["parentDict"],
  data() {
    return {
      loading: false,
      machineLoading: false,
      visible: false,
      isEdit: false,
      form: {},
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
      return [
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
          type: "number",
          min: 0,
          precision: 0,
        },
        {
          prop: "leftRightMould",
          label: this.$t("ui.data.column.lhMouldChangePlan.leftRightMould"),
          maxlength: 32,
        },
        {
          prop: "lhMachineCode",
          label: this.$t("ui.data.column.lhMouldChangePlan.lhMachineCode"),
          type: "select",
          dictData: this.machineOptions,
          props: {
            label: "machineCode",
            value: "machineCode",
          },
          filterable: true,
          remote: true,
          remoteMethod: this.remoteMachineMethod,
          loading: this.machineLoading,
          onFocus: this.handleMachineFocus,
          listeners: {
            change: this.handleMachineChange,
          },
        },
        {
          prop: "lhMachineName",
          label: this.$t("ui.data.column.lhMouldChangePlan.lhMachineName"),
          disabled: true,
          maxlength: 64,
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
          dateType: "datetime",
          valueFormat: "yyyy-MM-dd HH:mm:ss",
        },
        {
          prop: "mouldCode",
          label: this.$t("ui.data.column.lhMouldChangePlan.mouldCode"),
          maxlength: 64,
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
          dictData: this.parentDict.type.biz_yes_no,
          filterable: true,
          disabled: true,
        },
        {
          prop: "remark",
          label: this.$t("common.remark"),
          type: "textarea",
          rows: 3,
          maxlength: 500,
        },
      ];
    },
  },
  methods: {
    async remoteMachineMethod(query) {
      this.machineLoading = true;
      try {
        const res = await getMachineList({
          machineCode: query || "",
          pageSize: 10,
        });
        this.machineOptions = res || [];
      } catch (error) {
        console.log(error);
      } finally {
        this.machineLoading = false;
      }
    },
    handleMachineFocus() {
      if (this.machineOptions.length === 0) {
        this.remoteMachineMethod("");
      }
    },
    handleMachineChange(val) {
      if (val) {
        const item = this.machineOptions.find((i) => i.machineCode === val);
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

    // api
    async save(params) {
      try {
        this.loading = true;

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
        this.form = { ...data };
        if (data.isRelease === "1") {
          this.$set(this.form, "isRelease", "4");
          this.$set(this.form, "mouldStatus", "0");
        }
        if (data.lhMachineCode) {
          this.machineOptions = [
            {
              machineCode: data.lhMachineCode,
              machineName: data.lhMachineName || data.lhMachineCode,
            },
          ];
        }
      } else {
        this.isEdit = false;
        this.form = {
          factoryCode: "116",
          isRelease: "0",
          mouldStatus: "0",
        };
        this.machineOptions = [];
      }
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
