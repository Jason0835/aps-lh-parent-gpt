<template>
  <el-form class="form-item-height" ref="form" :model="user" :rules="rules" label-width="80px">
    <el-form-item :label="$t('common.api.user.columnname.oldPassword')" prop="oldPassword">
      <el-input v-model="user.oldPassword" :placeholder="$t('common.api.user.placeholder.oldPassword')" type="password" show-password/>
    </el-form-item>
    <el-form-item :label="$t('common.api.user.columnname.newPassword')" prop="newPassword">
      <el-input v-model="user.newPassword" :placeholder="$t('common.api.user.placeholder.newPassword')" type="password" show-password/>
    </el-form-item>
    <el-form-item :label="$t('common.api.user.columnname.confirmPassword')" prop="confirmPassword">
      <el-input v-model="user.confirmPassword" :placeholder="$t('common.api.user.placeholder.confirmPassword')" type="password" show-password/>
    </el-form-item>
    <el-form-item>
      <el-button type="primary" size="mini" :loading="loading" @click="submit">{{$t('common.button.save')}}</el-button>
      <el-button type="danger" size="mini" :loading="loading" @click="close">{{$t('common.button.close')}}</el-button>
    </el-form-item>
  </el-form>
</template>

<script>
import { updateUserPwd } from "@/api/system/user";

export default {
  data() {
    const equalToPassword = (rule, value, callback) => {
      if (this.user.newPassword !== value) {
        callback(new Error(this.$t("common.api.user.error.confirmPassword.equalToPassword")));
      } else {
        callback();
      }
    };
    return {
      loading: false,
      user: {
        oldPassword: undefined,
        newPassword: undefined,
        confirmPassword: undefined
      },
      // 表单校验
      rules: {
        oldPassword: [
          { required: true, message: this.$t("common.api.user.error.oldPassword.isnull"), trigger: "blur" }
        ],
        newPassword: [
          { required: true, message: this.$t("common.api.user.error.newPassword.isnull"), trigger: "blur" },
          { min: 6, max: 20, message: this.$t("common.api.user.error.newPassword.lengthLimit"), trigger: "blur" }
        ],
        confirmPassword: [
          { required: true, message: this.$t("common.api.user.error.confirmPassword.isnull"), trigger: "blur" },
          { required: true, validator: equalToPassword, trigger: "blur" }
        ]
      }
    };
  },
  methods: {
    submit() {
      this.$refs["form"].validate(async valid => {
        if (valid) {
          try {
            this.loading = true;
            const response = await updateUserPwd(this.user.oldPassword, this.user.newPassword);
            this.$modal.msgSuccess(this.$t("common.msg.success.modify"));
          } catch (error) {
            console.error(error);
          } finally {
            this.loading =false;
          }
        }
      });
    },
    close() {
      this.$tab.closePage();
    }
  }
};
</script>
