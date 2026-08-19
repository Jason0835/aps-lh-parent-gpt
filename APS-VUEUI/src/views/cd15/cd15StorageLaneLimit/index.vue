<template>
  <basic-container>
    <page-table
      tableRef="storageLaneMainTable"
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
        <el-button type="primary" plain v-hasPermi="['cd15:storageLaneLimit:add']" @click="handleAdd">{{ $t("ui.frame.btn.add") }}</el-button>
        <el-button v-hasPermi="['cd15:storageLaneLimit:edit']" @click="handleBatchEdit" :disabled="selection.length !== 1">{{ $t("ui.frame.btn.update") }}</el-button>
        <el-button type="danger" v-hasPermi="['cd15:storageLaneLimit:remove']" :disabled="selection.length === 0" @click="handleBatchDelete">{{ $t("ui.frame.btn.delete") }}</el-button>
        <el-button v-hasPermi="['cd15:storageLaneLimit:import']" @click="$refs.tltUpload.handleImport()">{{ $t("ui.frame.btn.import") }}</el-button>
        <el-button @click="handleExport" v-hasPermi="['cd15:storageLaneLimit:export']">{{ $t("ui.frame.btn.export") }}</el-button>
      </template>
    </page-table>
    <tlt-upload-form
      ref="tltUpload"
      :updateSupport="true"
      downloadUrl="/cd15/cd15StorageLaneLimit/importTemplate"
      uploadUrl="/cd15/cd15StorageLaneLimit/importData"
      @uploadSuccess="getList"
      labelWidth="0"
      :columns="importColumns"
    />
    <info-dialog
      ref="infoRef"
      :steel-strip-options="steelStripOptions"
      :machine-options="machineOptions"
      @factory-change="loadMachineOptions"
      @success="getList"
    />
  </basic-container>
</template>

<script>
import { delStorageLaneLimit, exportStorageLaneLimit, listSteelStripCodes, listStorageLaneLimit } from "@/api/cd15/storageLaneLimit";
import { getCd15MachineEnableOptions } from "@/api/cd15/cd15MachineInfo";
import TltUploadForm from "@/views/components/tltUploadForm.vue";
import infoDialog from "./components/infoDialog.vue";

const DEFAULT_FACTORY_CODE = "116";

