<template>
  <basic-container>
    <page-table tableRef="machineRollMappingMainTable" :calcHeight="true" v-loading="loading" :columns="columns" :searchColumns="searchColumns" :data="data" :page="page" :search="search" @refresh="getList" @search="handleSearch" @pageChange="handlePageChange" @sort-change="handleSortChange" @selection-change="handleSelectionChange" :showSummary="false" :selectArea="false">
      <template slot="header">
        <el-button type="primary" plain v-hasPermi="['cd90:machineRollMapping:add']" @click="handleAdd">{{ $t("ui.frame.btn.add") }}</el-button>
        <el-button v-hasPermi="['cd90:machineRollMapping:edit']" @click="handleBatchEdit" :disabled="selection.length !== 1">{{ $t("ui.frame.btn.update") }}</el-button>
        <el-button type="danger" v-hasPermi="['cd90:machineRollMapping:remove']" :disabled="selection.length === 0" @click="handleBatchDelete">{{ $t("ui.frame.btn.delete") }}</el-button>
        <el-button v-hasPermi="['cd90:machineRollMapping:import']" @click="$refs.tltUpload.handleImport()">{{ $t("ui.frame.btn.import") }}</el-button>
        <el-button @click="handleExport" v-hasPermi="['cd90:machineRollMapping:export']">{{ $t("ui.frame.btn.export") }}</el-button>
      </template>
    </page-table>
    <tlt-upload-form ref="tltUpload" :updateSupport="true" downloadUrl="/cd90/cd90MachineRollMapping/importTemplate" uploadUrl="/cd90/cd90MachineRollMapping/importData" @uploadSuccess="getList" labelWidth="0" :columns="importColumns" />
    <info-dialog ref="infoRef" :cloth-options="clothOptions" @success="getList" />
  </basic-container>
</template>

<script>
import { listMachineRollMapping, delMachineRollMapping, exportMachineRollMapping } from "@/api/cd90/machineRollMapping";
import { listTireFabricCodes } from "@/api/cd90/specifyMachine";
import { getCd90MachineEnableOptions } from "@/api/cd90/cd90MachineInfo";
import TltUploadForm from "@/views/components/tltUploadForm.vue";
import infoDialog from "./components/infoDialog.vue";

export default {
  name: "MachineRollMapping",
  components: { TltUploadForm, infoDialog },
  dicts: ["biz_factory_name"],
  provide() { return { parentDict: this.dict }; },
  data() {
    return {
      importColumns: [{ label: "", prop: "updateSupport", render: (form) => (<el-checkbox label={this.$t("common.rule.updateSupport")} v-model={form.updateSupport}>{this.$t("common.rule.updateSupport")}</el-checkbox>) }],
      loading: false, data: [], selection: [], clothOptions: [], machineOptions: [],
      page: { current: 1, pageSize: 20, total: 0 }, sort: {},
      search: { factoryCode: "116" }, query: { factoryCode: "116" },
    };
  },
  computed: {
    columns() {
      return [
        { type: "selection", fixed: "left" },
        { prop: "factoryCode", align: "center", halign: "center", label: this.$t("ui.data.column.cd90MachineRollMapping.factoryCode"), minWidth: 120, formatter: (row, column, value) => this.selectDictLabel(this.dict.type.biz_factory_name, value) },
        { prop: "bigRollCode", align: "center", halign: "center", label: this.$t("ui.data.column.cd90MachineRollMapping.bigRollCode"), minWidth: 140 },
        { prop: "cordFabricCode", align: "center", halign: "center", label: this.$t("ui.data.column.cd90MachineRollMapping.cordFabricCode"), minWidth: 140 },
        { prop: "machineCode", align: "center", halign: "center", label: this.$t("ui.data.column.cd90MachineRollMapping.machineCode"), minWidth: 140 },
        { prop: "remark", halign: "center", label: this.$t("ui.common.column.remark"), minWidth: 120 },
        { prop: "updateTime", align: "center", halign: "center", label: this.$t("common.updateTime"), minWidth: 160 },
        { align: "center", halign: "center", label: this.$t("ui.data.btn.option"), prop: "option", minWidth: 150, fixed: "right",
          render: ({ row }) => (<div><el-button v-hasPermi={["cd90:machineRollMapping:edit"]} class="minus" type="success" onClick={() => this.handleEdit(row)}>{this.$t("ui.frame.btn.update")}</el-button><el-button v-hasPermi={["cd90:machineRollMapping:remove"]} class="minus" type="danger" onClick={() => this.handleDelete(row)}>{this.$t("ui.frame.btn.delete")}</el-button></div>) },
      ];
    },
    searchColumns() {
      return [
        { label: this.$t("ui.data.column.cd90MachineRollMapping.factoryCode"), prop: "factoryCode", type: "select", dictData: this.dict.type.biz_factory_name, filterable: true },
        { label: this.$t("ui.data.column.cd90MachineRollMapping.bigRollCode"), prop: "bigRollCode" },
        { label: this.$t("ui.data.column.cd90MachineRollMapping.machineCode"), prop: "machineCode", type: "select", dictData: this.machineOptions, filterable: true, clearable: true },
        { label: this.$t("ui.data.column.cd90MachineRollMapping.cordFabricCode"), prop: "cordFabricCode", type: "select", dictData: this.clothOptions, filterable: true, clearable: true },
      ];
    },
  },
  methods: {
    handleAdd() { this.$refs.infoRef && this.$refs.infoRef.show(); },
    handleEdit(row) { this.$refs.infoRef && this.$refs.infoRef.show(row); },
    handleBatchEdit() { if (this.selection.length === 1) this.handleEdit(this.selection[0]); },
    handleDelete(row) { this.$confirm(this.$t("common.confirm.delete"), { type: "warning" }).then(() => { delMachineRollMapping({ ids: row.id }).then((data) => { this.$modal.msgSuccess(data.msg); this.$set(this.page, "current", 1); this.getList(); }); }); },
    handleBatchDelete() { if (!this.selection || this.selection.length === 0) return; this.$confirm(this.$t("common.confirm.delete"), { type: "warning" }).then(() => { const ids = this.selection.map(item => item.id).join(","); delMachineRollMapping({ ids }).then((data) => { this.$modal.msgSuccess(data.msg); this.selection = []; this.$set(this.page, "current", 1); this.getList(); }); }); },
    handleExport() { exportMachineRollMapping(this.query); },
    handleSearch(params) { this.page.current = 1; this.query = { ...params }; this.getList(); },
    handlePageChange(page) { this.page = page; this.getList(); },
    handleSortChange(sort) { this.sort = sort; this.getList(); },
    handleSelectionChange(selection) { this.selection = selection || []; },
    async getList() { this.loading = true; try { const params = { ...this.query, pageNum: this.page.current, pageSize: this.page.pageSize, orderByColumn: this.sort.prop, isAsc: this.sort.order }; const res = await listMachineRollMapping(params); this.data = res.rows || []; this.page.total = res.total || 0; } finally { this.loading = false; } },
    async loadClothOptions() {
      const res = await listTireFabricCodes();
      const rows = Array.isArray(res) ? res : (res.data || []);
      this.clothOptions = rows.map((code) => ({ label: code, value: code }));
    },
    async loadMachineOptions() {
      const res = await getCd90MachineEnableOptions({ factoryCode: this.query.factoryCode });
      const rows = Array.isArray(res) ? res : (res.rows || res.data || []);
      this.machineOptions = rows.map((item) => ({ label: item.machineCode, value: item.machineCode }));
    },
  },
  created() { this.getList(); this.loadClothOptions(); this.loadMachineOptions(); },
};
</script>