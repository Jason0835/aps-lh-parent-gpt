<!--
 * @Description:  菜单子页面弹出
 * @Author: qy
 * @Date: 2024/1/23
-->
<template>
  <div @click.self="closePopper">
    <div class="popper">
      <i class="el-icon-close close-btn" @click="closePopper"></i>
      <div
        class="fill-height"
        style="display: flex; justify-content: space-between"
      >
        <div
          class="fill-height"
          style="display: flex; flex-flow: column; flex: 1"
        >
          <el-input
            :placeholder="$t('tagsView.inputMenuName')"
            v-model="keyword"
            small
            class="input-with-search"
            @keyup.enter.native="handleSearch"
          >
            <i slot="prefix" class="el-input__icon el-icon-search"></i>
          </el-input>
          <div class="menu-list" style="align-content: start">
            <menuItem
              v-for="item in menuList"
              v-if="!item.hidden"
              v-on="$listeners"
              :key="item.menuId"
              :item="item"
              class="menu"
              :collectPaths="collectPaths"
              :storeKey="storeKey"
              :basePath="currentMenu.path"
              @updateCollectMenu="getCollectMenu"
            />
          </div>
        </div>
        <div class="fill-height" style="padding-top: 30px">
          <div class="fill-height collect-list">
            <div
              class="collect-item"
              v-for="(item, index) in collectMenu"
              :key="item.path"
              v-if="item.meta"
            >
              <app-link
                v-if="item.meta"
                :to="item.fullPath"
                @click.native="closePopper()"
                >{{ item.meta.title }}</app-link
              >
              <i
                class="el-icon-delete remove-btn"
                @click="removeCollectItem(index)"
              ></i>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import menuItem from "@/layout/components/SubMenuPopper/menu-item";
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
      collectMenu: [],
      collectPaths: [],
      storeKey: null, // 缓存的local Key
    };
  },
  computed: {
    ...mapGetters(["userId"]),
  },
  watch: {
    currentMenu() {
      this.menuList = (this.currentMenu && this.currentMenu.children) || [];
    },
  },
  mounted() {
    this.menuList = (this.currentMenu && this.currentMenu.children) || [];
    this.getCatchKey();
    this.getCollectMenu();
  },
  methods: {
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
    getCatchKey() {
      let key = "COLLECT_MENUS";
      if (this.userId || this.userId === 0) {
        key += `_${this.userId}`;
      }
      this.storeKey = key;
      return key;
    },
    handleFilterByName(menus, keyword) {
      const list = [];
      for (let i = 0; i < menus.length; i++) {
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
          const children = this.handleFilterByName(menus[i].children, keyword);
          list.push(...children);
        } else if (
          !menus[i].hidden &&
          menus[i].meta &&
          menus[i].meta.title.indexOf(keyword) !== -1
        ) {
          list.push({
            ...menus[i],
            children: null,
          });
        }
      }
      return list;
    },
    handleFilterByPath(menus, path, basePath) {
      const list = [];
      for (let i = 0; i < menus.length; i++) {
        if (!menus[i].hidden) {
          const fullPath = this.resolvePath(
            basePath || "",
            menus[i].path,
            menus[i].query
          );
          if (fullPath === path) {
            list.push({
              ...menus[i],
              children: null,
              fullPath,
            });
          } else if (menus[i].children && menus[i].children.length > 0) {
            const children = this.handleFilterByPath(
              menus[i].children,
              path,
              this.resolvePath(basePath || "", menus[i].path)
            );
            list.push(...children);
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
        this.keyword
      );
    },
    getCollectMenu() {
      const list = [];
      const str = cache.local.get(this.storeKey);
      const paths = str ? str.split(",") : [];
      for (let i = 0; i < paths.length; i++) {
        const arr = this.handleFilterByPath(this.sidebarRouters, paths[i]);
        if (arr[0]) {
          list.push(arr[0]);
        }
      }
      this.collectMenu = list;
      this.collectPaths = paths;
    },
    removeCollectItem(index) {
      this.collectPaths.splice(index, 1);
      this.collectMenu.splice(index, 1);
      cache.local.set(this.storeKey, this.collectPaths.join(","));
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
  padding: 30px
  position: relative
  width: 100%
  height: 100%
  background-color: #fff
  z-index: 999
  .close-btn
    position: absolute
    right: 25px
    top: 25px
    font-size: 18px
    cursor: pointer
  .input-with-search
    ::v-deep input
      border-radius: 0
      border-top: none
      border-left: none
      border-right: none
  .menu-list
    font-size: 16px
    padding: 30px
    display: flex
    flex: 1
    flex-wrap: wrap
    .menu
      width: 180px
      height: auto
      min-height: 0
      margin-right: 20px
      margin-bottom: 10px
  .collect-list
    width: 20%
    min-width: 240px
    overflow: auto
    padding: 20px 0 0 40px
    .collect-item
      font-size: 16px
      line-height: 35px
      font-weight: bold
      display: flex
      align-items: center
      .remove-btn
        color: transparent
        cursor: pointer
      &:hover
        .remove-btn
          color: red!important
</style>
