
<template>
  <basic-container>
    <page-table
      tableRef="cxFixedMachineMainTable"
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
      @selection-change="handleSelectionChange"
      :showSummary="false"
      :selectArea="false"
    >
      <!-- <template slot="header">
        <el-button
          type="primary"
          plain
          >{{ $t("MES抓取") }}</el-button
        >
        <el-button
          type="primary"
          plain
          v-hasPermi="['monthplan:ProductMoldingLimit:add']"
          @click="handleAdd"
          >{{ $t("ui.frame.btn.add") }}</el-button
        >
        <el-button
          type="warning"
          v-hasPermi="['monthplan:ProductMoldingLimit:edit']"
          :disabled="selection.length !== 1"
          @click="handleDelete(selection[0])"
          >{{ $t("ui.frame.btn.delete") }}</el-button
        >
        <el-button
          v-hasPermi="['monthplan:ProductMoldingLimit:import']"
          @click="$refs.tltUpload.handleImport()"
          >{{ $t("ui.frame.btn.import") }}</el-button
        >
        <el-button
          @click="handleExport"
          v-hasPermi="['monthplan:ProductMoldingLimit:export']"
          >{{ $t("ui.frame.btn.export") }}</el-button
        >
      </template> -->
    </page-table>
    <!-- <el-button style="display: none" ref="hidePopoverBtnRef"></el-button> -->
    <tlt-upload
      ref="tltUpload"
      downloadUrl="/mdm/productMoldingLimit/importTemplate"
      uploadUrl="/mdm/productMoldingLimit/importData"
      @uploadSuccess="getList"
    />
    <infoDialog ref="infoRef" @success="getList" />
  </basic-container>
</template>
<script>
//lib
import { mapState } from "vuex";
//utils
import { downloadLink } from "@/utils/request";

// import {
//   listProductMoldingLimit,
//   removeProductMoldingLimit,
// } from "@/api/mdm/productMoldingLimit";

//components
import tltUpload from "@/components/tltUpload/tltUpload.vue";

import infoDialog from "./components/infoDialog.vue";

