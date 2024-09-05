<!--
 * @Description: 基础数据组件
 * @Author: qy
 * @Date: 2024/1/29
-->
<template>
<div v-bind="$attrs">
  <div v-if="!$scopedSlots.content">
    <div class="row" v-for="item in list" :key="item.id">
      <span v-if="!$slots.default">{{getText(item[labelName])}}</span>
    </div>
  </div>
  <slot v-else :data="list" name="content"></slot>
</div>
</template>

<script>
export default {
  props: {
    dataKey: {
      type: String,
      default: ''
    },
    query: {
      key: Object,
      default: null
    },
    // 要显示的字段名称
    labelName: {
      type: String,
      default: 'name'
    },
  },
  data() {
    return {
      list: []
    }
  },
  mounted() {
    this.getList()
  },
  watch: {
    dataKey() {
      this.getList()
    }
  },
  methods: {
    getText(text) {
      text = text ? (text + '') : ''
      if (text && text.indexOf('{') !== -1 && text.indexOf('}') !== -1) {
        try {
          const json = JSON.parse(text)
          const lang = localStorage.getItem("language") || "zh_CN"
          if (Array.isArray(json)) {
            for (let i = 0; i < json.length; i++) {
              if (json[i][lang]) {
                return json[i][lang]
              }
            }
          }
        } catch (e) {
          return text
        }
      }
      return text
    },
    async getList() {
      this.list = []
      if (!this.query) {
        this.list = await this.$store.dispatch('baseData/getBaseData', this.dataKey) || []
        return
      }
      this.list = await this.$store.dispatch('baseData/getBaseDataValue', { key: this.dataKey, query: this.query}) || []
    }
  }
}
</script>

<style scoped>

</style>
