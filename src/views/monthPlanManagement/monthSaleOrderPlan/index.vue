
<template>
  <basic-container>
    <page-table
      tableRef="MonthSaleOrderPlanMainTable"
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
          v-hasPermi="['monthplan:syncInSaleOrder:sync']"
          @click="handleSyncInOrder"
          >{{ $t("抓取内销订单") }}</el-button
        >
        <el-button
          type="primary"
          plain
          v-hasPermi="['monthplan:syncOutSaleOrder:sync']"
          @click="handleSyncOutOrder"
          >{{ $t("抓取外销订单") }}</el-button
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
      :updateSupport="true"
      downloadUrl="/demand/monthSaleOrderPlan/importTemplate"
      uploadUrl="/demand/monthSaleOrderPlan/importData"
      @uploadSuccess="getList"
    />
    <!-- <infoDialog ref="infoRef" @success="getList" /> -->
    <syncOutDialog ref="outRef" @success="getList" />
    <syncInDialog ref="inRef" @success="getList" />
  </basic-container>
</template>
<script>
//lib
import moment from "moment";
//utils
import { downloadLink } from "@/utils/request";
//interface
import {
  listMonthSaleOrderPlan,
  removeMonthSaleOrderPlan,
  editMonthSaleOrderPlan,

} from "@/api/monthplan/monthSaleOrderPlan";
//components
import tltUpload from "@/components/tltUpload/tltUpload.vue";

// import infoDialog from "./components/infoDialog.vue";
import syncOutDialog from "./components/syncOutDialog.vue";
import syncInDialog from "./components/syncInDialog.vue";

export default {
  name: "MonthSaleOrderPlan",
  components: {
    tltUpload,
    // infoDialog,
    syncOutDialog,
    syncInDialog,
  },
  dicts: [
    "biz_factory_name",
    "biz_stor_type",
    "biz_yes_no",
    "biz_channel_type",
    "biz_brand_type",
    "TIRE_TYPE",
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
      yearMonth: moment().format("yyyy-MM"),
    };
  },
  computed: {
    columns() {
      let columns = [
        { type: "selection", fixed: "left" },
        {
          prop: "customCode",
          label: this.$t("ui.data.column.monthSaleOrderPlan.customCode"),
        },
        {
          prop: "customName",
          label: this.$t("ui.data.column.monthSaleOrderPlan.customName"),
          width: 250,
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
          prop: "tireType",
          label: this.$t("ui.data.column.monthSaleOrderPlan.tireType"),
          width: 150,
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.TIRE_TYPE, value);
          },
        },
        {
          prop: "proSize",
          label: this.$t("ui.data.column.monthSaleOrderPlan.proSize"),
        },
        {
          prop: "pattern",
          label: this.$t("ui.data.column.monthSaleOrderPlan.pattern"),
          width: 140,
        },

        {
          prop: "planQty",
          label: this.$t("ui.data.column.monthSaleOrderPlan.orderNum"),
        },
        {
          prop: "salePerson",
          label: this.$t("ui.data.column.monthSaleOrderPlan.salePerson"),
        },

        {
          prop: "deliveryDateDue",
          label: this.$t("ui.data.column.monthSaleOrderPlan.deliveryDateDue"),
          width: 160,
        },
        {
          prop: "isEmergency",
          label: this.$t("ui.data.column.monthSaleOrderPlan.isEmergency"),
          align: "center",
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_yes_no, value);
          },
        },
        {
          prop: "isEnsurePlan",
          label: this.$t("ui.data.column.monthSaleOrderPlan.isEnsurePlan"),
          align: "center",
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_yes_no, value);
          },
        },
        {
          prop: "isImportantCustom",
          label: this.$t("ui.data.column.monthSaleOrderPlan.isImportantCustom"),
          align: "center",
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_yes_no, value);
          },
        },
        {
          prop: "orderNo",
          label: this.$t("ui.data.column.monthSaleOrderPlan.orderNo"),
          width: 120,
        },
        {
          prop: "createTime",
          label: this.$t("common.createTime"),
          width: 160,
        },
        {
          prop: "sourceTypeDesc",
          label: this.$t("ui.data.column.monthSaleOrderPlan.sourceType"),
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
          type: "date",
          dateType: "month",
          valueFormat: "yyyy-MM",
          clearable: false,
        },
        {
          prop: "factoryCode",
          label: this.$t("ui.data.column.monthSaleOrderPlan.factoryCode"),
          type: "select",
          dictData: this.dict.type.biz_factory_name,
          clearable: false,
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
          prop: "isEnsurePlan",
          label: this.$t("ui.data.column.monthSaleOrderPlan.isEnsurePlan"),
          type: "select",
          dictData: this.dict.type.biz_yes_no,
        },
        {
          prop: "isImportantCustom",
          label: this.$t("ui.data.column.monthSaleOrderPlan.isImportantCustom"),
          type: "select",
          dictData: this.dict.type.biz_yes_no,
        },
        {
          prop: "productCode",
          label: this.$t("ui.data.column.monthSaleOrderPlan.productCode"),
        },
        {
          prop: "productDesc",
          label: this.$t("ui.data.column.monthSaleOrderPlan.productDesc"),
        },
        {
          prop: "orderNo",
          label: this.$t("ui.data.column.monthSaleOrderPlan.orderNo"),
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
          prop: "tireType",
          label: this.$t("ui.data.column.monthSaleOrderPlan.tireType"),
          type: "select",
          dictData: this.dict.type.TIRE_TYPE,
        },
        {
          prop: "proSize",
          label: this.$t("ui.data.column.monthSaleOrderPlan.proSize"),
        },
        {
          prop: "pattern",
          label: this.$t("ui.data.column.monthSaleOrderPlan.pattern"),
          width: 140,
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
        removeMonthSaleOrderPlan({ ids }).then((data) => {
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
          const res = await editMonthSaleOrderPlan({
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
    handleSyncInOrder() {
       if (this.$refs.inRef) {
        this.$refs.inRef.show();
      }
    },
    handleSyncOutOrder() {
      if (this.$refs.outRef) {
        this.$refs.outRef.show();
      }
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
        const data = await listMonthSaleOrderPlan(this.formatParams());
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
