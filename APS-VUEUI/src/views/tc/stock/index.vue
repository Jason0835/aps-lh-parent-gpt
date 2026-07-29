<template>
  <basic-container>
    <page-table
      v-loading="loading"
      :calcHeight="true"
      :columns="columns"
      :data="data"
      :page="page"
      :search="search"
      :searchColumns="searchColumns"
      :selectArea="false"
      :showSummary="false"
      tableRef="tcStockMainTable"
      @pageChange="handlePageChange"
      @refresh="getList"
      @search="handleSearch"
      @sort-change="handleSortChange"
      @selection-change="handleSelectionChange"
    >
      <template slot="header">
        <el-button
          v-hasPermi="['tc:tcStock:edit']"
          plain
          type="primary"
          @click="handleAdd"
        >{{ $t("ui.frame.btn.add") }}</el-button>
        <el-button
          v-hasPermi="['tc:tcStock:remove']"
          :disabled="selection.length == 0"
          type="danger"
          @click="handleDeleteAll"
        >{{ $t("ui.frame.btn.delete") }}</el-button>
        <el-button
          v-hasPermi="['tc:tcStock:import']"
          @click="$refs.tltUpload.handleImport()"
        >{{ $t("ui.frame.btn.import") }}</el-button>
        <el-button
          v-hasPermi="['tc:tcStock:export']"
          @click="handleExport"
        >{{ $t("ui.frame.btn.export") }}</el-button>
      </template>
    </page-table>
    <tlt-upload-form
      ref="tltUpload"
      :columns="importColumns"
      :updateSupport="true"
      downloadUrl="/tc/tcStock/importTemplate"
      labelWidth="0"
      uploadUrl="/tc/tcStock/importData"
      @uploadSuccess="getList"
    ></tlt-upload-form>
    <infoDialog ref="infoRef" @success="getList" />
  </basic-container>
</template>
<script>
import {downloadLink} from "@/utils/request";
import {listTcStock, removeTcStock} from "@/api/tc/stock";
import tltUpload from "@/components/tltUpload/tltUpload.vue";
import TltUploadForm from "@/views/components/tltUploadForm.vue";
import infoDialog from "./components/infoDialog.vue";

export default {
  name: "/tc/tcStock",
  components: {
    tltUpload,
    infoDialog,
    TltUploadForm,
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
      search: {},
      query: {},
      importDefaultValue: {},
      importRules: {},
    };
  },
  computed: {
    columns() {
      return [
        { type: "selection", fixed: "left" },
        {
          prop: "factoryCode",
          label: this.$t("ui.data.column.tc.stock.factoryCode"),
          type: "select",
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_factory_name, value);
          },
        },
        {
          prop: "stockDate",
          halign: "center",
          label: this.$t("ui.data.column.tc.stock.stockDate"),
        },
        {
          prop: "sidewallCode",
          halign: "center",
          label: this.$t("ui.data.column.tc.stock.sidewallCode"),
        },
        {
          prop: "stockQty",
          halign: "center",
          label: this.$t("ui.data.column.tc.stock.stockQty"),
        },
        {
          prop: "badQty",
          halign: "center",
          label: this.$t("ui.data.column.tc.stock.badQty"),
        },
        {
          prop: "adjustQty",
          halign: "center",
          label: this.$t("ui.data.column.tc.stock.adjustQty"),
        },
        {
          prop: "remark",
          halign: "center",
          label: this.$t("ui.common.column.remark"),
          minWidth: 100,
        },
        {
          prop: "updateTime",
          label: this.$t("ui.data.column.updateTime"),
          width: 180,
        },
        {
          align: "center",
          halign: "center",
          label: this.$t("ui.data.btn.option"),
          minWidth: 180,
          width: 200,
          fixed: "right",
          render: ({ row }) => {
            return (
              <div>
                <el-button
                  v-hasPermi={["tc:tcStock:edit"]}
                  class="minus"
                  type="success"
                  onClick={() => this.handleEdit(row)}
                >
                  {this.$t("ui.frame.btn.update")}
                </el-button>
                <el-button
                  v-hasPermi={["tc:tcStock:remove"]}
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
          prop: "factoryCode",
          label: this.$t("ui.data.column.tc.stock.factoryCode"),
          type: "select",
          dictData: this.dict.type.biz_factory_name,
        },
        {
          prop: "stockDate",
          label: this.$t("ui.data.column.tc.stock.stockDate"),
          type: "date",
        },
        {
          prop: "sidewallCode",
          label: this.$t("ui.data.column.tc.stock.sidewallCode"),
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
        removeTcStock({ ids }).then((data) => {
          this.$modal.msgSuccess(data.msg);
          this.$set(this.page, "current", 1);
          this.getList();
        });
      });
    },
    handleDeleteAll() {
      let ids = "";
      for (let i = 0; i < this.selection.length; i++) {
        if (i == this.selection.length - 1) {
          ids = ids + this.selection[i].id;
        } else {
          ids = ids + this.selection[i].id + ",";
        }
      }
      this.$confirm(this.$t("common.confirm.delete"), {
        type: "warning",
      }).then(() => {
        removeTcStock({ ids }).then((data) => {
          this.$modal.msgSuccess(data.msg);
          this.$set(this.page, "current", 1);
          this.getList();
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
        this.sort = {};
      }
      this.getList();
    },
    handleSelectionChange(rows) {
      this.selection = rows;
    },
    handleExport() {
      downloadLink("/tc/tcStock/export", this.formatParams(false));
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

      if (params.createTime && params.createTime[0]) {
        params.createTimeStart = params.createTime[0];
        params.createTimeEnd = params.createTime[1];
        params.createTime = undefined;
      }

      return params;
    },
    async getList() {
      try {
        this.loading = true;
        const data = await listTcStock(this.formatParams());
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
    let defaultParams = {
      factoryCode: "116",
    };
    this.search = {
      ...defaultParams,
    };
    this.query = {
      ...defaultParams,
    };
    this.getList();
  },
  activated() {
    // this.getList();
  },
};
</script>
<style lang="scss" scoped>
.more-btn {
  margin: 2px 0;
  width: 100%;
}
</style>
