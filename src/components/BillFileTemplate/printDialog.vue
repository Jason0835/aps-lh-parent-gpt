<!--
 * @Description: printDialog 页面
 * @Author: qy
 * @Date: 2024/2/26
-->
<script>
export default {
  name: "PrintDialog",
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    renderForm: {
      type: Function,
      default: null
    }
  },
  data() {
    return {
      form: {}
    }
  },
  watch: {
    visible: {
      handler: function(val, oldVal) {
        if (val) {
          this.form = {}
        }
      },
      immediate: true
    }
  },
  methods: {
    handleConfirm() {
      this.$emit('submit', this.form)
    },
    handleCancel() {
      this.$emit('close')
    }
  },
  render(createElement, context) {
    return (
      <el-dialog
      title={this.$t("common.button.print")}
      visible={this.visible}
      width="500px">
        { this.renderForm ? this.renderForm(this.form) : '' }
        <template slot="footer">
          <el-button type="primary" onClick={this.handleConfirm}>{this.$t("common.button.confirm")}</el-button>
        <el-button onClick={this.handleCancel}>{this.$t("common.button.cancel")}</el-button>
  </template>
    </el-dialog>
    )
  }
}
</script>

<style scoped>
::v-deep .el-dialog__body {
  padding-top: 0!important;
  padding-bottom: 0;
}
</style>
