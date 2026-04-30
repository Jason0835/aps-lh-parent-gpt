
<template>
  <basic-container>
    <page-table
      tableRef="lineSideStockMainTable"
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
          v-hasPermi="['cd90:lineSideStock:syncStock']"
          @click="handleSync"
          >{{ $t("ui.data.column.stock.sync") }}</el-button
        >
        <el-button @click="handleExport" v-hasPermi="['cd90:lineSideStock:export']">{{
          $t("ui.frame.btn.export")
        }}</el-button>
      </template>
    </page-table>
  </basic-container>
</template>
<script>
//lib
// import moment from "moment";
//utils
import { downloadLink } from "@/utils/request";
//interface
import { listLineSideStock, syncLineSideStock } from "@/api/cd90/lineSideStock";

export default {
 name: "LineSideStock",
  components: {
    // tltUpload,
  },
  dicts: [],
  provide() {
    return {
      parentDict: this.dict,
    };
  },
  data() {
    return {
      searchColumns: [
        {
          label: this.$t("ui.data.column.stock.stockDate"),
          prop: "stockDate",
        type: "date",
          dateType: "daterange",
          valueFormat: 'yyyy-MM-dd'
        },
        {
          label: this.$t("ui.data.column.badStock.materialCode"),
          prop: "materialCode",
        },
      ],
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
        { type: "selection", fixed: "left" },
        {
          prop: "stockDate",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.stock.stockDate"),
          // sortable: "custom",
        },
        {
          prop: "machineName",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.machine.machineName"),
          // sortable: "custom",
        },
        {
          prop: "materialCode",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.badStock.materialCode"),
          // sortable: "custom",
        },
        {
          prop: "barCode",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.badStock.roll.barcode"),
          // sortable: "custom",
        },

        {
          prop: "stockNum",
          align: "right",
          halign: "center",
          label: this.$t("ui.data.column.stock.stockNum"),
          // sortable: "custom",
        },
        {
          prop: "updateTime",
          align: "right",
          halign: "center",
          width: 180,
          label: this.$t("ui.data.column.stock.syncTime"),
          // sortable: "custom",
        },
        {
          prop: "remark",
          halign: "center",
          label: this.$t("ui.common.column.remark"),
          minWidth: 100,
          // sortable: "custom",
        },
      ];

      return columns;
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
    handleSync() {
      this.$confirm(this.$t("ui.data.column.stock.sync"), {
        type: "warning",
      }).then(() => {
        this.loading = true;
        syncLineSideStock()
          .then((data) => {
            this.$modal.msgSuccess(data.msg);
            this.$set(this.page, "current", 1);
            this.getList();
          })
          .catch((error) => {
            console.log(error);
            this.loading = false;
          });
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
      downloadLink("/cd90/stock/export", this.formatParams(false));
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

      if (params.stockDate && params.stockDate[0]) {
        params.startTime = params.stockDate[0];
        params.endTime = params.stockDate[1];
        params.stockDate = undefined;
      }

      return params;
    },
    // api
    async getList() {
      try {
        this.loading = true;
        const data = await listLineSideStock(this.formatParams());
        this.data = data.rows;
        this.page.total = data.total;
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
    },
  },
  created() {},
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
