<template>
  <el-dialog
    :title="title"
    :visible.sync="dialogVisible"
    width="640px"
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
import moment from "moment";
import infoForm from "@/views/components/infoForm.vue";
import { insertOrder, validateInsertOrder } from "@/api/gsq/scheduleResult";

export default {
  name: "GsqInsertOrderDialog",
  components: { infoForm },
  dicts: ["biz_factory_name"],
  props: {
    visible: {
      type: Boolean,
      default: false,
    },
  },
  data() {
    return {
      loading: false,
      form: {},
      rules: {
        factoryCode: [
          {
            required: true,
            message: this.$t("ui.data.column.gsqScheduleResult.factoryCodeRequired"),
            trigger: "change",
          },
        ],
        scheduleDateQuery: [
          {
            required: true,
            message: this.$t("ui.data.column.gsqScheduleResult.scheduleDateRequired"),
            trigger: "blur",
          },
        ],
        steelRingCode: [
          {
            required: true,
            message: this.$t("ui.data.column.gsqScheduleResult.steelRingCodeRequired"),
            trigger: "blur",
          },
        ],
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
      return this.$t("ui.data.column.gsqScheduleResult.insertOrderTitle");
    },
    columns() {
      return [
        {
          label: this.$t("ui.data.column.gsqScheduleResult.factoryCode"),
          prop: "factoryCode",
          type: "select",
          dictData: this.dict.type.biz_factory_name,
          filterable: true,
        },
        {
          label: this.$t("ui.data.column.gsqScheduleResult.scheduleDate"),
          prop: "scheduleDateQuery",
          type: "date",
          dateType: "date",
          valueFormat: "yyyy-MM-dd",
          format: "yyyy-MM-dd",
          clearable: false,
        },
        {
          label: this.$t("ui.data.column.gsqScheduleResult.steelRingCode"),
          prop: "steelRingCode",
          type: "input",
        },
        {
          label: this.$t("ui.data.column.gsqScheduleResult.twiningDiscCode"),
          prop: "twiningDiscCode",
          type: "input",
        },
        {
          label: this.$t("ui.data.column.gsqScheduleResult.machineCode"),
          prop: "machineCode",
          type: "input",
        },
        // 6个班次计划量（1班=D日中班，2/3/4班=D+1日夜早中，5/6班=D+2日夜早）
        {
          label: this.$t("ui.data.column.gsqScheduleResult.class1PlanQty"),
          prop: "class1PlanQty",
          type: "number",
          min: 0,
        },
        {
          label: this.$t("ui.data.column.gsqScheduleResult.class2PlanQty"),
          prop: "class2PlanQty",
          type: "number",
          min: 0,
        },
        {
          label: this.$t("ui.data.column.gsqScheduleResult.class3PlanQty"),
          prop: "class3PlanQty",
          type: "number",
          min: 0,
        },
        {
          label: this.$t("ui.data.column.gsqScheduleResult.class4PlanQty"),
          prop: "class4PlanQty",
          type: "number",
          min: 0,
        },
        {
          label: this.$t("ui.data.column.gsqScheduleResult.class5PlanQty"),
          prop: "class5PlanQty",
          type: "number",
          min: 0,
        },
        {
          label: this.$t("ui.data.column.gsqScheduleResult.class6PlanQty"),
          prop: "class6PlanQty",
          type: "number",
          min: 0,
        },
        {
          label: this.$t("ui.data.column.gsqScheduleResult.remark"),
          prop: "remark",
          type: "input",
        },
      ];
    },
  },
  created() {
    this.form = {
      factoryCode: "",
      scheduleDateQuery: moment().add(1, "days").format("YYYY-MM-DD"),
      steelRingCode: "",
      twiningDiscCode: "",
      machineCode: "",
      class1PlanQty: 0,
      class2PlanQty: 0,
      class3PlanQty: 0,
      class4PlanQty: 0,
      class5PlanQty: 0,
      class6PlanQty: 0,
      remark: "",
    };
  },
  methods: {
    hide() {
      this.dialogVisible = false;
    },
    handleConfirm() {
      this.$refs.form.triggerConfirm(this.submit);
    },
    submit() {
      const total =
        (Number(this.form.class1PlanQty) || 0) +
        (Number(this.form.class2PlanQty) || 0) +
        (Number(this.form.class3PlanQty) || 0) +
        (Number(this.form.class4PlanQty) || 0) +
        (Number(this.form.class5PlanQty) || 0) +
        (Number(this.form.class6PlanQty) || 0);
      if (total <= 0) {
        this.$modal.msgError(
          this.$t("ui.data.column.gsqScheduleResult.qtyCannotBeZero")
        );
        return;
      }
      this.loading = true;
      validateInsertOrder(this.form)
        .then((res) => {
          if (res.code !== 200) {
            this.$modal.msgError(res.msg);
            return Promise.reject();
          }
          return insertOrder(this.form);
        })
        .then((res) => {
          const tip = res.msg || res.message || "";
          if (res.code != null && res.code !== 200) {
            this.$modal.msgError(tip || this.$t("ui.common.message.operateFail"));
            return;
          }
          this.$modal.msgSuccess(
            tip || this.$t("ui.data.column.gsqScheduleResult.insertOrderSuccess")
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
