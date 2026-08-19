<template>
  <basic-container>
    <page-table
      tableRef="lhDayPlanAdjustRequireMainTable"
      :calcHeight="true"
      v-loading="loading"
      :columns="columns"
      :searchColumns="searchColumns"
      :data="data"
      :page="page"
      :search="search"
      @refresh="handleRefresh"
      @search="handleSearch"
      @pageChange="handlePageChange"
      :showSummary="false"
      :selectArea="false"
    />
    <edit-dialog ref="editDialogRef" @success="getList" />
  </basic-container>
</template>

<script>
import moment from "moment";
import { listLhDayPlanAdjustRequire } from "@/api/lh/lhDayPlanAdjustRequire";
import EditDialog from "./components/editDialog.vue";

const ADJUST_SLOT_COUNT = 3;

export default {
  name: "LhDayPlanAdjustRequire",
  components: { EditDialog },
  dicts: ["biz_factory_name", "lh_trial_status"],
  provide() {
    return {
      parentDict: this.dict,
    };
  },
  data() {
    const currentYearMonth = moment().format("yyyy-MM");
    return {
      loading: false,
      data: [],
      page: {
        current: 1,
        pageSize: 20,
        total: 0,
      },
      search: {
        factoryCode: "116",
        yearMonth: currentYearMonth,
      },
      query: {
        factoryCode: "116",
        yearMonth: currentYearMonth,
      },
    };
  },
  computed: {
    columns() {
      return [
        {
          prop: "materialCode",
          label: this.$t("ui.data.column.lhDayPlanAdjustRequire.materialCode"),
          minWidth: 150,
          fixed: "left",
        },
        {
          prop: "materialDesc",
          label: this.$t("ui.data.column.lhDayPlanAdjustRequire.materialDesc"),
          minWidth: 260,
          align: "left",
        },
        {
          prop: "yearMonth",
          label: this.$t("ui.data.column.lhDayPlanAdjustRequire.yearMonth"),
          width: 100,
          formatter: (row, column, value) => this.formatYearMonth(value),
        },
        {
          prop: "monthPlanQty",
          label: this.$t("ui.data.column.lhDayPlanAdjustRequire.monthPlanQty"),
          minWidth: 110,
          align: "right",
        },
        {
          label: this.$t("ui.data.column.lhDayPlanAdjustRequire.adjustGroup"),
          align: "center",
          children: this.buildAdjustQtyColumns(),
        },
        {
          prop: "adjustedTotalQty",
          label: this.$t("ui.data.column.lhDayPlanAdjustRequire.adjustedTotalQty"),
          minWidth: 130,
          align: "right",
        },
        {
          prop: "treadGlueTd",
          label: this.$t("ui.data.column.lhDayPlanAdjustRequire.treadGlueTd"),
          minWidth: 150,
          align: "left",
        },
        {
          label: this.$t("ui.data.column.lhDayPlanAdjustRequire.reasonGroup"),
          align: "center",
          children: this.buildAdjustReasonColumns(),
        },
        {
          prop: "productStatus",
          label: this.$t("ui.data.column.lhDayPlanAdjustRequire.productStatus"),
          minWidth: 110,
          formatter: (row, column, value) =>
            this.selectDictLabel(this.dict.type.lh_trial_status, value),
        },
        {
          prop: "adjuster",
          label: this.$t("ui.data.column.lhDayPlanAdjustRequire.lastAdjuster"),
          minWidth: 120,
        },
        {
          prop: "updateTime",
          label: this.$t("ui.data.column.lhDayPlanAdjustRequire.lastAdjustTime"),
          minWidth: 170,
        },
        {
          prop: "option",
          label: this.$t("ui.data.btn.option"),
          width: 100,
          fixed: "right",
          align: "center",
          render: ({ row }) => (
            <el-button
              v-hasPermi={["lh:dayPlanAdjustRequire:save"]}
              type="primary"
              size="mini"
              icon="el-icon-edit"
              onClick={() => this.handleEdit(row)}
            >
              {this.$t("ui.data.column.lhDayPlanAdjustRequire.editAction")}
            </el-button>
          ),
        },
      ];
    },
    searchColumns() {
      return [
        {
          label: this.$t("ui.data.column.lhDayPlanAdjustRequire.factoryCode"),
          prop: "factoryCode",
          type: "select",
          dictData: this.dict.type.biz_factory_name,
          filterable: true,
          clearable: false,
        },
        {
          label: this.$t("ui.data.column.lhDayPlanAdjustRequire.yearMonth"),
          prop: "yearMonth",
          type: "date",
          dateType: "month",
          valueFormat: "yyyy-MM",
          clearable: false,
        },
        {
          label: this.$t("ui.data.column.lhDayPlanAdjustRequire.materialCode"),
          prop: "materialCode",
        },
        {
          label: this.$t("ui.data.column.lhDayPlanAdjustRequire.materialDesc"),
          prop: "materialDesc",
        },
        {
          label: this.$t("ui.data.column.lhDayPlanAdjustRequire.productStatus"),
          prop: "productStatus",
          type: "select",
          dictData: this.dict.type.lh_trial_status,
          filterable: true,
        },
      ];
    },
  },
  methods: {
    buildAdjustQtyColumns() {
      return Array.from({ length: ADJUST_SLOT_COUNT }, (item, index) => {
        const adjustIndex = index + 1;
        return {
          prop: `adjustQty${adjustIndex}`,
          label: this.$t("ui.data.column.lhDayPlanAdjustRequire.adjust", [adjustIndex]),
          minWidth: 100,
          align: "right",
        };
      });
    },
    buildAdjustReasonColumns() {
      return Array.from({ length: ADJUST_SLOT_COUNT }, (item, index) => {
        const adjustIndex = index + 1;
        return {
          prop: `adjustReason${adjustIndex}`,
          label: this.$t("ui.data.column.lhDayPlanAdjustRequire.reason", [adjustIndex]),
          minWidth: 220,
          align: "left",
          showOverflowTooltip: true,
        };
      });
    },
    async handleSearch(data) {
      this.query = { ...data };
      this.$set(this.page, "current", 1);
      await this.getList();
    },
    async handlePageChange(current, pageSize) {
      this.$set(this.page, "current", current);
      this.$set(this.page, "pageSize", pageSize);
      await this.getList();
    },
    async handleRefresh() {
      await this.getList();
    },
    handleEdit(row) {
      this.$refs.editDialogRef.show(row);
    },
    formatYearMonth(value) {
      const yearMonth = String(value || "");
      return yearMonth.length === 6
        ? `${yearMonth.slice(0, 4)}-${yearMonth.slice(4)}`
        : yearMonth;
    },
    formatParams() {
      return {
        ...this.query,
        yearMonth: String(this.query.yearMonth).replace("-", ""),
        pageNum: this.page.current,
        pageSize: this.page.pageSize,
      };
    },
    async getList() {
      try {
        this.loading = true;
        const result = await listLhDayPlanAdjustRequire(this.formatParams());
        this.data = result.rows || [];
        this.page.total = result.total || 0;
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

<style lang="scss" scoped></style>
