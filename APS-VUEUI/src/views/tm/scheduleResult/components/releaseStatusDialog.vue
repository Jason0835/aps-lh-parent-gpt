<template>
  <el-dialog
    :append-to-body="true"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :title="title"
    :visible="visible"
    width="400px"
    @close="hide"
  >
    <el-form ref="infoForm" :model="form">
      <el-form-item :label="$t('ui.data.column.scheduleResult.isRelease')">
        <el-radio-group v-model="form.isRelease">
          <template v-for="(item, index) in parentDict.type.IS_RELEASE">
            <el-radio
              v-if="item.value == '1' || item.value == '2'"
              :key="`${item.value}-${index}`"
              :label="item.value"
              >{{ item.label }}</el-radio
            ></template
          >
        </el-radio-group>
      </el-form-item>
    </el-form>
    <template slot="footer">
      <el-button @click="hide">{{ this.$t("common.button.cancel") }}</el-button>
      <el-button :loading="loading" type="primary" @click="handleConfirm">{{
        this.$t("common.button.confirm")
      }}</el-button>
    </template>
  </el-dialog>
</template>

<script>
import {changeReleaseStatus} from "@/api/tm/scheduleResult";
import {resolveErrorMessage} from "@/utils/errorMessage";

export default {
  components: {},
  inject: ["parentDict"],
  data() {
    return {
      loading: false,
      visible: false,
      form: {},
      tableRows: [],
    };
  },
  computed: {
    title: function () {
      return this.$t("ui.data.column.scheduleResult.changeReleaseStatus");
    },
  },
  methods: {
    async save(params) {
      try {
        this.loading = true;
        const data = await changeReleaseStatus(params);
        this.$modal.msgSuccess(data.msg);
        this.loading = false;
        this.$emit("success");
        this.hide();
      } catch (error) {
        this.$modal.alertError(resolveErrorMessage(
          error,
          this.$t("ui.data.column.tm.scheduleResult.operationFailed")
        ));
        this.loading = false;
      }
    },

    show(rows) {
      this.visible = true;
      this.tableRows = rows || [];
      this.form = {};
    },
    hide() {
      this.form = {};
      this.tableRows = [];
      this.resetForm("infoForm");
      this.visible = false;
    },

    handleConfirm() {
      this.$refs.infoForm.validate((valid) => {
        if (valid) {
          const ids = this.tableRows.map((item) => item.id).join(",");
          this.save({
            ids: ids,
            isRelease: this.form.isRelease,
          });
        }
      });
    },
  },
};
</script>
