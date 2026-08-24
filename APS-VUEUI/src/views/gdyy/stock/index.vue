<template>
  <basic-container>
    <page-table
      tableRef="gdyyStockMainTable"
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
          v-hasPermi="['gdyy:stock:add']"
          @click="handleAdd"
        >{{ $t("ui.frame.btn.add") }}</el-button>
        <el-button
          v-hasPermi="['gdyy:stock:edit']"
          @click="handleBatchEdit"
          :disabled="selection.length !== 1"
        >{{ $t("ui.frame.btn.update") }}</el-button>
        <el-button
          type="danger"
          v-hasPermi="['gdyy:stock:remove']"
          :disabled="selection.length === 0"
          @click="handleBatchDelete"
        >{{ $t("ui.frame.btn.delete") }}</el-button>
        <el-button
          v-hasPermi="['gdyy:stock:import']"
          @click="$refs.tltUpload.handleImport()"
        >{{ $t("ui.frame.btn.import") }}</el-button>
        <el-button
          @click="handleExport"
          v-hasPermi="['gdyy:stock:export']"
        >{{ $t("ui.frame.btn.export") }}</el-button>
      </template>
    </page-table>
    <tlt-upload-form
      ref="tltUpload"
      :updateSupport="true"
      downloadUrl="/gdyy/stock/importTemplate"
      uploadUrl="/gdyy/stock/importData"
      @uploadSuccess="getList"
      labelWidth="0"
      :columns="importColumns"
    />
    <info-dialog ref="infoRef" @success="getList" />
  </basic-container>
</template>

<script>
import { listGdyyStock, delGdyyStock, exportGdyyStock } from "@/api/gdyy/gdyyStock";
import TltUploadForm from "@/views/components/tltUploadForm.vue";
import infoDialog from "./components/infoDialog.vue";

export default {
  name: "GdyyStock",
  components: {
    TltUploadForm,
    infoDialog,
  },
  dicts: ["biz_factory_name"],
  provide() {
    return {
      parentDict: this.dict,
    };
  },
  data() {
    return {
      importColumns: [
        {
          label: "",
          prop: "updateSupport",
          render: (form) => {
            return (
              <el-checkbox
                label={this.$t("common.rule.updateSupport")}
                v-model={form.updateSupport}
              >
                {this.$t("common.rule.updateSupport")}
              </el-checkbox>
            );
          },
        },
      ],
      loading: false,
      data: [],
      selection: [],
      page: {
        current: 1,
        size: 20,
        total: 0,
      },
      search: {
        factoryCode: "116",
        stockDate: "",
        bigRollCode: "",
      },
    };
  },
  computed: {
    searchColumns() {
      return [
        {
          label: this.$t("ui.data.column.gdyyStock.factoryCode"),
          prop: "factoryCode",
          type: "select",
          dictData: this.dict.type.biz_factory_name,
          filterable: true,
        },
        {
          label: this.$t("ui.data.column.gdyyStock.stockDate"),
          prop: "stockDate",
          type: "date",
          valueFormat: "yyyy-MM-dd",
        },
        {
          label: this.$t("ui.data.column.gdyyStock.bigRollCode"),
          prop: "bigRollCode",
          type: "input",
        },
      ];
    },
    columns() {
      return [
        { type: "selection", fixed: "left" },
        {
          label: this.$t("ui.data.column.gdyyStock.factoryCode"),
          prop: "factoryCode",
          dictType: "biz_factory_name",
          minWidth: 100,
        },
        {
          label: this.$t("ui.data.column.gdyyStock.stockDate"),
          prop: "stockDate",
          minWidth: 110,
        },
        {
          label: this.$t("ui.data.column.gdyyStock.bigRollCode"),
          prop: "bigRollCode",
          minWidth: 140,
        },
        {
          label: this.$t("ui.data.column.gdyyStock.bigRollBarcode"),
          prop: "bigRollBarcode",
          minWidth: 140,
        },
        {
          label: this.$t("ui.data.column.gdyyStock.stockNum"),
          prop: "stockNum",
          minWidth: 120,
        },
        {
          label: this.$t("ui.data.column.gdyyStock.stockRollNum"),
          prop: "stockRollNum",
          minWidth: 120,
        },
        {
          label: this.$t("ui.data.column.gdyyStock.stockMeters"),
          prop: "stockMeters",
          minWidth: 120,
        },
        {
          label: this.$t("ui.data.column.gdyyStock.modifyNum"),
          prop: "modifyNum",
          minWidth: 120,
        },
        {
          label: this.$t("ui.data.column.gdyyStock.badNum"),
          prop: "badNum",
          minWidth: 120,
        },
        {
          label: this.$t("ui.data.column.gdyyStock.estimateStockFlag"),
          prop: "estimateStockFlag",
          minWidth: 100,
        },
        {
          label: this.$t("ui.data.column.gdyyStock.remark"),
          prop: "remark",
          minWidth: 120,
        },
        {
          align: "center",
          label: this.$t("ui.frame.table.action"),
          fixed: "right",
          minWidth: 150,
          render: (row) => {
            return (
              <div>
                <el-button
                  v-hasPermi={["gdyy:stock:edit"]}
                  class="minus"
                  type="success"
                  onClick={() => this.handleEdit(row)}
                >
                  {this.$t("ui.frame.btn.update")}
                </el-button>
                <el-button
                  v-hasPermi={["gdyy:stock:remove"]}
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
  created() {
    this.getList();
  },
  methods: {
    getList() {
      this.loading = true;
      const params = {
        pageNum: this.page.current,
        pageSize: this.page.size,
        ...this.search,
      };
      listGdyyStock(params).then((res) => {
        this.data = res.rows;
        this.page.total = res.total;
        this.loading = false;
      });
    },
    handleSearch() {
      this.page.current = 1;
      this.getList();
    },
    handlePageChange(current, pageSize) {
      this.page.current = current;
      this.page.pageSize = pageSize;
      this.getList();
    },
    handleSortChange({ prop, order }) {
      this.search.orderBy = prop;
      this.search.order = order === "ascending" ? "asc" : "desc";
      this.getList();
    },
    handleSelectionChange(selection) {
      this.selection = selection;
    },
    handleAdd() {
      this.$refs.infoRef.openDialog("add");
    },
    handleBatchEdit() {
      if (this.selection.length !== 1) {
        this.$message.warning(this.$t("ui.frame.msg.selectOne"));
        return;
      }
      this.$refs.infoRef.openDialog("edit", this.selection[0]);
    },
    handleEdit(row) {
      this.$refs.infoRef.openDialog("edit", row);
    },
    handleBatchDelete() {
      if (this.selection.length === 0) {
        this.$message.warning(this.$t("ui.frame.msg.selectAtLeastOne"));
        return;
      }
      const ids = this.selection.map((item) => item.id);
      this.$confirm(this.$t("ui.frame.confirm.delete")).then(() => {
        delGdyyStock(ids).then((res) => {
          this.$message.success(this.$t("ui.frame.msg.success"));
          this.getList();
        });
      });
    },
    handleDelete(row) {
      this.$confirm(this.$t("ui.frame.confirm.delete")).then(() => {
        delGdyyStock([row.id]).then((res) => {
          this.$message.success(this.$t("ui.frame.msg.success"));
          this.getList();
        });
      });
    },
    handleExport() {
      exportGdyyStock(this.search);
    },
  },
};
</script>
