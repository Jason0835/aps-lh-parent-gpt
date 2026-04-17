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
        ? "编辑"
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
          dateType: "datetime",
          valueFormat: "yyyy-MM-dd HH:mm:ss",
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
          rows: 3,
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
        const saveParams = {
          id: params.id,
          factoryCode: params.factoryCode,
          companyCode: params.companyCode,
          lhCode: params.lhCode,
          cleanTime: params.cleanTime,
          cleanType: params.cleanType,
          leftRightMould: params.leftRightMould,
          remark: encodeURIComponent(params.remark),
          dataSource: params.dataSource,
          dataVersion: params.dataVersion
        };
        console.log('=== 保存参数完整信息 ===');
        console.log('保存参数:', JSON.stringify(saveParams));
        console.log('是否编辑模式:', this.isEdit);
        console.log('ID值:', saveParams.id, '类型:', typeof saveParams.id);
        if (this.isEdit && !saveParams.id) {
          this.$modal.msgError('编辑模式下缺少ID，无法保存');
          return;
        }
        const res = await editMouldCleanPlan(saveParams);
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
        console.log('=== 编辑模式 ===');
        console.log('原始数据:', JSON.stringify(data));
        console.log('原始数据ID:', data.id, '类型:', typeof data.id);
        this.form = { ...data };
        console.log('复制后form ID:', this.form.id, '类型:', typeof this.form.id);
        if (data.lhCode) {
          this.machineOptions = [
            {
              machineCode: data.lhCode,
              machineName: data.lhCode,
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

