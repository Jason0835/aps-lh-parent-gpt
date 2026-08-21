<template>
  <basic-container>
    <page-table
      tableRef="djStockMainTable"
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
          v-hasPermi="['dj:stock:add']"
          @click="handleAdd"
          >{{ $t("ui.frame.btn.add") }}</el-button
        >
        <el-button
          type="primary"
          plain
          v-hasPermi="['dj:stock:edit']"
          @click="handleEdit(selection[0])"
          >{{ $t("ui.frame.btn.modify") }}</el-button
        >
        <el-button
          type="warning"
          v-hasPermi="['dj:stock:stockRevise']"
          :disabled="selection.length !== 1"
          @click="() => handleModifyStock(selection[0])"
          >{{ $t("ui.frame.btn.stock.modify2") }}</el-button
        >
        <el-button
          type="danger"
          plain
          v-hasPermi="['dj:stock:remove']"
          @click="handleBatchDelete"
          >{{ $t("ui.frame.btn.delete") }}</el-button
        > 
        <el-button
          v-hasPermi="['dj:stock:import']"
          @click="() => $refs.tltUploadForm.handleImport(importDefaultValue)"
          >{{ $t("ui.frame.btn.import") }}</el-button
        >
        <el-button @click="handleExport" v-hasPermi="['dj:stock:export']">{{
          $t("ui.frame.btn.export")
        }}</el-button>
      </template>
    </page-table>
    <tlt-upload-form
      ref="tltUploadForm"
      :title="$t('ui.frame.page.stock.title')"
      downloadUrl="/dj/stock/importTemplate"
      uploadUrl="/dj/stock/importData"
      @uploadSuccess="getList"
      labelWidth="0"
      :columns="importColumns"
      :rules="importRules"
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
import { listStock, removeStock, releaseStock } from "@/api/dj/stock";
import { getConfigKey } from "@/api/system/config";
//components
import TltUploadForm from "@/views/components/tltUploadForm.vue";

import infoDialog from "./components/infoDialog.vue";

export default {
  name: "DjStock",
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
        factoryCode: '',
      },
      query: {
        factoryCode: '',
      },
      importDefaultValue: {
        updateSupport: false,
      },
      importColumns: [
        {
          label: "",
          prop: "updateSupport",
          render: (form) => {
            return (
              <el-checkbox
                label={this.$t("ui.checkbox.updateExistingData")}
                v-model={form.updateSupport}
              >
                {this.$t("ui.checkbox.updateExistingData")}
              </el-checkbox>
            );
          },
        },
      ],
      importRules: {},
    };
  },
  computed: {
    columns() {
      let columns = [
        { type: "selection", fixed: "left" },
        {
          label: this.$t("ui.data.column.factoryCode"),
          prop: "factoryCode",
          minWidth: 100,
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_factory_name, value);
          },
        },
        {
          prop: "stockDate",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.stock.stockDate"),
          minWidth: 100,
          // sortable: "custom",
        },
        {
          prop: "materialName",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.dj.scheduleResult.paddingName"),
          minWidth: 160,
          // sortable: "custom",
        },
        {
          prop: "materialCode",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.dj.scheduleResult.paddingCode"),
          // sortable: "custom",
        },
        {
          prop: "stockNum",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.stock.stockNum.meter"),
          // sortable: "custom",
        },

        {
          prop: "modifyNum",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.stock.modifyNum.meter"),
          // sortable: "custom",
        },
        {
          prop: "badNum",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.stock.badNum.meter"),
          // sortable: "custom",
        },
        {
          prop: "remark",
          align: "center",
          halign: "center",
          label: this.$t("ui.common.column.remark"),
          minWidth: 100,
          // sortable: "custom",
        },
        {
          prop: "updateTime",
          align: "center",
          halign: "center",
          label: this.$t("common.updateTime"),
          minWidth: 160,
        },
        {
          align: "center",
          halign: "center",
          label: this.$t("ui.data.btn.option"),
          minWidth: 200,
          width: 200,
          fixed: "right",
          render: ({ row }) => {
            return (
              <div>
                <el-button
                  v-hasPermi={["dj:stock:edit"]}
                  class="minus"
                  type="success"
                  onClick={() => this.handleModifyStock(row)}
                >
                  {this.$t("ui.frame.btn.stock.modify2")}
                </el-button>
                <el-button
                  v-hasPermi={["dj:stock:remove"]}
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
          label: this.$t("ui.data.column.factoryCode"),
          prop: "factoryCode",
          type: "select",
          dictData: this.dict.type.biz_factory_name,
          filterable: true,
        },
        {
          label: this.$t("ui.data.column.stock.stockDate"),
          prop: "stockDate",
          type: "date",
          dateType: "daterange",
          valueFormat: "yyyy-MM-dd",
        },
        {
          label: this.$t("ui.data.column.dj.scheduleResult.paddingName"),
          prop: "materialName",
        },
      ];
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
      downloadLink("/dj/stock/export", this.formatParams(false));
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
  created() {
    getConfigKey("sys.factory.code").then(response => {
      this.search.factoryCode = response.msg;
      this.query.factoryCode = response.msg;
      this.getList();
    }).catch(() => {
      this.getList();
    });},
};
</script>
<style lang="scss" scoped>
.more-btn {
  margin: 2px 0;
  width: 100%;
}
</style>
