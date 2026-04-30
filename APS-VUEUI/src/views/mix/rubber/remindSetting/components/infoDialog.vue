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
import {saveRemindSetting} from "@/api/setting/remindSetting";

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
      },
      rules: {
        fieldCode: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        fieldValue: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
      },
      columns: [
        {
          label: this.$t("setting.remindSetting.fieldCode"),
          prop: "fieldCode",
          maxlength: "20",
          required: true,
          type: "select",
          dictData: this.parentDict.type.REMIND_CODE,
        },
        {
          label: this.$t("setting.remindSetting.fieldValue"),
          prop: "fieldValue",
          maxlength: "40",
          required: true,
        },
        {
          label: this.$t("setting.remindSetting.wordColor"),
          prop: "wordColor",
          maxlength: "15",
          required: true,
          type: "select",
          render: (form) => {
            return (
              <div style="display: flex;">
                {/* <el-input style="width:calc(100% - 40px)" v-model={form.wordColor} maxlength="15"/> */}
                <el-color-picker v-model={form.wordColor}></el-color-picker>
              </div>
            );
          },
        },
        {
          label: this.$t("setting.remindSetting.backgroundColor"),
          prop: "backgroundColor",
          maxlength: "15",
          required: true,
          type: "select",
          render: (form) => {
            return (
              <div style="display: flex;">
                {/* <el-input style="width:calc(100% - 40px)" v-model={form.backgroundColor} maxlength="15"/> */}
                <el-color-picker v-model={form.backgroundColor}></el-color-picker>
              </div>
            );
          },
        },
        {
          label: this.$t("setting.remindSetting.tips"),
          prop: "tips",
          maxlength: "300",
          required: true,
        },
        {
          label: this.$t("ui.common.column.remark"),
          prop: "remark",
          type: "textarea",
          maxlength: "300",
        },
      ],
    };
  },
  computed: {
    title: function () {
      return this.$t("setting.remindSetting.modelName");
    },
  },
  methods: {
    // api
    async save(params) {
      // console.log(params);
      try {
        this.loading = true;
        const data = await saveRemindSetting(params);
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
