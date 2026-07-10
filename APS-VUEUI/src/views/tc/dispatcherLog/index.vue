
<template>
  <basic-container>
    <page-table
      v-loading="loading"
      :calcHeight="true"
      :cell-style="cellStyle"
      :columns="columns"
      :data="data"
      :page="page"
      :search="search"
      :searchColumns="searchColumns"
      :selectArea="false"
      :showSummary="false"
      tableRef="tcDispatcherLogMainTable"
      @pageChange="handlePageChange"
      @refresh="getList"
      @search="handleSearch"
      @sort-change="handleSortChange"
      @selection-change="handleSelectionChange"
    >
      <template slot="header">
        <el-button
          v-hasPermi="['tc:tcDispatcherLog:export']"
          @click="handleExport"
          >{{ $t("ui.frame.btn.export") }}</el-button
        >
      </template>
    </page-table>
  </basic-container>
</template>
<script>
import {downloadLink} from "@/utils/request";
import {listDispatcherLog} from "@/api/tc/dispatcherLog";
import {listTcMachineInfo} from "@/api/tc/machineInfo";

export default {
  name: "TcDispatcherLog",
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
    machineMap() {
      let obj = {};
      this.machineList.forEach((machine) => {
        obj[machine.machineCode] = machine.machineName;
      });
      return obj;
    },
    columns() {
      return [
        { type: "selection", fixed: "left" },
        {
          align: "center",
          label: this.$t("ui.data.column.tc.dispatcherLog.baseInfo"),
          children: [
            {
              prop: "operType",
              label: this.$t("ui.data.column.tc.dispatcherLog.operType"),
              valign: "middle",
              align: "center",
              halign: "center",
              formatter: (row, column, value) => {
                return this.selectDictLabel(
                  this.dict.type.DISPATCHER_OPER_TYPE,
                  value
                );
              },
            },
            {
              prop: "scheduleDate",
              label: this.$t("ui.data.column.tc.dispatcherLog.scheduleDate"),
              valign: "middle",
              align: "center",
              halign: "center",
              minWidth: 120,
            },
            {
              prop: "sidewallCode",
              label: this.$t("ui.data.column.tc.dispatcherLog.sidewallCode"),
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
              width: 140,
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
          label: this.$t("ui.data.column.tc.dispatcherLog.beforeOper"),
          children: [
            {
              prop: "beforeMachineCode",
              label: this.$t("ui.data.column.tc.dispatcherLog.machineCode"),
              valign: "middle",
              align: "center",
              halign: "center",
              width: 200,
              formatter: (row, column, value) => {
                if (this.isEmpty(value)) return "";
                return this.selectMachineName(value);
              },
            },
            {
              prop: "beforeClass1PlanQty",
              label: this.$t("ui.data.column.tc.dispatcherLog.class1PlanQty"),
              valign: "middle",
              align: "center",
              halign: "center",
            },
            {
              prop: "beforeClass2PlanQty",
              label: this.$t("ui.data.column.tc.dispatcherLog.class2PlanQty"),
              valign: "middle",
              align: "center",
              halign: "center",
            },
            {
              prop: "beforeClass3PlanQty",
              label: this.$t("ui.data.column.tc.dispatcherLog.class3PlanQty"),
              valign: "middle",
              align: "center",
              halign: "center",
            },
            {
              prop: "beforeClass4PlanQty",
              label: this.$t("ui.data.column.tc.dispatcherLog.class4PlanQty"),
              valign: "middle",
              align: "center",
              halign: "center",
            },
            {
              prop: "beforeClass5PlanQty",
              label: this.$t("ui.data.column.tc.dispatcherLog.class5PlanQty"),
              valign: "middle",
              align: "center",
              halign: "center",
            },
            {
              prop: "beforeClass6PlanQty",
              label: this.$t("ui.data.column.tc.dispatcherLog.class6PlanQty"),
              valign: "middle",
              align: "center",
              halign: "center",
            },
          ],
        },
        {
          align: "center",
          label: this.$t("ui.data.column.tc.dispatcherLog.afterOper"),
          children: [
            {
              prop: "afterMachineCode",
              label: this.$t("ui.data.column.tc.dispatcherLog.machineCode"),
              valign: "middle",
              align: "center",
              halign: "center",
              width: 200,
              formatter: (row, column, value) => {
                if (this.isEmpty(value)) return "";
                return this.selectMachineName(value);
              },
            },
            {
              prop: "afterClass1PlanQty",
              label: this.$t("ui.data.column.tc.dispatcherLog.class1PlanQty"),
              valign: "middle",
              align: "center",
              halign: "center",
            },
            {
              prop: "afterClass2PlanQty",
              label: this.$t("ui.data.column.tc.dispatcherLog.class2PlanQty"),
              valign: "middle",
              align: "center",
              halign: "center",
            },
            {
              prop: "afterClass3PlanQty",
              label: this.$t("ui.data.column.tc.dispatcherLog.class3PlanQty"),
              valign: "middle",
              align: "center",
              halign: "center",
            },
            {
              prop: "afterClass4PlanQty",
              label: this.$t("ui.data.column.tc.dispatcherLog.class4PlanQty"),
              valign: "middle",
              align: "center",
              halign: "center",
            },
            {
              prop: "afterClass5PlanQty",
              label: this.$t("ui.data.column.tc.dispatcherLog.class5PlanQty"),
              valign: "middle",
              align: "center",
              halign: "center",
            },
            {
              prop: "afterClass6PlanQty",
              label: this.$t("ui.data.column.tc.dispatcherLog.class6PlanQty"),
              valign: "middle",
              align: "center",
              halign: "center",
            },
          ],
        },
      ];
    },
    searchColumns() {
      return [
        {
          label: this.$t("ui.data.column.tc.dispatcherLog.operType"),
          prop: "operType",
          type: "select",
          dictData: this.dict.type.DISPATCHER_OPER_TYPE,
        },
        {
          label: this.$t("ui.data.column.tc.dispatcherLog.scheduleDate"),
          prop: "scheduleDate",
          type: "date",
          valueFormat: "yyyy-MM-dd",
        },
        {
          label: this.$t("ui.data.column.tc.dispatcherLog.sidewallCode"),
          prop: "sidewallCode",
        },
        {
          label: this.$t("ui.data.column.dispatcherlog.createBy"),
          prop: "createBy",
        },
        {
          label: this.$t("ui.data.column.dispatcherlog.createTime"),
          prop: "createTime",
          type: "date",
          dateType: "daterange",
          valueFormat: "yyyy-MM-dd",
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
      downloadLink("/tc/tcDispatcherLog/export", this.formatParams(false));
    },

    selectMachineName(value) {
      if (!value) return "";
      var arr = value.split(",");
      let strArr = arr.map((code) => {
        return this.machineMap[code] || code;
      });
      return strArr.join(",");
    },

    cellStyle({ row, column }) {
      if (column.property === "afterMachineCode") {
        if (row.beforeMachineCode != row.afterMachineCode) {
          return { background: "#FF7B7B" };
        }
      }
      if (column.property === "afterClass1PlanQty") {
        if (row.beforeClass1PlanQty != row.afterClass1PlanQty) {
          return { background: "#FF7B7B" };
        }
      }
      if (column.property === "afterClass2PlanQty") {
        if (row.beforeClass2PlanQty != row.afterClass2PlanQty) {
          return { background: "#FF7B7B" };
        }
      }
      if (column.property === "afterClass3PlanQty") {
        if (row.beforeClass3PlanQty != row.afterClass3PlanQty) {
          return { background: "#FF7B7B" };
        }
      }
      if (column.property === "afterClass4PlanQty") {
        if (row.beforeClass4PlanQty != row.afterClass4PlanQty) {
          return { background: "#FF7B7B" };
        }
      }
      if (column.property === "afterClass5PlanQty") {
        if (row.beforeClass5PlanQty != row.afterClass5PlanQty) {
          return { background: "#FF7B7B" };
        }
      }
      if (column.property === "afterClass6PlanQty") {
        if (row.beforeClass6PlanQty != row.afterClass6PlanQty) {
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

    async loadMachineList() {
      try {
        const data = await listTcMachineInfo({ pageSize: 9999, pageNum: 1 });
        this.machineList = data.rows || [];
      } catch (e) {
        console.error(e);
      }
    },

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
  },
  created() {
    this.loadMachineList();
  },
  activated() {
    this.getList();
  },
};
</script>
<style lang="scss" scoped>
</style>