export default {
  name: "Cd15StorageLaneLimit",
  components: { TltUploadForm, infoDialog },
  dicts: ["biz_factory_name", "class_num_three_plan"],
  provide() {
    return { parentDict: this.dict };
  },
  data() {
    return {
      importColumns: [
        {
          label: "",
          prop: "updateSupport",
          render: (form) => (
            <el-checkbox label={this.$t("common.rule.updateSupport")} v-model={form.updateSupport}>
              {this.$t("common.rule.updateSupport")}
            </el-checkbox>
          ),
        },
      ],
      loading: false,
      data: [],
      selection: [],
      steelStripOptions: [],
      machineOptions: [],
      page: { current: 1, pageSize: 20, total: 0 },
      sort: {},
      search: { factoryCode: "116", laneDate: new Date().toISOString().slice(0, 10) },
      query: { factoryCode: "116", laneDate: new Date().toISOString().slice(0, 10) },
    };
  },
  computed: {
    columns() {
      return [
        { type: "selection", fixed: "left" },
        {
          prop: "factoryCode",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.cd15StorageLaneLimit.factoryCode"),
          minWidth: 120,
          formatter: (row, column, value) => this.selectDictLabel(this.dict.type.biz_factory_name, value),
        },
        {
          prop: "laneDate",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.cd15StorageLaneLimit.laneDate"),
          minWidth: 110,
        },
        {
          prop: "machineCode",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.cd15StorageLaneLimit.machineCode"),
          minWidth: 120,
        },
        {
          prop: "materialCode",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.cd15StorageLaneLimit.materialCode"),
          minWidth: 160,
          formatter: (row, column, value) => value || this.$t("ui.data.column.cd15StorageLaneLimit.emptyLane"),
        },
        {
          prop: "shiftCode",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.cd15StorageLaneLimit.shiftCode"),
          minWidth: 90,
          formatter: (row, column, value) => this.selectDictLabel(this.dict.type.class_num_three_plan, value),
        },
        {
          prop: "storageLaneCode",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.cd15StorageLaneLimit.storageLaneCode"),
          minWidth: 130,
        },
        {
          prop: "carNum",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.cd15StorageLaneLimit.carNum"),
          minWidth: 90,
        },
        {
          prop: "maxCarNum",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.cd15StorageLaneLimit.maxCarNum"),
          minWidth: 90,
        },
        {
          prop: "remark",
          halign: "center",
          label: this.$t("ui.common.column.remark"),
          minWidth: 120,
        },
        {
          prop: "updateTime",
          align: "center",
          halign: "center",
          label: this.$t("common.updateTime"),
          minWidth: 160,
        },
        {
          align: "center",
          halign: "center",
          label: this.$t("ui.data.btn.option"),
          prop: "option",
          minWidth: 150,
          fixed: "right",
          render: ({ row }) => (
            <div>
              <el-button v-hasPermi={["cd15:storageLaneLimit:edit"]} class="minus" type="success" onClick={() => this.handleEdit(row)}>{this.$t("ui.frame.btn.update")}</el-button>
              <el-button v-hasPermi={["cd15:storageLaneLimit:remove"]} class="minus" type="danger" onClick={() => this.handleDelete(row)}>{this.$t("ui.frame.btn.delete")}</el-button>
            </div>
          ),
        },
      ];
    },
    searchColumns() {
      return [
        { label: this.$t("ui.data.column.cd15StorageLaneLimit.factoryCode"), prop: "factoryCode", type: "select", dictData: this.dict.type.biz_factory_name, filterable: true, listeners: { change: (factoryCode) => this.loadMachineOptions(factoryCode) } },
        { label: this.$t("ui.data.column.cd15StorageLaneLimit.materialCode"), prop: "materialCode", type: "select", dictData: this.steelStripOptions, filterable: true, clearable: true },
        { label: this.$t("ui.data.column.cd15StorageLaneLimit.laneDate"), prop: "laneDate", type: "date", valueFormat: "yyyy-MM-dd" },
        { label: this.$t("ui.data.column.cd15StorageLaneLimit.shiftCode"), prop: "shiftCode", type: "select", dictData: this.dict.type.class_num_three_plan, filterable: true },
        { label: this.$t("ui.data.column.cd15StorageLaneLimit.machineCode"), prop: "machineCode", type: "select", dictData: this.machineOptions, filterable: true, clearable: true },
        { label: this.$t("ui.data.column.cd15StorageLaneLimit.storageLaneCode"), prop: "storageLaneCode" },
      ];
    },
  },
  methods: {
    handleAdd() {
      this.$refs.infoRef && this.$refs.infoRef.show();
    },
    async handleEdit(row) {
      await this.loadMachineOptions(row.factoryCode);
      this.$refs.infoRef && this.$refs.infoRef.show(row);
    },
    handleBatchEdit() {
      if (this.selection.length === 1) {
        this.handleEdit(this.selection[0]);
      }
    },
    handleDelete(row) {
      this.$confirm(this.$t("common.confirm.delete"), { type: "warning" }).then(() => {
        delStorageLaneLimit({ ids: row.id }).then((data) => {
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
        delStorageLaneLimit({ ids }).then((data) => {
          this.$modal.msgSuccess(data.msg);
          this.selection = [];
          this.$set(this.page, "current", 1);
          this.getList();
        });
      });
    },
    handleExport() {
      exportStorageLaneLimit(this.query);
    },
    handleSearch(params) {
      this.page.current = 1;
      this.query = { ...params };
      this.loadMachineOptions();
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
        const params = {
          ...this.query,
          pageNum: this.page.current,
          pageSize: this.page.pageSize,
          orderByColumn: this.sort.prop,
          isAsc: this.sort.order,
        };
        const res = await listStorageLaneLimit(params);
        this.data = res.rows || [];
        this.page.total = res.total || 0;
      } finally {
        this.loading = false;
      }
    },
    async loadSteelStripOptions() {
      const res = await listSteelStripCodes();
      const rows = Array.isArray(res) ? res : (res.data || []);
      this.steelStripOptions = rows.map((code) => ({ label: code, value: code }));
    },
    async loadMachineOptions(factoryCode) {
      const code = factoryCode || this.query.factoryCode;
      if (!code) {
        this.machineOptions = [];
        return;
      }
      const res = await getCd15MachineEnableOptions({ factoryCode: code });
      const rows = Array.isArray(res) ? res : (res.data || []);
      this.machineOptions = rows.map((item) => ({ label: item.machineCode, value: item.machineCode }));
    },
  },
  created() {
    this.getList();
    this.loadSteelStripOptions();
    this.loadMachineOptions();
  },
};
</script>
