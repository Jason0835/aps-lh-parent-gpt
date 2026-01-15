
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
        <el-button type="primary" v-hasPermi="['monthplan:simulatedResult:createVmMonthPrediction']"  :loading="createLoading" plain @click="createVersion">{{ $t("ui.data.insertOrder.creater") }}</el-button>
        <!-- <el-button
          type="primary"
          plain
          v-hasPermi="['monthplan:ProductMoldingLimit:add']"
          @click="handleAdd"
          >{{ $t("ui.frame.btn.add") }}</el-button
        >
        <el-button
          type="warning"
          v-hasPermi="['monthplan:ProductMoldingLimit:edit']"
          :disabled="selection.length !== 1"
          @click="handleDelete(selection[0])"
          >{{ $t("ui.frame.btn.delete") }}</el-button
        >
        <el-button
          v-hasPermi="['monthplan:ProductMoldingLimit:import']"
          @click="$refs.tltUpload.handleImport()"
          >{{ $t("ui.frame.btn.import") }}</el-button
        > -->
        <el-button
          @click="handleExport"
          v-hasPermi="['monthplan:simulatedResult:export']"
          >{{ $t("ui.frame.btn.export") }}</el-button
        >
      </template>
    </page-table>
    <!-- <el-button style="display: none" ref="hidePopoverBtnRef"></el-button> -->
    <tlt-upload
      ref="tltUpload"
      downloadUrl="/mdm/productMoldingLimit/importTemplate"
      uploadUrl="/mdm/productMoldingLimit/importData"
      @uploadSuccess="getList"
    />
    <infoDialog ref="infoRef" @success="getList" />
  </basic-container>
</template>
<script>
//lib
import { mapState ,mapGetters} from "vuex";
//utils
import { downloadLink } from "@/utils/request";

import {
  listSimulateResult,
  createVmMonthPrediction,
} from "@/api/monthplan/insertOrder";

//components
import tltUpload from "@/components/tltUpload/tltUpload.vue";

import infoDialog from "./components/infoDialog.vue";

