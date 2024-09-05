
<template>
  <basic-container>
    <div style="height: calc(100% - 10px)">
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
            ...chartData
          }"
        />
      </div>
    </div>
  </basic-container>
</template>
<script>
import HeaderSearch from "@/components/Table/HeaderSearch.vue";
import * as echarts from "echarts";
import vChart from "@/views/components/vChart.vue";
import moment from "moment";
import { getGantData as getMonthPlanData } from "@/api/cx/mdmMonthProdPlan";
import { param } from "@/utils";
export default {
  components: { HeaderSearch, vChart },
  name: "monthlyPlanGantChart",
  data() {
    var category = ["1","2","3","4","5","6","7"];
    var lineData = [868,784,806,839,820,847,936];
    var barData = [32,28,31,32,32,31,33];
    let lineDataMax = Math.max(lineData) + 50;
    let barDataMax = Math.max(barData) + 10;
    return {
      defaultProps: {
        children: "children",
        label: "type",
      },
      typeData: [],

      chartData: {
        backgroundColor: "#0f375f",
        tooltip: {
          trigger: "axis",
          axisPointer: {
            type: "shadow",
            label: {
              show: true,
              backgroundColor: "#fff",
            },
          },
        },
        grid: {
          right: "20%",
        },
        toolbox: {
          feature: {
            dataView: { show: true, readOnly: false },
            restore: { show: true },
            saveAsImage: { show: true },
          },
        },
        legend: {
          data: ["日产量", "模具数量"],
          textStyle: {
            color: "#ccc",
          },
        },
        xAxis: [
          {
            type: "category",
            axisTick: {
              alignWithLabel: true,
            },
            axisLine: {
              lineStyle: {
                color: "#ccc",
              },
            },
            data: category,
          },
        ],
        yAxis: [
          {
            type: "value",
            name: "日产量",
            position: "left",
            alignTicks: true,
            splitLine: { show: false },
            axisLine: {
              lineStyle: {
                color: "#5470C6",
              },
            },
          },
          {
            type: "value",
            name: "模具数量",
            position: "right",
            alignTicks: true,
            splitLine: { show: false },
            axisLine: {
              lineStyle: {
                color: "#43eec6",
              },
            },
          },
        ],
        series: [
          {
            name: "日产量",
            type: "line",
            smooth: true,
            showAllSymbol: true,
            symbol: "emptyCircle",
            symbolSize: 15,
            data: lineData,
          },
          {
            name: "模具数量",
            type: "bar",
            yAxisIndex: 1,
            barWidth: 10,
            itemStyle: {
              normal: {
                barBorderRadius: 5,
                color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                  { offset: 0, color: "#14c8d4" },
                  { offset: 1, color: "#43eec6" },
                ]),
              },
            },
            data: barData,
          },
          {
            name: "日产量",
            type: "bar",
            barGap: "-100%",
            barWidth: 10,
            itemStyle: {
              normal: {
                color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                  { offset: 0, color: "rgba(20,200,212,0.5)" },
                  { offset: 0.2, color: "rgba(20,200,212,0.2)" },
                  { offset: 1, color: "rgba(20,200,212,0)" },
                ]),
              },
            },
            z: -12,
            data: lineData,
            tooltip: {
              trigger: "item",
            },
          },
          {
            name: "日产量",
            type: "pictorialBar",
            symbol: "rect",
            itemStyle: {
              normal: {
                color: "#0f375f",
              },
            },
            symbolRepeat: true,
            symbolSize: [12, 4],
            symbolMargin: 1,
            z: -10,
            data: lineData,
            tooltip: {
              trigger: "item",
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

    handleSearch(data) {
      this.query = data;
      // this.$set(this.page, "current", 1);
      this.getGantData();
    },

    //util
    handleSortChange({ column, prop, order }) {
      if (order) {
        this.sort = {
          orderBy: prop,
          isAsc: order == "ascending",
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
        params: {
          ...this.sort,
        },
      };

      return params;
    },

    async getCurveData() {
      try {
        this.loading = true;
        // const data = await getMonthPlanData({
        //   scheduleDate: this.scheduleDate,
        //   flag: 2,
        // });
        // const data = await this.$axios.get("device/shutdownAnalysis/list");
        //处理返回的数据
        // this.data = data.data.rows;

        // this.initRowData(data);
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
    },
    initRowData(data) {},
  },
  activated() {
    this.getCurveData();
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
