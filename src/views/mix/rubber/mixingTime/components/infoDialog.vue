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
import { saveMixingTime } from "@/api/setting/mixingTime";
import { selectMesPmtRecipeMachine } from "@/api/setting/MesPmtRecipe";

export default {
  components: { infoForm },
  inject: ["parentDict"],
  data() {
    return {
      loading: false,
      visible: false,
      isEdit: false,
      form: {},
      machines: [],
      rules: {
        machineCode: [
          {
            required: true,
            message: this.$t("common.rule.input"),
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
        mixArea: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        intervalTime: [
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
      return this.$t("setting.mixingTime.modelName");
    },
    columns() {
      return [
        {
          label: this.$t("setting.mixingTime.mixArea"),
          prop: "mixArea",
          maxlength: "20",
          required: true,
          type: "select",
          dictData: this.parentDict.type.MIX_AREA,
          listeners: {
            change: this.getMachines,
          },
        },
        {
          label: this.$t("setting.mixingTime.glue"),
          prop: "glue",
          maxlength: "30",
          required: true,
          listeners: {
            change: this.getMachines,
          },
        },
        {
          label: this.$t("setting.mixingTime.machineName"),
          prop: "machineCode",
          maxlength: "30",
          required: true,
          type: "select",
          dictData: this.machines,
          labelKey: "machineName",
          valueKey: "recipeEquipCode",
          listeners: {
            blur: this.handleMachineChange,
          },
        },
        {
          label: this.$t("setting.mixingTime.intervalTime"),
          prop: "intervalTime",
          // maxlength: "40",
          required: true,
          type: "number",
          min: 0,
          max: 999999999,
        },
        {
          label: this.$t("ui.common.column.remark"),
          prop: "remark",
          type: "textarea",
          maxlength: "300",
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
        const data = await saveMixingTime(params);
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
        };
        this.machines = [{
          recipeEquipCode: data.machineCode,
          machineName: data.machineName
        }]
        this.getMesPmtRecipeMachineList();
      } else {
      }
    },
    hide() {
      this.form = {};
      this.$nextTick(() => {
        this.machines = [];

      })
      this.$refs.form.triggerResetForm();
      // this.resetForm("infoForm");
      this.isEdit = false;
      this.visible = false;
    },
    async getMachines() {
      console.log("getMachines")
      if (!this.form.mixArea || !this.form.glue) {
        if (this.form.machineCode) {
          this.$set(this.form, "machineCode", "");
          this.$set(this.form, "machineName", "");
        }
        if (this.machines.length) {
          this.machines = [];

        }
        return;
      }

      await this.getMesPmtRecipeMachineList()

    },
    async getMesPmtRecipeMachineList(){
      try {
        const res = await selectMesPmtRecipeMachine({
          mixArea: this.form.mixArea,
          recipeMaterialName: this.form.glue,
        });

        this.machines = res;
      } catch (error) {
        console.error(error);
      }
    },

    handleMachineChange(val) {
      const find = this.machines.find((item) => {
        return item.recipeEquipCode === val;
      });
      if (find) {
        this.form.machineName = find.machineName;
      }
    },

    handleConfirm() {
      this.$refs.form.triggerConfirm((params) => {
        this.save(params)
      });
    },
  },
};
</script>
