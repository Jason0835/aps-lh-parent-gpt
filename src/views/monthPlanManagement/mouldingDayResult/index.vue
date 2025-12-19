<template>
  <basic-container>
    <page-table
      tableRef="MouldingDayResultMainTable"
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
          v-hasPermi="['monthplan:mouldingDayResult:edit']"
          @click="handleEdit(selection[0])"
          >{{ $t("ui.frame.btn.modify") }}</el-button
        > -->
        <!-- <el-buttonW
          type="danger"
          plain
          :disabled="selection.length === 0"
          v-hasPermi="['monthplan:mouldingDayResult:remove']"
          @click="handleDelete(selection)"
          >{{ $t("ui.frame.btn.delete") }}</el-button
        > -->
        <!-- <el-button
          v-hasPermi="['monthplan:mouldingDayResult:import']"
          @click="$refs.tltUpload.handleImport()"
          >{{ $t("ui.frame.btn.import") }}</el-button
        > -->
        <el-button
          @click="handleExport"
          v-hasPermi="['monthplan:mouldingDayResult:export']"
          >{{ $t("ui.frame.btn.export") }}</el-button
        >
      </template>
      <template slot="headerRight">
        <span class="stat-info">
          <span
            >排产SAP个数:
            <span class="stat-value"> {{ stat.productionCount }} </span></span
          >
          <span
            >未排SAP总量:
            <span class="stat-value">{{ stat.noProductionCount }}</span></span
          >
          <span
            >已排SAP总量:
            <span class="stat-value">{{ stat.productionSum }}</span></span
          >
          <span
            >提报的SAP个数:
            <span class="stat-value">{{ stat.reportCount }}</span></span
          >
          <span
            >提报的SAP总量:
            <span class="stat-value">{{ stat.reportSum }}</span></span
          >
          <span
            >备货量: <span class="stat-value">{{ stat.stockNum }}</span></span
          >
        </span>
      </template>
    </page-table>
    <!-- <el-button style="display: none" ref="hidePopoverBtnRef"></el-button> -->
    <tlt-upload
      ref="tltUpload"
      downloadUrl="/monthplan/mouldingDayResult/importTemplate"
      uploadUrl="/monthplan/mouldingDayResult/importData"
      @uploadSuccess="getList"
    />
    <infoDialog ref="infoRef" @success="getList" />
    <specDialog ref="specRef" @success="getList" />
  </basic-container>
</template>
<script>
//lib
import moment from "moment";
//utils
import { downloadLink } from "@/utils/request";
//interface
import {
  listMouldingDayResult,
  removeNouldingDayResult,
  editMouldingDayResult,
  getVersionList,
  statistics,
} from "@/api/monthplan/mouldingDayResult";
//components
import tltUpload from "@/components/tltUpload/tltUpload.vue";

import infoDialog from "./components/infoDialog.vue";
import specDialog from "./components/specDialog.vue";

