<template>
  <el-dialog
    :title="title"
    :visible="visible"
    width="800px"
    @close="hide"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :append-to-body="true"
  >
    <div class="content" v-loading="loading">
      <el-form
        ref="form"
        label-position="right"
        label-width="120px"
        :model="form"
      >
        <el-row type="flex" style="flex-wrap: wrap">
          <!-- 基础信息 -->
          <el-col :span="24">
            <h4 class="form-header h4">
              {{ $t("ui.data.column.scheduleResult.baseInfo") }}
            </h4>
          </el-col>

          <el-col :span="12">
            <el-form-item
              :label="$t('ui.data.column.tqScheduleResult.scheduleDate')"
              prop="scheduleDate"
            >
              <el-input v-model="form.scheduleDate" disabled></el-input>
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item
              :label="$t('ui.data.column.tqScheduleResult.beadCode')"
              prop="beadCode"
            >
              <el-input v-model="form.beadCode" disabled></el-input>
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item
              :label="$t('ui.data.column.tqScheduleResult.machineCode')"
              prop="machineCode"
            >
              <el-input v-model="form.machineCode" disabled></el-input>
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item
              :label="$t('ui.data.column.tqScheduleResult.releaseStatus')"
              prop="releaseStatus"
            >
              <dict-select
                v-model="form.releaseStatus"
                :options="parentDict.type.IS_RELEASE"
                disabled
              />
            </el-form-item>
          </el-col>

          <el-col :span="24">
            <el-form-item :label="$t('ui.common.column.remark')" prop="remark">
              <el-input
                type="textarea"
                v-model="form.remark"
                maxlength="200"
              ></el-input>
            </el-form-item>
          </el-col>

          <!-- 6个班次区域 -->
          <el-col :span="24" v-for="shiftIndex in 6" :key="shiftIndex">
            <h4 class="form-header h4">
              {{ getShiftLabel(shiftIndex) }}
            </h4>
          </el-col>

          <template v-for="shiftIndex in 6">
            <el-col :span="12" :key="'plan-' + shiftIndex">
              <el-form-item
                :label="$t('ui.data.column.scheduleResult.plan')"
                :prop="'class' + shiftIndex + 'PlanQty'"
              >
                <el-input-number
                  class="w100"
                  v-model="form['class' + shiftIndex + 'PlanQty']"
                  :disabled="shiftDisabledMap[shiftIndex]"
                  :min="0"
                  :max="maxPlanQty"
                  :precision="0"
                  controls-position="right"
                ></el-input-number>
              </el-form-item>
            </el-col>

            <el-col :span="12" :key="'finish-' + shiftIndex">
              <el-form-item
                :label="$t('ui.data.column.scheduleResult.finish')"
                :prop="'class' + shiftIndex + 'FinishQty'"
              >
                <el-input
                  v-model="form['class' + shiftIndex + 'FinishQty']"
                  disabled
                ></el-input>
              </el-form-item>
            </el-col>

            <el-col :span="12" :key="'analysis-' + shiftIndex">
              <el-form-item
                :label="$t('ui.data.column.scheduleResult.analysis')"
                :prop="'class' + shiftIndex + 'Analysis'"
              >
                <el-input
                  v-model="form['class' + shiftIndex + 'Analysis']"
                  :disabled="shiftDisabledMap[shiftIndex]"
                  maxlength="66"
                ></el-input>
              </el-form-item>
            </el-col>
          </template>
        </el-row>
      </el-form>
    </div>
    <template slot="footer">
      <el-button @click="hide">{{ $t("common.button.cancel") }}</el-button>
      <el-button type="primary" :loading="loading" @click="handleConfirm">{{
        $t("common.button.confirm")
      }}</el-button>
    </template>
  </el-dialog>
</template>

<script>
import moment from "moment";
import { changeQty, listScheduleShiftDates } from "@/api/tq/scheduleResult";

