<!--
 * @Description: form表单
 * @Author: qy
 * @Date: 2024/1/26
-->
<template>
  <div>
    <el-form
      v-bind="$attrs"
      ref="ruleForm"
      class="demo-ruleForm"
      :class="{ 'hide-ero-msg': !showMessage }"
    >
      <template>
        <slot></slot>
      </template>
    </el-form>
    <el-dialog
      :title="$t('common.prompt')"
      v-dialogDrag
      :visible.sync="dialogVisible"
      width="320px"
    >
      <p
        v-for="(item, index) in errorMsgList"
        style="color: #e66733; font-size: 12px"
        :key="index"
      >
        <i class="el-icon-warning"></i>【{{ item.label }}】{{ item.message }}
      </p>
      <span slot="footer" class="dialog-footer">
        <el-button @click="dialogVisible = false">{{$t("common.button.close")}}</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
export default {
  props: {
    showErrorDialog: {
      type: Boolean,
      default: false,
    },
    showMessage: {
      type: Boolean,
      default: false,
    },
  },
  data() {
    return {
      dialogVisible: false,
      ref: null,
      errorMsgList: [],
    };
  },
  mounted() {
    this.ref = this.$refs['ruleForm']
  },
  methods: {
    clearValidate(...params) {
      return this.$refs.ruleForm.clearValidate(params);
    },
    deregisterLabelWidth(...params) {
      return this.$refs.ruleForm.deregisterLabelWidth(params);
    },
    getLabelWidthIndex(...params) {
      return this.$refs.ruleForm.getLabelWidthIndex(params);
    },
    potentialLabelWidthArr(...params) {
      return this.$refs.ruleForm.potentialLabelWidthArr(params);
    },
    registerLabelWidth(...params) {
      return this.$refs.ruleForm.registerLabelWidth(params);
    },
    resetFields(...params) {
      return this.$refs.ruleForm.resetFields(params);
    },
    showDialog(errMsg) {
      const that = this;
      if (!errMsg) {
        return;
      }
      const labels = {};
      const fields = that.$refs.ruleForm.fields;
      for (let i = 0; i < fields.length; i++) {
        labels[fields[i].prop] =
          fields[i].label ||
          (fields[i].$attrs && fields[i].$attrs.labelText) ||
          "";
      }
      const list = [];
      for (let key in errMsg) {
        errMsg[key].map((item) => {
          list.push({
            field: item.field,
            message: item.message,
            label: labels[item.field] || "",
          });
        });
      }
      that.errorMsgList = list;
      that.dialogVisible = true;
    },
    validate(callback) {
      const that = this;
      return new Promise((resolve, reject) => {
        that.$refs.ruleForm.validate((valid, errMsg) => {
          if (that.showErrorDialog && !valid) {
            that.showDialog(errMsg);
          }
          callback(valid, errMsg);
          if (valid) {
            resolve(valid);
          } else {
            reject(errMsg);
          }
        });
      });
    },
    validateField(params, callback) {
      if (!params || typeof params !== 'string') {
        throw new Error('参数错误')
      }
      return this.$refs.ruleForm.validateField(params, (invalid, errMsg) => {
        if (this.showErrorDialog || errMsg) {
          this.showDialog(errMsg)
          if (callback && typeof callback === 'function') {
            callback(invalid, errMsg)
          }
        }
      });
    },
  },
};
</script>

<style lang="sass" scoped>
::v-deep .el-dialog__body
  padding: 0 20px
::v-deep .hide-ero-msg
  .el-form-item__error
    display: none!important
::v-deep .el-dialog__wrapper
  display: flex
  justify-items: center
  align-items: center
</style>