export default {
  name: "vulcanizationTable",
  components: {
    tltUpload,
    infoDialog,
  },
  dicts: ["LINE_TYPE", "JOB_TYPE", "biz_factory_name"],
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
    };
  },
  computed: {
    ...mapState({
      moldingMachines: (state) => state.molding.machines,
    }),
    columns() {
      let columns = [
        { type: "selection", fixed: "left" },
        {
          prop: "产品结构",
          label: this.$t("产品结构"),
        },
        {
          prop: "NC物料编码",
          label: this.$t("NC物料编码"),
        },
        {
          prop: "主物料",
          label: this.$t("主物料"),
        },
        {
          prop: "主物料",
          label: this.$t("主物料"),
        },
        {
          prop: "模具数",
          label: this.$t("模具数"),
        },
        {
          prop: "净需求量",
          label: this.$t("净需求量"),
        },
        {
          prop: "上机日期",
          label: this.$t("上机日期"),
        },
        {
          prop: "不合格数据量",
          label: this.$t("不合格数据量"),
        },
        {
          prop: "累计生产量",
          label: this.$t("累计生产量"),
        },
        {
          prop: "硫化余量",
          label: this.$t("硫化余量"),
        },
        {
          prop: "预计收尾天数",
          label: this.$t("预计收尾天数"),
        },
        {
          prop: "预计收尾时间",
          label: this.$t("预计收尾时间"),
        },
        {
          prop: "计划收尾时间",
          label: this.$t("计划收尾时间"),
        },
        {
          prop: "差异天数",
          label: this.$t("差异天数"),
        },
      ];

      return columns;
    },
    searchColumns() {
      return [
        // {
        //   prop: "工厂",
        //   label: this.$t("工厂"),
        //   type: "select",
        // },
        // {
        //   prop: "型腔模号",
        //   label: this.$t("型腔模号"),
        //   type: "select",
        // },
        // {
        //   prop: "规格",
        //   label: this.$t("规格"),
        //   type: "select",
        // },
        // {
        //   prop: "物流状态",
        //   label: this.$t("物流状态"),
        // },
        // {
        //   prop: "主花纹",
        //   label: this.$t("主花纹"),
        // },
        // {
        //   prop: "模具类型",
        //   label: this.$t("模具类型"),
        //   type: "select",
        // },
        // {
        //   prop: "花纹代号",
        //   label: this.$t("花纹代号"),
        //   type: "select",
        // },
        // {
        //   prop: "模壳标准",
        //   label: this.$t("模壳标准"),
        //   type: "select",
        // },
        // {
        //   prop: "可用状态",
        //   label: this.$t("模套型号"),
        //   type: "select",
        // },
      ];
    },
  },
  methods: {
    handleAdd() {
      if (this.$refs.infoRef) {
        this.$refs.infoRef.show();
      }
    },
    handleEdit(row) {
      if (this.$refs.infoRef) {
        this.$refs.infoRef.show(row);
      }
    },
    handleDelete(row) {
      this.$confirm(this.$t("common.confirm.delete"), {
        type: "warning",
      }).then(() => {
        const ids = row.id;
        // removeProductMoldingLimit({ ids }).then((data) => {
        //   this.$modal.msgSuccess(data.msg);
        //   this.$set(this.page, "current", 1);
        //   this.getList();
        // });
      });
    },

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
    handelSuccess() {
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
    handleSelectionChange(rows) {
      this.selection = rows;
    },
    handleExport() {
      downloadLink("/mdm/productMoldingLimit/export", this.formatParams(false));
    },

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
            模具数: "12",
            不合格数据量: "0",
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
            主物料: "ST235/80R16 129/125M 14PR AT505 BL3HAM",
            排产分类: "周期排产",
            年周号: "2586",
            均匀性: "是",
            动平衡: "是",
            累计生产量: "1247",
            库存: "256",
            月底余量: "158",
            排产净需求: "3425",
            是否排产: "是",
            净需求: "3425",
            净需求量: "3425",
            高优先级: "300",
            中优先级: "120",
            暂缓订单: "10",
            周期排产储备: "3000",
            常规排产储备: "2000",
            是否满足最小投产量: "是",
            最小投产量值: "1000",
            备注: "",
            计划收尾时间: "2025-11-20",
            数量: "10",
            客户国别: "越南",
            目的国: "澳大利亚",
            PO号: "JT25137-140XDW",
            发货模式: "分批交货",
            供应链优先级: "高",
            上机日期: "2025-11-20",
            EUDR: "29878",
            订单数: "60",
            计划满足: "0",
            未满足: "0",
            月计划: "321",
            计划累计: "145",
            硫化余量: "30",
            差异累计: "25",
            计划余量: "294",
            日期: "计划",
            结构:'385/65R22.5-JY598零度',
            规格:'385/65R22.5',
            模具分配数:'6',
            预计收尾天数:'5',
            预计收尾时间:'2025-11-10',
            差异天数:'0',
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
            模具数: "12",
            不合格数据量: "0",
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
            主物料: "295/80R22.5 152/149J 18PR JD756 BL4HJY",
            排产分类: "按单产",
            年周号: "3425",
            均匀性: "是",
            动平衡: "是",
            累计生产量: "1247",
            库存: "256",
            月底余量: "158",
            排产净需求: "3425",
            是否排产: "是",
            净需求: "3425",
            净需求量: "3425",
            高优先级: "300",
            中优先级: "120",
            暂缓订单: "10",
            周期排产储备: "3000",
            常规排产储备: "2000",
            是否满足最小投产量: "是",
            最小投产量值: "1000",
            备注: "",
            计划收尾时间: "2025-11-20",
            数量: "10",
            客户国别: "越南",
            目的国: "索马里",
            PO号: "ABS2507C",
            发货模式: "整单发货",
            供应链优先级: "高",
            上机日期: "2025-11-20",
            EUDR: "29878",
            订单数: "61",
            计划满足: "19",
            未满足: "0",
            月计划: "250",
            计划累计: "30",
            硫化余量: "20",
            差异累计: "15",
            计划余量: "16",
            日期: "计划",
            结构:'385/65R22.5-JY598四层',
            规格:'385/65R22.5',
            模具分配数:'10',
            预计收尾天数:'5',
            预计收尾时间:'2025-11-10',
            差异天数:'0',
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
            模具数: "12",
            不合格数据量: "0",
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
            主花纹: "JY598",
            NC物料编码: "3302002547",
            主物料: "295/80R22.5 152/149J 18PR JD756 BL4HJY",
            排产分类: "常规产品",
            年周号: "0000",
            均匀性: "是",
            动平衡: "是",
            累计生产量: "1247",
            库存: "256",
            月底余量: "158",
            排产净需求: "3425",
            是否排产: "是",
            净需求: "3425",
            净需求量: "3425",
            高优先级: "300",
            中优先级: "120",
            暂缓订单: "10",
            周期排产储备: "3000",
            常规排产储备: "2000",
            是否满足最小投产量: "是",
            最小投产量值: "1000",
            备注: "",
            计划收尾时间: "2025-11-20",
            数量: "10",
            客户国别: "越南",
            目的国: "乌拉圭",
            PO号: "JT25137-140XDW",
            发货模式: "整单发货",
            供应链优先级: "高",
            上机日期: "2025-11-20",
            EUDR: "29878",
            订单数: "140",
            计划满足: "0",
            未满足: "0",
            月计划: "321",
            计划累计: "58",
            硫化余量: "30",
            差异累计: "25",
            计划余量: "294",
            日期: "计划",
            结构:'385/65R22.5-JY598零度',
            规格:'385/65R22.5',
            模具分配数:'24',
            预计收尾天数:'5',
            预计收尾时间:'2025-11-10',
            差异天数:'0',
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
            模具数: "12",
            不合格数据量: "0",
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
            主物料: "12.00R20 158/155J 22PR JD756 BT0HJY",
            排产分类: "常规产品",
            年周号: "0000",
            均匀性: "是",
            动平衡: "是",
            累计生产量: "1247",
            库存: "256",
            月底余量: "158",
            排产净需求: "3425",
            是否排产: "是",
            净需求: "3425",
            净需求量: "3425",
            高优先级: "300",
            中优先级: "120",
            暂缓订单: "10",
            周期排产储备: "3000",
            常规排产储备: "2000",
            是否满足最小投产量: "是",
            最小投产量值: "1000",
            备注: "",
            计划收尾时间: "2025-11-20",
            数量: "10",
            客户国别: "越南",
            目的国: "乌拉圭",
            PO号: "SOM-2501",
            发货模式: "整单发货",
            供应链优先级: "高",
            上机日期: "2025-11-20",
            EUDR: "29878",
            订单数: "140",
            计划满足: "0",
            未满足: "0",
            月计划: "600",
            计划累计: "0",
            硫化余量: "602",
            差异累计: "25",
            计划余量: "294",
            日期: "差异",
            结构:'385/65R22.5-JY598四层',
            规格:'385/65R22.5',
            模具分配数:'10',
            预计收尾天数:'5',
            预计收尾时间:'2025-11-10',
            差异天数:'0',
          },
        ];
        this.data = list;
        this.page.total = 4;
        // const data = await listProductMoldingLimit(this.formatParams());
        // this.data = data.rows;
        // this.page.total = data.total;
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
    },
  },
  created() {
    if (this.moldingMachines.length === 0) {
      this.$store.dispatch("molding/getMachineList");
    }
    this.getList();
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
