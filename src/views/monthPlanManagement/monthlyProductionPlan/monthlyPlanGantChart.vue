
<template>
  <basic-container>
    <el-card class="top-el-card" shadow="never">
      <div style="height: 30px">
        <el-form :inline="true" style="display: contents">
          <el-form-item label="月度">
            <el-date-picker
              v-model="scheduleDate"
              type="month"
              value-format="yyyy-MM"
              :clearable="false"
            />
          </el-form-item>
        </el-form>
        <el-button @click="handleSearch">{{ $t("搜索") }}</el-button>
        <el-button
          @click="handleGotoCurve"
          style="position: absolute; right: 20px"
          >{{
            $t("ui.data.column.scheduleResult.monthPlan.dailyChart")
          }}</el-button
        >
      </div>
    </el-card>
    <!-- <header-search
        style="margin: 0 15px 0"
        :defaultValue="search"
        :columns="searchColumns"
        @search="handleSearch"
      >
      </header-search> -->
    <div style="height: calc(100% - 70px)">
      <div style="height: 100%; padding: 10px">
        <vChart
          ref="chartRef"
          height="100%"
          width="100%"
          :xAxis="chartData.xAxis"
          :yAxis="chartData.yAxis"
          :series="chartData.series"
          :dataset="chartData.dataset"
          :dataZoom="chartData.dataZoom"
          :props="{
            tooltip: chartData.tooltip,
            animation: false,
            silent: false,
          }"
        />
      </div>
    </div>
  </basic-container>
