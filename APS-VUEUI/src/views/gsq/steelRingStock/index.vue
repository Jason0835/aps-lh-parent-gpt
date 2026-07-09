<template>
  <basic-container>
    <page-table
      tableRef="gsqSteelRingStockMainTable"
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
          v-hasPermi="['gsq:steelRingStock:add']"
          @click="handleAdd"
        >{{ $t("ui.frame.btn.add") }}</el-button>
        <el-button
          type="danger"
          plain
          v-hasPermi="['gsq:steelRingStock:remove']"
          @click="handleBatchDelete"
        >{{ $t("ui.frame.btn.delete") }}</el-button>
        <el-button
          v-hasPermi="['gsq:steelRingStock:import']"
          @click="$refs.tltUpload.handleImport()"
        >{{ $t("ui.frame.btn.import") }}</el-button>
        <el-button
          @click="handleExport"
          v-hasPermi="['gsq:steelRingStock:export']"
        >{{ $t("ui.frame.btn.export") }}</el-button>
      </template>
    </page-table>
    <tlt-upload
      ref="tltUpload"
      downloadUrl="/gsq/steelRingStock/importTemplate"
      uploadUrl="/gsq/steelRingStock/importData"
      @uploadSuccess="getList"
    />
    <InfoDialog ref="infoRef" @success="getList" />
  </basic-container>
</template>
<script>
import {
  listSteelRingStock,
  removeSteelRingStock,
  exportSteelRingStock,
} from "@/api/gsq/steelRingStock";
import tltUpload from "@/components/tltUpload/tltUpload.vue";
import InfoDialog from "./components/infoDialog.vue";

export default {
  name: "GsqSteelRingStock",
  components: {
    tltUpload,
    InfoDialog,
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
    };
  },
  computed: {
    searchColumns() {
      return [
        {
          label: this.$t("ui.data.column.gsq.steelRingStock.stockDate"),
          prop: "stockDate",
          type: "daterange",
        },
        {
          label: this.$t("ui.data.column.gsq.steelRingStock.steelRingCode"),
          prop: "steelRingCode",
          type: "input",
          placeholder: this.$t("ui.data.column.gsq.steelRingStock.steelRingCode"),
        },
      ];
    },
    columns() {
      return [
        { type: "selection", fixed: "left" },
        {
          prop: "stockDate",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.gsq.steelRingStock.stockDate"),
          minWidth: 120,
          formatter: (row) => {
            return row.stockDate || "-";
          },
        },
        {
          prop: "steelRingCode",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.gsq.steelRingStock.steelRingCode"),
          minWidth: 120,
          formatter: (row) => {
            return row.steelRingCode || "-";
          },
        },
        {
          prop: "stockNum",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.gsq.steelRingStock.stockNum"),
          minWidth: 120,
        },
        {
          prop: "modifyNum",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.gsq.steelRingStock.modifyNum"),
          minWidth: 120,
        },
        {
          prop: "badNum",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.gsq.steelRingStock.badNum"),
          minWidth: 120,
        },
        {
          prop: "remark",
          halign: "center",
          label: this.$t("ui.common.column.remark"),
          minWidth: 100,
          formatter: (row) => {
            return row.remark || "-";
          },
        },
        {
          prop: "updateTime",
          halign: "center",
          label: this.$t("ui.data.column.gsq.steelRingStock.updateDate"),
          minWidth: 150,
        },
        {
          align: "center",
          halign: "center",
          label: this.$t("ui.data.btn.option"),
          prop: "option",
          width: 180,
          fixed: "right",
          render: ({ row }) => {
            return (
              <div>
                <el-button
                  v-hasPermi={["gsq:steelRingStock:edit"]}
                  class="minus"
                  type="primary"
                  onClick={() => this.handleEdit(row)}
                >
                  {this.$t("ui.frame.btn.modify")}
                </el-button>
                <el-button
                  v-hasPermi={["gsq:steelRingStock:remove"]}
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
        removeSteelRingStock(row.id).then((data) => {
          this.$modal.msgSuccess(data.msg);
          this.$set(this.page, "current", 1);
          this.getList();
        });
      });
    },
    handleBatchDelete() {
      if (this.selection.length === 0) {
        this.$modal.msgWarning(
          this.$t("common.confirm.selectDeleteData") || "请选择需要删除的数据"
        );
        return;
      }
      this.$confirm(this.$t("common.confirm.delete"), {
        type: "warning",
      }).then(() => {
        const ids = this.selection.map((row) => row.id);
        removeSteelRingStock(ids).then((data) => {
          this.$modal.msgSuccess(data.msg);
          this.$set(this.page, "current", 1);
          this.getList();
        });
      });
    },
    handleExport() {
      this.$confirm(this.$t("ui.data.column.gsq.steelRingStock.confirm.export"), {
        type: "warning",
      }).then(() => {
        try {
          this.loading = true;
          let params = this.formatParams(false);
          params = {
            ...params,
            pageSize: undefined,
            pageNum: undefined,
          };
          exportSteelRingStock(params);
        } catch (error) {
          console.error(error);
        } finally {
          this.loading = false;
        }
      });
    },
    handleSearch(data) {
      this.query = data;
      if (data.stockDate && data.stockDate.length === 2) {
        this.query.stockDateBegin = data.stockDate[0];
        this.query.stockDateEnd = data.stockDate[1];
        delete this.query.stockDate;
      } else {
        this.query.stockDateBegin = undefined;
        this.query.stockDateEnd = undefined;
        delete this.query.stockDate;
      }
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
        this.sort = {};
      }
      this.getList();
    },
    handleSelectionChange(rows) {
      this.selection = rows;
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
      return params;
    },
    async getList() {
      try {
        this.loading = true;
        const data = await listSteelRingStock(this.formatParams());
        this.data = data.rows;
        this.page.total = data.total;
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
    },
  },
  mounted() {
    this.getList();
  },
  activated() {
    this.getList();
  },
};
</script>
