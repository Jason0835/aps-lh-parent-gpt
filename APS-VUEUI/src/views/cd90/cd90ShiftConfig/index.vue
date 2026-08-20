<template>
  <basic-container>
    <page-table
      tableRef="cd90ShiftConfigMainTable"
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
          v-hasPermi="['cd90:shiftConfig:add']"
          @click="handleAdd"
        >{{ $t("ui.frame.btn.add") }}</el-button>
        <el-button
          v-hasPermi="['cd90:shiftConfig:edit']"
          @click="handleBatchEdit"
          :disabled="selection.length !== 1"
        >{{ $t("ui.frame.btn.update") }}</el-button>
        <el-button
          type="danger"
          v-hasPermi="['cd90:shiftConfig:remove']"
          :disabled="selection.length === 0"
          @click="handleBatchDelete"
        >{{ $t("ui.frame.btn.delete") }}</el-button>
        <el-button
          v-hasPermi="['cd90:shiftConfig:import']"
          @click="$refs.tltUpload.handleImport()"
        >{{ $t("ui.frame.btn.import") }}</el-button>
        <el-button
          @click="handleExport"
          v-hasPermi="['cd90:shiftConfig:export']"
        >{{ $t("ui.frame.btn.export") }}</el-button>
      </template>
    </page-table>
    <tlt-upload-form
      ref="tltUpload"
      :updateSupport="true"
      downloadUrl="/cd90/cd90ShiftConfig/importTemplate"
      uploadUrl="/cd90/cd90ShiftConfig/importData"
      @uploadSuccess="getList"
      labelWidth="0"
      :columns="importColumns"
    />
    <info-dialog ref="infoRef" @success="getList" />
  </basic-container>
</template>

<script>
import { listCd90ShiftConfig, delCd90ShiftConfig, exportCd90ShiftConfig, changeCd90ShiftConfigStatus } from "@/api/cd90/cd90ShiftConfig";
import TltUploadForm from "@/views/components/tltUploadForm.vue";
import infoDialog from "./components/infoDialog.vue";

