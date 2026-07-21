<template>
  <el-dialog
    :title="title"
    :visible.sync="dialogVisible"
    width="500px"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    append-to-body
  >
    <info-form
      ref="form"
      :form="form"
      :rules="rules"
      :columns="columns"
      label-position="right"
      label-width="140px"
      v-loading="loading"
    />
    <template slot="footer">
      <el-button @click="hide">{{ $t("ui.frame.btn.cancel") }}</el-button>
      <el-button type="primary" :loading="loading" @click="handleConfirm">
        {{ $t("ui.frame.btn.confirm") }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script>
import infoForm from "@/views/components/infoForm.vue";
import {
  changeMachine,
  validateChangeMachine,
} from "@/api/gsq/scheduleResult";

export default {
  name: "GsqChangeMachineDialog",
  components: { infoForm },
  props: {
    visible: {
      type: Boolean,
      default: false,
    },
    rows: {
      type: Array,
      default: () => [],
    },
  },
  data() {
    return {
      loading: false,
      form: {},
      rules: {
        machineCode: [
          {
            required: true,
            message: this.$t("ui.data.column.gsqScheduleResult.machineCodeRequired"),
            trigger: "blur",
          },
        ],
      },
    };
  },
  computed: {
    dialogVisible: {
      get() {
        return this.visible;
      },
      set(val) {
        this.$emit("update:visible", val);
      },
    },
    title() {
      return this.$t("ui.data.column.gsqScheduleResult.changeMachineTitle");
    },
    columns() {
      return [
        {
          label: this.$t("ui.data.column.gsqScheduleResult.machineCode"),
          prop: "machineCode",
          type: "input",
        },
        {
          label: this.$t("ui.data.column.gsqScheduleResult.remark"),
          prop: "remark",
          type: "input",
        },
      ];
    },
  },
  watch: {
    visible(val) {
      if (val) {
        this.form = {
          machineCode: "",
          remark: "",
        };
      }
    },
  },
  methods: {
    hide() {
      this.dialogVisible = false;
    },
    handleConfirm() {
      this.$refs.form.triggerConfirm(this.submit);
    },
    submit() {
      if (this.rows.length === 0) {
        this.$modal.msgError(
          this.$t("ui.data.column.gsqScheduleResult.noSelectRow")
        );
        return;
      }
      this.loading = true;
      const ids = this.rows.map((r) => r.id);
      const scheduleDate = this.rows[0].scheduleDate;
      const factoryCode = this.rows[0].factoryCode;
      const params = {
        ids,
        machineCode: this.form.machineCode,
        scheduleDateQuery: scheduleDate,
        factoryCode,
        remark: this.form.remark,
      };
      validateChangeMachine(params)
        .then((res) => {
          if (res.code !== 200) {
            this.$modal.msgError(res.msg);
            return Promise.reject();
          }
          return changeMachine(params);
        })
        .then((res) => {
          const tip = res.msg || res.message || "";
          if (res.code != null && res.code !== 200) {
            this.$modal.msgError(tip || this.$t("ui.common.message.operateFail"));
            return;
          }
          this.$modal.msgSuccess(
            tip || this.$t("ui.data.column.gsqScheduleResult.changeMachineSuccess")
          );
          this.$emit("refresh");
          this.hide();
        })
        .catch(() => {})
        .finally(() => {
          this.loading = false;
        });
    },
  },
};
</script>
