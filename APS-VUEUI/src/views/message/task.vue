<!--
 * @Description: 待办 页面
 * @Author: qy
 * @Date: 2024/3/1
-->
<template>
  <my-task />
</template>

<script>
import MyTask from "@/components/Task/MyTask";
import ApprovalTask from "@/components/Task/ApprovalTask";
export default {
  name:"/messagetask",
  components: {
    MyTask,
    ApprovalTask
  }
}
</script>

<style scoped>

</style>
