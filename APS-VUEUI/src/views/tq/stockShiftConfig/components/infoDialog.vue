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
import {
  saveStockShiftConfig,
  checkStockShiftConfigUnique,
  checkStockShiftConfigRangeCross,
} from "@/api/tq/stockShiftConfig";

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
        machineRange: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "change",
          },
        ],
        machineCount: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        shiftCount: [
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
      return (
        (this.isEdit
          ? this.$t("common.button.edit")
          : this.$t("common.button.add")) +
        this.$t("ui.data.column.tq.stockShiftConfig.modelName")
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
          label: this.$t("ui.data.column.stockShiftConfig.machineRange"),
          prop: "machineRange",
          span: 24,
          type: "select",
          dictData: this.parentDict.type.machine_range,
          filterable: true,
          required: true,
          disabled: this.isEdit,
        },
        {
          label: this.$t("ui.data.column.stockShiftConfig.machineCount"),
          prop: "machineCount",
          span: 24,
          type: "number",
          min: 1,
          max: 999,
          precision: 0,
          required: true,
          disabled: this.isEdit,
        },
        {
          label: this.$t("ui.data.column.stockShiftConfig.shiftCount"),
          prop: "shiftCount",
          span: 24,
          type: "number",
          min: 1,
          max: 99,
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
     */
    show(data) {
      this.visible = true;
      if (data) {
        this.isEdit = true;
        this.form = {
          ...data,
        };
      } else {
        this.isEdit = false;
        this.form = {
          factoryCode: "116", // 工厂默认值为"越南"
          machineRange: "EQ", // 机台范围默认值为"等于"
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
     * 校验唯一性
     */
    checkUnique() {
      return new Promise((resolve, reject) => {
        checkStockShiftConfigUnique({
          id: this.form.id,
          factoryCode: this.form.factoryCode,
          machineRange: this.form.machineRange,
          machineCount: this.form.machineCount,
        })
          .then((res) => {
            // 后端返回 UserConstants.UNIQUE=0（唯一）/ NOT_UNIQUE=1（不唯一）
            // 响应拦截器返回的是数字类型，使用 === 0 严格比较
            if (res === 0) {
              resolve();
            } else {
              reject(
                new Error(
                  this.$t("ui.error.message.stockShiftConfig.unique") ||
                    "该分厂下此机台数已存在配置"
                )
              );
            }
          })
          .catch((error) => {
            console.error(error);
            reject(new Error(this.$t("验证失败，请稍后再试")));
          });
      });
    },
    /**
     * 校验配置规则交叉
     */
    checkRangeCross() {
      return new Promise((resolve, reject) => {
        checkStockShiftConfigRangeCross({
          id: this.form.id,
          factoryCode: this.form.factoryCode,
          machineRange: this.form.machineRange,
          machineCount: this.form.machineCount,
        })
          .then((res) => {
            // 后端返回 UserConstants.UNIQUE=0（无交叉）/ NOT_UNIQUE=1（存在交叉）
            // 响应拦截器返回的是数字类型，使用 === 0 严格比较
            console.log(res);
            if (res === 0) {
              resolve();
            } else {
              reject(
                new Error(
                  this.$t("ui.data.column.tq.stockShiftConfig.rangeCross") ||
                    "配置规则存在交叉"
                )
              );
            }
          })
          .catch((error) => {
            console.error(error);
            reject(new Error(this.$t("验证失败，请稍后再试")));
          });
      });
    },
    /**
     * 确认保存
     */
    handleConfirm() {
      this.$refs.form.triggerConfirm(async (params) => {
        Object.keys(params).forEach((key) => {
          if (this.isEmpty(params[key])) {
            params[key] = "";
          }
        });

        try {
          this.loading = true;
          await this.checkUnique();
          await this.checkRangeCross();
          this.save(params);
        } catch (error) {
          console.error(error);
          this.$modal.msgError(error.message);
          this.loading = false;
        }
      });
    },
  },
};
</script>
