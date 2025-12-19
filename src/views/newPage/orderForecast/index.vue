
<template>
  <basic-container>
    <page-table
      tableRef="ProSizeSummaryMainTable"
      :calcHeight="true"
      v-loading="loading"
      :columns="columns"
      :searchColumns="searchColumns"
      :data="data"
      :page="page"
      :search="search"
      @refresh="getList"
      @search="handleSearch"
      @pageChange="handlePageChange"
      @sort-change="handleSortChange"
      :showSummary="true"
      :selectArea="false"
      :span-method="objectSpanMethod"
      :summary-method="getSummaryMethod"
    >
      <template slot="header">
        <el-button
          type="primary"
          plain
          v-hasPermi="['monthplan:productionMouldConfiguration:add']"
          >{{ $t("生成") }}
        </el-button>
        <el-button
          @click="handleExport"
          v-hasPermi="['report:proSizeSummary:export']"
          >{{ $t("ui.frame.btn.export") }}</el-button
        >
      </template>
      <template slot="headerRight"> </template>
    </page-table>
  </basic-container>
</template>
<script>
//lib
import moment from "moment";
import Big from "big.js";
//utils
// import { downloadLink } from "@/utils/request";
//interface
import {
  listProSizeSummary,
  exportProSizeSummary,
} from "@/api/monthplan/report";
//components

