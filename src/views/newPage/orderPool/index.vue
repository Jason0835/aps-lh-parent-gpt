<template>
  <basic-container>
    <page-table
      tableRef="productionMouldConfigurationMainTable"
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
          v-hasPermi="['monthplan:SalesOrderPool:getSCMData']"
          @click="SCMBtn"
          >{{ $t("ui.data.column.moldLedger.scm") }}
        </el-button>
        <!-- <el-button
          type="primary"
          plain
          v-hasPermi="['monthplan:productionMouldConfiguration:add']"
          @click="handleAdd"
          >{{ $t("ui.frame.btn.add") }}
        </el-button> -->
        <!-- <el-button
          type="danger"
          plain
          :disabled="selection.length == 0"
          v-hasPermi="['monthplan:productionMouldConfiguration:remove']"
          @click="handleDelete(selection)"
          >{{ $t("ui.frame.btn.delete") }}
        </el-button> -->

        <!-- <el-button
          v-hasPermi="['monthplan:productionMouldConfiguration:import']"
          @click="$refs.tltUpload.handleImport()"
          >{{ $t("ui.frame.btn.import") }}
        </el-button> -->
        <el-button
          :disabled="selection.length == 0"
            v-hasPermi="['monthplan:SalesOrderPool:edit']"
          @click="handleEdit(selection[0])"
          >{{ $t("ui.data.column.monthplan.modifyLevel") }}
        </el-button>
        <el-button
          @click="handleExport"
          v-hasPermi="['monthplan:SalesOrderPool:export']"
          >{{ $t("ui.frame.btn.export") }}
        </el-button>
        <el-button
          type="primary"
          plain
          v-hasPermi="['monthplan:SalesOrderPool:lock']"
          @click="lockBtn"
          >{{ $t("ui.data.column.oderPool.lock") }}
        </el-button>
      </template>
    </page-table>
    <!-- <el-button style="display: none" ref="hidePopoverBtnRef"></el-button> -->
    <tlt-upload
      ref="tltUpload"
      downloadUrl="/monthplan/productionMouldConfiguration/importTemplate"
      uploadUrl="/monthplan/SalesOrderPool/importData"
      @uploadSuccess="getList"
    />
    <infoDialog ref="infoRef" @success="getList" />
    <scmoDialog ref="scmRef" @success="getList" />
  </basic-container>
</template>
<script>
import { downloadLink } from "@/utils/request";
import {
  getSalesList,
  removeSales,
  getSCMData,
  saveData,
  getSCMDataCheck,
} from "@/api/newPage/salesOrderPool";
import tltUpload from "@/components/tltUpload/tltUpload.vue";

import infoDialog from "./components/infoDialog.vue";
import scmoDialog from "./components/scmDialog.vue";

