<template>
  <el-form class="form-item-height" ref="form" :model="user" :rules="rules" label-width="80px">
    <el-form-item :label="$t('common.api.user.columnname.nickname')" prop="nickName">
      <el-input v-model="user.nickName" maxlength="30" />
    </el-form-item>
    <el-form-item :label="$t('common.api.user.columnname.telphone')" prop="phonenumber">
      <el-input v-model="user.phonenumber" maxlength="11" />
    </el-form-item>
    <el-form-item :label="$t('common.api.user.columnname.email')" prop="email">
      <el-input v-model="user.email" maxlength="50" />
    </el-form-item>
    <el-form-item :label="$t('common.api.user.columnname.sex')">
      <el-radio-group v-model="user.sex">
        <el-radio label="0">{{$t('common.api.user.columnname.man')}}</el-radio>
        <el-radio label="1">{{$t('common.api.user.columnname.woman')}}</el-radio>
      </el-radio-group>
    </el-form-item>
    <el-form-item>
      <el-button type="primary" size="mini" :loading="loading" @click="submit">{{$t('common.button.save')}}</el-button>
      <el-button type="danger" size="mini" :loading="loading" @click="close">{{$t('common.button.close')}}</el-button>
    </el-form-item>
  </el-form>
</template>

<script>
import { updateUserProfile } from "@/api/system/user";

export default {
  props: {
    user: {
      type: Object
    }
  },
  data() {
    return {
      loading: false,
      // 表单校验
      rules: {
        nickName: [
          { required: true, message: this.$t("common.api.user.error.nickname.isnull"), trigger: "blur" }
        ],
        email: [
          { required: true, message: this.$t("common.api.user.error.email.isnull"), trigger: "blur" },
          {
            type: "email",
            message: this.$t("common.rule.email"),
            trigger: ["blur", "change"]
          }
        ],
        phonenumber: [
          { required: true, message: this.$t("common.api.user.error.telephone.isnull"), trigger: "blur" },
          {
            pattern: /^1[3|4|5|6|7|8|9][0-9]\d{8}$/,
            message: this.$t("common.rule.phone"),
            trigger: "blur"
          }
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
            const response = await updateUserProfile(this.user);
            this.$modal.msgSuccess(this.$t("common.msg.success.modify"));
          } catch (error) {
            console.error(error);
          }finally {
            this.loading = false;
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
