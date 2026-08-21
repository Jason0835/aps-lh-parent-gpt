<template>
  <basic-container>
    <page-table
      tableRef="gsqTwiningDiscMainTable"
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
          v-hasPermi="['gsq:twiningDisc:add']"
          @click="handleAdd"
        >{{ $t("ui.frame.btn.add") }}</el-button>
        <el-button
          type="danger"
          plain
          v-hasPermi="['gsq:twiningDisc:remove']"
          @click="handleBatchDelete"
        >{{ $t("ui.frame.btn.delete") }}</el-button>
        <el-button
          v-hasPermi="['gsq:twiningDisc:import']"
          @click="$refs.tltUpload.handleImport()"
        >{{ $t("ui.frame.btn.import") }}</el-button>
        <el-button
          @click="handleExport"
          v-hasPermi="['gsq:twiningDisc:export']"
        >{{ $t("ui.frame.btn.export") }}</el-button>
      </template>
    </page-table>
    <tlt-upload
      ref="tltUpload"
      downloadUrl="/gsq/twiningDisc/importTemplate"
      uploadUrl="/gsq/twiningDisc/importData"
      @uploadSuccess="getList"
    />
    <InfoDialog ref="infoRef" @success="getList" />
  </basic-container>
</template>
<script>
import {
  listTwiningDisc,
  removeTwiningDisc,
  exportTwiningDisc,
} from "@/api/gsq/twiningDisc";
import tltUpload from "@/components/tltUpload/tltUpload.vue";
import InfoDialog from "./components/infoDialog.vue";

export default {
  name: "GsqTwiningDisc",
  dicts: ["sys_normal_disable", "biz_factory_name", "lh_precision_data_source"],
  components: {
    tltUpload,
    InfoDialog,
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
      search: {},
      query: {},
    };
  },
  computed: {
    searchColumns() {
      return [
        {
          label: this.$t("ui.data.column.gsq.twiningDisc.factoryCode"),
          prop: "factoryCode",
          type: "select",
          span: 6,
          filterable: true,
          dictData: this.dict.type.biz_factory_name,
        },
        {
          label: this.$t("ui.data.column.gsq.twiningDisc.twiningDiscCode"),
          prop: "twiningDiscCode",
          type: "input",
        },
        {
          label: this.$t("ui.data.column.gsq.twiningDisc.twiningDiscName"),
          prop: "twiningDiscName",
          type: "input",
        },
        {
          label: this.$t("ui.data.column.gsq.twiningDisc.proSize"),
          prop: "proSize",
          type: "input",
        },
        {
          label: this.$t("ui.data.column.gsq.twiningDisc.sortType"),
          prop: "sortType",
          type: "input",
        },
        {
          label: this.$t("ui.data.column.gsq.twiningDisc.status"),
          prop: "status",
          type: "select",
          filterable: true,
          dictData: this.dict.type.sys_normal_disable,
        },
        {
          label: this.$t("ui.data.column.gsq.twiningDisc.dataSource"),
          prop: "dataSource",
          type: "select",
          filterable: true,
          dictData: this.dict.type.lh_precision_data_source,
        },
      ];
    },
    columns() {
      return [
        { type: "selection", fixed: "left" },
        {
          prop: "factoryCode",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.gsq.twiningDisc.factoryCode"),
          minWidth: 100,
          formatter: (row) => {
            return this.selectDictLabel(this.dict.type.biz_factory_name, row.factoryCode) || "-";
          },
        },
        {
          prop: "twiningDiscCode",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.gsq.twiningDisc.twiningDiscCode"),
          minWidth: 140,
          formatter: (row) => {
            return row.twiningDiscCode || "-";
          },
        },
        {
          prop: "twiningDiscName",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.gsq.twiningDisc.twiningDiscName"),
          minWidth: 140,
          formatter: (row) => {
            return row.twiningDiscName || "-";
          },
        },
        {
          prop: "proSize",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.gsq.twiningDisc.proSize"),
          minWidth: 100,
        },
        {
          prop: "sortType",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.gsq.twiningDisc.sortType"),
          minWidth: 120,
          formatter: (row) => {
            return row.sortType || "-";
          },
        },
        {
          prop: "qty",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.gsq.twiningDisc.qty"),
          minWidth: 100,
        },
        {
          prop: "specCount",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.gsq.twiningDisc.specCount"),
          minWidth: 110,
        },
        {
          prop: "machineCount",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.gsq.twiningDisc.machineCount"),
          minWidth: 110,
        },
        {
          prop: "status",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.gsq.twiningDisc.status"),
          minWidth: 100,
          formatter: (row) => {
            return this.selectDictLabel(this.dict.type.sys_normal_disable, row.status) || "-";
          },
        },
        {
          prop: "dataSource",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.gsq.twiningDisc.dataSource"),
          minWidth: 100,
          formatter: (row) => {
            return this.selectDictLabel(this.dict.type.lh_precision_data_source, row.dataSource) || "-";
          },
        },
        {
          prop: "remark",
          halign: "center",
          label: this.$t("ui.common.column.remark"),
          minWidth: 120,
          formatter: (row) => {
            return row.remark || "-";
          },
        },
        {
          prop: "updateTime",
          halign: "center",
          label: this.$t("ui.data.column.gsq.twiningDisc.updateTime"),
          minWidth: 150,
        },
        {
          align: "center",
          halign: "center",
          label: this.$t("ui.data.btn.option"),
          prop: "option",
          width: 180,
          fixed: "right",
          render: ({ row }) => {
            return (
              <div>
                <el-button
                  v-hasPermi={["gsq:twiningDisc:edit"]}
                  class="minus"
                  type="primary"
                  onClick={() => this.handleEdit(row)}
                >
                  {this.$t("ui.frame.btn.modify")}
                </el-button>
                <el-button
                  v-hasPermi={["gsq:twiningDisc:remove"]}
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
        removeTwiningDisc(ids).then((data) => {
          this.$modal.msgSuccess(data.msg);
          this.$set(this.page, "current", 1);
          this.getList();
        });
      });
    },
    handleBatchDelete() {
      if (this.selection.length === 0) {
        this.$modal.msgWarning(
          this.$t("common.confirm.selectDeleteData") || "请选择需要删除的数据"
        );
        return;
      }
      this.$confirm(this.$t("common.confirm.delete"), {
        type: "warning",
      }).then(() => {
        const ids = this.selection.map((row) => row.id).join(",");
        removeTwiningDisc(ids).then((data) => {
          this.$modal.msgSuccess(data.msg);
          this.$set(this.page, "current", 1);
          this.getList();
        });
      });
    },
    handleExport() {
      this.$confirm(this.$t("ui.data.column.gsq.twiningDisc.confirm.export"), {
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
          exportTwiningDisc(params);
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
        const data = await listTwiningDisc(this.formatParams());
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
    this.getList();
  },
  created() {
    // 工厂默认越南工厂116（与其他gsq页面保持一致）
    this.search = { factoryCode: "116" };
    this.query = { factoryCode: "116" };
  },
};
</script>
