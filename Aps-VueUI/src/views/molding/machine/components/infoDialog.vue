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

import { editMachine, checkMachineCodeUnique } from "@/api/cx/machine";

export default {
  components: { infoForm },
  data() {
    return {
      loading: false,
      visible: false,
      isEdit: false,
      editType: null,
      form: {
      },
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
          label: this.$t("ui.data.column.cx.machine.machineCode"),
          prop: "machineCode",
          span: 24,
          required: true,
        },
        {
          label: this.$t("ui.data.column.cx.machine.machineName"),
          prop: "machineName",
          span: 24,
          required: true,
        },
        {
          label: this.$t("ui.data.column.machine.machineType"),
          prop: "type",
          span: 24,
          type: "select", //CX_MACHINE_TYPE
        },
        {
          label: this.$t("ui.data.column.cx.machine.type"),
          prop: "machineType",
          span: 24,
          type: "select", //MACHINE_TYPE
        },
        {
          label: this.$t("ui.data.column.cx.machine.dimensionMiniMum"),
          prop: "dimensionMiniMum",
          span: 24,
        },
        {
          label: this.$t("ui.data.column.cx.machine.dimensionMaxiMum"),
          prop: "dimensionMaxiMum",
          span: 24,
        },
        {
          label: this.$t("ui.data.column.cx.machine.operatorQty"),
          prop: "operatorQty",
          span: 24,
        },
        {
          label: this.$t("ui.data.column.cx.machine.quata"),
          prop: "quata",
          span: 24,
          required: true,
        },
      ],
    };
  },
  computed: {
    title: function () {
      return this.$t("ui.data.column.cxScheduleResult.cxAutoPlan");
    },
  },
  methods: {
    // api
    async save(params) {
      try {
        this.loading = true;

        const res = await editMachine();

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
    checkMachineCode(rule, value, callback) {
      checkMachineCodeUnique({
        machineId: this.form.machineId,
        machineCode: value,
      })
        .then((res) => {
          console.log(res, 'gjy')
          if (res === 0) {
            callback();
          } else {
            callback(
              new Error(this.$t("ui.data.column.cx.machine.message"))
            );
          }
        })
        .catch((error) => {
          console.error(error);
          callback(new Error("验证失败，请稍后再试"));
        });
    },
    checkMachineName(rule, value, callback) {
      checkMachineCodeUnique({
        machineId: this.form.machineId,
        machineName: value,
      })
        .then((res) => {
          if (res === 0) {
            callback();
          } else {
            callback(new Error(this.$t("ui.data.column.cx.machineName.message")));
          }
        })
        .then((error) => {
          console.error(error);
          callback(new Error("验证失败，请稍后再试"));
        });
    },


    handleConfirm() {
      console.log(111)
      console.log(this.form)

      // this.$refs.form.validate((valid) => {
      //   if (valid) {
      //     this.save({
      //       ...this.form,
      //     });
      //   }
      // });
    },
  },
};
</script>
