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
          label: this.$t("ui.data.column.scheduleResult.batchNo"),
          prop: "batchNo",
          width: 180,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.scheduleDate"),
          prop: "scheduleDate",
          width: 160,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.materialCode"),
          prop: "materialCode",
        },
        {
          label: this.$t("ui.data.column.scheduleResult.materialDesc"),
          prop: "materialDesc",
          minWidth: 220,
          showOverflowTooltip: true,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.embryoDesc"),
          prop: "mainMaterialDesc",
          minWidth: 220,
          showOverflowTooltip: true,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.specifications"),
          prop: "specifications",
          minWidth: 180,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.unscheduledPlanQty"),
          prop: "unscheduledPlanQty",
          align: "right",
          minWidth: 120,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.unscheduledReason"),
          prop: "unscheduledReason",
          minWidth: 220,
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
          label: this.$t("ui.data.column.scheduleResult.scheduleDate"),
          prop: "scheduleDate",
          type: "date",
          dateType: "date",
          valueFormat: "yyyy-MM-dd",
          clearable: false,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.materialCode"),
          prop: "materialCode",
        },
        {
          label: this.$t("ui.data.column.scheduleResult.materialDesc"),
          prop: "materialDesc",
        },
        {
          label: this.$t("ui.data.column.scheduleResult.embryoDesc"),
          prop: "mainMaterialDesc",
        },
      ];
    },
  },
  methods: {
    getMockRows(scheduleDate) {
      return [
        {
          factoryCode: "116",
          batchNo: "LH20260430001",
          scheduleDate,
          materialCode: "MAT-0001",
          materialDesc: "205/55R16 示例物料描述",
          mainMaterialDesc: "胎胚A-示例",
          specifications: "205/55R16",
          unscheduledPlanQty: 120,
          unscheduledReason: "机台负荷不足",
          updateTime: "2026-04-30 15:00:00",
        },
        {
          factoryCode: "116",
          batchNo: "LH20260430001",
          scheduleDate,
          materialCode: "MAT-0002",
          materialDesc: "225/45R17 示例物料描述",
          mainMaterialDesc: "胎胚B-示例",
          specifications: "225/45R17",
          unscheduledPlanQty: 80,
          unscheduledReason: "胎胚库存不足",
          updateTime: "2026-04-30 15:00:00",
        },
      ];
    },
    queryMockList(params) {
      const rows = this.getMockRows(params.scheduleDate).filter((item) => {
        if (params.factoryCode && item.factoryCode !== params.factoryCode) return false;
        if (params.scheduleDate && item.scheduleDate !== params.scheduleDate) return false;
        if (params.materialCode && !item.materialCode.includes(params.materialCode)) return false;
        if (params.materialDesc && !item.materialDesc.includes(params.materialDesc)) return false;
        if (params.mainMaterialDesc && !item.mainMaterialDesc.includes(params.mainMaterialDesc)) return false;
        return true;
      });
      return Promise.resolve({
        rows,
        total: rows.length,
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
        // 当前页面使用本地模拟数据，后续接口联调时替换为 API 调用。
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
