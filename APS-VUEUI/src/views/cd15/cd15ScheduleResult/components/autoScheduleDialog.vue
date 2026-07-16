<template>
  <el-dialog
    :title="title"
    :visible="visible"
    width="400px"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    @close="hide"
  >
    <info-form
      ref="form"
      class="form-item-height"
      :form="form"
      :rules="rules"
      :columns="columns"
      label-position="right"
      label-width="80px"
      v-loading="loading"
    />
    <template slot="footer">
      <el-button @click="hide">{{ $t("common.button.cancel") }}</el-button>
      <el-button type="primary" :loading="loading" @click="handleConfirm">{{ $t("common.button.confirm") }}</el-button>
    </template>
  </el-dialog>
</template>

<script>
import moment from "moment";
import infoForm from "@/views/components/infoForm.vue";
import { autoSchedule } from "@/api/cd15/scheduleResult";

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
      return this.$t("ui.data.column.cd15ScheduleResult.autoScheduleTitle");
    },
    columns() {
      return [
        {
          label: this.$t("ui.data.column.cd15ScheduleResult.factoryCode"),
          prop: "factoryCode",
          type: "select",
          dictData: this.parentDict.type.biz_factory_name,
          filterable: true,
        },
        {
          label: this.$t("ui.data.column.cd15ScheduleResult.scheduleDate"),
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
      this.form = {
        scheduleDate: (data && data.scheduleDate) || moment().add(1, "days").format("yyyy-MM-DD"),
        factoryCode: (data && data.factoryCode) || "116",
      };
    },
    hide() {
      this.form = {};
      if (this.$refs.form) {
        this.$refs.form.triggerResetForm();
      }
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
        const result = await autoSchedule({ ...params, forceRegenerate: false });
        const data = (result && result.data) ? result.data : (result || {});
        const msg = result && result.msg ? result.msg : "";
        const needConfirm = !!(data.needConfirm || result.needConfirm);
        const taskId = data.taskId || result.taskId;
        if (needConfirm) {
          this.loading = false;
          try {
            await this.$confirm(msg || this.$t("ui.data.column.cd15ScheduleResult.autoScheduleConfirm"), this.$t("ui.message.tips"), {
              type: "warning",
              confirmButtonText: this.$t("common.button.confirm"),
              cancelButtonText: this.$t("common.button.cancel"),
            });
            this.loading = true;
            const confirmResult = await autoSchedule({ ...params, forceRegenerate: true });
            const confirmData = (confirmResult && confirmResult.data) ? confirmResult.data : (confirmResult || {});
            const confirmMsg = (confirmResult && confirmResult.msg) || "";
            this.loading = false;
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
  },
};
</script>