</template>
<script>
import HeaderSearch from "@/components/Table/HeaderSearch.vue";
import vChart from "@/views/components/vChart.vue";
import moment from "moment";
import { getGantData as getMonthPlanData } from "@/api/cx/mdmMonthProdPlan";
import { param } from "@/utils";
export default {
  components: { HeaderSearch, vChart },
 name: "MonthlyPlanGantChart",
  data() {
    let nowMonth = moment().format("YYYY-MM");
    var dayList = [];
    var days = moment().daysInMonth();

    for (var i = 1; i <= days; i++) {
      dayList.push(i); // 假设你的坐标轴是以天来标记的
    }
    return {
      defaultProps: {
        children: "children",
        label: "type",
      },
      typeData: [],

      scheduleDate: nowMonth,
      days: days,
      chartData: {
        animation: false,
        dataset: {
          dimensions: [
            "startDay",
            "endDay",
            "diffDay",
            "machineCode",
            "index",
            "cpdh",
            "specdh",
            "ydl",
            "jd",
            "storageLocation",
            "qualityGrade",
          ],
          source: [
            // ["start", "end", "spec", "index"],
            // [1, 5, "Matcha Latte", 1],
            // [5, 7, "Tea", 2],
            // [3, 4, "Cheese Cocoa", 3],
            // [14, 11, "Cheese Brownie", 4],
            // [15, 2, "Matcha Cocoa", 5],
            // [7, 7, "Tea", 6],
            // [8, 9, "Orange Juice", 7],
            // [9, 12, "Lemon Juice", 8],
            // [21, 2, "Walnut Brownie", 9],
          ],
        },
        dataZoom: [
          {
            id: "dataZoomX",
            type: "slider",
            xAxisIndex: [0],
            filterMode: "none",
            height: 20,
            bottom: 0,
            startValue: 0,
            endValue: 20,
            showDetail: false,
            brushSelect: false,
          },
          {
            type: "inside",
            id: "insideX",
            xAxisIndex: 0,
            filterMode: "none",
            startValue: 0,
            endValue: 20,
            zoomOnMouseWheel: false,
            moveOnMouseMove: true,
          },
          {
            id: "dataZoomY",
            type: "slider",
            yAxisIndex: [0, 1],
            filterMode: "weakFilter",
            width: 20,
            right: 0,
            startValue: 0,
            endValue: 20,
            minValueSpan: 20,
            maxValueSpan: 20,
            handleIcon: "",
            zoomLock: true,
            showDetail: false,
            brushSelect: false,
          },
          {
            type: "inside",
            id: "insideY",
            filterMode: "weakFilter",
            yAxisIndex: [0, 1],
            startValue: 0,
            endValue: 20,
            zoomOnMouseWheel: false,
            moveOnMouseMove: true,
            moveOnMouseWheel: true,
          },
        ],
        grid: {
          left: "3%",
          right: "4%",
          bottom: "3%",
          containLabel: true,
        },
        tooltip: {
          trigger: "axis",
          // alwaysShowContent: true,
          formatter: (params, ticket, callback) => {
            // dimensions:["startDay","endDay","diffDay","machineCode","index","cpdh","specdh","ydl","jd","storageLocation","qualityGrade"],
            if (params[0]) {
              let htmlStr = `
              <div>规格：${params[0].value.codeId}</div>
              <div style="display:flex;border-top: 1px solid #DDD;margin-top: 10px;padding-top: 10px;">
                  <div style="padding-right:20px">
                    <div>物料编码：${params[0].value.cpdh}</div>
                    <div>实际安排：${params[0].value.ydl}</div>
                    <div>库存地点：${params[0].value.storageLocation}</div>
                  </div>
                  <div>
                    <div>施工代码：${params[0].value.specdh}</div>
                    <div>阶段：${params[0].value.jd}</div>
                    <div>质量等级：${params[0].value.qualityGrade}</div>
                  </div>
                  </div>
              </div>`;
              return htmlStr;
            }
          },
        },
        xAxis: {
          position: "top",
          type: "value",
          minInterval: 1,
          maxInterval: 1,
          min: 1,
          max: days + 1,
          splitNumber: 1,
          // interval: 3600 * 24 * 1000,
          data: dayList,
          axisLine: { show: false },
          axisLabel: {
            position: [0, 10],
            interval: 1,
            formatter: (val, index) => {
              if (val == this.days + 1) {
                return "";
              }
              return parseInt(val) + "日";
            },
          },
        },
        yAxis: [
          {
            type: "category",
            name: this.$t("ui.data.column.scheduleResult.specDesc"),
            nameLocation: "start",
            nameTextStyle: {
              padding: [0, 80, 0, 0],
            },
            nameGap: 8,
            inverse: true,
            // axisLabel:false,
            axisLabel: {
              interval: 0, // 强制显示全部刻度名
              overflow: "truncate",
              width: 100,
              formatter: (val, index) => {
                let groupIndex = -1;
                if (this.groupInfo.indexList)
                  groupIndex = this.groupInfo.indexList.findIndex(
                    (el) => el == val
                  );
                if (groupIndex != -1) {
                  return this.groupInfo.labelList[groupIndex];
                }
                return "";
              },
            },
          },
          {
            inverse: true,
            type: "category",
            position: "left",
            offset: 55, // 向右偏移，使分组文字显示位置不与原y轴重叠
            axisLine: {
              show: false, // 隐藏分组y轴的轴线
            },
            axisTick: {
              length: 55, // 延长刻度线做分组线
              inside: true, // 使刻度线相对轴线在上面与原y轴相接
              lineStyle: { color: "#000000" },
              interval: (index, value) => {
                let groupIndex = -1;
                if (this.groupInfo.indexList)
                  groupIndex = this.groupInfo.indexList.findIndex(
                    (el) => el == index
                  );
                if (groupIndex != -1) {
                  return true;
                }
              },
            },
            axisLabel: false,
            splitLine: {
              show: true,
              interval: (index, value) => {
                let groupIndex = -1;
                if (this.groupInfo.indexList)
                  groupIndex = this.groupInfo.indexList.findIndex(
                    (el) => el == index
                  );
                if (groupIndex != -1) {
                  return true;
                }
              },
            },
          },
        ],
        series: [
          {
            type: "bar",
            stack: "gant",
            // dataGroupId: "Other",
            itemStyle: {
              normal: {
                color: "#fff0",
              },
            },
            barWidth: 26,
            encode: {
              // 将 "start" 列映射到 X 轴。
              x: "startDay",
              // 将 "index" 列映射到 Y 轴。
              y: "index",
            },
          },
          {
            type: "bar",
            stack: "gant",
            labelLayout: {
              hideOverlap: true,
            },
            label: {
              show: true,
              formatter: (params) => {
                return "实际安排：" + params.value.ydl;
              },
            },
            // dataGroupId: "Other",
            encode: {
              // 将 "start" 列映射到 X 轴。
              x: "diffDay",
              // 将 "index" 列映射到 Y 轴。
              y: "index",
            },
            barWidth: 26,
            itemStyle: {
              normal: {
                borderRadius: [13, 13, 13, 13],
                color: (params) => {
                  if (params) {
                    let groupIndex = -1;
                    if (this.groupInfo.indexList) {
                      groupIndex = this.groupInfo.labelList.findIndex(
                        (el) => el == params.data.codeId
                      );
                      if (groupIndex % 2) {
                        return "#a5d4c2";
                      } else {
                        return "#e9d276";
                      }
                    }
                  }
                },
                // borderColor: "#fff",
              },
            },
          },
        ],
      },
      groupInfo: {
        labelList: [],
        indexList: [],
      },

      loading: false,
      data: [],
      sort: {},
      query: {},
    };
  },

  methods: {
    handleQuery() {},
    handleExport() {},
    handlePrint() {},
    handleGotoCurve() {
      this.$router.push("/monthPlanManagement/dailyCurveChart");
    },

    handleSearch(data) {
      this.query = data;
      // this.$set(this.page, "current", 1);
      this.getGantData();
    },

    //util
    handleSortChange({ column, prop, order }) {
      if (order) {
        this.sort = {
          orderByColumn: prop,
          isAsc: order == "ascending" ? "asc" : "desc",
        };
      } else {
        //默认排序
        this.sort = {};
      }
      this.getGantData();
    },

    handleCurrentChange(row) {
      // console.log(row);
      if (row) {
        this.subQuery = {
          salCode: row.salCode,
          forwarderCorp: row.forwarderCorp,
          shipDate: row.shipDate,
        };
        this.getSubList();
      } else {
        this.subData = [];
        this.subPage.current = 1;
        this.subPage.total = 0;
      }
    },

    formatParams() {
      const params = {
        ...this.query,
        ...this.sort,
      };

      return params;
    },

    async getGantData() {
      try {
        this.loading = true;
        const data = await getMonthPlanData({
          scheduleDate: this.scheduleDate,
          flag: 2,
        });
        // const data = await this.$axios.get("device/shutdownAnalysis/list");
        //处理返回的数据
        // this.data = data.data.rows;

        this.initRowData(data);
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
    },
    initRowData(data) {
      var dayList = [];
      var days = moment(this.scheduleDate).daysInMonth();
      this.days = days;
      for (var i = 1; i <= days; i++) {
        dayList.push(i); // 假设你的坐标轴是以天来标记的
      }
      let groupData = this.getGroupData(data);
      let tempIndex = 0;
      let dataset = {
        dimensions: [
          "startDay",
          "endDay",
          "diffDay",
          "machineCode",
          "index",
          "cpdh",
          "specdh",
          "ydl",
          "jd",
          "storageLocation",
          "qualityGrade",
        ],
        source: [],
      };
      let groupInfo = {
        labelList: [],
        indexList: [],
      };
      let yAxisData = [];
      //有分组，设置分组信息。
      // 依据分组初始化数据
      groupData.forEach((value, key, map) => {
        groupInfo.indexList.push(tempIndex);
        groupInfo.labelList.push(key);
        let tempSource = value
          .sort((a, b) => a.startDay - b.startDay)
          .map((element) => {
            //按开始日期排序
            yAxisData.push(tempIndex);
            return {
              ...element,
              diffDay:
                parseInt(element.endDay) - parseInt(element.startDay) + 1,
              machineCode: element.codeId,
              index: tempIndex++,
              startDay: parseInt(element.startDay),
              endDay: parseInt(element.endDay),
            };
          });
        dataset.source.push(...tempSource);
      });

      this.chartData.dataset = dataset;
      this.chartData.xAxis.max = this.days + 1;
      this.chartData.xAxis.data = dayList;

      this.chartData.yAxis[0].data = yAxisData;
      this.chartData.yAxis[1].data = yAxisData;
      this.groupInfo = groupInfo;
    },
    getGroupData(data) {
      var markData = new Map();
      for (var i = 0; i < data.length; i++) {
        if (markData.has(data[i].codeId)) {
          markData.set(data[i].codeId, [
            ...markData.get(data[i].codeId),
            data[i],
          ]);
        } else {
          markData.set(data[i].codeId, [data[i]]);
        }
      }
      return markData;
    },
  },
  activated() {
    this.getGantData();
  },
};
</script>
<style scoped>
::v-deep .top-el-card .el-card__body {
  padding: 10px 20px;
  display: flex;
  align-items: center;
  position: relative;
}
</style>
