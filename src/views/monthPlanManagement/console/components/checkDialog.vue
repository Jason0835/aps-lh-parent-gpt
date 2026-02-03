<template>
  <el-dialog
    title="检查配置项"
    :visible="visible"
    width="800px"
    @close="hide"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :append-to-body="true"
  >
    <div class="check-container">
      <!-- 进度统计 -->
      <div class="progress-stat">
        <div class="stat-item">
          <div class="stat-label">总检查项</div>
          <div class="stat-value total">{{ checkItems.length }}</div>
        </div>
        <div class="stat-item">
          <div class="stat-label">已通过</div>
          <div class="stat-value success">{{ passedCount }}</div>
        </div>
        <div class="stat-item">
          <div class="stat-label">未通过</div>
          <div class="stat-value failed">{{ failedCount }}</div>
        </div>
      </div>

      <!-- 进度条 -->
      <div class="progress-bar">
        <el-progress
          :percentage="progressPercentage"
          :status="progressStatus"
          :stroke-width="12"
          :show-text="false"
        ></el-progress>
        <div class="progress-text">
          {{ progressText }}
        </div>
      </div>

      <!-- 检查项列表 -->
      <div class="check-list">
        <div
          v-for="(item, index) in checkItems"
          :key="item.id"
          class="check-item"
          :class="getItemClass(item.status)"
        >
          <div class="item-index">{{ index + 1 }}</div>

          <div class="item-icon">
            <i
              v-if="item.status === 'passed'"
              class="el-icon-success success-icon"
            ></i>
            <i
              v-else-if="item.status === 'failed'"
              class="el-icon-error failed-icon"
            ></i>
            <i
              v-else-if="item.status === 'checking'"
              class="el-icon-loading checking-icon"
            ></i>
            <i v-else class="el-icon-minus pending-icon"></i>
          </div>

          <div class="item-content">
            <div class="item-title">{{ item.label }}</div>
          </div>

          <div class="item-status">
            <span :class="getStatusClass(item.status)">
              {{ getStatusText(item.status) }}
            </span>
          </div>
        </div>
      </div>

      <!-- 操作按钮 -->
      <div class="action-buttons">
        <el-button @click="hide">{{
          this.$t("common.button.cancel")
        }}</el-button>
        <el-button
          @click="handleConfirm"
          :disabled="!allPassed || checking"
          :loading="confirming"
          type="primary"
        >
          {{ confirmText }}
        </el-button>
      </div>
    </div>
    <!-- <template slot="footer">
      <el-button @click="hide">{{ this.$t("common.button.cancel") }}</el-button>
      <el-button type="primary" :loading="loading" @click="handleConfirm">{{
        this.$t("common.button.confirm")
      }}</el-button>
    </template> -->
  </el-dialog>
</template>

<script>
import moment from "moment";

import infoForm from "@/views/components/infoForm.vue";

import { factoryWholeCourseProduction,checkProductionDemandPlan } from "@/api/factory/console";
export default {
  components: { infoForm },
  inject: ["parentDict"],
  data() {
    return {
      loading: false,
      visible: false,
      dialogVisible: false,
      checking: false,
      confirming: false,
      actionData:{},

      checkItems: []
    };
  },
  computed: {
    // 通过的数量
    passedCount() {
      return this.checkItems.filter((item) => item.status == true).length;
    },

    // 失败的数量
    failedCount() {
      return this.checkItems.filter((item) => item.status == false).length;
    },

    // 检查中的数量
    checkingCount() {
      return this.checkItems.filter((item) => item.status === "checking")
        .length;
    },

    // 已检查的数量
    checkedCount() {
      return this.checkItems.filter(
        (item) => item.status === "passed" || item.status === "failed"
      ).length;
    },

    // 是否有失败的检查项
    hasFailed() {
      return this.failedCount > 0;
    },

    // 是否全部通过
    allPassed() {
      return this.checkItems.every((item) => item.status === "passed");
    },

    // 进度百分比
    progressPercentage() {
      const total = this.checkItems.length;
      const checked = this.checkedCount;
      return Math.round((checked / total) * 100);
    },

    // 进度条状态
    progressStatus() {
      if (this.hasFailed) return "exception";
      if (this.checking) return "";
      return "success";
    },

    // 进度文本
    progressText() {
      if (this.checking) return "正在检查中...";
      if (this.allPassed)
        return `全部检查通过 (${this.passedCount}/${this.checkItems.length})`;
      return `已检查 ${this.checkedCount}/${this.checkItems.length}`;
    },

    // 确认按钮文本
    confirmText() {
      if (this.confirming) return "确认中...";
      if (this.checking) return "检查中...";
      if (this.allPassed)
        return `确认 (${this.passedCount}/${this.checkItems.length})`;
      return `确认 (${this.passedCount}/${this.checkItems.length})`;
    },
  },
  methods: {
    // 获取检查项CSS类
    getItemClass(status) {
      return {
        "check-item-passed": status == true,
        "check-item-failed": status ==false,
        "check-item-checking": status === "checking",
      };
    },

    // 获取状态CSS类
    getStatusClass(status) {
      return {
        "status-passed": status == true,
        "status-failed": status  ==false,
        "status-checking": status === "checking",
      };
    },

    // 获取状态文本
    getStatusText(status) {
      const statusMap = {
        true: "已通过",
        false: "未通过",
        checking: "检查中",
      };
      return statusMap[status] || status;
    },
    // api
    async save(params) {
      try {
        this.loading = true;

        const res = await factoryWholeCourseProduction(params);
        this.$modal.msgSuccess(res.msg);
        this.$emit("success");
        this.hide();
        this.loading = false;
      } catch (error) {
        console.log(error);
        this.loading = false;
      }
    },
    async startCheck(data){
      try{
        let res=await checkProductionDemandPlan(data)
        console.log(res)
      }catch(err){
        console.log(err)
      }
    },
    //utils
    show(data) {
      this.visible = true;
      this.actionData=data
      console.log(this.parentDict.type.check_item_type.length)
      let list=this.parentDict.type.check_item_type
      for (let i = 0; i < list.length; i++) {
        list[i].status='checking'

      }
      this.checkItems=list
      this.startCheck(data)

    },
    hide() {
      this.form = {};

      this.visible = false;
    },
    numberEmpty(val) {
      return this.isEmpty(val) ? undefined : val;
    },

    handleConfirm() {
      this.$refs.form.triggerConfirm(async (params) => {
        if (params.yearMonth) {
          let arr = params.yearMonth.split("-");
          params.year = arr[0];
          params.month = arr[1];
        }

        Object.keys(params).forEach((key) => {
          if (this.isEmpty(params[key])) {
            params[key] = "";
          }
        });

        this.save(params);
      });
    },
  },
};
</script>
<style scoped>
.check-container {
  padding: 10px 0;
}

