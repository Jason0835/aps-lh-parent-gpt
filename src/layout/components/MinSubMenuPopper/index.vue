<!--
 * @Description:  菜单子页面弹出
 * @Author: qy
 * @Date: 2024/3/5
-->
<template>
  <div @click.self="closePopper">
    <div ref="popper" class="popper" :style="popperStyle">
      <div class="search-box">
        <el-input
          :placeholder="$t('tagsView.inputMenuName')"
          v-model="keyword"
          small
          class="input-with-search"
          @keyup.enter.native="handleSearch"
        >
          <i
            slot="prefix"
            class="el-input__icon el-icon-search"
            @click="handleSearch"
          ></i>
        </el-input>
      </div>
      <div
        class="menu-list"
        style="padding-top: 10px; background-color: #036f71"
      >
        <el-scrollbar
          :style="{ height: scrollViewHeight }"
          wrap-class="scrollbar-wrapper"
        >
          <menuItem
            v-for="item in menuList"
            v-if="!item.hidden"
            v-on="$listeners"
            :key="item.menuId"
            :item="item"
            :collectPaths="collectPaths"
            :storeKey="storeKey"
            :basePath="currentMenu.path"
            :isTop="true"
          />
        </el-scrollbar>
      </div>
    </div>
  </div>
</template>

<script>
import menuItem from "./menu-item";
import { mapGetters } from "vuex";
import cache from "@/plugins/cache";
import { isExternal } from "@/utils/validate";
import AppLink from "../Sidebar/Link";
import path from "path";

export default {
  components: { menuItem, AppLink },
  props: {
    currentMenu: {
      type: Object,
      default: () => {
        return {};
      },
    },
    sidebarRouters: {
      type: Array,
      default: () => {
        return [];
      },
    },
  },
  data() {
    return {
      keyword: "",
      menuList: [],
      storeKey: null, // 缓存的local Key
      popperStyle: {},
      itemHeight: 40, // 菜单高度
    };
  },
  computed: {
    ...mapGetters(["userId", "collectPaths", "collectMenu"]),
    scrollViewHeight: {
      get() {
        const menusLen = this.getMenuLen(this.menuList);
        const viewportHeight =
          (document &&
            document.documentElement &&
            document.documentElement.clientHeight) ||
          (document && document.body && document.body.clientHeight) ||
          0; // 可视区域高度
        return (
          (viewportHeight - 85 > menusLen * this.itemHeight
            ? menusLen * this.itemHeight
            : viewportHeight - 85) + "px"
        );
      },
    },
  },
  watch: {
    currentMenu() {
      this.menuList = (this.currentMenu && this.currentMenu.children) || [];
    },
    menuList() {
      this.popperStyle = {};
      this.$nextTick(() => {
        this.updateHeight();
      });
    },
  },
  mounted() {
    this.menuList = (this.currentMenu && this.currentMenu.children) || [];
    this.$nextTick(() => {
      this.updateHeight();
    });
  },
  beforeDestroy() {},
  methods: {
    getMenuLen(menus) {
      let len = 0;
      for (let i = 0; i < menus.length; i++) {
        if (!menus[i].hidden) {
          len += 1;
        }
        if (menus[i].children) {
          len += this.getMenuLen(menus[i].children);
        }
      }
      return len;
    },
    updateHeight() {
      const viewportHeight = document.documentElement.clientHeight; // 可视区域高度
      const popperHeight = this.$refs.popper.clientHeight; // 菜单高度
      const offsetTop =
        this.currentMenu.offsetTop && this.currentMenu.offsetTop >= 0
          ? this.currentMenu.offsetTop
          : 0; // 点击的子菜单位置
      if (viewportHeight > popperHeight) {
        if (popperHeight + offsetTop > viewportHeight) {
          this.popperStyle = {
            bottom: "initial",
            top: viewportHeight - popperHeight + "px",
          };
        } else {
          this.popperStyle = {
            bottom: "initial",
            top: offsetTop + "px",
          };
        }
      }
    },
    closePopper() {
      this.$emit("close");
    },
    resolvePath(basePath, routePath, routeQuery) {
      if (isExternal(routePath)) {
        return routePath;
      }
      if (isExternal(basePath)) {
        return basePath;
      }
      if (routeQuery) {
        let query = JSON.parse(routeQuery);
        return { path: path.resolve(basePath, routePath), query: query };
      }
      return path.resolve(basePath, routePath);
    },
    handleFilterByName(menus, keyword, basePath) {
      const list = [];
      for (let i = 0; i < menus.length; i++) {
        console.log();

        let hasChildren = false;
        // 如果有可显示的子菜单
        if (menus[i].children && menus[i].children.length > 0) {
          for (let j = 0; j < menus[i].children.length; j++) {
            if (!menus[i].children[j].hidden) {
              hasChildren = true;
              break;
            }
          }
        }
        if (hasChildren) {
          const children = this.handleFilterByName(
            menus[i].children,
            keyword,
            this.resolvePath(basePath, menus[i].path)
          );
          list.push(...children);
        } else if (!menus[i].hidden && menus[i].meta) {
          let title = menus[i].meta.i18n ? this.$t(menus[i].meta.i18n) : menus[i].meta.title
          if (title.indexOf(keyword) !== -1) {
            list.push({
              ...menus[i],
              path: this.resolvePath(basePath, menus[i].path),
              children: null,
            });
          }
        }
      }
      return list;
    },
    handleSearch() {
      if (!this.keyword) {
        this.menuList = (this.currentMenu && this.currentMenu.children) || [];
        return;
      }
      this.menuList = this.handleFilterByName(
        this.sidebarRouters,
        this.keyword,
        "/"
      );
    },
  },
};
</script>

<style lang="sass" scoped>
::v-deep .collect-item a
  width: auto!important
  margin-right: 5px
.fill-height
  height: 100%
.popper
  position: absolute
  width: 277px
  max-height: 100vh
  background-color: #036F71
  z-index: 999
  padding: 19px 0
  .search-box
    padding: 0 14px 0 15px
    .input-with-search
      ::v-deep input
        height: 37px
        font-size: 14px
        background-color: #298A8C
        border-radius: 0
        border: none
        color: #fff
</style>
