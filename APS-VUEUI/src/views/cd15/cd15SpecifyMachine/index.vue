<template>
  <basic-container>
    <page-table
      tableRef="cd15SpecifyMachineMainTable"
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
          v-hasPermi="['cd15:specifyMachine:add']"
          @click="handleAdd"
        >{{ $t("ui.frame.btn.add") }}</el-button>
        <el-button
          v-hasPermi="['cd15:specifyMachine:edit']"
          @click="handleBatchEdit"
          :disabled="selection.length !== 1"
        >{{ $t("ui.frame.btn.update") }}</el-button>
        <el-button
          type="danger"
          v-hasPermi="['cd15:specifyMachine:remove']"
          :disabled="selection.length === 0"
          @click="handleBatchDelete"
        >{{ $t("ui.frame.btn.delete") }}</el-button>

        <el-button
          v-hasPermi="['cd15:specifyMachine:import']"
          @click="$refs.tltUpload.handleImport()"
        >{{ $t("ui.frame.btn.import") }}</el-button>
        <el-button
          @click="handleExport"
          v-hasPermi="['cd15:specifyMachine:export']"
        >{{ $t("ui.frame.btn.export") }}</el-button>
      </template>
    </page-table>
    <tlt-upload-form
      ref="tltUpload"
      :updateSupport="true"
      downloadUrl="/cd15/specifyMachine/importTemplate"
      uploadUrl="/cd15/specifyMachine/importData"
      @uploadSuccess="getList"
      labelWidth="0"
      :columns="importColumns"
    />
    <info-dialog
      ref="infoRef"
      :machine-options="machineOptions"
      @success="getList"
    />
  </basic-container>
</template>

<script>
import {
  exportSpecifyMachine,
  listSpecifyMachine,
  listSteelStripCodes,
  removeAllSpecifyMachine,
  removeSpecifyMachine,
} from "@/api/cd15/specifyMachine";
import { getCd15MachineEnableOptions } from "@/api/cd15/cd15MachineInfo";
import TltUploadForm from "@/views/components/tltUploadForm.vue";
import infoDialog from "./components/infoDialog.vue";

