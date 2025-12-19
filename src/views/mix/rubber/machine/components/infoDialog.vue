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
import { saveMachine } from "@/api/setting/machine";
export default {
  components: { infoForm },
  inject: ['parentDict'],
  data() {
    return {
      loading: false,
      visible: false,
      isEdit: false,
      form: {
      },
      rules: {
        machineCode: [
          {
            required: true,
            message: this.$t("common.rule.input"),
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
      return this.$t("setting.machine.modelName");
    },
    columns() {return [
        {
          label: this.$t("setting.machineGlueDecompose.mixArea"),
          prop: "mixArea",
          maxlength: "20",
          required: true,
          type: "select",
          dictData: this.parentDict.type.MIX_AREA
        },
        {
          label: this.$t("setting.machine.machineCode"),
          prop: "machineCode",
          maxlength: "30",
          required: true,
        },
        {
          label: this.$t("setting.machine.machineName"),
          prop: "machineName",
          maxlength: "40",
          required: true,
        },

        // {
        //   label: this.$t("setting.machine.haveSulfurSteelyard"),
        //   prop: "haveSulfurSteelyard",
        //   maxlength: "30",
        //   type: "switch",
        // },
        // {
        //   label: this.$t("setting.machine.haveMaterialSteelyard"),
        //   prop: "haveMaterialSteelyard",
        //   maxlength: "30",
        //   type: "switch",
        // },
        {
          label: this.$t("setting.machine.status"),
          prop: "status",
          maxlength: "30",
          type: "switch",
        },
        {
          label: this.$t("setting.machine.midStatus"),
          prop: "midStatus",
          maxlength: "30",
          type: "switch",
        },
        {
          label: this.$t("setting.machine.nightStatus"),
          prop: "nightStatus",
          maxlength: "30",
          type: "switch",
        },
        // {
        //   label: this.$t("setting.machine.dayStatus"),
        //   prop: "dayStatus",
        //   maxlength: "30",
        //   type: "switch",
        // },
        {
          label: this.$t("ui.common.column.remark"),
          prop: "remark",
          type: "textarea"
        },
      ]},
  },
  methods: {
    // api
    async save(params) {
      // console.log(params);
      try {
        this.loading = true;
        const data = await saveMachine(params);
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
      } else {
        this.form = {
        };
      }
    },
    hide() {
      this.form = {  };
      this.$refs.form.triggerResetForm();
      // this.resetForm("infoForm");
      this.isEdit = false;
      this.visible = false;
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