export default {
  inject: ["parentDict"],
  data() {
    return {
      // MySQL int 最大值，前端计划量输入需限制在该范围内
      maxPlanQty: 2147483647,
      loading: false,
      visible: false,
      form: {},
      // 班次日期列表
      dateList: this.getDefaultDateList(),
      // 班次是否禁用（历史班次不可编辑）
      shiftDisabledMap: {
        1: false,
        2: false,
        3: false,
        4: false,
        5: false,
        6: false,
      },
    };
  },
  computed: {
    title() {
      return this.$t("ui.data.btn.tqScheduleResult.adjustQty");
    },
  },
  methods: {
    /** 获取默认班次日期列表 */
    getDefaultDateList() {
      return [
        { shift: 1, shiftType: "afternoon", shiftDate: "" },
        { shift: 2, shiftType: "night", shiftDate: "" },
        { shift: 3, shiftType: "morning", shiftDate: "" },
        { shift: 4, shiftType: "afternoon", shiftDate: "" },
        { shift: 5, shiftType: "night", shiftDate: "" },
        { shift: 6, shiftType: "morning", shiftDate: "" },
      ];
    },

    /** 班次名称映射 */
    shiftPeriodName(shiftType) {
      const map = {
        night: this.$t("ui.data.column.scheduleResult.nightShift"),
        morning: this.$t("ui.data.column.scheduleResult.morningShift"),
        afternoon: this.$t("ui.data.column.scheduleResult.middleShift"),
      };
      return map[shiftType] || "";
    },

    /** 获取班次标题 */
    getShiftLabel(shiftIndex) {
      const item = this.dateList[shiftIndex - 1];
      if (!item) return "";
      const shiftName = this.shiftPeriodName(item.shiftType);
      return shiftName + " " + (item.shiftDate || "");
    },

    /** 获取班次日期列表 */
    async fetchScheduleShiftDates(scheduleDate) {
      if (!scheduleDate) {
        this.dateList = this.getDefaultDateList();
        return;
      }
      try {
        const res = await listScheduleShiftDates({ scheduleDateQuery: scheduleDate });
        if (Array.isArray(res) && res.length) {
          this.dateList = res;
        } else {
          this.dateList = this.getDefaultDateList();
        }
      } catch (error) {
        console.error(error);
        this.dateList = this.getDefaultDateList();
      }
    },

    /**
     * 根据排程日期和班次索引推导班次结束时间
     * 胎圈排程6班次时间窗口：
     * 1班：D日中班(16:00-24:00)
     * 2班：D+1日夜班(00:00-08:00)
     * 3班：D+1日早班(08:00-16:00)
     * 4班：D+1日中班(16:00-24:00)
     * 5班：D+2日夜班(00:00-08:00)
     * 6班：D+2日早班(08:00-16:00)
     * D = 排程日期 - 2
     */
    resolveShiftEndTime(scheduleDate, shiftIndex) {
      if (!scheduleDate) return null;
      // D = 排程日期 - 2
      const dDay = moment(scheduleDate).subtract(2, "days");
      switch (shiftIndex) {
        case 1:
          // 1班：D日中班结束时间=D日24:00
          return dDay.clone().endOf("day");
        case 2:
          // 2班：D+1日夜班结束时间=D+1日08:00
          return dDay.clone().add(1, "days").startOf("day").add(8, "hours");
        case 3:
          // 3班：D+1日早班结束时间=D+1日16:00
          return dDay.clone().add(1, "days").startOf("day").add(16, "hours");
        case 4:
          // 4班：D+1日中班结束时间=D+1日24:00
          return dDay.clone().add(1, "days").endOf("day");
        case 5:
          // 5班：D+2日夜班结束时间=D+2日08:00
          return dDay.clone().add(2, "days").startOf("day").add(8, "hours");
        case 6:
          // 6班：D+2日早班结束时间=D+2日16:00
          return dDay.clone().add(2, "days").startOf("day").add(16, "hours");
        default:
          return null;
      }
    },

    /**
     * 更新各班次是否禁用（历史班次不可编辑）
     */
    updateShiftDisabled(scheduleDate) {
      const now = moment();
      for (let i = 1; i <= 6; i++) {
        const endTime = this.resolveShiftEndTime(scheduleDate, i);
        if (!endTime) {
          this.$set(this.shiftDisabledMap, i, true);
          continue;
        }
        this.$set(this.shiftDisabledMap, i, !endTime.isValid() || now.isAfter(endTime));
      }
    },

    /** 前端校验：非历史班次计划量不能小于完成量 */
    validatePlanQtyByFinishQty() {
      for (let i = 1; i <= 6; i++) {
        if (this.shiftDisabledMap[i]) {
          continue;
        }
        const planQty = Number(this.form["class" + i + "PlanQty"] || 0);
        const finishQty = Number(this.form["class" + i + "FinishQty"] || 0);
        if (finishQty > 0 && planQty < finishQty) {
          this.$modal.msgError(
            this.$t(
              "ui.data.column.scheduleResult.planGreaterThanFinishWhenFinishPositive",
              { shift: i }
            )
          );
          return false;
        }
      }
      return true;
    },

    /** 保存调量 */
    async save(params) {
      try {
        this.loading = true;
        // 后端 changeQty 接口内部已执行 validateChangeQty 校验，无需重复调用
        const res = await changeQty(params);
        this.loading = false;
        this.$modal.msgSuccess(this.$t("common.msg.ajax.operation.success"));
        this.$emit("success");
        this.hide();
      } catch (error) {
        console.error(error);
        this.loading = false;
      }
    },

    /** 打开弹窗 */
    async show(data) {
      this.visible = true;
      if (data) {
        // 格式化排程日期用于显示
        const scheduleDateStr = data.scheduleDate
          ? moment(data.scheduleDate).format("YYYY-MM-DD")
          : "";

        this.form = {
          ...data,
          scheduleDate: scheduleDateStr,
        };

        // 获取班次日期列表
        await this.fetchScheduleShiftDates(scheduleDateStr);
        // 更新班次禁用状态
        this.updateShiftDisabled(scheduleDateStr);
      }
    },

    /** 关闭弹窗 */
    hide() {
      this.form = {};
      this.dateList = this.getDefaultDateList();
      this.shiftDisabledMap = {
        1: false,
        2: false,
        3: false,
        4: false,
        5: false,
        6: false,
      };
      if (this.$refs.form) {
        this.$refs.form.resetFields();
      }
      this.visible = false;
    },

    /** 确认按钮 */
    handleConfirm() {
      this.$refs.form.validate((valid) => {
        if (valid) {
          if (!this.validatePlanQtyByFinishQty()) {
            return;
          }
          this.save(this.form);
        }
      });
    },
  },
};
</script>

<style lang="scss" scoped>
.content {
  width: 100%;
  height: 100%;
  overflow: auto;
}

.w100 {
  width: 100%;
}
</style>
