<template>
  <basic-container>
    <page-table tableRef="storageLaneMainTable" :calcHeight="true" v-loading="loading" :columns="columns" :searchColumns="searchColumns" :data="data" :page="page" :search="search" @refresh="getList" @search="handleSearch" @pageChange="handlePageChange" @sort-change="handleSortChange" @selection-change="handleSelectionChange" :showSummary="false" :selectArea="false">
      <template slot="header">
        <el-button type="primary" plain v-hasPermi="['cd90:storageLaneLimit:add']" @click="handleAdd">{{ $t("ui.frame.btn.add") }}</el-button>
        <el-button v-hasPermi="['cd90:storageLaneLimit:edit']" @click="handleBatchEdit" :disabled="selection.length !== 1">{{ $t("ui.frame.btn.update") }}</el-button>
        <el-button type="danger" v-hasPermi="['cd90:storageLaneLimit:remove']" :disabled="selection.length === 0" @click="handleBatchDelete">{{ $t("ui.frame.btn.delete") }}</el-button>
        <el-button v-hasPermi="['cd90:storageLaneLimit:import']" @click="$refs.tltUpload.handleImport()">{{ $t("ui.frame.btn.import") }}</el-button>
        <el-button @click="handleExport" v-hasPermi="['cd90:storageLaneLimit:export']">{{ $t("ui.frame.btn.export") }}</el-button>
      </template>
    </page-table>
    <tlt-upload-form ref="tltUpload" :updateSupport="true" downloadUrl="/cd90/cd90StorageLaneLimit/importTemplate" uploadUrl="/cd90/cd90StorageLaneLimit/importData" @uploadSuccess="getList" labelWidth="0" :columns="importColumns" />
    <info-dialog ref="infoRef" :cloth-options="clothOptions" @success="getList" />
  </basic-container>
</template>

<script>
import { listStorageLaneLimit, delStorageLaneLimit, exportStorageLaneLimit } from "@/api/cd90/storageLaneLimit";
import { listTireFabricCodes } from "@/api/cd90/specifyMachine";
import TltUploadForm from "@/views/components/tltUploadForm.vue";
import infoDialog from "./components/infoDialog.vue";

