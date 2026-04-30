<template>
  <div class="screen vulcanization">
    <div
      class="screen-flex"
      :style="{
        transform: `scale(${size.scale})`,
        position: 'absolute',
        //top: `${size.top}px`,
        //left: `${size.left}px`,
        '--extraY': `${size.extraY}px`,
        '--extraX': `${size.extraX}px`,
      }"
    >
      <div class="header">
        <div class="header-left">
          <img
            class="header-bg"
            height="98"
            :width="1000"
            src="@/assets/largescreen/title_bg.png"
          />
          <div class="header-title">硫化</div>
        </div>
        <div class="header-right">
          <img
            class="header-bg"
            height="98"
            :width="1000"
            src="@/assets/largescreen/title_bg.png"
          />
          <div class="header-title">成型</div>
        </div>
      </div>
      <div class="content">
        <div class="top-layer">
          <!-- <div class="top-left-block"></div> -->
          <div class="top-center-block">
            <div class="data-block">
              <div class="label">
                <img src="@/assets/largescreen/icon/date.svg" />
                <span class="white-shadow-text data-label-text"
                  >计划量</span
                >
              </div>
              <div>
                <template v-if="totalPlanCount.value">
                  <span
                    class="blue-gradient-text data-value-text"
                    :style="{
                      minWidth:
                        totalPlanCount.value.toString().length * 15 + 'px',
                    }"
                  >
                    <countTo
                      :start-val="0"
                      :end-val="totalPlanCount.value"
                      :duration="2000"
                      :decimals="totalPlanCount.unit ? 2 : 0"
                    />
                  </span>
                  <span class="blue-gradient-text data-unit-text">{{
                    totalPlanCount.unit
                  }}</span>
                </template>
                <span v-else class="blue-gradient-text data-empty-text">-</span>
              </div>
            </div>
            <div class="data-block">
              <div class="label">
                <img src="@/assets/largescreen/icon/target.svg" />
                <span class="white-shadow-text data-label-text"
                  >完成量</span
                >
              </div>
              <div>
                <template v-if="totalPlanCount.value">
                  <span
                    class="blue-gradient-text data-value-text"
                    :style="{
                      minWidth:
                        totalPlanCount.value.toString().length * 15 + 'px',
                    }"
                  >
                    <countTo
                      :start-val="0"
                      :end-val="totalPlanCount.value"
                      :duration="2000"
                      :decimals="totalPlanCount.unit ? 2 : 0"
                    />
                  </span>
                  <span class="blue-gradient-text data-unit-text">{{
                    totalPlanCount.unit
                  }}</span>
                </template>
                <span v-else class="blue-gradient-text data-empty-text">-</span>
              </div>
            </div>
            <div class="data-block">
              <div class="label">
                <img src="@/assets/largescreen/icon/data.svg" />
                <span class="white-shadow-text data-label-text"
                  >达成率</span
                >
              </div>
              <div>
                <template v-if="totalPlanCount.value">
                  <span
                    class="blue-gradient-text data-value-text"
                    :style="{
                      minWidth:
                        totalPlanCount.value.toString().length * 15 + 'px',
                    }"
                  >
                    <countTo
                      :start-val="0"
                      :end-val="totalPlanCount.value"
                      :duration="2000"
                      :decimals="totalPlanCount.unit ? 2 : 0"
                    />
                  </span>
                  <span class="blue-gradient-text data-unit-text"
                    >{{ totalPlanCount.unit }}%</span
                  >
                </template>
                <span v-else class="blue-gradient-text data-empty-text">-</span>
              </div>
            </div>
          </div>
          <div class="top-right-block">
            <div class="data-block">
              <div class="label">
                <img src="@/assets/largescreen/icon/date.svg" />
                <span class="white-shadow-text data-label-text"
                  >计划量</span
                >
              </div>
              <div>
                <template v-if="totalPlanCount.value">
                  <span
                    class="blue-gradient-text data-value-text"
                    :style="{
                      minWidth:
                        totalPlanCount.value.toString().length * 15 + 'px',
                    }"
                  >
                    <countTo
                      :start-val="0"
                      :end-val="totalPlanCount.value"
                      :duration="2000"
                      :decimals="totalPlanCount.unit ? 2 : 0"
                    />
                  </span>
                  <span class="blue-gradient-text data-unit-text">{{
                    totalPlanCount.unit
                  }}</span>
                </template>
                <span v-else class="blue-gradient-text data-empty-text">-</span>
              </div>
            </div>
            <div class="data-block">
              <div class="label">
                <img src="@/assets/largescreen/icon/target.svg" />
                <span class="white-shadow-text data-label-text"
                  >完成量</span
                >
              </div>
              <div>
                <template v-if="totalPlanCount.value">
                  <span
                    class="blue-gradient-text data-value-text"
                    :style="{
                      minWidth:
                        totalPlanCount.value.toString().length * 15 + 'px',
                    }"
                  >
                    <countTo
                      :start-val="0"
                      :end-val="totalPlanCount.value"
                      :duration="2000"
                      :decimals="totalPlanCount.unit ? 2 : 0"
                    />
                  </span>
                  <span class="blue-gradient-text data-unit-text">{{
                    totalPlanCount.unit
                  }}</span>
                </template>
                <span v-else class="blue-gradient-text data-empty-text">-</span>
              </div>
            </div>
            <div class="data-block">
              <div class="label">
                <img src="@/assets/largescreen/icon/data.svg" />
                <span class="white-shadow-text data-label-text"
                  >达成率</span
                >
              </div>
              <div>
                <template v-if="totalPlanCount.value">
                  <span
                    class="blue-gradient-text data-value-text"
                    :style="{
                      minWidth:
                        totalPlanCount.value.toString().length * 15 + 'px',
                    }"
                  >
                    <countTo
                      :start-val="0"
                      :end-val="totalPlanCount.value"
                      :duration="2000"
                      :decimals="totalPlanCount.unit ? 2 : 0"
                    />
                  </span>
                  <span class="blue-gradient-text data-unit-text"
                    >{{ totalPlanCount.unit }}%</span
                  >
                </template>
                <span v-else class="blue-gradient-text data-empty-text">-</span>
              </div>
            </div>
          </div>
        </div>

        <div class="bottom-layer">
          <div class="bottom-left-block">
            <div class="block-title">
              <span class="grass-gradient-text"></span>
            </div>
            <div class="chart-content">
              <ScrollTable
                :columns="curingColumns"
                :data="completeData"
                :height="200 + size.extraY / 3 + 'px'"
              />
            </div>
          </div>
          <div class="bottom-right-block">
            <div class="block-title">
              <span class="grass-gradient-text"></span>
            </div>
            <div class="table-content">
              <ScrollTable
                :columns="curingColumns"
                :data="completeData"
                :height="200 + size.extraY / 3 + 'px'"
              />
            </div>
          </div>
        </div>

        <div class="bottom-layer">
          <div class="bottom-left-block">
            <!-- <div class="block-title">
              <span class="grass-gradient-text">按寸口产销数量分析</span>
            </div> -->
            <div class="chart-content">
             <GantChart ref="gantChartRef"  :nowDay="nowDay"   groupProp="innerMsg" groupLabel="硫化机" :dataLabel="getlabelStr" />
            </div>
          </div>

          <div class="bottom-right-block">
            <div class="block-title">
              <span class="grass-gradient-text">SKU数量分析</span>
            </div>
            <div class="table-content">
              <GantChart ref="gantChartRef"  :nowDay="nowDay"   groupProp="innerMsg" groupLabel="成型机" :dataLabel="getlabelStr" />
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
<script>
import moment  from "moment"

