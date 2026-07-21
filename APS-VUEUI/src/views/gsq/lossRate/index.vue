<template>
  <basic-container>
    <page-table
      tableRef="gsqLossRateMainTable"
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
          v-hasPermi="['gsq:lossRate:add']"
          @click="handleAdd"
        >{{ $t("ui.frame.btn.add") }}</el-button>
        <el-button
          type="danger"
          plain
          v-hasPermi="['gsq:lossRate:remove']"
          @click="handleBatchDelete"
        >{{ $t("ui.frame.btn.delete") }}</el-button>
        <el-button
          v-hasPermi="['gsq:lossRate:import']"
          @click="$refs.tltUpload.handleImport()"
        >{{ $t("ui.frame.btn.import") }}</el-button>
        <el-button
          @click="handleExport"
          v-hasPermi="['gsq:lossRate:export']"
        >{{ $t("ui.frame.btn.export") }}</el-button>
      </template>
    </page-table>
    <tlt-upload
      ref="tltUpload"
      downloadUrl="/gsq/lossRate/importTemplate"
      uploadUrl="/gsq/lossRate/importData"
      @uploadSuccess="getList"
    />
    <InfoDialog ref="infoRef" @success="getList" />
  </basic-container>
</template>
<script>
import {
  listLossRate,
  removeLossRate,
  exportLossRate,
} from "@/api/gsq/lossRate";
import { listEnabledMachines } from "@/api/gsq/machine";
import tltUpload from "@/components/tltUpload/tltUpload.vue";
import InfoDialog from "./components/infoDialog.vue";

export default {
  name: "GsqLossRate",
  dicts: [],
  components: {
    tltUpload,
    InfoDialog,
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
          label: this.$t("ui.data.column.gsq.lossRate.steelRingCode"),
          prop: "steelRingCode",
          type: "input",
        },
        {
          label: this.$t("ui.data.column.gsq.lossRate.machineName"),
          prop: "machineCode",
          type: "select",
          dictData: this.machineList,
          labelKey: "machineName",
          valueKey: "machineCode",
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
          label: this.$t("ui.data.column.gsq.lossRate.steelRingCode"),
          minWidth: 150,
          formatter: (row) => {
            return row.steelRingCode || "-";
          },
        },
        {
          prop: "machineName",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.gsq.lossRate.machineName"),
          minWidth: 150,
          formatter: (row) => {
            return row.machineName || "-";
          },
        },
        {
          prop: "lossRate",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.gsq.lossRate.lossRate"),
          minWidth: 120,
          formatter: (row) => {
            return row.lossRate != null ? row.lossRate : "-";
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
          label: this.$t("ui.data.column.gsq.lossRate.updateDate"),
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
                  v-hasPermi={["gsq:lossRate:edit"]}
                  class="minus"
                  type="primary"
                  onClick={() => this.handleEdit(row)}
                >
                  {this.$t("ui.frame.btn.modify")}
                </el-button>
                <el-button
                  v-hasPermi={["gsq:lossRate:remove"]}
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
        removeLossRate(row.id).then((data) => {
          this.$modal.msgSuccess(data.msg);
          this.$set(this.page, "current", 1);
          this.getList();
        });
      });
    },
    handleBatchDelete() {
      if (this.selection.length === 0) {
        this.$modal.msgWarning(
          this.$t("common.confirm.selectDeleteData") || "请选择需要删除的数据"
        );
        return;
      }
      this.$confirm(this.$t("common.confirm.delete"), {
        type: "warning",
      }).then(() => {
        const ids = this.selection.map((row) => row.id);
        removeLossRate(ids).then((data) => {
          this.$modal.msgSuccess(data.msg);
          this.$set(this.page, "current", 1);
          this.getList();
        });
      });
    },
    handleExport() {
      this.$confirm(this.$t("ui.data.column.gsq.lossRate.confirm.export"), {
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
          exportLossRate(params);
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
        const data = await listLossRate(this.formatParams());
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
  mounted() {
    this.getList();
    this.loadMachineList();
  },
  activated() {
    this.getList();
  },
};
</script>
