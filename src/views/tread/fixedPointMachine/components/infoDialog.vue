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
import { mapState } from "vuex";

import infoForm from "@/views/components/infoForm.vue";

import { editSpecifyMachine } from "@/api/tm/specifyMachine";

export default {
  components: { infoForm },
  inject: ["parentDict"],
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
    };
  },
  computed: {
    ...mapState({
      machines: (state) => state.tread.machines,
    }),
    title: function () {
      return this.$t("ui.tm.specifyMachine.column.modalName");
    },
    columns() {
      return [
        {
          label: this.$t("ui.tm.specifyMachine.column.treadCode"),
          prop: "treadCode",
          span: 24,
          required: true,
        },
        {
          label: this.$t("ui.specifyMachine.column.machineName"),
          prop: "machineId",
          span: 24,
          required: true,
          type: "select",
          dictData: this.machines,
          valueKey: "id",
          labelKey: "machineName",
        },
        {
          label: this.$t("ui.specifyMachine.column.lineType"),
          prop: "lineType",
          span: 24,
          type: "select", //LINE_TYPE
          dictData: this.parentDict.type.LINE_TYPE,
        },
        {
          label: this.$t("ui.specifyMachine.column.jobType"),
          prop: "jobType",
          span: 24,
          type: "select", //JOB_TYPE
          dictData: this.parentDict.type.JOB_TYPE,
        },
        {
          label: this.$t("ui.common.column.remark"),
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

        const res = await editSpecifyMachine(params);
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
