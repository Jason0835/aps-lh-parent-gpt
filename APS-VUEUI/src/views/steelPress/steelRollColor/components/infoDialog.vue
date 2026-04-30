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

import { editSteelRollColor, checkRollCodeUnique } from "@/api/gdyy/gdyySteelRollColor";

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
        bigRollCode: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
      },
      columns: [
        {
          label: this.$t("ui.steelRollColor.column.bigRollCode"),
          prop: "bigRollCode",
          span: 24,
          maxlength: "20",
          required: true,
          // disabled: true,
        },
        {
          label: this.$t("ui.data.column.sidewallCodeColor.colorCode"),
          prop: "colorCode",
          span: 24,
          // required: true,
          type: "select",
          render: (form) => {
            return (
              <div style="display: flex;">
                <el-input
                  style="width:calc(100% - 40px)"
                  v-model={form.colorCode}
                  maxlength="15"
                />
                <el-color-picker v-model={form.colorCode}></el-color-picker>
              </div>
            );
          },
        },
        {
          label: this.$t("ui.data.column.sidewallCodeColor.colorType"),
          prop: "colorType",
          span: 24,
          required: true,
          type: "select",
          render: (form) => {
            return (
              <dict-select
                v-model={form.colorType}
                options={this.parentDict.type.BIG_ROLL_COLOR}
              />
            );
          },
        },
        {
          label: this.$t("ui.data.column.status"),
          prop: "status",
          span: 24,
          required: false,
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
        this.$t("ui.steelRollColor.column.modalName")
      );
    },
  },
  methods: {
    // api
    async save(params) {
      try {
        this.loading = true;

        const res = await editSteelRollColor(params);
        this.$modal.msgSuccess(res.msg);
        this.$emit("success");
        this.hide();

        this.loading = false;
      } catch (error) {
        console.log(error);
        this.loading = false;
      }
    },
    checkRollCodeUnique(rule, value, callback) {
      return new Promise((resolve, reject) => {
        checkRollCodeUnique({
          id: this.form.id,
          bigRollCode: this.form.bigRollCode,
        })
          .then((res) => {
            if (res === 0) {
              resolve();
            } else {
              reject(
                new Error(
                  this.$t("ui.roll.alter.isRollCodeExist")
                )
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
        try {
          this.loading = true;
          await this.checkRollCodeUnique();
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
