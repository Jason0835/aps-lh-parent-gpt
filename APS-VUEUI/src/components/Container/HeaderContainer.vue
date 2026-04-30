<template>
  <div class="header-container">
    <header ref="headerRef">
      <slot name="header"></slot>
    </header>
    <div class="content" :style="{ height: height }">
      <slot></slot>
    </div>
  </div>
</template>
<script>
export default {
  name: "HeaderContainer",
  data() {
    return {
      height: "calc(100vh - 144px)",
    };
  },
  methods: {
    getHeight() {
      if (this.$refs.headerRef) {
        const headerHeight = this.$refs.headerRef.clientHeight;
        this.height = `calc(100vh - 120px - ${headerHeight}px)`;
        return;
      }

      this.height = "calc(100vh - 154px)";
    },

    handleBack() {
      this.$emit("back");
    },
  },
  mounted() {
    this.getHeight();
  },
};
</script>

<style lang="scss" scoped>
.header-container {
  width: 100%;
  height: 100%;
  overflow: hidden;
  padding: 10px;
  box-sizing: border-box;
  header {
    // min-height: 40px;
    box-sizing: border-box;
    padding: 0 0 5px 0;
    border-bottom: 1px solid #dcdfe6;
    margin-bottom: 10px;
  }
  .content {
    width: 100%;
    // height: calc(100vh - 144px);
    overflow-x: hidden;
    overflow-y: auto;
    box-sizing: content-box;
    padding-right: 10px;
  }
}
</style>
