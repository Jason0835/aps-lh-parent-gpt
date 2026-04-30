<template>
  <el-dialog
    :title="title"
    :visible="visible"
    width="400px"
    @close="hide"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
  >
    <info-form
      ref="form"
      :form="form"
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
  props: {
    scheduleDate: {
      type: String,
      require: true,
    },
  },
  inject: ["parentDict"],
  data() {
    return {
      loading: false,
      visible: false,
      form: {},
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
                {this.parentDict.type.IS_RELEASE.map((item) => {
                  if (item.value == "1" || item.value == "2") {
                    return <el-radio label={item.value}>{item.label}</el-radio>;
                  }
                })}
              </el-radio-group>
            );
          },
        },
      ],
      // scheduleDate: "",
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
          prefix: "lh/lhScheduleResult",
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
    show() {
      this.visible = true;
      // if (scheduleDate) {
      //   this.scheduleDate = scheduleDate;
      //   this.form = {
      //     ...data,
      //   };
      // }
    },
    hide() {
      // this.form = {};
      // this.scheduleDate = "";
      this.$refs.form.triggerResetForm();
      // this.resetForm("infoForm");
      this.isEdit = false;
      this.visible = false;
    },

    handleConfirm() {
      this.$refs.infoForm.validate((valid) => {
        if (valid) {
          this.save({
            ...this.form,
            scheduleDate: this.scheduleDate,
          });
        }
      });
    },
  },
};
</script>

<style>
</style>
