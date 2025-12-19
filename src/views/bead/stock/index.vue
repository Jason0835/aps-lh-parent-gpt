
<template>
  <basic-container>
    <page-table
      tableRef="beadStockMainTable"
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
          v-hasPermi="['tq:stock:add']"
          @click="handleAdd"
          >{{ $t("ui.frame.btn.add") }}</el-button
        >
        <el-button
          type="primary"
          plain
          v-hasPermi="['tq:stock:edit']"
          :disabled="selection.length !== 1"
          @click="() => handleEdit(selection[0])"
          >{{ $t("ui.frame.btn.modify") }}</el-button
        >
        <!-- <el-button
          type="warning"
          v-hasPermi="['tq:stock:stockRevise']"
          :disabled="selection.length !== 1"
          @click="() => handleModifyStock(selection[0])"
          >{{ $t("ui.frame.btn.stock.modify2") }}</el-button
        > -->
        <el-button
          type="danger"
          plain
          v-hasPermi="['tq:stock:remove']"
          @click="handleBatchDelete"
          >{{ $t("ui.frame.btn.delete") }}</el-button
        >
        <el-button
          v-hasPermi="['tq:stock:import']"
          @click="$refs.tltUpload.handleImport()"
          >{{ $t("ui.frame.btn.import") }}</el-button
        >
        <el-button @click="handleExport" v-hasPermi="['tq:stock:export']">{{
          $t("ui.frame.btn.export")
        }}</el-button>
      </template>
    </page-table>
    <tlt-upload
      ref="tltUpload"
      downloadUrl="/tq/stock/importTemplate"
      uploadUrl="/tq/stock/importData"
      @uploadSuccess="getList"
    />
    <infoDialog ref="infoRef" @success="getList" />
  </basic-container>
</template>
<script>
//lib
// import moment from "moment";
//utils
import { downloadLink } from "@/utils/request";
//interface
import { listStock, removeStock, releaseStock } from "@/api/tq/stock";
//components
import tltUpload from "@/components/tltUpload/tltUpload.vue";

import infoDialog from "./components/infoDialog.vue";

export default {
  name: "BeadStock",
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
      searchColumns: [
        {
          label: this.$t("ui.data.column.stock.stockDate"),
          prop: "stockDate",
          type: "date",
          dateType: "daterange",
          valueFormat: "yyyy-MM-dd",
        },
        {
          label: this.$t("ui.data.column.quota.beadCode"),
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
          prop: "materialCode",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.quota.beadCode"),
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
          prop: "modifyNum",
          align: "right",
          halign: "center",
          label: this.$t("ui.data.column.stock.modifyNum"),
          // sortable: "custom",
        },
        {
          prop: "badNum",
          align: "right",
          halign: "center",
          label: this.$t("ui.data.column.stock.badNum"),
          // sortable: "custom",
        },
        {
          prop: "remark",
          halign: "center",
          label: this.$t("ui.common.column.remark"),
          minWidth: 100,
          // sortable: "custom",
        },
        {
          align: "center",
          halign: "center",
          label: this.$t("ui.data.btn.option"),
          minWidth: 180,
          width: 180,
          fixed: "right",
          render: ({ row }) => {
            return (
              <div>
                <el-button
                  v-hasPermi={["tq:stock:stockRevise"]}
                  class="minus"
                  type="success"
                  onClick={() => this.handleModifyStock(row)}
                >
                  {this.$t("ui.frame.btn.stock.modify2")}
                </el-button>
                <el-button
                  v-hasPermi={["tq:stock:remove"]}
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

      return columns;
    },
  },
  methods: {
    handleAdd() {
      if (this.$refs.infoRef) {
        this.$refs.infoRef.show(null, "0");
      }
    },
    handleEdit(row) {
      if (this.$refs.infoRef) {
        this.$refs.infoRef.show(row, "1");
      }
    },
    handleDelete(row) {
      this.$confirm(this.$t("common.confirm.delete"), {
        type: "warning",
      }).then(() => {
        const ids = row.id;
        this.loading = true;
        removeStock({ ids })
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
    handleBatchDelete(row) {
      this.$confirm(this.$t("common.confirm.delete"), {
        type: "warning",
      }).then(() => {
        const ids = this.selection.map((row) => row.id).join(",");
        this.loading = true;
        removeStock({ ids })
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
      downloadLink("/tq/stock/export", this.formatParams(false));
    },
    handleModifyStock(row) {
      if (this.$refs.infoRef) {
        this.$refs.infoRef.show(row, "2");
      }
    },
    handleReleaseStock() {
      this.$confirm(this.$t("ui.biz.alter.makeSureReleaseStock")).then(
        async () => {
          try {
            this.loading = true;
            const ids = this.selection.map((row) => row.is).join(",");
            const res = await releaseStock({ ids });
            this.$modal.msgSuccess(res.msg);
            this.getList();
          } catch (error) {
            console.error(error);
            this.loading = false;
          }
        }
      );
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
        const data = await listStock(this.formatParams());
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
