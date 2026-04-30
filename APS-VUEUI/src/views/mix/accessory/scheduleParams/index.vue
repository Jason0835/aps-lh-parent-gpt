
<template>
  <basic-container>
    <page-table
      tableRef="fiberPressParamsMainTable"
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
        <!-- <el-button
          type="primary"
          v-hasPermi="['setting:lhflScheduleParams:add']"
          @click="handleAdd"
          >{{ $t("ui.frame.btn.add") }}</el-button
        > -->
        <!-- <el-button
          type="warning"
          v-hasPermi="['setting:lhflScheduleParams:edit']"
          @click="handleEdit(selection[0])"
          >{{ $t("ui.frame.btn.modify") }}</el-button
        > -->
        <!-- <el-button
          type="danger"
          v-hasPermi="['setting:lhflScheduleParams:remove']"
          @click="handleDelete(selection)"
          >{{ $t("ui.frame.btn.delete") }}</el-button
        > -->
        <!-- <el-button
          v-hasPermi="['setting:lhflScheduleParams:import']"
          @click="$refs.tltUpload.handleImport()"
          >{{ $t("ui.frame.btn.import") }}</el-button
        > -->
        <el-button
          @click="handleExport"
          v-hasPermi="['setting:lhflScheduleParams:export']"
          >{{ $t("ui.frame.btn.export") }}</el-button
        >
      </template>
    </page-table>
    <!-- <el-button style="display: none" ref="hidePopoverBtnRef"></el-button> -->
    <!-- <tlt-upload
      ref="tltUpload"
      downloadUrl="/setting/lhflScheduleParams/importTemplate"
      uploadUrl="/setting/lhflScheduleParams/importData"
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
import {
  listLhflScheduleParams,
  removeLhflScheduleParams,
} from "@/api/setting/lhflScheduleParams";
//components
// import tltUpload from "@/components/tltUpload/tltUpload.vue";

import infoDialog from "./components/infoDialog.vue";

export default {
  name: "rubberScheduleParams",
  components: {
    // tltUpload,
    infoDialog,
  },
  dicts: [],
  provide() {
    return {
      parentDict: this.dict,
    };
  },
  data() {
    return {
      searchColumns: [
        {
          label: this.$t("ui.data.column.paramsCode"),
          prop: "paramCode",
        },
        {
          label: this.$t("ui.data.column.paramsName"),
          prop: "paramName",
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
          prop: "mixArea",
          halign: "center",
          label: this.$t("setting.machine.mixArea"),
          // sortable: "custom",
        },
        {
          prop: "paramCode",
          halign: "center",
          label: this.$t("ui.data.column.paramsCode"),
          // sortable: "custom",
        },
        {
          prop: "paramName",
          halign: "center",
          label: this.$t("ui.data.column.paramsName"),
          titleTooltip: true,
          // sortable: "custom",
        },
        {
          prop: "paramValue",
          halign: "center",
          label: this.$t("ui.data.column.paramsValue"),
          // sortable: "custom",
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
                  class="minus"
                  type="success"
                  onClick={() => this.handleEdit(row)}
                >
                  {this.$t("ui.frame.btn.update")}
                </el-button>
                {/* <el-button
                  class="minus"
                  type="danger"
                  onClick={() => this.handleDelete(row)}
                >
                  {this.$t("ui.frame.btn.delete")}
                </el-button> */}
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
        removeLhflScheduleParams({ ids })
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
      downloadLink("/setting/lhflScheduleParams/export", this.formatParams(false));
    },

    // utils
    formatParams(hasPage = true) {
      const lhflScheduleParams = {
        ...this.query,
        lhflScheduleParams: {
          ...this.sort,
        },
      };

      if (hasPage) {
        lhflScheduleParams.pageSize = this.page.pageSize;
        lhflScheduleParams.pageNum = this.page.current;
      }

      if (lhflScheduleParams.createTime && lhflScheduleParams.createTime[0]) {
        lhflScheduleParams.createTimeStart = lhflScheduleParams.createTime[0];
        lhflScheduleParams.createTimeEnd = lhflScheduleParams.createTime[1];
        lhflScheduleParams.createTime = undefined;
      }

      return lhflScheduleParams;
    },
    // api
    async getList() {
      try {
        this.loading = true;
        const data = await listLhflScheduleParams(this.formatParams());
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
