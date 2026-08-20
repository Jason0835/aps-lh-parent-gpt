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
          type="warning"
          v-hasPermi="['dj:params:edit']"
          @click="handleEdit(selection[0])"
          :disabled="selection.length !== 1"
          >{{ $t("ui.frame.btn.modify") }}</el-button
        >
      </template>
    </page-table>
    <infoDialog ref="infoRef" @success="getList" />
  </basic-container>
</template>
<script>
import { listParams } from "@/api/dj/params";
import { getConfigKey } from "@/api/system/config";
import infoDialog from "./components/infoDialog.vue";

export default {
  name: "DjParams",
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
          label: this.$t("ui.dj.params.column.paramCode"),
          prop: "paramCode",
        },
        {
          label: this.$t("ui.dj.params.column.paramName"),
          prop: "paramName",
        },
      ];
    },
    columns() {
      let columns = [
        { type: "selection", fixed: "left" },
        {
          prop: "factoryCode",
          halign: "center",
          label: this.$t("ui.dj.params.column.factoryCode"),
          dictData: this.dict.type.biz_factory_name,
          formatter: (row, column, value, index) => {
            return this.selectDictLabel(this.dict.type.biz_factory_name, value);
          },
          width: 80,
        },
        {
          prop: "productTypeCode",
          halign: "center",
          label: this.$t("ui.dj.params.column.productTypeCode"),
          dictData: this.dict.type.biz_product_type,
          formatter: (row, column, value, index) => {
            return this.selectDictLabel(this.dict.type.biz_product_type, value);
          },
          width: 80,
        },
        {
          prop: "paramCode",
          halign: "center",
          label: this.$t("ui.dj.params.column.paramCode"),
          width: 100,
        },
        {
          prop: "paramName",
          halign: "center",
          label: this.$t("ui.dj.params.column.paramName"),
          titleTooltip: true,
          width: 190,
        },
        {
          prop: "paramValue",
          halign: "center",
          label: this.$t("ui.dj.params.column.paramValue"),
          width: 100,
        },
        {
          prop: "defauleValue",
          halign: "center",
          label: this.$t("ui.dj.params.column.defauleValue"),
          width: 100,
        },
        {
          prop: "remark",
          halign: "center",
          label: this.$t("ui.common.column.remark"),
          minWidth: 380,
        },
        {
          prop: "updateBy",
          halign: "center",
          label: this.$t("ui.common.column.updateBy"),
          width: 100,
        },
        {
          prop: "updateTime",
          halign: "center",
          label: this.$t("ui.common.column.updateTime"),
          width: 160,
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
    localStorage.removeItem("djParamsMainTable");
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