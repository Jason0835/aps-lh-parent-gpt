<template>
  <el-dialog
    :title="title"
    :visible="visible"
    width="300px"
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
import { changeExecute } from "@/api/lh/lhApsMoldAdjustPlan";
export default {
  components: { infoForm },
  data() {
    return {
      loading: false,
      visible: false,
      defaultValue: {},
      rules: {
        isExecute: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "blur",
          },
        ],
      },
      columns: [
        {
          label: this.$t("ui.data.column.lhApsMoldAdjustPlan.isExecute"),
          prop: "isExecute",
          render: (form) => {
            return (
              <el-radio-group v-model={form.isExecute}>
                <el-radio label="1">是</el-radio>
                <el-radio label="0">否</el-radio>
              </el-radio-group>
            );
          },
        },
      ],
      ids:"",
    };
  },
  computed: {
    title: function () {
      return this.$t("APS模具变动单更改是否执行");
    },
  },
  methods: {
    async save(params) {
      // console.log(params);
      try {
        this.loading = true;
        const data = await changeExecute({ ids: this.ids, ...params });
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
    show(ids) {
      this.visible = true;
      if (ids) {
        this.ids = ids;
        // this.defaultValue = {
        //   ...data,
        // };
      }
    },
    hide() {
      // this.defaultValue = {};
      this.ids = "";
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
