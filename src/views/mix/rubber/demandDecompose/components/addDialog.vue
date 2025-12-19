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

import { saveGlueDecomposePlan } from "@/api/schedule/glueDecomposePlan";
import { getFormulaMachineList } from "@/api/setting/formulaMachine";

export default {
  components: { infoForm },
  inject: ["parentDict"],
  data() {
    return {
      loading: false,
      visible: false,
      isEdit: false,
      editType: null,
      form: {
        planDate: moment().add(1, "days").format("yyyy-MM-DD"),
      },
      rules: {
        planDate: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "blur",
          },
        ],
        mixArea: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "blur",
          },
        ],
        glue: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        planQty: [
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
      machines: [],
    };
  },
  computed: {
    title: function () {
      return this.$t("schedule.glueDecomposePlan.modelName");
    },
    columns() {
      return [
        // {
        //   label: this.$t("ui.baseInfo"),
        //   type: "title",
        // },
        {
          label: this.$t("schedule.glueDecomposePlan.planDate"),
          prop: "planDate",
          span: 24,
          type: "date",
          valueFormat: "yyyy-MM-dd",
        },
        {
          label: this.$t("schedule.glueDecomposePlan.mixArea"),
          prop: "mixArea",
          span: 24,
          type: "select", //  MIX_AREA
          dictData: this.parentDict.type.MIX_AREA,
          listeners: {
            change: () => {
                this.getMachines();
            }
          }
        },

        {
          label: this.$t("schedule.glueDecomposePlan.glue"),
          prop: "glue",
          span: 24,
          maxlength: "30",
          listeners: {
            change: () => {
                this.getMachines();
            }
          }
        },

        {
          label: this.$t("schedule.glueDecomposePlan.planQty"),
          prop: "planQty",
          span: 24,
          type: "number",
        },
        {
          label: this.$t("setting.machine.machineName"),
          prop: "machineCode",
          span: 24,
          type: "select",
          required: true,
          dictData: this.machines,
          valueKey: "machineCode",
          labelKey: "machineName",
        },
        {
          label: this.$t("ui.common.column.remark"),
          prop: "remark",
          span: 24,
          type: "textarea",
          maxlength: "300",
        },
      ];
    },
  },
  watch: {
    // "form.mixArea": function (val) {
    //   // this.form.machineCode = "";
    //   this.getMachines();
    // },
    // "form.glue": function (val) {
    //   // this.form.machineCode = "";
    //   // this.getMachines();
    // },
  },
  methods: {
    // api
    async save(params) {
      try {
        this.loading = true;
        let result = await saveGlueDecomposePlan(params);
        this.$emit("success");
        this.hide();
        this.loading = false;
      } catch (error) {
        console.error(error);
        this.loading = false;
      }
    },
    async getMachines() {
      this.$set(this.form,"machineCode", "");

      if(!this.form.mixArea || !this.form.glue) {
        this.machines = [];
        return;
      }

      try {
        const res = await getFormulaMachineList({
          mixArea: this.form.mixArea,
          glue: this.form.glue,
        });
        this.machines = res;
        console.log(res);
        this.$forceUpdate();
      } catch (error) {
        console.error(error);
      }
    },

    //utils
    show(data, editType) {
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
    toUpperCase(e) {
      let value = e.target.value;
      if (value.length) {
        this.form.treadCode = value.toUpperCase();
      }
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
