<template>
  <basic-container class="month-plan-compare-wrap">
    <page-table
      tableRef="MonthPlanCompareMainTable"
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
      :showSummary="false"
      :selectArea="false"
      :span-method="objectSpanMethod"
      :cellClassName="cellClassName"
    >
      <template slot="header">
        <el-button
          @click="handleExport"
          v-hasPermi="['report:monthPlanCompare:export']"
          >{{ $t("ui.frame.btn.export") }}</el-button
        >
      </template>
      <template slot="headerRight"> </template>
    </page-table>
  </basic-container>
</template>
<script>
// lib
import moment from "moment";
// api
import { listMonthPlanCompare, exportMonthPlanCompare } from "@/api/monthplan/report";

export default {
  name: "MonthPlanCompare",
  dicts: ["biz_factory_name", "trial_status", "biz_product_type"],
  data() {
    // 默认查询条件：工厂=越南工厂116，年月=当前月
    const defaultDate = moment();
    const defaultYearMonth = defaultDate.format("yyyy-MM");
    const defaultDaysInMonth = defaultDate.daysInMonth();
    return {
      loading: false,
      data: [],
      // 默认搜索条件（在 data 中初始化，避免子组件 mounted 先于父组件触发 refresh 时参数为空）
      search: {
        factoryCode: "116",
        yearMonth: defaultYearMonth,
      },
      query: {
        factoryCode: "116",
        yearMonth: defaultYearMonth,
        year: defaultDate.year(),
        month: defaultDate.month() + 1,
      },
      // 当月天数（动态计算）
      daysInMonth: defaultDaysInMonth,
      // 分页对象（按SKU分页，pageSize=20 = 80行/页）
      page: {
        current: 1,
        pageSize: 20,
        total: 0,
      },
    };
  },
  computed: {
    // 动态列：固定列 + 按天数生成日期列
    columns() {
      const fixedCols = [
        {
          prop: "materialCode",
          label: this.$t("ui.data.column.monthPlanCompare.materialCode"),
          width: 140,
          align: "center",
        },
        {
          prop: "materialDesc",
          label: this.$t("ui.data.column.monthPlanCompare.materialDesc"),
          minWidth: 200,
          align: "left",
        },
        {
          prop: "rowTypeLabel",
          label: this.$t("ui.data.column.monthPlanCompare.rowType"),
          width: 90,
          align: "center",
        },
        {
          prop: "totalQty",
          label: this.$t("ui.data.column.monthPlanCompare.totalQty"),
          width: 110,
          align: "right",
          formatter: (row, column, value) => {
            return this.formatCell(row, value);
          },
        },
      ];
      // 动态日期列
      const dayCols = [];
      for (let d = 1; d <= this.daysInMonth; d++) {
        dayCols.push({
          prop: `day_${d}`,
          label: `${d}日`,
          width: 75,
          align: "right",
          formatter: (row, column, value) => {
            const idx = parseInt(column.property.replace("day_", "")) - 1;
            const dayValue = row.dayQtyList ? row.dayQtyList[idx] : null;
            return this.formatCell(row, dayValue);
          },
        });
      }
      return [...fixedCols, ...dayCols];
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
          label: this.$t("ui.data.colume.yearMonth"),
          prop: "yearMonth",
          type: "date",
          dateType: "month",
          valueFormat: "yyyy-MM",
          clearable: false,
        },
        {
          label: this.$t("ui.data.column.monthPlanCompare.materialCode"),
          prop: "materialCode",
          type: "input",
        },
        {
          label: this.$t("ui.data.column.monthPlanCompare.materialDesc"),
          prop: "materialDesc",
          type: "input",
        },
        {
          label: this.$t("ui.data.column.monthPlanCompare.productStatus"),
          prop: "productStatus",
          type: "select",
          dictData: this.dict.type.trial_status,
          filterable: true,
        },
        {
          label: this.$t("ui.data.column.monthPlanCompare.productTypeCode"),
          prop: "productTypeCode",
          type: "select",
          dictData: this.dict.type.biz_product_type,
          filterable: true,
        },
      ];
    },
  },
  // 主动触发首次查询（早于子组件 mounted，确保带默认参数请求）
  created() {
    this.getList();
  },
  methods: {
    // 必填校验：年月、工厂必填
    validateRequired() {
      if (!this.query.yearMonth) {
        this.$message.warning("请选择年月");
        return false;
      }
      if (!this.query.factoryCode) {
        this.$message.warning("请选择工厂");
        return false;
      }
      return true;
    },
    handleSearch(data) {
      this.query = { ...data };
      if (!this.validateRequired()) return;
      // 查询条件变化时重置到第1页
      this.$set(this.page, "current", 1);
      // 根据年月重新计算当月天数
      if (data.yearMonth) {
        const arr = data.yearMonth.split("-");
        this.query.year = parseInt(arr[0]);
        this.query.month = parseInt(arr[1]);
        this.daysInMonth = moment(data.yearMonth, "YYYY-MM").daysInMonth();
      }
      this.getList();
    },
    // 翻页/切换每页条数
    handlePageChange(current, pageSize) {
      this.$set(this.page, "current", current);
      this.$set(this.page, "pageSize", pageSize);
      this.getList();
    },
    handleExport() {
      if (!this.validateRequired()) return;
      // 导出全量数据，不传分页参数
      exportMonthPlanCompare(this.formatParams(false));
    },
    // 行合并：物料编码、物料描述每4行合并
    objectSpanMethod({ row, column, rowIndex, columnIndex }) {
      if (columnIndex === 0 || columnIndex === 1) {
        if (rowIndex % 4 === 0) {
          return {
            rowspan: 4,
            colspan: 1,
          };
        } else {
          return {
            rowspan: 0,
            colspan: 0,
          };
        }
      }
    },
    // 单元格样式（完成率着色）
    cellClassName({ row, column, rowIndex, columnIndex }) {
      if (row.rowType === "rate" && columnIndex >= 4) {
        const idx = parseInt(column.property.replace("day_", "")) - 1;
        const val = row.dayQtyList ? row.dayQtyList[idx] : null;
        if (val != null && val !== "") {
          const numVal = parseFloat(val);
          if (!isNaN(numVal)) {
            if (numVal < 60) return "cell-rate-red";
            if (numVal < 80) return "cell-rate-yellow";
          }
        }
      }
      if (row.rowType === "rate" && columnIndex === 3) {
        const val = row.totalQty;
        if (val != null && val !== "") {
          const numVal = parseFloat(val);
          if (!isNaN(numVal)) {
            if (numVal < 60) return "cell-rate-red";
            if (numVal < 80) return "cell-rate-yellow";
          }
        }
      }
      return "";
    },
    // 格式化单元格显示
    formatCell(row, value) {
      if (row.rowType === "rate") {
        // 完成率行：null显示"-"，否则加%
        if (value === null || value === undefined || value === "") {
          return "-";
        }
        const numVal = parseFloat(value);
        if (isNaN(numVal)) return "-";
        return numVal.toFixed(2) + "%";
      }
      // 其他行：数值显示
      if (value === null || value === undefined || value === "") {
        return "0";
      }
      return value;
    },
    /**
     * 组装请求参数
     * @param hasPage 是否包含分页参数（列表查询=true，导出=false）
     */
    formatParams(hasPage = true) {
      const params = { ...this.query };
      if (params.yearMonth) {
        const arr = params.yearMonth.split("-");
        params.year = parseInt(arr[0]);
        params.month = parseInt(arr[1]);
        delete params.yearMonth;
      }
      // 列表查询时传分页参数（按SKU分页）
      if (hasPage) {
        params.pageNum = this.page.current;
        params.pageSize = this.page.pageSize;
      }
      return params;
    },
    // api
    async getList() {
      try {
        this.loading = true;
        const res = await listMonthPlanCompare(this.formatParams(true));
        this.data = res.rows || [];
        // 更新分页总数（total = SKU 总数）
        this.$set(this.page, "total", res.total || 0);
      } catch (e) {
        console.error(e);
        this.data = [];
        this.$set(this.page, "total", 0);
      } finally {
        this.loading = false;
      }
    },
  },
};
</script>
<style lang="scss" scoped>
::v-deep .cell-rate-red {
  background-color: #fde2e2;
  color: #f56c6c;
  font-weight: bold;
}
::v-deep .cell-rate-yellow {
  background-color: #faecd8;
  color: #e6a23c;
  font-weight: bold;
}
/* 调深表格边框颜色（原色 #dfe6ec 过浅） */
::v-deep .el-table--border .el-table__cell {
  border-right-color: #909399 !important;
  border-bottom-color: #909399 !important;
}
::v-deep .el-table--border th.el-table__cell {
  border-right-color: #909399 !important;
  border-bottom-color: #909399 !important;
}
</style>