export default {
  name: "StorageLaneLimit",
  components: { TltUploadForm, infoDialog },
  dicts: ["biz_factory_name", "class_num_three_plan"],
  provide() { return { parentDict: this.dict }; },
  data() {
    return {
      importColumns: [{ label: "", prop: "updateSupport", render: (form) => (<el-checkbox label={this.$t("common.rule.updateSupport")} v-model={form.updateSupport}>{this.$t("common.rule.updateSupport")}</el-checkbox>) }],
      loading: false, data: [], selection: [], clothOptions: [],
      page: { current: 1, pageSize: 20, total: 0 }, sort: {},
      search: { factoryCode: "116", laneDate: new Date().toISOString().slice(0, 10) }, query: { factoryCode: "116", laneDate: new Date().toISOString().slice(0, 10) },
    };
  },
  computed: {
    columns() {
      return [
        { type: "selection", fixed: "left" },
        { prop: "factoryCode", align: "center", halign: "center", label: this.$t("ui.data.column.cd90StorageLaneLimit.factoryCode"), minWidth: 120, formatter: (row, column, value) => this.selectDictLabel(this.dict.type.biz_factory_name, value) },
        { prop: "laneDate", align: "center", halign: "center", label: this.$t("ui.data.column.cd90StorageLaneLimit.laneDate"), minWidth: 110 },
        { prop: "materialCode", align: "center", halign: "center", label: this.$t("ui.data.column.cd90StorageLaneLimit.materialCode"), minWidth: 150 },
        { prop: "shiftCode", align: "center", halign: "center", label: this.$t("ui.data.column.cd90StorageLaneLimit.shiftCode"), minWidth: 90, formatter: (row, column, value) => this.selectDictLabel(this.dict.type.class_num_three_plan, value) },
        { prop: "storageLaneCode", align: "center", halign: "center", label: this.$t("ui.data.column.cd90StorageLaneLimit.storageLaneCode"), minWidth: 130 },
        { prop: "carNum", align: "center", halign: "center", label: this.$t("ui.data.column.cd90StorageLaneLimit.carNum"), minWidth: 90 },
        { prop: "maxCarNum", align: "center", halign: "center", label: this.$t("ui.data.column.cd90StorageLaneLimit.maxCarNum"), minWidth: 90 },
        { prop: "remark", halign: "center", label: this.$t("ui.common.column.remark"), minWidth: 120 },
        { prop: "updateTime", align: "center", halign: "center", label: this.$t("common.updateTime"), minWidth: 160 },
        { align: "center", halign: "center", label: this.$t("ui.data.btn.option"), prop: "option", minWidth: 150, fixed: "right",
          render: ({ row }) => (<div><el-button v-hasPermi={["cd90:storageLaneLimit:edit"]} class="minus" type="success" onClick={() => this.handleEdit(row)}>{this.$t("ui.frame.btn.update")}</el-button><el-button v-hasPermi={["cd90:storageLaneLimit:remove"]} class="minus" type="danger" onClick={() => this.handleDelete(row)}>{this.$t("ui.frame.btn.delete")}</el-button></div>) },
      ];
    },
    searchColumns() {
      return [
        { label: this.$t("ui.data.column.cd90StorageLaneLimit.factoryCode"), prop: "factoryCode", type: "select", dictData: this.dict.type.biz_factory_name, filterable: true },
        { label: this.$t("ui.data.column.cd90StorageLaneLimit.materialCode"), prop: "materialCode", type: "select", dictData: this.clothOptions, filterable: true, clearable: true },
        { label: this.$t("ui.data.column.cd90StorageLaneLimit.laneDate"), prop: "laneDate", type: "date", valueFormat: "yyyy-MM-dd" },
        { label: this.$t("ui.data.column.cd90StorageLaneLimit.shiftCode"), prop: "shiftCode", type: "select", dictData: this.dict.type.class_num_three_plan, filterable: true },
        { label: this.$t("ui.data.column.cd90StorageLaneLimit.storageLaneCode"), prop: "storageLaneCode" },
      ];
    },
  },
  methods: {
    handleAdd() { this.$refs.infoRef && this.$refs.infoRef.show(); },
    handleEdit(row) { this.$refs.infoRef && this.$refs.infoRef.show(row); },
    handleBatchEdit() { if (this.selection.length === 1) this.handleEdit(this.selection[0]); },
    handleDelete(row) { this.$confirm(this.$t("common.confirm.delete"), { type: "warning" }).then(() => { delStorageLaneLimit({ ids: row.id }).then((data) => { this.$modal.msgSuccess(data.msg); this.$set(this.page, "current", 1); this.getList(); }); }); },
    handleBatchDelete() { if (!this.selection || this.selection.length === 0) return; this.$confirm(this.$t("common.confirm.delete"), { type: "warning" }).then(() => { const ids = this.selection.map(item => item.id).join(","); delStorageLaneLimit({ ids }).then((data) => { this.$modal.msgSuccess(data.msg); this.selection = []; this.$set(this.page, "current", 1); this.getList(); }); }); },
    handleExport() { exportStorageLaneLimit(this.query); },
    handleSearch(params) { this.page.current = 1; this.query = { ...params }; this.getList(); },
    handlePageChange(current, pageSize) { this.page.current = current; this.page.pageSize = pageSize; this.getList(); },
    handleSortChange(sort) { this.sort = sort; this.getList(); },
    handleSelectionChange(selection) { this.selection = selection || []; },
    async getList() { this.loading = true; try { const params = { ...this.query, pageNum: this.page.current, pageSize: this.page.pageSize, orderByColumn: this.sort.prop, isAsc: this.sort.order }; const res = await listStorageLaneLimit(params); this.data = res.rows || []; this.page.total = res.total || 0; } finally { this.loading = false; } },
    async loadClothOptions() {
      const res = await listTireFabricCodes();
      const rows = Array.isArray(res) ? res : (res.data || []);
      this.clothOptions = rows.map((code) => ({ label: code, value: code }));
    },
  },
  created() { this.getList(); this.loadClothOptions(); },
};
</script>