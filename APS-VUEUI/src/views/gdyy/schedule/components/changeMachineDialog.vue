<template>
  <el-dialog
    :title="$t('ui.data.column.scheduleResult.changeMachine')"
    :visible="visible"
    width="500px"
    @close="hide"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :append-to-body="true"
  >
    <el-form ref="form" :model="form" :rules="rules" label-width="100px">
      <el-form-item :label="$t('ui.data.column.gdyyScheduleResult.bigRollCode')">
        <el-input :value="form.bigRollCode" disabled />
      </el-form-item>
      <el-form-item :label="$t('ui.data.column.gdyyScheduleResult.machineCode')" prop="machineCode">
        <el-input v-model="form.machineCode" :placeholder="$t('common.rule.input')" />
      </el-form-item>
    </el-form>
    <template slot="footer">
      <el-button @click="hide">{{ $t("common.button.cancel") }}</el-button>
      <el-button type="primary" :loading="loading" @click="handleConfirm">{{ $t("common.button.confirm") }}</el-button>
    </template>
  </el-dialog>
</template>

<script>
import { changeMachineGdyyScheduleResult } from "@/api/gdyy/gdyyScheduleResult";

export default {
  data() {
    return {
      loading: false,
      visible: false,
      form: {},
      rules: {
        machineCode: [
          { required: true, message: this.$t("common.rule.input"), trigger: "blur" },
        ],
      },
    };
  },
  methods: {
    show(data) {
      this.visible = true;
      this.form = { ...data };
    },
    hide() {
      this.visible = false;
      this.form = {};
      this.$refs.form.resetFields();
    },
    handleConfirm() {
      this.$refs.form.validate((valid) => {
        if (valid) {
          this.loading = true;
          changeMachineGdyyScheduleResult(this.form)
            .then((res) => {
              this.$modal.msgSuccess(res.msg);
              this.$emit("success");
              this.hide();
            })
            .finally(() => {
              this.loading = false;
            });
        }
      });
    },
    openDialog(data) {
      this.show(data);
    },
  },
};
</script>
