<template>
  <basic-container>
    <page-table
      tableRef="lhMouldChangePlanMainTable"
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
          v-hasPermi="['lh:lhMouldChangePlan:edit']"
          @click="handleAdd"
        >{{ $t("ui.frame.btn.add") }}</el-button>
<!--        <el-button-->
<!--          v-hasPermi="['lh:lhMouldChangePlan:edit']"-->
<!--          @click="handleBatchEdit"-->
<!--          :disabled="!selection.length"-->
<!--        >{{ $t("ui.frame.btn.update") }}</el-button>-->
        <el-button
          v-hasPermi="['lh:lhMouldChangePlan:remove']"
          @click="handleBatchDelete"
          :disabled="!selection.length"
        >{{ $t("ui.frame.btn.delete") }}</el-button>
        <el-button
          v-hasPermi="['lh:lhMouldChangePlan:import']"
          @click="$refs.tltUpload.handleImport()"
        >{{ $t("ui.frame.btn.import") }}</el-button>
        <el-button
          @click="handleExport"
          v-hasPermi="['lh:lhMouldChangePlan:export']"
        >{{ $t("ui.frame.btn.export") }}</el-button>
      </template>
    </page-table>
    <tlt-upload
      ref="tltUpload"
      downloadUrl="/lh/lhMouldChangePlan/importTemplate"
      uploadUrl="/lh/lhMouldChangePlan/importData"
      @uploadSuccess="getList"
    />
    <infoDialog ref="infoRef" @success="getList" />
  </basic-container>
</template>
<script>
import { downloadLink } from "@/utils/request";
import { listLhMouldChangePlan, removeLhMouldChangePlan } from "@/api/lh/lhMouldChangePlan";
import tltUpload from "@/components/tltUpload/tltUpload.vue";
import infoDialog from "./components/infoDialog.vue";

