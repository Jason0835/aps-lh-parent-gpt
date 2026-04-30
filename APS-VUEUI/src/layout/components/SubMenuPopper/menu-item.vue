<!--
 * @Description: menu-item 页面
 * @Author: qy
 * @Date: 2024/1/24
-->
<template>
<div>
  <div class="route-link" :class="{ 'f-item': _item.children && _item.children.length > 0, 's-item': !_item.children || _item.children.length === 0 }">
    <app-link v-if="_item.meta && (!_item.children || _item.children.length === 0)" :to="resolvePath(_item.path, _item.query)" @click.native="closePopper()">{{ _item.meta.title }}</app-link>
    <span v-if="(_item.children && _item.children.length > 0)">{{ _item.meta.title }}</span>
    <i v-if="!isCollect(resolvePath(_item.path, _item.query)) && (!_item.children || _item.children.length === 0)" class="el-icon-star-off start-off" @click.stop="handleCollect(resolvePath(_item.path, _item.query))"></i>
    <i v-else-if="!_item.children || _item.children.length === 0" class="el-icon-star-on start-on" @click.stop="handleRemoveCollect(resolvePath(_item.path, _item.query))"></i>
  </div>
  <menu-item
    class="menu-item"
    v-if="!subItem.hidden"
    v-for="subItem in _item.children"
    v-on="$listeners"
    :key="subItem.menuId"
    :item="subItem"
    :collectPaths="collectPaths"
    :storeKey="storeKey"
    :basePath="resolvePath(_item.path)"
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
      const menuList = JSON.parse(JSON.stringify(this.collectPaths))
      console.log(menuList)
      console.log(path)
      if (path && !menuList.includes(path)) {
        menuList.push(path)
        console.log(menuList)
      }
      cache.local.set(this.storeKey, menuList.join(','))
      this.$emit('updateCollectMenu')
    },
    handleRemoveCollect(path) {
      const menuList = this.collectPaths
      const index = menuList.indexOf(path)
      if (index !== -1) {
        menuList.splice(index, 1)
      }
      cache.local.set(this.storeKey, menuList.join(','))
      this.$emit('updateCollectMenu')
    }
  }
}
</script>

<style lang="sass" scoped>
::v-deep .route-link a
  width: auto!important
  margin-right: 2px
.menu-item
  width: 240px
  font-weight: 400
  margin-bottom: 10px
  font-size: 14px
.f-item
  font-weight: bold
  margin-bottom: 10px
.s-item
  cursor: pointer
.route-link
  display: flex
  align-items: center
  .start-off
    color: transparent
  &:hover
    .start-off
      color: #555555
  .start-on
    color: #e6a23c
    font-size: 20px

</style>
