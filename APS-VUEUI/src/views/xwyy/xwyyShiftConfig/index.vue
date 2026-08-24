<template>
  <basic-container>
    <page-table tableRef="shiftConfigMainTable" :calcHeight="true" v-loading="loading" :columns="columns" :searchColumns="searchColumns" :data="data" :page="page" :search="search" @refresh="getList" @search="handleSearch" @pageChange="handlePageChange" @sort-change="handleSortChange" @selection-change="handleSelectionChange" :showSummary="false" :selectArea="false">
      <template slot="header">
        <el-button type="primary" plain v-hasPermi="['xwyy:shiftConfig:add']" @click="handleAdd">{{ $t("ui.frame.btn.add") }}</el-button>
        <el-button v-hasPermi="['xwyy:shiftConfig:edit']" @click="handleBatchEdit" :disabled="selection.length !== 1">{{ $t("ui.frame.btn.update") }}</el-button>
        <el-button type="danger" v-hasPermi="['xwyy:shiftConfig:remove']" :disabled="selection.length === 0" @click="handleBatchDelete">{{ $t("ui.frame.btn.delete") }}</el-button>
        <el-button v-hasPermi="['xwyy:shiftConfig:import']" @click="$refs.tltUpload.handleImport()">{{ $t("ui.frame.btn.import") }}</el-button>
        <el-button @click="handleExport" v-hasPermi="['xwyy:shiftConfig:export']">{{ $t("ui.frame.btn.export") }}</el-button>
      </template>
    </page-table>
    <tlt-upload-form ref="tltUpload" :updateSupport="true" downloadUrl="/xwyy/xwyyShiftConfig/importTemplate" uploadUrl="/xwyy/xwyyShiftConfig/importData" @uploadSuccess="getList" labelWidth="0" :columns="importColumns" />
    <info-dialog ref="infoRef" @success="getList" />
  </basic-container>
</template>

<script>
import { listShiftConfig, delShiftConfig, changeStatus, exportShiftConfig } from "@/api/xwyy/xwyyShiftConfig";
import TltUploadForm from "@/views/components/tltUploadForm.vue";
import infoDialog from "./components/infoDialog.vue";

