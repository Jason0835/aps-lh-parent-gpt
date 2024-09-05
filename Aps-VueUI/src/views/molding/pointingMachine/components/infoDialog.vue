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

import { editCxSpecifyMachine } from "@/api/cx/cxSpecifyMachine";

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
        machineCode: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
          {
            validator: this.checkMachineCode,
            trigger: "blur",
          },
        ],
        machineName: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
          {
            validator: this.checkMachineName,
            trigger: "blur",
          },
        ],
        quata: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
      },
      columns: [
        {
          label: this.$t("ui.data.column.specifyMachine.sapCode"),
          prop: "sapCode",
          span: 24,
          required: true,
        },
        {
          label: this.$t("ui.data.column.specifyMachine.embryoCode"),
          prop: "embryoCode",
          span: 24,
          required: true,
        },
        {
          label: this.$t("ui.data.column.specifyMachine.machineCode"),
          prop: "machineCode",
          span: 24,
          required: true,
          type: "select",
        },
        {
          label: this.$t("ui.data.column.specifyMachine.lineType"),
          prop: "lineType",
          span: 24,
          type: "select", //LINE_TYPE
        },
        {
          label: this.$t("ui.data.column.specifyMachine.jobType"),
          prop: "jobType",
          span: 24,
          type: "select", //JOB_TYPE
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
      return this.$t("ui.data.column.cx.specifyMachine.modalName");
    },
  },
  methods: {
    // api
    async save(params) {
      try {
        this.loading = true;

        const res = await editMachine(params);
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
