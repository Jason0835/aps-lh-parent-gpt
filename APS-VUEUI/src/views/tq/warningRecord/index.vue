<template>
  <basic-container>
    <page-table
      tableRef="tqWarningRecordMainTable"
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
      <template slot="header">
        <el-button @click="handleExport" v-hasPermi="['tq:warningRecord:export']">{{
          $t("ui.frame.btn.export")
        }}</el-button>
      </template>
    </page-table>
    <handleDialog ref="handleRef" @success="getList" />
  </basic-container>
</template>
<script>
import { downloadLink } from "@/utils/request";
import { listWarningRecord } from "@/api/tq/warningRecord";
import handleDialog from "./components/handleDialog.vue";

export default {
  name: "TqWarningRecord",
  components: {
    handleDialog,
  },
  dicts: ["biz_factory_name", "tq_warning_type", "tq_warning_level", "tq_warning_status"],
  provide() {
    return {
      parentDict: this.dict,
    };
  },
  data() {
    return {
      searchColumns: [
        {
          label: this.$t("ui.data.column.tqWarningRecord.factoryCode"),
          prop: "factoryCode",
          type: "select",
          dict: "biz_factory_name",
          filterable: true,
        },
        {
          label: this.$t("ui.data.column.tqWarningRecord.warningType"),
          prop: "warningType",
          type: "select",
          dict: "tq_warning_type",
          filterable: true,
        },
        {
          label: this.$t("ui.data.column.tqWarningRecord.warningLevel"),
          prop: "warningLevel",
          type: "select",
          dict: "tq_warning_level",
          filterable: true,
        },
        {
          label: this.$t("ui.data.column.tqWarningRecord.status"),
          prop: "status",
          type: "select",
          dict: "tq_warning_status",
          filterable: true,
        },
        {
          label: this.$t("ui.data.column.tqWarningRecord.beadCode"),
          prop: "beadCode",
        },
        {
          label: this.$t("ui.data.column.tqWarningRecord.machineCode"),
          prop: "machineCode",
        },
        {
          label: this.$t("ui.data.column.tqWarningRecord.scheduleDate"),
          prop: "scheduleDate",
          type: "date",
          dateType: "daterange",
          valueFormat: "yyyy-MM-dd",
        },
      ],
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
          label: this.$t("ui.data.column.tqWarningRecord.factoryCode"),
          minWidth: 120,
          formatter: (row) =>
            this.selectDictLabel(this.dict.type.biz_factory_name, row.factoryCode),
        },
        {
          prop: "warningType",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.tqWarningRecord.warningType"),
          formatter: (row) =>
            this.selectDictLabel(this.dict.type.tq_warning_type, row.warningType),
        },
        {
          prop: "warningLevel",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.tqWarningRecord.warningLevel"),
          formatter: (row) =>
            this.selectDictLabel(this.dict.type.tq_warning_level, row.warningLevel),
        },
        {
          prop: "beadCode",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.tqWarningRecord.beadCode"),
        },
        {
          prop: "machineCode",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.tqWarningRecord.machineCode"),
        },
        {
          prop: "scheduleDate",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.tqWarningRecord.scheduleDate"),
          minWidth: 100,
        },
        {
          prop: "shiftIndex",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.tqWarningRecord.shiftIndex"),
        },
        {
          prop: "warningTitle",
          align: "left",
          halign: "center",
          label: this.$t("ui.data.column.tqWarningRecord.warningTitle"),
          minWidth: 150,
        },
        {
          prop: "warningContent",
          align: "left",
          halign: "center",
          label: this.$t("ui.data.column.tqWarningRecord.warningContent"),
          minWidth: 200,
          showOverflowTooltip: true,
        },
        {
          prop: "planQty",
          align: "right",
          halign: "center",
          label: this.$t("ui.data.column.tqWarningRecord.planQty"),
        },
        {
          prop: "finishQty",
          align: "right",
          halign: "center",
          label: this.$t("ui.data.column.tqWarningRecord.finishQty"),
        },
        {
          prop: "finishRate",
          align: "right",
          halign: "center",
          label: this.$t("ui.data.column.tqWarningRecord.finishRate"),
          formatter: (row) =>
            row.finishRate != null
              ? (row.finishRate * 100).toFixed(2) + "%"
              : "",
        },
        {
          prop: "stockNum",
          align: "right",
          halign: "center",
          label: this.$t("ui.data.column.tqWarningRecord.stockNum"),
        },
        {
          prop: "threshold",
          align: "right",
          halign: "center",
          label: this.$t("ui.data.column.tqWarningRecord.threshold"),
        },
        {
          prop: "status",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.tqWarningRecord.status"),
          formatter: (row) =>
            this.selectDictLabel(this.dict.type.tq_warning_status, row.status),
        },
        {
          prop: "handler",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.tqWarningRecord.handler"),
        },
        {
          prop: "handleTime",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.tqWarningRecord.handleTime"),
          minWidth: 150,
        },
        {
          prop: "handleOpinion",
          align: "left",
          halign: "center",
          label: this.$t("ui.data.column.tqWarningRecord.handleOpinion"),
          minWidth: 150,
          showOverflowTooltip: true,
        },
        {
          prop: "notified",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.tqWarningRecord.notified"),
          formatter: (row) => (row.notified === 1 ? "是" : "否"),
        },
        {
          prop: "notifyTime",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.tqWarningRecord.notifyTime"),
          minWidth: 150,
        },
        {
          prop: "createTime",
          align: "center",
          halign: "center",
          label: this.$t("ui.common.column.createTime"),
          minWidth: 150,
        },
        {
          align: "center",
          halign: "center",
          label: this.$t("ui.data.btn.option"),
          minWidth: 160,
          width: 160,
          fixed: "right",
          render: ({ row }) => {
            return (
              <div>
                {row.status === "0" && (
                  <el-button
                    v-hasPermi={["tq:warningRecord:handle"]}
                    class="minus"
                    type="primary"
                    onClick={() => this.handleProcess(row)}
                  >
                    {this.$t("ui.data.btn.tqWarningRecord.handle")}
                  </el-button>
                )}
              </div>
            );
          },
        },
      ];

      return columns;
    },
  },
  methods: {
    handleProcess(row) {
      if (this.$refs.handleRef) {
        this.$refs.handleRef.show(row);
      }
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
      downloadLink("/tqWarningRecord/export", this.formatParams(false));
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

      if (params.scheduleDate && params.scheduleDate[0]) {
        params.scheduleDateStart = params.scheduleDate[0];
        params.scheduleDateEnd = params.scheduleDate[1];
        params.scheduleDate = undefined;
      }

      return params;
    },
    async getList() {
      try {
        this.loading = true;
        const data = await listWarningRecord(this.formatParams());
        this.data = data.rows;
        this.page.total = data.total;
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
    },
  },
  created() {},
  activated() {
    this.getList();
  },
};
</script>
