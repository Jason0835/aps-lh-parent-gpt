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
import { listMouldSleeve } from "@/api/mdm/mdmModelInfo";

export default {
  components: { infoForm },
  inject: ["parentDict"],
  data() {
    const validatePositiveInteger = (rule, value, callback) => {
      if (value === "" || value === null || value === undefined) {
        if (rule.required) {
          return callback(new Error(this.$t("common.rule.noData")));
        }
        return callback();
      }
      const strValue = String(value).trim();

      if (!/^\d+$/.test(strValue)) {
        return callback(
          new Error(this.$t("common.rule.noPoint"))
        );
      }

      const numValue = Number(strValue);
      if (numValue > 999999) {
        return callback(new Error(this.$t("common.rule.inoutMax")));
      }

      if (!Number.isInteger(numValue)) {
        return callback(new Error(this.$t("common.rule.peleaseInteger")));
      }

      callback();
    };
    return {
      loading: false,
      visible: false,
      isEdit: false,
      mouldSleeveOptions: [],
      form: {
        classShift: "2",
        openMachineClass: [],
      },
      rules: {
        factoryCode: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
        machineCode: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "change",
          },
        ],
        machineName: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "change",

          },
        ],
        dimension: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "change",
          },
        ],
        dimensionMinimum: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "change",
          },
        ],
        maxMoldNum: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "change",
          },
        ],
        single: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
        support195WideBase: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
        support225WideBase: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
        supportChipTire: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
        quota: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "change",
          },
          {
            validator: (rule, value, callback) => {
              validatePositiveInteger({ required: true }, value, callback);
            },
            trigger: ["change"],
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
          label: this.$t("common.factory"),
          prop: "factoryCode",
          type: "select",
          dictData: this.parentDict.type.biz_factory_name,
          disabled: this.isEdit,
        },
        {
          label: this.$t("ui.data.column.machine.machineCode"),
          prop: "machineCode",
          disabled: this.isEdit,
          maxlength:30,
        },
        {
          label: this.$t("ui.data.column.machine.machineName"),
          prop: "machineName",
          maxlength:20,
        },
        {
          label: this.$t("ui.data.column.machine.manufacturer"),
          prop: "manufacturer",
          maxlength:50,
        },
        {
          label: this.$t("ui.data.column.machine.lhMachineType"),
          prop: "machineType",
          type: "select",
          dictData: this.parentDict.type.LH_MACHINE_TYPE,
        },
        {
          label: this.$t("ui.data.column.machine.dimensionSize"),
          prop: "dimensionSize",
          maxlength:20,
        },
        {
          label: this.$t("ui.data.column.machine.hotPlateDiameter"),
          prop: "hotPlateDiameter",
          maxlength:20,
        },
        {
          label: this.$t("ui.data.column.machine.shellStandard"),
          prop: "shellStandard",
          type: "select",
          options: this.mouldSleeveOptions,
          filterable: true,
          attrs: {
            multiple: true,
          },
        },
        {
          label: this.$t("ui.data.column.lhMachineInfo.dimensionMinimum"),
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
          label: this.$t("ui.data.column.lhMachineInfo.dimensionMaximum"),
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
          label: this.$t("ui.data.column.machine.support195WideBase"),
          prop: "support195WideBase",
          type: "select",
          dictData: this.parentDict.type.biz_yes_no,
        },
        {
          label: this.$t("ui.data.column.machine.support225WideBase"),
          prop: "support225WideBase",
          type: "select",
          dictData: this.parentDict.type.biz_yes_no,
        },
        {
          label: this.$t("ui.data.column.machine.supportChipTire"),
          prop: "supportChipTire",
          type: "select",
          dictData: this.parentDict.type.biz_yes_no,
        },
        {
          label: this.$t("ui.data.column.lhMachineInfo.maxMoldNum"),
          prop: "maxMoldNum",
          type: "number",
          attrs: {
            class: "w100",
            controls: true,
            "controls-position": "right",
            precision: 0,
            min: 0,
            max: 255,
          },
        },
        {
          label: this.$t("ui.data.column.machine.quata"),
          prop: "quota",
          type: "number",
          // attrs: {
          //   class: "w100",
          //   controls: true,
          //   "controls-position": "right",
          //   precision: 2,

          // },
          min: 0,
          max: 999999,
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
            max: 999999,
          },
        },
        {
          label: this.$t("ui.data.column.machine.status"),
          prop: "status",
          type: "switch",
          activeValue: "1",
          inactiveValue: "0",
        },
        {
          label: this.$t("ui.common.column.remark"),
          prop: "remark",
          type: "textarea",
          maxlength:100
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
    async getMouldSleeveOptions() {
      try {
        const res = await listMouldSleeve();
        this.mouldSleeveOptions = res.map(item => ({
          label: item,
          value: item,
        }));
      } catch (error) {
        console.error("获取模套型号失败:", error);
      }
    },
    async show(data) {
      await this.getMouldSleeveOptions();
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
          singleDoubleMode: this.numberEmpty(data.singleDoubleMode),
          quota: this.numberEmpty(data.quota),
          openMachineClass: data.openMachineClass
            ? data.openMachineClass.split(",")
            : [],
          shellStandard: this.formatShellStandardForForm(data.shellStandard),
          status: data.status || "0",
        };
        console.log(this.form);
      } else {
        this.form = {
          classShift: "2",
          openMachineClass: [],
          factoryCode: "116",
          shellStandard: [],
          status: "1",
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
    // 将后端逗号分隔的模套型号转换为多选组件需要的数组，兼容接口直接返回数组的场景。
    formatShellStandardForForm(value) {
      if (Array.isArray(value)) {
        return value;
      }
      if (this.isEmpty(value)) {
        return [];
      }
      return String(value)
        .split(",")
        .map((item) => item.trim())
        .filter((item) => item);
    },
    // 将多选数组转换为后端需要的逗号分隔字符串，兼容表单中已经是字符串的场景。
    formatShellStandardForSubmit(value) {
      if (Array.isArray(value)) {
        return value.join(",");
      }
      return this.isEmpty(value) ? "" : value;
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
        params.shellStandard = this.formatShellStandardForSubmit(
          params.shellStandard
        );
        Object.keys(params).forEach((key) => {
          if (this.isEmpty(params[key])) {
            params[key] = "";
          }
        });
        this.save(params);
      });
    },
  },
};
</script>