export default {
  name: "RegionalCapacityAllocation",
  components: {
    tltUpload,
    infoDialog,
    scmoDialog
  },
  dicts: [
    "biz_product_type",
    "biz_order_type",
    "biz_factory_name",
    "biz_deliver_goods_type",
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
        // {
        //   prop: "year",
        //   label: this.$t("ui.data.column.productionMouldConfiguration.year"),
        // },
        // {
        //   prop: "month",
        //   label: this.$t("ui.data.column.productionMouldConfiguration.month"),
        // },
        {
          prop: "factoryCode",
          label: this.$t("common.factory"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_factory_name, value);
          },
        },
        {
          prop: "productType",
          label: this.$t("ui.data.column.monthplan.productType"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_product_type, value);
          },
        },
        {
          prop: "scmPriority",
          label: this.$t("ui.data.column.monthplan.scmPriority"),
          render: ({ row }) => {
            return (
              <el-select placeholder="请选择" v-model={row.scmPriority} onChange={(val) => this.handlePriorityChange(row, val)}>
                {this.dict.type.biz_order_type.map((item) => (
                  <el-option
                    key={item.value}
                    label={item.label}
                    value={item.value}
                  ></el-option>
                ))}
              </el-select>
            );
          },
        },
        {
          prop: "orderPriority",
          label: this.$t("ui.data.column.monthplan.orderPriority"),
          width: 150,
          // render: ({ row }) => {
          //   return (
          //     <el-select placeholder="请选择" v-model={row.orderPriority} onChange={(val) => this.handlePriorityChange(row, val)}>
          //       {this.dict.type.biz_order_type.map((item) => (
          //         <el-option
          //           key={item.value}
          //           label={item.label}
          //           value={item.value}
          //         ></el-option>
          //       ))}
          //     </el-select>
          //   );
          // },
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_order_type, value);
          },
        },
        {
          prop: "area",
          label: this.$t("common.area"),
        },
        {
          prop: "salCode",
          label: this.$t("ui.data.column.monthplan.salCode"),
          width: 120,
        },
        {
          prop: "salNCode",
          label: this.$t("ui.data.column.monthplan.salNCode"),
        },
        {
          prop: "natCode",
          label: this.$t("ui.data.column.monthplan.natCode"),
        },
        {
          prop: "brand",
          label: this.$t("common.brand"),
        },
        {
          prop: "salCodePo",
          label: this.$t("ui.data.column.monthplan.salCodePo"),
        },
        {
          prop: "billDate",
          label: this.$t("schedule.glueDecomposePlan.submissionDate"),
          width: 120,
        },
        {
          prop: "oriMaterialCode",
          label: this.$t("ui.data.column.monthplan.oriMaterialCode"),
        },
        {
          prop: "materialDesc",
          label: this.$t("ui.data.column.scheduleAdjust.productCodeDesc"),
          width:300,
        },
        {
          prop: "ordQty",
          label: this.$t("common.num"),
        },
        {
          prop: "weekYear",
          label: this.$t("ui.data.column.monthplan.weekYear"),
        },
        {
          prop: "isDynamicBalance",
          label: this.$t("ui.data.column.monthplan.dynamicBalance"),
          render: ({ row }) => {
            return (
              <div>
                {row.isDynamicBalance == 1
                  ? this.$t("common.yes")
                  : this.$t("common.no")}
              </div>
            );
          },
        },
        {
          prop: "isUniformity",
          label: this.$t("ui.data.column.monthplan.uniformity"),
          render: ({ row }) => {
            return (
              <div>
                {row.isUniformity == 1
                  ? this.$t("common.yes")
                  : this.$t("common.no")}
              </div>
            );
          },
        },
        {
          prop: "isEudr",
          label: this.$t("EUDR"),
          render: ({ row }) => {
            return (
              <div>
                {row.isEudr == 1 ? this.$t("common.yes") : this.$t("common.no")}
              </div>
            );
          },
        },
        {
          prop: "deliverGoodsType",
          label: this.$t("common.shipType"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(
              this.dict.type.biz_deliver_goods_type,
              value
            );
          },
        },

        {
          prop: "updateTime",
          label: this.$t("ui.data.column.scheduleAdjust.updata"),
          width: 200,
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
          prop: "productType",
          label: this.$t("ui.data.column.monthplan.productType"),
          type: "select",
          dictData: this.dict.type.biz_product_type,
        },
        {
          prop: "orderPriority",
          label: this.$t("ui.data.column.monthplan.orderPriority"),
          type: "select",
          dictData: this.dict.type.biz_order_type,
        },
        {
          prop: "area",
          label: this.$t("common.area"),
        },
        {
          prop: "salCode",
          label: this.$t("ui.data.column.monthplan.salCode"),
        },
        {
          prop: "salCodePo",
          label: this.$t("ui.data.column.monthplan.salCodePo"),
        },
        {
          label: this.$t("schedule.glueDecomposePlan.submissionDate"),
          prop: "billDate",
          type: "date",
          dateType: "daterange",
          valueFormat: "yyyy-MM-dd",
        },
        {
          prop: "oriMaterialCode",
          label: this.$t("ui.data.column.monthplan.oriMaterialCode"),
        },
        {
          prop: "materialDesc",
          label: this.$t("ui.data.column.scheduleAdjust.productCodeDesc"),
        },
        {
          prop: "deliverGoodsType",
          label: this.$t("common.shipType"),
          type: "select",
          dictData: this.dict.type.biz_deliver_goods_type,
        },
      ];
    },
  },
  methods: {
    handlePriorityChange(row, val) {
      console.log(row, val);
      // let params = {
      //   id: row.id,
      //   orderPriority: val,
      // };
      saveData(row)
        .then((res) => {
          this.$modal.msgSuccess(res.msg);
          this.getList();
        })
        .catch((err) => {
          console.log(err);
        });
    },
    async SCMBtn() {
      if (this.$refs.scmRef) {
        this.$refs.scmRef.show();
      }
      // try {
      //   let res = await getSCMDataCheck();
      //   console.log(res);
      //   if (res.data == 1) {
      //     this.$confirm(res.msg, {
      //       type: "warning",
      //     }).then(() => {
      //       getSCMData().then((data) => {
      //         this.$modal.msgSuccess(data.msg);
      //         this.$set(this.page, "current", 1);
      //         this.getList();
      //       });
      //     });
      //   } else {
      //     this.$modal.msgSuccess(res.msg);
      //     this.$set(this.page, "current", 1);
      //     this.getList();
      //   }
      // } catch (err) {
      //   console.log(err);
      // }
    },
    async lockBtn(){
      if (this.$refs.scmRef) {
        this.$refs.scmRef.show('lock');
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
    handleDelete(rows) {
      this.$confirm(this.$t("common.confirm.delete"), {
        type: "warning",
      }).then(() => {
        const ids = rows.map((row) => row.id).join(",");
        console.log(ids);
        removeSales({ ids }).then((data) => {
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
        "/monthplan/SalesOrderPool/export",
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

      if (params.billDate && params.billDate[0]) {
        params.billDateStartTime = params.billDate[0];
        params.billDateEndTime = params.billDate[1];
        params.billDate = undefined;
      }

      return params;
    },
    // api
    async getList() {
      try {
        const data = await getSalesList(this.formatParams());
        // console.log(data);
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
