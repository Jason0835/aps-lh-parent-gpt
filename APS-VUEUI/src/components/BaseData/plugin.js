/**
 * @Description:  baseData mixin
 * @Author: qy
 * @Date: 2024/1/29
 **/
export default {
  install(Vue) {
    Vue.mixin({
      created() {
        const that = this
        if (!that.$options.baseData) {
          return
        }
        const baseDataKeys = that.$options.baseData
        baseDataKeys.forEach(async (key) => {
          await that.$store.dispatch('baseData/getBaseData', key)
        })
      }
    })
  }
}
