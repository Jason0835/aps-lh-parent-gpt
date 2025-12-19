<template>
  <basic-container>
    <page-table
      tableRef="productionMouldConfigurationMainTable"
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
      <template slot="header">
        <!-- <el-button
          type="primary"
          plain
          v-hasPermi="['monthplan:productionMouldConfiguration:add']"
          @click="handleAdd"
          >{{ $t("SCM抓取") }}
        </el-button> -->
       <el-button
          type="primary"
          plain
          v-hasPermi="['monthplan:productionMouldConfiguration:add']"
          @click="handleAdd"
          >{{ $t("ui.frame.btn.add") }}
        </el-button>
        <el-button
          type="danger"
          plain
          :disabled="selection.length == 0"
          v-hasPermi="['monthplan:productionMouldConfiguration:remove']"
          @click="handleDelete(selection)"
          >{{ $t("ui.frame.btn.delete") }}
        </el-button>
        <el-button
          type="primary"
          plain
          >{{ $t("生成周期排产储备") }}
        </el-button>
        <el-button
          type="primary"
          plain
          >{{ $t("生成常规储备") }}
        </el-button>
        <!-- <el-button
          v-hasPermi="['monthplan:productionMouldConfiguration:import']"
          @click="$refs.tltUpload.handleImport()"
          >{{ $t("ui.frame.btn.import") }}
        </el-button> -->
        <el-button
          @click="handleExport"
          v-hasPermi="['monthplan:productionMouldConfiguration:export']"
          >{{ $t("ui.frame.btn.export") }}
        </el-button>
      </template>
    </page-table>
    <!-- <el-button style="display: none" ref="hidePopoverBtnRef"></el-button> -->
    <tlt-upload
      ref="tltUpload"
      downloadUrl="/monthplan/productionMouldConfiguration/importTemplate"
      uploadUrl="/monthplan/productionMouldConfiguration/importData"
      @uploadSuccess="getList"
    />
    <infoDialog ref="infoRef" @success="getList" />
  </basic-container>
</template>
<script>
import { downloadLink } from "@/utils/request";
import {
  listProductionMouldConfiguration,
  editProductionMouldConfiguration,
  removeProductionMouldConfiguration,
  buildMouldingProduct,
} from "@/api/monthplan/productionMouldConfiguration";
import tltUpload from "@/components/tltUpload/tltUpload.vue";

import infoDialog from "./components/infoDialog.vue";

