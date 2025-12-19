<!--
 * @Description: menu-item 页面
 * @Author: qy
 * @Date: 2024/1/24
-->
<template>
<div>
  <div class="menu-item" :class="{ 'f-item': isTop, 's-item': !isTop && !isBranch, 'ss-item': isBranch }">
    <app-link v-if="_item.meta && (!_item.children || _item.children.length === 0)" :to="resolvePath(_item.path, _item.query)" @click.native="closePopper()">{{ _item.meta.title }}</app-link>
    <span v-if="(_item.children && _item.children.length > 0)">{{ _item.meta.title }}</span>
    <i v-if="!isCollect(resolvePath(_item.path, _item.query)) && (!_item.children || _item.children.length === 0)" class="el-icon-star-off start-off" @click.stop="handleCollect(resolvePath(_item.path, _item.query))"></i>
    <i v-else-if="!_item.children || _item.children.length === 0" class="el-icon-star-on start-on" @click.stop="handleRemoveCollect(resolvePath(_item.path, _item.query))"></i>
  </div>
  <menu-item
    v-if="!subItem.hidden"
    v-for="subItem in _item.children"
    v-on="$listeners"
    :key="subItem.menuId"
    :item="subItem"
    :collectPaths="collectPaths"
    :storeKey="storeKey"
    :basePath="resolvePath(_item.path)"
    :isBranch="!isTop"
  />
</div>
</template>

<script>
import path from 'path'
import {mapGetters} from "vuex";
import cache from "@/plugins/cache";
import {isExternal} from "@/utils/validate";
import AppLink from '../Sidebar/Link'

export default {
  name: "MenuItem",
  components: { AppLink },
  props: {
    // 第三级及以下
    isBranch: {
      type: Boolean,
      default: false
    },
    // 是否顶级菜单
    isTop: {
      type: Boolean,
      default: false
    },
    item: {
      type: Object,
      default: () => {}
    },
    collectPaths: {
      type: Array,
      default: () => {
        return []
      }
    },
    storeKey: {
      type: String,
      default: ''
    },
    basePath: {
      type: String,
      default: ''
    }
  },
  computed: {
    ...mapGetters(['userId']),
    _item: {
      get() {
        if (this.item.children && this.item.children.length > 0) {
          const children = this.item.children.filter((m) => {
            return !m.hidden
          })
          return {
            ...this.item,
            children
          }
        } else {
          return {
            ...this.item
          }
        }
      },
      set() {}
    }
  },
  methods: {
    closePopper() {
      this.$emit('close')
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
    isCollect(path) {
      return this.collectPaths.includes(path)
    },
    handleCollect(path) {
      this.$store.dispatch('handleCollectMenu', path)
    },
    handleRemoveCollect(path) {
      this.$store.dispatch('handleREMOVECollectMenu', path)
    }
  }
}
</script>

<style lang="sass" scoped>
.menu-item
  width: 100%
  font-weight: 400
  font-size: 14px
  padding-left: 38px
  padding-right: 17px
  padding-top: 10px
  padding-bottom: 10px
  line-height: 20px
  display: flex
  align-items: center
  color: #98C2C3
  .start-off
    color: transparent
  &:hover
    .start-off
      color: #A8D3D4
      cursor: pointer
  .start-on
    color: #FFE04B
    font-size: 16px
    cursor: pointer
.f-item
  &:hover
    background-color: #036264
.s-item
  cursor: pointer
  color: #fff
  background-color: #036264
  padding-left: 57px
  &:hover
    background-color: #005658
.ss-item
  cursor: pointer
  color: #fff
  background-color: #005658
  padding-left: 76px


</style>
