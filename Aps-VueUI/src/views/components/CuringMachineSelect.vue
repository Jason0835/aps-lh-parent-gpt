<template>
  <el-select
    class="w100"
    :disabled="this.disabled"
    @visible-change="this.handleVisibleChange"
    v-bind="$attrs"
    v-on="$listeners"
    :loading="loading"
    :value="value"
    @change="handleChange"
  >
    <el-option
      v-if="data.length == 0 && !!label"
      :key="value"
      :value="value"
      :label="label"
    />
    <el-option
      v-for="el in data"
      :key="el.machineCode"
      :value="el.machineCode"
      :label="el.machineName"
    />
  </el-select>
</template>
<script>
import { listMachine } from "@/api/lh/machine";
export default {
  model: {
    prop: "value",
    change: "updateValue",
  },
  props: {
    options: {
      type: Array | Boolean,
      default: false,
    },
    value: String | Number,
    title: String,
    disabled: Boolean,
    label: String,
  },
  data() {
    return {
      loading: false,
      cacheData: [],
    };
  },
  computed: {
    data: function () {
      let that = this;
      if (this.options !== false) {
        if (!Array.isArray(this.options)) {
          console.warn("factory select options is error");

          return [];
        }
        return this.options;
      } else {
        return this.cacheData;
      }
    },
  },
  methods: {
    handleChange(val, row) {
      this.$emit("updateValue", val);
    },
    handleVisibleChange(val) {
      // console.log(val, this.cacheData.length == 0);
      if (val && this.cacheData.length == 0) {
        this.getData();
      }
    },
    async getData() {
      try {
        this.loading = true;
        const response = await listMachine();
        this.cacheData = response.rows;
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
    },
  },
  // render() {
  //   return (
  //     <el-select
  //       v-model={this.value}
  //       disabled={this.disabled}
  //       onVisibleChange={this.handleVisibleChange}
  //       props={{ onVisibleChange:this.handleVisibleChange,...this.$props }}
  //     >
  //       {this.data.map((el) => {
  //         return (
  //           <el-option
  //             key={el.machineCode}
  //             value={el.machineCode}
  //             label={el.machineName}
  //           />
  //         );
  //       })}
  //     </el-select>
  //   );
  // },
};
</script>

<style>
</style>
