<template>
  <basic-container>
    <page-table
      tableRef="gsqDispatcherLogMainTable"
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
      :cell-style="cellStyle"
    >
      <template slot="header">
        <el-button
          @click="handleExport"
          v-hasPermi="['gsq:dispatcherLog:export']"
          >{{ $t("ui.frame.btn.export") }}</el-button
        >
      </template>
    </page-table>
  </basic-container>
</template>
<script>
//utils
import { downloadLink } from "@/utils/request";
//interface
import { listDispatcherLog } from "@/api/gsq/dispatcherLog";
import { listMachine } from "@/api/gsq/machine";

/**
 * 钢丝圈调度员排程操作日志页面（6班次制）
 *
 * 班次定义：
 *   1班：D日中班(16:00-24:00)
 *   2班：D+1日夜班(00:00-08:00)
 *   3班：D+1日早班(08:00-16:00)
 *   4班：D+1日中班(16:00-24:00)
 *   5班：D+2日夜班(00:00-08:00)
 *   6班：D+2日早班(08:00-16:00)
 */
export default {
  name: "GsqDispatcherLog",
  components: {},
  dicts: ["DISPATCHER_OPER_TYPE"],
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
      // 机台列表（用于机台编码反显机台名称）
      machineList: [],
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
          align: "center",
          label: this.$t("ui.data.column.scheduleResult.baseInfo"),
          children: [
            {
              prop: "operType",
              label: this.$t("ui.data.column.gsq.dispatcherlog.operType"),
              valign: "middle",
              align: "center",
              halign: "center",
              formatter: (row, column, value, index) => {
                return this.selectDictLabel(
                  this.dict.type.DISPATCHER_OPER_TYPE,
                  value
                );
              },
            },
            {
              prop: "scheduleDate",
              label: this.$t("ui.data.column.gsq.dispatcherlog.scheduleDate"),
              valign: "middle",
              align: "center",
              halign: "center",
              minWidth: 120,
            },
            {
              prop: "steelRingCode",
              label: this.$t(
                "ui.data.column.gsq.dispatcherlog.steelRingCode"
              ),
              valign: "middle",
              align: "center",
              halign: "center",
            },
            {
              prop: "createBy",
              label: this.$t("ui.data.column.dispatcherlog.createBy"),
              valign: "middle",
              align: "center",
              halign: "center",
              width: 120,
            },
            {
              prop: "createTime",
              label: this.$t("ui.data.column.dispatcherlog.createTime"),
              valign: "middle",
              align: "center",
              halign: "center",
              width: 180,
            },
          ],
        },
        {
          align: "center",
          label: this.$t("ui.data.column.dispatcherlog.beforeOper"),
          children: [
            {
              prop: "beforeMachineCode",
              label: this.$t(
                "ui.data.column.gsq.dispatcherlog.beforeMachineCode"
              ),
              valign: "middle",
              align: "center",
              halign: "center",
              formatter: (row, column, value, index) => {
                return this.formatMachineName(value);
              },
            },
            {
              prop: "beforeClass1Plan",
              label: this.$t(
                "ui.data.column.gsq.dispatcherlog.beforeClass1Plan"
              ),
              valign: "middle",
              align: "center",
              halign: "center",
            },
            {
              prop: "beforeClass2Plan",
              label: this.$t(
                "ui.data.column.gsq.dispatcherlog.beforeClass2Plan"
              ),
              valign: "middle",
              align: "center",
              halign: "center",
            },
            {
              prop: "beforeClass3Plan",
              label: this.$t(
                "ui.data.column.gsq.dispatcherlog.beforeClass3Plan"
              ),
              valign: "middle",
              align: "center",
              halign: "center",
            },
            {
              prop: "beforeClass4Plan",
              label: this.$t(
                "ui.data.column.gsq.dispatcherlog.beforeClass4Plan"
              ),
              valign: "middle",
              align: "center",
              halign: "center",
            },
            {
              prop: "beforeClass5Plan",
              label: this.$t(
                "ui.data.column.gsq.dispatcherlog.beforeClass5Plan"
              ),
              valign: "middle",
              align: "center",
              halign: "center",
            },
            {
              prop: "beforeClass6Plan",
              label: this.$t(
                "ui.data.column.gsq.dispatcherlog.beforeClass6Plan"
              ),
              valign: "middle",
              align: "center",
              halign: "center",
            },
          ],
        },
        {
          align: "center",
          label: this.$t("ui.data.column.dispatcherlog.AfterOper"),
          children: [
            {
              prop: "afterMachineCode",
              label: this.$t(
                "ui.data.column.gsq.dispatcherlog.afterMachineCode"
              ),
              valign: "middle",
              align: "center",
              halign: "center",
              formatter: (row, column, value, index) => {
                return this.formatMachineName(value);
              },
            },
            {
              prop: "afterClass1Plan",
              label: this.$t(
                "ui.data.column.gsq.dispatcherlog.afterClass1Plan"
              ),
              valign: "middle",
              align: "center",
              halign: "center",
            },
            {
              prop: "afterClass2Plan",
              label: this.$t(
                "ui.data.column.gsq.dispatcherlog.afterClass2Plan"
              ),
              valign: "middle",
              align: "center",
              halign: "center",
            },
            {
              prop: "afterClass3Plan",
              label: this.$t(
                "ui.data.column.gsq.dispatcherlog.afterClass3Plan"
              ),
              valign: "middle",
              align: "center",
              halign: "center",
            },
            {
              prop: "afterClass4Plan",
              label: this.$t(
                "ui.data.column.gsq.dispatcherlog.afterClass4Plan"
              ),
              valign: "middle",
              align: "center",
              halign: "center",
            },
            {
              prop: "afterClass5Plan",
              label: this.$t(
                "ui.data.column.gsq.dispatcherlog.afterClass5Plan"
              ),
              valign: "middle",
              align: "center",
              halign: "center",
            },
            {
              prop: "afterClass6Plan",
              label: this.$t(
                "ui.data.column.gsq.dispatcherlog.afterClass6Plan"
              ),
              valign: "middle",
              align: "center",
              halign: "center",
            },
          ],
        },
      ];

      return columns;
    },
    searchColumns() {
      return [
        {
          label: this.$t("ui.data.column.gsq.dispatcherlog.operType"),
          prop: "operType",
          type: "select",
          filterable: true,
          dictData: this.dict.type.DISPATCHER_OPER_TYPE,
        },
        {
          label: this.$t("ui.data.column.gsq.dispatcherlog.scheduleDate"),
          prop: "scheduleDate",
          type: "date",
          valueFormat: "yyyy-MM-dd",
        },
        {
          label: this.$t("ui.data.column.gsq.dispatcherlog.steelRingCode"),
          prop: "steelRingCode",
        },
        {
          label: this.$t("ui.data.column.dispatcherlog.createBy"),
          prop: "createBy",
        },
        {
          label: this.$t("ui.data.column.maintenance.log.createTime"),
          prop: "createTime",
          date: "date",
          dateType: "daterange",
          valueFormat: "yyyy-MM-dd",
        },
      ];
    },
  },
  methods: {
    /**
     * 机台编码反显机台名称，支持逗号分隔的多机台编码；
     * 未匹配到机台信息时回显原始编码（兼容已停用机台）
     */
    formatMachineName(value) {
      if (!value) {
        return "";
      }
      return value
        .split(",")
        .map((code) => {
          const machine = this.machineList.find(
            (item) => item.machineCode === code
          );
          if (machine && machine.machineName) {
            return machine.machineName;
          }
          return code;
        })
        .join(",");
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
    handleExport() {
      downloadLink("/gsq/dispatcherLog/export", this.formatParams(false));
    },
    /**
     * 单元格样式：操作前后值不一致高亮显示（CORAL红色）
     * 覆盖机台编码和6个班次计划量的比对
     */
    cellStyle({ row, column, rowIndex, columnIndex }) {
      // 机台编码比对
      if (column.property === "afterMachineCode") {
        if (row.beforeMachineCode != row.afterMachineCode) {
          return { background: "#FF7B7B" };
        }
      }
      // 6班次计划量比对
      if (column.property === "afterClass1Plan") {
        if (row.beforeClass1Plan != row.afterClass1Plan) {
          return { background: "#FF7B7B" };
        }
      }
      if (column.property === "afterClass2Plan") {
        if (row.beforeClass2Plan != row.afterClass2Plan) {
          return { background: "#FF7B7B" };
        }
      }
      if (column.property === "afterClass3Plan") {
        if (row.beforeClass3Plan != row.afterClass3Plan) {
          return { background: "#FF7B7B" };
        }
      }
      if (column.property === "afterClass4Plan") {
        if (row.beforeClass4Plan != row.afterClass4Plan) {
          return { background: "#FF7B7B" };
        }
      }
      if (column.property === "afterClass5Plan") {
        if (row.beforeClass5Plan != row.afterClass5Plan) {
          return { background: "#FF7B7B" };
        }
      }
      if (column.property === "afterClass6Plan") {
        if (row.beforeClass6Plan != row.afterClass6Plan) {
          return { background: "#FF7B7B" };
        }
      }
      return {};
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

      if (params.createTime && params.createTime[0]) {
        params.startTime = params.createTime[0];
        params.endTime = params.createTime[1];
        params.createTime = undefined;
      }

      return params;
    },
    // api
    async getList() {
      try {
        this.loading = true;
        const data = await listDispatcherLog(this.formatParams());
        this.data = data.rows;
        this.page.total = data.total;
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
    },
    /**
     * 加载机台列表用于编码反显机台名称
     */
    async loadMachineList() {
      try {
        const data = await listMachine({});
        this.machineList = data.rows || [];
      } catch (error) {
        console.error(error);
      }
    },
  },
  created() {
    this.loadMachineList();
  },
  activated() {
    this.getList();
  },
};
</script>