export default {
  name: "LhMouldChangePlan",
  components: {
    tltUpload,
    infoDialog,
  },
  dicts: ["biz_factory_name", "biz_yes_no", "CHANGE_MOULD_TYPE", "IS_RELEASE"],
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
          prop: "lhResultBatchNo",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.lhMouldChangePlan.lhResultBatchNo"),
          minWidth: 140,
        },
        {
          prop: "orderNo",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.lhMouldChangePlan.orderNo"),
          minWidth: 140,
        },
        {
          prop: "planDate",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.lhMouldChangePlan.planDate"),
          minWidth: 140,
        },
        {
          prop: "scheduleDate",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.lhMouldChangePlan.scheduleDate"),
          minWidth: 140,
        },
        {
          prop: "lhMachineCode",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.lhMouldChangePlan.lhMachineCode"),
          minWidth: 140,
        },
        {
          prop: "lhMachineName",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.lhMouldChangePlan.lhMachineName"),
          minWidth: 160,
        },
        {
          prop: "beforeMaterialCode",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.lhMouldChangePlan.beforeMaterialCode"),
          minWidth: 150,
        },
        {
          prop: "beforeMaterialDesc",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.lhMouldChangePlan.beforeMaterialDesc"),
          minWidth: 180,
        },
        {
          prop: "changeMouldType",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.lhMouldChangePlan.changeMouldType"),
          minWidth: 120,
          formatter: (row, column, value, index) => {
            return this.selectDictLabel(this.dict.type.CHANGE_MOULD_TYPE, value);
          },
        },
        {
          prop: "mouldCode",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.lhMouldChangePlan.mouldCode"),
          minWidth: 120,
        },
        {
          prop: "isRelease",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.lhMouldChangePlan.isRelease"),
          minWidth: 120,
          formatter: (row, column, value, index) => {
            return this.selectDictLabel(this.dict.type.IS_RELEASE, value);
          },
        },
        {
          prop: "mouldStatus",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.lhMouldChangePlan.mouldStatus"),
          minWidth: 140,
          formatter: (row, column, value, index) => {
            return this.selectDictLabel(this.dict.type.biz_yes_no, value);
          },
        },

        // {
        //   prop: "createByName",
        //   align: "center",
        //   halign: "center",
        //   label: this.$t("ui.data.column.createBy"),
        //   minWidth: 120,
        // },
        // {
        //   prop: "createTime",
        //   align: "center",
        //   halign: "center",
        //   label: this.$t("ui.data.column.createTime"),
        //   minWidth: 160,
        // },
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
                  v-hasPermi={["lh:lhMouldChangePlan:edit"]}
                  class="minus"
                  type="success"
                  onClick={() => this.handleEdit(row)}
                >
                  {this.$t("ui.frame.btn.update")}
                </el-button>
                <el-button
                  v-hasPermi={["lh:lhMouldChangePlan:remove"]}
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
          label: this.$t("ui.data.column.lhMouldChangePlan.lhResultBatchNo"),
          prop: "lhResultBatchNo",
        },
        {
          label: this.$t("ui.data.column.lhMouldChangePlan.orderNo"),
          prop: "orderNo",
        },
        {
          label: this.$t("ui.data.column.lhMouldChangePlan.lhMachineCode"),
          prop: "lhMachineCode",
        },
        {
          label: this.$t("ui.data.column.lhMouldChangePlan.planDate"),
          prop: "planDate",
          type: "date",
          dateType: "daterange",
          valueFormat: "yyyy-MM-dd",
          startPlaceholder: this.$t("common.startTime"),
          endPlaceholder: this.$t("common.endTime"),
        },
        {
          label: this.$t("ui.data.column.lhMouldChangePlan.scheduleDate"),
          prop: "scheduleDate",
          type: "date",
          dateType: "daterange",
          valueFormat: "yyyy-MM-dd",
          startPlaceholder: this.$t("common.startTime"),
          endPlaceholder: this.$t("common.endTime"),
        },
        {
          label: this.$t("ui.data.column.lhMouldChangePlan.mouldCode"),
          prop: "mouldCode",
        },
        {
          label: this.$t("ui.data.column.lhMouldChangePlan.isRelease"),
          prop: "isRelease",
          type: "select",
          dictData: this.dict.type.IS_RELEASE,
          filterable: true,
        },

        {
          label: this.$t("ui.data.column.factoryCode"),
          prop: "factoryCode",
          type: "select",
          dictData: this.dict.type.biz_factory_name,
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
        removeLhMouldChangePlan(ids).then((data) => {
          this.$modal.msgSuccess(data.msg);
          this.$set(this.page, "current", 1);
          this.getList();
        });
      });
    },
    handleBatchDelete() {
      if (!this.selection || this.selection.length === 0) {
        this.$modal.msgWarning(this.$t("common.tip.selectOne"));
        return;
      }
      const ids = this.selection.map(item => item.id).join(',');
      this.$confirm(this.$t("common.confirm.delete"), {
        type: "warning",
      }).then(() => {
        removeLhMouldChangePlan(ids).then((data) => {
          this.$modal.msgSuccess(data.msg);
          this.$set(this.page, "current", 1);
          this.getList();
        });
      });
    },
    handleBatchEdit() {
      // 批量编辑可根据后续需求实现
      this.$modal.msgWarning(this.$t("lhMouldChangePlan.batchEditNotOpen"));
    },
    handleSearch(data) {
      this.query = data;
      if (data.planDate && data.planDate.length === 2) {
        this.query.planDateStart = data.planDate[0];
        this.query.planDateEnd = data.planDate[1];
        delete this.query.planDate;
      } else {
        delete this.query.planDateStart;
        delete this.query.planDateEnd;
      }
      if (data.scheduleDate && data.scheduleDate.length === 2) {
        this.query.scheduleDateStart = data.scheduleDate[0];
        this.query.scheduleDateEnd = data.scheduleDate[1];
        delete this.query.scheduleDate;
      } else {
        delete this.query.scheduleDateStart;
        delete this.query.scheduleDateEnd;
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
      downloadLink("/lh/lhMouldChangePlan/export", this.formatParams(false));
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
        const data = await listLhMouldChangePlan(this.formatParams());
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
