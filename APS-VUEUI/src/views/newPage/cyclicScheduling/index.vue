<template>
  <basic-container>
    <page-table
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
          v-hasPermi="['monthplan:productionMouldConfiguration:add']"
          @click="handleAdd"
          >{{ $t("SCM抓取") }}
        </el-button> -->
        <el-button
          type="primary"
          plain
          v-hasPermi="['monthplan:SalesOrderPool:edit']"
          @click="handleAdd"
          >{{ $t("ui.frame.btn.add") }}
        </el-button>
        <el-button
          type="danger"
          plain
          :disabled="selection.length == 0"
          v-hasPermi="['monthplan:supplyOrderPool:remove']"
          @click="handleDeleteAll"
          >{{ $t("ui.frame.btn.delete") }}
        </el-button>
        <el-button
          type="primary"
          v-hasPermi="['monthplan:supplyOrderPool:createCycleStockUp']"
          @click="generateCycle"
          plain
          >{{ $t("ui.data.defectiveStock.createCycleStockUp") }}
        </el-button>
        <el-button
          type="primary"
          v-hasPermi="['monthplan:supplyOrderPool:createPrecedentStockUp']"
          @click="generatePrecedent"
          plain
          >{{ $t("ui.data.defectiveStock.createPrecedentStockUp") }}
        </el-button>
        <!-- <el-button
          v-hasPermi="['monthplan:productionMouldConfiguration:import']"
          @click="$refs.tltUpload.handleImport()"
          >{{ $t("ui.frame.btn.import") }}
        </el-button> -->
        <el-button
          @click="handleExport"
          v-hasPermi="['monthplan:supplyOrderPool:export']"
          >{{ $t("ui.frame.btn.export") }}
        </el-button>
      </template>
    </page-table>
    <!-- <el-button style="display: none" ref="hidePopoverBtnRef"></el-button> -->
    <tlt-upload
      ref="tltUpload"
      downloadUrl="/monthplan/supplyOrderPool/importTemplate"
      uploadUrl="​/monthplan​/supplyOrderPool​/importData"
      @uploadSuccess="getList"
    />
    <infoDialog ref="infoRef" @success="getList" />
  </basic-container>
</template>
<script>
import { downloadLink } from "@/utils/request";
import {
  listSupplyOrderPool,
  removeSupplyOrderPool,
  createPrecedentStockUp,
  createCycleStockUp,
  schedulingPate
} from "@/api/monthplan/supplyOrderPool";
import tltUpload from "@/components/tltUpload/tltUpload.vue";

import infoDialog from "./components/infoDialog.vue";

