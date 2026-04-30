
<template>
  <basic-container>
    <page-table
      tableRef="ProductionMonthPlanInitMainTable"
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
          plain
          v-hasPermi="['monthplan:productionMonthPlanInit:edit']"
          @click="handleEdit(selection[0])"
          >{{ $t("ui.frame.btn.modify") }}</el-button
        > -->
        <!-- <el-button
          type="danger"
          plain
          :disabled="selection.length === 0"
          v-hasPermi="['monthplan:productionMonthPlanInit:remove']"
          @click="handleDelete(selection)"
          >{{ $t("ui.frame.btn.delete") }}</el-button
        > -->
        <!-- <el-button
          v-hasPermi="['monthplan:mpStructureAllocation:importDataStructureAllocation']"
          @click="$refs.tltUpload.handleImport()"
          >{{ $t("ui.frame.btn.import") }}</el-button
        > -->
        <el-button
          @click="handleExport"
          v-hasPermi="['monthplan:productionMonthPlanInit:export']"
          >{{ $t("ui.frame.btn.export") }}</el-button
        >
      </template>
    </page-table>
    <!-- <el-button style="display: none" ref="hidePopoverBtnRef"></el-button> -->
    <tlt-upload
      ref="tltUpload"
      downloadUrl=""
      uploadUrl="/monthplan/mpStructureAllocation/importDataStructureAllocation"
      @uploadSuccess="getList"
    />
    <!-- <infoDialog ref="infoRef" @success="getList" /> -->
  </basic-container>
</template>
<script>
//lib
import moment from "moment";
import { mapGetters } from "vuex";
//utils
import { downloadLink } from "@/utils/request";
//interface
import {
  listProductionMonthPlanInit,
  removeProductionMonthPlanInit,
  editProductionMonthPlanInit,
} from "@/api/monthplan/productionMonthPlanInit.js";
//components
import tltUpload from "@/components/tltUpload/tltUpload.vue";

// import infoDialog from "./components/infoDialog.vue";

export default {
  name: "ProductionMonthPlanInit",
  components: {
    tltUpload,
    // infoDialog,
  },
  dicts: [
    "biz_factory_name",
    "biz_stor_type",
    "biz_yes_no",
    "biz_channel_type",
    "biz_brand_type",
    "biz_construction_stage",
    "biz_plan_type",
  ],
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
    ...mapGetters("globalList", ["structureList"]),
    columns() {
      let columns = [
        // { type: "selection", fixed: "left" },
        {
          label: this.$t("common.factory"),
          prop: "factoryCode",
          width: 120,
          formatter: (row) => {
            return this.selectDictLabel(
              this.dict.type.biz_factory_name,
              row.factoryCode
            );
          },
        },
        {
          prop: "year",
          label: this.$t("common.year"),
          width: 120,
        },

        {
          prop: "month",
          label: this.$t("common.month"),
          width: 120,
        },
        {
          prop: "monthPlanVersion",
          label: this.$t("ui.data.demandPlan.monthPlanVersion"),
          width: 180,
        },
        {
          prop: "productionVersion",
          label: this.$t("ui.data.productionMonthPlanInit.productionVersion"),
          width: 180,
        },
        {
          prop: "planType",
          label: this.$t("ui.data.monthlyProductionPlan.planType"),
          with: 120,
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_plan_type, value);
          },
        },

        {
          prop: "structureName",
          label: this.$t("ui.data.column.finishStock.structureName"),
          width: 220,
        },
        {
          prop: "cxMachineCode",
          label: this.$t("ui.data.monthlyProductionPlan.cxMachineCode"),
          width: 120,
        },
        {
          prop: "maxEmbryoCodeCount",
          label: this.$t("ui.data.productionMonthPlanInit.maxEmbryoCodeCount"),
          with: 120,
        },
        {
          prop: "maxLhMachineCount",
          label: this.$t("ui.data.productionMonthPlanInit.maxLhMachineCount"),
          with: 120,
        },
        {
          prop: "minLhMachineCount",
          label: this.$t("实单最低硫化机台数"),
          with: 120,
        },
        {
          prop: "netQty",
          label: this.$t("ui.data.DemandPlan.netQty"),
          with: 120,
        },
        {
          prop: "lossQty",
          label: this.$t("ui.data.productionMonthPlanInit.lossQty"),
          with: 120,
        },
        {
          prop: "beginDay",
          label: this.$t("common.startDate"),
          width: 180,
        },
        {
          prop: "endDay",
          label: this.$t("common.endDate"),
          width: 180,
        },
        {
          prop: "allotDays",
          label: this.$t("ui.data.productionMonthPlanInit.allotDays"),
          with: 120,
        },
        {
          prop: "remark",
          label: this.$t("common.remark"),
          with: 120,
        },
      ];

      return columns;
    },
    searchColumns() {
      return [
        {
          prop: "structureName",
          label: this.$t("ui.data.column.finishStock.structureName"),
          type: "select",
          dictData: this.structureList,
          filterable: true,
        },
        {
          prop: "cxMachineCode",
          label: this.$t("ui.data.monthlyProductionPlan.cxMachineCode"),
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
    handleDelete(rows) {
      this.$confirm(this.$t("common.confirm.delete"), {
        type: "warning",
      }).then(() => {
        const ids = rows.map((row) => row.id).join(",");
        removeProductionMonthPlanInit({ ids }).then((data) => {
          this.$modal.msgSuccess(data.msg);
          this.$set(this.page, "current", 1);
          this.getList();
        });
      });
    },
    handleChangeStatus(status, row) {
      console.log(status);
      let label =
        status === "0"
          ? this.$t("ui.biz.alter.isOpen")
          : this.$t("ui.biz.alter.isStop");

      this.$confirm(label, {
        type: "warning",
      }).then(async () => {
        try {
          this.loading = true;
          const res = await editProductionMonthPlanInit({
            ...row,
            status,
          });
          this.$modal.msgSuccess(res.msg);
          this.getList();
        } catch (error) {
          this.loading = false;
        }
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

    handleExport() {
      downloadLink(
        "/monthplan/mpStructureAllocation/export",
        this.formatParams(false)
      );
    },

    handleSelectionChange(rows) {
      this.selection = rows;
    },

    // utils
    updateTableHeaderlabel() {
      //  TODO 更新表头标题
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
        const data = await listProductionMonthPlanInit(this.formatParams());
        console.log(data);
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
    console.log(this.$route.query);
    if (this.$route.query) {
      let defaultParams = {
        ...this.$route.query,
      };
      this.search = {
        ...defaultParams,
      };
      this.query = {
        ...defaultParams,
      };
    }
    this.getList();
  },
  activated() {},
};
</script>
<style lang="scss" scoped>
.more-btn {
  margin: 2px 0;
  width: 100%;
}
</style>
