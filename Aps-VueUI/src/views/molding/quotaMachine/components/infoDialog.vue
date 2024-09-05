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
import moment from "moment";

import infoForm from "@/views/components/infoForm.vue";

import { editQuotaMachine } from "@/api/cx/quota";

export default {
  components: { infoForm },
  data() {
    return {
      loading: false,
      visible: false,
      isEdit: false,
      editType: null,
      form: {},
      rules: {
        machineType: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        specDimension: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
      },
      columns: [
        {
          label: this.$t("ui.data.column.machine.machineType"),
          prop: "machineType",
          span: 24,
          required: true,
          type: "select", //CX_MACHINE_TYPE
        },
        {
          label: this.$t("ui.data.column.cx.limit.specDimension"),
          prop: "specDimension",
          span: 24,
          required: true,
          type: "number",
          min: 0,
          max: 9999999,
        },
        {
          label: this.$t("ui.data.column.cx.setting.carcassBothLayer"),
          prop: "carcassBothLayer",
          span: 24,
          required: true,
          type: "number",
          min: 0,
          max: 999,
          precision: 0,
        },
        {
          label: this.$t("ui.data.column.cx.setting.reinforce"),
          prop: "reinforce",
          span: 24,
          type: "select", //LINE_TYPE
        },
        {
          label: this.$t("ui.data.column.cx.setting.tireType"),
          prop: "tireType",
          span: 24,
          type: "select", //TIRE_TYPE
        },
        {
          label: this.$t("ui.data.column.cx.setting.sectionWidthMinimum"),
          prop: "sectionWidthMinimum",
          span: 24,
          type: "number",
          min: 0,
          max: 99999999,
          precision: 0,
        },
        {
          label: this.$t("ui.data.column.cx.setting.sectionWidthMaximum"),
          prop: "sectionWidthMaximum",
          span: 24,
          type: "number",
          min: 0,
          max: 99999999,
          precision: 0,
        },
        {
          label: this.$t("ui.data.column.cx.setting.twoPersonQuota"),
          prop: "twoPersonQuota",
          span: 24,
          type: "number",
          min: 0,
          max: 99999999,
          precision: 0,
        },
        {
          label: this.$t("ui.data.column.cx.setting.onePersonQuota"),
          prop: "onePersonQuota",
          span: 24,
          type: "number",
          min: 0,
          max: 99999999,
          precision: 0,
        },
        {
          label: this.$t("ui.data.column.remark"),
          prop: "remark",
          span: 24,
          type: "textarea",
        },
      ],
    };
  },
  computed: {
    title: function () {
      return this.$t("ui.data.column.cx.setting.modelName");
    },
  },
  methods: {
    // api
    async save(params) {
      try {
        this.loading = true;

        const res = await editQuotaMachine(params);
        this.$modal.message(res.msg);
        this.$emit("success");
        this.hide();

        this.loading = false;
      } catch (error) {
        console.log(error);
        this.lading = false;
      }
    },

    //utils
    show(data) {
      this.visible = true;
      if (data) {
        this.isEdit = true;
        this.form = {
          ...data,
        };
      }
    },
    hide() {
      this.form = {};
      this.$refs.form.triggerResetForm();
      // this.resetForm("infoForm");
      this.isEdit = false;
      this.visible = false;
    },
    handleConfirm() {
      this.$refs.form.triggerConfirm(this.save);
    },
  },
};
</script>
