
<template>
  <basic-container>
    <page-table
      tableRef="ParamsMoldingParamsMainTable"
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
          @click="handleAdd"

          >{{ $t("ui.frame.btn.add") }}</el-button
        >
        <el-button
          type="warning"
          :disabled="selection.length == 0"
          @click="handleEdit(selection[0])"
          >{{ $t("ui.frame.btn.modify") }}</el-button
        >
        <el-button
          type="danger"
          @click="handleDeleteAll"
          :disabled="selection.length == 0"
          >{{ $t("ui.frame.btn.delete") }}</el-button
        >
        <el-button
          @click="$refs.tltUpload.handleImport()"
          >{{ $t("ui.frame.btn.import") }}</el-button
        >
        <el-button @click="handleExport">{{
          $t("ui.frame.btn.export")
        }}</el-button>
      </template>
    </page-table>
    <!-- <el-button style="display: none" ref="hidePopoverBtnRef"></el-button> -->
    <tlt-upload
      ref="tltUpload"
      downloadUrl="/cx/cxKeyProduct/importTemplate"
      uploadUrl="/cx/cxKeyProduct/importData"
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
import { listMoldingParams, removeMoldingParams } from "@/api/cx/keyProduct";
//components
import tltUpload from "@/components/tltUpload/tltUpload.vue";

import infoDialog from "./components/infoDialog.vue";

export default {
 name: "KeyProduct",
  components: {
    tltUpload,
    infoDialog,
  },
  dicts: ['biz_yes_no','biz_factory_name'],
  provide() {
    return {
      parentDict: this.dict,
    };
  },
  data() {
    return {
      searchColumns: [
        {
          label: this.$t("结构"),
          prop: "structureName",
        },
        {
          label: this.$t("胎胚代码"),
          prop: "embryoCode",
        },
        {
          label: this.$t("胎胚描述"),
          prop: "embryoDesc",
        },
        {
          label: this.$t("是否启用"),
          prop: "isActive",
          type: "select",
          dictData: this.dict.type.biz_yes_no,
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
          prop: "structureName",
          align: "center",
          label: this.$t("结构"),
          // sortable: "custom",
        },
        {
          prop: "embryoCode",
          align: "center",
          label: this.$t("胎胚代码"),
        },
        {
          prop: "embryoDesc",
          align: "center",
          label: this.$t("胎胚描述"),
        },
        {
          prop: "isActive",
          align: "center",
          label: this.$t("是否启用"),
          // sortable: "custom",
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_yes_no, value);
          },
        },
        {
          prop: "updateTime",
          align: "center",
          label: this.$t("ui.data.column.updateTime"),
          minWidth: 160,
        },

        {
          align: "center",
          align: "center",
          label: this.$t("ui.data.btn.option"),

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
        removeMoldingParams({ ids })
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
    handleDeleteAll() {
      console.log(this.selection);
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
        removeMoldingParams({ ids }).then((data) => {
          this.$modal.msgSuccess(data.msg);
          this.$set(this.page, "current", 1);
          this.getList();
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
      downloadLink("/cx/cxKeyProduct/export", this.formatParams(false));
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
        const data = await listMoldingParams(this.formatParams());
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
