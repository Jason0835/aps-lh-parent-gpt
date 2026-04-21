<template>
  <basic-container>
    <page-table
      tableRef="lhSpecifyMachineMainTable"
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
          v-hasPermi="['lh:lhSpecifyMachine:add']"
          @click="handleAdd"
        >{{ $t("ui.frame.btn.add") }}</el-button>
        <el-button
          v-hasPermi="['lh:lhSpecifyMachine:edit']"
          @click="handleBatchEdit"
          :disabled="selection.length !== 1"
        >{{ $t("ui.frame.btn.update") }}</el-button>
        <el-button
          type="danger"
          v-hasPermi="['lh:lhSpecifyMachine:remove']"
          :disabled="selection.length == 0"
          @click="handleBatchDelete"
        >{{ $t("ui.frame.btn.delete") }}</el-button>
        <el-button
          v-hasPermi="['lh:lhSpecifyMachine:import']"
          @click="$refs.tltUpload.handleImport()"
        >{{ $t("ui.frame.btn.import") }}</el-button>
        <el-button
          @click="handleExport"
          v-hasPermi="['lh:lhSpecifyMachine:export']"
        >{{ $t("ui.frame.btn.export") }}</el-button>
      </template>
    </page-table>
    <tlt-upload-form
      ref="tltUpload"
      :updateSupport="true"
      downloadUrl="/lh/lhSpecifyMachine/importTemplate"
      uploadUrl="/lh/lhSpecifyMachine/importData"
      @uploadSuccess="getList"
      labelWidth="0"
      :columns="importColumns"
    ></tlt-upload-form>
    <infoDialog ref="infoRef" @success="getList" />
  </basic-container>
</template>
<script>
import { downloadLink } from "@/utils/request";
import { listLhSpecifyMachine, removeLhSpecifyMachine } from "@/api/lh/lhSpecifyMachine";
import TltUploadForm from "@/views/components/tltUploadForm.vue";
import infoDialog from "./components/infoDialog.vue";

export default {
  name: "LhSpecifyMachine",
  components: {
    TltUploadForm,
    infoDialog,
  },
  dicts: ["biz_factory_name", "LINE_TYPE", "JOB_TYPE"],
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
      let columns = [
        { type: "selection", fixed: "left" },
        {
          prop: "factoryCode",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.factoryCode"),
          minWidth: 120,
          formatter: (row, column, value, index) => {
            return this.selectDictLabel(this.dict.type.biz_factory_name, value);
          },
        },
        {
          prop: "specCode",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.lhSpecifyMachine.specCode"),
          minWidth: 150,
        },
        {
          prop: "materialDesc",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.lhSpecifyMachine.materialDesc"),
          minWidth: 180,
        },
        {
          prop: "machineCode",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.lhSpecifyMachine.machineCode"),
          minWidth: 150,
        },
        // {
        //   prop: "lineType",
        //   align: "center",
        //   halign: "center",
        //   label: this.$t("ui.data.column.lhSpecifyMachine.lineType"),
        //   minWidth: 120,
        //   formatter: (row, column, value, index) => {
        //     return this.selectDictLabel(this.dict.type.LINE_TYPE, value);
        //   },
        // },
        {
          prop: "jobType",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.lhSpecifyMachine.jobType"),
          minWidth: 120,
          formatter: (row, column, value, index) => {
            return this.selectDictLabel(this.dict.type.JOB_TYPE, value);
          },
        },
        {
          prop: "updateTime",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.updateTime"),
          minWidth: 160,
        },
        {
          prop: "remark",
          halign: "center",
          label: this.$t("ui.common.column.remark"),
          sortable: true,
          minWidth: 100,
        },
        {
          align: "center",
          halign: "center",
          label: this.$t("ui.data.btn.option"),
          prop: "option",
          minWidth: 150,
          render: ({ row }) => {
            return (
              <div>
                <el-button
                  v-hasPermi={["lh:lhSpecifyMachine:edit"]}
                  class="minus"
                  type="success"
                  onClick={() => this.handleEdit(row)}
                >
                  {this.$t("ui.frame.btn.update")}
                </el-button>
                <el-button
                  v-hasPermi={["lh:lhSpecifyMachine:remove"]}
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
      return columns;
    },
    searchColumns() {
      return [
        {
          label: this.$t("ui.data.column.factoryCode"),
          prop: "factoryCode",
          type: "select",
          dictData: this.dict.type.biz_factory_name,
          filterable: true,
        },
        {
          label: this.$t("ui.data.column.lhSpecifyMachine.specCode"),
          prop: "specCode",
        },
        {
          label: this.$t("ui.data.column.lhSpecifyMachine.materialDesc"),
          prop: "materialDesc",
        },
        {
          label: this.$t("ui.data.column.lhSpecifyMachine.machineCode"),
          prop: "machineCode",
        },
        // {
        //   label: this.$t("ui.data.column.lhSpecifyMachine.lineType"),
        //   prop: "lineType",
        //   type: "select",
        //   dictData: this.dict.type.LINE_TYPE,
        // },
        {
          label: this.$t("ui.data.column.lhSpecifyMachine.jobType"),
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
        removeLhSpecifyMachine({ ids }).then((data) => {
          this.$modal.msgSuccess(data.msg);
          this.$set(this.page, "current", 1);
          this.getList();
        });
      });
    },
    handleBatchEdit() {
      if (this.selection && this.selection.length === 1) {
        this.handleEdit(this.selection[0]);
      }
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
    handleExport() {
      downloadLink("/lh/lhSpecifyMachine/export", this.formatParams(false));
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
        const data = await listLhSpecifyMachine(this.formatParams());
        this.data = data.rows;
        this.page.total = data.total;
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
    },
  },
  activated() {
    this.search = {
      factoryCode: "116",
    };
    this.getList();
  },
};
</script>
<style lang="scss" scoped>
</style>
