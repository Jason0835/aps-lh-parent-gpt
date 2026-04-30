
<template>
  <basic-container>
    <page-table
      tableRef="MoldingDispatcherLogMainTable"
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
      :cell-style="cellStyle"
    >
      <template slot="header">
        <!-- <el-button
          type="primary"
          v-hasPermi="['cx:dispatcherLog:add']"
          @click="handleAdd"
          >{{ $t("ui.frame.btn.add") }}</el-button
        > -->
        <!-- <el-button
          type="warning"
          v-hasPermi="['cx:dispatcherLog:edit']"
          @click="handleEdit(selection[0])"
          >{{ $t("ui.frame.btn.modify") }}</el-button
        > -->
        <!-- <el-button
          v-hasPermi="['cx:dispatcherLog:import']"
          @click="$refs.tltUpload.handleImport()"
          >{{ $t("ui.frame.btn.import") }}</el-button
        > -->
        <el-button
          @click="handleExport"
          v-hasPermi="['cx:dispatcherLog:export']"
          >{{ $t("ui.frame.btn.export") }}</el-button
        >
      </template>
    </page-table>
    <!-- <el-button style="display: none" ref="hidePopoverBtnRef"></el-button> -->
    <tlt-upload
      ref="tltUpload"
      downloadUrl="/cx/dispatcherLog/importTemplate"
      uploadUrl="/cx/dispatcherLog/importData"
      @uploadSuccess="getList"
    />
    <!-- <infoDialog ref="infoRef" @success="getList" /> -->
  </basic-container>
</template>
<script>
//lib
// import moment from "moment";
//utils
import { downloadLink } from "@/utils/request";
//interface
import { listDispatcherLog } from "@/api/cx/dispatcherLog";
//components
import tltUpload from "@/components/tltUpload/tltUpload.vue";

// import infoDialog from "./components/infoDialog.vue";

