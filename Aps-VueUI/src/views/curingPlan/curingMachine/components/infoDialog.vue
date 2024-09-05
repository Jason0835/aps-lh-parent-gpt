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
      :defaultValue="defaultValue"
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
import infoForm from "@/views/components/infoForm.vue";
import { editMachine } from "@/api/lh/machine";
import CuringMachineSelect from "@/views/components/CuringMachineSelect.vue";
export default {
  components: { infoForm, CuringMachineSelect },
  data() {
    return {
      loading: false,
      visible: false,
      isEdit: false,
      defaultValue: {},
      rules: {
        machineCode: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        planDate: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        tireRoughStock: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
      },
      columns: [
        {
          label: this.$t("ui.data.column.machine.machineCode"),
          prop: "machineCode",
        },
        {
          label: this.$t("ui.data.column.machine.machineName"),
          prop: "machineName",
        },
        {
          label: this.$t("ui.data.column.machine.dimension"),
          prop: "dimension",
          attrs: {
            class: "w100",
            controls: false,
            precision: 2,
            min: 0,
            max: 9999.99,
          },
        },
        {
          label: this.$t("ui.data.column.machine.dimensionMinmum"),
          prop: "dimensionMinmum",
          type: "number",
          attrs: {
            class: "w100",
            controls: false,
            precision: 2,
            min: 0,
            max: 9999.99,
          },
        },
        {
          label: this.$t("ui.data.column.machine.dimensionMaximum"),
          prop: "dimensionMaximum",
          type: "number",
          attrs: {
            class: "w100",
            controls: false,
            precision: 2,
            min: 0,
            max: 9999.99,
          },
        },
        {
          label: this.$t("ui.data.column.machine.centripetalMechanism"),
          prop: "centripetalMechanism",
          render: (form) => {
            return (
              <el-radio-group v-model={form.centripetalMechanism}>
                <el-radio label="1">向心机构1</el-radio>
                <el-radio label="2">向心机构2</el-radio>
              </el-radio-group>
            );
          },
        },
        {
          label: this.$t("ui.data.column.machine.maxMoldNum"),
          prop: "maxMoldNum",
          type: "number",
          attrs: {
            class: "w100",
            controls: true,
            "controls-position": "right",
            precision: 0,
            min: 0,
            max: 10,
          },
        },
        {
          label: this.$t("ui.data.column.machine.quata"),
          prop: "quata",
          type: "number",
          attrs: {
            class: "w100",
            controls: true,
            "controls-position": "right",
            precision: 0,
            min: 0,
            max: 999999,
          },
        },
        {
          label: this.$t("ui.data.column.machine.classShift"),
          prop: "classShift",
          render: (form) => {
            return (
              <el-radio-group v-model={form.classShift}>
                <el-radio label="2">两班制</el-radio>
                <el-radio label="3">三班制</el-radio>
              </el-radio-group>
            );
          },
        },
        {
          label: this.$t("ui.data.column.machine.openMachineClass"),
          prop: "openMachineClass",
          render: (form) => {
            return (
              <el-radio-group v-model={form.classShift}>
                <el-radio label="2">两班制</el-radio>
                <el-radio label="3">三班制</el-radio>
              </el-radio-group>
            );
          },
        },
        {
          label: this.$t("ui.data.column.machine.status"),
          prop: "status",
          render: (form) => {
            if (form.classShift == "2") {
              return (
                <el-checkbox-group v-model={form.classShift}>
                  <el-checkbox label="2">中班</el-checkbox>
                  <el-checkbox label="3">夜班</el-checkbox>
                </el-checkbox-group>
              );
            } else {
              return (
                <el-checkbox-group v-model={form.classShift}>
                  <el-checkbox label="1">白班</el-checkbox>
                  <el-checkbox label="2">中班</el-checkbox>
                  <el-checkbox label="3">夜班</el-checkbox>
                </el-checkbox-group>
              );
            }
          },
        },
        {
          label: this.$t("ui.common.column.remark"),
          prop: "remark",
        },
      ],
    };
  },
  computed: {
    title: function () {
      return this.$t("修改APS模具变动单");
    },
  },
  methods: {
    // api
    async save(params) {
      // console.log(params);
      try {
        this.loading = true;
        const data = await editMachine(params);
        this.$modal.msgSuccess(data.msg);
        this.$emit("success");
        this.hide();
      } catch (error) {
        console.log(error);
      } finally {
        this.loading = false;
      }
    },

    //utils
    show(data) {
      this.visible = true;
      if (data) {
        this.isEdit = true;
        this.defaultValue = {
          ...data,
        };
      }
    },
    hide() {
      this.defaultValue = {};
      this.$refs.form.triggerResetForm();
      // this.resetForm("infoForm");
      this.isEdit = false;
      this.visible = false;
    },

    handleConfirm() {
      this.$refs.form.triggerConfirm(this.save);
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
