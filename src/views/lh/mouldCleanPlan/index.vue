<template>
  <basic-container>
    <page-table
      tableRef="mouldCleanPlanMainTable"
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
          v-hasPermi="['lh:mouldCleanPlan:edit']"
          @click="handleAdd"
        >{{ $t("ui.frame.btn.add") }}</el-button>
        <el-button
          v-hasPermi="['lh:mouldCleanPlan:import']"
          @click="$refs.tltUpload.handleImport()"
        >{{ $t("ui.frame.btn.import") }}</el-button>
        <el-button
          @click="handleExport"
          v-hasPermi="['lh:mouldCleanPlan:export']"
        >{{ $t("ui.frame.btn.export") }}</el-button>
      </template>
    </page-table>
    <tlt-upload
      ref="tltUpload"
      downloadUrl="/lh/mouldCleanPlan/importTemplate"
      uploadUrl="/lh/mouldCleanPlan/importData"
      @uploadSuccess="getList"
    />
    <infoDialog ref="infoRef" @success="getList" />
  </basic-container>
</template>

<script>
import { downloadLink } from "@/utils/request";
import { listMouldCleanPlan, removeMouldCleanPlan } from "@/api/lh/mouldCleanPlan";
import tltUpload from "@/components/tltUpload/tltUpload.vue";
import infoDialog from "./components/infoDialog.vue";

export default {
  name: "MdmMouldCleanPlan",
  components: {
    tltUpload,
    infoDialog,
  },
  dicts: ["biz_factory_name", "biz_brand_type"],
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
      search: {},
      query: {},
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
          prop: "brand",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.mouldCleanPlan.brand"),
          minWidth: 120,
          formatter: (row, column, value, index) => {
            return this.selectDictLabel(this.dict.type.biz_brand_type, value);
          },
        },
        {
          prop: "lhCode",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.mouldCleanPlan.lhCode"),
          minWidth: 150,
        },
        {
          prop: "operTime",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.mouldCleanPlan.operTime"),
          minWidth: 150,
        },
        {
          prop: "firstWashTime",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.mouldCleanPlan.firstWashTime"),
          minWidth: 150,
        },
        {
          prop: "secondWashTime",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.mouldCleanPlan.secondWashTime"),
          minWidth: 150,
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
                  v-hasPermi={["lh:mouldCleanPlan:edit"]}
                  class="minus"
                  type="success"
                  onClick={() => this.handleEdit(row)}
                >
                  {this.$t("ui.frame.btn.update")}
                </el-button>
                <el-button
                  v-hasPermi={["lh:mouldCleanPlan:remove"]}
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
          label: this.$t("ui.data.column.mouldCleanPlan.brand"),
          prop: "brand",
          type: "select",
          dictData: this.dict.type.biz_brand_type,
          filterable: true,
        },
        {
          label: this.$t("ui.data.column.mouldCleanPlan.operTime"),
          prop: "operTime",
          type: "date",
          dateType: "daterange",
          valueFormat: "yyyy-MM-dd",
          startPlaceholder: this.$t("common.startTime"),
          endPlaceholder: this.$t("common.endTime"),
        },
        {
          label: this.$t("ui.data.column.mouldCleanPlan.lhCode"),
          prop: "lhCode",
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
        removeMouldCleanPlan({ ids }).then((data) => {
          this.$modal.msgSuccess(data.msg);
          this.$set(this.page, "current", 1);
          this.getList();
        });
      });
    },
    handleSearch(data) {
      this.query = data;
      if (data.operTime && data.operTime.length === 2) {
        this.query.operTimeBegin = data.operTime[0];
        this.query.operTimeEnd = data.operTime[1];
        delete this.query.operTime;
      } else {
        delete this.query.operTimeBegin;
        delete this.query.operTimeEnd;
      }
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
      downloadLink("/lh/mouldCleanPlan/export", this.formatParams(false));
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
        const data = await listMouldCleanPlan(this.formatParams());
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
};
</script>

<style lang="scss" scoped>
</style>
