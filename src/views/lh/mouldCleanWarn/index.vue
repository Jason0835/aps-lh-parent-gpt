<template>
  <basic-container>
    <page-table
      tableRef="mouldCleanWarnMainTable"
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
        <el-button
          v-hasPermi="['lh:mouldCleanWarn:export']"
          @click="handleExport"
          >{{ $t("ui.frame.btn.export") }}</el-button
        >
      </template>
    </page-table>
  </basic-container>
</template>

<script>
import { listMouldCleanWarn } from "@/api/lh/mouldCleanWarn";
import { listMachine } from "@/api/lh/machine";
import { downloadLink } from "@/utils/ruoyi";

export default {
  name: "LhMouldCleanWarn",
  dicts: ["biz_factory_name"],
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
      search: {},
      query: {},
      machineOptions: [],
      machineLoading: false,
    };
  },
  computed: {
    columns() {
      let columns = [
        { type: "selection", fixed: "left" },
        {
          prop: "factoryCode",
          label: this.$t("common.factory"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_factory_name, value);
          },
        },
        {
          prop: "lhCode",
          label: this.$t("ui.data.column.mouldCleanWarn.lhCode"),
          width: 180
        },
        {
          prop: "operTime",
          label: this.$t("ui.data.column.mouldCleanWarn.operTime"),
        },
        {
          prop: "firstWashTime",
          label: this.$t("ui.data.column.mouldCleanWarn.firstWashTime"),
        },
        {
          prop: "secondWashTime",
          label: this.$t("ui.data.column.mouldCleanWarn.secondWashTime"),
        },
        {
          prop: "remark",
          label: this.$t("ui.data.column.mouldCleanWarn.remark"),
          showOverflowTooltip: true
        },
        {
          prop: "createTime",
          label: this.$t("ui.data.column.mouldCleanWarn.createTime"),
        },
      ];

      return columns;
    },
    searchColumns() {
      return [
        {
          label: this.$t("ui.data.column.mouldCleanWarn.lhCode"),
          prop: "lhCode",
          type: "select",
          dictData: this.machineOptions,
          props: {
            label: "machineCode",
            value: "machineCode",
          },
          filterable: true,
          loading: this.machineLoading,
          onFocus: this.handleMachineFocus,
        },
        {
          label: this.$t("ui.data.column.mouldCleanWarn.operTime"),
          prop: "operTime",
          type: "date",
          dateType: "daterange",
          valueFormat: "yyyy-MM-dd",
        },
        {
          label: this.$t("ui.data.column.mouldCleanWarn.firstWashTime"),
          prop: "firstWashTime",
          type: "date",
          dateType: "daterange",
          valueFormat: "yyyy-MM-dd",
        },
        {
          label: this.$t("ui.data.column.mouldCleanWarn.secondWashTime"),
          prop: "secondWashTime",
          type: "date",
          dateType: "daterange",
          valueFormat: "yyyy-MM-dd",
        },
      ];
    },
  },
  methods: {
    handleSearch(data) {
      this.query = { ...data };
      if (data.operTime && data.operTime.length === 2) {
        this.query.operTimeBegin = data.operTime[0];
        this.query.operTimeEnd = data.operTime[1];
        delete this.query.operTime;
      }
      if (data.firstWashTime && data.firstWashTime.length === 2) {
        this.query.firstWashTimeBegin = data.firstWashTime[0];
        this.query.firstWashTimeEnd = data.firstWashTime[1];
        delete this.query.firstWashTime;
      }
      if (data.secondWashTime && data.secondWashTime.length === 2) {
        this.query.secondWashTimeBegin = data.secondWashTime[0];
        this.query.secondWashTimeEnd = data.secondWashTime[1];
        delete this.query.secondWashTime;
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
    handleExport() {
      downloadLink("/lh/mouldCleanWarn/export", this.formatParams(false));
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
    async getList() {
      try {
        this.loading = true;
        const params = this.formatParams();
        const res = await listMouldCleanWarn(params);
        this.data = res.rows || [];
        this.page.total = res.total || 0;
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
    },
    async loadMachineList() {
      this.machineLoading = true;
      try {
        const res = await listMachine({
          machineCode: "",
          pageSize: 1000,
        });
        this.machineOptions = res.rows || res.data || res || [];
      } catch (error) {
        console.error(error);
      } finally {
        this.machineLoading = false;
      }
    },
    handleMachineFocus() {
      if (this.machineOptions.length === 0) {
        this.loadMachineList();
      }
    },
  },
  mounted() {
    this.getList();
    this.loadMachineList();
  },
};
</script>

<style lang="scss" scoped>
.mould-clean-warn {
  height: 100%;
  padding: 10px;
  box-sizing: border-box;
}
</style>
