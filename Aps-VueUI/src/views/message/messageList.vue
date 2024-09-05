<!--
 * @Description: messageList 页面
 * @Author: qy
 * @Date: 2024/3/1
-->
<template>
  <message-list />
</template>

<script>
import MessageList from "@/components/Message/MessageList";
import EmailMessage from "@/components/Message/EmailMessage";
export default {
  name:"/messageList",
  components: {
    MessageList,
    EmailMessage
  }
}
</script>

<style scoped>

</style>