export default {
  name: "RegionalCapacityAllocation",
  components: {
    tltUpload,
    infoDialog,
  },
  dicts: [],
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
      search: {
        mainPlanMonth: "",
      },
      query: {
        mainPlanMonth: "",
      },
      importDefaultValue: {},
      importRules: {},
    };
  },
  computed: {
    columns() {
      let columns = [
        { type: "selection", fixed: "left" },
        // {
        //   prop: "year",
        //   label: this.$t("ui.data.column.productionMouldConfiguration.year"),
        // },
        // {
        //   prop: "month",
        //   label: this.$t("ui.data.column.productionMouldConfiguration.month"),
        // },
        {
          prop: "订单类型",
          label: this.$t("订单类型"),
        },
        {
          prop: "工厂",
          label: this.$t("工厂"),
        },
        {
          prop: "产品品类",
          label: this.$t("产品品类"),
        },
        {
          prop: "内外销",
          label: this.$t("内外销"),
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
          prop: "产品分类",
          label: this.$t("产品分类"),
        },
        {
          prop: "储备数量",
          label: this.$t("储备数量"),
        },
        {
          prop: "适销区域",
          label: this.$t("适销区域"),
        },
        {
          prop: "近3个月月均销量",
          label: this.$t("近3个月月均销量"),
        },
        {
          prop: "近6个月月均销量",
          label: this.$t("近6个月月均销量"),
        },
        {
          prop: "滚动12个月销售频次",
          label: this.$t("滚动12个月销售频次"),
        },
        {
          prop: "滚动12个月结构上机频次",
          label: this.$t("滚动12个月结构上机频次"),
        },
        {
          prop: "超3个月库存",
          label: this.$t("超3个月库存"),
        },
        {
          prop: "超6个月库存",
          label: this.$t("超6个月库存"),
        },
        {
          prop: "超12个月库存",
          label: this.$t("超12个月库存"),
        },
        {
          prop: "备库上限",
          label: this.$t("备库上限"),
        },
        {
          prop: "更新日期",
          label: this.$t("更新日期"),
        },
      ];

      return columns;
    },
    searchColumns() {
      return [
      {
          prop: "mouldCode",
          label: this.$t("工厂"),
          type: "select",
        },
        {
          prop: "mouldCode",
          label: this.$t("产品分类"),
          type: "select",
        },
        {
          prop: "mouldCode",
          label: this.$t("订单类型"),
          type: "select",
        },
        {
          prop: "mouldCode",
          label: this.$t("NC物料编码"),
          type: "select",
        },
        {
          prop: "mouldCode",
          label: this.$t("区域"),
        },
        // {
        //   label: this.$t("提报日期"),
        //   prop: "date",
        //   type: "date",
        //   dateType: "date",
        //   valueFormat: "yyyy-MM-dd",
        // },
        // {
        //   prop: "mouldCode",
        //   label: this.$t("商品"),
        // },
        {
          prop: "mouldCode",
          label: this.$t("物料描述"),
        },
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
    handleDelete(rows) {
      this.$confirm(this.$t("common.confirm.delete"), {
        type: "warning",
      }).then(() => {
        // const ids = rows.map((row) => row.id).join(",");
        // console.log(ids);
        // removeProductionMouldConfiguration({ ids }).then((data) => {
        //   this.$modal.msgSuccess(data.msg);
        //   this.$set(this.page, "current", 1);
        //   this.getList();
        // });
      });
    },
    handleChangeStatus(status, row) {
      console.log(status);
      let label =
        status === "0"
          ? this.$t("ui.biz.alter.isOpen")
          : this.$t("ui.biz.alter.isStop");

      this.$confirm(label, {
        type: "warning",
      }).then(async () => {
        try {
          this.loading = true;
          const res = await editProductionMouldConfiguration({
            ...row,
            status,
          });
          this.$modal.msgSuccess(res.msg);
          this.getList();
        } catch (error) {
          this.loading = false;
        }
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
    handleExport() {
      downloadLink(
        "/monthplan/productionMouldConfiguration/export",
        this.formatParams(false)
      );
    },

    handleSelectionChange(rows) {
      this.selection = rows;
    },

    handleBuild() {
      if (this.$refs.buildRef) {
        this.$refs.buildRef.show();
      }
    },

    // utils
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
            订单类型:'周期排产储备',
            产品品类:'TBR',
            内外销:'外销',
            储备数量:'1465',
            适销区域:"非洲，东南亚",
            近3个月月均销量:'1688',
            近6个月月均销量:'1538',
            滚动12个月销售频次:'450',
            滚动12个月结构上机频次:'248',
            超3个月库存:'12',
            超6个月库存:'0',
            超12个月库存:'0',
            备库上限:'25',
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
            发货模式:'分批交货',
            供应链优先级:'高',
            提报日期:'2025-11-20',
            EUDR:'29878',
          },
          {
            订单类型:'周期排产储备',
            产品品类:'TBR',
            内外销:'外销',
            储备数量:'1465',
            适销区域:"非洲，越南",
            近3个月月均销量:'1688',
            近6个月月均销量:'1538',
            滚动12个月销售频次:'450',
            滚动12个月结构上机频次:'248',
            超3个月库存:'12',
            超6个月库存:'0',
            超12个月库存:'0',
            备库上限:'25',
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
            物料描述: "11R22.5 146/143L 16PR JD571 BL4HJY",
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
            PO号: "JT25137-140XDW",
            发货模式:'整单发货',
            供应链优先级:'高',
            提报日期:'2025-11-20',
            EUDR:'29878',
          },
          {
            订单类型:'常规储备',
            产品品类:'PCR',
            内外销:'外销',
            储备数量:'1465',
            适销区域:"非洲，越南",
            近3个月月均销量:'1688',
            近6个月月均销量:'1538',
            滚动12个月销售频次:'450',
            滚动12个月结构上机频次:'248',
            超3个月库存:'12',
            超6个月库存:'0',
            超12个月库存:'0',
            备库上限:'25',
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
            物料描述: "ST235/80R16 129/125M 14PR AT505 BL3HAM",
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
            发货模式:'整单发货',
            供应链优先级:'高',
            提报日期:'2025-11-20',
            EUDR:'29878',
          },
          {
            订单类型:'常规储备',
            产品品类:'PCR',
            内外销:'外销',
            储备数量:'1465',
            适销区域:"非洲，越南",
            近3个月月均销量:'1688',
            近6个月月均销量:'1538',
            滚动12个月销售频次:'450',
            滚动12个月结构上机频次:'248',
            超3个月库存:'12',
            超6个月库存:'0',
            超12个月库存:'0',
            备库上限:'25',
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
            物料描述: "ST235/80R16 129/125M 14PR AT505 BL3HAM",
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
            发货模式:'整单发货',
            供应链优先级:'高',
            提报日期:'2025-11-20',
            EUDR:'29878',
          },
        ];
        this.data = list;
        this.page.total = 4;
        // const data = await listProductionMouldConfiguration(
        //   this.formatParams()
        // );
        // console.log(data);
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
