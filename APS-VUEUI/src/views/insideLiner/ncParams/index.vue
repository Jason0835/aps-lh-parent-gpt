<template>
  <basic-container>
    <page-table
      tableRef="ncParamsMainTable"
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
          v-hasPermi="['nc:params:add']"
          @click="handleAdd"
          >{{ $t("ui.frame.btn.add") }}</el-button
        >
        <el-button
          type="warning"
          v-hasPermi="['nc:params:edit']"
          @click="handleEdit(selection[0])"
          :disabled="selection.length !== 1"
          >{{ $t("ui.frame.btn.modify") }}</el-button
        >
        <el-button
          @click="handleExport"
          v-hasPermi="['nc:params:export']"
          >{{ $t("ui.frame.btn.export") }}</el-button
        >
      </template>
    </page-table>
    <infoDialog ref="infoRef" @success="getList" />
  </basic-container>
</template>
<script>
import { downloadLink } from "@/utils/request";
import { listParams } from "@/api/nc/params";
import { getConfigKey } from "@/api/system/config";
import infoDialog from "./components/infoDialog.vue";

export default {
  name: "NcParams",
  components: {
    infoDialog,
  },
  dicts: ["biz_factory_name", "biz_product_type"],
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
    };
  },
  computed: {
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
          label: this.$t("ui.nc.params.column.paramCode"),
          prop: "paramCode",
        },
        {
          label: this.$t("ui.nc.params.column.paramName"),
          prop: "paramName",
        },
      ];
    },
    columns() {
      let columns = [
        { type: "selection", fixed: "left" },
        {
          prop: "factoryCode",
          align: "center",
          halign: "center",
          label: this.$t("ui.nc.params.column.factoryCode"),
          dictData: this.dict.type.biz_factory_name,
          formatter: (row, column, value, index) => {
            return this.selectDictLabel(this.dict.type.biz_factory_name, value);
          },
          minWidth: 80,
        },
        {
          prop: "productTypeCode",
          align: "center",
          halign: "center",
          label: this.$t("ui.nc.params.column.productTypeCode"),
          dictData: this.dict.type.biz_product_type,
          formatter: (row, column, value, index) => {
            return this.selectDictLabel(this.dict.type.biz_product_type, value);
          },
          minWidth: 80,
        },
        {
          prop: "paramCode",
          align: "center",
          halign: "center",
          label: this.$t("ui.nc.params.column.paramCode"),
          minWidth: 200,
        },
        {
          prop: "paramName",
          align: "center",
          halign: "center",
          label: this.$t("ui.nc.params.column.paramName"),
          titleTooltip: true,
          minWidth: 180,
        },
        {
          prop: "paramValue",
          align: "center",
          halign: "center",
          label: this.$t("ui.nc.params.column.paramValue"),
          minWidth: 100,
        },
        {
          prop: "defauleValue",
          align: "center",
          halign: "center",
          label: this.$t("ui.nc.params.column.defauleValue"),
          minWidth: 100,
        },
        {
          prop: "remark",
          align: "center",
          halign: "center",
          label: this.$t("ui.common.column.remark"),
          minWidth: 380,
        },
        {
          prop: "updateBy",
          align: "center",
          halign: "center",
          label: this.$t("ui.common.column.updateBy"),
          minWidth: 100,
        },
        {
          prop: "updateTime",
          align: "center",
          halign: "center",
          label: this.$t("ui.common.column.updateTime"),
          minWidth: 160,
        },
        {
          align: "center",
          halign: "center",
          label: this.$t("ui.data.btn.option"),
          minWidth: 120,
          width: 120,
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
      downloadLink("/nc/params/export", this.formatParams(false));
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
    // 清除之前持久化的错误列顺序，保留拖拽功能但不再恢复错误状态
    localStorage.removeItem("ncParamsMainTable");
    getConfigKey("sys.factory.code").then(response => {
      this.search.factoryCode = response.msg;
      this.query.factoryCode = response.msg;
      this.getList();
    }).catch(() => {
      this.getList();
    });
  },
};
</script>
<style lang="scss" scoped>
.more-btn {
  margin: 2px 0;
  width: 100%;
}
</style>
