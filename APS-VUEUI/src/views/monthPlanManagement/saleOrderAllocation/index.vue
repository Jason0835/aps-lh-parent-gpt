
<template>
  <basic-container>
    <page-table
      tableRef="saleOrderAllocationMainTable"
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
          v-hasPermi="['monthplan:SaleOrderAllocation:edit']"
          @click="handleEdit(selection[0])"
          >{{ $t("ui.frame.btn.modify") }}</el-button
        > -->
        <!-- <el-button
          type="danger"
          plain
          :disabled="selection.length === 0"
          v-hasPermi="['monthplan:SaleOrderAllocation:remove']"
          @click="handleDelete(selection)"
          >{{ $t("ui.frame.btn.delete") }}</el-button
        > -->
        <!-- <el-button
          v-hasPermi="['monthplan:SaleOrderAllocation:import']"
          @click="$refs.tltUpload.handleImport()"
          >{{ $t("ui.frame.btn.import") }}</el-button
        > -->
        <el-button
          @click="handleExport"
          v-hasPermi="['monthplan:SaleOrderAllocation:export']"
          >{{ $t("ui.frame.btn.export") }}</el-button
        >
      </template>
    </page-table>
    <!-- <el-button style="display: none" ref="hidePopoverBtnRef"></el-button> -->
    <tlt-upload
      ref="tltUpload"
      downloadUrl="/monthplan/SaleOrderAllocation/importTemplate"
      uploadUrl="/monthplan/SaleOrderAllocation/importData"
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
  listSaleOrderAllocation,
  removeSaleOrderAllocation,
  editSaleOrderAllocation,
  getVersionList,
} from "@/api/monthplan/saleOrderAllocation";
//components
import tltUpload from "@/components/tltUpload/tltUpload.vue";

import infoDialog from "./components/infoDialog.vue";

