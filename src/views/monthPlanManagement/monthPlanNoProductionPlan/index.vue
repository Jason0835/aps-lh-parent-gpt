
<template>
  <basic-container>
    <page-table
      tableRef="MonthPlanNoProductionPlanMainTable"
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
          v-hasPermi="['monthplan:monthPlanNoProductionPlan:edit']"
          @click="handleEdit(selection[0])"
          >{{ $t("ui.frame.btn.modify") }}</el-button
        > -->
        <!-- <el-buttonW
          type="danger"
          plain
          :disabled="selection.length === 0"
          v-hasPermi="['monthplan:monthPlanNoProductionPlan:remove']"
          @click="handleDelete(selection)"
          >{{ $t("ui.frame.btn.delete") }}</el-button
        > -->
        <!-- <el-button
          v-hasPermi="['monthplan:monthPlanNoProductionPlan:import']"
          @click="$refs.tltUpload.handleImport()"
          >{{ $t("ui.frame.btn.import") }}</el-button
        > -->
        <el-button
          @click="handleExport"
          v-hasPermi="['monthplan:monthPlanNoProductionPlan:export']"
          >{{ $t("ui.frame.btn.export") }}</el-button
        >
      </template>
    </page-table>
    <!-- <el-button style="display: none" ref="hidePopoverBtnRef"></el-button> -->
    <tlt-upload
      ref="tltUpload"
      downloadUrl="/monthplan/monthPlanNoProductionPlan/importTemplate"
      uploadUrl="/monthplan/monthPlanNoProductionPlan/importData"
      @uploadSuccess="getList"
    />
    <infoDialog ref="infoRef" @success="getList" />
  </basic-container>
</template>
<script>
//lib
import moment from "moment";
//utils
import { downloadLink } from "@/utils/request";
//interface
import {
  listMonthPlanNoProductionPlan,
  removeMonthPlanNoProductionPlan,
  editMonthPlanNoProductionPlan,
} from "@/api/monthplan/monthPlanNoProductionPlan.js";
//components
import tltUpload from "@/components/tltUpload/tltUpload.vue";

import infoDialog from "./components/infoDialog.vue";

