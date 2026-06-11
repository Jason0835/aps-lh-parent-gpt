<template>
  <basic-container>
    <page-table
      tableRef="djParamsMainTable"
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
          v-hasPermi="['dj:params:add']"
          @click="handleAdd"
          >{{ $t("ui.frame.btn.add") }}</el-button
        >
        <el-button
          type="warning"
          v-hasPermi="['dj:params:edit']"
          @click="handleEdit(selection[0])"
          :disabled="selection.length !== 1"
          >{{ $t("ui.frame.btn.modify") }}</el-button
        >
        <el-button
          type="danger"
          v-hasPermi="['dj:params:remove']"
          @click="handleDelete(selection)"
          :disabled="selection.length === 0"
          >{{ $t("ui.frame.btn.delete") }}</el-button
        >
        <el-button
          @click="handleExport"
          v-hasPermi="['dj:params:export']"
          >{{ $t("ui.frame.btn.export") }}</el-button
        >
      </template>
    </page-table>
    <infoDialog ref="infoRef" @success="getList" />
  </basic-container>
</template>
<script>
import { downloadLink } from "@/utils/request";
import { listParams, removeParams } from "@/api/dj/params";
import infoDialog from "./components/infoDialog.vue";

export default {
  name: "DjParams",
  components: {
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
          label: this.$t("ui.dj.params.column.paramCode"),
          prop: "paramCode",
        },
        {
          label: this.$t("ui.dj.params.column.paramName"),
          prop: "paramName",
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
    };
  },
  computed: {
    columns() {
      let columns = [
        { type: "selection", fixed: "left" },
        {
          prop: "factoryCode",
          halign: "center",
          label: this.$t("ui.dj.params.column.factoryCode"),
        },
        {
          prop: "productTypeCode",
          halign: "center",
          label: this.$t("ui.dj.params.column.productTypeCode"),
        },
        {
          prop: "paramCode",
          halign: "center",
          label: this.$t("ui.dj.params.column.paramCode"),
        },
        {
          prop: "paramName",
          halign: "center",
          label: this.$t("ui.dj.params.column.paramName"),
          titleTooltip: true,
        },
        {
          prop: "dataType",
          halign: "center",
          label: this.$t("ui.dj.params.column.dataType"),
        },
        {
          prop: "defauleValue",
          halign: "center",
          label: this.$t("ui.dj.params.column.defauleValue"),
        },
        {
          prop: "paramValue",
          halign: "center",
          label: this.$t("ui.dj.params.column.paramValue"),
        },
        {
          prop: "remark",
          halign: "center",
          label: this.$t("ui.common.column.remark"),
          minWidth: 100,
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
                  class="minus"
                  type="success"
                  onClick={() => this.handleEdit(row)}
                >
                  {this.$t("ui.frame.btn.update")}
                </el-button>
                <el-button
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
        this.$refs.infoRef.show();
      }
    },
    handleEdit(row) {
      if (this.$refs.infoRef) {
        this.$refs.infoRef.show(row);
      }
    },
    handleDelete(row) {
      const ids = Array.isArray(row) ? row.map((item) => item.id) : [row.id];
      this.$confirm(this.$t("common.confirm.delete"), {
        type: "warning",
      }).then(() => {
        this.loading = true;
        removeParams(ids)
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
      downloadLink("/dj/params/export", this.formatParams(false));
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
        const data = await listParams(this.formatParams());
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