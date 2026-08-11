<template>
  <el-dialog
    :append-to-body="true"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :title="title"
    :visible="visible"
    width="800px"
    @close="hide"
  >
    <info-form
      ref="form"
      v-loading="loading"
      :columns="columns"
      :form="form"
      :rules="rules"
      class="form-item-height"
      label-position="right"
      label-width="160px"
    >
    </info-form>
    <template slot="footer">
      <el-button @click="hide">{{ this.$t("common.button.cancel") }}</el-button>
      <el-button :loading="loading" type="primary" @click="handleConfirm">{{
        this.$t("common.button.confirm")
      }}</el-button>
    </template>
  </el-dialog>
</template>

<script>
import infoForm from "@/views/components/infoForm.vue";
import {saveTcMachineMaintenance} from "@/api/tc/machineMaintenance";
import {listTcShiftConfig} from "@/api/tc/shiftConfig";

export default {
  components: { infoForm },
  inject: ["parentDict"],
  data() {
    return {
      loading: false,
      visible: false,
      isEdit: false,
      shiftConfigs: [],
      form: {},
      rules: {
        factoryCode: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
        machineCode: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        stopStartTime: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
          {
            validator: (rule, value, callback) => {
              if (value && this.form.stopEndTime) {
                if (new Date(value).getTime() > new Date(this.form.stopEndTime).getTime()) {
                  callback(new Error("开始时间不能大于结束时间"));
                } else {
                  callback();
                }
              } else {
                callback();
              }
            },
            trigger: "change",
          },
        ],
        stopEndTime: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
          {
            validator: (rule, value, callback) => {
              if (value && this.form.stopStartTime) {
                if (new Date(value).getTime() < new Date(this.form.stopStartTime).getTime()) {
                  callback(new Error("结束时间不能小于开始时间"));
                } else {
                  callback();
                }
              } else {
                callback();
              }
            },
            trigger: "change",
          },
        ],
      },
    };
  },
  computed: {
    machines() {
      return this.$store.state.tc.machines;
    },
    title: function () {
      return (
        (this.isEdit
          ? this.$t("common.button.edit")
          : this.$t("common.button.add")) +
        this.$t("ui.data.column.tc.machineMaintenance.modelName")
      );
    },
    columns() {
      return [
        {
          prop: "factoryCode",
          label: this.$t("ui.data.column.tc.machineMaintenance.factoryCode"),
          type: "select",
          span: 12,
          required: true,
          disabled: true,
          dictData: this.parentDict.type.biz_factory_name,
        },
        {
          prop: "machineCode",
          label: this.$t("ui.data.column.tc.machineMaintenance.machineCode"),
          span: 12,
          required: true,
          type: "select",
          dictData: this.machines,
          props: { label: "machineCode", value: "machineCode" },
          filterable: true,
          listeners: {
            change: (value) => {
              if (value) {
                const machine = this.machines.find(m => m.machineCode === value);
                if (machine) {
                  this.form.factoryCode = machine.factoryCode;
                  this.loadShiftConfig(this.form.factoryCode);
                }
              }
            },
          },
        },
        {
          prop: "stopStartTime",
          label: this.$t("ui.data.column.tc.machineMaintenance.stopStartTime"),
          span: 12,
          type: "date",
          dateType: "datetime",
          valueFormat: "yyyy-MM-dd HH:mm:ss",
          listeners: {
            change: (value) => {
              this.resolveStopShiftByTime(value);
            },
          },
          pickerOptions: {
            disabledDate: (time) => {
              if (this.form.stopEndTime) {
                const end = new Date(this.form.stopEndTime);
                end.setHours(0, 0, 0, 0);
                return time.getTime() > end.getTime();
              }
              return false;
            },
          },
        },
        {
          prop: "stopEndTime",
          label: this.$t("ui.data.column.tc.machineMaintenance.stopEndTime"),
          span: 12,
          type: "date",
          dateType: "datetime",
          valueFormat: "yyyy-MM-dd HH:mm:ss",
          pickerOptions: {
            disabledDate: (time) => {
              if (this.form.stopStartTime) {
                const start = new Date(this.form.stopStartTime);
                start.setHours(0, 0, 0, 0);
                return time.getTime() < start.getTime();
              }
              return false;
            },
          },
        },
        {
          prop: "stopShift",
          label: this.$t("ui.data.column.tc.machineMaintenance.stopShift"),
          span: 12,
          type: "select",
          disabled: true,
          dictData: this.parentDict.type.class_num_three_plan,
        },
        {
          prop: "remark",
          label: this.$t("ui.common.column.remark"),
          span: 24,
          type: "textarea",
          maxlength: 900,
        },
      ];
    },
  },
  methods: {
    // 加载当前工厂启用的班次配置，用于根据停机开始时间自动回显停机班次
    async loadShiftConfig(factoryCode) {
      if (!factoryCode) {
        this.shiftConfigs = [];
        return;
      }
      try {
        const res = await listTcShiftConfig({ factoryCode });
        const rows = (res && res.rows) || [];
        this.shiftConfigs = rows
          .filter((item) => item.openFlag === "1")
          .sort((a, b) => (a.shiftOrder || 0) - (b.shiftOrder || 0));
      } catch (error) {
        console.log(error);
        this.shiftConfigs = [];
      }
    },
    // 根据停机开始时间解析并回显停机班次（与后端 resolveStopShift 逻辑保持一致）
    resolveStopShiftByTime(value) {
      if (!value) {
        this.$set(this.form, "stopShift", undefined);
        return;
      }
      const date = new Date(value);
      const timeMinutes = date.getHours() * 60 + date.getMinutes();
      const matched = (this.shiftConfigs || []).find((config) => {
        const start = this.parseTimeToMinutes(config.planStartTime);
        const end = this.parseTimeToMinutes(config.planEndTime);
        if (config.crossDayFlag === "1") {
          return end <= start
            ? timeMinutes >= start || timeMinutes < end
            : timeMinutes >= start && timeMinutes < end;
        }
        return timeMinutes >= start && timeMinutes < end;
      });
      this.$set(this.form, "stopShift", matched ? this.mapShiftNameToCode(matched.shiftName) : undefined);
    },
    // 班次名称映射为 class_num_three_plan 字典编码：夜班→01, 早班→02, 中班→03
    mapShiftNameToCode(shiftName) {
      if (shiftName === "夜班") return "01";
      if (shiftName === "早班") return "02";
      if (shiftName === "中班") return "03";
      return undefined;
    },
    // 将 HH:mm:ss 时间字符串转换为分钟数
    parseTimeToMinutes(timeStr) {
      if (!timeStr) return 0;
      const parts = timeStr.split(":");
      return parseInt(parts[0], 10) * 60 + (parts.length > 1 ? parseInt(parts[1], 10) : 0);
    },
    async save(params) {
      try {
        this.loading = true;
        const res = await saveTcMachineMaintenance(params);
        this.$modal.msgSuccess(res.msg);
        this.$emit("success");
        this.loading = false;
        this.hide();
      } catch (error) {
        console.log(error);
        this.loading = false;
      }
    },

    show(data) {
      this.visible = true;
      if (data) {
        this.isEdit = true;
        this.form = {
          ...data,
        };
      } else {
        this.form = {
          factoryCode: "116",
        };
      }
      this.loadShiftConfig(this.form.factoryCode);
    },
    hide() {
      this.form = {};
      this.$refs.form.triggerResetForm();
      this.isEdit = false;
      this.visible = false;
    },
    handleConfirm() {
      this.$refs.form.triggerConfirm(this.save);
    },
  },
};
</script>
