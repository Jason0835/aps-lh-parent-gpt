<template>
  <el-dialog
    :title="title"
    :visible="visible"
    width="640px"
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
      label-width="140px"
      v-loading="loading"
    />
    <template slot="footer">
      <el-button @click="hide">{{ $t("common.button.cancel") }}</el-button>
      <el-button type="primary" :loading="loading" @click="handleConfirm">{{ $t("common.button.confirm") }}</el-button>
    </template>
  </el-dialog>
</template>

<script>
import { addAngleWidthMapping, updateAngleWidthMapping } from "@/api/cd15/angleWidthMapping";
import infoForm from "@/views/components/infoForm.vue";

export default {
  components: { infoForm },
  inject: ["parentDict"],
  data() {
    const requiredSelect = {
      required: true,
      message: this.$t("common.rule.select"),
      trigger: "change",
    };
    const requiredInput = {
      required: true,
      message: this.$t("common.rule.input"),
      trigger: "blur",
    };
    const positiveNumber = {
      validator: (rule, value, callback) => {
        if (value === undefined || value === null || value === "") {
          callback(new Error(this.$t("common.rule.input")));
          return;
        }
        if (Number(value) <= 0) {
          callback(new Error(this.$t("ui.data.column.cd15AngleWidthMapping.widthPositive")));
          return;
        }
        callback();
      },
      trigger: "blur",
    };
    return {
      loading: false,
      visible: false,
      isEdit: false,
      form: {},
      rules: {
        factoryCode: [requiredSelect],
        cutAngle: [requiredSelect],
        clothWidthMax: [requiredInput, positiveNumber],
      },
    };
  },
  computed: {
    title() {
      return this.isEdit ? this.$t("common.button.edit") : this.$t("common.button.add");
    },
    columns() {
      return [
        {
          prop: "factoryCode",
          label: this.$t("ui.data.column.cd15AngleWidthMapping.factoryCode"),
          type: "select",
          dictData: this.parentDict.type.biz_factory_name,
          filterable: true,
        },
        {
          prop: "cutAngle",
          label: this.$t("ui.data.column.cd15AngleWidthMapping.cutAngle"),
          type: "select",
          dictData: this.parentDict.type.cd15_cut_angle,
          filterable: true,
        },
        {
          prop: "clothWidthMax",
          label: this.$t("ui.data.column.cd15AngleWidthMapping.clothWidthMax"),
          type: "number",
          min: 0,
          precision: 4,
        },
        {
          prop: "remark",
          label: this.$t("ui.common.column.remark"),
          type: "textarea",
          rows: 3,
          maxlength: 900,
        },
      ];
    },
  },
  methods: {
    async save(params) {
      this.loading = true;
      try {
        const res = this.isEdit
          ? await updateAngleWidthMapping(params)
          : await addAngleWidthMapping(params);
        this.$modal.msgSuccess(res.msg);
        this.$emit("success");
        this.hide();
      } finally {
        this.loading = false;
      }
    },
    show(data) {
      this.visible = true;
      if (data) {
        this.isEdit = true;
        this.form = { ...data };
      } else {
        this.form = {
          factoryCode: "116",
        };
      }
    },
    hide() {
      this.form = {};
      this.$refs.form && this.$refs.form.triggerResetForm();
      this.isEdit = false;
      this.visible = false;
    },
    handleConfirm() {
      this.$refs.form.triggerConfirm(this.save);
    },
  },
};
</script>
