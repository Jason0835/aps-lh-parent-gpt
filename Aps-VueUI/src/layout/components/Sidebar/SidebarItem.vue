<template>
  <div v-if="!item.hidden">
    <template v-if="hasOneShowingChild(item.children,item) && (!onlyOneChild.children||onlyOneChild.noShowingChildren)&&!item.alwaysShow">
      <app-link v-if="onlyOneChild.meta" :to="resolvePath(onlyOneChild.path, onlyOneChild.query)">
        <el-menu-item :index="resolvePath(onlyOneChild.path)" :class="{'submenu-title-noDropdown':!isNest}" @click.native="onSubMenuOver()">
          <item :icon="onlyOneChild.meta.icon||(item.meta&&item.meta.icon)" :title="getTitle(onlyOneChild)" />
        </el-menu-item>
      </app-link>
    </template>

    <el-submenu v-else ref="subMenu" :class="{'desktop-menu': device === 'desktop'}" :popper-class="subMenuPopperClass" :index="resolvePath(item.path)" @click.native="(e) => { onSubMenuOver(e, item) }" popper-append-to-body>
      <template slot="title">
        <item v-if="item.meta" :icon="item.meta && item.meta.icon" :title="getTitle(item)" />
      </template>
    </el-submenu>
  </div>
</template>

<script>
import path from 'path'
import { isExternal } from '@/utils/validate'
import Item from './Item'
import AppLink from './Link'
import { mapState } from 'vuex'
import FixiOSBug from './FixiOSBug'
import Clickoutside from 'element-ui/src/utils/clickoutside'

export default {
  name: 'SidebarItem',
  components: { Item, AppLink },
  mixins: [FixiOSBug],
  directives: { Clickoutside },
  props: {
    // route object
    item: {
      type: Object,
      required: true
    },
    isNest: {
      type: Boolean,
      default: false
    },
    basePath: {
      type: String,
      default: ''
    }
  },
  data() {
    this.onlyOneChild = null
    return {
      subMenuPopperClass: 'sidebar-menu-popper-class'
    }
  },
  computed: {
    ...mapState({
      device: state => state.app.device,
    }),
  },
  methods: {
    onSubMenuOver(e, item) {
      if (item) {
        this.$emit('showSubMenuPopper', { event: e, ...item, refNode: this.$refs.subMenu })
      } else {
        this.$emit('hideSubMenuPopper')
      }

    },
    hasOneShowingChild(children = [], parent) {
      if (!children) {
        children = [];
      }
      const showingChildren = children.filter(item => {
        if (item.hidden) {
          return false
        } else {
          // Temp set(will be used if only has one showing child)
          this.onlyOneChild = item
          return true
        }
      })

      // When there is only one child router, the child router is displayed by default
      if (showingChildren.length === 1) {
        return true
      }

      // Show parent if there are no child router to display
      if (showingChildren.length === 0) {
        this.onlyOneChild = { ... parent, path: '', noShowingChildren: true }
        return true
      }

      return false
    },
    resolvePath(routePath, routeQuery) {
      if (isExternal(routePath)) {
        return routePath
      }
      if (isExternal(this.basePath)) {
        return this.basePath
      }
      if (routeQuery) {
        let query = JSON.parse(routeQuery);
        return { path: path.resolve(this.basePath, routePath), query: query }
      }
      return path.resolve(this.basePath, routePath)
    },
    getTitle(onlyOneChild) {
      return onlyOneChild.meta.i18n ? this.$t(onlyOneChild.meta.i18n) : onlyOneChild.meta.title
    }
  }
}
</script>
<style lang="sass" scoped>
  ::v-deep .el-submenu__title
    &>.el-submenu__icon-arrow
      transform: rotateZ(270deg)!important
</style>
<style>
.sidebar-menu-popper-class> .el-menu--popup {
  padding: 0!important;
}
</style>
