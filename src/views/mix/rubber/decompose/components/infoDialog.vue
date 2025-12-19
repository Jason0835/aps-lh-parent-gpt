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
import {
  checkGlueDecomposeUnique,
  checkComplete,
  saveDecompose,
} from "@/api/setting/decompose";
export default {
  components: { infoForm },
  inject: ["parentDict"],
  data() {
    return {
      loading: false,
      visible: false,
      isEdit: false,

      form: {},
      rules: {
        glue: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        segment: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
      },
      columns: [
        {
          label: this.$t("setting.decompose.glue"),
          prop: "glue",
          maxlength: "30",
          required: true,
          listeners: {
            blur: this.autoCreateMotherGlue,
          },
        },
        {
          label: this.$t("setting.decompose.segment"),
          prop: "segment",
          maxlength: "20",
          required: true,
          type: "select",
          dictData: this.parentDict.type.SEGMENT,
          listeners: {
            change: this.autoCreateMotherGlue,
          },
        },
        {
          label: this.$t("setting.decompose.motherGlue1"),
          prop: "motherGlue1",
          maxlength: "30",
        },
        {
          label: this.$t("setting.decompose.motherGlue2"),
          prop: "motherGlue2",
          maxlength: "30",
        },
        {
          label: this.$t("setting.decompose.motherGlue3"),
          prop: "motherGlue3",
          maxlength: "30",
        },
        {
          label: this.$t("setting.decompose.motherGlue5"),
          prop: "motherGlue5",
          maxlength: "30",
        },
        {
          label: this.$t("setting.decompose.motherGlue6"),
          prop: "motherGlue6",
          maxlength: "30",
        },
        {
          label: this.$t("setting.decompose.motherGlue7"),
          prop: "motherGlue7",
          maxlength: "30",
        },
        {
          label: this.$t("setting.decompose.motherGlue8"),
          prop: "motherGlue8",
          maxlength: "30",
        },
        {
          label: this.$t("setting.decompose.motherGlue9"),
          prop: "motherGlue9",
          maxlength: "30",
        },
        {
          label: this.$t("ui.common.column.remark"),
          prop: "remark",
        },
      ],
    };
  },
  computed: {
    title: function () {
      return this.$t("setting.decompose.modelName");
    },
  },
  methods: {
    // api
    async save(params) {
      // console.log(params);
      try {
        this.loading = true;
        await this.checkGlueDecomposeUnique();
        await this.checkComplete();

        const data = await saveDecompose(params);
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
        };
      }
    },
    hide() {
      this.$refs.form.triggerResetForm();
      // this.resetForm("infoForm");
      this.isEdit = false;
      this.visible = false;
    },
    autoCreateMotherGlue() {
      let glue = this.form.glue;
      //获取段数
      let segment = this.form.segment;
      if (
        glue == null ||
        glue == "" ||
        glue == undefined ||
        segment == null ||
        segment == undefined
      ) {
        return;
      }
      let step = parseInt(segment);
      let max = step + 1;
      let form = {
        id: this.form.id,
        glue,
        segment,
      };

      let domPrefix = "motherGlue";
      for (var i = 1; i < max; i++) {
        if (i == segment) {
          break;
        }
        let motherGlue = glue + "/" + segment + i;
        form[domPrefix + i] = motherGlue;
      }
      this.form = form;
    },

    checkGlueDecomposeUnique() {
      return new Promise((resolve, reject) => {
        checkGlueDecomposeUnique({
          id: this.form.id,
          glue: this.form.glue,
        })
          .then((res) => {
            if (res === 0) {
              resolve();
            } else {
              let msg = this.$t("setting.decompose.glue.unique");
              this.$modal.msgError(msg)
              reject(new Error(msg));
            }
          })
          .catch((error) => {
            console.error(error);
            reject(new Error("验证失败，请稍后再试"));
          });
      });
    },

    checkComplete() {
      return new Promise((resolve, reject) => {
        checkComplete({
          segment: this.form.segment,
          motherGlue1: this.form.motherGlue1,
          motherGlue2: this.form.motherGlue2,
          motherGlue3: this.form.motherGlue3,
          motherGlue4: this.form.motherGlue4,
          motherGlue5: this.form.motherGlue5,
          motherGlue6: this.form.motherGlue6,
          motherGlue7: this.form.motherGlue7,
          motherGlue8: this.form.motherGlue8,
          motherGlue9: this.form.motherGlue9,
        })
          .then((res) => {
            if (res.msg == "0") {
              this.$confirm(
                this.$t("setting.decompose.segment.checkMotherExists")
              )
                .then(() => {
                  resolve();
                })
                .catch(() => {
                  reject();
                });
            } else if (res.msg == "1") {
              resolve();
            } else {
              reject(new Error(this.$t("setting.decompose.glue.unique")));
            }
          })
          .catch((error) => {
            console.error(error);
            reject(new Error("验证失败，请稍后再试"));
          });
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
