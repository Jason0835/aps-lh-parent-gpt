<template>
  <basic-container>
    <page-table
      tableRef="djDepthConfigMainTable"
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
          v-hasPermi="['dj:depthConfig:add']"
          @click="handleAdd"
          >{{ $t("ui.frame.btn.add") }}</el-button
        >
        <el-button
          type="warning"
          v-hasPermi="['dj:depthConfig:edit']"
          @click="handleEdit(selection[0])"
          :disabled="selection.length !== 1"
          >{{ $t("ui.frame.btn.modify") }}</el-button
        >
        <el-button
          type="danger"
          v-hasPermi="['dj:depthConfig:remove']"
          @click="handleDelete(selection)"
          :disabled="selection.length === 0"
          >{{ $t("ui.frame.btn.delete") }}</el-button
        >
        <el-button
          @click="handleExport"
          v-hasPermi="['dj:depthConfig:export']"
          >{{ $t("ui.frame.btn.export") }}</el-button
        >
        <el-button
          v-hasPermi="['dj:depthConfig:import']"
          @click="() => $refs.tltUpload.handleImport(importDefaultValue)"
          >{{ $t("ui.frame.btn.import") }}</el-button
        >
      </template>
    </page-table>
    <tlt-upload-form
      ref="tltUpload"
      :title="$t('ui.dj.depthConfig.column.modalName')"
      downloadUrl="/dj/depthConfig/importTemplate"
      uploadUrl="/dj/depthConfig/importData"
      @uploadSuccess="getList"
      labelWidth="0"
      :columns="importColumns"
      :rules="importRules"
    />
    <infoDialog ref="infoRef" @success="getList" />
  </basic-container>
</template>
<script>
import { downloadLink } from "@/utils/request";
import { listDepthConfig, removeDepthConfig } from "@/api/dj/depthConfig";
import { getConfigKey } from "@/api/system/config";
import TltUploadForm from "@/views/components/tltUploadForm.vue";
import infoDialog from "./components/infoDialog.vue";

export default {
  name: "DjDepthConfig",
  components: {
    infoDialog,
    TltUploadForm,
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
        factoryCode: '',
      },
      query: {
        factoryCode: '',
      },
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
                label={this.$t("ui.checkbox.updateExistingData")}
                v-model={form.updateSupport}
              >
                {this.$t("ui.checkbox.updateExistingData")}
              </el-checkbox>
            );
          },
        },
      ],
      importRules: {},
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
          label: this.$t("ui.dj.depthConfig.column.minMachineQty"),
          prop: "minMachineQty",
          type: "input",
        },
      ];
    },
    columns() {
      let columns = [
        { type: "selection", fixed: "left" },
        {
          label: this.$t("ui.data.column.factoryCode"),
          prop: "factoryCode",
          minWidth: 100,
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_factory_name, value);
          },
        },
        {
          prop: "minMachineQty",
          align: "center",
          halign: "center",
          label: this.$t("ui.dj.depthConfig.column.minMachineQty"),
        },
        {
          prop: "maxMachineQty",
          align: "center",
          halign: "center",
          label: this.$t("ui.dj.depthConfig.column.maxMachineQty"),
          formatter: (row, column, value) => value !== null && value !== undefined ? value : '∞',
        },
        {
          prop: "depthClassQty",
          align: "center",
          halign: "center",
          label: this.$t("ui.dj.depthConfig.column.depthClassQty"),
        },
        {
          prop: "remark",
          halign: "center",
          label: this.$t("ui.common.column.remark"),
          minWidth: 100,
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
          minWidth: 160,
          width: 160,
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
  },
  methods: {
    handleAdd() {
      if (this.$refs.infoRef) {
        this.$refs.infoRef.show(null, this.query.factoryCode);
      }
    },
    handleEdit(row) {
      if (this.$refs.infoRef) {
        this.$refs.infoRef.show(row);
      }
    },
    handleDelete(row) {
      const ids = Array.isArray(row) ? row.map((item) => item.id) : [row.id];
      this.$confirm(this.$t("common.confirm.delete"), {
        type: "warning",
      }).then(() => {
        this.loading = true;
        removeDepthConfig(ids)
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
      downloadLink("/dj/depthConfig/export", this.formatParams(false));
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
        const data = await listDepthConfig(this.formatParams());
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
    getConfigKey("sys.factory.code").then(response => {
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
