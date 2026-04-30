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

import { editSpecColor } from "@/api/cx/specColor";

export default {
  components: { infoForm },
  inject: ["parentDict"],
  data() {
    return {
      loading: false,
      visible: false,
      isEdit: false,
      editType: null,
      form: {},
      rules: {
        specDesc: [
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
      return (
        (this.isEdit
          ? this.$t("common.button.edit")
          : this.$t("common.button.add")) +
        this.$t("ui.data.column.cx.setting.modelName")
      );
    },
    columns() {
      return [
        {
          label: this.$t("ui.data.column.specColor.specDesc"),
          prop: "specDesc",
          span: 24,
          required: true,
        },
        {
          label: this.$t("ui.data.column.specColor.colorCode"),
          prop: "colorCode",
          span: 24,
          render: (form) => {
            return (
              <div>
                <el-color-picker v-model={form.colorCode}></el-color-picker>
              </div>
            );
          },
        },
        {
          label: this.$t("ui.data.column.specColor.colorType"),
          prop: "colorType",
          span: 24,
          required: true,
          type: "select", //BIG_ROLL_COLOR
          dictData: this.parentDict.type.BIG_ROLL_COLOR,
        },
        {
          label: this.$t("ui.data.column.status"),
          prop: "status",
          span: 24,
          render: (form) => {
            return (
              <el-switch
                v-model={form.status}
                active-value="0"
                inactive-value="1"
              />
            );
          },
        },
        {
          label: this.$t("ui.common.column.remark"),
          prop: "remark",
          span: 24,
          type: "textarea",
        },
      ];
    },
  },
  methods: {
    // api
    async save(params) {
      try {
        this.loading = true;

        const res = await editSpecColor(params);
        this.$modal.msgSuccess(res.msg);
        this.$emit("success");
        this.hide();

        this.loading = false;
      } catch (error) {
        console.log(error);
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
          colorCode: "#000000",
          colorType: "01",
          status: "0",
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