export default {
  name: "CyclicScheduling",
  components: {
    tltUpload,
    infoDialog,
  },
  dicts: [
    "product_category",
    "biz_product_type",
    "biz_factory_name",
    "supply_order_type",
    "biz_stor_type",
    "biz_brand_type",
    "is_schedule",
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
          prop: "factoryCode",
          label: this.$t("common.factory"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_factory_name, value);
          },
        },
        {
          prop: "year",
          label: this.$t("ui.data.colume.year"),
          width: 120,
        },
        {
          prop: "month",
          label: this.$t("ui.data.colume.month"),
          width: 120,
        },
        {
          prop: "productTypeCode",
          label: this.$t("ui.data.column.monthplan.productType"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_product_type, value);
          },
        },
        {
          prop: "orderType",
          label: this.$t("ui.data.defectiveStock.orderType"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(
              this.dict.type.supply_order_type,
              value
            );
          },
          width: 160,
        },
        {
          prop: "isSchedule",
          label: this.$t("是否参与排产"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(
              this.dict.type.is_schedule,
              value
            );
          },
          width: 160,
        },
        {
          prop: "locationType",
          label: this.$t("ui.data.column.finishStock.wai"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_stor_type, value);
          },
        },
        {
          prop: "brand",
          label: this.$t("common.brand"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_brand_type, value);
          },
          width: 120,
        },
        {
          prop: "materialCode",
          label: this.$t("ui.data.column.monthplan.oriMaterialCode"),
          width: 120,
        },
        {
          prop: "materialDesc",
          label: this.$t("ui.data.column.scheduleAdjust.productCodeDesc"),
          width: 400,
        },
        {
          prop: "productCategory",
          label: this.$t("ui.data.column.capsuleChuck.productTypeCode"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.product_category, value);
          },
        },
        {
          prop: "qty",
          label: this.$t("ui.data.defectiveStock.qty"),
        },
        {
          prop: "saleAreaName",
          label: this.$t("ui.data.defectiveStock.saleArea"),
          width: 180,
        },
        {
          prop: "threeAverageQty",
          label: this.$t("ui.data.defectiveStock.threeAverageQty"),
        },
        {
          prop: "sixAverageQty",
          label: this.$t("ui.data.defectiveStock.sixAverageQty"),
        },
        {
          prop: "deliveryFrequency",
          label: this.$t("ui.data.defectiveStock.deliveryFrequency"),
        },
        {
          prop: "structureFrequency",
          label: this.$t("ui.data.defectiveStock.structureFrequency"),
        },
        {
          prop: "threeOverdueStockQty",
          label: this.$t("ui.data.defectiveStock.threeOverdueStockQty"),
        },
        {
          prop: "sixOverdueStockQty",
          label: this.$t("ui.data.defectiveStock.sixOverdueStockQty"),
        },
        {
          prop: "twelveOverdueStockQty",
          label: this.$t("ui.data.defectiveStock.twelveOverdueStockQty"),
        },
        {
          prop: "stockLimit",
          label: this.$t("ui.data.defectiveStock.stockLimit"),
        },

        {
          prop: "remark",
          label: this.$t("common.remark"),
          width: 120,
        },
        {
          prop: "updateTime",
          label: this.$t("ui.data.column.scheduleAdjust.updata"),
          width: 180,
        },
        {
          align: "center",
          label: this.$t("common.option"),
          fixed: "right",
          width: 120,
          render: ({ row }) => {
            return (
              <div>
                {(row.orderType==4) && (
                  <el-button
                    class="minus"
                    type="success"
                    size="mini"
                    v-hasPermi={["monthplan:supplyOrderPool:setSchedule"]}
                    onClick={() => this.handlePate(row)}
                  >
                  {row.isSchedule==1?'取消参与排产':'参与排产'}
                  </el-button>
                )}
                <el-button
                  v-hasPermi={["monthplan:supplyOrderPool:remove"]}
                  class="minus"
                  type="danger"
                  onClick={() => this.handleDelete(row)}
                >
                  {this.$t("ui.frame.btn.delete")}
                </el-button>
              </div>
            );
          },
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
          prop: "productCategory",
          label: this.$t("ui.data.column.capsuleChuck.productTypeCode"),
          type: "select",
          dictData: this.dict.type.product_category,
        },
        {
          prop: "orderType",
          label: this.$t("ui.data.defectiveStock.orderType"),
          type: "select",
          dictData: this.dict.type.supply_order_type,
        },
        {
          prop: "materialCode",
          label: this.$t("ui.data.column.monthplan.oriMaterialCode"),
        },
        {
          prop: "saleAreaName",
          label: this.$t("区域"),
        },

        {
          prop: "materialDesc",
          label: this.$t("ui.data.column.scheduleAdjust.productCodeDesc"),
        },
      ];
    },
  },
  methods: {
    async generateCycle() {
      try {
        this.loading = true;
        const data = await createCycleStockUp();
        this.$modal.msgSuccess(data.msg);
        this.getList();
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
    },
    async generatePrecedent() {
      try {
        this.loading = true;
        const data = await createPrecedentStockUp();
        this.$modal.msgSuccess(data.msg);
        this.getList();
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
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
    handlePate(row) {
      this.$confirm(row.isSchedule==1?this.$t("确定取消参与排产"):this.$t("确定参与排产"), {
        type: "warning",
      }).then(() => {
        schedulingPate({id:row.id}).then((data) => {
          this.$modal.msgSuccess(data.msg);
          this.$set(this.page, "current", 1);
          this.getList();
        });
      });
    },
    handleDelete(rows) {
      this.$confirm(this.$t("common.confirm.delete"), {
        type: "warning",
      }).then(() => {
        const ids = rows.id;
        removeSupplyOrderPool({ ids }).then((data) => {
          this.$modal.msgSuccess(data.msg);
          this.$set(this.page, "current", 1);
          this.getList();
        });
      });
    },
    handleDeleteAll() {
      console.log(this.selection);
      let ids = "";
      for (let i = 0; i < this.selection.length; i++) {
        if (i == this.selection.length - 1) {
          ids = ids + this.selection[i].id;
        } else {
          ids = ids + this.selection[i].id + ",";
        }
      }
      this.$confirm(this.$t("common.confirm.delete"), {
        type: "warning",
      }).then(() => {
        removeSupplyOrderPool({ ids }).then((data) => {
          this.$modal.msgSuccess(data.msg);
          this.$set(this.page, "current", 1);
          this.getList();
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
    handleExport() {
      downloadLink(
        "/monthplan/supplyOrderPool/export",
        this.formatParams(false)
      );
    },

    handleSelectionChange(rows) {
      this.selection = rows;
    },

    handleBuild() {
      if (this.$refs.buildRef) {
        this.$refs.buildRef.show();
      }
    },

    // utils
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

        const data = await listSupplyOrderPool(this.formatParams());
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
    const now = new Date();
    const nextMonth = new Date(now.getFullYear(), now.getMonth() + 1, 1);
    const year = nextMonth.getFullYear();
    const month = nextMonth.getMonth() + 1; // 月份从0开始，需要+1
    let defaultParams = {
      factoryCode: "116",
      yearMonth: `${year}-${month}`,
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
