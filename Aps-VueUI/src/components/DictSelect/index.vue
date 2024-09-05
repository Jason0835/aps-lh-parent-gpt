<template>
  <el-select class="w100" v-bind="$attrs" v-on="$listeners">
    <el-option
      v-for="item in options"
      :key="item.value"
      :label="item.label"
      :value="item.value"
      :disabled="item.disabled"
    ></el-option>
  </el-select>
</template>
<script>
export default {
  name: "DictSelect",
  props: {
    type: String,
    disabledList: Array,
    // value: String,
    options: Array,
  },
  data() {
    return {
      // options: [],
    };
  },
  created() {},
  watch: {
    disabledList: {
      handler: function (val, oldVal) {
        if (Array.isArray(val)) {
          this.options.map((e) => {
            e.disabled = false;
            (val || []).map((v) => {
              if (e.value === v) {
                e.disabled = true;
              }
            });
          });
        }
      },
      immediate: true,
    },
  },

  methods: {},
};
</script>
