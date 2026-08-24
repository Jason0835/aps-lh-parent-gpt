<template>
  <basic-container>
    <page-table
      tableRef="tqMachineChuckMainTable"
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
          v-hasPermi="['tq:machineChuck:add']"
          @click="handleAdd"
          >{{ $t("ui.frame.btn.add") }}</el-button
        >
        <el-button
          type="danger"
          plain
          v-hasPermi="['tq:machineChuck:remove']"
          @click="handleBatchDelete"
          >{{ $t("ui.frame.btn.delete") }}</el-button
        >
        <el-button
          v-hasPermi="['tq:machineChuck:import']"
          @click="$refs.tltUpload.handleImport()"
          >{{ $t("ui.frame.btn.import") }}</el-button
        >
        <el-button
          @click="handleExport"
          v-hasPermi="['tq:machineChuck:export']"
          >{{ $t("ui.frame.btn.export") }}</el-button
        >
      </template>
    </page-table>
    <tlt-upload-form
      ref="tltUpload"
      :updateSupport="false"
      downloadUrl="/tq/machineChuck/importTemplate"
      uploadUrl="/tq/machineChuck/importData"
      @uploadSuccess="getList"
      labelWidth="0"
      :columns="importColumns"
    />
    <InfoDialog ref="infoRef" @success="getList" />
  </basic-container>
</template>
<script>
import {
  listMachineChuck,
  removeMachineChuck,
  exportMachineChuck,
} from "@/api/tq/machineChuck";
import { listEnabledMachines } from "@/api/tq/machine";
import TltUploadForm from "@/views/components/tltUploadForm.vue";
import InfoDialog from "./components/infoDialog.vue";

export default {
  name: "TqMachineChuck",
  components: {
    TltUploadForm,
    InfoDialog,
  },
  data() {
    return {
      loading: false,
      machineLoading: false,
      data: [],
      selection: [],
      // 导入弹窗表单列：是否更新已经存在的数据勾选框（勾选后按"机台编码+寸口编码"维度键全覆盖更新）
      importColumns: [
        {
          label: "",
          prop: "updateSupport",
          render: (form) => (
            <el-checkbox v-model={form.updateSupport}>
              {this.$t("common.rule.updateSupport")}
            </el-checkbox>
          ),
        },
      ],
      page: {
        current: 1,
        pageSize: 20,
        total: 0,
      },
      sort: {},
      search: {},
      query: {},
      machineList: [],
    };
  },
  computed: {
    searchColumns() {
      return [
        {
          label: this.$t("ui.tq.machineChuck.column.machineCode"),
          prop: "machineCode",
          type: "select",
          dictData: this.machineList,
          labelKey: "machineCode",
          valueKey: "machineCode",
          filterable: true,
        },
      ];
    },
    columns() {
      return [
        { type: "selection", fixed: "left" },
        {
          prop: "machineCode",
          align: "center",
          halign: "center",
          label: this.$t("ui.tq.machineChuck.column.machineCode"),
          minWidth: 120,
        },
        {
          prop: "chuckCode",
          align: "center",
          halign: "center",
          label: this.$t("ui.tq.machineChuck.column.chuckCode"),
          minWidth: 120,
        },
        {
          prop: "chuckName",
          align: "center",
          halign: "center",
          label: this.$t("ui.tq.machineChuck.column.chuckName"),
          minWidth: 120,
        },
        {
          prop: "inchSize",
          align: "center",
          halign: "center",
          label: this.$t("ui.tq.machineChuck.column.inchSize"),
          minWidth: 100,
        },
        {
          prop: "remark",
          halign: "center",
          label: this.$t("ui.common.column.remark"),
          minWidth: 100,
          formatter: (row) => {
            return row.remark || "-";
          },
        },
        {
          prop: "updateTime",
          halign: "center",
          label: this.$t("ui.data.column.updateTime"),
          minWidth: 150,
        },
        {
          align: "center",
          halign: "center",
          label: this.$t("ui.data.btn.option"),
          prop: "option",
          width: 160,
          fixed: "right",
          render: ({ row }) => {
            return (
              <div>
                <el-button
                  v-hasPermi={["tq:machineChuck:edit"]}
                  class="minus"
                  type="success"
                  onClick={() => this.handleEdit(row)}
                >
                  {this.$t("ui.frame.btn.update")}
                </el-button>
                <el-button
                  v-hasPermi={["tq:machineChuck:remove"]}
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
  },
  methods: {
    handleAdd() {
      if (this.$refs.infoRef) {
        this.$refs.infoRef.show();
      }
    },
    handleEdit(row) {
      if (this.$refs.infoRef) {
        this.$refs.infoRef.show(row);
      }
    },
    handleDelete(row) {
      this.$confirm(this.$t("common.confirm.delete"), {
        type: "warning",
      }).then(() => {
        const ids = row.id;
        removeMachineChuck(ids).then((data) => {
          this.$modal.msgSuccess(data.msg);
          this.$set(this.page, "current", 1);
          this.getList();
        });
      });
    },
    handleBatchDelete() {
      if (this.selection.length === 0) {
        this.$modal.msgWarning(this.$t("ui.placeholder.selectTableRow"));
        return;
      }
      this.$confirm(this.$t("common.confirm.delete"), {
        type: "warning",
      }).then(() => {
        const ids = this.selection.map((row) => row.id).join(",");
        removeMachineChuck(ids).then((data) => {
          this.$modal.msgSuccess(data.msg);
          this.$set(this.page, "current", 1);
          this.getList();
        });
      });
    },
    handleExport() {
      this.$confirm(this.$t("ui.tq.machineChuck.confirm.export"), {
        type: "warning",
      }).then(() => {
        try {
          this.loading = true;
          let params = this.formatParams(false);
          params = {
            ...params,
            pageSize: undefined,
            pageNum: undefined,
          };
          exportMachineChuck(params);
        } catch (error) {
          console.error(error);
        } finally {
          this.loading = false;
        }
      });
    },
    handleSearch(data) {
      this.query = data;
      this.$set(this.page, "current", 1);
      this.getList();
    },
    handlePageChange(current, pageSize) {
      this.$set(this.page, "current", current);
      this.$set(this.page, "pageSize", pageSize);
      this.getList();
    },
    handleSortChange({ column, prop, order }) {
      if (order) {
        this.sort = {
          orderByColumn: prop,
          isAsc: order == "ascending" ? "asc" : "desc",
        };
      } else {
        this.sort = {};
      }
      this.getList();
    },
    handleSelectionChange(rows) {
      this.selection = rows;
    },
    formatParams(hasPage = true) {
      const params = {
        ...this.query,
        ...this.sort,
      };
      if (hasPage) {
        params.pageSize = this.page.pageSize;
        params.pageNum = this.page.current;
      }
      return params;
    },
    async getList() {
      try {
        this.loading = true;
        const data = await listMachineChuck(this.formatParams());
        this.data = data.rows;
        this.page.total = data.total;
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
    },
    async loadMachineList() {
      this.machineLoading = true;
      try {
        const res = await listEnabledMachines();
        this.machineList = Array.isArray(res) ? res : (res.data || res.rows || []);
      } catch (error) {
        console.log(error);
      } finally {
        this.machineLoading = false;
      }
    },
    handleMachineFocus() {
      if (this.machineList.length === 0) {
        this.loadMachineList();
      }
    },
  },
  mounted() {
    this.getList();
    this.loadMachineList();
  },
  activated() {
    this.getList();
  },
};
</script>
<style lang="scss" scoped>
.more-btn {
  margin: 2px 0;
  width: 100%;
}
</style>
