<template>
  <el-button type="text" v-bind="$attrs" v-on="$listeners"
    ><slot></slot
  ></el-button>
</template>
<script>
export default {
  name: "TextButton",
};
</script>
