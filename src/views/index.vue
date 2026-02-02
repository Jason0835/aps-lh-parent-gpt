<!--
 * @Description: 首页 页面
 * @Author: qy
 * @Date: 2024/3/12
-->
<template>
  <div class="page flex flex-col">
    <div class="flex w-100">
      <div class="flex-1 flex flex-col flex-shrink-0 mr-13 h-396 border-box">
        <div
          class="title w-100 flex align-items-center justify-content-between"
        >
          <div class="flex align-items-center">
            <svg-icon
              class="icon job-2-icon"
              class-name="job-2-icon"
              icon-class="job-2"
            />{{ $t("任务中心") }}
          </div>
          <!-- <div class="more" @click="$router.push('/messagetask')"> -->
          <div class="more" @click="goTask">
            {{ $t("common.button.more") }}
          </div>
        </div>
        <div class="flex-1 list w-100">
          <div
            v-for="item in taskList"
            :key="item.id"
            class="item flex justify-content-between"
          >
            <div class="flex-1 text-truncate w-0">
              <span class="status success">待办</span>
              <app-link
                v-if="item.billUrl"
                :to="item.billUrl"
                @click.native="handleTaskClick(item)"
                >{{ item.msgContent }}</app-link
              >
              <span v-else @click="handleTaskClick(item)">{{
                item.msgContent
              }}</span>
            </div>
            <div class="right flex-shrink-0">{{ item.sendTime }}</div>
          </div>
          <el-empty v-if="taskList.length === 0" />
        </div>
      </div>
      <div class="flex-1 flex flex-col flex-shrink-0 h-396 border-box">
        <div
          class="title w-100 flex align-items-center justify-content-between"
        >
          <div class="flex align-items-center">
            <svg-icon
              class="icon mes-icon"
              class-name="mes-icon"
              icon-class="mes"
            />{{ $t("common.messageCenter") }}
          </div>
          <!-- <div class="more" @click="$router.push('/messageList')"> -->
          <div class="more" @click="goMessage">
            {{ $t("common.button.more") }}
          </div>
        </div>
        <div class="flex-1 list w-100">
          <div
            v-for="item in messageList"
            :key="item.id"
            class="item flex justify-content-between"
          >
            <div class="flex-1 text-truncate w-0 no-read">
              <el-popover
                placement="bottom"
                title="消息内容"
                width="500"
                trigger="click"
                :content=item.msgContent
                 @show="handleMessageClick(item)"
              >
                <span slot="reference" style="cursor: pointer;">{{
                  item.msgContent
                }}</span>
              </el-popover>
              <!-- <app-link v-if="item.billUrl" :to="item.billUrl"  @click.native="handleMessageClick(item)">{{
                item.msgContent
              }}</app-link>
              <span v-else  @click="handleMessageClick(item)">{{ item.msgContent }}</span> -->
            </div>
            <div class="right flex-shrink-0">{{ item.sendTime }}</div>
          </div>
          <el-empty v-if="messageList.length === 0" />
        </div>
      </div>
    </div>
    <div class="flex-1 flex flex-col flex-shrink-0 border-box w-100 mt-15">
      <div class="title w-100 flex align-items-center justify-content-between">
        <div class="flex align-items-center">
          <svg-icon
            class="icon mr-5"
            class-name="menu-icon"
            icon-class="menu"
          />{{ $t("common.commonFunctionNavigation") }}
        </div>
      </div>
      <div class="flex-1 menu-list w-100">
        <app-link
          v-for="(item, index) in collectMenu"
          :key="item.menuId"
          :to="item.fullPath"
        >
          <div
            class="item flex flex-col justify-content-center align-items-center"
          >
            <i
              class="el-icon el-icon-close close-btn"
              @click.prevent="removeCollectMenu(item)"
            />
            <svg-icon
              class="icon"
              :icon-class="
                item.meta.icon && item.meta.icon != '#'
                  ? item.meta.icon
                  : 'example'
              "
            />
            <div class="text">{{ item.meta.title }}</div>
          </div>
        </app-link>
        <el-empty v-if="collectMenu.length === 0" />
      </div>
    </div>
  </div>
</template>

<script>
import path from "path";
import { mapGetters } from "vuex";
import {
  messageListNoticeMessage,
  messageListTaskMessage,
  readMessage,
  readMessageTask,
} from "@/api/system/message";
import { isExternal } from "@/utils/validate";
import AppLink from "@/layout/components/Sidebar/Link";

