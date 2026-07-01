<template>
  <div>
    <div>
      <el-input
        v-if="allowInput"
        :placeholder="$t('common.rule.input')"
        style="width: 100%"
        ref="inputRef"
        :value="value"
        :maxlength="maxlength"
        clearable
        :disabled="disabled"
        @input="handleInputChange"
        @clear="handleClear"
      >
        <el-button
          slot="append"
          icon="el-icon-search"
          :disabled="disabled"
          @click="handleVisible"
        />
      </el-input>
      <el-select
        v-else
        :placeholder="$t('common.rule.select')"
        style="width: 100%"
        ref="inputRef"
        :value="value"
        clearable
        @focus.stop="handleVisible"
        @clear="handleClear"
        :disabled="disabled"
        @click.native="handleClick"
      />
    </div>
    <el-dialog
      :title="title"
      :visible="visible"
      :width="dialogWidth"
      @close="handleCancel"
      append-to-body
      :destroy-on-close="hideDestroy"
      class="limit-wrapper"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
    >
      <!-- <div class="content"> -->
      <slot></slot>
      <!-- </div> -->
      <template slot="footer">
        <el-button type="primary" :loading="loading" @click="handleConfirm"
          >{{$t("common.button.confirm")}}</el-button
        >
        <el-button :loading="loading" @click="handleCancel">{{$t("common.button.cancel")}}</el-button>
      </template>
    </el-dialog>
  </div>
</template>
<script>
export default {
  // model: {
  //   prop: "value",
  //   event: "update",
  // },
  props: {
    title: String,
    dialogWidth: String,
    value: {
      type: String | Number,
      default: "",
    },
    dialogWidth: {
      type: String | Number,
      default: "800px",
    },
    loading: {
      type: Boolean,
      default: false,
    },
    disabled: {
      type: Boolean,
      default: false,
    },
    hideDestroy: {
      type: Boolean,
      default: true,
    },
    /** 是否允许手动输入（同时保留弹窗选择） */
    allowInput: {
      type: Boolean,
      default: false,
    },
    maxlength: {
      type: Number,
      default: undefined,
    },
  },
  data() {
    return {
      visible: false,
      inputValue: "",
    };
  },
  // watch: {
  //   value:function(val) {
  //     console.log(val)
  //   }
  // },
  methods: {
    /**防止点击箭头出现无数据的下拉框 */
    handleClick() {
      this.$refs.inputRef.blur();
    },
    handleVisible() {
      this.$emit("show");
      this.visible = true;
    },
    handleCancel() {
      this.hide();
      this.$emit("cancel");
    },
    handleConfirm() {
      this.$emit("confirm", () => {
        this.hide();
      });
    },
    handleClear() {
      this.$emit("clear");
    },
    handleInputChange(val) {
      this.$emit("input-change", val);
    },

    hide() {
      if (this.$refs.inputRef) {
        this.$refs.inputRef.blur();
      }
      this.visible = false;
    },
  },
};
</script>
<style lang="scss" scoped>
.content {
  display: flex;
  flex-direction: column;
  height: 100%;
}
</style>
<style scoped>
@media (height < 730px) {
  .limit-wrapper >>> .table-container,
  .limit-wrapper >>> .content {
    height: calc(88vh - 180px);
  }
}
</style>
