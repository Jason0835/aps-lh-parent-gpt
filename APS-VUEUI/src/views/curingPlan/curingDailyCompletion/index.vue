<template>
  <basic-container>
    <page-table
      tableRef="curingDailyCompletionTable"
      :calcHeight="true"
      v-loading="loading"
      :columns="columns"
      :searchColumns="searchColumns"
      :data="data"
      :page="page"
      :search="search"
      :showSummary="false"
      :selectArea="false"
      @refresh="getList"
      @search="handleSearch"
      @pageChange="handlePageChange"
      @sort-change="handleSortChange"
    />
  </basic-container>
</template>

<script>
import { listResult } from "@/api/lh/curingDailyCompletion";

export default {
  name: "CuringDailyCompletion",
  dicts: ["biz_factory_name"],
  data() {
    return {
      loading: false,
      data: [],
      page: {
        current: 1,
        pageSize: 20,
        total: 0,
      },
      sort: {},
      search: {
        factoryCode: "116",
        materialCode: "",
        materialDesc: "",
      },
      query: {
        factoryCode: "116",
        materialCode: "",
        materialDesc: "",
      },
    };
  },
  computed: {
    columns() {
      return [
        {
          label: this.$t("ui.data.column.factoryCode"),
          prop: "factoryCode",
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_factory_name, value);
          },
        },
        {
          label: this.$t("ui.data.column.scheduleResult.materialCode"),
          prop: "materialCode",
          minWidth: 150,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.materialDesc"),
          align: "left",
          prop: "materialDesc",
          minWidth: 350,
          showOverflowTooltip: true,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.finishQty"),
          prop: "dayFinishQty",
          align: "right",
          minWidth: 100,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.completeDate"),
          prop: "finishDate",
          minWidth: 120,
        },
        {
          label: this.$t("ui.data.column.remark"),
          prop: "remark",
          minWidth: 200,
          showOverflowTooltip: true,
        },
        {
          label: this.$t("ui.data.column.updateTime"),
          prop: "updateTime",
          minWidth: 160,
        },
      ];
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
          label: this.$t("ui.data.column.scheduleResult.materialCode"),
          prop: "materialCode",
        },
        {
          label: this.$t("ui.data.column.scheduleResult.materialDesc"),
          minWidth: 350,
          align: "left",
          prop: "materialDesc",
        },
      ];
    },
  },
  methods: {
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
    handleSortChange({ prop, order }) {
      if (order) {
        this.sort = {
          orderByColumn: prop,
          isAsc: order === "ascending" ? "asc" : "desc",
        };
      } else {
        this.sort = {};
      }
      this.getList();
    },
    formatParams() {
      return {
        pageSize: this.page.pageSize,
        pageNum: this.page.current,
        ...this.query,
        ...this.sort,
      };
    },
    async getList() {
      try {
        this.loading = true;
        const res = await listResult(this.formatParams());
        this.data = res.rows || [];
        this.page.total = res.total || 0;
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
