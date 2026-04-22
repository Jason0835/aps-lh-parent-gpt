<template>
  <basic-container>
    <page-table
      tableRef="lhPrecisionPlanMainTable"
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
          v-hasPermi="['lh:lhPrecisionPlan:edit']"
          @click="handleBatchEdit"
          :disabled="selection.length !== 1"
          >{{ $t("ui.frame.btn.update") }}</el-button
        >
        <el-button
          type="primary"
          plain
          v-hasPermi="['lh:lhPrecisionPlan:sync']"
          @click="handleSyncFromMes"
          >{{ $t("ui.lh.precision.plan.sync.from.mes") }}</el-button
        >
        <el-button
          type="danger"
          v-hasPermi="['lh:lhPrecisionPlan:remove']"
          :disabled="selection.length == 0"
          @click="handleDeleteAll"
          >{{ $t("ui.frame.btn.delete") }}</el-button
        >
        <el-button
          v-hasPermi="['lh:lhPrecisionPlan:import']"
          @click="$refs.tltUpload.handleImport()"
          >{{ $t("ui.frame.btn.import") }}</el-button
        >
        <el-button
          @click="handleExport"
          v-hasPermi="['lh:lhPrecisionPlan:export']"
          >{{ $t("ui.frame.btn.export") }}</el-button
        >
      </template>
    </page-table>
    <tlt-upload-form
      ref="tltUpload"
      :updateSupport="true"
      :downloadUrl="importTemplateUrl"
      :uploadUrl="importUrl"
      @uploadSuccess="getList"
      labelWidth="0"
      :columns="importColumns"
    />
    <infoDialog ref="infoRef" @success="getList" />
  </basic-container>
</template>

<script>
import { downloadLink } from "@/utils/request";
import {
  listLhPrecisionPlan,
  removeLhPrecisionPlan,
  syncFromMes,
  autoGeneratePlans,
  checkWarning
} from "@/api/lh/lhPrecisionPlan";
import { listMachine } from "@/api/lh/machine";
import TltUploadForm from "@/views/components/tltUploadForm.vue";
import infoDialog from "./components/infoDialog.vue";

