
<template>
  <basic-container>
    <FullCalendar :options="calendarOptions">
      <template v-slot:eventContent="arg">
        <div class="cus-event">
          <div class="title">
            {{
              selectDictLabel(
                dict.type.molding_method,
                arg.event.extendedProps.mouldMethod
              )
            }}
          </div>
          <div class="shift">
            早班： {{ arg.event.extendedProps.quotaClass1 }}
          </div>
          <div class="shift">
            晚班： {{ arg.event.extendedProps.quotaClass2 }}
          </div>
        </div>
      </template>
    </FullCalendar>
    <infoDialog ref="infoRef" @success="getList" />
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
  listCxPersionTrainSetting,
  saveCxPersionTrainSetting,
} from "@/api/cx/cxPersionTrainSetting.js";
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
  dicts: ["biz_factory_name", "molding_method", "biz_personnel_type"],
  provide() {
    return {
      parentDict: this.dict,
    };
  },
  data() {
    return {
      loading: false,
      data: [],
      selection: [],
      page: {
        current: 1,
        pageSize: 20,
        total: 0,
      },
      sort: {},
      search: {},
      query: {},
      importDefaultValue: {},
      importRules: {},

      calendarOptions: {
        locale: "zh-cn",
        plugins: [dayGridPlugin, interactionPlugin],
        initialView: "dayGridMonth",
        contentHeight: "auto",
        // weekends: false,
        events: [{ title: "Meeting", start: new Date() }],
        eventClick: this.handleEventClick,
        dateClick: this.handleDateClick,
        validRange: function (nowDate) {
          return {
            start: moment()
              .subtract(1, "months")
              .startOf("month")
              .format("yyyy-MM-DD"),
            end: moment()
              .add(2, "months")
              .startOf("month")
              .format("yyyy-MM-DD"),
          };
        },
        buttonText: {
          today: "今天",
          month: "月",
          week: "周",
          day: "日",
        },
      },
    };
  },
  computed: {},
  methods: {
    handleEventClick(info) {
      // console.log(info);
      const scheduleDate = info.event.extendedProps.scheduleDate;
      if (moment().isAfter(scheduleDate)) {
        return;
      }

      if (this.$refs.infoRef) {
        const filter = this.calendarOptions.events.filter((row) => {
          return row.scheduleDate === scheduleDate;
        });

        this.$refs.infoRef.show(
          filter.length
            ? filter.map((row) => {
                return {
                  ...row,
                  mouldMethod: row.mouldMethod + "",
                };
              })
            : null,
          scheduleDate
        );
      }
    },
    handleDateClick(info) {
      console.log("handleDateClick");
      // console.log(info.dateStr);
      if (moment().isAfter(info.dateStr)) {
        return;
      }

      if (this.$refs.infoRef) {
        const filter = this.calendarOptions.events.filter((row) => {
          // console.log(row.scheduleDate, info.dateStr);
          return row.scheduleDate === info.dateStr;
        });

        this.$refs.infoRef.show(
          filter.length
            ? filter.map((row) => {
                return {
                  ...row,
                  mouldMethod: row.mouldMethod + "",
                };
              })
            : null,
          info.dateStr
        );
      }
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
        this.loading = true;
        const data = await listCxPersionTrainSetting(this.formatParams());
        console.log(data);
        let items = data.rows.map((row) => {
          return {
            ...row,
            start: new Date(row.scheduleDate),
            title: 1,
          };
        });
        console.log(items);
        this.$set(this.calendarOptions, "events", items);

        // this.page.total = data.total;
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
    },
  },
  created() {},
  activated() {
    this.getList();
  },
};
</script>
<style lang="scss" scoped>
.more-btn {
  margin: 2px 0;
  width: 100%;
}
.cus-event {
  width: 100%;
  padding: 8px;
  box-sizing: border-box;
  background-color: #f8f9fa;
  border-radius: 4px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
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
  color: #666;
  line-height: 1.5;
  padding: 2px 0;
}

/* 鼠标悬停效果 */
.cus-event:hover {
  background-color: #e9ecef;
  transition: background-color 0.3s ease;
}

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
