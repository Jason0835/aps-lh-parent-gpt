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
import { saveStockShiftConfig } from "@/api/tq/stockShiftConfig";

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
            trigger: "change",
          },
        ],
        minMachineQty: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
          {
            validator: (rule, value, callback) => {
              if (value !== '' && value !== null && value !== undefined && Number(value) <= 0) {
                callback(new Error(this.$t("ui.tq.depthConfig.positiveInteger")));
              } else {
                callback();
              }
            },
            trigger: 'blur',
          },
        ],
        maxMachineQty: [
          {
            validator: (rule, value, callback) => {
              if (value !== '' && value !== null && value !== undefined && Number(value) <= 0) {
                callback(new Error(this.$t("ui.tq.depthConfig.positiveInteger")));
              } else {
                callback();
              }
            },
            trigger: 'blur',
          },
        ],
        depthClassQty: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
          {
            validator: (rule, value, callback) => {
              if (value !== '' && value !== null && value !== undefined && Number(value) <= 0) {
                callback(new Error(this.$t("ui.tq.depthConfig.positiveInteger")));
              } else {
                callback();
              }
            },
            trigger: 'blur',
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
        this.$t("ui.tq.depthConfig.column.modalName")
      );
    },
    columns() {
      return [
        {
          label: this.$t("ui.data.column.factoryCode"),
          prop: "factoryCode",
          span: 24,
          type: "select",
          dictData: this.parentDict.type.biz_factory_name,
          filterable: true,
          required: true,
          disabled: this.isEdit,
        },
        {
          label: this.$t("ui.tq.depthConfig.column.minMachineQty"),
          prop: "minMachineQty",
          span: 24,
          type: "number",
          min: 1,
          precision: 0,
          required: true,
        },
        {
          label: this.$t("ui.tq.depthConfig.column.maxMachineQty"),
          prop: "maxMachineQty",
          span: 24,
          type: "input",
          tips: this.$t("ui.tq.depthConfig.maxMachineQtyTip"),
        },
        {
          label: this.$t("ui.tq.depthConfig.column.depthClassQty"),
          prop: "depthClassQty",
          span: 24,
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
          maxlength: "500",
        },
      ];
    },
  },
  methods: {
    /**
     * 保存配置
     */
    async save(params) {
      try {
        this.loading = true;
        // 将空字符串的 maxMachineQty 转为 null 提交
        if (params.maxMachineQty === '') {
          params.maxMachineQty = null;
        }
        const res = await saveStockShiftConfig(params);
        this.$modal.msgSuccess(res.msg);
        this.$emit("success");
        this.hide();
      } catch (error) {
        console.log(error);
      } finally {
        this.loading = false;
      }
    },
    /**
     * 显示弹窗
     * @param {Object} data 编辑数据，不传为新增
     * @param {String} defaultFactoryCode 默认分厂编码
     */
    show(data, defaultFactoryCode) {
      this.visible = true;
      if (data) {
        this.isEdit = true;
        this.form = {
          ...data,
          maxMachineQty: data.maxMachineQty != null ? data.maxMachineQty : '',
        };
      } else {
        this.isEdit = false;
        this.form = {
          factoryCode: defaultFactoryCode || "",
        };
      }
    },
    /**
     * 隐藏弹窗
     */
    hide() {
      this.form = {};
      this.$refs.form.triggerResetForm();
      this.isEdit = false;
      this.visible = false;
    },
    /**
     * 确认保存（区间连续性校验由后端 save 时统一校验）
     */
    handleConfirm() {
      this.$refs.form.triggerConfirm(this.save);
    },
  },
};
</script>
