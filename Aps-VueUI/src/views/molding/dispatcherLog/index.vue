
<template>
  <basic-container>
    <page-table
      tableRef="cxQuotaMachineMsgMainTable"
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
          v-hasPermi="['cx:quota:add']"
          @click="handleAdd"
          >{{ $t("ui.frame.btn.add") }}</el-button
        > -->
        <!-- <el-button
          type="warning"
          v-hasPermi="['cx:machine:edit']"
          @click="handleEdit"
          >{{ $t("ui.frame.btn.modify") }}</el-button
        > -->
        <el-button
          v-hasPermi="['cx:quota:import']"
          @click="$refs.tltUpload.handleImport()"
          >{{ $t("ui.frame.btn.import") }}</el-button
        >
        <!-- <el-button
          @click="handleExport"
          v-hasPermi="['cx:quota:export']"
          >{{ $t("ui.frame.btn.export") }}</el-button
        > -->
      </template>
    </page-table>
    <!-- <el-button style="display: none" ref="hidePopoverBtnRef"></el-button> -->
    <tlt-upload
      ref="tltUpload"
      downloadUrl="/cx/badNumber/importTemplate"
      uploadUrl="/cx/badNumber/importData"
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
  name: "machine",
  components: {
    tltUpload,
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
          label: this.$t("ui.data.column.dispatcherlog.operType"),
          prop: "sapCode",
          type: "select", //DISPATCHER_OPER_TYPE
        },
        {
          label: this.$t("ui.data.column.dispatcherlog.scheduleDate"),
          prop: "scheduleDate",
          type: "date", //DISPATCHER_OPER_TYPE
          valueFormat: "yyyy-MM-dd",
        },
        {
          label: this.$t("ui.data.column.dispatcherlog.createBy"),
          prop: "createBy",
        },
        {
          label: this.$t("ui.data.column.maintenance.log.createTime"),
          prop: "createTime",
          date: "date",
          dateType: "daterange",
          valueFormat: "yyyy-MM-dd",
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
          align: "center",
          label: this.$t("ui.data.column.scheduleResult.baseInfo"),
          children: [
            {
              prop: "operType",
              label: this.$t("ui.data.column.dispatcherlog.operType"),
              valign: "middle",
              align: "center",
              halign: "center",
              sortable: "custom",
              // formatter: function (value, row, index) {
              //   return $.table.selectDictLabel(operTypeDatas, value);
              // },
            },
            {
              prop: "scheduleDate",
              label: this.$t("ui.data.column.dispatcherlog.scheduleDate"),
              valign: "middle",
              align: "center",
              halign: "center",
              sortable: "custom",
            },
            {
              prop: "sapCode",
              label: this.$t("ui.data.column.cx.dispatcherlog.sapCode"),
              valign: "middle",
              align: "center",
              halign: "center",
              sortable: "custom",
            },
            {
              prop: "embryoCode",
              label: this.$t("ui.data.column.cx.dispatcherlog.embryoCode"),
              valign: "middle",
              align: "center",
              halign: "center",
              sortable: "custom",
            },
            {
              prop: "embryoVersion",
              label: this.$t("ui.data.column.cx.dispatcherlog.embryoVersion"),
              valign: "middle",
              align: "center",
              halign: "center",
              sortable: "custom",
            },
            {
              prop: "createBy",
              label: this.$t("ui.data.column.dispatcherlog.createBy"),
              valign: "middle",
              align: "center",
              halign: "center",
              sortable: "custom",
            },
            {
              prop: "createTime",
              label: this.$t("ui.data.column.dispatcherlog.createTime"),
              valign: "middle",
              align: "center",
              halign: "center",
              sortable: "custom",
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
              sortable: "custom",
              // formatter: function (value, row, index) {
              //   if ($.common.isEmpty(value)) {
              //     return "";
              //   }
              //   return selectMachineName(cxMachineNameList, value);
              // },
            },
            {
              prop: "beforeLhMachineCode",
              label: this.$t("ui.data.column.dispatcherlog.lhMachineCode"),
              valign: "middle",
              align: "center",
              halign: "center",
              sortable: "custom",
              // formatter: function (value, row, index) {
              //   if ($.common.isEmpty(value)) {
              //     return "";
              //   }
              //   return selectMachineName(lhMachineNameList, value);
              // },
            },
            {
              prop: "beforeClass1Plan",
              label: this.$t("ui.data.column.dispatcherlog.onePlan"),
              valign: "middle",
              align: "left",
              halign: "center",
              sortable: "custom",
            },
            {
              prop: "beforeClass2Plan",
              label: this.$t("ui.data.column.dispatcherlog.twoPlan"),
              valign: "middle",
              align: "left",
              halign: "center",
              sortable: "custom",
            },
            {
              prop: "beforeClass3Plan",
              label: this.$t("ui.data.column.dispatcherlog.threePlan"),
              valign: "middle",
              align: "left",
              halign: "center",
              sortable: "custom",
            },
            {
              prop: "beforeClass4Plan",
              label: this.$t("ui.data.column.dispatcherlog.nextOnePlan"),
              valign: "middle",
              align: "left",
              halign: "center",
              sortable: "custom",
            },
            {
              prop: "beforeClass5Plan",
              label: this.$t("ui.data.column.dispatcherlog.nextTwoPlan"),
              valign: "middle",
              align: "left",
              halign: "center",
              sortable: "custom",
            },
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
              sortable: "custom",
              // formatter: function (value, row, index) {
              //   if ($.common.isEmpty(value)) {
              //     return "";
              //   }
              //   return selectMachineName(cxMachineNameList, value);
              // },
              cellStyle: function (value, row, index) {
                if (row.beforeCxMachineCode != row.afterCxMachineCode) {
                  return { css: { background: "#FF7B7B" } };
                }
                return {};
              },
            },
            {
              prop: "afterLhMachineCode",
              label: this.$t("ui.data.column.dispatcherlog.lhMachineCode"),
              valign: "middle",
              align: "center",
              halign: "center",
              sortable: "custom",
              // formatter: function (value, row, index) {
              //   if ($.common.isEmpty(value)) {
              //     return "";
              //   }
              //   return selectMachineName(lhMachineNameList, value);
              // },
              cellStyle: function (value, row, index) {
                if (row.beforeLhMachineCode != row.afterLhMachineCode) {
                  return { css: { background: "#FF7B7B" } };
                }
                return {};
              },
            },
            {
              prop: "afterClass1Plan",
              label: this.$t("ui.data.column.dispatcherlog.onePlan"),
              valign: "middle",
              align: "left",
              halign: "center",
              sortable: "custom",
              cellStyle: function (value, row, index) {
                if (row.beforeClass1Plan != row.afterClass1Plan) {
                  return { css: { background: "#FF7B7B" } };
                }
                return {};
              },
            },
            {
              prop: "afterClass2Plan",
              label: this.$t("ui.data.column.dispatcherlog.twoPlan"),
              valign: "middle",
              align: "left",
              halign: "center",
              sortable: "custom",
              cellStyle: function (value, row, index) {
                if (row.beforeClass2Plan != row.afterClass2Plan) {
                  return { css: { background: "#FF7B7B" } };
                }
                return {};
              },
            },
            {
              prop: "afterClass3Plan",
              label: this.$t("ui.data.column.dispatcherlog.threePlan"),
              valign: "middle",
              align: "left",
              halign: "center",
              sortable: "custom",
              cellStyle: function (value, row, index) {
                if (row.beforeClass3Plan != row.afterClass3Plan) {
                  return { css: { background: "#FF7B7B" } };
                }
                return {};
              },
            },
            {
              prop: "afterClass4Plan",
              label: this.$t("ui.data.column.dispatcherlog.nextOnePlan"),
              valign: "middle",
              align: "left",
              halign: "center",
              sortable: "custom",
              cellStyle: function (value, row, index) {
                if (row.beforeClass4Plan != row.afterClass4Plan) {
                  return { css: { background: "#FF7B7B" } };
                }
                return {};
              },
            },
            {
              prop: "afterClass5Plan",
              label: this.$t("ui.data.column.dispatcherlog.nextTwoPlan"),
              valign: "middle",
              align: "left",
              halign: "center",
              sortable: "custom",
              cellStyle: function (value, row, index) {
                if (row.beforeClass5Plan != row.afterClass5Plan) {
                  return { css: { background: "#FF7B7B" } };
                }
                return {};
              },
            },
          ],
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
        removeQuotaMachine({ ids })
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
      downloadLink("/cx/badNumber/export", this.formatParams(false));
    },

    // utils
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
