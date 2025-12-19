
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
          v-hasPermi="['monthplan:productionMonthPlanInit:import']"
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
    <!-- <tlt-upload
      ref="tltUpload"
      downloadUrl="/demand/productionMonthPlanInit/importTemplate"
      uploadUrl="/demand/productionMonthPlanInit/importData"
      @uploadSuccess="getList"
    /> -->
    <!-- <infoDialog ref="infoRef" @success="getList" /> -->
  </basic-container>
</template>
<script>
//lib
import moment from "moment";
//utils
import { downloadLink } from "@/utils/request";
//interface
import {
  listProductionMonthPlanInit,
  removeProductionMonthPlanInit,
  editProductionMonthPlanInit,
} from "@/api/monthplan/productionMonthPlanInit.js";
//components
// import tltUpload from "@/components/tltUpload/tltUpload.vue";

// import infoDialog from "./components/infoDialog.vue";

export default {
  name: "ProductionMonthPlanInit",
  components: {
    // tltUpload,
    // infoDialog,
  },
  dicts: [
    "biz_factory_name",
    "biz_stor_type",
    "biz_yes_no",
    "biz_channel_type",
    "biz_brand_type",
    "biz_construction_stage",
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
    columns() {
      let columns = [
        // { type: "selection", fixed: "left" },
        {
          label: this.$t("ui.data.column.productionMonthPlanInit.factoryCode"),
          prop: "factoryCode",
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
        },

        {
          prop: "month",
          label: this.$t("common.month"),
        },
        {
          prop: "monthPlanVersion",
          label: this.$t(
            "ui.data.column.productionMonthPlanInit.monthPlanVersion"
          ),
          width:140,
        },
        {
          prop: "productionVersion",
          label: this.$t(
            "ui.data.column.productionMonthPlanInit.productionVersion"
          ),
          width: 140,
        },
        {
          prop: "constructionStage",
          label: this.$t(
            "ui.data.column.productionMonthPlanInit.constructionStage"
          ),
          formatter: (row, column, value) => {
            return this.selectDictLabel(
              this.dict.type.biz_construction_stage,
              value
            );
          },
        },

        {
          prop: "productCode",
          label: this.$t("ui.data.column.productionMonthPlanInit.productCode"),
        },
        {
          prop: "productDesc",
          label: this.$t("ui.data.column.productionMonthPlanInit.productDesc"),
          width: 250,
        },
        {
          prop: "locationType",
          label: this.$t("ui.data.column.productionMonthPlanInit.locationType"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_stor_type, value);
          },
        },
        {
          prop: "channel",
          label: this.$t("ui.data.column.productionMonthPlanInit.channel"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_channel_type, value);
          },
        },
        {
          prop: "brand",
          label: this.$t("ui.data.column.productionMonthPlanInit.brand"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_brand_type, value);
          },
        },
        {
          prop: "productTypeCode",
          label: this.$t(
            "ui.data.column.productionMonthPlanInit.productTypeCode"
          ),
          formatter: (row) => {
            return row.productTypeName;
          },
        },
        {
          prop: "proSize",
          label: this.$t("ui.data.column.productionMonthPlanInit.proSize"),
        },
        {
          prop: "specifications",
          label: this.$t(
            "ui.data.column.productionMonthPlanInit.specifications"
          ),
          width: 100,
        },
        {
          prop: "pattern",
          label: this.$t("ui.data.column.productionMonthPlanInit.pattern"),
          width: 140,
        },
        {
          prop: "prodReqPlan",
          label: this.$t("ui.data.column.productionMonthPlanInit.prodReqPlan"),
        },
        {
          prop: "factProdReqQty",
          label: this.$t(
            "ui.data.column.productionMonthPlanInit.factProdReqQty"
          ),
        },
        {
          prop: "isContinue",
          label: this.$t("ui.data.column.productionMonthPlanInit.isContinue"),
          align: "center",
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_yes_no, value);
          },
        },
        {
          prop: "isImportantCustom",
          label: this.$t(
            "ui.data.column.productionMonthPlanInit.isImportantCustom"
          ),
          align: "center",
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_yes_no, value);
          },
        },
        {
          prop: "isEnsurePlan",
          label: this.$t("ui.data.column.productionMonthPlanInit.isEnsurePlan"),
          align: "center",
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_yes_no, value);
          },
        },
        {
          prop: "isEmergency",
          label: this.$t("ui.data.column.productionMonthPlanInit.isEmergency"),
          align: "center",
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_yes_no, value);
          },
        },
        {
          prop: "isDebitPlan",
          label: this.$t("ui.data.column.productionMonthPlanInit.isDebitPlan"),
          align: "center",
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_yes_no, value);
          },
        },
        {
          prop: "deliveryDateDue",
          label: this.$t(
            "ui.data.column.productionMonthPlanInit.deliveryDateDue"
          ),
          width: 180,
        },
        {
          prop: "isStockUp",
          label: this.$t("ui.data.column.productionMonthPlanInit.isStockUp"),
          align: "center",
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_yes_no, value);
          },
        },
        {
          prop: "profitGrade",
          label: this.$t("ui.data.column.productionMonthPlanInit.profitGrade"),
        },
        {
          prop: "isFactoryProduction",
          label: this.$t(
            "ui.data.column.productionMonthPlanInit.isFactoryProduction"
          ),
          align: "center",
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_yes_no, value);
          },
        },
        {
          prop: "curingTime",
          label: this.$t("ui.data.column.productionMonthPlanInit.curingTime"),
        },
        {
          prop: "productionSequence",
          label: this.$t(
            "ui.data.column.productionMonthPlanInit.productionSequence"
          ),
        },
        {
          prop: "mouldQty",
          label: this.$t("ui.data.column.productionMonthPlanInit.mouldQty"),
        },
        {
          prop: "mouldFullQty",
          label: this.$t("ui.data.column.productionMonthPlanInit.mouldFullQty"),
        },
        {
          prop: "productionQty",
          label: this.$t(
            "ui.data.column.productionMonthPlanInit.productionQty"
          ),
        },
        {
          prop: "isProduction",
          label: this.$t("ui.data.column.productionMonthPlanInit.isProduction"),
          align: "center",
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_yes_no, value);
          },
        },
        {
          prop: "noProductionReason",
          label: this.$t(
            "ui.data.column.productionMonthPlanInit.noProductionReason"
          ),
        },
        {
          prop: "remark",
          label: this.$t("common.remark"),
          width: 200,
        },
        // {
        //   align: "center",
        //   halign: "center",
        //   label: this.$t("ui.data.btn.option"),
        //   render: ({ row }) => {
        //     return (
        //       <el-button
        //         class="minus"
        //         type="success"
        //         onClick={() => this.handleEdit(row)}
        //       >
        //         {this.$t("ui.frame.btn.update")}
        //       </el-button>
        //     );
        //   },
        // },
      ];

      return columns;
    },
    searchColumns() {
      return [
        {
          prop: "productCode",
          label: this.$t("ui.data.column.productionMonthPlanInit.productCode"),
        },
        {
          prop: "productDesc",
          label: this.$t("ui.data.column.productionMonthPlanInit.productDesc"),
        },
        {
          prop: "locationType",
          label: this.$t("ui.data.column.productionMonthPlanInit.locationType"),
          type: "select",
          dictData: this.dict.type.biz_stor_type,
        },
        {
          prop: "channel",
          label: this.$t("ui.data.column.productionMonthPlanInit.channel"),
          type: "select",
          dictData: this.dict.type.biz_channel_type,
        },
        {
          prop: "brand",
          label: this.$t("ui.data.column.productionMonthPlanInit.brand"),
          type: "select",
          dictData: this.dict.type.biz_brand_type,
        },
        {
          prop: "proSize",
          label: this.$t("ui.data.column.productionMonthPlanInit.proSize"),
        },
        {
          prop: "specifications",
          label: this.$t(
            "ui.data.column.productionMonthPlanInit.specifications"
          ),
        },
        {
          prop: "pattern",
          label: this.$t("ui.data.column.productionMonthPlanInit.pattern"),
        },
        {
          prop: "isProduction",
          label: this.$t("ui.data.column.productionMonthPlanInit.isProduction"),
          type: "select",
          dictData: this.dict.type.biz_yes_no,
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
        "/monthplan/productionMonthPlanInit/export",
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
        productionVersion: this.version,
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
    if (this.$route.params.id) {
      this.version = this.$route.params.id;
    }
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
