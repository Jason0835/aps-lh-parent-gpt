
<template>
  <basic-container>
    <div class="form">
      <div class="itemForm">
        {{ $t("common.factory") }}：
        <el-select
          :placeholder="$t('common.rule.select')"
          v-model="search.factoryCode"
          @change="getList"
        >
          <el-option
            v-for="item in this.dict.type.biz_factory_name"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          >
          </el-option>
        </el-select>
      </div>
      <div class="itemForm">
        <el-select
          v-model="search.year"
          placeholder="请选择"
          @change="changeYear"
        >
          <el-option
            v-for="item in yearRange"
            :key="item"
            :label="item"
            :value="item"
          >
          </el-option>
        </el-select>
      </div>
      <div class="itemForm">
        <el-button
          type="primary"
          v-hasPermi="['maindata:mdmWorkCalendar:genAnnualPlan']"
          @click="genYearlPlan"
          :disabled="genDisable"
          :loading="loading"
          >{{ $t("ui.data.workCalendar.genAnnualPlan") }}</el-button
        >
        <el-button
          v-hasPermi="['monthplan:mdmWorkCalendar:import']"
          @click="$refs.tltUpload.handleImport()"
          >{{ $t("ui.frame.btn.import") }}</el-button
        >
      </div>
    </div>
    <div style="display: flex; flex-direction: row">
      <el-tabs tab-position="left" @tab-click="handleClick">
        <el-tab-pane
          v-for="item in selectList"
          :key="item.dictValue"
          :label="item.dictLabel"
        ></el-tab-pane>
      </el-tabs>
      <div style="display: flex; flex: 1">
        <FullCalendar :options="calendarOptions" ref="fullCalendar">
          <template v-slot:eventContent="arg">
            <div style="display: flex; flex-direction: column; width: 100%">
              <div style="text-align: right;color: #cf1322;font-size: 16px;">{{arg.event.extendedProps.holidayNames}}</div>

              <div
                class="cus-event"
                v-if="search.procCode == 1"
                @click="changeDayFlag(arg.event.extendedProps)"
                :style="{
                  background:
                    arg.event.extendedProps.dayFlag == 0
                      ? '#ffebee'
                      : '#e3f2fd',
                }"
              >
                <div class="statusDiv"></div>
                <el-button
                  v-if="arg.event.extendedProps.rate != 100"
                  type="text"
                  @click.stop="showModal(arg.event.extendedProps)"
                  >{{ arg.event.extendedProps.rate + "%" }}</el-button
                >
                <!-- <div class="shift">{{arg.event.extendedProps.randomNum }}</div> -->
              </div>
              <div class="cus-event" v-else>
                <div
                  class="shift"
                  @click="
                    changeShiftFlag(arg.event.extendedProps, 'oneShiftFlag')
                  "
                  :style="{
                    background:
                      arg.event.extendedProps.oneShiftFlag == 0
                        ? '#ffebee'
                        : '#e3f2fd',
                  }"
                >
                  <span
                    :style="{
                      color:
                        arg.event.extendedProps.oneShiftFlag == 0
                          ? '#c62828'
                          : '#1565c0',
                    }"
                    >{{ $t("ui.data.workCalendar.night") }}</span
                  >

                  <!-- <div
                  class="statusDiv"


                ></div> -->
                </div>
                <div
                  class="shift marginDiv"
                  @click="
                    changeShiftFlag(arg.event.extendedProps, 'twoShiftFlag')
                  "
                  :style="{
                    background:
                      arg.event.extendedProps.twoShiftFlag == 0
                        ? '#ffebee'
                        : '#e3f2fd',
                  }"
                >
                  <span
                    :style="{
                      color:
                        arg.event.extendedProps.twoShiftFlag == 0
                          ? '#c62828'
                          : '#1565c0',
                    }"
                    >{{ $t("ui.data.workCalendar.morning") }}</span
                  >

                  <!-- <div
                  class="statusDiv"

                  :style="{
                    background:
                      arg.event.extendedProps.twoShiftFlag == 0
                        ? 'red'
                        : '#fff',
                  }"
                ></div> -->
                </div>
                <div
                  class="shift"
                  @click="
                    changeShiftFlag(arg.event.extendedProps, 'threeShiftFlag')
                  "
                  :style="{
                    background:
                      arg.event.extendedProps.threeShiftFlag == 0
                        ? '#ffebee'
                        : '#e3f2fd',
                  }"
                >
                  <span
                    :style="{
                      color:
                        arg.event.extendedProps.threeShiftFlag == 0
                          ? '#c62828'
                          : '#1565c0',
                    }"
                    >{{ $t("ui.data.workCalendar.noon") }}</span
                  >

                  <!--
                <div
                  class="statusDiv"

                  :style="{
                    background:
                      arg.event.extendedProps.threeShiftFlag == 0
                        ? 'red'
                        : '#fff',
                  }"
                ></div> -->
                </div>
              </div>
            </div>
          </template>
        </FullCalendar>
      </div>
    </div>

    <infoDialog ref="infoRef" @success="getList" />
    <el-dialog
      :title="$t('ui.data.workCalendar.adjustTitle')"
      :visible="visible"
      width="400px"
      @close="hide"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      :append-to-body="true"
    >
      <el-input
        :placeholder="$t('common.rule.input')"
        type="number"
        v-model="actionRate"
        min="1"
        max="100"
      ></el-input>
      <template slot="footer">
        <el-button @click="hide">{{
          this.$t("common.button.cancel")
        }}</el-button>
        <el-button
          type="primary"
          :loading="loading"
          @click="changeRateNumber"
          >{{ this.$t("common.button.confirm") }}</el-button
        >
      </template>
    </el-dialog>
     <tlt-upload
      ref="tltUpload"
      downloadUrl="/maindata/mdmWorkCalendar/importTemplate"
      uploadUrl="/maindata/mdmWorkCalendar/importData"
      @uploadSuccess="getList"
    />
  </basic-container>
