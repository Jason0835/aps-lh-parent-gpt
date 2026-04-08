<template>
  <el-dialog
    :title="title"
    :visible="visible"
    width="600px"
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
  editLhSpecifyMachine,
  getLhMachineList,
  getMaterialList,
} from "@/api/lh/lhSpecifyMachine";

import infoForm from "@/views/components/infoForm.vue";

export default {
  components: { infoForm },
  inject: ["parentDict"],
  data() {
    return {
      loading: false,
      machineLoading: false,
      materialLoading: false,
      visible: false,
      isEdit: false,
      form: {},
      machineOptions: [],
      materialOptions: [],
      rules: {
        factoryCode: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
        specCode: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
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
      return this.$t("ui.data.column.lhSpecifyMachine.modelName");
    },
    columns() {
      return [
        {
          prop: "factoryCode",
          label: this.$t("ui.data.column.factoryCode"),
          type: "select",
          dictData: this.parentDict.type.biz_factory_name,
        },
        {
          prop: "specCode",
          label: this.$t("ui.data.column.lhSpecifyMachine.specCode"),
          type: "select",
          dictData: this.materialOptions,
          props: {
            label: "materialCode",
            value: "materialCode",
          },
          filterable: true,
          remote: true,
          remoteMethod: this.remoteMaterialMethod,
          loading: this.materialLoading,
          onFocus: this.handleMaterialFocus,
        },
        {
          prop: "machineCode",
          label: this.$t("ui.data.column.lhSpecifyMachine.machineCode"),
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
        },
        {
          prop: "jobType",
          label: this.$t("ui.data.column.lhSpecifyMachine.jobType"),
          type: "select",
          dictData: this.parentDict.type.JOB_TYPE,
        },
        {
          prop: "remark",
          label: this.$t("ui.common.column.remark"),
          type: "textarea",
          rows: 3,
          maxlength: 300,
        },
      ];
    },
  },
  methods: {
    async save(params) {
      try {
        this.loading = true;

        const res = await editLhSpecifyMachine(params);
        this.$modal.msgSuccess(res.msg);
        this.$emit("success");
        this.hide();
      } catch (error) {
        console.log(error);
      } finally {
        this.loading = false;
      }
    },
    async remoteMachineMethod(query) {
      this.machineLoading = true;
      try {
        const res = await getLhMachineList({
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
    async remoteMaterialMethod(query) {
      this.materialLoading = true;
      try {
        const res = await getMaterialList({
          materialCode: query || "",
          pageSize: 10,
        });
        this.materialOptions = res.data || res || [];
      } catch (error) {
        console.log(error);
      } finally {
        this.materialLoading = false;
      }
    },
    handleMaterialFocus() {
      if (this.materialOptions.length === 0) {
        this.remoteMaterialMethod("");
      }
    },
    show(data) {
      this.visible = true;
      if (data) {
        this.isEdit = true;
        this.form = { ...data };
        if (data.machineCode) {
          this.machineOptions = [
            {
              machineCode: data.machineCode,
              machineName: data.machineName || data.machineCode,
            },
          ];
        }
        if (data.specCode) {
          this.materialOptions = [
            {
              materialCode: data.specCode,
            },
          ];
        }
      } else {
        this.form = {};
        this.machineOptions = [];
        this.materialOptions = [];
      }
    },
    hide() {
      this.form = {};
      this.machineOptions = [];
      this.materialOptions = [];
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
