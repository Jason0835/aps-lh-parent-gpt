<template>
  <el-dialog
    :title="title"
    :visible.sync="dialogVisible"
    width="640px"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    append-to-body
  >
    <el-alert
      v-if="row"
      :title="
        $t('ui.data.column.gsqScheduleResult.currentRowTip') +
        '：' +
        row.steelRingCode +
        ' / ' +
        row.machineCode
      "
      type="info"
      :closable="false"
      style="margin-bottom: 12px"
    />
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
import { changeQty, validateChangeQty } from "@/api/gsq/scheduleResult";

export default {
  name: "GsqChangeQtyDialog",
  components: { infoForm },
  props: {
    visible: {
      type: Boolean,
      default: false,
    },
    row: {
      type: Object,
      default: null,
    },
  },
  data() {
    return {
      loading: false,
      form: {},
      rules: {
        class1PlanQty: [
          {
            type: "number",
            min: 0,
            message: this.$t("ui.data.column.gsqScheduleResult.qtyMustBeNonNegative"),
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
      return this.$t("ui.data.column.gsqScheduleResult.changeQtyTitle");
    },
    columns() {
      return [
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
  watch: {
    visible(val) {
      if (val && this.row) {
        // 用当前行6个班次计划量初始化
        this.form = {
          id: this.row.id,
          scheduleDateQuery: this.row.scheduleDate,
          factoryCode: this.row.factoryCode,
          steelRingCode: this.row.steelRingCode,
          machineCode: this.row.machineCode,
          class1PlanQty: this.row.class1PlanQty || 0,
          class2PlanQty: this.row.class2PlanQty || 0,
          class3PlanQty: this.row.class3PlanQty || 0,
          class4PlanQty: this.row.class4PlanQty || 0,
          class5PlanQty: this.row.class5PlanQty || 0,
          class6PlanQty: this.row.class6PlanQty || 0,
          remark: this.row.remark || "",
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
      this.loading = true;
      validateChangeQty(this.form)
        .then((res) => {
          if (res.code !== 200) {
            this.$modal.msgError(res.msg);
            return Promise.reject();
          }
          return changeQty(this.form);
        })
        .then((res) => {
          const tip = res.msg || res.message || "";
          if (res.code != null && res.code !== 200) {
            this.$modal.msgError(tip || this.$t("ui.common.message.operateFail"));
            return;
          }
          this.$modal.msgSuccess(
            tip || this.$t("ui.data.column.gsqScheduleResult.changeQtySuccess")
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
