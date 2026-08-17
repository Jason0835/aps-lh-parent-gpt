<template>
  <basic-container>
    <page-table tableRef="cd15MachineMaintenanceMainTable" :calcHeight="true" v-loading="loading" :columns="columns" :searchColumns="searchColumns" :data="data" :page="page" :search="search" @refresh="getList" @search="handleSearch" @pageChange="handlePageChange" @sort-change="handleSortChange" @selection-change="handleSelectionChange" :showSummary="false" :selectArea="false">
      <template slot="header">
        <el-button type="primary" plain v-hasPermi="['cd15:machineMaintenancePlan:add']" @click="handleAdd">{{ $t("ui.frame.btn.add") }}</el-button>
        <el-button v-hasPermi="['cd15:machineMaintenancePlan:edit']" @click="handleBatchEdit" :disabled="selection.length !== 1">{{ $t("ui.frame.btn.update") }}</el-button>
        <el-button type="danger" v-hasPermi="['cd15:machineMaintenancePlan:remove']" :disabled="selection.length === 0" @click="handleBatchDelete">{{ $t("ui.frame.btn.delete") }}</el-button>
        <el-button v-hasPermi="['cd15:machineMaintenancePlan:import']" @click="$refs.tltUpload.handleImport()">{{ $t("ui.frame.btn.import") }}</el-button>
        <el-button @click="handleExport" v-hasPermi="['cd15:machineMaintenancePlan:export']">{{ $t("ui.frame.btn.export") }}</el-button>
      </template>
    </page-table>
    <tlt-upload-form ref="tltUpload" :updateSupport="true" downloadUrl="/cd15/cd15MachineMaintenance/importTemplate" uploadUrl="/cd15/cd15MachineMaintenance/importData" @uploadSuccess="getList" labelWidth="0" :columns="importColumns" />
    <info-dialog ref="infoRef" @success="getList" />
  </basic-container>
</template>

<script>
import { listMachineMaintenancePlan, delMachineMaintenancePlan, exportMachineMaintenancePlan } from "@/api/cd15/machineMaintenancePlan";
import { getCd15MachineEnableOptions } from "@/api/cd15/cd15MachineInfo";
import TltUploadForm from "@/views/components/tltUploadForm.vue";
import infoDialog from "./components/infoDialog.vue";

