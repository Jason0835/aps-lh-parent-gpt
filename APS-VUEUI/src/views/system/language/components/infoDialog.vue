<template>
  <el-dialog
    :title="title"
    :visible="visible"
    width="600px"
    @close="hide"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
  >
    <el-form
      class="form-item-height"
      ref="form"
      :model="form"
      :rules="rules"
      label-position="right"
      label-width="120px"
      v-loading="loading"
    >
      <el-form-item :label="$t('common.code')" prop="changeKey" required>
        <el-input v-model="form.changeKey" disabled></el-input>
      </el-form-item>
      <el-form-item :label="$t('common.chinese')" prop="changeValueI18n_zh_CN">
        <el-input v-model="form.changeValueI18n_zh_CN" maxlength="300"></el-input>
      </el-form-item>
      <el-form-item :label="$t('common.english')" prop="changeValueI18n_en_US">
        <el-input v-model="form.changeValueI18n_en_US" maxlength="300"></el-input>
      </el-form-item>
      <el-form-item :label="$t('common.vietnamese')" prop="changeValueI18n_vi_VN">
        <el-input v-model="form.changeValueI18n_vi_VN" maxlength="300"></el-input>
      </el-form-item>
    </el-form>
    <template slot="footer">
      <el-button @click="hide">{{ this.$t("common.button.cancel") }}</el-button>
      <el-button type="primary" :loading="loading" @click="handleConfirm">{{
        this.$t("common.button.confirm")
      }}</el-button>
    </template>
  </el-dialog>
</template>

<script>
  import regexp from "@/utils/regexp";
  import {saveLanguage} from "@/api/bd/i18nChange";


  export default {

  data() {
    return {
      loading: false,
      visible: false,
      isEdit: false,
      form: {},
      rules: {
        changeValueI18n_zh_CN: [
          { required: true, message: this.$t("common.rule.input") },
        ],
        changeValueI18n_en_US: [
          { required: true, message: this.$t("common.rule.input") },
        ],
        changeValueI18n_vi_VN: [
          { required: true, message: this.$t("common.rule.input") },
        ],
        // status: [
        //   {
        //     required: true,
        //     message: this.$t("common.rule.notEmpty"),
        //     trigger: "blur",
        //   },
        // ],
      },
    };
  },
  computed: {
    title: function () {
      return this.isEdit
        ? this.$t("common.button.edit")
        : this.$t("common.button.add");
    },
  },
  methods: {
    validCode: function (rule, value, callback) {
      if (regexp.code.test(value)) {
        //
        callback(rule);
      } else {
        callback();
      }
    },
    validNumber: function (rule, value, callback) {
      if (value && value != "" && isNaN(value)) {
        if (!regexp.num.test(value + "")) {
          return callback(rule);
        }
        callback();
      } else {
        callback();
      }
    },
    // api
    save(params) {
      this.$refs.form.validate(async (valid) => {
        if (!valid) return;
        try {
          this.loading = true;
          const data = await saveLanguage(params);
          this.$modal.msgSuccess(data.msg);
          this.$emit("success");
          this.hide();
        } catch (error) {
          console.log(error);
        } finally {
          this.loading = false;
        }
      });
    },

    //utils
    show(data) {
      this.visible = true;
      if (data) {
        this.isEdit = true;
        this.form = {
            id: data.id,
            changeKey: data.changeKey,
            changeValueI18n_zh_CN: data.changeValueI18n_zh_CN,
            changeValueI18n_en_US: data.changeValueI18n_en_US,
            changeValueI18n_vi_VN: data.changeValueI18n_vi_VN,
};
      }
    },
    hide() {
      this.form = {};
      this.resetForm("form");
      this.isEdit = false;
      this.visible = false;
    },

    handleConfirm() {
      this.$refs.form.validate((valid) => {
        if (valid) {
          this.save({
            ...this.form,
          });
        }
      });
    },
  },
};
</script>
