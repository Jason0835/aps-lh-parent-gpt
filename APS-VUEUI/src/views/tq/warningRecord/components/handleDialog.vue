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
    <el-form
      ref="form"
      :model="form"
      :rules="rules"
      label-position="right"
      label-width="100px"
      v-loading="loading"
    >
      <el-form-item :label="$t('ui.data.column.tqWarningRecord.warningTitle')">
        <span>{{ form.warningTitle }}</span>
      </el-form-item>
      <el-form-item :label="$t('ui.data.column.tqWarningRecord.warningContent')">
        <span>{{ form.warningContent }}</span>
      </el-form-item>
      <el-form-item :label="$t('ui.data.column.tqWarningRecord.handler')" prop="handler">
        <el-input v-model="form.handler" :placeholder="$t('common.rule.input')" maxlength="64" />
      </el-form-item>
      <el-form-item :label="$t('ui.data.column.tqWarningRecord.handleOpinion')" prop="handleOpinion">
        <el-input
          v-model="form.handleOpinion"
          type="textarea"
          :rows="4"
          :placeholder="$t('common.rule.input')"
          maxlength="500"
          show-word-limit
        />
      </el-form-item>
    </el-form>
    <template slot="footer">
      <el-button @click="hide">{{ $t("common.button.cancel") }}</el-button>
      <el-button type="primary" :loading="loading" @click="handleConfirm">{{
        $t("common.button.confirm")
      }}</el-button>
    </template>
  </el-dialog>
</template>

<script>
import { handleWarningRecord } from "@/api/tq/warningRecord";

export default {
  name: "TqWarningRecordHandleDialog",
  data() {
    return {
      loading: false,
      visible: false,
      form: {},
      rules: {
        handler: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        handleOpinion: [
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
    title() {
      return this.$t("ui.data.btn.tqWarningRecord.handle");
    },
  },
  methods: {
    show(row) {
      this.form = { ...row };
      this.visible = true;
      this.$nextTick(() => {
        if (this.$refs.form) {
          this.$refs.form.clearValidate();
        }
      });
    },
    hide() {
      this.visible = false;
      this.form = {};
    },
    handleConfirm() {
      this.$refs.form.validate((valid) => {
        if (!valid) {
          return;
        }
        this.loading = true;
        const params = {
          id: this.form.id,
          handler: this.form.handler,
          opinion: this.form.handleOpinion,
        };
        handleWarningRecord(params)
          .then((data) => {
            this.$modal.msgSuccess(data.msg);
            this.$emit("success");
            this.hide();
          })
          .catch((error) => {
            console.log(error);
          })
          .finally(() => {
            this.loading = false;
          });
      });
    },
  },
};
</script>
