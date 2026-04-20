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
import infoForm from "@/views/components/infoForm.vue";
import { addMachine, editMachine, checkMachineCodeUnique } from "@/api/tq/machine";

export default {
  components: { infoForm },
  inject: ["parentDict"],
  data() {
    return {
      loading: false,
      visible: false,
      isEdit: false,
      form: {
        classShift: "2",
        openMachineClass: [],
        status: "0",
      },
      rules: {
        machineCode: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        machineName: [
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
        this.$t("ui.data.column.machine.info")
      );
    },
    columns() {
      return [
        {
          label: this.$t("ui.data.column.machine.machineCode"),
          prop: "machineCode",
          maxlength: "30",
          required: true,
          disabled: this.isEdit,
        },
        {
          label: this.$t("ui.data.column.machine.machineName"),
          prop: "machineName",
          maxlength: "20",
          required: true,
        },
        {
          label: this.$t("ui.data.column.machine.efficiency"),
          prop: "efficiency",
          type: "number",
          min: 0,
          max: 999999,
          precision: 0,
        },
        {
          label: this.$t("ui.data.column.machine.toolingInfo"),
          prop: "toolingInfo",
          maxlength: "50",
        },
        {
          label: this.$t("ui.data.column.machine.quata"),
          prop: "quata",
          type: "number",
          min: 0,
          max: 999999,
          precision: 2,
        },
        {
          label: this.$t("ui.data.column.machine.classShift"),
          prop: "classShift",
          render: (form) => {
            return (
              <el-radio-group v-model={form.classShift}>
                {this.parentDict.type.CLASS_SHIFT.map((row) => {
                  return (
                    <el-radio key={row.value} label={row.value}>
                      {row.label}
                    </el-radio>
                  );
                })}
              </el-radio-group>
            );
          },
        },
        {
          label: this.$t("ui.data.column.machine.openMachineClass"),
          prop: "openMachineClass",
          render: (form) => {
            if (form.classShift == "2") {
              return (
                <el-checkbox-group v-model={form.openMachineClass}>
                  {this.parentDict.type.CLASS_NUM.map((row) => {
                    return (
                      <el-checkbox
                        key={`CLASS_NUM_${row.value}`}
                        label={row.value}
                      >
                        {row.label}
                      </el-checkbox>
                    );
                  })}
                </el-checkbox-group>
              );
            } else {
              return (
                <el-checkbox-group v-model={form.openMachineClass}>
                  {this.parentDict.type.CLASS_NUM_THREE.map((row) => {
                    return (
                      <el-checkbox
                        key={`CLASS_NUM_THREE_${row.value}`}
                        label={row.value}
                      >
                        {row.label}
                      </el-checkbox>
                    );
                  })}
                </el-checkbox-group>
              );
            }
          },
        },
        {
          label: this.$t("ui.data.column.machine.status"),
          prop: "status",
          type: "switch",
        },
        {
          label: this.$t("ui.common.column.remark"),
          prop: "remark",
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
          res = await editMachine(params);
        } else {
          res = await addMachine(params);
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
          efficiency: this.numberEmpty(data.efficiency),
          quata: this.numberEmpty(data.quata),
          openMachineClass: data.openMachineClass
            ? data.openMachineClass.split(",")
            : [],
        };
      }
    },
    hide() {
      this.form = { classShift: "2", openMachineClass: [], status: "0" };
      this.$refs.form.triggerResetForm();
      this.isEdit = false;
      this.visible = false;
    },
    numberEmpty(val) {
      return this.isEmpty(val) ? undefined : val;
    },

    checkMachineCode(rule, value, callback) {
      return new Promise((resolve, reject) => {
        checkMachineCodeUnique({
          id: this.form.id,
          machineCode: this.form.machineCode,
        })
          .then((res) => {
            if (res === 0) {
              resolve();
            } else {
              reject(new Error(this.$t("ui.data.column.cx.machine.message")));
            }
          })
          .catch((error) => {
            console.error(error);
            reject(new Error(this.$t("验证失败，请稍后再试")));
          });
      });
    },

    handleConfirm() {
      this.$refs.form.triggerConfirm(async (params) => {
        if (params.openMachineClass) {
          params.openMachineClass = params.openMachineClass.join(",");
        }
        Object.keys(params).forEach((key) => {
          if (this.isEmpty(params[key])) {
            params[key] = "";
          }
        });

        try {
          this.loading = true;
          await this.checkMachineCode();
          this.save(params);
        } catch (error) {
          console.error(error);
          this.$modal.msgError(error.message);
          this.loading = false;
        }
      });
    },
  },
};
</script>
