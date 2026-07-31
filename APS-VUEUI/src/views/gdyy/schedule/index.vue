<template>
  <basic-container>
    <page-table
      tableRef="gdyyScheduleResultMainTable"
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
          type="warning"
          v-hasPermi="['gdyy:scheduleResult:edit']"
          @click="handleAdd"
        >{{ $t("ui.data.column.scheduleResult.insertOrder") }}</el-button>
        <el-button
          type="danger"
          v-hasPermi="['gdyy:scheduleResult:remove']"
          :disabled="selection.length === 0"
          @click="handleBatchDelete"
        >{{ $t("ui.frame.btn.delete") }}</el-button>
        <el-button
          type="primary"
          v-hasPermi="['gdyy:scheduleResult:changeMachine']"
          :disabled="selection.length !== 1"
          @click="handleChangeMachine"
        >{{ $t("ui.data.column.scheduleResult.changeMachine") }}</el-button>
        <el-button
          type="primary"
          v-hasPermi="['gdyy:scheduleResult:changePlan']"
          :disabled="selection.length !== 1"
          @click="handleChangePlan"
        >{{ $t("ui.data.column.scheduleResult.changePlan") }}</el-button>
        <el-button
          type="primary"
          v-hasPermi="['gdyy:scheduleResult:publish']"
          :disabled="selection.length === 0"
          @click="handlePublish"
        >{{ $t("ui.data.column.scheduleResult.publish") }}</el-button>
        <el-button
          v-hasPermi="['gdyy:scheduleResult:import']"
          @click="$refs.tltUpload.handleImport()"
        >{{ $t("ui.frame.btn.import") }}</el-button>
        <el-button
          v-hasPermi="['gdyy:scheduleResult:export']"
          @click="handleExport"
        >{{ $t("ui.frame.btn.export") }}</el-button>
      </template>
    </page-table>
    <tlt-upload
      ref="tltUpload"
      :update-support="true"
      upload-url="/gdyy/scheduleResult/importDataByCust"
      :upload-params="importParams"
      @uploadSuccess="getList"
    />
    <edit-dialog ref="editRef" @success="getList" />
    <change-machine-dialog ref="changeMachineRef" @success="getList" />
    <change-plan-dialog ref="changePlanRef" @success="getList" />
    <publish-dialog ref="publishRef" @success="getList" />
  </basic-container>
</template>

<script>
import { listGdyyScheduleResult, delGdyyScheduleResult, exportGdyyScheduleResult, publishGdyyScheduleResult } from "@/api/gdyy/gdyyScheduleResult";
import EditDialog from "./components/editDialog.vue";
import ChangeMachineDialog from "./components/changeMachineDialog.vue";
import ChangePlanDialog from "./components/changePlanDialog.vue";
import PublishDialog from "./components/publishDialog.vue";

