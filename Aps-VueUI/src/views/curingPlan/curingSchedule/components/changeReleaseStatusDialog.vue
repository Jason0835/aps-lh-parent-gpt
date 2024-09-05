<template>
  <el-dialog
    :title="title"
    :visible="visible"
    width="350px"
    @close="hide"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
  >
    <info-form
      ref="form"
      :defaultValue="defaultValue"
      :rules="rules"
      :columns="columns"
      label-position="right"
      label-width="80px"
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
import { changeReleaseStatus } from "@/api/lh/scheduleResult";
export default {
  components: { infoForm },
  data() {
    return {
      loading: false,
      visible: false,
      defaultValue: {},
      rules: {
        isRelease: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "blur",
          },
        ],
      },
      columns: [
        {
          label: this.$t("ui.data.column.scheduleResult.isRelease"),
          prop: "isRelease",
          render: (form) => {
            return (
              <el-radio-group v-model={form.isRelease}>
                <el-radio label="1">已发布</el-radio>
                <el-radio label="2">发布失败</el-radio>
              </el-radio-group>
            );
          },
        },
      ],
      scheduleDate: "",
    };
  },
  computed: {
    title: function () {
      return this.$t("ui.data.column.lh.scheduleResult.modelName");
    },
  },
  methods: {
    async save(params) {
      // console.log(params);
      try {
        this.loading = true;
        const data = await changeReleaseStatus({
          scheduleDate: this.scheduleDate,
          prefix: "lh/scheduleResult",
          ...params,
        });
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
    show(scheduleDate) {
      this.visible = true;
      if (scheduleDate) {
        this.scheduleDate = scheduleDate;
        // this.defaultValue = {
        //   ...data,
        // };
      }
    },
    hide() {
      // this.defaultValue = {};
      this.scheduleDate = "";
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

<style>
</style>