export default {
  name: "Cd15SpecifyMachine",
  components: {
    TltUploadForm,
    infoDialog,
  },
  dicts: ["biz_factory_name", "JOB_TYPE"],
  provide() {
    return {
      parentDict: this.dict,
    };
  },
  data() {
    return {
      importColumns: [
        {
          label: "",
          prop: "updateSupport",
          render: (form) => {
            return (
              <el-checkbox
                label={this.$t("common.rule.updateSupport")}
                v-model={form.updateSupport}
              >
                {this.$t("common.rule.updateSupport")}
              </el-checkbox>
            );
          },
        },
      ],
      loading: false,
      data: [],
      selection: [],
      machineOptions: [],
      steelStripOptions: [],
      page: {
        current: 1,
        pageSize: 20,
        total: 0,
      },
      sort: {},
      search: {
        factoryCode: "116",
      },
      query: {
        factoryCode: "116",
      },
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
          label: this.$t("ui.data.column.cd15SpecifyMachine.factoryCode"),
          minWidth: 120,
          formatter: (row, column, value) => this.selectDictLabel(this.dict.type.biz_factory_name, value),
        },
        {
          prop: "steelStripCode",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.cd15SpecifyMachine.steelStripCode"),
          minWidth: 150,
        },
        {
          prop: "machineCode",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.cd15SpecifyMachine.machineCode"),
          minWidth: 150,
        },
        {
          prop: "jobType",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.cd15SpecifyMachine.jobType"),
          minWidth: 120,
          formatter: (row, column, value) => this.selectDictLabel(this.dict.type.JOB_TYPE, value),
        },
        {
          prop: "remark",
          halign: "center",
          label: this.$t("ui.common.column.remark"),
          minWidth: 150,
        },
        {
          align: "center",
          halign: "center",
          label: this.$t("ui.data.btn.option"),
          prop: "option",
          minWidth: 160, width: 160,
          fixed: "right",
          render: ({ row }) => {
            return (
              <div>
                <el-button
                  v-hasPermi={["cd15:specifyMachine:edit"]}
                  class="minus"
                  type="success"
                  onClick={() => this.handleEdit(row)}
                >
                  {this.$t("ui.frame.btn.update")}
                </el-button>
                <el-button
                  v-hasPermi={["cd15:specifyMachine:remove"]}
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
    searchColumns() {
      return [
        {
          label: this.$t("ui.data.column.cd15SpecifyMachine.factoryCode"),
          prop: "factoryCode",
          type: "select",
          dictData: this.dict.type.biz_factory_name,
          filterable: true,
          listeners: { change: (factoryCode) => this.loadMachineOptions(factoryCode) },
        },
        {
          label: this.$t("ui.data.column.cd15SpecifyMachine.steelStripCode"),
          prop: "steelStripCode",
          type: "select",
          dictData: this.steelStripOptions,
          filterable: true,
          clearable: true,
        },
        {
          label: this.$t("ui.data.column.cd15SpecifyMachine.machineCode"),
          prop: "machineCode",
          type: "select",
          dictData: this.machineOptions,
          filterable: true,
          clearable: true,
        },
        {
          label: this.$t("ui.data.column.cd15SpecifyMachine.jobType"),
          prop: "jobType",
          type: "select",
          dictData: this.dict.type.JOB_TYPE,
          filterable: true,
        },
      ];
    },
  },
  methods: {
    handleAdd() {
      this.$refs.infoRef && this.$refs.infoRef.show();
    },
    handleEdit(row) {
      this.$refs.infoRef && this.$refs.infoRef.show(row);
    },
    handleBatchEdit() {
      if (this.selection.length === 1) {
        this.handleEdit(this.selection[0]);
      }
    },
    handleDelete(row) {
      this.$confirm(this.$t("common.confirm.delete"), {
        type: "warning",
      }).then(() => {
        removeSpecifyMachine({ ids: row.id }).then((data) => {
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
      this.$confirm(this.$t("common.confirm.delete"), {
        type: "warning",
      }).then(() => {
        const ids = this.selection.map((item) => item.id).join(",");
        removeSpecifyMachine({ ids }).then((data) => {
          this.$modal.msgSuccess(data.msg);
          this.selection = [];
          this.$set(this.page, "current", 1);
          this.getList();
        });
      });
    },
    handleRemoveAll() {
      this.$confirm(this.$t("common.confirm.delete"), {
        type: "warning",
      }).then(() => {
        removeAllSpecifyMachine(this.query).then((data) => {
          this.$modal.msgSuccess(data.msg);
          this.selection = [];
          this.$set(this.page, "current", 1);
          this.getList();
        });
      });
    },
    handleExport() {
      exportSpecifyMachine(this.query);
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
    async loadSteelStripOptions() {
      const res = await listSteelStripCodes();
      const rows = Array.isArray(res) ? res : (res.rows || res.data || []);
      this.steelStripOptions = rows.map((code) => ({
        label: code,
        value: code,
      }));
    },
    async loadMachineOptions(factoryCode) {
      const code = factoryCode || this.query.factoryCode;
      if (!code) {
        this.machineOptions = [];
        return;
      }
      const res = await getCd15MachineEnableOptions({ factoryCode: code });
      const rows = Array.isArray(res) ? res : (res.rows || res.data || []);
      this.machineOptions = rows.map((item) => ({
        label: item.machineCode,
        value: item.machineCode,
      }));
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
        const res = await listSpecifyMachine(params);
        this.data = res.rows || [];
        this.page.total = res.total || 0;
      } finally {
        this.loading = false;
      }
    },
  },
  created() {
    this.getList();
    this.loadMachineOptions();
    this.loadSteelStripOptions();
  },
};
</script>