export default {
  name: "MouldingDayResult",
  components: {
    tltUpload,
    infoDialog,
    specDialog,
  },
  dicts: [
    "biz_factory_name",
    "biz_construction_stage",
    "biz_stor_type",
    "biz_yes_no",
    "biz_channel_type",
    "biz_brand_type",
  ],
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
      verList: [],
      dailyVisible: true,
      productionVersion: null,
      stat: {},
    };
  },
  computed: {
    columns() {
      let columns = [
        {
          label: this.$t("产品状态"),
          prop: "productStatus",
          minWidth: 100,
        },
        {
          label: this.$t("产品结构"),
          prop: "productStructure",
          minWidth: 100,
        },
        {
          label: this.$t("主物料"),
          prop: "mainMaterial",
          minWidth: 100,
        },
        {
          label: this.$t("物料描述"),
          prop: "materialDescription",
          minWidth: 100,
        },
        {
          label: this.$t("花纹"),
          prop: "pattern",
          minWidth: 100,
        },
        {
          label: this.$t("型腔"),
          prop: "cavity",
          minWidth: 100,
        },
        {
          label: this.$t("活块"),
          prop: "movingBlock",
          minWidth: 100,
        },
        {
          label: this.$t("净需求"),
          prop: "netDemand",
          minWidth: 100,
        },
        {
          label: this.$t("高优先级"),
          prop: "highPriority",
          minWidth: 100,
        },
        {
          label: this.$t("月均销量"),
          prop: "monthlyAverageSales",
          minWidth: 100,
        },
        {
          label: this.$t("库销比"),
          prop: "inventorySalesRatio",
          minWidth: 100,
        },
        {
          label: this.$t("日硫化量"),
          prop: "dailyVulcanization",
          minWidth: 100,
        }
      ];
      if (this.dailyVisible) {
        const query = this.$route.query;
        // if (query.productionStartDate) {
        //   //
        //   let start = moment(query.productionStartDate);
        //   let end =  moment(query.productionStartDate).add(1 ,"M");

        //   let list = [];

        //   while(start.isBefore(end)) {
        //     list.push(start.format("DD"))
        //     start.add(1, 'd')
        //   }
        //   // console.log(list);
        //   for (let i = 0; i < list.length; i++) {
        //     let dayNumStr = list[i];
        //     columns.push({
        //       // label: `${i + 1}号`,
        //       label: this.$t("ui.data.column.mouldingDayResult.day", {
        //         day: Number(dayNumStr),
        //       }),
        //       prop: `day${i+1}`,
        //       minWidth: "80px",
        //       type: "number",
        //     });
        //   }

        // } else {
          //显示每日数据
          // const date = moment(this.query.yearMonth);
          // const year = date.year();
          // const month = date.month() + 1;
          const days = 31;

          for (let i = 0; i < days; i++) {
            columns.push({
              // label: `${i + 1}号`,
              label: this.$t("ui.data.column.mouldingDayResult.day", {
                day: i + 1,
              }),
              prop: `day${i + 1}`,
              minWidth: "80px",
              type: "number",
            });
          }
        // }
      }
      columns.push({
        label: this.$t("ui.data.column.facMonthPlan.isImport"),
        prop: "isImport",
        align: "center",
        formatter: (row) => {
          return this.selectDictLabel(this.dict.type.biz_yes_no, row.isImport);
        },
      });
      return columns;
    },
    searchColumns() {
      return [
        // {
        //   label: this.$t("ui.data.column.mouldingDayResult.yearMonth"),
        //   prop: "yearMonth",
        //   type: "date",
        //   dateType: "month",
        //   valueFormat: "yyyy-MM",
        //   clearable: false,
        // },
        {
          label: this.$t("产品结构"),
          prop: "productCode",
        },
        {
          label: this.$t("主物料"),
          prop: "specCode",
        },
        {
          label: this.$t("物料描述"),
          prop: "embryoCode",
        },
        {
          label: this.$t("花纹"),
          prop: "productDesc",
        },
        {
          prop: "locationType",
          label: this.$t(
            "产品状态"
          ),
          type: "select",
          dictData: this.dict.type.biz_stor_type,
        },
        {
          label: this.$t("规格"),
          prop: "channel",
          type: "select",
          dictData: this.dict.type.biz_channel_type,
        },
        {
          label: this.$t("物料编码"),
          prop: "brand",
        },
        {
          label: this.$t("产品分类"),
          prop: "specifications",
        },
        {
          label: this.$t("备注"),
          prop: "proSize",
        }
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
        const ids = rows.map((row) => row.id).join(",");
        removemouldingDayResult({ ids }).then((data) => {
          this.$modal.msgSuccess(data.msg);
          this.$set(this.page, "current", 1);
          this.getList();
        });
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
          const res = await editMouldingDayResult({
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
        "/monthplan/mouldingDayResult/export",
        this.formatParams(false)
      );
    },

    handleSelectionChange(rows) {
      this.selection = rows;
    },
    handleYearChange(params) {
      this.getVersionList(params);
      console.log(1);
    },
    handleMonthChange(params) {
      this.getVersionList(params);
    },
    handleFactoryChange(params) {
      this.getVersionList(params);
    },
    handleChangeSpecCode(row) {
      this.$refs.specRef.show(row);
    },

    // utils
    updateTableHeaderlabel() {
      //  TODO 更新表头标题
    },

    formatParams(hasPage = true) {
      const params = {
        ...this.query,
        ...this.sort,
        productionVersion: this.productionVersion,
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
        const mockData = [
  {
    // 基础信息字段
    productStatus: "正规",
    productStructure: "385/65R22.5",
    mainMaterial: "24PR JT560 BL4EJY",
    materialDescription: "24PR JT560 BL4EJY",
    pattern: "JT560",
    cavity: "8",
    movingBlock: "8",
    
    // 需求相关字段
    netDemand: "150",
    highPriority: "50",
    monthlyAverageSales: "",
    inventorySalesRatio: "0.5",
    dailyVulcanization: "46",
    
    // 每日数据字段（1-31号）
    day1: "",
    day2: "",
    day3: "",
    day4: "",
    day5: "",
    day6: "",
    day7: "",
    day8: "",
    day9: "",
    day10: "",
    day11: "",
    day12: "",
    day13: "",
    day14: "",
    day15: "",
    day16: "",
    day17: "",
    day18: "",
    day19: "",
    day20: "",
    day21: "",
    day22: "",
    day23: "",
    day24: "",
    day25: "",
    day26: "",
    day27: "",
    day28: "",
    day29: "",
    day30: "",
    day31: "",
    
    // 系统字段
    isImport: "0"
  }
];
this.data = mockData;
        this.page.total = 1;
        // this.statistics(this.formatParams(false));
        // const data = await listMouldingDayResult(this.formatParams());
        // console.log(data);
        // this.data = data.rows;
        // this.page.total = data.total;
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
    },
    async statistics(params) {
      try {
        const res = await statistics(params);
        console.log(res);
        this.stat = res;
      } catch (error) {
        this.stat = {};
      }
    },

    async getVersionList(params) {
      this.search = {
        ...this.query,
        ...params,
        monthPlanVersion: undefined,
      };

      if (!params.year || !params.month || !params.factoryCode) {
        return;
      }

      try {
        this.verList = [];

        const res = await getVersionList({
          year: this.search.year,
          month: this.search.month,
          factoryCode: this.search.factoryCode,
        });
        this.verList = res;

        console.log(this.verList);
      } catch (error) {
        console.error(error);
        this.verList = [];
      }
    },
  },
  created() {
    // const date = moment();
    // let defaultParams = {
    //   year: date.format("yyyy"),
    //   month: date.format("MM"),
    // };
    // this.search = {
    //   ...defaultParams,
    // };
    // this.query = {
    //   ...defaultParams,
    // };
    if (this.$route.params.id) {
      this.productionVersion = this.$route.params.id;
    }
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
.table-link {
  font-size: 11pt;
}
.stat-info {
  font-size: 12px;
  color: #676a6c;
  font-weight: bold;
  .stat-value {
    color: #0088cc;
  }
  span {
    margin-left: 5px;
  }
}
</style>