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

import { editQuota } from "@/api/cx/quota";

export default {
  components: { infoForm },
  inject: ["parentDict"],
  data() {
    return {
      loading: false,
      visible: false,
      isEdit: false,
      editType: null,
      form: {
        reinforce: "1",
      },
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
        carcassBothLayer: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        tireType: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        sectionWidthMinimum: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        sectionWidthMaximum: [
          {
            required: true,
            message: this.$t("common.rule.input"),
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
        this.$t("ui.data.column.cx.setting.modelName")
      );
    },
    columns() {
      return [
        {
          label: this.$t("ui.data.column.machine.machineType"),
          prop: "machineType",
          span: 24,
          required: true,
          type: "select", //CX_MACHINE_TYPE
          dictData: this.parentDict.type.CX_MACHINE_TYPE,
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
          type: "switch",
        },
        {
          label: this.$t("ui.data.column.cx.setting.tireType"),
          prop: "tireType",
          span: 24,
          type: "select", //TIRE_TYPE
          dictData: this.parentDict.type.TIRE_TYPE,
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
      ];
    },
  },
  methods: {
    // api
    async save(params) {
      try {
        this.loading = true;

        const res = await editQuota(params);
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
      // this.form = {};
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
