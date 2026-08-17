<template>
  <basic-container>
    <page-table
      tableRef="cd15MachineInfoMainTable"
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
          v-hasPermi="['cd15:machineInfo:add']"
          @click="handleAdd"
        >{{ $t("ui.frame.btn.add") }}</el-button>
        <el-button
          v-hasPermi="['cd15:machineInfo:edit']"
          @click="handleBatchEdit"
          :disabled="selection.length !== 1"
        >{{ $t("ui.frame.btn.update") }}</el-button>
        <el-button
          type="danger"
          v-hasPermi="['cd15:machineInfo:remove']"
          :disabled="selection.length === 0"
          @click="handleBatchDelete"
        >{{ $t("ui.frame.btn.delete") }}</el-button>
        <el-button
          v-hasPermi="['cd15:machineInfo:import']"
          @click="$refs.tltUpload.handleImport()"
        >{{ $t("ui.frame.btn.import") }}</el-button>
        <el-button
          @click="handleExport"
          v-hasPermi="['cd15:machineInfo:export']"
        >{{ $t("ui.frame.btn.export") }}</el-button>
      </template>
    </page-table>
    <tlt-upload-form
      ref="tltUpload"
      :updateSupport="true"
      downloadUrl="/cd15/cd15MachineInfo/importTemplate"
      uploadUrl="/cd15/cd15MachineInfo/importData"
      @uploadSuccess="getList"
      labelWidth="0"
      :columns="importColumns"
    />
    <info-dialog ref="infoRef" @success="getList" />
  </basic-container>
</template>

<script>
import { listCd15MachineInfo, delCd15MachineInfo, exportCd15MachineInfo, changeCd15MachineStatus } from "@/api/cd15/cd15MachineInfo";
import TltUploadForm from "@/views/components/tltUploadForm.vue";
import infoDialog from "./components/infoDialog.vue";

export default {
  name: "Cd15MachineInfo",
  components: {
    TltUploadForm,
    infoDialog,
  },
  dicts: ["biz_factory_name", "biz_yes_no", "sys_enable_disable", "class_num_three_plan", "cd15_default_cut_mode"],
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
          label: this.$t("ui.data.column.cd15MachineInfo.factoryCode"),
          minWidth: 120,
          formatter: (row, column, value) => this.selectDictLabel(this.dict.type.biz_factory_name, value),
        },
        {
          prop: "machineCode",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.cd15MachineInfo.machineCode"),
          minWidth: 140,
        },

        {
          prop: "clothWidthMax",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.cd15MachineInfo.clothWidthMax"),
          minWidth: 150,
        },
        {
          prop: "clothWidthMin",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.cd15MachineInfo.clothWidthMin"),
          minWidth: 150,
        },
        {
          prop: "openMachineClass",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.cd15MachineInfo.openMachineClass"),
          minWidth: 130,
          render: ({ row }) => {
            let value = row.openMachineClass;
            if (this.isEmpty(value)) {
              return "";
            }
            return this.selectDictLabels(this.dict.type.class_num_three_plan, value);
          },
        },
        {
          prop: "singleCutFlag",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.cd15MachineInfo.singleCutFlag"),
          minWidth: 130,
          formatter: (row, column, value) => this.selectDictLabel(this.dict.type.biz_yes_no, value),
        },
        {
          prop: "splitCutFlag",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.cd15MachineInfo.splitCutFlag"),
          minWidth: 130,
          formatter: (row, column, value) => this.selectDictLabel(this.dict.type.biz_yes_no, value),
        },
        {
          prop: "defaultCutMode",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.cd15MachineInfo.defaultCutMode"),
          minWidth: 150,
          formatter: (row, column, value) => this.selectDictLabel(this.dict.type.cd15_default_cut_mode, value),
        },
        {
          prop: "singleShiftCapacity",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.cd15MachineInfo.singleShiftCapacity"),
          minWidth: 170,
        },
        {
          prop: "splitShiftCapacity",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.cd15MachineInfo.splitShiftCapacity"),
          minWidth: 170,
        },
        {
          prop: "status",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.cd15MachineInfo.status"),
          minWidth: 100,
          render: ({ row }) => {
            return (
              <el-switch
                value={row.status}
                active-value="1"
                inactive-value="0"
                onChange={(val) => this.handleStatusChange(row, val)}
              />
            );
          },
        },
        {
          prop: "remark",
          halign: "center",
          label: this.$t("ui.common.column.remark"),
          minWidth: 120,
        },
        {
          align: "center",
          halign: "center",
          label: this.$t("ui.data.btn.option"),
          prop: "option",
          minWidth: 150,
          fixed: "right",
          render: ({ row }) => {
            return (
              <div>
                <el-button
                  v-hasPermi={["cd15:machineInfo:edit"]}
                  class="minus"
                  type="success"
                  onClick={() => this.handleEdit(row)}
                >
                  {this.$t("ui.frame.btn.update")}
                </el-button>
                <el-button
                  v-hasPermi={["cd15:machineInfo:remove"]}
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
          label: this.$t("ui.data.column.cd15MachineInfo.factoryCode"),
          prop: "factoryCode",
          type: "select",
          dictData: this.dict.type.biz_factory_name,
          filterable: true,
        },
        {
          label: this.$t("ui.data.column.cd15MachineInfo.machineCode"),
          prop: "machineCode",
        },
        {
          label: this.$t("ui.data.column.cd15MachineInfo.status"),
          prop: "status",
          type: "select",
          dictData: this.dict.type.sys_enable_disable,
          filterable: true,
        },
      ];
    },
  },
  methods: {
    handleStatusChange(row, value) {
      changeCd15MachineStatus({ id: row.id, status: value }).then((res) => {
        this.$modal.msgSuccess(res.msg);
        this.getList();
      });
    },
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
        const ids = row.id;
        delCd15MachineInfo({ ids }).then((data) => {
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
        delCd15MachineInfo({ ids }).then((data) => {
          this.$modal.msgSuccess(data.msg);
          this.selection = [];
          this.$set(this.page, "current", 1);
          this.getList();
        });
      });
    },
    handleExport() {
      exportCd15MachineInfo(this.query);
    },
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
        const params = {
          ...this.query,
          pageNum: this.page.current,
          pageSize: this.page.pageSize,
          orderByColumn: this.sort.prop,
          isAsc: this.sort.order,
        };
        const res = await listCd15MachineInfo(params);
        this.data = res.rows || [];
        this.page.total = res.total || 0;
      } finally {
        this.loading = false;
      }
    },
  },
  created() {
    this.getList();
  },
};
</script>