export default {
  name: "LhPrecisionPlan",
  components: {
    infoDialog,
    TltUploadForm
  },
  dicts: ["biz_factory_name", "lh_machine", "lh_precision_type", "lh_completion_status", "lh_warning_status", "lh_precision_data_source"],
  provide() {
    return {
      parentDict: this.dict,
    };
  },
  data() {
    return {
      importColumns: [
        {
          label: "",
          prop: "updateSupport",
          render: (form) => {
            return (
              <el-checkbox
                label={this.$t("common.rule.updateSupport")}
                v-model={form.updateSupport}
              >
                {this.$t("common.rule.updateSupport")}
              </el-checkbox>
            );
          },
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
      yearList: [],
      machineList: [],
      importUrl: '/lh/lhPrecisionPlan/importData',
      importTemplateUrl: '/lh/lhPrecisionPlan/importTemplate'
    };
  },
  mounted() {
    this.getMachineList();
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
          prop: "year",
          label: this.$t("ui.lh.precision.plan.year"),
        },
        {
          prop: "machineCode",
          label: this.$t("ui.lh.precision.plan.machine.code"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.lh_machine, value);
          },
        },
        {
          prop: "precisionType",
          label: this.$t("ui.lh.precision.plan.precision.type"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.lh_precision_type, value);
          },
        },
        {
          prop: "planDate",
          label: this.$t("ui.lh.precision.plan.plan.date"),
        },
        {
          prop: "actualDate",
          label: this.$t("ui.lh.precision.plan.actual.date"),
        },
        {
          prop: "daysToDue",
          label: this.$t("ui.lh.precision.plan.days.to.due"),
          formatter: (row, column, value) => {
            return value < 0 ? 0 : value;
          },
        },
        {
          prop: "dataSource",
          label: this.$t("ui.lh.precision.plan.data.source"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.lh_precision_data_source, value);
          },
        },
        {
          prop: "updateTime",
          width: 180,
          label: this.$t("ui.lh.precision.plan.updateTime"),
        },
        {
          prop: "remark",
          label: this.$t("ui.data.column.remark"),
          showOverflowTooltip: true,
          formatter: (row, column, value) => {
            if (!value) return value;
            return value
              .replace(/__PERCENT__/g, '%')
              .replace(/__AMP__/g, '&')
              .replace(/__LT__/g, '<')
              .replace(/__GT__/g, '>')
              .replace(/__QUOT__/g, '"')
              .replace(/__APOS__/g, "'");
          }
        },
        {
          align: "center",
          label: this.$t("ui.data.btn.option"),
          width: 180,
          render: ({ row }) => {
            return (
              <div>
                <el-button
                  v-hasPermi={["lh:lhPrecisionPlan:edit"]}
                  class="minus"
                  type="success"
                  onClick={() => this.handleEdit(row)}
                >
                  {this.$t("ui.frame.btn.update")}
                </el-button>
                <el-button
                  v-hasPermi={["lh:lhPrecisionPlan:remove"]}
                  class="minus"
                  type="danger"
                  onClick={() => this.handleDelete(row)}
                >
                  {this.$t("ui.frame.btn.delete")}
                </el-button>
              </div>
            );
          },
        },
      ];

      return columns;
    },
    searchColumns() {
      return [
        {
          label: this.$t("ui.lh.precision.plan.year"),
          prop: "year",
          type: "select",
          dictData: this.yearList,
          value: new Date().getFullYear().toString(),
        },
        {
          label: this.$t("common.factory"),
          prop: "factoryCode",
          type: "select",
          dictData: this.dict.type.biz_factory_name,
        },
        {
          label: this.$t("ui.lh.precision.plan.machine.code"),
          prop: "machineCode",
          type: "select",
          dictData: this.machineList,
          filterable: true,
        },
        {
          label: this.$t("ui.lh.precision.plan.plan.date"),
          prop: "planDate",
          type: "date",
          dateType: "daterange",
          valueFormat: "yyyy-MM-dd",
          startPlaceholder: this.$t("common.startTime"),
          endPlaceholder: this.$t("common.endTime"),
        },
        {
          label: this.$t("ui.lh.precision.plan.actual.date"),
          prop: "actualDate",
          type: "date",
          dateType: "daterange",
          valueFormat: "yyyy-MM-dd",
          startPlaceholder: this.$t("common.startTime"),
          endPlaceholder: this.$t("common.endTime"),
        },
      ];
    },
  },
  methods: {
    getDaysToDueValue(planDate) {
      if (!planDate) {
        return ''
      }
      const target = new Date(planDate)
      if (Number.isNaN(target.getTime())) {
        return ''
      }
      const now = new Date()
      const startOfToday = new Date(now.getFullYear(), now.getMonth(), now.getDate())
      const startOfTarget = new Date(target.getFullYear(), target.getMonth(), target.getDate())
      return Math.floor((startOfToday.getTime() - startOfTarget.getTime()) / 86400000)
    },
    initYearList() {
      const currentYear = new Date().getFullYear();
      const years = [];
      for (let i = currentYear - 2; i <= currentYear + 2; i++) {
        years.push({
          label: i.toString(),
          value: i.toString(),
        });
      }
      this.yearList = years;
    },
    async getMachineList() {
      try {
        const res = await listMachine({});
        const list = res.rows || [];
        const map = new Map();
        list.forEach((item) => {
          if (item && item.machineCode) {
            // 转换为 dictData 格式：包含 label 和 value
            map.set(item.machineCode, {
              label: item.machineCode,
              value: item.machineCode
            });
          }
        });
        this.machineList = Array.from(map.values());
      } catch (e) {
        this.machineList = [];
        console.error(e);
      }
    },
    handleSyncFromMes() {
      const year = this.query.year || new Date().getFullYear().toString();
      this.$confirm(this.$t("ui.lh.precision.plan.sync.confirm", [year]), {
        type: "warning",
      }).then(() => {
        this.loading = true;
        syncFromMes(year).then((res) => {
          this.$modal.msgSuccess(res.msg || this.$t("common.success"));
          this.$set(this.page, "current", 1);
          this.getList();
        }).catch(() => {
          this.loading = false;
        });
      });
    },
    handleAutoGenerate() {
      const currentYear = new Date().getFullYear();
      this.$confirm(this.$t("ui.lh.precision.plan.auto.generate.confirm", { year: currentYear }), {
        type: "warning",
      }).then(() => {
        this.loading = true;
        autoGeneratePlans(currentYear).then((res) => {
          this.$modal.msgSuccess(res.msg || this.$t("common.success"));
          this.getList();
        }).catch(() => {
          this.loading = false;
        });
      });
    },
    handleCheckWarning() {
      this.loading = true;
      checkWarning().then((res) => {
        this.$modal.msgSuccess(res.msg || this.$t("common.success"));
        this.getList();
      }).catch(() => {
        this.loading = false;
      });
    },
    handleEdit(row) {
      if (this.$refs.infoRef) {
        this.$refs.infoRef.show(row);
      }
    },
    handleBatchEdit() {
      if (this.selection && this.selection.length === 1) {
        this.handleEdit(this.selection[0]);
      }
    },
    handleDeleteAll() {
      const ids = this.selection.map(item => item.id).join(',')
      this.$confirm(this.$t("common.confirm.delete"), {
        type: "warning",
      }).then(() => {
        removeLhPrecisionPlan(ids).then((data) => {
          this.$modal.msgSuccess(data.msg || this.$t("common.success"));
          this.$set(this.page, "current", 1);
          this.getList();
        });
      });
    },
    handleDelete(row) {
      this.$confirm(this.$t("common.confirm.delete"), {
        type: "warning",
      }).then(() => {
        removeLhPrecisionPlan(row.id).then((data) => {
          this.$modal.msgSuccess(data.msg || this.$t("common.success"));
          this.$set(this.page, "current", 1);
          this.getList();
        });
      });
    },
    handleSearch(data) {
      this.query = { ...data };
      if (data.planDate && data.planDate.length === 2) {
        this.query.planDateStart = data.planDate[0];
        this.query.planDateEnd = data.planDate[1];
      } else {
        this.query.planDateStart = undefined;
        this.query.planDateEnd = undefined;
      }
      if (data.actualDate && data.actualDate.length === 2) {
        this.query.actualDateStart = data.actualDate[0];
        this.query.actualDateEnd = data.actualDate[1];
      } else {
        this.query.actualDateStart = undefined;
        this.query.actualDateEnd = undefined;
      }
      delete this.query.planDate;
      delete this.query.actualDate;
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
      downloadLink('/lh/lhPrecisionPlan/export', this.formatParams(false));
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
        const data = await listLhPrecisionPlan(this.formatParams());
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
    this.initYearList();
    const defaultParams = {
      factoryCode: '116'
    }
    this.search = { ...defaultParams }
    this.query = { ...defaultParams }
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
