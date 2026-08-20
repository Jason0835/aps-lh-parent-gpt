<template>
  <basic-container>
    <page-table
      tableRef="lhSkuDecrementMainTable"
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
      @selection-change="handleSelectionChange"
      :showSummary="false"
      :selectArea="false"
    >
      <template slot="header">
        <el-button
          type="danger"
          v-hasPermi="['lh:skuDecrement:remove']"
          :disabled="selection.length === 0"
          @click="handleBatchDelete"
        >{{ $t("ui.frame.btn.delete") }}</el-button>
        <el-button
          v-hasPermi="['lh:skuDecrement:export']"
          @click="handleExport"
        >{{ $t("ui.frame.btn.export") }}</el-button>
      </template>
    </page-table>
  </basic-container>
</template>

<script>
import moment from "moment";
import { downloadLink } from "@/utils/request";
import { listLhSkuDecrement, removeLhSkuDecrement } from "@/api/lh/lhSkuDecrement";

export default {
  name: "LhSkuDecrement",
  dicts: ["biz_factory_name", "lh_trial_status"],
  data() {
    const now = moment();
    return {
      loading: false,
      data: [],
      selection: [],
      page: {
        current: 1,
        pageSize: 20,
        total: 0,
      },
      search: {
        factoryCode: "116",
        yearMonth: now.format("YYYY-MM"),
      },
      query: {
        factoryCode: "116",
        yearMonth: now.format("YYYY-MM"),
      },
    };
  },
  computed: {
    columns() {
      return [
        { type: "selection", fixed: "left" },
        {
          prop: "factoryCode",
          label: this.$t("ui.data.column.lhSkuDecrement.factoryCode"),
          minWidth: 120,
          formatter: (row, column, value) => this.selectDictLabel(this.dict.type.biz_factory_name, value),
        },
        {
          prop: "year",
          label: this.$t("ui.data.column.lhSkuDecrement.year"),
          minWidth: 80,
          align: "center",
        },
        {
          prop: "month",
          label: this.$t("ui.data.column.lhSkuDecrement.month"),
          minWidth: 80,
          align: "center",
        },
        {
          prop: "materialCode",
          label: this.$t("ui.data.column.lhSkuDecrement.materialCode"),
          minWidth: 140,
        },
        {
          prop: "materialDesc",
          label: this.$t("ui.data.column.lhSkuDecrement.materialDesc"),
          minWidth: 260,
          align: "left",
        },
        {
          prop: "embryoDesc",
          label: this.$t("ui.data.column.lhSkuDecrement.embryoDesc"),
          minWidth: 260,
          align: "left",
        },
        {
          prop: "productStatus",
          label: this.$t("ui.data.column.lhSkuDecrement.productStatus"),
          minWidth: 120,
          formatter: (row, column, value) => this.selectDictLabel(this.dict.type.lh_trial_status, value),
        },
        {
          prop: "updateBy",
          label: this.$t("ui.data.column.updateBy"),
          minWidth: 100,
        },
        {
          prop: "updateTime",
          label: this.$t("ui.data.column.updateTime"),
          minWidth: 170,
        },
        {
          prop: "option",
          label: this.$t("common.option"),
          width: 120,
          fixed: "right",
          align: "center",
          render: ({ row }) => {
            return (
              <div>
                <el-button
                  v-hasPermi={["lh:skuDecrement:remove"]}
                  class="minus"
                  type="danger"
                  onClick={() => this.handleDelete(row)}
                >
                  {this.$t("ui.frame.btn.delete")}
                </el-button>
              </div>
            );
          },
        },
      ];
    },
    searchColumns() {
      return [
        {
          label: this.$t("ui.data.column.lhSkuDecrement.factoryCode"),
          prop: "factoryCode",
          type: "select",
          dictData: this.dict.type.biz_factory_name,
          filterable: true,
        },
        {
          label: this.$t("ui.data.column.lhSkuDecrement.yearMonth"),
          prop: "yearMonth",
          type: "date",
          dateType: "month",
          valueFormat: "yyyy-MM",
          minWidth: 150,
        },
        {
          label: this.$t("ui.data.column.lhSkuDecrement.materialCode"),
          prop: "materialCode",
          minWidth: 160,
        },
        {
          label: this.$t("ui.data.column.lhSkuDecrement.materialDesc"),
          prop: "materialDesc",
          minWidth: 220,
        },
        {
          label: this.$t("ui.data.column.lhSkuDecrement.embryoDesc"),
          prop: "embryoDesc",
          minWidth: 220,
        },
        {
          label: this.$t("ui.data.column.lhSkuDecrement.productStatus"),
          prop: "productStatus",
          type: "select",
          dictData: this.dict.type.lh_trial_status,
          filterable: true,
        },
      ];
    },
  },
  methods: {
    handleSearch(data) {
      this.query = { ...data };
      this.$set(this.page, "current", 1);
      this.getList();
    },
    handlePageChange(current, pageSize) {
      this.$set(this.page, "current", current);
      this.$set(this.page, "pageSize", pageSize);
      this.getList();
    },
    handleSelectionChange(rows) {
      this.selection = rows;
    },
    handleDelete(row) {
      this.$confirm(this.$t("common.confirm.delete"), {
        type: "warning",
      }).then(async () => {
        const data = await removeLhSkuDecrement({ ids: row.id });
        this.$modal.msgSuccess(data.msg);
        this.getList();
      });
    },
    handleBatchDelete() {
      this.$confirm(this.$t("common.confirm.delete"), {
        type: "warning",
      }).then(async () => {
        const ids = this.selection.map((item) => item.id).join(",");
        const data = await removeLhSkuDecrement({ ids });
        this.$modal.msgSuccess(data.msg);
        this.selection = [];
        this.getList();
      });
    },
    handleExport() {
      downloadLink("/lh/lhSkuDecrement/export", this.formatParams(false));
    },
    formatParams(hasPage = true) {
      const params = {
        ...this.query,
      };
      if (params.yearMonth) {
        const yearMonth = moment(params.yearMonth, "YYYY-MM");
        if (yearMonth.isValid()) {
          params.year = yearMonth.year();
          params.month = yearMonth.month() + 1;
        }
      }
      delete params.yearMonth;
      if (hasPage) {
        params.pageNum = this.page.current;
        params.pageSize = this.page.pageSize;
      }
      return params;
    },
    async getList() {
      try {
        this.loading = true;
        const data = await listLhSkuDecrement(this.formatParams());
        this.data = data.rows || [];
        this.page.total = data.total || 0;
      } finally {
        this.loading = false;
      }
    },
  },
  activated() {
    this.getList();
  },
};
</script>
