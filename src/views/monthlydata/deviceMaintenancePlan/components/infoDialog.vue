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
      <el-button type="primary" :loading="loading" @click="handleConfirm"
        >{{ this.$t("common.button.confirm") }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script>
import infoForm from "@/views/components/infoForm.vue";

import { editDeviceMaintenancePlan } from "@/api/mdm/deviceMaintenancePlan";

export default {
  components: { infoForm },
  props: {
    machineTypes: Array,
  },
  inject: ["parentDict"],
  data() {
    return {
      loading: false,
      visible: false,
      isEdit: false,
      type: "",
      editType: null,
      form: {},
      rules: {
        factoryCode: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "blur",
          },
        ],
        machineType: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "blur",
          },
        ],
        machineCode: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        // yearMonth: [
        //   {
        //     required: true,
        //     message: this.$t("common.rule.select"),
        //     trigger: "blur",
        //   },
        // ],
        beginDate: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "blur",
          },
        ],
        endDay: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "blur",
          },
        ],
      },
    };
  },
  computed: {
    title: function () {
      return (
        (this.isEdit
          ? this.$t("common.button.edit")
          : this.$t("common.button.add")) +
        this.$t("ui.data.column.docDeviceMaintenancePlan.modelName")
      );
    },
    columns() {
      return [
        {
          prop: "factoryCode",
          label: this.$t("ui.data.colume.factory"),
          type: "select",
          dictData: this.parentDict.type.biz_factory_name,
        },
        {
          prop: "machineType",
          label: this.$t("ui.data.column.docDeviceMaintenancePlan.machineType"),
          type: "select",
          dictData: this.machineTypes,
        },
        {
          prop: "machineCode",
          label: this.machineCodeLabel,
        },
        {
          prop: "beginDate",
          label: this.$t("ui.data.column.docDeviceMaintenancePlan.startDate"),
          disabled: !this.form.machineType,
          type: "date",
          dateType: this.form.machineType === "2" ? "date" : "datetime",
          valueFormat:
            this.form.machineType === "2"
              ? "yyyy-MM-dd"
              : "yyyy-MM-dd HH:mm:00",
          format:
            this.form.machineType === "2"
              ? "yyyy-MM-dd"
              : "yyyy-MM-dd HH:mm:00",
        },
        {
          prop: "endDay",
          label: this.$t("ui.data.column.docDeviceMaintenancePlan.finallyDate"),
          disabled: !this.form.machineType,
          type: "date",
          dateType: this.form.machineType === "2" ? "date" : "datetime",
          valueFormat:
            this.form.machineType === "2"
              ? "yyyy-MM-dd"
              : "yyyy-MM-dd HH:mm:00",
          format:
            this.form.machineType === "2"
              ? "yyyy-MM-dd"
              : "yyyy-MM-dd HH:mm:00",
        },
        {
          prop: "yearMonth",
          label: this.$t("ui.data.colume.yearMonth"),
          disabled: true,
          type: "date",
          dateType: "month",
          valueFormat: "yyyy-MM",
        },
      ];
    },
    machineCodeLabel: function () {
      if (this.form.machineType === "0") {
        //成型
        return this.$t(
          "ui.data.column.docDeviceMaintenancePlan.moldingMachineId"
        );
      } else if (
        this.form.machineType === "1" ||
        this.form.machineType === "3"
      ) {
        return this.$t(
          "ui.data.column.docDeviceMaintenancePlan.curingMachineId"
        );
      } else if (this.form.machineType === "2") {
        return this.$t("ui.data.column.docDeviceMaintenancePlan.moldMachineId");
      } else {
        return this.$t("ui.data.column.docDeviceMaintenancePlan.machineId");
      }
    },
  },

  watch: {
    "form.beginDate": function (newVal) {
      if (newVal) {
        // 提取年份和月份
        this.form.yearMonth = newVal.substring(0, 7);
      } else {
        this.form.yearMonth = "";
      }
    },
  },
  methods: {
    // api
    async save(params) {
      try {
        this.loading = true;

        const res = await editDeviceMaintenancePlan(this.type, params);
        this.$modal.msgSuccess(res.msg);
        this.$emit("success");
        this.hide();
        this.loading = false;
      } catch (error) {
        console.log(error);
        this.loading = false;
      }
    },

    //utils
    show(type, data) {
      this.type = type;
      this.visible = true;
      if (data) {
        this.isEdit = true;
        this.form = {
          ...data,
          yearMonth: data.year + "-" + data.month,
          machineType: data.machineType + "",
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
      this.$refs.form.triggerConfirm(async (params) => {
        Object.keys(params).forEach((key) => {
          if (this.isEmpty(params[key])) {
            params[key] = "";
          }
        });
        // 选择的日期年月必须相同
        let bs = params.beginDate.slice(0, 7);
        let es = params.endDay.slice(0, 7);
        if (bs !== es) {
          this.$modal.msgError(
            this.$t(
              "ui.alert.message.productionCalendar.yearAndMonthMustBeTheSame"
            )
          );
          return;
        }

        // 解析年月
        let array = params.yearMonth.split("-");
        params.year = array[0];
        params.month = array[1];

        try {
          this.loading = true;
          this.save(params);
        } catch (error) {
          console.error(error);
          this.$modal.msgError(error.message);
          this.loading = false;
        }
      });
    },
  },
};
</script>
