<template>
    <div :class="{'has-logo':showLogo}" :style="{ backgroundColor: settings.sideTheme === 'theme-dark' ? variables.menuBackground : variables.menuLightBackground }">
      <MinSubMenuPopper
        v-if="showSubMenuPopper"
        :key="currentMenu.path"
        class="menu-popup"
        :style="{ 'padding-left': !isCollapse ? '200px': '54px!important' }"
        :class="{ 'collapse': isCollapse }"
        :currentMenu="currentMenu"
        :sidebarRouters="sidebarRouters"
        @close="handleHideSubMenuPopper"
      />
      <logo v-if="showLogo" :collapse="isCollapse" />
      <el-scrollbar ref="scrollbar" :class="settings.sideTheme" wrap-class="scrollbar-wrapper">
          <el-menu
              :default-active="activeMenu"
              :collapse="isCollapse"
              :background-color="settings.sideTheme === 'theme-dark' ? variables.menuBackground : variables.menuLightBackground"
              :text-color="settings.sideTheme === 'theme-dark' ? variables.menuColor : variables.menuLightColor"
              :unique-opened="true"
              :active-text-color="variables.menuColorActive"
              :collapse-transition="false"
              mode="vertical"
          >
              <sidebar-item
                  v-for="(route, index) in sidebarRouters"
                  :key="route.path  + index"
                  :item="route"
                  :base-path="route.path"
                  @showSubMenuPopper="handleShowSubMenuPopper"
                  @hideSubMenuPopper="handleHideSubMenuPopper"
              />
          </el-menu>
      </el-scrollbar>
    </div>
</template>

<script>
import { mapGetters, mapState } from "vuex";
import Logo from "./Logo";
import SidebarItem from "./SidebarItem";
import variables from "@/assets/styles/variables.scss";
import MinSubMenuPopper from "@/layout/components/MinSubMenuPopper";

export default {
  components: { SidebarItem, Logo, MinSubMenuPopper },
  data() {
      return {
        showSubMenuPopper: false,
        currentMenu: {}
      }
  },
  computed: {
      ...mapState(["settings", "app"]),
      ...mapGetters(["sidebarRouters", "sidebar"]),
      activeMenu() {
          const route = this.$route;
          const { meta, path } = route;
          // if set path, the sidebar will highlight the path you set
          if (meta.activeMenu) {
              return meta.activeMenu;
          }
          return path;
      },
      showLogo() {
          return this.$store.state.settings.sidebarLogo;
      },
      variables() {
          return variables;
      },
      isCollapse() {
          return !this.sidebar.opened;
      }
  },
  methods: {
    handleShowSubMenuPopper(item) {
      this.currentMenu = {
        ...item,
        offsetTop: item.refNode.referenceElm.offsetTop - (this.$refs.scrollbar.$el.clientHeight * this.$refs.scrollbar.moveY / 100)
      }
      this.showSubMenuPopper = true
    },
    handleHideSubMenuPopper() {
      this.showSubMenuPopper = false
    }
  }
};
</script>

<style lang="sass" scoped>
.menu-popup
  width: 100%
  height: 100vh
  position: fixed
  bottom: 0
  right: 0
  padding-left: 200px
  transition: width 0.28s
  &.collapse
    padding-left: 54px!important

</style>
