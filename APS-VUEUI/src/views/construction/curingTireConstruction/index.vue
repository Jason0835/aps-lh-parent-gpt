
<template>
  <basic-container>
    <page-table
      tableRef="CuringDispatcherLogMainTable"
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
          v-hasPermi="['cx:lhTireConstructionInfo:add']"
          @click="handleAdd"
          >{{ $t("ui.frame.btn.add") }}</el-button
        > -->
        <!-- <el-button
          type="warning"
          v-hasPermi="['cx:lhTireConstructionInfo:edit']"
          @click="handleEdit(selection[0])"
          >{{ $t("ui.frame.btn.modify") }}</el-button
        > -->
        <!-- <el-button
          v-hasPermi="['cx:lhTireConstructionInfo:import']"
          @click="$refs.tltUpload.handleImport()"
          >{{ $t("ui.frame.btn.import") }}</el-button
        > -->
        <el-button
          @click="handleExport"
          v-hasPermi="['cx:lhTireConstructionInfo:export']"
          >{{ $t("ui.frame.btn.export") }}</el-button
        >
      </template>
    </page-table>
    <!-- <el-button style="display: none" ref="hidePopoverBtnRef"></el-button> -->
    <!-- <tlt-upload
      ref="tltUpload"
      downloadUrl="/cx/badNumber/importTemplate"
      uploadUrl="/cx/badNumber/importData"
      @uploadSuccess="getList"
    /> -->
    <!-- <infoDialog ref="infoRef" @success="getList" /> -->
  </basic-container>
</template>
<script>
//lib
// import moment from "moment";
//utils
import { downloadLink } from "@/utils/request";
//interface
import { listLhTireConstructionInfo } from "@/api/lh/lhTireConstructionInfo";
//components
// import tltUpload from "@/components/tltUpload/tltUpload.vue";

// import infoDialog from "./components/infoDialog.vue";

export default {
 name: "CuringTireConstruction",
  components: {
    // tltUpload,
    // infoDialog,
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
          label: this.$t("ui.data.column.lhTireConstructionInfo.sapCode"),
          prop: "sapCode",
        },
        {
          label: this.$t("ui.data.column.lhTireConstructionInfo.embryoCode"),
          prop: "embryoCode",
        },
        {
          label: this.$t("ui.data.column.lhTireConstructionInfo.embryoVersion"),
          prop: "embryoVersion",
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
          prop: "sapCode",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.lhTireConstructionInfo.sapCode"),
          // sortable: "custom",
        },
        {
          prop: "embryoCode",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.lhTireConstructionInfo.embryoCode"),
          // sortable: "custom",
        },
        {
          prop: "embryoVersion",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.lhTireConstructionInfo.embryoVersion"),
          // sortable: "custom",
        },
        {
          prop: "specDesc",
          align: "left",
          halign: "center",
          label: this.$t("ui.data.column.cxScheduleResult.specDesc"),
          // sortable: "custom",
        },
        {
          prop: "clampingPressure",
          align: "right",
          halign: "center",
          label: this.$t(
            "ui.data.column.lhTireConstructionInfo.clampingPressure"
          ),
          // sortable: "custom",
        },
        {
          prop: "curingTime",
          align: "right",
          halign: "center",
          label: this.$t("ui.data.column.lhTireConstructionInfo.curingTime"),
          // sortable: "custom",
        },
        {
          prop: "remark",
          halign: "center",
          label: this.$t("ui.data.column.remark"),
          minWidth: 100,
          // sortable: "custom",
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
        removeQuota({ ids })
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
      downloadLink("/cx/dispatcherLog/export", this.formatParams(false));
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
        const data = await listLhTireConstructionInfo(this.formatParams());
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
