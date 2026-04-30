
<template>
  <basic-container>
    <page-table
      tableRef="cxShiftLimitMainTable"
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
          v-hasPermi="['cx:shiftLimit:add']"
          @click="handleAdd"
          >{{ $t("ui.frame.btn.add") }}</el-button
        >
        <!-- <el-button
          type="warning"
          v-hasPermi="['cx:machine:edit']"
          @click="handleEdit(selection[0])"
          >{{ $t("ui.frame.btn.modify") }}</el-button
        > -->
        <el-button
          type="danger"
          plain
          v-hasPermi="['cx:shiftLimit:remove']"
          @click="handleDelete(selection)"
          >{{ $t("ui.frame.btn.delete") }}</el-button
        >
        <!-- <el-button
          v-hasPermi="['cx:shiftLimit:import']"
          @click="$refs.tltUpload.handleImport()"
          >{{ $t("ui.frame.btn.import") }}</el-button
        >
        <el-button
          @click="handleExport"
          v-hasPermi="['cx:shiftLimit:export']"
          >{{ $t("ui.frame.btn.export") }}</el-button
        > -->
      </template>
    </page-table>
    <!-- <el-button style="display: none" ref="hidePopoverBtnRef"></el-button> -->
    <!-- <tlt-upload
      ref="tltUpload"
      downloadUrl="/cx/shiftLimit/importTemplate"
      uploadUrl="/cx/shiftLimit/importData"
      @uploadSuccess="getList"
    /> -->
    <infoDialog ref="infoRef" @success="getList" />
  </basic-container>
</template>
<script>
//lib
// import moment from "moment";
//utils
import { downloadLink } from "@/utils/request";
//interface
import { listShiftLimit, removeShiftLimit } from "@/api/cx/shiftLimit";
//components
// import tltUpload from "@/components/tltUpload/tltUpload.vue";

import infoDialog from "./components/infoDialog.vue";

export default {
  name: "ShiftLimit",
  components: {
    // tltUpload,
    infoDialog,
  },
  dicts: ["MACHINE_TYPE", "limit_type", "biz_stor_type"],
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
      importDefaultValue: {},
      importRules: {},
    };
  },
  computed: {
    columns() {
      let columns = [
        { type: "selection", fixed: "left" },
        {
          prop: "limitName",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.shiftLimit.limitName"),
          // sortable: "custom",
        },
        {
          prop: "type",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.shiftLimit.type"),
          // sortable: "custom",
          formatter: (row, column, value, index) => {
            return this.selectDictLabel(this.dict.type.MACHINE_TYPE, value);
          },
        },
        {
          prop: "limitType",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.shiftLimit.limitType"),
          // sortable: "custom",
          formatter: (row, column, value, index) => {
            return this.selectDictLabel(this.dict.type.limit_type, value);
          },
        },
        {
          prop: "stockNum",
          align: "right",
          halign: "center",
          label: this.$t("ui.data.column.shiftLimit.stockNum"),
          // sortable: "custom",
        },
        {
          prop: "shiftParams",
          align: "right",
          halign: "center",
          label: this.$t("ui.data.column.shiftLimit.shiftParams"),
          // sortable: "custom",
           formatter: (row,column, value) => {
            return this.selectDictLabel(
              this.dict.type.biz_stor_type,
              value
            );
          },
        },
        {
          prop: "remark",
          halign: "center",
          label: this.$t("ui.common.column.remark"),
          minWidth: 100,
          // sortable: "custom",
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
                  v-hasPermi={["cx:shiftLimit:edit"]}
                  class="minus"
                  type="success"
                  onClick={() => this.handleEdit(row)}
                >
                  {this.$t("ui.frame.btn.update")}
                </el-button>
                <el-button
                  v-hasPermi={["cx:shiftLimit:remove"]}
                  class="minus"
                  type="danger"
                  onClick={() => this.handleDelete([row])}
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
          label: this.$t("ui.data.column.shiftLimit.limitName"),
          prop: "limitName",
        },
        {
          label: this.$t("ui.data.column.shiftLimit.type"),
          prop: "type",
          type: "select", // MACHINE_TYPE
          dictData: this.dict.type.MACHINE_TYPE,
        },
        {
          label: this.$t("ui.data.column.shiftLimit.limitType"),
          prop: "limitType",
          type: "select", // limit_type
          dictData: this.dict.type.limit_type,
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
    handleDelete(rows) {
      this.$confirm(this.$t("common.confirm.delete"), {
        type: "warning",
      }).then(() => {
        const ids = rows.map((row) => row.id).join(",");
        this.loading = true;
        removeShiftLimit({ ids })
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
        //默认排序
        this.sort = {};
      }
      this.getList();
    },
    handleSelectionChange(rows) {
      this.selection = rows;
    },
    handleExport() {
      downloadLink("/cx/shiftLimit/export", this.formatParams(false));
    },

    // utils
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
    // api
    async getList() {
      try {
        this.loading = true;
        const data = await listShiftLimit(this.formatParams());
        this.data = data.rows;
        this.page.total = data.total;
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
    },
  },
  created() {},
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