export default {
  name: "InsertOrder",
  components: {
    tltUpload,
    infoDialog,
  },
  dicts: [
    "LINE_TYPE",
    "JOB_TYPE",
    "biz_factory_name",
    "biz_product_type",
    "biz_brand_type",
  ],
  provide() {
    return {
      parentDict: this.dict,
    };
  },
  data() {
    return {
      createLoading:false,
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
    ...mapGetters('globalList', ['structureList']),
    ...mapState({
      moldingMachines: (state) => state.molding.machines,
    }),
    columns() {
      let columns = [
        { type: "selection", fixed: "left" },
        {
          prop: "factoryCode",
          label: this.$t("common.factory"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_factory_name, value);
          },
          width:120,
        },
        {
          prop: "year",
          label: this.$t("ui.data.colume.year"),
          width:120,
        },
        {
          prop: "month",
          label: this.$t("ui.data.colume.month"),
          width:120,
        },
        {
          prop: "productTypeCode",
          label: this.$t("ui.data.column.monthplan.productType"),
          width:120,
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_product_type, value);
          },
        },
        // {
        //   prop: "类型",
        //   label: this.$t("类型"),
        //   width:120,
        // },
        {
          prop: "structureName",
          label: this.$t("ui.data.column.finishStock.structureName"),
          width:180,
        },
        {
          prop: "specifications",
          label: this.$t("ui.data.column.trialPlan.specifications"),
          width:120,
        },
        {
          prop: "pattern",
          label: this.$t("ui.data.column.modelinfo.pattern"),
          width:120,
        },
        {
          prop: "mainPattern",
          label: this.$t("ui.data.column.moldLedger.mainPattern"),
          width:120,
        },
        {
          prop: "brand",
          label: this.$t("common.brand"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_brand_type, value);
          },
          width:120,
        },
        {
          prop: "mainMaterialDesc",
          label: this.$t("ui.data.insertOrder.mainMaterialDesc"),
          width:120,
        },
        {
          prop: "materialCode",
          label: this.$t("ui.data.column.monthplan.oriMaterialCode"),
          width:120,
        },
        {
          prop: "materialDesc",
          label: this.$t("ui.data.column.scheduleAdjust.productCodeDesc"),
          width:320,
        },
        {
          prop: "mouldQty",
          label: this.$t("ui.data.insertOrder.mouldQty"),
          width:120,
        },
        {
          prop: "typeBlockQty",
          label: this.$t("ui.data.monthlyProductionPlan.typeBlockQty"),
          width:120,
        },
        {
          prop: "netQty",
          label: this.$t("ui.data.monthlyProductionPlan.prodReqPlan"),
          width:120,
        },
        {
          prop: "heightQty",
          label: this.$t("ui.data.insertOrder.heightQty"),
          width:120,
        },
        {
          prop: "productionQty",
          label: this.$t("ui.data.insertOrder.productionQty"),
          width:120,
        },
        {
          prop: "monthPlanVersion",
          label: this.$t("ui.data.column.finishStock.requireVersion"),
          width:180,
        },
        {
          prop: "month1",
          label: 'T'+this.$t("ui.data.insertOrder.monthQty"),
        },
        // {
        //   prop: "T+1月排产量",
        //   label: this.$t("T+1月排产量"),
        // },
        // {
        //   prop: "T+2月排产量",
        //   label: this.$t("T+2月排产量"),
        // },
        // {
        //   prop: "T+n月排产量",
        //   label: this.$t("T+n月排产量"),
        // },


        // {
        //   align: "center",
        //   label: this.$t("ui.data.btn.option"),
        //   fixed: "right",
        //   render: ({ row }) => {
        //     return (
        //       <div>
        //         <el-button
        //           v-hasPermi={["monthplan:ProductMoldingLimit:edit"]}
        //           class="minus"
        //           type="success"
        //           onClick={() => this.handleEdit(row)}
        //         >
        //           {this.$t("ui.frame.btn.update")}
        //         </el-button>
        //         <el-button
        //           v-hasPermi={["monthplan:ProductMoldingLimit:remove"]}
        //           class="minus"
        //           type="danger"
        //           onClick={() => this.handleDelete(row)}
        //         >
        //           {this.$t("ui.frame.btn.delete")}
        //         </el-button>
        //       </div>
        //     );
        //   },
        // },
      ];

      for (let i = 0; i < 23; i++) {
        columns.push({
          label: `T+${i + 1}`+ this.$t("ui.data.insertOrder.monthQty"),
          // label: this.$t("ui.data.column.mouldingDayResult.day", {
          //   day: i + 1,
          // }),
          prop: `month${i + 2}`,
          minWidth: "80px",
          type: "number",
        });
      }
      columns.push(
        {
          prop: "remark",
          label: this.$t("common.remark"),
          width:120,
        },
        {
          prop: "updateTime",
          label: this.$t("ui.data.column.scheduleAdjust.updata"),
          width: 180,
        },
      )

      return columns;
    },
    searchColumns() {
      return [
        {
          prop: "factoryCode",
          label: this.$t("common.factory"),
          type: "select",
          dictData: this.dict.type.biz_factory_name,
        },
        {
          prop: "structureName",
          label: this.$t("ui.data.column.finishStock.structureName"),
          type: "select",
          dictData:this.structureList,
          filterable: true
        },
        {
          prop: "productTypeCode",
          label: this.$t("ui.data.column.monthplan.productType"),
          type: "select",
          dictData: this.dict.type.biz_product_type,
        },
        {
          prop: "specifications",
          label: this.$t("ui.data.column.trialPlan.specifications"),
        },
        {
          prop: "pattern",
          label: this.$t("ui.data.column.modelinfo.pattern"),
        },
        {
          prop: "mainMaterialDesc",
          label: this.$t("ui.data.column.skuEmbryoRelation.materialCode"),
        },
        {
          prop: "materialCode",
          label: this.$t("ui.data.column.monthplan.oriMaterialCode"),
        },
        {
          prop: "materialDesc",
          label: this.$t("ui.data.column.scheduleAdjust.productCodeDesc"),
        },
        {
          prop: "brand",
          label: this.$t("common.brand"),
          type: "select",
          dictData: this.dict.type.biz_brand_type,

        },
      ];
    },
  },
  methods: {
    async createVersion() {

      try {
        this.createLoading=true
        let res = await createVmMonthPrediction(this.formatParams());
        this.$modal.msgSuccess(res.msg);
        this.getList();
        this.createLoading=false
      } catch (err) {
        this.createLoading=false
      }
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
      downloadLink(
        "/monthplan/simulatedResult/export",
        this.formatParams(false)
      );
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
        const data = await listSimulateResult(this.formatParams());
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
    let defaultParams = {
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
  activated() {},
};
</script>
<style lang="scss" scoped>
.more-btn {
  margin: 2px 0;
  width: 100%;
}
</style>
