<template>
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
</template>

<script>
import vChart from "@/views/components/vChart.vue";
import moment from "moment";
export default {
  components: { vChart },
  props: {
    nowDay: {
      type: String | Number,
    },
    groupProp: {
      type: String,
      default: "codeId",
    },
    dataLabel: {
      type: String | Function,
      default: "innerMsg",
    },
    groupLabel: {
      type: String,
      default: "硫化机",
    },
  },
  data() {
    var hoursList = [];
    for (var i = 1; i <= 72; i++) {
      hoursList.push(i); // 显示三天，24小时
    }
    return {
      defaultProps: {
        children: "children",
        label: "type",
      },
      typeData: [],
      chartData: {
        animation: false,
        dataset: {
          dimensions: [
            "hourStart",
            "hourEnd",
            "hourDiff",
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
            xAxisIndex: [0, 1],
            filterMode: "none",
            height: 20,
            bottom: 0,
            startValue: 0,
            endValue: 40,
            showDetail: false,
            brushSelect: false,
          },
          {
            type: "inside",
            id: "insideX",
            xAxisIndex: [0, 1],
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
            // dimensions:["hourStart","hourEnd","hourDiff","machineCode","index","cpdh","specdh","ydl","jd","storageLocation","qualityGrade"],
            if (params[0]) {
              let htmlStr = `
              <div>${params[0].value.innerMsg}</div>
              <div style="display:flex;border-top: 1px solid #DDD;margin-top: 10px;padding-top: 10px;">
                  <div style="padding-right:20px">
                    <div>SAP品号：${params[0].value.cpdh}</div>
                    <div>计划量：${params[0].value.ydl}</div>
                  </div>
                  <div>
                    <div>胎胚代号：${params[0].value.specdh}</div>
                    <div>已完成量：${params[0].value.ywcl}</div>
                  </div>
                  </div>
              </div>`;
              return htmlStr;
            }
          },
        },
        xAxis: [
          {
            position: "top",
            type: "value",
            minInterval: 1,
            maxInterval: 1,
            min: 1,
            max: 72,
            splitNumber: 1,
            // interval: 3600 * 24 * 1000,
            data: hoursList,
            axisLine: { show: false },
            axisLabel: {
              position: [0, 10],
              interval: 1,
              formatter: (val, index) => {
                if (val == 72) {
                  return "";
                }
                return parseInt(val % 24) || 24;
              },
              color: "#ffffff", // 修改文字颜色为
            },
          },
          {
            position: "top",
            type: "value",
            offset: 30, // 向上偏移，使分组文字显示位置不与原x轴重叠
            minInterval: 12,
            maxInterval: 12,
            min: 1,
            max: 72,
            data: hoursList,
            axisLine: { show: false },
            axisLabel: {
              formatter: (val, index) => {
                switch (val) {
                  case 12:
                    return "前一天";
                    break;
                  case 36:
                    return this.nowDay + "号";
                    break;
                  case 60:
                    return "后一天";
                    break;
                  default:
                    return "";
                    break;
                }
              },
              verticalAlign: "bottom",
              height: 80,
              color: "#ffffff", // 修改文字颜色为
            },
            axisTick: {
              show: true,
              length: 5, // 延长刻度线做分组线
              inside: false, // 使刻度线相对轴线在上面与原x轴相接，默认在轴线下方
              lineStyle: {
                color: "#ffffff",
              },
            },
          },
        ],
        yAxis: [
          {
            type: "category",
            name: this.groupLabel,
            nameLocation: "start",
            nameTextStyle: {
              color: "#ffffff",
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
            offset: 55,
            axisLine: {
              show: false,
            },
            axisTick: {
              length: 55,
              inside: true,
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
                color: "#fff",
              },
            },
            barWidth: 26,
            encode: {
              // 将 "start" 列映射到 X 轴。
              x: "startHour",
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
              position: "insideLeft",
              formatter: (params) => {
                console.log(params);
                let tempLabel;
                if (typeof this.dataLabel == "function") {
                  tempLabel = this.dataLabel(params.value);
                } else {
                  tempLabel = params.value[this.dataLabel];
                }
                return `{r|${params.value.innerIndex + 1}}` + " " + tempLabel;
              },
              rich: {
                r: {
                  width: 8,
                  height: 8,

                  color: "#FFF",
                  backgroundColor: "#d15686",

                  borderRadius: 10,
                  padding: [6, 5, 5, 5],
                  marginRight: 5,
                  // 没有设置 `align`，则 `align` 为 right
                },
              },
            },
            // dataGroupId: "Other",
            encode: {
              // 将 "start" 列映射到 X 轴。
              x: "hourDiff",
              // 将 "index" 列映射到 Y 轴。
              y: "index",
            },
            barWidth: 26,
            colorBy: "data",
            itemStyle: {
              normal: {
                borderRadius: [13, 13, 13, 13],
                color: (params) => {
                  //颜色按分组显示，

                  if (params) {
                    switch (params.value.innerIndex % 5) {
                      case 0:
                        return "#a5d4c2";
                        break;
                      case 1:
                        return "#e6bfbf";
                        break;
                      case 2:
                        return "#e9d276";
                        break;
                      case 3:
                        return "#89d1d8";
                      case 4:
                        return "#e6e7ec";
                        break;
                      default:
                        return "#a5d4c2";
                        break;
                    }
                  }
                  return "#a5d4c2";
                },
                borderColor: "#fff",
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
    initRowData(data) {
      var days = moment(this.scheduleDate);

      let groupData = this.getGroupData(data);
      let tempIndex = 0;
      let dataset = {
        dimensions: [
          "hourStart",
          "hourEnd",
          "hourDiff",
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
          // .sort((a, b) => a.startDay - b.startDay)//按开始日期排序
          .map((element, index) => {
            yAxisData.push(tempIndex);
            return {
              ...element,
              hourDiff: parseInt(element.hourInterval),
              machineCode: element[this.groupProp],
              index: tempIndex++,
              innerIndex: index,
              hourStart: parseInt(element.hourStart) + 1,
              hourEnd:
                parseInt(element.hourStart) +
                1 +
                parseInt(element.hourInterval),
            };
          });
        dataset.source.push(...tempSource);
      });

      this.chartData.dataset = dataset;
      this.chartData.yAxis[0].data = yAxisData;
      this.chartData.yAxis[1].data = yAxisData;
      this.groupInfo = groupInfo;
    },
    getGroupData(data) {
      var markData = new Map();
      for (var i = 0; i < data.length; i++) {
        if (markData.has(data[i][this.groupProp])) {
          markData.set(data[i][this.groupProp], [
            ...markData.get(data[i][this.groupProp]),
            data[i],
          ]);
        } else {
          markData.set(data[i][this.groupProp], [data[i]]);
        }
      }
      return markData;
    },
  },
};
</script>

<style>
</style>
