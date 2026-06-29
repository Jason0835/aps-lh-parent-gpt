<template>
  <el-dialog
    :title="$t('ui.data.column.scheduleResult.publish')"
    :visible="visible"
    width="400px"
    @close="hide"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
  >
    <info-form
      class="form-item-height"
      ref="form"
      :form="form"
      :rules="rules"
      :columns="columns"
      label-position="right"
      label-width="100px"
      v-loading="loading"
    />
    <template slot="footer">
      <el-button @click="hide">{{ $t("common.button.cancel") }}</el-button>
      <el-button type="primary" :loading="loading" @click="handleConfirm">{{
        $t("common.button.confirm")
      }}</el-button>
    </template>
  </el-dialog>
</template>

<script>
import infoForm from "@/views/components/infoForm.vue";
import { publishScheduleResult } from "@/api/xwyy/xwyyScheduleResult";

export default {
  components: { infoForm },
  inject: ["parentDict"],
  props: {
    scheduleDate: { type: String, default: "" },
  },
  data() {
    return {
      loading: false,
      visible: false,
      ids: "",
      form: {},
      rules: {
        factoryCode: [{ required: true, message: this.$t("common.rule.select"), trigger: "blur" }],
        scheduleDate: [{ required: true, message: this.$t("common.rule.select"), trigger: "blur" }],
      },
    };
  },
  computed: {
    columns() {
      return [
        {
          label: this.$t("ui.data.column.factoryCode"),
          prop: "factoryCode",
          type: "select",
          dictData: this.parentDict.type.biz_factory_name,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.scheduleDate"),
          prop: "scheduleDate",
          type: "date",
          valueFormat: "yyyy-MM-dd",
        },
      ];
    },
  },
  methods: {
    show(selectedIds, scheduleDate, factoryCode) {
      this.visible = true;
      this.ids = selectedIds;
      this.form = {
        scheduleDate: scheduleDate || "",
        factoryCode: factoryCode || "116",
      };
    },
    hide() {
      this.form = {};
      this.ids = "";
      this.$refs.form.triggerResetForm();
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
        const result = await publishScheduleResult({
          scheduleDate: params.scheduleDate,
          factoryCode: params.factoryCode,
          ids: this.ids,
        });
        this.$modal.msgSuccess(result.msg);
        this.$emit("success");
        this.hide();
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
    },
  },
};
</script>