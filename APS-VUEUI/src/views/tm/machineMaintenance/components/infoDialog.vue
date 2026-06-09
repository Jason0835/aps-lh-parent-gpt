<template>
  <el-dialog
    :title="title"
    :visible="visible"
    width="800px"
    @close="hide"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :append-to-body="true"
  >
    <info-form
      class="form-item-height"
      ref="form"
      :form="form"
      :rules="rules"
      :columns="columns"
      label-position="right"
      label-width="160px"
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
import infoForm from "@/views/components/infoForm.vue";
import {saveTmMachineMaintenance} from "@/api/tm/machineMaintenance";

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
                  this.$refs.form.$refs.infoForm.validateField("stopEndTime");
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
                  this.$refs.form.$refs.infoForm.validateField("stopStartTime");
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
      return this.$store.state.tm.machines;
    },
    title: function () {
      return (
        (this.isEdit
          ? this.$t("common.button.edit")
          : this.$t("common.button.add")) +
        this.$t("ui.data.column.tm.machineMaintenance.modelName")
      );
    },
    columns() {
      return [
        {
          prop: "factoryCode",
          label: this.$t("ui.data.column.tm.machineMaintenance.factoryCode"),
          type: "select",
          span: 12,
          required: true,
          dictData: this.parentDict.type.biz_factory_name,
        },
        {
          prop: "machineCode",
          label: this.$t("ui.data.column.tm.machineMaintenance.machineCode"),
          span: 12,
          required: true,
          type: "select",
          dictData: this.machines,
          props: { label: "machineCode", value: "machineCode" },
          filterable: true,
        },
        {
          prop: "stopStartTime",
          label: this.$t("ui.data.column.tm.machineMaintenance.stopStartTime"),
          span: 12,
          type: "date",
          dateType: "datetime",
          valueFormat: "yyyy-MM-dd HH:mm:ss",
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
          label: this.$t("ui.data.column.tm.machineMaintenance.stopEndTime"),
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
    async save(params) {
      try {
        this.loading = true;
        const res = await saveTmMachineMaintenance(params);
        this.$modal.msgSuccess(res.msg);
        this.$emit("success");
        this.hide();
        this.loading = false;
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