export default {
  name: "MonthPlanNoProductionPlan",
  components: {
    tltUpload,
    infoDialog,
  },
  dicts: [
    "biz_factory_name",
    "biz_stor_type",
    "biz_yes_no",
    "biz_channel_type",
    "biz_brand_type",
    "biz_construction_stage",
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
      dailyVisible: false,
      productionVersion: null,
    };
  },
  computed: {
    columns() {
      let columns = [
        // {
        //   label: this.$t("ui.data.column.mouldingDayResult.物料编码"),
        //   prop: "productCode",
        //   minWidth: 100,
        //   sortable: "custom",
        // },
        // {
        //   label: this.$t("ui.data.column.mouldingDayResult.物料描述"),
        //   prop: "productDesc",
        //   minWidth: 250,
        //   sortable: "custom",
        // },
        {
          label: this.$t("ui.data.column.mouldingDayResult.factoryCode"),
          prop: "factoryCode",
          minWidth: 100,
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_factory_name, value);
          },
        },
        {
          label: this.$t("ui.data.column.mouldingDayResult.year"),
          prop: "year",
          minWidth: 100,
          // sortable: "custom",
        },
        {
          label: this.$t("ui.data.column.mouldingDayResult.month"),
          prop: "month",
          minWidth: 100,
          // sortable: "custom",
        },
        {
          label: this.$t("ui.data.column.mouldingDayResult.constructionStage"),
          prop: "constructionStage",
          minWidth: 100,
          formatter: (row, column, value) => {
            return this.selectDictLabel(
              this.dict.type.biz_construction_stage,
              value
            );
          },
        },
        {
          label: this.$t("ui.data.column.mouldingDayResult.productCode"),
          prop: "productCode",
          minWidth: 100,
          // sortable: "custom",
        },
        {
          label: this.$t("ui.data.column.mouldingDayResult.productDesc"),
          prop: "productDesc",
          minWidth: 250,
          // sortable: "custom",
        },
        {
          label: this.$t("ui.data.column.mouldingDayResult.locationType"),
          prop: "locationType",
          minWidth: 100,
          // sortable: "custom",
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_stor_type, value);
          },
        },
        {
          label: this.$t("ui.data.column.mouldingDayResult.channel"),
          prop: "channel",
          minWidth: 100,
          // sortable: "custom",
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_channel_type, value);
          },
        },
        {
          label: this.$t("ui.data.column.mouldingDayResult.brand"),
          prop: "brand",
          minWidth: 100,
          // sortable: "custom",
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_brand_type, value);
          },
        },
        // {
        //   label: this.$t("ui.data.column.mouldingDayResult.施工号"),
        //   prop: "productDesc",
        //   minWidth: 100,
        //   sortable: "custom",
        // },
        {
          label: this.$t("ui.data.column.mouldingDayResult.proSize"),
          prop: "proSize",
          minWidth: 100,
          // sortable: "custom",
        },
        {
          label: this.$t("ui.data.column.mouldingDayResult.specifications"),
          prop: "specifications",
          minWidth: 100,
        },
        {
          label: this.$t("ui.data.column.mouldingDayResult.pattern"),
          prop: "pattern",
          minWidth: 140,
          // sortable: "custom",
        },
        ,
        // {
        //   label: this.$t("ui.data.column.mouldingDayResult.levelCode"),
        //   prop: "levelCode",
        //   minWidth: 100,
        //   sortable: "custom",
        // },
        // {
        //   label: this.$t("ui.data.column.mouldingDayResult.类型标识"),
        //   prop: "productDesc",
        //   minWidth: 100,
        //   sortable: "custom",
        // },
        // {
        //   label: this.$t("ui.data.column.mouldingDayResult.BOI"),
        //   prop: "productDesc",
        //   minWidth: 100,
        // },

        {
          label: this.$t("ui.data.column.mouldingDayResult.prodReqPlan"),
          prop: "prodReqPlan",
          minWidth: 100,
        },
        // {
        //   label: this.$t("ui.data.column.mouldingDayResult.备库计划"),
        //   prop: "productDesc",
        //   minWidth: 100,
        // },
        // {
        //   label: this.$t("ui.data.column.mouldingDayResult.预计超欠产"),
        //   prop: "productDesc",
        //   minWidth: 100,
        // },
        // {
        //   label: this.$t("ui.data.column.mouldingDayResult.理论生产需求计划"),
        //   prop: "productDesc",
        //   minWidth: 100,
        // },
        {
          label: this.$t("ui.data.column.mouldingDayResult.totalQty"),
          prop: "totalQty",
          minWidth: 100,
        },
        {
          label: this.$t("ui.data.column.mouldingDayResult.factProdReqQty"),
          prop: "factProdReqQty",
          minWidth: 100,
        },
        {
          label: this.$t("ui.data.column.mouldingDayResult.differenceQty"),
          prop: "unProductionQty",
          minWidth: 100,
        },

        // {
        //   label: this.$t("ui.data.column.mouldingDayResult.成型机编号"),
        //   prop: "productDesc",
        //   minWidth: 100,
        // },
        // {
        //   label: this.$t("ui.data.column.mouldingDayResult.mouldNo"),
        //   prop: "mouldNo",
        //   minWidth: 100,
        // },
        // {
        //   label: this.$t("ui.data.column.mouldingDayResult.mouldQty"),
        //   prop: "mouldQty",
        //   minWidth: 100,
        // },
        // {
        //   label: this.$t("ui.data.column.mouldingDayResult.beginDate"),
        //   prop: "beginDate",
        //   minWidth: 100,
        // },
        // {
        //   label: this.$t("ui.data.column.mouldingDayResult.endDay"),
        //   prop: "endDay",
        //   minWidth: 100,
        // },
        {
          label: this.$t("ui.data.column.mouldingDayResult.reason"),
          prop: "reason",
          minWidth: 240,
        },
        {
          label: this.$t("common.remark"),
          prop: "remark",
          minWidth: 200,
          // sortable: "custom",
        },
      ];
      if (this.dailyVisible) {
        //显示每日数据
        const date = moment(this.query.mainPlanMonth);
        // const year = date.year();
        const month = date.month() + 1;
        const days = date.daysInMonth();

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
      }
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
          label: this.$t("ui.data.column.mouldingDayResult.productCode"),
          prop: "productCode",
        },
        {
          label: this.$t("ui.data.column.mouldingDayResult.productDesc"),
          prop: "productDesc",
        },
        {
          prop: "locationType",
          label: this.$t(
            "ui.data.column.LocationChannelConfiguration.locationType"
          ),
          type: "select",
          dictData: this.dict.type.biz_stor_type,
        },
        {
          label: this.$t("ui.data.column.mouldingDayResult.channel"),
          prop: "channel",
          type: "select",
          dictData: this.dict.type.biz_channel_type,
        },
        {
          label: this.$t("ui.data.column.mouldingDayResult.brand"),
          prop: "brand",
          type: "select",
          dictData: this.dict.type.biz_brand_type,
        },
        {
          label: this.$t("ui.data.column.mouldingDayResult.specifications"),
          prop: "specifications",
        },
        {
          label: this.$t("ui.data.column.mouldingDayResult.proSize"),
          prop: "proSize",
        },
        {
          label: this.$t("ui.data.column.mouldingDayResult.pattern"),
          prop: "pattern",
        },
        // {
        //   label: this.$t("ui.data.column.mouldingDayResult.mouldNo"),
        //   prop: "mouldNo",
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
        "/monthplan/monthPlanNoProductionPlan/export",
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
        const data = await listMonthPlanNoProductionPlan(this.formatParams());
        console.log(data);
        this.data = data.rows;
        this.page.total = data.total;
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
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
</style>
