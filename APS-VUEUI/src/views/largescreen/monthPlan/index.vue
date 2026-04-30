<template>
  <div class="screen month-plan">
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
        <img
          class="header-bg"
          height="98"
          :width="1000"
          src="@/assets/largescreen/title_bg.png"
        />
        <div class="header-title">月计划</div>
        <div class="screen-btn" @click="handleFullScreen">
          <i class="el-icon-full-screen" />
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
                  >本月总计划量</span
                >
              </div>
              <div>
                <template v-if="isNumber(monthTotalPlanQty.value)">
                  <span
                    class="blue-gradient-text data-value-text"
                    :style="{
                      minWidth:
                        monthTotalPlanQty.value.toString().length * 15 + 'px',
                    }"
                  >
                    <countTo
                      :start-val="0"
                      :end-val="monthTotalPlanQty.value"
                      :duration="2000"
                      :decimals="monthTotalPlanQty.unit ? 2 : 0"
                    />
                  </span>
                  <span class="blue-gradient-text data-unit-text">{{
                    monthTotalPlanQty.unit
                  }}</span>
                </template>
                <span v-else class="blue-gradient-text data-empty-text">-</span>
              </div>
            </div>
            <div class="data-block">
              <div class="label">
                <img src="@/assets/largescreen/icon/target.svg" />
                <span class="white-shadow-text data-label-text"
                  >本月完成量</span
                >
              </div>
              <div>
                <template v-if="isNumber(monthTotalFinishQty.value)">
                  <span
                    class="blue-gradient-text data-value-text"
                    :style="{
                      minWidth:
                        monthTotalFinishQty.value.toString().length * 15 + 'px',
                    }"
                  >
                    <countTo
                      :start-val="0"
                      :end-val="monthTotalFinishQty.value"
                      :duration="2000"
                      :decimals="monthTotalFinishQty.unit ? 2 : 0"
                    />
                  </span>
                  <span class="blue-gradient-text data-unit-text">{{
                    monthTotalFinishQty.unit
                  }}</span>
                </template>
                <span v-else class="blue-gradient-text data-empty-text">-</span>
              </div>
            </div>
            <div class="data-block">
              <div class="label">
                <img src="@/assets/largescreen/icon/data.svg" />
                <span class="white-shadow-text data-label-text"
                  >计划达成率</span
                >
              </div>
              <div>
                <template v-if="isNumber(planFinishRate.value)">
                  <span
                    class="blue-gradient-text data-value-text"
                    :style="{
                      minWidth:
                        planFinishRate.value.toString().length * 15 + 'px',
                    }"
                  >
                    <countTo
                      :start-val="0"
                      :end-val="planFinishRate.value"
                      :duration="2000"
                      :decimals="planFinishRate.unit ? 2 : 0"
                    />
                  </span>
                  <span class="blue-gradient-text data-unit-text"
                    >{{ planFinishRate.unit }}%</span
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
                  >本月总规格数</span
                >
              </div>
              <div>
                <template v-if="isNumber(monthTotalSpecQty.value)">
                  <span
                    class="blue-gradient-text data-value-text"
                    :style="{
                      minWidth:
                        monthTotalSpecQty.value.toString().length * 15 + 'px',
                    }"
                  >
                    <countTo
                      :start-val="0"
                      :end-val="monthTotalSpecQty.value"
                      :duration="2000"
                      :decimals="monthTotalSpecQty.unit ? 2 : 0"
                    />
                  </span>
                  <span class="blue-gradient-text data-unit-text">{{
                    monthTotalSpecQty.unit
                  }}</span>
                </template>
                <span v-else class="blue-gradient-text data-empty-text">-</span>
              </div>
            </div>
            <div class="data-block">
              <div class="label">
                <img src="@/assets/largescreen/icon/target.svg" />
                <span class="white-shadow-text data-label-text"
                  >本月规格完成量</span
                >
              </div>
              <div>
                <template v-if="isNumber(monthTotalSpecFinishQty.value)">
                  <span
                    class="blue-gradient-text data-value-text"
                    :style="{
                      minWidth:
                        monthTotalSpecFinishQty.value.toString().length * 15 +
                        'px',
                    }"
                  >
                    <countTo
                      :start-val="0"
                      :end-val="monthTotalSpecFinishQty.value"
                      :duration="2000"
                      :decimals="monthTotalSpecFinishQty.unit ? 2 : 0"
                    />
                  </span>
                  <span class="blue-gradient-text data-unit-text">{{
                    monthTotalSpecFinishQty.unit
                  }}</span>
                </template>
                <span v-else class="blue-gradient-text data-empty-text">-</span>
              </div>
            </div>
            <div class="data-block">
              <div class="label">
                <img src="@/assets/largescreen/icon/data.svg" />
                <span class="white-shadow-text data-label-text"
                  >规格完成率</span
                >
              </div>
              <div>
                <template v-if="isNumber(monthSpecFinishRate.value)">
                  <span
                    class="blue-gradient-text data-value-text"
                    :style="{
                      minWidth:
                        monthSpecFinishRate.value.toString().length * 15 + 'px',
                    }"
                  >
                    <countTo
                      :start-val="0"
                      :end-val="monthSpecFinishRate.value"
                      :duration="2000"
                      :decimals="monthSpecFinishRate.unit ? 2 : 0"
                    />
                  </span>
                  <span class="blue-gradient-text data-unit-text"
                    >{{ monthSpecFinishRate.unit }}%</span
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
              <span class="grass-gradient-text">按品牌产销数量分析</span>
            </div>
            <div class="table-content">
              <ScrollTable
                :columns="brandColumns"
                :data="brandTableData"
                :height="200 + size.extraY / 3 + 'px'"
              />
            </div>
          </div>
          <div class="bottom-right-block">
            <div class="block-title">
              <span class="grass-gradient-text">按渠道产销数量分析</span>
            </div>
            <div class="table-content">
              <ScrollTable
                :columns="channelColumns"
                :data="channelTableData"
                :height="200 + size.extraY / 3 + 'px'"
              />
            </div>
          </div>
        </div>

        <div class="bottom-layer">
          <div class="bottom-left-block">
            <div class="block-title">
              <span class="grass-gradient-text">按寸口产销数量分析</span>
            </div>
            <div class="table-content">
              <ScrollTable
                :columns="proSizeColumns"
                :data="proSizeTableData"
                :height="200 + size.extraY / 3 + 'px'"
              />
            </div>
          </div>

          <div class="bottom-right-block">
            <div class="block-title">
              <span class="grass-gradient-text">SKU数量分析</span>
            </div>
            <div class="table-content">
              <ScrollTable
                :columns="skuColumns"
                :data="skuTableData"
                :height="200 + size.extraY / 3 + 'px'"
              />
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
<script>
import moment from "moment";
import Big from "big.js";

