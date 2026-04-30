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

import { numberEmpty } from "@/utils/index";

import infoForm from "@/views/components/infoForm.vue";

import { editBigRoll, checkBigRollCodeUnique } from "@/api/cd15/bigRoll";

export default {
  components: { infoForm },
  data() {
    return {
      loading: false,
      visible: false,
      isEdit: false,
      editType: null,
      form: {},
      rules: {
        bigRollCode: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        actClothLength: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
      },
      columns: [
        {
          label: this.$t("ui.common.column.gy.bigRollCode"),
          prop: "bigRollCode",
          span: 24,
          maxlength: "30",
          required: true,
          // disabled: true,
        },
        {
          label: this.$t("ui.bigRoll.column.actClothLength"),
          prop: "actClothLength",
          span: 24,
          type: "number",
          min: 0,
          max: 999999,
          precision: 0,
        },
        {
          label: this.$t("ui.bigRoll.column.convertProduceNum"),
          prop: "convertProduceNum",
          span: 24,
          type: "number",
          min: 0,
          max: 999999,
          precision: 0,
        },
        {
          label: this.$t("ui.bigRoll.column.clothLength"),
          prop: "clothLength",
          span: 24,
          type: "number",
          min: 0,
          max: 999999,
          precision: 0,
        },
        {
          label: this.$t("ui.common.column.remark"),
          prop: "remark",
          span: 24,
          type: "textarea",
          maxlength: "300",
        },
      ],
    };
  },
  computed: {
    title: function () {
      return (
        (this.isEdit
          ? this.$t("common.button.edit")
          : this.$t("common.button.add")) +
        this.$t("ui.cd15.bigRoll.column.modalName")
      );
    },
  },
  methods: {
    // api
    async save(params) {
      try {
        this.loading = true;

        const res = await editBigRoll(params);
        this.$modal.msgSuccess(res.msg);
        this.$emit("success");
        this.hide();

        this.loading = false;
      } catch (error) {
        console.log(error);
        this.loading = false;
      }
    },

    checkBigRollCode(rule, value, callback) {
      return new Promise((resolve, reject) => {
        checkBigRollCodeUnique({
          id: this.form.id,
          bigRollCode: this.form.bigRollCode,
        })
          .then((res) => {
            if (res === 0) {
              resolve();
            } else {
              reject(
                new Error(this.$t("ui.cd15.bigRoll.alter.isBigRollExist"))
              );
            }
          })
          .catch((error) => {
            console.error(error);
            reject(new Error("验证失败，请稍后再试"));
          });
      });
    },

    //utils
    show(data) {
      this.visible = true;
      if (data) {
        this.isEdit = true;
        this.form = {
          ...data,
          actClothLength: numberEmpty(data.actClothLength),
          convertProduceNum: numberEmpty(data.convertProduceNum),
          clothLength: numberEmpty(data.clothLength),
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
      this.$refs.form.triggerConfirm(async (params) => {
        Object.keys(params).forEach((key) => {
          if (this.isEmpty(params[key])) {
            params[key] = "";
          }
        });
        try {
          this.loading = true;
          await this.checkBigRollCode();
          this.save(params);
        } catch (error) {
          this.$modal.msgError(error.message);
          this.loading = false;
        }
      });
    },
  },
};
</script>
