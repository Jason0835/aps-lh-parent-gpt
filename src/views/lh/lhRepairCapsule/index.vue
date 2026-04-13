<template>
  <basic-container>
    <page-table
      tableRef="lhRepairCapsuleMainTable"
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
    </page-table>
  </basic-container>
</template>

<script>
import { listLhRepairCapsule } from "@/api/lh/lhRepairCapsule";

export default {
  name: "LhRepairCapsule",
  dicts: ["biz_factory_name", "biz_brand_type"],
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
      let columns = [
        { type: "selection", fixed: "left" },
        {
          prop: "factoryCode",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.factoryCode"),
          minWidth: 120,
          formatter: (row, column, value, index) => {
            return this.selectDictLabel(this.dict.type.biz_factory_name, value);
          },
        },
        {
          prop: "obtainTime",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.lhRepairCapsule.obtainTime"),
          minWidth: 150,
        },
        {
          prop: "lhCode",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.lhRepairCapsule.lhCode"),
          minWidth: 150,
        },
        {
          prop: "materialCode",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.lhRepairCapsule.materialCode"),
          minWidth: 150,
        },
        {
          prop: "replaceCapsuleCount",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.lhRepairCapsule.replaceCapsuleCount"),
          minWidth: 120,
        },
        {
          prop: "replaceCapsuleCount2",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.lhRepairCapsule.replaceCapsuleCount2"),
          minWidth: 120,
        },
        {
          prop: "brand",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.lhRepairCapsule.brand"),
          minWidth: 120,
          formatter: (row, column, value, index) => {
            return this.selectDictLabel(this.dict.type.biz_brand_type, value);
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
          label: this.$t("ui.data.column.lhRepairCapsule.obtainTime"),
          prop: "obtainTime",
          type: "date",
          valueFormat: "yyyy-MM-dd",
        },
        {
          label: this.$t("ui.data.column.lhRepairCapsule.lhCode"),
          prop: "lhCode",
        },
        {
          label: this.$t("ui.data.column.lhRepairCapsule.materialCode"),
          prop: "materialCode",
        },
        {
          label: this.$t("ui.data.column.lhRepairCapsule.brand"),
          prop: "brand",
          type: "select",
          dictData: this.dict.type.biz_brand_type,
          filterable: true,
        },
      ];
    },
  },
  methods: {
    handleSearch(data) {
      this.query = data;
      if (data.obtainTime) {
        // Backend expects begin/end fields; single-date search maps to that day.
        this.query.obtainTimeBegin = data.obtainTime;
        this.query.obtainTimeEnd = data.obtainTime;
        delete this.query.obtainTime;
      } else {
        delete this.query.obtainTimeBegin;
        delete this.query.obtainTimeEnd;
      }
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
    formatParams() {
      const params = {
        ...this.query,
        ...this.sort,
        pageSize: this.page.pageSize,
        pageNum: this.page.current,
      };
      return params;
    },
    async getList() {
      try {
        this.loading = true;
        const data = await listLhRepairCapsule(this.formatParams());
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
    this.search = {
      factoryCode: "116",
    };
    this.query = {
      factoryCode: "116",
    };
    this.getList();
  },
};
</script>

<style lang="scss" scoped>
</style>
