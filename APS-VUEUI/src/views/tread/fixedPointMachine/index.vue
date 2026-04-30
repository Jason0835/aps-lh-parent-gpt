
<template>
  <basic-container>
    <page-table
      tableRef="treadFixedPointMachineMainTable"
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
          v-hasPermi="['tm:specifyMachine:add']"
          @click="handleAdd"
          >{{ $t("ui.frame.btn.add") }}</el-button
        >
        <!-- <el-button
          type="warning"
          v-hasPermi="['tm:specifyMachine:edit']"
          @click="handleEdit(selection[0])"
          >{{ $t("ui.frame.btn.modify") }}</el-button
        > -->
        <el-button
          v-hasPermi="['tm:specifyMachine:import']"
          @click="$refs.tltUpload.handleImport()"
          >{{ $t("ui.frame.btn.import") }}</el-button
        >
        <el-button
          @click="handleExport"
          v-hasPermi="['tm:specifyMachine:export']"
          >{{ $t("ui.frame.btn.export") }}</el-button
        >
      </template>
    </page-table>
    <!-- <el-button style="display: none" ref="hidePopoverBtnRef"></el-button> -->
    <tlt-upload
      ref="tltUpload"
      downloadUrl="/tm/specifyMachine/importTemplate"
      uploadUrl="/tm/specifyMachine/importData"
      @uploadSuccess="getList"
    />
    <infoDialog ref="infoRef" @success="getList" />
  </basic-container>
</template>
<script>
//lib
import { mapState } from "vuex";
//utils
import { downloadLink } from "@/utils/request";
//interface
import {
  listSpecifyMachine,
  removeSpecifyMachine,
  removeAllSpecifyMachine,
} from "@/api/tm/specifyMachine";
//components
import tltUpload from "@/components/tltUpload/tltUpload.vue";

import infoDialog from "./components/infoDialog.vue";

export default {
  name: "TreadFixedPointMachine",
  components: {
    tltUpload,
    infoDialog,
  },
  dicts: ["LINE_TYPE", "JOB_TYPE"],
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
    ...mapState({
      machines: (state) => state.tread.machines,
    }),
    columns() {
      let columns = [
        { type: "selection", fixed: "left" },
        {
          prop: "treadCode",
          align: "center",
          halign: "center",
          label: this.$t("ui.tm.specifyMachine.column.treadCode"),
          width: 150,
          // sortable: "custom",
        },
        {
          prop: "machineName",
          align: "center",
          halign: "center",
          label: this.$t("ui.specifyMachine.column.machineName"),
          width: 150,
          // sortable: "custom",
        },
        {
          prop: "lineType",
          align: "center",
          halign: "center",
          label: this.$t("ui.specifyMachine.column.lineType"),
          // sortable: "custom",
          formatter: (row, column, value, index) => {
            return this.selectDictLabel(this.dict.type.LINE_TYPE, value);
          },
        },
        {
          prop: "jobType",
          align: "center",
          halign: "center",
          label: this.$t("ui.specifyMachine.column.jobType"),
          // sortable: "custom",
          formatter: (row, column, value, index) => {
            return this.selectDictLabel(this.dict.type.JOB_TYPE, value);
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
          prop: "option",
          render: ({ row }) => {
            return (
              <div>
                <el-button
                  v-hasPermi={["tm:specifyMachine:edit"]}
                  class="minus"
                  type="success"
                  onClick={() => this.handleEdit(row)}
                >
                  {this.$t("ui.frame.btn.update")}
                </el-button>
                <el-button
                  v-hasPermi={["tm:specifyMachine:remove"]}
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
          label: this.$t("ui.tm.specifyMachine.column.treadCode"),
          prop: "treadCode",
        },
        {
          label: this.$t("ui.specifyMachine.column.machineName"),
          prop: "machineId",
          type: "select",
          dictData: this.machines,
          valueKey: "id",
          labelKey: "machineName",
        },
        {
          label: this.$t("ui.data.column.specifyMachine.lineType"),
          prop: "lineType",
          type: "select",
          dictData: this.dict.type.LINE_TYPE, // "LINE_TYPE",
        },
        {
          label: this.$t("ui.data.column.specifyMachine.jobType"),
          prop: "jobType",
          type: "select",
          dictData: this.dict.type.JOB_TYPE, // "JOB_TYPE",
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
        removeSpecifyMachine({ ids }).then((data) => {
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
      downloadLink("/tm/specifyMachine/export", this.formatParams(false));
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
        const data = await listSpecifyMachine(this.formatParams());
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
    this.$store.dispatch("tread/getMachineList");
  },
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
