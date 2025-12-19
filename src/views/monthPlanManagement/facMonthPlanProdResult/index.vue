
<template>
  <basic-container>
    <page-table
      tableRef="FacMonthPlanProdResultMainTable"
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
          v-hasPermi="['monthplan:monthSaleOrderPlan:add']"
          @click="handleAdd"
          >{{ $t("生产销售需求计划") }}</el-button
        >
        <el-button
          type="primary"
          plain
          v-hasPermi="['monthplan:monthSaleOrderPlan:add']"
          @click="handleAdd"
          >{{ $t("定稿") }}</el-button
        >
        <el-button
          type="primary"
          plain
          v-hasPermi="['monthplan:monthSaleOrderPlan:add']"
          @click="handleAdd"
          >{{ $t("调整") }}</el-button
        >
        <el-button
          type="primary"
          plain
          v-hasPermi="['monthplan:monthSaleOrderPlan:add']"
          @click="handleAdd"
          >{{ $t("上线设置") }}</el-button
        >
        <!-- <el-button
          type="primary"
          plain
          v-hasPermi="['monthplan:monthSaleOrderPlan:edit']"
          @click="handleEdit(selection[0])"
          >{{ $t("ui.frame.btn.modify") }}</el-button
        > -->
        <!-- <el-button
          type="danger"
          plain
          :disabled="selection.length === 0"
          v-hasPermi="['monthplan:monthSaleOrderPlan:remove']"
          @click="handleDelete(selection)"
          >{{ $t("ui.frame.btn.delete") }}</el-button
        > -->
        <el-button
          v-hasPermi="['monthplan:monthSaleOrderPlan:import']"
          @click="$refs.tltUpload.handleImport()"
          >{{ $t("ui.frame.btn.import") }}</el-button
        >
        <!-- <el-button
          @click="handleExport"
          v-hasPermi="['monthplan:monthSaleOrderPlan:export']"
          >{{ $t("ui.frame.btn.export") }}</el-button
        > -->
      </template>
    </page-table>
    <!-- <el-button style="display: none" ref="hidePopoverBtnRef"></el-button> -->
    <tlt-upload
      ref="tltUpload"
      downloadUrl="/demand/monthSaleOrderPlan/importTemplate"
      uploadUrl="/demand/monthSaleOrderPlan/importData"
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
import {
  listFacMonthPlanProdResult,
  removeFacMonthPlanProdResult,
  editFacMonthPlanProdResult,
} from "@/api/factory/facMonthPlanProdResult";
//components
import tltUpload from "@/components/tltUpload/tltUpload.vue";

import infoDialog from "./components/infoDialog.vue";

export default {
  name: "FacMonthPlanProdResult",
  components: {
    tltUpload,
    infoDialog,
  },
  dicts: [
    "biz_factory_name",
    "biz_stor_type",
    "biz_yes_no",
    "biz_channel_type",
    "biz_brand_type",
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
        { type: "selection", fixed: "left" },
        {
          label: this.$t("月度计划版本"),
          prop: "",
        },
        {
          prop: "customCode",
          label: this.$t("ui.data.column.monthSaleOrderPlan.customCode"),
        },
        {
          prop: "customName",
          label: this.$t("ui.data.column.monthSaleOrderPlan.customName"),
        },
        {
          prop: "factoryCode",
          label: this.$t("ui.data.column.monthSaleOrderPlan.factoryCode"),
          formatter: (row) => {
            return this.selectDictLabel(
              this.dict.type.biz_factory_name,
              row.factoryCode
            );
          },
        },
        {
          prop: "year",
          label: this.$t("ui.data.column.monthSaleOrderPlan.year"),
        },
        {
          prop: "month",
          label: this.$t("ui.data.column.monthSaleOrderPlan.month"),
        },
        {
          prop: "locationType",
          label: this.$t("ui.data.column.monthSaleOrderPlan.locationType"),
          formatter: (row) => {
            return this.selectDictLabel(
              this.dict.type.biz_stor_type,
              row.locationType
            );
          },
        },
        {
          prop: "productCode",
          label: this.$t("ui.data.column.monthSaleOrderPlan.productCode"),
        },
        {
          prop: "productDesc",
          label: this.$t("ui.data.column.monthSaleOrderPlan.productDesc"),
          width: 250,
        },
        {
          prop: "locationType",
          label: this.$t("ui.data.column.monthSaleOrderPlan.locationType"),
          formatter: (row) => {
            return this.selectDictLabel(
              this.dict.type.biz_stor_type,
              row.locationType
            );
          },
        },
        {
          prop: "channel",
          label: this.$t("ui.data.column.monthSaleOrderPlan.channel"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_channel_type, value);
          },
        },
        {
          prop: "brand",
          label: this.$t("ui.data.column.monthSaleOrderPlan.brand"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_brand_type, value);
          },
        },
        {
          prop: "planQty",
          label: this.$t("ui.data.column.monthSaleOrderPlan.planQty"),
        },
        {
          prop: "salePerson",
          label: this.$t("ui.data.column.monthSaleOrderPlan.salePerson"),
        },

        // {
        //   prop: "deliveryDateDue",
        //   label: this.$t("ui.data.column.monthSaleOrderPlan.deliveryDateDue"),
        // },
        // {
        //   prop: "isEmergency",
        //   label: this.$t("ui.data.column.monthSaleOrderPlan.isEmergency"),
        //   formatter: (row, column, value) => {
        //     return this.selectDictLabel(this.dict.type.biz_yes_no, value);
        //   },
        // },
        // {
        //   prop: "isEnsurePlan",
        //   label: this.$t("ui.data.column.monthSaleOrderPlan.isEnsurePlan"),
        //   formatter: (row, column, value) => {
        //     return this.selectDictLabel(this.dict.type.biz_yes_no, value);
        //   },
        // },
        // {
        //   prop: "isImportantCustom",
        //   label: this.$t("ui.data.column.monthSaleOrderPlan.isImportantCustom"),
        //   formatter: (row, column, value) => {
        //     return this.selectDictLabel(this.dict.type.biz_yes_no, value);
        //   },
        // },
        // {
        //   prop: "remark",
        //   label: this.$t("common.remark"),
        // },
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
          prop: "year",
          label: this.$t("ui.data.column.monthSaleOrderPlan.year"),
          type: "date",
          dateType: "year",
          valueFormat: "yyyy",
          clearable: false,
        },
        {
          prop: "month",
          label: this.$t("ui.data.column.monthSaleOrderPlan.month"),
          type: "date",
          dateType: "month",
          valueFormat: "MM",
          clearable: false,
        },
        {
          prop: "factoryCode",
          label: this.$t("ui.data.column.monthSaleOrderPlan.factoryCode"),
          type: "select",
          dictData: this.dict.type.biz_factory_name,
        },
        {
          prop: "locationType",
          label: this.$t("ui.data.column.monthSaleOrderPlan.locationType"),
          type: "select",
          dictData: this.dict.type.biz_stor_type,
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
        removeFacMonthPlanProdResult({ ids }).then((data) => {
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
          const res = await editFacMonthPlanProdResult({
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
        "/demand/monthSaleOrderPlan/export",
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
        const data = await listFacMonthPlanProdResult(this.formatParams());
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
    // const date = moment();
    // let defaultParams = {
    //   year: date.format("yyyy"),
    //   month: date.format("MM"),
    // };
    // this.search = {
    //   ...defaultParams,
    // };
    // this.query = {
    //   ...defaultParams,
    // };
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
