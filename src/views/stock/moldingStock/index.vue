
<template>
  <basic-container>
    <page-table
      tableRef="cxMoldingStockMainTable"
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
          type="warning"
          v-hasPermi="['cx:stock:edit']"
          @click="handleEdit(selection[0])"
          >{{ $t("编辑") }}</el-button
        >
        <el-button
          type="danger"
          v-hasPermi="['cx:stock:remove']"
          @click="handleBatchDelete"
          >{{ $t("ui.frame.btn.delete") }}</el-button
        >
        <el-button
          v-hasPermi="['cx:stock:import']"
          @click="$refs.tltUpload.handleImport()"
          >{{ $t("ui.frame.btn.import") }}</el-button
        >
        <el-button @click="handleExport" v-hasPermi="['cx:stock:export']">{{
          $t("ui.frame.btn.export")
        }}</el-button>
      </template>
    </page-table>
    <tlt-upload
      ref="tltUpload"
      downloadUrl="/cx/stock/importTemplate"
      uploadUrl="/cx/stock/importData"
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
import { listStock, removeStock, releaseStock } from "@/api/cx/stock";
//components
import tltUpload from "@/components/tltUpload/tltUpload.vue";

import infoDialog from "./components/infoDialog.vue";

export default {
 name: "MoldingStock",
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
          label: this.$t("胎胚编号"),
          prop: "stockDate",
        },
        {
          prop: "yearMonth",
          label: this.$t("库存日期"),
          type: "date",
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
          prop: "embryoCode",
          align: "center",
          halign: "center",
          label: this.$t("胎胚编号"),
        },
        {
          prop: "stockDate",
          align: "center",
          halign: "center",
          minWidth: 100,
          label: this.$t("ui.data.column.stock.stockDate"),
          // sortable: "custom",
        },
        {
          prop: "stockNum",
          align: "right",
          halign: "center",
          label: this.$t("可用数量")
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
        // {
        //   align: "center",
        //   halign: "center",
        //   label: this.$t("ui.data.btn.option"),
        //   minWidth: 180,
        //   width: 180,
        //   fixed: "right",
        //   render: ({ row }) => {
        //     return (
        //       <div>
        //         <el-button
        //           class="minus"
        //           type="success"
        //           onClick={() => this.handleModifyStock(row)}
        //         >
        //           {this.$t("ui.frame.btn.stock.modify2")}
        //         </el-button>
        //         <el-button
        //           class="minus"
        //           type="danger"
        //           onClick={() => this.handleDelete(row)}
        //         >
        //           {this.$t("ui.frame.btn.delete")}
        //         </el-button>
        //       </div>
        //     );
        //   },
        // },
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
      downloadLink("/cx/stock/export", this.formatParams(false));
    },
    handleModifyStock(row) {
      if (this.$refs.infoRef) {
        this.$refs.infoRef.show(row);
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
