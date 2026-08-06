<template>
  <basic-container>
    <page-table
      tableRef="ncMachineMaintenanceMainTable"
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
          v-hasPermi="['nc:machineMaintenance:add']"
          @click="handleAdd"
          >{{ $t("ui.frame.btn.add") }}</el-button
        >
        <el-button
          v-hasPermi="['nc:machineMaintenance:import']"
          @click="$refs.tltUpload.handleImport()"
          >{{ $t("ui.frame.btn.import") }}</el-button
        >
        <el-button @click="handleExport" v-hasPermi="['nc:machineMaintenance:export']">{{
          $t("ui.frame.btn.export")
        }}</el-button>
      </template>
    </page-table>
    <tlt-upload
      ref="tltUpload"
      downloadUrl="/nc/machineMaintenance/importTemplate"
      uploadUrl="/nc/machineMaintenance/importData"
      @uploadSuccess="getList"
    />
    <infoDialog ref="infoRef" @success="getList" />
  </basic-container>
</template>
<script>
//lib
import { mapState } from "vuex";
//utils
import { downloadLink } from "@/utils/request";
//interface
import { listMachineMaintenance, delMachineMaintenance } from "@/api/nc/machineMaintenance";
import { getConfigKey } from "@/api/system/config";
//components
import tltUpload from "@/components/tltUpload/tltUpload.vue";

import infoDialog from "./components/infoDialog.vue";

export default {
  name: "NcMachineMaintenance",
  components: {
    tltUpload,
    infoDialog,
  },
  dicts: ["biz_factory_name"],
  provide() {
    return {
      parentDict: this.dict,
    };
  },
  data() {
    return {
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
        factoryCode: "",
        machineCode: "",
      },
      query: {
        factoryCode: "",
        machineCode: "",
      },
      importDefaultValue: {},
      importRules: {},
    };
  },
  computed: {
    ...mapState({
      machines: (state) => state.insideLiner.machines,
    }),
    columns() {
      let columns = [
        { type: "selection", fixed: "left" },
        {
          prop: "machineCode",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.nc.machineMaintenance.machineCode"),
          sortable: true,
        },
        {
          prop: "machineName",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.nc.machineMaintenance.machineName"),
          sortable: true,
        },
        {
          prop: "stopStartTime",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.nc.machineMaintenance.stopStartTime"),
          sortable: true,
        },
        {
          prop: "stopEndTime",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.nc.machineMaintenance.stopEndTime"),
          sortable: true,
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
          minWidth: 180,
          width: 180,
          fixed: "right",
          render: ({ row }) => {
            return (
              <div>
                <el-button
                  class="minus"
                  type="success"
                  onClick={() => this.handleEdit(row)}
                >
                  {this.$t("ui.frame.btn.update")}
                </el-button>
                <el-button
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
          label: this.$t("ui.data.column.nc.machineMaintenance.machineCode"),
          prop: "machineCode",
          type: "select",
          dictData: this.machines,
          labelKey: "machineName",
          valueKey: "machineCode",
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
        this.loading = true;
        delMachineMaintenance({ ids })
          .then((data) => {
            this.$modal.msgSuccess(data.msg);
            this.$set(this.page, "current", 1);
            this.getList();
          })
          .catch((error) => {
            console.log(error);
            this.loading = false;
          });
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
    handelSuccess() {
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
      downloadLink("/nc/machineMaintenance/export", this.formatParams(false));
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

      if (params.createTime && params.createTime[0]) {
        params.createTimeStart = params.createTime[0];
        params.createTimeEnd = params.createTime[1];
        params.createTime = undefined;
      }

      return params;
    },
    async getList() {
      try {
        this.loading = true;
        const data = await listMachineMaintenance(this.formatParams());
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
    this.$store.dispatch("insideLiner/getMachineList");
    getConfigKey("sys.factory.code").then((response) => {
      this.search.factoryCode = response.msg;
      this.query.factoryCode = response.msg;
      this.getList();
    }).catch(() => {
      this.getList();
    });
  },
};
</script>
<style lang="scss" scoped>
.more-btn {
  margin: 2px 0;
  width: 100%;
}
</style>
