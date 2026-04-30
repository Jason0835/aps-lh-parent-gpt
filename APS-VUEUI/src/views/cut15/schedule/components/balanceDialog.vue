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

import { balance } from "@/api/cd15/scheduleResult.js";

export default {
  components: { infoForm },
  data() {
    const iniDate = moment().add(1, "days").format("yyyy-MM-DD");
    return {
      loading: false,
      visible: false,
      isEdit: false,
      editType: null,
      initDate: iniDate,
      form: {
        scheduleDate: iniDate,
      },
      rules: {},
    };
  },
  computed: {
    title: function () {
      return this.$t("ui.data.column.scheduleResult.balance");
    },
    columns () {
      return [
        {
          label: this.$t("ui.data.column.scheduleResult.scheduleDate"),
          prop: "scheduleDate",
          span: 24,
          type: "date",
          valueFormat: "yyyy-MM-dd",
          pickerOptions: {
            disabledDate:(time) => {
              return moment(time).isBefore(this.initDate, "day");
            },
          },
        },
      ];
    },
  },
  methods: {
    // api
    async save(params) {
      try {
        this.loading = true;
        let res = await balance(params);
        this.$modal.msgSuccess(
          this.$t("common.msg.ajax.operation.success")
        );
        this.loading = false;
        this.$emit("success");
        this.hide();
      } catch (error) {
        console.error(error);
        this.loading = false;
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

    handleConfirm() {
      this.$refs.form.triggerConfirm(this.save);
    },
  },
};
</script>
