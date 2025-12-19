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
      class="form-item-height"
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
import moment from "moment";

import infoForm from "@/views/components/infoForm.vue";

import { grabGlueDemandPlan } from "@/api/schedule/glueDemandPlan";

export default {
  components: { infoForm },
  data() {
    return {
      loading: false,
      visible: false,
      isEdit: false,
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
      },
    };
  },
  computed: {
    title: function () {
      return this.$t("schedule.glueCollectPlan.modelName");
    },
    columns: function () {
      return [
        {
          label: this.$t("schedule.glueCollectPlan.planDate"),
          prop: "planDate",
          span: 24,
          type: "date",
          valueFormat: "yyyy-MM-dd",
        },
      ];
    },
  },
  methods: {
    // api

    async save(params) {
      try {
        this.loading = true;

        let res = await grabGlueDemandPlan(params);
        this.$modal.msgSuccess(res.msg);
        this.loading = false;
        this.$emit("success");
        this.hide();
      } catch (error) {
        console.error(error);
        this.loading = false;
      }
    },

    //utils
    show() {
      this.visible = true;
      // if (data) {
      //   this.isEdit = true;
      this.form = {
        planDate: moment().add(1, "days").format("yyyy-MM-DD"),
      };
      // }
    },
    hide() {
      this.form = {};
      this.$refs.form.triggerResetForm();
      // this.resetForm("infoForm");
      this.isEdit = false;
      this.visible = false;
    },

    handleConfirm() {
      this.$refs.form.triggerConfirm((params) => {
        this.save(params);
      });
    },
  },
};
</script>
