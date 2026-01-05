<template>
  <basic-container>
    <page-table
      tableRef="DemandPlanMainTable"
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
          v-hasPermi="['monthplan:mdmFinishStock:export4Mes']"
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
import { getListMes } from "@/api/monthplan/finishStock";
import tltUpload from "@/components/tltUpload/tltUpload.vue";

// import infoDialog from "./components/infoDialog.vue";

export default {
  name: "fgInventory",
  components: {
    tltUpload,
    // infoDialog,
  },
  dicts: ["biz_factory_name", "biz_product_type", "biz_yes_no",'biz_stor_type'],
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
          prop: "brand",
          label: this.$t("common.brand"),
        },
        {
          prop: "structureName",
          label: this.$t("ui.data.column.finishStock.structureName"),
          width:200
        },
        {
          prop: "locationType",
          label: this.$t("ui.data.column.finishStock.wai"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_stor_type, value);
          },
          width:120
        },
        {
          prop: "materialCode",
          label: this.$t("ui.data.column.monthplan.oriMaterialCode"),
          width:180
        },
        {
          prop: "materialDesc",
          label: this.$t("ui.data.column.scheduleAdjust.productCodeDesc"),
          width:300
        },

        {
          prop: "stockQty",
          label: this.$t("ui.data.column.finishStock.stockQty"),
        },
        {
          prop: "stockDate",
          label: this.$t("ui.data.defectiveStock.stockDate"),
          width:120
        },
        {
          prop: "weekYear",
          label: this.$t("ui.data.column.monthplan.weekYear"),
        },

        {
          prop: "isDynamicBalance",
          label: this.$t("ui.data.column.monthplan.dynamicBalance"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_yes_no, value);
          },
        },
        {
          prop: "isUniformity",
          label: this.$t("ui.data.column.monthplan.uniformity"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_yes_no, value);
          },
        },
        {
          prop: "isExceedTire",
          label: this.$t("ui.data.column.finishStock.isExceedMonth"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_yes_no, value);
          },
        },
        {
          prop: "isExceedThreeMonth",
          label: this.$t("ui.data.column.finishStock.isExceedThreeMonth"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_yes_no, value);
          },
        },
        {
          prop: "isExceedSixMonth",
          label: this.$t("ui.data.column.finishStock.isExceedSixMonth"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_yes_no, value);
          },
        },
        {
          prop: "isExceedTwelveMonth",
          label: this.$t("ui.data.column.finishStock.isExceedTwelveMonth"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_yes_no, value);
          },
        },
        {
          prop: "updateTime",
          width: 180,
          label: this.$t("ui.data.column.scheduleAdjust.updata"),
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
          prop: "productTypeCode",
          label: this.$t("ui.data.column.monthplan.productType"),
          type: "select",
          dictData: this.dict.type.biz_product_type,
        },
        // {
        //   prop: "mouldCode",
        //   label: this.$t("common.brand"),
        // },
        // {

        {
          prop: "materialCode",
          label: this.$t("ui.data.column.monthplan.oriMaterialCode"),
        },
        {
          prop: "materialDesc",
          label: this.$t("ui.data.column.scheduleAdjust.productCodeDesc"),
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
          label: this.$t("ui.data.column.finishStock.isExceedMonth"),
          type: "checkbox",
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
        params.isExceedThreeMonth = 1;
        params.isExceedSixMonth = 1;
        params.isExceedNineMonth = 1;
        params.isExceedTwelveMonth = 1;
      } else {
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
