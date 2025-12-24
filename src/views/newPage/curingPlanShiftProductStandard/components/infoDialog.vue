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

import { saveCapacity } from "@/api/monthplan/mdmSkuLhCapacity";
import materialCodeSelect from "@/views/components/materialCodeSelect.vue";
import infoForm from "@/views/components/infoForm.vue";

export default {
  components: { infoForm, materialCodeSelect },
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
        return callback(new Error(this.$t("common.rule.noPoint")));
      }

      // 转换为数字
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
      editType: null,
      form: {},
      rules: {
        factoryCode: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
        materialCode: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "change",
          },
        ],
        embryoCode: [
          {
            required: true,
            message: this.$t("common.rule.input"),
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
        standardCapacity: [
          {
            validator: (rule, value, callback) => {
              validatePositiveInteger({ required: false }, value, callback);
            },
            trigger: ["change"],
          },
        ],
        classCapacity: [
          {
            validator: (rule, value, callback) => {
              validatePositiveInteger({ required: false }, value, callback);
            },
            trigger: ["change"],
          },
        ],
        sumVulcanization: [
          {
            validator: (rule, value, callback) => {
              validatePositiveInteger({ required: false }, value, callback);
            },
            trigger: ["change"],
          },
        ],
        vulcanizationTime: [
          {
            validator: (rule, value, callback) => {
              validatePositiveInteger({ required: false }, value, callback);
            },
            trigger: ["change"],
          },
        ],
        mechanicalTime: [
          {
            validator: (rule, value, callback) => {
              validatePositiveInteger({ required: false }, value, callback);
            },
            trigger: ["change"],
          },
        ],
        checkTime: [
          {
            validator: (rule, value, callback) => {
              validatePositiveInteger({ required: false }, value, callback);
            },
            trigger: ["change"],
          },
        ],
        clearTime: [
          {
            validator: (rule, value, callback) => {
              validatePositiveInteger({ required: false }, value, callback);
            },
            trigger: ["change"],
          },
        ],
        dineTime: [
          {
            validator: (rule, value, callback) => {
              validatePositiveInteger({ required: false }, value, callback);
            },
            trigger: ["change"],
          },
        ],
        standardTime: [
          {
            validator: (rule, value, callback) => {
              validatePositiveInteger({ required: false }, value, callback);
            },
            trigger: ["change"],
          },
        ],
        productionTime:[
        {
            validator: (rule, value, callback) => {
              validatePositiveInteger({ required: false }, value, callback);
            },
            trigger: ["change"],
          },
        ]
      },
    };
  },
  computed: {
    ...mapState({
      moldingMachines: (state) => state.molding.machines,
    }),
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
          prop: "materialCode",
          label: this.$t("ui.data.column.monthplan.oriMaterialCode"),
          span: 12,

          render: (form) => {
            return (
              <materialCodeSelect
                key={form.materialCode}
                disabled={this.isEdit}
                v-model={form.materialCode}
                onChange={this.handleMaterialCodeChange}
              />
            );
          },
        },
        {
          prop: "materialDesc",
          label: this.$t("ui.data.column.scheduleAdjust.productCodeDesc"),
          span: 12,
          disabled: true,
        },
        {
          prop: "classCapacity",
          label: this.$t("ui.data.column.curingPlan.classCapacity"),
          span: 12,
          type: "number",
        },
        {
          prop: "standardCapacity",
          label: this.$t("ui.data.column.curingPlan.standardCapacity"),
          span: 12,
          type: "number",
          required: true,
          min: 0,
          max: 99999999,
        },
        {
          prop: "sumVulcanization",
          label: this.$t("ui.data.column.curingPlan.sumVulcanization"),
          span: 12,
          type: "number",
          required: true,
          min: 0,
          max: 99999999,
        },
        {
          prop: "vulcanizationTime",
          label: this.$t("ui.data.column.curingPlan.vulcanizationTime"),
          span: 12,
          type: "number",
          required: true,
          min: 0,
          max: 99999999,
        },
        {
          prop: "mechanicalTime",
          label: this.$t("ui.data.column.curingPlan.mechanicalTime"),
          type: "number",
          required: true,
          min: 0,
          max: 99999999,
          // children: [
          //   {
          //     prop: "金宇越南",
          //     label: this.$t("金宇越南"),
          //     span: 12,
          //   },
          // ],
        },
        {
          label: this.$t("ui.data.column.curingPlan.nostandardTime"),
          type: "title",
        },

        {
          prop: "checkTime",
          label: this.$t("ui.data.column.curingPlan.checkTime"),
          span: 12,
          type: "number",
          required: true,
          min: 0,
          max: 99999999,
        },
        {
          prop: "clearTime",
          label: this.$t("ui.data.column.curingPlan.clearTime"),
          span: 12,
          type: "number",
          required: true,
          min: 0,
          max: 99999999,
        },
        {
          prop: "dineTime",
          label: this.$t("ui.data.column.curingPlan.dineTime"),
          span: 12,
          type: "number",
          required: true,
          min: 0,
          max: 99999999,
        },

        {
          prop: "standardTime",
          label: this.$t("ui.data.column.curingPlan.standardTime"),
          span: 12,
          type: "number",
          required: true,
          min: 0,
          max: 99999999,
        },
        {
          prop: "productionTime",
          label: this.$t("ui.data.column.curingPlan.productionTime"),
          span: 12,
          type: "number",
          required: true,
          min: 0,
          max: 99999999,
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

        const res = await saveCapacity(params);
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
          factoryCode: "116",
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
    handleMaterialCodeChange(val, row) {
      if (val) {
        this.$set(this.form, "materialDesc", row.materialDesc);
      } else {
        this.$set(this.form, "materialDesc", "");
      }
    },
  },
};
</script>
