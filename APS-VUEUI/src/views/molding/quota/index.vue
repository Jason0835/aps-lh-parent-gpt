
<template>
  <basic-container>
    <page-table
      tableRef="cxQuotaMainTable"
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
          v-hasPermi="['cx:quota:add']"
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
          v-hasPermi="['cx:quota:import']"
          @click="$refs.tltUpload.handleImport()"
          >{{ $t("ui.frame.btn.import") }}</el-button
        >
        <el-button @click="handleExport" v-hasPermi="['cx:quota:export']">{{
          $t("ui.frame.btn.export")
        }}</el-button>
      </template>
    </page-table>
    <!-- <el-button style="display: none" ref="hidePopoverBtnRef"></el-button> -->
    <tlt-upload
      ref="tltUpload"
      downloadUrl="/cx/quota/importTemplate"
      uploadUrl="/cx/quota/importData"
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
import { listQuota, removeQuota } from "@/api/cx/quota";
//components
import tltUpload from "@/components/tltUpload/tltUpload.vue";

import infoDialog from "./components/infoDialog.vue";

export default {
  name: "Quota",
  components: {
    tltUpload,
    infoDialog,
  },
  dicts: ["CX_MACHINE_TYPE", "TIRE_TYPE"],
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
        mainPlanMonth: "",
      },
      query: {
        mainPlanMonth: "",
      },
      importDefaultValue: {},
      importRules: {},
    };
  },
  computed: {
    columns() {
      let columns = [
        { type: "selection", fixed: "left" },
        {
          prop: "machineType",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.machine.machineType"),
          // sortable: "custom",
          formatter: (row, column, value, index) => {
            return this.selectDictLabel(this.dict.type.CX_MACHINE_TYPE, value);
          },
        },
        {
          prop: "specDimension",
          align: "right",
          halign: "center",
          label: this.$t("ui.data.column.cx.limit.specDimension"),
          // sortable: "custom",
        },
        {
          prop: "carcassBothLayer",
          align: "right",
          halign: "center",
          label: this.$t("ui.data.column.cx.setting.carcassBothLayer"),
          // sortable: "custom",
        },
        {
          prop: "reinforce",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.cx.setting.reinforce"),
          // sortable: "custom",
         formatter: (row, column, value, index) => {
            if (isNaN(value)) {
              return "";
            }
            return value == "0" ? "是" : "否"; //TODO 待定
          },
        },
        {
          prop: "tireType",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.cx.setting.tireType"),
          // sortable: "custom",
         formatter: (row, column, value, index) => {
            return this.selectDictLabel(this.dict.type.TIRE_TYPE, value);
          },
        },
        {
          prop: "sectionWidthMinimum",
          align: "right",
          halign: "center",
          label: this.$t("ui.data.column.cx.setting.sectionWidthMinimum"),
          // sortable: "custom",
        },
        {
          prop: "sectionWidthMaximum",
          align: "right",
          halign: "center",
          label: this.$t("ui.data.column.cx.setting.sectionWidthMaximum"),
          // sortable: "custom",
        },
        {
          prop: "twoPersonQuota",
          align: "right",
          halign: "center",
          label: this.$t("ui.data.column.cx.setting.twoPersonQuota"),
          // sortable: "custom",
        },
        {
          prop: "onePersonQuota",
          align: "right",
          halign: "center",
          label: this.$t("ui.data.column.cx.setting.onePersonQuota"),
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
    searchColumns() {
      return [
        {
          label: this.$t("ui.data.column.machine.machineType"),
          prop: "machineType",
          type: "select", //CX_MACHINE_TYPE
          dictData: this.dict.type.CX_MACHINE_TYPE,
        },
        {
          label: this.$t("ui.data.column.cx.limit.specDimension"),
          prop: "specDimension",
        },
        {
          label: this.$t("ui.data.column.cx.setting.tireType"),
          prop: "machineCode",
          type: "select", //TIRE_TYPE
          dictData: this.dict.type.TIRE_TYPE,
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
      downloadLink("/cx/quota/export", this.formatParams(false));
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
        const data = await listQuota(this.formatParams());
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
