<template>
  <basic-container>
    <page-table
      tableRef="tqStockShiftConfigMainTable"
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
          v-hasPermi="['tq:stockShiftConfig:add']"
          type="primary"
          @click="handleAdd"
          >{{ $t("ui.frame.btn.add") }}</el-button
        >
        <el-button
          v-hasPermi="['tq:stockShiftConfig:edit']"
          type="warning"
          @click="() => handleEdit(selection[0])"
          :disabled="selection.length != 1"
          >{{ $t("ui.frame.btn.modify") }}</el-button
        >
        <el-button
          type="danger"
          v-hasPermi="['tq:stockShiftConfig:remove']"
          @click="handleBatchDelete"
          :disabled="selection.length === 0"
          >{{ $t("ui.frame.btn.delete") }}</el-button
        >
        <el-button
          v-hasPermi="['tq:stockShiftConfig:import']"
          @click="() => $refs.tltUploadForm.handleImport(importDefaultValue)"
          >{{ $t("ui.frame.btn.import") }}</el-button
        >
        <el-button
          v-hasPermi="['tq:stockShiftConfig:export']"
          @click="handleExport"
          >{{ $t("ui.frame.btn.export") }}</el-button
        >
      </template>
    </page-table>
    <tlt-upload-form
      ref="tltUploadForm"
      title="导入胎圈备库班数配置数据"
      downloadUrl="/tq/stockShiftConfig/importTemplate"
      uploadUrl="/tq/stockShiftConfig/importData"
      @uploadSuccess="getList"
      labelWidth="0"
      :columns="importColumns"
      :rules="importRules"
    />
    <InfoDialog ref="infoDialogRef" @success="handelSuccess" />
  </basic-container>
</template>
<script>
import {
  listStockShiftConfig,
  removeStockShiftConfig,
  exportStockShiftConfig,
} from "@/api/tq/stockShiftConfig";
import InfoDialog from "./components/infoDialog.vue";
import TltUploadForm from "@/views/components/tltUploadForm.vue";
import { getConfigKey } from "@/api/system/config";

export default {
  name: "TqStockShiftConfig",
  components: { InfoDialog, TltUploadForm },
  dicts: ["biz_factory_name"],
  provide() {
    return {
      parentDict: this.dict,
    };
  },
  data() {
    return {
      importDefaultValue: {
        updateSupport: false,
      },
      importColumns: [
        {
          label: "",
          prop: "updateSupport",
          render: (form) => {
            return (
              <el-checkbox
                label="是否更新已经存在的用户数据"
                true-label={true}
                false-label={false}
                v-model={form.updateSupport}
              >
                是否更新已经存在的用户数据
              </el-checkbox>
            );
          },
        },
      ],
      importRules: {},
      loading: false,
      data: [],
      page: {
        current: 1,
        pageSize: 20,
        total: 0,
      },
      sort: {},
      search: {},
      query: {},
      selection: [],
    };
  },
  computed: {
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
          label: this.$t("ui.tq.depthConfig.column.minMachineQty"),
          prop: "minMachineQty",
          type: "number",
        },
      ];
    },
    columns() {
      return [
        { type: "selection", fixed: "left" },
        {
          label: this.$t("ui.data.column.factoryCode"),
          prop: "factoryCode",
          minWidth: 120,
          formatter: (row) => {
            return (
              this.selectDictLabel(
                this.dict.type.biz_factory_name,
                row.factoryCode
              ) || "-"
            );
          },
        },
        {
          label: this.$t("ui.tq.depthConfig.column.minMachineQty"),
          prop: "minMachineQty",
          minWidth: 120,
        },
        {
          label: this.$t("ui.tq.depthConfig.column.maxMachineQty"),
          prop: "maxMachineQty",
          minWidth: 120,
          formatter: (row) => {
            return row.maxMachineQty != null && row.maxMachineQty !== undefined
              ? row.maxMachineQty
              : "∞";
          },
        },
        {
          label: this.$t("ui.tq.depthConfig.column.depthClassQty"),
          prop: "depthClassQty",
          minWidth: 120,
        },
        {
          label: this.$t("ui.common.column.remark"),
          prop: "remark",
          minWidth: 100,
          formatter: (row) => {
            return row.remark || "-";
          },
        },
        {
          label: this.$t("ui.data.column.updateTime"),
          prop: "updateTime",
          minWidth: 150,
        },
        {
          label: this.$t("common.option"),
          prop: "option",
          width: "180px",
          render: ({ row }) => {
            return (
              <div>
                <el-button
                  v-hasPermi={["tq:stockShiftConfig:edit"]}
                  class="minus"
                  type="success"
                  onClick={() => this.handleEdit(row)}
                >
                  {this.$t("ui.frame.btn.modify")}
                </el-button>
                <el-button
                  v-hasPermi={["tq:stockShiftConfig:remove"]}
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
      if (this.$refs.infoDialogRef) {
        this.$refs.infoDialogRef.show(null, this.query.factoryCode);
      }
    },
    handleEdit(row) {
      if (this.$refs.infoDialogRef) {
        this.$refs.infoDialogRef.show(row);
      }
    },
    handleDelete(row) {
      this.$confirm(this.$t("common.confirm.delete"), {
        type: "warning",
      }).then(() => {
        const ids = row.id;
        this.loading = true;
        removeStockShiftConfig({ ids })
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
    handleBatchDelete() {
      if (this.selection.length === 0) {
        this.$modal.msgWarning(this.$t("ui.placeholder.selectTableRow"));
        return;
      }
      this.$confirm(this.$t("common.confirm.delete"), {
        type: "warning",
      }).then(() => {
        const ids = this.selection.map((row) => row.id).join(",");
        this.loading = true;
        removeStockShiftConfig({ ids })
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
    handleExport() {
      this.$confirm(
        this.$t("ui.data.column.stockShiftConfig.exportConfirm"),
        {
          type: "warning",
        }
      ).then(() => {
        try {
          this.loading = true;
          let params = this.formatParams(false);
          params = {
            ...params,
            pageSize: undefined,
            pageNum: undefined,
          };
          exportStockShiftConfig(params);
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
        const data = await listStockShiftConfig(this.formatParams());
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
    getConfigKey("sys.factory.code")
      .then((response) => {
        this.query.factoryCode = response.msg;
      })
      .catch(() => {});
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
