<template>
  <div class="basic-container">
    <slot></slot>
  </div>
</template>
<style lang="scss" scoped>
.basic-container {
  width: 100%;
  height: calc(100vh - 80px);
  min-height: 720px;
  overflow-y: scroll;
  padding: 10px;
  box-sizing: border-box;
}
</style>
