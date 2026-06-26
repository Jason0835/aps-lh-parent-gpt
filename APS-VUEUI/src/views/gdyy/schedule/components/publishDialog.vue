<template>
  <el-dialog
    :title="$t('ui.data.column.scheduleResult.publish')"
    :visible="visible"
    width="500px"
    @close="hide"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :append-to-body="true"
  >
    <div style="text-align: center; padding: 20px;">
      <p>{{ $t("ui.data.column.scheduleResult.publishConfirm", { count: selection.length }) }}</p>
    </div>
    <template slot="footer">
      <el-button @click="hide">{{ $t("common.button.cancel") }}</el-button>
      <el-button type="primary" :loading="loading" @click="handleConfirm">{{ $t("common.button.confirm") }}</el-button>
    </template>
  </el-dialog>
</template>

<script>
import { publishGdyyScheduleResult } from "@/api/gdyy/gdyyScheduleResult";

export default {
  data() {
    return {
      loading: false,
      visible: false,
      selection: [],
    };
  },
  methods: {
    show(data) {
      this.visible = true;
      this.selection = data || [];
    },
    hide() {
      this.visible = false;
      this.selection = [];
    },
    handleConfirm() {
      this.loading = true;
      const ids = this.selection.map((item) => item.id);
      publishGdyyScheduleResult(ids)
        .then((res) => {
          this.$modal.msgSuccess(res.msg);
          this.$emit("success");
          this.hide();
        })
        .finally(() => {
          this.loading = false;
        });
    },
    openDialog(data) {
      this.show(data);
    },
  },
};
</script>
