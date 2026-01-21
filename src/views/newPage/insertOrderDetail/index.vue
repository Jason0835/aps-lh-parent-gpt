
<template>
  <basic-container>
    <page-table
      tableRef="cxFixedMachineMainTable"
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
        <el-tabs v-model="activeName" @tab-click="handleClick" type="card">
          <el-tab-pane label="净需求计划" name="first"> </el-tab-pane>
          <el-tab-pane label="供应链订单" name="second"> </el-tab-pane>
          <el-tab-pane label="排产计划" name="three"> </el-tab-pane>
          <el-tab-pane label="未排产计划" name="four"> </el-tab-pane>
        </el-tabs>
      </template>
    </page-table>
    <tlt-upload
      ref="tltUpload"
      downloadUrl="/mdm/productMoldingLimit/importTemplate"
      uploadUrl="/mdm/productMoldingLimit/importData"
      @uploadSuccess="getList"
    />
    <!-- <infoDialog ref="infoRef" @success="getList" /> -->
  </basic-container>
</template>
<script>
//lib
import { mapState, mapGetters } from "vuex";
//utils
import { downloadLink } from "@/utils/request";

import { listVulcanizationTable } from "@/api/monthplan/vulcanizationTable";

//components
import tltUpload from "@/components/tltUpload/tltUpload.vue";

// import infoDialog from "./components/infoDialog.vue";

export default {
  name: "insertOrderDetail",
  components: {
    tltUpload,
    // infoDialog,
  },
  dicts: ["biz_factory_name"],
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
      activeName:'first'
    };
  },
  computed: {
    ...mapGetters("globalList", ["structureList"]),
    ...mapState({
      moldingMachines: (state) => state.molding.machines,
    }),
    columns() {
      let columns = [
        // { type: "selection", fixed: "left" },
        {
          prop: "onboardDate",
          label: this.$t(
            "financialManagement.averageAccountsReceivable.updateTime"
          ),
          width: 180,
        },

        {
          prop: "structureName",
          label: this.$t("ui.data.column.finishStock.structureName"),
          width: 180,
        },

        {
          prop: "lhMachine",
          label: this.$t("ui.data.vulcanizationTable.lhMachine"),
          width: 120,
        },
        {
          prop: "cxMachine",
          label: this.$t("ui.data.vulcanizationTable.cxMachine"),
          width: 120,
        },
        {
          prop: "materialCode",
          label: this.$t("ui.data.colume.wms.unused.productCode"),
          width: 120,
        },

        {
          prop: "materialDesc",
          label: this.$t("ui.data.column.scheduleAdjust.productCodeDesc"),
          width: 320,
        },
        {
          prop: "mesMaterialCode",
          label: this.$t("ui.data.rubberMaterial.embryoDesc"),
          width: 120,
        },

        {
          prop: "mouldQty",
          label: this.$t("ui.data.vulcanizationTable.mouldQty"),
          width: 120,
        },
        {
          prop: "netDemandQty",
          label: this.$t("ui.data.vulcanizationTable.netDemandQty"),
          width: 120,
        },
        {
          prop: "onboardDate",
          label: this.$t("ui.data.column.monthplan.boardingDate"),
          width: 120,
        },
        {
          prop: "unqualifiedQty",
          label: this.$t("ui.data.vulcanizationTable.unqualifiedQty"),
          width: 120,
        },
        {
          prop: "productionQty",
          label: this.$t("ui.data.vulcanizationTable.productionQty"),
          width: 120,
        },
        {
          prop: "lhMargin",
          label: this.$t("ui.data.vulcanizationTable.lhMargin"),
          width: 120,
        },
        {
          prop: "expectedCloseDay",
          label: this.$t("ui.data.vulcanizationTable.expectedCloseDay"),
          width: 120,
        },
        {
          prop: "expectedCloseDate",
          label: this.$t("ui.data.vulcanizationTable.expectedCloseDate"),
          width: 120,
        },
        {
          prop: "planCloseDate",
          label: this.$t("ui.data.vulcanizationTable.planCloseDate"),
          width: 120,
        },
        {
          prop: "diffDay",
          label: this.$t("ui.data.vulcanizationTable.diffDay"),
          width: 120,
        },
      ];

      return columns;
    },
    searchColumns() {
      return [
        {
          prop: "factoryCode",
          label: this.$t("common.factory"),
          type: "select",
          dictData: this.dict.type.biz_factory_name,
          clearable: false,
        },
        {
          prop: "yearMonth",
          label: this.$t("ui.data.column.report.proSizeSummary.yearMonth"),
          type: "date",
          dateType: "month",
          valueFormat: "yyyy-MM",
          clearable: false,
        },
        {
          prop: "structureName",
          label: this.$t("ui.data.column.finishStock.structureName"),
          type: "select",
          dictData: this.structureList,
          filterable: true,
        },
      ];
    },
  },
  methods: {
    handleClick(){

    },
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
        // removeProductMoldingLimit({ ids }).then((data) => {
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
      downloadLink("/mdm/productMoldingLimit/export", this.formatParams(false));
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
      if (params.yearMonth) {
        let arr = params.yearMonth.split("-");
        params.year = arr[0];
        params.month = arr[1];
        params.yearMonth = "";
      }
      return params;
    },
    // api
    async getList() {
      try {
        this.loading = true;

        // const data = await listVulcanizationTable(this.formatParams());
        // this.data = data.rows;
        // this.page.total = data.total;
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
    },
  },
  created() {
    const now = new Date();
    const year = now.getFullYear(); // 2024
    const month = now.getMonth() + 1; // 注意：月份从0开始，需要+1
    let defaultParams = {
      yearMonth: `${year}-${month < 10 ? "0" + month : month}`,
      factoryCode: "116",
    };
    this.search = {
      ...defaultParams,
    };
    this.query = {
      ...defaultParams,
    };
    this.getList();
  },
  activated() {
    // this.getList();
  },
};
</script>
<style lang="scss" scoped>
.more-btn {
  margin: 2px 0;
  width: 100%;
}
</style>
