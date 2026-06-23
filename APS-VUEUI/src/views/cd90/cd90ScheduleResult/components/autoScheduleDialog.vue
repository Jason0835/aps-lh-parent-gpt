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
      label-width="80px"
      v-loading="loading"
    />
    <template slot="footer">
      <el-button @click="hide">{{ $t("common.button.cancel") }}</el-button>
      <el-button type="primary" :loading="loading" @click="handleConfirm">{{
        $t("common.button.confirm")
      }}</el-button>
    </template>
  </el-dialog>
</template>

<script>
import moment from "moment";
import infoForm from "@/views/components/infoForm.vue";
import { autoScheduleResult } from "@/api/cd90/scheduleResult";

export default {
  components: { infoForm },
  inject: ["parentDict"],
  data() {
    return {
      loading: false,
      visible: false,
      form: {
        scheduleDate: moment().add(1, "days").format("yyyy-MM-DD"),
        factoryCode: "116",
      },
      rules: {
        factoryCode: [{ required: true, message: this.$t("common.rule.select"), trigger: "change" }],
        scheduleDate: [{ required: true, message: this.$t("common.rule.select"), trigger: "change" }],
      },
    };
  },
  computed: {
    title() {
      return this.$t("ui.data.column.cd90ScheduleResult.autoScheduleTitle");
    },
    columns() {
      return [
        {
          label: this.$t("ui.data.column.cd90ScheduleResult.factoryCode"),
          prop: "factoryCode",
          type: "select",
          dictData: this.parentDict.type.biz_factory_name,
          filterable: true,
        },
        {
          label: this.$t("ui.data.column.cd90ScheduleResult.scheduleDate"),
          prop: "scheduleDate",
          span: 24,
          type: "date",
          valueFormat: "yyyy-MM-dd",
        },
      ];
    },
  },
  methods: {
    show(data) {
      this.visible = true;
      if (data) {
        this.form = {
          scheduleDate: data.scheduleDate || moment().add(1, "days").format("yyyy-MM-DD"),
          factoryCode: data.factoryCode || "116",
        };
      } else {
        this.form = {
          scheduleDate: moment().add(1, "days").format("yyyy-MM-DD"),
          factoryCode: "116",
        };
      }
    },
    hide() {
      this.form = {};
      this.$refs.form.triggerResetForm();
      this.visible = false;
    },
    handleConfirm() {
      this.$refs.form.triggerConfirm((params) => {
        this.save(params);
      });
    },
    async save(params) {
      this.loading = true;
      try {
        const result = await autoScheduleResult({ ...params, forceRegenerate: false });
        // 兼容响应拦截器两种返回形态：
        //   - 剥离后：result = { needConfirm, taskId, batchCheckFailed, errors, warnings, ... }
        //   - 完整体：result = { code, msg, data: { ... } }
        const data = (result && result.data) ? result.data : (result || {});
        const msg = result && result.msg ? result.msg : "";
        const batchCheckFailed = !!(data.batchCheckFailed || result.batchCheckFailed);
        if (batchCheckFailed) {
          // 1.2节批次级数据先行检查失败：不创建任务、不进入排程，直接展示结构化错误。
          this.loading = false;
          this.showBatchCheckAlert(data, msg);
          return;
        }
        const needConfirm = !!(data.needConfirm || result.needConfirm);
        const taskId = data.taskId || result.taskId;
        if (needConfirm) {
          this.loading = false;
          try {
            const confirmText = msg || this.$t("ui.data.column.cd90ScheduleResult.autoScheduleConfirm");
            await this.$confirm(confirmText, this.$t("ui.message.tips"), {
              type: "warning",
              confirmButtonText: this.$t("common.button.confirm"),
              cancelButtonText: this.$t("common.button.cancel"),
            });
            this.loading = true;
            const confirmResult = await autoScheduleResult({ ...params, forceRegenerate: true });
            this.loading = false;
            const confirmData = (confirmResult && confirmResult.data) ? confirmResult.data : (confirmResult || {});
            const confirmMsg = (confirmResult && confirmResult.msg) || "";
            this.$modal.msgSuccess(confirmMsg || this.$t("common.message.operationSuccess"));
            this.$emit("success", params.scheduleDate, confirmData || {});
            this.hide();
          } catch (e) {
            this.loading = false;
          }
          return;
        }
        this.loading = false;
        this.$modal.msgSuccess(msg || this.$t("common.message.operationSuccess"));
        this.$emit("success", params.scheduleDate, { ...(data || {}), taskId });
        this.hide();
      } catch (error) {
        console.error(error);
        this.loading = false;
      }
    },
    /**
     * 渲染批次级数据检查失败的结构化错误/警告面板。
     * 渲染逻辑参考 molding/moldingSchedule/components/autoPlanDialog.vue 的 generate 方法。
     */
    showBatchCheckAlert(data, fallbackMsg) {
      const errors = (data && data.errors) || [];
      const warnings = (data && data.warnings) || [];
      const summary = fallbackMsg
        || (errors.length > 0 ? errors[0].message : "")
        || this.$t("ui.data.column.cxScheduleResult.scheduleFailed");

      let html = '';
      // 摘要信息
      html += '<div style="margin-bottom:16px;padding:10px;background:#fef0f0;border:1px solid #fde2e2;border-radius:4px;">';
      html += '<div style="color:#F56C6C;font-size:14px;font-weight:bold;">⚠️ '
        + this.$t("ui.data.column.cxScheduleResult.scheduleFailed") + '</div>';
      html += '<div style="color:#909399;font-size:13px;margin-top:4px;">' + summary + '</div>';
      html += '</div>';

      // 错误列表
      if (errors.length > 0) {
        html += '<div style="margin-bottom:12px;">';
        html += '<div style="color:#F56C6C;font-size:13px;font-weight:bold;margin-bottom:8px;display:flex;align-items:center;">';
        html += '<span style="display:inline-block;width:4px;height:14px;background:#F56C6C;margin-right:6px;border-radius:2px;"></span>';
        html += this.$t("ui.data.column.cxScheduleResult.errorLabel") + ' ('
          + errors.length + ' ' + this.$t("ui.data.column.cxScheduleResult.itemsLabel") + ')</div>';
        errors.forEach((item) => {
          html += '<div style="margin-bottom:12px;padding:10px;background:#fef0f0;border-left:3px solid #F56C6C;border-radius:3px;">';
          html += '<div style="font-weight:bold;color:#303133;font-size:13px;margin-bottom:6px;">' + (item.field || item.reasonCode || '') + '</div>';
          html += '<div style="color:#F56C6C;font-size:13px;line-height:1.6;margin-bottom:4px;">' + (item.message || '') + '</div>';
          if (item.suggestion) {
            html += '<div style="color:#909399;font-size:12px;line-height:1.6;"><span style="opacity:0.7;">💡</span> ' + item.suggestion + '</div>';
          }
          html += '</div>';
        });
        html += '</div>';
      }

      // 警告列表
      if (warnings.length > 0) {
        if (errors.length > 0) html += '<hr style="border:none;border-top:1px solid #EBEEF5;margin:16px 0;"/>';
        html += '<div>';
        html += '<div style="color:#E6A23C;font-size:13px;font-weight:bold;margin-bottom:8px;display:flex;align-items:center;">';
        html += '<span style="display:inline-block;width:4px;height:14px;background:#E6A23C;margin-right:6px;border-radius:2px;"></span>';
        html += this.$t("ui.data.column.cxScheduleResult.warningLabel") + ' ('
          + warnings.length + ' ' + this.$t("ui.data.column.cxScheduleResult.itemsLabel") + ')</div>';
        warnings.forEach((item) => {
          html += '<div style="margin-bottom:12px;padding:10px;background:#fdf6ec;border-left:3px solid #E6A23C;border-radius:3px;">';
          html += '<div style="font-weight:bold;color:#303133;font-size:13px;margin-bottom:6px;">' + (item.field || item.reasonCode || '') + '</div>';
          html += '<div style="color:#E6A23C;font-size:13px;line-height:1.6;margin-bottom:4px;">' + (item.message || '') + '</div>';
          if (item.suggestion) {
            html += '<div style="color:#909399;font-size:12px;line-height:1.6;"><span style="opacity:0.7;">💡</span> ' + item.suggestion + '</div>';
          }
          html += '</div>';
        });
        html += '</div>';
      }

      this.$alert(html, this.$t("ui.data.column.cxScheduleResult.scheduleFailed"), {
        dangerouslyUseHTMLString: true,
        type: 'error',
        customClass: 'cd90-auto-schedule-batch-check',
        confirmButtonText: this.$t("ui.data.column.cxScheduleResult.gotIt"),
      });
    },
  },
};
</script>

<style>
/* 批次级数据检查错误/警告弹窗，内容过多时可滚动（非scoped，因为 $alert 挂在 body 级别） */
.cd90-auto-schedule-batch-check {
  max-width: 600px;
}
.cd90-auto-schedule-batch-check .el-message-box {
  max-height: 85vh;
  display: flex;
  flex-direction: column;
}
.cd90-auto-schedule-batch-check .el-message-box__content {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
.cd90-auto-schedule-batch-check .el-message-box__message {
  flex: 1;
  overflow-y: auto;
  max-height: calc(85vh - 130px);
  padding-right: 8px;
}
</style>
