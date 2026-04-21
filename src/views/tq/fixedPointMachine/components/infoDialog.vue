<template>
  <el-dialog
    :title="title"
    :visible="visible"
    width="600px"
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
      label-width="150px"
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
import { addSpecifyMachine, editSpecifyMachine } from "@/api/tq/specifyMachine";

export default {
  components: { infoForm },
  inject: ["parentDict"],
  data() {
    return {
      loading: false,
      visible: false,
      isEdit: false,
      form: {},
      rules: {
        beadCode: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        machineId: [
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
    ...mapState({
      machines: (state) => state.bead.machines,
    }),
    title: function () {
      return (
        (this.isEdit
          ? this.$t("common.button.edit")
          : this.$t("common.button.add")) +
        this.$t("ui.tq.specifyMachine.column.modalName")
      );
    },
    columns() {
      return [
        {
          label: this.$t("ui.tq.specifyMachine.column.beadCode"),
          prop: "beadCode",
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
          type: "select",
          dictData: this.parentDict.type.LINE_TYPE,
        },
        {
          label: this.$t("ui.specifyMachine.column.jobType"),
          prop: "jobType",
          span: 24,
          type: "select",
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
    async save(params) {
      try {
        this.loading = true;
        let res;
        if (this.isEdit) {
          res = await editSpecifyMachine(params);
        } else {
          res = await addSpecifyMachine(params);
        }
        this.$modal.msgSuccess(res.msg);
        this.$emit("success");
        this.hide();
      } catch (error) {
        console.log(error);
      } finally {
        this.loading = false;
      }
    },
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
      this.isEdit = false;
      this.visible = false;
    },
    handleConfirm() {
      this.$refs.form.triggerConfirm(this.save);
    },
  },
};
</script>
