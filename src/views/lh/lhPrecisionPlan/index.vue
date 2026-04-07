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
          type="primary"
          plain
          v-hasPermi="['schedule:lhPrecisionPlan:sync']"
          @click="handleSyncFromMes"
          >{{ $t("ui.lh.precision.plan.sync.from.mes") }}</el-button
        >
        <el-button
          v-hasPermi="['schedule:lhPrecisionPlan:edit']"
          @click="handleBatchEdit"
          :disabled="selection.length !== 1"
          >{{ $t("ui.frame.btn.update") }}</el-button
        >
        <el-button
          type="danger"
          v-hasPermi="['schedule:lhPrecisionPlan:remove']"
          :disabled="selection.length == 0"
          @click="handleDeleteAll"
          >{{ $t("ui.frame.btn.delete") }}</el-button
        >
        <el-button
          v-hasPermi="['schedule:lhPrecisionPlan:import']"
          @click="$refs.tltUpload.handleImport()"
          >{{ $t("ui.frame.btn.import") }}</el-button
        >
        <el-button
          @click="handleExport"
          v-hasPermi="['schedule:lhPrecisionPlan:export']"
          >{{ $t("ui.frame.btn.export") }}</el-button
        >
      </template>
    </page-table>
    <tlt-upload-form
      ref="tltUpload"
      :updateSupport="true"
      downloadUrl="/schedule/lhPrecisionPlan/importTemplate"
      uploadUrl="/schedule/lhPrecisionPlan/importData"
      @uploadSuccess="getList"
      labelWidth="0"
      :columns="importColumns"
    ></tlt-upload-form>
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
import TltUploadForm from "@/views/components/tltUploadForm.vue";
import tltUpload from "@/components/tltUpload/tltUpload.vue";
import infoDialog from "./components/infoDialog.vue";

export default {
  name: "LhPrecisionPlan",
  components: {
    tltUpload,
    infoDialog,
    TltUploadForm
  },
  dicts: ["biz_factory_name", "lh_machine", "lh_precision_type", "lh_completion_status", "lh_warning_status", "lh_data_source"],
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
          prop: "dueDate",
          label: this.$t("ui.lh.precision.plan.due.date"),
        },
        {
          prop: "daysToDue",
          label: this.$t("ui.lh.precision.plan.days.to.due"),
        },
        {
          prop: "lastMaintenanceDate",
          label: this.$t("ui.lh.precision.plan.last.maintenance.date"),
        },
        {
          prop: "completionStatus",
          label: this.$t("ui.lh.precision.plan.completion.status"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.lh_completion_status, value);
          },
        },
        {
          prop: "year",
          label: this.$t("ui.lh.precision.plan.year"),
        },
        {
          prop: "warningStatus",
          label: this.$t("ui.lh.precision.plan.warning.status"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.lh_warning_status, value);
          },
        },
        {
          prop: "warningDate",
          label: this.$t("ui.lh.precision.plan.warning.date"),
        },
        {
          prop: "dataSource",
          label: this.$t("ui.lh.precision.plan.data.source"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.lh_data_source, value);
          },
        },
        {
          prop: "updateTime",
          width: 180,
          label: this.$t("ui.data.column.scheduleAdjust.updata"),
        },
        {
          align: "center",
          label: this.$t("ui.data.btn.option"),
          width: 180,
          render: ({ row }) => {
            return (
              <div>
                <el-button
                  v-hasPermi={["lh:precisionPlan:edit"]}
                  class="minus"
                  type="success"
                  onClick={() => this.handleEdit(row)}
                >
                  {this.$t("ui.frame.btn.update")}
                </el-button>
                <el-button
                  v-hasPermi={["lh:precisionPlan:remove"]}
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
          label: this.$t("common.factory"),
          prop: "factoryCode",
          type: "select",
          dictData: this.dict.type.biz_factory_name,
        },
        {
          label: this.$t("ui.lh.precision.plan.machine.code"),
          prop: "machineCode",
          type: "select",
          dictData: this.dict.type.lh_machine,
          filterable: true,
        },
        {
          label: this.$t("ui.lh.precision.plan.precision.type"),
          prop: "precisionType",
          type: "select",
          dictData: this.dict.type.lh_precision_type,
        },
        {
          label: this.$t("ui.lh.precision.plan.plan.date"),
          prop: "planDate",
          type: "date",
          dateType: "date",
          valueFormat: "yyyy-MM-dd",
        },
        {
          label: this.$t("ui.lh.precision.plan.actual.date"),
          prop: "actualDate",
          type: "date",
          dateType: "date",
          valueFormat: "yyyy-MM-dd",
        },
      ];
    },
  },
  methods: {
    handleSyncFromMes() {
      this.$confirm(this.$t("ui.lh.precision.plan.sync.confirm"), {
        type: "warning",
      }).then(() => {
        this.loading = true;
        syncFromMes().then((res) => {
          this.$modal.msgSuccess(res.msg || this.$t("common.success"));
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
      let ids = this.selection.map(item => item.id).join(",");
      this.$confirm(this.$t("common.confirm.delete"), {
        type: "warning",
      }).then(() => {
        removeLhPrecisionPlan([ids]).then((data) => {
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
        removeLhPrecisionPlan([row.id]).then((data) => {
          this.$modal.msgSuccess(data.msg || this.$t("common.success"));
          this.$set(this.page, "current", 1);
          this.getList();
        });
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
      downloadLink("/lh/precisionPlan/export", this.formatParams(false));
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
    const today = new Date();
    const defaultDate = today.toISOString().split('T')[0];
    let defaultParams = {
      factoryCode: "116",
      planDate: defaultDate,
    };
    this.search = {
      ...defaultParams,
    };
    this.query = {
      ...defaultParams,
    };
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
