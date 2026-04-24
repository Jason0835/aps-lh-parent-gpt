<template>
  <basic-container>
    <page-table
      tableRef="mdmChipStockMainTable"
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
          v-hasPermi="['lh:mdmChipStock:edit']"
          @click="handleAdd"
        >{{ $t("ui.frame.btn.add") }}</el-button>
        <el-button
          type="danger"
          v-hasPermi="['lh:mdmChipStock:remove']"
          :disabled="selection.length == 0"
          @click="handleBatchDelete"
        >{{ $t("ui.frame.btn.delete") }}</el-button>
        <el-button
          v-hasPermi="['lh:mdmChipStock:import']"
          @click="$refs.tltUpload.handleImport()"
        >{{ $t("ui.frame.btn.import") }}</el-button>
        <el-button
          @click="handleExport"
          v-hasPermi="['lh:mdmChipStock:export']"
        >{{ $t("ui.frame.btn.export") }}</el-button>
      </template>
    </page-table>
    <tlt-upload-form
      ref="tltUpload"
      :updateSupport="true"
      downloadUrl="/lh/mdmChipStock/importTemplate"
      uploadUrl="/lh/mdmChipStock/importData"
      @uploadSuccess="getList"
      labelWidth="0"
      :columns="importColumns"
    ></tlt-upload-form>
    <infoDialog ref="infoRef" @success="getList" />
  </basic-container>
</template>
<script>
import { downloadLink } from "@/utils/request";
import { listMdmChipStock, removeMdmChipStock } from "@/api/lh/mdmChipStock";
import TltUploadForm from "@/views/components/tltUploadForm.vue";
import infoDialog from "./components/infoDialog.vue";

export default {
  name: "MdmChipStock",
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
        pageSize: 20,
        total: 0,
      },
      sort: {},
      search: {
        factoryCode: "116",
      },
      query: {
        factoryCode: "116",
      },
    };
  },
  computed: {
    columns() {
      let columns = [
        { type: "selection", fixed: "left" },
        // {
        //   prop: "companyCode",
        //   align: "center",
        //   halign: "center",
        //   label: this.$t("ui.data.column.mdmChipStock.companyCode"),
        //   minWidth: 120,
        // },
        {
          prop: "factoryCode",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.mdmChipStock.factoryCode"),
          minWidth: 120,
          formatter: (row, column, value, index) => {
            return this.selectDictLabel(this.dict.type.biz_factory_name, value);
          },
        },
        {
          prop: "chipCode",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.mdmChipStock.chipCode"),
          minWidth: 150,
        },
        {
          prop: "stockNum",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.mdmChipStock.stockNum"),
          minWidth: 120,
        },
        {
          prop: "finishQty",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.mdmChipStock.finishQty"),
          minWidth: 120,
        },
        {
          prop: "remainStockNum",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.mdmChipStock.remainStockNum"),
          minWidth: 120,
        },
        // {
        //   prop: "dataVersion",
        //   align: "center",
        //   halign: "center",
        //   label: this.$t("ui.data.column.mdmChipStock.dataVersion"),
        //   minWidth: 120,
        // },
        {
          prop: "remark",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.mdmChipStock.remark"),
          minWidth: 200,
        },
        {
          align: "center",
          halign: "center",
          label: this.$t("ui.data.btn.option"),
          prop: "option",
          minWidth: 150,
          render: ({ row }) => {
            return (
              <div>
                <el-button
                  v-hasPermi={["lh:mdmChipStock:edit"]}
                  class="minus"
                  type="success"
                  onClick={() => this.handleEdit(row)}
                >
                  {this.$t("ui.frame.btn.update")}
                </el-button>
                <el-button
                  v-hasPermi={["lh:mdmChipStock:remove"]}
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
    searchColumns() {
      return [
        {
          label: this.$t("ui.data.column.mdmChipStock.factoryCode"),
          prop: "factoryCode",
          type: "select",
          dictData: this.dict.type.biz_factory_name,
          filterable: true,
        },
        {
          label: this.$t("ui.data.column.mdmChipStock.chipCode"),
          prop: "chipCode",
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
        const ids = row.id;
        removeMdmChipStock(ids).then((data) => {
          this.$modal.msgSuccess(data.msg);
          this.$set(this.page, "current", 1);
          this.getList();
        });
      });
    },
    handleBatchEdit() {
      if (this.selection && this.selection.length === 1) {
        this.handleEdit(this.selection[0]);
      }
    },
    handleBatchDelete() {
      if (!this.selection || this.selection.length === 0) {
        this.$modal.msgWarning(this.$t("common.tip.selectOne"));
        return;
      }
      const ids = this.selection.map(item => item.id).join(',');
      this.$confirm(this.$t("common.confirm.delete"), {
        type: "warning",
      }).then(() => {
        removeMdmChipStock(ids).then((data) => {
          this.$modal.msgSuccess(data.msg);
          this.$set(this.page, "current", 1);
          this.getList();
        });
      });
    },
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
    handleExport() {
      downloadLink("/lh/mdmChipStock/export", this.formatParams(false));
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
        const data = await listMdmChipStock(this.formatParams());
        this.data = data.rows;
        this.page.total = data.total;
      } catch (error) {
        console.error(error);
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
<style lang="scss" scoped>
</style>