export default {
  name: "SaleOrderAllocation",
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
    // "TIRE_TYPE",
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
      verList: [],
    };
  },
  computed: {
    columns() {
      let columns = [
        { type: "selection", fixed: "left" },
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
          prop: "monthPlanVersion",
          label: this.$t("ui.data.column.monthSaleOrderPlan.monthPlanVersion"),
          width: 150,
        },
        {
          prop: "customCode",
          label: this.$t("ui.data.column.monthSaleOrderPlan.customCode"),
        },
        {
          prop: "customName",
          label: this.$t("ui.data.column.monthSaleOrderPlan.customName"),
          width: 220,
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
          prop: "productTypeCode",
          label: this.$t("ui.data.column.monthSaleOrderPlan.productTypeCode"),
          formatter: (row) => {
            return row.productTypeName;
          },
        },
        // {
        //   prop: "tireType",
        //   label: this.$t("ui.data.column.monthSaleOrderPlan.tireType"),
        //   formatter: (row, column, value) => {
        //     return this.selectDictLabel(this.dict.type.TIRE_TYPE, value);
        //   },
        // },
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
          prop: "proSize",
          label: this.$t("ui.data.column.monthSaleOrderPlan.proSize"),
        },
        {
          prop: "specifications",
          label: this.$t("ui.data.column.monthSaleOrderPlan.specifications"),
          width: 100,
        },
        {
          prop: "pattern",
          label: this.$t("ui.data.column.monthSaleOrderPlan.pattern"),
          width: 140,
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
          prop: "planQty",
          label: this.$t("ui.data.column.monthSaleOrderPlan.planQty"),
        },
        {
          prop: "allocationQty",
          label: this.$t("ui.data.column.monthSaleOrderPlan.allocationQty"),
        },
        {
          prop: "isEnsurePlan",
          label: this.$t("ui.data.column.monthSaleOrderPlan.isEnsurePlan"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_yes_no, value);
          },
        },
        {
          prop: "isImportantCustom",
          label: this.$t("ui.data.column.monthSaleOrderPlan.isImportantCustom"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_yes_no, value);
          },
        },
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
          prop: "yearMonth",
          label: this.$t("ui.data.column.monthSaleOrderPlan.yearMonth"),
          // type: "date",
          // dateType: "month",
          // valueFormat: "MM",
          // clearable: false,
          render: (form) => {
            return (
              <el-date-picker
                type="month"
                v-model={form.yearMonth}
                value-format={"yyyy-MM"}
                format="yyyy-MM"
                clearable={false}
                onChange={(val) =>
                  this.handleMonthChange({
                    yearMonth: val,
                    ...form,
                  })
                }
              />
            );
          },
        },
        {
          prop: "factoryCode",
          label: this.$t("ui.data.column.monthSaleOrderPlan.factoryCode"),
          type: "select",
          dictData: this.dict.type.biz_factory_name,
          render: (form) => {
            return (
              <dict-select
                options={this.dict.type.biz_factory_name}
                v-model={form.factoryCode}
                onChange={(val) =>
                  this.handleFactoryChange({
                    factoryCode: val,
                    ...form,
                  })
                }
              />
            );
          },
        },
        {
          prop: "monthPlanVersion",
          label: this.$t("ui.data.column.monthSaleOrderPlan.monthPlanVersion"),
          type: "select",
          render: (form) => {
            return (
              <el-select v-model={form.monthPlanVersion} clearable={true}>
                {this.verList.map((item) => {
                  return <el-option key={item} value={item} label={item} />;
                })}
              </el-select>
            );
          },
        },
        // {
        //   prop: "customCode",
        //   label: this.$t("ui.data.column.monthSaleOrderPlan.customCode"),
        // },
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
          type: "select",
          dictData: this.dict.type.biz_stor_type,
        },
        {
          prop: "channel",
          label: this.$t("ui.data.column.monthSaleOrderPlan.channel"),
          type: "select",
          dictData: this.dict.type.biz_channel_type,
        },
        {
          prop: "brand",
          label: this.$t("ui.data.column.monthSaleOrderPlan.brand"),
          type: "select",
          dictData: this.dict.type.biz_brand_type,
        },

        {
          prop: "proSize",
          label: this.$t("ui.data.column.monthSaleOrderPlan.proSize"),
        },
        {
          prop: "specifications",
          label: this.$t("ui.data.column.monthSaleOrderPlan.specifications"),
        },
        {
          prop: "pattern",
          label: this.$t("ui.data.column.monthSaleOrderPlan.pattern"),
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
        removeSaleOrderAllocation({ ids }).then((data) => {
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
          const res = await editSaleOrderAllocation({
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
        "/monthplan/SaleOrderAllocation/export",
        this.formatParams(false)
      );
    },

    handleSelectionChange(rows) {
      this.selection = rows;
    },
    handleYearChange(params) {
      this.getVersionList(params);
      console.log(1);
    },
    handleMonthChange(params) {
      this.getVersionList(params);
    },
    handleFactoryChange(params) {
      this.getVersionList(params);
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
      if (params.yearMonth) {
        let arr = params.yearMonth.split("-");
        params.year = arr[0];
        params.month = arr[1];
        params.yearMonth = undefined;
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
        const data = await listSaleOrderAllocation(this.formatParams());
        console.log(data);
        this.data = data.rows;
        this.page.total = data.total;
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
    },
    async getVersionList(params) {
      this.search = {
        ...this.query,
        ...params,
        monthPlanVersion: undefined,
      };

      if (!params.yearMonth || !params.factoryCode) {
        return;
      }

      try {
        this.verList = [];

        let arr = params.yearMonth.split("-");

        const res = await getVersionList({
          year: arr[0],
          month: arr[1],
          factoryCode: this.search.factoryCode,
        });
        this.verList = res;

        console.log(this.verList);
      } catch (error) {
        console.error(error);
        this.verList = [];
      }
    },
  },
  created() {
    const date = moment();
    let defaultParams = {
      yearMonth: date.format("yyyy-MM"),
      factoryCode: "",
    };
    this.search = {
      ...defaultParams,
    };
    this.query = {
      ...defaultParams,
    };
    this.getVersionList(this.search);
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
