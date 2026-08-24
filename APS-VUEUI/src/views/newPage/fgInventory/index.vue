<template>
  <basic-container>
    <page-table
      tableRef="DemandPlanMainTable"
      :calcHeight="true"
      v-loading="loading"
      :columns="columns"
      :searchColumns="searchColumns"
       :row-class-name="tableRowClassName"
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
          v-hasPermi="['monthplan:mdmFinishStock:export4Mes']"
          >{{ $t("ui.frame.btn.export") }}
        </el-button>
          <el-button
          type="primary"
          plain
          v-hasPermi="['monthplan:mdmFinishStock:genOverDueSkuByStock']"
          @click="handleAdd"
          >{{ $t("ui.btn.fgInventory.genOverDueSku") }}
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
    <infoDialog ref="infoRef" @success="getList" />

  </basic-container>
</template>
<script>
import { downloadLink } from "@/utils/request";
import { getListMes } from "@/api/monthplan/finishStock";
import tltUpload from "@/components/tltUpload/tltUpload.vue";

import infoDialog from "./components/infoDialog.vue";

export default {
  name: "FgInventory",
  components: {
    tltUpload,
    infoDialog,
  },
  dicts: ["biz_factory_name", "biz_product_type", "biz_yes_no",'biz_stor_type','biz_brand_type'],
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
        // { type: "selection", fixed: "left" },
        {
          prop: "factoryCode",
          align: "center",
          label: this.$t("ui.data.column.productStock.factoryCode"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_factory_name, value);
          },
        },
        {
          prop: "productTypeCode",
          align: "center",
          label: this.$t("ui.data.column.productStock.productTypeCode"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_product_type, value);
          },
        },
        {
          prop: "brand",
          align: "center",
          label: this.$t("ui.data.column.productStock.brand"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_brand_type, value);
          },
          minWidth:120
        },
        {
          prop: "finalGrade",
          align: "center",
          label: this.$t("ui.data.column.productStock.finalGrade"),
          width:120
        },
        {
          prop: "structureName",
          label: this.$t("ui.data.column.productStock.structureName"),
          width:200
        },
        {
          prop: "locationType",
          align: "center",
          label: this.$t("ui.data.column.productStock.locationType"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_stor_type, value);
          },
          width:120
        },
        {
          prop: "materialCode",
          align: "center",
          label: this.$t("ui.data.column.productStock.materialCode"),
          width:180
        },
        {
          prop: "materialDesc",
          label: this.$t("ui.data.column.productStock.materialDesc"),
          align: "left",
          minWidth: 350
        },

        {
          prop: "stockQty",
          align: "center",
          label: this.$t("ui.data.column.productStock.stockQty"),
        },
        {
          prop: "stockDate",
          align: "center",
          label: this.$t("ui.data.defectiveStock.stockDate"),
          width:120
        },
        {
          prop: "weekYear",
          align: "center",
          label: this.$t("ui.data.column.productStock.weekYear"),
        },

        // {
        //   prop: "isDynamicBalance",
        //   label: this.$t("ui.data.column.monthplan.dynamicBalance"),
        //   formatter: (row, column, value) => {
        //     return this.selectDictLabel(this.dict.type.biz_yes_no, value);
        //   },
        // },
        // {
        //   prop: "isUniformity",
        //   label: this.$t("ui.data.column.monthplan.uniformity"),
        //   formatter: (row, column, value) => {
        //     return this.selectDictLabel(this.dict.type.biz_yes_no, value);
        //   },
        // },
        {
          prop: "isExceedTire",
          align: "center",
          label: this.$t("ui.data.column.productStock.isExceedTire"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_yes_no, value);
          },
        },
        {
          prop: "isExceedThreeMonth",
          align: "center",
          label: this.$t("ui.data.column.productStock.isExceedThreeMonth"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_yes_no, value);
          },
        },
        {
          prop: "isExceedSixMonth",
          align: "center",
          label: this.$t("ui.data.column.productStock.isExceedSixMonth"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_yes_no, value);
          },
        },
        {
          prop: "isExceedTwelveMonth",
          align: "center",
          label: this.$t("ui.data.column.productStock.isExceedTwelveMonth"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_yes_no, value);
          },
        },
        {
          prop: "updateBy",
          align: "center",
          label: this.$t("ui.data.column.updateBy"),
          width: 100,
        },
        {
          prop: "updateTime",
          width: 180,
          label: this.$t("ui.data.column.updateTime"),
        },
      ];

      return columns;
    },
    searchColumns() {
      return [
        {
          prop: "factoryCode",
          label: this.$t("ui.data.column.productStock.factoryCode"),
          type: "select",
          dictData: this.dict.type.biz_factory_name,
        },
        {
          prop: "productTypeCode",
          label: this.$t("ui.data.column.productStock.productTypeCode"),
          type: "select",
          dictData: this.dict.type.biz_product_type,
        },
        // {
        //   prop: "mouldCode",
        //   label: this.$t("ui.data.column.productStock.brand"),
        // },
        // {

        {
          prop: "materialCode",
          label: this.$t("ui.data.column.productStock.materialCode"),
        },
        {
          prop: "materialDesc",
          label: this.$t("ui.data.column.productStock.materialDesc"),
          minWidth: 350,
          align: "left",
        },
        // {
        //   prop: "mouldCode",
        //   label: this.$t("ui.data.column.monthplan.dynamicBalance"),
        // },
        // {
        //   prop: "mouldCode",
        //   label: this.$t("ui.data.column.monthplan.uniformity"),
        // },
        {
          prop: "isExceedMonth",
          label: this.$t("ui.data.column.productStock.isExceedTire"),
          type: "checkbox",
        },
      ];
    },
  },
  methods: {
    tableRowClassName({ row, rowIndex }) {
      if (row.isExceedTwelveMonth == 1) {
        return 'warning-row'

      }
      if (row.isExceedSixMonth == 1) {
        return "deep-yellow";
      }
      if (row.isExceedThreeMonth == 1) {
        return "light-yellow";
      }

      return "";
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
        // const ids = rows.map((row) => row.id).join(",");
        // console.log(ids);
        // removeProductionMouldConfiguration({ ids }).then((data) => {
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
    handleExport() {
      downloadLink(
        "/monthplan/mdmProductStock/export",
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
      if (params.isExceedMonth) {
        // params.isExceedThreeMonth = 1;
        // params.isExceedSixMonth = 1;
        // params.isExceedNineMonth = 1;
        // params.isExceedTwelveMonth = 1;
        params.isExceedTire=1
      } else {
        params.isExceedTire=''
        // params.isExceedThreeMonth = 0;
        // params.isExceedSixMonth = 0;
        // params.isExceedNineMonth = 0;
        // params.isExceedTwelveMonth = 0;
      }
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

        const data = await getListMes(this.formatParams());
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
    let defaultParams = {
      factoryCode: "116",
      isExceedMonth:false
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

  },
};
</script>
<style lang="scss" scoped>
.more-btn {
  margin: 2px 0;
  width: 100%;
}
</style>
