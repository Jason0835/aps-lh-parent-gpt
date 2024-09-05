
<template>
  <basic-container>
    <page-table
      tableRef="cxMachineMsgMainTable"
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
          type="warning"
          v-hasPermi="['cx:machine:add']"
          @click="handleAdd"
          >{{ $t("ui.frame.btn.add") }}</el-button
        >
        <!-- <el-button
          type="warning"
          v-hasPermi="['cx:machine:edit']"
          @click="handleEdit"
          >{{ $t("ui.frame.btn.modify") }}</el-button
        > -->
        <el-button
          type="warning"
          v-hasPermi="['cx:machine:import']"
          @click="$refs.tltUpload.handleImport()"
          >{{ $t("ui.frame.btn.import") }}</el-button
        >
        <el-button
          type="primary"
          @click="handleExport"
          v-hasPermi="['cx:machine:export']"
          >{{ $t("ui.frame.btn.export") }}</el-button
        >
      </template>
    </page-table>
    <!-- <el-button style="display: none" ref="hidePopoverBtnRef"></el-button> -->
    <tlt-upload
      ref="tltUpload"
      downloadUrl="/cx/cxSpecifyMachine/importTemplate"
      uploadUrl="/cx/cxSpecifyMachine/importData"
      @uploadSuccess="getList"
    />
    <infoDialog ref="infoRef" @success="getList" />
  </basic-container>
</template>
<script>
//lib
import moment from "moment";
//utils
import { downloadLink } from "@/utils/request";
//interface
import { lisCxSpecifyMachine } from "@/api/cx/cxSpecifyMachine";
//components
import tltUpload from "@/components/tltUpload/tltUpload.vue";

import infoDialog from "./components/infoDialog.vue";

export default {
  name: "machine",
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
          label: this.$t("ui.data.column.specifyMachine.sapCode"),
          prop: "sapCode",
        },
        {
          label: this.$t("ui.data.column.specifyMachine.embryoCode"),
          prop: "embryoCode",
        },
        {
          label: this.$t("ui.data.column.specifyMachine.machineCode"),
          prop: "machineCode",
          type: "select",
        },
        {
          label: this.$t("ui.data.column.specifyMachine.lineType"),
          prop: "type",
          type: "select",
          dictData: [], // "LINE_TYPE",
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
          prop: "sapCode",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.specifyMachine.sapCode"),
          sortable: "custom",
        },
        {
          prop: "embryoCode",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.specifyMachine.embryoCode"),
          sortable: "custom",
        },
        {
          prop: "machineName",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.specifyMachine.machineCode"),
          sortable: "custom",
        },
        {
          prop: "machineCode",
          sortable: "custom",
          visible: false,
        },
        {
          prop: "lineType",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.specifyMachine.lineType"),
          sortable: "custom",
          // formatter: function (value, row, index) {
          //   return $.table.selectDictLabel(lineTypeDatas, value);
          // },
        },
        {
          prop: "jobType",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.specifyMachine.jobType"),
          sortable: "custom",
          // formatter: function (value, row, index) {
          //   return $.table.selectDictLabel(jobTypeDatas, value);
          // },
        },
        {
          prop: "remark",
          halign: "center",
          label: this.$t("ui.data.column.remark"),
          sortable: "custom",
          // formatter: function (value, row, index) {
          //   return $.table.tooltip(value);
          // },
        },
        {
          align: "center",
          halign: "center",
          label: this.$t("ui.data.btn.option"),
          // formatter: function (value, row, index) {
          //   var actions = [];
          //   actions.push(
          //     '<a class="btn btn-success btn-xs ' +
          //       editFlag +
          //       '" href="javascript:void(0)" onclick="$.operate.edit(' +
          //       row.id +
          //       "," +
          //       "450" +
          //       ')"><i class="fa fa-edit"></i>' +
          //       this.$t("ui.frame.btn.update") +
          //       "</a> "
          //   );
          //   actions.push(
          //     '<a class="btn btn-danger btn-xs ' +
          //       removeFlag +
          //       '" href="javascript:void(0)" onclick="$.operate.remove(\'' +
          //       row.id +
          //       '\')"><i class="fa fa-remove"></i>' +
          //       this.$t("ui.frame.btn.delete") +
          //       "</a>"
          //   );
          //   return actions.join("");
          // },
          render: ({ row }) => {
            return (
              <div>
                <el-button type="success" onClick={() => this.handleEdit(row)}>
                  {this.$t("ui.frame.btn.update")}
                </el-button>
                <el-button type="danger" onClick={() => this.handleDelete(row)}>
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
        console.log(ids);
        // removeArea({ ids }).then((data) => {
        //   this.$modal.msgSuccess(data.msg);
        //   this.$set(this.page, "current", 1);
        //   this.getList();
        // });
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
          orderBy: prop,
          isAsc: order == "ascending",
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
      downloadLink("/cx/cxSpecifyMachine/export", this.formatParams(false));
    },

    formatParams(hasPage = true) {
      const params = {
        ...this.query,
        params: {
          ...this.sort,
        },
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
        const data = await lisCxSpecifyMachine(this.formatParams());
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
