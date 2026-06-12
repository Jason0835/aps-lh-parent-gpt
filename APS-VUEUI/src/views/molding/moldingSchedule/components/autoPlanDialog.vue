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

import {
  autoPlan,
  validateAutoPlan,
  lhAutoPlan,
  lhValidateAutoPlan,
  modelChange,
  modelChangeValidate,
  modelAdjustPlan,
  generatePlan
} from "@/api/cx/cxScheduleResult";

export default {
  components: { infoForm },
  inject: ["parentDict"],
  data() {
    return {
      loading: false,
      visible: false,
      isEdit: false,
      editType: null,
      form: {
        scheduleDate: moment().add(1, "days").format("yyyy-MM-DD"),
      },
      rules: {
        // specName: [
        //   {
        //     required: true,
        //     message: this.$t("common.rule.input"),
        //     trigger: "blur",
        //   },
        // ],
      },
      // columns: [
      //   {
      //     label: this.$t("ui.data.column.scheduleResult.scheduleDate"),
      //     prop: "scheduleDate",
      //     span: 24,
      //     type: "date",
      //     valueFormat: "yyyy-MM-dd",
      //   },
      //   {
      //     label: this.$t("ui.data.column.cxScheduleResult.cxMachineCode"),
      //     prop: "cxMachineCode",
      //     span: 24,
      //     hidden: this.editType != "1",
      //   },
      // ],
    };
  },
  computed: {
    title: function () {
      return this.$t("ui.data.column.cxScheduleResult.cxAutoPlan");
    },
    columns: function () {
      return [
      {
          label: this.$t("ui.data.column.factoryCode"),
          prop: "factoryCode",
          type: "select",
          dictData: this.parentDict.type.biz_factory_name,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.scheduleDate"),
          prop: "scheduleDate",
          span: 24,
          type: "date",
          valueFormat: "yyyy-MM-dd",
        },

      ];
    },
  },
  methods: {
    // api
    async save(params) {
      // console.log(params);
      if (this.editType == "1") {
        this.generate(params)
        // this.autoPlan(params);
      } else if (this.editType == "2") {
        this.lhAutoPlan({
          scheduleDate: params.scheduleDate,
        });
      } else if (this.editType == 3) {
        this.modelChange({
          scheduleDate: params.scheduleDate,
        });
      } else {
        throw "editType error";
      }
    },
    async generate(params){
      this.loading = true;
      try {
        const result = await generatePlan({
          ...params,
          day:'3'
        });
        this.loading = false;
        if (result.code === 200) {
          this.$modal.msgSuccess(result.msg);
          this.$emit("success", params.scheduleDate);
          this.hide();
        } else if (result.code === 423) {
          // 排程执行中（锁冲突），提示用户稍后查看
          this.$modal.msgWarning(result.msg || this.$t("ui.data.column.cxScheduleResult.scheduleRunning"));
          this.hide();
        } else if (result.code === 500) {
          const errorData = result.data || {};
          const errorCount = errorData.errorCount || 0;
          const warningCount = errorData.warningCount || 0;

          // 校验已通过但超时：排程实际正在执行中，友好提示用户稍后刷新
          if (errorCount === 0 && warningCount === 0) {
            this.$modal.msgWarning(this.$t("ui.data.column.cxScheduleResult.scheduleValidating"));
            this.hide();
            return;
          }

          const errors = errorData.errors || [];
          const warnings = errorData.warnings || [];

          // 构建标题，包含日期
          const scheduleFailedText = this.$t("ui.data.column.cxScheduleResult.scheduleFailed");
          const titleMatch = result.msg.match(new RegExp(scheduleFailedText.replace(/[.*+?^${}()|[\]\\]/g, '\\$&') + '\\[(\\d{4}-\\d{2}-\\d{2})\\]:'));
          const scheduleDate = titleMatch ? titleMatch[1] : '';
          const summaryMsg = result.msg.replace(new RegExp(scheduleFailedText.replace(/[.*+?^${}()|[\]\\]/g, '\\$&') + '\\[\\d{4}-\\d{2}-\\d{2}\\]:'), '').trim();

          let html = '';

          // 摘要信息
          html += '<div style="margin-bottom:16px;padding:10px;background:#fef0f0;border:1px solid #fde2e2;border-radius:4px;">';
          html += '<div style="color:#F56C6C;font-size:14px;font-weight:bold;">⚠️ ' + this.$t("ui.data.column.cxScheduleResult.scheduleFailed") + '</div>';
          if (scheduleDate) {
            html += '<div style="color:#606266;font-size:13px;margin-top:4px;">' + this.$t("ui.data.column.cxScheduleResult.scheduleDateLabel") + scheduleDate + '</div>';
          }
          html += '<div style="color:#909399;font-size:13px;margin-top:4px;">' + summaryMsg + '</div>';
          html += '</div>';

          // 错误列表
          if (errors.length > 0) {
            html += '<div style="margin-bottom:12px;">';
            html += '<div style="color:#F56C6C;font-size:13px;font-weight:bold;margin-bottom:8px;display:flex;align-items:center;">';
            html += '<span style="display:inline-block;width:4px;height:14px;background:#F56C6C;margin-right:6px;border-radius:2px;"></span>';
            html += this.$t("ui.data.column.cxScheduleResult.errorLabel") + ' (' + errorData.errorCount + ' ' + this.$t("ui.data.column.cxScheduleResult.itemsLabel") + ')</div>';

            errors.forEach((item, idx) => {
              html += '<div style="margin-bottom:12px;padding:10px;background:#fef0f0;border-left:3px solid #F56C6C;border-radius:3px;">';
              html += '<div style="font-weight:bold;color:#303133;font-size:13px;margin-bottom:6px;">' + item.dataItem + '</div>';
              html += '<div style="color:#F56C6C;font-size:13px;line-height:1.6;margin-bottom:4px;">' + item.message + '</div>';
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
            html += this.$t("ui.data.column.cxScheduleResult.warningLabel") + ' (' + errorData.warningCount + ' ' + this.$t("ui.data.column.cxScheduleResult.itemsLabel") + ')</div>';

            warnings.forEach((item, idx) => {
              html += '<div style="margin-bottom:12px;padding:10px;background:#fdf6ec;border-left:3px solid #E6A23C;border-radius:3px;">';
              html += '<div style="font-weight:bold;color:#303133;font-size:13px;margin-bottom:6px;">' + item.dataItem + '</div>';
              html += '<div style="color:#E6A23C;font-size:13px;line-height:1.6;margin-bottom:4px;">' + item.message + '</div>';
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
            customClass: 'molding-auto-plan-error',
            confirmButtonText: this.$t("ui.data.column.cxScheduleResult.gotIt"),
          });
        }
      } catch (error) {
        console.error(error);
        this.loading = false;
        this.$modal.msgWarning(this.$t("ui.data.column.cxScheduleResult.scheduleTimeout"));
      }
    },
    async autoPlan(params) {
      try {
        this.loading = true;
        let valid = await validateAutoPlan(params);
        if (valid.msg == "0") {
          this.$confirm(this.$t("ui.biz.alter.makeSureRecreateByPublished"))
            .then(async () => {
              let result = await autoPlan(params);
              this.loading = false;
              if (result.code == 200) {
                this.$modal.msgSuccess(result.msg);
                this.$emit("success");
                this.hide();
              }
            })
            .catch(() => {
              this.loading = false;
            });
        } else if (valid.msg == "1") {
          this.$confirm(this.$t("ui.biz.alter.makeSureRecreate"))
            .then(async () => {
              let result = await autoPlan(params);
              this.loading = false;
              if (result.code == 200) {
                this.$modal.msgSuccess(result.msg);
                this.$emit("success");
                this.hide();
              }
            })
            .catch(() => {
              this.loading = false;
            });
        } else if (valid.msg == "2") {
           let result = await autoPlan(params);
           this.loading = false;
           if (result.code == 200) {
             this.$modal.msgSuccess(result.msg);
             this.$emit("success", params.scheduleDate);
             this.hide();
           }
        } else if (valid.msg == "3") {
          this.loading = false;
          this.$modal.warning(this.$t("ui.biz.alter.CanNotRecreate"));
        } else {
          this.loading = false;
          this.$modal.warning(result.msg);
        }
      } catch (error) {
        console.error(error);
        this.loading = false;
      }
    },
    async lhAutoPlan(params) {
      try {
        this.loading = true;
        const valid = await lhValidateAutoPlan(params);
        if (valid.msg == "1") {
          this.$confirm(this.$t("ui.biz.alter.makeSureRecreate")).then(
            async () => {
               let result = await lhAutoPlan(params);
               this.loading = false;
               if (result.code == 200) {
                 this.$modal.msgSuccess(result.msg);
                 this.$emit("success", params.scheduleDate);
                 this.hide();
               }
            }
          );
        } else if (valid.msg == "2") {
           let result = await lhAutoPlan(params);
           this.loading = false;
           if (result.code == 200) {
             this.$modal.msgSuccess(result.msg);
             this.$emit("success", params.scheduleDate);
             this.hide();
           }
        } else if (valid.msg == "3") {
          this.$modal.warning(this.$t("ui.biz.alter.CanNotRecreate"));
        }
      } catch (error) {
        console.error(error);
        this.loading = false;
      }
    },
    async modelChange(params) {
      try {
        this.loading = true;
        const valid = await modelChangeValidate(params);

        if (valid.msg == "0") {
          this.$confirm(
            this.$t("ui.data.column.scheduleResult.modelChangeValidate")
          ).then(async () => {
             const result = await modelChange(params);
             this.loading = false;
             this.$modal.msgSuccess(result.msg);
             this.$emit("success", params.scheduleDate);
             this.hide();
          });
        } else {
           const result = await modelChange(params);
           this.loading = false;
           this.$modal.msgSuccess(result.msg);
           this.$emit("success", params.scheduleDate);
           this.hide();
        }
      } catch (error) {
        console.error(error);
        this.loading = false;
      }
    },
    async modelAdjustPlan(params) {
      try {
        this.loading = true;

         const result = await modelAdjustPlan(params);
         this.loading = false;
         this.$modal.msgSuccess(result.msg);
         this.$emit("success", params.scheduleDate);
         this.hide();
      } catch (error) {
        console.error(error);
        this.loading = false;
      }
    },

    //utils
    show(data, editType) {
      this.visible = true;
      this.editType = editType;
      if (data) {
        this.isEdit = true;
        this.form = {
          ...data,
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
      // this.resetForm("infoForm");
      this.isEdit = false;
      this.visible = false;
    },

    handleConfirm() {
      this.$refs.form.triggerConfirm((params) => {
        this.save(params);
      });
    },
  },
};
</script>

<style>
/* 自动排程错误/警告弹窗，内容过多时可滚动（非scoped，因为 $alert 挂载在 body 级别） */
.molding-auto-plan-error {
  max-width: 600px;
}
.molding-auto-plan-error .el-message-box {
  max-height: 85vh;
  display: flex;
  flex-direction: column;
}
.molding-auto-plan-error .el-message-box__content {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
.molding-auto-plan-error .el-message-box__message {
  flex: 1;
  overflow-y: auto;
  max-height: calc(85vh - 130px);
  padding-right: 8px;
}
</style>
