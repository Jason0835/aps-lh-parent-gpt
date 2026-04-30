
<template>
  <basic-container>
    <page-table
      tableRef="bigRollRubberCarRelationMainTable"
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
          v-hasPermi="['xwyy:bigRollRubberCarRelation:add']"
          @click="handleAdd"
          >{{ $t("ui.frame.btn.add") }}</el-button
        >
        <!-- <el-button
          type="primary"
          plain
          v-hasPermi="['xwyy:bigRollRubberCarRelation:edit']"
          :disabled="selection.length === 0"
          @click="handleEdit(selection[0])"
          >{{ $t("ui.frame.btn.modify") }}</el-button
        > -->
        <el-button
          type="danger"
          plain
          v-hasPermi="['xwyy:bigRollRubberCarRelation:remove']"
          disabled="selection.length === 0"
          @click="handleDelete(selection)"
          >{{ $t("ui.frame.btn.delete") }}</el-button
        >
        <el-button
          v-hasPermi="['xwyy:bigRollRubberCarRelation:import']"
          @click="$refs.tltUpload.handleImport()"
          >{{ $t("ui.frame.btn.import") }}</el-button
        >
        <el-button
          @click="handleExport"
          v-hasPermi="['xwyy:bigRollRubberCarRelation:export']"
          >{{ $t("ui.frame.btn.export") }}</el-button
        >
      </template>
    </page-table>
    <!-- <el-button style="display: none" ref="hidePopoverBtnRef"></el-button> -->
    <tlt-upload
      ref="tltUpload"
      downloadUrl="/xwyy/bigRollRubberCarRelation/importTemplate"
      uploadUrl="/xwyy/bigRollRubberCarRelation/importData"
      @uploadSuccess="getList"
    />
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
  listBigRollRubberCarRelation,
  editBigRollRubberCarRelation,
  removeBigRollRubberCarRelation,
} from "@/api/xwyy/bigRollRubberCarRelation";
//components
import tltUpload from "@/components/tltUpload/tltUpload.vue";

import infoDialog from "./components/infoDialog.vue";

export default {
  name: "BigRollRubberCarRelation",
  components: {
    tltUpload,
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
          label: this.$t("ui.data.column.carRelation.bigRollCode"),
          prop: "bigRollCode",
        },
        {
          label: this.$t("ui.data.column.carRelation.rubberCode"),
          prop: "rubberCode",
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
          prop: "bigRollCode",
          halign: "center",
          align: "center",
          label: this.$t("ui.data.column.carRelation.bigRollCode"),
          // sortable: "custom",
        },
        {
          prop: "rubberCode",
          halign: "center",
          align: "center",
          label: this.$t("ui.data.column.carRelation.rubberCode"),
          // sortable: "custom",
        },
        {
          prop: "carNumber",
          halign: "center",
          align: "right",
          label: this.$t("ui.data.column.carRelation.carNumber"),
          // sortable: "custom",
        },
        {
          prop: "remark",
          align: "center",
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
                  v-hasPermi={["xwyy:bigRollRubberCarRelation:edit"]}
                  class="minus"
                  type="success"
                  onClick={() => this.handleEdit(row)}
                >
                  {this.$t("ui.frame.btn.update")}
                </el-button>
                <el-button
                  v-hasPermi={["xwyy:bigRollRubberCarRelation:remove"]}
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
        removeBigRollRubberCarRelation({ ids })
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
      downloadLink(
        "/xwyy/bigRollRubberCarRelation/export",
        this.formatParams(false)
      );
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
        const data = await listBigRollRubberCarRelation(this.formatParams());
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
