<template>
  <section class="app-main">
    <transition name="fade-transform" mode="out-in">
      <keep-alive :include="cachedViews">
        <router-view v-if="!$route.meta.link" />
      </keep-alive>
    </transition>
    <iframe-toggle />
  </section>
</template>

<script>
import iframeToggle from "./IframeToggle/index";

export default {
  name: "AppMain",
  components: { iframeToggle },
  computed: {
    cachedViews() {
      console.log(this.$store.state.tagsView.cachedViews)
      return this.$store.state.tagsView.cachedViews;
    },
    key() {
      return this.$route.path;
    },
  },
};
</script>

<style lang="scss" scoped>
.app-main {
  /* 50= navbar  50  */
  min-height: calc(100vh - 50px);
  width: 100%;
  position: relative;
  overflow: hidden;
}

.fixed-header + .app-main {
  padding-top: 50px;
}

.hasTagsView {
  .app-main {
    /* 84 = navbar + tags-view = 50 + 34 */
    min-height: calc(100vh - 84px);
  }

  .fixed-header + .app-main {
    padding-top: 84px;
  }
}
</style>

<style lang="scss">
// fix css style bug in open el-dialog
.el-popup-parent--hidden {
  .fixed-header {
    padding-right: 6px;
  }
}

.el-button--primary {
  color: #fff;
  background-color: #007f81;
  border-color: #007f81;
}
.el-button--primary:focus,
.el-button--primary:hover {
  background: #017476;
  border-color: #017476;
  color: #fff;
}
.el-input__inner {
  -webkit-appearance: none;
  background-color: #ffffff;
  background-image: none;
  border-radius: 4px;
  border: 1px solid #dadee5;
  -webkit-box-sizing: border-box;
  box-sizing: border-box;
  color: #000;
  display: inline-block;
  font-size: inherit;
  outline: none;
  padding: 0 15px;
  -webkit-transition: border-color 0.2s cubic-bezier(0.645, 0.045, 0.355, 1);
  transition: border-color 0.2s cubic-bezier(0.645, 0.045, 0.355, 1);
  width: 100%;
}
.el-input.is-disabled .el-input__inner {
  // 字体颜色：#b1b1b1
  background-color: #f7f8fa;
  color: #b1b1b1;
  border: 1px solid #dfe4ed;
}

.readOnly
  .el-checkbox__input.is-disabled.is-checked
  .el-checkbox__inner::after {
  border-color: #409eff;
}

.readOnly .el-checkbox__input.is-disabled.is-checked .el-checkbox__inner {
  background-color: #ffffff;
  border-color: #409eff;
}
.form-item-height .el-form-item--mini.el-form-item {
  margin-bottom: 20px;
}

::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

::-webkit-scrollbar-track {
  background-color: #f1f1f1;
}

::-webkit-scrollbar-thumb {
  background-color: #c0c0c0;
  border-radius: 3px;
}

.amt.el-input .el-input__inner {
  text-align: right;
}
.amt.el-input-number .el-input__inner {
  text-align: right;
}

.due-month .el-date-picker__header {
  display: none;
}

.white-space-pre {
  .cell {
    white-space: pre;
  }
}

.el-table .yellow-row {
  background: oldlace;
}
.el-table .yellow2-row {
  background: #fec171;
}
.el-table .yellow3-row {
  background: #fffbc7;
}

.el-table .red-row {
  background: #f9ebeb;
}
.el-table .green-row {
  background: #f0f9eb;
}

.stat-info {
  font-size: 12px;
  color: #676a6c;
  font-weight: bold;
  .stat-value {
    color: #0088cc;
  }
  span {
    margin-left: 5px;
  }
}
</style>
