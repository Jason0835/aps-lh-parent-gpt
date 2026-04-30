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
import { editMachine, checkMachineCodeUnique } from "@/api/tm/machine";
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
          label: this.$t("ui.data.column.machine.widthMin"),
          prop: "widthMin",
          type: "number",
          min: 0,
          max: 999999,
          precision: 0,
        },
        {
          label: this.$t("ui.data.column.machine.widthMax"),
          prop: "widthMax",
          type: "number",
          min: 0,
          max: 999999,
          precision: 0,
        },
        {
          label: this.$t("ui.data.column.machine.thickMin"),
          prop: "thickMin",
          type: "number",
          min: 0,
          max: 999999,
          precision: 0,
        },
        {
          label: this.$t("ui.data.column.machine.thickMax"),
          prop: "thickMax",
          type: "number",
          min: 0,
          max: 999999,
          precision: 0,
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
          render: (form) => {
            return (
              <el-switch
                active-value="0"
                inactive-value="1"
                v-model={form.status}
              />
            );
          },
        },
        {
          label: this.$t("ui.common.column.remark"),
          prop: "remark",
        },
      ];
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
        this.form = {
          id: data.id,
          machineCode: data.machineCode,
          machineName: data.machineName,
          widthMin: data.widthMin ? data.widthMin : undefined,
          widthMax: data.widthMax ? data.widthMax : undefined,
          thickMin: data.thickMin ? data.thickMin : undefined,
          thickMax: data.thickMax ? data.thickMax : undefined,
          quata: data.quata ? data.quata : undefined,
          classShift: data.classShift,
          openMachineClass: data.openMachineClass
            ? data.openMachineClass.split(",")
            : [],
          status: data.status,
          remark: data.remark,
        };
      }
    },
    hide() {
      this.form = { classShift: "2", openMachineClass: [] };
      // this.$refs.form.triggerResetForm();
      // this.resetForm("infoForm");
      this.isEdit = false;
      this.visible = false;
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
            reject(new Error("验证失败，请稍后再试"));
          });
      });
    },
    checkMachineName() {
      return new Promise((resolve, reject) => {
        setTimeout(() => {
          checkMachineCodeUnique({
            id: this.form.id,
            machineName: this.form.machineName,
          })
            .then((res) => {
              if (res === 0) {
                resolve();
              } else {
                reject(
                  new Error(this.$t("ui.data.column.cx.machineName.message"))
                );
              }
            })
            .catch((error) => {
              console.error(error);
              reject(new Error("验证失败，请稍后再试"));
            });
        }, 201);
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
          await this.checkMachineName();
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
