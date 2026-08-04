<template>
  <el-dialog
    :append-to-body="true"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :title="title"
    :visible="visible"
    width="800px"
    @close="hide"
  >
    <info-form
      ref="form"
      v-loading="loading"
      :columns="columns"
      :form="form"
      :rules="rules"
      class="form-item-height"
      label-position="right"
      label-width="160px"
    >
    </info-form>
    <template slot="footer">
      <el-button @click="hide">{{ this.$t("common.button.cancel") }}</el-button>
      <el-button :loading="loading" type="primary" @click="handleConfirm">{{
        this.$t("common.button.confirm")
      }}</el-button>
    </template>
  </el-dialog>
</template>

<script>
import infoForm from "@/views/components/infoForm.vue";
import {saveDepthConfig} from "@/api/tc/depthConfig";

export default {
  components: { infoForm },
  inject: ["parentDict"],
  data() {
    return {
      loading: false,
      visible: false,
      isEdit: false,
      form: {},
      rules: {
        factoryCode: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        minMachineQty: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
          {
            validator: this.validatePositiveInteger,
            trigger: "blur",
          },
        ],
        maxMachineQty: [
          {
            validator: this.validateMaxMachineQty,
            trigger: "blur",
          },
        ],
        depthClassQty: [
          {
            validator: this.validatePositiveInteger,
            trigger: "blur",
          },
        ],
      },
      columns: [
        {
          label: this.$t("ui.data.column.factoryCode"),
          prop: "factoryCode",
          span: 12,
          type: "select",
          dictData: this.parentDict.type.biz_factory_name,
          filterable: true,
          required: true,
        },
        {
          label: this.$t("ui.tc.depthConfig.column.minMachineQty"),
          prop: "minMachineQty",
          span: 12,
          type: "number",
          min: 1,
          required: true,
        },
        {
          label: this.$t("ui.tc.depthConfig.column.maxMachineQty"),
          prop: "maxMachineQty",
          span: 12,
          type: "input",
          tips: this.$t("ui.tc.depthConfig.maxMachineQtyTip"),
        },
        {
          label: this.$t("ui.tc.depthConfig.column.depthClassQty"),
          prop: "depthClassQty",
          span: 12,
          type: "number",
          min: 1,
          precision: 0,
          required: true,
        },
        {
          label: this.$t("ui.common.column.remark"),
          prop: "remark",
          span: 24,
          type: "textarea",
          maxlength: "300",
        },
      ],
    };
  },
  computed: {
    title: function () {
      return (
        (this.isEdit
          ? this.$t("common.button.edit")
          : this.$t("common.button.add")) +
        this.$t("ui.tc.depthConfig.column.modalName")
      );
    },
  },
  methods: {
    validatePositiveInteger(rule, value, callback) {
      if (value === "" || value === null || value === undefined) {
        callback(new Error(this.$t("common.rule.input")));
        return;
      }
      if (!Number.isInteger(Number(value)) || Number(value) <= 0) {
        callback(new Error(this.$t("ui.tc.depthConfig.positiveInteger")));
        return;
      }
      callback();
    },
    validateMaxMachineQty(rule, value, callback) {
      if (value === "" || value === null || value === undefined) {
        callback();
        return;
      }
      if (!Number.isInteger(Number(value)) || Number(value) <= 0) {
        callback(new Error(this.$t("ui.tc.depthConfig.positiveInteger")));
        return;
      }
      if (this.form.minMachineQty && Number(value) < Number(this.form.minMachineQty)) {
        callback(new Error(this.$t("ui.tc.depthConfig.maxLessThanMin")));
        return;
      }
      callback();
    },
    async save(params) {
      try {
        this.loading = true;
        if (params.maxMachineQty === "") {
          params.maxMachineQty = null;
        }
        let res;
        res = await saveDepthConfig(params);
        this.$modal.msgSuccess(res.msg);
        this.$emit("success");
        this.hide();
        this.loading = false;
      } catch (error) {
        console.log(error);
        this.loading = false;
      }
    },
    show(data, defaultFactoryCode) {
      this.visible = true;
      if (data) {
        this.isEdit = true;
        this.form = {
          ...data,
          maxMachineQty: data.maxMachineQty != null ? data.maxMachineQty : "",
        };
      } else {
        this.isEdit = false;
        this.form = {
          factoryCode: defaultFactoryCode || '',
        };
      }
    },
    hide() {
      this.form = {};
      this.$refs.form.triggerResetForm();
      this.isEdit = false;
      this.visible = false;
    },
    handleConfirm() {
      this.$refs.form.triggerConfirm(this.save);
    },
  },
};
</script>
