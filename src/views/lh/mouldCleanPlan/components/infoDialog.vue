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
import { editMouldCleanPlan, getMachineList } from "@/api/lh/mouldCleanPlan";

import infoForm from "@/views/components/infoForm.vue";

export default {
  components: { infoForm },
  inject: ["parentDict"],
  data() {
    return {
      loading: false,
      machineLoading: false,
      machineOptions: [],
      visible: false,
      isEdit: false,
      form: {},
      rules: {
        factoryCode: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
        lhCode: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
        cleanTime: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
        cleanType: [
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
          disabled: this.isEdit,
        },
        {
          prop: "lhCode",
          label: this.$t("ui.data.column.mouldCleanPlan.lhCode"),
          type: "select",
          dictData: this.machineOptions,
          props: {
            label: "machineCode",
            value: "machineCode",
          },
          filterable: true,
          loading: this.machineLoading,
          onFocus: this.handleMachineFocus,
          disabled: this.isEdit,
        },
        {
          prop: "cleanTime",
          label: this.$t("ui.data.column.mouldCleanPlan.cleanTime"),
          type: "date",
          valueFormat: "yyyy-MM-dd",
        },
        {
          prop: "cleanType",
          label: this.$t("ui.data.column.mouldCleanPlan.cleanType"),
          type: "select",
          dictData: this.parentDict.type.MOULD_CLEAN_TYPE,
          disabled: this.isEdit,
        },
        {
          prop: "remark",
          label: this.$t("ui.data.column.mouldCleanPlan.remark"),
          type: "textarea",
          maxlength: 360,
          showWordLimit: true,
        },
      ];
    },
  },
  methods: {
    async loadMachineList() {
      this.machineLoading = true;
      try {
        const res = await getMachineList({
          machineCode: "",
          pageSize: 1000,
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
        this.loadMachineList();
      }
    },
    async save(params) {
      try {
        this.loading = true;

        const res = await editMouldCleanPlan(params);
        this.$modal.msgSuccess(res.msg);
        this.$emit("success");
        this.hide();
      } catch (error) {
        console.log(error);
      } finally {
        this.loading = false;
      }
    },

    show(data) {
      this.visible = true;
      this.machineOptions = [];
      if (data) {
        this.isEdit = true;
        this.form = { ...data };
        if (data.lhCode) {
          this.machineOptions = [
            {
              machineCode: data.lhCode,
              machineName: data.lhCode,
            },
          ];
        }
      } else {
        this.form = {};
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