</template>
<script>
//lib
import moment from "moment";
import FullCalendar from "@fullcalendar/vue";
import dayGridPlugin from "@fullcalendar/daygrid";
import interactionPlugin from "@fullcalendar/interaction";
//utils
import { downloadLink } from "@/utils/request";
//interface
import {
  mdmWorkCalendar,
  genAnnualPlan,
  selectProcCodeList,
  editAnnualPlan,
} from "@/api/newPage/workCalendar.js";
//components
import tltUpload from "@/components/tltUpload/tltUpload.vue";

import infoDialog from "./components/infoDialog.vue";

export default {
  name: "PersonTrainSetting",
  components: {
    tltUpload,
    infoDialog,
    FullCalendar,
  },
  dicts: [
    "biz_factory_name",
    "molding_method",
    "biz_personnel_type",
    "work_calendar_proc",
  ],
  provide() {
    return {
      parentDict: this.dict,
    };
  },
  data() {
    return {
      yearRange: [],
      visible: false,
      loading: false,
      genDisable: false,
      actionData: {},
      actionRate: 0,
      data: [],
      selection: [],
      page: {
        current: 1,
        pageSize: 20,
        total: 0,
      },
      sort: {},
      search: {
        factoryCode: "116",
        procCode: "01",
        year: "",
      },
      query: {},
      selectList: [],

      calendarOptions: {
        plugins: [dayGridPlugin, interactionPlugin],
        initialView: "dayGridMonth",
        locale: this.$i18n.locale == "zh_CN" ? "zh-cn" : "vi",
        contentHeight: "auto",
        // weekends: false,
        events: [],
        datesSet: this.handleDatesSet,
        eventClick: this.handleEventClick,
        dateClick: this.handleDateClick,
        validRange: function (nowDate) {
          return {
            start: moment().startOf("year").format("YYYY-MM-DD"),
            end: moment()
              .add(12, "months")
              .startOf("year")
              .format("yyyy-MM-DD"),
          };
        },
        buttonText: {
          today: this.$t("ui.data.workCalendar.today"),
          // month: "月",
          // week: "周",
          // day: "日",
        },
      },
      dateList: [],
    };
  },
  computed: {},
  methods: {
    initYearRange() {
      const currentYear = new Date().getFullYear();
      // for (let i = currentYear - 1; i <= currentYear + 1; i++) {
      //   this.yearRange.push(i);
      // }
      for (let i = currentYear - 10; i <= currentYear + 10; i++) {
        this.yearRange.push(i);
      }
    },
    // 跳转到特定年份
    changeYear() {
      this.getList();
      const calendarApi = this.$refs.fullCalendar.getApi();
      calendarApi.gotoDate(this.search.year + "-01-01");
      this.calendarOptions.validRange = {
        start: this.search.year + "-01-01",
        end: this.search.year + 1 + "-01-01",
      };
    },
    hasPermission(permission) {
      const permissions = this.$store.state.user.permissions || [];
      if (Array.isArray(permission)) {
        return permission.some((perm) => permissions.includes(perm));
      }
      return permissions.includes(permission);
    },
    handleClick(tab, event) {
      let obj = this.selectList.find((item) => item.dictLabel == tab.label);
      this.search.procCode = obj.dictValue;
      this.getList();
    },
    changeShiftFlag(info, type) {
      if (!this.hasPermission("maindata:mdmWorkCalendar:edit")) {
        return; // 直接返回，不执行后续逻辑
      }
      let obj = JSON.parse(JSON.stringify(info));
      obj.id = obj.editId;
      let title = "";
      if (type == "oneShiftFlag") {
        title =
          obj.oneShiftFlag == 0
            ? this.$t("ui.data.workCalendar.isNightStart")
            : this.$t("ui.data.workCalendar.isNightStop");
        obj.oneShiftFlag = obj.oneShiftFlag == 0 ? 1 : 0;
      }
      if (type == "twoShiftFlag") {
        title =
          obj.twoShiftFlag == 0
            ? this.$t("ui.data.workCalendar.isMorningStart")
            : this.$t("ui.data.workCalendar.isMorningStop");
        obj.twoShiftFlag = obj.twoShiftFlag == 0 ? 1 : 0;
      }
      if (type == "threeShiftFlag") {
        title =
          obj.threeShiftFlag == 0
            ? this.$t("ui.data.workCalendar.isNoongStart")
            : this.$t("ui.data.workCalendar.isNoongStop");
        obj.threeShiftFlag = obj.threeShiftFlag == 0 ? 1 : 0;
      }
      this.$confirm(title, {
        type: "warning",
      }).then(() => {
        editAnnualPlan(obj).then((data) => {
          this.$modal.msgSuccess(data.msg);
          this.$set(this.page, "current", 1);
          this.getList();
        });
        // removeProductMoldingLimit({ ids }).then((data) => {
        //   this.$modal.msgSuccess(data.msg);
        //   this.$set(this.page, "current", 1);
        //   this.getList();
        // });
      });
    },
    handleDatesSet(info) {
      console.log("当前视图范围:", info);
    },
    hide() {
      this.visible = false;
    },
    changeDayFlag(info) {
      if (!this.hasPermission("maindata:mdmWorkCalendar:edit")) {
        return; // 直接返回，不执行后续逻辑
      }
      let obj = JSON.parse(JSON.stringify(info));
      obj.id = obj.editId;
      this.$confirm(
        obj.dayFlag == 1
          ? this.$t("ui.data.workCalendar.isStop")
          : this.$t("ui.data.workCalendar.isStart"),
        {
          type: "warning",
        }
      ).then(() => {
        obj.dayFlag = obj.dayFlag == 0 ? 1 : 0;

        editAnnualPlan(obj).then((data) => {
          this.$modal.msgSuccess(data.msg);
          this.$set(this.page, "current", 1);
          this.getList();
        });
      });
    },
    showModal(info) {
      if (!this.hasPermission("maindata:mdmWorkCalendar:edit")) {
        return; // 直接返回，不执行后续逻辑
      }
      this.actionData = info;
      this.actionRate = info.rate + "";
      this.visible = true;
    },
    handleEventClick(info) {
      const scheduleDate = info.event.extendedProps.scheduleDate;
      if (moment().isAfter(scheduleDate)) {
        return;
      }

      if (this.$refs.infoRef) {
        const filter = this.calendarOptions.events.filter((row) => {
          return row.scheduleDate === scheduleDate;
        });

        // this.$refs.infoRef.show(
        //   filter.length
        //     ? filter.map((row) => {
        //         return {
        //           ...row,
        //           mouldMethod: row.mouldMethod + "",
        //         };
        //       })
        //     : null,
        //   scheduleDate
        // );
      }
    },
    handleDateClick(info) {
      if (this.search.procCode != 1) {
        return;
      }
      const result = this.dateList.find(
        (item) => item.calendarTime == info.dateStr
      );

      this.showModal(result);
      // if (moment().isAfter(info.dateStr)) {
      //   return;
      // }
    },

    handleExport() {
      downloadLink(
        "/monthplan/mdmPersonLevel/export",
        this.formatParams(false)
      );
    },

    // utils
    formatParams(hasPage = true) {
      const params = {
        ...this.query,
        ...this.sort,
      };

      if (hasPage) {
        params.pageSize = this.page.pageSize;
        params.pageNum = this.page.current;
      }

      if (params.createTime && params.createTime[0]) {
        params.createTimeStart = params.createTime[0];
        params.createTimeEnd = params.createTime[1];
        params.createTime = undefined;
      }

      return params;
    },
    // api
    async getList() {
      try {
        let res = await mdmWorkCalendar(this.search);
        if (res.rows.length == 0) {
          this.genDisable = false;
        } else {
          this.genDisable = true;
        }
        let items = [];
        for (let i = 0; i < res.rows.length; i++) {
          let obj = res.rows[i];
          obj.start = new Date(res.rows[i].calendarTime);
          obj.editId = obj.id;
          items.push(obj);
        }
        this.dateList = items;
        this.$set(this.calendarOptions, "events", items);
        // this.page.total = data.total;
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
    },
    async getCodeList() {
      try {
        let res = await selectProcCodeList();
        console.log(res);
        this.selectList = res;
        this.search.procCode = res[0].dictValue;
        this.getList();
      } catch (err) {}
    },
    async genYearlPlan() {
      try {
        this.loading = true;
        let res = await genAnnualPlan(this.search);
        this.getList();
      } catch (err) {
      } finally {
        this.loading = false;
      }
    },
    async changeRateNumber() {
      try {
        if (
          this.actionRate < 1 ||
          this.actionRate > 100 ||
          this.actionRate.includes(".") ||
          isNaN(this.actionRate)
        ) {
          this.$modal.msgError(this.$t("ui.data.workCalendar.peleaseInteger"));
          return;
        }
        const processedData = JSON.parse(JSON.stringify(this.actionData));
        processedData.id = processedData.editId;
        processedData.rate = this.actionRate;
        let res = await editAnnualPlan(processedData);
        this.$modal.msgSuccess(res.msg);
        this.getList();
        this.hide();
      } catch (err) {
        console.log(err);
      }
    },
  },
  created() {
    const now = new Date();
    const year = now.getFullYear();
    const month = now.getMonth();
    const daysInMonth = new Date(year, month + 1, 0).getDate();
    this.search.year = year;
    this.initYearRange();
    this.getCodeList();
  },
  activated() {},
};
</script>
<style lang="scss" scoped>
.statusDiv {
  width: 30px;
  height: 30px;
  cursor: pointer;
}
.basic-container {
  position: relative;
}
.form {
  display: flex;
  flex-direction: row;
  align-items: center;
  position: absolute;
  top: 20px;
  right: 200px;
}
.itemForm {
  margin-right: 30px;
}
.more-btn {
  margin: 2px 0;
  width: 100%;
}
.cus-event {
  width: 100%;
  padding: 8px;
  // box-sizing: border-box;
  // background-color: #f8f9fa;
  // border-radius: 4px;
  // box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: space-between;
}

/* 标题样式 */
.title {
  width: 100%;
  font-size: 16px;
  font-weight: 600;
  text-align: center;
  color: #333;
  margin-bottom: 8px;
  padding-bottom: 4px;
  border-bottom: 1px solid #eaeaea;
}

/* 班次信息样式 */
.shift {
  font-size: 14px;
  line-height: 1.5;
  padding: 2px 0;
  display: flex;
  flex-direction: column;
  flex: 1;
  align-items: center;
  justify-content: center;
  height: 60px;
}
.marginDiv {
  margin: 0;
}
/* 鼠标悬停效果 */
// .cus-event:hover {
//   background-color: #e9ecef;
//   transition: background-color 0.3s ease;
// }

/* 响应式调整 */
@media (max-width: 768px) {
  .title {
    font-size: 14px;
  }
  .shift {
    font-size: 12px;
  }
}
</style>
