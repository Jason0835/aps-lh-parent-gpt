<template>
  <basic-container>
    <page-table
      v-loading="loading"
      :calcHeight="true"
      :columns="columns"
      :data="data"
      :page="page"
      :search="search"
      :searchColumns="searchColumns"
      :selectArea="false"
      :showSummary="false"
      tableRef="tcDepthConfigMainTable"
      @pageChange="handlePageChange"
      @refresh="getList"
      @search="handleSearch"
      @sort-change="handleSortChange"
      @selection-change="handleSelectionChange"
    >
      <template slot="header">
        <el-button
          v-hasPermi="['tc:depthConfig:add']"
          type="primary"
          @click="handleAdd"
          >{{ $t("ui.frame.btn.add") }}</el-button
        >
        <el-button
          v-hasPermi="['tc:depthConfig:edit']"
          :disabled="selection.length !== 1"
          type="warning"
          @click="handleEdit(selection[0])"
          >{{ $t("ui.frame.btn.modify") }}</el-button
        >
        <el-button
          v-hasPermi="['tc:depthConfig:remove']"
          :disabled="selection.length === 0"
          type="danger"
          @click="handleDelete(selection)"
          >{{ $t("ui.frame.btn.delete") }}</el-button
        >
        <el-button
          v-hasPermi="['tc:depthConfig:export']"
          @click="handleExport"
          >{{ $t("ui.frame.btn.export") }}</el-button
        >
        <el-button
          v-hasPermi="['tc:depthConfig:import']"
          @click="$refs.tltUpload.handleImport()"
          >{{ $t("ui.frame.btn.import") }}</el-button
        >
      </template>
    </page-table>
    <tlt-upload-form
      ref="tltUpload"
      downloadUrl="/tc/depthConfig/importTemplate"
      uploadUrl="/tc/depthConfig/importData"
      @uploadSuccess="getList"
    />
    <infoDialog ref="infoRef" @success="getList" />
  </basic-container>
</template>
<script>
import {downloadLink} from "@/utils/request";
import {listDepthConfig, removeDepthConfig} from "@/api/tc/depthConfig";
import {getConfigKey} from "@/api/system/config";
import TltUploadForm from "@/views/components/tltUploadForm.vue";
import infoDialog from "./components/infoDialog.vue";

export default {
  name: "/tc/tcDepthConfig",
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
          label: this.$t("ui.tc.depthConfig.column.minMachineQty"),
          prop: "minMachineQty",
          type: "number",
        },
      ];
    },
    columns() {
      let columns = [
        { type: "selection", fixed: "left" },
        {
          prop: "factoryCode",
          halign: "center",
          label: this.$t("ui.data.column.factoryCode"),
          formatter: (row, column, value) => this.selectDictLabel(this.dict.type.biz_factory_name, value),
        },
        {
          prop: "minMachineQty",
          halign: "center",
          label: this.$t("ui.tc.depthConfig.column.minMachineQty"),
        },
        {
          prop: "maxMachineQty",
          halign: "center",
          label: this.$t("ui.tc.depthConfig.column.maxMachineQty"),
          formatter: (row, column, value) => value !== null && value !== undefined ? value : "∞",
        },
        {
          prop: "depthClassQty",
          halign: "center",
          label: this.$t("ui.tc.depthConfig.column.depthClassQty"),
        },
        {
          prop: "remark",
          halign: "center",
          label: this.$t("ui.common.column.remark"),
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
      downloadLink("/tc/depthConfig/export", this.formatParams(false));
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