export default {
  name: "orderForecast",
  components: {
    // tltUpload,
  },
  dicts: ["biz_factory_name"],
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
      stat: {},
    };
  },
  computed: {
    columns() {
      let columns = [
        // {
        //   prop: "yearMonth",
        //   label: this.$t("ui.data.column.report.proSizeSummary.yearMonth"),
        // },
        {
          prop: "工厂",
          label: this.$t("工厂"),
          align: "center",
          width: 120,
        },
        {
          prop: "年份",
          label: this.$t("年份"),
        },
        {
          prop: "月份",
          label: this.$t("月份"),
        },
        {
          prop: "产品分类",
          label: this.$t("产品分类"),
        },
        {
          prop: "类型",
          label: this.$t("类型"),
        },
        {
          prop: "品牌",
          label: this.$t("品牌"),
        },
        {
          prop: "NC物料编码",
          label: this.$t("NC物料编码"),
        },
        {
          prop: "物料描述",
          label: this.$t("物料描述"),
        },
        {
          prop: "T月",
          label: this.$t("T月"),
        },

        {
          prop: "T1月",
          label: this.$t("T+1月"),
          // align: "right",
          // formatter: (row, column, value) => {
          //   return value
          //     ? Big(value).times(100).toString() + "%"
          //     : value === 0
          //     ? "0%"
          //     : "";
          // },
        },
        {
          prop: "T2月",
          label: this.$t("T+2月"),
        },
        {
          prop: "备注",
          label: this.$t("备注"),
          // align: "right",
          // formatter: (row, column, value) => {
          //   return value
          //     ? Big(value).times(100).toString() + "%"
          //     : value === 0
          //     ? "0%"
          //     : "";
          // },
        },
        {
          prop: "生成时间",
          label: this.$t("生成时间"),
        },


      ];

      return columns;
    },
    searchColumns() {
      return [
        {
          prop: "yearMonth",
          label: this.$t("ui.data.column.report.proSizeSummary.yearMonth"),
          type: "date",
          dateType: "month",
          valueFormat: "yyyy-MM",
          clearable: false,
        },

        {
          label: this.$t("工厂"),
          prop: "areaID",
          type: "select",
        },
        {
          label: this.$t("产品分类"),
          prop: "brand",
          type: "select",
        },

        {
          label: this.$t("NC物料编码"),
          prop: "productDesc",
        },
        {
          label: this.$t("物料描述"),
          prop: "productDesc",
        },
      ];
    },
  },
  methods: {
    handleSearch(data) {
      this.query = data;
      this.$set(this.page, "current", 1);
      this.getList();
    },
    handlePageChange(current, pageSize) {
      this.$set(this.page, "current", current);
      this.$set(this.page, "pageSize", pageSize);
      this.getList();
    },
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
      this.getList();
    },
    handleExport() {
      exportProSizeSummary(this.formatParams(false));
    },

    // utils
    setSum(data) {
      if (data.length === 0) {
        this.stat = {};
        return;
      }
      const map = {};
      const keys = Object.keys(data[0]);
      data.forEach((item) => {
        keys.forEach((key) => {
          if (item[key] && !isNaN(item[key])) {
            if (map[key]) {
              map[key] = Big(map[key]).plus(item[key]).toString();
            } else {
              map[key] = item[key];
            }
          }
        });
      });

      if (map.proFinishQty && map.proPlanQty) {
        map.proFinishRate =
          Big(map.proFinishQty)
            .div(map.proPlanQty)
            .times(100)
            .round(2)
            .toString() + "%";
      }
      if (map.saleFinishQty && map.salePlanQty) {
        map.saleFinishRate =
          Big(map.saleFinishQty)
            .div(map.salePlanQty)
            .times(100)
            .round(2)
            .toString() + "%";
      }

      this.stat = map;
      console.log(map);
    },
    getSummaryMethod(param) {
      const { columns, data } = param;
      const sums = [];
      columns.forEach((column, index) => {
        if (column.property === "proSize") {
          sums[index] = "合计";
          return;
        } else {
          sums[index] = this.stat[column.property]
            ? this.stat[column.property]
            : "";
        }
      });

      return sums;
    },
    objectSpanMethod({ row, column, rowIndex, columnIndex }) {
      // if (column.property === "product") {
      //   if (rowIndex % 5 === 0) {
      //     return {
      //       rowspan: 5,
      //       colspan: 1,
      //     };
      //   } else {
      //     return {
      //       rowspan: 0,
      //       colspan: 0,
      //     };
      //   }
      // }
    },
    formatParams(hasPage = true) {
      const params = {
        ...this.query,
        ...this.sort,
      };

      if (hasPage) {
        // params.pageSize = this.page.pageSize;
        // params.pageNum = this.page.current;
      }
      if (params.yearMonth) {
        let arr = params.yearMonth.split("-");
        params.year = arr[0];
        params.month = arr[1];
        params.yearMonth = undefined;
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
        let list = [
          {
            年月: "20025-11",
            需求版本号: "",
            订单数量: "60",
            库存总数: "121",
            库存分配量: "60",
            月底计划余量分配量: "1260",
            订单类型: "周期排产储备",
            产品品类: "TBR",
            内外销: "外销",
            储备数量: "1465",
            适销区域: "非洲，东南亚",
            近3个月月均销量: "1688",
            近6个月月均销量: "1538",
            滚动12个月销售频次: "450",
            滚动12个月结构上机频次: "248",
            超3个月库存: "12",
            超6个月库存: "0",
            超12个月库存: "0",
            备库上限: "25",
            工厂: "116",
            年份: "2025",
            月份: "11",
            产品分类: "导向",
            订单优先级: "高优先级",
            区域: "北美",
            品牌: "AMULET",
            客户: "Join",
            产品结构: "ST235/80R16",
            主花纹: "AT505",
            NC物料编码: "3302002547",
            物料描述: "ST235/80R16 129/125M 14PR AT505 BL3HAM",
            排产分类: "周期排产",
            年周号: "2586",
            均匀性: "是",
            动平衡: "是",
            订单量: "1247",
            库存: "256",
            月底余量: "158",
            排产净需求: "3425",
            是否排产: "是",
            净需求: "3425",
            净需求No: "3425",
            高优先级: "300",
            中优先级: "120",
            暂缓订单: "10",
            周期排产储备: "3000",
            常规排产储备: "2000",
            是否满足最小投产量: "是",
            最小投产量值: "1000",
            备注: "",
            更新日期: "2025-11-20",
            数量: "10",
            客户国别: "越南",
            目的国: "澳大利亚",
            PO号: "JT25137-140XDW",
            发货模式: "分批交货",
            供应链优先级: "高",
            提报日期: "2025-11-20",
            EUDR: "29878",
            订单数: "60",
            计划满足: "0",
            未满足: "0",
            月计划: "321",
            计划累计: "145",
            硫化产量: "30",
            差异累计: "25",
            计划余量: "294",
            日期: "计划",
            库存数量:'300',
            是否超3个月胎:'否',
            是否超6个月胎:'否',
            是否超12个月胎:'否',
            T月:'700',
            T1月:'500',
            T2月:'100',
            生成时间:'2025-11',
          },
          {
            年月: "20025-11",
            需求版本号: "",
            订单数量: "80",
            库存总数: "121",
            库存分配量: "61",
            月底计划余量分配量: "160",
            订单类型: "周期排产储备",
            产品品类: "TBR",
            内外销: "外销",
            储备数量: "1465",
            适销区域: "非洲，越南",
            近3个月月均销量: "1688",
            近6个月月均销量: "1538",
            滚动12个月销售频次: "450",
            滚动12个月结构上机频次: "248",
            超3个月库存: "12",
            超6个月库存: "0",
            超12个月库存: "0",
            备库上限: "25",
            工厂: "116",
            年份: "2025",
            月份: "11",
            产品分类: "导向",
            订单优先级: "高优先级",
            区域: "环亚太",
            品牌: "AMULET",
            客户: "Join",
            产品结构: "11R22.5-JD571",
            主花纹: "JF568",
            NC物料编码: "330201108",
            物料描述: "295/80R22.5 152/149J 18PR JD756 BL4HJY",
            排产分类: "按单产",
            年周号: "3425",
            均匀性: "是",
            动平衡: "是",
            订单量: "1247",
            库存: "256",
            月底余量: "158",
            排产净需求: "3425",
            是否排产: "是",
            净需求: "3425",
            净需求No: "3425",
            高优先级: "300",
            中优先级: "120",
            暂缓订单: "10",
            周期排产储备: "3000",
            常规排产储备: "2000",
            是否满足最小投产量: "是",
            最小投产量值: "1000",
            备注: "",
            更新日期: "2025-11-20",
            数量: "10",
            客户国别: "越南",
            目的国: "索马里",
            PO号: "ABS2507C",
            发货模式: "整单发货",
            供应链优先级: "高",
            提报日期: "2025-11-20",
            EUDR: "29878",
            订单数: "61",
            计划满足: "19",
            未满足: "0",
            月计划: "250",
            计划累计: "30",
            硫化产量: "20",
            差异累计: "15",
            计划余量: "16",
            日期: "计划",
            库存数量:'800',
            是否超3个月胎:'否',
            是否超6个月胎:'否',
            是否超12个月胎:'否',
            T月:'500',
            T1月:'400',
            T2月:'200',
            生成时间:'2025-11',
          },
          {
            年月: "20025-11",
            需求版本号: "",
            订单数量: "140",
            库存总数: "246",
            库存分配量: "140",
            月底计划余量分配量: "160",
            订单类型: "常规储备",
            产品品类: "PCR",
            内外销: "外销",
            储备数量: "1465",
            适销区域: "非洲，越南",
            近3个月月均销量: "1688",
            近6个月月均销量: "1538",
            滚动12个月销售频次: "450",
            滚动12个月结构上机频次: "248",
            超3个月库存: "12",
            超6个月库存: "0",
            超12个月库存: "0",
            备库上限: "25",
            工厂: "116",
            年份: "2025",
            月份: "11",
            产品分类: "驱动",
            订单优先级: "高优先级",
            区域: "非洲",
            品牌: "AMULET",
            客户: "Join",
            产品结构: "ST235/80R16",
            主花纹: "AT505",
            NC物料编码: "3302002547",
            物料描述: "295/80R22.5 152/149J 18PR JD756 BL4HJY",
            排产分类: "常规产品",
            年周号: "0000",
            均匀性: "是",
            动平衡: "是",
            订单量: "1247",
            库存: "256",
            月底余量: "158",
            排产净需求: "3425",
            是否排产: "是",
            净需求: "3425",
            净需求No: "3425",
            高优先级: "300",
            中优先级: "120",
            暂缓订单: "10",
            周期排产储备: "3000",
            常规排产储备: "2000",
            是否满足最小投产量: "是",
            最小投产量值: "1000",
            备注: "",
            更新日期: "2025-11-20",
            数量: "10",
            客户国别: "越南",
            目的国: "乌拉圭",
            PO号: "JT25137-140XDW",
            发货模式: "整单发货",
            供应链优先级: "高",
            提报日期: "2025-11-20",
            EUDR: "29878",
            订单数: "140",
            计划满足: "0",
            未满足: "0",
            月计划: "321",
            计划累计: "58",
            硫化产量: "30",
            差异累计: "25",
            计划余量: "294",
            日期: "计划",
            库存数量:'468',
            是否超3个月胎:'否',
            是否超6个月胎:'否',
            是否超12个月胎:'否',
            T月:'8200',
            T1月:'600',
            T2月:'300',
            生成时间:'2025-11',
          },
          {
            年月: "20025-11",
            需求版本号: "",
            订单数量: "140",
            库存总数: "246",
            库存分配量: "140",
            月底计划余量分配量: "160",
            订单类型: "常规储备",
            产品品类: "PCR",
            内外销: "外销",
            储备数量: "1465",
            适销区域: "非洲，越南",
            近3个月月均销量: "1688",
            近6个月月均销量: "1538",
            滚动12个月销售频次: "450",
            滚动12个月结构上机频次: "248",
            超3个月库存: "12",
            超6个月库存: "0",
            超12个月库存: "0",
            备库上限: "25",
            工厂: "116",
            年份: "2025",
            月份: "11",
            产品分类: "驱动",
            订单优先级: "高优先级",
            区域: "非洲",
            品牌: "AMULET",
            客户: "Join",
            产品结构: "ST235/80R16",
            主花纹: "AT505",
            NC物料编码: "3302002547",
            物料描述: "12.00R20 158/155J 22PR JD756 BT0HJY",
            排产分类: "常规产品",
            年周号: "0000",
            均匀性: "是",
            动平衡: "是",
            订单量: "1247",
            库存: "256",
            月底余量: "158",
            排产净需求: "3425",
            是否排产: "是",
            净需求: "3425",
            净需求No: "3425",
            高优先级: "300",
            中优先级: "120",
            暂缓订单: "10",
            周期排产储备: "3000",
            常规排产储备: "2000",
            是否满足最小投产量: "是",
            最小投产量值: "1000",
            备注: "",
            更新日期: "2025-11-20",
            数量: "10",
            客户国别: "越南",
            目的国: "乌拉圭",
            PO号: "SOM-2501",
            发货模式: "整单发货",
            供应链优先级: "高",
            提报日期: "2025-11-20",
            EUDR: "29878",
            订单数: "140",
            计划满足: "0",
            未满足: "0",
            月计划: "600",
            计划累计: "0",
            硫化产量: "602",
            差异累计: "25",
            计划余量: "294",
            日期: "差异",
            库存数量:'500',
            是否超3个月胎:'否',
            是否超6个月胎:'否',
            是否超12个月胎:'否',
            T月:'200',
            T1月:'200',
            T2月:'300',
            生成时间:'2025-11',

          },
        ];
        this.data = list;

        this.page.total = 4;
        // const res = await listProSizeSummary(this.formatParams());
        // // console.log()

        // this.data = res.rows;

        // this.setSum(res.rows);

        // this.page.total = data.total;
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
    },
  },
  created() {
    const date = moment();
    this.search = {
      yearMonth: date.format("yyyy-MM"),
      factoryCode: "",
    };
    this.query = {
      yearMonth: date.format("yyyy-MM"),
      factoryCode: "",
    };
  },
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
</style>
<style lang="scss" scoped>
.stat-info {
  font-size: 12px;
  color: #5f5858;
  span {
    margin-left: 5px;
  }
}
</style>