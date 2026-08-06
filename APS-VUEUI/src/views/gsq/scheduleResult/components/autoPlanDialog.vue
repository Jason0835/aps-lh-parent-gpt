<template>
  <el-dialog
    :title="title"
    :visible.sync="dialogVisible"
    width="420px"
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
      label-width="130px"
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
import { autoPlan } from "@/api/gsq/scheduleResult";

export default {
  name: "GsqAutoPlanDialog",
  components: { infoForm },
  dicts: ["biz_factory_name"],
  props: {
    visible: {
      type: Boolean,
      default: false,
    },
    // 默认工厂，由列表页查询条件带入（默认越南 116）
    defaultFactoryCode: {
      type: String,
      default: "",
    },
    // 默认排程日期，由列表页查询条件带入
    defaultScheduleDate: {
      type: String,
      default: "",
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
      return this.$t("ui.data.column.gsqScheduleResult.autoPlanTitle");
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
      ];
    },
  },
  created() {
    // 排程日期默认取列表页查询条件，未传时默认 D+1（钢丝圈排程日期对应D+1）
    this.form = {
      factoryCode: this.defaultFactoryCode || "",
      scheduleDateQuery:
        this.defaultScheduleDate || moment().add(1, "days").format("YYYY-MM-DD"),
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
      this.loading = true;
      autoPlan(this.form)
        .then((res) => {
          const tip = res.msg || res.message || "";
          if (res.code != null && res.code !== 200) {
            this.$modal.msgError(tip || this.$t("ui.common.message.operateFail"));
            return;
          }
          this.$modal.msgSuccess(
            tip || this.$t("ui.data.column.gsqScheduleResult.autoPlanSuccess")
          );
          this.$emit("refresh");
          this.hide();
        })
        .catch(() => {
          this.$modal.msgWarning(
            this.$t("ui.data.column.gsqScheduleResult.autoPlanTimeout")
          );
        })
        .finally(() => {
          this.loading = false;
        });
    },
  },
};
</script>
