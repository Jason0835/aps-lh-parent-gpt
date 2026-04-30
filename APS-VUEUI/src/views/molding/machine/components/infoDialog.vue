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

import {
  // listMdmMoldingMachine,
  editMdmMoldingMachine,
  // removeMdmMoldingMachine,
} from "@/api/maindata/mdmMoldingMachine";
// import { editMachine, checkMachineCodeUnique } from "@/api/cx/machine";

export default {
  components: { infoForm },
  inject: ["parentDict"],
  props: {
    moldingMachineClass: Array,
  },
  data() {
    return {
      loading: false,
      visible: false,
      isEdit: false,
      editType: null,
      form: {},
      rules: {
        factoryCode: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "blur",
          },
        ],
        productTypeCode: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "blur",
          },
        ],
        moldingMachineCode: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        moldingMachineClassId: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "blur",
          },
        ],
        minSize: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        maxSize: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        moldingDrumMax: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        moldingDrumMin: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        sectionWidthMax: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        sectionWidthMin: [
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
          label: this.$t("ui.data.column.machine.productTypeCode"),
          prop: "productTypeCode",
          type: "select",
          dictData: this.parentDict.type.biz_product_name,
          disabled: this.isEdit,
        },
        {
          label: this.$t("ui.data.column.cx.machine.machineCode"),
          prop: "moldingMachineCode",
          span: 24,
          required: true,
          disabled: this.isEdit,
        },
        // {
        //   label: this.$t("ui.data.column.cx.machine.machineName"),
        //   prop: "machineName",
        //   span: 24,
        //   required: true,
        // },
        {
          label: this.$t("ui.data.column.machine.machineType"),
          prop: "moldingMachineClassId",
          span: 24,
          type: "select", //CX_MACHINE_TYPE
          dictData: this.moldingMachineClass,
          valueKey: "id",
          labelKey: "moldingMachineClassName",
          listeners: {
            change: this.handleMachineClassChange,
          },
        },
        // {
        //   prop: "moldingDrum",
        //   label: this.$t("ui.data.column.cx.machine.moldingDrum"),
        //   // sortable: "custom",
        // },
        {
          prop: "moldingDrumMax",
          label: this.$t("ui.data.column.cx.machine.moldingDrumMax"),
          type: "number",
          min: 1,
          max: 999,
          precision: 0,
        },
        {
          prop: "moldingDrumMin",
          label: this.$t("ui.data.column.cx.machine.moldingDrumMin"),
          type: "number",
          min: 1,
          max: 999,
          precision: 0,
        },
        {
          prop: "sectionWidthMax",
          label: this.$t("ui.data.column.cx.machine.sectionWidthMax"),
          type: "number",
          min: 1,
          max: 999,
          precision: 0,
        },
        {
          prop: "sectionWidthMin",
          label: this.$t("ui.data.column.cx.machine.sectionWidthMin"),
          type: "number",
          min: 1,
          max: 999,
          precision: 0,
        },
        // {
        //   label: this.$t("ui.data.column.cx.machine.type"),
        //   prop: "machineType",
        //   span: 24,
        //   type: "select", //MACHINE_TYPE
        //   dictData: this.parentDict.type.MACHINE_TYPE,
        // },
        {
          label: this.$t("ui.data.column.cx.machine.dimensionMiniMum"),
          prop: "minSize",
          span: 24,
          type: "number",
          min: 1,
          max: 99,
          precision: 0,
        },
        {
          label: this.$t("ui.data.column.cx.machine.dimensionMaxiMum"),
          prop: "maxSize",
          span: 24,
          type: "number",
          min: 1,
          max: 99,
          precision: 0,
        },
        // {
        //   label: this.$t("ui.data.column.cx.machine.operatorQty"),
        //   prop: "operatorQty",
        //   span: 24,
        //   type: "number",
        //   min: 0,
        //   max: 999,
        //   precision: 0,
        // },
        // {
        //   label: this.$t("ui.data.column.cx.machine.quata"),
        //   prop: "quata",
        //   span: 24,
        //   required: true,
        //   type: "number",
        //   min: 0,
        //   max: 9999999,
        //   precision: 0,
        // },
        // {
        //   label: this.$t("ui.data.column.cx.machine.quotaRatio"),
        //   prop: "quotaRatio",
        //   span: 24,
        //   type: "number",
        //   min: 0.01,
        //   max: 9999.99,
        //   precision: 2,
        // },
        {
          label: this.$t("ui.data.column.cx.machine.classShift"),
          prop: "classes",
          span: 24,
          render: (form) => {
            return (
              <el-radio-group v-model={form.classes}>
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
          prop: "carcassClothType",
          align: "center",
          label: this.$t("ui.data.column.cx.machine.carcassClothType"),
          type: "select",
          dictData: this.parentDict.type.biz_carcassCloth_type,
        },
        {
          label: this.$t("ui.data.column.machine.status"),
          prop: "machineStatus",
          span: 24,
          type: "switch",
          activeValue: "0",
          inactiveValue: "1",
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

        const res = await editMdmMoldingMachine(params);
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
          classes: data.classes + "",
          maxSize: this.numberEmpty(data.maxSize),
          minSize: this.numberEmpty(data.minSize),
          // operatorQty: this.numberEmpty(data.operatorQty),
          // dimensionMaxiMum: this.numberEmpty(data.dimensionMaxiMum),
          // dimensionMiniMum: this.numberEmpty(data.dimensionMiniMum),
        };
      } else {
        this.form = {
          classes: "2",
          machineStatus: "0",
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
        if (
          Number(params.minSize) > Number(params.maxSize) &&
          !this.isEmpty(params.maxSize)
        ) {
          this.$modal.msgError(
            this.$t(
              "ui.data.column.machine.dimensionMaximumBigThanDimensionMinmum"
            )
          );
          return;
        }
        if (
          Number(params.moldingDrumMin) > Number(params.moldingDrumMax) &&
          !this.isEmpty(params.moldingDrumMax)
        ) {
          this.$modal.msgError(
            this.$t(
              "ui.data.column.machine.moldingDrumMaximumBigThanMoldingDrumMinimum"
            )
          );
          return;
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
    handleMachineClassChange(val) {
      const result = this.moldingMachineClass.find((row) => row.id == val);
      if (result) {
        this.form.moldingMachineClsName = result.moldingMachineClassName;
      }
    },
  },
};
</script>
