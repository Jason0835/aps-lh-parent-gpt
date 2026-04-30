<template>
  <div>
    <slot v-if="$scopedSlots.content" :data="slotData" name="content" />
    <template v-else>
      <template v-for="(item, index) in options">
        <template v-if="values.includes(item[valueKey] + '') || (item.raw && values.includes(item.raw[valueKey] + ''))">
          <span
            v-if="
              (item.raw.listClass == 'default' || item.raw.listClass == '') &&
                (item.raw.cssClass == '' || item.raw.cssClass == null)
            "
            :key="item.id || item[valueKey]"
            :index="index"
            :class="item.raw.cssClass"
          >{{ item[labelKey] + " " }}</span>
          <el-tag
            v-else
            :key="item.id || item[valueKey]"
            :disable-transitions="true"
            :index="index"
            :type="item.raw.listClass == 'primary' ? '' : item.raw.listClass"
            :class="item.raw.cssClass"
          >
            {{ getLabel(item) }}
          </el-tag>
        </template>
      </template>
      <template v-if="unmatch && showValue">
        {{ unmatchArray | handleArray }}
      </template>
    </template>
  </div>
</template>

<script>
export default {
  name: 'DictTag',
  filters: {
    handleArray(array) {
      if (array.length === 0) return ''
      return array.reduce((pre, cur) => {
        return pre + ' ' + cur
      })
    }
  },
  props: {
    options: {
      type: Array,
      default: null
    },
    value: [Number, String, Array],
    // 要过滤的字段，默认是value
    valueKey: {
      type: String,
      default: 'value'
    },
    // 显示的字段
    labelKey: {
      type: [String, Array],
      default: 'label'
    },
    // 当未找到匹配的数据时，显示value
    showValue: {
      type: Boolean,
      default: true
    }
  },
  data() {
    return {
      unmatchArray: [] // 记录未匹配的项
    }
  },
  computed: {
    values() {
      if (this.value !== null && typeof this.value !== 'undefined') {
        return Array.isArray(this.value) ? this.value : [String(this.value)]
      } else {
        return []
      }
    },
    slotData() {
      const list = this.options || []
      const sList = []
      if (this.values.length === 0) {
        for (let i = 0; i < list.length; i++) {
          sList.push(list[i].raw ? list[i].raw : list[i])
        }
        return sList
      }
      const _values = [...this.values]
      for (let i = 0; i < list.length; i++) {
        for (let j = 0; j < _values.length; j++) {
          if (list[i][this.valueKey] + '' === _values[j] + '') {
            sList.push(list[i].raw ? list[i].raw : list[i])
          }
        }
      }
      return sList
    },
    unmatch() {
      this.unmatchArray = []
      if (this.value !== null && typeof this.value !== 'undefined') {
        // 传入值为非数组
        if (!Array.isArray(this.value)) {
          if (!this.options || this.options.some((v) => (v[this.valueKey] == this.value) || (v.raw && v.raw[this.valueKey] == this.value))) return false
          this.unmatchArray.push(this.value)
          return true
        }
        // 传入值为Array
        this.value.forEach((item) => {
          if (!this.options || !this.options.some((v) => (v[this.valueKey] == item) || (v.raw && v.raw[this.valueKey] == item))) { this.unmatchArray.push(item) }
        })
        return true
      }
      // 没有value不显示
      return false
    }
  },
  methods: {
    getText(obj, keys) {
      if (keys.length === 0 || !keys[0]) {
        return obj
      } else {
        const _key = keys[0]
        const keyArr = JSON.parse(JSON.stringify(keys))
        keyArr.splice(0, 1)
        return typeof obj[_key] === 'object' ? this.getText(obj[_key], keyArr) : this.getLangText(obj[_key])
      }
    },
    getLangText(text) {
      text = text ? (text + '') : ''
      if (text && text.indexOf('{') !== -1 && text.indexOf('}') !== -1) {
        try {
          const json = JSON.parse(text)
          const lang = localStorage.getItem('language') || 'zh_CN'
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
    getLabel(item) {
      if (typeof this.labelKey === 'string') {
        return (item[this.labelKey] || item[this.labelKey] === 0) ? this.getLangText(item[this.labelKey])
          : (item.raw && (item.raw[this.labelKey] || item.raw[this.labelKey] === 0)) ? this.getLangText(item.raw[this.labelKey])
            : this.getText(item.raw ? item.raw : item, [this.labelKey])
      } else if (Array.isArray(this.labelKey)) {
        return this.getText(item.raw ? item.raw : item, this.labelKey)
      } else {
        return ''
      }
    }
  }
}
</script>
<style scoped>
.el-tag + .el-tag {
  margin-left: 10px;
}
</style>
