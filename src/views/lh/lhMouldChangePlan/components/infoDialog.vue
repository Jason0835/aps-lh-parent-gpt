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
  getMaterialList,
} from "@/api/lh/lhMouldChangePlan";

import infoForm from "@/views/components/infoForm.vue";

export default {
  components: { infoForm },
  inject: ["parentDict"],
  data() {
    return {
      loading: false,
      machineLoading: false,
      beforeMaterialLoading: false,
      afterMaterialLoading: false,
      visible: false,
      isEdit: false,
      form: {},
      machineOptions: [],
      beforeMaterialOptions: [],
      afterMaterialOptions: [],
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
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
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
          type: "select",
          dictData: this.beforeMaterialOptions,
          props: {
            label: "materialCode",
            value: "materialCode",
          },
          filterable: true,
          remote: true,
          remoteMethod: this.remoteBeforeMaterialMethod,
          loading: this.beforeMaterialLoading,
          onFocus: this.handleBeforeMaterialFocus,
          listeners: {
            change: this.handleBeforeMaterialChange,
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
          type: "select",
          dictData: this.afterMaterialOptions,
          props: {
            label: "materialCode",
            value: "materialCode",
          },
          filterable: true,
          remote: true,
          remoteMethod: this.remoteAfterMaterialMethod,
          loading: this.afterMaterialLoading,
          onFocus: this.handleAfterMaterialFocus,
          listeners: {
            change: this.handleAfterMaterialChange,
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
        },
        {
          prop: "mouldStatus",
          label: this.$t("ui.data.column.lhMouldChangePlan.mouldStatus"),
          type: "select",
          dictData: this.parentDict.type.biz_yes_no,
          filterable: true,
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
    async remoteBeforeMaterialMethod(query) {
      this.beforeMaterialLoading = true;
      try {
        const res = await getMaterialList({
          materialCode: query || "",
          pageSize: 10,
        });
        this.beforeMaterialOptions = res || [];
      } catch (error) {
        console.log(error);
      } finally {
        this.beforeMaterialLoading = false;
      }
    },
    handleBeforeMaterialFocus() {
      if (this.beforeMaterialOptions.length === 0) {
        this.remoteBeforeMaterialMethod("");
      }
    },
    async remoteAfterMaterialMethod(query) {
      this.afterMaterialLoading = true;
      try {
        const res = await getMaterialList({
          materialCode: query || "",
          pageSize: 10,
        });
        this.afterMaterialOptions = res || [];
      } catch (error) {
        console.log(error);
      } finally {
        this.afterMaterialLoading = false;
      }
    },
    handleAfterMaterialFocus() {
      if (this.afterMaterialOptions.length === 0) {
        this.remoteAfterMaterialMethod("");
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
    handleBeforeMaterialChange(val) {
      if (val) {
        const item = this.beforeMaterialOptions.find((i) => i.materialCode === val);
        if (item) {
          this.$set(this.form, "beforeMaterialDesc", item.materialDesc || val);
        }
      } else {
        this.$set(this.form, "beforeMaterialDesc", "");
      }
    },
    handleAfterMaterialChange(val) {
      if (val) {
        const item = this.afterMaterialOptions.find((i) => i.materialCode === val);
        if (item) {
          this.$set(this.form, "afterMaterialDesc", item.materialDesc || val);
        }
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
        if (data.lhMachineCode) {
          this.machineOptions = [
            {
              machineCode: data.lhMachineCode,
              machineName: data.lhMachineName || data.lhMachineCode,
            },
          ];
        }
        if (data.beforeMaterialCode) {
          this.beforeMaterialOptions = [
            {
              materialCode: data.beforeMaterialCode,
            },
          ];
        }
        if (data.afterMaterialCode) {
          this.afterMaterialOptions = [
            {
              materialCode: data.afterMaterialCode,
            },
          ];
        }
      } else {
        this.isEdit = false;
        this.form = {
          factoryCode: "116",
        };
        this.machineOptions = [];
        this.beforeMaterialOptions = [];
        this.afterMaterialOptions = [];
      }
    },
    hide() {
      this.form = {};
      this.machineOptions = [];
      this.beforeMaterialOptions = [];
      this.afterMaterialOptions = [];
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
