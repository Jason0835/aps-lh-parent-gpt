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
import { autoPlan } from "@/api/tq/tqNewScheduleResult";

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
        scheduleDateQuery: [
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
          label: this.$t("ui.data.column.tqNewScheduleResult.scheduleDate"),
          prop: "scheduleDateQuery",
          type: "date",
          dateType: "date",
          valueFormat: "yyyy-MM-dd",
          format: "yyyy-MM-dd",
          clearable: false,
        },
      ],
    };
  },
  computed: {
    title: function () {
      return this.$t("ui.data.btn.tqNewScheduleResult.autoPlan");
    },
  },
  methods: {
    /**
     * 处理胎圈自动排程接口响应
     * @param {Object} data 接口响应
     * @param {Object} params 排程参数
     */
    handleAutoPlanResponse(data, params) {
      const tip = data.message || data.msg || "";
      if (data.code != null && data.code !== 200) {
        this.$modal.msgError(tip || this.$t("ui.data.btn.ajax.code.msg"));
        return;
      }
      this.$modal.msgSuccess(tip || this.$t("ui.data.btn.tqNewScheduleResult.autoPlan"));
      this.$emit("success", { ...params });
    },
    /**
     * 提交胎圈自动排程：先提示用户后台执行中，再调用后端接口
     * @param {Object} params 排程参数
     */
    handleAutoPlan(params) {
      this.$modal.msgSuccess(
        this.$t("ui.data.btn.tqNewScheduleResult.autoPlan")
      );
      this.hide();
      autoPlan(params)
        .then((data) => this.handleAutoPlanResponse(data, params))
        .catch((error) => {
          console.error(error);
          this.$modal.msgWarning(
            this.$t("ui.data.btn.tqNewScheduleResult.autoPlan")
          );
        });
    },

    //utils
    /**
     * 打开弹窗
     * @param {Object} data 初始数据，可携带 scheduleDateQuery
     */
    show(data) {
      this.visible = true;
      // 排程日期默认 T+1（与硫化排程一致），若传入则使用传入日期
      const raw = data && data.scheduleDateQuery;
      const scheduleDateQuery =
        raw != null && String(raw).trim() !== ""
          ? moment(raw).format("YYYY-MM-DD")
          : moment().add(1, "days").format("YYYY-MM-DD");
      this.form = {
        factoryCode: "116",
        scheduleDateQuery,
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
