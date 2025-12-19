
<template>
  <basic-container>
    <page-table
      tableRef="MonthlydataMpHistoryQtyMainTable"
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
        <el-button
          type="primary"
          plain
          v-hasPermi="['monthplan:mdmStockUpPlan:createStockUpPlan']"
          @click="handleCreate"
          >{{ $t("生成备货计划") }}</el-button
        >
        <!-- <el-button
          type="warning"
          v-hasPermi="['monthplan:mdmStockUpPlan:add']"
          @click="handleAdd"
          >{{ $t("ui.frame.btn.add") }}</el-button
        > -->
        <!-- <el-button
          type="warning"
          v-hasPermi="['monthplan:mdmStockUpPlan:edit']"
          @click="handleEdit(selection[0])"
          >{{ $t("ui.frame.btn.modify") }}</el-button
        > -->
        <!-- <el-button
          type="warning"
          v-hasPermi="['monthplan:mdmStockUpPlan:edit']"
          @click="handleDelete(selection)"
          >{{ $t("ui.frame.btn.delete") }}</el-button
        > -->
        <el-button
          v-hasPermi="['monthplan:mdmStockUpPlan:import']"
          @click="$refs.tltUpload.handleImport()"
          >{{ $t("ui.frame.btn.import") }}</el-button
        >
        <el-button
          @click="handleExport"
          v-hasPermi="['monthplan:mdmStockUpPlan:export']"
          >{{ $t("ui.frame.btn.export") }}</el-button
        >
      </template>
    </page-table>
    <!-- <el-button style="display: none" ref="hidePopoverBtnRef"></el-button> -->
    <tlt-upload
      ref="tltUpload"
      downloadUrl="/monthplan/mdmStockUpPlan/importTemplate"
      uploadUrl="/monthplan/mdmStockUpPlan/importData"
      @uploadSuccess="getList"
    />
    <!-- <infoDialog ref="infoRef" @success="getList" /> -->
  </basic-container>
</template>
<script>
//lib
import moment from "moment";
//utils
import { downloadLink } from "@/utils/request";
//interface
import {
  listMdmStockUpPlan,
  createStockUpPlan,
} from "@/api/monthplan/mdmStockUpPlan";
//components
import tltUpload from "@/components/tltUpload/tltUpload.vue";

// import infoDialog from "./components/infoDialog.vue";

export default {
  name: "MdmStockUpPlan",
  components: {
    tltUpload,
    // infoDialog,
  },
  dicts: ["biz_stor_type"],
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
    columns() {
      let columns = [
        // { type: "selection", fixed: "left" },
        {
          prop: "productCode",
          label: this.$t("ui.data.column.mdmStockUpPlan.productCode"),
        },
        {
          prop: "productDesc",
          label: this.$t("ui.data.column.mdmStockUpPlan.productDesc"),
          width: 250,
        },
        // {
        //   prop: "productDesc",
        //   label: this.$t("花纹"),
        // },
        {
          prop: "year",
          label: this.$t("ui.data.column.mouldusestatus.year"),
        },
        {
          prop: "month",
          label: this.$t("ui.data.column.mouldusestatus.month"),
        },
        {
          prop: "locationType",
          label: this.$t("ui.data.column.mdmStockUpPlan.locationType"),
          formatter: (row) => {
            return this.selectDictLabel(
              this.dict.type.biz_stor_type,
              row.locationType
            );
          },
        },
        {
          prop: "stockQty",
          label: this.$t("ui.data.column.mdmStockUpPlan.stockQty"),
          type: "number",
        },
        // {
        //   prop: "stockoist",
        //   label: this.$t("ui.data.column.mdmStockUpPlan.stockoist"),
        // },
        // {
        //   prop: "stockTime",
        //   label: this.$t("ui.data.column.mdmStockUpPlan.stockTime"),
        // },
        // {
        //   prop: "approver",
        //   label: this.$t("ui.data.column.mdmStockUpPlan.approver"),
        // },
        // {
        //   prop: "approveTime",
        //   label: this.$t("ui.data.column.mdmStockUpPlan.approveTime"),
        // },
        {
          prop: "remark",
          label: this.$t("common.remark"),
          minWidth: 100,
        },
        {
          prop: "createByName",
          label: this.$t("common.createByName"),
        },
        {
          prop: "createTime",
          label: this.$t("common.createTime"),
          width: 180,
        },
      ];

      return columns;
    },
    searchColumns() {
      return [
        {
          label: this.$t("ui.data.column.mouldusestatus.year"),
          prop: "year",
          type: "date",
          dateType: "year",
          valueFormat: "yyyy",
        },
        {
          label: this.$t("ui.data.column.mouldusestatus.month"),
          prop: "month",
          type: "date",
          dateType: "month",
          valueFormat: "MM",
        },
        {
          prop: "locationType",
          label: this.$t("库位类别"),
          type: "select",
          dictData: this.dict.type.biz_stor_type,
        },
        {
          prop: "productCode",
          label: this.$t("SAP代码"),
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
        const ids = rows.map((row) => row.id).join(",");
        console.log(ids);
        removeArea({ ids }).then((data) => {
          this.$modal.msgSuccess(data.msg);
          this.$set(this.page, "current", 1);
          this.getList();
        });
      });
    },

    handleCreate() {
      this.$router.push({
        path: "/monthlydata/mdmStockUpPlanCreate",
      });

    },
    handleHistoryQuery() {},

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
        "/monthplan/mdmStockUpPlan/export",
        this.formatParams(false)
      );
    },
    handleSelectionChange(rows) {
      this.selection = rows;
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
        const data = await listMdmStockUpPlan(this.formatParams());
        console.log(data);
        this.data = data.rows;
        this.page.total = data.total;
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
    },
  },
  created() {
    const date = moment();
    let defaultParams = {
      year: date.format("yyyy"),
      month: date.format("MM"),
    };
    this.search = {
      ...defaultParams,
    };

    this.query = {
      ...defaultParams,
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
