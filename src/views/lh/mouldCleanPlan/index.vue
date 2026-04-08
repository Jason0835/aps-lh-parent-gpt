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
          v-hasPermi="['lh:mouldCleanPlan:import']"
          @click="$refs.tltUpload.handleImport()"
          >{{ $t("ui.frame.btn.import") }}</el-button
        >

        <el-button
          @click="handleExport"
          v-hasPermi="['lh:mouldCleanPlan:export']"
          >{{ $t("ui.frame.btn.export") }}</el-button
        >
        
        <el-button
          type="success"
          v-hasPermi="['lh:mouldCleanPlan:sync']"
          @click="handleSyncFromWarn"
          >{{ $t("ui.mould.clean.plan.sync.from.warn") }}</el-button
        >
      </template>
    </page-table>

    <tlt-upload-form
      ref="tltUpload"
      :updateSupport="true"
      downloadUrl="/lh/mouldCleanPlan/importTemplate"
      uploadUrl="/lh/mouldCleanPlan/importData"
      @uploadSuccess="getList"
      labelWidth="0"
      :columns="importColumns"
    ></tlt-upload-form>

    <infoDialog ref="infoRef" @success="getList" />
  </basic-container>
</template>

<script>
//lib
//utils
import { downloadLink } from "@/utils/request";

import {
  listMouldCleanPlan,
  removeMouldCleanPlan,
  syncFromWarn
} from "@/api/lh/mouldCleanPlan";
import TltUploadForm from "@/views/components/tltUploadForm.vue";

//components
import infoDialog from "./components/infoDialog.vue";

export default {
  name: "MdmMouldCleanPlan",
  components: {
    infoDialog,
    TltUploadForm
  },
  dicts: ["biz_factory_name", "lh_machine", "MOULD_CLEAN_TYPE"],
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
      search: {},
      query: {},
      importDefaultValue: {},
      importRules: {},
    };
  },
  computed: {
    columns() {
      let columns = [
        { type: "selection", fixed: "left" },
        {
          prop: "factoryCode",
          label: this.$t("common.factory"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_factory_name, value);
          },
        },
        {
          prop: "lhCode",
          label: this.$t("ui.data.column.mouldCleanPlan.lhCode"),
          width: 180
        },
        {
          prop: "cleanTime",
          label: this.$t("ui.data.column.mouldCleanPlan.cleanTime"),
        },
        {
          prop: "cleanType",
          label: this.$t("ui.data.column.mouldCleanPlan.cleanType"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.MOULD_CLEAN_TYPE, value);
          },
        },
        {
          prop: "remark",
          label: this.$t("ui.data.column.mouldCleanPlan.remark"),
          showOverflowTooltip: true
        },

        {
          align: "center",
          label: this.$t("ui.data.btn.option"),
          width: 180,
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
          label: this.$t("ui.data.column.mouldCleanPlan.lhCode"),
          prop: "lhCode",
          type: "select",
          dictData: this.dict.type.lh_machine,
          filterable: true,
        },
        {
          label: this.$t("ui.data.column.mouldCleanPlan.cleanTime"),
          prop: "cleanTime",
          type: "date",
          dateType: "daterange",
          valueFormat: "yyyy-MM-dd",
        },
        {
          label: this.$t("ui.data.column.mouldCleanPlan.cleanType"),
          prop: "cleanType",
          type: "select",
          dictData: this.dict.type.MOULD_CLEAN_TYPE,
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
    handleBatchEdit() {
      if (this.selection && this.selection.length === 1) {
        this.handleEdit(this.selection[0]);
      }
    },

    handleSyncFromWarn() {
      this.$confirm(this.$t("ui.mould.clean.plan.sync.confirm"), this.$t("common.hint.hint"), {
        confirmButtonText: this.$t("common.button.confirm"),
        cancelButtonText: this.$t("common.button.cancel"),
        type: 'warning'
      }).then(async () => {
        try {
          const res = await syncFromWarn();
          this.$modal.msgSuccess(res.msg);
          this.getList();
        } catch (error) {
          console.error(error);
        }
      });
    },

    handleDeleteAll() {
      let ids = "";
      for (let i = 0; i < this.selection.length; i++) {
        if (i == this.selection.length - 1) {
          ids = ids + this.selection[i].id;
        } else {
          ids = ids + this.selection[i].id + ",";
        }
      }
      this.$confirm(this.$t("common.confirm.delete"), {
        type: "warning",
      }).then(() => {
        removeMouldCleanPlan({ ids }).then((data) => {
          this.$modal.msgSuccess(data.msg);
          this.$set(this.page, "current", 1);
          this.getList();
        });
      });
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
      this.query = { ...data };
      if (data.cleanTime && data.cleanTime.length === 2) {
        this.query.cleanTimeBegin = data.cleanTime[0];
        this.query.cleanTimeEnd = data.cleanTime[1];
      } else {
        this.query.cleanTimeBegin = undefined;
        this.query.cleanTimeEnd = undefined;
      }
      delete this.query.cleanTime;
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
        //默认排序
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
    // api
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
  created() {
    let defaultParams = {};
    this.search = {
      ...defaultParams,
    };
    this.query = {
      ...defaultParams,
    };
    this.getList();
  },
  activated() {
    // this.getList();
  },
};
</script>

<style lang="scss" scoped>
.more-btn {
  margin: 2px 0;
  width: 100%;
}
</style>