export default {
  name: "XwyyShiftConfig",
  components: { TltUploadForm, infoDialog },
  dicts: ["biz_factory_name", "biz_yes_no", "sys_enable_disable"],
  provide() { return { parentDict: this.dict }; },
  data() {
    return {
      importColumns: [{ label: "", prop: "updateSupport", render: (form) => (<el-checkbox label={this.$t("common.rule.updateSupport")} v-model={form.updateSupport}>{this.$t("common.rule.updateSupport")}</el-checkbox>) }],
      loading: false, data: [], selection: [],
      page: { current: 1, pageSize: 20, total: 0 }, sort: {},
      search: { factoryCode: "116" }, query: { factoryCode: "116" },
    };
  },
  computed: {
    columns() {
      return [
        { type: "selection", fixed: "left" },
        { prop: "factoryCode", align: "center", halign: "center", label: this.$t("ui.data.column.xwyyShiftConfig.factoryCode"), minWidth: 120, formatter: (row, column, value) => this.selectDictLabel(this.dict.type.biz_factory_name, value) },
        { prop: "shiftCode", align: "center", halign: "center", label: this.$t("ui.data.column.xwyyShiftConfig.shiftCode"), minWidth: 120 },
        { prop: "shiftName", align: "center", halign: "center", label: this.$t("ui.data.column.xwyyShiftConfig.shiftName"), minWidth: 120 },
        { prop: "shiftOrder", align: "center", halign: "center", label: this.$t("ui.data.column.xwyyShiftConfig.shiftOrder"), minWidth: 80 },
        { prop: "startTime", align: "center", halign: "center", label: this.$t("ui.data.column.xwyyShiftConfig.startTime"), minWidth: 100 },
        { prop: "endTime", align: "center", halign: "center", label: this.$t("ui.data.column.xwyyShiftConfig.endTime"), minWidth: 100 },
        { prop: "shiftHours", align: "center", halign: "center", label: this.$t("ui.data.column.xwyyShiftConfig.shiftHours"), minWidth: 80 },
        { prop: "isCrossDay", align: "center", halign: "center", label: this.$t("ui.data.column.xwyyShiftConfig.isCrossDay"), minWidth: 80, formatter: (row, column, value) => this.selectDictLabel(this.dict.type.biz_yes_no, value) },
        { prop: "scheduleDay", align: "center", halign: "center", label: this.$t("ui.data.column.xwyyShiftConfig.scheduleDay"), minWidth: 80 },
        { prop: "dayShiftOrder", align: "center", halign: "center", label: this.$t("ui.data.column.xwyyShiftConfig.dayShiftOrder"), minWidth: 100 },
        { prop: "classField", align: "center", halign: "center", label: this.$t("ui.data.column.xwyyShiftConfig.classField"), minWidth: 120 },
        { prop: "isActive", align: "center", halign: "center", label: this.$t("ui.data.column.xwyyShiftConfig.isActive"), minWidth: 80, formatter: (row, column, value) => this.selectDictLabel(this.dict.type.sys_enable_disable, value) },
        { prop: "remark", halign: "center", label: this.$t("ui.common.column.remark"), minWidth: 120 },
        { align: "center", halign: "center", label: this.$t("ui.data.btn.option"), prop: "option", minWidth: 260, width: 260, fixed: "right",
          render: ({ row }) => (<div><el-button v-hasPermi={["xwyy:shiftConfig:edit"]} class="minus" type="success" onClick={() => this.handleEdit(row)}>{this.$t("ui.frame.btn.update")}</el-button><el-button v-hasPermi={["xwyy:shiftConfig:edit"]} class="minus" type="warning" onClick={() => this.handleToggleStatus(row)}>{row.isActive === 1 ? this.$t("common.disable") : this.$t("common.enable")}</el-button><el-button v-hasPermi={["xwyy:shiftConfig:remove"]} class="minus" type="danger" onClick={() => this.handleDelete(row)}>{this.$t("ui.frame.btn.delete")}</el-button></div>) },
      ];
    },
    searchColumns() {
      return [
        { label: this.$t("ui.data.column.xwyyShiftConfig.factoryCode"), prop: "factoryCode", type: "select", dictData: this.dict.type.biz_factory_name, filterable: true },
        { label: this.$t("ui.data.column.xwyyShiftConfig.shiftCode"), prop: "shiftCode", type: "input" },
        { label: this.$t("ui.data.column.xwyyShiftConfig.shiftName"), prop: "shiftName", type: "input" },
        { label: this.$t("ui.data.column.xwyyShiftConfig.isActive"), prop: "isActive", type: "select", dictData: this.dict.type.sys_enable_disable },
      ];
    },
  },
  methods: {
    handleAdd() { this.$refs.infoRef && this.$refs.infoRef.show(); },
    handleEdit(row) { this.$refs.infoRef && this.$refs.infoRef.show(row); },
    handleBatchEdit() { if (this.selection.length === 1) this.handleEdit(this.selection[0]); },
    async handleToggleStatus(row) {
      const newStatus = row.isActive === 1 ? 0 : 1;
      const res = await changeStatus({ id: row.id, isActive: newStatus });
      this.$modal.msgSuccess(res.msg);
      this.getList();
    },
    handleDelete(row) { this.$confirm(this.$t("common.confirm.delete"), { type: "warning" }).then(() => { delShiftConfig({ ids: row.id }).then((data) => { this.$modal.msgSuccess(data.msg); this.$set(this.page, "current", 1); this.getList(); }); }); },
    handleBatchDelete() { if (!this.selection || this.selection.length === 0) return; this.$confirm(this.$t("common.confirm.delete"), { type: "warning" }).then(() => { const ids = this.selection.map(item => item.id).join(","); delShiftConfig({ ids }).then((data) => { this.$modal.msgSuccess(data.msg); this.selection = []; this.$set(this.page, "current", 1); this.getList(); }); }); },
    handleExport() { exportShiftConfig(this.query); },
    handleSearch(params) { this.page.current = 1; this.query = { ...params }; this.getList(); },
    handlePageChange(current, pageSize) { this.page.current = current; this.page.pageSize = pageSize; this.getList(); },
    handleSortChange(sort) { this.sort = sort; this.getList(); },
    handleSelectionChange(selection) { this.selection = selection || []; },
    async getList() { this.loading = true; try { const params = { ...this.query, pageNum: this.page.current, pageSize: this.page.pageSize, orderByColumn: this.sort.prop, isAsc: this.sort.order }; const res = await listShiftConfig(params); this.data = res.rows || []; this.page.total = res.total || 0; } finally { this.loading = false; } },
  },
  created() { this.getList(); },
};
</script>
