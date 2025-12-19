
<template>
  <basic-container>
    <el-card class="top-el-card" shadow="never">
      <div style="height: 30px">
        <el-form :inline="true" style="display: contents">
          <el-form-item
            :label="$t('ui.data.column.scheduleResult.scheduleDate')"
          >
            <el-date-picker
              v-model="scheduleDate"
              type="date"
              value-format="yyyy-MM-dd"
              :clearable="false"
            />
          </el-form-item>
        </el-form>
        <el-button @click="handleSearch">{{ $t("搜索") }}</el-button>
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
        <GantChart ref="gantChartRef" :nowDay="this.nowDay" groupProp="codeId" dataLabel="innerMsg" />
        <!-- <vChart
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
        /> -->
      </div>
    </div>
  </basic-container>
</template>
<script>
import HeaderSearch from "@/components/Table/HeaderSearch.vue";
import vChart from "@/views/components/vChart.vue";
import moment from "moment";
import { getGantData as getMachineGantData } from "@/api/lh/scheduleResult";
import GantChart from "./components/gantChart.vue";
export default {
  components: { HeaderSearch, vChart, GantChart },
 name: "MachineGantChart",
  data() {
    let nowDate = moment().format("YYYY-MM-DD");
    nowDate = "2023-07-01"; //TODO 先设置为有数据的天数
    let nowDay = moment(nowDate).date();
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

      scheduleDate: nowDate,
      nowDay: nowDay,
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
        const data = await getMachineGantData({
          scheduleDate: this.scheduleDate,
          flag: 1,
        });
        // const data = await this.$axios.get("device/shutdownAnalysis/list");
        //处理返回的数据
        // this.data = data.data.rows;
        var nowDay = moment(this.scheduleDate).date();
        this.nowDay = nowDay;
        this.$refs.gantChartRef.initRowData(data);
        // this.initRowData(data);
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
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
