<template>
  <basic-container>
    <page-table tableRef="depthConfigMainTable" :calcHeight="true" v-loading="loading" :columns="columns" :searchColumns="searchColumns" :data="data" :page="page" :search="search" @refresh="getList" @search="handleSearch" @pageChange="handlePageChange" @sort-change="handleSortChange" @selection-change="handleSelectionChange" :showSummary="false" :selectArea="false">
      <template slot="header">
        <el-button type="primary" plain v-hasPermi="['cd90:depthConfig:add']" @click="handleAdd">{{ $t("ui.frame.btn.add") }}</el-button>
        <el-button v-hasPermi="['cd90:depthConfig:edit']" @click="handleBatchEdit" :disabled="selection.length !== 1">{{ $t("ui.frame.btn.update") }}</el-button>
        <el-button type="danger" v-hasPermi="['cd90:depthConfig:remove']" :disabled="selection.length === 0" @click="handleBatchDelete">{{ $t("ui.frame.btn.delete") }}</el-button>

      </template>
    </page-table>
    <info-dialog ref="infoRef" @success="getList" />
  </basic-container>
</template>

<script>
import { listDepthConfig, delDepthConfig } from "@/api/cd90/depthConfig";
import infoDialog from "./components/infoDialog.vue";

export default {
  name: "DepthConfig",
  components: { infoDialog },
  dicts: ["biz_factory_name", "machine_range"],
  provide() { return { parentDict: this.dict }; },
  data() {
    return {
      loading: false, data: [], selection: [],
      page: { current: 1, pageSize: 20, total: 0 }, sort: {},
      search: { factoryCode: "116" }, query: { factoryCode: "116" },
    };
  },
  computed: {
    columns() {
      return [
        { type: "selection", fixed: "left" },
        { prop: "factoryCode", align: "center", halign: "center", label: this.$t("ui.data.column.cd90DepthConfig.factoryCode"), minWidth: 120, formatter: (row, column, value) => this.selectDictLabel(this.dict.type.biz_factory_name, value) },
        { prop: "machineQty", align: "center", halign: "center", label: this.$t("ui.data.column.cd90DepthConfig.machineQty"), minWidth: 120 },
        { prop: "machineRange", align: "center", halign: "center", label: this.$t("ui.data.column.cd90DepthConfig.machineRange"), minWidth: 100, formatter: (row, column, value) => this.selectDictLabel(this.dict.type.machine_range, value) },
        { prop: "depthClassQty", align: "center", halign: "center", label: this.$t("ui.data.column.cd90DepthConfig.depthClassQty"), minWidth: 100 },
        { prop: "remark", halign: "center", label: this.$t("ui.common.column.remark"), minWidth: 120 },
        { prop: "updateTime", align: "center", halign: "center", label: this.$t("common.updateTime"), minWidth: 160 },
        { align: "center", halign: "center", label: this.$t("ui.data.btn.option"), prop: "option", minWidth: 150, fixed: "right",
          render: ({ row }) => (<div><el-button v-hasPermi={["cd90:depthConfig:edit"]} class="minus" type="success" onClick={() => this.handleEdit(row)}>{this.$t("ui.frame.btn.update")}</el-button><el-button v-hasPermi={["cd90:depthConfig:remove"]} class="minus" type="danger" onClick={() => this.handleDelete(row)}>{this.$t("ui.frame.btn.delete")}</el-button></div>) },
      ];
    },
    searchColumns() {
      return [
        { label: this.$t("ui.data.column.cd90DepthConfig.factoryCode"), prop: "factoryCode", type: "select", dictData: this.dict.type.biz_factory_name, filterable: true },
        { label: this.$t("ui.data.column.cd90DepthConfig.machineQty"), prop: "machineQty", type: "number" },
        { label: this.$t("ui.data.column.cd90DepthConfig.machineRange"), prop: "machineRange", type: "select", dictData: this.dict.type.machine_range, filterable: true },
      ];
    },
  },
  methods: {
    handleAdd() { this.$refs.infoRef && this.$refs.infoRef.show(); },
    handleEdit(row) { this.$refs.infoRef && this.$refs.infoRef.show(row); },
    handleBatchEdit() { if (this.selection.length === 1) this.handleEdit(this.selection[0]); },
    handleDelete(row) { this.$confirm(this.$t("common.confirm.delete"), { type: "warning" }).then(() => { delDepthConfig({ ids: row.id }).then((data) => { this.$modal.msgSuccess(data.msg); this.$set(this.page, "current", 1); this.getList(); }); }); },
    handleBatchDelete() { if (!this.selection || this.selection.length === 0) return; this.$confirm(this.$t("common.confirm.delete"), { type: "warning" }).then(() => { const ids = this.selection.map(item => item.id).join(","); delDepthConfig({ ids }).then((data) => { this.$modal.msgSuccess(data.msg); this.selection = []; this.$set(this.page, "current", 1); this.getList(); }); }); },
    handleSearch(params) { this.page.current = 1; this.query = { ...params }; this.getList(); },
    handlePageChange(current, pageSize) { this.page.current = current; this.page.pageSize = pageSize; this.getList(); },
    handleSortChange(sort) { this.sort = sort; this.getList(); },
    handleSelectionChange(selection) { this.selection = selection || []; },
    async getList() { this.loading = true; try { const params = { ...this.query, pageNum: this.page.current, pageSize: this.page.pageSize, orderByColumn: this.sort.prop, isAsc: this.sort.order }; const res = await listDepthConfig(params); this.data = res.rows || []; this.page.total = res.total || 0; } finally { this.loading = false; } },
  },
  created() { this.getList(); },
};
</script>
