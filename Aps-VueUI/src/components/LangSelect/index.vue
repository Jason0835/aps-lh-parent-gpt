<template>
  <el-dropdown
    trigger="click"
    class="international"
    @command="handleSetLanguage"
  >
    <div class="international-icon">
      <svg-icon class-name="international-icon" icon-class="language2" />
    </div>
    <el-dropdown-menu slot="dropdown">
      <el-dropdown-item :disabled="language === 'zh_CN'" command="zh_CN">
        中文
      </el-dropdown-item>
      <!-- <el-dropdown-item :disabled="language === 'en_US'" command="en_US">
        English
      </el-dropdown-item> -->
      <el-dropdown-item :disabled="language === 'vi_VN'" command="vi_VN">
        Việt nam
      </el-dropdown-item>
    </el-dropdown-menu>
  </el-dropdown>
</template>

<script>
import { changeLang } from "@/api/user";
export default {
  computed: {
    language() {
      return this.$store.getters.language;
    },
    userId() {
      return this.$store.getters.userId;
    },
  },
  methods: {
    async handleSetLanguage(lang) {
      // this.$i18n.locale = lang

      // this.$store.dispatch('app/setLanguage', lang)
      this.$message({
        message: "设置语言成功",
        type: "success",
      });
      if (this.userId) {
        await changeLang(lang);
      }
      // await this.$router.push({
      //   path: this.$route.path,
      //   query: { ...this.$route.query, lang },
      //   meta: { isReplace: true },
      // });
      // this.$router.go(0)

       let newUrl = window.location.pathname + "?lang="+lang;
       window.location.href = newUrl;
    },
  },
};
</script>

<style lang="scss" scoped>
.international {
  // border: 1px solid black;
  height: 2em;
  width: 2em;
  .international-icon {
    width: 100%;
    height: 100%;
    transform: translateY(-0.3em);
  }
}
</style>
