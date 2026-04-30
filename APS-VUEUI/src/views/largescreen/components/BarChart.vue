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
    const tody = moment().format("YYYY-MM-DD");

    const dayList = [
      moment(tody).subtract(6, "days").format("YYYY-MM-DD"),
      moment(tody).subtract(5, "days").format("YYYY-MM-DD"),
      moment(tody).subtract(4, "days").format("YYYY-MM-DD"),
      moment(tody).subtract(3, "days").format("YYYY-MM-DD"),
      moment(tody).subtract(2, "days").format("YYYY-MM-DD"),
      moment(tody).subtract(1, "days").format("YYYY-MM-DD"),
      tody,
    ];


    return {
      defaultProps: {
        children: "children",
        label: "type",
      },
      typeData: [],
      chartData: {
        xAxis: {
          type: "category",
          data: dayList,
          axisLabel: {
            color: "#ffffff",
          },
        },
        yAxis: {
          type: "value",
          axisLabel: {
            formatter: "{value} %", // 显示百分比
            color: "#ffffff",
          },
        },
        series: [
          {
            data: [80, 60, 100, 80, 90, 92, 77],
            type: "bar",
            itemStyle: {
              color: "#5786d7",
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
