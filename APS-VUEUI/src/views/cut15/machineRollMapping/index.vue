<template>
  <basic-container>
    <page-table
      tableRef="cut15PressMachineRollMappingMainTable"
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
        <el-button type="primary" plain v-hasPermi="['cd15:machineRollMapping:add']" @click="handleAdd">{{ $t("ui.frame.btn.add") }}</el-button>
        <el-button type="danger" plain v-hasPermi="['cd15:machineRollMapping:remove']" :disabled="selection.length === 0" @click="handleDelete(selection)">{{ $t("ui.frame.btn.delete") }}</el-button>
        <el-button v-hasPermi="['cd15:machineRollMapping:import']" @click="$refs.tltUpload.handleImport()">{{ $t("ui.frame.btn.import") }}</el-button>
        <el-button @click="handleExport" v-hasPermi="['cd15:machineRollMapping:export']">{{ $t("ui.frame.btn.export") }}</el-button>
      </template>
    </page-table>
    <tlt-upload
      ref="tltUpload"
      downloadUrl="/cd15/machineRollMapping/importTemplate"
      uploadUrl="/cd15/machineRollMapping/importData"
      @uploadSuccess="getList"
    />
    <infoDialog ref="infoRef" :machineOptions="machineOptions" @success="getList" />
  </basic-container>
</template>

<script>
import { downloadLink } from "@/utils/request";
import { listMachineRollMapping, removeMachineRollMapping } from "@/api/cd15/machineRollMapping";
import { getCd15MachineEnableOptions } from "@/api/cd15/cd15MachineInfo";
import tltUpload from "@/components/tltUpload/tltUpload.vue";
import infoDialog from "./components/infoDialog.vue";

export default {
  name: "Cut15MachineRollMapping",
  components: { tltUpload, infoDialog },
  dicts: ["biz_factory_name"],
  provide() {
    return { parentDict: this.dict };
  },
  data() {
    return {
      loading: false,
      data: [],
      selection: [],
      page: { current: 1, pageSize: 20, total: 0 },
      sort: {},
      search: {},
      query: { factoryCode: "116" },
      machineOptions: [],
    };
  },
  computed: {
    columns() {
      return [
        { type: "selection", fixed: "left" },
        { prop: "factoryCode", halign: "center", align: "center", label: this.$t("ui.data.column.cd15MachineRollMapping.factoryCode"), dictType: "biz_factory_name", width: 120 },
        { prop: "bigRollCode", halign: "center", label: this.$t("ui.data.column.cd15MachineRollMapping.bigRollCode"), minWidth: 160 },
        { prop: "machineCode", halign: "center", align: "center", label: this.$t("ui.data.column.cd15MachineRollMapping.machineCode"), width: 140 },
        { prop: "remark", halign: "center", label: this.$t("ui.common.column.remark"), minWidth: 160 },
        {
          align: "center",
          halign: "center",
          label: this.$t("ui.data.btn.option"),
          minWidth: 160,
          width: 160,
          fixed: "right",
          render: ({ row }) => (
            <div>
              <el-button v-hasPermi={["cd15:machineRollMapping:edit"]} class="minus" type="success" onClick={() => this.handleEdit(row)}>{this.$t("ui.frame.btn.update")}</el-button>
              <el-button v-hasPermi={["cd15:machineRollMapping:remove"]} class="minus" type="danger" onClick={() => this.handleDelete([row])}>{this.$t("ui.frame.btn.delete")}</el-button>
            </div>
          ),
        },
      ];
    },
    searchColumns() {
      return [
        { label: this.$t("ui.data.column.cd15MachineRollMapping.factoryCode"), prop: "factoryCode", type: "select", dictData: this.dict.type.biz_factory_name, filterable: true, change: () => this.loadMachineOptions() },
        { label: this.$t("ui.data.column.cd15MachineRollMapping.bigRollCode"), prop: "bigRollCode" },
        { label: this.$t("ui.data.column.cd15MachineRollMapping.machineCode"), prop: "machineCode", type: "select", dictData: this.machineOptions, filterable: true },
      ];
    },
  },
  methods: {
    handleAdd() { this.$refs.infoRef && this.$refs.infoRef.show(); },
    handleEdit(row) { this.$refs.infoRef && this.$refs.infoRef.show(row); },
    handleDelete(rows) {
      this.$confirm(this.$t("common.confirm.delete"), { type: "warning" }).then(() => {
        const ids = rows.map((row) => row.id).join(",");
        this.loading = true;
        removeMachineRollMapping({ ids })
          .then((data) => { this.$modal.msgSuccess(data.msg); this.$set(this.page, "current", 1); this.getList(); })
          .catch((error) => { console.log(error); this.loading = false; });
      });
    },
    handleSearch(data) { this.query = data; this.$set(this.page, "current", 1); this.loadMachineOptions(); this.getList(); },
    handlePageChange(current, pageSize) { this.$set(this.page, "current", current); this.$set(this.page, "pageSize", pageSize); this.getList(); },
    handleSortChange({ prop, order }) { this.sort = order ? { orderByColumn: prop, isAsc: order === "ascending" ? "asc" : "desc" } : {}; this.getList(); },
    handleSelectionChange(rows) { this.selection = rows; },
    handleExport() { downloadLink("/cd15/machineRollMapping/export", this.formatParams(false)); },
    formatParams(hasPage = true) {
      const params = { ...this.query, ...this.sort };
      if (hasPage) { params.pageSize = this.page.pageSize; params.pageNum = this.page.current; }
      return params;
    },
    async loadMachineOptions() {
      const res = await getCd15MachineEnableOptions({ factoryCode: this.query.factoryCode || "116" });
      const rows = Array.isArray(res) ? res : (res.rows || res.data || []);
      this.machineOptions = rows.map((item) => ({ label: item.machineCode, value: item.machineCode }));
    },
    async getList() {
      try {
        this.loading = true;
        const data = await listMachineRollMapping(this.formatParams());
        this.data = data.rows;
        this.page.total = data.total;
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
    },
  },
  created() {
    this.loadMachineOptions();
  },
  activated() {
    this.getList();
  },
};
</script>