import LineChart from "@/views/largescreen/components/LineChart";
import ScrollTable from "@/views/largescreen/components/ScrollTable";
import GantChart from "@/views/largescreen/components/GantChart";
import countTo from "vue-count-to";
import VueSeamlessScroll from "vue-seamless-scroll";
export default {
  name: "largescreenIndex",
  components: {
    LineChart,
    ScrollTable,
    GantChart,
    VueSeamlessScroll,
    countTo,
  },

  data() {
    return {
      //
      size: {
        height: 1080,
        width: 1920,
        scale: 1,
        top: 0,
        left: 0,
      },

      nowDay: moment().format("yyyy-MM-DD"), // 当前日期
      getlabelStr: "label",
      totalPlanCount: {
        value: 100,
        unit: "万",
      },

      orderChart: {
        xAxisData: ["202501", "202502", "202503"],
        yAxisName: "",
        seriesList: [
          {
            seriesName: "计划订单量",
            yAxisData: [111, 222, 333],
            colors: {
              dot: "#093F83",
              dotBorder: "#39CEE6",
              line: "#45EFFF",
              area0: "#00deff",
              area1: "#0B3773",
            },
          },
          {
            seriesName: "备货量",
            yAxisData: [333, 111, 666],
            colors: {
              dot: "#093F83",
              dotBorder: "#c0d61f",
              line: "#c0d61f",
              area0: "#c8ff00",
              area1: "#0B3773",
            },
          },
        ],
      },

      curingColumns: [
        {
          prop: "no",
          label: "序号",
          width: 40,
        },
        {
          prop: "xxx",
          label: "机台",
          width: 40,
        },
        {
          prop: "yyy",
          label: "规格",
          width: 40,
        },
        {
          prop: "zzz",
          label: "规格代码",
          width: 40,
        },
        {
          prop: "qqq",
          label: "胚胎号",
          width: 80,
        },
        {
          prop: "qqq",
          label: "班别",
          width: 80,
        },
        {
          prop: "qqq",
          label: "计划量",
          width: 80,
        },
        {
          prop: "qqq",
          label: "完成量",
          width: 80,
        },
        {
          prop: "qqq",
          label: "达成率",
          width: 80,
        },
      ],
      completeColumns: [
        {
          prop: "brand",
          label: "寸口",
          width: 40,
        },
        {
          prop: "xxx",
          label: "销售需求SKU数",
          width: 40,
        },
        {
          prop: "yyy",
          label: "排产SKU数",
          width: 40,
        },
        {
          prop: "zzz",
          label: "规格完成率",
          width: 40,
        },
        {
          prop: "qqq",
          label: "销售需求计划量",
          width: 80,
        },
        {
          prop: "qqq",
          label: "排产计划量",
          width: 80,
        },
        {
          prop: "qqq",
          label: "计划完成率",
          width: 80,
        },
      ],
      channelColumns: [
        {
          prop: "channel",
          label: "渠道",
          width: 40,
        },
        {
          prop: "xxx",
          label: "销售需求SKU数",
          width: 40,
        },
        {
          prop: "yyy",
          label: "排产SKU数",
          width: 40,
        },
        {
          prop: "zzz",
          label: "规格完成率",
          width: 40,
        },
        {
          prop: "qqq",
          label: "销售需求计划量",
          width: 80,
        },
        {
          prop: "qqq",
          label: "排产计划量",
          width: 80,
        },
        {
          prop: "qqq",
          label: "计划完成率",
          width: 80,
        },
      ],
      skuColumns: [
        {
          prop: "project",
          label: "项目",
          width: 40,
        },
        {
          prop: "xxx",
          label: "月份",
          width: 40,
        },
        {
          prop: "yyy",
          label: "4月",
          width: 40,
        },
        {
          prop: "zzz",
          label: "5月",
          width: 40,
        },
        {
          prop: "qqq",
          label: "6月",
          width: 80,
        },
        {
          prop: "qqq",
          label: "年累计",
          width: 80,
        },
        {
          prop: "qqq",
          label: "H1",
          width: 80,
        },
        {
          prop: "qqq",
          label: "月对比",
          width: 80,
        },
      ],
      skuData: [],

      completeData: [
        {
          xxx: 1,
          yyy: 3,
          zzz: 5,
          qqq: 99,
          brand: "双钱",
        },
        {
          xxx: 1,
          yyy: 3,
          zzz: 5,
          qqq: 99,
        },
        {
          xxx: 1,
          yyy: 3,
          zzz: 5,
          qqq: 99,
        },
        {
          xxx: 1,
          yyy: 3,
          zzz: 5,
          qqq: 99,
        },
        {
          xxx: 1,
          yyy: 3,
          zzz: 5,
          qqq: 99,
        },
      ],

      deviceList: [
        {
          label: "可用模具数",
          value: "111",
        },
        {
          label: "硫化机台数",
          value: "222",
        },
        {
          label: "成型机台数",
          value: "333",
        },
        {
          label: "压出机台数",
          value: "444",
        },
        {
          label: "裁断机台数",
          value: "555",
        },
        {
          label: "压延机台数",
          value: "666",
        },
        {
          label: "密炼机台数",
          value: "777",
        },
      ],
    };
  },
  computed: {},
  created() {
    this.calcSize(window.innerWidth, window.innerHeight);
    // 监听窗口大小，缩放屏幕
    window.addEventListener("resize", () => {
      this.calcSize(window.innerWidth, window.innerHeight);
    });
  },
  beforeDestroy() {},
  methods: {
    // 计算屏幕缩放
    calcSize(width, height) {
      this.size.height = height;
      this.size.width = width;
      const xScale = window.innerHeight / 1080;
      const yScale = window.innerWidth / 1920;
      // console.log(yScale, xScale)

      if (xScale > yScale) {
        // 屏幕更高，上方留出空间，按更小的缩放
        this.size.scale = yScale;
        this.size.top = ((xScale - yScale) * 1080) / 2;
        this.size.left = 0;
        this.size.extraY = ((xScale - yScale) * 1080) / yScale;
        this.size.extraX = 0;
      } else {
        // 屏幕更扁，左右方留出空间，按更小的缩放
        this.size.scale = xScale;
        this.size.top = 0;
        this.size.left = ((yScale - xScale) * 1920) / 2;
        this.size.extraY = 0;
        this.size.extraX = ((yScale - xScale) * 1920) / xScale;
      }
    },
    //页面跳转
    handleToPage(url) {},
  },
};
</script>


<style lang="scss" scoped>
@font-face {
  font-family: "DingTalk";
  src: url("../../../assets/largescreen/fonts/DingTalk JinBuTi.ttf");
}

@font-face {
  font-family: "DingTalk Sans";
  src: url("../../../assets/largescreen/fonts/DingTalk Sans.ttf");
}

@import "../../../assets/largescreen/style.scss";

.screen {
  background: url(../../../assets/largescreen/bg.png);
}
.processes-block {
  background-image: url(../../../assets/largescreen/block.png);
}
.block-title {
  background-image: url(../../../assets/largescreen/block_title_bg.png);
}
</style>