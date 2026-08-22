<template>
  <basic-container>
    <page-table
      tableRef="gsqSpecifyMachineMainTable"
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
          v-hasPermi="['gsq:specifyMachine:add']"
          @click="handleAdd"
        >{{ $t("ui.frame.btn.add") }}</el-button>
        <el-button
          type="danger"
          plain
          v-hasPermi="['gsq:specifyMachine:remove']"
          @click="handleBatchDelete"
        >{{ $t("ui.frame.btn.delete") }}</el-button>
        <el-button
          v-hasPermi="['gsq:specifyMachine:import']"
          @click="$refs.tltUpload.handleImport()"
        >{{ $t("ui.frame.btn.import") }}</el-button>
        <el-button
          @click="handleExport"
          v-hasPermi="['gsq:specifyMachine:export']"
        >{{ $t("ui.frame.btn.export") }}</el-button>
      </template>
    </page-table>
    <tlt-upload
      ref="tltUpload"
      downloadUrl="/gsq/specifyMachine/importTemplate"
      uploadUrl="/gsq/specifyMachine/importData"
      @uploadSuccess="getList"
    />
    <InfoDialog ref="infoRef" @success="getList" />
  </basic-container>
</template>
<script>
import {
  listSpecifyMachine,
  removeSpecifyMachine,
  exportSpecifyMachine,
} from "@/api/gsq/specifyMachine";
import { listEnabledMachines } from "@/api/gsq/machine";
import tltUpload from "@/components/tltUpload/tltUpload.vue";
import InfoDialog from "./components/infoDialog.vue";

export default {
  name: "GsqSpecifyMachine",
  dicts: ["LINE_TYPE", "JOB_TYPE"],
  components: {
    tltUpload,
    InfoDialog,
  },
  provide() {
    return {
      parentDict: this.dict,
    };
  },
  data() {
    return {
      loading: false,
      machineLoading: false,
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
    searchColumns() {
      return [
        {
          label: this.$t("ui.data.column.gsq.specifyMachine.steelRingCode"),
          prop: "steelRingCode",
          type: "input",
        },
        {
          label: this.$t("ui.data.column.gsq.specifyMachine.machineName"),
          prop: "machineCode",
          type: "select",
          dictData: this.machineList,
          labelKey: "machineName",
          valueKey: "machineCode",
          filterable: true,
          loading: this.machineLoading,
          onFocus: this.handleMachineFocus,
        },
        {
          label: this.$t("ui.data.column.gsq.specifyMachine.lineType"),
          prop: "lineType",
          type: "select",
          dictData: this.dict.type.LINE_TYPE,
          filterable: true,
        },
        {
          label: this.$t("ui.data.column.gsq.specifyMachine.jobType"),
          prop: "jobType",
          type: "select",
          dictData: this.dict.type.JOB_TYPE,
          filterable: true,
        },
      ];
    },
    columns() {
      return [
        { type: "selection", fixed: "left" },
        {
          prop: "steelRingCode",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.gsq.specifyMachine.steelRingCode"),
          minWidth: 150,
          formatter: (row) => {
            return row.steelRingCode || "-";
          },
        },
        {
          prop: "machineName",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.gsq.specifyMachine.machineName"),
          minWidth: 120,
          formatter: (row) => {
            return row.machineName || "-";
          },
        },
        {
          prop: "lineType",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.gsq.specifyMachine.lineType"),
          minWidth: 120,
          formatter: (row) => {
            return this.selectDictLabel(this.dict.type.LINE_TYPE, row.lineType) || "-";
          },
        },
        {
          prop: "jobType",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.gsq.specifyMachine.jobType"),
          minWidth: 120,
          formatter: (row) => {
            return this.selectDictLabel(this.dict.type.JOB_TYPE, row.jobType) || "-";
          },
        },
        {
          prop: "remark",
          halign: "center",
          label: this.$t("ui.common.column.remark"),
          minWidth: 100,
          formatter: (row) => {
            return row.remark || "-";
          },
        },
        {
          prop: "updateTime",
          halign: "center",
          label: this.$t("ui.data.column.gsq.specifyMachine.updateDate"),
          minWidth: 150,
        },
        {
          align: "center",
          halign: "center",
          label: this.$t("ui.data.btn.option"),
          prop: "option",
          width: 180,
          fixed: "right",
          render: ({ row }) => {
            return (
              <div>
                <el-button
                  v-hasPermi={["gsq:specifyMachine:edit"]}
                  class="minus"
                  type="primary"
                  onClick={() => this.handleEdit(row)}
                >
                  {this.$t("ui.frame.btn.modify")}
                </el-button>
                <el-button
                  v-hasPermi={["gsq:specifyMachine:remove"]}
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
  methods: {
    handleAdd() {
      if (this.$refs.infoRef) {
        this.$refs.infoRef.show();
      }
    },
    handleEdit(row) {
      if (this.$refs.infoRef) {
        this.$refs.infoRef.show(row);
      }
    },
    handleDelete(row) {
      this.$confirm(this.$t("common.confirm.delete"), {
        type: "warning",
      }).then(() => {
        const ids = row.id;
        removeSpecifyMachine(ids).then((data) => {
          this.$modal.msgSuccess(data.msg);
          this.$set(this.page, "current", 1);
          this.getList();
        });
      });
    },
    handleBatchDelete() {
      if (this.selection.length === 0) {
        this.$modal.msgWarning(this.$t("ui.placeholder.selectTableRow"));
        return;
      }
      this.$confirm(this.$t("common.confirm.delete"), {
        type: "warning",
      }).then(() => {
        const ids = this.selection.map((row) => row.id).join(",");
        removeSpecifyMachine(ids).then((data) => {
          this.$modal.msgSuccess(data.msg);
          this.$set(this.page, "current", 1);
          this.getList();
        });
      });
    },
    handleExport() {
      this.$confirm(this.$t("ui.data.column.gsq.specifyMachine.confirm.export"), {
        type: "warning",
      }).then(() => {
        try {
          this.loading = true;
          let params = this.formatParams(false);
          params = {
            ...params,
            pageSize: undefined,
            pageNum: undefined,
          };
          exportSpecifyMachine(params);
        } catch (error) {
          console.error(error);
        } finally {
          this.loading = false;
        }
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
        const data = await listSpecifyMachine(this.formatParams());
        this.data = data.rows;
        this.page.total = data.total;
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
    },
    async loadMachineList() {
      this.machineLoading = true;
      try {
        const res = await listEnabledMachines();
        this.machineList = Array.isArray(res) ? res : (res.data || res.rows || []);
      } catch (error) {
        console.log(error);
      } finally {
        this.machineLoading = false;
      }
    },
    handleMachineFocus() {
      if (this.machineList.length === 0) {
        this.loadMachineList();
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
