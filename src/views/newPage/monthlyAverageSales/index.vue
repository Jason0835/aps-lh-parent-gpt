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
          @click="handleExport"
          v-hasPermi="['monthplan:mpMonthlySaleQty:export']"
          >{{ $t("ui.frame.btn.export") }}
        </el-button>
      </template>
    </page-table>
    <!-- <el-button style="display: none" ref="hidePopoverBtnRef"></el-button> -->
    <tlt-upload
      ref="tltUpload"
      downloadUrl="/monthplan/productionMouldConfiguration/importTemplate"
      uploadUrl="/monthplan/productionMouldConfiguration/importData"
      @uploadSuccess="getList"
    />
    <!-- <infoDialog ref="infoRef" @success="getList" /> -->
  </basic-container>
</template>
<script>
import { downloadLink } from "@/utils/request";
import {
  listMpMonthlySaleQty,
  tabletMpMonthlySaleQty,
} from "@/api/monthplan/mpMonthlySaleQty";
import tltUpload from "@/components/tltUpload/tltUpload.vue";

// import infoDialog from "./components/infoDialog.vue";

export default {
  name: "MonthlyAverageSales",
  components: {
    tltUpload,
    // infoDialog,
  },
  dicts: [
    "biz_product_type",
    "biz_factory_name",
    "biz_brand_type",
    "biz_stor_type",
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
      areaList: [],
      monthList: [],
    };
  },
  computed: {
    columns() {
      let columns = [
        {
          prop: "factoryCode",
          label: this.$t("common.factory"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_factory_name, value);
          },
        },
        {
          prop: "productTypeCode",
          label: this.$t("ui.data.column.monthplan.productType"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_product_type, value);
          },
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
          width: 120,
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_brand_type, value);
          },
        },
        {
          prop: "materialCode",
          width: 120,
          label: this.$t("ui.data.column.monthplan.oriMaterialCode"),
        },
        {
          prop: "materialDesc",
          label: this.$t("ui.data.column.scheduleAdjust.productCodeDesc"),
          width: 300,
        },
        {
          prop: "rollTwelveMonthSaleQty",
          label: this.$t(
            "ui.data.column.mpMonthlySaleQty.rollTwelveMonthSaleQty"
          ),
        },
        {
          prop: "averageSaleQty",
          label: this.$t("ui.data.column.mpMonthlySaleQty.averageSaleQty"),
        },
        {
          prop: "saleAreaName",
          label: this.$t("ui.data.column.mpMonthlySaleQty.saleArea"),
          width: 180,
        },
        {
          prop: "areaAll",
          label: this.$t("ui.data.column.mpMonthlySaleQty.areaAll"),
          children: [],
        },
        {
          prop: "monthAll",
          label: this.$t("ui.data.column.mpMonthlySaleQty.monthAll"),
          children: [],
        },
        {
          prop: "updateTime",
          label: this.$t("ui.data.column.scheduleAdjust.updata"),
          width: 180,
        },
      ];
      for (let i = 0; i < this.areaList.length; i++) {
        columns[columns.length - 3].children.push({
          prop: this.areaList[i].areaCodeShow,
          label: this.areaList[i].areaCodeNameI18n,
          minWidth:120,
          render: ({ row }) => {
            return (
              <div
                style={{
                  width: "100%", // 缺少引号
                  height: "100%", // 缺少引号
                }}
              >
                {row[this.areaList[i].areaCodeShow + "isYell"] == 1 && (
                  <div
                    style={{
                      width: "100%", // 缺少引号
                      height: "100%", // 缺少引号
                      background: "yellow",
                    }}
                  >
                    {row[this.areaList[i].areaCodeShow]}
                  </div>
                )}
                {row[this.areaList[i].areaCodeShow + "isYell"] != 1 && (
                  <div style={{}}> {row[this.areaList[i].areaCodeShow]}</div>
                )}
              </div>
            );
          },
        });
      }
      for (let i = 0; i < this.monthList.length; i++) {
        columns[columns.length - 2].children.push({
          prop: this.monthList[i].monthShow,
          label: this.monthList[i].month + this.$t("common.month"),
          minWidth:120,
        });
      }
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
          prop: "productTypeCode",
          label: this.$t("ui.data.column.monthplan.productType"),
          type: "select",
          dictData: this.dict.type.biz_product_type,
        },
        {
          prop: "brand",
          label: this.$t("common.brand"),
          type: "select",
          dictData: this.dict.type.biz_brand_type,
        },
        {
          prop: "materialCode",
          label: this.$t("ui.data.column.monthplan.oriMaterialCode"),
        },
        {
          prop: "materialDesc",
          label: this.$t("ui.data.column.scheduleAdjust.productCodeDesc"),
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
        console.log(ids);
        removeProductionMouldConfiguration({ ids }).then((data) => {
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
        "/monthplan/mpMonthlySaleQty/export",
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
        let res = await tabletMpMonthlySaleQty(this.formatParams());
        this.areaList = res.areaTableTitle;
        this.monthList = res.monthTableTitle;
        let listdata = await listMpMonthlySaleQty(this.formatParams());
        let data = listdata.rows;

        for (let i = 0; i < data.length; i++) {
          if (!data[i].areaGroupList) {
            data[i].areaGroupList = [];
          }
          if (!data[i].monthGroupList) {
            data[i].monthGroupList = [];
          }
          console.log(data[i].areaGroupList.length);
          for (let j = 0; j < data[i].areaGroupList.length; j++) {
            data[i][data[i].areaGroupList[j].areaCodeShow] =
              data[i].areaGroupList[j].saleQty;
            data[i][data[i].areaGroupList[j].areaCodeShow + "isYell"] =
              data[i].areaGroupList[j].yellowColorFlag;
          }
          for (let k = 0; k < data[i].monthGroupList.length; k++) {
            data[i][data[i].monthGroupList[k].areaCodeShow] =
              data[i].monthGroupList[k].saleQty;
          }
        }
        this.data = data;
        console.log(data);
        this.page.total = listdata.total;
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
