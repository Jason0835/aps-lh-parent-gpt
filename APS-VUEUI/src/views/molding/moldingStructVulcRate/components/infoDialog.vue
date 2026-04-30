<template>
  <el-dialog :title="title" :visible="visible" width="800px" @close="hide" :close-on-click-modal="false"
    :close-on-press-escape="false" :append-to-body="true">
    <info-form class="form-item-height" ref="form" :form="form" :rules="rules" :columns="columns" label-position="right"
      label-width="160px" v-loading="loading">
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

import {
  saveStructure
} from "@/api/monthplan/mdmStructureLhRatio";
import structureSelect from "@/views/components/structureSelect.vue";

import infoForm from "@/views/components/infoForm.vue";

export default {
  components: { infoForm,structureSelect },
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

      // 检查是否只包含数字
      if (!/^\d+$/.test(strValue)) {
        return callback(
          new Error(this.$t("common.rule.noPoint"))
        );
      }

      // 转换为数字
      const numValue = Number(strValue);
      if (numValue > 99) {
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
        // cxMachineBrandCode: [
        //   {
        //     required: true,
        //     message: this.$t("common.rule.input"),
        //     trigger: "blur",
        //   },
        // ],
        structureName: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        maxEmbryoQty: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
          {
            validator: (rule, value, callback) => {
              validatePositiveInteger({ required: true }, value, callback);
            },
            trigger: ["change"],
          },
        ],
        lhMachineMaxQty: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
          {
            validator: (rule, value, callback) => {
              validatePositiveInteger({ required: true }, value, callback);
            },
            trigger: ["change"],
          },
        ],
        jobType: [
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
      moldingMachines: (state) => state.molding.machines,
    }),
    title: function () {
      return (
        (this.isEdit
          ? this.$t("common.button.edit")
          : this.$t("common.button.add"))
      );
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
          prop: "cxMachineTypeCode",
          label: this.$t("ui.data.column.curingPlan.cxMachineTypeCode"),
          type: "select",
          dictData: this.parentDict.type.cx_machine_type_code,

        },
        {
          prop: "structureName",
          label: this.$t("ui.data.column.finishStock.structureName"),
          render: (form) => {
            return (
              <structureSelect
                key={form.structureName}
                multiple={false}
                v-model={form.structureName}
              />
            );
          },
        },
        {
          prop: "lhMachineMaxQty",
          label: this.$t("ui.data.column.curingPlan.lhMachineMaxQty"),
          type: "number",
          max:99
        },
        {
          prop: "maxEmbryoQty",
          label: this.$t("ui.data.column.curingPlan.maxEmbryoQty"),
          type: "number",
          max:99
        },
        {
          prop: "remark",
          label: this.$t("common.remark"),
        },
      ];
    },
  },
  methods: {
    // api
    async save(params) {
      try {
        this.loading = true;

        const res = await saveStructure(params);
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
      } else {
        this.form = {
          factoryCode: "",
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
