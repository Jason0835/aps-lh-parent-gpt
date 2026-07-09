<template>
  <el-dialog
    :append-to-body="true"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :title="title"
    :visible="visible"
    width="800px"
    @close="hide"
  >
    <info-form
      ref="form"
      v-loading="loading"
      :columns="columns"
      :form="form"
      :rules="rules"
      class="form-item-height"
      label-position="right"
      label-width="160px"
    >
    </info-form>
    <template slot="footer">
      <el-button @click="hide">{{ this.$t("common.button.cancel") }}</el-button>
      <el-button :loading="loading" type="primary" @click="handleConfirm">{{
        this.$t("common.button.confirm")
      }}</el-button>
    </template>
  </el-dialog>
</template>

<script>
import infoForm from "@/views/components/infoForm.vue";
import {saveGlueMachineReal} from "@/api/tc/glueMachineReal";

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
        factoryCode: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
        glueCode: [
          {
            required: true,
            message: this.$t("common.rule.input"),
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
      },
    }
  },
  computed: {
    machines() {
      return this.$store.state.tc.machines;
    },
    title: function () {
      return (
        (this.isEdit
          ? this.$t("common.button.edit")
          : this.$t("common.button.add")) +
        this.$t("ui.data.column.tcGlueMachineReal.modelName")
      );
    },
    columns() {
      return [
        {
          prop: "factoryCode",
          label: this.$t("ui.data.column.tcGlueMachineReal.factoryCode"),
          type: "select",
          span: 12,
          required: true,
          disabled: true,
          dictData: this.parentDict.type.biz_factory_name,
        },
        {
          prop: "glueCode",
          label: this.$t("ui.data.column.tcGlueMachineReal.glueCode"),
          span: 12,
          maxlength: 20,
          required: true,
          disabled: this.isEdit,
        },
        // {
        //   prop: "baseGlueCode",
        //   label: this.$t("ui.data.column.tcGlueMachineReal.baseGlueCode"),
        //   span: 12,
        //   maxlength: 60,
        // },
        {
          prop: "machineCode",
          label: this.$t("ui.data.column.tcGlueMachineReal.machineCode"),
          span: 12,
          required: true,
          disabled: this.isEdit,
          type: "select",
          dictData: this.machines,
          props: { label: "machineCode", value: "machineCode" },
          filterable: true,
          listeners: {
            change: (value) => {
              if (value) {
                const machine = this.machines.find(m => m.machineCode === value);
                if (machine) {
                  this.form.factoryCode = machine.factoryCode;
                }
              }
            },
          },
        },
        {
          prop: "shiftCode",
          label: this.$t("ui.data.column.tcGlueMachineReal.shiftCode"),
          type: "select",
          span: 12,
          dictData: this.parentDict.type.class_num_three_plan,
        },
        {
          prop: "priority",
          label: this.$t("ui.data.column.tcGlueMachineReal.priority"),
          span: 12,
          type: "number",
        },
        {
          prop: "allowFlag",
          label: this.$t("ui.data.column.tcGlueMachineReal.allowFlag"),
          type: "switch",
          span: 12,
          activeValue: "1",
          inactiveValue: "0",
        },
        {
          prop: "enableStatus",
          label: this.$t("ui.data.column.tcGlueMachineReal.enableStatus"),
          type: "switch",
          span: 12,
          activeValue: "1",
          inactiveValue: "0",
        },
        {
          prop: "remark",
          label: this.$t("ui.common.column.remark"),
          span: 24,
          type: "textarea",
          maxlength: 900,
        },
      ];
    },
  },
  methods: {
    async save(params) {
      try {
        this.loading = true;
        const res = await saveGlueMachineReal(params);
        this.$modal.msgSuccess(res.msg);
        this.$emit("success");
        this.hide();
        this.loading = false;
      } catch (error) {
        console.log(error);
        this.loading = false;
      }
    },

    show(data) {
      this.visible = true;
      if (data) {
        this.isEdit = true;
        this.form = {
          ...data,
          enableStatus: data.enableStatus || "0",
          allowFlag: data.allowFlag || "0",
        };
      } else {
        this.form = {
          factoryCode: "116",
          enableStatus: "1",
          allowFlag: "1",
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
