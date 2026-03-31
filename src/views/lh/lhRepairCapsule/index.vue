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
  dicts: ["biz_factory_name"],
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
        },
        {
          label: this.$t("ui.data.column.lhRepairCapsule.obtainTime"),
          prop: "obtainTime",
          type: "date",
          dateType: "daterange",
          valueFormat: "yyyy-MM-dd",
          startPlaceholder: this.$t("common.startTime"),
          endPlaceholder: this.$t("common.endTime"),
        },
        {
          label: this.$t("ui.data.column.lhRepairCapsule.lhCode"),
          prop: "lhCode",
        },
        {
          label: this.$t("ui.data.column.lhRepairCapsule.materialCode"),
          prop: "materialCode",
        },
      ];
    },
  },
  methods: {
    handleSearch(data) {
      this.query = data;
      if (data.obtainTime && data.obtainTime.length === 2) {
        this.query.obtainTimeBegin = data.obtainTime[0];
        this.query.obtainTimeEnd = data.obtainTime[1];
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
    this.getList();
  },
};
</script>

<style lang="scss" scoped>
</style>
