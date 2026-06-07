<template>
  <basic-container>
    <page-table tableRef="scheduleResultMainTable" :calcHeight="true" v-loading="loading" :columns="columns" :searchColumns="searchColumns" :data="data" :page="page" :search="search" @refresh="getList" @search="handleSearch" @pageChange="handlePageChange" @sort-change="handleSortChange" @selection-change="handleSelectionChange" :showSummary="false" :selectArea="false">
      <template slot="header">
        <el-button type="danger" v-hasPermi="['cd90:scheduleResult:remove']" :disabled="selection.length === 0" @click="handleBatchDelete">{{ $t("ui.frame.btn.delete") }}</el-button>
        <el-button v-hasPermi="['cd90:scheduleResult:import']" @click="$refs.tltUpload.handleImport()">{{ $t("ui.frame.btn.import") }}</el-button>
        <el-button @click="handleExport" v-hasPermi="['cd90:scheduleResult:export']">{{ $t("ui.frame.btn.export") }}</el-button>
      </template>
    </page-table>
    <tlt-upload-form ref="tltUpload" :updateSupport="true" downloadUrl="/cd90/cd90ScheduleResult/importTemplate" uploadUrl="/cd90/cd90ScheduleResult/importData" @uploadSuccess="getList" labelWidth="0" :columns="importColumns" />
  </basic-container>
</template>

<script>
import { listScheduleResult, delScheduleResult, exportScheduleResult } from "@/api/cd90/scheduleResult";
import TltUploadForm from "@/views/components/tltUploadForm.vue";

export default {
  name: "ScheduleResult",
  components: { TltUploadForm },
  dicts: ["biz_factory_name"],
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
        { prop: "factoryCode", align: "center", halign: "center", label: this.$t("ui.data.column.scheduleResult.factoryCode"), minWidth: 120, formatter: (row, column, value) => this.selectDictLabel(this.dict.type.biz_factory_name, value) },
        { prop: "scheduleDate", align: "center", halign: "center", label: this.$t("ui.data.column.scheduleResult.scheduleDate"), minWidth: 120 },
        { prop: "batchNo", align: "center", halign: "center", label: this.$t("ui.data.column.scheduleResult.batchNo"), minWidth: 140 },
        { prop: "clothCode", align: "center", halign: "center", label: this.$t("ui.data.column.scheduleResult.clothCode"), minWidth: 120 },
        { prop: "machineCode", align: "center", halign: "center", label: this.$t("ui.data.column.scheduleResult.machineCode"), minWidth: 130 },
        { prop: "bigRollCode", align: "center", halign: "center", label: this.$t("ui.data.column.scheduleResult.bigRollCode"), minWidth: 140 },
        { prop: "stockQty", align: "center", halign: "center", label: this.$t("ui.data.column.scheduleResult.stockQty"), minWidth: 110 },
        { prop: "class1PlanQty", align: "center", halign: "center", label: this.$t("ui.data.column.scheduleResult.class1PlanQty"), minWidth: 110 },
        { prop: "class2PlanQty", align: "center", halign: "center", label: this.$t("ui.data.column.scheduleResult.class2PlanQty"), minWidth: 110 },
        { prop: "class3PlanQty", align: "center", halign: "center", label: this.$t("ui.data.column.scheduleResult.class3PlanQty"), minWidth: 110 },
        { prop: "isRelease", align: "center", halign: "center", label: this.$t("ui.data.column.scheduleResult.isRelease"), minWidth: 100 },
        { prop: "remark", halign: "center", label: this.$t("ui.common.column.remark"), minWidth: 120 },
      ];
    },
    searchColumns() {
      return [
        { label: this.$t("ui.data.column.scheduleResult.factoryCode"), prop: "factoryCode", type: "select", dictData: this.dict.type.biz_factory_name, filterable: true },
        { label: this.$t("ui.data.column.scheduleResult.scheduleDate"), prop: "scheduleDate" },
        { label: this.$t("ui.data.column.scheduleResult.clothCode"), prop: "clothCode" },
        { label: this.$t("ui.data.column.scheduleResult.machineCode"), prop: "machineCode" },
      ];
    },
  },
  methods: {
    handleBatchDelete() { if (!this.selection || this.selection.length === 0) return; this.$confirm(this.$t("common.confirm.delete"), { type: "warning" }).then(() => { const ids = this.selection.map(item => item.id).join(","); delScheduleResult({ ids }).then((data) => { this.$modal.msgSuccess(data.msg); this.selection = []; this.$set(this.page, "current", 1); this.getList(); }); }); },
    handleExport() { exportScheduleResult(this.query); },
    handleSearch(params) { this.page.current = 1; this.query = { ...params }; this.getList(); },
    handlePageChange(page) { this.page = page; this.getList(); },
    handleSortChange(sort) { this.sort = sort; this.getList(); },
    handleSelectionChange(selection) { this.selection = selection || []; },
    async getList() { this.loading = true; try { const params = { ...this.query, pageNum: this.page.current, pageSize: this.page.pageSize, orderByColumn: this.sort.prop, isAsc: this.sort.order }; const res = await listScheduleResult(params); this.data = res.rows || []; this.page.total = res.total || 0; } finally { this.loading = false; } },
  },
  created() { this.getList(); },
};
</script>