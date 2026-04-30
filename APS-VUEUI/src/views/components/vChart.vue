<template>
  <div
    class="chart-container"
    ref="chartRef"
    :style="[{ width: this.width }, { height: this.height }]"
  ></div>
</template>
<script>
import * as echarts from "echarts";

export default {
  data() {
    return {
      chartEl: null,
    };
  },
  props: {
    width: {
      type: String,
      // default: "800px",
    },
    height: {
      type: String,
      // default: "400px"
    },
    xAxis: {
      required: false,
      type: Object | Array,
    },
    yAxis: {
      required: false,
      type: Object | Array,
    },
    props: {
      required: false,
      type: Object,
    },
    series: {
      required: false,
      type: Object | Array,
    },
    dataset: {
      required: false,
      type: Object,
    },
    dataZoom:{
      required: false,
      type: Object | Array,
    },
  },
  computed: {
    option: function () {
      // this.chartEl.setOption({
      //   legend: {
      //     show: true,
      //   },
      //   dataSet: this.dataSet || {},
      //   xAxis: this.xAxis || {},
      //   yAxis: this.yAxis || {},
      //   series: this.series,
      //   ...this.props,
      // });
      return {
        legend: {
          show: true,
        },
        dataset: this.dataset || {},
        xAxis: this.xAxis || {},
        yAxis: this.yAxis || {},
        series: this.series,
        dataZoom:this.dataZoom,
        ...this.props,
      };
    },
  },
  mounted() {
    window.addEventListener("resize", this._resizeHandler);
    if (this.height == "100%") {
      setTimeout(() => {
        this.chartEl = echarts.init(this.$el);
        this.chartEl.resize();
        this.chartEl.setOption(this.option);
      }, 0);
    } else {
      this.chartEl = echarts.init(this.$el);
      this.chartEl.setOption(this.option);
    }
  },
  beforeDestroy() {
    window.removeEventListener("resize", this._resizeHandler);
  },
  watch: {
    option: {
      handler(val, oldVal) {
        if(this.chartEl) this.chartEl.clear(oldVal);
        if(this.chartEl) this.chartEl.setOption(val);
      },
      immediate: true,
      deep: true,
    },
  },
  methods: {
    setOption(val) {
      // console.log(this.option);
      this.chartEl.setOption(val);
    },
    _resizeHandler() {
      this.chartEl && this.chartEl.resize();
    },
  },
};
</script>
<style scoped>
.chart-container {
  min-height: 350px;
}
</style>