export default {
  name: "GdyyScheduleResult",
  components: {
    EditDialog,
    ChangeMachineDialog,
    ChangePlanDialog,
    PublishDialog,
  },
  dicts: ["biz_factory_name", "IS_RELEASE"],
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
        size: 20,
        total: 0,
      },
      search: {
        factoryCode: "116",
        scheduleDate: "",
        bigRollCode: "",
        machineCode: "",
        isRelease: "",
      },
    };
  },
  computed: {
    importParams() {
      return {
        factoryCode: this.search.factoryCode,
      };
    },
    searchColumns() {
      return [
        {
          label: this.$t("ui.data.column.gdyyScheduleResult.factoryCode"),
          prop: "factoryCode",
          type: "select",
          dictData: this.dict.type.biz_factory_name,
          filterable: true,
        },
        {
          label: this.$t("ui.data.column.gdyyScheduleResult.scheduleDate"),
          prop: "scheduleDate",
          type: "date",
          valueFormat: "yyyy-MM-dd",
        },
        {
          label: this.$t("ui.data.column.gdyyScheduleResult.bigRollCode"),
          prop: "bigRollCode",
          type: "input",
        },
        {
          label: this.$t("ui.data.column.gdyyScheduleResult.machineCode"),
          prop: "machineCode",
          type: "input",
        },
        {
          label: this.$t("ui.data.column.gdyyScheduleResult.isRelease"),
          prop: "isRelease",
          type: "select",
          dictData: this.dict.type.IS_RELEASE,
        },
      ];
    },
    columns() {
      return [
        { type: "selection", fixed: "left" },
        {
          label: this.$t("ui.data.column.gdyyScheduleResult.factoryCode"),
          prop: "factoryCode",
          dictType: "biz_factory_name",
          minWidth: 100,
        },
        {
          label: this.$t("ui.data.column.gdyyScheduleResult.scheduleDate"),
          prop: "scheduleDate",
          minWidth: 110,
        },
        {
          label: this.$t("ui.data.column.gdyyScheduleResult.bigRollCode"),
          prop: "bigRollCode",
          minWidth: 140,
        },
        {
          label: this.$t("ui.data.column.gdyyScheduleResult.machineCode"),
          prop: "machineCode",
          minWidth: 120,
        },
        {
          label: this.$t("ui.data.column.gdyyScheduleResult.batchNo"),
          prop: "batchNo",
          minWidth: 140,
        },
        {
          label: this.$t("ui.data.column.gdyyScheduleResult.orderNo"),
          prop: "orderNo",
          minWidth: 140,
        },
        {
          label: this.$t("ui.data.column.gdyyScheduleResult.dayUsed"),
          prop: "dayUsed",
          minWidth: 100,
        },
        {
          label: this.$t("ui.data.column.gdyyScheduleResult.stockQty"),
          prop: "stockQty",
          minWidth: 100,
        },
        {
          label: this.$t("ui.data.column.gdyyScheduleResult.isRelease"),
          prop: "isRelease",
          dictType: "IS_RELEASE",
          minWidth: 100,
        },
        {
          label: this.$t("ui.data.column.gdyyScheduleResult.productionStatus"),
          prop: "productionStatus",
          minWidth: 100,
        },
        {
          label: this.$t("ui.data.column.gdyyScheduleResult.remark"),
          prop: "remark",
          minWidth: 120,
        },
        {
          label: this.$t("ui.frame.table.action"),
          fixed: "right",
          minWidth: 200,
          render: (row) => {
            return (
              <div>
                <el-button
                  v-hasPermi={["gdyy:scheduleResult:edit"]}
                  class="minus"
                  type="success"
                  onClick={() => this.handleEdit(row)}
                >
                  {this.$t("ui.frame.btn.update")}
                </el-button>
                <el-button
                  v-hasPermi={["gdyy:scheduleResult:remove"]}
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
    },
  },
  created() {
    this.getList();
  },
  methods: {
    getList() {
      this.loading = true;
      const params = {
        pageNum: this.page.current,
        pageSize: this.page.size,
        ...this.search,
      };
      listGdyyScheduleResult(params).then((res) => {
        this.data = res.rows;
        this.page.total = res.total;
        this.loading = false;
      });
    },
    handleSearch() {
      this.page.current = 1;
      this.getList();
    },
    handlePageChange(current, pageSize) {
      this.page.current = current;
      this.page.pageSize = pageSize;
      this.getList();
    },
    handleSortChange({ prop, order }) {
      this.search.orderBy = prop;
      this.search.order = order === "ascending" ? "asc" : "desc";
      this.getList();
    },
    handleSelectionChange(selection) {
      this.selection = selection;
    },
    handleAdd() {
      this.$refs.editRef.openDialog("add");
    },
    handleEdit(row) {
      this.$refs.editRef.openDialog("edit", row);
    },
    handleBatchDelete() {
      if (this.selection.length === 0) {
        this.$message.warning(this.$t("ui.frame.msg.selectAtLeastOne"));
        return;
      }
      const ids = this.selection.map((item) => item.id);
      this.$confirm(this.$t("ui.frame.confirm.delete")).then(() => {
        delGdyyScheduleResult(ids).then((res) => {
          this.$message.success(this.$t("ui.frame.msg.success"));
          this.getList();
        });
      });
    },
    handleDelete(row) {
      this.$confirm(this.$t("ui.frame.confirm.delete")).then(() => {
        delGdyyScheduleResult([row.id]).then((res) => {
          this.$message.success(this.$t("ui.frame.msg.success"));
          this.getList();
        });
      });
    },
    handleChangeMachine() {
      if (this.selection.length !== 1) {
        this.$message.warning(this.$t("ui.frame.msg.selectOne"));
        return;
      }
      this.$refs.changeMachineRef.openDialog(this.selection[0]);
    },
    handleChangePlan() {
      if (this.selection.length !== 1) {
        this.$message.warning(this.$t("ui.frame.msg.selectOne"));
        return;
      }
      this.$refs.changePlanRef.openDialog(this.selection[0]);
    },
    handlePublish() {
      if (this.selection.length === 0) {
        this.$message.warning(this.$t("ui.frame.msg.selectAtLeastOne"));
        return;
      }
      this.$refs.publishRef.openDialog(this.selection);
    },
    handleExport() {
      exportGdyyScheduleResult(this.search);
    },
  },
};
</script>
