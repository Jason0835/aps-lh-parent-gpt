<template>
  <el-dialog
    :title="title"
    :visible="visible"
    width="400px"
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
import moment from "moment";

import infoForm from "@/views/components/infoForm.vue";
import { autoPlan } from "@/api/lh/scheduleResult";

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
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
        scheduleDate: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "blur",
          },
        ],
      },
      columns: [
        {
          label: this.$t("ui.data.column.factoryCode"),
          prop: "factoryCode",
          type: "select",
          dictData: this.parentDict.type.biz_factory_name,
          filterable: true,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.scheduleDate"),
          prop: "scheduleDate",
          type: "date",
          dateType: "date",
          valueFormat: "yyyy-MM-dd 00:00:00",
          format: "yyyy-MM-dd",
          clearable: false,
        },
      ],
    };
  },
  computed: {
    title: function () {
      return this.$t("ui.data.column.cxScheduleResult.lhAutoPlan");
    },
  },
  methods: {
    /**
     * 判断排程响应是否包含可展示的校验明细
     * @param {Object} data 接口响应
     * @returns {boolean}
     */
    hasScheduleValidationDetails(data) {
      return (
        (data.validationErrors &&
          Array.isArray(data.validationErrors) &&
          data.validationErrors.length > 0) ||
        (data.validationErrorDetails &&
          Array.isArray(data.validationErrorDetails) &&
          data.validationErrorDetails.length > 0)
      );
    },
    /**
     * 处理硫化自动排程接口响应（后台异步回调）
     * @param {Object} data 接口响应
     * @param {Object} params 排程参数
     */
    handleAutoPlanResponse(data, params) {
      const tip = data.message || data.msg || "";
      if (data.success === false) {
        if (this.hasScheduleValidationDetails(data)) {
          this.$emit("validationError", data);
        } else {
          this.$modal.msgError(tip || this.$t("ui.data.btn.ajax.code.msg"));
        }
        return;
      }
      this.$modal.msgSuccess(
        tip || this.$t("ui.data.column.scheduleResult.lhScheduleCompleted")
      );
      this.$emit("success", { ...params, batchNo: data.batchNo });
    },
    /**
     * 提交硫化自动排程：先提示用户后台执行中，再异步调用后端接口
     * @param {Object} params 排程参数
     */
    handleAutoPlan(params) {
      this.$modal.msgSuccess(
        this.$t("ui.data.column.scheduleResult.lhScheduleExecuting")
      );
      this.hide();
      autoPlan(params)
        .then((data) => this.handleAutoPlanResponse(data, params))
        .catch((error) => {
          console.error(error);
          this.$modal.msgWarning(
            this.$t("ui.data.column.cxScheduleResult.scheduleTimeout")
          );
        });
    },

    //utils
    show(data) {
      this.visible = true;
       // 与硫化排程管理列表查询条件一致：当前日期 + 2 天
      const raw = data && data.scheduleDate;
      const scheduleDate =
        raw != null && String(raw).trim() !== ""
          ? moment(raw).format("YYYY-MM-DD 00:00:00")
          : moment().add(1, "days").format("YYYY-MM-DD 00:00:00");
      this.form = {
        factoryCode: "116",
        scheduleDate,
      };
    },
    hide() {
      this.form = {};
      this.$refs.form.triggerResetForm();
      this.isEdit = false;
      this.visible = false;
    },

    handleConfirm() {
      this.$refs.form.triggerConfirm(this.handleAutoPlan);
    },
  },
};
</script>