export default {
  name: "Cd90ShiftConfig",
  components: {
    TltUploadForm,
    infoDialog,
  },
  dicts: ["biz_factory_name", "biz_yes_no", "sys_enable_disable"],
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
        size: 20,
        total: 0,
      },
      search: {
        factoryCode: "116",
        shiftCode: "",
        shiftName: "",
        isActive: "",
      },
      searchColumns: [
        {
          label: this.$t("ui.data.column.cd90ShiftConfig.factoryCode"),
          prop: "factoryCode",
          type: "input",
        },
        {
          label: this.$t("ui.data.column.cd90ShiftConfig.shiftCode"),
          prop: "shiftCode",
          type: "input",
        },
        {
          label: this.$t("ui.data.column.cd90ShiftConfig.shiftName"),
          prop: "shiftName",
          type: "input",
        },
        {
          label: this.$t("ui.data.column.cd90ShiftConfig.isActive"),
          prop: "isActive",
          type: "dict",
          dictType: "sys_enable_disable",
        },
      ],
      columns: [
        {
          label: this.$t("ui.data.column.cd90ShiftConfig.factoryCode"),
          prop: "factoryCode",
          dictType: "biz_factory_name",
        },
        {
          label: this.$t("ui.data.column.cd90ShiftConfig.shiftCode"),
          prop: "shiftCode",
        },
        {
          label: this.$t("ui.data.column.cd90ShiftConfig.shiftName"),
          prop: "shiftName",
        },
        {
          label: this.$t("ui.data.column.cd90ShiftConfig.shiftOrder"),
          prop: "shiftOrder",
        },
        {
          label: this.$t("ui.data.column.cd90ShiftConfig.startTime"),
          prop: "startTime",
        },
        {
          label: this.$t("ui.data.column.cd90ShiftConfig.endTime"),
          prop: "endTime",
        },
        {
          label: this.$t("ui.data.column.cd90ShiftConfig.shiftHours"),
          prop: "shiftHours",
        },
        {
          label: this.$t("ui.data.column.cd90ShiftConfig.isCrossDay"),
          prop: "isCrossDay",
          dictType: "biz_yes_no",
        },
        {
          label: this.$t("ui.data.column.cd90ShiftConfig.scheduleDay"),
          prop: "scheduleDay",
        },
        {
          label: this.$t("ui.data.column.cd90ShiftConfig.dayShiftOrder"),
          prop: "dayShiftOrder",
        },
        {
          label: this.$t("ui.data.column.cd90ShiftConfig.classField"),
          prop: "classField",
        },
        {
          align: "center",
          label: this.$t("ui.data.column.cd90ShiftConfig.isActive"),
          prop: "isActive",
          dictType: "sys_enable_disable",
          render: (row) => {
            return (
              <el-switch
                v-model={row.isActive}
                active-value={1}
                inactive-value={0}
                onChange={() => this.handleStatusChange(row)}
              />
            );
          },
        },
        {
          label: this.$t("ui.data.column.cd90ShiftConfig.remark"),
          prop: "remark",
        },
        {
          align: "center",
          label: this.$t("ui.frame.table.action"),
          render: (row) => {
            return (
              <div>
                <el-button
                  v-hasPermi={["cd90:shiftConfig:edit"]}
                  class="minus"
                  type="success"
                  onClick={() => this.handleEdit(row)}
                >
                  {this.$t("ui.frame.btn.update")}
                </el-button>
                <el-button
                  v-hasPermi={["cd90:shiftConfig:remove"]}
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
      ],
    };
  },
  created() {
    this.getList();
  },
  methods: {
    getList() {
      this.loading = true;
      const params = {
        pageNum: this.page.current,
        pageSize: this.page.size,
        ...this.search,
      };
      listCd90ShiftConfig(params).then((res) => {
        this.data = res.rows;
        this.page.total = res.total;
        this.loading = false;
      });
    },
    handleSearch() {
      this.page.current = 1;
      this.getList();
    },
    handlePageChange(current, pageSize) {
      this.page.current = current;
      this.page.pageSize = pageSize;
      this.getList();
    },
    handleSortChange({ prop, order }) {
      this.search.orderBy = prop;
      this.search.order = order === "ascending" ? "asc" : "desc";
      this.getList();
    },
    handleSelectionChange(selection) {
      this.selection = selection;
    },
    handleAdd() {
      this.$refs.infoRef.openDialog("add");
    },
    handleBatchEdit() {
      if (this.selection.length !== 1) {
        this.$message.warning(this.$t("ui.frame.msg.selectOne"));
        return;
      }
      this.$refs.infoRef.openDialog("edit", this.selection[0]);
    },
    handleEdit(row) {
      this.$refs.infoRef.openDialog("edit", row);
    },
    handleBatchDelete() {
      if (this.selection.length === 0) {
        this.$message.warning(this.$t("ui.frame.msg.selectAtLeastOne"));
        return;
      }
      const ids = this.selection.map((item) => item.id);
      this.$confirm(this.$t("ui.frame.confirm.delete")).then(() => {
        delCd90ShiftConfig(ids).then((res) => {
          this.$message.success(this.$t("ui.frame.msg.success"));
          this.getList();
        });
      });
    },
    handleDelete(row) {
      this.$confirm(this.$t("ui.frame.confirm.delete")).then(() => {
        delCd90ShiftConfig([row.id]).then((res) => {
          this.$message.success(this.$t("ui.frame.msg.success"));
          this.getList();
        });
      });
    },
    handleExport() {
      exportCd90ShiftConfig(this.search);
    },
    handleStatusChange(row) {
      changeCd90ShiftConfigStatus({ id: row.id, isActive: row.isActive }).then((res) => {
        this.$message.success(this.$t("ui.frame.msg.success"));
      });
    },
  },
};
</script>