export default {
  name: "Cd15MachineMaintenance",
  components: { TltUploadForm, infoDialog },
  dicts: ["biz_factory_name"],
  provide() { return { parentDict: this.dict }; },
  data() {
    return {
      importColumns: [{ label: "", prop: "updateSupport", render: (form) => (<el-checkbox label={this.$t("common.rule.updateSupport")} v-model={form.updateSupport}>{this.$t("common.rule.updateSupport")}</el-checkbox>) }],
      loading: false, data: [], selection: [], machineOptions: [],
      page: { current: 1, pageSize: 20, total: 0 }, sort: {},
      search: { factoryCode: "116" }, query: { factoryCode: "116" },
    };
  },
  computed: {
    columns() {
      return [
        { type: "selection", fixed: "left" },
        { prop: "factoryCode", align: "center", halign: "center", label: this.$t("ui.data.column.cd15MachineMaintenancePlan.factoryCode"), minWidth: 120, formatter: (row, column, value) => this.selectDictLabel(this.dict.type.biz_factory_name, value) },
        { prop: "machineCode", align: "center", halign: "center", label: this.$t("ui.data.column.cd15MachineMaintenancePlan.machineCode"), minWidth: 140 },
        { prop: "downtimeStartTime", align: "center", halign: "center", label: this.$t("ui.data.column.cd15MachineMaintenancePlan.downtimeStartTime"), minWidth: 160 },
        { prop: "downtimeEndTime", align: "center", halign: "center", label: this.$t("ui.data.column.cd15MachineMaintenancePlan.downtimeEndTime"), minWidth: 160 },
        { prop: "downtimeHours", align: "center", halign: "center", label: this.$t("ui.data.column.cd15MachineMaintenancePlan.downtimeHours"), minWidth: 120 },
        { prop: "remark", halign: "center", label: this.$t("ui.common.column.remark"), minWidth: 120 },
        { prop: "updateTime", align: "center", halign: "center", label: this.$t("common.updateTime"), minWidth: 160 },
        { align: "center", halign: "center", label: this.$t("ui.data.btn.option"), prop: "option", minWidth: 150, fixed: "right",
          render: ({ row }) => (<div><el-button v-hasPermi={["cd15:machineMaintenancePlan:edit"]} class="minus" type="success" onClick={() => this.handleEdit(row)}>{this.$t("ui.frame.btn.update")}</el-button><el-button v-hasPermi={["cd15:machineMaintenancePlan:remove"]} class="minus" type="danger" onClick={() => this.handleDelete(row)}>{this.$t("ui.frame.btn.delete")}</el-button></div>) },
      ];
    },
    searchColumns() {
      return [
        { label: this.$t("ui.data.column.cd15MachineMaintenancePlan.factoryCode"), prop: "factoryCode", type: "select", dictData: this.dict.type.biz_factory_name, filterable: true, listeners: { change: (factoryCode) => this.loadMachineOptions(factoryCode) } },
        { label: this.$t("ui.data.column.cd15MachineMaintenancePlan.machineCode"), prop: "machineCode", type: "select", dictData: this.machineOptions, filterable: true, clearable: true },
        { label: this.$t("ui.data.column.cd15MachineMaintenancePlan.downtimeStartTime"), prop: "downtimeStartTimeRange", type: "date", dateType: "datetimerange", valueFormat: "yyyy-MM-dd HH:mm:ss" },
        { label: this.$t("ui.data.column.cd15MachineMaintenancePlan.downtimeEndTime"), prop: "downtimeEndTimeRange", type: "date", dateType: "datetimerange", valueFormat: "yyyy-MM-dd HH:mm:ss" },
      ];
    },
  },
  methods: {
    handleAdd() { this.$refs.infoRef && this.$refs.infoRef.show(); },
    handleEdit(row) { this.$refs.infoRef && this.$refs.infoRef.show(row); },
    handleBatchEdit() { if (this.selection.length === 1) this.handleEdit(this.selection[0]); },
    handleDelete(row) { this.$confirm(this.$t("common.confirm.delete"), { type: "warning" }).then(() => { delMachineMaintenancePlan({ ids: row.id }).then((data) => { this.$modal.msgSuccess(data.msg); this.$set(this.page, "current", 1); this.getList(); }); }); },
    handleBatchDelete() { if (!this.selection || this.selection.length === 0) return; this.$confirm(this.$t("common.confirm.delete"), { type: "warning" }).then(() => { const ids = this.selection.map(item => item.id).join(","); delMachineMaintenancePlan({ ids }).then((data) => { this.$modal.msgSuccess(data.msg); this.selection = []; this.$set(this.page, "current", 1); this.getList(); }); }); },
    handleExport() { exportMachineMaintenancePlan(this.query); },
    handleSearch(params) {
      this.page.current = 1;
      const queryParams = { ...params };
      // 日期时间范围控件返回 [起, 止] 数组，拆分为开始/结束两个查询参数提交
      const splitRange = (rangeProp, startProp, endProp) => {
        const range = queryParams[rangeProp];
        if (Array.isArray(range) && range.length === 2) {
          queryParams[startProp] = range[0];
          queryParams[endProp] = range[1];
        }
        delete queryParams[rangeProp];
      };
      splitRange("downtimeStartTimeRange", "downtimeStartTimeStart", "downtimeStartTimeEnd");
      splitRange("downtimeEndTimeRange", "downtimeEndTimeStart", "downtimeEndTimeEnd");
      this.query = queryParams;
      this.loadMachineOptions();
      this.getList();
    },
    handlePageChange(current, pageSize) { this.page.current = current; this.page.pageSize = pageSize; this.getList(); },
    handleSortChange(sort) { this.sort = sort; this.getList(); },
    handleSelectionChange(selection) { this.selection = selection || []; },
    async getList() { this.loading = true; try { const params = { ...this.query, pageNum: this.page.current, pageSize: this.page.pageSize, orderByColumn: this.sort.prop, isAsc: this.sort.order }; const res = await listMachineMaintenancePlan(params); this.data = res.rows || []; this.page.total = res.total || 0; } finally { this.loading = false; } },
    async loadMachineOptions(factoryCode) {
      // 未传工厂时使用当前查询条件中的工厂；工厂为空时不请求，避免展示全厂机台
      const code = factoryCode || this.query.factoryCode;
      if (!code) {
        this.machineOptions = [];
        return;
      }
      const res = await getCd15MachineEnableOptions({ factoryCode: code });
      const rows = Array.isArray(res) ? res : (res.rows || res.data || []);
      this.machineOptions = rows.map((item) => ({ label: item.machineCode, value: item.machineCode }));
    },
  },
  created() { this.getList(); this.loadMachineOptions(); },
};
</script>