/* 进度统计 */
.progress-stat {
  display: flex;
  justify-content: space-between;
  margin-bottom: 20px;
  padding: 0 20px;
}

.stat-item {
  text-align: center;
  flex: 1;
}

.stat-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  margin: 4px 0;
}

.stat-value.total {
  color: #303133;
}

.stat-value.success {
  color: #67c23a;
}

.stat-value.failed {
  color: #f56c6c;
}

.stat-value.checking {
  color: #409eff;
}

/* 进度条 */
.progress-bar {
  padding: 0 20px;
  margin-bottom: 20px;
}

.progress-text {
  text-align: center;
  margin-top: 8px;
  color: #606266;
  font-size: 14px;
}

/* 检查项列表 */
.check-list {
  max-height: 400px;
  overflow-y: auto;
  margin-bottom: 20px;
  padding: 0 20px;
}

.check-item {
  display: flex;
  align-items: center;
  padding: 12px;
  margin-bottom: 8px;
  border-radius: 4px;
  border: 1px solid #ebeef5;
  transition: all 0.3s;
}

.check-item:hover {
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.check-item-passed {
  background-color: #f0f9eb;
  border-color: #e1f3d8;
}

.check-item-failed {
  background-color: #fef0f0;
  border-color: #fde2e2;
}

.check-item-checking {
  background-color: #f0f9ff;
  border-color: #d9ecff;
}

.check-item-pending {
  background-color: #fafafa;
}

.item-index {
  width: 24px;
  height: 24px;
  line-height: 24px;
  text-align: center;
  background: #f5f7fa;
  border-radius: 12px;
  margin-right: 12px;
  color: #909399;
  font-size: 12px;
  flex-shrink: 0;
}

.item-icon {
  margin-right: 12px;
  font-size: 18px;
  flex-shrink: 0;
}

.success-icon {
  color: #67c23a;
}

.failed-icon {
  color: #f56c6c;
}

.checking-icon {
  color: #409eff;
  animation: rotating 2s linear infinite;
}

.pending-icon {
  color: #909399;
}

.item-content {
  flex: 1;
  min-width: 0;
}

.item-title {
  font-weight: 500;
  color: #303133;
  margin-bottom: 4px;
  font-size: 14px;
}

.item-desc {
  font-size: 12px;
  color: #909399;
}

.item-status {
  margin-left: 10px;
  flex-shrink: 0;
}

.item-status span {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 10px;
}

.status-passed {
  background-color: #67c23a;
  color: white;
}

.status-failed {
  background-color: #f56c6c;
  color: white;
}

.status-checking {
  background-color: #409eff;
  color: white;
}

.status-pending {
  background-color: #909399;
  color: white;
}

/* 操作按钮 */
.action-buttons {
  display: flex;
  justify-content: flex-end;
  padding: 20px 20px 0;
  border-top: 1px solid #ebeef5;
}

/* 加载动画 */
@keyframes rotating {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

/* 滚动条样式 */
.check-list::-webkit-scrollbar {
  width: 6px;
}

.check-list::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

.check-list::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

.check-list::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}
</style>