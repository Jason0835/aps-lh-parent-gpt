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
import { saveType, checkRecipeTypeUnique } from "@/api/setting/type";
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
        recipeTypeCode: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        recipeTypeName: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
      },
      columns: [
        {
          label: this.$t("setting.type.recipeTypeCode"),
          prop: "recipeTypeCode",
          maxlength: "10",
          required: true,
        },
        {
          label: this.$t("setting.type.recipeTypeName"),
          prop: "recipeTypeName",
          maxlength: "50",
          required: true,
        },
        {
          label: this.$t("ui.common.column.remark"),
          prop: "remark",
          type: "textarea",
        },
      ],
    };
  },
  computed: {
    title: function () {
      return this.$t("setting.type.modelName");
    },
  },
  methods: {
    // api
    async save(params) {
      // console.log(params);
      try {
        this.loading = true;

        await this.checkCode();
        await this.checkName();
        const data = await saveType(params);
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
    checkCode(rule, value, callback) {
      return new Promise((resolve, reject) => {
        checkRecipeTypeUnique({
          id: this.form.id,
          recipeTypeCode: this.form.recipeTypeCode,
        })
          .then((res) => {
            if (res == "0") {
              resolve();
            } else {
              this.$modal.msgError(this.$t("setting.type.recipeTypeCode.unique"))
              reject();
            }
          })
          .catch((error) => {
            console.error(error);
            reject(new Error("验证失败，请稍后再试"));
          });
      });
    },
    checkName() {
      return new Promise((resolve, reject) => {
        setTimeout(() => {
          checkRecipeTypeUnique({
            id: this.form.id,
            recipeTypeName: this.form.recipeTypeName,
          })
            .then((res) => {
              if (res == "0") {
                resolve();
              } else {
                this.$modal.msgError(this.$t("setting.type.recipeTypeName.unique"))

                reject();
              }
            })
            .catch((error) => {
              console.error(error);
              reject(new Error("验证失败，请稍后再试"));
            });
        }, 201);
      });
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