export default {
  components: { AppLink },
  data() {
    return {
      messageList: [],
      taskList: [],
      collectPaths: [],
      storeKey: null, // 缓存的local Key
      menuList: [
        {
          text: "内销管理",
          icon: "domestic",
        },
        {
          text: "外销管理",
          icon: "export_sales",
        },
        {
          text: "计划管理",
          icon: "plan",
        },
        {
          text: "订舱管理",
          icon: "booking",
        },
        {
          text: "汇总报表",
          icon: "summary",
        },
        {
          text: "出口操作",
          icon: "export",
        },
        {
          text: "派车单",
          icon: "dispatch",
        },
        {
          text: "单证制作",
          icon: "singly_document",
        },
        {
          text: "财务管理",
          icon: "finance",
        },
        {
          text: "客户信贷",
          icon: "credit",
        },
        {
          text: "基础资料",
          icon: "basic_data",
        },
        {
          text: "系统管理",
          icon: "manage",
        },
        {
          text: "系统监控",
          icon: "monitor-2",
        },
      ],
    };
  },
  computed: {
    // ...mapGetters(['sidebarRouters', 'collectMenu']),
    ...mapGetters(["sidebarRouters", "collectMenu", "name"]),
  },
  mounted() {},
  watch: {
    // 也可以监听权限变化
    "$store.state.user.permissions": {
      handler(permissions) {
        if (permissions && permissions.length > 0) {
          this.loadData();
        }
      },
      immediate: true,
    },
  },
  created() {},
  activated() {
    console.log("activeated");

    // 组件创建时获取数据
  },
  methods: {
    handleTaskClick(row) {
      readMessageTask(row.id).then((response) => {
        // this.getTaskData()
      });
    },
    handleMessageClick(row) {
      readMessage(row.id).then((response) => {
        // this.getMessageData()
      });
    },
    loadData() {
      if (this.hasPermission("message:messageTaskList:list")) {
        this.getTaskData();
      }
      if (this.hasPermission("message:messageList:list")) {
        this.getMessageData();
      }
    },
    goMessage() {
      if (this.hasPermission("message:messageTaskList:list")) {
        this.$router.push("/messageList");
      }
    },
    goTask() {
      if (this.hasPermission("message:messageTaskList:list")) {
        this.$router.push("/messagetask");
      }
    },
    hasPermission(permission) {
      const permissions = this.$store.state.user.permissions || [];
      if (Array.isArray(permission)) {
        return permission.some((perm) => permissions.includes(perm));
      }
      return permissions.includes(permission);
    },
    async getMessageData() {
      const messageData = await messageListNoticeMessage({
        receivedBy: this.name,
        msgType: 0,
      });
      this.messageList = messageData?.rows || [];
    },
    async getTaskData() {
      const taskData = await messageListTaskMessage({
        receivedBy: this.name,
        msgType: 1,
      });
      this.taskList = taskData?.rows || [];
    },
    resolvePath(routePath, routeQuery) {
      if (isExternal(routePath)) {
        return routePath;
      }
      if (isExternal(this.basePath)) {
        return this.basePath;
      }
      if (routeQuery) {
        const query = JSON.parse(routeQuery);
        return { path: path.resolve(this.basePath, routePath), query: query };
      }
      return path.resolve(this.basePath, routePath);
    },
    removeCollectMenu(item) {
      this.$store.dispatch("handleREMOVECollectMenu", item.fullPath);
    },
  },
};
</script>

<style lang="sass" scoped>
.page
  width: 100%
  height: 100%
  padding: 11px 15px 12px 13px
  overflow: auto
.h-396
  height: 396px
.border-box
  border: 1px solid rgba(221,221,221,0.97)
  .title
    height: 40px
    line-height: 40px
    background-color: #E8F9F8
    font-weight: 400
    font-size: 14px
    color: #000000
    padding: 0 13px 0 14px
    .icon
      color: #007F81
      &.job-2-icon
        font-size: 22px
      &.mes-icon
        font-size: 17px
        margin-right: 5px
    .more
      font-weight: 400
      font-size: 14px
      color: #007F81
      position: relative
      padding-right: 15px
      cursor: pointer
      &:after
        content: ''
        position: absolute
        border: 5px solid
        border-color: transparent transparent transparent #007F81
        right: 0
        top: 15px
  .list
    overflow: auto
    padding: 13px 16px
    .item
      line-height: 34px
      font-size: 14px
      color: #333333
      .no-read
        position: relative
        padding-left: 15px
        &:before
          content: ' '
          position: absolute
          width: 5px
          height: 5px
          border-radius: 50%
          background-color: red
          left: 0
          top: 15px
      .status
        padding: 4px 6px
        color: #ffffff
        font-size: 12px
        margin-right: 9px
        &.success
          background-color: #00A86B
        &.error
          background-color: #D91C1C
      .right
        width: 140px
        text-align: right
        font-size: 14px
        color: #999999
  .menu-list
    overflow: auto
    padding: 37px 0 0 37px
    .item
      float: left
      width: 130px
      height: 100px
      background: #F2FEFE
      border-radius: 4px
      border: 1px solid #B7DFDF
      margin-bottom: 58px
      margin-right: 83px
      position: relative
      &:hover
        .close-btn
          display: block
      .close-btn
        position: absolute
        right: 5px
        top: 5px
        display: none
      .icon
        font-size: 24px
        color: #007F81
      .text
        font-weight: 400
        font-size: 14px
        color: #333333
        line-height: initial
        margin-top: 15px
</style>
