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
      label-width="160px"
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
import { editLhMouldChangePlan, getMachineList, getMaterialList } from "@/api/lh/lhMouldChangePlan";

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
          maxlength: 32,
        },
        {
          prop: "orderNo",
          label: this.$t("ui.data.column.lhMouldChangePlan.orderNo"),
          maxlength: 32,
        },
        {
          prop: "planDate",
          label: this.$t("ui.data.column.lhMouldChangePlan.planDate"),
          type: "date",
          valueFormat: "yyyy-MM-dd",
          listeners: {
          },
        },
        {
          prop: "scheduleDate",
          label: this.$t("ui.data.column.lhMouldChangePlan.scheduleDate"),
          type: "date",
          valueFormat: "yyyy-MM-dd",
          listeners: {
          },
        },
        {
          prop: "planOrder",
          label: this.$t("ui.data.column.lhMouldChangePlan.planOrder"),
          type: "number",
        },
        {
          prop: "leftRightMould",
          label: this.$t("ui.data.column.lhMouldChangePlan.leftRightMould"),
          maxlength: 4,
        },
        {
          prop: "lhMachineCode",
          label: this.$t("ui.data.column.lhMouldChangePlan.lhMachineCode"),
          type: "select",
          filterable: true,
          remote: true,
          remoteMethod: this.remoteMachineMethod,
          loading: this.machineLoading,
          clearable: true,
          props: {
            label: "machineCode",
            value: "machineCode",
          },
          options: this.machineOptions,
          onFocus: this.handleMachineFocus,
        },
        {
          prop: "lhMachineName",
          label: this.$t("ui.data.column.lhMouldChangePlan.lhMachineName"),
          maxlength: 100,
          disabled: true,
        },
        {
          prop: "beforeMaterialCode",
          label: this.$t("ui.data.column.lhMouldChangePlan.beforeMaterialCode"),
          type: "select",
          filterable: true,
          remote: true,
          remoteMethod: this.remoteBeforeMaterialMethod,
          loading: this.beforeMaterialLoading,
          clearable: true,
          props: {
            label: "materialCode",
            value: "materialCode",
          },
          options: this.beforeMaterialOptions,
          onFocus: this.handleBeforeMaterialFocus,
        },
        {
          prop: "beforeMaterialDesc",
          label: this.$t("ui.data.column.lhMouldChangePlan.beforeMaterialDesc"),
          maxlength: 255,
          disabled: true,
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
          filterable: true,
          remote: true,
          remoteMethod: this.remoteAfterMaterialMethod,
          loading: this.afterMaterialLoading,
          clearable: true,
          props: {
            label: "materialCode",
            value: "materialCode",
          },
          options: this.afterMaterialOptions,
          onFocus: this.handleAfterMaterialFocus,
        },
        {
          prop: "afterMaterialDesc",
          label: this.$t("ui.data.column.lhMouldChangePlan.afterMaterialDesc"),
          maxlength: 255,
          disabled: true,
        },
        {
          prop: "changeTime",
          label: this.$t("ui.data.column.lhMouldChangePlan.changeTime"),
          type: "datetime",
          valueFormat: "yyyy-MM-dd HH:mm:ss",
          listeners: {
          },
        },
        {
          prop: "mouldCode",
          label: this.$t("ui.data.column.lhMouldChangePlan.mouldCode"),
          maxlength: 32,
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
          maxlength: 500,
          rows: 3,
        },
      ];
    },
  },
  methods: {
    async save(params) {
      try {
        this.loading = true;
        const res = await editLhMouldChangePlan(params);
        this.$modal.msgSuccess(res.msg);
        this.$emit("success");
        this.hide();
      } catch (error) {
        console.log(error);
        this.loading = false;
      } finally {
        this.loading = false;
      }
    },
    async remoteMachineMethod(query) {
      this.machineLoading = true;
      try {
        const res = await getMachineList({
          machineCode: query || "",
          pageSize: 10,
        });
        this.machineOptions = res.data || res || [];
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
        this.beforeMaterialOptions = res.data || res || [];
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
        this.afterMaterialOptions = res.data || res || [];
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
    show(data) {
      this.visible = true;
      if (data) {
        this.isEdit = true;
        this.form = {
          ...data,
        };
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
        this.form = {};
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
      if (this.$refs.form) {
        this.$refs.form.triggerResetForm();
      }
      this.isEdit = false;
      this.visible = false;
    },
    handleConfirm() {
      this.$refs.form.triggerConfirm(this.save);
    },
  },
};
</script>
