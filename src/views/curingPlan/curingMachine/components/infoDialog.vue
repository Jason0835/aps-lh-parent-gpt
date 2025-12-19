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
import { editMachine, checkMachineCodeUnique } from "@/api/lh/machine";
// import { editVulcanizingMachine } from "@/api/mdm/vulcanizingMachine";

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
        factoryCode: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "blur",
          },
        ],
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
        dimension: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        dimensionMinimum: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        dimensionMinimum: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        maxMoldNum: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        single: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "blur",
          },
        ],
        quota: [
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
      return this.isEdit
        ? this.$t("common.button.edit")
        : this.$t("common.button.add");
    },
    columns() {
      return [
        {
          label: this.$t("ui.data.column.machine.factoryCode"),
          prop: "factoryCode",
          type: "select",
          dictData: this.parentDict.type.biz_factory_name,
          disabled: this.isEdit,
        },
        {
          label: this.$t("ui.data.column.machine.machineCode"),
          prop: "machineCode",
          disabled: this.isEdit,
        },
        {
          label: this.$t("ui.data.column.machine.machineName"),
          prop: "machineName",
        },
        {
          label: this.$t("ui.data.column.machine.lhMachineType"),
          prop: "machineType",
          type: "select",
          dictData: this.parentDict.type.LH_MACHINE_TYPE,
        },

        // {
        //   label: this.$t("ui.data.column.machine.dimension"),
        //   prop: "dimension",
        //   type: "number",
        //   attrs: {
        //     class: "w100",
        //     controls: false,
        //     precision: 2,
        //     min: 0,
        //     max: 9999.99,
        //   },
        // },
        {
          label: this.$t("ui.data.column.machine.dimensionMinmum"),
          prop: "dimensionMinimum",
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
          label: this.$t("ui.data.column.machine.mouldNum"),
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
          prop: "quota",
          type: "number",
          attrs: {
            class: "w100",
            controls: true,
            "controls-position": "right",
            precision: 2,
            min: 0,
            max: 999999,
          },
        },
        // {
        //   label: this.$t("ui.data.column.machine.single"),
        //   prop: "single",
        //   type: "select",
        //   dictData: this.parentDict.type.biz_yes_no,
        // },
        {
          label: this.$t("ui.data.column.machine.machineOrder"),
          prop: "machineOrder",
          type: "number",
          attrs: {
            class: "w100",
            controls: true,
            "controls-position": "right",
            precision: 0,
            min: 0,
            max: 99999999999,
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
          type: "textarea",
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
          ...data,
          machineOrder: this.numberEmpty(data.machineOrder),
          dimension: this.numberEmpty(data.dimension),
          dimensionMinimum: this.numberEmpty(data.dimensionMinimum),
          dimensionMaximum: this.numberEmpty(data.dimensionMaximum),
          maxMoldNum: this.numberEmpty(data.maxMoldNum),
          quota: this.numberEmpty(data.quota),
          openMachineClass: data.openMachineClass
            ? data.openMachineClass.split(",")
            : [],
          // mouldType: data.mouldType ? data.mouldType.split(",") : [],
        };
      } else {
        this.form = {
          classShift: "2",
          openMachineClass: [],
        };
      }
    },
    hide() {
      this.$refs.form.triggerResetForm();
      // this.resetForm("infoForm");
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
        // if (params.mouldType) {
        //   params.mouldType = params.mouldType.join(",");
        // }
        if (params.openMachineClass) {
          params.openMachineClass = params.openMachineClass.join(",");
        }
        Object.keys(params).forEach((key) => {
          if (this.isEmpty(params[key])) {
            params[key] = "";
          }
        });
        this.save(params);
        // try {
        //   this.loading = true;
        //   await this.checkMachineCode();
        //   await this.checkMachineName();
        //   this.save(params);
        // } catch (error) {
        //   console.error(error);
        //   this.$modal.msgError(error.message);
        //   this.loading = false;
        // }
      });
    },
  },
};
</script>
