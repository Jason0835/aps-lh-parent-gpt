<template>
  <basic-container>
    <page-table
      tableRef="cd15DepthConfigMainTable"
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
        <el-button type="primary" plain v-hasPermi="['cd15:depthConfig:add']" @click="handleAdd">{{ $t("ui.frame.btn.add") }}</el-button>
        <el-button v-hasPermi="['cd15:depthConfig:edit']" @click="handleBatchEdit" :disabled="selection.length !== 1">{{ $t("ui.frame.btn.update") }}</el-button>
        <el-button type="danger" v-hasPermi="['cd15:depthConfig:remove']" :disabled="selection.length === 0" @click="handleBatchDelete">{{ $t("ui.frame.btn.delete") }}</el-button>
        <el-button v-hasPermi="['cd15:depthConfig:import']" @click="$refs.tltUpload.handleImport()">{{ $t("ui.frame.btn.import") }}</el-button>
        <el-button @click="handleExport" v-hasPermi="['cd15:depthConfig:export']">{{ $t("ui.frame.btn.export") }}</el-button>
      </template>
    </page-table>
    <tlt-upload-form ref="tltUpload" :updateSupport="true" downloadUrl="/cd15/cd15DepthConfig/importTemplate" uploadUrl="/cd15/cd15DepthConfig/importData" @uploadSuccess="getList" labelWidth="0" :columns="importColumns" />
    <info-dialog ref="infoRef" @success="getList" />
  </basic-container>
</template>

<script>
import { listDepthConfig, delDepthConfig, exportDepthConfig } from "@/api/cd15/depthConfig";
import TltUploadForm from "@/views/components/tltUploadForm.vue";
import infoDialog from "./components/infoDialog.vue";

export default {
  name: "Cd15DepthConfig",
  components: { TltUploadForm, infoDialog },
  dicts: ["biz_factory_name"],
  provide() { return { parentDict: this.dict }; },
  data() {
    return {
      loading: false,
      importColumns: [{ label: "", prop: "updateSupport", render: (form) => { return (<el-checkbox label={this.$t("common.rule.updateSupport")} v-model={form.updateSupport}>{this.$t("common.rule.updateSupport")}</el-checkbox>); } }],
      data: [],
      selection: [],
      page: { current: 1, pageSize: 20, total: 0 },
      sort: {},
      search: { factoryCode: "116" },
      query: { factoryCode: "116" },
    };
  },
  computed: {
    columns() {
      return [
        { type: "selection", fixed: "left" },
        { prop: "factoryCode", align: "center", halign: "center", label: this.$t("ui.data.column.cd15DepthConfig.factoryCode"), minWidth: 120, formatter: (row, column, value) => this.selectDictLabel(this.dict.type.biz_factory_name, value) },
        { prop: "minMachineQty", align: "center", halign: "center", label: this.$t("ui.data.column.cd15DepthConfig.minMachineQty"), minWidth: 120 },
        { prop: "maxMachineQty", align: "center", halign: "center", label: this.$t("ui.data.column.cd15DepthConfig.maxMachineQty"), minWidth: 120, formatter: (row, column, value) => value == null ? "∞" : value },
        { prop: "depthClassQty", align: "center", halign: "center", label: this.$t("ui.data.column.cd15DepthConfig.depthClassQty"), minWidth: 100 },
        { prop: "remark", halign: "center", label: this.$t("ui.common.column.remark"), minWidth: 120 },
        { prop: "updateTime", align: "center", halign: "center", label: this.$t("common.updateTime"), minWidth: 160 },
        {
          align: "center",
          halign: "center",
          label: this.$t("ui.data.btn.option"),
          prop: "option",
          minWidth: 160, width: 160,
          fixed: "right",
          render: ({ row }) => (
            <div>
              <el-button v-hasPermi={["cd15:depthConfig:edit"]} class="minus" type="success" onClick={() => this.handleEdit(row)}>{this.$t("ui.frame.btn.update")}</el-button>
              <el-button v-hasPermi={["cd15:depthConfig:remove"]} class="minus" type="danger" onClick={() => this.handleDelete(row)}>{this.$t("ui.frame.btn.delete")}</el-button>
            </div>
          ),
        },
      ];
    },
    searchColumns() {
      return [
        { label: this.$t("ui.data.column.cd15DepthConfig.factoryCode"), prop: "factoryCode", type: "select", dictData: this.dict.type.biz_factory_name, filterable: true },
        { label: this.$t("ui.data.column.cd15DepthConfig.minMachineQty"), prop: "minMachineQty", type: "number" },
      ];
    },
  },
  methods: {
    handleAdd() { this.$refs.infoRef && this.$refs.infoRef.show(null, this.query.factoryCode || this.search.factoryCode); },
    handleEdit(row) { this.$refs.infoRef && this.$refs.infoRef.show(row); },
    handleBatchEdit() {
      if (this.selection.length === 1) {
        this.handleEdit(this.selection[0]);
      }
    },
    handleDelete(row) {
      this.$confirm(this.$t("common.confirm.delete"), { type: "warning" }).then(() => {
        delDepthConfig({ ids: row.id }).then((data) => {
          this.$modal.msgSuccess(data.msg);
          this.$set(this.page, "current", 1);
          this.getList();
        });
      });
    },
    handleBatchDelete() {
      if (!this.selection || this.selection.length === 0) {
        return;
      }
      this.$confirm(this.$t("common.confirm.delete"), { type: "warning" }).then(() => {
        const ids = this.selection.map((item) => item.id).join(",");
        delDepthConfig({ ids }).then((data) => {
          this.$modal.msgSuccess(data.msg);
          this.selection = [];
          this.$set(this.page, "current", 1);
          this.getList();
        });
      });
    },
    handleExport() { exportDepthConfig(this.query); },
    handleSearch(params) {
      this.page.current = 1;
      this.query = { ...params };
      this.getList();
    },
    handlePageChange(current, pageSize) {
      this.page.current = current;
      this.page.pageSize = pageSize;
      this.getList();
    },
    handleSortChange(sort) {
      this.sort = sort;
      this.getList();
    },
    handleSelectionChange(selection) {
      this.selection = selection || [];
    },
    async getList() {
      this.loading = true;
      try {
        const params = { ...this.query, pageNum: this.page.current, pageSize: this.page.pageSize, orderByColumn: this.sort.prop, isAsc: this.sort.order };
        const res = await listDepthConfig(params);
        this.data = res.rows || [];
        this.page.total = res.total || 0;
      } finally {
        this.loading = false;
      }
    },
  },
  created() { this.getList(); },
};
</script>
