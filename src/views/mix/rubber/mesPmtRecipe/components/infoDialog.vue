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
import { saveMachineGlueDecompose, selectMesPmtRecipeMachine } from "@/api/setting/machineGlueDecompose";
export default {
  components: { infoForm },
  data() {
    return {
      loading: false,
      visible: false,
      isEdit: false,
      form: {
        classShift: "2",
      },
      rules: {
        machineCode: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        planDate: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        tireRoughStock: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
      },
      columns: [
        {
          label: this.$t("setting.machineGlueDecompose.mixArea"),
          prop: "mixArea",
          maxlength: "20",
          required: true,
          type: "select",
        },
        {
          label: this.$t("setting.machineGlueDecompose.glue"),
          prop: "glue",
          maxlength: "30",
          required: true,
        },
        {
          label: this.$t("setting.machineGlueDecompose.machineName"),
          prop: "machineCode",
          maxlength: "30",
          required: true,
          type: "select",
        },

        {
          label: this.$t("setting.machineGlueDecompose.motherGlue1"),
          prop: "motherGlue1",
          maxlength: "30",
        },
        {
          label: this.$t("setting.machineGlueDecompose.motherGlue2"),
          prop: "motherGlue2",
          maxlength: "30",
        },
        {
          label: this.$t("setting.machineGlueDecompose.motherGlue3"),
          prop: "motherGlue3",
          maxlength: "30",
        },
        {
          label: this.$t("setting.machineGlueDecompose.motherGlue5"),
          prop: "motherGlue5",
          maxlength: "30",
        },
        {
          label: this.$t("setting.machineGlueDecompose.motherGlue6"),
          prop: "motherGlue6",
          maxlength: "30",
        },
        {
          label: this.$t("setting.machineGlueDecompose.motherGlue7"),
          prop: "motherGlue7",
          maxlength: "30",
        },
        {
          label: this.$t("setting.machineGlueDecompose.motherGlue8"),
          prop: "motherGlue8",
          maxlength: "30",
        },
        {
          label: this.$t("setting.machineGlueDecompose.motherGlue9"),
          prop: "motherGlue9",
          maxlength: "30",
        },
        {
          label: this.$t("ui.common.column.remark"),
          prop: "remark",
        },
      ],
    };
  },
  computed: {
    title: function () {
      return this.$t("setting.machineGlueDecompose.modelName");
    },
  },
  methods: {
    // api
    async save(params) {
      // console.log(params);
      try {
        this.loading = true;
        const data = await saveMachineGlueDecompose(params);
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
          classShift: "2",
        };
      }
    },
    hide() {
      this.form = { classShift: "2" };
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