export default {
 name: "DispatcherLog",
  components: {
    tltUpload,
    // infoDialog,
  },
  dicts: ["DISPATCHER_OPER_TYPE"],
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
          align: "center",
          label: this.$t("ui.data.column.scheduleResult.baseInfo"),
          children: [
            {
              prop: "operType",
              label: this.$t("ui.data.column.dispatcherlog.operType"),
              valign: "middle",
              align: "center",
              halign: "center",
             //  sortable: "custom",
              formatter: (row, column, value, index) => {
                return this.selectDictLabel(
                  this.dict.type.DISPATCHER_OPER_TYPE,
                  value
                );
              },
            },
            {
              prop: "scheduleDate",
              label: this.$t("ui.data.column.dispatcherlog.scheduleDate"),
              valign: "middle",
              align: "center",
              halign: "center",
              width: 180,
             //  sortable: "custom",
            },
            {
              prop: "sapCode",
              label: this.$t("ui.data.column.cx.dispatcherlog.sapCode"),
              valign: "middle",
              align: "center",
              halign: "center",
             //  sortable: "custom",
            },
            {
              prop: "embryoCode",
              label: this.$t("ui.data.column.cx.dispatcherlog.embryoCode"),
              valign: "middle",
              align: "center",
              halign: "center",
             //  sortable: "custom",
            },
            {
              prop: "embryoVersion",
              label: this.$t("ui.data.column.cx.dispatcherlog.embryoVersion"),
              valign: "middle",
              align: "center",
              halign: "center",
             //  sortable: "custom",
            },
            {
              prop: "createBy",
              label: this.$t("ui.data.column.dispatcherlog.createBy"),
              valign: "middle",
              align: "center",
              halign: "center",
             //  sortable: "custom",
            },
            {
              prop: "createTime",
              label: this.$t("ui.data.column.dispatcherlog.createTime"),
              valign: "middle",
              align: "center",
              halign: "center",
              width: 180,
             //  sortable: "custom",
            },
          ],
        },
        {
          align: "center",
          label: this.$t("ui.data.column.dispatcherlog.beforeOper"),
          children: [
            {
              prop: "beforeCxMachineCode",
              label: this.$t("ui.data.column.dispatcherlog.cxMachineCode"),
              valign: "middle",
              align: "center",
              halign: "center",
             //  sortable: "custom",
              formatter: (row, column, value, index) => {
                return row.beforeCxMachineName;
              },
            },
            // {
            //   prop: "beforeLhMachineCode",
            //   label: this.$t("ui.data.column.dispatcherlog.lhMachineCode"),
            //   valign: "middle",
            //   align: "center",
            //   halign: "center",
            //  //  sortable: "custom",
            //   formatter: (row, column, value, index) => {
            //     return row.beforeLhMachineName;
            //   },
            // },
            {
              prop: "beforeClass1Plan",
              label: this.$t("ui.data.column.dispatcherlog.onePlan"),
              valign: "middle",
              align: "left",
              halign: "center",
             //  sortable: "custom",
            },
            {
              prop: "beforeClass2Plan",
              label: this.$t("ui.data.column.dispatcherlog.twoPlan"),
              valign: "middle",
              align: "left",
              halign: "center",
             //  sortable: "custom",
            },
            {
              prop: "beforeClass3Plan",
              label: this.$t("ui.data.column.dispatcherlog.threePlan"),
              valign: "middle",
              align: "left",
              halign: "center",
             //  sortable: "custom",
            },
            {
              prop: "beforeClass4Plan",
              label: this.$t("ui.data.column.dispatcherlog.nextOnePlan"),
              valign: "middle",
              align: "left",
              halign: "center",
             //  sortable: "custom",
            },
            // {
            //   prop: "beforeClass5Plan",
            //   label: this.$t("ui.data.column.dispatcherlog.nextTwoPlan"),
            //   valign: "middle",
            //   align: "left",
            //   halign: "center",
            //  //  sortable: "custom",
            // },
          ],
        },
        {
          align: "center",
          label: this.$t("ui.data.column.dispatcherlog.AfterOper"),
          children: [
            {
              prop: "afterCxMachineCode",
              label: this.$t("ui.data.column.dispatcherlog.cxMachineCode"),
              valign: "middle",
              align: "center",
              halign: "center",
             //  sortable: "custom",
              formatter: (row, column, value, index) => {
                return this.afterCxMachineName;
              },
            },
            // {
            //   prop: "afterLhMachineCode",
            //   label: this.$t("ui.data.column.dispatcherlog.lhMachineCode"),
            //   valign: "middle",
            //   align: "center",
            //   halign: "center",
            //  //  sortable: "custom",
            //   formatter: (row, column, value, index) => {
            //     return this.afterLhMachineName;
            //   },
            // },
            {
              prop: "afterClass1Plan",
              label: this.$t("ui.data.column.dispatcherlog.onePlan"),
              valign: "middle",
              align: "left",
              halign: "center",
             //  sortable: "custom",
            },
            {
              prop: "afterClass2Plan",
              label: this.$t("ui.data.column.dispatcherlog.twoPlan"),
              valign: "middle",
              align: "left",
              halign: "center",
             //  sortable: "custom",
            },
            {
              prop: "afterClass3Plan",
              label: this.$t("ui.data.column.dispatcherlog.threePlan"),
              valign: "middle",
              align: "left",
              halign: "center",
             //  sortable: "custom",
            },
            {
              prop: "afterClass4Plan",
              label: this.$t("ui.data.column.dispatcherlog.nextOnePlan"),
              valign: "middle",
              align: "left",
              halign: "center",
             //  sortable: "custom",
              cellStyle: function (value, row, index) {
                if (row.beforeClass4Plan != row.afterClass4Plan) {
                  return { css: { background: "#FF7B7B" } };
                }
                return {};
              },
            },
            // {
            //   prop: "afterClass5Plan",
            //   label: this.$t("ui.data.column.dispatcherlog.nextTwoPlan"),
            //   valign: "middle",
            //   align: "left",
            //   halign: "center",
            //  //  sortable: "custom",
            //   cellStyle: function (value, row, index) {
            //     if (row.beforeClass5Plan != row.afterClass5Plan) {
            //       return { css: { background: "#FF7B7B" } };
            //     }
            //     return {};
            //   },
            // },
          ],
        },
      ];

      return columns;
    },
    searchColumns() {
      return [
        {
          label: this.$t("ui.data.column.dispatcherlog.operType"),
          prop: "operType",
          type: "select", //DISPATCHER_OPER_TYPE
          dictData: this.dict.type.DISPATCHER_OPER_TYPE,
        },
        {
          label: this.$t("ui.data.column.dispatcherlog.scheduleDate"),
          prop: "scheduleDate",
          type: "date",
          valueFormat: "yyyy-MM-dd",
        },
        {
          label: this.$t("ui.data.column.dispatcherlog.createBy"),
          prop: "createBy",
        },
        {
          label: this.$t("ui.data.column.maintenance.log.createTime"),
          prop: "createTime",
          type: "date",
          dateType: "daterange",
          valueFormat: "yyyy-MM-dd",
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
      downloadLink("/cx/dispatcherLog/export", this.formatParams(false));
    },

    // utils
    cellStyle({ row, column, rowIndex, columnIndex }) {
      if (column.property === "afterCxMachineCode") {
        if (row.beforeCxMachineCode != row.afterCxMachineCode) {
          return { background: "#FF7B7B" };
        }
      }
      if (column.property === "afterLhMachineCode") {
        if (row.beforeLhMachineCode != row.afterLhMachineCode) {
          return { background: "#FF7B7B" };
        }
      }
      if (column.property === "afterClass1Plan") {
        if (row.beforeClass1Plan != row.afterClass1Plan) {
          return { background: "#FF7B7B" };
        }
      }
      if (column.property === "afterClass2Plan") {
        if (row.beforeClass2Plan != row.afterClass2Plan) {
          return { background: "#FF7B7B" };
        }
      }
      if (column.property === "afterClass3Plan") {
        if (row.beforeClass3Plan != row.afterClass3Plan) {
          return { background: "#FF7B7B" };
        }
      }
      if (column.property === "afterClass4Plan") {
        if (row.beforeClass4Plan != row.afterClass4Plan) {
          return { background: "#FF7B7B" };
        }
      }

      return {};
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
        params.startTime = params.createTime[0];
        params.endTime = params.createTime[1];
        params.createTime = undefined;
      }

      return params;
    },
    // api
    async getList() {
      try {
        this.loading = true;
        const data = await listDispatcherLog(this.formatParams());
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