import {
  homePage4Plan,
  listMonthFinishRateBrand,
  listProSizeSummary,
  listChannelClassification,
  selectSkuSummary4BigScreen,
  listBrandClassification,
} from "@/api/monthplan/report";

import LineChart from "@/views/largescreen/components/LineChart";
import ScrollTable from "@/views/largescreen/components/ScrollTable";
// import TestScrollTable from "@/views/largescreen/components/TestScrollTable";
import countTo from "vue-count-to";
import VueSeamlessScroll from "vue-seamless-scroll";
export default {
  name: "largescreenIndex",
  components: {
    LineChart,
    ScrollTable,
    // TestScrollTable,
    VueSeamlessScroll,
    countTo,
  },
  props: {
    fullScreen: {
      type: Boolean,
      default: true,
    },
  },
  data() {
    const today = moment().format("yyyy-MM").split("-");

    const skuColumns = [
      {
        prop: "product",
        label: "项目",
        width: 40,
      },
      {
        prop: "name",
        label: "项目",
        width: 80,
      },
    ];
    skuColumns.push({
      prop: `month${moment().subtract(2, "months").month() + 1}`,
      label: `${moment().subtract(2, "months").month() + 1}月份`,
      width: 40,
    });
    skuColumns.push({
      prop: `month${moment().subtract(1, "months").month() + 1}`,
      label: `${moment().subtract(1, "months").month() + 1}月份`,
      width: 40,
    });
    skuColumns.push({
      prop: `month${moment().month() + 1}`,
      label: `${moment().month() + 1}月份`,
      width: 40,
    });

    skuColumns.push(
      {
        prop: "yearSum",
        label: "年累计",
        width: 40,
      },
      {
        prop: "monthAvg",
        label: "H1",
        width: 40,
      },
      {
        prop: "currentMonthAvgDiff",
        label: "月对比",
        width: 40,
      }
    );

    return {
      //
      size: {
        height: 1080,
        width: 1920,
        scale: 1,
        top: 0,
        left: 0,
      },
      year: today[0],
      month: today[1],

      monthTotalPlanQty: {
        value: "",
        unit: "",
      },
      monthTotalFinishQty: { value: "", unit: "" },
      planFinishRate: { value: "", unit: "" },
      monthTotalSpecQty: { value: "", unit: "" },
      monthTotalSpecFinishQty: { value: "", unit: "" },
      monthSpecFinishRate: { value: "", unit: "" },

      brandColumns: [
        {
          prop: "classificationName",
          label: "品牌",
          width: 40,
        },
        {
          prop: "saleSkuCount",
          label: "销售需求SKU数",
          width: 40,
        },
        {
          prop: "produceSkuCount",
          label: "排产SKU数",
          width: 40,
        },
        {
          prop: "specFinishRate",
          label: "规格完成率",
          width: 40,
        },
        {
          prop: "salePlanQty",
          label: "销售需求计划量",
          width: 80,
        },
        {
          prop: "producePlanQty",
          label: "排产计划量",
          width: 40,
        },
        {
          prop: "planFinishRate",
          label: "计划完成率",
          width: 40,
        },
      ],
      brandTableData: [],
      // 寸口
      proSizeColumns: [
        {
          prop: "proSize",
          label: "寸口",
          width: 40,
        },
        {
          prop: "salePlanCount",
          label: "销售需求SKU数",
          width: 40,
        },
        {
          prop: "proPlanCount",
          label: "排产SKU数",
          width: 40,
        },
        {
          prop: "proFinishRate",
          label: "规格完成率",
          width: 40,
        },
        {
          prop: "salePlanQty",
          label: "销售需求计划量",
          width: 80,
        },
        {
          prop: "proPlanQty",
          label: "排产计划量",
          width: 80,
        },
        {
          prop: "saleFinishRate",
          label: "计划完成率",
          width: 80,
        },
      ],
      proSizeTableData: [],
      //渠道
      channelColumns: [
        {
          prop: "classificationName",
          label: "渠道",
          width: 40,
        },
        {
          prop: "saleSkuCount",
          label: "销售需求SKU数",
          width: 40,
        },
        {
          prop: "produceSkuCount",
          label: "排产SKU数",
          width: 40,
        },
        {
          prop: "specFinishRate",
          label: "规格完成率",
          width: 40,
        },
        {
          prop: "salePlanQty",
          label: "销售需求计划量",
          width: 40,
        },
        {
          prop: "producePlanQty",
          label: "排产计划量",
          width: 80,
        },
        {
          prop: "planFinishRate",
          label: "计划完成率",
          width: 40,
        },
      ],
      channelTableData: [],
      //
      skuColumns: skuColumns,
      skuTableData: [],
    };
  },
  computed: {},
  created() {
    this.homePage4Plan();
    this.listMonthFinishRateBrand();
    this.listProSizeSummary();
    this.listChannelClassification();
    this.selectSkuSummary4BigScreen();

    this.resize();
    // 监听窗口大小，缩放屏幕
    window.addEventListener("resize", this.resize);
  },
  mounted() {
    this.resize();
  },
  beforeDestroy() {
    window.removeEventListener("resize", this.resize);
  },
  methods: {
    // 计算屏幕缩放
    calcSize(width, height) {
      this.size.height = height;
      this.size.width = width;
      const xScale = height / 1080;
      const yScale = width / 1920;
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
    isNumber(val) {
      return val !== "" && !isNaN(val);
    },
    //页面跳转
    handleToPage(url) {},
    handleFullScreen() {
      if (this.fullScreen) {
        this.$router.push(`/`);
      } else {
        this.$router.push(`largescreen/home`);
      }
    },
    resize() {
      let width = window.innerWidth;
      let height = window.innerHeight;
      if (this.fullScreen === false && this.$el) {
        width = this.$el.offsetWidth;
        height = this.$el.offsetHeight;
        // console.log(this.$el.offsetHeight, this.$el.innerWidth)
      }

      this.calcSize(width, height);
    },
    async homePage4Plan() {
      try {
        const res = await homePage4Plan();

        this.monthTotalPlanQty = {
          value: res.monthTotalPlanQty,
          unit: "",
        };
        this.monthTotalFinishQty = {
          value: res.monthTotalFinishQty,
          unit: "",
        };
        this.planFinishRate = {
          value: parseFloat((res.planFinishRate * 100).toFixed(2)),
          unit: "",
        };
        this.monthTotalSpecQty = {
          value: res.monthTotalSpecQty,
          unit: "",
        };
        this.monthTotalSpecFinishQty = {
          value: res.monthTotalSpecFinishQty,
          unit: "",
        };
        this.monthSpecFinishRate = {
          value: parseFloat((res.monthSpecFinishRate * 100).toFixed(2)),
          unit: "",
        };
      } catch (error) {
        console.error(error);
      }
    },
    async listMonthFinishRateBrand() {
      try {
        this.loading = true;
        const data = await listBrandClassification({
          factoryCode: "AH01",
          year: this.year,
          month: this.month,
        });
        this.brandTableData = data.rows.map((row) => {
          return {
            ...row,
            saleSkuCount: row.saleSkuCount || 0,
            produceSkuCount: row.produceSkuCount || 0,
            salePlanSkuCount: row.salePlanSkuCount || 0,
            producePlanQty: row.producePlanQty || 0,
            specFinishRate: row.specFinishRate
              ? Big(row.specFinishRate).times(100).round(2).toString() + "%"
              : "0%",
            planFinishRate: row.planFinishRate
              ? Big(row.planFinishRate).times(100).round(2).toString() + "%"
              : "0%",
          };
        });
        // console.log(data);
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
    },
    async listProSizeSummary() {
      try {
        this.loading = true;
        const data = await listProSizeSummary({
          factoryCode: "AH01",
          year: this.year,
          month: this.month,
        });
        this.proSizeTableData = data.rows.map((row) => {
          return {
            ...row,
            saleSkuCount: row.saleSkuCount || 0,
            produceSkuCount: row.produceSkuCount || 0,
            accuracyRate: row.accuracyRate || 0,
            salePlanSkuCount: row.salePlanSkuCount || 0,
            producePlanQty: row.producePlanQty || 0,
            produceFinishRate: row.produceFinishRate || 0,
            salePlanCount: row.salePlanCount || 0,
            proPlanCount: row.proPlanCount || 0,
            proFinishRate: row.proFinishRate
              ? Big(row.proFinishRate).times(100).round(2).round(2).toString() +
                "%"
              : "0%",
            saleFinishRate: row.saleFinishRate
              ? Big(row.saleFinishRate)
                  .times(100)
                  .round(2)
                  .round(2)
                  .toString() + "%"
              : "0%",
          };
        });
        // console.log(data);
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
    },
    async listChannelClassification() {
      try {
        this.loading = true;
        const data = await listChannelClassification({
          factoryCode: "AH01",
          year: this.year,
          month: this.month,
        });
        this.channelTableData = data.rows
          .map((row) => {
            return {
              ...row,
              saleSkuCount: row.saleSkuCount || 0,
              produceSkuCount: row.produceSkuCount || 0,
              accuracyRate: row.accuracyRate || 0,
              salePlanSkuCount: row.salePlanSkuCount || 0,
              producePlanQty: row.producePlanQty || 0,
              produceFinishRate: row.produceFinishRate || 0,
              salePlanCount: row.salePlanCount || 0,
              proPlanCount: row.proPlanCount || 0,
              planFinishRate: row.planFinishRate
                ? Big(row.planFinishRate).times(100).round(2).toString() + "%"
                : "0%",
              specFinishRate: row.specFinishRate
                ? Big(row.specFinishRate).times(100).round(2).toString() + "%"
                : "0%",
            };
          })
          .filter((row) => {
            return row.classificationName != "合计";
          });
        // console.log(data);
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
    },
    async selectSkuSummary4BigScreen() {
      try {
        this.loading = true;
        const res = await selectSkuSummary4BigScreen({
          factoryCode: "AH01",
          year: this.year,
          month: this.month,
        });

        const keys = Object.keys(res);
        let map = {};
        keys.forEach((key) => {
          let item = res[key];
          const itemKeys = Object.keys(item);
          itemKeys.forEach((itemKey) => {
            if (map[itemKey]) {
              map[itemKey][key] = item[itemKey];
            } else {
              map[itemKey] = {};
              map[itemKey][key] = item[itemKey];
            }
          });
        });

        this.skuTableData = [
          {
            product: "计划",
            name: this.$t("ui.data.column.report.skuSummary.produceTotal"),
            ...map["produceTotal"],
          },
          {
            product: "计划",
            name: this.$t("ui.data.column.report.skuSummary.planDay"),
            ...map["planDay"],
          },
          {
            product: "计划",
            name: this.$t("ui.data.column.report.skuSummary.avgDailyProduce"),
            ...map["avgDailyProduce"],
          },
          {
            product: "计划",
            name: this.$t("ui.data.column.report.skuSummary.produceSkuCount"),
            ...map["produceSkuCount"],
          },
          {
            product: "计划",
            name: this.$t(
              "ui.data.column.report.skuSummary.produceAvgSkuCount"
            ),
            ...map["produceAvgSkuCount"],
          },
          {
            product: "完成",
            name: this.$t("ui.data.column.report.skuSummary.finishTotal"),
            ...map["finishTotal"],
          },
          {
            product: "完成",
            name: this.$t("ui.data.column.report.skuSummary.actualDay"),
            ...map["actualDay"],
          },
          {
            product: "完成",
            name: this.$t("ui.data.column.report.skuSummary.avgDailyFinish"),
            ...map["avgDailyFinish"],
          },
          {
            product: "完成",
            name: this.$t("ui.data.column.report.skuSummary.finishSkuCount"),
            ...map["finishSkuCount"],
          },
          {
            product: "完成",
            name: this.$t("ui.data.column.report.skuSummary.finishAvgSkuCount"),
            ...map["finishAvgSkuCount"],
          },
          {
            product: "投产分析",
            name: this.$t(
              "ui.data.column.report.skuSummaryProduce.day1To7Count"
            ),
            ...map["day1To7Count"],
          },
          {
            product: "投产分析",
            name: this.$t(
              "ui.data.column.report.skuSummaryProduce.day8To14Count"
            ),
            ...map["day8To14Count"],
          },
          {
            product: "投产分析",
            name: this.$t(
              "ui.data.column.report.skuSummaryProduce.day15To21Count"
            ),
            ...map["day15To21Count"],
          },
          {
            product: "投产分析",
            name: this.$t(
              "ui.data.column.report.skuSummaryProduce.day22To31Count"
            ),
            ...map["day22To31Count"],
          },
        ].map((row) => {
          return {
            ...row,
            currentMonthAvgDiff: row.currentMonthAvgDiff
              ? Big(row.currentMonthAvgDiff).times(100).round(2).toString() +
                "%"
              : "0%",
          };
        });
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
    },
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
.full-screen {
  background: url(../../../assets/largescreen/bg.png);
}
.processes-block {
  background-image: url(../../../assets/largescreen/block.png);
}
.block-title {
  background-image: url(../../../assets/largescreen/block_title_bg.png);
}
</style>