<template>
  <basic-container>
    <page-table
      tableRef="cd15ParamsMainTable"
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
          v-hasPermi="['cd15:params:edit']"
          @click="handleBatchEdit"
          :disabled="selection.length !== 1"
        >{{ $t("ui.frame.btn.update") }}</el-button>
        <el-button
          @click="handleExport"
          v-hasPermi="['cd15:params:export']"
        >{{ $t("ui.frame.btn.export") }}</el-button>
      </template>
    </page-table>
    <info-dialog ref="infoRef" @success="getList" />
  </basic-container>
</template>

<script>
import { listCd15Params, exportCd15Params } from "@/api/cd15/cd15Params";
import infoDialog from "./components/infoDialog.vue";

export default {
  name: "Cd15Params",
  components: {
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
        factoryCode: "116",
      },
      query: {
        factoryCode: "116",
      },
    };
  },
  computed: {
    columns() {
      return [
        { type: "selection", fixed: "left" },
        {
          prop: "factoryCode",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.cd15Params.factoryCode"),
          minWidth: 120,
          formatter: (row, column, value) => this.selectDictLabel(this.dict.type.biz_factory_name, value),
        },
        {
          prop: "paramCode",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.cd15Params.paramCode"),
          minWidth: 140,
        },
        {
          prop: "paramName",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.cd15Params.paramName"),
          minWidth: 140,
        },
        {
          prop: "paramValue",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.cd15Params.paramValue"),
          minWidth: 130,
        },
        {
          prop: "remark",
          halign: "center",
          label: this.$t("ui.common.column.remark"),
          minWidth: 120,
        },
        {
          align: "center",
          halign: "center",
          label: this.$t("ui.data.btn.option"),
          prop: "option",
          minWidth: 150,
          fixed: "right",
          render: ({ row }) => {
            return (
              <div>
                <el-button
                  v-hasPermi={["cd15:params:edit"]}
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
    },
    searchColumns() {
      return [
        {
          label: this.$t("ui.data.column.cd15Params.factoryCode"),
          prop: "factoryCode",
          type: "select",
          dictData: this.dict.type.biz_factory_name,
          filterable: true,
        },
        {
          label: this.$t("ui.data.column.cd15Params.paramCode"),
          prop: "paramCode",
        },
        {
          label: this.$t("ui.data.column.cd15Params.paramName"),
          prop: "paramName",
        },
      ];
    },
  },
  methods: {
    handleEdit(row) {
      this.$refs.infoRef && this.$refs.infoRef.show(row);
    },
    handleBatchEdit() {
      if (this.selection.length === 1) {
        this.handleEdit(this.selection[0]);
      }
    },
    handleExport() {
      exportCd15Params(this.query);
    },
    handleSearch(params) {
      this.page.current = 1;
      this.query = { ...params };
      this.getList();
    },
    handlePageChange(current, pageSize) {
      this.page.current = current;
      this.page.pageSize = pageSize;
      this.getList();
    },
    handleSortChange(sort) {
      this.sort = sort;
      this.getList();
    },
    handleSelectionChange(selection) {
      this.selection = selection || [];
    },
    async getList() {
      this.loading = true;
      try {
        const params = {
          ...this.query,
          pageNum: this.page.current,
          pageSize: this.page.pageSize,
          orderByColumn: this.sort.prop,
          isAsc: this.sort.order,
        };
        const res = await listCd15Params(params);
        this.data = res.rows || [];
        this.page.total = res.total || 0;
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
