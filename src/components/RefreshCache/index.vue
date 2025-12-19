<template>
  <div @click="handleRefreshCache">
    <!-- <i class="el-icon-refresh"  @click="handleRefreshCache" /> -->
    <svg-icon icon-class="refresh" />
  </div>
</template>

<script>
import { refreshCache } from "@/api/system/dict/type";
export default {
  name: "RefreshCache",
  data() {
    return {};
  },
  methods: {
    /** 刷新缓存按钮操作 */
    handleRefreshCache() {
      this.$modal
        .confirm(this.$t("common.button.refreshCache"))
        .then(function () {
          return refreshCache();
        })
        .then(() => {
          this.$modal.msgSuccess(this.$t("common.msg.success.refresh"));
          this.$store.dispatch("dict/cleanDict");
          //再刷新页面
          setTimeout(() => {
            window.location.href = window.location.pathname;
          }, 1000);

        })
        .catch(() => {});
    },
  },
};
</script>

<style scoped>
.del-svg {
  display: inline-block;
  cursor: pointer;
  fill: #5a5e66;
  width: 20px;
  height: 20px;
  vertical-align: 10px;
}
</style>
