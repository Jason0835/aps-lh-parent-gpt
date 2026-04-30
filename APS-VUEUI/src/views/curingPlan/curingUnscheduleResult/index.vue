<template>
  <basic-container>
    <page-table
      tableRef="curingUnscheduleResultTable"
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
      :showSummary="false"
      :selectArea="false"
    />
  </basic-container>
</template>

<script>
import moment from "moment";
import { listCuringUnscheduleResult } from "@/api/lh/lhUnscheduledResult";

export default {
  name: "CuringUnscheduleResult",
  dicts: ["biz_factory_name"],
  data() {
    const defaultScheduleDate = this.$route.query.scheduleDate || moment().add(2, "days").format("YYYY-MM-DD");
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
        factoryCode: this.$route.query.factoryCode || "116",
        scheduleDate: defaultScheduleDate,
      },
      query: {
        factoryCode: this.$route.query.factoryCode || "116",
        scheduleDate: defaultScheduleDate,
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
          label: this.$t("ui.data.column.lhUnscheduledResult.batchNo"),
          prop: "batchNo",
          width: 180,
        },
        {
          label: this.$t("ui.data.column.lhUnscheduledResult.scheduleDate"),
          prop: "scheduleDate",
          width: 160,
        },
        {
          label: this.$t("ui.data.column.lhUnscheduledResult.materialCode"),
          prop: "materialCode",
        },
        {
          label: this.$t("ui.data.column.lhUnscheduledResult.materialDesc"),
          prop: "materialDesc",
          minWidth: 220,
          showOverflowTooltip: true,
        },
        {
          label: this.$t("ui.data.column.lhUnscheduledResult.mainMaterialDesc"),
          prop: "mainMaterialDesc",
          minWidth: 220,
          showOverflowTooltip: true,
        },
        {
          label: this.$t("ui.data.column.lhUnscheduledResult.specDesc"),
          prop: "specDesc",
          minWidth: 180,
          showOverflowTooltip: true,
        },
        {
          label: this.$t("ui.data.column.lhUnscheduledResult.unscheduledQty"),
          prop: "unscheduledQty",
          align: "right",
          minWidth: 120,
        },
        {
          label: this.$t("ui.data.column.lhUnscheduledResult.unscheduledReason"),
          prop: "unscheduledReason",
          minWidth: 220,
          showOverflowTooltip: true,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.updateTime"),
          prop: "processedTime",
          minWidth: 160,
          formatter: (row) => row.processedTime || row.updateTime,
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
          label: this.$t("ui.data.column.lhUnscheduledResult.scheduleDate"),
          prop: "scheduleDate",
          type: "date",
          dateType: "date",
          valueFormat: "yyyy-MM-dd",
          clearable: false,
        },
        {
          label: this.$t("ui.data.column.lhUnscheduledResult.materialCode"),
          prop: "materialCode",
        },
        {
          label: this.$t("ui.data.column.lhUnscheduledResult.materialDesc"),
          prop: "materialDesc",
        },
        {
          label: this.$t("ui.data.column.lhUnscheduledResult.mainMaterialDesc"),
          prop: "mainMaterialDesc",
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
        const res = await listCuringUnscheduleResult(this.formatParams());
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
