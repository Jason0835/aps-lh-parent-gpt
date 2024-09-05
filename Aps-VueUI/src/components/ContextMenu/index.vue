<!--
 * @Description: index 右键菜单
 * @Author: qy
 * @Date: 2024/1/19
-->
<template>
  <div style="display: inline-block" v-clickoutside="(e) => handleClickOutside(e)">
    <div @mousedown="handleMouseDown" @contextmenu.prevent style="left: initial;display: inline-block"><slot></slot></div>
    <transition :name="transition" @after-leave="handleDropdownLeave">
      <div
        v-show="dropDownVisible"
        :key="contextmenuKey"
        :id="contextmenuKey"
        ref="popper"
        class="popper"
        :style="{'left': this.position.left, 'top': this.position.top, 'right': this.position.right, 'bottom': this.position.bottom, 'position': 'fixed', 'zIndex': this.zIndex}"
        :x-placement="placement"
        :class="['el-popper', 'el-cascader__dropdown', 'popper', 'context-menu-popper']">
        <el-cascader-panel
          ref="panel"
          v-bind="$attrs"
          :style="{'flex-direction': itemRowReverse ? 'row-reverse' : 'row'}"
          :value="checkedValue"
          :options="options"
          :props="{ expandTrigger: 'hover' }"
          :border="false"
          :render-label="$scopedSlots.content"
          @change="handleChange"
          @expand-change="handleExpandChange"
          @close="handleClose()"></el-cascader-panel>
      </div>
    </transition>
  </div>
</template>

<script>
import Vue from 'vue';
import Popper from 'element-ui/src/utils/vue-popper';
import Clickoutside from 'element-ui/src/utils/clickoutside';

const PopperMixin = {
  props: {
    placement: {
      type: String,
      default: 'top-start'
    },
    appendToBody: Popper.props.appendToBody,
    visibleArrow: {
      type: Boolean,
      default: true
    },
    arrowOffset: Popper.props.arrowOffset,
    offset: Popper.props.offset,
    boundariesPadding: Popper.props.boundariesPadding,
    popperOptions: Popper.props.popperOptions,
    transformOrigin: Popper.props.transformOrigin
  },
  methods: Popper.methods,
  data: Popper.data,
  beforeDestroy: Popper.beforeDestroy
};

export default {
  name: 'TContextMenu',
  componentName: 'TContextMenu',
  props: {
    options: {
      type: Array,
      default: () => []
    }
  },
  data() {
    return {
      itemWidth: 360,
      itemHeight: 204,
      transition: 'el-zoom-in-bottom',
      columnReverse: false,
      dropDownVisible: false,
      itemRowReverse: false,
      contextmenuKey: new Date().getTime(),
      checkedValue: '',
      position: {
        left: 'initial',
        right: 'initial',
        top: 'initial',
        bottom: 'initial'
      },
      contentMenuOption: [],
      zIndex: 2000
    };
  },
  mixins: [PopperMixin],
  directives: { Clickoutside },
  mounted() {
  },
  beforeDestroy() {
    window.removeEventListener('scroll', this.handleScroll);
  },
  methods: {
    handleDropdownLeave() {},
    handleChange(e) {
      const that = this;
      this.checkedValue = e;
      this.$emit('change', e);
      this.$nextTick(() => {
        that.toggleDropDownVisible(false);
      });
    },
    handleExpandChange(e) {
      const cascaderEl = this.$refs.panel.$el.children;
      this.$nextTick(() => {
        if (cascaderEl.length > 1) {
          const cHeight = cascaderEl[cascaderEl.length - 2] && cascaderEl[cascaderEl.length - 2].querySelector('.el-cascader-menu__wrap') && cascaderEl[cascaderEl.length - 2].querySelector('.el-cascader-menu__wrap').style ? cascaderEl[cascaderEl.length - 2].querySelector('.el-cascader-menu__wrap').style.height : '';
          const curPanel = cascaderEl[cascaderEl.length - 1] && cascaderEl[cascaderEl.length - 1].querySelector('.el-cascader-menu__wrap') ? cascaderEl[cascaderEl.length - 1].querySelector('.el-cascader-menu__wrap') : null;
          if (curPanel && cHeight) {
            curPanel.style.height = cHeight;
          }
        }
      });
    },
    handleScroll() {
      if (this.dropDownVisible) {
        this.toggleDropDownVisible(false);
      }
    },
    handleClose() {
      this.toggleDropDownVisible(false);
    },
    handleClickOutside() {
      this.toggleDropDownVisible(false);
    },
    toggleDropDownVisible(visible, c) {
      const that = this;
      if (!visible && this.$refs.panel) {
        this.checkedValue = [];
      }
      that.$nextTick(() => {
        that.dropDownVisible = visible;
        that.$nextTick(() => {
          that.zIndex = Vue && Vue.prototype && Vue.prototype.$ELEMENT && Vue.prototype.$ELEMENT.zIndex ? Vue.prototype.$ELEMENT.zIndex : 2000;
          // const popperEl = that.$refs.panel.$el.querySelector('.el-cascader-menu').querySelector('.el-cascader-menu__wrap');
          // if (popperEl) {
          //   popperEl.style.height = that.contentMenuOption.length * 33 + 30 + 'px';
          // }
          that.updatePopper();
          if (visible) {
            window.addEventListener('scroll', that.handleScroll);
          } else {
            window.removeEventListener('scroll', that.handleScroll);
          }
        });
      });
    },
    setPosition(e) {
      const visibleArea = {
        width: window.innerWidth || document.documentElement.clientWidth || document.body.clientWidth,
        height: window.innerHeight || document.documentElement.clientHeight || document.body.clientHeight
      };
      if ((visibleArea.width - e.clientX) <= this.itemWidth) {
        this.position.right = visibleArea.width - e.clientX + 'px';
        this.itemRowReverse = true;
        this.position.left = 'initial';
      } else {
        this.position.left = e.clientX + 'px';
        this.itemRowReverse = false;
        this.position.right = 'initial';
      }
      if ((visibleArea.height - e.clientY) <= this.itemHeight) {
        this.position.bottom = visibleArea.height - e.clientY + 'px';
        this.position.top = 'initial';
        this.transition = 'el-zoom-in-bottom';
      } else {
        this.position.top = e.clientY + 'px';
        this.position.bottom = 'initial';
        this.transition = 'el-zoom-in-top';
      }
    },
    handleMouseDown(e) {
      if (e.button === 2 && !this.dropDownVisible) {
        e.preventDefault();
        this.setPosition(e);
        this.contextmenuKey = new Date().getTime();
        this.toggleDropDownVisible(true);
      } else {
        this.toggleDropDownVisible(false);
      }
    }
  }
};
</script>

<style scoped>
.popper {
  display: inline-block;
  position: fixed;
}
::v-deep .el-cascader-menu {
  border-right: solid 1px #dfe4ed!important;
}
</style>
