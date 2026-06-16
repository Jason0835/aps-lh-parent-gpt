
<template>
  <basic-container>
    <page-table
      tableRef="djDispatcherLogMainTable"
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
          v-hasPermi="['dj:dispatcherLog:export']"
          >{{ $t("ui.frame.btn.export") }}</el-button
        >
      </template>
    </page-table>
  </basic-container>
</template>
<script>
//lib
import { downloadLink } from "@/utils/request";
//interface
import { listDispatcherLog } from "@/api/dj/dispatcherLog";
import { getConfigKey } from "@/api/system/config";
//components
// import tltUpload from "@/components/tltUpload/tltUpload.vue";

// import infoDialog from "./components/infoDialog.vue";

export default {
  name: "djDispatcherLog",
  components: {
    // tltUpload,
    // infoDialog,
  },
  dicts: ["DISPATCHER_OPER_TYPE", "biz_factory_name"],
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
      page: {
        current: 1,
        pageSize: 20,
        total: 0,
      },
      sort: {},
      search: {
        factoryCode: '',
      },
      query: {
        factoryCode: '',
      },
      importDefaultValue: {},
      importRules: {},
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
              label: this.$t("ui.data.column.dj.dispatcherlog.operType"),
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
              label: this.$t("ui.data.column.dj.dispatcherlog.scheduleDate"),
              valign: "middle",
              align: "center",
              halign: "center",
              minWidth: 120,
            },
            {
              prop: "materialCode",
              label: this.$t("ui.data.column.dj.dispatcherlog.materialCode"),
              valign: "middle",
              align: "center",
              halign: "center",
            },
          ],
        },
        {
          align: "center",
          label: this.$t("ui.data.column.dispatcherlog.beforeOper"),
          children: [
            {
              prop: "beforeMachineCode",
              label: this.$t("ui.data.column.dj.dispatcherlog.beforeMachineCode"),
              valign: "middle",
              align: "center",
              halign: "center",
            },
            {
              prop: "beforeClass1PlanQty",
              label: this.$t("ui.data.column.dj.dispatcherlog.beforeClass1PlanQty"),
              valign: "middle",
              align: "right",
              halign: "center",
              type: "number",
            },
            {
              prop: "beforeClass2PlanQty",
              label: this.$t("ui.data.column.dj.dispatcherlog.beforeClass2PlanQty"),
              valign: "middle",
              align: "right",
              halign: "center",
              type: "number",
            },
            {
              prop: "beforeClass3PlanQty",
              label: this.$t("ui.data.column.dj.dispatcherlog.beforeClass3PlanQty"),
              valign: "middle",
              align: "right",
              halign: "center",
              type: "number",
            },
          ],
        },
        {
          align: "center",
          label: this.$t("ui.data.column.dispatcherlog.AfterOper"),
          children: [
            {
              prop: "afterMachineCode",
              label: this.$t("ui.data.column.dj.dispatcherlog.afterMachineId"),
              valign: "middle",
              align: "center",
              halign: "center",
            },
            {
              prop: "afterClass1PlanQty",
              label: this.$t("ui.data.column.dj.dispatcherlog.afterDayPlan"),
              valign: "middle",
              align: "right",
              halign: "center",
              type: "number",
            },
            {
              prop: "afterClass2PlanQty",
              label: this.$t("ui.data.column.dj.dispatcherlog.afterNightPlan"),
              valign: "middle",
              align: "right",
              halign: "center",
              type: "number",
            },
            {
              prop: "afterClass3PlanQty",
              label: this.$t("ui.data.column.dj.dispatcherlog.afterNightPlan"),
              valign: "middle",
              align: "right",
              halign: "center",
              type: "number",
            },
          ],
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
          label: this.$t("ui.data.column.dj.dispatcherlog.operType"),
          prop: "operType",
          type: "select",
          dictData: this.dict.type.DISPATCHER_OPER_TYPE,
        },
        {
          label: this.$t("ui.data.column.dj.dispatcherlog.scheduleDate"),
          prop: "scheduleDate",
          type: "date",
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
    handelSuccess() {
      this.getList();
    },
    handleSortChange({ column, prop, order }) {
      if (order) {
        this.sort = {
          orderByColumn: prop,
          isAsc: order == "ascending" ? "asc" : "desc",
        };
      } else {
        //默认排序
        this.sort = {};
      }
      this.getList();
    },
    handleSelectionChange(rows) {
      this.selection = rows;
    },
    handleExport() {
      downloadLink("/dj/dispatcherLog/export", this.formatParams(false));
    },

    cellStyle({ row, column, rowIndex, columnIndex }) {
      if (column.property === "afterMachineCode") {
        if (row.beforeMachineCode != row.afterMachineCode) {
          return { background: "#FF7B7B" };
        }
      }
      if (column.property === "afterClass1PlanQty") {
        if (row.beforeClass1PlanQty !== row.afterClass1PlanQty) {
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
  },
  created() {
    getConfigKey("sys.factory.code").then(response => {
      this.search.factoryCode = response.msg;
      this.query.factoryCode = response.msg;
      this.$store.dispatch("dj/getMachineList");
    }).catch(() => {
      this.$store.dispatch("dj/getMachineList");
    });
  },
  activated() {
    this.getList();
  },
};
</script>
<style lang="scss" scoped>
.more-btn {
  margin: 2px 0;
  width: 100%;
}
</style>
