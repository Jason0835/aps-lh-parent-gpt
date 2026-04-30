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
          prop: "materialDesc",
          minWidth: 240,
          showOverflowTooltip: true,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.finishQty"),
          prop: "finishQty",
          align: "right",
          minWidth: 100,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.completeDate"),
          prop: "completeDate",
          minWidth: 120,
        },
        {
          label: this.$t("ui.data.column.remark"),
          prop: "remark",
          minWidth: 200,
          showOverflowTooltip: true,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.updateTime"),
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
          prop: "materialDesc",
        },
      ];
    },
  },
  methods: {
    getMockRows() {
      const materialDescMap = {
        "MAT-0001": "205/55R16 示例物料描述",
        "MAT-0002": "225/45R17 示例物料描述",
        "MAT-0003": "235/55R18 示例物料描述",
      };
      return [
        {
          id: 1,
          factoryCode: "116",
          materialCode: "MAT-0001",
          finishQty: 120,
          completeDate: "2026-04-29",
          remark: "机台负荷正常",
          updateTime: "2026-04-30 15:00:00",
        },
        {
          id: 2,
          factoryCode: "116",
          materialCode: "MAT-0002",
          finishQty: 96,
          completeDate: "2026-04-29",
          remark: "夜班补产",
          updateTime: "2026-04-30 15:10:00",
        },
        {
          id: 3,
          factoryCode: "117",
          materialCode: "MAT-0003",
          finishQty: 80,
          completeDate: "2026-04-29",
          remark: "计划达成",
          updateTime: "2026-04-30 15:15:00",
        },
      ].map((item) => ({
        ...item,
        materialDesc: materialDescMap[item.materialCode] || "",
      }));
    },
    queryMockList(params) {
      const {
        pageNum = 1,
        pageSize = 20,
        factoryCode,
        materialCode,
        materialDesc,
        orderByColumn,
        isAsc,
      } = params;
      let rows = this.getMockRows().filter((item) => {
        if (factoryCode && item.factoryCode !== factoryCode) return false;
        if (materialCode && !item.materialCode.includes(materialCode)) return false;
        if (materialDesc && !item.materialDesc.includes(materialDesc)) return false;
        return true;
      });
      if (orderByColumn) {
        rows = rows.sort((a, b) => {
          const aValue = a[orderByColumn];
          const bValue = b[orderByColumn];
          if (aValue === bValue) return 0;
          if (isAsc === "desc") return aValue > bValue ? -1 : 1;
          return aValue > bValue ? 1 : -1;
        });
      }
      const total = rows.length;
      const start = (Number(pageNum) - 1) * Number(pageSize);
      const end = start + Number(pageSize);
      return Promise.resolve({
        rows: rows.slice(start, end),
        total,
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
        // 当前页面先使用本地模拟数据，后续联调时可直接替换为 listResult(this.formatParams())。
        const res = await this.queryMockList(this.formatParams());
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
